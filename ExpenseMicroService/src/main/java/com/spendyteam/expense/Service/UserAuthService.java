package com.spendyteam.expense.Service;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
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
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for authenticating and resolving user identity
 * by cryptographically validating the RS256 JWT access token locally.
 */
@Service
public class UserAuthService {

    @Value("${jwt.issuer:spendy-auth}")
    private String issuer = "spendy-auth";

    @Value("${jwt.audience:spendy-app}")
    private String audience = "spendy-app";

    @Value("${jwt.public-key-location:}")
    private String publicKeyLocation = "";

    @Value("${jwt.public-key:}")
    private String publicKeyPem;

    private final ResourceLoader resourceLoader;
    private final WebClient webClient;
    private final String gatewayUri;
    private RSAPublicKey publicKey;

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
        if (publicKey != null || canLoadPublicKey()) {
            try {
                if (publicKey == null) {
                    publicKey = loadPublicKey();
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
                // If local validation fails or token is malformed, do not fallback to mock unless explicitly intended
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
        } catch (Exception e) {
            return null;
        }
    }
}
