package com.example.grpc.service;

import com.example.grpc.*;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecutorGreeterService extends GreeterGrpc.GreeterImplBase {

    Logger logger = LoggerFactory.getLogger(ExecutorGreeterService.class);

    @Override
    public StreamObserver<HelloResponseWrapper> exchange(StreamObserver<HelloRequestWrapper> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(HelloResponseWrapper helloResponseWrapper) {
                logger.info("Executor received from Main Server: {}", helloResponseWrapper);

                // Simulate some processing
                String processedName = helloResponseWrapper.getMessage();

                // Send back to Main Server
                HelloRequestWrapper reply = HelloRequestWrapper.newBuilder()
                        .setReqId(helloResponseWrapper.getReqId()) // must match so server can map it back to client
                        .setName(processedName)
                        .build();

                responseObserver.onNext(reply);
            }

            @Override
            public void onError(Throwable t) {
                logger.error("Error in executor exchange", t);
            }

            @Override
            public void onCompleted() {
                logger.info("Main server closed stream");
                responseObserver.onCompleted();
            }
        };
    }
}
