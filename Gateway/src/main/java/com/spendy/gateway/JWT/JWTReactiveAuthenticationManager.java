package com.spendy.gateway.JWT;

import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

import java.util.Collections;

public class JWTReactiveAuthenticationManager implements ReactiveAuthenticationManager {

    private final TokenManager tokenManager;

    public JWTReactiveAuthenticationManager(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        Object credentials = authentication.getCredentials();
        if (credentials == null) {
            return Mono.error(new RuntimeException("No JWT token found in credentials"));
        }
        String token = credentials.toString();

        String username = tokenManager.verifyToken(token);
        if (username != null) {
            return Mono.just(new UsernamePasswordAuthenticationToken(username, token, Collections.emptyList()));
        }
        return Mono.empty();
    }
}