"""Unit tests for the ComponentExtractor framework.

Adapted from upstream chorus-deserializer:
  - tests/test_component_extractor_framework.py
  - tests/test_chorus_api.py (only the Pydantic model + helper tests
    survived; the API server itself was dropped from this fork.)

Tests covering the dropped api_server / websocket / Redis / Docker
surfaces have been removed rather than mocked into existence.
"""

import asyncio
import json
import os
import tempfile
import time
from pathlib import Path
from typing import Any, Dict, List

import pytest

from awd_deserializer.component_extractor import (
    ComponentExtractionOrchestrator,
    ComponentExtractor,
    ComponentMetadata,
    ExtractionContext,
    ExtractionResult,
    PerformanceMonitor,
    ValidationFramework,
)


# ---------------------------------------------------------------------------
# PerformanceMonitor
# ---------------------------------------------------------------------------


class TestPerformanceMonitor:
    def test_initialization(self):
        monitor = PerformanceMonitor(267)
        assert monitor.budget_ms == 267
        assert monitor.start_time is None
        assert monitor.checkpoints == []

    def test_start_and_checkpoint(self):
        monitor = PerformanceMonitor(267)
        monitor.start()
        assert monitor.start_time is not None

        elapsed = monitor.checkpoint("step")
        assert elapsed >= 0
        assert len(monitor.checkpoints) == 1
        assert monitor.checkpoints[0][0] == "step"

        total = monitor.finish()
        assert total >= elapsed

    def test_budget_exceeded_warning(self, caplog):
        monitor = PerformanceMonitor(1)  # 1ms budget
        monitor.start()
        time.sleep(0.005)  # 5ms > budget
        elapsed = monitor.checkpoint("slow")
        assert elapsed > 1
        assert "Performance budget exceeded" in caplog.text

    def test_report_generation(self):
        monitor = PerformanceMonitor(267)
        monitor.start()
        monitor.checkpoint("step1")
        monitor.checkpoint("step2")
        total = monitor.finish()

        report = monitor.get_report()
        assert report["total_time_ms"] == total
        assert report["budget_ms"] == 267
        assert "budget_exceeded" in report
        assert len(report["checkpoints"]) == 3  # step1, step2, finish


# ---------------------------------------------------------------------------
# ValidationFramework
# ---------------------------------------------------------------------------


@pytest.fixture
def temp_reference_dir(tmp_path):
    """Create a temporary reference directory with sample files."""
    bpmn_dir = tmp_path / "bpmn"
    forms_dir = tmp_path / "forms"
    data_types_dir = tmp_path / "data_types"
    services_dir = tmp_path / "services"

    for d in (bpmn_dir, forms_dir, data_types_dir, services_dir):
        d.mkdir(parents=True)

    (bpmn_dir / "test_process.bpmn").write_text(
        """<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL">
    <bpmn:process id="test_process">
        <bpmn:startEvent id="start"/>
        <bpmn:endEvent id="end"/>
        <bpmn:userTask id="task"/>
    </bpmn:process>
</bpmn:definitions>"""
    )
    (services_dir / "test_service.json").write_text(
        '[{"name": "test_service", "inputs": [], "outputs": []}]'
    )
    return str(tmp_path)


class TestValidationFramework:
    @pytest.mark.asyncio
    async def test_initialization(self, temp_reference_dir):
        framework = ValidationFramework(temp_reference_dir)
        assert framework.reference_dir == temp_reference_dir
        assert framework.reference_cache == {}

    @pytest.mark.asyncio
    async def test_find_reference_file_exact_match(self, temp_reference_dir):
        framework = ValidationFramework(temp_reference_dir)
        ref = await framework._find_reference_file("bpmn", "test_process")
        assert ref is not None
        assert "test_process.bpmn" in ref

    @pytest.mark.asyncio
    async def test_find_reference_file_fuzzy_match(self, temp_reference_dir):
        framework = ValidationFramework(temp_reference_dir)
        fuzzy = await framework._find_reference_file("bpmn", "test")
        assert fuzzy is not None

    @pytest.mark.asyncio
    async def test_find_reference_file_no_match(self, temp_reference_dir):
        framework = ValidationFramework(temp_reference_dir)
        none = await framework._find_reference_file("bpmn", "nonexistent_component")
        assert none is None

    @pytest.mark.asyncio
    async def test_bpmn_validation_passes(self, temp_reference_dir, tmp_path):
        framework = ValidationFramework(temp_reference_dir)
        gen = tmp_path / "generated.bpmn"
        gen.write_text(
            """<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL">
    <bpmn:process id="test_process">
        <bpmn:startEvent id="start"/>
        <bpmn:endEvent id="end"/>
        <bpmn:userTask id="task"/>
    </bpmn:process>
</bpmn:definitions>"""
        )
        ref = os.path.join(temp_reference_dir, "bpmn", "test_process.bpmn")
        result = await framework._validate_bpmn(str(gen), ref)
        assert result["validation_passed"] is True
        assert result["score"] >= 0.75
        assert result["differences"] == []


# ---------------------------------------------------------------------------
# ExtractionContext
# ---------------------------------------------------------------------------


class TestExtractionContext:
    def test_initialization_with_valid_params(self, tmp_path):
        source_file = tmp_path / "src.json"
        source_file.write_text("{}")
        output_dir = tmp_path / "out"

        context = ExtractionContext(
            source_file=str(source_file),
            output_directory=str(output_dir),
            source_name="test_source",
            performance_budget_ms=267,
        )
        assert os.path.isabs(context.source_file)
        assert os.path.isabs(context.output_directory)
        assert context.performance_budget_ms == 267
        assert context.validation_enabled is True
        assert context.max_concurrent_extractors == 4

    def test_validation_errors_on_missing_source(self, tmp_path):
        with pytest.raises(ValueError, match="Source file not found"):
            ExtractionContext(
                source_file=str(tmp_path / "nonexistent.json"),
                output_directory=str(tmp_path),
            )

    def test_directory_auto_creation(self, tmp_path):
        source_file = tmp_path / "src.json"
        source_file.write_text("{}")
        new_dir = tmp_path / "new_output_dir"

        context = ExtractionContext(
            source_file=str(source_file),
            output_directory=str(new_dir),
        )
        assert os.path.exists(new_dir)
        assert context.output_directory == os.path.abspath(str(new_dir))


# ---------------------------------------------------------------------------
# ComponentMetadata / ExtractionResult
# ---------------------------------------------------------------------------


class TestComponentMetadata:
    def test_creation(self):
        metadata = ComponentMetadata(
            component_type="bpmn",
            source_location="/path/to/source.design",
            extraction_time=125.5,
            file_size_bytes=2048,
            validation_status="passed",
            guid="test-guid-123",
            dependencies=["dep1", "dep2"],
            business_rules=["rule1", "rule2"],
        )
        assert metadata.component_type == "bpmn"
        assert metadata.extraction_time == 125.5
        assert metadata.file_size_bytes == 2048
        assert metadata.validation_status == "passed"
        assert len(metadata.dependencies) == 2
        assert len(metadata.business_rules) == 2


class TestExtractionResultBase:
    def _meta(self):
        return ComponentMetadata(
            component_type="bpmn",
            source_location="/path/to/source.design",
            extraction_time=100.0,
            file_size_bytes=1024,
        )

    def test_creation(self):
        result = ExtractionResult(
            extractor_name="BPMNProcessExtractor",
            component_type="bpmn",
            success=True,
            files_generated=["/path/to/output.bpmn"],
            metadata=self._meta(),
            processing_time_ms=100.0,
        )
        assert result.extractor_name == "BPMNProcessExtractor"
        assert result.success is True
        assert len(result.files_generated) == 1
        assert result.processing_time_ms == 100.0

    def test_is_valid_within_budget(self):
        result = ExtractionResult(
            extractor_name="BPMNProcessExtractor",
            component_type="bpmn",
            success=True,
            files_generated=["/path/to/output.bpmn"],
            metadata=self._meta(),
            processing_time_ms=200.0,
        )
        assert result.is_valid() is True

    def test_is_valid_exceeds_budget(self):
        result = ExtractionResult(
            extractor_name="BPMNProcessExtractor",
            component_type="bpmn",
            success=True,
            files_generated=["/path/to/output.bpmn"],
            metadata=self._meta(),
            processing_time_ms=300.0,  # > 267 budget
        )
        assert result.is_valid() is False

    def test_is_valid_with_errors(self):
        result = ExtractionResult(
            extractor_name="BPMNProcessExtractor",
            component_type="bpmn",
            success=True,
            files_generated=["/path/to/output.bpmn"],
            metadata=self._meta(),
            processing_time_ms=200.0,
            errors=["something broke"],
        )
        assert result.is_valid() is False

    def test_to_dict_serializes_to_json(self):
        result = ExtractionResult(
            extractor_name="BPMNProcessExtractor",
            component_type="bpmn",
            success=True,
            files_generated=["/path/to/output.bpmn"],
            metadata=self._meta(),
            processing_time_ms=100.0,
        )
        as_dict = result.to_dict()
        assert isinstance(as_dict, dict)
        assert as_dict["extractor_name"] == "BPMNProcessExtractor"
        assert as_dict["success"] is True
        assert "metadata" in as_dict
        # Round-trip through JSON to confirm it's serializable
        json_str = json.dumps(as_dict, default=str)
        assert isinstance(json_str, str)


# ---------------------------------------------------------------------------
# ComponentExtractor lifecycle
# ---------------------------------------------------------------------------


class _MockExtractor(ComponentExtractor):
    """Minimal ComponentExtractor for lifecycle tests."""

    def __init__(self, extractor_name: str, component_type: str):
        super().__init__(extractor_name)
        self._component_type = component_type
        self.extraction_call_count = 0

    @property
    def component_type(self) -> str:
        return self._component_type

    @property
    def supported_formats(self) -> List[str]:
        return ["xml", "json"]

    async def extract_components(
        self, context: ExtractionContext, processed_data: Dict[str, Any]
    ) -> ExtractionResult:
        self.extraction_call_count += 1
        await asyncio.sleep(0.01)

        output_file = os.path.join(
            context.output_directory, f"mock_{self.component_type}.xml"
        )
        with open(output_file, "w") as fh:
            fh.write(f"<mock_{self.component_type}>data</mock_{self.component_type}>")

        metadata = self._create_component_metadata(context, [output_file])
        return ExtractionResult(
            extractor_name=self.extractor_name,
            component_type=self.component_type,
            success=True,
            files_generated=[output_file],
            metadata=metadata,
            processing_time_ms=0.0,
        )


class TestComponentExtractorLifecycle:
    @pytest.mark.asyncio
    async def test_extract_with_monitoring_happy_path(self, tmp_path):
        source = tmp_path / "src.json"
        source.write_text('{"test": "data"}')
        output = tmp_path / "out"

        context = ExtractionContext(
            source_file=str(source),
            output_directory=str(output),
            performance_budget_ms=267,
            validation_enabled=False,  # no reference dir
        )

        extractor = _MockExtractor("TestExtractor", "test")
        result = await extractor.extract_with_monitoring(context, {"k": "v"})

        assert result.success is True
        assert result.extractor_name == "TestExtractor"
        assert result.component_type == "test"
        assert len(result.files_generated) == 1
        assert result.processing_time_ms > 0
        assert extractor.extraction_call_count == 1

    @pytest.mark.asyncio
    async def test_extract_with_monitoring_error_path(self, tmp_path):
        class FailingExtractor(_MockExtractor):
            async def extract_components(self, context, processed_data):
                raise ValueError("Test extraction error")

        source = tmp_path / "src.json"
        source.write_text('{"test": "data"}')
        output = tmp_path / "out"

        context = ExtractionContext(
            source_file=str(source),
            output_directory=str(output),
            validation_enabled=False,
        )

        extractor = FailingExtractor("FailingExtractor", "test")
        result = await extractor.extract_with_monitoring(context, {"k": "v"})

        assert result.success is False
        assert len(result.errors) > 0
        assert "Test extraction error" in result.errors[0]


# ---------------------------------------------------------------------------
# ComponentExtractionOrchestrator
# ---------------------------------------------------------------------------


class TestOrchestrator:
    @pytest.mark.asyncio
    async def test_registration(self):
        orch = ComponentExtractionOrchestrator()
        e1 = _MockExtractor("Extractor1", "type1")
        e2 = _MockExtractor("Extractor2", "type2")
        orch.register_extractor(e1)
        orch.register_extractor(e2)

        assert len(orch.extractors) == 2
        assert "type1" in orch.extractors
        assert "type2" in orch.extractors

    @pytest.mark.asyncio
    async def test_parallel_extraction(self, tmp_path):
        source = tmp_path / "src.json"
        source.write_text('{"test": "data"}')
        output = tmp_path / "out"

        context = ExtractionContext(
            source_file=str(source),
            output_directory=str(output),
            max_concurrent_extractors=2,
            validation_enabled=False,
        )

        orch = ComponentExtractionOrchestrator()
        e1 = _MockExtractor("BPMNExtractor", "bpmn")
        e2 = _MockExtractor("FormExtractor", "forms")
        e3 = _MockExtractor("ServiceExtractor", "services")
        orch.register_extractor(e1)
        orch.register_extractor(e2)
        orch.register_extractor(e3)

        results = await orch.extract_all_components(context, {"k": "v"})
        assert len(results) == 3
        assert "bpmn" in results
        assert "forms" in results
        assert "services" in results
        for r in results.values():
            assert r.success is True
        assert e1.extraction_call_count == 1
        assert e2.extraction_call_count == 1
        assert e3.extraction_call_count == 1

    @pytest.mark.asyncio
    async def test_orchestrator_error_handling(self, tmp_path):
        class FailingExtractor(_MockExtractor):
            async def extract_components(self, context, processed_data):
                raise RuntimeError("Extraction failed")

        source = tmp_path / "src.json"
        source.write_text('{"test": "data"}')
        output = tmp_path / "out"

        context = ExtractionContext(
            source_file=str(source),
            output_directory=str(output),
            validation_enabled=False,
        )

        orch = ComponentExtractionOrchestrator()
        success_ext = _MockExtractor("SuccessExtractor", "success")
        fail_ext = FailingExtractor("FailExtractor", "fail")
        orch.register_extractor(success_ext)
        orch.register_extractor(fail_ext)

        results = await orch.extract_all_components(context, {"k": "v"})
        assert len(results) == 2
        assert results["success"].success is True
        assert results["fail"].success is False

    def test_summary_generation(self):
        orch = ComponentExtractionOrchestrator()
        m1 = ComponentMetadata("bpmn", "/source", 100.0, 1024)
        m2 = ComponentMetadata("forms", "/source", 150.0, 2048)
        results = {
            "bpmn": ExtractionResult(
                "BPMNExtractor", "bpmn", True, ["/file1.bpmn"], m1, 100.0
            ),
            "forms": ExtractionResult(
                "FormExtractor", "forms", False, [], m2, 150.0, errors=["err"]
            ),
        }
        summary = orch.get_extraction_summary(results)
        assert summary["total_extractors"] == 2
        assert summary["successful_extractions"] == 1
        assert summary["failed_extractions"] == 1
        assert summary["total_files_generated"] == 1
        assert summary["total_processing_time_ms"] == 150.0
        assert summary["total_errors"] == 1
        assert "results_by_type" in summary


# ---------------------------------------------------------------------------
# java_bridge dataclasses (shape-only — no Java process)
# ---------------------------------------------------------------------------


class TestJavaBridgeDataclasses:
    """Shape checks for the bridge's request/result dataclasses."""

    def test_extraction_request_defaults(self):
        from awd_deserializer.java_bridge import ExtractionRequest

        req = ExtractionRequest(
            source_file="/x.design",
            output_directory="/out",
            extraction_types=["bpmn"],
        )
        assert req.source_name is None
        assert req.dation_format is False
        assert req.include_metadata is True
        assert req.max_concurrent == 4
        assert req.timeout_minutes == 10

    def test_extraction_result_shape(self):
        from awd_deserializer.java_bridge import ExtractionResult as BridgeResult

        result = BridgeResult(
            job_id="awd_1",
            status="success",
            source_name="src",
            output_directory="/out",
            files_generated={"bpmn": ["a.bpmn"]},
            total_files=1,
            processing_time_ms=42,
            errors=[],
            metadata={"v": "1"},
        )
        assert result.job_id == "awd_1"
        assert result.status == "success"
        assert result.total_files == 1
        assert result.files_generated["bpmn"] == ["a.bpmn"]

    def test_result_processor_parses_error_exit(self):
        from awd_deserializer.java_bridge import ResultProcessor

        result = ResultProcessor.parse_extraction_result(
            stdout="", stderr="boom", exit_code=1, job_id="job_1"
        )
        assert result.status == "error"
        assert result.errors and "exit code 1" in result.errors[0]

    def test_result_processor_parses_json_stdout(self):
        from awd_deserializer.java_bridge import ResultProcessor

        payload = json.dumps(
            {
                "status": "success",
                "sourceName": "src",
                "outputDirectory": "/o",
                "filesGenerated": {"bpmn": ["x.bpmn"]},
                "totalFilesGenerated": 1,
                "totalProcessingTime": 12,
                "errors": [],
                "metadata": {},
            }
        )
        result = ResultProcessor.parse_extraction_result(
            stdout=payload, stderr="", exit_code=0, job_id="job_2"
        )
        assert result.status == "success"
        assert result.total_files == 1
        assert result.files_generated == {"bpmn": ["x.bpmn"]}
