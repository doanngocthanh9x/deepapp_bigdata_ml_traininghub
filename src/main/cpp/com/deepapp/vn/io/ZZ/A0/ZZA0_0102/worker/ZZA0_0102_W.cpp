#include "com/deepapp/infrastructure/BaseWorker.h"
#include "com/deepapp/infrastructure/WorkerRegistry.h"
#include "com/deepapp/infrastructure/GrpcWorkerClient.h"
#include "com/deepapp/infrastructure/FileHasher.h"
#include <nlohmann/json.hpp>
#include <iostream>
#include <fstream>
#include <string>
#include <vector>
#include <memory>
#include <opencv2/opencv.hpp>
#include <onnxruntime_cxx_api.h>

using json = nlohmann::json;

namespace deepapp {
namespace workers {

/**
 * ZZA0_0102_Worker - YOLO Object Detection Worker
 * Handles YOLO object detection using ONNX models
 */
class ZZA0_0102_Worker : public infrastructure::BaseWorker {
public:
    ZZA0_0102_Worker() : infrastructure::BaseWorker("ZZA0_0102_Worker") {
        std::cout << "[ZZA0_0102_Worker] YOLO Detection Worker Initialized" << std::endl;
        std::cout << "[ZZA0_0102_Worker] Supported models: giay_ra_vien" << std::endl;

        // Initialize ONNX Runtime
        initializeOnnxRuntime();
    }

    ~ZZA0_0102_Worker() {
        cleanupOnnxRuntime();
    }

    std::string processTask(const std::string& event_type, const std::string& payload) override {
        std::cout << "[ZZA0_0102_Worker] Processing YOLO detection task:" << std::endl;
        std::cout << "  Event Type: " << event_type << std::endl;
        std::cout << "  Payload size: " << payload.size() << " bytes" << std::endl;

        try {
            json response;
            response["worker"] = "ZZA0_0102_W";
            response["timestamp"] = std::time(nullptr);

            // Handle different event types
            if (event_type == "detect") {
                return detectObjects(payload);
            } else if (event_type == "test") {
                return testDetection(payload);
            } else if (event_type == "echo") {
                response["status"] = "success";
                response["data"] = "Echo: " + payload;
                return response.dump();
            } else if (event_type == "health_check") {
                response["status"] = "success";
                response["message"] = "YOLO worker is healthy";
                return response.dump();
            } else {
                response["status"] = "error";
                response["error"] = "Unknown event type: " + event_type;
                return response.dump();
            }

        } catch (const std::exception& e) {
            json error_response;
            error_response["worker"] = "ZZA0_0102_W";
            error_response["status"] = "error";
            error_response["error"] = std::string("Exception: ") + e.what();
            error_response["timestamp"] = std::time(nullptr);
            return error_response.dump();
        }
    }

private:
    // ONNX Runtime components
    Ort::Env* env_ = nullptr;
    Ort::Session* session_ = nullptr;
    std::vector<std::string> input_names_;
    std::vector<std::string> output_names_;
    std::vector<std::vector<int64_t>> input_shapes_;
    std::vector<std::vector<int64_t>> output_shapes_;
    std::vector<std::string> class_names_;  // Class names loaded from model

    /**
     * Load class names from ONNX model metadata
     */
    void loadClassNamesFromModel() {
        try {
            Ort::AllocatorWithDefaultOptions allocator;
            auto model_metadata = session_->GetModelMetadata();

            // Try to get class names from metadata
            std::string names_str;
            auto result = model_metadata.LookupCustomMetadataMapAllocated("names", allocator);
            if (result) {
                names_str = result.get();
                std::cout << "[ZZA0_0102_Worker] Found class names in metadata: " << names_str << std::endl;
            }

            if (!names_str.empty()) {
                // Parse JSON-like string, Python dict format, or comma-separated values
                if (names_str.find('{') != std::string::npos) {
                    // Try JSON format first: {"0": "class1", "1": "class2", ...}
                    try {
                        json names_json = json::parse(names_str);
                        class_names_.clear();
                        for (auto& item : names_json.items()) {
                            int idx = std::stoi(item.key());
                            if (idx >= static_cast<int>(class_names_.size())) {
                                class_names_.resize(idx + 1);
                            }
                            class_names_[idx] = item.value();
                        }
                    } catch (...) {
                        // Try Python dict format: {0: 'class1', 1: 'class2', ...}
                        try {
                            class_names_.clear();
                            std::string cleaned = names_str;
                            
                            // Remove { and }
                            if (cleaned.front() == '{') cleaned = cleaned.substr(1);
                            if (cleaned.back() == '}') cleaned = cleaned.substr(0, cleaned.size() - 1);
                            
                            // Split by comma
                            std::stringstream ss(cleaned);
                            std::string pair;
                            while (std::getline(ss, pair, ',')) {
                                // Trim whitespace
                                pair.erase(pair.begin(), std::find_if(pair.begin(), pair.end(), [](unsigned char ch) {
                                    return !std::isspace(ch);
                                }));
                                pair.erase(std::find_if(pair.rbegin(), pair.rend(), [](unsigned char ch) {
                                    return !std::isspace(ch);
                                }).base(), pair.end());
                                
                                // Parse key: 'value' format
                                size_t colon_pos = pair.find(':');
                                if (colon_pos != std::string::npos) {
                                    std::string key_str = pair.substr(0, colon_pos);
                                    std::string value_str = pair.substr(colon_pos + 1);
                                    
                                    // Trim whitespace
                                    key_str.erase(key_str.begin(), std::find_if(key_str.begin(), key_str.end(), [](unsigned char ch) {
                                        return !std::isspace(ch);
                                    }));
                                    key_str.erase(std::find_if(key_str.rbegin(), key_str.rend(), [](unsigned char ch) {
                                        return !std::isspace(ch);
                                    }).base(), key_str.end());
                                    
                                    value_str.erase(value_str.begin(), std::find_if(value_str.begin(), value_str.end(), [](unsigned char ch) {
                                        return !std::isspace(ch);
                                    }));
                                    value_str.erase(std::find_if(value_str.rbegin(), value_str.rend(), [](unsigned char ch) {
                                        return !std::isspace(ch);
                                    }).base(), value_str.end());
                                    
                                    // Remove quotes from value
                                    if (value_str.front() == '\'' && value_str.back() == '\'') {
                                        value_str = value_str.substr(1, value_str.size() - 2);
                                    } else if (value_str.front() == '"' && value_str.back() == '"') {
                                        value_str = value_str.substr(1, value_str.size() - 2);
                                    }
                                    
                                    int idx = std::stoi(key_str);
                                    if (idx >= static_cast<int>(class_names_.size())) {
                                        class_names_.resize(idx + 1);
                                    }
                                    class_names_[idx] = value_str;
                                }
                            }
                            
                            if (!class_names_.empty()) {
                                std::cout << "[ZZA0_0102_Worker] Successfully parsed Python dict format" << std::endl;
                            }
                        } catch (...) {
                            std::cerr << "[ZZA0_0102_Worker] Failed to parse Python dict class names" << std::endl;
                        }
                    }
                } else if (names_str.find(',') != std::string::npos) {
                    // Comma-separated format
                    std::stringstream ss(names_str);
                    std::string class_name;
                    while (std::getline(ss, class_name, ',')) {
                        // Trim whitespace
                        class_name.erase(class_name.begin(), std::find_if(class_name.begin(), class_name.end(), [](unsigned char ch) {
                            return !std::isspace(ch);
                        }));
                        class_name.erase(std::find_if(class_name.rbegin(), class_name.rend(), [](unsigned char ch) {
                            return !std::isspace(ch);
                        }).base(), class_name.end());
                        class_names_.push_back(class_name);
                    }
                }
            }

            // Try alternative metadata keys
            std::vector<std::string> metadata_keys = {"classes", "class_names", "labels", "categories"};
            for (const auto& key : metadata_keys) {
                if (!class_names_.empty()) break;  // Already found names

                auto result = model_metadata.LookupCustomMetadataMapAllocated(key.c_str(), allocator);
                if (result) {
                    std::string value_str = result.get();
                    std::cout << "[ZZA0_0102_Worker] Found class names in metadata key '" << key << "': " << value_str << std::endl;
                    // Parse similar to above
                    class_names_.clear();
                    std::stringstream ss(value_str);
                    std::string class_name;
                    while (std::getline(ss, class_name, ',')) {
                        class_name.erase(class_name.begin(), std::find_if(class_name.begin(), class_name.end(), [](unsigned char ch) {
                            return !std::isspace(ch);
                        }));
                        class_name.erase(std::find_if(class_name.rbegin(), class_name.rend(), [](unsigned char ch) {
                            return !std::isspace(ch);
                        }).base(), class_name.end());
                        class_names_.push_back(class_name);
                    }
                    break;
                }
            }

            if (class_names_.empty()) {
                // Fallback: try to infer from output shape
                if (!output_shapes_.empty() && output_shapes_[0].size() >= 3) {
                    size_t detection_size = output_shapes_[0][2];
                    if (detection_size > 5) {  // x1,y1,x2,y2,conf + classes
                        size_t num_classes = detection_size - 5;
                        std::cout << "[ZZA0_0102_Worker] Inferred " << num_classes << " classes from output shape" << std::endl;
                        class_names_.resize(num_classes);
                        for (size_t i = 0; i < num_classes; i++) {
                            class_names_[i] = "class_" + std::to_string(i);
                        }
                    }
                }

                // If still empty, use default names
                if (class_names_.empty()) {
                    class_names_ = {"text", "signature", "stamp", "logo", "table", "image"};
                    std::cout << "[ZZA0_0102_Worker] Using default class names" << std::endl;
                }
            }

            std::cout << "[ZZA0_0102_Worker] Loaded " << class_names_.size() << " class names" << std::endl;
            for (size_t i = 0; i < class_names_.size(); i++) {
                std::cout << "  " << i << ": " << class_names_[i] << std::endl;
            }

        } catch (const std::exception& e) {
            std::cerr << "[ZZA0_0102_Worker] Failed to load class names from model: " << e.what() << std::endl;
            // Fallback to default
            class_names_ = {"text", "signature", "stamp", "logo", "table", "image"};
        }
    }

    /**
     * Initialize ONNX Runtime environment and load model
     */
    void initializeOnnxRuntime() {
        try {
            // Create ONNX Runtime environment
            env_ = new Ort::Env(ORT_LOGGING_LEVEL_WARNING, "YOLO_Detection");

            // Configure session options
            Ort::SessionOptions session_options;
            session_options.SetIntraOpNumThreads(1);
            session_options.SetGraphOptimizationLevel(GraphOptimizationLevel::ORT_ENABLE_EXTENDED);

            // Model path - use environment variable for project root
            const char* project_root_env = std::getenv("DEEPAPP_PROJECT_ROOT");
            std::string model_path;

            if (project_root_env) {
                model_path = std::string(project_root_env) + "/src/main/resources/models/yolo/giay_ra_vien/best.onnx";
                std::cout << "[ZZA0_0102_Worker] Using project root from environment: " << project_root_env << std::endl;
            } else {
                // Fallback to absolute path if environment variable not set
                model_path = "/home/vpslocal/new_workspace/deepapp_bigdata_ml_traininghub/src/main/resources/models/yolo/giay_ra_vien/best.onnx";
                std::cout << "[ZZA0_0102_Worker] Environment variable DEEPAPP_PROJECT_ROOT not set, using fallback path" << std::endl;
            }

            // Check if model file exists
            std::ifstream model_file(model_path);
            // if (!model_file.good()) {
            //     // Try absolute path
            //     model_path = "/home/vpslocal/new_workspace/deepapp_bigdata_ml_traininghub/src/main/resources/models/yolo/giay_ra_vien/best.onnx";
            //     //model_path = "/root/deepapp/deepapp_main/src/main/resources/models/yolo/giay_ra_vien/best.onnx";
               
            //     std::ifstream abs_model_file(model_path);
            //     if (!abs_model_file.good()) {
            //         std::cerr << "[ZZA0_0102_Worker] ERROR: Model file not found at: " << model_path << std::endl;
            //         std::cerr << "[ZZA0_0102_Worker] ERROR: Also tried relative path: src/main/resources/models/yolo/giay_ra_vien/best.onnx" << std::endl;
            //         return;
            //     }
            // }

            // Create session
            session_ = new Ort::Session(*env_, model_path.c_str(), session_options);

            // Load class names from model metadata
            loadClassNamesFromModel();

            // Get input/output names and shapes
            Ort::AllocatorWithDefaultOptions allocator;

            // Input info
            size_t input_count = session_->GetInputCount();
            for (size_t i = 0; i < input_count; i++) {
                auto input_name = session_->GetInputNameAllocated(i, allocator);
                input_names_.push_back(input_name.get());

                auto input_shape = session_->GetInputTypeInfo(i).GetTensorTypeAndShapeInfo().GetShape();
                input_shapes_.push_back(input_shape);
            }

            // Output info
            size_t output_count = session_->GetOutputCount();
            for (size_t i = 0; i < output_count; i++) {
                auto output_name = session_->GetOutputNameAllocated(i, allocator);
                output_names_.push_back(output_name.get());

                auto output_shape = session_->GetOutputTypeInfo(i).GetTensorTypeAndShapeInfo().GetShape();
                output_shapes_.push_back(output_shape);
            }

            std::cout << "[ZZA0_0102_Worker] ✓ ONNX model loaded successfully" << std::endl;
            std::cout << "[ZZA0_0102_Worker]   - Inputs: " << input_count << std::endl;
            std::cout << "[ZZA0_0102_Worker]   - Outputs: " << output_count << std::endl;

        } catch (const Ort::Exception& e) {
            std::cerr << "[ZZA0_0102_Worker] ERROR: Failed to initialize ONNX Runtime: " << e.what() << std::endl;
        } catch (const std::exception& e) {
            std::cerr << "[ZZA0_0102_Worker] ERROR: Exception during ONNX initialization: " << e.what() << std::endl;
        }
    }

    /**
     * Cleanup ONNX Runtime resources
     */
    void cleanupOnnxRuntime() {
        if (session_) {
            delete session_;
            session_ = nullptr;
        }
        if (env_) {
            delete env_;
            env_ = nullptr;
        }
    }

    /**
     * Decode base64 string to bytes
     */
    std::vector<uint8_t> decodeBase64(const std::string& encoded) {
        // Simple base64 decode implementation
        static const std::string base64_chars =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            "abcdefghijklmnopqrstuvwxyz"
            "0123456789+/";

        std::vector<uint8_t> decoded;
        int val = 0, valb = -8;
        for (unsigned char c : encoded) {
            if (c == '=') break;
            auto it = base64_chars.find(c);
            if (it == std::string::npos) continue;
            val = (val << 6) + it;
            valb += 6;
            if (valb >= 0) {
                decoded.push_back((val >> valb) & 0xFF);
                valb -= 8;
            }
        }
        return decoded;
    }

    /**
     * Load image from base64 or file path
     */
    cv::Mat loadImage(const json& request) {
        if (request.contains("image_data") && !request["image_data"].get<std::string>().empty()) {
            // Load from base64
            std::string base64_data = request["image_data"];
            std::vector<uint8_t> image_data = decodeBase64(base64_data);
            return cv::imdecode(image_data, cv::IMREAD_COLOR);
        } else if (request.contains("image") && !request["image"].get<std::string>().empty()) {
            // Load from base64 (legacy)
            std::string base64_data = request["image"];
            std::vector<uint8_t> image_data = decodeBase64(base64_data);
            return cv::imdecode(image_data, cv::IMREAD_COLOR);
        } else if (request.contains("image_path") && !request["image_path"].get<std::string>().empty()) {
            // Load from file path
            std::string image_path = request["image_path"];
            std::cout << "[ZZA0_0102_Worker] Loading image from file: " << image_path << std::endl;
            return cv::imread(image_path, cv::IMREAD_COLOR);
        }
        return cv::Mat();
    }

    /**
     * Preprocess image for YOLO model
     */
    cv::Mat preprocessImage(const cv::Mat& image, int target_size, float& scale_factor, int& x_offset, int& y_offset) {
        cv::Mat processed;

        // Resize image to target size (keeping aspect ratio)
        int max_dim = std::max(image.rows, image.cols);
        scale_factor = static_cast<float>(target_size) / max_dim;

        cv::Size new_size(image.cols * scale_factor, image.rows * scale_factor);
        cv::resize(image, processed, new_size, 0, 0, cv::INTER_LINEAR);

        // Create square canvas
        cv::Mat canvas(target_size, target_size, CV_8UC3, cv::Scalar(114, 114, 114));

        // Center the image
        x_offset = (target_size - processed.cols) / 2;
        y_offset = (target_size - processed.rows) / 2;
        processed.copyTo(canvas(cv::Rect(x_offset, y_offset, processed.cols, processed.rows)));

        // Convert BGR to RGB
        cv::cvtColor(canvas, canvas, cv::COLOR_BGR2RGB);

        // Convert to float and normalize to [0, 1]
        canvas.convertTo(canvas, CV_32F, 1.0 / 255.0);

        return canvas;
    }

   /**
 * Run YOLO inference
 */
std::vector<std::vector<float>> runInference(const cv::Mat& processed_image) {
    if (!session_) {
        throw std::runtime_error("ONNX session not initialized");
    }

    try {
        // Get input shape
        auto input_shape = input_shapes_[0];
        int64_t batch_size = input_shape[0];
        int64_t channels = input_shape[1];
        int64_t height = input_shape[2];
        int64_t width = input_shape[3];

        // Prepare input tensor
        std::vector<float> input_tensor_values;
        input_tensor_values.reserve(batch_size * channels * height * width);

        // Convert image to CHW format
        std::vector<cv::Mat> channels_split;
        cv::split(processed_image, channels_split);

        for (int c = 0; c < channels; c++) {
            cv::Mat channel = channels_split[c];
            for (int h = 0; h < height; h++) {
                for (int w = 0; w < width; w++) {
                    input_tensor_values.push_back(channel.at<float>(h, w));
                }
            }
        }

        // Create input tensor
        Ort::MemoryInfo memory_info = Ort::MemoryInfo::CreateCpu(OrtArenaAllocator, OrtMemTypeDefault);
        Ort::Value input_tensor = Ort::Value::CreateTensor<float>(
            memory_info, input_tensor_values.data(),
            input_tensor_values.size(), input_shape.data(), input_shape.size());

        // Prepare input/output names
        std::vector<const char*> input_names_ptr(input_names_.size());
        for (size_t i = 0; i < input_names_.size(); i++) {
            input_names_ptr[i] = input_names_[i].c_str();
        }

        std::vector<const char*> output_names_ptr(output_names_.size());
        for (size_t i = 0; i < output_names_.size(); i++) {
            output_names_ptr[i] = output_names_[i].c_str();
        }

        // Run inference
        auto output_tensors = session_->Run(
            Ort::RunOptions{nullptr},
            input_names_ptr.data(), &input_tensor, 1,
            output_names_ptr.data(), output_names_.size());

        // Extract output
        float* output_data = output_tensors[0].GetTensorMutableData<float>();
        auto output_shape = output_shapes_[0];

        std::cout << "[ZZA0_0102_Worker] Output shape: [";
        for (size_t dim : output_shape) {
            std::cout << dim << ", ";
        }
        std::cout << "]" << std::endl;

        // YOLOv8 output format: [1, num_classes+4, num_predictions]
        // Need to transpose to [num_predictions, num_classes+4]
        size_t num_classes = class_names_.size();
        size_t num_predictions = 0;
        size_t features = 0;
        
        if (output_shape.size() == 3) {
            // Format: [batch, features, predictions]
            features = output_shape[1];  // num_classes + 4
            num_predictions = output_shape[2];
        } else if (output_shape.size() == 2) {
            // Already in correct format: [predictions, features]
            num_predictions = output_shape[0];
            features = output_shape[1];
        }

        std::cout << "[ZZA0_0102_Worker] Features: " << features << ", Predictions: " << num_predictions << std::endl;

        std::vector<std::vector<float>> detections;
        
        // Transpose if needed (from [1, features, predictions] to [predictions, features])
        if (output_shape.size() == 3) {
            for (size_t i = 0; i < num_predictions; i++) {
                std::vector<float> detection;
                detection.reserve(features);
                
                for (size_t j = 0; j < features; j++) {
                    // Access transposed: output[0][j][i]
                    detection.push_back(output_data[j * num_predictions + i]);
                }
                
                detections.push_back(detection);
            }
        } else {
            // Already in correct format
            for (size_t i = 0; i < num_predictions; i++) {
                std::vector<float> detection;
                for (size_t j = 0; j < features; j++) {
                    detection.push_back(output_data[i * features + j]);
                }
                detections.push_back(detection);
            }
        }

        std::cout << "[ZZA0_0102_Worker] Transposed to " << detections.size() << " detections" << std::endl;
        
        // Debug: Print first detection
        if (!detections.empty()) {
            std::cout << "[ZZA0_0102_Worker] First detection (first 10 values): ";
            for (size_t i = 0; i < std::min(size_t(10), detections[0].size()); i++) {
                std::cout << detections[0][i] << " ";
            }
            std::cout << std::endl;
        }

        return detections;

    } catch (const Ort::Exception& e) {
        throw std::runtime_error(std::string("ONNX inference failed: ") + e.what());
    }
}

/**
 * Post-process YOLO detections
 */
std::vector<json> postProcessDetections(const std::vector<std::vector<float>>& raw_detections,
                                      float confidence_threshold, float iou_threshold,
                                      int original_width, int original_height,
                                      float scale_factor, int x_offset, int y_offset, int target_size = 640, int max_detections = 100) {
    std::vector<json> results;

    int valid_count = 0;
    
    for (const auto& detection : raw_detections) {
        if (detection.size() < 4 + class_names_.size()) {
            continue;  // Need at least x,y,w,h + class scores
        }

        // YOLOv8 format: [x_center, y_center, width, height, class0_score, class1_score, ...]
        float x_center = detection[0];
        float y_center = detection[1];
        float width = detection[2];
        float height = detection[3];

        // Debug: Print first few detections raw values
        if (valid_count < 3) {
            std::cout << "[ZZA0_0102_Worker] Detection " << valid_count << " raw: x_center=" << x_center 
                      << ", y_center=" << y_center << ", width=" << width << ", height=" << height << std::endl;
        }

        // Debug: Print first detection raw values
        if (valid_count == 0) {
            std::cout << "[ZZA0_0102_Worker] First detection raw: x_center=" << x_center 
                      << ", y_center=" << y_center << ", width=" << width << ", height=" << height << std::endl;
        }

        // Find class with highest score
        int class_id = 0;
        float max_score = 0.0f;
        for (size_t i = 4; i < detection.size() && i < 4 + class_names_.size(); i++) {
            if (detection[i] > max_score) {
                max_score = detection[i];
                class_id = static_cast<int>(i - 4);
            }
        }

        // Filter by confidence (max_score is the confidence for YOLOv8)
        if (max_score < confidence_threshold) {
            continue;
        }

        // Convert from center coordinates to corner coordinates
        float x1 = x_center - width / 2.0f;
        float y1_val = y_center - height / 2.0f;
        float x2 = x_center + width / 2.0f;
        float y2 = y_center + height / 2.0f;

        // Convert from model coordinates (0-640) to canvas coordinates
        // Then remove padding and scale back to original image
        
        // Remove padding offset
        x1 = x1 - x_offset;
        y1_val = y1_val - y_offset;
        x2 = x2 - x_offset;
        y2 = y2 - y_offset;

        // Scale back to original image size
        x1 = x1 / scale_factor;
        y1_val = y1_val / scale_factor;
        x2 = x2 / scale_factor;
        y2 = y2 / scale_factor;

        // Clip to image boundaries
        x1 = std::max(0.0f, std::min(x1, static_cast<float>(original_width)));
        y1_val = std::max(0.0f, std::min(y1_val, static_cast<float>(original_height)));
        x2 = std::max(0.0f, std::min(x2, static_cast<float>(original_width)));
        y2 = std::max(0.0f, std::min(y2, static_cast<float>(original_height)));

        // Ensure valid bounding box
        if (x2 <= x1 || y2 <= y1_val) {
            continue;
        }

        // Filter out very small boxes
        float box_width = x2 - x1;
        float box_height = y2 - y1_val;
        float area = box_width * box_height;
        
        if (area < 100.0f || box_width < 10.0f || box_height < 10.0f) {
            continue;
        }

        valid_count++;

        // Create detection result
        json det;
        det["label"] = getClassName(class_id);
        det["confidence"] = max_score;
        
        // Debug output for first few detections
        if (valid_count <= 3) {
            std::cout << "[ZZA0_0102_Worker] Detection " << valid_count << " final coords: "
                      << "x1=" << x1 << ", y1=" << y1_val << ", x2=" << x2 << ", y2=" << y2 
                      << " (original: " << original_width << "x" << original_height << ")" << std::endl;
        }
        
        det["bbox"]["x1"] = std::round(x1);
        det["bbox"]["y1"] = std::round(y1_val);
        det["bbox"]["x2"] = std::round(x2);
        det["bbox"]["y2"] = std::round(y2);

        results.push_back(det);
    }

    std::cout << "[ZZA0_0102_Worker] Valid detections before NMS: " << valid_count << std::endl;

    // Apply NMS
    auto final_results = applyNMS(results, iou_threshold);
    
    std::cout << "[ZZA0_0102_Worker] Final detections after NMS: " << final_results.size() << std::endl;

    // Limit number of detections
    if (final_results.size() > static_cast<size_t>(max_detections)) {
        final_results.resize(max_detections);
        std::cout << "[ZZA0_0102_Worker] Limited to max_detections: " << max_detections << std::endl;
    }

    return final_results;
}

    /**
     * Calculate Intersection over Union (IoU)
     */
    float calculateIoU(const json& box1, const json& box2) {
        float x1_1 = box1["bbox"]["x1"];
        float y1_1 = box1["bbox"]["y1"];
        float x2_1 = box1["bbox"]["x2"];
        float y2_1 = box1["bbox"]["y2"];

        float x1_2 = box2["bbox"]["x1"];
        float y1_2 = box2["bbox"]["y1"];
        float x2_2 = box2["bbox"]["x2"];
        float y2_2 = box2["bbox"]["y2"];

        // Calculate intersection
        float x1_inter = std::max(x1_1, x1_2);
        float y1_inter = std::max(y1_1, y1_2);
        float x2_inter = std::min(x2_1, x2_2);
        float y2_inter = std::min(y2_1, y2_2);

        float inter_area = std::max(0.0f, x2_inter - x1_inter) * std::max(0.0f, y2_inter - y1_inter);

        // Calculate union
        float area1 = (x2_1 - x1_1) * (y2_1 - y1_1);
        float area2 = (x2_2 - x1_2) * (y2_2 - y1_2);
        float union_area = area1 + area2 - inter_area;

        return inter_area / union_area;
    }

    /**
     * Apply Non-Maximum Suppression
     */
    std::vector<json> applyNMS(const std::vector<json>& detections, float iou_threshold) {
        if (detections.empty()) return detections;

        // Sort by confidence (descending)
        std::vector<json> sorted_detections = detections;
        std::sort(sorted_detections.begin(), sorted_detections.end(),
                 [](const json& a, const json& b) {
                     return a["confidence"].get<float>() > b["confidence"].get<float>();
                 });

        std::vector<json> results;

        for (const auto& detection : sorted_detections) {
            bool keep = true;

            for (const auto& kept : results) {
                float iou = calculateIoU(detection, kept);
                if (iou > iou_threshold) {
                    keep = false;
                    break;
                }
            }

            if (keep) {
                results.push_back(detection);
            }
        }

        return results;
    }

    /**
     * Get class name from class ID
     */
    std::string getClassName(int class_id) {
        if (class_id >= 0 && class_id < static_cast<int>(class_names_.size())) {
            return class_names_[class_id];
        }
        return "unknown_" + std::to_string(class_id);
    }

    /**
     * Main detection method
     */
    std::string detectObjects(const std::string& payload) {
        try {
            json request = json::parse(payload);
            json response;

            response["worker"] = "ZZA0_0102_W";
            response["status"] = "success";
            response["timestamp"] = std::time(nullptr);

            // Extract parameters
            std::string model = request.value("model", "giay_ra_vien");
            float confidence = request.value("confidence", 0.9f);
            float iou_threshold = request.value("iou", 0.45f);
            int max_detections = request.value("max_detections", 100);
            int img_size = request.value("img_size", 640);
            bool augment = request.value("augment", false);
            bool half_precision = request.value("half_precision", false);

            response["model"] = model;
            response["confidence"] = confidence;
            response["iouThreshold"] = iou_threshold;
            response["max_detections"] = max_detections;
            response["img_size"] = img_size;
            response["augment"] = augment;
            response["half_precision"] = half_precision;

            // Note: augment and half_precision not yet implemented
            if (augment) {
                std::cout << "[ZZA0_0102_Worker] Note: Test-time augmentation not yet implemented" << std::endl;
            }
            if (half_precision) {
                std::cout << "[ZZA0_0102_Worker] Note: Half precision inference not yet implemented" << std::endl;
            }

            std::cout << "[ZZA0_0102_Worker] Parameters: confidence=" << confidence 
                      << ", iou=" << iou_threshold << ", max_detections=" << max_detections
                      << ", img_size=" << img_size << ", augment=" << (augment ? "true" : "false")
                      << ", half_precision=" << (half_precision ? "true" : "false") << std::endl;

            // Load image
            cv::Mat image = loadImage(request);
            if (image.empty()) {
                response["status"] = "error";
                response["error"] = "Failed to load image";
                return response.dump();
            }

            response["dimensions"]["width"] = image.cols;
            response["dimensions"]["height"] = image.rows;

            std::cout << "[ZZA0_0102_Worker] Processing image: " << image.cols << "x" << image.rows << std::endl;

            // Preprocess image
            float scale_factor;
            int x_offset, y_offset;
            cv::Mat processed = preprocessImage(image, img_size, scale_factor, x_offset, y_offset);

            std::cout << "[ZZA0_0102_Worker] Image preprocessing: original=" << image.cols << "x" << image.rows 
                      << ", scale_factor=" << scale_factor << ", offsets=(" << x_offset << "," << y_offset << ")" << std::endl;

            // Run inference
            auto start_time = std::chrono::high_resolution_clock::now();
            auto raw_detections = runInference(processed);
            auto end_time = std::chrono::high_resolution_clock::now();

            auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end_time - start_time);
            response["processingTime"] = duration.count();

            std::cout << "[ZZA0_0102_Worker] Inference completed in " << duration.count() << "ms" << std::endl;

            // Post-process detections
            auto detections = postProcessDetections(raw_detections, confidence, iou_threshold,
                                                  image.cols, image.rows, scale_factor, x_offset, y_offset, img_size, max_detections);

            response["detections"] = detections;

            std::cout << "[ZZA0_0102_Worker] ✓ Detected " << detections.size() << " objects" << std::endl;

            return response.dump();

        } catch (const std::exception& e) {
            json error_response;
            error_response["worker"] = "ZZA0_0102_W";
            error_response["status"] = "error";
            error_response["error"] = std::string("Detection failed: ") + e.what();
            error_response["timestamp"] = std::time(nullptr);
            return error_response.dump();
        }
    }

    /**
     * Test detection method for validation
     */
    std::string testDetection(const std::string& payload) {
        try {
            json request = json::parse(payload);
            json response;

            response["worker"] = "ZZA0_0102_W";
            response["status"] = "success";
            response["timestamp"] = std::time(nullptr);
            response["event"] = "test";

            // Extract parameters
            std::string model = request.value("model", "giay_ra_vien");
            float confidence = request.value("confidence", 0.5f);
            float iou_threshold = request.value("iouThreshold", 0.45f);
            int max_detections = request.value("max_detections", 100);
            int img_size = request.value("img_size", 640);
            bool augment = request.value("augment", false);
            bool half_precision = request.value("half_precision", false);

            response["model"] = model;
            response["confidence"] = confidence;
            response["iouThreshold"] = iou_threshold;
            response["max_detections"] = max_detections;
            response["img_size"] = img_size;
            response["augment"] = augment;
            response["half_precision"] = half_precision;

            // Load image
            cv::Mat image = loadImage(request);
            if (image.empty()) {
                response["status"] = "error";
                response["error"] = "Failed to load image";
                return response.dump();
            }

            response["dimensions"]["width"] = image.cols;
            response["dimensions"]["height"] = image.rows;

            std::cout << "[ZZA0_0102_Worker] Test processing image: " << image.cols << "x" << image.rows << std::endl;

            // Preprocess image
            float scale_factor;
            int x_offset, y_offset;
            cv::Mat processed = preprocessImage(image, img_size, scale_factor, x_offset, y_offset);

            std::cout << "[ZZA0_0102_Worker] Test preprocessing: original=" << image.cols << "x" << image.rows 
                      << ", scale_factor=" << scale_factor << ", offsets=(" << x_offset << "," << y_offset << ")" << std::endl;

            // Run inference
            auto start_time = std::chrono::high_resolution_clock::now();
            auto raw_detections = runInference(processed);
            auto end_time = std::chrono::high_resolution_clock::now();

            auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end_time - start_time);
            response["processingTime"] = duration.count();

            std::cout << "[ZZA0_0102_Worker] Test inference completed in " << duration.count() << "ms" << std::endl;

            std::cout << "[ZZA0_0102_Worker] Test parameters: confidence=" << confidence 
                      << ", iou=" << iou_threshold << ", max_detections=" << max_detections
                      << ", img_size=" << img_size << ", augment=" << augment 
                      << ", half_precision=" << half_precision << std::endl;

            // Post-process detections
            auto detections = postProcessDetections(raw_detections, confidence, iou_threshold,
                                                  image.cols, image.rows, scale_factor, x_offset, y_offset, img_size, max_detections);

            response["detections"] = detections;

            std::cout << "[ZZA0_0102_Worker] ✓ Test detected " << detections.size() << " objects" << std::endl;

            return response.dump();

        } catch (const std::exception& e) {
            json error_response;
            error_response["worker"] = "ZZA0_0102_W";
            error_response["status"] = "error";
            error_response["error"] = std::string("Test detection failed: ") + e.what();
            error_response["timestamp"] = std::time(nullptr);
            return error_response.dump();
        }
    }
};

// Register the worker
REGISTER_WORKER(deepapp::workers::ZZA0_0102_Worker, "ZZA0_0102_W")

} // namespace workers
} // namespace deepapp