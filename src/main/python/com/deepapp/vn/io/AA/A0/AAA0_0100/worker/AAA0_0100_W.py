"""
Example Python Worker: AAA0_0100_W
Demonstrates how to create a worker that auto-registers
"""

import json
from datetime import datetime
from com.deepapp.infrastructure.BaseWorker import BaseWorker
from com.deepapp.infrastructure.WorkerRegistry import register_worker


@register_worker("AAA0_0100_W")
class AAA0_0100_Worker(BaseWorker):
    """
    Example Worker: AAA0_0100_W
    Demonstrates basic worker functionality
    """

    def __init__(self):
        super().__init__("AAA0_0100_Worker")
        self.log("Initialized")

    def process_task(self, event_type: str, payload: str) -> str:
        """Process a task and return result"""
        self.log(f"Processing task: event_type='{event_type}', payload='{payload}'")

        if event_type == "echo":
            return self._echo(payload)
        elif event_type == "process":
            return self._process_data(payload)
        elif event_type == "transform":
            return self._transform_data(payload)
        else:
            return self.create_response("unknown_event",
                f"Event type '{event_type}' not supported")

    def can_handle(self, event_type: str) -> bool:
        """Check if this worker can handle the given event type"""
        return event_type in ["echo", "process", "transform"]

    def _echo(self, payload: str) -> str:
        """Echo the payload back"""
        self.log(f"Echoing payload: {payload}")
        return self.create_response("success", payload)

    def _process_data(self, payload: str) -> str:
        """Process data (example: convert to uppercase)"""
        try:
            result = payload.upper()
            self.log(f"Processed data: '{payload}' -> '{result}'")
            return self.create_response("processed", result)
        except Exception as e:
            self.log(f"Error processing data: {e}", "ERROR")
            return self.create_response("error", str(e))

    def _transform_data(self, payload: str) -> str:
        """Transform data (example: add timestamp)"""
        try:
            now = datetime.now()
            result = {
                "timestamp": now.isoformat(),
                "data": payload,
                "processed_by": self.worker_id
            }
            json_result = json.dumps(result, ensure_ascii=False)
            self.log(f"Transformed data: {json_result}")
            return self.create_response("transformed", result)
        except Exception as e:
            self.log(f"Error transforming data: {e}", "ERROR")
            return self.create_response("error", str(e))