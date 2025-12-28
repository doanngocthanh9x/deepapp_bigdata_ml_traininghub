# Pipeline tối ưu: PaddleOCR detection → Classification → Rotation → VietOCR recognition
import cv2
import numpy as np
from PIL import Image
from vietocr.tool.predictor import Predictor
from vietocr.tool.config import Cfg
from nets import nn
from utils import util

# Load models
detection = nn.Detection('/home/vpslocal/new_workspace/deepapp_bigdata_ml_traininghub/src/main/resources/models/paddlet/detection.onnx')
classification = nn.Classification('/home/vpslocal/new_workspace/deepapp_bigdata_ml_traininghub/src/main/resources/models/paddlet/classification.onnx')

config = Cfg.load_config_from_name('vgg_transformer')
config['device'] = 'cpu'
viet_predictor = Predictor(config)

def rotate_image(image, angle):
    """
    Rotate image by angle (0, 90, 180, 270)
    """
    if angle == 0:
        return image
    elif angle == 90:
        return cv2.rotate(image, cv2.ROTATE_90_COUNTERCLOCKWISE)
    elif angle == 180:
        return cv2.rotate(image, cv2.ROTATE_180)
    elif angle == 270:
        return cv2.rotate(image, cv2.ROTATE_90_CLOCKWISE)
    return image

def process_image(image_path):
    # Load image
    frame = cv2.imread(image_path)
    rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    
    # Step 1: Detect text regions with PaddleOCR
    points = detection(rgb_frame)
    points = util.sort_polygon(list(points))
    
    # Step 2: Crop regions
    cropped_images = [util.crop_image(rgb_frame, x) for x in points]
    
    # Step 3: Classify orientation and rotate
    cropped_images_rotated, angles = classification(cropped_images)
    
    # Step 4: Recognize with VietOCR
    results = []
    
    for i, (cropped, angle) in enumerate(zip(cropped_images_rotated, angles)):
        # Convert to PIL Image
        pil_image = Image.fromarray(cropped)
        
        # Optional: Enhance contrast for better recognition
        gray = cv2.cvtColor(cropped, cv2.COLOR_RGB2GRAY)
        
        # Apply adaptive threshold or histogram equalization
        # Method 1: Histogram equalization
        enhanced = cv2.equalizeHist(gray)
        
        # Method 2: Adaptive threshold (uncomment to try)
        # enhanced = cv2.adaptiveThreshold(gray, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, 
        #                                  cv2.THRESH_BINARY, 11, 2)
        
        pil_enhanced = Image.fromarray(enhanced)
        
        # Recognize text
        text = viet_predictor.predict(pil_image)
        results.append({
            'text': text,
            'angle': angle,
            'bbox': points[i]
        })
        
        # Draw on original image
        point = points[i]
        x, y, w, h = cv2.boundingRect(point)
        cv2.rectangle(frame, (x, y), (x+w, y+h), (0, 255, 0), 2)
        
        # Add angle info to display
        label = f"{text} ({angle}°)"
        cv2.putText(frame, label, (x, y-5), 
                   cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 255), 2)
        
        # Optional: Save rotated crops for debugging
        # cv2.imwrite(f'cropped_{i}_angle{angle}.png', cropped)
    
    return results, frame

# Usage
results, annotated_image = process_image('/home/vpslocal/new_workspace/deepapp_bigdata_ml_traininghub/src/main/python/test/01HM00012524_300003_image_92.png')

print("Recognized text with angles:")
for idx, result in enumerate(results):
    print(f"{idx}: '{result['text']}' - Angle: {result['angle']}°")

cv2.imshow('Result', annotated_image)
cv2.waitKey(0)
cv2.destroyAllWindows()