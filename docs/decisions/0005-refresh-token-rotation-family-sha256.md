# 5. Refresh Token Rotation con Rilevamento Riutilizzo (Family ID) e Hashing SHA-256

- **Stato**: Accettata
- **Data**: 2026-10-16
- **Decisori**: Spendy Core Team
- **Commit di Riferimento**: `271b17b` (*feat: implement security hardening and JWT authentication across microservices*), `a90e372` (*fix: harden JWT key resolution, prohibit ephemeral keys in production*)

---

## Contesto

I token di accesso (JWT Access Token) hanno una durata limitata (15 minuti) per ridurre la finestra di rischio in caso di intercettazione. Per consentire agli utenti di rimanere autenticati senza dover inserire continuamente le credenziali, il sistema rilascia un Refresh Token a lunga scadenza (30 giorni).
Tuttavia, i refresh token tradizionali a lunga scadenza e riutilizzabili comportano severi rischi di sicurezza:
1. Se un refresh token viene sottratto (ad esempio tramite XSS su un client o intercettazione del traffico), l'attaccante può utilizzarlo indefinitamente fino alla sua scadenza per ottenere nuovi access token.
2. La memorizzazione in chiaro dei token nel database espone gli utenti al furto di sessione massivo in caso di dump non autorizzato o data breach.
3. In caso di richieste simultanee legittime da parte del client (es. riapertura dell'applicazione con chiamate concorrenti), possono verificarsi race conditions sul database.

---

## Decisione

È stata implementata un'architettura rigorosa conforme alle linee guida **OAuth 2.0 Security Best Current Practice (BCP)** e **RFC 6749**:

1. **Refresh Token Rotation Atomica**:
   - Ad ogni invocazione dell'endpoint `/rest/auth/refresh`, il refresh token presentato viene **immediatamente revocato** (`revoked = true`).
   - Viene contestualmente emesso un nuovo refresh token valido (32 byte casuali crittografici) associato allo stesso `family_id`.
2. **Rilevamento del Riutilizzo e Revoca a Catena (Reuse Detection)**:
   - Se il server riceve una richiesta di refresh specificando un token già marcato come `revoked`, scatta un allarme di sicurezza per furto di credenziali.
   - Il metodo [IRefreshTokenRepository.revokeFamily](file:///Users/gius03/Progetti/SpendyProject/SpendyApp/AuthMicroService/src/main/java/com/spendy/auth/Repository/IRefreshTokenRepository.java#L40) invalida istantaneamente **tutti i token appartenenti a quella famiglia di sessione**, bloccando sia l'attaccante che l'utente legittimo e costringendo a un nuovo login con password.
3. **Hashing Crittografico SHA-256 nel Database**:
   - I token non vengono mai salvati in chiaro: il valore originale viene comunicato al client e nel database viene persistito unicamente il digest SHA-256 (`token_hash`), con la colonna `token` impostata a `NULL` (migrazioni Flyway `V2` e `V3`).
4. **Locking Pessimistico Concorrente**:
   - La lettura del token avviene tramite query `@Lock(LockModeType.PESSIMISTIC_WRITE)` (`findByTokenHashForUpdate`) per serializzare le richieste concorrenti e prevenire race conditions.

---

## Alternative Considerate

- **Refresh Token statici senza rotazione**: Scartata perché considerata non conforme ai moderni standard di sicurezza OWASP / OAuth 2.0.
- **Salvataggio delle sessioni in cache volatile Redis**: Sebbene valida per scalabilità orizzontale, è stata preferita la persistenza su MySQL con Flyway per mantenere l'architettura autosufficiente e priva di dipendenze infrastrutturali esterne obbligatorie in ambienti ridotti come Hugging Face Spaces.

---

## Conseguenze

### Positive:
- **Resilienza al Furto di Token**: Qualsiasi riutilizzo illecito di un token precedentemente ruotato neutralizza l'intera sessione dell'attaccante.
- **Protezione del Database (Data-at-Rest)**: L'intrusione nel database `spendy_auth` non fornisce all'attaccante token di refresh utilizzabili (sono conservati solo gli hash SHA-256 monodirezionali).
- **Assenza di Race Condition**: Il locking pessimistico a livello riga assicura la corretta serializzazione delle chiamate concorrenti.

### Negative e Trade-off:
- La rotazione frequente produce più record nella tabella `refresh_tokens`, rendendo consigliabile una procedura schedulata di pulizia (housekeeping/pruning) per i token scaduti e revocati da lungo tempo.
- Eventuali ritardi di rete estremi o disconnessioni del client durante la rotazione potrebbero richiedere all'utente di ripetere il login qualora il client perda il nuovo token emesso.
