package com.example.grpc.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.ClientInterceptor;
import io.grpc.stub.MetadataUtils;
import com.example.grpc.GreeterGrpc;
import com.example.grpc.HelloRequest;
import com.example.grpc.HelloResponse;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class GreeterClientTest {

    public static void main(String[] args) {
        // 1⃣ Create the channel
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", 9090)
                .usePlaintext()
                .build();

        // 2⃣ Prepare Basic Auth credentials
        String username = "admin";
        String password = "password";
        String credentials = username + ":" + password;
        String base64Credentials = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        // 3⃣ Build metadata with Authorization header
        Metadata headers = new Metadata();
        Metadata.Key<String> AUTHORIZATION =
                Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
        headers.put(AUTHORIZATION, "Basic " + base64Credentials);

        // 4⃣ Create a client interceptor that attaches these headers
        ClientInterceptor authInterceptor = MetadataUtils.newAttachHeadersInterceptor(headers);

        // 5⃣ Apply the interceptor to the stub
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(channel)
                .withInterceptors(authInterceptor);

        // 6⃣ Build the request
        HelloRequest request = HelloRequest.newBuilder()
                .setName("Piyush")
                .build();

        System.out.println("Sending request to Main Server...");
        HelloResponse response = stub.sayHello(request);

        System.out.println("✅ Final response from Main Server: " + response.getMessage());

        // 7⃣ Clean up
        channel.shutdown();
    }
}
