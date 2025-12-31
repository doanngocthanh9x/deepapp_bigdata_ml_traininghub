package com.deepapp.vn.io.AA.A0.AAA0_0104.entity;

/**
 * Entity cho bảng bboxes - lưu trữ thông tin bounding boxes từ OCR/YOLO
 */
public class BBoxEntity {
    private int id;
    private int pageId; // Foreign key to pages.id
    private String coordinates; // JSON string of bbox coordinates
    private String clazz; // loại đối tượng detect (vd: text, table, object)
    private double confidence; // độ tin cậy
    private String filePath; // crop file lưu riêng (nếu có)

    public BBoxEntity() {}

    public BBoxEntity(int pageId, String coordinates, String clazz, double confidence) {
        this.pageId = pageId;
        this.coordinates = coordinates;
        this.clazz = clazz;
        this.confidence = confidence;
    }

    public BBoxEntity(int pageId, String coordinates, String clazz, double confidence, String filePath) {
        this(pageId, coordinates, clazz, confidence);
        this.filePath = filePath;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPageId() {
        return pageId;
    }

    public void setPageId(int pageId) {
        this.pageId = pageId;
    }

    public String getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(String coordinates) {
        this.coordinates = coordinates;
    }

    public String getClassType() {
        return clazz;
    }

    public void setClassType(String clazz) {
        this.clazz = clazz;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public String toString() {
        return "BBoxEntity{" +
                "id=" + id +
                ", pageId=" + pageId +
                ", coordinates='" + coordinates + '\'' +
                ", clazz='" + clazz + '\'' +
                ", confidence=" + confidence +
                ", filePath='" + filePath + '\'' +
                '}';
    }
}