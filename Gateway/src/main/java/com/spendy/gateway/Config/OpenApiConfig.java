package com.spendy.gateway.Config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Spendy API Gateway",
        version = "0.0.1-SNAPSHOT",
        description = "Central reactive API Gateway and reverse proxy for SpendyApp. Exposes token management endpoints, routes client requests to Auth and Expense microservices, enforces JWT security policies, and aggregates OpenAPI specifications.",
        contact = @Contact(name = "Spendy Team", url = "https://github.com/Giusk10/SpendyApp")
    ),
    servers = {
        @Server(url = "http://localhost:7860", description = "API Gateway Server")
    }
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT Bearer token. Enter your access token in the format: <token>"
)
public class OpenApiConfig {
}
