package com.deepapp.vn.io.AA.A0.AAA0_0300.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InferenceResponse {
    
    private String status;
    private String response;
    private String message;
    
    @JsonProperty("tokens")
    private Integer tokens;
    
    @JsonProperty("inferenceTime")
    private Long inferenceTime;
    
    @JsonProperty("workerType")
    private String workerType;
    
    @JsonProperty("modelName")
    private String modelName;

    public InferenceResponse() {
    }

    public InferenceResponse(String status, String response) {
        this.status = status;
        this.response = response;
    }

    public static InferenceResponse success(String response) {
        return new InferenceResponse("success", response);
    }

    public static InferenceResponse error(String message) {
        InferenceResponse resp = new InferenceResponse();
        resp.status = "error";
        resp.message = message;
        return resp;
    }

    // Getters and Setters
    
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getTokens() {
        return tokens;
    }

    public void setTokens(Integer tokens) {
        this.tokens = tokens;
    }

    public Long getInferenceTime() {
        return inferenceTime;
    }

    public void setInferenceTime(Long inferenceTime) {
        this.inferenceTime = inferenceTime;
    }

    public String getWorkerType() {
        return workerType;
    }

    public void setWorkerType(String workerType) {
        this.workerType = workerType;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
}
