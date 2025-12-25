#!/bin/bash

# Python Worker Launcher Script
# Similar to how C++ worker is launched

set -e

# Configuration
PYTHON_PATH="${PYTHON_PATH:-python3}"
MAIN_SCRIPT="src/main/python/com/deepapp/vn/io/main.py"
CLIENT_ID="${CLIENT_ID:-python-worker}"
GRPC_HOST="${GRPC_HOST:-localhost}"
GRPC_PORT="${GRPC_PORT:-50052}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')] [INFO] $1${NC}"
}

log_error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] [ERROR] $1${NC}"
}

log_warn() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] [WARN] $1${NC}"
}

# Check if Python is available
check_python() {
    if ! command -v "$PYTHON_PATH" &> /dev/null; then
        log_error "Python not found at: $PYTHON_PATH"
        log_error "Please install Python 3.8+ or set PYTHON_PATH environment variable"
        exit 1
    fi

    PYTHON_VERSION=$("$PYTHON_PATH" --version 2>&1 | grep -oP 'Python \K[0-9]+\.[0-9]+')
    log_info "Using Python $PYTHON_VERSION at: $PYTHON_PATH"
}

# Check if main script exists
check_script() {
    if [ ! -f "$MAIN_SCRIPT" ]; then
        log_error "Main script not found: $MAIN_SCRIPT"
        exit 1
    fi
    log_info "Found main script: $MAIN_SCRIPT"
}

# Set Python path
setup_python_path() {
    export PYTHONPATH="/root/deepapp/deepapp_main/src/main/python:$PYTHONPATH"
    log_info "Set PYTHONPATH to: $PYTHONPATH"
}

# Main function
main() {
    log_info "========================================"
    log_info "DeepApp Python Worker Launcher"
    log_info "========================================"
    log_info "Configuration:"
    log_info "  Python Path: $PYTHON_PATH"
    log_info "  Main Script: $MAIN_SCRIPT"
    log_info "  Client ID: $CLIENT_ID"
    log_info "  gRPC Host: $GRPC_HOST"
    log_info "  gRPC Port: $GRPC_PORT"
    log_info ""

    # Pre-flight checks
    check_python
    check_script
    setup_python_path

    # Launch Python worker
    log_info "Starting Python worker..."
    exec "$PYTHON_PATH" "$MAIN_SCRIPT" \
        --client-id "$CLIENT_ID" \
        --host "$GRPC_HOST" \
        --port "$GRPC_PORT"
}

# Run main function
main "$@"