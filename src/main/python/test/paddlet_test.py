import os
from argparse import ArgumentParser
from warnings import filterwarnings

import cv2
import numpy

from nets import nn
from utils import util
import sys
import numpy as np

filterwarnings("ignore")
#/home/vpslocal/new_workspace/deepapp_bigdata_ml_traininghub/src/main/resources/models/paddlet
detection = nn.Detection('/home/vpslocal/new_workspace/deepapp_bigdata_ml_traininghub/src/main/resources/models/paddlet/detection.onnx')
recognition = nn.Recognition('/home/vpslocal/new_workspace/deepapp_bigdata_ml_traininghub/src/main/resources/models/paddlet/recognition.onnx')
classification = nn.Classification('/home/vpslocal/new_workspace/deepapp_bigdata_ml_traininghub/src/main/resources/models/paddlet/classification.onnx')


def main():
    parser = ArgumentParser()
    parser.add_argument('filepath', type=str, help='image file path')
    args = parser.parse_args()

    frame = cv2.imread(args.filepath)
    image = frame.copy()

    cv2.cvtColor(frame, cv2.COLOR_BGR2RGB, frame)  # inplace

    points = detection(frame)
    points = util.sort_polygon(list(points))

    # draw detected polygon
    for point in points:
        point = numpy.array(point, dtype=numpy.int32)
        cv2.polylines(image,
                      [point], True,
                      (0, 255, 0), 2)

    cropped_images = [util.crop_image(frame, x) for x in points]
    cropped_images, angles = classification(cropped_images)
    results, confidences = recognition(cropped_images)

    # draw recognized text
    for i, result in enumerate(results):
        point = points[i]
        x, y, w, h = cv2.boundingRect(point)
        image = cv2.putText(image, result, (int(x), int(y - 2)), cv2.FONT_HERSHEY_SIMPLEX,
                            0.4, (200, 200, 0), 1, cv2.LINE_AA)
    ##cv2.imwrite(os.path.basename(args.filepath), image)

    print(results)

    # Save each rotated cropped image as a separate file named after its recognized text
    for i, (cropped, result) in enumerate(zip(cropped_images, results)):
        filename = f"{result}_{i}.png"
        cv2.imshow(filename, cropped)
      


if __name__ == '__main__':
    #/home/vpslocal/new_workspace/deepapp_bigdata_ml_traininghub/src/main/resources/models/yolo/giay_ra_vien/01HM00012252_300005_image_66.png
    sys.argv = ['paddlet_test.py', '/home/vpslocal/new_workspace/deepapp_bigdata_ml_traininghub/src/main/resources/models/yolo/giay_ra_vien/01HM00012252_300005_image_66.png']
    main()