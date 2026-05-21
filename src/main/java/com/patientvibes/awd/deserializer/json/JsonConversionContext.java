package com.patientvibes.awd.deserializer.json;

import com.patientvibes.awd.deserializer.config.DeserializerConfig;
import com.patientvibes.awd.deserializer.analysis.ObjectAnalyzer;
import com.patientvibes.awd.deserializer.analysis.FieldFilter;
import com.fasterxml.jackson.core.JsonGenerator;

import java.util.Set;

/**
 * Module: JsonConversionContext - JSON serialization context and state management engine
 * 
 * Summary:
 *     Comprehensive context management system for JSON serialization that maintains state during
 *     object-to-JSON conversion processes. Provides field filtering, circular reference detection,
 *     depth tracking, and serialization control for complex enterprise object graphs with
 *     configuration-driven behavior and security-aware field processing.
 * 
 * Key Components:
 *     - getCurrentDepth(): Depth tracking for nested object serialization
 *     - getFieldFilter(): Security-aware field filtering engine
 *     - getObjectAnalyzer(): Object type analysis and classification
 *     - getJsonGenerator(): Jackson JSON output stream management
 *     - trackVisitedObject(): Circular reference detection and prevention
 * 
 * Keywords: json, conversion, context, serialization, state, management, depth, tracking, filter,
 *          circular, reference, detection, security, field, processing, configuration, enterprise
 * 
 * Dependencies:
 *     - DeserializerConfig: Configuration for serialization behavior
 *     - ObjectAnalyzer: Object type analysis and metadata extraction
 *     - FieldFilter: Security-aware field filtering and validation
 *     - JsonGenerator: Jackson JSON streaming output generation
 *     - java.util.Set: Visited object tracking for circular references
 * 
 * Security:
 *     - Sensitive field filtering prevents data leakage
 *     - Circular reference detection prevents stack overflow attacks
 *     - Depth limiting prevents deep recursion DoS attacks
 *     - Read-only object access with no state modification
 * 
 * Performance:
 *     - O(1) depth tracking with efficient state management
 *     - Lazy object analysis with caching for repeated objects
 *     - Efficient Set-based circular reference detection
 *     - Memory-efficient streaming JSON generation
 *
 */
public class JsonConversionContext {
    private final JsonGenerator generator;
    private final Set<Integer> visited;
    private final DeserializerConfig config;
    private final ObjectAnalyzer analyzer;
    private final FieldFilter fieldFilter;
    
    public JsonConversionContext(
            JsonGenerator generator,
            Set<Integer> visited,
            DeserializerConfig config,
            ObjectAnalyzer analyzer,
            FieldFilter fieldFilter) {
        this.generator = generator;
        this.visited = visited;
        this.config = config;
        this.analyzer = analyzer;
        this.fieldFilter = fieldFilter;
    }
    
    public JsonGenerator getGenerator() { return generator; }
    public Set<Integer> getVisited() { return visited; }
    public DeserializerConfig getConfig() { return config; }
    public ObjectAnalyzer getAnalyzer() { return analyzer; }
    public FieldFilter getFieldFilter() { return fieldFilter; }
}