package com.deepapp.vn.io.ZZ.A0.ZZA0_0102.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * YOLO Detection Request Model
 */
public class YoloDetectionRequest {

    /**
     * Base64 encoded image data (required if imagePath not provided)
     */
    private String image;

    /**
     * Path to image file (alternative to base64)
     */
    private String imagePath;

    /**
     * Model name to use for detection (default: "giay_ra_vien")
     */
    private String model = "giay_ra_vien";

    /**
     * Confidence threshold (0.0 - 1.0, default: 0.9)
     */
    private Double confidence = 0.9;

    /**
     * IoU threshold for NMS (0.0 - 1.0, default: 0.45)
     */
    private Double iouThreshold = 0.45;

    // Getters and Setters
    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
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

    /**
     * Check if request has valid image data
     */
    public boolean hasImageData() {
        return (image != null && !image.isEmpty()) ||
               (imagePath != null && !imagePath.isEmpty());
    }
}