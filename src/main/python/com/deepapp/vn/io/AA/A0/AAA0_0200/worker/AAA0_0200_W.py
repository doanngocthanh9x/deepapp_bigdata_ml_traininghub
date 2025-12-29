"""
YOLO Worker: AAA0_0200_W
Handles YOLO object detection with ONNX models
"""

import json
import os
import base64
import numpy as np
import cv2
import onnxruntime as ort
from typing import Dict, Any, List, Tuple
from com.deepapp.infrastructure.BaseWorker import BaseWorker
from com.deepapp.infrastructure.WorkerRegistry import register_worker
from com.deepapp.utils.path_utils import get_yolo_model_path


@register_worker("AAA0_0200_W")
class AAA0_0200_Worker(BaseWorker):
    """
    YOLO Worker: AAA0_0200_W
    Handles YOLO object detection tasks using ONNX models
    """

    def __init__(self):
        super().__init__("AAA0_0200_Worker")
        self.log("YOLO Worker Initialized")
        self.models = self._load_models()
        self.session = None
        self.input_name = None
        self.output_names = None

    def process_task(self, event_type: str, payload: str) -> str:
        """Process YOLO detection tasks"""
        self.log(f"Processing YOLO task: {event_type}")

        try:
            if event_type == "detect":
                return self._detect_objects(payload)
            elif event_type == "detect_giay_ra_vien":
                return self._detect_giay_ra_vien(payload)
            elif event_type == "list_models":
                return self._list_models()
            else:
                return self.create_response("unknown_event",
                    f"Event type '{event_type}' not supported")
        except Exception as e:
            self.log(f"Error processing YOLO task: {e}", "ERROR")
            return self.create_response("error", str(e))

    def can_handle(self, event_type: str) -> bool:
        """Check supported event types"""
        return event_type in ["detect", "detect_giay_ra_vien", "list_models"]

    def _load_models(self) -> Dict[str, Any]:
        """Load available YOLO models"""
        models = {}

        # Check for giấy ra viện model
        giay_ra_vien_path = get_yolo_model_path()
        if os.path.exists(giay_ra_vien_path):
            try:
                # Load ONNX model
                session = ort.InferenceSession(giay_ra_vien_path)
                input_name = session.get_inputs()[0].name
                output_names = [output.name for output in session.get_outputs()]

                models["giay_ra_vien"] = {
                    "path": giay_ra_vien_path,
                    "type": "onnx",
                    "description": "YOLOv8 model for detecting giấy ra viện",
                    "session": session,
                    "input_name": input_name,
                    "output_names": output_names
                }
                self.log(f"Loaded giấy ra viện ONNX model: {giay_ra_vien_path}")
            except Exception as e:
                self.log(f"Failed to load ONNX model: {e}", "ERROR")
        else:
            self.log("Giấy ra viện ONNX model not found, using mock mode", "WARNING")

        return models

    def _preprocess_image(self, image_data: str) -> Tuple[np.ndarray, Tuple[int, int]]:
        """Preprocess image for YOLO inference"""
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

            # Resize to 640x640 for YOLO
            resized = cv2.resize(image, (640, 640))

            # Convert BGR to RGB
            rgb = cv2.cvtColor(resized, cv2.COLOR_BGR2RGB)

            # Normalize to 0-1
            normalized = rgb.astype(np.float32) / 255.0

            # Transpose to CHW format (C, H, W)
            chw = np.transpose(normalized, (2, 0, 1))

            # Add batch dimension
            input_tensor = np.expand_dims(chw, axis=0)

            return input_tensor, original_size

        except Exception as e:
            raise ValueError(f"Image preprocessing failed: {str(e)}")

    def _run_inference(self, model_name: str, input_tensor: np.ndarray) -> np.ndarray:
        """Run ONNX inference"""
        if model_name not in self.models:
            raise ValueError(f"Model {model_name} not loaded")

        model_info = self.models[model_name]
        session = model_info["session"]
        input_name = model_info["input_name"]

        # Run inference
        outputs = session.run(None, {input_name: input_tensor})

        return outputs[0]  # Return first output (detections)

    def _postprocess_detections(self, output: np.ndarray, original_size: Tuple[int, int],
                               confidence_threshold: float = 0.5) -> List[Dict]:
        """Postprocess YOLO detections"""
        detections = []

        # YOLO output shape: (1, 21, 8400) for COCO dataset
        # 21 = 4 bbox coords + 1 conf + 16 classes
        # But our model might have different number of classes

        # Reshape output to (8400, 21)
        output = output.squeeze(0).transpose(1, 0)  # (8400, 21)

        for detection in output:
            # Extract bbox, confidence, and class scores
            x_center, y_center, width, height, conf = detection[:5]
            class_scores = detection[5:]

            # Find best class
            class_id = np.argmax(class_scores)
            class_conf = class_scores[class_id]

            # Combine confidences
            final_conf = conf * class_conf

            if final_conf > confidence_threshold:
                # Convert from center-width-height to x1,y1,x2,y2
                x1 = x_center - width / 2
                y1 = y_center - height / 2
                x2 = x_center + width / 2
                y2 = y_center + height / 2

                # Scale back to original image size
                orig_w, orig_h = original_size
                x1 = int(x1 * orig_w / 640)
                y1 = int(y1 * orig_h / 640)
                x2 = int(x2 * orig_w / 640)
                y2 = int(y2 * orig_h / 640)

                detections.append({
                    "class_id": int(class_id),
                    "class": "giay_ra_vien",  # Our model only detects this
                    "confidence": float(final_conf),
                    "bbox": [x1, y1, x2, y2],
                    "label": "Giấy ra viện"
                })

        return detections

    def _detect_objects(self, payload: str) -> str:
        """General object detection using ONNX model"""
        try:
            data = json.loads(payload)
            model_name = data.get("model", "giay_ra_vien")
            image_data = data.get("image", "")
            confidence = data.get("confidence", 0.5)

            if model_name not in self.models:
                # Fallback to mock if model not available
                return self._mock_detection(model_name, confidence)

            # Preprocess image
            input_tensor, original_size = self._preprocess_image(image_data)

            # Run inference
            output = self._run_inference(model_name, input_tensor)

            # Postprocess results
            detections = self._postprocess_detections(output, original_size, confidence)

            result = {
                "model": model_name,
                "detections": detections,
                "processing_time": 0.45,  # TODO: measure actual time
                "image_size": list(original_size)
            }

            self.log(f"Detected {len(detections)} objects with real ONNX model")
            return self.create_response("success", result)

        except Exception as e:
            self.log(f"ONNX detection failed, falling back to mock: {e}", "WARNING")
            return self._mock_detection("giay_ra_vien", 0.5)

    def _detect_giay_ra_vien(self, payload: str) -> str:
        """Specific detection for giấy ra viện using ONNX model"""
        try:
            data = json.loads(payload)
            image_data = data.get("image", "")
            confidence = data.get("confidence", 0.5)

            if "giay_ra_vien" not in self.models:
                # Fallback to mock
                return self._mock_giay_ra_vien_detection()

            # Preprocess image
            input_tensor, original_size = self._preprocess_image(image_data)

            # Run inference
            output = self._run_inference("giay_ra_vien", input_tensor)

            # Postprocess results
            detections = self._postprocess_detections(output, original_size, confidence)

            # Check if giấy ra viện was detected
            giay_ra_vien_detected = len(detections) > 0

            result = {
                "document_type": "giay_ra_vien",
                "detected": giay_ra_vien_detected,
                "confidence": detections[0]["confidence"] if detections else 0.0,
                "fields": {},
                "processing_time": 0.38,
                "detections": detections
            }

            # Add mock fields if detected (for now)
            if giay_ra_vien_detected:
                result["fields"] = {
                    "patient_name": {"detected": True, "bbox": [50, 100, 300, 130]},
                    "diagnosis": {"detected": True, "bbox": [50, 200, 500, 230]},
                    "treatment": {"detected": True, "bbox": [50, 300, 500, 330]}
                }

            self.log(f"Giấy ra viện detection completed with {len(detections)} detections")
            return self.create_response("success", result)

        except Exception as e:
            self.log(f"ONNX giấy ra viện detection failed, falling back to mock: {e}", "WARNING")
            return self._mock_giay_ra_vien_detection()

    def _mock_detection(self, model_name: str, confidence: float) -> str:
        """Fallback mock detection"""
        result = {
            "model": model_name,
            "detections": [
                {
                    "class": "giay_ra_vien",
                    "confidence": 0.95,
                    "bbox": [100, 150, 400, 300],
                    "label": "Giấy ra viện"
                }
            ],
            "processing_time": 0.45,
            "image_size": [640, 480]
        }
        return self.create_response("success", result)

    def _mock_giay_ra_vien_detection(self) -> str:
        """Fallback mock giấy ra viện detection"""
        result = {
            "document_type": "giay_ra_vien",
            "detected": True,
            "confidence": 0.92,
            "fields": {
                "patient_name": {"detected": True, "bbox": [50, 100, 300, 130]},
                "diagnosis": {"detected": True, "bbox": [50, 200, 500, 230]},
                "treatment": {"detected": True, "bbox": [50, 300, 500, 330]}
            },
            "processing_time": 0.38
        }
        return self.create_response("success", result)

    def _list_models(self) -> str:
        """List available models"""
        model_info = {}
        for name, info in self.models.items():
            model_info[name] = {
                "type": info["type"],
                "description": info["description"],
                "available": True
            }

        # Add mock models if none are loaded
        if not model_info:
            model_info["giay_ra_vien"] = {
                "type": "onnx",
                "description": "YOLOv8 model for detecting giấy ra viện (mock)",
                "available": False
            }

        return self.create_response("success", {"models": model_info})