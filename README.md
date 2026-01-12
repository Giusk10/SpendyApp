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

# SpendyApp 🏠💸

SpendyApp è un'applicazione a microservizi per la gestione delle spese condivise tra coinquilini. Permette di registrare utenti, gestire gruppi (case), tracciare le spese e calcolare i conguagli.

## Architecture 

Il progetto è basato su **Spring Boot** e segue un'architettura a microservizi orchestrata da un API Gateway.

### Moduli Principali
* **Gateway** (Porta 8080): Punto di ingresso unico. Gestisce il routing delle richieste e la sicurezza tramite filtri JWT.
* **AuthMicroService** (Porta 8081): Gestisce la registrazione utenti, il login e l'emissione dei token JWT.
* **ExpenseMicroService** (Porta 8084): Gestisce la logica delle spese, inclusa l'importazione da CSV e la categorizzazione automatica (es. "Ristorazione", "Trasporti").
* **Database**: Ogni microservizio utilizza **MongoDB** per la persistenza dei dati.

## Tech Stack 🛠️

* **Java**: 21
* **Framework**: Spring Boot 3.x (Spring Cloud Gateway, Spring Security)
* **Database**: MongoDB
* **Security**: JWT (JSON Web Tokens) & BCrypt per l'hashing delle password
* **Build Tool**: Maven (con Maven Wrapper)

## Features ✨

* **Autenticazione Sicura**: Registrazione e Login con token JWT.
* **Gestione Spese**: CRUD completo delle spese.
* **Smart Import**: Importazione spese da file CSV con riconoscimento automatico del separatore.
* **Auto-Categorizzazione**: Classificazione automatica delle spese basata su parole chiave (es. "Netflix" -> "Abbonamenti", "Uber" -> "Trasporti").

## License 📄

Distribuito sotto la licenza AGPLv3. Vedi il file `LICENSE` per maggiori informazioni.
