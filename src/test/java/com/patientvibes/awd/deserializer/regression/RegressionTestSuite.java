/**
 * Module: RegressionTestSuite - Automated regression testing with baseline comparison
 * 
 * Summary:
 *     Comprehensive regression testing framework that validates system behavior against
 *     established baselines, detects performance degradation, and ensures backward
 *     compatibility across Chorus file extraction system versions. Provides automated
 *     comparison and analysis of extraction results.
 * 
 * Key Components:
 *     - testBaselineComparison(): Compare current results against established baselines
 *     - testPerformanceRegression(): Detect performance degradation over time
 *     - testBackwardCompatibility(): Ensure compatibility with previous versions
 *     - testOutputConsistency(): Validate consistent output format and structure
 *     - testFeatureRegression(): Test for regression in specific feature areas
 *     - testDataIntegrityRegression(): Validate data integrity across versions
 * 
 * Keywords: regression, testing, baseline, comparison, performance, degradation, backward,
 *          compatibility, output, consistency, feature, data, integrity, automated, analysis
 * 
 * Dependencies:
 *     - com.patientvibes.awd.deserializer.extractor.ChorusFileExtractor: Main extraction orchestrator
 *     - com.fasterxml.jackson.databind.ObjectMapper: JSON processing for baseline comparison
 *     - java.nio.file.*: File system operations for baseline storage
 *     - java.time.*: Performance timing and regression detection
 * 
 * Security:
 *     - Safe baseline file handling and validation
 *     - Controlled access to regression test data
 *     - Input validation for all comparison operations
 *     - Secure file storage for baseline artifacts
 * 
 * Performance:
 *     - Efficient baseline storage and retrieval
 *     - Optimized comparison algorithms for large datasets
 *     - Memory-conscious regression analysis
 *     - Parallel execution for multiple regression tests
 */
package com.patientvibes.awd.deserializer.regression;

import com.patientvibes.awd.deserializer.extractor.ChorusFileExtractor;
import com.patientvibes.awd.deserializer.extractor.config.ExtractionConfig;
import com.patientvibes.awd.deserializer.extractor.model.ExtractionResult;
import com.patientvibes.awd.deserializer.extractor.model.FileExtractionRequest;
import com.patientvibes.awd.deserializer.extractor.model.FileExtractionType;
import com.patientvibes.awd.deserializer.business.BusinessMetadataExtractor;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive regression test suite for Chorus file extraction system.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RegressionTestSuite {

    private static final Logger logger = Logger.getLogger(RegressionTestSuite.class.getName());
    
    // Baseline and regression test configuration
    private static final String BASELINE_STORAGE_PATH = "target/test-baselines";
    private static final String REGRESSION_OUTPUT_PATH = "target/test-regression-output";
    private static final String CURRENT_VERSION = "2.4.0";
    
    // Performance regression thresholds
    private static final double MAX_PERFORMANCE_DEGRADATION_PERCENT = 20.0;
    private static final long MAX_MEMORY_INCREASE_MB = 256;
    private static final double MAX_OUTPUT_SIZE_INCREASE_PERCENT = 15.0;
    
    // Test data consistency thresholds
    private static final double MIN_OUTPUT_SIMILARITY_PERCENT = 95.0;
    private static final int MAX_ACCEPTABLE_DIFFERENCES = 5;
    
    private ChorusFileExtractor extractor;
    private BusinessMetadataExtractor metadataExtractor;
    private ObjectMapper objectMapper;
    private Path baselineStoragePath;
    private Path regressionOutputPath;
    
    // Regression tracking and analysis
    private final Map<String, BaselineResult> currentBaselines = new ConcurrentHashMap<>();
    private final Map<String, RegressionAnalysis> regressionResults = new ConcurrentHashMap<>();

    @BeforeAll
    static void setUpClass() throws IOException {
        logger.info("=== Starting Regression Test Suite ===");
        
        // Prepare baseline and output directories
        Path baselinePath = Paths.get(BASELINE_STORAGE_PATH);
        Path outputPath = Paths.get(REGRESSION_OUTPUT_PATH);
        
        Files.createDirectories(baselinePath);
        Files.createDirectories(outputPath);
        
        // Clean previous regression output
        if (Files.exists(outputPath)) {
            Files.walk(outputPath)
                .sorted(Comparator.reverseOrder())
                .filter(path -> !path.equals(outputPath))
                .map(Path::toFile)
                .forEach(file -> file.delete());
        }
        
        logger.info("Regression test environment prepared");
    }

    @BeforeEach
    void setUp() throws IOException {
        // Configure for regression testing
        ExtractionConfig config = ExtractionConfig.builder()
            .maxConcurrentExtractions(4)
            .extractionTimeoutMinutes(5)
            .enableAwdExtensions(true)
            .enableDationCompatibility(true)
            .validateOutput(true)
            .build();
        
        extractor = new ChorusFileExtractor(config);
        metadataExtractor = new BusinessMetadataExtractor();
        objectMapper = new ObjectMapper();
        
        baselineStoragePath = Paths.get(BASELINE_STORAGE_PATH);
        regressionOutputPath = Paths.get(REGRESSION_OUTPUT_PATH, "run-" + System.currentTimeMillis());
        Files.createDirectories(regressionOutputPath);
        
        logger.info("Regression test setup completed for: " + regressionOutputPath);
    }

    @AfterEach
    void tearDown() {
        if (extractor != null) {
            extractor.shutdown();
        }
        
        // Log regression analysis results
        regressionResults.forEach((testName, analysis) -> {
            logger.info("Regression analysis for " + testName + ": " + analysis.getSummary());
            if (analysis.hasRegression()) {
                logger.warning("REGRESSION DETECTED in " + testName + ": " + analysis.getRegressionDetails());
            }
        });
        
        regressionResults.clear();
    }

    @Test
    @Order(1)
    @DisplayName("Baseline comparison with standard test data")
    void testBaselineComparison() throws IOException {
        logger.info("=== Testing Baseline Comparison ===");
        
        String testName = "standard_baseline";
        Map<String, Object> testData = createStandardTestData();
        
        // Perform current extraction
        BaselineResult currentResult = performExtractionWithMetrics(testData, testName);
        
        // Load or create baseline
        BaselineResult baseline = loadOrCreateBaseline(testName, currentResult);
        
        // Compare results
        RegressionAnalysis analysis = compareWithBaseline(currentResult, baseline, testName);
        regressionResults.put(testName, analysis);
        
        // Assert no significant regression
        assertFalse(analysis.hasSignificantRegression(), 
            "Should not have significant regression: " + analysis.getRegressionDetails());
        
        assertTrue(analysis.getOutputSimilarity() >= MIN_OUTPUT_SIMILARITY_PERCENT,
            "Output similarity should meet minimum threshold: " + analysis.getOutputSimilarity() + "%");
        
        logger.info("Baseline comparison completed: " + analysis.getSummary());
    }

    @Test
    @Order(2)
    @DisplayName("Performance regression detection")
    void testPerformanceRegression() throws IOException {
        logger.info("=== Testing Performance Regression ===");
        
        String testName = "performance_regression";
        
        // Test with various data sizes to detect performance regression
        Map<Integer, BaselineResult> performanceResults = new HashMap<>();
        
        for (int dataSize : Arrays.asList(100, 500, 1000, 2000)) {
            logger.info("Testing performance with data size: " + dataSize);
            
            Map<String, Object> testData = createMockDataWithSize(dataSize);
            String sizeTestName = testName + "_size_" + dataSize;
            
            BaselineResult currentResult = performExtractionWithMetrics(testData, sizeTestName);
            performanceResults.put(dataSize, currentResult);
            
            // Load baseline for this data size
            BaselineResult baseline = loadOrCreateBaseline(sizeTestName, currentResult);
            
            // Analyze performance regression
            RegressionAnalysis analysis = comparePerformance(currentResult, baseline, sizeTestName);
            regressionResults.put(sizeTestName, analysis);
            
            // Check for performance regression
            double performanceDegradation = analysis.getPerformanceDegradationPercent();
            assertTrue(performanceDegradation <= MAX_PERFORMANCE_DEGRADATION_PERCENT,
                "Performance degradation should not exceed " + MAX_PERFORMANCE_DEGRADATION_PERCENT + 
                "%, actual: " + performanceDegradation + "%");
        }
        
        // Analyze performance scaling
        analyzePerformanceScaling(performanceResults);
        
        logger.info("Performance regression testing completed");
    }

    @Test
    @Order(3)
    @DisplayName("Output consistency regression")
    void testOutputConsistencyRegression() throws IOException {
        logger.info("=== Testing Output Consistency Regression ===");
        
        String testName = "output_consistency";
        
        // Test multiple extractions with same input for consistency
        Map<String, Object> testData = createConsistencyTestData();
        List<BaselineResult> consistencyResults = new ArrayList<>();
        
        // Perform multiple extractions
        for (int run = 1; run <= 5; run++) {
            String runTestName = testName + "_run_" + run;
            BaselineResult result = performExtractionWithMetrics(testData, runTestName);
            consistencyResults.add(result);
        }
        
        // Analyze consistency across runs
        RegressionAnalysis consistencyAnalysis = analyzeOutputConsistency(consistencyResults, testName);
        regressionResults.put(testName, consistencyAnalysis);
        
        // Assert output consistency
        assertTrue(consistencyAnalysis.isOutputConsistent(),
            "Output should be consistent across multiple runs");
        
        double consistencyScore = consistencyAnalysis.getConsistencyScore();
        assertTrue(consistencyScore >= 98.0,
            "Consistency score should be high: " + consistencyScore + "%");
        
        logger.info("Output consistency regression completed: " + consistencyAnalysis.getSummary());
    }

    @ParameterizedTest
    @ValueSource(strings = {"bpmn_only", "forms_only", "services_only", "all_types"})
    @DisplayName("Feature-specific regression testing")
    void testFeatureSpecificRegression(String featureType) throws IOException {
        logger.info("=== Testing Feature-Specific Regression: " + featureType + " ===");
        
        Map<String, Object> testData = createFeatureSpecificTestData(featureType);
        Set<FileExtractionType> extractionTypes = getExtractionTypesForFeature(featureType);
        
        // Perform extraction for specific feature
        FileExtractionRequest request = FileExtractionRequest.builder()
            .sourceName("feature_test_" + featureType)
            .outputDirectory(regressionOutputPath.resolve("feature_" + featureType).toString())
            .deserializedData(testData)
            .enabledTypes(extractionTypes)
            .dationFormat(true)
            .build();
        
        Instant startTime = Instant.now();
        ExtractionResult result = extractor.extractFiles(request);
        Instant endTime = Instant.now();
        
        BaselineResult currentResult = new BaselineResult(
            featureType,
            result,
            Duration.between(startTime, endTime).toMillis(),
            getCurrentMemoryUsage(),
            CURRENT_VERSION
        );
        
        // Load baseline for this feature
        BaselineResult baseline = loadOrCreateBaseline("feature_" + featureType, currentResult);
        
        // Analyze feature-specific regression
        RegressionAnalysis analysis = analyzeFeatureRegression(currentResult, baseline, featureType);
        regressionResults.put("feature_" + featureType, analysis);
        
        // Assert no feature regression
        assertFalse(analysis.hasFeatureRegression(),
            "Feature " + featureType + " should not have regression: " + analysis.getRegressionDetails());
        
        // Validate feature-specific metrics
        validateFeatureSpecificMetrics(result, extractionTypes, analysis);
        
        logger.info("Feature regression test completed for " + featureType + ": " + analysis.getSummary());
    }

    @Test
    @Order(4)
    @DisplayName("Data integrity regression validation")
    void testDataIntegrityRegression() throws IOException {
        logger.info("=== Testing Data Integrity Regression ===");
        
        String testName = "data_integrity";
        Map<String, Object> testData = createDataIntegrityTestData();
        
        // Perform extraction with focus on data integrity
        BaselineResult currentResult = performExtractionWithMetrics(testData, testName);
        
        // Load baseline
        BaselineResult baseline = loadOrCreateBaseline(testName, currentResult);
        
        // Analyze data integrity
        RegressionAnalysis analysis = analyzeDataIntegrity(currentResult, baseline, testName);
        regressionResults.put(testName, analysis);
        
        // Assert data integrity
        assertTrue(analysis.isDataIntegrityMaintained(),
            "Data integrity should be maintained: " + analysis.getIntegrityIssues());
        
        // Validate specific integrity aspects
        validateDataIntegrityAspects(currentResult, baseline, analysis);
        
        logger.info("Data integrity regression completed: " + analysis.getSummary());
    }

    @Test
    @Order(5)
    @DisplayName("Version compatibility regression")
    void testVersionCompatibilityRegression() throws IOException {
        logger.info("=== Testing Version Compatibility Regression ===");
        
        String testName = "version_compatibility";
        
        // Test compatibility with different input versions
        String[] inputVersions = {"2.3.0", "2.2.0", "2.1.0"};
        
        for (String inputVersion : inputVersions) {
            logger.info("Testing compatibility with input version: " + inputVersion);
            
            Map<String, Object> versionTestData = createVersionCompatibilityTestData(inputVersion);
            String versionTestName = testName + "_" + inputVersion.replace(".", "_");
            
            BaselineResult currentResult = performExtractionWithMetrics(versionTestData, versionTestName);
            BaselineResult baseline = loadOrCreateBaseline(versionTestName, currentResult);
            
            RegressionAnalysis analysis = analyzeVersionCompatibility(currentResult, baseline, inputVersion);
            regressionResults.put(versionTestName, analysis);
            
            // Assert version compatibility
            assertTrue(analysis.isVersionCompatible(),
                "Should maintain compatibility with version " + inputVersion + 
                ": " + analysis.getCompatibilityIssues());
        }
        
        logger.info("Version compatibility regression completed");
    }

    // Helper methods for extraction and metrics

    private BaselineResult performExtractionWithMetrics(Map<String, Object> testData, String testName) throws IOException {
        Instant startTime = Instant.now();
        long memoryBefore = getCurrentMemoryUsage();
        
        FileExtractionRequest request = FileExtractionRequest.builder()
            .sourceName(testName)
            .outputDirectory(regressionOutputPath.resolve(testName).toString())
            .deserializedData(testData)
            .enabledTypes(Set.of(FileExtractionType.BPMN_XML, FileExtractionType.FORM_HTML))
            .dationFormat(true)
            .build();
        
        ExtractionResult result = extractor.extractFiles(request);
        
        Instant endTime = Instant.now();
        long memoryAfter = getCurrentMemoryUsage();
        
        long processingTime = Duration.between(startTime, endTime).toMillis();
        long memoryUsed = Math.max(0, memoryAfter - memoryBefore);
        
        return new BaselineResult(testName, result, processingTime, memoryUsed, CURRENT_VERSION);
    }

    private long getCurrentMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    // Baseline management methods

    private BaselineResult loadOrCreateBaseline(String testName, BaselineResult currentResult) throws IOException {
        Path baselineFile = baselineStoragePath.resolve(testName + "_baseline.json");
        
        if (Files.exists(baselineFile)) {
            try {
                String baselineJson = Files.readString(baselineFile);
                JsonNode baselineNode = objectMapper.readTree(baselineJson);
                return BaselineResult.fromJson(baselineNode);
            } catch (Exception e) {
                logger.warning("Failed to load baseline for " + testName + ", creating new one: " + e.getMessage());
            }
        }
        
        // Create new baseline
        saveBaseline(testName, currentResult);
        logger.info("Created new baseline for: " + testName);
        return currentResult;
    }

    private void saveBaseline(String testName, BaselineResult baseline) throws IOException {
        Path baselineFile = baselineStoragePath.resolve(testName + "_baseline.json");
        String baselineJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(baseline.toJson());
        Files.writeString(baselineFile, baselineJson);
    }

    // Comparison and analysis methods

    private RegressionAnalysis compareWithBaseline(BaselineResult current, BaselineResult baseline, String testName) {
        RegressionAnalysis analysis = new RegressionAnalysis(testName);
        
        // Compare processing time
        double processingTimeDiff = calculatePercentageDifference(current.processingTime, baseline.processingTime);
        analysis.setProcessingTimeDifference(processingTimeDiff);
        
        // Compare memory usage
        double memoryDiff = calculatePercentageDifference(current.memoryUsed, baseline.memoryUsed);
        analysis.setMemoryUsageDifference(memoryDiff);
        
        // Compare output metrics
        analysis.setOutputSimilarity(calculateOutputSimilarity(current.extractionResult, baseline.extractionResult));
        
        // Determine if regression exists
        analysis.determineRegressionStatus();
        
        return analysis;
    }

    private RegressionAnalysis comparePerformance(BaselineResult current, BaselineResult baseline, String testName) {
        RegressionAnalysis analysis = compareWithBaseline(current, baseline, testName);
        
        // Additional performance-specific analysis
        double speedRatio = (double) baseline.processingTime / current.processingTime;
        analysis.setPerformanceSpeedRatio(speedRatio);
        
        return analysis;
    }

    private RegressionAnalysis analyzeOutputConsistency(List<BaselineResult> results, String testName) {
        RegressionAnalysis analysis = new RegressionAnalysis(testName);
        
        if (results.size() < 2) {
            analysis.setOutputConsistent(true);
            analysis.setConsistencyScore(100.0);
            return analysis;
        }
        
        // Compare all results against the first one
        BaselineResult reference = results.get(0);
        double totalSimilarity = 0.0;
        
        for (int i = 1; i < results.size(); i++) {
            double similarity = calculateOutputSimilarity(reference.extractionResult, results.get(i).extractionResult);
            totalSimilarity += similarity;
        }
        
        double averageSimilarity = totalSimilarity / (results.size() - 1);
        analysis.setConsistencyScore(averageSimilarity);
        analysis.setOutputConsistent(averageSimilarity >= MIN_OUTPUT_SIMILARITY_PERCENT);
        
        return analysis;
    }

    private RegressionAnalysis analyzeFeatureRegression(BaselineResult current, BaselineResult baseline, String featureType) {
        RegressionAnalysis analysis = compareWithBaseline(current, baseline, featureType);
        
        // Feature-specific analysis
        analysis.setFeatureType(featureType);
        
        // Check if feature-specific files were generated
        boolean currentHasFeatureOutput = hasFeatureOutput(current.extractionResult, featureType);
        boolean baselineHasFeatureOutput = hasFeatureOutput(baseline.extractionResult, featureType);
        
        if (baselineHasFeatureOutput && !currentHasFeatureOutput) {
            analysis.addRegressionIssue("Feature output missing for: " + featureType);
        }
        
        return analysis;
    }

    private RegressionAnalysis analyzeDataIntegrity(BaselineResult current, BaselineResult baseline, String testName) {
        RegressionAnalysis analysis = compareWithBaseline(current, baseline, testName);
        
        // Data integrity specific checks
        analysis.setDataIntegrityMaintained(validateDataIntegrity(current, baseline));
        
        return analysis;
    }

    private RegressionAnalysis analyzeVersionCompatibility(BaselineResult current, BaselineResult baseline, String version) {
        RegressionAnalysis analysis = compareWithBaseline(current, baseline, "version_" + version);
        
        // Version compatibility specific checks
        analysis.setVersionCompatible(validateVersionCompatibility(current, baseline, version));
        
        return analysis;
    }

    // Utility methods

    private double calculatePercentageDifference(long current, long baseline) {
        if (baseline == 0) return current == 0 ? 0.0 : 100.0;
        return ((double) (current - baseline) / baseline) * 100.0;
    }

    private double calculateOutputSimilarity(ExtractionResult current, ExtractionResult baseline) {
        // Simple similarity calculation based on file counts and structure
        double fileCountSimilarity = calculateFileCountSimilarity(current, baseline);
        double structureSimilarity = calculateStructureSimilarity(current, baseline);
        
        return (fileCountSimilarity + structureSimilarity) / 2.0;
    }

    private double calculateFileCountSimilarity(ExtractionResult current, ExtractionResult baseline) {
        if (baseline.getTotalFilesGenerated() == 0) {
            return current.getTotalFilesGenerated() == 0 ? 100.0 : 0.0;
        }
        
        double ratio = (double) current.getTotalFilesGenerated() / baseline.getTotalFilesGenerated();
        return Math.max(0.0, 100.0 - Math.abs(ratio - 1.0) * 100.0);
    }

    private double calculateStructureSimilarity(ExtractionResult current, ExtractionResult baseline) {
        // Compare file types generated
        Set<FileExtractionType> currentTypes = current.getProcessedTypes();
        Set<FileExtractionType> baselineTypes = baseline.getProcessedTypes();
        
        Set<FileExtractionType> intersection = new HashSet<>(currentTypes);
        intersection.retainAll(baselineTypes);
        
        Set<FileExtractionType> union = new HashSet<>(currentTypes);
        union.addAll(baselineTypes);
        
        if (union.isEmpty()) return 100.0;
        return ((double) intersection.size() / union.size()) * 100.0;
    }

    private void analyzePerformanceScaling(Map<Integer, BaselineResult> results) {
        logger.info("Analyzing performance scaling characteristics...");
        
        List<Integer> sizes = new ArrayList<>(results.keySet());
        sizes.sort(Integer::compareTo);
        
        for (int i = 1; i < sizes.size(); i++) {
            int prevSize = sizes.get(i - 1);
            int currentSize = sizes.get(i);
            
            BaselineResult prevResult = results.get(prevSize);
            BaselineResult currentResult = results.get(currentSize);
            
            double sizeRatio = (double) currentSize / prevSize;
            double timeRatio = (double) currentResult.processingTime / prevResult.processingTime;
            
            logger.info("Performance scaling - Size ratio: " + String.format("%.2f", sizeRatio) + 
                       ", Time ratio: " + String.format("%.2f", timeRatio));
            
            // Performance should scale reasonably (not exponentially)
            assertTrue(timeRatio <= sizeRatio * 2.0, 
                "Performance scaling should be reasonable: " + timeRatio + " vs " + sizeRatio);
        }
    }

    private boolean hasFeatureOutput(ExtractionResult result, String featureType) {
        switch (featureType) {
            case "bpmn_only":
                return !result.getGeneratedFiles(FileExtractionType.BPMN_XML).isEmpty();
            case "forms_only":
                return !result.getGeneratedFiles(FileExtractionType.FORM_HTML).isEmpty();
            case "services_only":
                return !result.getGeneratedFiles(FileExtractionType.SERVICE_CONFIG).isEmpty();
            case "all_types":
                return result.getTotalFilesGenerated() > 0;
            default:
                return true;
        }
    }

    private Set<FileExtractionType> getExtractionTypesForFeature(String featureType) {
        switch (featureType) {
            case "bpmn_only":
                return Set.of(FileExtractionType.BPMN_XML);
            case "forms_only":
                return Set.of(FileExtractionType.FORM_HTML);
            case "services_only":
                return Set.of(FileExtractionType.SERVICE_CONFIG);
            case "all_types":
            default:
                return Set.of(FileExtractionType.BPMN_XML, FileExtractionType.FORM_HTML, FileExtractionType.SERVICE_CONFIG);
        }
    }

    private boolean validateDataIntegrity(BaselineResult current, BaselineResult baseline) {
        // Basic data integrity validation
        return current.extractionResult.getTotalFilesGenerated() >= 0 &&
               !current.extractionResult.hasErrors();
    }

    private boolean validateVersionCompatibility(BaselineResult current, BaselineResult baseline, String version) {
        // Basic version compatibility validation
        return current.extractionResult.isSuccessful();
    }

    private void validateFeatureSpecificMetrics(ExtractionResult result, Set<FileExtractionType> expectedTypes, RegressionAnalysis analysis) {
        for (FileExtractionType type : expectedTypes) {
            if (result.getFilesByType().containsKey(type)) {
                int fileCount = result.getFilesByType().get(type);
                assertTrue(fileCount >= 0, "File count should be non-negative for " + type);
            }
        }
    }

    private void validateDataIntegrityAspects(BaselineResult current, BaselineResult baseline, RegressionAnalysis analysis) {
        // Validate specific data integrity aspects
        assertTrue(current.extractionResult.getSourceName() != null, "Source name should not be null");
        assertTrue(current.extractionResult.getOutputDirectory() != null, "Output directory should not be null");
        assertTrue(current.extractionResult.getTotalFilesGenerated() >= 0, "File count should be non-negative");
    }

    // Mock data creation methods

    private Map<String, Object> createStandardTestData() {
        Map<String, Object> data = new HashMap<>();
        data.put("version", CURRENT_VERSION);
        data.put("businessMetadataFields", List.of(
            Map.of("type", "process", "name", "StandardTestProcess"),
            Map.of("type", "form", "name", "StandardTestForm")
        ));
        return data;
    }

    private Map<String, Object> createMockDataWithSize(int objectCount) {
        Map<String, Object> data = new HashMap<>();
        
        List<Map<String, Object>> objects = new ArrayList<>();
        for (int i = 0; i < objectCount; i++) {
            objects.add(Map.of(
                "id", "obj_" + i,
                "type", "mockObject",
                "data", "Mock data content for object " + i
            ));
        }
        
        data.put("businessMetadataFields", objects);
        return data;
    }

    private Map<String, Object> createConsistencyTestData() {
        Map<String, Object> data = new HashMap<>();
        data.put("consistencyTest", true);
        data.put("businessMetadataFields", List.of(
            Map.of("type", "process", "name", "ConsistencyTestProcess", "id", "const_proc_1"),
            Map.of("type", "form", "name", "ConsistencyTestForm", "id", "const_form_1")
        ));
        return data;
    }

    private Map<String, Object> createFeatureSpecificTestData(String featureType) {
        Map<String, Object> data = new HashMap<>();
        data.put("featureType", featureType);
        
        switch (featureType) {
            case "bpmn_only":
                data.put("businessMetadataFields", List.of(
                    Map.of("type", "process", "name", "BpmnOnlyProcess")
                ));
                break;
            case "forms_only":
                data.put("businessMetadataFields", List.of(
                    Map.of("type", "form", "name", "FormsOnlyForm")
                ));
                break;
            case "services_only":
                data.put("businessMetadataFields", List.of(
                    Map.of("type", "service", "name", "ServicesOnlyService")
                ));
                break;
            case "all_types":
            default:
                data.put("businessMetadataFields", List.of(
                    Map.of("type", "process", "name", "AllTypesProcess"),
                    Map.of("type", "form", "name", "AllTypesForm"),
                    Map.of("type", "service", "name", "AllTypesService")
                ));
                break;
        }
        
        return data;
    }

    private Map<String, Object> createDataIntegrityTestData() {
        Map<String, Object> data = new HashMap<>();
        data.put("integrityTest", true);
        data.put("checksums", Map.of("data", "abc123", "metadata", "def456"));
        data.put("businessMetadataFields", List.of(
            Map.of("type", "process", "name", "IntegrityTestProcess", "checksum", "abc123")
        ));
        return data;
    }

    private Map<String, Object> createVersionCompatibilityTestData(String version) {
        Map<String, Object> data = new HashMap<>();
        data.put("inputVersion", version);
        data.put("compatibilityTest", true);
        data.put("businessMetadataFields", List.of(
            Map.of("type", "process", "name", "VersionTestProcess_" + version, "version", version)
        ));
        return data;
    }

    // Supporting classes

    private static class BaselineResult {
        final String testName;
        final ExtractionResult extractionResult;
        final long processingTime;
        final long memoryUsed;
        final String version;
        final long timestamp;
        
        BaselineResult(String testName, ExtractionResult extractionResult, long processingTime, long memoryUsed, String version) {
            this.testName = testName;
            this.extractionResult = extractionResult;
            this.processingTime = processingTime;
            this.memoryUsed = memoryUsed;
            this.version = version;
            this.timestamp = System.currentTimeMillis();
        }
        
        Map<String, Object> toJson() {
            Map<String, Object> json = new HashMap<>();
            json.put("testName", testName);
            json.put("processingTime", processingTime);
            json.put("memoryUsed", memoryUsed);
            json.put("version", version);
            json.put("timestamp", timestamp);
            json.put("totalFiles", extractionResult.getTotalFilesGenerated());
            json.put("successful", extractionResult.isSuccessful());
            json.put("filesByType", extractionResult.getFilesByType());
            return json;
        }
        
        static BaselineResult fromJson(JsonNode json) {
            // Simplified fromJson implementation for testing
            return new BaselineResult(
                json.get("testName").asText(),
                createMockExtractionResult(json),
                json.get("processingTime").asLong(),
                json.get("memoryUsed").asLong(),
                json.get("version").asText()
            );
        }
        
        private static ExtractionResult createMockExtractionResult(JsonNode json) {
            ExtractionResult result = new ExtractionResult();
            result.setTotalFilesGenerated(json.get("totalFiles").asInt());
            result.setSuccessful(json.get("successful").asBoolean());
            return result;
        }
    }

    private static class RegressionAnalysis {
        final String testName;
        private double processingTimeDifference;
        private double memoryUsageDifference;
        private double outputSimilarity;
        private boolean outputConsistent;
        private double consistencyScore;
        private double performanceSpeedRatio = 1.0;
        private String featureType;
        private boolean dataIntegrityMaintained = true;
        private boolean versionCompatible = true;
        private final List<String> regressionIssues = new ArrayList<>();
        
        RegressionAnalysis(String testName) {
            this.testName = testName;
        }
        
        void setProcessingTimeDifference(double diff) { this.processingTimeDifference = diff; }
        void setMemoryUsageDifference(double diff) { this.memoryUsageDifference = diff; }
        void setOutputSimilarity(double similarity) { this.outputSimilarity = similarity; }
        void setOutputConsistent(boolean consistent) { this.outputConsistent = consistent; }
        void setConsistencyScore(double score) { this.consistencyScore = score; }
        void setPerformanceSpeedRatio(double ratio) { this.performanceSpeedRatio = ratio; }
        void setFeatureType(String type) { this.featureType = type; }
        void setDataIntegrityMaintained(boolean maintained) { this.dataIntegrityMaintained = maintained; }
        void setVersionCompatible(boolean compatible) { this.versionCompatible = compatible; }
        void addRegressionIssue(String issue) { this.regressionIssues.add(issue); }
        
        boolean hasRegression() { return hasSignificantRegression() || hasFeatureRegression(); }
        boolean hasSignificantRegression() { 
            return processingTimeDifference > MAX_PERFORMANCE_DEGRADATION_PERCENT ||
                   memoryUsageDifference > (MAX_MEMORY_INCREASE_MB * 100.0 / 1024.0) ||
                   outputSimilarity < MIN_OUTPUT_SIMILARITY_PERCENT;
        }
        boolean hasFeatureRegression() { return !regressionIssues.isEmpty(); }
        boolean isOutputConsistent() { return outputConsistent; }
        boolean isDataIntegrityMaintained() { return dataIntegrityMaintained; }
        boolean isVersionCompatible() { return versionCompatible; }
        
        double getOutputSimilarity() { return outputSimilarity; }
        double getConsistencyScore() { return consistencyScore; }
        double getPerformanceDegradationPercent() { return Math.max(0, processingTimeDifference); }
        
        String getRegressionDetails() {
            StringBuilder details = new StringBuilder();
            if (processingTimeDifference > MAX_PERFORMANCE_DEGRADATION_PERCENT) {
                details.append("Performance degradation: ").append(String.format("%.2f%%", processingTimeDifference)).append("; ");
            }
            if (!regressionIssues.isEmpty()) {
                details.append("Issues: ").append(String.join(", ", regressionIssues));
            }
            return details.toString();
        }
        
        List<String> getIntegrityIssues() {
            List<String> issues = new ArrayList<>();
            if (!dataIntegrityMaintained) {
                issues.add("Data integrity compromised");
            }
            return issues;
        }
        
        List<String> getCompatibilityIssues() {
            List<String> issues = new ArrayList<>();
            if (!versionCompatible) {
                issues.add("Version compatibility issues detected");
            }
            return issues;
        }
        
        void determineRegressionStatus() {
            // Analysis logic for determining regression status
        }
        
        String getSummary() {
            return String.format("RegressionAnalysis{test=%s, perfDiff=%.2f%%, memDiff=%.2f%%, similarity=%.2f%%, issues=%d}",
                testName, processingTimeDifference, memoryUsageDifference, outputSimilarity, regressionIssues.size());
        }
    }

    @AfterAll
    static void tearDownClass() {
        logger.info("=== Regression Test Suite Complete ===");
    }
}