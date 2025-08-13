package com.example.grpc.service;

import com.example.grpc.GreeterGrpc;
import com.example.grpc.HelloRequest;
import com.example.grpc.HelloResponse;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
public class GreeterService extends GreeterGrpc.GreeterImplBase {
    Logger logger = LoggerFactory.getLogger(GreeterService.class);

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
        String name = request.getName();
        String message = "Hello " + name;
        HelloResponse response = HelloResponse.newBuilder().setMessage(message).build();
        // Send the response
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void serverStreamHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
        try {
            for (int i = 0; i < 5; i++) { // send 5 messages then stop
                responseObserver.onNext(
                        HelloResponse.newBuilder()
                                .setMessage("Hello " + request.getName())
                                .build()
                );
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public StreamObserver<HelloRequest> clientStreamHello(StreamObserver<HelloResponse> responseObserver) {
        return new StreamObserver<HelloRequest>() {
            final StringBuilder names = new StringBuilder();

            @Override
            public void onNext(HelloRequest request) {
                names.append(request.getName()).append(", ");
            }

            @Override
            public void onError(Throwable t) {
                logger.error("Error in client stream: {}", t.getMessage());
                responseObserver.onError(t);
            }

            @Override
            public void onCompleted() {
                String allNames = names.toString();
                HelloResponse response = HelloResponse.newBuilder()
                        .setMessage("Hello to everyone: " + allNames)
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        };
    }
}