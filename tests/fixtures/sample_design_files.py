"""
Module: sample_design_files - Python fixtures for test design files

Summary:
    Provides pre-defined sample data and fixtures for testing the Chorus Deserializer.
    Contains various test scenarios including valid designs, edge cases, and error
    conditions to ensure comprehensive test coverage.

Key Components:
    - SAMPLE_DESIGNS: Dictionary of pre-defined design objects
    - get_sample_design(): Retrieve specific test designs
    - create_sample_file(): Create sample .design files
    - validate_sample(): Validate sample data integrity
    - get_test_scenarios(): Get complete test scenarios

Keywords: sample, design, files, fixtures, test, data, chorus, deserializer,
          validation, scenarios, edge, cases, mock, testing, python
Dependencies: pathlib, json, test_constants, mock_data_generator
Security: Includes security test samples for vulnerability testing
Performance: Provides performance test samples of varying sizes
"""

import json
from pathlib import Path
from typing import Dict, List, Any, Optional, Tuple
from datetime import datetime, timezone

from .test_constants import (
    DATA_DIR,
    EXPECTED_DIR,
    TEST_DATA_PATTERNS,
    SECURITY_CONSTANTS,
    ERROR_MESSAGES,
)
from .mock_data_generator import (
    generate_design_file,
    generate_json_output,
    create_test_design_file,
)


# Pre-defined sample designs
SAMPLE_DESIGNS = {
    "minimal": {
        "description": "Minimal valid design object",
        "class": "com.chorus.model.MinimalDesign",
        "data": {
            "id": "minimal_001",
            "type": "MinimalDesign",
        },
        "expected_json": {
            "id": "minimal_001",
            "type": "MinimalDesign",
        }
    },
    
    "simple": {
        "description": "Simple design with basic fields",
        "class": "com.chorus.model.SimpleDesign",
        "data": {
            "id": "simple_001",
            "name": "Simple Test Design",
            "version": "1.0.0",
            "created": "2025-01-01T00:00:00Z",
            "description": "A simple design for testing basic functionality",
        },
        "expected_json": {
            "id": "simple_001",
            "name": "Simple Test Design",
            "version": "1.0.0",
            "created": "2025-01-01T00:00:00Z",
            "description": "A simple design for testing basic functionality",
            "type": "SimpleDesign",
        }
    },
    
    "nested": {
        "description": "Design with nested objects",
        "class": "com.chorus.model.NestedDesign",
        "data": {
            "id": "nested_001",
            "name": "Nested Design",
            "metadata": {
                "author": "Test Author",
                "tags": ["test", "nested", "complex"],
                "properties": {
                    "level1": {
                        "level2": {
                            "level3": {
                                "value": "deeply nested"
                            }
                        }
                    }
                }
            },
            "components": [
                {"id": "comp_1", "name": "Component 1", "active": True},
                {"id": "comp_2", "name": "Component 2", "active": False},
            ]
        },
        "expected_json": {
            "id": "nested_001",
            "name": "Nested Design",
            "type": "NestedDesign",
            "metadata": {
                "author": "Test Author",
                "tags": ["test", "nested", "complex"],
                "properties": {
                    "level1": {
                        "level2": {
                            "level3": {
                                "value": "deeply nested"
                            }
                        }
                    }
                }
            },
            "components": [
                {"id": "comp_1", "name": "Component 1", "active": True},
                {"id": "comp_2", "name": "Component 2", "active": False},
            ]
        }
    },
    
    "array_heavy": {
        "description": "Design with large arrays",
        "class": "com.chorus.model.ArrayDesign",
        "data": {
            "id": "array_001",
            "name": "Array Heavy Design",
            "integers": list(range(1000)),
            "strings": [f"string_{i}" for i in range(500)],
            "floats": [i * 0.1 for i in range(100)],
            "booleans": [i % 2 == 0 for i in range(50)],
            "mixed": [1, "two", 3.0, True, None, {"key": "value"}] * 10,
        },
        "expected_json": {
            "id": "array_001",
            "name": "Array Heavy Design",
            "type": "ArrayDesign",
            "integers": list(range(1000)),
            "strings": [f"string_{i}" for i in range(500)],
            "floats": [i * 0.1 for i in range(100)],
            "booleans": [i % 2 == 0 for i in range(50)],
            "mixed": [1, "two", 3.0, True, None, {"key": "value"}] * 10,
        }
    },
    
    "map_heavy": {
        "description": "Design with large maps/dictionaries",
        "class": "com.chorus.model.MapDesign",
        "data": {
            "id": "map_001",
            "name": "Map Heavy Design",
            "properties": {f"key_{i}": f"value_{i}" for i in range(1000)},
            "nested_maps": {
                "level1": {
                    f"l1_key_{i}": {
                        "level2": {
                            f"l2_key_{j}": j * i
                            for j in range(10)
                        }
                    }
                    for i in range(10)
                }
            }
        },
        "expected_json": {
            "id": "map_001",
            "name": "Map Heavy Design",
            "type": "MapDesign",
            "properties": {f"key_{i}": f"value_{i}" for i in range(1000)},
            "nested_maps": {
                "level1": {
                    f"l1_key_{i}": {
                        "level2": {
                            f"l2_key_{j}": j * i
                            for j in range(10)
                        }
                    }
                    for i in range(10)
                }
            }
        }
    },
    
    "unicode": {
        "description": "Design with unicode and special characters",
        "class": "com.chorus.model.UnicodeDesign",
        "data": {
            "id": "unicode_001",
            "name": "Unicode Test Design 🎨",
            "descriptions": {
                "english": "Hello World",
                "chinese": "你好世界",
                "arabic": "مرحبا بالعالم",
                "russian": "Привет мир",
                "emoji": "🎭🎨🎪🎬🎯",
                "special": "!@#$%^&*()_+-=[]{}|;':\",./<>?",
            },
            "unicode_array": ["α", "β", "γ", "δ", "ε", "ζ", "η", "θ"],
        },
        "expected_json": {
            "id": "unicode_001",
            "name": "Unicode Test Design 🎨",
            "type": "UnicodeDesign",
            "descriptions": {
                "english": "Hello World",
                "chinese": "你好世界",
                "arabic": "مرحبا بالعالم",
                "russian": "Привет мир",
                "emoji": "🎭🎨🎪🎬🎯",
                "special": "!@#$%^&*()_+-=[]{}|;':\",./<>?",
            },
            "unicode_array": ["α", "β", "γ", "δ", "ε", "ζ", "η", "θ"],
        }
    },
    
    "null_values": {
        "description": "Design with null values",
        "class": "com.chorus.model.NullableDesign",
        "data": {
            "id": "null_001",
            "name": "Nullable Design",
            "optional_field": None,
            "array_with_nulls": [1, None, 3, None, 5],
            "nested_with_nulls": {
                "value": "present",
                "null_value": None,
                "nested": {
                    "deep_null": None
                }
            }
        },
        "expected_json": {
            "id": "null_001",
            "name": "Nullable Design",
            "type": "NullableDesign",
            "optional_field": None,
            "array_with_nulls": [1, None, 3, None, 5],
            "nested_with_nulls": {
                "value": "present",
                "null_value": None,
                "nested": {
                    "deep_null": None
                }
            }
        }
    },
    
    "empty_collections": {
        "description": "Design with empty collections",
        "class": "com.chorus.model.EmptyCollectionsDesign",
        "data": {
            "id": "empty_001",
            "name": "Empty Collections",
            "empty_array": [],
            "empty_map": {},
            "empty_string": "",
            "zero_value": 0,
            "false_value": False,
        },
        "expected_json": {
            "id": "empty_001",
            "name": "Empty Collections",
            "type": "EmptyCollectionsDesign",
            "empty_array": [],
            "empty_map": {},
            "empty_string": "",
            "zero_value": 0,
            "false_value": False,
        }
    },
    
    "large_numbers": {
        "description": "Design with large numeric values",
        "class": "com.chorus.model.NumericDesign",
        "data": {
            "id": "numeric_001",
            "name": "Numeric Boundaries",
            "max_int": 2147483647,  # Integer.MAX_VALUE
            "min_int": -2147483648,  # Integer.MIN_VALUE
            "max_long": 9223372036854775807,  # Long.MAX_VALUE
            "min_long": -9223372036854775808,  # Long.MIN_VALUE
            "max_float": 3.4028235e+38,  # Float.MAX_VALUE
            "min_float": 1.175494e-38,  # Float.MIN_VALUE
            "pi": 3.141592653589793,
            "euler": 2.718281828459045,
        },
        "expected_json": {
            "id": "numeric_001",
            "name": "Numeric Boundaries",
            "type": "NumericDesign",
            "max_int": 2147483647,
            "min_int": -2147483648,
            "max_long": 9223372036854775807,
            "min_long": -9223372036854775808,
            "max_float": 3.4028235e+38,
            "min_float": 1.175494e-38,
            "pi": 3.141592653589793,
            "euler": 2.718281828459045,
        }
    },
    
    "datetime_values": {
        "description": "Design with various datetime formats",
        "class": "com.chorus.model.DateTimeDesign",
        "data": {
            "id": "datetime_001",
            "name": "DateTime Test",
            "iso_datetime": "2025-01-01T12:00:00Z",
            "iso_with_millis": "2025-01-01T12:00:00.123Z",
            "iso_with_timezone": "2025-01-01T12:00:00+05:30",
            "epoch_seconds": 1735732800,
            "epoch_millis": 1735732800000,
            "date_only": "2025-01-01",
            "time_only": "12:00:00",
        },
        "expected_json": {
            "id": "datetime_001",
            "name": "DateTime Test",
            "type": "DateTimeDesign",
            "iso_datetime": "2025-01-01T12:00:00Z",
            "iso_with_millis": "2025-01-01T12:00:00.123Z",
            "iso_with_timezone": "2025-01-01T12:00:00+05:30",
            "epoch_seconds": 1735732800,
            "epoch_millis": 1735732800000,
            "date_only": "2025-01-01",
            "time_only": "12:00:00",
        }
    }
}


# Security test samples
SECURITY_SAMPLES = {
    "runtime_class": {
        "description": "Attempt to deserialize Runtime class",
        "class": "java.lang.Runtime",
        "should_fail": True,
        "expected_error": ERROR_MESSAGES["blocked_class"],
    },
    
    "process_builder": {
        "description": "Attempt to deserialize ProcessBuilder",
        "class": "java.lang.ProcessBuilder",
        "should_fail": True,
        "expected_error": ERROR_MESSAGES["blocked_class"],
    },
    
    "class_loader": {
        "description": "Attempt to deserialize URLClassLoader",
        "class": "java.net.URLClassLoader",
        "should_fail": True,
        "expected_error": ERROR_MESSAGES["blocked_class"],
    },
    
    "script_engine": {
        "description": "Attempt to deserialize ScriptEngineManager",
        "class": "javax.script.ScriptEngineManager",
        "should_fail": True,
        "expected_error": ERROR_MESSAGES["blocked_class"],
    },
}


# Test scenarios combining fixtures and expected outcomes
TEST_SCENARIOS = {
    "basic_functionality": {
        "description": "Test basic deserialization functionality",
        "fixtures": ["minimal", "simple", "nested"],
        "expected_success": True,
    },
    
    "complex_structures": {
        "description": "Test complex data structures",
        "fixtures": ["array_heavy", "map_heavy", "nested"],
        "expected_success": True,
    },
    
    "edge_cases": {
        "description": "Test edge cases and boundary conditions",
        "fixtures": ["unicode", "null_values", "empty_collections", "large_numbers"],
        "expected_success": True,
    },
    
    "security_validation": {
        "description": "Test security validation",
        "fixtures": list(SECURITY_SAMPLES.keys()),
        "expected_success": False,
    },
    
    "performance": {
        "description": "Test performance with large datasets",
        "fixtures": ["array_heavy", "map_heavy"],
        "expected_success": True,
        "performance_threshold_ms": 1000,
    },
}


def get_sample_design(name: str) -> Dict[str, Any]:
    """Get a specific sample design by name."""
    if name not in SAMPLE_DESIGNS:
        raise ValueError(f"Sample design '{name}' not found")
    return SAMPLE_DESIGNS[name].copy()


def get_security_sample(name: str) -> Dict[str, Any]:
    """Get a specific security test sample."""
    if name not in SECURITY_SAMPLES:
        raise ValueError(f"Security sample '{name}' not found")
    return SECURITY_SAMPLES[name].copy()


def create_sample_file(
    name: str,
    output_path: Path,
    include_json: bool = True
) -> Tuple[Path, Optional[Path]]:
    """Create a sample .design file and optionally its expected JSON output."""
    if name not in SAMPLE_DESIGNS:
        raise ValueError(f"Sample design '{name}' not found")
    
    sample = SAMPLE_DESIGNS[name]
    
    # Determine size based on data
    data_str = json.dumps(sample["data"])
    if len(data_str) < 1024:
        size = "small"
    elif len(data_str) < 102400:
        size = "medium"
    else:
        size = "large"
    
    # Create design file
    design_path = output_path / f"{name}.design"
    create_test_design_file(design_path, size, "valid")
    
    # Create JSON file if requested
    json_path = None
    if include_json and "expected_json" in sample:
        json_path = output_path / f"{name}.json"
        json_path.write_text(
            json.dumps(sample["expected_json"], indent=2, sort_keys=True)
        )
    
    return design_path, json_path


def validate_sample(name: str) -> bool:
    """Validate that a sample has all required fields."""
    if name not in SAMPLE_DESIGNS:
        return False
    
    sample = SAMPLE_DESIGNS[name]
    required_fields = ["description", "class", "data", "expected_json"]
    
    return all(field in sample for field in required_fields)


def get_test_scenario(scenario_name: str) -> Dict[str, Any]:
    """Get a complete test scenario with fixtures and expectations."""
    if scenario_name not in TEST_SCENARIOS:
        raise ValueError(f"Test scenario '{scenario_name}' not found")
    
    scenario = TEST_SCENARIOS[scenario_name].copy()
    
    # Expand fixture references
    fixtures = []
    for fixture_name in scenario["fixtures"]:
        if fixture_name in SAMPLE_DESIGNS:
            fixtures.append(SAMPLE_DESIGNS[fixture_name])
        elif fixture_name in SECURITY_SAMPLES:
            fixtures.append(SECURITY_SAMPLES[fixture_name])
    
    scenario["expanded_fixtures"] = fixtures
    return scenario


def list_all_samples() -> Dict[str, List[str]]:
    """List all available samples by category."""
    return {
        "normal": list(SAMPLE_DESIGNS.keys()),
        "security": list(SECURITY_SAMPLES.keys()),
        "scenarios": list(TEST_SCENARIOS.keys()),
    }


def create_all_samples(output_dir: Path) -> Dict[str, Path]:
    """Create all sample files in the specified directory."""
    created_files = {}
    
    # Create normal samples
    for name in SAMPLE_DESIGNS:
        design_path, json_path = create_sample_file(name, output_dir)
        created_files[f"{name}.design"] = design_path
        if json_path:
            created_files[f"{name}.json"] = json_path
    
    return created_files


# Export main components
__all__ = [
    "SAMPLE_DESIGNS",
    "SECURITY_SAMPLES",
    "TEST_SCENARIOS",
    "get_sample_design",
    "get_security_sample",
    "create_sample_file",
    "validate_sample",
    "get_test_scenario",
    "list_all_samples",
    "create_all_samples",
]