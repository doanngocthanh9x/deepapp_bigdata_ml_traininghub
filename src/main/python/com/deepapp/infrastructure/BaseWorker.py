"""
Base class for all Python workers
Each worker must inherit from this and implement process_task()
"""

import abc
from typing import Optional, Any
import json
import time
from datetime import datetime


class GrpcWorkerClient:
    """Forward declaration for gRPC client"""
    pass


class BaseWorker(abc.ABC):
    """
    Base class for all workers
    Each worker must inherit from this and implement process_task()
    """

    def __init__(self, worker_id: str):
        self.worker_id = worker_id
        self.grpc_client: Optional[GrpcWorkerClient] = None

    def set_grpc_client(self, client: GrpcWorkerClient) -> None:
        """Set the gRPC client (called by infrastructure)"""
        self.grpc_client = client

    def process_task_wrapper(self, event_type: str, payload: str) -> str:
        """
        Wrapper that checks for cached requests and delegates to actual processor
        """
        try:
            # Check if this is a cached request reference
            data = json.loads(payload)
            if isinstance(data, dict) and data.get('cached_request') == True:
                cache_id = data.get('cache_id')
                if cache_id:
                    self.log(f"📦 Detected cached request: {cache_id}")

                    # Read actual payload from SQLite cache
                    from .ResponseCache import get_response_cache
                    cache = get_response_cache()
                    cached_data = cache.get_response(cache_id)

                    if cached_data:
                        actual_payload = cached_data.get('payload')
                        self.log(f"✅ Retrieved cached request ({len(actual_payload)} bytes)")

                        # Process with actual payload
                        return self.process_task(event_type, actual_payload)
                    else:
                        self.log(f"❌ Failed to retrieve cached request: {cache_id}", "ERROR")
                        return self.create_response("error", f"Cached request not found: {cache_id}")
        except (json.JSONDecodeError, TypeError):
            # Not JSON or not a dict - process normally
            pass

        # Normal request - process directly
        return self.process_task(event_type, payload)

    @abc.abstractmethod
    def process_task(self, event_type: str, payload: str) -> str:
        """
        Process a task and return result
        @param event_type: The type of event/task
        @param payload: The task payload
        @return: Result as string (JSON)
        """
        pass

    def get_worker_id(self) -> str:
        """Get worker ID (used for routing)"""
        return self.worker_id

    def can_handle(self, event_type: str) -> bool:
        """
        Check if this worker can handle the given event type
        Default: handle all events
        """
        return True

    def create_response(self, status: str, data: Any, **kwargs) -> str:
        """Helper method to create standardized JSON response"""
        if status == "error":
            response = {"error": data}
        else:
            response = {
                "status": status,
                "data": data
            }
        response.update(kwargs)
        # Use ensure_ascii=True to avoid gRPC protobuf encoding issues with Unicode
        result = json.dumps(response, ensure_ascii=True)

        # Log response size for debugging
        response_size = len(result)
        if response_size > 100000:  # > 100KB
            self.log(f"⚠️  Large response: {response_size} bytes ({response_size/1024:.1f} KB)", "WARNING")

        # CRITICAL: Limit response size to prevent gRPC buffer overflow
        # gRPC default max message size is 4MB, but we limit to 1MB for safety
        MAX_RESPONSE_SIZE = 1024 * 1024  # 1MB
        if response_size > MAX_RESPONSE_SIZE:
            self.log(f"❌ Response too large: {response_size} bytes - truncating", "ERROR")
            # Return error instead of huge response
            return json.dumps({
                "status": "error",
                "error": f"Response too large ({response_size} bytes). Please reduce page_size or use pagination."
            }, ensure_ascii=True)

        return result

    def log(self, message: str, level: str = "INFO") -> None:
        """Helper method for logging"""
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        print(f"[{timestamp}] [{level}] [{self.worker_id}] {message}")