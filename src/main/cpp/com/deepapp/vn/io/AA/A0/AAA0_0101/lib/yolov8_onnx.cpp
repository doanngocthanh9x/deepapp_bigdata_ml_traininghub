/**
 * YOLOv8 ONNX Implementation
 */

#include "yolov8_onnx.hpp"
#include <algorithm>
#include <iostream>
#include <fstream>
#include <numeric>

namespace yolov8 {

YOLOv8_ONNX::YOLOv8_ONNX(
    const std::string& model_path,
    const Config& config
) : env_(ORT_LOGGING_LEVEL_WARNING, "YOLOv8"),
    config_(config) {
    
    std::cout << "📦 Loading YOLOv8 ONNX model..." << std::endl;
    
    // Setup session options
    session_options_.SetIntraOpNumThreads(4);
    session_options_.SetGraphOptimizationLevel(
        GraphOptimizationLevel::ORT_ENABLE_ALL
    );
    
    // Load model
    try {
        session_ = std::make_unique<Ort::Session>(
            env_, model_path.c_str(), session_options_
        );
        std::cout << "  ✓ Model loaded: " << model_path << std::endl;
    } catch (const Ort::Exception& e) {
        throw std::runtime_error("Failed to load YOLOv8 model: " + std::string(e.what()));
    }
    
    // Get input shape
    auto input_info = session_->GetInputTypeInfo(0);
    auto tensor_info = input_info.GetTensorTypeAndShapeInfo();
    input_shape_ = tensor_info.GetShape();
    
    // Get output shape
    auto output_info = session_->GetOutputTypeInfo(0);
    auto output_tensor_info = output_info.GetTensorTypeAndShapeInfo();
    output_shape_ = output_tensor_info.GetShape();
    
    // Load class names if not provided
    if (config_.class_names.empty()) {
        load_coco_names();
    }
    
    std::cout << "  ✓ Input shape: [" << input_shape_[0] << ", " 
              << input_shape_[1] << ", " << input_shape_[2] << ", " 
              << input_shape_[3] << "]" << std::endl;
    std::cout << "  ✓ Classes: " << config_.class_names.size() << std::endl;
    std::cout << "✓ YOLOv8 initialized successfully" << std::endl;
}

void YOLOv8_ONNX::load_coco_names() {
    // COCO 80 class names
    config_.class_names = {
        "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat",
        "traffic light", "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat",
        "dog", "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "backpack",
        "umbrella", "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard", "sports ball",
        "kite", "baseball bat", "baseball glove", "skateboard", "surfboard", "tennis racket",
        "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple",
        "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair",
        "couch", "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse",
        "remote", "keyboard", "cell phone", "microwave", "oven", "toaster", "sink",
        "refrigerator", "book", "clock", "vase", "scissors", "teddy bear", "hair drier",
        "toothbrush"
    };
}

cv::Mat YOLOv8_ONNX::preprocess_image(const cv::Mat& image) {
    // Resize to model input size (letterbox)
    int target_w = config_.input_width;
    int target_h = config_.input_height;
    
    // Calculate scale to fit image in target size
    float scale = std::min(
        static_cast<float>(target_w) / image.cols,
        static_cast<float>(target_h) / image.rows
    );
    
    int new_w = static_cast<int>(image.cols * scale);
    int new_h = static_cast<int>(image.rows * scale);
    
    // Resize image
    cv::Mat resized;
    cv::resize(image, resized, cv::Size(new_w, new_h), 0, 0, cv::INTER_LINEAR);
    
    // Create canvas and center image
    cv::Mat canvas = cv::Mat::zeros(target_h, target_w, CV_8UC3);
    canvas.setTo(cv::Scalar(114, 114, 114));  // Gray padding
    
    int top = (target_h - new_h) / 2;
    int left = (target_w - new_w) / 2;
    
    resized.copyTo(canvas(cv::Rect(left, top, new_w, new_h)));
    
    // Convert BGR to RGB
    cv::Mat rgb;
    cv::cvtColor(canvas, rgb, cv::COLOR_BGR2RGB);
    
    return rgb;
}

std::vector<float> YOLOv8_ONNX::image_to_tensor(const cv::Mat& image) {
    // Convert to float and normalize [0, 255] -> [0, 1]
    cv::Mat float_image;
    image.convertTo(float_image, CV_32F, 1.0 / 255.0);
    
    // Prepare tensor in NCHW format
    int height = float_image.rows;
    int width = float_image.cols;
    std::vector<float> tensor(1 * 3 * height * width);
    
    for (int c = 0; c < 3; c++) {
        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                int tensor_idx = c * (height * width) + h * width + w;
                tensor[tensor_idx] = float_image.at<cv::Vec3f>(h, w)[c];
            }
        }
    }
    
    return tensor;
}

std::vector<Detection> YOLOv8_ONNX::detect(const cv::Mat& image) {
    if (image.empty()) {
        return {};
    }
    
    cv::Size original_size = image.size();
    
    // Preprocess
    cv::Mat preprocessed = preprocess_image(image);
    std::vector<float> input_tensor = image_to_tensor(preprocessed);
    
    // Prepare input
    std::vector<int64_t> input_shape = {1, 3, config_.input_height, config_.input_width};
    auto memory_info = Ort::MemoryInfo::CreateCpu(OrtArenaAllocator, OrtMemTypeDefault);
    Ort::Value input = Ort::Value::CreateTensor<float>(
        memory_info,
        input_tensor.data(),
        input_tensor.size(),
        input_shape.data(),
        input_shape.size()
    );
    
    // Run inference
    const char* input_names[] = {"images"};
    const char* output_names[] = {"output0"};
    
    try {
        auto output_tensors = session_->Run(
            Ort::RunOptions{nullptr},
            input_names,
            &input,
            1,
            output_names,
            1
        );
        
        // Extract output
        float* output_data = output_tensors[0].GetTensorMutableData<float>();
        auto output_shape = output_tensors[0].GetTensorTypeAndShapeInfo().GetShape();
        
        size_t output_size = 1;
        for (auto dim : output_shape) {
            output_size *= dim;
        }
        
        std::vector<float> output(output_data, output_data + output_size);
        
        // Postprocess
        return postprocess(output, original_size);
        
    } catch (const Ort::Exception& e) {
        std::cerr << "⚠️  YOLOv8 inference failed: " << e.what() << std::endl;
        return {};
    }
}

std::vector<Detection> YOLOv8_ONNX::postprocess(
    const std::vector<float>& output,
    const cv::Size& original_size
) {
    // YOLOv8 output format: [1, 84, 8400] or [1, num_classes+4, num_boxes]
    // Each detection: [x, y, w, h, class0_conf, class1_conf, ...]
    
    int num_classes = config_.class_names.size();
    int num_boxes = output.size() / (num_classes + 4);
    
    std::vector<cv::Rect> boxes;
    std::vector<float> confidences;
    std::vector<int> class_ids;
    
    // Calculate scale factors
    float scale_x = static_cast<float>(original_size.width) / config_.input_width;
    float scale_y = static_cast<float>(original_size.height) / config_.input_height;
    
    // Parse detections
    for (int i = 0; i < num_boxes; i++) {
        // Get class scores
        float max_conf = 0.0f;
        int max_class = 0;
        
        for (int c = 0; c < num_classes; c++) {
            int idx = (4 + c) * num_boxes + i;
            float conf = output[idx];
            if (conf > max_conf) {
                max_conf = conf;
                max_class = c;
            }
        }
        
        // Filter by confidence
        if (max_conf < config_.conf_threshold) {
            continue;
        }
        
        // Get box coordinates
        float cx = output[0 * num_boxes + i];
        float cy = output[1 * num_boxes + i];
        float w = output[2 * num_boxes + i];
        float h = output[3 * num_boxes + i];
        
        // Convert to corner format and scale
        int x1 = static_cast<int>((cx - w / 2) * scale_x);
        int y1 = static_cast<int>((cy - h / 2) * scale_y);
        int x2 = static_cast<int>((cx + w / 2) * scale_x);
        int y2 = static_cast<int>((cy + h / 2) * scale_y);
        
        // Clamp to image bounds
        x1 = std::max(0, std::min(x1, original_size.width - 1));
        y1 = std::max(0, std::min(y1, original_size.height - 1));
        x2 = std::max(0, std::min(x2, original_size.width - 1));
        y2 = std::max(0, std::min(y2, original_size.height - 1));
        
        boxes.push_back(cv::Rect(x1, y1, x2 - x1, y2 - y1));
        confidences.push_back(max_conf);
        class_ids.push_back(max_class);
    }
    
    // Apply NMS
    std::vector<int> nms_indices = nms(boxes, confidences, config_.iou_threshold);
    
    // Create final detections
    std::vector<Detection> detections;
    for (int idx : nms_indices) {
        Detection det;
        det.bbox = boxes[idx];
        det.confidence = confidences[idx];
        det.class_id = class_ids[idx];
        det.class_name = (class_ids[idx] < config_.class_names.size()) 
            ? config_.class_names[class_ids[idx]] : "unknown";
        detections.push_back(det);
    }
    
    return detections;
}

std::vector<int> YOLOv8_ONNX::nms(
    const std::vector<cv::Rect>& boxes,
    const std::vector<float>& scores,
    float iou_threshold
) {
    // Sort by score descending
    std::vector<int> indices(boxes.size());
    std::iota(indices.begin(), indices.end(), 0);
    std::sort(indices.begin(), indices.end(), [&scores](int a, int b) {
        return scores[a] > scores[b];
    });
    
    std::vector<int> keep;
    std::vector<bool> suppressed(boxes.size(), false);
    
    for (size_t i = 0; i < indices.size(); i++) {
        int idx = indices[i];
        if (suppressed[idx]) continue;
        
        keep.push_back(idx);
        
        // Suppress overlapping boxes
        for (size_t j = i + 1; j < indices.size(); j++) {
            int idx2 = indices[j];
            if (suppressed[idx2]) continue;
            
            float iou = calculate_iou(boxes[idx], boxes[idx2]);
            if (iou > iou_threshold) {
                suppressed[idx2] = true;
            }
        }
    }
    
    return keep;
}

float YOLOv8_ONNX::calculate_iou(const cv::Rect& box1, const cv::Rect& box2) {
    int x1 = std::max(box1.x, box2.x);
    int y1 = std::max(box1.y, box2.y);
    int x2 = std::min(box1.x + box1.width, box2.x + box2.width);
    int y2 = std::min(box1.y + box1.height, box2.y + box2.height);
    
    int intersection = std::max(0, x2 - x1) * std::max(0, y2 - y1);
    int union_area = box1.area() + box2.area() - intersection;
    
    return (union_area > 0) ? static_cast<float>(intersection) / union_area : 0.0f;
}

std::vector<std::vector<Detection>> YOLOv8_ONNX::detect_batch(
    const std::vector<cv::Mat>& images
) {
    std::vector<std::vector<Detection>> results;
    results.reserve(images.size());
    
    for (const auto& image : images) {
        results.push_back(detect(image));
    }
    
    return results;
}

void YOLOv8_ONNX::draw_detections(
    cv::Mat& image,
    const std::vector<Detection>& detections
) {
    // Color palette
    std::vector<cv::Scalar> colors = {
        cv::Scalar(255, 0, 0), cv::Scalar(0, 255, 0), cv::Scalar(0, 0, 255),
        cv::Scalar(255, 255, 0), cv::Scalar(255, 0, 255), cv::Scalar(0, 255, 255),
        cv::Scalar(128, 0, 0), cv::Scalar(0, 128, 0), cv::Scalar(0, 0, 128)
    };
    
    for (const auto& det : detections) {
        cv::Scalar color = colors[det.class_id % colors.size()];
        
        // Draw bounding box
        cv::rectangle(image, det.bbox, color, 2);
        
        // Draw label
        std::string label = det.class_name + " " + 
                           std::to_string(static_cast<int>(det.confidence * 100)) + "%";
        
        int baseline;
        cv::Size label_size = cv::getTextSize(label, cv::FONT_HERSHEY_SIMPLEX, 0.5, 1, &baseline);
        
        int top = std::max(det.bbox.y, label_size.height);
        cv::rectangle(image, 
                     cv::Point(det.bbox.x, top - label_size.height - baseline),
                     cv::Point(det.bbox.x + label_size.width, top),
                     color, cv::FILLED);
        
        cv::putText(image, label, cv::Point(det.bbox.x, top - baseline),
                   cv::FONT_HERSHEY_SIMPLEX, 0.5, cv::Scalar(255, 255, 255), 1);
    }
}

} // namespace yolov8
