/**
 * Module: DationFormattingServiceTest - Unit tests for dation formatting service
 * 
 * Summary:
 *     Comprehensive unit tests for DationFormattingService covering AWD form data
 *     transformation to dation-compatible JSON format, field naming conventions,
 *     metadata enhancement, and multi-format output generation. Tests validate
 *     proper snake_case conversion, metadata structure, and compatibility checks.
 * 
 * Test Coverage:
 *     - Dation format transformation accuracy
 *     - Field naming convention compliance
 *     - Metadata enhancement and validation
 *     - JSON/XML output generation
 *     - Format validation and compatibility
 */
package com.patientvibes.awd.deserializer.extractor;

import com.patientvibes.awd.deserializer.extractor.model.UxFormDefinition;
import com.patientvibes.awd.deserializer.extractor.model.FormField;
import com.patientvibes.awd.deserializer.extractor.model.ValidationRule;
import com.patientvibes.awd.deserializer.extractor.model.DationMetadata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DationFormattingService.
 */
public class DationFormattingServiceTest {
    
    private DationFormattingService formattingService;
    private UxFormDefinition testFormDefinition;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        formattingService = new DationFormattingService();
        testFormDefinition = createTestFormDefinition();
    }
    
    @Test
    @DisplayName("Should format form for dation compatibility")
    void testFormatForDation() {
        // Act
        Map<String, Object> dationForm = formattingService.formatForDation(testFormDefinition);
        
        // Assert
        assertNotNull(dationForm);
        
        // Check required dation fields
        assertTrue(dationForm.containsKey("form_id"));
        assertTrue(dationForm.containsKey("form_name"));
        assertTrue(dationForm.containsKey("form_type"));
        assertTrue(dationForm.containsKey("fields"));
        assertTrue(dationForm.containsKey("dation_metadata"));
        
        // Verify field naming convention (snake_case)
        assertEquals("test_form_123", dationForm.get("form_id"));
        assertEquals("Test Form", dationForm.get("form_name"));
        assertEquals("awd_user_task", dationForm.get("form_type"));
    }
    
    @Test
    @DisplayName("Should transform fields to dation format")
    void testFieldTransformation() {
        // Act
        Map<String, Object> dationForm = formattingService.formatForDation(testFormDefinition);
        
        // Assert
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fields = (List<Map<String, Object>>) dationForm.get("fields");
        
        assertNotNull(fields);
        assertFalse(fields.isEmpty());
        
        Map<String, Object> firstField = fields.get(0);
        
        // Check dation field structure
        assertTrue(firstField.containsKey("field_name"));
        assertTrue(firstField.containsKey("field_type"));
        assertTrue(firstField.containsKey("field_label"));
        
        // Verify field naming convention
        assertEquals("customer_name", firstField.get("field_name"));
        assertEquals("string", firstField.get("field_type")); // text -> string
        assertEquals("Customer Name", firstField.get("field_label"));
    }
    
    @Test
    @DisplayName("Should handle validation rules transformation")
    void testValidationRulesTransformation() {
        // Act
        Map<String, Object> dationForm = formattingService.formatForDation(testFormDefinition);
        
        // Assert
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> validationRules = (List<Map<String, Object>>) dationForm.get("validation_rules");
        
        assertNotNull(validationRules);
        assertFalse(validationRules.isEmpty());
        
        Map<String, Object> firstRule = validationRules.get(0);
        
        // Check validation rule structure
        assertTrue(firstRule.containsKey("field_name"));
        assertTrue(firstRule.containsKey("rule_type"));
        assertTrue(firstRule.containsKey("rule_enabled"));
        assertTrue(firstRule.containsKey("javascript_validation"));
        
        assertEquals("customer_name", firstRule.get("field_name"));
        assertEquals("required", firstRule.get("rule_type"));
        assertEquals(true, firstRule.get("rule_enabled"));
    }
    
    @Test
    @DisplayName("Should enhance dation metadata")
    void testDationMetadataEnhancement() {
        // Act
        Map<String, Object> dationForm = formattingService.formatForDation(testFormDefinition);
        
        // Assert
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) dationForm.get("dation_metadata");
        
        assertNotNull(metadata);
        
        // Check original metadata preservation
        assertEquals("awd", metadata.get("source_system"));
        assertTrue(metadata.containsKey("extraction_timestamp"));
        assertEquals("userTask_test", metadata.get("awd_element_id"));
        
        // Check dation enhancements
        assertEquals("1.0", metadata.get("dation_format_version"));
        assertTrue(metadata.containsKey("transformation_timestamp"));
        assertEquals("dation_v1", metadata.get("format_compliance"));
    }
    
    @Test
    @DisplayName("Should generate form statistics")
    void testFormStatistics() {
        // Act
        Map<String, Object> dationForm = formattingService.formatForDation(testFormDefinition);
        
        // Assert
        @SuppressWarnings("unchecked")
        Map<String, Object> stats = (Map<String, Object>) dationForm.get("form_statistics");
        
        assertNotNull(stats);
        
        // Check statistics content
        assertEquals(2, stats.get("total_fields"));
        assertEquals(1, stats.get("required_fields"));
        assertEquals(1, stats.get("optional_fields"));
        assertTrue(stats.containsKey("field_types"));
        assertTrue(stats.containsKey("total_validation_rules"));
        assertTrue(stats.containsKey("fields_with_awd_mapping"));
    }
    
    @Test
    @DisplayName("Should include compatibility information")
    void testCompatibilityInformation() {
        // Act
        Map<String, Object> dationForm = formattingService.formatForDation(testFormDefinition);
        
        // Assert
        @SuppressWarnings("unchecked")
        Map<String, Object> compatibility = (Map<String, Object>) dationForm.get("dation_compatibility");
        
        assertNotNull(compatibility);
        
        assertEquals(true, compatibility.get("dation_compatible"));
        assertEquals(true, compatibility.get("python_ecosystem_ready"));
        assertEquals(true, compatibility.get("json_serializable"));
        assertEquals("snake_case", compatibility.get("field_naming_convention"));
        assertEquals("UTF-8", compatibility.get("encoding"));
        
        @SuppressWarnings("unchecked")
        List<String> formats = (List<String>) compatibility.get("supported_formats");
        assertTrue(formats.contains("json"));
        assertTrue(formats.contains("xml"));
    }
    
    @Test
    @DisplayName("Should generate valid JSON output")
    void testGenerateJsonOutput() throws IOException {
        // Act
        String jsonOutput = formattingService.generateJsonOutput(testFormDefinition);
        
        // Assert
        assertNotNull(jsonOutput);
        assertFalse(jsonOutput.trim().isEmpty());
        
        // Verify JSON structure
        assertTrue(jsonOutput.contains("\"form_id\""));
        assertTrue(jsonOutput.contains("\"dation_metadata\""));
        assertTrue(jsonOutput.contains("\"fields\""));
        
        // Check formatting (should be pretty-printed)
        assertTrue(jsonOutput.contains("\n"));
        assertTrue(jsonOutput.contains("  ")); // Indentation
    }
    
    @Test
    @DisplayName("Should generate XML output")
    void testGenerateXmlOutput() throws IOException {
        // Act
        String xmlOutput = formattingService.generateXmlOutput(testFormDefinition);
        
        // Assert
        assertNotNull(xmlOutput);
        assertFalse(xmlOutput.trim().isEmpty());
        
        // Verify XML structure
        assertTrue(xmlOutput.contains("<?xml version=\"1.0\""));
        assertTrue(xmlOutput.contains("<dation_form>"));
        assertTrue(xmlOutput.contains("</dation_form>"));
        assertTrue(xmlOutput.contains("<![CDATA["));
    }
    
    @Test
    @DisplayName("Should save dation form to file")
    void testSaveDationForm() throws IOException {
        // Act - Save as JSON
        String jsonFilePath = formattingService.saveDationForm(testFormDefinition, tempDir, "json");
        
        // Assert
        assertNotNull(jsonFilePath);
        assertTrue(Files.exists(Path.of(jsonFilePath)));
        assertTrue(jsonFilePath.endsWith(".json"));
        
        String content = Files.readString(Path.of(jsonFilePath));
        assertTrue(content.contains("\"form_id\""));
        
        // Act - Save as XML
        String xmlFilePath = formattingService.saveDationForm(testFormDefinition, tempDir, "xml");
        
        // Assert
        assertNotNull(xmlFilePath);
        assertTrue(Files.exists(Path.of(xmlFilePath)));
        assertTrue(xmlFilePath.endsWith(".xml"));
        
        String xmlContent = Files.readString(Path.of(xmlFilePath));
        assertTrue(xmlContent.contains("<dation_form>"));
    }
    
    @Test
    @DisplayName("Should validate dation format")
    void testValidateDationFormat() {
        // Arrange
        Map<String, Object> validDationForm = formattingService.formatForDation(testFormDefinition);
        
        // Act & Assert - Valid format
        assertTrue(formattingService.validateDationFormat(validDationForm));
        
        // Test invalid format - missing required field
        Map<String, Object> invalidForm = new HashMap<>(validDationForm);
        invalidForm.remove("form_id");
        assertFalse(formattingService.validateDationFormat(invalidForm));
        
        // Test null input
        assertFalse(formattingService.validateDationFormat(null));
    }
    
    @Test
    @DisplayName("Should handle null form definition")
    void testHandleNullFormDefinition() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            formattingService.formatForDation(null));
    }
    
    @Test
    @DisplayName("Should handle form with no fields")
    void testHandleFormWithNoFields() {
        // Arrange
        UxFormDefinition emptyForm = new UxFormDefinition();
        emptyForm.setFormId("empty_form");
        emptyForm.setFormName("Empty Form");
        emptyForm.setFormType("awd_user_task");
        emptyForm.setDationMetadata(createTestDationMetadata());
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            formattingService.formatForDation(emptyForm));
    }
    
    @Test
    @DisplayName("Should sanitize field names for dation compatibility")
    void testFieldNameSanitization() {
        // Arrange
        UxFormDefinition formWithSpecialChars = createTestFormDefinition();
        FormField specialField = new FormField();
        specialField.setName("field-with-special@chars!");
        specialField.setType("text");
        specialField.setLabel("Special Field");
        formWithSpecialChars.addField(specialField);
        
        // Act
        Map<String, Object> dationForm = formattingService.formatForDation(formWithSpecialChars);
        
        // Assert
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fields = (List<Map<String, Object>>) dationForm.get("fields");
        
        // Find the sanitized field
        Optional<Map<String, Object>> sanitizedField = fields.stream()
            .filter(f -> "field_with_special_chars".equals(f.get("field_name")))
            .findFirst();
        
        assertTrue(sanitizedField.isPresent());
    }
    
    // Helper methods
    
    private UxFormDefinition createTestFormDefinition() {
        UxFormDefinition form = new UxFormDefinition();
        form.setFormId("testForm123");
        form.setFormName("Test Form");
        form.setFormType("awd_user_task");
        form.setAwdElementId("userTask_test");
        
        // Add fields
        FormField field1 = new FormField();
        field1.setName("customerName");
        field1.setType("text");
        field1.setLabel("Customer Name");
        field1.setValidation("required|minLength:2");
        field1.setAwdMapping("awd:awd-value('customer.name')");
        field1.setHtmlControl("HtmlTextInput");
        field1.setRequired(true);
        form.addField(field1);
        
        FormField field2 = new FormField();
        field2.setName("emailAddress");
        field2.setType("email");
        field2.setLabel("Email Address");
        field2.setAwdMapping("awd:awd-value('customer.email')");
        field2.setHtmlControl("HtmlEmailInput");
        field2.setRequired(false);
        form.addField(field2);
        
        // Add validation rules
        ValidationRule rule1 = new ValidationRule();
        rule1.setFieldName("customerName");
        rule1.setRuleType("required");
        rule1.setErrorMessage("Customer name is required");
        rule1.setPriority(1);
        rule1.setEnabled(true);
        form.addValidationRule(rule1);
        
        // Add dation metadata
        form.setDationMetadata(createTestDationMetadata());
        
        return form;
    }
    
    private DationMetadata createTestDationMetadata() {
        DationMetadata metadata = new DationMetadata();
        metadata.setSourceSystem("awd");
        metadata.setExtractionTimestamp(LocalDateTime.now().toString());
        metadata.setAwdElementId("userTask_test");
        metadata.setElementType("userTask");
        metadata.setProcessId("test_process");
        metadata.setExtractorVersion("2.4.0");
        
        return metadata;
    }
}