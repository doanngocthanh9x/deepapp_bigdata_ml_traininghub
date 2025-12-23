#!/bin/bash

# Start both Java and C++ applications

echo "========================================"
echo "Starting DeepApp (Java + C++ Workers)"
echo "========================================"

# Configuration
SERVER_ADDRESS="72.60.111.138:50051"
CPP_CLIENT_ID="cpp-worker"
JAVA_PORT=8080

# Function to cleanup on exit
cleanup() {
    echo ""
    echo "Shutting down..."
    kill $JAVA_PID 2>/dev/null
    kill $CPP_PID 2>/dev/null
    exit 0
}

trap cleanup SIGINT SIGTERM

# Check if C++ worker is built
if [ ! -f "/root/deepapp/deepapp_main/build/deepapp_worker_main" ]; then
    echo "⚠️  C++ worker not found. Building..."
    /root/deepapp/deepapp_main/build_cpp_workers.sh || {
        echo "❌ Failed to build C++ workers"
        exit 1
    }
fi

cd /root/deepapp/deepapp_main

# Start Java application in background
echo ""
echo "🚀 Starting Java Application (port $JAVA_PORT)..."
echo "========================================"
mvn spring-boot:run &
JAVA_PID=$!

# Wait a bit for Java to start
sleep 5

# Start C++ workers in background
echo ""
echo "🚀 Starting C++ Workers..."
echo "========================================"
./build/deepapp_worker_main $SERVER_ADDRESS $CPP_CLIENT_ID &
CPP_PID=$!

echo ""
echo "✅ Both applications started!"
echo ""
echo "Java PID: $JAVA_PID (port $JAVA_PORT)"
echo "C++ PID:  $CPP_PID"
echo ""
echo "Press Ctrl+C to stop both applications"
echo ""

# Wait for both processes
wait
