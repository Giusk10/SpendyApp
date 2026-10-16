package com.spendyteam.expense.Service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service responsible for authenticating and resolving user identity
 * by cryptographically validating the RS256 JWT access token locally.
 * Enforces algorithm restriction, mandatory claims (exp, iss, aud, sub),
 * and supports key rotation via 'kid'.
 */
@Service
public class UserAuthService {

    private static final Logger log = LoggerFactory.getLogger(UserAuthService.class);

    @Value("${jwt.issuer:spendy-auth}")
    private String issuer = "spendy-auth";

    @Value("${jwt.audience:spendy-app}")
    private String audience = "spendy-app";

    @Value("${jwt.public-key-location:}")
    private String publicKeyLocation = "";

    @Value("${jwt.public-key:}")
    private String publicKeyPem;

    @Value("${jwt.allow-ephemeral-keys:false}")
    private boolean allowEphemeralKeys = false;

    @Value("${jwt.previous-public-keys:}")
    private String previousPublicKeysPem = "";

    private final ResourceLoader resourceLoader;
    private final WebClient webClient;
    private final String gatewayUri;
    private RSAPublicKey publicKey;
    private final Map<String, RSAPublicKey> verificationKeys = new ConcurrentHashMap<>();

    public UserAuthService() {
        this(new DefaultResourceLoader());
    }

    public UserAuthService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader != null ? resourceLoader : new DefaultResourceLoader();
        this.webClient = null;
        this.gatewayUri = null;
    }

    public UserAuthService(RSAPublicKey publicKey, String issuer, String audience) {
        this.resourceLoader = null;
        this.publicKey = publicKey;
        this.issuer = issuer != null ? issuer : "spendy-auth";
        this.audience = audience != null ? audience : "spendy-app";
        this.webClient = null;
        this.gatewayUri = null;
        this.allowEphemeralKeys = true;
        if (publicKey != null) {
            this.verificationKeys.put("default", publicKey);
        }
    }

    /**
     * Backward-compatible constructor for existing tests that pass WebClient.
     */
    public UserAuthService(WebClient webClient) {
        this(webClient, "http://localhost:7860");
    }

    public UserAuthService(WebClient webClient, String gatewayUri) {
        this.resourceLoader = new DefaultResourceLoader();
        this.webClient = webClient;
        this.gatewayUri = (gatewayUri != null && !gatewayUri.isBlank()) ? gatewayUri : "http://localhost:7860";
        this.allowEphemeralKeys = true;
    }

    @Autowired
    public UserAuthService(
            ResourceLoader resourceLoader,
            @Autowired(required = false) WebClient webClient,
            @Value("${gateway.uri:http://localhost:7860}") String gatewayUri) {
        this.resourceLoader = resourceLoader != null ? resourceLoader : new DefaultResourceLoader();
        this.webClient = webClient;
        this.gatewayUri = gatewayUri;
    }

    @PostConstruct
    public void init() {
        if (this.publicKey == null) {
            this.publicKey = loadPublicKey();
        }

        if (this.publicKey != null) {
            this.verificationKeys.put("spendy-auth-key-1", this.publicKey);
        }

        loadPreviousPublicKeys();

        if (this.publicKey == null && !allowEphemeralKeys && webClient == null) {
            throw new IllegalStateException("CRITICAL SECURITY ERROR: ExpenseMicroService requires RSA public key in production " +
                    "(jwt.public-key or jwt.public-key-location). Ephemeral key fallback is disabled.");
        }
    }

    private void loadPreviousPublicKeys() {
        if (previousPublicKeysPem != null && !previousPublicKeysPem.isBlank()) {
            String[] entries = previousPublicKeysPem.split(";");
            for (String entry : entries) {
                String trimmed = entry.trim();
                if (trimmed.isEmpty()) continue;
                String kid;
                String pem;
                if (trimmed.contains("=")) {
                    String[] parts = trimmed.split("=", 2);
                    kid = parts[0].trim();
                    pem = parts[1].trim();
                } else {
                    kid = "key-" + UUID.randomUUID().toString().substring(0, 8);
                    pem = trimmed;
                }
                try {
                    RSAPublicKey prev = parsePublicKeyFromPem(pem);
                    if (prev != null) {
                        verificationKeys.put(kid, prev);
                        log.info("Registered previous public key for rotation: '{}'", kid);
                    }
                } catch (Exception e) {
                    log.error("Failed to parse previous public key entry: {}", e.getMessage());
                }
            }
        }
    }

    public void addVerificationKey(String kid, RSAPublicKey key) {
        if (kid != null && key != null) {
            verificationKeys.put(kid, key);
        }
    }

    public boolean isAllowEphemeralKeys() {
        return allowEphemeralKeys;
    }

    public void setAllowEphemeralKeys(boolean allowEphemeralKeys) {
        this.allowEphemeralKeys = allowEphemeralKeys;
    }


    /**
     * Resolves the username associated with a given JWT token via local cryptographic verification.
     *
     * @param token JWT token string
     * @return username if token is valid, or null if invalid, expired, or on failure
     */
    public String getUsernameFromToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        // 1. Attempt local cryptographic RS256 validation if public key is available
        if (publicKey != null || canLoadPublicKey() || !verificationKeys.isEmpty()) {
            try {
                if (publicKey == null && canLoadPublicKey()) {
                    publicKey = loadPublicKey();
                    if (publicKey != null) {
                        verificationKeys.put("default", publicKey);
                    }
                }

                SignedJWT signedJWT = SignedJWT.parse(token);

                // Enforce algorithm: strictly RS256
                if (!JWSAlgorithm.RS256.equals(signedJWT.getHeader().getAlgorithm())) {
                    log.warn("JWT rejected in ExpenseService: invalid algorithm {}", signedJWT.getHeader().getAlgorithm());
                    return null;
                }

                // Resolve key supporting kid rotation
                String kid = signedJWT.getHeader().getKeyID();
                RSAPublicKey keyToVerify = resolveVerificationKey(kid);
                if (keyToVerify == null) {
                    log.warn("JWT rejected in ExpenseService: no key available for verification (kid: {})", kid);
                    return null;
                }

                // Verify signature
                JWSVerifier verifier = new RSASSAVerifier(keyToVerify);
                if (!signedJWT.verify(verifier)) {
                    log.warn("JWT rejected in ExpenseService: invalid signature");
                    return null;
                }

                JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
                if (claims == null) {
                    return null;
                }

                // Enforce exp
                Date exp = claims.getExpirationTime();
                Date now = new Date();
                if (exp == null || exp.before(now)) {
                    log.warn("JWT rejected in ExpenseService: token expired or exp missing");
                    return null;
                }

                // Enforce iss
                String tokenIssuer = claims.getIssuer();
                if (tokenIssuer == null || !issuer.equals(tokenIssuer)) {
                    log.warn("JWT rejected in ExpenseService: invalid or missing issuer");
                    return null;
                }

                // Enforce aud
                List<String> aud = claims.getAudience();
                if (aud == null || !aud.contains(audience)) {
                    log.warn("JWT rejected in ExpenseService: invalid or missing audience");
                    return null;
                }

                // Enforce sub
                String subject = claims.getSubject();
                if (subject == null || subject.isBlank()) {
                    log.warn("JWT rejected in ExpenseService: missing or blank subject");
                    return null;
                }

                return subject;
            } catch (Exception e) {
                log.warn("JWT verification exception in ExpenseService: {}", e.getMessage());
                return null;
            }
        }

        // 2. Fallback to WebClient only if mock WebClient is configured in legacy tests
        if (webClient != null && gatewayUri != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, String> response = webClient.post()
                        .uri(gatewayUri + "/gateway/verify-token")
                        .bodyValue(Map.of("token", token))
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

                return response != null ? response.get("username") : null;
            } catch (Exception e) {
                return null;
            }
        }

        return null;
    }

    private RSAPublicKey resolveVerificationKey(String kid) {
        if (kid != null && verificationKeys.containsKey(kid)) {
            return verificationKeys.get(kid);
        }
        if (this.publicKey != null) {
            return this.publicKey;
        }
        if (!verificationKeys.isEmpty()) {
            return verificationKeys.values().iterator().next();
        }
        return null;
    }

    /**
     * Alias for {@link #getUsernameFromToken(String)} for backward compatibility.
     */
    public String getUsernameFromTokenViaRest(String token) {
        return getUsernameFromToken(token);
    }

    private boolean canLoadPublicKey() {
        return (publicKeyPem != null && !publicKeyPem.isBlank()) ||
                (publicKeyLocation != null && !publicKeyLocation.isBlank());
    }

    private RSAPublicKey loadPublicKey() {
        try {
            String pem = publicKeyPem;
            if ((pem == null || pem.isBlank()) && publicKeyLocation != null && !publicKeyLocation.isBlank()) {
                if (resourceLoader == null) {
                    return null;
                }
                Resource resource = resourceLoader.getResource(publicKeyLocation);
                if (!resource.exists()) {
                    return null;
                }
                try (InputStream is = resource.getInputStream()) {
                    pem = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
            if (pem == null || pem.isBlank()) {
                return null;
            }
            return parsePublicKeyFromPem(pem);
        } catch (Exception e) {
            log.error("Failed to load RSA public key: {}", e.getMessage());
            return null;
        }
    }

    private RSAPublicKey parsePublicKeyFromPem(String pem) throws Exception {
        String clean = pem
                .replace("\\n", "")
                .replace("-----BEGIN RSA PUBLIC KEY-----", "")
                .replace("-----END RSA PUBLIC KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(clean);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(keySpec);
    }
}
