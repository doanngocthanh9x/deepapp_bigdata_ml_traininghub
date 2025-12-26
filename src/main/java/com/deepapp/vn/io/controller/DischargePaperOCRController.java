package com.deepapp.vn.io.controller;

import com.deepapp.vn.io.service.YOLOService;
import com.deepapp.vn.io.AA.A0.AAA0_0101.model.OcrRequest;
import com.deepapp.vn.io.AA.A0.AAA0_0101.model.OcrResponse;
import com.deepapp.vn.io.AA.A0.AAA0_0101.service.OcrService;
import com.deepapp.vn.io.workers.PythonWorkerClient;
import com.deepapp.vn.io.workers.CppWorkerClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

import java.util.Base64;

/**
 * Discharge Paper OCR Controller
 * Combines YOLO detection for text regions with VietOCR for text extraction
 * Specifically designed for Vietnamese discharge papers (giấy ra viện)
 */
@RestController
@RequestMapping("/api/discharge-paper-ocr")
@Tag(name = "Discharge Paper OCR", description = "OCR for Vietnamese discharge papers using YOLO + VietOCR")
public class DischargePaperOCRController {

    private static final Logger logger = LoggerFactory.getLogger(DischargePaperOCRController.class);

    @Autowired
    private YOLOService yoloService;

    @Autowired
    private OcrService ocrService;

    @Autowired
    private PythonWorkerClient pythonWorkerClient;

    @Autowired
    private CppWorkerClient cppWorkerClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Process discharge paper OCR
     * 1. Use YOLO to detect text regions
     * 2. Use VietOCR to extract text from each region
     */
    @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Process Discharge Paper OCR",
        description = "Extract text from Vietnamese discharge paper using YOLO detection + VietOCR"
    )
    public ResponseEntity<?> processDischargePaper(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "confidence", defaultValue = "0.5") Double confidence,
            @RequestParam(value = "iou", defaultValue = "0.45") Double iou,
            @RequestParam(value = "max_detections", defaultValue = "50") Integer maxDetections,
            @RequestParam(value = "worker", defaultValue = "python") String workerType) {

        try {
            logger.info("Discharge paper OCR request - Image: {}, Confidence: {}, IOU: {}, MaxDetections: {}",
                       image.getOriginalFilename(), confidence, iou, maxDetections);

            // Validate input
            if (image.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Image file is required"));
            }

            // Step 1: YOLO Detection for text regions
            Map<String, Object> yoloResult = yoloService.detectObjects(
                image, "giay_ra_vien", "cpp", confidence, iou, maxDetections,
                640, false, false
            );

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> detections = (List<Map<String, Object>>) yoloResult.get("detections");

            if (detections == null || detections.isEmpty()) {
                logger.warn("No text regions detected in discharge paper");
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "No text regions detected",
                    "detections", 0,
                    "extracted_text", "",
                    "regions", new ArrayList<>()
                ));
            }

            logger.info("YOLO detected {} text regions", detections.size());

            // Step 2: Extract text from each detected region using VietOCR
            List<Map<String, Object>> ocrResults = new ArrayList<>();
            StringBuilder fullText = new StringBuilder();

            // Convert image to base64 for OCR processing
            byte[] imageBytes = image.getBytes();
            String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);

            // Process each detected region individually
            try {
                // Prepare bboxes data
                List<Map<String, Object>> bboxData = new ArrayList<>();
                for (Map<String, Object> detection : detections) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> bbox = (Map<String, Object>) detection.get("bbox");
                    if (bbox != null) {
                        Map<String, Object> bboxInfo = new HashMap<>();
                        // Ensure coordinates are numbers, not null
                        Double x1 = bbox.get("x1") instanceof Number ? ((Number) bbox.get("x1")).doubleValue() : 0.0;
                        Double y1 = bbox.get("y1") instanceof Number ? ((Number) bbox.get("y1")).doubleValue() : 0.0;
                        Double x2 = bbox.get("x2") instanceof Number ? ((Number) bbox.get("x2")).doubleValue() : 0.0;
                        Double y2 = bbox.get("y2") instanceof Number ? ((Number) bbox.get("y2")).doubleValue() : 0.0;

                        bboxInfo.put("x1", x1);
                        bboxInfo.put("y1", y1);
                        bboxInfo.put("x2", x2);
                        bboxInfo.put("y2", y2);
                        bboxInfo.put("label", detection.get("label"));
                        bboxInfo.put("confidence", detection.get("confidence"));
                        bboxData.add(bboxInfo);

                        logger.debug("Bbox {}: x1={}, y1={}, x2={}, y2={}, label={}",
                                   bboxData.size()-1, x1, y1, x2, y2, detection.get("label"));
                    }
                }

                // Call Python worker with bboxes
                Map<String, Object> payloadData = new HashMap<>();
                payloadData.put("image", imageBase64);
                payloadData.put("bboxes", bboxData);
                payloadData.put("image_width", 640);
                payloadData.put("image_height", 640);
                
                String bboxPayload = new ObjectMapper().writeValueAsString(payloadData);

                logger.info("Calling {} worker for bbox-based OCR with {} regions", workerType.toUpperCase(), bboxData.size());
                logger.debug("Bbox payload (first bbox): {}", bboxData.size() > 0 ? bboxData.get(0) : "No bboxes");
                logger.debug("Bbox JSON: {}", new ObjectMapper().writeValueAsString(bboxData).substring(0, Math.min(200, new ObjectMapper().writeValueAsString(bboxData).length())));
                
                String bboxResult;
                if ("cpp".equalsIgnoreCase(workerType)) {
                    // Call C++ worker for bbox processing
                    bboxResult = cppWorkerClient.callWorker("ZZA0_0101_W", "extract_text_from_bboxes", bboxPayload).get();
                } else {
                    // Default to Python worker
                    bboxResult = pythonWorkerClient.callWorker("AAA0_0101_W", "extract_text_from_bboxes", bboxPayload).get();
                }
                
                // Parse bbox results - handle both formats: direct "results" (C++) and "data.results" (Python)
                JsonNode bboxJson = new ObjectMapper().readTree(bboxResult);
                JsonNode resultsNode = null;

                // Check for C++ worker format first (direct "results")
                if (bboxJson.has("results")) {
                    resultsNode = bboxJson.get("results");
                }
                // Check for Python worker format ("data.results")
                else if (bboxJson.has("data") && bboxJson.get("data").has("results")) {
                    resultsNode = bboxJson.get("data").get("results");
                }

                if (resultsNode != null && resultsNode.isArray()) {
                    for (int i = 0; i < resultsNode.size() && i < detections.size(); i++) {
                        JsonNode resultNode = resultsNode.get(i);
                        Map<String, Object> detection = detections.get(i);

                        @SuppressWarnings("unchecked")
                        Map<String, Object> bbox = (Map<String, Object>) detection.get("bbox");

                        Map<String, Object> regionResult = new HashMap<>();
                        regionResult.put("bbox", bbox);
                        regionResult.put("confidence", detection.get("confidence"));
                        regionResult.put("label", detection.get("label"));
                        regionResult.put("text", resultNode.has("text") ? resultNode.get("text").asText() : "");
                        regionResult.put("ocr_success", resultNode.has("text") && !resultNode.get("text").asText().isEmpty());

                        ocrResults.add(regionResult);

                        // Add to full text
                        String text = resultNode.has("text") ? resultNode.get("text").asText() : "";
                        if (!text.isEmpty()) {
                            if (fullText.length() > 0) fullText.append(" ");
                            fullText.append(text);
                        }
                    }
                }

            } catch (Exception e) {
                logger.error("Error processing OCR for bboxes", e);
                // Fallback: create empty results
                for (Map<String, Object> detection : detections) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> bbox = (Map<String, Object>) detection.get("bbox");
                    if (bbox == null) continue;

                    Map<String, Object> regionResult = new HashMap<>();
                    regionResult.put("bbox", bbox);
                    regionResult.put("confidence", detection.get("confidence"));
                    regionResult.put("label", detection.get("label"));
                    regionResult.put("text", "");
                    regionResult.put("ocr_success", false);

                    ocrResults.add(regionResult);
                }
            }

            // Return successful response
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "OCR processing completed",
                "worker", workerType.toUpperCase() + " Worker",
                "detections", detections.size(),
                "extracted_text", fullText.toString(),
                "regions", ocrResults
            ));

        } catch (Exception e) {
            logger.error("Error processing OCR", e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if discharge paper OCR service is available")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "service", "Discharge Paper OCR",
            "status", "active",
            "pipeline", "YOLO Detection + VietOCR",
            "model", "giay_ra_vien",
            "timestamp", System.currentTimeMillis()
        ));
    }
}