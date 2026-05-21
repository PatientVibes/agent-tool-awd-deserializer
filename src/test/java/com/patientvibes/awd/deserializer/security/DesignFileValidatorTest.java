package com.patientvibes.awd.deserializer.security;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Deflater;
import java.net.URL;
import java.net.URI;
import java.net.InetAddress;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive security tests for DesignFileValidator.
 * Tests various attack vectors and edge cases to ensure robust validation.
 */
class DesignFileValidatorTest {
    
    @TempDir
    Path tempDir;
    
    private DesignFileValidator validator;
    private DesignFileValidator strictValidator;
    private DesignFileValidator lenientValidator;
    
    @BeforeEach
    void setUp() {
        // Default validator with standard settings
        validator = new DesignFileValidator();
        
        // Strict validator with small file size limit
        strictValidator = new DesignFileValidator(
            1024 * 1024, // 1MB limit
            new HashSet<>(Arrays.asList("com.dstawd.", "java.lang.", "java.util.")),
            new HashSet<>(Arrays.asList("java.lang.Runtime", "java.lang.ProcessBuilder")),
            true
        );
        
        // Lenient validator for testing
        lenientValidator = new DesignFileValidator(
            100 * 1024 * 1024, // 100MB limit
            new HashSet<>(Arrays.asList("com.", "java.", "javax.", "org.")),
            new HashSet<>(),
            false
        );
    }
    
    @Test
    @DisplayName("Should reject oversized files")
    void testOversizedFile() throws IOException {
        // Create a file larger than 1MB (strict validator limit)
        Path oversizedFile = tempDir.resolve("oversized.design");
        
        // Create a large file that will exceed 1MB when saved
        // We need to create enough data that the file itself is > 1MB
        try (FileOutputStream fos = new FileOutputStream(oversizedFile.toFile())) {
            // Write directly without compression to ensure file size > 1MB
            byte[] chunk = new byte[1024 * 100]; // 100KB chunks
            Arrays.fill(chunk, (byte) 0x42);
            for (int i = 0; i < 15; i++) { // Write 1.5MB
                fos.write(chunk);
            }
        }
        
        // Verify file is actually > 1MB
        long fileSize = Files.size(oversizedFile);
        assertTrue(fileSize > 1024 * 1024, "File should be larger than 1MB");
        
        // Test with strict validator (1MB limit)
        DesignFileValidator.ValidationResult result = strictValidator.validate(oversizedFile);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("exceeds maximum allowed size"));
        
        // Test with lenient validator (100MB limit) - should pass size check but fail on format
        result = lenientValidator.validate(oversizedFile);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("not a valid GZIP file"));
    }
    
    @Test
    @DisplayName("Should reject files with corrupted GZIP headers")
    void testCorruptedGzipHeaders() throws IOException {
        // Test 1: File with wrong magic bytes
        Path corruptedFile1 = tempDir.resolve("corrupted1.design");
        try (FileOutputStream fos = new FileOutputStream(corruptedFile1.toFile())) {
            fos.write(new byte[]{0x00, 0x00, 0x01, 0x02, 0x03}); // Not GZIP magic bytes
        }
        
        DesignFileValidator.ValidationResult result = validator.validate(corruptedFile1);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("not a valid GZIP file"));
        
        // Test 2: File with partial GZIP header
        Path corruptedFile2 = tempDir.resolve("corrupted2.design");
        try (FileOutputStream fos = new FileOutputStream(corruptedFile2.toFile())) {
            fos.write(new byte[]{0x1f}); // Only first byte of GZIP magic
        }
        
        result = validator.validate(corruptedFile2);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("not a valid GZIP file"));
        
        // Test 3: Empty file
        Path emptyFile = tempDir.resolve("empty.design");
        Files.createFile(emptyFile);
        
        result = validator.validate(emptyFile);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("not a valid GZIP file"));
    }
    
    @Test
    @DisplayName("Should reject malicious class injection attempts")
    void testMaliciousClassInjection() throws IOException {
        // Note: Since our test objects don't actually contain Runtime/ProcessBuilder objects,
        // we need to test with actual restricted classes or simulate them differently
        
        // Test 1: Create a file with non-whitelisted class (java.net.URL)
        Path maliciousFile1 = createSerializedFile("malicious1.design", 
            new URL("http://malicious.com")); // java.net not in whitelist
        
        DesignFileValidator.ValidationResult result = validator.validate(maliciousFile1);
        assertFalse(result.isValid(), "Should reject non-whitelisted class");
        assertTrue(result.getErrorMessage().contains("Unauthorized class found"),
                  "Error message should indicate unauthorized class");
        
        // Test 2: Create file with another non-whitelisted class (java.net.InetAddress)
        Path maliciousFile2 = createSerializedFile("malicious2.design", 
            InetAddress.getLoopbackAddress()); // java.net not in whitelist
        
        result = validator.validate(maliciousFile2);
        assertFalse(result.isValid(), "Should reject java.net classes");
        assertTrue(result.getErrorMessage().contains("Unauthorized class found"));
        
        // Test 3: Create file with java.io.File (not whitelisted)
        Path maliciousFile3 = createSerializedFile("malicious3.design", 
            new File("/etc/passwd")); // java.io.File not in whitelist
        
        result = validator.validate(maliciousFile3);
        assertFalse(result.isValid(), "Should reject java.io classes");
        assertTrue(result.getErrorMessage().contains("Unauthorized class found"));
    }
    
    @Test
    @DisplayName("Should accept valid design files")
    void testValidFileAcceptance() throws IOException {
        // Test 1: Valid serialized object with allowed classes
        Path validFile1 = createSerializedFile("valid1.design", 
            new ValidTestObject("Test Data", 42));
        
        DesignFileValidator.ValidationResult result = lenientValidator.validate(validFile1);
        assertTrue(result.isValid(), "Should accept valid serialized file");
        assertNull(result.getErrorMessage());
        
        // Test 2: Valid file with collections
        Path validFile2 = createSerializedFile("valid2.design", 
            Arrays.asList("item1", "item2", "item3"));
        
        result = lenientValidator.validate(validFile2);
        assertTrue(result.isValid(), "Should accept valid file with collections");
    }
    
    @Test
    @DisplayName("Should handle edge cases gracefully")
    void testEdgeCases() throws IOException {
        // Test 1: Non-existent file
        Path nonExistentFile = tempDir.resolve("nonexistent.design");
        DesignFileValidator.ValidationResult result = validator.validate(nonExistentFile);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("File does not exist"));
        
        // Test 2: Directory instead of file
        Path directory = tempDir.resolve("directory");
        Files.createDirectory(directory);
        
        result = validator.validate(directory);
        assertFalse(result.isValid());
        
        // Test 3: GZIP file with non-serialized content
        Path nonSerializedGzip = tempDir.resolve("text.design");
        try (FileOutputStream fos = new FileOutputStream(nonSerializedGzip.toFile());
             GZIPOutputStream gzos = new GZIPOutputStream(fos)) {
            gzos.write("This is plain text, not serialized data".getBytes());
        }
        
        result = validator.validate(nonSerializedGzip);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("not a valid Java serialized object"));
    }
    
    @Test
    @DisplayName("Should validate class whitelisting correctly")
    void testClassWhitelisting() throws IOException {
        // Create custom validator with specific whitelist
        Set<String> allowedPrefixes = new HashSet<>(Arrays.asList(
            "com.patientvibes.awd.deserializer.security.DesignFileValidatorTest$",
            "java.lang.",
            "java.util."
        ));
        Set<String> blockedClasses = new HashSet<>();
        
        DesignFileValidator customValidator = new DesignFileValidator(
            10 * 1024 * 1024, // 10MB
            allowedPrefixes,
            blockedClasses,
            true // strict mode
        );
        
        // Test allowed class
        Path allowedFile = createSerializedFile("allowed.design", 
            new ValidTestObject("Allowed", 100));
        
        DesignFileValidator.ValidationResult result = customValidator.validate(allowedFile);
        assertTrue(result.isValid(), "Should accept whitelisted class");
        
        // Test non-whitelisted class (using java.io.File which is not in our custom whitelist)
        Path notAllowedFile = createSerializedFile("notallowed.design", 
            new java.io.File("test.txt")); // java.io.File not whitelisted
        
        result = customValidator.validate(notAllowedFile);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Unauthorized class found"));
    }
    
    @Test
    @DisplayName("Should handle various GZIP compression levels")
    void testGzipCompressionLevels() throws IOException {
        // Test different GZIP compression levels
        for (int level = Deflater.NO_COMPRESSION; level <= Deflater.BEST_COMPRESSION; level++) {
            Path compressedFile = tempDir.resolve("compressed_level_" + level + ".design");
            final int compressionLevel = level; // Make it final for inner class
            
            try (FileOutputStream fos = new FileOutputStream(compressedFile.toFile());
                 GZIPOutputStream gzos = new GZIPOutputStream(fos) {{
                     def.setLevel(compressionLevel);
                 }};
                 ObjectOutputStream oos = new ObjectOutputStream(gzos)) {
                
                oos.writeObject(new ValidTestObject("Test", level));
            }
            
            DesignFileValidator.ValidationResult result = lenientValidator.validate(compressedFile);
            assertTrue(result.isValid(), "Should accept GZIP with compression level " + level);
        }
    }
    
    @Test
    @DisplayName("Should detect nested malicious objects")
    void testNestedMaliciousObjects() throws IOException {
        // Create object with nested non-whitelisted object
        ValidTestObject outer = new ValidTestObject("Outer", 1);
        outer.nested = new java.net.Socket(); // java.net.Socket not whitelisted
        
        Path nestedMaliciousFile = createSerializedFile("nested_malicious.design", outer);
        
        DesignFileValidator.ValidationResult result = validator.validate(nestedMaliciousFile);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Unauthorized class found"));
    }
    
    // Helper method to create a serialized file
    private Path createSerializedFile(String filename, Object object) throws IOException {
        Path file = tempDir.resolve(filename);
        
        try (FileOutputStream fos = new FileOutputStream(file.toFile());
             GZIPOutputStream gzos = new GZIPOutputStream(fos);
             ObjectOutputStream oos = new ObjectOutputStream(gzos)) {
            
            oos.writeObject(object);
        }
        
        return file;
    }
    
    // Test class representing a valid object
    static class ValidTestObject implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String data;
        private int value;
        Object nested;
        
        public ValidTestObject(String data, int value) {
            this.data = data;
            this.value = value;
        }
    }
    
    // Test class simulating malicious object (for testing purposes only)
    static class MaliciousObject implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String className;
        
        public MaliciousObject(String className) {
            this.className = className;
        }
        
        private void writeObject(ObjectOutputStream out) throws IOException {
            // Simulate malicious class reference in serialization
            out.defaultWriteObject();
        }
    }
    
    // Additional test for performance with large files
    @Test
    @DisplayName("Should validate large files efficiently")
    void testLargeFilePerformance() throws IOException {
        // Create a moderately large valid file (5MB compressed)
        Path largeFile = tempDir.resolve("large_valid.design");
        
        try (FileOutputStream fos = new FileOutputStream(largeFile.toFile());
             GZIPOutputStream gzos = new GZIPOutputStream(fos);
             ObjectOutputStream oos = new ObjectOutputStream(gzos)) {
            
            // Write many objects
            for (int i = 0; i < 10000; i++) {
                oos.writeObject(new ValidTestObject("Data " + i, i));
            }
        }
        
        long startTime = System.currentTimeMillis();
        DesignFileValidator.ValidationResult result = lenientValidator.validate(largeFile);
        long endTime = System.currentTimeMillis();
        
        assertTrue(result.isValid(), "Should validate large file successfully");
        assertTrue((endTime - startTime) < 5000, "Validation should complete within 5 seconds");
    }
}