#include "com/deepapp/infrastructure/BaseWorker.h"
#include "com/deepapp/infrastructure/WorkerRegistry.h"
#include "llama.h"
#include <nlohmann/json.hpp>
#include <iostream>
#include <fstream>
#include <string>
#include <vector>
#include <memory>
#include <chrono>

using json = nlohmann::json;

namespace deepapp {
namespace workers {

/**
 * AAA0_0300_CPP_W - LLM Inference Worker (C++)
 * Vietnamese Language Model inference using llama.cpp native library
 */
class AAA0_0300_Worker : public infrastructure::BaseWorker {
private:
    // Model cache
    struct ModelContext {
        llama_model* model;
        llama_context* ctx;
        std::string name;
    };
    
    std::map<std::string, std::shared_ptr<ModelContext>> models_;
    std::string default_model_path_;
    
    // Statistics
    int inference_count_;
    int total_tokens_;
    
public:
    AAA0_0300_Worker() 
        : BaseWorker("AAA0_0300_CPP_Worker"),
          default_model_path_("/root/models/vinallama-7b-chat_q5_0.gguf"),
          inference_count_(0),
          total_tokens_(0) {
        
        std::cout << "[AAA0_0300_CPP_W] === LLM Inference Worker (C++) ===" << std::endl;
        
        // Initialize llama backend
        llama_backend_init(false);
        
        std::cout << "[AAA0_0300_CPP_W] ✓ llama.cpp backend initialized" << std::endl;
        std::cout << "[AAA0_0300_CPP_W] ✓ Worker initialized" << std::endl;
    }
    
    ~AAA0_0300_Worker() {
        // Cleanup all models
        for (auto& pair : models_) {
            if (pair.second->ctx) {
                llama_free(pair.second->ctx);
            }
            if (pair.second->model) {
                llama_free_model(pair.second->model);
            }
        }
        models_.clear();
        
        llama_backend_free();
        std::cout << "[AAA0_0300_CPP_W] Worker destroyed, models cleaned up" << std::endl;
    }
    
    std::string processTask(const std::string& event_type, 
                           const std::string& payload) override {
        std::cout << "[AAA0_0300_CPP_W] 📨 Event: " << event_type << std::endl;
        
        try {
            if (event_type == "inference") {
                return runInference(payload);
            }
            else if (event_type == "load_model") {
                return loadModel(payload);
            }
            else if (event_type == "unload_model") {
                return unloadModel(payload);
            }
            else if (event_type == "list_models") {
                return listModels();
            }
            else if (event_type == "get_stats") {
                return getStats();
            }
            else {
                return createResponse("error", 
                    "Unknown event type: " + event_type);
            }
        }
        catch (const std::exception& e) {
            std::cerr << "[AAA0_0300_CPP_W] ✗ Error: " << e.what() << std::endl;
            return createResponse("error", std::string(e.what()));
        }
    }
    
    bool canHandle(const std::string& event_type) const override {
        return event_type == "inference" || 
               event_type == "load_model" ||
               event_type == "unload_model" ||
               event_type == "list_models" ||
               event_type == "get_stats";
    }
    
private:
    std::string runInference(const std::string& payload) {
        auto start_time = std::chrono::high_resolution_clock::now();
        
        try {
            json data = json::parse(payload);
            std::string prompt = data.value("prompt", "");
            double temperature = data.value("temperature", 0.1);
            int max_tokens = data.value("max_tokens", 200);
            std::string model_name = data.value("model_name", "vinallama-7b-chat");
            
            std::cout << "[AAA0_0300_CPP_W] 🤖 Running inference - Model: " 
                     << model_name << ", Tokens: " << max_tokens << std::endl;
            
            // Load model if not loaded
            if (models_.find(model_name) == models_.end()) {
                loadModelInternal(model_name);
            }
            
            auto model_ctx = models_[model_name];
            if (!model_ctx || !model_ctx->model || !model_ctx->ctx) {
                throw std::runtime_error("Model not available: " + model_name);
            }
            
            // Build prompt
            std::string full_prompt = buildPrompt(prompt, data);
            
            std::cout << "[AAA0_0300_CPP_W] 📝 Prompt length: " 
                     << full_prompt.length() << " chars" << std::endl;
            
            // Tokenize prompt
            std::vector<llama_token> tokens = tokenize(model_ctx->model, full_prompt);
            
            // Generate response
            std::string response = generate(model_ctx, tokens, max_tokens, temperature);
            
            auto end_time = std::chrono::high_resolution_clock::now();
            auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(
                end_time - start_time).count();
            
            // Update statistics
            inference_count_++;
            total_tokens_ += response.length() / 4; // Rough estimate
            
            std::cout << "[AAA0_0300_CPP_W] ✅ Inference completed in " 
                     << (duration / 1000.0) << "s" << std::endl;
            
            json result;
            result["status"] = "success";
            result["response"] = response;
            result["tokens"] = static_cast<int>(response.length() / 4);
            result["inference_time"] = duration / 1000.0;
            result["model"] = model_name;
            
            return result.dump();
        }
        catch (const std::exception& e) {
            std::cerr << "[AAA0_0300_CPP_W] ❌ Inference failed: " 
                     << e.what() << std::endl;
            return createResponse("error", std::string(e.what()));
        }
    }
    
    std::string buildPrompt(const std::string& prompt, const json& data) {
        std::string full_prompt;
        
        // Add chat history if provided
        if (data.contains("chat_history")) {
            for (const auto& msg : data["chat_history"]) {
                std::string role = msg.value("role", "user");
                std::string content = msg.value("content", "");
                
                if (role == "user") {
                    full_prompt += "<|im_start|>user\n" + content + "<|im_end|>\n";
                } else if (role == "assistant") {
                    full_prompt += "<|im_start|>assistant\n" + content + "<|im_end|>\n";
                }
            }
        }
        
        // Add current prompt
        full_prompt += "<|im_start|>user\n" + prompt + "<|im_end|>\n<|im_start|>assistant\n";
        
        return full_prompt;
    }
    
    std::vector<llama_token> tokenize(llama_model* model, const std::string& text) {
        int n_tokens = text.length() + 100; // Rough estimate
        std::vector<llama_token> tokens(n_tokens);
        
        int actual_tokens = llama_tokenize(
            model,
            text.c_str(),
            text.length(),
            tokens.data(),
            tokens.size(),
            true,  // add_bos
            false  // special
        );
        
        if (actual_tokens < 0) {
            tokens.resize(-actual_tokens);
            actual_tokens = llama_tokenize(
                model,
                text.c_str(),
                text.length(),
                tokens.data(),
                tokens.size(),
                true,
                false
            );
        }
        
        tokens.resize(actual_tokens);
        return tokens;
    }
    
    std::string generate(std::shared_ptr<ModelContext> model_ctx, 
                        std::vector<llama_token>& prompt_tokens,
                        int max_tokens,
                        float temperature) {
        
        std::string result;
        
        // Evaluate prompt
        llama_batch batch = llama_batch_get_one(
            prompt_tokens.data(),
            prompt_tokens.size(),
            0,
            0
        );
        
        if (llama_decode(model_ctx->ctx, batch) != 0) {
            throw std::runtime_error("Failed to decode prompt");
        }
        
        // Generate tokens
        std::vector<std::string> stop_sequences = {"<|im_end|>", "</s>"};
        
        for (int i = 0; i < max_tokens; i++) {
            // Sample next token
            llama_token new_token = sampleToken(model_ctx->ctx, temperature);
            
            // Check for EOS
            if (new_token == llama_token_eos(model_ctx->model)) {
                break;
            }
            
            // Convert token to text
            char buf[256];
            int n = llama_token_to_piece(
                model_ctx->model,
                new_token,
                buf,
                sizeof(buf)
            );
            
            if (n > 0) {
                std::string token_text(buf, n);
                result += token_text;
                
                // Check for stop sequences
                bool should_stop = false;
                for (const auto& stop : stop_sequences) {
                    if (result.find(stop) != std::string::npos) {
                        should_stop = true;
                        break;
                    }
                }
                if (should_stop) break;
            }
            
            // Prepare next token for evaluation
            prompt_tokens.push_back(new_token);
            
            batch = llama_batch_get_one(&new_token, 1, prompt_tokens.size() - 1, 0);
            if (llama_decode(model_ctx->ctx, batch) != 0) {
                break;
            }
        }
        
        // Clean up stop sequences from result
        for (const auto& stop : stop_sequences) {
            size_t pos = result.find(stop);
            if (pos != std::string::npos) {
                result = result.substr(0, pos);
            }
        }
        
        return result;
    }
    
    llama_token sampleToken(llama_context* ctx, float temperature) {
        auto* logits = llama_get_logits_ith(ctx, -1);
        int n_vocab = llama_n_vocab(llama_get_model(ctx));
        
        std::vector<llama_token_data> candidates;
        candidates.reserve(n_vocab);
        
        for (llama_token token_id = 0; token_id < n_vocab; token_id++) {
            candidates.push_back({token_id, logits[token_id], 0.0f});
        }
        
        llama_token_data_array candidates_p = {
            candidates.data(),
            candidates.size(),
            false
        };
        
        // Sample with temperature
        llama_sample_temp(ctx, &candidates_p, temperature);
        return llama_sample_token(ctx, &candidates_p);
    }
    
    void loadModelInternal(const std::string& model_name) {
        std::cout << "[AAA0_0300_CPP_W] 🔄 Loading model: " << model_name << std::endl;
        
        // Map model names to file paths
        std::map<std::string, std::string> model_paths = {
            {"vinallama-7b-chat", "/root/models/vinallama-7b-chat_q5_0.gguf"},
            {"vietcuna-7b", "/root/models/vietcuna-7b-q5_k_m.gguf"},
            {"phobert-base", "/root/models/phobert-base.gguf"}
        };
        
        std::string model_path = model_paths.count(model_name) 
            ? model_paths[model_name] 
            : default_model_path_;
        
        // Check if file exists
        std::ifstream file(model_path);
        if (!file.good()) {
            throw std::runtime_error("Model file not found: " + model_path);
        }
        
        // Load model
        llama_model_params model_params = llama_model_default_params();
        llama_model* model = llama_load_model_from_file(model_path.c_str(), model_params);
        
        if (!model) {
            throw std::runtime_error("Failed to load model: " + model_path);
        }
        
        // Create context
        llama_context_params ctx_params = llama_context_default_params();
        ctx_params.n_ctx = 2048;
        ctx_params.n_batch = 512;
        ctx_params.n_threads = 4;
        
        llama_context* ctx = llama_new_context_with_model(model, ctx_params);
        
        if (!ctx) {
            llama_free_model(model);
            throw std::runtime_error("Failed to create context for model: " + model_name);
        }
        
        // Store model context
        auto model_ctx = std::make_shared<ModelContext>();
        model_ctx->model = model;
        model_ctx->ctx = ctx;
        model_ctx->name = model_name;
        
        models_[model_name] = model_ctx;
        
        std::cout << "[AAA0_0300_CPP_W] ✅ Model " << model_name 
                 << " loaded successfully" << std::endl;
    }
    
    std::string loadModel(const std::string& payload) {
        json data = json::parse(payload);
        std::string model_name = data.value("model_name", "vinallama-7b-chat");
        
        loadModelInternal(model_name);
        
        return createResponse("success", "Model " + model_name + " loaded");
    }
    
    std::string unloadModel(const std::string& payload) {
        json data = json::parse(payload);
        std::string model_name = data.value("model_name", "vinallama-7b-chat");
        
        auto it = models_.find(model_name);
        if (it != models_.end()) {
            if (it->second->ctx) llama_free(it->second->ctx);
            if (it->second->model) llama_free_model(it->second->model);
            models_.erase(it);
            
            std::cout << "[AAA0_0300_CPP_W] Model " << model_name << " unloaded" << std::endl;
            return createResponse("success", "Model " + model_name + " unloaded");
        }
        
        return createResponse("error", "Model " + model_name + " not loaded");
    }
    
    std::string listModels() {
        json result;
        result["status"] = "success";
        
        std::vector<std::string> loaded;
        for (const auto& pair : models_) {
            loaded.push_back(pair.first);
        }
        result["loaded_models"] = loaded;
        
        result["available_models"] = {
            "vinallama-7b-chat",
            "vietcuna-7b",
            "phobert-base"
        };
        
        return result.dump();
    }
    
    std::string getStats() {
        json result;
        result["status"] = "success";
        result["inference_count"] = inference_count_;
        result["total_tokens"] = total_tokens_;
        result["avg_tokens"] = inference_count_ > 0 
            ? static_cast<double>(total_tokens_) / inference_count_ 
            : 0.0;
        
        std::vector<std::string> loaded;
        for (const auto& pair : models_) {
            loaded.push_back(pair.first);
        }
        result["loaded_models"] = loaded;
        
        return result.dump();
    }
};

} // namespace workers
} // namespace deepapp

// Register worker
REGISTER_WORKER(deepapp::workers::AAA0_0300_Worker, "AAA0_0300_CPP_W")
