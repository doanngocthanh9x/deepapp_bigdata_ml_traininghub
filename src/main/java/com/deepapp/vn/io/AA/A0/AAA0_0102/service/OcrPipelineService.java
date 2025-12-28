package com.deepapp.vn.io.AA.A0.AAA0_0102.service;

import com.deepapp.vn.io.AA.A0.AAA0_0102.model.OcrPipelineRequest;
import com.deepapp.vn.io.AA.A0.AAA0_0102.model.OcrPipelineResponse;
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
 * OCR Pipeline Service - Business logic for OCR pipeline operations
 * Combines PaddleOCR detection/classification with VietOCR recognition
 */
@Service
public class OcrPipelineService {

    private static final Logger logger = LoggerFactory.getLogger(OcrPipelineService.class);
    private static final int OCR_TIMEOUT_SECONDS = 60; // Longer timeout for pipeline
    private static final String OCR_WORKER_ID = "AAA0_0102_W";  // OCR Pipeline worker task ID

    @Autowired
    private PythonWorkerClient pythonWorkerClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Process OCR pipeline request
     */
    public OcrPipelineResponse processOcrPipeline(OcrPipelineRequest request) {
        try {
            // Validate request
            if (!request.hasImageData()) {
                return OcrPipelineResponse.error("Either 'image' or 'imagePath' must be provided");
            }

            // Build JSON payload for Python worker
            String payload = buildOcrPipelinePayload(request);
            logger.info("Sending OCR pipeline request to worker: engine={}, language={}",
                       request.getEngine(), request.getLanguage());

            // Call OCR pipeline worker via gRPC using PythonWorkerClient
            long startTime = System.currentTimeMillis();
            String result = pythonWorkerClient.callWorker(OCR_WORKER_ID, "recognize_pipeline", payload)
                    .get(OCR_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            long duration = System.currentTimeMillis() - startTime;

            logger.info("OCR pipeline completed in {} ms", duration);

            // Parse result
            return parseWorkerResponse(result, duration);

        } catch (Exception e) {
            logger.error("OCR pipeline processing failed", e);
            return OcrPipelineResponse.error("OCR pipeline processing failed: " + e.getMessage());
        }
    }

    /**
     * Build JSON payload for OCR pipeline worker
     */
    private String buildOcrPipelinePayload(OcrPipelineRequest request) {
        try {
            // Create payload object
            com.fasterxml.jackson.databind.node.ObjectNode payload = objectMapper.createObjectNode();

            // Add image data
            if (request.getImage() != null) {
                payload.put("image", request.getImage());
            } else if (request.getImagePath() != null) {
                // For file path, we might need to read the file and encode it
                // For now, assume base64 is provided
                payload.put("imagePath", request.getImagePath());
            }

            // Add other parameters
            payload.put("engine", request.getEngine());
            payload.put("language", request.getLanguage());

            return objectMapper.writeValueAsString(payload);

        } catch (Exception e) {
            logger.error("Failed to build OCR pipeline payload", e);
            throw new RuntimeException("Failed to build payload", e);
        }
    }

    /**
     * Parse worker response into OcrPipelineResponse
     */
    private OcrPipelineResponse parseWorkerResponse(String result, long processingTimeMs) {
        try {
            JsonNode root = objectMapper.readTree(result);

            String status = root.path("status").asText();
            String message = root.path("message").asText();

            if ("success".equals(status)) {
                // Parse successful response
                JsonNode dataNode = root.path("data");
                OcrPipelineResponse.OcrPipelineData data = objectMapper.treeToValue(
                    dataNode, OcrPipelineResponse.OcrPipelineData.class);

                // Set processing time
                data.setProcessingTimeMs(processingTimeMs);

                return OcrPipelineResponse.success(data);

            } else {
                // Handle error response
                return OcrPipelineResponse.error(message);
            }

        } catch (Exception e) {
            logger.error("Failed to parse worker response", e);
            return OcrPipelineResponse.error("Failed to parse response: " + e.getMessage());
        }
    }

    /**
     * Health check for OCR pipeline service
     */
    public OcrPipelineResponse healthCheck() {
        try {
            String payload = "{}";
            String result = pythonWorkerClient.callWorker(OCR_WORKER_ID, "health_check", payload)
                    .get(10, TimeUnit.SECONDS);

            JsonNode root = objectMapper.readTree(result);
            String status = root.path("status").asText();

            if ("success".equals(status)) {
                JsonNode dataNode = root.path("data");
                String healthStatus = dataNode.path("status").asText();
                return new OcrPipelineResponse("healthy".equals(healthStatus), "Health check completed", null);
            } else {
                return OcrPipelineResponse.error("Worker health check failed");
            }

        } catch (Exception e) {
            logger.error("Health check failed", e);
            return OcrPipelineResponse.error("Health check failed: " + e.getMessage());
        }
    }
}