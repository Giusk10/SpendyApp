package com.spendy.auth.Utility;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test per la classe AuthResult
 * Verifica la corretta gestione dei risultati di autenticazione
 */
@DisplayName("Test per AuthResult")
class AuthResultTest {

    /**
     * Verifica la creazione di AuthResult con access e refresh token
     */
    @Test
    @DisplayName("Creazione AuthResult con entrambi i token")
    void testCreateAuthResultWithBothTokens() {
        // Crea un risultato con entrambi i token
        AuthResult result = new AuthResult(
                StatusAuth.SUCCESS,
                "accessToken123",
                "refreshToken456"
        );

        // Verifica i valori
        assertEquals(StatusAuth.SUCCESS, result.getStatusAuth());
        assertEquals("accessToken123", result.getAccessToken());
        assertEquals("refreshToken456", result.getRefreshToken());
    }

    /**
     * Verifica la creazione di AuthResult con solo access token
     */
    @Test
    @DisplayName("Creazione AuthResult con solo access token")
    void testCreateAuthResultWithAccessTokenOnly() {
        // Crea un risultato con solo access token
        AuthResult result = new AuthResult(
                StatusAuth.SUCCESS,
                "accessToken123"
        );

        // Verifica i valori
        assertEquals(StatusAuth.SUCCESS, result.getStatusAuth());
        assertEquals("accessToken123", result.getAccessToken());
        assertNull(result.getRefreshToken());
    }

    /**
     * Verifica la creazione di AuthResult per errore (senza token)
     */
    @Test
    @DisplayName("Creazione AuthResult per errore senza token")
    void testCreateAuthResultForError() {
        // Crea un risultato di errore
        AuthResult result = new AuthResult(
                StatusAuth.USER_NOT_FOUND,
                null
        );

        // Verifica i valori
        assertEquals(StatusAuth.USER_NOT_FOUND, result.getStatusAuth());
        assertNull(result.getAccessToken());
        assertNull(result.getRefreshToken());
    }

    /**
     * Verifica tutti gli stati di autenticazione possibili
     */
    @Test
    @DisplayName("Verifica tutti gli stati di autenticazione")
    void testAllAuthStatuses() {
        // SUCCESS
        AuthResult success = new AuthResult(StatusAuth.SUCCESS, "token");
        assertEquals(StatusAuth.SUCCESS, success.getStatusAuth());

        // USER_NOT_FOUND
        AuthResult userNotFound = new AuthResult(StatusAuth.USER_NOT_FOUND, null);
        assertEquals(StatusAuth.USER_NOT_FOUND, userNotFound.getStatusAuth());

        // INVALID_CREDENTIALS
        AuthResult invalidCreds = new AuthResult(StatusAuth.INVALID_CREDENTIALS, null);
        assertEquals(StatusAuth.INVALID_CREDENTIALS, invalidCreds.getStatusAuth());

        // USER_ALREADY_EXISTS
        AuthResult userExists = new AuthResult(StatusAuth.USER_ALREADY_EXISTS, null);
        assertEquals(StatusAuth.USER_ALREADY_EXISTS, userExists.getStatusAuth());

        // TOKEN_EXPIRED
        AuthResult tokenExpired = new AuthResult(StatusAuth.TOKEN_EXPIRED, null);
        assertEquals(StatusAuth.TOKEN_EXPIRED, tokenExpired.getStatusAuth());

        // TOKEN_INVALID
        AuthResult tokenInvalid = new AuthResult(StatusAuth.TOKEN_INVALID, null);
        assertEquals(StatusAuth.TOKEN_INVALID, tokenInvalid.getStatusAuth());
    }

    /**
     * Verifica che i getter restituiscano i valori corretti
     */
    @Test
    @DisplayName("Verifica getter dei valori")
    void testGetters() {
        String accessToken = "myAccessToken";
        String refreshToken = "myRefreshToken";
        StatusAuth status = StatusAuth.SUCCESS;

        AuthResult result = new AuthResult(status, accessToken, refreshToken);

        // Verifica che i getter restituiscano i valori attesi
        assertSame(status, result.getStatusAuth());
        assertSame(accessToken, result.getAccessToken());
        assertSame(refreshToken, result.getRefreshToken());
    }

    /**
     * Verifica la gestione di valori null
     */
    @Test
    @DisplayName("Gestione corretta di valori null")
    void testNullValues() {
        // Tutti i parametri null
        AuthResult result1 = new AuthResult(null, null, null);
        assertNull(result1.getStatusAuth());
        assertNull(result1.getAccessToken());
        assertNull(result1.getRefreshToken());

        // Solo status non null
        AuthResult result2 = new AuthResult(StatusAuth.SUCCESS, null, null);
        assertEquals(StatusAuth.SUCCESS, result2.getStatusAuth());
        assertNull(result2.getAccessToken());
        assertNull(result2.getRefreshToken());

        // Costruttore a 2 parametri con null
        AuthResult result3 = new AuthResult(StatusAuth.TOKEN_INVALID, null);
        assertEquals(StatusAuth.TOKEN_INVALID, result3.getStatusAuth());
        assertNull(result3.getAccessToken());
        assertNull(result3.getRefreshToken());
    }

    /**
     * Verifica il pattern di utilizzo tipico per login con successo
     */
    @Test
    @DisplayName("Pattern di utilizzo tipico per login con successo")
    void testTypicalSuccessfulLoginPattern() {
        AuthResult result = new AuthResult(
                StatusAuth.SUCCESS,
                "jwt.access.token",
                "refresh-uuid-token"
        );

        // Simula controllo nel codice client
        if (result.getStatusAuth() == StatusAuth.SUCCESS) {
            assertNotNull(result.getAccessToken());
            assertNotNull(result.getRefreshToken());
            assertTrue(result.getAccessToken().contains("."));
        }
    }

    /**
     * Verifica il pattern di utilizzo tipico per errore di autenticazione
     */
    @Test
    @DisplayName("Pattern di utilizzo tipico per errore di autenticazione")
    void testTypicalAuthErrorPattern() {
        AuthResult result = new AuthResult(
                StatusAuth.INVALID_CREDENTIALS,
                null
        );

        // Simula controllo nel codice client
        if (result.getStatusAuth() != StatusAuth.SUCCESS) {
            assertNull(result.getAccessToken());
            assertTrue(
                result.getStatusAuth() == StatusAuth.INVALID_CREDENTIALS ||
                result.getStatusAuth() == StatusAuth.USER_NOT_FOUND
            );
        }
    }
}

