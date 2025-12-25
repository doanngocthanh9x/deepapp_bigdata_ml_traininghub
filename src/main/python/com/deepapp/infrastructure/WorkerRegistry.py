"""
Registry for all Python workers
Automatically registers workers at startup
"""

import json
from typing import Dict, Optional, Callable, Any
from .BaseWorker import BaseWorker

WorkerPtr = BaseWorker


class WorkerRegistry:
    """
    Registry for all workers
    Automatically registers workers at startup
    """

    _instance = None

    def __init__(self):
        self.workers: Dict[str, WorkerPtr] = {}

    @classmethod
    def instance(cls) -> 'WorkerRegistry':
        """Singleton instance"""
        if cls._instance is None:
            cls._instance = cls()
        return cls._instance

    def register_worker(self, task_id: str, worker: WorkerPtr) -> None:
        """Register a worker"""
        self.workers[task_id] = worker
        print(f"[WorkerRegistry] Registered worker: {task_id} -> {worker.get_worker_id()}")

    def get_worker(self, task_id: str) -> Optional[WorkerPtr]:
        """Get worker by task ID"""
        return self.workers.get(task_id)

    def process_task(self, task_id: str, event_type: str, payload: str) -> str:
        """
        Process a task by routing to appropriate worker
        """
        worker = self.get_worker(task_id)
        if not worker:
            error_msg = f"No worker found for task_id: {task_id}"
            print(f"[WorkerRegistry] ERROR: {error_msg}")
            return json.dumps({
                "error": error_msg,
                "task_id": task_id,
                "event_type": event_type,
                "timestamp": int(__import__('time').time())
            })

        try:
            # Check if worker can handle this event type
            if not worker.can_handle(event_type):
                error_msg = f"Worker {task_id} cannot handle event type: {event_type}"
                print(f"[WorkerRegistry] ERROR: {error_msg}")
                return json.dumps({
                    "error": error_msg,
                    "task_id": task_id,
                    "event_type": event_type,
                    "timestamp": int(__import__('time').time())
                })

            # Process the task
            print(f"[WorkerRegistry] Processing task {task_id} with event '{event_type}'")
            result = worker.process_task(event_type, payload)
            print(f"[WorkerRegistry] Task {task_id} completed successfully")
            return result

        except Exception as e:
            error_msg = f"Error processing task {task_id}: {str(e)}"
            print(f"[WorkerRegistry] ERROR: {error_msg}")
            return json.dumps({
                "error": error_msg,
                "task_id": task_id,
                "event_type": event_type,
                "exception": str(e),
                "timestamp": int(__import__('time').time())
            })

    def list_workers(self) -> Dict[str, str]:
        """List all registered workers"""
        return {task_id: worker.get_worker_id() for task_id, worker in self.workers.items()}

    def get_worker_count(self) -> int:
        """Get total number of registered workers"""
        return len(self.workers)


# Global registry instance
_registry = WorkerRegistry()

def register_worker(task_id: str) -> Callable:
    """
    Decorator function to register a worker
    Usage: @register_worker("WORKER_ID")
    """
    def decorator(cls):
        # Create instance and register
        worker_instance = cls()
        _registry.register_worker(task_id, worker_instance)
        return cls
    return decorator


def get_registry() -> WorkerRegistry:
    """Get the global worker registry"""
    return _registry