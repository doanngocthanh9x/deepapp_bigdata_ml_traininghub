#!/usr/bin/env python3
"""
Main entry point for Python workers
Similar to C++ main.cpp but for Python workers
"""

import sys
import os
import time
import signal
import argparse
from typing import Optional

# Add the project root to Python path
project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', '..', '..'))
sys.path.insert(0, project_root)

from com.deepapp.infrastructure.GrpcWorkerClient import WorkerManager
from com.deepapp.infrastructure.WorkerRegistry import get_registry


class PythonWorkerApplication:
    """Main application class for Python workers"""

    def __init__(self, client_id: str = "python-worker", host: str = "72.60.111.138", port: int = 50051):
        self.client_id = client_id
        self.host = host
        self.port = port
        self.worker_manager: Optional[WorkerManager] = None
        self.running = False

    def initialize(self) -> bool:
        """Initialize the application"""
        print("=" * 50)
        print("DeepApp Python Worker Application")
        print("=" * 50)
        print(f"Configuration:")
        print(f"  Server: {self.host}:{self.port}")
        print(f"  Client ID: {self.client_id}")
        print()

        # Create worker manager
        self.worker_manager = WorkerManager(
            host=self.host,
            port=self.port,
            client_id=self.client_id
        )

        # Load all workers (this will trigger registration)
        self._load_workers()

        return True

    def _load_workers(self) -> None:
        """Load all worker modules"""
        print("Loading workers...")

        # Import worker modules to trigger registration
        try:
            # Import AA workers
            from com.deepapp.vn.io.AA.A0.AAA0_0100.worker import AAA0_0100_W
            from com.deepapp.vn.io.AA.A0.AAA0_0101.worker import AAA0_0101_W
            from com.deepapp.vn.io.AA.A0.AAA0_0102.worker import AAA0_0102_W
            from com.deepapp.vn.io.AA.A0.AAA0_0200.worker import AAA0_0200_W
            from com.deepapp.vn.io.AA.A0.AAA0_0201.worker import AAA0_0201_W
            from com.deepapp.vn.io.AA.A0.AAA0_0202.worker import AAA0_0202_W
            from com.deepapp.vn.io.AA.A0.AAA0_0203.worker import AAA0_0203_W

            # Import ZZ workers
            from com.deepapp.vn.io.ZZ.A0.ZZA0_0100.worker import ZZA0_0100_W

            print("All worker modules loaded successfully")

        except ImportError as e:
            print(f"Warning: Some worker modules could not be loaded: {e}")

        # Show registered workers
        registry = get_registry()
        workers = registry.list_workers()
        print(f"Worker Registry Status:")
        print(f"  Total workers: {len(workers)}")
        if workers:
            print("  ✓ Registered workers:")
            for task_id, worker_id in workers.items():
                print(f"    - {task_id} ({worker_id})")
        print()

    def start(self) -> bool:
        """Start the application"""
        if not self.initialize():
            return False

        print("Starting worker manager...")
        if not self.worker_manager or not self.worker_manager.start():
            print("Failed to start worker manager")
            return False

        self.running = True
        print("Python Worker Application started successfully")
        print("=" * 50)
        print()

        # Setup signal handlers
        signal.signal(signal.SIGINT, self._signal_handler)
        signal.signal(signal.SIGTERM, self._signal_handler)

        # Keep running
        try:
            while self.running:
                time.sleep(1)
        except KeyboardInterrupt:
            pass

        return True

    def stop(self) -> None:
        """Stop the application"""
        print("\nStopping Python Worker Application...")
        self.running = False

        if self.worker_manager:
            self.worker_manager.stop()

        print("Python Worker Application stopped")

    def _signal_handler(self, signum, frame) -> None:
        """Handle shutdown signals"""
        print(f"\nReceived signal {signum}, shutting down...")
        self.stop()
        sys.exit(0)


def main():
    """Main entry point"""
    parser = argparse.ArgumentParser(description='DeepApp Python Worker Application')
    parser.add_argument('--client-id', default='python-worker',
                       help='Client ID for gRPC communication')
    parser.add_argument('--host', default='72.60.111.138',
                       help='gRPC server host')
    parser.add_argument('--port', type=int, default=50051,
                       help='gRPC server port')

    args = parser.parse_args()

    app = PythonWorkerApplication(
        client_id=args.client_id,
        host=args.host,
        port=args.port
    )

    try:
        if app.start():
            print("Application finished successfully")
        else:
            print("Application failed to start")
            sys.exit(1)
    except Exception as e:
        print(f"Application error: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()