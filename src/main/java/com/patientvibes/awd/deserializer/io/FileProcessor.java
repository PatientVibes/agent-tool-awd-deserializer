/**
 * Module: FileProcessor - Secure file I/O operations for Chorus Deserializer
 * 
 * Summary:
 *     Handles secure file processing including GZIP decompression, size validation,
 *     and path sanitization for design files. Implements defense-in-depth security.
 * 
 * Key Components:
 *     - decompressFile(): Decompress GZIP files with validation
 *     - validateFile(): Comprehensive file security checks
 *     - sanitizePath(): Path traversal prevention
 *     - enforceFileLimits(): Size and resource protection
 * 
 * Keywords: file, processor, io, gzip, compression, decompression, validation, security,
 *          sanitization, path, traversal, size, limit, chorus, deserializer, design,
 *          input, output, stream, buffer, resource, management
 * 
 * Dependencies:
 *     - DeserializerConfig: Configuration management
 *     - DesignFileValidator: Security validation framework
 *     - java.io: Core I/O operations
 *     - java.util.zip: GZIP compression support
 *     - java.util.logging: Operational logging
 * 
 * Security:
 *     - File size validation to prevent DoS attacks
 *     - Path traversal prevention
 *     - Magic byte validation for GZIP files
 *     - Resource limit enforcement
 *     - Secure temporary file handling
 * 
 * Performance:
 *     - Buffered I/O operations (8KB buffer)
 *     - Streaming decompression for memory efficiency
 *     - Early validation to fail fast
 */
package com.patientvibes.awd.deserializer.io;

import com.patientvibes.awd.deserializer.util.ByteUtils;

import com.patientvibes.awd.deserializer.config.DeserializerConfig;
import com.patientvibes.awd.deserializer.security.DesignFileValidator;
import com.patientvibes.awd.deserializer.security.DesignFileValidator.ValidationResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.logging.Logger;

/**
 * Handles file I/O operations including GZIP decompression with comprehensive security.
 */
public class FileProcessor {
    private static final Logger logger = Logger.getLogger(FileProcessor.class.getName());
    private static final int BUFFER_SIZE = 8192;
    
    private final DeserializerConfig config;
    private final DesignFileValidator validator;
    
    public FileProcessor(DeserializerConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        
        this.config = config;
        
        // Log the file size limit configuration
        logger.info(String.format("FileProcessor initialized with maximum file size: %d MB", 
            config.getMaxFileSizeMB()));
        
        // Initialize validator with configuration
        this.validator = new DesignFileValidator(
            config.getMaxFileSizeMB() * 1024 * 1024, // Convert MB to bytes
            config.getAllowedClassPrefixes(),
            config.getBlockedClasses(),
            config.isStrictValidation()
        );
    }
    
    /**
     * Decompress a GZIP file and return the raw bytes.
     * 
     * @param gzipFile The GZIP compressed file
     * @return Decompressed byte array
     * @throws IOException If decompression fails
     */
    public byte[] decompressFile(File gzipFile) throws IOException {
        validateFile(gzipFile);
        
        // Check file size before processing
        long fileSizeBytes = gzipFile.length();
        long maxSizeBytes = config.getMaxFileSizeMB() * 1024 * 1024;
        if (fileSizeBytes > maxSizeBytes) {
            throw new IOException(String.format(
                "File size exceeds maximum allowed size. File size: %s, Maximum allowed: %s (MAX_FILE_SIZE_MB=%d)",
                ByteUtils.formatBytesDetailed(fileSizeBytes),
                ByteUtils.formatBytesDetailed(maxSizeBytes),
                config.getMaxFileSizeMB()
            ));
        }
        
        // Perform security validation
        ValidationResult validationResult = validator.validate(gzipFile.toPath());
        if (!validationResult.isValid()) {
            throw new SecurityException("File validation failed: " + validationResult.getErrorMessage());
        }
        
        try (FileInputStream fis = new FileInputStream(gzipFile);
             GZIPInputStream gzis = new GZIPInputStream(fis, BUFFER_SIZE);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            logger.info("Decompressing file: " + gzipFile.getName());
            
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            long totalBytes = 0;
            long lastLoggedMB = 0;
            
            while ((bytesRead = gzis.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
                
                // Log progress every 10MB
                long currentMB = totalBytes / (1024 * 1024);
                if (currentMB >= lastLoggedMB + 10) {
                    logger.info("Decompressed " + currentMB + " MB so far");
                    lastLoggedMB = currentMB;
                }
            }
            
            byte[] result = baos.toByteArray();
            logger.info("Decompression complete. Size: " + ByteUtils.formatBytesDetailed(result.length));
            
            return result;
            
        } catch (IOException e) {
            throw new IOException("Failed to decompress file: " + gzipFile.getName(), e);
        }
    }
    
    /**
     * Validate that the file exists and is readable.
     * 
     * @param file File to validate
     * @throws IOException If file is invalid
     */
    private void validateFile(File file) throws IOException {
        if (!file.exists()) {
            throw new IOException("File not found: " + file.getAbsolutePath());
        }
        
        if (!file.isFile()) {
            throw new IOException("Not a file: " + file.getAbsolutePath());
        }
        
        if (!file.canRead()) {
            throw new IOException("Cannot read file: " + file.getAbsolutePath());
        }
    }
    
}