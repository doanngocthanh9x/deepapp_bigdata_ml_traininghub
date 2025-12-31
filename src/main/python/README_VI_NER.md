# Vietnamese NER Training Guide

This guide shows how to train Named Entity Recognition (NER) models for Vietnamese text using spaCy.

## Installation

1. Install spaCy and Vietnamese language support:
```bash
pip install spacy pyvi
```

2. Install Vietnamese model (optional, for tokenization):
```bash
pip install https://gitlab.com/trungtv/vi_spacy/-/raw/master/packages/vi_core_news_lg-3.6.0/dist/vi_core_news_lg-3.6.0.tar.gz
```

## Quick Start

### 1. Basic Usage with Pre-trained Model

```python
import spacy

# Load Vietnamese model (only has POS tagging, no NER)
nlp = spacy.load('vi_core_news_lg')

doc = nlp('Cộng đồng xử lý ngôn ngữ tự nhiên')
for token in doc:
    print(token.text, token.pos_, token.dep_)
```
 
### 2. Train Custom NER Model

```python
import spacy
from spacy.training import Example

# Create blank Vietnamese model
nlp = spacy.blank('vi')
nlp.add_pipe('sentencizer')
ner = nlp.add_pipe('ner')

# Add entity labels
ner.add_label('PERSON')  # Người
ner.add_label('ORG')     # Tổ chức
ner.add_label('GPE')     # Địa điểm
ner.add_label('MISC')    # Khác

# Training data format: (text, {"entities": [(start, end, label), ...]})
TRAIN_DATA = [
    ("Nguyễn Văn A là kỹ sư", {"entities": [(0, 11, "PERSON")]}),
    ("Công ty ABC Việt Nam", {"entities": [(0, 19, "ORG")]}),
    ("Hà Nội là thủ đô", {"entities": [(0, 6, "GPE")]}),
]

# Training loop
optimizer = nlp.begin_training()
for epoch in range(50):
    losses = {}
    for text, annotations in TRAIN_DATA:
        doc = nlp.make_doc(text)
        example = Example.from_dict(doc, annotations)
        nlp.update([example], losses=losses)
    print(f"Epoch {epoch}, Losses: {losses}")

# Save model
nlp.to_disk('vi_ner_model')
```

### 3. Use Trained Model

```python
import spacy

# Load trained model
nlp = spacy.load('vi_ner_model')

# Process text
doc = nlp('Nguyễn Văn A làm việc tại công ty XYZ ở Hà Nội')

# Extract entities
for ent in doc.ents:
    print(f"{ent.text} -> {ent.label_}")
```

## Training Data Format

Training data should be a list of tuples:
```python
[
    ("Text with entities", {
        "entities": [
            (start_char, end_char, "LABEL"),
            # ...
        ]
    }),
    # ...
]
```

### Entity Labels

Common Vietnamese NER labels:
- `PERSON`: Tên người (Nguyễn Văn A)
- `ORG`: Tổ chức (Công ty ABC, Bệnh viện Việt Đức)
- `GPE`: Địa điểm (Hà Nội, TP.HCM)
- `LOC`: Địa danh (Sông Hồng, Núi Bà Đen)
- `MISC`: Khác (các thực thể không phân loại được)

### Medical Domain Labels

For medical documents:
- `PATIENT_NAME`: Tên bệnh nhân
- `DOCTOR_NAME`: Tên bác sĩ
- `HOSPITAL_NAME`: Tên bệnh viện
- `DIAGNOSIS`: Chẩn đoán bệnh
- `MEDICATION`: Tên thuốc
- `DEPARTMENT`: Khoa phòng

## Best Practices

1. **Character Offsets**: Ensure entity start/end positions are correct character indices
2. **Consistent Labels**: Use consistent entity labels throughout training data
3. **Data Quality**: Clean and diverse training data improves model performance
4. **Validation**: Always validate entity extractions on test data

## Troubleshooting

### Common Issues:

1. **Entity Alignment Errors**: Check character offsets in training data
2. **Model Not Found**: Ensure model path is correct when loading
3. **Low Accuracy**: Increase training iterations or add more training data

### Debug Entity Alignment:

```python
from spacy.training.offsets_to_biluo_tags import offsets_to_biluo_tags

text = "Nguyễn Văn A là kỹ sư"
entities = [(0, 11, "PERSON")]
doc = nlp.make_doc(text)
tags = offsets_to_biluo_tags(doc, entities)
print(tags)  # Should not contain '-' (misaligned entities)
```

## Advanced Usage

### Custom Tokenizer

For better Vietnamese tokenization:
```python
from pyvi import ViTokenizer

def custom_tokenizer(text):
    return ViTokenizer.tokenize(text).split()

# Use in spaCy pipeline
# (Advanced configuration required)
```

### Model Evaluation

```python
from spacy.scorer import Scorer

scorer = Scorer()
for text, annotations in TEST_DATA:
    doc = nlp(text)
    scorer.score(doc, annotations)

print(scorer.scores)
```

## Files in This Project

- `vi_spacy_demo.py`: Demo script for basic usage
- `train_vi_ner.py`: Complete training pipeline
- `vi_ner_model/`: Trained model directory (after training)

## Resources

- [spaCy Documentation](https://spacy.io/)
- [vi_spacy Repository](https://github.com/trungtv/vi_spacy)
- [Vietnamese NLP Resources](https://github.com/stopwords/vietnamese-stopwords)

## Next Steps

1. Collect more training data for your specific domain
2. Fine-tune hyperparameters (learning rate, batch size, etc.)
3. Add domain-specific entity types
4. Evaluate model performance on real data
5. Deploy model in production pipeline