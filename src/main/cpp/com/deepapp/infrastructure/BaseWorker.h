#pragma once

#include <string>
#include <memory>
#include <functional>
#include "hub.pb.h"

namespace deepapp {
namespace infrastructure {

/**
 * Base class for all workers
 * Each worker must inherit from this and implement processTask()
 */
class BaseWorker {
public:
    BaseWorker(const std::string& worker_id) : worker_id_(worker_id) {}
    virtual ~BaseWorker() = default;

    /**
     * Process a task and return result
     * @param event_type The type of event/task
     * @param payload The task payload
     * @return Result as string
     */
    virtual std::string processTask(const std::string& event_type, 
                                    const std::string& payload) = 0;

    /**
     * Get worker ID (used for routing)
     */
    std::string getWorkerId() const { return worker_id_; }

    /**
     * Check if this worker can handle the given event type
     */
    virtual bool canHandle(const std::string& event_type) const {
        return true; // Default: handle all events
    }

protected:
    std::string worker_id_;
};

using WorkerPtr = std::shared_ptr<BaseWorker>;

} // namespace infrastructure
} // namespace deepapp
