package com.spendy.auth.Security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service responsible for signing and verifying JWT tokens using RS256,
 * managing cryptographic keys, supporting key rotation via 'kid',
 * and strictly validating token claims and signatures.
 */
@Service
public class JwtTokenService {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenService.class);

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

    @Value("${jwt.private-key-location:}")
    private String privateKeyLocation = "";

    @Value("${jwt.public-key-location:}")
    private String publicKeyLocation = "";

    @Value("${jwt.allow-ephemeral-keys:false}")
    private boolean allowEphemeralKeys = false;

    @Value("${jwt.active-key-id:spendy-auth-key-1}")
    private String activeKeyId = KEY_ID;

    @Value("${jwt.previous-public-keys:}")
    private String previousPublicKeysPem = "";

    private final ResourceLoader resourceLoader;

    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;
    private final Map<String, RSAPublicKey> verificationKeys = new ConcurrentHashMap<>();
    private JWKSet jwkSet;

    public JwtTokenService() {
        this(new DefaultResourceLoader());
    }

    @Autowired
    public JwtTokenService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader != null ? resourceLoader : new DefaultResourceLoader();
    }

    // Constructor for testing with direct keys
    public JwtTokenService(RSAPrivateKey privateKey, RSAPublicKey publicKey, String issuer, String audience, long expirationTimeMs) {
        this(privateKey, publicKey, issuer, audience, expirationTimeMs, KEY_ID);
    }

    public JwtTokenService(RSAPrivateKey privateKey, RSAPublicKey publicKey, String issuer, String audience, long expirationTimeMs, String activeKeyId) {
        this.resourceLoader = null;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.issuer = issuer != null ? issuer : "spendy-auth";
        this.audience = audience != null ? audience : "spendy-app";
        this.expirationTimeMs = expirationTimeMs;
        this.activeKeyId = activeKeyId != null ? activeKeyId : KEY_ID;
        this.allowEphemeralKeys = true;
        if (publicKey != null) {
            this.verificationKeys.put(this.activeKeyId, publicKey);
        }
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

        if (privateKey == null || publicKey == null) {
            throw new IllegalStateException("CRITICAL SECURITY ERROR: Both RSA private key and public key must be initialized.");
        }

        this.verificationKeys.put(this.activeKeyId, this.publicKey);

        // Load any previous public keys for rotation window
        loadPreviousPublicKeys();

        initJwkSet();
        log.info("JwtTokenService initialized successfully. Active Key ID: '{}'. Verification keys count: {}",
                activeKeyId, verificationKeys.size());
    }

    private void loadPreviousPublicKeys() {
        if (previousPublicKeysPem != null && !previousPublicKeysPem.isBlank()) {
            String[] keyEntries = previousPublicKeysPem.split(";");
            for (String entry : keyEntries) {
                String trimmed = entry.trim();
                if (trimmed.isEmpty()) continue;
                String kid;
                String pem;
                if (trimmed.contains("=")) {
                    String[] parts = trimmed.split("=", 2);
                    kid = parts[0].trim();
                    pem = parts[1].trim();
                } else {
                    kid = "spendy-auth-key-prev-" + UUID.randomUUID().toString().substring(0, 8);
                    pem = trimmed;
                }
                try {
                    RSAPublicKey prevKey = parsePublicKeyFromPem(pem);
                    if (prevKey != null) {
                        verificationKeys.put(kid, prevKey);
                        log.info("Registered previous public key with kid: '{}'", kid);
                    }
                } catch (Exception e) {
                    log.error("Failed to parse previous public key entry: {}", e.getMessage());
                }
            }
        }
    }

    public synchronized void initJwkSet() {
        List<JWK> keys = new ArrayList<>();
        for (Map.Entry<String, RSAPublicKey> entry : verificationKeys.entrySet()) {
            RSAKey rsaKey = new RSAKey.Builder(entry.getValue())
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.RS256)
                    .keyID(entry.getKey())
                    .build();
            keys.add(rsaKey);
        }
        this.jwkSet = new JWKSet(keys);
    }

    public void addVerificationKey(String kid, RSAPublicKey key) {
        if (kid != null && key != null) {
            verificationKeys.put(kid, key);
            initJwkSet();
        }
    }

    public String generateToken(String username) {
        return generateToken(username, List.of("ROLE_USER"));
    }

    public String generateToken(String username, List<String> roles) {
        try {
            if (username == null || username.isBlank()) {
                throw new IllegalArgumentException("Username cannot be null or blank when generating token.");
            }
            if (privateKey == null) {
                throw new IllegalStateException("Cannot sign JWT: RSA private key is not configured.");
            }

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
                    .keyID(activeKeyId)
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
        JWTClaimsSet claims = verifyAndGetClaims(token);
        return claims != null ? claims.getSubject() : null;
    }

    public JWTClaimsSet verifyAndGetClaims(String token) {
        try {
            if (token == null || token.isBlank()) {
                return null;
            }

            SignedJWT signedJWT = SignedJWT.parse(token);

            // 1. Strict Algorithm Validation: must be exactly RS256
            if (!JWSAlgorithm.RS256.equals(signedJWT.getHeader().getAlgorithm())) {
                log.warn("JWT rejected: invalid or unapproved algorithm: {}", signedJWT.getHeader().getAlgorithm());
                return null;
            }

            // 2. Resolve verification key supporting kid rotation
            String kid = signedJWT.getHeader().getKeyID();
            RSAPublicKey keyToVerify = resolveVerificationKey(kid);
            if (keyToVerify == null) {
                log.warn("JWT rejected: unknown key ID: {}", kid);
                return null;
            }

            // 3. Cryptographic Signature Verification
            JWSVerifier verifier = new RSASSAVerifier(keyToVerify);
            if (!signedJWT.verify(verifier)) {
                log.warn("JWT signature verification failed");
                return null;
            }

            // 4. Validate Claims Set
            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
            if (claimsSet == null) {
                log.warn("JWT rejected: missing claims set");
                return null;
            }

            // 5. Expiration Check (exp is mandatory)
            Date exp = claimsSet.getExpirationTime();
            if (exp == null) {
                log.warn("JWT rejected: missing exp claim");
                return null;
            }
            Date now = new Date();
            if (exp.before(now)) {
                log.warn("JWT rejected: token expired");
                return null;
            }

            // 6. Issuer Check (iss is mandatory)
            String tokenIssuer = claimsSet.getIssuer();
            if (tokenIssuer == null || !issuer.equals(tokenIssuer)) {
                log.warn("JWT rejected: invalid issuer");
                return null;
            }

            // 7. Audience Check (aud is mandatory)
            List<String> tokenAudience = claimsSet.getAudience();
            if (tokenAudience == null || !tokenAudience.contains(audience)) {
                log.warn("JWT rejected: invalid audience");
                return null;
            }

            // 8. Subject Check (sub is mandatory)
            String subject = claimsSet.getSubject();
            if (subject == null || subject.isBlank()) {
                log.warn("JWT rejected: missing or blank subject");
                return null;
            }

            return claimsSet;
        } catch (Exception e) {
            log.warn("JWT verification exception: {}", e.getMessage());
            return null;
        }
    }

    private RSAPublicKey resolveVerificationKey(String kid) {
        if (kid != null && verificationKeys.containsKey(kid)) {
            return verificationKeys.get(kid);
        }
        return this.publicKey;
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

    public String getActiveKeyId() {
        return activeKeyId;
    }

    public void setActiveKeyId(String activeKeyId) {
        this.activeKeyId = activeKeyId;
    }

    public boolean isAllowEphemeralKeys() {
        return allowEphemeralKeys;
    }

    public void setAllowEphemeralKeys(boolean allowEphemeralKeys) {
        this.allowEphemeralKeys = allowEphemeralKeys;
    }

    private RSAPrivateKey loadPrivateKey() {
        try {
            String pem = privateKeyPem;
            if ((pem == null || pem.isBlank()) && privateKeyLocation != null && !privateKeyLocation.isBlank()) {
                pem = readResourceContent(privateKeyLocation);
            }
            if (pem != null && !pem.isBlank()) {
                byte[] decoded = decodePem(pem, "PRIVATE KEY");
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
                KeyFactory kf = KeyFactory.getInstance("RSA");
                return (RSAPrivateKey) kf.generatePrivate(keySpec);
            }

            if (!allowEphemeralKeys) {
                throw new IllegalStateException("CRITICAL SECURITY ERROR: RSA private key is missing in production environment. " +
                        "Ephemeral key fallback is disabled (jwt.allow-ephemeral-keys=false). Please provide JWT_PRIVATE_KEY or JWT_PRIVATE_KEY_LOCATION.");
            }

            // In dev / ephemeral mode, reuse existing key from keys/ if present
            pem = readResourceContent("file:keys/private_key.pem");
            if (pem != null && !pem.isBlank()) {
                byte[] decoded = decodePem(pem, "PRIVATE KEY");
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
                KeyFactory kf = KeyFactory.getInstance("RSA");
                return (RSAPrivateKey) kf.generatePrivate(keySpec);
            }

            log.warn("WARNING: Ephemeral 2048-bit RSA key pair generated because jwt.allow-ephemeral-keys=true. " +
                    "DO NOT use this configuration in production!");
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair kp = kpg.generateKeyPair();
            this.publicKey = (RSAPublicKey) kp.getPublic();
            saveEphemeralKeysIfPossible(kp);
            return (RSAPrivateKey) kp.getPrivate();
        } catch (IllegalStateException ise) {
            throw ise;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load RSA private key: " + e.getMessage(), e);
        }
    }

    private void saveEphemeralKeysIfPossible(KeyPair kp) {
        try {
            java.io.File keysDir = new java.io.File("keys");
            if (!keysDir.exists() && new java.io.File("../keys").exists()) {
                keysDir = new java.io.File("../keys");
            } else if (!keysDir.exists()) {
                keysDir.mkdirs();
            }
            if (keysDir.exists() && keysDir.isDirectory() && keysDir.canWrite()) {
                java.io.File privFile = new java.io.File(keysDir, "private_key.pem");
                java.io.File pubFile = new java.io.File(keysDir, "public_key.pem");
                if (!privFile.exists()) {
                    String privPem = "-----BEGIN PRIVATE KEY-----\n" +
                            Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(kp.getPrivate().getEncoded()) +
                            "\n-----END PRIVATE KEY-----\n";
                    java.nio.file.Files.writeString(privFile.toPath(), privPem);
                }
                if (!pubFile.exists()) {
                    String pubPem = "-----BEGIN PUBLIC KEY-----\n" +
                            Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(kp.getPublic().getEncoded()) +
                            "\n-----END PUBLIC KEY-----\n";
                    java.nio.file.Files.writeString(pubFile.toPath(), pubPem);
                }
            }
        } catch (Exception e) {
            log.debug("Ephemeral key auto-persisting skipped: {}", e.getMessage());
        }
    }

    private RSAPublicKey loadPublicKey() {
        if (this.publicKey != null) {
            return this.publicKey;
        }
        try {
            String pem = publicKeyPem;
            if ((pem == null || pem.isBlank()) && publicKeyLocation != null && !publicKeyLocation.isBlank()) {
                pem = readResourceContent(publicKeyLocation);
            }
            if ((pem == null || pem.isBlank()) && allowEphemeralKeys) {
                pem = readResourceContent("file:keys/public_key.pem");
            }
            if (pem != null && !pem.isBlank()) {
                return parsePublicKeyFromPem(pem);
            }
            return null;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load RSA public key: " + e.getMessage(), e);
        }
    }

    private RSAPublicKey parsePublicKeyFromPem(String pem) throws Exception {
        byte[] decoded = decodePem(pem, "PUBLIC KEY");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(keySpec);
    }

    private String readResourceContent(String location) {
        if (resourceLoader == null || location == null || location.isBlank()) {
            return null;
        }
        try {
            Resource resource = resourceLoader.getResource(location);
            if (!resource.exists() && location.startsWith("file:") && !location.startsWith("file:/")) {
                String rel = location.substring("file:".length());
                Resource alt = resourceLoader.getResource("file:../" + rel);
                if (alt.exists()) {
                    resource = alt;
                }
            }
            if (!resource.exists()) {
                return null;
            }
            try (InputStream is = resource.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] decodePem(String pem, String type) {
        String clean = pem
                .replace("\\n", "")
                .replace("-----BEGIN RSA " + type + "-----", "")
                .replace("-----END RSA " + type + "-----", "")
                .replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "")
                .replaceAll("\\s+", "");
        return Base64.getDecoder().decode(clean);
    }
}
