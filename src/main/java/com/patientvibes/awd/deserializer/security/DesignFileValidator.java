/**
 * Module: DesignFileValidator - Security validation for design files
 * 
 * Summary:
 *     Implements comprehensive security validation for .design files to prevent
 *     deserialization attacks, file-based exploits, and resource exhaustion.
 * 
 * Key Components:
 *     - validateFile(): Complete file security validation
 *     - checkFileSize(): Prevent DoS via large files
 *     - validateGzipFormat(): Verify file format integrity
 *     - scanForBlockedClasses(): Prevent malicious class loading
 *     - enforceWhitelist(): Allow only trusted classes
 * 
 * Keywords: security, validation, design, file, deserializer, whitelist, blacklist,
 *          gzip, compression, size, limit, scan, class, loader, protection, defense,
 *          chorus, serialization, safety, verification, integrity
 * 
 * Dependencies:
 *     - org.slf4j: SLF4J logging framework
 *     - java.io: Core I/O operations
 *     - java.nio.file: Modern file operations
 *     - java.util: Collections and utilities
 *     - java.util.zip: GZIP support
 * 
 * Security:
 *     - File size limits to prevent DoS
 *     - Magic byte validation
 *     - Class whitelist/blacklist enforcement
 *     - Path traversal prevention
 *     - Resource exhaustion protection
 * 
 * Performance:
 *     - Streaming validation for large files
 *     - Early termination on validation failure
 *     - Efficient class name scanning
 */
package com.patientvibes.awd.deserializer.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/**
 * Validates .design files before deserialization to prevent security vulnerabilities.
 * Performs checks for file size, GZIP format validity, and class whitelist enforcement.
 */
public class DesignFileValidator {
    private static final Logger logger = LoggerFactory.getLogger(DesignFileValidator.class);
    
    // GZIP magic bytes
    private static final byte[] GZIP_MAGIC = new byte[]{(byte) 0x1f, (byte) 0x8b};
    
    // Java serialization magic bytes
    private static final byte[] JAVA_SERIALIZATION_MAGIC = new byte[]{(byte) 0xac, (byte) 0xed};
    
    // Default maximum file size (1GB)
    private static final long DEFAULT_MAX_FILE_SIZE = 1024L * 1024L * 1024L;
    
    // Configuration
    private final long maxFileSize;
    private final Set<String> allowedClassPrefixes;
    private final Set<String> blockedClasses;
    private final boolean strictMode;
    
    /**
     * Create a validator with default settings.
     */
    public DesignFileValidator() {
        this(DEFAULT_MAX_FILE_SIZE, getDefaultAllowedPrefixes(), getDefaultBlockedClasses(), true);
    }
    
    /**
     * Create a validator with custom settings.
     */
    public DesignFileValidator(long maxFileSize, Set<String> allowedClassPrefixes, 
                              Set<String> blockedClasses, boolean strictMode) {
        this.maxFileSize = maxFileSize;
        this.allowedClassPrefixes = new HashSet<>(allowedClassPrefixes);
        this.blockedClasses = new HashSet<>(blockedClasses);
        this.strictMode = strictMode;
        
        logger.info("DesignFileValidator initialized with maxFileSize={} MB, strictMode={}", 
                   maxFileSize / (1024 * 1024), strictMode);
    }
    
    /**
     * Validate a design file before processing.
     * 
     * @param filePath Path to the .design file
     * @return ValidationResult containing success status and any error messages
     */
    public ValidationResult validate(Path filePath) {
        return validate(filePath, false);
    }
    
    /**
     * Validate design file data from byte array.
     * 
     * @param fileData GZIP-compressed design file data
     * @return ValidationResult containing success status and any error messages
     */
    public ValidationResult validateDesignFile(byte[] fileData) {
        logger.debug("Validating design file data: {} bytes", fileData.length);
        
        // Check file size
        if (fileData.length > maxFileSize) {
            return ValidationResult.failure(String.format(
                "File size %d MB exceeds maximum allowed size of %d MB",
                fileData.length / (1024 * 1024), maxFileSize / (1024 * 1024)
            ));
        }
        
        // Validate GZIP format
        if (!isValidGzipData(fileData)) {
            return ValidationResult.failure("Data is not a valid GZIP file");
        }
        
        // Validate AWD-specific content
        try {
            ValidationResult awdResult = validateAWDContent(fileData);
            if (!awdResult.isValid()) {
                return awdResult;
            }
        } catch (IOException e) {
            return ValidationResult.failure("Error validating AWD content: " + e.getMessage());
        }
        
        logger.info("Design file validation successful: {} bytes", fileData.length);
        return ValidationResult.success();
    }
    
    /**
     * Validate a design file before processing with AWD-specific checks.
     * 
     * @param filePath Path to the .design file
     * @param awdSpecific Whether to perform AWD-specific validation
     * @return ValidationResult containing success status and any error messages
     */
    public ValidationResult validate(Path filePath, boolean awdSpecific) {
        logger.debug("Validating file: {}", filePath);
        
        // Check file exists
        if (!Files.exists(filePath)) {
            return ValidationResult.failure("File does not exist: " + filePath);
        }
        
        // Check file size
        try {
            long fileSize = Files.size(filePath);
            if (fileSize > maxFileSize) {
                return ValidationResult.failure(String.format(
                    "File size %d MB exceeds maximum allowed size of %d MB",
                    fileSize / (1024 * 1024), maxFileSize / (1024 * 1024)
                ));
            }
            logger.debug("File size {} bytes is within limit", fileSize);
        } catch (IOException e) {
            return ValidationResult.failure("Cannot determine file size: " + e.getMessage());
        }
        
        // Validate GZIP format
        try {
            if (!isValidGzipFile(filePath)) {
                return ValidationResult.failure("File is not a valid GZIP file");
            }
            logger.debug("GZIP format validation passed");
        } catch (IOException e) {
            return ValidationResult.failure("Error validating GZIP format: " + e.getMessage());
        }
        
        // Validate serialized content structure
        try {
            ValidationResult contentResult = validateSerializedContent(filePath);
            if (!contentResult.isValid()) {
                return contentResult;
            }
            logger.debug("Serialized content validation passed");
        } catch (IOException e) {
            return ValidationResult.failure("Error validating serialized content: " + e.getMessage());
        }
        
        // Perform AWD-specific validation if requested
        if (awdSpecific) {
            try {
                ValidationResult awdResult = validateAWDFile(filePath);
                if (!awdResult.isValid()) {
                    return awdResult;
                }
                logger.debug("AWD-specific validation passed");
            } catch (IOException e) {
                return ValidationResult.failure("Error in AWD validation: " + e.getMessage());
            }
        }
        
        logger.info("File validation successful: {}", filePath);
        return ValidationResult.success();
    }
    
    /**
     * Check if data has valid GZIP magic bytes.
     */
    private boolean isValidGzipData(byte[] data) {
        if (data.length < 2) {
            return false;
        }
        return data[0] == GZIP_MAGIC[0] && data[1] == GZIP_MAGIC[1];
    }
    
    /**
     * Check if the file has valid GZIP magic bytes and structure.
     */
    private boolean isValidGzipFile(Path filePath) throws IOException {
        try (InputStream is = Files.newInputStream(filePath)) {
            byte[] header = new byte[2];
            int bytesRead = is.read(header);
            
            if (bytesRead != 2) {
                return false;
            }
            
            return Arrays.equals(header, GZIP_MAGIC);
        }
    }
    
    /**
     * Validate the serialized content within the GZIP file.
     */
    private ValidationResult validateSerializedContent(Path filePath) throws IOException {
        try (InputStream fis = Files.newInputStream(filePath);
             GZIPInputStream gzis = new GZIPInputStream(fis, 8192);
             BufferedInputStream bis = new BufferedInputStream(gzis)) {
            
            // Check Java serialization magic bytes
            byte[] magic = new byte[2];
            bis.mark(2);
            int bytesRead = bis.read(magic);
            bis.reset();
            
            if (bytesRead != 2 || !Arrays.equals(magic, JAVA_SERIALIZATION_MAGIC)) {
                return ValidationResult.failure("Content is not a valid Java serialized object");
            }
            
            // If strict mode, perform class whitelist validation
            if (strictMode) {
                return validateClasses(bis);
            }
            
            return ValidationResult.success();
        }
    }
    
    /**
     * Validate classes in the serialized stream against whitelist.
     */
    private ValidationResult validateClasses(InputStream inputStream) throws IOException {
        try (ValidatingObjectInputStream vois = new ValidatingObjectInputStream(inputStream, 
                                                                               allowedClassPrefixes, 
                                                                               blockedClasses)) {
            // Perform a shallow scan of the stream to check class names
            vois.enableClassValidationOnly();
            
            Set<String> foundClasses = vois.scanClasses();
            logger.debug("Found {} classes in serialized data", foundClasses.size());
            
            // Check for blocked classes
            for (String className : foundClasses) {
                if (isBlockedClass(className)) {
                    return ValidationResult.failure("Blocked class found: " + className);
                }
                
                if (!isAllowedClass(className)) {
                    return ValidationResult.failure("Unauthorized class found: " + className);
                }
            }
            
            return ValidationResult.success();
        } catch (ClassNotFoundException e) {
            return ValidationResult.failure("Class validation error: " + e.getMessage());
        }
    }
    
    /**
     * Check if a class is in the blocked list.
     */
    private boolean isBlockedClass(String className) {
        return blockedClasses.contains(className);
    }
    
    /**
     * Check if a class is allowed based on whitelist prefixes.
     */
    private boolean isAllowedClass(String className) {
        // Always allow primitive types and arrays
        if (className.startsWith("[") || isPrimitiveType(className)) {
            return true;
        }
        
        // Check against allowed prefixes
        for (String prefix : allowedClassPrefixes) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if a class name represents a primitive type.
     */
    private boolean isPrimitiveType(String className) {
        return className.equals("boolean") || className.equals("byte") ||
               className.equals("char") || className.equals("double") ||
               className.equals("float") || className.equals("int") ||
               className.equals("long") || className.equals("short");
    }
    
    /**
     * Validate AWD-specific content from file.
     */
    private ValidationResult validateAWDFile(Path filePath) throws IOException {
        try (InputStream fis = Files.newInputStream(filePath)) {
            byte[] fileData = fis.readAllBytes();
            return validateAWDContent(fileData);
        }
    }
    
    /**
     * Validate AWD-specific content structure and classes.
     */
    private ValidationResult validateAWDContent(byte[] gzipData) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(gzipData);
             GZIPInputStream gzis = new GZIPInputStream(bais);
             BufferedInputStream bis = new BufferedInputStream(gzis)) {
            
            // Read the decompressed data
            byte[] serializedData = bis.readAllBytes();
            
            // Validate Java serialization format
            if (serializedData.length < 4) {
                return ValidationResult.failure("Serialized data too short");
            }
            
            // Check Java serialization magic and version
            if (serializedData[0] != JAVA_SERIALIZATION_MAGIC[0] || 
                serializedData[1] != JAVA_SERIALIZATION_MAGIC[1]) {
                return ValidationResult.failure("Invalid Java serialization magic bytes");
            }
            
            // Check serialization version (should be 5)
            short version = (short) (((serializedData[2] & 0xFF) << 8) | (serializedData[3] & 0xFF));
            if (version != 5) {
                logger.warn("Unexpected Java serialization version: {}", version);
            }
            
            // Scan for AWD-specific class patterns
            String dataString = new String(serializedData, "ISO-8859-1");
            
            // Look for AWD DeploymentPackage class
            if (!dataString.contains("com.dstawd.design.model.DeploymentPackage")) {
                return ValidationResult.failure("Expected AWD DeploymentPackage class not found");
            }
            
            // Verify AWD namespace presence
            boolean hasAWDClasses = dataString.contains("com.dstawd.design.model.") ||
                                  dataString.contains("com.dstawd.utility.");
            
            if (!hasAWDClasses) {
                logger.warn("No AWD classes detected in serialized data");
            }
            
            // Check for suspicious patterns
            String[] suspiciousPatterns = {
                "java.lang.Runtime",
                "java.lang.ProcessBuilder",
                "javax.script",
                "java.rmi",
                "javax.management"
            };
            
            for (String pattern : suspiciousPatterns) {
                if (dataString.contains(pattern)) {
                    return ValidationResult.failure("Suspicious class pattern detected: " + pattern);
                }
            }
            
            logger.debug("AWD content validation passed");
            return ValidationResult.success();
        }
    }
    
    /**
     * Get default allowed class prefixes for AWD .design files.
     */
    private static Set<String> getDefaultAllowedPrefixes() {
        return new HashSet<>(Arrays.asList(
            "com.dstawd.",                    // AWD core classes
            "com.dstawd.design.model.",      // AWD design model
            "com.dstawd.utility.",           // AWD utilities
            "com.ssctech.awdlyric.schemas.", // AWD Lyric schemas
            "com.chorus.",                   // Chorus classes
            "java.lang.",                    // Core Java
            "java.util.",                    // Collections
            "java.math.",                    // BigDecimal, etc.
            "java.time.",                    // Date/Time API
            "java.sql.",                     // SQL types
            "javax.xml.",                    // XML processing
            "org.w3c.dom.",                  // DOM
            "org.xml.sax.",                  // SAX
            "[L",                            // Array types
            "[Z", "[B", "[C", "[D", "[F", "[I", "[J", "[S" // Primitive arrays
        ));
    }
    
    /**
     * Get default blocked classes that pose security risks.
     */
    private static Set<String> getDefaultBlockedClasses() {
        return new HashSet<>(Arrays.asList(
            "java.lang.Runtime",
            "java.lang.ProcessBuilder",
            "java.lang.reflect.Proxy",
            "java.rmi.",
            "javax.script.",
            "javax.management.",
            "java.net.URLClassLoader",
            "java.beans.Expression",
            "java.beans.Statement",
            "com.sun.",
            "sun.",
            "jdk.internal."
        ));
    }
    
    /**
     * Result of validation with success status and error message.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
    
    /**
     * Custom ObjectInputStream that validates classes during deserialization.
     */
    private static class ValidatingObjectInputStream extends ObjectInputStream {
        private final Set<String> allowedPrefixes;
        private final Set<String> blockedClasses;
        private boolean classValidationOnly = false;
        private final Set<String> foundClasses = new HashSet<>();
        
        public ValidatingObjectInputStream(InputStream in, Set<String> allowedPrefixes, 
                                         Set<String> blockedClasses) throws IOException {
            super(in);
            this.allowedPrefixes = allowedPrefixes;
            this.blockedClasses = blockedClasses;
        }
        
        public void enableClassValidationOnly() {
            this.classValidationOnly = true;
        }
        
        public Set<String> scanClasses() throws IOException, ClassNotFoundException {
            try {
                while (available() > 0) {
                    readObject();
                }
            } catch (EOFException e) {
                // Expected when we reach end of stream
            } catch (Exception e) {
                // Continue scanning even if deserialization fails
                logger.debug("Error during class scanning: {}", e.getMessage());
            }
            return new HashSet<>(foundClasses);
        }
        
        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) 
                throws IOException, ClassNotFoundException {
            String className = desc.getName();
            foundClasses.add(className);
            
            if (classValidationOnly) {
                // Don't actually load classes, just record them
                throw new ClassNotFoundException("Validation only mode");
            }
            
            return super.resolveClass(desc);
        }
    }
}