package com.spendy.auth.Service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.spendy.auth.Data.User;
import com.spendy.auth.Repository.IUserRepository;
import com.spendy.auth.Utility.AuthResult;
import com.spendy.auth.Utility.StatusAuth;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.junit.jupiter.api.Assertions.*;

// Estensione di Mockito per i test
@ExtendWith(MockitoExtension.class)
class WebAuthClientServiceTest {

    // Mock del repository utente
    @Mock
    private IUserRepository userRepository;

    // Mock del WebClient per simulare chiamate HTTP (RETURNS_DEEP_STUBS permette chiamate nidificate)
    @Mock(answer = RETURNS_DEEP_STUBS)
    private WebClient webClient;

    // Inietta i mock nel service da testare
    @InjectMocks
    private WebAuthClientService webAuthClientService;

    /**
     * Verifica che il metodo linkHouseToUser restituisca INVALID_CREDENTIALS se il token o il codice della casa sono null.
     */
    @Test
    void linkHouseToUser_NullTokenOrHouseCode_ReturnsInvalidCredentials() {
        // Test con token nullo
        AuthResult result1 = webAuthClientService.linkHouseToUser(null, "house");
        // Test con codice casa nullo
        AuthResult result2 = webAuthClientService.linkHouseToUser("token", null);
        // Verifica che venga restituito INVALID_CREDENTIALS in entrambi i casi
        assertEquals(StatusAuth.INVALID_CREDENTIALS, result1.getStatusAuth());
        assertEquals(StatusAuth.INVALID_CREDENTIALS, result2.getStatusAuth());
    }

    /**
     * Verifica che il metodo linkHouseToUser restituisca USER_NOT_FOUND se il token non è valido.
     * Simula la chiamata HTTP che restituisce null (token non valido), ci si aspetta USER_NOT_FOUND.
     */
    @Test
    void linkHouseToUser_InvalidToken_ReturnsUserNotFound() {
        // Simula risposta null dal WebClient (token non valido)
        when(webClient.post().uri(anyString()).bodyValue(any()).retrieve().bodyToMono(Map.class).block())
                .thenReturn(null);
        // Esegue il metodo
        AuthResult result = webAuthClientService.linkHouseToUser("invalidtoken", "house");
        // Verifica USER_NOT_FOUND
        assertEquals(StatusAuth.USER_NOT_FOUND, result.getStatusAuth());
    }

    /**
     * Verifica che il metodo linkHouseToUser restituisca USER_NOT_FOUND se l'utente non viene trovato nel database.
     * Simula una chiamata HTTP che restituisce un utente, ma il repository non trova l'utente.
     */
    @Test
    void linkHouseToUser_UserNotFound_ReturnsUserNotFound() {
        // Simula risposta del WebClient con username
        when(webClient.post().uri(anyString()).bodyValue(any()).retrieve().bodyToMono(Map.class).block())
                .thenReturn(Map.of("username", "user"));
        // Simula utente non trovato nel repository
        when(userRepository.findByUsername("user")).thenReturn(null);
        // Esegue il metodo
        AuthResult result = webAuthClientService.linkHouseToUser("token", "house");
        // Verifica USER_NOT_FOUND
        assertEquals(StatusAuth.USER_NOT_FOUND, result.getStatusAuth());
    }

    /**
     * Verifica i casi in cui l'utente è già collegato a una casa (USER_ALREADY_LINKED) o può essere collegato con successo (SUCCESS).
     * Simula una chiamata HTTP che restituisce un utente con un codice casa già associato.
     */
    @ParameterizedTest
    @CsvSource({
            "true, SUCCESS",
            "false, USER_ALREADY_LINKED"
    })
    void linkHouseToUser_UserAlreadyLinked_VariousCases(boolean checkpwResult, StatusAuth expectedStatus) {
        // Mock utente
        User user = mock(User.class);
        // Simula houseUser già presente
        when(user.getHouseUser()).thenReturn("hashedHouse");
        // Simula risposta del WebClient con username
        when(webClient.post().uri(anyString()).bodyValue(any()).retrieve().bodyToMono(Map.class).block())
                .thenReturn(Map.of("username", "user"));
        // Simula utente trovato nel repository
        when(userRepository.findByUsername("user")).thenReturn(user);

        // Mock statico di BCrypt.checkpw per forzare il risultato
        try (var mocked = org.mockito.Mockito.mockStatic(BCrypt.class)) {
            mocked.when(() -> BCrypt.checkpw(eq("house"), anyString())).thenReturn(checkpwResult);
            // Esegue il metodo
            AuthResult result = webAuthClientService.linkHouseToUser("token", "house");
            // Verifica lo stato atteso
            assertEquals(expectedStatus, result.getStatusAuth());
        }
    }

    /**
     * Verifica che il collegamento tra utente e casa avvenga con successo e venga chiamato il metodo setHouseUser.
     * Simula una chiamata HTTP che restituisce un utente e verifica che il repository venga aggiornato.
     */
    @Test
    void linkHouseToUser_SuccessfulLink_ReturnsSuccess() {
        // Mock utente
        User user = mock(User.class);
        // Simula username e houseUser null
        when(user.getUsername()).thenReturn("user");
        when(user.getHouseUser()).thenReturn(null);
        // Simula risposta del WebClient con username
        when(webClient.post().uri(anyString()).bodyValue(any()).retrieve().bodyToMono(Map.class).block())
                .thenReturn(Map.of("username", "user"));
        // Simula utente trovato nel repository
        when(userRepository.findByUsername("user")).thenReturn(user);

        // Esegue il metodo
        AuthResult result = webAuthClientService.linkHouseToUser("token", "house");
        // Verifica SUCCESS
        assertEquals(StatusAuth.SUCCESS, result.getStatusAuth());
        // Verifica che il metodo setHouseUser sia stato chiamato una volta
        verify(userRepository, times(1)).setHouseUser(eq("user"), anyString());
    }
}