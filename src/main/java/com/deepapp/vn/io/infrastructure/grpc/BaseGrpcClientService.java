package com.deepapp.vn.io.infrastructure.grpc;

import com.deepapp.hub.DataStreamGrpc;
import com.deepapp.hub.EventChunk;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
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
    protected FileServiceClient fileServiceClient;
    
    // Track pending requests for synchronous communication
    protected final ConcurrentHashMap<String, CompletableFuture<EventChunk>> pendingRequests = new ConcurrentHashMap<>();
    protected final AtomicLong requestCounter = new AtomicLong(0);

    private final String serverHost;
    private final int serverPort;
    
    // Reconnection management
    private volatile boolean isShuttingDown = false;
    private volatile boolean isConnected = false;
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final long RECONNECT_DELAY_MS = 5000;

    public BaseGrpcClientService(String clientId, String serverHost, int serverPort) {
        this.clientId = clientId;
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            logger.info("Initializing gRPC client '{}' to {}:{}", clientId, serverHost, serverPort);
            
            // Set max message sizes to prevent BufferOverflowException
            int maxMessageSize = 200 * 1024 * 1024;  // 200MB

            channel = NettyChannelBuilder.forAddress(serverHost, serverPort)
                    .usePlaintext()
                    .keepAliveTime(10, TimeUnit.MINUTES)  // Increased from 2 to 10 minutes
                    .keepAliveTimeout(30, TimeUnit.SECONDS)  // Increased timeout
                    .keepAliveWithoutCalls(false)  // Only keepalive during active calls
                    .idleTimeout(Long.MAX_VALUE, TimeUnit.DAYS)  // Disable idle timeout
                    .maxInboundMessageSize(maxMessageSize)   // 200MB for receiving messages
                    .maxInboundMetadataSize(maxMessageSize)  // 200MB for metadata
                    .build();
            
            asyncStub = DataStreamGrpc.newStub(channel);
            fileServiceClient = new FileServiceClient(channel);
            setupBidirectionalStream();
            
            logger.info("gRPC client '{}' initialized with keep-alive and auto-reconnect", clientId);
        } catch (Exception e) {
            logger.error("Failed to initialize gRPC client '{}': {}", clientId, e.getMessage(), e);
            throw new RuntimeException("Failed to initialize gRPC client", e);
        }
    }

    @Override
    public void destroy() throws Exception {
        logger.info("Shutting down gRPC client '{}'", clientId);
        isShuttingDown = true;
        isConnected = false;
        
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
                isConnected = false;
                handleStreamError(t);
                
                // Auto-reconnect if not shutting down
                if (!isShuttingDown) {
                    scheduleReconnect();
                }
            }

            @Override
            public void onCompleted() {
                logger.info("gRPC stream completed for client '{}'", clientId);
                isConnected = false;
                handleStreamCompleted();
                
                // Auto-reconnect if not shutting down
                if (!isShuttingDown) {
                    scheduleReconnect();
                }
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
        isConnected = true;
        logger.info("Sent connection message for client '{}' - Stream established", clientId);
    }
    
    /**
     * Schedule reconnection with exponential backoff
     */
    private void scheduleReconnect() {
        if (isShuttingDown) {
            logger.info("Client '{}' is shutting down, skip reconnect", clientId);
            return;
        }
        
        new Thread(() -> {
            for (int attempt = 1; attempt <= MAX_RECONNECT_ATTEMPTS; attempt++) {
                if (isShuttingDown) {
                    break;
                }
                
                try {
                    long delay = RECONNECT_DELAY_MS * attempt;
                    logger.info("Client '{}' will reconnect in {} seconds (attempt {}/{})", 
                               clientId, delay/1000, attempt, MAX_RECONNECT_ATTEMPTS);
                    Thread.sleep(delay);
                    
                    if (isShuttingDown) {
                        break;
                    }
                    
                    logger.info("Client '{}' attempting reconnection (attempt {}/{})", 
                               clientId, attempt, MAX_RECONNECT_ATTEMPTS);
                    
                    // Re-establish stream
                    setupBidirectionalStream();
                    logger.info("Client '{}' reconnected successfully", clientId);
                    return;
                    
                } catch (Exception e) {
                    logger.error("Client '{}' reconnection attempt {} failed: {}", 
                                clientId, attempt, e.getMessage());
                    if (attempt == MAX_RECONNECT_ATTEMPTS) {
                        logger.error("Client '{}' failed to reconnect after {} attempts", 
                                    clientId, MAX_RECONNECT_ATTEMPTS);
                        handleReconnectFailed();
                    }
                }
            }
        }, "reconnect-" + clientId).start();
    }
    
    /**
     * Check if client is connected
     */
    public boolean isConnected() {
        return isConnected && requestObserver != null;
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
        if (requestObserver == null || !isConnected) {
            logger.warn("Client '{}' is not connected, cannot send event '{}'", clientId, eventType);
            throw new IllegalStateException("gRPC client not connected");
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

        // Debug: Log event size
        int eventSize = event.getSerializedSize();
        logger.info("Sending event '{}' from '{}' to '{}' - Event size: {} bytes ({} KB)",
                    eventType, clientId, targetId, eventSize, eventSize / 1024.0);

        try {
            requestObserver.onNext(event);
            logger.info("Sent event '{}' from '{}' to '{}' with requestId={}",
                        eventType, clientId, targetId, event.getMetadataMap().get("requestId"));
        } catch (Exception e) {
            logger.error("Failed to send event '{}': {} - Attempting to recreate stream", eventType, e.getMessage());

            // Mark as disconnected and trigger reconnection
            isConnected = false;

            // Try to recreate stream immediately
            try {
                setupBidirectionalStream();
                logger.info("Stream recreated, retrying send");

                // Retry send once
                requestObserver.onNext(event);
                logger.info("Retry successful: Sent event '{}' from '{}' to '{}'",
                            eventType, clientId, targetId);
            } catch (Exception retryError) {
                logger.error("Retry failed for event '{}': {}", eventType, retryError.getMessage());
                throw new RuntimeException("Failed to send event after stream recreation: " + retryError.getMessage(), retryError);
            }
        }
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

    public FileServiceClient getFileServiceClient() {
        return fileServiceClient;
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
    
    /**
     * Override this method to handle reconnection failure
     */
    protected void handleReconnectFailed() {
        logger.error("Client '{}' failed to reconnect - may need manual intervention", clientId);
    }
}
