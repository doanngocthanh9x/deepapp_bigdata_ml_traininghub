#include "com/deepapp/infrastructure/BaseWorker.h"
#include "com/deepapp/infrastructure/WorkerRegistry.h"
#include <iostream>
#include <sstream>
#include <chrono>
#include <iomanip>

namespace deepapp {
namespace workers {

/**
 * Example Worker: AAA0_0100_W
 * Demonstrates how to create a worker that auto-registers
 */
class AAA0_0100_Worker : public infrastructure::BaseWorker {
public:
    AAA0_0100_Worker() : BaseWorker("AAA0_0100_Worker") {
        std::cout << "[AAA0_0100_Worker] Initialized" << std::endl;
    }

    std::string processTask(const std::string& event_type, 
                           const std::string& payload) override {
        std::cout << "[AAA0_0100_Worker] Processing task:" << std::endl;
        std::cout << "  Event Type: " << event_type << std::endl;
        std::cout << "  Payload: " << payload << std::endl;

        // Example processing logic
        if (event_type == "echo") {
            return createResponse("success", payload);
        } 
        else if (event_type == "process") {
            return processData(payload);
        }
        else if (event_type == "transform") {
            return transformData(payload);
        }
        else {
            return createResponse("unknown_event", 
                "Event type '" + event_type + "' not supported");
        }
    }

    bool canHandle(const std::string& event_type) const override {
        return event_type == "echo" || 
               event_type == "process" || 
               event_type == "transform";
    }

private:
    std::string processData(const std::string& payload) {
        // Example: Convert to uppercase
        std::string result = payload;
        for (char& c : result) {
            c = std::toupper(c);
        }
        return createResponse("processed", result);
    }

    std::string transformData(const std::string& payload) {
        // Example: Add timestamp
        auto now = std::chrono::system_clock::now();
        auto time_t = std::chrono::system_clock::to_time_t(now);
        
        std::ostringstream oss;
        oss << "{\"timestamp\":\"" 
            << std::put_time(std::localtime(&time_t), "%Y-%m-%d %H:%M:%S")
            << "\",\"data\":\"" << payload << "\"}";
        
        return createResponse("transformed", oss.str());
    }

    std::string createResponse(const std::string& status, 
                              const std::string& data) {
        std::ostringstream oss;
        oss << "{"
            << "\"worker\":\"AAA0_0100_W\","
            << "\"status\":\"" << status << "\","
            << "\"data\":\"" << data << "\","
            << "\"timestamp\":" << std::time(nullptr)
            << "}";
        return oss.str();
    }
};

} // namespace workers
} // namespace deepapp

// Auto-register this worker with task ID "AAA0_0100_W"
REGISTER_WORKER(deepapp::workers::AAA0_0100_Worker, "AAA0_0100_W")
