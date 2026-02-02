package com.spendy.gateway;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Test base per verificare che la classe GatewayApplication sia caricabile
 */
class GatewayApplicationTest {

    @Test
    void testMainMethodExists() {
        // Given & When & Then - verifica che il metodo main esista
        assertDoesNotThrow(() -> {
            GatewayApplication.class.getDeclaredMethod("main", String[].class);
        });
    }
}

