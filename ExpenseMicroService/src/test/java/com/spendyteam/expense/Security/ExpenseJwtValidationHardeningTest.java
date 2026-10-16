package com.spendyteam.expense.Security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.spendyteam.expense.Service.UserAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Fase 10 - Validazione Rigorosa JWT in ExpenseMicroService")
class ExpenseJwtValidationHardeningTest {

    private UserAuthService userAuthService;
    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;

    private static final String ISSUER = "spendy-auth";
    private static final String AUDIENCE = "spendy-app";

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        privateKey = (RSAPrivateKey) kp.getPrivate();
        publicKey = (RSAPublicKey) kp.getPublic();

        userAuthService = new UserAuthService(publicKey, ISSUER, AUDIENCE);
    }

    private String buildJwt(JWSAlgorithm alg, String issuer, String audience, String subject, Date exp, String kid) throws Exception {
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .jwtID(UUID.randomUUID().toString())
                .issueTime(new Date());

        if (subject != null) builder.subject(subject);
        if (issuer != null) builder.issuer(issuer);
        if (audience != null) builder.audience(audience);
        if (exp != null) builder.expirationTime(exp);

        JWSHeader.Builder headerBuilder = new JWSHeader.Builder(alg);
        if (kid != null) headerBuilder.keyID(kid);

        SignedJWT signedJWT = new SignedJWT(headerBuilder.build(), builder.build());
        if (alg.equals(JWSAlgorithm.RS256)) {
            signedJWT.sign(new RSASSASigner(privateKey));
        } else if (alg.equals(JWSAlgorithm.HS256)) {
            byte[] secret = new byte[32];
            new java.security.SecureRandom().nextBytes(secret);
            signedJWT.sign(new MACSigner(secret));
        }
        return signedJWT.serialize();
    }

    @Test
    @DisplayName("JWT con algoritmo non consentito (HS256) viene rifiutato")
    void testDisallowedAlgorithmHS256Rejected() throws Exception {
        String hs256Token = buildJwt(JWSAlgorithm.HS256, ISSUER, AUDIENCE, "attacker",
                new Date(System.currentTimeMillis() + 60000), "kid-1");

        assertNull(userAuthService.getUsernameFromToken(hs256Token),
                "I token con algoritmo non-RS256 devono essere rifiutati");
    }

    @Test
    @DisplayName("JWT privo di claim exp obbligatorio viene rifiutato")
    void testMissingExpClaimRejected() throws Exception {
        String tokenWithoutExp = buildJwt(JWSAlgorithm.RS256, ISSUER, AUDIENCE, "user1", null, "spendy-auth-key-1");
        assertNull(userAuthService.getUsernameFromToken(tokenWithoutExp));
    }

    @Test
    @DisplayName("JWT con claim exp scaduto viene rifiutato")
    void testExpiredTokenRejected() throws Exception {
        String expiredToken = buildJwt(JWSAlgorithm.RS256, ISSUER, AUDIENCE, "user1",
                new Date(System.currentTimeMillis() - 10000), "spendy-auth-key-1");
        assertNull(userAuthService.getUsernameFromToken(expiredToken));
    }

    @Test
    @DisplayName("JWT con issuer mancante o errato viene rifiutato")
    void testInvalidIssuerRejected() throws Exception {
        String wrongIssuerToken = buildJwt(JWSAlgorithm.RS256, "wrong-issuer", AUDIENCE, "user1",
                new Date(System.currentTimeMillis() + 60000), "spendy-auth-key-1");
        assertNull(userAuthService.getUsernameFromToken(wrongIssuerToken));

        String nullIssuerToken = buildJwt(JWSAlgorithm.RS256, null, AUDIENCE, "user1",
                new Date(System.currentTimeMillis() + 60000), "spendy-auth-key-1");
        assertNull(userAuthService.getUsernameFromToken(nullIssuerToken));
    }

    @Test
    @DisplayName("JWT con audience mancante o errata viene rifiutato")
    void testInvalidAudienceRejected() throws Exception {
        String wrongAudToken = buildJwt(JWSAlgorithm.RS256, ISSUER, "wrong-aud", "user1",
                new Date(System.currentTimeMillis() + 60000), "spendy-auth-key-1");
        assertNull(userAuthService.getUsernameFromToken(wrongAudToken));

        String nullAudToken = buildJwt(JWSAlgorithm.RS256, ISSUER, null, "user1",
                new Date(System.currentTimeMillis() + 60000), "spendy-auth-key-1");
        assertNull(userAuthService.getUsernameFromToken(nullAudToken));
    }

    @Test
    @DisplayName("Supporto per rotazione chiavi con kid in ExpenseMicroService")
    void testKeyRotationSupport() throws Exception {
        KeyPairGenerator kpgRot = KeyPairGenerator.getInstance("RSA");
        kpgRot.initialize(2048);
        KeyPair rotatedKp = kpgRot.generateKeyPair();
        String rotatedKid = "rotated-key-2";

        userAuthService.addVerificationKey(rotatedKid, (RSAPublicKey) rotatedKp.getPublic());

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("rotatedUser")
                .issuer(ISSUER)
                .audience(AUDIENCE)
                .expirationTime(new Date(System.currentTimeMillis() + 60000))
                .build();
        SignedJWT token = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rotatedKid).build(),
                claims
        );
        token.sign(new RSASSASigner((RSAPrivateKey) rotatedKp.getPrivate()));

        String username = userAuthService.getUsernameFromToken(token.serialize());
        assertEquals("rotatedUser", username, "Il token con kid ruotato deve essere validato correttamente");
    }

    @Test
    @DisplayName("JWT con kid sconosciuto viene rifiutato esplicitamente in ExpenseMicroService")
    void testUnknownKidExplicitlyRejected() throws Exception {
        String unknownKidToken = buildJwt(JWSAlgorithm.RS256, ISSUER, AUDIENCE, "attacker",
                new Date(System.currentTimeMillis() + 60000), "unknown-unregistered-kid-999");
        assertNull(userAuthService.getUsernameFromToken(unknownKidToken),
                "Un JWT con kid sconosciuto non deve essere accettato da ExpenseService");
    }
}
