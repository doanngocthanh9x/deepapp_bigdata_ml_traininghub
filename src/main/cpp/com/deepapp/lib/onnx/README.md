# Shared ONNX Library

## Overview

Thư viện ONNX chung để tái sử dụng cho tất cả các models (VietOCR, PaddleOCR, YOLO, etc.)

## Architecture

```
lib/
├── onnx/
│   ├── OnnxModelBase.hpp       # Base class cho tất cả ONNX models
│   ├── OnnxModelBase.cpp       # Implementation
│   └── README.md               # Documentation
└── utils/
    └── base64.hpp              # Base64 encode/decode utilities
```

## OnnxModelBase Class

### Features

- **Lazy Loading**: Load model khi cần thiết
- **Thread-Safe**: Mutex protection cho inference
- **Metadata Auto-Discovery**: Tự động lấy input/output names và shapes
- **Helper Methods**: Tensor creation, data extraction, inference execution

### Usage Example

```cpp
#include "lib/onnx/OnnxModelBase.hpp"

class MyModel : public deepapp::lib::onnx::OnnxModelBase {
public:
    MyModel(const std::string& model_path)
        : OnnxModelBase(model_path, 4) {}
    
    std::string predict(const cv::Mat& image) {
        // Preprocess
        std::vector<float> input_data = preprocess(image);
        std::vector<int64_t> input_shape = {1, 3, 224, 224};
        
        // Create tensor
        auto input_tensor = createTensor(input_data, input_shape);
        std::vector<Ort::Value> inputs;
        inputs.push_back(std::move(input_tensor));
        
        // Run inference
        auto outputs = runInference(inputs);
        
        // Postprocess
        return postprocess(outputs[0]);
    }
    
private:
    std::vector<float> preprocess(const cv::Mat& image) {
        // Your preprocessing logic
    }
    
    std::string postprocess(Ort::Value& output) {
        auto data = extractTensorData(output);
        // Your postprocessing logic
    }
};
```

## Base64 Utilities

```cpp
#include "lib/utils/base64.hpp"

using deepapp::lib::utils::Base64;

// Decode base64 to binary
std::string base64_str = "...";
auto binary = Base64::decode(base64_str);

// Decode to OpenCV Mat
cv::Mat image = cv::imdecode(binary, cv::IMREAD_COLOR);

// Encode binary to base64
std::string encoded = Base64::encode(binary);
```

## CMakeLists.txt Integration

```cmake
# Add shared library sources
set(SHARED_LIB_SOURCES
    ${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/lib/onnx/OnnxModelBase.cpp
)

# Include directories
include_directories(
    ${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp
)

# Link with ONNX Runtime
find_package(onnxruntime REQUIRED)
target_link_libraries(your_target PRIVATE onnxruntime)
```

## Benefits

1. **Code Reusability**: Không cần viết lại ONNX loading logic cho mỗi model
2. **Consistency**: Tất cả models sử dụng cùng một cách load và run
3. **Maintainability**: Bug fixes ở một chỗ, apply cho tất cả models
4. **Thread Safety**: Built-in mutex protection
5. **Easy Extension**: Chỉ cần extend class và implement preprocess/postprocess

## Supported Models

- ✅ VietOCR (transformer encoder/decoder)
- 🔄 PaddleOCR (in progress)
- 🔄 YOLO v8 (in progress)
- 🔜 Any ONNX model

## Requirements

- ONNX Runtime 1.16.0+
- OpenCV 4.x (for image processing)
- C++17 or later

## Next Steps

1. Refactor VietOCR to use OnnxModelBase
2. Refactor YOLO to use OnnxModelBase
3. Add PaddleOCR implementation
4. Add performance benchmarks
