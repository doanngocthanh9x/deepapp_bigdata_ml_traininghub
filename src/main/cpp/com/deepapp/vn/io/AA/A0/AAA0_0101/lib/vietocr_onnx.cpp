#include "vietocr_onnx.hpp"
#include <fstream>
#include <sstream>
#include <algorithm>
#include <cmath>
#include <codecvt>
#include <locale>

VietOCR_ONNX::VietOCR_ONNX(
    const std::string& encoder_path,
    const std::string& decoder_path,
    const std::string& vocab_path
) : env_(ORT_LOGGING_LEVEL_WARNING, "VietOCR") {
    
    std::cout << "📦 Loading VietOCR ONNX models..." << std::endl;
    
    // Setup session options
    session_options_.SetIntraOpNumThreads(4);
    session_options_.SetGraphOptimizationLevel(
        GraphOptimizationLevel::ORT_ENABLE_ALL
    );
    
    // Load encoder
    try {
        encoder_session_ = std::make_unique<Ort::Session>(
            env_, encoder_path.c_str(), session_options_
        );
        std::cout << "  ✓ Encoder loaded: " << encoder_path << std::endl;
    } catch (const Ort::Exception& e) {
        throw std::runtime_error("Failed to load encoder: " + std::string(e.what()));
    }
    
    // Load decoder
    try {
        decoder_session_ = std::make_unique<Ort::Session>(
            env_, decoder_path.c_str(), session_options_
        );
        std::cout << "  ✓ Decoder loaded: " << decoder_path << std::endl;
    } catch (const Ort::Exception& e) {
        throw std::runtime_error("Failed to load decoder: " + std::string(e.what()));
    }
    
    // Load vocabulary
    load_vocab(vocab_path);
    
    std::cout << "  ✓ Vocabulary loaded: " << vocab_chars_.size() << " characters" << std::endl;
    std::cout << "✓ VietOCR initialized successfully" << std::endl;
}

void VietOCR_ONNX::load_vocab(const std::string& vocab_path) {
    std::ifstream file(vocab_path);
    
    std::string vocab_string;
    if (file.is_open()) {
        // Read first line (vocab is single line with all chars)
        std::getline(file, vocab_string);
        file.close();
    } else {
        std::cerr << "⚠️  Vocab file not found, using default Vietnamese characters" << std::endl;
        
        // Default Vietnamese vocab (from VietOCR)
        vocab_string = 
            "aAàÀảẢãÃáÁạẠăĂằẰẳẲẵẴắẮặẶâÂầẦẩẨẫẪấẤậẬbBcCdDđĐeEèÈẻẺẽẼéÉẹẸêÊềỀểỂễỄếẾệỆfFgGhHiIìÌỉỈĩĨíÍịỊjJkKlLmMnNoOòÒỏỎõÕóÓọỌôÔồỒổỔỗỖốỐộỘơƠờỜởỞỡỠớỚợỢpPqQrRsStTuUùÙủỦũŨúÚụỤưƯừỪửỬữỮứỨựỰvVwWxXyYỳỲỷỶỹỸýÝỵỴzZ0123456789!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~ ";
    }
    
    // Parse UTF-8 characters properly
    for (size_t i = 0; i < vocab_string.size(); ) {
        unsigned char c = vocab_string[i];
        int char_len = 1;
        
        // Determine UTF-8 character length
        if ((c & 0x80) == 0) {
            char_len = 1;
        } else if ((c & 0xE0) == 0xC0) {
            char_len = 2;
        } else if ((c & 0xF0) == 0xE0) {
            char_len = 3;
        } else if ((c & 0xF8) == 0xF0) {
            char_len = 4;
        }
        
        std::string utf8_char = vocab_string.substr(i, char_len);
        vocab_chars_.push_back(utf8_char);
        i += char_len;
    }
    
    // Special tokens (matching VietOCR vocab.py)
    pad_token_ = 0;
    sos_token_ = 1;  // go token
    eos_token_ = 2;
}

cv::Mat VietOCR_ONNX::preprocess_image(const cv::Mat& img) {
    // Resize to height 32, keep aspect ratio
    int target_height = 32;
    float ratio = static_cast<float>(target_height) / img.rows;
    int target_width = std::max(32, static_cast<int>(img.cols * ratio));
    
    // Make width multiple of 4 for better performance
    target_width = (target_width + 3) / 4 * 4;
    
    cv::Mat resized;
    cv::resize(img, resized, cv::Size(target_width, target_height), 0, 0, cv::INTER_LINEAR);
    
    // Convert BGR to RGB
    cv::Mat rgb;
    cv::cvtColor(resized, rgb, cv::COLOR_BGR2RGB);
    
    // Normalize to [0, 1] - VietOCR only divides by 255, no mean/std normalization
    cv::Mat normalized;
    rgb.convertTo(normalized, CV_32F, 1.0 / 255.0);
    
    return normalized;
}

std::vector<float> VietOCR_ONNX::run_encoder(const cv::Mat& preprocessed) {
    int height = preprocessed.rows;
    int width = preprocessed.cols;
    
    // Prepare input tensor [1, 3, H, W]
    std::vector<int64_t> input_shape = {1, 3, height, width};
    size_t input_tensor_size = 1 * 3 * height * width;
    std::vector<float> input_tensor_values(input_tensor_size);
    
    // Fill tensor in NCHW format
    for (int c = 0; c < 3; c++) {
        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                int idx = c * (height * width) + h * width + w;
                input_tensor_values[idx] = preprocessed.at<cv::Vec3f>(h, w)[c];
            }
        }
    }
    
    // Create input tensor
    auto memory_info = Ort::MemoryInfo::CreateCpu(OrtArenaAllocator, OrtMemTypeDefault);
    Ort::Value input_tensor = Ort::Value::CreateTensor<float>(
        memory_info,
        input_tensor_values.data(),
        input_tensor_size,
        input_shape.data(),
        input_shape.size()
    );
    
    // Run encoder
    const char* input_names[] = {"input"};
    const char* output_names[] = {"output"};  // Fixed: encoder output is "output" not "memory"
    
    try {
        auto output_tensors = encoder_session_->Run(
            Ort::RunOptions{nullptr},
            input_names,
            &input_tensor,
            1,
            output_names,
            1
        );
        
        // Extract output
        float* output_data = output_tensors[0].GetTensorMutableData<float>();
        auto output_shape = output_tensors[0].GetTensorTypeAndShapeInfo().GetShape();
        
        // Calculate total size
        size_t output_size = 1;
        for (auto dim : output_shape) {
            output_size *= dim;
        }
        
        // Store shape info for decoder
        memory_shape_ = output_shape;
        
        return std::vector<float>(output_data, output_data + output_size);
        
    } catch (const Ort::Exception& e) {
        throw std::runtime_error("Encoder inference failed: " + std::string(e.what()));
    }
}

std::vector<int> VietOCR_ONNX::greedy_decode(
    const std::vector<float>& memory,
    int max_len
) {
    std::vector<int> result = {sos_token_};
    
    // Memory shape from encoder: [seq_len, batch, d_model]
    int64_t seq_len = memory_shape_[0];
    int64_t batch = memory_shape_[1];
    int64_t d_model = memory_shape_[2];
    
    auto memory_info = Ort::MemoryInfo::CreateCpu(OrtArenaAllocator, OrtMemTypeDefault);
    
    for (int step = 0; step < max_len; step++) {
        // Prepare decoder input
        // tgt_inp: [tgt_len, batch]
        std::vector<int64_t> tgt_shape = {static_cast<int64_t>(result.size()), 1};
        std::vector<int64_t> tgt_data;
        for (int token : result) {
            tgt_data.push_back(token);
        }
        
        Ort::Value tgt_tensor = Ort::Value::CreateTensor<int64_t>(
            memory_info,
            tgt_data.data(),
            tgt_data.size(),
            tgt_shape.data(),
            tgt_shape.size()
        );
        
        // Memory: [seq_len, batch, d_model]
        std::vector<int64_t> memory_shape_vec = {seq_len, batch, d_model};
        std::vector<float> memory_copy = memory;
        
        Ort::Value memory_tensor = Ort::Value::CreateTensor<float>(
            memory_info,
            memory_copy.data(),
            memory_copy.size(),
            memory_shape_vec.data(),
            memory_shape_vec.size()
        );
        
        // Run decoder
        const char* input_names[] = {"tgt_inp", "memory"};
        const char* output_names[] = {"values", "indices"};
        
        std::vector<Ort::Value> input_tensors;
        input_tensors.push_back(std::move(tgt_tensor));
        input_tensors.push_back(std::move(memory_tensor));
        
        try {
            auto output_tensors = decoder_session_->Run(
                Ort::RunOptions{nullptr},
                input_names,
                input_tensors.data(),
                2,
                output_names,
                2  // 2 outputs: values and indices
            );
            
            // Get indices output: [batch, tgt_len, top_k=5]
            // We only care about top-1 prediction
            int64_t* indices_data = output_tensors[1].GetTensorMutableData<int64_t>();
            auto indices_shape = output_tensors[1].GetTensorTypeAndShapeInfo().GetShape();
            
            int64_t batch_size = indices_shape[0];
            int64_t tgt_len = indices_shape[1];
            int64_t top_k = indices_shape[2];
            
            // Get last token prediction (top-1)
            // indices[batch=0, last_pos, top_1=0]
            int last_token_idx = (batch_size * (tgt_len - 1) * top_k) + 0;
            int next_token = static_cast<int>(indices_data[last_token_idx]);
            
            // Add to result
            result.push_back(next_token);
            
            // Check for EOS
            if (next_token == eos_token_) {
                break;
            }
            
        } catch (const Ort::Exception& e) {
            std::cerr << "⚠️  Decoder step failed: " << e.what() << std::endl;
            break;
        }
    }
    
    return result;
}

std::string VietOCR_ONNX::tokens_to_text(const std::vector<int>& tokens) {
    std::string result;
    
    for (int token : tokens) {
        // Skip special tokens (0-3)
        if (token < 4) {
            continue;
        }
        
        // Token index maps to char: token - 4
        int char_idx = token - 4;
        
        // Check bounds
        if (char_idx >= 0 && char_idx < static_cast<int>(vocab_chars_.size())) {
            result += vocab_chars_[char_idx];
        }
    }
    
    return result;
}

std::string VietOCR_ONNX::predict(const cv::Mat& image) {
    if (image.empty()) {
        return "";
    }
    
    try {
        // 1. Preprocess image
        cv::Mat preprocessed = preprocess_image(image);
        
        // 2. Run encoder
        auto memory = run_encoder(preprocessed);
        
        // 3. Greedy decode
        auto tokens = greedy_decode(memory, 128);
        
        // 4. Convert tokens to text
        std::string text = tokens_to_text(tokens);
        
        return text;
        
    } catch (const std::exception& e) {
        std::cerr << "⚠️  Prediction failed: " << e.what() << std::endl;
        return "";
    }
}

std::vector<std::string> VietOCR_ONNX::predict_batch(const std::vector<cv::Mat>& images) {
    std::vector<std::string> results;
    results.reserve(images.size());
    
    for (const auto& image : images) {
        results.push_back(predict(image));
    }
    
    return results;
}