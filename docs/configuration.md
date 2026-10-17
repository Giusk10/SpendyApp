# Guida alla Configurazione e Variabili d'Ambiente

Questa guida documenta in modo esaustivo tutte le variabili d'ambiente utilizzate dai microservizi di **SpendyApp**, specificandone obbligatorietà, valori predefiniti e differenze tra ambiente di sviluppo e produzione.

---

## 1. Principi di Sicurezza per la Configurazione

1. **Nessun Segreto nel Version Control**: Il file `.env` e le chiavi crittografiche (`*.pem`, `*.key`) sono esclusi da Git tramite `.gitignore` e da Docker tramite `.dockerignore`.
2. **Template Documentato**: Il file [`.env.example`](../.env.example) funge da modello unico con valori dimostrativi privi di segreti reali.
3. **Fail-Fast in Produzione**: Se mancano parametri obbligatori in produzione (ad esempio le chiavi RSA o le password del database), l'avvio si interrompe immediatamente con un codice di uscita diverso da zero.

---

## 2. Matrice delle Variabili d'Ambiente

### 2.1. Database Relazionale (MySQL 8.4)

| Variabile | Obbligatoria (Prod) | Valore di Default (Dev) | Descrizione |
| :--- | :---: | :--- | :--- |
| `MYSQL_HOST` | **Sì** | `mysql` (Compose) / `localhost` (Locale) | Hostname del server MySQL. In Docker Compose corrisponde al nome del servizio `mysql`. |
| `MYSQL_PORT` | No | `3306` | Porta del server MySQL. |
| `MYSQL_USER` | **Sì** | `spendy` | Utente del database dotato di privilegi sugli schemi applicativi. |
| `MYSQL_PASSWORD` | **Sì** | `spendy_dev_password` | Password dell'utente MySQL per l'accesso ai dati. In produzione deve essere generata in modo sicuro e complessa. |
| `MYSQL_ROOT_PASSWORD` | **Sì** | `spendy_root_dev_password` | Password di `root` per MySQL, richiesta per l'inizializzazione del container MySQL. |
| `MYSQL_DATABASE_AUTH` | No | `spendy_auth` | Nome del database dedicato al modulo AuthMicroService. |
| `MYSQL_DATABASE_EXPENSE` | No | `spendy_expense` | Nome del database dedicato al modulo ExpenseMicroService. |

### 2.2. Crittografia e Token JWT (RS256 Asimmetrico)

| Variabile | Obbligatoria (Prod) | Valore di Default (Dev) | Descrizione |
| :--- | :---: | :--- | :--- |
| `JWT_ISSUER` | No | `spendy-auth` | Claim `iss` verificato all'interno dei token JWT. |
| `JWT_AUDIENCE` | No | `spendy-app` | Claim `aud` verificato all'interno dei token JWT. |
| `JWT_EXPIRATION` | No | `900000` (15 min) | Durata di validità degli Access Token in millisecondi. |
| `JWT_ALLOW_EPHEMERAL_KEYS` | **Sì (deve essere false)** | `false` | Se `true`, consente la generazione temporanea di chiavi effimere in memoria. **In produzione deve essere sempre `false`**. |
| `JWT_PRIVATE_KEY` | Opzionale se impostata LOCATION | *(vuoto)* | Chiave RSA privata a 2048-bit in formato PEM stringa per `AuthMicroService`. |
| `JWT_PUBLIC_KEY` | Opzionale se impostata LOCATION | *(vuoto)* | Chiave RSA pubblica a 2048-bit in formato PEM stringa per `Gateway` ed `ExpenseMicroService`. |
| `JWT_PRIVATE_KEY_LOCATION` | Opzionale se impostata KEY | `file:keys/private_key.pem` | Percorso o URI risorsa della chiave privata (es. `file:/run/secrets/privkey.pem`). |
| `JWT_PUBLIC_KEY_LOCATION` | Opzionale se impostata KEY | `file:keys/public_key.pem` | Percorso o URI risorsa della chiave pubblica (es. `file:/run/secrets/pubkey.pem`). |
| `JWT_ACTIVE_KEY_ID` | No | `spendy-auth-key-1` | Identificativo attivo (`kid`) inserito nell'header JWT. |
| `JWT_PREVIOUS_PUBLIC_KEYS` | No | *(vuoto)* | Lista di chiavi pubbliche precedenti (`kid=PEM,...`) per consentire la rotazione senza disservizi. |
| `JWT_JWK_SET_URI` | No | *(vuoto)* | Endpoint JWKS remoto per risoluzione dinamica delle chiavi su Gateway (`http://auth-service:8081/rest/auth/jwks.json`). |

### 2.3. Rete, CORS e Gateway

| Variabile | Obbligatoria (Prod) | Valore di Default (Dev) | Descrizione |
| :--- | :---: | :--- | :--- |
| `GATEWAY_PORT` | No | `7860` | Porta pubblicata verso l'host locale per accedere all'API Gateway. |
| `CORS_ALLOWED_ORIGINS` | **Sì** | `http://localhost:3000,http://localhost:7860,http://127.0.0.1:7860,http://localhost:8080,http://127.0.0.1:8080` | Origini consentite (senza wildcard con credenziali). In prod inserire i domini del frontend o Hugging Face. |
| `RATE_LIMITING_ENABLED` | No | `true` | Abilita il rate limiting su base IP in `AuthMicroService` (max 5 tentativi login per minuto). |
| `AUTH_SERVICE_URI` | No | `http://auth-service:8081` | URI interno attraverso cui il Gateway instrada le chiamate verso AuthMicroService. |
| `EXPENSE_SERVICE_URI` | No | `http://expense-service:8084` | URI interno attraverso cui il Gateway instrada le chiamate verso ExpenseMicroService. |

### 2.4. JVM e Profilo Operativo

| Variabile | Obbligatoria (Prod) | Valore di Default (Dev) | Descrizione |
| :--- | :---: | :--- | :--- |
| `ENVIRONMENT` | **Sì** | `development` | Se impostato a `production` o `prod`, attiva i controlli restrittivi all'avvio. |
| `SPRING_PROFILES_ACTIVE` | No | *(default)* | Profilo Spring attivo (`production`, `docker`, `test`). |
| `JAVA_TOOL_OPTIONS` | No | `-Xms128m -Xmx256m` | Opzioni JVM di memoria e garbage collector passate ai container. |

---

## 3. Configurazione degli Ambienti

### 3.1. Ambiente di Sviluppo Locale (Docker Compose)
1. Copiare `.env.example` in `.env`:
   ```bash
   cp .env.example .env
   ```
2. Lo script [`./scripts/dev-up.sh`](../scripts/dev-up.sh) verifica la presenza di `.env` e crea automaticamente le chiavi RSA in `keys/` se non presenti.

### 3.2. Ambiente Hugging Face Spaces (All-in-One)
Nelle impostazioni dello Space (**Settings &rarr; Variables and Secrets**):
- **Secrets**: `MYSQL_PASSWORD`, `JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEY`.
- **Variables**: `ENVIRONMENT=production`, `MYSQL_HOST=...`, `MYSQL_USER=...`, `CORS_ALLOWED_ORIGINS=...`.
