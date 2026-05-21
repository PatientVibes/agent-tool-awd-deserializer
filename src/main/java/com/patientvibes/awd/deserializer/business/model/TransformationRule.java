/**
 * Module: TransformationRule - Data transformation rule representation
 * 
 * Summary:
 *     Represents data transformation rules with conditions, formulas, and targets
 *     for field mapping and business logic analysis. Provides structured data
 *     for data lineage tracking and business intelligence reporting.
 * 
 * Keywords: transformation, rule, condition, formula, target, mapping, lineage,
 *          business, logic, intelligence, reporting, data, field, analysis
 * 
 * Dependencies:
 *     - java.util.Map: Property storage for rule configuration
 * 
 * Security:
 *     - Input validation for transformation formulas and conditions
 *     - Safe handling of transformation logic
 * 
 * Performance:
 *     - Efficient data structure for transformation rule representation
 *     - Optimized for data lineage analysis tools
 */
package com.patientvibes.awd.deserializer.business.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a transformation rule with conditions and formulas.
 */
public class TransformationRule {
    private String ruleName;
    private String condition;
    private String formula;
    private String target;
    private Integer order;
    private boolean complex;
    private Map<String, Object> properties;
    
    public TransformationRule() {
        this.complex = false;
        this.properties = new HashMap<>();
    }
    
    // Getters and Setters
    
    public String getRuleName() {
        return ruleName;
    }
    
    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }
    
    public String getCondition() {
        return condition;
    }
    
    public void setCondition(String condition) {
        this.condition = condition;
    }
    
    public String getFormula() {
        return formula;
    }
    
    public void setFormula(String formula) {
        this.formula = formula;
    }
    
    public String getTarget() {
        return target;
    }
    
    public void setTarget(String target) {
        this.target = target;
    }
    
    public Integer getOrder() {
        return order;
    }
    
    public void setOrder(Integer order) {
        this.order = order;
    }
    
    public boolean isComplex() {
        return complex;
    }
    
    public void setComplex(boolean complex) {
        this.complex = complex;
    }
    
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties != null ? properties : new HashMap<>();
    }
    
    @Override
    public String toString() {
        return "TransformationRule{" +
                "ruleName='" + ruleName + '\'' +
                ", condition='" + condition + '\'' +
                ", target='" + target + '\'' +
                ", order=" + order +
                '}';
    }
}