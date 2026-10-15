package com.spendy.auth.Integration;

import com.spendy.auth.Data.RefreshToken;
import com.spendy.auth.Data.User;
import com.spendy.auth.Repository.IRefreshTokenRepository;
import com.spendy.auth.Repository.IUserRepository;
import com.spendy.auth.Security.JwtTokenService;
import com.spendy.auth.Service.AuthService;
import com.spendy.auth.Utility.AuthResult;
import com.spendy.auth.Utility.StatusAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

    private JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private AuthService authService;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        jwtTokenService = new JwtTokenService((RSAPrivateKey) kp.getPrivate(), (RSAPublicKey) kp.getPublic(), "spendy-auth", "spendy-app", 60000);

        authService = new AuthService(userRepository, refreshTokenRepository, jwtTokenService, passwordEncoder);
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
        when(userRepository.findByUsername(username)).thenReturn(null);
        when(userRepository.findByEmail(email)).thenReturn(null);

        AuthResult registerResult = authService.register(username, "Test", password, "User", email);

        assertEquals(StatusAuth.SUCCESS, registerResult.getStatusAuth());
        assertNotNull(registerResult.getAccessToken());
        verify(userRepository, times(1)).save(any(User.class));

        // 2. LOGIN
        User registeredUser = new User(username, "Test", passwordEncoder.encode(password), "User", email);
        when(userRepository.findByUsername(username)).thenReturn(registeredUser);

        AuthResult loginResult = authService.login(username, password);

        assertEquals(StatusAuth.SUCCESS, loginResult.getStatusAuth());
        assertNotNull(loginResult.getAccessToken());
        assertNotNull(loginResult.getRefreshToken());
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));

        // 3. REFRESH TOKEN
        String refreshTokenValue = "refreshToken123";
        RefreshToken refreshToken = new RefreshToken(refreshTokenValue, username, LocalDateTime.now().plusDays(30));

        when(refreshTokenRepository.findByToken(refreshTokenValue)).thenReturn(Optional.of(refreshToken));

        AuthResult refreshResult = authService.refreshAccessToken(refreshTokenValue);

        assertEquals(StatusAuth.SUCCESS, refreshResult.getStatusAuth());
        assertNotNull(refreshResult.getAccessToken());
        assertNotNull(refreshResult.getRefreshToken());
        verify(refreshTokenRepository, times(1)).delete(refreshToken);
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));

        // 4. AGGIORNAMENTO PROFILO
        String validToken = loginResult.getAccessToken();
        when(userRepository.findByUsername(username)).thenReturn(registeredUser);

        User updatedData = new User();
        updatedData.setName("UpdatedName");
        updatedData.setSurname("UpdatedSurname");

        AuthResult updateResult = authService.updateUserProfile(validToken, updatedData);

        assertEquals(StatusAuth.SUCCESS, updateResult.getStatusAuth());
        assertEquals("UpdatedName", registeredUser.getName());
        assertEquals("UpdatedSurname", registeredUser.getSurname());
        verify(userRepository, times(1)).save(registeredUser);
    }

    @Test
    @DisplayName("Flusso errore: tentativo di registrazione con username già esistente")
    void testDuplicateRegistrationFlow() {
        String username = "existinguser";
        String email = "existing@test.com";

        User existingUser = new User(username, "Test", "hashedpass", "User", email);
        when(userRepository.findByUsername(username)).thenReturn(existingUser);

        AuthResult result = authService.register(username, "Test", "password", "User", email);

        assertEquals(StatusAuth.USER_ALREADY_EXISTS, result.getStatusAuth());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Flusso errore: login con password errata")
    void testInvalidPasswordLoginFlow() {
        String username = "testuser";
        String correctPassword = "correctpassword";
        String wrongPassword = "wrongpassword";

        User user = new User(username, "Test", passwordEncoder.encode(correctPassword), "User", "test@test.com");
        when(userRepository.findByUsername(username)).thenReturn(user);

        AuthResult result = authService.login(username, wrongPassword);

        assertEquals(StatusAuth.INVALID_CREDENTIALS, result.getStatusAuth());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Flusso sicurezza: refresh con token scaduto")
    void testExpiredRefreshTokenFlow() {
        String expiredTokenValue = "expiredToken123";
        String username = "testuser";

        RefreshToken expiredToken = new RefreshToken(expiredTokenValue, username, LocalDateTime.now().minusDays(1));
        when(refreshTokenRepository.findByToken(expiredTokenValue)).thenReturn(Optional.of(expiredToken));

        AuthResult result = authService.refreshAccessToken(expiredTokenValue);

        assertEquals(StatusAuth.TOKEN_EXPIRED, result.getStatusAuth());
        verify(refreshTokenRepository, times(1)).delete(expiredToken);
    }

    @Test
    @DisplayName("Flusso sicurezza: token rotation previene riuso del token")
    void testTokenRotationPreventsReuse() {
        String oldTokenValue = "oldToken123";
        String username = "testuser";

        RefreshToken oldToken = new RefreshToken(oldTokenValue, username, LocalDateTime.now().plusDays(30));
        when(refreshTokenRepository.findByToken(oldTokenValue)).thenReturn(Optional.of(oldToken));

        AuthResult firstRefresh = authService.refreshAccessToken(oldTokenValue);

        assertEquals(StatusAuth.SUCCESS, firstRefresh.getStatusAuth());
        verify(refreshTokenRepository, times(1)).delete(oldToken);
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));

        // Secondo tentativo di riuso dello stesso token
        when(refreshTokenRepository.findByToken(oldTokenValue)).thenReturn(Optional.empty());

        AuthResult secondRefresh = authService.refreshAccessToken(oldTokenValue);

        assertEquals(StatusAuth.TOKEN_INVALID, secondRefresh.getStatusAuth());
    }

    @Test
    @DisplayName("Flusso: login con email invece di username")
    void testLoginWithEmailFlow() {
        String email = "user@test.com";
        String password = "password123";

        User user = new User("testuser", "Test", passwordEncoder.encode(password), "User", email);
        when(userRepository.findByUsername(email)).thenReturn(null);
        when(userRepository.findByEmail(email)).thenReturn(user);

        AuthResult result = authService.login(email, password);

        assertEquals(StatusAuth.SUCCESS, result.getStatusAuth());
        assertNotNull(result.getAccessToken());
        verify(userRepository, times(1)).findByUsername(email);
        verify(userRepository, times(1)).findByEmail(email);
    }

    @Test
    @DisplayName("Flusso: recupero profilo con token valido")
    void testGetProfileFlow() {
        String username = "testuser";
        String accessToken = jwtTokenService.generateToken(username);

        User user = new User(username, "Test", "hashedpass", "User", "test@test.com");
        when(userRepository.findByUsername(username)).thenReturn(user);

        User profile = authService.getUserFromToken(accessToken);

        assertNotNull(profile);
        assertEquals(username, profile.getUsername());
        assertEquals("Test", profile.getName());
        assertEquals("User", profile.getSurname());
        assertEquals("test@test.com", profile.getEmail());
    }

    @Test
    @DisplayName("Flusso errore: aggiornamento profilo con token invalido")
    void testUpdateProfileWithInvalidTokenFlow() {
        String invalidToken = "invalidToken";

        User updatedData = new User();
        updatedData.setName("NewName");

        AuthResult result = authService.updateUserProfile(invalidToken, updatedData);

        assertEquals(StatusAuth.TOKEN_INVALID, result.getStatusAuth());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Flusso: gestione multiple sessioni (multiple refresh token)")
    void testMultipleSessionsFlow() {
        String username = "testuser";
        String password = "password123";

        User user = new User(username, "Test", passwordEncoder.encode(password), "User", "test@test.com");
        when(userRepository.findByUsername(username)).thenReturn(user);

        AuthResult login1 = authService.login(username, password);
        assertEquals(StatusAuth.SUCCESS, login1.getStatusAuth());

        AuthResult login2 = authService.login(username, password);
        assertEquals(StatusAuth.SUCCESS, login2.getStatusAuth());

        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }
}
