package com.spendy.auth;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.spendy.auth")
@ApplicationPath("/rest")
public class AuthMicroServiceApplication extends ResourceConfig
{
    public AuthMicroServiceApplication()
    {
        packages("com/spendy/auth/Controller");
    }

    public static void main(String[] args)
    {
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

        SpringApplication.run(AuthMicroServiceApplication.class, args);
    }
}
