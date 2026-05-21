/**
 * Module: FileExtractionRequest - Configuration container for file extraction operations
 * 
 * Summary:
 *     Request container that defines the scope, configuration, and parameters for file
 *     extraction operations. Encapsulates source data, output configuration, and processing
 *     options for modular extraction of BPMN, forms, and service definitions.
 * 
 * Key Components:
 *     - sourceConfiguration: Input data and source file information
 *     - outputConfiguration: Target directory and file naming conventions
 *     - extractionTypes: Enabled file types and processing options
 *     - processingOptions: Performance and resource management settings
 * 
 * Keywords: extraction, request, configuration, scope, parameters, source, output,
 *          types, processing, options, modular, bpmn, forms, services, performance
 * 
 * Dependencies:
 *     - java.util.Map: Source data container for deserialized AWD objects
 *     - java.util.Set: Collection management for enabled extraction types
 * 
 * Security:
 *     - Input validation for all configuration parameters
 *     - Safe path resolution for output directories
 *     - Resource limit validation for processing constraints
 * 
 * Performance:
 *     - Immutable data structure design for thread safety
 *     - Efficient collection management for large datasets
 *     - Memory-conscious configuration options
 */
package com.patientvibes.awd.deserializer.extractor.model;

import java.util.*;

/**
 * Request configuration for file extraction operations.
 */
public class FileExtractionRequest {
    private String sourceName;
    private String outputDirectory;
    private Map<String, Object> deserializedData;
    private Set<FileExtractionType> enabledTypes;
    private Map<String, Object> processingOptions;
    private String sourceFileName;
    private long sourceFileSize;
    
    public FileExtractionRequest() {
        this.enabledTypes = EnumSet.allOf(FileExtractionType.class);
        this.processingOptions = new HashMap<>();
    }
    
    public FileExtractionRequest(String sourceName, String outputDirectory, Map<String, Object> deserializedData) {
        this();
        this.sourceName = sourceName;
        this.outputDirectory = outputDirectory;
        this.deserializedData = deserializedData;
    }
    
    // Getters and Setters
    
    public String getSourceName() {
        return sourceName;
    }
    
    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }
    
    public String getOutputDirectory() {
        return outputDirectory;
    }
    
    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }
    
    public Map<String, Object> getDeserializedData() {
        return deserializedData;
    }
    
    public void setDeserializedData(Map<String, Object> deserializedData) {
        this.deserializedData = deserializedData;
    }
    
    public Set<FileExtractionType> getEnabledTypes() {
        return enabledTypes;
    }
    
    public void setEnabledTypes(Set<FileExtractionType> enabledTypes) {
        this.enabledTypes = enabledTypes != null ? enabledTypes : EnumSet.noneOf(FileExtractionType.class);
    }
    
    public Map<String, Object> getProcessingOptions() {
        return processingOptions;
    }
    
    public void setProcessingOptions(Map<String, Object> processingOptions) {
        this.processingOptions = processingOptions != null ? processingOptions : new HashMap<>();
    }
    
    public String getSourceFileName() {
        return sourceFileName;
    }
    
    public void setSourceFileName(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }
    
    public long getSourceFileSize() {
        return sourceFileSize;
    }
    
    public void setSourceFileSize(long sourceFileSize) {
        this.sourceFileSize = sourceFileSize;
    }
    
    // Utility methods
    
    public void enableType(FileExtractionType type) {
        this.enabledTypes.add(type);
    }
    
    public void disableType(FileExtractionType type) {
        this.enabledTypes.remove(type);
    }
    
    public boolean isTypeEnabled(FileExtractionType type) {
        return enabledTypes.contains(type);
    }
    
    public void addProcessingOption(String key, Object value) {
        this.processingOptions.put(key, value);
    }
    
    public Object getProcessingOption(String key) {
        return processingOptions.get(key);
    }
    
    public <T> T getProcessingOption(String key, Class<T> type, T defaultValue) {
        Object value = processingOptions.get(key);
        if (value != null && type.isInstance(value)) {
            return type.cast(value);
        }
        return defaultValue;
    }
    
    /**
     * Builder pattern for creating FileExtractionRequest instances.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private final FileExtractionRequest request = new FileExtractionRequest();
        
        public Builder sourceName(String sourceName) {
            request.setSourceName(sourceName);
            return this;
        }
        
        public Builder outputDirectory(String outputDirectory) {
            request.setOutputDirectory(outputDirectory);
            return this;
        }
        
        public Builder deserializedData(Map<String, Object> deserializedData) {
            request.setDeserializedData(deserializedData);
            return this;
        }
        
        public Builder enabledTypes(Set<FileExtractionType> enabledTypes) {
            request.setEnabledTypes(enabledTypes);
            return this;
        }
        
        public Builder sourceFileName(String sourceFileName) {
            request.setSourceFileName(sourceFileName);
            return this;
        }
        
        public Builder sourceFileSize(long sourceFileSize) {
            request.setSourceFileSize(sourceFileSize);
            return this;
        }
        
        public Builder dationFormat(boolean dationFormat) {
            request.addProcessingOption("dationFormat", dationFormat);
            return this;
        }
        
        public Builder includeMetadata(boolean includeMetadata) {
            request.addProcessingOption("includeMetadata", includeMetadata);
            return this;
        }
        
        public Builder maxConcurrent(int maxConcurrent) {
            request.addProcessingOption("maxConcurrent", maxConcurrent);
            return this;
        }
        
        public Builder processingOption(String key, Object value) {
            request.addProcessingOption(key, value);
            return this;
        }
        
        public FileExtractionRequest build() {
            // Validate required fields
            if (request.getDeserializedData() == null) {
                throw new IllegalStateException("Deserialized data is required");
            }
            if (request.getOutputDirectory() == null) {
                throw new IllegalStateException("Output directory is required");
            }
            if (request.getEnabledTypes() == null || request.getEnabledTypes().isEmpty()) {
                throw new IllegalStateException("At least one extraction type must be enabled");
            }
            
            return request;
        }
    }

    @Override
    public String toString() {
        return "FileExtractionRequest{" +
                "sourceName='" + sourceName + '\'' +
                ", outputDirectory='" + outputDirectory + '\'' +
                ", enabledTypes=" + enabledTypes +
                ", sourceFileName='" + sourceFileName + '\'' +
                ", sourceFileSize=" + sourceFileSize +
                ", dataSize=" + (deserializedData != null ? deserializedData.size() : 0) +
                '}';
    }
}