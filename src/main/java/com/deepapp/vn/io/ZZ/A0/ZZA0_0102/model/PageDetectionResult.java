package com.deepapp.vn.io.ZZ.A0.ZZA0_0102.model;

import com.deepapp.vn.io.ZZ.A0.ZZA0_0102.model.YoloDetectionResponse.DetectionResult;

import java.util.List;

/**
 * Page Detection Result Model - for individual page results
 */
public class PageDetectionResult {

    /**
     * Page number (1-based)
     */
    private Integer pageNumber;

    /**
     * Path to the page image
     */
    private String imagePath;

    /**
     * Status of processing this page
     */
    private String status;

    /**
     * Error message if status is "error"
     */
    private String error;

    /**
     * Processing time for this page in milliseconds
     */
    private Long processingTime;

    /**
     * List of detected objects on this page
     */
    private List<DetectionResult> detections;

    // Getters and Setters
    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Long getProcessingTime() {
        return processingTime;
    }

    public void setProcessingTime(Long processingTime) {
        this.processingTime = processingTime;
    }

    public List<DetectionResult> getDetections() {
        return detections;
    }

    public void setDetections(List<DetectionResult> detections) {
        this.detections = detections;
    }
}