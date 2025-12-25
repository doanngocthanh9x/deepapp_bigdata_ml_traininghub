package com.deepapp.vn.io.workers;

import com.deepapp.hub.EventChunk;
import com.deepapp.vn.io.ZZ.A0.ZZA0_0100.service.DocumentStreamRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * Generic C++ Worker Client
 * Can call any C++ worker by task ID
 */
public class CppWorkerClient extends BaseWorkerClient {

    private static final Logger logger = LoggerFactory.getLogger(CppWorkerClient.class);

    @Value("${workers.cpp.targetId:cpp-worker}")
    private String cppTargetId;
    
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
}
