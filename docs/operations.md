# Manuale delle Operazioni e Monitoraggio

Questo documento descrive le procedure operative standard per la gestione, il monitoraggio della salute del sistema e la diagnostica continua di **SpendyApp**.

---

## 1. Livelli di Controllo dello Stato di Salute (Health Checks)

SpendyApp definisce una gerarchia di controlli per determinare se il sistema è realmente pronto e funzionante:

```
┌────────────────────────────────────────────────────────┐
│ 1. Container Running (Docker Engine)                  │
│    Stato: docker compose ps                            │
├────────────────────────────────────────────────────────┤
│ 2. Processo JVM Attivo (PID Alive)                     │
│    Verifica: kill -0 $PID                              │
├────────────────────────────────────────────────────────┤
│ 3. Spring Boot Actuator Ready                          │
│    Endpoint: GET /actuator/health -> {"status":"UP"}   │
├────────────────────────────────────────────────────────┤
│ 4. Dipendenze Raggiungibili (Database Connection Pool)  │
│    Verifica: HikariCP connection test                  │
├────────────────────────────────────────────────────────┤
│ 5. Gateway Routing & Ingress Operativo                 │
│    Endpoint: GET /Auth/rest/auth/health -> 200 OK      │
├────────────────────────────────────────────────────────┤
│ 6. Policy di Sicurezza Applicate                       │
│    Endpoint: GET /Expense/rest/expense/all -> 401 Unauth│
└────────────────────────────────────────────────────────┘
```

### 1.1. Endpoint di Health Ufficiali

| Componente | Endpoint Diretto | Endpoint tramite Gateway | Accessibilità |
| :--- | :--- | :--- | :--- |
| **Gateway** | `http://localhost:7860/actuator/health` | *(stesso)* | Pubblico (PermitAll) |
| **Auth Service** | `http://auth-service:8081/actuator/health` | `http://localhost:7860/Auth/rest/auth/health` | Pubblico (PermitAll) |
| **Expense Service** | `http://expense-service:8084/actuator/health` | *(privato, isolato nella rete Docker)* | Rete interna |
| **MySQL** | `mysqladmin ping -h localhost -u root` | N/A | Rete interna (Healthcheck container) |

---

## 2. Smoke Test e Validazione Automatica

Per verificare istantaneamente l'integrità end-to-end dell'applicazione:

```bash
# Esegue lo smoke test puntando all'istanza locale predefinita (http://localhost:7860)
./scripts/smoke-test.sh

# Esegue lo smoke test su un ambiente specifico o remoto
./scripts/smoke-test.sh https://tuo-space-huggingface.hf.space
```

Lo script restituisce:
- Exit code `0`: se tutti i controlli (Gateway health, routing Auth, documentazione Swagger, JWKS, sicurezza 401 su Expense) hanno avuto successo.
- Exit code `1`: in caso di fallimento o timeout.

---

## 3. Gestione e Consultazione dei Log

Lo script [`./scripts/dev-logs.sh`](../scripts/dev-logs.sh) semplifica il monitoraggio in tempo reale:

```bash
# Visualizza i log aggregati di tutti i container
./scripts/dev-logs.sh

# Filtra solo i log dell'API Gateway
./scripts/dev-logs.sh gateway-service

# Filtra solo i log dell'Auth Service
./scripts/dev-logs.sh auth-service

# Filtra i log di MySQL
./scripts/dev-logs.sh mysql
```

---

## 4. Operazioni di Manutenzione Ordinaria

### 4.1. Riavvio Controllato di un Singolo Microservizio
Se è necessario riavviare solo un microservizio (ad esempio dopo una modifica):
```bash
docker compose restart auth-service
```

### 4.2. Ricompilazione e Aggiornamento Rapido dell'Immagine
Per ricostruire i JAR locali e riavviare i container senza toccare il volume del database:
```bash
./scripts/dev-up.sh
```

### 4.3. Ispezione Risorse e Metriche
Per verificare l'utilizzo di memoria e CPU dei container:
```bash
docker stats --no-stream
```
I limiti consigliati sono:
- `mysql`: ~150-250 MB
- `auth-service`: ~200-350 MB
- `expense-service`: ~250-450 MB (inclusi modelli NLP)
- `gateway-service`: ~150-250 MB

### 4.4. Verifica e Manutenzione Privilegi Database
Per controllare i privilegi attivi concessi agli utenti MySQL ed assicurarsi che rispettino il principio del minimo privilegio:
```bash
# Verifica privilegi degli account runtime e Flyway (adattare i nomi se personalizzati)
docker compose exec mysql mysql -u root -p"$MYSQL_ROOT_PASSWORD" -e "SHOW GRANTS FOR 'spendy_auth'@'%';"
docker compose exec mysql mysql -u root -p"$MYSQL_ROOT_PASSWORD" -e "SHOW GRANTS FOR 'spendy_auth_migrator'@'%';"

# Per volumi esistenti: seguire la procedura con backup e verifica
# docs/runbooks/mysql-account-migration.md
```

