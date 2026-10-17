# 4. Strategia di Categorizzazione Spese Ibrida: OpenNLP MaxEnt e Fallback Euristico

- **Stato**: Accettata
- **Data**: 2026-01-30
- **Decisori**: Spendy Core Team
- **Commit di Riferimento**: `778aa9e` (*Add SmartCategorizerService for automatic expense categorization and integrate with ExpenseImportService*), `9e2a59e` (*Refactor model file paths in ExpenseModelTrainer and SmartCategorizerService*)

---

## Contesto

L'applicazione consente l'importazione massiva di estratti conto bancari (Revolut, Intesa Sanpaolo, N26, ecc.) contenenti centinaia di transazioni con descrizioni grezze (es. `"PAGAMENTO POS MC DONALDS"`, `"ADDEBITO NETFLIX COM"`, `"CONAD VIA ROMA"`).
La classificazione manuale di ciascuna riga comporterebbe un attrito inaccettabile per l'utente.
Era necessario implementare un motore di categorizzazione automatica in grado di:
1. Operare a bassissima latenza (millisecondi per transazione durante l'importazione di migliaia di righe).
2. Funzionare offline all'interno del container senza dipendere da API esterne a pagamento (es. OpenAI, Claude) e senza trasmettere dati finanziari riservati degli utenti verso provider terzi.
3. Garantire la classificazione anche in caso di mancata disponibilità o incertezza del modello statistico.

---

## Decisione

È stata adottata una **strategia di intelligenza ibrida a doppio livello** all'interno di `ExpenseMicroService`:

1. **Livello Primario: Modello Machine Learning Apache OpenNLP (MaxEnt)**:
   - Implementato in [SmartCategorizerService.java](../../ExpenseMicroService/src/main/java/com/spendyteam/expense/Service/SmartCategorizerService.java).
   - Utilizza l'algoritmo di classificazione a Massima Entropia (`DocumentCategorizerME`) addestrato tramite [ExpenseModelTrainer.java](../../ExpenseMicroService/src/main/java/com/spendyteam/expense/Training/ExpenseModelTrainer.java) su dataset di transazioni bancarie (`100` iterazioni, cutoff `0`).
   - Il modello binario pre-addestrato `expense-model.bin` viene caricato direttamente dalle risorse dell'applicazione all'avvio (`@PostConstruct`).
2. **Livello Secondario: Fallback Euristico Deterministico**:
   - Implementato in [ExpenseClassifier.java](../../ExpenseMicroService/src/main/java/com/spendyteam/expense/Utility/ExpenseClassifier.java).
   - Se il modello OpenNLP non è caricato, genera un'eccezione imprevista o la descrizione è vuota, il sistema interroga un dizionario statico di parole chiave categorizzate (*Supermercati e Alimentari*, *Ristorazione e Bar*, *Abbonamenti e Servizi Digitali*, *Trasporti*, *Carburante e Auto*, *Alloggi e Viaggi*, *Shopping e Abbigliamento*, *Pagamenti e Trasferimenti*).

---

## Alternative Considerate

- **Integrazione con LLM Cloud Esterno (es. OpenAI GPT-4o-mini)**: Scartata per motivi di riservatezza dei dati finanziari (GDPR), latenza di rete elevata (centinaia di millisecondi per singola chiamata che rallenterebbero l'import CSV) e costi ricorrenti per token.
- **Solo Classificazione Euristica basata su Regex/Keyword**: Scartata perché rigida, incapace di generalizzare variazioni lessicali nelle causali bancarie e onerosa da manutenere manualmente per ogni nuovo esercente.

---

## Conseguenze

### Positive:
- **Latenza Quasi Istantanea**: La classificazione avviene in memoria in microsecondi per riga, permettendo l'elaborazione parallela di migliaia di righe CSV in pochi secondi.
- **Privacy e Conformità GDPR**: Nessun dato bancario o descrittivo dell'utente abbandona il confine del server locale.
- **Tolleranza ai Guasti (Graceful Degradation)**: Qualora il file binario del modello sia assente in fase di sviluppo, il fallback euristico assicura che il sistema non interrompa l'importazione.

### Negative e Trade-off:
- La qualità della categorizzazione OpenNLP dipende strettamente dalla ricchezza del dataset di addestramento iniziale (`dataset.txt`).
- Per introdurre nuove categorie è necessario rieseguire l'addestramento offline tramite `ExpenseModelTrainer` e ri-impacchettare il file `expense-model.bin` nel build Maven.
