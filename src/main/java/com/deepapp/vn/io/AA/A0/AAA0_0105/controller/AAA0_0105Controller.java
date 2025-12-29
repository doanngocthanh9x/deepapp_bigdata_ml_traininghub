package com.deepapp.vn.io.AA.A0.AAA0_0105.controller;

import com.deepapp.vn.io.service.YOLOService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * AAA0_0105 - YOLO Object Detection Controller
 * Supports detection via Java, C++, and Python workers
 */
@RestController
@RequestMapping("/AA/A0/AAA0_0105")
public class AAA0_0105Controller {

    private static final Logger logger = LoggerFactory.getLogger(AAA0_0105Controller.class);

    @Autowired
    private YOLOService yoloService;

    /**
     * Detect objects in image using specified worker
     */
    @PostMapping(value = "/detect", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> detectObjects(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "model", defaultValue = "giay_ra_vien") String model,
            @RequestParam(value = "worker", defaultValue = "java") String worker,
            @RequestParam(value = "confidence", defaultValue = "0.5") Double confidence,
            @RequestParam(value = "iou", defaultValue = "0.45") Double iou,
            @RequestParam(value = "max_detections", defaultValue = "100") Integer maxDetections,
            @RequestParam(value = "img_size", defaultValue = "640") Integer imgSize,
            @RequestParam(value = "augment", defaultValue = "false") Boolean augment,
            @RequestParam(value = "half_precision", defaultValue = "false") Boolean halfPrecision) {

        try {
            logger.info("YOLO detection request - Model: {}, Worker: {}, Image: {}, Confidence: {}, IOU: {}, MaxDetections: {}, ImgSize: {}, Augment: {}, HalfPrecision: {}",
                       model, worker, image.getOriginalFilename(), confidence, iou, maxDetections, imgSize, augment, halfPrecision);

            // Validate input
            if (image.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Image file is required"));
            }

            if (!isValidWorker(worker)) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid worker type. Must be: java, cpp, or python"));
            }

            if (!isValidModel(model)) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid model. Must be: giay_ra_vien or general"));
            }

            // Process detection
            Map<String, Object> result = yoloService.detectObjects(image, model, worker, confidence, iou, maxDetections, imgSize, augment, halfPrecision);

            logger.info("YOLO detection completed - Worker: {}, Detections: {}",
                       worker, result.get("detections"));

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("YOLO detection error", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Detection failed: " + e.getMessage()));
        }
    }

    /**
     * Get available models
     */
    @GetMapping("/models")
    public ResponseEntity<?> getAvailableModels() {
        try {
            Map<String, Object> models = yoloService.getAvailableModels();
            return ResponseEntity.ok(models);
        } catch (Exception e) {
            logger.error("Error getting models", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get models: " + e.getMessage()));
        }
    }

    /**
     * Get worker status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getWorkerStatus() {
        try {
            Map<String, Object> status = yoloService.getWorkerStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("Error getting worker status", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get status: " + e.getMessage()));
        }
    }

    /**
     * Test endpoint
     */
    @GetMapping("/test")
    public ResponseEntity<?> test() {
        return ResponseEntity.ok(Map.of(
            "status", "YOLO Controller is running",
            "workers", new String[]{"java", "cpp", "python"},
            "models", new String[]{"giay_ra_vien", "general"}
        ));
    }

    private boolean isValidWorker(String worker) {
        return worker != null && (worker.equals("java") || worker.equals("cpp") || worker.equals("python"));
    }

    private boolean isValidModel(String model) {
        return model != null && (model.equals("giay_ra_vien") || model.equals("general"));
    }
}