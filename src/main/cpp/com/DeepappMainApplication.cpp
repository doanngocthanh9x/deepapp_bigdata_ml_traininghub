#include <iostream>
#include <csignal>
#include <memory>
#include <thread>
#include <chrono>
#include "com/deepapp/infrastructure/GrpcWorkerClient.h"
#include "com/deepapp/infrastructure/WorkerRegistry.h"

using namespace deepapp::infrastructure;

// Signal handler for graceful shutdown
std::unique_ptr<GrpcWorkerClient> g_client;

void signalHandler(int signal) {
    std::cout << "\n[Main] Received signal " << signal << ", shutting down..." << std::endl;
    if (g_client) {
        g_client->stop();
    }
    exit(0);
}

int main(int argc, char** argv) {
    std::cout << "========================================" << std::endl;
    std::cout << "DeepApp C++ Worker Application" << std::endl;
    std::cout << "========================================" << std::endl;

    // Parse command line arguments
    std::string server_address = "72.60.111.138:50051";
    std::string client_id = "cpp-worker";

    if (argc > 1) {
        server_address = argv[1];
    }
    if (argc > 2) {
        client_id = argv[2];
    }

    std::cout << "[Main] Configuration:" << std::endl;
    std::cout << "  Server: " << server_address << std::endl;
    std::cout << "  Client ID: " << client_id << std::endl;
    std::cout << std::endl;

    // Show registered workers
    auto& registry = WorkerRegistry::instance();
    std::cout << "[Main] Worker Registry Status:" << std::endl;
    std::cout << "  Total workers: " << registry.getWorkerCount() << std::endl;
    
    auto task_ids = registry.getTaskIds();
    if (task_ids.empty()) {
        std::cout << "  ⚠️  WARNING: No workers registered!" << std::endl;
        std::cout << "  Make sure worker files are compiled and linked." << std::endl;
    } else {
        std::cout << "  ✓ Registered workers:" << std::endl;
        for (const auto& id : task_ids) {
            auto worker = registry.getWorker(id);
            std::cout << "    - " << id << " (" << worker->getWorkerId() << ")" << std::endl;
        }
    }
    std::cout << std::endl;

    // Setup signal handlers
    std::signal(SIGINT, signalHandler);
    std::signal(SIGTERM, signalHandler);

    // Create and start gRPC client
    g_client = std::make_unique<GrpcWorkerClient>(client_id, server_address);

    std::cout << "[Main] Starting gRPC worker client..." << std::endl;
    std::cout << "========================================" << std::endl;
    std::cout << std::endl;

    try {
        // Start client (blocking call)
        g_client->start();
    } catch (const std::exception& e) {
        std::cerr << "[Main] Fatal error: " << e.what() << std::endl;
        return 1;
    }

    std::cout << "[Main] Client stopped." << std::endl;
    return 0;
}
