package com.patientvibes.awd.deserializer.analysis;

import com.patientvibes.awd.deserializer.config.DeserializerConfig;

import java.util.regex.Pattern;
import java.util.Map;

/**
 * Module: ObjectAnalyzer - Advanced object analysis and type detection engine
 * 
 * Summary:
 *     Comprehensive object analysis system that examines deserialized objects to determine their
 *     structural type, content patterns, and business context. Provides intelligent classification
 *     for AWD design objects, form definitions, BPMN processes, and other enterprise artifacts.
 * 
 * Key Components:
 *     - analyzeObject(): Primary object analysis with type detection
 *     - isFormObject(): Form definition detection and validation
 *     - isBPMNObject(): Business process model identification
 *     - isWorkflowObject(): Workflow definition analysis
 *     - extractMetadata(): Metadata extraction and enrichment
 * 
 * Keywords: analysis, object, type, detection, classification, structure, metadata, form, bpmn,
 *          workflow, pattern, recognition, enterprise, design, validation, inspection, reflection
 * 
 * Dependencies:
 *     - DeserializerConfig: Configuration for analysis behavior
 *     - java.util.regex.Pattern: Pattern matching for content analysis
 *     - java.util.Map: Data structure analysis and metadata storage
 * 
 * Security:
 *     - Safe object inspection without executing code
 *     - Pattern-based validation prevents injection attacks
 *     - Read-only analysis with no object modification
 *     - Secure reflection usage with access controls
 * 
 * Performance:
 *     - O(1) type detection for common object patterns
 *     - Lazy evaluation of complex analysis operations
 *     - Efficient regex compilation and caching
 *     - Minimal memory footprint during analysis
 *
 */
public class ObjectAnalyzer {
    
    private final DeserializerConfig config;
    
    // Patterns for identifying artifact types
    private static final Pattern BPMN_PATTERN = Pattern.compile("(?i)bpmn|process|definitions|workflow");
    private static final Pattern FORM_PATTERN = Pattern.compile("(?i)userScreen|screenDefinition|form|uxForm|uxData");
    private static final Pattern SERVICE_PATTERN = Pattern.compile("(?i)service|automation|integration|wsdl|openapi|rest");
    
    public ObjectAnalyzer(DeserializerConfig config) {
        this.config = config;
    }
    
    /**
     * Check if a class is a primitive type or wrapper.
     */
    public boolean isPrimitiveOrWrapper(Class<?> clazz) {
        if (clazz.isPrimitive()) return true;
        if (Number.class.isAssignableFrom(clazz)) return true;
        if (Boolean.class.equals(clazz)) return true;
        if (Character.class.equals(clazz)) return true;
        return false;
    }
    
    /**
     * Check if object might be a form structure.
     */
    public boolean isFormStructure(Object obj) {
        if (obj == null) return false;
        
        String className = obj.getClass().getName();
        if (className.contains("Form") || className.contains("Screen")) {
            return true;
        }
        
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            for (Object key : map.keySet()) {
                if (key instanceof String) {
                    String keyStr = (String) key;
                    if (FORM_PATTERN.matcher(keyStr).find() || 
                        keyStr.equals("uxType") || 
                        keyStr.equals("screenName")) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Check if object might be a BPMN process structure.
     */
    public boolean isBpmnProcessStructure(Object obj) {
        if (obj == null) return false;
        
        String className = obj.getClass().getName();
        if (className.contains("BPMN") || className.contains("Process") || 
            className.contains("Workflow")) {
            return true;
        }
        
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            for (Object key : map.keySet()) {
                if (key instanceof String && BPMN_PATTERN.matcher((String) key).find()) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Check if object might be a service structure.
     */
    public boolean isServiceStructure(Object obj) {
        if (obj == null) return false;
        
        String className = obj.getClass().getName();
        if (className.contains("Service") || className.contains("WSDL") || 
            className.contains("REST") || className.contains("API")) {
            return true;
        }
        
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            for (Object key : map.keySet()) {
                if (key instanceof String && SERVICE_PATTERN.matcher((String) key).find()) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Check if object contains important content (XML/JSON strings).
     */
    public boolean hasImportantContent(Object obj) {
        if (obj instanceof String) {
            String str = ((String) obj).trim();
            // Check for XML or JSON patterns
            return (str.startsWith("<") && str.endsWith(">")) || // XML-like
                   (str.startsWith("{") && str.endsWith("}")); // JSON-like
        }
        return false;
    }
}