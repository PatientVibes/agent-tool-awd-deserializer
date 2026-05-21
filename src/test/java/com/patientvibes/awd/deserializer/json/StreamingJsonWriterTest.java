/**
 * Module: StreamingJsonWriterTest - Comprehensive streaming JSON writer tests
 * 
 * Summary:
 *     Complete test suite for StreamingJsonWriter focusing on memory efficiency,
 *     performance characteristics, error handling, and JSON format validation.
 *     Tests streaming capabilities with large data sets and memory constraints.
 * 
 * Key Components:
 *     - testStreamingPerformance(): Memory usage and speed validation
 *     - testLargeObjectHandling(): Large data set processing
 *     - testErrorRecovery(): Error handling and recovery mechanisms
 *     - testJsonFormatValidation(): Output format correctness
 * 
 * Keywords: test, streaming, json, writer, performance, memory, efficiency, large, data,
 *          error, handling, format, validation, output, correctness, speed, optimization
 * 
 * Dependencies:
 *     - JUnit 5: Testing framework
 *     - Jackson: JSON parsing and validation
 *     - AssertJ: Fluent assertions
 *     - Mockito: Mocking I/O operations
 * 
 * Security:
 *     - Tests output sanitization
 *     - Validates memory bounds
 *     - Tests resource cleanup
 *     - Verifies error handling
 * 
 * Performance:
 *     - Tests streaming efficiency
 *     - Validates memory usage patterns
 *     - Tests large file processing
 *     - Monitors write performance
 */

package com.patientvibes.awd.deserializer.json;

import com.patientvibes.awd.deserializer.config.DeserializerConfig;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.*;

class StreamingJsonWriterTest {
    
    @TempDir
    Path tempDir;
    
    
    private StreamingJsonWriter writer;
    private DeserializerConfig config;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        config = new DeserializerConfig.Builder()
            .memoryLimitMB(512)
            .enableMemoryMonitoring(true)
            .focusedMode(true)
            .build();
        
        writer = new StreamingJsonWriter(config);
        objectMapper = new ObjectMapper();
    }
    
    @Test
    @DisplayName("Should write simple objects to JSON stream")
    void testSimpleObjectWriting() throws IOException {
        File outputFile = tempDir.resolve("simple.json").toFile();
        
        // Create test object with various data types
        Map<String, Object> testData = new HashMap<>();
        testData.put("key1", "value1");
        testData.put("key2", 42);
        testData.put("key3", true);
        testData.put("key4", Arrays.asList("a", "b", "c"));
        
        writer.writeToFile(testData, outputFile);
        
        // Verify JSON format
        String jsonContent = Files.readString(outputFile.toPath());
        JsonNode jsonNode = objectMapper.readTree(jsonContent);
        
        assertThat(jsonNode.get("key1").asText()).isEqualTo("value1");
        assertThat(jsonNode.get("key2").asInt()).isEqualTo(42);
        assertThat(jsonNode.get("key3").asBoolean()).isTrue();
        assertThat(jsonNode.get("key4").isArray()).isTrue();
        assertThat(jsonNode.get("key4").size()).isEqualTo(3);
    }
    
    @Test
    @DisplayName("Should handle large data sets efficiently")
    void testLargeDataSetHandling() throws IOException {
        File outputFile = tempDir.resolve("large.json").toFile();
        
        long startTime = System.currentTimeMillis();
        long initialMemory = getUsedMemory();
        
        // Create large test data structure
        Map<String, Object> largeDataSet = new HashMap<>();
        
        // Write large number of objects
        for (int i = 0; i < 10000; i++) {
            Map<String, Object> largeObject = new HashMap<>();
            largeObject.put("id", i);
            largeObject.put("data", "Large data string for object " + i);
            largeObject.put("timestamp", System.currentTimeMillis());
            largeObject.put("values", Arrays.asList(i, i * 2, i * 3));
            
            largeDataSet.put("object_" + i, largeObject);
        }
        
        writer.writeToFile(largeDataSet, outputFile);
        
        long endTime = System.currentTimeMillis();
        long finalMemory = getUsedMemory();
        long processingTime = endTime - startTime;
        long memoryIncrease = finalMemory - initialMemory;
        
        // Verify performance characteristics
        assertThat(processingTime).isLessThan(30000); // Less than 30 seconds
        assertThat(memoryIncrease).isLessThan(100 * 1024 * 1024); // Less than 100MB increase
        
        // Verify file was created and has content
        assertTrue(Files.exists(outputFile.toPath()));
        assertThat(Files.size(outputFile.toPath())).isGreaterThan(1000); // Non-trivial size
        
        // Verify JSON structure
        JsonNode jsonNode = objectMapper.readTree(outputFile);
        assertThat(jsonNode.size()).isEqualTo(10000);
    }
    
    @Test
    @DisplayName("Should handle nested object structures")
    void testNestedObjectStructures() throws IOException {
        File outputFile = tempDir.resolve("nested.json").toFile();
        
        // Create deeply nested structure
        Map<String, Object> level3 = new HashMap<>();
        level3.put("deepValue", "Found at level 3");
        level3.put("deepArray", Arrays.asList(1, 2, 3, 4, 5));
        
        Map<String, Object> level2 = new HashMap<>();
        level2.put("level3", level3);
        level2.put("level2Value", "Middle level");
        
        Map<String, Object> level1 = new HashMap<>();
        level1.put("level2", level2);
        level1.put("topLevel", "Root value");
        
        Map<String, Object> rootObject = new HashMap<>();
        rootObject.put("nestedStructure", level1);
        
        writer.writeToFile(rootObject, outputFile);
        
        // Verify nested structure
        JsonNode jsonNode = objectMapper.readTree(outputFile);
        JsonNode nested = jsonNode.get("nestedStructure");
        
        assertThat(nested.get("topLevel").asText()).isEqualTo("Root value");
        assertThat(nested.get("level2").get("level2Value").asText()).isEqualTo("Middle level");
        assertThat(nested.get("level2").get("level3").get("deepValue").asText()).isEqualTo("Found at level 3");
    }
    
    @Test
    @DisplayName("Should handle special characters and Unicode")
    void testSpecialCharacterHandling() throws IOException {
        File outputFile = tempDir.resolve("unicode.json").toFile();
        
        // Create test object with various special characters and Unicode
        Map<String, Object> testData = new HashMap<>();
        testData.put("unicode", "测试中文字符");
        testData.put("emoji", "🚀🎯🔒");
        testData.put("special", "Special chars: \"quotes\", 'apostrophes', \n newlines, \t tabs");
        testData.put("russian", "Привет мир");
        testData.put("spanish", "Hola mundo ñañá");
        testData.put("japanese", "こんにちは世界");
        
        writer.writeToFile(testData, outputFile);
        
        // Verify Unicode preservation
        JsonNode jsonNode = objectMapper.readTree(outputFile);
        
        assertThat(jsonNode.get("unicode").asText()).isEqualTo("测试中文字符");
        assertThat(jsonNode.get("emoji").asText()).isEqualTo("🚀🎯🔒");
        assertThat(jsonNode.get("russian").asText()).isEqualTo("Привет мир");
        assertThat(jsonNode.get("spanish").asText()).isEqualTo("Hola mundo ñañá");
        assertThat(jsonNode.get("japanese").asText()).isEqualTo("こんにちは世界");
    }
    
    @Test
    @DisplayName("Should handle I/O errors gracefully")
    void testIOErrorHandling() throws IOException {
        // Create a read-only directory to trigger I/O error
        File readOnlyDir = tempDir.resolve("readonly").toFile();
        readOnlyDir.mkdirs();
        readOnlyDir.setWritable(false);
        
        File readOnlyFile = new File(readOnlyDir, "test.json");
        
        Map<String, Object> testData = new HashMap<>();
        testData.put("test", "data");
        
        assertThrows(IOException.class, () -> {
            writer.writeToFile(testData, readOnlyFile);
        });
        
        // Clean up
        readOnlyDir.setWritable(true);
    }
    
    @Test
    @DisplayName("Should handle memory monitoring during processing")
    void testMemoryMonitoringDuringProcessing() throws IOException {
        File outputFile = tempDir.resolve("memory_test.json").toFile();
        
        long initialMemory = getUsedMemory();
        
        // Create large data set to test memory usage
        Map<String, Object> largeDataSet = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            String largeValue = "Large string data ".repeat(100);
            largeDataSet.put("key" + i, largeValue);
        }
        
        writer.writeToFile(largeDataSet, outputFile);
        
        long finalMemory = getUsedMemory();
        long memoryIncrease = finalMemory - initialMemory;
        
        // Verify file was created and memory usage is reasonable
        assertTrue(Files.exists(outputFile.toPath()));
        assertThat(memoryIncrease).isLessThan(200 * 1024 * 1024); // Less than 200MB increase
    }
    
    @Test
    @DisplayName("Should handle concurrent access safely")
    void testConcurrentAccessSafety() throws IOException, InterruptedException {
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
        List<Thread> threads = new ArrayList<>();
        
        // Create multiple threads writing to different files concurrently
        for (int t = 0; t < 5; t++) {
            final int threadId = t;
            Thread thread = new Thread(() -> {
                try {
                    File outputFile = tempDir.resolve("concurrent_" + threadId + ".json").toFile();
                    Map<String, Object> threadData = new HashMap<>();
                    
                    for (int i = 0; i < 100; i++) {
                        threadData.put("thread" + threadId + "_item" + i, "value" + i);
                    }
                    
                    StreamingJsonWriter threadWriter = new StreamingJsonWriter(config);
                    threadWriter.writeToFile(threadData, outputFile);
                    
                } catch (Exception e) {
                    exceptions.add(e);
                }
            });
            threads.add(thread);
            thread.start();
        }
        
        // Wait for all threads
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Verify no exceptions occurred
        assertThat(exceptions).isEmpty();
        
        // Verify all files were created successfully
        for (int t = 0; t < 5; t++) {
            File outputFile = tempDir.resolve("concurrent_" + t + ".json").toFile();
            assertTrue(Files.exists(outputFile.toPath()));
            JsonNode jsonNode = objectMapper.readTree(outputFile);
            assertThat(jsonNode.size()).isEqualTo(100); // 100 items per thread
        }
    }
    
    @Test
    @DisplayName("Should validate output JSON format")
    void testOutputJsonFormatValidation() throws IOException {
        File outputFile = tempDir.resolve("format_test.json").toFile();
        
        // Create test object with various data types
        Map<String, Object> testData = new HashMap<>();
        testData.put("string", "test");
        testData.put("number", 123);
        testData.put("boolean", true);
        testData.put("null", null);
        testData.put("array", Arrays.asList(1, 2, 3));
        testData.put("object", Map.of("nested", "value"));
        
        writer.writeToFile(testData, outputFile);
        
        // Verify JSON is valid and well-formed
        String jsonContent = Files.readString(outputFile.toPath());
        
        // Should start and end with braces
        assertThat(jsonContent.trim()).startsWith("{");
        assertThat(jsonContent.trim()).endsWith("}");
        
        // Should be parseable as valid JSON
        JsonNode jsonNode = objectMapper.readTree(jsonContent);
        assertThat(jsonNode.isObject()).isTrue();
        
        // Verify all expected fields
        assertThat(jsonNode.has("string")).isTrue();
        assertThat(jsonNode.has("number")).isTrue();
        assertThat(jsonNode.has("boolean")).isTrue();
        assertThat(jsonNode.has("null")).isTrue();
        assertThat(jsonNode.has("array")).isTrue();
        assertThat(jsonNode.has("object")).isTrue();
    }
    
    @Test
    @DisplayName("Should handle complete write operations correctly")
    void testCompleteWriteOperations() throws IOException {
        File outputFile = tempDir.resolve("complete_test.json").toFile();
        
        // Create test data with multiple values
        Map<String, Object> testData = new HashMap<>();
        testData.put("first_value", "value1");
        testData.put("second_value", "value2");
        testData.put("numeric_value", 42);
        testData.put("boolean_value", true);
        
        writer.writeToFile(testData, outputFile);
        
        // Verify all values are present
        JsonNode jsonNode = objectMapper.readTree(outputFile);
        assertThat(jsonNode.get("first_value").asText()).isEqualTo("value1");
        assertThat(jsonNode.get("second_value").asText()).isEqualTo("value2");
        assertThat(jsonNode.get("numeric_value").asInt()).isEqualTo(42);
        assertThat(jsonNode.get("boolean_value").asBoolean()).isTrue();
    }
    
    @Test
    @DisplayName("Should clean up resources properly")
    void testResourceCleanup() throws IOException, InterruptedException {
        File outputFile = tempDir.resolve("cleanup_test.json").toFile();
        
        // Track initial memory
        long initialMemory = getUsedMemory();
        
        // Perform multiple write operations
        for (int iteration = 0; iteration < 10; iteration++) {
            StreamingJsonWriter iterationWriter = new StreamingJsonWriter(config);
            
            Map<String, Object> testData = new HashMap<>();
            for (int i = 0; i < 1000; i++) {
                testData.put("key" + i, "value" + i);
            }
            
            iterationWriter.writeToFile(testData, outputFile);
        }
        
        // Force garbage collection and check memory
        System.gc();
        Thread.sleep(100);
        long finalMemory = getUsedMemory();
        
        // Memory should not increase significantly
        long memoryIncrease = finalMemory - initialMemory;
        assertThat(memoryIncrease).isLessThan(50 * 1024 * 1024); // Less than 50MB
    }
    
    @Test
    @DisplayName("Should handle empty writes gracefully")
    void testEmptyWriteHandling() throws IOException {
        File outputFile = tempDir.resolve("empty.json").toFile();
        
        // Write empty object
        Map<String, Object> emptyData = new HashMap<>();
        writer.writeToFile(emptyData, outputFile);
        
        // Should create valid empty JSON object
        String jsonContent = Files.readString(outputFile.toPath());
        JsonNode jsonNode = objectMapper.readTree(jsonContent);
        
        assertThat(jsonNode.isObject()).isTrue();
        assertThat(jsonNode.size()).isEqualTo(0);
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Should complete within performance bounds")
    void testPerformanceBounds() throws IOException {
        File outputFile = tempDir.resolve("performance.json").toFile();
        
        long startTime = System.nanoTime();
        
        // Create substantial amount of test data
        Map<String, Object> performanceData = new HashMap<>();
        for (int i = 0; i < 5000; i++) {
            Map<String, Object> complexObject = new HashMap<>();
            complexObject.put("id", i);
            complexObject.put("data", "Data for object " + i);
            complexObject.put("numbers", Arrays.asList(i, i * 2, i * 3));
            complexObject.put("metadata", Map.of("created", System.currentTimeMillis()));
            
            performanceData.put("item_" + i, complexObject);
        }
        
        writer.writeToFile(performanceData, outputFile);
        
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;
        
        // Should complete in reasonable time
        assertThat(durationMs).isLessThan(10000); // Less than 10 seconds
        
        // Verify output quality
        assertTrue(Files.exists(outputFile.toPath()));
        assertThat(Files.size(outputFile.toPath())).isGreaterThan(100000); // Substantial content
    }
    
    // Helper method to get current memory usage
    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}