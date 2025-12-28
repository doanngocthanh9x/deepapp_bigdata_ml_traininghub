package com.deepapp.vn.io.AA.A0.AAA0_0102.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request model for OCR Pipeline operations
 */
public class OcrPipelineRequest {

    @JsonProperty("image")
    private String image; // Base64 encoded image

    @JsonProperty("imagePath")
    private String imagePath; // File path to image

    @JsonProperty("engine")
    private String engine = "pipeline"; // OCR engine type

    @JsonProperty("language")
    private String language = "vie"; // Language for OCR

    // Constructors
    public OcrPipelineRequest() {}

    public OcrPipelineRequest(String image, String imagePath) {
        this.image = image;
        this.imagePath = imagePath;
    }

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

    // Utility methods
    public boolean hasImageData() {
        return (image != null && !image.trim().isEmpty()) ||
               (imagePath != null && !imagePath.trim().isEmpty());
    }

    @Override
    public String toString() {
        return "OcrPipelineRequest{" +
                "image='" + (image != null ? image.substring(0, Math.min(50, image.length())) + "..." : null) + '\'' +
                ", imagePath='" + imagePath + '\'' +
                ", engine='" + engine + '\'' +
                ", language='" + language + '\'' +
                '}';
    }
}