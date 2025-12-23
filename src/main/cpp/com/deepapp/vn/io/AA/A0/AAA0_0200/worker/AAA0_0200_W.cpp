#include "com/deepapp/infrastructure/BaseWorker.h"
#include "com/deepapp/infrastructure/WorkerRegistry.h"
#include <iostream>
#include <sstream>

namespace deepapp {
namespace workers {

/**
 * Example Worker 2: AAA0_0200_W
 * Another example to demonstrate multiple workers
 */
class AAA0_0200_Worker : public infrastructure::BaseWorker {
public:
    AAA0_0200_Worker() : BaseWorker("AAA0_0200_Worker") {
        std::cout << "[AAA0_0200_Worker] Initialized" << std::endl;
    }

    std::string processTask(const std::string& event_type, 
                           const std::string& payload) override {
        std::cout << "[AAA0_0200_Worker] Processing: " << event_type << std::endl;

        if (event_type == "calculate") {
            return calculate(payload);
        }
        else if (event_type == "validate") {
            return validate(payload);
        }
        else {
            return "{\"error\":\"Unknown event type\"}";
        }
    }

private:
    std::string calculate(const std::string& payload) {
        // Example calculation
        std::ostringstream oss;
        oss << "{\"worker\":\"AAA0_0200_W\","
            << "\"result\":\"calculated\","
            << "\"value\":" << payload.length() << "}";
        return oss.str();
    }

    std::string validate(const std::string& payload) {
        bool valid = !payload.empty();
        std::ostringstream oss;
        oss << "{\"worker\":\"AAA0_0200_W\","
            << "\"valid\":" << (valid ? "true" : "false") << "}";
        return oss.str();
    }
};

} // namespace workers
} // namespace deepapp

// Auto-register with task ID "AAA0_0200_W"
REGISTER_WORKER(deepapp::workers::AAA0_0200_Worker, "AAA0_0200_W")
