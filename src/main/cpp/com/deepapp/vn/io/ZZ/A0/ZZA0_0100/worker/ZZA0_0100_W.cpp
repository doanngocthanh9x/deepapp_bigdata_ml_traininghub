#include "com/deepapp/infrastructure/BaseWorker.h"
#include "com/deepapp/infrastructure/WorkerRegistry.h"
#include "com/deepapp/infrastructure/GrpcWorkerClient.h"
#include "com/deepapp/document/DocumentProcessor.h"
#include "com/deepapp/storage/DocumentStorage.h"
#include <nlohmann/json.hpp>
#include <iostream>
#include <fstream>
#include <string>
#include <vector>
#include <ctime>
#include <sstream>
#include <iomanip>
#include <cstring>
#include <algorithm>
#include <map>
#include <memory>

using json = nlohmann::json;
using deepapp::document::DocumentProcessor;
using deepapp::storage::DocumentStorage;
using deepapp::storage::DocumentRecord;
using deepapp::storage::PageRecord;
using deepapp::storage::TaskRecord;

namespace deepapp {
namespace workers {

// Struct to hold chunked file data
struct ChunkedFile {
    std::string filename;
    std::string requestId;
    size_t totalSize;
    int totalChunks;
    std::map<int, std::vector<uint8_t>> chunks;
    
    bool isComplete() const {
        return chunks.size() == static_cast<size_t>(totalChunks);
    }
    
    std::vector<uint8_t> assembleFile() const {
        std::vector<uint8_t> result;
        result.reserve(totalSize);
        
        for (int i = 0; i < totalChunks; i++) {
            auto it = chunks.find(i);
            if (it != chunks.end()) {
                result.insert(result.end(), it->second.begin(), it->second.end());
            }
        }
        
        return result;
    }
};

/**
 * ZZA0_0100_Worker - Document Processing Worker
 * Handles TIFF, TIF, and PDF document processing
 * Extracts pages and returns them to Java
 */
class ZZA0_0100_Worker : public infrastructure::BaseWorker {
public:
    ZZA0_0100_Worker() : infrastructure::BaseWorker("ZZA0_0100_Worker") {
        std::cout << "[ZZA0_0100_Worker] Document Processing Worker Initialized" << std::endl;
        std::cout << "[ZZA0_0100_Worker] Supported formats: TIFF, TIF, PDF" << std::endl;
        
        // Initialize DocumentStorage
        storage_ = std::make_unique<DocumentStorage>("/tmp/deepapp/documents.db");
        if (!storage_->initialize()) {
            std::cerr << "[ZZA0_0100_Worker] WARNING: Failed to initialize DocumentStorage: " 
                      << storage_->getLastError() << std::endl;
        } else {
            std::cout << "[ZZA0_0100_Worker] DocumentStorage initialized successfully" << std::endl;
            
            // Print statistics
            auto stats = storage_->getStatistics();
            std::cout << "[ZZA0_0100_Worker] Database stats:" << std::endl;
            for (const auto& pair : stats) {
                std::cout << "  - " << pair.first << ": " << pair.second << std::endl;
            }
        }
    }
    
    ~ZZA0_0100_Worker() {
        if (storage_) {
            storage_->close();
        }
    }

    std::string processTask(const std::string& event_type, const std::string& payload) override {
        std::cout << "[ZZA0_0100_Worker] Processing document task:" << std::endl;
        std::cout << "  Event Type: " << event_type << std::endl;
        std::cout << "  Payload size: " << payload.size() << " bytes" << std::endl;

        try {
            json response;
            response["worker"] = "ZZA0_0100_W";
            response["timestamp"] = std::time(nullptr);

            // Handle different event types
            if (event_type == "process_document") {
                return processDocument(payload);
            } else if (event_type == "prepare_chunked_file") {
                return prepareChunkedFile(payload);
            } else if (event_type == "upload_chunk") {
                return uploadChunk(payload);
            } else if (event_type == "process_chunked_document") {
                return processChunkedDocument(payload);
            } else if (event_type == "get_page") {
                return getPage(payload);
            } else if (event_type == "document_info") {
                return getDocumentInfo(payload);
            } else if (event_type == "echo") {
                response["status"] = "success";
                response["data"] = "Echo: " + payload;
                return response.dump();
            } else {
                response["status"] = "error";
                response["error"] = "Unknown event type: " + event_type;
                return response.dump();
            }

        } catch (const std::exception& e) {
            json error_response;
            error_response["worker"] = "ZZA0_0100_W";
            error_response["status"] = "error";
            error_response["error"] = std::string("Exception: ") + e.what();
            error_response["timestamp"] = std::time(nullptr);
            return error_response.dump();
        }
    }

private:
    /**
     * Sanitize UTF-8 string by removing invalid bytes
     */
    std::string sanitizeUtf8(const std::string& input) {
        std::string output;
        output.reserve(input.size());
        
        for (size_t i = 0; i < input.size(); ) {
            unsigned char c = input[i];
            
            // ASCII (0x00-0x7F)
            if (c <= 0x7F) {
                output.push_back(c);
                i++;
            }
            // 2-byte UTF-8 (0xC0-0xDF)
            else if (c >= 0xC0 && c <= 0xDF && i + 1 < input.size()) {
                unsigned char c2 = input[i + 1];
                if ((c2 & 0xC0) == 0x80) {
                    output.push_back(c);
                    output.push_back(c2);
                    i += 2;
                } else {
                    i++; // Skip invalid byte
                }
            }
            // 3-byte UTF-8 (0xE0-0xEF)
            else if (c >= 0xE0 && c <= 0xEF && i + 2 < input.size()) {
                unsigned char c2 = input[i + 1];
                unsigned char c3 = input[i + 2];
                if ((c2 & 0xC0) == 0x80 && (c3 & 0xC0) == 0x80) {
                    output.push_back(c);
                    output.push_back(c2);
                    output.push_back(c3);
                    i += 3;
                } else {
                    i++; // Skip invalid byte
                }
            }
            // 4-byte UTF-8 (0xF0-0xF7)
            else if (c >= 0xF0 && c <= 0xF7 && i + 3 < input.size()) {
                unsigned char c2 = input[i + 1];
                unsigned char c3 = input[i + 2];
                unsigned char c4 = input[i + 3];
                if ((c2 & 0xC0) == 0x80 && (c3 & 0xC0) == 0x80 && (c4 & 0xC0) == 0x80) {
                    output.push_back(c);
                    output.push_back(c2);
                    output.push_back(c3);
                    output.push_back(c4);
                    i += 4;
                } else {
                    i++; // Skip invalid byte
                }
            }
            else {
                // Invalid UTF-8 byte, skip it
                i++;
            }
        }
        
        return output;
    }

    /**
     * Process document and stream pages back to Java
     * NEW: Read file from disk path (no size limit!)
     */
    std::string processDocument(const std::string& payload) {
        try {
            json request = json::parse(payload);
            std::string filename = request.value("filename", "unknown");
            std::string filePath = request.value("filePath", "");
            std::string requestId = request.value("requestId", "");
            std::string clientId = request.value("clientId", "java-cpp-client");
            std::string outputDir = request.value("outputDir", "/tmp/deepapp/outputs");
            int dpi = request.value("dpi", 150);  // Default 150 DPI for speed

            std::cout << "[ZZA0_0100_Worker] Processing document: " << filename << std::endl;
            std::cout << "[ZZA0_0100_Worker] File path: " << filePath << std::endl;
            std::cout << "[ZZA0_0100_Worker] Request ID: " << requestId << std::endl;
            std::cout << "[ZZA0_0100_Worker] DPI: " << dpi << std::endl;

            // Use high-performance DocumentProcessor
            DocumentProcessor processor;
            
            if (!filePath.empty()) {
                // Load from disk (preferred - no memory overhead)
                if (!processor.loadFromFile(filePath)) {
                    throw std::runtime_error("Failed to load document: " + processor.getLastError());
                }
                std::cout << "[ZZA0_0100_Worker] Loaded from disk: " << filePath << std::endl;
            } else {
                // Fallback: load from base64 data
                std::string base64_data = request.value("data", "");
                if (base64_data.empty()) {
                    throw std::runtime_error("No filePath or data provided");
                }
                std::vector<uint8_t> file_data = decodeBase64(base64_data);
                std::string file_type = getFileExtension(filename);
                if (!processor.loadFromMemory(file_data.data(), file_data.size(), file_type)) {
                    throw std::runtime_error("Failed to load from memory: " + processor.getLastError());
                }
                std::cout << "[ZZA0_0100_Worker] Loaded from memory (" << file_data.size() << " bytes)" << std::endl;
            }

            // Get document info
            int pageCount = processor.getPageCount();
            std::string format = processor.getFormat();
            
            std::cout << "[ZZA0_0100_Worker] Document: " << format << ", " << pageCount << " page(s)" << std::endl;

            // ============================================================
            // SAVE DOCUMENT TO DATABASE
            // ============================================================
            int documentId = -1;
            if (storage_ && storage_->isOpen()) {
                DocumentRecord doc;
                doc.requestId = requestId;
                doc.filename = filename;
                doc.filePath = filePath;
                doc.format = format;
                doc.pageCount = pageCount;
                doc.fileSize = 0;  // Could get from stat(filePath)
                doc.status = "processing";
                doc.errorMessage = "";
                
                documentId = storage_->saveDocument(doc);
                if (documentId > 0) {
                    std::cout << "[ZZA0_0100_Worker] ✓ Saved document to DB (ID: " << documentId << ")" << std::endl;
                    
                    // Create task record
                    TaskRecord task;
                    task.requestId = requestId;
                    task.taskType = "document_processing";
                    task.status = "running";
                    task.totalPages = pageCount;
                    task.processedPages = 0;
                    storage_->createTask(task);
                } else {
                    std::cerr << "[ZZA0_0100_Worker] Failed to save document: " << storage_->getLastError() << std::endl;
                }
            }

            // Send initial metadata event to Java
            json metadataEvent;
            metadataEvent["eventType"] = "document_metadata";
            metadataEvent["requestId"] = requestId;
            metadataEvent["filename"] = filename;
            metadataEvent["fileType"] = format;
            metadataEvent["format"] = format;
            metadataEvent["pageCount"] = pageCount;
            metadataEvent["timestamp"] = std::time(nullptr);
            
            sendEventToJava(clientId, "document_event", metadataEvent.dump(), requestId);
            std::cout << "[ZZA0_0100_Worker] ✓ Sent metadata to Java" << std::endl;

            // Process and stream each page individually with REAL rendering
            for (int pageNum = 1; pageNum <= pageCount; pageNum++) {
                std::cout << "[ZZA0_0100_Worker] Rendering page " << pageNum << "/" << pageCount << " at " << dpi << " DPI..." << std::endl;
                
                // Extract page info (fast)
                auto pageInfo = processor.extractPageInfo(pageNum, dpi);
                
                // Render page to PNG and encode to base64 (this is the heavy operation)
                std::string imageBase64 = processor.extractPageAsBase64PNG(pageNum, dpi);
                
                if (imageBase64.empty()) {
                    std::cerr << "[ZZA0_0100_Worker] Failed to render page " << pageNum << ": " 
                              << processor.getLastError() << std::endl;
                    continue;
                }
                
                std::cout << "[ZZA0_0100_Worker] Rendered page " << pageNum << " (" << imageBase64.length() << " bytes base64)" << std::endl;
                
                // ============================================================
                // SAVE PAGE TO DATABASE
                // ============================================================
                if (storage_ && storage_->isOpen() && documentId > 0) {
                    PageRecord page;
                    page.documentId = documentId;
                    page.requestId = requestId;
                    page.pageNumber = pageNum;
                    page.width = pageInfo.width;
                    page.height = pageInfo.height;
                    page.dpi = dpi;
                    page.format = format;
                    page.imagePath = "";  // Could save PNG to disk and store path
                    page.text = sanitizeUtf8(pageInfo.text);
                    page.status = "rendered";
                    
                    int pageId = storage_->savePage(page);
                    if (pageId > 0) {
                        std::cout << "[ZZA0_0100_Worker] ✓ Saved page " << pageNum << " to DB (ID: " << pageId << ")" << std::endl;
                        
                        // Update task progress
                        storage_->updateTaskProgress(requestId, pageNum);
                    }
                }
                
                // Build page data
                json pageData;
                pageData["pageNumber"] = pageNum;
                pageData["format"] = format;
                pageData["width"] = pageInfo.width;
                pageData["height"] = pageInfo.height;
                pageData["dpi"] = dpi;
                pageData["imageData"] = imageBase64;  // Real PNG image!
                if (!pageInfo.text.empty()) {
                    // Sanitize text to remove invalid UTF-8 characters
                    pageData["text"] = sanitizeUtf8(pageInfo.text);
                }
                
                // Send page event to Java immediately
                json pageEvent;
                pageEvent["eventType"] = "document_page";
                pageEvent["requestId"] = requestId;
                pageEvent["pageNumber"] = pageNum;
                pageEvent["totalPages"] = pageCount;
                pageEvent["pageData"] = pageData;
                pageEvent["timestamp"] = std::time(nullptr);
                
                sendEventToJava(clientId, "document_event", pageEvent.dump(), requestId);
                std::cout << "[ZZA0_0100_Worker] ✓ Sent page " << pageNum << " to Java" << std::endl;
            }

            // ============================================================
            // MARK AS COMPLETED IN DATABASE
            // ============================================================
            if (storage_ && storage_->isOpen()) {
                storage_->updateDocumentStatus(requestId, "completed");
                storage_->updateTaskStatus(requestId, "completed");
                std::cout << "[ZZA0_0100_Worker] ✓ Marked document as completed in DB" << std::endl;
            }

            // Send completion event
            json completeEvent;
            completeEvent["eventType"] = "document_complete";
            completeEvent["requestId"] = requestId;
            completeEvent["filename"] = filename;
            completeEvent["totalPages"] = pageCount;
            completeEvent["timestamp"] = std::time(nullptr);
            
            sendEventToJava(clientId, "document_event", completeEvent.dump(), requestId);
            std::cout << "[ZZA0_0100_Worker] ✓ Sent completion event to Java" << std::endl;

            // Return immediate acknowledgment
            json response;
            response["worker"] = "ZZA0_0100_W";
            response["status"] = "streaming";
            response["message"] = "Pages are being streamed to Java via gRPC events";
            response["requestId"] = requestId;
            response["pageCount"] = pageCount;
            response["timestamp"] = std::time(nullptr);

            return response.dump();

        } catch (const std::exception& e) {
            json error_response;
            error_response["worker"] = "ZZA0_0100_W";
            error_response["status"] = "error";
            error_response["error"] = e.what();
            return error_response.dump();
        }
    }
    
    /**
     * Helper to send events back to Java client
     */
    void sendEventToJava(const std::string& targetClientId, 
                        const std::string& eventType,
                        const std::string& payload,
                        const std::string& requestId) {
        std::cout << "[ZZA0_0100_Worker] Sending " << eventType 
                  << " to " << targetClientId << std::endl;
        
        if (grpc_client_) {
            std::map<std::string, std::string> metadata;
            metadata["requestId"] = requestId;
            grpc_client_->sendEvent(targetClientId, eventType, payload, metadata);
        } else {
            std::cerr << "[ZZA0_0100_Worker] ERROR: grpc_client_ is null!" << std::endl;
        }
    }
    
    /**
     * Extract a single page from document
     */
    json extractSinglePage(const std::vector<uint8_t>& file_data,
                          const std::string& filename,
                          const std::string& file_type,
                          int pageNumber) {
        json page;
        
        if (file_type == "pdf") {
            page["pageNumber"] = pageNumber;
            page["width"] = 595;  // A4 width
            page["height"] = 842; // A4 height
            page["format"] = "PDF";
            page["imageData"] = "base64_page_" + std::to_string(pageNumber) + "_data";
            page["text"] = "Text from page " + std::to_string(pageNumber);
        } else if (file_type == "tiff" || file_type == "tif") {
            page["pageNumber"] = pageNumber;
            page["width"] = 2480;
            page["height"] = 3508;
            page["format"] = "TIFF";
            page["dpi"] = 300;
            page["imageData"] = "base64_tiff_page_" + std::to_string(pageNumber) + "_data";
        }
        
        return page;
    }

    /**
     * Get a specific page from document
     */
    std::string getPage(const std::string& payload) {
        try {
            json request = json::parse(payload);
            std::string filename = request.value("filename", "unknown");
            int page_number = request.value("pageNumber", 1);

            std::cout << "[ZZA0_0100_Worker] Getting page " << page_number 
                      << " from: " << filename << std::endl;

            json response;
            response["worker"] = "ZZA0_0100_W";
            response["status"] = "success";
            response["filename"] = filename;
            response["pageNumber"] = page_number;
            response["pageData"] = "Page " + std::to_string(page_number) + " image data (base64)";
            response["timestamp"] = std::time(nullptr);

            return response.dump();

        } catch (const std::exception& e) {
            json error_response;
            error_response["worker"] = "ZZA0_0100_W";
            error_response["status"] = "error";
            error_response["error"] = e.what();
            return error_response.dump();
        }
    }

    /**
     * Get document information (page count, dimensions, etc.)
     */
    std::string getDocumentInfo(const std::string& payload) {
        try {
            json request = json::parse(payload);
            std::string filename = request.value("filename", "unknown");
            std::string file_type = getFileExtension(filename);
            
            int pageCount = -1;
            
            // Try to get real page count if data is provided
            if (request.contains("data") && !request["data"].get<std::string>().empty()) {
                std::string base64_data = request["data"];
                std::vector<uint8_t> file_data = decodeBase64(base64_data);
                pageCount = getRealPageCount(file_data, file_type);
            } else {
                // No data provided - return mock value with warning
                pageCount = -1;  // -1 indicates unknown
            }

            json response;
            response["worker"] = "ZZA0_0100_W";
            response["status"] = "success";
            response["filename"] = filename;
            response["pageCount"] = pageCount;
            response["format"] = file_type;
            response["timestamp"] = std::time(nullptr);
            
            if (pageCount == -1) {
                response["note"] = "Page count unavailable - no file data provided";
            }

            return response.dump();

        } catch (const std::exception& e) {
            json error_response;
            error_response["worker"] = "ZZA0_0100_W";
            error_response["status"] = "error";
            error_response["error"] = e.what();
            return error_response.dump();
        }
    }
    
    /**
     * Get real page count from PDF/TIFF data
     * This is a simplified parser - for production use proper libraries
     */
    int getRealPageCount(const std::vector<uint8_t>& file_data, const std::string& file_type) {
        if (file_type == "pdf") {
            return getPdfPageCount(file_data);
        } else if (file_type == "tiff" || file_type == "tif") {
            return getTiffPageCount(file_data);
        }
        return -1;
    }
    
    /**
     * Parse PDF to get page count
     * Simple method: count "/Type /Page" occurrences
     */
    int getPdfPageCount(const std::vector<uint8_t>& data) {
        try {
            std::string content(data.begin(), data.end());
            
            // Method 1: Look for /Count in /Pages object (most reliable)
            size_t pages_pos = content.find("/Type /Pages");
            if (pages_pos != std::string::npos) {
                size_t count_pos = content.find("/Count", pages_pos);
                if (count_pos != std::string::npos && count_pos - pages_pos < 200) {
                    // Extract number after /Count
                    size_t num_start = count_pos + 6;
                    while (num_start < content.length() && std::isspace(content[num_start])) num_start++;
                    
                    std::string num_str;
                    while (num_start < content.length() && std::isdigit(content[num_start])) {
                        num_str += content[num_start++];
                    }
                    
                    if (!num_str.empty()) {
                        return std::stoi(num_str);
                    }
                }
            }
            
            // Method 2: Count individual page objects
            int page_count = 0;
            size_t pos = 0;
            while ((pos = content.find("/Type /Page", pos)) != std::string::npos) {
                // Make sure it's "/Type /Page" not "/Type /Pages"
                if (pos + 11 < content.length() && content[pos + 11] != 's') {
                    page_count++;
                }
                pos += 11;
            }
            
            return (page_count > 0) ? page_count : -1;
            
        } catch (...) {
            return -1;
        }
    }
    
    /**
     * Get TIFF page count
     */
    int getTiffPageCount(const std::vector<uint8_t>& data) {
        // Simplified TIFF parser - count IFD (Image File Directory) entries
        if (data.size() < 8) return -1;
        
        // Check TIFF header
        bool little_endian = (data[0] == 'I' && data[1] == 'I');
        bool big_endian = (data[0] == 'M' && data[1] == 'M');
        
        if (!little_endian && !big_endian) return -1;
        
        // For now, return mock count
        // Full TIFF parsing requires libtiff
        return 1;  // Assume single page for now
    }

    /**
     * Extract pages from PDF (mock implementation)
     * In production, use libraries like Poppler or MuPDF
     */
    std::vector<json> extractPdfPages(const std::vector<uint8_t>& file_data, 
                                      const std::string& filename) {
        std::vector<json> pages;

        // Mock: Create 3 sample pages
        for (int i = 1; i <= 3; i++) {
            json page;
            page["pageNumber"] = i;
            page["width"] = 595;  // A4 width in points
            page["height"] = 842; // A4 height in points
            page["format"] = "PDF";
            page["imageData"] = "base64_encoded_image_data_page_" + std::to_string(i);
            page["text"] = "Sample text from page " + std::to_string(i);
            pages.push_back(page);
        }

        return pages;
    }

    /**
     * Extract pages from TIFF (mock implementation)
     * In production, use libtiff
     */
    std::vector<json> extractTiffPages(const std::vector<uint8_t>& file_data,
                                       const std::string& filename) {
        std::vector<json> pages;

        // Mock: Create 2 sample pages
        for (int i = 1; i <= 2; i++) {
            json page;
            page["pageNumber"] = i;
            page["width"] = 2480;  // Sample width
            page["height"] = 3508; // Sample height (A4 at 300 DPI)
            page["format"] = "TIFF";
            page["dpi"] = 300;
            page["compression"] = "LZW";
            page["imageData"] = "base64_encoded_tiff_page_" + std::to_string(i);
            pages.push_back(page);
        }

        return pages;
    }

    /**
     * Download file from gRPC Hub using FileService
     */
    std::vector<uint8_t> downloadFileFromHub(const std::string& fileId) {
        try {
            // Get gRPC client to access channel
            if (!grpc_client_) {
                throw std::runtime_error("gRPC client not available");
            }
            
            // Get channel from gRPC client (need to add getChannel() method)
            // For now, create direct connection - TODO: refactor to reuse existing channel
            auto channel = grpc::CreateChannel("72.60.111.138:50051", 
                                              grpc::InsecureChannelCredentials());
            auto fileStub = hub::FileService::NewStub(channel);
            
            // Create download request
            hub::DownloadRequest request;
            request.set_file_id(fileId);
            
            // Start streaming download
            grpc::ClientContext context;
            context.set_deadline(std::chrono::system_clock::now() + std::chrono::seconds(300));  // 5 min timeout
            
            auto reader = fileStub->Download(&context, request);
            
            // Accumulate file data
            std::vector<uint8_t> fileData;
            hub::FileChunk chunk;
            int chunkCount = 0;
            
            while (reader->Read(&chunk)) {
                const std::string& data = chunk.data();
                fileData.insert(fileData.end(), data.begin(), data.end());
                chunkCount++;
                
                if (chunkCount % 100 == 0) {
                    std::cout << "[ZZA0_0100_Worker] Downloaded " << fileData.size() 
                              << " bytes (" << chunkCount << " chunks)" << std::endl;
                }
            }
            
            grpc::Status status = reader->Finish();
            if (!status.ok()) {
                throw std::runtime_error("Download failed: " + status.error_message());
            }
            
            std::cout << "[ZZA0_0100_Worker] Download complete: " << fileData.size() 
                      << " bytes in " << chunkCount << " chunks" << std::endl;
            
            return fileData;
            
        } catch (const std::exception& e) {
            std::cerr << "[ZZA0_0100_Worker] Download error: " << e.what() << std::endl;
            throw;
        }
    }

    /**
     * Decode base64 string (simplified implementation)
     */
    std::vector<uint8_t> decodeBase64(const std::string& base64_string) {
        // This is a simplified mock - use proper base64 library in production
        std::vector<uint8_t> data;
        data.resize(base64_string.size());
        // Just copy for demo purposes
        std::memcpy(data.data(), base64_string.data(), base64_string.size());
        return data;
    }

    /**
     * Get file extension from filename
     */
    std::string getFileExtension(const std::string& filename) {
        size_t dot_pos = filename.find_last_of('.');
        if (dot_pos != std::string::npos) {
            std::string ext = filename.substr(dot_pos + 1);
            // Convert to lowercase
            std::transform(ext.begin(), ext.end(), ext.begin(), ::tolower);
            return ext;
        }
        return "";
    }
    
    /**
     * Read file from disk
     * Returns file content as bytes
     */
    std::vector<uint8_t> readFileFromDisk(const std::string& filePath) {
        std::ifstream file(filePath, std::ios::binary | std::ios::ate);
        if (!file.is_open()) {
            throw std::runtime_error("Failed to open file: " + filePath);
        }
        
        // Get file size
        std::streamsize size = file.tellg();
        file.seekg(0, std::ios::beg);
        
        // Read file content
        std::vector<uint8_t> buffer(size);
        if (!file.read(reinterpret_cast<char*>(buffer.data()), size)) {
            throw std::runtime_error("Failed to read file: " + filePath);
        }
        
        return buffer;
    }

private:
    // Storage for chunked files being uploaded
    std::map<std::string, ChunkedFile> chunkedFiles_;
    
    // SQLite database for document/page storage
    std::unique_ptr<DocumentStorage> storage_;

    /**
     * Prepare to receive chunked file
     */
    std::string prepareChunkedFile(const std::string& payload) {
        try {
            json request = json::parse(payload);
            std::string fileId = request.value("fileId", "");
            
            ChunkedFile cf;
            cf.filename = request.value("filename", "unknown");
            cf.requestId = request.value("requestId", "");
            cf.totalSize = request.value("totalSize", 0);
            cf.totalChunks = request.value("totalChunks", 0);
            
            chunkedFiles_[fileId] = cf;
            
            std::cout << "[ZZA0_0100_Worker] Prepared chunked upload: " << cf.filename 
                      << " (" << cf.totalSize << " bytes, " << cf.totalChunks << " chunks)" << std::endl;
            
            json response;
            response["worker"] = "ZZA0_0100_W";
            response["status"] = "ready";
            response["fileId"] = fileId;
            return response.dump();
            
        } catch (const std::exception& e) {
            json error;
            error["status"] = "error";
            error["error"] = e.what();
            return error.dump();
        }
    }

    /**
     * Receive and store one chunk
     */
    std::string uploadChunk(const std::string& payload) {
        try {
            json request = json::parse(payload);
            std::string fileId = request.value("fileId", "");
            int chunkNumber = request.value("chunkNumber", -1);
            std::string base64Data = request.value("data", "");
            
            auto it = chunkedFiles_.find(fileId);
            if (it == chunkedFiles_.end()) {
                throw std::runtime_error("File ID not found: " + fileId);
            }
            
            // Decode chunk
            std::vector<uint8_t> chunkData = decodeBase64(base64Data);
            it->second.chunks[chunkNumber] = chunkData;
            
            std::cout << "[ZZA0_0100_Worker] Received chunk " << (chunkNumber + 1) 
                      << "/" << it->second.totalChunks << " (" << chunkData.size() << " bytes)" << std::endl;
            
            json response;
            response["worker"] = "ZZA0_0100_W";
            response["status"] = "chunk_received";
            response["chunkNumber"] = chunkNumber;
            response["isComplete"] = it->second.isComplete();
            return response.dump();
            
        } catch (const std::exception& e) {
            json error;
            error["status"] = "error";
            error["error"] = e.what();
            return error.dump();
        }
    }

    /**
     * Process chunked document after all chunks received
     */
    std::string processChunkedDocument(const std::string& payload) {
        try {
            json request = json::parse(payload);
            std::string fileId = request.value("fileId", "");
            std::string filename = request.value("filename", "unknown");
            std::string requestId = request.value("requestId", "");
            std::string clientId = request.value("clientId", "java-cpp-client");
            
            auto it = chunkedFiles_.find(fileId);
            if (it == chunkedFiles_.end()) {
                throw std::runtime_error("File ID not found: " + fileId);
            }
            
            if (!it->second.isComplete()) {
                throw std::runtime_error("Not all chunks received: " + 
                    std::to_string(it->second.chunks.size()) + "/" + 
                    std::to_string(it->second.totalChunks));
            }
            
            // Assemble file
            std::cout << "[ZZA0_0100_Worker] Assembling " << it->second.totalChunks 
                      << " chunks..." << std::endl;
            std::vector<uint8_t> file_data = it->second.assembleFile();
            std::cout << "[ZZA0_0100_Worker] Assembled " << file_data.size() << " bytes" << std::endl;
            
            // Clean up chunks
            chunkedFiles_.erase(it);
            
            // Now process the assembled file (same as regular file processing)
            std::string file_type = getFileExtension(filename);
            int pageCount = getRealPageCount(file_data, file_type);
            
            std::cout << "[ZZA0_0100_Worker] Processing " << pageCount << " pages..." << std::endl;
            
            // Send metadata
            json metadataEvent;
            metadataEvent["eventType"] = "document_metadata";
            metadataEvent["requestId"] = requestId;
            metadataEvent["filename"] = filename;
            metadataEvent["pageCount"] = pageCount;
            metadataEvent["fileType"] = file_type;
            metadataEvent["timestamp"] = std::time(nullptr);
            
            sendEventToJava(clientId, "document_event", metadataEvent.dump(), requestId);
            
            // Process each page
            for (int pageNum = 1; pageNum <= pageCount; pageNum++) {
                json pageData = extractSinglePage(file_data, filename, file_type, pageNum);
                
                json pageEvent;
                pageEvent["eventType"] = "document_page";
                pageEvent["requestId"] = requestId;
                pageEvent["pageNumber"] = pageNum;
                pageEvent["totalPages"] = pageCount;
                pageEvent["pageData"] = pageData;
                pageEvent["timestamp"] = std::time(nullptr);
                
                sendEventToJava(clientId, "document_event", pageEvent.dump(), requestId);
            }
            
            // Send completion
            json completeEvent;
            completeEvent["eventType"] = "document_complete";
            completeEvent["requestId"] = requestId;
            completeEvent["filename"] = filename;
            completeEvent["totalPages"] = pageCount;
            completeEvent["timestamp"] = std::time(nullptr);
            
            sendEventToJava(clientId, "document_event", completeEvent.dump(), requestId);
            
            json response;
            response["worker"] = "ZZA0_0100_W";
            response["status"] = "streaming";
            response["message"] = "Pages are being streamed to Java via gRPC events";
            response["requestId"] = requestId;
            response["pageCount"] = pageCount;
            response["timestamp"] = std::time(nullptr);
            
            return response.dump();
            
        } catch (const std::exception& e) {
            json error;
            error["status"] = "error";
            error["error"] = e.what();
            return error.dump();
        }
    }
};

} // namespace workers
} // namespace deepapp

// Register the worker
REGISTER_WORKER(deepapp::workers::ZZA0_0100_Worker, "ZZA0_0100_W")
