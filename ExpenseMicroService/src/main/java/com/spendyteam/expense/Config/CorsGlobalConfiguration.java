package com.spendyteam.expense.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsGlobalConfiguration {

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:8080,http://localhost:7860,http://127.0.0.1:7860,http://127.0.0.1:8080,http://127.0.0.1:3000,http://localhost:8081,http://127.0.0.1:8081,http://localhost:8084,http://127.0.0.1:8084}")
    private String corsAllowedOrigins = "http://localhost:3000,http://localhost:8080,http://localhost:7860,http://127.0.0.1:7860,http://127.0.0.1:8080,http://127.0.0.1:3000,http://localhost:8081,http://127.0.0.1:8081,http://localhost:8084,http://127.0.0.1:8084";

    @Bean
    @Primary
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        List<String> origins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty() && !s.equals("*"))
                .toList();

        if (origins.isEmpty()) {
            corsConfig.setAllowedOrigins(List.of(
                    "http://localhost:3000", "http://localhost:8080", "http://localhost:7860",
                    "http://127.0.0.1:7860", "http://127.0.0.1:8080", "http://127.0.0.1:3000",
                    "http://localhost:8081", "http://127.0.0.1:8081", "http://localhost:8084", "http://127.0.0.1:8084"
            ));
        } else {
            corsConfig.setAllowedOrigins(origins);
        }

        corsConfig.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*", "https://*.hf.space", "https://*.huggingface.co"));
        corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        corsConfig.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        corsConfig.setExposedHeaders(List.of("Authorization"));
        corsConfig.setAllowCredentials(true);
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        return source;
    }

    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource());
    }
}
