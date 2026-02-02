package com.spendy.gateway.JWT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JWTReactiveAuthenticationManagerTest {

    @Mock
    private TokenManager tokenManager;

    private JWTReactiveAuthenticationManager authenticationManager;

    @BeforeEach
    void setUp() {
        authenticationManager = new JWTReactiveAuthenticationManager(tokenManager);
    }

    @Test
    void testAuthenticate_ValidToken() {
        String token = "valid-token-123";
        String username = "testuser";
        Authentication inputAuth = new UsernamePasswordAuthenticationToken(null, token, Collections.emptyList());

        when(tokenManager.verifyToken(token)).thenReturn(username);

        Mono<Authentication> result = authenticationManager.authenticate(inputAuth);

        StepVerifier.create(result)
                .expectNextMatches(auth ->
                    auth.getPrincipal().equals(username) &&
                    auth.getCredentials().equals(token) &&
                    auth.isAuthenticated()
                )
                .verifyComplete();
    }

    @Test
    void testAuthenticate_InvalidToken() {
        String token = "invalid-token-123";
        Authentication inputAuth = new UsernamePasswordAuthenticationToken(null, token, Collections.emptyList());

        when(tokenManager.verifyToken(token)).thenReturn(null);

        Mono<Authentication> result = authenticationManager.authenticate(inputAuth);

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void testAuthenticate_NullCredentials() {
        Authentication inputAuth = new UsernamePasswordAuthenticationToken(null, null, Collections.emptyList());

        Mono<Authentication> result = authenticationManager.authenticate(inputAuth);

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }
}

