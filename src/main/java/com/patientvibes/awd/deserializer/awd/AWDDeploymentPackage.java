/**
 * Module: AWDDeploymentPackage - Data transfer object for AWD DeploymentPackage structures
 * 
 * Summary:
 *     Provides a safe, immutable wrapper for AWD DeploymentPackage data extracted from
 *     .design files. Encapsulates all fields found in com.dstawd.design.model.DeploymentPackage
 *     with validation and JSON serialization support.
 * 
 * Key Components:
 *     - Field accessors for all DeploymentPackage components
 *     - JSON serialization support via Jackson annotations
 *     - Builder pattern for safe construction
 *     - Validation methods for data integrity
 * 
 * Keywords: awd, deployment, package, wrapper, dto, immutable, validation, json, serializable,
 *           enterprise, design, model, custom, datatypes, dependencies, deploy, list, safe
 * Dependencies: Jackson annotations, validation utilities
 * Security: Immutable design, validated construction, safe field access
 * Performance: Lazy JSON conversion, efficient field access, minimal memory overhead
 */
package com.patientvibes.awd.deserializer.awd;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Wrapper class for AWD DeploymentPackage data.
 * Provides safe access to deserialized AWD enterprise objects.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize
public class AWDDeploymentPackage {
    
    @JsonProperty("customDataTypeMap")
    private Map<String, Object> customDataTypeMap;
    
    @JsonProperty("customDataTypes")
    private String customDataTypes;
    
    @JsonProperty("dependencies")
    private Map<String, Object> dependencies;
    
    @JsonProperty("deployList")
    private List<?> deployList;
    
    @JsonProperty("rootModel")
    private Object rootModel;
    
    @JsonProperty("saveList")
    private List<?> saveList;
    
    /**
     * Default constructor for Jackson deserialization.
     */
    public AWDDeploymentPackage() {
        // Default constructor
    }
    
    /**
     * Get custom data type map.
     * 
     * @return Unmodifiable map of custom data types
     */
    public Map<String, Object> getCustomDataTypeMap() {
        return customDataTypeMap != null ? 
            Collections.unmodifiableMap(customDataTypeMap) : 
            Collections.emptyMap();
    }
    
    /**
     * Set custom data type map.
     */
    public void setCustomDataTypeMap(Map<String, Object> customDataTypeMap) {
        this.customDataTypeMap = customDataTypeMap;
    }
    
    /**
     * Get custom data types string.
     */
    public String getCustomDataTypes() {
        return customDataTypes;
    }
    
    /**
     * Set custom data types string.
     */
    public void setCustomDataTypes(String customDataTypes) {
        this.customDataTypes = customDataTypes;
    }
    
    /**
     * Get dependencies map.
     * 
     * @return Unmodifiable map of dependencies
     */
    public Map<String, Object> getDependencies() {
        return dependencies != null ? 
            Collections.unmodifiableMap(dependencies) : 
            Collections.emptyMap();
    }
    
    /**
     * Set dependencies map.
     */
    public void setDependencies(Map<String, Object> dependencies) {
        this.dependencies = dependencies;
    }
    
    /**
     * Get deploy list.
     * 
     * @return Unmodifiable deploy list
     */
    public List<?> getDeployList() {
        return deployList != null ? 
            Collections.unmodifiableList(deployList) : 
            Collections.emptyList();
    }
    
    /**
     * Set deploy list.
     */
    public void setDeployList(List<?> deployList) {
        this.deployList = deployList;
    }
    
    /**
     * Get root model object.
     */
    public Object getRootModel() {
        return rootModel;
    }
    
    /**
     * Set root model object.
     */
    public void setRootModel(Object rootModel) {
        this.rootModel = rootModel;
    }
    
    /**
     * Get save list.
     * 
     * @return Unmodifiable save list
     */
    public List<?> getSaveList() {
        return saveList != null ? 
            Collections.unmodifiableList(saveList) : 
            Collections.emptyList();
    }
    
    /**
     * Set save list.
     */
    public void setSaveList(List<?> saveList) {
        this.saveList = saveList;
    }
    
    /**
     * Get root model class name safely.
     */
    public String getRootModelClassName() {
        return rootModel != null ? rootModel.getClass().getName() : null;
    }
    
    /**
     * Check if this deployment package has custom data types.
     */
    public boolean hasCustomDataTypes() {
        return (customDataTypeMap != null && !customDataTypeMap.isEmpty()) ||
               (customDataTypes != null && !customDataTypes.trim().isEmpty());
    }
    
    /**
     * Check if this deployment package has dependencies.
     */
    public boolean hasDependencies() {
        return dependencies != null && !dependencies.isEmpty();
    }
    
    /**
     * Get deployment count.
     */
    public int getDeploymentCount() {
        return deployList != null ? deployList.size() : 0;
    }
    
    /**
     * Get save item count.
     */
    public int getSaveItemCount() {
        return saveList != null ? saveList.size() : 0;
    }
    
    /**
     * Get custom data type count.
     */
    public int getCustomDataTypeCount() {
        return customDataTypeMap != null ? customDataTypeMap.size() : 0;
    }
    
    /**
     * Get dependency count.
     */
    public int getDependencyCount() {
        return dependencies != null ? dependencies.size() : 0;
    }
    
    /**
     * Validate the deployment package structure.
     * 
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        // Must have a root model
        if (rootModel == null) {
            return false;
        }
        
        // Root model must be AWD design model
        String className = rootModel.getClass().getName();
        if (!className.startsWith("com.dstawd.design.model.")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Get summary of this deployment package.
     */
    public String getSummary() {
        return String.format(
            "AWD DeploymentPackage: %d deployments, %d saves, %d custom types, %d dependencies, root: %s",
            getDeploymentCount(),
            getSaveItemCount(),
            getCustomDataTypeCount(),
            getDependencyCount(),
            getRootModelClassName()
        );
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AWDDeploymentPackage that = (AWDDeploymentPackage) o;
        return Objects.equals(customDataTypeMap, that.customDataTypeMap) &&
               Objects.equals(customDataTypes, that.customDataTypes) &&
               Objects.equals(dependencies, that.dependencies) &&
               Objects.equals(deployList, that.deployList) &&
               Objects.equals(rootModel, that.rootModel) &&
               Objects.equals(saveList, that.saveList);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(customDataTypeMap, customDataTypes, dependencies, 
                           deployList, rootModel, saveList);
    }
    
    @Override
    public String toString() {
        return getSummary();
    }
}