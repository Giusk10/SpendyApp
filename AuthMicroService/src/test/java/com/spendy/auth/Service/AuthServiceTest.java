package com.spendy.auth.Service;

import com.spendy.auth.Data.RefreshToken;
import com.spendy.auth.Data.User;
import com.spendy.auth.Repository.IRefreshTokenRepository;
import com.spendy.auth.Repository.IUserRepository;
import com.spendy.auth.Security.JwtTokenService;
import com.spendy.auth.Utility.AuthResult;
import com.spendy.auth.Utility.StatusAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private IUserRepository userRepository;

    @Mock
    private IRefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenService jwtTokenService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, refreshTokenRepository, jwtTokenService, passwordEncoder);
    }

    @Test
    void register_UserAlreadyExists_ReturnsUserAlreadyExists() {
        when(userRepository.findByUsername("user")).thenReturn(new User());

        AuthResult result = authService.register("user", "nome", "pass", "cognome", "email@test.com");

        assertEquals(StatusAuth.USER_ALREADY_EXISTS, result.getStatusAuth());
    }

    @Test
    void register_NewUser_ReturnsSuccess() {
        when(userRepository.findByUsername("user")).thenReturn(null);
        when(jwtTokenService.generateToken("user")).thenReturn("testtoken");

        AuthResult result = authService.register("user", "nome", "pass", "cognome", "email@test.com");

        assertEquals(StatusAuth.SUCCESS, result.getStatusAuth());
        assertEquals("testtoken", result.getAccessToken());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void login_UserNotFound_ReturnsUserNotFound() {
        when(userRepository.findByUsername("user")).thenReturn(null);
        when(userRepository.findByEmail("user")).thenReturn(null);

        AuthResult result = authService.login("user", "pass");

        assertEquals(StatusAuth.USER_NOT_FOUND, result.getStatusAuth());
    }

    @Test
    void login_InvalidPassword_ReturnsInvalidCredentials() {
        User user = new User("user", "nome", passwordEncoder.encode("rightpass"), "cognome", "email@test.com");
        when(userRepository.findByUsername("user")).thenReturn(user);

        AuthResult result = authService.login("user", "wrongpass");

        assertEquals(StatusAuth.INVALID_CREDENTIALS, result.getStatusAuth());
    }

    @Test
    void login_ValidCredentials_ReturnsSuccessWithTokens() {
        String password = "correctpass";
        User user = new User("user", "nome", passwordEncoder.encode(password), "cognome", "email@test.com");

        when(userRepository.findByUsername("user")).thenReturn(user);
        when(jwtTokenService.generateToken("user")).thenReturn("accessToken123");

        AuthResult result = authService.login("user", password);

        assertEquals(StatusAuth.SUCCESS, result.getStatusAuth());
        assertEquals("accessToken123", result.getAccessToken());
        assertNotNull(result.getRefreshToken());
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    void refreshAccessToken_ValidToken_ReturnsNewTokens() {
        String oldRefreshToken = "oldRefreshToken123";
        String username = "testuser";

        RefreshToken refreshToken = new RefreshToken(oldRefreshToken, username, LocalDateTime.now().plusDays(30));

        when(refreshTokenRepository.findByToken(oldRefreshToken)).thenReturn(Optional.of(refreshToken));
        when(jwtTokenService.generateToken(username)).thenReturn("newAccessToken123");

        AuthResult result = authService.refreshAccessToken(oldRefreshToken);

        assertEquals(StatusAuth.SUCCESS, result.getStatusAuth());
        assertEquals("newAccessToken123", result.getAccessToken());
        assertNotNull(result.getRefreshToken());
        // Verify current token marked revoked/rotated and new token saved
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void refreshAccessToken_TokenNotFound_ReturnsTokenInvalid() {
        String invalidToken = "invalidToken";

        when(refreshTokenRepository.findByToken(invalidToken)).thenReturn(Optional.empty());

        AuthResult result = authService.refreshAccessToken(invalidToken);

        assertEquals(StatusAuth.TOKEN_INVALID, result.getStatusAuth());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void refreshAccessToken_ExpiredToken_ReturnsTokenExpired() {
        String expiredToken = "expiredToken123";
        String username = "testuser";

        RefreshToken refreshToken = new RefreshToken(expiredToken, username, LocalDateTime.now().minusDays(1));

        when(refreshTokenRepository.findByToken(expiredToken)).thenReturn(Optional.of(refreshToken));

        AuthResult result = authService.refreshAccessToken(expiredToken);

        assertEquals(StatusAuth.TOKEN_EXPIRED, result.getStatusAuth());
        verify(refreshTokenRepository, times(1)).save(refreshToken);
    }

    @Test
    void getUserFromToken_ValidToken_ReturnsUser() {
        String validToken = "validAccessToken";
        String username = "testuser";
        User expectedUser = new User(username, "Test", "password", "User", "test@test.com");

        when(jwtTokenService.verifyTokenAndGetSubject(validToken)).thenReturn(username);
        when(userRepository.findByUsername(username)).thenReturn(expectedUser);

        User result = authService.getUserFromToken(validToken);

        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals("Test", result.getName());
    }

    @Test
    void getUserFromToken_InvalidToken_ReturnsNull() {
        String invalidToken = "invalidAccessToken";

        when(jwtTokenService.verifyTokenAndGetSubject(invalidToken)).thenReturn(null);

        User result = authService.getUserFromToken(invalidToken);

        assertNull(result);
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    void updateUserProfile_ValidToken_UpdatesUser() {
        String validToken = "validAccessToken";
        String username = "testuser";
        User existingUser = new User(username, "OldName", "password", "OldSurname", "test@test.com");
        User updatedUser = new User();
        updatedUser.setName("NewName");
        updatedUser.setSurname("NewSurname");

        when(jwtTokenService.verifyTokenAndGetSubject(validToken)).thenReturn(username);
        when(userRepository.findByUsername(username)).thenReturn(existingUser);

        AuthResult result = authService.updateUserProfile(validToken, updatedUser);

        assertEquals(StatusAuth.SUCCESS, result.getStatusAuth());
        assertEquals("NewName", existingUser.getName());
        assertEquals("NewSurname", existingUser.getSurname());
        verify(userRepository, times(1)).save(existingUser);
    }

    @Test
    void updateUserProfile_InvalidToken_ReturnsTokenInvalid() {
        String invalidToken = "invalidAccessToken";
        User updatedUser = new User();
        updatedUser.setName("NewName");

        when(jwtTokenService.verifyTokenAndGetSubject(invalidToken)).thenReturn(null);

        AuthResult result = authService.updateUserProfile(invalidToken, updatedUser);

        assertEquals(StatusAuth.TOKEN_INVALID, result.getStatusAuth());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserProfile_UserNotFound_ReturnsUserNotFound() {
        String validToken = "validAccessToken";
        String username = "nonexistentuser";
        User updatedUser = new User();
        updatedUser.setName("NewName");

        when(jwtTokenService.verifyTokenAndGetSubject(validToken)).thenReturn(username);
        when(userRepository.findByUsername(username)).thenReturn(null);

        AuthResult result = authService.updateUserProfile(validToken, updatedUser);

        assertEquals(StatusAuth.USER_NOT_FOUND, result.getStatusAuth());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_EmailAlreadyExists_ReturnsUserAlreadyExists() {
        when(userRepository.findByUsername("newuser")).thenReturn(null);
        when(userRepository.findByEmail("existing@test.com")).thenReturn(new User());

        AuthResult result = authService.register("newuser", "nome", "pass", "cognome", "existing@test.com");

        assertEquals(StatusAuth.USER_ALREADY_EXISTS, result.getStatusAuth());
        verify(userRepository, never()).save(any(User.class));
    }
}
