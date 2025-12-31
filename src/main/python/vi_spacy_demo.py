"""
Demo script for Vietnamese NER using vi_spacy
"""

import spacy

def demo_vi_spacy():
    """Demo Vietnamese NER with vi_spacy"""

    print("=== VIETNAMESE NER DEMO WITH VI_SPACY ===\n")

    # Load the Vietnamese model
    try:
        nlp = spacy.load('vi_core_news_lg')
        print("✓ Successfully loaded vi_core_news_lg model")
    except OSError as e:
        print(f"✗ Failed to load model: {e}")
        print("Make sure you have installed the model with:")
        print("pip install https://gitlab.com/trungtv/vi_spacy/-/raw/master/packages/vi_core_news_lg-3.6.0/dist/vi_core_news_lg-3.6.0.tar.gz")
        return

    # Test texts for NER
    test_texts = [
        "Cộng đồng xử lý ngôn ngữ tự nhiên",
        "Nguyễn Văn A sống tại Hà Nội",
        "Công ty TNHH ABC được thành lập năm 2020",
        "Bệnh viện Việt Đức ở số 40 Tràng Thi, Hoàn Kiếm, Hà Nội",
        "Ông Trần Văn B là giám đốc của công ty XYZ",
        "Tôi tên là Lê Thị C, sinh ngày 15/06/1990 tại TP.HCM",
        "Số điện thoại: 0987654321, email: example@gmail.com",
        "Giấy phép kinh doanh số 123456789 do Sở KHĐT Hà Nội cấp"
    ]

    print("\n--- TOKENIZATION & POS TAGGING ---")
    doc = nlp('Cộng đồng xử lý ngôn ngữ tự nhiên')
    for token in doc:
        print(f"{token.text:15} {token.lemma_:15} {token.pos_:8} {token.tag_:8} {token.dep_:10} {token.shape_:10} {token.is_alpha} {token.is_stop}")

    print("\n--- NAMED ENTITY RECOGNITION ---")
    for i, text in enumerate(test_texts, 1):
        print(f"\nTest {i}: {text}")
        doc = nlp(text)

        if doc.ents:
            for ent in doc.ents:
                print(f"  {ent.label_:15} '{ent.text}' ({ent.start_char}-{ent.end_char})")
        else:
            print("  No entities found")

    print("\n--- AVAILABLE ENTITY TYPES ---")
    # Show all available entity labels
    labels = nlp.get_pipe("ner").labels
    print(f"Available NER labels: {sorted(labels)}")

    print("\n=== DEMO COMPLETE ===")

def demo_custom_ner_training():
    """Demo how to train custom NER model"""

    print("\n=== CUSTOM NER TRAINING DEMO ===\n")

    print("To train a custom NER model for Vietnamese medical documents:")
    print()
    print("1. Prepare training data in spaCy format:")
    print("""
    TRAIN_DATA = [
        ("Nguyễn Văn A là bệnh nhân nam", {"entities": [(0, 11, "PERSON"), (26, 28, "GENDER")]}),
        ("Bệnh viện Việt Đức ở Hà Nội", {"entities": [(0, 18, "ORG"), (22, 29, "GPE")]}),
    ]
    """)

    print("2. Create and train model:")
    print("""
    import spacy
    from spacy.training import Example

    # Load base model
    nlp = spacy.load('vi_core_news_lg')

    # Add custom entity labels
    ner = nlp.get_pipe("ner")
    ner.add_label("MEDICAL_ORG")
    ner.add_label("PATIENT_NAME")
    ner.add_label("DIAGNOSIS")

    # Training loop
    optimizer = nlp.begin_training()
    for epoch in range(100):
        losses = {}
        for text, annotations in TRAIN_DATA:
            doc = nlp.make_doc(text)
            example = Example.from_dict(doc, annotations)
            nlp.update([example], losses=losses)
        print(f"Epoch {epoch}, Losses: {losses}")
    """)

    print("3. Save and use the trained model:")
    print("""
    nlp.to_disk("custom_vi_ner")
    # Load later: nlp = spacy.load("custom_vi_ner")
    """)

if __name__ == "__main__":
    demo_vi_spacy()
    demo_custom_ner_training()