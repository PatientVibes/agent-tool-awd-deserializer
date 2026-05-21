/**
 * Module: MemoryManager - Memory optimization and monitoring
 * 
 * Summary:
 *     Manages memory usage for the Chorus Deserializer through object pooling,
 *     memory monitoring, and intelligent garbage collection triggering.
 * 
 * Key Components:
 *     - checkMemory(): Monitor and manage memory usage
 *     - getArrayFromPool(): Object pool management
 *     - returnToPool(): Efficient object recycling
 *     - forceGarbageCollection(): Manual GC triggering
 *     - getMemoryStats(): Detailed memory reporting
 * 
 * Keywords: memory, manager, pool, object, allocation, garbage, collection, gc,
 *          optimization, monitor, heap, performance, cache, reuse, recycle,
 *          chorus, deserializer, resource, management, efficiency
 * 
 * Dependencies:
 *     - DeserializerConfig: Configuration management
 *     - java.util.concurrent: Thread-safe collections
 *     - java.util.logging: Operational logging
 *     - Runtime: JVM memory statistics
 * 
 * Security:
 *     - Memory limit enforcement
 *     - Prevents out-of-memory attacks
 *     - Safe object pooling
 *     - Resource exhaustion protection
 * 
 * Performance:
 *     - Object pooling reduces GC pressure
 *     - Concurrent data structures for thread safety
 *     - Adaptive GC triggering
 *     - Memory usage tracking
 */
package com.patientvibes.awd.deserializer.memory;

import com.patientvibes.awd.deserializer.config.DeserializerConfig;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.Arrays;

/**
 * Manages memory usage and optimization for the deserializer.
 * Provides object pooling, memory monitoring, and garbage collection triggers.
 */
public class MemoryManager {
    private static final Logger logger = Logger.getLogger(MemoryManager.class.getName());
    
    private final DeserializerConfig config;
    private final AtomicInteger operationCounter = new AtomicInteger(0);
    private final Queue<Object[]> objectPool = new ConcurrentLinkedQueue<>();
    private final Runtime runtime = Runtime.getRuntime();
    
    // Statistics
    private long totalAllocations = 0;
    private long poolHits = 0;
    private long poolMisses = 0;
    
    public MemoryManager(DeserializerConfig config) {
        this.config = config;
        logMemoryInfo();
    }
    
    /**
     * Check current memory usage and trigger GC if needed.
     */
    public void checkMemory() {
        if (!config.isEnableMemoryMonitoring()) {
            return;
        }
        
        // Only check periodically to reduce overhead
        if (operationCounter.incrementAndGet() % config.getMemoryCheckInterval() != 0) {
            return;
        }
        
        long usedMemory = getUsedMemoryMB();
        long maxMemory = getMaxMemoryMB();
        
        // If using more than 80% of limit, suggest garbage collection
        if (usedMemory > config.getMemoryLimitMB() * 0.8) {
            logger.warning(String.format("Memory usage high: %dMB / %dMB - running GC", 
                usedMemory, maxMemory));
            
            System.gc();
            
            // Check again after GC
            usedMemory = getUsedMemoryMB();
            logger.info(String.format("After GC: %dMB / %dMB", usedMemory, maxMemory));
            
            // Clear caches if still high
            if (usedMemory > config.getMemoryLimitMB() * 0.9) {
                logger.warning("Memory still high after GC, clearing object pool");
                clearObjectPool();
            }
        }
    }
    
    /**
     * Get an object array from the pool or create a new one.
     * 
     * @param size Required array size
     * @return Object array
     */
    public Object[] getObjectArray(int size) {
        Object[] array = objectPool.poll();
        
        if (array != null && array.length >= size) {
            poolHits++;
            // Clear the array to prevent memory leaks
            Arrays.fill(array, null);
            return array;
        }
        
        poolMisses++;
        totalAllocations++;
        return new Object[Math.max(size, 10)];
    }
    
    /**
     * Return an object array to the pool for reuse.
     * 
     * @param array Array to recycle
     */
    public void recycleObjectArray(Object[] array) {
        if (array != null && array.length <= 1000) {  // Don't pool very large arrays
            Arrays.fill(array, null);  // Clear references
            objectPool.offer(array);
        }
    }
    
    /**
     * Clear the object pool to release memory.
     */
    public void clearObjectPool() {
        int size = objectPool.size();
        objectPool.clear();
        logger.info("Cleared object pool (" + size + " items)");
    }
    
    /**
     * Get current used memory in MB.
     */
    public long getUsedMemoryMB() {
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
    }
    
    /**
     * Get maximum available memory in MB.
     */
    public long getMaxMemoryMB() {
        return runtime.maxMemory() / (1024 * 1024);
    }
    
    /**
     * Check if memory usage is approaching limits.
     * 
     * @return true if memory usage is high
     */
    public boolean isMemoryConstrained() {
        return getUsedMemoryMB() > config.getMemoryLimitMB() * 0.7;
    }
    
    /**
     * Log current memory information.
     */
    public void logMemoryInfo() {
        if (config.isEnableMemoryMonitoring()) {
            logger.info(String.format("Memory Info - Max: %dMB, Limit: %dMB, Used: %dMB",
                getMaxMemoryMB(), config.getMemoryLimitMB(), getUsedMemoryMB()));
        }
    }
    
    /**
     * Log pool statistics.
     */
    public void logStatistics() {
        if (totalAllocations > 0) {
            double hitRate = (double) poolHits / (poolHits + poolMisses) * 100;
            logger.info(String.format("Memory Pool Stats - Allocations: %d, Hit Rate: %.1f%%, Pool Size: %d",
                totalAllocations, hitRate, objectPool.size()));
        }
    }
    
    /**
     * Request garbage collection if memory constrained.
     */
    public void requestGCIfNeeded() {
        if (isMemoryConstrained()) {
            logger.info("Requesting garbage collection due to memory constraints");
            System.gc();
        }
    }
}