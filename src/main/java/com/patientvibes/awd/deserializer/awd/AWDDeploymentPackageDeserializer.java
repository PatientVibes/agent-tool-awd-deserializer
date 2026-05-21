/**
 * Module: AWDDeploymentPackageDeserializer - Specialized deserializer for AWD enterprise .design files
 * 
 * Summary:
 *     Provides secure deserialization for AWD DeploymentPackage objects from GZIP-compressed
 *     Java serialization format. Implements AWD-specific validation and schema handling
 *     with comprehensive error recovery and logging.
 * 
 * Key Components:
 *     - deserializeDesignFile(): Main entry point for .design file processing
 *     - extractDeploymentPackage(): Safe extraction with validation
 *     - validateAWDStructure(): AWD-specific schema validation
 *     - processCustomDataTypes(): Handle custom AWD data types
 * 
 * Keywords: awd, deployment, package, design, file, deserialization, enterprise, gzip,
 *           compression, validation, schema, custom, datatypes, security, safe, extraction
 * Dependencies: DeserializationEngine, DesignFileValidator, ObjectInputFilter
 * Security: AWD class whitelist, input validation, safe deserialization, depth limits
 * Performance: GZIP decompression, streaming processing, memory efficient validation
 */
package com.patientvibes.awd.deserializer.awd;

import com.patientvibes.awd.deserializer.util.ByteUtils;

import com.patientvibes.awd.deserializer.core.DeserializationEngine;
import com.patientvibes.awd.deserializer.security.DesignFileValidator;
import com.patientvibes.awd.deserializer.memory.MemoryManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.logging.Logger;
import java.util.Map;
import java.util.List;
import java.util.HashMap;

/**
 * Specialized deserializer for AWD (Advanced Work Distribution) DeploymentPackage objects.
 * Handles the GZIP-compressed Java serialization format used by AWD enterprise systems.
 */
public class AWDDeploymentPackageDeserializer {
    private static final Logger logger = Logger.getLogger(AWDDeploymentPackageDeserializer.class.getName());
    
    private final DeserializationEngine deserializationEngine;
    private final DesignFileValidator validator;
    private final MemoryManager memoryManager;
    
    public AWDDeploymentPackageDeserializer(DeserializationEngine deserializationEngine, 
                                          DesignFileValidator validator,
                                          MemoryManager memoryManager) {
        this.deserializationEngine = deserializationEngine;
        this.validator = validator;
        this.memoryManager = memoryManager;
    }
    
    /**
     * Deserialize AWD .design file containing DeploymentPackage.
     * 
     * @param designFileData GZIP-compressed Java serialization data
     * @return AWDDeploymentPackage wrapper with extracted data
     * @throws IOException If deserialization or decompression fails
     * @throws ClassNotFoundException If AWD classes not found
     */
    public AWDDeploymentPackage deserializeDesignFile(byte[] designFileData) 
            throws IOException, ClassNotFoundException {
        logger.info("Starting AWD .design file deserialization for " + ByteUtils.formatBytes(designFileData.length));
        
        // Validate file format and security
        validator.validateDesignFile(designFileData);
        
        // Check memory before processing
        memoryManager.checkMemory();
        
        try {
            // Decompress GZIP data
            byte[] decompressedData = decompressGZIP(designFileData);
            logger.info("Decompressed .design file: " + ByteUtils.formatBytes(decompressedData.length));
            
            // Deserialize Java object
            Object rootObject = deserializationEngine.deserialize(decompressedData);
            
            // Extract and validate DeploymentPackage
            return extractDeploymentPackage(rootObject);
            
        } catch (Exception e) {
            logger.severe("AWD .design file deserialization failed: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Decompress GZIP-compressed data.
     */
    private byte[] decompressGZIP(byte[] compressedData) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
             GZIPInputStream gzis = new GZIPInputStream(bais)) {
            
            return gzis.readAllBytes();
        }
    }
    
    /**
     * Extract and validate DeploymentPackage from deserialized object.
     */
    private AWDDeploymentPackage extractDeploymentPackage(Object rootObject) 
            throws IOException, ClassNotFoundException {
        
        if (rootObject == null) {
            throw new IOException("Root object is null");
        }
        
        String className = rootObject.getClass().getName();
        logger.info("Processing root object of type: " + className);
        
        // Validate it's a DeploymentPackage
        if (!className.equals("com.dstawd.design.model.DeploymentPackage")) {
            throw new ClassNotFoundException("Expected DeploymentPackage, got: " + className);
        }
        
        try {
            // Use reflection to safely extract fields
            return extractDeploymentPackageFields(rootObject);
        } catch (Exception e) {
            logger.severe("Failed to extract DeploymentPackage fields: " + e.getMessage());
            throw new IOException("DeploymentPackage extraction failed", e);
        }
    }
    
    /**
     * Extract fields from DeploymentPackage using reflection.
     */
    private AWDDeploymentPackage extractDeploymentPackageFields(Object deploymentPackage) 
            throws Exception {
        
        Class<?> clazz = deploymentPackage.getClass();
        
        // Extract key fields safely
        Map<String, Object> customDataTypeMap = extractField(deploymentPackage, "customDataTypeMap", Map.class);
        String customDataTypes = extractField(deploymentPackage, "customDataTypes", String.class);
        Map<String, Object> dependencies = extractField(deploymentPackage, "dependencies", Map.class);
        List<?> deployList = extractField(deploymentPackage, "deployList", List.class);
        Object rootModel = extractField(deploymentPackage, "rootModel", Object.class);
        List<?> saveList = extractField(deploymentPackage, "saveList", List.class);
        
        // Validate extracted structure
        validateAWDStructure(customDataTypeMap, dependencies, deployList, rootModel);
        
        // Create wrapper object
        AWDDeploymentPackage result = new AWDDeploymentPackage();
        result.setCustomDataTypeMap(customDataTypeMap != null ? customDataTypeMap : new HashMap<>());
        result.setCustomDataTypes(customDataTypes);
        result.setDependencies(dependencies != null ? dependencies : new HashMap<>());
        result.setDeployList(deployList);
        result.setRootModel(rootModel);
        result.setSaveList(saveList);
        
        logger.info("Successfully extracted AWD DeploymentPackage with " + 
                   (deployList != null ? deployList.size() : 0) + " deploy items");
        
        return result;
    }
    
    /**
     * Safely extract field using reflection.
     */
    @SuppressWarnings("unchecked")
    private <T> T extractField(Object obj, String fieldName, Class<T> expectedType) {
        try {
            var field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(obj);
            
            if (value == null) {
                return null;
            }
            
            if (expectedType.isAssignableFrom(value.getClass())) {
                return (T) value;
            } else {
                logger.warning("Field " + fieldName + " type mismatch. Expected: " + 
                             expectedType.getName() + ", got: " + value.getClass().getName());
                return null;
            }
        } catch (Exception e) {
            logger.warning("Failed to extract field " + fieldName + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Validate AWD structure integrity.
     */
    private void validateAWDStructure(Map<String, Object> customDataTypeMap, 
                                     Map<String, Object> dependencies,
                                     List<?> deployList, 
                                     Object rootModel) throws IOException {
        
        // Validate deploy list
        if (deployList == null || deployList.isEmpty()) {
            logger.warning("DeploymentPackage has empty deploy list");
        }
        
        // Validate root model
        if (rootModel == null) {
            throw new IOException("DeploymentPackage missing root model");
        }
        
        String rootModelClass = rootModel.getClass().getName();
        if (!rootModelClass.startsWith("com.dstawd.design.model.")) {
            throw new IOException("Invalid root model class: " + rootModelClass);
        }
        
        // Validate custom data types
        if (customDataTypeMap != null && !customDataTypeMap.isEmpty()) {
            logger.info("Found " + customDataTypeMap.size() + " custom data types");
            processCustomDataTypes(customDataTypeMap);
        }
        
        // Validate dependencies
        if (dependencies != null && !dependencies.isEmpty()) {
            logger.info("Found " + dependencies.size() + " dependencies");
        }
        
        logger.info("AWD structure validation completed successfully");
    }
    
    /**
     * Process and validate custom data types.
     */
    private void processCustomDataTypes(Map<String, Object> customDataTypeMap) {
        for (Map.Entry<String, Object> entry : customDataTypeMap.entrySet()) {
            String typeName = entry.getKey();
            Object typeDefinition = entry.getValue();
            
            logger.fine("Processing custom data type: " + typeName + 
                       " (class: " + (typeDefinition != null ? typeDefinition.getClass().getName() : "null") + ")");
            
            // Additional validation can be added here for specific custom types
        }
    }
    
}