#!/usr/bin/env bash
set -Eeuo pipefail

# SpendyApp - Avvio Ambiente di Sviluppo Locale
# Directory radice del progetto
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

echo "=================================================="
echo "🚀 SpendyApp - Avvio Ambiente Docker Compose"
echo "=================================================="

# 1. Verifica installazione Docker e disponibilità del demone
if ! command -v docker >/dev/null 2>&1; then
    echo "❌ Errore: Docker non risulta installato o non è presente nel PATH." >&2
    exit 1
fi

if ! docker info >/dev/null 2>&1; then
    echo "❌ Errore: Il demone Docker non è in esecuzione o non risponde." >&2
    echo "   Assicurati che Docker Desktop o il servizio dockerd sia avviato." >&2
    exit 1
fi

# Rilevamento comando compose ('docker compose' vs 'docker-compose')
if docker compose version >/dev/null 2>&1; then
    COMPOSE_CMD=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
    COMPOSE_CMD=(docker-compose)
else
    echo "❌ Errore: Docker Compose (plugin o standalone) non è installato." >&2
    exit 1
fi

# 2. Controllo e inizializzazione file di configurazione .env
if [ ! -f "$REPO_ROOT/.env" ]; then
    if [ -f "$REPO_ROOT/.env.example" ]; then
        echo "ℹ️  File .env non trovato: inizializzazione automatica per sviluppo locale da .env.example..."
        cp "$REPO_ROOT/.env.example" "$REPO_ROOT/.env"
        echo "✅ File .env locale creato da .env.example."
        echo "ℹ️  I valori in .env.example sono credenziali predefinite valide ESCLUSIVAMENTE per lo sviluppo locale."
    else
        echo "❌ Errore: File di configurazione .env e template .env.example non trovati." >&2
        exit 1
    fi
fi

# 3. Preparazione chiavi RSA locali se assenti
if [ ! -f "$REPO_ROOT/keys/private_key.pem" ]; then
    if command -v openssl >/dev/null 2>&1; then
        echo "🔑 Generazione automatica chiavi RSA (2048-bit) per sviluppo locale in keys/..."
        mkdir -p "$REPO_ROOT/keys"
        openssl genpkey -algorithm RSA -out "$REPO_ROOT/keys/private_key.pem" -pkeyopt rsa_keygen_bits:2048 2>/dev/null
        openssl rsa -pubout -in "$REPO_ROOT/keys/private_key.pem" -out "$REPO_ROOT/keys/public_key.pem" 2>/dev/null
        echo "✅ Chiavi RSA locali generate in keys/"
    fi
fi

# 4. Avvio dei container
echo "📦 Build e avvio dei servizi in corso..."
"${COMPOSE_CMD[@]}" up -d --build "$@"

# 5. Attesa disponibilità di tutti i servizi essenziali (Healthcheck)
echo ""
echo "⏳ Attesa che tutti i servizi essenziali completino i controlli di salute (healthcheck)..."
GATEWAY_PORT="${GATEWAY_PORT:-7860}"
MAX_WAIT="${DEV_UP_TIMEOUT:-90}"
ELAPSED=0
INTERVAL=3
ESSENTIAL_SERVICES=("mysql" "auth-service" "expense-service" "gateway-service")
ALL_HEALTHY=0

while [ $ELAPSED -lt $MAX_WAIT ]; do
    UNHEALTHY_COUNT=0
    CRASHED_SERVICES=()

    for svc in "${ESSENTIAL_SERVICES[@]}"; do
        # Ispezione dello stato e della salute del container
        STATUS_LINE=$("${COMPOSE_CMD[@]}" ps "$svc" --format "{{.State}}|{{.Health}}" 2>/dev/null || echo "not_found|")
        STATE="${STATUS_LINE%%|*}"
        HEALTH="${STATUS_LINE##*|}"

        if [ "$STATE" = "exited" ] || [ "$STATE" = "dead" ]; then
            CRASHED_SERVICES+=("$svc ($STATE)")
            UNHEALTHY_COUNT=$((UNHEALTHY_COUNT + 1))
        elif [ "$HEALTH" != "healthy" ]; then
            UNHEALTHY_COUNT=$((UNHEALTHY_COUNT + 1))
        fi
    done

    # Segnalazione rapida in caso di crash immediato di un servizio essenziale
    if [ ${#CRASHED_SERVICES[@]} -gt 0 ]; then
        echo "   ⚠️  Rilevato arresto anomalo: ${CRASHED_SERVICES[*]}"
    fi

    if [ $UNHEALTHY_COUNT -eq 0 ]; then
        ALL_HEALTHY=1
        break
    fi

    sleep $INTERVAL
    ELAPSED=$((ELAPSED + INTERVAL))
    echo "   ... in attesa ($ELAPSED/$MAX_WAIT s - servizi non ancora pronti: $UNHEALTHY_COUNT/${#ESSENTIAL_SERVICES[@]})"
done

echo ""
if [ $ALL_HEALTHY -eq 1 ]; then
    echo "=================================================="
    echo "🎉 SpendyApp è pienamente operativa e sana!"
    echo "   Gateway URL:    http://localhost:${GATEWAY_PORT}"
    echo "   Swagger UI:     http://localhost:${GATEWAY_PORT}/swagger-ui.html"
    echo "   Auth Health:    http://localhost:${GATEWAY_PORT}/Auth/rest/auth/health"
    echo "   Gateway Health: http://localhost:${GATEWAY_PORT}/actuator/health"
    echo ""
    echo "Per visualizzare i log:  ./scripts/dev-logs.sh"
    echo "Per eseguire smoke test: ./scripts/smoke-test.sh"
    echo "Per arrestare l'ambiente: ./scripts/dev-down.sh"
    echo "=================================================="
    exit 0
else
    echo "==================================================" >&2
    echo "❌ ERRORE: Uno o più servizi essenziali non hanno raggiunto lo stato 'healthy' entro ${MAX_WAIT}s!" >&2
    echo "==================================================" >&2
    echo "Dettaglio stato dei singoli servizi:" >&2
    for svc in "${ESSENTIAL_SERVICES[@]}"; do
        STATUS_LINE=$("${COMPOSE_CMD[@]}" ps "$svc" --format "{{.State}}|{{.Health}}" 2>/dev/null || echo "non trovato|")
        STATE="${STATUS_LINE%%|*}"
        HEALTH="${STATUS_LINE##*|}"
        if [ "$HEALTH" = "healthy" ]; then
            echo "   ✅ $svc: Stato '$STATE', Salute 'healthy'" >&2
        else
            echo "   ❌ $svc: Stato '${STATE:-sconosciuto}', Salute '${HEALTH:-non disponibile}'" >&2
        fi
    done
    echo "" >&2
    echo "Istruzioni per la diagnosi:" >&2
    echo "  1. Per verificare lo stato generale:      ${COMPOSE_CMD[*]} ps" >&2
    echo "  2. Per visualizzare i log del servizio:   ${COMPOSE_CMD[*]} logs <nome-servizio>" >&2
    echo "  3. Per arrestare senza cancellare volumi: ./scripts/dev-down.sh" >&2
    echo "==================================================" >&2
    exit 1
fi
