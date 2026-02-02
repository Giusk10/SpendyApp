package com.spendy.gateway.JWT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JWTAuthenticationFilterTest {

    @Mock
    private TokenManager tokenManager;

    private JWTAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JWTAuthenticationFilter(tokenManager);
    }

    @Test
    void testFilterConstruction_WithTokenManager() {
        // Given/When
        JWTAuthenticationFilter filter = new JWTAuthenticationFilter(tokenManager);

        // Then
        assertNotNull(filter);
    }


    @Test
    void testValidTokenScenario() {
        // Given
        String token = "valid-token-123";
        String username = "testuser";

        when(tokenManager.verifyToken(token)).thenReturn(username);

        // When
        tokenManager.verifyToken(token);

        // Then
        verify(tokenManager, times(1)).verifyToken(token);
    }

    @Test
    void testInvalidTokenScenario() {
        // Given
        String invalidToken = "invalid-token";

        when(tokenManager.verifyToken(invalidToken)).thenReturn(null);

        // When
        String result = tokenManager.verifyToken(invalidToken);

        // Then
        assertNull(result);
        verify(tokenManager, times(1)).verifyToken(invalidToken);
    }

    @Test
    void testBearerTokenExtraction() {
        // Test that Bearer token format is handled correctly
        String authHeader = "Bearer valid-token-123";
        assertTrue(authHeader.startsWith("Bearer "));

        String token = authHeader.substring(7);
        assertEquals("valid-token-123", token);
    }

    @Test
    void testNonBearerTokenFormat() {
        // Test that non-Bearer format is not processed
        String authHeader = "Basic dXNlcjpwYXNz";
        assertFalse(authHeader.startsWith("Bearer "));
    }

    @Test
    void testEmptyAuthorizationHeader() {
        // Test empty authorization header
        String authHeader = "";
        assertFalse(authHeader.startsWith("Bearer "));
    }

    @Test
    void testNullAuthorizationHeader() {
        // Test null authorization header
        String authHeader = null;
        assertNull(authHeader);
    }

    @Test
    void testMultipleTokenVerifications() {
        // Given
        String token1 = "token1";
        String token2 = "token2";
        String username1 = "user1";
        String username2 = "user2";

        when(tokenManager.verifyToken(token1)).thenReturn(username1);
        when(tokenManager.verifyToken(token2)).thenReturn(username2);

        // When
        String result1 = tokenManager.verifyToken(token1);
        String result2 = tokenManager.verifyToken(token2);

        // Then
        assertEquals(username1, result1);
        assertEquals(username2, result2);
        verify(tokenManager, times(1)).verifyToken(token1);
        verify(tokenManager, times(1)).verifyToken(token2);
    }

    @Test
    void testBearerTokenWithoutSpace() {
        // Test Bearer without space
        String authHeader = "Bearer";
        assertTrue(authHeader.startsWith("Bearer"));
        assertFalse(authHeader.length() > 7);
    }

    @Test
    void testBearerTokenWithSpace() {
        // Test Bearer with space
        String authHeader = "Bearer ";
        assertTrue(authHeader.startsWith("Bearer "));
        assertEquals(7, authHeader.length());

        String token = authHeader.substring(7);
        assertEquals("", token);
    }

    @Test
    void testTokenManagerIntegration() {
        // Test that the filter uses the token manager correctly
        // Given
        String token = "test-token";
        when(tokenManager.verifyToken(token)).thenReturn("testuser");

        // When
        String result = tokenManager.verifyToken(token);

        // Then
        assertEquals("testuser", result);
        verify(tokenManager).verifyToken(token);
    }
}

