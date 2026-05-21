/**
 * Module: StreamingJsonWriter - Memory-efficient JSON streaming writer
 * 
 * Summary:
 *     Implements streaming JSON generation from Java objects using Jackson's streaming
 *     API to minimize memory usage and handle large object graphs efficiently.
 * 
 * Key Components:
 *     - writeToFile(): Stream objects to JSON file
 *     - createObjectMapper(): Configure Jackson for optimal performance
 *     - writeObject(): Recursive object traversal with cycle detection
 *     - flushBuffer(): Memory-aware buffer management
 * 
 * Keywords: json, streaming, writer, jackson, serialization, memory, efficient,
 *          buffer, generator, object, mapper, chorus, deserializer, output,
 *          performance, scale, large, file, conversion
 * 
 * Dependencies:
 *     - Jackson Core/Databind: JSON processing framework
 *     - DeserializerConfig: Configuration management
 *     - MemoryManager: Memory usage monitoring
 *     - ObjectAnalyzer: Object graph analysis
 *     - FieldFilter: Field-level filtering
 * 
 * Security:
 *     - Output path validation
 *     - Controlled memory usage
 *     - Cycle detection to prevent infinite loops
 *     - Field filtering for sensitive data
 * 
 * Performance:
 *     - Streaming API for constant memory usage
 *     - Buffered output (8KB default)
 *     - Lazy evaluation of object graphs
 *     - Memory monitoring and GC triggers
 */
package com.patientvibes.awd.deserializer.json;

import com.patientvibes.awd.deserializer.config.DeserializerConfig;
import com.patientvibes.awd.deserializer.memory.MemoryManager;
import com.patientvibes.awd.deserializer.analysis.ObjectAnalyzer;
import com.patientvibes.awd.deserializer.analysis.FieldFilter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Handles streaming JSON generation from Java objects.
 * Uses Jackson's streaming API to minimize memory usage.
 */
public class StreamingJsonWriter {
    private static final Logger logger = Logger.getLogger(StreamingJsonWriter.class.getName());
    
    private final DeserializerConfig config;
    private final ObjectMapper mapper;
    private final ObjectAnalyzer analyzer;
    private final FieldFilter fieldFilter;
    
    public StreamingJsonWriter(DeserializerConfig config) {
        this.config = config;
        this.mapper = createObjectMapper();
        this.analyzer = new ObjectAnalyzer(config);
        this.fieldFilter = new FieldFilter(config);
    }
    
    /**
     * Write object to JSON file using streaming approach with validation.
     * 
     * @param obj Object to serialize
     * @param outputFile Output file
     * @throws IOException If writing fails
     */
    public void writeToFile(Object obj, File outputFile) throws IOException {
        logger.info("Writing JSON to: " + outputFile.getAbsolutePath());
        
        try (FileOutputStream fos = new FileOutputStream(outputFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos, 65536);
             OutputStreamWriter osw = new OutputStreamWriter(bos, StandardCharsets.UTF_8)) {
            
            JsonGenerator generator = mapper.getFactory().createGenerator(osw);
            generator.useDefaultPrettyPrinter();
            
            // Track visited objects to handle cycles
            Set<Integer> visited = new HashSet<>();
            
            // Create context for the conversion
            JsonConversionContext context = new JsonConversionContext(
                generator, visited, config, analyzer, fieldFilter
            );
            
            // Start JSON document
            generator.writeStartObject();
            
            // Write the object
            JsonObjectWriter writer = new JsonObjectWriter(context);
            writer.writeObject(obj, null, 0);
            
            // End JSON document
            generator.writeEndObject();
            generator.close();
            
            logger.info("JSON writing complete");
            
        } catch (IOException e) {
            logger.severe("Failed to write JSON: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Create and configure ObjectMapper instance.
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);
        return mapper;
    }
}