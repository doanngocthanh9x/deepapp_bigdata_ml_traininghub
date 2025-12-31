-- AAA0_0104 Database Schema for Document Processing
-- SQLite database schema for tasks_files, pages, and bboxes tables

-- Create tasks_files table
CREATE TABLE IF NOT EXISTS tasks_files (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    file_name TEXT NOT NULL,
    file_path TEXT NOT NULL,
    file_size INTEGER NOT NULL,
    mime_type TEXT NOT NULL,
    page_count INTEGER DEFAULT 0,
    status TEXT DEFAULT 'pending',
    request_id TEXT UNIQUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Create pages table
CREATE TABLE IF NOT EXISTS pages (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER NOT NULL,
    page_number INTEGER NOT NULL,
    file_path TEXT,
    width INTEGER DEFAULT 0,
    height INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES tasks_files(id) ON DELETE CASCADE,
    UNIQUE(task_id, page_number)
);

-- Create bboxes table
CREATE TABLE IF NOT EXISTS bboxes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    page_id INTEGER NOT NULL,
    coordinates TEXT NOT NULL, -- JSON string of bbox coordinates
    class TEXT NOT NULL,
    confidence REAL NOT NULL,
    file_path TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (page_id) REFERENCES pages(id) ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_tasks_files_status ON tasks_files(status);
CREATE INDEX IF NOT EXISTS idx_tasks_files_request_id ON tasks_files(request_id);
CREATE INDEX IF NOT EXISTS idx_pages_task_id ON pages(task_id);
CREATE INDEX IF NOT EXISTS idx_bboxes_page_id ON bboxes(page_id);
CREATE INDEX IF NOT EXISTS idx_bboxes_class ON bboxes(class);

-- Insert sample data for testing (optional)
INSERT OR IGNORE INTO tasks_files (file_name, file_path, file_size, mime_type, page_count, status, request_id)
VALUES ('sample.pdf', '/uploads/sample.pdf', 1024000, 'application/pdf', 5, 'completed', 'sample-request-123');