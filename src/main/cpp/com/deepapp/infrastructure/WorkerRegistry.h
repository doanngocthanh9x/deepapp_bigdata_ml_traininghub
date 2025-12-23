#pragma once

#include <map>
#include <memory>
#include <string>
#include <vector>
#include <functional>
#include "BaseWorker.h"

namespace deepapp {
namespace infrastructure {

/**
 * Registry for all workers
 * Automatically registers workers at startup
 */
class WorkerRegistry {
public:
    static WorkerRegistry& instance() {
        static WorkerRegistry instance;
        return instance;
    }

    /**
     * Register a worker
     */
    void registerWorker(const std::string& task_id, WorkerPtr worker) {
        workers_[task_id] = worker;
        std::cout << "[WorkerRegistry] Registered worker: " << task_id 
                  << " -> " << worker->getWorkerId() << std::endl;
    }

    /**
     * Get worker by task ID
     */
    WorkerPtr getWorker(const std::string& task_id) {
        auto it = workers_.find(task_id);
        if (it != workers_.end()) {
            return it->second;
        }
        return nullptr;
    }

    /**
     * Process a task by routing to appropriate worker
     */
    std::string processTask(const std::string& task_id, 
                          const std::string& event_type,
                          const std::string& payload) {
        auto worker = getWorker(task_id);
        if (worker) {
            return worker->processTask(event_type, payload);
        }
        return "{\"error\":\"Worker not found: " + task_id + "\"}";
    }

    /**
     * Get all registered task IDs
     */
    std::vector<std::string> getTaskIds() const {
        std::vector<std::string> ids;
        for (const auto& pair : workers_) {
            ids.push_back(pair.first);
        }
        return ids;
    }

    /**
     * Get count of registered workers
     */
    size_t getWorkerCount() const {
        return workers_.size();
    }

private:
    WorkerRegistry() = default;
    std::map<std::string, WorkerPtr> workers_;
};

/**
 * Auto-registration helper
 * Use this in worker files to auto-register at startup
 */
template<typename WorkerClass>
class WorkerRegistrar {
public:
    WorkerRegistrar(const std::string& task_id) {
        auto worker = std::make_shared<WorkerClass>();
        WorkerRegistry::instance().registerWorker(task_id, worker);
    }
};

// Macro for easy worker registration
// Use __LINE__ and __COUNTER__ to make unique names
#define REGISTER_WORKER(WorkerClass, TaskId) \
    namespace { \
        struct WorkerRegistrar_##__LINE__ { \
            WorkerRegistrar_##__LINE__() { \
                auto worker = std::make_shared<WorkerClass>(); \
                deepapp::infrastructure::WorkerRegistry::instance().registerWorker(TaskId, worker); \
            } \
        }; \
        static WorkerRegistrar_##__LINE__ _worker_registrar_instance_##__LINE__; \
    }

} // namespace infrastructure
} // namespace deepapp
