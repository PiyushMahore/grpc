package com.example.grpc;

import com.example.grpc.intercepter.AuthInterceptor;
import com.example.grpc.service.GreeterService;
import com.example.grpc.service.AuthService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import io.grpc.ClientInterceptor;
import org.junit.jupiter.api.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class AuthTest {

    private static Server server;
    private ManagedChannel channel;
    private GreeterGrpc.GreeterBlockingStub greeterStub;

    @BeforeAll
    static void startServer() throws Exception {
        // Start in-process gRPC server with AuthInterceptor
        server = ServerBuilder.forPort(9090)
                .addService(new AuthService())
                .addService(new GreeterService())
                .intercept(new AuthInterceptor())
                .build()
                .start();
        System.out.println("🚀 gRPC Test Server started on port 9090");
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.shutdownNow();
        }
    }

    @BeforeEach
    void setUp() {
        // Create client channel
        channel = ManagedChannelBuilder.forAddress("localhost", 9090)
                .usePlaintext()
                .build();

        greeterStub = GreeterGrpc.newBlockingStub(channel);
        AuthGrpc.AuthBlockingStub authStub = AuthGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() {
        channel.shutdownNow();
    }

    @Test
    void sayHelloWithoutToken_shouldFail() {
        HelloRequest request = HelloRequest.newBuilder()
                .setName("TestUser")
                .build();

        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> {
            greeterStub.sayHello(request);
        });

        assertTrue(exception.getMessage().contains("UNAUTHENTICATED"));
    }

    @Test
    void sayHelloWithBasicAuth_shouldSucceed() {
        String username = "admin";
        String password = "password";
        String credentials = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));

        Metadata headers = new Metadata();
        headers.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Basic " + credentials);

        ClientInterceptor interceptor = MetadataUtils.newAttachHeadersInterceptor(headers);
        GreeterGrpc.GreeterBlockingStub stubWithAuth = greeterStub.withInterceptors(interceptor);

        String message = stubWithAuth.sayHello(HelloRequest.newBuilder().setName("TestUser").build()).getMessage();

        assertEquals("Hello TestUser", message);
    }
}
