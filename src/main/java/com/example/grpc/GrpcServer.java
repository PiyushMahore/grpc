package com.example.grpc;

import com.example.grpc.service.GreeterService;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;

public class GrpcServer {
    public static void main(String[] args) throws Exception {
        Server server = Grpc.newServerBuilderForPort(9090, InsecureServerCredentials.create())
                .addService(new GreeterService())
                .build()
                .start();
        System.out.println("🚀 gRPC Server started on port 9090");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("🛑 Shutting down gRPC server...");
            server.shutdown();
        }));
        server.awaitTermination();
    }
}