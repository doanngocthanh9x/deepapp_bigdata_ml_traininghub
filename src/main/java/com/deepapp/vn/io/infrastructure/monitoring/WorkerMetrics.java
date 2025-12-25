package com.deepapp.vn.io.infrastructure.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * gRPC Worker Metrics
 * 
 * Track metrics for C++ worker interactions
 */
@Component
public class WorkerMetrics {
    
    private static final Logger log = LoggerFactory.getLogger(WorkerMetrics.class);
    
    private final MeterRegistry meterRegistry;
    
    public WorkerMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    /**
     * Record gRPC request
     */
    public void recordRequest(String workerId, String eventType) {
        Counter.builder("worker_requests_total")
                .tag("worker_id", workerId)
                .tag("event_type", eventType)
                .tag("language", "java")
                .description("Total worker requests")
                .register(meterRegistry)
                .increment();
        
        log.debug("Worker request: worker={}, event={}", workerId, eventType);
    }
    
    /**
     * Record gRPC success
     */
    public void recordSuccess(String workerId, String eventType, long durationMs) {
        Counter.builder("worker_requests_success")
                .tag("worker_id", workerId)
                .tag("event_type", eventType)
                .tag("language", "java")
                .description("Successful worker requests")
                .register(meterRegistry)
                .increment();
        
        Timer.builder("worker_request_duration_ms")
                .tag("worker_id", workerId)
                .tag("event_type", eventType)
                .tag("status", "success")
                .tag("language", "java")
                .description("Worker request duration")
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
        
        log.info("Worker success: worker={}, event={}, duration={}ms", 
                workerId, eventType, durationMs);
    }
    
    /**
     * Record gRPC error
     */
    public void recordError(String workerId, String eventType, String errorType, long durationMs) {
        Counter.builder("worker_requests_error")
                .tag("worker_id", workerId)
                .tag("event_type", eventType)
                .tag("error_type", errorType)
                .tag("language", "java")
                .description("Failed worker requests")
                .register(meterRegistry)
                .increment();
        
        Timer.builder("worker_request_duration_ms")
                .tag("worker_id", workerId)
                .tag("event_type", eventType)
                .tag("status", "error")
                .tag("language", "java")
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
        
        log.error("Worker error: worker={}, event={}, error={}, duration={}ms",
                workerId, eventType, errorType, durationMs);
    }
    
    /**
     * Record active requests gauge
     */
    public void setActiveRequests(String workerId, int count) {
        meterRegistry.gauge("worker_active_requests",
                java.util.List.of(
                        io.micrometer.core.instrument.Tag.of("worker_id", workerId),
                        io.micrometer.core.instrument.Tag.of("language", "java")
                ),
                count);
    }
}
