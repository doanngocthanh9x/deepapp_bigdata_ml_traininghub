package com.deepapp.vn.io.model;

/**
 * Detection result from YOLO model
 */
public class Detection {

    private String className;
    private double confidence;
    private int x;
    private int y;
    private int width;
    private int height;

    public Detection() {}

    public Detection(String className, double confidence, int x, int y, int width, int height) {
        this.className = className;
        this.confidence = confidence;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // Getters and setters
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }

    public int getY() { return y; }
    public void setY(int y) { this.y = y; }

    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    @Override
    public String toString() {
        return String.format("Detection{class='%s', conf=%.2f, bbox=[%d,%d,%d,%d]}",
                           className, confidence, x, y, width, height);
    }
}