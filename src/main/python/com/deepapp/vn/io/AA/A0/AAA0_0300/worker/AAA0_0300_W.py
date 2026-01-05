"""
LLM Inference Worker: AAA0_0300_W
Vietnamese Language Model Inference using llama-cpp-python
"""

import json
import os
from typing import Dict, Any
from datetime import datetime

from com.deepapp.infrastructure.BaseWorker import BaseWorker
from com.deepapp.infrastructure.WorkerRegistry import register_worker

# Import llama-cpp-python
try:
    from llama_cpp import Llama
    LLAMA_CPP_AVAILABLE = True
except ImportError:
    LLAMA_CPP_AVAILABLE = False
    print("⚠️  llama-cpp-python not installed. Install with: pip install llama-cpp-python")


@register_worker("AAA0_0300_W")
class AAA0_0300_Worker(BaseWorker):
    """LLM Inference Worker using llama-cpp-python"""

    def __init__(self):
        super().__init__("AAA0_0300_Worker")
        self.log("=== LLM Inference Worker (Python) ===")

        # Model cache
        self.models = {}
        self.default_model_path = "/root/models/vinallama-7b-chat_q5_0.gguf"
        
        # Statistics
        self.inference_count = 0
        self.total_tokens = 0

        if not LLAMA_CPP_AVAILABLE:
            self.log("⚠️  llama-cpp-python not available", "WARNING")
        else:
            self.log("✓ llama-cpp-python available")

        self.log("✓ Worker initialized")

    def process_task(self, event_type: str, payload: str) -> str:
        """Process LLM inference tasks"""
        self.log(f"📨 Event: {event_type}")

        if not LLAMA_CPP_AVAILABLE:
            return self.create_response("error", 
                "llama-cpp-python not installed. Install with: pip install llama-cpp-python")

        handlers = {
            "inference": self._run_inference,
            "load_model": self._load_model,
            "unload_model": self._unload_model,
            "list_models": self._list_models,
            "get_stats": self._get_stats,
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

    def _run_inference(self, payload: str) -> str:
        """Run LLM inference"""
        try:
            data = json.loads(payload)
            prompt = data.get("prompt", "")
            temperature = data.get("temperature", 0.1)
            max_tokens = data.get("max_tokens", 200)
            model_name = data.get("model_name", "vinallama-7b-chat")
            chat_history = data.get("chat_history", [])

            self.log(f"🤖 Running inference - Model: {model_name}, Tokens: {max_tokens}")

            # Load model if not loaded
            if model_name not in self.models:
                self._load_model_internal(model_name)

            llm = self.models.get(model_name)
            if not llm:
                return self.create_response("error", f"Model {model_name} not available")

            # Build prompt with chat history
            full_prompt = self._build_prompt(prompt, chat_history)
            
            self.log(f"📝 Prompt length: {len(full_prompt)} chars")

            # Run inference
            start_time = datetime.now()
            
            output = llm(
                full_prompt,
                max_tokens=max_tokens,
                temperature=temperature,
                stop=["<|im_end|>", "</s>"],
                echo=False
            )

            inference_time = (datetime.now() - start_time).total_seconds()
            
            # Extract response
            response_text = output["choices"][0]["text"].strip()
            tokens = output["usage"]["completion_tokens"]

            # Update statistics
            self.inference_count += 1
            self.total_tokens += tokens

            self.log(f"✅ Inference completed in {inference_time:.2f}s, {tokens} tokens")

            return json.dumps({
                "status": "success",
                "response": response_text,
                "tokens": tokens,
                "inference_time": inference_time,
                "model": model_name
            })

        except Exception as e:
            self.log(f"❌ Inference failed: {e}", "ERROR")
            import traceback
            self.log(traceback.format_exc(), "ERROR")
            return self.create_response("error", str(e))

    def _build_prompt(self, prompt: str, chat_history: list) -> str:
        """Build prompt with chat history"""
        # VinAllama chat template format
        full_prompt = ""
        
        # Add chat history
        for msg in chat_history:
            role = msg.get("role", "user")
            content = msg.get("content", "")
            
            if role == "user":
                full_prompt += f"<|im_start|>user\n{content}<|im_end|>\n"
            elif role == "assistant":
                full_prompt += f"<|im_start|>assistant\n{content}<|im_end|>\n"
        
        # Add current prompt
        full_prompt += f"<|im_start|>user\n{prompt}<|im_end|>\n<|im_start|>assistant\n"
        
        return full_prompt

    def _load_model(self, payload: str) -> str:
        """Load a model"""
        try:
            data = json.loads(payload)
            model_name = data.get("model_name", "vinallama-7b-chat")
            
            self._load_model_internal(model_name)
            
            return self.create_response("success", f"Model {model_name} loaded")
        except Exception as e:
            return self.create_response("error", str(e))

    def _load_model_internal(self, model_name: str) -> None:
        """Internal method to load model"""
        if model_name in self.models:
            self.log(f"Model {model_name} already loaded")
            return

        self.log(f"🔄 Loading model: {model_name}")

        # Map model names to file paths
        model_paths = {
            "vinallama-7b-chat": "/root/models/vinallama-7b-chat_q5_0.gguf",
            "vietcuna-7b": "/root/models/vietcuna-7b-q5_k_m.gguf",
            "phobert-base": "/root/models/phobert-base.gguf",
        }

        model_path = model_paths.get(model_name, self.default_model_path)

        # Check if model file exists
        if not os.path.exists(model_path):
            self.log(f"⚠️  Model file not found: {model_path}", "WARNING")
            self.log(f"Attempting to download from HuggingFace...")
            
            # Try to download from HuggingFace
            try:
                llm = Llama.from_pretrained(
                    repo_id="vilm/vinallama-7b-chat-GGUF",
                    filename="vinallama-7b-chat_q5_0.gguf",
                    n_ctx=2048,
                    n_threads=4,
                    verbose=False
                )
            except Exception as e:
                raise Exception(f"Failed to download model: {e}")
        else:
            # Load from local file
            llm = Llama(
                model_path=model_path,
                n_ctx=2048,
                n_threads=4,
                verbose=False
            )

        self.models[model_name] = llm
        self.log(f"✅ Model {model_name} loaded successfully")

    def _unload_model(self, payload: str) -> str:
        """Unload a model"""
        try:
            data = json.loads(payload)
            model_name = data.get("model_name", "vinallama-7b-chat")
            
            if model_name in self.models:
                del self.models[model_name]
                self.log(f"Model {model_name} unloaded")
                return self.create_response("success", f"Model {model_name} unloaded")
            else:
                return self.create_response("error", f"Model {model_name} not loaded")
        except Exception as e:
            return self.create_response("error", str(e))

    def _list_models(self) -> str:
        """List loaded models"""
        loaded_models = list(self.models.keys())
        return json.dumps({
            "status": "success",
            "loaded_models": loaded_models,
            "available_models": [
                "vinallama-7b-chat",
                "vietcuna-7b",
                "phobert-base"
            ]
        })

    def _get_stats(self) -> str:
        """Get inference statistics"""
        return json.dumps({
            "status": "success",
            "inference_count": self.inference_count,
            "total_tokens": self.total_tokens,
            "avg_tokens": self.total_tokens / self.inference_count if self.inference_count > 0 else 0,
            "loaded_models": list(self.models.keys())
        })


# Test function
def test_worker():
    """Test the worker"""
    worker = AAA0_0300_Worker()
    
    # Test inference
    payload = json.dumps({
        "prompt": "Tên người bệnh? chỉ cần trả về họ tên đầy đủ - Họ tên người bệnh: nguyễn quốc tỉnh\nNgày sinh: 20/11/1978",
        "temperature": 0.1,
        "max_tokens": 50,
        "model_name": "vinallama-7b-chat"
    })
    
    result = worker.process_task("inference", payload)
    print(f"Result: {result}")


if __name__ == "__main__":
    test_worker()
