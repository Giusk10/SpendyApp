package com.spendy.gateway.JWT;

import jakarta.ws.rs.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         JWTAuthenticationFilter jwtAuthenticationFilter) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .cors(cors -> cors.configurationSource(exchange -> {
                    org.springframework.web.cors.CorsConfiguration config = new org.springframework.web.cors.CorsConfiguration();
                    config.addAllowedOrigin("http://localhost:5173");
                    config.addAllowedMethod("*");
                    config.addAllowedHeader("*");
                    config.setAllowCredentials(true);
                    return config;
                }))
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(HttpMethod.POST, "/gateway/generate-token").permitAll()
                        .pathMatchers(HttpMethod.POST, "/gateway/verify-token").permitAll()
                        .pathMatchers("/Auth/**").permitAll()
                        .pathMatchers("/Expense/**").authenticated()
                        .anyExchange().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }


}