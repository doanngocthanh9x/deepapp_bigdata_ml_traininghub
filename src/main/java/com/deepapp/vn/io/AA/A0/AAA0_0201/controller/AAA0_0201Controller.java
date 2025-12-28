package com.deepapp.vn.io.AA.A0.AAA0_0201.controller;

import com.deepapp.vn.io.AA.A0.AAA0_0201.model.PaddleOcrRequest;
import com.deepapp.vn.io.AA.A0.AAA0_0201.model.PaddleOcrResponse;
import com.deepapp.vn.io.AA.A0.AAA0_0201.service.PaddleOcrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AAA0_0201 - PaddleOCR Text Recognition Controller
 *
 * Provides text recognition services using PaddleOCR ONNX models
 * through Python workers
 */
@RestController
@RequestMapping("/AA/A0/AAA0_0201")
@Tag(name = "AAA0_0201 - PaddleOCR Text Recognition", description = "PaddleOCR text recognition with ONNX models")
public class AAA0_0201Controller {

    private static final Logger logger = LoggerFactory.getLogger(AAA0_0201Controller.class);

    @Autowired
    private PaddleOcrService paddleOcrService;

    /**
     * Health check endpoint
     */
    @GetMapping
    @Operation(summary = "Health check", description = "Check if PaddleOCR service is available")
    public ResponseEntity<Object> healthCheck() {
        return ResponseEntity.ok()
            .body(java.util.Map.of(
                "service", "AAA0_0201 - PaddleOCR Text Recognition",
                "status", "active",
                "models", java.util.List.of("recognition"),
                "languages", java.util.List.of("vi", "en"),
                "timestamp", System.currentTimeMillis()
            ));
    }

    /**
     * Process PaddleOCR text recognition request
     *
     * Accepts either base64 encoded image or image path
     */
    @PostMapping
    @Operation(
        summary = "Recognize Text",
        description = "Extract text from image using PaddleOCR recognition model"
    )
    public ResponseEntity<PaddleOcrResponse> recognizeText(@RequestBody PaddleOcrRequest request) {
        logger.info("PaddleOCR text recognition request received: model={}, language={}",
                   request.getModel(), request.getLanguage());

        try {
            PaddleOcrResponse response = paddleOcrService.processPaddleOcr(request);

            if (response.isSuccess()) {
                logger.info("PaddleOCR successful: text length={}, confidence={}",
                          response.getText() != null ? response.getText().length() : 0,
                          response.getConfidence());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("PaddleOCR failed: {}", response.getError());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

        } catch (Exception e) {
            logger.error("PaddleOCR processing error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(PaddleOcrResponse.error("Internal error: " + e.getMessage()));
        }
    }

    /**
     * Process PaddleOCR with recognition model (convenience endpoint)
     */
    @PostMapping("/recognize")
    @Operation(summary = "Recognize Text (Recognition Model)", description = "Extract text using PaddleOCR recognition model")
    public ResponseEntity<PaddleOcrResponse> recognizeWithRecognitionModel(@RequestBody PaddleOcrRequest request) {
        request.setModel("recognition");
        return recognizeText(request);
    }

    /**
     * List available models
     */
    @GetMapping("/models")
    @Operation(summary = "List Models", description = "Get list of available PaddleOCR models")
    public ResponseEntity<Object> listModels() {
        return ResponseEntity.ok()
            .body(java.util.Map.of(
                "models", java.util.List.of(
                    java.util.Map.of(
                        "name", "recognition",
                        "description", "PaddleOCR text recognition ONNX model",
                        "type", "onnx",
                        "supported_languages", java.util.List.of("vi", "en")
                    )
                )
            ));
    }
}