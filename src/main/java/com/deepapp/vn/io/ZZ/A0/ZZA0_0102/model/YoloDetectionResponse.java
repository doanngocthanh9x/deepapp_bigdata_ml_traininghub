package com.deepapp.vn.io.ZZ.A0.ZZA0_0102.model;

import java.util.List;

/**
 * YOLO Detection Response Model
 */
public class YoloDetectionResponse {

    /**
     * Status of the detection operation
     */
    private String status;

    /**
     * Error message if status is "error"
     */
    private String error;

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
     * Processing time in milliseconds
     */
    private Long processingTime;

    /**
     * List of detected objects
     */
    private List<DetectionResult> detections;

    /**
     * Image dimensions
     */
    private ImageDimensions dimensions;

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

    public ImageDimensions getDimensions() {
        return dimensions;
    }

    public void setDimensions(ImageDimensions dimensions) {
        this.dimensions = dimensions;
    }

    /**
     * Detection result for a single object
     */
    public static class DetectionResult {
        private String label;
        private Double confidence;
        private BoundingBox bbox;

        public DetectionResult() {}

        public DetectionResult(String label, Double confidence, BoundingBox bbox) {
            this.label = label;
            this.confidence = confidence;
            this.bbox = bbox;
        }

        // Getters and Setters
        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public Double getConfidence() {
            return confidence;
        }

        public void setConfidence(Double confidence) {
            this.confidence = confidence;
        }

        public BoundingBox getBbox() {
            return bbox;
        }

        public void setBbox(BoundingBox bbox) {
            this.bbox = bbox;
        }
    }

    /**
     * Bounding box coordinates
     */
    public static class BoundingBox {
        private Double x1;
        private Double y1;
        private Double x2;
        private Double y2;

        public BoundingBox() {}

        public BoundingBox(Double x1, Double y1, Double x2, Double y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        // Getters and Setters
        public Double getX1() {
            return x1;
        }

        public void setX1(Double x1) {
            this.x1 = x1;
        }

        public Double getY1() {
            return y1;
        }

        public void setY1(Double y1) {
            this.y1 = y1;
        }

        public Double getX2() {
            return x2;
        }

        public void setX2(Double x2) {
            this.x2 = x2;
        }

        public Double getY2() {
            return y2;
        }

        public void setY2(Double y2) {
            this.y2 = y2;
        }
    }

    /**
     * Image dimensions
     */
    public static class ImageDimensions {
        private Integer width;
        private Integer height;

        public ImageDimensions() {}

        public ImageDimensions(Integer width, Integer height) {
            this.width = width;
            this.height = height;
        }

        // Getters and Setters
        public Integer getWidth() {
            return width;
        }

        public void setWidth(Integer width) {
            this.width = width;
        }

        public Integer getHeight() {
            return height;
        }

        public void setHeight(Integer height) {
            this.height = height;
        }
    }
}