package com.deepapp.vn.io.workers;

import com.deepapp.hub.EventChunk;
import com.deepapp.vn.io.ZZ.A0.ZZA0_0100.service.DocumentStreamRegistry;
import com.deepapp.vn.io.model.Detection;
import com.deepapp.vn.io.model.DetectionResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger; 
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * Generic C++ Worker Client
 * Can call any C++ worker by task ID
 */
public class CppWorkerClient extends BaseWorkerClient {

    private static final Logger logger = LoggerFactory.getLogger(CppWorkerClient.class);

    @Value("${workers.cpp.targetId:cpp-worker}")
    private String cppTargetId;
    
    @Value("${cpp.worker.images.path:/tmp/deepapp/images}")
    private String imagesTempPath;
    
    @Autowired(required = false)
    private DocumentStreamRegistry streamRegistry;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CppWorkerClient(
            @Value("${workers.cpp.host:localhost}") String host,
            @Value("${workers.cpp.port:50051}") int port) {
        super("java-cpp-client", host, port);
        logger.info("CppWorkerClient created with host={}, port={}", host, port);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        logger.info("C++ Worker Client initialized - target: {}", cppTargetId);
    }

    @Override
    protected String getWorkerTargetId() {
        return cppTargetId;
    }

    /**
     * Call a specific C++ worker task
     * @param taskId The worker task ID (e.g., "AAA0_0100_W")
     * @param eventType The event type (e.g., "process", "echo")
     * @param taskData The task data
     */
    public java.util.concurrent.CompletableFuture<String> callWorker(
            String taskId, String eventType, String taskData) {
        logger.info("Calling C++ worker: taskId={}, eventType={}", taskId, eventType);
        
        // Send to cpp-worker (C++ clientId) with taskId in metadata
        // C++ worker will route internally based on taskId
        byte[] payload = taskData.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        
        // Create metadata with taskId for internal C++ routing
        java.util.Map<String, String> metadata = new java.util.HashMap<>();
        metadata.put("taskId", taskId);
        
        // Send to cpp-worker, not taskId directly
        // Increased timeout to 10 minutes (600000ms) for large document processing
        return sendEventAndWaitForResponse(cppTargetId, eventType, payload, metadata, 600000)
                .thenApply(event -> event.getPayload().toStringUtf8());
    }

    /**
     * Call AAA0_0100_W worker
     */
    public java.util.concurrent.CompletableFuture<String> callAAA0_0100(
            String eventType, String data) {
        return callWorker("AAA0_0100_W", eventType, data);
    }

    /**
     * Call AAA0_0200_W worker
     */
    public java.util.concurrent.CompletableFuture<String> callAAA0_0200(
            String eventType, String data) {
        return callWorker("AAA0_0200_W", eventType, data);
    }

    @Override
    protected void handleWorkerEvent(EventChunk event) {
        logger.info("C++ Worker event: {} - {}", 
            event.getEventType(), event.getPayload().toStringUtf8());
        
        // Handle document streaming events from C++
        if ("document_event".equals(event.getEventType())) {
            handleDocumentEvent(event);
        }
    }
    
    /**
     * Handle document streaming events from C++ worker
     * C++ sends: document_metadata, document_page, document_complete
     */
    private void handleDocumentEvent(EventChunk event) {
        try {
            String payload = event.getPayload().toStringUtf8();
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            
            String requestId = (String) data.get("requestId");
            String eventType = (String) data.get("eventType");
            
            if (requestId == null || streamRegistry == null) {
                logger.warn("No requestId or streamRegistry not available");
                return;
            }
            
            SseEmitter emitter = streamRegistry.getEmitter(requestId);
            if (emitter == null) {
                logger.warn("No SSE emitter found for requestId: {}", requestId);
                return;
            }
            
            // Forward event to SSE client
            logger.info("Forwarding {} to SSE client for requestId: {}", eventType, requestId);
            
            if ("document_metadata".equals(eventType)) {
                emitter.send(SseEmitter.event()
                    .name("metadata")
                    .data(data));
                    
            } else if ("document_page".equals(eventType)) {
                emitter.send(SseEmitter.event()
                    .name("page")
                    .data(data));
                    
            } else if ("document_complete".equals(eventType)) {
                emitter.send(SseEmitter.event()
                    .name("complete")
                    .data(data));
                emitter.complete();
                streamRegistry.removeStream(requestId);
            }
            
        } catch (Exception e) {
            logger.error("Error handling document event", e);
        }
    }

    /**
     * Detect objects using YOLO model via C++ worker
     */
    public DetectionResult detectObjects(byte[] imageBytes, String model, Double confidence, Double iou, 
                                       Integer maxDetections, Integer imgSize, Boolean augment, Boolean halfPrecision) throws Exception {
        logger.info("Sending YOLO detection request to C++ worker - Model: {}, Confidence: {}, IOU: {}, MaxDetections: {}, ImgSize: {}, Augment: {}, HalfPrecision: {}", 
                   model, confidence, iou, maxDetections, imgSize, augment, halfPrecision);

        java.nio.file.Path imagePath = null;
        try {
            // Create temporary file for image
            java.nio.file.Path tempDir = java.nio.file.Paths.get(imagesTempPath);
            java.nio.file.Files.createDirectories(tempDir);
            
            String fileName = "yolo_input_" + System.currentTimeMillis() + "_" + java.util.concurrent.ThreadLocalRandom.current().nextInt(1000) + ".jpg";
            imagePath = tempDir.resolve(fileName);
            
            // Write image bytes to temporary file
            java.nio.file.Files.write(imagePath, imageBytes);
            logger.debug("Saved image to temporary file: {}", imagePath.toString());

            // Create JSON payload with file path instead of base64 image data
            Map<String, Object> payload = new HashMap<>();
            payload.put("image_path", imagePath.toString());
            payload.put("model", model);
            payload.put("confidence", confidence);
            payload.put("iou", iou);
            payload.put("max_detections", maxDetections);
            payload.put("img_size", imgSize);
            payload.put("augment", augment);
            payload.put("half_precision", halfPrecision);

            String jsonPayload = objectMapper.writeValueAsString(payload);

            // Call YOLO detection task
            java.util.concurrent.CompletableFuture<String> future = 
                callWorker("ZZA0_0102_W", "detect", jsonPayload);
            
            String response = future.get();
            
            // Parse response
            @SuppressWarnings("unchecked")
            Map<String, Object> responseData = objectMapper.readValue(response, Map.class);
            
            DetectionResult result = new DetectionResult();
            result.setModel(model);
            // Use actual processing time from response if available
            if (responseData.containsKey("processingTime")) {
                result.setProcessingTime(((Number) responseData.get("processingTime")).longValue());
            } else {
                result.setProcessingTime(150); // Fallback
            }
            
            // Parse detections if available
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> detectionList = 
                (java.util.List<Map<String, Object>>) responseData.get("detections");
            
            if (detectionList != null && !detectionList.isEmpty()) {
                java.util.List<Detection> detections = new java.util.ArrayList<>();
                for (Map<String, Object> det : detectionList) {
                    Detection detection = new Detection();
                    // Handle both "class" and "label" fields
                    String className = (String) det.get("class");
                    if (className == null) {
                        className = (String) det.get("label");
                    }
                    detection.setClassName(className != null ? className : "unknown");
                    
                    detection.setConfidence(((Number) det.get("confidence")).doubleValue());
                    
                    // Handle bbox format: can be x,y,width,height or x1,y1,x2,y2
                    @SuppressWarnings("unchecked")
                    Map<String, Object> bbox = (Map<String, Object>) det.get("bbox");
                    if (bbox != null) {
                        if (bbox.containsKey("x1")) {
                            // x1,y1,x2,y2 format
                            double x1 = ((Number) bbox.get("x1")).doubleValue();
                            double y1 = ((Number) bbox.get("y1")).doubleValue();
                            double x2 = ((Number) bbox.get("x2")).doubleValue();
                            double y2 = ((Number) bbox.get("y2")).doubleValue();
                            detection.setX((int) x1);
                            detection.setY((int) y1);
                            detection.setWidth((int) (x2 - x1));
                            detection.setHeight((int) (y2 - y1));
                        } else {
                            // x,y,width,height format
                            detection.setX(((Number) bbox.get("x")).intValue());
                            detection.setY(((Number) bbox.get("y")).intValue());
                            detection.setWidth(((Number) bbox.get("width")).intValue());
                            detection.setHeight(((Number) bbox.get("height")).intValue());
                        }
                    } else {
                        // Fallback to direct fields
                        detection.setX(((Number) det.get("x")).intValue());
                        detection.setY(((Number) det.get("y")).intValue());
                        detection.setWidth(((Number) det.get("width")).intValue());
                        detection.setHeight(((Number) det.get("height")).intValue());
                    }
                    
                    detections.add(detection);
                }
                result.setDetections(detections);
            } else {
                // No detections found - return empty list instead of mock data
                logger.warn("No detections returned from C++ worker - returning empty result");
                result.setDetections(java.util.Collections.emptyList());
            }

            logger.info("YOLO detection completed - Found {} objects", result.getDetections().size());
            
            // Clean up temporary file
            try {
                java.nio.file.Files.deleteIfExists(imagePath);
                logger.debug("Cleaned up temporary image file: {}", imagePath.toString());
            } catch (Exception e) {
                logger.warn("Failed to cleanup temporary image file: {}", imagePath.toString(), e);
            }
            
            return result;

        } catch (Exception e) {
            // Clean up temporary file on error
            try {
                java.nio.file.Files.deleteIfExists(imagePath);
                logger.debug("Cleaned up temporary image file after error: {}", imagePath.toString());
            } catch (Exception cleanupException) {
                logger.warn("Failed to cleanup temporary image file after error: {}", imagePath.toString(), cleanupException);
            }
            
            logger.error("YOLO detection failed", e);
            throw new RuntimeException("C++ worker detection failed: " + e.getMessage(), e);
        }
    }

    /**
     * Check if C++ worker is available
     */
    public boolean isAvailable() {
        try {
            // Test with a known worker (YOLO worker) and health_check event
            java.util.concurrent.CompletableFuture<String> future = 
                callWorker("ZZA0_0102_W", "health_check", "{}");
            String response = future.get(5, java.util.concurrent.TimeUnit.SECONDS);
            return response != null && !response.contains("error");
        } catch (Exception e) {
            logger.warn("C++ worker availability check failed: {}", e.getMessage());
            return false;
        }
    }
}
