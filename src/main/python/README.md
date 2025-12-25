# Python Workers Implementation

This directory contains Python workers that mirror the C++ worker architecture, providing an alternative implementation using Python instead of C++.

## Architecture Overview

The Python workers follow the same architecture as C++ workers:

```
Python Workers
├── infrastructure/           # Core infrastructure
│   ├── BaseWorker.py        # Base worker class
│   ├── WorkerRegistry.py    # Worker registration system
│   └── GrpcWorkerClient.py  # gRPC communication client
├── vn/io/                   # Worker implementations
│   ├── AA/A0/              # AA workers
│   │   ├── AAA0_0100/      # Example worker
│   │   ├── AAA0_0101/      # VietOCR worker
│   │   └── AAA0_0200/      # YOLO worker
│   └── ZZ/A0/              # ZZ workers
│       └── ZZA0_0100/      # Document processing worker
└── main.py                 # Main entry point
```

## Available Workers

### AAA0_0100_W - Example Worker
- **Purpose**: Demonstrates basic worker functionality
- **Supported Events**:
  - `echo`: Echo payload back
  - `process`: Convert text to uppercase
  - `transform`: Add timestamp to data

### AAA0_0101_W - VietOCR Worker
- **Purpose**: Vietnamese OCR processing
- **Supported Events**:
  - `vietocr`: Process with VietOCR
  - `paddleocr`: Process with PaddleOCR
  - `health_check`: Check OCR service status

### AAA0_0200_W - YOLO Worker
- **Purpose**: YOLO object detection
- **Supported Events**:
  - `detect`: General object detection
  - `detect_giay_ra_vien`: Specific detection for discharge papers
  - `list_models`: List available models

### ZZA0_0100_W - Document Processing Worker
- **Purpose**: PDF/TIFF document processing
- **Supported Events**:
  - `process_document`: Process complete document
  - `extract_pages`: Extract individual pages
  - `extract_text`: Extract text from pages
  - `get_metadata`: Get document metadata

## Running Python Workers

### Method 1: Using the run script
```bash
cd /root/deepapp/deepapp_main
./scripts/run_python_workers.sh
```

### Method 2: Direct Python execution
```bash
cd /root/deepapp/deepapp_main/src/main/python/com/deepapp/vn/io
export PYTHONPATH="/root/deepapp/deepapp_main/src/main/python:$PYTHONPATH"
python3 main.py --client-id python-worker --host 72.60.111.138 --port 50051
```

### Method 3: Custom configuration
```bash
python3 main.py \
  --client-id my-python-worker \
  --host localhost \
  --port 50052
```

## Testing Workers

You can test the workers using the existing Java application or by sending direct gRPC messages.

### Example Test Commands

```bash
# Test AAA0_0100_W echo
curl -X POST http://localhost:8080/api/cpp/call \
  -H "Content-Type: application/json" \
  -d '{
    "taskId": "AAA0_0100_W",
    "eventType": "echo",
    "payload": "Hello Python Worker!"
  }'

# Test AAA0_0101_W health check
curl -X POST http://localhost:8080/api/cpp/call \
  -H "Content-Type: application/json" \
  -d '{
    "taskId": "AAA0_0101_W",
    "eventType": "health_check",
    "payload": "{}"
  }'

# Test ZZA0_0100_W document processing
curl -X POST http://localhost:8080/api/cpp/call \
  -H "Content-Type: application/json" \
  -d '{
    "taskId": "ZZA0_0100_W",
    "eventType": "get_metadata",
    "payload": "{\"document_id\": \"test_doc_123\"}"
  }'
```

## Key Differences from C++ Workers

### Advantages of Python Workers:
- **Easier Development**: Python syntax is more concise
- **Rich Ecosystem**: Access to vast Python libraries (NumPy, OpenCV, etc.)
- **Dynamic Typing**: Faster prototyping
- **Cross-platform**: Better portability

### Current Limitations:
- **Performance**: Generally slower than C++ for compute-intensive tasks
- **Memory Usage**: Higher memory footprint
- **gRPC Integration**: Currently uses mock implementation (needs proto files)

## Adding New Workers

1. **Create worker directory structure**:
   ```bash
   mkdir -p com/deepapp/vn/io/XX/Y0/XXY0_1234/worker
   ```

2. **Create worker class**:
   ```python
   from com.deepapp.infrastructure.BaseWorker import BaseWorker
   from com.deepapp.infrastructure.WorkerRegistry import register_worker

   @register_worker("XXY0_1234_W")
   class XXY0_1234_Worker(BaseWorker):
       def __init__(self):
           super().__init__("XXY0_1234_Worker")

       def process_task(self, event_type: str, payload: str) -> str:
           # Implement your logic here
           return self.create_response("success", "result")
   ```

3. **Add supported event types**:
   ```python
   def can_handle(self, event_type: str) -> bool:
       return event_type in ["event1", "event2", "event3"]
   ```

4. **Create __init__.py files** in all directories

## Integration with YOLO ONNX

The AAA0_0200_W worker is designed to work with YOLO ONNX models. To use real YOLO detection:

1. **Convert PyTorch model to ONNX**:
   ```bash
   pip install ultralytics onnx
   python3 -c "
   from ultralytics import YOLO
   model = YOLO('path/to/best.pt')
   model.export(format='onnx')
   "
   ```

2. **Update model path** in AAA0_0200_W.py:
   ```python
   giay_ra_vien_path = '/path/to/converted/model.onnx'
   ```

3. **Implement real ONNX inference** using onnxruntime

## PythonWorkerManager Integration

Python workers are automatically managed by `PythonWorkerManager` (similar to `CppWorkerManager`):

### Configuration

Add to `application.yml`:
```yaml
python:
  worker:
    autoStart: true
    pythonPath: python3
    mainScript: src/main/python/com/deepapp/vn/io/main.py

workers:
  python:
    host: localhost
    port: 50052
    targetId: python-worker
```

### Automatic Startup

When Spring Boot starts:
1. `PythonWorkerManager.initialize()` is called
2. Python process starts with configured parameters
3. Workers register and connect via gRPC
4. Java application can route tasks to Python workers

### Manual Startup

```bash
# Using the launcher script
./scripts/run_python_workers.sh

# Direct execution
cd src/main/python/com/deepapp/vn/io
python3 main.py --client-id python-worker --host localhost --port 50052
```

## Future Enhancements

- [ ] Real gRPC integration with proto-generated files
- [ ] ONNX Runtime integration for YOLO models
- [ ] Async processing capabilities
- [ ] Worker health monitoring
- [ ] Load balancing across multiple Python processes
- [ ] Configuration management
- [ ] Logging integration with main application