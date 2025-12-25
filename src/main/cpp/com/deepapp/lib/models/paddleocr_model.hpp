/**
 * PaddleOCR ONNX Model
 * 
 * Chinese/Multi-language OCR using PaddlePaddle models
 * Extends OnnxModelBase for consistency
 */

#pragma once

#include "lib/onnx/OnnxModelBase.hpp"
#include <opencv2/opencv.hpp>
#include <string>
#include <vector>
#include <memory>

namespace deepapp {
namespace ocr {

/**
 * PaddleOCR Detection Model (DB - Differentiable Binarization)
 */
class PaddleDetector : public lib::onnx::OnnxModelBase {
public:
    PaddleDetector(const std::string& model_path)
        : OnnxModelBase(model_path, 4) {}
    
    /**
     * Detect text regions in image
     * @return Vector of bounding boxes (x, y, w, h)
     */
    std::vector<cv::Rect> detect(const cv::Mat& image);

private:
    std::vector<float> preprocessImage(const cv::Mat& image);
    std::vector<cv::Rect> postprocessDetection(
        const std::vector<float>& output,
        const cv::Size& original_size
    );
};

/**
 * PaddleOCR Recognition Model (CRNN)
 */
class PaddleRecognizer : public lib::onnx::OnnxModelBase {
public:
    PaddleRecognizer(const std::string& model_path,
                     const std::string& dict_path)
        : OnnxModelBase(model_path, 4) {
        loadDictionary(dict_path);
    }
    
    /**
     * Recognize text from cropped text region
     */
    std::string recognize(const cv::Mat& image);

private:
    std::vector<std::string> char_dict_;
    
    void loadDictionary(const std::string& dict_path);
    std::vector<float> preprocessImage(const cv::Mat& image);
    std::string postprocessRecognition(const std::vector<float>& output);
};

/**
 * Complete PaddleOCR Pipeline
 */
class PaddleOCR {
public:
    PaddleOCR(const std::string& det_model_path,
              const std::string& rec_model_path,
              const std::string& dict_path);
    
    /**
     * OCR full pipeline: detection + recognition
     * @return Vector of (text, bounding_box) pairs
     */
    std::vector<std::pair<std::string, cv::Rect>> ocr(const cv::Mat& image);
    
    /**
     * Recognition only (assumes pre-cropped text)
     */
    std::string recognize(const cv::Mat& image);

private:
    std::unique_ptr<PaddleDetector> detector_;
    std::unique_ptr<PaddleRecognizer> recognizer_;
};

} // namespace ocr
} // namespace deepapp
