package com.example.mini_task.service.impl;

import com.example.mini_task.dto.auth.AuthResponse;
import com.example.mini_task.dto.auth.LoginRequest;
import com.example.mini_task.dto.auth.RegisterRequest;
import com.example.mini_task.entity.User;
import com.example.mini_task.exception.AuthenticationException;
import com.example.mini_task.repo.UserRepository;
import com.example.mini_task.security.JwtTokenProvider;
import com.example.mini_task.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Override
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        return registerWithRole(request, User.Role.USER);
    }

    @Override
    public AuthResponse registerAdmin(RegisterRequest request) {
        log.info("Registering new admin with email: {}", request.getEmail());

        return registerWithRole(request, User.Role.ADMIN);
    }

    private AuthResponse registerWithRole(RegisterRequest request, User.Role role) {

        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            log.error("User already exists with email: {}", request.getEmail());
            throw new AuthenticationException("Email already registered");
        }

        // Validate password confirmation
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            log.error("Password confirmation does not match");
            throw new AuthenticationException("Password and confirm password do not match");
        }

        // Create new user
        User user = User.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with id: {}", savedUser.getId());

        // Generate token
        String token = tokenProvider.generateToken(savedUser);

        return AuthResponse.builder()
                .token(token)
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .role(savedUser.getRole())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Authenticating user with email: {}", request.getEmail());

        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.error("User not found with email: {}", request.getEmail());
                    return new AuthenticationException("Invalid email or password");
                });

        // Validate password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.error("Invalid password for user: {}", request.getEmail());
            throw new AuthenticationException("Invalid email or password");
        }

        log.info("User authenticated successfully: {}", request.getEmail());

        // Generate token
        String token = tokenProvider.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .build();
    }
}



