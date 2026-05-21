/**
 * Module: WorkflowElement - Individual workflow component representation
 * 
 * Summary:
 *     Represents individual workflow elements and activities extracted from AWD processes.
 *     Contains element metadata, properties, and relationships for workflow analysis
 *     and business process documentation generation.
 * 
 * Key Components:
 *     - elementIdentification: Element type, ID, and name information
 *     - elementProperties: Configuration properties and metadata
 *     - elementRelationships: Connections to other workflow elements
 *     - elementActions: Activities and operations performed by the element
 * 
 * Keywords: workflow, element, activity, component, metadata, property, relationship,
 *          action, operation, configuration, business, process, analysis, documentation
 * 
 * Dependencies:
 *     - java.util.Map: Property storage for element configuration
 *     - java.util.List: Collection management for relationships and actions
 * 
 * Security:
 *     - Input validation for element properties and relationships
 *     - Safe handling of element configuration data
 *     - Controlled access to workflow element metadata
 * 
 * Performance:
 *     - Efficient data structure for workflow analysis operations
 *     - Optimized for business process documentation tools
 *     - Memory-efficient storage of element properties
 */
package com.patientvibes.awd.deserializer.business.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an individual workflow element or activity.
 */
public class WorkflowElement {
    private String elementId;
    private String elementName;
    private String elementType;
    private String description;
    private String category;
    
    private List<String> inputConnections;
    private List<String> outputConnections;
    private List<ElementAction> actions;
    private Map<String, Object> properties;
    private Map<String, Object> configuration;
    
    public WorkflowElement() {
        this.inputConnections = new ArrayList<>();
        this.outputConnections = new ArrayList<>();
        this.actions = new ArrayList<>();
        this.properties = new HashMap<>();
        this.configuration = new HashMap<>();
    }
    
    // Getters and Setters
    
    public String getElementId() {
        return elementId;
    }
    
    public void setElementId(String elementId) {
        this.elementId = elementId;
    }
    
    public String getElementName() {
        return elementName;
    }
    
    public void setElementName(String elementName) {
        this.elementName = elementName;
    }
    
    public String getElementType() {
        return elementType;
    }
    
    public void setElementType(String elementType) {
        this.elementType = elementType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public List<String> getInputConnections() {
        return inputConnections;
    }
    
    public void setInputConnections(List<String> inputConnections) {
        this.inputConnections = inputConnections != null ? inputConnections : new ArrayList<>();
    }
    
    public List<String> getOutputConnections() {
        return outputConnections;
    }
    
    public void setOutputConnections(List<String> outputConnections) {
        this.outputConnections = outputConnections != null ? outputConnections : new ArrayList<>();
    }
    
    public List<ElementAction> getActions() {
        return actions;
    }
    
    public void setActions(List<ElementAction> actions) {
        this.actions = actions != null ? actions : new ArrayList<>();
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
    
    public void addInputConnection(String connectionId) {
        if (connectionId != null && !connectionId.trim().isEmpty()) {
            this.inputConnections.add(connectionId.trim());
        }
    }
    
    public void addOutputConnection(String connectionId) {
        if (connectionId != null && !connectionId.trim().isEmpty()) {
            this.outputConnections.add(connectionId.trim());
        }
    }
    
    public void addAction(ElementAction action) {
        if (action != null) {
            this.actions.add(action);
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
        return "WorkflowElement{" +
                "elementId='" + elementId + '\'' +
                ", elementName='" + elementName + '\'' +
                ", elementType='" + elementType + '\'' +
                ", category='" + category + '\'' +
                ", actions=" + (actions != null ? actions.size() : 0) +
                '}';
    }
}

/**
 * Represents an action or operation performed by a workflow element.
 */
class ElementAction {
    private String actionId;
    private String actionName;
    private String actionType;
    private String description;
    private Integer order;
    private Map<String, Object> parameters;
    
    public ElementAction() {
        this.parameters = new HashMap<>();
    }
    
    // Getters and Setters
    
    public String getActionId() {
        return actionId;
    }
    
    public void setActionId(String actionId) {
        this.actionId = actionId;
    }
    
    public String getActionName() {
        return actionName;
    }
    
    public void setActionName(String actionName) {
        this.actionName = actionName;
    }
    
    public String getActionType() {
        return actionType;
    }
    
    public void setActionType(String actionType) {
        this.actionType = actionType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Integer getOrder() {
        return order;
    }
    
    public void setOrder(Integer order) {
        this.order = order;
    }
    
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters != null ? parameters : new HashMap<>();
    }
    
    @Override
    public String toString() {
        return "ElementAction{" +
                "actionId='" + actionId + '\'' +
                ", actionName='" + actionName + '\'' +
                ", actionType='" + actionType + '\'' +
                ", order=" + order +
                '}';
    }
}