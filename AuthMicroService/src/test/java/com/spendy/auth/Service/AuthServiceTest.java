package com.spendy.auth.Service;

import com.spendy.auth.Data.RefreshToken;
import com.spendy.auth.Data.User;
import com.spendy.auth.Repository.IRefreshTokenRepository;
import com.spendy.auth.Repository.IUserRepository;
import com.spendy.auth.Utility.AuthResult;
import com.spendy.auth.Utility.StatusAuth;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Estensione di Mockito per i test
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // Mock del repository utente
    @Mock
    private IUserRepository userRepository;

    // Mock del repository refresh token
    @Mock
    private IRefreshTokenRepository refreshTokenRepository;

    // Mock del WebClient per chiamate HTTP
    @Mock(answer = RETURNS_DEEP_STUBS)
    private WebClient webClient;

    // Inietta i mock nel service da testare
    @InjectMocks
    private AuthService authService;

    /**
     * Verifica che la registrazione fallisca se l'utente esiste già (restituisce USER_ALREADY_EXISTS)
     */
    @Test
    void register_UserAlreadyExists_ReturnsUserAlreadyExists() {
        // Simula utente già esistente
        when(userRepository.findByUsername("user")).thenReturn(new User());

        // Esegue la registrazione
        AuthResult result = authService.register("user", "nome", "pass", "cognome", "email@test.com");

        // Verifica che lo stato sia USER_ALREADY_EXISTS
        assertEquals(StatusAuth.USER_ALREADY_EXISTS, result.getStatusAuth());
    }

    /**
     * Verifica che la registrazione di un nuovo utente abbia successo, venga salvato e restituisca il token
     */
    @Test
    void register_NewUser_ReturnsSuccess() {
        // Simula utente non esistente
        when(userRepository.findByUsername("user")).thenReturn(null);
        // Simula risposta del WebClient con token
        when(webClient.post().uri(anyString()).bodyValue(any()).retrieve().bodyToMono(Map.class).block())
                .thenReturn(Map.of("token", "testtoken"));

        // Esegue la registrazione
        AuthResult result = authService.register("user", "nome", "pass", "cognome", "email@test.com");

        // Verifica che lo stato sia SUCCESS e il token sia corretto
        assertEquals(StatusAuth.SUCCESS, result.getStatusAuth());
        assertEquals("testtoken", result.getAccessToken());
        // Verifica che il salvataggio sia stato chiamato una volta
        verify(userRepository, times(1)).save(any(User.class));
    }

    /**
     * Verifica che il login fallisca se l'utente non viene trovato (restituisce USER_NOT_FOUND)
     */
    @Test
    void login_UserNotFound_ReturnsUserNotFound() {
        // Simula utente non trovato per username ed email
        when(userRepository.findByUsername("user")).thenReturn(null);
        when(userRepository.findByEmail("user")).thenReturn(null);

        // Esegue il login
        AuthResult result = authService.login("user", "pass");

        // Verifica che lo stato sia USER_NOT_FOUND
        assertEquals(StatusAuth.USER_NOT_FOUND, result.getStatusAuth());
    }

    /**
     * Verifica che il login fallisca se la password è errata (restituisce INVALID_CREDENTIALS)
     */
    @Test
    void login_InvalidPassword_ReturnsInvalidCredentials() {
        // Crea utente con password hashata
        User user = new User("user", "nome", BCrypt.hashpw("rightpass", BCrypt.gensalt()), "cognome", "email@test.com");
        // Simula utente trovato
        when(userRepository.findByUsername("user")).thenReturn(user);

        // Esegue il login con password errata
        AuthResult result = authService.login("user", "wrongpass");

        // Verifica che lo stato sia INVALID_CREDENTIALS
        assertEquals(StatusAuth.INVALID_CREDENTIALS, result.getStatusAuth());
    }

    /**
     * Verifica che il login con credenziali valide restituisca SUCCESS con access token e refresh token
     */
    @Test
    void login_ValidCredentials_ReturnsSuccessWithTokens() {
        // Crea utente con password hashata
        String password = "correctpass";
        User user = new User("user", "nome", BCrypt.hashpw(password, BCrypt.gensalt()), "cognome", "email@test.com");

        // Simula utente trovato
        when(userRepository.findByUsername("user")).thenReturn(user);

        // Simula risposta del WebClient con access token
        when(webClient.post().uri(anyString()).bodyValue(any()).retrieve().bodyToMono(Map.class).block())
                .thenReturn(Map.of("token", "accessToken123"));

        // Esegue il login con password corretta
        AuthResult result = authService.login("user", password);

        // Verifica che lo stato sia SUCCESS
        assertEquals(StatusAuth.SUCCESS, result.getStatusAuth());
        // Verifica che access token sia presente
        assertEquals("accessToken123", result.getAccessToken());
        // Verifica che refresh token sia presente
        assertNotNull(result.getRefreshToken());
        // Verifica che il refresh token sia stato salvato
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    /**
     * Verifica che il refresh token valido generi nuovi token (rotation)
     */
    @Test
    void refreshAccessToken_ValidToken_ReturnsNewTokens() {
        String oldRefreshToken = "oldRefreshToken123";
        String username = "testuser";

        // Crea un refresh token valido
        RefreshToken refreshToken = new RefreshToken(oldRefreshToken, username, LocalDateTime.now().plusDays(30));

        // Simula che il token venga trovato
        when(refreshTokenRepository.findByToken(oldRefreshToken)).thenReturn(Optional.of(refreshToken));

        // Simula risposta del WebClient con nuovo access token
        when(webClient.post().uri(anyString()).bodyValue(any()).retrieve().bodyToMono(Map.class).block())
                .thenReturn(Map.of("token", "newAccessToken123"));

        // Esegue il refresh
        AuthResult result = authService.refreshAccessToken(oldRefreshToken);

        // Verifica che lo stato sia SUCCESS
        assertEquals(StatusAuth.SUCCESS, result.getStatusAuth());
        // Verifica che il nuovo access token sia presente
        assertEquals("newAccessToken123", result.getAccessToken());
        // Verifica che il nuovo refresh token sia presente
        assertNotNull(result.getRefreshToken());
        // Verifica che il vecchio token sia stato eliminato (rotation)
        verify(refreshTokenRepository, times(1)).delete(refreshToken);
        // Verifica che un nuovo refresh token sia stato salvato
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    /**
     * Verifica che un refresh token non trovato restituisca TOKEN_INVALID
     */
    @Test
    void refreshAccessToken_TokenNotFound_ReturnsTokenInvalid() {
        String invalidToken = "invalidToken";

        // Simula che il token non venga trovato
        when(refreshTokenRepository.findByToken(invalidToken)).thenReturn(Optional.empty());

        // Esegue il refresh
        AuthResult result = authService.refreshAccessToken(invalidToken);

        // Verifica che lo stato sia TOKEN_INVALID
        assertEquals(StatusAuth.TOKEN_INVALID, result.getStatusAuth());
        // Verifica che non venga salvato nessun nuovo token
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    /**
     * Verifica che un refresh token scaduto restituisca TOKEN_EXPIRED
     */
    @Test
    void refreshAccessToken_ExpiredToken_ReturnsTokenExpired() {
        String expiredToken = "expiredToken123";
        String username = "testuser";

        // Crea un refresh token scaduto
        RefreshToken refreshToken = new RefreshToken(expiredToken, username, LocalDateTime.now().minusDays(1));

        // Simula che il token venga trovato
        when(refreshTokenRepository.findByToken(expiredToken)).thenReturn(Optional.of(refreshToken));

        // Esegue il refresh
        AuthResult result = authService.refreshAccessToken(expiredToken);

        // Verifica che lo stato sia TOKEN_EXPIRED
        assertEquals(StatusAuth.TOKEN_EXPIRED, result.getStatusAuth());
        // Verifica che il token scaduto sia stato eliminato
        verify(refreshTokenRepository, times(1)).delete(refreshToken);
        // Verifica che non venga salvato nessun nuovo token
        verify(refreshTokenRepository, times(1)).delete(any(RefreshToken.class));
    }

    /**
     * Verifica che getUserFromToken con token valido restituisca l'utente
     */
    @Test
    void getUserFromToken_ValidToken_ReturnsUser() {
        String validToken = "validAccessToken";
        String username = "testuser";
        User expectedUser = new User(username, "Test", "password", "User", "test@test.com");

        // Simula risposta del WebClient con username
        when(webClient.post().uri(anyString()).bodyValue(any()).retrieve().bodyToMono(Map.class).block())
                .thenReturn(Map.of("username", username));

        // Simula che l'utente venga trovato
        when(userRepository.findByUsername(username)).thenReturn(expectedUser);

        // Esegue la verifica del token
        User result = authService.getUserFromToken(validToken);

        // Verifica che l'utente sia quello atteso
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals("Test", result.getName());
    }

    /**
     * Verifica che getUserFromToken con token invalido restituisca null
     */
    @Test
    void getUserFromToken_InvalidToken_ReturnsNull() {
        String invalidToken = "invalidAccessToken";

        // Simula risposta del WebClient con username null (token non valido)
        Map<String, String> responseMap = new java.util.HashMap<>();
        responseMap.put("username", null);
        when(webClient.post().uri(anyString()).bodyValue(any()).retrieve().bodyToMono(Map.class).block())
                .thenReturn(responseMap);

        // Esegue la verifica del token
        User result = authService.getUserFromToken(invalidToken);

        // Verifica che il risultato sia null
        assertNull(result);
        // Verifica che non sia stata chiamata la repository
        verify(userRepository, never()).findByUsername(anyString());
    }

    /**
     * Verifica che updateUserProfile con token valido aggiorni l'utente
     */
    @Test
    void updateUserProfile_ValidToken_UpdatesUser() {
        String validToken = "validAccessToken";
        String username = "testuser";
        User existingUser = new User(username, "OldName", "password", "OldSurname", "test@test.com");
        User updatedUser = new User();
        updatedUser.setName("NewName");
        updatedUser.setSurname("NewSurname");

        // Simula risposta del WebClient con username
        when(webClient.post().uri(anyString()).bodyValue(any()).retrieve().bodyToMono(Map.class).block())
                .thenReturn(Map.of("username", username));

        // Simula che l'utente venga trovato
        when(userRepository.findByUsername(username)).thenReturn(existingUser);

        // Esegue l'aggiornamento del profilo
        AuthResult result = authService.updateUserProfile(validToken, updatedUser);

        // Verifica che lo stato sia SUCCESS
        assertEquals(StatusAuth.SUCCESS, result.getStatusAuth());
        // Verifica che i dati siano stati aggiornati
        assertEquals("NewName", existingUser.getName());
        assertEquals("NewSurname", existingUser.getSurname());
        // Verifica che l'utente sia stato salvato
        verify(userRepository, times(1)).save(existingUser);
    }

    /**
     * Verifica che updateUserProfile con token invalido restituisca TOKEN_INVALID
     */
    @Test
    void updateUserProfile_InvalidToken_ReturnsTokenInvalid() {
        String invalidToken = "invalidAccessToken";
        User updatedUser = new User();
        updatedUser.setName("NewName");

        // Simula risposta del WebClient con username null
        Map<String, String> responseMap = new java.util.HashMap<>();
        responseMap.put("username", null);
        when(webClient.post().uri(anyString()).bodyValue(any()).retrieve().bodyToMono(Map.class).block())
                .thenReturn(responseMap);

        // Esegue l'aggiornamento del profilo
        AuthResult result = authService.updateUserProfile(invalidToken, updatedUser);

        // Verifica che lo stato sia TOKEN_INVALID
        assertEquals(StatusAuth.TOKEN_INVALID, result.getStatusAuth());
        // Verifica che non sia stato salvato nessun utente
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * Verifica che updateUserProfile con utente non trovato restituisca USER_NOT_FOUND
     */
    @Test
    void updateUserProfile_UserNotFound_ReturnsUserNotFound() {
        String validToken = "validAccessToken";
        String username = "nonexistentuser";
        User updatedUser = new User();
        updatedUser.setName("NewName");

        // Simula risposta del WebClient con username
        when(webClient.post().uri(anyString()).bodyValue(any()).retrieve().bodyToMono(Map.class).block())
                .thenReturn(Map.of("username", username));

        // Simula che l'utente non venga trovato
        when(userRepository.findByUsername(username)).thenReturn(null);

        // Esegue l'aggiornamento del profilo
        AuthResult result = authService.updateUserProfile(validToken, updatedUser);

        // Verifica che lo stato sia USER_NOT_FOUND
        assertEquals(StatusAuth.USER_NOT_FOUND, result.getStatusAuth());
        // Verifica che non sia stato salvato nessun utente
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * Verifica che la registrazione con email già esistente restituisca USER_ALREADY_EXISTS
     */
    @Test
    void register_EmailAlreadyExists_ReturnsUserAlreadyExists() {
        // Simula che l'username non esista ma l'email sia già presente
        when(userRepository.findByUsername("newuser")).thenReturn(null);
        when(userRepository.findByEmail("existing@test.com")).thenReturn(new User());

        // Esegue la registrazione
        AuthResult result = authService.register("newuser", "nome", "pass", "cognome", "existing@test.com");

        // Verifica che lo stato sia USER_ALREADY_EXISTS
        assertEquals(StatusAuth.USER_ALREADY_EXISTS, result.getStatusAuth());
        // Verifica che non sia stato salvato nessun utente
        verify(userRepository, never()).save(any(User.class));
    }
}
