#include "com/deepapp/utils/PathUtils.h"
#include <iostream>
#include <cstdlib>

namespace deepapp {
namespace utils {

std::string PathUtils::findSrcDirectory() {
    // Try environment variable first
    const char* env_path = std::getenv("DEEPAPP_PROJECT_SRC");
    if (env_path) {
        return std::string(env_path);
    }

    // Fallback: assume executable is in build/ directory
    // build/ -> deepapp_main/ -> src/
    std::filesystem::path exe_path = std::filesystem::current_path();
    std::filesystem::path src_path = exe_path.parent_path().parent_path() / "src";

    if (std::filesystem::exists(src_path)) {
        return src_path.string();
    }

    // Final fallback: hard-coded path
    return "/root/deepapp/deepapp_main/src";
}

std::string PathUtils::getProjectBasePath() {
    return findSrcDirectory();
}

std::string PathUtils::getResourcesPath() {
    return getProjectBasePath() + "/main/resources";
}

std::string PathUtils::getModelsPath() {
    return getResourcesPath() + "/models";
}

std::string PathUtils::getVietOcrModelPath() {
    return getModelsPath() + "/vietocr_onnx/";
}

std::string PathUtils::getYoloModelPath() {
    return getModelsPath() + "/yolo/";
}

std::string PathUtils::getVocabPath() {
    return getModelsPath() + "/vietocr_onnx/vocab.txt";
}

} // namespace utils
} // namespace deepapp