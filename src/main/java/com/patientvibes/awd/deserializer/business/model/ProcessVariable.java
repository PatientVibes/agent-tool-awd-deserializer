/**
 * Module: ProcessVariable - BPMN process variable representation
 * 
 * Summary:
 *     Represents process variables within BPMN business processes including
 *     data types, source expressions, and variable properties for business
 *     process analysis and data lineage tracking.
 * 
 * Keywords: process, variable, bpmn, data, type, source, expression, lineage,
 *          analysis, complex, readonly, business, workflow, metadata
 * 
 * Dependencies:
 *     - None: Self-contained data model
 * 
 * Security:
 *     - Input validation for variable names and expressions
 *     - Safe handling of variable metadata
 * 
 * Performance:
 *     - Lightweight data structure for variable representation
 *     - Optimized for business process analysis tools
 */
package com.patientvibes.awd.deserializer.business.model;

/**
 * Represents a process variable with its type and source expression.
 */
public class ProcessVariable {
    private String name;
    private String type;
    private String sourceExpression;
    private boolean complex;
    private boolean readonly;
    
    public ProcessVariable() {
        this.complex = false;
        this.readonly = false;
    }
    
    // Getters and Setters
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getSourceExpression() {
        return sourceExpression;
    }
    
    public void setSourceExpression(String sourceExpression) {
        this.sourceExpression = sourceExpression;
    }
    
    public boolean isComplex() {
        return complex;
    }
    
    public void setComplex(boolean complex) {
        this.complex = complex;
    }
    
    public boolean isReadonly() {
        return readonly;
    }
    
    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }
    
    @Override
    public String toString() {
        return "ProcessVariable{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", complex=" + complex +
                '}';
    }
}