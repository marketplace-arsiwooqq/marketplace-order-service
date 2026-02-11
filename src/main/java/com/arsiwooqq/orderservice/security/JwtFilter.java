package com.arsiwooqq.orderservice.security;

import com.auth0.jwt.JWT;
import com.arsiwooqq.orderservice.client.AuthServiceClient;
import com.arsiwooqq.orderservice.dto.ValidateTokenRequest;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {
    private final AuthServiceClient authServiceClient;

    @Override
    public void doFilterInternal(@NonNull HttpServletRequest request,
                                 @NonNull HttpServletResponse response,
                                 @NonNull FilterChain filterChain) throws ServletException, IOException {
        log.debug("Starting JWT filter");
        String token = getTokenFromRequest(request);
        log.debug("Token fetched from request");
        if (token != null) {
            if (validateToken(token)) {
                log.debug("Token is valid. Setting authentication");
                setAuthentication(token);
                request.setAttribute("jwt", token);
            } else {
                log.debug("Token is invalid");
            }
        } else {
            log.debug("Token is null");
        }
        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean validateToken(String token) {
        try {
            var response = authServiceClient.validate(new ValidateTokenRequest(token));
            log.debug("Received response from Auth Service: {}", response);
            return response != null && response.getData() != null && response.getData().equals(true);
        } catch (Exception e) {
            log.error("Error while validating token", e);
        }
        return false;
    }

    private String getRole(String token) {
        return JWT.decode(token).getClaim("role").asString();
    }

    private String getId(String token) {
        return JWT.decode(token).getSubject();
    }

    private void setAuthentication(String token) {
        var id = getId(token);
        var role = getRole(token);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        id,
                        null,
                        List.of(new SimpleGrantedAuthority(role))
                )
        );
        log.debug("Authenticated user with id {}. Role: {}", id, role);
    }
}
