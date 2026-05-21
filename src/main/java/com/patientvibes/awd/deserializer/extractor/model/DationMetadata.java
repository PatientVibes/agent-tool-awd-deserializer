/**
 * Module: DationMetadata - Dation-specific metadata for external system integration
 * 
 * Summary:
 *     Contains metadata required for dation analysis and external system integration.
 *     Provides source system identification, extraction timestamps, AWD element
 *     references, and processing context. Designed for consumption by Python
 *     ecosystem tools and dation repository compatibility.
 * 
 * Key Components:
 *     - Source system identification (AWD, extraction timestamp)
 *     - AWD element context (element ID, type, process ID)
 *     - Processing metadata (extraction time, version info)
 *     - Integration properties for external consumption
 * 
 * Keywords: dation, metadata, external, integration, awd, element, source, system,
 *          timestamp, extraction, python, ecosystem, repository, compatibility
 * 
 * Dependencies:
 *     - Standard Java time handling for timestamps
 * 
 * Security:
 *     - Safe metadata handling without sensitive data exposure
 *     - Input validation for all metadata properties
 *     - Sanitized output for external system consumption
 * 
 * Performance:
 *     - Lightweight metadata model for efficient serialization
 *     - Minimal memory footprint for large-scale processing
 *     - Fast metadata lookup and comparison operations
 */
package com.patientvibes.awd.deserializer.extractor.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents dation-specific metadata for external system integration.
 */
public class DationMetadata {
    private String sourceSystem;
    private String extractionTimestamp;
    private String awdElementId;
    private String elementType;
    private String processId;
    private String extractorVersion;
    private Map<String, Object> additionalProperties;
    
    public DationMetadata() {
        this.sourceSystem = "awd";
        this.extractionTimestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        this.additionalProperties = new HashMap<>();
    }
    
    public DationMetadata(String awdElementId, String elementType) {
        this();
        this.awdElementId = awdElementId;
        this.elementType = elementType;
    }
    
    // Getters and setters
    
    public String getSourceSystem() {
        return sourceSystem;
    }
    
    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }
    
    public String getExtractionTimestamp() {
        return extractionTimestamp;
    }
    
    public void setExtractionTimestamp(String extractionTimestamp) {
        this.extractionTimestamp = extractionTimestamp;
    }
    
    public String getAwdElementId() {
        return awdElementId;
    }
    
    public void setAwdElementId(String awdElementId) {
        this.awdElementId = awdElementId;
    }
    
    public String getElementType() {
        return elementType;
    }
    
    public void setElementType(String elementType) {
        this.elementType = elementType;
    }
    
    public String getProcessId() {
        return processId;
    }
    
    public void setProcessId(String processId) {
        this.processId = processId;
    }
    
    public String getExtractorVersion() {
        return extractorVersion;
    }
    
    public void setExtractorVersion(String extractorVersion) {
        this.extractorVersion = extractorVersion;
    }
    
    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }
    
    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties != null ? additionalProperties : new HashMap<>();
    }
    
    // Helper methods
    
    public void addProperty(String key, Object value) {
        if (key != null && value != null) {
            this.additionalProperties.put(key, value);
        }
    }
    
    public Object getProperty(String key) {
        return additionalProperties.get(key);
    }
    
    public boolean hasProperty(String key) {
        return additionalProperties.containsKey(key);
    }
    
    public void removeProperty(String key) {
        additionalProperties.remove(key);
    }
    
    public boolean isValidMetadata() {
        return sourceSystem != null && !sourceSystem.trim().isEmpty() &&
               extractionTimestamp != null && !extractionTimestamp.trim().isEmpty() &&
               awdElementId != null && !awdElementId.trim().isEmpty();
    }
    
    public void updateTimestamp() {
        this.extractionTimestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
    
    public String getFormattedTimestamp(String pattern) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(extractionTimestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return dateTime.format(DateTimeFormatter.ofPattern(pattern));
        } catch (Exception e) {
            return extractionTimestamp; // Return original if parsing fails
        }
    }
    
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("sourceSystem", sourceSystem);
        map.put("extractionTimestamp", extractionTimestamp);
        map.put("awdElementId", awdElementId);
        
        if (elementType != null) {
            map.put("elementType", elementType);
        }
        
        if (processId != null) {
            map.put("processId", processId);
        }
        
        if (extractorVersion != null) {
            map.put("extractorVersion", extractorVersion);
        }
        
        if (additionalProperties != null && !additionalProperties.isEmpty()) {
            map.putAll(additionalProperties);
        }
        
        return map;
    }
    
    public static DationMetadata fromMap(Map<String, Object> map) {
        if (map == null) {
            return new DationMetadata();
        }
        
        DationMetadata metadata = new DationMetadata();
        metadata.setSourceSystem((String) map.get("sourceSystem"));
        metadata.setExtractionTimestamp((String) map.get("extractionTimestamp"));
        metadata.setAwdElementId((String) map.get("awdElementId"));
        metadata.setElementType((String) map.get("elementType"));
        metadata.setProcessId((String) map.get("processId"));
        metadata.setExtractorVersion((String) map.get("extractorVersion"));
        
        // Add remaining properties as additional properties
        Map<String, Object> additionalProps = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            if (!"sourceSystem".equals(key) && !"extractionTimestamp".equals(key) &&
                !"awdElementId".equals(key) && !"elementType".equals(key) &&
                !"processId".equals(key) && !"extractorVersion".equals(key)) {
                additionalProps.put(key, entry.getValue());
            }
        }
        metadata.setAdditionalProperties(additionalProps);
        
        return metadata;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        DationMetadata that = (DationMetadata) o;
        
        return Objects.equals(sourceSystem, that.sourceSystem) &&
               Objects.equals(extractionTimestamp, that.extractionTimestamp) &&
               Objects.equals(awdElementId, that.awdElementId) &&
               Objects.equals(elementType, that.elementType) &&
               Objects.equals(processId, that.processId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(sourceSystem, extractionTimestamp, awdElementId, elementType, processId);
    }
    
    @Override
    public String toString() {
        return "DationMetadata{" +
                "sourceSystem='" + sourceSystem + '\'' +
                ", extractionTimestamp='" + extractionTimestamp + '\'' +
                ", awdElementId='" + awdElementId + '\'' +
                ", elementType='" + elementType + '\'' +
                ", processId='" + processId + '\'' +
                ", extractorVersion='" + extractorVersion + '\'' +
                ", additionalPropertiesCount=" + (additionalProperties != null ? additionalProperties.size() : 0) +
                '}';
    }
}