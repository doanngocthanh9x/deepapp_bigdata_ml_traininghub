package com.deepapp.vn.io.workers;

import com.deepapp.hub.EventChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Generic C++ Worker Client
 * Can call any C++ worker by task ID
 */
@Service
public class CppWorkerClient extends BaseWorkerClient {

    private static final Logger logger = LoggerFactory.getLogger(CppWorkerClient.class);

    @Value("${workers.cpp.targetId:cpp-worker}")
    private String cppTargetId;

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
        return sendEventAndWaitForResponse(cppTargetId, eventType, payload, metadata, 30000)
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
    }
}
