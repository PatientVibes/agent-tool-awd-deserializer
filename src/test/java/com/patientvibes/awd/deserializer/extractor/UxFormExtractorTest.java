/**
 * Module: UxFormExtractorTest - Comprehensive unit tests for UX form extraction
 * 
 * Summary:
 *     Unit tests for UxFormExtractor covering form extraction from AWD workflow elements,
 *     dation-compatible JSON output generation, and AWD form control mapping scenarios.
 *     Tests include validation of individual form generation, field mapping accuracy,
 *     validation rule extraction, and dation metadata consistency.
 * 
 * Key Test Areas:
 *     - AWD workflow element form extraction
 *     - Dation-compatible JSON output validation
 *     - AWD HtmlControl mapping accuracy
 *     - Form field validation rule extraction
 *     - Parallel extraction performance testing
 * 
 * Keywords: test, ux, form, extractor, awd, workflow, dation, json, mapping,
 *          validation, extraction, htmlcontrol, metadata, parallel, performance
 */
package com.patientvibes.awd.deserializer.extractor;

import com.patientvibes.awd.deserializer.business.model.BusinessMetadata;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for UxFormExtractor functionality.
 */
public class UxFormExtractorTest {
    
    private UxFormExtractor extractor;
    private BusinessMetadata testMetadata;
    private Map<String, Object> testMappedData;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        extractor = new UxFormExtractor();
        testMetadata = createTestBusinessMetadata();
        testMappedData = createTestMappedData();
    }
    
    @Test
    @DisplayName("Should extract forms from AWD workflow elements")
    void testExtractUxForms() throws IOException {
        // Act
        List<String> generatedFiles = extractor.extractUxForms(testMetadata, testMappedData, tempDir);
        
        // Assert
        assertNotNull(generatedFiles);
        assertFalse(generatedFiles.isEmpty());
        assertEquals(2, generatedFiles.size()); // Based on test data with 2 user tasks
        
        // Verify files were created
        for (String filePath : generatedFiles) {
            assertTrue(Files.exists(Path.of(filePath)));
            assertTrue(Files.size(Path.of(filePath)) > 0);
        }
    }
    
    @Test
    @DisplayName("Should handle null inputs gracefully")
    void testExtractUxForms_NullInputs() {
        // Test null metadata
        assertThrows(IllegalArgumentException.class, () -> 
            extractor.extractUxForms(null, testMappedData, tempDir));
        
        // Test null mapped data
        assertThrows(IllegalArgumentException.class, () -> 
            extractor.extractUxForms(testMetadata, null, tempDir));
        
        // Test null output directory
        assertThrows(IllegalArgumentException.class, () -> 
            extractor.extractUxForms(testMetadata, testMappedData, null));
    }
    
    @Test
    @DisplayName("Should handle empty workflow elements")
    void testExtractUxForms_EmptyWorkflow() throws IOException {
        // Arrange
        Map<String, Object> emptyMappedData = createEmptyMappedData();
        
        // Act
        List<String> generatedFiles = extractor.extractUxForms(testMetadata, emptyMappedData, tempDir);
        
        // Assert
        assertNotNull(generatedFiles);
        assertTrue(generatedFiles.isEmpty());
    }
    
    @Test
    @DisplayName("Should correctly map AWD form controls to UX types")
    void testAwdControlMapping() throws IOException {
        // Arrange
        Map<String, Object> mappedDataWithControls = createMappedDataWithFormControls();
        
        // Act
        List<String> generatedFiles = extractor.extractUxForms(testMetadata, mappedDataWithControls, tempDir);
        
        // Assert
        assertFalse(generatedFiles.isEmpty());
        
        // Read and verify the generated JSON
        String jsonContent = Files.readString(Path.of(generatedFiles.get(0)));
        assertTrue(jsonContent.contains("\"field_type\":\"string\""));  // HtmlTextInput -> string
        assertTrue(jsonContent.contains("\"field_type\":\"single_choice\"")); // HtmlRadioGroup -> single_choice
        assertTrue(jsonContent.contains("\"original_html_control\":\"HtmlTextInput\""));
    }
    
    @Test
    @DisplayName("Should generate valid dation metadata")
    void testDationMetadataGeneration() throws IOException {
        // Act
        List<String> generatedFiles = extractor.extractUxForms(testMetadata, testMappedData, tempDir);
        
        // Assert
        assertFalse(generatedFiles.isEmpty());
        
        String jsonContent = Files.readString(Path.of(generatedFiles.get(0)));
        
        // Verify dation metadata structure
        assertTrue(jsonContent.contains("\"dation_metadata\""));
        assertTrue(jsonContent.contains("\"source_system\":\"awd\""));
        assertTrue(jsonContent.contains("\"extraction_timestamp\""));
        assertTrue(jsonContent.contains("\"awd_element_id\""));
    }
    
    @Test
    @DisplayName("Should extract validation rules correctly")
    void testValidationRuleExtraction() throws IOException {
        // Arrange
        Map<String, Object> mappedDataWithValidation = createMappedDataWithValidation();
        
        // Act
        List<String> generatedFiles = extractor.extractUxForms(testMetadata, mappedDataWithValidation, tempDir);
        
        // Assert
        assertFalse(generatedFiles.isEmpty());
        
        String jsonContent = Files.readString(Path.of(generatedFiles.get(0)));
        
        // Verify validation rules
        assertTrue(jsonContent.contains("\"validation_rules\""));
        assertTrue(jsonContent.contains("\"rule_type\":\"required\""));
        assertTrue(jsonContent.contains("\"error_message\""));
    }
    
    @Test
    @DisplayName("Should generate unique file names")
    void testUniqueFileNames() throws IOException {
        // Act - Run extraction twice
        List<String> firstRun = extractor.extractUxForms(testMetadata, testMappedData, tempDir);
        
        // Wait to ensure different timestamps
        try { Thread.sleep(1000); } catch (InterruptedException e) { /* ignore */ }
        
        List<String> secondRun = extractor.extractUxForms(testMetadata, testMappedData, tempDir);
        
        // Assert
        assertFalse(firstRun.isEmpty());
        assertFalse(secondRun.isEmpty());
        
        // File names should be different due to timestamps
        Set<String> allFiles = new HashSet<>();
        allFiles.addAll(firstRun);
        allFiles.addAll(secondRun);
        
        assertEquals(firstRun.size() + secondRun.size(), allFiles.size());
    }
    
    @Test
    @DisplayName("Should handle complex form structures")
    void testComplexFormStructures() throws IOException {
        // Arrange
        Map<String, Object> complexMappedData = createComplexMappedData();
        
        // Act
        List<String> generatedFiles = extractor.extractUxForms(testMetadata, complexMappedData, tempDir);
        
        // Assert
        assertFalse(generatedFiles.isEmpty());
        
        String jsonContent = Files.readString(Path.of(generatedFiles.get(0)));
        
        // Verify complex structure handling
        assertTrue(jsonContent.contains("\"field_options\""));
        assertTrue(jsonContent.contains("\"awd_mapping\""));
        assertTrue(jsonContent.contains("awd:awd-value"));
    }
    
    @Test
    @DisplayName("Should shutdown cleanly")
    void testShutdown() {
        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> extractor.shutdown());
    }
    
    // Helper methods for creating test data
    
    private BusinessMetadata createTestBusinessMetadata() {
        BusinessMetadata metadata = new BusinessMetadata();
        metadata.addBusinessField("processName", "Test Process");
        metadata.addBusinessField("processId", "test_process_123");
        return metadata;
    }
    
    private Map<String, Object> createTestMappedData() {
        Map<String, Object> mappedData = new HashMap<>();
        
        // Create workflow data with user tasks
        Map<String, Object> workflowData = new HashMap<>();
        List<Map<String, Object>> elements = new ArrayList<>();
        
        // User task 1
        Map<String, Object> userTask1 = new HashMap<>();
        userTask1.put("id", "userTask_1");
        userTask1.put("name", "Customer Entry");
        userTask1.put("type", "userTask");
        
        Map<String, Object> properties1 = new HashMap<>();
        properties1.put("formKey", "customerForm");
        
        Map<String, Object> formConfig1 = new HashMap<>();
        List<Map<String, Object>> fields1 = new ArrayList<>();
        
        Map<String, Object> field1 = new HashMap<>();
        field1.put("name", "customerName");
        field1.put("type", "HtmlTextInput");
        field1.put("label", "Customer Name");
        field1.put("validation", "required|minLength:2");
        field1.put("mapping", "awd:awd-value('customer.name')");
        fields1.add(field1);
        
        formConfig1.put("fields", fields1);
        properties1.put("formConfig", formConfig1);
        userTask1.put("properties", properties1);
        elements.add(userTask1);
        
        // User task 2
        Map<String, Object> userTask2 = new HashMap<>();
        userTask2.put("id", "userTask_2");
        userTask2.put("name", "Priority Selection");
        userTask2.put("type", "userTask");
        
        Map<String, Object> properties2 = new HashMap<>();
        properties2.put("formKey", "priorityForm");
        userTask2.put("properties", properties2);
        elements.add(userTask2);
        
        workflowData.put("elements", elements);
        mappedData.put("workflow", workflowData);
        
        return mappedData;
    }
    
    private Map<String, Object> createEmptyMappedData() {
        Map<String, Object> mappedData = new HashMap<>();
        Map<String, Object> workflowData = new HashMap<>();
        workflowData.put("elements", new ArrayList<>());
        mappedData.put("workflow", workflowData);
        return mappedData;
    }
    
    private Map<String, Object> createMappedDataWithFormControls() {
        Map<String, Object> mappedData = new HashMap<>();
        Map<String, Object> workflowData = new HashMap<>();
        List<Map<String, Object>> elements = new ArrayList<>();
        
        Map<String, Object> userTask = new HashMap<>();
        userTask.put("id", "userTask_controls");
        userTask.put("name", "Control Test Form");
        userTask.put("type", "userTask");
        
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> formConfig = new HashMap<>();
        List<Map<String, Object>> fields = new ArrayList<>();
        
        // Text input field
        Map<String, Object> textField = new HashMap<>();
        textField.put("name", "textField");
        textField.put("type", "HtmlTextInput");
        textField.put("label", "Text Field");
        fields.add(textField);
        
        // Radio group field
        Map<String, Object> radioField = new HashMap<>();
        radioField.put("name", "radioField");
        radioField.put("type", "HtmlRadioGroup");
        radioField.put("label", "Radio Field");
        radioField.put("options", Arrays.asList("Option1", "Option2", "Option3"));
        fields.add(radioField);
        
        formConfig.put("fields", fields);
        properties.put("formConfig", formConfig);
        userTask.put("properties", properties);
        elements.add(userTask);
        
        workflowData.put("elements", elements);
        mappedData.put("workflow", workflowData);
        
        return mappedData;
    }
    
    private Map<String, Object> createMappedDataWithValidation() {
        Map<String, Object> mappedData = new HashMap<>();
        Map<String, Object> workflowData = new HashMap<>();
        List<Map<String, Object>> elements = new ArrayList<>();
        
        Map<String, Object> userTask = new HashMap<>();
        userTask.put("id", "userTask_validation");
        userTask.put("name", "Validation Test Form");
        userTask.put("type", "userTask");
        
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> formConfig = new HashMap<>();
        List<Map<String, Object>> fields = new ArrayList<>();
        
        Map<String, Object> field = new HashMap<>();
        field.put("name", "validatedField");
        field.put("type", "HtmlTextInput");
        field.put("label", "Validated Field");
        field.put("validation", "required|minLength:5|maxLength:100");
        field.put("mapping", "awd:awd-value('validated.field')");
        fields.add(field);
        
        formConfig.put("fields", fields);
        properties.put("formConfig", formConfig);
        userTask.put("properties", properties);
        elements.add(userTask);
        
        workflowData.put("elements", elements);
        mappedData.put("workflow", workflowData);
        
        return mappedData;
    }
    
    private Map<String, Object> createComplexMappedData() {
        Map<String, Object> mappedData = new HashMap<>();
        Map<String, Object> workflowData = new HashMap<>();
        List<Map<String, Object>> elements = new ArrayList<>();
        
        Map<String, Object> userTask = new HashMap<>();
        userTask.put("id", "userTask_complex");
        userTask.put("name", "Complex Form");
        userTask.put("type", "userTask");
        userTask.put("processId", "complex_process");
        
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> formConfig = new HashMap<>();
        List<Map<String, Object>> fields = new ArrayList<>();
        
        // Complex field with options and AWD mapping
        Map<String, Object> complexField = new HashMap<>();
        complexField.put("name", "complexField");
        complexField.put("type", "HtmlRadioGroup");
        complexField.put("label", "Complex Field");
        complexField.put("options", Arrays.asList("LOW", "MEDIUM", "HIGH"));
        complexField.put("mapping", "awd:awd-value('complex.priority', '//case/priority')");
        complexField.put("placeholder", "Select priority level");
        fields.add(complexField);
        
        formConfig.put("fields", fields);
        properties.put("formConfig", formConfig);
        userTask.put("properties", properties);
        elements.add(userTask);
        
        workflowData.put("elements", elements);
        mappedData.put("workflow", workflowData);
        
        return mappedData;
    }
}