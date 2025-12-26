package com.deepapp.vn.io.AA.A0.AAA0_0101.service;

import com.deepapp.vn.io.AA.A0.AAA0_0101.model.OcrRequest;
import com.deepapp.vn.io.AA.A0.AAA0_0101.model.OcrResponse;
import com.deepapp.vn.io.workers.PythonWorkerClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * OCR Service - Business logic for OCR operations
 */
@Service
public class OcrService {
    
    private static final Logger logger = LoggerFactory.getLogger(OcrService.class);
    private static final int OCR_TIMEOUT_SECONDS = 30;
    private static final String OCR_WORKER_ID = "AAA0_0101_W";  // C++ OCR worker task ID
    
    @Autowired
    private PythonWorkerClient pythonWorkerClient;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Process OCR request
     */
    public OcrResponse processOcr(OcrRequest request) {
        try {
            // Validate request
            if (!request.hasImageData()) {
                return OcrResponse.error("Either 'image' or 'imagePath' must be provided");
            }
            
            // Build JSON payload for C++ worker
            String payload = buildOcrPayload(request);
            logger.info("Sending OCR request to worker: engine={}, language={}", 
                       request.getEngine(), request.getLanguage());
            
            // Call OCR worker via gRPC using PythonWorkerClient
            long startTime = System.currentTimeMillis();
            String result = pythonWorkerClient.callWorker(OCR_WORKER_ID, "vietocr", payload).get();
            long duration = System.currentTimeMillis() - startTime;
            
            logger.info("OCR completed in {}ms", duration);
            
            // Parse response from C++ worker
            return parseOcrResult(result, duration, request);
            
        } catch (Exception e) {
            logger.error("OCR processing failed", e);
            return OcrResponse.error("OCR processing failed: " + e.getMessage());
        }
    }
    
    /**
     * Build JSON payload for C++ OCR worker
     */
    private String buildOcrPayload(OcrRequest request) {
        try {
            StringBuilder json = new StringBuilder("{");
            
            if (request.getImage() != null && !request.getImage().isEmpty()) {
                json.append("\"image\":\"").append(request.getImage()).append("\",");
            }
            
            if (request.getImagePath() != null && !request.getImagePath().isEmpty()) {
                json.append("\"imagePath\":\"").append(request.getImagePath()).append("\",");
            }
            
            json.append("\"language\":\"").append(request.getLanguage()).append("\",");
            json.append("\"engine\":\"").append(request.getEngine()).append("\"");
            json.append("}");
            
            return json.toString();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to build OCR payload", e);
        }
    }
    
    /**
     * Parse OCR result from C++ worker
     */
    private OcrResponse parseOcrResult(String result, long duration, OcrRequest request) {
        try {
            JsonNode jsonNode = objectMapper.readTree(result);
            
            // Check for error
            if (jsonNode.has("error")) {
                return OcrResponse.error(jsonNode.get("error").asText());
            }
            
            // Extract text - check both direct field and nested in data field
            String text = "";
            if (jsonNode.has("text")) {
                text = jsonNode.get("text").asText();
            } else if (jsonNode.has("data") && jsonNode.get("data").has("text")) {
                text = jsonNode.get("data").get("text").asText();
            }
            
            Long timeMs = duration;
            if (jsonNode.has("time_ms")) {
                timeMs = jsonNode.get("time_ms").asLong();
            } else if (jsonNode.has("data") && jsonNode.get("data").has("processing_time")) {
                timeMs = (long) (jsonNode.get("data").get("processing_time").asDouble() * 1000);
            }
            
            OcrResponse response = OcrResponse.success(text, timeMs);
            response.setEngine(request.getEngine());
            response.setLanguage(request.getLanguage());
            
            return response;
            
        } catch (Exception e) {
            logger.error("Failed to parse OCR result", e);
            return OcrResponse.error("Failed to parse OCR result: " + e.getMessage());
        }
    }
}
