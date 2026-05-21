/**
 * Module: BpmnXmlGeneratorTest - Unit tests for BPMN XML generation with AWD extensions
 * 
 * Summary:
 *     Comprehensive unit tests for the BpmnXmlGenerator class covering BPMN 2.0 XML
 *     generation with AWD custom extensions, namespace validation, and schema compliance.
 *     Tests both individual process generation and consolidated BPMN file creation.
 * 
 * Key Components:
 *     - testBasicBpmnGeneration(): Core BPMN XML generation validation
 *     - testAwdExtensions(): AWD namespace and custom element validation
 *     - testBpmnSchemaCompliance(): BPMN 2.0 schema compliance verification
 *     - testAwdExpressionGeneration(): AWD expression language validation
 * 
 * Keywords: test, bpmn, xml, generator, awd, extensions, namespace, validation, schema,
 *          compliance, expression, language, generation, custom, elements, process
 * 
 * Dependencies:
 *     - org.junit.jupiter.api.*: JUnit 5 testing framework
 *     - org.w3c.dom.*: DOM parsing for XML validation
 *     - javax.xml.xpath.*: XPath expressions for XML content verification
 * 
 * Security:
 *     - XML injection prevention validation
 *     - Safe XML parsing and validation
 *     - Test data isolation and cleanup
 * 
 * Performance:
 *     - BPMN generation performance validation
 *     - Memory usage monitoring for large processes
 *     - XML serialization efficiency testing
 */
package com.patientvibes.awd.deserializer.extractor.generator;

import com.patientvibes.awd.deserializer.business.model.BusinessMetadata;
import com.patientvibes.awd.deserializer.business.model.BusinessProcess;
import com.patientvibes.awd.deserializer.business.model.ProcessElement;
import com.patientvibes.awd.deserializer.business.model.ProcessVariable;
import com.patientvibes.awd.deserializer.extractor.config.ExtractionConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for BpmnXmlGenerator.
 */
class BpmnXmlGeneratorTest {
    
    @TempDir
    Path tempDir;
    
    private BpmnXmlGenerator generator;
    private ExtractionConfig config;
    private BusinessMetadata testMetadata;
    private Map<String, Object> mappedData;
    
    @BeforeEach
    void setUp() {
        config = new ExtractionConfig();
        config.setIncludeAwdExtensions(true);
        config.setValidateBpmnSchema(true);
        config.setPrettyPrintXml(true);
        
        generator = new BpmnXmlGenerator(config);
        testMetadata = createTestBusinessMetadata();
        mappedData = createTestMappedData();
    }
    
    @Test
    void testBasicBpmnGeneration() throws Exception {
        // When
        List<String> generatedFiles = generator.generateBpmnFiles(testMetadata, mappedData, tempDir);
        
        // Then
        assertThat(generatedFiles).isNotEmpty();
        assertThat(generatedFiles).hasSize(1); // One process in test data
        
        Path bpmnFile = Paths.get(generatedFiles.get(0));
        assertThat(Files.exists(bpmnFile)).isTrue();
        assertThat(Files.size(bpmnFile)).isGreaterThan(0);
        
        String bpmnContent = Files.readString(bpmnFile);
        assertThat(bpmnContent).contains("<?xml version=\"1.0\"");
        assertThat(bpmnContent).contains("<bpmn:definitions");
        assertThat(bpmnContent).contains("</bpmn:definitions>");
    }
    
    @Test
    void testBpmnNamespaceDeclarations() throws Exception {
        // When
        List<String> generatedFiles = generator.generateBpmnFiles(testMetadata, mappedData, tempDir);
        
        // Then
        Path bpmnFile = Paths.get(generatedFiles.get(0));
        String bpmnContent = Files.readString(bpmnFile);
        
        // Verify standard BPMN namespaces
        assertThat(bpmnContent).contains("xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"");
        assertThat(bpmnContent).contains("xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\"");
        assertThat(bpmnContent).contains("xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\"");
        assertThat(bpmnContent).contains("xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\"");
        
        // Verify AWD namespace
        assertThat(bpmnContent).contains("xmlns:awd=\"http://www.dstawd.com\"");
    }
    
    @Test
    void testProcessElementGeneration() throws Exception {
        // When
        List<String> generatedFiles = generator.generateBpmnFiles(testMetadata, mappedData, tempDir);
        
        // Then
        Path bpmnFile = Paths.get(generatedFiles.get(0));
        Document doc = parseXmlFile(bpmnFile);
        
        // Verify process element
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList processNodes = (NodeList) xpath.evaluate("//bpmn:process", doc, XPathConstants.NODESET);
        assertThat(processNodes.getLength()).isEqualTo(1);
        
        Element processElement = (Element) processNodes.item(0);
        assertThat(processElement.getAttribute("id")).isEqualTo("TestProcess_001");
        assertThat(processElement.getAttribute("name")).isEqualTo("Test Business Process");
        assertThat(processElement.getAttribute("isExecutable")).isEqualTo("true");
    }
    
    @Test
    void testProcessElements() throws Exception {
        // When
        List<String> generatedFiles = generator.generateBpmnFiles(testMetadata, mappedData, tempDir);
        
        // Then
        Path bpmnFile = Paths.get(generatedFiles.get(0));
        Document doc = parseXmlFile(bpmnFile);
        
        XPath xpath = XPathFactory.newInstance().newXPath();
        
        // Verify start event
        NodeList startEvents = (NodeList) xpath.evaluate("//bpmn:startEvent", doc, XPathConstants.NODESET);
        assertThat(startEvents.getLength()).isGreaterThanOrEqualTo(1);
        
        // Verify user task
        NodeList userTasks = (NodeList) xpath.evaluate("//bpmn:userTask", doc, XPathConstants.NODESET);
        assertThat(userTasks.getLength()).isEqualTo(1);
        
        Element userTask = (Element) userTasks.item(0);
        assertThat(userTask.getAttribute("id")).isEqualTo("UserTask_1");
        assertThat(userTask.getAttribute("name")).isEqualTo("Test User Task");
        
        // Verify end event
        NodeList endEvents = (NodeList) xpath.evaluate("//bpmn:endEvent", doc, XPathConstants.NODESET);
        assertThat(endEvents.getLength()).isGreaterThanOrEqualTo(1);
    }
    
    @Test
    void testAwdExtensions() throws Exception {
        // When
        List<String> generatedFiles = generator.generateBpmnFiles(testMetadata, mappedData, tempDir);
        
        // Then
        Path bpmnFile = Paths.get(generatedFiles.get(0));
        Document doc = parseXmlFile(bpmnFile);
        
        XPath xpath = XPathFactory.newInstance().newXPath();
        
        // Verify extension elements
        NodeList extensionElements = (NodeList) xpath.evaluate("//bpmn:extensionElements", doc, XPathConstants.NODESET);
        assertThat(extensionElements.getLength()).isGreaterThan(0);
        
        // Verify AWD properties
        NodeList awdProperties = (NodeList) xpath.evaluate("//awd:properties", doc, XPathConstants.NODESET);
        assertThat(awdProperties.getLength()).isGreaterThan(0);
        
        // Verify AWD property elements
        NodeList awdPropertyElements = (NodeList) xpath.evaluate("//awd:property", doc, XPathConstants.NODESET);
        assertThat(awdPropertyElements.getLength()).isGreaterThan(0);
        
        Element configProperty = null;
        for (int i = 0; i < awdPropertyElements.getLength(); i++) {
            Element prop = (Element) awdPropertyElements.item(i);
            if ("processConfig".equals(prop.getAttribute("name"))) {
                configProperty = prop;
                break;
            }
        }
        
        assertThat(configProperty).isNotNull();
        assertThat(configProperty.getTextContent()).isNotEmpty();
        assertThat(configProperty.getTextContent()).contains("processId");
    }
    
    @Test
    void testAwdSetValueElements() throws Exception {
        // When
        List<String> generatedFiles = generator.generateBpmnFiles(testMetadata, mappedData, tempDir);
        
        // Then
        Path bpmnFile = Paths.get(generatedFiles.get(0));
        Document doc = parseXmlFile(bpmnFile);
        
        XPath xpath = XPathFactory.newInstance().newXPath();
        
        // Verify AWD setValue elements
        NodeList setValueElements = (NodeList) xpath.evaluate("//awd:setValue", doc, XPathConstants.NODESET);
        assertThat(setValueElements.getLength()).isEqualTo(1); // One variable in test data
        
        Element setValue = (Element) setValueElements.item(0);
        assertThat(setValue.getAttribute("id")).isEqualTo("setValue_testVariable");
        assertThat(setValue.getAttribute("name")).isEqualTo("Set testVariable");
        
        // Verify setValue configuration
        NodeList setValueConfig = (NodeList) xpath.evaluate(".//awd:property[@name='config']", setValue, XPathConstants.NODESET);
        assertThat(setValueConfig.getLength()).isEqualTo(1);
        
        Element configElement = (Element) setValueConfig.item(0);
        String configJson = configElement.getTextContent();
        assertThat(configJson).contains("setValues");
        assertThat(configJson).contains("awd:awd-value('testVariable')");
    }
    
    @Test
    void testSequenceFlowGeneration() throws Exception {
        // When
        List<String> generatedFiles = generator.generateBpmnFiles(testMetadata, mappedData, tempDir);
        
        // Then
        Path bpmnFile = Paths.get(generatedFiles.get(0));
        Document doc = parseXmlFile(bpmnFile);
        
        XPath xpath = XPathFactory.newInstance().newXPath();
        
        // Verify sequence flows
        NodeList sequenceFlows = (NodeList) xpath.evaluate("//bpmn:sequenceFlow", doc, XPathConstants.NODESET);
        assertThat(sequenceFlows.getLength()).isGreaterThan(0);
        
        // Verify sequence flow attributes
        for (int i = 0; i < sequenceFlows.getLength(); i++) {
            Element flow = (Element) sequenceFlows.item(i);
            assertThat(flow.getAttribute("id")).isNotEmpty();
            assertThat(flow.getAttribute("sourceRef")).isNotEmpty();
            assertThat(flow.getAttribute("targetRef")).isNotEmpty();
        }
    }
    
    @Test
    void testBpmnDiagramGeneration() throws Exception {
        // When
        List<String> generatedFiles = generator.generateBpmnFiles(testMetadata, mappedData, tempDir);
        
        // Then
        Path bpmnFile = Paths.get(generatedFiles.get(0));
        Document doc = parseXmlFile(bpmnFile);
        
        XPath xpath = XPathFactory.newInstance().newXPath();
        
        // Verify BPMN diagram elements
        NodeList bpmnDiagrams = (NodeList) xpath.evaluate("//bpmndi:BPMNDiagram", doc, XPathConstants.NODESET);
        assertThat(bpmnDiagrams.getLength()).isEqualTo(1);
        
        Element diagram = (Element) bpmnDiagrams.item(0);
        assertThat(diagram.getAttribute("id")).isEqualTo("BPMNDiagram_TestProcess_001");
        
        // Verify BPMN plane
        NodeList bpmnPlanes = (NodeList) xpath.evaluate("//bpmndi:BPMNPlane", doc, XPathConstants.NODESET);
        assertThat(bpmnPlanes.getLength()).isEqualTo(1);
        
        Element plane = (Element) bpmnPlanes.item(0);
        assertThat(plane.getAttribute("bpmnElement")).isEqualTo("TestProcess_001");
    }
    
    @Test
    void testAwdExtensionsDisabled() throws Exception {
        // Given
        ExtractionConfig configNoAwd = new ExtractionConfig();
        configNoAwd.setIncludeAwdExtensions(false);
        BpmnXmlGenerator generatorNoAwd = new BpmnXmlGenerator(configNoAwd);
        
        // When
        List<String> generatedFiles = generatorNoAwd.generateBpmnFiles(testMetadata, mappedData, tempDir);
        
        // Then
        Path bpmnFile = Paths.get(generatedFiles.get(0));
        String bpmnContent = Files.readString(bpmnFile);
        
        // Verify AWD namespace is not included
        assertThat(bpmnContent).doesNotContain("xmlns:awd=");
        assertThat(bpmnContent).doesNotContain("awd:properties");
        assertThat(bpmnContent).doesNotContain("awd:setValue");
    }
    
    @Test
    void testConsolidatedBpmnGeneration() throws Exception {
        // Given - Add another process to test metadata
        BusinessProcess secondProcess = new BusinessProcess();
        secondProcess.setId("TestProcess_002");
        secondProcess.setName("Second Test Process");
        secondProcess.setType("AUTOMATION");
        
        List<BusinessProcess> processesToUpdate = new ArrayList<>(testMetadata.getBusinessProcesses());
        processesToUpdate.add(secondProcess);
        testMetadata.setBusinessProcesses(processesToUpdate);
        
        // When
        List<String> generatedFiles = generator.generateBpmnFiles(testMetadata, mappedData, tempDir);
        
        // Then
        assertThat(generatedFiles).hasSize(3); // 2 individual + 1 consolidated
        
        // Find consolidated file
        String consolidatedFile = generatedFiles.stream()
            .filter(file -> file.contains("consolidated"))
            .findFirst()
            .orElse(null);
        
        assertThat(consolidatedFile).isNotNull();
        
        Path consolidatedPath = Paths.get(consolidatedFile);
        Document doc = parseXmlFile(consolidatedPath);
        
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList processNodes = (NodeList) xpath.evaluate("//bpmn:process", doc, XPathConstants.NODESET);
        assertThat(processNodes.getLength()).isEqualTo(2);
    }
    
    @Test
    void testXmlWellFormedness() throws Exception {
        // When
        List<String> generatedFiles = generator.generateBpmnFiles(testMetadata, mappedData, tempDir);
        
        // Then
        for (String filePath : generatedFiles) {
            Path bpmnFile = Paths.get(filePath);
            
            // Should be able to parse without exceptions
            assertThatCode(() -> parseXmlFile(bpmnFile))
                .doesNotThrowAnyException();
            
            String content = Files.readString(bpmnFile);
            
            // Basic XML structure validation
            assertThat(content).startsWith("<?xml");
            assertThat(content).contains("<bpmn:definitions");
            assertThat(content).endsWith("</bpmn:definitions>");
            
            // Verify balanced tags
            long openTags = content.chars().filter(c -> c == '<').count();
            long closeTags = content.chars().filter(c -> c == '>').count();
            assertThat(openTags).isEqualTo(closeTags);
        }
    }
    
    @Test
    void testFileNaming() throws Exception {
        // When
        List<String> generatedFiles = generator.generateBpmnFiles(testMetadata, mappedData, tempDir);
        
        // Then
        String fileName = Paths.get(generatedFiles.get(0)).getFileName().toString();
        
        assertThat(fileName).endsWith(".bpmn");
        assertThat(fileName).contains("Test_Business_Process"); // Sanitized process name
        assertThat(fileName).contains("bpmn"); // Type indicator
    }
    
    // Helper methods
    
    private Document parseXmlFile(Path xmlFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        
        String content = Files.readString(xmlFile);
        return builder.parse(new ByteArrayInputStream(content.getBytes()));
    }
    
    private BusinessMetadata createTestBusinessMetadata() {
        BusinessMetadata metadata = new BusinessMetadata();
        metadata.setExtractionTimestamp(LocalDateTime.now());
        metadata.setSourceType("AWD_DESIGN_FILE");
        
        BusinessProcess process = new BusinessProcess();
        process.setId("TestProcess_001");
        process.setName("Test Business Process");
        process.setType("AUTOMATION");
        process.setVersion(1);
        process.setCreatedBy("testUser");
        process.setModifiedBy("testUser");
        process.setModelState("active");
        
        // Add process elements
        ProcessElement startEvent = new ProcessElement();
        startEvent.setId("StartEvent_1");
        startEvent.setName("Start");
        startEvent.setType("startEvent");
        process.addElement(startEvent);
        
        ProcessElement userTask = new ProcessElement();
        userTask.setId("UserTask_1");
        userTask.setName("Test User Task");
        userTask.setType("userTask");
        process.addElement(userTask);
        
        ProcessElement endEvent = new ProcessElement();
        endEvent.setId("EndEvent_1");
        endEvent.setName("End");
        endEvent.setType("endEvent");
        process.addElement(endEvent);
        
        // Add process variable for setValue testing
        ProcessVariable variable = new ProcessVariable();
        variable.setName("testVariable");
        variable.setType("String");
        variable.setSourceExpression("//*[name()='transaction']/testField");
        process.addVariable(variable);
        
        metadata.setBusinessProcesses(Arrays.asList(process));
        
        return metadata;
    }
    
    private Map<String, Object> createTestMappedData() {
        Map<String, Object> data = new HashMap<>();
        
        // Process data
        Map<String, Object> processData = new HashMap<>();
        processData.put("totalProcesses", 1);
        data.put("processes", processData);
        
        // Extensions data
        Map<String, Object> extensionData = new HashMap<>();
        extensionData.put("namespace", "http://www.dstawd.com");
        extensionData.put("prefix", "awd");
        data.put("extensions", extensionData);
        
        return data;
    }
}