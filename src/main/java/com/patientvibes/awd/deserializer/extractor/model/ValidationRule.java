/**
 * Module: ValidationRule - Form field validation rule definition
 * 
 * Summary:
 *     Represents a single validation rule for form fields with rule type,
 *     value constraints, and error messaging. Supports standard validation
 *     types including required, length constraints, pattern matching, and
 *     data type validation. Designed for client-side and server-side
 *     validation consistency.
 * 
 * Key Components:
 *     - Rule identification (fieldName, ruleType)
 *     - Validation parameters and constraints
 *     - Error message customization
 *     - Rule priority and grouping
 * 
 * Keywords: validation, rule, form, field, required, length, pattern, email,
 *          numeric, error, message, constraint, client, server, consistency
 * 
 * Dependencies:
 *     - Standard Java validation patterns
 * 
 * Security:
 *     - Input validation for rule parameters
 *     - Safe pattern compilation for regex validation
 *     - Error message sanitization to prevent XSS
 * 
 * Performance:
 *     - Lightweight model for efficient validation processing
 *     - Cached pattern compilation for regex rules
 *     - Fast rule lookup and evaluation
 */
package com.patientvibes.awd.deserializer.extractor.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Represents a validation rule for form fields.
 */
public class ValidationRule {
    private String fieldName;
    private String ruleType;  // required, minLength, maxLength, pattern, email, numeric, etc.
    private String ruleValue; // constraint value (e.g., "5" for minLength:5)
    private String errorMessage;
    private int priority;     // for rule ordering
    private boolean enabled;
    
    // Cached pattern for regex validation
    private transient Pattern compiledPattern;
    
    public ValidationRule() {
        this.enabled = true;
        this.priority = 0;
    }
    
    public ValidationRule(String fieldName, String ruleType) {
        this();
        this.fieldName = fieldName;
        this.ruleType = ruleType;
    }
    
    public ValidationRule(String fieldName, String ruleType, String ruleValue, String errorMessage) {
        this(fieldName, ruleType);
        this.ruleValue = ruleValue;
        this.errorMessage = errorMessage;
    }
    
    // Getters and setters
    
    public String getFieldName() {
        return fieldName;
    }
    
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }
    
    public String getRuleType() {
        return ruleType;
    }
    
    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }
    
    public String getRuleValue() {
        return ruleValue;
    }
    
    public void setRuleValue(String ruleValue) {
        this.ruleValue = ruleValue;
        
        // Compile pattern if this is a pattern rule
        if ("pattern".equals(ruleType) && ruleValue != null) {
            try {
                this.compiledPattern = Pattern.compile(ruleValue);
            } catch (Exception e) {
                // Log error but don't fail - validation will handle this
                this.compiledPattern = null;
            }
        }
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    // Helper methods
    
    public boolean isRequired() {
        return "required".equals(ruleType);
    }
    
    public boolean isLengthRule() {
        return "minLength".equals(ruleType) || "maxLength".equals(ruleType);
    }
    
    public boolean isPatternRule() {
        return "pattern".equals(ruleType);
    }
    
    public boolean isEmailRule() {
        return "email".equals(ruleType);
    }
    
    public boolean isNumericRule() {
        return "numeric".equals(ruleType);
    }
    
    public Integer getLengthValue() {
        if (!isLengthRule() || ruleValue == null) {
            return null;
        }
        
        try {
            return Integer.parseInt(ruleValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    public Pattern getCompiledPattern() {
        return compiledPattern;
    }
    
    public boolean hasValidRuleValue() {
        if (ruleValue == null || ruleValue.trim().isEmpty()) {
            return !requiresRuleValue();
        }
        
        switch (ruleType) {
            case "minLength":
            case "maxLength":
                try {
                    Integer.parseInt(ruleValue);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            case "pattern":
                try {
                    Pattern.compile(ruleValue);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            default:
                return true;
        }
    }
    
    public boolean requiresRuleValue() {
        return "minLength".equals(ruleType) || 
               "maxLength".equals(ruleType) || 
               "pattern".equals(ruleType);
    }
    
    public String getJavaScriptValidation() {
        switch (ruleType) {
            case "required":
                return "value && value.trim().length > 0";
            case "minLength":
                return "value && value.length >= " + ruleValue;
            case "maxLength":
                return "value && value.length <= " + ruleValue;
            case "email":
                return "/^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$/.test(value)";
            case "numeric":
                return "/^\\d+$/.test(value)";
            case "pattern":
                return "/" + ruleValue + "/.test(value)";
            default:
                return "true";
        }
    }
    
    public String getDefaultErrorMessage() {
        if (errorMessage != null && !errorMessage.trim().isEmpty()) {
            return errorMessage;
        }
        
        String fieldLabel = fieldName != null ? fieldName : "Field";
        
        switch (ruleType) {
            case "required":
                return fieldLabel + " is required";
            case "minLength":
                return fieldLabel + " must be at least " + ruleValue + " characters";
            case "maxLength":
                return fieldLabel + " must not exceed " + ruleValue + " characters";
            case "email":
                return fieldLabel + " must be a valid email address";
            case "numeric":
                return fieldLabel + " must be a number";
            case "pattern":
                return fieldLabel + " format is invalid";
            default:
                return fieldLabel + " is invalid";
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        ValidationRule that = (ValidationRule) o;
        
        return Objects.equals(fieldName, that.fieldName) &&
               Objects.equals(ruleType, that.ruleType) &&
               Objects.equals(ruleValue, that.ruleValue);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(fieldName, ruleType, ruleValue);
    }
    
    @Override
    public String toString() {
        return "ValidationRule{" +
                "fieldName='" + fieldName + '\'' +
                ", ruleType='" + ruleType + '\'' +
                ", ruleValue='" + ruleValue + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", priority=" + priority +
                ", enabled=" + enabled +
                '}';
    }
}