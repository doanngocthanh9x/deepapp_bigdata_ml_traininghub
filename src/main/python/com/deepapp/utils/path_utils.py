"""
Path utilities for the DeepApp project
Provides functions to get consistent paths regardless of current working directory
"""
import os
import sys

def get_project_base_path():
    """
    Get the base path of the project (/root/deepapp/deepapp_main/src)
    Works from any subdirectory

    Returns:
        str: Absolute path to the src directory
    """
    # Get the directory of the calling file
    frame = sys._getframe(1)
    caller_file = frame.f_code.co_filename
    current_file_dir = os.path.dirname(os.path.abspath(caller_file))

    # Navigate up to find the 'src' directory
    path_parts = current_file_dir.split(os.sep)

    # Find the index of 'src' in the path
    try:
        src_index = path_parts.index('src')
        # Reconstruct path up to 'src'
        base_path = os.sep.join(path_parts[:src_index + 1])
        return base_path
    except ValueError:
        # Fallback: assume we're in src/some/deep/path
        # Go up until we find 'src'
        current_dir = current_file_dir
        while current_dir != os.sep:
            if os.path.basename(current_dir) == 'src':
                return current_dir
            current_dir = os.path.dirname(current_dir)

        # Final fallback: go up 3 levels from current file
        return os.path.dirname(os.path.dirname(os.path.dirname(current_file_dir)))

def get_resources_path():
    """Get path to resources directory"""
    return os.path.join(get_project_base_path(), 'main', 'resources')

def get_models_path():
    """Get path to models directory"""
    return os.path.join(get_resources_path(), 'models')

def get_vietocr_model_path():
    """Get path to VietOCR model"""
    return os.path.join(get_models_path(), 'vietocr_oonx', 'vgg_transformer.pth')

def get_yolo_model_path():
    """Get path to YOLO model"""
    return os.path.join(get_models_path(), 'yolo', 'giay_ra_vien', 'best.onnx')

def get_paddle_model_path(model_type):
    """Get path to PaddleOCR model"""
    return os.path.join(get_models_path(), 'paddlet', f'{model_type}.onnx')

def get_test_data_path():
    """Get path to test data directory"""
    return os.path.join(get_project_base_path(), 'main', 'python', 'test')

def get_test_image_path(filename):
    """Get path to a test image file"""
    return os.path.join(get_test_data_path(), filename)
PROJECT_BASE = get_project_base_path()
RESOURCES_PATH = get_resources_path()
MODELS_PATH = get_models_path()

if __name__ == "__main__":
    print(f"Project base path: {PROJECT_BASE}")
    print(f"Resources path: {RESOURCES_PATH}")
    print(f"Models path: {MODELS_PATH}")
    print(f"VietOCR model: {get_vietocr_model_path()}")
    print(f"YOLO model: {get_yolo_model_path()}")
    print(f"Paddle detection: {get_paddle_model_path('detection')}")