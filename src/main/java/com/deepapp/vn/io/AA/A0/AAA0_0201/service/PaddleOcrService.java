package com.deepapp.vn.io.AA.A0.AAA0_0201.service;

import com.deepapp.vn.io.AA.A0.AAA0_0201.model.PaddleOcrRequest;
import com.deepapp.vn.io.AA.A0.AAA0_0201.model.PaddleOcrResponse;
import com.deepapp.vn.io.workers.PythonWorkerClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * PaddleOCR Text Recognition Service - Business logic for PaddleOCR operations
 */
@Service
public class PaddleOcrService {

    private static final Logger logger = LoggerFactory.getLogger(PaddleOcrService.class);
    private static final int OCR_TIMEOUT_SECONDS = 30;
    private static final String PADDLE_OCR_WORKER_ID = "AAA0_0201_W";  // Python PaddleOCR worker task ID

    @Autowired
    private PythonWorkerClient pythonWorkerClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Process PaddleOCR text recognition request
     */
    public PaddleOcrResponse processPaddleOcr(PaddleOcrRequest request) {
        try {
            // Validate request
            if (!request.hasImageData()) {
                return PaddleOcrResponse.error("Either 'image' or 'imagePath' must be provided");
            }

            // Build JSON payload for Python worker
            String payload = buildPaddleOcrPayload(request);
            logger.info("Sending PaddleOCR request to worker: model={}, language={}",
                       request.getModel(), request.getLanguage());

            // Call PaddleOCR worker via gRPC using PythonWorkerClient
            long startTime = System.currentTimeMillis();
            String result = pythonWorkerClient.callWorker(PADDLE_OCR_WORKER_ID, "recognize", payload).get();
            long duration = System.currentTimeMillis() - startTime;

            logger.info("PaddleOCR completed in {}ms", duration);

            // Parse response from Python worker
            return parsePaddleOcrResult(result, duration, request);

        } catch (Exception e) {
            logger.error("PaddleOCR processing failed", e);
            return PaddleOcrResponse.error("PaddleOCR processing failed: " + e.getMessage());
        }
    }

    /**
     * Build JSON payload for Python PaddleOCR worker
     */
    private String buildPaddleOcrPayload(PaddleOcrRequest request) {
        try {
            StringBuilder json = new StringBuilder("{");

            if (request.getImage() != null && !request.getImage().isEmpty()) {
                json.append("\"image\":\"").append(request.getImage()).append("\",");
            }

            if (request.getImagePath() != null && !request.getImagePath().isEmpty()) {
                json.append("\"imagePath\":\"").append(request.getImagePath()).append("\",");
            }

            json.append("\"language\":\"").append(request.getLanguage()).append("\",");
            json.append("\"model\":\"").append(request.getModel()).append("\"");
            json.append("}");

            return json.toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to build PaddleOCR payload", e);
        }
    }

    /**
     * Parse PaddleOCR result from Python worker
     */
    private PaddleOcrResponse parsePaddleOcrResult(String result, long duration, PaddleOcrRequest request) {
        try {
            JsonNode jsonNode = objectMapper.readTree(result);

            // Check for error
            if (jsonNode.has("error")) {
                return PaddleOcrResponse.error(jsonNode.get("error").asText());
            }

            // Extract text - check both direct field and nested in data field
            String text = "";
            if (jsonNode.has("text")) {
                text = jsonNode.get("text").asText();
            } else if (jsonNode.has("data") && jsonNode.get("data").has("text")) {
                text = jsonNode.get("data").get("text").asText();
            }

            // Extract confidence
            Double confidence = null;
            if (jsonNode.has("confidence")) {
                confidence = jsonNode.get("confidence").asDouble();
            } else if (jsonNode.has("data") && jsonNode.get("data").has("confidence")) {
                confidence = jsonNode.get("data").get("confidence").asDouble();
            }

            // Extract original size
            int originalWidth = 0;
            int originalHeight = 0;
            if (jsonNode.has("original_size")) {
                JsonNode sizeNode = jsonNode.get("original_size");
                if (sizeNode.isArray() && sizeNode.size() >= 2) {
                    originalWidth = sizeNode.get(0).asInt();
                    originalHeight = sizeNode.get(1).asInt();
                }
            }

            PaddleOcrResponse response;
            if (confidence != null) {
                response = PaddleOcrResponse.success(text, duration, confidence);
            } else {
                response = PaddleOcrResponse.success(text, duration);
            }

            return response;

        } catch (Exception e) {
            logger.error("Failed to parse PaddleOCR result", e);
            return PaddleOcrResponse.error("Failed to parse PaddleOCR result: " + e.getMessage());
        }
    }
}