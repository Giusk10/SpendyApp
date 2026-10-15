package com.spendyteam.expense.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAuthServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private UserAuthService userAuthService;

    @BeforeEach
    void setUp() {
        userAuthService = new UserAuthService(webClient, "http://localhost:7860");

        lenient().when(webClient.post()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.bodyValue(any())).thenAnswer(invocation -> requestBodySpec);
        lenient().when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void testGetUsernameFromTokenValid() {
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(Map.of("username", "john_doe")));

        String username = userAuthService.getUsernameFromToken("valid-jwt-token");

        assertEquals("john_doe", username);
    }

    @Test
    void testGetUsernameFromTokenInvalidOrExpired() {
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(Collections.emptyMap()));

        String username = userAuthService.getUsernameFromToken("invalid-token");

        assertNull(username);
    }

    @Test
    void testGetUsernameFromTokenNullOrEmpty() {
        assertNull(userAuthService.getUsernameFromToken(null));
        assertNull(userAuthService.getUsernameFromToken(""));
        assertNull(userAuthService.getUsernameFromToken("   "));
    }

    @Test
    void testGetUsernameFromTokenNetworkFailure() {
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.error(new RuntimeException("Gateway unreachable")));

        String username = userAuthService.getUsernameFromToken("token-causes-error");

        assertNull(username);
    }

    @Test
    void testGetUsernameFromTokenViaRestAlias() {
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(Map.of("username", "alice")));

        String username = userAuthService.getUsernameFromTokenViaRest("alias-token");

        assertEquals("alice", username);
    }
}
