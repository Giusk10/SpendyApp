// Funzione per mostrare il form di registrazione
function showRegister() {
    document.getElementById("loginForm").classList.add("hidden");
    document.getElementById("registerForm").classList.remove("hidden");
    document.getElementById("formTitle").textContent = "Registrazione";
}

// Funzione per mostrare il form di login
function showLogin() {
    document.getElementById("registerForm").classList.add("hidden");
    document.getElementById("loginForm").classList.remove("hidden");
    document.getElementById("formTitle").textContent = "Login";
}

// Funzione per reindirizzare alla pagina principale
function redirectToHouse() {
    const token = localStorage.getItem("token");
    if (token) {
        fetch("http://localhost:8080/Auth/rest/client/house", {
            method: "GET",
            headers: {
                "Authorization": "Bearer " + token
            }
        })
            .then(response => {
                if (response.ok) {
                    return response.json();
                } else {
                    throw new Error("Errore: " + response.statusText);
                }
            })
            .then(data => {
                const link = data["path"];
                if (link) {
                    window.location.href = link;
                } else {
                    alert("Errore: 'path' non trovato nella risposta");
                }
            })
            .catch(err => {
                alert(err.message);
            });
    } else {
        alert("Token non trovato. Assicurati di essere autenticato.");
    }
}


// Aggiungi un listener per il click sul link "Registrati"
document.querySelector("#registerForm button").addEventListener("click", async () => {
    const inputs = document.querySelectorAll("#registerForm input");
    const data = {
        name: inputs[0].value,
        surname: inputs[1].value,
        username: inputs[2].value,
        email: inputs[3].value,
        password: inputs[4].value,
    };

    try {
        const res = await fetch("http://localhost:8080/Auth/rest/auth/register", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify(data),
        });

        const result = await res.text();
        if (res.ok) {
            var out = document.getElementById("outputreg");
            out.innerHTML = "✅ Registrazione completata!";
            setTimeout(() => {
                showLogin()
            }, 400);
        } else {
            var out = document.getElementById("outputreg");
            out.innerHTML = "❗️" + result;
        }
    } catch (error) {
        var out = document.getElementById("outputreg");
        out.innerHTML = "❗️" + error.message;
    }
});

// Aggiungi un listener per il click sul link "Hai già un account?"
document.querySelector("#loginForm").addEventListener("submit", async (event) => {
    event.preventDefault(); // Previene il comportamento predefinito del form
    const inputs = document.querySelectorAll("#loginForm input");
    const data = {
        username: inputs[0].value,
        password: inputs[1].value,
    };

    try {
        const res = await fetch(`http://localhost:8080/Auth/rest/auth/login`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify(data),
        });

        const result = await res.text();
        if (res.ok) {
            var out = document.getElementById("outputlog");
            out.innerHTML = "✅ Login effettuato!";
            const json = JSON.parse(result);
            const token = json["token"];
            localStorage.setItem("token", token);
            redirectToHouse();
        } else {
            var out = document.getElementById("outputlog");
            out.innerHTML = "❗️" + result;
        }
    } catch (error) {
        var out = document.getElementById("outputlog");
        out.innerHTML = "❗️" + error.message;
    }
});

// Aggiungi un listener per il click sul link "Hai già un account?"
document.querySelector("#loginForm button").addEventListener("click", async (event) => {
    event.preventDefault(); // Previene il doppio invio del form
    document.querySelector("#loginForm").dispatchEvent(new Event("submit"));
});

