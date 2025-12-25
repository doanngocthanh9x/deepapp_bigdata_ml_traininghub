# AAA0_0101 - VietOCR Integration

## Tổng quan

Worker AAA0_0101 tích hợp VietOCR (Vietnamese OCR) với 2 versions:

1. **AAA0_0101_W.cpp** (Current - Mock version)
   - Đang sử dụng
   - Trả về mock data để test infrastructure
   - Không cần dependencies phức tạp

2. **AAA0_0101_W_FULL.cpp** (Full VietOCR implementation)
   - Implementation đầy đủ với ONNX Runtime
   - Cần models và dependencies bổ sung
   - Sẵn sàng deploy khi có models

## Architecture

```
AAA0_0101/
├── worker/
│   ├── AAA0_0101_W.cpp          # Mock version (hiện tại)
│   └── AAA0_0101_W_FULL.cpp     # Full version (sẵn sàng)
└── lib/
    ├── vietocr_onnx.hpp         # VietOCR ONNX wrapper
    ├── vietocr_onnx.cpp
    ├── yolov8_onnx.hpp          # YOLO v8 ONNX wrapper  
    └── yolov8_onnx.cpp
```

## Dependencies (For Full Version)

### 1. OpenCV 4.x
```bash
apt-get install libopencv-dev
```

### 2. ONNX Runtime
```bash
# Download from https://github.com/microsoft/onnxruntime/releases
wget https://github.com/microsoft/onnxruntime/releases/download/v1.16.0/onnxruntime-linux-x64-1.16.0.tgz
tar -xzf onnxruntime-linux-x64-1.16.0.tgz
```

### 3. VietOCR Models
Models cần đặt trong `/app/models/vietocr/`:
- `transformer_encoder.onnx` - CNN + Transformer Encoder
- `transformer_decoder.onnx` - Transformer Decoder  
- `vocab.txt` - Vietnamese vocabulary (233 characters)

Download từ: https://github.com/pbcquoc/vietocr

## CMakeLists.txt Updates Needed

```cmake
# Find OpenCV
find_package(OpenCV REQUIRED)
include_directories(${OpenCV_INCLUDE_DIRS})

# Find ONNX Runtime
set(ONNXRUNTIME_DIR "/opt/onnxruntime")
include_directories(${ONNXRUNTIME_DIR}/include)
link_directories(${ONNXRUNTIME_DIR}/lib)

# Add VietOCR library sources
set(VIETOCR_LIB_SOURCES
    ${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/vn/io/AA/A0/AAA0_0101/lib/vietocr_onnx.cpp
)

# Link libraries
target_link_libraries(deepapp_worker_main
    ${GRPC_LIBRARIES}
    ${PROTOBUF_LIBRARIES}
    ${OpenCV_LIBS}
    onnxruntime
    pthread
)
```

## Switching to Full Version

1. **Backup mock version:**
```bash
mv AAA0_0101_W.cpp AAA0_0101_W_MOCK.cpp
```

2. **Activate full version:**
```bash
mv AAA0_0101_W_FULL.cpp AAA0_0101_W.cpp
```

3. **Update CMakeLists.txt** with dependencies above

4. **Rebuild:**
```bash
cd /root/deepapp/deepapp_main
mvn clean package
docker-compose build
docker-compose up -d
```

## Testing

### Mock Version (Current)
```bash
curl -X POST http://localhost:8080/AA/A0/AAA0_0101 \
  -H "Content-Type: application/json" \
  -d '{
    "imagePath":"/app/test.jpg",
    "language":"vi",
    "engine":"vietocr"
  }'
```

Response:
```json
{
  "success": true,
  "text": "Image from path: /app/test.jpg - Mock OCR result",
  "time_ms": 0,
  "engine": "vietocr",
  "language": "vi",
  "worker": "AAA0_0101_W"
}
```

### Full Version (After Setup)
Same request, but with actual VietOCR recognition:
```json
{
  "success": true,
  "text": "Hà Nội - Việt Nam",
  "time_ms": 597,
  "total_ms": 612,
  "engine": "vietocr",
  "language": "vi",
  "worker": "AAA0_0101_W",
  "image": {"width": 800, "height": 600}
}
```

## Base64 Image Support

Both versions support base64 encoded images:

```bash
curl -X POST http://localhost:8080/AA/A0/AAA0_0101 \
  -H "Content-Type: application/json" \
  -d '{
    "image": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
    "language": "vi"
  }'
```

## Performance

### Mock Version
- Cold start: < 1ms
- Processing: < 1ms

### Full Version (Expected)
- Cold start (model loading): ~2-3 seconds
- Processing: 300-800ms per image
- Memory: ~500MB (models + inference)

## Next Steps

1. ✅ Infrastructure working với mock data
2. ⏳ Chuẩn bị models và dependencies
3. ⏳ Deploy full version
4. ⏳ Performance tuning
5. ⏳ Add PaddleOCR engine support

## References

- VietOCR: https://github.com/pbcquoc/vietocr
- ONNX Runtime: https://onnxruntime.ai/
- OpenCV: https://opencv.org/
