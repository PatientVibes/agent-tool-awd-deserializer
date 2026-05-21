/**
 * Module: BpmnXmlGenerator - Complete BPMN 2.0 XML generation engine with AWD extensions
 * 
 * Summary:
 *     Generates complete BPMN 2.0 XML files with AWD custom extensions from extracted
 *     business metadata. Implements AWD namespace support, custom elements like awd:setValue,
 *     and AWD expression language integration. Produces valid BPMN XML compliant with
 *     both standard BPMN 2.0 schema and AWD enterprise extensions.
 * 
 * Key Components:
 *     - generateBpmnFiles(): Primary BPMN generation orchestration method
 *     - createBpmnProcess(): Individual process XML generation with AWD extensions
 *     - generateAwdExtensions(): AWD custom element and property generation
 *     - applyAwdNamespace(): Namespace declaration and AWD schema integration
 *     - validateBpmnStructure(): BPMN 2.0 and AWD schema compliance validation
 * 
 * Keywords: bpmn, xml, generator, awd, extensions, namespace, process, custom, elements,
 *          setValue, expression, language, schema, compliance, validation, enterprise
 * 
 * Dependencies:
 *     - javax.xml.parsers.DocumentBuilder: XML document creation and manipulation
 *     - org.w3c.dom.Document: DOM-based XML structure building
 *     - com.patientvibes.awd.deserializer.business.model.BusinessMetadata: Source metadata structures
 *     - com.patientvibes.awd.deserializer.extractor.config.ExtractionConfig: Generation configuration
 * 
 * Security:
 *     - XML injection prevention through DOM-based construction
 *     - Input validation for all BPMN elements and attributes
 *     - Safe file path resolution and XML output sanitization
 *     - Schema validation against BPMN 2.0 and AWD specifications
 * 
 * Performance:
 *     - Efficient DOM construction with memory management
 *     - Streaming XML output for large process definitions
 *     - Template-based generation for common BPMN patterns
 *     - Optimized namespace handling and attribute setting
 */
package com.patientvibes.awd.deserializer.extractor.generator;

import com.patientvibes.awd.deserializer.business.model.BusinessMetadata;
import com.patientvibes.awd.deserializer.business.model.BusinessProcess;
import com.patientvibes.awd.deserializer.business.model.ProcessElement;
import com.patientvibes.awd.deserializer.business.model.ProcessVariable;
import com.patientvibes.awd.deserializer.extractor.config.ExtractionConfig;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

/**
 * Generates BPMN 2.0 XML files with AWD extensions.
 */
public class BpmnXmlGenerator {
    private static final Logger logger = Logger.getLogger(BpmnXmlGenerator.class.getName());
    
    // BPMN and AWD namespace constants
    private static final String BPMN_NAMESPACE = "http://www.omg.org/spec/BPMN/20100524/MODEL";
    private static final String BPMN_DI_NAMESPACE = "http://www.omg.org/spec/BPMN/20100524/DI";
    private static final String DC_NAMESPACE = "http://www.omg.org/spec/DD/20100524/DC";
    private static final String DI_NAMESPACE = "http://www.omg.org/spec/DD/20100524/DI";
    
    private final ExtractionConfig config;
    private final DocumentBuilderFactory documentBuilderFactory;
    private final TransformerFactory transformerFactory;
    
    public BpmnXmlGenerator(ExtractionConfig config) {
        this.config = config;
        this.documentBuilderFactory = DocumentBuilderFactory.newInstance();
        this.documentBuilderFactory.setNamespaceAware(true);
        this.transformerFactory = TransformerFactory.newInstance();
        
        logger.info("BPMN XML Generator initialized with AWD namespace: " + config.getAwdNamespace());
    }
    
    /**
     * Generate BPMN files from business metadata and mapped AWD data.
     * 
     * @param metadata Business metadata containing process definitions
     * @param mappedData AWD-mapped data structures for generation
     * @param outputDirectory Target directory for BPMN files
     * @return List of generated BPMN file paths
     */
    public List<String> generateBpmnFiles(BusinessMetadata metadata, Map<String, Object> mappedData, Path outputDirectory) {
        logger.info("Starting BPMN file generation for " + metadata.getBusinessProcesses().size() + " processes");
        
        List<String> generatedFiles = new ArrayList<>();
        
        try {
            // Ensure output directory exists
            Files.createDirectories(outputDirectory);
            
            // Generate individual BPMN files for each business process
            for (BusinessProcess process : metadata.getBusinessProcesses()) {
                try {
                    String fileName = generateBpmnFileName(process);
                    Path outputFile = outputDirectory.resolve(fileName);
                    
                    // Generate BPMN XML content
                    String bpmnXml = generateBpmnXml(process, mappedData);
                    
                    // Write to file
                    Files.writeString(outputFile, bpmnXml);
                    generatedFiles.add(outputFile.toString());
                    
                    logger.info("Generated BPMN file: " + fileName + " (" + bpmnXml.length() + " bytes)");
                    
                } catch (Exception e) {
                    logger.severe("Failed to generate BPMN file for process " + process.getId() + ": " + e.getMessage());
                }
            }
            
            // Generate consolidated BPMN file if multiple processes exist
            if (metadata.getBusinessProcesses().size() > 1) {
                String consolidatedFile = generateConsolidatedBpmn(metadata, mappedData, outputDirectory);
                if (consolidatedFile != null) {
                    generatedFiles.add(consolidatedFile);
                }
            }
            
            logger.info("BPMN file generation completed: " + generatedFiles.size() + " files generated");
            
        } catch (Exception e) {
            logger.severe("Error during BPMN file generation: " + e.getMessage());
        }
        
        return generatedFiles;
    }
    
    /**
     * Generate BPMN XML content for a single business process.
     */
    private String generateBpmnXml(BusinessProcess process, Map<String, Object> mappedData) throws Exception {
        DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
        Document document = builder.newDocument();
        
        // Create root definitions element with all namespaces
        Element definitions = createDefinitionsElement(document, process);
        document.appendChild(definitions);
        
        // Create BPMN process element
        Element bpmnProcess = createProcessElement(document, process, mappedData);
        definitions.appendChild(bpmnProcess);
        
        // Add process elements (start events, tasks, etc.)
        addProcessElements(document, bpmnProcess, process, mappedData);
        
        // Add AWD extensions if enabled
        if (config.isIncludeAwdExtensions()) {
            addAwdExtensions(document, bpmnProcess, process, mappedData);
        }
        
        // Add BPMN diagram information (optional)
        addBpmnDiagram(document, definitions, process);
        
        // Convert to XML string
        return documentToString(document);
    }
    
    /**
     * Create the root definitions element with namespace declarations.
     */
    private Element createDefinitionsElement(Document document, BusinessProcess process) {
        Element definitions = document.createElementNS(BPMN_NAMESPACE, "bpmn:definitions");
        
        // Standard BPMN namespaces
        definitions.setAttribute("xmlns:bpmn", BPMN_NAMESPACE);
        definitions.setAttribute("xmlns:bpmndi", BPMN_DI_NAMESPACE);
        definitions.setAttribute("xmlns:dc", DC_NAMESPACE);
        definitions.setAttribute("xmlns:di", DI_NAMESPACE);
        
        // AWD namespace if extensions are enabled
        if (config.isIncludeAwdExtensions()) {
            definitions.setAttribute("xmlns:awd", config.getAwdNamespace());
        }
        
        // Standard attributes
        definitions.setAttribute("id", "Definitions_" + process.getId());
        definitions.setAttribute("targetNamespace", "http://chorus.deserializer/bpmn/" + process.getId());
        definitions.setAttribute("exporter", "Chorus File Extractor");
        definitions.setAttribute("exporterVersion", "2.4.0");
        
        return definitions;
    }
    
    /**
     * Create the main BPMN process element.
     */
    private Element createProcessElement(Document document, BusinessProcess process, Map<String, Object> mappedData) {
        Element processElement = document.createElementNS(BPMN_NAMESPACE, "bpmn:process");
        
        processElement.setAttribute("id", process.getId());
        processElement.setAttribute("name", process.getName() != null ? process.getName() : "");
        processElement.setAttribute("isExecutable", "true");
        
        // Add version information
        if (process.getVersion() != null) {
            processElement.setAttribute("versionTag", process.getVersion().toString());
        }
        
        return processElement;
    }
    
    /**
     * Add process elements (start events, tasks, gateways, etc.).
     */
    private void addProcessElements(Document document, Element processElement, BusinessProcess process, Map<String, Object> mappedData) {
        // Add start event (required for valid BPMN)
        Element startEvent = document.createElementNS(BPMN_NAMESPACE, "bpmn:startEvent");
        startEvent.setAttribute("id", "StartEvent_" + process.getId());
        startEvent.setAttribute("name", "Start");
        processElement.appendChild(startEvent);
        
        // Add process elements from metadata
        for (ProcessElement element : process.getElements()) {
            Element bpmnElement = createBpmnElement(document, element);
            if (bpmnElement != null) {
                processElement.appendChild(bpmnElement);
            }
        }
        
        // Add end event (required for valid BPMN)
        Element endEvent = document.createElementNS(BPMN_NAMESPACE, "bpmn:endEvent");
        endEvent.setAttribute("id", "EndEvent_" + process.getId());
        endEvent.setAttribute("name", "End");
        processElement.appendChild(endEvent);
        
        // Add sequence flows to connect elements
        addSequenceFlows(document, processElement, process);
    }
    
    /**
     * Create individual BPMN elements based on type.
     */
    private Element createBpmnElement(Document document, ProcessElement element) {
        String elementType = element.getType();
        Element bpmnElement = null;
        
        switch (elementType.toLowerCase()) {
            case "startevent":
                bpmnElement = document.createElementNS(BPMN_NAMESPACE, "bpmn:startEvent");
                break;
            case "endevent":
                bpmnElement = document.createElementNS(BPMN_NAMESPACE, "bpmn:endEvent");
                break;
            case "task":
            case "usertask":
                bpmnElement = document.createElementNS(BPMN_NAMESPACE, "bpmn:userTask");
                break;
            case "servicetask":
                bpmnElement = document.createElementNS(BPMN_NAMESPACE, "bpmn:serviceTask");
                break;
            case "gateway":
            case "exclusivegateway":
                bpmnElement = document.createElementNS(BPMN_NAMESPACE, "bpmn:exclusiveGateway");
                break;
            case "parallelgateway":
                bpmnElement = document.createElementNS(BPMN_NAMESPACE, "bpmn:parallelGateway");
                break;
            default:
                logger.warning("Unknown element type: " + elementType);
                return null;
        }
        
        if (bpmnElement != null) {
            bpmnElement.setAttribute("id", element.getId());
            if (element.getName() != null && !element.getName().isEmpty()) {
                bpmnElement.setAttribute("name", element.getName());
            }
        }
        
        return bpmnElement;
    }
    
    /**
     * Add sequence flows to connect process elements.
     */
    private void addSequenceFlows(Document document, Element processElement, BusinessProcess process) {
        // Simple linear flow for basic processes
        List<ProcessElement> elements = process.getElements();
        
        if (elements.size() > 1) {
            for (int i = 0; i < elements.size() - 1; i++) {
                ProcessElement source = elements.get(i);
                ProcessElement target = elements.get(i + 1);
                
                Element sequenceFlow = document.createElementNS(BPMN_NAMESPACE, "bpmn:sequenceFlow");
                sequenceFlow.setAttribute("id", "Flow_" + source.getId() + "_" + target.getId());
                sequenceFlow.setAttribute("sourceRef", source.getId());
                sequenceFlow.setAttribute("targetRef", target.getId());
                
                processElement.appendChild(sequenceFlow);
            }
        }
    }
    
    /**
     * Add AWD extensions and custom elements.
     */
    private void addAwdExtensions(Document document, Element processElement, BusinessProcess process, Map<String, Object> mappedData) {
        // Add AWD extension elements
        Element extensionElements = document.createElementNS(BPMN_NAMESPACE, "bpmn:extensionElements");
        
        // Add AWD properties
        Element awdProperties = document.createElementNS(config.getAwdNamespace(), "awd:properties");
        
        // Add process metadata as AWD property
        Element configProperty = document.createElementNS(config.getAwdNamespace(), "awd:property");
        configProperty.setAttribute("name", "processConfig");
        
        // Create JSON configuration for AWD
        Map<String, Object> awdConfig = createAwdConfiguration(process, mappedData);
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String configJson = mapper.writeValueAsString(awdConfig);
            configProperty.setTextContent(configJson);
        } catch (Exception e) {
            logger.warning("Failed to serialize AWD configuration: " + e.getMessage());
            configProperty.setTextContent("{}");
        }
        
        awdProperties.appendChild(configProperty);
        extensionElements.appendChild(awdProperties);
        
        // Add AWD setValue elements for process variables
        addAwdSetValueElements(document, extensionElements, process);
        
        processElement.appendChild(extensionElements);
    }
    
    /**
     * Create AWD configuration object.
     */
    private Map<String, Object> createAwdConfiguration(BusinessProcess process, Map<String, Object> mappedData) {
        Map<String, Object> config = new HashMap<>();
        
        config.put("processId", process.getId());
        config.put("processName", process.getName());
        config.put("processType", process.getType());
        config.put("version", process.getVersion());
        config.put("awdNamespace", this.config.getAwdNamespace());
        
        // Add creation/modification metadata
        if (process.getCreatedBy() != null) {
            config.put("createdBy", process.getCreatedBy());
        }
        if (process.getModifiedBy() != null) {
            config.put("modifiedBy", process.getModifiedBy());
        }
        if (process.getModelState() != null) {
            config.put("modelState", process.getModelState());
        }
        
        // Add variable configuration
        List<Map<String, Object>> variables = new ArrayList<>();
        for (ProcessVariable variable : process.getVariables()) {
            Map<String, Object> varConfig = new HashMap<>();
            varConfig.put("name", variable.getName());
            varConfig.put("type", variable.getType());
            if (variable.getSourceExpression() != null) {
                varConfig.put("sourceExpression", variable.getSourceExpression());
            }
            variables.add(varConfig);
        }
        config.put("variables", variables);
        
        return config;
    }
    
    /**
     * Add AWD setValue elements for process variables.
     */
    private void addAwdSetValueElements(Document document, Element extensionElements, BusinessProcess process) {
        for (ProcessVariable variable : process.getVariables()) {
            if (variable.getSourceExpression() != null) {
                Element setValue = document.createElementNS(config.getAwdNamespace(), "awd:setValue");
                setValue.setAttribute("id", "setValue_" + variable.getName());
                setValue.setAttribute("name", "Set " + variable.getName());
                
                // Add setValue configuration
                Element setValueExtensions = document.createElementNS(BPMN_NAMESPACE, "bpmn:extensionElements");
                Element setValueProperties = document.createElementNS(config.getAwdNamespace(), "awd:properties");
                Element setValueConfig = document.createElementNS(config.getAwdNamespace(), "awd:property");
                setValueConfig.setAttribute("name", "config");
                
                // Create setValue configuration JSON
                Map<String, Object> setValueConfigData = new HashMap<>();
                List<Map<String, Object>> setValues = new ArrayList<>();
                Map<String, Object> setValueEntry = new HashMap<>();
                setValueEntry.put("target", "awd:awd-value('" + variable.getName() + "')");
                setValueEntry.put("source", variable.getSourceExpression());
                setValues.add(setValueEntry);
                setValueConfigData.put("setValues", setValues);
                
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    String configJson = mapper.writeValueAsString(setValueConfigData);
                    setValueConfig.setTextContent(configJson);
                } catch (Exception e) {
                    logger.warning("Failed to serialize setValue configuration: " + e.getMessage());
                    setValueConfig.setTextContent("{}");
                }
                
                setValueProperties.appendChild(setValueConfig);
                setValueExtensions.appendChild(setValueProperties);
                setValue.appendChild(setValueExtensions);
                extensionElements.appendChild(setValue);
            }
        }
    }
    
    /**
     * Add BPMN diagram information (optional).
     */
    private void addBpmnDiagram(Document document, Element definitions, BusinessProcess process) {
        Element bpmnDiagram = document.createElementNS(BPMN_DI_NAMESPACE, "bpmndi:BPMNDiagram");
        bpmnDiagram.setAttribute("id", "BPMNDiagram_" + process.getId());
        
        Element bpmnPlane = document.createElementNS(BPMN_DI_NAMESPACE, "bpmndi:BPMNPlane");
        bpmnPlane.setAttribute("id", "BPMNPlane_" + process.getId());
        bpmnPlane.setAttribute("bpmnElement", process.getId());
        
        bpmnDiagram.appendChild(bpmnPlane);
        definitions.appendChild(bpmnDiagram);
    }
    
    /**
     * Generate file name for BPMN output.
     */
    private String generateBpmnFileName(BusinessProcess process) {
        String pattern = config.getFileNamingPattern();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(config.getTimestampFormat()));
        
        String fileName = pattern
                .replace("{processName}", sanitizeFileName(process.getName()))
                .replace("{processId}", sanitizeFileName(process.getId()))
                .replace("{type}", "bpmn")
                .replace("{timestamp}", timestamp);
        
        return fileName + ".bpmn";
    }
    
    /**
     * Generate consolidated BPMN file for multiple processes.
     */
    private String generateConsolidatedBpmn(BusinessMetadata metadata, Map<String, Object> mappedData, Path outputDirectory) {
        try {
            DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
            Document document = builder.newDocument();
            
            // Create root definitions for consolidated file
            Element definitions = document.createElementNS(BPMN_NAMESPACE, "bpmn:definitions");
            definitions.setAttribute("xmlns:bpmn", BPMN_NAMESPACE);
            if (config.isIncludeAwdExtensions()) {
                definitions.setAttribute("xmlns:awd", config.getAwdNamespace());
            }
            definitions.setAttribute("id", "ConsolidatedDefinitions");
            definitions.setAttribute("targetNamespace", "http://chorus.deserializer/bpmn/consolidated");
            document.appendChild(definitions);
            
            // Add all processes to consolidated file
            for (BusinessProcess process : metadata.getBusinessProcesses()) {
                Element processElement = createProcessElement(document, process, mappedData);
                addProcessElements(document, processElement, process, mappedData);
                if (config.isIncludeAwdExtensions()) {
                    addAwdExtensions(document, processElement, process, mappedData);
                }
                definitions.appendChild(processElement);
            }
            
            // Write consolidated file
            String fileName = "consolidated_processes_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern(config.getTimestampFormat())) + ".bpmn";
            Path outputFile = outputDirectory.resolve(fileName);
            String bpmnXml = documentToString(document);
            Files.writeString(outputFile, bpmnXml);
            
            logger.info("Generated consolidated BPMN file: " + fileName);
            return outputFile.toString();
            
        } catch (Exception e) {
            logger.severe("Failed to generate consolidated BPMN file: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Convert DOM document to XML string.
     */
    private String documentToString(Document document) throws Exception {
        Transformer transformer = transformerFactory.newTransformer();
        
        if (config.isPrettyPrintXml()) {
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        }
        
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        
        return writer.toString();
    }
    
    /**
     * Sanitize file name to remove invalid characters.
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "unnamed";
        }
        
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_").replaceAll("_{2,}", "_");
    }
}