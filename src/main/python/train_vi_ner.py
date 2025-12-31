"""
Guide to add NER component to Vietnamese spaCy model and train custom NER
"""

import spacy
from spacy.training import Example
from spacy.util import minibatch, compounding
import random

def add_ner_to_vi_model():
    """Add NER component to Vietnamese model"""

    print("=== ADDING NER COMPONENT TO VIETNAMESE MODEL ===\n")

    # Create blank Vietnamese model to avoid compatibility issues
    nlp = spacy.blank('vi')
    print("✓ Created blank Vietnamese model")

    # Add required components
    if "sentencizer" not in nlp.pipe_names:
        nlp.add_pipe("sentencizer")

    # Add NER component
    if "ner" not in nlp.pipe_names:
        ner = nlp.add_pipe("ner", last=True)
        print("✓ Added NER component")
    else:
        ner = nlp.get_pipe("ner")
        print("✓ NER component already exists")

    print(f"Components: {nlp.component_names}")
    return nlp

def prepare_training_data():
    """Prepare training data for Vietnamese NER"""

    # Sample training data for Vietnamese NER
    # Format: (text, {"entities": [(start, end, label), ...]})
    train_data = [
        # Person names
        ("Nguyễn Văn A là một kỹ sư", {"entities": [(0, 11, "PERSON")]}),
        ("Cô Trần Thị B dạy học", {"entities": [(3, 12, "PERSON")]}),
        ("Ông Lê Văn C là bác sĩ", {"entities": [(3, 12, "PERSON")]}),

        # Organizations
        ("Công ty TNHH ABC Việt Nam", {"entities": [(0, 23, "ORG")]}),
        ("Bệnh viện Việt Đức Hà Nội", {"entities": [(0, 23, "ORG")]}),
        ("Đại học Quốc gia Hà Nội", {"entities": [(0, 22, "ORG")]}),

        # Locations
        ("Tôi sống ở Hà Nội", {"entities": [(11, 17, "GPE")]}),
        ("TP.HCM là thành phố lớn", {"entities": [(0, 6, "GPE")]}),
        ("Đà Nẵng có biển đẹp", {"entities": [(0, 7, "GPE")]}),

        # Mixed entities
        ("Nguyễn Văn A làm việc tại công ty XYZ ở Hà Nội", {
            "entities": [(0, 11, "PERSON"), (27, 35, "ORG"), (39, 45, "GPE")]
        }),

        ("Bác sĩ Trần Thị B tại bệnh viện Việt Đức, Hà Nội", {
            "entities": [(7, 16, "PERSON"), (25, 40, "ORG"), (42, 48, "GPE")]
        }),
    ]

    return train_data

def train_ner_model(nlp, train_data, n_iter=50):
    """Train the NER model"""

    print(f"\n=== TRAINING NER MODEL ===")
    print(f"Training data size: {len(train_data)} examples")
    print(f"Training iterations: {n_iter}")

    # Get NER component
    ner = nlp.get_pipe("ner")

    # Add entity labels
    labels = set()
    for _, annotations in train_data:
        for ent in annotations.get("entities", []):
            labels.add(ent[2])

    for label in labels:
        ner.add_label(label)

    print(f"Entity labels: {sorted(labels)}")

    # Disable other pipes during training
    other_pipes = [pipe for pipe in nlp.pipe_names if pipe != "ner"]
    with nlp.disable_pipes(*other_pipes):
        optimizer = nlp.begin_training()

        for iteration in range(n_iter):
            random.shuffle(train_data)
            losses = {}

            # Batch training data
            batches = minibatch(train_data, size=compounding(4.0, 32.0, 1.001))

            for batch in batches:
                examples = []
                for text, annotations in batch:
                    doc = nlp.make_doc(text)
                    try:
                        example = Example.from_dict(doc, annotations)
                        examples.append(example)
                    except ValueError as e:
                        print(f"Skipping problematic example: {e}")
                        continue

                if examples:
                    nlp.update(examples, drop=0.5, losses=losses)

            if iteration % 10 == 0:
                print(f"Iteration {iteration}, Losses: {losses}")

    print("✓ Training completed")
    return nlp

def test_trained_model(nlp):
    """Test the trained NER model"""

    print("\n=== TESTING TRAINED MODEL ===")

    test_texts = [
        "Nguyễn Văn A sống tại Hà Nội",
        "Công ty ABC Việt Nam tuyển dụng",
        "Bệnh viện Việt Đức ở Hà Nội",
        "Tôi gặp Trần Thị B ở TP.HCM",
        "Đại học Quốc gia Hà Nội có nhiều sinh viên"
    ]

    for text in test_texts:
        doc = nlp(text)
        print(f"\nText: {text}")
        if doc.ents:
            for ent in doc.ents:
                print(f"  {ent.label_:10} '{ent.text}'")
        else:
            print("  No entities found")

def save_and_load_model(nlp, model_path="vi_ner_model"):
    """Save and demonstrate loading the trained model"""

    print(f"\n=== SAVING MODEL TO {model_path} ===")

    # Save model
    nlp.to_disk(model_path)
    print("✓ Model saved")

    # Load model
    print("\nLoading model...")
    nlp_loaded = spacy.load(model_path)
    print("✓ Model loaded successfully")

    # Test loaded model
    test_text = "Nguyễn Văn A làm việc tại công ty XYZ"
    doc = nlp_loaded(test_text)
    print(f"\nTest with loaded model: {test_text}")
    for ent in doc.ents:
        print(f"  {ent.label_:10} '{ent.text}'")

def main():
    """Main function to demonstrate Vietnamese NER training"""

    print("VIETNAMESE NER TRAINING GUIDE")
    print("=" * 50)

    # Step 1: Add NER component
    nlp = add_ner_to_vi_model()

    # Step 2: Prepare training data
    train_data = prepare_training_data()
    print(f"\n✓ Prepared {len(train_data)} training examples")

    # Step 3: Train model
    nlp = train_ner_model(nlp, train_data, n_iter=50)

    # Step 4: Test model
    test_trained_model(nlp)

    # Step 5: Save and load model
    save_and_load_model(nlp)

    print("\n" + "=" * 50)
    print("✓ VIETNAMESE NER TRAINING COMPLETE")
    print("\nTo use your trained model:")
    print("import spacy")
    print("nlp = spacy.load('vi_ner_model')")
    print("doc = nlp('Your Vietnamese text here')")
    print("for ent in doc.ents:")
    print("    print(ent.text, ent.label_)")

if __name__ == "__main__":
    main()