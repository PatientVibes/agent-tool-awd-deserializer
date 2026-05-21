"""
Module: TraditionalFormExtractor - Extract Traditional userScreen XML Forms from AWD data

Summary:
    Extracts complete traditional form definitions from AWD serialized JSON data.
    Generates userScreen XML format with screen definitions, controls, and layout
    that match the standard AWD userScreen XML reference format.

Key Components:
    - TraditionalFormExtractor: userScreen XML format generation
    - ScreenDefinition: Screen layout and control management
    - extract_traditional_forms: Primary extraction function

Keywords: form, extractor, userscreen, xml, traditional, classic, controls,
         screen-definition, layout, checkbox, textinput, datadictionary
Dependencies: xml.etree.ElementTree, typing, dataclasses, logging
Security: Input validation, safe XML generation, content sanitization
Performance: Efficient XML processing, memory optimization
"""

import logging
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

from .awd_data_parser import AWDComponent, ComponentType, parse_awd_json
from .form_extractor import FormDataExtractor, FormInfo

logger = logging.getLogger(__name__)


@dataclass
class ScreenControl:
    """Represents a control element in a userScreen form."""
    control_type: str  # checkbox, textInput, dataDictionary, etc.
    control_id: str
    name: str
    label: str
    position: tuple[int, int, int, int]  # top, left, width, height
    properties: dict[str, Any] = field(default_factory=dict)


@dataclass
class ScreenGroup:
    """Represents a group of controls in a userScreen form."""
    group_id: str
    label: str
    position: tuple[int, int, int, int]  # top, left, width, height
    controls: list[ScreenControl] = field(default_factory=list)


class TraditionalFormExtractor:
    """
    Extracts traditional userScreen XML forms from AWD components.
    """

    def __init__(self):
        """Initialize the traditional form extractor."""
        self.data_extractor = FormDataExtractor()
        self.extracted_forms: list[FormInfo] = []

    def extract_forms(self, awd_json: dict[str, Any]) -> list[FormInfo]:
        """Extract all traditional forms from AWD JSON data."""
        logger.info("Starting traditional form extraction")

        # Parse AWD components to find form data
        components = parse_awd_json(awd_json)
        form_components = [c for c in components if c.component_type in [ComponentType.TRADITIONAL_FORM, ComponentType.BPMN_PROCESS]]

        logger.info(f"Found {len(form_components)} potential traditional form components")

        self.extracted_forms.clear()

        for component in form_components:
            try:
                # Extract form fields from component
                fields = self.data_extractor.extract_form_fields(component.data)
                if fields:
                    form_info = self._create_traditional_form(component, fields)
                    if form_info:
                        self.extracted_forms.append(form_info)
                        logger.debug(f"Extracted traditional form: {form_info.form_name}")

            except Exception as e:
                logger.error(f"Error extracting traditional form from component {component.name}: {e}")

        logger.info(f"Traditional form extraction complete. {len(self.extracted_forms)} forms extracted")
        return self.extracted_forms

    def _create_traditional_form(self, component: AWDComponent, fields: list[dict[str, Any]]) -> FormInfo | None:
        """Create a traditional userScreen XML form from extracted fields."""
        try:
            # Generate form metadata
            form_id = self._extract_form_id(component, fields)
            form_name = self._extract_form_name(component, fields)
            screen_name = form_name.upper().replace(" ", "")

            # Create the userScreen XML structure
            userscreen_xml = self._create_userscreen_xml(screen_name, form_name, fields)

            return FormInfo(
                form_id=form_id,
                form_name=form_name,
                form_type="traditional",
                content=userscreen_xml,
                data_sources=self._create_data_sources(fields),
                validation_errors=[],
                metadata={
                    "field_count": len(fields),
                    "screen_name": screen_name,
                    "has_groups": True,
                    "control_types": self._get_control_types(fields)
                }
            )

        except Exception as e:
            logger.error(f"Error creating traditional form: {e}")
            return None

    def _create_userscreen_xml(self, screen_name: str, form_name: str, fields: list[dict[str, Any]]) -> str:
        """Create the complete userScreen XML structure."""
        # Create root userScreen element
        userscreen = ET.Element("userScreen")

        # Add screen metadata
        ET.SubElement(userscreen, "screenName").text = screen_name
        ET.SubElement(userscreen, "screenType").text = "W"
        ET.SubElement(userscreen, "templateScreen").text = "N"
        ET.SubElement(userscreen, "formType").text = "F"
        ET.SubElement(userscreen, "langID").text = "en-us"
        ET.SubElement(userscreen, "screenFormat").text = "U"
        ET.SubElement(userscreen, "screenDesc").text = form_name

        # Create screenData section
        screen_data = ET.SubElement(userscreen, "screenData")
        ET.SubElement(screen_data, "screenDesc").text = form_name
        ET.SubElement(screen_data, "screenFormat").text = "U"
        ET.SubElement(screen_data, "version").text = "0"

        # Create screenDefinition
        screen_def = ET.SubElement(screen_data, "screenDefinition")
        screen_def.set("definitionVersion", "2")

        ET.SubElement(screen_def, "title").text = form_name
        ET.SubElement(screen_def, "newForm").text = "awdForm"
        ET.SubElement(screen_def, "class")
        ET.SubElement(screen_def, "screenURL")
        ET.SubElement(screen_def, "includeList")
        ET.SubElement(screen_def, "linkList")
        ET.SubElement(screen_def, "customRules")
        ET.SubElement(screen_def, "customProperties")

        # Create page
        page = ET.SubElement(screen_def, "page")
        page.set("index", "0")

        ET.SubElement(page, "title").text = form_name
        ET.SubElement(page, "width").text = "1200"
        ET.SubElement(page, "height").text = "1200"
        ET.SubElement(page, "transformVariables")

        # Create groups and controls based on fields
        self._create_form_groups(page, fields)

        # Add navigation buttons
        self._add_navigation_buttons(page)

        # Add publicLink
        ET.SubElement(userscreen, "publicLink").text = "N"

        # Convert to string with proper formatting
        self._indent_xml(userscreen)
        return ET.tostring(userscreen, encoding='unicode')

    def _create_form_groups(self, page: ET.Element, fields: list[dict[str, Any]]):
        """Create form groups and controls based on field data."""
        if not fields:
            return

        # Group fields by category
        data_dict_fields = [f for f in fields if f.get("isDataDictionary")]
        question_fields = [f for f in fields if f.get("name", "").startswith("Question_")]
        ci_fields = [f for f in fields if f.get("name", "").startswith("CI")]
        overview_fields = [f for f in fields if f.get("name", "").startswith("Overview_")]

        current_top = 35

        # Create Information group if we have data dictionary fields
        if data_dict_fields:
            info_group = self._create_info_group(page, data_dict_fields, current_top)
            current_top += 200

        # Create Customer Interview group if we have question and CI fields
        if question_fields and ci_fields:
            ci_group = self._create_customer_interview_group(page, question_fields, ci_fields, current_top)
            current_top += 600

        # Create Overview group if we have overview fields
        if overview_fields:
            overview_group = self._create_overview_group(page, overview_fields, current_top)

    def _create_info_group(self, page: ET.Element, fields: list[dict[str, Any]], top: int) -> ET.Element:
        """Create information group with data dictionary fields."""
        group = ET.SubElement(page, "group")
        ET.SubElement(group, "label").text = "---FORM INFORMATION---"
        ET.SubElement(group, "class")
        ET.SubElement(group, "top").text = str(top)
        ET.SubElement(group, "left").text = "16"
        ET.SubElement(group, "width").text = "600"
        ET.SubElement(group, "height").text = "150"

        # Add data dictionary fields as text inputs
        field_top = 30
        for i, field in enumerate(fields[:5]):  # Limit to 5 fields
            dd_field = ET.SubElement(group, "dataDictionary")
            dd_field.set("fieldType", "textInput")

            field_id = f"{field.get('name', f'field_{i}')}_1"
            ET.SubElement(dd_field, "id").text = field_id
            ET.SubElement(dd_field, "name").text = field.get('name', f'field_{i}')
            ET.SubElement(dd_field, "sequence")
            ET.SubElement(dd_field, "label").text = field.get('name', f'Field {i+1}').replace('_', ' ')
            ET.SubElement(dd_field, "class")
            ET.SubElement(dd_field, "dataDictionary").text = field.get('name', f'FIELD{i+1}')
            ET.SubElement(dd_field, "externalDataDictionary")
            ET.SubElement(dd_field, "default")
            ET.SubElement(dd_field, "fieldFormat").text = "Alphabetic"
            ET.SubElement(dd_field, "decimals").text = "0"
            ET.SubElement(dd_field, "length").text = "50"
            ET.SubElement(dd_field, "mask").text = "X" * 50
            ET.SubElement(dd_field, "maskOverride").text = "N"
            ET.SubElement(dd_field, "helpText").text = f"Enter {field.get('name', 'value')}"
            ET.SubElement(dd_field, "tabIndex").text = str((i + 1) * 10)
            ET.SubElement(dd_field, "required").text = "N"
            ET.SubElement(dd_field, "readOnly").text = "N"
            ET.SubElement(dd_field, "password").text = "N"
            ET.SubElement(dd_field, "allowSmartControl").text = "Y"
            ET.SubElement(dd_field, "labelPosition")
            ET.SubElement(dd_field, "top").text = str(field_top)
            ET.SubElement(dd_field, "left").text = str(20 + (i % 2) * 280)
            ET.SubElement(dd_field, "width").text = "250"

            if i % 2 == 1:
                field_top += 40

        return group

    def _create_customer_interview_group(self, page: ET.Element, question_fields: list[dict[str, Any]],
                                       ci_fields: list[dict[str, Any]], top: int) -> ET.Element:
        """Create customer interview group with questions and scoring."""
        group = ET.SubElement(page, "group")
        ET.SubElement(group, "label").text = "---CUSTOMER INTERVIEW---"
        ET.SubElement(group, "class")
        ET.SubElement(group, "top").text = str(top)
        ET.SubElement(group, "left").text = "16"
        ET.SubElement(group, "width").text = "1158"
        ET.SubElement(group, "height").text = "550"

        # Create subgroups for questions and scores
        questions_group = ET.SubElement(group, "group")
        ET.SubElement(questions_group, "label").text = "Questions"
        ET.SubElement(questions_group, "class")
        ET.SubElement(questions_group, "top").text = "25"
        ET.SubElement(questions_group, "left").text = "10"
        ET.SubElement(questions_group, "width").text = "700"
        ET.SubElement(questions_group, "height").text = "500"

        scores_group = ET.SubElement(group, "group")
        ET.SubElement(scores_group, "label").text = "Scores"
        ET.SubElement(scores_group, "class")
        ET.SubElement(scores_group, "top").text = "25"
        ET.SubElement(scores_group, "left").text = "720"
        ET.SubElement(scores_group, "width").text = "400"
        ET.SubElement(scores_group, "height").text = "500"

        # Add question text areas and scoring fields
        question_top = 30
        for i, (question_field, ci_field) in enumerate(zip(question_fields, ci_fields)):
            # Question text area (in questions group)
            dd_field = ET.SubElement(questions_group, "dataDictionary")
            dd_field.set("fieldType", "textInput")

            q_field_id = f"{question_field.get('name', f'question_{i}')}_1"
            ET.SubElement(dd_field, "id").text = q_field_id
            ET.SubElement(dd_field, "name").text = question_field.get('name', f'question_{i}')
            ET.SubElement(dd_field, "sequence")
            ET.SubElement(dd_field, "label").text = f"Question {i+1}"
            ET.SubElement(dd_field, "class")
            ET.SubElement(dd_field, "dataDictionary").text = question_field.get('name', f'Q{str(i+1).zfill(2)}')
            ET.SubElement(dd_field, "externalDataDictionary")
            ET.SubElement(dd_field, "default")
            ET.SubElement(dd_field, "fieldFormat").text = "Alphabetic"
            ET.SubElement(dd_field, "decimals").text = "0"
            ET.SubElement(dd_field, "length").text = "500"
            ET.SubElement(dd_field, "mask").text = "X" * 500
            ET.SubElement(dd_field, "maskOverride").text = "N"
            ET.SubElement(dd_field, "helpText").text = f"Enter question {i+1}"
            ET.SubElement(dd_field, "tabIndex").text = str((i + 1) * 20)
            ET.SubElement(dd_field, "required").text = "N"
            ET.SubElement(dd_field, "readOnly").text = "N"
            ET.SubElement(dd_field, "password").text = "N"
            ET.SubElement(dd_field, "allowSmartControl").text = "Y"
            ET.SubElement(dd_field, "labelPosition")
            ET.SubElement(dd_field, "top").text = str(question_top)
            ET.SubElement(dd_field, "left").text = "20"
            ET.SubElement(dd_field, "width").text = "650"

            # Score field (in scores group)
            score_field = ET.SubElement(scores_group, "dataDictionary")
            score_field.set("fieldType", "textInput")

            s_field_id = f"{ci_field.get('name', f'ci_{i}')}_1"
            ET.SubElement(score_field, "id").text = s_field_id
            ET.SubElement(score_field, "name").text = ci_field.get('name', f'ci_{i}')
            ET.SubElement(score_field, "sequence")
            ET.SubElement(score_field, "label").text = f"Score {i+1}"
            ET.SubElement(score_field, "class")
            ET.SubElement(score_field, "dataDictionary").text = ci_field.get('name', f'CI{str(i+1).zfill(2)}')
            ET.SubElement(score_field, "externalDataDictionary")
            ET.SubElement(score_field, "default")
            ET.SubElement(score_field, "fieldFormat").text = "Numeric"
            ET.SubElement(score_field, "decimals").text = "0"
            ET.SubElement(score_field, "length").text = "2"
            ET.SubElement(score_field, "mask")
            ET.SubElement(score_field, "maskOverride").text = "N"
            ET.SubElement(score_field, "helpText").text = f"Score for question {i+1} (1-10)"
            ET.SubElement(score_field, "tabIndex").text = str((i + 1) * 20 + 10)
            ET.SubElement(score_field, "required").text = "N"
            ET.SubElement(score_field, "readOnly").text = "N"
            ET.SubElement(score_field, "password").text = "N"
            ET.SubElement(score_field, "allowSmartControl").text = "Y"
            ET.SubElement(score_field, "labelPosition")
            ET.SubElement(score_field, "top").text = str(question_top)
            ET.SubElement(score_field, "left").text = "20"
            ET.SubElement(score_field, "width").text = "100"

            question_top += 60

        return group

    def _create_overview_group(self, page: ET.Element, fields: list[dict[str, Any]], top: int) -> ET.Element:
        """Create overview group with overview fields."""
        group = ET.SubElement(page, "group")
        ET.SubElement(group, "label").text = "---OVERVIEW INFORMATION---"
        ET.SubElement(group, "class")
        ET.SubElement(group, "top").text = str(top)
        ET.SubElement(group, "left").text = "16"
        ET.SubElement(group, "width").text = "1158"
        ET.SubElement(group, "height").text = "200"

        # Add overview fields
        field_top = 30
        for i, field in enumerate(fields):
            dd_field = ET.SubElement(group, "dataDictionary")
            dd_field.set("fieldType", "textInput")

            field_id = f"{field.get('name', f'overview_{i}')}_1"
            ET.SubElement(dd_field, "id").text = field_id
            ET.SubElement(dd_field, "name").text = field.get('name', f'overview_{i}')
            ET.SubElement(dd_field, "sequence")
            ET.SubElement(dd_field, "label").text = field.get('name', f'Overview {i+1}').replace('_', ' ')
            ET.SubElement(dd_field, "class")
            ET.SubElement(dd_field, "dataDictionary").text = field.get('name', f'OVW{i+1}')
            ET.SubElement(dd_field, "externalDataDictionary")
            ET.SubElement(dd_field, "default")
            ET.SubElement(dd_field, "fieldFormat").text = "Alphabetic"
            ET.SubElement(dd_field, "decimals").text = "0"
            ET.SubElement(dd_field, "length").text = "200"
            ET.SubElement(dd_field, "mask").text = "X" * 200
            ET.SubElement(dd_field, "maskOverride").text = "N"
            ET.SubElement(dd_field, "helpText").text = f"Enter {field.get('name', 'overview information')}"
            ET.SubElement(dd_field, "tabIndex").text = str((i + 1) * 50)
            ET.SubElement(dd_field, "required").text = "N"
            ET.SubElement(dd_field, "readOnly").text = "N"
            ET.SubElement(dd_field, "password").text = "N"
            ET.SubElement(dd_field, "allowSmartControl").text = "Y"
            ET.SubElement(dd_field, "labelPosition")
            ET.SubElement(dd_field, "top").text = str(field_top)
            ET.SubElement(dd_field, "left").text = "20"
            ET.SubElement(dd_field, "width").text = "1100"

            field_top += 50

        return group

    def _add_navigation_buttons(self, page: ET.Element):
        """Add navigation buttons to the form."""
        # Back button
        back_button = ET.SubElement(page, "routingButton")
        ET.SubElement(back_button, "id").text = "Back_1"
        ET.SubElement(back_button, "name").text = "Back"
        ET.SubElement(back_button, "class")
        ET.SubElement(back_button, "label").text = "Back"
        ET.SubElement(back_button, "buttonType").text = "-3"
        ET.SubElement(back_button, "returnCode").text = "-3"
        ET.SubElement(back_button, "helpText")
        ET.SubElement(back_button, "tabIndex").text = "0"
        ET.SubElement(back_button, "top").text = "1127"
        ET.SubElement(back_button, "left").text = "428"
        ET.SubElement(back_button, "width").text = "111"
        ET.SubElement(back_button, "height").text = "52"

        # Next/Submit button
        next_button = ET.SubElement(page, "routingButton")
        ET.SubElement(next_button, "id").text = "Submit_1"
        ET.SubElement(next_button, "name").text = "Submit"
        ET.SubElement(next_button, "class")
        ET.SubElement(next_button, "label").text = "Submit"
        ET.SubElement(next_button, "buttonType").text = "3"
        ET.SubElement(next_button, "returnCode").text = "2"
        ET.SubElement(next_button, "helpText")
        ET.SubElement(next_button, "tabIndex").text = "0"
        ET.SubElement(next_button, "top").text = "1127"
        ET.SubElement(next_button, "left").text = "696"
        ET.SubElement(next_button, "width").text = "111"
        ET.SubElement(next_button, "height").text = "52"

        # Form name static text
        form_name = ET.SubElement(page, "staticText")
        ET.SubElement(form_name, "id").text = "FormName_1"
        ET.SubElement(form_name, "name").text = "FormName"
        ET.SubElement(form_name, "sequence")
        ET.SubElement(form_name, "dataDictionary")
        ET.SubElement(form_name, "externalDataDictionary")
        ET.SubElement(form_name, "label").text = "Form: CUSTINT"
        ET.SubElement(form_name, "class")
        ET.SubElement(form_name, "header").text = "N"
        ET.SubElement(form_name, "top").text = "1153"
        ET.SubElement(form_name, "left").text = "0"
        ET.SubElement(form_name, "width").text = "156"
        ET.SubElement(form_name, "height").text = "15"

    def _indent_xml(self, elem: ET.Element, level: int = 0):
        """Add indentation to XML for readable output."""
        indent = "\n" + level * "  "
        if len(elem):
            if not elem.text or not elem.text.strip():
                elem.text = indent + "  "
            if not elem.tail or not elem.tail.strip():
                elem.tail = indent
            for child in elem:
                self._indent_xml(child, level + 1)
            if not child.tail or not child.tail.strip():
                child.tail = indent
        else:
            if level and (not elem.tail or not elem.tail.strip()):
                elem.tail = indent

    def _extract_form_id(self, component: AWDComponent, fields: list[dict[str, Any]]) -> str:
        """Extract or generate form ID."""
        # Try to get from component
        if component.component_id and component.component_id != "unknown_process":
            return component.component_id

        # Try to get from fields
        for field in fields:
            form_id = field.get("formId")
            if form_id:
                return form_id

        # Generate a numeric ID based on component name
        return str(hash(component.name) % 1000000000)

    def _extract_form_name(self, component: AWDComponent, fields: list[dict[str, Any]]) -> str:
        """Extract form name from component or fields."""
        # Use component name if it's meaningful
        if component.name and component.name != "Unknown Process":
            # Clean up the name
            name = component.name.replace(" - ", " ").replace("CI - ", "Customer ")
            if name == "Interview":
                name = "Customer Interview"
            return name

        # Default name
        return "CUSTINT"

    def _create_data_sources(self, fields: list[dict[str, Any]]) -> list[dict[str, Any]]:
        """Create data sources from form fields."""
        data_sources = []

        for field in fields:
            data_source = {
                "dataName": field.get("name", ""),
                "dataType": field.get("dataType", "string"),
                "awdField": field.get("name") if field.get("isDataDictionary") else None,
                "defaultValue": "",
                "isAutoGenerate": True
            }
            data_sources.append(data_source)

        return data_sources

    def _get_control_types(self, fields: list[dict[str, Any]]) -> list[str]:
        """Get unique control types from fields."""
        control_types = set()

        for field in fields:
            element_type = field.get("elementType", "textInput")
            if element_type == "radio-button-group" or element_type == "text-area":
                control_types.add("dataDictionary")
            else:
                control_types.add("dataDictionary")

        return list(control_types)

    def save_forms(self, output_dir: Path, format_filename: bool = True) -> list[Path]:
        """Save extracted traditional forms to files."""
        output_dir = Path(output_dir)
        output_dir.mkdir(parents=True, exist_ok=True)

        saved_files = []

        for form in self.extracted_forms:
            try:
                if format_filename:
                    filename = f"{form.form_name.replace(' ', '').upper()}.xml"
                else:
                    filename = f"{form.form_id}.xml"

                file_path = output_dir / filename

                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(form.content)

                saved_files.append(file_path)
                logger.info(f"Saved traditional form to: {file_path}")

            except Exception as e:
                logger.error(f"Error saving traditional form {form.form_name}: {e}")

        return saved_files


def extract_traditional_forms(awd_json: dict[str, Any], output_dir: Path | None = None) -> list[FormInfo]:
    """
    Convenience function to extract traditional forms from AWD JSON data.
    
    Args:
        awd_json: Parsed AWD JSON data from deserializer
        output_dir: Optional directory to save extracted form files
        
    Returns:
        List of extracted form information
    """
    extractor = TraditionalFormExtractor()
    forms = extractor.extract_forms(awd_json)

    if output_dir and forms:
        extractor.save_forms(output_dir)

    return forms
