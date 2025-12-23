#include <iostream>
#include <memory>
#include <string>
#include <thread>
#include <chrono>

#include <grpcpp/grpcpp.h>
#include "hub.grpc.pb.h"

using grpc::Channel;
using grpc::ClientContext;
using grpc::Status;
using grpc::ClientReaderWriter;

using hub::DataStream;
using hub::EventChunk;

class HubClient {
public:
    HubClient(std::shared_ptr<Channel> channel, const std::string& client_id)
        : stub_(DataStream::NewStub(channel)), client_id_(client_id) {}

    void ConnectAndListen() {
        ClientContext context;
        std::shared_ptr<ClientReaderWriter<EventChunk, EventChunk>> stream(
            stub_->StreamEvents(&context));

        // Send connection message
        EventChunk connect_msg;
        connect_msg.set_sender_id(client_id_);
        connect_msg.set_target_id("");
        connect_msg.set_event_type("connect");
        connect_msg.set_payload(client_id_);
        connect_msg.set_timestamp(std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::system_clock::now().time_since_epoch()).count());

        std::cout << "[C++] Sending connection message with ID: " << client_id_ << std::endl;
        stream->Write(connect_msg);

        // Start reading thread
        std::thread reader([stream, this]() {
            EventChunk received_msg;
            while (stream->Read(&received_msg)) {
                std::cout << "[C++] 📨 RECEIVED: Event '" << received_msg.event_type() << "' from '"
                          << received_msg.sender_id() << "' to '" << received_msg.target_id()
                          << "' | Payload: '" << received_msg.payload() << "'" << std::endl;

                // Echo back if message is targeted to us
                if (received_msg.target_id() == client_id_ || received_msg.target_id().empty()) {
                    std::cout << "[C++] Processing message targeted to us..." << std::endl;
                }
            }
            std::cout << "[C++] Stream ended" << std::endl;
        });

        // Wait for a bit to receive messages
        std::this_thread::sleep_for(std::chrono::seconds(30));

        // Send a test message back to Java client
        EventChunk response_msg;
        response_msg.set_sender_id(client_id_);
        response_msg.set_target_id("java-client");  // Try to send to Java
        response_msg.set_event_type("response");
        response_msg.set_payload("Hello from C++ test client!");
        response_msg.set_timestamp(std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::system_clock::now().time_since_epoch()).count());

        std::cout << "[C++] 📤 SENDING: Response to Java client" << std::endl;
        stream->Write(response_msg);

        // Wait a bit more
        std::this_thread::sleep_for(std::chrono::seconds(10));

        // Close the stream
        stream->WritesDone();
        Status status = stream->Finish();

        if (!status.ok()) {
            std::cout << "[C++] Stream finished with error: " << status.error_message() << std::endl;
        } else {
            std::cout << "[C++] Stream finished successfully" << std::endl;
        }

        reader.join();
    }

private:
    std::unique_ptr<DataStream::Stub> stub_;
    std::string client_id_;
};

int main(int argc, char** argv) {
    std::string client_id = "cpp_test_client";
    if (argc > 1) {
        client_id = argv[1];
    }

    std::cout << "Starting C++ Test gRPC client: " << client_id << std::endl;

    HubClient client(
        grpc::CreateChannel("72.60.111.138:50051", grpc::InsecureChannelCredentials()),
        client_id
    );

    client.ConnectAndListen();

    std::cout << "C++ test client finished" << std::endl;
    return 0;
}