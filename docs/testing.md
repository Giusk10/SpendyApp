# Strategia di Test e Assicurazione della Qualità

Documentazione tecnica della strategia di testing, dei framework utilizzati, dei livelli di verifica e dei risultati di copertura del codice (**JaCoCo**) in **SpendyApp**.

---

## 1. Strategia e Livelli di Test

La suite di test di SpendyApp garantisce la correttezza logica, l'integrità dei dati e la robustezza crittografica attraverso tre livelli distinti:

```mermaid
flowchart TD
    subgraph TestPyramid ["Piramide dei Test"]
        Unit["1. Unit Tests (Isolati & Veloci)<br/>- Entità (User, Expense, RefreshToken)<br/>- Parsers (ExpenseDataParser, ExpenseCsvParser)<br/>- Utility & DTOs<br/>- Servizio di classificazione ML & fallback"]
        Security["2. Security & Hardening Tests<br/>- Validazione crittografica JWT RS256<br/>- Reiezione algoritmi non autorizzati (none, HS256)<br/>- Protezione Brute-Force (RateLimitingFilter)<br/>- Refresh Token Rotation & Rilevamento Riutilizzo<br/>- Blocco chiavi effimere in produzione"]
        Persistence["3. JPA Integration Tests (H2 in-memory)<br/>- Verifica query JPQL e Native SQL<br/>- Locking pessimistico (PESSIMISTIC_WRITE)<br/>- Vincoli di unicità e cancellazione a cascata (ON DELETE CASCADE)"]
    end
```

---

## 2. Framework e Strumenti Utilizzati

| Framework / Libreria | Versione | Ambito di Utilizzo |
| :--- | :--- | :--- |
| **JUnit 5 (Jupiter)** | 5.11.x (Spring Boot 3.5 BOM) | Esecuzione dei test unitari e di integrazione |
| **Mockito & Mockito Inline** | 5.2.0 | Creazione di mock per repository, servizi Web e token |
| **Spring Boot Starter Test** | 3.5.0 | Contesto di test Spring, iniezione delle dipendenze per slice test |
| **Spring Security Test** | 6.x | Simulazione di contesti di sicurezza `SecurityContext` e token Bearer |
| **Reactor Test** | 3.6.x | Test reattivi non bloccanti per le route e la sicurezza del Gateway WebFlux |
| **H2 Database** | 2.3.x | Motore di database in memoria con compatibilità MySQL (`MODE=MySQL`) |
| **JaCoCo Maven Plugin** | 0.8.12 | Generazione dei report di copertura e verifica delle metriche di istruzione |

---

## 3. Risultati della Suite di Test e Metriche di Copertura

La suite di test automatizzata conta **382 test totali** (164 in `AuthMicroService`, 23 in `Gateway`, 195 in `ExpenseMicroService`), tutti superati con successo con 0 fallimenti, 0 errori e 0 test disabilitati:

```text
[INFO] Reactor Summary for spendy 0.0.1-SNAPSHOT:
[INFO] spendy ............................................. SUCCESS
[INFO] AuthMicroService ................................... SUCCESS [164 tests]
[INFO] Gateway ............................................ SUCCESS [ 23 tests]
[INFO] ExpenseMicroService ................................ SUCCESS [195 tests]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] Tests run: 382, Failures: 0, Errors: 0, Skipped: 0
```

### Copertura del Codice JaCoCo e Nuove Suite Aggiunte:
Le verifiche di copertura JaCoCo sono pienamente soddisfatte su tutti i moduli (`mvn verify`).
Tra le nuove classi di test aggiunte a copertura delle modifiche di affidabilità e performance:
- `ExpenseCsvParserExtendedTest`: validazione parsing CSV con differenti delimitatori (`,`, `;`), quote escaping, date invalide, importi malformati, conservazione deterministica dell'ordine originale tramite indicizzazione concorrente su Virtual Threads, e applicazione dei limiti dimensionali (max rows e max bytes).
- `ExpenseServiceExtendedTest`: verifica gestione transazionale e fallimenti batch parziali (HTTP 207 Multi-Status) e totali (HTTP 500), deduplicazione idempotente per chiave composta (`username + date + amount + description`), paginazione deterministica con limiti massimi di sicurezza (500 per pagina), e confini inclusivi temporali delle query di aggregazione.
- `GlobalExceptionHandlerTest`: verifica delle risposte di errore standardizzate, codici HTTP corretti (400, 401, 403, 404, 405, 500) e assenza di leak di stack trace o percorsi interni.
- `CorrelationIdFilterTest` (Gateway, AuthMicroService, ExpenseMicroService): propagazione end-to-end dell'header `X-Correlation-ID`, valorizzazione automatica via UUID se assente, e associazione corretta al contesto MDC di SLF4J.
- `ExpenseBenchmarkAndConcurrencyTest`: benchmark prestazionali riproducibili su 1.000, 10.000 e 50.000 righe CSV, oltre a test di importazione concorrente su pool di thread multipli (5 thread simultanei).

### Metriche Prestazionali Misurate:
- **1.000 righe CSV**: ~29.65 ms totali (Parse: 29.53 ms, Persistenza Batch: 0.12 ms) &rarr; **~33.729 righe/sec**, allocazione heap contenuta (~25 MB delta).
- **10.000 righe CSV**: ~267.92 ms totali (Parse: 267.37 ms, Persistenza Batch: 0.55 ms) &rarr; **~37.324 righe/sec**, heap delta ~18 MB.
- **50.000 righe CSV**: ~775.16 ms di parsing &rarr; **~64.502 righe/sec**, heap delta ~148 MB.
- **Concorrenza (5 thread simultanei &times; 1.000 righe = 5.000 righe totali)**: ~239.53 ms &rarr; **~20.873 righe/sec** aggregate.

---

## 4. Test di Sicurezza Specifici (Security Hardening Test Suite)

Il codebase include test mirati alla verifica puntuale delle difese di sicurezza:

1. **[JwtSecurityHardeningTest.java](../AuthMicroService/src/test/java/com/spendy/auth/Security/JwtSecurityHardeningTest.java)**:
   - Rifiuto categorico di token con algoritmo `none`.
   - Rifiuto di token con firma manomessa o calcolata con algoritmo simmetrico HS256 non autorizzato.
   - Rigetto immediato di token con claim obbligatori mancanti (`exp`, `iss`, `aud`, `sub`).
   - Rifiuto di token scaduti anche per 1 solo secondo.
2. **[RateLimitingFilterSecurityTest.java](../AuthMicroService/src/test/java/com/spendy/auth/Security/RateLimitingFilterSecurityTest.java)**:
   - Verifica del blocco al raggiungimento dell'11° tentativo di login nello stesso minuto.
   - Calcolo accurato dell'header HTTP `Retry-After`.
   - Rifiuto dello spoofing dell'header `X-Forwarded-For` da nodi non riconosciuti come proxy fidati.
3. **[SessionSecurityHardeningTest.java](../AuthMicroService/src/test/java/com/spendy/auth/Security/SessionSecurityHardeningTest.java)**:
   - Verifica della Refresh Token Rotation: emissione di un nuovo token e contestuale revoca del precedente.
   - Rilevamento del riutilizzo di token revocati e conseguente revoca a catena di tutti i token con lo stesso `family_id`.
   - Conferma che nel database viene salvato unicamente l'hash SHA-256 e che la colonna `token` è `NULL`.
4. **[DeploymentSecurityHardeningTest.java](../AuthMicroService/src/test/java/com/spendy/auth/Security/DeploymentSecurityHardeningTest.java)**:
   - Verifica che l'avvio in modalità produzione fallisca con eccezione se `jwt.allow-ephemeral-keys=true` o se mancano le chiavi RSA.

---

## 5. Esecuzione dei Test e Debugging

### 5.1. Esecuzione Globale
```bash
./mvnw clean test
```

### 5.2. Esecuzione per Singolo Modulo
```bash
./mvnw test -pl AuthMicroService
./mvnw test -pl ExpenseMicroService
./mvnw test -pl Gateway
```

### 5.3. Esecuzione di una Singola Classe o Metodo di Test
```bash
# Esecuzione di una specifica classe di test:
./mvnw test -Dtest=JwtSecurityHardeningTest

# Esecuzione di un singolo metodo di test:
./mvnw test -Dtest=SessionSecurityHardeningTest#testRefreshTokenReuseRevokesFamily
```

---

## 6. Aree Non Coperte e Debito Tecnico dei Test

Sebbene la suite unitaria e di integrazione sia solida (354 test), l'analisi del repository evidenzia le seguenti aree scoperte:
1. **Test End-to-End su MySQL Reale**:
   I test di integrazione JPA correnti ([AuthJpaPersistenceIntegrationTest](../AuthMicroService/src/test/java/com/spendy/auth/Repository/AuthJpaPersistenceIntegrationTest.java) e [ExpenseJpaPersistenceIntegrationTest](../ExpenseMicroService/src/test/java/com/spendyteam/expense/Repository/ExpenseJpaPersistenceIntegrationTest.java)) si appoggiano su un database in-memory H2 configurato con alias SQL ([H2FunctionAliases.java](../ExpenseMicroService/src/test/java/com/spendyteam/expense/Utility/H2FunctionAliases.java)). Sarebbe opportuno integrare **Testcontainers** per validare le query native direttamente su un container MySQL 8.4 ufficiale.
2. **Test di Carico e Stress Concurrent**:
   I precedenti test di performance sono stati dismessi (commit `1eeb583` e `3526a5c`). È raccomandata l'adozione di strumenti come k6 o Gatling per validare la tenuta del semaforo di persistenza (`ExpenseBatchWriter`) e del pool HikariCP sotto carichi elevati di importazione CSV.
3. **Integrazione tra Gateway e Microservizi in Rete Reale**:
   Attualmente i test del Gateway testano la catena reattiva e la decodifica JWT, ma non eseguono chiamate HTTP reali verso le istanze in ascolto dei microservizi a valle.
