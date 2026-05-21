/**
 * Module: FormValidationEngineTest - Unit tests for form validation engine
 * 
 * Summary:
 *     Comprehensive unit tests for FormValidationEngine covering form integrity
 *     validation, JSON schema compliance, field validation, dation compatibility
 *     checks, and validation rule verification. Tests ensure proper error detection,
 *     warning generation, and validation result accuracy.
 * 
 * Test Coverage:
 *     - Form integrity and completeness validation
 *     - Individual field validation scenarios
 *     - Validation rule syntax and logic verification
 *     - Dation format compliance checking
 *     - AWD expression validation
 */
package com.patientvibes.awd.deserializer.extractor;

import com.patientvibes.awd.deserializer.extractor.model.UxFormDefinition;
import com.patientvibes.awd.deserializer.extractor.model.FormField;
import com.patientvibes.awd.deserializer.extractor.model.ValidationRule;
import com.patientvibes.awd.deserializer.extractor.model.DationMetadata;
import com.patientvibes.awd.deserializer.extractor.FormValidationEngine.ValidationResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FormValidationEngine.
 */
public class FormValidationEngineTest {
    
    private FormValidationEngine validationEngine;
    private UxFormDefinition validFormDefinition;
    
    @BeforeEach
    void setUp() {
        validationEngine = new FormValidationEngine();
        validFormDefinition = createValidFormDefinition();
    }
    
    @Test
    @DisplayName("Should validate valid form successfully")
    void testValidateValidForm() {
        // Act
        ValidationResult result = validationEngine.validateForm(validFormDefinition);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.isValid());
        assertFalse(result.hasErrors());
        assertEquals(0, result.getErrors().size());
    }
    
    @Test
    @DisplayName("Should detect null form definition")
    void testValidateNullForm() {
        // Act
        ValidationResult result = validationEngine.validateForm(null);
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("cannot be null"));
    }
    
    @Test
    @DisplayName("Should detect missing form ID")
    void testValidateMissingFormId() {
        // Arrange
        validFormDefinition.setFormId(null);
        
        // Act
        ValidationResult result = validationEngine.validateForm(validFormDefinition);
        
        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().stream()
            .anyMatch(error -> error.contains("Form ID cannot be null")));
    }
    
    @Test
    @DisplayName("Should detect empty form ID")
    void testValidateEmptyFormId() {
        // Arrange
        validFormDefinition.setFormId("");
        
        // Act
        ValidationResult result = validationEngine.validateForm(validFormDefinition);
        
        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().stream()
            .anyMatch(error -> error.contains("Form ID cannot be null or empty")));
    }
    
    @Test
    @DisplayName("Should validate form with no fields")
    void testValidateFormWithNoFields() {
        // Arrange
        validFormDefinition.setFields(new ArrayList<>());
        
        // Act
        ValidationResult result = validationEngine.validateForm(validFormDefinition);
        
        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().stream()
            .anyMatch(error -> error.contains("must have at least one field")));
    }
    
    @Test
    @DisplayName("Should detect duplicate field names")
    void testValidateDuplicateFieldNames() {
        // Arrange
        FormField duplicateField = new FormField();
        duplicateField.setName("customerName"); // Same as existing field
        duplicateField.setType("text");
        duplicateField.setLabel("Duplicate Field");
        validFormDefinition.addField(duplicateField);
        
        // Act
        ValidationResult result = validationEngine.validateForm(validFormDefinition);
        
        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().stream()
            .anyMatch(error -> error.contains("Duplicate field name")));
    }
    
    @Test
    @DisplayName("Should validate individual field correctly")
    void testValidateField() {
        // Arrange
        FormField validField = createValidFormField();
        
        // Act
        ValidationResult result = validationEngine.validateField(validField);
        
        // Assert
        assertTrue(result.isValid());
        assertFalse(result.hasErrors());
    }
    
    @Test
    @DisplayName("Should detect invalid field name")
    void testValidateInvalidFieldName() {
        // Arrange
        FormField invalidField = createValidFormField();
        invalidField.setName("invalid-field-name!"); // Contains invalid characters
        
        // Act
        ValidationResult result = validationEngine.validateField(invalidField);
        
        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasWarnings());
        assertTrue(result.getWarnings().stream()
            .anyMatch(warning -> warning.contains("should follow naming convention")));
    }
    
    @Test
    @DisplayName("Should detect missing field type")
    void testValidateMissingFieldType() {
        // Arrange
        FormField invalidField = createValidFormField();
        invalidField.setType(null);
        
        // Act
        ValidationResult result = validationEngine.validateField(invalidField);
        
        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().stream()
            .anyMatch(error -> error.contains("Field type cannot be null")));
    }
    
    @Test
    @DisplayName("Should validate AWD mapping expressions")
    void testValidateAwdMapping() {
        // Arrange
        FormField fieldWithValidMapping = createValidFormField();
        fieldWithValidMapping.setAwdMapping("awd:awd-value('customer.name')");
        
        FormField fieldWithInvalidMapping = createValidFormField();
        fieldWithInvalidMapping.setName("invalidMappingField");
        fieldWithInvalidMapping.setAwdMapping("invalid-awd-expression");
        
        // Act
        ValidationResult validResult = validationEngine.validateField(fieldWithValidMapping);
        ValidationResult invalidResult = validationEngine.validateField(fieldWithInvalidMapping);
        
        // Assert
        assertTrue(validResult.isValid());
        
        assertTrue(invalidResult.hasWarnings());
        assertTrue(invalidResult.getWarnings().stream()
            .anyMatch(warning -> warning.contains("AWD mapping may not be valid")));
    }
    
    @Test
    @DisplayName("Should validate regex patterns in validation rules")
    void testValidateRegexPatterns() {
        // Arrange
        FormField fieldWithInvalidRegex = createValidFormField();
        fieldWithInvalidRegex.setValidation("pattern:[invalid-regex"); // Missing closing bracket
        
        // Act
        ValidationResult result = validationEngine.validateField(fieldWithInvalidRegex);
        
        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().stream()
            .anyMatch(error -> error.contains("Invalid regex pattern")));
    }
    
    @Test
    @DisplayName("Should validate numeric constraints")
    void testValidateNumericConstraints() {
        // Arrange
        FormField fieldWithInvalidNumeric = createValidFormField();
        fieldWithInvalidNumeric.setValidation("minLength:not-a-number");
        
        // Act
        ValidationResult result = validationEngine.validateField(fieldWithInvalidNumeric);
        
        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().stream()
            .anyMatch(error -> error.contains("Invalid numeric constraint")));
    }
    
    @Test
    @DisplayName("Should validate dation metadata")
    void testValidateDationMetadata() {
        // Arrange - Remove required metadata
        validFormDefinition.setDationMetadata(null);
        
        // Act
        ValidationResult result = validationEngine.validateForm(validFormDefinition);
        
        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().stream()
            .anyMatch(error -> error.contains("Dation metadata is required")));
    }
    
    @Test
    @DisplayName("Should validate dation format")
    void testValidateDationFormat() {
        // Arrange - Valid dation format
        Map<String, Object> validDationForm = createValidDationForm();
        
        // Act
        ValidationResult result = validationEngine.validateDationFormat(validDationForm);
        
        // Assert
        assertTrue(result.isValid());
        assertFalse(result.hasErrors());
    }
    
    @Test
    @DisplayName("Should detect missing required dation fields")
    void testValidateMissingDationFields() {
        // Arrange
        Map<String, Object> invalidDationForm = createValidDationForm();
        invalidDationForm.remove("form_id");
        
        // Act
        ValidationResult result = validationEngine.validateDationFormat(invalidDationForm);
        
        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().stream()
            .anyMatch(error -> error.contains("Required dation field missing: form_id")));
    }
    
    @Test
    @DisplayName("Should validate field options for choice fields")
    void testValidateFieldOptions() {
        // Arrange
        FormField choiceFieldWithoutOptions = new FormField();
        choiceFieldWithoutOptions.setName("choiceField");
        choiceFieldWithoutOptions.setType("radio");
        choiceFieldWithoutOptions.setLabel("Choice Field");
        
        // Act
        ValidationResult result = validationEngine.validateField(choiceFieldWithoutOptions);
        
        // Assert
        assertTrue(result.hasWarnings());
        assertTrue(result.getWarnings().stream()
            .anyMatch(warning -> warning.contains("should have options defined")));
    }
    
    @Test
    @DisplayName("Should detect duplicate options")
    void testValidateDuplicateOptions() {
        // Arrange
        FormField fieldWithDuplicateOptions = createValidFormField();
        fieldWithDuplicateOptions.setType("radio");
        fieldWithDuplicateOptions.setOptions(Arrays.asList("Option1", "Option2", "Option1")); // Duplicate
        
        // Act
        ValidationResult result = validationEngine.validateField(fieldWithDuplicateOptions);
        
        // Assert
        assertTrue(result.hasWarnings());
        assertTrue(result.getWarnings().stream()
            .anyMatch(warning -> warning.contains("duplicate options")));
    }
    
    @Test
    @DisplayName("Should check form validity quickly")
    void testIsFormValid() {
        // Act & Assert - Valid form
        assertTrue(validationEngine.isFormValid(validFormDefinition));
        
        // Act & Assert - Invalid form (null)
        assertFalse(validationEngine.isFormValid(null));
        
        // Act & Assert - Invalid form (no fields)
        UxFormDefinition invalidForm = createValidFormDefinition();
        invalidForm.setFields(new ArrayList<>());
        assertFalse(validationEngine.isFormValid(invalidForm));
    }
    
    @Test
    @DisplayName("Should validate JSON schema structure")
    void testValidateAgainstJsonSchema() {
        // Arrange
        Map<String, Object> formData = new HashMap<>();
        formData.put("formId", "test");
        formData.put("formName", "Test Form");
        formData.put("fields", Arrays.asList(
            Map.of("name", "field1", "type", "text")
        ));
        
        String schema = """
            {
                "type": "object",
                "required": ["formId", "formName", "fields"],
                "properties": {
                    "formId": {"type": "string"},
                    "formName": {"type": "string"},
                    "fields": {"type": "array"}
                }
            }
            """;
        
        // Act
        ValidationResult result = validationEngine.validateAgainstJsonSchema(formData, schema);
        
        // Assert
        assertTrue(result.isValid());
    }
    
    @Test
    @DisplayName("Should handle invalid JSON schema")
    void testValidateAgainstInvalidJsonSchema() {
        // Arrange
        Map<String, Object> formData = new HashMap<>();
        String invalidSchema = "invalid-json";
        
        // Act
        ValidationResult result = validationEngine.validateAgainstJsonSchema(formData, invalidSchema);
        
        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().stream()
            .anyMatch(error -> error.contains("Failed to parse JSON schema")));
    }
    
    // Helper methods
    
    private UxFormDefinition createValidFormDefinition() {
        UxFormDefinition form = new UxFormDefinition();
        form.setFormId("validForm");
        form.setFormName("Valid Form");
        form.setFormType("awd_user_task");
        form.setAwdElementId("userTask_valid");
        
        // Add valid field
        FormField validField = createValidFormField();
        form.addField(validField);
        
        // Add valid validation rule
        ValidationRule validRule = new ValidationRule();
        validRule.setFieldName("customerName");
        validRule.setRuleType("required");
        validRule.setErrorMessage("Customer name is required");
        form.addValidationRule(validRule);
        
        // Add valid dation metadata
        DationMetadata metadata = new DationMetadata();
        metadata.setSourceSystem("awd");
        metadata.setExtractionTimestamp(LocalDateTime.now().toString());
        metadata.setAwdElementId("userTask_valid");
        metadata.setElementType("userTask");
        form.setDationMetadata(metadata);
        
        return form;
    }
    
    private FormField createValidFormField() {
        FormField field = new FormField();
        field.setName("customerName");
        field.setType("text");
        field.setLabel("Customer Name");
        field.setValidation("required|minLength:2");
        field.setAwdMapping("awd:awd-value('customer.name')");
        field.setHtmlControl("HtmlTextInput");
        field.setRequired(true);
        
        return field;
    }
    
    private Map<String, Object> createValidDationForm() {
        Map<String, Object> dationForm = new HashMap<>();
        dationForm.put("form_id", "valid_form");
        dationForm.put("form_name", "Valid Form");
        dationForm.put("form_type", "awd_user_task");
        
        // Add fields
        List<Map<String, Object>> fields = new ArrayList<>();
        Map<String, Object> field = new HashMap<>();
        field.put("field_name", "customer_name");
        field.put("field_type", "string");
        fields.add(field);
        dationForm.put("fields", fields);
        
        // Add dation metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source_system", "awd");
        metadata.put("extraction_timestamp", LocalDateTime.now().toString());
        metadata.put("awd_element_id", "userTask_valid");
        dationForm.put("dation_metadata", metadata);
        
        return dationForm;
    }
}