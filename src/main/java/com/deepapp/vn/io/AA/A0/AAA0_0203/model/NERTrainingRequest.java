package com.deepapp.vn.io.AA.A0.AAA0_0203.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Request model for NER Training Data operations
 */
public class NERTrainingRequest {

    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("payload")
    private Map<String, Object> payload;

    // Constructors
    public NERTrainingRequest() {}

    public NERTrainingRequest(String eventType, Map<String, Object> payload) {
        this.eventType = eventType;
        this.payload = payload;
    }

    // Getters and Setters
    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}