/**
 * Module: ExtractionConfig - Configuration container for file extraction operations
 * 
 * Summary:
 *     Configuration class that defines performance, resource, and processing parameters
 *     for file extraction operations. Controls concurrent processing, memory limits,
 *     timeout settings, and output formatting options for modular extraction.
 * 
 * Key Components:
 *     - performanceSettings: Thread pool size, timeout, and processing limits
 *     - resourceLimits: Memory bounds, file size limits, and resource constraints
 *     - outputConfiguration: File naming, formatting, and directory options
 *     - processingOptions: Feature flags and extraction behavior controls
 * 
 * Keywords: extraction, configuration, performance, settings, resource, limits, timeout,
 *          processing, concurrent, memory, bounds, output, formatting, behavior, controls
 * 
 * Dependencies:
 *     - java.nio.file.Path: Path configuration for output directories
 *     - java.util.Map: Configuration parameter storage
 * 
 * Security:
 *     - Input validation for all configuration parameters
 *     - Safe resource limit enforcement
 *     - Controlled concurrent processing limits
 * 
 * Performance:
 *     - Optimized default values for enterprise workloads
 *     - Configurable resource allocation and management
 *     - Memory-conscious processing parameters
 */
package com.patientvibes.awd.deserializer.extractor.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for file extraction operations.
 */
public class ExtractionConfig {
    
    // Performance settings
    private int maxConcurrentExtractions = 4;
    private int extractionTimeoutMinutes = 30;
    private long maxMemoryUsageMB = 512;
    private int maxFilesPerType = 1000;
    
    // Processing options
    private boolean enableParallelProcessing = true;
    private boolean generateMetadata = true;
    private boolean validateOutput = true;
    private boolean compressOutput = false;
    
    // Output configuration
    private String fileNamingPattern = "{processName}_{type}_{timestamp}";
    private String timestampFormat = "yyyyMMdd_HHmmss";
    private boolean createTypeDirectories = true;
    private boolean preserveSourceStructure = false;
    
    // BPMN-specific settings
    private boolean includeAwdExtensions = true;
    private boolean validateBpmnSchema = true;
    private String awdNamespace = "http://www.dstawd.com";
    private boolean prettyPrintXml = true;
    
    // Form-specific settings
    private boolean generateCss = true;
    private boolean includeJavaScript = false;
    private String formTheme = "default";
    private boolean responsiveDesign = true;
    
    // Service-specific settings
    private boolean includeServiceDependencies = true;
    private boolean generateSwaggerDocs = false;
    private String serviceConfigFormat = "json";
    
    // Advanced options
    private Map<String, Object> customOptions;
    
    public ExtractionConfig() {
        this.customOptions = new HashMap<>();
    }
    
    // Getters and Setters
    
    public int getMaxConcurrentExtractions() {
        return maxConcurrentExtractions;
    }
    
    public void setMaxConcurrentExtractions(int maxConcurrentExtractions) {
        if (maxConcurrentExtractions < 1 || maxConcurrentExtractions > 20) {
            throw new IllegalArgumentException("Max concurrent extractions must be between 1 and 20");
        }
        this.maxConcurrentExtractions = maxConcurrentExtractions;
    }
    
    public int getExtractionTimeoutMinutes() {
        return extractionTimeoutMinutes;
    }
    
    public void setExtractionTimeoutMinutes(int extractionTimeoutMinutes) {
        if (extractionTimeoutMinutes < 1 || extractionTimeoutMinutes > 120) {
            throw new IllegalArgumentException("Extraction timeout must be between 1 and 120 minutes");
        }
        this.extractionTimeoutMinutes = extractionTimeoutMinutes;
    }
    
    public long getMaxMemoryUsageMB() {
        return maxMemoryUsageMB;
    }
    
    public void setMaxMemoryUsageMB(long maxMemoryUsageMB) {
        if (maxMemoryUsageMB < 64 || maxMemoryUsageMB > 4096) {
            throw new IllegalArgumentException("Max memory usage must be between 64MB and 4096MB");
        }
        this.maxMemoryUsageMB = maxMemoryUsageMB;
    }
    
    public int getMaxFilesPerType() {
        return maxFilesPerType;
    }
    
    public void setMaxFilesPerType(int maxFilesPerType) {
        if (maxFilesPerType < 1 || maxFilesPerType > 10000) {
            throw new IllegalArgumentException("Max files per type must be between 1 and 10000");
        }
        this.maxFilesPerType = maxFilesPerType;
    }
    
    public boolean isEnableParallelProcessing() {
        return enableParallelProcessing;
    }
    
    public void setEnableParallelProcessing(boolean enableParallelProcessing) {
        this.enableParallelProcessing = enableParallelProcessing;
    }
    
    public boolean isGenerateMetadata() {
        return generateMetadata;
    }
    
    public void setGenerateMetadata(boolean generateMetadata) {
        this.generateMetadata = generateMetadata;
    }
    
    public boolean isValidateOutput() {
        return validateOutput;
    }
    
    public void setValidateOutput(boolean validateOutput) {
        this.validateOutput = validateOutput;
    }
    
    public boolean isCompressOutput() {
        return compressOutput;
    }
    
    public void setCompressOutput(boolean compressOutput) {
        this.compressOutput = compressOutput;
    }
    
    public String getFileNamingPattern() {
        return fileNamingPattern;
    }
    
    public void setFileNamingPattern(String fileNamingPattern) {
        if (fileNamingPattern == null || fileNamingPattern.trim().isEmpty()) {
            throw new IllegalArgumentException("File naming pattern cannot be null or empty");
        }
        this.fileNamingPattern = fileNamingPattern;
    }
    
    public String getTimestampFormat() {
        return timestampFormat;
    }
    
    public void setTimestampFormat(String timestampFormat) {
        if (timestampFormat == null || timestampFormat.trim().isEmpty()) {
            throw new IllegalArgumentException("Timestamp format cannot be null or empty");
        }
        this.timestampFormat = timestampFormat;
    }
    
    public boolean isCreateTypeDirectories() {
        return createTypeDirectories;
    }
    
    public void setCreateTypeDirectories(boolean createTypeDirectories) {
        this.createTypeDirectories = createTypeDirectories;
    }
    
    public boolean isPreserveSourceStructure() {
        return preserveSourceStructure;
    }
    
    public void setPreserveSourceStructure(boolean preserveSourceStructure) {
        this.preserveSourceStructure = preserveSourceStructure;
    }
    
    public boolean isIncludeAwdExtensions() {
        return includeAwdExtensions;
    }
    
    public void setIncludeAwdExtensions(boolean includeAwdExtensions) {
        this.includeAwdExtensions = includeAwdExtensions;
    }
    
    public boolean isValidateBpmnSchema() {
        return validateBpmnSchema;
    }
    
    public void setValidateBpmnSchema(boolean validateBpmnSchema) {
        this.validateBpmnSchema = validateBpmnSchema;
    }
    
    public String getAwdNamespace() {
        return awdNamespace;
    }
    
    public void setAwdNamespace(String awdNamespace) {
        if (awdNamespace == null || awdNamespace.trim().isEmpty()) {
            throw new IllegalArgumentException("AWD namespace cannot be null or empty");
        }
        this.awdNamespace = awdNamespace;
    }
    
    public boolean isPrettyPrintXml() {
        return prettyPrintXml;
    }
    
    public void setPrettyPrintXml(boolean prettyPrintXml) {
        this.prettyPrintXml = prettyPrintXml;
    }
    
    public boolean isGenerateCss() {
        return generateCss;
    }
    
    public void setGenerateCss(boolean generateCss) {
        this.generateCss = generateCss;
    }
    
    public boolean isIncludeJavaScript() {
        return includeJavaScript;
    }
    
    public void setIncludeJavaScript(boolean includeJavaScript) {
        this.includeJavaScript = includeJavaScript;
    }
    
    public String getFormTheme() {
        return formTheme;
    }
    
    public void setFormTheme(String formTheme) {
        this.formTheme = formTheme != null ? formTheme : "default";
    }
    
    public boolean isResponsiveDesign() {
        return responsiveDesign;
    }
    
    public void setResponsiveDesign(boolean responsiveDesign) {
        this.responsiveDesign = responsiveDesign;
    }
    
    public boolean isIncludeServiceDependencies() {
        return includeServiceDependencies;
    }
    
    public void setIncludeServiceDependencies(boolean includeServiceDependencies) {
        this.includeServiceDependencies = includeServiceDependencies;
    }
    
    public boolean isGenerateSwaggerDocs() {
        return generateSwaggerDocs;
    }
    
    public void setGenerateSwaggerDocs(boolean generateSwaggerDocs) {
        this.generateSwaggerDocs = generateSwaggerDocs;
    }
    
    public String getServiceConfigFormat() {
        return serviceConfigFormat;
    }
    
    public void setServiceConfigFormat(String serviceConfigFormat) {
        if (!"json".equals(serviceConfigFormat) && !"yaml".equals(serviceConfigFormat)) {
            throw new IllegalArgumentException("Service config format must be 'json' or 'yaml'");
        }
        this.serviceConfigFormat = serviceConfigFormat;
    }
    
    public Map<String, Object> getCustomOptions() {
        return customOptions;
    }
    
    public void setCustomOptions(Map<String, Object> customOptions) {
        this.customOptions = customOptions != null ? customOptions : new HashMap<>();
    }
    
    // Utility methods
    
    public void addCustomOption(String key, Object value) {
        this.customOptions.put(key, value);
    }
    
    public Object getCustomOption(String key) {
        return customOptions.get(key);
    }
    
    public <T> T getCustomOption(String key, Class<T> type, T defaultValue) {
        Object value = customOptions.get(key);
        if (value != null && type.isInstance(value)) {
            return type.cast(value);
        }
        return defaultValue;
    }
    
    /**
     * Builder pattern for creating ExtractionConfig instances.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private final ExtractionConfig config = new ExtractionConfig();
        
        public Builder maxConcurrentExtractions(int maxConcurrentExtractions) {
            config.setMaxConcurrentExtractions(maxConcurrentExtractions);
            return this;
        }
        
        public Builder extractionTimeoutMinutes(int extractionTimeoutMinutes) {
            config.setExtractionTimeoutMinutes(extractionTimeoutMinutes);
            return this;
        }
        
        public Builder maxMemoryUsageMB(long maxMemoryUsageMB) {
            config.setMaxMemoryUsageMB(maxMemoryUsageMB);
            return this;
        }
        
        public Builder enableBpmnGeneration(boolean enable) {
            config.addCustomOption("enableBpmnGeneration", enable);
            return this;
        }
        
        public Builder enableFormExtraction(boolean enable) {
            config.addCustomOption("enableFormExtraction", enable);
            return this;
        }
        
        public Builder enableServiceExtraction(boolean enable) {
            config.addCustomOption("enableServiceExtraction", enable);
            return this;
        }
        
        public Builder enableAwdExtensions(boolean enable) {
            config.setIncludeAwdExtensions(enable);
            return this;
        }
        
        public Builder enableDationCompatibility(boolean enable) {
            config.addCustomOption("enableDationCompatibility", enable);
            return this;
        }
        
        public Builder enableParallelProcessing(boolean enable) {
            config.setEnableParallelProcessing(enable);
            return this;
        }
        
        public Builder validateOutput(boolean validate) {
            config.setValidateOutput(validate);
            return this;
        }
        
        public Builder prettyPrintXml(boolean prettyPrint) {
            config.setPrettyPrintXml(prettyPrint);
            return this;
        }
        
        public Builder customOption(String key, Object value) {
            config.addCustomOption(key, value);
            return this;
        }
        
        public ExtractionConfig build() {
            return config.copy();
        }
    }

    /**
     * Create a copy of this configuration.
     */
    public ExtractionConfig copy() {
        ExtractionConfig copy = new ExtractionConfig();
        copy.maxConcurrentExtractions = this.maxConcurrentExtractions;
        copy.extractionTimeoutMinutes = this.extractionTimeoutMinutes;
        copy.maxMemoryUsageMB = this.maxMemoryUsageMB;
        copy.maxFilesPerType = this.maxFilesPerType;
        copy.enableParallelProcessing = this.enableParallelProcessing;
        copy.generateMetadata = this.generateMetadata;
        copy.validateOutput = this.validateOutput;
        copy.compressOutput = this.compressOutput;
        copy.fileNamingPattern = this.fileNamingPattern;
        copy.timestampFormat = this.timestampFormat;
        copy.createTypeDirectories = this.createTypeDirectories;
        copy.preserveSourceStructure = this.preserveSourceStructure;
        copy.includeAwdExtensions = this.includeAwdExtensions;
        copy.validateBpmnSchema = this.validateBpmnSchema;
        copy.awdNamespace = this.awdNamespace;
        copy.prettyPrintXml = this.prettyPrintXml;
        copy.generateCss = this.generateCss;
        copy.includeJavaScript = this.includeJavaScript;
        copy.formTheme = this.formTheme;
        copy.responsiveDesign = this.responsiveDesign;
        copy.includeServiceDependencies = this.includeServiceDependencies;
        copy.generateSwaggerDocs = this.generateSwaggerDocs;
        copy.serviceConfigFormat = this.serviceConfigFormat;
        copy.customOptions = new HashMap<>(this.customOptions);
        return copy;
    }
    
    @Override
    public String toString() {
        return "ExtractionConfig{" +
                "maxConcurrentExtractions=" + maxConcurrentExtractions +
                ", extractionTimeoutMinutes=" + extractionTimeoutMinutes +
                ", maxMemoryUsageMB=" + maxMemoryUsageMB +
                ", enableParallelProcessing=" + enableParallelProcessing +
                ", includeAwdExtensions=" + includeAwdExtensions +
                ", validateOutput=" + validateOutput +
                '}';
    }
}