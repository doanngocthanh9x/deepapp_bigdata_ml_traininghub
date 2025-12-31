# AAA0_0202 - OCR with RAG System

## Tổng quan

AAA0_0202 là module xử lý tài liệu thông minh kết hợp OCR (Optical Character Recognition) với hệ thống RAG (Retrieval-Augmented Generation) để trích xuất và truy vấn thông tin từ tài liệu y tế và hành chính.

## Tính năng chính

### 1. OCR Processing
- Sử dụng PaddleOCR + VietOCR để nhận dạng văn bản tiếng Việt
- Hỗ trợ nhiều định dạng tài liệu y tế (giấy ra viện, đơn thuốc, etc.)
- Xử lý định hướng và bố cục tài liệu tự động

### 2. Document Template System
- Hệ thống template linh hoạt cho các loại tài liệu khác nhau
- Cấu hình rules để trích xuất thông tin theo pattern
- Hỗ trợ validation và transformation dữ liệu

### 3. RAG (Retrieval-Augmented Generation)
- Vector search với Sentence Transformers
- FAISS index cho tìm kiếm nhanh
- Q&A thông minh về nội dung tài liệu

### 4. Template Configuration
- Giao diện web để cấu hình template
- Quản lý rules trích xuất thông tin
- Lưu trữ và tải template

## Kiến trúc

### Java Service Layer
- **RAGOcrService**: Tích hợp với PythonWorkerClient để gọi worker Python qua gRPC
- **PythonWorkerClient**: Xử lý giao tiếp gRPC với Python workers
- **Request/Response Models**: RAGOcrRequest và RAGOcrResponse

### Python Worker Layer
- **AAA0_0202_W**: Worker chính xử lý OCR + RAG
- **DocumentProcessor**: Quản lý templates và extraction rules
- **FAISS Integration**: Vector search cho RAG

### Frontend Layer
- **React Components**: Giao diện web với 3 tabs chính
- **API Integration**: Gọi REST APIs từ Java backend

## Cấu trúc Module

### Backend (Java Spring Boot)

#### Controller: `AAA0_0202Controller.java`
- `/AA/A0/AAA0_0202/process` - Xử lý tài liệu với RAG
- `/AA/A0/AAA0_0202/query` - Truy vấn thông tin
- `/AA/A0/AAA0_0202/templates` - Quản lý template

#### Service: `RAGOcrService.java`
- Tích hợp với PythonWorkerClient để gọi AAA0_0202_W qua gRPC
- Quản lý document templates
- Xử lý RAG queries và responses

#### Models:
- `RAGOcrRequest.java` - Request model
- `RAGOcrResponse.java` - Response model

### Python Worker: `AAA0_0202_W.py`

#### OCR Components:
- PaddleOCR detection/classification
- VietOCR text recognition
- Image preprocessing

#### RAG Components:
- Sentence Transformers cho embeddings
- FAISS vector search
- Document template processing

#### Template System:
- DocumentProcessor class
- FieldRule definitions
- Extraction strategies

### Frontend (React/TypeScript): `RAGOcrPage.tsx`

#### Tabs:
1. **Process Document** - Upload và xử lý tài liệu
2. **Query Document** - Đặt câu hỏi về tài liệu
3. **Template Config** - Cấu hình templates và rules

#### Features:
- File upload với drag & drop
- Real-time processing status
- Results visualization
- Template management UI

## Sử dụng

### 1. Xử lý tài liệu

```javascript
// Upload và xử lý tài liệu
const formData = new FormData();
formData.append('file', file);
formData.append('template_id', 'discharge_summary');
formData.append('query', 'Tên bệnh nhân là gì?');

const response = await fetch('/AA/A0/AAA0_0202/process', {
    method: 'POST',
    body: formData
});
```

### 2. Truy vấn thông tin

```javascript
// Query thông tin từ tài liệu đã xử lý
const response = await fetch('/AA/A0/AAA0_0202/query', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
        query: 'Bao nhiêu tuổi?',
        template_id: 'discharge_summary'
    })
});
```

### 3. Quản lý Templates

```javascript
// Lấy danh sách templates
const templates = await fetch('/AA/A0/AAA0_0202/templates');

// Lưu template mới
await fetch('/AA/A0/AAA0_0202/templates', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(templateConfig)
});
```

## Document Templates

### Cấu trúc Template

```json
{
  "template_id": "discharge_summary",
  "template_name": "Giấy ra viện",
  "description": "Template cho giấy tóm tắt ra viện",
  "rules": [
    {
      "field_name": "họ_tên",
      "display_name": "Họ và tên",
      "keywords": ["họ tên", "tên bệnh nhân"],
      "strategy": "KEYWORD_VALUE",
      "priority": 10,
      "query_aliases": ["tên", "bệnh nhân"]
    }
  ],
  "classifier_keywords": ["ra viện", "giấy ra viện"]
}
```

### Extraction Strategies

1. **REGEX** - Trích xuất bằng regular expressions
2. **KEYWORD_VALUE** - Pattern "Keyword: Value"
3. **NEXT_LINE** - Giá trị ở dòng tiếp theo
4. **MULTI_LINE** - Giá trị trải dài nhiều dòng
5. **CUSTOM** - Logic tùy chỉnh

## Dependencies

### Python Requirements
```
sentence-transformers
faiss-cpu
transformers
torch
Pillow
opencv-python
VietOCR
```

### Java Dependencies
- Spring Boot Web
- Jackson (JSON processing)
- Swagger/OpenAPI

## Test

Chạy test script:

```bash
cd /root/deepapp/deepapp_main/src/main/python
PYTHONPATH=/root/deepapp/deepapp_main/src/main/python python3 test/test_AAA0_0202.py
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/AA/A0/AAA0_0202` | Health check |
| POST | `/AA/A0/AAA0_0202/process` | Process document with RAG |
| POST | `/AA/A0/AAA0_0202/query` | Query document information |
| GET | `/AA/A0/AAA0_0202/templates` | List templates |
| POST | `/AA/A0/AAA0_0202/templates` | Save template |
| DELETE | `/AA/A0/AAA0_0202/templates/{id}` | Delete template |

## Response Format

### Success Response
```json
{
  "success": true,
  "ocr_text": ["Extracted text lines..."],
  "extracted_fields": {
    "patient_name": "Nguyễn Văn A",
    "diagnosis": "Viêm phổi"
  },
  "answer": "Nguyễn Văn A",
  "confidence": "HIGH",
  "rag_results": [...],
  "processing_time_ms": 1500
}
```

### Error Response
```json
{
  "success": false,
  "error": "Error description"
}
```

## Future Enhancements

- [ ] Support for more document types
- [ ] Advanced NER with PhoBERT
- [ ] Multi-language support
- [ ] Batch processing
- [ ] Template auto-learning
- [ ] Integration with external LLMs