# 🚀 Quick Start Guide

## Prerequisites
- Java 17+
- Maven 3.6+
- Running gRPC hub server (Go server at configured address)
- C++ workers running (OCR, NER, etc.)

## Step 1: Build the Project

```bash
cd /root/deepapp/deepapp_main

# Compile protobuf and build project
mvn clean compile

# Or build JAR
mvn clean package
```

## Step 2: Configure Workers

Edit `src/main/resources/application.yml`:

```yaml
workers:
  ocr:
    host: 72.60.111.138  # Your gRPC hub server
    port: 50051
    targetId: cpp-ocr-worker
    enabled: true
  
  ner:
    host: 72.60.111.138
    port: 50051
    targetId: cpp-ner-worker
    enabled: true
```

## Step 3: Run the Application

```bash
# Using Maven
mvn spring-boot:run

# Or using JAR
java -jar target/deepapp_main-0.0.1-SNAPSHOT.jar
```

The application will start on port 8080 (configurable in `application.yml`).

## Step 4: Test the APIs

### Check Health
```bash
curl http://localhost:8080/api/documents/health
curl http://localhost:8080/api/analytics/health
```

### Test Document Processing
```bash
# Run test script
./test_document_processing.sh

# Or manual test
curl -X POST http://localhost:8080/api/documents/ocr \
  -H "Content-Type: application/json" \
  -d '{"imagePath": "/path/to/image.jpg"}'
```

## 📝 Adding Your First Custom Worker

### 1. Add Configuration
Edit `application.yml`:
```yaml
workers:
  myworker:
    host: localhost
    port: 50054
    targetId: cpp-my-worker
    enabled: true
```

### 2. Create Worker Client
Create `src/main/java/com/deepapp/vn/io/workers/MyWorkerClient.java`:

```java
package com.deepapp.vn.io.workers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MyWorkerClient extends BaseWorkerClient {

    private static final Logger logger = LoggerFactory.getLogger(MyWorkerClient.class);

    @Value("${workers.myworker.targetId:cpp-my-worker}")
    private String workerTargetId;

    public MyWorkerClient(
            @Value("${workers.myworker.host:localhost}") String host,
            @Value("${workers.myworker.port:50054}") int port) {
        super("java-myworker-client", host, port);
    }

    @Override
    protected String getWorkerTargetId() {
        return workerTargetId;
    }

    // Add your custom methods
    public java.util.concurrent.CompletableFuture<String> doSomething(String data) {
        return executeTask("my-task-type", data);
    }
}
```

### 3. Use in Service
```java
@Autowired
private MyWorkerClient myWorkerClient;

public CompletableFuture<String> processData(String data) {
    return myWorkerClient.doSomething(data);
}
```

## 📂 Project Structure Summary

```
infrastructure/grpc/     → Base gRPC communication
workers/                 → Worker client implementations
modules/                 → Business logic modules
  ├── documentprocessing/  → Module 1
  └── dataanalytics/       → Module 2
```

## 🔍 Troubleshooting

### Proto compilation fails
```bash
# Make sure proto file exists
ls -la src/main/proto/hub.proto

# Clean and recompile
mvn clean compile
```

### Cannot connect to worker
- Check worker is running
- Verify host/port in `application.yml`
- Check targetId matches worker's ID
- Review logs for connection errors

### Worker not responding
- Check worker logs
- Verify gRPC hub server is running
- Ensure targetId routing is correct

## 📊 Architecture Benefits

✅ **Separation**: Infrastructure | Workers | Modules  
✅ **Scalability**: Add workers/modules without touching existing code  
✅ **Configuration**: All settings in `application.yml`  
✅ **Type Safety**: Dedicated clients with typed methods  
✅ **Async by Default**: CompletableFuture-based APIs  

## 🎯 Next Steps

1. ✅ Build and run the project
2. ✅ Test existing endpoints
3. ✅ Add your first custom worker
4. ✅ Create your first custom module
5. 📈 Scale: Add more workers and modules as needed!

For detailed architecture documentation, see [README_ARCHITECTURE.md](README_ARCHITECTURE.md).
