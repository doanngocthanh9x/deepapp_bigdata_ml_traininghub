# CMakeLists.txt Update Guide

## Required Changes for Shared ONNX Library

### 1. Add Shared Library Sources

```cmake
# Near the top, after finding packages
set(SHARED_LIB_SOURCES
    ${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/lib/onnx/OnnxModelBase.cpp
)
```

### 2. Update Include Directories

```cmake
# Add base path for lib/ includes
include_directories(
    ${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp
    ${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/infrastructure
    ${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/lib  # NEW
    ${Protobuf_INCLUDE_DIRS}
    ${CMAKE_CURRENT_BINARY_DIR}
)
```

### 3. Add Shared Libraries to Executable

```cmake
# Update deepapp_worker_main executable
add_executable(deepapp_worker_main
    ${WORKER_SOURCES}
    ${SHARED_LIB_SOURCES}  # NEW - Add shared libs
    ${PROTO_SRCS}
    ${PROTO_HDRS}
    ${GRPC_SRCS}
    ${GRPC_HDRS}
    ${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/infrastructure/BaseWorker.cpp
    ${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/infrastructure/WorkerRegistry.cpp
    ${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/infrastructure/GrpcWorkerClient.cpp
    ${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/main.cpp
)
```

### 4. Find and Link ONNX Runtime

```cmake
# Find ONNX Runtime (after other find_package commands)
find_package(onnxruntime REQUIRED)

# Alternative if not using find_package:
# set(ONNXRUNTIME_INCLUDE_DIRS /usr/include/onnxruntime)
# set(ONNXRUNTIME_LIBRARIES onnxruntime)

# Link libraries
target_link_libraries(deepapp_worker_main
    PRIVATE
    gRPC::grpc++
    protobuf::libprotobuf
    pthread
    onnxruntime  # NEW
    opencv_core
    opencv_imgproc
    opencv_imgcodecs
    opencv_highgui
)
```

### 5. Optional: Add Model-Specific Libraries

If implementing model wrappers in `.cpp` files:

```cmake
# Add model implementations
set(MODEL_LIB_SOURCES
    ${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/lib/models/vietocr_model.cpp
    ${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/lib/models/paddleocr_model.cpp
    ${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/lib/models/yolov8_model.cpp
)

# Add to executable
add_executable(deepapp_worker_main
    ${WORKER_SOURCES}
    ${SHARED_LIB_SOURCES}
    ${MODEL_LIB_SOURCES}  # NEW
    ...
)
```

### 6. Optional: Create Shared Library

For better modularity, create a separate shared library:

```cmake
# Create shared library for common code
add_library(deepapp_common SHARED
    ${SHARED_LIB_SOURCES}
    ${MODEL_LIB_SOURCES}
)

target_include_directories(deepapp_common
    PUBLIC
    ${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp
    ${ONNXRUNTIME_INCLUDE_DIRS}
)

target_link_libraries(deepapp_common
    PUBLIC
    onnxruntime
    opencv_core
    opencv_imgproc
    opencv_imgcodecs
)

# Link worker executable with shared library
target_link_libraries(deepapp_worker_main
    PRIVATE
    deepapp_common  # Use shared library
    gRPC::grpc++
    protobuf::libprotobuf
    pthread
)
```

## Complete CMakeLists.txt Template

```cmake
cmake_minimum_required(VERSION 3.16)
project(deepapp_worker VERSION 1.0.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find packages
find_package(Protobuf REQUIRED)
find_package(gRPC CONFIG REQUIRED)
find_package(OpenCV REQUIRED)
find_package(onnxruntime REQUIRED)

# Protobuf/gRPC generation
set(PROTO_PATH "${CMAKE_SOURCE_DIR}/proto")
file(GLOB PROTO_FILES "${PROTO_PATH}/*.proto")

foreach(PROTO_FILE ${PROTO_FILES})
    get_filename_component(PROTO_NAME ${PROTO_FILE} NAME_WE)
    set(PROTO_SRC "${CMAKE_CURRENT_BINARY_DIR}/${PROTO_NAME}.pb.cc")
    set(PROTO_HDR "${CMAKE_CURRENT_BINARY_DIR}/${PROTO_NAME}.pb.h")
    set(GRPC_SRC "${CMAKE_CURRENT_BINARY_DIR}/${PROTO_NAME}.grpc.pb.cc")
    set(GRPC_HDR "${CMAKE_CURRENT_BINARY_DIR}/${PROTO_NAME}.grpc.pb.h")
    
    list(APPEND PROTO_SRCS ${PROTO_SRC})
    list(APPEND PROTO_HDRS ${PROTO_HDR})
    list(APPEND GRPC_SRCS ${GRPC_SRC})
    list(APPEND GRPC_HDRS ${GRPC_HDR})
    
    add_custom_command(
        OUTPUT ${PROTO_SRC} ${PROTO_HDR} ${GRPC_SRC} ${GRPC_HDR}
        COMMAND protobuf::protoc
        ARGS --grpc_out=${CMAKE_CURRENT_BINARY_DIR}
             --cpp_out=${CMAKE_CURRENT_BINARY_DIR}
             --plugin=protoc-gen-grpc=$<TARGET_FILE:gRPC::grpc_cpp_plugin>
             -I${PROTO_PATH}
             ${PROTO_FILE}
        DEPENDS ${PROTO_FILE}
    )
endforeach()

# Include directories
include_directories(
    ${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp
    ${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/infrastructure
    ${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/lib
    ${Protobuf_INCLUDE_DIRS}
    ${CMAKE_CURRENT_BINARY_DIR}
)

# Collect worker sources
file(GLOB_RECURSE WORKER_SOURCES
    "${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/vn/*_W.cpp"
)

# Shared library sources
set(SHARED_LIB_SOURCES
    ${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/lib/onnx/OnnxModelBase.cpp
)

# Optional: Model implementations
set(MODEL_LIB_SOURCES
    # Add when .cpp files are created
    # ${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/lib/models/vietocr_model.cpp
    # ${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/lib/models/paddleocr_model.cpp
    # ${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/lib/models/yolov8_model.cpp
)

# Main worker executable
add_executable(deepapp_worker_main
    ${WORKER_SOURCES}
    ${SHARED_LIB_SOURCES}
    ${MODEL_LIB_SOURCES}
    ${PROTO_SRCS}
    ${PROTO_HDRS}
    ${GRPC_SRCS}
    ${GRPC_HDRS}
    ${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/infrastructure/BaseWorker.cpp
    ${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/infrastructure/WorkerRegistry.cpp
    ${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/infrastructure/GrpcWorkerClient.cpp
    ${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/main.cpp
)

# Link libraries
target_link_libraries(deepapp_worker_main
    PRIVATE
    gRPC::grpc++
    protobuf::libprotobuf
    pthread
    onnxruntime
    opencv_core
    opencv_imgproc
    opencv_imgcodecs
    opencv_highgui
)

# Install target
install(TARGETS deepapp_worker_main
    RUNTIME DESTINATION /app/build
)
```

## Verification

After updating CMakeLists.txt:

```bash
# Clean build
cd /root/deepapp/deepapp_main
rm -rf build
mkdir build && cd build

# Configure
cmake ..

# Check for errors - should see:
# -- Found onnxruntime
# -- Configuring done
# -- Generating done

# Build
make -j$(nproc)

# Verify executable links correctly
ldd deepapp_worker_main | grep onnx
# Should show: libonnxruntime.so => /usr/lib/...
```

## Troubleshooting

### ONNX Runtime Not Found

```bash
# Install ONNX Runtime
apt-get install -y libonnxruntime-dev

# Or download manually:
wget https://github.com/microsoft/onnxruntime/releases/download/v1.16.3/onnxruntime-linux-x64-1.16.3.tgz
tar -xzf onnxruntime-linux-x64-1.16.3.tgz
cp -r onnxruntime-linux-x64-1.16.3/include/* /usr/include/
cp -r onnxruntime-linux-x64-1.16.3/lib/* /usr/lib/
```

### Include Path Issues

```bash
# Verify include structure
ls -la /root/deepapp/deepapp_main/src/main/cpp/com/deepapp/lib/
# Should show: onnx/ utils/ models/

# Test compile with explicit paths
g++ -I/root/deepapp/deepapp_main/src/main/cpp/com/deepapp \
    -c lib/onnx/OnnxModelBase.cpp
```

### Link Errors

```bash
# Check ONNX Runtime location
find /usr -name "libonnxruntime.so*"

# Add to LD_LIBRARY_PATH if needed
export LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu:$LD_LIBRARY_PATH
```

## Docker Update

Update Dockerfile to include ONNX Runtime:

```dockerfile
# In build stage
RUN apt-get update && apt-get install -y \
    cmake \
    g++ \
    libgrpc++-dev \
    libprotobuf-dev \
    protobuf-compiler-grpc \
    libopencv-dev \
    libonnxruntime-dev \  # NEW
    && rm -rf /var/lib/apt/lists/*

# In runtime stage
RUN apt-get update && apt-get install -y \
    libgrpc++1.51 \
    libprotobuf32 \
    libopencv-core4.6 \
    libopencv-imgproc4.6 \
    libopencv-imgcodecs4.6 \
    libonnxruntime1.16.3 \  # NEW
    && rm -rf /var/lib/apt/lists/*
```
