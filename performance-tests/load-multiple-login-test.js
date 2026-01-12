import http from 'k6/http';
import { check, sleep } from 'k6';

// Configurazione del test (Identica a prima)
export const options = {
    stages: [
        { duration: '10s', target: 10 }, // Riscaldamento
        { duration: '30s', target: 50 }, // Carico alto (50 utenti contemporanei)
        { duration: '10s', target: 0 },  // Raffreddamento
    ],
    thresholds: {
        // Abbiamo alzato leggermente la soglia a 800ms perché il login è più pesante dell'health check
        http_req_duration: ['p(95)<2600'],
        // Fallisci se più dell'1% delle richieste va in errore (es. server sovraccarico)
        http_req_failed: ['rate<0.01'],
    },
};

export default function () {
    // 1. URL corretto (Passando dal Gateway)
    // Sostituisci Khondor03/Spendy con il tuo URL reale se diverso
    const url = 'https://Khondor03-Spendy.hf.space/Auth/rest/auth/login';

    // 2. Il Payload (Body): I dati che invii
    // IMPORTANTE: Sostituisci con credenziali VALIDE nel tuo DB
    const payload = JSON.stringify({
        username: "GG",
        password: "GG"
    });

    // 3. Gli Headers: Diciamo al server che stiamo mandando JSON
    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    // 4. Esegui la richiesta POST (invece di GET)
    const res = http.post(url, payload, params);

    // 5. Controlli (Asserzioni)
    check(res, {
        'status was 200': (r) => r.status === 200,
        // Controllo opzionale: Verifica che la risposta contenga un token
        'token received': (r) => r.body && r.body.includes('token'),
    });

    // Aspetta 1 secondo prima di riprovare
    sleep(1);
}