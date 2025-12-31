# document_templates.py
"""
Flexible multi-document OCR system
Hỗ trợ nhiều loại document với config rules riêng
"""

import re
import json
from typing import List, Dict, Optional, Callable, Any
from dataclasses import dataclass, field
from abc import ABC, abstractmethod
from enum import Enum


class ExtractionStrategy(Enum):
    """Các strategy để extract data"""
    REGEX = "regex"
    KEYWORD_VALUE = "keyword_value"  # "Label: Value"
    NEXT_LINE = "next_line"  # Value ở dòng sau
    PREV_LINE = "prev_line"  # Value ở dòng trước
    MULTI_LINE = "multi_line"  # Value trải dài nhiều dòng
    CUSTOM = "custom"  # Custom function


@dataclass
class FieldRule:
    """Rule để extract một field cụ thể"""
    field_name: str
    display_name: str  # Tên hiển thị (VD: "Họ tên")
    keywords: List[str]  # Keywords trigger rule
    strategy: ExtractionStrategy
    
    # Regex patterns (nếu dùng REGEX strategy)
    patterns: List[str] = field(default_factory=list)
    
    # Validation function
    validator: Optional[Callable[[str], bool]] = None
    
    # Transform function (VD: format date, uppercase, etc.)
    transformer: Optional[Callable[[str], str]] = None
    
    # Priority (cao hơn = check trước)
    priority: int = 5
    
    # Stop keywords (dừng multi-line extraction)
    stop_keywords: List[str] = field(default_factory=list)
    
    # Custom extractor function
    custom_extractor: Optional[Callable] = None
    
    # Query aliases (để map từ câu hỏi sang field)
    query_aliases: List[str] = field(default_factory=list)


@dataclass
class DocumentTemplate:
    """Template cho một loại document"""
    template_id: str
    template_name: str
    description: str
    rules: List[FieldRule]
    
    # Document classifier keywords (để detect loại document)
    classifier_keywords: List[str] = field(default_factory=list)
    
    # Metadata
    version: str = "1.0"
    author: str = ""
    created_at: str = ""


class BaseExtractor:
    """Base class cho extractors"""
    
    @staticmethod
    def extract_value_from_context(text: str, next_line: Optional[str] = None) -> Optional[str]:
        """Extract value từ "Label: Value" format"""
        if ':' in text:
            parts = text.split(':', 1)
            if len(parts) > 1:
                value = parts[1].strip()
                if value and len(value) > 1:
                    return value
        
        if next_line and next_line.strip():
            return next_line.strip()
        
        return None
    
    @staticmethod
    def extract_by_regex(text: str, patterns: List[str]) -> Optional[str]:
        """Extract bằng regex patterns"""
        for pattern in patterns:
            match = re.search(pattern, text, re.IGNORECASE)
            if match:
                # Return first captured group hoặc whole match
                return match.group(1) if match.groups() else match.group(0)
        return None
    
    @staticmethod
    def extract_next_line(current_index: int, ocr_results: List[Dict]) -> Optional[str]:
        """Get next line text"""
        if current_index + 1 < len(ocr_results):
            return ocr_results[current_index + 1]['text']
        return None
    
    @staticmethod
    def extract_prev_line(current_index: int, ocr_results: List[Dict]) -> Optional[str]:
        """Get previous line text"""
        if current_index > 0:
            return ocr_results[current_index - 1]['text']
        return None
    
    @staticmethod
    def extract_multi_line(current_index: int, ocr_results: List[Dict], 
                          stop_keywords: List[str]) -> Optional[str]:
        """Extract multi-line value cho đến khi gặp stop keywords"""
        lines = []
        idx = current_index + 1
        
        while idx < len(ocr_results):
            line = ocr_results[idx]['text']
            line_lower = line.lower()
            
            # Stop nếu gặp keyword
            if any(kw in line_lower for kw in stop_keywords):
                break
            
            # Stop nếu gặp pattern "Label:"
            if ':' in line and len(line.split(':')[0]) < 30:
                break
            
            lines.append(line.strip())
            idx += 1
            
            # Max 3 lines
            if len(lines) >= 3:
                break
        
        return ' '.join(lines) if lines else None


class DocumentProcessor:
    """Main processor sử dụng template system"""
    
    def __init__(self):
        self.templates: Dict[str, DocumentTemplate] = {}
        self.current_template: Optional[DocumentTemplate] = None
        self.extractor = BaseExtractor()
    
    def register_template(self, template: DocumentTemplate):
        """Register một document template"""
        self.templates[template.template_id] = template
        print(f"✓ Registered template: {template.template_name}")
    
    def load_template_from_json(self, filepath: str):
        """Load template từ JSON file"""
        with open(filepath, 'r', encoding='utf-8') as f:
            data = json.load(f)
        
        rules = []
        for rule_data in data['rules']:
            # Convert strategy string to enum
            strategy = ExtractionStrategy(rule_data['strategy'])
            
            # Load validator if exists
            validator = None
            if 'validator' in rule_data and rule_data['validator']:
                validator = eval(rule_data['validator'])
            
            # Load transformer
            transformer = None
            if 'transformer' in rule_data and rule_data['transformer']:
                transformer = eval(rule_data['transformer'])
            
            rule = FieldRule(
                field_name=rule_data['field_name'],
                display_name=rule_data['display_name'],
                keywords=rule_data['keywords'],
                strategy=strategy,
                patterns=rule_data.get('patterns', []),
                validator=validator,
                transformer=transformer,
                priority=rule_data.get('priority', 5),
                stop_keywords=rule_data.get('stop_keywords', []),
                query_aliases=rule_data.get('query_aliases', [])
            )
            rules.append(rule)
        
        # Sort by priority
        rules.sort(key=lambda r: r.priority, reverse=True)
        
        template = DocumentTemplate(
            template_id=data['template_id'],
            template_name=data['template_name'],
            description=data['description'],
            rules=rules,
            classifier_keywords=data.get('classifier_keywords', []),
            version=data.get('version', '1.0')
        )
        
        self.register_template(template)
    
    def detect_document_type(self, ocr_results: List[Dict]) -> Optional[str]:
        """Auto-detect loại document dựa vào classifier keywords"""
        all_text = ' '.join([r['text'].lower() for r in ocr_results])
        
        best_match = None
        max_matches = 0
        
        for template_id, template in self.templates.items():
            matches = sum(1 for kw in template.classifier_keywords if kw in all_text)
            if matches > max_matches:
                max_matches = matches
                best_match = template_id
        
        return best_match
    
    def extract_fields(self, ocr_results: List[Dict], 
                      template_id: Optional[str] = None) -> Dict[str, Any]:
        """Extract tất cả fields theo template"""
        
        # Auto-detect nếu không chỉ định template
        if template_id is None:
            template_id = self.detect_document_type(ocr_results)
            if template_id:
                print(f"🔍 Auto-detected document type: {self.templates[template_id].template_name}")
        
        if template_id not in self.templates:
            raise ValueError(f"Template '{template_id}' not found")
        
        template = self.templates[template_id]
        self.current_template = template
        
        # Initialize fields
        fields = {rule.field_name: None for rule in template.rules}
        
        # Extract theo từng rule
        for i, result in enumerate(ocr_results):
            text = result['text']
            text_lower = text.lower()
            
            for rule in template.rules:
                # Skip nếu đã extract
                if fields[rule.field_name]:
                    continue
                
                # Check keywords
                if not any(kw in text_lower for kw in rule.keywords):
                    continue
                
                # Extract theo strategy
                value = None
                
                if rule.strategy == ExtractionStrategy.REGEX:
                    value = self.extractor.extract_by_regex(text, rule.patterns)
                    if not value:
                        next_line = self.extractor.extract_next_line(i, ocr_results)
                        if next_line:
                            value = self.extractor.extract_by_regex(next_line, rule.patterns)
                
                elif rule.strategy == ExtractionStrategy.KEYWORD_VALUE:
                    # First try next line
                    next_line = self.extractor.extract_next_line(i, ocr_results)
                    value = self.extractor.extract_value_from_context(text, next_line)
                    
                    # If no value found and keyword is at end of line, check previous line
                    if not value and text.rstrip().endswith(':'):
                        prev_line = self.extractor.extract_prev_line(i, ocr_results)
                        if prev_line and prev_line.strip():
                            value = prev_line.strip()
                
                elif rule.strategy == ExtractionStrategy.NEXT_LINE:
                    value = self.extractor.extract_next_line(i, ocr_results)
                
                elif rule.strategy == ExtractionStrategy.PREV_LINE:
                    value = self.extractor.extract_prev_line(i, ocr_results)
                
                elif rule.strategy == ExtractionStrategy.MULTI_LINE:
                    # First try to extract from current line after keyword
                    if ':' in text:
                        parts = text.split(':', 1)
                        if len(parts) > 1:
                            current_line_value = parts[1].strip()
                            if current_line_value:
                                value = current_line_value + ' '
                    
                    # Then add multi-line continuation
                    multi_value = self.extractor.extract_multi_line(i, ocr_results, rule.stop_keywords)
                    if multi_value:
                        value = (value or '') + multi_value
                
                elif rule.strategy == ExtractionStrategy.CUSTOM and rule.custom_extractor:
                    context = {'index': i, 'ocr_results': ocr_results}
                    value = rule.custom_extractor(text, context)
                
                # Validate
                if value and rule.validator:
                    if not rule.validator(value):
                        value = None
                
                # Transform
                if value and rule.transformer:
                    value = rule.transformer(value)
                
                if value:
                    fields[rule.field_name] = value
        
        return fields
    
    def query_field(self, question: str, fields: Dict[str, Any]) -> Dict[str, Any]:
        """Query field từ câu hỏi"""
        if not self.current_template:
            return {'answer': 'Không có template được load', 'confidence': 'LOW'}
        
        question_lower = question.lower()
        
        # Find matching field
        matched_field = None
        for rule in self.current_template.rules:
            # Check query aliases
            if any(alias in question_lower for alias in rule.query_aliases):
                matched_field = rule.field_name
                break
            
            # Check keywords
            if any(kw in question_lower for kw in rule.keywords):
                matched_field = rule.field_name
                break
        
        if matched_field and matched_field in fields:
            return {
                'answer': fields[matched_field] or 'Không tìm thấy',
                'field': matched_field,
                'confidence': 'HIGH' if fields[matched_field] else 'LOW'
            }
        
        return {'answer': 'Không tìm thấy', 'confidence': 'LOW'}


# ==================== VALIDATORS ====================

def validate_date(value: str) -> bool:
    """Validate date format dd/mm/yyyy or dd tháng mm năm yyyy, handling OCR errors"""
    # Common OCR corrections for months
    ocr_corrections = {
        'li': '11',
        'l1': '11', 
        'll': '11',
        'i1': '11',
        'il': '11',
        'O': '0',
        'o': '0',
        'I': '1',
        'l': '1',
        'S': '5',
        's': '5',
        'B': '8',
        'G': '6',
        'Z': '2',
        'A': '4',
        'a': '4',
        'e': '6',
        'g': '9'
    }
    
    # Apply OCR corrections
    corrected_value = value
    for ocr_char, correct_char in ocr_corrections.items():
        corrected_value = corrected_value.replace(ocr_char, correct_char)
    
    # Pattern for dd/mm/yyyy
    pattern1 = r'\d{1,2}/\d{1,2}/\d{4}'
    # Pattern for dd tháng mm năm yyyy
    pattern2 = r'\d{1,2}\s*tháng\s*\d{1,2}\s*năm\s*\d{4}'
    # Pattern for ngày dd tháng mm năm yyyy
    pattern3 = r'ngày\s*\d{1,2}\s*tháng\s*\d{1,2}\s*năm\s*\d{4}'
    # Pattern for dd/mm năm yyyy
    pattern4 = r'\d{1,2}/\d{1,2}\s*năm\s*\d{4}'
    # Pattern for dd tháng mm
    pattern5 = r'\d{1,2}\s*tháng\s*\d{1,2}'
    
    return bool(re.search(pattern1, corrected_value) or 
                re.search(pattern2, corrected_value) or 
                re.search(pattern3, corrected_value) or
                re.search(pattern4, corrected_value) or
                re.search(pattern5, corrected_value))


def validate_age(value: str) -> bool:
    """Validate age (0-120), handling OCR errors"""
    # Common OCR corrections for digits
    ocr_corrections = {
        'O': '0',
        'o': '0',
        'I': '1',
        'l': '1',
        'S': '5',
        's': '5',
        'B': '8',
        'G': '6',
        'Z': '2'
    }
    
    # Apply OCR corrections
    corrected_value = value
    for ocr_char, correct_char in ocr_corrections.items():
        corrected_value = corrected_value.replace(ocr_char, correct_char)
    
    match = re.search(r'(\d+)', corrected_value)
    if match:
        age = int(match.group(1))
        return 0 <= age <= 120
    return False


def validate_birth_year(value: str) -> bool:
    """Validate birth year (1900-2025)"""
    match = re.search(r'(\d{4})', value)
    if match:
        year = int(match.group(1))
        return 1900 <= year <= 2025
    return False


def format_birth_year_transformer(value: str) -> str:
    """Format birth year consistently"""
    match = re.search(r'(\d{4})', value)
    if match:
        return match.group(1)
    return value


def validate_phone(value: str) -> bool:
    """Validate phone number"""
    pattern = r'^\d{10,11}$'
    return bool(re.match(pattern, value.replace(' ', '')))


# ==================== TRANSFORMERS ====================

def uppercase_transformer(value: str) -> str:
    """Convert to uppercase"""
    return value.upper()


def format_date_transformer(value: str) -> str:
    """Format date consistently, handling OCR errors and fragmented dates"""
    # Common OCR corrections for months
    ocr_corrections = {
        'li': '11',
        'l1': '11', 
        'll': '11',
        'i1': '11',
        'il': '11',
        'O': '0',
        'o': '0',
        'I': '1',
        'l': '1',
        'S': '5',
        's': '5',
        'B': '8',
        'G': '6',
        'Z': '2',
        'A': '4',  # Common OCR error
        'a': '4',
        'e': '6',
        'g': '9'
    }
    
    # Apply OCR corrections to the value
    corrected_value = value
    for ocr_char, correct_char in ocr_corrections.items():
        corrected_value = corrected_value.replace(ocr_char, correct_char)
    
    # Extract date patterns from corrected value - handle more fragmented patterns
    date_patterns = [
        r'(\d{1,2})/(\d{1,2})/(\d{4})',  # dd/mm/yyyy
        r'(\d{1,2})\s*tháng\s*(\d{1,2})\s*năm\s*(\d{4})',  # dd tháng mm năm yyyy
        r'ngày\s*(\d{1,2})\s*tháng\s*(\d{1,2})\s*năm\s*(\d{4})',  # ngày dd tháng mm năm yyyy
        r'(\d{1,2})\s*tháng\s*(\d{1,2})\s*năm\s*(\d{4})',  # dd tháng mm năm yyyy (without ngày)
        r'(\d{1,2})/(\d{1,2})\s*năm\s*(\d{4})',  # dd/mm năm yyyy
        r'(\d{1,2})\s*tháng\s*(\d{1,2})',  # dd tháng mm (assume current year)
        r'(\d{1,2})/(\d{1,2})',  # dd/mm (assume current year)
    ]
    
    for pattern in date_patterns:
        match = re.search(pattern, corrected_value, re.IGNORECASE)
        if match:
            groups = match.groups()
            if len(groups) == 3:
                day, month, year = groups
                # Validate month is 1-12
                try:
                    month_num = int(month)
                    if 1 <= month_num <= 12:
                        # If year is 2 digits, assume 20xx
                        if len(year) == 2:
                            year = f"20{year}"
                        return f"{day.zfill(2)}/{month.zfill(2)}/{year}"
                except ValueError:
                    pass
            elif len(groups) == 2:
                # Handle dd/mm or dd tháng mm patterns
                day, month = groups
                try:
                    month_num = int(month)
                    if 1 <= month_num <= 12:
                        # Assume current year if not specified
                        return f"{day.zfill(2)}/{month.zfill(2)}/2024"
                except ValueError:
                    pass
    
    return value


def normalize_age_transformer(value: str) -> str:
    """Normalize age, handling OCR errors"""
    # Common OCR corrections for digits (age-specific)
    ocr_corrections = {
        'O': '0',
        'o': '0',
        'I': '1',
        'l': '1',
        'S': '8',  # In age context, S often means 8
        's': '8',
        'B': '8',
        'G': '6',
        'Z': '2'
    }
    
    # Apply OCR corrections
    corrected_value = value
    for ocr_char, correct_char in ocr_corrections.items():
        corrected_value = corrected_value.replace(ocr_char, correct_char)
    
    # Extract the first number found
    match = re.search(r'(\d+)', corrected_value)
    if match:
        return match.group(1)
    
    return value


def normalize_spaces(value: str) -> str:
    """Normalize multiple spaces to single space"""
    return ' '.join(value.split())


# ==================== DEMO ====================

def create_discharge_summary_template() -> DocumentTemplate:
    """Tạo template cho giấy ra viện"""
    rules = [
        FieldRule(
            field_name='họ_tên',
            display_name='Họ và tên',
            keywords=['họ tên', 'họ và tên', 'tên bệnh nhân'],
            strategy=ExtractionStrategy.KEYWORD_VALUE,
            priority=10,
            query_aliases=['tên', 'người bệnh', 'bệnh nhân']
        ),
        FieldRule(
            field_name='ngày_sinh',
            display_name='Ngày sinh',
            keywords=['ngày sinh', 'sinh:', 'năm sinh'],
            strategy=ExtractionStrategy.REGEX,
            patterns=[r'(\d{1,2}/\d{1,2}/\d{4})'],
            validator=validate_date,
            priority=10,
            query_aliases=['sinh nhật', 'sinh']
        ),
        FieldRule(
            field_name='tuổi',
            display_name='Tuổi',
            keywords=['tuổi:', 'tuổi'],
            strategy=ExtractionStrategy.REGEX,
            patterns=[r'(\d+)\s*tuổi'],
            validator=validate_age,
            priority=8,
            query_aliases=['bao nhiêu tuổi', 'mấy tuổi']
        ),
        FieldRule(
            field_name='chẩn_đoán',
            display_name='Chẩn đoán',
            keywords=['chẩn đoán', 'chần đoán', 'chuẩn đoán'],
            strategy=ExtractionStrategy.MULTI_LINE,
            stop_keywords=['bệnh kèm', 'phương pháp', 'ghi chú'],
            priority=9,
            transformer=normalize_spaces,
            query_aliases=['bệnh', 'mắc bệnh', 'bị bệnh']
        ),
        # ... more rules
    ]
    
    return DocumentTemplate(
        template_id='discharge_summary',
        template_name='Giấy ra viện',
        description='Template cho giấy tóm tắt ra viện',
        rules=rules,
        classifier_keywords=['ra viện', 'tóm tắt bệnh án', 'giấy ra viện']
    )


def create_prescription_template() -> DocumentTemplate:
    """Tạo template cho đơn thuốc"""
    rules = [
        FieldRule(
            field_name='bác_sĩ',
            display_name='Bác sĩ kê đơn',
            keywords=['bác sĩ', 'bs.', 'doctor'],
            strategy=ExtractionStrategy.KEYWORD_VALUE,
            priority=10,
            query_aliases=['ai kê đơn', 'bác sĩ nào']
        ),
        FieldRule(
            field_name='ngày_kê_đơn',
            display_name='Ngày kê đơn',
            keywords=['ngày', 'date'],
            strategy=ExtractionStrategy.REGEX,
            patterns=[r'(\d{1,2}/\d{1,2}/\d{4})'],
            priority=9
        ),
        FieldRule(
            field_name='thuốc',
            display_name='Danh sách thuốc',
            keywords=['thuốc', 'medication'],
            strategy=ExtractionStrategy.MULTI_LINE,
            stop_keywords=['tái khám', 'lưu ý', 'chú ý'],
            priority=8,
            query_aliases=['dùng thuốc gì', 'thuốc gì']
        ),
    ]
    
    return DocumentTemplate(
        template_id='prescription',
        template_name='Đơn thuốc',
        description='Template cho đơn thuốc',
        rules=rules,
        classifier_keywords=['đơn thuốc', 'prescription', 'toa thuốc']
    )


def demo():
    """Demo multi-document system"""
    processor = DocumentProcessor()
    
    # Register templates
    processor.register_template(create_discharge_summary_template())
    processor.register_template(create_prescription_template())
    
    print(f"\n✓ Loaded {len(processor.templates)} templates")
    for tid, template in processor.templates.items():
        print(f"  - {template.template_name} ({len(template.rules)} rules)")
    
    # Mock OCR results cho giấy ra viện
    ocr_results = [
        {'text': 'GIẤY TÓM TẮT RA VIỆN', 'index': 0},
        {'text': 'Họ tên: Nguyễn Văn A', 'index': 1},
        {'text': 'Ngày sinh: 15/05/1980', 'index': 2},
        {'text': 'Tuổi: 44 tuổi', 'index': 3},
        {'text': 'Chẩn đoán: Viêm phổi', 'index': 4},
        {'text': 'do vi khuẩn', 'index': 5},
    ]
    
    # Extract (auto-detect)
    fields = processor.extract_fields(ocr_results)
    
    print("\n" + "="*60)
    print("EXTRACTED FIELDS")
    print("="*60)
    for field_name, value in fields.items():
        if value:
            # Get display name
            rule = next((r for r in processor.current_template.rules 
                        if r.field_name == field_name), None)
            display = rule.display_name if rule else field_name
            print(f"{display:20s}: {value}")
    
    # Query
    print("\n" + "="*60)
    print("QUERY TEST")
    print("="*60)
    
    queries = ["Tên bệnh nhân là gì?", "Bao nhiêu tuổi?", "Bệnh gì?"]
    for q in queries:
        result = processor.query_field(q, fields)
        print(f"Q: {q}")
        print(f"A: {result['answer']} (confidence: {result['confidence']})\n")


if __name__ == "__main__":
    demo()