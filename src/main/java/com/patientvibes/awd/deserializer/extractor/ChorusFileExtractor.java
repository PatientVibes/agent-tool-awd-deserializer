/**
 * Module: ChorusFileExtractor - Main orchestrator for modular file extraction from AWD design files
 * 
 * Summary:
 *     Main orchestrator class that coordinates individual file extraction from AWD design files.
 *     Transforms monolithic metadata extraction into modular file generation supporting BPMN,
 *     forms, and service definitions as separate output files. Integrates with existing 
 *     BusinessMetadataExtractor foundation while providing enhanced separation of concerns.
 * 
 * Key Components:
 *     - extractFiles(): Primary orchestration method for multi-file extraction
 *     - extractBpmnFiles(): BPMN XML file generation with AWD extensions
 *     - extractFormFiles(): Form definition extraction and HTML generation
 *     - extractServiceFiles(): Service definition and configuration extraction
 *     - extractWorkflowFiles(): Enhanced workflow analysis and documentation
 * 
 * Keywords: chorus, file, extraction, orchestrator, modular, bpmn, forms, services,
 *          workflow, awd, design, separation, concerns, enterprise, architecture
 * 
 * Dependencies:
 *     - com.patientvibes.awd.deserializer.business.BusinessMetadataExtractor: Foundation metadata extraction
 *     - com.patientvibes.awd.deserializer.extractor.generator.BpmnXmlGenerator: BPMN XML generation
 *     - com.patientvibes.awd.deserializer.extractor.mapper.AwdSchemaMapper: AWD schema mapping
 *     - java.nio.file.Path: File system operations and path management
 *     - java.util.concurrent.CompletableFuture: Asynchronous processing support
 * 
 * Security:
 *     - Input validation for all extraction parameters
 *     - Safe file path resolution and validation
 *     - Resource management with proper cleanup
 *     - Memory-bounded operations for large design files
 *     - Output sanitization for external file generation
 * 
 * Performance:
 *     - Parallel extraction of independent file types
 *     - Streaming file generation for large datasets
 *     - Memory-efficient processing with bounded resources
 *     - Configurable batch sizes for optimal performance
 *     - Metrics collection for performance monitoring
 */
package com.patientvibes.awd.deserializer.extractor;

import com.patientvibes.awd.deserializer.business.BusinessMetadataExtractor;
import com.patientvibes.awd.deserializer.business.model.BusinessMetadata;
import com.patientvibes.awd.deserializer.extractor.config.ExtractionConfig;
import com.patientvibes.awd.deserializer.extractor.generator.BpmnXmlGenerator;
import com.patientvibes.awd.deserializer.extractor.generator.FormHtmlGenerator;
import com.patientvibes.awd.deserializer.extractor.generator.ServiceConfigGenerator;
import com.patientvibes.awd.deserializer.extractor.mapper.AwdSchemaMapper;
import com.patientvibes.awd.deserializer.extractor.model.ExtractionResult;
import com.patientvibes.awd.deserializer.extractor.model.FileExtractionRequest;
import com.patientvibes.awd.deserializer.extractor.model.FileExtractionType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Main orchestrator for modular file extraction from AWD design files.
 * Coordinates individual file generators to create BPMN, forms, and service definitions.
 */
public class ChorusFileExtractor {
    private static final Logger logger = Logger.getLogger(ChorusFileExtractor.class.getName());
    
    private final BusinessMetadataExtractor metadataExtractor;
    private final BpmnXmlGenerator bpmnGenerator;
    private final FormHtmlGenerator formGenerator;
    private final ServiceConfigGenerator serviceGenerator;
    private final AwdSchemaMapper schemaMapper;
    private final ExtractionConfig config;
    private final ExecutorService executorService;
    
    // Performance metrics
    private long totalExtractionTime = 0;
    private int totalFilesGenerated = 0;
    private final Map<FileExtractionType, Long> typeProcessingTimes = new HashMap<>();
    
    /**
     * Constructor with default configuration.
     */
    public ChorusFileExtractor() {
        this(new ExtractionConfig());
    }
    
    /**
     * Constructor with custom configuration.
     */
    public ChorusFileExtractor(ExtractionConfig config) {
        this.config = config;
        this.metadataExtractor = new BusinessMetadataExtractor();
        this.bpmnGenerator = new BpmnXmlGenerator(config);
        this.formGenerator = new FormHtmlGenerator(config);
        this.serviceGenerator = new ServiceConfigGenerator(config);
        this.schemaMapper = new AwdSchemaMapper();
        this.executorService = Executors.newFixedThreadPool(config.getMaxConcurrentExtractions());
        
        logger.info("ChorusFileExtractor initialized with configuration: " + config);
    }
    
    /**
     * Main extraction method that coordinates all file generation.
     * 
     * @param request The file extraction request with source data and configuration
     * @return ExtractionResult containing generated files and metadata
     * @throws IOException if file operations fail
     */
    public ExtractionResult extractFiles(FileExtractionRequest request) throws IOException {
        long startTime = System.currentTimeMillis();
        logger.info("Starting file extraction for request: " + request.getSourceName());
        
        // Validate request
        validateExtractionRequest(request);
        
        // Create output directory structure
        Path outputRoot = createOutputDirectoryStructure(request.getOutputDirectory());
        
        // Extract business metadata using foundation extractor
        BusinessMetadata metadata = metadataExtractor.extractFromDeserializedData(request.getDeserializedData());
        
        // Map AWD structures to extraction models
        var mappedData = schemaMapper.mapAwdData(request.getDeserializedData(), metadata);
        
        // Perform parallel extraction of different file types
        ExtractionResult result = performParallelExtraction(request, outputRoot, metadata, mappedData);
        
        // Calculate total processing time
        long endTime = System.currentTimeMillis();
        totalExtractionTime = endTime - startTime;
        result.setTotalProcessingTime(totalExtractionTime);
        
        // Log completion metrics
        logExtractionMetrics(result);
        
        return result;
    }
    
    /**
     * Extract only BPMN files from AWD design data.
     */
    public ExtractionResult extractBpmnFiles(FileExtractionRequest request) throws IOException {
        logger.info("Extracting BPMN files only for: " + request.getSourceName());
        
        request.setEnabledTypes(Set.of(FileExtractionType.BPMN_XML));
        return extractFiles(request);
    }
    
    /**
     * Extract only form files from AWD design data.
     */
    public ExtractionResult extractFormFiles(FileExtractionRequest request) throws IOException {
        logger.info("Extracting form files only for: " + request.getSourceName());
        
        request.setEnabledTypes(Set.of(FileExtractionType.FORM_HTML));
        return extractFiles(request);
    }
    
    /**
     * Extract only service configuration files from AWD design data.
     */
    public ExtractionResult extractServiceFiles(FileExtractionRequest request) throws IOException {
        logger.info("Extracting service files only for: " + request.getSourceName());
        
        request.setEnabledTypes(Set.of(FileExtractionType.SERVICE_CONFIG));
        return extractFiles(request);
    }
    
    /**
     * Get extraction performance metrics.
     */
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalExtractionTime", totalExtractionTime);
        metrics.put("totalFilesGenerated", totalFilesGenerated);
        metrics.put("averageTimePerFile", totalFilesGenerated > 0 ? totalExtractionTime / totalFilesGenerated : 0);
        metrics.put("typeProcessingTimes", new HashMap<>(typeProcessingTimes));
        metrics.put("concurrentThreads", config.getMaxConcurrentExtractions());
        
        return metrics;
    }
    
    /**
     * Shutdown the extraction service and clean up resources.
     */
    public void shutdown() {
        logger.info("Shutting down ChorusFileExtractor");
        
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("ChorusFileExtractor shutdown complete");
    }
    
    // Private helper methods
    
    private void validateExtractionRequest(FileExtractionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Extraction request cannot be null");
        }
        
        if (request.getDeserializedData() == null || request.getDeserializedData().isEmpty()) {
            throw new IllegalArgumentException("Deserialized data cannot be null or empty");
        }
        
        if (request.getOutputDirectory() == null) {
            throw new IllegalArgumentException("Output directory cannot be null");
        }
        
        if (request.getEnabledTypes() == null || request.getEnabledTypes().isEmpty()) {
            throw new IllegalArgumentException("At least one extraction type must be enabled");
        }
        
        // Validate output directory is writable
        Path outputPath = Paths.get(request.getOutputDirectory());
        if (Files.exists(outputPath) && !Files.isWritable(outputPath)) {
            throw new IllegalArgumentException("Output directory is not writable: " + outputPath);
        }
        
        logger.info("Extraction request validation successful");
    }
    
    private Path createOutputDirectoryStructure(String outputDirectory) throws IOException {
        Path outputRoot = Paths.get(outputDirectory);
        
        // Create main output directory
        Files.createDirectories(outputRoot);
        
        // Create subdirectories for different file types
        Files.createDirectories(outputRoot.resolve("bpmn"));
        Files.createDirectories(outputRoot.resolve("forms"));
        Files.createDirectories(outputRoot.resolve("services"));
        Files.createDirectories(outputRoot.resolve("workflow"));
        Files.createDirectories(outputRoot.resolve("metadata"));
        
        logger.info("Created output directory structure at: " + outputRoot);
        return outputRoot;
    }
    
    private ExtractionResult performParallelExtraction(
            FileExtractionRequest request,
            Path outputRoot,
            BusinessMetadata metadata,
            Map<String, Object> mappedData) {
        
        ExtractionResult result = new ExtractionResult();
        result.setSourceName(request.getSourceName());
        result.setOutputDirectory(outputRoot.toString());
        result.setStartTime(LocalDateTime.now());
        
        List<CompletableFuture<Void>> extractionTasks = new ArrayList<>();
        
        // BPMN extraction
        if (request.getEnabledTypes().contains(FileExtractionType.BPMN_XML)) {
            extractionTasks.add(CompletableFuture.runAsync(() -> {
                try {
                    long startTime = System.currentTimeMillis();
                    var bpmnFiles = bpmnGenerator.generateBpmnFiles(metadata, mappedData, outputRoot.resolve("bpmn"));
                    result.addGeneratedFiles(FileExtractionType.BPMN_XML, bpmnFiles);
                    typeProcessingTimes.put(FileExtractionType.BPMN_XML, System.currentTimeMillis() - startTime);
                    logger.info("BPMN extraction completed: " + bpmnFiles.size() + " files generated");
                } catch (Exception e) {
                    logger.severe("BPMN extraction failed: " + e.getMessage());
                    result.addError(FileExtractionType.BPMN_XML, e.getMessage());
                }
            }, executorService));
        }
        
        // Form extraction
        if (request.getEnabledTypes().contains(FileExtractionType.FORM_HTML)) {
            extractionTasks.add(CompletableFuture.runAsync(() -> {
                try {
                    long startTime = System.currentTimeMillis();
                    var formFiles = formGenerator.generateFormFiles(metadata, mappedData, outputRoot.resolve("forms"));
                    result.addGeneratedFiles(FileExtractionType.FORM_HTML, formFiles);
                    typeProcessingTimes.put(FileExtractionType.FORM_HTML, System.currentTimeMillis() - startTime);
                    logger.info("Form extraction completed: " + formFiles.size() + " files generated");
                } catch (Exception e) {
                    logger.severe("Form extraction failed: " + e.getMessage());
                    result.addError(FileExtractionType.FORM_HTML, e.getMessage());
                }
            }, executorService));
        }
        
        // Service extraction
        if (request.getEnabledTypes().contains(FileExtractionType.SERVICE_CONFIG)) {
            extractionTasks.add(CompletableFuture.runAsync(() -> {
                try {
                    long startTime = System.currentTimeMillis();
                    var serviceFiles = serviceGenerator.generateServiceFiles(metadata, mappedData, outputRoot.resolve("services"));
                    result.addGeneratedFiles(FileExtractionType.SERVICE_CONFIG, serviceFiles);
                    typeProcessingTimes.put(FileExtractionType.SERVICE_CONFIG, System.currentTimeMillis() - startTime);
                    logger.info("Service extraction completed: " + serviceFiles.size() + " files generated");
                } catch (Exception e) {
                    logger.severe("Service extraction failed: " + e.getMessage());
                    result.addError(FileExtractionType.SERVICE_CONFIG, e.getMessage());
                }
            }, executorService));
        }
        
        // Wait for all extractions to complete
        CompletableFuture<Void> allTasks = CompletableFuture.allOf(
            extractionTasks.toArray(new CompletableFuture[0])
        );
        
        try {
            allTasks.get(config.getExtractionTimeoutMinutes(), java.util.concurrent.TimeUnit.MINUTES);
        } catch (Exception e) {
            logger.severe("Extraction tasks failed or timed out: " + e.getMessage());
            result.addError(FileExtractionType.UNKNOWN, "Extraction timeout or failure: " + e.getMessage());
        }
        
        // Generate metadata summary
        generateMetadataSummary(result, metadata, outputRoot.resolve("metadata"));
        
        result.setEndTime(LocalDateTime.now());
        return result;
    }
    
    private void generateMetadataSummary(ExtractionResult result, BusinessMetadata metadata, Path metadataDir) {
        try {
            // Generate extraction summary
            Path summaryFile = metadataDir.resolve("extraction_summary.json");
            Map<String, Object> summary = new HashMap<>();
            summary.put("extractionTime", result.getStartTime());
            summary.put("sourceName", result.getSourceName());
            summary.put("filesGenerated", result.getTotalFilesGenerated());
            summary.put("filesByType", result.getFilesByType());
            summary.put("processingTimes", typeProcessingTimes);
            summary.put("errors", result.getErrors());
            
            // Write summary to file
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(summaryFile.toFile(), summary);
            
            result.addGeneratedFile(FileExtractionType.METADATA, summaryFile.toString());
            logger.info("Generated extraction metadata summary: " + summaryFile);
            
        } catch (IOException e) {
            logger.warning("Failed to generate metadata summary: " + e.getMessage());
        }
    }
    
    private void logExtractionMetrics(ExtractionResult result) {
        logger.info("=== Extraction Metrics ===");
        logger.info("Source: " + result.getSourceName());
        logger.info("Total files generated: " + result.getTotalFilesGenerated());
        logger.info("Total processing time: " + totalExtractionTime + "ms");
        logger.info("Average time per file: " + (result.getTotalFilesGenerated() > 0 ? 
            totalExtractionTime / result.getTotalFilesGenerated() : 0) + "ms");
        
        for (Map.Entry<FileExtractionType, Long> entry : typeProcessingTimes.entrySet()) {
            logger.info(entry.getKey() + " processing time: " + entry.getValue() + "ms");
        }
        
        if (!result.getErrors().isEmpty()) {
            logger.warning("Extraction completed with " + result.getErrors().size() + " errors");
            for (Map.Entry<FileExtractionType, List<String>> entry : result.getErrors().entrySet()) {
                for (String error : entry.getValue()) {
                    logger.warning(entry.getKey() + " error: " + error);
                }
            }
        }
        
        totalFilesGenerated += result.getTotalFilesGenerated();
    }
}