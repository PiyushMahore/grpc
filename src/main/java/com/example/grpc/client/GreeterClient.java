package com.example.grpc.client;

import com.example.grpc.GreeterGrpc;
import com.example.grpc.HelloRequest;
import com.example.grpc.HelloResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class GreeterClient {
    Logger logger = LoggerFactory.getLogger(GreeterClient.class);
    private final GreeterGrpc.GreeterBlockingStub blockingStub;
    private final GreeterGrpc.GreeterStub asyncStub;

    public GreeterClient() {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9090)
                .usePlaintext()
                .build();
        this.blockingStub = GreeterGrpc.newBlockingStub(channel);
        this.asyncStub = GreeterGrpc.newStub(channel);
    }

    public String sayHello(String name) {
        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        HelloResponse response = blockingStub.sayHello(request);
        return response.getMessage();
    }

    public void serverStreamHello(String name) {
        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        Iterator<HelloResponse> responses = blockingStub.serverStreamHello(request);

        while (responses.hasNext()) {
            HelloResponse response = responses.next();
            logger.info("Received: {}", response.getMessage());
        }

        logger.info("Stream completed after receiving all messages.");
    }

    public void clientStreamHello(List<String> names) {
        // Response handler
        StreamObserver<HelloResponse> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(HelloResponse value) {
                logger.info("Server responded: {}", value.getMessage());
            }

            @Override
            public void onError(Throwable t) {
                logger.error("Error from server: {}", t.getMessage());
            }

            @Override
            public void onCompleted() {
                logger.info("Server completed sending response.");
            }
        };

        // Get request observer from asyncStub
        StreamObserver<HelloRequest> requestObserver = asyncStub.clientStreamHello(responseObserver);

        try {
            // Send dynamic list of names
            for (String name : names) {
                requestObserver.onNext(HelloRequest.newBuilder().setName(name).build());
            }

            // Complete request stream
            requestObserver.onCompleted();

        } catch (Exception e) {
            requestObserver.onError(e);
        }
    }
}