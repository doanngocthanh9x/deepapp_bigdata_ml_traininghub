package com.deepapp.vn.io.AA.A0.AAA0_0102.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Response model for OCR Pipeline operations
 */
public class OcrPipelineResponse {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private OcrPipelineData data;

    @JsonProperty("error")
    private String error;

    // Constructors
    public OcrPipelineResponse() {}

    public OcrPipelineResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public OcrPipelineResponse(boolean success, String message, OcrPipelineData data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static OcrPipelineResponse success(OcrPipelineData data) {
        return new OcrPipelineResponse(true, "OCR pipeline completed successfully", data);
    }

    public static OcrPipelineResponse error(String errorMessage) {
        OcrPipelineResponse response = new OcrPipelineResponse(false, errorMessage);
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public OcrPipelineData getData() {
        return data;
    }

    public void setData(OcrPipelineData data) {
        this.data = data;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    /**
     * Inner class for OCR pipeline data
     */
    public static class OcrPipelineData {

        @JsonProperty("results")
        private List<OcrResult> results;

        @JsonProperty("total_regions")
        private int totalRegions;

        @JsonProperty("processing_time_ms")
        private long processingTimeMs;

        public OcrPipelineData() {}

        public OcrPipelineData(List<OcrResult> results, int totalRegions, long processingTimeMs) {
            this.results = results;
            this.totalRegions = totalRegions;
            this.processingTimeMs = processingTimeMs;
        }

        // Getters and Setters
        public List<OcrResult> getResults() {
            return results;
        }

        public void setResults(List<OcrResult> results) {
            this.results = results;
        }

        public int getTotalRegions() {
            return totalRegions;
        }

        public void setTotalRegions(int totalRegions) {
            this.totalRegions = totalRegions;
        }

        public long getProcessingTimeMs() {
            return processingTimeMs;
        }

        public void setProcessingTimeMs(long processingTimeMs) {
            this.processingTimeMs = processingTimeMs;
        }
    }

    /**
     * Inner class for individual OCR result
     */
    public static class OcrResult {

        @JsonProperty("text")
        private String text;

        @JsonProperty("angle")
        private int angle;

        @JsonProperty("bbox")
        private List<List<Integer>> bbox;

        @JsonProperty("confidence")
        private double confidence;

        public OcrResult() {}

        public OcrResult(String text, int angle, List<List<Integer>> bbox, double confidence) {
            this.text = text;
            this.angle = angle;
            this.bbox = bbox;
            this.confidence = confidence;
        }

        // Getters and Setters
        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public int getAngle() {
            return angle;
        }

        public void setAngle(int angle) {
            this.angle = angle;
        }

        public List<List<Integer>> getBbox() {
            return bbox;
        }

        public void setBbox(List<List<Integer>> bbox) {
            this.bbox = bbox;
        }

        public double getConfidence() {
            return confidence;
        }

        public void setConfidence(double confidence) {
            this.confidence = confidence;
        }
    }

    @Override
    public String toString() {
        return "OcrPipelineResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", data=" + data +
                ", error='" + error + '\'' +
                '}';
    }
}