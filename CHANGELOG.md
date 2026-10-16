# Changelog di SpendyApp

Tutte le modifiche rilevanti apportate al progetto SpendyApp sono documentate in questo file, ricostruite fedelmente dalla cronologia dei commit di Git.

Il formato è ispirato a [Keep a Changelog](https://keepachangelog.com/it/1.1.0/).

---

## [Non Rilasciato / Milestone Security & Hardening] - 2026-10-16

### Sicurezza e Crittografia
- **Rafforzamento Validazione JWT e Divieto Chiavi Effimere in Produzione (`a90e372`)**:
  - Bloccato l'avvio in modalità produzione se `jwt.allow-ephemeral-keys=true` o in assenza di chiavi RSA esplicitamente configurate.
  - Implementata la verifica rigorosa dei claim obbligatori (`exp`, `iss`, `aud`, `sub`) e dell'algoritmo `RS256`.
  - Estesa la suite di test di sicurezza ([JwtSecurityHardeningTest](file:///Users/gius03/Progetti/SpendyProject/SpendyApp/AuthMicroService/src/test/java/com/spendy/auth/Security/JwtSecurityHardeningTest.java), [DeploymentSecurityHardeningTest](file:///Users/gius03/Progetti/SpendyProject/SpendyApp/AuthMicroService/src/test/java/com/spendy/auth/Security/DeploymentSecurityHardeningTest.java)).
- **Generazione Automatica Chiavi RSA per Sviluppo e Origini CORS (`198dcf5`)**:
  - Aggiunta generazione automatica di chiavi RSA a 2048 bit in `keys/` in ambiente dev/test quando consentito.
  - Migliorata la risoluzione del file `.env` sia dalla root che dalle sottodirectory dei moduli.
  - Aggiornata la lista delle origini CORS consentite per includere domini locali e Hugging Face Spaces.
- **Security Hardening e Autenticazione JWT sui Microservizi (`271b17b`)**:
  - Implementato [RateLimitingFilter](file:///Users/gius03/Progetti/SpendyProject/SpendyApp/AuthMicroService/src/main/java/com/spendy/auth/Security/RateLimitingFilter.java) con protezione anti-bruteforce su login, registrazione e refresh token.
  - Implementata Refresh Token Rotation con rilevamento del riutilizzo (`family_id`) e revoca immediata a catena.
  - Archiviazione sicura dei refresh token tramite hash SHA-256 e azzeramento del campo plaintext (migrazione Flyway `V3`).
  - Mitigazione dei timing attack durante il login tramite valutazione in tempo costante con hash BCrypt fittizio (`DUMMY_BCRYPT_HASH`).
- **Supporto Configurazione Dinamica Chiavi JWT (`e346d8a`, `3029d3a`)**:
  - Supporto per iniezione di chiavi RSA via variabili d'ambiente (`JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEY`) o percorsi file montati (`JWT_PRIVATE_KEY_LOCATION`, `JWT_PUBLIC_KEY_LOCATION`).
  - Rimozione di file PEM statici con chiavi private dal version control.
- **Migrazione ad Asimmetrico RSA JWT (`f2d9c41`)**:
  - Transizione da firma simmetrica HMAC (HS256) a crittografia asimmetrica RSA (RS256).
  - Esposizione dell'endpoint standard RFC 7517 `/rest/auth/jwks.json`.
  - Aggiunto supporto a rotazione delle chiavi tramite `kid` e `previous-public-keys`.

### Refactoring e Manutenzione
- Pulizia classi di configurazione obsolete e dipendenze WebFlux superflue nei moduli servlet (`cc75b45`).
- Rimozione di test di performance dismessi e vecchi script di migrazione (`1eeb583`, `3526a5c`).
- Pulizia DTO obsoleti, dead code e metadata IDE (`4ad3bd6`).
- Aggiunta costruttore predefinito a `JwtTokenService` e miglioramento log di avvio (`4029cf3`).
- Configurazione database in-memory H2 per test di integrazione JPA in ambiente CI (`fe6aa7b`).

---

## [Milestone Persistenza MySQL & OpenAPI] - 2026-10-15

### Aggiunte e Modifiche
- **Migrazione Persistenza da MongoDB a MySQL 8.x (`95abbe8`)**:
  - Sostituzione di Spring Data MongoDB con Spring Data JPA e Hibernate 6.
  - Introduzione di Flyway per le migrazioni automatiche dello schema relazionale (`V1__create_auth_schema.sql`, `V1__create_expense_schema.sql`).
  - Configurazione connection pool HikariCP con dimensione massima 20 connessioni per servizio.
- **OpenAPI 3.0 e Swagger UI (`c629b1d`)**:
  - Integrazione di `springdoc-openapi-starter-webmvc-ui` e `springdoc-openapi-starter-webflux-ui`.
  - Configurazione Swagger UI aggregata su Gateway (`/swagger-ui.html`) unificando Auth, Expense e Gateway.
- **Configurazione CORS Globale (`3246f22`)**:
  - Configurazione unificata dei filtri CORS e impostazione dei server OpenAPI predefiniti per tutti i servizi.
- **Rifattorizzazione Componenti ExpenseMicroService (`9cb2653`)**:
  - Separazione delle responsabilità in componenti dedicati: [ExpenseCsvParser](file:///Users/gius03/Progetti/SpendyProject/SpendyApp/ExpenseMicroService/src/main/java/com/spendyteam/expense/Service/ExpenseCsvParser.java), [ExpenseBatchWriter](file:///Users/gius03/Progetti/SpendyProject/SpendyApp/ExpenseMicroService/src/main/java/com/spendyteam/expense/Service/ExpenseBatchWriter.java), [ExpenseDataParser](file:///Users/gius03/Progetti/SpendyProject/SpendyApp/ExpenseMicroService/src/main/java/com/spendyteam/expense/Utility/ExpenseDataParser.java).
  - Aggiunti unit test e suite di verifica.
- **Centralizzazione Dipendenze Maven (`19db235`)**:
  - Centralizzazione delle versioni nel `<dependencyManagement>` del POM radice e pulizia delle dipendenze duplicate.

---

## [Milestone Categorizzazione NLP & Virtual Threads] - 2026-01-30 / 2026-02-03

### Aggiunte e Ottimizzazioni
- **Categorizzazione Intelligente OpenNLP (`778aa9e`, `9e2a59e`, `47c258c`)**:
  - Implementazione di [SmartCategorizerService](file:///Users/gius03/Progetti/SpendyProject/SpendyApp/ExpenseMicroService/src/main/java/com/spendyteam/expense/Service/SmartCategorizerService.java) basato su Apache OpenNLP Maximum Entropy document classification.
  - Aggiunto [ExpenseModelTrainer](file:///Users/gius03/Progetti/SpendyProject/SpendyApp/ExpenseMicroService/src/main/java/com/spendyteam/expense/Training/ExpenseModelTrainer.java) per l'addestramento offline del modello binario `expense-model.bin`.
  - Fallback deterministico a regole implementato in [ExpenseClassifier](file:///Users/gius03/Progetti/SpendyProject/SpendyApp/ExpenseMicroService/src/main/java/com/spendyteam/expense/Utility/ExpenseClassifier.java).
- **Importazione Parallela CSV con Virtual Threads (`2f16afa`, `1b91800`)**:
  - Parsing concorrente delle righe CSV tramite Virtual Threads di Java 21 (`Executors.newVirtualThreadPerTaskExecutor()`).
  - Scrittura a lotti di 500 record controllata da `Semaphore(10)` per proteggere il database.
  - Auto-rilevamento delimitatori CSV (virgola, punto e virgola, tabulatore).
- **Integrazione JaCoCo & Pipeline CI/CD (`d9a4dc2`, `40aea10`)**:
  - Configurazione del plugin `jacoco-maven-plugin` per il monitoraggio della code coverage.
  - Workflow GitHub Actions per l'esecuzione automatica dei test e sincronizzazione con Hugging Face Spaces.
- **Licenze di Terze Parti (`01fb24b`, `7e46ff9`)**:
  - Configurazione di `license-maven-plugin` e generazione automatica del report di licenze `THIRD-PARTY-ALL.txt`.

---

## [Milestone Iniziale & Microservizi Base] - 2026-01-03 / 2026-01-29

### Aggiunte
- Strutturazione del progetto multi-modulo Maven (AuthMicroService, ExpenseMicroService, Gateway).
- Implementazione endpoint di autenticazione di base (registrazione, login, refresh token, profilo utente).
- Implementazione endpoint di spesa (importazione CSV, lista spese, filtri per data/mese/anno, inserimento e cancellazione).
- Containerizzazione Docker con build multi-stage e script [entrypoint.sh](file:///Users/gius03/Progetti/SpendyProject/SpendyApp/entrypoint.sh) per il deployment All-in-One.
- Configurazione di Spring Cloud Gateway su porta `7860` con logging reattivo Netty.
