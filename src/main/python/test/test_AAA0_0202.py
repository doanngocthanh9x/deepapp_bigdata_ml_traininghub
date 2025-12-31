#!/usr/bin/env python3
"""
Test script for AAA0_0202 - OCR with RAG System
"""

import sys
import os
sys.path.append('/root/deepapp/deepapp_main/src/main/python')

from com.deepapp.vn.io.AA.A0.AAA0_0202.worker.AAA0_0202_W import AAA0_0202_Worker
from com.deepapp.utils.path_utils import get_test_image_path

def test_rag_ocr_worker():
    """Test the RAG OCR worker"""
    print("Testing AAA0_0202 RAG OCR Worker...")

    worker = AAA0_0202_Worker()

    # Test process document
    test_image = get_test_image_path('images/01HM00012243_300005_image_74.png')
    payload = {
        "image_path": test_image,
        "template_id": "discharge_summary",
        "query": "Tên bệnh nhân là gì?",
        "save_index": True,
        "index_path": "test_rag_index"
    }

    print(f"Processing document: {test_image}")
    result = worker.process_task("process_document", json.dumps(payload))

    print("Result:", result)

    # Test query
    query_payload = {
        "query": "Bao nhiêu tuổi?",
        "index_path": "test_rag_index"
    }

    print("Testing query...")
    query_result = worker.process_task("query_document", json.dumps(query_payload))
    print("Query result:", query_result)

    # Test list templates
    templates_result = worker.process_task("list_templates", "{}")
    print("Templates:", templates_result)

if __name__ == "__main__":
    import json
    test_rag_ocr_worker()