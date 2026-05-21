/**
 * Module: DeserializerConfig - Configuration management for Chorus Deserializer
 * 
 * Summary:
 *     Manages all configuration settings for the deserializer including memory limits,
 *     security settings, and processing options. Uses Builder pattern for flexibility.
 * 
 * Key Components:
 *     - Builder: Fluent API for configuration construction
 *     - Memory settings: Heap size, monitoring, GC triggers
 *     - Security settings: File size limits, class whitelists
 *     - Processing settings: Batch size, reflection depth
 *     - Field filtering: Sensitive data protection
 * 
 * Keywords: configuration, config, settings, builder, pattern, memory, security,
 *          validation, whitelist, blacklist, chorus, deserializer, parameters,
 *          options, limits, thresholds, management, control
 * 
 * Dependencies:
 *     - java.util: Collections for configuration storage
 *     - No external dependencies (self-contained)
 * 
 * Security:
 *     - Immutable configuration after construction
 *     - Secure defaults for all settings
 *     - Class whitelist/blacklist support
 *     - File size limit enforcement
 *     - Sensitive field filtering
 * 
 * Performance:
 *     - Lightweight configuration object
 *     - Fast lookup with HashSets
 *     - No runtime overhead
 */
package com.patientvibes.awd.deserializer.config;

import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;

/**
 * Configuration settings for the Chorus Deserializer.
 * Manages all configurable parameters and provides defaults.
 */
public class DeserializerConfig {
    
    // Memory management settings
    private final boolean enableMemoryMonitoring;
    private final long memoryCheckInterval;
    private final long memoryLimitMB;
    
    // Processing settings
    private final boolean useSafeMode;
    private final boolean focusedMode;
    private final int batchSize;
    private final int maxReflectionDepth;
    
    // Security validation settings
    private final long maxFileSizeMB;
    private final boolean strictValidation;
    private final Set<String> allowedClassPrefixes;
    private final Set<String> blockedClasses;
    
    // Field filtering
    private final Set<String> whitelistFields;
    private final Set<String> sensitiveFields;
    private final Set<String> problemClasses;
    private final Set<String> importantStructures;
    
    private DeserializerConfig(Builder builder) {
        this.enableMemoryMonitoring = builder.enableMemoryMonitoring;
        this.memoryCheckInterval = builder.memoryCheckInterval;
        this.memoryLimitMB = builder.memoryLimitMB;
        this.useSafeMode = builder.useSafeMode;
        this.focusedMode = builder.focusedMode;
        this.batchSize = builder.batchSize;
        this.maxReflectionDepth = builder.maxReflectionDepth;
        this.maxFileSizeMB = builder.maxFileSizeMB;
        this.strictValidation = builder.strictValidation;
        this.allowedClassPrefixes = new HashSet<>(builder.allowedClassPrefixes);
        this.blockedClasses = new HashSet<>(builder.blockedClasses);
        this.whitelistFields = new HashSet<>(builder.whitelistFields);
        this.sensitiveFields = new HashSet<>(builder.sensitiveFields);
        this.problemClasses = new HashSet<>(builder.problemClasses);
        this.importantStructures = new HashSet<>(builder.importantStructures);
    }
    
    /**
     * Create configuration from system properties.
     */
    public static DeserializerConfig fromSystemProperties() {
        Builder builder = new Builder();
        
        // Read system properties
        String focusedModeStr = System.getProperty("focused.mode", "true");
        builder.focusedMode(Boolean.parseBoolean(focusedModeStr));
        
        String safeModeStr = System.getProperty("safe.reflection", "true");
        builder.useSafeMode(Boolean.parseBoolean(safeModeStr));
        
        String memoryLimitStr = System.getProperty("memory.limit.mb");
        if (memoryLimitStr != null && !memoryLimitStr.isEmpty()) {
            try {
                builder.memoryLimitMB(Long.parseLong(memoryLimitStr));
            } catch (NumberFormatException e) {
                // Use default
            }
        }
        
        String batchSizeStr = System.getProperty("batch.size");
        if (batchSizeStr != null && !batchSizeStr.isEmpty()) {
            try {
                builder.batchSize(Integer.parseInt(batchSizeStr));
            } catch (NumberFormatException e) {
                // Use default
            }
        }
        
        // First try environment variable, then system property
        String maxFileSizeStr = System.getenv("MAX_FILE_SIZE_MB");
        if (maxFileSizeStr == null || maxFileSizeStr.isEmpty()) {
            maxFileSizeStr = System.getProperty("max.file.size.mb");
        }
        if (maxFileSizeStr != null && !maxFileSizeStr.isEmpty()) {
            try {
                builder.maxFileSizeMB(Long.parseLong(maxFileSizeStr));
            } catch (NumberFormatException e) {
                // Use default
            }
        }
        
        String strictValidationStr = System.getProperty("strict.validation", "true");
        builder.strictValidation(Boolean.parseBoolean(strictValidationStr));
        
        return builder.build();
    }
    
    // Getters
    public boolean isEnableMemoryMonitoring() { return enableMemoryMonitoring; }
    public boolean getEnableMemoryMonitoring() { return enableMemoryMonitoring; }
    public long getMemoryCheckInterval() { return memoryCheckInterval; }
    public long getMemoryLimitMB() { return memoryLimitMB; }
    public boolean isUseSafeMode() { return useSafeMode; }
    public boolean getUseSafeMode() { return useSafeMode; }
    public boolean isFocusedMode() { return focusedMode; }
    public boolean getFocusedMode() { return focusedMode; }
    public int getBatchSize() { return batchSize; }
    public int getMaxReflectionDepth() { return maxReflectionDepth; }
    public Set<String> getWhitelistFields() { return new HashSet<>(whitelistFields); }
    public Set<String> getSensitiveFields() { return new HashSet<>(sensitiveFields); }
    public Set<String> getProblemClasses() { return new HashSet<>(problemClasses); }
    public Set<String> getImportantStructures() { return new HashSet<>(importantStructures); }
    public long getMaxFileSizeMB() { return maxFileSizeMB; }
    public boolean isStrictValidation() { return strictValidation; }
    public Set<String> getAllowedClassPrefixes() { return new HashSet<>(allowedClassPrefixes); }
    public Set<String> getBlockedClasses() { return new HashSet<>(blockedClasses); }
    
    /**
     * Builder for DeserializerConfig.
     */
    public static class Builder {
        // Default values
        private boolean enableMemoryMonitoring = true;
        private long memoryCheckInterval = 1000;
        private long memoryLimitMB = calculateDefaultMemoryLimit();
        private boolean useSafeMode = true;
        private boolean focusedMode = true;
        private int batchSize = 200;
        private int maxReflectionDepth = 20;
        private long maxFileSizeMB = 1024; // 1GB default
        private boolean strictValidation = true;
        
        private Set<String> whitelistFields = new HashSet<>(Arrays.asList(
            // Data Dictionary Fields
            "PCNM", "PFNM", "PLNM", "PEML", "GUID", "CCNM", "CFNM", "CLNM", "CEML",
            "ENME", "ED01", "ED02", "EDON", "EDWS", "CERT", "EIMP", "EOVR", "EURL", 
            "FLNM", "ECST", "NDTY", "CNTY", "EBSC", "ETPT", "SCRE", "CS01", "CS02", 
            "CS03", "CS04", "CS05", "CS06", "CS07", "CS08", "CS09", "CS10", "CSTP",
            "CSPO", "CID1", "CID2", "CIPN", "CITL", "CITP", "CRPN", "CRTL", "CRTP",
            "CRD1", "CRD2", "CSPN", "CSTL", "CST1", "CST2", "CST3", "CST4", "CST5",
            "CSWC", "CTTL", "AMTT", "AMTV", "CASE", "CSD1", "CSD2", "EGOT", "ENDQ",
            "ETYP", "EXTN", "INCR", "KEY0", "KEY1", "KEY2", "KEY3", "KEY4", "KEY5",
            "KEY6", "KEY7", "KEY8", "KEY9", "LOBF", "METL", "MS01", "MS02", "MS03",
            "MS04", "MS05", "MS06", "MS07", "MS08", "NPBP", "NPPR", "PPHN", "PRGN",
            "PRTY", "PTTL", "REGN", "REVN", "ROUT", "RTEF", "TF01", "TF02", "TF03",
            "VIFL", "comments", "name", "id", "type", "template", "formType", "version",
            
            // Queues
            "INDEX", "QUALITY2", "END", "PROCESS", "PEND",
            
            // Statuses
            "CREATED", "PROCESSED", "REMINDER", "FAILED", "PASSED", 
            "QUALITY", "EXPIRED", "RIPPED",
            
            // Forms
            "ENGAGEND", "ENGAEMNT", "FLDXMPL", "SRCXMPL",
            
            // Business Areas
            "PSN",
            
            // Work Types
            "CONTACT",
            
            // Other important fields
            "screenName", "screenType", "templateScreen", "formType", "langID", 
            "screenFormat", "screenDesc", "definitionVersion", "source", "document",
            "userScreen", "screenDefinition", "uxType", "uxData", "inputs", "outputs"
        ));
        
        private Set<String> sensitiveFields = new HashSet<>(Arrays.asList(
            "password", "secret", "key", "token", "credential", "auth", "privateKey"
        ));
        
        private Set<String> problemClasses = new HashSet<>(Arrays.asList(
            "jdk.internal.module.ModuleReferenceImpl",
            "jdk.internal.module.SystemModuleDescriptors",
            "jdk.internal.module.ModuleLoaderMap"
        ));
        
        private Set<String> importantStructures = new HashSet<>(Arrays.asList(
            "java.util.HashMap", 
            "java.util.ArrayList",
            "java.util.LinkedHashMap"
        ));
        
        private Set<String> allowedClassPrefixes = new HashSet<>(Arrays.asList(
            "com.dstawd.",           // AWD classes
            "com.chorus.",           // Chorus classes
            "java.lang.",            // Core Java
            "java.util.",            // Collections
            "java.math.",            // BigDecimal, etc.
            "java.time.",            // Date/Time API
            "java.sql.",             // SQL types
            "javax.xml.",            // XML processing
            "org.w3c.dom.",          // DOM
            "org.xml.sax."           // SAX
        ));
        
        private Set<String> blockedClasses = new HashSet<>(Arrays.asList(
            "java.lang.Runtime",
            "java.lang.ProcessBuilder",
            "java.lang.reflect.Proxy",
            "java.rmi.",
            "javax.script.",
            "javax.management.",
            "java.net.URLClassLoader",
            "java.beans.Expression",
            "java.beans.Statement",
            "com.sun.",
            "sun."
        ));
        
        private static long calculateDefaultMemoryLimit() {
            return (long)(Runtime.getRuntime().maxMemory() * 0.8 / (1024 * 1024));
        }
        
        public Builder enableMemoryMonitoring(boolean enable) {
            this.enableMemoryMonitoring = enable;
            return this;
        }
        
        public Builder memoryCheckInterval(long interval) {
            this.memoryCheckInterval = interval;
            return this;
        }
        
        public Builder memoryLimitMB(long limit) {
            this.memoryLimitMB = limit;
            return this;
        }
        
        public Builder useSafeMode(boolean safe) {
            this.useSafeMode = safe;
            return this;
        }
        
        public Builder focusedMode(boolean focused) {
            this.focusedMode = focused;
            return this;
        }
        
        public Builder batchSize(int size) {
            this.batchSize = size;
            return this;
        }
        
        public Builder maxReflectionDepth(int depth) {
            this.maxReflectionDepth = depth;
            return this;
        }
        
        public Builder addWhitelistField(String field) {
            this.whitelistFields.add(field);
            return this;
        }
        
        public Builder addSensitiveField(String field) {
            this.sensitiveFields.add(field);
            return this;
        }
        
        public Builder addProblemClass(String className) {
            this.problemClasses.add(className);
            return this;
        }
        
        public Builder maxFileSizeMB(long maxSize) {
            this.maxFileSizeMB = maxSize;
            return this;
        }
        
        public Builder strictValidation(boolean strict) {
            this.strictValidation = strict;
            return this;
        }
        
        public Builder addAllowedClassPrefix(String prefix) {
            this.allowedClassPrefixes.add(prefix);
            return this;
        }
        
        public Builder addBlockedClass(String className) {
            this.blockedClasses.add(className);
            return this;
        }
        
        public DeserializerConfig build() {
            return new DeserializerConfig(this);
        }
    }
}