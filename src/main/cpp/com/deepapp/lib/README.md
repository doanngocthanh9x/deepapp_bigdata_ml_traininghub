# Shared ONNX Library Architecture

## Overview

Kiến trúc thư viện ONNX chung cho tất cả các models AI trong deepapp.

## Structure

```
src/main/cpp/com/deepapp/
├── lib/                                    # Shared libraries
│   ├── onnx/                              # ONNX base functionality
│   │   ├── OnnxModelBase.hpp             # Base class cho ONNX models
│   │   ├── OnnxModelBase.cpp             # Implementation
│   │   └── README.md                      # Documentation
│   │
│   ├── utils/                             # Utility functions
│   │   ├── base64.hpp                     # Base64 encode/decode
│   │   └── image_utils.hpp (future)       # Image processing utilities
│   │
│   └── models/                            # Model implementations
│       ├── vietocr_model.hpp             # VietOCR (refactored)
│       ├── paddleocr_model.hpp           # PaddleOCR
│       └── yolov8_model.hpp              # YOLO v8
│
└── vn/io/AA/A0/                          # Business logic modules
    ├── AAA0_0101/                         # OCR Services
    │   ├── worker/
    │   │   └── AAA0_0101_W.cpp           # OCR Worker (uses lib/models)
    │   └── lib/                           # Legacy (to be deprecated)
    │       ├── vietocr_onnx.hpp          # Old implementation
    │       └── yolov8_onnx.hpp           # Old implementation
    │
    └── AAA0_0102/ (future)                # Object Detection Services
        └── worker/
            └── AAA0_0102_W.cpp           # Detection Worker
```

## Design Principles

### 1. Base Class Approach

**OnnxModelBase** provides common functionality:
- Model loading and session management
- Thread-safe inference
- Automatic metadata discovery
- Helper methods for tensor operations

**Specific models** extend OnnxModelBase:
- VietOCR: Encoder-decoder transformer for Vietnamese text
- PaddleOCR: Detection + Recognition pipeline
- YOLOv8: Object detection

### 2. Separation of Concerns

```
┌─────────────────────────────────────────┐
│         Worker Layer                     │  # Business logic
│  - Request parsing                       │  # Error handling
│  - Response formatting                   │  # Timing/logging
│  - Model lifecycle                       │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│         Model Layer                      │  # AI/ML logic
│  - Preprocessing                         │  # Inference
│  - Postprocessing                        │  # Model-specific
│  - Domain logic                          │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│         ONNX Base Layer                  │  # Infrastructure
│  - Model loading                         │  # Session management
│  - Tensor operations                     │  # Thread safety
│  - Generic inference                     │
└─────────────────────────────────────────┘
```

### 3. Code Reusability

**Before (Old Architecture)**:
```cpp
// AAA0_0101/lib/vietocr_onnx.hpp
class VietOCR_ONNX {
    Ort::Env env_;
    Ort::SessionOptions session_options_;
    std::unique_ptr<Ort::Session> encoder_session_;
    // ... duplicate ONNX setup code
};

// AAA0_0101/lib/yolov8_onnx.hpp
class YOLOv8_ONNX {
    Ort::Env env_;
    Ort::SessionOptions session_options_;
    std::unique_ptr<Ort::Session> session_;
    // ... duplicate ONNX setup code
};
```

**After (New Architecture)**:
```cpp
// lib/models/vietocr_model.hpp
class VietOCR {
    std::unique_ptr<OnnxModelBase> encoder_;
    std::unique_ptr<OnnxModelBase> decoder_;
    // Only model-specific logic
};

// lib/models/yolov8_model.hpp
class YOLOv8 : public OnnxModelBase {
    // Only detection-specific logic
};
```

## Usage Examples

### Example 1: VietOCR Worker

```cpp
#include "lib/models/vietocr_model.hpp"
#include "lib/utils/base64.hpp"

class AAA0_0101_Worker : public BaseWorker {
private:
    std::unique_ptr<deepapp::ocr::VietOCR> vietocr_;
    
public:
    void initialize() {
        vietocr_ = std::make_unique<deepapp::ocr::VietOCR>(
            "/app/models/vietocr/encoder.onnx",
            "/app/models/vietocr/decoder.onnx",
            "/app/models/vietocr/vocab.txt"
        );
    }
    
    std::string processTask(const std::string& event_type,
                           const std::string& payload) {
        // Parse request
        std::string base64_image = extractImageFromJson(payload);
        
        // Decode base64
        auto binary = deepapp::lib::utils::Base64::decode(base64_image);
        cv::Mat image = cv::imdecode(binary, cv::IMREAD_COLOR);
        
        // OCR
        std::string text = vietocr_->predict(image);
        
        // Format response
        return formatResponse(text);
    }
};
```

### Example 2: PaddleOCR Worker

```cpp
#include "lib/models/paddleocr_model.hpp"

class PaddleOCRWorker : public BaseWorker {
private:
    std::unique_ptr<deepapp::ocr::PaddleOCR> paddleocr_;
    
public:
    void initialize() {
        paddleocr_ = std::make_unique<deepapp::ocr::PaddleOCR>(
            "/app/models/paddle/det_model.onnx",
            "/app/models/paddle/rec_model.onnx",
            "/app/models/paddle/ppocr_keys_v1.txt"
        );
    }
    
    std::string processTask(const std::string& event_type,
                           const std::string& payload) {
        cv::Mat image = loadImage(payload);
        
        // Full pipeline: detection + recognition
        auto results = paddleocr_->ocr(image);
        
        // results = [(text1, bbox1), (text2, bbox2), ...]
        return formatResults(results);
    }
};
```

### Example 3: YOLO Detection Worker

```cpp
#include "lib/models/yolov8_model.hpp"

class DetectionWorker : public BaseWorker {
private:
    std::unique_ptr<deepapp::detection::YOLOv8> yolo_;
    
public:
    void initialize() {
        std::vector<std::string> classes = {"person", "car", "document", ...};
        yolo_ = std::make_unique<deepapp::detection::YOLOv8>(
            "/app/models/yolo/yolov8n.onnx",
            classes,
            0.25f,  // confidence threshold
            0.45f   // IOU threshold
        );
    }
    
    std::string processTask(const std::string& event_type,
                           const std::string& payload) {
        cv::Mat image = loadImage(payload);
        
        // Detect objects
        auto detections = yolo_->detect(image);
        
        // detections = [Detection{bbox, confidence, class_id}, ...]
        return formatDetections(detections);
    }
};
```

## Migration Guide

### Step 1: Update CMakeLists.txt

```cmake
# Add shared library sources
set(SHARED_LIB_SOURCES
    ${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/lib/onnx/OnnxModelBase.cpp
)

# Add to worker executable
add_executable(deepapp_worker_main
    ${WORKER_SOURCES}
    ${SHARED_LIB_SOURCES}  # Add shared libs
    ${PROTO_SRCS}
    ${PROTO_HDRS}
)

# Include directories
include_directories(
    ${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp  # Base path
    ${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/lib  # Shared libs
)

# Link ONNX Runtime
find_package(onnxruntime REQUIRED)
target_link_libraries(deepapp_worker_main
    PRIVATE
    onnxruntime
    opencv_core
    opencv_imgproc
    opencv_imgcodecs
)
```

### Step 2: Update Existing Workers

Replace old includes:
```cpp
// Old
#include "../lib/vietocr_onnx.hpp"

// New
#include "lib/models/vietocr_model.hpp"
#include "lib/utils/base64.hpp"
```

Replace old class usage:
```cpp
// Old
std::unique_ptr<VietOCR_ONNX> recognizer_;
recognizer_ = std::make_unique<VietOCR_ONNX>(...);

// New
std::unique_ptr<deepapp::ocr::VietOCR> recognizer_;
recognizer_ = std::make_unique<deepapp::ocr::VietOCR>(...);
```

### Step 3: Implement Model Layers (if not exist)

Create `.cpp` files for model implementations in `lib/models/`:
- `vietocr_model.cpp`
- `paddleocr_model.cpp`
- `yolov8_model.cpp`

## Benefits

### 1. Maintainability
- Bug fixes in one place apply to all models
- Consistent error handling
- Unified logging

### 2. Performance
- Shared session pool (future enhancement)
- Optimized tensor operations
- Thread-safe by design

### 3. Extensibility
- Easy to add new models
- Minimal boilerplate code
- Clear interfaces

### 4. Testing
- Mock OnnxModelBase for unit tests
- Test model logic independently
- Integration tests at worker layer

## Performance Considerations

### Model Loading
- **Cold start**: 2-3 seconds per model
- **Lazy loading**: Load on first request
- **Keep-alive**: Models stay in memory

### Inference
- **VietOCR**: ~300-800ms per image
- **PaddleOCR**: ~200-500ms per image
- **YOLOv8**: ~50-150ms per image

### Memory Usage
- **VietOCR**: ~500MB (encoder + decoder)
- **PaddleOCR**: ~300MB (det + rec)
- **YOLOv8**: ~200MB (single model)

## Future Enhancements

1. **Model Versioning**: Support multiple model versions
2. **Dynamic Loading**: Load/unload models based on usage
3. **GPU Support**: CUDA/TensorRT execution providers
4. **Model Registry**: Centralized model management
5. **Quantization**: INT8/FP16 for faster inference
6. **Batch Processing**: Optimize for batch inference

## Related Documentation

- [OnnxModelBase API](lib/onnx/README.md)
- [Base64 Utilities](lib/utils/base64.hpp)
- [AAA0_0101 Worker](vn/io/AA/A0/AAA0_0101/README.md)

## Support

For questions or issues:
1. Check existing documentation
2. Review example workers
3. Test with mock models first
4. Profile performance bottlenecks
