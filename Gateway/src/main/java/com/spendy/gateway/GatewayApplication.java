package com.spendy.gateway;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
@RestController
@SpringBootApplication(scanBasePackages = "com.spendy.gateway")
public class GatewayApplication {

    public static void main(String[] args) {

        // Carica .env dalla cartella padre (root del progetto)
        Dotenv dotenv = Dotenv.configure()
                .directory("./") // <--- PUNTA ALLA ROOT
                .ignoreIfMissing()
                .load();

        // Stampa di debug per essere sicuri che lo trovi
        if (dotenv.get("MONGODB_URI") == null) {
            System.out.println("⚠️ ATTENZIONE: Variabile MONGODB_URI non trovata nel .env o file non letto!");
        } else {
            System.out.println("✅ .env caricato correttamente. URI trovato.");
        }

        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

        SpringApplication.run(GatewayApplication.class, args);
    }

    @GetMapping("/")
    public Mono<String> home() {
        return Mono.just("🟢 Spendy Gateway è attivo e funzionante! (V. Finale)");
    }
}