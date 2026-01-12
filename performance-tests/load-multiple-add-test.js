import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '5s', target: 5 },   // Riscaldamento leggero
        { duration: '20s', target: 20 }, // 20 Utenti che aggiungono spese contemporaneamente
        { duration: '5s', target: 0 },   // Raffreddamento
    ],
    thresholds: {
        http_req_duration: ['p(95)<2000'], // Soglia 2s (include Login + AddExpense)
        http_req_failed: ['rate<0.01'],    // Meno dell'1% di errori
    },
};

export default function () {
    // --- STEP 1: LOGIN ---
    const loginUrl = 'https://Khondor03-Spendy.hf.space/Auth/rest/auth/login';
    const loginPayload = JSON.stringify({
        username: "GG", // O "email" se usi l'email, controlla il tuo DTO
        password: "GG"
    });

    const loginParams = {
        headers: { 'Content-Type': 'application/json' },
    };

    const loginRes = http.post(loginUrl, loginPayload, loginParams);

    // Controlliamo se il login è andato a buon fine
    const isLoginSuccess = check(loginRes, {
        'Login riuscito (200)': (r) => r.status === 200,
    });

    // Se il login fallisce, fermiamo questa iterazione (inutile provare ad aggiungere la spesa)
    if (!isLoginSuccess) {
        console.error(`Login fallito: ${loginRes.status} ${loginRes.body}`);
        return;
    }

    // --- STEP 2: ESTRAZIONE TOKEN ---
    // Assumiamo che il server risponda con JSON: { "token": "ey..." }
    // Se il tuo server risponde SOLO con la stringa del token, usa: loginRes.body
    const authToken = loginRes.json('token');

    // --- STEP 3: AGGIUNGI SPESA (Add Expense) ---
    const expenseUrl = 'https://Khondor03-Spendy.hf.space/Expense/rest/expense/addExpense';

    const expensePayload = JSON.stringify({
        "type": "CARD_PAYMENT",
        "product": "Current",
        "startedDate": "2025-05-31T22:00:00.000Z",
        "completedDate": "2025-05-31T22:00:00.000Z",
        "description": "Capone Pizza Stress Test",
        "amount": "-69.90",
        "fee": "0",
        "currency": "EUR",
        "state": "CONTABILIZZATO"
    });

    const expenseParams = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${authToken}` // <--- QUI INSERIAMO IL TOKEN
        },
    };

    const expenseRes = http.post(expenseUrl, expensePayload, expenseParams);

    // --- STEP 4: VERIFICA ---
    check(expenseRes, {
        'Spesa aggiunta (200)': (r) => r.status === 201,
    });

    sleep(1);
}