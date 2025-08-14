package com.example.grpc.client;

import com.example.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class GreeterClientTest {
    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", 9090) // Main Server
                .usePlaintext()
                .build();

        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(channel);

        HelloRequest request = HelloRequest.newBuilder()
                .setName("Piyush")
                .build();

        System.out.println("Sending request to Main Server...");
        HelloResponse response = stub.sayHello(request);

        System.out.println("✅ Final response from Main Server: " + response.getMessage());

        channel.shutdown();
    }
}
