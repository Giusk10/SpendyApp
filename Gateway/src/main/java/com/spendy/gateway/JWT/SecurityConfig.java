package com.spendy.gateway.JWT;

import com.nimbusds.jose.JWSAlgorithm;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:8080,http://localhost:7860,http://127.0.0.1:7860,http://127.0.0.1:8080,http://127.0.0.1:3000,http://localhost:8081,http://127.0.0.1:8081,http://localhost:8084,http://127.0.0.1:8084}")
    private String corsAllowedOrigins = "http://localhost:3000,http://localhost:8080,http://localhost:7860,http://127.0.0.1:7860,http://127.0.0.1:8080,http://127.0.0.1:3000,http://localhost:8081,http://127.0.0.1:8081,http://localhost:8084,http://127.0.0.1:8084";

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        
        List<String> origins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty() && !s.equals("*"))
                .toList();

        if (origins.isEmpty()) {
            config.setAllowedOrigins(List.of(
                    "http://localhost:3000", "http://localhost:8080", "http://localhost:7860",
                    "http://127.0.0.1:7860", "http://127.0.0.1:8080", "http://127.0.0.1:3000",
                    "http://localhost:8081", "http://127.0.0.1:8081", "http://localhost:8084", "http://127.0.0.1:8084"
            ));
        } else {
            config.setAllowedOrigins(origins);
        }

        config.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*", "https://*.hf.space", "https://*.huggingface.co"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder(
            @Value("${jwt.public-key-location:}") String publicKeyLocation,
            @Value("${jwt.public-key:}") String publicKeyPem,
            @Value("${jwt.jwk-set-uri:}") String jwkSetUri,
            @Value("${jwt.issuer:spendy-auth}") String issuer,
            @Value("${jwt.audience:spendy-app}") String audience,
            @Value("${jwt.allow-ephemeral-keys:false}") boolean allowEphemeralKeys,
            ResourceLoader resourceLoader) {

        NimbusReactiveJwtDecoder decoder;
        if (jwkSetUri != null && !jwkSetUri.isBlank()) {
            decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
        } else {
            RSAPublicKey publicKey = loadPublicKey(publicKeyPem, publicKeyLocation, allowEphemeralKeys, resourceLoader);
            decoder = NimbusReactiveJwtDecoder.withPublicKey(publicKey).build();
        }

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> strictValidator = new DelegatingOAuth2TokenValidator<>(
                withIssuer,
                jwt -> {
                    // Enforce algorithm RS256
                    Object alg = jwt.getHeaders().get("alg");
                    if (alg == null || !JWSAlgorithm.RS256.getName().equalsIgnoreCase(alg.toString())) {
                        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid JWT algorithm: RS256 expected", null));
                    }
                    // Enforce audience
                    List<String> aud = jwt.getAudience();
                    if (aud == null || !aud.contains(audience)) {
                        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid or missing audience", null));
                    }
                    // Enforce subject
                    String sub = jwt.getSubject();
                    if (sub == null || sub.isBlank()) {
                        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Missing or blank subject", null));
                    }
                    return OAuth2TokenValidatorResult.success();
                });
        decoder.setJwtValidator(strictValidator);
        return decoder;
    }

    public ReactiveJwtDecoder reactiveJwtDecoder(
            String publicKeyLocation,
            String publicKeyPem,
            String issuer,
            String audience,
            ResourceLoader resourceLoader) {
        return reactiveJwtDecoder(publicKeyLocation, publicKeyPem, null, issuer, audience, true, resourceLoader);
    }

    public ReactiveJwtDecoder reactiveJwtDecoder(
            String publicKeyLocation,
            String publicKeyPem,
            String jwkSetUri,
            String issuer,
            String audience,
            ResourceLoader resourceLoader) {
        return reactiveJwtDecoder(publicKeyLocation, publicKeyPem, jwkSetUri, issuer, audience, true, resourceLoader);
    }


    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
            ReactiveJwtDecoder reactiveJwtDecoder) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/", "/swagger-ui/**", "/swagger-ui.html",
                                "/v3/api-docs", "/v3/api-docs/**",
                                "/webjars/**", "/favicon.ico")
                        .permitAll()
                        .pathMatchers(HttpMethod.GET,
                                "/Auth/v3/api-docs", "/Auth/v3/api-docs/**",
                                "/Expense/v3/api-docs", "/Expense/v3/api-docs/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/Auth/rest/auth/login", "/Auth/rest/auth/register",
                                "/Auth/rest/auth/refresh")
                        .permitAll()
                        .pathMatchers(HttpMethod.GET, "/Auth/rest/auth/health", "/Auth/rest/auth/jwks.json",
                                "/Auth/rest/auth/.well-known/jwks.json")
                        .permitAll()
                        .pathMatchers(HttpMethod.GET, "/Auth/rest/auth/profile").authenticated()
                        .pathMatchers(HttpMethod.GET, "/Auth/rest/auth/updateProfile").authenticated()
                        .pathMatchers(HttpMethod.PUT, "/Auth/rest/auth/updateProfile").authenticated()
                        .pathMatchers(HttpMethod.POST, "/Auth/rest/auth/logout").authenticated()
                        .pathMatchers("/Expense/**").authenticated()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtDecoder(reactiveJwtDecoder))
                        .authenticationEntryPoint((exchange, ex) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        })
                        .accessDeniedHandler((exchange, ex) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                            return exchange.getResponse().setComplete();
                        }))
                .build();
    }

    private RSAPublicKey loadPublicKey(String pem, String location, boolean allowEphemeralKeys, ResourceLoader resourceLoader) {
        try {
            if ((pem == null || pem.isBlank()) && location != null && !location.isBlank()) {
                pem = readResourceContent(location, resourceLoader);
            }
            if (pem == null || pem.isBlank()) {
                if (!allowEphemeralKeys) {
                    throw new IllegalStateException("CRITICAL SECURITY ERROR: Gateway requires an RSA public key in production " +
                            "(set JWT_PUBLIC_KEY, JWT_PUBLIC_KEY_LOCATION, or JWT_JWK_SET_URI). Ephemeral key fallback is disabled.");
                }
                pem = readResourceContent("file:keys/public_key.pem", resourceLoader);
                if (pem == null || pem.isBlank()) {
                    log.warn("WARNING: No RSA public key configured for Gateway. Using an ephemeral RSA public key because jwt.allow-ephemeral-keys=true.");
                    KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                    kpg.initialize(2048);
                    return (RSAPublicKey) kpg.generateKeyPair().getPublic();
                }
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
        } catch (IllegalStateException ise) {
            throw ise;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RSA public key for Gateway: " + e.getMessage(), e);
        }
    }

    private String readResourceContent(String location, ResourceLoader resourceLoader) {
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
}