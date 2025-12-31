package com.deepapp.vn.io.AA.A0.AAA0_0104.entity;

/**
 * Entity cho bảng pages - lưu trữ thông tin từng trang sau khi convert
 */
public class PageEntity {
    private int id;
    private int taskId; // Foreign key to tasks_files.id
    private int pageNumber;
    private String filePath; // đường dẫn ảnh trang convert
    private int originalWidth;
    private int originalHeight;

    public PageEntity() {}

    public PageEntity(int taskId, int pageNumber, String filePath, int originalWidth, int originalHeight) {
        this.taskId = taskId;
        this.pageNumber = pageNumber;
        this.filePath = filePath;
        this.originalWidth = originalWidth;
        this.originalHeight = originalHeight;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getOriginalWidth() {
        return originalWidth;
    }

    public void setOriginalWidth(int originalWidth) {
        this.originalWidth = originalWidth;
    }

    public int getOriginalHeight() {
        return originalHeight;
    }

    public void setOriginalHeight(int originalHeight) {
        this.originalHeight = originalHeight;
    }

    @Override
    public String toString() {
        return "PageEntity{" +
                "id=" + id +
                ", taskId=" + taskId +
                ", pageNumber=" + pageNumber +
                ", filePath='" + filePath + '\'' +
                ", originalWidth=" + originalWidth +
                ", originalHeight=" + originalHeight +
                '}';
    }
}