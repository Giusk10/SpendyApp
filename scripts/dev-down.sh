#!/usr/bin/env bash
set -Eeuo pipefail

# SpendyApp - Arresto Ambiente di Sviluppo Locale
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

echo "=================================================="
echo "🛑 SpendyApp - Arresto Ambiente Locale"
echo "=================================================="

if ! command -v docker >/dev/null 2>&1; then
    echo "❌ Errore: Docker non risulta installato o non è presente nel PATH."
    exit 1
fi

if docker compose version >/dev/null 2>&1; then
    COMPOSE_CMD=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
    COMPOSE_CMD=(docker-compose)
else
    echo "❌ Errore: Docker Compose non trovato."
    exit 1
fi

# Arresto dei container mantenendo intatti i volumi dati persistenti (NO -v)
echo "Arresto e rimozione dei container della rete spendy-network..."
"${COMPOSE_CMD[@]}" down "$@"

echo ""
echo "✅ Tutti i container sono stati arrestati."
echo "ℹ️  I dati del database sono stati preservati nel volume 'spendy-mysql-data'."
echo "   (Per riavviare l'ambiente: ./scripts/dev-up.sh)"
