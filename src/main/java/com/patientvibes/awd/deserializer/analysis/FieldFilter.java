/**
 * Module: FieldFilter - Field and object filtering engine
 * 
 * Summary:
 *     Implements intelligent filtering of fields and objects based on configuration
 *     rules to control what data is included in the JSON output.
 * 
 * Key Components:
 *     - shouldProcessObject(): Object-level filtering
 *     - shouldIncludeField(): Field-level filtering
 *     - isWhitelisted(): Whitelist validation
 *     - isSensitive(): Sensitive data detection
 *     - analyzePriority(): Priority-based filtering
 * 
 * Keywords: filter, field, object, analysis, whitelist, blacklist, sensitive,
 *          data, protection, privacy, configuration, rules, processing,
 *          chorus, deserializer, inclusion, exclusion
 * 
 * Dependencies:
 *     - DeserializerConfig: Filtering configuration
 *     - ObjectAnalyzer: Object analysis utilities
 *     - java.util.Set: Collection support
 * 
 * Security:
 *     - Sensitive field filtering
 *     - Data privacy protection
 *     - Configurable access control
 *     - Whitelist/blacklist enforcement
 * 
 * Performance:
 *     - Efficient Set-based lookups
 *     - Early termination on matches
 *     - Minimal object inspection
 */
package com.patientvibes.awd.deserializer.analysis;

import com.patientvibes.awd.deserializer.config.DeserializerConfig;

import java.util.Set;

/**
 * Filters fields and objects based on configuration rules.
 * Determines what should be included in the JSON output.
 */
public class FieldFilter {
    
    private final DeserializerConfig config;
    private final ObjectAnalyzer analyzer;
    
    public FieldFilter(DeserializerConfig config) {
        this.config = config;
        this.analyzer = new ObjectAnalyzer(config);
    }
    
    /**
     * Determine if we should process this object based on filtering rules.
     * 
     * @param obj The object to check
     * @param fieldName The field name of this object (if available)
     * @return True if we should process this object
     */
    public boolean shouldProcessObject(Object obj, String fieldName) {
        if (!config.isFocusedMode()) {
            return true; // Process everything if focused mode is disabled
        }
        
        if (obj == null) {
            return true; // Always process nulls
        }
        
        // Always process primitives and strings
        if (analyzer.isPrimitiveOrWrapper(obj.getClass()) || obj instanceof String) {
            return true;
        }
        
        // Process if field name is in whitelist
        if (fieldName != null && config.getWhitelistFields().contains(fieldName)) {
            return true;
        }
        
        // Always process important structure types
        String className = obj.getClass().getName();
        if (config.getImportantStructures().contains(className)) {
            return true;
        }
        
        // Check if it's a form, BPMN process, or service
        if (analyzer.isFormStructure(obj) || 
            analyzer.isBpmnProcessStructure(obj) || 
            analyzer.isServiceStructure(obj)) {
            return true;
        }
        
        // Check if object has important content
        if (analyzer.hasImportantContent(obj)) {
            return true;
        }
        
        return false; // Skip by default in focused mode
    }
    
    /**
     * Check if a class should be skipped entirely.
     * 
     * @param clazz Class to check
     * @return True if class should be skipped
     */
    public boolean shouldSkipClass(Class<?> clazz) {
        if (clazz == null) return true;
        
        String className = clazz.getName();
        
        // Skip problematic classes
        if (config.getProblemClasses().contains(className)) {
            return true;
        }
        
        // Skip certain core Java classes that can cause issues
        return className.equals("java.lang.Thread") ||
               className.equals("java.lang.ThreadLocal") ||
               className.equals("java.lang.ClassLoader") ||
               className.equals("java.security.ProtectionDomain") ||
               className.equals("java.lang.SecurityManager") ||
               className.startsWith("com.sun.proxy.$Proxy");
    }
    
    /**
     * Check if a field contains sensitive information.
     * 
     * @param fieldName Field name to check
     * @return True if field is sensitive
     */
    public boolean isSensitiveField(String fieldName) {
        if (fieldName == null) return false;
        
        String lowerFieldName = fieldName.toLowerCase();
        return config.getSensitiveFields().stream()
            .anyMatch(lowerFieldName::contains);
    }
    
    /**
     * Check if a class is an internal JDK class that might have restrictions.
     * 
     * @param clazz Class to check
     * @return True if class is internal JDK class
     */
    public boolean isInternalJdkClass(Class<?> clazz) {
        if (clazz == null) return false;
        
        String packageName = clazz.getPackage() != null ? clazz.getPackage().getName() : "";
        
        return packageName.startsWith("jdk.internal.") ||
               packageName.startsWith("sun.") ||
               packageName.startsWith("com.sun.") ||
               packageName.startsWith("java.base/") ||
               (packageName.startsWith("java.sql.") && packageName.contains("internal")) ||
               packageName.contains("jdk.internal") ||
               packageName.startsWith("java.util.concurrent.locks") ||
               packageName.startsWith("java.lang.invoke");
    }
}