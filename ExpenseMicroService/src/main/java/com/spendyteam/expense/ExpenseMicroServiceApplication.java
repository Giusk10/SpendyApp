package com.spendyteam.expense;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ExpenseMicroServiceApplication {
	public static void main(String[] args) {

		// Carica .env dalla root del progetto (anche se avviato da sottomodulo o IDE)
		String envDir = "./";
		if (!new java.io.File(".env").exists() && new java.io.File("../.env").exists()) {
			envDir = "../";
		}
		Dotenv dotenv = Dotenv.configure()
				.directory(envDir)
				.ignoreIfMissing()
				.load();

		// Stampa di debug per verificare configurazione database
		if (dotenv.get("SPRING_DATASOURCE_URL") == null && dotenv.get("MYSQL_HOST") == null && dotenv.get("MYSQL_USER") == null) {
			System.out.println("ℹ️ Configurazione MySQL da variabili di default/application.properties");
		} else {
			System.out.println("✅ .env caricato correttamente. Configurazione database trovata.");
		}

		dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

		SpringApplication.run(ExpenseMicroServiceApplication.class, args);
	}
}
