package com.aicareer.jobradar.module.auth.service;

import com.aicareer.jobradar.module.auth.dto.AuthResponse;
import com.aicareer.jobradar.module.auth.dto.LoginRequest;
import com.aicareer.jobradar.module.auth.dto.RegisterRequest;
import com.aicareer.jobradar.module.auth.model.User;
import com.aicareer.jobradar.module.auth.repository.UserRepository;
import com.aicareer.jobradar.security.JwtTokenProvider;
import com.aicareer.jobradar.shared.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw AppException.conflict("Email already registered");
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .build();

        userRepository.save(user);
        String token = jwtTokenProvider.generateToken(user.getEmail());
        return AuthResponse.of(token, user.getId(), user.getName(), user.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = (User) auth.getPrincipal();
        String token = jwtTokenProvider.generateToken(auth);
        return AuthResponse.of(token, user.getId(), user.getName(), user.getEmail());
    }
}
