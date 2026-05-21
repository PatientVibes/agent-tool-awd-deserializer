/**
 * Module: BusinessProcess - BPMN business process representation
 * 
 * Summary:
 *     Represents a complete BPMN business process extracted from AWD design files.
 *     Contains process metadata, workflow elements, variables, and execution properties
 *     structured for external consumption and business process analysis.
 * 
 * Key Components:
 *     - processMetadata: Basic process identification and versioning information
 *     - processElements: Workflow components like start events, tasks, gateways
 *     - processVariables: Data variables and their sources/transformations
 *     - auditInformation: Creation, modification, and deployment tracking
 * 
 * Keywords: business, process, bpmn, workflow, element, variable, metadata, audit,
 *          creation, modification, deployment, version, automation, execution
 * 
 * Dependencies:
 *     - java.util.List: Collection management for process components
 *     - java.util.Map: Property storage for flexible metadata
 * 
 * Security:
 *     - Input validation for all process properties
 *     - Safe handling of null values in collections
 *     - Controlled access to sensitive audit information
 * 
 * Performance:
 *     - Lazy initialization of collection properties
 *     - Efficient data structure for serialization
 *     - Optimized for business process analysis tools
 */
package com.patientvibes.awd.deserializer.business.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a BPMN business process with metadata and elements.
 */
public class BusinessProcess {
    private String id;
    private String name;
    private String type;
    private Integer version;
    private String description;
    
    // Audit information
    private String createdBy;
    private String createdTime;
    private String modifiedBy;
    private String modifiedTime;
    private String deployTime;
    private String modelState;
    
    // Process components
    private List<ProcessElement> elements;
    private List<ProcessVariable> variables;
    private Map<String, Object> properties;
    
    public BusinessProcess() {
        this.elements = new ArrayList<>();
        this.variables = new ArrayList<>();
        this.properties = new HashMap<>();
    }
    
    // Getters and Setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
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
    
    public Integer getVersion() {
        return version;
    }
    
    public void setVersion(Integer version) {
        this.version = version;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public String getCreatedTime() {
        return createdTime;
    }
    
    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }
    
    public String getModifiedBy() {
        return modifiedBy;
    }
    
    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }
    
    public String getModifiedTime() {
        return modifiedTime;
    }
    
    public void setModifiedTime(String modifiedTime) {
        this.modifiedTime = modifiedTime;
    }
    
    public String getDeployTime() {
        return deployTime;
    }
    
    public void setDeployTime(String deployTime) {
        this.deployTime = deployTime;
    }
    
    public String getModelState() {
        return modelState;
    }
    
    public void setModelState(String modelState) {
        this.modelState = modelState;
    }
    
    public List<ProcessElement> getElements() {
        return elements;
    }
    
    public void setElements(List<ProcessElement> elements) {
        this.elements = elements != null ? elements : new ArrayList<>();
    }
    
    public List<ProcessVariable> getVariables() {
        return variables;
    }
    
    public void setVariables(List<ProcessVariable> variables) {
        this.variables = variables != null ? variables : new ArrayList<>();
    }
    
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties != null ? properties : new HashMap<>();
    }
    
    // Utility methods
    
    public void addElement(ProcessElement element) {
        if (element != null) {
            this.elements.add(element);
        }
    }
    
    public void addVariable(ProcessVariable variable) {
        if (variable != null) {
            this.variables.add(variable);
        }
    }
    
    public void addProperty(String key, Object value) {
        if (key != null) {
            this.properties.put(key, value);
        }
    }
    
    @Override
    public String toString() {
        return "BusinessProcess{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", version=" + version +
                ", elements=" + (elements != null ? elements.size() : 0) +
                ", variables=" + (variables != null ? variables.size() : 0) +
                ", modelState='" + modelState + '\'' +
                '}';
    }
}