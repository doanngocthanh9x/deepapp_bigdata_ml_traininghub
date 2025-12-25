#include "DocumentProcessor.h"
#include <algorithm>
#include <poppler/cpp/poppler-document.h>
#include <poppler/cpp/poppler-page.h>
#include <poppler/cpp/poppler-page-renderer.h>
#include <poppler/cpp/poppler-image.h>
#include <tiffio.h>
#include <png.h>
#include <fstream>
#include <sstream>
#include <cstring>
#include <iostream>

namespace deepapp {
namespace document {

// Base64 encoding table
static const char base64_chars[] =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    "abcdefghijklmnopqrstuvwxyz"
    "0123456789+/";

std::string base64_encode(const uint8_t* data, size_t len) {
    std::string ret;
    int i = 0;
    int j = 0;
    uint8_t char_array_3[3];
    uint8_t char_array_4[4];

    while (len--) {
        char_array_3[i++] = *(data++);
        if (i == 3) {
            char_array_4[0] = (char_array_3[0] & 0xfc) >> 2;
            char_array_4[1] = ((char_array_3[0] & 0x03) << 4) + ((char_array_3[1] & 0xf0) >> 4);
            char_array_4[2] = ((char_array_3[1] & 0x0f) << 2) + ((char_array_3[2] & 0xc0) >> 6);
            char_array_4[3] = char_array_3[2] & 0x3f;

            for(i = 0; i < 4; i++)
                ret += base64_chars[char_array_4[i]];
            i = 0;
        }
    }

    if (i) {
        for(j = i; j < 3; j++)
            char_array_3[j] = '\0';

        char_array_4[0] = (char_array_3[0] & 0xfc) >> 2;
        char_array_4[1] = ((char_array_3[0] & 0x03) << 4) + ((char_array_3[1] & 0xf0) >> 4);
        char_array_4[2] = ((char_array_3[1] & 0x0f) << 2) + ((char_array_3[2] & 0xc0) >> 6);

        for (j = 0; j < i + 1; j++)
            ret += base64_chars[char_array_4[j]];

        while((i++ < 3))
            ret += '=';
    }

    return ret;
}

// PNG write callback
struct PNGMemoryWriter {
    std::vector<uint8_t> data;
};

void png_write_callback(png_structp png_ptr, png_bytep data, png_size_t length) {
    PNGMemoryWriter* writer = (PNGMemoryWriter*)png_get_io_ptr(png_ptr);
    writer->data.insert(writer->data.end(), data, data + length);
}

class DocumentProcessor::Impl {
public:
    std::unique_ptr<poppler::document> pdfDoc;
    TIFF* tiffHandle = nullptr;
    std::string format;
    std::string lastError;
    std::vector<uint8_t> fileData;

    ~Impl() {
        if (tiffHandle) {
            TIFFClose(tiffHandle);
            tiffHandle = nullptr;
        }
    }

    bool loadPDFFromFile(const std::string& filePath) {
        pdfDoc.reset(poppler::document::load_from_file(filePath));
        if (!pdfDoc) {
            lastError = "Failed to load PDF: " + filePath;
            return false;
        }
        format = "PDF";
        return true;
    }

    bool loadPDFFromMemory(const uint8_t* data, size_t size) {
        fileData.assign(data, data + size);
        pdfDoc.reset(poppler::document::load_from_raw_data(
            reinterpret_cast<const char*>(fileData.data()), 
            fileData.size()
        ));
        if (!pdfDoc) {
            lastError = "Failed to load PDF from memory";
            return false;
        }
        format = "PDF";
        return true;
    }

    bool loadTIFFFromFile(const std::string& filePath) {
        tiffHandle = TIFFOpen(filePath.c_str(), "r");
        if (!tiffHandle) {
            lastError = "Failed to open TIFF: " + filePath;
            return false;
        }
        format = "TIFF";
        return true;
    }

    std::string renderPDFPageToPNG(int pageNumber, int dpi) {
        if (!pdfDoc || pageNumber < 1 || pageNumber > pdfDoc->pages()) {
            lastError = "Invalid page number";
            return "";
        }

        std::unique_ptr<poppler::page> page(pdfDoc->create_page(pageNumber - 1));
        if (!page) {
            lastError = "Failed to create page";
            return "";
        }

        poppler::page_renderer renderer;
        renderer.set_render_hint(poppler::page_renderer::antialiasing, true);
        renderer.set_render_hint(poppler::page_renderer::text_antialiasing, true);

        // Render at specified DPI
        poppler::image img = renderer.render_page(page.get(), dpi, dpi);
        if (!img.is_valid()) {
            lastError = "Failed to render page";
            return "";
        }

        // Convert to PNG
        std::vector<uint8_t> pngData = convertImageToPNG(
            reinterpret_cast<const uint8_t*>(img.const_data()), 
            img.width(), 
            img.height(), 
            img.bytes_per_row(),
            img.format() == poppler::image::format_rgb24 ? 3 : 4
        );

        if (pngData.empty()) {
            return "";
        }

        return base64_encode(pngData.data(), pngData.size());
    }

    std::string renderTIFFPageToPNG(int pageNumber, int dpi) {
        if (!tiffHandle) {
            lastError = "TIFF not loaded";
            return "";
        }

        // Navigate to page (TIFF pages are 0-indexed in directory)
        if (TIFFSetDirectory(tiffHandle, pageNumber - 1) == 0) {
            lastError = "Failed to set TIFF directory";
            return "";
        }

        uint32_t width, height;
        TIFFGetField(tiffHandle, TIFFTAG_IMAGEWIDTH, &width);
        TIFFGetField(tiffHandle, TIFFTAG_IMAGELENGTH, &height);

        // Read RGBA data
        std::vector<uint32_t> raster(width * height);
        if (!TIFFReadRGBAImageOriented(tiffHandle, width, height, raster.data(), ORIENTATION_TOPLEFT, 0)) {
            lastError = "Failed to read TIFF image";
            return "";
        }

        // Convert RGBA to PNG
        std::vector<uint8_t> pngData = convertImageToPNG(
            reinterpret_cast<uint8_t*>(raster.data()),
            width,
            height,
            width * 4,
            4
        );

        if (pngData.empty()) {
            return "";
        }

        return base64_encode(pngData.data(), pngData.size());
    }

    std::vector<uint8_t> convertImageToPNG(const uint8_t* data, int width, int height, 
                                           int stride, int channels) {
        PNGMemoryWriter writer;

        png_structp png = png_create_write_struct(PNG_LIBPNG_VER_STRING, nullptr, nullptr, nullptr);
        if (!png) {
            lastError = "Failed to create PNG write struct";
            return {};
        }

        png_infop info = png_create_info_struct(png);
        if (!info) {
            png_destroy_write_struct(&png, nullptr);
            lastError = "Failed to create PNG info struct";
            return {};
        }

        if (setjmp(png_jmpbuf(png))) {
            png_destroy_write_struct(&png, &info);
            lastError = "PNG encoding error";
            return {};
        }

        png_set_write_fn(png, &writer, png_write_callback, nullptr);

        int colorType = (channels == 4) ? PNG_COLOR_TYPE_RGB_ALPHA : PNG_COLOR_TYPE_RGB;
        png_set_IHDR(png, info, width, height, 8, colorType,
                     PNG_INTERLACE_NONE, PNG_COMPRESSION_TYPE_DEFAULT, PNG_FILTER_TYPE_DEFAULT);

        png_write_info(png, info);

        // Write image data
        std::vector<png_bytep> rows(height);
        for (int y = 0; y < height; y++) {
            rows[y] = const_cast<png_bytep>(data + y * stride);
        }
        png_write_image(png, rows.data());
        png_write_end(png, nullptr);

        png_destroy_write_struct(&png, &info);

        return writer.data;
    }
};

DocumentProcessor::DocumentProcessor() : pImpl(std::make_unique<Impl>()) {}

DocumentProcessor::~DocumentProcessor() = default;

bool DocumentProcessor::loadFromFile(const std::string& filePath) {
    std::string ext = filePath.substr(filePath.find_last_of('.') + 1);
    std::transform(ext.begin(), ext.end(), ext.begin(), ::tolower);

    if (ext == "pdf") {
        return pImpl->loadPDFFromFile(filePath);
    } else if (ext == "tif" || ext == "tiff") {
        return pImpl->loadTIFFFromFile(filePath);
    } else {
        pImpl->lastError = "Unsupported file format: " + ext;
        return false;
    }
}

bool DocumentProcessor::loadFromMemory(const uint8_t* data, size_t size, const std::string& fileType) {
    if (fileType == "pdf") {
        return pImpl->loadPDFFromMemory(data, size);
    } else {
        pImpl->lastError = "Memory loading not implemented for: " + fileType;
        return false;
    }
}

int DocumentProcessor::getPageCount() const {
    if (pImpl->pdfDoc) {
        return pImpl->pdfDoc->pages();
    } else if (pImpl->tiffHandle) {
        int count = 0;
        do {
            count++;
        } while (TIFFReadDirectory(pImpl->tiffHandle));
        TIFFSetDirectory(pImpl->tiffHandle, 0);  // Reset to first page
        return count;
    }
    return 0;
}

std::string DocumentProcessor::getFormat() const {
    return pImpl->format;
}

std::string DocumentProcessor::extractPageAsBase64PNG(int pageNumber, int dpi) {
    if (pImpl->format == "PDF") {
        return pImpl->renderPDFPageToPNG(pageNumber, dpi);
    } else if (pImpl->format == "TIFF") {
        return pImpl->renderTIFFPageToPNG(pageNumber, dpi);
    }
    pImpl->lastError = "No document loaded";
    return "";
}

PageInfo DocumentProcessor::extractPageInfo(int pageNumber, int dpi) {
    PageInfo info;
    info.pageNumber = pageNumber;
    info.format = pImpl->format;
    info.dpi = dpi;

    if (pImpl->format == "PDF" && pImpl->pdfDoc) {
        std::unique_ptr<poppler::page> page(pImpl->pdfDoc->create_page(pageNumber - 1));
        if (page) {
            poppler::rectf rect = page->page_rect();
            info.width = static_cast<int>(rect.width() * dpi / 72.0);
            info.height = static_cast<int>(rect.height() * dpi / 72.0);
            
            // Extract text
            info.text = page->text(rect).to_latin1();
            if (info.text.length() > 100) {
                info.text = info.text.substr(0, 100) + "...";
            }
        }
    } else if (pImpl->format == "TIFF" && pImpl->tiffHandle) {
        if (TIFFSetDirectory(pImpl->tiffHandle, pageNumber - 1) != 0) {
            uint32_t width, height;
            TIFFGetField(pImpl->tiffHandle, TIFFTAG_IMAGEWIDTH, &width);
            TIFFGetField(pImpl->tiffHandle, TIFFTAG_IMAGELENGTH, &height);
            info.width = width;
            info.height = height;
        }
    }

    return info;
}

std::string DocumentProcessor::getLastError() const {
    return pImpl->lastError;
}

} // namespace document
} // namespace deepapp
