# DeepApp Main - Scalable Java + C++ gRPC Architecture

## 🏗️ Architecture Overview

This project demonstrates a **scalable, modular architecture** for Java services communicating with C++ workers via gRPC. The design emphasizes clear separation of concerns and easy extensibility.

## 📁 Project Structure

```
deepapp_main/
├── proto/                              # Protocol Buffer definitions
│   └── hub.proto                       # Shared gRPC contract
├── src/main/java/com/deepapp/vn/io/
│   ├── infrastructure/                 # Core infrastructure layer
│   │   └── grpc/
│   │       ├── BaseGrpcClientService.java    # Base gRPC client
│   │       └── GrpcClientManager.java        # Client registry
│   ├── workers/                        # C++ Worker client layer
│   │   ├── BaseWorkerClient.java      # Abstract worker client
│   │   ├── OcrWorkerClient.java       # OCR worker client
│   │   └── NerWorkerClient.java       # NER worker client
│   └── modules/                        # Business logic modules
│       ├── documentprocessing/
│       │   ├── DocumentProcessingService.java
│       │   └── DocumentProcessingController.java
│       └── dataanalytics/
│           ├── DataAnalyticsService.java
│           └── DataAnalyticsController.java
└── src/main/resources/
    └── application.yml                 # Configuration
```

## 🎯 Key Design Principles

### 1. **Layered Architecture**
- **Infrastructure Layer**: Generic gRPC communication logic
- **Worker Layer**: Worker-specific client implementations
- **Module Layer**: Business logic using workers

### 2. **Separation of Concerns**
- Each C++ worker has its own dedicated Java client
- Each business module focuses on specific functionality
- Configuration is externalized in `application.yml`

### 3. **Easy Scalability**
- **Adding a new C++ worker**: Create a new worker client extending `BaseWorkerClient`
- **Adding a new Java module**: Create new service + controller in `modules/`
- **No modification needed** to existing code

## 🚀 How to Add New Components

### Adding a New C++ Worker Client

1. **Add configuration** to `application.yml`:
```yaml
workers:
  yolo:
    host: localhost
    port: 50054
    targetId: cpp-yolo-worker
    enabled: true
```

2. **Create worker client** in `workers/`:
```java
@Service
public class YoloWorkerClient extends BaseWorkerClient {
    
    @Value("${workers.yolo.host:localhost}")
    private String yoloHost;
    
    @Value("${workers.yolo.port:50054}")
    private int yoloPort;
    
    @Value("${workers.yolo.targetId:cpp-yolo-worker}")
    private String yoloTargetId;
    
    public YoloWorkerClient() {
        super("java-yolo-client", "localhost", 50054);
    }
    
    @Override
    protected String getWorkerTargetId() {
        return yoloTargetId;
    }
    
    public CompletableFuture<String> detectObjects(String imagePath) {
        return executeTask("detect", imagePath);
    }
}
```

### Adding a New Java Module

1. **Create module package**: `modules/yourmodule/`
2. **Create service class**: Inject and use worker clients
3. **Create controller**: Expose REST endpoints

Example structure:
```
modules/
  └── yourmodule/
      ├── YourModuleService.java     # Business logic
      └── YourModuleController.java  # REST API
```

## 📡 API Examples

### Document Processing Module

**Process Document (OCR + NER)**
```bash
curl -X POST http://localhost:8080/api/documents/process \
  -H "Content-Type: application/json" \
  -d '{"imagePath": "/path/to/image.jpg"}'
```

**Extract Text Only (OCR)**
```bash
curl -X POST http://localhost:8080/api/documents/ocr \
  -H "Content-Type: application/json" \
  -d '{"imagePath": "/path/to/image.jpg"}'
```

**Extract Entities (NER)**
```bash
curl -X POST http://localhost:8080/api/documents/ner \
  -H "Content-Type: application/json" \
  -d '{"text": "Sample text for NER"}'
```

### Data Analytics Module

**Batch Process Images**
```bash
curl -X POST http://localhost:8080/api/analytics/batch \
  -H "Content-Type: application/json" \
  -d '{"imagePaths": ["/path/1.jpg", "/path/2.jpg"]}'
```

**Analyze Image**
```bash
curl -X POST http://localhost:8080/api/analytics/analyze \
  -H "Content-Type: application/json" \
  -d '{"imagePath": "/path/to/image.jpg"}'
```

## 🔧 Building and Running

### Compile Protocol Buffers
```bash
cd /root/deepapp/deepapp_main
mvn clean compile
```

### Run the Application
```bash
mvn spring-boot:run
```

### Build JAR
```bash
mvn clean package
java -jar target/deepapp_main-0.0.1-SNAPSHOT.jar
```

## ⚙️ Configuration

### Worker Configuration (`application.yml`)
```yaml
workers:
  ocr:
    host: localhost        # Worker server host
    port: 50052           # Worker server port
    targetId: cpp-ocr-worker  # Worker ID for routing
    enabled: true         # Enable/disable worker
```

### Server Configuration
```yaml
server:
  port: 8080              # Java application port
```

## 🔑 Key Classes

### Infrastructure Layer

**BaseGrpcClientService**
- Abstract base for all gRPC clients
- Handles bidirectional streaming
- Manages request/response correlation
- Provides async and sync communication

**GrpcClientManager**
- Central registry for all gRPC clients
- Manages client lifecycle
- Provides client lookup

### Worker Layer

**BaseWorkerClient**
- Extends `BaseGrpcClientService`
- Provides worker-specific abstractions
- Implements `executeTask()` for common operations

**Concrete Worker Clients**
- `OcrWorkerClient`: OCR operations
- `NerWorkerClient`: Named Entity Recognition
- Each client wraps worker-specific operations

### Module Layer

**Services**: Business logic combining multiple workers
**Controllers**: REST API endpoints

## 🆚 Comparison with /root/bigbox

### Similarities
- Both use gRPC with protobuf
- Both support bidirectional streaming
- Both handle Java ↔ C++ communication

### Improvements in deepapp_main
1. **Better separation**: Infrastructure, Workers, Modules layers
2. **More scalable**: Easy to add workers and modules independently
3. **Cleaner config**: Centralized in `application.yml`
4. **Type safety**: Dedicated worker clients with typed methods
5. **Reusability**: Base classes provide common functionality

## 📊 Example Use Cases

### 1. Document Processing Pipeline
```
Image → OCR Worker → Text → NER Worker → Entities
```

### 2. Batch Processing
```
Multiple Images → OCR Worker (parallel) → Results
```

### 3. Multi-Worker Composition
```
Image → OCR + YOLO (parallel) → Combined Result
```

## 🧪 Testing

Create test scripts:
```bash
# Test document processing
./test_document_processing.sh

# Test analytics
./test_analytics.sh
```

## 📝 Notes

- All worker clients use **async communication by default**
- Use `CompletableFuture` for composing operations
- Configure timeouts in worker client methods
- Workers can be on different servers (configurable via `application.yml`)

## 🔮 Future Enhancements

1. **Health checks**: Add worker health monitoring
2. **Circuit breaker**: Implement resilience patterns
3. **Load balancing**: Support multiple worker instances
4. **Metrics**: Add Prometheus/Grafana integration
5. **Service discovery**: Add Consul/Eureka support

## 📚 Related Projects

- `/root/bigbox`: Original implementation
- `/root/new_flow_project`: Flow-based architecture
- `/root/workspace/deepapp_ocr_vietnam_cpp`: C++ OCR worker
