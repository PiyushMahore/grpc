package com.example.grpc.service;

import com.example.grpc.AuthGrpc;
import com.example.grpc.LoginRequest;
import com.example.grpc.LoginResponse;
import io.grpc.stub.StreamObserver;

public class AuthService extends AuthGrpc.AuthImplBase {

    @Override
    public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
        String username = request.getUsername();
        String password = request.getPassword();

        // Basic validation
        if ("admin".equals(username) && "password".equals(password)) {
            String token = "valid-token"; // In real apps, generate JWT
            LoginResponse response = LoginResponse.newBuilder()
                    .setToken(token)
                    .build();
            responseObserver.onNext(response);
        } else {
            // Return empty token for invalid credentials
            LoginResponse response = LoginResponse.newBuilder()
                    .setToken("")
                    .build();
            responseObserver.onNext(response);
        }

        responseObserver.onCompleted();
    }
}
