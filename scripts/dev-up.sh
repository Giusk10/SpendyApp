#!/usr/bin/env bash
set -Eeuo pipefail

# SpendyApp - Avvio Ambiente di Sviluppo Locale
# Directory radice del progetto
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

echo "=================================================="
echo "🚀 SpendyApp - Avvio Ambiente Docker Compose"
echo "=================================================="

# 1. Verifica dipendenze
if ! command -v docker >/dev/null 2>&1; then
    echo "❌ Errore: Docker non risulta installato o non è presente nel PATH."
    exit 1
fi

# Rilevamento comando compose ('docker compose' vs 'docker-compose')
if docker compose version >/dev/null 2>&1; then
    COMPOSE_CMD=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
    COMPOSE_CMD=(docker-compose)
else
    echo "❌ Errore: Docker Compose (plugin o standalone) non è installato."
    exit 1
fi

# 2. Controllo e preparazione file .env
if [ ! -f "$REPO_ROOT/.env" ]; then
    if [ -f "$REPO_ROOT/.env.example" ]; then
        echo "ℹ️  File .env non trovato. Creazione automatica da .env.example per sviluppo locale..."
        cp "$REPO_ROOT/.env.example" "$REPO_ROOT/.env"
    else
        echo "❌ Errore: Né .env né .env.example sono presenti."
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
        echo "✅ Chiavi RSA generate in keys/"
    fi
fi

# 4. Avvio dei container
echo "📦 Build e avvio dei servizi in corso..."
"${COMPOSE_CMD[@]}" up -d --build "$@"

# 5. Attesa disponibilità dei servizi (Healthcheck)
echo ""
echo "⏳ Attesa che tutti i servizi completino i controlli di salute (healthcheck)..."
GATEWAY_PORT="${GATEWAY_PORT:-7860}"

MAX_WAIT=90
ELAPSED=0
READY=0

while [ $ELAPSED -lt $MAX_WAIT ]; do
    GW_STATUS=$("${COMPOSE_CMD[@]}" ps gateway-service --format "{{.Health}}" 2>/dev/null || true)
    if [[ "$GW_STATUS" =~ healthy ]]; then
        READY=1
        break
    fi
    sleep 3
    ELAPSED=$((ELAPSED + 3))
    echo "   ... in attesa ($ELAPSED/$MAX_WAIT s - stato gateway: ${GW_STATUS:-avvio})"
done

echo ""
if [ $READY -eq 1 ]; then
    echo "=================================================="
    echo "🎉 SpendyApp è operativa!"
    echo "   Gateway URL:    http://localhost:${GATEWAY_PORT}"
    echo "   Swagger UI:     http://localhost:${GATEWAY_PORT}/swagger-ui.html"
    echo "   Auth Health:    http://localhost:${GATEWAY_PORT}/Auth/rest/auth/health"
    echo "   Gateway Health: http://localhost:${GATEWAY_PORT}/actuator/health"
    echo ""
    echo "Per visualizzare i log:  ./scripts/dev-logs.sh"
    echo "Per eseguire smoke test: ./scripts/smoke-test.sh"
    echo "Per arrestare l'ambiente: ./scripts/dev-down.sh"
    echo "=================================================="
else
    echo "⚠️  I servizi sono stati avviati, ma il Gateway non ha ancora raggiunto lo stato 'healthy'."
    echo "Controlla lo stato dei container con: ${COMPOSE_CMD[*]} ps"
    echo "Consulta i log completi con:          ./scripts/dev-logs.sh"
fi
