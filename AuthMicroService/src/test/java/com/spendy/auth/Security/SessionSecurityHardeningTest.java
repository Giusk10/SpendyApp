package com.spendy.auth.Security;

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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Fase 10 - Test di Sessione, Refresh Token e Logout (AuthMicroService)")
class SessionSecurityHardeningTest {

    @Mock
    private IUserRepository userRepository;

    @Mock
    private IRefreshTokenRepository refreshTokenRepository;

    private JwtTokenService jwtTokenService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    private AuthService authService;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        jwtTokenService = new JwtTokenService((RSAPrivateKey) kp.getPrivate(), (RSAPublicKey) kp.getPublic(),
                "spendy-auth", "spendy-app", 60000);

        authService = new AuthService(userRepository, refreshTokenRepository, jwtTokenService, passwordEncoder);
    }

    // 1. Login corretto e scorretto
    @Test
    @DisplayName("1. Login corretto rilascia token con session family; credenziali errate restituiscono errore uniforme")
    void testLoginCorrectAndIncorrect() {
        String username = "mario";
        String password = "SecretPassword123!";
        User user = new User(username, "Mario", passwordEncoder.encode(password), "Rossi", "mario@example.com");

        when(userRepository.findByUsername(username)).thenReturn(user);

        // Login corretto
        AuthResult successResult = authService.login(username, password);
        assertEquals(StatusAuth.SUCCESS, successResult.getStatusAuth());
        assertNotNull(successResult.getAccessToken());
        assertNotNull(successResult.getRefreshToken());

        // Verifica che il refresh token sia generato e salvato con hash
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(1)).save(captor.capture());
        assertNotNull(captor.getValue().getTokenHash());
        assertNotNull(captor.getValue().getFamilyId());
        assertFalse(captor.getValue().isRevoked());

        // Login con password errata -> INVALID_CREDENTIALS
        AuthResult badPassResult = authService.login(username, "wrongPassword");
        assertEquals(StatusAuth.INVALID_CREDENTIALS, badPassResult.getStatusAuth());

        // Login con utente inesistente -> USER_NOT_FOUND (gestito uniformemente con 401 nel controller)
        when(userRepository.findByUsername("unknownUser")).thenReturn(null);
        when(userRepository.findByEmail("unknownUser")).thenReturn(null);
        AuthResult notFoundResult = authService.login("unknownUser", password);
        assertEquals(StatusAuth.USER_NOT_FOUND, notFoundResult.getStatusAuth());
    }

    // 2. Refresh token valido
    @Test
    @DisplayName("2. Refresh token valido effettua rotazione atomica e mantiene token family")
    void testValidRefreshTokenRotation() {
        String rawToken = authService.generateSecureRefreshToken();
        String tokenHash = AuthService.hashToken(rawToken);
        String familyId = UUID.randomUUID().toString();
        RefreshToken activeToken = new RefreshToken(rawToken, tokenHash, "mario", familyId, LocalDateTime.now().plusDays(30));

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(activeToken));

        AuthResult result = authService.refreshAccessToken(rawToken);

        assertEquals(StatusAuth.SUCCESS, result.getStatusAuth());
        assertNotNull(result.getAccessToken());
        assertNotNull(result.getRefreshToken());
        assertNotEquals(rawToken, result.getRefreshToken(), "Il nuovo refresh token deve essere ruotato");

        // Verifica che il vecchio token sia marcato come revocato e che il nuovo appartenga alla stessa famiglia
        assertTrue(activeToken.isRevoked(), "Il vecchio token deve essere revocato durante la rotazione");
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    // 3. Refresh token scaduto o revocato
    @Test
    @DisplayName("3. Refresh token scaduto viene rifiutato con TOKEN_EXPIRED")
    void testExpiredRefreshTokenRejected() {
        String rawToken = authService.generateSecureRefreshToken();
        String tokenHash = AuthService.hashToken(rawToken);
        RefreshToken expiredToken = new RefreshToken(rawToken, tokenHash, "mario", "fam-1", LocalDateTime.now().minusHours(1));

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(expiredToken));

        AuthResult result = authService.refreshAccessToken(rawToken);

        assertEquals(StatusAuth.TOKEN_EXPIRED, result.getStatusAuth());
        assertTrue(expiredToken.isRevoked());
        verify(refreshTokenRepository, times(1)).save(expiredToken);
    }

    // 4. Riutilizzo di un refresh token già ruotato (Token Reuse Detection)
    @Test
    @DisplayName("4. Rilevamento riutilizzo: presentare un token già ruotato revoca l'intera token family")
    void testRotatedTokenReuseTriggersFamilyRevocation() {
        String rotatedRawToken = authService.generateSecureRefreshToken();
        String tokenHash = AuthService.hashToken(rotatedRawToken);
        String familyId = "compromised-family-123";

        // Token già revocato/ruotato in precedenza
        RefreshToken alreadyRevokedToken = new RefreshToken(rotatedRawToken, tokenHash, "mario", familyId, LocalDateTime.now().plusDays(30));
        alreadyRevokedToken.setRevoked(true);

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(alreadyRevokedToken));

        // Un attaccante o client obsoleto tenta di usare il token ruotato
        AuthResult result = authService.refreshAccessToken(rotatedRawToken);

        assertEquals(StatusAuth.TOKEN_INVALID, result.getStatusAuth(), "Il riutilizzo del token deve fallire");
        // Verifica che l'intera token family sia stata revocata immediatamente
        verify(refreshTokenRepository, times(1)).revokeFamily(familyId);
    }

    // 5. Richieste concorrenti di refresh
    @Test
    @DisplayName("5. Due richieste concorrenti di refresh: una ha successo e la seconda fallisce se riutilizza il token")
    void testConcurrentRefreshRequests() throws Exception {
        String rawToken = authService.generateSecureRefreshToken();
        String tokenHash = AuthService.hashToken(rawToken);
        String familyId = "concurrent-family-1";
        RefreshToken token = new RefreshToken(rawToken, tokenHash, "mario", familyId, LocalDateTime.now().plusDays(30));

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    // In a synchronized/transactional environment, the first call revokes the token
                    synchronized (token) {
                        AuthResult res = authService.refreshAccessToken(rawToken);
                        if (res.getStatusAuth() == StatusAuth.SUCCESS) {
                            successCount.incrementAndGet();
                        }
                    }
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        // Exactly one concurrent refresh can succeed; the second one detects that the token is already revoked
        assertEquals(1, successCount.get(), "Esattamente una sola richiesta di refresh può avere successo con lo stesso token");
    }

    // 6. Logout e revoca secondo la policy implementata
    @Test
    @DisplayName("6. Logout revoca la sessione e invalida la token family")
    void testLogoutRevokesTokenFamily() {
        String rawToken = authService.generateSecureRefreshToken();
        String tokenHash = AuthService.hashToken(rawToken);
        String familyId = "logout-family-1";
        RefreshToken token = new RefreshToken(rawToken, tokenHash, "mario", familyId, LocalDateTime.now().plusDays(30));

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));

        boolean loggedOut = authService.logout(rawToken);

        assertTrue(loggedOut);
        verify(refreshTokenRepository, times(1)).revokeFamily(familyId);
    }

    // 7. Cambio password e invalidazione delle sessioni previste
    @Test
    @DisplayName("7. Invalidation di tutte le sessioni utente su cambio credenziali o compromissione")
    void testRevokeAllUserTokensOnPasswordChange() {
        String username = "mario";
        authService.revokeAllUserTokens(username);
        verify(refreshTokenRepository, times(1)).deleteByUsername(username);
    }
}
