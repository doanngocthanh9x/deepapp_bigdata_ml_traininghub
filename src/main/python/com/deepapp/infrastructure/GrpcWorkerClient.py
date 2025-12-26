"""
gRPC Worker Client for Python workers
Handles communication with the main Java application
"""

import grpc
import time
import threading
import json
from typing import Optional, Callable, Any, Dict
from concurrent import futures
import queue
import sys
import os

# Add proto path
proto_path = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', '..', '..', 'src', 'main', 'python'))
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
                self.metadata = {}

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
        self.message_queue = queue.Queue()
        self.message_handlers: Dict[str, Callable] = {}

    def connect(self) -> bool:
        """Connect to gRPC server"""
        try:
            # Check if we have real gRPC or dummy classes
            if hasattr(hub_pb2_grpc, 'DataStreamStub') and hasattr(hub_pb2_grpc.DataStreamStub, '__init__'):
                # Real gRPC classes - configure with keepalive options like Java
                options = [
                    ('grpc.keepalive_time_ms', 10 * 60 * 1000),  # 10 minutes
                    ('grpc.keepalive_timeout_ms', 30 * 1000),    # 30 seconds
                    ('grpc.keepalive_permit_without_calls', 0),  # Only keepalive during active calls
                    ('grpc.http2.max_pings_without_data', 0),    # Disable pings without data
                    ('grpc.max_receive_message_length', 200 * 1024 * 1024),  # 200MB
                ]
                self.channel = grpc.insecure_channel(f"{self.host}:{self.port}", options=options)
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

    def send_message(self, target_id: str, event_type: str, payload: str, metadata: Dict[str, str] = None) -> bool:
        """Send a message to another client"""
        if not self.connected or not self.running:
            print("[GrpcWorkerClient] Not connected or not running, cannot send message")
            return False

        try:
            # Create message
            message = hub_pb2.EventChunk()
            message.sender_id = self.client_id
            message.target_id = target_id
            message.event_type = event_type
            # Convert payload to bytes if it's a string
            if isinstance(payload, str):
                message.payload = payload.encode('utf-8')
            else:
                message.payload = payload
            message.timestamp = int(time.time() * 1000)
            
            # Add metadata if provided
            if metadata:
                for key, value in metadata.items():
                    message.metadata[key] = value

            # Put message in queue for the generator to send
            self.message_queue.put(message)
            print(f"[GrpcWorkerClient] Queued message: {event_type} to {target_id}")
            return True

        except Exception as e:
            print(f"[GrpcWorkerClient] Failed to queue message: {e}")
            return False

    def register_message_handler(self, event_type: str, handler: Callable) -> None:
        """Register a handler for specific event types"""
        self.message_handlers[event_type] = handler
        print(f"[GrpcWorkerClient] Registered handler for event: {event_type}")

    def start_listening(self) -> None:
        """Start listening for incoming messages using bidirectional streaming"""
        if not self.connected:
            print("[GrpcWorkerClient] Not connected, cannot start listening")
            return

        self.running = True
        print("[GrpcWorkerClient] Started listening for messages")

        def request_generator():
            """Generator that yields messages to send"""
            # Send initial connect message
            connect_message = hub_pb2.EventChunk()
            connect_message.sender_id = self.client_id
            connect_message.target_id = "server"
            connect_message.event_type = "connect"
            connect_message.payload = b"connect"
            connect_message.timestamp = int(time.time() * 1000)
            print(f"[GrpcWorkerClient] Sending connect message for client '{self.client_id}'")
            yield connect_message
            
            # Wait for messages to send
            while self.running:
                try:
                    # Wait for a message with timeout
                    message = self.message_queue.get(timeout=1.0)
                    print(f"[GrpcWorkerClient] Sending message: {message.event_type} to {message.target_id}")
                    yield message
                except queue.Empty:
                    # Don't send keepalives for now
                    continue

        def listen_loop():
            try:
                # Create metadata with client ID
                metadata = [('client-id', self.client_id)]
                
                # Start bidirectional streaming with metadata
                responses = self.stub.StreamEvents(request_generator(), metadata=metadata)
                
                # Read responses
                for response in responses:
                    if not self.running:
                        break
                    self._handle_event(response)
                    
            except Exception as e:
                print(f"[GrpcWorkerClient] Error in listen loop: {e}")
                import traceback
                traceback.print_exc()
                self.connected = False

        thread = threading.Thread(target=listen_loop, daemon=True)
        thread.start()

    def _handle_event(self, event) -> None:
        """Handle incoming event from gRPC stream"""
        try:
            sender_id = getattr(event, 'sender_id', '')
            target_id = getattr(event, 'target_id', '')
            event_type = getattr(event, 'event_type', '')
            payload = getattr(event, 'payload', '')
            timestamp = getattr(event, 'timestamp', 0)
            
            print(f"[GrpcWorkerClient] Received event:")
            print(f"  From: {sender_id}")
            print(f"  To: {target_id}")
            print(f"  Type: {event_type}")
            print(f"  Payload: {payload[:100]}..." if len(payload) > 100 else f"  Payload: {payload}")
            
            # Get task_id from metadata or target_id
            task_id = target_id
            request_id = None
            
            # Check for metadata
            metadata = {}
            if hasattr(event, 'metadata'):
                metadata = getattr(event, 'metadata', {})
                if isinstance(metadata, dict):
                    if 'taskId' in metadata:
                        task_id = metadata['taskId']
                        print(f"  TaskId (from metadata): {task_id}")
                    if 'requestId' in metadata:
                        request_id = metadata['requestId']
                        print(f"  RequestId: {request_id}")
                else:
                    # Handle protobuf map
                    for key in metadata:
                        if key == 'taskId':
                            task_id = metadata[key]
                            print(f"  TaskId (from metadata): {task_id}")
                        elif key == 'requestId':
                            request_id = metadata[key]
                            print(f"  RequestId: {request_id}")
            else:
                print(f"  TaskId (from targetId): {task_id}")
            
            # Route to appropriate worker
            from .WorkerRegistry import get_registry
            registry = get_registry()
            
            if task_id in registry.workers:
                worker = registry.workers[task_id]
                if worker.can_handle(event_type):
                    print(f"[GrpcWorkerClient] Routing {event_type} to worker {task_id}")
                    
                    try:
                        result = worker.process_task(event_type, payload)
                        
                        # Send response back with the same requestId
                        response_metadata = {}
                        if request_id:
                            response_metadata['requestId'] = request_id
                        
                        self.send_message(sender_id, "response", result, response_metadata)
                        print(f"[GrpcWorkerClient] Sent response to {sender_id} for request {request_id}")
                        
                    except Exception as worker_error:
                        error_msg = json.dumps({
                            "error": f"Worker processing failed: {str(worker_error)}"
                        })
                        response_metadata = {}
                        if request_id:
                            response_metadata['requestId'] = request_id
                        
                        self.send_message(sender_id, "response", error_msg, response_metadata)
                        print(f"[GrpcWorkerClient] Sent error response to {sender_id}")
                        
                else:
                    print(f"[GrpcWorkerClient] Worker {task_id} cannot handle event {event_type}")
                    error_response = json.dumps({
                        "error": f"Event type '{event_type}' not supported by worker {task_id}"
                    })
                    response_metadata = {}
                    if request_id:
                        response_metadata['requestId'] = request_id
                    
                    self.send_message(sender_id, "response", error_response, response_metadata)
            else:
                print(f"[GrpcWorkerClient] No worker found for task_id: {task_id}")
                error_response = json.dumps({
                    "error": f"Worker not found: {task_id}"
                })
                response_metadata = {}
                if request_id:
                    response_metadata['requestId'] = request_id
                
                self.send_message(sender_id, "response", error_response, response_metadata)
                
        except Exception as e:
            print(f"[GrpcWorkerClient] Error handling event: {e}")
            import traceback
            traceback.print_exc()

    def stop_listening(self) -> None:
        """Stop listening for messages"""
        self.running = False
        print("[GrpcWorkerClient] Stopped listening")


class WorkerManager:
    """
    Manages Python workers and their lifecycle
    """

    def __init__(self, host: str = "72.60.111.138", port: int = 50051, client_id: str = "python-worker"):
        self.grpc_client = GrpcWorkerClient(host, port, client_id)
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