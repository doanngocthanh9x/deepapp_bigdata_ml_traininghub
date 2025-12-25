/**
 * AAA0_0101 OCR Worker
 * 
 * Vietnamese OCR worker using VietOCR ONNX models
 * Handles text recognition from images via gRPC
 * 
 * Expected input JSON format:
 * {
 *   "image": "base64_encoded_image_data",  // Base64 image string
 *   "imagePath": "/path/to/image.jpg",     // Or file path (for testing)
 *   "language": "vi",                      // Language code (vi/en)
 *   "engine": "vietocr"                    // OCR engine (vietocr/paddleocr)
 * }
 * 
 * Output JSON format:
 * {
 *   "success": true,
 *   "text": "Recognized text here",
 *   "time_ms": 597,
 *   "engine": "vietocr",
 *   "language": "vi",
 *   "worker": "AAA0_0101_W"
 * }
 */

#include "com/deepapp/infrastructure/BaseWorker.h"
#include "com/deepapp/infrastructure/WorkerRegistry.h"
#include <iostream>
#include <sstream>
#include <string>
#include <memory>
#include <chrono>

namespace deepapp {
namespace workers {

/**
 * VietOCR Worker Implementation
 */
class AAA0_0101_Worker : public infrastructure::BaseWorker {
public:
    AAA0_0101_Worker() : BaseWorker("AAA0_0101_Worker") {
        std::cout << "[AAA0_0101_Worker] VietOCR Worker Initialized" << std::endl;
        // TODO: Initialize VietOCR models here in future
        // For now, this is a placeholder that returns mock data
    }

    /**
     * Process OCR task
     */
    std::string processTask(
        const std::string& eventType, 
        const std::string& payload) override {
        
        std::cout << "[AAA0_0101_Worker] Processing OCR task:" << std::endl;
        std::cout << "  Event Type: " << eventType << std::endl;
        std::cout << "  Payload length: " << payload.length() << " bytes" << std::endl;

        auto start_time = std::chrono::high_resolution_clock::now();

        try {
            // Parse input JSON (simple string parsing for now)
            std::string engine = extractJsonField(payload, "engine");
            std::string language = extractJsonField(payload, "language");
            std::string image = extractJsonField(payload, "image");
            std::string imagePath = extractJsonField(payload, "imagePath");

            if (engine.empty()) engine = "vietocr";
            if (language.empty()) language = "vi";

            std::cout << "  Engine: " << engine << std::endl;
            std::cout << "  Language: " << language << std::endl;

            // TODO: Implement actual VietOCR processing
            // For now, return mock response
            std::string recognizedText = "Văn bản tiếng Việt được nhận diện - Mock Data";
            
            if (!imagePath.empty()) {
                recognizedText = "Image from path: " + imagePath + " - Mock OCR result";
            } else if (!image.empty()) {
                recognizedText = "Base64 image decoded - Mock OCR result (length: " + 
                               std::to_string(image.length()) + " chars)";
            }

            auto end_time = std::chrono::high_resolution_clock::now();
            auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(
                end_time - start_time
            ).count();

            // Build response JSON
            std::ostringstream response;
            response << "{"
                    << "\"success\":true,"
                    << "\"text\":\"" << escapeJson(recognizedText) << "\","
                    << "\"time_ms\":" << duration << ","
                    << "\"engine\":\"" << engine << "\","
                    << "\"language\":\"" << language << "\","
                    << "\"worker\":\"AAA0_0101_W\","
                    << "\"timestamp\":" << std::time(nullptr)
                    << "}";

            std::string result = response.str();
            std::cout << "[AAA0_0101_Worker] OCR completed in " << duration << "ms" << std::endl;
            std::cout << "[AAA0_0101_Worker] Result: " << result << std::endl;

            return result;

        } catch (const std::exception& e) {
            std::cerr << "[AAA0_0101_Worker] Error: " << e.what() << std::endl;
            
            std::ostringstream error_response;
            error_response << "{"
                          << "\"success\":false,"
                          << "\"error\":\"" << escapeJson(e.what()) << "\","
                          << "\"worker\":\"AAA0_0101_W\""
                          << "}";
            return error_response.str();
        }
    }

private:
    /**
     * Extract field value from simple JSON string
     * NOTE: This is a simple parser, not production-ready
     */
    std::string extractJsonField(const std::string& json, const std::string& field) {
        std::string search = "\"" + field + "\":";
        size_t pos = json.find(search);
        
        if (pos == std::string::npos) {
            return "";
        }

        pos += search.length();
        
        // Skip whitespace
        while (pos < json.length() && std::isspace(json[pos])) {
            pos++;
        }

        // Check if value is quoted string
        if (pos < json.length() && json[pos] == '"') {
            pos++; // Skip opening quote
            size_t end = json.find('"', pos);
            if (end != std::string::npos) {
                return json.substr(pos, end - pos);
            }
        }

        return "";
    }

    /**
     * Escape special characters for JSON
     */
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
};

} // namespace workers
} // namespace deepapp

// Auto-register this worker with task ID "AAA0_0101_W"
REGISTER_WORKER(deepapp::workers::AAA0_0101_Worker, "AAA0_0101_W")
