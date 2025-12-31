package com.deepapp.vn.io.AA.A0.AAA0_0203.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Response model for NER Training Data operations
 */
public class NERTrainingResponse {

    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private Map<String, Object> data;

    // Constructors
    public NERTrainingResponse() {}

    public NERTrainingResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public NERTrainingResponse(String status, String message, Map<String, Object> data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    // Utility methods
    public static NERTrainingResponse success(String message) {
        return new NERTrainingResponse("success", message);
    }

    public static NERTrainingResponse success(String message, Map<String, Object> data) {
        return new NERTrainingResponse("success", message, data);
    }

    public static NERTrainingResponse error(String message) {
        return new NERTrainingResponse("error", message);
    }
}