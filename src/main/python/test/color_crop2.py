import cv2
import numpy as np

# Đọc ảnh
img_path = "/home/vpslocal/new_workspace/deepapp_bigdata_ml_traininghub/src/main/resources/models/yolo/giay_ra_vien/01HM00012249_300005_image_82.png"
img = cv2.imread(img_path)

if img is None:
    print(f"Không thể đọc ảnh tại: {img_path}")
    exit()

img = cv2.resize(img, (960, 1280))
original_img = img.copy()
hsv = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)

# Biến toàn cục
drawing = False
start_point = (-1, -1)
end_point = (-1, -1)
selected_color = None

def mouse_callback(event, x, y, flags, param):
    global drawing, start_point, end_point, selected_color, img
    
    if event == cv2.EVENT_LBUTTONDOWN:
        drawing = True
        start_point = (x, y)
    
    elif event == cv2.EVENT_MOUSEMOVE:
        if drawing:
            end_point = (x, y)
            temp_img = img.copy()
            cv2.rectangle(temp_img, start_point, end_point, (0, 255, 0), 2)
            cv2.imshow("Original - Chon vung mau", temp_img)
    
    elif event == cv2.EVENT_LBUTTONUP:
        drawing = False
        end_point = (x, y)
        
        x1, y1 = start_point
        x2, y2 = end_point
        x_min = min(x1, x2)
        x_max = max(x1, x2)
        y_min = min(y1, y2)
        y_max = max(y1, y2)
        
        roi = hsv[y_min:y_max, x_min:x_max]
        if roi.size > 0:
            selected_color = np.mean(roi, axis=(0, 1)).astype(int)
            print(f"Đã chọn HSV: {selected_color}")

def order_points(pts):
    """Sắp xếp 4 điểm theo thứ tự: top-left, top-right, bottom-right, bottom-left"""
    rect = np.zeros((4, 2), dtype="float32")
    s = pts.sum(axis=1)
    rect[0] = pts[np.argmin(s)]
    rect[2] = pts[np.argmax(s)]
    diff = np.diff(pts, axis=1)
    rect[1] = pts[np.argmin(diff)]
    rect[3] = pts[np.argmax(diff)]
    return rect

def extend_corners_to_image_bounds(corners, img_shape, margin=50):
    """Mở rộng góc về phía biên ảnh nếu tài liệu bị mất góc"""
    h, w = img_shape[:2]
    extended = corners.copy()
    
    for i, corner in enumerate(corners):
        x, y = corner
        if y < margin:
            extended[i][1] = 0
        if y > h - margin:
            extended[i][1] = h - 1
        if x < margin:
            extended[i][0] = 0
        if x > w - margin:
            extended[i][0] = w - 1
    
    return extended

def find_best_4_corners(contour, img_shape):
    """Tìm 4 góc tốt nhất từ contour"""
    epsilon = 0.02 * cv2.arcLength(contour, True)
    approx = cv2.approxPolyDP(contour, epsilon, True)
    
    if len(approx) == 4:
        print("✓ Phương pháp: Approximation (4 góc)")
        return approx.reshape(4, 2), "approximation"
    
    # Fallback: Rotated Bounding Box
    rect = cv2.minAreaRect(contour)
    box = cv2.boxPoints(rect)
    box = box.astype(int)  # Thay np.int0 bằng astype(int)
    print("✓ Phương pháp: Rotated Bounding Box")
    
    extended_box = extend_corners_to_image_bounds(box, img_shape, margin=30)
    return extended_box, "rotated_bbox"

def preprocess_for_ocr(image, method='adaptive'):
    """
    Xử lý ảnh để OCR text tốt hơn
    method: 'adaptive', 'otsu', 'sauvola', 'combined'
    """
    # Chuyển sang grayscale
    if len(image.shape) == 3:
        gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    else:
        gray = image.copy()
    
    # Tăng độ tương phản (CLAHE)
    clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
    enhanced = clahe.apply(gray)
    
    # Khử nhiễu
    denoised = cv2.fastNlMeansDenoising(enhanced, None, h=10, templateWindowSize=7, searchWindowSize=21)
    
    if method == 'adaptive':
        # Adaptive Threshold - tốt cho ảnh có độ sáng không đồng đều
        binary = cv2.adaptiveThreshold(
            denoised, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, 
            cv2.THRESH_BINARY, 11, 2
        )
    elif method == 'otsu':
        # Otsu's Thresholding - tốt cho ảnh có histogram rõ ràng
        _, binary = cv2.threshold(denoised, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
    elif method == 'sauvola':
        # Sauvola - tốt cho văn bản có background phức tạp
        window_size = 25
        k = 0.2
        mean = cv2.blur(denoised, (window_size, window_size))
        mean_sq = cv2.blur(denoised**2, (window_size, window_size))
        std = np.sqrt(mean_sq - mean**2)
        threshold = mean * (1 + k * ((std / 128) - 1))
        binary = np.where(denoised > threshold, 255, 0).astype(np.uint8)
    else:  # combined
        # Kết hợp nhiều phương pháp
        _, otsu = cv2.threshold(denoised, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
        adaptive = cv2.adaptiveThreshold(
            denoised, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, 
            cv2.THRESH_BINARY, 11, 2
        )
        binary = cv2.bitwise_and(otsu, adaptive)
    
    # Morphological operations để làm sạch text
    kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (2, 2))
    
    # Loại bỏ noise nhỏ
    opening = cv2.morphologyEx(binary, cv2.MORPH_OPEN, kernel, iterations=1)
    
    # Kết nối các ký tự bị đứt
    kernel_close = cv2.getStructuringElement(cv2.MORPH_RECT, (2, 1))
    closing = cv2.morphologyEx(opening, cv2.MORPH_CLOSE, kernel_close, iterations=1)
    
    return closing

def deskew_image(image):
    """Xoay ảnh để text thẳng hàng (deskew)"""
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY) if len(image.shape) == 3 else image
    
    # Phát hiện cạnh
    edges = cv2.Canny(gray, 50, 150, apertureSize=3)
    
    # Phát hiện đường thẳng bằng Hough Transform
    lines = cv2.HoughLinesP(edges, 1, np.pi/180, 100, minLineLength=100, maxLineGap=10)
    
    if lines is not None and len(lines) > 0:
        angles = []
        for line in lines:
            x1, y1, x2, y2 = line[0]
            angle = np.degrees(np.arctan2(y2 - y1, x2 - x1))
            # Chỉ lấy góc gần nằm ngang
            if abs(angle) < 45:
                angles.append(angle)
        
        if len(angles) > 0:
            median_angle = np.median(angles)
            print(f"Góc nghiêng phát hiện: {median_angle:.2f}°")
            
            # Xoay ảnh
            (h, w) = image.shape[:2]
            center = (w // 2, h // 2)
            M = cv2.getRotationMatrix2D(center, median_angle, 1.0)
            rotated = cv2.warpAffine(image, M, (w, h), 
                                    flags=cv2.INTER_CUBIC, 
                                    borderMode=cv2.BORDER_REPLICATE)
            return rotated
    
    print("Không phát hiện góc nghiêng")
    return image

def sharpen_image(image):
    """Làm sắc nét ảnh"""
    kernel = np.array([[-1,-1,-1],
                       [-1, 9,-1],
                       [-1,-1,-1]])
    sharpened = cv2.filter2D(image, -1, kernel)
    return sharpened

# ===== MAIN PROCESS =====

print("Hãy vẽ bbox xung quanh vùng màu của tài liệu...")
cv2.imshow("Original - Chon vung mau", img)
cv2.setMouseCallback("Original - Chon vung mau", mouse_callback)

while selected_color is None:
    cv2.waitKey(1)

cv2.destroyAllWindows()

# Tạo mask
tolerance = 35
lower = np.array([max(0, selected_color[0] - tolerance), 
                  max(0, selected_color[1] - tolerance), 
                  max(0, selected_color[2] - tolerance)])
upper = np.array([min(179, selected_color[0] + tolerance), 
                  min(255, selected_color[1] + tolerance), 
                  min(255, selected_color[2] + tolerance)])

mask = cv2.inRange(hsv, lower, upper)

# Làm sạch mask
kernel = np.ones((7, 7), np.uint8)
mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, kernel, iterations=3)
mask = cv2.morphologyEx(mask, cv2.MORPH_OPEN, kernel, iterations=2)
mask = cv2.dilate(mask, kernel, iterations=2)

# Tìm contours
contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

if len(contours) == 0:
    print("Không tìm thấy contour!")
    exit()

largest_contour = max(contours, key=cv2.contourArea)
print(f"Diện tích contour: {cv2.contourArea(largest_contour)}")

# Tìm 4 góc
corners, method = find_best_4_corners(largest_contour, img.shape)
rect = order_points(corners)

# Vẽ góc
img_with_corners = original_img.copy()
colors = [(255, 0, 0), (0, 255, 0), (0, 0, 255), (255, 255, 0)]
labels = ['TL', 'TR', 'BR', 'BL']

for point, color, label in zip(rect, colors, labels):
    pt = tuple(point.astype(int))
    cv2.circle(img_with_corners, pt, 10, color, -1)
    cv2.putText(img_with_corners, label, (pt[0]+15, pt[1]), 
                cv2.FONT_HERSHEY_SIMPLEX, 0.8, color, 2)

cv2.imshow("1. Detected Corners", img_with_corners)

# Tính kích thước đầu ra
(tl, tr, br, bl) = rect
widthA = np.sqrt(((br[0] - bl[0]) ** 2) + ((br[1] - bl[1]) ** 2))
widthB = np.sqrt(((tr[0] - tl[0]) ** 2) + ((tr[1] - tl[1]) ** 2))
maxWidth = max(int(widthA), int(widthB))

heightA = np.sqrt(((tr[0] - br[0]) ** 2) + ((tr[1] - br[1]) ** 2))
heightB = np.sqrt(((tl[0] - bl[0]) ** 2) + ((tl[1] - bl[1]) ** 2))
maxHeight = max(int(heightA), int(heightB))

print(f"Kích thước ảnh đầu ra: {maxWidth}x{maxHeight}")

# Perspective transform
dst = np.array([
    [0, 0],
    [maxWidth - 1, 0],
    [maxWidth - 1, maxHeight - 1],
    [0, maxHeight - 1]
], dtype="float32")

M = cv2.getPerspectiveTransform(rect, dst)
warped = cv2.warpPerspective(original_img, M, (maxWidth, maxHeight))

cv2.imshow("2. Warped Image", warped)

# ===== XỬ LÝ ĐỂ OCR =====

print("\n=== Bắt đầu xử lý ảnh cho OCR ===")

# Bước 1: Deskew (xoay thẳng)
print("Bước 1: Deskew...")
deskewed = deskew_image(warped)
cv2.imshow("3. Deskewed", deskewed)

# Bước 2: Làm sắc nét
print("Bước 2: Sharpen...")
sharpened = sharpen_image(deskewed)
cv2.imshow("4. Sharpened", sharpened)

# Bước 3: Xử lý nhiều phương pháp
print("Bước 3: Binarization...")

# Thử các phương pháp khác nhau
adaptive = preprocess_for_ocr(sharpened, method='adaptive')
otsu = preprocess_for_ocr(sharpened, method='otsu')
combined = preprocess_for_ocr(sharpened, method='combined')

# Hiển thị các kết quả
cv2.imshow("5a. Adaptive Threshold", adaptive)
cv2.imshow("5b. Otsu Threshold", otsu)
cv2.imshow("5c. Combined Method", combined)

# Lưu các kết quả
outputs = {
    "warped": warped,
    "deskewed": deskewed,
    "sharpened": sharpened,
    "adaptive": adaptive,
    "otsu": otsu,
    "combined": combined
}

print("\n=== Lưu kết quả ===")
for name, image in outputs.items():
    path = f"/home/vpslocal/output_{name}.png"
    cv2.imwrite(path, image)
    print(f"✓ {path}")

print("\n" + "="*50)
print("HƯỚNG DẪN:")
print("- 'warped.png': Ảnh sau perspective transform")
print("- 'deskewed.png': Ảnh đã xoay thẳng")
print("- 'sharpened.png': Ảnh đã làm sắc nét")
print("- 'adaptive.png': Binary (Adaptive) - TỐT cho ảnh sáng không đều")
print("- 'otsu.png': Binary (Otsu) - TỐT cho ảnh tương phản cao")
print("- 'combined.png': Binary (Combined) - KHUYẾN NGHỊ cho OCR")
print("="*50)

print("\nNhấn phím bất kỳ để thoát...")
cv2.waitKey(0)
cv2.destroyAllWindows()