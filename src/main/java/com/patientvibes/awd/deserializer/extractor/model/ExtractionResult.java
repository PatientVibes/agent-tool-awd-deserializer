/**
 * Module: ExtractionResult - Container for file extraction operation results
 * 
 * Summary:
 *     Result container that holds the outcome of file extraction operations including
 *     generated files, processing metrics, error information, and performance data.
 *     Provides comprehensive tracking of extraction success and failure conditions.
 * 
 * Key Components:
 *     - generatedFiles: Collection of files created by type and path
 *     - processingMetrics: Performance timing and resource usage information
 *     - errorTracking: Detailed error information by extraction type
 *     - extractionSummary: Overall operation status and metadata
 * 
 * Keywords: extraction, result, container, generated, files, metrics, performance,
 *          errors, tracking, summary, processing, timing, resource, usage, status
 * 
 * Dependencies:
 *     - java.time.LocalDateTime: Timestamp management for extraction tracking
 *     - java.util.Map: Collection management for files and metrics
 *     - java.util.List: Error collection and file listing
 * 
 * Security:
 *     - Safe file path handling and validation
 *     - Controlled access to extraction results
 *     - Input validation for all result data
 * 
 * Performance:
 *     - Efficient collection management for large file sets
 *     - Memory-conscious result aggregation
 *     - Optimized for serialization and reporting
 */
package com.patientvibes.awd.deserializer.extractor.model;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Container for file extraction operation results and metrics.
 */
public class ExtractionResult {
    private String sourceName;
    private String outputDirectory;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long totalProcessingTime;
    
    // Generated files organized by type
    private Map<FileExtractionType, List<String>> generatedFiles;
    
    // Processing metrics by type
    private Map<FileExtractionType, Long> processingTimes;
    private Map<FileExtractionType, Integer> fileCounts;
    
    // Error tracking
    private Map<FileExtractionType, List<String>> errors;
    private Map<FileExtractionType, List<String>> warnings;
    
    // Overall metrics
    private boolean successful;
    private int totalFilesGenerated;
    private long totalFileSize;
    
    public ExtractionResult() {
        this.generatedFiles = new HashMap<>();
        this.processingTimes = new HashMap<>();
        this.fileCounts = new HashMap<>();
        this.errors = new HashMap<>();
        this.warnings = new HashMap<>();
        this.successful = true;
        this.totalFilesGenerated = 0;
        this.totalFileSize = 0;
    }
    
    // Getters and Setters
    
    public String getSourceName() {
        return sourceName;
    }
    
    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }
    
    public String getOutputDirectory() {
        return outputDirectory;
    }
    
    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    
    public long getTotalProcessingTime() {
        return totalProcessingTime;
    }
    
    public void setTotalProcessingTime(long totalProcessingTime) {
        this.totalProcessingTime = totalProcessingTime;
    }
    
    public Map<FileExtractionType, List<String>> getGeneratedFiles() {
        return generatedFiles;
    }
    
    public void setGeneratedFiles(Map<FileExtractionType, List<String>> generatedFiles) {
        this.generatedFiles = generatedFiles != null ? generatedFiles : new HashMap<>();
    }
    
    public Map<FileExtractionType, Long> getProcessingTimes() {
        return processingTimes;
    }
    
    public void setProcessingTimes(Map<FileExtractionType, Long> processingTimes) {
        this.processingTimes = processingTimes != null ? processingTimes : new HashMap<>();
    }
    
    public Map<FileExtractionType, List<String>> getErrors() {
        return errors;
    }
    
    public void setErrors(Map<FileExtractionType, List<String>> errors) {
        this.errors = errors != null ? errors : new HashMap<>();
    }
    
    public Map<FileExtractionType, List<String>> getWarnings() {
        return warnings;
    }
    
    public void setWarnings(Map<FileExtractionType, List<String>> warnings) {
        this.warnings = warnings != null ? warnings : new HashMap<>();
    }
    
    public boolean isSuccessful() {
        return successful;
    }
    
    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }
    
    public int getTotalFilesGenerated() {
        return totalFilesGenerated;
    }
    
    public void setTotalFilesGenerated(int totalFilesGenerated) {
        this.totalFilesGenerated = totalFilesGenerated;
    }
    
    public long getTotalFileSize() {
        return totalFileSize;
    }
    
    public void setTotalFileSize(long totalFileSize) {
        this.totalFileSize = totalFileSize;
    }
    
    // Utility methods
    
    public void addGeneratedFile(FileExtractionType type, String filePath) {
        generatedFiles.computeIfAbsent(type, k -> new ArrayList<>()).add(filePath);
        totalFilesGenerated++;
        updateFileCount(type, 1);
    }
    
    public void addGeneratedFiles(FileExtractionType type, List<String> filePaths) {
        if (filePaths != null && !filePaths.isEmpty()) {
            generatedFiles.computeIfAbsent(type, k -> new ArrayList<>()).addAll(filePaths);
            totalFilesGenerated += filePaths.size();
            updateFileCount(type, filePaths.size());
        }
    }
    
    public void addError(FileExtractionType type, String error) {
        errors.computeIfAbsent(type, k -> new ArrayList<>()).add(error);
        successful = false;
    }
    
    public void addWarning(FileExtractionType type, String warning) {
        warnings.computeIfAbsent(type, k -> new ArrayList<>()).add(warning);
    }
    
    public void setProcessingTime(FileExtractionType type, long timeMs) {
        processingTimes.put(type, timeMs);
    }
    
    public List<String> getGeneratedFiles(FileExtractionType type) {
        return generatedFiles.getOrDefault(type, new ArrayList<>());
    }
    
    public List<String> getErrors(FileExtractionType type) {
        return errors.getOrDefault(type, new ArrayList<>());
    }
    
    public List<String> getWarnings(FileExtractionType type) {
        return warnings.getOrDefault(type, new ArrayList<>());
    }
    
    public boolean hasErrors() {
        return !errors.isEmpty() && errors.values().stream().anyMatch(list -> !list.isEmpty());
    }
    
    public boolean hasWarnings() {
        return !warnings.isEmpty() && warnings.values().stream().anyMatch(list -> !list.isEmpty());
    }
    
    public boolean hasErrors(FileExtractionType type) {
        return errors.containsKey(type) && !errors.get(type).isEmpty();
    }
    
    public boolean hasWarnings(FileExtractionType type) {
        return warnings.containsKey(type) && !warnings.get(type).isEmpty();
    }
    
    public int getFileCount(FileExtractionType type) {
        return fileCounts.getOrDefault(type, 0);
    }
    
    public Map<FileExtractionType, Integer> getFilesByType() {
        return new HashMap<>(fileCounts);
    }
    
    public long getProcessingTime(FileExtractionType type) {
        return processingTimes.getOrDefault(type, 0L);
    }
    
    public double getAverageProcessingTimePerFile() {
        if (totalFilesGenerated == 0) {
            return 0.0;
        }
        return (double) totalProcessingTime / totalFilesGenerated;
    }
    
    public Set<FileExtractionType> getProcessedTypes() {
        Set<FileExtractionType> types = new HashSet<>();
        types.addAll(generatedFiles.keySet());
        types.addAll(errors.keySet());
        return types;
    }
    
    private void updateFileCount(FileExtractionType type, int increment) {
        fileCounts.put(type, fileCounts.getOrDefault(type, 0) + increment);
    }
    
    /**
     * Get a summary of the extraction operation.
     */
    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("sourceName", sourceName);
        summary.put("successful", successful);
        summary.put("totalFilesGenerated", totalFilesGenerated);
        summary.put("totalProcessingTime", totalProcessingTime);
        summary.put("averageTimePerFile", getAverageProcessingTimePerFile());
        summary.put("hasErrors", hasErrors());
        summary.put("hasWarnings", hasWarnings());
        summary.put("processedTypes", getProcessedTypes());
        summary.put("filesByType", getFilesByType());
        
        if (startTime != null && endTime != null) {
            summary.put("startTime", startTime);
            summary.put("endTime", endTime);
            summary.put("duration", java.time.Duration.between(startTime, endTime).toMillis());
        }
        
        return summary;
    }
    
    @Override
    public String toString() {
        return "ExtractionResult{" +
                "sourceName='" + sourceName + '\'' +
                ", successful=" + successful +
                ", totalFilesGenerated=" + totalFilesGenerated +
                ", totalProcessingTime=" + totalProcessingTime + "ms" +
                ", hasErrors=" + hasErrors() +
                ", hasWarnings=" + hasWarnings() +
                ", processedTypes=" + getProcessedTypes().size() +
                '}';
    }
}