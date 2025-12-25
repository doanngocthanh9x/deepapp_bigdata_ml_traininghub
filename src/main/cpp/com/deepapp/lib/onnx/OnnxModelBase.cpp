#include "OnnxModelBase.hpp"
#include <stdexcept>

namespace deepapp {
namespace lib {
namespace onnx {

OnnxModelBase::OnnxModelBase(const std::string& model_path, int num_threads)
    : env_(ORT_LOGGING_LEVEL_WARNING, "DeepappOnnx"),
      model_path_(model_path) {
    
    std::cout << "[OnnxModelBase] Loading model: " << model_path << std::endl;
    
    // Configure session options
    session_options_.SetIntraOpNumThreads(num_threads);
    session_options_.SetGraphOptimizationLevel(
        GraphOptimizationLevel::ORT_ENABLE_ALL
    );
    
    try {
        // Load model
        session_ = std::make_unique<Ort::Session>(
            env_, model_path.c_str(), session_options_
        );
        
        // Load metadata
        loadMetadata();
        
        std::cout << "[OnnxModelBase] Model loaded successfully" << std::endl;
        printModelInfo();
        
    } catch (const Ort::Exception& e) {
        throw std::runtime_error(
            "Failed to load ONNX model: " + std::string(e.what())
        );
    }
}

void OnnxModelBase::loadMetadata() {
    if (!session_) return;
    
    Ort::AllocatorWithDefaultOptions allocator;
    
    // Get input names and shapes
    size_t num_inputs = session_->GetInputCount();
    input_names_.reserve(num_inputs);
    input_shapes_.reserve(num_inputs);
    
    for (size_t i = 0; i < num_inputs; i++) {
        // Get input name
        auto input_name = session_->GetInputNameAllocated(i, allocator);
        input_names_.push_back(input_name.get());
        
        // Get input shape
        auto input_info = session_->GetInputTypeInfo(i);
        auto tensor_info = input_info.GetTensorTypeAndShapeInfo();
        input_shapes_.push_back(tensor_info.GetShape());
    }
    
    // Get output names and shapes
    size_t num_outputs = session_->GetOutputCount();
    output_names_.reserve(num_outputs);
    output_shapes_.reserve(num_outputs);
    
    for (size_t i = 0; i < num_outputs; i++) {
        // Get output name
        auto output_name = session_->GetOutputNameAllocated(i, allocator);
        output_names_.push_back(output_name.get());
        
        // Get output shape
        auto output_info = session_->GetOutputTypeInfo(i);
        auto tensor_info = output_info.GetTensorTypeAndShapeInfo();
        output_shapes_.push_back(tensor_info.GetShape());
    }
}

std::vector<std::string> OnnxModelBase::getInputNames() const {
    return input_names_;
}

std::vector<std::string> OnnxModelBase::getOutputNames() const {
    return output_names_;
}

std::vector<int64_t> OnnxModelBase::getInputShape(size_t input_index) const {
    if (input_index >= input_shapes_.size()) {
        throw std::out_of_range("Input index out of range");
    }
    return input_shapes_[input_index];
}

std::vector<int64_t> OnnxModelBase::getOutputShape(size_t output_index) const {
    if (output_index >= output_shapes_.size()) {
        throw std::out_of_range("Output index out of range");
    }
    return output_shapes_[output_index];
}

Ort::Value OnnxModelBase::createTensor(
    const std::vector<float>& data,
    const std::vector<int64_t>& shape) {
    
    auto memory_info = Ort::MemoryInfo::CreateCpu(
        OrtArenaAllocator, OrtMemTypeDefault
    );
    
    return Ort::Value::CreateTensor<float>(
        memory_info,
        const_cast<float*>(data.data()),
        data.size(),
        shape.data(),
        shape.size()
    );
}

std::vector<float> OnnxModelBase::extractTensorData(Ort::Value& tensor) {
    float* tensor_data = tensor.GetTensorMutableData<float>();
    auto shape = tensor.GetTensorTypeAndShapeInfo().GetShape();
    
    size_t tensor_size = 1;
    for (auto dim : shape) {
        tensor_size *= dim;
    }
    
    return std::vector<float>(tensor_data, tensor_data + tensor_size);
}

std::vector<Ort::Value> OnnxModelBase::runInference(
    const std::vector<Ort::Value>& input_tensors) {
    
    std::lock_guard<std::mutex> lock(session_mutex_);
    
    if (!session_) {
        throw std::runtime_error("Model not loaded");
    }
    
    // Prepare input/output names as const char*
    std::vector<const char*> input_names_cstr;
    std::vector<const char*> output_names_cstr;
    
    for (const auto& name : input_names_) {
        input_names_cstr.push_back(name.c_str());
    }
    
    for (const auto& name : output_names_) {
        output_names_cstr.push_back(name.c_str());
    }
    
    // Run inference
    return session_->Run(
        Ort::RunOptions{nullptr},
        input_names_cstr.data(),
        input_tensors.data(),
        input_tensors.size(),
        output_names_cstr.data(),
        output_names_cstr.size()
    );
}

void OnnxModelBase::printModelInfo() const {
    std::cout << "  Inputs (" << input_names_.size() << "):" << std::endl;
    for (size_t i = 0; i < input_names_.size(); i++) {
        std::cout << "    [" << i << "] " << input_names_[i] << " - Shape: [";
        for (size_t j = 0; j < input_shapes_[i].size(); j++) {
            std::cout << input_shapes_[i][j];
            if (j < input_shapes_[i].size() - 1) std::cout << ", ";
        }
        std::cout << "]" << std::endl;
    }
    
    std::cout << "  Outputs (" << output_names_.size() << "):" << std::endl;
    for (size_t i = 0; i < output_names_.size(); i++) {
        std::cout << "    [" << i << "] " << output_names_[i] << " - Shape: [";
        for (size_t j = 0; j < output_shapes_[i].size(); j++) {
            std::cout << output_shapes_[i][j];
            if (j < output_shapes_[i].size() - 1) std::cout << ", ";
        }
        std::cout << "]" << std::endl;
    }
}

} // namespace onnx
} // namespace lib
} // namespace deepapp
