package com.spendy.gateway.Utility;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatusGatewayTest {

    @Test
    void testEnumValues() {
        // When
        StatusGateway[] values = StatusGateway.values();

        // Then
        assertNotNull(values);
        assertEquals(8, values.length);
    }

    @Test
    void testEnumContainsExpectedValues() {
        // Assert all expected values exist
        assertNotNull(StatusGateway.TOKEN_GENERATION_SUCCESS);
        assertNotNull(StatusGateway.TOKEN_GENERATION_FAILED);
        assertNotNull(StatusGateway.USER_NOT_FOUND);
        assertNotNull(StatusGateway.USER_ALREADY_EXISTS);
        assertNotNull(StatusGateway.INVALID_CREDENTIALS);
        assertNotNull(StatusGateway.TOKEN_VERIFICATION_SUCCESS);
        assertNotNull(StatusGateway.TOKEN_VERIFICATION_FAILED);
        assertNotNull(StatusGateway.INTERNAL_ERROR);
    }

    @Test
    void testValueOf() {
        // When/Then
        assertEquals(StatusGateway.TOKEN_GENERATION_SUCCESS,
                     StatusGateway.valueOf("TOKEN_GENERATION_SUCCESS"));
        assertEquals(StatusGateway.TOKEN_GENERATION_FAILED,
                     StatusGateway.valueOf("TOKEN_GENERATION_FAILED"));
        assertEquals(StatusGateway.TOKEN_VERIFICATION_SUCCESS,
                     StatusGateway.valueOf("TOKEN_VERIFICATION_SUCCESS"));
        assertEquals(StatusGateway.TOKEN_VERIFICATION_FAILED,
                     StatusGateway.valueOf("TOKEN_VERIFICATION_FAILED"));
        assertEquals(StatusGateway.USER_NOT_FOUND,
                     StatusGateway.valueOf("USER_NOT_FOUND"));
        assertEquals(StatusGateway.USER_ALREADY_EXISTS,
                     StatusGateway.valueOf("USER_ALREADY_EXISTS"));
        assertEquals(StatusGateway.INVALID_CREDENTIALS,
                     StatusGateway.valueOf("INVALID_CREDENTIALS"));
        assertEquals(StatusGateway.INTERNAL_ERROR,
                     StatusGateway.valueOf("INTERNAL_ERROR"));
    }

    @Test
    void testValueOf_InvalidValue() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            StatusGateway.valueOf("INVALID_STATUS");
        });
    }

    @Test
    void testEnumEquality() {
        // Given
        StatusGateway status1 = StatusGateway.TOKEN_GENERATION_SUCCESS;
        StatusGateway status2 = StatusGateway.TOKEN_GENERATION_SUCCESS;
        StatusGateway status3 = StatusGateway.TOKEN_GENERATION_FAILED;

        // Then
        assertEquals(status1, status2);
        assertNotEquals(status1, status3);
    }

    @Test
    void testEnumName() {
        // When/Then
        assertEquals("TOKEN_GENERATION_SUCCESS",
                     StatusGateway.TOKEN_GENERATION_SUCCESS.name());
        assertEquals("TOKEN_VERIFICATION_FAILED",
                     StatusGateway.TOKEN_VERIFICATION_FAILED.name());
        assertEquals("INTERNAL_ERROR",
                     StatusGateway.INTERNAL_ERROR.name());
    }
}

