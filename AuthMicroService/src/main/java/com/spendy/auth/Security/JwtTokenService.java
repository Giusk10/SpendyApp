package com.spendy.auth.Security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

@Service
public class JwtTokenService {

    public static final String KEY_ID = "spendy-auth-key-1";

    @Value("${jwt.issuer:spendy-auth}")
    private String issuer = "spendy-auth";

    @Value("${jwt.audience:spendy-app}")
    private String audience = "spendy-app";

    @Value("${jwt.expiration:900000}")
    private long expirationTimeMs = 900000;

    @Value("${jwt.private-key:}")
    private String privateKeyPem;

    @Value("${jwt.public-key:}")
    private String publicKeyPem;

    @Value("${jwt.private-key-location:classpath:keys/private_key.pem}")
    private String privateKeyLocation = "classpath:keys/private_key.pem";

    @Value("${jwt.public-key-location:classpath:keys/public_key.pem}")
    private String publicKeyLocation = "classpath:keys/public_key.pem";

    private final ResourceLoader resourceLoader;

    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;
    private JWKSet jwkSet;

    public JwtTokenService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    // Constructor for testing with direct keys
    public JwtTokenService(RSAPrivateKey privateKey, RSAPublicKey publicKey, String issuer, String audience, long expirationTimeMs) {
        this.resourceLoader = null;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.issuer = issuer != null ? issuer : "spendy-auth";
        this.audience = audience != null ? audience : "spendy-app";
        this.expirationTimeMs = expirationTimeMs;
        initJwkSet();
    }

    @PostConstruct
    public void init() {
        if (privateKey == null) {
            this.privateKey = loadPrivateKey();
        }
        if (publicKey == null) {
            this.publicKey = loadPublicKey();
        }
        initJwkSet();
    }

    private void initJwkSet() {
        if (publicKey != null) {
            RSAKey rsaKey = new RSAKey.Builder(publicKey)
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.RS256)
                    .keyID(KEY_ID)
                    .build();
            this.jwkSet = new JWKSet(rsaKey);
        }
    }

    public String generateToken(String username) {
        return generateToken(username, List.of("ROLE_USER"));
    }

    public String generateToken(String username, List<String> roles) {
        try {
            Date now = new Date();
            Date expiry = new Date(now.getTime() + expirationTimeMs);

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(username)
                    .issuer(issuer)
                    .audience(audience)
                    .issueTime(now)
                    .expirationTime(expiry)
                    .jwtID(UUID.randomUUID().toString())
                    .claim("roles", roles != null ? roles : List.of("ROLE_USER"))
                    .build();

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(KEY_ID)
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            JWSSigner signer = new RSASSASigner(privateKey);
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate JWT token: " + e.getMessage(), e);
        }
    }

    public String verifyTokenAndGetSubject(String token) {
        try {
            if (token == null || token.isBlank()) {
                return null;
            }

            SignedJWT signedJWT = SignedJWT.parse(token);

            JWSVerifier verifier = new RSASSAVerifier(publicKey);
            if (!signedJWT.verify(verifier)) {
                return null;
            }

            Date now = new Date();
            Date exp = signedJWT.getJWTClaimsSet().getExpirationTime();
            if (exp == null || exp.before(now)) {
                return null;
            }

            String tokenIssuer = signedJWT.getJWTClaimsSet().getIssuer();
            if (tokenIssuer != null && !issuer.equals(tokenIssuer)) {
                return null;
            }

            List<String> aud = signedJWT.getJWTClaimsSet().getAudience();
            if (aud != null && !aud.isEmpty() && !aud.contains(audience)) {
                return null;
            }

            return signedJWT.getJWTClaimsSet().getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    public Map<String, Object> getJwkSetAsMap() {
        return jwkSet != null ? jwkSet.toJSONObject() : Collections.emptyMap();
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    public RSAPrivateKey getPrivateKey() {
        return privateKey;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getAudience() {
        return audience;
    }

    public long getExpirationTimeMs() {
        return expirationTimeMs;
    }

    private RSAPrivateKey loadPrivateKey() {
        try {
            String pem = privateKeyPem;
            if (pem == null || pem.isBlank()) {
                pem = readResourceContent(privateKeyLocation);
            }
            byte[] decoded = decodePem(pem, "PRIVATE KEY");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) kf.generatePrivate(keySpec);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load RSA private key: " + e.getMessage(), e);
        }
    }

    private RSAPublicKey loadPublicKey() {
        try {
            String pem = publicKeyPem;
            if (pem == null || pem.isBlank()) {
                pem = readResourceContent(publicKeyLocation);
            }
            byte[] decoded = decodePem(pem, "PUBLIC KEY");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) kf.generatePublic(keySpec);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load RSA public key: " + e.getMessage(), e);
        }
    }

    private String readResourceContent(String location) throws Exception {
        if (resourceLoader == null) {
            throw new IllegalStateException("ResourceLoader is not available");
        }
        Resource resource = resourceLoader.getResource(location);
        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private byte[] decodePem(String pem, String type) {
        String clean = pem
                .replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "")
                .replaceAll("\\s+", "");
        return Base64.getDecoder().decode(clean);
    }
}
