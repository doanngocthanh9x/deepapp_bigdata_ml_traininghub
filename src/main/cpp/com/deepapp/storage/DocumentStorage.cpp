#include "com/deepapp/storage/DocumentStorage.h"
#include <iostream>
#include <sstream>
#include <iomanip>
#include <sys/stat.h>
#include <unistd.h>

namespace deepapp {
namespace storage {

DocumentStorage::DocumentStorage(const std::string& dbPath)
    : db_(nullptr), dbPath_(dbPath), lastError_("") {
}

DocumentStorage::~DocumentStorage() {
    close();
}

bool DocumentStorage::initialize() {
    // Create directory if it doesn't exist
    std::string dir = dbPath_.substr(0, dbPath_.find_last_of('/'));
    mkdir(dir.c_str(), 0755);

    // Open database
    int rc = sqlite3_open(dbPath_.c_str(), &db_);
    if (rc != SQLITE_OK) {
        setLastError();
        std::cerr << "[DocumentStorage] Failed to open database: " << lastError_ << std::endl;
        return false;
    }

    std::cout << "[DocumentStorage] Database opened: " << dbPath_ << std::endl;

    // Enable foreign keys
    executeSQL("PRAGMA foreign_keys = ON;");
    
    // Create tables
    if (!createSchema()) {
        std::cerr << "[DocumentStorage] Failed to create schema" << std::endl;
        return false;
    }

    std::cout << "[DocumentStorage] Initialized successfully" << std::endl;
    return true;
}

void DocumentStorage::close() {
    if (db_) {
        sqlite3_close(db_);
        db_ = nullptr;
        std::cout << "[DocumentStorage] Database closed" << std::endl;
    }
}

bool DocumentStorage::createSchema() {
    // Documents table
    std::string documentsTable = R"(
        CREATE TABLE IF NOT EXISTS documents (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            request_id TEXT UNIQUE NOT NULL,
            filename TEXT NOT NULL,
            file_path TEXT,
            format TEXT NOT NULL,
            page_count INTEGER DEFAULT 0,
            file_size INTEGER DEFAULT 0,
            status TEXT DEFAULT 'processing',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            error_message TEXT
        );
    )";

    // Pages table
    std::string pagesTable = R"(
        CREATE TABLE IF NOT EXISTS pages (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            document_id INTEGER NOT NULL,
            request_id TEXT NOT NULL,
            page_number INTEGER NOT NULL,
            width INTEGER DEFAULT 0,
            height INTEGER DEFAULT 0,
            dpi INTEGER DEFAULT 150,
            format TEXT,
            image_path TEXT,
            image_data TEXT,
            text TEXT,
            status TEXT DEFAULT 'rendered',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
            UNIQUE(request_id, page_number)
        );
    )";

    // Tasks table
    std::string tasksTable = R"(
        CREATE TABLE IF NOT EXISTS tasks (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            request_id TEXT UNIQUE NOT NULL,
            task_type TEXT NOT NULL,
            status TEXT DEFAULT 'pending',
            total_pages INTEGER DEFAULT 0,
            processed_pages INTEGER DEFAULT 0,
            started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            completed_at TIMESTAMP,
            error_message TEXT
        );
    )";

    // Create indexes for faster queries
    std::string indexes = R"(
        CREATE INDEX IF NOT EXISTS idx_documents_request_id ON documents(request_id);
        CREATE INDEX IF NOT EXISTS idx_documents_status ON documents(status);
        CREATE INDEX IF NOT EXISTS idx_pages_request_id ON pages(request_id);
        CREATE INDEX IF NOT EXISTS idx_pages_document_id ON pages(document_id);
        CREATE INDEX IF NOT EXISTS idx_pages_page_number ON pages(page_number);
        CREATE INDEX IF NOT EXISTS idx_tasks_request_id ON tasks(request_id);
        CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status);
    )";

    return executeSQL(documentsTable) && 
           executeSQL(pagesTable) && 
           executeSQL(tasksTable) &&
           executeSQL(indexes);
}

bool DocumentStorage::executeSQL(const std::string& sql) {
    char* errMsg = nullptr;
    int rc = sqlite3_exec(db_, sql.c_str(), nullptr, nullptr, &errMsg);
    
    if (rc != SQLITE_OK) {
        if (errMsg) {
            lastError_ = errMsg;
            sqlite3_free(errMsg);
        }
        std::cerr << "[DocumentStorage] SQL error: " << lastError_ << std::endl;
        return false;
    }
    
    return true;
}

void DocumentStorage::setLastError() {
    if (db_) {
        lastError_ = sqlite3_errmsg(db_);
    }
}

std::string DocumentStorage::timeToSQL(std::time_t time) {
    std::tm* tm = std::gmtime(&time);
    std::ostringstream oss;
    oss << std::put_time(tm, "%Y-%m-%d %H:%M:%S");
    return oss.str();
}

std::time_t DocumentStorage::sqlToTime(const std::string& sqlTime) {
    std::tm tm = {};
    std::istringstream ss(sqlTime);
    ss >> std::get_time(&tm, "%Y-%m-%d %H:%M:%S");
    return std::mktime(&tm);
}

// ============================================================
// DOCUMENT OPERATIONS
// ============================================================

int DocumentStorage::saveDocument(const DocumentRecord& doc) {
    std::ostringstream sql;
    sql << "INSERT INTO documents (request_id, filename, file_path, format, "
        << "page_count, file_size, status, error_message) VALUES ('"
        << doc.requestId << "', '" << doc.filename << "', '" << doc.filePath << "', '"
        << doc.format << "', " << doc.pageCount << ", " << doc.fileSize << ", '"
        << doc.status << "', '" << doc.errorMessage << "');";

    if (!executeSQL(sql.str())) {
        return -1;
    }

    return static_cast<int>(sqlite3_last_insert_rowid(db_));
}

bool DocumentStorage::updateDocumentStatus(const std::string& requestId, 
                                           const std::string& status,
                                           const std::string& errorMessage) {
    std::ostringstream sql;
    sql << "UPDATE documents SET status = '" << status 
        << "', updated_at = CURRENT_TIMESTAMP";
    
    if (!errorMessage.empty()) {
        sql << ", error_message = '" << errorMessage << "'";
    }
    
    sql << " WHERE request_id = '" << requestId << "';";

    return executeSQL(sql.str());
}

DocumentRecord DocumentStorage::getDocument(const std::string& requestId) {
    DocumentRecord doc = {};
    
    std::string sql = "SELECT id, request_id, filename, file_path, format, "
                     "page_count, file_size, status, created_at, updated_at, error_message "
                     "FROM documents WHERE request_id = '" + requestId + "';";

    sqlite3_stmt* stmt;
    int rc = sqlite3_prepare_v2(db_, sql.c_str(), -1, &stmt, nullptr);
    
    if (rc != SQLITE_OK) {
        setLastError();
        return doc;
    }

    if (sqlite3_step(stmt) == SQLITE_ROW) {
        doc.id = sqlite3_column_int(stmt, 0);
        doc.requestId = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 1));
        doc.filename = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 2));
        doc.filePath = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 3));
        doc.format = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 4));
        doc.pageCount = sqlite3_column_int(stmt, 5);
        doc.fileSize = sqlite3_column_int64(stmt, 6);
        doc.status = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 7));
        // Parse timestamps if needed
        const char* errorMsg = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 10));
        if (errorMsg) doc.errorMessage = errorMsg;
    }

    sqlite3_finalize(stmt);
    return doc;
}

DocumentRecord DocumentStorage::getDocumentById(int documentId) {
    DocumentRecord doc = {};
    
    std::ostringstream sql;
    sql << "SELECT id, request_id, filename, file_path, format, "
        << "page_count, file_size, status, created_at, updated_at, error_message "
        << "FROM documents WHERE id = " << documentId << ";";

    sqlite3_stmt* stmt;
    int rc = sqlite3_prepare_v2(db_, sql.str().c_str(), -1, &stmt, nullptr);
    
    if (rc != SQLITE_OK) {
        setLastError();
        return doc;
    }

    if (sqlite3_step(stmt) == SQLITE_ROW) {
        doc.id = sqlite3_column_int(stmt, 0);
        doc.requestId = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 1));
        doc.filename = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 2));
        doc.filePath = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 3));
        doc.format = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 4));
        doc.pageCount = sqlite3_column_int(stmt, 5);
        doc.fileSize = sqlite3_column_int64(stmt, 6);
        doc.status = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 7));
        const char* errorMsg = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 10));
        if (errorMsg) doc.errorMessage = errorMsg;
    }

    sqlite3_finalize(stmt);
    return doc;
}

std::vector<DocumentRecord> DocumentStorage::getDocuments(const std::string& status) {
    std::vector<DocumentRecord> docs;
    
    std::string sql = "SELECT id, request_id, filename, file_path, format, "
                     "page_count, file_size, status, created_at, updated_at, error_message "
                     "FROM documents";
    
    if (!status.empty()) {
        sql += " WHERE status = '" + status + "'";
    }
    
    sql += " ORDER BY created_at DESC;";

    sqlite3_stmt* stmt;
    int rc = sqlite3_prepare_v2(db_, sql.c_str(), -1, &stmt, nullptr);
    
    if (rc != SQLITE_OK) {
        setLastError();
        return docs;
    }

    while (sqlite3_step(stmt) == SQLITE_ROW) {
        DocumentRecord doc;
        doc.id = sqlite3_column_int(stmt, 0);
        doc.requestId = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 1));
        doc.filename = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 2));
        doc.filePath = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 3));
        doc.format = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 4));
        doc.pageCount = sqlite3_column_int(stmt, 5);
        doc.fileSize = sqlite3_column_int64(stmt, 6);
        doc.status = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 7));
        const char* errorMsg = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 10));
        if (errorMsg) doc.errorMessage = errorMsg;
        docs.push_back(doc);
    }

    sqlite3_finalize(stmt);
    return docs;
}

bool DocumentStorage::deleteDocument(const std::string& requestId) {
    std::string sql = "DELETE FROM documents WHERE request_id = '" + requestId + "';";
    return executeSQL(sql);
}

// ============================================================
// PAGE OPERATIONS
// ============================================================

int DocumentStorage::savePage(const PageRecord& page) {
    std::ostringstream sql;
    sql << "INSERT INTO pages (document_id, request_id, page_number, width, height, "
        << "dpi, format, image_path, image_data, text, status) VALUES ("
        << page.documentId << ", '" << page.requestId << "', " << page.pageNumber << ", "
        << page.width << ", " << page.height << ", " << page.dpi << ", '"
        << page.format << "', '" << page.imagePath << "', '', '" 
        << page.text << "', '" << page.status << "');";
    
    // Note: image_data is empty to avoid storing huge base64 in DB
    // Store to file instead and save path in image_path

    if (!executeSQL(sql.str())) {
        return -1;
    }

    return static_cast<int>(sqlite3_last_insert_rowid(db_));
}

PageRecord DocumentStorage::getPage(const std::string& requestId, int pageNumber) {
    PageRecord page = {};
    
    std::ostringstream sql;
    sql << "SELECT id, document_id, request_id, page_number, width, height, "
        << "dpi, format, image_path, text, status, created_at "
        << "FROM pages WHERE request_id = '" << requestId 
        << "' AND page_number = " << pageNumber << ";";

    sqlite3_stmt* stmt;
    int rc = sqlite3_prepare_v2(db_, sql.str().c_str(), -1, &stmt, nullptr);
    
    if (rc != SQLITE_OK) {
        setLastError();
        return page;
    }

    if (sqlite3_step(stmt) == SQLITE_ROW) {
        page.id = sqlite3_column_int(stmt, 0);
        page.documentId = sqlite3_column_int(stmt, 1);
        page.requestId = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 2));
        page.pageNumber = sqlite3_column_int(stmt, 3);
        page.width = sqlite3_column_int(stmt, 4);
        page.height = sqlite3_column_int(stmt, 5);
        page.dpi = sqlite3_column_int(stmt, 6);
        page.format = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 7));
        const char* imagePath = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 8));
        if (imagePath) page.imagePath = imagePath;
        const char* text = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 9));
        if (text) page.text = text;
        page.status = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 10));
    }

    sqlite3_finalize(stmt);
    return page;
}

std::vector<PageRecord> DocumentStorage::getPages(const std::string& requestId) {
    std::vector<PageRecord> pages;
    
    std::ostringstream sql;
    sql << "SELECT id, document_id, request_id, page_number, width, height, "
        << "dpi, format, image_path, text, status, created_at "
        << "FROM pages WHERE request_id = '" << requestId 
        << "' ORDER BY page_number;";

    sqlite3_stmt* stmt;
    int rc = sqlite3_prepare_v2(db_, sql.str().c_str(), -1, &stmt, nullptr);
    
    if (rc != SQLITE_OK) {
        setLastError();
        return pages;
    }

    while (sqlite3_step(stmt) == SQLITE_ROW) {
        PageRecord page;
        page.id = sqlite3_column_int(stmt, 0);
        page.documentId = sqlite3_column_int(stmt, 1);
        page.requestId = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 2));
        page.pageNumber = sqlite3_column_int(stmt, 3);
        page.width = sqlite3_column_int(stmt, 4);
        page.height = sqlite3_column_int(stmt, 5);
        page.dpi = sqlite3_column_int(stmt, 6);
        page.format = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 7));
        const char* imagePath = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 8));
        if (imagePath) page.imagePath = imagePath;
        const char* text = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 9));
        if (text) page.text = text;
        page.status = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 10));
        pages.push_back(page);
    }

    sqlite3_finalize(stmt);
    return pages;
}

bool DocumentStorage::updatePageStatus(int pageId, const std::string& status) {
    std::ostringstream sql;
    sql << "UPDATE pages SET status = '" << status << "' WHERE id = " << pageId << ";";
    return executeSQL(sql.str());
}

bool DocumentStorage::updatePageText(int pageId, const std::string& text) {
    std::ostringstream sql;
    sql << "UPDATE pages SET text = '" << text << "' WHERE id = " << pageId << ";";
    return executeSQL(sql.str());
}

bool DocumentStorage::deletePage(int pageId) {
    std::ostringstream sql;
    sql << "DELETE FROM pages WHERE id = " << pageId << ";";
    return executeSQL(sql.str());
}

// ============================================================
// TASK OPERATIONS
// ============================================================

int DocumentStorage::createTask(const TaskRecord& task) {
    std::ostringstream sql;
    sql << "INSERT INTO tasks (request_id, task_type, status, total_pages, "
        << "processed_pages, error_message) VALUES ('"
        << task.requestId << "', '" << task.taskType << "', '" << task.status << "', "
        << task.totalPages << ", " << task.processedPages << ", '" 
        << task.errorMessage << "');";

    if (!executeSQL(sql.str())) {
        return -1;
    }

    return static_cast<int>(sqlite3_last_insert_rowid(db_));
}

bool DocumentStorage::updateTaskProgress(const std::string& requestId, int processedPages) {
    std::ostringstream sql;
    sql << "UPDATE tasks SET processed_pages = " << processedPages 
        << " WHERE request_id = '" << requestId << "';";
    return executeSQL(sql.str());
}

bool DocumentStorage::updateTaskStatus(const std::string& requestId, 
                                       const std::string& status,
                                       const std::string& errorMessage) {
    std::ostringstream sql;
    sql << "UPDATE tasks SET status = '" << status << "'";
    
    if (status == "completed" || status == "failed") {
        sql << ", completed_at = CURRENT_TIMESTAMP";
    }
    
    if (!errorMessage.empty()) {
        sql << ", error_message = '" << errorMessage << "'";
    }
    
    sql << " WHERE request_id = '" << requestId << "';";

    return executeSQL(sql.str());
}

TaskRecord DocumentStorage::getTask(const std::string& requestId) {
    TaskRecord task = {};
    
    std::string sql = "SELECT id, request_id, task_type, status, total_pages, "
                     "processed_pages, started_at, completed_at, error_message "
                     "FROM tasks WHERE request_id = '" + requestId + "';";

    sqlite3_stmt* stmt;
    int rc = sqlite3_prepare_v2(db_, sql.c_str(), -1, &stmt, nullptr);
    
    if (rc != SQLITE_OK) {
        setLastError();
        return task;
    }

    if (sqlite3_step(stmt) == SQLITE_ROW) {
        task.id = sqlite3_column_int(stmt, 0);
        task.requestId = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 1));
        task.taskType = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 2));
        task.status = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 3));
        task.totalPages = sqlite3_column_int(stmt, 4);
        task.processedPages = sqlite3_column_int(stmt, 5);
        const char* errorMsg = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 8));
        if (errorMsg) task.errorMessage = errorMsg;
    }

    sqlite3_finalize(stmt);
    return task;
}

std::vector<TaskRecord> DocumentStorage::getTasks(const std::string& status) {
    std::vector<TaskRecord> tasks;
    
    std::string sql = "SELECT id, request_id, task_type, status, total_pages, "
                     "processed_pages, started_at, completed_at, error_message "
                     "FROM tasks";
    
    if (!status.empty()) {
        sql += " WHERE status = '" + status + "'";
    }
    
    sql += " ORDER BY started_at DESC;";

    sqlite3_stmt* stmt;
    int rc = sqlite3_prepare_v2(db_, sql.c_str(), -1, &stmt, nullptr);
    
    if (rc != SQLITE_OK) {
        setLastError();
        return tasks;
    }

    while (sqlite3_step(stmt) == SQLITE_ROW) {
        TaskRecord task;
        task.id = sqlite3_column_int(stmt, 0);
        task.requestId = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 1));
        task.taskType = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 2));
        task.status = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 3));
        task.totalPages = sqlite3_column_int(stmt, 4);
        task.processedPages = sqlite3_column_int(stmt, 5);
        const char* errorMsg = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 8));
        if (errorMsg) task.errorMessage = errorMsg;
        tasks.push_back(task);
    }

    sqlite3_finalize(stmt);
    return tasks;
}

bool DocumentStorage::deleteTask(const std::string& requestId) {
    std::string sql = "DELETE FROM tasks WHERE request_id = '" + requestId + "';";
    return executeSQL(sql);
}

// ============================================================
// UTILITY OPERATIONS
// ============================================================

bool DocumentStorage::clearAll() {
    return executeSQL("DELETE FROM pages;") &&
           executeSQL("DELETE FROM documents;") &&
           executeSQL("DELETE FROM tasks;");
}

bool DocumentStorage::clearOldData(int daysOld) {
    std::ostringstream sql;
    sql << "DELETE FROM documents WHERE created_at < datetime('now', '-" 
        << daysOld << " days');";
    
    bool result = executeSQL(sql.str());
    
    // Also clean up orphaned pages and tasks
    executeSQL("DELETE FROM pages WHERE document_id NOT IN (SELECT id FROM documents);");
    executeSQL("DELETE FROM tasks WHERE request_id NOT IN (SELECT request_id FROM documents);");
    
    return result;
}

std::map<std::string, int> DocumentStorage::getStatistics() {
    std::map<std::string, int> stats;
    
    // Count documents
    std::string sql = "SELECT COUNT(*) FROM documents;";
    sqlite3_stmt* stmt;
    
    if (sqlite3_prepare_v2(db_, sql.c_str(), -1, &stmt, nullptr) == SQLITE_OK) {
        if (sqlite3_step(stmt) == SQLITE_ROW) {
            stats["total_documents"] = sqlite3_column_int(stmt, 0);
        }
        sqlite3_finalize(stmt);
    }
    
    // Count pages
    sql = "SELECT COUNT(*) FROM pages;";
    if (sqlite3_prepare_v2(db_, sql.c_str(), -1, &stmt, nullptr) == SQLITE_OK) {
        if (sqlite3_step(stmt) == SQLITE_ROW) {
            stats["total_pages"] = sqlite3_column_int(stmt, 0);
        }
        sqlite3_finalize(stmt);
    }
    
    // Count tasks
    sql = "SELECT COUNT(*) FROM tasks;";
    if (sqlite3_prepare_v2(db_, sql.c_str(), -1, &stmt, nullptr) == SQLITE_OK) {
        if (sqlite3_step(stmt) == SQLITE_ROW) {
            stats["total_tasks"] = sqlite3_column_int(stmt, 0);
        }
        sqlite3_finalize(stmt);
    }
    
    // Count by status
    sql = "SELECT status, COUNT(*) FROM documents GROUP BY status;";
    if (sqlite3_prepare_v2(db_, sql.c_str(), -1, &stmt, nullptr) == SQLITE_OK) {
        while (sqlite3_step(stmt) == SQLITE_ROW) {
            std::string status = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 0));
            int count = sqlite3_column_int(stmt, 1);
            stats["documents_" + status] = count;
        }
        sqlite3_finalize(stmt);
    }
    
    return stats;
}

} // namespace storage
} // namespace deepapp
