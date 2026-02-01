package com.spendy.auth.Data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test per la classe RefreshToken
 * Verifica la corretta gestione dei refresh token
 */
@DisplayName("Test per RefreshToken")
class RefreshTokenTest {

    private RefreshToken refreshToken;
    private LocalDateTime futureDate;

    @BeforeEach
    void setUp() {
        futureDate = LocalDateTime.now().plusDays(30);
        refreshToken = new RefreshToken("token123", "testuser", futureDate);
    }

    /**
     * Verifica la creazione di un refresh token con costruttore completo
     */
    @Test
    @DisplayName("Creazione refresh token con costruttore completo")
    void testFullConstructor() {
        String token = "myToken123";
        String username = "myUser";
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(30);

        RefreshToken rt = new RefreshToken(token, username, expiryDate);

        assertEquals(token, rt.getToken());
        assertEquals(username, rt.getUsername());
        assertEquals(expiryDate, rt.getExpiryDate());
    }

    /**
     * Verifica la creazione di un refresh token con costruttore vuoto
     */
    @Test
    @DisplayName("Creazione refresh token con costruttore vuoto")
    void testEmptyConstructor() {
        RefreshToken rt = new RefreshToken();

        assertNull(rt.getToken());
        assertNull(rt.getUsername());
        assertNull(rt.getExpiryDate());
    }

    /**
     * Verifica il getter per token
     */
    @Test
    @DisplayName("Verifica getter per token")
    void testGetToken() {
        assertEquals("token123", refreshToken.getToken());
    }

    /**
     * Verifica il getter per username
     */
    @Test
    @DisplayName("Verifica getter per username")
    void testGetUsername() {
        assertEquals("testuser", refreshToken.getUsername());
    }

    /**
     * Verifica il getter per expiryDate
     */
    @Test
    @DisplayName("Verifica getter per expiryDate")
    void testGetExpiryDate() {
        assertEquals(futureDate, refreshToken.getExpiryDate());
    }

    /**
     * Verifica la creazione di un token con data di scadenza nel futuro
     */
    @Test
    @DisplayName("Token con data di scadenza nel futuro")
    void testFutureExpiryDate() {
        LocalDateTime future = LocalDateTime.now().plusDays(30);
        RefreshToken rt = new RefreshToken("token", "user", future);

        assertTrue(rt.getExpiryDate().isAfter(LocalDateTime.now()));
    }

    /**
     * Verifica la creazione di un token con data di scadenza nel passato
     */
    @Test
    @DisplayName("Token con data di scadenza nel passato")
    void testPastExpiryDate() {
        LocalDateTime past = LocalDateTime.now().minusDays(1);
        RefreshToken rt = new RefreshToken("token", "user", past);

        assertTrue(rt.getExpiryDate().isBefore(LocalDateTime.now()));
    }

    /**
     * Verifica la gestione di valori null
     */
    @Test
    @DisplayName("Gestione valori null")
    void testNullValues() {
        RefreshToken rt = new RefreshToken(null, null, null);

        assertNull(rt.getToken());
        assertNull(rt.getUsername());
        assertNull(rt.getExpiryDate());
    }

    /**
     * Verifica la creazione di token con diversi periodi di validità
     */
    @Test
    @DisplayName("Token con diversi periodi di validità")
    void testDifferentValidityPeriods() {
        // Token valido per 7 giorni
        RefreshToken weekToken = new RefreshToken(
            "week",
            "user1",
            LocalDateTime.now().plusDays(7)
        );

        // Token valido per 30 giorni
        RefreshToken monthToken = new RefreshToken(
            "month",
            "user2",
            LocalDateTime.now().plusDays(30)
        );

        // Token valido per 90 giorni
        RefreshToken quarterToken = new RefreshToken(
            "quarter",
            "user3",
            LocalDateTime.now().plusDays(90)
        );

        assertTrue(weekToken.getExpiryDate().isBefore(monthToken.getExpiryDate()));
        assertTrue(monthToken.getExpiryDate().isBefore(quarterToken.getExpiryDate()));
    }

    /**
     * Verifica il formato tipico di un refresh token UUID
     */
    @Test
    @DisplayName("Verifica formato UUID del token")
    void testUUIDFormat() {
        String uuidToken = "550e8400-e29b-41d4-a716-446655440000";
        RefreshToken rt = new RefreshToken(uuidToken, "user", futureDate);

        assertTrue(rt.getToken().contains("-"));
        assertEquals(36, rt.getToken().length());
    }

    /**
     * Verifica scenario di verifica della validità del token
     */
    @Test
    @DisplayName("Scenario verifica validità token")
    void testTokenValidityScenario() {
        LocalDateTime now = LocalDateTime.now();

        // Token valido
        RefreshToken validToken = new RefreshToken(
            "valid",
            "user1",
            now.plusDays(30)
        );
        assertTrue(validToken.getExpiryDate().isAfter(now));

        // Token scaduto
        RefreshToken expiredToken = new RefreshToken(
            "expired",
            "user2",
            now.minusDays(1)
        );
        assertTrue(expiredToken.getExpiryDate().isBefore(now));

        // Token che scade tra poco
        RefreshToken soonExpiredToken = new RefreshToken(
            "soonexpired",
            "user3",
            now.plusMinutes(5)
        );
        assertTrue(soonExpiredToken.getExpiryDate().isAfter(now));
    }

    /**
     * Verifica la creazione di token per lo stesso utente
     */
    @Test
    @DisplayName("Multiple token per lo stesso utente")
    void testMultipleTokensForSameUser() {
        String username = "multideviceuser";

        RefreshToken token1 = new RefreshToken(
            "device1-token",
            username,
            LocalDateTime.now().plusDays(30)
        );

        RefreshToken token2 = new RefreshToken(
            "device2-token",
            username,
            LocalDateTime.now().plusDays(30)
        );

        RefreshToken token3 = new RefreshToken(
            "device3-token",
            username,
            LocalDateTime.now().plusDays(30)
        );

        // Stesso username ma token diversi
        assertEquals(username, token1.getUsername());
        assertEquals(username, token2.getUsername());
        assertEquals(username, token3.getUsername());

        assertNotEquals(token1.getToken(), token2.getToken());
        assertNotEquals(token2.getToken(), token3.getToken());
        assertNotEquals(token1.getToken(), token3.getToken());
    }

    /**
     * Verifica scenario di rotazione del token
     */
    @Test
    @DisplayName("Scenario rotazione token")
    void testTokenRotationScenario() {
        // Token originale
        RefreshToken oldToken = new RefreshToken(
            "oldToken123",
            "user",
            LocalDateTime.now().plusDays(30)
        );

        // Nuovo token dopo rotazione
        RefreshToken newToken = new RefreshToken(
            "newToken456",
            "user",
            LocalDateTime.now().plusDays(30)
        );

        // Stesso utente
        assertEquals(oldToken.getUsername(), newToken.getUsername());

        // Token diversi
        assertNotEquals(oldToken.getToken(), newToken.getToken());

        // Entrambi validi
        assertTrue(oldToken.getExpiryDate().isAfter(LocalDateTime.now()));
        assertTrue(newToken.getExpiryDate().isAfter(LocalDateTime.now()));
    }

    /**
     * Verifica che la data di scadenza sia immutabile dopo la creazione
     */
    @Test
    @DisplayName("Immutabilità della data di scadenza")
    void testExpiryDateImmutability() {
        LocalDateTime originalDate = LocalDateTime.of(2026, 12, 31, 23, 59, 59);
        RefreshToken rt = new RefreshToken("token", "user", originalDate);

        // La data restituita dovrebbe essere la stessa
        assertEquals(originalDate, rt.getExpiryDate());

        // Nota: LocalDateTime è immutabile, quindi non possiamo modificarlo
        LocalDateTime retrieved = rt.getExpiryDate();
        assertEquals(originalDate, retrieved);
    }

    /**
     * Verifica gestione di username con caratteri speciali
     */
    @Test
    @DisplayName("Username con caratteri speciali")
    void testSpecialCharactersInUsername() {
        String specialUsername = "user.name+test@domain";
        RefreshToken rt = new RefreshToken("token", specialUsername, futureDate);

        assertEquals(specialUsername, rt.getUsername());
    }

    /**
     * Verifica gestione di token molto lunghi
     */
    @Test
    @DisplayName("Token molto lunghi")
    void testVeryLongToken() {
        StringBuilder longToken = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longToken.append("a");
        }

        RefreshToken rt = new RefreshToken(longToken.toString(), "user", futureDate);

        assertEquals(1000, rt.getToken().length());
        assertEquals(longToken.toString(), rt.getToken());
    }

    /**
     * Verifica che due refresh token con gli stessi valori non siano lo stesso oggetto
     */
    @Test
    @DisplayName("Due token con stessi valori sono oggetti diversi")
    void testDifferentObjectsWithSameValues() {
        RefreshToken rt1 = new RefreshToken("token", "user", futureDate);
        RefreshToken rt2 = new RefreshToken("token", "user", futureDate);

        // Oggetti diversi
        assertNotSame(rt1, rt2);

        // Ma con gli stessi valori
        assertEquals(rt1.getToken(), rt2.getToken());
        assertEquals(rt1.getUsername(), rt2.getUsername());
        assertEquals(rt1.getExpiryDate(), rt2.getExpiryDate());
    }
}

