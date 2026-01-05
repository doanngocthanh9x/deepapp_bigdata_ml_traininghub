package com.deepapp.vn.io.AA.A0.AAA0_0300.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class InferenceRequest {
    
    private String prompt;
    
    @JsonProperty("workerType")
    private String workerType = "python"; // python or cpp
    
    private Double temperature = 0.1;
    
    @JsonProperty("maxTokens")
    private Integer maxTokens = 200;
    
    @JsonProperty("modelName")
    private String modelName = "vinallama-7b-chat";
    
    @JsonProperty("chatHistory")
    private List<Map<String, String>> chatHistory;

    // Getters and Setters
    
    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getWorkerType() {
        return workerType;
    }

    public void setWorkerType(String workerType) {
        this.workerType = workerType;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public List<Map<String, String>> getChatHistory() {
        return chatHistory;
    }

    public void setChatHistory(List<Map<String, String>> chatHistory) {
        this.chatHistory = chatHistory;
    }
}
