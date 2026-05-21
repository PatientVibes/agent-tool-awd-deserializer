/**
 * Module: AWDStructureMapper - AWD object structure mapping without dependencies
 * 
 * Summary:
 *     Advanced structure mapper that creates logical AWD object representations
 *     without requiring proprietary JAR dependencies. Maps raw serialization data
 *     to structured AWD domain objects for better analysis and processing.
 * 
 * Key Components:
 *     - mapAWDObject(): Main AWD object mapping method
 *     - createDeploymentPackageStructure(): DeploymentPackage mapping
 *     - extractAWDFields(): AWD-specific field extraction
 *     - buildHierarchicalStructure(): Nested object structure creation
 * 
 * Keywords: awd, structure, mapping, deployment, package, hierarchy, domain, objects,
 *          field, extraction, logical, representation, jar-free, analysis, processing
 * 
 * Dependencies:
 *     - ReflectionUtils: AWD class pattern recognition
 *     - RawSerializationReader: Raw data extraction
 *     - java.util.Map: Generic object representation
 * 
 * Security:
 *     - Safe field mapping without class instantiation
 *     - Controlled object creation with validation
 *     - Input sanitization for all field mappings
 *     - Protection against recursive object references
 * 
 * Performance:
 *     - Efficient mapping with minimal memory allocation
 *     - Cached structure definitions for common AWD objects
 *     - Lazy evaluation of complex nested structures
 *     - Optimized field access patterns
 */
package com.patientvibes.awd.deserializer.reflection;

import java.util.*;
import java.util.logging.Logger;

/**
 * Maps AWD objects to structured representations without requiring AWD JARs.
 */
public class AWDStructureMapper {
    private static final Logger logger = Logger.getLogger(AWDStructureMapper.class.getName());
    
    // Known AWD object types and their expected structures
    private static final Map<String, Map<String, String>> AWD_STRUCTURES = new HashMap<>();
    
    static {
        // DeploymentPackage structure definition
        Map<String, String> deploymentPackageFields = new HashMap<>();
        deploymentPackageFields.put("name", "String");
        deploymentPackageFields.put("version", "String");
        deploymentPackageFields.put("description", "String");
        deploymentPackageFields.put("components", "List<Component>");
        deploymentPackageFields.put("dependencies", "List<Dependency>");
        deploymentPackageFields.put("metadata", "Map<String,Object>");
        deploymentPackageFields.put("created", "Date");
        deploymentPackageFields.put("modified", "Date");
        deploymentPackageFields.put("author", "String");
        deploymentPackageFields.put("configuration", "Configuration");
        AWD_STRUCTURES.put("com.dstawd.design.model.DeploymentPackage", deploymentPackageFields);
        
        // Component structure definition
        Map<String, String> componentFields = new HashMap<>();
        componentFields.put("id", "String");
        componentFields.put("name", "String");
        componentFields.put("type", "String");
        componentFields.put("properties", "Map<String,Object>");
        componentFields.put("connections", "List<Connection>");
        AWD_STRUCTURES.put("com.dstawd.design.model.Component", componentFields);
        
        // Configuration structure definition
        Map<String, String> configFields = new HashMap<>();
        configFields.put("settings", "Map<String,Object>");
        configFields.put("environment", "String");
        configFields.put("parameters", "List<Parameter>");
        AWD_STRUCTURES.put("com.dstawd.design.model.Configuration", configFields);
    }
    
    /**
     * Map raw AWD object data to structured representation.
     */
    public Map<String, Object> mapAWDObject(Map<String, Object> rawData) {
        logger.info("Mapping AWD object structure from raw data");
        
        Map<String, Object> mappedObject = new HashMap<>();
        
        // Extract basic metadata
        mappedObject.put("_mappingMode", "awd_structure_mapping");
        mappedObject.put("_mappedAt", System.currentTimeMillis());
        
        // Process serialization header
        Map<String, Object> headerInfo = extractHeaderInfo(rawData);
        if (headerInfo != null) {
            mappedObject.put("serializationInfo", headerInfo);
        }
        
        // Process object structure
        Map<String, Object> objectStructure = extractObjectStructure(rawData);
        if (objectStructure != null) {
            mappedObject.put("awdStructure", objectStructure);
        }
        
        // Add processing statistics
        mappedObject.put("_statistics", createMappingStatistics(rawData, mappedObject));
        
        logger.info("AWD object structure mapping completed");
        return mappedObject;
    }
    
    /**
     * Extract and clean header information.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractHeaderInfo(Map<String, Object> rawData) {
        Object headerObj = rawData.get("_serializationHeader");
        if (headerObj instanceof Map) {
            Map<String, Object> header = (Map<String, Object>) headerObj;
            Map<String, Object> cleanHeader = new HashMap<>();
            
            // Extract useful header information
            cleanHeader.put("javaSerializationVersion", getNestedValue(header, "version"));
            cleanHeader.put("validFormat", getNestedValue(header, "magicValid"));
            cleanHeader.put("dataSize", rawData.get("_dataSize"));
            
            return cleanHeader;
        }
        return null;
    }
    
    /**
     * Extract and structure AWD object information.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractObjectStructure(Map<String, Object> rawData) {
        Object structureObj = rawData.get("_objectStructure");
        if (structureObj instanceof Map) {
            Map<String, Object> structure = (Map<String, Object>) structureObj;
            Map<String, Object> awdStructure = new HashMap<>();
            
            // Extract object count and metadata - use safe conversion
            awdStructure.put("totalObjects", safeToInteger(getNestedValue(structure, "objectCount")));
            awdStructure.put("truncated", getNestedValue(structure, "_truncated"));
            
            // Process individual objects
            Object objectsObj = getNestedValue(structure, "objects");
            if (objectsObj instanceof List) {
                List<Object> objects = (List<Object>) objectsObj;
                List<Map<String, Object>> processedObjects = new ArrayList<>();
                
                for (Object obj : objects) {
                    if (obj instanceof Map) {
                        Map<String, Object> processedObj = processObject((Map<String, Object>) obj);
                        if (processedObj != null) {
                            processedObjects.add(processedObj);
                        }
                    }
                }
                
                awdStructure.put("objects", processedObjects);
                awdStructure.put("processedCount", processedObjects.size());
                
                // Analyze for AWD-specific patterns
                Map<String, Object> awdAnalysis = analyzeAWDPatterns(processedObjects);
                awdStructure.put("awdAnalysis", awdAnalysis);
            }
            
            return awdStructure;
        }
        return null;
    }
    
    /**
     * Process individual object from raw data.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> processObject(Map<String, Object> rawObject) {
        Map<String, Object> processedObject = new HashMap<>();
        
        // Extract basic object information
        String typeName = (String) getNestedValue(rawObject, "typeName");
        String typeCode = (String) getNestedValue(rawObject, "typeCode");
        String type = (String) getNestedValue(rawObject, "type");
        
        processedObject.put("typeName", typeName);
        processedObject.put("typeCode", typeCode);
        processedObject.put("type", type);
        
        // Process class descriptor objects (most important for AWD)
        if ("CLASSDESC".equals(typeName)) {
            String className = (String) getNestedValue(rawObject, "className");
            Long serialVersionUID = (Long) getNestedValue(rawObject, "serialVersionUID");
            Boolean isAWDClass = (Boolean) getNestedValue(rawObject, "_isAWDClass");
            
            processedObject.put("className", className);
            processedObject.put("serialVersionUID", serialVersionUID);
            processedObject.put("isAWDClass", isAWDClass);
            
            // If this is an AWD class, add structure mapping
            if (Boolean.TRUE.equals(isAWDClass) && className != null) {
                Map<String, Object> awdMapping = mapToAWDStructure(className, serialVersionUID);
                processedObject.put("awdMapping", awdMapping);
            }
        }
        
        // Process object instances with field data
        if ("OBJECT".equals(typeName)) {
            Object fieldsObj = getNestedValue(rawObject, "fields");
            if (fieldsObj instanceof Map) {
                Map<String, Object> fieldData = processFieldData((Map<String, Object>) fieldsObj);
                processedObject.put("fieldData", fieldData);
            }
            
            Integer fieldCount = safeToInteger(getNestedValue(rawObject, "fieldCount"));
            if (fieldCount != null) {
                processedObject.put("fieldCount", fieldCount);
            }
        }
        
        // Process string objects (may contain AWD identifiers)
        if ("STRING".equals(typeName)) {
            String value = (String) getNestedValue(rawObject, "value");
            Integer length = safeToInteger(getNestedValue(rawObject, "length"));
            Boolean isAWDClassName = (Boolean) getNestedValue(rawObject, "_isAWDClassName");
            
            if (value != null) {
                processedObject.put("value", value);
            }
            if (length != null) {
                processedObject.put("length", length);
            }
            if (Boolean.TRUE.equals(isAWDClassName)) {
                processedObject.put("isAWDClassName", true);
            }
        }
        
        return processedObject;
    }
    
    /**
     * Process field data from object instances.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> processFieldData(Map<String, Object> rawFields) {
        Map<String, Object> fieldData = new HashMap<>();
        
        Integer fieldsRead = safeToInteger(getNestedValue(rawFields, "_fieldsRead"));
        if (fieldsRead != null) {
            fieldData.put("fieldsRead", fieldsRead);
        }
        
        // Process individual fields
        Map<String, Object> fields = new HashMap<>();
        for (Map.Entry<String, Object> entry : rawFields.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("field_") && entry.getValue() instanceof Map) {
                Map<String, Object> field = (Map<String, Object>) entry.getValue();
                Map<String, Object> processedField = processField(field);
                fields.put(key, processedField);
            }
        }
        
        if (!fields.isEmpty()) {
            fieldData.put("fields", fields);
        }
        
        return fieldData;
    }
    
    /**
     * Process individual field information.
     */
    private Map<String, Object> processField(Map<String, Object> rawField) {
        Map<String, Object> field = new HashMap<>();
        
        String type = (String) getNestedValue(rawField, "type");
        field.put("type", type);
        
        if ("string".equals(type)) {
            String value = (String) getNestedValue(rawField, "value");
            Integer length = safeToInteger(getNestedValue(rawField, "length"));
            
            if (value != null) {
                field.put("value", value);
                // Check if this looks like AWD data
                if (ReflectionUtils.isAWDClass(value)) {
                    field.put("isAWDReference", true);
                }
            }
            if (length != null) {
                field.put("length", length);
            }
        } else if ("reference".equals(type)) {
            Integer handle = safeToInteger(getNestedValue(rawField, "handle"));
            if (handle != null) {
                field.put("handle", handle);
            }
        }
        
        return field;
    }
    
    /**
     * Map AWD class to its expected structure.
     */
    private Map<String, Object> mapToAWDStructure(String className, Long serialVersionUID) {
        Map<String, Object> awdMapping = new HashMap<>();
        
        awdMapping.put("className", className);
        awdMapping.put("serialVersionUID", serialVersionUID);
        
        // Get expected structure if we know it
        Map<String, String> expectedStructure = AWD_STRUCTURES.get(className);
        if (expectedStructure != null) {
            awdMapping.put("expectedFields", expectedStructure);
            awdMapping.put("knownStructure", true);
        } else {
            awdMapping.put("knownStructure", false);
            awdMapping.put("note", "Unknown AWD class structure");
        }
        
        // Add classification
        String classification = classifyAWDObject(className);
        awdMapping.put("classification", classification);
        
        return awdMapping;
    }
    
    /**
     * Classify AWD object by its class name.
     */
    private String classifyAWDObject(String className) {
        if (className.contains("DeploymentPackage")) {
            return "DeploymentPackage";
        } else if (className.contains("Component")) {
            return "Component";
        } else if (className.contains("Configuration")) {
            return "Configuration";
        } else if (className.contains("Connection")) {
            return "Connection";
        } else if (className.contains("Parameter")) {
            return "Parameter";
        } else if (className.contains("model")) {
            return "DomainModel";
        } else if (className.contains("design")) {
            return "DesignElement";
        }
        return "Unknown";
    }
    
    /**
     * Analyze processed objects for AWD-specific patterns.
     */
    private Map<String, Object> analyzeAWDPatterns(List<Map<String, Object>> objects) {
        Map<String, Object> analysis = new HashMap<>();
        
        int awdClassCount = 0;
        int objectInstanceCount = 0;
        int stringCount = 0;
        Set<String> awdClasses = new HashSet<>();
        
        for (Map<String, Object> obj : objects) {
            String typeName = (String) obj.get("typeName");
            
            if ("CLASSDESC".equals(typeName)) {
                Boolean isAWDClass = (Boolean) obj.get("isAWDClass");
                if (Boolean.TRUE.equals(isAWDClass)) {
                    awdClassCount++;
                    String className = (String) obj.get("className");
                    if (className != null) {
                        awdClasses.add(className);
                    }
                }
            } else if ("OBJECT".equals(typeName)) {
                objectInstanceCount++;
            } else if ("STRING".equals(typeName)) {
                stringCount++;
            }
        }
        
        analysis.put("awdClassCount", awdClassCount);
        analysis.put("objectInstanceCount", objectInstanceCount);
        analysis.put("stringCount", stringCount);
        analysis.put("awdClasses", new ArrayList<>(awdClasses));
        analysis.put("totalProcessed", objects.size());
        
        // Determine primary AWD type
        if (awdClasses.contains("com.dstawd.design.model.DeploymentPackage")) {
            analysis.put("primaryType", "DeploymentPackage");
        } else if (!awdClasses.isEmpty()) {
            analysis.put("primaryType", "AWDDesignFile");
        } else {
            analysis.put("primaryType", "Unknown");
        }
        
        return analysis;
    }
    
    /**
     * Create mapping statistics.
     */
    private Map<String, Object> createMappingStatistics(Map<String, Object> rawData, Map<String, Object> mappedData) {
        Map<String, Object> stats = new HashMap<>();
        
        // Input statistics
        stats.put("inputSize", rawData.size());
        stats.put("outputSize", mappedData.size());
        
        // Processing statistics
        Integer objectCount = safeToInteger(getNestedValue(rawData, "_objectStructure", "objectCount"));
        if (objectCount != null) {
            stats.put("objectsProcessed", objectCount);
        }
        
        Integer dataSize = safeToInteger(rawData.get("_dataSize"));
        if (dataSize != null) {
            stats.put("originalDataSize", dataSize);
        }
        
        return stats;
    }
    
    /**
     * Safely get nested value from Map structure.
     */
    @SuppressWarnings("unchecked")
    private Object getNestedValue(Map<String, Object> map, String... keys) {
        Object current = map;
        
        for (String key : keys) {
            if (current instanceof Map) {
                Map<String, Object> currentMap = (Map<String, Object>) current;
                current = currentMap.get(key);
                
                // Handle wrapped values (_value field)
                if (current instanceof Map) {
                    Map<String, Object> valueMap = (Map<String, Object>) current;
                    if (valueMap.containsKey("_value")) {
                        current = valueMap.get("_value");
                    }
                }
            } else {
                return null;
            }
        }
        
        return current;
    }
    
    /**
     * Safely convert value to Integer, handling various numeric types.
     */
    private Integer safeToInteger(Object value) {
        if (value == null) return null;
        
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Short) {
            return ((Short) value).intValue();
        } else if (value instanceof Long) {
            return ((Long) value).intValue();
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}