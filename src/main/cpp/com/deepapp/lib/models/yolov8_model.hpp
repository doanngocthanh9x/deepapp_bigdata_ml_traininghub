/**
 * YOLO v8 ONNX Model (Refactored)
 * 
 * Object detection using YOLOv8
 * Extends OnnxModelBase for consistency
 */

#pragma once

#include "lib/onnx/OnnxModelBase.hpp"
#include <opencv2/opencv.hpp>
#include <string>
#include <vector>
#include <memory>

namespace deepapp {
namespace detection {

/**
 * Detection result
 */
struct Detection {
    cv::Rect bbox;
    float confidence;
    int class_id;
    std::string class_name;
};

/**
 * YOLOv8 Detection Model
 */
class YOLOv8 : public lib::onnx::OnnxModelBase {
public:
    /**
     * Constructor
     * @param model_path Path to YOLOv8 ONNX model
     * @param class_names Vector of class names (optional)
     * @param conf_threshold Confidence threshold (default: 0.25)
     * @param iou_threshold IOU threshold for NMS (default: 0.45)
     */
    YOLOv8(const std::string& model_path,
           const std::vector<std::string>& class_names = {},
           float conf_threshold = 0.25f,
           float iou_threshold = 0.45f);
    
    /**
     * Detect objects in image
     * @param image Input image (BGR format)
     * @return Vector of detections
     */
    std::vector<Detection> detect(const cv::Mat& image);
    
    /**
     * Draw detections on image
     */
    cv::Mat drawDetections(const cv::Mat& image,
                           const std::vector<Detection>& detections);

private:
    std::vector<std::string> class_names_;
    float conf_threshold_;
    float iou_threshold_;
    
    static constexpr int INPUT_WIDTH = 640;
    static constexpr int INPUT_HEIGHT = 640;
    
    /**
     * Preprocess image for YOLO
     */
    std::vector<float> preprocessImage(const cv::Mat& image,
                                       float& scale_x,
                                       float& scale_y);
    
    /**
     * Postprocess YOLO output
     */
    std::vector<Detection> postprocessDetections(
        const std::vector<float>& output,
        float scale_x,
        float scale_y,
        const cv::Size& original_size
    );
    
    /**
     * Non-maximum suppression
     */
    std::vector<Detection> applyNMS(const std::vector<Detection>& detections);
};

} // namespace detection
} // namespace deepapp
