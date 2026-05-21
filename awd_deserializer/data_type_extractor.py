"""
Module: data_type_extractor - DataTypeDefinitionExtractor for AWD data type schemas

Summary:
    Extracts data type definitions and XML schemas from AWD .design files.
    Generates XML schema files for data types, structures, and field definitions
    with comprehensive metadata and dependency tracking for external consumption.

Key Components:
    - DataTypeDefinitionExtractor: Main extractor for data type definitions
    - XMLSchemaGenerator: Generates XML schema definitions
    - FieldAnalyzer: Analyzes field structures and relationships
    - DependencyTracker: Tracks data type dependencies and references

Keywords: data, type, definition, extractor, xml, schema, structures, fields,
         dependencies, metadata, awd, process, validation, external
Dependencies: xml, asyncio, typing, pathlib, logging, re, xml.etree.ElementTree
Security: XML parsing security, schema validation, safe XML generation
Performance: Streaming XML generation, efficient parsing, memory optimization
"""

import logging
import re
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Any
from xml.dom import minidom

from .component_extractor import (
    ComponentExtractor,
    ComponentMetadata,
    ExtractionContext,
    ExtractionResult,
)

logger = logging.getLogger(__name__)


class FieldAnalyzer:
    """Analyzes field structures and data type relationships."""

    def __init__(self):
        self.field_patterns = [
            r'<field[^>]*name\s*=\s*["\']([^"\']+)["\'][^>]*type\s*=\s*["\']([^"\']+)["\']',
            r'"fieldName"\s*:\s*"([^"]+)".*?"fieldType"\s*:\s*"([^"]+)"',
            r'field\s+(\w+)\s*:\s*(\w+)',
            r'(\w+)\s+(\w+);'  # Java-style field declarations
        ]

        self.type_mappings = {
            'string': 'xs:string',
            'int': 'xs:int',
            'integer': 'xs:int',
            'long': 'xs:long',
            'double': 'xs:double',
            'float': 'xs:float',
            'boolean': 'xs:boolean',
            'date': 'xs:date',
            'datetime': 'xs:dateTime',
            'timestamp': 'xs:dateTime',
            'decimal': 'xs:decimal',
            'byte': 'xs:byte',
            'short': 'xs:short'
        }

    def extract_fields(self, content: str, context_name: str) -> list[dict[str, Any]]:
        """Extract field definitions from content."""
        fields = []
        field_names = set()  # Avoid duplicates

        try:
            for pattern in self.field_patterns:
                matches = re.findall(pattern, content, re.IGNORECASE | re.MULTILINE)

                for match in matches:
                    if len(match) >= 2:
                        field_name, field_type = match[0], match[1]

                        if field_name not in field_names:
                            field_names.add(field_name)

                            field_info = {
                                "name": field_name.strip(),
                                "type": self._normalize_type(field_type.strip()),
                                "original_type": field_type.strip(),
                                "context": context_name,
                                "required": self._is_field_required(content, field_name),
                                "constraints": self._extract_field_constraints(content, field_name)
                            }
                            fields.append(field_info)

            # Look for complex type references
            complex_fields = self._extract_complex_fields(content, context_name)
            for field in complex_fields:
                if field["name"] not in field_names:
                    fields.append(field)
                    field_names.add(field["name"])

            logger.debug(f"Extracted {len(fields)} fields for context {context_name}")

        except Exception as e:
            logger.error(f"Error extracting fields for {context_name}: {e}")

        return fields

    def _normalize_type(self, field_type: str) -> str:
        """Normalize field type to XML Schema type."""
        type_lower = field_type.lower()
        return self.type_mappings.get(type_lower, 'xs:string')

    def _is_field_required(self, content: str, field_name: str) -> bool:
        """Determine if a field is required based on context."""
        # Look for required indicators near the field definition
        required_patterns = [
            rf'{field_name}[^,}}]*required["\s]*:\s*true',
            rf'{field_name}[^,}}]*mandatory["\s]*:\s*true',
            rf'required.*{field_name}',
            rf'{field_name}.*\*'  # Asterisk indicating required
        ]

        for pattern in required_patterns:
            if re.search(pattern, content, re.IGNORECASE):
                return True

        return False

    def _extract_field_constraints(self, content: str, field_name: str) -> dict[str, Any]:
        """Extract field constraints like length, range, etc."""
        constraints = {}

        try:
            # Look for length constraints
            length_pattern = rf'{field_name}[^,}}]*(?:length|maxLength)["\s]*:\s*(\d+)'
            length_match = re.search(length_pattern, content, re.IGNORECASE)
            if length_match:
                constraints["maxLength"] = int(length_match.group(1))

            # Look for minimum/maximum values
            min_pattern = rf'{field_name}[^,}}]*(?:min|minimum)["\s]*:\s*(\d+)'
            min_match = re.search(min_pattern, content, re.IGNORECASE)
            if min_match:
                constraints["minimum"] = int(min_match.group(1))

            max_pattern = rf'{field_name}[^,}}]*(?:max|maximum)["\s]*:\s*(\d+)'
            max_match = re.search(max_pattern, content, re.IGNORECASE)
            if max_match:
                constraints["maximum"] = int(max_match.group(1))

            # Look for pattern constraints
            pattern_pattern = rf'{field_name}[^,}}]*pattern["\s]*:\s*["\']([^"\']+)["\']'
            pattern_match = re.search(pattern_pattern, content, re.IGNORECASE)
            if pattern_match:
                constraints["pattern"] = pattern_match.group(1)

        except Exception as e:
            logger.debug(f"Error extracting constraints for {field_name}: {e}")

        return constraints

    def _extract_complex_fields(self, content: str, context_name: str) -> list[dict[str, Any]]:
        """Extract complex field types like arrays and objects."""
        complex_fields = []

        try:
            # Look for array fields
            array_patterns = [
                r'(\w+)\[\]',  # Java-style arrays
                r'List<(\w+)>',  # Generic lists
                r'Array<(\w+)>',  # Explicit arrays
                r'"([^"]+)"\s*:\s*\[',  # JSON array patterns
            ]

            for pattern in array_patterns:
                matches = re.findall(pattern, content, re.IGNORECASE)
                for match in matches:
                    field_name = match if isinstance(match, str) else match[0]
                    complex_fields.append({
                        "name": f"{field_name}_array",
                        "type": "xs:array",
                        "original_type": f"Array<{field_name}>",
                        "context": context_name,
                        "required": False,
                        "constraints": {"itemType": field_name}
                    })

            # Look for nested object references
            object_patterns = [
                r'(\w+)\s+(\w+)\s*{',  # Object declarations
                r'"([^"]+)"\s*:\s*{',  # JSON object patterns
            ]

            for pattern in object_patterns:
                matches = re.findall(pattern, content, re.IGNORECASE)
                for match in matches:
                    if isinstance(match, tuple) and len(match) >= 2:
                        object_type, object_name = match[0], match[1]
                    else:
                        object_name = match
                        object_type = "Object"

                    complex_fields.append({
                        "name": object_name,
                        "type": "xs:complexType",
                        "original_type": object_type,
                        "context": context_name,
                        "required": False,
                        "constraints": {"complexType": object_type}
                    })

        except Exception as e:
            logger.debug(f"Error extracting complex fields: {e}")

        return complex_fields


class DependencyTracker:
    """Tracks data type dependencies and references."""

    def __init__(self):
        self.dependencies: dict[str, set[str]] = {}
        self.reverse_dependencies: dict[str, set[str]] = {}

    def add_dependency(self, from_type: str, to_type: str):
        """Add a dependency relationship."""
        if from_type not in self.dependencies:
            self.dependencies[from_type] = set()
        self.dependencies[from_type].add(to_type)

        if to_type not in self.reverse_dependencies:
            self.reverse_dependencies[to_type] = set()
        self.reverse_dependencies[to_type].add(from_type)

    def get_dependencies(self, type_name: str) -> list[str]:
        """Get direct dependencies for a type."""
        return list(self.dependencies.get(type_name, set()))

    def get_dependents(self, type_name: str) -> list[str]:
        """Get types that depend on this type."""
        return list(self.reverse_dependencies.get(type_name, set()))

    def get_dependency_order(self) -> list[str]:
        """Get types in dependency order (dependencies first)."""
        visited = set()
        result = []

        def visit(type_name: str):
            if type_name in visited:
                return
            visited.add(type_name)

            # Visit dependencies first
            for dep in self.dependencies.get(type_name, set()):
                visit(dep)

            result.append(type_name)

        # Visit all types
        for type_name in self.dependencies.keys():
            visit(type_name)

        return result


class XMLSchemaGenerator:
    """Generates XML schema definitions from extracted data types."""

    def __init__(self):
        self.namespace_uri = "http://www.dstawd.com/schema"
        self.target_namespace = "http://www.dstawd.com/datatypes"

    def generate_schema(self,
                       data_types: dict[str, list[dict[str, Any]]],
                       dependencies: DependencyTracker) -> str:
        """Generate XML schema from data types."""

        # Create schema root element
        schema = ET.Element("xs:schema")
        schema.set("xmlns:xs", "http://www.w3.org/2001/XMLSchema")
        schema.set("targetNamespace", self.target_namespace)
        schema.set("xmlns:tns", self.target_namespace)
        schema.set("elementFormDefault", "qualified")

        # Add schema documentation
        annotation = ET.SubElement(schema, "xs:annotation")
        documentation = ET.SubElement(annotation, "xs:documentation")
        documentation.text = "AWD Data Type Definitions extracted from .design file"

        # Generate complex types for each data type
        dependency_order = dependencies.get_dependency_order()

        for type_name in dependency_order:
            if type_name in data_types:
                complex_type = self._generate_complex_type(type_name, data_types[type_name])
                schema.append(complex_type)

        # Add any remaining types not in dependency order
        for type_name, fields in data_types.items():
            if type_name not in dependency_order:
                complex_type = self._generate_complex_type(type_name, fields)
                schema.append(complex_type)

        # Generate root elements for each type
        for type_name in data_types:
            element = ET.SubElement(schema, "xs:element")
            element.set("name", type_name)
            element.set("type", f"tns:{type_name}Type")

        return self._format_xml(schema)

    def _generate_complex_type(self, type_name: str, fields: list[dict[str, Any]]) -> ET.Element:
        """Generate complex type definition for a data type."""

        complex_type = ET.Element("xs:complexType")
        complex_type.set("name", f"{type_name}Type")

        # Add type documentation
        annotation = ET.SubElement(complex_type, "xs:annotation")
        documentation = ET.SubElement(annotation, "xs:documentation")
        documentation.text = f"Data type definition for {type_name}"

        if fields:
            sequence = ET.SubElement(complex_type, "xs:sequence")

            for field in fields:
                element = ET.SubElement(sequence, "xs:element")
                element.set("name", field["name"])
                element.set("type", field["type"])

                # Set occurrence constraints
                if not field.get("required", False):
                    element.set("minOccurs", "0")

                # Add field constraints
                constraints = field.get("constraints", {})
                if constraints:
                    self._add_field_constraints(element, constraints)

                # Add field documentation
                if field.get("original_type"):
                    field_annotation = ET.SubElement(element, "xs:annotation")
                    field_doc = ET.SubElement(field_annotation, "xs:documentation")
                    field_doc.text = f"Original type: {field['original_type']}"

        return complex_type

    def _add_field_constraints(self, element: ET.Element, constraints: dict[str, Any]):
        """Add field constraints to element."""

        if constraints.get("maxLength"):
            restriction = ET.SubElement(element, "xs:simpleType")
            restriction_base = ET.SubElement(restriction, "xs:restriction")
            restriction_base.set("base", "xs:string")

            max_length = ET.SubElement(restriction_base, "xs:maxLength")
            max_length.set("value", str(constraints["maxLength"]))

        if constraints.get("pattern"):
            if element.find("xs:simpleType") is None:
                restriction = ET.SubElement(element, "xs:simpleType")
                restriction_base = ET.SubElement(restriction, "xs:restriction")
                restriction_base.set("base", "xs:string")
            else:
                restriction_base = element.find("xs:simpleType/xs:restriction")

            pattern = ET.SubElement(restriction_base, "xs:pattern")
            pattern.set("value", constraints["pattern"])

        if constraints.get("minimum") is not None or constraints.get("maximum") is not None:
            if element.find("xs:simpleType") is None:
                restriction = ET.SubElement(element, "xs:simpleType")
                restriction_base = ET.SubElement(restriction, "xs:restriction")
                restriction_base.set("base", "xs:int")
            else:
                restriction_base = element.find("xs:simpleType/xs:restriction")

            if constraints.get("minimum") is not None:
                min_inclusive = ET.SubElement(restriction_base, "xs:minInclusive")
                min_inclusive.set("value", str(constraints["minimum"]))

            if constraints.get("maximum") is not None:
                max_inclusive = ET.SubElement(restriction_base, "xs:maxInclusive")
                max_inclusive.set("value", str(constraints["maximum"]))

    def _format_xml(self, element: ET.Element) -> str:
        """Format XML element as pretty-printed string."""
        rough_string = ET.tostring(element, encoding='unicode')
        reparsed = minidom.parseString(rough_string)
        return reparsed.toprettyxml(indent="  ", encoding=None)


class DataTypeDefinitionExtractor(ComponentExtractor):
    """
    Extractor for AWD data type definitions and XML schemas.
    
    Generates XML schema files for data types, structures, and field definitions
    with comprehensive metadata and dependency tracking.
    """

    def __init__(self):
        super().__init__("DataTypeDefinitionExtractor")
        self.field_analyzer = FieldAnalyzer()
        self.dependency_tracker = DependencyTracker()
        self.schema_generator = XMLSchemaGenerator()

    @property
    def component_type(self) -> str:
        return "data_types"

    @property
    def supported_formats(self) -> list[str]:
        return ["xml", "xsd"]

    async def extract_components(self,
                               context: ExtractionContext,
                               processed_data: dict[str, Any]) -> ExtractionResult:
        """Extract data type definition components from AWD data."""

        files_generated = []
        extraction_errors = []

        try:
            # Create data_types output directory
            data_types_dir = Path(context.output_directory) / "data_types"
            self._safe_create_directory(str(data_types_dir))

            # Extract data types from processed data
            data_types = await self._extract_data_types(processed_data, context)

            if data_types:
                # Generate individual XML files for each data type
                for type_name, fields in data_types.items():
                    if fields:  # Only generate files for types with fields
                        xml_file = data_types_dir / f"{type_name}.xml"
                        xml_content = self._generate_individual_schema(type_name, fields)
                        self._safe_write_file(str(xml_file), xml_content)
                        files_generated.append(str(xml_file))

                # Generate comprehensive schema file
                if len(data_types) > 1:
                    schema_file = data_types_dir / "comprehensive_schema.xsd"
                    schema_content = self.schema_generator.generate_schema(
                        data_types, self.dependency_tracker
                    )
                    self._safe_write_file(str(schema_file), schema_content)
                    files_generated.append(str(schema_file))

                logger.info(f"Generated {len(files_generated)} data type definition files")
            else:
                logger.warning("No data types found in processed_data")
                extraction_errors.append("No data type definitions found for extraction")

            # Create metadata
            metadata = self._create_component_metadata(context, files_generated)
            metadata.validation_status = "extracted"
            metadata.dependencies = list(self.dependency_tracker.dependencies.keys())

            return ExtractionResult(
                extractor_name=self.extractor_name,
                component_type=self.component_type,
                success=len(files_generated) > 0,
                files_generated=files_generated,
                metadata=metadata,
                processing_time_ms=0.0,  # Set by monitoring
                errors=extraction_errors
            )

        except Exception as e:
            logger.error(f"Data type extraction failed: {e}")

            metadata = ComponentMetadata(
                component_type=self.component_type,
                source_location=context.source_file,
                extraction_time=0.0,
                file_size_bytes=0,
                validation_status="error"
            )

            return ExtractionResult(
                extractor_name=self.extractor_name,
                component_type=self.component_type,
                success=False,
                files_generated=[],
                metadata=metadata,
                processing_time_ms=0.0,
                errors=[f"Data type extraction failed: {str(e)}"]
            )

    async def _extract_data_types(self,
                                processed_data: dict[str, Any],
                                context: ExtractionContext) -> dict[str, list[dict[str, Any]]]:
        """Extract data type definitions from processed AWD data."""
        data_types = {}

        try:
            # Convert processed_data to string for analysis
            content = self._serialize_for_analysis(processed_data)

            # Extract from AWD structure
            awd_structure = processed_data.get("awdStructure", {})
            if awd_structure:
                awd_types = await self._extract_from_awd_structure(awd_structure)
                data_types.update(awd_types)

            # Extract from business metadata
            business_metadata = processed_data.get("businessMetadata", {})
            if business_metadata:
                business_types = await self._extract_from_business_metadata(business_metadata)
                data_types.update(business_types)

            # Extract from general content patterns
            content_types = await self._extract_from_content_patterns(content)
            data_types.update(content_types)

            # Build dependency relationships
            self._build_dependencies(data_types)

            logger.debug(f"Extracted {len(data_types)} data type definitions")

        except Exception as e:
            logger.error(f"Error extracting data types: {e}")

        return data_types

    def _serialize_for_analysis(self, data: Any) -> str:
        """Serialize data for pattern analysis."""
        import json
        try:
            return json.dumps(data, indent=2, default=str)
        except Exception:
            return str(data)

    async def _extract_from_awd_structure(self,
                                        awd_structure: dict[str, Any]) -> dict[str, list[dict[str, Any]]]:
        """Extract data types from AWD structure."""
        data_types = {}

        try:
            objects = awd_structure.get("objects", [])

            for i, obj in enumerate(objects):
                if isinstance(obj, dict):
                    obj_content = self._serialize_for_analysis(obj)

                    # Determine object type name
                    type_name = (obj.get("type") or
                               obj.get("className") or
                               obj.get("_class") or
                               f"DataType_{i}")

                    # Extract fields from this object
                    fields = self.field_analyzer.extract_fields(obj_content, type_name)

                    if fields:
                        data_types[type_name] = fields

        except Exception as e:
            logger.error(f"Error extracting from AWD structure: {e}")

        return data_types

    async def _extract_from_business_metadata(self,
                                            business_metadata: dict[str, Any]) -> dict[str, list[dict[str, Any]]]:
        """Extract data types from business metadata."""
        data_types = {}

        try:
            # Look for data type definitions in business metadata
            if "dataTypes" in business_metadata:
                for dt in business_metadata["dataTypes"]:
                    if isinstance(dt, dict):
                        type_name = dt.get("name", f"BusinessDataType_{len(data_types)}")

                        # Extract fields from data type definition
                        dt_content = self._serialize_for_analysis(dt)
                        fields = self.field_analyzer.extract_fields(dt_content, type_name)

                        if fields:
                            data_types[type_name] = fields

            # Look for form data types
            if "forms" in business_metadata:
                for form in business_metadata["forms"]:
                    if isinstance(form, dict):
                        form_name = form.get("name", f"FormDataType_{len(data_types)}")

                        # Extract field definitions from form
                        form_content = self._serialize_for_analysis(form)
                        fields = self.field_analyzer.extract_fields(form_content, form_name)

                        if fields:
                            data_types[form_name] = fields

        except Exception as e:
            logger.error(f"Error extracting from business metadata: {e}")

        return data_types

    async def _extract_from_content_patterns(self, content: str) -> dict[str, list[dict[str, Any]]]:
        """Extract data types from general content patterns."""
        data_types = {}

        try:
            # Look for class-like structures
            class_patterns = [
                r'class\s+(\w+)\s*{([^}]+)}',
                r'interface\s+(\w+)\s*{([^}]+)}',
                r'struct\s+(\w+)\s*{([^}]+)}',
                r'type\s+(\w+)\s*=\s*{([^}]+)}'
            ]

            for pattern in class_patterns:
                matches = re.findall(pattern, content, re.IGNORECASE | re.MULTILINE | re.DOTALL)

                for class_name, class_body in matches:
                    fields = self.field_analyzer.extract_fields(class_body, class_name)
                    if fields:
                        data_types[class_name] = fields

            # Look for JSON schema-like structures
            json_schema_pattern = r'"([^"]+)"\s*:\s*{\s*"type"\s*:\s*"object"[^}]*"properties"\s*:\s*{([^}]+)}'
            json_matches = re.findall(json_schema_pattern, content, re.IGNORECASE | re.DOTALL)

            for schema_name, properties in json_matches:
                fields = self.field_analyzer.extract_fields(properties, schema_name)
                if fields:
                    data_types[f"{schema_name}Schema"] = fields

        except Exception as e:
            logger.error(f"Error extracting from content patterns: {e}")

        return data_types

    def _build_dependencies(self, data_types: dict[str, list[dict[str, Any]]]):
        """Build dependency relationships between data types."""

        try:
            # Clear existing dependencies
            self.dependency_tracker = DependencyTracker()

            for type_name, fields in data_types.items():
                for field in fields:
                    field_type = field.get("original_type", field.get("type", ""))

                    # Check if field type references another data type
                    for other_type in data_types:
                        if other_type != type_name and other_type in field_type:
                            self.dependency_tracker.add_dependency(type_name, other_type)

                    # Check for complex type references in constraints
                    constraints = field.get("constraints", {})
                    if "complexType" in constraints:
                        complex_type = constraints["complexType"]
                        if complex_type in data_types:
                            self.dependency_tracker.add_dependency(type_name, complex_type)

        except Exception as e:
            logger.error(f"Error building dependencies: {e}")

    def _generate_individual_schema(self, type_name: str, fields: list[dict[str, Any]]) -> str:
        """Generate individual XML schema for a single data type."""

        # Create simple schema for individual type
        schema = ET.Element("xs:schema")
        schema.set("xmlns:xs", "http://www.w3.org/2001/XMLSchema")
        schema.set("targetNamespace", self.schema_generator.target_namespace)
        schema.set("xmlns:tns", self.schema_generator.target_namespace)
        schema.set("elementFormDefault", "qualified")

        # Add schema documentation
        annotation = ET.SubElement(schema, "xs:annotation")
        documentation = ET.SubElement(annotation, "xs:documentation")
        documentation.text = f"Data type definition for {type_name}"

        # Generate complex type
        complex_type = self.schema_generator._generate_complex_type(type_name, fields)
        schema.append(complex_type)

        # Generate root element
        element = ET.SubElement(schema, "xs:element")
        element.set("name", type_name)
        element.set("type", f"tns:{type_name}Type")

        return self.schema_generator._format_xml(schema)


# Export the extractor
__all__ = ['DataTypeDefinitionExtractor']
