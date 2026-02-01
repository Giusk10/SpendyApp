package com.spendy.auth.Repository;

import com.spendy.auth.Data.RefreshToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test per IRefreshTokenRepository
 * Verifica le operazioni CRUD sui refresh token
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Test per IRefreshTokenRepository")
class IRefreshTokenRepositoryTest {

    @Mock
    private IRefreshTokenRepository refreshTokenRepository;

    private RefreshToken testRefreshToken;

    @BeforeEach
    void setUp() {
        // Crea un refresh token di test valido per 30 giorni
        testRefreshToken = new RefreshToken(
                "testToken123",
                "testuser",
                LocalDateTime.now().plusDays(30)
        );
    }

    /**
     * Verifica che un refresh token venga trovato correttamente dal token
     */
    @Test
    @DisplayName("Trova refresh token dal token string")
    void testFindByToken_Success() {
        // Simula che il token venga trovato
        when(refreshTokenRepository.findByToken("testToken123"))
                .thenReturn(Optional.of(testRefreshToken));

        // Esegue la ricerca
        Optional<RefreshToken> result = refreshTokenRepository.findByToken("testToken123");

        // Verifica che il token sia stato trovato
        assertTrue(result.isPresent());
        assertEquals("testToken123", result.get().getToken());
        assertEquals("testuser", result.get().getUsername());
        assertNotNull(result.get().getExpiryDate());

        // Verifica che il metodo sia stato chiamato una volta
        verify(refreshTokenRepository, times(1)).findByToken("testToken123");
    }

    /**
     * Verifica che la ricerca di un token inesistente restituisca Optional.empty()
     */
    @Test
    @DisplayName("Restituisce Optional.empty() per token non trovato")
    void testFindByToken_NotFound() {
        // Simula che il token non venga trovato
        when(refreshTokenRepository.findByToken("nonexistentToken"))
                .thenReturn(Optional.empty());

        // Esegue la ricerca
        Optional<RefreshToken> result = refreshTokenRepository.findByToken("nonexistentToken");

        // Verifica che il risultato sia vuoto
        assertFalse(result.isPresent());

        // Verifica che il metodo sia stato chiamato una volta
        verify(refreshTokenRepository, times(1)).findByToken("nonexistentToken");
    }

    /**
     * Verifica che il salvataggio di un refresh token funzioni correttamente
     */
    @Test
    @DisplayName("Salva un nuovo refresh token")
    void testSaveRefreshToken() {
        // Simula il salvataggio
        when(refreshTokenRepository.save(testRefreshToken))
                .thenReturn(testRefreshToken);

        // Esegue il salvataggio
        RefreshToken savedToken = refreshTokenRepository.save(testRefreshToken);

        // Verifica che il token sia stato salvato
        assertNotNull(savedToken);
        assertEquals("testToken123", savedToken.getToken());
        assertEquals("testuser", savedToken.getUsername());

        // Verifica che il metodo sia stato chiamato una volta
        verify(refreshTokenRepository, times(1)).save(testRefreshToken);
    }

    /**
     * Verifica che l'eliminazione di refresh token per username funzioni
     */
    @Test
    @DisplayName("Elimina refresh token per username")
    void testDeleteByUsername() {
        String username = "testuser";

        // Non serve configurare when() per void methods
        doNothing().when(refreshTokenRepository).deleteByUsername(username);

        // Esegue l'eliminazione
        refreshTokenRepository.deleteByUsername(username);

        // Verifica che il metodo sia stato chiamato una volta
        verify(refreshTokenRepository, times(1)).deleteByUsername(username);
    }

    /**
     * Verifica che l'eliminazione di un refresh token specifico funzioni
     */
    @Test
    @DisplayName("Elimina un refresh token specifico")
    void testDeleteRefreshToken() {
        // Non serve configurare when() per void methods
        doNothing().when(refreshTokenRepository).delete(testRefreshToken);

        // Esegue l'eliminazione
        refreshTokenRepository.delete(testRefreshToken);

        // Verifica che il metodo sia stato chiamato una volta
        verify(refreshTokenRepository, times(1)).delete(testRefreshToken);
    }

    /**
     * Verifica la gestione di token con date di scadenza diverse
     */
    @Test
    @DisplayName("Gestisce correttamente token con date di scadenza diverse")
    void testTokensWithDifferentExpiryDates() {
        // Token valido
        RefreshToken validToken = new RefreshToken(
                "validToken",
                "user1",
                LocalDateTime.now().plusDays(15)
        );

        // Token scaduto
        RefreshToken expiredToken = new RefreshToken(
                "expiredToken",
                "user2",
                LocalDateTime.now().minusDays(1)
        );

        // Simula il ritrovamento dei token
        when(refreshTokenRepository.findByToken("validToken"))
                .thenReturn(Optional.of(validToken));
        when(refreshTokenRepository.findByToken("expiredToken"))
                .thenReturn(Optional.of(expiredToken));

        // Verifica token valido
        Optional<RefreshToken> validResult = refreshTokenRepository.findByToken("validToken");
        assertTrue(validResult.isPresent());
        assertTrue(validResult.get().getExpiryDate().isAfter(LocalDateTime.now()));

        // Verifica token scaduto
        Optional<RefreshToken> expiredResult = refreshTokenRepository.findByToken("expiredToken");
        assertTrue(expiredResult.isPresent());
        assertTrue(expiredResult.get().getExpiryDate().isBefore(LocalDateTime.now()));
    }

    /**
     * Verifica che il repository gestisca correttamente multiple operazioni
     */
    @Test
    @DisplayName("Gestisce operazioni multiple su token diversi")
    void testMultipleTokenOperations() {
        RefreshToken token1 = new RefreshToken("token1", "user1", LocalDateTime.now().plusDays(30));
        RefreshToken token2 = new RefreshToken("token2", "user2", LocalDateTime.now().plusDays(30));

        // Simula salvataggio di entrambi i token
        when(refreshTokenRepository.save(token1)).thenReturn(token1);
        when(refreshTokenRepository.save(token2)).thenReturn(token2);

        // Salva entrambi i token
        refreshTokenRepository.save(token1);
        refreshTokenRepository.save(token2);

        // Verifica che entrambi siano stati salvati
        verify(refreshTokenRepository, times(1)).save(token1);
        verify(refreshTokenRepository, times(1)).save(token2);
    }
}

