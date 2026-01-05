# !pip install llama-cpp-python

from llama_cpp import Llama
llm = Llama.from_pretrained(
	repo_id="vilm/vinallama-7b-chat-GGUF",
	filename="vinallama-7b-chat_q5_0.gguf",
)

prompt = """<|im_start|>user
Tên người bệnh? chỉ cần trả về họ tên đầy đủ - Họ tên người bệnh: nguyễn quốc tỉnh
Ngày sinh: 20/11/1978<|im_end|>
<|im_start|>assistant
"""
output = llm(prompt, max_tokens=200, temperature=0.1)
print(output["choices"][0]["text"])
