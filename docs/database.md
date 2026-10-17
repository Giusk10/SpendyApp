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
                            │    File: db/init/00-init-db.sql            │
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

### 2.2. Script di Inizializzazione
- `db/init/00-init-db.sql` crea i due schemi e concede privilegi di compatibilità all'account condiviso.
- `db/init/99-init-users.sh` viene eseguito dopo la creazione degli schemi. Quando sono configurati gli account dedicati, crea gli account runtime e Flyway, applica grant per schema e revoca i privilegi dell'account condiviso.
- Gli script in `/docker-entrypoint-initdb.d/` vengono eseguiti **solo quando il volume MySQL è vuoto**. Aggiornare i file non modifica automaticamente i grant di un volume esistente.

### 2.3. Separazione degli Account Runtime e Flyway
Per il setup locale, `.env.example` propone account separati:
- `MYSQL_AUTH_USER` / `MYSQL_AUTH_PASSWORD`: runtime Auth, DML sul solo schema `spendy_auth`.
- `MYSQL_EXPENSE_USER` / `MYSQL_EXPENSE_PASSWORD`: runtime Expense, DML sul solo schema `spendy_expense`.
- `MYSQL_AUTH_MIGRATION_USER` / `MYSQL_AUTH_MIGRATION_PASSWORD`: account Flyway con DDL sul solo schema Auth.
- `MYSQL_EXPENSE_MIGRATION_USER` / `MYSQL_EXPENSE_MIGRATION_PASSWORD`: account Flyway con DDL sul solo schema Expense.

Se non si configurano account Flyway distinti, per compatibilità l'account runtime mantiene anche i privilegi DDL necessari a Flyway. La separazione runtime/migrazione completa richiede quindi tutti e quattro gli account dedicati. Le password di `.env.example` sono solo per sviluppo locale e non vanno usate in produzione.

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

## 3. Procedura di Migrazione per Volumi MySQL Esistenti

MySQL non riesegue gli script di `/docker-entrypoint-initdb.d/` su un volume già inizializzato. Per aggiornare gli account senza eliminare database o volumi, effettuare prima un backup ripristinabile e seguire la procedura dettagliata in [`docs/runbooks/mysql-account-migration.md`](runbooks/mysql-account-migration.md).

La procedura configura le credenziali in `.env`, esegue il provisioning esplicito con `db/init/99-init-users.sh`, ricrea i microservizi e verifica Flyway e gli smoke test. Non eseguire comandi distruttivi come `docker compose down -v` per applicare la migrazione.

---

## 4. Persistenza dei Dati

In ambiente Docker Compose, i dati di MySQL sono persistiti nel volume nominato:
```yaml
volumes:
  mysql-data:
    name: spendy-mysql-data
```
- Il comando standard [`./scripts/dev-down.sh`](../scripts/dev-down.sh) esegue `docker compose down` **senza il flag `-v`**, preservando intatti i dati tra riavvii successivi.
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
