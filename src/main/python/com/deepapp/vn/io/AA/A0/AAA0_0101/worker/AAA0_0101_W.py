"""
VietOCR Worker: AAA0_0101_W
Handles Vietnamese OCR processing
"""

import json
import base64
from typing import Dict, Any
from com.deepapp.infrastructure.BaseWorker import BaseWorker
from com.deepapp.infrastructure.WorkerRegistry import register_worker


@register_worker("AAA0_0101_W")
class AAA0_0101_Worker(BaseWorker):
    """
    VietOCR Worker: AAA0_0101_W
    Handles Vietnamese OCR processing with VietOCR and PaddleOCR
    """

    def __init__(self):
        super().__init__("AAA0_0101_Worker")
        self.log("VietOCR Worker Initialized")
        self.vietocr_available = self._check_vietocr()
        self.paddleocr_available = self._check_paddleocr()

    def process_task(self, event_type: str, payload: str) -> str:
        """Process OCR tasks"""
        self.log(f"Processing OCR task: {event_type}")

        try:
            if event_type == "vietocr":
                return self._process_vietocr(payload)
            elif event_type == "paddleocr":
                return self._process_paddleocr(payload)
            elif event_type == "health_check":
                return self._health_check()
            else:
                return self.create_response("unknown_event",
                    f"Event type '{event_type}' not supported")
        except Exception as e:
            self.log(f"Error processing OCR task: {e}", "ERROR")
            return self.create_response("error", str(e))

    def can_handle(self, event_type: str) -> bool:
        """Check supported event types"""
        return event_type in ["vietocr", "paddleocr", "health_check"]

    def _check_vietocr(self) -> bool:
        """Check if VietOCR is available"""
        try:
            import VietOCR
            return True
        except ImportError:
            self.log("VietOCR not available", "WARNING")
            return False

    def _check_paddleocr(self) -> bool:
        """Check if PaddleOCR is available"""
        try:
            import paddleocr
            return True
        except ImportError:
            self.log("PaddleOCR not available", "WARNING")
            return False

    def _process_vietocr(self, payload: str) -> str:
        """Process image with VietOCR"""
        if not self.vietocr_available:
            return self.create_response("error", "VietOCR not available")

        try:
            data = json.loads(payload)
            image_data = data.get("image", "")
            lang = data.get("lang", "vi")

            # Decode base64 image
            image_bytes = base64.b64decode(image_data)

            # Mock VietOCR processing (replace with actual implementation)
            result = {
                "text": f"MOCK_VIETOCR_RESULT_{lang}",
                "confidence": 0.95,
                "language": lang,
                "processing_time": 1.2
            }

            self.log(f"VietOCR processed image, result: {result['text']}")
            return self.create_response("success", result)

        except Exception as e:
            return self.create_response("error", f"VietOCR processing failed: {str(e)}")

    def _process_paddleocr(self, payload: str) -> str:
        """Process image with PaddleOCR"""
        if not self.paddleocr_available:
            return self.create_response("error", "PaddleOCR not available")

        try:
            data = json.loads(payload)
            image_data = data.get("image", "")
            lang = data.get("lang", "vi")

            # Decode base64 image
            image_bytes = base64.b64decode(image_data)

            # Mock PaddleOCR processing (replace with actual implementation)
            result = {
                "text": f"MOCK_PADDLEOCR_RESULT_{lang}",
                "confidence": 0.92,
                "language": lang,
                "processing_time": 0.8
            }

            self.log(f"PaddleOCR processed image, result: {result['text']}")
            return self.create_response("success", result)

        except Exception as e:
            return self.create_response("error", f"PaddleOCR processing failed: {str(e)}")

    def _health_check(self) -> str:
        """Health check for OCR services"""
        health_status = {
            "vietocr_available": self.vietocr_available,
            "paddleocr_available": self.paddleocr_available,
            "status": "healthy" if (self.vietocr_available or self.paddleocr_available) else "degraded"
        }
        return self.create_response("health_check", health_status)