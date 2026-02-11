package com.arsiwooqq.orderservice.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.arsiwooqq.orderservice.client.AuthServiceClient;
import com.arsiwooqq.orderservice.dto.ApiResponse;
import com.arsiwooqq.orderservice.dto.ValidateTokenRequest;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private AuthServiceClient authServiceClient;

    @InjectMocks
    private JwtFilter jwtFilter;

    @BeforeEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should continue filter chain when Authorization header does not exist")
    void givenNoAuthorizationHeader_whenDoFilterInternal_thenContinueFilterChain() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Should continue filter chain when Authorization header does not start with Bearer")
    void givenInvalidAuthorizationHeaderFormat_whenDoFilterInternal_thenContinueFilterChain() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("TOKEN");

        // When
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Should authenticate user when valid token provided and validated")
    void givenValidToken_whenDoFilterInternal_thenAuthenticateUser() throws ServletException, IOException {
        // Given
        var token = JWT.create()
                .withSubject("123")
                .withClaim("role", "USER")
                .withExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .sign(Algorithm.HMAC256("SECRET"));

        var authResponse = ApiResponse.<Boolean>builder()
                .success(true)
                .data(true)
                .build();

        // When
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(authServiceClient.validate(any(ValidateTokenRequest.class))).thenReturn(authResponse);

        jwtFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("123", authentication.getPrincipal());
        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("USER")));
    }

    @Test
    @DisplayName("Should continue filter chain without authentication when token validation returns false")
    void givenInvalidToken_whenDoFilterInternal_thenContinueWithoutAuthentication() throws ServletException, IOException {
        // Given
        var token = "TOKEN";

        var authResponse = ApiResponse.<Boolean>builder()
                .success(true)
                .data(false)
                .build();

        // When
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(authServiceClient.validate(any(ValidateTokenRequest.class))).thenReturn(authResponse);

        jwtFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Should continue filter chain without authentication when authServiceClient throws exception")
    void givenAuthServiceException_whenDoFilterInternal_thenContinueWithoutAuthentication() throws ServletException, IOException {
        // Given
        var token = "TOKEN";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(authServiceClient.validate(any(ValidateTokenRequest.class))).thenThrow(new RuntimeException());

        // When
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}