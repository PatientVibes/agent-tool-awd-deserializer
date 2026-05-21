"""
Module: component_extractor - Base ComponentExtractor framework for parallel AWD component extraction

Summary:
    Provides a unified architecture for extracting different types of components from AWD .design files.
    Implements the base ComponentExtractor class with common patterns, error handling, validation,
    and performance monitoring. Serves as the foundation for specialized extractors handling
    service configurations, data types, BPMN processes, and forms.

Key Components:
    - ComponentExtractor: Abstract base class with common extraction patterns
    - ExtractionResult: Structured result container with metadata and validation
    - ValidationFramework: Reference file validation against validation_output/actual_components/
    - PerformanceMonitor: 267ms processing budget enforcement
    - ExtractionContext: Shared context and state management across extractors

Keywords: component, extraction, framework, base, architecture, awd, parallel, validation,
         performance, monitoring, service, data, bpmn, forms, enterprise, governance
Dependencies: abc, json, pathlib, time, logging, dataclasses, typing, asyncio
Security: Input validation, file path sanitization, resource monitoring, safe JSON parsing
Performance: Parallel processing, streaming extraction, memory-efficient data handling
"""

import os
import json
import time
import asyncio
from abc import ABC, abstractmethod
from pathlib import Path
from typing import Dict, List, Optional, Any, Union, Tuple, Type
from dataclasses import dataclass, asdict, field
from contextlib import asynccontextmanager
import logging

logger = logging.getLogger(__name__)


@dataclass
class ExtractionContext:
    """Shared context and configuration for component extraction."""
    source_file: str
    output_directory: str
    source_name: Optional[str] = None
    performance_budget_ms: int = 267
    validation_enabled: bool = True
    reference_dir: str = "validation_output/actual_components"
    max_concurrent_extractors: int = 4
    preserve_metadata: bool = True
    
    def __post_init__(self):
        """Validate and normalize context parameters."""
        if not os.path.exists(self.source_file):
            raise ValueError(f"Source file not found: {self.source_file}")
        
        # Create output directory if it doesn't exist
        os.makedirs(self.output_directory, exist_ok=True)
        
        # Normalize paths
        self.source_file = os.path.abspath(self.source_file)
        self.output_directory = os.path.abspath(self.output_directory)


@dataclass
class ComponentMetadata:
    """Metadata for extracted components."""
    component_type: str
    source_location: str
    extraction_time: float
    file_size_bytes: int
    validation_status: str = "pending"
    guid: Optional[str] = None
    dependencies: List[str] = field(default_factory=list)
    business_rules: List[str] = field(default_factory=list)


@dataclass
class ExtractionResult:
    """Structured result from component extraction with validation."""
    extractor_name: str
    component_type: str
    success: bool
    files_generated: List[str]
    metadata: ComponentMetadata
    processing_time_ms: float
    errors: List[str] = field(default_factory=list)
    warnings: List[str] = field(default_factory=list)
    validation_results: Dict[str, Any] = field(default_factory=dict)
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert result to dictionary for JSON serialization."""
        return asdict(self)
    
    def is_valid(self) -> bool:
        """Check if extraction result meets validation criteria."""
        return (self.success and 
                len(self.files_generated) > 0 and 
                len(self.errors) == 0 and
                self.processing_time_ms <= 267)


class PerformanceMonitor:
    """Monitor extraction performance and enforce processing budgets."""
    
    def __init__(self, budget_ms: int = 267):
        self.budget_ms = budget_ms
        self.start_time: Optional[float] = None
        self.checkpoints: List[Tuple[str, float]] = []
    
    def start(self) -> None:
        """Start performance monitoring."""
        self.start_time = time.time() * 1000
        logger.debug(f"Performance monitoring started with {self.budget_ms}ms budget")
    
    def checkpoint(self, name: str) -> float:
        """Record performance checkpoint and return elapsed time."""
        if self.start_time is None:
            raise RuntimeError("Performance monitoring not started")
        
        elapsed = (time.time() * 1000) - self.start_time
        self.checkpoints.append((name, elapsed))
        
        if elapsed > self.budget_ms:
            logger.warning(f"Performance budget exceeded at {name}: {elapsed:.1f}ms > {self.budget_ms}ms")
        
        logger.debug(f"Checkpoint {name}: {elapsed:.1f}ms")
        return elapsed
    
    def finish(self) -> float:
        """Finish monitoring and return total elapsed time."""
        total_time = self.checkpoint("finish")
        
        if total_time > self.budget_ms:
            logger.error(f"Performance budget exceeded: {total_time:.1f}ms > {self.budget_ms}ms")
        
        return total_time
    
    def get_report(self) -> Dict[str, Any]:
        """Generate performance report."""
        if not self.checkpoints:
            return {"status": "not_started"}
        
        total_time = self.checkpoints[-1][1]
        return {
            "total_time_ms": total_time,
            "budget_ms": self.budget_ms,
            "budget_exceeded": total_time > self.budget_ms,
            "checkpoints": self.checkpoints,
            "efficiency": (self.budget_ms / total_time) if total_time > 0 else 1.0
        }


class ValidationFramework:
    """Framework for validating extracted components against reference files."""
    
    def __init__(self, reference_dir: str):
        self.reference_dir = reference_dir
        self.reference_cache: Dict[str, Any] = {}
    
    async def validate_component(self, 
                               component_type: str, 
                               generated_file: str, 
                               component_name: str) -> Dict[str, Any]:
        """Validate generated component against reference files."""
        
        validation_result = {
            "component_type": component_type,
            "component_name": component_name,
            "generated_file": generated_file,
            "reference_found": False,
            "validation_passed": False,
            "differences": [],
            "score": 0.0
        }
        
        try:
            # Find reference file
            reference_file = await self._find_reference_file(component_type, component_name)
            
            if reference_file:
                validation_result["reference_found"] = True
                validation_result["reference_file"] = reference_file
                
                # Perform validation based on component type
                if component_type == "bpmn":
                    validation_result.update(await self._validate_bpmn(generated_file, reference_file))
                elif component_type == "forms":
                    validation_result.update(await self._validate_form(generated_file, reference_file))
                elif component_type == "data_types":
                    validation_result.update(await self._validate_data_type(generated_file, reference_file))
                elif component_type == "services":
                    validation_result.update(await self._validate_service(generated_file, reference_file))
                else:
                    validation_result["differences"].append(f"Unknown component type: {component_type}")
            
            else:
                validation_result["differences"].append("No reference file found for validation")
        
        except Exception as e:
            logger.error(f"Validation error for {component_type}/{component_name}: {e}")
            validation_result["differences"].append(f"Validation error: {str(e)}")
        
        return validation_result
    
    async def _find_reference_file(self, component_type: str, component_name: str) -> Optional[str]:
        """Find reference file for validation."""
        type_dir = os.path.join(self.reference_dir, component_type)
        
        if not os.path.exists(type_dir):
            return None
        
        # Try exact match first
        for ext in ['.bpmn', '.xml', '.json', '.txt']:
            exact_match = os.path.join(type_dir, f"{component_name}{ext}")
            if os.path.exists(exact_match):
                return exact_match
        
        # Try fuzzy matching based on name similarity
        for file in os.listdir(type_dir):
            if component_name.lower() in file.lower():
                return os.path.join(type_dir, file)
        
        return None
    
    async def _validate_bpmn(self, generated: str, reference: str) -> Dict[str, Any]:
        """Validate BPMN file against reference."""
        # Simplified validation - check for key BPMN elements
        differences = []
        score = 0.0
        
        try:
            with open(generated, 'r', encoding='utf-8') as f:
                generated_content = f.read()
            
            with open(reference, 'r', encoding='utf-8') as f:
                reference_content = f.read()
            
            # Check for essential BPMN elements
            bpmn_elements = ['bpmn:process', 'bpmn:startEvent', 'bpmn:endEvent', 'bpmn:userTask']
            for element in bpmn_elements:
                gen_has = element in generated_content
                ref_has = element in reference_content
                
                if gen_has == ref_has:
                    score += 0.25
                else:
                    differences.append(f"Element {element} mismatch: generated={gen_has}, reference={ref_has}")
            
            return {
                "validation_passed": score >= 0.75,
                "score": score,
                "differences": differences
            }
        
        except Exception as e:
            return {
                "validation_passed": False,
                "score": 0.0,
                "differences": [f"BPMN validation error: {str(e)}"]
            }
    
    async def _validate_form(self, generated: str, reference: str) -> Dict[str, Any]:
        """Validate form file against reference."""
        differences = []
        score = 1.0  # Start with perfect score
        
        try:
            # Basic file existence and size validation
            gen_size = os.path.getsize(generated)
            ref_size = os.path.getsize(reference)
            
            size_ratio = min(gen_size, ref_size) / max(gen_size, ref_size) if max(gen_size, ref_size) > 0 else 0
            score *= size_ratio
            
            if size_ratio < 0.8:
                differences.append(f"File size difference: generated={gen_size}, reference={ref_size}")
            
            return {
                "validation_passed": score >= 0.7,
                "score": score,
                "differences": differences
            }
        
        except Exception as e:
            return {
                "validation_passed": False,
                "score": 0.0,
                "differences": [f"Form validation error: {str(e)}"]
            }
    
    async def _validate_data_type(self, generated: str, reference: str) -> Dict[str, Any]:
        """Validate data type file against reference."""
        differences = []
        score = 0.0
        
        try:
            with open(generated, 'r', encoding='utf-8') as f:
                generated_content = f.read()
            
            with open(reference, 'r', encoding='utf-8') as f:
                reference_content = f.read()
            
            # Check for XML structure elements
            xml_elements = ['dataType', 'structure', 'field']
            for element in xml_elements:
                if element in generated_content and element in reference_content:
                    score += 0.33
                elif element not in generated_content and element not in reference_content:
                    score += 0.33
                else:
                    differences.append(f"XML element {element} mismatch")
            
            return {
                "validation_passed": score >= 0.7,
                "score": score,
                "differences": differences
            }
        
        except Exception as e:
            return {
                "validation_passed": False,
                "score": 0.0,
                "differences": [f"Data type validation error: {str(e)}"]
            }
    
    async def _validate_service(self, generated: str, reference: str) -> Dict[str, Any]:
        """Validate service configuration against reference."""
        differences = []
        score = 1.0
        
        try:
            with open(generated, 'r', encoding='utf-8') as f:
                generated_data = json.load(f)
            
            with open(reference, 'r', encoding='utf-8') as f:
                reference_data = json.load(f)
            
            # Compare JSON structure
            if isinstance(generated_data, list) and isinstance(reference_data, list):
                if len(generated_data) != len(reference_data):
                    differences.append(f"Array length mismatch: {len(generated_data)} vs {len(reference_data)}")
                    score *= 0.8
            
            return {
                "validation_passed": score >= 0.7,
                "score": score,
                "differences": differences
            }
        
        except Exception as e:
            return {
                "validation_passed": False,
                "score": 0.0,
                "differences": [f"Service validation error: {str(e)}"]
            }


class ComponentExtractor(ABC):
    """
    Abstract base class for AWD component extractors.
    
    Provides common functionality for extracting different types of components
    from AWD .design files with performance monitoring, validation, and error handling.
    """
    
    def __init__(self, extractor_name: str):
        self.extractor_name = extractor_name
        self.performance_monitor = PerformanceMonitor()
        self.validation_framework: Optional[ValidationFramework] = None
        
    @property
    @abstractmethod
    def component_type(self) -> str:
        """Return the type of component this extractor handles."""
        pass
    
    @property
    @abstractmethod
    def supported_formats(self) -> List[str]:
        """Return list of supported output formats."""
        pass
    
    @abstractmethod
    async def extract_components(self, 
                               context: ExtractionContext, 
                               processed_data: Dict[str, Any]) -> ExtractionResult:
        """
        Extract components from processed AWD data.
        
        Args:
            context: Extraction context with configuration
            processed_data: Pre-processed data from AWD file
        
        Returns:
            ExtractionResult with generated files and metadata
        """
        pass
    
    async def extract_with_monitoring(self, 
                                    context: ExtractionContext, 
                                    processed_data: Dict[str, Any]) -> ExtractionResult:
        """
        Extract components with performance monitoring and validation.
        
        This is the main entry point that provides common functionality
        like performance monitoring, error handling, and validation.
        """
        
        # Initialize performance monitoring
        self.performance_monitor = PerformanceMonitor(context.performance_budget_ms)
        self.performance_monitor.start()
        
        # Initialize validation framework if enabled
        if context.validation_enabled:
            self.validation_framework = ValidationFramework(context.reference_dir)
        
        try:
            logger.info(f"Starting {self.extractor_name} extraction for {self.component_type}")
            
            # Validate input data
            self._validate_input_data(processed_data)
            self.performance_monitor.checkpoint("input_validation")
            
            # Perform extraction
            result = await self.extract_components(context, processed_data)
            self.performance_monitor.checkpoint("extraction_complete")
            
            # Validate generated files if validation is enabled
            if context.validation_enabled and self.validation_framework:
                await self._validate_generated_files(result)
                self.performance_monitor.checkpoint("validation_complete")
            
            # Finish performance monitoring
            total_time = self.performance_monitor.finish()
            result.processing_time_ms = total_time
            
            # Add performance report to metadata
            performance_report = self.performance_monitor.get_report()
            result.metadata.extraction_time = total_time
            
            logger.info(f"{self.extractor_name} extraction completed: "
                       f"{len(result.files_generated)} files in {total_time:.1f}ms")
            
            return result
        
        except Exception as e:
            # Handle extraction errors
            total_time = self.performance_monitor.finish()
            
            error_result = ExtractionResult(
                extractor_name=self.extractor_name,
                component_type=self.component_type,
                success=False,
                files_generated=[],
                metadata=ComponentMetadata(
                    component_type=self.component_type,
                    source_location=context.source_file,
                    extraction_time=total_time,
                    file_size_bytes=0,
                    validation_status="error"
                ),
                processing_time_ms=total_time,
                errors=[f"Extraction failed: {str(e)}"]
            )
            
            logger.error(f"{self.extractor_name} extraction failed: {e}")
            return error_result
    
    def _validate_input_data(self, processed_data: Dict[str, Any]) -> None:
        """Validate input data structure."""
        if not isinstance(processed_data, dict):
            raise ValueError("Processed data must be a dictionary")
        
        if not processed_data:
            raise ValueError("Processed data is empty")
    
    async def _validate_generated_files(self, result: ExtractionResult) -> None:
        """Validate generated files against reference files."""
        if not self.validation_framework:
            return
        
        validation_results = {}
        
        for generated_file in result.files_generated:
            if not os.path.exists(generated_file):
                result.errors.append(f"Generated file not found: {generated_file}")
                continue
            
            # Extract component name from file path
            component_name = Path(generated_file).stem
            
            # Validate against reference
            validation_result = await self.validation_framework.validate_component(
                self.component_type, generated_file, component_name
            )
            
            validation_results[generated_file] = validation_result
            
            # Add validation warnings/errors to result
            if not validation_result["validation_passed"]:
                result.warnings.append(
                    f"Validation failed for {generated_file}: {validation_result['differences']}"
                )
        
        result.validation_results = validation_results
    
    def _create_component_metadata(self, 
                                 context: ExtractionContext, 
                                 files_generated: List[str]) -> ComponentMetadata:
        """Create metadata for extracted components."""
        total_size = sum(os.path.getsize(f) for f in files_generated if os.path.exists(f))
        
        return ComponentMetadata(
            component_type=self.component_type,
            source_location=context.source_file,
            extraction_time=0.0,  # Will be set later
            file_size_bytes=total_size,
            validation_status="pending"
        )
    
    def _safe_create_directory(self, directory_path: str) -> None:
        """Safely create directory with proper error handling."""
        try:
            os.makedirs(directory_path, exist_ok=True)
            logger.debug(f"Created directory: {directory_path}")
        except Exception as e:
            raise RuntimeError(f"Failed to create directory {directory_path}: {e}")
    
    def _safe_write_file(self, file_path: str, content: str, encoding: str = 'utf-8') -> None:
        """Safely write file with proper error handling."""
        try:
            # Ensure parent directory exists
            parent_dir = os.path.dirname(file_path)
            if parent_dir:
                self._safe_create_directory(parent_dir)
            
            with open(file_path, 'w', encoding=encoding) as f:
                f.write(content)
            
            logger.debug(f"Wrote file: {file_path} ({len(content)} chars)")
        
        except Exception as e:
            raise RuntimeError(f"Failed to write file {file_path}: {e}")
    
    def _safe_write_json(self, file_path: str, data: Any) -> None:
        """Safely write JSON file with proper error handling."""
        try:
            json_content = json.dumps(data, indent=2, ensure_ascii=False)
            self._safe_write_file(file_path, json_content)
        except Exception as e:
            raise RuntimeError(f"Failed to write JSON file {file_path}: {e}")


class ComponentExtractionOrchestrator:
    """
    Orchestrator for coordinating parallel component extraction.
    
    Manages multiple extractors running in parallel while maintaining
    performance budgets and handling dependencies between extractors.
    """
    
    def __init__(self):
        self.extractors: Dict[str, ComponentExtractor] = {}
        self.extraction_semaphore: Optional[asyncio.Semaphore] = None
    
    def register_extractor(self, extractor: ComponentExtractor) -> None:
        """Register a component extractor."""
        self.extractors[extractor.component_type] = extractor
        logger.info(f"Registered extractor: {extractor.extractor_name} for {extractor.component_type}")
    
    async def extract_all_components(self, 
                                   context: ExtractionContext, 
                                   processed_data: Dict[str, Any]) -> Dict[str, ExtractionResult]:
        """
        Extract all registered component types in parallel.
        
        Args:
            context: Extraction context
            processed_data: Pre-processed AWD data
        
        Returns:
            Dictionary mapping component types to extraction results
        """
        
        # Initialize semaphore for concurrency control
        self.extraction_semaphore = asyncio.Semaphore(context.max_concurrent_extractors)
        
        logger.info(f"Starting parallel extraction with {len(self.extractors)} extractors")
        
        # Create extraction tasks
        tasks = []
        for component_type, extractor in self.extractors.items():
            task = asyncio.create_task(
                self._extract_with_semaphore(extractor, context, processed_data)
            )
            tasks.append((component_type, task))
        
        # Execute tasks and collect results
        results = {}
        for component_type, task in tasks:
            try:
                result = await task
                results[component_type] = result
                logger.info(f"Completed extraction for {component_type}: "
                           f"{'success' if result.success else 'failed'}")
            except Exception as e:
                logger.error(f"Failed to extract {component_type}: {e}")
                # Create error result
                results[component_type] = ExtractionResult(
                    extractor_name=f"{component_type}_extractor",
                    component_type=component_type,
                    success=False,
                    files_generated=[],
                    metadata=ComponentMetadata(
                        component_type=component_type,
                        source_location=context.source_file,
                        extraction_time=0.0,
                        file_size_bytes=0,
                        validation_status="error"
                    ),
                    processing_time_ms=0.0,
                    errors=[f"Extraction task failed: {str(e)}"]
                )
        
        return results
    
    async def _extract_with_semaphore(self, 
                                    extractor: ComponentExtractor, 
                                    context: ExtractionContext, 
                                    processed_data: Dict[str, Any]) -> ExtractionResult:
        """Extract components with semaphore-controlled concurrency."""
        async with self.extraction_semaphore:
            return await extractor.extract_with_monitoring(context, processed_data)
    
    def get_extraction_summary(self, results: Dict[str, ExtractionResult]) -> Dict[str, Any]:
        """Generate summary of extraction results."""
        
        total_files = sum(len(result.files_generated) for result in results.values())
        successful_extractions = sum(1 for result in results.values() if result.success)
        total_time = max((result.processing_time_ms for result in results.values()), default=0.0)
        total_errors = sum(len(result.errors) for result in results.values())
        
        return {
            "total_extractors": len(results),
            "successful_extractions": successful_extractions,
            "failed_extractions": len(results) - successful_extractions,
            "total_files_generated": total_files,
            "total_processing_time_ms": total_time,
            "total_errors": total_errors,
            "results_by_type": {
                component_type: {
                    "success": result.success,
                    "files_count": len(result.files_generated),
                    "processing_time_ms": result.processing_time_ms,
                    "errors_count": len(result.errors)
                }
                for component_type, result in results.items()
            }
        }


# Export main classes for use by specialized extractors
__all__ = [
    'ComponentExtractor',
    'ExtractionContext', 
    'ExtractionResult',
    'ComponentMetadata',
    'PerformanceMonitor',
    'ValidationFramework',
    'ComponentExtractionOrchestrator'
]