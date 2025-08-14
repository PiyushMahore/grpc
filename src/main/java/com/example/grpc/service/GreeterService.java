package com.example.grpc.service;

import com.example.grpc.GreeterGrpc;
import com.example.grpc.HelloRequest;
import com.example.grpc.HelloRequestWrapper;
import com.example.grpc.HelloResponse;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GreeterService extends GreeterGrpc.GreeterImplBase {

    Logger logger = LoggerFactory.getLogger(GreeterService.class);
    private Map<String, StreamObserver<HelloResponse>> observerMap = new HashMap<>();

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
        String name = request.getName();
        String message = "Hello " + name;
        var id = UUID.randomUUID().toString();
        observerMap.put(id, responseObserver);
        respondToMessage(id, message);
        HelloRequestWrapper helloRequestWrapper = HelloRequestWrapper.newBuilder().setReqId(id).setName(name).build();
        //send the message to executor;
    }

    public void respondToMessage(String id, String message) {
        var responseObserver = observerMap.get(id);
        HelloResponse response = HelloResponse.newBuilder().setMessage(message).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void serverStreamHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
        try {
            for (int i = 0; i < 5; i++) {
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