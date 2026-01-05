cmake_minimum_required(VERSION 3.16)
project(DeepappWorkers VERSION 1.0.0)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

# Compiler flags
add_compile_options(-Wall -Wextra -Wpedantic)

# Find gRPC and Protobuf
find_package(PkgConfig REQUIRED)
pkg_check_modules(GRPC REQUIRED grpc++)
pkg_check_modules(PROTOBUF REQUIRED protobuf)

# Find Poppler (PDF processing) and TIFF libraries
pkg_check_modules(POPPLER REQUIRED poppler-cpp)
pkg_check_modules(TIFF REQUIRED libtiff-4)
pkg_check_modules(PNG REQUIRED libpng)

# Find ONNX Runtime for YOLO models
find_path(ONNXRUNTIME_INCLUDE_DIR onnxruntime_cxx_api.h
    PATHS /usr/local/include/onnxruntime /usr/local/include
)

find_library(ONNXRUNTIME_LIBRARY NAMES onnxruntime
    PATHS /usr/local/lib
)

if(NOT ONNXRUNTIME_INCLUDE_DIR OR NOT ONNXRUNTIME_LIBRARY)
    message(FATAL_ERROR "ONNX Runtime not found. Please install ONNX Runtime.")
else()
    message(STATUS "Found ONNX Runtime: ${ONNXRUNTIME_INCLUDE_DIR}, ${ONNXRUNTIME_LIBRARY}")
endif()

# Find OpenCV for image processing
find_package(OpenCV REQUIRED)

# Find SQLite3 for document storage
find_package(SQLite3 REQUIRED)

# Find llama.cpp (optional - for LLM worker)
find_path(LLAMA_INCLUDE_DIR llama.h
    PATHS 
        /usr/local/include
        /usr/include
        /root/llama.cpp/include
        ${CMAKE_SOURCE_DIR}/../llama.cpp/include
)

find_library(LLAMA_LIBRARY NAMES llama
    PATHS 
        /usr/local/lib
        /usr/lib
        /root/llama.cpp/build
        ${CMAKE_SOURCE_DIR}/../llama.cpp/build
)

if(LLAMA_INCLUDE_DIR AND LLAMA_LIBRARY)
    set(LLAMA_FOUND TRUE)
    message(STATUS "Found llama.cpp: ${LLAMA_INCLUDE_DIR}, ${LLAMA_LIBRARY}")
else()
    set(LLAMA_FOUND FALSE)
    message(STATUS "llama.cpp not found - AAA0_0300 C++ worker will be skipped")
    message(STATUS "To install llama.cpp:")
    message(STATUS "  git clone https://github.com/ggerganov/llama.cpp /root/llama.cpp")
    message(STATUS "  cd /root/llama.cpp && mkdir build && cd build")
    message(STATUS "  cmake .. && make -j")
endif()

# Include directories
include_directories(${GRPC_INCLUDE_DIRS})
include_directories(${PROTOBUF_INCLUDE_DIRS})
include_directories(${POPPLER_INCLUDE_DIRS})
include_directories(${TIFF_INCLUDE_DIRS})
include_directories(${PNG_INCLUDE_DIRS})
include_directories(${SQLite3_INCLUDE_DIRS})
include_directories(${OpenCV_INCLUDE_DIRS})
include_directories(${CMAKE_SOURCE_DIR}/src/main/cpp)
include_directories(${ONNXRUNTIME_INCLUDE_DIR})

if(LLAMA_FOUND)
    include_directories(${LLAMA_INCLUDE_DIR})
    include_directories(/root/llama.cpp/ggml/include)
    include_directories(/root/llama.cpp/common)
endif()

# Generate protobuf files from proto
find_program(PROTOC protoc)
find_program(GRPC_CPP_PLUGIN grpc_cpp_plugin)

set(PROTO_FILES ${CMAKE_SOURCE_DIR}/src/main/resources/proto/hub.proto)
set(PROTO_SRC_DIR ${CMAKE_BINARY_DIR}/proto-generated)
file(MAKE_DIRECTORY ${PROTO_SRC_DIR})

execute_process(
    COMMAND ${PROTOC} 
        --proto_path=${CMAKE_SOURCE_DIR}/src/main/resources/proto 
        --cpp_out=${PROTO_SRC_DIR} 
        --grpc_out=${PROTO_SRC_DIR} 
        --plugin=protoc-gen-grpc=${GRPC_CPP_PLUGIN} 
        ${PROTO_FILES}
    WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}
    RESULT_VARIABLE PROTOC_RESULT
)

if(NOT PROTOC_RESULT EQUAL 0)
    message(FATAL_ERROR "Protobuf generation failed")
endif()

file(GLOB PROTO_SRCS "${PROTO_SRC_DIR}/*.pb.cc")
file(GLOB GRPC_SRCS "${PROTO_SRC_DIR}/*.grpc.pb.cc")

include_directories(${PROTO_SRC_DIR})

# Collect all worker files automatically
file(GLOB_RECURSE WORKER_SOURCES 
    "${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/vn/io/AA/A0/AAA0_0100/worker/AAA0_0100_W.cpp"
    "${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/vn/io/AA/A0/AAA0_0101/worker/AAA0_0101_W.cpp"
    "${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/vn/io/AA/A0/AAA0_0200/worker/AAA0_0200_W.cpp"
    "${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/vn/io/ZZ/A0/ZZA0_0100/worker/ZZA0_0100_W.cpp"
    "${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/vn/io/ZZ/A0/ZZA0_0102/worker/ZZA0_0102_W.cpp"
)

# Conditionally add AAA0_0300 C++ worker if llama.cpp is available
# TEMPORARILY DISABLED: llama.cpp API has changed, C++ worker needs update
# Python worker (AAA0_0300_W.py) is fully functional and tested
if(FALSE)  # Disabled - uncomment when llama.cpp C++ code is updated
    if(LLAMA_FOUND)
        list(APPEND WORKER_SOURCES 
            "${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/vn/io/AA/A0/AAA0_0300/worker/AAA0_0300_CPP_W.cpp"
        )
        message(STATUS "✓ AAA0_0300 C++ LLM worker will be included")
    else()
        message(STATUS "⊗ AAA0_0300 C++ LLM worker will be skipped (llama.cpp not found)")
    endif()
endif()
message(STATUS "⊗ AAA0_0300 C++ LLM worker skipped (Python worker available)")

# Alternative: Auto-discover all *_W.cpp files (excluding AAA0_0300 if llama not found)
file(GLOB_RECURSE WORKER_SOURCES_AUTO
    "${CMAKE_SOURCE_DIR}/src/main/cpp/*_W.cpp"
)
if(NOT LLAMA_FOUND)
    # Remove AAA0_0300_CPP_W.cpp from auto-discovered list
    list(FILTER WORKER_SOURCES_AUTO EXCLUDE REGEX "AAA0_0300_CPP_W.cpp")
endif()
if(WORKER_SOURCES_AUTO)
    set(WORKER_SOURCES ${WORKER_SOURCES_AUTO})
endif()

# Print found workers
list(LENGTH WORKER_SOURCES WORKER_COUNT)
message(STATUS "Found ${WORKER_COUNT} worker(s)")

# Core infrastructure sources
set(INFRASTRUCTURE_SOURCES
    ${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/infrastructure/BaseWorker.cpp
    ${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/infrastructure/WorkerRegistry.cpp
    ${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/infrastructure/GrpcWorkerClient.cpp
    ${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/document/DocumentProcessor.cpp
    ${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/storage/DocumentStorage.cpp
    ${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/utils/PathUtils.cpp
)

# Main application
add_executable(deepapp_worker_main
    ${CMAKE_SOURCE_DIR}/src/main/cpp/com/DeepappMainApplication.cpp
    ${INFRASTRUCTURE_SOURCES}
    ${WORKER_SOURCES}
    ${PROTO_SRCS}
    ${GRPC_SRCS}
)

# Link libraries
target_link_libraries(deepapp_worker_main
    ${GRPC_LIBRARIES}
    ${PROTOBUF_LIBRARIES}
    ${POPPLER_LIBRARIES}
    ${TIFF_LIBRARIES}
    ${PNG_LIBRARIES}
    ${OpenCV_LIBRARIES}
    ${ONNXRUNTIME_LIBRARY}
    SQLite::SQLite3
    pthread
)

# Link llama.cpp if found
if(LLAMA_FOUND)
    target_link_libraries(deepapp_worker_main ${LLAMA_LIBRARY})
endif()

# Installation
install(TARGETS deepapp_worker_main DESTINATION bin)

message(STATUS "===========================================")
message(STATUS "Workers found: ${WORKER_SOURCES}")
message(STATUS "===========================================")
