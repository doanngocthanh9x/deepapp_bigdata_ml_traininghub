"""
Response caching system using SQLite to avoid gRPC buffer overflow
Instead of sending large data through gRPC, store in SQLite and return query ID
"""

import sqlite3
import json
import os
import time
from datetime import datetime
from typing import Any, Optional

class ResponseCache:
    """Cache large responses in SQLite database"""

    def __init__(self, db_path: str = "/tmp/grpc_response_cache.db"):
        self.db_path = db_path
        self._init_database()

    def _init_database(self):
        """Initialize SQLite database"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()

        # Create response cache table
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS response_cache (
                response_id TEXT PRIMARY KEY,
                data TEXT NOT NULL,
                created_at REAL NOT NULL,
                expires_at REAL NOT NULL,
                size_bytes INTEGER NOT NULL
            )
        """)

        # Create index on expiration time for cleanup
        cursor.execute("""
            CREATE INDEX IF NOT EXISTS idx_expires_at
            ON response_cache(expires_at)
        """)

        conn.commit()
        conn.close()

    def store_response(self, data: Any, ttl_seconds: int = 300) -> str:
        """
        Store response data in SQLite and return query ID

        Args:
            data: Response data (will be JSON serialized)
            ttl_seconds: Time to live in seconds (default 5 minutes)

        Returns:
            response_id: Unique ID to retrieve this response
        """
        # Generate unique response ID
        timestamp = int(time.time() * 1000)
        response_id = f"resp_{timestamp}_{os.getpid()}"

        # Serialize data
        json_data = json.dumps(data, ensure_ascii=True)
        size_bytes = len(json_data)

        # Calculate expiration time
        created_at = time.time()
        expires_at = created_at + ttl_seconds

        # Store in database
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()

        cursor.execute("""
            INSERT INTO response_cache
            (response_id, data, created_at, expires_at, size_bytes)
            VALUES (?, ?, ?, ?, ?)
        """, (response_id, json_data, created_at, expires_at, size_bytes))

        conn.commit()
        conn.close()

        print(f"[ResponseCache] Stored response {response_id} ({size_bytes} bytes, TTL: {ttl_seconds}s)")

        # Cleanup old entries (async)
        self._cleanup_expired()

        return response_id

    def get_response(self, response_id: str) -> Optional[dict]:
        """
        Retrieve response data by ID

        Args:
            response_id: The response ID to retrieve

        Returns:
            Response data as dict, or None if not found/expired
        """
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()

        cursor.execute("""
            SELECT data, expires_at
            FROM response_cache
            WHERE response_id = ?
        """, (response_id,))

        row = cursor.fetchone()
        conn.close()

        if not row:
            return None

        json_data, expires_at = row

        # Check if expired
        if time.time() > expires_at:
            self._delete_response(response_id)
            return None

        return json.loads(json_data)

    def _delete_response(self, response_id: str):
        """Delete a specific response"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        cursor.execute("DELETE FROM response_cache WHERE response_id = ?", (response_id,))
        conn.commit()
        conn.close()

    def _cleanup_expired(self):
        """Remove expired responses"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()

        current_time = time.time()
        cursor.execute("DELETE FROM response_cache WHERE expires_at < ?", (current_time,))

        deleted_count = cursor.rowcount
        conn.commit()
        conn.close()

        if deleted_count > 0:
            print(f"[ResponseCache] Cleaned up {deleted_count} expired responses")

    def get_stats(self) -> dict:
        """Get cache statistics"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()

        cursor.execute("""
            SELECT
                COUNT(*) as total_responses,
                SUM(size_bytes) as total_size_bytes,
                AVG(size_bytes) as avg_size_bytes
            FROM response_cache
        """)

        row = cursor.fetchone()
        conn.close()

        if row:
            return {
                "total_responses": row[0],
                "total_size_bytes": row[1] or 0,
                "total_size_kb": (row[1] or 0) / 1024,
                "avg_size_bytes": row[2] or 0
            }

        return {
            "total_responses": 0,
            "total_size_bytes": 0,
            "total_size_kb": 0,
            "avg_size_bytes": 0
        }


# Global instance
_response_cache = None

def get_response_cache() -> ResponseCache:
    """Get global response cache instance"""
    global _response_cache
    if _response_cache is None:
        _response_cache = ResponseCache()
    return _response_cache
