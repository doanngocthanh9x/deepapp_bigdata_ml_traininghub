#!/bin/bash

#############################################
# Download Models from GitLab Repository
#############################################

set -e

# Configuration
GITLAB_REPO="https://gitlab.com/dnt.doanngocthanh/deepappmodels"
BRANCH="main"
MODELS_DIR="/app/models"
GITLAB_TOKEN="${GITLAB_TOKEN:-}"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

download_file() {
    local file_path="$1"
    local local_path="${MODELS_DIR}/${file_path}"
    local url="${GITLAB_REPO}/-/raw/${BRANCH}/${file_path}"
    
    # Create directory
    mkdir -p "$(dirname "$local_path")"
    
    # Check if file exists
    if [ -f "$local_path" ]; then
        log_info "File exists: $local_path"
        return 0
    fi
    
    log_info "Downloading: $file_path"
    
    # Build curl command
    CURL_CMD="curl -L -f -s"
    
    if [ -n "$GITLAB_TOKEN" ]; then
        CURL_CMD="$CURL_CMD -H \"PRIVATE-TOKEN: $GITLAB_TOKEN\""
    fi
    
    CURL_CMD="$CURL_CMD -o \"$local_path\" \"$url\""
    
    # Download
    if eval $CURL_CMD; then
        log_info "Downloaded: $local_path"
        return 0
    else
        log_error "Failed to download: $file_path"
        return 1
    fi
}

download_model_set() {
    local model_name="$1"
    shift
    local files=("$@")
    
    log_info "=== Downloading $model_name ==="
    
    local success=true
    for file in "${files[@]}"; do
        if ! download_file "$file"; then
            success=false
        fi
    done
    
    if $success; then
        log_info "$model_name downloaded successfully"
    else
        log_error "$model_name download failed"
        return 1
    fi
}

# Parse arguments
MODEL_TYPE="${1:-all}"

case "$MODEL_TYPE" in
    vietocr)
        log_info "Downloading VietOCR models..."
        download_model_set "VietOCR" \
            "vietocr/transformer_encoder.onnx" \
            "vietocr/transformer_decoder.onnx" \
            "vietocr/vocab.txt"
        ;;
    
    paddleocr)
        log_info "Downloading PaddleOCR models..."
        download_model_set "PaddleOCR" \
            "paddleocr/ch_PP-OCRv4_det_infer.onnx" \
            "paddleocr/ch_PP-OCRv4_rec_infer.onnx" \
            "paddleocr/ppocr_keys_v1.txt"
        ;;
    
    yolo)
        log_info "Downloading YOLO models..."
        download_model_set "YOLO" \
            "yolo/yolov8n.onnx" \
            "yolo/yolov8s.onnx" \
            "yolo/coco_classes.txt"
        ;;
    
    all)
        log_info "Downloading all models..."
        
        # VietOCR
        download_model_set "VietOCR" \
            "vietocr/transformer_encoder.onnx" \
            "vietocr/transformer_decoder.onnx" \
            "vietocr/vocab.txt" || true
        
        # PaddleOCR
        download_model_set "PaddleOCR" \
            "paddleocr/ch_PP-OCRv4_det_infer.onnx" \
            "paddleocr/ch_PP-OCRv4_rec_infer.onnx" \
            "paddleocr/ppocr_keys_v1.txt" || true
        
        # YOLO
        download_model_set "YOLO" \
            "yolo/yolov8n.onnx" \
            "yolo/yolov8s.onnx" \
            "yolo/coco_classes.txt" || true
        ;;
    
    list)
        log_info "Available models in $MODELS_DIR:"
        if [ -d "$MODELS_DIR" ]; then
            find "$MODELS_DIR" -type f | sort
        else
            log_warn "Models directory does not exist"
        fi
        exit 0
        ;;
    
    clean)
        log_warn "Cleaning models directory..."
        rm -rf "$MODELS_DIR"
        mkdir -p "$MODELS_DIR"
        log_info "Models cleaned"
        exit 0
        ;;
    
    *)
        echo "Usage: $0 {vietocr|paddleocr|yolo|all|list|clean}"
        echo ""
        echo "Examples:"
        echo "  $0 vietocr          # Download VietOCR models only"
        echo "  $0 all              # Download all models"
        echo "  $0 list             # List downloaded models"
        echo "  $0 clean            # Clean models directory"
        echo ""
        echo "Environment variables:"
        echo "  GITLAB_TOKEN        # GitLab access token (for private repos)"
        echo "  MODELS_DIR          # Models directory (default: /app/models)"
        exit 1
        ;;
esac

log_info "=== Download Summary ==="
if [ -d "$MODELS_DIR" ]; then
    TOTAL_SIZE=$(du -sh "$MODELS_DIR" | cut -f1)
    FILE_COUNT=$(find "$MODELS_DIR" -type f | wc -l)
    log_info "Total models: $FILE_COUNT files ($TOTAL_SIZE)"
else
    log_warn "No models downloaded"
fi

log_info "Done!"
