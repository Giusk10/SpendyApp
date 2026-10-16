#!/usr/bin/env bash
set -Eeuo pipefail

# SpendyApp - Visualizzazione Log Servizi
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

if ! command -v docker >/dev/null 2>&1; then
    echo "❌ Errore: Docker non risulta installato o non è presente nel PATH." >&2
    exit 1
fi

if ! docker info >/dev/null 2>&1; then
    echo "❌ Errore: Il demone Docker non è in esecuzione o non risponde." >&2
    echo "   Assicurati che Docker Desktop o il servizio dockerd sia avviato." >&2
    exit 1
fi

if docker compose version >/dev/null 2>&1; then
    COMPOSE_CMD=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
    COMPOSE_CMD=(docker-compose)
else
    echo "❌ Errore: Docker Compose non trovato." >&2
    exit 1
fi

echo "=================================================="
echo "📋 SpendyApp - Stream dei Log (Ctrl+C per uscire)"
if [ "$#" -gt 0 ]; then
    echo "   Filtro servizi: $*"
fi
echo "=================================================="

"${COMPOSE_CMD[@]}" logs -f --tail=100 "$@"
