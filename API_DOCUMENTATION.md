# 📚 Document Management System - API Documentation

## Base URL
```
http://localhost:8080
```

---

## 🚀 Document Processing API

### 1. Stream Document Processing (SSE) - STORAGE CONTROLLED

**Endpoint:** `POST /ZZ/A0/ZZA0_0100/stream`

**Description:** Upload and process PDF/TIFF documents with real-time streaming of results. **ALL processing now goes through DocumentUploadService in storage layer for centralized control.**

**Content-Type:** `multipart/form-data`

**Processing Flow:**
1. **Validation** - File type, size, format validation
2. **Storage** - Save file to `/tmp/deepapp/uploads/{requestId}/`
3. **Database** - Create document, task, page records
4. **Processing** - Stream page-by-page processing results
5. **Completion** - Update status and provide management links

**Request Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `file` | File | Yes | PDF or TIFF file to process |
| `startPage` | Integer | No | Starting page number (default: 1) |
| `maxPages` | Integer | No | Maximum pages to process (default: all) |

**cURL Example:**
```bash
curl -X POST http://localhost:8080/ZZ/A0/ZZA0_0100/stream \
  -F "file=@document.pdf" \
  -F "startPage=1" \
  -F "maxPages=10"
```

**Response:** Server-Sent Events (SSE) Stream

**SSE Event Types:**

#### Event: `status`
```json
{
  "message": "File uploaded successfully, starting processing...",
  "requestId": "req_1735094123456_abc123",
  "filename": "document.pdf",
  "fileSize": 12345678
}
```

#### Event: `metadata`
```json
{
  "requestId": "req_1735094123456_abc123",
  "filename": "document.pdf",
  "format": "pdf",
  "pageCount": 100,
  "fileSize": 12345678
}
```

#### Event: `page`
```json
{
  "requestId": "req_1735094123456_abc123",
  "pageNumber": 1,
  "pageData": {
    "width": 2481,
    "height": 3508,
    "dpi": 150,
    "format": "PNG",
    "imagePath": "/tmp/deepapp/uploads/req_1735094123456_abc123/page_1.png",
    "text": "Extracted text content",
    "status": "completed"
  }
}
```

#### Event: `complete`
```json
{
  "requestId": "req_1735094123456_abc123",
  "totalPages": 100,
  "message": "Document processing completed successfully"
}
```

**Database Records Created:**
- **DocumentEntity**: Stores file metadata and processing status
- **TaskEntity**: Tracks processing progress and status
- **PageEntity**: Stores individual page data and extracted text

**Integration with Document Management:**
- After processing completes, client receives `requestId`
- Can immediately view document in management interface
- All data accessible via REST APIs for further processing

---

## 📄 Document Management API

### 2. Get All Documents

**Endpoint:** `GET /api/documents`

**Description:** Retrieve list of all documents in the system

**Request Parameters:** None

**cURL Example:**
```bash
curl -X GET http://localhost:8080/api/documents
```

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "requestId": "req_1735094123456_abc123",
    "filename": "document.pdf",
    "filePath": "/tmp/deepapp/uploads/req_1735094123456_abc123/document.pdf",
    "format": "pdf",
    "pageCount": 100,
    "fileSize": 12345678,
    "status": "completed",
    "createdAt": "2025-12-25T09:30:00",
    "updatedAt": "2025-12-25T09:35:00",
    "errorMessage": null
  }
]
```

---

### 3. Get Documents by Status

**Endpoint:** `GET /api/documents/status/{status}`

**Description:** Filter documents by processing status

**Path Parameters:**

| Parameter | Type | Values | Description |
|-----------|------|--------|-------------|
| `status` | String | `completed`, `processing`, `failed` | Document status |

**cURL Example:**
```bash
curl -X GET http://localhost:8080/api/documents/status/completed
```

**Response:** `200 OK` - Same format as "Get All Documents"

---

### 4. Get Document by Request ID

**Endpoint:** `GET /api/documents/{requestId}`

**Description:** Get detailed information about a specific document

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `requestId` | String | Unique request identifier |

**cURL Example:**
```bash
curl -X GET http://localhost:8080/api/documents/req_1735094123456_abc123
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "requestId": "req_1735094123456_abc123",
  "filename": "document.pdf",
  "filePath": "/tmp/deepapp/uploads/req_1735094123456_abc123/document.pdf",
  "format": "pdf",
  "pageCount": 100,
  "fileSize": 12345678,
  "status": "completed",
  "createdAt": "2025-12-25T09:30:00",
  "updatedAt": "2025-12-25T09:35:00",
  "errorMessage": null
}
```

**Response:** `404 Not Found` - If document doesn't exist

---

### 5. Get Document Pages

**Endpoint:** `GET /api/documents/{requestId}/pages`

**Description:** Get all pages for a document

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `requestId` | String | Unique request identifier |

**cURL Example:**
```bash
curl -X GET http://localhost:8080/api/documents/req_1735094123456_abc123/pages
```

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "documentId": 1,
    "requestId": "req_1735094123456_abc123",
    "pageNumber": 1,
    "width": 2481,
    "height": 3508,
    "dpi": 150,
    "format": "PNG",
    "imagePath": "/tmp/deepapp/uploads/req_1735094123456_abc123/page_1.png",
    "imageData": "iVBORw0KGgoAAAANSUhEUgAA...(base64)",
    "text": "Extracted text content",
    "status": "completed",
    "createdAt": "2025-12-25T09:31:00"
  },
  {
    "id": 2,
    "documentId": 1,
    "requestId": "req_1735094123456_abc123",
    "pageNumber": 2,
    "width": 2481,
    "height": 3508,
    "dpi": 150,
    "format": "PNG",
    "imagePath": "/tmp/deepapp/uploads/req_1735094123456_abc123/page_2.png",
    "imageData": "iVBORw0KGgoAAAANSUhEUgAA...(base64)",
    "text": "More extracted text",
    "status": "completed",
    "createdAt": "2025-12-25T09:31:15"
  }
]
```

---

### 6. Get Specific Page

**Endpoint:** `GET /api/documents/{requestId}/pages/{pageNumber}`

**Description:** Get a specific page of a document

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `requestId` | String | Unique request identifier |
| `pageNumber` | Integer | Page number (1-based) |

**cURL Example:**
```bash
curl -X GET http://localhost:8080/api/documents/req_1735094123456_abc123/pages/1
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "documentId": 1,
  "requestId": "req_1735094123456_abc123",
  "pageNumber": 1,
  "width": 2481,
  "height": 3508,
  "dpi": 150,
  "format": "PNG",
  "imagePath": "/tmp/deepapp/uploads/req_1735094123456_abc123/page_1.png",
  "imageData": "iVBORw0KGgoAAAANSUhEUgAA...(base64)",
  "text": "Extracted text content from page 1",
  "status": "completed",
  "createdAt": "2025-12-25T09:31:00"
}
```

**Response:** `404 Not Found` - If page doesn't exist

---

### 7. Delete Document

**Endpoint:** `DELETE /api/documents/{requestId}`

**Description:** Delete a document and all its pages (including physical files)

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `requestId` | String | Unique request identifier |

**cURL Example:**
```bash
curl -X DELETE http://localhost:8080/api/documents/req_1735094123456_abc123
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Document deleted successfully",
  "requestId": "req_1735094123456_abc123"
}
```

**Response:** `404 Not Found` - If document doesn't exist

---

### 8. Delete Multiple Documents

**Endpoint:** `POST /api/documents/delete-batch`

**Description:** Delete multiple documents in one request

**Content-Type:** `application/json`

**Request Body:**
```json
[
  "req_1735094123456_abc123",
  "req_1735094123457_def456",
  "req_1735094123458_ghi789"
]
```

**cURL Example:**
```bash
curl -X POST http://localhost:8080/api/documents/delete-batch \
  -H "Content-Type: application/json" \
  -d '["req_1735094123456_abc123", "req_1735094123457_def456"]'
```

**Response:** `200 OK`
```json
{
  "success": true,
  "deleted": 2,
  "total": 2
}
```

---

### 9. Get Statistics

**Endpoint:** `GET /api/documents/statistics`

**Description:** Get basic statistics about documents

**cURL Example:**
```bash
curl -X GET http://localhost:8080/api/documents/statistics
```

**Response:** `200 OK`
```json
{
  "totalDocuments": 150,
  "totalPages": 12500,
  "totalStorageSize": 1073741824,
  "completedDocuments": 145,
  "processingDocuments": 2,
  "failedDocuments": 3
}
```

---

### 10. Get Detailed Statistics

**Endpoint:** `GET /api/documents/statistics/detailed`

**Description:** Get detailed statistics with breakdown by status

**cURL Example:**
```bash
curl -X GET http://localhost:8080/api/documents/statistics/detailed
```

**Response:** `200 OK`
```json
{
  "totalDocuments": 150,
  "totalPages": 12500,
  "totalStorageSize": 1073741824,
  "totalStorageSizeFormatted": "1.00 GB",
  "completedDocuments": 145,
  "processingDocuments": 2,
  "failedDocuments": 3,
  "statusBreakdown": {
    "completed": 145,
    "processing": 2,
    "failed": 3
  },
  "averagePagesPerDocument": 83.33,
  "averageFileSizeBytes": 7158278
}
```

---

## 🗑️ Cleanup APIs

### 11. Cleanup Old Documents

**Endpoint:** `POST /api/documents/cleanup`

**Description:** Manually trigger cleanup of old documents

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `days` | Integer | No | 7 | Delete documents older than N days |

**cURL Example:**
```bash
curl -X POST "http://localhost:8080/api/documents/cleanup?days=30"
```

**Response:** `200 OK`
```json
{
  "success": true,
  "deletedCount": 25,
  "message": "Cleanup completed successfully"
}
```

---

### 12. Cleanup by Retention Policy

**Endpoint:** `POST /api/documents/cleanup/retention`

**Description:** Cleanup documents using flexible time units

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `amount` | Integer | Yes | Time amount (e.g., 30) |
| `unit` | String | Yes | Time unit: `minutes`, `hours`, `days`, `months` |

**cURL Examples:**
```bash
# Delete documents older than 30 minutes
curl -X POST "http://localhost:8080/api/documents/cleanup/retention?amount=30&unit=minutes"

# Delete documents older than 2 hours
curl -X POST "http://localhost:8080/api/documents/cleanup/retention?amount=2&unit=hours"

# Delete documents older than 7 days
curl -X POST "http://localhost:8080/api/documents/cleanup/retention?amount=7&unit=days"

# Delete documents older than 3 months
curl -X POST "http://localhost:8080/api/documents/cleanup/retention?amount=3&unit=months"
```

**Response:** `200 OK`
```json
{
  "success": true,
  "deletedCount": 15,
  "message": "Cleanup completed successfully"
}
```

---

### 13. Cleanup Failed Documents

**Endpoint:** `POST /api/documents/cleanup/failed`

**Description:** Remove all documents with 'failed' status

**cURL Example:**
```bash
curl -X POST http://localhost:8080/api/documents/cleanup/failed
```

**Response:** `200 OK`
```json
{
  "success": true,
  "deletedCount": 3,
  "message": "Failed documents cleaned up successfully"
}
```

---

### 14. Clear All Data (DANGEROUS)

**Endpoint:** `DELETE /api/documents/clear-all`

**Description:** Delete ALL documents from the system. Use with caution!

**cURL Example:**
```bash
curl -X DELETE http://localhost:8080/api/documents/clear-all
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "All data cleared successfully"
}
```

---

## 🎨 Web UI Endpoints

### 15. Document Processing UI

**URL:** `http://localhost:8080/document-stream-test.html`

**Description:** Interactive web interface for uploading and processing documents with real-time streaming

**Features:**
- Drag & drop file upload
- Real-time page processing progress
- Visual page preview
- Direct link to document management

---

### 16. Document Management UI

**URL:** `http://localhost:8080/document-management.html`

**Description:** Complete document management dashboard

**Features:**
- View all documents with statistics
- Filter by status (completed/processing/failed)
- View document pages with images
- Delete documents (single or batch)
- Manual cleanup with flexible retention policies
- Auto-refresh every 30 seconds

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `requestId` | String | Highlight specific document (from processing UI) |

---

## � Architecture - Storage Layer Control

### Document Processing Flow

```
1. Client Upload → POST /ZZ/A0/ZZA0_0100/stream
2. Controller → DocumentUploadService.processDocumentUpload()
3. Validation → File type, size, format checks
4. Storage → Save to /tmp/deepapp/uploads/{requestId}/
5. Database → Create DocumentEntity, TaskEntity records
6. Processing → Stream page-by-page via SSE
7. Completion → Update status, provide management access
8. Management → View in document-management.html
```

### Service Layer Responsibilities

**DocumentUploadService** - Central upload control:
- ✅ File validation (type, size, format)
- ✅ File storage management
- ✅ Database record creation (documents, tasks, pages)
- ✅ Processing coordination
- ✅ Status tracking and updates
- ✅ Error handling and cleanup

**DocumentManagementService** - Data management:
- ✅ CRUD operations for documents/pages
- ✅ Statistics and reporting
- ✅ Scheduled cleanup (cron jobs)
- ✅ Batch operations
- ✅ Storage management

### Data Flow

```
Upload Request
    ↓
DocumentUploadService (Validation & Storage)
    ↓
SQLite Database (Documents, Tasks, Pages)
    ↓
SSE Streaming (Real-time progress)
    ↓
Document Management UI (View & Control)
    ↓
REST APIs (Programmatic access)
```

### Document Entity
```typescript
{
  id: number;                    // Primary key
  requestId: string;             // Unique request identifier
  filename: string;              // Original filename
  filePath: string;              // Physical file path
  format: string;                // File format (pdf, tiff)
  pageCount: number;             // Total number of pages
  fileSize: number;              // File size in bytes
  status: string;                // Status: completed, processing, failed
  createdAt: string;             // ISO 8601 timestamp
  updatedAt: string;             // ISO 8601 timestamp
  errorMessage: string | null;   // Error details if failed
}
```

### Page Entity
```typescript
{
  id: number;                    // Primary key
  documentId: number;            // Foreign key to document
  requestId: string;             // Parent document request ID
  pageNumber: number;            // Page number (1-based)
  width: number;                 // Image width in pixels
  height: number;                // Image height in pixels
  dpi: number;                   // Dots per inch
  format: string;                // Image format (PNG, JPEG)
  imagePath: string;             // Physical image file path
  imageData: string;             // Base64 encoded image (from API only)
  text: string;                  // Extracted text content
  status: string;                // Status: completed, failed
  createdAt: string;             // ISO 8601 timestamp
}
```

### Task Entity
```typescript
{
  id: number;                    // Primary key
  requestId: string;             // Unique task identifier (FK to document)
  taskType: string;              // Task type: document_processing
  status: string;                // Status: pending, running, completed, failed
  totalPages: number;            // Total pages to process
  processedPages: number;        // Number of pages processed
  startedAt: string;             // ISO 8601 timestamp
  completedAt: string | null;    // ISO 8601 timestamp
  errorMessage: string | null;   // Error details if failed
}
```

---

## ⚙️ Configuration

### Application Properties (`application.yml`)

```yaml
# Database Configuration
spring:
  datasource:
    url: jdbc:sqlite:/tmp/deepapp/documents.db
    driver-class-name: org.sqlite.JDBC
  jpa:
    database-platform: org.hibernate.community.dialect.SQLiteDialect
    hibernate:
      ddl-auto: update  # Auto-create/update tables
    show-sql: false

# Document Storage Configuration
document:
  storage:
    path: /tmp/deepapp/uploads  # Physical file storage location
  cleanup:
    enabled: true                # Enable scheduled cleanup
    retention-days: 7            # Default retention period
    cron: "0 0 2 * * ?"         # Cleanup schedule (2 AM daily)
```

### Cron Expression Examples

| Expression | Description |
|------------|-------------|
| `0 */5 * * * ?` | Every 5 minutes |
| `0 */30 * * * ?` | Every 30 minutes |
| `0 0 * * * ?` | Every hour |
| `0 0 2 * * ?` | 2 AM daily |
| `0 0 0 * * ?` | Midnight daily |
| `0 0 0 * * SUN` | Midnight every Sunday |

---

## 🔧 Error Responses

### Standard Error Format
```json
{
  "timestamp": "2025-12-25T09:30:00.000+00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Document not found with requestId: req_abc123",
  "path": "/api/documents/req_abc123"
}
```

### Common HTTP Status Codes

| Code | Description |
|------|-------------|
| 200 | OK - Request successful |
| 404 | Not Found - Resource doesn't exist |
| 400 | Bad Request - Invalid parameters |
| 500 | Internal Server Error - Server error |

---

## 📝 Usage Examples

### Java Example (Using RestTemplate)
```java
RestTemplate restTemplate = new RestTemplate();

// Get all documents
ResponseEntity<DocumentEntity[]> response = restTemplate.getForEntity(
    "http://localhost:8080/api/documents",
    DocumentEntity[].class
);
DocumentEntity[] documents = response.getBody();

// Get specific document
DocumentEntity document = restTemplate.getForObject(
    "http://localhost:8080/api/documents/req_1735094123456_abc123",
    DocumentEntity.class
);

// Delete document
restTemplate.delete("http://localhost:8080/api/documents/req_1735094123456_abc123");
```

### Python Example
```python
import requests

# Get all documents
response = requests.get('http://localhost:8080/api/documents')
documents = response.json()

# Get pages
response = requests.get('http://localhost:8080/api/documents/req_1735094123456_abc123/pages')
pages = response.json()

# Delete document
response = requests.delete('http://localhost:8080/api/documents/req_1735094123456_abc123')
print(response.json())

# Cleanup old documents
response = requests.post('http://localhost:8080/api/documents/cleanup/retention', 
    params={'amount': 30, 'unit': 'minutes'})
print(f"Deleted {response.json()['deletedCount']} documents")
```

### JavaScript/TypeScript Example
```typescript
// Get all documents
const response = await fetch('http://localhost:8080/api/documents');
const documents = await response.json();

// Get document pages
const pagesResponse = await fetch(
  `http://localhost:8080/api/documents/${requestId}/pages`
);
const pages = await pagesResponse.json();

// Delete multiple documents
await fetch('http://localhost:8080/api/documents/delete-batch', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(['req_1', 'req_2', 'req_3'])
});

// Get statistics
const stats = await fetch('http://localhost:8080/api/documents/statistics/detailed');
const data = await stats.json();
console.log(`Total: ${data.totalDocuments}, Storage: ${data.totalStorageSizeFormatted}`);
```

---

## 🏗️ Architecture Overview

### Storage Layer Control Architecture

```
┌─────────────────┐    ┌──────────────────────┐    ┌─────────────────┐
│   Controller    │───▶│ DocumentUploadService │───▶│   Repositories  │
│                 │    │  (Storage Layer)      │    │                 │
│ ZZA0_0100       │    │                      │    │ DocumentEntity   │
│ Controller      │    │ • File Validation    │    │ TaskEntity       │
└─────────────────┘    │ • Storage Management │    │ PageEntity       │
                       │ • Database Records   │    └─────────────────┘
                       │ • Processing Control │
                       └──────────────────────┘
                                │
                                ▼
                       ┌──────────────────────┐
                       │  DocumentManagement  │
                       │      Service         │
                       │                      │
                       │ • Statistics         │
                       │ • Cleanup            │
                       │ • UI Integration     │
                       └──────────────────────┘
```

### Service Responsibilities

#### DocumentUploadService (Storage Layer)
- **File Validation**: Type, size, format checks (PDF/TIFF, max 100MB)
- **Storage Management**: Save files to `/tmp/deepapp/uploads/{requestId}/`
- **Database Operations**: Create/update DocumentEntity, TaskEntity, PageEntity
- **Processing Coordination**: Stream page-by-page results via SSE
- **Error Handling**: Comprehensive validation and cleanup on failures

#### DocumentManagementService
- **Statistics**: Document counts, storage usage, processing metrics
- **Cleanup Operations**: Scheduled and manual document deletion
- **UI Integration**: Provide data for web interface
- **API Endpoints**: RESTful access to document information

#### Controller Layer
- **HTTP Handling**: Receive multipart file uploads
- **SSE Streaming**: Real-time progress updates to client
- **Delegation**: Forward processing to DocumentUploadService
- **Error Responses**: Handle and format error responses

### Data Flow

1. **Upload Request** → Controller receives multipart file
2. **Validation** → DocumentUploadService validates file
3. **Storage** → File saved to organized directory structure
4. **Database** → Records created for document, task, pages
5. **Processing** → Page-by-page processing with SSE streaming
6. **Completion** → Status updated, management integration ready
7. **UI Access** → Document highlighted in management interface

### Key Benefits

- **Centralized Control**: All upload logic in one service
- **Separation of Concerns**: Storage, processing, management separated
- **Database Consistency**: Automatic record creation during upload
- **Real-time Feedback**: SSE streaming for progress updates
- **Error Resilience**: Comprehensive validation and cleanup
- **UI Integration**: Seamless document management experience

---

## 🔒 Security Considerations

⚠️ **Current Implementation:** No authentication/authorization

**Recommendations for Production:**
1. Add Spring Security with JWT authentication
2. Implement role-based access control (RBAC)
3. Add rate limiting for API endpoints
4. Validate file types and sizes on server-side
5. Sanitize file names and paths
6. Use HTTPS for all endpoints
7. Add API keys for programmatic access

---

## 📞 Support & Troubleshooting

### Check Application Logs
```bash
tail -f /root/deepapp/deepapp_main/logs/application.log
```

### Check Database
```bash
sqlite3 /tmp/deepapp/documents.db "SELECT * FROM documents;"
```

### Check Physical Files
```bash
ls -lh /tmp/deepapp/uploads/
```

### Clear Database (Reset)
```bash
rm /tmp/deepapp/documents.db
rm -rf /tmp/deepapp/uploads/*
# Restart application to recreate database
```

---

## 📄 License & Version

**Version:** 1.0.0  
**Last Updated:** December 25, 2025  
**Framework:** Spring Boot 4.0.1, SQLite 3.45.0.0

---

**Built with ❤️ by DeepApp Team**
