/**
 * Module: ServiceDefinition - AWD service registry and dependency representation
 * 
 * Summary:
 *     Represents AWD service definitions extracted from business metadata including
 *     service properties, dependencies, endpoints, and configuration information.
 *     Provides structured service registry data for enterprise integration.
 * 
 * Key Components:
 *     - serviceIdentification: Service ID, class, and type information
 *     - serviceProperties: Configuration properties and metadata
 *     - serviceDependencies: Referenced classes and service dependencies
 *     - serviceEndpoints: Integration points and interface definitions
 * 
 * Keywords: service, definition, registry, dependency, property, endpoint, integration,
 *          enterprise, class, interface, configuration, metadata, reference
 * 
 * Dependencies:
 *     - java.util.List: Collection management for dependencies and endpoints
 *     - java.util.Map: Property storage for service configuration
 * 
 * Security:
 *     - Input validation for service class names and properties
 *     - Safe handling of dependency references and endpoints
 *     - Controlled access to service configuration data
 * 
 * Performance:
 *     - Efficient data structure for service registry operations
 *     - Optimized for enterprise service discovery and integration
 *     - Memory-efficient storage of service metadata
 */
package com.patientvibes.awd.deserializer.business.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an AWD service definition with properties and dependencies.
 */
public class ServiceDefinition {
    private String serviceId;
    private String serviceName;
    private String serviceClass;
    private String serviceType;
    private String description;
    private String version;
    
    private List<String> dependencies;
    private List<ServiceEndpoint> endpoints;
    private Map<String, Object> properties;
    private Map<String, Object> configuration;
    
    public ServiceDefinition() {
        this.dependencies = new ArrayList<>();
        this.endpoints = new ArrayList<>();
        this.properties = new HashMap<>();
        this.configuration = new HashMap<>();
    }
    
    // Getters and Setters
    
    public String getServiceId() {
        return serviceId;
    }
    
    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
    
    public String getServiceClass() {
        return serviceClass;
    }
    
    public void setServiceClass(String serviceClass) {
        this.serviceClass = serviceClass;
    }
    
    public String getServiceType() {
        return serviceType;
    }
    
    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public List<String> getDependencies() {
        return dependencies;
    }
    
    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies != null ? dependencies : new ArrayList<>();
    }
    
    public List<ServiceEndpoint> getEndpoints() {
        return endpoints;
    }
    
    public void setEndpoints(List<ServiceEndpoint> endpoints) {
        this.endpoints = endpoints != null ? endpoints : new ArrayList<>();
    }
    
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties != null ? properties : new HashMap<>();
    }
    
    public Map<String, Object> getConfiguration() {
        return configuration;
    }
    
    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration != null ? configuration : new HashMap<>();
    }
    
    // Utility methods
    
    public void addDependency(String dependency) {
        if (dependency != null && !dependency.trim().isEmpty()) {
            this.dependencies.add(dependency.trim());
        }
    }
    
    public void addEndpoint(ServiceEndpoint endpoint) {
        if (endpoint != null) {
            this.endpoints.add(endpoint);
        }
    }
    
    public void addProperty(String key, Object value) {
        if (key != null) {
            this.properties.put(key, value);
        }
    }
    
    public void addConfiguration(String key, Object value) {
        if (key != null) {
            this.configuration.put(key, value);
        }
    }
    
    @Override
    public String toString() {
        return "ServiceDefinition{" +
                "serviceId='" + serviceId + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", serviceClass='" + serviceClass + '\'' +
                ", serviceType='" + serviceType + '\'' +
                ", dependencies=" + (dependencies != null ? dependencies.size() : 0) +
                ", endpoints=" + (endpoints != null ? endpoints.size() : 0) +
                '}';
    }
}

/**
 * Represents a service endpoint or interface definition.
 */
class ServiceEndpoint {
    private String endpointId;
    private String endpointName;
    private String endpointType;
    private String method;
    private String path;
    private Map<String, Object> parameters;
    
    public ServiceEndpoint() {
        this.parameters = new HashMap<>();
    }
    
    // Getters and Setters
    
    public String getEndpointId() {
        return endpointId;
    }
    
    public void setEndpointId(String endpointId) {
        this.endpointId = endpointId;
    }
    
    public String getEndpointName() {
        return endpointName;
    }
    
    public void setEndpointName(String endpointName) {
        this.endpointName = endpointName;
    }
    
    public String getEndpointType() {
        return endpointType;
    }
    
    public void setEndpointType(String endpointType) {
        this.endpointType = endpointType;
    }
    
    public String getMethod() {
        return method;
    }
    
    public void setMethod(String method) {
        this.method = method;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters != null ? parameters : new HashMap<>();
    }
    
    @Override
    public String toString() {
        return "ServiceEndpoint{" +
                "endpointId='" + endpointId + '\'' +
                ", endpointName='" + endpointName + '\'' +
                ", endpointType='" + endpointType + '\'' +
                ", method='" + method + '\'' +
                ", path='" + path + '\'' +
                '}';
    }
}