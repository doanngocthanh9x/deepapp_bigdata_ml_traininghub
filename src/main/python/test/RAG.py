import cv2
import numpy as np
from PIL import Image
from vietocr.tool.predictor import Predictor
from vietocr.tool.config import Cfg
from nets import nn
from utils import util
from com.deepapp.utils.path_utils import get_paddle_model_path, get_test_image_path

# RAG components
from sentence_transformers import SentenceTransformer
import faiss
import json
import re
from typing import List, Dict, Tuple, Optional

class RAGSystem:
    """RAG system with context-aware search"""
    
    def __init__(self, model_name="sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"):
        print("Loading embedding model...")
        self.model = SentenceTransformer(model_name)
        self.index = None
        self.texts = []
        self.metadata = []
        
    def build_index(self, texts: List[str], metadata: List[Dict] = None):
        """Build FAISS index from texts"""
        print(f"Building index from {len(texts)} texts...")
        
        embeddings = self.model.encode(texts, batch_size=32, show_progress_bar=True)
        
        dimension = embeddings.shape[1]
        self.index = faiss.IndexFlatL2(dimension)
        self.index.add(np.array(embeddings).astype('float32'))
        
        self.texts = texts
        self.metadata = metadata if metadata else [{} for _ in texts]
        
        print(f"Index built: {self.index.ntotal} vectors, dim={dimension}")
        
    def search(self, query: str, top_k: int = 5) -> List[Tuple[str, float, Dict]]:
        """Search for relevant texts"""
        if self.index is None:
            raise ValueError("Index not built.")
        
        q_vec = self.model.encode([query])
        distances, indices = self.index.search(np.array(q_vec).astype('float32'), 
                                               min(top_k * 2, len(self.texts)))
        
        results = []
        for i, idx in enumerate(indices[0]):
            if idx < len(self.texts) and len(results) < top_k:
                results.append((
                    self.texts[idx],
                    float(distances[0][i]),
                    self.metadata[idx]
                ))
        
        return results
    
    def save_index(self, filepath: str):
        """Save index and data"""
        faiss.write_index(self.index, f"{filepath}.faiss")
        
        def convert_numpy(obj):
            if isinstance(obj, np.ndarray):
                return obj.tolist()
            elif isinstance(obj, (np.float32, np.float64)):
                return float(obj)
            elif isinstance(obj, (np.int32, np.int64)):
                return int(obj)
            elif isinstance(obj, dict):
                return {k: convert_numpy(v) for k, v in obj.items()}
            elif isinstance(obj, list):
                return [convert_numpy(item) for item in obj]
            else:
                return obj
        
        with open(f"{filepath}.json", 'w', encoding='utf-8') as f:
            json.dump({
                'texts': self.texts,
                'metadata': convert_numpy(self.metadata)
            }, f, ensure_ascii=False, indent=2)
        print(f"Index saved to {filepath}")


class MedicalOCRPipeline:
    """OCR + RAG with context-aware extraction"""
    
    def __init__(self):
        print("Loading OCR models...")
        self.detection = nn.Detection(get_paddle_model_path('detection'))
        self.classification = nn.Classification(get_paddle_model_path('classification'))
        
        config = Cfg.load_config_from_name('vgg_transformer')
        config['device'] = 'cpu'
        self.viet_predictor = Predictor(config)
        
        self.rag = RAGSystem()
        self.ocr_results = []  # Store for context-aware search
        
    def process_image(self, image_path: str) -> Tuple[List[Dict], np.ndarray]:
        """Process image and extract text"""
        frame = cv2.imread(image_path)
        rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        
        points = self.detection(rgb_frame)
        points = util.sort_polygon(list(points))
        
        cropped_images = [util.crop_image(rgb_frame, x) for x in points]
        cropped_images_rotated, angles = self.classification(cropped_images)
        
        results = []
        for i, (cropped, angle) in enumerate(zip(cropped_images_rotated, angles)):
            pil_image = Image.fromarray(cropped)
            text = self.viet_predictor.predict(pil_image).strip()
            
            if not text:
                continue
            
            angle_value = 0
            if isinstance(angle, (list, tuple)):
                angle_value = int(angle[0]) if angle[0] else 0
            elif isinstance(angle, (int, float, np.number)):
                angle_value = int(angle)
            
            results.append({
                'text': text,
                'angle': angle_value,
                'bbox': points[i].tolist(),
                'bbox_rect': cv2.boundingRect(points[i]),
                'index': i  # Store original index for context lookup
            })
            
            x, y, w, h = cv2.boundingRect(points[i])
            cv2.rectangle(frame, (x, y), (x+w, y+h), (0, 255, 0), 2)
            display_text = text[:40] + "..." if len(text) > 40 else text
            cv2.putText(frame, display_text, (x, y-5), 
                       cv2.FONT_HERSHEY_SIMPLEX, 0.4, (0, 0, 255), 1)
        
        return results, frame
    
    def build_rag_from_ocr(self, ocr_results: List[Dict]):
        """Build RAG index"""
        texts = [r['text'] for r in ocr_results]
        metadata = [{'angle': r['angle'], 'bbox': r['bbox'], 'index': r['index']} 
                   for r in ocr_results]
        self.rag.build_index(texts, metadata)
        
    def get_next_line(self, current_index: int) -> Optional[str]:
        """Get text from next line (for context-aware extraction)"""
        if current_index + 1 < len(self.ocr_results):
            return self.ocr_results[current_index + 1]['text']
        return None
    
    def extract_value_from_context(self, keyword_line: str, next_line: Optional[str] = None) -> Optional[str]:
        """Extract value from current line or next line"""
        # Try current line first (format: "Label: Value")
        if ':' in keyword_line:
            parts = keyword_line.split(':', 1)
            if len(parts) > 1:
                value = parts[1].strip()
                if value and len(value) > 1:  # Not empty
                    return value
        
        # If current line has no value, use next line
        if next_line and next_line.strip():
            return next_line.strip()
        
        return None
    
    def extract_medical_fields(self, ocr_results: List[Dict]) -> Dict[str, str]:
        """Extract fields with improved context awareness and fuzzy matching"""
        fields = {
            'họ_tên': None,
            'ngày_sinh': None,
            'giới_tính': None,
            'địa_chỉ': None,
            'chẩn_đoán': None,
            'bệnh_kèm': None,
            'mã_bệnh_nhân': None,
            'số_bhyt': None,
            'ngày_vào_viện': None,
            'ngày_ra_viện': None,
            'tuổi': None,
            'dân_tộc': None,
            'nghề_nghiệp': None
        }
        
        self.ocr_results = ocr_results
        
        for i, result in enumerate(ocr_results):
            text = result['text']
            text_lower = text.lower()
            next_line = self.get_next_line(i)
            
            # Họ tên
            if any(kw in text_lower for kw in ['họ tên', 'họ và tên', 'tên bệnh nhân', 'tên người bệnh']):
                value = self.extract_value_from_context(text, next_line)
                if value and not fields['họ_tên']:
                    fields['họ_tên'] = value
            
            # Ngày sinh - tìm dd/mm/yyyy
            if any(kw in text_lower for kw in ['ngày sinh', 'sinh:', 'năm sinh', 'ngày tháng năm sinh']):
                match = re.search(r'(\d{1,2}/\d{1,2}/\d{4})', text)
                if match:
                    fields['ngày_sinh'] = match.group(1)
                elif next_line:
                    match = re.search(r'(\d{1,2}/\d{1,2}/\d{4})', next_line)
                    if match:
                        fields['ngày_sinh'] = match.group(1)
            
            # Tuổi - handle separated lines
            if text_lower.strip() == 'tuổi:' or text_lower.strip() == 'tuổi':
                if next_line and next_line.strip().isdigit():
                    fields['tuổi'] = next_line.strip() + ' tuổi'
            elif 'tuổi' in text_lower and not fields['tuổi']:
                match = re.search(r'(\d+)\s*tuổi', text, re.IGNORECASE)
                if match:
                    fields['tuổi'] = match.group(1) + ' tuổi'
            
            # Giới tính
            if 'nam/nữ' in text_lower:
                if ': nữ' in text_lower or 'nữ' in text_lower.split('nam/nữ')[1][:15].lower():
                    fields['giới_tính'] = 'Nữ'
                elif ': nam' in text_lower or 'nam' in text_lower.split('nam/nữ')[1][:15].lower():
                    fields['giới_tính'] = 'Nam'
            
            # Dân tộc
            if 'dân tộc' in text_lower:
                value = self.extract_value_from_context(text, next_line)
                if value and not fields['dân_tộc']:
                    fields['dân_tộc'] = value
            
            # Nghề nghiệp
            if 'nghề nghiệp' in text_lower:
                value = self.extract_value_from_context(text, next_line)
                if value and not fields['nghề_nghiệp']:
                    fields['nghề_nghiệp'] = value
            
            # Địa chỉ
            if any(kw in text_lower for kw in ['địa chỉ', 'địa chi', 'nơi ở']):
                value = self.extract_value_from_context(text, next_line)
                if value and not fields['địa_chỉ']:
                    fields['địa_chỉ'] = value
            
            # Chẩn đoán - fuzzy match for OCR errors ("Chần đoán" instead of "Chẩn đoán")
            if any(kw in text_lower for kw in ['chẩn đoán', 'chần đoán', 'chuẩn đoán']):
                if 'bệnh kèm' not in text_lower:
                    value = self.extract_value_from_context(text, next_line)
                    if value and not fields['chẩn_đoán']:
                        # Check if next line continues the diagnosis
                        if next_line and ':' not in next_line and i + 1 < len(ocr_results):
                            next_next_line = self.get_next_line(i + 1)
                            if next_next_line and not any(kw in next_next_line.lower() for kw in ['phương pháp', 'ghi chú', 'ra viện']):
                                value = value + ' ' + next_next_line
                        fields['chẩn_đoán'] = value
            
            # Bệnh kèm
            if 'bệnh kèm' in text_lower:
                value = self.extract_value_from_context(text, next_line)
                if value and not fields['bệnh_kèm']:
                    fields['bệnh_kèm'] = value
            
            # Mã bệnh nhân (Mã Y tế)
            if 'mã y tế' in text_lower or 'mã ytế' in text_lower:
                match = re.search(r'mã y\s*tế:\s*(.+)', text, re.IGNORECASE)
                if match:
                    fields['mã_bệnh_nhân'] = match.group(1).strip()
            
            # Số BHYT - improved pattern matching
            if 'bhyt' in text_lower or 'bhxh' in text_lower:
                # Check current and next line for GD pattern
                combined_text = text
                if next_line:
                    combined_text += ' ' + next_line
                match = re.search(r'GD\s*\d+[\s\d/]+', combined_text, re.IGNORECASE)
                if match and not fields['số_bhyt']:
                    fields['số_bhyt'] = match.group(0).strip()
            elif re.match(r'^GD\s*\d', text_lower):
                # Direct GD number line
                if not fields['số_bhyt']:
                    # May span multiple lines
                    combined = text.strip()
                    if next_line and re.search(r'^\d+', next_line):
                        combined += ' ' + next_line.strip()
                    fields['số_bhyt'] = combined
            
            # Ngày vào viện
            if 'vào viện' in text_lower:
                # Try to find date in current or next line
                match = re.search(r'ngày\s+(\d{1,2})\s+tháng\s+(\d{1,2})\s+năm\s+(\d{4})', text, re.IGNORECASE)
                if match:
                    fields['ngày_vào_viện'] = f"{match.group(1)}/{match.group(2)}/{match.group(3)}"
                elif next_line:
                    match = re.search(r'ngày\s+(\d{1,2})\s+tháng\s+(\d{1,2})\s+năm\s+(\d{4})', next_line, re.IGNORECASE)
                    if match:
                        fields['ngày_vào_viện'] = f"{match.group(1)}/{match.group(2)}/{match.group(3)}"
            
            # Ngày ra viện
            if 'ra viện' in text_lower:
                match = re.search(r'(\d{1,2})\s+giờ.*?ngày\s+(\d{1,2})\s+tháng\s+(\d{1,2})\s+năm\s+(\d{4})', text, re.IGNORECASE)
                if match:
                    fields['ngày_ra_viện'] = f"{match.group(2)}/{match.group(3)}/{match.group(4)}"
                elif next_line:
                    match = re.search(r'(\d{1,2})\s+giờ.*?ngày\s+(\d{1,2})\s+tháng\s+(\d{1,2})\s+năm\s+(\d{4})', next_line, re.IGNORECASE)
                    if match:
                        fields['ngày_ra_viện'] = f"{match.group(2)}/{match.group(3)}/{match.group(4)}"
        
        return fields
    
    def smart_query(self, question: str, show_context: bool = True, top_k: int = 5) -> Dict:
        """Smart query with detailed RAG results"""
        question_lower = question.lower()
        
        # Map question to field
        field_map = {
            'họ tên': 'họ_tên', 'tên': 'họ_tên', 'người bệnh': 'họ_tên',
            'ngày sinh': 'ngày_sinh', 'sinh nhật': 'ngày_sinh', 'sinh': 'ngày_sinh',
            'giới tính': 'giới_tính', 'nam nữ': 'giới_tính',
            'tuổi': 'tuổi', 'bao nhiêu tuổi': 'tuổi',
            'dân tộc': 'dân_tộc', 'người dân tộc': 'dân_tộc',
            'nghề nghiệp': 'nghề_nghiệp', 'nghề': 'nghề_nghiệp', 'làm nghề': 'nghề_nghiệp',
            'địa chỉ': 'địa_chỉ', 'ở đâu': 'địa_chỉ',
            'chẩn đoán': 'chẩn_đoán', 'bệnh': 'chẩn_đoán', 'mắc bệnh': 'chẩn_đoán', 'bị bệnh': 'chẩn_đoán',
            'bệnh kèm': 'bệnh_kèm', 'bệnh khác': 'bệnh_kèm',
            'mã': 'mã_bệnh_nhân', 'mã bệnh nhân': 'mã_bệnh_nhân',
            'bhyt': 'số_bhyt', 'bảo hiểm': 'số_bhyt',
            'vào viện': 'ngày_vào_viện', 'nhập viện': 'ngày_vào_viện',
            'ra viện': 'ngày_ra_viện', 'xuất viện': 'ngày_ra_viện'
        }
        
        # Try pattern matching first
        matched_field = None
        structured_answer = None
        for key, field in field_map.items():
            if key in question_lower and hasattr(self, '_medical_fields'):
                value = self._medical_fields.get(field)
                if value:
                    matched_field = field
                    structured_answer = value
                    break
        
        # Get RAG results
        rag_results = self.rag.search(question, top_k=top_k)
        
        # Extract answer from RAG if no structured match
        rag_answer = None
        if not structured_answer:
            for text, distance, metadata in rag_results:
                if ':' in text:
                    parts = text.split(':', 1)
                    if len(parts) > 1 and parts[1].strip():
                        rag_answer = parts[1].strip()
                        break
                
                if metadata and 'index' in metadata:
                    idx = metadata['index']
                    next_line = self.get_next_line(idx)
                    if next_line and len(next_line.strip()) > 2:
                        rag_answer = next_line
                        break
            
            if not rag_answer and rag_results:
                rag_answer = rag_results[0][0]
        
        # Calculate confidence
        confidence = "HIGH" if structured_answer else "MEDIUM" if rag_answer else "LOW"
        if rag_results and rag_results[0][1] < 5.0:
            confidence = "VERY HIGH"
        elif rag_results and rag_results[0][1] > 15.0:
            confidence = "LOW"
        
        return {
            'answer': structured_answer or rag_answer or "Không tìm thấy",
            'source': 'structured' if structured_answer else 'rag',
            'matched_field': matched_field,
            'confidence': confidence,
            'rag_results': [
                {
                    'text': text,
                    'distance': round(distance, 2),
                    'rank': i + 1
                }
                for i, (text, distance, _) in enumerate(rag_results)
            ]
        }
    
    def process_document(self, image_path: str, save_index: bool = False, 
                        index_path: str = None) -> Dict:
        """Complete pipeline"""
        print(f"\n{'='*70}")
        print(f"Processing: {image_path.split('/')[-1]}")
        print(f"{'='*70}")
        
        # OCR
        ocr_results, annotated_image = self.process_image(image_path)
        print(f"✓ Extracted {len(ocr_results)} text regions\n")
        
        # Build RAG
        if ocr_results:
            self.build_rag_from_ocr(ocr_results)
            if save_index and index_path:
                self.rag.save_index(index_path)
        
        # Extract fields
        medical_fields = self.extract_medical_fields(ocr_results)
        self._medical_fields = medical_fields
        
        return {
            'ocr_results': ocr_results,
            'annotated_image': annotated_image,
            'medical_fields': medical_fields
        }


def main():
    pipeline = MedicalOCRPipeline()
    
    image_path = get_test_image_path('images/01HM00012769_300005_image_70.png')
    results = pipeline.process_document(image_path, save_index=True, 
                                       index_path='medical_doc_index')
    
    # Show all OCR results
    print("=== ALL OCR RESULTS ===")
    for idx, result in enumerate(results['ocr_results']):
        print(f"{idx:2d}: {result['text']}")
    
    # Show extracted fields
    print("\n" + "="*70)
    print("THÔNG TIN BỆNH NHÂN")
    print("="*70)
    
    for field, value in results['medical_fields'].items():
        if value:
            label = field.replace('_', ' ').title()
            print(f"📋 {label:20s}: {value}")
    
    # Test queries with detailed RAG output
    print("\n" + "="*70)
    print("RAG QUERY TEST (with detailed results)")
    print("="*70)
    
    queries = [
        "Tên bệnh nhân là gì?",
        "Bao nhiêu tuổi?",
        "Dân tộc gì?",
        "Nghề nghiệp",
        "Chẩn đoán bệnh chính",
        "Địa chỉ của bệnh nhân",
        "Số bảo hiểm y tế",
        "Khi nào vào viện?",
        "Khi nào ra viện?",
        "Phương pháp điều trị"
    ]
    
    for query in queries:
        result = pipeline.smart_query(query, top_k=5)
        
        print(f"\n{'─'*70}")
        print(f"❓ QUERY: {query}")
        print(f"{'─'*70}")
        
        # Main answer
        source_icon = "📋" if result['source'] == 'structured' else "🔍"
        confidence_color = {
            'VERY HIGH': '✅',
            'HIGH': '✓',
            'MEDIUM': '○',
            'LOW': '⚠'
        }
        conf_icon = confidence_color.get(result['confidence'], '○')
        
        print(f"\n{source_icon} ANSWER: {result['answer']}")
        print(f"{conf_icon} Confidence: {result['confidence']}", end="")
        if result['matched_field']:
            print(f" (từ field: {result['matched_field']})")
        else:
            print(f" (từ RAG search)")
        
        # RAG Top-K results
        print(f"\n📊 RAG Top-5 Results:")
        for rag_item in result['rag_results'][:5]:
            rank = rag_item['rank']
            text = rag_item['text']
            distance = rag_item['distance']
            
            # Truncate long text
            display_text = text[:70] + "..." if len(text) > 70 else text
            
            # Distance indicator
            if distance < 5:
                dist_icon = "🎯"
            elif distance < 10:
                dist_icon = "✓"
            elif distance < 15:
                dist_icon = "○"
            else:
                dist_icon = "·"
            
            print(f"   {rank}. {dist_icon} [{distance:5.1f}] {display_text}")
    
    # Save image
    cv2.imwrite('annotated_output.png', results['annotated_image'])
    print(f"\n✓ Saved: annotated_output.png")


if __name__ == "__main__":
    main()