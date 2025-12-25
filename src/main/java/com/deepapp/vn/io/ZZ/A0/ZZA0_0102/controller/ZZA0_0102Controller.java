package com.deepapp.vn.io.ZZ.A0.ZZA0_0102.controller;

import com.deepapp.vn.io.ZZ.A0.ZZA0_0102.model.YoloDetectionRequest;
import com.deepapp.vn.io.ZZ.A0.ZZA0_0102.model.YoloDetectionResponse;
import com.deepapp.vn.io.ZZ.A0.ZZA0_0102.model.DocumentDetectionResponse;
import com.deepapp.vn.io.ZZ.A0.ZZA0_0102.model.PageDetectionResult;
import com.deepapp.vn.io.ZZ.A0.ZZA0_0102.service.YoloDetectionService;
import com.deepapp.vn.io.storage.service.DocumentManagementService;
import com.deepapp.vn.io.storage.dto.PageDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ZZA0_0102 - YOLO Object Detection Controller
 *
 * Provides YOLO object detection services for document images
 * using ONNX models through C++ workers
 */
@RestController
@RequestMapping("/ZZ/A0/ZZA0_0102")
@Tag(name = "ZZA0_0102 - YOLO Detection", description = "YOLO object detection for document images")
public class ZZA0_0102Controller {

    private static final Logger logger = LoggerFactory.getLogger(ZZA0_0102Controller.class);

    @Autowired
    private YoloDetectionService yoloDetectionService;

    @Autowired
    private DocumentManagementService documentManagementService;

    /**
     * Health check endpoint
     */
    @GetMapping
    @Operation(summary = "Health check", description = "Check if YOLO detection service is available")
    public ResponseEntity<Object> healthCheck() {
        return ResponseEntity.ok()
            .body(java.util.Map.of(
                "service", "ZZA0_0102 - YOLO Detection",
                "status", "active",
                "models", java.util.List.of("giay_ra_vien"),
                "features", java.util.List.of("object_detection", "bounding_boxes"),
                "timestamp", System.currentTimeMillis()
            ));
    }

    /**
     * Process YOLO detection request
     *
     * Accepts either base64 encoded image or image path
     * Uses YOLO ONNX model for object detection
     */
    @PostMapping
    @Operation(
        summary = "Detect Objects",
        description = "Detect objects in image using YOLO ONNX model"
    )
    public ResponseEntity<YoloDetectionResponse> detectObjects(@RequestBody YoloDetectionRequest request) {
        logger.info("YOLO detection request received: model={}, confidence={}",
                   request.getModel(), request.getConfidence());

        try {
            YoloDetectionResponse response = yoloDetectionService.processDetection(request);

            if ("success".equals(response.getStatus())) {
                logger.info("YOLO detection successful: {} objects detected",
                          response.getDetections() != null ? response.getDetections().size() : 0);
                return ResponseEntity.ok(response);
            } else {
                logger.warn("YOLO detection failed: {}", response.getError());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

        } catch (Exception e) {
            logger.error("YOLO detection processing error", e);
            YoloDetectionResponse errorResponse = new YoloDetectionResponse();
            errorResponse.setStatus("error");
            errorResponse.setError("Internal error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Process YOLO detection with default model (convenience endpoint)
     */
    @PostMapping("/detect")
    @Operation(summary = "Detect with Default Model", description = "Detect objects using default YOLO model (giay_ra_vien)")
    public ResponseEntity<YoloDetectionResponse> detectWithDefaultModel(@RequestBody YoloDetectionRequest request) {
        request.setModel("giay_ra_vien");
        return detectObjects(request);
    }

    /**
     * Process YOLO detection with custom confidence threshold
     */
    @PostMapping("/detect/{confidence}")
    @Operation(summary = "Detect with Confidence", description = "Detect objects with custom confidence threshold")
    public ResponseEntity<YoloDetectionResponse> detectWithConfidence(
            @RequestBody YoloDetectionRequest request,
            @PathVariable Double confidence) {
        request.setConfidence(confidence);
        return detectObjects(request);
    }

    /**
     * Process document file upload and YOLO detection on all pages
     */
    @PostMapping("/detect-file")
    @Operation(summary = "Detect Objects in Document", description = "Upload PDF/TIFF file and detect objects on all pages")
    public ResponseEntity<DocumentDetectionResponse> detectObjectsInDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "model", defaultValue = "giay_ra_vien") String model,
            @RequestParam(value = "confidence", defaultValue = "0.5") Double confidence,
            @RequestParam(value = "iouThreshold", defaultValue = "0.45") Double iouThreshold) {

        logger.info("Document YOLO detection request: filename={}, size={}, model={}",
                   file.getOriginalFilename(), file.getSize(), model);

        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    DocumentDetectionResponse.error("File is empty"));
            }

            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.toLowerCase().endsWith(".pdf") &&
                                   !filename.toLowerCase().endsWith(".tiff") &&
                                   !filename.toLowerCase().endsWith(".tif"))) {
                return ResponseEntity.badRequest().body(
                    DocumentDetectionResponse.error("Only PDF and TIFF files are supported"));
            }

            // Generate request ID
            String requestId = "yolo_" + System.currentTimeMillis() + "_" +
                             java.util.UUID.randomUUID().toString().substring(0, 8);

            // Process document upload (this will extract pages)
            // Note: We need to call the document upload service first
            // For now, we'll assume pages are already extracted and stored

            // Get all pages for this document
            List<PageDTO> pages = documentManagementService.getPages(requestId);
            if (pages.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    DocumentDetectionResponse.error("No pages found for document. Please upload document first."));
            }

            // Process each page
            List<PageDetectionResult> pageResults = new ArrayList<>();
            long totalProcessingTime = 0;

            for (PageDTO page : pages) {
                try {
                    // Create detection request for this page
                    YoloDetectionRequest detectionRequest = new YoloDetectionRequest();
                    detectionRequest.setImagePath(page.getImagePath());
                    detectionRequest.setModel(model);
                    detectionRequest.setConfidence(confidence);
                    detectionRequest.setIouThreshold(iouThreshold);

                    // Run detection
                    YoloDetectionResponse detectionResponse = yoloDetectionService.processDetection(detectionRequest);

                    // Create page result
                    PageDetectionResult pageResult = new PageDetectionResult();
                    pageResult.setPageNumber(page.getPageNumber());
                    pageResult.setImagePath(page.getImagePath());
                    pageResult.setStatus(detectionResponse.getStatus());
                    pageResult.setDetections(detectionResponse.getDetections());
                    pageResult.setProcessingTime(detectionResponse.getProcessingTime());

                    if (detectionResponse.getProcessingTime() != null) {
                        totalProcessingTime += detectionResponse.getProcessingTime();
                    }

                    pageResults.add(pageResult);

                } catch (Exception e) {
                    logger.error("Failed to process page {}: {}", page.getPageNumber(), e.getMessage());

                    PageDetectionResult errorResult = new PageDetectionResult();
                    errorResult.setPageNumber(page.getPageNumber());
                    errorResult.setImagePath(page.getImagePath());
                    errorResult.setStatus("error");
                    errorResult.setError("Processing failed: " + e.getMessage());
                    pageResults.add(errorResult);
                }
            }

            // Create document response
            DocumentDetectionResponse response = new DocumentDetectionResponse();
            response.setStatus("success");
            response.setRequestId(requestId);
            response.setFilename(filename);
            response.setTotalPages(pages.size());
            response.setProcessedPages(pageResults.size());
            response.setModel(model);
            response.setConfidence(confidence);
            response.setIouThreshold(iouThreshold);
            response.setTotalProcessingTime(totalProcessingTime);
            response.setPageResults(pageResults);

            logger.info("Document YOLO detection completed: {} pages processed in {}ms",
                       pageResults.size(), totalProcessingTime);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Document YOLO detection failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                DocumentDetectionResponse.error("Internal error: " + e.getMessage()));
        }
    }
}