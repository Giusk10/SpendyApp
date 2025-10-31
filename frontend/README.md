# Spendy Frontend

Applicazione web moderna in React + Vite che si integra con i microservizi presenti nella repository per offrire:

- autenticazione e registrazione degli utenti tramite `AuthMicroService`
- collegamento del profilo a una casa e recupero dei coinquilini
- importazione di estratti conto in formato CSV verso `ExpenseMicroService`
- dashboard interattiva per analizzare le spese classificate automaticamente

## Prerequisiti

- Node.js >= 18
- npm >= 9
- Backend Spring Boot della repository in esecuzione (gateway su `http://localhost:8080`)

## Avvio in locale

1. Assicurarsi che i microservizi Spring siano in esecuzione. Dalla cartella principale del progetto:

   ```bash
   cd AuthMicroService && mvn spring-boot:run
   ```

   In nuove tab del terminale eseguire anche:

   ```bash
   cd ExpenseMicroService && mvn spring-boot:run
   ```

   ```bash
   cd Gateway && mvn spring-boot:run
   ```

   Il gateway espone le API su `http://localhost:8080`.

2. Installare le dipendenze del frontend ed avviare il server di sviluppo:

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

   L'interfaccia sarà disponibile su `http://localhost:5173`. Tutte le chiamate API verso `/Auth`, `/Expense`, `/Dashboard`, `/Rank`, `/Shift` e `/House` sono automaticamente proxate verso il gateway configurato sulla porta `8080`, evitando problemi di CORS.

3. (Opzionale) Per creare una build ottimizzata pronta per la pubblicazione:

   ```bash
   npm run build
   npm run preview
   ```

   È possibile personalizzare il backend di destinazione impostando la variabile d'ambiente `BACKEND_URL` prima di eseguire `npm run dev` o `npm run preview`.

## Configurazione

- `src/api/httpClient.ts`: gestisce l'istanza Axios e l'iniezione del token JWT
- `src/context/AuthContext.tsx`: conserva il token JWT e lo rende disponibile all'applicazione
- `src/pages/*`: viste principali (login, registrazione, dashboard, importazione, gestione casa)
- `src/components/*`: componenti UI riutilizzabili (navbar, card, grafici)
- `src/utils/expense.ts`: funzioni di normalizzazione e aggregazione delle spese

## Workflow suggerito

1. Avviare i microservizi Spring Boot (`AuthMicroService`, `ExpenseMicroService`, `Gateway`)
2. Registrare un nuovo utente oppure accedere con un account esistente
3. Collegare l'utente a una casa usando il codice generato dal backend
4. Importare un estratto conto in formato CSV dal proprio istituto bancario
5. Esplorare la dashboard con gli insight generati automaticamente

### Dettaglio delle pagine principali

- **Login / Registrazione**: inserire le credenziali, creare un nuovo account se necessario e ricevere automaticamente il token JWT per le chiamate successive.
- **Collegamento casa**: specificare il codice casa generato dal backend per associare il profilo alla propria abitazione. La sezione mostra l'elenco aggiornato dei coinquilini.
- **Coinquilini**: cercare utenti registrati per nome o email ed inviare il codice casa da condividere.
- **Caricamento estratto conto**: caricare file CSV contenenti i movimenti bancari; l'applicazione invierà i dati ad `ExpenseMicroService`, che restituirà le spese classificate automaticamente.
- **Dashboard**: analizzare le spese attraverso grafici (trend mensile, ripartizione per categoria) e tabelle filtrabili; puoi limitare l'analisi a uno specifico mese o a un intervallo di date per confrontare periodi diversi. I dati sono aggiornati in tempo reale dopo ogni importazione.

## Note sull'importazione CSV

Il microservizio delle spese attualmente supporta file in formato CSV (non XLS/XLSX). Se la banca fornisce il file in Excel è sufficiente esportarlo o salvarlo in CSV prima del caricamento.
