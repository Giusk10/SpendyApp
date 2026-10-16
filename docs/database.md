# Gestione del Database e Migrazioni

Questo documento descrive l'architettura dei dati, il processo di inizializzazione, le migrazioni di schema gestite tramite **Flyway**, la persistenza dei volumi e le procedure di backup e ripristino per **SpendyApp**.

---

## 1. Architettura dei Database

SpendyApp adotta il pattern architetturale **Database per Service** (o schema logico dedicato) per garantire l'isolamento del dominio applicativo tra i microservizi:

- **`spendy_auth`**: Gestito esclusivamente da `AuthMicroService`. Contiene le tabelle `users` e `refresh_tokens`.
- **`spendy_expense`**: Gestito esclusivamente da `ExpenseMicroService`. Contiene la tabella `expenses`.

Entrambi gli schemi sono ospitati su un'istanza MySQL 8.4 isolata nella rete privata `spendy-network` e accessibile con l'utente applicativo `spendy`.

---

## 2. Inizializzazione vs Migrazioni Flyway

È fondamentale distinguere i due livelli di gestione dei dati:

```
                            ┌─────────────────────────────────────────┐
                            │    1. Livello Infrastruttura (MySQL)    │
                            │    File: db/init/init-db.sql            │
                            │    - Creazione database se assenti      │
                            │    - Assegnazione privilegi utente      │
                            └────────────────────┬────────────────────┘
                                                 │
                                                 ▼
                 ┌─────────────────────────────────────────────────────────┐
                 │          2. Livello Applicativo (Spring Boot)           │
                 │          Migrazioni Versionate Flyway                   │
                 └──────────────┬───────────────────────────┬──────────────┘
                                │                           │
                                ▼                           ▼
                  AuthMicroService Migrations   ExpenseMicroService Migrations
                  - V1__create_auth_schema.sql  - V1__create_expense_schema.sql
                  - V2__security_tokens.sql
                  - V3__security_null_token.sql
```

### 2.1. Script di Inizializzazione (`db/init/init-db.sql`)
- **Ruolo**: Eseguito **esclusivamente al primo avvio** del container MySQL quando la directory `/var/lib/mysql` è vuota.
- **Contenuto**:
  ```sql
  CREATE DATABASE IF NOT EXISTS spendy_auth CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  CREATE DATABASE IF NOT EXISTS spendy_expense CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

  GRANT ALL PRIVILEGES ON spendy_auth.* TO 'spendy'@'%';
  GRANT ALL PRIVILEGES ON spendy_expense.* TO 'spendy'@'%';
  FLUSH PRIVILEGES;
  ```
- **Idempotenza**: I comandi usano `IF NOT EXISTS` per prevenire errori in caso di riesecuzione.

### 2.2. Migrazioni Applicative (Flyway)
- **Ruolo**: Versionamento controllato e incrementale della struttura delle tabelle (DDL) nel tempo.
- **Esecuzione**: Avviene in modo del tutto automatico all'avvio dei microservizi (`spring.flyway.enabled=true`).
- **File di migrazione**:
  - `AuthMicroService/src/main/resources/db/migration/`:
    - `V1__create_auth_schema.sql`: Creazione tabella `users` e tabella iniziale `refresh_tokens`.
    - `V2__security_refresh_tokens_hardening.sql`: Aggiunta colonne per rotazione sicura dei token (family ID, sha256).
    - `V3__security_refresh_tokens_null_token.sql`: Hardening dei vincoli di unicità e nullability.
  - `ExpenseMicroService/src/main/resources/db/migration/`:
    - `V1__create_expense_schema.sql`: Creazione tabella `expenses` con indici per utente e data.

---

## 3. Persistenza dei Dati

In ambiente Docker Compose, i dati di MySQL sono persistiti nel volume nominato:
```yaml
volumes:
  mysql-data:
    name: spendy-mysql-data
```
- Il comando standard [`./scripts/dev-down.sh`](file:///Users/gius03/Progetti/SpendyProject/SpendyApp/scripts/dev-down.sh) esegue `docker compose down` **senza il flag `-v`**, preservando intatti i dati tra riavvii successivi.
- **Comportamento su volumi esistenti**: Se il volume esiste già, MySQL salta l'esecuzione di `init-db.sql` e carica direttamente i dati presenti. Flyway legge la tabella `flyway_schema_history` e applica unicamente le migrazioni non ancora eseguite.

---

## 4. Procedure Operative: Backup e Ripristino

### 4.1. Backup a Caldo (MySQL Dump)
Per esportare lo stato completo di entrambi i database:

```bash
# Esegue il dump di entrambi gli schemi applicativi in un file compresso
docker exec spendy-mysql mysqldump -u spendy -p"$(grep MYSQL_PASSWORD .env | cut -d '=' -f2)" \
  --databases spendy_auth spendy_expense \
  --single-transaction --quick > backup_spendy_$(date +%Y%m%d_%H%M%S).sql
```

### 4.2. Ripristino dei Dati da Backup
Per importare un dump SQL esistente nel container:

```bash
# Ripristina gli schemi dal file di backup
docker exec -i spendy-mysql mysql -u spendy -p"$(grep MYSQL_PASSWORD .env | cut -d '=' -f2)" < backup_spendy_YYYYMMDD_HHMMSS.sql
```

### 4.3. Verifica Post-Migrazione
Dopo aver applicato nuove migrazioni Flyway o ripristinato un backup:
1. Verificare lo stato della tabella di storico Flyway:
   ```bash
   docker exec -it spendy-mysql mysql -u spendy -p"$(grep MYSQL_PASSWORD .env | cut -d '=' -f2)" spendy_auth \
     -e "SELECT version, description, type, installed_on, success FROM flyway_schema_history;"
   ```
2. Eseguire lo smoke test per validare l'accesso ai dati:
   ```bash
   ./scripts/smoke-test.sh
   ```
