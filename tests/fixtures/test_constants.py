"""
Module: test_constants - Common test constants and configurations for Chorus Deserializer

Summary:
    Provides centralized test constants, configuration values, and test data parameters
    used across all test fixtures and test suites. Ensures consistency and maintainability
    of test data and configurations.

Key Components:
    - FILE_SIZE_LIMITS: Standard file size limits for testing
    - MAGIC_BYTES: Valid and invalid magic bytes for .design files
    - SECURITY_CONSTANTS: Security testing parameters
    - PERFORMANCE_THRESHOLDS: Performance benchmarking values
    - ERROR_MESSAGES: Expected error messages for validation

Keywords: test, constants, configuration, limits, thresholds, security, validation,
          performance, benchmarks, fixtures, testing, parameters, chorus, deserializer
Dependencies: None (pure constants module)
Security: Security test constants for vulnerability testing
Performance: Performance benchmarking thresholds
"""

import os
from pathlib import Path
from typing import Dict, List, Tuple, Any

# Base paths
PROJECT_ROOT = Path(__file__).parent.parent.parent
FIXTURES_DIR = Path(__file__).parent
DATA_DIR = FIXTURES_DIR / "data"
EXPECTED_DIR = FIXTURES_DIR / "expected_outputs"

# File size limits (following governance standards)
FILE_SIZE_LIMITS = {
    "small": 1024,  # 1KB
    "medium": 102400,  # 100KB
    "large": 1048576,  # 1MB
    "max_allowed": 104857600,  # 100MB (security limit)
    "oversized": 209715200,  # 200MB (should fail)
}

# Magic bytes for .design files (Java serialization)
MAGIC_BYTES = {
    "valid_java": b"\xac\xed\x00\x05",  # Java serialization stream magic
    "valid_java_tc": b"\xac\xed\x00\x05\x73",  # With TC_OBJECT
    "invalid": b"\xde\xad\xbe\xef",  # Invalid magic bytes
    "malicious": b"\xac\xed\x00\x05\x77",  # With TC_BLOCKDATA (potential exploit)
}

# Security test constants
SECURITY_CONSTANTS = {
    "max_nesting_depth": 100,
    "max_array_size": 1000000,
    "max_string_length": 1048576,  # 1MB
    "blocked_classes": [
        "java.lang.Runtime",
        "java.lang.ProcessBuilder",
        "javax.script.ScriptEngineManager",
        "java.net.URLClassLoader",
        "java.lang.reflect.Proxy",
    ],
    "allowed_classes": [
        "java.lang.String",
        "java.lang.Integer",
        "java.util.ArrayList",
        "java.util.HashMap",
        "com.chorus.model.*",
    ],
}

# Performance thresholds
PERFORMANCE_THRESHOLDS = {
    "small_file_ms": 100,  # Max processing time for small files
    "medium_file_ms": 500,  # Max processing time for medium files
    "large_file_ms": 2000,  # Max processing time for large files
    "memory_overhead_percent": 150,  # Max 150% of file size in memory
    "concurrent_files": 10,  # Number of files for concurrent testing
}

# Test data patterns
TEST_DATA_PATTERNS = {
    "simple_object": {
        "type": "com.chorus.model.SimpleDesign",
        "fields": ["id", "name", "version", "created"],
        "nesting": 0,
    },
    "nested_object": {
        "type": "com.chorus.model.ComplexDesign",
        "fields": ["id", "name", "components", "metadata"],
        "nesting": 3,
    },
    "array_heavy": {
        "type": "com.chorus.model.ArrayDesign",
        "array_sizes": [10, 100, 1000, 10000],
        "nesting": 2,
    },
    "map_heavy": {
        "type": "com.chorus.model.MapDesign",
        "map_entries": [10, 100, 1000],
        "nesting": 2,
    },
}

# Error messages for validation
ERROR_MESSAGES = {
    "invalid_magic": "Invalid file format: not a valid .design file",
    "file_too_large": "File size exceeds maximum allowed limit",
    "blocked_class": "Security violation: blocked class detected",
    "malformed_stream": "Malformed serialization stream",
    "memory_limit": "Memory limit exceeded during deserialization",
    "invalid_encoding": "Invalid character encoding in stream",
}

# JSON output configuration
JSON_OUTPUT_CONFIG = {
    "indent": 2,
    "sort_keys": True,
    "ensure_ascii": False,
}

# Test file metadata
TEST_FILE_METADATA = {
    "small.design": {
        "size": FILE_SIZE_LIMITS["small"],
        "objects": 1,
        "complexity": "simple",
        "valid": True,
    },
    "medium.design": {
        "size": FILE_SIZE_LIMITS["medium"],
        "objects": 50,
        "complexity": "moderate",
        "valid": True,
    },
    "large.design": {
        "size": FILE_SIZE_LIMITS["large"],
        "objects": 1000,
        "complexity": "complex",
        "valid": True,
    },
    "malformed.design": {
        "size": 512,
        "objects": 0,
        "complexity": "invalid",
        "valid": False,
    },
    "edge_cases.design": {
        "size": 10240,
        "objects": 10,
        "complexity": "edge",
        "valid": True,
    },
}

# Validation rules
VALIDATION_RULES = {
    "require_magic_bytes": True,
    "max_file_size": FILE_SIZE_LIMITS["max_allowed"],
    "allowed_extensions": [".design"],
    "check_class_whitelist": True,
    "validate_encoding": True,
}

# Docker test constants
DOCKER_CONSTANTS = {
    "image_name": "chorus-deserializer",
    "container_memory": "512m",
    "container_cpu": "1.0",
    "timeout_seconds": 30,
}

# CLI test constants
CLI_CONSTANTS = {
    "default_output_format": "json",
    "supported_formats": ["json", "yaml", "xml"],
    "max_batch_size": 100,
    "default_timeout": 300,
}


def get_test_file_path(filename: str) -> Path:
    """Get full path for a test data file."""
    return DATA_DIR / filename


def get_expected_output_path(filename: str) -> Path:
    """Get full path for expected output file."""
    base_name = filename.replace(".design", ".json")
    return EXPECTED_DIR / base_name


def get_file_size_category(size: int) -> str:
    """Categorize file size for testing purposes."""
    if size <= FILE_SIZE_LIMITS["small"]:
        return "small"
    elif size <= FILE_SIZE_LIMITS["medium"]:
        return "medium"
    elif size <= FILE_SIZE_LIMITS["large"]:
        return "large"
    else:
        return "oversized"


# Export all constants
__all__ = [
    "PROJECT_ROOT",
    "FIXTURES_DIR",
    "DATA_DIR",
    "EXPECTED_DIR",
    "FILE_SIZE_LIMITS",
    "MAGIC_BYTES",
    "SECURITY_CONSTANTS",
    "PERFORMANCE_THRESHOLDS",
    "TEST_DATA_PATTERNS",
    "ERROR_MESSAGES",
    "JSON_OUTPUT_CONFIG",
    "TEST_FILE_METADATA",
    "VALIDATION_RULES",
    "DOCKER_CONSTANTS",
    "CLI_CONSTANTS",
    "get_test_file_path",
    "get_expected_output_path",
    "get_file_size_category",
]