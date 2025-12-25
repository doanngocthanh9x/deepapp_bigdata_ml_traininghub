#pragma once

#include <onnxruntime_cxx_api.h>
#include <opencv2/opencv.hpp>
#include <vector>
#include <string>
#include <memory>
#include <iostream>

class VietOCR_ONNX {
public:
    /**
     * Constructor
     * @param encoder_path Path to transformer_encoder.onnx
     * @param decoder_path Path to transformer_decoder.onnx
     * @param vocab_path Path to vocabulary file (one char per line)
     */
    VietOCR_ONNX(
        const std::string& encoder_path,
        const std::string& decoder_path, 
        const std::string& vocab_path
    );
    
    /**
     * Predict text from single image
     * @param image Input image (BGR format)
     * @return Recognized text
     */
    std::string predict(const cv::Mat& image);
    
    /**
     * Predict text from multiple images (batch)
     * @param images Vector of input images
     * @return Vector of recognized texts
     */
    std::vector<std::string> predict_batch(const std::vector<cv::Mat>& images);
    
private:
    // ONNX Runtime environment and sessions
    Ort::Env env_;
    Ort::SessionOptions session_options_;
    std::unique_ptr<Ort::Session> encoder_session_;
    std::unique_ptr<Ort::Session> decoder_session_;
    
    // Vocabulary
    std::vector<std::string> vocab_chars_;
    int sos_token_ = 0;  // Start of sequence token
    int eos_token_ = 1;  // End of sequence token
    int pad_token_ = 2;  // Padding token
    
    // Memory shape from encoder (for decoder input)
    std::vector<int64_t> memory_shape_;
    
    /**
     * Preprocess image for VietOCR input
     * - Resize to height 32, keep aspect ratio
     * - Normalize with ImageNet stats
     * - Convert to RGB
     * @param img Input image
     * @return Preprocessed image [H, W, 3] float32
     */
    cv::Mat preprocess_image(const cv::Mat& img);
    
    /**
     * Run encoder (CNN + Transformer Encoder)
     * @param preprocessed Preprocessed image
     * @return Memory tensor (encoder output)
     */
    std::vector<float> run_encoder(const cv::Mat& preprocessed);
    
    /**
     * Greedy decoding (autoregressive)
     * @param memory Encoder output
     * @param max_len Maximum sequence length
     * @return Vector of token IDs
     */
    std::vector<int> greedy_decode(
        const std::vector<float>& memory,
        int max_len = 128
    );
    
    /**
     * Convert token IDs to text string
     * @param tokens Vector of token IDs
     * @return UTF-8 text string
     */
    std::string tokens_to_text(const std::vector<int>& tokens);
    
    /**
     * Load vocabulary from file
     * @param vocab_path Path to vocab file
     */
    void load_vocab(const std::string& vocab_path);
};