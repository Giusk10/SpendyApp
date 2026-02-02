package com.spendy.gateway.Service;

import com.spendy.gateway.JWT.TokenManager;
import com.spendy.gateway.Utility.GatewayResult;
import com.spendy.gateway.Utility.StatusGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test unitari per GatwayClientService
 */
@ExtendWith(MockitoExtension.class)
class GatwayClientServiceTest {

    @Mock
    private TokenManager tokenManager;

    @InjectMocks
    private GatwayClientService service;

    @Test
    @DisplayName("generateToken - Con username valido - Genera token con successo")
    void testGenerateToken_WithValidUsername_Success() {
        // Given
        String username = "testuser";
        String expectedToken = "token123";
        when(tokenManager.generateToken(username)).thenReturn(expectedToken);

        // When
        GatewayResult result = service.generateToken(username);

        // Then
        assertNotNull(result);
        assertEquals(StatusGateway.TOKEN_GENERATION_SUCCESS, result.getStatusGateway());
        assertEquals(expectedToken, result.getToken());
        verify(tokenManager, times(1)).generateToken(username);
    }

    @Test
    @DisplayName("generateToken - Con username null - Lancia eccezione")
    void testGenerateToken_WithNullUsername_ThrowsException() {
        // Given & When & Then
        assertThrows(IllegalArgumentException.class, () ->
            service.generateToken(null)
        );
        verify(tokenManager, never()).generateToken(any());
    }

    @Test
    @DisplayName("generateToken - Con username vuoto - Lancia eccezione")
    void testGenerateToken_WithEmptyUsername_ThrowsException() {
        // Given & When & Then
        assertThrows(IllegalArgumentException.class, () ->
            service.generateToken("")
        );
        verify(tokenManager, never()).generateToken(any());
    }

    @Test
    @DisplayName("generateToken - TokenManager ritorna null - Fallimento")
    void testGenerateToken_WhenTokenManagerReturnsNull_Failure() {
        // Given
        String username = "testuser";
        when(tokenManager.generateToken(username)).thenReturn(null);

        // When
        GatewayResult result = service.generateToken(username);

        // Then
        assertNotNull(result);
        assertEquals(StatusGateway.TOKEN_GENERATION_FAILED, result.getStatusGateway());
        assertNull(result.getToken());
        verify(tokenManager, times(1)).generateToken(username);
    }

    @Test
    @DisplayName("verifyToken - Con token valido - Verifica con successo")
    void testVerifyToken_WithValidToken_Success() {
        // Given
        String token = "validToken123";
        String expectedUsername = "testuser";
        when(tokenManager.verifyToken(token)).thenReturn(expectedUsername);

        // When
        GatewayResult result = service.verifyToken(token);

        // Then
        assertNotNull(result);
        assertEquals(StatusGateway.TOKEN_VERIFICATION_SUCCESS, result.getStatusGateway());
        assertEquals(expectedUsername, result.getToken());
        verify(tokenManager, times(1)).verifyToken(token);
    }

    @Test
    @DisplayName("verifyToken - Con token null - Lancia eccezione")
    void testVerifyToken_WithNullToken_ThrowsException() {
        // Given & When & Then
        assertThrows(IllegalArgumentException.class, () ->
            service.verifyToken(null)
        );
        verify(tokenManager, never()).verifyToken(any());
    }

    @Test
    @DisplayName("verifyToken - Con token vuoto - Lancia eccezione")
    void testVerifyToken_WithEmptyToken_ThrowsException() {
        // Given & When & Then
        assertThrows(IllegalArgumentException.class, () ->
            service.verifyToken("")
        );
        verify(tokenManager, never()).verifyToken(any());
    }

    @Test
    @DisplayName("verifyToken - Con token invalido - Fallimento")
    void testVerifyToken_WithInvalidToken_Failure() {
        // Given
        String token = "invalidToken";
        when(tokenManager.verifyToken(token)).thenReturn(null);

        // When
        GatewayResult result = service.verifyToken(token);

        // Then
        assertNotNull(result);
        assertEquals(StatusGateway.TOKEN_VERIFICATION_FAILED, result.getStatusGateway());
        assertNull(result.getToken());
        verify(tokenManager, times(1)).verifyToken(token);
    }
}

