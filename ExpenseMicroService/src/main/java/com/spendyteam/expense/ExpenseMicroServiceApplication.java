package com.spendyteam.expense;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ExpenseMicroServiceApplication {
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

		SpringApplication.run(ExpenseMicroServiceApplication.class, args);
	}
}
