package com.example.grpc.greeterServiceTest;

import com.example.grpc.GreeterGrpc;
import com.example.grpc.HelloRequest;
import com.example.grpc.HelloResponse;
import com.example.grpc.service.GreeterService;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class GreeterServiceTest {

    private static ManagedChannel channel;

    @BeforeAll
    static void setUp() throws IOException {
        String serverName = InProcessServerBuilder.generateName();

        // Start in-process gRPC server
        InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(new GreeterService()) // Your service implementation
                .build()
                .start();

        // Create channel to connect to in-process server
        channel = InProcessChannelBuilder.forName(serverName)
                .directExecutor()
                .build();
    }

    @AfterAll
    static void tearDown() {
        if (channel != null) {
            channel.shutdownNow();
        }
    }

    @Test
    void testUnarySayHello() {
        GreeterGrpc.GreeterBlockingStub blockingStub = GreeterGrpc.newBlockingStub(channel);

        HelloRequest request = HelloRequest.newBuilder().setName("Piyush").build();
        HelloResponse response = blockingStub.sayHello(request);

        assertEquals("Hello Piyush", response.getMessage());
    }

    @Test
    void testServerStreamingHello() {
        GreeterGrpc.GreeterBlockingStub blockingStub = GreeterGrpc.newBlockingStub(channel);
        HelloRequest request = HelloRequest.newBuilder().setName("Piyush").build();

        Iterator<HelloResponse> responses = blockingStub.serverStreamHello(request);

        List<String> messages = new ArrayList<>();
        int count = 0;
        while (responses.hasNext() && count < 3) { // limit messages
            messages.add(responses.next().getMessage());
            count++;
        }

        assertFalse(messages.isEmpty(), "Server should return multiple messages");
    }

    @Test
    void testClientStreamingHello() throws InterruptedException {
        GreeterGrpc.GreeterStub asyncStub = GreeterGrpc.newStub(channel);

        CountDownLatch latch = new CountDownLatch(1);
        List<String> resultMessage = new ArrayList<>();

        StreamObserver<HelloResponse> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(HelloResponse value) {
                resultMessage.add(value.getMessage());
            }

            @Override
            public void onError(Throwable t) {
                fail("Error in client streaming: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                latch.countDown();
            }
        };

        StreamObserver<HelloRequest> requestObserver = asyncStub.clientStreamHello(responseObserver);

        // Send multiple names
        requestObserver.onNext(HelloRequest.newBuilder().setName("Piyush").build());
        requestObserver.onNext(HelloRequest.newBuilder().setName("John").build());
        requestObserver.onNext(HelloRequest.newBuilder().setName("Anand").build());

        // Complete request stream
        requestObserver.onCompleted();

        // Wait for server response
        assertTrue(latch.await(2, TimeUnit.SECONDS), "Did not receive server response in time");

        assertFalse(resultMessage.isEmpty(), "Server did not send any response");
        assertTrue(resultMessage.get(0).contains("Piyush"));
        assertTrue(resultMessage.get(0).contains("John"));
        assertTrue(resultMessage.get(0).contains("Anand"));
    }
}
