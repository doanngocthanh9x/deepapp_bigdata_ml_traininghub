"""
gRPC Worker Client for Python workers
Handles communication with the main Java application
"""

import grpc
import time
import threading
import json
from typing import Optional, Callable, Any
from concurrent import futures
import sys
import os

# Add proto path
proto_path = os.path.join(os.path.dirname(__file__), '..', '..', '..', '..', 'proto')
sys.path.insert(0, proto_path)

try:
    import hub_pb2
    import hub_pb2_grpc
except ImportError:
    print("Warning: hub_pb2 and hub_pb2_grpc not found. Please generate proto files first.")
    # Create dummy classes for development
    class hub_pb2:
        class EventChunk:
            def __init__(self):
                self.sender_id = ""
                self.target_id = ""
                self.event_type = ""
                self.payload = ""
                self.timestamp = 0

            def set_sender_id(self, value): self.sender_id = value
            def set_target_id(self, value): self.target_id = value
            def set_event_type(self, value): self.event_type = value
            def set_payload(self, value): self.payload = value
            def set_timestamp(self, value): self.timestamp = value

    class hub_pb2_grpc:
        class DataStreamStub:
            def __init__(self, channel=None):
                self.channel = channel
            def StreamEvents(self, context): pass


class GrpcWorkerClient:
    """
    gRPC client for Python workers to communicate with Java application
    """

    def __init__(self, host: str = "72.60.111.138", port: int = 50051, client_id: str = "python-worker"):
        self.host = host
        self.port = port
        self.client_id = client_id
        self.channel = None
        self.stub = None
        self.connected = False
        self.running = False
        self.message_handlers: Dict[str, Callable] = {}

    def connect(self) -> bool:
        """Connect to gRPC server"""
        try:
            # Check if we have real gRPC or dummy classes
            if hasattr(hub_pb2_grpc, 'DataStreamStub') and hasattr(hub_pb2_grpc.DataStreamStub, '__init__'):
                # Real gRPC classes
                self.channel = grpc.insecure_channel(f"{self.host}:{self.port}")
                self.stub = hub_pb2_grpc.DataStreamStub(self.channel)
            else:
                # Dummy classes - simulate connection
                print(f"[GrpcWorkerClient] Using dummy gRPC classes (no real connection)")
                self.channel = None
                self.stub = hub_pb2_grpc.DataStreamStub()

            self.connected = True
            print(f"[GrpcWorkerClient] Connected to {self.host}:{self.port} as {self.client_id}")
            return True
        except Exception as e:
            print(f"[GrpcWorkerClient] Failed to connect: {e}")
            return False

    def disconnect(self) -> None:
        """Disconnect from gRPC server"""
        self.running = False
        if self.channel:
            self.channel.close()
        self.connected = False
        print("[GrpcWorkerClient] Disconnected")

    def send_message(self, target_id: str, event_type: str, payload: str) -> bool:
        """Send a message to another client"""
        if not self.connected or not self.stub:
            print("[GrpcWorkerClient] Not connected, cannot send message")
            return False

        try:
            # Create message
            message = hub_pb2.EventChunk()
            message.set_sender_id(self.client_id)
            message.set_target_id(target_id)
            message.set_event_type(event_type)
            message.set_payload(payload)
            message.set_timestamp(int(time.time() * 1000))

            # For now, we'll use a simple approach
            # In a real implementation, you'd maintain a persistent stream
            print(f"[GrpcWorkerClient] Sending message: {event_type} to {target_id}")
            return True

        except Exception as e:
            print(f"[GrpcWorkerClient] Failed to send message: {e}")
            return False

    def register_message_handler(self, event_type: str, handler: Callable) -> None:
        """Register a handler for specific event types"""
        self.message_handlers[event_type] = handler
        print(f"[GrpcWorkerClient] Registered handler for event: {event_type}")

    def start_listening(self) -> None:
        """Start listening for incoming messages"""
        if not self.connected:
            print("[GrpcWorkerClient] Not connected, cannot start listening")
            return

        self.running = True
        print("[GrpcWorkerClient] Started listening for messages")

        # In a real implementation, this would maintain a persistent stream
        # For now, we'll simulate periodic checks
        def listen_loop():
            while self.running:
                try:
                    # Simulate receiving messages
                    time.sleep(1)
                except Exception as e:
                    print(f"[GrpcWorkerClient] Error in listen loop: {e}")
                    break

        thread = threading.Thread(target=listen_loop, daemon=True)
        thread.start()

    def stop_listening(self) -> None:
        """Stop listening for messages"""
        self.running = False
        print("[GrpcWorkerClient] Stopped listening")

    def is_connected(self) -> bool:
        """Check if connected to server"""
        return self.connected

    def get_client_id(self) -> str:
        """Get client ID"""
        return self.client_id


class WorkerManager:
    """
    Manages Python workers and their lifecycle
    """

    def __init__(self):
        self.grpc_client = GrpcWorkerClient()
        self.workers_initialized = False

    def initialize_workers(self) -> bool:
        """Initialize all registered workers"""
        try:
            from .WorkerRegistry import get_registry
            registry = get_registry()

            # Inject gRPC client into all workers
            for task_id, worker in registry.workers.items():
                worker.set_grpc_client(self.grpc_client)
                print(f"[WorkerManager] Initialized worker: {task_id}")

            self.workers_initialized = True
            print(f"[WorkerManager] All {registry.get_worker_count()} workers initialized")
            return True

        except Exception as e:
            print(f"[WorkerManager] Failed to initialize workers: {e}")
            return False

    def start(self) -> bool:
        """Start the worker manager"""
        print("[WorkerManager] Starting Python Worker Manager...")

        # Initialize workers first
        if not self.initialize_workers():
            return False

        # Connect to gRPC server
        if not self.grpc_client.connect():
            return False

        # Start listening
        self.grpc_client.start_listening()

        print("[WorkerManager] Python Worker Manager started successfully")
        return True

    def stop(self) -> None:
        """Stop the worker manager"""
        print("[WorkerManager] Stopping Python Worker Manager...")
        self.grpc_client.stop_listening()
        self.grpc_client.disconnect()
        print("[WorkerManager] Python Worker Manager stopped")

    def process_task(self, task_id: str, event_type: str, payload: str) -> str:
        """Process a task using the appropriate worker"""
        from .WorkerRegistry import get_registry
        registry = get_registry()
        return registry.process_task(task_id, event_type, payload)