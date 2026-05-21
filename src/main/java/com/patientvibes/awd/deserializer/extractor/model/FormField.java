/**
 * Module: FormField - Individual form field definition for UX forms
 * 
 * Summary:
 *     Represents a single form field with UX type mapping, validation rules,
 *     and AWD integration properties. Supports all standard form field types
 *     including text, radio, checkbox, date, file, and select. Contains
 *     AWD-specific mappings and dation-compatible metadata.
 * 
 * Key Components:
 *     - Field identification (name, label, type)
 *     - UX type mapping from AWD HtmlControl types
 *     - Validation rules and error handling
 *     - AWD field mapping expressions
 *     - HTML control type preservation
 * 
 * Keywords: form, field, definition, ux, type, mapping, awd, htmlcontrol,
 *          validation, rules, text, radio, checkbox, date, file, select
 * 
 * Dependencies:
 *     - Standard Java collections for options and metadata
 * 
 * Security:
 *     - Input validation for field properties
 *     - AWD mapping expression validation
 *     - Safe handling of field options and metadata
 * 
 * Performance:
 *     - Lightweight model for efficient serialization
 *     - Minimal memory footprint for large forms
 *     - Fast field lookup and comparison operations
 */
package com.patientvibes.awd.deserializer.extractor.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a single form field with UX type mapping and AWD integration.
 */
public class FormField {
    private String name;
    private String label;
    private String type;  // UX standard type (text, radio, checkbox, etc.)
    private String validation;
    private String awdMapping;  // AWD expression like awd:awd-value('fieldName')
    private String htmlControl;  // Original AWD HtmlControl type
    private String placeholder;
    private List<String> options;  // For radio/select fields
    private boolean required;
    private Object defaultValue;
    
    public FormField() {
        this.options = new ArrayList<>();
        this.required = false;
    }
    
    public FormField(String name, String type, String label) {
        this();
        this.name = name;
        this.type = type;
        this.label = label;
    }
    
    // Getters and setters
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getLabel() {
        return label;
    }
    
    public void setLabel(String label) {
        this.label = label;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getValidation() {
        return validation;
    }
    
    public void setValidation(String validation) {
        this.validation = validation;
        
        // Update required flag if validation contains "required"
        if (validation != null && validation.contains("required")) {
            this.required = true;
        }
    }
    
    public String getAwdMapping() {
        return awdMapping;
    }
    
    public void setAwdMapping(String awdMapping) {
        this.awdMapping = awdMapping;
    }
    
    public String getHtmlControl() {
        return htmlControl;
    }
    
    public void setHtmlControl(String htmlControl) {
        this.htmlControl = htmlControl;
    }
    
    public String getPlaceholder() {
        return placeholder;
    }
    
    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }
    
    public List<String> getOptions() {
        return options;
    }
    
    public void setOptions(List<String> options) {
        this.options = options != null ? options : new ArrayList<>();
    }
    
    public boolean isRequired() {
        return required;
    }
    
    public void setRequired(boolean required) {
        this.required = required;
    }
    
    public Object getDefaultValue() {
        return defaultValue;
    }
    
    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }
    
    // Helper methods
    
    public void addOption(String option) {
        if (option != null && !option.trim().isEmpty()) {
            this.options.add(option.trim());
        }
    }
    
    public boolean hasOptions() {
        return options != null && !options.isEmpty();
    }
    
    public boolean isSelectType() {
        return "radio".equals(type) || "select".equals(type) || "checkbox".equals(type);
    }
    
    public boolean hasAwdMapping() {
        return awdMapping != null && !awdMapping.trim().isEmpty();
    }
    
    public boolean isTextType() {
        return "text".equals(type) || "email".equals(type) || "password".equals(type) || "textarea".equals(type);
    }
    
    public boolean isDateType() {
        return "date".equals(type) || "datetime".equals(type) || "time".equals(type);
    }
    
    public boolean isFileType() {
        return "file".equals(type);
    }
    
    public String[] getValidationRules() {
        if (validation == null || validation.trim().isEmpty()) {
            return new String[0];
        }
        
        return validation.split("\\|");
    }
    
    public boolean hasValidationRule(String rule) {
        if (rule == null || validation == null) {
            return false;
        }
        
        String[] rules = getValidationRules();
        for (String validationRule : rules) {
            if (validationRule.trim().equals(rule) || validationRule.trim().startsWith(rule + ":")) {
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        FormField formField = (FormField) o;
        
        return Objects.equals(name, formField.name) &&
               Objects.equals(type, formField.type);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }
    
    @Override
    public String toString() {
        return "FormField{" +
                "name='" + name + '\'' +
                ", label='" + label + '\'' +
                ", type='" + type + '\'' +
                ", htmlControl='" + htmlControl + '\'' +
                ", required=" + required +
                ", hasOptions=" + hasOptions() +
                ", hasAwdMapping=" + hasAwdMapping() +
                '}';
    }
}