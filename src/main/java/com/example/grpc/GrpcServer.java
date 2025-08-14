package com.example.grpc;

import com.example.grpc.intercepter.AuthInterceptor;
import com.example.grpc.service.AuthService;
import com.example.grpc.service.GreeterService;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;

public class GrpcServer {

    public static void main(String[] args) throws Exception {

        // Build and start gRPC server
        Server server = Grpc.newServerBuilderForPort(9090, InsecureServerCredentials.create())
                .intercept(new AuthInterceptor())        // interceptor first
                .addService(new AuthService())           // then all services
                .addService(new GreeterService())
                .build()
                .start();

        System.out.println("🚀 gRPC Server started on port 9090");

        // Add shutdown hook for graceful termination
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("🛑 Shutting down gRPC server...");
            server.shutdown();
        }));

        // Keep the server running
        server.awaitTermination();
    }
}