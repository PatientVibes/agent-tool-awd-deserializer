"""
Module: component_extraction_orchestrator - Integrated component extraction orchestrator

Summary:
    Orchestrates the complete ComponentExtractor framework with all 4 specialized extractors
    integrated into the existing Python codebase. Provides seamless integration with
    java_bridge.py patterns while maintaining enterprise governance standards and
    performance requirements. Coordinates parallel extraction of services, data types,
    BPMN processes, and forms from AWD .design files.

Key Components:
    - IntegratedComponentOrchestrator: Main orchestrator for complete extraction workflow
    - JavaBridgeIntegration: Integration with existing java_bridge.py patterns
    - ExtractionPipeline: Coordinated pipeline for all extraction types
    - ValidationEngine: Comprehensive validation against reference files
    - PerformanceCoordinator: Performance monitoring across all extractors

Keywords: orchestrator, integration, component, extraction, framework, parallel, java, bridge,
         performance, validation, pipeline, coordination, enterprise, governance, awd
Dependencies: asyncio, logging, java_bridge, component extractors, pathlib, json
Security: Input validation, secure processing, resource monitoring, error isolation
Performance: Parallel processing, 267ms budget management, memory optimization
"""

import os
import json
import time
import asyncio
from pathlib import Path
from typing import Dict, List, Optional, Any, Union, Tuple
from dataclasses import dataclass, asdict
import logging

# Import base framework
from .component_extractor import (
    ComponentExtractor,
    ExtractionContext,
    ExtractionResult,
    ComponentExtractionOrchestrator,
    PerformanceMonitor,
    ValidationFramework
)

# Import specialized extractors
from .service_config_extractor import ServiceConfigExtractor
from .data_type_extractor import DataTypeDefinitionExtractor
from .bpmn_process_extractor import BPMNProcessExtractor
from .awd_form_extractor import AWDFormExtractor

# Import existing java bridge
from .java_bridge import JavaBridge, ExtractionRequest as JavaExtractionRequest

logger = logging.getLogger(__name__)


@dataclass
class ComponentExtractionRequest:
    """Request for component extraction with all parameters."""
    source_file: str
    output_directory: str
    extraction_types: List[str]  # services, data_types, bpmn, forms, all
    source_name: Optional[str] = None
    validation_enabled: bool = True
    performance_budget_ms: int = 267
    max_concurrent_extractors: int = 4
    java_preprocessing: bool = True
    
    def to_extraction_context(self) -> ExtractionContext:
        """Convert to ExtractionContext for extractors."""
        return ExtractionContext(
            source_file=self.source_file,
            output_directory=self.output_directory,
            source_name=self.source_name,
            performance_budget_ms=self.performance_budget_ms,
            validation_enabled=self.validation_enabled,
            max_concurrent_extractors=self.max_concurrent_extractors
        )


@dataclass
class ComponentExtractionSummary:
    """Summary of complete component extraction operation."""
    request: ComponentExtractionRequest
    success: bool
    total_files_generated: int
    total_processing_time_ms: float
    java_preprocessing_time_ms: float
    component_extraction_time_ms: float
    validation_time_ms: float
    results_by_type: Dict[str, ExtractionResult]
    errors: List[str]
    warnings: List[str]
    performance_analysis: Dict[str, Any]
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary for JSON serialization."""
        return {
            "summary": {
                "success": self.success,
                "total_files_generated": self.total_files_generated,
                "total_processing_time_ms": self.total_processing_time_ms,
                "java_preprocessing_time_ms": self.java_preprocessing_time_ms,
                "component_extraction_time_ms": self.component_extraction_time_ms,
                "validation_time_ms": self.validation_time_ms,
                "errors_count": len(self.errors),
                "warnings_count": len(self.warnings)
            },
            "request": {
                "source_file": self.request.source_file,
                "output_directory": self.request.output_directory,
                "extraction_types": self.request.extraction_types,
                "validation_enabled": self.request.validation_enabled,
                "performance_budget_ms": self.request.performance_budget_ms
            },
            "results_by_type": {
                component_type: {
                    "success": result.success,
                    "files_count": len(result.files_generated),
                    "processing_time_ms": result.processing_time_ms,
                    "errors_count": len(result.errors),
                    "warnings_count": len(result.warnings),
                    "validation_passed": result.validation_results and 
                                       all(v.get("validation_passed", False) 
                                           for v in result.validation_results.values())
                }
                for component_type, result in self.results_by_type.items()
            },
            "performance_analysis": self.performance_analysis,
            "errors": self.errors,
            "warnings": self.warnings
        }


class JavaBridgeIntegration:
    """Integration layer between ComponentExtractor framework and existing JavaBridge."""
    
    def __init__(self, java_bridge: JavaBridge):
        self.java_bridge = java_bridge
        
    async def preprocess_awd_file(self, request: ComponentExtractionRequest) -> Dict[str, Any]:
        """Preprocess AWD file using existing Java bridge to get processed data."""
        
        logger.info(f"Preprocessing AWD file: {request.source_file}")
        
        try:
            # Create Java extraction request for preprocessing
            java_request = JavaExtractionRequest(
                source_file=request.source_file,
                output_directory=request.output_directory,
                extraction_types=["all"],  # Get all data for component processing
                source_name=request.source_name,
                include_metadata=True,
                timeout_minutes=5  # Keep preprocessing quick
            )
            
            # Use existing Java bridge to extract and process the file
            java_result = await self.java_bridge.awd_extractor.extract_files(java_request)
            
            if not java_result.success:
                raise RuntimeError(f"Java preprocessing failed: {java_result.errors}")
            
            # Load processed JSON data from Java output
            processed_data = await self._load_processed_data(java_result, request.output_directory)
            
            logger.info(f"Java preprocessing completed successfully: {java_result.total_files} files generated")
            return processed_data
        
        except Exception as e:
            logger.error(f"Java preprocessing failed: {e}")
            raise RuntimeError(f"Failed to preprocess AWD file: {e}")
    
    async def _load_processed_data(self, 
                                 java_result: Any, 
                                 output_directory: str) -> Dict[str, Any]:
        """Load processed data from Java extraction result."""
        
        processed_data = {}
        
        # Look for JSON files generated by Java processing
        output_path = Path(output_directory)
        
        # Find and load JSON files that contain processed data
        json_files = list(output_path.rglob("*.json"))
        
        for json_file in json_files:
            try:
                with open(json_file, 'r', encoding='utf-8') as f:
                    file_data = json.load(f)
                
                # Merge data from different files
                file_key = json_file.stem
                processed_data[file_key] = file_data
                
            except Exception as e:
                logger.warning(f"Failed to load JSON file {json_file}: {e}")
        
        # Also include metadata from Java result
        processed_data["java_result_metadata"] = {
            "source_name": java_result.source_name,
            "total_files": java_result.total_files,
            "processing_time_ms": java_result.processing_time_ms,
            "files_generated": java_result.files_generated
        }
        
        return processed_data


class ExtractionPipeline:
    """Coordinated extraction pipeline for all component types."""
    
    def __init__(self):
        self.orchestrator = ComponentExtractionOrchestrator()
        self.performance_monitor = PerformanceMonitor()
        
        # Register all extractors
        self._register_extractors()
    
    def _register_extractors(self):
        """Register all specialized extractors."""
        self.orchestrator.register_extractor(ServiceConfigExtractor())
        self.orchestrator.register_extractor(DataTypeDefinitionExtractor())
        self.orchestrator.register_extractor(BPMNProcessExtractor())
        self.orchestrator.register_extractor(AWDFormExtractor())
        
        logger.info("Registered 4 specialized component extractors")
    
    async def execute_extraction(self, 
                               context: ExtractionContext, 
                               processed_data: Dict[str, Any],
                               extraction_types: List[str]) -> Dict[str, ExtractionResult]:
        """Execute component extraction pipeline."""
        
        self.performance_monitor.start()
        
        # Filter extractors based on requested types
        if "all" not in extraction_types:
            # Create filtered orchestrator with only requested extractors
            filtered_orchestrator = ComponentExtractionOrchestrator()
            
            type_mapping = {
                "services": ServiceConfigExtractor(),
                "data_types": DataTypeDefinitionExtractor(), 
                "bpmn": BPMNProcessExtractor(),
                "forms": AWDFormExtractor()
            }
            
            for extraction_type in extraction_types:
                if extraction_type in type_mapping:
                    filtered_orchestrator.register_extractor(type_mapping[extraction_type])
            
            results = await filtered_orchestrator.extract_all_components(context, processed_data)
        else:
            # Use full orchestrator for all types
            results = await self.orchestrator.extract_all_components(context, processed_data)
        
        self.performance_monitor.checkpoint("component_extraction_complete")
        
        return results


class ValidationEngine:
    """Comprehensive validation engine for all extracted components."""
    
    def __init__(self, reference_dir: str):
        self.validation_framework = ValidationFramework(reference_dir)
        
    async def validate_extraction_results(self, 
                                        results: Dict[str, ExtractionResult]) -> Dict[str, Any]:
        """Validate all extraction results against reference files."""
        
        validation_summary = {
            "total_components": 0,
            "validated_components": 0,
            "validation_passed": 0,
            "validation_failed": 0,
            "validation_details": {}
        }
        
        for component_type, result in results.items():
            if not result.success:
                continue
            
            component_validations = []
            
            for generated_file in result.files_generated:
                if os.path.exists(generated_file):
                    # Extract component name from file
                    component_name = Path(generated_file).stem
                    
                    # Validate component
                    validation_result = await self.validation_framework.validate_component(
                        component_type, generated_file, component_name
                    )
                    
                    component_validations.append(validation_result)
                    validation_summary["total_components"] += 1
                    validation_summary["validated_components"] += 1
                    
                    if validation_result["validation_passed"]:
                        validation_summary["validation_passed"] += 1
                    else:
                        validation_summary["validation_failed"] += 1
            
            validation_summary["validation_details"][component_type] = component_validations
        
        return validation_summary


class PerformanceCoordinator:
    """Coordinate performance monitoring across all extractors."""
    
    def __init__(self, budget_ms: int = 267):
        self.budget_ms = budget_ms
        self.main_monitor = PerformanceMonitor(budget_ms)
        self.phase_timings: Dict[str, float] = {}
    
    def start_monitoring(self):
        """Start performance monitoring."""
        self.main_monitor.start()
    
    def record_phase(self, phase_name: str) -> float:
        """Record completion of a processing phase."""
        elapsed = self.main_monitor.checkpoint(phase_name)
        self.phase_timings[phase_name] = elapsed
        return elapsed
    
    def finish_monitoring(self) -> Dict[str, Any]:
        """Finish monitoring and generate performance analysis."""
        total_time = self.main_monitor.finish()
        
        analysis = {
            "total_time_ms": total_time,
            "budget_ms": self.budget_ms,
            "budget_exceeded": total_time > self.budget_ms,
            "budget_utilization": (total_time / self.budget_ms) * 100,
            "phase_timings": self.phase_timings,
            "performance_report": self.main_monitor.get_report()
        }
        
        # Add performance recommendations
        if total_time > self.budget_ms:
            analysis["recommendations"] = self._generate_performance_recommendations(analysis)
        
        return analysis
    
    def _generate_performance_recommendations(self, analysis: Dict[str, Any]) -> List[str]:
        """Generate performance improvement recommendations."""
        recommendations = []
        
        phase_timings = analysis["phase_timings"]
        
        # Check for slow phases
        if phase_timings.get("java_preprocessing", 0) > 100:
            recommendations.append("Consider optimizing Java preprocessing pipeline")
        
        if phase_timings.get("component_extraction", 0) > 150:
            recommendations.append("Consider reducing concurrent extractor count")
        
        if phase_timings.get("validation", 0) > 50:
            recommendations.append("Consider disabling validation for performance-critical scenarios")
        
        if analysis["budget_utilization"] > 150:
            recommendations.append("Consider processing files in smaller batches")
        
        return recommendations


class IntegratedComponentOrchestrator:
    """
    Main orchestrator for complete component extraction workflow.
    
    Integrates with existing Java bridge while providing parallel component
    extraction with comprehensive validation and performance monitoring.
    """
    
    def __init__(self, java_bridge: Optional[JavaBridge] = None):
        self.java_bridge_integration = JavaBridgeIntegration(
            java_bridge or self._get_default_java_bridge()
        )
        self.extraction_pipeline = ExtractionPipeline()
        self.performance_coordinator = PerformanceCoordinator()
        
    def _get_default_java_bridge(self) -> JavaBridge:
        """Get default Java bridge instance."""
        from .java_bridge import get_java_bridge
        return get_java_bridge()
    
    async def extract_components(self, request: ComponentExtractionRequest) -> ComponentExtractionSummary:
        """
        Execute complete component extraction workflow.
        
        This is the main entry point for the integrated component extraction system.
        """
        
        logger.info(f"Starting component extraction for {request.source_file}")
        logger.info(f"Extraction types: {request.extraction_types}")
        logger.info(f"Performance budget: {request.performance_budget_ms}ms")
        
        # Start performance monitoring
        self.performance_coordinator = PerformanceCoordinator(request.performance_budget_ms)
        self.performance_coordinator.start_monitoring()
        
        errors = []
        warnings = []
        results_by_type = {}
        
        try:
            # Phase 1: Java preprocessing (if enabled)
            java_preprocessing_time = 0.0
            if request.java_preprocessing:
                processed_data = await self.java_bridge_integration.preprocess_awd_file(request)
                java_preprocessing_time = self.performance_coordinator.record_phase("java_preprocessing")
            else:
                # Skip Java preprocessing, assume data is already processed
                processed_data = {"source_file": request.source_file}
                self.performance_coordinator.record_phase("java_preprocessing_skipped")
            
            # Phase 2: Component extraction
            context = request.to_extraction_context()
            results_by_type = await self.extraction_pipeline.execute_extraction(
                context, processed_data, request.extraction_types
            )
            component_extraction_time = self.performance_coordinator.record_phase("component_extraction")
            
            # Phase 3: Validation (if enabled)
            validation_time = 0.0
            if request.validation_enabled:
                validation_engine = ValidationEngine(context.reference_dir)
                validation_summary = await validation_engine.validate_extraction_results(results_by_type)
                validation_time = self.performance_coordinator.record_phase("validation")
                
                # Add validation results to each extraction result
                for component_type, result in results_by_type.items():
                    if component_type in validation_summary["validation_details"]:
                        result.validation_results = {
                            f"validation_{i}": val_result
                            for i, val_result in enumerate(validation_summary["validation_details"][component_type])
                        }
            else:
                self.performance_coordinator.record_phase("validation_skipped")
            
            # Phase 4: Generate summary files
            await self._generate_summary_files(request, results_by_type)
            self.performance_coordinator.record_phase("summary_generation")
            
            # Calculate totals
            total_files = sum(len(result.files_generated) for result in results_by_type.values())
            all_errors = []
            all_warnings = []
            
            for result in results_by_type.values():
                all_errors.extend(result.errors)
                all_warnings.extend(result.warnings)
            
            success = len(all_errors) == 0 and all(result.success for result in results_by_type.values())
            
            logger.info(f"Component extraction completed: {total_files} files generated")
            
        except Exception as e:
            error_msg = f"Component extraction failed: {str(e)}"
            logger.error(error_msg)
            errors.append(error_msg)
            success = False
            total_files = 0
            java_preprocessing_time = 0.0
            component_extraction_time = 0.0
            validation_time = 0.0
        
        # Finish performance monitoring
        performance_analysis = self.performance_coordinator.finish_monitoring()
        total_processing_time = performance_analysis["total_time_ms"]
        
        # Create summary
        summary = ComponentExtractionSummary(
            request=request,
            success=success,
            total_files_generated=total_files,
            total_processing_time_ms=total_processing_time,
            java_preprocessing_time_ms=java_preprocessing_time,
            component_extraction_time_ms=component_extraction_time,
            validation_time_ms=validation_time,
            results_by_type=results_by_type,
            errors=errors + all_errors,
            warnings=warnings + all_warnings,
            performance_analysis=performance_analysis
        )
        
        # Write summary to output directory
        await self._write_extraction_summary(summary)
        
        return summary
    
    async def _generate_summary_files(self, 
                                    request: ComponentExtractionRequest, 
                                    results: Dict[str, ExtractionResult]) -> None:
        """Generate summary files for the extraction."""
        
        # Generate master file manifest
        all_files = []
        for component_type, result in results.items():
            for file_path in result.files_generated:
                all_files.append({
                    "file": os.path.basename(file_path),
                    "full_path": file_path,
                    "component_type": component_type,
                    "size_bytes": os.path.getsize(file_path) if os.path.exists(file_path) else 0
                })
        
        manifest = {
            "extraction_manifest": {
                "source_file": request.source_file,
                "extraction_types": request.extraction_types,
                "total_files": len(all_files),
                "files_by_type": {
                    component_type: len(result.files_generated)
                    for component_type, result in results.items()
                },
                "files": all_files
            }
        }
        
        manifest_file = os.path.join(request.output_directory, "extraction_manifest.json")
        with open(manifest_file, 'w', encoding='utf-8') as f:
            json.dump(manifest, f, indent=2, ensure_ascii=False)
    
    async def _write_extraction_summary(self, summary: ComponentExtractionSummary) -> None:
        """Write extraction summary to output directory."""
        
        summary_file = os.path.join(summary.request.output_directory, "component_extraction_summary.json")
        summary_data = summary.to_dict()
        
        with open(summary_file, 'w', encoding='utf-8') as f:
            json.dump(summary_data, f, indent=2, ensure_ascii=False)
        
        logger.info(f"Extraction summary written to: {summary_file}")


# Convenience functions for easy integration with existing codebase

async def extract_all_components(source_file: str, 
                               output_directory: str,
                               source_name: Optional[str] = None,
                               validation_enabled: bool = True,
                               performance_budget_ms: int = 267) -> ComponentExtractionSummary:
    """
    Extract all component types from an AWD file.
    
    Convenience function that integrates with existing patterns while providing
    full component extraction capabilities.
    """
    
    request = ComponentExtractionRequest(
        source_file=source_file,
        output_directory=output_directory,
        extraction_types=["all"],
        source_name=source_name,
        validation_enabled=validation_enabled,
        performance_budget_ms=performance_budget_ms
    )
    
    orchestrator = IntegratedComponentOrchestrator()
    return await orchestrator.extract_components(request)


async def extract_specific_components(source_file: str,
                                    output_directory: str,
                                    component_types: List[str],
                                    source_name: Optional[str] = None,
                                    validation_enabled: bool = True) -> ComponentExtractionSummary:
    """
    Extract specific component types from an AWD file.
    
    Args:
        component_types: List of component types to extract 
                        (services, data_types, bpmn, forms)
    """
    
    request = ComponentExtractionRequest(
        source_file=source_file,
        output_directory=output_directory,
        extraction_types=component_types,
        source_name=source_name,
        validation_enabled=validation_enabled
    )
    
    orchestrator = IntegratedComponentOrchestrator()
    return await orchestrator.extract_components(request)


# Integration with existing java_bridge patterns
def create_component_extraction_request_from_java_request(
    java_request: JavaExtractionRequest) -> ComponentExtractionRequest:
    """Create ComponentExtractionRequest from existing JavaExtractionRequest."""
    
    # Map Java extraction types to component types
    component_types = []
    if "all" in java_request.extraction_types:
        component_types = ["all"]
    else:
        type_mapping = {
            "bpmn": "bpmn",
            "forms": "forms", 
            "services": "services",
            "data": "data_types"
        }
        
        for java_type in java_request.extraction_types:
            if java_type in type_mapping:
                component_types.append(type_mapping[java_type])
    
    return ComponentExtractionRequest(
        source_file=java_request.source_file,
        output_directory=java_request.output_directory,
        extraction_types=component_types,
        source_name=java_request.source_name,
        validation_enabled=True,
        performance_budget_ms=267,
        java_preprocessing=True
    )


# Export main classes and functions
__all__ = [
    'IntegratedComponentOrchestrator',
    'ComponentExtractionRequest',
    'ComponentExtractionSummary',
    'extract_all_components',
    'extract_specific_components',
    'create_component_extraction_request_from_java_request'
]