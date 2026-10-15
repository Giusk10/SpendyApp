package com.spendy.gateway.Utility;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GatewayResultTest {

    @Test
    void testConstructor_WithValidParameters() {
        // Given
        StatusGateway status = StatusGateway.TOKEN_GENERATION_SUCCESS;
        String token = "test-token-123";

        // When
        GatewayResult result = new GatewayResult(status, token);

        // Then
        assertNotNull(result);
        assertEquals(status, result.getStatusGateway());
        assertEquals(token, result.getToken());
    }

    @Test
    void testConstructor_WithNullToken() {
        // Given
        StatusGateway status = StatusGateway.TOKEN_GENERATION_FAILED;

        // When
        GatewayResult result = new GatewayResult(status, null);

        // Then
        assertNotNull(result);
        assertEquals(status, result.getStatusGateway());
        assertNull(result.getToken());
    }

    @Test
    void testSetStatusGateway() {
        // Given
        GatewayResult result = new GatewayResult(StatusGateway.TOKEN_GENERATION_SUCCESS, "token");

        // When
        result.setStatusGateway(StatusGateway.TOKEN_VERIFICATION_SUCCESS);

        // Then
        assertEquals(StatusGateway.TOKEN_VERIFICATION_SUCCESS, result.getStatusGateway());
    }

    @Test
    void testSetToken() {
        // Given
        GatewayResult result = new GatewayResult(StatusGateway.TOKEN_GENERATION_SUCCESS, "old-token");

        // When
        result.setToken("new-token");

        // Then
        assertEquals("new-token", result.getToken());
    }

    @Test
    void testGetStatusGateway() {
        // Given
        StatusGateway status = StatusGateway.TOKEN_VERIFICATION_FAILED;
        GatewayResult result = new GatewayResult(status, "token");

        // When
        StatusGateway retrievedStatus = result.getStatusGateway();

        // Then
        assertEquals(status, retrievedStatus);
    }

    @Test
    void testGetToken() {
        // Given
        String token = "test-token-456";
        GatewayResult result = new GatewayResult(StatusGateway.TOKEN_GENERATION_SUCCESS, token);

        // When
        String retrievedToken = result.getToken();

        // Then
        assertEquals(token, retrievedToken);
    }

    @Test
    void testAllStatusTypes() {
        // Test with all different status types
        StatusGateway[] statuses = {
            StatusGateway.TOKEN_GENERATION_SUCCESS,
            StatusGateway.TOKEN_GENERATION_FAILED,
            StatusGateway.TOKEN_VERIFICATION_SUCCESS,
            StatusGateway.TOKEN_VERIFICATION_FAILED,
            StatusGateway.USER_NOT_FOUND,
            StatusGateway.USER_ALREADY_EXISTS,
            StatusGateway.INVALID_CREDENTIALS,
            StatusGateway.INTERNAL_ERROR
        };

        for (StatusGateway status : statuses) {
            GatewayResult result = new GatewayResult(status, "token");
            assertEquals(status, result.getStatusGateway());
        }
    }
}

