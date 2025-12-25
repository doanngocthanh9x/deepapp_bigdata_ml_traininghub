/**
 * Base ONNX Model Wrapper
 * 
 * Shared utility class for loading and running ONNX models
 * Can be used for VietOCR, PaddleOCR, YOLO, and other ONNX models
 * 
 * Features:
 * - Lazy loading
 * - Session management
 * - Input/Output tensor handling
 * - Thread-safe operations
 */

#pragma once

#include <onnxruntime_cxx_api.h>
#include <string>
#include <vector>
#include <memory>
#include <mutex>
#include <iostream>

namespace deepapp {
namespace lib {
namespace onnx {

class OnnxModelBase {
public:
    /**
     * Constructor
     * @param model_path Path to .onnx model file
     * @param num_threads Number of threads for inference (default: 4)
     */
    OnnxModelBase(const std::string& model_path, int num_threads = 4);
    
    virtual ~OnnxModelBase() = default;

    /**
     * Check if model is loaded
     */
    bool isLoaded() const { return session_ != nullptr; }

    /**
     * Get model input names
     */
    std::vector<std::string> getInputNames() const;

    /**
     * Get model output names
     */
    std::vector<std::string> getOutputNames() const;

    /**
     * Get model input shape
     * @param input_index Index of input (default: 0)
     */
    std::vector<int64_t> getInputShape(size_t input_index = 0) const;

    /**
     * Get model output shape
     * @param output_index Index of output (default: 0)
     */
    std::vector<int64_t> getOutputShape(size_t output_index = 0) const;

protected:
    /**
     * ONNX Runtime environment and session
     */
    Ort::Env env_;
    Ort::SessionOptions session_options_;
    std::unique_ptr<Ort::Session> session_;
    
    /**
     * Model metadata
     */
    std::string model_path_;
    std::vector<std::string> input_names_;
    std::vector<std::string> output_names_;
    std::vector<std::vector<int64_t>> input_shapes_;
    std::vector<std::vector<int64_t>> output_shapes_;
    
    /**
     * Thread safety
     */
    mutable std::mutex session_mutex_;

    /**
     * Helper: Create input tensor
     */
    Ort::Value createTensor(
        const std::vector<float>& data,
        const std::vector<int64_t>& shape
    );

    /**
     * Helper: Extract output tensor data
     */
    std::vector<float> extractTensorData(Ort::Value& tensor);

    /**
     * Helper: Run inference
     */
    std::vector<Ort::Value> runInference(
        const std::vector<Ort::Value>& input_tensors
    );

    /**
     * Helper: Load model metadata
     */
    void loadMetadata();

    /**
     * Helper: Print model info
     */
    void printModelInfo() const;
};

} // namespace onnx
} // namespace lib
} // namespace deepapp
