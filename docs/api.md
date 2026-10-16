# Specifiche API REST di SpendyApp

Questa guida documenta in modo esaustivo tutte le API REST esposte dall'ecosistema **SpendyApp**, descrivendo metodi HTTP, percorsi instradati dal Gateway, payload di richiesta, risposte, codici di stato e requisiti di sicurezza.

Tutte le richieste esterne devono essere indirizzate verso l'**API Gateway**:
- **Ambiente Locale**: `http://localhost:7860` (oppure `http://localhost:8080`)
- **Ambiente Produzione / Hugging Face Spaces**: `https://khondor03-Spendy.hf.space`
- **Interfaccia Swagger UI interattiva**: `/swagger-ui.html`

---

## 1. Convenzioni Globali e Autenticazione

### 1.1. Intestazioni HTTP Standard
| Header | Obbligatorio | Descrizione |
| :--- | :--- | :--- |
| `Content-Type` | Sì (per richieste con body) | Generalmente `application/json` o `multipart/form-data` per upload CSV. |
| `Authorization` | Sì (per endpoint protetti) | Token di accesso formattato come: `Bearer <accessToken>`. |
| `Retry-After` | In risposta a HTTP 429 | Numero di secondi da attendere prima di ripetere la richiesta (generato da [RateLimitingFilter.java](file:///Users/gius03/Progetti/SpendyProject/SpendyApp/AuthMicroService/src/main/java/com/spendy/auth/Security/RateLimitingFilter.java)). |

### 1.2. Mappatura dei Percorsi Gateway
Il Gateway applica le seguenti regole di riscrittura (`RewritePath`):
- Rotta Auth: `http://localhost:7860/Auth/**` &rarr; inoltrata internamente a `http://localhost:8081/**`
- Rotta Expense: `http://localhost:7860/Expense/**` &rarr; inoltrata internamente a `http://localhost:8084/**`

---

## 2. API Gateway Status

### 2.1. Verifica Stato Gateway (Root Health)
- **Metodo**: `GET`
- **Path Gateway**: `/`
- **Classe Controller**: [GatewayApplication.java](file:///Users/gius03/Progetti/SpendyProject/SpendyApp/Gateway/src/main/java/com/spendy/gateway/GatewayApplication.java#L53-L56)
- **Autenticazione richiesta**: No (pubblico)
- **Risposta `200 OK`**:
  ```text
  🟢 Spendy Gateway è attivo e funzionante! (V. Finale)
  ```

---

## 3. Endpoints di Autenticazione (`AuthMicroService`)

Base path esterno via Gateway: `/Auth/rest/auth`  
Classe Controller: [AuthController.java](file:///Users/gius03/Progetti/SpendyProject/SpendyApp/AuthMicroService/src/main/java/com/spendy/auth/Controller/AuthController.java)

### 3.1. Registrazione Utente
Crea un nuovo account nel database relazionale `spendy_auth.users`.
- **Metodo**: `POST`
- **Path**: `/Auth/rest/auth/register`
- **Autenticazione**: Nessuna
- **Rate Limit**: Max 5 richieste/minuto per indirizzo IP
- **Corpo Richiesta (`application/json`)**:
  ```json
  {
    "username": "mario_rossi",
    "password": "Password123!",
    "name": "Mario",
    "surname": "Rossi",
    "email": "mario.rossi@example.com"
  }
  ```
  *Regole di validazione*:
  - `username`: 3-50 caratteri alfanumerici, trattini o underscore (`^[a-zA-Z0-9_.-]+$`).
  - `password`: tra 8 e 128 caratteri.
  - `email`: formato valido (regex `^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$`), max 150 caratteri.
  - `name`, `surname`: opzionali, max 100 caratteri.
- **Risposte**:
  - `200 OK`: `"Registration successful"`
  - `400 Bad Request`: `"Payload di registrazione obbligatorio"` o `"Password deve contenere tra 8 e 128 caratteri"`
  - `401 Unauthorized`: `"Email invalido"`
  - `409 Conflict`: `"User already exists"` (se username o email sono già registrati)
  - `429 Too Many Requests`: `{"error": "Too many requests. Please try again later."}`

### 3.2. Login Utente
Autentica l'utente tramite username (oppure email) e password.
- **Metodo**: `POST`
- **Path**: `/Auth/rest/auth/login`
- **Autenticazione**: Nessuna
- **Rate Limit**: Max 10 richieste/minuto per indirizzo IP
- **Corpo Richiesta (`application/json`)**:
  ```json
  {
    "username": "mario_rossi",
    "password": "Password123!"
  }
  ```
  *Nota: è possibile specificare il campo `"email"` in alternativa a `"username"`.*
- **Risposte**:
  - `200 OK`:
    ```json
    {
      "accessToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6InNwZW5keS1hdXRoLWtleS0xIiwidHlwIjoiSldUIn0...",
      "refreshToken": "xK8mN2pL9qRtUvWz..."
    }
    ```
  - `400 Bad Request`: `"Payload di login obbligatorio"` o `"Password obbligatoria"`
  - `401 Unauthorized`: `"Credenziali errate o non valide"` (restituito in tempo costante anche se l'utente non esiste per mitigare l'account enumeration)
  - `429 Too Many Requests`: Superamento soglia tentativi

### 3.3. Rinnovo Access Token (Token Refresh & Rotation)
Esegue la rotazione atomica del refresh token con verifica contro il riutilizzo (RFC 6749).
- **Metodo**: `POST`
- **Path**: `/Auth/rest/auth/refresh`
- **Autenticazione**: Nessuna (il token è nel body)
- **Rate Limit**: Max 30 richieste/minuto per indirizzo IP
- **Corpo Richiesta (`application/json`)**:
  ```json
  {
    "refreshToken": "xK8mN2pL9qRtUvWz..."
  }
  ```
- **Risposte**:
  - `200 OK`:
    ```json
    {
      "accessToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6InNwZW5keS1hdXRoLWtleS0xIn0...",
      "refreshToken": "nuovo_refresh_token_ruotato..."
    }
    ```
  - `400 Bad Request`: `"Refresh Token missing"`
  - `401 Unauthorized`: `"Refresh Token expired or invalid"` (generato anche in caso di rilevamento riutilizzo, con contestuale revoca dell'intera famiglia di token)

### 3.4. Profilo Utente
Recupera le informazioni dell'utente dal subject del JWT access token.
- **Metodo**: `GET`
- **Path**: `/Auth/rest/auth/profile`
- **Autenticazione**: Richiesta (`Bearer <accessToken>`)
- **Risposte**:
  - `200 OK`:
    ```json
    {
      "username": "mario_rossi",
      "name": "Mario",
      "surname": "Rossi",
      "email": "mario.rossi@example.com"
    }
    ```
  - `401 Unauthorized`: `"Missing or invalid Authorization header"` o `"Invalid access token"`

### 3.5. Aggiornamento Profilo Utente
Aggiorna i dati anagrafici dell'utente collegato al token Bearer.
- **Metodo**: `PUT`
- **Path**: `/Auth/rest/auth/updateProfile`
- **Autenticazione**: Richiesta (`Bearer <accessToken>`)
- **Corpo Richiesta (`application/json`)**:
  ```json
  {
    "name": "Mario Andrea",
    "surname": "Rossi"
  }
  ```
- **Risposte**:
  - `200 OK`: `"Profile updated successfully"`
  - `400 Bad Request`: `"User payload required"` o `"Nome troppo lungo"`
  - `401 Unauthorized`: Token mancante o non valido
  - `404 Not Found`: `"User not found"`

### 3.6. Logout Utente
Invalida il refresh token attivo e revoca l'intera famiglia di sessione associata nel database.
- **Metodo**: `POST`
- **Path**: `/Auth/rest/auth/logout`
- **Autenticazione**: Nessuna
- **Corpo Richiesta (`application/json`)**:
  ```json
  {
    "refreshToken": "xK8mN2pL9qRtUvWz..."
  }
  ```
- **Risposte**:
  - `200 OK`:
    ```json
    {
      "message": "Logged out successfully"
    }
    ```

### 3.7. Set di Chiavi Pubbliche JWKS (RFC 7517)
Espone il set di chiavi crittografiche pubbliche RSA per la verifica distribuita della firma dei token.
- **Metodo**: `GET`
- **Path**: `/Auth/rest/auth/jwks.json` (disponibile anche come alias perimetrale)
- **Autenticazione**: Nessuna (pubblico)
- **Risposte**:
  - `200 OK`:
    ```json
    {
      "keys": [
        {
          "kty": "RSA",
          "use": "sig",
          "alg": "RS256",
          "kid": "spendy-auth-key-1",
          "n": "u1wE...",
          "e": "AQAB"
        }
      ]
    }
    ```

### 3.8. Health Check Servizio Auth
- **Metodo**: `GET`
- **Path**: `/Auth/rest/auth/health`
- **Autenticazione**: Nessuna
- **Risposta `200 OK`**: `"AuthMicroService is running"`

---

## 4. Endpoints Gestione Spese (`ExpenseMicroService`)

Base path esterno via Gateway: `/Expense/rest/expense`  
Classe Controller: [ExpenseController.java](file:///Users/gius03/Progetti/SpendyProject/SpendyApp/ExpenseMicroService/src/main/java/com/spendyteam/expense/Controller/ExpenseController.java)  
*Tutti gli endpoint delle spese richiedono l'header `Authorization: Bearer <accessToken>`.*

### 4.1. Importazione Estratto Conto CSV
Importa un file CSV bancario, ne analizza le righe concorrentemente con Virtual Threads, categorizza le spese via OpenNLP e le persiste a blocchi.
- **Metodo**: `POST`
- **Path**: `/Expense/rest/expense/import`
- **Content-Type**: `multipart/form-data`
- **Autenticazione**: Richiesta (`Bearer <accessToken>`)
- **Form Data**:
  - `file`: allegato file CSV (dimensione massima configurata: 10 MB).
- **Risposte**:
  - `200 OK`: `"Import completato. Importati: 142. Errori Parsing: 0. Errori DB: 0"`
  - `204 No Content`: `"CSV file is empty."`
  - `400 Bad Request`: `"Nessuna riga valida trovata"` o errore di formato header
  - `401 Unauthorized`: `"Invalid or expired token."`

### 4.2. Lista di Tutte le Spese dell'Utente
Restituisce tutte le transazioni registrate per l'utente autenticato, ordinate cronologicamente per data d'inizio crescente.
- **Metodo**: `GET`
- **Path**: `/Expense/rest/expense/getExpenses`
- **Autenticazione**: Richiesta (`Bearer <accessToken>`)
- **Risposte**:
  - `200 OK`:
    ```json
    [
      {
        "id": "65a1234b-5c6d-7e8f-9012-345678abcdef",
        "type": "CARD_PAYMENT",
        "product": "Conad Supermercato",
        "startedDate": "2026-03-15T14:30:00",
        "completedDate": "2026-03-15T14:30:00",
        "description": "PAGAMENTO POS CONAD VIA ROMA",
        "amount": -45.50,
        "fee": 0.00,
        "currency": "EUR",
        "state": "COMPLETED",
        "category": "Supermercati e Alimentari",
        "username": "mario_rossi"
      }
    ]
    ```
  - `204 No Content`: Nessuna spesa presente
  - `401 Unauthorized`: Token non valido o scaduto

### 4.3. Filtro Spese per Intervallo Date
- **Metodo**: `POST`
- **Path**: `/Expense/rest/expense/getExpenseByDate`
- **Autenticazione**: Richiesta (`Bearer <accessToken>`)
- **Corpo Richiesta (`application/json`)**:
  ```json
  {
    "startedDate": "2026-03-01",
    "completedDate": "2026-03-31"
  }
  ```
  *Formati data supportati*: `yyyy-MM-dd`, `yyyy-MM-dd HH:mm:ss`, `yyyy-MM-dd'T'HH:mm:ss`, `dd/MM/yyyy` ed epoch timestamp.
- **Risposte**:
  - `200 OK`: Lista JSON delle entità `Expense` comprese nell'intervallo.
  - `204 No Content`: Nessuna spesa trovata nell'intervallo specificato.
  - `400 Bad Request`: Formato data non valido.

### 4.4. Filtro Spese per Mese e Anno
- **Metodo**: `POST`
- **Path**: `/Expense/rest/expense/getExpenseByMonth`
- **Autenticazione**: Richiesta (`Bearer <accessToken>`)
- **Corpo Richiesta (`application/json`)**:
  ```json
  {
    "month": "03",
    "year": "2026"
  }
  ```
- **Risposte**:
  - `200 OK`: Elenco delle spese del mese indicato.
  - `204 No Content`: Nessun record nel mese indicato.
  - `400 Bad Request`: Mese o anno non validi.

### 4.5. Aggregazione Spese Mensili dell'Anno
Calcola i totali delle uscite suddivisi per ciascun mese dell'anno direttamente nel database relazionale.
- **Metodo**: `POST`
- **Path**: `/Expense/rest/expense/getMonthlyAmountOfYear`
- **Autenticazione**: Richiesta (`Bearer <accessToken>`)
- **Corpo Richiesta (`application/json`)**:
  ```json
  {
    "year": "2026"
  }
  ```
- **Risposte**:
  - `200 OK`:
    ```json
    {
      "2026-01": -650.40,
      "2026-02": -820.15,
      "2026-03": -540.00
    }
    ```
  - `204 No Content`: Nessuna transazione registrata nell'anno.
  - `400 Bad Request`: Anno non valido.

### 4.6. Creazione Singola Spesa
Inserisce manualmente una nuova transazione. Se il campo `category` non viene valorizzato, il categorizzatore NLP tenterà di assegnarla automaticamente in base a `description`.
- **Metodo**: `POST`
- **Path**: `/Expense/rest/expense/addExpense`
- **Autenticazione**: Richiesta (`Bearer <accessToken>`)
- **Corpo Richiesta (`application/json`)**:
  ```json
  {
    "type": "CARD_PAYMENT",
    "product": "Ristorante Pizzeria",
    "startedDate": "2026-03-20 20:30:00",
    "completedDate": "2026-03-20 20:30:00",
    "description": "Cena con colleghi",
    "amount": "-35.00",
    "fee": "0.00",
    "currency": "EUR",
    "state": "COMPLETED"
  }
  ```
- **Risposte**:
  - `201 Created`: Oggetto `Expense` creato con ID valorizzato (UUID).
  - `400 Bad Request`: Body mancante o campi invalidi.
  - `401 Unauthorized`: Mancanza o invalidità del token Bearer.

### 4.7. Modifica Singola Spesa
Aggiorna i campi di una transazione esistente di proprietà dell'utente autenticato.
- **Metodo**: `POST`
- **Path**: `/Expense/rest/expense/updateExpense`
- **Autenticazione**: Richiesta (`Bearer <accessToken>`)
- **Corpo Richiesta (`application/json`)**:
  ```json
  {
    "id": "65a1234b-5c6d-7e8f-9012-345678abcdef",
    "description": "Cena sociale rivista",
    "amount": "-38.00"
  }
  ```
- **Risposte**:
  - `200 OK`: Oggetto `Expense` aggiornato.
  - `400 Bad Request`: ID spesa omesso.
  - `403 Forbidden`: L'utente autenticato non è il proprietario della spesa.
  - `404 Not Found`: Spesa non trovata.

### 4.8. Cancellazione Singola Spesa
- **Metodo**: `DELETE`
- **Path**: `/Expense/rest/expense/deleteExpense`
- **Autenticazione**: Richiesta (`Bearer <accessToken>`)
- **Corpo Richiesta (`application/json`)**:
  ```json
  {
    "expenseId": "65a1234b-5c6d-7e8f-9012-345678abcdef"
  }
  ```
- **Risposte**:
  - `200 OK`: `"Expense deleted successfully."`
  - `403 Forbidden`: Tentativo di eliminare una spesa appartenente a un altro utente.
  - `404 Not Found`: Spesa non trovata.

### 4.9. Cancellazione Massiva di Tutte le Spese
Elimina con una singola operazione database (`deleteByUsername`) tutte le spese dell'utente autenticato.
- **Metodo**: `DELETE`
- **Path**: `/Expense/rest/expense/deleteAllExpenses`
- **Autenticazione**: Richiesta (`Bearer <accessToken>`)
- **Risposte**:
  - `200 OK`: `"Deleted 142 expenses for user: mario_rossi"` oppure `"No expenses found for user: mario_rossi"`
  - `401 Unauthorized`: Token mancante o non valido.

### 4.10. Health Check Servizio Spese
- **Metodo**: `POST`
- **Path**: `/Expense/rest/expense/test`
- **Autenticazione**: Richiesta per configurazione di sicurezza Spring (`/rest/expense/**`)
- **Risposta `200 OK`**: `"Expense Microservice is up and running!"`

---

## 5. Corrispondenza con la Specifica OpenAPI

I tre microservizi generano automaticamente i contratti OpenAPI 3.0 via `springdoc-openapi-starter-webmvc-ui` e `springdoc-openapi-starter-webflux-ui`:

| Servizio | Documentazione JSON | UI Swagger Diretta | Attraverso Gateway |
| :--- | :--- | :--- | :--- |
| **API Gateway** | `/v3/api-docs` | `/swagger-ui.html` | `/swagger-ui.html` (Aggregatore globale) |
| **AuthMicroService** | `/v3/api-docs` | `http://localhost:8081/swagger-ui.html` | `/Auth/v3/api-docs` |
| **ExpenseMicroService**| `/v3/api-docs` | `http://localhost:8084/swagger-ui.html` | `/Expense/v3/api-docs` |

### Discrepanze e Note Implementative Rilevate dal Codice:
1. *Endpoint `/rest/expense/test`*: Nel codice di [ExpenseController.java](file:///Users/gius03/Progetti/SpendyProject/SpendyApp/ExpenseMicroService/src/main/java/com/spendyteam/expense/Controller/ExpenseController.java#L574) è annotato con `@PostMapping`, mentre in alcuni client o documentazioni precedenti veniva trattato come GET. Inoltre, ricadendo sotto il path `/rest/expense/**`, è protetto dalla `SecurityFilterChain` di Spring Security e richiede un token valido, a differenza di `/actuator/health` che è pubblicamente accessibile.
2. *Mappatura JAX-RS vs Spring Web*: In [AuthController.java](file:///Users/gius03/Progetti/SpendyProject/SpendyApp/AuthMicroService/src/main/java/com/spendy/auth/Controller/AuthController.java) sono presenti annotazioni ibride sia JAX-RS (`@Path`, `@POST`, `@GET`, `@Produces`) sia Spring Web (`@RestController`, `@PostMapping`, `@GetMapping`). L'applicazione è configurata sia con Jersey che con Spring WebMVC, garantendo piena compatibilità con i client esistenti.
