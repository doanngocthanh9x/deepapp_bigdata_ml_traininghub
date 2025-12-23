# Multi-stage Dockerfile for Java Spring Boot + C++ gRPC Workers
# Stage 1: Build C++ Workers
FROM ubuntu:24.04 AS cpp-builder

# Install C++ build dependencies with retry on network errors
RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --fix-missing \
    build-essential \
    cmake \
    libgrpc++-dev \
    libprotobuf-dev \
    protobuf-compiler-grpc \
    git \
    pkg-config \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy all C++ files (Docker will cache this entire stage if unchanged)
COPY src/main/cpp ./src/main/cpp
COPY src/main/resources/proto ./src/main/resources/proto
COPY CMakeLists.txt ./

# Build C++ workers
RUN mkdir -p build && cd build && \
    cmake .. && \
    make -j$(nproc)

# Stage 2: Build Java Application (use local build)
FROM maven:3.9-eclipse-temurin-17 AS java-builder

WORKDIR /app

# Copy already built jar from host
# Build locally first: mvn clean package -DskipTests
COPY target/*.jar ./app.jar

# Stage 3: Runtime
FROM eclipse-temurin:17-jre

# Install C++ runtime dependencies and curl for healthcheck
RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y \
    libgrpc++1.51t64 \
    libgrpc29t64 \
    libprotobuf32t64 \
    curl \
    && rm -rf /var/lib/apt/lists/* \
    && ln -s /usr/lib/x86_64-linux-gnu/libgrpc++.so.1.51 /usr/lib/x86_64-linux-gnu/libgrpc++.so.1 \
    && ln -s /usr/lib/x86_64-linux-gnu/libgrpc.so.29 /usr/lib/x86_64-linux-gnu/libgrpc.so.1 \
    && ldconfig

WORKDIR /app

# Copy C++ worker binary from builder
COPY --from=cpp-builder /app/build/deepapp_worker_main ./build/

# Copy Java application from builder
COPY --from=java-builder /app/app.jar ./app.jar

# Copy application configuration
COPY src/main/resources/application.yml ./config/application.yml
COPY src/main/resources/application-docker.yml ./config/application-docker.yml

# Create logs directory
RUN mkdir -p /app/logs

# Expose Spring Boot port
EXPOSE 8080

# Environment variables
ENV JAVA_OPTS="-Xms512m -Xmx2g" \
    GRPC_HOST="72.60.111.138" \
    GRPC_PORT="50051" \
    CPP_CLIENT_ID="cpp-worker" \
    SPRING_PROFILES_ACTIVE="docker"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Start Java application (will auto-start C++ worker)
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
