package com.spendy.gateway.JWT;


import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Component
public class JWTAuthenticationFilter extends AuthenticationWebFilter {

    public JWTAuthenticationFilter(TokenManager tokenManager) {
        super(new JWTReactiveAuthenticationManager(tokenManager));
        this.setServerAuthenticationConverter(exchange -> extractAuthentication(exchange.getRequest(), tokenManager));
    }

    private Mono<Authentication> extractAuthentication(ServerHttpRequest request, TokenManager tokenManager) {
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            String username = tokenManager.verifyToken(token);
            if (username != null) {
                // Passa il token come credentials, non null
                return Mono.just(new UsernamePasswordAuthenticationToken(username, token, Collections.emptyList()));
            }
        }
        return Mono.empty();
    }

}