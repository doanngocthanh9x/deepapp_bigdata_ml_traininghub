#!/bin/bash

# Build C++ Workers

echo "========================================"
echo "Building DeepApp C++ Workers"
echo "========================================"

cd /root/deepapp/deepapp_main

# Create build directory
mkdir -p build
cd build

# Run CMake
echo "Running CMake..."
cmake .. || {
    echo "❌ CMake failed!"
    exit 1
}

# Build
echo "Building..."
make -j$(nproc) || {
    echo "❌ Build failed!"
    exit 1
}

echo ""
echo "✅ Build successful!"
echo ""
echo "Executable: build/deepapp_worker_main"
echo ""
echo "To run:"
echo "  ./build/deepapp_worker_main [server_address] [client_id]"
echo ""
echo "Example:"
echo "  ./build/deepapp_worker_main 72.60.111.138:50051 cpp-worker"
echo ""
