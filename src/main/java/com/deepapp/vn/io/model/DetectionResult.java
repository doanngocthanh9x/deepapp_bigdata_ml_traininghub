package com.deepapp.vn.io.model;

import java.util.List;

/**
 * Result of YOLO object detection
 */
public class DetectionResult {

    private List<Detection> detections;
    private String model;
    private long processingTime;
    private String error;

    public DetectionResult() {}

    public DetectionResult(List<Detection> detections, String model, long processingTime) {
        this.detections = detections;
        this.model = model;
        this.processingTime = processingTime;
    }

    // Getters and setters
    public List<Detection> getDetections() { return detections; }
    public void setDetections(List<Detection> detections) { this.detections = detections; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public long getProcessingTime() { return processingTime; }
    public void setProcessingTime(long processingTime) { this.processingTime = processingTime; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public boolean hasError() {
        return error != null && !error.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("DetectionResult{model='%s', detections=%d, time=%dms, error='%s'}",
                           model, detections != null ? detections.size() : 0, processingTime, error);
    }
}