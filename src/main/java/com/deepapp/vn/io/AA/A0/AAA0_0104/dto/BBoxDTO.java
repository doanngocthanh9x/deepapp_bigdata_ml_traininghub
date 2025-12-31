package com.deepapp.vn.io.AA.A0.AAA0_0104.dto;

import com.deepapp.vn.io.AA.A0.AAA0_0104.entity.BBoxEntity;

/**
 * DTO cho thông tin bounding box trả về API
 */
public class BBoxDTO {
    private int id;
    private int pageId;
    private String coordinates;
    private String clazz;
    private double confidence;
    private String filePath;

    public BBoxDTO() {}

    public static BBoxDTO fromEntity(BBoxEntity entity) {
        BBoxDTO dto = new BBoxDTO();
        dto.setId(entity.getId());
        dto.setPageId(entity.getPageId());
        dto.setCoordinates(entity.getCoordinates());
        dto.setClassType(entity.getClassType());
        dto.setConfidence(entity.getConfidence());
        dto.setFilePath(entity.getFilePath());
        return dto;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPageId() { return pageId; }
    public void setPageId(int pageId) { this.pageId = pageId; }

    public String getCoordinates() { return coordinates; }
    public void setCoordinates(String coordinates) { this.coordinates = coordinates; }

    public String getClassType() { return clazz; }
    public void setClassType(String clazz) { this.clazz = clazz; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
}