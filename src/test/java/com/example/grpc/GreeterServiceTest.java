package com.example.grpc;

import io.grpc.*;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class GreeterServiceTest {

    private ManagedChannel channel;
    private GreeterGrpc.GreeterBlockingStub blockingStub;

    @BeforeEach
    void setUp() {
        channel = ManagedChannelBuilder.forAddress("localhost", 9090)
                .usePlaintext()
                .build();

        blockingStub = GreeterGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() {
        channel.shutdownNow();
    }

    @Test
    void sayHelloTest() {
        HelloRequest request = HelloRequest.newBuilder().setName("Piyush").build();
        var stubWithAuth = authenticate();
        HelloResponse response = stubWithAuth.sayHello(request);
        assertEquals("Hello Piyush", response.getMessage());
    }

    @Test
    void serverStreamingHelloTest() {
        HelloRequest request = HelloRequest.newBuilder().setName("Piyush").build();
        var stubWithAuth = authenticate();
        Iterator<HelloResponse> responses = stubWithAuth.serverStreamHello(request);
        List<String> messages = new ArrayList<>();
        int count = 0;
        while (responses.hasNext() && count < 3) { // limit messages
            messages.add(responses.next().getMessage());
            count++;
        }
        assertFalse(messages.isEmpty(), "Server should return multiple messages");
    }

    @Test
    void clientStreamingHelloTest() throws InterruptedException {
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

        String username = "admin";
        String password = "password";
        String credentials = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));

        Metadata headers = new Metadata();
        headers.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Basic " + credentials);

        ClientInterceptor interceptor = MetadataUtils.newAttachHeadersInterceptor(headers);
        var stubWithAuth = GreeterGrpc.newStub(channel).withInterceptors(interceptor); // async stub

        StreamObserver<HelloRequest> requestObserver = stubWithAuth.clientStreamHello(responseObserver);

        // Send multiple names
        requestObserver.onNext(HelloRequest.newBuilder().setName("Piyush").build());
        requestObserver.onNext(HelloRequest.newBuilder().setName("Jay").build());
        requestObserver.onNext(HelloRequest.newBuilder().setName("Anand").build());

        // Complete request stream
        requestObserver.onCompleted();

        // Wait for server response
        assertTrue(latch.await(2, TimeUnit.SECONDS), "Did not receive server response in time");

        assertFalse(resultMessage.isEmpty(), "Server did not send any response");
        assertTrue(resultMessage.get(0).contains("Piyush"));
        assertTrue(resultMessage.get(0).contains("Jay"));
        assertTrue(resultMessage.get(0).contains("Anand"));
    }

    @Test
    void sayHelloWithEmptyNameTest() {
        HelloRequest request = HelloRequest.newBuilder().setName("").build();
        var stubWithAuth = authenticate();
        HelloResponse response = stubWithAuth.sayHello(request);
        // Assuming your service handles empty name gracefully
        assertEquals("Hello ", response.getMessage(), "Server should handle empty name");
    }

    @Test
    void serverStreamingHelloWithNullNameTest() {
        HelloRequest request = HelloRequest.newBuilder().setName("").build();
        var stubWithAuth = authenticate();
        Iterator<HelloResponse> responses = stubWithAuth.serverStreamHello(request);
        List<String> messages = new ArrayList<>();
        while (responses.hasNext()) {
            messages.add(responses.next().getMessage());
        }
        assertFalse(messages.isEmpty(), "Server should return messages even for empty name");
    }

    @Test
    void clientCancelsStreamingTest() throws InterruptedException {
        GreeterGrpc.GreeterStub asyncStub = GreeterGrpc.newStub(channel);
        CountDownLatch latch = new CountDownLatch(1);

        StreamObserver<HelloResponse> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(HelloResponse value) {
                fail("Should not receive messages after cancellation");
            }

            @Override
            public void onError(Throwable t) {
                assertNotNull(t, "Cancellation should trigger error");
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                fail("Server should not complete normally after cancellation");
            }
        };

        StreamObserver<HelloRequest> requestObserver = asyncStub.clientStreamHello(responseObserver);

        requestObserver.onNext(HelloRequest.newBuilder().setName("Piyush").build());
        // Simulate client cancellation
        requestObserver.onError(new RuntimeException("Client cancels"));

        assertTrue(latch.await(2, TimeUnit.SECONDS), "Did not receive cancellation error");
    }

    private GreeterGrpc.GreeterBlockingStub authenticate() {
        String username = "admin";
        String password = "password";
        String credentials = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));

        Metadata headers = new Metadata();
        headers.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Basic " + credentials);

        ClientInterceptor interceptor = MetadataUtils.newAttachHeadersInterceptor(headers);
        return blockingStub.withInterceptors(interceptor);
    }
}
