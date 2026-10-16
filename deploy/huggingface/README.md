# Deployment su Hugging Face Spaces (SpendyApp)

Questa directory contiene gli artefatti e le configurazioni dedicate al deployment del backend di **SpendyApp** su **Hugging Face Spaces** come container Docker autonomo (*All-in-One*).

---

## 1. Caratteristiche dell'Ambiente Hugging Face Spaces

- **Tipo di Space**: `docker`
- **Porta Ingress Obbligatoria**: `7860` (HTTP)
- **Utente di Esecuzione**: `user` con UID non privilegiato `1000`
- **Architettura del Container**: All-in-One Multi-Processo supervisionato da [`entrypoint.sh`](file:///Users/gius03/Progetti/SpendyProject/SpendyApp/deploy/huggingface/entrypoint.sh).

```
                      Hugging Face Spaces Ingress (Porta 7860)
                                      │
                                      ▼
                      ┌─────────────────────────────────┐
                      │    Spring Cloud Gateway (7860)  │
                      └──────────────┬──────────────────┘
                                     │
                 ┌───────────────────┴───────────────────┐
                 ▼                                       ▼
    ┌───────────────────────────┐           ┌───────────────────────────┐
    │  AuthMicroService (8081)  │           │ ExpenseMicroService (8084)│
    └────────────┬──────────────┘           └────────────┬──────────────┘
                 │                                       │
                 └───────────────────┬───────────────────┘
                                     ▼
                      MySQL Database (Remoto / Cloud)
```

---

## 2. Configurazione delle Variabili e dei Secrets su Hugging Face

Nelle impostazioni dello Space (**Settings &rarr; Variables and Secrets**), configurare i seguenti parametri:

### Repository Secrets (Sensibili)
| Nome Secret | Descrizione |
| :--- | :--- |
| `MYSQL_PASSWORD` | Password dell'utente MySQL per accedere agli schemi `spendy_auth` e `spendy_expense`. |
| `JWT_PRIVATE_KEY` | Chiave RSA privata (2048-bit PEM string) per la firma dei token in `AuthMicroService`. |
| `JWT_PUBLIC_KEY` | Chiave RSA pubblica (2048-bit PEM string) per la validazione dei token in `Gateway` ed `Expense`. |

### Repository Variables (Configurazioni non segrete)
| Nome Variabile | Valore Tipico | Descrizione |
| :--- | :--- | :--- |
| `ENVIRONMENT` | `production` | Attiva i controlli rigorosi di sicurezza all'avvio. |
| `MYSQL_HOST` | `mysql.tuoprovider.com` | Hostname del database MySQL gestito o su VPS/cloud. |
| `MYSQL_PORT` | `3306` | Porta del database MySQL. |
| `MYSQL_USER` | `spendy` | Nome utente del database MySQL. |
| `MYSQL_DATABASE_AUTH` | `spendy_auth` | Nome dello schema per l'autenticazione. |
| `MYSQL_DATABASE_EXPENSE` | `spendy_expense` | Nome dello schema per le spese. |
| `JWT_ISSUER` | `spendy-auth` | Issuer del token JWT. |
| `JWT_AUDIENCE` | `spendy-app` | Audience attesa del token JWT. |
| `JWT_EXPIRATION` | `900000` | Scadenza token (15 minuti in millisecondi). |
| `JWT_ALLOW_EPHEMERAL_KEYS` | `false` | Disabilita chiavi effimere in produzione. |
| `CORS_ALLOWED_ORIGINS` | `https://*.hf.space,http://localhost:3000` | Origini CORS ammesse. |

---

## 3. Flusso di Avvio e Supervisione ([`entrypoint.sh`](file:///Users/gius03/Progetti/SpendyProject/SpendyApp/deploy/huggingface/entrypoint.sh))

1. **Rilevamento Ambiente e Controllo Chiavi**:
   - In produzione (`ENVIRONMENT=production`), valida la presenza di `JWT_PRIVATE_KEY` e `JWT_PUBLIC_KEY`. Se assenti, l'avvio si interrompe immediatamente con codice di errore `1`.
   - In sviluppo/demo, genera automaticamente un set di chiavi RSA a 2048 bit in `/app/keys/` se non configurate.
2. **Avvio Servizi Interni**:
   - Avvia `AuthMicroService` in background sulla porta 8081.
   - Avvia `ExpenseMicroService` in background sulla porta 8084.
3. **Attesa Disponibilità (Readiness Check)**:
   - Interroga periodicamente gli endpoint `/actuator/health` di entrambi i microservizi (timeout: 60s).
   - Se un servizio termina o non risponde entro il tempo limite, il container abortisce mostrando gli ultimi log utili.
4. **Avvio API Gateway**:
   - Avvia lo Spring Cloud Gateway sulla porta pubblica 7860.
5. **Supervisione Attiva dei Processi**:
   - Un ciclo leggero monitora i PID di tutti e tre i processi JVM.
   - Se uno qualsiasi dei tre servizi si arresta improvvisamente, lo script intercetta la terminazione, arresta i processi residui e fa terminare il container, consentendo a Hugging Face Spaces di registrare il crash e tentare il riavvio.
6. **Graceful Shutdown**:
   - Alla ricezione di segnali `SIGTERM` o `SIGINT`, invia segnali controllati a tutti i microservizi per rilasciare connessioni al database e thread pool.
