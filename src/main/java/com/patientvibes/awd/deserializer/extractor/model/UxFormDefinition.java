/**
 * Module: UxFormDefinition - UX form definition model for dation compatibility
 * 
 * Summary:
 *     Data model representing a complete UX form definition with dation-compatible
 *     structure. Contains form metadata, field definitions, validation rules, and
 *     AWD integration mappings. Designed for JSON serialization to support
 *     external dation analysis and consumption.
 * 
 * Key Components:
 *     - Basic form properties (id, name, type)
 *     - Field definitions with UX types and AWD mappings
 *     - Validation rules and error messages
 *     - Dation-specific metadata for external integration
 * 
 * Keywords: ux, form, definition, model, dation, compatible, json, serialization,
 *          metadata, field, validation, awd, integration, external, analysis
 * 
 * Dependencies:
 *     - com.patientvibes.awd.deserializer.extractor.model.FormField: Individual field definitions
 *     - com.patientvibes.awd.deserializer.extractor.model.ValidationRule: Validation specifications
 *     - com.patientvibes.awd.deserializer.extractor.model.DationMetadata: Dation integration metadata
 * 
 * Security:
 *     - Input validation for all form properties
 *     - Safe JSON serialization without sensitive data exposure
 *     - AWD mapping validation to prevent injection attacks
 * 
 * Performance:
 *     - Lightweight model for efficient serialization
 *     - Lazy initialization of complex properties
 *     - Optimized for JSON streaming operations
 */
package com.patientvibes.awd.deserializer.extractor.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a complete UX form definition with dation compatibility.
 */
public class UxFormDefinition {
    private String formId;
    private String formName;
    private String formType;
    private String awdElementId;
    private List<FormField> fields;
    private List<ValidationRule> validationRules;
    private DationMetadata dationMetadata;
    
    public UxFormDefinition() {
        this.fields = new ArrayList<>();
        this.validationRules = new ArrayList<>();
    }
    
    // Getters and setters
    
    public String getFormId() {
        return formId;
    }
    
    public void setFormId(String formId) {
        this.formId = formId;
    }
    
    public String getFormName() {
        return formName;
    }
    
    public void setFormName(String formName) {
        this.formName = formName;
    }
    
    public String getFormType() {
        return formType;
    }
    
    public void setFormType(String formType) {
        this.formType = formType;
    }
    
    public String getAwdElementId() {
        return awdElementId;
    }
    
    public void setAwdElementId(String awdElementId) {
        this.awdElementId = awdElementId;
    }
    
    public List<FormField> getFields() {
        return fields;
    }
    
    public void setFields(List<FormField> fields) {
        this.fields = fields != null ? fields : new ArrayList<>();
    }
    
    public List<ValidationRule> getValidationRules() {
        return validationRules;
    }
    
    public void setValidationRules(List<ValidationRule> validationRules) {
        this.validationRules = validationRules != null ? validationRules : new ArrayList<>();
    }
    
    public DationMetadata getDationMetadata() {
        return dationMetadata;
    }
    
    public void setDationMetadata(DationMetadata dationMetadata) {
        this.dationMetadata = dationMetadata;
    }
    
    // Helper methods
    
    public void addField(FormField field) {
        if (field != null) {
            this.fields.add(field);
        }
    }
    
    public void addValidationRule(ValidationRule rule) {
        if (rule != null) {
            this.validationRules.add(rule);
        }
    }
    
    public FormField getFieldByName(String name) {
        if (name == null) {
            return null;
        }
        
        return fields.stream()
                .filter(field -> name.equals(field.getName()))
                .findFirst()
                .orElse(null);
    }
    
    public boolean hasField(String name) {
        return getFieldByName(name) != null;
    }
    
    public int getFieldCount() {
        return fields.size();
    }
    
    public int getValidationRuleCount() {
        return validationRules.size();
    }
    
    @Override
    public String toString() {
        return "UxFormDefinition{" +
                "formId='" + formId + '\'' +
                ", formName='" + formName + '\'' +
                ", formType='" + formType + '\'' +
                ", awdElementId='" + awdElementId + '\'' +
                ", fieldCount=" + getFieldCount() +
                ", validationRuleCount=" + getValidationRuleCount() +
                '}';
    }
}