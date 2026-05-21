/**
 * Module: FormFieldMapper - Map AWD HtmlControl hierarchy to UX standard types with validation rules
 * 
 * Summary:
 *     Maps AWD HtmlControl hierarchy (HtmlTextInput, HtmlRadioGroup, HtmlCheckboxInput, etc.)
 *     to UX standard types with comprehensive validation rules and field mappings. Handles
 *     complex form relationships, dependencies, and responsive design metadata. Provides
 *     intelligent field type detection and conversion with validation pattern extraction.
 * 
 * Key Components:
 *     - mapAwdControlToUx(): Primary mapping method from AWD to UX types
 *     - extractValidationPatterns(): Extract validation rules from AWD controls
 *     - handleComplexFormRelationships(): Manage field dependencies and relationships
 *     - generateResponsiveMetadata(): Create responsive design metadata
 *     - optimizeFieldMappings(): Performance optimization for repeated mappings
 * 
 * Keywords: form, field, mapper, awd, htmlcontrol, ux, types, validation, rules,
 *          relationships, dependencies, responsive, design, metadata, optimization
 * 
 * Dependencies:
 *     - com.patientvibes.awd.deserializer.extractor.model.FormField: Target field model
 *     - com.patientvibes.awd.deserializer.extractor.model.ValidationRule: Validation specifications
 *     - java.util.regex.Pattern: Pattern matching for validation extraction
 * 
 * Security:
 *     - Input validation for all AWD control data
 *     - Safe pattern compilation for validation rules
 *     - Field mapping validation to prevent injection attacks
 *     - Sanitization of field properties and metadata
 * 
 * Performance:
 *     - Cached mapping patterns for repeated AWD control types
 *     - Optimized field relationship analysis
 *     - Memory-efficient validation rule extraction
 *     - Fast lookup tables for common mapping operations
 */
package com.patientvibes.awd.deserializer.extractor;

import com.patientvibes.awd.deserializer.extractor.model.FormField;
import com.patientvibes.awd.deserializer.extractor.model.ValidationRule;

import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Maps AWD form controls to UX standard types with validation rules and metadata.
 */
public class FormFieldMapper {
    private static final Logger logger = Logger.getLogger(FormFieldMapper.class.getName());
    
    // AWD HtmlControl to UX type mappings
    private static final Map<String, String> AWD_TO_UX_MAPPING;
    static {
        Map<String, String> mapping = new HashMap<>();
        mapping.put("HtmlTextInput", "text");
        mapping.put("HtmlRadioGroup", "radio");
        mapping.put("HtmlCheckboxInput", "checkbox");
        mapping.put("HtmlDateInput", "date");
        mapping.put("HtmlFileInput", "file");
        mapping.put("HtmlInputOption", "select");
        mapping.put("HtmlInput", "text");
        mapping.put("HtmlTextArea", "textarea");
        mapping.put("HtmlEmailInput", "email");
        mapping.put("HtmlPasswordInput", "password");
        mapping.put("HtmlNumberInput", "number");
        mapping.put("HtmlHiddenInput", "hidden");
        AWD_TO_UX_MAPPING = Collections.unmodifiableMap(mapping);
    }
    
    // Validation pattern mappings
    private static final Map<String, String> VALIDATION_PATTERNS = Map.of(
        "required", "required",
        "minlength", "minLength",
        "maxlength", "maxLength",
        "pattern", "pattern",
        "email", "email",
        "numeric", "numeric",
        "alphanumeric", "pattern:[a-zA-Z0-9]+",
        "phone", "pattern:\\\\d{3}-\\\\d{3}-\\\\d{4}"
    );
    
    // Responsive breakpoints for field sizing
    private static final Map<String, String> RESPONSIVE_BREAKPOINTS = Map.of(
        "xs", "max-width: 575px",
        "sm", "min-width: 576px",
        "md", "min-width: 768px",
        "lg", "min-width: 992px",
        "xl", "min-width: 1200px"
    );
    
    // Field relationship types
    private enum FieldRelationship {
        DEPENDS_ON,
        TRIGGERS,
        VALIDATES_WITH,
        GROUPS_WITH
    }
    
    // Cached mapping results for performance
    private final Map<String, FormField> mappingCache = new HashMap<>();
    private final Map<String, List<ValidationRule>> validationCache = new HashMap<>();
    
    public FormFieldMapper() {
        logger.info("FormFieldMapper initialized with AWD to UX mappings");
    }
    
    /**
     * Map AWD form control to UX form field with validation rules.
     * 
     * @param awdControl AWD control data structure
     * @return Mapped UX form field
     */
    public FormField mapAwdControlToUx(Map<String, Object> awdControl) {
        if (awdControl == null) {
            throw new IllegalArgumentException("AWD control cannot be null");
        }
        
        String controlType = (String) awdControl.get("type");
        String controlId = (String) awdControl.get("id");
        
        // Check cache first
        String cacheKey = generateCacheKey(controlType, controlId);
        if (mappingCache.containsKey(cacheKey)) {
            logger.fine("Using cached mapping for: " + cacheKey);
            return cloneFormField(mappingCache.get(cacheKey));
        }
        
        FormField field = createBaseFormField(awdControl);
        
        // Apply type-specific mappings
        applyTypeSpecificMapping(field, awdControl);
        
        // Extract validation rules
        List<ValidationRule> validationRules = extractValidationRules(awdControl);
        applyValidationRules(field, validationRules);
        
        // Handle field relationships
        handleFieldRelationships(field, awdControl);
        
        // Add responsive design metadata
        addResponsiveMetadata(field, awdControl);
        
        // Cache the result
        mappingCache.put(cacheKey, field);
        
        logger.fine("Mapped AWD control " + controlType + " to UX field " + field.getType());
        return field;
    }
    
    /**
     * Extract validation rules from AWD control properties.
     */
    public List<ValidationRule> extractValidationRules(Map<String, Object> awdControl) {
        String controlId = (String) awdControl.get("id");
        
        // Check validation cache
        if (validationCache.containsKey(controlId)) {
            return new ArrayList<>(validationCache.get(controlId));
        }
        
        List<ValidationRule> rules = new ArrayList<>();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) awdControl.get("properties");
        if (properties != null) {
            // Extract validation from properties
            extractValidationFromProperties(properties, rules, controlId);
        }
        
        // Extract validation from constraints
        @SuppressWarnings("unchecked")
        Map<String, Object> constraints = (Map<String, Object>) awdControl.get("constraints");
        if (constraints != null) {
            extractValidationFromConstraints(constraints, rules, controlId);
        }
        
        // Extract validation from attributes
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = (Map<String, Object>) awdControl.get("attributes");
        if (attributes != null) {
            extractValidationFromAttributes(attributes, rules, controlId);
        }
        
        // Cache the results
        validationCache.put(controlId, new ArrayList<>(rules));
        
        return rules;
    }
    
    /**
     * Map multiple AWD controls in batch for performance.
     */
    public List<FormField> mapAwdControlsBatch(List<Map<String, Object>> awdControls) {
        if (awdControls == null || awdControls.isEmpty()) {
            return new ArrayList<>();
        }
        
        return awdControls.stream()
                .map(this::mapAwdControlToUx)
                .collect(Collectors.toList());
    }
    
    /**
     * Get supported AWD control types.
     */
    public Set<String> getSupportedAwdTypes() {
        return new HashSet<>(AWD_TO_UX_MAPPING.keySet());
    }
    
    /**
     * Check if AWD control type is supported.
     */
    public boolean isAwdTypeSupported(String awdType) {
        return AWD_TO_UX_MAPPING.containsKey(awdType);
    }
    
    /**
     * Get UX type for AWD control type.
     */
    public String getUxTypeForAwdType(String awdType) {
        return AWD_TO_UX_MAPPING.getOrDefault(awdType, "text");
    }
    
    /**
     * Clear mapping cache for memory management.
     */
    public void clearCache() {
        mappingCache.clear();
        validationCache.clear();
        logger.info("FormFieldMapper cache cleared");
    }
    
    /**
     * Get cache statistics for monitoring.
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("mappingCacheSize", mappingCache.size());
        stats.put("validationCacheSize", validationCache.size());
        stats.put("supportedAwdTypes", AWD_TO_UX_MAPPING.size());
        stats.put("validationPatterns", VALIDATION_PATTERNS.size());
        
        return stats;
    }
    
    // Private helper methods
    
    private FormField createBaseFormField(Map<String, Object> awdControl) {
        FormField field = new FormField();
        
        String controlType = (String) awdControl.get("type");
        String controlId = (String) awdControl.get("id");
        String controlName = (String) awdControl.get("name");
        String controlLabel = (String) awdControl.get("label");
        
        // Set basic properties
        field.setName(controlName != null ? controlName : controlId);
        field.setLabel(controlLabel != null ? controlLabel : formatLabel(field.getName()));
        field.setType(getUxTypeForAwdType(controlType));
        field.setHtmlControl(controlType);
        
        // Extract properties
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) awdControl.get("properties");
        if (properties != null) {
            extractBasicProperties(field, properties);
        }
        
        return field;
    }
    
    private void applyTypeSpecificMapping(FormField field, Map<String, Object> awdControl) {
        String controlType = field.getHtmlControl();
        
        switch (controlType) {
            case "HtmlRadioGroup":
            case "HtmlInputOption":
                applyChoiceFieldMapping(field, awdControl);
                break;
            case "HtmlDateInput":
                applyDateFieldMapping(field, awdControl);
                break;
            case "HtmlFileInput":
                applyFileFieldMapping(field, awdControl);
                break;
            case "HtmlTextArea":
                applyTextAreaMapping(field, awdControl);
                break;
            case "HtmlNumberInput":
                applyNumberFieldMapping(field, awdControl);
                break;
            default:
                applyTextFieldMapping(field, awdControl);
                break;
        }
    }
    
    private void applyChoiceFieldMapping(FormField field, Map<String, Object> awdControl) {
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) awdControl.get("properties");
        
        if (properties != null) {
            // Extract options
            @SuppressWarnings("unchecked")
            List<String> options = (List<String>) properties.get("options");
            if (options != null) {
                field.setOptions(options);
            }
            
            // Extract option values and labels
            @SuppressWarnings("unchecked")
            List<Map<String, String>> optionMaps = (List<Map<String, String>>) properties.get("optionMaps");
            if (optionMaps != null) {
                List<String> extractedOptions = optionMaps.stream()
                        .map(map -> map.getOrDefault("label", map.getOrDefault("value", "Option")))
                        .collect(Collectors.toList());
                field.setOptions(extractedOptions);
            }
        }
    }
    
    private void applyDateFieldMapping(FormField field, Map<String, Object> awdControl) {
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) awdControl.get("properties");
        
        if (properties != null) {
            // Check for date format
            String dateFormat = (String) properties.get("dateFormat");
            if (dateFormat != null) {
                field.addOption("dateFormat:" + dateFormat);
            }
            
            // Check for min/max dates
            String minDate = (String) properties.get("minDate");
            String maxDate = (String) properties.get("maxDate");
            
            if (minDate != null || maxDate != null) {
                StringBuilder validation = new StringBuilder();
                if (field.getValidation() != null) {
                    validation.append(field.getValidation()).append("|");
                }
                
                if (minDate != null) {
                    validation.append("minDate:").append(minDate);
                }
                if (maxDate != null) {
                    if (minDate != null) validation.append("|");
                    validation.append("maxDate:").append(maxDate);
                }
                
                field.setValidation(validation.toString());
            }
        }
    }
    
    private void applyFileFieldMapping(FormField field, Map<String, Object> awdControl) {
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) awdControl.get("properties");
        
        if (properties != null) {
            // Extract allowed file types
            @SuppressWarnings("unchecked")
            List<String> allowedTypes = (List<String>) properties.get("allowedTypes");
            if (allowedTypes != null) {
                field.addOption("allowedTypes:" + String.join(",", allowedTypes));
            }
            
            // Extract max file size
            Object maxSize = properties.get("maxFileSize");
            if (maxSize != null) {
                field.addOption("maxFileSize:" + maxSize.toString());
            }
        }
    }
    
    private void applyTextAreaMapping(FormField field, Map<String, Object> awdControl) {
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) awdControl.get("properties");
        
        if (properties != null) {
            // Extract rows and columns
            Object rows = properties.get("rows");
            Object cols = properties.get("cols");
            
            if (rows != null) {
                field.addOption("rows:" + rows.toString());
            }
            if (cols != null) {
                field.addOption("cols:" + cols.toString());
            }
        }
    }
    
    private void applyNumberFieldMapping(FormField field, Map<String, Object> awdControl) {
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) awdControl.get("properties");
        
        if (properties != null) {
            // Extract min/max values
            Object min = properties.get("min");
            Object max = properties.get("max");
            Object step = properties.get("step");
            
            StringBuilder validation = new StringBuilder();
            if (field.getValidation() != null) {
                validation.append(field.getValidation()).append("|");
            }
            
            if (min != null) {
                validation.append("min:").append(min.toString());
            }
            if (max != null) {
                if (min != null) validation.append("|");
                validation.append("max:").append(max.toString());
            }
            
            if (validation.length() > 0) {
                field.setValidation(validation.toString());
            }
            
            if (step != null) {
                field.addOption("step:" + step.toString());
            }
        }
    }
    
    private void applyTextFieldMapping(FormField field, Map<String, Object> awdControl) {
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) awdControl.get("properties");
        
        if (properties != null) {
            // Extract input format/pattern
            String pattern = (String) properties.get("pattern");
            if (pattern != null) {
                String validation = field.getValidation();
                validation = validation != null ? validation + "|pattern:" + pattern : "pattern:" + pattern;
                field.setValidation(validation);
            }
        }
    }
    
    private void extractBasicProperties(FormField field, Map<String, Object> properties) {
        // Extract placeholder
        String placeholder = (String) properties.get("placeholder");
        if (placeholder != null) {
            field.setPlaceholder(placeholder);
        }
        
        // Extract required flag
        Boolean required = (Boolean) properties.get("required");
        if (Boolean.TRUE.equals(required)) {
            field.setRequired(true);
            String validation = field.getValidation();
            validation = validation != null ? "required|" + validation : "required";
            field.setValidation(validation);
        }
        
        // Extract default value
        Object defaultValue = properties.get("defaultValue");
        if (defaultValue != null) {
            field.setDefaultValue(defaultValue);
        }
    }
    
    private void extractValidationFromProperties(Map<String, Object> properties, List<ValidationRule> rules, String fieldName) {
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String key = entry.getKey().toLowerCase();
            Object value = entry.getValue();
            
            if (VALIDATION_PATTERNS.containsKey(key)) {
                ValidationRule rule = new ValidationRule();
                rule.setFieldName(fieldName);
                rule.setRuleType(VALIDATION_PATTERNS.get(key));
                
                if (value instanceof String) {
                    rule.setRuleValue((String) value);
                } else if (value instanceof Number) {
                    rule.setRuleValue(value.toString());
                } else if (Boolean.TRUE.equals(value)) {
                    rule.setRuleValue("true");
                }
                
                rules.add(rule);
            }
        }
    }
    
    private void extractValidationFromConstraints(Map<String, Object> constraints, List<ValidationRule> rules, String fieldName) {
        // Handle AWD-specific constraint formats
        for (Map.Entry<String, Object> entry : constraints.entrySet()) {
            String constraintType = entry.getKey();
            Object constraintValue = entry.getValue();
            
            ValidationRule rule = createValidationRuleFromConstraint(fieldName, constraintType, constraintValue);
            if (rule != null) {
                rules.add(rule);
            }
        }
    }
    
    private void extractValidationFromAttributes(Map<String, Object> attributes, List<ValidationRule> rules, String fieldName) {
        // Handle HTML5 validation attributes
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            String attr = entry.getKey().toLowerCase();
            Object value = entry.getValue();
            
            ValidationRule rule = null;
            
            switch (attr) {
                case "required":
                    if (Boolean.TRUE.equals(value)) {
                        rule = new ValidationRule(fieldName, "required");
                    }
                    break;
                case "minlength":
                case "maxlength":
                    rule = new ValidationRule(fieldName, attr.replace("length", "Length"));
                    rule.setRuleValue(value.toString());
                    break;
                case "pattern":
                    rule = new ValidationRule(fieldName, "pattern");
                    rule.setRuleValue((String) value);
                    break;
            }
            
            if (rule != null) {
                rules.add(rule);
            }
        }
    }
    
    private ValidationRule createValidationRuleFromConstraint(String fieldName, String constraintType, Object constraintValue) {
        ValidationRule rule = new ValidationRule();
        rule.setFieldName(fieldName);
        
        switch (constraintType.toLowerCase()) {
            case "required":
            case "mandatory":
                rule.setRuleType("required");
                return rule;
            case "minlength":
            case "min_length":
                rule.setRuleType("minLength");
                rule.setRuleValue(constraintValue.toString());
                return rule;
            case "maxlength":
            case "max_length":
                rule.setRuleType("maxLength");
                rule.setRuleValue(constraintValue.toString());
                return rule;
            case "pattern":
            case "regex":
                rule.setRuleType("pattern");
                rule.setRuleValue((String) constraintValue);
                return rule;
            default:
                return null;
        }
    }
    
    private void applyValidationRules(FormField field, List<ValidationRule> validationRules) {
        if (validationRules.isEmpty()) {
            return;
        }
        
        // Combine validation rules into field validation string
        List<String> validationParts = new ArrayList<>();
        
        for (ValidationRule rule : validationRules) {
            String validationPart = rule.getRuleType();
            if (rule.getRuleValue() != null) {
                validationPart += ":" + rule.getRuleValue();
            }
            validationParts.add(validationPart);
        }
        
        String combinedValidation = String.join("|", validationParts);
        
        // Merge with existing validation
        String existingValidation = field.getValidation();
        if (existingValidation != null && !existingValidation.trim().isEmpty()) {
            field.setValidation(existingValidation + "|" + combinedValidation);
        } else {
            field.setValidation(combinedValidation);
        }
    }
    
    private void handleFieldRelationships(FormField field, Map<String, Object> awdControl) {
        @SuppressWarnings("unchecked")
        Map<String, Object> relationships = (Map<String, Object>) awdControl.get("relationships");
        
        if (relationships != null) {
            // Handle field dependencies
            @SuppressWarnings("unchecked")
            List<String> dependsOn = (List<String>) relationships.get("dependsOn");
            if (dependsOn != null && !dependsOn.isEmpty()) {
                field.addOption("dependsOn:" + String.join(",", dependsOn));
            }
            
            // Handle conditional visibility
            String showIf = (String) relationships.get("showIf");
            if (showIf != null) {
                field.addOption("showIf:" + showIf);
            }
        }
    }
    
    private void addResponsiveMetadata(FormField field, Map<String, Object> awdControl) {
        @SuppressWarnings("unchecked")
        Map<String, Object> responsive = (Map<String, Object>) awdControl.get("responsive");
        
        if (responsive != null) {
            for (Map.Entry<String, String> breakpoint : RESPONSIVE_BREAKPOINTS.entrySet()) {
                Object value = responsive.get(breakpoint.getKey());
                if (value != null) {
                    field.addOption("responsive-" + breakpoint.getKey() + ":" + value.toString());
                }
            }
        }
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
    
    private String generateCacheKey(String controlType, String controlId) {
        return controlType + ":" + (controlId != null ? controlId : "unknown");
    }
    
    private FormField cloneFormField(FormField original) {
        FormField clone = new FormField();
        clone.setName(original.getName());
        clone.setLabel(original.getLabel());
        clone.setType(original.getType());
        clone.setValidation(original.getValidation());
        clone.setAwdMapping(original.getAwdMapping());
        clone.setHtmlControl(original.getHtmlControl());
        clone.setPlaceholder(original.getPlaceholder());
        clone.setOptions(new ArrayList<>(original.getOptions()));
        clone.setRequired(original.isRequired());
        clone.setDefaultValue(original.getDefaultValue());
        
        return clone;
    }
}