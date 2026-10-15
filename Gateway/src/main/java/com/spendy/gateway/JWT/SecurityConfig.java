package com.spendy.gateway.JWT;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.addExposedHeader("*");
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder(
            @Value("${jwt.public-key-location:classpath:keys/public_key.pem}") String publicKeyLocation,
            @Value("${jwt.public-key:}") String publicKeyPem,
            @Value("${jwt.issuer:spendy-auth}") String issuer,
            @Value("${jwt.audience:spendy-app}") String audience,
            ResourceLoader resourceLoader) {

        RSAPublicKey publicKey = loadPublicKey(publicKeyPem, publicKeyLocation, resourceLoader);
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withPublicKey(publicKey).build();

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(
                withIssuer,
                jwt -> {
                    List<String> aud = jwt.getAudience();
                    if (aud != null && aud.contains(audience)) {
                        return OAuth2TokenValidatorResult.success();
                    }
                    return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid audience", null));
                }
        );
        decoder.setJwtValidator(withAudience);
        return decoder;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, ReactiveJwtDecoder reactiveJwtDecoder) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/webjars/**", "/favicon.ico").permitAll()
                        .pathMatchers(HttpMethod.GET, "/Auth/v3/api-docs/**", "/Expense/v3/api-docs/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/Auth/rest/auth/login", "/Auth/rest/auth/register", "/Auth/rest/auth/refresh").permitAll()
                        .pathMatchers(HttpMethod.GET, "/Auth/rest/auth/health", "/Auth/rest/auth/jwks.json", "/Auth/rest/auth/.well-known/jwks.json").permitAll()
                        .pathMatchers(HttpMethod.GET, "/Auth/rest/auth/profile").authenticated()
                        .pathMatchers(HttpMethod.GET, "/Auth/rest/auth/updateProfile").authenticated()
                        .pathMatchers(HttpMethod.PUT, "/Auth/rest/auth/updateProfile").authenticated()
                        .pathMatchers("/Auth/**").permitAll()
                        .pathMatchers("/Expense/**").authenticated()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtDecoder(reactiveJwtDecoder))
                        .authenticationEntryPoint((exchange, ex) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        })
                        .accessDeniedHandler((exchange, ex) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                            return exchange.getResponse().setComplete();
                        })
                )
                .build();
    }

    private RSAPublicKey loadPublicKey(String pem, String location, ResourceLoader resourceLoader) {
        try {
            if (pem == null || pem.isBlank()) {
                Resource resource = resourceLoader.getResource(location);
                try (InputStream is = resource.getInputStream()) {
                    pem = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
            String clean = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(clean);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) kf.generatePublic(keySpec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RSA public key for Gateway: " + e.getMessage(), e);
        }
    }
}