package com.deepapp.vn.io.ZZ.A0.ZZA0_0102.service;

import com.deepapp.vn.io.ZZ.A0.ZZA0_0102.model.YoloDetectionRequest;
import com.deepapp.vn.io.ZZ.A0.ZZA0_0102.model.YoloDetectionResponse;
import com.deepapp.vn.io.workers.CppWorkerClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * YOLO Detection Service - Business logic for YOLO object detection operations
 */
@Service
public class YoloDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(YoloDetectionService.class);
    private static final int DETECTION_TIMEOUT_SECONDS = 60;
    private static final String YOLO_WORKER_ID = "ZZA0_0102_W";  // C++ YOLO worker task ID

    @Autowired
    private CppWorkerClient cppWorkerClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Process YOLO detection request
     */
    public YoloDetectionResponse processDetection(YoloDetectionRequest request) {
        return processDetection(request, "detect");
    }

    /**
     * Process YOLO detection request with custom event type
     */
    public YoloDetectionResponse processDetection(YoloDetectionRequest request, String eventType) {
        try {
            // Validate request
            if (!request.hasImageData()) {
                return createErrorResponse("Either 'image' or 'imagePath' must be provided");
            }

            // Build JSON payload for C++ worker
            String payload = buildDetectionPayload(request);
            logger.info("Sending YOLO {} request to worker: model={}, confidence={}",
                       eventType, request.getModel(), request.getConfidence());

            // Call YOLO worker via gRPC using CppWorkerClient
            long startTime = System.currentTimeMillis();
            String result = cppWorkerClient.callWorker(YOLO_WORKER_ID, eventType, payload).get();
            long duration = System.currentTimeMillis() - startTime;

            logger.info("YOLO {} completed in {}ms", eventType, duration);

            // Parse response from C++ worker
            return parseDetectionResult(result, duration, request);

        } catch (Exception e) {
            logger.error("YOLO {} processing failed", eventType, e);
            return createErrorResponse("YOLO " + eventType + " processing failed: " + e.getMessage());
        }
    }

    /**
     * Build JSON payload for C++ YOLO worker
     */
    private String buildDetectionPayload(YoloDetectionRequest request) {
        try {
            StringBuilder json = new StringBuilder("{");

            if (request.getImage() != null && !request.getImage().isEmpty()) {
                json.append("\"image\":\"").append(request.getImage()).append("\",");
            }

            if (request.getImagePath() != null && !request.getImagePath().isEmpty()) {
                json.append("\"imagePath\":\"").append(request.getImagePath()).append("\",");
            }

            json.append("\"model\":\"").append(request.getModel()).append("\",");
            json.append("\"confidence\":").append(request.getConfidence()).append(",");
            json.append("\"iouThreshold\":").append(request.getIouThreshold());
            json.append("}");

            return json.toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to build YOLO detection payload", e);
        }
    }

    /**
     * Parse YOLO detection result from C++ worker
     */
    private YoloDetectionResponse parseDetectionResult(String result, long duration, YoloDetectionRequest request) {
        try {
            JsonNode jsonNode = objectMapper.readTree(result);

            // Check for error
            if (jsonNode.has("error")) {
                return createErrorResponse(jsonNode.get("error").asText());
            }

            // Build successful response
            YoloDetectionResponse response = new YoloDetectionResponse();
            response.setStatus("success");
            response.setModel(request.getModel());
            response.setConfidence(request.getConfidence());
            response.setIouThreshold(request.getIouThreshold());
            response.setProcessingTime(duration);

            // Parse detections
            if (jsonNode.has("detections")) {
                List<YoloDetectionResponse.DetectionResult> detections = new ArrayList<>();
                JsonNode detectionsNode = jsonNode.get("detections");

                for (JsonNode detection : detectionsNode) {
                    YoloDetectionResponse.DetectionResult det = new YoloDetectionResponse.DetectionResult();
                    det.setLabel(detection.get("label").asText());
                    det.setConfidence(detection.get("confidence").asDouble());

                    // Parse bounding box
                    if (detection.has("bbox")) {
                        JsonNode bbox = detection.get("bbox");
                        YoloDetectionResponse.BoundingBox box = new YoloDetectionResponse.BoundingBox();
                        box.setX1(bbox.get("x1").asDouble());
                        box.setY1(bbox.get("y1").asDouble());
                        box.setX2(bbox.get("x2").asDouble());
                        box.setY2(bbox.get("y2").asDouble());
                        det.setBbox(box);
                    }

                    detections.add(det);
                }

                response.setDetections(detections);
            }

            // Parse image dimensions
            if (jsonNode.has("width") && jsonNode.has("height")) {
                YoloDetectionResponse.ImageDimensions dims = new YoloDetectionResponse.ImageDimensions();
                dims.setWidth(jsonNode.get("width").asInt());
                dims.setHeight(jsonNode.get("height").asInt());
                response.setDimensions(dims);
            }

            return response;

        } catch (Exception e) {
            logger.error("Failed to parse YOLO detection result", e);
            return createErrorResponse("Failed to parse YOLO detection result: " + e.getMessage());
        }
    }

    /**
     * Create error response
     */
    private YoloDetectionResponse createErrorResponse(String error) {
        YoloDetectionResponse response = new YoloDetectionResponse();
        response.setStatus("error");
        response.setError(error);
        return response;
    }
}