#!/usr/bin/env python3
"""
Batch process discharge papers from a folder to quickly generate training data
"""

import os
import json
import sys
import time
from pathlib import Path

# Add project to path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from com.deepapp.vn.io.AA.A0.AAA0_0202.worker.AAA0_0202_W import AAA0_0202_Worker

def batch_process_folder(folder_path: str, template_id: str = "discharge_summary"):
    """Process all image files in a folder"""

    # Initialize worker
    print("🔧 Initializing AAA0_0202 OCR+RAG worker...")
    worker = AAA0_0202_Worker()
    print("✅ Worker initialized\n")

    # Get all image files
    image_extensions = ['.jpg', '.jpeg', '.png', '.pdf']
    image_files = []

    for ext in image_extensions:
        image_files.extend(Path(folder_path).glob(f"*{ext}"))
        image_files.extend(Path(folder_path).glob(f"*{ext.upper()}"))

    image_files = sorted(image_files)

    if not image_files:
        print(f"❌ No image files found in {folder_path}")
        return

    print(f"📂 Found {len(image_files)} image files")
    print(f"🎯 Template: {template_id}\n")

    # Process each file
    success_count = 0
    error_count = 0

    for idx, image_file in enumerate(image_files, 1):
        print(f"[{idx}/{len(image_files)}] Processing: {image_file.name}")

        try:
            # Read image file
            with open(image_file, 'rb') as f:
                image_data = f.read()

            # Create payload
            payload = {
                'image_data': list(image_data),  # Convert bytes to list for JSON
                'template_id': template_id,
                'file_name': image_file.name
            }
            payload_json = json.dumps(payload)

            # Process with OCR+RAG
            result = worker.process_task("ocr_with_rag", payload_json)
            result_data = json.loads(result)

            if result_data.get('status') == 'success':
                data = result_data.get('data', {})
                fields = data.get('extracted_fields', {})
                field_count = len([v for v in fields.values() if v and v != 'Không tìm thấy'])

                print(f"  ✅ Success - Extracted {field_count} fields")
                success_count += 1
            else:
                error = result_data.get('data', 'Unknown error')
                print(f"  ❌ Error: {error}")
                error_count += 1

            # Small delay to avoid overwhelming system
            time.sleep(0.5)

        except Exception as e:
            print(f"  ❌ Exception: {e}")
            error_count += 1

    # Summary
    print(f"\n" + "="*60)
    print(f"📊 BATCH PROCESSING COMPLETE")
    print(f"="*60)
    print(f"✅ Successful: {success_count}")
    print(f"❌ Failed: {error_count}")
    print(f"📈 Total: {len(image_files)}")
    print(f"="*60)

    # Check training data
    data_dir = os.path.join(os.path.dirname(__file__), '..', 'com', 'deepapp', 'vn', 'io', 'AA', 'A0', 'AAA0_0203', 'data')
    metadata_path = os.path.join(data_dir, template_id, "metadata.json")

    if os.path.exists(metadata_path):
        with open(metadata_path, 'r', encoding='utf-8') as f:
            metadata = json.load(f)

        print(f"\n📊 Training Data Status:")
        print(f"  Total samples: {metadata.get('total_samples', 0)}")
        print(f"  Approved samples: {metadata.get('approved_count', 0)}")
        print(f"  Can train: {'✅ YES' if metadata.get('approved_count', 0) >= 50 else '❌ NO (need 50)'}")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python3 batch_process_discharge_papers.py <folder_path> [template_id]")
        print("\nExample:")
        print("  python3 batch_process_discharge_papers.py /path/to/discharge_papers")
        print("  python3 batch_process_discharge_papers.py /path/to/papers discharge_summary")
        sys.exit(1)

    folder_path = sys.argv[1]
    template_id = sys.argv[2] if len(sys.argv) > 2 else "discharge_summary"

    if not os.path.exists(folder_path):
        print(f"❌ Folder not found: {folder_path}")
        sys.exit(1)

    batch_process_folder(folder_path, template_id)
