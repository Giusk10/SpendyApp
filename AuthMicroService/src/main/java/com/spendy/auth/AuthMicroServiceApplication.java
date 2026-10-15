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

        // Stampa di debug per verificare configurazione database
        if (dotenv.get("SPRING_DATASOURCE_URL") == null && dotenv.get("MYSQL_HOST") == null && dotenv.get("MYSQL_USER") == null) {
            System.out.println("ℹ️ Configurazione MySQL da variabili di default/application.properties");
        } else {
            System.out.println("✅ .env caricato correttamente. Configurazione database trovata.");
        }

        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

        SpringApplication.run(AuthMicroServiceApplication.class, args);
    }
}
