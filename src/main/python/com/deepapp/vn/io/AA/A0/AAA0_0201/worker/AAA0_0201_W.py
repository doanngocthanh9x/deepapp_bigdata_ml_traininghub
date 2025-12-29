"""
PaddleOCR Text Recognition Worker: AAA0_0201_W
Handles text recognition using PaddleOCR ONNX models
"""

import json
import os
import base64
import numpy as np
import cv2
from typing import Dict, Any, List, Tuple
from com.deepapp.infrastructure.BaseWorker import BaseWorker
from com.deepapp.infrastructure.WorkerRegistry import register_worker
from com.deepapp.utils.path_utils import get_paddle_model_path
from nets.nn import Recognition, Detection, Classification
from utils.util import crop_image, sort_polygon


@register_worker("AAA0_0201_W")
class AAA0_0201_Worker(BaseWorker):
    """
    PaddleOCR Text Recognition Worker: AAA0_0201_W
    Handles text recognition tasks using PaddleOCR ONNX models
    """

    def __init__(self):
        super().__init__("AAA0_0201_Worker")
        self.log("PaddleOCR Text Recognition Worker Initialized")
        self.detection = self._load_detection_model()
        self.classification = self._load_classification_model()
        self.recognition = self._load_recognition_model()

    def process_task(self, event_type: str, payload: str) -> str:
        """Process text recognition tasks"""
        self.log(f"Processing PaddleOCR task: {event_type}")

        try:
            if event_type == "recognize":
                return self._recognize_text(payload)
            elif event_type == "recognize_paddle":
                return self._recognize_paddle(payload)
            elif event_type == "list_models":
                return self._list_models()
            else:
                return self.create_response("unknown_event",
                    f"Event type '{event_type}' not supported")
        except Exception as e:
            self.log(f"Error processing PaddleOCR task: {e}", "ERROR")
            return self.create_response("error", str(e))

    def can_handle(self, event_type: str) -> bool:
        """Check supported event types"""
        return event_type in ["recognize", "recognize_paddle", "list_models"]

    def _load_detection_model(self) -> Detection:
        """Load PaddleOCR detection model"""
        detection_path = get_paddle_model_path('detection')

        if os.path.exists(detection_path):
            try:
                detection = Detection(detection_path)
                self.log(f"Loaded PaddleOCR detection model: {detection_path}")
                return detection
            except Exception as e:
                self.log(f"Failed to load PaddleOCR detection model: {e}", "ERROR")
                return None
        else:
            self.log(f"PaddleOCR detection model not found at {detection_path}", "WARNING")
            return None

    def _load_classification_model(self) -> Classification:
        """Load PaddleOCR classification model"""
        classification_path = get_paddle_model_path('classification')

        if os.path.exists(classification_path):
            try:
                classification = Classification(classification_path)
                self.log(f"Loaded PaddleOCR classification model: {classification_path}")
                return classification
            except Exception as e:
                self.log(f"Failed to load PaddleOCR classification model: {e}", "ERROR")
                return None
        else:
            self.log(f"PaddleOCR classification model not found at {classification_path}", "WARNING")
            return None

    def _load_recognition_model(self) -> Recognition:
        """Load PaddleOCR recognition model"""
        # Check for recognition model
        project_root = os.environ.get('DEEPAPP_PROJECT_ROOT')
        if project_root:
            recognition_path = os.path.join(project_root, "src", "main", "resources", "models", "paddlet", "recognition.onnx")
        else:
            # Fallback to hardcoded path
            recognition_path = "/root/deepapp/deepapp_main/src/main/resources/models/paddlet/recognition.onnx"

        if os.path.exists(recognition_path):
            try:
                # Use Recognition class from PaddleOCR-onnx
                recognition = Recognition(recognition_path)
                self.log(f"Loaded PaddleOCR recognition model: {recognition_path}")
                return recognition
            except Exception as e:
                self.log(f"Failed to load PaddleOCR recognition model: {e}", "ERROR")
                return None
        else:
            self.log(f"PaddleOCR recognition model not found at {recognition_path}", "WARNING")
            return None

    def _preprocess_image(self, image_data: str) -> Tuple[np.ndarray, Tuple[int, int]]:
        """Preprocess image for PaddleOCR - convert to RGB format for detection"""
        try:
            # Decode base64 image
            if image_data.startswith('data:image'):
                # Handle data URL format
                header, image_data = image_data.split(',', 1)

            image_bytes = base64.b64decode(image_data)
            image_array = np.frombuffer(image_bytes, dtype=np.uint8)
            image = cv2.imdecode(image_array, cv2.IMREAD_COLOR)

            if image is None:
                raise ValueError("Failed to decode image")

            # Store original size
            original_size = (image.shape[1], image.shape[0])  # (width, height)

            # Convert BGR to RGB for PaddleOCR pipeline (matches main.py)
            image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)

            # Return image in HWC format (Height, Width, Channels)
            # Don't resize here - detection needs full image
            return image, original_size

        except Exception as e:
            raise ValueError(f"Image preprocessing failed: {str(e)}")


    def _recognize_text(self, payload: str) -> str:
        """Recognize text in image using full PaddleOCR pipeline (Detection + Classification + Recognition)"""
        try:
            data = json.loads(payload)
            image_data = data.get("image", "")

            if not image_data:
                return self.create_response("error", "No image data provided")

            if self.detection is None or self.classification is None or self.recognition is None:
                return self.create_response("error", "One or more PaddleOCR models not loaded")

            # Preprocess image - decode and convert to RGB
            image, original_size = self._preprocess_image(image_data)

            # Full OCR pipeline like main.py
            frame_rgb = image.copy()

            # Step 1: Detection - find text regions
            dt_boxes = self.detection(frame_rgb)
            self.log(f"Detected {len(dt_boxes)} text regions")

            if len(dt_boxes) == 0:
                # No text detected, return empty result
                result = {
                    "text": "",
                    "confidence": 0.0,
                    "original_size": original_size,
                    "text_regions": []
                }
                return self.create_response("success", result)

            # Step 2: Crop text regions from original image
            img_list = []
            for box in dt_boxes:
                # Convert box to polygon format expected by crop_image
                points = box.astype(np.int32).tolist()
                # Ensure we have 4 points
                if len(points) == 4:
                    try:
                        cropped_img = crop_image(frame_rgb, np.array(points))
                        img_list.append(cropped_img)
                    except Exception as e:
                        self.log(f"Failed to crop image for box {box}: {e}")
                        continue

            self.log(f"Cropped {len(img_list)} text regions")

            # Step 3: Classification - correct orientation
            img_list, _ = self.classification(img_list)
            self.log(f"Classified {len(img_list)} text regions")

            # Step 4: Recognition - extract text
            results, confidences = self.recognition(img_list)

            # Combine results from all text regions
            all_texts = []
            all_confidences = []

            for i, (text, conf) in enumerate(zip(results, confidences)):
                if text.strip():  # Only include non-empty text
                    all_texts.append(text)
                    # Calculate average confidence for this text region
                    avg_conf = np.mean(conf) if conf else 0.0
                    all_confidences.append(avg_conf)

            # Combine all detected text
            combined_text = " ".join(all_texts)
            # Use average of all region confidences as overall confidence
            overall_confidence = np.mean(all_confidences) if all_confidences else 0.0

            # Prepare text regions info
            text_regions = []
            for i, (box, text, conf) in enumerate(zip(dt_boxes, results, confidences)):
                avg_conf = np.mean(conf) if conf else 0.0
                text_regions.append({
                    "bbox": box.tolist(),
                    "text": text,
                    "confidence": float(avg_conf)
                })

            result = {
                "text": combined_text,
                "confidence": float(overall_confidence),
                "original_size": original_size,
                "text_regions": text_regions,
                "num_regions": len(text_regions)
            }

            return self.create_response("success", result)

        except Exception as e:
            self.log(f"Text recognition failed: {e}", "ERROR")
            import traceback
            self.log(f"Traceback: {traceback.format_exc()}", "ERROR")
            return self.create_response("error", str(e))

    def _recognize_paddle(self, payload: str) -> str:
        """Recognize text using PaddleOCR model"""
        return self._recognize_text(payload)

    def _list_models(self) -> str:
        """List available models"""
        model_list = []
        if self.detection is not None:
            model_list.append({
                "name": "detection",
                "type": "onnx",
                "description": "PaddleOCR text detection ONNX model",
                "loaded": True
            })
        if self.classification is not None:
            model_list.append({
                "name": "classification",
                "type": "onnx",
                "description": "PaddleOCR text classification ONNX model",
                "loaded": True
            })
        if self.recognition is not None:
            model_list.append({
                "name": "recognition",
                "type": "onnx",
                "description": "PaddleOCR text recognition ONNX model",
                "loaded": True
            })

        return self.create_response("success", {"models": model_list})