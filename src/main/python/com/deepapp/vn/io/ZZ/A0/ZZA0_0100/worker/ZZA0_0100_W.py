"""
Document Processing Worker: ZZA0_0100_W
Handles PDF/TIFF document processing and text extraction
"""

import json
import base64
import os
from typing import Dict, Any, List
from com.deepapp.infrastructure.BaseWorker import BaseWorker
from com.deepapp.infrastructure.WorkerRegistry import register_worker


@register_worker("ZZA0_0100_W")
class ZZA0_0100_Worker(BaseWorker):
    """
    Document Processing Worker: ZZA0_0100_W
    Handles PDF/TIFF document processing, OCR, and text extraction
    """

    def __init__(self):
        super().__init__("ZZA0_0100_Worker")
        self.log("Document Processing Worker Initialized")
        self.supported_formats = ["TIFF", "TIF", "PDF"]
        self.log(f"Supported formats: {', '.join(self.supported_formats)}")

        # Initialize storage
        self._init_storage()

    def process_task(self, event_type: str, payload: str) -> str:
        """Process document processing tasks"""
        self.log(f"Processing document task: {event_type}")

        try:
            if event_type == "process_document":
                return self._process_document(payload)
            elif event_type == "extract_pages":
                return self._extract_pages(payload)
            elif event_type == "extract_text":
                return self._extract_text(payload)
            elif event_type == "get_metadata":
                return self._get_metadata(payload)
            else:
                return self.create_response("unknown_event",
                    f"Event type '{event_type}' not supported")
        except Exception as e:
            self.log(f"Error processing document task: {e}", "ERROR")
            return self.create_response("error", str(e))

    def can_handle(self, event_type: str) -> bool:
        """Check supported event types"""
        return event_type in ["process_document", "extract_pages", "extract_text", "get_metadata"]

    def _init_storage(self) -> None:
        """Initialize storage directories"""
        try:
            os.makedirs("/tmp/deepapp/uploads", exist_ok=True)
            os.makedirs("/tmp/deepapp/outputs", exist_ok=True)
            self.log("Storage directories initialized")
        except Exception as e:
            self.log(f"Failed to initialize storage: {e}", "ERROR")

    def _process_document(self, payload: str) -> str:
        """Process a complete document"""
        try:
            data = json.loads(payload)
            document_id = data.get("document_id", "")
            file_data = data.get("file_data", "")
            filename = data.get("filename", "document.pdf")

            # Determine file format
            file_format = self._get_file_format(filename)
            if file_format not in self.supported_formats:
                return self.create_response("error",
                    f"Unsupported format: {file_format}. Supported: {', '.join(self.supported_formats)}")

            # Save file temporarily
            file_path = f"/tmp/deepapp/uploads/{document_id}_{filename}"
            with open(file_path, "wb") as f:
                f.write(base64.b64decode(file_data))

            # Mock processing result
            result = {
                "document_id": document_id,
                "filename": filename,
                "format": file_format,
                "page_count": 5,  # Mock
                "total_size": len(file_data),
                "processing_status": "completed",
                "pages": [
                    {
                        "page_number": i + 1,
                        "width": 2481,
                        "height": 3508,
                        "text": f"Mock extracted text for page {i + 1}",
                        "image_path": f"/tmp/deepapp/outputs/{document_id}_page_{i + 1}.png"
                    } for i in range(5)
                ]
            }

            self.log(f"Document processed: {filename} ({result['page_count']} pages)")
            return self.create_response("success", result)

        except Exception as e:
            return self.create_response("error", f"Document processing failed: {str(e)}")

    def _extract_pages(self, payload: str) -> str:
        """Extract individual pages from document"""
        try:
            data = json.loads(payload)
            document_id = data.get("document_id", "")
            page_numbers = data.get("page_numbers", [])

            pages = []
            for page_num in page_numbers:
                page_data = {
                    "page_number": page_num,
                    "width": 2481,
                    "height": 3508,
                    "dpi": 150,
                    "format": "PNG",
                    "image_path": f"/tmp/deepapp/outputs/{document_id}_page_{page_num}.png",
                    "text": f"Mock OCR text for page {page_num}",
                    "status": "completed"
                }
                pages.append(page_data)

            result = {
                "document_id": document_id,
                "extracted_pages": len(pages),
                "pages": pages
            }

            self.log(f"Extracted {len(pages)} pages for document {document_id}")
            return self.create_response("success", result)

        except Exception as e:
            return self.create_response("error", f"Page extraction failed: {str(e)}")

    def _extract_text(self, payload: str) -> str:
        """Extract text from document"""
        try:
            data = json.loads(payload)
            document_id = data.get("document_id", "")
            page_number = data.get("page_number", 1)

            # Mock text extraction
            mock_texts = {
                1: "BỆNH VIỆN ĐA KHOA HÀ NỘI\nHọ và tên: Nguyễn Văn A\nChẩn đoán: Viêm phổi",
                2: "KẾ HOẠCH ĐIỀU TRỊ\nKháng sinh: Amoxicillin 500mg\nThời gian: 7 ngày",
                3: "LƯU Ý\nUống thuốc đúng giờ\nTái khám sau 1 tuần"
            }

            text = mock_texts.get(page_number, f"Mock text for page {page_number}")

            result = {
                "document_id": document_id,
                "page_number": page_number,
                "text": text,
                "confidence": 0.95,
                "language": "vi"
            }

            self.log(f"Extracted text from page {page_number}")
            return self.create_response("success", result)

        except Exception as e:
            return self.create_response("error", f"Text extraction failed: {str(e)}")

    def _get_metadata(self, payload: str) -> str:
        """Get document metadata"""
        try:
            data = json.loads(payload)
            document_id = data.get("document_id", "")

            metadata = {
                "document_id": document_id,
                "format": "PDF",
                "page_count": 5,
                "title": "Giấy ra viện mẫu",
                "author": "Bệnh viện Đa khoa Hà Nội",
                "created_date": "2025-01-01",
                "file_size": 123456,
                "dimensions": {"width": 2481, "height": 3508}
            }

            self.log(f"Retrieved metadata for document {document_id}")
            return self.create_response("success", metadata)

        except Exception as e:
            return self.create_response("error", f"Metadata retrieval failed: {str(e)}")

    def _get_file_format(self, filename: str) -> str:
        """Determine file format from filename"""
        ext = filename.lower().split('.')[-1]
        format_map = {
            'pdf': 'PDF',
            'tiff': 'TIFF',
            'tif': 'TIFF'
        }
        return format_map.get(ext, ext.upper())