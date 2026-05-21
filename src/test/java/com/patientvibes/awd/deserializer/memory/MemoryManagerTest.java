package com.patientvibes.awd.deserializer.memory;

import com.patientvibes.awd.deserializer.config.DeserializerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MemoryManagerTest {
    
    private MemoryManager memoryManager;
    private DeserializerConfig config;
    
    @BeforeEach
    void setUp() {
        config = DeserializerConfig.fromSystemProperties();
        memoryManager = new MemoryManager(config);
    }
    
    @Test
    void testObjectPooling() {
        // Request an array
        Object[] array1 = memoryManager.getObjectArray(10);
        assertNotNull(array1);
        assertEquals(10, array1.length);
        
        // Fill with test data
        for (int i = 0; i < array1.length; i++) {
            array1[i] = "test" + i;
        }
        
        // Recycle the array
        memoryManager.recycleObjectArray(array1);
        
        // Request another array - should get the recycled one
        Object[] array2 = memoryManager.getObjectArray(10);
        assertNotNull(array2);
        
        // Verify array was cleared
        for (int i = 0; i < array2.length; i++) {
            assertNull(array2[i]);
        }
    }
    
    @Test
    void testMemoryMetrics() {
        long usedMemory = memoryManager.getUsedMemoryMB();
        long maxMemory = memoryManager.getMaxMemoryMB();
        
        assertTrue(usedMemory >= 0);
        assertTrue(maxMemory > 0);
        assertTrue(usedMemory <= maxMemory);
    }
    
    @Test
    void testLargeArrayNotPooled() {
        // Large arrays should not be pooled
        Object[] largeArray = new Object[2000];
        memoryManager.recycleObjectArray(largeArray);
        
        // Request a small array - should not get the large one
        Object[] smallArray = memoryManager.getObjectArray(10);
        assertTrue(smallArray.length < 2000);
    }
}