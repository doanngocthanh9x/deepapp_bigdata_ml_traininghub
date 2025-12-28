import cv2
import numpy as np
from scipy.spatial import ConvexHull

# Đọc ảnh
img_path = "/home/vpslocal/new_workspace/deepapp_bigdata_ml_traininghub/src/main/resources/models/yolo/giay_ra_vien/01HM00012177_300005_image_74.png"
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
    rect[0] = pts[np.argmin(s)]  # top-left
    rect[2] = pts[np.argmax(s)]  # bottom-right
    
    diff = np.diff(pts, axis=1)
    rect[1] = pts[np.argmin(diff)]  # top-right
    rect[3] = pts[np.argmax(diff)]  # bottom-left
    
    return rect

def get_bounding_box_corners(contour):
    """Lấy 4 góc của bounding box xoay (rotated rectangle)"""
    rect = cv2.minAreaRect(contour)
    box = cv2.boxPoints(rect)
    box = np.int0(box)
    return box

def extend_corners_to_image_bounds(corners, img_shape, margin=50):
    """Mở rộng góc về phía biên ảnh nếu tài liệu bị mất góc"""
    h, w = img_shape[:2]
    extended = corners.copy()
    
    for i, corner in enumerate(corners):
        x, y = corner
        
        # Nếu góc gần biên trên
        if y < margin:
            extended[i][1] = 0
        # Nếu góc gần biên dưới
        if y > h - margin:
            extended[i][1] = h - 1
        # Nếu góc gần biên trái
        if x < margin:
            extended[i][0] = 0
        # Nếu góc gần biên phải
        if x > w - margin:
            extended[i][0] = w - 1
    
    return extended

def get_extreme_points(contour):
    """Lấy các điểm cực trị từ contour"""
    # Tìm điểm cực trái, phải, trên, dưới
    leftmost = tuple(contour[contour[:, :, 0].argmin()][0])
    rightmost = tuple(contour[contour[:, :, 0].argmax()][0])
    topmost = tuple(contour[contour[:, :, 1].argmin()][0])
    bottommost = tuple(contour[contour[:, :, 1].argmax()][0])
    
    return [leftmost, rightmost, topmost, bottommost]

def find_best_4_corners(contour, img_shape):
    """Tìm 4 góc tốt nhất từ contour bằng nhiều phương pháp"""
    
    # Phương pháp 1: Approximation
    epsilon = 0.02 * cv2.arcLength(contour, True)
    approx = cv2.approxPolyDP(contour, epsilon, True)
    
    if len(approx) == 4:
        print("✓ Phương pháp 1: Tìm thấy 4 góc từ approximation")
        return approx.reshape(4, 2), "approximation"
    
    # Phương pháp 2: Rotated Bounding Box
    box = get_bounding_box_corners(contour)
    print("✓ Phương pháp 2: Sử dụng rotated bounding box")
    
    # Kiểm tra nếu box gần sát biên ảnh, mở rộng ra
    extended_box = extend_corners_to_image_bounds(box, img_shape, margin=30)
    
    return extended_box, "rotated_bbox"

def get_largest_inscribed_rectangle(contour):
    """Tìm hình chữ nhật lớn nhất nằm trong contour"""
    rect = cv2.minAreaRect(contour)
    box = cv2.boxPoints(rect)
    return np.int0(box)

# Hiển thị và chọn vùng màu
print("Hãy vẽ bbox xung quanh vùng màu của tài liệu...")
cv2.imshow("Original - Chon vung mau", img)
cv2.setMouseCallback("Original - Chon vung mau", mouse_callback)

while selected_color is None:
    cv2.waitKey(1)

cv2.destroyAllWindows()

# Tạo mask
tolerance = 35  # Tăng tolerance để bao phủ tốt hơn
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

# Dilate để lấp đầy các khoảng trống
mask = cv2.dilate(mask, kernel, iterations=2)

cv2.imshow("Mask", mask)

# Tìm contours
contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

if len(contours) == 0:
    print("Không tìm thấy contour nào!")
    cv2.waitKey(0)
    cv2.destroyAllWindows()
    exit()

# Lấy contour lớn nhất
largest_contour = max(contours, key=cv2.contourArea)
area = cv2.contourArea(largest_contour)
print(f"Diện tích contour: {area}")

# Vẽ contour
img_with_contour = original_img.copy()
cv2.drawContours(img_with_contour, [largest_contour], -1, (0, 255, 0), 3)
cv2.imshow("Detected Contour", img_with_contour)

# Tìm 4 góc tốt nhất
corners, method = find_best_4_corners(largest_contour, img.shape)
print(f"Phương pháp sử dụng: {method}")

# Sắp xếp các góc
rect = order_points(corners)

# Vẽ các góc
img_with_corners = original_img.copy()
colors = [(255, 0, 0), (0, 255, 0), (0, 0, 255), (255, 255, 0)]
labels = ['TL', 'TR', 'BR', 'BL']

for i, (point, color, label) in enumerate(zip(rect, colors, labels)):
    pt = tuple(point.astype(int))
    cv2.circle(img_with_corners, pt, 10, color, -1)
    cv2.putText(img_with_corners, label, (pt[0]+15, pt[1]), 
                cv2.FONT_HERSHEY_SIMPLEX, 0.8, color, 2)

cv2.imshow("Corners Detected", img_with_corners)

# Tính kích thước ảnh đầu ra
(tl, tr, br, bl) = rect
widthA = np.sqrt(((br[0] - bl[0]) ** 2) + ((br[1] - bl[1]) ** 2))
widthB = np.sqrt(((tr[0] - tl[0]) ** 2) + ((tr[1] - tl[1]) ** 2))
maxWidth = max(int(widthA), int(widthB))

heightA = np.sqrt(((tr[0] - br[0]) ** 2) + ((tr[1] - br[1]) ** 2))
heightB = np.sqrt(((tl[0] - bl[0]) ** 2) + ((tl[1] - bl[1]) ** 2))
maxHeight = max(int(heightA), int(heightB))

print(f"Kích thước ảnh đầu ra: {maxWidth}x{maxHeight}")

# Điểm đích
dst = np.array([
    [0, 0],
    [maxWidth - 1, 0],
    [maxWidth - 1, maxHeight - 1],
    [0, maxHeight - 1]
], dtype="float32")

# Perspective transform
M = cv2.getPerspectiveTransform(rect, dst)
warped = cv2.warpPerspective(original_img, M, (maxWidth, maxHeight))

cv2.imshow("Flattened Image", warped)

# Lưu kết quả
output_path = "/home/vpslocal/flattened_output.png"
cv2.imwrite(output_path, warped)
print(f"\n✓ Đã lưu ảnh làm phẳng tại: {output_path}")

# Thêm tùy chọn: Cắt bỏ viền đen (nếu có)
print("\nNhấn 'c' để cắt bỏ viền đen, nhấn phím khác để thoát...")
key = cv2.waitKey(0)

if key == ord('c'):
    # Chuyển sang grayscale và tìm vùng không đen
    gray = cv2.cvtColor(warped, cv2.COLOR_BGR2GRAY)
    _, thresh = cv2.threshold(gray, 10, 255, cv2.THRESH_BINARY)
    
    # Tìm contour của vùng không đen
    contours, _ = cv2.findContours(thresh, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    if len(contours) > 0:
        largest = max(contours, key=cv2.contourArea)
        x, y, w, h = cv2.boundingRect(largest)
        cropped = warped[y:y+h, x:x+w]
        
        cv2.imshow("Cropped Result", cropped)
        output_cropped = "/home/vpslocal/flattened_cropped.png"
        cv2.imwrite(output_cropped, cropped)
        print(f"✓ Đã lưu ảnh đã cắt tại: {output_cropped}")
        cv2.waitKey(0)

cv2.destroyAllWindows()