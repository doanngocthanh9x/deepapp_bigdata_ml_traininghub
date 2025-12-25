## ✅ FIXED: document-stream-test.html - Stream và hình ảnh không hiển thị

**Vấn đề:**
- document-stream-test.html không hiển thị stream SSE và hình ảnh
- Load rất lâu, không có response

**Nguyên nhân:**
- `DocumentUploadService.java` sử dụng mock data thay vì gọi C++ worker thực
- Method `getDocumentMetadata()` return pageCount = 10 (mock) thay vì lấy từ C++ worker
- Method `processDocumentPages()` chỉ tạo mock page data KHÔNG có `imageData` (base64), chỉ có `imagePath`
- Frontend cần `imageData` (base64) để hiển thị hình ảnh

**Giải pháp đã áp dụng:**
1. Inject `DocumentProcessingService` vào `DocumentUploadService`
2. Sửa `getDocumentMetadata()` để gọi `documentProcessingService.getDocumentInfo()` - lấy metadata thực từ C++ worker
3. Sửa `processDocumentPages()` để gọi `documentProcessingService.getSpecificPage()` cho từng trang - lấy imageData (base64) thực từ C++ worker
4. Frontend giờ sẽ nhận được SSE events với imageData thực để hiển thị

**Files đã sửa:**
- `src/main/java/com/deepapp/vn/io/storage/service/DocumentUploadService.java`

## ✅ FIXED: document-management.html - Không ghi nhận file upload

**Vấn đề:**
- document-management.html không hiển thị file được upload từ document-stream-test.html

**Nguyên nhân:**
- Cùng nguyên nhân với vấn đề trên - mock data không lưu vào database đúng cách
- Metadata không chính xác (pageCount = 10 mock thay vì giá trị thực)

**Giải pháp:**
- Đã được fix bởi các thay đổi trên
- Giờ hệ thống sẽ:
  1. Upload file và lưu vào storage
  2. Lấy metadata thực từ C++ worker (pageCount, format, dpi)
  3. Lưu document record vào database với metadata chính xác
  4. Process từng trang và lưu pageData với imageData vào database
  5. document-management.html sẽ query database và hiển thị đúng

**Luồng xử lý đã được cải thiện:**
```
User Upload File (document-stream-test.html)
  ↓
Java Controller: /ZZ/A0/ZZA0_0100/stream
  ↓
DocumentUploadService.processDocumentUpload()
  ↓ validate & save file
  ↓ get metadata via DocumentProcessingService.getDocumentInfo()
  ↓   → C++ Worker gRPC call → return real metadata
  ↓ save document record to DB
  ↓ process pages via DocumentProcessingService.getSpecificPage()
  ↓   → C++ Worker gRPC call for each page → return imageData (base64)
  ↓ send SSE events to frontend with real data
  ↓ save page records to DB
  ↓
document-management.html queries DB and displays documents
```

---

## ✅ FIXED: JSON Nesting Depth Error - Circular Reference

**Vấn đề:**
```
Could not write JSON: Document nesting depth (501) exceeds the maximum allowed (500,
from `StreamWriteConstraints.getMaxNestingDepth()`)
```
- Xảy ra khi gọi `/api/documents` (getAllDocuments)
- Response committed already - không thể trả về error response

**Nguyên nhân:**
- **Circular Reference** trong JPA entities:
  - `DocumentEntity` → `pages` (OneToMany) → `PageEntity` → `document` (ManyToOne) → loop vô hạn
  - `DocumentEntity` → `task` (OneToOne) → `TaskEntity` → `document` (OneToOne) → loop vô hạn
- Jackson serializer cố serialize Document → Pages → Document → Pages... cho đến khi đạt max depth (500)

**Giải pháp đã áp dụng:**
Sử dụng Jackson annotations để break circular reference:
1. **@JsonManagedReference** trên parent side (DocumentEntity)
2. **@JsonBackReference** trên child side (PageEntity, TaskEntity)

**Files đã sửa:**
- `DocumentEntity.java`:
  - Added `@JsonManagedReference("document-pages")` on `pages` field
  - Added `@JsonManagedReference("document-task")` on `task` field

- `PageEntity.java`:
  - Added `@JsonBackReference("document-pages")` on `document` field

- `TaskEntity.java`:
  - Added `@JsonBackReference("document-task")` on `document` field

**Kết quả:**
- Jackson sẽ serialize Document và Pages/Task KHÔNG serialize document field trong Pages/Task
- Break circular reference
- Response JSON có cấu trúc đúng, không bị infinite loop
- document-management.html giờ có thể load danh sách documents thành công

**Cách hoạt động:**
```json
{
  "id": 1,
  "requestId": "abc-123",
  "filename": "test.pdf",
  "pages": [
    {
      "id": 1,
      "pageNumber": 1,
      // "document" field bị bỏ qua bởi @JsonBackReference
    },
    {
      "id": 2,
      "pageNumber": 2,
      // "document" field bị bỏ qua bởi @JsonBackReference
    }
  ],
  "task": {
    "id": 1,
    "status": "completed"
    // "document" field bị bỏ qua bởi @JsonBackReference
  }
}
```

---

## ✅ FIXED: Pages không hiển thị trong document-management.html

**Vấn đề:**
- Click "Xem" trong document-management.html
- Modal hiển thị "Không có trang nào"
- Pages không được lưu vào database hoặc không có imageData

**Nguyên nhân:**
- `DocumentUploadService.processDocumentPages()` lưu pageData với imagePath
- Nhưng C++ worker trả về imageData (base64) qua gRPC, **KHÔNG lưu file vào disk**
- imagePath trỏ đến file không tồn tại
- `PageEntity.getImageDataBase64()` đọc file từ imagePath → trả về null
- Frontend nhận pageData KHÔNG có imageData → không hiển thị được

**Giải pháp đã áp dụng:**
1. Thêm field `imageData` (MEDIUMTEXT) vào `PageEntity` để lưu base64 trực tiếp vào DB
2. Thêm getter/setter cho `imageData`
3. Sửa `getImageDataBase64()` để ưu tiên trả về imageData từ DB, fallback về đọc file
4. Sửa `DocumentUploadService.savePageData()` để lưu imageData từ C++ worker response

**Files đã sửa:**
- `PageEntity.java` (src/main/java/com/deepapp/vn/io/storage/entity/PageEntity.java:46-47,94-95,110-131)
  - Added `imageData` field (MEDIUMTEXT column)
  - Added getter/setter for imageData
  - Updated getImageDataBase64() to prioritize imageData from DB

- `DocumentUploadService.java` (src/main/java/com/deepapp/vn/io/storage/service/DocumentUploadService.java:213)
  - Added `page.setImageData()` to save imageData from C++ worker response

**Database Schema:**
- Hibernate auto-update enabled (application.yml: `ddl-auto: update`)
- Column `image_data` sẽ được tự động thêm vào bảng `pages` khi restart

**Luồng xử lý:**
```
DocumentUploadService.processDocumentPages()
  ↓ loop each page
  ↓ call DocumentProcessingService.getSpecificPage()
  ↓   → C++ Worker gRPC → return pageData with imageData (base64)
  ↓ savePageData(requestId, pageNumber, pageData)
  ↓   → PageEntity.setImageData(pageData.get("imageData"))
  ↓   → Save to DB with imageData
  ↓
Frontend: /api/documents/{requestId}/pages
  ↓ DocumentManagementService.getPages()
  ↓   → PageDTO.fromEntity(pageEntity)
  ↓     → pageEntity.getImageDataBase64()
  ↓       → return imageData from DB
  ↓   → Return List<PageDTO> with imageData
  ↓
Frontend displays pages with images
```

**Kết quả:**
- Pages được lưu vào DB với imageData (base64)
- API `/api/documents/{requestId}/pages` trả về pages với imageData
- document-management.html hiển thị pages và hình ảnh đúng

---

## ✅ FIXED: PageCount = -1, C++ Worker báo "no file data provided"

**Vấn đề:**
```
C++ Worker log: "Page count unavailable - no file data provided"
Result: pageCount: -1
DocumentUploadService: Processing -1 pages → không process page nào
```

**Nguyên nhân:**
- `processDocumentUpload()` gọi `file.getBytes()` nhiều lần:
  - Line 97 (saveFile): `Files.write(filePath, file.getBytes())`
  - Line 250 (getMetadata): `getDocumentMetadata(file.getBytes(), ...)`
  - Line 270 (processPages): `processDocumentPages(..., file.getBytes(), ...)`
- **MultipartFile có thể bị issue khi đọc nhiều lần** (input stream consumed)
- Lần đọc thứ 2 và 3 trả về empty array
- C++ worker nhận request với `data: ""` (empty base64) → return pageCount = -1

**Giải pháp đã áp dụng:**
**Đọc file từ disk sau khi đã save** thay vì đọc lại từ MultipartFile:

1. Save file to disk (line 236): `saveFile(file, requestId)` → lưu vào `/tmp/deepapp/uploads/{requestId}/document.{ext}`

2. Read from disk (line 250):
```java
byte[] fileData = Files.readAllBytes(Paths.get(fileStorage.getFilePath()));
Map<String, Object> metadata = getDocumentMetadata(fileData, ...);
```

3. Reuse fileData (line 271):
```java
processDocumentPages(requestId, fileData, fileStorage, emitter, pageCount);
```

**Files đã sửa:**
- `DocumentUploadService.java` (src/main/java/com/deepapp/vn/io/storage/service/DocumentUploadService.java:250-251,271)
  - Read file from disk after saving
  - Reuse byte array for both metadata and page processing

**Kết quả:**
- ✅ C++ worker nhận đầy đủ file data
- ✅ Return pageCount chính xác (không còn -1)
- ✅ Loop xử lý pages chạy đúng số lần
- ✅ Pages được process và lưu vào DB với imageData
- ✅ Frontend hiển thị pages đầy đủ