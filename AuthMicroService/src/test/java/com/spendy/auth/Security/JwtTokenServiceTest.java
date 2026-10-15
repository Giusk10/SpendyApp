package com.spendy.auth.Security;

import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenServiceTest {

    private JwtTokenService jwtTokenService;
    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;

    @BeforeEach
    void setUp() throws Exception {
        // Generate a test RSA 2048 key pair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        privateKey = (RSAPrivateKey) kp.getPrivate();
        publicKey = (RSAPublicKey) kp.getPublic();

        jwtTokenService = new JwtTokenService(privateKey, publicKey, "spendy-auth", "spendy-app", 60000);
    }

    @Test
    void testGenerateToken_ClaimsAndSignature() throws Exception {
        String username = "testuser";
        String token = jwtTokenService.generateToken(username);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        SignedJWT signedJWT = SignedJWT.parse(token);
        assertEquals("RS256", signedJWT.getHeader().getAlgorithm().getName());
        assertEquals(JwtTokenService.KEY_ID, signedJWT.getHeader().getKeyID());

        assertEquals(username, signedJWT.getJWTClaimsSet().getSubject());
        assertEquals("spendy-auth", signedJWT.getJWTClaimsSet().getIssuer());
        assertTrue(signedJWT.getJWTClaimsSet().getAudience().contains("spendy-app"));
        assertNotNull(signedJWT.getJWTClaimsSet().getIssueTime());
        assertNotNull(signedJWT.getJWTClaimsSet().getExpirationTime());
        assertNotNull(signedJWT.getJWTClaimsSet().getJWTID());
        assertEquals(List.of("ROLE_USER"), signedJWT.getJWTClaimsSet().getStringListClaim("roles"));
    }

    @Test
    void testVerifyTokenAndGetSubject_Valid() {
        String token = jwtTokenService.generateToken("alice");
        String subject = jwtTokenService.verifyTokenAndGetSubject(token);
        assertEquals("alice", subject);
    }

    @Test
    void testVerifyTokenAndGetSubject_Expired() {
        JwtTokenService expiredService = new JwtTokenService(privateKey, publicKey, "spendy-auth", "spendy-app", -1000);
        String token = expiredService.generateToken("bob");

        String subject = jwtTokenService.verifyTokenAndGetSubject(token);
        assertNull(subject);
    }

    @Test
    void testVerifyTokenAndGetSubject_TamperedSignature() {
        String token = jwtTokenService.generateToken("charlie");
        int lastDot = token.lastIndexOf('.');
        int targetPos = lastDot + 10;
        char originalChar = token.charAt(targetPos);
        char replacedChar = (originalChar == 'A') ? 'B' : 'A';
        String tamperedToken = token.substring(0, targetPos) + replacedChar + token.substring(targetPos + 1);

        String subject = jwtTokenService.verifyTokenAndGetSubject(tamperedToken);
        assertNull(subject);
    }

    @Test
    void testVerifyTokenAndGetSubject_WrongKey() throws Exception {
        // Generate another key pair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp2 = kpg.generateKeyPair();
        JwtTokenService otherService = new JwtTokenService((RSAPrivateKey) kp2.getPrivate(), (RSAPublicKey) kp2.getPublic(), "spendy-auth", "spendy-app", 60000);

        String tokenFromOther = otherService.generateToken("david");

        // Verification with our key should fail
        String subject = jwtTokenService.verifyTokenAndGetSubject(tokenFromOther);
        assertNull(subject);
    }

    @Test
    void testVerifyTokenAndGetSubject_NullOrEmpty() {
        assertNull(jwtTokenService.verifyTokenAndGetSubject(null));
        assertNull(jwtTokenService.verifyTokenAndGetSubject(""));
        assertNull(jwtTokenService.verifyTokenAndGetSubject("invalid.jwt.token"));
    }

    @Test
    void testJwkSetStructure() {
        Map<String, Object> jwkSetMap = jwtTokenService.getJwkSetAsMap();
        assertNotNull(jwkSetMap);
        assertTrue(jwkSetMap.containsKey("keys"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> keys = (List<Map<String, Object>>) jwkSetMap.get("keys");
        assertFalse(keys.isEmpty());
        Map<String, Object> firstKey = keys.get(0);
        assertEquals("RSA", firstKey.get("kty"));
        assertEquals("sig", firstKey.get("use"));
        assertEquals("RS256", firstKey.get("alg"));
        assertEquals(JwtTokenService.KEY_ID, firstKey.get("kid"));
        assertNotNull(firstKey.get("n"));
        assertNotNull(firstKey.get("e"));
        // Ensure private key parameters are NEVER exposed in JWKS
        assertNull(firstKey.get("d"));
        assertNull(firstKey.get("p"));
        assertNull(firstKey.get("q"));
    }

    @Test
    void testResourceLoaderInit() {
        JwtTokenService serviceFromResources = new JwtTokenService(new DefaultResourceLoader());
        serviceFromResources.init();
        assertNotNull(serviceFromResources.getPublicKey());
        assertNotNull(serviceFromResources.getPrivateKey());

        String token = serviceFromResources.generateToken("resourceUser");
        assertEquals("resourceUser", serviceFromResources.verifyTokenAndGetSubject(token));
    }
}
