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
#include <algorithm>
#include <cmath>

using json = nlohmann::json;

namespace deepapp {
namespace workers {

/**
 * ZZA0_0101_Worker - Vietnamese OCR Worker
 * Handles Vietnamese text extraction using ONNX models (CNN + Encoder + Decoder)
 */
class ZZA0_0101_Worker : public infrastructure::BaseWorker {
public:
    ZZA0_0101_Worker() : infrastructure::BaseWorker("ZZA0_0101_Worker") {
        std::cout << "[ZZA0_0101_Worker] Vietnamese OCR Worker Initialized" << std::endl;
        
        // Initialize ONNX Runtime
        initializeOnnxRuntime();
        
        // Load vocabulary
        loadVocabulary();
    }

    ~ZZA0_0101_Worker() {
        cleanupOnnxRuntime();
    }

    std::string processTask(const std::string& event_type, const std::string& payload) override {
        std::cout << "[ZZA0_0101_Worker] Processing OCR task:" << std::endl;
        std::cout << "  Event Type: " << event_type << std::endl;
        std::cout << "  Payload size: " << payload.size() << " bytes" << std::endl;

        try {
            json response;
            response["worker"] = "ZZA0_0101_W";
            response["timestamp"] = std::time(nullptr);

            if (event_type == "extract_text") {
                return extractText(payload);
            } else if (event_type == "extract_text_from_bboxes") {
                return extractText(payload);
            } else if (event_type == "test") {
                return testOcr(payload);
            } else if (event_type == "echo") {
                response["status"] = "success";
                response["data"] = "Echo: " + payload;
                return response.dump();
            } else if (event_type == "health_check") {
                response["status"] = "success";
                response["message"] = "VietOCR worker is healthy";
                response["models_loaded"] = (cnn_session_ != nullptr && 
                                            encoder_session_ != nullptr && 
                                            decoder_session_ != nullptr);
                response["vocab_size"] = vocab_.size();
                return response.dump();
            } else {
                response["status"] = "error";
                response["error"] = "Unknown event type: " + event_type;
                return response.dump();
            }

        } catch (const std::exception& e) {
            json error_response;
            error_response["worker"] = "ZZA0_0101_W";
            error_response["status"] = "error";
            error_response["error"] = std::string("Exception: ") + e.what();
            error_response["timestamp"] = std::time(nullptr);
            return error_response.dump();
        }
    }

private:
    // ONNX Runtime components
    Ort::Env* env_ = nullptr;
    Ort::Session* cnn_session_ = nullptr;
    Ort::Session* encoder_session_ = nullptr;
    Ort::Session* decoder_session_ = nullptr;
    std::vector<std::string> vocab_;
    
    // Token constants (correct values from research)
    static constexpr int64_t PAD_TOKEN = 0;
    static constexpr int64_t SOS_TOKEN = 1;
    static constexpr int64_t EOS_TOKEN = 2;
    static constexpr int64_t UNK_TOKEN = 3;
    static constexpr int MAX_SEQ_LENGTH = 128;
    static constexpr int IMAGE_HEIGHT = 32;

    /**
     * Load vocabulary from file
     */
    void loadVocabulary() {
        std::string vocab_path = "/root/deepapp/deepapp_main/src/main/resources/models/vietocr_onnx/vocab.txt";
        
        // Try alternative path if first fails
        std::ifstream vocab_file(vocab_path);
        if (!vocab_file.good()) {
            vocab_path = "src/main/resources/models/vietocr_onnx/vocab.txt";
            vocab_file.open(vocab_path);
        }
        
        if (!vocab_file.good()) {
            std::cerr << "[ZZA0_0101_Worker] ERROR: Vocabulary file not found" << std::endl;
            // Create default minimal vocab
            vocab_ = {"<pad>", "<sos>", "<eos>", "<unk>"};
            return;
        }

        vocab_.clear();
        std::string vocab_content;
        std::string line;
        while (std::getline(vocab_file, line)) {
            vocab_content += line;
        }
        
        // Special tokens first
        vocab_ = {"<pad>", "<sos>", "<eos>", "*"};
        
        // Parse UTF-8 string into individual characters
        size_t i = 0;
        while (i < vocab_content.size()) {
            size_t char_len = 1;
            if ((vocab_content[i] & 0x80) == 0) {
                // ASCII
                char_len = 1;
            } else if ((vocab_content[i] & 0xE0) == 0xC0) {
                // 2-byte UTF-8
                char_len = 2;
            } else if ((vocab_content[i] & 0xF0) == 0xE0) {
                // 3-byte UTF-8
                char_len = 3;
            } else if ((vocab_content[i] & 0xF8) == 0xF0) {
                // 4-byte UTF-8
                char_len = 4;
            }
            
            if (i + char_len <= vocab_content.size()) {
                vocab_.push_back(vocab_content.substr(i, char_len));
            }
            i += char_len;
        }

        std::cout << "[ZZA0_0101_Worker] ✓ Loaded vocabulary with " << vocab_.size() << " tokens" << std::endl;
        std::cout << "[ZZA0_0101_Worker]   - PAD=" << PAD_TOKEN << ", SOS=" << SOS_TOKEN 
                  << ", EOS=" << EOS_TOKEN << ", UNK=" << UNK_TOKEN << std::endl;
        if (vocab_.size() > 10) {
            std::cout << "[ZZA0_0101_Worker]   - Sample: vocab[0]='" << vocab_[0] << "', vocab[4]='" << vocab_[4] << "', vocab[5]='" << vocab_[5] << "'" << std::endl;
        }
    }

    /**
     * Initialize ONNX Runtime environment and load models
     */
    void initializeOnnxRuntime() {
        try {
            // Create ONNX Runtime environment
            env_ = new Ort::Env(ORT_LOGGING_LEVEL_WARNING, "VietOCR");

            // Configure session options
            Ort::SessionOptions session_options;
            session_options.SetIntraOpNumThreads(1);
            session_options.SetGraphOptimizationLevel(GraphOptimizationLevel::ORT_ENABLE_EXTENDED);

            // Model paths
            std::string base_path = "/root/deepapp/deepapp_main/src/main/resources/models/vietocr_onnx/";
            std::string cnn_path = base_path + "cnn.onnx";
            std::string encoder_path = base_path + "encoder.onnx";
            std::string decoder_path = base_path + "decoder.onnx";

            // Try alternative path if first fails
            std::ifstream test_file(cnn_path);
            if (!test_file.good()) {
                base_path = "src/main/resources/models/vietocr_onnx/";
                cnn_path = base_path + "cnn.onnx";
                encoder_path = base_path + "encoder.onnx";
                decoder_path = base_path + "decoder.onnx";
            }

            // Load CNN model
            std::cout << "[ZZA0_0101_Worker] Loading CNN model from: " << cnn_path << std::endl;
            cnn_session_ = new Ort::Session(*env_, cnn_path.c_str(), session_options);

            // Load Encoder model
            std::cout << "[ZZA0_0101_Worker] Loading Encoder model from: " << encoder_path << std::endl;
            encoder_session_ = new Ort::Session(*env_, encoder_path.c_str(), session_options);

            // Load Decoder model
            std::cout << "[ZZA0_0101_Worker] Loading Decoder model from: " << decoder_path << std::endl;
            decoder_session_ = new Ort::Session(*env_, decoder_path.c_str(), session_options);

            std::cout << "[ZZA0_0101_Worker] ✓ All ONNX models loaded successfully" << std::endl;

        } catch (const Ort::Exception& e) {
            std::cerr << "[ZZA0_0101_Worker] ERROR: Failed to initialize ONNX Runtime: " << e.what() << std::endl;
        } catch (const std::exception& e) {
            std::cerr << "[ZZA0_0101_Worker] ERROR: Exception during ONNX initialization: " << e.what() << std::endl;
        }
    }

    /**
     * Cleanup ONNX Runtime resources
     */
    void cleanupOnnxRuntime() {
        if (decoder_session_) {
            delete decoder_session_;
            decoder_session_ = nullptr;
        }
        if (encoder_session_) {
            delete encoder_session_;
            encoder_session_ = nullptr;
        }
        if (cnn_session_) {
            delete cnn_session_;
            cnn_session_ = nullptr;
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
        if (request.contains("image") && !request["image"].get<std::string>().empty()) {
            std::string base64_data = request["image"];
            std::vector<uint8_t> image_data = decodeBase64(base64_data);
            return cv::imdecode(image_data, cv::IMREAD_COLOR);
        } else if (request.contains("image_path") && !request["image_path"].get<std::string>().empty()) {
            std::string image_path = request["image_path"];
            std::cout << "[ZZA0_0101_Worker] Loading image from file: " << image_path << std::endl;
            return cv::imread(image_path, cv::IMREAD_COLOR);
        }
        return cv::Mat();
    }

    /**
     * Preprocess image for VietOCR (resize to height=32, maintain aspect ratio)
     */
    cv::Mat preprocessImage(const cv::Mat& image) {
        if (image.empty()) {
            throw std::runtime_error("Empty input image");
        }

        cv::Mat processed;

        // Calculate new width maintaining aspect ratio
        float aspect_ratio = static_cast<float>(image.cols) / image.rows;
        int new_width = static_cast<int>(IMAGE_HEIGHT * aspect_ratio);
        
        // Ensure minimum width
        if (new_width < 32) {
            new_width = 32;
        }

        // Resize to height=32
        cv::resize(image, processed, cv::Size(new_width, IMAGE_HEIGHT), 0, 0, cv::INTER_LANCZOS4);

        // Convert BGR to RGB
        cv::cvtColor(processed, processed, cv::COLOR_BGR2RGB);

        // Convert to float and normalize to [0, 1]
        processed.convertTo(processed, CV_32F, 1.0 / 255.0);

        return processed;
    }

    /**
     * Apply log softmax to decoder output
     */
    std::vector<float> logSoftmax(const std::vector<float>& logits) {
        // Find max for numerical stability
        float max_val = *std::max_element(logits.begin(), logits.end());
        
        // Compute exp(x - max)
        std::vector<float> exp_values(logits.size());
        float sum_exp = 0.0f;
        for (size_t i = 0; i < logits.size(); i++) {
            exp_values[i] = std::exp(logits[i] - max_val);
            sum_exp += exp_values[i];
        }
        
        // Compute log(softmax(x))
        std::vector<float> log_probs(logits.size());
        float log_sum_exp = std::log(sum_exp);
        for (size_t i = 0; i < logits.size(); i++) {
            log_probs[i] = (logits[i] - max_val) - log_sum_exp;
        }
        
        return log_probs;
    }

    /**
     * CORE FUNCTION: Run VietOCR inference on a single bounding box
     * This is the main implementation requested in the task
     */
    std::string runVietocrInference(const cv::Mat& bboxImage, 
                                   Ort::Session& cnnSession,
                                   Ort::Session& encoderSession, 
                                   Ort::Session& decoderSession,
                                   const std::vector<std::string>& vocab) {
        try {
            if (bboxImage.empty()) {
                std::cerr << "[VietOCR] Empty bbox image" << std::endl;
                return "";
            }

            std::cout << "[VietOCR] Processing bbox: " << bboxImage.cols << "x" << bboxImage.rows << std::endl;

            // Step 1: Preprocess image
            cv::Mat processed = preprocessImage(bboxImage);
            int width = processed.cols;
            int height = processed.rows;

            std::cout << "[VietOCR] Preprocessed image: " << width << "x" << height << std::endl;

            // Step 2: Prepare CNN input tensor [1, 3, 32, W]
            std::vector<int64_t> cnn_input_shape = {1, 3, IMAGE_HEIGHT, static_cast<int64_t>(width)};
            std::vector<float> cnn_input_data;
            cnn_input_data.reserve(3 * IMAGE_HEIGHT * width);

            // Convert to CHW format
            std::vector<cv::Mat> channels;
            cv::split(processed, channels);
            for (int c = 0; c < 3; c++) {
                for (int h = 0; h < IMAGE_HEIGHT; h++) {
                    for (int w = 0; w < width; w++) {
                        cnn_input_data.push_back(channels[c].at<float>(h, w));
                    }
                }
            }

            std::cout << "[VietOCR] CNN input shape: [" << cnn_input_shape[0] << ", " 
                      << cnn_input_shape[1] << ", " << cnn_input_shape[2] << ", " 
                      << cnn_input_shape[3] << "]" << std::endl;

            // Step 3: Run CNN
            Ort::MemoryInfo memory_info = Ort::MemoryInfo::CreateCpu(OrtArenaAllocator, OrtMemTypeDefault);
            Ort::Value cnn_input_tensor = Ort::Value::CreateTensor<float>(
                memory_info, cnn_input_data.data(), cnn_input_data.size(),
                cnn_input_shape.data(), cnn_input_shape.size());

            Ort::AllocatorWithDefaultOptions allocator;
            auto cnn_input_name = cnnSession.GetInputNameAllocated(0, allocator);
            auto cnn_output_name = cnnSession.GetOutputNameAllocated(0, allocator);
            
            const char* cnn_input_names[] = {cnn_input_name.get()};
            const char* cnn_output_names[] = {cnn_output_name.get()};

            auto cnn_outputs = cnnSession.Run(
                Ort::RunOptions{nullptr},
                cnn_input_names, &cnn_input_tensor, 1,
                cnn_output_names, 1);

            // Get CNN output shape and data
            auto cnn_output_shape = cnn_outputs[0].GetTensorTypeAndShapeInfo().GetShape();
            float* cnn_output_data = cnn_outputs[0].GetTensorMutableData<float>();
            
            size_t cnn_output_size = 1;
            for (auto dim : cnn_output_shape) {
                cnn_output_size *= dim;
            }

            std::cout << "[VietOCR] CNN output shape: [";
            for (auto dim : cnn_output_shape) {
                std::cout << dim << ", ";
            }
            std::cout << "]" << std::endl;

            // Step 4: Run Encoder (once)
            // CRITICAL: Use CNN output directly without reshape
            std::vector<float> encoder_input_data(cnn_output_data, cnn_output_data + cnn_output_size);
            
            Ort::Value encoder_input_tensor = Ort::Value::CreateTensor<float>(
                memory_info, encoder_input_data.data(), encoder_input_data.size(),
                cnn_output_shape.data(), cnn_output_shape.size());

            auto encoder_input_name = encoderSession.GetInputNameAllocated(0, allocator);
            auto encoder_output_name0 = encoderSession.GetOutputNameAllocated(0, allocator);
            auto encoder_output_name1 = encoderSession.GetOutputNameAllocated(1, allocator);
            
            const char* encoder_input_names[] = {encoder_input_name.get()};
            const char* encoder_output_names[] = {encoder_output_name0.get(), encoder_output_name1.get()};

            auto encoder_outputs = encoderSession.Run(
                Ort::RunOptions{nullptr},
                encoder_input_names, &encoder_input_tensor, 1,
                encoder_output_names, 2);

            // Extract encoder outputs
            auto encoder_output_shape = encoder_outputs[0].GetTensorTypeAndShapeInfo().GetShape();
            float* encoder_outputs_data = encoder_outputs[0].GetTensorMutableData<float>();
            
            auto hidden_shape = encoder_outputs[1].GetTensorTypeAndShapeInfo().GetShape();
            float* hidden_data = encoder_outputs[1].GetTensorMutableData<float>();

            size_t encoder_output_size = 1;
            for (auto dim : encoder_output_shape) {
                encoder_output_size *= dim;
            }
            
            size_t hidden_size = 1;
            for (auto dim : hidden_shape) {
                hidden_size *= dim;
            }

            std::cout << "[VietOCR] Encoder outputs shape: [";
            for (auto dim : encoder_output_shape) {
                std::cout << dim << ", ";
            }
            std::cout << "], hidden shape: [";
            for (auto dim : hidden_shape) {
                std::cout << dim << ", ";
            }
            std::cout << "]" << std::endl;

            // Step 5: Decoder loop
            std::vector<int64_t> predicted_tokens;
            predicted_tokens.push_back(SOS_TOKEN);

            // Current hidden state (copy from encoder)
            std::vector<float> current_hidden(hidden_data, hidden_data + hidden_size);
            std::vector<int64_t> current_hidden_shape = hidden_shape;

            // Encoder outputs (constant during decoding)
            std::vector<float> encoder_memory(encoder_outputs_data, encoder_outputs_data + encoder_output_size);
            std::vector<int64_t> encoder_memory_shape = encoder_output_shape;

            auto decoder_input_name0 = decoderSession.GetInputNameAllocated(0, allocator);
            auto decoder_input_name1 = decoderSession.GetInputNameAllocated(1, allocator);
            auto decoder_input_name2 = decoderSession.GetInputNameAllocated(2, allocator);
            auto decoder_output_name0 = decoderSession.GetOutputNameAllocated(0, allocator);
            auto decoder_output_name1 = decoderSession.GetOutputNameAllocated(1, allocator);
            
            const char* decoder_input_names[] = {
                decoder_input_name0.get(),
                decoder_input_name1.get(),
                decoder_input_name2.get()
            };
            const char* decoder_output_names[] = {
                decoder_output_name0.get(),
                decoder_output_name1.get()
            };

            for (int step = 0; step < MAX_SEQ_LENGTH; step++) {
                // Prepare decoder inputs
                // CRITICAL: tgt shape is {1} (scalar), NOT {1,1}
                std::vector<int64_t> tgt_shape = {1};
                std::vector<int64_t> tgt_data = {predicted_tokens.back()};

                Ort::Value tgt_tensor = Ort::Value::CreateTensor<int64_t>(
                    memory_info, tgt_data.data(), tgt_data.size(),
                    tgt_shape.data(), tgt_shape.size());

                Ort::Value hidden_tensor = Ort::Value::CreateTensor<float>(
                    memory_info, current_hidden.data(), current_hidden.size(),
                    current_hidden_shape.data(), current_hidden_shape.size());

                Ort::Value encoder_outputs_tensor = Ort::Value::CreateTensor<float>(
                    memory_info, encoder_memory.data(), encoder_memory.size(),
                    encoder_memory_shape.data(), encoder_memory_shape.size());

                std::vector<Ort::Value> decoder_inputs;
                decoder_inputs.push_back(std::move(tgt_tensor));
                decoder_inputs.push_back(std::move(hidden_tensor));
                decoder_inputs.push_back(std::move(encoder_outputs_tensor));

                // Run decoder
                auto decoder_outputs = decoderSession.Run(
                    Ort::RunOptions{nullptr},
                    decoder_input_names, decoder_inputs.data(), 3,
                    decoder_output_names, 2);

                // Extract decoder outputs
                auto output_shape = decoder_outputs[0].GetTensorTypeAndShapeInfo().GetShape();
                float* output_data = decoder_outputs[0].GetTensorMutableData<float>();
                
                size_t output_size = 1;
                for (auto dim : output_shape) {
                    output_size *= dim;
                }

                // Apply log_softmax and get argmax
                std::vector<float> logits(output_data, output_data + output_size);
                std::vector<float> log_probs = logSoftmax(logits);
                
                auto max_it = std::max_element(log_probs.begin(), log_probs.end());
                int64_t next_token = std::distance(log_probs.begin(), max_it);

                predicted_tokens.push_back(next_token);

                // Check for EOS token
                if (next_token == EOS_TOKEN) {
                    std::cout << "[VietOCR] Reached EOS token at step " << step << std::endl;
                    break;
                }

                // Update hidden state for next step
                auto new_hidden_shape = decoder_outputs[1].GetTensorTypeAndShapeInfo().GetShape();
                float* new_hidden_data = decoder_outputs[1].GetTensorMutableData<float>();
                
                size_t new_hidden_size = 1;
                for (auto dim : new_hidden_shape) {
                    new_hidden_size *= dim;
                }
                
                current_hidden.assign(new_hidden_data, new_hidden_data + new_hidden_size);
                current_hidden_shape = new_hidden_shape;
            }

            std::cout << "[VietOCR] Predicted " << predicted_tokens.size() << " tokens" << std::endl;

            // Step 6: Decode tokens to text
            std::string result;
            for (size_t i = 1; i < predicted_tokens.size(); i++) {  // Skip SOS token
                int64_t token = predicted_tokens[i];
                
                if (token == EOS_TOKEN) {
                    break;  // Stop at EOS
                }
                
                if (token >= 0 && token < static_cast<int64_t>(vocab.size())) {
                    result += vocab[token];
                } else {
                    std::cerr << "[VietOCR] Warning: Token " << token << " out of vocab range" << std::endl;
                    result += "*";  // Use UNK symbol
                }
            }

            std::cout << "[VietOCR] Extracted text: " << result << std::endl;
            return result;

        } catch (const Ort::Exception& e) {
            std::cerr << "[VietOCR] ONNX error: " << e.what() << std::endl;
            return "";
        } catch (const std::exception& e) {
            std::cerr << "[VietOCR] Error: " << e.what() << std::endl;
            return "";
        }
    }

    /**
     * Extract text from image with bounding boxes
     */
    std::string extractText(const std::string& payload) {
        try {
            json request = json::parse(payload);
            json response;

            response["worker"] = "ZZA0_0101_W";
            response["status"] = "success";
            response["timestamp"] = std::time(nullptr);

            // Check if models are loaded
            if (!cnn_session_ || !encoder_session_ || !decoder_session_) {
                response["status"] = "error";
                response["error"] = "ONNX models not loaded";
                return response.dump();
            }

            // Load image
            cv::Mat image = loadImage(request);
            if (image.empty()) {
                response["status"] = "error";
                response["error"] = "Failed to load image";
                return response.dump();
            }

            response["dimensions"]["width"] = image.cols;
            response["dimensions"]["height"] = image.rows;

            std::cout << "[ZZA0_0101_Worker] Processing image: " << image.cols << "x" << image.rows << std::endl;

            // Extract text from bounding boxes
            json results = json::array();
            
            if (request.contains("bboxes") && request["bboxes"].is_array()) {
                auto start_time = std::chrono::high_resolution_clock::now();

                for (const auto& bbox : request["bboxes"]) {
                    int x1 = bbox["x1"].get<int>();
                    int y1 = bbox["y1"].get<int>();
                    int x2 = bbox["x2"].get<int>();
                    int y2 = bbox["y2"].get<int>();

                    // Validate and clip bbox
                    x1 = std::max(0, std::min(x1, image.cols - 1));
                    y1 = std::max(0, std::min(y1, image.rows - 1));
                    x2 = std::max(0, std::min(x2, image.cols));
                    y2 = std::max(0, std::min(y2, image.rows));

                    if (x2 <= x1 || y2 <= y1) {
                        std::cerr << "[ZZA0_0101_Worker] Invalid bbox: (" << x1 << "," << y1 
                                  << "," << x2 << "," << y2 << ")" << std::endl;
                        continue;
                    }

                    // Extract bbox region
                    cv::Mat bbox_image = image(cv::Rect(x1, y1, x2 - x1, y2 - y1));

                    // Run OCR inference
                    std::string text = runVietocrInference(
                        bbox_image, 
                        *cnn_session_, 
                        *encoder_session_, 
                        *decoder_session_, 
                        vocab_
                    );

                    json result;
                    result["bbox"]["x1"] = x1;
                    result["bbox"]["y1"] = y1;
                    result["bbox"]["x2"] = x2;
                    result["bbox"]["y2"] = y2;
                    result["text"] = text;
                    if (bbox.contains("label")) {
                        result["label"] = bbox["label"];
                    }

                    results.push_back(result);
                }

                auto end_time = std::chrono::high_resolution_clock::now();
                auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end_time - start_time);
                response["processingTime"] = duration.count();

                std::cout << "[ZZA0_0101_Worker] Processed " << results.size() 
                          << " bounding boxes in " << duration.count() << "ms" << std::endl;
            }

            response["results"] = results;
            return response.dump();

        } catch (const std::exception& e) {
            json error_response;
            error_response["worker"] = "ZZA0_0101_W";
            error_response["status"] = "error";
            error_response["error"] = std::string("Text extraction failed: ") + e.what();
            error_response["timestamp"] = std::time(nullptr);
            return error_response.dump();
        }
    }

    /**
     * Test OCR method for validation
     */
    std::string testOcr(const std::string& payload) {
        try {
            json request = json::parse(payload);
            json response;

            response["worker"] = "ZZA0_0101_W";
            response["status"] = "success";
            response["timestamp"] = std::time(nullptr);
            response["event"] = "test";

            // Check if models are loaded
            response["models_loaded"] = (cnn_session_ != nullptr && 
                                        encoder_session_ != nullptr && 
                                        decoder_session_ != nullptr);
            response["vocab_size"] = vocab_.size();

            if (!cnn_session_ || !encoder_session_ || !decoder_session_) {
                response["status"] = "error";
                response["error"] = "ONNX models not loaded";
                return response.dump();
            }

            // Load image
            cv::Mat image = loadImage(request);
            if (image.empty()) {
                response["status"] = "error";
                response["error"] = "Failed to load image";
                return response.dump();
            }

            response["dimensions"]["width"] = image.cols;
            response["dimensions"]["height"] = image.rows;

            // Test with a single bbox or full image
            cv::Mat test_image;
            if (request.contains("bbox")) {
                int x1 = request["bbox"]["x1"].get<int>();
                int y1 = request["bbox"]["y1"].get<int>();
                int x2 = request["bbox"]["x2"].get<int>();
                int y2 = request["bbox"]["y2"].get<int>();

                x1 = std::max(0, std::min(x1, image.cols - 1));
                y1 = std::max(0, std::min(y1, image.rows - 1));
                x2 = std::max(0, std::min(x2, image.cols));
                y2 = std::max(0, std::min(y2, image.rows));

                if (x2 > x1 && y2 > y1) {
                    test_image = image(cv::Rect(x1, y1, x2 - x1, y2 - y1));
                }
            } else {
                test_image = image;
            }

            if (test_image.empty()) {
                response["status"] = "error";
                response["error"] = "Invalid test image";
                return response.dump();
            }

            // Run OCR
            auto start_time = std::chrono::high_resolution_clock::now();
            std::string text = runVietocrInference(
                test_image, 
                *cnn_session_, 
                *encoder_session_, 
                *decoder_session_, 
                vocab_
            );
            auto end_time = std::chrono::high_resolution_clock::now();

            auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end_time - start_time);

            response["text"] = text;
            response["processingTime"] = duration.count();

            std::cout << "[ZZA0_0101_Worker] ✓ Test OCR completed: \"" << text 
                      << "\" (" << duration.count() << "ms)" << std::endl;

            return response.dump();

        } catch (const std::exception& e) {
            json error_response;
            error_response["worker"] = "ZZA0_0101_W";
            error_response["status"] = "error";
            error_response["error"] = std::string("Test OCR failed: ") + e.what();
            error_response["timestamp"] = std::time(nullptr);
            return error_response.dump();
        }
    }
};

// Register the worker
REGISTER_WORKER(deepapp::workers::ZZA0_0101_Worker, "ZZA0_0101_W")

} // namespace workers
} // namespace deepapp