package com.example.grpc;

import com.example.grpc.service.ExecutorGreeterService;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;

public class ExecutorServer {
    public static void main(String[] args) throws Exception {
        Server server = Grpc.newServerBuilderForPort(50052, InsecureServerCredentials.create())
                .addService(new ExecutorGreeterService()) // ✅ add our executor service
                .build()
                .start();
        System.out.println("🚀 Executor Server started on port 50052");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("🛑 Shutting down Executor server...");
            server.shutdown();
        }));
        server.awaitTermination();
    }
}
