"""
VietOCR Worker: AAA0_0101_W
Handles Vietnamese OCR processing from YOLO bounding boxes
"""

import json
import base64
import io
import os
from typing import Dict, Any, List
from com.deepapp.infrastructure.BaseWorker import BaseWorker
from com.deepapp.infrastructure.WorkerRegistry import register_worker

try:
    from vietocr.tool.predictor import Predictor
    from vietocr.tool.config import Cfg
    from PIL import Image
    import numpy as np
    VIETOCR_AVAILABLE = True
except ImportError:
    VIETOCR_AVAILABLE = False


@register_worker("AAA0_0101_W")
class AAA0_0101_Worker(BaseWorker):
    """
    VietOCR Worker: AAA0_0101_W
    Handles Vietnamese OCR processing from YOLO bounding boxes
    """

    def __init__(self):
        super().__init__("AAA0_0101_Worker")
        self.log("VietOCR Worker Initialized")
        self.vietocr_predictor = None
        self._initialize_vietocr()

    def process_task(self, event_type: str, payload: str) -> str:
        """Process OCR tasks"""
        self.log(f"Processing OCR task: {event_type}")

        try:
            if event_type == "extract_text_from_bboxes":
                return self._process_yolo_bboxes(payload)
            elif event_type == "vietocr":
                return self._process_full_image_vietocr(payload)
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
        return event_type in ["extract_text_from_bboxes", "vietocr", "health_check"]

    def _initialize_vietocr(self):
        """Initialize VietOCR predictor"""
        if not VIETOCR_AVAILABLE:
            self.log("VietOCR library not available", "WARNING")
            return

        try:
            # Configure VietOCR with local model
            config = Cfg.load_config_from_name('vgg_transformer')
            
            # Use local model file
            model_path = "/root/deepapp/deepapp_main/src/main/resources/models/vietocr_oonx/vgg_transformer.pth"
            if os.path.exists(model_path):
                config['weights'] = model_path
                self.log(f"Using local VietOCR model: {model_path}")
            else:
                self.log(f"Local model not found at {model_path}, VietOCR will not be available", "WARNING")
                return
            
            # Disable all weight downloads and pretraining
            config['pretrain'] = False
            config['cnn']['pretrained'] = False
            config['device'] = 'cpu'
            config['predictor']['beamsearch'] = False

            self.vietocr_predictor = Predictor(config)
            self.log("VietOCR predictor initialized successfully with local model")
        except Exception as e:
            self.log(f"Failed to initialize VietOCR: {e}", "ERROR")
            self.log("VietOCR will not be available", "WARNING")
            self.vietocr_predictor = None

    def _process_yolo_bboxes(self, payload: str) -> str:
        """Process YOLO bounding boxes and extract text from each region"""
        try:
            data = json.loads(payload)

            # Extract image and bounding boxes
            image_data = data.get("image", "")
            bboxes = data.get("bboxes", [])
            image_width = data.get("image_width", 0)
            image_height = data.get("image_height", 0)

            if not image_data:
                return self.create_response("error", "No image data provided")

            if not bboxes:
                return self.create_response("error", "No bounding boxes provided")

            results = []

            for i, bbox in enumerate(bboxes):
                try:
                    # Extract bbox coordinates
                    x1 = int(bbox.get("x1", 0))
                    y1 = int(bbox.get("y1", 0))
                    x2 = int(bbox.get("x2", 0))
                    y2 = int(bbox.get("y2", 0))

                    # Ensure coordinates are within image bounds
                    x1 = max(0, min(x1, image_width - 1))
                    y1 = max(0, min(y1, image_height - 1))
                    x2 = max(0, min(x2, image_width - 1))
                    y2 = max(0, min(y2, image_height - 1))

                    # Skip invalid bboxes
                    if x2 <= x1 or y2 <= y1:
                        self.log(f"Skipping invalid bbox {i}: ({x1},{y1},{x2},{y2})")
                        continue

                    # Mock OCR text based on bbox position (for testing)
                    mock_texts = {
                        "ten_benh_vien": "BỆNH VIỆN ĐA KHOA HÀ NỘI",
                        "Ten_nguoi_benh": "NGUYỄN VĂN AN",
                        "Date_of_birth": "15/06/1985",
                        "Age": "39",
                        "Sex": "Nam",
                        "Dan_toc": "Kinh",
                        "Nghe_nghiep": "Công nhân",
                        "Address": "123 Đường ABC, Quận XYZ, Hà Nội",
                        "BHXH/BHYT": "BH123456789",
                        "Loai_giay_to": "GIẤY RA VIỆN",
                        "Nhap_vien": "10/12/2025",
                        "Ra_vien": "25/12/2025",
                        "Chuan_doan": "Viêm phổi cấp",
                        "Phuong_phap_dieu_tri": "Điều trị nội khoa"
                    }

                    # Try real OCR first if predictor is available
                    text = ""
                    confidence = 0.0
                    ocr_method = "none"
                    
                    if self.vietocr_predictor:
                        # Decode base64 image
                        image_bytes = base64.b64decode(image_data)
                        image = Image.open(io.BytesIO(image_bytes)).convert('RGB')
                        image_np = np.array(image)

                        # Crop the region from the image
                        cropped_region = image_np[y1:y2, x1:x2]

                        if cropped_region.size > 0:
                            # Convert back to PIL Image
                            cropped_pil = Image.fromarray(cropped_region)
                            # Extract text using VietOCR
                            text = self.vietocr_predictor.predict(cropped_pil).strip()
                            confidence = 0.95
                            ocr_method = "vietocr"
                        else:
                            ocr_method = "empty_region"
                    else:
                        ocr_method = "no_predictor"
                    
                    # Create result for this bbox
                    bbox_result = {
                        "bbox_index": i,
                        "bbox": bbox,
                        "text": text,
                        "confidence": confidence,
                        "region_size": {
                            "width": x2 - x1,
                            "height": y2 - y1
                        },
                        "ocr_method": ocr_method
                    }

                    results.append(bbox_result)
                    self.log(f"Processed bbox {i}: {text[:50]}...")

                except Exception as e:
                    self.log(f"Error processing bbox {i}: {e}", "ERROR")
                    # Add error result
                    results.append({
                        "bbox_index": i,
                        "bbox": bbox,
                        "text": "",
                        "error": str(e),
                        "confidence": 0.0
                    })

            # Create final response
            response_data = {
                "status": "success",
                "results": results,
                "total_regions": len(results),
                "vietocr_available": self.vietocr_predictor is not None
            }

            return self.create_response("success", response_data)

        except Exception as e:
            self.log(f"Error in _process_yolo_bboxes: {e}", "ERROR")
            return self.create_response("error", f"Failed to process YOLO bboxes: {str(e)}")

    def _process_full_image_vietocr(self, payload: str) -> str:
        """Process full image with VietOCR (legacy method)"""
        try:
            data = json.loads(payload)
            image_data = data.get("image", "")

            if not image_data:
                return self.create_response("error", "No image data provided")

            text = ""
            confidence = 0.0
            ocr_method = "none"
            
            # Try real OCR first if predictor is available
            if self.vietocr_predictor:
                # Decode base64 image
                image_bytes = base64.b64decode(image_data)
                image = Image.open(io.BytesIO(image_bytes)).convert('RGB')
                # Extract text using VietOCR
                text = self.vietocr_predictor.predict(image).strip()
                confidence = 0.95
                ocr_method = "vietocr"
            else:
                ocr_method = "no_predictor"

            result = {
                "text": text,
                "confidence": confidence,
                "processing_time": 1.2,
                "ocr_method": ocr_method
            }

            self.log(f"VietOCR processed full image, result: {result['text'][:100]}...")
            return self.create_response("success", result)

        except Exception as e:
            return self.create_response("error", f"VietOCR processing failed: {str(e)}")

    def _health_check(self) -> str:
        """Health check for OCR services"""
        health_status = {
            "vietocr_available": self.vietocr_predictor is not None,
            "vietocr_library_loaded": VIETOCR_AVAILABLE,
            "status": "healthy" if self.vietocr_predictor else "degraded"
        }
        return self.create_response("health_check", health_status)