package com.deepapp.vn.io.AA.A0.AAA0_0102.controller;

import com.deepapp.vn.io.AA.A0.AAA0_0102.model.OcrPipelineRequest;
import com.deepapp.vn.io.AA.A0.AAA0_0102.model.OcrPipelineResponse;
import com.deepapp.vn.io.AA.A0.AAA0_0102.service.OcrPipelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * OCR Pipeline Controller - REST API endpoints for OCR pipeline operations
 * Combines PaddleOCR detection/classification with VietOCR recognition
 */
@RestController
@RequestMapping("/api/v1/ocr-pipeline")
@Tag(name = "OCR Pipeline", description = "OCR Pipeline API with PaddleOCR + VietOCR")
public class OcrPipelineController {

    private static final Logger logger = LoggerFactory.getLogger(OcrPipelineController.class);

    @Autowired
    private OcrPipelineService ocrPipelineService;

    /**
     * Process OCR pipeline request
     */
    @PostMapping(value = "/process", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Process OCR Pipeline", description = "Run full OCR pipeline with PaddleOCR detection/classification and VietOCR recognition")
    public ResponseEntity<OcrPipelineResponse> processOcr(@RequestBody OcrPipelineRequest request) {
        try {
            logger.info("Received OCR pipeline request");

            OcrPipelineResponse response = ocrPipelineService.processOcrPipeline(request);

            if (response.isSuccess()) {
                logger.info("OCR pipeline completed successfully");
                return ResponseEntity.ok(response);
            } else {
                logger.warn("OCR pipeline failed: {}", response.getError());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            logger.error("OCR pipeline processing error", e);
            OcrPipelineResponse errorResponse = OcrPipelineResponse.error("Internal server error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @Operation(summary = "Health Check", description = "Check if OCR pipeline service and models are healthy")
    public ResponseEntity<OcrPipelineResponse> healthCheck() {
        try {
            OcrPipelineResponse response = ocrPipelineService.healthCheck();

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(503).body(response); // Service Unavailable
            }

        } catch (Exception e) {
            logger.error("Health check error", e);
            OcrPipelineResponse errorResponse = OcrPipelineResponse.error("Health check failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Demo page endpoint
     */
    @GetMapping("/demo")
    @Operation(summary = "OCR Pipeline Demo Page", description = "Access the HTML demo page for OCR pipeline testing")
    public String demoPage() {
        return "redirect:/demo/ocr-pipeline-demo.html";
    }
}