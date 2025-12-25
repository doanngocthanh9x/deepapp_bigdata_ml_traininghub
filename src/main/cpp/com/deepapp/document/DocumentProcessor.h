#pragma once

#include <string>
#include <vector>
#include <cstdint>
#include <memory>

namespace deepapp {
namespace document {

/**
 * Document page information
 */
struct PageInfo {
    int pageNumber;
    int width;
    int height;
    int dpi;
    std::string format;
    std::vector<uint8_t> imageData;  // PNG encoded data
    std::string text;  // Extracted text (if available)
};

/**
 * High-performance document processor for PDF and TIFF files
 * Uses Poppler-cpp for PDF and libtiff for TIFF
 */
class DocumentProcessor {
public:
    DocumentProcessor();
    ~DocumentProcessor();

    /**
     * Load document from file path
     * @param filePath Absolute path to PDF or TIFF file
     * @return true if loaded successfully
     */
    bool loadFromFile(const std::string& filePath);

    /**
     * Load document from memory buffer
     * @param data File data
     * @param size Size of data
     * @param fileType "pdf" or "tiff"
     * @return true if loaded successfully
     */
    bool loadFromMemory(const uint8_t* data, size_t size, const std::string& fileType);

    /**
     * Get number of pages in document
     */
    int getPageCount() const;

    /**
     * Get document format (PDF or TIFF)
     */
    std::string getFormat() const;

    /**
     * Extract single page as PNG image with base64 encoding
     * @param pageNumber Page number (1-based)
     * @param dpi Resolution (default 150 for speed, 300 for quality)
     * @return Base64 encoded PNG image
     */
    std::string extractPageAsBase64PNG(int pageNumber, int dpi = 150);

    /**
     * Extract page information
     */
    PageInfo extractPageInfo(int pageNumber, int dpi = 150);

    /**
     * Get last error message
     */
    std::string getLastError() const;

private:
    class Impl;
    std::unique_ptr<Impl> pImpl;
};

} // namespace document
} // namespace deepapp
