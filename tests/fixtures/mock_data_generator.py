"""
Module: mock_data_generator - Generate test data for Chorus Deserializer testing

Summary:
    Provides utilities to generate various test data scenarios including valid and
    invalid .design files, edge cases, and security test cases. Supports both
    Java serialization format generation and JSON output creation.

Key Components:
    - generate_java_serialized_object(): Create Java serialized objects
    - generate_design_file(): Generate complete .design files
    - generate_malformed_data(): Create invalid test cases
    - generate_security_payloads(): Security testing payloads
    - generate_json_output(): Expected JSON outputs

Keywords: mock, data, generator, test, fixtures, serialization, java, design,
          security, payloads, malformed, edge, cases, chorus, deserializer
Dependencies: struct, io, json, random, test_constants
Security: Generates security test payloads for vulnerability testing
Performance: Supports generation of large datasets for performance testing
"""

import io
import json
import struct
import random
import string
from datetime import datetime, timezone
from typing import Any, Dict, List, Optional, Union, Tuple
from pathlib import Path

from .test_constants import (
    MAGIC_BYTES,
    SECURITY_CONSTANTS,
    TEST_DATA_PATTERNS,
    FILE_SIZE_LIMITS,
    JSON_OUTPUT_CONFIG,
)


class JavaSerializationBuilder:
    """Builder for creating Java serialization streams."""
    
    # Java serialization protocol constants
    STREAM_MAGIC = 0xACED
    STREAM_VERSION = 0x0005
    
    # Type codes
    TC_NULL = 0x70
    TC_REFERENCE = 0x71
    TC_CLASSDESC = 0x72
    TC_OBJECT = 0x73
    TC_STRING = 0x74
    TC_ARRAY = 0x75
    TC_CLASS = 0x76
    TC_BLOCKDATA = 0x77
    TC_ENDBLOCKDATA = 0x78
    TC_RESET = 0x79
    TC_BLOCKDATALONG = 0x7A
    TC_EXCEPTION = 0x7B
    TC_LONGSTRING = 0x7C
    TC_PROXYCLASSDESC = 0x7D
    TC_ENUM = 0x7E
    
    # Object flags
    SC_WRITE_METHOD = 0x01
    SC_SERIALIZABLE = 0x02
    SC_EXTERNALIZABLE = 0x04
    SC_BLOCK_DATA = 0x08
    SC_ENUM = 0x10
    
    def __init__(self):
        self.stream = io.BytesIO()
        self.references = []
        
    def write_header(self) -> 'JavaSerializationBuilder':
        """Write Java serialization stream header."""
        self.stream.write(struct.pack('>HH', self.STREAM_MAGIC, self.STREAM_VERSION))
        return self
        
    def write_string(self, value: str) -> 'JavaSerializationBuilder':
        """Write a string to the stream."""
        encoded = value.encode('utf-8')
        if len(encoded) <= 65535:
            self.stream.write(bytes([self.TC_STRING]))
            self.stream.write(struct.pack('>H', len(encoded)))
        else:
            self.stream.write(bytes([self.TC_LONGSTRING]))
            self.stream.write(struct.pack('>Q', len(encoded)))
        self.stream.write(encoded)
        return self
        
    def write_object_start(self, class_name: str) -> 'JavaSerializationBuilder':
        """Start writing an object."""
        self.stream.write(bytes([self.TC_OBJECT]))
        self.write_class_desc(class_name)
        return self
        
    def write_class_desc(self, class_name: str) -> 'JavaSerializationBuilder':
        """Write a class descriptor."""
        self.stream.write(bytes([self.TC_CLASSDESC]))
        self.write_string(class_name)
        # Serial version UID (random for testing)
        self.stream.write(struct.pack('>Q', random.randint(1, 2**63-1)))
        # Flags
        self.stream.write(bytes([self.SC_SERIALIZABLE]))
        # Field count
        self.stream.write(struct.pack('>H', 0))
        # End block data
        self.stream.write(bytes([self.TC_ENDBLOCKDATA]))
        # Super class (null)
        self.stream.write(bytes([self.TC_NULL]))
        return self
        
    def write_null(self) -> 'JavaSerializationBuilder':
        """Write null reference."""
        self.stream.write(bytes([self.TC_NULL]))
        return self
        
    def build(self) -> bytes:
        """Build the final serialized data."""
        return self.stream.getvalue()


def generate_simple_design_object() -> bytes:
    """Generate a simple design object in Java serialization format."""
    builder = JavaSerializationBuilder()
    return (builder
            .write_header()
            .write_object_start("com.chorus.model.SimpleDesign")
            .build())


def generate_complex_design_object(size_category: str = "medium") -> bytes:
    """Generate a complex design object with nested structures."""
    builder = JavaSerializationBuilder()
    builder.write_header()
    
    # Main object
    builder.write_object_start("com.chorus.model.ComplexDesign")
    
    # Add nested objects based on size
    if size_category == "small":
        iterations = 1
    elif size_category == "medium":
        iterations = 10
    else:  # large
        iterations = 100
        
    for i in range(iterations):
        builder.write_object_start(f"com.chorus.model.Component{i}")
        
    return builder.build()


def generate_malformed_data(malformation_type: str) -> bytes:
    """Generate malformed data for error testing."""
    malformations = {
        "invalid_magic": b"\xDE\xAD\xBE\xEF" + b"\x00" * 100,
        "truncated_header": b"\xAC\xED",  # Incomplete header
        "invalid_version": b"\xAC\xED\xFF\xFF",  # Invalid version
        "corrupted_stream": MAGIC_BYTES["valid_java"] + b"\xFF" * 100,
        "empty_file": b"",
        "text_file": b"This is not a valid .design file\n",
    }
    
    return malformations.get(malformation_type, malformations["invalid_magic"])


def generate_security_payload(attack_type: str) -> bytes:
    """Generate security test payloads."""
    builder = JavaSerializationBuilder()
    builder.write_header()
    
    if attack_type == "runtime_exec":
        # Attempt to use Runtime class
        builder.write_object_start("java.lang.Runtime")
    elif attack_type == "process_builder":
        # Attempt to use ProcessBuilder
        builder.write_object_start("java.lang.ProcessBuilder")
    elif attack_type == "url_classloader":
        # Attempt to use URLClassLoader
        builder.write_object_start("java.net.URLClassLoader")
    elif attack_type == "nested_overflow":
        # Deep nesting to cause stack overflow
        for i in range(1000):
            builder.write_object_start(f"com.chorus.model.Nested{i}")
    elif attack_type == "large_array":
        # Extremely large array
        builder.stream.write(bytes([builder.TC_ARRAY]))
        builder.write_class_desc("[Ljava.lang.String;")
        builder.stream.write(struct.pack('>I', 2**30))  # 1 billion elements
        
    return builder.build()


def generate_edge_case_data(case_type: str) -> bytes:
    """Generate edge case test data."""
    builder = JavaSerializationBuilder()
    builder.write_header()
    
    if case_type == "unicode_stress":
        # Unicode stress test
        builder.write_object_start("com.chorus.model.UnicodeTest")
        builder.write_string("🎭🎨🎪🎬" * 100)  # Emoji stress
        builder.write_string("你好世界" * 100)  # Chinese characters
        builder.write_string("مرحبا بالعالم" * 100)  # Arabic (RTL)
    elif case_type == "null_heavy":
        # Many null references
        builder.write_object_start("com.chorus.model.NullTest")
        for _ in range(100):
            builder.write_null()
    elif case_type == "circular_reference":
        # Circular reference (simplified)
        builder.write_object_start("com.chorus.model.CircularA")
        builder.stream.write(bytes([builder.TC_REFERENCE]))
        builder.stream.write(struct.pack('>I', 0x7E0000))  # Back reference
    elif case_type == "boundary_values":
        # Boundary value testing
        builder.write_object_start("com.chorus.model.BoundaryTest")
        builder.write_string("")  # Empty string
        builder.write_string("A" * 65535)  # Max short string
        
    return builder.build()


def generate_design_file(
    filename: str,
    size_category: str,
    content_type: str = "valid"
) -> bytes:
    """Generate a complete .design file."""
    
    if content_type == "valid":
        if size_category == "small":
            data = generate_simple_design_object()
        else:
            data = generate_complex_design_object(size_category)
            
        # Pad to target size
        target_size = FILE_SIZE_LIMITS[size_category]
        if len(data) < target_size:
            # Add padding data (as serialized strings)
            builder = JavaSerializationBuilder()
            builder.stream.write(data)
            
            while len(builder.stream.getvalue()) < target_size:
                padding = "X" * min(1000, target_size - len(builder.stream.getvalue()))
                builder.write_string(padding)
                
            data = builder.build()
            
    elif content_type == "malformed":
        data = generate_malformed_data("invalid_magic")
    elif content_type == "security":
        data = generate_security_payload("runtime_exec")
    elif content_type == "edge":
        data = generate_edge_case_data("unicode_stress")
    else:
        data = b""
        
    return data[:FILE_SIZE_LIMITS.get(size_category, len(data))]


def generate_json_output(
    design_type: str,
    object_count: int = 1
) -> Dict[str, Any]:
    """Generate expected JSON output for a design file."""
    
    base_structure = {
        "metadata": {
            "version": "1.0.0",
            "created": datetime.now(timezone.utc).isoformat(),
            "object_count": object_count,
            "file_type": "design",
        },
        "objects": []
    }
    
    # Generate objects based on type
    for i in range(object_count):
        if design_type == "simple":
            obj = {
                "type": "SimpleDesign",
                "id": f"simple_{i}",
                "name": f"Simple Design {i}",
                "version": "1.0",
                "created": datetime.now(timezone.utc).isoformat(),
            }
        elif design_type == "complex":
            obj = {
                "type": "ComplexDesign",
                "id": f"complex_{i}",
                "name": f"Complex Design {i}",
                "components": [
                    {
                        "id": f"comp_{i}_{j}",
                        "name": f"Component {j}",
                        "properties": {
                            "key1": "value1",
                            "key2": j,
                            "key3": j % 2 == 0,
                        }
                    } for j in range(5)
                ],
                "metadata": {
                    "tags": ["test", "complex", f"item_{i}"],
                    "attributes": {
                        "complexity": "high",
                        "index": i,
                    }
                }
            }
        elif design_type == "array":
            obj = {
                "type": "ArrayDesign",
                "id": f"array_{i}",
                "arrays": {
                    "integers": list(range(100)),
                    "strings": [f"item_{j}" for j in range(50)],
                    "mixed": [1, "two", 3.0, True, None] * 10,
                }
            }
        elif design_type == "map":
            obj = {
                "type": "MapDesign",
                "id": f"map_{i}",
                "properties": {
                    f"key_{j}": f"value_{j}" for j in range(100)
                }
            }
        else:
            obj = {"type": "Unknown", "id": f"unknown_{i}"}
            
        base_structure["objects"].append(obj)
        
    return base_structure


def generate_batch_files(output_dir: Path, count: int = 10) -> List[Path]:
    """Generate a batch of test files for batch processing tests."""
    files = []
    
    for i in range(count):
        size = random.choice(["small", "medium"])
        filename = f"batch_test_{i:03d}.design"
        filepath = output_dir / filename
        
        data = generate_design_file(filename, size)
        filepath.write_bytes(data)
        files.append(filepath)
        
    return files


def generate_performance_test_data(
    test_type: str
) -> Union[bytes, List[bytes]]:
    """Generate data for performance testing."""
    
    if test_type == "throughput":
        # Generate files of varying sizes
        return [
            generate_design_file(f"perf_{size}.design", size)
            for size in ["small", "medium", "large"]
        ]
    elif test_type == "latency":
        # Single small file for latency testing
        return generate_design_file("latency.design", "small")
    elif test_type == "memory":
        # Large file to test memory usage
        return generate_design_file("memory.design", "large")
    elif test_type == "concurrent":
        # Multiple files for concurrent processing
        return [
            generate_design_file(f"concurrent_{i}.design", "medium")
            for i in range(10)
        ]
    else:
        return generate_simple_design_object()


# Utility functions for test file creation
def create_test_design_file(
    filepath: Path,
    size_category: str,
    content_type: str = "valid"
) -> None:
    """Create a test .design file at the specified path."""
    data = generate_design_file(filepath.name, size_category, content_type)
    filepath.write_bytes(data)


def create_expected_json_file(
    filepath: Path,
    design_type: str,
    object_count: int = 1
) -> None:
    """Create an expected JSON output file."""
    data = generate_json_output(design_type, object_count)
    filepath.write_text(
        json.dumps(data, **JSON_OUTPUT_CONFIG)
    )


def create_all_test_fixtures(
    data_dir: Path,
    output_dir: Path
) -> Dict[str, Path]:
    """Create all standard test fixtures."""
    fixtures = {}
    
    # Create standard test files
    test_files = [
        ("small.design", "small", "valid", "simple", 1),
        ("medium.design", "medium", "valid", "complex", 50),
        ("large.design", "large", "valid", "array", 1000),
        ("malformed.design", "small", "malformed", None, 0),
        ("edge_cases.design", "medium", "edge", "complex", 10),
    ]
    
    for filename, size, content_type, json_type, obj_count in test_files:
        # Create design file
        design_path = data_dir / filename
        create_test_design_file(design_path, size, content_type)
        fixtures[filename] = design_path
        
        # Create expected JSON (except for malformed)
        if json_type:
            json_path = output_dir / filename.replace(".design", ".json")
            create_expected_json_file(json_path, json_type, obj_count)
            fixtures[filename.replace(".design", ".json")] = json_path
            
    return fixtures


# Export main functions
__all__ = [
    "JavaSerializationBuilder",
    "generate_simple_design_object",
    "generate_complex_design_object",
    "generate_malformed_data",
    "generate_security_payload",
    "generate_edge_case_data",
    "generate_design_file",
    "generate_json_output",
    "generate_batch_files",
    "generate_performance_test_data",
    "create_test_design_file",
    "create_expected_json_file",
    "create_all_test_fixtures",
]