package com.spendy.gateway.JWT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private ReactiveJwtDecoder reactiveJwtDecoder;

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig();
    }

    @Test
    void testSecurityConfig_Instantiation() {
        assertNotNull(securityConfig);
    }

    @Test
    void testSecurityWebFilterChain_Creation() {
        ServerHttpSecurity http = ServerHttpSecurity.http();
        SecurityWebFilterChain filterChain = securityConfig.securityWebFilterChain(http, reactiveJwtDecoder);

        assertNotNull(filterChain);
    }

    @Test
    void testReactiveJwtDecoder_CreationFromPem() throws Exception {
        java.security.KeyPair kp = java.security.KeyPairGenerator.getInstance("RSA").generateKeyPair();
        String pem = "-----BEGIN PUBLIC KEY-----\n" +
                java.util.Base64.getEncoder().encodeToString(kp.getPublic().getEncoded()) +
                "\n-----END PUBLIC KEY-----";

        ReactiveJwtDecoder decoder = securityConfig.reactiveJwtDecoder(
                "",
                pem,
                "spendy-auth",
                "spendy-app",
                new DefaultResourceLoader()
        );

        assertNotNull(decoder);
    }

    @Test
    void testReactiveJwtDecoder_EphemeralFallbackWhenNoKeyConfigured() {
        ReactiveJwtDecoder decoder = securityConfig.reactiveJwtDecoder(
                "",
                "",
                "spendy-auth",
                "spendy-app",
                new DefaultResourceLoader()
        );

        assertNotNull(decoder);
    }

    @Test
    void testReactiveJwtDecoder_CreationWithJwkSetUri() {
        ReactiveJwtDecoder decoder = securityConfig.reactiveJwtDecoder(
                "",
                "",
                "http://localhost:8081/rest/auth/jwks.json",
                "spendy-auth",
                "spendy-app",
                new DefaultResourceLoader()
        );

        assertNotNull(decoder);
    }

    @Test
    void testCorsConfigurationSource() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        assertNotNull(source);

        org.springframework.mock.http.server.reactive.MockServerHttpRequest request =
                org.springframework.mock.http.server.reactive.MockServerHttpRequest.get("/Expense/rest/expense/getAll")
                        .header("Origin", "http://localhost:5173")
                        .build();
        org.springframework.mock.web.server.MockServerWebExchange exchange =
                org.springframework.mock.web.server.MockServerWebExchange.from(request);

        CorsConfiguration config = source.getCorsConfiguration(exchange);
        assertNotNull(config);
        assertTrue(Boolean.TRUE.equals(config.getAllowCredentials()));
        assertTrue(config.getAllowedOriginPatterns().contains("*"));
    }
}
