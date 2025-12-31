"""
NER Training Data Preparation Worker: AAA0_0203
Collects processed documents from AAA0_0202 and prepares NER training data
"""

import json
import os
import re
import random
from typing import Dict, Any, List, Tuple, Optional
from datetime import datetime

from com.deepapp.infrastructure.BaseWorker import BaseWorker
from com.deepapp.infrastructure.WorkerRegistry import register_worker
from com.deepapp.infrastructure.ResponseCache import get_response_cache
from com.deepapp.utils.path_utils import (
    get_aaa0_0202_template_dir,
    get_aaa0_0203_data_dir,
    get_aaa0_0203_models_dir
)


@register_worker("AAA0_0203_W")
class AAA0_0203_Worker(BaseWorker):
    """NER Training Data Preparation Worker"""

    def __init__(self):
        super().__init__("AAA0_0203_Worker")
        self.log("=== NER Training Data Preparation Worker ===")

        # Configuration
        self.data_dir = get_aaa0_0203_data_dir()
        self.models_dir = get_aaa0_0203_models_dir()
        self.aaa0_0202_template_dir = get_aaa0_0202_template_dir()

        # Create directories
        os.makedirs(self.data_dir, exist_ok=True)
        os.makedirs(self.models_dir, exist_ok=True)

        # NER entity types mapping
        self.entity_types = {
            'họ_tên': 'PERSON',
            'ngày_sinh': 'DATE',
            'tuổi': 'AGE',
            'chẩn_đoán': 'DIAGNOSIS',
            'ngày_vào_viện': 'DATE',
            'ngày_ra_viện': 'DATE',
            'giới_tính': 'GENDER',
            'dân_tộc': 'ETHNICITY',
            'nghề_nghiệp': 'OCCUPATION',
            'địa_chỉ': 'LOCATION',
            'phương_pháp_điều_trị': 'TREATMENT',
            'ghi_chú': 'NOTE',
            'mã_số_bhxh': 'ID',
            'mã_y_tế': 'ID',
            'số_lưu_trữ': 'ID'
        }

        self.log("✓ Worker initialized")

    def process_task(self, event_type: str, payload: str) -> str:
        """Process NER training data preparation events"""
        self.log(f"📨 Event: {event_type}")

        handlers = {
            "collect_training_data": self._collect_training_data,
            "generate_ner_dataset": self._generate_ner_dataset,
            "list_training_data": self._list_training_data,
            "export_ner_format": self._export_ner_format,
            "validate_ner_data": self._validate_ner_data,
            "get_dataset_samples": self._get_dataset_samples,
            "update_sample_annotations": self._update_sample_annotations,
            "approve_sample": self._approve_sample,
            "reject_sample": self._reject_sample,
            "bulk_delete": self._bulk_delete_datasets,
            "bulk_export": self._bulk_export_datasets,
            "bulk_delete_samples": self._bulk_delete_samples,
            "start_training": self._start_training,
            "get_training_status": self._get_training_status,
            "list_trained_models": self._list_trained_models,
            "predict_ner": self._predict_ner,
            "evaluate_model": self._evaluate_model,
        }

        handler = handlers.get(event_type)
        if not handler:
            return self.create_response("error",
                f"Unknown event type: {event_type}")

        try:
            return handler(payload) if payload else handler()
        except Exception as e:
            self.log(f"✗ Error in {event_type}: {e}", "ERROR")
            import traceback
            self.log(traceback.format_exc(), "ERROR")
            return self.create_response("error", str(e))

    def _collect_training_data(self, payload: str) -> str:
        """Collect processed documents from AAA0_0202 for training"""
        self.log(f"📥 _collect_training_data called with payload: {len(payload) if payload else 0} chars")
        
        try:
            data = json.loads(payload)
            
            # Check if data is provided via file path (new approach) or directly in payload (legacy)
            if 'training_data_path' in data:
                # New approach: read from file path
                training_data_path = data.get('training_data_path')
                template_id = data.get('template_id')
                
                self.log(f"📂 Reading training data from file: {training_data_path}")
                
                if not os.path.exists(training_data_path):
                    return self.create_response("error", f"Training data file not found: {training_data_path}")
                
                with open(training_data_path, 'r', encoding='utf-8') as f:
                    file_data = json.load(f)
                
                source_data = file_data.get('processed_documents', [])
                # Use template_id from file if not provided in payload
                if not template_id:
                    template_id = file_data.get('template_id')
                    
                self.log(f"📂 Loaded {len(source_data)} documents from file for template: {template_id}")
            else:
                # Legacy approach: data embedded in payload
                source_data = data.get('processed_documents', [])
                template_id = data.get('template_id')
                self.log(f"📥 Processing {len(source_data)} documents for template: {template_id} (legacy mode)")
            
            if not source_data:
                self.log("⚠️ No processed documents provided")
                return self.create_response("error", "No processed documents provided")

            collected = 0
            training_samples = []

            for doc_data in source_data:
                self.log(f"📄 Converting document: {doc_data.get('document_type', 'unknown')}")
                sample = self._convert_to_training_sample(doc_data, template_id)
                if sample:
                    training_samples.append(sample)
                    collected += 1
                    self.log(f"✅ Converted to training sample #{collected}")
                else:
                    self.log(f"⚠️ Failed to convert document to training sample")

            self.log(f"📝 Collected {collected} training samples, saving to folder structure...")

            # Use folder structure instead of single file
            if not template_id:
                timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
                template_id = f"mixed_{timestamp}"

            folder_path = os.path.join(self.data_dir, template_id)
            samples_dir = os.path.join(folder_path, "samples")
            metadata_path = os.path.join(folder_path, "metadata.json")

            # Create directories if needed
            os.makedirs(samples_dir, exist_ok=True)

            # Load existing metadata or create new
            existing_total = 0
            approved_count = 0
            batch_size = 1000
            created_at = datetime.now().isoformat()

            if os.path.exists(metadata_path):
                try:
                    with open(metadata_path, 'r', encoding='utf-8') as f:
                        metadata = json.load(f)
                        existing_total = metadata.get('total_samples', 0)
                        approved_count = metadata.get('approved_count', 0)
                        batch_size = metadata.get('batch_size', 1000)
                        created_at = metadata.get('created_at', datetime.now().isoformat())
                        self.log(f"📂 Loaded metadata: {existing_total} existing samples")
                except Exception as e:
                    self.log(f"⚠️ Could not load metadata: {e}", "WARN")

            # Load existing samples from all batch files
            existing_samples = []
            batch_idx = 0
            while True:
                batch_file = os.path.join(samples_dir, f"batch_{batch_idx}.json")
                if not os.path.exists(batch_file):
                    break
                try:
                    with open(batch_file, 'r', encoding='utf-8') as f:
                        batch_data = json.load(f)
                        existing_samples.extend(batch_data.get('samples', []))
                    batch_idx += 1
                except Exception as e:
                    self.log(f"⚠️ Could not load batch {batch_idx}: {e}", "WARN")
                    break

            self.log(f"📂 Loaded {len(existing_samples)} existing samples from {batch_idx} batch files")

            # Add IDs to new samples and auto-approve if they have annotations
            for idx, sample in enumerate(training_samples):
                if 'id' not in sample:
                    sample['id'] = f"{template_id}_sample_{existing_total + idx}"

                # Auto-approve samples with annotations (ready for training)
                if sample.get('annotations') and len(sample['annotations']) > 0:
                    sample['approved'] = True
                    sample['status'] = 'approved'
                    self.log(f"✓ Auto-approved sample {sample['id']} ({len(sample['annotations'])} annotations)")

            # Combine existing and new samples
            all_samples = existing_samples + training_samples
            total_samples = len(all_samples)

            # Count approved samples
            approved_count = sum(1 for s in all_samples if s.get('approved', False))

            # Split into batch files
            num_batches = (total_samples + batch_size - 1) // batch_size

            # Write batch files
            for batch_idx in range(num_batches):
                start_idx = batch_idx * batch_size
                end_idx = min(start_idx + batch_size, total_samples)
                batch_samples = all_samples[start_idx:end_idx]

                batch_file = os.path.join(samples_dir, f"batch_{batch_idx}.json")
                with open(batch_file, 'w', encoding='utf-8') as f:
                    json.dump({
                        "batch_index": batch_idx,
                        "samples": batch_samples,
                        "count": len(batch_samples)
                    }, f, ensure_ascii=False, indent=2)

            # Write metadata
            with open(metadata_path, 'w', encoding='utf-8') as f:
                json.dump({
                    "template_id": template_id,
                    "total_samples": total_samples,
                    "approved_count": approved_count,
                    "created_at": created_at,
                    "last_updated": datetime.now().isoformat(),
                    "format_version": "2.0",
                    "batch_size": batch_size,
                    "num_batches": num_batches
                }, f, ensure_ascii=False, indent=2)

            self.log(f"✓ Appended {collected} new samples to folder {template_id} (total: {total_samples}, {num_batches} batches)")

            return self.create_response("success", {
                "message": f"Collected {collected} training samples (total: {total_samples})",
                "template_id": template_id,
                "folder_path": folder_path,
                "samples_count": total_samples,
                "new_samples": collected,
                "num_batches": num_batches
            })

        except Exception as e:
            self.log(f"✗ Error collecting training data: {e}", "ERROR")
            return self.create_response("error", str(e))

    def _convert_to_training_sample(self, doc_data: Dict, template_id: str = None) -> Optional[Dict]:
        """Convert processed document to training sample"""
        try:
            ocr_text = doc_data.get('ocr_text', [])
            extracted_fields = doc_data.get('extracted_fields', {})

            if not ocr_text or not extracted_fields:
                return None

            # Reconstruct full text - preserve line breaks
            full_text = '\n'.join(ocr_text)
            self.log(f"📄 Reconstructed text ({len(full_text)} chars): {repr(full_text[:200])}...")
            self.log(f"📋 Extracted fields: {list(extracted_fields.keys())}")

            # Create NER annotations
            annotations = []
            for field_name, field_value in extracted_fields.items():
                if field_value and field_value != 'Không tìm thấy':
                    entity_type = self.entity_types.get(field_name, 'O')
                    if entity_type != 'O':
                        # Find all occurrences of the field value in text
                        positions = self._find_all_positions(full_text, str(field_value))
                        self.log(f"🔍 Looking for '{field_value}' in text, found {len(positions)} positions")
                        for start, end in positions:
                            annotations.append({
                                "start": start,
                                "end": end,
                                "text": str(field_value),
                                "label": entity_type,
                                "field_name": field_name
                            })

            if not annotations:
                return None

            return {
                "text": full_text,
                "annotations": annotations,
                "template_id": template_id or doc_data.get('template_id'),
                "source": "AAA0_0202_processed"
            }

        except Exception as e:
            self.log(f"✗ Error converting sample: {e}", "ERROR")
            return None

    def _find_all_positions(self, text: str, substring: str) -> List[Tuple[int, int]]:
        """Find all positions of substring in text (case-insensitive, trimmed)"""
        positions = []
        text_lower = text.lower()
        substring_lower = substring.lower().strip()

        start = 0
        while True:
            pos = text_lower.find(substring_lower, start)
            if pos == -1:
                break

            # Check if the match is at word boundaries (not inside another word)
            if (pos == 0 or not text[pos-1].isalnum()) and \
               (pos + len(substring) == len(text) or not text[pos + len(substring)].isalnum()):
                positions.append((pos, pos + len(substring)))
            start = pos + 1

        return positions

    def _generate_ner_dataset(self, payload: str) -> str:
        """Generate NER dataset in standard format"""
        try:
            data = json.loads(payload)
            template_ids = data.get('template_ids', [])
            output_format = data.get('format', 'json')  # json, conll, spacy

            if not template_ids:
                # Get all available training data files
                training_files = [f for f in os.listdir(self.data_dir)
                                if f.endswith('_training_data.json')]
                template_ids = [f.replace('_training_data.json', '') for f in training_files]

            all_samples = []
            for template_id in template_ids:
                filepath = os.path.join(self.data_dir, f"{template_id}_training_data.json")
                if os.path.exists(filepath):
                    with open(filepath, 'r', encoding='utf-8') as f:
                        template_data = json.load(f)
                        all_samples.extend(template_data.get('samples', []))

            if not all_samples:
                return self.create_response("error", "No training samples found")

            # Generate dataset in requested format
            if output_format == 'json':
                dataset = self._generate_json_format(all_samples)
            elif output_format == 'conll':
                dataset = self._generate_conll_format(all_samples)
            elif output_format == 'spacy':
                dataset = self._generate_spacy_format(all_samples)
            else:
                return self.create_response("error", f"Unsupported format: {output_format}")

            # Save dataset
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            filename = f"ner_dataset_{output_format}_{timestamp}"
            if output_format == 'json':
                filename += '.json'
                filepath = os.path.join(self.data_dir, filename)
                with open(filepath, 'w', encoding='utf-8') as f:
                    json.dump(dataset, f, ensure_ascii=False, indent=2)
            else:
                filename += '.txt'
                filepath = os.path.join(self.data_dir, filename)
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(dataset)

            return self.create_response("success", {
                "message": f"Generated NER dataset in {output_format} format",
                "filepath": filepath,
                "samples_count": len(all_samples),
                "format": output_format
            })

        except Exception as e:
            self.log(f"✗ Error generating NER dataset: {e}", "ERROR")
            return self.create_response("error", str(e))
    def _get_dataset_samples(self, payload: str) -> str:
        """Get samples one-by-one to avoid gRPC buffer overflow - STREAM SAMPLES"""
        try:
            data = json.loads(payload)
            template_id = data.get('template_id')
            page = data.get('page', 0)  # Page number (0-indexed)
            page_size = data.get('page_size', 5)  # Default 5 samples per page (reduced to avoid gRPC overflow)
            # NEW: Support fetching single sample by index
            sample_index = data.get('sample_index', None)  # If provided, return only this sample

            if not template_id:
                return self.create_response("error", "template_id required")

            # Validate pagination parameters
            if page < 0:
                page = 0
            if page_size < 1 or page_size > 20:  # Max 20 samples per page (reduced from 100)
                page_size = 5

            # Check for new folder format first
            folder_path = os.path.join(self.data_dir, template_id)
            metadata_path = os.path.join(folder_path, "metadata.json")

            if os.path.exists(metadata_path):
                # New folder format - read and return actual samples (Python does the file reading)
                with open(metadata_path, 'r', encoding='utf-8') as f:
                    metadata = json.load(f)

                batch_size = metadata.get('batch_size', 1000)
                total_samples = metadata.get('total_samples', 0)

                # If requesting a specific sample by index
                if sample_index is not None:
                    if sample_index < 0 or sample_index >= total_samples:
                        return self.create_response("error", f"Sample index {sample_index} out of range (0-{total_samples-1})")

                    # Find which batch contains this sample
                    batch_idx = sample_index // batch_size
                    offset_in_batch = sample_index % batch_size

                    batch_file = os.path.join(folder_path, "samples", f"batch_{batch_idx}.json")
                    if not os.path.exists(batch_file):
                        return self.create_response("error", f"Batch file not found: {batch_file}")

                    with open(batch_file, 'r', encoding='utf-8') as f:
                        batch_data = json.load(f)
                        samples = batch_data.get('samples', [])

                        if offset_in_batch >= len(samples):
                            return self.create_response("error", f"Sample not found at index {sample_index}")

                        sample = samples[offset_in_batch]

                        return self.create_response("success", {
                            "template_id": template_id,
                            "sample": sample,  # Single sample only
                            "sample_index": sample_index,
                            "total_samples": total_samples
                        })

                # Otherwise, return a page of samples
                start_idx = page * page_size
                end_idx = min(start_idx + page_size, total_samples)

                # Load samples from batch files (with caching to avoid re-reading same batch)
                page_samples = []
                batch_cache = {}  # Cache batch data in memory during this request

                for idx in range(start_idx, end_idx):
                    batch_idx = idx // batch_size
                    offset_in_batch = idx % batch_size

                    # Check cache first
                    if batch_idx not in batch_cache:
                        batch_file = os.path.join(folder_path, "samples", f"batch_{batch_idx}.json")
                        if os.path.exists(batch_file):
                            with open(batch_file, 'r', encoding='utf-8') as f:
                                batch_data = json.load(f)
                                batch_cache[batch_idx] = batch_data.get('samples', [])
                        else:
                            batch_cache[batch_idx] = []

                    # Get sample from cached batch
                    cached_samples = batch_cache.get(batch_idx, [])
                    if offset_in_batch < len(cached_samples):
                        page_samples.append(cached_samples[offset_in_batch])

                # Prepare response data
                response_data = {
                    "template_id": template_id,
                    "samples": page_samples,
                    "total_samples": total_samples,
                    "page": page,
                    "page_size": page_size,
                    "start_index": start_idx,
                    "end_index": end_idx,
                    "returned_count": len(page_samples),
                    "total_pages": (total_samples + page_size - 1) // page_size,
                    "has_more": end_idx < total_samples,
                    "format": "folder"
                }

                # Store in SQLite cache to avoid gRPC buffer overflow
                cache = get_response_cache()
                response_id = cache.store_response(response_data, ttl_seconds=300)

                # Return only the response ID (tiny payload)
                return self.create_response("success", {
                    "cached": True,
                    "response_id": response_id,
                    "info": f"Response cached ({len(page_samples)} samples). Use response_id to retrieve."
                })

            else:
                # Old format - fallback for backwards compatibility
                filepath = os.path.join(self.data_dir, f"{template_id}_training_data.json")
                if not os.path.exists(filepath):
                    return self.create_response("error", f"Training data not found for {template_id}")

                with open(filepath, 'r', encoding='utf-8') as f:
                    training_data = json.load(f)

                all_samples = training_data.get('samples', [])
                total_samples = len(all_samples)

                # If requesting a specific sample
                if sample_index is not None:
                    if sample_index < 0 or sample_index >= total_samples:
                        return self.create_response("error", f"Sample index {sample_index} out of range")

                    return self.create_response("success", {
                        "template_id": template_id,
                        "sample": all_samples[sample_index],
                        "sample_index": sample_index,
                        "total_samples": total_samples
                    })

                # Return page of samples
                start_idx = page * page_size
                end_idx = min(start_idx + page_size, total_samples)
                page_samples = all_samples[start_idx:end_idx]

                response_data = {
                    "template_id": template_id,
                    "samples": page_samples,
                    "total_samples": total_samples,
                    "page": page,
                    "page_size": page_size,
                    "start_index": start_idx,
                    "end_index": end_idx,
                    "returned_count": len(page_samples),
                    "total_pages": (total_samples + page_size - 1) // page_size,
                    "has_more": end_idx < total_samples,
                    "format": "single_file"
                }

                # Store in SQLite cache
                cache = get_response_cache()
                response_id = cache.store_response(response_data, ttl_seconds=300)

                return self.create_response("success", {
                    "cached": True,
                    "response_id": response_id,
                    "info": f"Response cached ({len(page_samples)} samples). Use response_id to retrieve."
                })

        except Exception as e:
            self.log(f"✗ Error getting dataset samples: {e}", "ERROR")
            return self.create_response("error", str(e))

    def _update_sample_annotations(self, payload: str) -> str:
        """Update annotations for a specific sample"""
        try:
            data = json.loads(payload)
            template_id = data.get('template_id')
            sample_id = data.get('sample_id')
            text = data.get('text')
            annotations = data.get('annotations', [])

            if not template_id or not sample_id:
                return self.create_response("error", "template_id and sample_id required")

            # Load training data
            filepath = os.path.join(self.data_dir, f"{template_id}_training_data.json")
            if not os.path.exists(filepath):
                return self.create_response("error", f"Training data not found for {template_id}")

            with open(filepath, 'r', encoding='utf-8') as f:
                training_data = json.load(f)

            samples = training_data.get('samples', [])
            
            # Ensure all samples have IDs
            for idx, sample in enumerate(samples):
                if 'id' not in sample:
                    sample['id'] = f"{template_id}_sample_{idx}"

            updated = False

            # Find and update the sample
            for sample in samples:
                if sample.get('id') == sample_id:
                    sample['text'] = text
                    sample['annotations'] = annotations
                    updated = True
                    break

            if not updated:
                return self.create_response("error", f"Sample {sample_id} not found")

            # Save updated data
            with open(filepath, 'w', encoding='utf-8') as f:
                json.dump(training_data, f, ensure_ascii=False, indent=2)

            self.log(f"✓ Updated annotations for sample {sample_id}")
            return self.create_response("success", {
                "message": f"Updated annotations for sample {sample_id}",
                "template_id": template_id
            })

        except Exception as e:
            self.log(f"✗ Error updating sample annotations: {e}", "ERROR")
            return self.create_response("error", str(e))

    def _approve_sample(self, payload: str) -> str:
        """Mark a sample as approved"""
        try:
            data = json.loads(payload)
            template_id = data.get('template_id')
            sample_id = data.get('sample_id')

            if not template_id or not sample_id:
                return self.create_response("error", "template_id and sample_id required")

            # Load training data
            filepath = os.path.join(self.data_dir, f"{template_id}_training_data.json")
            if not os.path.exists(filepath):
                return self.create_response("error", f"Training data not found for {template_id}")

            with open(filepath, 'r', encoding='utf-8') as f:
                training_data = json.load(f)

            samples = training_data.get('samples', [])
            
            # Ensure all samples have IDs
            for idx, sample in enumerate(samples):
                if 'id' not in sample:
                    sample['id'] = f"{template_id}_sample_{idx}"

            updated = False

            # Find and approve the sample
            for sample in samples:
                if sample.get('id') == sample_id:
                    sample['approved'] = True
                    sample['status'] = 'approved'
                    updated = True
                    break

            if not updated:
                return self.create_response("error", f"Sample {sample_id} not found")

            # Save updated data
            with open(filepath, 'w', encoding='utf-8') as f:
                json.dump(training_data, f, ensure_ascii=False, indent=2)

            self.log(f"✓ Approved sample {sample_id}")
            return self.create_response("success", {
                "message": f"Approved sample {sample_id}",
                "template_id": template_id
            })

        except Exception as e:
            self.log(f"✗ Error approving sample: {e}", "ERROR")
            return self.create_response("error", str(e))

    def _reject_sample(self, payload: str) -> str:
        """Mark a sample as rejected"""
        try:
            data = json.loads(payload)
            template_id = data.get('template_id')
            sample_id = data.get('sample_id')

            if not template_id or not sample_id:
                return self.create_response("error", "template_id and sample_id required")

            # Load training data
            filepath = os.path.join(self.data_dir, f"{template_id}_training_data.json")
            if not os.path.exists(filepath):
                return self.create_response("error", f"Training data not found for {template_id}")

            with open(filepath, 'r', encoding='utf-8') as f:
                training_data = json.load(f)

            samples = training_data.get('samples', [])
            
            # Ensure all samples have IDs
            for idx, sample in enumerate(samples):
                if 'id' not in sample:
                    sample['id'] = f"{template_id}_sample_{idx}"

            updated = False

            # Find and reject the sample
            for sample in samples:
                if sample.get('id') == sample_id:
                    sample['approved'] = False
                    sample['status'] = 'rejected'
                    updated = True
                    break

            if not updated:
                return self.create_response("error", f"Sample {sample_id} not found")

            # Save updated data
            with open(filepath, 'w', encoding='utf-8') as f:
                json.dump(training_data, f, ensure_ascii=False, indent=2)

            self.log(f"✓ Rejected sample {sample_id}")
            return self.create_response("success", {
                "message": f"Rejected sample {sample_id}",
                "template_id": template_id
            })

        except Exception as e:
            self.log(f"✗ Error rejecting sample: {e}", "ERROR")
            return self.create_response("error", str(e))
    def _generate_json_format(self, samples: List[Dict]) -> Dict:
        """Generate JSON format NER dataset"""
        return {
            "dataset": samples,
            "metadata": {
                "total_samples": len(samples),
                "entity_types": list(set(self.entity_types.values())),
                "created_at": datetime.now().isoformat(),
                "format": "json"
            }
        }

    def _generate_conll_format(self, samples: List[Dict]) -> str:
        """Generate CoNLL format NER dataset"""
        lines = []
        for sample in samples:
            text = sample['text']
            annotations = sample.get('annotations', [])

            # Sort annotations by start position
            annotations.sort(key=lambda x: x['start'])

            # Tokenize and tag
            words = text.split()
            current_pos = 0
            annotation_idx = 0

            for word in words:
                word_start = text.find(word, current_pos)
                word_end = word_start + len(word)

                # Find if word is part of an annotation
                label = 'O'
                for ann in annotations:
                    if ann['start'] <= word_start < ann['end']:
                        label = f"B-{ann['label']}" if word_start == ann['start'] else f"I-{ann['label']}"
                        break

                lines.append(f"{word} {label}")
                current_pos = word_end

            lines.append("")  # Empty line between sentences

        return "\n".join(lines)

    def _generate_spacy_format(self, samples: List[Dict]) -> str:
        """Generate spaCy format NER dataset"""
        # This would be more complex for actual spaCy training
        # For now, return JSON format that can be converted to spaCy
        return json.dumps(self._generate_json_format(samples), ensure_ascii=False, indent=2)

    def _list_training_data_old(self, payload: str = None) -> str:
        """DEPRECATED - Old simplified version"""
        try:
            # Very simple response for debugging
            return '{"status":"success","data":{"training_data":[],"count":0}}'

        except Exception as e:
            return '{"error":"Failed"}'

    def _calculate_entity_statistics(self, training_data: List[Dict]) -> Dict[str, Any]:
        """Calculate entity statistics from training data"""
        try:
            # Define medical entity types with Vietnamese labels
            medical_entity_types = {
                'PERSON': {'label': 'Tên người bệnh', 'count': 0},
                'HOSPITAL': {'label': 'Tên bệnh viện', 'count': 0},
                'DATE': {'label': 'Ngày tháng', 'count': 0},
                'AGE': {'label': 'Tuổi', 'count': 0},
                'GENDER': {'label': 'Giới tính', 'count': 0},
                'DIAGNOSIS': {'label': 'Chẩn đoán', 'count': 0},
                'TREATMENT': {'label': 'Phương pháp điều trị', 'count': 0},
                'ADDRESS': {'label': 'Địa chỉ', 'count': 0},
                'ID_NUMBER': {'label': 'Số CMND/CCCD', 'count': 0},
                'INSURANCE': {'label': 'BHXH/BHYT', 'count': 0},
                'OCCUPATION': {'label': 'Nghề nghiệp', 'count': 0},
                'ETHNICITY': {'label': 'Dân tộc', 'count': 0},
                'DOCUMENT_TYPE': {'label': 'Loại giấy tờ', 'count': 0}
            }

            total_samples = 0
            total_entities = 0

            # Count entities from each training file
            for dataset in training_data:
                filepath = dataset.get('filepath')
                if filepath and os.path.exists(filepath):
                    try:
                        with open(filepath, 'r', encoding='utf-8') as f:
                            data = json.load(f)
                            samples = data.get('samples', [])
                            total_samples += len(samples)

                            for sample in samples:
                                annotations = sample.get('annotations', [])
                                for annotation in annotations:
                                    entity_type = annotation.get('label', 'UNKNOWN')
                                    if entity_type in medical_entity_types:
                                        medical_entity_types[entity_type]['count'] += 1
                                        total_entities += 1
                    except Exception as e:
                        self.log(f"✗ Error reading {filepath} for statistics: {e}", "ERROR")

            # Format statistics for display
            statistics = []
            for entity_type, info in medical_entity_types.items():
                statistics.append({
                    'entity_type': entity_type,
                    'vietnamese_label': info['label'],
                    'count': info['count']
                })

            return {
                'total_samples': total_samples,
                'total_entities': total_entities,
                'entity_breakdown': statistics
            }

        except Exception as e:
            self.log(f"✗ Error calculating entity statistics: {e}", "ERROR")
            return {
                'total_samples': 0,
                'total_entities': 0,
                'entity_breakdown': []
            }

    def _export_ner_format(self, payload: str) -> str:
        """Export training data in NER format for specific template"""
        try:
            data = json.loads(payload)
            template_id = data.get('template_id')
            format_type = data.get('format', 'vietnamese_ner')

            if not template_id:
                return self.create_response("error", "template_id required")

            # Load training data
            filepath = os.path.join(self.data_dir, f"{template_id}_training_data.json")
            if not os.path.exists(filepath):
                return self.create_response("error", f"Training data not found for {template_id}")

            with open(filepath, 'r', encoding='utf-8') as f:
                training_data = json.load(f)

            samples = training_data.get('samples', [])

            if format_type == 'vietnamese_ner':
                ner_data = self._export_vietnamese_ner_format(samples, template_id)
            else:
                return self.create_response("error", f"Unsupported format: {format_type}")

            # Save export
            export_filename = f"{template_id}_ner_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
            export_filepath = os.path.join(self.data_dir, export_filename)

            with open(export_filepath, 'w', encoding='utf-8') as f:
                json.dump(ner_data, f, ensure_ascii=False, indent=2)

            return self.create_response("success", {
                "message": f"Exported NER data for {template_id}",
                "filepath": export_filepath,
                "samples_count": len(samples),
                "format": format_type
            })

        except Exception as e:
            self.log(f"✗ Error exporting NER format: {e}", "ERROR")
            return self.create_response("error", str(e))

    def _export_vietnamese_ner_format(self, samples: List[Dict], template_id: str) -> Dict:
        """Export in Vietnamese NER training format"""
        ner_samples = []

        for sample in samples:
            text = sample['text']
            annotations = sample.get('annotations', [])

            # Create BIO format tags for Vietnamese
            tokens = self._tokenize_vietnamese(text)
            tags = ['O'] * len(tokens)

            # Apply entity tags
            for ann in annotations:
                entity_text = ann['text']
                entity_label = ann['label']

                # Find token positions for this entity
                entity_tokens = self._tokenize_vietnamese(entity_text)
                entity_positions = self._find_entity_positions(tokens, entity_tokens)

                for start_idx, end_idx in entity_positions:
                    if start_idx < len(tags):
                        tags[start_idx] = f"B-{entity_label}"
                        for i in range(start_idx + 1, min(end_idx, len(tags))):
                            tags[i] = f"I-{entity_label}"

            ner_samples.append({
                "tokens": tokens,
                "tags": tags,
                "text": text,
                "template_id": template_id
            })

        return {
            "template_id": template_id,
            "ner_samples": ner_samples,
            "metadata": {
                "total_samples": len(ner_samples),
                "entity_types": list(set(self.entity_types.values())),
                "created_at": datetime.now().isoformat(),
                "format": "vietnamese_ner_bio"
            }
        }

    def _tokenize_vietnamese(self, text: str) -> List[str]:
        """Simple Vietnamese tokenization (can be improved with proper tokenizer)"""
        # Basic tokenization - split by spaces and punctuation
        import re
        tokens = re.findall(r'\w+|[^\w\s]', text)
        return tokens

    def _find_entity_positions(self, tokens: List[str], entity_tokens: List[str]) -> List[Tuple[int, int]]:
        """Find positions of entity tokens in the full token list"""
        positions = []
        entity_len = len(entity_tokens)

        for i in range(len(tokens) - entity_len + 1):
            if tokens[i:i + entity_len] == entity_tokens:
                positions.append((i, i + entity_len))

        return positions

    def _validate_ner_data(self, payload: str) -> str:
        """Validate NER training data"""
        try:
            data = json.loads(payload)
            template_id = data.get('template_id')

            if not template_id:
                return self.create_response("error", "template_id required")

            # Load training data
            filepath = os.path.join(self.data_dir, f"{template_id}_training_data.json")
            if not os.path.exists(filepath):
                return self.create_response("error", f"Training data not found for {template_id}")

            with open(filepath, 'r', encoding='utf-8') as f:
                training_data = json.load(f)

            samples = training_data.get('samples', [])

            # Validation checks
            validation_results = {
                "total_samples": len(samples),
                "samples_with_annotations": 0,
                "total_annotations": 0,
                "entity_type_counts": {},
                "issues": []
            }

            for i, sample in enumerate(samples):
                annotations = sample.get('annotations', [])
                if annotations:
                    validation_results["samples_with_annotations"] += 1
                    validation_results["total_annotations"] += len(annotations)

                    for ann in annotations:
                        label = ann.get('label', 'UNKNOWN')
                        validation_results["entity_type_counts"][label] = \
                            validation_results["entity_type_counts"].get(label, 0) + 1

                # Check for issues
                text = sample.get('text', '')
                if not text.strip():
                    validation_results["issues"].append(f"Sample {i}: Empty text")

                for ann in annotations:
                    ann_text = ann.get('text', '')
                    start = ann.get('start', -1)
                    end = ann.get('end', -1)

                    if start < 0 or end < 0 or end <= start:
                        validation_results["issues"].append(f"Sample {i}: Invalid annotation positions")

                    if ann_text not in text:
                        validation_results["issues"].append(f"Sample {i}: Annotation text not found in original text")

            return self.create_response("success", {
                "template_id": template_id,
                "validation": validation_results
            })

        except Exception as e:
            self.log(f"✗ Error validating NER data: {e}", "ERROR")

    def _bulk_delete_datasets(self, payload: str) -> str:
        """Bulk delete multiple datasets"""
        try:
            data = json.loads(payload)
            template_ids = data.get('template_ids', [])

            self.log(f"🗑️ Bulk deleting {len(template_ids)} datasets: {template_ids}")

            deleted_count = 0
            errors = []

            for template_id in template_ids:
                try:
                    dataset_file = os.path.join(self.data_dir, f"{template_id}.json")
                    if os.path.exists(dataset_file):
                        os.remove(dataset_file)
                        deleted_count += 1
                        self.log(f"  ✓ Deleted: {template_id}")
                    else:
                        errors.append(f"Dataset {template_id} not found")
                        self.log(f"  ⚠️ Not found: {template_id}")
                except Exception as e:
                    errors.append(f"Failed to delete {template_id}: {str(e)}")
                    self.log(f"  ✗ Failed to delete {template_id}: {e}", "ERROR")

            return self.create_response("success", {
                "deleted_count": deleted_count,
                "total_requested": len(template_ids),
                "errors": errors
            })

        except Exception as e:
            self.log(f"✗ Bulk delete failed: {e}", "ERROR")
            return self.create_response("error", str(e))

    def _bulk_export_datasets(self, payload: str) -> str:
        """Bulk export multiple datasets"""
        try:
            data = json.loads(payload)
            template_ids = data.get('template_ids', [])
            format_type = data.get('format', 'json')

            self.log(f"📤 Bulk exporting {len(template_ids)} datasets in {format_type} format")

            combined_data = {
                "export_info": {
                    "timestamp": datetime.now().isoformat(),
                    "format": format_type,
                    "datasets_count": len(template_ids),
                    "template_ids": template_ids
                },
                "datasets": {}
            }

            for template_id in template_ids:
                try:
                    dataset_file = os.path.join(self.data_dir, f"{template_id}.json")
                    if os.path.exists(dataset_file):
                        with open(dataset_file, 'r', encoding='utf-8') as f:
                            dataset_data = json.load(f)
                        combined_data["datasets"][template_id] = dataset_data
                        self.log(f"  ✓ Loaded: {template_id}")
                    else:
                        self.log(f"  ⚠️ Not found: {template_id}")
                except Exception as e:
                    self.log(f"  ✗ Failed to load {template_id}: {e}", "ERROR")

            # Convert to requested format
            if format_type == 'json':
                content = json.dumps(combined_data, ensure_ascii=False, indent=2)
            else:
                # Default to JSON for now
                content = json.dumps(combined_data, ensure_ascii=False, indent=2)

            return self.create_response("success", {
                "content": content,
                "format": format_type,
                "datasets_exported": len(combined_data["datasets"])
            })

        except Exception as e:
            self.log(f"✗ Bulk export failed: {e}", "ERROR")
            return self.create_response("error", str(e))

    def _bulk_delete_samples(self, payload: str) -> str:
        """Bulk delete samples within a specific dataset"""
        try:
            self.log(f"🔍 Payload received: {payload}")
            data = json.loads(payload)
            template_id = data.get('template_id', '')
            sample_ids = data.get('sample_ids', [])

            self.log(f"🗑️ Bulk deleting {len(sample_ids)} samples from dataset: {template_id}")
            self.log(f"📋 Sample IDs to delete: {sample_ids}")

            dataset_file = os.path.join(self.data_dir, f"{template_id}_training_data.json")

            if not os.path.exists(dataset_file):
                self.log(f"❌ Dataset file not found: {dataset_file}")
                return self.create_response("error", f"Dataset {template_id} not found")

            # Load existing dataset
            with open(dataset_file, 'r', encoding='utf-8') as f:
                dataset_data = json.load(f)

            original_samples = dataset_data.get('samples', [])
            
            # Ensure all samples have IDs before processing
            for idx, sample in enumerate(original_samples):
                if 'id' not in sample:
                    sample['id'] = f"{template_id}_sample_{idx}"
            
            self.log(f"📊 Original sample count: {len(original_samples)}")

            # Filter out samples to delete
            remaining_samples = []
            deleted_count = 0

            for i, sample in enumerate(original_samples):
                sample_id = sample.get('id')
                self.log(f"  Sample {i}: ID='{sample_id}', in_delete_list={sample_id in sample_ids}")
                if sample_id in sample_ids:
                    deleted_count += 1
                    self.log(f"  ✓ Deleted sample: {sample_id}")
                else:
                    remaining_samples.append(sample)

            # Update dataset
            dataset_data['samples'] = remaining_samples
            
            # Update metadata if it exists, otherwise create it
            if 'metadata' not in dataset_data:
                dataset_data['metadata'] = {}
            dataset_data['metadata']['sample_count'] = len(remaining_samples)
            dataset_data['metadata']['last_updated'] = datetime.now().isoformat()

            # Save updated dataset
            with open(dataset_file, 'w', encoding='utf-8') as f:
                json.dump(dataset_data, f, ensure_ascii=False, indent=2)

            self.log(f"  Updated sample count: {len(remaining_samples)}")

            return self.create_response("success", {
                "template_id": template_id,
                "deleted_count": deleted_count,
                "remaining_count": len(remaining_samples),
                "total_requested": len(sample_ids)
            })

        except Exception as e:
            self.log(f"✗ Bulk delete samples failed: {e}", "ERROR")
            return self.create_response("error", str(e))

    def _start_training(self, payload: str) -> str:
        """Start NER model training with spaCy + PhoBERT"""
        try:
            self.log("🚀 Starting NER model training")
            data = json.loads(payload)
            
            config = data.get('config', {})
            template_ids = data.get('template_ids', [])
            
            # Create training directory
            training_id = f"ner_training_{int(datetime.now().timestamp())}"
            training_dir = os.path.join(self.models_dir, training_id)
            os.makedirs(training_dir, exist_ok=True)
            
            self.log(f"📁 Training directory: {training_dir}")
            
            # Prepare training data
            self.log("📊 Preparing training data...")
            all_samples = []

            for template_id in template_ids:
                # Try folder structure first (new format)
                folder_path = os.path.join(self.data_dir, template_id)
                samples_dir = os.path.join(folder_path, "samples")

                if os.path.exists(samples_dir):
                    # New folder format - load from batch files
                    batch_idx = 0
                    while True:
                        batch_file = os.path.join(samples_dir, f"batch_{batch_idx}.json")
                        if not os.path.exists(batch_file):
                            break

                        with open(batch_file, 'r', encoding='utf-8') as f:
                            batch_data = json.load(f)
                            samples = batch_data.get('samples', [])
                            # Filter approved samples only
                            approved_samples = [s for s in samples if s.get('approved', False)]
                            all_samples.extend(approved_samples)

                        batch_idx += 1

                    self.log(f"  ✓ Loaded approved samples from {template_id} ({batch_idx} batches)")

                else:
                    # Fallback to old single file format
                    dataset_file = os.path.join(self.data_dir, f"{template_id}_training_data.json")
                    if os.path.exists(dataset_file):
                        with open(dataset_file, 'r', encoding='utf-8') as f:
                            dataset_data = json.load(f)
                            samples = dataset_data.get('samples', [])
                            # Filter approved samples only
                            approved_samples = [s for s in samples if s.get('approved', False)]
                            all_samples.extend(approved_samples)
                            self.log(f"  ✓ Loaded {len(approved_samples)} approved samples from {template_id} (old format)")

            self.log(f"📊 Total approved samples loaded: {len(all_samples)}")
            
            if len(all_samples) < 50:
                return self.create_response("error", f"Need at least 50 approved samples, got {len(all_samples)}")
            
            # Split train/dev
            random.shuffle(all_samples)
            split_idx = int(len(all_samples) * 0.8)
            train_samples = all_samples[:split_idx]
            dev_samples = all_samples[split_idx:]
            
            # Create spaCy format
            self.log("🔄 Converting to spaCy format...")
            train_data = self._convert_to_spacy_format(train_samples)
            dev_data = self._convert_to_spacy_format(dev_samples)
            
            # Save training data
            corpus_dir = os.path.join(training_dir, "corpus")
            os.makedirs(corpus_dir, exist_ok=True)
            
            self._create_docbin(train_data, os.path.join(corpus_dir, "train.spacy"))
            self._create_docbin(dev_data, os.path.join(corpus_dir, "dev.spacy"))
            
            # Create config file
            config_path = os.path.join(training_dir, "config.cfg")
            self._create_training_config(config_path, config)
            
            # Start training in background
            self.log("🎯 Starting spaCy training...")
            import subprocess
            import threading
            
            def run_training():
                try:
                    cmd = [
                        "python", "-m", "spacy", "train",
                        config_path,
                        "--output", os.path.join(training_dir, "output"),
                        "--paths.train", os.path.join(corpus_dir, "train.spacy"),
                        "--paths.dev", os.path.join(corpus_dir, "dev.spacy"),
                        "--gpu-id", "0" if config.get('use_gpu', False) else "cpu"
                    ]
                    
                    self.log(f"Running command: {' '.join(cmd)}")
                    result = subprocess.run(cmd, capture_output=True, text=True, cwd=training_dir)
                    
                    # Save training results
                    with open(os.path.join(training_dir, "training.log"), 'w', encoding='utf-8') as f:
                        f.write(f"STDOUT:\n{result.stdout}\n\nSTDERR:\n{result.stderr}")
                    
                    if result.returncode == 0:
                        self.log("✅ Training completed successfully")
                        # Save model metadata
                        self._save_model_metadata(training_id, config, len(train_samples), len(dev_samples))
                    else:
                        self.log(f"❌ Training failed with code {result.returncode}", "ERROR")
                        
                except Exception as e:
                    self.log(f"Training error: {e}", "ERROR")
            
            # Start training in background thread
            training_thread = threading.Thread(target=run_training, daemon=True)
            training_thread.start()
            
            return self.create_response("success", {
                "training_id": training_id,
                "status": "started",
                "message": f"Training started with {len(train_samples)} train samples, {len(dev_samples)} dev samples",
                "training_dir": training_dir
            })
            
        except Exception as e:
            self.log(f"✗ Training start failed: {e}", "ERROR")
            return self.create_response("error", str(e))

    def _convert_to_spacy_format(self, samples):
        """Convert samples to spaCy training format"""
        training_data = []
        
        for sample in samples:
            text = sample['text']
            entities = []
            
            for ann in sample.get('annotations', []):
                entities.append((ann['start'], ann['end'], ann['label']))
            
            training_data.append((text, {"entities": entities}))
        
        return training_data

    def _create_docbin(self, training_data, output_path):
        """Create DocBin for spaCy training"""
        import spacy
        from spacy.tokens import DocBin
        
        nlp = spacy.blank("vi")
        db = DocBin()
        
        for text, annotations in training_data:
            doc = nlp.make_doc(text)
            ents = []
            
            for start, end, label in annotations["entities"]:
                span = doc.char_span(start, end, label=label, alignment_mode="contract")
                if span is not None:
                    ents.append(span)
            
            doc.ents = ents
            db.add(doc)
        
        db.to_disk(output_path)
        self.log(f"✓ Created DocBin with {len(training_data)} documents: {output_path}")

    def _create_training_config(self, config_path, config):
        """Create spaCy training config file"""
        config_content = f'''[paths]
train = ./corpus/train.spacy
dev = ./corpus/dev.spacy
vectors = null
init_tok2vec = null

[system]
gpu_allocator = pytorch
seed = 42

[nlp]
lang = vi
pipeline = [transformer, ner]
batch_size = {config.get('batch_size', 128)}

[components.transformer]
factory = transformer
max_batch_items = 4096

[components.transformer.model]
@architectures = spacy-transformers.TransformerModel.v3
name = vinai/phobert-base

[components.ner]
factory = ner

[components.ner.model]
@architectures = spacy.TransitionBasedParser.v2

[corpora.train]
@readers = spacy.Corpus.v1
path = ${{paths.train}}

[corpora.dev]
@readers = spacy.Corpus.v1
path = ${{paths.dev}}

[training]
train_corpus = corpora.train
dev_corpus = corpora.dev
seed = ${{system.seed}}
epochs = {config.get('epochs', 50)}
patience = 5000
max_steps = 20000
eval_frequency = 200

[training.optimizer]
@optimizers = Adam.v1
learn_rate = {config.get('learning_rate', 0.00005)}

[training.score_weights]
ents_f = 1.0
ents_p = 0.0
ents_r = 0.0
'''
        
        with open(config_path, 'w', encoding='utf-8') as f:
            f.write(config_content)
        
        self.log(f"✓ Created training config: {config_path}")

    def _save_model_metadata(self, training_id, config, train_count, dev_count):
        """Save model metadata"""
        metadata = {
            "training_id": training_id,
            "created_at": datetime.now().isoformat(),
            "config": config,
            "train_samples": train_count,
            "dev_samples": dev_count,
            "status": "completed",
            "model_path": f"{training_id}/output/model-best"
        }
        
        metadata_file = os.path.join(self.models_dir, f"{training_id}_metadata.json")
        with open(metadata_file, 'w', encoding='utf-8') as f:
            json.dump(metadata, f, ensure_ascii=False, indent=2)

    def _get_training_status(self, payload: str) -> str:
        """Get training status"""
        try:
            data = json.loads(payload)
            training_id = data.get('training_id')
            
            training_dir = os.path.join(self.models_dir, training_id)
            metadata_file = os.path.join(self.models_dir, f"{training_id}_metadata.json")
            
            if os.path.exists(metadata_file):
                with open(metadata_file, 'r', encoding='utf-8') as f:
                    metadata = json.load(f)
                return self.create_response("success", metadata)
            
            # Check if training is still running
            log_file = os.path.join(training_dir, "training.log")
            if os.path.exists(log_file):
                with open(log_file, 'r', encoding='utf-8') as f:
                    log_content = f.read()
                
                # Simple status detection
                if "Training completed" in log_content:
                    status = "completed"
                elif "Error" in log_content.lower():
                    status = "failed"
                else:
                    status = "running"
                
                return self.create_response("success", {
                    "training_id": training_id,
                    "status": status,
                    "log_preview": log_content[-500:]  # Last 500 chars
                })
            
            return self.create_response("error", "Training not found")
            
        except Exception as e:
            self.log(f"✗ Get training status failed: {e}", "ERROR")
            return self.create_response("error", str(e))

    def _list_trained_models(self, payload: str) -> str:
        """List all trained NER models"""
        try:
            models = []
            
            # Scan models directory for metadata files
            for filename in os.listdir(self.models_dir):
                if filename.endswith("_metadata.json"):
                    metadata_file = os.path.join(self.models_dir, filename)
                    try:
                        with open(metadata_file, 'r', encoding='utf-8') as f:
                            metadata = json.load(f)
                            models.append(metadata)
                    except Exception as e:
                        self.log(f"Error reading metadata {filename}: {e}", "WARN")
            
            # Sort by creation date
            models.sort(key=lambda x: x.get('created_at', ''), reverse=True)
            
            return self.create_response("success", {
                "models": models,
                "total": len(models)
            })
            
        except Exception as e:
            self.log(f"✗ List models failed: {e}", "ERROR")
            return self.create_response("error", str(e))

    def _predict_ner(self, payload: str) -> str:
        """Predict NER entities in text using trained model"""
        try:
            data = json.loads(payload)
            model_path = data.get('model_path')
            text = data.get('text', '')
            
            if not model_path or not text:
                return self.create_response("error", "model_path and text required")
            
            # Load model
            full_model_path = os.path.join(self.models_dir, model_path)
            if not os.path.exists(full_model_path):
                return self.create_response("error", f"Model not found: {model_path}")
            
            import spacy
            nlp = spacy.load(full_model_path)
            
            # Predict
            doc = nlp(text)
            
            entities = []
            for ent in doc.ents:
                entities.append({
                    "text": ent.text,
                    "label": ent.label_,
                    "start": ent.start_char,
                    "end": ent.end_char,
                    "confidence": getattr(ent, '_.confidence', None)
                })
            
            return self.create_response("success", {
                "text": text,
                "entities": entities,
                "entity_count": len(entities)
            })
            
        except Exception as e:
            self.log(f"✗ NER prediction failed: {e}", "ERROR")
            return self.create_response("error", str(e))

    def _evaluate_model(self, payload: str) -> str:
        """Evaluate trained NER model"""
        try:
            data = json.loads(payload)
            model_path = data.get('model_path')
            test_data = data.get('test_data', [])
            
            if not model_path:
                return self.create_response("error", "model_path required")
            
            # Load model
            full_model_path = os.path.join(self.models_dir, model_path)
            if not os.path.exists(full_model_path):
                return self.create_response("error", f"Model not found: {model_path}")
            
            import spacy
            nlp = spacy.load(full_model_path)
            
            # Evaluate
            from spacy.training import Example
            
            examples = []
            for text, annotations in test_data:
                doc = nlp.make_doc(text)
                example = Example.from_dict(doc, annotations)
                examples.append(example)
            
            # Calculate metrics
            scores = nlp.evaluate(examples)
            
            return self.create_response("success", {
                "model_path": model_path,
                "scores": scores,
                "test_samples": len(examples)
            })
            
        except Exception as e:
            self.log(f"✗ Model evaluation failed: {e}", "ERROR")
            return self.create_response("error", str(e))

    def _list_trained_models(self, payload: str = None) -> str:
        """List all trained NER models"""
        try:
            models = []

            # Check if models directory exists
            if not os.path.exists(self.models_dir):
                self.log(f"⚠️ Models directory does not exist: {self.models_dir}", "WARN")
                return self.create_response("success", {
                    "models": [],
                    "count": 0,
                    "message": "Models directory not found"
                })

            # List model directories with safety limits
            try:
                items = os.listdir(self.models_dir)[:20]  # Limit to 20 items

                for item in items:
                    model_path = os.path.join(self.models_dir, item)
                    if os.path.isdir(model_path):
                        # Check if it's a spaCy model (has config.cfg)
                        config_path = os.path.join(model_path, "config.cfg")
                        if os.path.exists(config_path):
                            try:
                                model_info = {
                                    "name": item,
                                    "path": model_path,
                                    "type": "spacy",
                                    "created": datetime.fromtimestamp(os.path.getctime(model_path)).isoformat()
                                }

                                models.append(model_info)
                            except Exception as e:
                                self.log(f"⚠️ Error processing model {item}: {e}", "WARN")
                                continue

            except Exception as e:
                self.log(f"⚠️ Error listing models directory: {e}", "WARN")
                return self.create_response("success", {
                    "models": [],
                    "count": 0,
                    "message": f"Error accessing models directory: {str(e)}"
                })

            return self.create_response("success", {
                "models": models,
                "count": len(models)
            })

        except Exception as e:
            self.log(f"✗ Error listing trained models: {e}", "ERROR")
            return self.create_response("error", f"Failed to list trained models: {str(e)}")

    def _list_training_data(self, payload: str = None) -> str:
        """List all training data - returns only metadata paths for Java to read"""
        try:
            training_datasets = []

            # Check if data directory exists
            if not os.path.exists(self.data_dir):
                self.log(f"⚠️ Data directory does not exist: {self.data_dir}", "WARN")
                return self.create_response("success", {
                    "data_dir": self.data_dir,
                    "training_datasets": [],
                    "count": 0,
                    "total_approved_samples": 0,
                    "can_train": False
                })

            # List dataset folders
            try:
                total_approved = 0

                # Check for both old format files and new format folders
                items = os.listdir(self.data_dir)

                for item in items:
                    item_path = os.path.join(self.data_dir, item)

                    # New format: folder with metadata.json
                    if os.path.isdir(item_path):
                        metadata_path = os.path.join(item_path, "metadata.json")
                        if os.path.exists(metadata_path):
                            # Just return the metadata file path - Java will read it
                            training_datasets.append({
                                "template_id": item,
                                "metadata_path": metadata_path,
                                "is_folder_format": True
                            })

                    # Old format: single JSON file (for backwards compatibility)
                    elif item.endswith('_training_data.json'):
                        template_id = item.replace('_training_data.json', '')
                        training_datasets.append({
                            "template_id": template_id,
                            "metadata_path": item_path,  # Java will read and extract metadata
                            "is_folder_format": False
                        })

            except Exception as e:
                self.log(f"⚠️ Error listing directory: {e}", "WARN")
                return self.create_response("success", {
                    "data_dir": self.data_dir,
                    "training_datasets": [],
                    "count": 0,
                    "total_approved_samples": 0,
                    "can_train": False,
                    "message": f"Error accessing data directory: {str(e)}"
                })

            # Return only metadata paths - Java reads metadata files
            response = self.create_response("success", {
                "data_dir": self.data_dir,
                "training_datasets": training_datasets,
                "count": len(training_datasets)
            })

            self.log(f"✓ Returning {len(training_datasets)} metadata paths to Java")
            return response

        except Exception as e:
            self.log(f"✗ Error listing training data: {e}", "ERROR")
            return self.create_response("error", f"Failed to list training data: {str(e)}")

    def can_handle(self, event_type: str) -> bool:
        """Check if this worker can handle the given event type"""
        supported_events = [
            "collect_training_data",
            "generate_ner_dataset",
            "list_training_data",
            "export_ner_format",
            "validate_ner_data",
            "get_dataset_samples",
            "update_sample_annotations",
            "approve_sample",
            "reject_sample",
            "bulk_delete",
            "bulk_export",
            "bulk_delete_samples",
            "start_training",
            "get_training_status",
            "list_trained_models",
            "predict_ner",
            "evaluate_model"
        ]
        return event_type in supported_events

    def process_task(self, event_type: str, payload: str) -> str:
        """Process a task and return result"""
        try:
            self.log(f"🔄 Processing task: {event_type}")
            
            if event_type == "list_trained_models":
                return self._list_trained_models(payload)
            elif event_type == "list_training_data":
                return self._list_training_data(payload)
            elif event_type == "get_dataset_samples":
                return self._get_dataset_samples(payload)
            elif event_type == "update_sample_annotations":
                return self._update_sample_annotations(payload)
            elif event_type == "approve_sample":
                return self._approve_sample(payload)
            elif event_type == "reject_sample":
                return self._reject_sample(payload)
            elif event_type == "bulk_delete":
                return self._bulk_delete_datasets(payload)
            elif event_type == "bulk_export":
                return self._bulk_export_datasets(payload)
            elif event_type == "bulk_delete_samples":
                return self._bulk_delete_samples(payload)
            elif event_type == "collect_training_data":
                return self._collect_training_data(payload)
            elif event_type == "generate_ner_dataset":
                return self._generate_ner_dataset(payload)
            elif event_type == "export_ner_format":
                return self._export_ner_format(payload)
            elif event_type == "validate_ner_data":
                return self._validate_ner_data(payload)
            elif event_type == "start_training":
                return self._start_training(payload)
            elif event_type == "get_training_status":
                return self._get_training_status(payload)
            elif event_type == "train_ner_model":
                return self._train_ner_model(payload)
            elif event_type == "predict_ner":
                return self._predict_ner(payload)
            elif event_type == "evaluate_model":
                return self._evaluate_model(payload)
            else:
                return self.create_response("error", f"Unsupported event type: {event_type}")
                
        except Exception as e:
            self.log(f"✗ Error processing task {event_type}: {e}", "ERROR")
            return self.create_response("error", str(e))
