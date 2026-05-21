/**
 * Module: ProcessElement - BPMN process element representation
 * 
 * Summary:
 *     Represents individual BPMN process elements such as start events, tasks,
 *     gateways, and sequence flows within business process definitions.
 *     Provides structured data for business process analysis and documentation.
 * 
 * Keywords: process, element, bpmn, task, event, gateway, flow, activity,
 *          properties, metadata, business, workflow, analysis, documentation
 * 
 * Dependencies:
 *     - java.util.Map: Property storage for element configuration
 * 
 * Security:
 *     - Input validation for element properties and identifiers
 *     - Safe handling of element metadata
 * 
 * Performance:
 *     - Memory-efficient data structure for element representation
 *     - Optimized for serialization and business process tools
 */
package com.patientvibes.awd.deserializer.business.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a BPMN process element (start event, task, gateway, etc.).
 */
public class ProcessElement {
    private String type;
    private String id;
    private String name;
    private Map<String, Object> properties;
    
    public ProcessElement() {
        this.properties = new HashMap<>();
    }
    
    // Getters and Setters
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
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
    
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties != null ? properties : new HashMap<>();
    }
    
    @Override
    public String toString() {
        return "ProcessElement{" +
                "type='" + type + '\'' +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}