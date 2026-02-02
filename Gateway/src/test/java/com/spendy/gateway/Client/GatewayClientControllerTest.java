package com.spendy.gateway.Client;

import com.spendy.gateway.Service.GatwayClientService;
import com.spendy.gateway.Utility.GatewayResult;
import com.spendy.gateway.Utility.StatusGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GatewayClientControllerTest {

    @Mock
    private GatwayClientService gatwayClientService;

    @InjectMocks
    private GatewayClientController gatewayClientController;

    @Test
    void testGenerateToken_Success() {
        // Given
        String username = "testuser";
        String token = "generated-token-123";
        GatewayResult result = new GatewayResult(StatusGateway.TOKEN_GENERATION_SUCCESS, token);

        when(gatwayClientService.generateToken(username)).thenReturn(result);

        // When
        ResponseEntity<?> response = gatewayClientController.generateToken(Map.of("username", username));

        // Then
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals(token, body.get("token"));
    }

    @Test
    void testGenerateToken_Failure() {
        // Given
        String username = "testuser";
        GatewayResult result = new GatewayResult(StatusGateway.TOKEN_GENERATION_FAILED, null);

        when(gatwayClientService.generateToken(username)).thenReturn(result);

        // When
        ResponseEntity<?> response = gatewayClientController.generateToken(Map.of("username", username));

        // Then
        assertEquals(500, response.getStatusCode().value());
        assertEquals("Token generation failed", response.getBody());
    }

    @Test
    void testVerifyToken_Success() {
        // Given
        String token = "valid-token-123";
        String username = "testuser";
        GatewayResult result = new GatewayResult(StatusGateway.TOKEN_VERIFICATION_SUCCESS, username);

        when(gatwayClientService.verifyToken(token)).thenReturn(result);

        // When
        ResponseEntity<?> response = gatewayClientController.verifyToken(Map.of("token", token));

        // Then
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals(username, body.get("username"));
    }

    @Test
    void testVerifyToken_Failure() {
        // Given
        String token = "invalid-token";
        GatewayResult result = new GatewayResult(StatusGateway.TOKEN_VERIFICATION_FAILED, null);

        when(gatwayClientService.verifyToken(token)).thenReturn(result);

        // When
        ResponseEntity<?> response = gatewayClientController.verifyToken(Map.of("token", token));

        // Then
        assertEquals(401, response.getStatusCode().value());
        assertEquals("Token verification failed", response.getBody());
    }

    @Test
    void testGenerateToken_ThrowsException() {
        // Given
        when(gatwayClientService.generateToken(anyString()))
            .thenThrow(new IllegalArgumentException("Username cannot be null or empty"));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            gatewayClientController.generateToken(Map.of("username", ""));
        });
    }

    @Test
    void testVerifyToken_ThrowsException() {
        // Given
        when(gatwayClientService.verifyToken(anyString()))
            .thenThrow(new IllegalArgumentException("Token cannot be null or empty"));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            gatewayClientController.verifyToken(Map.of("token", ""));
        });
    }

    @Test
    void testController_NotNull() {
        assertNotNull(gatewayClientController);
    }
}
