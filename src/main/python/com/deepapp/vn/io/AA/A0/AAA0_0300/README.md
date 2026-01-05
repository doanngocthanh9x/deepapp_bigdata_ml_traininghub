# AAA0_0300 - LLM Inference Worker

Vietnamese Language Model Inference using llama-cpp-python and llama.cpp

## Features

- Vietnamese LLM inference (VinAllama, VietCuna, PhoBERT)
- Support for both Python and C++ workers
- Chat history support
- Model caching
- Inference statistics

## Installation

### Python Worker

```bash
pip install llama-cpp-python
```

For GPU support (CUDA):
```bash
CMAKE_ARGS="-DLLAMA_CUBLAS=on" pip install llama-cpp-python
```

### C++ Worker

```bash
cd /path/to/llama.cpp
mkdir build && cd build
cmake .. -DLLAMA_CUBLAS=ON
make -j
```

## Models

Download models from HuggingFace:

```bash
# VinAllama 7B Chat
huggingface-cli download vilm/vinallama-7b-chat-GGUF vinallama-7b-chat_q5_0.gguf --local-dir /root/models

# VietCuna 7B
huggingface-cli download Viet-Mistral/vietcuna-7b-v3-GGUF vietcuna-7b-q5_k_m.gguf --local-dir /root/models
```

## Usage

### Frontend (React)

Navigate to `/modules/AA/A0/AAA0_0300` in the web interface.

### REST API

```bash
# Run inference
curl -X POST http://localhost:8080/AA/A0/AAA0_0300/inference \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Tên người bệnh? - Họ tên: Nguyễn Văn A",
    "workerType": "python",
    "temperature": 0.1,
    "maxTokens": 200,
    "modelName": "vinallama-7b-chat"
  }'

# Get statistics
curl http://localhost:8080/AA/A0/AAA0_0300/stats

# List models
curl http://localhost:8080/AA/A0/AAA0_0300/models
```

### Python Worker Direct

```python
from com.deepapp.vn.io.AA.A0.AAA0_0300.worker.AAA0_0300_W import AAA0_0300_Worker

worker = AAA0_0300_Worker()
result = worker.process_task("inference", json.dumps({
    "prompt": "Your prompt here",
    "temperature": 0.1,
    "max_tokens": 200
}))
```

## Configuration

- **Temperature**: 0.0 - 1.0 (lower = more focused, higher = more creative)
- **Max Tokens**: 50 - 2048 (response length)
- **Worker Type**: python or cpp
- **Model**: vinallama-7b-chat, vietcuna-7b, phobert-base

## Architecture

```
Frontend (React) → Java Controller → gRPC Hub → Worker (Python/C++)
                                                      ↓
                                                  llama.cpp
                                                      ↓
                                                   GGUF Model
```

## Performance

- Python Worker: ~2-5s per inference (CPU)
- C++ Worker: ~1-3s per inference (CPU)
- With GPU: ~0.5-1s per inference

## Troubleshooting

### Model not found

Place GGUF models in `/root/models/` or configure path in worker.

### Memory issues

Reduce `n_ctx` parameter or use smaller quantization (Q4 instead of Q5).

### Slow inference

Enable GPU support or use C++ worker for better performance.
