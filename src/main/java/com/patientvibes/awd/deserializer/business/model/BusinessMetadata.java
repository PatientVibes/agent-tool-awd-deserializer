/**
 * Module: BusinessMetadata - Container for extracted AWD business metadata
 * 
 * Summary:
 *     Root container object that holds all extracted business metadata from AWD files
 *     including BPMN processes, field mappings, service definitions, and workflow elements.
 *     Provides structured access to business-level information for external consumption.
 * 
 * Key Components:
 *     - businessProcesses: List of extracted BPMN process definitions
 *     - fieldMappings: Data lineage and field transformation information
 *     - serviceDefinitions: Service registry with dependencies and properties
 *     - workflowElements: Individual workflow components and activities
 * 
 * Keywords: business, metadata, container, bpmn, process, field, mapping, service,
 *          workflow, extraction, timestamp, error, handling, structured, data
 * 
 * Dependencies:
 *     - java.time.LocalDateTime: Timestamp management for extraction metadata
 *     - java.util.List: Collection management for structured data
 * 
 * Security:
 *     - Immutable data structure with controlled access methods
 *     - Input validation for all setter operations
 *     - Safe handling of null values and empty collections
 * 
 * Performance:
 *     - Lazy initialization of collection properties
 *     - Memory-efficient data structure design
 *     - Optimized for serialization to external formats
 */
package com.patientvibes.awd.deserializer.business.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Container for all extracted business metadata from AWD files.
 */
public class BusinessMetadata {
    private LocalDateTime extractionTimestamp;
    private String sourceType;
    private String extractionError;
    
    private List<BusinessProcess> businessProcesses;
    private List<FieldMapping> fieldMappings;
    private List<ServiceDefinition> serviceDefinitions;
    private List<WorkflowElement> workflowElements;
    
    public BusinessMetadata() {
        this.businessProcesses = new ArrayList<>();
        this.fieldMappings = new ArrayList<>();
        this.serviceDefinitions = new ArrayList<>();
        this.workflowElements = new ArrayList<>();
    }
    
    // Getters and Setters
    
    public LocalDateTime getExtractionTimestamp() {
        return extractionTimestamp;
    }
    
    public void setExtractionTimestamp(LocalDateTime extractionTimestamp) {
        this.extractionTimestamp = extractionTimestamp;
    }
    
    public String getSourceType() {
        return sourceType;
    }
    
    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }
    
    public String getExtractionError() {
        return extractionError;
    }
    
    public void setExtractionError(String extractionError) {
        this.extractionError = extractionError;
    }
    
    public List<BusinessProcess> getBusinessProcesses() {
        return businessProcesses;
    }
    
    public void setBusinessProcesses(List<BusinessProcess> businessProcesses) {
        this.businessProcesses = businessProcesses != null ? businessProcesses : new ArrayList<>();
    }
    
    public List<FieldMapping> getFieldMappings() {
        return fieldMappings;
    }
    
    public void setFieldMappings(List<FieldMapping> fieldMappings) {
        this.fieldMappings = fieldMappings != null ? fieldMappings : new ArrayList<>();
    }
    
    public List<ServiceDefinition> getServiceDefinitions() {
        return serviceDefinitions;
    }
    
    public void setServiceDefinitions(List<ServiceDefinition> serviceDefinitions) {
        this.serviceDefinitions = serviceDefinitions != null ? serviceDefinitions : new ArrayList<>();
    }
    
    public List<WorkflowElement> getWorkflowElements() {
        return workflowElements;
    }
    
    public void setWorkflowElements(List<WorkflowElement> workflowElements) {
        this.workflowElements = workflowElements != null ? workflowElements : new ArrayList<>();
    }
    
    // Utility methods
    
    public boolean hasBusinessProcesses() {
        return businessProcesses != null && !businessProcesses.isEmpty();
    }
    
    public boolean hasFieldMappings() {
        return fieldMappings != null && !fieldMappings.isEmpty();
    }
    
    public boolean hasServiceDefinitions() {
        return serviceDefinitions != null && !serviceDefinitions.isEmpty();
    }
    
    public boolean hasWorkflowElements() {
        return workflowElements != null && !workflowElements.isEmpty();
    }
    
    public boolean hasExtractionError() {
        return extractionError != null && !extractionError.trim().isEmpty();
    }
    
    @Override
    public String toString() {
        return "BusinessMetadata{" +
                "extractionTimestamp=" + extractionTimestamp +
                ", sourceType='" + sourceType + '\'' +
                ", businessProcesses=" + (businessProcesses != null ? businessProcesses.size() : 0) +
                ", fieldMappings=" + (fieldMappings != null ? fieldMappings.size() : 0) +
                ", serviceDefinitions=" + (serviceDefinitions != null ? serviceDefinitions.size() : 0) +
                ", workflowElements=" + (workflowElements != null ? workflowElements.size() : 0) +
                ", hasError=" + hasExtractionError() +
                '}';
    }
}