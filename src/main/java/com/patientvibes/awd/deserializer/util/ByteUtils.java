package com.patientvibes.awd.deserializer.util;

/**
 * Module: ByteUtils - Byte formatting and manipulation utilities
 * 
 * Summary:
 *     Centralized utilities for byte operations including human-readable formatting,
 *     size calculations, and memory unit conversions. Consolidates duplicate formatting
 *     logic across multiple deserializer components.
 * 
 * Key Components:
 *     - formatBytes(): Human-readable byte size formatting (bytes/KB/MB)
 *     - formatBytesDetailed(): Extended formatting with GB support
 *     - convertToMB(): Byte to megabyte conversion utility
 * 
 * Keywords: bytes, formatting, utility, memory, size, conversion, human-readable, deduplication, consolidation, helper
 * Dependencies: None (pure utility class)
 * Security: Input validation for negative values
 * Performance: Optimized for frequent formatting operations
 */
public final class ByteUtils {
    
    private static final long KB = 1024;
    private static final long MB = KB * 1024;
    private static final long GB = MB * 1024;
    
    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private ByteUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * Format bytes to human-readable string with appropriate unit (bytes/KB/MB).
     * 
     * @param bytes the number of bytes to format
     * @return formatted string representation
     * @throws IllegalArgumentException if bytes is negative
     */
    public static String formatBytes(long bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("Bytes cannot be negative: " + bytes);
        }
        
        if (bytes < KB) {
            return bytes + " bytes";
        } else if (bytes < MB) {
            return String.format("%.2f KB", bytes / (double) KB);
        } else {
            return String.format("%.2f MB", bytes / (double) MB);
        }
    }
    
    /**
     * Format bytes to human-readable string with extended unit support (bytes/KB/MB/GB).
     * 
     * @param bytes the number of bytes to format
     * @return formatted string representation with appropriate unit
     * @throws IllegalArgumentException if bytes is negative
     */
    public static String formatBytesDetailed(long bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("Bytes cannot be negative: " + bytes);
        }
        
        if (bytes < KB) {
            return bytes + " bytes";
        } else if (bytes < MB) {
            return String.format("%.2f KB", bytes / (double) KB);
        } else if (bytes < GB) {
            return String.format("%.2f MB", bytes / (double) MB);
        } else {
            return String.format("%.2f GB", bytes / (double) GB);
        }
    }
    
    /**
     * Convert bytes to megabytes with precision.
     * 
     * @param bytes the number of bytes to convert
     * @return megabytes as double
     * @throws IllegalArgumentException if bytes is negative
     */
    public static double convertToMB(long bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("Bytes cannot be negative: " + bytes);
        }
        return bytes / (double) MB;
    }
    
    /**
     * Convert megabytes to bytes.
     * 
     * @param megabytes the number of megabytes to convert
     * @return bytes as long
     * @throws IllegalArgumentException if megabytes is negative
     */
    public static long convertMBToBytes(double megabytes) {
        if (megabytes < 0) {
            throw new IllegalArgumentException("Megabytes cannot be negative: " + megabytes);
        }
        return Math.round(megabytes * MB);
    }
}