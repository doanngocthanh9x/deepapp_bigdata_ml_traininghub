package com.deepapp.vn.io.infrastructure.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * HTTP Request Interceptor for Metrics and Logging
 */
@Component
public class MonitoringInterceptor implements HandlerInterceptor {
    
    private static final Logger log = LoggerFactory.getLogger(MonitoringInterceptor.class);
    private static final String REQUEST_START_TIME = "requestStartTime";
    private static final String TRACE_ID = "traceId";
    
    private final MeterRegistry meterRegistry;
    private final Counter requestCounter;
    private final Timer requestTimer;
    
    public MonitoringInterceptor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize metrics
        this.requestCounter = Counter.builder("http_requests_total")
                .description("Total HTTP requests")
                .register(meterRegistry);
        
        this.requestTimer = Timer.builder("http_request_duration_seconds")
                .description("HTTP request duration")
                .register(meterRegistry);
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) {
        // Generate trace ID
        String traceId = UUID.randomUUID().toString();
        MDC.put(TRACE_ID, traceId);
        
        // Store start time
        request.setAttribute(REQUEST_START_TIME, System.currentTimeMillis());
        
        // Add trace ID to response header
        response.setHeader("X-Trace-Id", traceId);
        
        // Log request
        log.info("HTTP Request: {} {} from {}",
                request.getMethod(),
                request.getRequestURI(),
                request.getRemoteAddr());
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request,
                               HttpServletResponse response,
                               Object handler,
                               Exception ex) {
        try {
            // Calculate duration
            Long startTime = (Long) request.getAttribute(REQUEST_START_TIME);
            if (startTime != null) {
                long duration = System.currentTimeMillis() - startTime;
                
                // Record metrics
                String method = request.getMethod();
                String uri = request.getRequestURI();
                int status = response.getStatus();
                
                requestCounter.increment();
                
                Timer.builder("http_request_duration_ms")
                        .tag("method", method)
                        .tag("uri", sanitizeUri(uri))
                        .tag("status", String.valueOf(status))
                        .register(meterRegistry)
                        .record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
                
                // Log response
                if (ex != null) {
                    log.error("HTTP Response: {} {} - {} ({}ms) - Error: {}",
                            method, uri, status, duration, ex.getMessage());
                } else {
                    log.info("HTTP Response: {} {} - {} ({}ms)",
                            method, uri, status, duration);
                }
            }
        } finally {
            // Clean up MDC
            MDC.remove(TRACE_ID);
        }
    }
    
    private String sanitizeUri(String uri) {
        // Remove IDs and sensitive data from URI for better cardinality
        return uri.replaceAll("/\\d+", "/{id}")
                  .replaceAll("/[a-f0-9-]{36}", "/{uuid}");
    }
}
