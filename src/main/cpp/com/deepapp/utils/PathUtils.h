#pragma once

#include <string>
#include <filesystem>

namespace deepapp {
namespace utils {

/**
 * Path utilities for consistent path handling across the project
 */
class PathUtils {
public:
    /**
     * Get the base path of the project (/root/deepapp/deepapp_main/src)
     * Works from any executable location
     */
    static std::string getProjectBasePath();

    /**
     * Get path to resources directory
     */
    static std::string getResourcesPath();

    /**
     * Get path to models directory
     */
    static std::string getModelsPath();

    /**
     * Get path to VietOCR model
     */
    static std::string getVietOcrModelPath();

    /**
     * Get path to YOLO model
     */
    static std::string getYoloModelPath();

    /**
     * Get path to vocab file
     */
    static std::string getVocabPath();

private:
    static std::string findSrcDirectory();
};

} // namespace utils
} // namespace deepapp