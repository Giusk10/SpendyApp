---
title: Spendy
emoji: 🏆
colorFrom: green
colorTo: pink
sdk: docker
pinned: false
license: agpl-3.0
app_port: 7860
---

# Spendy Backend 🏠💸

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.0-6DB33F?style=flat&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring_Cloud_Gateway-Reactive-6DB33F?style=flat&logo=spring&logoColor=white)](https://spring.io/projects/spring-cloud-gateway)
[![MySQL](https://img.shields.io/badge/MySQL-8.4-4479A1?style=flat&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Hibernate](https://img.shields.io/badge/Hibernate-JPA_6-59666C?style=flat&logo=hibernate&logoColor=white)](https://hibernate.org/)
[![Flyway](https://img.shields.io/badge/Flyway-Migrations-CC0200?style=flat&logo=flyway&logoColor=white)](https://flywaydb.org/)
[![Apache OpenNLP](https://img.shields.io/badge/Apache_OpenNLP-2.3.0-D22128?style=flat&logo=apache&logoColor=white)](https://opennlp.apache.org/)
[![Nimbus JOSE](https://img.shields.io/badge/Nimbus-RS256_JWT-blue?style=flat)](https://connect2id.com/products/nimbus-jose-jwt)
[![Docker](https://img.shields.io/badge/Docker-Multi--stage-2496ED?style=flat&logo=docker&logoColor=white)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-AGPLv3-blue.svg)](LICENSE)

**Spendy** è una piattaforma backend ad alte prestazioni basata su microservizi, progettata per la **gestione finanziaria personale** e per la gestione delle spese condivise in famiglie o coinquilini: tracciamento delle spese giornaliere, importazione parallela di estratti conto bancari CSV, analisi aggregata dei flussi di cassa e categorizzazione automatica basata su Machine Learning.

Ingegnerizzato su **Java 21 LTS** e **Spring Boot 3.5**, Spendy utilizza un **API Gateway reattivo basato su Spring WebFlux e Reactor Netty**, **Virtual Threads (Project Loom)**, database relazionali **MySQL 8.4** con **Spring Data JPA** e **Hibernate 6**, migrazioni dello schema versionate con **Flyway**, autenticazione asimmetrica **RSA (RS256)** con **Refresh Token Rotation**, e un motore di Machine Learning locale basato su **Apache OpenNLP**.

---

## 📑 Indice dei Contenuti

- [Documentazione Tecnica Completa](#-documentazione-tecnica-completa)
- [Architettura del Sistema](#-architettura-del-sistema)
- [Scomposizione dei Microservizi](#-scomposizione-dei-microservizi)
  - [API Gateway](#1-api-gateway)
  - [Auth Microservice](#2-auth-microservice)
  - [Expense Microservice](#3-expense-microservice)
- [Motore di Categorizzazione Machine Learning](#-motore-di-categorizzazione-machine-learning)
- [Stack Tecnologico & Librerie](#-stack-tecnologico--librerie)
- [Configurazione delle Variabili d'Ambiente](#-configurazione-delle-variabili-dambiente)
- [Riferimento Rapido API REST](#-riferimento-rapido-api-rest)
- [Installazione e Avvio Locale](#-installazione-e-avvio-locale)
- [Deployment Docker e Containerizzazione](#-deployment-docker-e-containerizzazione)
- [Test e Assicurazione della Qualità](#-test-e-assicurazione-della-qualit%C3%A0)
- [Licenza](#-licenza)

---

## 📚 Documentazione Tecnica Completa

La documentazione approfondita dell'intero sistema è consultabile nella directory [`docs/`](docs/):

| Documento | Descrizione |
| :--- | :--- |
| 🏛️ [Architettura del Sistema](docs/architecture.md) | Stile architetturale, componenti, flussi sincroni/asincroni, confini dati e diagrammi Mermaid. |
| ⚙️ [Configurazione & Variabili](docs/configuration.md) | Elenco completo variabili d'ambiente, differenze tra dev e prod, gestione segreti e fail-fast. |
| 🗄️ [Database & Migrazioni](docs/database.md) | Schemi `spendy_auth`/`spendy_expense`, inizializzazione `init-db.sql`, Flyway, backup e restore. |
| 💻 [Guida allo Sviluppo](docs/development.md) | Setup ambiente locale, struttura directory, comandi Maven reactor e script di gestione. |
| 🚀 [Deployment & CI/CD](docs/deployment.md) | Pipeline GitHub Actions separate (CI & CD), Hugging Face Spaces, Docker Compose e rollback. |
| 🩺 [Operazioni & Monitoraggio](docs/operations.md) | Livelli di health check, smoke test, ispezione dei log e manutenzione ordinaria. |
| 🔧 [Risoluzione Problemi (Troubleshooting)](docs/troubleshooting.md) | Diagnostica problemi comuni, cause probabili e comandi di risoluzione. |
| 🔌 [Specifiche API REST](docs/api.md) | Contratti di tutti gli endpoint, metodi HTTP, parametri, schemi di risposta ed esempi JSON. |
| 🔐 [Autenticazione & Sessioni](docs/authentication.md) | Crittografia asimmetrica RS256, BCrypt, Refresh Token Rotation, hashing SHA-256 e rate limiting. |
| 🗃️ [Modello Dati & Persistenza](docs/data-model.md) | Schemi relazionali, diagramma ER, indici e vincoli di integrità. |
| 🧪 [Strategia di Test & JaCoCo](docs/testing.md) | Suite di 354 test, integration test con database H2 in memoria e metriche JaCoCo. |
| 🛡️ [Analisi di Sicurezza & Threat Model](docs/security.md) | Superfici d'attacco, mitigazione timing attack, protezione IDOR, CORS e gestione segreti. |
| 📜 [Architecture Decision Records (ADR)](docs/decisions/0001-migrazione-mysql-flyway.md) | Decisioni architetturali motivate (MySQL, RSA RS256, Gateway reattivo, OpenNLP, Token Rotation). |
| 📋 [Changelog Completo](CHANGELOG.md) | Cronologia ufficiale dei rilasci e commit significativi ricostruita dalla storia Git. |

---

## 🏗 Architettura del Sistema

Spendy adotta un'architettura a microservizi asincrona e cloud-native protetta da un reverse proxy perimetrale reattivo (**API Gateway**). Il Gateway gestisce l'autenticazione perimetrale OAuth2, la validazione crittografica della firma JWT, le policy CORS globali, la riscrittura dei percorsi verso i servizi interni e la sanitizzazione degli header.

```mermaid
flowchart TD
    Client["Client Applications<br/>(iOS App / Web Client)"]
    
    subgraph Edge ["Edge Layer"]
        Gateway["Spring Cloud Gateway<br/>(Port: 7860 / 8080)<br/>- Reactive Netty WebFlux<br/>- Virtual Threads (Java 21)<br/>- RS256 JWT Verification<br/>- CORS & Security Policy"]
    end

    subgraph Microservices ["Microservices Layer (Internal Network)"]
        AuthService["AuthMicroService<br/>(Port: 8081)<br/>- Spring Boot & Jersey<br/>- BCrypt Password Hashing (Cost 12)<br/>- RS256 Asymmetric JWT Signer<br/>- Refresh Token Rotation (SHA-256)"]
        ExpenseService["ExpenseMicroService<br/>(Port: 8084)<br/>- Spring Boot MVC<br/>- Parallel CSV Parser (Virtual Threads)<br/>- OpenNLP ML Categorizer<br/>- Throttled Batch Writer (Semaphore)"]
    end

    subgraph DataStore ["Data Layer (MySQL 8.4)"]
        AuthDB[("MySQL: spendy_auth<br/>(users, refresh_tokens)")]
        ExpenseDB[("MySQL: spendy_expense<br/>(expenses)")]
    end

    Client -->|HTTP / HTTPS| Gateway
    Gateway -->|Rewrite: /Auth/** &rarr; /rest/auth/**| AuthService
    Gateway -->|Rewrite: /Expense/** &rarr; /rest/expense/**| ExpenseService
    AuthService --> AuthDB
    ExpenseService --> ExpenseDB
```

---

## 🧩 Scomposizione dei Microservizi

### 1. API Gateway
- **Directory**: `Gateway/`
- **Porta**: `7860` (Produzione / Hugging Face Spaces) o `8080` (Fallback locale)
- **Tecnologie**: Spring Cloud Gateway Server WebFlux, Reactor Netty, Java 21 Virtual Threads.
- **Funzionalità Chiave**:
  - Routing non bloccante reattivo verso i microservizi interni.
  - Verifica perimetrale dei token JWT Bearer tramite chiave pubblica RSA (RS256) o endpoint JWKS RFC 7517.
  - Riscrittura trasparente dei percorsi (`/Auth/**` e `/Expense/**`).
  - Sanificazione preventiva degli header con rimozione di `X-Auth-User` e `X-User-Id` per bloccare attacchi di spoofing.
  - Deduplica degli header CORS verso i client Web.
  - Swagger UI aggregata (`/swagger-ui.html`) con le specifiche OpenAPI unificate di tutti i microservizi.

### 2. Auth Microservice
- **Directory**: `AuthMicroService/`
- **Porta**: `8081`
- **Tecnologie**: Spring Boot 3.5, Jersey 3.x, Spring Data JPA, Hibernate 6, Flyway, jBCrypt, Nimbus JOSE.
- **Funzionalità Chiave**:
  - Registrazione utenti con validazione regex di email e username e hashing password con **BCrypt (costo 12)**.
  - Login con mitigazione dei timing attack tramite hash BCrypt fittizio per contrastare l'enumerazione utenti.
  - Emissione di Access Token JWT firmati con chiave privata RSA a 2048 bit (durata: 15 minuti).
  - Refresh Token Rotation atomica con rilevamento del riutilizzo (`family_id`), revoca a catena e salvataggio nel database unicamente del digest **SHA-256**.
  - Filtro [RateLimitingFilter](AuthMicroService/src/main/java/com/spendy/auth/Security/RateLimitingFilter.java) con verifica rigorosa dei proxy fidati.
  - Endpoint pubblico RFC 7517 `/rest/auth/jwks.json`.

### 3. Expense Microservice
- **Directory**: `ExpenseMicroService/`
- **Porta**: `8084`
- **Tecnologie**: Spring Boot 3.5, Spring Data JPA, Hibernate 6, Apache OpenNLP 2.3.0, OpenCSV 5.9, Virtual Threads.
- **Funzionalità Chiave**:
  - **Smart CSV Parser**: Riconoscimento automatico del delimitatore (virgola, punto e virgola, tabulatore) ed elaborazione parallela delle righe tramite Virtual Threads di Java 21.
  - **Batch Persistence Throttling**: Scrittura a lotti di 500 record disciplinata da `Semaphore(10)` per salvaguardare il connection pool MySQL HikariCP.
  - **Classificazione AI Dual-Layer**: Predizione della categoria di spesa con Machine Learning OpenNLP e fallback euristico deterministico.
  - **Aggregazioni Finanziarie SQL**: Calcolo dei totali mensili e annuali push-down a livello database con query native `DATE_FORMAT`.
  - **CRUD Completo & Prevenzione IDOR**: Creazione, consultazione, aggiornamento e cancellazione sicura con verifica formale della proprietà del record.

---

## 🤖 Motore di Categorizzazione Machine Learning

Le transazioni finanziarie vengono classificate tramite un'architettura **ibrida a due stadi** integrata in `ExpenseMicroService`:

```mermaid
flowchart LR
    Input["Descrizione Transazione<br/>(es. 'PAGAMENTO POS CONAD')"] --> Preprocess["Tokenizer & Lowercase Normalizer"]
    Preprocess --> OpenNLP{"Modello OpenNLP<br/>(expense-model.bin)?"}
    OpenNLP -- Disponibile --> MaxEnt["Classificatore Maximum Entropy<br/>(DocumentCategorizerME)"]
    OpenNLP -- Errore / Assente --> Fallback["Fallback Euristico Deterministico<br/>(ExpenseClassifier)"]
    MaxEnt --> Category["Categoria Assegnata<br/>(es. 'Supermercati e Alimentari')"]
    Fallback --> Category
```

1. **Modello Machine Learning (`SmartCategorizerService`)**:
   - Algoritmo di classificazione a Massima Entropia (**MaxEnt**) addestrato tramite [ExpenseModelTrainer.java](ExpenseMicroService/src/main/java/com/spendyteam/expense/Training/ExpenseModelTrainer.java).
   - Esecuzione locale in microsecondi senza trasmissione di dati riservati a server o LLM esterni (GDPR compliant).
2. **Dizionario Euristico di Fallback (`ExpenseClassifier`)**:
   - Assicura la classificazione anche in caso di formati anomali per macro-categorie: *Supermercati e Alimentari*, *Ristorazione e Bar*, *Abbonamenti e Servizi Digitali*, *Trasporti*, *Carburante e Auto*, *Alloggi e Viaggi*, *Shopping e Abbigliamento*, *Pagamenti e Trasferimenti*.

---

## 🛠 Stack Tecnologico & Librerie

| Categoria | Tecnologia | Descrizione |
| :--- | :--- | :--- |
| **Linguaggio** | Java 21 LTS | Utilizzo di Virtual Threads (Project Loom) e feature moderne del linguaggio |
| **Framework** | Spring Boot 3.5.0 | Runtime modulare, dependency injection e container applicativo |
| **API Gateway** | Spring Cloud Gateway | Reverse proxy reattivo asincrono su Spring WebFlux e Reactor Netty |
| **REST APIs** | Spring Web & Jersey JAX-RS | Gestione endpoint REST, DTO e validazione |
| **Database** | MySQL 8.4 LTS | Database relazionale con schemi isolati (`spendy_auth`, `spendy_expense`) |
| **ORM / Persistenza** | Spring Data JPA & Hibernate 6 | Mappatura relazionale, connection pool HikariCP, batch inserts a 500 record |
| **Schema Migrations** | Flyway | Migrazioni DDL versionate e automatiche all'avvio (`db/migration/`) |
| **Crittografia & JWT** | Nimbus JOSE+JWT & Spring OAuth2 | Firma e verifica asimmetrica RS256 con supporto Key ID (`kid`) e JWKS |
| **Hashing Credenziali** | jBCrypt 0.4 (Cost 12) | Hashing salted delle password con verifica a tempo costante |
| **Machine Learning** | Apache OpenNLP 2.3.0 | Classificatore documentale Maximum Entropy per categorizzazione spese |
| **CSV Processing** | OpenCSV 5.9 | Parser delimitato con auto-rilevamento separatori |
| **Build System** | Maven 3.9+ & Maven Wrapper | Gestione multi-modulo coordinata e plugin `license-maven-plugin` |
| **Testing** | JUnit 5, Mockito, H2, JaCoCo | Suite di 354 test unitari, test di sicurezza e metriche di copertura |
| **Containerizzazione** | Docker & Docker Compose | Immagine OCI multi-stage rootless (`user:1000`) e compose distribuito |

---

## ⚙️ Configurazione delle Variabili d'Ambiente

SpendyApp carica le configurazioni tramite il file `.env` posizionato nella root o mediante variabili d'ambiente di sistema:

```bash
cp .env.example .env
```

Configurazione essenziale:
```env
# Database MySQL
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_USER=spendy
MYSQL_PASSWORD=spendy_user_secret_change_in_prod
MYSQL_ROOT_PASSWORD=spendy_root_secret_change_in_prod
MYSQL_DATABASE_AUTH=spendy_auth
MYSQL_DATABASE_EXPENSE=spendy_expense

# JWT RS256 Asimmetrico
JWT_ISSUER=spendy-auth
JWT_AUDIENCE=spendy-app
JWT_EXPIRATION=900000
JWT_ALLOW_EPHEMERAL_KEYS=false
JWT_ACTIVE_KEY_ID=spendy-auth-key-1

# In produzione, specificare le chiavi RSA tramite PEM:
# JWT_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----"
# JWT_PUBLIC_KEY="-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----"

# Routing dei Microservizi
AUTH_SERVICE_URI=http://localhost:8081
EXPENSE_SERVICE_URI=http://localhost:8084
```

*Per la descrizione dettagliata di tutte le variabili supportate, consultare la [Guida al Deployment](docs/deployment.md#2-tabella-completa-delle-variabili-dambiente).*

---

## 🔌 Riferimento Rapido API REST

Tutte le chiamate dall'esterno devono essere inviate verso l'**API Gateway**:
- **URL Locale**: `http://localhost:7860` (o `http://localhost:8080`)
- **Documentazione Swagger UI**: `http://localhost:7860/swagger-ui.html`

### Endpoint di Autenticazione (`/Auth/rest/auth/`)
| Metodo | Endpoint | Autenticazione | Descrizione |
| :--- | :--- | :--- | :--- |
| `POST` | `/Auth/rest/auth/register` | No | Registrazione nuovo utente (rate limit: 5/min). |
| `POST` | `/Auth/rest/auth/login` | No | Login con username o email (rate limit: 10/min). |
| `POST` | `/Auth/rest/auth/refresh` | No | Rotazione del refresh token con verifica contro il riutilizzo. |
| `GET` | `/Auth/rest/auth/profile` | `Bearer <token>` | Recupero dati anagrafici del profilo utente. |
| `PUT` | `/Auth/rest/auth/updateProfile` | `Bearer <token>` | Aggiornamento nome e cognome utente. |
| `POST` | `/Auth/rest/auth/logout` | No | Invalida il refresh token e revoca l'intera sessione. |
| `GET` | `/Auth/rest/auth/jwks.json` | No | Set di chiavi pubbliche RSA RFC 7517. |
| `GET` | `/Auth/rest/auth/health` | No | Controllo stato operativo del servizio Auth. |

### Endpoint Gestione Spese (`/Expense/rest/expense/`)
*Tutti gli endpoint di spesa richiedono l'header `Authorization: Bearer <accessToken>`.*

| Metodo | Endpoint | Descrizione |
| :--- | :--- | :--- |
| `POST` | `/Expense/rest/expense/import` | Importazione estratto conto CSV in parallelo con auto-categorizzazione AI. |
| `GET` | `/Expense/rest/expense/getExpenses` | Lista delle spese dell'utente (ordinamento cronologico deterministico, supporto parametri di paginazione `page` e `size`). |
| `POST` | `/Expense/rest/expense/getExpenseByDate` | Ricerca spese comprese tra `startedDate` e `completedDate`. |
| `POST` | `/Expense/rest/expense/getExpenseByMonth` | Filtro spese per mese (`month`) e anno (`year`). |
| `POST` | `/Expense/rest/expense/getMonthlyAmountOfYear` | Aggregazione totale spese mese per mese per un dato anno (`year`). |
| `POST` | `/Expense/rest/expense/addExpense` | Aggiunta singola transazione (assegnazione automatica categoria AI). |
| `POST` | `/Expense/rest/expense/updateExpense` | Modifica puntuale di una spesa esistente. |
| `DELETE`| `/Expense/rest/expense/deleteExpense` | Cancellazione singola spesa previa verifica di proprietà. |
| `DELETE`| `/Expense/rest/expense/deleteAllExpenses` | Cancellazione massiva dell'intero storico spese dell'utente. |

*Per gli schemi completi dei payload e codici di risposta, consultare la [Guida API REST](docs/api.md).*

---

## 🚀 Avvio Rapido e Comandi di Sviluppo

### Prerequisiti
- **Docker & Docker Compose** (raccomandato per avviare l'intero stack isolato)
- **Java 21 LTS** (Maven Wrapper `./mvnw` è incluso nel repository per build senza dipendenze globali)

### 1. Avvio dell'Ambiente Locale
Per preparare le variabili d'ambiente (copia automatica da `.env.example`), generare le chiavi RSA locali, avviare MySQL e tutti i microservizi con health check:
```bash
./scripts/dev-up.sh
```
L'applicazione è operativa su **`http://localhost:7860`** (Swagger UI aggregata: `http://localhost:7860/swagger-ui.html`).

### 2. Esecuzione Smoke Test
Per verificare che tutti gli endpoint chiave, il routing e le policy di sicurezza rispondano correttamente:
```bash
./scripts/smoke-test.sh
```

### 3. Consultazione dei Log
```bash
./scripts/dev-logs.sh               # Tutti i servizi in streaming
./scripts/dev-logs.sh gateway-service # Solo API Gateway
```

### 4. Arresto dell'Ambiente Locale
Per arrestare i servizi preservando i dati nel volume MySQL:
```bash
./scripts/dev-down.sh
```

---

## 🧪 Esecuzione dei Test e Build Maven

La suite comprende **382 test** automatizzati unitari, di sicurezza, di integrazione e benchmark prestazionali (164 in `AuthMicroService`, 23 in `Gateway`, 195 in `ExpenseMicroService` con database H2 in memoria):

```bash
# Esecuzione di tutti i test (tramite Maven Wrapper)
./mvnw clean test

# Verifica completa con controllo soglia JaCoCo e generazione report
./mvnw clean verify
```

I report di copertura del codice JaCoCo vengono generati nei percorsi:
- `AuthMicroService/target/site/jacoco/index.html`
- `ExpenseMicroService/target/site/jacoco/index.html`
- `Gateway/target/site/jacoco/index.html`

Per la guida completa e il flusso di contribuzione, consultare la [Guida allo Sviluppo](docs/development.md) e la [Guida al Deployment](docs/deployment.md).

*Dettagli approfonditi sono disponibili nella [Guida al Testing](docs/testing.md).*

---

## 📄 Licenza

Questo progetto è rilasciato sotto licenza **GNU Affero General Public License v3.0 (AGPLv3)**.  
Consultare il file [LICENSE](LICENSE) per i termini completi della licenza.

I riconoscimenti e le note di licenza delle librerie di terze parti sono aggregati automaticamente nel file [THIRD-PARTY-ALL.txt](THIRD-PARTY-ALL.txt).
