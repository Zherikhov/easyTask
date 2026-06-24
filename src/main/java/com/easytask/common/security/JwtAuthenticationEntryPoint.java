package com.easytask.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Without this, Spring Security's default behavior for a missing/invalid/expired
 * bearer token (no httpBasic/formLogin configured) is to answer 403, not 401 —
 * indistinguishable from a genuine permission-denied AppException (see
 * InsufficientWorkspaceRoleException etc.), which the frontend must NOT treat as
 * "log the user out". This entry point makes "not authenticated at all" its own
 * 401 response so app.js's existing 401 handler (clear session, redirect to
 * login) actually fires instead of dead-ending on a raw error toast.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"message\":\"Session expired. Please log in again.\"}");
    }
}
