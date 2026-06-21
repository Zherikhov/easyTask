package com.easytask.auth.service;

import com.easytask.common.config.RegistrationProperties;
import com.easytask.auth.dto.AuthResponse;
import com.easytask.auth.dto.LoginRequest;
import com.easytask.auth.dto.RegisterRequest;
import com.easytask.auth.entity.User;
import com.easytask.auth.entity.UserStatus;
import com.easytask.auth.exception.EmailAlreadyUsedException;
import com.easytask.auth.exception.InvalidCredentialsException;
import com.easytask.auth.exception.RegistrationDisabledException;
import com.easytask.auth.repository.UserRepository;
import com.easytask.common.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RegistrationProperties registrationProperties;

    public AuthService(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        RegistrationProperties registrationProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.registrationProperties = registrationProperties;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        boolean firstUserBootstrap = userRepository.count() == 0;
        if (!registrationProperties.isPublicSignupEnabled() && !firstUserBootstrap) {
            throw new RegistrationDisabledException();
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyUsedException(request.email());
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName());
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        return new AuthResponse(jwtService.generateToken(user.getId()));
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (user.getStatus() != UserStatus.ACTIVE
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return new AuthResponse(jwtService.generateToken(user.getId()));
    }
}
