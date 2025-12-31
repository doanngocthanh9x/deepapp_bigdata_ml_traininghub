package com.deepapp.vn.io.AA.A0.AAA0_0104.dto;

import com.deepapp.vn.io.AA.A0.AAA0_0104.entity.PageEntity;

/**
 * DTO cho thông tin trang trả về API
 */
public class PageDTO {
    private int id;
    private int taskId;
    private int pageNumber;
    private String filePath;
    private int originalWidth;
    private int originalHeight;

    public PageDTO() {}

    public static PageDTO fromEntity(PageEntity entity) {
        PageDTO dto = new PageDTO();
        dto.setId(entity.getId());
        dto.setTaskId(entity.getTaskId());
        dto.setPageNumber(entity.getPageNumber());
        dto.setFilePath(entity.getFilePath());
        dto.setOriginalWidth(entity.getOriginalWidth());
        dto.setOriginalHeight(entity.getOriginalHeight());
        return dto;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }

    public int getPageNumber() { return pageNumber; }
    public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public int getOriginalWidth() { return originalWidth; }
    public void setOriginalWidth(int originalWidth) { this.originalWidth = originalWidth; }

    public int getOriginalHeight() { return originalHeight; }
    public void setOriginalHeight(int originalHeight) { this.originalHeight = originalHeight; }
}