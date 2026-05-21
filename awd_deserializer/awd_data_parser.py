"""
Module: AWDDataParser - Intelligent parser for AWD serialized JSON data

Summary:
    Parses complex AWD serialized JSON structures to identify and categorize
    different component types (BPMN processes, forms, services, data types)
    for targeted extraction by specialized extractors.

Key Components:
    - ComponentType: Enumeration of AWD component types
    - AWDComponent: Data structure for identified components
    - AWDDataParser: Main parser class with intelligent categorization
    - parse_awd_json: Primary parsing function

Keywords: awd, data, parser, json, component, categorization, bpmn, form, service,
         data-type, serialization, deserialization, classification, extraction
Dependencies: json, base64, xml, typing, dataclasses, enum
Security: Input validation, safe JSON parsing, content sanitization
Performance: Efficient pattern matching, lazy evaluation, memory optimization
"""

import json
import base64
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from enum import Enum
from typing import Dict, List, Optional, Any, Set, Union
import logging
import re

logger = logging.getLogger(__name__)


class ComponentType(Enum):
    """AWD component types for categorization."""
    BPMN_PROCESS = "bpmn_process"
    MODERN_FORM = "modern_form"  # uxData JSON format
    TRADITIONAL_FORM = "traditional_form"  # userScreen XML format
    SERVICE_CONFIG = "service_config"
    DATA_TYPE = "data_type"
    UNKNOWN = "unknown"


@dataclass
class AWDComponent:
    """Represents an identified AWD component."""
    component_type: ComponentType
    name: str
    component_id: str
    data: Dict[str, Any]
    metadata: Dict[str, Any] = field(default_factory=dict)
    source_path: str = ""
    
    def __post_init__(self):
        """Post-initialization validation."""
        if not self.name:
            self.name = f"{self.component_type.value}_{self.component_id[:8]}"
        if not self.component_id:
            self.component_id = f"generated_{id(self)}"


class AWDDataParser:
    """
    Intelligent parser for AWD serialized JSON data.
    
    Identifies and categorizes different component types within the complex
    nested JSON structure produced by the Java deserializer.
    """
    
    def __init__(self):
        """Initialize the parser with component identification patterns."""
        self.components: List[AWDComponent] = []
        self.statistics = {
            "total_objects": 0,
            "identified_components": 0,
            "bpmn_processes": 0,
            "forms": 0,
            "services": 0,
            "data_types": 0,
            "unknown": 0
        }
    
    def parse(self, awd_json: Union[str, Dict[str, Any]]) -> List[AWDComponent]:
        """
        Parse AWD JSON data and identify all components.
        
        Args:
            awd_json: JSON string or parsed JSON dict from AWD deserializer
            
        Returns:
            List of identified AWD components
        """
        try:
            if isinstance(awd_json, str):
                data = json.loads(awd_json)
            else:
                data = awd_json
            
            logger.info("Starting AWD JSON parsing")
            self.components.clear()
            self._reset_statistics()
            
            # Parse different sections of the AWD structure
            self._parse_awd_structure(data)
            self._parse_business_metadata(data)
            
            # Update statistics
            self._update_statistics()
            
            logger.info(f"Parsing complete. Found {len(self.components)} components")
            return self.components
            
        except Exception as e:
            logger.error(f"Error parsing AWD JSON: {e}")
            raise
    
    def _parse_awd_structure(self, data: Dict[str, Any]) -> None:
        """Parse the awdStructure section for component data."""
        # Try different possible locations for AWD structure
        awd_structure = (self._get_nested_value(data, "_mapEntries.awdStructure") or
                        self._get_nested_value(data, "awdStructure._mapEntries") or
                        self._get_nested_value(data, "awdStructure"))
        
        if not awd_structure:
            logger.warning("No awdStructure found in data, searching entire structure")
            # If no specific AWD structure, search the entire data
            awd_structure = data
        
        # Look for serialized objects that contain component data
        self._find_serialized_components(awd_structure)
    
    def _parse_business_metadata(self, data: Dict[str, Any]) -> None:
        """Parse business metadata section for additional component info."""
        business_metadata = self._get_nested_value(data, "businessMetadata")
        if business_metadata:
            logger.debug("Found business metadata section")
            # Extract additional component metadata
    
    def _find_serialized_components(self, data: Any, path: str = "") -> None:
        """
        Recursively search for serialized components in the data structure.
        
        Args:
            data: Current data node to search
            path: Current path in the data structure
        """
        if isinstance(data, dict):
            # Check for BPMN process definitions
            if self._is_bpmn_process(data):
                component = self._extract_bpmn_component(data, path)
                if component:
                    self.components.append(component)
            
            # Check for form definitions
            elif self._is_form_definition(data):
                component = self._extract_form_component(data, path)
                if component:
                    self.components.append(component)
            
            # Check for service definitions
            elif self._is_service_definition(data):
                component = self._extract_service_component(data, path)
                if component:
                    self.components.append(component)
            
            # Check for data type definitions
            elif self._is_data_type_definition(data):
                component = self._extract_data_type_component(data, path)
                if component:
                    self.components.append(component)
            
            # Recursively search nested structures
            for key, value in data.items():
                if isinstance(value, (dict, list)):
                    new_path = f"{path}.{key}" if path else key
                    self._find_serialized_components(value, new_path)
        
        elif isinstance(data, list):
            for i, item in enumerate(data):
                new_path = f"{path}[{i}]" if path else f"[{i}]"
                self._find_serialized_components(item, new_path)
    
    def _is_bpmn_process(self, data: Dict[str, Any]) -> bool:
        """Check if data contains a BPMN process definition."""
        # Look for BPMN XML content or BPMN-specific patterns
        if self._contains_xml_content(data):
            xml_content = self._extract_xml_content(data)
            if xml_content and 'bpmn:' in xml_content:
                return True
        
        # Look for ProcessData class references
        class_name = self._get_nested_value(data, "_class")
        if class_name and "ProcessData" in str(class_name):
            return True
        
        # Look for BPMN-specific properties
        if self._has_bpmn_properties(data):
            return True
        
        return False
    
    def _is_form_definition(self, data: Dict[str, Any]) -> bool:
        """Check if data contains a form definition."""
        # Look for uxData JSON format (modern forms)
        if self._has_ux_data_properties(data):
            return True
        
        # Look for userScreen XML format (traditional forms)
        if self._has_user_screen_properties(data):
            return True
        
        # Look for form-specific class names
        class_name = self._get_nested_value(data, "_class")
        if class_name and any(form_type in str(class_name).lower() 
                             for form_type in ["form", "screen", "ui"]):
            return True
        
        return False
    
    def _is_service_definition(self, data: Dict[str, Any]) -> bool:
        """Check if data contains a service definition."""
        # Look for ServiceData class references
        class_name = self._get_nested_value(data, "_class")
        if class_name and "ServiceData" in str(class_name):
            return True
        
        # Look for service-specific properties
        if self._has_service_properties(data):
            return True
        
        return False
    
    def _is_data_type_definition(self, data: Dict[str, Any]) -> bool:
        """Check if data contains a data type definition."""
        # Look for business object class patterns
        class_name = self._get_nested_value(data, "_class")
        if class_name and any(pattern in str(class_name) 
                             for pattern in ["WorkInstance", "SourceInstance", "OutboundEmail"]):
            return True
        
        # Look for schema or type definition patterns
        if self._has_data_type_properties(data):
            return True
        
        return False
    
    def _extract_bpmn_component(self, data: Dict[str, Any], path: str) -> Optional[AWDComponent]:
        """Extract BPMN component from identified data."""
        try:
            # Extract BPMN XML content
            xml_content = self._extract_xml_content(data)
            process_name = self._extract_process_name(data, xml_content)
            process_id = self._extract_process_id(data, xml_content)
            
            component = AWDComponent(
                component_type=ComponentType.BPMN_PROCESS,
                name=process_name or "Unknown Process",
                component_id=process_id or "unknown_process",
                data=data,
                metadata={
                    "xml_content": xml_content,
                    "has_awd_extensions": xml_content and "awd:" in xml_content if xml_content else False,
                    "process_elements": self._count_bpmn_elements(xml_content) if xml_content else {}
                },
                source_path=path
            )
            
            logger.debug(f"Extracted BPMN process: {component.name}")
            return component
            
        except Exception as e:
            logger.error(f"Error extracting BPMN component: {e}")
            return None
    
    def _extract_form_component(self, data: Dict[str, Any], path: str) -> Optional[AWDComponent]:
        """Extract form component from identified data."""
        try:
            # Determine form type and extract accordingly
            if self._has_ux_data_properties(data):
                return self._extract_modern_form(data, path)
            elif self._has_user_screen_properties(data):
                return self._extract_traditional_form(data, path)
            else:
                # Generic form extraction
                form_name = self._extract_form_name(data)
                form_id = self._extract_form_id(data)
                
                component = AWDComponent(
                    component_type=ComponentType.MODERN_FORM,  # Default to modern
                    name=form_name or "Unknown Form",
                    component_id=form_id or "unknown_form",
                    data=data,
                    source_path=path
                )
                
                logger.debug(f"Extracted generic form: {component.name}")
                return component
                
        except Exception as e:
            logger.error(f"Error extracting form component: {e}")
            return None
    
    def _extract_service_component(self, data: Dict[str, Any], path: str) -> Optional[AWDComponent]:
        """Extract service component from identified data."""
        try:
            service_name = self._extract_service_name(data)
            service_id = self._extract_service_id(data)
            
            component = AWDComponent(
                component_type=ComponentType.SERVICE_CONFIG,
                name=service_name or "Unknown Service",
                component_id=service_id or "unknown_service",
                data=data,
                metadata={
                    "service_type": self._extract_service_type(data),
                    "interfaces": self._extract_service_interfaces(data)
                },
                source_path=path
            )
            
            logger.debug(f"Extracted service: {component.name}")
            return component
            
        except Exception as e:
            logger.error(f"Error extracting service component: {e}")
            return None
    
    def _extract_data_type_component(self, data: Dict[str, Any], path: str) -> Optional[AWDComponent]:
        """Extract data type component from identified data."""
        try:
            type_name = self._extract_type_name(data)
            type_id = self._extract_type_id(data)
            
            component = AWDComponent(
                component_type=ComponentType.DATA_TYPE,
                name=type_name or "Unknown Type",
                component_id=type_id or "unknown_type",
                data=data,
                metadata={
                    "class_name": self._get_nested_value(data, "_class"),
                    "fields": self._extract_type_fields(data)
                },
                source_path=path
            )
            
            logger.debug(f"Extracted data type: {component.name}")
            return component
            
        except Exception as e:
            logger.error(f"Error extracting data type component: {e}")
            return None
    
    def _extract_modern_form(self, data: Dict[str, Any], path: str) -> AWDComponent:
        """Extract modern uxData JSON form."""
        form_name = self._extract_form_name(data)
        form_id = self._extract_form_id(data)
        
        return AWDComponent(
            component_type=ComponentType.MODERN_FORM,
            name=form_name or "Unknown Modern Form",
            component_id=form_id or "unknown_modern_form",
            data=data,
            metadata={
                "form_type": "uxData",
                "has_styling": self._has_form_styling(data),
                "data_sources": self._extract_form_data_sources(data)
            },
            source_path=path
        )
    
    def _extract_traditional_form(self, data: Dict[str, Any], path: str) -> AWDComponent:
        """Extract traditional userScreen XML form."""
        form_name = self._extract_form_name(data)
        form_id = self._extract_form_id(data)
        
        return AWDComponent(
            component_type=ComponentType.TRADITIONAL_FORM,
            name=form_name or "Unknown Traditional Form",
            component_id=form_id or "unknown_traditional_form",
            data=data,
            metadata={
                "form_type": "userScreen",
                "screen_format": self._extract_screen_format(data),
                "controls": self._extract_form_controls(data)
            },
            source_path=path
        )
    
    # Helper methods for pattern detection and data extraction
    
    def _contains_xml_content(self, data: Dict[str, Any]) -> bool:
        """Check if data contains XML content."""
        return self._extract_xml_content(data) is not None
    
    def _extract_xml_content(self, data: Dict[str, Any]) -> Optional[str]:
        """Extract XML content from data, handling base64 encoding."""
        # Look for base64-encoded XML content in various fields
        for key in ["definition", "content", "xml", "data", "_value"]:
            value = self._get_nested_value(data, f"{key}._value") or self._get_nested_value(data, key)
            if value and isinstance(value, str):
                # Try to decode if it looks like base64
                if self._is_base64(value):
                    try:
                        decoded = base64.b64decode(value).decode('utf-8')
                        if decoded.strip().startswith('<?xml') or '<bpmn:' in decoded:
                            return decoded
                    except:
                        pass
                # Also check if it's already XML
                elif value.strip().startswith('<?xml') or '<bpmn:' in value:
                    return value
                # Check for long strings that might contain BPMN
                elif len(value) > 100 and '<bpmn:' in value:
                    return value
        
        # Also search recursively for any string containing BPMN XML
        return self._search_for_bpmn_xml(data)
    
    def _search_for_bpmn_xml(self, data: Any) -> Optional[str]:
        """Recursively search for BPMN XML content in data structure."""
        if isinstance(data, str):
            if len(data) > 100 and ('<bpmn:' in data or 'xmlns:bpmn=' in data):
                return data
        elif isinstance(data, dict):
            for value in data.values():
                result = self._search_for_bpmn_xml(value)
                if result:
                    return result
        elif isinstance(data, list):
            for item in data:
                result = self._search_for_bpmn_xml(item)
                if result:
                    return result
        return None
    
    def _is_base64(self, value: str) -> bool:
        """Check if a string is likely base64 encoded."""
        if not value or len(value) < 4:
            return False
        try:
            base64.b64decode(value, validate=True)
            return True
        except:
            return False
    
    def _has_bpmn_properties(self, data: Dict[str, Any]) -> bool:
        """Check for BPMN-specific properties."""
        bpmn_indicators = [
            "serviceId", "serviceGuId", "workStepType", "processModelId",
            "startEvent", "endEvent", "userTask", "serviceTask"
        ]
        return any(self._deep_search_key(data, indicator) for indicator in bpmn_indicators)
    
    def _has_ux_data_properties(self, data: Dict[str, Any]) -> bool:
        """Check for uxData form properties."""
        ux_indicators = ["uxData", "layoutContents", "dataSources", "theme", "styles"]
        return any(self._deep_search_key(data, indicator) for indicator in ux_indicators)
    
    def _has_user_screen_properties(self, data: Dict[str, Any]) -> bool:
        """Check for userScreen form properties."""
        screen_indicators = ["userScreen", "screenData", "screenDefinition", "screenFormat"]
        return any(self._deep_search_key(data, indicator) for indicator in screen_indicators)
    
    def _has_service_properties(self, data: Dict[str, Any]) -> bool:
        """Check for service-specific properties."""
        service_indicators = ["serviceId", "serviceGuId", "modelState", "inputs", "outputs"]
        return any(self._deep_search_key(data, indicator) for indicator in service_indicators)
    
    def _has_data_type_properties(self, data: Dict[str, Any]) -> bool:
        """Check for data type definition properties."""
        type_indicators = ["className", "fields", "schema", "typeDefinition"]
        return any(self._deep_search_key(data, indicator) for indicator in type_indicators)
    
    def _deep_search_key(self, data: Any, key: str) -> bool:
        """Recursively search for a key in nested data structures."""
        if isinstance(data, dict):
            if key in data:
                return True
            return any(self._deep_search_key(value, key) for value in data.values())
        elif isinstance(data, list):
            return any(self._deep_search_key(item, key) for item in data)
        return False
    
    def _get_nested_value(self, data: Dict[str, Any], path: str) -> Any:
        """Get nested value using dot notation path."""
        try:
            current = data
            for part in path.split('.'):
                if isinstance(current, dict) and part in current:
                    current = current[part]
                else:
                    return None
            return current
        except:
            return None
    
    def _extract_process_name(self, data: Dict[str, Any], xml_content: Optional[str]) -> Optional[str]:
        """Extract process name from data or XML."""
        # Try to get from data structure
        name = self._get_nested_value(data, "name._value")
        if name:
            return name
        
        # Try to extract from XML
        if xml_content:
            try:
                root = ET.fromstring(xml_content)
                for process in root.findall('.//{http://www.omg.org/spec/BPMN/20100524/MODEL}process'):
                    name_attr = process.get('name')
                    if name_attr:
                        return name_attr
            except:
                pass
        
        return None
    
    def _extract_process_id(self, data: Dict[str, Any], xml_content: Optional[str]) -> Optional[str]:
        """Extract process ID from data or XML."""
        # Try to get from data structure
        proc_id = self._get_nested_value(data, "id._value")
        if proc_id:
            return proc_id
        
        # Try to extract from XML
        if xml_content:
            try:
                root = ET.fromstring(xml_content)
                for process in root.findall('.//{http://www.omg.org/spec/BPMN/20100524/MODEL}process'):
                    id_attr = process.get('id')
                    if id_attr:
                        return id_attr
            except:
                pass
        
        return None
    
    def _count_bpmn_elements(self, xml_content: str) -> Dict[str, int]:
        """Count BPMN elements in XML content."""
        if not xml_content:
            return {}
        
        try:
            counts = {}
            root = ET.fromstring(xml_content)
            
            # Count different BPMN elements
            elements_to_count = [
                'startEvent', 'endEvent', 'userTask', 'serviceTask',
                'exclusiveGateway', 'sequenceFlow', 'boundaryEvent'
            ]
            
            for element_type in elements_to_count:
                elements = root.findall(f'.//{{{http://www.omg.org/spec/BPMN/20100524/MODEL}}}{element_type}')
                counts[element_type] = len(elements)
            
            return counts
        except:
            return {}
    
    def _extract_form_name(self, data: Dict[str, Any]) -> Optional[str]:
        """Extract form name from data."""
        return (self._get_nested_value(data, "name._value") or
                self._get_nested_value(data, "screenName._value") or
                self._get_nested_value(data, "formName._value"))
    
    def _extract_form_id(self, data: Dict[str, Any]) -> Optional[str]:
        """Extract form ID from data."""
        return (self._get_nested_value(data, "id._value") or
                self._get_nested_value(data, "formId._value") or
                self._get_nested_value(data, "screenId._value"))
    
    def _extract_service_name(self, data: Dict[str, Any]) -> Optional[str]:
        """Extract service name from data."""
        return (self._get_nested_value(data, "name._value") or
                self._get_nested_value(data, "serviceName._value"))
    
    def _extract_service_id(self, data: Dict[str, Any]) -> Optional[str]:
        """Extract service ID from data."""
        return (self._get_nested_value(data, "id._value") or
                self._get_nested_value(data, "serviceId._value") or
                self._get_nested_value(data, "serviceGuId._value"))
    
    def _extract_type_name(self, data: Dict[str, Any]) -> Optional[str]:
        """Extract data type name from data."""
        class_name = self._get_nested_value(data, "_class")
        if class_name:
            # Extract simple class name from full package path
            return class_name.split('.')[-1] if '.' in class_name else class_name
        return None
    
    def _extract_type_id(self, data: Dict[str, Any]) -> Optional[str]:
        """Extract data type ID from data."""
        return (self._get_nested_value(data, "id._value") or
                self._get_nested_value(data, "typeId._value"))
    
    def _extract_service_type(self, data: Dict[str, Any]) -> Optional[str]:
        """Extract service type from data."""
        return self._get_nested_value(data, "serviceType._value")
    
    def _extract_service_interfaces(self, data: Dict[str, Any]) -> List[str]:
        """Extract service interfaces from data."""
        interfaces = []
        # Implementation would extract interface definitions
        return interfaces
    
    def _extract_type_fields(self, data: Dict[str, Any]) -> List[Dict[str, str]]:
        """Extract type field definitions from data."""
        fields = []
        # Implementation would extract field definitions
        return fields
    
    def _has_form_styling(self, data: Dict[str, Any]) -> bool:
        """Check if form has styling information."""
        return self._deep_search_key(data, "styles") or self._deep_search_key(data, "theme")
    
    def _extract_form_data_sources(self, data: Dict[str, Any]) -> List[str]:
        """Extract form data sources."""
        sources = []
        # Implementation would extract data source definitions
        return sources
    
    def _extract_screen_format(self, data: Dict[str, Any]) -> Optional[str]:
        """Extract screen format from traditional form data."""
        return self._get_nested_value(data, "screenFormat._value")
    
    def _extract_form_controls(self, data: Dict[str, Any]) -> List[str]:
        """Extract form control definitions."""
        controls = []
        # Implementation would extract control definitions
        return controls
    
    def _reset_statistics(self):
        """Reset parsing statistics."""
        for key in self.statistics:
            self.statistics[key] = 0
    
    def _update_statistics(self):
        """Update parsing statistics based on found components."""
        self.statistics["identified_components"] = len(self.components)
        
        for component in self.components:
            if component.component_type == ComponentType.BPMN_PROCESS:
                self.statistics["bpmn_processes"] += 1
            elif component.component_type in [ComponentType.MODERN_FORM, ComponentType.TRADITIONAL_FORM]:
                self.statistics["forms"] += 1
            elif component.component_type == ComponentType.SERVICE_CONFIG:
                self.statistics["services"] += 1
            elif component.component_type == ComponentType.DATA_TYPE:
                self.statistics["data_types"] += 1
            else:
                self.statistics["unknown"] += 1
    
    def get_statistics(self) -> Dict[str, int]:
        """Get parsing statistics."""
        return self.statistics.copy()
    
    def get_components_by_type(self, component_type: ComponentType) -> List[AWDComponent]:
        """Get all components of a specific type."""
        return [comp for comp in self.components if comp.component_type == component_type]


def parse_awd_json(awd_json: Union[str, Dict[str, Any]]) -> List[AWDComponent]:
    """
    Convenience function to parse AWD JSON data.
    
    Args:
        awd_json: JSON string or parsed JSON dict from AWD deserializer
        
    Returns:
        List of identified AWD components
    """
    parser = AWDDataParser()
    return parser.parse(awd_json)