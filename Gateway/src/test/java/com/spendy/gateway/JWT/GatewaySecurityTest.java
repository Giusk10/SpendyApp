package com.spendy.gateway.JWT;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;

class GatewaySecurityTest {

    private WebTestClient webTestClient;
    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;
    private RSAPrivateKey wrongPrivateKey;

    @RestController
    static class TestController {
        @GetMapping("/")
        public Mono<String> home() {
            return Mono.just("Home");
        }

        @PostMapping("/Auth/rest/auth/login")
        public Mono<String> login() {
            return Mono.just("Login OK");
        }

        @GetMapping("/Expense/rest/expense/getExpenses")
        public Mono<String> getExpenses() {
            return Mono.just("Expenses data");
        }

        @GetMapping("/Auth/rest/auth/profile")
        public Mono<String> profile() {
            return Mono.just("Profile OK");
        }

        @GetMapping("/Auth/rest/auth/unmapped")
        public Mono<String> unmappedAuth() {
            return Mono.just("Unmapped Auth Endpoint");
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        privateKey = (RSAPrivateKey) kp.getPrivate();
        publicKey = (RSAPublicKey) kp.getPublic();

        KeyPair wrongKp = kpg.generateKeyPair();
        wrongPrivateKey = (RSAPrivateKey) wrongKp.getPrivate();

        SecurityConfig securityConfig = new SecurityConfig();

        // Convert public key to PEM string for decoder
        String pubPem = "-----BEGIN PUBLIC KEY-----\n" +
                java.util.Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(publicKey.getEncoded()) +
                "\n-----END PUBLIC KEY-----";

        ReactiveJwtDecoder decoder = securityConfig.reactiveJwtDecoder(
                "",
                pubPem,
                "spendy-auth",
                "spendy-app",
                new DefaultResourceLoader()
        );

        ServerHttpSecurity http = ServerHttpSecurity.http();
        SecurityWebFilterChain chain = securityConfig.securityWebFilterChain(http, decoder);

        webTestClient = WebTestClient.bindToController(new TestController())
                .webFilter(new org.springframework.security.web.server.WebFilterChainProxy(chain))
                .build();
    }

    private String createToken(RSAPrivateKey key, String issuer, String audience, long expiryOffsetMs) throws Exception {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiryOffsetMs);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("testuser")
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
    void testPublicRouteWithoutToken_ReturnsOk() {
        webTestClient.get().uri("/")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void testPublicLoginRouteWithoutToken_ReturnsOk() {
        webTestClient.post().uri("/Auth/rest/auth/login")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void testProtectedRouteWithoutToken_ReturnsUnauthorized() {
        webTestClient.get().uri("/Expense/rest/expense/getExpenses")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testProtectedRouteWithValidToken_ReturnsOk() throws Exception {
        String token = createToken(privateKey, "spendy-auth", "spendy-app", 60000);

        webTestClient.get().uri("/Expense/rest/expense/getExpenses")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("Expenses data");
    }

    @Test
    void testProtectedRouteWithExpiredToken_ReturnsUnauthorized() throws Exception {
        String token = createToken(privateKey, "spendy-auth", "spendy-app", -5000);

        webTestClient.get().uri("/Expense/rest/expense/getExpenses")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testProtectedRouteWithTamperedSignature_ReturnsUnauthorized() throws Exception {
        String token = createToken(privateKey, "spendy-auth", "spendy-app", 60000);
        int lastDot = token.lastIndexOf('.');
        int targetPos = lastDot + 10;
        char orig = token.charAt(targetPos);
        char replaced = (orig == 'A') ? 'B' : 'A';
        String tampered = token.substring(0, targetPos) + replaced + token.substring(targetPos + 1);

        webTestClient.get().uri("/Expense/rest/expense/getExpenses")
                .header("Authorization", "Bearer " + tampered)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testProtectedRouteWithUnknownKey_ReturnsUnauthorized() throws Exception {
        String token = createToken(wrongPrivateKey, "spendy-auth", "spendy-app", 60000);

        webTestClient.get().uri("/Expense/rest/expense/getExpenses")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testProtectedRouteWithWrongIssuer_ReturnsUnauthorized() throws Exception {
        String token = createToken(privateKey, "wrong-issuer", "spendy-app", 60000);

        webTestClient.get().uri("/Expense/rest/expense/getExpenses")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testProtectedRouteWithWrongAudience_ReturnsUnauthorized() throws Exception {
        String token = createToken(privateKey, "spendy-auth", "wrong-audience", 60000);

        webTestClient.get().uri("/Expense/rest/expense/getExpenses")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testUnmappedAuthEndpointWithoutToken_ReturnsUnauthorized() {
        // Confirms that general wildcard .pathMatchers("/Auth/**").permitAll() was removed
        webTestClient.get().uri("/Auth/rest/auth/unmapped")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testProtectedRouteWithDisallowedAlgorithmHS256_ReturnsUnauthorized() throws Exception {
        byte[] secret = new byte[32];
        new java.security.SecureRandom().nextBytes(secret);
        JWSSigner hmacSigner = new com.nimbusds.jose.crypto.MACSigner(secret);

        Date now = new Date();
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("attacker")
                .issuer("spendy-auth")
                .audience("spendy-app")
                .issueTime(now)
                .expirationTime(new Date(now.getTime() + 60000))
                .build();

        SignedJWT hmacJwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.HS256).keyID("spendy-auth-key-1").build(),
                claimsSet
        );
        hmacJwt.sign(hmacSigner);
        String hmacToken = hmacJwt.serialize();

        webTestClient.get().uri("/Expense/rest/expense/getExpenses")
                .header("Authorization", "Bearer " + hmacToken)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}

