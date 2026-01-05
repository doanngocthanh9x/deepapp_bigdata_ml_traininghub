cmake_minimum_required(VERSION 3.14)

# AAA0_0300 C++ Worker - LLM Inference

# Find llama.cpp
find_package(PkgConfig)
pkg_check_modules(LLAMA REQUIRED llama)

# Add worker source
set(AAA0_0300_SOURCES
    com/deepapp/vn/io/AA/A0/AAA0_0300/worker/AAA0_0300_CPP_W.cpp
)

# Create worker library
add_library(aaa0_0300_worker SHARED ${AAA0_0300_SOURCES})

target_include_directories(aaa0_0300_worker PRIVATE
    ${CMAKE_SOURCE_DIR}/src/main/cpp
    ${LLAMA_INCLUDE_DIRS}
)

target_link_libraries(aaa0_0300_worker
    deepapp_infrastructure
    ${LLAMA_LIBRARIES}
    nlohmann_json::nlohmann_json
)

# Install
install(TARGETS aaa0_0300_worker
    LIBRARY DESTINATION lib
    ARCHIVE DESTINATION lib
)
