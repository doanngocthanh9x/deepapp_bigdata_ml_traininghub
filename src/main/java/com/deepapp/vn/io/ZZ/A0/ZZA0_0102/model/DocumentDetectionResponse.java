package com.deepapp.vn.io.ZZ.A0.ZZA0_0102.model;

import java.util.List;

/**
 * Document Detection Response Model - for processing entire documents
 */
public class DocumentDetectionResponse {

    /**
     * Status of the document detection operation
     */
    private String status;

    /**
     * Error message if status is "error"
     */
    private String error;

    /**
     * Request ID for tracking
     */
    private String requestId;

    /**
     * Original filename
     */
    private String filename;

    /**
     * Total number of pages in document
     */
    private Integer totalPages;

    /**
     * Number of pages successfully processed
     */
    private Integer processedPages;

    /**
     * Model used for detection
     */
    private String model;

    /**
     * Confidence threshold used
     */
    private Double confidence;

    /**
     * IoU threshold used
     */
    private Double iouThreshold;

    /**
     * Total processing time in milliseconds
     */
    private Long totalProcessingTime;

    /**
     * Detection results for each page
     */
    private List<PageDetectionResult> pageResults;

    // Getters and Setters
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

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public Integer getProcessedPages() {
        return processedPages;
    }

    public void setProcessedPages(Integer processedPages) {
        this.processedPages = processedPages;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public Double getIouThreshold() {
        return iouThreshold;
    }

    public void setIouThreshold(Double iouThreshold) {
        this.iouThreshold = iouThreshold;
    }

    public Long getTotalProcessingTime() {
        return totalProcessingTime;
    }

    public void setTotalProcessingTime(Long totalProcessingTime) {
        this.totalProcessingTime = totalProcessingTime;
    }

    public List<PageDetectionResult> getPageResults() {
        return pageResults;
    }

    public void setPageResults(List<PageDetectionResult> pageResults) {
        this.pageResults = pageResults;
    }

    /**
     * Create error response
     */
    public static DocumentDetectionResponse error(String error) {
        DocumentDetectionResponse response = new DocumentDetectionResponse();
        response.setStatus("error");
        response.setError(error);
        return response;
    }
}