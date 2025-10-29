
/*Logica per il profilo utente*/

const utente = {
    immagineProfilo: "user-solid.svg"
};

const userLink = document.getElementById('user');

// Crea l'immagine
const img = document.createElement('img');
img.src = utente.immagineProfilo;
img.className = 'profile-img';

// Pulisce il contenuto esistente (opzionale)
userLink.innerHTML = '';

// Inserisce immagine
userLink.appendChild(img);

const houseId = localStorage.getItem("houseId");
console.log("House ID:", houseId);

document.querySelector('.nav_links li:nth-child(1) a').href = 'http://localhost:8080/dashPage.html';
document.querySelector('.nav_links li:nth-child(2) a').href = 'http://localhost:8080/expensePage.html';
document.querySelector('.nav_links li:nth-child(3) a').href = 'http://localhost:8080/shiftPage.html';
document.querySelector('.nav_links li:nth-child(4) a').href = 'http://localhost:8080/notYet.html';

const expenses = [];
let coinquilini = [];
function local()
{
    let user=JSON.parse(localStorage.getItem("listCoiquy"));
    user.forEach((item) => {
        //const userObj = JSON.parse(item);
        coinquilini.push(item);
        console.log(item);
    })
}
console.log(coinquilini);

const descriptionInput = document.getElementById("description");
const amountInput = document.getElementById("amount");
let payerInput = document.getElementById("option")
const categorySelect = document.getElementById("category");
const addBtn = document.getElementById("add-btn");
const expenseList = document.getElementById("expense-list");
const totalSpan = document.getElementById("total");
const balancesDiv = document.getElementById("balances");

function addExpense() {
    const description = descriptionInput.value.trim();
    const amountStr = amountInput.value.trim().replace(",", ".");
    const amount = parseFloat(amountStr);
    const createdBy = payerInput.value.trim();
    const category = categorySelect.value;
    const selectedParticipants = Array.from(document.querySelectorAll('input[name="participants"]:checked')).map(cb => cb.value);
    console.log("Selected Participants:", selectedParticipants);
    // Validazione dei campi
    if (!description || !amountStr || isNaN(amount) || !createdBy || !category || selectedParticipants.length === 0) {
        alert("Per favore, compila tutti i campi correttamente.");
        return;
    }

    fetch('http://localhost:8080/Expense/rest/expense/createExpense', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${localStorage.getItem("token")}`
        },
        body: JSON.stringify({

            description: description,
            amount: amount,
            createdBy: createdBy,
            category: category,
            participants: selectedParticipants,
            houseId: houseId
        })
    })
        .then(response => {
            if (!response.ok) {
                if (response.status === 400) {
                    alert("❌ Errore: uno o più campi non sono stati compilati correttamente.");
                    throw new Error('Errore durante la creazione della spesa.');
                }
                throw new Error('Errore durante la creazione della spesa.');
            }
            return response.json();
        })
        .then(data => {
            console.log('Spesa creata con successo:', data);
            expenses.push(data.expenses); // Aggiungi la spesa all'array
            renderSingleExpense(data.expenses, expenses.length - 1); // Usa funzione centralizzata per il rendering
            updateTotal(); // Aggiorna il totale dopo l'aggiunta
            calculateDebts(); // Ricalcola i debiti dopo l'aggiunta
        })
        .catch(error => {
            console.error('Errore:', error);
        });

    descriptionInput.value = "";
    amountInput.value = "";
    payerInput.value = "";
    categorySelect.value = "";
    balancesDiv.innerHTML = "<p>ℹ️ Nessun calcolo effettuato</p>";
}

function renderSingleExpense(expense, index) {
    const li = document.createElement("li-expense");
    li.classList.toggle("paid", expense.paid);

    const detailsDiv = document.createElement("div");
    detailsDiv.classList.add("details");
    detailsDiv.innerHTML = `
        <span class="title">${expense.description}</span>
        <span class="meta">🍂 ${expense.category} • 🙋 ${expense.createdBy}</span>`;

    const amountDiv = document.createElement("div");
    amountDiv.classList.add("amount");
    amountDiv.textContent = `€${expense.amount.toFixed(2)}`;

    const paidBtn = document.createElement("button");
    paidBtn.textContent = expense.status === "SETTLED" ? "✅ Pagata" : "✔️ Salda";
    paidBtn.disabled = expense.status === "SETTLED";
    paidBtn.classList.add("paid-btn");

    paidBtn.addEventListener("click", () => {
        // Chiama l'API per aggiornare lo stato dell'expense
        fetch(`http://localhost:8080/Expense/rest/expense/updateExpenseStatus`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${localStorage.getItem("token")}` // Aggiungi il token di autenticazione
            },
            body: JSON.stringify({
                expenseId: expense.id
            })
        })
            .then(response => {
                if (response.ok) {
                    expenses[index].status = "SETTLED";
                    li.classList.add("paid");
                    paidBtn.textContent = "✅ Pagata";
                    paidBtn.disabled = true;
                    updateTotal();
                    calculateDebts();
                }
            })
            .catch(error => console.error('Errore nell\'aggiornamento dello stato:', error));
    });

    li.appendChild(detailsDiv);
    li.appendChild(amountDiv);
    li.appendChild(paidBtn);

    expenseList.appendChild(li);
    return li;
}


async function retriveExpenses() {
    expenseList.innerHTML = ""; // pulisci DOM

    await fetch(`http://localhost:8080/Expense/rest/expense/getAllExpenses?houseId=${localStorage.getItem("houseId")}`,{
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${localStorage.getItem("token")}` // Aggiungi il token di autenticazione
        }
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Errore durante il recupero delle spese.');
            }
            return response.json();
        })
        .then(data => {
            console.log('Spese recuperate con successo:', data);
            if (Array.isArray(data.expenses)) {
                data.expenses.forEach((expense, index) => {
                    expenses.push(expense);
                    renderSingleExpense(expense, index); // usa funzione centralizzata
                });
            }
        })
        .catch(error => {
            console.error('Errore:', error);
        });
}


function updateTotal() {
    const total = expenses
        .filter(exp => exp.status !== "SETTLED") // Filtra le spese non pagate
        .reduce((sum, exp) => sum + exp.amount, 0);
    totalSpan.textContent = `€${total.toFixed(2)}`;
}

async function retriveCoinquys() {
    try {
        // Participants Checkbox (scelta multipla)
        const container = document.getElementById('participants-container');
        container.innerHTML = ""; // Pulisce eventuali contenuti precedenti

        const participantsLabel = document.createElement('label');
        participantsLabel.textContent = "👥 Chi partecipa ?";
        participantsLabel.style.display = "block";
        participantsLabel.style.marginLeft= "2px";
        participantsLabel.style.fontWeight = "bold";
        participantsLabel.style.marginBottom = "8px";
        container.appendChild(participantsLabel);

        const participantsGrid = document.createElement('div');
        participantsGrid.style.display = "grid";
        participantsGrid.style.gridTemplateColumns = "1fr 1fr";
        participantsGrid.style.gap = "8px";
        participantsGrid.style.maxHeight = "100px"; // Limita altezza visibile
        participantsGrid.style.overflowY = "auto";  // Scroll verticale
        participantsGrid.style.paddingRight = "6px"; // Spazio per scrollbar
        participantsGrid.style.border = "1px solid #ccc"; // (Opzionale) per chiarezza
        participantsGrid.style.borderRadius = "8px";

        coinquilini.forEach(coinquilino => {
            const div = document.createElement('div');
            div.style.display = "flex";
            div.style.alignItems = "center";
            div.style.background = "#f4f4f8";
            div.style.borderRadius = "8px";
            div.style.padding = "6px 10px";
            div.style.boxShadow = "0 1px 3px rgba(0,0,0,0.04)";
            div.style.transition = "background 0.2s";

            const checkbox = document.createElement('input');
            checkbox.type = "checkbox";
            checkbox.name = "participants";
            checkbox.value = coinquilino.username;
            checkbox.style.marginRight = "8px";
            checkbox.style.accentColor = "#29297f";

            const label = document.createElement('label');
            label.textContent = coinquilino.username;
            label.style.cursor = "pointer";
            label.style.userSelect = "none";

            div.appendChild(checkbox);
            div.appendChild(label);
            participantsGrid.appendChild(div);
        });
        container.appendChild(participantsGrid);

        //Payer Select
        const containerSelect = document.getElementById('payer-container');
        containerSelect.innerHTML = ""; // Pulisce eventuali contenuti precedenti
        const selectPayer = document.createElement('select');
        selectPayer.id = "payer";
        selectPayer.name = "payer";
        selectPayer.style.width = "100%";
        const defaultOption = document.createElement('option');
        defaultOption.value = "";
        defaultOption.textContent = "🙋 Chi ha pagato ?";
        selectPayer.appendChild(defaultOption);
        coinquilini.forEach(coinquilino => {
            const option = document.createElement('option');
            option.value = coinquilino.username;
            option.textContent = coinquilino.username;
            selectPayer.appendChild(option);
        });
        containerSelect.appendChild(selectPayer);

        // Funzione per sincronizzare checkbox con select
        function syncCheckboxes() {
            const payerValue = selectPayer.value;
            const checkboxes = container.querySelectorAll('input[name="participants"]');
            checkboxes.forEach(cb => {
                // Se il checkbox corrisponde al payer, lo seleziono di default
                cb.checked = (cb.value === payerValue);
                cb.disabled = (cb.value === payerValue); // Disabilita il checkbox del payer
            });
        }

        selectPayer.addEventListener('change', syncCheckboxes);
    } catch (error) {
        console.error('Errore nel recupero dei coinquilini:', error);
        alert("Errore durante il caricamento dei coinquilini.");
    }
}

// Funzione riutilizzabile per calcolare e mostrare i debiti
function calculateDebts() {
    fetch("http://localhost:8080/Expense/rest/expense/calculateDebt", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            'Authorization': `Bearer ${localStorage.getItem("token")}` // Aggiungi il token di autenticazione
        },
        body: JSON.stringify({
            houseId: houseId
        })
    })
        .then(response => response.json())
        .then(data => {
            const balancesDiv = document.getElementById("balances");
            balancesDiv.innerHTML = "";

            if (!data.debts || data.debts.length === 0) {
                balancesDiv.innerHTML = "<p>ℹ️ Nessun debito trovato.</p>";
                return;
            }

            data.debts.forEach(debt => {
                const debtorNames = Object.keys(debt.participants);
                const amounts = Object.values(debt.participants);

                const allSameAmount = amounts.every(amount => amount === amounts[0]);
                let message = "";

                if (allSameAmount) {
                    // Gestione della grammatica in base al numero di nomi
                    const formattedAmount = parseFloat(amounts[0]).toFixed(2);
                    if (debtorNames.length === 1) {
                        message = `💸 ${capitalize(debtorNames[0])} deve ${formattedAmount}$ a ${debt.createdBy}`;
                    } else if (debtorNames.length === 2) {
                        const formattedNames = debtorNames.map(n => capitalize(n)).join(" e ");
                        message = `💸 ${formattedNames} devono ${formattedAmount}$ a ${debt.createdBy}`;
                    } else {
                        // Per più di 2 nomi, usa la virgola e "e" prima dell'ultimo
                        const capitalizedNames = debtorNames.map(n => capitalize(n));
                        const lastIndex = capitalizedNames.length - 1;
                        const namesExceptLast = capitalizedNames.slice(0, lastIndex).join(", ");
                        const lastName = capitalizedNames[lastIndex];
                        message = `💸 ${namesExceptLast} e ${lastName} devono ${formattedAmount}$ a ${debt.createdBy}`;
                    }
                } else {
                    const lines = debtorNames.map(name => {
                        const formattedAmount = parseFloat(debt.participants[name]).toFixed(2);
                        return `💸 ${capitalize(name)} deve ${formattedAmount}$ a ${debt.createdBy}`;
                    });
                    message = lines.join("<br>");
                }

                const p = document.createElement("p");
                p.innerHTML = message;
                balancesDiv.appendChild(p);
            });
        })

        .catch(error => {
            console.error("Errore nel calcolo dei debiti:", error);
            document.getElementById("balances").innerHTML = "<p>⚠️ Errore nella richiesta al server.</p>";
        });

    function capitalize(str) {
        return str.charAt(0).toUpperCase() + str.slice(1);
    }
}


//per inizializzazione dei contenuti della pagina nel caso ci sono
//delle informazioni salvate nel DB
document.addEventListener('DOMContentLoaded', async () => {
    local()
    await retriveCoinquys();
    payerInput = document.getElementById("payer");
    await retriveExpenses();
    await calculateDebts();
    updateTotal();
});

addBtn.addEventListener("click", addExpense);
