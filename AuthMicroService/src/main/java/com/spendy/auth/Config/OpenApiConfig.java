package com.spendy.auth.Config;

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
        title = "Spendy Authentication Microservice API",
        version = "0.0.1-SNAPSHOT",
        description = "Authentication and User Management microservice for SpendyApp. Handles user registration, credentials verification, JWT token issuance, refresh token rotation, and user profile management.",
        contact = @Contact(name = "Spendy Team", url = "https://github.com/Giusk10/SpendyApp")
    ),
    servers = {
        @Server(url = "http://localhost:7860/Auth", description = "Via API Gateway (Default)"),
        @Server(url = "http://localhost:8081", description = "Direct Auth Microservice")
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
