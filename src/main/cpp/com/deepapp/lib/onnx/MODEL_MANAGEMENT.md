# Model Management

## Overview

Models được lưu trữ trong GitLab repository riêng biệt và được tự động download khi cần thiết.

**Repository**: https://gitlab.com/dnt.doanngocthanh/deepappmodels

## Architecture

```
GitLab Repository (deepappmodels)
    ├── vietocr/
    │   ├── transformer_encoder.onnx
    │   ├── transformer_decoder.onnx
    │   └── vocab.txt
    │
    ├── paddleocr/
    │   ├── ch_PP-OCRv4_det_infer.onnx
    │   ├── ch_PP-OCRv4_rec_infer.onnx
    │   └── ppocr_keys_v1.txt
    │
    └── yolo/
        ├── yolov8n.onnx
        ├── yolov8s.onnx
        └── coco_classes.txt

                    ↓ Download

Local Storage (/app/models)
    └── [same structure]

                    ↓ Load

ONNX Runtime (in memory)
```

## ModelManager Class

### C++ Usage

```cpp
#include "lib/onnx/ModelManager.hpp"

using deepapp::lib::onnx::ModelManager;

// Get singleton instance
auto& model_manager = ModelManager::getInstance();

// Configure (optional - has defaults)
model_manager.setModelsDir("/app/models");
model_manager.setGitLabRepo(
    "https://gitlab.com/dnt.doanngocthanh/deepappmodels",
    "main"
);

// Get model path - will download if not exists
try {
    std::string encoder_path = model_manager.getModelPath("vietocr/transformer_encoder.onnx");
    std::string decoder_path = model_manager.getModelPath("vietocr/transformer_decoder.onnx");
    std::string vocab_path = model_manager.getModelPath("vietocr/vocab.txt");
    
    // Now use models
    auto vietocr = std::make_unique<VietOCR>(encoder_path, decoder_path, vocab_path);
    
} catch (const std::exception& e) {
    std::cerr << "Failed to get models: " << e.what() << std::endl;
}
```

### Worker Example

```cpp
class AAA0_0101_Worker : public BaseWorker {
private:
    std::unique_ptr<deepapp::ocr::VietOCR> vietocr_;
    
public:
    AAA0_0101_Worker() {
        auto& mgr = deepapp::lib::onnx::ModelManager::getInstance();
        
        try {
            // Models will be downloaded automatically if not present
            std::string encoder = mgr.getModelPath("vietocr/transformer_encoder.onnx");
            std::string decoder = mgr.getModelPath("vietocr/transformer_decoder.onnx");
            std::string vocab = mgr.getModelPath("vietocr/vocab.txt");
            
            vietocr_ = std::make_unique<deepapp::ocr::VietOCR>(
                encoder, decoder, vocab
            );
            
            std::cout << "[AAA0_0101] VietOCR initialized successfully" << std::endl;
            
        } catch (const std::exception& e) {
            std::cerr << "[AAA0_0101] Failed to initialize: " << e.what() << std::endl;
            throw;
        }
    }
    
    std::string processTask(const std::string& event_type,
                           const std::string& payload) override {
        // Use vietocr_->predict()...
    }
};
```

## Download Script

### Manual Download

```bash
# Download all models
/root/deepapp/deepapp_main/scripts/download_models.sh all

# Download specific model set
./scripts/download_models.sh vietocr
./scripts/download_models.sh paddleocr
./scripts/download_models.sh yolo

# List downloaded models
./scripts/download_models.sh list

# Clean cache
./scripts/download_models.sh clean
```

### Docker Integration

Add to Dockerfile:

```dockerfile
# Copy download script
COPY scripts/download_models.sh /app/scripts/

# Download models during build (optional)
RUN /app/scripts/download_models.sh all || echo "Models will be downloaded at runtime"

# Or download at runtime in entrypoint
ENTRYPOINT ["/bin/bash", "-c", "/app/scripts/download_models.sh all && /app/build/deepapp_worker_main"]
```

### Docker Compose with Volume

```yaml
version: '3.8'

services:
  deepapp-main:
    image: deepapp_main
    volumes:
      - ./models:/app/models  # Persistent model storage
    environment:
      - GITLAB_TOKEN=${GITLAB_TOKEN}  # Optional, for private repo
```

## Environment Variables

```bash
# GitLab access token (for private repositories)
export GITLAB_TOKEN="your-gitlab-token-here"

# Custom models directory
export MODELS_DIR="/custom/path/to/models"

# Use in application
docker run -e GITLAB_TOKEN=$GITLAB_TOKEN \
           -v $(pwd)/models:/app/models \
           deepapp_main
```

## Model Repository Structure

### Required Files

Each model set must include:

**VietOCR**:
- `vietocr/transformer_encoder.onnx` (~50MB)
- `vietocr/transformer_decoder.onnx` (~30MB)
- `vietocr/vocab.txt` (233 characters)

**PaddleOCR**:
- `paddleocr/ch_PP-OCRv4_det_infer.onnx` (~2.5MB)
- `paddleocr/ch_PP-OCRv4_rec_infer.onnx` (~8MB)
- `paddleocr/ppocr_keys_v1.txt` (6623 characters)

**YOLO**:
- `yolo/yolov8n.onnx` (~6MB)
- `yolo/yolov8s.onnx` (~22MB)
- `yolo/coco_classes.txt` (80 classes)

### Upload Models to GitLab

```bash
# Clone deepappmodels repository
git clone https://gitlab.com/dnt.doanngocthanh/deepappmodels.git
cd deepappmodels

# Create directories
mkdir -p vietocr paddleocr yolo

# Copy models
cp /path/to/models/vietocr/* vietocr/
cp /path/to/models/paddleocr/* paddleocr/
cp /path/to/models/yolo/* yolo/

# Commit and push
git add .
git commit -m "Add ONNX models"
git push origin main
```

### Git LFS for Large Files

For models > 100MB, use Git LFS:

```bash
# Install Git LFS
git lfs install

# Track large files
git lfs track "*.onnx"
git add .gitattributes

# Commit and push as normal
git add .
git commit -m "Add models with LFS"
git push
```

## Verification

### Check Local Models

```bash
# List all models
ls -lh /app/models/**/*.onnx

# Check sizes
du -sh /app/models/*

# Verify integrity
file /app/models/vietocr/transformer_encoder.onnx
# Should output: ONNX model data
```

### Test Download

```cpp
#include "lib/onnx/ModelManager.hpp"

int main() {
    auto& mgr = deepapp::lib::onnx::ModelManager::getInstance();
    
    // Check if model exists
    bool exists = mgr.modelExists("/app/models/vietocr/transformer_encoder.onnx");
    std::cout << "Model exists: " << (exists ? "yes" : "no") << std::endl;
    
    // List local models
    auto models = mgr.listLocalModels();
    std::cout << "Local models (" << models.size() << "):" << std::endl;
    for (const auto& model : models) {
        std::cout << "  - " << model << std::endl;
    }
    
    // Get cache size
    size_t cache_size = mgr.getCacheSize();
    std::cout << "Cache size: " << cache_size / 1024 / 1024 << " MB" << std::endl;
    
    return 0;
}
```

## Performance Optimization

### Preload Models

Download models during Docker build instead of runtime:

```dockerfile
# Dockerfile
ARG GITLAB_TOKEN
ENV GITLAB_TOKEN=${GITLAB_TOKEN}

RUN /app/scripts/download_models.sh all
```

Build with token:

```bash
docker build --build-arg GITLAB_TOKEN=$GITLAB_TOKEN -t deepapp_main .
```

### Persistent Volume

Use Docker volume to avoid re-downloading:

```yaml
volumes:
  model-cache:

services:
  deepapp-main:
    volumes:
      - model-cache:/app/models
```

### Lazy Loading

Models are only downloaded when first requested:

```cpp
// First request - downloads models (2-3 seconds)
std::string path = model_manager.getModelPath("vietocr/encoder.onnx");

// Subsequent requests - instant (file exists)
std::string path2 = model_manager.getModelPath("vietocr/encoder.onnx");
```

## Troubleshooting

### Download Fails

```bash
# Check network
curl -I https://gitlab.com/dnt.doanngocthanh/deepappmodels

# Check token (if private repo)
curl -H "PRIVATE-TOKEN: $GITLAB_TOKEN" \
  https://gitlab.com/api/v4/user

# Manual download
curl -L -o /app/models/vietocr/encoder.onnx \
  https://gitlab.com/dnt.doanngocthanh/deepappmodels/-/raw/main/vietocr/transformer_encoder.onnx
```

### Permission Issues

```bash
# Fix permissions
chown -R app:app /app/models
chmod -R 755 /app/models
```

### Disk Space

```bash
# Check available space
df -h /app/models

# Clear old models
rm -rf /app/models/*

# Re-download
/app/scripts/download_models.sh all
```

## Best Practices

1. **Version Control**: Tag model versions in GitLab
2. **Caching**: Use persistent volumes in production
3. **Monitoring**: Log download times and failures
4. **Fallback**: Have backup download URLs
5. **Validation**: Verify model checksums after download
6. **Cleanup**: Implement model rotation for old versions

## Security

### Private Repository

```bash
# Generate GitLab token
# Settings -> Access Tokens -> Add new token
# Scope: read_repository

# Set token
export GITLAB_TOKEN="glpat-xxxxxxxxxxxxx"

# Use in application
docker run -e GITLAB_TOKEN=$GITLAB_TOKEN deepapp_main
```

### Token Storage

```bash
# Use Docker secrets
echo "$GITLAB_TOKEN" | docker secret create gitlab_token -

# Use in service
services:
  deepapp-main:
    secrets:
      - gitlab_token
```

## Monitoring

### Log Downloads

```cpp
// ModelManager logs automatically
[ModelManager] Model not found, downloading: vietocr/encoder.onnx
[ModelManager] Downloading from: https://gitlab.com/.../vietocr/encoder.onnx
[ModelManager] Model downloaded successfully: /app/models/vietocr/encoder.onnx
```

### Metrics

Track:
- Download time per model
- Cache hit rate
- Disk usage
- Failed downloads

## Future Enhancements

1. **CDN Integration**: Use CloudFlare/S3 for faster downloads
2. **Checksum Verification**: SHA256 validation
3. **Compression**: Gzip models during transfer
4. **Delta Updates**: Only download changed parts
5. **Mirror Sites**: Fallback download locations
6. **Model Registry**: Central model catalog with metadata
