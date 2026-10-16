# Gestione del Database e Migrazioni

Questo documento descrive l'architettura dei dati, il processo di inizializzazione, la gestione della sicurezza e dei privilegi minimi (Least Privilege), le migrazioni di schema gestite tramite **Flyway**, la persistenza dei volumi e le procedure di migrazione e backup per **SpendyApp**.

---

## 1. Architettura dei Database

SpendyApp adotta il pattern architetturale **Database per Service** (schema logico dedicato) per garantire l'isolamento del dominio applicativo tra i microservizi:

- **`spendy_auth`**: Gestito esclusivamente da `AuthMicroService`. Contiene le tabelle `users` e `refresh_tokens`.
- **`spendy_expense`**: Gestito esclusivamente da `ExpenseMicroService`. Contiene la tabella `expenses`.

Entrambi gli schemi risiedono su un'istanza MySQL 8.4 isolata nella rete privata `spendy-network`.

---

## 2. Inizializzazione, Privilegi Minimi e Migrazioni Flyway

È fondamentale distinguere i diversi livelli di gestione e responsabilità sui dati:

```
                            ┌─────────────────────────────────────────┐
                            │    1. Livello Infrastruttura (MySQL)    │
                            │    File: db/init/init-db.sql            │
                            │    - Creazione database se assenti      │
                            │    - Assegnazione PRIVILEGI MINIMI      │
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

### 2.1. Principio del Minimo Privilegio (Least Privilege)
A differenza delle configurazioni predefinite di sviluppo che assegnano `ALL PRIVILEGES` o `GRANT OPTION`, SpendyApp applica rigorosamente il principio del privilegio minimo:
- **Operazioni DML di Runtime (JPA / Hibernate)**: `SELECT`, `INSERT`, `UPDATE`, `DELETE` per la gestione ordinaria delle entità.
- **Operazioni DDL di Migrazione (Flyway)**: `CREATE`, `DROP`, `ALTER`, `INDEX`, `REFERENCES` per creare/modificare tabelle, indici e vincoli di foreign key all'avvio del servizio.
- **Privilegi Esclusi/Revocati**: Sono tassativamente esclusi permessi amministrativi globali (`*.*`), `GRANT OPTION`, `DROP DATABASE`, `SUPER`, `PROCESS`, `RELOAD`, `SHUTDOWN`, `FILE`. In questo modo un'eventuale vulnerabilità applicativa non può compromettere l'intero server database o cancellare database adiacenti.

### 2.2. Script di Inizializzazione (`db/init/init-db.sql`)
- **Ruolo**: Eseguito **esclusivamente al primo avvio** del container MySQL quando la directory `/var/lib/mysql` è vuota.
- **Contenuto**:
  ```sql
  CREATE DATABASE IF NOT EXISTS spendy_auth CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  CREATE DATABASE IF NOT EXISTS spendy_expense CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

  GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, INDEX, REFERENCES ON spendy_auth.* TO 'spendy'@'%';
  GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, INDEX, REFERENCES ON spendy_expense.* TO 'spendy'@'%';
  FLUSH PRIVILEGES;
  ```

### 2.3. Separazione degli Account Applicativi (Auth vs Expense)
Per ambienti ad alta sicurezza o conformità, SpendyApp supporta la completa segregazione delle utenze:
1. **Configurazione variabili d'ambiente** (in `.env`):
   ```properties
   MYSQL_AUTH_USER=spendy_auth
   MYSQL_AUTH_PASSWORD=spendy_auth_secure_password
   MYSQL_EXPENSE_USER=spendy_expense
   MYSQL_EXPENSE_PASSWORD=spendy_expense_secure_password
   ```
2. **Provisioning automatico (`db/init/02-init-users.sh`)**:
   Se queste variabili sono valorizzate, lo script di inizializzazione crea automaticamente i due utenti dedicati, concede a ciascuno i privilegi minimi esclusivamente sul rispettivo schema (`spendy_auth.*` per auth, `spendy_expense.*` per expense) e revoca i permessi all'utente generico `spendy`.
3. **Fallback compatibile**: Se le variabili dedicate non sono specificate, il sistema continua a funzionare con l'utente condiviso `spendy` configurato con privilegi minimi.

### 2.4. Migrazioni Applicative (Flyway)
- **Ruolo**: Versionamento controllato e incrementale della struttura delle tabelle (DDL) nel tempo.
- **Esecuzione**: Avviene in modo automatico all'avvio dei microservizi (`spring.flyway.enabled=true`).
- **File di migrazione**:
  - `AuthMicroService/src/main/resources/db/migration/`:
    - `V1__create_auth_schema.sql`: Creazione tabella `users` e tabella iniziale `refresh_tokens`.
    - `V2__security_refresh_tokens_hardening.sql`: Aggiunta colonne per rotazione token (`family_id`, `token_hash`).
    - `V3__security_refresh_tokens_null_token.sql`: Hardening unicità e archiviazione solo hash.
  - `ExpenseMicroService/src/main/resources/db/migration/`:
    - `V1__create_expense_schema.sql`: Creazione tabella `expenses` con indici per utente e data.

---

## 3. Procedura di Migrazione Privilegi per Volumi MySQL Esistenti

> [!IMPORTANT]
> La modifica di `init-db.sql` ha effetto **soltanto sui nuovi container**. Su volumi MySQL esistenti (`spendy-mysql-data`), MySQL non riesegue i file in `/docker-entrypoint-initdb.d/`.

Per aggiornare un'installazione esistente revocando `ALL PRIVILEGES` e applicando i privilegi minimi **senza cancellare il volume o perdere dati**:

1. Assicurarsi che l'ambiente sia avviato:
   ```bash
   ./scripts/dev-up.sh
   ```
2. Eseguire lo script di migrazione dei privilegi a caldo:
   ```bash
   docker compose exec -i mysql mysql -u root -p"$MYSQL_ROOT_PASSWORD" < db/init/migrate-least-privilege.sql
   ```
3. Verificare i privilegi attivi assegnati all'utente:
   ```bash
   docker compose exec -i mysql mysql -u root -p"$MYSQL_ROOT_PASSWORD" -e "SHOW GRANTS FOR 'spendy'@'%';"
   ```
   L'output mostrerà esclusivamente:
   ```text
   GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, INDEX, REFERENCES ON `spendy_auth`.* TO 'spendy'@'%'
   GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, INDEX, REFERENCES ON `spendy_expense`.* TO 'spendy'@'%'
   ```

---

## 4. Persistenza dei Dati

In ambiente Docker Compose, i dati di MySQL sono persistiti nel volume nominato:
```yaml
volumes:
  mysql-data:
    name: spendy-mysql-data
```
- Il comando standard [`./scripts/dev-down.sh`](file:///Users/gius03/Progetti/SpendyProject/SpendyApp/scripts/dev-down.sh) esegue `docker compose down` **senza il flag `-v`**, preservando intatti i dati tra riavvii successivi.
- **Comportamento su volumi esistenti**: Se il volume esiste già, MySQL salta l'esecuzione di `init-db.sql` e carica direttamente i dati presenti. Flyway legge la tabella `flyway_schema_history` e applica unicamente le migrazioni non ancora eseguite.

---

## 5. Procedure Operative: Backup e Ripristino

### 5.1. Backup a Caldo (MySQL Dump)
Per esportare lo stato completo di entrambi i database:

```bash
docker exec spendy-mysql mysqldump -u spendy -p"$(grep MYSQL_PASSWORD .env | cut -d '=' -f2)" \
  --databases spendy_auth spendy_expense \
  --single-transaction --quick > backup_spendy_$(date +%Y%m%d_%H%M%S).sql
```

### 5.2. Ripristino dei Dati da Backup
Per importare un dump SQL esistente nel container:

```bash
docker exec -i spendy-mysql mysql -u spendy -p"$(grep MYSQL_PASSWORD .env | cut -d '=' -f2)" < backup_spendy_YYYYMMDD_HHMMSS.sql
```

### 5.3. Verifica Post-Migrazione
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
