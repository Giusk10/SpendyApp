const houseId = localStorage.getItem('houseId');

const monthYear = document.getElementById("monthYear");
const calendarDays = document.getElementById("calendarDays");
const prevBtn = document.getElementById("prevMonth");
const nextBtn = document.getElementById("nextMonth");

let date = new Date();

function renderCalendar() {
    const year = date.getFullYear();
    const month = date.getMonth();
    const firstDay = new Date(year, month, 1).getDay();
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const lastMonth_lastDay = new Date(year, month, 0).getDate();
    const lastMonth_DaysToAdd = firstDay === 0 ? 6 : firstDay - 1;
    const nextMonth_DaysToAdd = 42 - (daysInMonth + lastMonth_DaysToAdd);

    const monthNames = ["Gennaio", "Febbraio", "Marzo", "Aprile", "Maggio", "Giugno",
        "Luglio", "Agosto", "Settembre", "Ottobre", "Novembre", "Dicembre"];

    monthYear.textContent = `${monthNames[month]} ${year}`;
    calendarDays.innerHTML = "";

    for (let i = lastMonth_DaysToAdd - 1; i >= 0; i--) {
        calendarDays.innerHTML += `<div class="prev-days">${lastMonth_lastDay - i}</div>`;
    }

    for (let day = 1; day <= daysInMonth; day++) {
        calendarDays.innerHTML += `<div class="day">${day}</div>`;
    }

    for (let i = 1; i <= nextMonth_DaysToAdd; i++) {
        calendarDays.innerHTML += `<div class="next-days">${i}</div>`;
    }
}

prevBtn.addEventListener("click", () => {
    date.setMonth(date.getMonth() - 1);
    renderCalendar();
});

nextBtn.addEventListener("click", () => {
    date.setMonth(date.getMonth() + 1);
    renderCalendar();
});

renderCalendar();

// Eventi menu
document.getElementById("btnHome").addEventListener("click", function () {
    window.location.reload();
});

document.getElementById("btnSpese").addEventListener("click", function () {
    fetch("http://localhost:8080/Dashboard/rest/clientDash/spese", {
        method: "GET",
        headers: {
            "Authorization": "Bearer " + localStorage.getItem("token"),
        }
    })
        .then(response => {
            if (!response.ok) throw new Error(`Errore ${response.status}`);
            return response.json();
        })
        .then(data => {
            if (data.path) window.location.href = data.path;
        })
        .catch(err => console.error("Errore nella navigazione:", err));
});

document.getElementById("btnTurni").addEventListener("click", function () {
    fetch("http://localhost:8080/Dashboard/rest/clientDash/turni", {
        method: "GET",
        headers: {
            "Authorization": "Bearer " + localStorage.getItem("token"),
        }
    })
        .then(response => {
            if (!response.ok) throw new Error(`Errore ${response.status}`);
            return response.json();
        })
        .then(data => {
            if (data.path) window.location.href = data.path;
        })
        .catch(err => console.error("Errore nella navigazione:", err));
});

document.getElementById("btnRegole").addEventListener("click", () => {
    window.location.href = "notYet.html";
});


document.addEventListener("DOMContentLoaded", () => {
    retrieveCoinquy()
        .then(() => {
            //retriveTurni();
            retrieveClassifica();
        })
        .catch(err => console.error("Errore inizializzazione:", err));

});


function retrieveCoinquy() {
    return fetch(`http://localhost:8080/Dashboard/rest/dash/retrieveCoinquy?houseId=${houseId}`, {
        method: 'GET',
        headers: {
            "Content-Type": "application/json",
            "Authorization": "Bearer " + localStorage.getItem("token")
        }
    })
        .then(response => response.json())
        .then(data => {
            const coinquyList = document.getElementById("lista-persone");
            coinquyList.innerHTML = "";
            data.forEach(user => {
                const li = document.createElement("li");
                li.className = "persona-item";
                li.innerHTML = `
                <img src="user-solid.svg" alt="${user.name} ${user.surname}" class="persona-img">
                <span>${user.name} ${user.surname}</span>
                 `;

                coinquyList.appendChild(li);
            });
            localStorage.setItem("listCoiquy", JSON.stringify(data));
        })
        .catch(error => console.error('Errore:', error));
}

function retriveTurni() {
    fetch(`http://localhost:8080/Dashboard/rest/dash/retrieveTurni?houseId=${houseId}`, {
        method: 'GET',
        headers: {
            "Content-Type": "application/json",
            "Authorization": "Bearer " + localStorage.getItem("token")
        }
    })
        .then(response => {
            if (!response.ok) throw new Error("Errore nel recupero dei turni");
            return response.json();
        })
        .then(data => {
            console.log("Turni:", data);
            // TODO: mostra turni nel DOM
        })
        .catch(error => console.error("Errore:", error));
}

function retrieveClassifica() {
    const houseId = localStorage.getItem("houseId");
    const listCoiquy = JSON.parse(localStorage.getItem("listCoiquy"));
    console.log(listCoiquy);

    fetch(`http://localhost:8080/Dashboard/rest/dash/retrieveClassifica`, {
        method: "POST",
        headers: {
            "Accept": "application/json",
            "Content-Type": "application/json",
            "Authorization": "Bearer " + localStorage.getItem("token")
        },
        body: JSON.stringify({
            coiquyList: listCoiquy.map(user => ({ username: user.username, houseId: houseId }))
        })
    })
        .then(response => {
            if (!response.ok) throw new Error("Errore HTTP " + response.status);
            return response.json();
        })
        .then(classificaList => {
            const rankContainer = document.getElementById("rank");
            rankContainer.innerHTML = "";

            const entries = Object.values(classificaList || {});

            if (entries.length === 0) {
                rankContainer.textContent = "Nessun dato disponibile.";
                return;
            }

            // **1) Ordino per totalPoint decrescente**
            entries.sort((a, b) => b.totalPoint - a.totalPoint);

            // **2) Itero sull’array già ordinato**
            entries.forEach((item, index) => {
                const nome = item.idCoinquy || "Sconosciuto";
                const punti = item.totalPoint;
                const posizione = index + 1;

                let medaglia = "";
                if (posizione === 1) medaglia = "🥇";
                else if (posizione === 2) medaglia = "🥈";
                else if (posizione === 3) medaglia = "🥉";

                const p = document.createElement("p");
                p.textContent = `${medaglia || posizione + "."} ${nome} - ${punti} punti`;
                rankContainer.appendChild(p);
            });
        })
        .catch(error => {
            console.error("Errore nel recupero classifica:", error);
            document.getElementById("rank").textContent = "Errore nel caricamento.";
        });
}