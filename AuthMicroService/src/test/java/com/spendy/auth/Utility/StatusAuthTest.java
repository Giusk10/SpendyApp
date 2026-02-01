package com.spendy.auth.Utility;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test per l'enum StatusAuth
 * Verifica che tutti gli stati di autenticazione siano definiti correttamente
 */
@DisplayName("Test per StatusAuth")
class StatusAuthTest {

    /**
     * Verifica che tutti gli stati di autenticazione esistano
     */
    @Test
    @DisplayName("Verifica esistenza di tutti gli stati")
    void testAllStatusesExist() {
        // Verifica che tutti gli stati previsti esistano
        assertNotNull(StatusAuth.SUCCESS);
        assertNotNull(StatusAuth.USER_NOT_FOUND);
        assertNotNull(StatusAuth.INVALID_CREDENTIALS);
        assertNotNull(StatusAuth.USER_ALREADY_EXISTS);
        assertNotNull(StatusAuth.TOKEN_EXPIRED);
        assertNotNull(StatusAuth.TOKEN_INVALID);
        assertNotNull(StatusAuth.LINKED_ERROR);
        assertNotNull(StatusAuth.USER_ALREADY_LINKED);
        assertNotNull(StatusAuth.USERS_NOT_FOUND);
        assertNotNull(StatusAuth.USERS_FOUNDED);
    }

    /**
     * Verifica il numero totale di stati
     */
    @Test
    @DisplayName("Verifica numero totale di stati")
    void testTotalNumberOfStatuses() {
        StatusAuth[] statuses = StatusAuth.values();
        assertEquals(10, statuses.length, "Dovrebbero esserci 10 stati di autenticazione");
    }

    /**
     * Verifica che valueOf funzioni correttamente
     */
    @Test
    @DisplayName("Verifica valueOf per ogni stato")
    void testValueOf() {
        assertEquals(StatusAuth.SUCCESS, StatusAuth.valueOf("SUCCESS"));
        assertEquals(StatusAuth.USER_NOT_FOUND, StatusAuth.valueOf("USER_NOT_FOUND"));
        assertEquals(StatusAuth.INVALID_CREDENTIALS, StatusAuth.valueOf("INVALID_CREDENTIALS"));
        assertEquals(StatusAuth.USER_ALREADY_EXISTS, StatusAuth.valueOf("USER_ALREADY_EXISTS"));
        assertEquals(StatusAuth.TOKEN_EXPIRED, StatusAuth.valueOf("TOKEN_EXPIRED"));
        assertEquals(StatusAuth.TOKEN_INVALID, StatusAuth.valueOf("TOKEN_INVALID"));
        assertEquals(StatusAuth.LINKED_ERROR, StatusAuth.valueOf("LINKED_ERROR"));
        assertEquals(StatusAuth.USER_ALREADY_LINKED, StatusAuth.valueOf("USER_ALREADY_LINKED"));
        assertEquals(StatusAuth.USERS_NOT_FOUND, StatusAuth.valueOf("USERS_NOT_FOUND"));
        assertEquals(StatusAuth.USERS_FOUNDED, StatusAuth.valueOf("USERS_FOUNDED"));
    }

    /**
     * Verifica che valueOf con valore invalido lanci eccezione
     */
    @Test
    @DisplayName("Verifica eccezione per valueOf invalido")
    void testValueOfInvalid() {
        assertThrows(IllegalArgumentException.class, () -> StatusAuth.valueOf("INVALID_STATUS"));
    }

    /**
     * Verifica che ogni stato sia univoco
     */
    @Test
    @DisplayName("Verifica unicità degli stati")
    void testStatusUniqueness() {
        StatusAuth[] statuses = StatusAuth.values();

        // Verifica che ogni stato sia diverso dagli altri
        for (int i = 0; i < statuses.length; i++) {
            for (int j = i + 1; j < statuses.length; j++) {
                assertNotEquals(statuses[i], statuses[j],
                    "Gli stati " + statuses[i] + " e " + statuses[j] + " non dovrebbero essere uguali");
            }
        }
    }

    /**
     * Verifica l'utilizzo tipico in switch case
     */
    @Test
    @DisplayName("Verifica utilizzo in switch case")
    void testSwitchCaseUsage() {
        String message = getMessageForStatus(StatusAuth.SUCCESS);
        assertEquals("Operazione completata con successo", message);

        message = getMessageForStatus(StatusAuth.USER_NOT_FOUND);
        assertEquals("Utente non trovato", message);

        message = getMessageForStatus(StatusAuth.INVALID_CREDENTIALS);
        assertEquals("Credenziali non valide", message);

        message = getMessageForStatus(StatusAuth.TOKEN_EXPIRED);
        assertEquals("Token scaduto", message);
    }

    /**
     * Helper method per testare l'uso in switch
     */
    private String getMessageForStatus(StatusAuth status) {
        return switch (status) {
            case SUCCESS -> "Operazione completata con successo";
            case USER_NOT_FOUND -> "Utente non trovato";
            case INVALID_CREDENTIALS -> "Credenziali non valide";
            case USER_ALREADY_EXISTS -> "Utente già esistente";
            case TOKEN_EXPIRED -> "Token scaduto";
            case TOKEN_INVALID -> "Token non valido";
            case LINKED_ERROR -> "Errore di collegamento";
            case USER_ALREADY_LINKED -> "Utente già collegato";
            case USERS_NOT_FOUND -> "Utenti non trovati";
            case USERS_FOUNDED -> "Utenti trovati";
        };
    }

    /**
     * Verifica che gli stati possano essere confrontati
     */
    @Test
    @DisplayName("Verifica confronto tra stati")
    void testStatusComparison() {
        StatusAuth status1 = StatusAuth.SUCCESS;
        StatusAuth status2 = StatusAuth.SUCCESS;
        StatusAuth status3 = StatusAuth.USER_NOT_FOUND;

        // Stesso stato
        assertEquals(status1, status2);
        assertSame(status1, status2); // enum può usare ==

        // Stati diversi
        assertNotEquals(status1, status3);
        assertNotSame(status1, status3);
    }

    /**
     * Verifica ordine degli enum (ordinal)
     */
    @Test
    @DisplayName("Verifica ordinal degli stati")
    void testOrdinal() {
        assertEquals(0, StatusAuth.SUCCESS.ordinal());
        assertEquals(1, StatusAuth.USER_NOT_FOUND.ordinal());
        assertEquals(2, StatusAuth.INVALID_CREDENTIALS.ordinal());
        assertEquals(3, StatusAuth.USER_ALREADY_EXISTS.ordinal());
        assertEquals(4, StatusAuth.TOKEN_EXPIRED.ordinal());
        assertEquals(5, StatusAuth.TOKEN_INVALID.ordinal());
        assertEquals(6, StatusAuth.LINKED_ERROR.ordinal());
        assertEquals(7, StatusAuth.USER_ALREADY_LINKED.ordinal());
        assertEquals(8, StatusAuth.USERS_NOT_FOUND.ordinal());
        assertEquals(9, StatusAuth.USERS_FOUNDED.ordinal());
    }

    /**
     * Verifica che name() restituisca il nome corretto
     */
    @Test
    @DisplayName("Verifica name() per ogni stato")
    void testName() {
        assertEquals("SUCCESS", StatusAuth.SUCCESS.name());
        assertEquals("USER_NOT_FOUND", StatusAuth.USER_NOT_FOUND.name());
        assertEquals("INVALID_CREDENTIALS", StatusAuth.INVALID_CREDENTIALS.name());
        assertEquals("TOKEN_EXPIRED", StatusAuth.TOKEN_EXPIRED.name());
    }

    /**
     * Verifica categorizzazione degli stati per tipo di errore
     */
    @Test
    @DisplayName("Verifica categorizzazione degli stati")
    void testStatusCategorization() {
        // Stati di successo
        assertTrue(isSuccessStatus(StatusAuth.SUCCESS));
        assertTrue(isSuccessStatus(StatusAuth.USERS_FOUNDED));

        // Stati di errore autenticazione
        assertTrue(isAuthErrorStatus(StatusAuth.USER_NOT_FOUND));
        assertTrue(isAuthErrorStatus(StatusAuth.INVALID_CREDENTIALS));

        // Stati di errore token
        assertTrue(isTokenErrorStatus(StatusAuth.TOKEN_EXPIRED));
        assertTrue(isTokenErrorStatus(StatusAuth.TOKEN_INVALID));

        // Stati di errore esistenza utente
        assertTrue(isUserExistenceErrorStatus(StatusAuth.USER_ALREADY_EXISTS));
        assertTrue(isUserExistenceErrorStatus(StatusAuth.USER_ALREADY_LINKED));
    }

    private boolean isSuccessStatus(StatusAuth status) {
        return status == StatusAuth.SUCCESS || status == StatusAuth.USERS_FOUNDED;
    }

    private boolean isAuthErrorStatus(StatusAuth status) {
        return status == StatusAuth.USER_NOT_FOUND ||
               status == StatusAuth.INVALID_CREDENTIALS ||
               status == StatusAuth.USERS_NOT_FOUND;
    }

    private boolean isTokenErrorStatus(StatusAuth status) {
        return status == StatusAuth.TOKEN_EXPIRED ||
               status == StatusAuth.TOKEN_INVALID;
    }

    private boolean isUserExistenceErrorStatus(StatusAuth status) {
        return status == StatusAuth.USER_ALREADY_EXISTS ||
               status == StatusAuth.USER_ALREADY_LINKED;
    }
}

