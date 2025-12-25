#ifndef DEEPAPP_STORAGE_DOCUMENT_STORAGE_H
#define DEEPAPP_STORAGE_DOCUMENT_STORAGE_H

#include <string>
#include <vector>
#include <memory>
#include <ctime>
#include <map>
#include <sqlite3.h>

namespace deepapp {
namespace storage {

/**
 * Document metadata structure
 */
struct DocumentRecord {
    int id;
    std::string requestId;
    std::string filename;
    std::string filePath;
    std::string format;  // PDF, TIFF, TIF, PNG, JPEG
    int pageCount;
    long fileSize;
    std::string status;  // processing, completed, failed
    std::time_t createdAt;
    std::time_t updatedAt;
    std::string errorMessage;
};

/**
 * Page data structure
 */
struct PageRecord {
    int id;
    int documentId;
    std::string requestId;
    int pageNumber;
    int width;
    int height;
    int dpi;
    std::string format;
    std::string imagePath;      // Path to saved PNG file
    std::string imageData;      // Base64 data (optional, for small images)
    std::string text;           // Extracted OCR text
    std::string status;         // rendered, ocr_pending, ocr_completed
    std::time_t createdAt;
};

/**
 * Task tracking structure
 */
struct TaskRecord {
    int id;
    std::string requestId;
    std::string taskType;       // document_processing, ocr, ner, etc.
    std::string status;         // pending, running, completed, failed
    int totalPages;
    int processedPages;
    std::time_t startedAt;
    std::time_t completedAt;
    std::string errorMessage;
};

/**
 * DocumentStorage - SQLite-based storage for documents, pages, and tasks
 * Thread-safe, production-ready storage layer
 */
class DocumentStorage {
public:
    /**
     * Constructor
     * @param dbPath Path to SQLite database file (default: /tmp/deepapp/documents.db)
     */
    explicit DocumentStorage(const std::string& dbPath = "/tmp/deepapp/documents.db");
    
    /**
     * Destructor - closes database connection
     */
    ~DocumentStorage();

    // Prevent copying
    DocumentStorage(const DocumentStorage&) = delete;
    DocumentStorage& operator=(const DocumentStorage&) = delete;

    /**
     * Initialize database and create tables if they don't exist
     */
    bool initialize();

    /**
     * Close database connection
     */
    void close();

    // ============================================================
    // DOCUMENT OPERATIONS
    // ============================================================

    /**
     * Save document metadata
     * @return Document ID (auto-generated) or -1 on error
     */
    int saveDocument(const DocumentRecord& doc);

    /**
     * Update document status
     */
    bool updateDocumentStatus(const std::string& requestId, 
                             const std::string& status,
                             const std::string& errorMessage = "");

    /**
     * Get document by request ID
     */
    DocumentRecord getDocument(const std::string& requestId);

    /**
     * Get document by ID
     */
    DocumentRecord getDocumentById(int documentId);

    /**
     * Get all documents (optionally filter by status)
     */
    std::vector<DocumentRecord> getDocuments(const std::string& status = "");

    /**
     * Delete document and all its pages
     */
    bool deleteDocument(const std::string& requestId);

    // ============================================================
    // PAGE OPERATIONS
    // ============================================================

    /**
     * Save page data
     * @return Page ID or -1 on error
     */
    int savePage(const PageRecord& page);

    /**
     * Get page by document request ID and page number
     */
    PageRecord getPage(const std::string& requestId, int pageNumber);

    /**
     * Get all pages for a document
     */
    std::vector<PageRecord> getPages(const std::string& requestId);

    /**
     * Update page status (e.g., after OCR)
     */
    bool updatePageStatus(int pageId, const std::string& status);

    /**
     * Update page OCR text
     */
    bool updatePageText(int pageId, const std::string& text);

    /**
     * Delete page
     */
    bool deletePage(int pageId);

    // ============================================================
    // TASK OPERATIONS
    // ============================================================

    /**
     * Create new task
     */
    int createTask(const TaskRecord& task);

    /**
     * Update task progress
     */
    bool updateTaskProgress(const std::string& requestId, int processedPages);

    /**
     * Update task status
     */
    bool updateTaskStatus(const std::string& requestId, 
                         const std::string& status,
                         const std::string& errorMessage = "");

    /**
     * Get task by request ID
     */
    TaskRecord getTask(const std::string& requestId);

    /**
     * Get all tasks (optionally filter by status)
     */
    std::vector<TaskRecord> getTasks(const std::string& status = "");

    /**
     * Delete task
     */
    bool deleteTask(const std::string& requestId);

    // ============================================================
    // UTILITY OPERATIONS
    // ============================================================

    /**
     * Clear all data (DANGEROUS - deletes everything!)
     */
    bool clearAll();

    /**
     * Clear old data (older than N days)
     */
    bool clearOldData(int daysOld = 7);

    /**
     * Get database statistics
     */
    std::map<std::string, int> getStatistics();

    /**
     * Get last error message
     */
    std::string getLastError() const { return lastError_; }

    /**
     * Check if database is open
     */
    bool isOpen() const { return db_ != nullptr; }

private:
    sqlite3* db_;
    std::string dbPath_;
    std::string lastError_;

    /**
     * Execute SQL query
     */
    bool executeSQL(const std::string& sql);

    /**
     * Set last error from SQLite
     */
    void setLastError();

    /**
     * Create database schema
     */
    bool createSchema();

    /**
     * Convert time_t to SQL timestamp string
     */
    std::string timeToSQL(std::time_t time);

    /**
     * Convert SQL timestamp to time_t
     */
    std::time_t sqlToTime(const std::string& sqlTime);
};

} // namespace storage
} // namespace deepapp

#endif // DEEPAPP_STORAGE_DOCUMENT_STORAGE_H
