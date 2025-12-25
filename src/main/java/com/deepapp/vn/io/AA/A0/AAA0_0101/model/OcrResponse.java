package com.deepapp.vn.io.AA.A0.AAA0_0101.model;

/**
 * OCR Response Model
 */
public class OcrResponse {
    
    private boolean success;
    private String text;
    private String error;
    private Long timeMs;
    private String engine;
    private String language;

    public OcrResponse() {
    }

    public OcrResponse(boolean success, String text) {
        this.success = success;
        this.text = text;
    }

    public static OcrResponse success(String text, Long timeMs) {
        OcrResponse response = new OcrResponse();
        response.setSuccess(true);
        response.setText(text);
        response.setTimeMs(timeMs);
        return response;
    }

    public static OcrResponse error(String errorMessage) {
        OcrResponse response = new OcrResponse();
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

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
