package com.deepapp.vn.io.AA.A0.AAA0_0201.model;

/**
 * PaddleOCR Text Recognition Response Model
 */
public class PaddleOcrResponse {

    private boolean success;
    private String text;
    private String error;
    private Long timeMs;
    private Double confidence;

    public PaddleOcrResponse() {
    }

    public PaddleOcrResponse(boolean success, String text) {
        this.success = success;
        this.text = text;
    }

    public static PaddleOcrResponse success(String text, Long timeMs) {
        PaddleOcrResponse response = new PaddleOcrResponse();
        response.setSuccess(true);
        response.setText(text);
        response.setTimeMs(timeMs);
        return response;
    }

    public static PaddleOcrResponse success(String text, Long timeMs, Double confidence) {
        PaddleOcrResponse response = new PaddleOcrResponse();
        response.setSuccess(true);
        response.setText(text);
        response.setTimeMs(timeMs);
        response.setConfidence(confidence);
        return response;
    }

    public static PaddleOcrResponse error(String errorMessage) {
        PaddleOcrResponse response = new PaddleOcrResponse();
        response.setSuccess(false);
        response.setError(errorMessage);
        return response;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Long getTimeMs() {
        return timeMs;
    }

    public void setTimeMs(Long timeMs) {
        this.timeMs = timeMs;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
}