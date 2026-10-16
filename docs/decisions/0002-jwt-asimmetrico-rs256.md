# 2. Transizione da HMAC Simmetrico ad Asimmetrico RSA (RS256) per JWT

- **Stato**: Accettata
- **Data**: 2026-10-16
- **Decisori**: Spendy Core Team
- **Commit di Riferimento**: `f2d9c41` (*refactor: migrate authentication to asymmetric RSA JWT and update microservice security configs*), `a90e372` (*fix: harden JWT key resolution, prohibit ephemeral keys in production*)

---

## Contesto

In precedenza, l'autenticazione JWT tra i microservizi impiegava una firma simmetrica basata su segreto condiviso (algoritmo HMAC SHA-256 / `HS256`).
Questa configurazione presentava gravi criticità di sicurezza e scalabilità in un'architettura a microservizi distribuiti:
1. Tutti i componenti (AuthMicroService, ExpenseMicroService, Gateway) dovevano condividere la medesima chiave segreta simmetrica per verificare i token.
2. La compromissione della chiave su un singolo servizio (ad esempio ExpenseMicroService) avrebbe consentito a un attaccante di falsificare token con qualsiasi identità utente, compromettendo l'intera piattaforma.
3. Mancava il supporto nativo alla rotazione delle chiavi senza disservizi per i client attivi.

---

## Decisione

È stata adottata la **crittografia asimmetrica RSA con algoritmo RS256** (RSA Signature with SHA-256, chiavi a 2048 bit):
1. **Separazione dei Ruoli**:
   - `AuthMicroService` custodisce in modo esclusivo la **chiave privata RSA** ed è l'unico componente abilitato a firmare i token.
   - `Gateway` ed `ExpenseMicroService` possiedono esclusivamente la **chiave pubblica RSA** per convalidare localmente le firme dei token.
2. **Supporto alla Rotazione delle Chiavi (Key Rotation)**:
   - Ciascun token include nell'header il parametro standard `kid` (`JWT_ACTIVE_KEY_ID`, default: `spendy-auth-key-1`).
   - I servizi verificatori supportano la configurazione di chiavi pubbliche precedenti (`JWT_PREVIOUS_PUBLIC_KEYS`) per convalidare i token emessi prima della rotazione.
   - Esposizione pubblica delle chiavi attive tramite endpoint JWKS RFC 7517 (`/rest/auth/jwks.json`).
3. **Divieto di Chiavi Effimere in Produzione**:
   - L'uso di chiavi generate automaticamente in memoria è consentito esclusivamente negli ambienti locali di sviluppo o test (`JWT_ALLOW_EPHEMERAL_KEYS=true`).
   - In produzione, l'applicazione solleva un'eccezione bloccante (`IllegalStateException`) se le chiavi RSA non sono configurate esplicitamente.

---

## Alternative Considerate

- **Mantenimento di HMAC-SHA256 con secret distribuito via Vault**: Scartata perché non elimina il rischio intrinseco di contraffazione dei token qualora un servizio periferico venga compromesso.
- **Validazione sincrona via chiamata REST al Gateway/Auth**: Scartata per evitare colli di bottiglia e latenze di rete per ogni richiesta HTTP in ingresso verso ExpenseMicroService.
- **Crittografia su Curve Ellittiche (ECDSA / ES256)**: Considerata valida, ma scartata a favore di RSA-2048 per la maggiore compatibilità con le librerie Spring Security e Nimbus JOSE utilizzate nel progetto.

---

## Conseguenze

### Positive:
- **Zero-Trust Perimetrale**: La compromissione di un servizio a valle non compromette la capacità di firma dei token.
- **Zero Overhead di Rete**: I microservizi validano i token localmente in frazioni di millisecondo tramite operazione matematica di verifica con la chiave pubblica.
- **Rotazione Senza Downtime**: Possibilità di ruotare la chiave privata e mantenere attive le vecchie chiavi pubbliche per la finestra di validità residua dei token emessi (15 minuti).

### Negative e Trade-off:
- La dimensione dell'header e della firma dei token JWT è leggermente superiore rispetto a HS256 (firme RSA a 2048 bit producono circa 342 caratteri per la sola firma).
- Maggiore complessità di configurazione iniziale (gestione delle stringhe o file PEM `BEGIN RSA PRIVATE KEY` / `BEGIN PUBLIC KEY`).
