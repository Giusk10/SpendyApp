# Guida allo Sviluppo e Contribuzione

Guida operativa per sviluppatori e manutentori di **SpendyApp**, contenente le istruzioni per configurare l'ambiente locale, eseguire build e test con Maven, gestire i container Docker tramite gli script dedicati e consultare i log.

---

## 1. Prerequisiti dell'Ambiente di Sviluppo

Per sviluppare ed eseguire i microservizi di SpendyApp in locale sono necessari i seguenti strumenti:

| Strumento | Versione Minima Richiesta | Comando di Verifica | Scopo |
| :--- | :--- | :--- | :--- |
| **Java Development Kit (JDK)** | **Java 21 LTS** (Eclipse Temurin o OpenJDK) | `java -version` | Compilazione con Virtual Threads (Project Loom) |
| **Apache Maven** | **3.9.x** o superiore | `mvn -version` | Gestione build e dipendenze multi-modulo |
| **Docker & Docker Compose** | Versione recente con supporto Compose V2 | `docker compose version` | Orchestrazione container locale (MySQL e servizi) |
| **OpenSSL** | 1.1.1 o 3.x | `openssl version` | Generazione facoltativa chiavi RSA in locale |
| **cURL** | Qualsiasi versione standard | `curl --version` | Esecuzione dello smoke test end-to-end |

---

## 2. Struttura del Repository

```text
SpendyApp/
├── .env.example                                      # Modello documentato delle variabili d'ambiente
├── .gitignore                                        # File e cartelle esclusi dal controllo di versione
├── .dockerignore                                     # Esclusione secret e artefatti locali dalle immagini Docker
├── Dockerfile                                        # Build multi-stage OCI (auth, expense, gateway, all-in-one)
├── docker-compose.yml                                # Orchestrazione locale multi-container con healthcheck
├── pom.xml                                           # POM Maven padre (Reactor & Dependency Management)
├── README.md                                         # Documentazione e punto di ingresso rapido
├── LICENSE                                           # Licenza GNU AGPLv3
├── THIRD-PARTY-ALL.txt                               # Licenze delle dipendenze di terze parti
├── .github/
│   └── workflows/
│       ├── ci.yml                                    # Pipeline CI (Build, Test, Surefire & JaCoCo Artifacts)
│       └── deploy-huggingface.yml                    # Pipeline CD (Deploy All-in-One su Hugging Face Spaces)
├── db/
│   └── init/
│       └── init-db.sql                               # Inizializzazione schemi MySQL e privilegi utente
├── deploy/
│   └── huggingface/
│       ├── entrypoint.sh                             # Script di avvio All-in-One supervisionato
│       └── README.md                                 # Specifiche architetturali dello Space Hugging Face
├── scripts/
│   ├── dev-up.sh                                     # Avvio automatico dell'ambiente locale con healthcheck
│   ├── dev-down.sh                                   # Arresto dei container preservando i volumi dati
│   ├── dev-logs.sh                                   # Stream in tempo reale dei log dei container
│   └── smoke-test.sh                                 # Smoke test automatizzato degli endpoint
├── docs/                                             # Documentazione tecnica di dettaglio
│   ├── architecture.md                               # Architettura dei microservizi, routing e porte
│   ├── configuration.md                              # Guida completa alle variabili d'ambiente
│   ├── database.md                                   # Schema, Flyway, persistenza e backup
│   ├── deployment.md                                 # Guida CI/CD, Docker Compose e HF Spaces
│   ├── development.md                                # Setup ambiente locale, comandi Maven e script
│   ├── operations.md                                 # Monitoraggio, livelli di salute e manutenzione
│   └── troubleshooting.md                            # Diagnostica problemi comuni e risoluzioni
├── AuthMicroService/                                 # Microservizio Identity e Autenticazione (Porta 8081)
├── ExpenseMicroService/                              # Microservizio Gestione Spese e NLP (Porta 8084)
└── Gateway/                                          # Spring Cloud Gateway Reattivo Ingress (Porta 7860)
```

---

## 3. Avvio Rapido dell'Ambiente Locale

### 3.1. Avvio con Script Automatico (Consigliato)
Lo script [`./scripts/dev-up.sh`](../scripts/dev-up.sh) si occupa di verificare i prerequisiti, inizializzare il file `.env`, generare le chiavi RSA in `keys/` se assenti, compilare e avviare i container, e attendere che il Gateway sia in stato `healthy`:

```bash
./scripts/dev-up.sh
```

Al termine dell'avvio, l'applicazione è accessibile a:
- **API Gateway**: `http://localhost:7860`
- **Swagger UI**: `http://localhost:7860/swagger-ui.html`
- **Gateway Health**: `http://localhost:7860/actuator/health`
- **Auth Health**: `http://localhost:7860/Auth/rest/auth/health`

### 3.2. Verifica con Smoke Test
Per confermare che tutti i microservizi, il database e i filtri di sicurezza siano operativi:

```bash
./scripts/smoke-test.sh
```

### 3.3. Consultazione dei Log
Per visualizzare i log in tempo reale:

```bash
# Tutti i servizi
./scripts/dev-logs.sh

# Solo un servizio specifico
./scripts/dev-logs.sh gateway-service
```

### 3.4. Arresto dell'Ambiente Locale
Per arrestare i servizi preservando i dati nel volume MySQL:

```bash
./scripts/dev-down.sh
```

---

## 4. Compilazione, Test e Build con Maven

SpendyApp sfrutta il reactor del POM padre. Tutti i comandi possono essere eseguiti direttamente dalla radice del repository:

### 4.1. Compilazione
```bash
mvn clean compile
```

### 4.2. Esecuzione Test Unitari e di Integrazione
```bash
# Esegue tutti i 170+ test del progetto
mvn clean test

# Esegue i test di un singolo microservizio
mvn test -pl AuthMicroService
mvn test -pl ExpenseMicroService
mvn test -pl Gateway
```

### 4.3. Verifica Completa e Generazione Report Coverage (JaCoCo)
```bash
# Esegue test, report Surefire e verifica della soglia JaCoCo (minimo 35% a livello BUNDLE)
mvn clean verify
```
I report HTML di JaCoCo vengono generati in:
- `AuthMicroService/target/site/jacoco/index.html`
- `ExpenseMicroService/target/site/jacoco/index.html`
- `Gateway/target/site/jacoco/index.html`

### 4.4. Creazione Pacchetti JAR
```bash
mvn clean package -DskipTests
```
I JAR eseguibili Spring Boot repackaged vengono generati in:
- `AuthMicroService/target/AuthMicroService-0.0.1-SNAPSHOT.jar`
- `ExpenseMicroService/target/ExpenseMicroService-0.0.1-SNAPSHOT.jar`
- `Gateway/target/Gateway-0.0.1-SNAPSHOT.jar`
