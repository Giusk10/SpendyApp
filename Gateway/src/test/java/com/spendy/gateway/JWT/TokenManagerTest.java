package com.spendy.gateway.JWT;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class TokenManagerTest {

    private TokenManager tokenManager;
    private final String SECRET_KEY = "testSecretKeyForJWTTokenGenerationAndVerification12345";
    private final long EXPIRATION_TIME = 3600000;

    @BeforeEach
    void setUp() {
        tokenManager = new TokenManager();
        ReflectionTestUtils.setField(tokenManager, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(tokenManager, "EXPIRATION_TIME", EXPIRATION_TIME);
    }

    @Test
    void testGenerateToken_Success() {
        String username = "testuser";
        String token = tokenManager.generateToken(username);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        Claims claims = Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(token)
                .getBody();

        assertEquals(username, claims.getSubject());
    }

    @Test
    void testVerifyToken_ValidToken() {
        String username = "testuser";
        String token = tokenManager.generateToken(username);
        String result = tokenManager.verifyToken(token);

        assertNotNull(result);
        assertEquals(username, result);
    }

    @Test
    void testVerifyToken_InvalidToken() {
        String invalidToken = "invalid.token.here";
        String result = tokenManager.verifyToken(invalidToken);

        assertNull(result);
    }

    @Test
    void testVerifyToken_ExpiredToken() {
        TokenManager shortExpirationTokenManager = new TokenManager();
        ReflectionTestUtils.setField(shortExpirationTokenManager, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(shortExpirationTokenManager, "EXPIRATION_TIME", -1000L);

        String username = "testuser";
        String expiredToken = shortExpirationTokenManager.generateToken(username);
        String result = tokenManager.verifyToken(expiredToken);

        assertNull(result);
    }

    @Test
    void testGenerateToken_WithDifferentUsernames() {
        String username1 = "user1";
        String username2 = "user2";

        String token1 = tokenManager.generateToken(username1);
        String token2 = tokenManager.generateToken(username2);

        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
    }

    @Test
    void testTokenExpiration_IsSet() {
        String username = "testuser";
        String token = tokenManager.generateToken(username);

        Claims claims = Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(token)
                .getBody();

        assertNotNull(claims.getExpiration());
        assertTrue(claims.getExpiration().getTime() > System.currentTimeMillis());
    }

    @Test
    void testVerifyToken_NullToken() {
        String result = tokenManager.verifyToken(null);
        assertNull(result);
    }

    @Test
    void testVerifyToken_EmptyToken() {
        String result = tokenManager.verifyToken("");
        assertNull(result);
    }

    @Test
    void testVerifyToken_MalformedToken() {
        String malformedToken = "not.a.valid.jwt.token.at.all";
        String result = tokenManager.verifyToken(malformedToken);
        assertNull(result);
    }

    @Test
    void testGenerateToken_SpecialCharactersInUsername() {
        String username = "test@user.com";
        String token = tokenManager.generateToken(username);

        assertNotNull(token);
        String result = tokenManager.verifyToken(token);
        assertEquals(username, result);
    }

    @Test
    void testGenerateToken_LongUsername() {
        String username = "a".repeat(100);
        String token = tokenManager.generateToken(username);

        assertNotNull(token);
        String result = tokenManager.verifyToken(token);
        assertEquals(username, result);
    }

    @Test
    void testVerifyToken_TokenWithWrongSignature() {
        String username = "testuser";
        String token = tokenManager.generateToken(username);

        // Modifica l'ultimo carattere del token per invalidare la firma
        String tamperedToken = token.substring(0, token.length() - 1) + "X";
        String result = tokenManager.verifyToken(tamperedToken);

        assertNull(result);
    }
}


