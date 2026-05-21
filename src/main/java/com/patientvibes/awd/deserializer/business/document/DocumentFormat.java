/**
 * Module: DocumentFormat - Business document output format enumeration
 * 
 * Summary:
 *     Defines supported output formats for business metadata documents including
 *     JSON, XML, and specialized formats for different business use cases.
 *     Provides format-specific configuration and file extension mapping.
 * 
 * Key Components:
 *     - formatTypes: Enumeration of supported document formats
 *     - formatProperties: Format-specific configuration and properties
 *     - fileExtensions: File extension mapping for output files
 *     - formatValidation: Format capability and constraint definitions
 * 
 * Keywords: document, format, output, json, xml, bpmn, field, mapping, service,
 *          registry, extension, configuration, validation, capability, constraint
 * 
 * Dependencies:
 *     - None: Self-contained enumeration with utility methods
 * 
 * Security:
 *     - Format validation for supported output types
 *     - Safe file extension handling and validation
 *     - Controlled format capability definitions
 * 
 * Performance:
 *     - Efficient enum-based format selection
 *     - Optimized for document generation workflows
 *     - Memory-efficient format configuration
 */
package com.patientvibes.awd.deserializer.business.document;

/**
 * Enumeration of supported business document formats.
 */
public enum DocumentFormat {
    BPMN_JSON("json", "Business Process Model JSON", true, false),
    BPMN_XML("xml", "Business Process Model XML", true, true),
    FIELD_MAPPING_JSON("json", "Field Mapping JSON", true, false),
    FIELD_MAPPING_XML("xml", "Field Mapping XML", true, true),
    SERVICE_REGISTRY_JSON("json", "Service Registry JSON", true, false),
    SERVICE_REGISTRY_XML("xml", "Service Registry XML", true, true),
    CONSOLIDATED_JSON("json", "Consolidated Business Metadata JSON", true, false),
    CONSOLIDATED_XML("xml", "Consolidated Business Metadata XML", true, true),
    CUSTOM_TEMPLATE("txt", "Custom Template Format", false, false);
    
    private final String extension;
    private final String description;
    private final boolean structured;
    private final boolean xmlBased;
    
    DocumentFormat(String extension, String description, boolean structured, boolean xmlBased) {
        this.extension = extension;
        this.description = description;
        this.structured = structured;
        this.xmlBased = xmlBased;
    }
    
    /**
     * Get the file extension for this format.
     */
    public String getExtension() {
        return extension;
    }
    
    /**
     * Get the human-readable description of this format.
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if this format is structured (JSON/XML).
     */
    public boolean isStructured() {
        return structured;
    }
    
    /**
     * Check if this format is XML-based.
     */
    public boolean isXmlBased() {
        return xmlBased;
    }
    
    /**
     * Check if this format is JSON-based.
     */
    public boolean isJsonBased() {
        return structured && !xmlBased;
    }
    
    /**
     * Get the MIME type for this format.
     */
    public String getMimeType() {
        switch (this) {
            case BPMN_JSON:
            case FIELD_MAPPING_JSON:
            case SERVICE_REGISTRY_JSON:
            case CONSOLIDATED_JSON:
                return "application/json";
            case BPMN_XML:
            case FIELD_MAPPING_XML:
            case SERVICE_REGISTRY_XML:
            case CONSOLIDATED_XML:
                return "application/xml";
            case CUSTOM_TEMPLATE:
                return "text/plain";
            default:
                return "application/octet-stream";
        }
    }
    
    /**
     * Check if this format supports business process data.
     */
    public boolean supportsBpmn() {
        return this == BPMN_JSON || this == BPMN_XML || 
               this == CONSOLIDATED_JSON || this == CONSOLIDATED_XML;
    }
    
    /**
     * Check if this format supports field mapping data.
     */
    public boolean supportsFieldMappings() {
        return this == FIELD_MAPPING_JSON || this == FIELD_MAPPING_XML ||
               this == CONSOLIDATED_JSON || this == CONSOLIDATED_XML;
    }
    
    /**
     * Check if this format supports service registry data.
     */
    public boolean supportsServiceRegistry() {
        return this == SERVICE_REGISTRY_JSON || this == SERVICE_REGISTRY_XML ||
               this == CONSOLIDATED_JSON || this == CONSOLIDATED_XML;
    }
    
    /**
     * Get format by file extension.
     */
    public static DocumentFormat fromExtension(String extension) {
        if (extension == null) {
            return null;
        }
        
        String cleanExtension = extension.toLowerCase().replace(".", "");
        for (DocumentFormat format : values()) {
            if (format.getExtension().equals(cleanExtension)) {
                return format;
            }
        }
        return null;
    }
    
    /**
     * Get all JSON formats.
     */
    public static DocumentFormat[] getJsonFormats() {
        return new DocumentFormat[]{
            BPMN_JSON, FIELD_MAPPING_JSON, SERVICE_REGISTRY_JSON, CONSOLIDATED_JSON
        };
    }
    
    /**
     * Get all XML formats.
     */
    public static DocumentFormat[] getXmlFormats() {
        return new DocumentFormat[]{
            BPMN_XML, FIELD_MAPPING_XML, SERVICE_REGISTRY_XML, CONSOLIDATED_XML
        };
    }
    
    /**
     * Get all consolidated formats.
     */
    public static DocumentFormat[] getConsolidatedFormats() {
        return new DocumentFormat[]{
            CONSOLIDATED_JSON, CONSOLIDATED_XML
        };
    }
    
    @Override
    public String toString() {
        return description + " (." + extension + ")";
    }
}