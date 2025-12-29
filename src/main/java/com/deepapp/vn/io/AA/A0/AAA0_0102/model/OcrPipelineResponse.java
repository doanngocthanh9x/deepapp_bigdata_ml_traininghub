package com.deepapp.vn.io.AA.A0.AAA0_0102.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response model for OCR Pipeline operations - Updated to match frontend interface
 */
public class OcrPipelineResponse {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("text")
    private String text;

    @JsonProperty("confidence")
    private Double confidence;

    @JsonProperty("boundingBoxes")
    private List<BoundingBox> boundingBoxes;

    @JsonProperty("error")
    private String error;

    @JsonProperty("processingTime")
    private Long processingTime;

    // Constructors
    public OcrPipelineResponse() {}

    public OcrPipelineResponse(boolean success) {
        this.success = success;
    }

    public static OcrPipelineResponse success(String text, Double confidence, List<BoundingBox> boundingBoxes, Long processingTime) {
        OcrPipelineResponse response = new OcrPipelineResponse(true);
        response.setText(text);
        response.setConfidence(confidence);
        response.setBoundingBoxes(boundingBoxes);
        response.setProcessingTime(processingTime);
        return response;
    }

    public static OcrPipelineResponse error(String errorMessage) {
        OcrPipelineResponse response = new OcrPipelineResponse(false);
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

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public List<BoundingBox> getBoundingBoxes() {
        return boundingBoxes;
    }

    public void setBoundingBoxes(List<BoundingBox> boundingBoxes) {
        this.boundingBoxes = boundingBoxes;
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

    /**
     * Bounding box class matching frontend interface
     */
    public static class BoundingBox {

        @JsonProperty("x")
        private int x;

        @JsonProperty("y")
        private int y;

        @JsonProperty("width")
        private int width;

        @JsonProperty("height")
        private int height;

        @JsonProperty("text")
        private String text;

        public BoundingBox() {}

        public BoundingBox(int x, int y, int width, int height, String text) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.text = text;
        }

        // Convert from old bbox format [[x1,y1], [x2,y2], [x3,y3], [x4,y4]] to new format
        public static BoundingBox fromPolygon(List<List<Integer>> polygon, String text) {
            if (polygon == null || polygon.size() < 4) {
                return new BoundingBox(0, 0, 0, 0, text);
            }

            // Find min/max coordinates
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;

            for (List<Integer> point : polygon) {
                if (point.size() >= 2) {
                    minX = Math.min(minX, point.get(0));
                    minY = Math.min(minY, point.get(1));
                    maxX = Math.max(maxX, point.get(0));
                    maxY = Math.max(maxY, point.get(1));
                }
            }

            return new BoundingBox(minX, minY, maxX - minX, maxY - minY, text);
        }

        // Getters and Setters
        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    @Override
    public String toString() {
        return "OcrPipelineResponse{" +
                "success=" + success +
                ", text='" + text + '\'' +
                ", confidence=" + confidence +
                ", boundingBoxes=" + boundingBoxes +
                ", error='" + error + '\'' +
                ", processingTime=" + processingTime +
                '}';
    }
}