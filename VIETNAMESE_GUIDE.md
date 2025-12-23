# Tài Liệu Kiến Trúc DeepApp Main - Hệ Thống Java + C++ gRPC

## 📋 Tổng Quan

Dự án `/root/deepapp/deepapp_main` đã được thiết kế với **kiến trúc module hóa, dễ mở rộng** để giao tiếp giữa Java services và C++ workers thông qua gRPC, tương tự như `/root/bigbox` nhưng được **tổ chức tốt hơn và dễ scale hơn**.

## ✅ Những Gì Đã Được Setup

### 1. **Cấu Trúc Thư Mục**
```
deepapp_main/
├── proto/
│   └── hub.proto                           # Protocol Buffer definition
├── src/main/
│   ├── proto/
│   │   └── hub.proto                       # Proto cho Maven compile
│   ├── java/com/deepapp/vn/io/
│   │   ├── infrastructure/grpc/            # Lớp Infrastructure
│   │   │   ├── BaseGrpcClientService.java  # Base class cho tất cả gRPC clients
│   │   │   └── GrpcClientManager.java      # Quản lý registry của clients
│   │   ├── workers/                        # Lớp Worker Clients
│   │   │   ├── BaseWorkerClient.java       # Base class cho C++ workers
│   │   │   ├── OcrWorkerClient.java        # Client cho OCR worker
│   │   │   └── NerWorkerClient.java        # Client cho NER worker
│   │   └── modules/                        # Lớp Business Logic
│   │       ├── documentprocessing/         # Module xử lý tài liệu
│   │       │   ├── DocumentProcessingService.java
│   │       │   └── DocumentProcessingController.java
│   │       └── dataanalytics/              # Module phân tích dữ liệu
│   │           ├── DataAnalyticsService.java
│   │           └── DataAnalyticsController.java
│   └── resources/
│       └── application.yml                 # Cấu hình tập trung
├── test_document_processing.sh             # Script test
├── test_analytics.sh                       # Script test
├── README_ARCHITECTURE.md                  # Tài liệu chi tiết (English)
└── QUICKSTART.md                          # Hướng dẫn bắt đầu nhanh
```

### 2. **Ba Lớp Kiến Trúc**

#### **Lớp 1: Infrastructure (infrastructure/grpc/)**
- `BaseGrpcClientService`: Class cơ sở xử lý giao tiếp gRPC bidirectional streaming
- `GrpcClientManager`: Quản lý và registry các gRPC clients
- **Chức năng**: Xử lý kết nối, streaming, request/response correlation

#### **Lớp 2: Workers (workers/)**
- `BaseWorkerClient`: Class cơ sở cho tất cả C++ worker clients
- `OcrWorkerClient`: Client riêng cho OCR worker
- `NerWorkerClient`: Client riêng cho NER worker
- **Chức năng**: Wrap các tác vụ cụ thể của từng worker, cung cấp typed methods

#### **Lớp 3: Modules (modules/)**
- `documentprocessing`: Module xử lý tài liệu (OCR + NER)
- `dataanalytics`: Module phân tích dữ liệu
- **Chức năng**: Business logic, kết hợp nhiều workers, expose REST APIs

### 3. **Cấu Hình Tập Trung**

File `application.yml`:
```yaml
workers:
  ocr:
    host: localhost
    port: 50052
    targetId: cpp-ocr-worker
    enabled: true
  
  ner:
    host: localhost
    port: 50053
    targetId: cpp-ner-worker
    enabled: true
```

**Lợi ích**: 
- Dễ dàng thay đổi địa chỉ workers
- Có thể bật/tắt workers
- Không cần sửa code khi thay đổi config

## 🎯 So Sánh Với /root/bigbox

| Aspect | bigbox | deepapp_main |
|--------|--------|--------------|
| **Tổ chức code** | Flat structure | 3-layer architecture |
| **Scalability** | Khó thêm workers | Dễ dàng extend |
| **Configuration** | Hardcoded | Centralized yml |
| **Worker Clients** | Generic | Typed, specific |
| **Modules** | Monolithic | Separated modules |
| **Reusability** | Limited | High |

## 📚 Cách Sử Dụng

### **Thêm Worker Mới**

1. **Thêm config vào `application.yml`**:
```yaml
workers:
  yolo:
    host: localhost
    port: 50054
    targetId: cpp-yolo-worker
    enabled: true
```

2. **Tạo Worker Client**:
```java
@Service
public class YoloWorkerClient extends BaseWorkerClient {
    
    @Value("${workers.yolo.targetId:cpp-yolo-worker}")
    private String yoloTargetId;

    public YoloWorkerClient(
            @Value("${workers.yolo.host:localhost}") String host,
            @Value("${workers.yolo.port:50054}") int port) {
        super("java-yolo-client", host, port);
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

3. **Sử dụng trong Service**:
```java
@Autowired
private YoloWorkerClient yoloWorkerClient;

public CompletableFuture<String> processImage(String path) {
    return yoloWorkerClient.detectObjects(path);
}
```

### **Thêm Module Mới**

1. Tạo package: `modules/yourmodule/`
2. Tạo Service: Business logic
3. Tạo Controller: REST endpoints
4. Inject các worker clients cần thiết

## 🚀 Build & Run

```bash
# Compile project
cd /root/deepapp/deepapp_main
mvn clean compile

# Run application
mvn spring-boot:run

# Build JAR
mvn clean package
java -jar target/deepapp_main-0.0.1-SNAPSHOT.jar
```

## 🧪 Test APIs

### Module Document Processing
```bash
# Test OCR
curl -X POST http://localhost:8080/api/documents/ocr \
  -H "Content-Type: application/json" \
  -d '{"imagePath": "/path/to/image.jpg"}'

# Test NER
curl -X POST http://localhost:8080/api/documents/ner \
  -H "Content-Type: application/json" \
  -d '{"text": "Your text here"}'

# Test Full Processing (OCR + NER)
curl -X POST http://localhost:8080/api/documents/process \
  -H "Content-Type: application/json" \
  -d '{"imagePath": "/path/to/document.jpg"}'
```

### Module Data Analytics
```bash
# Batch process
curl -X POST http://localhost:8080/api/analytics/batch \
  -H "Content-Type: application/json" \
  -d '{"imagePaths": ["/path/1.jpg", "/path/2.jpg"]}'

# Analyze
curl -X POST http://localhost:8080/api/analytics/analyze \
  -H "Content-Type: application/json" \
  -d '{"imagePath": "/path/to/image.jpg"}'
```

Hoặc dùng test scripts:
```bash
./test_document_processing.sh
./test_analytics.sh
```

## 🔑 Key Features

### 1. **Tách Biệt Rõ Ràng**
- Infrastructure không biết về business logic
- Workers không biết về nhau
- Modules độc lập, dễ maintain

### 2. **Dễ Mở Rộng**
- Thêm worker: Chỉ cần tạo 1 class mới
- Thêm module: Tạo package mới
- Không cần sửa code cũ

### 3. **Type Safe**
- Mỗi worker có typed methods
- CompletableFuture cho async operations
- Compile-time safety

### 4. **Configuration-Driven**
- Tất cả config trong application.yml
- Dễ dàng deploy với nhiều môi trường
- Có thể override bằng environment variables

### 5. **Async by Default**
- CompletableFuture API
- Non-blocking operations
- Dễ compose operations

## 📊 Pattern Sử Dụng

### Pattern 1: Sequential Processing
```java
// OCR -> NER
ocrWorkerClient.performOcr(imagePath)
    .thenCompose(text -> nerWorkerClient.extractEntities(text))
    .thenApply(entities -> processResults(entities))
```

### Pattern 2: Parallel Processing
```java
CompletableFuture<String> ocr = ocrWorkerClient.performOcr(path);
CompletableFuture<String> yolo = yoloWorkerClient.detectObjects(path);

CompletableFuture.allOf(ocr, yolo)
    .thenApply(v -> combineResults(ocr.join(), yolo.join()))
```

### Pattern 3: Batch Processing
```java
List<CompletableFuture<BatchResult>> futures = imagePaths.stream()
    .map(path -> ocrWorkerClient.performOcr(path)
        .thenApply(text -> new BatchResult(path, text)))
    .collect(Collectors.toList());

CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
    .thenApply(v -> futures.stream()
        .map(CompletableFuture::join)
        .collect(Collectors.toList()))
```

## 🎓 Best Practices

1. **Một worker = một client class**
2. **Module chỉ import workers cần thiết**
3. **Service xử lý business logic, Controller xử lý HTTP**
4. **Sử dụng CompletableFuture cho async operations**
5. **Config trong application.yml, không hardcode**
6. **Log đầy đủ để debug**
7. **Handle exceptions properly**

## 📈 Roadmap Mở Rộng

### Phase 1: ✅ Done
- Base infrastructure
- Worker abstraction
- Example modules
- Configuration

### Phase 2: Future
- [ ] Health checks cho workers
- [ ] Circuit breaker pattern
- [ ] Metrics & monitoring
- [ ] Service discovery
- [ ] Load balancing cho multiple workers
- [ ] Retry logic
- [ ] Caching layer

## 🔍 Debugging Tips

```bash
# Xem logs chi tiết
mvn spring-boot:run -Dspring-boot.run.arguments=--logging.level.com.deepapp.vn.io=DEBUG

# Check generated proto classes
ls -la target/generated-sources/protobuf/

# Test connectivity
telnet <worker-host> <worker-port>
```

## 📖 Tài Liệu Bổ Sung

- [README_ARCHITECTURE.md](README_ARCHITECTURE.md) - Chi tiết architecture (English)
- [QUICKSTART.md](QUICKSTART.md) - Hướng dẫn bắt đầu nhanh
- [proto/hub.proto](proto/hub.proto) - gRPC contract definition

## 💡 Ví Dụ Thực Tế

### Thêm YOLO Worker (Ví dụ hoàn chỉnh)

**Bước 1**: Config
```yaml
# application.yml
workers:
  yolo:
    host: 72.60.111.138
    port: 50051
    targetId: cpp-yolo-worker
```

**Bước 2**: Worker Client
```java
// workers/YoloWorkerClient.java
@Service
public class YoloWorkerClient extends BaseWorkerClient {
    @Value("${workers.yolo.targetId}")
    private String targetId;

    public YoloWorkerClient(
            @Value("${workers.yolo.host}") String host,
            @Value("${workers.yolo.port}") int port) {
        super("java-yolo-client", host, port);
    }

    @Override
    protected String getWorkerTargetId() {
        return targetId;
    }

    public CompletableFuture<DetectionResult> detectObjects(String imagePath) {
        return executeTask("detect", imagePath)
            .thenApply(json -> parseDetectionResult(json));
    }
}
```

**Bước 3**: Sử dụng trong Module
```java
// modules/objectdetection/ObjectDetectionService.java
@Service
public class ObjectDetectionService {
    @Autowired
    private YoloWorkerClient yoloWorkerClient;

    public CompletableFuture<DetectionResult> detect(String imagePath) {
        return yoloWorkerClient.detectObjects(imagePath);
    }
}
```

**Bước 4**: REST API
```java
// modules/objectdetection/ObjectDetectionController.java
@RestController
@RequestMapping("/api/detection")
public class ObjectDetectionController {
    @Autowired
    private ObjectDetectionService service;

    @PostMapping("/detect")
    public ResponseEntity<DetectionResult> detect(@RequestBody Request req) {
        return ResponseEntity.ok(service.detect(req.getImagePath()).get());
    }
}
```

**Done!** Giờ có thể gọi: `POST /api/detection/detect`

## 🎉 Kết Luận

Architecture này được thiết kế để:
- ✅ **Scale dễ dàng**: Thêm workers/modules không ảnh hưởng code cũ
- ✅ **Maintain dễ**: Code tổ chức rõ ràng, tách biệt
- ✅ **Production-ready**: Config external, async by default
- ✅ **Team-friendly**: Nhiều người có thể làm song song

Bạn có thể bắt đầu với 2 workers (OCR, NER) và dần thêm nhiều workers khác (YOLO, LLM, v.v.) mà không cần refactor!
