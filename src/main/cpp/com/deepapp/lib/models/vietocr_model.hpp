/**
 * VietOCR ONNX Model (Refactored)
 * 
 * Vietnamese OCR using transformer encoder-decoder architecture
 * Now extends OnnxModelBase for better code reusability
 */

#pragma once

#include "lib/onnx/OnnxModelBase.hpp"
#include <opencv2/opencv.hpp>
#include <string>
#include <vector>
#include <memory>

namespace deepapp {
namespace ocr {

class VietOCR {
public:
    /**
     * Constructor
     * @param encoder_path Path to encoder ONNX model
     * @param decoder_path Path to decoder ONNX model
     * @param vocab_path Path to vocabulary file
     */
    VietOCR(const std::string& encoder_path,
            const std::string& decoder_path,
            const std::string& vocab_path);

    /**
     * Recognize text from image
     * @param image Input image (BGR format)
     * @return Recognized text
     */
    std::string predict(const cv::Mat& image);

    /**
     * Batch prediction
     */
    std::vector<std::string> predict_batch(const std::vector<cv::Mat>& images);

private:
    std::unique_ptr<lib::onnx::OnnxModelBase> encoder_;
    std::unique_ptr<lib::onnx::OnnxModelBase> decoder_;
    std::vector<std::string> vocab_;
    
    // Constants
    static constexpr int IMG_HEIGHT = 32;
    static constexpr int MAX_SEQ_LEN = 128;
    
    /**
     * Load vocabulary from file
     */
    void loadVocabulary(const std::string& vocab_path);
    
    /**
     * Preprocess image for encoder
     */
    std::vector<float> preprocessImage(const cv::Mat& image);
    
    /**
     * Run encoder
     */
    std::vector<float> runEncoder(const std::vector<float>& image_data);
    
    /**
     * Run decoder with greedy search
     */
    std::string runDecoder(const std::vector<float>& encoder_output);
    
    /**
     * Decode token IDs to text
     */
    std::string decodeTokens(const std::vector<int>& token_ids);
};

} // namespace ocr
} // namespace deepapp
