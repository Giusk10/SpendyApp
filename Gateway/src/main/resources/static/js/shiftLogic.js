
const token=localStorage.getItem("token");
let coinquilini = [];
let shift =[];

function local()
{
    let user=JSON.parse(localStorage.getItem("listCoiquy"));
    user.forEach((item) => {
        //const userObj = JSON.parse(item);
        coinquilini.push(item);
        console.log(item);
    })
    /*if(localStorage.getItem("shift")!== null)
    {
        let s=JSON.parse(localStorage.getItem("shift"));
        s.forEach((item) => {
            //const userObj = JSON.parse(item);
            shift.push(item);
            console.log(item);
        })
    }*/
}
const houseId = localStorage.getItem("houseId");

document.querySelector('.nav_links li:nth-child(1) a').href = 'http://localhost:8080/dashPage.html';
document.querySelector('.nav_links li:nth-child(2) a').href = 'http://localhost:8080/expensePage.html';
document.querySelector('.nav_links li:nth-child(3) a').href = 'http://localhost:8080/shiftPage.html';
document.querySelector('.nav_links li:nth-child(4) a').href = 'http://localhost:8080/notYet.html';

function unavailableForm()
{
    const form = document.querySelector(".form-container");

    form.addEventListener("submit", (e) => {
        e.preventDefault();

        var selectedTenantId = document.getElementById("coinquys-select").value;
        const startTime = document.getElementById("start-time").value;
        const endTime = document.getElementById("end-time").value;

        if (!selectedTenantId || !startTime || !endTime) {
            alert("⚠️ Tutti i campi sono obbligatori!");
            return;
        }
        console.log(token);
        console.log("Selected Tenant ID:", selectedTenantId);
        fetch(`http://localhost:8080/Shift/rest/unAvailability/addAvailability`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': "Bearer " + token
            },
            body: JSON.stringify({
                username: selectedTenantId,
                start: startTime,
                end: endTime,
                houseId: houseId
            })
        })
            .then(response => {
                if (response.ok) {
                    alert("✅ Indisponibilità salvata con successo!");
                    form.reset();
                }
                else
                {
                    return response.text().then(text => { throw new Error(text); });
                }
            })
            .catch(error => {
                console.error("Errore durante la richiesta:", error);
                alert("⚠️ Errore: " + error.message);
            });
    });
}

function fetchPlanning()
{
    const spinnerDiv = document.getElementById("loadingSpinner");
    spinnerDiv.style.display = "block";

    // Controllo: shift deve contenere elementi validi
    let problemId=null;
    if (shift.length !== 0) {
        problemId = shift[0].problemId;
        console.log("📌 problemId usato per il planning:", problemId);
    }


    fetch(`http://localhost:8080/Shift/rest/calendar/getPlanning`, {
        method: "POST",
        headers: {
            'Content-Type': 'application/json',
            'Authorization': "Bearer " + token
        },
        body: JSON.stringify({
            houseId: houseId,
            problemId: problemId
        })
    })
        .then(response => {
            console.log(response.statusText);
            if (!response.ok && response.status !== 302)
            {
                return response.text().then(text => { throw new Error(text); });
            }
            return response.json();
        })
        .then(data => {
            console.log(data)
            renderCalendar(data);
        })
        .catch(error => {
            console.error("Errore durante la richiesta:", error);
            alert("⚠️ Errore: " + error.message);
        })
        .finally(() => {
            spinnerDiv.style.display = "none"; // Nasconde lo spinner sempre
        });
}

document.getElementById("viewPlanningLink").addEventListener("click", (e) => {
    e.preventDefault()
    fetchPlanning()
})

function retriveCoinquys()
{
    const select = document.getElementById('coinquys-select');
    //const swapSelect = document.getElementById('swapWith');

    coinquilini.forEach(coinquilino => {
        const option = document.createElement('option');
        option.value = coinquilino.username;
        option.textContent = coinquilino.username;
        select.appendChild(option);

        //const swapOption = option.cloneNode(true);
        //swapSelect.appendChild(swapOption);
    });
}

async function renderCalendar(data) {

    if (!data || data.length === 0) {
        const emptyMessage = document.getElementById("emptyMessage");
        emptyMessage.style.display = "block";
        return;
    }
    const tbody = document.getElementById("calendarBody");
    tbody.innerHTML = "";
    document.getElementById("calendarPlaceholder").style.display = "none";

    //data = data.filter(assignment => assignment.task && assignment.task.isDone === false);
    data.forEach(cleaningAssignment => {
        const { assignedRoommate, task } = cleaningAssignment;
        const { task: taskCategory, description, timeSlot} = task;
        const { start, end } = timeSlot;

        const row = document.createElement("tr");

        row.innerHTML = `
            <td>${new Date(start).toLocaleDateString()}</td>
            <td>${assignedRoommate ? assignedRoommate.usernameRoommate : "Unassigned"}</td>
            <td>${taskCategory}</td>
            <td>${description}</td>
            <td>${new Date(start).toLocaleTimeString()} - ${new Date(end).toLocaleTimeString()}</td>
            <td>
            <button class="confirm-btn" ${cleaningAssignment.task.done === true ? "disabled" : ""}>
             ${cleaningAssignment.task.done === false ? "❌ Not done" : " ✅ Done"}
            </button>
            </td>
          `;

        tbody.appendChild(row);



        // Aggiungi event listener al bottone di conferma
        const confirmBtn = row.querySelector('.confirm-btn');
        confirmBtn.addEventListener('click', function() {handleDoneButton(cleaningAssignment).then(r => {})})
    })
}

async function handleDoneButton(cleaningAssignment) {
    try {
        const response = await fetch("http://localhost:8080/Shift/rest/calendar/taskDone", {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': "Bearer " + token
            },
            body: JSON.stringify({ id: cleaningAssignment.id})
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText);
        }
        console.log("STO PER ASSEGANRE I PUNTI")
        await assignedPoint(cleaningAssignment.task.task, cleaningAssignment.assignedRoommate.usernameRoommate, cleaningAssignment.task.timeSlot.end)
        window.location.reload();
    } catch (error) {
        console.error("Errore durante il completamento del compito:", error);
        alert("⚠️ Errore: " + error.message);
    }
}

async function assignedPoint(task, username, endTime)
{
    console.log(task);
    console.log(endTime);
    const response = await fetch("http://localhost:8080/Shift/rest/client/toRank", {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': "Bearer " + token
        },
        body: JSON.stringify({
            typeTask: task,
            username: username,
            houseId: houseId,
            dateComplete: new Date().toISOString().slice(0, 19),
            endTime: endTime
        })
    });

    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText);
    }

    const data = await response.json();
    console.log("Punti assegnati correttamente:", data);
}


//funzione che gestisce l'accettazione o il rifiuto di una richiesta di scambio
/*function handleRequestAction(idSwap, accept) {
    const action = accept ? "accept" : "decline";

    fetch(`http://localhost:8080/Shift/rest/swaps/${idSwap}/${action}`, {
        method: "PUT",
        headers: {
            'Content-Type': 'application/json',
            'Authorization': localStorage.getItem("token") || ""
        }
    })
        .then(response => {
            if (!response.ok) {
                return response.text().then(text => { throw new Error(text); });
            }
            alert(`✅ Richiesta ${accept ? "accettata" : "rifiutata"} con successo!`);
        })
        .catch(error => {
            console.error("Errore:", error);
            alert("⚠️ Errore: " + error.message);
        });

    if(action==="accept")
    {
        window.location.reload();

        /*fetch(`https://localhost:8085/Shift/rest/shif/calendar/getPlanning?houseId=${localStorage.getItem("houseId")}`, {
            method: "GET",
            headers: {
                'Content-Type': 'application/json',
                'Authorization': localStorage.getItem("token") || ""
            }
        })
            .then(response => {
                if (!response.ok) {
                    return response.text().then(text => { throw new Error(text); });
                }
                return response.json();
            })
            .then(data => {
                renderCalendar(data)
            })
            .catch(error => {
                console.error("Errore durante la richiesta:", error);
                alert("⚠️ Errore: " + error.message);
            })
    }
}*/

// Funzione per popolare entrambi i select dei turni (A e B)
/*function populateAssignmentSelects(shifts) {
    const currentTaskSelect = document.getElementById("currentTask");
    const assignmentBSelect = document.getElementById("assignmentB");

    // Svuota le select
    currentTaskSelect.innerHTML = '<option value="" disabled selected>Seleziona il tuo turno</option>';
    assignmentBSelect.innerHTML = '<option value="" disabled selected>Seleziona turno</option>';

    if (!shifts || shifts.length === 0) {
        fetch(`http://localhost:8080/Shift/rest/swaps/getSwapRequests?houseId=${localStorage.getItem("houseId")}`, {
            method: "GET",
            headers: { //TODO: body mancante
                'Content-Type': 'application/json',
                'Authorization': "Bearer " + token
            }
        })
            .then(response => {
                if (!response.ok) {
                    return response.text().then(text => {
                        throw new Error(text);
                    });
                }
                return response.json();
            })
            .then(assignments => {
                renderReceivedSwapRequests(assignments, currentTaskSelect, assignmentBSelect);
            })
            .catch(error => {
                console.error("Errore durante il recupero delle richieste di scambio:", error);
                alert("⚠️ Errore: " + error.message);
            });
    } else {
        renderReceivedSwapRequests(shifts, currentTaskSelect, assignmentBSelect);
    }
}*/

// Funzione per riempire le select con i dati
/*function renderReceivedSwapRequests(assignments, currentTaskSelect, assignmentBSelect) {
    const list = document.getElementById("receivedRequests");
    list.innerHTML = "";

    if (assignments.length === 0) {
        list.innerHTML = "<li>Nessuna richiesta ricevuta</li>";
        return;
    }

    assignments.forEach(assignment => {
        // Aggiungi alle select
        const optionA = document.createElement("option");
        optionA.value = assignment.id;
        optionA.textContent = `${assignment.task.task} - ${new Date(assignment.task.timeSlot.start).toLocaleString()}`;
        currentTaskSelect.appendChild(optionA);

        const optionB = optionA.cloneNode(true);
        assignmentBSelect.appendChild(optionB);

        // Aggiungi alla lista
        const li = document.createElement("li");
        li.innerHTML = `
            <strong>Da:</strong> ${assignment.from}<br>
            <strong>Compito:</strong> ${assignment.task.task}<br>
            <strong>Data:</strong> ${new Date(assignment.task.timeSlot.start).toLocaleString()}<br>
            <strong>Stato:</strong> <span class="${assignment.status.toLowerCase()}">${assignment.status}</span>
            ${assignment.status === "PENDING" ? `
                <div style="margin-top: 5px;">
                    <button class="accept-btn" >✅</button>
                    <button class="decline-btn">❌</button>
                </div>` : ""}
        `;

        // Eventi su bottoni
        if (assignment.status === "PENDING")
        {
            const acceptBtn = li.querySelector(".accept-btn");
            const declineBtn = li.querySelector(".decline-btn");

            acceptBtn.addEventListener("click", () => handleRequestAction(assignment.id, true));
            declineBtn.addEventListener("click", () => handleRequestAction(assignment.id, false));
        }

        list.appendChild(li);
    });
}*/


//Poupop dello scambio
const openModalBtn = document.getElementById('openSwapModal');
const modal = document.getElementById('swapModal');
//const closeBtn = modal.querySelector('.close');

const closeBtn = modal.querySelector('.close');
const closeComingSoonBtn = document.getElementById('closeComingSoon');
closeBtn.onclick = () => modal.style.display = 'none';
closeComingSoonBtn.onclick = () => modal.style.display = 'none';

openModalBtn.addEventListener('click', () => {
    modal.style.display = 'block';
});

closeBtn.addEventListener('click', () => {
    modal.style.display = 'none';
});

window.addEventListener('click', (event) => {
    if (event.target === modal) {
        modal.style.display = 'none';
    }
});

//funzione per creare la richiesta di scambio
/*document.getElementById("sendSwapBtn").addEventListener("click", async function(e) {
    e.preventDefault();
    const currentTask = document.getElementById("currentTask").value;
    const assignmentB = document.getElementById("assignmentB").value;

    if (!currentTask || !assignmentB) {
        alert("⚠️ Seleziona entrambi i turni per lo scambio!");
        return;
    }

    try
    {
        const response = await fetch("http://localhost:8080/Shift/rest/swapRequest", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": "Bearer " + token
            },
            body: JSON.stringify({
                fromAssignmentId: currentTask,
                toAssignmentId: assignmentB,
                houseId: localStorage.getItem("houseId")
            })
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText);
        }

        alert("✅ Richiesta di scambio inviata!");
        document.getElementById("swapForm").reset();
        document.getElementById("swapModal").style.display = "none";
    } catch (error)
    {
        alert("⚠️ Errore nell'invio della richiesta: " + error.message);
    }
});*/

const form = document.getElementById("createTaskForm");
form.addEventListener("submit", async function (e) {
    e.preventDefault();

    const description = form.querySelector("textarea").value;
    const type = form.querySelector("#taskType").value;
    const dateTime = form.querySelector("input[type='datetime-local']").value;

    if (!description || !type || !dateTime) {
        alert("⚠️ Compila tutti i campi!");
        return;
    }

    try {
        const response = await fetch("http://localhost:8080/Shift/rest/tasks/createTask", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": "Bearer " + token
            },
            body: JSON.stringify({
                description: description,
                task: type,
                timeSlot: { start: dateTime, end: null },
                houseId: houseId
            })
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText);
        }

        alert("✅ Compito creato con successo!");
        form.reset();
    } catch (error) {
        alert("⚠️ Errore nella creazione del compito: " + error.message);
    }
});

async function retriveShifts() {
    try {
        const response = await fetch("http://localhost:8080/Shift/rest/calendar/getAllShifts", {
            method: "POST",
            headers: {
                'Content-Type': 'application/json',
                'Authorization': "Bearer " + token
            },
            body: JSON.stringify({
                houseId: houseId
            })
        });
        if (!response.ok) {
            const text = await response.text();
            throw new Error(text);
        }
        return await response.json();
    } catch (error) {
        console.error("Errore durante il recupero dei turni:", error);
        alert("⚠️ Errore: " + error.message);
        return [];
    }
}

function initAvaibility()
{
    fetch("http://localhost:8080/Shift/rest/unAvailability/initializeUnavailability", {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': "Bearer " + token
        },
        body: JSON.stringify({
            coiquyList: coinquilini.map(coinquy => ({ usernameRoommate: coinquy.username, houseId: houseId }))
        })
    })
        .then(response => {
            if (!response.ok) {
                return response.text().then(text => { throw new Error(text); });
            }
            return response.json();
        })
        .catch(error => {
            console.error("Errore durante l'inizializzazione:", error);
            alert("⚠️ Errore: " + error.message);
        });
}

document.addEventListener("DOMContentLoaded", async () => {
    local();
    retriveCoinquys();
    let shiftsDb = await retriveShifts();
    console.log("shiftsDb", shiftsDb);
    console.log("length", shiftsDb.length);
    if (shiftsDb.length > 0) {
        shiftsDb.forEach((item) => {
            shift.push(item);
            console.log(item);
        });
        renderCalendar(shift)
    }
    //populateAssignmentSelects(shift
    initAvaibility();
});
