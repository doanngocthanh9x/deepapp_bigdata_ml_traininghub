"""
Enhanced OCR with RAG System Worker: AAA0_0202_W
Multi-document support with template management
"""

import json
import os
import base64
import shutil
import tempfile
import numpy as np
import cv2
import hashlib
from datetime import datetime
from typing import Dict, Any, List, Tuple, Optional
from difflib import SequenceMatcher

from com.deepapp.infrastructure.BaseWorker import BaseWorker
from com.deepapp.infrastructure.WorkerRegistry import register_worker
from com.deepapp.utils.path_utils import (
    get_paddle_model_path, 
    get_test_image_path,
    get_aaa0_0202_template_dir,
    get_aaa0_0202_rag_indices_dir
)

# RAG components
from sentence_transformers import SentenceTransformer
import faiss

# Document processor
import sys
sys.path.append('/root/deepapp/deepapp_main/src/main/python/test')
from RAGv2 import (
    DocumentProcessor, FieldRule, DocumentTemplate, 
    ExtractionStrategy, validate_date, format_date_transformer
)

# OCR components
from nets import nn
from utils import util
from vietocr.tool.predictor import Predictor
from vietocr.tool.config import Cfg
from PIL import Image


@register_worker("AAA0_0202_W")
class AAA0_0202_Worker(BaseWorker):
    """Enhanced OCR + RAG Worker with multi-document support"""

    def __init__(self):
        super().__init__("AAA0_0202_Worker")
        self.log("=== Enhanced OCR with RAG System Worker ===")
        
        # Configuration
        self.template_dir = get_aaa0_0202_template_dir()
        self.rag_index_dir = get_aaa0_0202_rag_indices_dir()
        self.data_dir = os.path.join(get_aaa0_0202_template_dir(), '..', 'data')
        self.processed_files_log = os.path.join(self.data_dir, 'processed_files.json')
        
        # Create directories
        os.makedirs(self.template_dir, exist_ok=True)
        os.makedirs(self.rag_index_dir, exist_ok=True)
        os.makedirs(self.data_dir, exist_ok=True)
        
        # Load processed files tracking
        self.processed_files = self._load_processed_files_log()
        
        # Initialize components
        self._initialize_ocr_models()
        self._initialize_rag_system()
        self._load_all_templates()
        
        self.log(f"✓ Worker initialized with {len(self.document_processor.templates)} templates")

    def _load_processed_files_log(self) -> Dict[str, Any]:
        """Load processed files log to prevent duplicates"""
        if os.path.exists(self.processed_files_log):
            try:
                with open(self.processed_files_log, 'r', encoding='utf-8') as f:
                    return json.load(f)
            except Exception as e:
                self.log(f"Warning: Could not load processed files log: {e}", "WARNING")
        return {}

    def _save_processed_files_log(self):
        """Save processed files log"""
        try:
            with open(self.processed_files_log, 'w', encoding='utf-8') as f:
                json.dump(self.processed_files, f, ensure_ascii=False, indent=2)
        except Exception as e:
            self.log(f"Error saving processed files log: {e}", "ERROR")

    def _calculate_file_hash(self, file_path: str) -> str:
        """Calculate SHA256 hash of file content"""
        try:
            with open(file_path, 'rb') as f:
                return hashlib.sha256(f.read()).hexdigest()
        except Exception as e:
            self.log(f"Error calculating file hash: {e}", "ERROR")
            return ""

    def _is_file_processed(self, file_hash: str, template_id: str = None) -> bool:
        """Check if file has been processed before"""
        if file_hash in self.processed_files:
            processed_info = self.processed_files[file_hash]
            if template_id and processed_info.get('template_id') == template_id:
                return True
            elif not template_id:
                return True
        return False

    def _log_processed_file(self, file_hash: str, template_id: str, 
                           ocr_text_count: int, extracted_fields_count: int,
                           ocr_texts: list = None, extracted_fields: dict = None, 
                           template_name: str = None, ocr_results: list = None):
        """Log processed file information with full results"""
        self.processed_files[file_hash] = {
            'template_id': template_id,
            'template_name': template_name or "Unknown",
            'processed_at': datetime.now().isoformat(),
            'ocr_text_count': ocr_text_count,
            'extracted_fields_count': extracted_fields_count,
            'ocr_texts': ocr_texts,
            'extracted_fields': extracted_fields,
            'ocr_results': ocr_results
        }
        self._save_processed_files_log()

    def _initialize_ocr_models(self):
        """Initialize OCR models"""
        try:
            self.detection = nn.Detection(get_paddle_model_path('detection'))
            self.classification = nn.Classification(get_paddle_model_path('classification'))
            
            config = Cfg.load_config_from_name('vgg_transformer')
            config['device'] = 'cpu'
            self.vietocr_predictor = Predictor(config)
            
            self.log("✓ OCR models initialized")
        except Exception as e:
            self.log(f"✗ OCR initialization failed: {e}", "ERROR")
            raise

    def _initialize_rag_system(self):
        """Initialize RAG components"""
        try:
            self.embedding_model = SentenceTransformer(
                "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
            )
            self.document_processor = DocumentProcessor()
            self.rag_indices = {}  # In-memory cache
            
            self.log("✓ RAG system initialized")
        except Exception as e:
            self.log(f"✗ RAG initialization failed: {e}", "ERROR")
            raise

    def _load_all_templates(self):
        """Load all templates from JSON files"""
        try:
            template_files = [f for f in os.listdir(self.template_dir) 
                            if f.endswith('.json')]
            
            if not template_files:
                self.log("No templates found - creating default discharge_summary")
                self._create_default_discharge_template()
                template_files = ['discharge_summary.json']
            
            loaded = 0
            for filename in template_files:
                try:
                    filepath = os.path.join(self.template_dir, filename)
                    self.document_processor.load_template_from_json(filepath)
                    loaded += 1
                    self.log(f"  ✓ Loaded: {filename}")
                except Exception as e:
                    self.log(f"  ✗ Failed to load {filename}: {e}", "ERROR")
            
            self.log(f"✓ Templates loaded: {loaded}/{len(template_files)}")
            
        except Exception as e:
            self.log(f"✗ Template loading failed: {e}", "ERROR")

    def _create_default_discharge_template(self):
        """Create default discharge summary template"""
        template_data = {
            "template_id": "discharge_summary",
            "template_name": "Giấy ra viện",
            "description": "Template cho giấy tóm tắt ra viện",
            "version": "1.0",
            "classifier_keywords": [
                "ra viện", "tóm tắt bệnh án", "giấy ra viện", "xuất viện"
            ],
            "rules": [
                {
                    "field_name": "họ_tên",
                    "display_name": "Họ và tên",
                    "keywords": ["họ tên", "họ và tên", "tên bệnh nhân"],
                    "strategy": "keyword_value",
                    "priority": 10,
                    "query_aliases": ["tên", "người bệnh", "bệnh nhân"]
                },
                {
                    "field_name": "tuổi",
                    "display_name": "Tuổi",
                    "keywords": ["tuổi:", "tuổi", "(tuổi"],
                    "strategy": "regex",
                    "patterns": [
                        r'\(Tuổi:\s*(\d+)\)',
                        r'(\d+)\s*tuổi',
                        r'tuổi[:\s]+(\d+)'
                    ],
                    "priority": 8,
                    "query_aliases": ["bao nhiêu tuổi", "mấy tuổi"]
                },
                {
                    "field_name": "chẩn_đoán",
                    "display_name": "Chẩn đoán",
                    "keywords": ["chẩn đoán", "chần đoán","chan doan","chân đoán","chấn đoán"],
                    "strategy": "multi_line",
                    "stop_keywords": ["phương pháp", "ghi chú", "ra viện lúc"],
                    "priority": 9,
                    "query_aliases": ["bệnh", "mắc bệnh", "bệnh gì"]
                },
                {
                    "field_name": "ngày_vào_viện",
                    "display_name": "Ngày vào viện",
                    "keywords": ["vào viện", "nhập viện"],
                    "strategy": "keyword_value",
                    "priority": 7,
                    "validator": "validate_date",
                    "transformer": "format_date_transformer",
                    "query_aliases": ["khi nào vào viện", "ngày nhập viện"]
                },
                {
                    "field_name": "ngày_ra_viện",
                    "display_name": "Ngày ra viện",
                    "keywords": ["ra viện", "xuất viện"],
                    "strategy": "keyword_value",
                    "priority": 7,
                    "validator": "validate_date",
                    "transformer": "format_date_transformer",
                    "query_aliases": ["khi nào ra viện", "ngày xuất viện"]
                }
            ]
        }
        
        filepath = os.path.join(self.template_dir, 'discharge_summary.json')
        with open(filepath, 'w', encoding='utf-8') as f:
            json.dump(template_data, f, ensure_ascii=False, indent=2)
        
        self.log(f"✓ Created default template: {filepath}")

    # ==================== Main Event Handler ====================
    
    def process_task(self, event_type: str, payload: str) -> str:
        """Process events with enhanced routing"""
        self.log(f"📨 Event: {event_type}")
        
        handlers = {
            # Document processing
            "process_document": self._process_document,
            "query_document": self._query_document,
            
            # Template management
            "list_templates": self._list_templates,
            "get_template": self._get_template,
            "create_template": self._create_template,
            "update_template": self._update_template,
            "delete_template": self._delete_template,
            "reload_templates": self._reload_templates,
            "validate_template": self._validate_template,
            
            # RAG management
            "list_indices": self._list_indices,
            "delete_index": self._delete_index,
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

    # ==================== Document Processing ====================
    
    def _process_document(self, payload: str) -> str:
        """Enhanced document processing with auto-detection"""
        try:
            data = json.loads(payload)
            image_path = data.get('image_path')
            image_base64 = data.get('image_base64')
            template_id = data.get('template_id')  # Optional
            query = data.get('query')
            save_index = data.get('save_index', True)
            
            self.log(f"📄 Processing: template={template_id or 'auto'}, query={bool(query)}")
            
            # Resolve image
            image_path = self._resolve_image_path(image_path, image_base64)
            if not image_path:
                return self.create_response("error", "Invalid image input")
            
            # Check for duplicate processing
            file_hash = self._calculate_file_hash(image_path)
            if file_hash and self._is_file_processed(file_hash, template_id):
                self.log(f"📋 File already processed (hash: {file_hash[:8]}...), loading cached results")
                cached_data = self.processed_files[file_hash]
                ocr_texts = cached_data.get('ocr_texts', [])
                extracted_fields = cached_data.get('extracted_fields', {})
                ocr_results = cached_data.get('ocr_results', [])
                
                # Rebuild RAG index if query is provided
                query_response = None
                if query and ocr_texts:
                    rag_index_key = f"doc_{hash(tuple(ocr_texts))}"
                    self.log(f"🔗 Rebuilding RAG index for cached data: {rag_index_key}")
                    self._build_rag_index(ocr_texts, rag_index_key)
                    query_response = self._hybrid_query(
                        query, extracted_fields, rag_index_key, ocr_results
                    )
                    self.log(f"  ✓ Cached query answer: {query_response['answer'][:50]}... "
                            f"(confidence: {query_response['confidence']})")
                
                # Return cached processing results
                return self.create_response("success", {
                    "document_type": cached_data.get('template_name', "Unknown"),
                    "template_id": cached_data['template_id'],
                    "ocr_text": ocr_texts,
                    "extracted_fields": extracted_fields,
                    "query_response": query_response,
                    "stats": {
                        "total_text_regions": cached_data.get('ocr_text_count', 0),
                        "fields_extracted": cached_data.get('extracted_fields_count', 0),
                        "total_fields": len(extracted_fields)
                    },
                    "cached": True,
                    "processed_at": cached_data['processed_at']
                })
            
            # OCR
            self.log("🔍 Performing OCR...")
            ocr_results = self._perform_ocr(image_path)
            if not ocr_results:
                return self.create_response("error", "No text detected")
            
            self.log(f"  ✓ Detected {len(ocr_results)} text regions")
            ocr_texts = [r['text'] for r in ocr_results]
            
            # Auto-detect template if not specified
            if not template_id:
                template_id = self.document_processor.detect_document_type(ocr_results)
                self.log(f"  🎯 Auto-detected: {template_id}")
            
            # Extract fields
            extracted_fields = {}
            template_name = "Unknown"
            if template_id in self.document_processor.templates:
                self.log(f"📋 Extracting fields with template: {template_id}")
                extracted_fields = self.document_processor.extract_fields(
                    ocr_results, template_id
                )
                template_name = self.document_processor.templates[template_id].template_name
                filled_fields = len([v for v in extracted_fields.values() if v])
                self.log(f"  ✓ Extracted {filled_fields}/{len(extracted_fields)} fields")
            else:
                self.log(f"  ⚠️  Template not found: {template_id}")
            
            # Build RAG index
            rag_index_key = f"doc_{hash(tuple(ocr_texts))}"
            if save_index or query:
                self.log(f"🔗 Building RAG index: {rag_index_key}")
                self._build_rag_index(ocr_texts, rag_index_key)
            
            # Process query
            query_response = None
            if query:
                self.log(f"❓ Processing query: {query}")
                query_response = self._hybrid_query(
                    query, extracted_fields, rag_index_key, ocr_results
                )
                self.log(f"  ✓ Answer: {query_response['answer'][:50]}... "
                        f"(confidence: {query_response['confidence']})")
            
            # Log processed file
            if file_hash:
                self._log_processed_file(
                    file_hash, 
                    template_id, 
                    len(ocr_texts), 
                    len([v for v in extracted_fields.values() if v]),
                    ocr_texts,
                    extracted_fields,
                    template_name,
                    ocr_results
                )
                self.log(f"📝 Logged processed file: {file_hash[:8]}...")
            
            # Send to NER training data collection (AAA0_0203)
            self._send_to_ner_training({
                'ocr_text': ocr_texts,
                'extracted_fields': extracted_fields,
                'template_id': template_id,
                'document_type': template_name,
                'stats': {
                    'total_text_regions': len(ocr_results),
                    'fields_extracted': len([v for v in extracted_fields.values() if v]),
                    'total_fields': len(extracted_fields)
                }
            })
            
            # Cleanup temp file
            if image_base64 and os.path.exists(image_path):
                os.unlink(image_path)
            
            return self.create_response("success", {
                "document_type": template_name,
                "template_id": template_id,
                "ocr_text": ocr_texts,
                "extracted_fields": extracted_fields,
                "query_response": query_response,
                "stats": {
                    "total_text_regions": len(ocr_results),
                    "fields_extracted": len([v for v in extracted_fields.values() if v]),
                    "total_fields": len(extracted_fields)
                }
            })
            
        except Exception as e:
            self.log(f"✗ Document processing error: {e}", "ERROR")
            import traceback
            self.log(traceback.format_exc(), "ERROR")
            return self.create_response("error", str(e))

    def _query_document(self, payload: str) -> str:
        """Query existing RAG index without OCR"""
        try:
            data = json.loads(payload)
            query = data.get('query')
            index_key = data.get('index_key')
            extracted_fields = data.get('extracted_fields', {})
            ocr_results = data.get('ocr_results', [])
            
            if not query:
                return self.create_response("error", "Query required")
            
            if not index_key or index_key not in self.rag_indices:
                return self.create_response("error", "Valid index_key required")
            
            self.log(f"❓ Querying index {index_key}: {query}")
            
            query_response = self._hybrid_query(
                query, extracted_fields, index_key, ocr_results
            )
            
            return self.create_response("success", {
                "query_response": query_response
            })
            
        except Exception as e:
            self.log(f"✗ Query error: {e}", "ERROR")
            return self.create_response("error", str(e))

    def _resolve_image_path(self, image_path: str, image_base64: str) -> Optional[str]:
        """Resolve image from path or base64"""
        if image_base64:
            try:
                image_data = base64.b64decode(image_base64)
                with tempfile.NamedTemporaryFile(suffix='.png', delete=False) as f:
                    f.write(image_data)
                    return f.name
            except Exception as e:
                self.log(f"✗ Base64 decode failed: {e}", "ERROR")
                return None
        
        if image_path:
            if not os.path.exists(image_path):
                image_path = get_test_image_path(image_path)
            return image_path if os.path.exists(image_path) else None
        
        return None

    def _hybrid_query(self, query: str, extracted_fields: Dict, 
                     rag_index_key: str, ocr_results: List[Dict]) -> Dict:
        """
        3-tier hybrid query strategy:
        1. Template-based field matching (HIGH confidence)
        2. RAG semantic search (MEDIUM confidence)
        3. Fuzzy keyword matching (LOW confidence)
        """
        
        # Tier 1: Template-based
        if extracted_fields:
            result = self.document_processor.query_field(query, extracted_fields)
            if result['answer'] and result['answer'] != 'Không tìm thấy':
                return {
                    'answer': result['answer'],
                    'confidence': result['confidence'],
                    'source': 'template_field',
                    'field': result.get('field'),
                    'rag_results': []
                }
        
        # Tier 2: RAG semantic search
        rag_results = self._search_rag(query, rag_index_key, top_k=5)
        if rag_results and rag_results[0]['score'] < 0.8:
            return {
                'answer': rag_results[0]['text'],
                'confidence': 'MEDIUM',
                'source': 'rag_semantic',
                'rag_results': rag_results[:3]
            }
        
        # Tier 3: Fuzzy keyword matching
        fuzzy_answer = self._fuzzy_keyword_search(query, ocr_results)
        if fuzzy_answer:
            return {
                'answer': fuzzy_answer,
                'confidence': 'LOW',
                'source': 'fuzzy_keyword',
                'rag_results': rag_results[:3] if rag_results else []
            }
        
        # No answer
        return {
            'answer': 'Không tìm thấy thông tin',
            'confidence': 'NONE',
            'source': 'not_found',
            'rag_results': rag_results[:3] if rag_results else []
        }

    def _fuzzy_keyword_search(self, query: str, ocr_results: List[Dict]) -> Optional[str]:
        """Fuzzy keyword matching as fallback"""
        query_lower = query.lower()
        best_match = None
        best_score = 0
        
        for result in ocr_results:
            text = result['text']
            similarity = SequenceMatcher(None, query_lower, text.lower()).ratio()
            if similarity > best_score and similarity > 0.3:
                best_score = similarity
                best_match = text
        
        return best_match

    # ==================== OCR ====================
    
    def _perform_ocr(self, image_path: str) -> List[Dict]:
        """Perform OCR with error handling"""
        try:
            frame = cv2.imread(image_path)
            if frame is None:
                self.log(f"✗ Failed to load image: {image_path}", "ERROR")
                return []
            
            rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            
            # Detect
            points = self.detection(rgb_frame)
            points = util.sort_polygon(list(points))
            
            # Crop
            cropped_images = [util.crop_image(rgb_frame, x) for x in points]
            
            # Classify
            cropped_images_rotated, angles = self.classification(cropped_images)
            
            # Recognize
            results = []
            for i, (cropped, angle) in enumerate(zip(cropped_images_rotated, angles)):
                pil_image = Image.fromarray(cropped)
                text = self.vietocr_predictor.predict(pil_image).strip()
                
                if text:  # Only add non-empty text
                    results.append({
                        'text': text,
                        'angle': angle,
                        'bbox': points[i] if i < len(points) else None,
                        'index': i
                    })
            
            return results
            
        except Exception as e:
            self.log(f"✗ OCR error: {e}", "ERROR")
            return []

    # ==================== RAG ====================
    
    def _build_rag_index(self, texts: List[str], index_key: str):
        """Build FAISS index"""
        try:
            embeddings = self.embedding_model.encode(texts, show_progress_bar=False)
            dimension = embeddings.shape[1]
            index = faiss.IndexFlatL2(dimension)
            index.add(np.array(embeddings).astype('float32'))
            
            self.rag_indices[index_key] = {
                'index': index,
                'texts': texts,
                'embeddings': embeddings
            }
            
            self.log(f"  ✓ RAG index built: {len(texts)} texts, dim={dimension}")
            
        except Exception as e:
            self.log(f"✗ RAG index error: {e}", "ERROR")

    def _search_rag(self, query: str, index_key: str, top_k: int = 5) -> List[Dict]:
        """Search RAG index"""
        try:
            if index_key not in self.rag_indices:
                return []
            
            rag_data = self.rag_indices[index_key]
            query_emb = self.embedding_model.encode([query], show_progress_bar=False)
            distances, indices = rag_data['index'].search(
                query_emb.astype('float32'), top_k
            )
            
            results = []
            for i, idx in enumerate(indices[0]):
                if idx < len(rag_data['texts']):
                    results.append({
                        'text': rag_data['texts'][idx],
                        'score': float(distances[0][i]),
                        'index': int(idx)
                    })
            
            return results
            
        except Exception as e:
            self.log(f"✗ RAG search error: {e}", "ERROR")
            return []

    # ==================== Template Management ====================
    
    def _list_templates(self, payload: str = None) -> str:
        """List all templates"""
        templates = []
        for tid, template in self.document_processor.templates.items():
            templates.append({
                'id': template.template_id,
                'name': template.template_name,
                'description': template.description,
                'version': template.version,
                'rules_count': len(template.rules),
                'classifier_keywords': template.classifier_keywords
            })
        
        return self.create_response("success", {
            "templates": templates,
            "count": len(templates)
        })

    def _get_template(self, payload: str) -> str:
        """Get template details"""
        data = json.loads(payload)
        template_id = data.get('template_id')
        
        if template_id not in self.document_processor.templates:
            return self.create_response("error", "Template not found")
        
        template = self.document_processor.templates[template_id]
        
        return self.create_response("success", {
            "template": {
                "id": template.template_id,
                "name": template.template_name,
                "description": template.description,
                "version": template.version,
                "rules": [
                    {
                        "field_name": r.field_name,
                        "display_name": r.display_name,
                        "keywords": r.keywords,
                        "strategy": r.strategy.value,
                        "priority": r.priority,
                        "query_aliases": r.query_aliases
                    }
                    for r in template.rules
                ],
                "classifier_keywords": template.classifier_keywords
            }
        })

    def _create_template(self, payload: str) -> str:
        """Create new template"""
        data = json.loads(payload)
        template_data = data.get('template')
        
        if not template_data:
            return self.create_response("error", "template data required")
        
        # Validate
        validation = self._validate_template_data(template_data)
        if not validation['valid']:
            return self.create_response("error", validation['error'])
        
        template_id = template_data['template_id']
        filepath = os.path.join(self.template_dir, f"{template_id}.json")
        
        if os.path.exists(filepath):
            return self.create_response("error", 
                "Template exists - use update_template")
        
        # Save
        with open(filepath, 'w', encoding='utf-8') as f:
            json.dump(template_data, f, ensure_ascii=False, indent=2)
        
        # Load
        self.document_processor.load_template_from_json(filepath)
        
        return self.create_response("success", {
            "message": "Template created",
            "template_id": template_id,
            "filepath": filepath
        })

    def _update_template(self, payload: str) -> str:
        """Update template"""
        data = json.loads(payload)
        template_data = data.get('template')
        template_id = template_data.get('template_id')
        
        filepath = os.path.join(self.template_dir, f"{template_id}.json")
        if not os.path.exists(filepath):
            return self.create_response("error", "Template not found")
        
        # Validate
        validation = self._validate_template_data(template_data)
        if not validation['valid']:
            return self.create_response("error", validation['error'])
        
        # Backup
        backup_path = f"{filepath}.backup"
        shutil.copy(filepath, backup_path)
        
        # Save
        with open(filepath, 'w', encoding='utf-8') as f:
            json.dump(template_data, f, ensure_ascii=False, indent=2)
        
        # Reload
        self.document_processor.load_template_from_json(filepath)
        
        return self.create_response("success", {
            "message": "Template updated",
            "template_id": template_id,
            "backup": backup_path
        })

    def _delete_template(self, payload: str) -> str:
        """Delete template"""
        data = json.loads(payload)
        template_id = data.get('template_id')
        
        if template_id not in self.document_processor.templates:
            return self.create_response("error", "Template not found")
        
        # Remove from memory
        del self.document_processor.templates[template_id]
        
        # Remove file
        filepath = os.path.join(self.template_dir, f"{template_id}.json")
        if os.path.exists(filepath):
            os.remove(filepath)
        
        return self.create_response("success", {
            "message": "Template deleted",
            "template_id": template_id
        })

    def _reload_templates(self, payload: str = None) -> str:
        """Reload all templates"""
        self.document_processor.templates.clear()
        self._load_all_templates()
        
        return self.create_response("success", {
            "message": "Templates reloaded",
            "count": len(self.document_processor.templates)
        })

    def _validate_template(self, payload: str) -> str:
        """Validate template structure"""
        data = json.loads(payload)
        template_data = data.get('template')
        
        validation = self._validate_template_data(template_data)
        
        return self.create_response("success" if validation['valid'] else "error", 
                                   validation)

    def _validate_template_data(self, template_data: Dict) -> Dict:
        """Validate template JSON"""
        required = ['template_id', 'template_name', 'description', 'rules']
        
        for field in required:
            if field not in template_data:
                return {'valid': False, 'error': f"Missing: {field}"}
        
        if not isinstance(template_data['rules'], list):
            return {'valid': False, 'error': "rules must be list"}
        
        for i, rule in enumerate(template_data['rules']):
            req_fields = ['field_name', 'display_name', 'keywords', 'strategy']
            for field in req_fields:
                if field not in rule:
                    return {'valid': False, 
                           'error': f"Rule {i} missing: {field}"}
        
        return {'valid': True, 'message': 'Template is valid'}

    # ==================== NER Training Data Forwarding ====================
    
    def _send_to_ner_training(self, training_data: Dict):
        """
        Send processed document data to AAA0_0203 NER training via REST API
        Uses new database-first architecture instead of file-based approach
        
        Creates ONE sample with ENTIRE document text (all OCR lines) and all annotations
        """
        try:
            self.log(f"🔄 Sending training data to AAA0_0203 API: {training_data.get('template_id', 'unknown')}")
            
            import uuid
            import requests
            from datetime import datetime
            
            template_id = training_data.get('template_id', 'default')
            
            # Get OCR texts and extracted fields
            ocr_texts = training_data.get('ocr_text', [])
            extracted_fields = training_data.get('extracted_fields', {})
            
            if not ocr_texts:
                self.log(f"⚠️ No OCR text available")
                return
            
            # Merge all OCR lines into ONE multiline text document
            # Keep line breaks to preserve document structure
            full_document_text = '\n'.join([text.strip() for text in ocr_texts if text and isinstance(text, str)])
            
            if not full_document_text.strip():
                self.log(f"⚠️ Document text is empty after merging")
                return
            
            self.log(f"📄 Full document text: {len(full_document_text)} chars, {len(ocr_texts)} lines")
            
            # Build map: field_value -> field_name for quick lookup
            field_values_map = {}
            for field_name, field_value in extracted_fields.items():
                if field_value and isinstance(field_value, str) and field_value.strip():
                    field_values_map[field_value.strip()] = field_name
            
            if not field_values_map:
                self.log(f"⚠️ No valid extracted fields to create annotations")
                # Still create sample with no annotations for review
            
            # Find ALL field values in the entire document text
            annotations = []
            for field_value, field_name in field_values_map.items():
                # Find all occurrences of this field value in the entire document
                start_pos = 0
                occurrences = 0
                while True:
                    pos = full_document_text.find(field_value, start_pos)
                    if pos == -1:
                        break
                    
                    annotations.append({
                        'start': pos,
                        'end': pos + len(field_value),
                        'text': field_value,
                        'label': self._map_field_to_entity_type(field_name)
                    })
                    
                    occurrences += 1
                    start_pos = pos + len(field_value)
                
                if occurrences > 0:
                    self.log(f"   ✓ Found '{field_name}' = '{field_value}' ({occurrences} occurrence(s))")
            
            # Sort annotations by start position
            annotations.sort(key=lambda x: x['start'])
            
            # Generate unique sample ID
            sample_id = f"{template_id}_{datetime.now().strftime('%Y%m%d_%H%M%S')}_{str(uuid.uuid4())[:8]}"
            
            # Create ONE sample with entire document
            sample = {
                'id': sample_id,
                'text': full_document_text,
                'annotations': annotations,
                'approved': False,  # Initially not approved, requires manual review
                'metadata': {
                    'total_lines': len(ocr_texts),
                    'annotation_count': len(annotations),
                    'extracted_fields_count': len(extracted_fields)
                }
            }
            
            samples = [sample]
            
            self.log(f"✨ Created 1 document sample: {len(annotations)} annotations, {len(full_document_text)} chars")
            
            # Send to AAA0_0203 REST API endpoint
            api_url = "http://localhost:8080/AA/A0/AAA0_0203/process"
            
            payload = {
                'event_type': 'add_training_samples',
                'payload': {
                    'template_id': template_id,
                    'samples': samples,
                    'source': 'AAA0_0202_OCR',
                    'timestamp': datetime.now().isoformat(),
                    'stats': training_data.get('stats', {})
                }
            }
            
            self.log(f"📤 Sending {len(samples)} samples to AAA0_0203 API")
            
            # Make HTTP POST request to Java service
            try:
                response = requests.post(
                    api_url,
                    json=payload,
                    headers={'Content-Type': 'application/json'},
                    timeout=30
                )
                
                if response.status_code == 200:
                    result = response.json()
                    self.log(f"✅ Successfully sent to AAA0_0203: {result.get('message', 'OK')}")
                    self.log(f"📊 Stats: {result.get('data', {})}")
                else:
                    self.log(f"⚠️ AAA0_0203 API returned status {response.status_code}: {response.text[:200]}", "WARNING")
                    
            except requests.exceptions.RequestException as e:
                self.log(f"⚠️ HTTP request to AAA0_0203 failed: {e}", "ERROR")
                # Fall back to file-based approach for backwards compatibility
                self._send_to_ner_training_legacy(training_data)
                
        except Exception as e:
            self.log(f"⚠️ Failed to forward to NER training: {e}", "ERROR")
            import traceback
            self.log(f"⚠️ Traceback: {traceback.format_exc()}", "ERROR")
    
    def _map_field_to_entity_type(self, field_name: str) -> str:
        """Map extracted field name to NER entity type"""
        field_mapping = {
            'họ_tên': 'PERSON',
            'patient_name': 'PERSON',
            'ngày_sinh': 'DATE',
            'date_of_birth': 'DATE',
            'tuổi': 'AGE',
            'age': 'AGE',
            'chẩn_đoán': 'DIAGNOSIS',
            'diagnosis': 'DIAGNOSIS',
            'ngày_vào_viện': 'DATE',
            'admission_date': 'DATE',
            'ngày_ra_viện': 'DATE',
            'discharge_date': 'DATE',
            'giới_tính': 'GENDER',
            'gender': 'GENDER',
            'địa_chỉ': 'LOCATION',
            'address': 'LOCATION',
            'phương_pháp_điều_trị': 'TREATMENT',
            'treatment': 'TREATMENT',
            'mã_số_bhxh': 'ID',
            'insurance_id': 'ID'
        }
        return field_mapping.get(field_name.lower(), 'OTHER')
    
    def _send_to_ner_training_legacy(self, training_data: Dict):
        """
        Legacy file-based approach for backwards compatibility
        Only used if REST API is not available
        """
        try:
            self.log(f"💾 Using legacy file-based approach for AAA0_0203")
            
            import uuid
            from datetime import datetime
            
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            unique_id = str(uuid.uuid4())[:8]
            filename = f"training_data_{timestamp}_{unique_id}.json"
            
            # Use the same data directory as AAA0_0203 for consistency
            training_data_dir = os.path.join(os.path.dirname(__file__), '..', '..', '..', 'AAA0_0203', 'data')
            os.makedirs(training_data_dir, exist_ok=True)
            
            filepath = os.path.join(training_data_dir, filename)
            
            # Save training data to file
            with open(filepath, 'w', encoding='utf-8') as f:
                json.dump({
                    'processed_documents': [training_data],
                    'template_id': training_data.get('template_id'),
                    'timestamp': datetime.now().isoformat()
                }, f, ensure_ascii=False, indent=2)
            
            self.log(f"💾 Saved training data to file: {filepath}")
            
        except Exception as e:
            self.log(f"⚠️ Legacy file save also failed: {e}", "ERROR")

    # ==================== RAG Management ====================
    
    def _list_indices(self, payload: str = None) -> str:
        """List RAG indices"""
        indices = [
            {
                'key': key,
                'text_count': len(data['texts']),
                'dimension': data['embeddings'].shape[1]
            }
            for key, data in self.rag_indices.items()
        ]
        
        return self.create_response("success", {
            "indices": indices,
            "count": len(indices)
        })

    def _delete_index(self, payload: str) -> str:
        """Delete RAG index"""
        data = json.loads(payload)
        index_key = data.get('index_key')
        
        if index_key in self.rag_indices:
            del self.rag_indices[index_key]
            return self.create_response("success", {
                "message": "Index deleted",
                "index_key": index_key
            })
        
        return self.create_response("error", "Index not found")