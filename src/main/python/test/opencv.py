import cv2
import numpy as np

#initializing
img = cv2.imread(r"new_workspace/deepapp_bigdata_ml_traininghub/src/main/resources/models/yolo/giay_ra_vien/01HM00012874_300005_image_94.png")
img = cv2.resize(img, (int(480*2), int(640*2)))
# write code here
GrayImg = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
BlurredFrame = cv2.GaussianBlur(GrayImg, (5, 5), 1)
CannyFrame = cv2.Canny(BlurredFrame, 190, 190)
contours, _ = cv2.findContours(CannyFrame, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
ContourFrame = img.copy()
ContourFrame = cv2.drawContours(ContourFrame, contours, -1, (255, 0, 255), 4)
CornerFrame = img.copy()
maxArea = 0
biggest = []
for i in contours :
    area = cv2.contourArea(i)
    if area > 500 :
        peri = cv2.arcLength(i, True)
        edges = cv2.approxPolyDP(i, 0.02*peri, True)
        if area > maxArea and len(edges) == 4 :
            biggest = edges
            maxArea = area
if len(biggest) != 0 :
    CornerFrame = cv2.drawContours(CornerFrame, biggest, -1, (255, 0, 255), 25)
    
    # Perspective transform to straighten the paper
    biggest = biggest.reshape((4, 2))
    rect = np.zeros((4, 2), dtype="float32")
    
    # Sort points: top-left, top-right, bottom-right, bottom-left
    s = biggest.sum(axis=1)
    rect[0] = biggest[np.argmin(s)]
    rect[2] = biggest[np.argmax(s)]
    
    diff = np.diff(biggest, axis=1)
    rect[1] = biggest[np.argmin(diff)]
    rect[3] = biggest[np.argmax(diff)]
    
    # Compute width and height
    (tl, tr, br, bl) = rect
    widthA = np.sqrt(((br[0] - bl[0]) ** 2) + ((br[1] - bl[1]) ** 2))
    widthB = np.sqrt(((tr[0] - tl[0]) ** 2) + ((tr[1] - tl[1]) ** 2))
    maxWidth = max(int(widthA), int(widthB))
    
    heightA = np.sqrt(((tr[0] - br[0]) ** 2) + ((tr[1] - br[1]) ** 2))
    heightB = np.sqrt(((tl[0] - bl[0]) ** 2) + ((tl[1] - bl[1]) ** 2))
    maxHeight = max(int(heightA), int(heightB))
    
    dst = np.array([
        [0, 0],
        [maxWidth - 1, 0],
        [maxWidth - 1, maxHeight - 1],
        [0, maxHeight - 1]], dtype="float32")
    
    M = cv2.getPerspectiveTransform(rect, dst)
    warped = cv2.warpPerspective(img, M, (maxWidth, maxHeight))
    
    # Resize warped image for display
    warped_resized = cv2.resize(warped, (480, 640))
    cv2.imshow("Warped Paper", warped_resized)
# resizing
img = cv2.resize(img, (480, 640))
GrayImg = cv2.resize(GrayImg, (480, 640))
BlurredFrame = cv2.resize(BlurredFrame, (480, 640))
CannyFrame = cv2.resize(CannyFrame, (480, 640))
ContourFrame = cv2.resize(ContourFrame, (480, 640))
CornerFrame = cv2.resize(CornerFrame, (480, 640))
#displaying
cv2.imshow("img", img)
cv2.imshow("GrayImg", GrayImg)
cv2.imshow("BlurredFrame", BlurredFrame)
cv2.imshow("CannyFrame", CannyFrame)
cv2.imshow("ContourFrame", ContourFrame)
cv2.imshow("CornerFrame", CornerFrame)
cv2.waitKey(0)