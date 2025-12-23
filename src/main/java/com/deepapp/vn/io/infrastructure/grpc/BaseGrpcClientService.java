package com.deepapp.vn.io.infrastructure.grpc;

import com.deepapp.hub.DataStreamGrpc;
import com.deepapp.hub.EventChunk;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Base gRPC client service for managing bidirectional streaming connections.
 * This class handles the low-level gRPC communication and provides a foundation
 * for building scalable worker clients.
 */
public abstract class BaseGrpcClientService implements InitializingBean, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(BaseGrpcClientService.class);

    protected ManagedChannel channel;
    protected DataStreamGrpc.DataStreamStub asyncStub;
    protected StreamObserver<EventChunk> requestObserver;
    protected String clientId;
    
    // Track pending requests for synchronous communication
    protected final ConcurrentHashMap<String, CompletableFuture<EventChunk>> pendingRequests = new ConcurrentHashMap<>();
    protected final AtomicLong requestCounter = new AtomicLong(0);

    private final String serverHost;
    private final int serverPort;

    public BaseGrpcClientService(String clientId, String serverHost, int serverPort) {
        this.clientId = clientId;
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            logger.info("Initializing gRPC client '{}' to {}:{}", clientId, serverHost, serverPort);
            
            channel = ManagedChannelBuilder.forAddress(serverHost, serverPort)
                    .usePlaintext()
                    .build();
            
            asyncStub = DataStreamGrpc.newStub(channel);
            setupBidirectionalStream();
            
            logger.info("gRPC client '{}' initialized successfully", clientId);
        } catch (Exception e) {
            logger.error("Failed to initialize gRPC client '{}': {}", clientId, e.getMessage(), e);
            throw new RuntimeException("Failed to initialize gRPC client", e);
        }
    }

    @Override
    public void destroy() throws Exception {
        logger.info("Shutting down gRPC client '{}'", clientId);
        if (requestObserver != null) {
            try {
                requestObserver.onCompleted();
            } catch (Exception e) {
                logger.warn("Error completing request observer: {}", e.getMessage());
            }
        }
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
            try {
                if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                    channel.shutdownNow();
                }
            } catch (InterruptedException e) {
                channel.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void setupBidirectionalStream() {
        StreamObserver<EventChunk> responseObserver = new StreamObserver<EventChunk>() {
            @Override
            public void onNext(EventChunk event) {
                logger.debug("Received event '{}' from '{}' to '{}'",
                        event.getEventType(), event.getSenderId(), event.getTargetId());
                
                // Check if this is a response to a pending request
                String requestId = event.getMetadataOrDefault("requestId", null);
                if (requestId != null && pendingRequests.containsKey(requestId)) {
                    CompletableFuture<EventChunk> future = pendingRequests.remove(requestId);
                    future.complete(event);
                    return;
                }
                
                // Handle custom events
                handleIncomingEvent(event);
            }

            @Override
            public void onError(Throwable t) {
                logger.error("gRPC stream error for client '{}': {}", clientId, t.getMessage(), t);
                handleStreamError(t);
            }

            @Override
            public void onCompleted() {
                logger.info("gRPC stream completed for client '{}'", clientId);
                handleStreamCompleted();
            }
        };

        requestObserver = asyncStub.streamEvents(responseObserver);

        // Send initial connection message
        EventChunk connectMessage = EventChunk.newBuilder()
                .setSenderId(clientId)
                .setTargetId("")
                .setEventType("connect")
                .setTimestamp(System.currentTimeMillis())
                .build();

        requestObserver.onNext(connectMessage);
        logger.info("Sent connection message for client '{}'", clientId);
    }

    /**
     * Send an event asynchronously (fire and forget)
     */
    public void sendEvent(String targetId, String eventType, byte[] payload) {
        sendEvent(targetId, eventType, payload, null);
    }

    /**
     * Send an event with custom metadata
     */
    public void sendEvent(String targetId, String eventType, byte[] payload, Map<String, String> metadata) {
        if (requestObserver == null) {
            throw new IllegalStateException("gRPC client not initialized");
        }

        EventChunk.Builder builder = EventChunk.newBuilder()
                .setSenderId(clientId)
                .setTargetId(targetId != null ? targetId : "")
                .setEventType(eventType != null ? eventType : "message")
                .setPayload(com.google.protobuf.ByteString.copyFrom(payload != null ? payload : new byte[0]))
                .setTimestamp(System.currentTimeMillis());

        if (metadata != null) {
            builder.putAllMetadata(metadata);
        }

        EventChunk event = builder.build();
        requestObserver.onNext(event);
        logger.info("Sent event '{}' from '{}' to '{}' with requestId={}", 
                    eventType, clientId, targetId, event.getMetadataMap().get("requestId"));
    }

    /**
     * Send an event and wait for a response
     */
    public CompletableFuture<EventChunk> sendEventAndWaitForResponse(
            String targetId, String eventType, byte[] payload, long timeoutMs) {
        return sendEventAndWaitForResponse(targetId, eventType, payload, null, timeoutMs);
    }

    /**
     * Send an event and wait for a response with custom metadata
     */
    public CompletableFuture<EventChunk> sendEventAndWaitForResponse(
            String targetId, String eventType, byte[] payload, 
            Map<String, String> customMetadata, long timeoutMs) {
        
        String requestId = clientId + "_req_" + requestCounter.incrementAndGet();
        CompletableFuture<EventChunk> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);

        Map<String, String> metadata = new ConcurrentHashMap<>();
        metadata.put("requestId", requestId);
        if (customMetadata != null) {
            metadata.putAll(customMetadata);
        }
        
        sendEvent(targetId, eventType, payload, metadata);

        // Set timeout
        CompletableFuture.delayedExecutor(timeoutMs, TimeUnit.MILLISECONDS).execute(() -> {
            if (pendingRequests.remove(requestId) != null) {
                future.completeExceptionally(new RuntimeException("Request timeout"));
            }
        });

        return future;
    }

    public String getClientId() {
        return clientId;
    }

    /**
     * Override this method to handle incoming events from the server
     */
    protected abstract void handleIncomingEvent(EventChunk event);

    /**
     * Override this method to handle stream errors
     */
    protected void handleStreamError(Throwable t) {
        // Default implementation - can be overridden
    }

    /**
     * Override this method to handle stream completion
     */
    protected void handleStreamCompleted() {
        // Default implementation - can be overridden
    }
}
