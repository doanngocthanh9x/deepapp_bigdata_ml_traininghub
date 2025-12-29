"""
VietOCR with PaddleOCR Pipeline Worker: AAA0_0102_W
Handles text recognition using PaddleOCR detection/classification + VietOCR recognition
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

try:
    from vietocr.tool.predictor import Predictor
    from vietocr.tool.config import Cfg
    from PIL import Image
    from nets.nn import Detection, Classification
    from utils.util import crop_image, sort_polygon
    VIETOCR_AVAILABLE = True
    PADDLE_AVAILABLE = True
except ImportError:
    VIETOCR_AVAILABLE = False
    PADDLE_AVAILABLE = False


@register_worker("AAA0_0102_W")
class AAA0_0102_Worker(BaseWorker):
    """
    VietOCR with PaddleOCR Pipeline Worker: AAA0_0102_W
    Handles text recognition using PaddleOCR detection/classification + VietOCR recognition
    """

    def __init__(self):
        super().__init__("AAA0_0102_Worker")
        self.log("VietOCR with PaddleOCR Pipeline Worker Initialized")
        self.detection = None
        self.classification = None
        self.vietocr_predictor = None
        self._initialize_models()

    def _initialize_models(self):
        """Initialize PaddleOCR and VietOCR models"""
        self.log(f"PADDLE_AVAILABLE: {PADDLE_AVAILABLE}, VIETOCR_AVAILABLE: {VIETOCR_AVAILABLE}")

        if not PADDLE_AVAILABLE or not VIETOCR_AVAILABLE:
            self.log("Required libraries not available", "ERROR")
            return

        # Load PaddleOCR models
        self.detection = self._load_detection_model()
        self.classification = self._load_classification_model()

        # Load VietOCR
        self._initialize_vietocr()

        # Log initialization status
        self.log(f"Models initialized - Detection: {self.detection is not None}, Classification: {self.classification is not None}, VietOCR: {self.vietocr_predictor is not None}")

    def _initialize_vietocr(self):
        """Initialize VietOCR predictor"""
        try:
            config = Cfg.load_config_from_name('vgg_transformer')
            config['device'] = 'cpu'
            self.vietocr_predictor = Predictor(config)
            self.log("VietOCR predictor initialized")
        except Exception as e:
            self.log(f"Failed to initialize VietOCR: {e}", "ERROR")
            self.vietocr_predictor = None

    def process_task(self, event_type: str, payload: str) -> str:
        """Process OCR pipeline tasks"""
        self.log(f"Processing OCR pipeline task: {event_type}")

        try:
            if event_type == "recognize_pipeline":
                return self._recognize_pipeline(payload)
            elif event_type == "health_check":
                return self._health_check()
            else:
                return self.create_response("unknown_event",
                    f"Event type '{event_type}' not supported")
        except Exception as e:
            self.log(f"Error processing OCR pipeline task: {e}", "ERROR")
            return self.create_response("error", str(e))

    def can_handle(self, event_type: str) -> bool:
        """Check supported event types"""
        return event_type in ["recognize_pipeline", "health_check"]

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

    def _preprocess_image(self, image_data: str) -> np.ndarray:
        """Preprocess image for OCR pipeline"""
        try:
            # Decode base64 image
            if image_data.startswith('data:image'):
                header, image_data = image_data.split(',', 1)

            image_bytes = base64.b64decode(image_data)
            image_array = np.frombuffer(image_bytes, dtype=np.uint8)
            image = cv2.imdecode(image_array, cv2.IMREAD_COLOR)

            if image is None:
                raise ValueError("Failed to decode image")

            # Convert BGR to RGB
            image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)

            return image

        except Exception as e:
            raise ValueError(f"Image preprocessing failed: {str(e)}")

    def _rotate_image(self, image: np.ndarray, angle: int) -> np.ndarray:
        """Rotate image by angle (0, 90, 180, 270)"""
        if angle == 0:
            return image
        elif angle == 90:
            return cv2.rotate(image, cv2.ROTATE_90_COUNTERCLOCKWISE)
        elif angle == 180:
            return cv2.rotate(image, cv2.ROTATE_180)
        elif angle == 270:
            return cv2.rotate(image, cv2.ROTATE_90_CLOCKWISE)
        return image

    def _recognize_pipeline(self, payload: str) -> str:
        """Run full OCR pipeline: PaddleOCR detection/classification + VietOCR recognition"""
        self.log("Starting OCR pipeline recognition")
        try:
            # Parse payload
            self.log(f"Parsing payload: {payload[:100]}...")
            data = json.loads(payload)
            image_data = data.get('image')

            if not image_data:
                self.log("No image data provided", "ERROR")
                return self.create_response("error", "No image data provided")

            self.log("Preprocessing image")
            # Preprocess image
            rgb_frame = self._preprocess_image(image_data)
            self.log(f"Image preprocessed, shape: {rgb_frame.shape}")

            # Step 1: Detect text regions with PaddleOCR
            if self.detection is None:
                self.log("Detection model not loaded", "ERROR")
                return self.create_response("error", "Detection model not loaded")

            self.log("Running detection")
            points = self.detection(rgb_frame)
            points = sort_polygon(list(points))
            self.log(f"Detection completed, found {len(points)} regions")

            # Step 2: Crop regions
            self.log("Cropping regions")
            cropped_images = [crop_image(rgb_frame, x) for x in points]
            self.log(f"Cropped {len(cropped_images)} images")

            # Step 3: Classify orientation and rotate
            if self.classification is None:
                self.log("Classification model not loaded", "ERROR")
                return self.create_response("error", "Classification model not loaded")

            self.log("Running classification")
            cropped_images_rotated, angles = self.classification(cropped_images)
            self.log(f"Classification completed, angles: {angles}")

            # Step 4: Recognize with VietOCR
            if self.vietocr_predictor is None:
                self.log("VietOCR predictor not initialized", "ERROR")
                return self.create_response("error", "VietOCR predictor not initialized")

            self.log("Running VietOCR recognition")
            results = []

            for i, (cropped, angle) in enumerate(zip(cropped_images_rotated, angles)):
                # Convert to PIL Image
                pil_image = Image.fromarray(cropped)

                # Optional: Enhance contrast for better recognition
                gray = cv2.cvtColor(cropped, cv2.COLOR_RGB2GRAY)
                enhanced = cv2.equalizeHist(gray)
                pil_enhanced = Image.fromarray(enhanced)

                # Recognize text
                text = self.vietocr_predictor.predict(pil_enhanced)

                results.append({
                    'text': text,
                    'angle': int(angle[0]) if isinstance(angle, list) else int(angle),
                    'bbox': points[i].tolist() if hasattr(points[i], 'tolist') else points[i]
                })

            self.log(f"OCR pipeline completed successfully with {len(results)} results")
            return self.create_response("success", {
                "results": results,
                "total_regions": len(results)
            })

        except Exception as e:
            self.log(f"Pipeline recognition failed: {e}", "ERROR")
            import traceback
            self.log(f"Traceback: {traceback.format_exc()}", "ERROR")
            return self.create_response("error", str(e))

    def _health_check(self) -> str:
        """Check if all models are loaded and ready"""
        health = {
            "detection_model": self.detection is not None,
            "classification_model": self.classification is not None,
            "vietocr_predictor": self.vietocr_predictor is not None,
            "paddle_available": PADDLE_AVAILABLE,
            "vietocr_available": VIETOCR_AVAILABLE
        }

        all_ready = all(health.values())
        return self.create_response("health_check", {
            "status": "healthy" if all_ready else "unhealthy",
            "components": health
        })