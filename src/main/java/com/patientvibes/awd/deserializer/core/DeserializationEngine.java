/**
 * Module: DeserializationEngine - Core engine for secure Java object deserialization
 * 
 * Summary:
 *     Provides secure deserialization of Java objects with class validation, memory
 *     management, and configurable security restrictions. Implements defense-in-depth
 *     against deserialization vulnerabilities.
 * 
 * Key Components:
 *     - deserialize(): Main deserialization method with security checks
 *     - createSafeObjectInputStream(): Security-enhanced ObjectInputStream
 *     - validateClass(): Class whitelist/blacklist validation
 *     - Memory monitoring and overflow prevention
 * 
 * Keywords: deserialization, security, validation, whitelist, blacklist, object, stream,
 *           java, serialization, memory, safe, restricted, class, loader, vulnerability
 * Dependencies: DeserializerConfig, MemoryManager, ObjectInputStream
 * Security: Class validation, blocked class detection, strict mode, memory limits
 * Performance: Memory monitoring, streaming deserialization, efficient validation
 */
package com.patientvibes.awd.deserializer.core;

import com.patientvibes.awd.deserializer.util.ByteUtils;

import com.patientvibes.awd.deserializer.config.DeserializerConfig;
import com.patientvibes.awd.deserializer.reflection.ReflectionUtils;
import com.patientvibes.awd.deserializer.memory.MemoryManager;
import com.patientvibes.awd.deserializer.reflection.ReflectionDeserializer;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectStreamClass;
import java.io.ObjectInputFilter;
import java.io.InvalidClassException;
import java.util.logging.Logger;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

/**
 * Core deserialization engine that handles Java object deserialization.
 */
public class DeserializationEngine {
    private static final Logger logger = Logger.getLogger(DeserializationEngine.class.getName());
    
    private final DeserializerConfig config;
    private final MemoryManager memoryManager;
    private final ObjectInputFilter securityFilter;
    private final ReflectionDeserializer reflectionDeserializer;
    
    // AWD Enterprise classes whitelist
    private static final Set<String> AWD_ALLOWED_CLASSES = Set.of(
        "com.dstawd.design.model.DeploymentPackage",
        "com.dstawd.design.model.Deployable",
        "com.dstawd.design.model.CustomDataType",
        "com.dstawd.utility.datasource.DataSourceHelper",
        "com.ssctech.awdlyric.schemas.WorkObject",
        "java.util.ArrayList",
        "java.util.LinkedList",
        "java.util.HashMap",
        "java.util.Map",
        "java.util.List",
        "java.lang.String",
        "java.lang.Integer",
        "java.lang.Long",
        "java.lang.Double",
        "java.lang.Boolean",
        "java.math.BigDecimal",
        "java.time.LocalDateTime",
        "java.time.ZonedDateTime"
    );
    
    public DeserializationEngine(DeserializerConfig config, MemoryManager memoryManager) {
        this.config = config;
        this.memoryManager = memoryManager;
        this.securityFilter = createSecurityFilter();
        this.reflectionDeserializer = new ReflectionDeserializer();
        
        // Configure global security filter if Java 17+
        try {
            ObjectInputFilter.Config.setSerialFilter(this.securityFilter);
            logger.info("ObjectInputFilter configured for enhanced security");
        } catch (Exception e) {
            logger.warning("Failed to set global ObjectInputFilter: " + e.getMessage());
        }
    }
    
    /**
     * Deserialize Java object from byte array.
     * 
     * @param data Serialized object data
     * @return Deserialized object
     * @throws IOException If deserialization fails
     * @throws ClassNotFoundException If class not found
     */
    public Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        logger.info("Starting Java object deserialization for " + ByteUtils.formatBytes(data.length) + " of data");
        
        // Check memory before deserialization
        memoryManager.checkMemory();
        memoryManager.logMemoryInfo();
        
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             ObjectInputStream ois = createSafeObjectInputStream(bais)) {
            
            long startTime = System.currentTimeMillis();
            Object obj = ois.readObject();
            long deserializationTime = System.currentTimeMillis() - startTime;
            
            if (obj == null) {
                throw new IOException("Deserialization returned null object");
            }
            
            // Log success with timing info
            logger.info(String.format("Successfully deserialized object of type: %s in %d ms", 
                obj.getClass().getName(), deserializationTime));
            
            // Final memory check
            memoryManager.checkMemory();
            
            return obj;
            
        } catch (OutOfMemoryError e) {
            memoryManager.logMemoryInfo();
            logger.severe("Out of memory during deserialization. Current usage: " + 
                         memoryManager.getUsedMemoryMB() + "MB");
            throw new IOException("Out of memory during deserialization", e);
        } catch (ClassNotFoundException e) {
            logger.info("Class not found during standard deserialization, attempting reflection fallback: " + e.getMessage());
            return deserializeWithReflectionFallback(data, e);
        } catch (IOException e) {
            logger.severe("Deserialization failed: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Attempt deserialization with reflection fallback for missing classes.
     */
    private Object deserializeWithReflectionFallback(byte[] data, ClassNotFoundException originalException) throws IOException {
        logger.info("🆕 JAR-Free Mode: Attempting reflection-based deserialization for missing AWD classes");
        
        try {
            long startTime = System.currentTimeMillis();
            Object result = reflectionDeserializer.deserializeWithReflection(data);
            long reflectionTime = System.currentTimeMillis() - startTime;
            
            logger.info("✅ JAR-Free Success: Reflection-based deserialization completed in " + reflectionTime + "ms");
            
            // Log missing classes for debugging
            Set<String> missingClasses = reflectionDeserializer.getMissingClasses();
            if (!missingClasses.isEmpty()) {
                logger.info("📋 Missing classes processed via reflection: " + missingClasses);
            }
            
            return result;
            
        } catch (Exception e) {
            logger.warning("Reflection fallback also failed: " + e.getMessage());
            
            // Final fallback - return a minimal representation
            return createMinimalFallbackObject(originalException, data.length);
        }
    }
    
    /**
     * Create minimal fallback object when all deserialization attempts fail.
     */
    private Object createMinimalFallbackObject(ClassNotFoundException originalException, int dataSize) {
        logger.info("Creating minimal fallback representation");
        
        Map<String, Object> fallbackObject = new HashMap<>();
        fallbackObject.put("_status", "jar_free_fallback");
        fallbackObject.put("_original_error", originalException.getMessage());
        fallbackObject.put("_data_size_bytes", dataSize);
        fallbackObject.put("_processing_mode", "reflection_fallback");
        fallbackObject.put("_timestamp", System.currentTimeMillis());
        
        return fallbackObject;
    }
    
    
    /**
     * Create ObjectInputFilter for security validation.
     */
    private ObjectInputFilter createSecurityFilter() {
        return filterInfo -> {
            Class<?> serialClass = filterInfo.serialClass();
            
            if (serialClass != null) {
                String className = serialClass.getName();
                
                // Always allow primitive types and arrays
                if (serialClass.isPrimitive() || serialClass.isArray()) {
                    return ObjectInputFilter.Status.ALLOWED;
                }
                
                // Check AWD whitelist
                if (ReflectionUtils.isAWDClass(className) || AWD_ALLOWED_CLASSES.contains(className)) {
                    logger.fine("AWD class allowed: " + className);
                    return ObjectInputFilter.Status.ALLOWED;
                }
                
                // Check configuration whitelist
                for (String allowedPrefix : config.getAllowedClassPrefixes()) {
                    if (className.startsWith(allowedPrefix)) {
                        logger.fine("Config whitelist allowed: " + className);
                        return ObjectInputFilter.Status.ALLOWED;
                    }
                }
                
                // Check blocked classes
                for (String blockedClass : config.getBlockedClasses()) {
                    if (className.equals(blockedClass) || className.startsWith(blockedClass)) {
                        logger.severe("Blocked class rejected: " + className);
                        return ObjectInputFilter.Status.REJECTED;
                    }
                }
                
                // Default behavior based on strict validation
                if (config.isStrictValidation()) {
                    logger.warning("Class not in whitelist (strict mode): " + className);
                    return ObjectInputFilter.Status.REJECTED;
                } else {
                    logger.info("Class allowed (permissive mode): " + className);
                    return ObjectInputFilter.Status.ALLOWED;
                }
            }
            
            // Check object depth and array size limits
            if (filterInfo.depth() > 50) {
                logger.severe("Object depth limit exceeded: " + filterInfo.depth());
                return ObjectInputFilter.Status.REJECTED;
            }
            
            if (filterInfo.arrayLength() > 10000) {
                logger.severe("Array size limit exceeded: " + filterInfo.arrayLength());
                return ObjectInputFilter.Status.REJECTED;
            }
            
            return ObjectInputFilter.Status.UNDECIDED;
        };
    }
    
    
    /**
     * Create a safe ObjectInputStream that handles security restrictions.
     * 
     * @param bais ByteArrayInputStream to read from
     * @return Safe ObjectInputStream
     * @throws IOException If stream creation fails
     */
    private ObjectInputStream createSafeObjectInputStream(ByteArrayInputStream bais) throws IOException {
        ObjectInputStream ois = new ObjectInputStream(bais) {
            @Override
            protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                String className = desc.getName();
                
                // Legacy validation for backwards compatibility
                if (config.isStrictValidation()) {
                    validateClass(className);
                }
                
                try {
                    return super.resolveClass(desc);
                } catch (SecurityException e) {
                    logger.warning("Security restriction for class: " + className);
                    throw new ClassNotFoundException("Security restriction: " + className, e);
                }
            }
            
            @Override
            protected Class<?> resolveProxyClass(String[] interfaces) throws IOException, ClassNotFoundException {
                // Validate proxy interfaces if strict validation is enabled
                if (config.isStrictValidation()) {
                    for (String interfaceName : interfaces) {
                        validateClass(interfaceName);
                    }
                }
                
                try {
                    return super.resolveProxyClass(interfaces);
                } catch (SecurityException e) {
                    logger.warning("Security restriction for proxy class with interfaces: " + String.join(", ", interfaces));
                    throw new ClassNotFoundException("Security restriction for proxy class", e);
                }
            }
            
            private void validateClass(String className) throws ClassNotFoundException {
                // Check if class is blocked
                for (String blockedClass : config.getBlockedClasses()) {
                    if (className.equals(blockedClass) || className.startsWith(blockedClass)) {
                        logger.severe("Blocked class detected: " + className);
                        throw new ClassNotFoundException("Blocked class: " + className);
                    }
                }
                
                // Check if class is allowed (skip validation for primitives and arrays)
                if (!className.startsWith("[") && !isPrimitiveType(className)) {
                    boolean allowed = false;
                    for (String prefix : config.getAllowedClassPrefixes()) {
                        if (className.startsWith(prefix)) {
                            allowed = true;
                            break;
                        }
                    }
                    
                    if (!allowed) {
                        logger.severe("Unauthorized class detected: " + className);
                        throw new ClassNotFoundException("Unauthorized class: " + className);
                    }
                }
            }
            
            private boolean isPrimitiveType(String className) {
                return className.equals("boolean") || className.equals("byte") ||
                       className.equals("char") || className.equals("double") ||
                       className.equals("float") || className.equals("int") ||
                       className.equals("long") || className.equals("short");
            }
        };
        
        // Set the security filter on this stream
        try {
            ois.setObjectInputFilter(this.securityFilter);
        } catch (Exception e) {
            logger.warning("Failed to set ObjectInputFilter on stream: " + e.getMessage());
        }
        
        return ois;
    }
}