package com.aicareer.jobradar.module.auth.controller;

import com.aicareer.jobradar.module.auth.dto.AuthResponse;
import com.aicareer.jobradar.module.auth.dto.LoginRequest;
import com.aicareer.jobradar.module.auth.dto.RegisterRequest;
import com.aicareer.jobradar.module.auth.service.AuthService;
import com.aicareer.jobradar.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok("Registration successful", authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Login successful", authService.login(request));
    }
}
