package com.patientvibes.awd.deserializer.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ByteUtils utility methods.
 */
public class ByteUtilsTest {
    
    @Test
    void testFormatBytes_SmallValues() {
        assertEquals("0 bytes", ByteUtils.formatBytes(0));
        assertEquals("1 bytes", ByteUtils.formatBytes(1));
        assertEquals("1023 bytes", ByteUtils.formatBytes(1023));
    }
    
    @Test
    void testFormatBytes_KilobyteValues() {
        assertEquals("1.00 KB", ByteUtils.formatBytes(1024));
        assertEquals("1.50 KB", ByteUtils.formatBytes(1536));
        assertEquals("1023.00 KB", ByteUtils.formatBytes(1024 * 1023));
    }
    
    @Test
    void testFormatBytes_MegabyteValues() {
        assertEquals("1.00 MB", ByteUtils.formatBytes(1024 * 1024));
        assertEquals("2.50 MB", ByteUtils.formatBytes((long)(2.5 * 1024 * 1024)));
        assertEquals("105.00 MB", ByteUtils.formatBytes(105L * 1024 * 1024));
    }
    
    @Test
    void testFormatBytes_NegativeValues() {
        assertThrows(IllegalArgumentException.class, () -> ByteUtils.formatBytes(-1));
        assertThrows(IllegalArgumentException.class, () -> ByteUtils.formatBytes(-1000));
    }
    
    @Test
    void testFormatBytesDetailed_GigabyteValues() {
        long oneGB = 1024L * 1024 * 1024;
        assertEquals("1.00 GB", ByteUtils.formatBytesDetailed(oneGB));
        assertEquals("2.50 GB", ByteUtils.formatBytesDetailed((long)(2.5 * oneGB)));
    }
    
    @Test
    void testConvertToMB() {
        assertEquals(0.0, ByteUtils.convertToMB(0), 0.001);
        assertEquals(1.0, ByteUtils.convertToMB(1024 * 1024), 0.001);
        assertEquals(2.5, ByteUtils.convertToMB((long)(2.5 * 1024 * 1024)), 0.001);
    }
    
    @Test
    void testConvertMBToBytes() {
        assertEquals(0L, ByteUtils.convertMBToBytes(0.0));
        assertEquals(1024L * 1024, ByteUtils.convertMBToBytes(1.0));
        assertEquals(Math.round(2.5 * 1024 * 1024), ByteUtils.convertMBToBytes(2.5));
    }
    
    @Test
    void testConvertToMB_NegativeValues() {
        assertThrows(IllegalArgumentException.class, () -> ByteUtils.convertToMB(-1));
    }
    
    @Test
    void testConvertMBToBytes_NegativeValues() {
        assertThrows(IllegalArgumentException.class, () -> ByteUtils.convertMBToBytes(-1.0));
    }
}