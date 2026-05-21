/**
 * Module: SimplifiedChorusIntegrationTest - Essential integration testing for Chorus components
 * 
 * Summary:
 *     Simplified integration testing focused on the core Chorus file extraction components
 *     that actually exist and can be tested. Tests the integration between ChorusFileExtractor,
 *     BusinessMetadataExtractor, and the extractor framework without requiring components
 *     that haven't been implemented yet.
 * 
 * Key Components:
 *     - testChorusFileExtractorBasicIntegration(): Core extractor functionality
 *     - testFileExtractionRequestBuilder(): Request builder pattern validation
 *     - testExtractionResultHandling(): Result processing and validation
 *     - testConfigurationManagement(): Configuration validation and builder patterns
 *     - testErrorHandlingAndValidation(): Error scenarios and edge cases
 * 
 * Keywords: integration, test, chorus, extraction, simplified, basic, validation,
 *          configuration, builder, result, error, handling, framework, components
 * 
 * Dependencies:
 *     - com.patientvibes.awd.deserializer.extractor.ChorusFileExtractor: Main orchestrator
 *     - com.patientvibes.awd.deserializer.extractor.config.ExtractionConfig: Configuration management
 *     - com.patientvibes.awd.deserializer.extractor.model.*: Request and result models
 *     - com.patientvibes.awd.deserializer.business.BusinessMetadataExtractor: Metadata processing
 * 
 * Security:
 *     - Input validation testing for all components
 *     - Configuration parameter validation
 *     - Error handling security verification
 * 
 * Performance:
 *     - Basic performance validation
 *     - Memory usage monitoring
 *     - Configuration impact assessment
 */
package com.patientvibes.awd.deserializer.integration;

import com.patientvibes.awd.deserializer.extractor.ChorusFileExtractor;
import com.patientvibes.awd.deserializer.extractor.config.ExtractionConfig;
import com.patientvibes.awd.deserializer.extractor.model.ExtractionResult;
import com.patientvibes.awd.deserializer.extractor.model.FileExtractionRequest;
import com.patientvibes.awd.deserializer.extractor.model.FileExtractionType;
import com.patientvibes.awd.deserializer.business.BusinessMetadataExtractor;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simplified integration test suite focusing on existing Chorus components.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SimplifiedChorusIntegrationTest {

    private static final Logger logger = Logger.getLogger(SimplifiedChorusIntegrationTest.class.getName());
    
    // Test output directories
    private static final String TEST_OUTPUT_BASE = "target/test-simplified-integration";
    
    // Test configuration constants
    private static final int TEST_TIMEOUT_SECONDS = 30;
    private static final long MAX_MEMORY_USAGE_MB = 512;
    
    private ChorusFileExtractor extractor;
    private BusinessMetadataExtractor metadataExtractor;
    private Path testOutputRoot;
    
    // Test metrics tracking
    private final Map<String, Long> testMetrics = new HashMap<>();

    @BeforeAll
    static void setUpClass() throws IOException {
        logger.info("=== Starting Simplified Chorus Integration Test Suite ===");
        
        // Ensure test output directory exists
        Path outputPath = Paths.get(TEST_OUTPUT_BASE);
        if (Files.exists(outputPath)) {
            // Clean existing output
            Files.walk(outputPath)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(file -> file.delete());
        }
        Files.createDirectories(outputPath);
        
        logger.info("Test environment prepared at: " + outputPath);
    }

    @BeforeEach
    void setUp() throws IOException {
        logger.info("Setting up test environment for simplified Chorus testing");
        
        // Configure for testing
        ExtractionConfig extractionConfig = ExtractionConfig.builder()
            .maxConcurrentExtractions(2)
            .extractionTimeoutMinutes(3)
            .enableAwdExtensions(true)
            .enableDationCompatibility(true)
            .validateOutput(true)
            .build();
        
        extractor = new ChorusFileExtractor(extractionConfig);
        metadataExtractor = new BusinessMetadataExtractor();
        
        // Create unique test output directory
        testOutputRoot = Paths.get(TEST_OUTPUT_BASE, "test-" + System.currentTimeMillis());
        Files.createDirectories(testOutputRoot);
        
        logger.info("Test setup completed for output: " + testOutputRoot);
    }

    @AfterEach
    void tearDown() {
        logger.info("Cleaning up test resources");
        
        if (extractor != null) {
            extractor.shutdown();
        }
        
        // Log test metrics
        if (!testMetrics.isEmpty()) {
            logger.info("Test metrics: " + testMetrics);
        }
        
        testMetrics.clear();
    }

    @Test
    @Order(1)
    @DisplayName("Test ChorusFileExtractor basic initialization")
    void testChorusFileExtractorInitialization() {
        logger.info("=== Testing ChorusFileExtractor Initialization ===");
        
        // Test default constructor
        ChorusFileExtractor defaultExtractor = new ChorusFileExtractor();
        assertNotNull(defaultExtractor, "Default constructor should create extractor");
        
        // Test configured constructor
        ExtractionConfig config = new ExtractionConfig();
        ChorusFileExtractor configuredExtractor = new ChorusFileExtractor(config);
        assertNotNull(configuredExtractor, "Configured constructor should create extractor");
        
        // Test performance metrics access
        Map<String, Object> metrics = extractor.getPerformanceMetrics();
        assertNotNull(metrics, "Performance metrics should be accessible");
        assertTrue(metrics.containsKey("totalExtractionTime"), "Should contain extraction time metric");
        assertTrue(metrics.containsKey("totalFilesGenerated"), "Should contain files generated metric");
        
        // Cleanup
        defaultExtractor.shutdown();
        configuredExtractor.shutdown();
        
        logger.info("ChorusFileExtractor initialization test completed");
    }

    @Test
    @Order(2)
    @DisplayName("Test FileExtractionRequest builder pattern")
    void testFileExtractionRequestBuilder() {
        logger.info("=== Testing FileExtractionRequest Builder Pattern ===");
        
        // Create sample data
        Map<String, Object> sampleData = new HashMap<>();
        sampleData.put("metadata", Map.of("source", "test"));
        sampleData.put("awdStructure", Map.of("objects", List.of()));
        
        // Test basic builder usage
        FileExtractionRequest request = FileExtractionRequest.builder()
            .sourceName("Test_Source")
            .outputDirectory(testOutputRoot.toString())
            .deserializedData(sampleData)
            .enabledTypes(Set.of(FileExtractionType.BPMN_XML))
            .dationFormat(true)
            .includeMetadata(true)
            .maxConcurrent(2)
            .build();
        
        // Validate request properties
        assertNotNull(request, "Builder should create request");
        assertEquals("Test_Source", request.getSourceName());
        assertEquals(testOutputRoot.toString(), request.getOutputDirectory());
        assertNotNull(request.getDeserializedData());
        assertEquals(sampleData, request.getDeserializedData());
        assertTrue(request.getEnabledTypes().contains(FileExtractionType.BPMN_XML));
        
        // Test processing options
        assertEquals(true, request.getProcessingOption("dationFormat"));
        assertEquals(true, request.getProcessingOption("includeMetadata"));
        assertEquals(2, request.getProcessingOption("maxConcurrent"));
        
        logger.info("FileExtractionRequest builder test completed: " + request);
    }

    @Test
    @Order(3)
    @DisplayName("Test ExtractionConfig builder pattern")
    void testExtractionConfigBuilder() {
        logger.info("=== Testing ExtractionConfig Builder Pattern ===");
        
        // Test builder with various configurations
        ExtractionConfig config = ExtractionConfig.builder()
            .maxConcurrentExtractions(3)
            .extractionTimeoutMinutes(5)
            .maxMemoryUsageMB(1024)
            .enableBpmnGeneration(true)
            .enableFormExtraction(true)
            .enableServiceExtraction(false)
            .enableAwdExtensions(true)
            .enableDationCompatibility(true)
            .validateOutput(true)
            .prettyPrintXml(true)
            .customOption("testOption", "testValue")
            .build();
        
        // Validate configuration properties
        assertNotNull(config, "Builder should create config");
        assertEquals(3, config.getMaxConcurrentExtractions());
        assertEquals(5, config.getExtractionTimeoutMinutes());
        assertEquals(1024, config.getMaxMemoryUsageMB());
        assertTrue(config.isIncludeAwdExtensions());
        assertTrue(config.isValidateOutput());
        assertTrue(config.isPrettyPrintXml());
        
        // Test custom options
        assertEquals("testValue", config.getCustomOption("testOption"));
        assertEquals(true, config.getCustomOption("enableBpmnGeneration"));
        assertEquals(true, config.getCustomOption("enableDationCompatibility"));
        
        logger.info("ExtractionConfig builder test completed: " + config);
    }

    @Test
    @Order(4)
    @DisplayName("Test extraction with mock data")
    void testExtractionWithMockData() throws IOException {
        logger.info("=== Testing Extraction with Mock Data ===");
        
        // Create comprehensive mock data simulating a deserialized AWD file
        Map<String, Object> mockData = createMockAwdData();
        
        // Create extraction request
        FileExtractionRequest request = FileExtractionRequest.builder()
            .sourceName("Mock_AWD_Test")
            .outputDirectory(testOutputRoot.toString())
            .deserializedData(mockData)
            .enabledTypes(Set.of(FileExtractionType.BPMN_XML, FileExtractionType.FORM_HTML))
            .dationFormat(true)
            .includeMetadata(true)
            .build();
        
        // Record performance metrics
        Instant startTime = Instant.now();
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        // Perform extraction
        ExtractionResult result = extractor.extractFiles(request);
        
        Instant endTime = Instant.now();
        long processingTime = Duration.between(startTime, endTime).toMillis();
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = (memoryAfter - memoryBefore) / (1024 * 1024); // MB
        
        // Record metrics
        testMetrics.put("mockDataProcessingTime", processingTime);
        testMetrics.put("mockDataMemoryUsed", memoryUsed);
        
        // Validate results
        assertNotNull(result, "Extraction should produce results");
        assertEquals("Mock_AWD_Test", result.getSourceName());
        assertEquals(testOutputRoot.toString(), result.getOutputDirectory());
        assertNotNull(result.getStartTime());
        assertNotNull(result.getEndTime());
        
        // Validate no critical errors occurred
        assertFalse(result.hasErrors(), "Should not have critical errors with mock data");
        
        // Performance validation
        assertTrue(processingTime < TEST_TIMEOUT_SECONDS * 1000, 
            "Processing should complete within timeout");
        assertTrue(memoryUsed < MAX_MEMORY_USAGE_MB, 
            "Memory usage should be reasonable: " + memoryUsed + "MB");
        
        logger.info("Mock data extraction completed: " + result.getTotalFilesGenerated() + 
                   " files in " + processingTime + "ms");
    }

    @Test
    @Order(5)
    @DisplayName("Test extraction error handling")
    void testExtractionErrorHandling() {
        logger.info("=== Testing Extraction Error Handling ===");
        
        // Test with null data
        assertThrows(IllegalStateException.class, () -> {
            FileExtractionRequest.builder()
                .sourceName("Error_Test")
                .outputDirectory(testOutputRoot.toString())
                .deserializedData(null)
                .enabledTypes(Set.of(FileExtractionType.BPMN_XML))
                .build();
        }, "Should reject null deserialized data");
        
        // Test with null output directory
        assertThrows(IllegalStateException.class, () -> {
            FileExtractionRequest.builder()
                .sourceName("Error_Test")
                .outputDirectory(null)
                .deserializedData(Map.of("test", "data"))
                .enabledTypes(Set.of(FileExtractionType.BPMN_XML))
                .build();
        }, "Should reject null output directory");
        
        // Test with no enabled types
        assertThrows(IllegalStateException.class, () -> {
            FileExtractionRequest.builder()
                .sourceName("Error_Test")
                .outputDirectory(testOutputRoot.toString())
                .deserializedData(Map.of("test", "data"))
                .enabledTypes(Set.of())
                .build();
        }, "Should reject empty enabled types");
        
        logger.info("Error handling tests completed");
    }

    @Test
    @Order(6)
    @DisplayName("Test ExtractionResult functionality")
    void testExtractionResultFunctionality() {
        logger.info("=== Testing ExtractionResult Functionality ===");
        
        // Create and populate extraction result
        ExtractionResult result = new ExtractionResult();
        result.setSourceName("Test_Result");
        result.setOutputDirectory(testOutputRoot.toString());
        result.setStartTime(java.time.LocalDateTime.now().minusMinutes(1));
        result.setEndTime(java.time.LocalDateTime.now());
        result.setTotalProcessingTime(60000); // 1 minute
        
        // Add generated files
        result.addGeneratedFile(FileExtractionType.BPMN_XML, "/test/path/file1.bpmn");
        result.addGeneratedFile(FileExtractionType.BPMN_XML, "/test/path/file2.bpmn");
        result.addGeneratedFiles(FileExtractionType.FORM_HTML, 
            List.of("/test/path/form1.html", "/test/path/form2.html"));
        
        // Add errors and warnings
        result.addError(FileExtractionType.SERVICE_CONFIG, "Test error message");
        result.addWarning(FileExtractionType.FORM_HTML, "Test warning message");
        
        // Set processing times
        result.setProcessingTime(FileExtractionType.BPMN_XML, 30000);
        result.setProcessingTime(FileExtractionType.FORM_HTML, 20000);
        
        // Validate result state
        assertEquals("Test_Result", result.getSourceName());
        assertEquals(4, result.getTotalFilesGenerated());
        assertTrue(result.hasErrors());
        assertTrue(result.hasWarnings());
        assertFalse(result.isSuccessful()); // Should be false due to errors
        
        // Validate file counts by type
        assertEquals(2, result.getFileCount(FileExtractionType.BPMN_XML));
        assertEquals(2, result.getFileCount(FileExtractionType.FORM_HTML));
        assertEquals(0, result.getFileCount(FileExtractionType.SERVICE_CONFIG));
        
        // Validate processing times
        assertEquals(30000, result.getProcessingTime(FileExtractionType.BPMN_XML));
        assertEquals(20000, result.getProcessingTime(FileExtractionType.FORM_HTML));
        
        // Test summary generation
        Map<String, Object> summary = result.getSummary();
        assertNotNull(summary);
        assertTrue(summary.containsKey("sourceName"));
        assertTrue(summary.containsKey("totalFilesGenerated"));
        assertTrue(summary.containsKey("hasErrors"));
        assertTrue(summary.containsKey("hasWarnings"));
        
        logger.info("ExtractionResult functionality test completed: " + result);
    }

    @Test
    @Order(7)
    @DisplayName("Test business metadata extractor integration")
    void testBusinessMetadataExtractorIntegration() {
        logger.info("=== Testing Business Metadata Extractor Integration ===");
        
        // Create mock AWD data with business metadata
        Map<String, Object> awdData = createMockAwdDataWithBusinessMetadata();
        
        // Extract business metadata
        Instant startTime = Instant.now();
        var businessMetadata = metadataExtractor.extractFromDeserializedData(awdData);
        Instant endTime = Instant.now();
        
        long extractionTime = Duration.between(startTime, endTime).toMillis();
        testMetrics.put("businessMetadataExtractionTime", extractionTime);
        
        // Validate extracted metadata
        assertNotNull(businessMetadata, "Business metadata should be extracted");
        
        // Log results
        logger.info("Business metadata extraction completed in " + extractionTime + "ms");
        logger.info("Extracted metadata: " + businessMetadata.toString());
        
        if (businessMetadata.getBusinessProcesses() != null) {
            logger.info("BPMN processes found: " + businessMetadata.getBusinessProcesses().size());
        }
        
        if (businessMetadata.getFieldMappings() != null) {
            logger.info("Field mappings found: " + businessMetadata.getFieldMappings().size());
        }
        
        // Performance validation
        assertTrue(extractionTime < 5000, "Metadata extraction should be fast");
    }

    // Helper methods for creating test data

    private Map<String, Object> createMockAwdData() {
        Map<String, Object> mockData = new HashMap<>();
        
        // Mock metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fileSize", 1024L);
        metadata.put("timestamp", System.currentTimeMillis());
        metadata.put("version", "2.4.0");
        metadata.put("processingMode", "jar-free");
        mockData.put("metadata", metadata);
        
        // Mock AWD structure
        Map<String, Object> awdStructure = new HashMap<>();
        List<Map<String, Object>> objects = new ArrayList<>();
        
        // Mock AWD object 1
        Map<String, Object> object1 = new HashMap<>();
        object1.put("className", "com.dstawd.design.model.DeploymentPackage");
        object1.put("fieldCount", 5);
        object1.put("fields", Map.of(
            "processName", "MockProcess",
            "version", "1.0",
            "status", "active"
        ));
        objects.add(object1);
        
        // Mock AWD object 2
        Map<String, Object> object2 = new HashMap<>();
        object2.put("className", "com.dstawd.design.model.WorkObject");
        object2.put("fieldCount", 3);
        object2.put("fields", Map.of(
            "workType", "form",
            "formName", "CustomerEntry"
        ));
        objects.add(object2);
        
        awdStructure.put("objects", objects);
        awdStructure.put("totalObjects", objects.size());
        mockData.put("awdStructure", awdStructure);
        
        return mockData;
    }

    private Map<String, Object> createMockAwdDataWithBusinessMetadata() {
        Map<String, Object> mockData = createMockAwdData();
        
        // Add business metadata fields
        Map<String, Object> businessFields = new HashMap<>();
        businessFields.put("processDefinitions", List.of(
            Map.of("name", "CustomerOnboarding", "type", "BPMN"),
            Map.of("name", "OrderProcessing", "type", "BPMN")
        ));
        businessFields.put("formDefinitions", List.of(
            Map.of("name", "CustomerForm", "fields", 10),
            Map.of("name", "OrderForm", "fields", 8)
        ));
        businessFields.put("serviceDefinitions", List.of(
            Map.of("name", "ValidationService", "operations", 3)
        ));
        
        mockData.put("businessMetadataFields", List.of(businessFields));
        
        return mockData;
    }

    @AfterAll
    static void tearDownClass() {
        logger.info("=== Simplified Chorus Integration Test Suite Complete ===");
        
        // Optional: Clean up test output directory
        // Uncomment if you want to clean up after tests
        /*
        try {
            Path outputPath = Paths.get(TEST_OUTPUT_BASE);
            if (Files.exists(outputPath)) {
                Files.walk(outputPath)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
        } catch (IOException e) {
            logger.warning("Failed to clean up test output: " + e.getMessage());
        }
        */
    }
}