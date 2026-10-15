package com.spendyteam.expense.Config;

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
        title = "Spendy Expense Microservice API",
        version = "0.0.1-SNAPSHOT",
        description = "Expense management and AI smart categorization microservice for SpendyApp. Supports batch CSV expense imports, date/month/year queries, CRUD operations on expenses, and NLP categorization.",
        contact = @Contact(name = "Spendy Team", url = "https://github.com/Giusk10/SpendyApp")
    ),
    servers = {
        @Server(url = "http://localhost:7860/Expense", description = "Via API Gateway (Default)"),
        @Server(url = "http://localhost:8084", description = "Direct Expense Microservice")
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
