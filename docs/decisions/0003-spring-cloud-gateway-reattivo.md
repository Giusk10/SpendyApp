# 3. Adozione di Spring Cloud Gateway Reattivo Netty come Edge Proxy

- **Stato**: Accettata
- **Data**: 2026-01-12 (Aggiornata 2026-10-15)
- **Decisori**: Spendy Core Team
- **Commit di Riferimento**: `272f90b` (*Update service URIs in application properties and add Docker configuration*), `3246f22` (*feat: configure global CORS settings and update OpenAPI server defaults across microservices*)

---

## Contesto

In un'architettura a microservizi aperta a client mobili (iOS) e web client (Single Page Applications):
1. I client non devono essere esposti alla complessità della topologia di rete interna e alle singole porte dei microservizi (`8081`, `8084`).
2. È necessario un unico punto di ingresso per gestire certificati SSL, CORS pre-flight, sanitizzazione delle intestazioni HTTP e documentazione aggregata.
3. Il proxy deve gestire un numero elevato di connessioni simultanee con consumo ridotto di risorse CPU e memoria, in particolare su ambienti containerizzati a risorse contenute (es. Hugging Face Spaces con 2 core e memoria limitata).

---

## Decisione

È stato implementato un modulo dedicato **Gateway** basato su **Spring Cloud Gateway Server WebFlux** (Netty) e **Java 21 Virtual Threads**:
1. **Modello Reattivo Non-Bloccante**: L'architettura ad eventi asincrona basata su Project Reactor e Netty consente la gestione efficiente di migliaia di richieste concorrenti senza dedicare un thread di sistema operativo per ogni connessione client.
2. **Path Rewriting Centralizzato**:
   - `/Auth/(?<segment>.*)` &rarr; `/${segment}`
   - `/Expense/(?<segment>.*)` &rarr; `/${segment}`
3. **Filtri di Sicurezza Perimetrale**:
   - `RemoveRequestHeader=X-Auth-User` e `RemoveRequestHeader=X-User-Id` per prevenire lo spoofing di header interni.
   - `DedupeResponseHeader` per evitare header CORS duplicati verso i browser.
4. **Aggregazione Documentazione OpenAPI**: Swagger UI aggregata su `/swagger-ui.html` che unisce le definizioni API di tutti i microservizi registrati.

---

## Alternative Considerate

- **Nginx o Envoy Proxy Esterno**: Sebbene eccellenti come performance pure, avrebbero richiesto configurazioni esterne separate, file di configurazione non integrati nel ciclo di build Maven di Spring Boot e una maggiore difficoltà nell'integrazione con Spring Security OAuth2 Reactive Decoder.
- **Spring Cloud Gateway MVC (Servlet blocking)**: Scartato a favore della variante WebFlux per via del consumo di memoria inferiore e del modello a eventi nativo di Netty.

---

## Conseguenze

### Positive:
- **Punto di Ingresso Unificato**: I client comunicano unicamente con l'URL base del Gateway sulla porta `7860`.
- **Basso Consumo di Memoria**: Il runtime Netty ha un footprint di memoria significativamente inferiore rispetto ai tradizionali container Servlet Tomcat.
- **Sicurezza Accoppiata al Codice**: La configurazione di sicurezza reattiva (`SecurityWebFilterChain`) è gestita e testata all'interno del codice Java con strumenti standard (`reactor-test`).

### Negative e Trade-off:
- La programmazione reattiva (WebFlux) richiede una gestione attenta dei filtri per non bloccare l'Event Loop con chiamate I/O sincroniche non protette.
- Maggiore complessità nel troubleshooting dei flussi reattivi in caso di eccezioni nello stack trace rispetto al modello thread-per-request tradizionale.
