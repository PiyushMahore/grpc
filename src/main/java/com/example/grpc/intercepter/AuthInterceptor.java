package com.example.grpc.intercepter;

import io.grpc.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class AuthInterceptor implements ServerInterceptor {

    private static final String VALID_USERNAME = "admin";
    private static final String VALID_PASSWORD = "password";

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String authHeader = headers.get(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER));
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            System.out.println("❌ Missing or invalid Authorization header for " + call.getMethodDescriptor().getFullMethodName());
            call.close(Status.UNAUTHENTICATED.withDescription("Missing or invalid Authorization header"), new Metadata());
            return new ServerCall.Listener<>() {};
        }

        // Decode Base64 username:password
        String base64Credentials = authHeader.substring("Basic ".length());
        String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
        String[] parts = credentials.split(":", 2);

        if (parts.length != 2 || !parts[0].trim().equals(VALID_USERNAME) || !parts[1].trim().equals(VALID_PASSWORD)) {
            System.out.println("❌ Invalid credentials for " + call.getMethodDescriptor().getFullMethodName());
            call.close(Status.UNAUTHENTICATED.withDescription("Invalid username or password"), new Metadata());
            return new ServerCall.Listener<>() {};
        }

        // Proceed if valid
        return next.startCall(call, headers);
    }
}