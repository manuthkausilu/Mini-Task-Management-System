package com.example.mini_task.controller;

import com.example.mini_task.dto.auth.AuthResponse;
import com.example.mini_task.dto.auth.LoginRequest;
import com.example.mini_task.dto.auth.RegisterRequest;
import com.example.mini_task.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationService authenticationService;

    /**
     * Register a new user
     * POST /api/v1/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Received registration request for email: {}", request.getEmail());
        AuthResponse response = authenticationService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Login user and get JWT token
     * POST /api/v1/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Received login request for email: {}", request.getEmail());
        AuthResponse response = authenticationService.login(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}

