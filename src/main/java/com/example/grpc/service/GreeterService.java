package com.example.grpc.service;

import com.example.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class GreeterService extends GreeterGrpc.GreeterImplBase {

    Logger logger = LoggerFactory.getLogger(GreeterService.class);
    private final StreamObserver<HelloResponseWrapper> executorRequestStream;

    public GreeterService() {
        // Create channel to executor
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", 50052)
                .usePlaintext()
                .build();

        GreeterGrpc.GreeterStub executorStub = GreeterGrpc.newStub(channel);

        // Start streaming with executor
        executorRequestStream = executorStub.exchange(new StreamObserver<HelloRequestWrapper>() {
            @Override
            public void onNext(HelloRequestWrapper helloRequestWrapper) {
                // This is a request from the executor (not our client)
                logger.info("Received from executor: {}", helloRequestWrapper);
                respondToMessage(helloRequestWrapper.getReqId(),
                        "Executor says hello to " + helloRequestWrapper.getName());
            }

            @Override
            public void onError(Throwable t) {
                logger.error("Executor stream error", t);
            }

            @Override
            public void onCompleted() {
                logger.info("Executor stream completed");
            }
        });
    }

    private final Map<String, StreamObserver<HelloResponse>> observerMap = new HashMap<>();

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
        String reqId = UUID.randomUUID().toString();
        observerMap.put(reqId, responseObserver);

        // Send request to executor
        HelloResponseWrapper wrapper = HelloResponseWrapper.newBuilder()
                .setReqId(reqId)
                .setMessage("Hello from service to executor for " + request.getName())
                .build();

        executorRequestStream.onNext(wrapper);

        // Don't respond here — wait for executor callback
    }

    private void respondToMessage(String reqId, String message) {
        StreamObserver<HelloResponse> observer = observerMap.remove(reqId);
        if (observer != null) {
            observer.onNext(HelloResponse.newBuilder().setMessage(message).build());
            observer.onCompleted();
        }
    }

    /**
     * Server streaming RPC
     */
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

    /**
     * Client streaming RPC
     */
    @Override
    public StreamObserver<HelloRequest> clientStreamHello(StreamObserver<HelloResponse> responseObserver) {
        return new StreamObserver<>() {
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