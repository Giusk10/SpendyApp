package com.spendy.auth.Integration;

import com.spendy.auth.Data.RefreshToken;
import com.spendy.auth.Data.User;
import com.spendy.auth.Repository.IRefreshTokenRepository;
import com.spendy.auth.Repository.IUserRepository;
import com.spendy.auth.Service.AuthService;
import com.spendy.auth.Utility.AuthResult;
import com.spendy.auth.Utility.StatusAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Test di integrazione per il microservizio Auth
 * Verifica i flussi completi di autenticazione e gestione utenti
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Test di integrazione Auth Service")
class AuthServiceIntegrationTest {

    @Mock
    private IUserRepository userRepository;

    @Mock
    private IRefreshTokenRepository refreshTokenRepository;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private WebClient webClient;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, webClient, refreshTokenRepository);
    }

    /**
     * Test del flusso completo: registrazione -> login -> refresh -> update profile
     */
    @Test
    @DisplayName("Flusso completo: registrazione, login, refresh e aggiornamento profilo")
    void testCompleteAuthFlow() {
        String username = "newuser";
        String password = "password123";
        String email = "newuser@test.com";

        // 1. REGISTRAZIONE
        // Simula che l'utente non esista
        when(userRepository.findByUsername(username)).thenReturn(null);
        when(userRepository.findByEmail(email)).thenReturn(null);

        // Simula generazione token
        when(webClient.post().uri(anyString()).bodyValue(any()).retrieve().bodyToMono(Map.class).block())
                .thenReturn(Map.of("token", "registrationToken"));

        AuthResult registerResult = authService.register(username, "Test", password, "User", email);

        // Verifica registrazione
        assertEquals(StatusAuth.SUCCESS, registerResult.getStatusAuth());
        assertEquals("registrationToken", registerResult.getAccessToken());
        verify(userRepository, times(1)).save(any(User.class));

        // 2. LOGIN
        // Simula utente registrato
        User registeredUser = new User(username, "Test", BCrypt.hashpw(password, BCrypt.gensalt()), "User", email);
        when(userRepository.findByUsername(username)).thenReturn(registeredUser);

        // Simula generazione access token
        when(webClient.post().uri(anyString()).bodyValue(any()).retrieve().bodyToMono(Map.class).block())
                .thenReturn(Map.of("token", "accessToken123"));

        AuthResult loginResult = authService.login(username, password);

        // Verifica login
        assertEquals(StatusAuth.SUCCESS, loginResult.getStatusAuth());
        assertEquals("accessToken123", loginResult.getAccessToken());
        assertNotNull(loginResult.getRefreshToken());
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));

        // 3. REFRESH TOKEN
        String refreshTokenValue = "refreshToken123";
        RefreshToken refreshToken = new RefreshToken(refreshTokenValue, username, LocalDateTime.now().plusDays(30));

        when(refreshTokenRepository.findByToken(refreshTokenValue)).thenReturn(Optional.of(refreshToken));
        when(webClient.post().uri(anyString()).bodyValue(any()).retrieve().bodyToMono(Map.class).block())
                .thenReturn(Map.of("token", "newAccessToken"));

        AuthResult refreshResult = authService.refreshAccessToken(refreshTokenValue);

        // Verifica refresh (rotation)
        assertEquals(StatusAuth.SUCCESS, refreshResult.getStatusAuth());
        assertEquals("newAccessToken", refreshResult.getAccessToken());
        assertNotNull(refreshResult.getRefreshToken());
        verify(refreshTokenRepository, times(1)).delete(refreshToken);
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class)); // 1 dal login + 1 dal refresh

        // 4. AGGIORNAMENTO PROFILO
        when(webClient.post().uri(anyString()).bodyValue(any()).retrieve().bodyToMono(Map.class).block())
                .thenReturn(Map.of("username", username));
        when(userRepository.findByUsername(username)).thenReturn(registeredUser);

        User updatedData = new User();
        updatedData.setName("UpdatedName");
        updatedData.setSurname("UpdatedSurname");

        AuthResult updateResult = authService.updateUserProfile("validToken", updatedData);

        // Verifica aggiornamento
        assertEquals(StatusAuth.SUCCESS, updateResult.getStatusAuth());
        assertEquals("UpdatedName", registeredUser.getName());
        assertEquals("UpdatedSurname", registeredUser.getSurname());
        verify(userRepository, times(1)).save(registeredUser);
    }

    /**
     * Test del flusso di errore: tentativo di registrazione duplicata
     */
    @Test
    @DisplayName("Flusso errore: tentativo di registrazione con username già esistente")
    void testDuplicateRegistrationFlow() {
        String username = "existinguser";
        String email = "existing@test.com";

        // Simula utente già esistente
        User existingUser = new User(username, "Test", "hashedpass", "User", email);
        when(userRepository.findByUsername(username)).thenReturn(existingUser);

        // Tentativo di registrazione
        AuthResult result = authService.register(username, "Test", "password", "User", email);

        // Verifica che la registrazione fallisca
        assertEquals(StatusAuth.USER_ALREADY_EXISTS, result.getStatusAuth());
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * Test del flusso di errore: login con credenziali errate
     */
    @Test
    @DisplayName("Flusso errore: login con password errata")
    void testInvalidPasswordLoginFlow() {
        String username = "testuser";
        String correctPassword = "correctpassword";
        String wrongPassword = "wrongpassword";

        // Simula utente esistente
        User user = new User(username, "Test", BCrypt.hashpw(correctPassword, BCrypt.gensalt()), "User", "test@test.com");
        when(userRepository.findByUsername(username)).thenReturn(user);

        // Tentativo di login con password errata
        AuthResult result = authService.login(username, wrongPassword);

        // Verifica che il login fallisca
        assertEquals(StatusAuth.INVALID_CREDENTIALS, result.getStatusAuth());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    /**
     * Test del flusso di sicurezza: token rotation con refresh token scaduto
     */
    @Test
    @DisplayName("Flusso sicurezza: refresh con token scaduto")
    void testExpiredRefreshTokenFlow() {
        String expiredTokenValue = "expiredToken123";
        String username = "testuser";

        // Simula token scaduto
        RefreshToken expiredToken = new RefreshToken(expiredTokenValue, username, LocalDateTime.now().minusDays(1));
        when(refreshTokenRepository.findByToken(expiredTokenValue)).thenReturn(Optional.of(expiredToken));

        // Tentativo di refresh
        AuthResult result = authService.refreshAccessToken(expiredTokenValue);

        // Verifica che il refresh fallisca
        assertEquals(StatusAuth.TOKEN_EXPIRED, result.getStatusAuth());
        // Verifica che il token scaduto sia stato eliminato
        verify(refreshTokenRepository, times(1)).delete(expiredToken);
        // Verifica che non sia stato creato un nuovo token
        verify(refreshTokenRepository, times(1)).delete(any(RefreshToken.class));
    }

    /**
     * Test del flusso di sicurezza: token rotation (il vecchio non può essere riusato)
     */
    @Test
    @DisplayName("Flusso sicurezza: token rotation previene riuso del token")
    void testTokenRotationPreventsReuse() {
        String oldTokenValue = "oldToken123";
        String username = "testuser";

        // Prima richiesta di refresh
        RefreshToken oldToken = new RefreshToken(oldTokenValue, username, LocalDateTime.now().plusDays(30));
        when(refreshTokenRepository.findByToken(oldTokenValue)).thenReturn(Optional.of(oldToken));
        when(webClient.post().uri(anyString()).bodyValue(any()).retrieve().bodyToMono(Map.class).block())
                .thenReturn(Map.of("token", "newAccessToken"));

        AuthResult firstRefresh = authService.refreshAccessToken(oldTokenValue);

        // Verifica che il primo refresh abbia successo
        assertEquals(StatusAuth.SUCCESS, firstRefresh.getStatusAuth());
        verify(refreshTokenRepository, times(1)).delete(oldToken);
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));

        // Secondo tentativo di riuso dello stesso token
        when(refreshTokenRepository.findByToken(oldTokenValue)).thenReturn(Optional.empty());

        AuthResult secondRefresh = authService.refreshAccessToken(oldTokenValue);

        // Verifica che il secondo tentativo fallisca (token già usato e eliminato)
        assertEquals(StatusAuth.TOKEN_INVALID, secondRefresh.getStatusAuth());
    }

    /**
     * Test del flusso: login con email invece di username
     */
    @Test
    @DisplayName("Flusso: login con email invece di username")
    void testLoginWithEmailFlow() {
        String email = "user@test.com";
        String password = "password123";

        // Simula utente esistente
        User user = new User("testuser", "Test", BCrypt.hashpw(password, BCrypt.gensalt()), "User", email);
        when(userRepository.findByUsername(email)).thenReturn(null);
        when(userRepository.findByEmail(email)).thenReturn(user);

        when(webClient.post().uri(anyString()).bodyValue(any()).retrieve().bodyToMono(Map.class).block())
                .thenReturn(Map.of("token", "accessToken123"));

        // Login con email
        AuthResult result = authService.login(email, password);

        // Verifica che il login abbia successo
        assertEquals(StatusAuth.SUCCESS, result.getStatusAuth());
        assertEquals("accessToken123", result.getAccessToken());
        verify(userRepository, times(1)).findByUsername(email);
        verify(userRepository, times(1)).findByEmail(email);
    }

    /**
     * Test del flusso: getProfile con token valido
     */
    @Test
    @DisplayName("Flusso: recupero profilo con token valido")
    void testGetProfileFlow() {
        String accessToken = "validToken";
        String username = "testuser";

        // Simula verifica token
        when(webClient.post().uri(anyString()).bodyValue(any()).retrieve().bodyToMono(Map.class).block())
                .thenReturn(Map.of("username", username));

        // Simula utente esistente
        User user = new User(username, "Test", "hashedpass", "User", "test@test.com");
        when(userRepository.findByUsername(username)).thenReturn(user);

        // Recupera profilo
        User profile = authService.getUserFromToken(accessToken);

        // Verifica
        assertNotNull(profile);
        assertEquals(username, profile.getUsername());
        assertEquals("Test", profile.getName());
        assertEquals("User", profile.getSurname());
        assertEquals("test@test.com", profile.getEmail());
    }

    /**
     * Test del flusso: aggiornamento profilo con token invalido
     */
    @Test
    @DisplayName("Flusso errore: aggiornamento profilo con token invalido")
    void testUpdateProfileWithInvalidTokenFlow() {
        String invalidToken = "invalidToken";

        // Simula verifica token fallita
        Map<String, String> responseMap = new java.util.HashMap<>();
        responseMap.put("username", null);
        when(webClient.post().uri(anyString()).bodyValue(any()).retrieve().bodyToMono(Map.class).block())
                .thenReturn(responseMap);

        User updatedData = new User();
        updatedData.setName("NewName");

        // Tentativo di aggiornamento
        AuthResult result = authService.updateUserProfile(invalidToken, updatedData);

        // Verifica che l'aggiornamento fallisca
        assertEquals(StatusAuth.TOKEN_INVALID, result.getStatusAuth());
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * Test del flusso: multiple sessioni per lo stesso utente
     */
    @Test
    @DisplayName("Flusso: gestione multiple sessioni (multiple refresh token)")
    void testMultipleSessionsFlow() {
        String username = "testuser";
        String password = "password123";

        // Simula utente esistente
        User user = new User(username, "Test", BCrypt.hashpw(password, BCrypt.gensalt()), "User", "test@test.com");
        when(userRepository.findByUsername(username)).thenReturn(user);

        when(webClient.post().uri(anyString()).bodyValue(any()).retrieve().bodyToMono(Map.class).block())
                .thenReturn(Map.of("token", "accessToken1"))
                .thenReturn(Map.of("token", "accessToken2"));

        // Primo login (dispositivo 1)
        AuthResult login1 = authService.login(username, password);
        assertEquals(StatusAuth.SUCCESS, login1.getStatusAuth());

        // Secondo login (dispositivo 2)
        AuthResult login2 = authService.login(username, password);
        assertEquals(StatusAuth.SUCCESS, login2.getStatusAuth());

        // Verifica che siano stati creati due refresh token
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }
}

