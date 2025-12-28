package com.deepapp.vn.io.controller;

import com.deepapp.vn.io.workers.PythonWorkerClient;
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

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

/**
 * PaddleOCR Text Recognition Controller
 * Uses PaddleOCR ONNX models for text recognition
 */
@RestController
@RequestMapping("/api/paddle-ocr")
@Tag(name = "PaddleOCR Text Recognition", description = "Text recognition using PaddleOCR ONNX models")
public class PaddleOCRController {

    private static final Logger logger = LoggerFactory.getLogger(PaddleOCRController.class);

    @Autowired
    private PythonWorkerClient pythonWorkerClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Process text recognition using PaddleOCR
     */
    @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Process Text Recognition",
        description = "Extract text from image using PaddleOCR ONNX models"
    )
    public ResponseEntity<?> processTextRecognition(
            @RequestParam("image") MultipartFile image) {

        try {
            logger.info("PaddleOCR text recognition request - Image: {}", image.getOriginalFilename());

            // Validate input
            if (image.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Image file is required"));
            }

            // Convert image to base64
            byte[] imageBytes = image.getBytes();
            String base64Image = "data:image/" + getFileExtension(image.getOriginalFilename()) + ";base64," +
                                Base64.getEncoder().encodeToString(imageBytes);

            // Prepare payload for Python worker
            Map<String, Object> payloadData = new HashMap<>();
            payloadData.put("image", base64Image);

            String payload = objectMapper.writeValueAsString(payloadData);

            logger.info("Calling Python worker AAA0_0201_W for text recognition");

            // Call Python worker
            String result = pythonWorkerClient.callWorker("AAA0_0201_W", "recognize", payload).get();
            
            logger.info("Received response from Python worker: {}", result);

            // Parse result - result is a JSON string from Python worker
            try {
                // First try to parse as JSON
                Map<String, Object> response = objectMapper.readValue(result, Map.class);

                // Check if it's an error response
                if ("error".equals(response.get("status"))) {
                    return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "Text recognition failed",
                        "error", response.get("data")
                    ));
                }

                if (response.containsKey("data")) {
                    Object dataObj = response.get("data");
                    if (dataObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = (Map<String, Object>) dataObj;
                        if (data != null && data.containsKey("text")) {
                            // Draw bboxes on image
                            @SuppressWarnings("unchecked")
                            java.util.List<Map<String, Object>> textRegions = (java.util.List<Map<String, Object>>) data.get("text_regions");
                            String imageWithBboxes = drawBboxesOnImage(base64Image, textRegions);

                            return ResponseEntity.ok(Map.of(
                                "success", true,
                                "message", "Text recognition completed",
                                "worker", "Python Worker (AAA0_0201_W)",
                                "extracted_text", data.get("text"),
                                "confidence", data.get("confidence"),
                                "original_size", data.get("original_size"),
                                "text_regions", data.get("text_regions"),
                                "image_with_bboxes", imageWithBboxes
                            ));
                        }
                    } else {
                        // If data is not a Map, perhaps it's a string or other
                        return ResponseEntity.ok(Map.of(
                            "success", false,
                            "message", "Unexpected data format",
                            "error", dataObj.toString()
                        ));
                    }
                } else if (response.containsKey("text")) {
                    // Draw bboxes on image
                    @SuppressWarnings("unchecked")
                    java.util.List<Map<String, Object>> textRegions = (java.util.List<Map<String, Object>>) response.get("text_regions");
                    String imageWithBboxes = drawBboxesOnImage(base64Image, textRegions);

                    return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Text recognition completed",
                        "worker", "Python Worker (AAA0_0201_W)",
                        "extracted_text", response.get("text"),
                        "confidence", response.get("confidence"),
                        "original_size", response.get("original_size"),
                        "text_regions", response.get("text_regions"),
                        "image_with_bboxes", imageWithBboxes
                    ));
                }
            } catch (Exception parseException) {
                logger.error("Failed to parse worker response: {}", result, parseException);
                // If parsing fails, treat the result as a plain error message
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Failed to parse worker response",
                    "error", result,
                    "raw_response", result
                ));
            }

            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Text recognition failed",
                "error", "No text extracted"
            ));

        } catch (Exception e) {
            logger.error("Error processing text recognition", e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if PaddleOCR service is available")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "service", "PaddleOCR Text Recognition",
            "status", "active",
            "worker", "AAA0_0201_W",
            "model", "PaddleOCR ONNX",
            "timestamp", System.currentTimeMillis()
        ));
    }

    private String getFileExtension(String filename) {
        if (filename == null) return "png";
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) return "png";
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }

    private String drawBboxesOnImage(String base64Image, java.util.List<Map<String, Object>> textRegions) throws Exception {
        // Decode base64 to BufferedImage
        String[] parts = base64Image.split(",");
        String imageData = parts[1];
        byte[] imageBytes = Base64.getDecoder().decode(imageData);
        ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
        BufferedImage image = ImageIO.read(bais);

        // Create graphics
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(2));

        // Draw each bbox
        for (Map<String, Object> region : textRegions) {
            @SuppressWarnings("unchecked")
            java.util.List<java.util.List<Double>> bbox = (java.util.List<java.util.List<Double>>) region.get("bbox");
            if (bbox != null && bbox.size() == 4) {
                int[] xPoints = new int[4];
                int[] yPoints = new int[4];
                for (int i = 0; i < 4; i++) {
                    xPoints[i] = bbox.get(i).get(0).intValue();
                    yPoints[i] = bbox.get(i).get(1).intValue();
                }
                g2d.drawPolygon(xPoints, yPoints, 4);
            }
        }

        g2d.dispose();

        // Encode back to base64
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        byte[] outputBytes = baos.toByteArray();
        String outputBase64 = Base64.getEncoder().encodeToString(outputBytes);
        return "data:image/png;base64," + outputBase64;
    }}