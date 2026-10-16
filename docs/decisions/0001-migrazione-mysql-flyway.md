# 1. Migrazione della Persistenza da MongoDB a MySQL 8.x con Supporto Flyway

- **Stato**: Accettata
- **Data**: 2026-10-15
- **Decisori**: Spendy Core Team
- **Commit di Riferimento**: `95abbe8` (*refactor: migrate persistence layers from MongoDB to MySQL with Flyway support*)

---

## Contesto

Nelle prime iterazioni dell'architettura di SpendyApp, la persistenza dei dati era affidata a MongoDB (NoSQL document-oriented). Tuttavia, l'evoluzione dei requisiti funzionali ha evidenziato diverse limitazioni critiche:
1. Le transazioni finanziarie e i saldi delle spese richiedono forti garanzie di integrità referenziale e consistenza ACID.
2. La gestione delle sessioni utenti necessita di vincoli relazionali formali (chiave esterna tra refresh token e utente con eliminazione a cascata `ON DELETE CASCADE`).
3. L'assenza di schemi DDL rigidi in MongoDB comportava il rischio di inconsistenze tra record importati in epoche diverse.
4. Mancava uno strumento standardizzato e riproducibile per tracciare le modifiche allo schema dati nei vari ambienti di deployment (locale, CI/CD, produzione).

---

## Decisione

È stata deliberata la completa migrazione del layer di persistenza a **MySQL 8.x** (nello specifico versione 8.4 LTS in container Docker), adottando:
1. **Spring Data JPA** e **Hibernate 6** per la mappatura object-relational, con generazione automatica delle query e supporto a batch inserts (`hibernate.jdbc.batch_size=500`).
2. **Flyway** come motore di database migration automatica all'avvio dell'applicazione (`spring.flyway.enabled=true`, `spring.jpa.hibernate.ddl-auto=validate`).
3. **Due schemi database separati**:
   - `spendy_auth` per la gestione di identità e token di sessione.
   - `spendy_expense` per la memorizzazione e aggregazione delle transazioni contabili.

---

## Alternative Considerate

- **Mantenimento di MongoDB con transazioni multi-documento**: Rifiutata a causa dell'overhead computazionale, della complessità di gestione di un replica set obbligatorio per le transazioni e della mancanza di vincoli referenziali a livello engine.
- **PostgreSQL**: Alternativa valida e comparabile, ma MySQL 8.4 è stato scelto per uniformità con lo stack di produzione preesistente, la disponibilità su Hugging Face Spaces e l'efficienza con le query di raggruppamento temporale (`DATE_FORMAT`).

---

## Conseguenze

### Positive:
- **Integrità Referenziale Garantita**: I vincoli di chiave esterna e unicità (`UNIQUE INDEX`) impediscono duplicazioni di username ed email a livello database.
- **Evoluzione dello Schema Tracciabile**: Ogni modifica DDL è versionata in file SQL immutabili (`V1`, `V2`, `V3`) controllati da Git.
- **Ottimizzazione delle Query di Reportistica**: Le aggregazioni mensili delle uscite avvengono direttamente nell'engine MySQL senza caricare migliaia di record in memoria.

### Negative e Trade-off:
- Necessità di gestire i tipi data rigorosi (`DATETIME(6)`) e i formati numerici con `DECIMAL(15, 2)` per prevenire errori di arrotondamento floating-point.
- I test di integrazione in ambiente CI/CD richiedono la configurazione di un database compatibile in memoria (H2 in modalità MySQL) o un container dedicato.
