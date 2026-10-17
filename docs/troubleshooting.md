# Guida alla Risoluzione dei Problemi (Troubleshooting)

Questa guida raccoglie i problemi più frequenti che possono verificarsi durante lo sviluppo, l'avvio locale o il deployment di **SpendyApp**, con sintomi precisi, cause probabili, comandi diagnostici e soluzioni concrete.

---

## 1. Gateway non parte o si arresta con errore `depends_on`

### Sintomo
Eseguendo `docker compose up` o `./scripts/dev-up.sh`, il container `gateway-service` rimane in attesa indefinita o fallisce l'avvio perché `auth-service` o `expense-service` non raggiungono lo stato `healthy`.

### Causa Probabile
I microservizi interni (`auth-service` o `expense-service`) impiegano più tempo del previsto ad avviarsi oppure falliscono la connessione al database MySQL o la migrazione Flyway.

### Comandi Diagnostici
```bash
# 1. Controlla lo stato di salute dei container
docker compose ps

# 2. Controlla gli ultimi 50 log di Auth Service ed Expense Service
docker compose logs --tail=50 auth-service
docker compose logs --tail=50 expense-service

# 3. Verifica se il database MySQL è sano
docker compose logs --tail=20 mysql
```

### Soluzione
- Se MySQL fallisce l'autenticazione, verificare che `MYSQL_PASSWORD` in `.env` corrisponda alla password inizializzata nel volume `spendy-mysql-data`.
- Se si tratta di un timeout dovuto a risorse limitate della macchina host, aumentare il parametro `start_period` o i `retries` nei relativi healthcheck in [docker-compose.yml](../docker-compose.yml).

---

## 2. Errore Compose: `required variable MYSQL_ROOT_PASSWORD is missing a value`

### Sintomo
Il comando `docker compose config` o `docker compose up` fallisce con il messaggio:
```
error while interpolating services.mysql.environment.MYSQL_ROOT_PASSWORD: required variable MYSQL_ROOT_PASSWORD is missing a value: MYSQL_ROOT_PASSWORD must be configured in .env
```

### Causa Probabile
Il file `.env` locale non contiene la variabile obbligatoria `MYSQL_ROOT_PASSWORD` oppure il file `.env` non è stato ancora creato.

### Comandi Diagnostici
```bash
grep "MYSQL_ROOT_PASSWORD" .env || echo "Variabile assente in .env"
```

### Soluzione
Copiare `.env.example` in `.env` o aggiungere la variabile mancante:
```bash
echo "MYSQL_ROOT_PASSWORD=spendy_root_dev_password" >> .env
```
In alternativa, eseguire lo script automatico [`./scripts/dev-up.sh`](../scripts/dev-up.sh) che gestisce la configurazione automatica di sviluppo.

---

## 3. Conflitto porta host: `Bind for 0.0.0.0:7860 failed: port is already allocated`

### Sintomo
All'avvio dei container viene segnalato che la porta `7860` è già occupata da un altro processo o da un'istanza precedente.

### Comandi Diagnostici
```bash
# macOS / Linux: identifica il processo che occupa la porta 7860
lsof -i :7860
```

### Soluzione
- **Opzione A**: Arrestare il processo che occupa la porta (`kill <PID>`) o fermare container precedenti con `./scripts/dev-down.sh`.
- **Opzione B**: Rimappare la porta esposta su una porta differente (es. `8080`) configurando in `.env`:
  ```bash
  GATEWAY_PORT=8080
  ```

---

## 4. Errore JWT: `Invalid JWT algorithm: RS256 expected` o `Missing private key`

### Sintomo
Nei log di `AuthMicroService` compare:
`❌ ERRORE CRITICO DI SICUREZZA: Chiavi RSA obbligatorie mancanti in produzione!`
oppure il Gateway rifiuta tutti i token con HTTP 401.

### Causa Probabile
1. In produzione (`ENVIRONMENT=production`), non sono state fornite le chiavi RSA tramite `JWT_PRIVATE_KEY` / `JWT_PUBLIC_KEY`.
2. In locale, le chiavi in `keys/` non sono state generate o il percorso specificato in `.env` non esiste.

### Comandi Diagnostici
```bash
# Verifica se esistono i file delle chiavi locali
ls -la keys/private_key.pem keys/public_key.pem
```

### Soluzione
Per generare rapidamente una coppia di chiavi RSA a 2048-bit valida per lo sviluppo locale:
```bash
mkdir -p keys
openssl genpkey -algorithm RSA -out keys/private_key.pem -pkeyopt rsa_keygen_bits:2048
openssl rsa -pubout -in keys/private_key.pem -out keys/public_key.pem
```
Assicurarsi che in `.env` siano impostati:
```properties
JWT_PRIVATE_KEY_LOCATION=file:keys/private_key.pem
JWT_PUBLIC_KEY_LOCATION=file:keys/public_key.pem
```

---

## 5. Flyway: `Checksum mismatch for migration version Vx`

### Sintomo
All'avvio di `auth-service` o `expense-service`, Spring Boot lancia `FlywayValidateException: Validate failed: Migrations have failed validation`.

### Causa Probabile
È stato modificato a posteriori il contenuto di un file SQL di migrazione già applicato in precedenza (ad esempio modificando `V1__create_auth_schema.sql`).

### Soluzione
- In ambiente di sviluppo:
  Se i dati sono puramente di test e possono essere ricreati, eliminare il volume e riavviare:
  ```bash
  docker compose down -v
  ./scripts/dev-up.sh
  ```
  *(Attenzione: questa operazione cancella tutti i dati del database locale).*
- In ambiente di produzione / stage:
  **Non modificare mai i file di migrazione già eseguiti!** Creare sempre un nuovo file di migrazione incrementale (es. `V4__nome_modifica.sql`).

---

## 6. Errori CORS nel browser (`Blocked by CORS policy`)

### Sintomo
I client web o le chiamate da frontend ricevono errori CORS nella console del browser con status HTTP `403` o `Blocked`.

### Causa Probabile
L'origine da cui proviene la richiesta (es. `http://localhost:5173` o un URL custom) non è inclusa nella lista consentita.

### Soluzione
Aggiungere l'origine specifica nella variabile `CORS_ALLOWED_ORIGINS` in `.env`:
```properties
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:7860,http://127.0.0.1:7860,http://localhost:5173
```
Riavviare i container con `./scripts/dev-up.sh`.

---

## 7. Divergenza Git durante il Deploy (`CD - Deploy to Hugging Face Spaces`)

### Sintomo
Il job `deploy` della pipeline fallisce con:
```
❌ ERRORE CRITICO: Rilevata divergenza tra il repository GitHub e Hugging Face Spaces!
La sincronizzazione è stata bloccata senza tentare alcun force-push automatico.
```

### Causa Probabile
Lo Space su Hugging Face è stato modificato esternamente (ad es. modifica dei metadati o del README via interfaccia web di Hugging Face) oppure un commit precedente è stato riscritto, rendendo le storie non lineari.

### Soluzione
1. **Analisi locale delle differenze**:
   ```bash
   git fetch https://huggingface.co/spaces/<HF_USERNAME>/<SPACE_NAME> main
   git log HEAD...FETCH_HEAD --oneline
   ```
2. **Integrazione delle modifiche**: Se i commit remoti contengono modifiche da preservare, eseguire un `merge` o `rebase` locale sul branch `main` e inviare le modifiche su GitHub con un nuovo commit.
3. **Sovrascrittura controllata di emergenza**: Se si è certi che lo stato su GitHub debba sovrascrivere interamente lo Space remoto:
   - Aprire GitHub &rarr; **Actions** &rarr; **CD - Deploy to Hugging Face Spaces**.
   - Cliccare su **Run workflow** e impostare `force_push: true`.

---

## 8. Fallimento della Verifica Post-Deployment (`verify-deployment`)

### Sintomo
Il codice viene inviato correttamente a Hugging Face, ma il job successivo `verify-deployment` fallisce con:
`❌ ERRORE: L'applicazione non ha risposto UP su .../actuator/health entro 180s` oppure lo Space entra in `RUNTIME_ERROR`.

### Causa Probabile
1. In produzione, mancano le chiavi `JWT_PRIVATE_KEY` o `JWT_PUBLIC_KEY` tra i secret di Hugging Face.
2. Il database MySQL remoto non è raggiungibile dallo Space o le credenziali `MYSQL_PASSWORD` sono errate.
3. Lo Space è ancora in fase di build lenta per indisponibilità momentanea delle risorse di calcolo.

### Diagnosi e Soluzione
1. Aprire l'URL dello Space: `https://huggingface.co/spaces/<HF_USERNAME>/<SPACE_NAME>`.
2. Esaminare la scheda **Logs** per consultare l'output di `entrypoint.sh` e dei microservizi interni (`[Auth]`, `[Expense]`, Gateway).
3. Correggere i secret su Hugging Face (**Settings &rarr; Variables and Secrets**) e riavviare lo Space o riattivare il workflow.

---

## 9. Fallimento Avvio Locale (`scripts/dev-up.sh` termina con exit code 1)

### Sintomo
Lo script `./scripts/dev-up.sh` non completa l'avvio e restituisce un errore esplicito.

### Cause e Risoluzioni
- **Demone Docker non attivo**:
  `❌ Errore: Il demone Docker non è in esecuzione o non risponde.`
  *Soluzione*: Avviare Docker Desktop (su macOS/Windows) o il demone `sudo systemctl start docker` (su Linux).
- **File `.env` assente**:
  `❌ Errore: File di configurazione .env non trovato nella radice del progetto.`
  *Soluzione*: Eseguire `cp .env.example .env` e verificare le variabili prima di rilanciare `./scripts/dev-up.sh`.
- **Uno o più servizi non healthy**:
  `❌ ERRORE: Uno o più servizi essenziali non hanno raggiunto lo stato 'healthy' entro 90s!`
  *Soluzione*: Lo script elenca il servizio specifico guasto. Eseguire `./scripts/dev-logs.sh <servizio>` per visionare l'errore puntuale (ad es. porta MySQL occupata o fallimento connessione).

