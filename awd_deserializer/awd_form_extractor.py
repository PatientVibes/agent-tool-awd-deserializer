"""
Module: awd_form_extractor - AWDFormExtractor for AWD form definitions

Summary:
    Extracts AWD form definitions from .design files in both userScreen XML format
    and uxData JSON format. Generates dual-format form files for external consumption
    with comprehensive field mappings, validation rules, and UI metadata.

Key Components:
    - AWDFormExtractor: Main extractor for AWD form definitions
    - FormFieldAnalyzer: Analyzes form fields and UI components
    - XMLFormGenerator: Generates userScreen XML format
    - JSONFormGenerator: Generates uxData JSON format

Keywords: awd, form, extractor, userscreen, xml, json, ux, fields, validation,
         ui, components, metadata, dual, format, external, consumption
Dependencies: xml, json, asyncio, typing, pathlib, logging, re, xml.etree.ElementTree
Security: XML parsing security, form validation, safe content generation
Performance: Streaming generation, efficient form processing, memory optimization
"""

import re
import json
import asyncio
import xml.etree.ElementTree as ET
from typing import Dict, List, Any, Optional, Tuple, Set
from pathlib import Path
import logging
from xml.dom import minidom
import uuid

from .component_extractor import (
    ComponentExtractor,
    ExtractionContext,
    ExtractionResult,
    ComponentMetadata
)

logger = logging.getLogger(__name__)


class FormFieldAnalyzer:
    """Analyzes form fields and UI components from AWD content."""
    
    def __init__(self):
        self.field_patterns = [
            # XML-style field patterns
            r'<(?:field|input|checkbox|button|text)[^>]*name\s*=\s*["\']([^"\']+)["\'][^>]*(?:type\s*=\s*["\']([^"\']+)["\'])?[^>]*>',
            # JSON-style field patterns
            r'"(?:fieldName|name|id)"\s*:\s*"([^"]+)".*?"(?:fieldType|type)"\s*:\s*"([^"]+)"',
            # AWD-specific patterns
            r'name\s*[=:]\s*["\']([^"\']+)["\'].*?(?:type|class)\s*[=:]\s*["\']([^"\']+)["\']',
            # Form control patterns
            r'(\w+)(?:Box|Field|Input|Button)\d*_\d+.*?name["\s]*[=:]\s*["\']([^"\']+)["\']'
        ]
        
        self.ui_component_types = {
            'checkbox': 'boolean',
            'text': 'string',
            'input': 'string',
            'number': 'number',
            'date': 'date',
            'button': 'action',
            'select': 'choice',
            'radio': 'choice',
            'textarea': 'text'
        }
        
        self.awd_form_properties = [
            'screenName', 'screenType', 'formType', 'langID', 'screenFormat',
            'screenDesc', 'templateScreen', 'newForm', 'class', 'screenURL',
            'top', 'left', 'width', 'height', 'tabIndex', 'readOnly',
            'checkedValue', 'uncheckedValue', 'labelPosition', 'helpText'
        ]
    
    def extract_form_fields(self, content: str, form_name: str) -> List[Dict[str, Any]]:
        """Extract form field definitions from content."""
        fields = []
        field_names = set()  # Avoid duplicates
        
        try:
            for pattern in self.field_patterns:
                matches = re.findall(pattern, content, re.IGNORECASE | re.MULTILINE | re.DOTALL)
                
                for match in matches:
                    if len(match) >= 1:
                        field_name = match[0] if isinstance(match, tuple) else match
                        field_type = match[1] if len(match) > 1 and match[1] else 'string'
                        
                        if field_name not in field_names:
                            field_names.add(field_name)
                            
                            field_info = {
                                "name": field_name.strip(),
                                "type": self._normalize_field_type(field_type.strip()),
                                "original_type": field_type.strip(),
                                "form": form_name,
                                "ui_properties": self._extract_ui_properties(content, field_name),
                                "validation": self._extract_validation_rules(content, field_name)
                            }
                            fields.append(field_info)
            
            # Extract form groups and layouts
            groups = self._extract_form_groups(content, form_name)
            for group in groups:
                if group["name"] not in field_names:
                    fields.append(group)
                    field_names.add(group["name"])
            
            logger.debug(f"Extracted {len(fields)} form fields for {form_name}")
            
        except Exception as e:
            logger.error(f"Error extracting form fields for {form_name}: {e}")
        
        return fields
    
    def _normalize_field_type(self, field_type: str) -> str:
        """Normalize field type to standard form types."""
        type_lower = field_type.lower()
        
        # Direct mapping
        if type_lower in self.ui_component_types:
            return self.ui_component_types[type_lower]
        
        # Pattern-based mapping
        if 'check' in type_lower or 'bool' in type_lower:
            return 'boolean'
        elif 'num' in type_lower or 'int' in type_lower or 'float' in type_lower:
            return 'number'
        elif 'date' in type_lower or 'time' in type_lower:
            return 'date'
        elif 'button' in type_lower or 'submit' in type_lower:
            return 'action'
        elif 'select' in type_lower or 'option' in type_lower:
            return 'choice'
        else:
            return 'string'
    
    def _extract_ui_properties(self, content: str, field_name: str) -> Dict[str, Any]:
        """Extract UI properties for a specific field."""
        properties = {}
        
        try:
            # Find field section in content
            field_section_pattern = rf'{field_name}[^}}{{]*(?:top|left|width|height|tabIndex)[^}}{{]*'
            field_section = re.search(field_section_pattern, content, re.IGNORECASE | re.DOTALL)
            
            if field_section:
                section_content = field_section.group(0)
                
                # Extract UI positioning and styling properties
                for prop in self.awd_form_properties:
                    prop_pattern = rf'{prop}["\s]*[>\s:=]\s*["\']?([^"\'>\s]+)["\']?'
                    prop_match = re.search(prop_pattern, section_content, re.IGNORECASE)
                    
                    if prop_match:
                        value = prop_match.group(1)
                        # Try to convert numeric values
                        try:
                            if value.isdigit():
                                properties[prop] = int(value)
                            elif '.' in value and value.replace('.', '').isdigit():
                                properties[prop] = float(value)
                            else:
                                properties[prop] = value
                        except:
                            properties[prop] = value
            
            # Look for label and help text
            label_pattern = rf'(?:label|description)["\s]*[>=:]\s*["\']([^"\']+)["\'].*?{field_name}'
            label_match = re.search(label_pattern, content, re.IGNORECASE | re.DOTALL)
            if label_match:
                properties['label'] = label_match.group(1)
        
        except Exception as e:
            logger.debug(f"Error extracting UI properties for {field_name}: {e}")
        
        return properties
    
    def _extract_validation_rules(self, content: str, field_name: str) -> Dict[str, Any]:
        """Extract validation rules for a field."""
        validation = {}
        
        try:
            # Look for required indicators
            required_patterns = [
                rf'{field_name}[^}}]*required["\s]*[=:]\s*true',
                rf'{field_name}[^}}]*mandatory["\s]*[=:]\s*true',
                rf'required.*{field_name}',
                rf'{field_name}.*\*'  # Asterisk indicating required
            ]
            
            for pattern in required_patterns:
                if re.search(pattern, content, re.IGNORECASE):
                    validation['required'] = True
                    break
            
            # Look for length constraints
            length_pattern = rf'{field_name}[^}}]*(?:maxLength|length)["\s]*[=:]\s*(\d+)'
            length_match = re.search(length_pattern, content, re.IGNORECASE)
            if length_match:
                validation['maxLength'] = int(length_match.group(1))
            
            # Look for pattern validation
            pattern_pattern = rf'{field_name}[^}}]*pattern["\s]*[=:]\s*["\']([^"\']+)["\']'
            pattern_match = re.search(pattern_pattern, content, re.IGNORECASE)
            if pattern_match:
                validation['pattern'] = pattern_match.group(1)
            
            # Look for value constraints
            value_constraints = ['min', 'max', 'step']
            for constraint in value_constraints:
                constraint_pattern = rf'{field_name}[^}}]*{constraint}["\s]*[=:]\s*([^\s,}}]+)'
                constraint_match = re.search(constraint_pattern, content, re.IGNORECASE)
                if constraint_match:
                    try:
                        validation[constraint] = float(constraint_match.group(1))
                    except:
                        validation[constraint] = constraint_match.group(1)
        
        except Exception as e:
            logger.debug(f"Error extracting validation rules for {field_name}: {e}")
        
        return validation
    
    def _extract_form_groups(self, content: str, form_name: str) -> List[Dict[str, Any]]:
        """Extract form groups and layout containers."""
        groups = []
        
        try:
            # Look for group/container patterns
            group_patterns = [
                r'<group[^>]*label\s*=\s*["\']([^"\']+)["\'][^>]*>',
                r'"group"\s*:\s*{\s*"label"\s*:\s*"([^"]+)"',
                r'group["\s]*[=:]\s*["\']([^"\']+)["\']'
            ]
            
            for pattern in group_patterns:
                matches = re.findall(pattern, content, re.IGNORECASE | re.MULTILINE)
                
                for match in matches:
                    group_name = match if isinstance(match, str) else match[0]
                    
                    groups.append({
                        "name": f"group_{group_name}",
                        "type": "group",
                        "original_type": "group",
                        "form": form_name,
                        "ui_properties": {
                            "label": group_name,
                            "type": "container"
                        },
                        "validation": {}
                    })
        
        except Exception as e:
            logger.debug(f"Error extracting form groups: {e}")
        
        return groups


class XMLFormGenerator:
    """Generates userScreen XML format from form data."""
    
    def __init__(self):
        self.xml_namespace = "http://www.dstawd.com/userscreen"
    
    def generate_userscreen_xml(self, 
                               form_name: str, 
                               fields: List[Dict[str, Any]], 
                               form_metadata: Dict[str, Any]) -> str:
        """Generate userScreen XML format."""
        
        # Create root userScreen element
        user_screen = ET.Element("userScreen")
        
        # Add screen metadata
        screen_name_elem = ET.SubElement(user_screen, "screenName")
        screen_name_elem.text = form_name
        
        screen_type_elem = ET.SubElement(user_screen, "screenType")
        screen_type_elem.text = form_metadata.get("screenType", "W")
        
        template_screen_elem = ET.SubElement(user_screen, "templateScreen")
        template_screen_elem.text = form_metadata.get("templateScreen", "N")
        
        form_type_elem = ET.SubElement(user_screen, "formType")
        form_type_elem.text = form_metadata.get("formType", "F")
        
        lang_id_elem = ET.SubElement(user_screen, "langID")
        lang_id_elem.text = form_metadata.get("langID", "en-us")
        
        screen_format_elem = ET.SubElement(user_screen, "screenFormat")
        screen_format_elem.text = form_metadata.get("screenFormat", "U")
        
        screen_desc_elem = ET.SubElement(user_screen, "screenDesc")
        screen_desc_elem.text = form_metadata.get("screenDesc", form_name)
        
        # Add screenData container
        screen_data = ET.SubElement(user_screen, "screenData")
        
        # Copy metadata to screenData
        screen_data_desc = ET.SubElement(screen_data, "screenDesc")
        screen_data_desc.text = form_metadata.get("screenDesc", form_name)
        
        screen_data_format = ET.SubElement(screen_data, "screenFormat")
        screen_data_format.text = form_metadata.get("screenFormat", "U")
        
        version_elem = ET.SubElement(screen_data, "version")
        version_elem.text = "0"
        
        # Add screen definition
        screen_definition = ET.SubElement(screen_data, "screenDefinition")
        screen_definition.set("definitionVersion", "2")
        
        title_elem = ET.SubElement(screen_definition, "title")
        title_elem.text = form_name
        
        new_form_elem = ET.SubElement(screen_definition, "newForm")
        new_form_elem.text = "awdForm"
        
        class_elem = ET.SubElement(screen_definition, "class")
        screen_url_elem = ET.SubElement(screen_definition, "screenURL")
        include_list_elem = ET.SubElement(screen_definition, "includeList")
        link_list_elem = ET.SubElement(screen_definition, "linkList")
        custom_rules_elem = ET.SubElement(screen_definition, "customRules")
        custom_properties_elem = ET.SubElement(screen_definition, "customProperties")
        
        # Add page container
        page = ET.SubElement(screen_definition, "page")
        page.set("index", "0")
        
        page_title = ET.SubElement(page, "title")
        page_title.text = form_name
        
        page_width = ET.SubElement(page, "width")
        page_width.text = "1200"
        
        page_height = ET.SubElement(page, "height")
        page_height.text = "1200"
        
        transform_vars = ET.SubElement(page, "transformVariables")
        
        # Add form fields
        self._add_form_fields(page, fields)
        
        # Add publicLink
        public_link = ET.SubElement(user_screen, "publicLink")
        public_link.text = "N"
        
        return self._format_xml(user_screen)
    
    def _add_form_fields(self, page: ET.Element, fields: List[Dict[str, Any]]):
        """Add form fields to the page element."""
        
        # Group fields by type
        groups = {}
        standalone_fields = []
        
        for field in fields:
            if field.get("type") == "group":
                group_name = field["ui_properties"].get("label", "Default Group")
                if group_name not in groups:
                    groups[group_name] = []
            else:
                # For now, add all fields as standalone
                standalone_fields.append(field)
        
        # Add groups first
        y_position = 35
        for group_name, group_fields in groups.items():
            group_elem = ET.SubElement(page, "group")
            
            label = ET.SubElement(group_elem, "label")
            label.text = f"---{group_name.upper()}---"
            
            class_elem = ET.SubElement(group_elem, "class")
            
            top_elem = ET.SubElement(group_elem, "top")
            top_elem.text = str(y_position)
            
            left_elem = ET.SubElement(group_elem, "left")
            left_elem.text = "16"
            
            width_elem = ET.SubElement(group_elem, "width")
            width_elem.text = "368"
            
            height_elem = ET.SubElement(group_elem, "height")
            height_elem.text = str(max(60, len(group_fields) * 40))
            
            y_position += 120
        
        # Add standalone fields
        for i, field in enumerate(standalone_fields):
            field_elem = self._create_field_element(field, i)
            if field_elem is not None:
                page.append(field_elem)
    
    def _create_field_element(self, field: Dict[str, Any], index: int) -> Optional[ET.Element]:
        """Create XML element for a form field."""
        
        field_type = field.get("type", "string")
        ui_props = field.get("ui_properties", {})
        validation = field.get("validation", {})
        
        # Create appropriate element based on field type
        if field_type == "boolean":
            elem = ET.Element("checkbox")
            
            # Add checkbox-specific attributes
            checked_value = ET.SubElement(elem, "checkedValue")
            checked_value.text = "Y"
            
            unchecked_value = ET.SubElement(elem, "uncheckedValue")
            unchecked_value.text = "N"
            
        elif field_type == "action":
            elem = ET.Element("routingButton")
            
            # Add button-specific attributes
            button_type = ET.SubElement(elem, "buttonType")
            button_type.text = "3"
            
            return_code = ET.SubElement(elem, "returnCode")
            return_code.text = "2"
            
        else:
            # Default to text input
            elem = ET.Element("input")
        
        # Add common attributes
        id_elem = ET.SubElement(elem, "id")
        id_elem.text = f"{field['name']}_{index + 1}"
        
        name_elem = ET.SubElement(elem, "name")
        name_elem.text = field["name"]
        
        sequence_elem = ET.SubElement(elem, "sequence")
        
        label_elem = ET.SubElement(elem, "label")
        label_elem.text = ui_props.get("label", field["name"])
        
        class_elem = ET.SubElement(elem, "class")
        
        data_dict_elem = ET.SubElement(elem, "dataDictionary")
        ext_data_dict_elem = ET.SubElement(elem, "externalDataDictionary")
        
        default_elem = ET.SubElement(elem, "default")
        default_elem.text = "N" if field_type == "boolean" else ""
        
        help_text_elem = ET.SubElement(elem, "helpText")
        
        tab_index_elem = ET.SubElement(elem, "tabIndex")
        tab_index_elem.text = str(ui_props.get("tabIndex", 0))
        
        readonly_elem = ET.SubElement(elem, "readOnly")
        readonly_elem.text = "Y" if validation.get("readonly", False) else "N"
        
        # Add positioning
        top_elem = ET.SubElement(elem, "top")
        top_elem.text = str(ui_props.get("top", 50 + index * 40))
        
        left_elem = ET.SubElement(elem, "left")
        left_elem.text = str(ui_props.get("left", 20))
        
        width_elem = ET.SubElement(elem, "width")
        width_elem.text = str(ui_props.get("width", 200))
        
        if field_type != "action":  # Buttons don't have height in the same way
            height_elem = ET.SubElement(elem, "height")
            height_elem.text = str(ui_props.get("height", 30))
        
        return elem
    
    def _format_xml(self, element: ET.Element) -> str:
        """Format XML element as pretty-printed string."""
        rough_string = ET.tostring(element, encoding='unicode')
        reparsed = minidom.parseString(rough_string)
        return reparsed.toprettyxml(indent="  ", encoding=None)


class JSONFormGenerator:
    """Generates uxData JSON format from form data."""
    
    def __init__(self):
        self.format_version = "1.0"
    
    def generate_uxdata_json(self, 
                           form_name: str, 
                           fields: List[Dict[str, Any]], 
                           form_metadata: Dict[str, Any]) -> Dict[str, Any]:
        """Generate uxData JSON format."""
        
        ux_data = {
            "formName": form_name,
            "formType": form_metadata.get("formType", "interactive"),
            "version": self.format_version,
            "metadata": {
                "screenName": form_name,
                "screenType": form_metadata.get("screenType", "flow"),
                "language": form_metadata.get("langID", "en-us"),
                "description": form_metadata.get("screenDesc", form_name),
                "extractionTimestamp": self._get_timestamp()
            },
            "layout": {
                "width": 1200,
                "height": 1200,
                "orientation": "portrait"
            },
            "fields": [],
            "groups": [],
            "validation": {
                "rules": [],
                "messages": {}
            },
            "styling": {
                "theme": "awd-default",
                "responsive": True
            }
        }
        
        # Process fields
        for field in fields:
            if field.get("type") == "group":
                ux_data["groups"].append(self._create_group_definition(field))
            else:
                ux_data["fields"].append(self._create_field_definition(field))
        
        # Extract validation rules
        validation_rules = self._extract_global_validation(fields)
        ux_data["validation"]["rules"] = validation_rules
        
        return ux_data
    
    def _create_field_definition(self, field: Dict[str, Any]) -> Dict[str, Any]:
        """Create JSON field definition."""
        
        ui_props = field.get("ui_properties", {})
        validation = field.get("validation", {})
        
        field_def = {
            "name": field["name"],
            "type": field["type"],
            "label": ui_props.get("label", field["name"]),
            "required": validation.get("required", False),
            "position": {
                "x": ui_props.get("left", 0),
                "y": ui_props.get("top", 0),
                "width": ui_props.get("width", 200),
                "height": ui_props.get("height", 30)
            },
            "properties": {
                "tabIndex": ui_props.get("tabIndex", 0),
                "readOnly": validation.get("readonly", False),
                "helpText": ui_props.get("helpText", "")
            }
        }
        
        # Add type-specific properties
        if field["type"] == "boolean":
            field_def["properties"]["checkedValue"] = "Y"
            field_def["properties"]["uncheckedValue"] = "N"
            field_def["properties"]["default"] = False
        
        elif field["type"] == "string":
            if validation.get("maxLength"):
                field_def["properties"]["maxLength"] = validation["maxLength"]
            if validation.get("pattern"):
                field_def["properties"]["pattern"] = validation["pattern"]
        
        elif field["type"] == "number":
            if validation.get("min") is not None:
                field_def["properties"]["min"] = validation["min"]
            if validation.get("max") is not None:
                field_def["properties"]["max"] = validation["max"]
            if validation.get("step"):
                field_def["properties"]["step"] = validation["step"]
        
        elif field["type"] == "action":
            field_def["properties"]["buttonType"] = "submit"
            field_def["properties"]["returnCode"] = 2
        
        # Add styling information
        field_def["styling"] = {
            "className": ui_props.get("class", ""),
            "inline": False,
            "width": "auto"
        }
        
        return field_def
    
    def _create_group_definition(self, group: Dict[str, Any]) -> Dict[str, Any]:
        """Create JSON group definition."""
        
        ui_props = group.get("ui_properties", {})
        
        return {
            "name": group["name"],
            "label": ui_props.get("label", group["name"]),
            "type": "container",
            "position": {
                "x": ui_props.get("left", 0),
                "y": ui_props.get("top", 0),
                "width": ui_props.get("width", 400),
                "height": ui_props.get("height", 100)
            },
            "styling": {
                "border": True,
                "collapsible": False
            },
            "fields": []  # Would be populated with field references
        }
    
    def _extract_global_validation(self, fields: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """Extract global validation rules."""
        rules = []
        
        for field in fields:
            validation = field.get("validation", {})
            
            if validation.get("required"):
                rules.append({
                    "type": "required",
                    "field": field["name"],
                    "message": f"{field['name']} is required"
                })
            
            if validation.get("pattern"):
                rules.append({
                    "type": "pattern",
                    "field": field["name"],
                    "pattern": validation["pattern"],
                    "message": f"{field['name']} format is invalid"
                })
            
            if validation.get("maxLength"):
                rules.append({
                    "type": "maxLength",
                    "field": field["name"],
                    "value": validation["maxLength"],
                    "message": f"{field['name']} exceeds maximum length"
                })
        
        return rules
    
    def _get_timestamp(self) -> str:
        """Get current timestamp."""
        from datetime import datetime
        return datetime.now().isoformat()


class AWDFormExtractor(ComponentExtractor):
    """
    Extractor for AWD form definitions in dual format (XML + JSON).
    
    Generates both userScreen XML format and uxData JSON format
    for comprehensive form definitions with UI metadata and validation.
    """
    
    def __init__(self):
        super().__init__("AWDFormExtractor")
        self.field_analyzer = FormFieldAnalyzer()
        self.xml_generator = XMLFormGenerator()
        self.json_generator = JSONFormGenerator()
    
    @property
    def component_type(self) -> str:
        return "forms"
    
    @property
    def supported_formats(self) -> List[str]:
        return ["xml", "json"]
    
    async def extract_components(self, 
                               context: ExtractionContext, 
                               processed_data: Dict[str, Any]) -> ExtractionResult:
        """Extract form components from AWD data."""
        
        files_generated = []
        extraction_errors = []
        
        try:
            # Create forms output directory
            forms_dir = Path(context.output_directory) / "forms"
            self._safe_create_directory(str(forms_dir))
            
            # Extract forms from processed data
            forms = await self._extract_forms(processed_data, context)
            
            if forms:
                # Generate form files for each form
                for form_name, form_data in forms.items():
                    # Generate XML format
                    xml_file = forms_dir / f"{form_name}.xml"
                    xml_content = self._generate_xml_form(form_name, form_data)
                    self._safe_write_file(str(xml_file), xml_content)
                    files_generated.append(str(xml_file))
                    
                    # Generate JSON format
                    json_file = forms_dir / f"{form_name}_uxdata.json"
                    json_content = self._generate_json_form(form_name, form_data)
                    self._safe_write_json(str(json_file), json_content)
                    files_generated.append(str(json_file))
                
                logger.info(f"Generated {len(files_generated)} form files")
            else:
                logger.warning("No forms found in processed_data")
                extraction_errors.append("No form definitions found for extraction")
            
            # Create metadata
            metadata = self._create_component_metadata(context, files_generated)
            metadata.validation_status = "extracted"
            
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
            logger.error(f"Form extraction failed: {e}")
            
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
                errors=[f"Form extraction failed: {str(e)}"]
            )
    
    async def _extract_forms(self, 
                           processed_data: Dict[str, Any], 
                           context: ExtractionContext) -> Dict[str, Dict[str, Any]]:
        """Extract form definitions from processed AWD data."""
        forms = {}
        
        try:
            # Convert processed_data to string for analysis
            content = self._serialize_for_analysis(processed_data)
            
            # Extract from business metadata
            business_metadata = processed_data.get("businessMetadata", {})
            if business_metadata:
                business_forms = await self._extract_from_business_metadata(business_metadata, content)
                forms.update(business_forms)
            
            # Extract from AWD structure
            awd_structure = processed_data.get("awdStructure", {})
            if awd_structure:
                awd_forms = await self._extract_from_awd_structure(awd_structure, content)
                forms.update(awd_forms)
            
            # Look for userScreen patterns in general content
            general_forms = await self._extract_from_content_patterns(content)
            forms.update(general_forms)
            
            # If no forms found, create a general form from field patterns
            if not forms:
                general_form = await self._extract_general_form(content, context)
                if general_form:
                    source_name = context.source_name or Path(context.source_file).stem
                    forms[f"{source_name}_form"] = general_form
            
            logger.debug(f"Extracted {len(forms)} form definitions")
            
        except Exception as e:
            logger.error(f"Error extracting forms: {e}")
        
        return forms
    
    def _serialize_for_analysis(self, data: Any) -> str:
        """Serialize data for pattern analysis."""
        import json
        try:
            return json.dumps(data, indent=2, default=str)
        except Exception:
            return str(data)
    
    async def _extract_from_business_metadata(self, 
                                            business_metadata: Dict[str, Any], 
                                            content: str) -> Dict[str, Dict[str, Any]]:
        """Extract forms from business metadata."""
        forms = {}
        
        try:
            # Look for form definitions in business metadata
            if "forms" in business_metadata:
                for form_info in business_metadata["forms"]:
                    if isinstance(form_info, dict):
                        form_name = form_info.get("name", f"BusinessForm_{len(forms)}")
                        
                        # Extract fields for this form
                        form_content = self._serialize_for_analysis(form_info)
                        fields = self.field_analyzer.extract_form_fields(
                            form_content + content, form_name
                        )
                        
                        if fields:
                            forms[form_name] = {
                                "fields": fields,
                                "metadata": form_info
                            }
            
            # Look for userScreen definitions
            if "userScreens" in business_metadata:
                for screen in business_metadata["userScreens"]:
                    if isinstance(screen, dict):
                        screen_name = screen.get("screenName", f"UserScreen_{len(forms)}")
                        
                        # Extract fields from user screen
                        screen_content = self._serialize_for_analysis(screen)
                        fields = self.field_analyzer.extract_form_fields(
                            screen_content + content, screen_name
                        )
                        
                        if fields:
                            forms[screen_name] = {
                                "fields": fields,
                                "metadata": screen
                            }
        
        except Exception as e:
            logger.error(f"Error extracting forms from business metadata: {e}")
        
        return forms
    
    async def _extract_from_awd_structure(self, 
                                        awd_structure: Dict[str, Any], 
                                        content: str) -> Dict[str, Dict[str, Any]]:
        """Extract forms from AWD structure."""
        forms = {}
        
        try:
            objects = awd_structure.get("objects", [])
            
            for i, obj in enumerate(objects):
                if isinstance(obj, dict):
                    obj_content = self._serialize_for_analysis(obj)
                    
                    # Check if this object contains form-like content
                    if self._is_form_object(obj_content):
                        form_name = (obj.get("screenName") or 
                                   obj.get("name") or 
                                   f"AWDForm_{i}")
                        
                        # Extract fields from this object
                        fields = self.field_analyzer.extract_form_fields(
                            obj_content + content, form_name
                        )
                        
                        if fields:
                            forms[form_name] = {
                                "fields": fields,
                                "metadata": obj
                            }
        
        except Exception as e:
            logger.error(f"Error extracting forms from AWD structure: {e}")
        
        return forms
    
    async def _extract_from_content_patterns(self, content: str) -> Dict[str, Dict[str, Any]]:
        """Extract forms from general content patterns."""
        forms = {}
        
        try:
            # Look for userScreen XML patterns
            userscreen_pattern = r'<userScreen[^>]*>.*?<screenName>([^<]+)</screenName>.*?</userScreen>'
            userscreen_matches = re.findall(userscreen_pattern, content, re.IGNORECASE | re.DOTALL)
            
            for screen_name in userscreen_matches:
                # Extract the full userScreen block
                screen_block_pattern = rf'<userScreen[^>]*>.*?<screenName>{re.escape(screen_name)}</screenName>.*?</userScreen>'
                screen_block = re.search(screen_block_pattern, content, re.IGNORECASE | re.DOTALL)
                
                if screen_block:
                    screen_content = screen_block.group(0)
                    fields = self.field_analyzer.extract_form_fields(screen_content, screen_name)
                    
                    if fields:
                        forms[screen_name] = {
                            "fields": fields,
                            "metadata": {"source": "userScreen_pattern"}
                        }
            
            # Look for form-like JSON structures
            form_json_pattern = r'"(?:form|screen|ui)"\s*:\s*{\s*"name"\s*:\s*"([^"]+)"'
            json_matches = re.findall(form_json_pattern, content, re.IGNORECASE)
            
            for form_name in json_matches:
                # Extract fields around this form definition
                fields = self.field_analyzer.extract_form_fields(content, form_name)
                
                if fields and form_name not in forms:
                    forms[form_name] = {
                        "fields": fields,
                        "metadata": {"source": "json_pattern"}
                    }
        
        except Exception as e:
            logger.error(f"Error extracting forms from content patterns: {e}")
        
        return forms
    
    async def _extract_general_form(self, content: str, context: ExtractionContext) -> Optional[Dict[str, Any]]:
        """Extract a general form from field patterns in content."""
        
        try:
            source_name = context.source_name or Path(context.source_file).stem
            
            # Extract all fields from content
            fields = self.field_analyzer.extract_form_fields(content, source_name)
            
            if fields:
                return {
                    "fields": fields,
                    "metadata": {
                        "source": "general_extraction",
                        "extractedFrom": context.source_file,
                        "totalFields": len(fields)
                    }
                }
        
        except Exception as e:
            logger.error(f"Error extracting general form: {e}")
        
        return None
    
    def _is_form_object(self, content: str) -> bool:
        """Check if an object contains form-like content."""
        
        form_indicators = [
            'userScreen', 'screenName', 'formType', 'checkbox', 'input',
            'field', 'button', 'form', 'ui', 'screen', 'label'
        ]
        
        content_lower = content.lower()
        return any(indicator in content_lower for indicator in form_indicators)
    
    def _generate_xml_form(self, form_name: str, form_data: Dict[str, Any]) -> str:
        """Generate XML form content."""
        
        try:
            fields = form_data.get("fields", [])
            metadata = form_data.get("metadata", {})
            
            return self.xml_generator.generate_userscreen_xml(form_name, fields, metadata)
        
        except Exception as e:
            logger.error(f"Error generating XML form for {form_name}: {e}")
            return self._generate_minimal_xml_form(form_name)
    
    def _generate_json_form(self, form_name: str, form_data: Dict[str, Any]) -> Dict[str, Any]:
        """Generate JSON form content."""
        
        try:
            fields = form_data.get("fields", [])
            metadata = form_data.get("metadata", {})
            
            return self.json_generator.generate_uxdata_json(form_name, fields, metadata)
        
        except Exception as e:
            logger.error(f"Error generating JSON form for {form_name}: {e}")
            return self._generate_minimal_json_form(form_name)
    
    def _generate_minimal_xml_form(self, form_name: str) -> str:
        """Generate minimal XML form."""
        
        return f"""<?xml version="1.0" encoding="UTF-8"?>
<userScreen>
    <screenName>{form_name}</screenName>
    <screenType>W</screenType>
    <templateScreen>N</templateScreen>
    <formType>F</formType>
    <langID>en-us</langID>
    <screenFormat>U</screenFormat>
    <screenDesc>{form_name}</screenDesc>
    <screenData>
        <screenDesc>{form_name}</screenDesc>
        <screenFormat>U</screenFormat>
        <version>0</version>
        <screenDefinition definitionVersion="2">
            <title>{form_name}</title>
            <newForm>awdForm</newForm>
            <class/>
            <page index="0">
                <title>{form_name}</title>
                <width>1200</width>
                <height>1200</height>
                <transformVariables/>
            </page>
        </screenDefinition>
    </screenData>
    <publicLink>N</publicLink>
</userScreen>"""
    
    def _generate_minimal_json_form(self, form_name: str) -> Dict[str, Any]:
        """Generate minimal JSON form."""
        
        return {
            "formName": form_name,
            "formType": "interactive",
            "version": "1.0",
            "metadata": {
                "screenName": form_name,
                "screenType": "flow",
                "language": "en-us",
                "description": form_name,
                "extractionTimestamp": self.json_generator._get_timestamp()
            },
            "layout": {
                "width": 1200,
                "height": 1200,
                "orientation": "portrait"
            },
            "fields": [],
            "groups": [],
            "validation": {
                "rules": [],
                "messages": {}
            },
            "styling": {
                "theme": "awd-default",
                "responsive": True
            }
        }


# Export the extractor
__all__ = ['AWDFormExtractor']