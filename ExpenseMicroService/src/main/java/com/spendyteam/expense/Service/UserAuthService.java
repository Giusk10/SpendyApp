package com.spendyteam.expense.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Service responsible for authenticating and resolving user identity
 * by interacting with the API Gateway token verification endpoint.
 */
@Service
public class UserAuthService {

    private final WebClient webClient;
    private final String gatewayUri;

    public UserAuthService() {
        this(WebClient.builder().build(), "http://localhost:7860");
    }

    public UserAuthService(WebClient webClient) {
        this(webClient, "http://localhost:7860");
    }

    @Autowired
    public UserAuthService(
            @Autowired(required = false) WebClient webClient,
            @Value("${gateway.uri:http://localhost:7860}") String gatewayUri) {
        this.webClient = webClient != null ? webClient : WebClient.builder().build();
        this.gatewayUri = (gatewayUri != null && !gatewayUri.isBlank()) ? gatewayUri : "http://localhost:7860";
    }

    /**
     * Resolves the username associated with a given JWT token via Gateway REST call.
     *
     * @param token JWT token string
     * @return username if token is valid, or null if invalid, expired, or on failure
     */
    public String getUsernameFromToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

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

    /**
     * Alias for {@link #getUsernameFromToken(String)} for backward compatibility.
     */
    public String getUsernameFromTokenViaRest(String token) {
        return getUsernameFromToken(token);
    }
}
