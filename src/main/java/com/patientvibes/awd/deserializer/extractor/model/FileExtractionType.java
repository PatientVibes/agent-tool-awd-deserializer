/**
 * Module: FileExtractionType - Enumeration of supported file extraction types
 * 
 * Summary:
 *     Defines the types of files that can be extracted from AWD design files including
 *     BPMN XML processes, HTML forms, service configurations, and metadata summaries.
 *     Provides file extension mapping and type categorization for modular extraction.
 * 
 * Key Components:
 *     - extractionTypes: Enumeration of supported output file types
 *     - fileExtensions: File extension mapping for each type
 *     - typeCategories: Logical grouping of related extraction types
 *     - processingPriority: Order and dependency management for extraction
 * 
 * Keywords: extraction, type, enumeration, bpmn, xml, html, forms, service, config,
 *          metadata, workflow, file, extension, category, priority, processing
 * 
 * Dependencies:
 *     - None: Self-contained enumeration with utility methods
 * 
 * Security:
 *     - Type validation for supported extraction operations
 *     - Safe file extension handling and validation
 * 
 * Performance:
 *     - Efficient enum-based type selection and validation
 *     - Optimized for file generation workflows
 */
package com.patientvibes.awd.deserializer.extractor.model;

/**
 * Enumeration of supported file extraction types.
 */
public enum FileExtractionType {
    BPMN_XML("bpmn", "BPMN Process Definition", "Business Process Model XML with AWD extensions", 1),
    FORM_HTML("html", "HTML Form Definition", "User interface forms with AWD field mappings", 2),
    SERVICE_CONFIG("json", "Service Configuration", "Service definitions and integration configurations", 2),
    WORKFLOW_DOC("md", "Workflow Documentation", "Human-readable workflow documentation", 3),
    METADATA("json", "Extraction Metadata", "Extraction summary and processing information", 4),
    UNKNOWN("txt", "Unknown Type", "Unknown or unspecified file type", 99);
    
    private final String fileExtension;
    private final String displayName;
    private final String description;
    private final int processingPriority;
    
    FileExtractionType(String fileExtension, String displayName, String description, int processingPriority) {
        this.fileExtension = fileExtension;
        this.displayName = displayName;
        this.description = description;
        this.processingPriority = processingPriority;
    }
    
    /**
     * Get the file extension for this extraction type.
     */
    public String getFileExtension() {
        return fileExtension;
    }
    
    /**
     * Get the human-readable display name.
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Get the detailed description of this extraction type.
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Get the processing priority (lower numbers processed first).
     */
    public int getProcessingPriority() {
        return processingPriority;
    }
    
    /**
     * Check if this type represents a primary business artifact.
     */
    public boolean isPrimaryArtifact() {
        return this == BPMN_XML || this == FORM_HTML || this == SERVICE_CONFIG;
    }
    
    /**
     * Check if this type represents supporting documentation.
     */
    public boolean isDocumentation() {
        return this == WORKFLOW_DOC || this == METADATA;
    }
    
    /**
     * Check if this type requires business metadata.
     */
    public boolean requiresBusinessMetadata() {
        return this != METADATA && this != UNKNOWN;
    }
    
    /**
     * Get the MIME type for this file type.
     */
    public String getMimeType() {
        switch (this) {
            case BPMN_XML:
                return "application/xml";
            case FORM_HTML:
                return "text/html";
            case SERVICE_CONFIG:
            case METADATA:
                return "application/json";
            case WORKFLOW_DOC:
                return "text/markdown";
            default:
                return "text/plain";
        }
    }
    
    /**
     * Get file type by extension.
     */
    public static FileExtractionType fromExtension(String extension) {
        if (extension == null) {
            return UNKNOWN;
        }
        
        String cleanExtension = extension.toLowerCase().replace(".", "");
        for (FileExtractionType type : values()) {
            if (type.getFileExtension().equals(cleanExtension)) {
                return type;
            }
        }
        return UNKNOWN;
    }
    
    /**
     * Get all primary artifact types.
     */
    public static FileExtractionType[] getPrimaryTypes() {
        return new FileExtractionType[]{BPMN_XML, FORM_HTML, SERVICE_CONFIG};
    }
    
    /**
     * Get all documentation types.
     */
    public static FileExtractionType[] getDocumentationTypes() {
        return new FileExtractionType[]{WORKFLOW_DOC, METADATA};
    }
    
    @Override
    public String toString() {
        return displayName + " (." + fileExtension + ")";
    }
}