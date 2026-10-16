package com.spendyteam.expense.Service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UserAuthServiceTest {

    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;
    private UserAuthService userAuthService;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        privateKey = (RSAPrivateKey) kp.getPrivate();
        publicKey = (RSAPublicKey) kp.getPublic();

        userAuthService = new UserAuthService(publicKey, "spendy-auth", "spendy-app");
    }

    private String createToken(RSAPrivateKey key, String issuer, String audience, long expiryOffsetMs) throws Exception {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiryOffsetMs);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("john_doe")
                .issuer(issuer)
                .audience(audience)
                .issueTime(now)
                .expirationTime(expiry)
                .jwtID(UUID.randomUUID().toString())
                .claim("roles", List.of("ROLE_USER"))
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("spendy-auth-key-1").build(),
                claimsSet
        );

        JWSSigner signer = new RSASSASigner(key);
        signedJWT.sign(signer);

        return signedJWT.serialize();
    }

    @Test
    void testGetUsernameFromTokenValid() throws Exception {
        String token = createToken(privateKey, "spendy-auth", "spendy-app", 60000);
        String username = userAuthService.getUsernameFromToken(token);

        assertEquals("john_doe", username);
    }

    @Test
    void testGetUsernameFromTokenInvalidOrExpired() throws Exception {
        String token = createToken(privateKey, "spendy-auth", "spendy-app", -5000);
        String username = userAuthService.getUsernameFromToken(token);

        assertNull(username);
    }

    @Test
    void testGetUsernameFromTokenTamperedSignature() throws Exception {
        String token = createToken(privateKey, "spendy-auth", "spendy-app", 60000);
        int lastDot = token.lastIndexOf('.');
        int targetPos = lastDot + 10;
        char orig = token.charAt(targetPos);
        char replaced = (orig == 'A') ? 'B' : 'A';
        String tampered = token.substring(0, targetPos) + replaced + token.substring(targetPos + 1);

        String username = userAuthService.getUsernameFromToken(tampered);
        assertNull(username);
    }

    @Test
    void testGetUsernameFromTokenWrongKey() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp2 = kpg.generateKeyPair();
        String token = createToken((RSAPrivateKey) kp2.getPrivate(), "spendy-auth", "spendy-app", 60000);

        String username = userAuthService.getUsernameFromToken(token);
        assertNull(username);
    }

    @Test
    void testGetUsernameFromTokenWrongIssuer() throws Exception {
        String token = createToken(privateKey, "wrong-issuer", "spendy-app", 60000);
        String username = userAuthService.getUsernameFromToken(token);

        assertNull(username);
    }

    @Test
    void testGetUsernameFromTokenWrongAudience() throws Exception {
        String token = createToken(privateKey, "spendy-auth", "wrong-audience", 60000);
        String username = userAuthService.getUsernameFromToken(token);

        assertNull(username);
    }

    @Test
    void testGetUsernameFromTokenNullOrEmpty() {
        assertNull(userAuthService.getUsernameFromToken(null));
        assertNull(userAuthService.getUsernameFromToken(""));
        assertNull(userAuthService.getUsernameFromToken("   "));
        assertNull(userAuthService.getUsernameFromToken("invalid.jwt.token"));
    }

    @Test
    void testGetUsernameFromTokenViaRestAlias() throws Exception {
        String token = createToken(privateKey, "spendy-auth", "spendy-app", 60000);
        String username = userAuthService.getUsernameFromTokenViaRest(token);

        assertEquals("john_doe", username);
    }

    @Test
    void testInitAndLoadPublicKeyFromClasspath() {
        UserAuthService service = new UserAuthService();
        service.setAllowEphemeralKeys(true);
        service.init();
        assertNotNull(service);
    }

    @Test
    void testInitAndLoadPublicKeyFromPemSecret() throws Exception {
        String pubPem = "-----BEGIN PUBLIC KEY-----\n" +
                java.util.Base64.getEncoder().encodeToString(publicKey.getEncoded()) +
                "\n-----END PUBLIC KEY-----";

        UserAuthService service = new UserAuthService();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "publicKeyPem", pubPem);
        service.init();

        String token = createToken(privateKey, "spendy-auth", "spendy-app", 60000);
        assertEquals("john_doe", service.getUsernameFromToken(token));
    }
}
