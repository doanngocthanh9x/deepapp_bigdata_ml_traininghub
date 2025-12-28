package com.deepapp.vn.io.AA.A0.AAA0_0201.model;

/**
 * PaddleOCR Text Recognition Request Model
 */
public class PaddleOcrRequest {

    /**
     * Base64 encoded image data (required if imagePath not provided)
     */
    private String image;

    /**
     * Path to image file (alternative to base64)
     */
    private String imagePath;

    /**
     * Language code: "vi" for Vietnamese, "en" for English
     * Default: "vi"
     */
    private String language = "vi";

    /**
     * Model name: "recognition" for text recognition
     * Default: "recognition"
     */
    private String model = "recognition";

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

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    /**
     * Validate that at least one image source is provided
     */
    public boolean hasImageData() {
        return (image != null && !image.isEmpty()) ||
               (imagePath != null && !imagePath.isEmpty());
    }
}