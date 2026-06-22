package com.easytask.auth.service;

import com.easytask.common.config.InviteProperties;
import com.easytask.common.config.RegistrationProperties;
import com.easytask.auth.dto.AcceptInviteRequest;
import com.easytask.auth.dto.AuthResponse;
import com.easytask.auth.dto.LoginRequest;
import com.easytask.auth.dto.RegisterRequest;
import com.easytask.auth.entity.User;
import com.easytask.auth.entity.UserStatus;
import com.easytask.auth.exception.EmailAlreadyUsedException;
import com.easytask.auth.exception.InvalidCredentialsException;
import com.easytask.auth.exception.InvalidInviteTokenException;
import com.easytask.auth.exception.RegistrationDisabledException;
import com.easytask.auth.repository.UserRepository;
import com.easytask.common.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RegistrationProperties registrationProperties;
    private final InviteProperties inviteProperties;

    public AuthService(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        RegistrationProperties registrationProperties,
                        InviteProperties inviteProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.registrationProperties = registrationProperties;
        this.inviteProperties = inviteProperties;
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

        return new AuthResponse(jwtService.generateToken(user.getId()), user.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (user.getStatus() != UserStatus.ACTIVE
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return new AuthResponse(jwtService.generateToken(user.getId()), user.getEmail());
    }

    @Transactional
    public User createPendingUser(String email, String displayName) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setDisplayName(displayName != null && !displayName.isBlank() ? displayName : defaultDisplayName(email));
        user.setStatus(UserStatus.PENDING);
        user.setInviteToken(UUID.randomUUID().toString());
        user.setInviteTokenExpiresAt(OffsetDateTime.now().plusDays(inviteProperties.getExpirationDays()));
        return userRepository.saveAndFlush(user);
    }

    @Transactional
    public AuthResponse acceptInvite(AcceptInviteRequest request) {
        User user = userRepository.findByInviteToken(request.token())
                .orElseThrow(InvalidInviteTokenException::new);

        if (user.getStatus() != UserStatus.PENDING
                || user.getInviteTokenExpiresAt() == null
                || user.getInviteTokenExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new InvalidInviteTokenException();
        }

        user.setPasswordHash(passwordEncoder.encode(request.password()));
        if (request.displayName() != null && !request.displayName().isBlank()) {
            user.setDisplayName(request.displayName());
        }
        user.setStatus(UserStatus.ACTIVE);
        user.setInviteToken(null);
        user.setInviteTokenExpiresAt(null);
        userRepository.saveAndFlush(user);

        return new AuthResponse(jwtService.generateToken(user.getId()), user.getEmail());
    }

    private String defaultDisplayName(String email) {
        String local = email.split("@", 2)[0];
        return local.substring(0, 1).toUpperCase(Locale.ROOT) + local.substring(1);
    }
}
