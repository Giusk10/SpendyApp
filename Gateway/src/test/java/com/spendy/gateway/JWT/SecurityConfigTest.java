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
}
