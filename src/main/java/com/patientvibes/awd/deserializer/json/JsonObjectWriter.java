/**
 * Module: JsonObjectWriter - Object to JSON conversion engine
 * 
 * Summary:
 *     Core JSON writing engine that handles recursive object traversal and conversion.
 *     Manages memory usage, cycle detection, and field filtering during serialization.
 * 
 * Key Components:
 *     - writeObject(): Main object conversion entry point
 *     - writeField(): Individual field processing
 *     - handleCollection(): Collection serialization
 *     - checkCycles(): Circular reference detection
 *     - manageMemory(): Memory usage monitoring
 * 
 * Keywords: json, writer, object, conversion, recursive, traversal, serialization,
 *          memory, management, cycle, detection, field, filter, chorus, jackson,
 *          generator, reflection, graph, traversal
 * 
 * Dependencies:
 *     - MemoryManager: Memory usage control
 *     - JsonConversionContext: Processing context
 *     - Jackson JsonGenerator: JSON output generation
 *     - Java Reflection API: Object introspection
 * 
 * Security:
 *     - Circular reference prevention
 *     - Memory usage limits
 *     - Field access validation
 *     - Depth limit enforcement
 * 
 * Performance:
 *     - Streaming JSON generation
 *     - Memory-aware processing
 *     - Efficient reflection caching
 *     - Optimized collection handling
 */
package com.patientvibes.awd.deserializer.json;

import com.patientvibes.awd.deserializer.memory.MemoryManager;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Logger;

/**
 * Handles the actual writing of objects to JSON format.
 * Implements the recursive traversal and conversion logic.
 */
public class JsonObjectWriter {
    private static final Logger logger = Logger.getLogger(JsonObjectWriter.class.getName());
    
    private final JsonConversionContext context;
    private final MemoryManager memoryManager;
    
    public JsonObjectWriter(JsonConversionContext context) {
        this.context = context;
        this.memoryManager = new MemoryManager(context.getConfig());
    }
    
    /**
     * Write an object to JSON.
     * 
     * @param obj Object to write
     * @param fieldName Field name (if object is a field value)
     * @param depth Current recursion depth
     * @throws IOException If writing fails
     */
    public void writeObject(Object obj, String fieldName, int depth) throws IOException {
        // Check memory periodically
        memoryManager.checkMemory();
        
        JsonGenerator generator = context.getGenerator();
        
        // Handle null values
        if (obj == null) {
            generator.writeStringField("_null", "true");
            return;
        }
        
        // Check recursion depth
        if (depth > context.getConfig().getMaxReflectionDepth()) {
            generator.writeStringField("_error", "MAX_DEPTH_EXCEEDED");
            generator.writeStringField("_class", obj.getClass().getName());
            return;
        }
        
        // Check for cycles
        int identityHash = System.identityHashCode(obj);
        if (context.getVisited().contains(identityHash)) {
            generator.writeStringField("_cyclicRef", "true");
            generator.writeStringField("_class", obj.getClass().getName());
            return;
        }
        
        // Check if we should process this object
        if (context.getConfig().isFocusedMode() && 
            !context.getFieldFilter().shouldProcessObject(obj, fieldName)) {
            generator.writeStringField("_skipped", "FILTERED");
            return;
        }
        
        context.getVisited().add(identityHash);
        
        try {
            // Write object based on its type
            Class<?> clazz = obj.getClass();
            generator.writeStringField("_class", clazz.getName());
            
            if (context.getAnalyzer().isPrimitiveOrWrapper(clazz) || obj instanceof String) {
                generator.writeStringField("_value", obj.toString());
            } else if (clazz.isArray()) {
                writeArray(obj, depth);
            } else if (obj instanceof Iterable) {
                writeCollection((Iterable<?>) obj, depth);
            } else if (obj instanceof Map) {
                writeMap((Map<?, ?>) obj, depth);
            } else {
                writeComplexObject(obj, depth);
            }
            
        } finally {
            context.getVisited().remove(identityHash);
        }
    }
    
    /**
     * Write an array to JSON.
     */
    private void writeArray(Object array, int depth) throws IOException {
        JsonGenerator generator = context.getGenerator();
        generator.writeFieldName("_arrayElements");
        generator.writeStartArray();
        
        int length = java.lang.reflect.Array.getLength(array);
        int maxSize = 1000;
        int actualLength = Math.min(length, maxSize);
        
        for (int i = 0; i < actualLength; i++) {
            Object element = java.lang.reflect.Array.get(array, i);
            
            if (context.getConfig().isFocusedMode() && 
                !context.getFieldFilter().shouldProcessObject(element, null)) {
                continue;
            }
            
            generator.writeStartObject();
            writeObject(element, null, depth + 1);
            generator.writeEndObject();
        }
        
        if (length > maxSize) {
            generator.writeStartObject();
            generator.writeStringField("_truncated", "true");
            generator.writeNumberField("_originalLength", length);
            generator.writeEndObject();
        }
        
        generator.writeEndArray();
    }
    
    /**
     * Write a collection to JSON.
     */
    private void writeCollection(Iterable<?> collection, int depth) throws IOException {
        JsonGenerator generator = context.getGenerator();
        generator.writeFieldName("_collectionElements");
        generator.writeStartArray();
        
        int count = 0;
        int maxSize = 1000;
        
        for (Object element : collection) {
            if (count >= maxSize) {
                generator.writeStartObject();
                generator.writeStringField("_truncated", "true");
                generator.writeEndObject();
                break;
            }
            
            if (context.getConfig().isFocusedMode() && 
                !context.getFieldFilter().shouldProcessObject(element, null)) {
                count++;
                continue;
            }
            
            generator.writeStartObject();
            writeObject(element, null, depth + 1);
            generator.writeEndObject();
            count++;
        }
        
        generator.writeEndArray();
    }
    
    /**
     * Write a map to JSON.
     */
    private void writeMap(Map<?, ?> map, int depth) throws IOException {
        JsonGenerator generator = context.getGenerator();
        generator.writeFieldName("_mapEntries");
        generator.writeStartObject();
        
        int count = 0;
        int maxSize = 500;
        
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (count >= maxSize) {
                generator.writeStringField("_truncated", "true");
                break;
            }
            
            Object key = entry.getKey();
            Object value = entry.getValue();
            String keyStr = (key == null) ? "null" : key.toString();
            
            if (context.getConfig().isFocusedMode() && 
                !context.getFieldFilter().shouldProcessObject(value, keyStr)) {
                count++;
                continue;
            }
            
            keyStr = sanitizeJsonKey(keyStr);
            generator.writeFieldName(keyStr);
            generator.writeStartObject();
            writeObject(value, keyStr, depth + 1);
            generator.writeEndObject();
            count++;
        }
        
        generator.writeEndObject();
    }
    
    /**
     * Write a complex object by reflecting its fields.
     */
    private void writeComplexObject(Object obj, int depth) throws IOException {
        Class<?> clazz = obj.getClass();
        
        // Skip problematic classes
        if (context.getFieldFilter().shouldSkipClass(clazz)) {
            context.getGenerator().writeStringField("_skipped", "CLASS_SKIPPED");
            return;
        }
        
        // Process all fields in the class hierarchy
        while (clazz != null && clazz != Object.class) {
            Field[] fields = clazz.getDeclaredFields();
            
            for (Field field : fields) {
                if (shouldSkipField(field)) {
                    continue;
                }
                
                try {
                    field.setAccessible(true);
                    Object fieldValue = field.get(obj);
                    
                    if (context.getConfig().isFocusedMode() && 
                        !context.getFieldFilter().shouldProcessObject(fieldValue, field.getName())) {
                        continue;
                    }
                    
                    context.getGenerator().writeFieldName(field.getName());
                    context.getGenerator().writeStartObject();
                    writeObject(fieldValue, field.getName(), depth + 1);
                    context.getGenerator().writeEndObject();
                    
                } catch (IllegalAccessException e) {
                    context.getGenerator().writeStringField(field.getName(), "[ACCESS_ERROR]");
                } catch (Exception e) {
                    context.getGenerator().writeStringField(field.getName(), 
                        "[ERROR: " + e.getClass().getSimpleName() + "]");
                }
            }
            
            clazz = clazz.getSuperclass();
        }
    }
    
    /**
     * Check if a field should be skipped.
     */
    private boolean shouldSkipField(Field field) {
        // Skip synthetic fields
        if (field.isSynthetic()) {
            return true;
        }
        
        // Skip static fields
        if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
            return true;
        }
        
        // Skip sensitive fields
        if (context.getFieldFilter().isSensitiveField(field.getName())) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Sanitize a string to be used as a JSON key.
     */
    private String sanitizeJsonKey(String key) {
        if (key == null || key.isEmpty()) {
            return "null";
        }
        
        // Limit length and remove control characters
        String sanitized = key.substring(0, Math.min(key.length(), 1000));
        sanitized = sanitized.replaceAll("[\u0000-\u001F\u007F-\u009F]", "");
        
        return sanitized.isEmpty() ? "empty_key" : sanitized;
    }
}