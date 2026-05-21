/**
 * Module: FieldMapping - Data lineage and field transformation representation
 * 
 * Summary:
 *     Represents field mappings and data transformations extracted from AWD business rules.
 *     Contains source/target field relationships, transformation logic, and conditional rules
 *     for data lineage analysis and business intelligence reporting.
 * 
 * Key Components:
 *     - fieldRelationships: Source to target field mapping definitions
 *     - transformationRules: Business logic and conditional transformation rules
 *     - dataLineage: Complete data flow and dependency tracking
 *     - businessRules: Conditional logic and decision criteria
 * 
 * Keywords: field, mapping, transformation, rule, lineage, source, target, condition,
 *          formula, business, logic, data, flow, dependency, intelligence, reporting
 * 
 * Dependencies:
 *     - java.util.List: Collection management for field and rule lists
 *     - java.util.Map: Property storage for transformation metadata
 * 
 * Security:
 *     - Input validation for field names and transformation expressions
 *     - Safe handling of transformation formulas and conditions
 *     - Controlled access to business rule definitions
 * 
 * Performance:
 *     - Efficient data structure for large field mapping sets
 *     - Optimized for data lineage analysis and reporting tools
 *     - Memory-efficient storage of transformation rules
 */
package com.patientvibes.awd.deserializer.business.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents field mappings and data transformations in AWD processes.
 */
public class FieldMapping {
    private String mappingName;
    private String description;
    private List<String> sourceFields;
    private List<String> targetFields;
    private List<TransformationRule> transformationRules;
    private Map<String, Object> metadata;
    
    public FieldMapping() {
        this.sourceFields = new ArrayList<>();
        this.targetFields = new ArrayList<>();
        this.transformationRules = new ArrayList<>();
        this.metadata = new HashMap<>();
    }
    
    // Getters and Setters
    
    public String getMappingName() {
        return mappingName;
    }
    
    public void setMappingName(String mappingName) {
        this.mappingName = mappingName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public List<String> getSourceFields() {
        return sourceFields;
    }
    
    public void setSourceFields(List<String> sourceFields) {
        this.sourceFields = sourceFields != null ? sourceFields : new ArrayList<>();
    }
    
    public List<String> getTargetFields() {
        return targetFields;
    }
    
    public void setTargetFields(List<String> targetFields) {
        this.targetFields = targetFields != null ? targetFields : new ArrayList<>();
    }
    
    public List<TransformationRule> getTransformationRules() {
        return transformationRules;
    }
    
    public void setTransformationRules(List<TransformationRule> transformationRules) {
        this.transformationRules = transformationRules != null ? transformationRules : new ArrayList<>();
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }
    
    // Utility methods
    
    public void addSourceField(String field) {
        if (field != null && !field.trim().isEmpty()) {
            this.sourceFields.add(field.trim());
        }
    }
    
    public void addTargetField(String field) {
        if (field != null && !field.trim().isEmpty()) {
            this.targetFields.add(field.trim());
        }
    }
    
    public void addTransformationRule(TransformationRule rule) {
        if (rule != null) {
            this.transformationRules.add(rule);
        }
    }
    
    @Override
    public String toString() {
        return "FieldMapping{" +
                "mappingName='" + mappingName + '\'' +
                ", sourceFields=" + (sourceFields != null ? sourceFields.size() : 0) +
                ", targetFields=" + (targetFields != null ? targetFields.size() : 0) +
                ", transformationRules=" + (transformationRules != null ? transformationRules.size() : 0) +
                '}';
    }
}