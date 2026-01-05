#!/bin/bash
# Quick setup script for AAA0_0300 LLM Inference with llama.cpp

set -e

echo "================================================"
echo "AAA0_0300 - LLM Inference Setup"
echo "================================================"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Step 1: Clone llama.cpp
echo -e "${YELLOW}Step 1: Installing llama.cpp...${NC}"
if [ -d "/root/llama.cpp" ]; then
    echo "llama.cpp already exists, pulling latest changes..."
    cd /root/llama.cpp
    git pull
else
    echo "Cloning llama.cpp..."
    cd /root
    git clone https://github.com/ggerganov/llama.cpp
    cd llama.cpp
fi

# Step 2: Build llama.cpp
echo -e "${YELLOW}Step 2: Building llama.cpp...${NC}"
mkdir -p build
cd build

# Detect GPU
if command -v nvidia-smi &> /dev/null; then
    echo "NVIDIA GPU detected, building with CUDA support..."
    cmake .. -DLLAMA_CUBLAS=ON -DLLAMA_CURL=OFF -DCMAKE_BUILD_TYPE=Release
else
    echo "Building for CPU..."
    cmake .. -DLLAMA_CURL=OFF -DCMAKE_BUILD_TYPE=Release
fi

make -j$(nproc)

# Verify build
if [ -f "libllama.so" ]; then
    echo -e "${GREEN}✓ llama.cpp built successfully${NC}"
    ls -lh libllama.so
else
    echo -e "${RED}✗ Failed to build llama.cpp${NC}"
    exit 1
fi

# Step 3: Install system-wide (optional)
echo -e "${YELLOW}Step 3: Installing llama.cpp system-wide...${NC}"
read -p "Install system-wide? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    sudo cp libllama.so /usr/local/lib/
    sudo cp ../llama.h /usr/local/include/
    sudo ldconfig
    echo -e "${GREEN}✓ Installed to /usr/local${NC}"
else
    echo "Skipped system-wide installation"
    echo "Library at: /root/llama.cpp/build/libllama.so"
fi

# Step 4: Download models
echo -e "${YELLOW}Step 4: Downloading models...${NC}"
mkdir -p /root/models
cd /root/models

if [ ! -f "vinallama-7b-chat_q5_0.gguf" ]; then
    echo "Downloading VinAllama 7B Chat (Q5_0, ~4.7GB)..."
    
    # Install huggingface-cli if not present
    if ! command -v huggingface-cli &> /dev/null; then
        echo "Installing huggingface-cli..."
        pip install -U huggingface-hub
    fi
    
    huggingface-cli download vilm/vinallama-7b-chat-GGUF \
        vinallama-7b-chat_q5_0.gguf \
        --local-dir /root/models \
        --local-dir-use-symlinks False
    
    echo -e "${GREEN}✓ Model downloaded${NC}"
else
    echo "Model already exists"
fi

ls -lh /root/models/*.gguf

# Step 5: Rebuild deepapp
echo -e "${YELLOW}Step 5: Building deepapp with llama.cpp support...${NC}"
cd /root/deepapp/deepapp_main

# Clean build
rm -rf build
mkdir build
cd build

# Configure
cmake ..

# Check if llama was found
if grep -q "Found llama.cpp" CMakeCache.txt 2>/dev/null; then
    echo -e "${GREEN}✓ llama.cpp detected by CMake${NC}"
else
    echo -e "${YELLOW}⚠ llama.cpp not auto-detected, specifying manually...${NC}"
    cmake .. \
        -DLLAMA_INCLUDE_DIR=/root/llama.cpp \
        -DLLAMA_LIBRARY=/root/llama.cpp/build/libllama.so
fi

# Build
echo "Building deepapp_worker_main..."
make -j$(nproc)

if [ -f "deepapp_worker_main" ]; then
    echo -e "${GREEN}✓ deepapp_worker_main built successfully${NC}"
    ls -lh deepapp_worker_main
else
    echo -e "${RED}✗ Failed to build deepapp_worker_main${NC}"
    exit 1
fi

# Step 6: Install Python dependencies
echo -e "${YELLOW}Step 6: Installing Python dependencies...${NC}"
pip install llama-cpp-python --upgrade

echo ""
echo -e "${GREEN}================================================${NC}"
echo -e "${GREEN}Installation Complete!${NC}"
echo -e "${GREEN}================================================${NC}"
echo ""
echo "To start the application:"
echo "  cd /root/deepapp/deepapp_main"
echo "  mvn spring-boot:run"
echo ""
echo "Or run C++ worker directly:"
echo "  cd /root/deepapp/deepapp_main/build"
echo "  ./deepapp_worker_main"
echo ""
echo "Test inference:"
echo "  curl -X POST http://localhost:8080/AA/A0/AAA0_0300/inference \\"
echo "    -H 'Content-Type: application/json' \\"
echo "    -d '{\"prompt\": \"Xin chào\", \"workerType\": \"cpp\"}'"
echo ""
echo "Frontend: http://localhost:8080/modules/AA/A0/AAA0_0300"
echo ""
