# SpendyApp Frontend

Interfaccia web moderna per SpendyApp, pensata per dialogare con i microservizi Spring Boot presenti in questa repository.

## Funzionalità principali

- Autenticazione (login/registrazione) tramite `AuthMicroService`.
- Importazione guidata di estratti conto in formato CSV o Excel (conversione automatica in CSV lato browser) verso `ExpenseMicroService`.
- Dashboard dinamica con riepilogo, grafico dell'andamento mensile, distribuzione per categoria e tabelle filtrabili.
- Filtri avanzati per intervallo date o per mese/anno.
- Gestione automatica del token JWT e persistenza in `localStorage`.

## Requisiti

- Node.js >= 18
- npm o pnpm o yarn

## Configurazione

Copiare il file `.env.example` e rinominarlo in `.env`, personalizzando l'URL del gateway se necessario:

```bash
cp .env.example .env
```

Per default le chiamate puntano al gateway Spring (`http://localhost:8080`).

## Avvio in sviluppo

```bash
npm install
npm run dev
```

L'applicazione sarà disponibile su `http://localhost:5173`.

## Build produzione

```bash
npm install
npm run build
```

I file statici pronti per il deploy verranno generati nella cartella `dist/`.

## Integrazione con il backend

| Funzione | Endpoint gateway |
|----------|------------------|
| Login | `POST /Auth/auth/login` |
| Registrazione | `POST /Auth/auth/register` |
| Import spese | `POST /Expense/rest/expense/import` |
| Lista spese | `GET /Expense/rest/expense/getExpenses` |
| Filtra per intervallo | `POST /Expense/rest/expense/getExpenseByDate` |
| Filtra per mese/anno | `POST /Expense/rest/expense/getExpenseByMonth` |
| Aggregati mensili | `POST /Expense/rest/expense/getMonthlyAmountOfYear` |

Il token JWT generato dal microservizio di autenticazione viene memorizzato in `localStorage` e allegato automaticamente (header `Authorization: Bearer ...`) a tutte le richieste successive.

## Struttura cartelle

- `src/components`: componenti UI principali (form di autenticazione, dashboard, grafici, tabelle).
- `src/context`: context React per lo stato di autenticazione.
- `src/hooks`: hook personalizzati per la gestione delle chiamate spese.
- `src/lib`: client Axios centralizzato e definizione delle rotte REST.
- `src/styles`: fogli di stile modulari per componenti e layout.
- `src/utils`: helper per trasformare dati e calcolare metriche.

## Test manuali suggeriti

1. Avviare i microservizi backend (`AuthMicroService`, `ExpenseMicroService`, `Gateway`).
2. Registrare un nuovo utente e verificare il messaggio di conferma.
3. Effettuare il login e controllare la presenza della dashboard.
4. Importare un file CSV/Excel: al termine devono apparire messaggio di successo e nuove righe nella tabella.
5. Utilizzare i filtri per intervallo e mese: la tabella e i grafici devono aggiornarsi.
6. Fare logout e verificare il ritorno alla schermata di autenticazione.
