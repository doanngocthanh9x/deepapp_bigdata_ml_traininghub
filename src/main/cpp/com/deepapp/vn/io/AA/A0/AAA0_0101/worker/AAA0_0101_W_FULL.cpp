/**
 * AAA0_0101 OCR Worker - With VietOCR Implementation
 * 
 * Full VietOCR + YOLO integration
 * 
 * Requires:
 * - OpenCV 4.x
 * - ONNX Runtime
 * - VietOCR models in /app/models/vietocr/
 */

#include "com/deepapp/infrastructure/BaseWorker.h"
#include "com/deepapp/infrastructure/WorkerRegistry.h"
#include "../lib/vietocr_onnx.hpp"
#include <iostream>
#include <sstream>
#include <string>
#include <memory>
#include <chrono>
#include <opencv2/opencv.hpp>

// Model paths
const std::string ENCODER_MODEL = "/app/models/vietocr/transformer_encoder.onnx";
const std::string DECODER_MODEL = "/app/models/vietocr/transformer_decoder.onnx";
const std::string VOCAB_PATH = "/app/models/vietocr/vocab.txt";

namespace deepapp {
namespace workers {

/**
 * Base64 decode utility
 */
std::vector<unsigned char> base64_decode(const std::string& encoded_string) {
    static const std::string base64_chars = 
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        "abcdefghijklmnopqrstuvwxyz"
        "0123456789+/";
    
    std::vector<unsigned char> ret;
    int i = 0;
    int j = 0;
    unsigned char char_array_4[4], char_array_3[3];
    
    for (char c : encoded_string) {
        if (c == '=') break;
        if (isspace(c)) continue;
        
        size_t pos = base64_chars.find(c);
        if (pos == std::string::npos) continue;
        
        char_array_4[i++] = pos;
        if (i == 4) {
            char_array_3[0] = (char_array_4[0] << 2) + ((char_array_4[1] & 0x30) >> 4);
            char_array_3[1] = ((char_array_4[1] & 0xf) << 4) + ((char_array_4[2] & 0x3c) >> 2);
            char_array_3[2] = ((char_array_4[2] & 0x3) << 6) + char_array_4[3];
            
            for (i = 0; i < 3; i++)
                ret.push_back(char_array_3[i]);
            i = 0;
        }
    }
    
    if (i) {
        for (j = i; j < 4; j++)
            char_array_4[j] = 0;
        
        char_array_3[0] = (char_array_4[0] << 2) + ((char_array_4[1] & 0x30) >> 4);
        char_array_3[1] = ((char_array_4[1] & 0xf) << 4) + ((char_array_4[2] & 0x3c) >> 2);
        char_array_3[2] = ((char_array_4[2] & 0x3) << 6) + char_array_4[3];
        
        for (j = 0; j < i - 1; j++)
            ret.push_back(char_array_3[j]);
    }
    
    return ret;
}

/**
 * VietOCR Worker with full OCR capabilities
 */
class AAA0_0101_Worker : public infrastructure::BaseWorker {
private:
    std::unique_ptr<VietOCR_ONNX> recognizer_;
    bool initialized_ = false;

public:
    AAA0_0101_Worker() : BaseWorker("AAA0_0101_Worker") {
        std::cout << "[AAA0_0101_Worker] VietOCR Worker Initialized (lazy loading)" << std::endl;
    }

    /**
     * Process OCR task with VietOCR
     */
    std::string processTask(
        const std::string& eventType, 
        const std::string& payload) override {
        
        std::cout << "[AAA0_0101_Worker] Processing OCR task:" << std::endl;
        std::cout << "  Event Type: " << eventType << std::endl;

        auto start_time = std::chrono::high_resolution_clock::now();

        try {
            // Initialize VietOCR on first use (lazy loading)
            if (!initialized_) {
                std::cout << "[AAA0_0101_Worker] Initializing VietOCR models (first time)..." << std::endl;
                recognizer_ = std::make_unique<VietOCR_ONNX>(
                    ENCODER_MODEL,
                    DECODER_MODEL,
                    VOCAB_PATH
                );
                initialized_ = true;
                std::cout << "[AAA0_0101_Worker] VietOCR models loaded successfully" << std::endl;
            }

            // Parse input
            std::string engine = extractJsonField(payload, "engine");
            std::string language = extractJsonField(payload, "language");
            std::string image_b64 = extractJsonField(payload, "image");
            std::string imagePath = extractJsonField(payload, "imagePath");

            if (engine.empty()) engine = "vietocr";
            if (language.empty()) language = "vi";

            // Load image
            cv::Mat image;
            if (!image_b64.empty()) {
                // Decode base64
                std::vector<unsigned char> image_data = base64_decode(image_b64);
                if (image_data.empty()) {
                    return buildErrorResponse("Failed to decode base64 image");
                }
                image = cv::imdecode(image_data, cv::IMREAD_COLOR);
                
            } else if (!imagePath.empty()) {
                // Load from file
                image = cv::imread(imagePath);
                
            } else {
                return buildErrorResponse("Missing 'image' or 'imagePath' parameter");
            }

            if (image.empty()) {
                return buildErrorResponse("Failed to load image");
            }

            // Perform OCR
            std::cout << "[AAA0_0101_Worker] Running OCR on image " 
                      << image.cols << "x" << image.rows << std::endl;
            
            auto ocr_start = std::chrono::high_resolution_clock::now();
            std::string recognized_text = recognizer_->predict(image);
            auto ocr_end = std::chrono::high_resolution_clock::now();
            auto ocr_duration = std::chrono::duration_cast<std::chrono::milliseconds>(
                ocr_end - ocr_start
            ).count();

            auto end_time = std::chrono::high_resolution_clock::now();
            auto total_duration = std::chrono::duration_cast<std::chrono::milliseconds>(
                end_time - start_time
            ).count();

            std::cout << "[AAA0_0101_Worker] OCR completed in " << ocr_duration 
                      << "ms, text: \"" << recognized_text << "\"" << std::endl;

            // Build response
            std::ostringstream response;
            response << "{"
                    << "\"success\":true,"
                    << "\"text\":\"" << escapeJson(recognized_text) << "\","
                    << "\"time_ms\":" << ocr_duration << ","
                    << "\"total_ms\":" << total_duration << ","
                    << "\"engine\":\"" << engine << "\","
                    << "\"language\":\"" << language << "\","
                    << "\"worker\":\"AAA0_0101_W\","
                    << "\"image\":{\"width\":" << image.cols << ",\"height\":" << image.rows << "},"
                    << "\"timestamp\":" << std::time(nullptr)
                    << "}";

            return response.str();

        } catch (const std::exception& e) {
            std::cerr << "[AAA0_0101_Worker] Error: " << e.what() << std::endl;
            return buildErrorResponse(e.what());
        }
    }

private:
    std::string extractJsonField(const std::string& json, const std::string& field) {
        std::string search = "\"" + field + "\":";
        size_t pos = json.find(search);
        
        if (pos == std::string::npos) return "";

        pos += search.length();
        while (pos < json.length() && std::isspace(json[pos])) pos++;

        if (pos < json.length() && json[pos] == '"') {
            pos++;
            size_t end = json.find('"', pos);
            if (end != std::string::npos) {
                return json.substr(pos, end - pos);
            }
        }

        return "";
    }

    std::string escapeJson(const std::string& str) {
        std::ostringstream escaped;
        for (char c : str) {
            switch (c) {
                case '"': escaped << "\\\""; break;
                case '\\': escaped << "\\\\"; break;
                case '\n': escaped << "\\n"; break;
                case '\r': escaped << "\\r"; break;
                case '\t': escaped << "\\t"; break;
                default: escaped << c; break;
            }
        }
        return escaped.str();
    }

    std::string buildErrorResponse(const std::string& error_msg) {
        std::ostringstream response;
        response << "{"
                << "\"success\":false,"
                << "\"error\":\"" << escapeJson(error_msg) << "\","
                << "\"worker\":\"AAA0_0101_W\""
                << "}";
        return response.str();
    }
};

} // namespace workers
} // namespace deepapp

// Auto-register worker
REGISTER_WORKER(deepapp::workers::AAA0_0101_Worker, "AAA0_0101_W")
