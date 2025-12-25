/**
 * YOLOv8 ONNX C++ Implementation
 * 
 * Object detection using YOLOv8 with ONNX Runtime
 * Based on: https://github.com/cyrusbehr/YOLOv8-TensorRT-CPP
 * 
 * Features:
 * - Object detection (bounding boxes)
 * - NMS (Non-Maximum Suppression)
 * - Multi-class detection
 */

#pragma once

#include <onnxruntime_cxx_api.h>
#include <opencv2/opencv.hpp>
#include <vector>
#include <string>
#include <memory>

namespace yolov8 {

// Detection result structure
struct Detection {
    cv::Rect bbox;       // Bounding box
    float confidence;    // Detection confidence
    int class_id;        // Class ID
    std::string class_name;  // Class name
};

// YOLOv8 configuration
struct Config {
    float conf_threshold = 0.25f;    // Confidence threshold
    float iou_threshold = 0.45f;     // IoU threshold for NMS
    int input_width = 640;           // Model input width
    int input_height = 640;          // Model input height
    std::vector<std::string> class_names;  // COCO class names
};

class YOLOv8_ONNX {
public:
    /**
     * Constructor
     * @param model_path Path to ONNX model file
     * @param config YOLOv8 configuration
     */
    YOLOv8_ONNX(
        const std::string& model_path,
        const Config& config = Config()
    );
    
    /**
     * Run object detection on image
     * @param image Input image (BGR format)
     * @return Vector of detections
     */
    std::vector<Detection> detect(const cv::Mat& image);
    
    /**
     * Run detection on batch of images
     * @param images Vector of input images
     * @return Vector of detection vectors
     */
    std::vector<std::vector<Detection>> detect_batch(
        const std::vector<cv::Mat>& images
    );
    
    /**
     * Draw detections on image
     * @param image Input/output image
     * @param detections Detections to draw
     */
    static void draw_detections(
        cv::Mat& image,
        const std::vector<Detection>& detections
    );

private:
    // ONNX Runtime components
    Ort::Env env_;
    Ort::SessionOptions session_options_;
    std::unique_ptr<Ort::Session> session_;
    
    // Model configuration
    Config config_;
    std::vector<int64_t> input_shape_;
    std::vector<int64_t> output_shape_;
    
    // Helper functions
    cv::Mat preprocess_image(const cv::Mat& image);
    std::vector<float> image_to_tensor(const cv::Mat& image);
    std::vector<Detection> postprocess(
        const std::vector<float>& output,
        const cv::Size& original_size
    );
    std::vector<int> nms(
        const std::vector<cv::Rect>& boxes,
        const std::vector<float>& scores,
        float iou_threshold
    );
    float calculate_iou(const cv::Rect& box1, const cv::Rect& box2);
    void load_coco_names();
};

} // namespace yolov8
