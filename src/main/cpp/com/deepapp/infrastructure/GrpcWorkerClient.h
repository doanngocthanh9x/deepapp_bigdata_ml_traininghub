#pragma once

#include <grpcpp/grpcpp.h>
#include <memory>
#include <string>
#include <atomic>
#include <thread>
#include <queue>
#include <mutex>
#include <condition_variable>
#include <map>
#include "hub.grpc.pb.h"
#include "WorkerRegistry.h"
#include "BaseWorker.h"

namespace deepapp {
namespace infrastructure {

/**
 * gRPC Worker Client
 * Connects to gRPC hub and processes incoming tasks
 */
class GrpcWorkerClient {
public:
    GrpcWorkerClient(const std::string& client_id, 
                     const std::string& server_address)
        : client_id_(client_id), 
          server_address_(server_address),
          running_(false) {}

    ~GrpcWorkerClient() {
        stop();
    }

    /**
     * Start the gRPC client
     */
    void start() {
        if (running_) return;

        std::cout << "[GrpcWorkerClient] Connecting to " << server_address_ 
                  << " with client_id: " << client_id_ << std::endl;

        // Inject this client into all workers
        auto task_ids = WorkerRegistry::instance().getTaskIds();
        for (const auto& id : task_ids) {
            auto worker = WorkerRegistry::instance().getWorker(id);
            if (worker) {
                worker->setGrpcClient(this);
            }
        }
        std::cout << "[GrpcWorkerClient] Injected gRPC client into " << task_ids.size() << " workers" << std::endl;

        // Create channel
        auto channel = grpc::CreateChannel(server_address_, 
                                          grpc::InsecureChannelCredentials());
        stub_ = hub::DataStream::NewStub(channel);

        running_ = true;
        
        // Start bidirectional streaming
        grpc::ClientContext context;
        stream_ = stub_->StreamEvents(&context);

        // Send connect message
        hub::EventChunk connect_msg;
        connect_msg.set_sender_id(client_id_);
        connect_msg.set_target_id("");
        connect_msg.set_event_type("connect");
        connect_msg.set_timestamp(std::time(nullptr) * 1000);
        
        if (!stream_->Write(connect_msg)) {
            std::cerr << "[GrpcWorkerClient] Failed to send connect message" << std::endl;
            return;
        }

        std::cout << "[GrpcWorkerClient] Connected! Waiting for tasks..." << std::endl;
        
        // Print registered workers (reuse task_ids from above)
        std::cout << "[GrpcWorkerClient] Registered " 
                  << task_ids.size() << " workers:" << std::endl;
        for (const auto& id : task_ids) {
            std::cout << "  - " << id << std::endl;
        }

        // Start reading messages
        hub::EventChunk event;
        while (running_ && stream_->Read(&event)) {
            handleEvent(event);
        }

        stream_->WritesDone();
        auto status = stream_->Finish();
        
        if (!status.ok()) {
            std::cerr << "[GrpcWorkerClient] Stream error: " 
                      << status.error_message() << std::endl;
        }
    }

    /**
     * Stop the client
     */
    void stop() {
        running_ = false;
    }

    /**
     * Send an event
     */
    void sendEvent(const std::string& target_id,
                  const std::string& event_type,
                  const std::string& payload,
                  const std::map<std::string, std::string>& metadata = {}) {
        hub::EventChunk event;
        event.set_sender_id(client_id_);
        event.set_target_id(target_id);
        event.set_event_type(event_type);
        event.set_payload(payload);
        event.set_timestamp(std::time(nullptr) * 1000);
        
        for (const auto& [key, value] : metadata) {
            (*event.mutable_metadata())[key] = value;
        }

        std::lock_guard<std::mutex> lock(write_mutex_);
        if (stream_) {
            stream_->Write(event);
        }
    }

private:
    void handleEvent(const hub::EventChunk& event) {
        std::cout << "\n[GrpcWorkerClient] Received event:" << std::endl;
        std::cout << "  From: " << event.sender_id() << std::endl;
        std::cout << "  To: " << event.target_id() << std::endl;
        std::cout << "  Type: " << event.event_type() << std::endl;
        std::cout << "  Payload: " << event.payload() << std::endl;

        // Get task_id from metadata first, fallback to target_id
        std::string task_id;
        if (event.metadata().count("taskId") > 0) {
            task_id = event.metadata().at("taskId");
            std::cout << "  TaskId (from metadata): " << task_id << std::endl;
        } else {
            task_id = event.target_id();
            std::cout << "  TaskId (from targetId): " << task_id << std::endl;
        }
        
        // Route to appropriate worker
        std::string result = WorkerRegistry::instance().processTask(
            task_id, 
            event.event_type(), 
            event.payload()
        );

        std::cout << "  Result: " << result << std::endl;

        // Send response back
        std::map<std::string, std::string> metadata;
        
        // Check if request has requestId in metadata
        if (event.metadata().count("requestId") > 0) {
            metadata["requestId"] = event.metadata().at("requestId");
        }

        sendEvent(
            event.sender_id(),  // Send back to sender
            "response",
            result,
            metadata
        );

        std::cout << "[GrpcWorkerClient] Response sent!" << std::endl;
    }

    std::string client_id_;
    std::string server_address_;
    std::atomic<bool> running_;
    std::unique_ptr<hub::DataStream::Stub> stub_;
    std::shared_ptr<grpc::ClientReaderWriter<hub::EventChunk, hub::EventChunk>> stream_;
    std::mutex write_mutex_;
};

} // namespace infrastructure
} // namespace deepapp
