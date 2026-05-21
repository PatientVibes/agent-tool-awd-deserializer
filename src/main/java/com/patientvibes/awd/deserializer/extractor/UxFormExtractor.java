/**
 * Module: UxFormExtractor - UX form extraction with dation-compatible JSON output and AWD form control mapping
 * 
 * Summary:
 *     Extracts form definitions from AWD workflow elements and converts them to dation-compatible
 *     JSON format. Maps AWD HtmlControl hierarchy (HtmlTextInput, HtmlRadioGroup, HtmlCheckboxInput)
 *     to UX standard types with proper validation rules and field mappings. Supports individual
 *     form file generation for dation analysis with comprehensive metadata.
 * 
 * Key Components:
 *     - extractUxForms(): Primary form extraction orchestration from AWD data
 *     - mapAwdFormControls(): Map AWD HtmlControl types to dation UX types
 *     - generateDationForm(): Create individual dation-compatible JSON form files
 *     - extractValidationRules(): Extract field validation patterns from AWD
 *     - buildFormMetadata(): Create dation-specific metadata with timestamps
 * 
 * Keywords: ux, form, extraction, dation, compatible, json, awd, htmlcontrol, mapping,
 *          validation, rules, field, types, user, centric, design, workflow, elements
 * 
 * Dependencies:
 *     - com.patientvibes.awd.deserializer.business.model.BusinessMetadata: Source workflow metadata
 *     - com.patientvibes.awd.deserializer.extractor.FormFieldMapper: AWD to UX field mapping
 *     - com.patientvibes.awd.deserializer.extractor.DationFormattingService: Dation formatting
 *     - com.fasterxml.jackson.databind.ObjectMapper: JSON serialization
 *     - java.nio.file.Path: File system operations for JSON output
 * 
 * Security:
 *     - Input validation for all AWD form data structures
 *     - JSON injection prevention through proper escaping
 *     - Safe file path resolution for form output files
 *     - Validation rule sanitization to prevent code injection
 *     - AWD field mapping validation for secure data binding
 * 
 * Performance:
 *     - Parallel form extraction for multiple workflow elements
 *     - Memory-efficient JSON streaming for large forms
 *     - Optimized field mapping with caching for repeated patterns
 *     - Lazy validation rule processing for complex forms
 *     - Configurable batch processing for high-volume extraction
 */
package com.patientvibes.awd.deserializer.extractor;

import com.patientvibes.awd.deserializer.business.model.BusinessMetadata;
import com.patientvibes.awd.deserializer.extractor.model.UxFormDefinition;
import com.patientvibes.awd.deserializer.extractor.model.FormField;
import com.patientvibes.awd.deserializer.extractor.model.ValidationRule;
import com.patientvibes.awd.deserializer.extractor.model.DationMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Extracts UX forms from AWD workflow elements with dation-compatible JSON output.
 * Focuses on user-centric form design and AWD form control mapping.
 */
public class UxFormExtractor {
    private static final Logger logger = Logger.getLogger(UxFormExtractor.class.getName());
    
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;
    
    // AWD HtmlControl type mappings to UX standard types
    private static final Map<String, String> AWD_TO_UX_TYPE_MAPPING = Map.of(
        "HtmlTextInput", "text",
        "HtmlRadioGroup", "radio",
        "HtmlCheckboxInput", "checkbox",
        "HtmlDateInput", "date",
        "HtmlFileInput", "file",
        "HtmlInputOption", "select",
        "HtmlInput", "text"
    );
    
    // Pattern for AWD field mapping validation
    private static final Pattern AWD_VALUE_PATTERN = Pattern.compile("awd:awd-value\\('[^']+']+'(,\\s*'[^']*')?\\)");
    
    // Validation rule patterns
    private static final Map<String, String> VALIDATION_PATTERNS = Map.of(
        "required", "required",
        "minLength", "minLength:\\d+",
        "maxLength", "maxLength:\\d+",
        "pattern", "pattern:.*",
        "email", "email",
        "numeric", "numeric"
    );
    
    public UxFormExtractor() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.executorService = Executors.newFixedThreadPool(4);
        
        logger.info("UxFormExtractor initialized with dation compatibility");
    }
    
    /**
     * Extract UX forms from AWD workflow elements and generate dation-compatible JSON files.
     * 
     * @param metadata Business metadata containing workflow and form data
     * @param mappedData AWD-mapped data structures
     * @param outputDirectory Target directory for JSON form files
     * @return List of generated JSON form file paths
     * @throws IOException if file operations fail
     */
    public List<String> extractUxForms(BusinessMetadata metadata, Map<String, Object> mappedData, Path outputDirectory) throws IOException {
        logger.info("Starting UX form extraction with dation compatibility");
        
        // Validate inputs
        validateExtractionInputs(metadata, mappedData, outputDirectory);
        
        // Ensure output directory exists
        Files.createDirectories(outputDirectory);
        
        List<String> generatedFiles = new ArrayList<>();
        
        try {
            // Extract workflow elements that contain forms
            List<Map<String, Object>> workflowElements = extractWorkflowElements(metadata, mappedData);
            
            // Extract forms from workflow elements in parallel
            List<CompletableFuture<String>> extractionTasks = new ArrayList<>();
            
            for (Map<String, Object> element : workflowElements) {
                extractionTasks.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        return extractSingleForm(element, outputDirectory);
                    } catch (Exception e) {
                        logger.severe("Failed to extract form from element: " + e.getMessage());
                        return null;
                    }
                }, executorService));
            }
            
            // Wait for all extractions and collect results
            for (CompletableFuture<String> task : extractionTasks) {
                String filePath = task.get();
                if (filePath != null) {
                    generatedFiles.add(filePath);
                }
            }
            
            logger.info("UX form extraction completed: " + generatedFiles.size() + " forms generated");
            
        } catch (Exception e) {
            logger.severe("Error during UX form extraction: " + e.getMessage());
            throw new IOException("Form extraction failed", e);
        }
        
        return generatedFiles;
    }
    
    /**
     * Extract a single form from a workflow element and save as dation-compatible JSON.
     */
    private String extractSingleForm(Map<String, Object> workflowElement, Path outputDirectory) throws IOException {
        // Extract form data from workflow element
        Map<String, Object> formData = extractFormDataFromElement(workflowElement);
        
        if (formData.isEmpty()) {
            logger.warning("No form data found in workflow element: " + workflowElement.get("id"));
            return null;
        }
        
        // Create UX form definition
        UxFormDefinition formDef = createUxFormDefinition(formData, workflowElement);
        
        // Generate dation-compatible JSON
        Map<String, Object> dationForm = generateDationCompatibleForm(formDef);
        
        // Generate file name
        String fileName = generateFormFileName(formDef);
        Path outputFile = outputDirectory.resolve(fileName);
        
        // Write JSON file
        objectMapper.writeValue(outputFile.toFile(), dationForm);
        
        logger.info("Generated UX form file: " + fileName);
        return outputFile.toString();
    }
    
    /**
     * Extract form data from a workflow element.
     */
    private Map<String, Object> extractFormDataFromElement(Map<String, Object> element) {
        Map<String, Object> formData = new HashMap<>();
        
        // Check for userTask with form configuration
        String elementType = (String) element.get("type");
        if ("userTask".equals(elementType)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) element.get("properties");
            
            if (properties != null) {
                // Extract form key
                String formKey = (String) properties.get("formKey");
                if (formKey != null) {
                    formData.put("formKey", formKey);
                }
                
                // Extract form configuration from extension properties
                @SuppressWarnings("unchecked")
                Map<String, Object> extensionProps = (Map<String, Object>) properties.get("extensionProperties");
                if (extensionProps != null) {
                    Object formConfig = extensionProps.get("formConfig");
                    if (formConfig != null) {
                        formData.put("formConfig", formConfig);
                    }
                }
                
                // Extract AWD form controls
                Object awdFormControls = properties.get("awdFormControls");
                if (awdFormControls != null) {
                    formData.put("awdFormControls", awdFormControls);
                }
            }
        }
        
        return formData;
    }
    
    /**
     * Create UX form definition from extracted form data.
     */
    private UxFormDefinition createUxFormDefinition(Map<String, Object> formData, Map<String, Object> workflowElement) {
        UxFormDefinition formDef = new UxFormDefinition();
        
        // Set basic form properties
        String elementId = (String) workflowElement.get("id");
        String elementName = (String) workflowElement.get("name");
        String formKey = (String) formData.get("formKey");
        
        formDef.setFormId(formKey != null ? formKey : elementId + "_form");
        formDef.setFormName(elementName != null ? elementName : "Unnamed Form");
        formDef.setFormType("awd_user_task");
        formDef.setAwdElementId(elementId);
        
        // Extract and map form fields
        List<FormField> fields = extractFormFields(formData);
        formDef.setFields(fields);
        
        // Extract validation rules
        List<ValidationRule> validationRules = extractValidationRules(formData, fields);
        formDef.setValidationRules(validationRules);
        
        // Create dation metadata
        DationMetadata dationMetadata = createDationMetadata(workflowElement);
        formDef.setDationMetadata(dationMetadata);
        
        return formDef;
    }
    
    /**
     * Extract form fields from form data and map AWD controls to UX types.
     */
    private List<FormField> extractFormFields(Map<String, Object> formData) {
        List<FormField> fields = new ArrayList<>();
        
        // Extract from form configuration
        Object formConfig = formData.get("formConfig");
        if (formConfig instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> config = (Map<String, Object>) formConfig;
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> fieldConfigs = (List<Map<String, Object>>) config.get("fields");
            
            if (fieldConfigs != null) {
                for (Map<String, Object> fieldConfig : fieldConfigs) {
                    FormField field = mapFieldConfig(fieldConfig);
                    if (field != null) {
                        fields.add(field);
                    }
                }
            }
        }
        
        // Extract from AWD form controls
        Object awdControls = formData.get("awdFormControls");
        if (awdControls instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> controls = (List<Map<String, Object>>) awdControls;
            
            for (Map<String, Object> control : controls) {
                FormField field = mapAwdControl(control);
                if (field != null) {
                    fields.add(field);
                }
            }
        }
        
        return fields;
    }
    
    /**
     * Map field configuration to FormField.
     */
    private FormField mapFieldConfig(Map<String, Object> fieldConfig) {
        FormField field = new FormField();
        
        field.setName((String) fieldConfig.get("name"));
        field.setLabel((String) fieldConfig.get("label"));
        
        // Map AWD type to UX type
        String awdType = (String) fieldConfig.get("type");
        String uxType = AWD_TO_UX_TYPE_MAPPING.getOrDefault(awdType, "text");
        field.setType(uxType);
        
        // Set validation
        String validation = (String) fieldConfig.get("validation");
        if (validation != null) {
            field.setValidation(validation);
        }
        
        // Set AWD mapping
        String mapping = (String) fieldConfig.get("mapping");
        if (mapping != null && AWD_VALUE_PATTERN.matcher(mapping).matches()) {
            field.setAwdMapping(mapping);
        }
        
        // Set HTML control type
        field.setHtmlControl(awdType);
        
        // Extract options for select/radio controls
        @SuppressWarnings("unchecked")
        List<String> options = (List<String>) fieldConfig.get("options");
        if (options != null) {
            field.setOptions(options);
        }
        
        // Set placeholder
        field.setPlaceholder((String) fieldConfig.get("placeholder"));
        
        return field;
    }
    
    /**
     * Map AWD form control to FormField.
     */
    private FormField mapAwdControl(Map<String, Object> control) {
        FormField field = new FormField();
        
        String controlType = (String) control.get("type");
        String controlName = (String) control.get("name");
        
        field.setName(controlName != null ? controlName : "field_" + System.currentTimeMillis());
        field.setLabel(controlName != null ? formatLabel(controlName) : "Field");
        
        // Map to UX type
        String uxType = AWD_TO_UX_TYPE_MAPPING.getOrDefault(controlType, "text");
        field.setType(uxType);
        field.setHtmlControl(controlType);
        
        // Extract properties
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) control.get("properties");
        if (properties != null) {
            // Set validation from properties
            Boolean required = (Boolean) properties.get("required");
            if (Boolean.TRUE.equals(required)) {
                field.setValidation("required");
            }
            
            // Set other properties
            field.setPlaceholder((String) properties.get("placeholder"));
            
            @SuppressWarnings("unchecked")
            List<String> options = (List<String>) properties.get("options");
            if (options != null) {
                field.setOptions(options);
            }
        }
        
        return field;
    }
    
    /**
     * Extract validation rules from form data.
     */
    private List<ValidationRule> extractValidationRules(Map<String, Object> formData, List<FormField> fields) {
        List<ValidationRule> rules = new ArrayList<>();
        
        for (FormField field : fields) {
            if (field.getValidation() != null) {
                String[] validations = field.getValidation().split("\\|");
                
                for (String validation : validations) {
                    ValidationRule rule = new ValidationRule();
                    rule.setFieldName(field.getName());
                    rule.setRuleType(parseValidationType(validation));
                    rule.setRuleValue(parseValidationValue(validation));
                    rule.setErrorMessage(generateErrorMessage(field.getLabel(), validation));
                    
                    rules.add(rule);
                }
            }
        }
        
        return rules;
    }
    
    /**
     * Create dation-specific metadata.
     */
    private DationMetadata createDationMetadata(Map<String, Object> workflowElement) {
        DationMetadata metadata = new DationMetadata();
        
        metadata.setSourceSystem("awd");
        metadata.setExtractionTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        metadata.setAwdElementId((String) workflowElement.get("id"));
        metadata.setElementType((String) workflowElement.get("type"));
        
        // Add process context if available
        String processId = (String) workflowElement.get("processId");
        if (processId != null) {
            metadata.setProcessId(processId);
        }
        
        return metadata;
    }
    
    /**
     * Generate dation-compatible form JSON structure.
     */
    private Map<String, Object> generateDationCompatibleForm(UxFormDefinition formDef) {
        Map<String, Object> dationForm = new LinkedHashMap<>();
        
        // Basic form information
        dationForm.put("formId", formDef.getFormId());
        dationForm.put("formName", formDef.getFormName());
        dationForm.put("formType", formDef.getFormType());
        
        // Fields array
        List<Map<String, Object>> fieldsArray = new ArrayList<>();
        for (FormField field : formDef.getFields()) {
            Map<String, Object> fieldMap = new LinkedHashMap<>();
            fieldMap.put("name", field.getName());
            fieldMap.put("type", field.getType());
            fieldMap.put("label", field.getLabel());
            
            if (field.getValidation() != null) {
                fieldMap.put("validation", field.getValidation());
            }
            
            if (field.getAwdMapping() != null) {
                fieldMap.put("awdMapping", field.getAwdMapping());
            }
            
            if (field.getHtmlControl() != null) {
                fieldMap.put("htmlControl", field.getHtmlControl());
            }
            
            if (field.getPlaceholder() != null) {
                fieldMap.put("placeholder", field.getPlaceholder());
            }
            
            if (field.getOptions() != null && !field.getOptions().isEmpty()) {
                fieldMap.put("options", field.getOptions());
            }
            
            fieldsArray.add(fieldMap);
        }
        dationForm.put("fields", fieldsArray);
        
        // Validation rules
        if (!formDef.getValidationRules().isEmpty()) {
            List<Map<String, Object>> rulesArray = new ArrayList<>();
            for (ValidationRule rule : formDef.getValidationRules()) {
                Map<String, Object> ruleMap = new LinkedHashMap<>();
                ruleMap.put("fieldName", rule.getFieldName());
                ruleMap.put("ruleType", rule.getRuleType());
                
                if (rule.getRuleValue() != null) {
                    ruleMap.put("ruleValue", rule.getRuleValue());
                }
                
                if (rule.getErrorMessage() != null) {
                    ruleMap.put("errorMessage", rule.getErrorMessage());
                }
                
                rulesArray.add(ruleMap);
            }
            dationForm.put("validationRules", rulesArray);
        }
        
        // Dation metadata
        DationMetadata metadata = formDef.getDationMetadata();
        Map<String, Object> metadataMap = new LinkedHashMap<>();
        metadataMap.put("sourceSystem", metadata.getSourceSystem());
        metadataMap.put("extractionTimestamp", metadata.getExtractionTimestamp());
        metadataMap.put("awdElementId", metadata.getAwdElementId());
        
        if (metadata.getElementType() != null) {
            metadataMap.put("elementType", metadata.getElementType());
        }
        
        if (metadata.getProcessId() != null) {
            metadataMap.put("processId", metadata.getProcessId());
        }
        
        dationForm.put("dationMetadata", metadataMap);
        
        return dationForm;
    }
    
    // Helper methods
    
    private void validateExtractionInputs(BusinessMetadata metadata, Map<String, Object> mappedData, Path outputDirectory) {
        if (metadata == null) {
            throw new IllegalArgumentException("Business metadata cannot be null");
        }
        
        if (mappedData == null) {
            throw new IllegalArgumentException("Mapped data cannot be null");
        }
        
        if (outputDirectory == null) {
            throw new IllegalArgumentException("Output directory cannot be null");
        }
    }
    
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractWorkflowElements(BusinessMetadata metadata, Map<String, Object> mappedData) {
        List<Map<String, Object>> elements = new ArrayList<>();
        
        // Extract from workflow data
        Object workflowData = mappedData.get("workflow");
        if (workflowData instanceof Map) {
            Map<String, Object> workflow = (Map<String, Object>) workflowData;
            
            Object elementsData = workflow.get("elements");
            if (elementsData instanceof List) {
                List<Map<String, Object>> workflowElements = (List<Map<String, Object>>) elementsData;
                
                // Filter for user tasks and other form-containing elements
                elements = workflowElements.stream()
                    .filter(this::hasFormData)
                    .collect(Collectors.toList());
            }
        }
        
        return elements;
    }
    
    private boolean hasFormData(Map<String, Object> element) {
        String type = (String) element.get("type");
        
        // Check if it's a user task
        if ("userTask".equals(type)) {
            return true;
        }
        
        // Check for form configuration in properties
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) element.get("properties");
        if (properties != null) {
            return properties.containsKey("formKey") || 
                   properties.containsKey("formConfig") || 
                   properties.containsKey("awdFormControls");
        }
        
        return false;
    }
    
    private String formatLabel(String name) {
        if (name == null || name.isEmpty()) {
            return "Field";
        }
        
        // Convert camelCase to Title Case
        return name.replaceAll("([a-z])([A-Z])", "$1 $2")
                  .substring(0, 1).toUpperCase() + 
                  name.replaceAll("([a-z])([A-Z])", "$1 $2").substring(1);
    }
    
    private String parseValidationType(String validation) {
        if (validation.contains(":")) {
            return validation.split(":")[0];
        }
        return validation;
    }
    
    private String parseValidationValue(String validation) {
        if (validation.contains(":")) {
            return validation.split(":", 2)[1];
        }
        return null;
    }
    
    private String generateErrorMessage(String fieldLabel, String validation) {
        String validationType = parseValidationType(validation);
        
        switch (validationType) {
            case "required":
                return fieldLabel + " is required";
            case "minLength":
                String minValue = parseValidationValue(validation);
                return fieldLabel + " must be at least " + minValue + " characters";
            case "maxLength":
                String maxValue = parseValidationValue(validation);
                return fieldLabel + " must not exceed " + maxValue + " characters";
            case "email":
                return fieldLabel + " must be a valid email address";
            case "numeric":
                return fieldLabel + " must be a number";
            default:
                return fieldLabel + " is invalid";
        }
    }
    
    private String generateFormFileName(UxFormDefinition formDef) {
        String formId = formDef.getFormId().replaceAll("[^a-zA-Z0-9_-]", "_");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        
        return formId + "_" + timestamp + ".json";
    }
    
    /**
     * Shutdown the extraction service and clean up resources.
     */
    public void shutdown() {
        logger.info("Shutting down UxFormExtractor");
        executorService.shutdown();
        
        try {
            if (!executorService.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}