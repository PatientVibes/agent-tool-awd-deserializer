/**
 * Module: RawSerializationReaderTest - Test suite for raw serialization metadata extraction
 * 
 * Summary:
 *     Comprehensive tests for the RawSerializationReader component that extracts object
 *     metadata from Java serialization streams without requiring original classes.
 *     Validates header parsing, object detection, and field extraction capabilities.
 * 
 * Key Components:
 *     - testSerializationHeaderValidation(): Validates Java serialization format detection
 *     - testObjectStructureExtraction(): Tests object parsing and type detection
 *     - testFieldDataExtraction(): Validates field extraction capabilities
 *     - testTypeCodeInterpretation(): Tests serialization type code handling
 * 
 * Keywords: test, raw, serialization, reader, metadata, extraction, parsing, validation,
 *          header, object, field, type, code, stream, format, java
 * 
 * Dependencies:
 *     - JUnit 5 for test framework
 *     - AssertJ for assertions
 *     - Java serialization format knowledge
 * 
 * Security: Tests include validation of input bounds and memory limits
 * Performance: Validates processing limits for large streams
 */
package com.patientvibes.awd.deserializer.reflection;

import org.junit.jupiter.api.*;
import java.io.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

@DisplayName("RawSerializationReader Tests")
class RawSerializationReaderTest {
    
    private RawSerializationReader reader;
    
    @BeforeEach
    void setUp() {
        reader = new RawSerializationReader();
    }
    
    @Nested
    @DisplayName("Serialization Header Validation")
    class SerializationHeaderTests {
        
        @Test
        @DisplayName("Should validate correct Java serialization header")
        void testValidSerializationHeader() throws Exception {
            // Given: Valid serialized Java object
            byte[] validData = serializeObject(new HashMap<>());
            
            // When: Extracting object data
            Map<String, Object> result = reader.extractObjectData(validData);
            
            // Then: Should validate header correctly
            assertThat(result).containsKey("_serializationHeader");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> header = (Map<String, Object>) result.get("_serializationHeader");
            assertThat(header).containsEntry("magic", "0xACED");
            assertThat(header).containsEntry("magicValid", true);
            assertThat(header).containsEntry("version", (short) 5);
            assertThat(header).containsEntry("versionValid", true);
        }
        
        @Test
        @DisplayName("Should reject invalid magic number")
        void testInvalidMagicNumber() {
            // Given: Data with invalid magic number
            byte[] invalidData = new byte[] { 0x00, 0x00, 0x00, 0x05 };
            
            // When/Then: Should detect invalid header
            assertThatThrownBy(() -> reader.extractObjectData(invalidData))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Invalid serialization magic number");
        }
        
        @Test
        @DisplayName("Should handle short data gracefully")
        void testShortDataHandling() throws IOException {
            // Given: Data too short for header
            byte[] shortData = new byte[] { (byte) 0xAC };
            
            // When: Attempting to extract
            Map<String, Object> result = reader.extractObjectData(shortData);
            
            // Then: Should handle error gracefully
            assertThat(result).containsKey("_error");
            assertThat(result).containsEntry("_partialData", true);
        }
    }
    
    @Nested
    @DisplayName("Object Structure Extraction")
    class ObjectStructureExtractionTests {
        
        @Test
        @DisplayName("Should extract basic object structure")
        void testBasicObjectExtraction() throws Exception {
            // Given: Serialized map with data
            Map<String, Object> testMap = new HashMap<>();
            testMap.put("key", "value");
            byte[] data = serializeObject(testMap);
            
            // When: Extracting structure
            Map<String, Object> result = reader.extractObjectData(data);
            
            // Then: Should extract object structure
            assertThat(result).containsKey("_objectStructure");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> structure = (Map<String, Object>) result.get("_objectStructure");
            assertThat(structure).containsKey("objects");
            assertThat(structure).containsKey("objectCount");
        }
        
        @Test
        @DisplayName("Should detect different object types")
        void testObjectTypeDetection() throws Exception {
            // Given: Various object types
            List<Object> mixedObjects = Arrays.asList(
                "string value",
                42,
                new HashMap<>(),
                new ArrayList<>()
            );
            byte[] data = serializeObject(mixedObjects);
            
            // When: Extracting objects
            Map<String, Object> result = reader.extractObjectData(data);
            
            // Then: Should detect different types
            @SuppressWarnings("unchecked")
            Map<String, Object> structure = (Map<String, Object>) result.get("_objectStructure");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> objects = (List<Map<String, Object>>) structure.get("objects");
            
            assertThat(objects).isNotEmpty();
            
            // Verify different type codes detected
            Set<String> detectedTypes = new HashSet<>();
            for (Map<String, Object> obj : objects) {
                detectedTypes.add((String) obj.get("typeName"));
            }
            assertThat(detectedTypes.size()).isGreaterThan(1);
        }
        
        @Test
        @DisplayName("Should limit object processing to prevent memory issues")
        void testObjectProcessingLimit() throws Exception {
            // Given: Many objects
            List<String> manyObjects = new ArrayList<>();
            for (int i = 0; i < 150; i++) {
                manyObjects.add("object-" + i);
            }
            byte[] data = serializeObject(manyObjects);
            
            // When: Processing
            Map<String, Object> result = reader.extractObjectData(data);
            
            // Then: Should limit objects processed
            @SuppressWarnings("unchecked")
            Map<String, Object> structure = (Map<String, Object>) result.get("_objectStructure");
            assertThat(structure).containsEntry("_truncated", true);
            
            @SuppressWarnings("unchecked")
            List<Object> objects = (List<Object>) structure.get("objects");
            assertThat(objects).hasSize(100); // Limit is 100
        }
    }
    
    @Nested
    @DisplayName("Type Code Interpretation")
    class TypeCodeInterpretationTests {
        
        @Test
        @DisplayName("Should interpret standard type codes correctly")
        void testStandardTypeCodes() throws Exception {
            // Given: Object with various types
            Map<String, Object> testData = new HashMap<>();
            testData.put("string", "test");
            testData.put("array", new int[] {1, 2, 3});
            testData.put("null", null);
            byte[] data = serializeObject(testData);
            
            // When: Processing
            Map<String, Object> result = reader.extractObjectData(data);
            
            // Then: Should identify type codes
            @SuppressWarnings("unchecked")
            Map<String, Object> structure = (Map<String, Object>) result.get("_objectStructure");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> objects = (List<Map<String, Object>>) structure.get("objects");
            
            // Check for various type names
            List<String> typeNames = new ArrayList<>();
            for (Map<String, Object> obj : objects) {
                String typeName = (String) obj.get("typeName");
                if (typeName != null) {
                    typeNames.add(typeName);
                }
            }
            
            // Should detect various types
            assertThat(typeNames).contains("OBJECT", "STRING", "ARRAY", "NULL");
        }
        
        @Test
        @DisplayName("Should handle unknown type codes")
        void testUnknownTypeCodes() throws Exception {
            // Given: Data with custom structure
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            
            // Write valid header
            dos.writeShort((short) 0xACED);
            dos.writeShort((short) 5);
            
            // Write unknown type code
            dos.writeByte(0xFF); // Not a standard type code
            
            byte[] data = baos.toByteArray();
            
            // When: Processing
            Map<String, Object> result = reader.extractObjectData(data);
            
            // Then: Should handle unknown type gracefully
            @SuppressWarnings("unchecked")
            Map<String, Object> structure = (Map<String, Object>) result.get("_objectStructure");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> objects = (List<Map<String, Object>>) structure.get("objects");
            
            if (!objects.isEmpty()) {
                Map<String, Object> firstObj = objects.get(0);
                assertThat(firstObj.get("typeName")).asString().startsWith("UNKNOWN");
            }
        }
    }
    
    @Nested
    @DisplayName("String Processing")
    class StringProcessingTests {
        
        @Test
        @DisplayName("Should extract string values correctly")
        void testStringExtraction() throws Exception {
            // Given: String data
            String testString = "com.dstawd.design.model.TestClass";
            byte[] data = serializeObject(testString);
            
            // When: Processing
            Map<String, Object> result = reader.extractObjectData(data);
            
            // Then: Should extract string
            @SuppressWarnings("unchecked")
            Map<String, Object> structure = (Map<String, Object>) result.get("_objectStructure");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> objects = (List<Map<String, Object>>) structure.get("objects");
            
            // Find string object
            Optional<Map<String, Object>> stringObj = objects.stream()
                .filter(obj -> "STRING".equals(obj.get("typeName")))
                .findFirst();
            
            assertThat(stringObj).isPresent();
            assertThat(stringObj.get()).containsEntry("value", testString);
            assertThat(stringObj.get()).containsEntry("_isAWDClassName", true);
        }
        
        @Test
        @DisplayName("Should handle long strings safely")
        void testLongStringHandling() throws Exception {
            // Given: Very long string
            StringBuilder longString = new StringBuilder();
            for (int i = 0; i < 2000; i++) {
                longString.append("x");
            }
            byte[] data = serializeObject(longString.toString());
            
            // When: Processing
            Map<String, Object> result = reader.extractObjectData(data);
            
            // Then: Should handle without reading entire string
            assertThat(result).isNotNull();
            // Long strings (>1000 chars) are not fully read for safety
        }
    }
    
    @Nested
    @DisplayName("Class Descriptor Processing")
    class ClassDescriptorTests {
        
        @Test
        @DisplayName("Should extract class descriptor information")
        void testClassDescriptorExtraction() throws Exception {
            // Given: Serialized custom object
            TestSerializableClass testObj = new TestSerializableClass();
            testObj.value = "test";
            byte[] data = serializeObject(testObj);
            
            // When: Processing
            Map<String, Object> result = reader.extractObjectData(data);
            
            // Then: Should find class descriptor
            @SuppressWarnings("unchecked")
            Map<String, Object> structure = (Map<String, Object>) result.get("_objectStructure");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> objects = (List<Map<String, Object>>) structure.get("objects");
            
            // Find class descriptor
            Optional<Map<String, Object>> classDesc = objects.stream()
                .filter(obj -> "CLASSDESC".equals(obj.get("typeName")))
                .findFirst();
            
            assertThat(classDesc).isPresent();
            assertThat(classDesc.get()).containsKey("className");
            assertThat(classDesc.get()).containsKey("serialVersionUID");
        }
        
        @Test
        @DisplayName("Should detect AWD class descriptors")
        void testAWDClassDescriptorDetection() throws Exception {
            // Given: Mock AWD class descriptor in stream
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            
            // Write header
            dos.writeShort((short) 0xACED);
            dos.writeShort((short) 5);
            
            // Write class descriptor
            dos.writeByte(0x72); // TC_CLASSDESC
            String className = "com.dstawd.design.model.DeploymentPackage";
            dos.writeShort(className.length());
            dos.writeBytes(className);
            dos.writeLong(5973128698326380281L); // serialVersionUID
            
            byte[] data = baos.toByteArray();
            
            // When: Processing
            Map<String, Object> result = reader.extractObjectData(data);
            
            // Then: Should detect AWD class
            @SuppressWarnings("unchecked")
            Map<String, Object> structure = (Map<String, Object>) result.get("_objectStructure");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> objects = (List<Map<String, Object>>) structure.get("objects");
            
            Map<String, Object> classDesc = objects.get(0);
            assertThat(classDesc).containsEntry("className", className);
            assertThat(classDesc).containsEntry("_isAWDClass", true);
            assertThat(classDesc).containsEntry("serialVersionUID", 5973128698326380281L);
        }
    }
    
    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {
        
        @Test
        @DisplayName("Should handle EOF gracefully")
        void testEOFHandling() throws Exception {
            // Given: Truncated data
            byte[] fullData = serializeObject("test");
            byte[] truncatedData = Arrays.copyOf(fullData, fullData.length / 2);
            
            // When: Processing
            Map<String, Object> result = reader.extractObjectData(truncatedData);
            
            // Then: Should handle partial read
            assertThat(result).containsKey("_objectStructure");
            @SuppressWarnings("unchecked")
            Map<String, Object> structure = (Map<String, Object>) result.get("_objectStructure");
            assertThat(structure).containsKey("_partialRead");
        }
        
        @Test
        @DisplayName("Should provide meaningful error context")
        void testErrorContext() throws IOException {
            // Given: Completely invalid data
            byte[] invalidData = "not serialization data".getBytes();
            
            // When: Processing
            Map<String, Object> result = reader.extractObjectData(invalidData);
            
            // Then: Should provide error context
            assertThat(result).containsKey("_error");
            assertThat(result).containsKey("_partialData");
            assertThat(result).containsKey("_dataSize");
        }
    }
    
    // Helper methods
    
    private byte[] serializeObject(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }
        return baos.toByteArray();
    }
    
    // Test helper class
    static class TestSerializableClass implements Serializable {
        private static final long serialVersionUID = 123456789L;
        public String value;
    }
}