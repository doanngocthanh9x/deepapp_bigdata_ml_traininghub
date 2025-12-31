package com.deepapp.vn.io.service;

import com.deepapp.vn.io.workers.CppWorkerClient;
import com.deepapp.vn.io.model.DetectionResult;
import com.deepapp.vn.io.utils.ImageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

/**
 * YOLO Object Detection Service
 * Routes detection requests to appropriate workers (Java/C++/Python)
 */
@Service
public class YOLOService {

    private static final Logger logger = LoggerFactory.getLogger(YOLOService.class);

    @Autowired
    private CppWorkerClient cppWorkerClient;

    // Java-based YOLO detection (fallback)
    @Autowired
    private JavaYOLODetector javaYoloDetector;

    /**
     * Detect objects using specified worker
     */
    public Map<String, Object> detectObjects(MultipartFile image, String model, String worker, 
                                           Double confidence, Double iou, Integer maxDetections, 
                                           Integer imgSize, Boolean augment, Boolean halfPrecision) throws Exception {
        logger.info("Starting YOLO detection - Worker: {}, Model: {}, Confidence: {}, IOU: {}, MaxDetections: {}, ImgSize: {}, Augment: {}, HalfPrecision: {}", 
                   worker, model, confidence, iou, maxDetections, imgSize, augment, halfPrecision);

        // Convert image to bytes
        byte[] imageBytes = image.getBytes();

        // Note: Image optimization removed for C++ worker since we now pass file path instead of byte array

        DetectionResult result;
        long startTime = System.currentTimeMillis();

        try {
            switch (worker.toLowerCase()) {
                case "cpp":
                    result = cppWorkerClient.detectObjects(imageBytes, model, confidence, iou, maxDetections, imgSize, augment, halfPrecision);
                    break;
                case "java":
                default:
                    result = javaYoloDetector.detectObjects(imageBytes, model, confidence, iou, maxDetections, imgSize, augment, halfPrecision);
                    break;
            }

            long processingTime = System.currentTimeMillis() - startTime;
            logger.info("Detection completed in {}ms - Worker: {}, Detections: {}",
                       processingTime, worker, result.getDetections().size());

            // Convert to response format
            return createResponse(result, worker, processingTime, imgSize);

        } catch (Exception e) {
            logger.error("Detection failed for worker: " + worker, e);
            throw new RuntimeException("Detection failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get available models
     */
    public Map<String, Object> getAvailableModels() {
        return Map.of(
            "models", Arrays.asList(
                Map.of("id", "giay_ra_vien", "name", "Giấy Ra Viện", "description", "Medical discharge papers"),
                Map.of("id", "general", "name", "General Objects", "description", "General object detection")
            )
        );
    }

    /**
     * Get worker status
     */
    public Map<String, Object> getWorkerStatus() {
        Map<String, Object> status = new HashMap<>();

        // Check Java worker
        status.put("java", Map.of(
            "status", "available",
            "type", "Java (Spring Boot)",
            "models", Arrays.asList("giay_ra_vien", "general")
        ));

        // Check C++ worker
        try {
            boolean cppAvailable = cppWorkerClient.isAvailable();
            status.put("cpp", Map.of(
                "status", cppAvailable ? "available" : "unavailable",
                "type", "C++ (gRPC)",
                "models", cppAvailable ? Arrays.asList("giay_ra_vien", "general") : Collections.emptyList()
            ));
        } catch (Exception e) {
            status.put("cpp", Map.of(
                "status", "error",
                "type", "C++ (gRPC)",
                "error", e.getMessage()
            ));
        }

        return status;
    }

    /**
     * Create standardized response
     */
    private Map<String, Object> createResponse(DetectionResult result, String worker, long processingTime, Integer imgSize) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("worker", worker);
        response.put("processingTime", processingTime + "ms");
        response.put("timestamp", System.currentTimeMillis());
        response.put("detectionImageSize", imgSize); // Size of image used for detection (e.g., 640)

        // Convert detections
        List<Map<String, Object>> detections = new ArrayList<>();
        for (var detection : result.getDetections()) {
            Map<String, Object> det = new HashMap<>();
            det.put("label", detection.getClassName());
            det.put("confidence", detection.getConfidence());
            // Use x1,y1,x2,y2 format for consistency with C++ worker
            det.put("bbox", Map.of(
                "x1", detection.getX(),
                "y1", detection.getY(),
                "x2", detection.getX() + detection.getWidth(),
                "y2", detection.getY() + detection.getHeight()
            ));
            detections.add(det);
        }

        response.put("detections", detections);
        response.put("count", detections.size());

        return response;
    }
}