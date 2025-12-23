cmake_minimum_required(VERSION 3.16)
project(grpc_test_client)

set(CMAKE_CXX_STANDARD 17)

# Find gRPC and protobuf
find_package(Protobuf REQUIRED)
find_package(gRPC REQUIRED)

# Generate protobuf files
#path proto /root/deepapp/deepapp_main/src/main/resources/proto/hub.proto
#path hiện tại file CMake đang đứng  /root/deepapp/deepapp_main/src/main/cpp/com/deepapp/vn/io/CMakeLists.txt
set(PROTO_FILES ${CMAKE_SOURCE_DIR}/../../../../resources/proto/hub.proto)
set(PROTO_SRC ${CMAKE_BINARY_DIR}/hub.pb.cc)
set(PROTO_HDR ${CMAKE_BINARY_DIR}/hub.pb.h)
set(GRPC_SRC ${CMAKE_BINARY_DIR}/hub.grpc.pb.cc)
set(GRPC_HDR ${CMAKE_BINARY_DIR}/hub.grpc.pb.h)

add_custom_command(
    OUTPUT ${PROTO_SRC} ${PROTO_HDR} ${GRPC_SRC} ${GRPC_HDR}
    COMMAND protobuf::protoc
    ARGS --grpc_out=${CMAKE_BINARY_DIR}
         --cpp_out=${CMAKE_BINARY_DIR}
         --plugin=protoc-gen-grpc=$<TARGET_FILE:gRPC::grpc_cpp_plugin>
         ${PROTO_FILES}
    DEPENDS ${PROTO_FILES}
)

# Create executable
add_executable(grpc_test_client
    main.cpp
    ${PROTO_SRC}
    ${GRPC_SRC}
)

target_include_directories(grpc_test_client PRIVATE
    ${CMAKE_BINARY_DIR}
    ${Protobuf_INCLUDE_DIRS}
    ${gRPC_INCLUDE_DIRS}
)

target_link_libraries(grpc_test_client
    gRPC::grpc++
    protobuf::libprotobuf
)