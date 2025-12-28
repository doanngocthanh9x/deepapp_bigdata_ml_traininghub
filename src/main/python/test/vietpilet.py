# Pipeline: PaddleOCR detection → Classification → Pytesseract sub-detection → VietOCR recognition
import cv2
import numpy as np
from PIL import Image
import pytesseract
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

def preprocess_for_pytesseract(image):
    """
    Preprocess image for better pytesseract detection
    """
    gray = cv2.cvtColor(image, cv2.COLOR_RGB2GRAY)
    
    # Try adaptive threshold
    thresh = cv2.adaptiveThreshold(
        gray, 255, 
        cv2.ADAPTIVE_THRESH_GAUSSIAN_C, 
        cv2.THRESH_BINARY, 11, 2
    )
    
    return thresh

def detect_sub_regions_with_pytesseract(cropped_image):
    """
    Use pytesseract ONLY for detecting bounding boxes, NOT for text recognition
    Returns list of (x, y, w, h) tuples
    """
    # Preprocess
    preprocessed = preprocess_for_pytesseract(cropped_image)
    pil_preprocessed = Image.fromarray(preprocessed)
    
    # Get bounding boxes from pytesseract (ONLY for detection)
    custom_config = r'--oem 3 --psm 6 -l vie+eng'
    data = pytesseract.image_to_data(
        pil_preprocessed, 
        output_type=pytesseract.Output.DICT, 
        config=custom_config
    )
    
    # Filter valid detections - only get bounding boxes
    sub_regions = []
    n_boxes = len(data['level'])
    
    for i in range(n_boxes):
        text = data['text'][i].strip()
        conf = int(data['conf'][i]) if data['conf'][i] != '-1' else 0
        
        # Only keep detections with reasonable confidence
        # We ignore the text here, only use it to filter valid boxes
        if text and conf > 30:
            x = data['left'][i]
            y = data['top'][i]
            w = data['width'][i]
            h = data['height'][i]
            sub_regions.append((x, y, w, h))
    
    return sub_regions

def enhance_for_vietocr(image):
    """
    Preprocess image specifically for VietOCR
    """
    gray = cv2.cvtColor(image, cv2.COLOR_RGB2GRAY)
    
    # Histogram equalization for better contrast
    enhanced = cv2.equalizeHist(gray)
    
    return enhanced

def process_image(image_path, use_pytesseract_subdetection=True):
    """
    Main processing pipeline
    - PaddleOCR: detect large text regions
    - Classification: detect rotation angle
    - Pytesseract: detect smaller sub-regions (bounding boxes only)
    - VietOCR: recognize ALL text (final recognition)
    
    Args:
        image_path: Path to input image
        use_pytesseract_subdetection: If True, use pytesseract to detect sub-regions
    """
    # Load image
    frame = cv2.imread(image_path)
    rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    
    # Step 1: Detect text regions with PaddleOCR
    points = detection(rgb_frame)
    points = util.sort_polygon(list(points))
    
    print(f"PaddleOCR detected {len(points)} regions")
    
    # Step 2: Crop regions
    cropped_images = [util.crop_image(rgb_frame, x) for x in points]
    
    # Step 3: Classify orientation and rotate
    cropped_images_rotated, angles = classification(cropped_images)
    
    # Step 4: Process each region
    all_results = []
    
    for i, (cropped, angle_info) in enumerate(zip(cropped_images_rotated, angles)):
        # Extract angle value
        if isinstance(angle_info, (list, tuple)):
            angle = int(angle_info[0]) if str(angle_info[0]).replace('.','').isdigit() else 0
        else:
            angle = int(angle_info) if str(angle_info).replace('.','').isdigit() else 0
        
        print(f"\nProcessing region {i} (angle: {angle}°)")
        
        point = points[i]
        # Convert point to proper format for OpenCV
        point_array = np.array(point, dtype=np.int32)
        bbox_x, bbox_y, bbox_w, bbox_h = cv2.boundingRect(point_array)
        
        if use_pytesseract_subdetection:
            # Step 4a: Detect sub-regions with pytesseract (ONLY bounding boxes)
            sub_regions = detect_sub_regions_with_pytesseract(cropped)
            print(f"  Pytesseract detected {len(sub_regions)} sub-regions (bounding boxes only)")
            
            if len(sub_regions) > 0:
                # Process each sub-region with VietOCR (ONLY VietOCR for text recognition)
                for j, (sub_x, sub_y, sub_w, sub_h) in enumerate(sub_regions):
                    # Crop the sub-region
                    sub_cropped = cropped[sub_y:sub_y+sub_h, sub_x:sub_x+sub_w]
                    
                    if sub_cropped.size == 0:
                        continue
                    
                    # Enhance for VietOCR
                    enhanced = enhance_for_vietocr(sub_cropped)
                    pil_enhanced = Image.fromarray(enhanced)
                    
                    # Recognize with VietOCR (FINAL and ONLY text recognition)
                    viet_text = viet_predictor.predict(pil_enhanced)
                    
                    print(f"    Sub-region {j}: VietOCR detected '{viet_text}'")
                    
                    # Store result
                    all_results.append({
                        'text': viet_text,  # Only VietOCR text
                        'angle': angle,
                        'region_id': i,
                        'sub_region_id': j,
                        'bbox': (bbox_x + sub_x, bbox_y + sub_y, sub_w, sub_h),
                        'parent_bbox': point
                    })
                    
                    # Draw sub-region on original image
                    abs_x = bbox_x + sub_x
                    abs_y = bbox_y + sub_y
                    cv2.rectangle(frame, (abs_x, abs_y), (abs_x+sub_w, abs_y+sub_h), 
                                (255, 0, 0), 1)  # Blue for sub-regions
                    
                    # Display text
                    display_text = viet_text[:20] if len(viet_text) > 20 else viet_text
                    cv2.putText(frame, display_text, (abs_x, abs_y-3), 
                               cv2.FONT_HERSHEY_SIMPLEX, 0.3, (255, 0, 0), 1)
            else:
                # No sub-regions detected, process whole region with VietOCR
                enhanced = enhance_for_vietocr(cropped)
                pil_enhanced = Image.fromarray(enhanced)
                
                # Use VietOCR for recognition
                text = viet_predictor.predict(pil_enhanced)
                print(f"  Whole region: VietOCR detected '{text}'")
                
                all_results.append({
                    'text': text,
                    'angle': angle,
                    'region_id': i,
                    'sub_region_id': None,
                    'bbox': (bbox_x, bbox_y, bbox_w, bbox_h),
                    'parent_bbox': point
                })
                
                # Draw text on image
                display_text = text[:30] if len(text) > 30 else text
                cv2.putText(frame, display_text, (bbox_x, bbox_y + bbox_h + 15), 
                           cv2.FONT_HERSHEY_SIMPLEX, 0.4, (0, 255, 0), 1)
        else:
            # Original method: process whole region with VietOCR
            enhanced = enhance_for_vietocr(cropped)
            pil_enhanced = Image.fromarray(enhanced)
            
            # Use VietOCR for recognition
            text = viet_predictor.predict(pil_enhanced)
            
            all_results.append({
                'text': text,
                'angle': angle,
                'region_id': i,
                'bbox': (bbox_x, bbox_y, bbox_w, bbox_h),
                'parent_bbox': point
            })
        
        # Draw parent region on original image
        cv2.polylines(frame, [point_array], True, (0, 255, 0), 2)  # Green for parent regions
        label = f"R{i} ({angle}°)"
        cv2.putText(frame, label, (bbox_x, bbox_y-5), 
                   cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 2)
    
    return all_results, frame

# Usage
print("="*80)
print("Pipeline: PaddleOCR (detect) → Classification (rotate) → Pytesseract (sub-detect) → VietOCR (recognize)")
print("="*80)

results, annotated_image = process_image(
    '/home/vpslocal/new_workspace/deepapp_bigdata_ml_traininghub/src/main/resources/models/yolo/giay_ra_vien/01HM00012177_300005_image_74.png',
    use_pytesseract_subdetection=True
)

print("\n" + "="*80)
print("FINAL RESULTS (All text from VietOCR):")
print("="*80)
for idx, result in enumerate(results):
    if result.get('sub_region_id') is not None:
        print(f"{idx}: Region {result['region_id']}.{result['sub_region_id']} → '{result['text']}' (angle: {result['angle']}°)")
    else:
        print(f"{idx}: Region {result['region_id']} → '{result['text']}' (angle: {result['angle']}°)")

# Save result
output_path = 'output_vietocr_only.png'
cv2.imwrite(output_path, annotated_image)
print(f"\nResult saved to: {output_path}")

cv2.imshow('Result', annotated_image)
cv2.waitKey(0)
cv2.destroyAllWindows()