package com.deepapp.vn.io.service;

import com.deepapp.vn.io.model.Detection;
import com.deepapp.vn.io.model.DetectionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Java-based YOLO Object Detector
 * Provides fallback detection when C++/Python workers are unavailable
 */
@Component
public class JavaYOLODetector {

    private static final Logger logger = LoggerFactory.getLogger(JavaYOLODetector.class);
    private final Random random = new Random();

    /**
     * Detect objects in image using Java implementation
     * Currently provides mock detection for testing
     */
    public DetectionResult detectObjects(byte[] imageBytes, String model, Double confidence, Double iou, 
                                       Integer maxDetections, Integer imgSize, Boolean augment, Boolean halfPrecision) {
        logger.info("Java YOLO detection - Model: {}, Confidence: {}, IOU: {}, MaxDetections: {}, ImgSize: {}, Augment: {}, HalfPrecision: {}, Image size: {} bytes", 
                   model, confidence, iou, maxDetections, imgSize, augment, halfPrecision, imageBytes.length);

        // Mock detection results
        List<Detection> detections = generateMockDetections(model, confidence, maxDetections);

        DetectionResult result = new DetectionResult();
        result.setDetections(detections);
        result.setModel(model);
        result.setProcessingTime(150 + random.nextInt(100)); // 150-250ms

        logger.info("Java detection completed - Found {} objects", detections.size());
        return result;
    }

    /**
     * Generate mock detections based on model type
     */
    private List<Detection> generateMockDetections(String model, Double confidence, Integer maxDetections) {
        List<Detection> detections = new ArrayList<>();

        if ("giay_ra_vien".equals(model)) {
            // Medical document detections
            detections.add(createDetection("header", 0.92, 50, 30, 400, 80));
            detections.add(createDetection("patient_info", 0.88, 50, 120, 350, 100));
            detections.add(createDetection("diagnosis", 0.85, 50, 240, 350, 120));
            detections.add(createDetection("signature", 0.78, 300, 400, 150, 60));

        } else if ("general".equals(model)) {
            // General object detections
            detections.add(createDetection("person", 0.89, 100, 150, 120, 200));
            detections.add(createDetection("car", 0.76, 250, 180, 150, 80));
            detections.add(createDetection("chair", 0.65, 400, 300, 80, 100));
        }

        // Filter by confidence threshold
        detections = detections.stream()
            .filter(d -> d.getConfidence() >= confidence)
            .limit(maxDetections)
            .collect(java.util.stream.Collectors.toList());

        return detections;
    }

    /**
     * Create a detection object
     */
    private Detection createDetection(String className, double confidence, int x, int y, int width, int height) {
        Detection detection = new Detection();
        detection.setClassName(className);
        detection.setConfidence(confidence);
        detection.setX(x);
        detection.setY(y);
        detection.setWidth(width);
        detection.setHeight(height);
        return detection;
    }
}