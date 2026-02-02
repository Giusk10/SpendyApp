package com.spendy.gateway;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    @Test
    void testHomeMethodExists() {
        // Given & When & Then - verifica che il metodo home esista
        assertDoesNotThrow(() -> {
            GatewayApplication.class.getDeclaredMethod("home");
        });
    }

    @Test
    void testGatewayApplicationClassExists() {
        // Given & When
        GatewayApplication instance = new GatewayApplication();

        // Then
        assertNotNull(instance);
    }
}

