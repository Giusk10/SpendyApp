package com.spendy.auth.Security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
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

@DisplayName("Fase 10 - Test di Sicurezza JWT (AuthMicroService)")
class JwtSecurityHardeningTest {

    private JwtTokenService jwtTokenService;
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

        jwtTokenService = new JwtTokenService(privateKey, publicKey, ISSUER, AUDIENCE, 60000);
    }

    // 1. JWT valido accettato
    @Test
    @DisplayName("1. JWT valido con tutti i claim richiesti viene accettato")
    void testValidJwtAccepted() {
        String token = jwtTokenService.generateToken("alice");
        assertNotNull(token);
        String subject = jwtTokenService.verifyTokenAndGetSubject(token);
        assertEquals("alice", subject);
    }

    // 2. Firma alterata rifiutata
    @Test
    @DisplayName("2. JWT con firma alterata viene categoricamente rifiutato")
    void testTamperedSignatureRejected() {
        String token = jwtTokenService.generateToken("alice");
        int lastDot = token.lastIndexOf('.');
        int targetPos = lastDot + 5;
        char orig = token.charAt(targetPos);
        char replaced = (orig == 'A') ? 'B' : 'A';
        String tampered = token.substring(0, targetPos) + replaced + token.substring(targetPos + 1);

        String subject = jwtTokenService.verifyTokenAndGetSubject(tampered);
        assertNull(subject, "Il token con firma manomessa deve restituire null");
    }

    // 3. Token scaduto rifiutato
    @Test
    @DisplayName("3. JWT scaduto viene rifiutato")
    void testExpiredTokenRejected() {
        JwtTokenService expiredService = new JwtTokenService(privateKey, publicKey, ISSUER, AUDIENCE, -5000);
        String expiredToken = expiredService.generateToken("bob");

        assertNull(jwtTokenService.verifyTokenAndGetSubject(expiredToken), "Il token scaduto deve essere rifiutato");
    }

    // 4. Claim exp mancante rifiutato
    @Test
    @DisplayName("4. JWT privo del claim exp obbligatorio viene rifiutato")
    void testMissingExpClaimRejected() throws Exception {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("charlie")
                .issuer(ISSUER)
                .audience(AUDIENCE)
                .issueTime(new Date())
                // No expirationTime set!
                .jwtID(UUID.randomUUID().toString())
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(JwtTokenService.KEY_ID).build(),
                claimsSet
        );
        signedJWT.sign(new RSASSASigner(privateKey));
        String tokenWithoutExp = signedJWT.serialize();

        assertNull(jwtTokenService.verifyTokenAndGetSubject(tokenWithoutExp),
                "Un JWT senza il claim 'exp' deve essere rifiutato");
    }

    // 5. Issuer errato o mancante rifiutato
    @Test
    @DisplayName("5. JWT con issuer errato o mancante viene rifiutato")
    void testWrongOrMissingIssuerRejected() throws Exception {
        // Wrong issuer
        JWTClaimsSet wrongIssuerClaims = new JWTClaimsSet.Builder()
                .subject("david")
                .issuer("malicious-issuer")
                .audience(AUDIENCE)
                .expirationTime(new Date(System.currentTimeMillis() + 60000))
                .build();
        SignedJWT jwt1 = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), wrongIssuerClaims);
        jwt1.sign(new RSASSASigner(privateKey));
        assertNull(jwtTokenService.verifyTokenAndGetSubject(jwt1.serialize()), "Issuer errato deve essere rifiutato");

        // Missing issuer
        JWTClaimsSet noIssuerClaims = new JWTClaimsSet.Builder()
                .subject("david")
                .audience(AUDIENCE)
                .expirationTime(new Date(System.currentTimeMillis() + 60000))
                .build();
        SignedJWT jwt2 = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), noIssuerClaims);
        jwt2.sign(new RSASSASigner(privateKey));
        assertNull(jwtTokenService.verifyTokenAndGetSubject(jwt2.serialize()), "Issuer mancante deve essere rifiutato");
    }

    // 6. Audience errata o mancante rifiutata
    @Test
    @DisplayName("6. JWT con audience errata o mancante viene rifiutato")
    void testWrongOrMissingAudienceRejected() throws Exception {
        // Wrong audience
        JWTClaimsSet wrongAudClaims = new JWTClaimsSet.Builder()
                .subject("eve")
                .issuer(ISSUER)
                .audience("another-app")
                .expirationTime(new Date(System.currentTimeMillis() + 60000))
                .build();
        SignedJWT jwt1 = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), wrongAudClaims);
        jwt1.sign(new RSASSASigner(privateKey));
        assertNull(jwtTokenService.verifyTokenAndGetSubject(jwt1.serialize()), "Audience errata deve essere rifiutata");

        // Missing audience
        JWTClaimsSet noAudClaims = new JWTClaimsSet.Builder()
                .subject("eve")
                .issuer(ISSUER)
                .expirationTime(new Date(System.currentTimeMillis() + 60000))
                .build();
        SignedJWT jwt2 = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), noAudClaims);
        jwt2.sign(new RSASSASigner(privateKey));
        assertNull(jwtTokenService.verifyTokenAndGetSubject(jwt2.serialize()), "Audience mancante deve essere rifiutata");
    }

    // 7. Algoritmo non consentito rifiutato (es. HS256, 'none')
    @Test
    @DisplayName("7. JWT con algoritmo non consentito (HS256 o 'none') viene rifiutato")
    void testDisallowedAlgorithmRejected() throws Exception {
        // Test HS256 symmetric signature attempt
        byte[] secret = new byte[32];
        new java.security.SecureRandom().nextBytes(secret);
        JWSSigner hmacSigner = new MACSigner(secret);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("frank")
                .issuer(ISSUER)
                .audience(AUDIENCE)
                .expirationTime(new Date(System.currentTimeMillis() + 60000))
                .build();

        SignedJWT hmacJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        hmacJwt.sign(hmacSigner);
        String hmacToken = hmacJwt.serialize();

        assertNull(jwtTokenService.verifyTokenAndGetSubject(hmacToken),
                "Algoritmo simmetrico HS256 deve essere categoricamente rifiutato");
    }

    // 8. Token malformato rifiutato
    @Test
    @DisplayName("8. Token malformati o stringhe arbitrarie vengono rifiutati")
    void testMalformedTokenRejected() {
        assertNull(jwtTokenService.verifyTokenAndGetSubject("not-a-jwt"));
        assertNull(jwtTokenService.verifyTokenAndGetSubject("abc.def.ghi"));
        assertNull(jwtTokenService.verifyTokenAndGetSubject(null));
        assertNull(jwtTokenService.verifyTokenAndGetSubject(""));
        assertNull(jwtTokenService.verifyTokenAndGetSubject("    "));
    }

    // 9. Token firmato con chiave sconosciuta rifiutato
    @Test
    @DisplayName("9. JWT firmato con chiave privata sconosciuta viene rifiutato")
    void testUnknownKeySignedTokenRejected() throws Exception {
        KeyPairGenerator kpg2 = KeyPairGenerator.getInstance("RSA");
        kpg2.initialize(2048);
        KeyPair kp2 = kpg2.generateKeyPair();

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("grace")
                .issuer(ISSUER)
                .audience(AUDIENCE)
                .expirationTime(new Date(System.currentTimeMillis() + 60000))
                .build();

        SignedJWT unknownKeyJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        unknownKeyJwt.sign(new RSASSASigner((RSAPrivateKey) kp2.getPrivate()));

        assertNull(jwtTokenService.verifyTokenAndGetSubject(unknownKeyJwt.serialize()),
                "Token firmato con chiave non registrata deve essere rifiutato");
    }

    // 10. Rotazione delle chiavi verificata (kid)
    @Test
    @DisplayName("10. Rotazione delle chiavi: verifica token con kid precedente durante la transizione")
    void testKeyRotationWithKid() throws Exception {
        // Crea una seconda chiave per rappresentare la chiave precedente
        KeyPairGenerator kpgRot = KeyPairGenerator.getInstance("RSA");
        kpgRot.initialize(2048);
        KeyPair oldKp = kpgRot.generateKeyPair();
        String oldKid = "spendy-auth-key-0";

        // Registra la vecchia chiave pubblica per la verifica di transizione
        jwtTokenService.addVerificationKey(oldKid, (RSAPublicKey) oldKp.getPublic());

        // Emetti un token con la vecchia chiave privata
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("heidi")
                .issuer(ISSUER)
                .audience(AUDIENCE)
                .expirationTime(new Date(System.currentTimeMillis() + 60000))
                .build();
        SignedJWT oldToken = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(oldKid).build(),
                claims
        );
        oldToken.sign(new RSASSASigner((RSAPrivateKey) oldKp.getPrivate()));

        // Il servizio deve verificare con successo il token emesso con la chiave precedente
        String subject = jwtTokenService.verifyTokenAndGetSubject(oldToken.serialize());
        assertEquals("heidi", subject, "Il token con kid della chiave precedente deve essere verificato con successo durante la rotazione");
    }
}
