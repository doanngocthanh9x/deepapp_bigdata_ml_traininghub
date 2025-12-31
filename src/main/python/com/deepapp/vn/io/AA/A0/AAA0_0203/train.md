"""
Training NER với spaCy-transformers + PhoBERT cho văn bản y tế tiếng Việt
"""

import json
import spacy
from spacy.tokens import DocBin
from spacy.training import Example
import random
from pathlib import Path

# ===========================
# BƯỚC 1: Cài đặt dependencies
# ===========================
"""
Chạy các lệnh sau trong terminal:

pip install spacy spacy-transformers
pip install transformers torch

# Download PhoBERT config cho spaCy
python -m spacy download vi_core_news_lg
"""

# ===========================
# BƯỚC 2: Chuyển đổi dữ liệu
# ===========================

def convert_json_to_spacy_format(json_file_path):
    """
    Chuyển đổi JSON annotations sang format spaCy
    """
    with open(json_file_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    training_data = []
    
    for sample in data['samples']:
        text = sample['text']
        entities = []
        
        for ann in sample['annotations']:
            entities.append((ann['start'], ann['end'], ann['label']))
        
        training_data.append((text, {"entities": entities}))
    
    return training_data

def create_docbin(training_data, output_path):
    """
    Tạo DocBin cho spaCy training
    """
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
    print(f"✓ Đã lưu {len(training_data)} documents vào {output_path}")

# ===========================
# BƯỚC 3: Setup và Training
# ===========================

def prepare_training_data(json_file, train_ratio=0.8):
    """
    Chuẩn bị train/dev split
    """
    training_data = convert_json_to_spacy_format(json_file)
    random.shuffle(training_data)
    
    split_idx = int(len(training_data) * train_ratio)
    train_data = training_data[:split_idx]
    dev_data = training_data[split_idx:]
    
    # Tạo thư mục
    Path("./corpus").mkdir(exist_ok=True)
    
    create_docbin(train_data, "./corpus/train.spacy")
    create_docbin(dev_data, "./corpus/dev.spacy")
    
    return len(train_data), len(dev_data)

# ===========================
# BƯỚC 4: Tạo config file
# ===========================

CONFIG_TEMPLATE = """
[paths]
train = ./corpus/train.spacy
dev = ./corpus/dev.spacy
vectors = null
init_tok2vec = null

[system]
gpu_allocator = "pytorch"
seed = 42

[nlp]
lang = "vi"
pipeline = ["transformer", "ner"]
batch_size = 128
disabled = []
before_creation = null
after_creation = null
after_pipeline_creation = null
tokenizer = {"@tokenizers": "spacy.Tokenizer.v1"}

[components]

[components.transformer]
factory = "transformer"
max_batch_items = 4096
set_extra_annotations = {"@annotation_setters": "spacy-transformers.null_annotation_setter.v1"}

[components.transformer.model]
@architectures = "spacy-transformers.TransformerModel.v3"
name = "vinai/phobert-base"
mixed_precision = false

[components.transformer.model.get_spans]
@span_getters = "spacy-transformers.strided_spans.v1"
window = 128
stride = 96

[components.transformer.model.grad_scaler_config]

[components.transformer.model.tokenizer_config]
use_fast = true

[components.ner]
factory = "ner"
incorrect_spans_key = null
moves = null
scorer = {"@scorers": "spacy.ner_scorer.v1"}
update_with_oracle_cut_size = 100

[components.ner.model]
@architectures = "spacy.TransitionBasedParser.v2"
state_type = "ner"
extra_state_tokens = false
hidden_width = 64
maxout_pieces = 2
use_upper = false
nO = null

[components.ner.model.tok2vec]
@architectures = "spacy-transformers.TransformerListener.v1"
grad_factor = 1.0
pooling = {"@layers": "reduce_mean.v1"}
upstream = "*"

[corpora]

[corpora.dev]
@readers = "spacy.Corpus.v1"
path = ${paths.dev}
max_length = 0
gold_preproc = false
limit = 0
augmenter = null

[corpora.train]
@readers = "spacy.Corpus.v1"
path = ${paths.train}
max_length = 0
gold_preproc = false
limit = 0
augmenter = null

[training]
dev_corpus = "corpora.dev"
train_corpus = "corpora.train"
seed = ${system.seed}
gpu_allocator = ${system.gpu_allocator}
dropout = 0.1
accumulate_gradient = 3
patience = 5000
max_epochs = 0
max_steps = 20000
eval_frequency = 200
frozen_components = []
annotating_components = []
before_to_disk = null
before_update = null

[training.batcher]
@batchers = "spacy.batch_by_padded.v1"
discard_oversize = true
size = 2000
buffer = 256
get_length = null

[training.logger]
@loggers = "spacy.ConsoleLogger.v1"
progress_bar = false

[training.optimizer]
@optimizers = "Adam.v1"
beta1 = 0.9
beta2 = 0.999
L2_is_weight_decay = true
L2 = 0.01
grad_clip = 1.0
use_averages = false
eps = 0.00000001
learn_rate = 0.00005

[training.optimizer.schedules]
@schedules = "warmup_linear.v1"
warmup_steps = 250
total_steps = 20000
initial_rate = 0.00005

[training.score_weights]
ents_f = 1.0
ents_p = 0.0
ents_r = 0.0
ents_per_type = null

[pretraining]

[initialize]
vectors = ${paths.vectors}
init_tok2vec = ${paths.init_tok2vec}
vocab_data = null
lookups = null
before_init = null
after_init = null

[initialize.components]

[initialize.tokenizer]
"""

def create_config_file():
    """
    Tạo file config cho training
    """
    with open("config.cfg", "w", encoding="utf-8") as f:
        f.write(CONFIG_TEMPLATE)
    print("✓ Đã tạo config.cfg")

# ===========================
# BƯỚC 5: Script chính
# ===========================

def main():
    print("=" * 60)
    print("TRAINING NER VỚI SPACY + PHOBERT")
    print("=" * 60)
    
    # 1. Chuẩn bị dữ liệu
    print("\n[1/4] Chuẩn bị dữ liệu...")
    json_file = "discharge_summary_data.json"  # Thay bằng path file của bạn
    train_count, dev_count = prepare_training_data(json_file)
    print(f"  → Train: {train_count} samples")
    print(f"  → Dev: {dev_count} samples")
    
    # 2. Tạo config
    print("\n[2/4] Tạo config file...")
    create_config_file()
    
    # 3. Training
    print("\n[3/4] Bắt đầu training...")
    print("\n→ Chạy lệnh sau trong terminal:\n")
    print("  python -m spacy train config.cfg --output ./output --paths.train ./corpus/train.spacy --paths.dev ./corpus/dev.spacy --gpu-id 0")
    print("\n  (Bỏ --gpu-id 0 nếu không có GPU)")
    
    # 4. Evaluation
    print("\n[4/4] Sau khi train xong, đánh giá model:")
    print("\n  python -m spacy evaluate ./output/model-best ./corpus/dev.spacy --gpu-id 0")
    
    print("\n" + "=" * 60)
    print("HOÀN TẤT SETUP!")
    print("=" * 60)

# ===========================
# BƯỚC 6: Sử dụng model đã train
# ===========================

def predict_with_trained_model(model_path, text):
    """
    Sử dụng model đã train để predict
    """
    nlp = spacy.load(model_path)
    doc = nlp(text)
    
    print(f"\nText: {text}\n")
    print("Entities found:")
    print("-" * 60)
    
    for ent in doc.ents:
        print(f"{ent.text:30} | {ent.label_:15} | ({ent.start_char}, {ent.end_char})")
    
    return doc

# Example usage
def test_model():
    """
    Test model với văn bản mẫu
    """
    model_path = "./output/model-best"
    
    test_text = """
    Họ tên người bệnh: NGUYỄN VĂN A
    Tuổi: 45
    Giới tính: Nam
    Chẩn đoán: Viêm phổi cấp
    Phương pháp điều trị: Kháng sinh, truyền dịch
    """
    
    doc = predict_with_trained_model(model_path, test_text)
    return doc

if __name__ == "__main__":
    # Chạy setup và training
    main()
    
    # Uncomment để test model sau khi train xong
    # test_model()