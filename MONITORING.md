# Monitoring & Observability Stack

## Overview

Hệ thống monitoring đồng bộ cho cả Java và C++ với:
- **Metrics**: Prometheus + Micrometer
- **Logs**: Loki + Promtail với JSON format
- **Visualization**: Grafana dashboards
- **Tracing**: Distributed tracing với trace ID

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Grafana (Port 3000)                   │
│              Dashboards & Visualization                  │
└───────────────┬────────────────┬────────────────────────┘
                │                │
                ▼                ▼
    ┌──────────────────┐  ┌──────────────────┐
    │   Prometheus     │  │      Loki        │
    │   (Port 9090)    │  │   (Port 3100)    │
    │   Metrics DB     │  │    Logs DB       │
    └────────┬─────────┘  └────────┬─────────┘
             │                     │
             │                     │
    ┌────────▼─────────┐  ┌────────▼─────────┐
    │   /actuator/     │  │    Promtail      │
    │   prometheus     │  │  Log Collector   │
    └────────┬─────────┘  └────────┬─────────┘
             │                     │
             │                     │
    ┌────────▼─────────────────────▼─────────┐
    │         Deepapp Application             │
    │  ┌─────────────┐   ┌─────────────┐    │
    │  │  Java API   │   │ C++ Worker  │    │
    │  │  (Metrics)  │   │  (Metrics)  │    │
    │  │  (JSON Log) │   │ (JSON Log)  │    │
    │  └─────────────┘   └─────────────┘    │
    └─────────────────────────────────────────┘
```

## Quick Start

### 1. Start Monitoring Stack

```bash
cd /root/deepapp/deepapp_main

# Start all services
docker-compose -f docker-compose-monitoring.yml up -d

# Check status
docker-compose -f docker-compose-monitoring.yml ps

# View logs
docker-compose -f docker-compose-monitoring.yml logs -f
```

### 2. Access Services

- **Grafana**: http://localhost:3000 (admin/admin123)
- **Prometheus**: http://localhost:9090
- **Loki**: http://localhost:3100
- **Application**: http://localhost:8080

### 3. View Metrics

```bash
# Java metrics endpoint
curl http://localhost:8080/actuator/prometheus

# Health check
curl http://localhost:8080/actuator/health

# All actuator endpoints
curl http://localhost:8080/actuator
```

## Metrics

### Java Metrics (Micrometer)

**HTTP Metrics**:
- `http_requests_total` - Total HTTP requests
- `http_request_duration_ms` - Request duration
- `http_server_requests` - Spring Boot HTTP metrics

**Worker Metrics**:
- `worker_requests_total{worker_id,event_type,language="java"}` - Total requests
- `worker_requests_success{worker_id,event_type,language="java"}` - Success count
- `worker_requests_error{worker_id,event_type,error_type,language="java"}` - Error count
- `worker_request_duration_ms{worker_id,event_type,language="java"}` - Duration

**JVM Metrics**:
- `jvm_memory_used_bytes` - Memory usage
- `jvm_threads_live` - Thread count
- `jvm_gc_pause_seconds` - GC pause time

**Usage in Code**:
```java
@Autowired
private WorkerMetrics workerMetrics;

// Record request
workerMetrics.recordRequest("AAA0_0101_W", "process");

// Record success
long duration = System.currentTimeMillis() - startTime;
workerMetrics.recordSuccess("AAA0_0101_W", "process", duration);

// Record error
workerMetrics.recordError("AAA0_0101_W", "process", "timeout", duration);
```

### C++ Metrics

**Worker Metrics**:
- `worker_requests_total{worker_id,event_type,language="cpp"}` - Total requests
- `worker_requests_success{worker_id,event_type,language="cpp"}` - Success count
- `worker_requests_error{worker_id,event_type,error_type,language="cpp"}` - Error count
- `worker_request_duration_ms{worker_id,event_type,language="cpp"}` - Duration

**Model Metrics**:
- `model_load_duration_ms{model_name}` - Model load time
- `model_loads_total{model_name}` - Model load count
- `model_memory_bytes{model_name}` - Model memory usage

**Usage in Code**:
```cpp
#include "lib/monitoring/Metrics.hpp"

using deepapp::monitoring::WorkerMetrics;
using deepapp::monitoring::Timer;

// Record request
WorkerMetrics::recordRequest("AAA0_0101_W", "process");

// Measure duration
{
    Timer timer("worker_request_duration_ms", {
        {"worker_id", "AAA0_0101_W"},
        {"event_type", "process"}
    });
    
    // Process request
    // ...
    
} // Timer automatically records duration

// Record success/error
WorkerMetrics::recordSuccess("AAA0_0101_W", "process");
// or
WorkerMetrics::recordError("AAA0_0101_W", "process", "timeout");
```

## Logging

### JSON Log Format

**Java Logs** (logback-spring.xml):
```json
{
  "timestamp": "2025-12-24T10:30:45.123+0700",
  "level": "INFO",
  "service": "deepapp-main",
  "language": "java",
  "message": "HTTP Request: GET /AA/A0/AAA0_0101 from 172.18.0.1",
  "logger_name": "com.deepapp.vn.io.infrastructure.monitoring.MonitoringInterceptor",
  "thread_name": "http-nio-8080-exec-1",
  "context": {
    "traceId": "550e8400-e29b-41d4-a716-446655440000",
    "userId": "user123"
  }
}
```

**C++ Logs** (Logger.hpp):
```json
{
  "timestamp": "2025-12-24T10:30:45.456+0700",
  "level": "INFO",
  "service": "deepapp-worker",
  "language": "cpp",
  "message": "Processing OCR task",
  "context": {
    "worker_id": "AAA0_0101_W",
    "event_type": "process"
  }
}
```

### Usage

**Java**:
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

private static final Logger log = LoggerFactory.getLogger(MyClass.class);

// Simple log
log.info("Processing request");

// With context
MDC.put("userId", "user123");
MDC.put("workerId", "AAA0_0101_W");
log.info("Request processed successfully");
MDC.clear();
```

**C++**:
```cpp
#include "lib/monitoring/Logger.hpp"

using deepapp::monitoring::Logger;

auto& logger = Logger::getInstance();

// Simple log
logger.info("Processing request");

// With context
logger.info("Request processed", {
    {"worker_id", "AAA0_0101_W"},
    {"duration_ms", "250"}
});

// Error log
logger.error("Request failed", {
    {"error", "timeout"},
    {"duration_ms", "5000"}
});
```

## Grafana Dashboards

### Import Dashboards

1. Access Grafana: http://localhost:3000
2. Login: admin/admin123
3. Go to: Dashboards → Import
4. Import JSON files from `monitoring/grafana/dashboards/`

### Available Dashboards

**1. Application Overview**:
- Request rate (req/s)
- Error rate (%)
- Response time (p50, p95, p99)
- Active requests

**2. Worker Metrics**:
- Worker request rate by worker_id
- Worker success/error rate
- Worker duration by event_type
- Model load time

**3. System Metrics**:
- CPU usage
- Memory usage
- JVM heap usage
- Thread count
- GC activity

**4. Logs Explorer**:
- Real-time log streaming
- Log search and filtering
- Error rate trends

## Queries

### Prometheus Queries (PromQL)

```promql
# Request rate (per second)
rate(http_requests_total[5m])

# Error rate (percentage)
(rate(worker_requests_error[5m]) / rate(worker_requests_total[5m])) * 100

# Average response time
rate(worker_request_duration_ms_sum[5m]) / rate(worker_request_duration_ms_count[5m])

# P95 response time
histogram_quantile(0.95, rate(http_request_duration_ms_bucket[5m]))

# Top 5 slowest workers
topk(5, avg by(worker_id) (worker_request_duration_ms))

# Memory usage by model
sum by(model_name) (model_memory_bytes)
```

### Loki Queries (LogQL)

```logql
# All logs from Java service
{service="deepapp-main", language="java"}

# Error logs only
{service="deepapp-main"} |= "ERROR"

# Logs for specific worker
{service="deepapp-worker", worker_id="AAA0_0101_W"}

# Search in message
{service="deepapp-main"} |~ "HTTP Request.*AAA0_0101"

# Rate of errors (per second)
rate({service="deepapp-main"} |= "ERROR" [5m])

# Extract duration from logs
{service="deepapp-main"} | json | duration > 1000

# Top 10 error messages
topk(10, sum by (message) (count_over_time({level="ERROR"}[1h])))
```

## Alerting

### Prometheus Alerts

Create `monitoring/alerts.yml`:

```yaml
groups:
  - name: deepapp_alerts
    interval: 30s
    rules:
      # High error rate
      - alert: HighErrorRate
        expr: (rate(worker_requests_error[5m]) / rate(worker_requests_total[5m])) > 0.05
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value | humanizePercentage }}"
      
      # Slow requests
      - alert: SlowRequests
        expr: histogram_quantile(0.95, rate(http_request_duration_ms_bucket[5m])) > 2000
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "95th percentile latency is high"
          description: "P95 latency is {{ $value }}ms"
      
      # High memory usage
      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes / jvm_memory_max_bytes > 0.9
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "JVM memory usage is high"
          description: "Memory usage is {{ $value | humanizePercentage }}"
```

## Performance Tips

### 1. Reduce Metric Cardinality

```java
// Bad - high cardinality
metrics.tag("user_id", userId);  // Millions of unique values

// Good - low cardinality
metrics.tag("user_type", userType);  // Few unique values
```

### 2. Sample High-Frequency Metrics

```java
// Sample 10% of requests
if (Math.random() < 0.1) {
    metrics.record();
}
```

### 3. Batch Log Writes

Use async appenders in logback-spring.xml (already configured)

### 4. Set Retention Policies

```yaml
# Prometheus
storage.tsdb.retention.time: 30d

# Loki
retention_period: 720h  # 30 days
```

## Troubleshooting

### Metrics Not Appearing

```bash
# Check actuator endpoint
curl http://localhost:8080/actuator/prometheus

# Check Prometheus targets
http://localhost:9090/targets

# Check Prometheus logs
docker logs deepapp-prometheus
```

### Logs Not Appearing

```bash
# Check log files exist
ls -la /root/deepapp/deepapp_main/logs/

# Check Promtail status
docker logs deepapp-promtail

# Check Loki status
docker logs deepapp-loki

# Query Loki directly
curl -G -s "http://localhost:3100/loki/api/v1/query" \
  --data-urlencode 'query={service="deepapp-main"}'
```

### High Resource Usage

```bash
# Limit Prometheus memory
docker run ... -e PROMETHEUS_MEMORY=512m

# Reduce scrape interval
scrape_interval: 30s  # Instead of 15s

# Reduce log retention
retention_period: 168h  # 7 days
```

## Production Deployment

### 1. Persistent Volumes

```yaml
volumes:
  prometheus-data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: /data/prometheus
  
  grafana-data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: /data/grafana
```

### 2. External Storage

Use S3/GCS for long-term storage:

```yaml
# Prometheus remote write
remote_write:
  - url: "https://your-tsdb.com/api/v1/push"

# Loki S3 storage
storage_config:
  aws:
    s3: s3://your-region/your-bucket
```

### 3. High Availability

Run multiple replicas of each service with load balancing

### 4. Security

```yaml
# Enable authentication
GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_PASSWORD}
GF_AUTH_ANONYMOUS_ENABLED: false

# Use TLS
GF_SERVER_PROTOCOL: https
GF_SERVER_CERT_FILE: /etc/grafana/cert.pem
GF_SERVER_CERT_KEY: /etc/grafana/key.pem
```

## Next Steps

1. ✅ Setup monitoring stack
2. ✅ Configure metrics collection
3. ✅ Setup log aggregation
4. 🔄 Create custom Grafana dashboards
5. ⏳ Configure alerting rules
6. ⏳ Setup notification channels (Slack, email)
7. ⏳ Add distributed tracing (Jaeger/Zipkin)
8. ⏳ Implement SLO/SLI dashboards

## Resources

- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Loki Documentation](https://grafana.com/docs/loki/)
- [Micrometer Documentation](https://micrometer.io/docs/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
