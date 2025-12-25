package com.deepapp.vn.io.ZZ.A0.ZZA0_0100.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry to track active SSE connections for document streaming
 * Maps requestId -> SseEmitter
 */
@Service
public class DocumentStreamRegistry {

    private final Map<String, SseEmitter> activeStreams = new ConcurrentHashMap<>();

    /**
     * Register a new stream for a request
     */
    public void registerStream(String requestId, SseEmitter emitter) {
        activeStreams.put(requestId, emitter);
        
        // Auto cleanup on completion/timeout/error
        emitter.onCompletion(() -> activeStreams.remove(requestId));
        emitter.onTimeout(() -> activeStreams.remove(requestId));
        emitter.onError((e) -> activeStreams.remove(requestId));
    }

    /**
     * Get emitter for a request
     */
    public SseEmitter getEmitter(String requestId) {
        return activeStreams.get(requestId);
    }

    /**
     * Remove a stream
     */
    public void removeStream(String requestId) {
        activeStreams.remove(requestId);
    }

    /**
     * Check if stream exists
     */
    public boolean hasStream(String requestId) {
        return activeStreams.containsKey(requestId);
    }

    /**
     * Get active stream count
     */
    public int getActiveStreamCount() {
        return activeStreams.size();
    }
}
