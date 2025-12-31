package com.deepapp.vn.io.AA.A0.AAA0_0202.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request model for RAG OCR processing
 */
public class RAGOcrRequest {

    @JsonProperty("image_base64")
    private String imageBase64;

    @JsonProperty("image_path")
    private String imagePath;

    @JsonProperty("template_id")
    private String templateId;

    @JsonProperty("query")
    private String query;

    @JsonProperty("language")
    private String language = "vi";

    @JsonProperty("save_index")
    private Boolean saveIndex = false;

    @JsonProperty("index_path")
    private String indexPath;

    // Constructors
    public RAGOcrRequest() {}

    public RAGOcrRequest(String imageBase64, String templateId) {
        this.imageBase64 = imageBase64;
        this.templateId = templateId;
    }

    // Getters and Setters
    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Boolean getSaveIndex() {
        return saveIndex;
    }

    public void setSaveIndex(Boolean saveIndex) {
        this.saveIndex = saveIndex;
    }

    public String getIndexPath() {
        return indexPath;
    }

    public void setIndexPath(String indexPath) {
        this.indexPath = indexPath;
    }

    @Override
    public String toString() {
        return "RAGOcrRequest{" +
                "imageBase64='" + (imageBase64 != null ? imageBase64.substring(0, Math.min(50, imageBase64.length())) + "..." : null) + '\'' +
                ", imagePath='" + imagePath + '\'' +
                ", templateId='" + templateId + '\'' +
                ", query='" + query + '\'' +
                ", language='" + language + '\'' +
                ", saveIndex=" + saveIndex +
                ", indexPath='" + indexPath + '\'' +
                '}';
    }
}