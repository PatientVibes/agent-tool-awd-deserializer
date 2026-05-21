/**
 * Module: AvroWorkObjectDeserializer - Apache Avro deserializer for AWD WorkObject schema
 * 
 * Summary:
 *     Provides specialized deserialization for AWD WorkObject records using Apache Avro.
 *     Handles the com.ssctech.awdlyric.schemas.WorkObject schema with proper validation,
 *     BigDecimal handling, and nullable field processing.
 * 
 * Key Components:
 *     - deserializeWorkObject(): Main Avro deserialization method
 *     - validateWorkObjectSchema(): Schema validation against expected structure
 *     - convertBigDecimalFields(): Handle BigDecimal logical types
 *     - processCustomFields(): Handle extensible custom field arrays
 * 
 * Keywords: avro, workobject, schema, deserialization, awd, lyric, kafka, events, bigdecimal,
 *           logical, types, nullable, custom, fields, validation, enterprise, workflow, queue
 * Dependencies: Apache Avro, Jackson, schema registry, WorkObject schema
 * Security: Schema validation, type checking, safe field access, input validation
 * Performance: Efficient Avro processing, streaming deserialization, memory optimized
 */
package com.patientvibes.awd.deserializer.avro;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Specialized deserializer for AWD WorkObject Avro records.
 * Handles the Kafka event schema used by AWD enterprise workflow systems.
 */
public class AvroWorkObjectDeserializer {
    private static final Logger logger = Logger.getLogger(AvroWorkObjectDeserializer.class.getName());
    
    private final Schema workObjectSchema;
    private final DatumReader<GenericRecord> datumReader;
    
    // AWD WorkObject schema as JSON string
    private static final String WORK_OBJECT_SCHEMA_JSON = """
        {
          "type": "record",
          "name": "WorkObject",
          "namespace": "com.ssctech.awdlyric.schemas",
          "doc": "When a work object is created or modified, the object is published into a Kafka topic",
          "fields": [
            {"name": "id", "type": "string"},
            {"name": "eventType", "type": {"name": "eventTypeEnum", "type": "enum", "symbols": ["CREATE", "MODIFY"]}},
            {"name": "wrkt", "type": ["null", "string"], "doc": "ex: SAMPLEWT"},
            {"name": "unit", "type": ["null", "string"], "doc": "SAMPLEBA"},
            {"name": "vifl", "type": ["null", "string"]},
            {"name": "inx1", "type": ["null", "string"]},
            {"name": "inx2", "type": ["null", "string"]},
            {"name": "inx3", "type": ["null", "string"]},
            {"name": "inx4", "type": ["null", "string"]},
            {"name": "lastNonbatchUser", "type": ["null", "string"]},
            {"name": "statusChangeDateTime", "type": ["null", "string"], "doc": "eg: 27-Apr-21 04.41.17.743760000 PM"},
            {"name": "creationDateTime", "type": ["null", "string"], "doc": "eg: 27-Apr-21 04.41.17.743760000 PM"},
            {"name": "creationNode", "type": ["null", "string"]},
            {"name": "susp", "type": ["null", "string"]},
            {"name": "assignto", "type": ["null", "string"]},
            {"name": "lock", "type": ["null", "string"]},
            {"name": "incr", "type": ["null", {"type": "string", "java-class": "java.math.BigDecimal", "logicalType": "float"}]},
            {"name": "reco", "type": ["null", "string"]},
            {"name": "amtv", "type": ["null", {"type": "string", "java-class": "java.math.BigDecimal", "logicalType": "float"}]},
            {"name": "amtt", "type": ["null", "string"]},
            {"name": "prty", "type": ["null", {"type": "string", "java-class": "java.math.BigDecimal", "logicalType": "float"}]},
            {"name": "stat", "type": ["null", "string"]},
            {"name": "end_queue_flg", "type": ["null", "string"]},
            {"name": "queu", "type": ["null", "string"]},
            {
              "name": "fieldValues",
              "type": [{
                "name": "fieldValue",
                "type": "array",
                "items": {
                  "type": "record",
                  "name": "customField",
                  "fields": [
                    {"name": "name", "type": ["null", "string"]},
                    {"name": "value", "type": ["null", "string"]},
                    {"name": "countryCode", "type": {"type": "string", "java-class": "java.math.BigDecimal", "logicalType": "float"}},
                    {"name": "desc", "type": ["null", "string"]}
                  ]
                }
              }, "null"]
            }
          ]
        }
        """;
    
    public AvroWorkObjectDeserializer() throws IOException {
        this.workObjectSchema = new Schema.Parser().parse(WORK_OBJECT_SCHEMA_JSON);
        this.datumReader = new GenericDatumReader<>(workObjectSchema);
        logger.info("Initialized AWD WorkObject Avro deserializer");
    }
    
    /**
     * Deserialize Avro WorkObject from byte array.
     * 
     * @param avroData Avro-encoded WorkObject data
     * @return Deserialized WorkObject
     * @throws IOException If deserialization fails
     */
    public WorkObject deserializeWorkObject(byte[] avroData) throws IOException {
        logger.info("Deserializing AWD WorkObject from " + avroData.length + " bytes of Avro data");
        
        try {
            // Create Avro decoder
            Decoder decoder = DecoderFactory.get().binaryDecoder(avroData, null);
            
            // Read generic record
            GenericRecord record = datumReader.read(null, decoder);
            
            // Validate schema
            validateWorkObjectSchema(record);
            
            // Convert to typed object
            return convertToWorkObject(record);
            
        } catch (Exception e) {
            logger.severe("WorkObject Avro deserialization failed: " + e.getMessage());
            throw new IOException("Avro deserialization failed", e);
        }
    }
    
    /**
     * Validate WorkObject record against expected schema.
     */
    private void validateWorkObjectSchema(GenericRecord record) throws IOException {
        if (!record.getSchema().getName().equals("WorkObject")) {
            throw new IOException("Expected WorkObject record, got: " + record.getSchema().getName());
        }
        
        if (!record.getSchema().getNamespace().equals("com.ssctech.awdlyric.schemas")) {
            throw new IOException("Expected AWD Lyric namespace, got: " + record.getSchema().getNamespace());
        }
        
        // Validate required fields
        if (record.get("id") == null) {
            throw new IOException("WorkObject missing required 'id' field");
        }
        
        if (record.get("eventType") == null) {
            throw new IOException("WorkObject missing required 'eventType' field");
        }
        
        logger.fine("WorkObject schema validation passed");
    }
    
    /**
     * Convert GenericRecord to typed WorkObject.
     */
    private WorkObject convertToWorkObject(GenericRecord record) throws IOException {
        WorkObject workObject = new WorkObject();
        
        // Required fields
        workObject.setId((String) record.get("id"));
        workObject.setEventType(record.get("eventType").toString());
        
        // Optional string fields
        workObject.setWrkt(getStringField(record, "wrkt"));
        workObject.setUnit(getStringField(record, "unit"));
        workObject.setVifl(getStringField(record, "vifl"));
        workObject.setInx1(getStringField(record, "inx1"));
        workObject.setInx2(getStringField(record, "inx2"));
        workObject.setInx3(getStringField(record, "inx3"));
        workObject.setInx4(getStringField(record, "inx4"));
        workObject.setLastNonbatchUser(getStringField(record, "lastNonbatchUser"));
        workObject.setStatusChangeDateTime(getStringField(record, "statusChangeDateTime"));
        workObject.setCreationDateTime(getStringField(record, "creationDateTime"));
        workObject.setCreationNode(getStringField(record, "creationNode"));
        workObject.setSusp(getStringField(record, "susp"));
        workObject.setAssignto(getStringField(record, "assignto"));
        workObject.setLock(getStringField(record, "lock"));
        workObject.setReco(getStringField(record, "reco"));
        workObject.setAmtt(getStringField(record, "amtt"));
        workObject.setStat(getStringField(record, "stat"));
        workObject.setEndQueueFlg(getStringField(record, "end_queue_flg"));
        workObject.setQueu(getStringField(record, "queu"));
        
        // BigDecimal fields
        workObject.setIncr(getBigDecimalField(record, "incr"));
        workObject.setAmtv(getBigDecimalField(record, "amtv"));
        workObject.setPrty(getBigDecimalField(record, "prty"));
        
        // Custom fields array
        workObject.setFieldValues(processCustomFields(record));
        
        logger.info("Successfully converted WorkObject: " + workObject.getId() + " (" + workObject.getEventType() + ")");
        return workObject;
    }
    
    /**
     * Safely get string field from record.
     */
    private String getStringField(GenericRecord record, String fieldName) {
        Object value = record.get(fieldName);
        return value != null ? value.toString() : null;
    }
    
    /**
     * Get BigDecimal field with logical type handling.
     */
    private BigDecimal getBigDecimalField(GenericRecord record, String fieldName) {
        Object value = record.get(fieldName);
        if (value == null) {
            return null;
        }
        
        try {
            // Handle string representation of BigDecimal
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            logger.warning("Failed to parse BigDecimal field " + fieldName + ": " + value);
            return null;
        }
    }
    
    /**
     * Process custom fields array.
     */
    @SuppressWarnings("unchecked")
    private List<CustomField> processCustomFields(GenericRecord record) {
        Object fieldValuesObj = record.get("fieldValues");
        if (fieldValuesObj == null) {
            return new ArrayList<>();
        }
        
        List<CustomField> customFields = new ArrayList<>();
        
        try {
            List<GenericRecord> fieldRecords = (List<GenericRecord>) fieldValuesObj;
            
            for (GenericRecord fieldRecord : fieldRecords) {
                CustomField customField = new CustomField();
                customField.setName(getStringField(fieldRecord, "name"));
                customField.setValue(getStringField(fieldRecord, "value"));
                customField.setCountryCode(getBigDecimalField(fieldRecord, "countryCode"));
                customField.setDesc(getStringField(fieldRecord, "desc"));
                
                customFields.add(customField);
            }
            
            logger.fine("Processed " + customFields.size() + " custom fields");
            
        } catch (Exception e) {
            logger.warning("Failed to process custom fields: " + e.getMessage());
        }
        
        return customFields;
    }
    
    /**
     * AWD WorkObject data class.
     */
    @JsonSerialize
    public static class WorkObject {
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("eventType")
        private String eventType;
        
        @JsonProperty("wrkt")
        private String wrkt;
        
        @JsonProperty("unit")
        private String unit;
        
        @JsonProperty("vifl")
        private String vifl;
        
        @JsonProperty("inx1")
        private String inx1;
        
        @JsonProperty("inx2")
        private String inx2;
        
        @JsonProperty("inx3")
        private String inx3;
        
        @JsonProperty("inx4")
        private String inx4;
        
        @JsonProperty("lastNonbatchUser")
        private String lastNonbatchUser;
        
        @JsonProperty("statusChangeDateTime")
        private String statusChangeDateTime;
        
        @JsonProperty("creationDateTime")
        private String creationDateTime;
        
        @JsonProperty("creationNode")
        private String creationNode;
        
        @JsonProperty("susp")
        private String susp;
        
        @JsonProperty("assignto")
        private String assignto;
        
        @JsonProperty("lock")
        private String lock;
        
        @JsonProperty("incr")
        private BigDecimal incr;
        
        @JsonProperty("reco")
        private String reco;
        
        @JsonProperty("amtv")
        private BigDecimal amtv;
        
        @JsonProperty("amtt")
        private String amtt;
        
        @JsonProperty("prty")
        private BigDecimal prty;
        
        @JsonProperty("stat")
        private String stat;
        
        @JsonProperty("end_queue_flg")
        private String endQueueFlg;
        
        @JsonProperty("queu")
        private String queu;
        
        @JsonProperty("fieldValues")
        private List<CustomField> fieldValues;
        
        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        
        public String getWrkt() { return wrkt; }
        public void setWrkt(String wrkt) { this.wrkt = wrkt; }
        
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        
        public String getVifl() { return vifl; }
        public void setVifl(String vifl) { this.vifl = vifl; }
        
        public String getInx1() { return inx1; }
        public void setInx1(String inx1) { this.inx1 = inx1; }
        
        public String getInx2() { return inx2; }
        public void setInx2(String inx2) { this.inx2 = inx2; }
        
        public String getInx3() { return inx3; }
        public void setInx3(String inx3) { this.inx3 = inx3; }
        
        public String getInx4() { return inx4; }
        public void setInx4(String inx4) { this.inx4 = inx4; }
        
        public String getLastNonbatchUser() { return lastNonbatchUser; }
        public void setLastNonbatchUser(String lastNonbatchUser) { this.lastNonbatchUser = lastNonbatchUser; }
        
        public String getStatusChangeDateTime() { return statusChangeDateTime; }
        public void setStatusChangeDateTime(String statusChangeDateTime) { this.statusChangeDateTime = statusChangeDateTime; }
        
        public String getCreationDateTime() { return creationDateTime; }
        public void setCreationDateTime(String creationDateTime) { this.creationDateTime = creationDateTime; }
        
        public String getCreationNode() { return creationNode; }
        public void setCreationNode(String creationNode) { this.creationNode = creationNode; }
        
        public String getSusp() { return susp; }
        public void setSusp(String susp) { this.susp = susp; }
        
        public String getAssignto() { return assignto; }
        public void setAssignto(String assignto) { this.assignto = assignto; }
        
        public String getLock() { return lock; }
        public void setLock(String lock) { this.lock = lock; }
        
        public BigDecimal getIncr() { return incr; }
        public void setIncr(BigDecimal incr) { this.incr = incr; }
        
        public String getReco() { return reco; }
        public void setReco(String reco) { this.reco = reco; }
        
        public BigDecimal getAmtv() { return amtv; }
        public void setAmtv(BigDecimal amtv) { this.amtv = amtv; }
        
        public String getAmtt() { return amtt; }
        public void setAmtt(String amtt) { this.amtt = amtt; }
        
        public BigDecimal getPrty() { return prty; }
        public void setPrty(BigDecimal prty) { this.prty = prty; }
        
        public String getStat() { return stat; }
        public void setStat(String stat) { this.stat = stat; }
        
        public String getEndQueueFlg() { return endQueueFlg; }
        public void setEndQueueFlg(String endQueueFlg) { this.endQueueFlg = endQueueFlg; }
        
        public String getQueu() { return queu; }
        public void setQueu(String queu) { this.queu = queu; }
        
        public List<CustomField> getFieldValues() { return fieldValues; }
        public void setFieldValues(List<CustomField> fieldValues) { this.fieldValues = fieldValues; }
    }
    
    /**
     * Custom field data class.
     */
    @JsonSerialize
    public static class CustomField {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("value")
        private String value;
        
        @JsonProperty("countryCode")
        private BigDecimal countryCode;
        
        @JsonProperty("desc")
        private String desc;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        
        public BigDecimal getCountryCode() { return countryCode; }
        public void setCountryCode(BigDecimal countryCode) { this.countryCode = countryCode; }
        
        public String getDesc() { return desc; }
        public void setDesc(String desc) { this.desc = desc; }
    }
}