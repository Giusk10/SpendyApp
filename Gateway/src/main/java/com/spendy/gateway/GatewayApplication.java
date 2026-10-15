package com.spendy.gateway;

import io.github.cdimascio.dotenv.Dotenv;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Tag(name = "Gateway Status", description = "Gateway root health and status endpoint")
@RestController
@SpringBootApplication(scanBasePackages = "com.spendy.gateway")
public class GatewayApplication {

    public static void main(String[] args) {

        // Carica .env dalla cartella padre (root del progetto)
        Dotenv dotenv = Dotenv.configure()
                .directory("./") // <--- PUNTA ALLA ROOT
                .ignoreIfMissing()
                .load();

        // Stampa di debug per verificare configurazione
        if (dotenv.get("JWT_ISSUER") == null && dotenv.get("AUTH_SERVICE_URI") == null) {
            System.out.println("ℹ️ Configurazione .env non trovata o parziale, verranno usati i default di applicazione.");
        } else {
            System.out.println("✅ .env caricato correttamente per Gateway.");
        }

        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

        SpringApplication.run(GatewayApplication.class, args);
    }

    @Operation(
        summary = "Gateway root status check",
        description = "Lightweight health and welcome endpoint confirming that Spendy Gateway is online."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Gateway is active and running",
        content = @Content(mediaType = "text/plain", schema = @Schema(example = "🟢 Spendy Gateway è attivo e funzionante! (V. Finale)"))
    )
    @GetMapping("/")
    public Mono<String> home() {
        return Mono.just("🟢 Spendy Gateway è attivo e funzionante! (V. Finale)");
    }
}