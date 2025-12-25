#!/usr/bin/env python3
"""
Convert YOLOv8 .pt model to ONNX format
Usage: python convert_yolo_to_onnx.py <model.pt> [output.onnx]
"""

import sys
import os
from pathlib import Path

def convert_to_onnx(pt_path, onnx_path=None):
    """Convert YOLOv8 .pt to ONNX"""
    try:
        from ultralytics import YOLO

        # Load model
        print(f"Loading model: {pt_path}")
        model = YOLO(pt_path)

        # Generate output path if not provided
        if onnx_path is None:
            onnx_path = str(Path(pt_path).with_suffix('.onnx'))

        # Export to ONNX
        print(f"Exporting to ONNX: {onnx_path}")
        model.export(
            format='onnx',
            imgsz=640,  # Image size
            dynamic=False,  # Static shape for better C++ compatibility
            simplify=True,  # Simplify ONNX model
            opset=12  # ONNX opset version
        )

        # The export creates a file with same name but .onnx extension
        exported_path = str(Path(pt_path).with_suffix('.onnx'))

        # Move if different output path requested
        if exported_path != onnx_path:
            import shutil
            shutil.move(exported_path, onnx_path)

        print(f"✓ Successfully exported to: {onnx_path}")

        # Print model info
        file_size = os.path.getsize(onnx_path) / (1024 * 1024)
        print(f"  Model size: {file_size:.2f} MB")

        return onnx_path

    except ImportError:
        print("ERROR: ultralytics package not found!")
        print("Install with: pip install ultralytics")
        sys.exit(1)
    except Exception as e:
        print(f"ERROR: {e}")
        sys.exit(1)

def main():
    if len(sys.argv) < 2:
        print("Usage: python convert_yolo_to_onnx.py <model.pt> [output.onnx]")
        sys.exit(1)

    pt_path = sys.argv[1]
    onnx_path = sys.argv[2] if len(sys.argv) > 2 else None

    if not os.path.exists(pt_path):
        print(f"ERROR: Model file not found: {pt_path}")
        sys.exit(1)

    convert_to_onnx(pt_path, onnx_path)

if __name__ == "__main__":
    main()
