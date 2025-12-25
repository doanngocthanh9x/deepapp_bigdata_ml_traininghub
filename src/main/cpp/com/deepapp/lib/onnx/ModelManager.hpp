/**
 * Model Manager
 * 
 * Manages ONNX model files from GitLab repository
 * Features:
 * - Check if model exists locally
 * - Download model from GitLab if missing
 * - Verify model integrity
 * - Cache management
 */

#pragma once

#include <string>
#include <vector>
#include <map>
#include <filesystem>
#include <iostream>
#include <fstream>
#include <cstdlib>

namespace deepapp {
namespace lib {
namespace onnx {

class ModelManager {
public:
    /**
     * Get singleton instance
     */
    static ModelManager& getInstance() {
        static ModelManager instance;
        return instance;
    }

    /**
     * Get model path - will download if not exists
     * @param model_name Model name in repository (e.g., "vietocr/encoder.onnx")
     * @return Full local path to model file
     * @throws std::runtime_error if download fails
     */
    std::string getModelPath(const std::string& model_name) {
        std::string local_path = models_dir_ + "/" + model_name;
        
        // Check if model exists
        if (modelExists(local_path)) {
            std::cout << "[ModelManager] Model found: " << local_path << std::endl;
            return local_path;
        }
        
        // Download model
        std::cout << "[ModelManager] Model not found, downloading: " << model_name << std::endl;
        downloadModel(model_name, local_path);
        
        // Verify download
        if (!modelExists(local_path)) {
            throw std::runtime_error("Failed to download model: " + model_name);
        }
        
        std::cout << "[ModelManager] Model downloaded successfully: " << local_path << std::endl;
        return local_path;
    }

    /**
     * Check if model exists locally
     */
    bool modelExists(const std::string& local_path) {
        return std::filesystem::exists(local_path) && 
               std::filesystem::is_regular_file(local_path) &&
               std::filesystem::file_size(local_path) > 0;
    }

    /**
     * Set models directory
     */
    void setModelsDir(const std::string& dir) {
        models_dir_ = dir;
        std::filesystem::create_directories(models_dir_);
    }

    /**
     * Set GitLab repository info
     */
    void setGitLabRepo(const std::string& repo_url, const std::string& branch = "main") {
        gitlab_repo_url_ = repo_url;
        gitlab_branch_ = branch;
    }

    /**
     * Set GitLab access token (optional, for private repos)
     */
    void setGitLabToken(const std::string& token) {
        gitlab_token_ = token;
    }

    /**
     * Download specific model from GitLab
     */
    void downloadModel(const std::string& model_name, const std::string& local_path) {
        // Create directory if not exists
        std::filesystem::path path(local_path);
        std::filesystem::create_directories(path.parent_path());
        
        // Build download URL
        // Format: https://gitlab.com/user/repo/-/raw/branch/path/to/file
        std::string base_url = gitlab_repo_url_;
        if (base_url.back() == '/') {
            base_url.pop_back();
        }
        
        std::string download_url = base_url + "/-/raw/" + gitlab_branch_ + "/" + model_name;
        
        // Build curl command
        std::string curl_cmd = "curl -L -f -s";
        
        // Add authentication if token provided
        if (!gitlab_token_.empty()) {
            curl_cmd += " -H \"PRIVATE-TOKEN: " + gitlab_token_ + "\"";
        }
        
        curl_cmd += " -o \"" + local_path + "\" \"" + download_url + "\"";
        
        std::cout << "[ModelManager] Downloading from: " << download_url << std::endl;
        
        // Execute download
        int result = std::system(curl_cmd.c_str());
        
        if (result != 0) {
            throw std::runtime_error(
                "Failed to download model from GitLab: " + model_name + 
                " (curl exit code: " + std::to_string(result) + ")"
            );
        }
    }

    /**
     * Download all models for a specific service
     * @param model_names List of model names to download
     */
    void downloadModels(const std::vector<std::string>& model_names) {
        for (const auto& model_name : model_names) {
            try {
                getModelPath(model_name);
            } catch (const std::exception& e) {
                std::cerr << "[ModelManager] Failed to download " << model_name 
                         << ": " << e.what() << std::endl;
                throw;
            }
        }
    }

    /**
     * List all local models
     */
    std::vector<std::string> listLocalModels() {
        std::vector<std::string> models;
        
        if (!std::filesystem::exists(models_dir_)) {
            return models;
        }
        
        for (const auto& entry : std::filesystem::recursive_directory_iterator(models_dir_)) {
            if (entry.is_regular_file()) {
                std::string relative_path = std::filesystem::relative(
                    entry.path(), models_dir_
                ).string();
                models.push_back(relative_path);
            }
        }
        
        return models;
    }

    /**
     * Clear model cache
     */
    void clearCache() {
        if (std::filesystem::exists(models_dir_)) {
            std::filesystem::remove_all(models_dir_);
            std::filesystem::create_directories(models_dir_);
            std::cout << "[ModelManager] Model cache cleared" << std::endl;
        }
    }

    /**
     * Get cache size in bytes
     */
    size_t getCacheSize() {
        size_t total_size = 0;
        
        if (!std::filesystem::exists(models_dir_)) {
            return 0;
        }
        
        for (const auto& entry : std::filesystem::recursive_directory_iterator(models_dir_)) {
            if (entry.is_regular_file()) {
                total_size += std::filesystem::file_size(entry.path());
            }
        }
        
        return total_size;
    }

private:
    ModelManager() {
        // Default configuration
        models_dir_ = "/app/models";
        gitlab_repo_url_ = "https://gitlab.com/dnt.doanngocthanh/deepappmodels";
        gitlab_branch_ = "main";
        
        // Try to get token from environment
        const char* token_env = std::getenv("GITLAB_TOKEN");
        if (token_env) {
            gitlab_token_ = token_env;
        }
        
        // Create models directory
        std::filesystem::create_directories(models_dir_);
    }

    std::string models_dir_;
    std::string gitlab_repo_url_;
    std::string gitlab_branch_;
    std::string gitlab_token_;
};

} // namespace onnx
} // namespace lib
} // namespace deepapp
