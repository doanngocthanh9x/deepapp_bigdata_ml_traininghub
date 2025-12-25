# Document Processing API - Optimized for Fast Response

## Workflow tối ưu (Fast & Phân trang)

### 1. Upload file - Trả về NHANH chỉ metadata
```bash
curl -X POST http://localhost:8080/ZZ/A0/ZZA0_0100 \
  -F "file=@document.pdf"
```

**Response (NHANH - chỉ vài KB):**
```json
{
  "success": true,
  "filename": "document.pdf",
  "fileSize": 71561,
  "pageCount": 3,
  "format": "pdf",
  "message": "Use GET /ZZ/A0/ZZA0_0100/page?pageNumber=N to retrieve individual pages"
}
```

### 2. Lấy từng trang khi cần (On-demand)
```bash
# Lấy trang 1
curl -X POST http://localhost:8080/ZZ/A0/ZZA0_0100/page \
  -F "file=@document.pdf" \
  -F "pageNumber=1"
```

**Response (NHANH - chỉ 1 trang):**
```json
{
  "success": true,
  "filename": "document.pdf",
  "pageNumber": 1,
  "pageData": "base64_encoded_page_1_image",
  "width": 595,
  "height": 842
}
```

## So sánh

### Trước (Chậm):
- Upload → Xử lý tất cả trang → Trả về ALL data (có thể vài MB)
- Thời gian: 5-10 giây
- Bandwidth: Cao

### Sau (Nhanh):
- Upload → Trả về metadata ngay (vài KB)  
- Thời gian: < 1 giây
- Lấy trang khi cần → Từng trang riêng
- Bandwidth: Thấp

## Cấu hình tăng file size limit

Đã tăng max upload size lên 50MB trong `application.properties`:
```properties
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB
```

## API Endpoints

- `POST /ZZ/A0/ZZA0_0100` - Upload file, trả về metadata (FAST)
- `POST /ZZ/A0/ZZA0_0100/page` - Lấy 1 trang cụ thể (FAST)
- `POST /ZZ/A0/ZZA0_0100/process` - Xử lý full (SLOW - trả tất cả trang)
- `GET /ZZ/A0/ZZA0_0100` - Health check
