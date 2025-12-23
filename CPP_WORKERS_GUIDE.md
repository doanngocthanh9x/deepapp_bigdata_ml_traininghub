# DeepApp C++ Workers - Hướng Dẫn

## 🎯 Tổng Quan

Hệ thống C++ workers được thiết kế để:
- ✅ Auto-register workers tự động
- ✅ Start đồng thời với Java application
- ✅ Lắng nghe gRPC realtime với client_id = "cpp-worker"
- ✅ Route tasks theo target_id (tên file worker như AAA0_0100_W)
- ✅ Trả kết quả về Java qua gRPC

## 📁 Cấu Trúc

```
deepapp_main/
├── CMakeLists.txt                          # Build configuration
├── src/main/cpp/
│   ├── com/
│   │   ├── DeepappMainApplication.cpp      # Main C++ entry point
│   │   └── deepapp/
│   │       ├── infrastructure/             # Core infrastructure
│   │       │   ├── BaseWorker.h/.cpp       # Base worker class
│   │       │   ├── WorkerRegistry.h/.cpp   # Auto-registration
│   │       │   └── GrpcWorkerClient.h/.cpp # gRPC client
│   │       └── vn/io/AA/A0/
│   │           ├── AAA0_0100/worker/
│   │           │   └── AAA0_0100_W.cpp     # Worker example 1
│   │           └── AAA0_0200/worker/
│   │               └── AAA0_0200_W.cpp     # Worker example 2
├── build_cpp_workers.sh                    # Build script
├── run_cpp_workers.sh                      # Run C++ only
└── start_all.sh                            # Start Java + C++
```

## 🚀 Quick Start

### 1. Build C++ Workers
```bash
cd /root/deepapp/deepapp_main
./build_cpp_workers.sh
```

### 2. Run C++ Workers Only
```bash
./run_cpp_workers.sh [server_address] [client_id]

# Example:
./run_cpp_workers.sh 72.60.111.138:50051 cpp-worker
```

### 3. Start Both Java + C++
```bash
./start_all.sh
```

## 📝 Cách Tạo Worker Mới

### Bước 1: Tạo File Worker

Tạo file theo pattern: `**/worker/*_W.cpp`

Ví dụ: `src/main/cpp/com/deepapp/vn/io/BB/B1/BBB1_0100/worker/BBB1_0100_W.cpp`

```cpp
#include "com/deepapp/infrastructure/BaseWorker.h"
#include "com/deepapp/infrastructure/WorkerRegistry.h"
#include <iostream>

namespace deepapp {
namespace workers {

class BBB1_0100_Worker : public infrastructure::BaseWorker {
public:
    BBB1_0100_Worker() : BaseWorker("BBB1_0100_Worker") {
        std::cout << "[BBB1_0100_Worker] Initialized" << std::endl;
    }

    std::string processTask(const std::string& event_type, 
                           const std::string& payload) override {
        // Xử lý logic của bạn ở đây
        if (event_type == "my_task") {
            return processMyTask(payload);
        }
        return "{\"error\":\"Unknown event\"}";
    }

private:
    std::string processMyTask(const std::string& payload) {
        // Logic xử lý
        return "{\"result\":\"success\",\"data\":\"" + payload + "\"}";
    }
};

} // namespace workers
} // namespace deepapp

// Auto-register với task ID
REGISTER_WORKER(deepapp::workers::BBB1_0100_Worker, "BBB1_0100_W")
```

### Bước 2: Build Lại

```bash
./build_cpp_workers.sh
```

**Xong!** Worker sẽ tự động được register và sẵn sàng nhận tasks.

## 🔌 Giao Tiếp với Java

### Từ Java gọi C++ Worker

```java
@Autowired
private GrpcClientService grpcClient;

// Gửi task đến C++ worker
String result = grpcClient.sendEventAndWaitForResponse(
    "java-client",           // sender_id
    "AAA0_0100_W",          // target_id (worker name)
    "process",              // event_type
    payload.getBytes()      // payload
).get();

System.out.println("Result from C++: " + result);
```

### Flow Hoạt Động

```
Java Client                  gRPC Hub                 C++ Worker
    |                           |                          |
    |--[AAA0_0100_W, "process"]->|                         |
    |                           |--[route by target_id]--->|
    |                           |                          |
    |                           |                    [Process Task]
    |                           |                          |
    |                           |<---[response]------------|
    |<---[response]-------------|                          |
    |                           |                          |
```

## 📊 Worker Registry System

### Auto-Registration

Mỗi worker tự động register khi start:

```cpp
REGISTER_WORKER(YourWorkerClass, "TASK_ID")
```

### Registry API

```cpp
// Get worker by task ID
auto worker = WorkerRegistry::instance().getWorker("AAA0_0100_W");

// Process task
std::string result = WorkerRegistry::instance().processTask(
    "AAA0_0100_W",  // task_id
    "process",      // event_type
    "data"          // payload
);

// Get all registered workers
auto task_ids = WorkerRegistry::instance().getTaskIds();
```

## 🧪 Testing Workers

### Test với curl (qua Java)

```bash
# Update application.yml để Java gọi local C++ worker
workers:
  cpp:
    host: localhost
    port: 50051
    targetId: cpp-worker

# Start both
./start_all.sh

# Test từ curl
curl -X POST http://localhost:8080/api/test/worker \
  -H "Content-Type: application/json" \
  -d '{
    "targetId": "AAA0_0100_W",
    "eventType": "echo",
    "payload": "Hello from Java!"
  }'
```

### Test C++ Worker Directly

```bash
# Run C++ worker
./run_cpp_workers.sh

# In another terminal, test with Java client or another C++ client
```

## ⚙️ Configuration

### CMakeLists.txt

Tự động tìm tất cả workers:
```cmake
file(GLOB_RECURSE WORKER_SOURCES 
    "${CMAKE_SOURCE_DIR}/src/main/cpp/com/deepapp/vn/io/**/worker/*_W.cpp"
)
```

### Runtime Configuration

```bash
# Default
./build/deepapp_worker_main

# Custom server
./build/deepapp_worker_main 72.60.111.138:50051 cpp-worker

# Arguments:
# $1 = server_address (default: 72.60.111.138:50051)
# $2 = client_id (default: cpp-worker)
```

## 🎯 Best Practices

### 1. Worker Naming Convention
- File: `*_W.cpp` (ví dụ: `AAA0_0100_W.cpp`)
- Class: `*_Worker` (ví dụ: `AAA0_0100_Worker`)
- Task ID: `*_W` (ví dụ: `AAA0_0100_W`)

### 2. Worker Structure
```cpp
class YourWorker : public BaseWorker {
public:
    YourWorker() : BaseWorker("WorkerName") {}
    
    std::string processTask(const std::string& event_type, 
                           const std::string& payload) override {
        // Route by event_type
        if (event_type == "task1") return handleTask1(payload);
        if (event_type == "task2") return handleTask2(payload);
        return errorResponse("Unknown event");
    }
    
private:
    std::string handleTask1(const std::string& payload) {
        // Implementation
        return jsonResponse("success", result);
    }
};

REGISTER_WORKER(YourWorker, "YOUR_TASK_ID")
```

### 3. Error Handling
```cpp
try {
    // Process logic
    return successResponse(result);
} catch (const std::exception& e) {
    return errorResponse(e.what());
}
```

### 4. JSON Response Format
```cpp
std::string createResponse(const std::string& status, const std::string& data) {
    std::ostringstream oss;
    oss << "{"
        << "\"worker\":\"" << getWorkerId() << "\","
        << "\"status\":\"" << status << "\","
        << "\"data\":\"" << data << "\","
        << "\"timestamp\":" << std::time(nullptr)
        << "}";
    return oss.str();
}
```

## 🔍 Debugging

### Xem Workers Registered

Khi start C++ worker, sẽ hiển thị:
```
[Main] Worker Registry Status:
  Total workers: 2
  ✓ Registered workers:
    - AAA0_0100_W (AAA0_0100_Worker)
    - AAA0_0200_W (AAA0_0200_Worker)
```

### Log Level

Mỗi worker tự in log:
```cpp
std::cout << "[" << getWorkerId() << "] Processing: " << event_type << std::endl;
```

### Check Connection

```bash
# Check if C++ worker connected to gRPC hub
tail -f /var/log/grpc_hub.log

# Or check from Java logs
tail -f logs/spring.log | grep cpp-worker
```

## 📈 Scaling

### Multiple Workers

Tất cả workers trong một process, share một gRPC connection:
- ✅ Hiệu quả cho small/medium workloads
- ✅ Dễ deploy và maintain
- ✅ Shared registry và resources

### Load Balancing (Future)

Nếu cần scale, có thể:
1. Run multiple instances với different client_ids
2. Use load balancer ở gRPC hub level
3. Implement worker pools

## 🎓 Examples

### Example 1: Simple Echo Worker
```cpp
class EchoWorker : public BaseWorker {
public:
    EchoWorker() : BaseWorker("EchoWorker") {}
    
    std::string processTask(const std::string& event_type, 
                           const std::string& payload) override {
        return "{\"echo\":\"" + payload + "\"}";
    }
};
REGISTER_WORKER(EchoWorker, "ECHO_W")
```

### Example 2: Data Processing Worker
```cpp
class DataWorker : public BaseWorker {
public:
    DataWorker() : BaseWorker("DataWorker") {}
    
    std::string processTask(const std::string& event_type, 
                           const std::string& payload) override {
        if (event_type == "transform") {
            return transformData(payload);
        }
        return errorResponse();
    }
    
private:
    std::string transformData(const std::string& data) {
        // Process data
        std::string result = processLogic(data);
        return jsonResponse("success", result);
    }
};
REGISTER_WORKER(DataWorker, "DATA_W")
```

## 🆘 Troubleshooting

### Worker không được register
- ✅ Check file name pattern: `*_W.cpp`
- ✅ Check REGISTER_WORKER macro có được gọi
- ✅ Rebuild project: `./build_cpp_workers.sh`

### Connection failed
- ✅ Check gRPC hub đang chạy
- ✅ Check server address đúng
- ✅ Check firewall/network

### Task không được route
- ✅ Check target_id match với registered task_id
- ✅ Check worker đã registered: xem logs khi start

## 🎉 Kết Luận

Hệ thống C++ workers đã được setup với:
- ✅ Auto-registration of workers
- ✅ gRPC bidirectional streaming
- ✅ Easy to add new workers (just create file)
- ✅ Integrated with Java application
- ✅ Production-ready architecture

Bạn chỉ cần tạo file `*_W.cpp`, implement `processTask()`, và call `REGISTER_WORKER()` - hệ thống sẽ tự động handle phần còn lại!
