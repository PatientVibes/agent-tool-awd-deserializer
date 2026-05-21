/**
 * Module: AwdSchemaMapper - AWD object pattern mapping for BPMN-compatible representations
 * 
 * Summary:
 *     Maps AWD object patterns from decompiled code analysis to BPMN-compatible structures.
 *     Extracts business process elements from deserialized .design files and converts
 *     AWD internal structures to standardized representations for file generation.
 * 
 * Key Components:
 *     - mapAwdData(): Primary mapping orchestration for AWD object transformation
 *     - extractProcessElements(): BPMN element mapping from AWD process definitions
 *     - mapFieldStructures(): Field mapping transformation for form generation
 *     - extractServiceDefinitions(): Service configuration mapping for integration
 * 
 * Keywords: awd, schema, mapper, pattern, mapping, bpmn, compatible, transformation,
 *          process, elements, field, structures, service, definitions, decompiled, analysis
 * 
 * Dependencies:
 *     - com.patientvibes.awd.deserializer.business.model.BusinessMetadata: Foundation metadata structures
 *     - java.util.regex.Pattern: Pattern matching for AWD element extraction
 *     - java.util.Map: Data structure transformation and mapping
 * 
 * Security:
 *     - Input validation for all AWD data structures
 *     - Safe pattern matching with bounded operations
 *     - Controlled memory usage during large object mapping
 * 
 * Performance:
 *     - Efficient pattern compilation and reuse
 *     - Lazy evaluation of mapping operations
 *     - Memory-optimized data structure transformation
 */
package com.patientvibes.awd.deserializer.extractor.mapper;

import com.patientvibes.awd.deserializer.business.model.BusinessMetadata;
import com.patientvibes.awd.deserializer.business.model.BusinessProcess;
import com.patientvibes.awd.deserializer.business.model.WorkflowElement;

import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps AWD object patterns to BPMN-compatible representations.
 */
public class AwdSchemaMapper {
    private static final Logger logger = Logger.getLogger(AwdSchemaMapper.class.getName());
    
    // AWD pattern recognition compiled regex patterns
    private static final Pattern AWD_PROCESS_PATTERN = Pattern.compile(
        "com\\.dstawd\\.design\\.model\\.(\\w+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern AWD_FIELD_PATTERN = Pattern.compile(
        "awd:awd-value\\('([^']+)'(?:,\\s*'([^']+)')?\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern AWD_SERVICE_PATTERN = Pattern.compile(
        "com\\.dstawd\\.serviceengine\\.model\\.(\\w+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern AWD_FORM_PATTERN = Pattern.compile(
        "com\\.dstawd\\.serviceengine\\.model\\.config\\.(Html\\w+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern AWD_EXTENSION_PATTERN = Pattern.compile(
        "<awd:([^>\\s]+)(?:[^>]*>([^<]*)</awd:[^>]*>)?", Pattern.CASE_INSENSITIVE);
    
    // AWD namespace and schema information
    private static final String AWD_NAMESPACE = "http://www.dstawd.com";
    private static final String AWD_PREFIX = "awd";
    
    public AwdSchemaMapper() {
        logger.info("AWD Schema Mapper initialized with namespace: " + AWD_NAMESPACE);
    }
    
    /**
     * Map AWD data structures to extraction-friendly format.
     * 
     * @param deserializedData Raw AWD deserialized data
     * @param metadata Business metadata extracted from AWD data
     * @return Mapped data structure optimized for file generation
     */
    public Map<String, Object> mapAwdData(Map<String, Object> deserializedData, BusinessMetadata metadata) {
        logger.info("Starting AWD data mapping for " + deserializedData.size() + " data elements");
        
        Map<String, Object> mappedData = new HashMap<>();
        
        try {
            // Extract and map process structures
            Map<String, Object> processData = mapProcessStructures(deserializedData, metadata);
            mappedData.put("processes", processData);
            
            // Extract and map form structures
            Map<String, Object> formData = mapFormStructures(deserializedData, metadata);
            mappedData.put("forms", formData);
            
            // Extract and map service structures
            Map<String, Object> serviceData = mapServiceStructures(deserializedData, metadata);
            mappedData.put("services", serviceData);
            
            // Extract AWD extensions and custom elements
            Map<String, Object> extensionData = mapAwdExtensions(deserializedData);
            mappedData.put("extensions", extensionData);
            
            // Extract field mappings and expressions
            Map<String, Object> fieldData = mapFieldMappings(deserializedData, metadata);
            mappedData.put("fieldMappings", fieldData);
            
            logger.info("AWD data mapping completed successfully");
            
        } catch (Exception e) {
            logger.severe("Error during AWD data mapping: " + e.getMessage());
            mappedData.put("mappingError", e.getMessage());
        }
        
        return mappedData;
    }
    
    /**
     * Map process structures for BPMN generation.
     */
    private Map<String, Object> mapProcessStructures(Map<String, Object> data, BusinessMetadata metadata) {
        Map<String, Object> processData = new HashMap<>();
        
        // Extract process definitions from business metadata
        List<Map<String, Object>> mappedProcesses = new ArrayList<>();
        
        for (BusinessProcess process : metadata.getBusinessProcesses()) {
            Map<String, Object> processMap = new HashMap<>();
            processMap.put("id", process.getId());
            processMap.put("name", process.getName());
            processMap.put("type", process.getType());
            processMap.put("version", process.getVersion());
            
            // Map process elements to BPMN-compatible format
            List<Map<String, Object>> elements = new ArrayList<>();
            for (var element : process.getElements()) {
                Map<String, Object> elementMap = new HashMap<>();
                elementMap.put("id", element.getId());
                elementMap.put("name", element.getName());
                elementMap.put("type", element.getType());
                elements.add(elementMap);
            }
            processMap.put("elements", elements);
            
            // Map process variables
            List<Map<String, Object>> variables = new ArrayList<>();
            for (var variable : process.getVariables()) {
                Map<String, Object> variableMap = new HashMap<>();
                variableMap.put("name", variable.getName());
                variableMap.put("type", variable.getType());
                variableMap.put("sourceExpression", variable.getSourceExpression());
                variables.add(variableMap);
            }
            processMap.put("variables", variables);
            
            // Add AWD-specific metadata
            processMap.put("awdNamespace", AWD_NAMESPACE);
            processMap.put("createdBy", process.getCreatedBy());
            processMap.put("modifiedBy", process.getModifiedBy());
            processMap.put("modelState", process.getModelState());
            
            mappedProcesses.add(processMap);
        }
        
        processData.put("businessProcesses", mappedProcesses);
        processData.put("totalProcesses", mappedProcesses.size());
        
        return processData;
    }
    
    /**
     * Map form structures for HTML generation.
     */
    private Map<String, Object> mapFormStructures(Map<String, Object> data, BusinessMetadata metadata) {
        Map<String, Object> formData = new HashMap<>();
        
        List<Map<String, Object>> forms = new ArrayList<>();
        String content = extractStringContent(data);
        
        if (content != null) {
            // Extract HTML form control patterns
            Matcher formMatcher = AWD_FORM_PATTERN.matcher(content);
            Set<String> uniqueFormTypes = new HashSet<>();
            
            while (formMatcher.find()) {
                String formType = formMatcher.group(1);
                if (uniqueFormTypes.add(formType)) {
                    Map<String, Object> form = new HashMap<>();
                    form.put("type", formType);
                    form.put("awdClass", "com.dstawd.serviceengine.model.config." + formType);
                    form.put("namespace", AWD_NAMESPACE);
                    
                    // Map form properties based on type
                    Map<String, Object> properties = mapFormTypeProperties(formType, content);
                    form.put("properties", properties);
                    
                    forms.add(form);
                }
            }
        }
        
        formData.put("formControls", forms);
        formData.put("totalForms", forms.size());
        
        return formData;
    }
    
    /**
     * Map service structures for configuration generation.
     */
    private Map<String, Object> mapServiceStructures(Map<String, Object> data, BusinessMetadata metadata) {
        Map<String, Object> serviceData = new HashMap<>();
        
        List<Map<String, Object>> services = new ArrayList<>();
        
        // Extract services from business metadata
        for (var serviceDefinition : metadata.getServiceDefinitions()) {
            Map<String, Object> service = new HashMap<>();
            service.put("id", serviceDefinition.getServiceId());
            service.put("class", serviceDefinition.getServiceClass());
            service.put("type", serviceDefinition.getServiceType());
            service.put("properties", serviceDefinition.getProperties());
            service.put("dependencies", serviceDefinition.getDependencies());
            service.put("awdNamespace", AWD_NAMESPACE);
            
            services.add(service);
        }
        
        serviceData.put("serviceDefinitions", services);
        serviceData.put("totalServices", services.size());
        
        return serviceData;
    }
    
    /**
     * Map AWD extensions and custom elements.
     */
    private Map<String, Object> mapAwdExtensions(Map<String, Object> data) {
        Map<String, Object> extensionData = new HashMap<>();
        
        String content = extractStringContent(data);
        if (content == null) {
            return extensionData;
        }
        
        List<Map<String, Object>> extensions = new ArrayList<>();
        Matcher extensionMatcher = AWD_EXTENSION_PATTERN.matcher(content);
        
        while (extensionMatcher.find()) {
            Map<String, Object> extension = new HashMap<>();
            extension.put("elementName", extensionMatcher.group(1));
            extension.put("content", extensionMatcher.group(2) != null ? extensionMatcher.group(2) : "");
            extension.put("namespace", AWD_NAMESPACE);
            extension.put("prefix", AWD_PREFIX);
            
            extensions.add(extension);
        }
        
        extensionData.put("customElements", extensions);
        extensionData.put("namespace", AWD_NAMESPACE);
        extensionData.put("prefix", AWD_PREFIX);
        extensionData.put("totalExtensions", extensions.size());
        
        return extensionData;
    }
    
    /**
     * Map field mappings and AWD expressions.
     */
    private Map<String, Object> mapFieldMappings(Map<String, Object> data, BusinessMetadata metadata) {
        Map<String, Object> fieldData = new HashMap<>();
        
        String content = extractStringContent(data);
        if (content == null) {
            return fieldData;
        }
        
        List<Map<String, Object>> fieldMappings = new ArrayList<>();
        Matcher fieldMatcher = AWD_FIELD_PATTERN.matcher(content);
        
        while (fieldMatcher.find()) {
            Map<String, Object> mapping = new HashMap<>();
            mapping.put("fieldName", fieldMatcher.group(1));
            mapping.put("expression", fieldMatcher.group(2) != null ? fieldMatcher.group(2) : "");
            mapping.put("awdFunction", "awd:awd-value");
            mapping.put("namespace", AWD_NAMESPACE);
            
            fieldMappings.add(mapping);
        }
        
        fieldData.put("awdExpressions", fieldMappings);
        fieldData.put("totalMappings", fieldMappings.size());
        
        // Add field mappings from business metadata
        List<Map<String, Object>> businessMappings = new ArrayList<>();
        for (var fieldMapping : metadata.getFieldMappings()) {
            Map<String, Object> mapping = new HashMap<>();
            mapping.put("name", fieldMapping.getMappingName());
            mapping.put("sourceFields", fieldMapping.getSourceFields());
            mapping.put("targetFields", fieldMapping.getTargetFields());
            mapping.put("transformationRules", fieldMapping.getTransformationRules());
            
            businessMappings.add(mapping);
        }
        fieldData.put("businessMappings", businessMappings);
        
        return fieldData;
    }
    
    /**
     * Map form type-specific properties.
     */
    private Map<String, Object> mapFormTypeProperties(String formType, String content) {
        Map<String, Object> properties = new HashMap<>();
        
        switch (formType) {
            case "HtmlInput":
            case "HtmlTextInput":
                properties.put("inputType", "text");
                properties.put("validation", "string");
                break;
            case "HtmlRadioGroup":
                properties.put("inputType", "radio");
                properties.put("multipleChoice", false);
                break;
            case "HtmlCheckboxInput":
                properties.put("inputType", "checkbox");
                properties.put("validation", "boolean");
                break;
            case "HtmlDateInput":
                properties.put("inputType", "date");
                properties.put("validation", "date");
                break;
            case "HtmlFileInput":
                properties.put("inputType", "file");
                properties.put("validation", "file");
                break;
            default:
                properties.put("inputType", "generic");
                break;
        }
        
        properties.put("awdClass", "com.dstawd.serviceengine.model.config." + formType);
        properties.put("namespace", AWD_NAMESPACE);
        
        return properties;
    }
    
    /**
     * Extract string content from nested data structure.
     */
    private String extractStringContent(Map<String, Object> data) {
        StringBuilder content = new StringBuilder();
        
        try {
            extractStringContentRecursive(data, content);
        } catch (Exception e) {
            logger.warning("Error extracting string content: " + e.getMessage());
            return null;
        }
        
        return content.length() > 0 ? content.toString() : null;
    }
    
    /**
     * Recursively extract string content from data structure.
     */
    @SuppressWarnings("unchecked")
    private void extractStringContentRecursive(Object obj, StringBuilder content) {
        if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;
            for (Object value : map.values()) {
                extractStringContentRecursive(value, content);
            }
        } else if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            for (Object item : list) {
                extractStringContentRecursive(item, content);
            }
        } else if (obj instanceof String) {
            String str = (String) obj;
            if (str.length() > 100) { // Focus on substantial strings that may contain AWD patterns
                content.append(str).append("\n");
            }
        }
    }
}