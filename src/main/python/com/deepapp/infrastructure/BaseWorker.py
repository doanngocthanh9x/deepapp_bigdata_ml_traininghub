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
        response = {
            "worker": self.worker_id,
            "status": status,
            "data": data,
            "timestamp": int(time.time())
        }
        response.update(kwargs)
        return json.dumps(response, ensure_ascii=False)

    def log(self, message: str, level: str = "INFO") -> None:
        """Helper method for logging"""
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        print(f"[{timestamp}] [{level}] [{self.worker_id}] {message}")