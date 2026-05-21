/**
 * Module: ChorusFileExtractorTest - Unit tests for ChorusFileExtractor orchestrator
 * 
 * Summary:
 *     Comprehensive unit tests for the ChorusFileExtractor class covering modular file
 *     extraction functionality, error handling, performance validation, and integration
 *     with AWD schema mapping. Tests both individual and parallel extraction scenarios.
 * 
 * Key Components:
 *     - testBasicFileExtraction(): Core extraction functionality validation
 *     - testParallelExtraction(): Concurrent processing validation
 *     - testErrorHandling(): Error scenarios and recovery testing
 *     - testPerformanceMetrics(): Performance tracking and reporting validation
 * 
 * Keywords: test, chorus, file, extractor, unit, testing, validation, parallel, extraction,
 *          error, handling, performance, metrics, awd, schema, mapping, integration
 * 
 * Dependencies:
 *     - org.junit.jupiter.api.*: JUnit 5 testing framework
 *     - org.mockito.*: Mocking framework for test isolation
 *     - org.assertj.core.api.Assertions: Enhanced assertion library
 * 
 * Security:
 *     - Test data isolation and cleanup
 *     - Temporary file management for test outputs
 *     - Mock object validation for security boundaries
 * 
 * Performance:
 *     - Test execution time monitoring
 *     - Memory usage validation during large extractions
 *     - Concurrent processing verification
 */
package com.patientvibes.awd.deserializer.extractor;

import com.patientvibes.awd.deserializer.business.model.BusinessMetadata;
import com.patientvibes.awd.deserializer.business.model.BusinessProcess;
import com.patientvibes.awd.deserializer.business.model.ProcessElement;
import com.patientvibes.awd.deserializer.business.model.ServiceDefinition;
import com.patientvibes.awd.deserializer.extractor.config.ExtractionConfig;
import com.patientvibes.awd.deserializer.extractor.model.ExtractionResult;
import com.patientvibes.awd.deserializer.extractor.model.FileExtractionRequest;
import com.patientvibes.awd.deserializer.extractor.model.FileExtractionType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ChorusFileExtractor.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChorusFileExtractorTest {
    
    @TempDir
    Path tempDir;
    
    private ChorusFileExtractor extractor;
    private ExtractionConfig config;
    private BusinessMetadata testMetadata;
    private Map<String, Object> testData;
    
    @BeforeEach
    void setUp() {
        config = new ExtractionConfig();
        config.setMaxConcurrentExtractions(2);
        config.setExtractionTimeoutMinutes(5);
        config.setValidateOutput(true);
        
        extractor = new ChorusFileExtractor(config);
        testMetadata = createTestBusinessMetadata();
        testData = createTestDeserializedData();
    }
    
    @Test
    void testBasicFileExtraction() throws IOException {
        // Given
        FileExtractionRequest request = new FileExtractionRequest(
            "TestProcess", 
            tempDir.toString(), 
            testData
        );
        request.setEnabledTypes(Set.of(FileExtractionType.BPMN_XML));
        
        // When
        ExtractionResult result = extractor.extractFiles(request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getTotalFilesGenerated()).isGreaterThan(0);
        assertThat(result.getGeneratedFiles(FileExtractionType.BPMN_XML)).isNotEmpty();
        
        // Verify files were created
        List<String> bpmnFiles = result.getGeneratedFiles(FileExtractionType.BPMN_XML);
        for (String filePath : bpmnFiles) {
            Path file = Paths.get(filePath);
            assertThat(Files.exists(file)).isTrue();
            assertThat(Files.size(file)).isGreaterThan(0);
        }
    }
    
    @Test
    void testParallelExtraction() throws IOException {
        // Given
        FileExtractionRequest request = new FileExtractionRequest(
            "ParallelTestProcess", 
            tempDir.toString(), 
            testData
        );
        request.setEnabledTypes(Set.of(
            FileExtractionType.BPMN_XML, 
            FileExtractionType.FORM_HTML, 
            FileExtractionType.SERVICE_CONFIG
        ));
        
        // When
        long startTime = System.currentTimeMillis();
        ExtractionResult result = extractor.extractFiles(request);
        long endTime = System.currentTimeMillis();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getProcessedTypes()).hasSize(3);
        
        // Verify all types were processed
        assertThat(result.getGeneratedFiles(FileExtractionType.BPMN_XML)).isNotEmpty();
        assertThat(result.getGeneratedFiles(FileExtractionType.FORM_HTML)).isNotEmpty();
        assertThat(result.getGeneratedFiles(FileExtractionType.SERVICE_CONFIG)).isNotEmpty();
        
        // Verify performance (parallel should be faster than sequential)
        long processingTime = endTime - startTime;
        assertThat(processingTime).isLessThan(10000); // Less than 10 seconds
    }
    
    @Test
    void testBpmnOnlyExtraction() throws IOException {
        // Given
        FileExtractionRequest request = new FileExtractionRequest(
            "BpmnOnlyProcess", 
            tempDir.toString(), 
            testData
        );
        
        // When
        ExtractionResult result = extractor.extractBpmnFiles(request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getProcessedTypes()).containsOnly(FileExtractionType.BPMN_XML, FileExtractionType.METADATA);
        
        // Verify BPMN content
        List<String> bpmnFiles = result.getGeneratedFiles(FileExtractionType.BPMN_XML);
        assertThat(bpmnFiles).hasSize(1); // One process in test data
        
        Path bpmnFile = Paths.get(bpmnFiles.get(0));
        String bpmnContent = Files.readString(bpmnFile);
        assertThat(bpmnContent).contains("<?xml version=\"1.0\"");
        assertThat(bpmnContent).contains("xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"");
        assertThat(bpmnContent).contains("xmlns:awd=\"http://www.dstawd.com\"");
        assertThat(bpmnContent).contains("bpmn:process");
    }
    
    @Test
    void testFormOnlyExtraction() throws IOException {
        // Given
        FileExtractionRequest request = new FileExtractionRequest(
            "FormOnlyProcess", 
            tempDir.toString(), 
            testData
        );
        
        // When
        ExtractionResult result = extractor.extractFormFiles(request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getProcessedTypes()).contains(FileExtractionType.FORM_HTML);
        
        // Verify form files exist
        List<String> formFiles = result.getGeneratedFiles(FileExtractionType.FORM_HTML);
        if (!formFiles.isEmpty()) {
            Path formFile = Paths.get(formFiles.get(0));
            String htmlContent = Files.readString(formFile);
            assertThat(htmlContent).contains("<!DOCTYPE html>");
            assertThat(htmlContent).contains("<form");
        }
    }
    
    @Test
    void testServiceOnlyExtraction() throws IOException {
        // Given
        FileExtractionRequest request = new FileExtractionRequest(
            "ServiceOnlyProcess", 
            tempDir.toString(), 
            testData
        );
        
        // When
        ExtractionResult result = extractor.extractServiceFiles(request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getProcessedTypes()).contains(FileExtractionType.SERVICE_CONFIG);
        
        // Verify service config files
        List<String> serviceFiles = result.getGeneratedFiles(FileExtractionType.SERVICE_CONFIG);
        assertThat(serviceFiles).isNotEmpty();
        
        Path serviceFile = Paths.get(serviceFiles.get(0));
        String configContent = Files.readString(serviceFile);
        assertThat(configContent).contains("serviceId");
        assertThat(configContent).contains("awdNamespace");
    }
    
    @Test
    void testInvalidRequest() {
        // Test null request
        assertThatThrownBy(() -> extractor.extractFiles(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Extraction request cannot be null");
        
        // Test empty data
        FileExtractionRequest emptyRequest = new FileExtractionRequest(
            "EmptyProcess", 
            tempDir.toString(), 
            new HashMap<>()
        );
        
        assertThatThrownBy(() -> extractor.extractFiles(emptyRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Deserialized data cannot be null or empty");
        
        // Test null output directory
        FileExtractionRequest nullDirRequest = new FileExtractionRequest(
            "NullDirProcess", 
            null, 
            testData
        );
        
        assertThatThrownBy(() -> extractor.extractFiles(nullDirRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Output directory cannot be null");
    }
    
    @Test
    void testErrorHandling() throws IOException {
        // Given - Create request with invalid output directory
        FileExtractionRequest request = new FileExtractionRequest(
            "ErrorTestProcess", 
            "/invalid/directory/path", 
            testData
        );
        
        // When/Then - Should handle error gracefully
        assertThatCode(() -> {
            ExtractionResult result = extractor.extractFiles(request);
            // Should not throw exception, but result should indicate failure
            assertThat(result.hasErrors()).isTrue();
        }).doesNotThrowAnyException();
    }
    
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testExtractionTimeout() throws IOException {
        // Given - Set very short timeout
        ExtractionConfig shortTimeoutConfig = new ExtractionConfig();
        shortTimeoutConfig.setExtractionTimeoutMinutes(1);
        
        ChorusFileExtractor timeoutExtractor = new ChorusFileExtractor(shortTimeoutConfig);
        
        FileExtractionRequest request = new FileExtractionRequest(
            "TimeoutTestProcess", 
            tempDir.toString(), 
            testData
        );
        
        // When
        ExtractionResult result = timeoutExtractor.extractFiles(request);
        
        // Then - Should complete within timeout
        assertThat(result).isNotNull();
        assertThat(result.getTotalProcessingTime()).isLessThan(60000); // Less than 1 minute
        
        timeoutExtractor.shutdown();
    }
    
    @Test
    void testPerformanceMetrics() throws IOException {
        // Given
        FileExtractionRequest request = new FileExtractionRequest(
            "MetricsTestProcess", 
            tempDir.toString(), 
            testData
        );
        
        // When
        ExtractionResult result = extractor.extractFiles(request);
        
        // Then - Verify metrics are collected
        assertThat(result.getTotalProcessingTime()).isGreaterThan(0);
        assertThat(result.getStartTime()).isNotNull();
        assertThat(result.getEndTime()).isNotNull();
        assertThat(result.getStartTime()).isBefore(result.getEndTime());
        
        Map<String, Object> performanceMetrics = extractor.getPerformanceMetrics();
        assertThat(performanceMetrics).isNotEmpty();
        assertThat(performanceMetrics).containsKey("totalExtractionTime");
        assertThat(performanceMetrics).containsKey("totalFilesGenerated");
    }
    
    @Test
    void testDirectoryStructureCreation() throws IOException {
        // Given
        FileExtractionRequest request = new FileExtractionRequest(
            "DirectoryTestProcess", 
            tempDir.toString(), 
            testData
        );
        
        // When
        ExtractionResult result = extractor.extractFiles(request);
        
        // Then - Verify directory structure is created
        assertThat(Files.exists(tempDir.resolve("bpmn"))).isTrue();
        assertThat(Files.exists(tempDir.resolve("forms"))).isTrue();
        assertThat(Files.exists(tempDir.resolve("services"))).isTrue();
        assertThat(Files.exists(tempDir.resolve("metadata"))).isTrue();
        
        assertThat(Files.isDirectory(tempDir.resolve("bpmn"))).isTrue();
        assertThat(Files.isDirectory(tempDir.resolve("forms"))).isTrue();
        assertThat(Files.isDirectory(tempDir.resolve("services"))).isTrue();
        assertThat(Files.isDirectory(tempDir.resolve("metadata"))).isTrue();
    }
    
    @Test
    void testExtractionResultSummary() throws IOException {
        // Given
        FileExtractionRequest request = new FileExtractionRequest(
            "SummaryTestProcess", 
            tempDir.toString(), 
            testData
        );
        
        // When
        ExtractionResult result = extractor.extractFiles(request);
        
        // Then
        Map<String, Object> summary = result.getSummary();
        assertThat(summary).isNotEmpty();
        assertThat(summary).containsKey("sourceName");
        assertThat(summary).containsKey("successful");
        assertThat(summary).containsKey("totalFilesGenerated");
        assertThat(summary).containsKey("processedTypes");
        assertThat(summary.get("sourceName")).isEqualTo("SummaryTestProcess");
    }
    
    // Helper methods
    
    private BusinessMetadata createTestBusinessMetadata() {
        BusinessMetadata metadata = new BusinessMetadata();
        metadata.setExtractionTimestamp(LocalDateTime.now());
        metadata.setSourceType("AWD_DESIGN_FILE");
        
        // Create test business process
        BusinessProcess process = new BusinessProcess();
        process.setId("TestProcess_001");
        process.setName("Test Business Process");
        process.setType("AUTOMATION");
        process.setVersion(1);
        
        // Add process elements
        ProcessElement startEvent = new ProcessElement();
        startEvent.setId("StartEvent_1");
        startEvent.setName("Start");
        startEvent.setType("startEvent");
        process.addElement(startEvent);
        
        ProcessElement userTask = new ProcessElement();
        userTask.setId("UserTask_1");
        userTask.setName("Test User Task");
        userTask.setType("userTask");
        process.addElement(userTask);
        
        ProcessElement endEvent = new ProcessElement();
        endEvent.setId("EndEvent_1");
        endEvent.setName("End");
        endEvent.setType("endEvent");
        process.addElement(endEvent);
        
        metadata.setBusinessProcesses(Arrays.asList(process));
        
        // Create test service definition
        ServiceDefinition service = new ServiceDefinition();
        service.setServiceId("TestService_001");
        service.setServiceClass("com.test.TestService");
        service.setServiceType("AWD_ENTERPRISE_SERVICE");
        service.setProperties(Map.of("version", 1, "enabled", true));
        service.setDependencies(Arrays.asList("com.dstawd.core.BaseService"));
        
        metadata.setServiceDefinitions(Arrays.asList(service));
        
        return metadata;
    }
    
    private Map<String, Object> createTestDeserializedData() {
        Map<String, Object> data = new HashMap<>();
        
        data.put("rootObject", "com.dstawd.design.model.DeploymentPackage");
        data.put("version", "2.4.0");
        data.put("processDefinitions", Arrays.asList(
            Map.of("id", "TestProcess_001", "name", "Test Process", "type", "BPMN")
        ));
        
        // Add some AWD-specific content for pattern matching
        String awdContent = """
            <bpmn:process id="TestProcess_001" name="Test Business Process">
                <bpmn:extensionElements>
                    <awd:properties>
                        <awd:property name="config">
                            {
                                "setValues": [
                                    {
                                        "target": "awd:awd-value('customerName')",
                                        "source": "//*[name()='transaction']/customer/name"
                                    }
                                ]
                            }
                        </awd:property>
                    </awd:properties>
                </bpmn:extensionElements>
                <awd:setValue id="setValue1" name="Set Customer Data">
                    <bpmn:extensionElements>
                        <awd:properties>
                            <awd:property name="config">
                                {"target": "awd:awd-value('status')", "source": "PENDING"}
                            </awd:property>
                        </awd:properties>
                    </bpmn:extensionElements>
                </awd:setValue>
            </bpmn:process>
            """;
        
        data.put("bpmnContent", awdContent);
        data.put("formDefinitions", Map.of(
            "HtmlTextInput", Map.of("type", "text", "validation", "required"),
            "HtmlRadioGroup", Map.of("type", "radio", "options", Arrays.asList("Yes", "No"))
        ));
        
        return data;
    }
}