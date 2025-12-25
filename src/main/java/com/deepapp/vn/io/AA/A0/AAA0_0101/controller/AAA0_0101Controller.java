package com.deepapp.vn.io.AA.A0.AAA0_0101.controller;

import com.deepapp.vn.io.AA.A0.AAA0_0101.model.OcrRequest;
import com.deepapp.vn.io.AA.A0.AAA0_0101.model.OcrResponse;
import com.deepapp.vn.io.AA.A0.AAA0_0101.service.OcrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AAA0_0101 - VietOCR & PaddleOCR Controller
 * 
 * Provides OCR (Optical Character Recognition) services for Vietnamese text
 * using VietOCR and PaddleOCR engines through C++ workers
 */
@RestController
@RequestMapping("/AA/A0/AAA0_0101")
@Tag(name = "AAA0_0101 - OCR Services", description = "Vietnamese OCR with VietOCR and PaddleOCR")
public class AAA0_0101Controller {
    
    private static final Logger logger = LoggerFactory.getLogger(AAA0_0101Controller.class);
    
    @Autowired
    private OcrService ocrService;
    
    /**
     * Health check endpoint
     */
    @GetMapping
    @Operation(summary = "Health check", description = "Check if OCR service is available")
    public ResponseEntity<Object> healthCheck() {
        return ResponseEntity.ok()
            .body(java.util.Map.of(
                "service", "AAA0_0101 - OCR Services",
                "status", "active",
                "engines", java.util.List.of("vietocr", "paddleocr"),
                "languages", java.util.List.of("vi", "en"),
                "timestamp", System.currentTimeMillis()
            ));
    }
    
    /**
     * Process OCR request
     * 
     * Accepts either base64 encoded image or image path
     * Supports VietOCR and PaddleOCR engines
     */
    @PostMapping
    @Operation(
        summary = "Process OCR", 
        description = "Extract text from image using VietOCR or PaddleOCR engine"
    )
    public ResponseEntity<OcrResponse> processOcr(@RequestBody OcrRequest request) {
        logger.info("OCR request received: engine={}, language={}", 
                   request.getEngine(), request.getLanguage());
        
        try {
            OcrResponse response = ocrService.processOcr(request);
            
            if (response.isSuccess()) {
                logger.info("OCR successful: text length={}", 
                          response.getText() != null ? response.getText().length() : 0);
                return ResponseEntity.ok(response);
            } else {
                logger.warn("OCR failed: {}", response.getError());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
        } catch (Exception e) {
            logger.error("OCR processing error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(OcrResponse.error("Internal error: " + e.getMessage()));
        }
    }
    
    /**
     * Process OCR with VietOCR (convenience endpoint)
     */
    @PostMapping("/vietocr")
    @Operation(summary = "VietOCR", description = "Extract Vietnamese text using VietOCR engine")
    public ResponseEntity<OcrResponse> processVietOcr(@RequestBody OcrRequest request) {
        request.setEngine("vietocr");
        return processOcr(request);
    }
    
    /**
     * Process OCR with PaddleOCR (convenience endpoint)
     */
    @PostMapping("/paddleocr")
    @Operation(summary = "PaddleOCR", description = "Extract text using PaddleOCR engine")
    public ResponseEntity<OcrResponse> processPaddleOcr(@RequestBody OcrRequest request) {
        request.setEngine("paddleocr");
        return processOcr(request);
    }
}
