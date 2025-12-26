# Multi-stage Dockerfile for Java Spring Boot + C++ gRPC Workers
# Stage 1: Build C++ Workers
FROM ubuntu:24.04 AS cpp-builder

# Install C++ build dependencies
RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --fix-missing \
    build-essential \
    cmake \
    libgrpc++-dev \
    libprotobuf-dev \
    protobuf-compiler-grpc \
    git \
    pkg-config \
    curl \
    libssl-dev \
    nlohmann-json3-dev \
    libpoppler-cpp-dev \
    libpoppler-cpp0t64 \
    libtiff-dev \
    libpng-dev \
    libopencv-dev \
    libsqlite3-dev \
    wget \
    && rm -rf /var/lib/apt/lists/*\
    && ldconfig

# Install ONNX Runtime
ARG ONNX_VERSION=1.15.1
RUN wget -q "https://github.com/microsoft/onnxruntime/releases/download/v${ONNX_VERSION}/onnxruntime-linux-x64-${ONNX_VERSION}.tgz" \
    && tar -xzf "onnxruntime-linux-x64-${ONNX_VERSION}.tgz" \
    && mkdir -p /usr/local/onnxruntime \
    && cp -r onnxruntime-linux-x64-${ONNX_VERSION}/* /usr/local/onnxruntime/ \
    && cp /usr/local/onnxruntime/lib/libonnxruntime.so.${ONNX_VERSION} /usr/local/lib/ \
    && ln -sf /usr/local/lib/libonnxruntime.so.${ONNX_VERSION} /usr/local/lib/libonnxruntime.so \
    && ln -sf /usr/local/lib/libonnxruntime.so.${ONNX_VERSION} /usr/local/lib/libonnxruntime.so.1 \
    && mkdir -p /usr/local/include/onnxruntime \
    && cp -r /usr/local/onnxruntime/include/* /usr/local/include/onnxruntime/ \
    && ldconfig \
    && rm -rf onnxruntime-linux-x64-${ONNX_VERSION} \
    && rm -f onnxruntime-linux-x64-${ONNX_VERSION}.tgz 
   
# Set working directory
WORKDIR /app

# Copy sources
COPY src ./src
# COPY src/main/cpp ./src/main/cpp
# COPY src/main/resources/proto ./src/main/resources/proto

COPY CMakeLists.txt ./

# Build C++ workers
RUN mkdir -p build && cd build && \
    cmake .. && \
    make -j$(nproc)
# Stage 2: Build Java Application
FROM maven:3.9-eclipse-temurin-17 AS java-builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests
RUN cp target/*.jar ./app.jar
# Stage 3: Runtime
FROM eclipse-temurin:17-jre
# Install Python runtime
RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y \
    python3 \
    python3-pip \
    python3-venv \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY src ./src
RUN python3 -m venv /opt/venv \
    && /opt/venv/bin/pip install --upgrade pip \
    && /opt/venv/bin/pip install --no-cache-dir -r /app/src/main/python/requirements.txt
# Install C++ runtime dependencies and curl for healthcheck
ENV PATH="/opt/venv/bin:$PATH"
RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --fix-missing \
    build-essential \
    cmake \
    libgrpc++-dev \
    libprotobuf-dev \
    protobuf-compiler-grpc \
    git \
    pkg-config \
    curl \
    libssl-dev \
    nlohmann-json3-dev \
    libpoppler-cpp-dev \
    libpoppler-cpp0t64 \
    libtiff-dev \
    libpng-dev \
    libopencv-dev \
    libsqlite3-dev \
    wget \
    && rm -rf /var/lib/apt/lists/*\
    && ldconfig

WORKDIR /app

# Copy ONNX Runtime from build stage
COPY --from=cpp-builder /usr/local/lib/libonnxruntime.so* /usr/local/lib/
COPY --from=cpp-builder /usr/local/include/onnxruntime /usr/local/include/onnxruntime
# 🔴 FIX QUAN TRỌNG
RUN echo "/usr/local/lib" > /etc/ld.so.conf.d/onnxruntime.conf \
    && ldconfig
# Copy C++ worker binary
COPY --from=cpp-builder /app/build/deepapp_worker_main ./build/

# Copy Java application
COPY --from=java-builder /app/app.jar ./app.jar

# Config files
COPY src/main/resources/application.yml ./config/application.yml
COPY src/main/resources/application-docker.yml ./config/application-docker.yml

# Ensure linker can find libs
ENV LD_LIBRARY_PATH="/usr/local/lib:/usr/lib/x86_64-linux-gnu"

# Logs & temp
RUN mkdir -p /app/logs /tmp/deepapp

EXPOSE 8080

ENV JAVA_OPTS="-Xms512m -Xmx2g" \
    GRPC_HOST="72.60.111.138" \
    GRPC_PORT="50051" \
    CPP_CLIENT_ID="cpp-worker" \
    SPRING_PROFILES_ACTIVE="docker"

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

