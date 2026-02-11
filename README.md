---
title: Spendy
emoji: 🏆
colorFrom: green
colorTo: pink
sdk: docker
pinned: false
license: agpl-3.0
app_port: 7860
---

# 🏠 SpendyApp

**SpendyApp** è una piattaforma moderna a microservizi progettata per la **gestione finanziaria personale**. L'applicazione permette agli utenti di tenere traccia delle proprie spese quotidiane, importare storici bancari e analizzare i flussi di cassa in modo sicuro ed efficiente.

Il sistema integra un motore di **Smart Categorization** basato su Machine Learning (OpenNLP) che classifica automaticamente le transazioni (es. "Ristorante X" → "Alimentari"), riducendo il lavoro manuale per l'utente.

## 🏗 Architettura

Il sistema segue un'architettura a microservizi orchestrata da Spring Cloud Gateway:

* **🛡️ Gateway Service**: Punto di ingresso unico. Gestisce il routing, la sicurezza globale e il rewrite dei path per tutti i servizi sottostanti.
* **🔐 Auth MicroService**: Gestisce l'identità digitale dell'utente. Si occupa della registrazione, del login sicuro e della gestione dei token di sessione (JWT e Refresh Token) per garantire che solo il proprietario dei dati possa accedervi.
* **💸 Expense MicroService**: Il cuore operativo della gestione finanziaria. Permette all'utente di eseguire operazioni CRUD (creazione, lettura, aggiornamento, eliminazione) sulle proprie spese, calcolare totali e importare dati esterni.
* **🧠 ML Engine**: Integrato nel servizio spese, utilizza **Apache OpenNLP** per predire la categoria di spesa basandosi su un modello addestrato, migliorando l'organizzazione del budget personale.

## 🛠 Tech Stack

* **Core**: Java 21, Spring Boot 3.5.0
* **Security**: JWT (JSON Web Tokens), Spring Security Reactive
* **Database**: MongoDB
* **AI/ML**: Apache OpenNLP (Document Categorizer)
* **Build**: Maven
* **Container**: Docker

---

Markdown
---
title: Spendy
emoji: 🏆
colorFrom: green
colorTo: pink
sdk: docker
pinned: false
license: agpl-3.0
app_port: 7860
---

# 🏠 SpendyApp

**SpendyApp** è una piattaforma moderna a microservizi progettata per la **gestione finanziaria personale**. L'applicazione permette agli utenti di tenere traccia delle proprie spese quotidiane, importare storici bancari e analizzare i flussi di cassa in modo sicuro ed efficiente.

Il sistema integra un motore di **Smart Categorization** basato su Machine Learning (OpenNLP) che classifica automaticamente le transazioni (es. "Ristorante X" → "Alimentari"), riducendo il lavoro manuale per l'utente.

## 🏗 Architettura

Il sistema segue un'architettura a microservizi orchestrata da Spring Cloud Gateway:

* **🛡️ Gateway Service**: Punto di ingresso unico. Gestisce il routing, la sicurezza globale e il rewrite dei path per tutti i servizi sottostanti.
* **🔐 Auth MicroService**: Gestisce l'identità digitale dell'utente. Si occupa della registrazione, del login sicuro e della gestione dei token di sessione (JWT e Refresh Token) per garantire che solo il proprietario dei dati possa accedervi.
* **💸 Expense MicroService**: Il cuore operativo della gestione finanziaria. Permette all'utente di eseguire operazioni CRUD (creazione, lettura, aggiornamento, eliminazione) sulle proprie spese, calcolare totali e importare dati esterni.
* **🧠 ML Engine**: Integrato nel servizio spese, utilizza **Apache OpenNLP** per predire la categoria di spesa basandosi su un modello addestrato, migliorando l'organizzazione del budget personale.

## 🛠 Tech Stack

* **Core**: Java 21, Spring Boot 3.5.0
* **Security**: JWT (JSON Web Tokens), Spring Security Reactive
* **Database**: MongoDB
* **AI/ML**: Apache OpenNLP (Document Categorizer)
* **Build**: Maven
* **Container**: Docker

---

## 🚀 API Documentation

Tutte le richieste devono essere effettuate tramite l'**API Gateway**.
**Base URL**: `https://khondor03-Spendy.hf.space`.

### 🔐 Autenticazione (Auth Service)

Prefisso route: `/Auth/auth`
*Gestione del profilo e della sicurezza dell'account personale.*

| Metodo | Endpoint | Descrizione | Body Richiesto / Note |
| :--- | :--- | :--- | :--- |
| **POST** | `/login` | Accesso sicuro | `{ "username": "...", "password": "..." }` <br> (o email). Ritorna `accessToken` e `refreshToken`. |
| **POST** | `/register` | Creazione account personale | `{ "username": "...", "password": "...", "name": "...", "surname": "...", "email": "..." }` |
| **POST** | `/refresh` | Rinnova sessione (Token) | `{ "refreshToken": "..." }` |
| **GET** | `/profile` | Visualizza profilo personale | **Header**: `Authorization: Bearer <token>` |
| **PUT** | `/updateProfile` | Modifica dati personali | **Header**: `Authorization: Bearer <token>` <br> **Body**: Oggetto User aggiornato. |
| **GET** | `/health` | Health Check | Verifica operatività del servizio. |

### 💸 Gestione Spese (Expense Service)

Prefisso route: `/Expense/rest/expense`
*Operazioni sul portafoglio digitale. Tutte le chiamate richiedono l'header `Authorization: Bearer <access_token>`.*

#### Operazioni CRUD

| Metodo | Endpoint | Descrizione | Body Richiesto |
| --- | --- | --- | --- |
| **POST** | `/addExpense` | Aggiunge una nuova spesa | `{ "amount": 20.5, "description": "Spesa Supermercato", "date": "..." }`. |
| **GET** | `/getExpenses` | Lista le spese dell'utente | Nessun body. |
| **POST** | `/updateExpense` | Aggiorna dettagli spesa | JSON completo della spesa con ID. |
| **DELETE** | `/deleteExpense` | Rimuove una spesa | `{ "expenseId": "..." }`. |
| **DELETE** | `/deleteAllExpenses` | Reset totale account | Nessun body. |

#### Filtri e Statistiche

| Metodo | Endpoint | Descrizione | Body Richiesto |
| --- | --- | --- | --- |
| **POST** | `/getExpenseByDate` | Filtra per intervallo temporale | `{ "startedDate": "yyyy-MM-dd", "completedDate": "yyyy-MM-dd" }`. |
| **POST** | `/getExpenseByMonth` | Visualizza spese mensili | `{ "month": "10", "year": "2023" }`. |
| **POST** | `/getMonthlyAmountOfYear` | Report annuale | `{ "year": "2023" }`. |

#### Importazione Dati

| Metodo | Endpoint | Descrizione | Formato |
| --- | --- | --- | --- |
| **POST** | `/import` | **Smart Import CSV**. Carica l'estratto conto bancario. Il sistema analizzerà le descrizioni per categorizzare automaticamente le spese personali. |  |

---

## 🧠 Smart Categorization

Il sistema utilizza un modello `DoccatModel` di OpenNLP per assistere l'utente nella contabilità.

* **File Modello**: `expense-model.bin` (in `/src/main/resources`).
* **Logica**: Quando una descrizione viene inserita o importata (es. "Uber Trip"), il `SmartCategorizerService` la analizza e assegna la categoria (es. "Trasporti"). Se il modello non è sicuro, viene usato un fallback basato su keyword manuali.

## 📄 Licenza

Questo progetto è distribuito sotto la licenza **AGPLv3**. Consulta il file `LICENSE` per maggiori informazioni.
