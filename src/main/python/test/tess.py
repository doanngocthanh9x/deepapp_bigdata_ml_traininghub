# Import required libraries
from PIL import Image
import pytesseract
import cv2
import numpy as np
from vietocr.tool.predictor import Predictor
from vietocr.tool.config import Cfg

# Load the image
image_path = 'Dja chi 610 van tien dungPhuong Hoa XuanQuan Cam LeThanh pho Da Nang_25.png'
image = Image.open(image_path)

# Preprocess the image for better OCR accuracy
image_cv = cv2.cvtColor(np.array(image), cv2.COLOR_RGB2BGR)
gray = cv2.cvtColor(image_cv, cv2.COLOR_BGR2GRAY)

# Try multiple preprocessing techniques
# Method 1: Adaptive threshold
thresh = cv2.adaptiveThreshold(gray, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 11, 2)

# Method 2: Simple threshold (uncomment to try)
# _, thresh = cv2.threshold(gray, 150, 255, cv2.THRESH_BINARY)

# Method 3: Otsu's threshold (uncomment to try)
# _, thresh = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)

preprocessed_image = Image.fromarray(thresh)

# Configure pytesseract for Vietnamese
# Add language support: 'vie' for Vietnamese, 'eng' for English
custom_config = r'--oem 3 --psm 6 -l vie+eng'

# Configure VietOCR
config = Cfg.load_config_from_name('vgg_transformer')
config['device'] = 'cpu'  # or 'cuda:0' if GPU available
predictor = Predictor(config)

# Extract text
data = pytesseract.image_to_data(preprocessed_image, output_type=pytesseract.Output.DICT, config=custom_config)

# Also get the full text
full_text = pytesseract.image_to_string(preprocessed_image, config=custom_config)
print("Full Extracted Text:")
print(full_text)
print("\n" + "="*50 + "\n")

# Get indices of non-empty text detections
indices = [i for i in range(len(data['level'])) if data['text'][i].strip() != '']

# Sort indices by top (y) coordinate first, then left (x) for natural reading order
indices.sort(key=lambda i: (data['top'][i], data['left'][i]))

# Create a copy of the image for drawing
result_image = image_cv.copy()

# Loop through each detected word
print("Detected words with bounding boxes:")
for i in indices:
    text = data['text'][i]
    x, y, w, h = data['left'][i], data['top'][i], data['width'][i], data['height'][i]
    conf = data['conf'][i]  # Confidence score
    
    print(f"Text: '{text}' - Box: ({x}, {y}, {x+w}, {y+h}) - Confidence: {conf}")
    
    # Crop the bounding box
    cropped = image_cv[y:y+h, x:x+w]
    cropped_pil = Image.fromarray(cropped)
    
    # Use VietOCR to process the cropped image
    vietocr_text = predictor.predict(cropped_pil)
    print(f"VietOCR Text: '{vietocr_text}'")
    
    # Draw bounding box
    cv2.rectangle(result_image, (x, y), (x + w, y + h), (0, 255, 0), 2)
    # Draw text label
    cv2.putText(result_image, text, (x, y - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 255), 2)

# Show the final result with all bounding boxes
cv2.imshow('Image with OCR Bounding Boxes', result_image)
cv2.waitKey(0)
cv2.destroyAllWindows()

# Optionally save the result
#cv2.imwrite('output_with_boxes.png', result_image)
#print("\nResult saved as 'output_with_boxes.png'")