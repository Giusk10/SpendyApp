package com.spendy.gateway.JWT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.web.server.SecurityWebFilterChain;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private JWTAuthenticationFilter jwtAuthenticationFilter;

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig();
    }

    @Test
    void testSecurityConfig_Instantiation() {
        // When & Then
        assertNotNull(securityConfig);
    }

    @Test
    void testSecurityWebFilterChain_Creation() {
        // Given
        org.springframework.security.config.web.server.ServerHttpSecurity http =
            org.springframework.security.config.web.server.ServerHttpSecurity.http();

        // When
        SecurityWebFilterChain filterChain = securityConfig.securityWebFilterChain(http, jwtAuthenticationFilter);

        // Then
        assertNotNull(filterChain);
    }

    @Test
    void testSecurityConfig_NotNull() {
        assertNotNull(securityConfig);
    }

    @Test
    void testCorsConfigurationSource() {
        org.springframework.web.cors.reactive.CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        assertNotNull(source);

        org.springframework.mock.http.server.reactive.MockServerHttpRequest request =
            org.springframework.mock.http.server.reactive.MockServerHttpRequest.get("/Expense/rest/expense/getAll")
                .header("Origin", "http://localhost:5173")
                .build();
        org.springframework.mock.web.server.MockServerWebExchange exchange =
            org.springframework.mock.web.server.MockServerWebExchange.from(request);

        org.springframework.web.cors.CorsConfiguration config = source.getCorsConfiguration(exchange);
        assertNotNull(config);
        org.junit.jupiter.api.Assertions.assertTrue(Boolean.TRUE.equals(config.getAllowCredentials()));
        org.junit.jupiter.api.Assertions.assertTrue(config.getAllowedOriginPatterns().contains("*"));
    }
}
