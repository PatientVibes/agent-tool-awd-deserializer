/**
 * Module: ChorusDeserializer - Main entry point for design file deserialization
 * 
 * Summary:
 *     Orchestrates the complete deserialization pipeline from compressed .design files
 *     to JSON output. Manages memory optimization, security validation, and error
 *     handling for enterprise-scale file processing.
 * 
 * Key Components:
 *     - process(): Main processing pipeline for file conversion
 *     - main(): Command-line interface entry point
 *     - Configuration management via DeserializerConfig
 *     - Memory monitoring and optimization
 * 
 * Keywords: deserializer, main, entry, point, design, file, json, converter, processor, 
 *           orchestrator, pipeline, memory, optimization, streaming, gzip, decompress
 * Dependencies: DeserializerConfig, DeserializationEngine, FileProcessor, MemoryManager, StreamingJsonWriter
 * Security: Input file validation, memory limits, secure deserialization
 * Performance: Streaming processing, memory pooling, batch optimization
 */
package com.patientvibes.awd.deserializer;

import com.patientvibes.awd.deserializer.config.DeserializerConfig;
import com.patientvibes.awd.deserializer.core.DeserializationEngine;
import com.patientvibes.awd.deserializer.io.FileProcessor;
import com.patientvibes.awd.deserializer.memory.MemoryManager;
import com.patientvibes.awd.deserializer.json.StreamingJsonWriter;
import com.patientvibes.awd.deserializer.business.BusinessMetadataExtractor;
import com.patientvibes.awd.deserializer.business.document.DocumentFormat;
import com.patientvibes.awd.deserializer.business.model.BusinessMetadata;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Main entry point for the Chorus Deserializer application.
 * Orchestrates the deserialization process from .design files to JSON.
 */
public class ChorusDeserializer {
    private static final Logger logger = Logger.getLogger(ChorusDeserializer.class.getName());
    
    private final DeserializerConfig config;
    private final MemoryManager memoryManager;
    private final FileProcessor fileProcessor;
    private final DeserializationEngine engine;
    private final StreamingJsonWriter jsonWriter;
    private final BusinessMetadataExtractor businessExtractor;
    
    public ChorusDeserializer(DeserializerConfig config) {
        this.config = config;
        this.memoryManager = new MemoryManager(config);
        this.fileProcessor = new FileProcessor(config);
        this.engine = new DeserializationEngine(config, memoryManager);
        this.jsonWriter = new StreamingJsonWriter(config);
        this.businessExtractor = new BusinessMetadataExtractor();
    }
    
    /**
     * Process a design file and convert it to JSON.
     * 
     * @param inputFile The input .design file
     * @param outputFile The output .json file
     * @throws IOException If file processing fails
     */
    public void process(File inputFile, File outputFile) throws IOException {
        long startTime = System.currentTimeMillis();
        logger.info("Starting deserialization of: " + inputFile.getAbsolutePath());
        
        // Log initial memory state
        memoryManager.logMemoryInfo();
        
        try {
            // Step 1: Decompress the GZIP file
            logger.info("Step 1/3: Decompressing file...");
            long stepStart = System.currentTimeMillis();
            byte[] decompressedData = fileProcessor.decompressFile(inputFile);
            long stepTime = System.currentTimeMillis() - stepStart;
            logger.info("Decompression completed in " + stepTime + "ms");
            
            // Step 2: Deserialize the Java object
            logger.info("Step 2/3: Deserializing Java object...");
            stepStart = System.currentTimeMillis();
            Object rootObject = engine.deserialize(decompressedData);
            stepTime = System.currentTimeMillis() - stepStart;
            logger.info("Deserialization completed in " + stepTime + "ms");
            
            if (rootObject == null) {
                throw new IOException("Deserialization returned null object");
            }
            
            // Step 3: Convert to JSON and write to file
            logger.info("Step 3/4: Converting to JSON...");
            stepStart = System.currentTimeMillis();
            jsonWriter.writeToFile(rootObject, outputFile);
            stepTime = System.currentTimeMillis() - stepStart;
            logger.info("JSON conversion completed in " + stepTime + "ms");
            
            // Step 4: Generate business documents if enabled
            generateBusinessDocuments(rootObject, outputFile);
            
            long totalTime = System.currentTimeMillis() - startTime;
            logger.info(String.format("Successfully completed processing in %d ms. Output: %s", 
                totalTime, outputFile.getAbsolutePath()));
            
            // Log final memory statistics
            memoryManager.logMemoryInfo();
            memoryManager.logStatistics();
            
        } catch (OutOfMemoryError e) {
            memoryManager.logMemoryInfo();
            logger.severe("Out of memory. Try increasing heap size with -Xmx flag");
            logger.severe("Current max memory: " + memoryManager.getMaxMemoryMB() + "MB");
            logger.severe("Current used memory: " + memoryManager.getUsedMemoryMB() + "MB");
            throw new IOException("Out of memory during processing", e);
        } catch (ClassNotFoundException e) {
            logger.severe("Class not found during deserialization: " + e.getMessage());
            throw new IOException("Deserialization failed due to missing class: " + e.getMessage(), e);
        } catch (SecurityException e) {
            logger.severe("Security restriction during processing: " + e.getMessage());
            throw new IOException("Security restriction: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error during processing", e);
            throw new IOException("Failed to process file: " + e.getMessage(), e);
        } finally {
            // Clean up memory pools
            memoryManager.requestGCIfNeeded();
        }
    }
    
    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }
        
        try {
            // Load configuration
            DeserializerConfig config = DeserializerConfig.fromSystemProperties();
            
            // Create deserializer instance
            ChorusDeserializer deserializer = new ChorusDeserializer(config);
            
            // Parse command line arguments
            File inputFile = new File(args[0]);
            File outputFile = (args.length >= 2) 
                ? new File(args[1]) 
                : new File("deserialized_dump.json");
            
            // Validate input
            if (!inputFile.exists()) {
                System.err.println("Error: Input file not found: " + inputFile.getAbsolutePath());
                System.exit(1);
            }
            
            // Ensure output directory exists
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    System.err.println("Error: Could not create output directory: " + parentDir.getAbsolutePath());
                    System.exit(1);
                }
            }
            
            // Process the file
            deserializer.process(inputFile, outputFile);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void printUsage() {
        System.out.println("Usage: java -jar chorus-deserializer.jar <input.design> [output.json]");
        System.out.println("  <input.design>  Path to design file to process");
        System.out.println("  [output.json]   Optional output path (default: deserialized_dump.json)");
        System.out.println();
        System.out.println("Options (as system properties):");
        System.out.println("  -Dfocused.mode=true|false   Enable/disable whitelist filtering (default: true)");
        System.out.println("  -Dmemory.limit.mb=<value>   Set memory limit in MB");
        System.out.println("  -Dsafe.reflection=true|false Enable/disable safe reflection mode (default: true)");
        System.out.println("  -Dbatch.size=<value>        Set batch size for collections (default: 200)");
        System.out.println("  -Dmax.file.size.mb=<value>  Maximum file size in MB (default: 1024)");
        System.out.println("  -Dstrict.validation=true|false Enable/disable strict class validation (default: true)");
        System.out.println("  -Dbusiness.docs=true|false Enable/disable business document generation (default: false)");
        System.out.println("  -Dbusiness.formats=json,xml Comma-separated list of business document formats");
    }
    
    /**
     * Generate business documents from extracted metadata if enabled.
     */
    @SuppressWarnings("unchecked")
    private void generateBusinessDocuments(Object rootObject, File outputFile) {
        // Check if business document generation is enabled
        String businessDocsEnabled = System.getProperty("business.docs", "false");
        if (!"true".equalsIgnoreCase(businessDocsEnabled)) {
            return;
        }
        
        try {
            logger.info("Step 4/4: Generating business documents...");
            long stepStart = System.currentTimeMillis();
            
            // Convert root object to map for extraction
            Map<String, Object> dataMap = convertToMap(rootObject);
            
            // Extract business metadata
            BusinessMetadata businessMetadata = businessExtractor.extractFromDeserializedData(dataMap);
            
            // Determine output formats
            String formatsProperty = System.getProperty("business.formats", "json");
            Set<DocumentFormat> formats = parseDocumentFormats(formatsProperty);
            
            // Create business documents directory
            Path outputDir = Paths.get(outputFile.getParent(), "business_docs");
            
            // Generate business documents
            businessExtractor.generateBusinessDocuments(businessMetadata, outputDir, formats);
            
            long stepTime = System.currentTimeMillis() - stepStart;
            logger.info("Business document generation completed in " + stepTime + "ms");
            logger.info("Business documents written to: " + outputDir.toAbsolutePath());
            
        } catch (Exception e) {
            logger.warning("Failed to generate business documents: " + e.getMessage());
            // Don't fail the entire process for business document generation errors
        }
    }
    
    /**
     * Convert root object to Map for business metadata extraction.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToMap(Object rootObject) {
        if (rootObject instanceof Map) {
            return (Map<String, Object>) rootObject;
        }
        
        // For other object types, create a wrapper map
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("rootObject", rootObject);
        wrapper.put("objectType", rootObject.getClass().getSimpleName());
        return wrapper;
    }
    
    /**
     * Parse document formats from system property.
     */
    private Set<DocumentFormat> parseDocumentFormats(String formatsProperty) {
        Set<DocumentFormat> formats = new HashSet<>();
        
        if (formatsProperty == null || formatsProperty.trim().isEmpty()) {
            formats.add(DocumentFormat.CONSOLIDATED_JSON);
            return formats;
        }
        
        String[] formatNames = formatsProperty.split(",");
        for (String formatName : formatNames) {
            String trimmed = formatName.trim().toLowerCase();
            switch (trimmed) {
                case "json":
                    formats.add(DocumentFormat.CONSOLIDATED_JSON);
                    break;
                case "xml":
                    formats.add(DocumentFormat.CONSOLIDATED_XML);
                    break;
                case "bpmn-json":
                    formats.add(DocumentFormat.BPMN_JSON);
                    break;
                case "bpmn-xml":
                    formats.add(DocumentFormat.BPMN_XML);
                    break;
                case "fields-json":
                    formats.add(DocumentFormat.FIELD_MAPPING_JSON);
                    break;
                case "fields-xml":
                    formats.add(DocumentFormat.FIELD_MAPPING_XML);
                    break;
                case "services-json":
                    formats.add(DocumentFormat.SERVICE_REGISTRY_JSON);
                    break;
                case "services-xml":
                    formats.add(DocumentFormat.SERVICE_REGISTRY_XML);
                    break;
                default:
                    logger.warning("Unknown business document format: " + formatName);
            }
        }
        
        // Default to consolidated JSON if no valid formats found
        if (formats.isEmpty()) {
            formats.add(DocumentFormat.CONSOLIDATED_JSON);
        }
        
        return formats;
    }
}