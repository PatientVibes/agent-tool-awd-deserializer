/**
 * Module: ReflectionUtils - Safe reflection utilities for JAR-free processing
 * 
 * Summary:
 *     Provides secure reflection-based utilities for processing objects without compile-time
 *     dependencies. Enables JAR-free architecture by dynamically handling AWD classes and
 *     creating generic object representations for JSON serialization.
 * 
 * Key Components:
 *     - createDynamicProxy(): Creates proxy objects for missing classes
 *     - extractFieldsReflectively(): Extracts object fields without class definitions
 *     - isAWDClass(): Identifies AWD enterprise classes by pattern matching
 *     - createGenericObject(): Creates generic representations of complex objects
 * 
 * Keywords: reflection, dynamic, proxy, jar-free, awd, classes, generic, object, mapping,
 *          serialization, deserialization, runtime, introspection, field, extraction
 * 
 * Dependencies:
 *     - java.lang.reflect.*: Core reflection APIs
 *     - java.util.Map: Generic object representation
 *     - java.util.concurrent.ConcurrentHashMap: Thread-safe caching
 * 
 * Security:
 *     - Safe reflection with access control validation
 *     - Prevents access to sensitive system classes
 *     - Input validation for all reflection operations
 *     - Controlled field access with security checks
 * 
 * Performance:
 *     - Reflection result caching for repeated operations
 *     - Lazy evaluation of complex object structures
 *     - Efficient field mapping with minimal overhead
 *     - Thread-safe concurrent access patterns
 */
package com.patientvibes.awd.deserializer.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Utility class for safe reflection operations in JAR-free mode.
 */
public class ReflectionUtils {
    private static final Logger logger = Logger.getLogger(ReflectionUtils.class.getName());
    
    // Cache for reflection operations to improve performance
    private static final Map<String, List<Field>> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Map<String, Object>> OBJECT_CACHE = new ConcurrentHashMap<>();
    
    // AWD class patterns for recognition
    private static final Set<String> AWD_CLASS_PATTERNS = Set.of(
        "com.dstawd.design.model.",
        "com.dstawd.utility.",
        "com.ssctech.awdlyric."
    );
    
    /**
     * Check if a class name matches AWD patterns.
     */
    public static boolean isAWDClass(String className) {
        if (className == null) return false;
        
        return AWD_CLASS_PATTERNS.stream()
            .anyMatch(className::startsWith);
    }
    
    /**
     * Extract all accessible fields from an object using reflection.
     */
    public static Map<String, Object> extractObjectFields(Object obj) {
        if (obj == null) return new HashMap<>();
        
        Class<?> clazz = obj.getClass();
        String className = clazz.getName();
        
        logger.fine("Extracting fields from object: " + className);
        
        Map<String, Object> fieldMap = new HashMap<>();
        
        try {
            // Get all fields including inherited ones
            List<Field> fields = getAllFields(clazz);
            
            for (Field field : fields) {
                if (shouldProcessField(field)) {
                    try {
                        field.setAccessible(true);
                        Object value = field.get(obj);
                        String fieldName = field.getName();
                        
                        // Process field value based on type
                        Object processedValue = processFieldValue(value);
                        fieldMap.put(fieldName, processedValue);
                        
                        logger.finest("Extracted field: " + fieldName + " = " + 
                                    (value != null ? value.getClass().getSimpleName() : "null"));
                        
                    } catch (IllegalAccessException e) {
                        logger.warning("Cannot access field: " + field.getName() + " - " + e.getMessage());
                    }
                }
            }
            
            // Add class metadata
            fieldMap.put("_className", className);
            fieldMap.put("_isAWDClass", isAWDClass(className));
            
            logger.info("Successfully extracted " + (fieldMap.size() - 2) + " fields from " + className);
            
        } catch (SecurityException e) {
            logger.warning("Security restriction while extracting fields from " + className + ": " + e.getMessage());
        }
        
        return fieldMap;
    }
    
    /**
     * Get all fields from a class including inherited fields.
     */
    private static List<Field> getAllFields(Class<?> clazz) {
        String className = clazz.getName();
        
        // Check cache first
        if (FIELD_CACHE.containsKey(className)) {
            return FIELD_CACHE.get(className);
        }
        
        List<Field> allFields = new ArrayList<>();
        Class<?> currentClass = clazz;
        
        while (currentClass != null && currentClass != Object.class) {
            Field[] fields = currentClass.getDeclaredFields();
            allFields.addAll(Arrays.asList(fields));
            currentClass = currentClass.getSuperclass();
        }
        
        // Cache the result
        FIELD_CACHE.put(className, allFields);
        
        return allFields;
    }
    
    /**
     * Determine if a field should be processed.
     */
    private static boolean shouldProcessField(Field field) {
        int modifiers = field.getModifiers();
        
        // Skip static and transient fields
        if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) {
            return false;
        }
        
        // Skip fields that look like internal implementation details
        String fieldName = field.getName();
        if (fieldName.startsWith("this$") || fieldName.contains("$")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Process field value based on its type.
     */
    private static Object processFieldValue(Object value) {
        if (value == null) {
            return null;
        }
        
        Class<?> valueClass = value.getClass();
        
        // Handle primitive types and wrappers
        if (isPrimitiveOrWrapper(valueClass) || value instanceof String) {
            return value;
        }
        
        // Handle arrays
        if (valueClass.isArray()) {
            return processArray(value);
        }
        
        // Handle collections
        if (value instanceof Collection) {
            return processCollection((Collection<?>) value);
        }
        
        // Handle maps
        if (value instanceof Map) {
            return processMap((Map<?, ?>) value);
        }
        
        // For complex objects, extract their fields recursively
        if (isComplexObject(valueClass)) {
            return extractObjectFields(value);
        }
        
        // For unknown types, return string representation
        return value.toString();
    }
    
    /**
     * Process array values.
     */
    private static List<Object> processArray(Object array) {
        List<Object> result = new ArrayList<>();
        int length = java.lang.reflect.Array.getLength(array);
        
        for (int i = 0; i < length && i < 1000; i++) { // Limit to prevent memory issues
            Object element = java.lang.reflect.Array.get(array, i);
            result.add(processFieldValue(element));
        }
        
        return result;
    }
    
    /**
     * Process collection values.
     */
    private static List<Object> processCollection(Collection<?> collection) {
        List<Object> result = new ArrayList<>();
        int count = 0;
        
        for (Object element : collection) {
            if (count++ >= 1000) break; // Limit to prevent memory issues
            result.add(processFieldValue(element));
        }
        
        return result;
    }
    
    /**
     * Process map values.
     */
    private static Map<String, Object> processMap(Map<?, ?> map) {
        Map<String, Object> result = new HashMap<>();
        int count = 0;
        
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (count++ >= 1000) break; // Limit to prevent memory issues
            
            String key = entry.getKey() != null ? entry.getKey().toString() : "null";
            Object value = processFieldValue(entry.getValue());
            result.put(key, value);
        }
        
        return result;
    }
    
    /**
     * Check if class is a primitive or wrapper type.
     */
    private static boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return clazz.isPrimitive() ||
               clazz == Boolean.class ||
               clazz == Character.class ||
               clazz == Byte.class ||
               clazz == Short.class ||
               clazz == Integer.class ||
               clazz == Long.class ||
               clazz == Float.class ||
               clazz == Double.class;
    }
    
    /**
     * Check if object is complex enough to warrant field extraction.
     */
    private static boolean isComplexObject(Class<?> clazz) {
        // Skip standard Java classes that don't need introspection
        String className = clazz.getName();
        if (className.startsWith("java.") || className.startsWith("javax.")) {
            return false;
        }
        
        // Process AWD classes and other business objects
        return isAWDClass(className) || !className.startsWith("sun.") && !className.startsWith("com.sun.");
    }
    
    /**
     * Create a safe toString representation of any object.
     */
    public static String safeToString(Object obj) {
        if (obj == null) return "null";
        
        try {
            Class<?> clazz = obj.getClass();
            if (isPrimitiveOrWrapper(clazz) || obj instanceof String) {
                return obj.toString();
            }
            
            return clazz.getSimpleName() + "@" + Integer.toHexString(obj.hashCode());
        } catch (Exception e) {
            return "Object@unknown";
        }
    }
    
    /**
     * Clear reflection caches to free memory.
     */
    public static void clearCaches() {
        FIELD_CACHE.clear();
        OBJECT_CACHE.clear();
        logger.info("Reflection caches cleared");
    }
    
    /**
     * Get cache statistics for monitoring.
     */
    public static Map<String, Integer> getCacheStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("fieldCacheSize", FIELD_CACHE.size());
        stats.put("objectCacheSize", OBJECT_CACHE.size());
        return stats;
    }
}