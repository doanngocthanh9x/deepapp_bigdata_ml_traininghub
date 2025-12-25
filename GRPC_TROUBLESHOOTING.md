# gRPC Connection Issues & Fixes

## Problem: "too_many_pings" Error

### Symptom
```
RESOURCE_EXHAUSTED: Connection closed after GOAWAY. 
HTTP/2 error code: ENHANCE_YOUR_CALM (Bandwidth exhausted), 
debug data: too_many_pings
```

### Root Cause

Java gRPC client gửi **keepalive pings** quá nhanh đến gRPC hub server (`72.60.111.138:50051`), server từ chối kết nối với error "too_many_pings".

### Architecture

```
┌─────────────────┐         ┌──────────────────────┐         ┌─────────────────┐
│  Java API       │◄───────►│   gRPC Hub Server    │◄───────►│  C++ Worker     │
│  (Client)       │         │  72.60.111.138:50051 │         │  (Client)       │
│                 │         │                      │         │                 │
│ keepAliveTime:  │         │ Min ping interval:   │         │ No keepalive    │
│ 2 mins → 10 mins│         │ enforcement          │         │ (default)       │
└─────────────────┘         └──────────────────────┘         └─────────────────┘
        ▲                            │
        │ too_many_pings!            │
        └────────────────────────────┘
```

### Fix Applied

**File**: `BaseGrpcClientService.java`

**Before**:
```java
.keepAliveTime(2, TimeUnit.MINUTES)  // Too aggressive!
.keepAliveTimeout(20, TimeUnit.SECONDS)
.idleTimeout(30, TimeUnit.MINUTES)
```

**After**:
```java
.keepAliveTime(10, TimeUnit.MINUTES)  // Relaxed to 10 minutes
.keepAliveTimeout(30, TimeUnit.SECONDS)  // Increased timeout
.idleTimeout(Long.MAX_VALUE, TimeUnit.DAYS)  // Disabled idle timeout
```

### Why This Fixes The Issue

1. **keepAliveTime: 10 minutes** - Pings sent less frequently
2. **keepAliveWithoutCalls: false** - Only ping during active calls
3. **Disabled idleTimeout** - Connection stays alive indefinitely
4. **Auto-reconnect** - Already implemented in BaseGrpcClientService

### Verification

```bash
# Watch logs for successful connection
docker logs -f deepapp-main | grep "gRPC\|keepalive\|GOAWAY"

# Should see:
# ✓ Client 'java-cpp-client' reconnected successfully
# ✓ No more "too_many_pings" errors
# ✓ Connection remains stable
```

## Other gRPC Connection Issues

### Issue 1: Connection Timeout

**Symptom**: `DEADLINE_EXCEEDED` after 60 seconds

**Fix**:
```java
// Increase call timeout
EventChunk response = stub
    .withDeadlineAfter(120, TimeUnit.SECONDS)  // 2 minutes
    .sendEvent(request);
```

### Issue 2: Max Message Size Exceeded

**Symptom**: `RESOURCE_EXHAUSTED: gRPC message exceeds maximum size`

**Fix**:
```java
// Already configured in BaseGrpcClientService
.maxInboundMessageSize(100 * 1024 * 1024)  // 100MB
.maxInboundMetadataSize(8 * 1024)  // 8KB headers
```

### Issue 3: SSL/TLS Handshake Failed

**Symptom**: `UNAVAILABLE: io exception`

**Fix**:
```java
// For development - use plaintext
.usePlaintext()

// For production - use TLS
.useTransportSecurity()
.overrideAuthority("your-domain.com")
```

### Issue 4: Channel Shutdown During Request

**Symptom**: `CANCELLED: call already cancelled`

**Fix**: Already handled with `@PreDestroy` in services

## Best Practices

### 1. Client Configuration

```java
ManagedChannel channel = ManagedChannelBuilder
    .forAddress(host, port)
    .usePlaintext()
    
    // Keepalive settings (production)
    .keepAliveTime(10, TimeUnit.MINUTES)
    .keepAliveTimeout(30, TimeUnit.SECONDS)
    .keepAliveWithoutCalls(false)  // Important!
    
    // Idle/connection settings
    .idleTimeout(Long.MAX_VALUE, TimeUnit.DAYS)
    .maxRetryAttempts(3)
    .retryBufferSize(16 * 1024 * 1024)
    
    // Message size limits
    .maxInboundMessageSize(100 * 1024 * 1024)
    
    .build();
```

### 2. Server Configuration (if you control the server)

```java
Server server = NettyServerBuilder
    .forPort(50051)
    
    // Permit keepalive from clients
    .permitKeepAliveTime(5, TimeUnit.MINUTES)
    .permitKeepAliveWithoutCalls(false)
    
    // Max connection settings
    .maxConnectionIdle(Long.MAX_VALUE, TimeUnit.DAYS)
    .maxConnectionAge(Long.MAX_VALUE, TimeUnit.DAYS)
    .maxConnectionAgeGrace(Long.MAX_VALUE, TimeUnit.DAYS)
    
    // Message size
    .maxInboundMessageSize(100 * 1024 * 1024)
    
    .build();
```

### 3. Reconnection Logic

Already implemented in `BaseGrpcClientService`:
- Auto-reconnect on disconnect
- Exponential backoff (5s initial delay)
- Max 5 reconnection attempts
- Graceful shutdown handling

### 4. Monitoring

```java
// Log connection events
logger.info("gRPC stream established");
logger.warn("gRPC stream error: {}", status.getDescription());
logger.info("Reconnecting... (attempt {}/{})", attempt, maxAttempts);
```

## Testing

### Test Keepalive Settings

```bash
# 1. Start application
cd /root/deepapp/deepapp_main
mvn clean package
java -jar target/*.jar

# 2. Monitor connection
docker logs -f deepapp-main | grep "keepalive\|GOAWAY"

# 3. Wait 15 minutes - should stay connected

# 4. Send test request
curl -X POST http://localhost:8080/AA/A0/AAA0_0101 \
  -H "Content-Type: application/json" \
  -d '{"imagePath":"/test.jpg","language":"vi"}'

# 5. Check logs - no "too_many_pings" errors
```

### Stress Test

```bash
# Send 100 requests with 1 second delay
for i in {1..100}; do
  curl -s -X POST http://localhost:8080/AA/A0/AAA0_0101 \
    -d '{"imagePath":"/test.jpg"}' &
  sleep 1
done

# Should complete without connection errors
```

## Troubleshooting

### Still Getting "too_many_pings"?

1. Check hub server configuration:
   ```bash
   # Server must permit client keepalive
   permitKeepAliveTime: >= 5 minutes
   permitKeepAliveWithoutCalls: false
   ```

2. Verify Java client settings:
   ```bash
   # Grep for keepalive in logs
   grep -i "keepalive" /path/to/logs
   ```

3. Increase keepalive time further:
   ```java
   .keepAliveTime(30, TimeUnit.MINUTES)  // Very relaxed
   ```

### Connection Drops After Idle

**Current Fix**: `idleTimeout` disabled

**Alternative**: Enable with longer timeout
```java
.idleTimeout(1, TimeUnit.HOURS)  // Close after 1 hour idle
```

### High Latency

**Check**:
- Network between client and server
- Server load
- Message size

**Fix**:
```java
// Enable compression
.compressor("gzip")
.decompressor("gzip")

// Use faster serialization
.usePlaintext()  // vs TLS overhead
```

## Related Files

- `BaseGrpcClientService.java` - Client configuration
- `CppWorkerManager.java` - C++ worker management
- `application-docker.yml` - gRPC settings
- `MONITORING.md` - Connection metrics

## References

- [gRPC Keepalive](https://grpc.io/docs/guides/keepalive/)
- [gRPC Error Codes](https://grpc.github.io/grpc/core/md_doc_statuscodes.html)
- [Java gRPC API](https://grpc.io/docs/languages/java/basics/)
