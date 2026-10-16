#!/usr/bin/env bash
set -Eeuo pipefail

# SpendyApp - Smoke Test End-to-End
# Esegue verifiche non distruttive sugli endpoint chiave per validare la salute del sistema

GATEWAY_URL="${1:-${GATEWAY_URL:-http://localhost:7860}}"
# Rimuove eventuale slash finale
GATEWAY_URL="${GATEWAY_URL%/}"

echo "=================================================="
echo "🧪 SpendyApp - Esecuzione Smoke Test"
echo "   Target Gateway: $GATEWAY_URL"
echo "=================================================="

# Verifica dipendenze
if ! command -v curl >/dev/null 2>&1; then
    echo "❌ Errore: curl non risulta installato o non è presente nel PATH."
    exit 1
fi

# 1. Attesa disponibilità iniziale del Gateway
MAX_WAIT=60
ELAPSED=0
ONLINE=0

echo "⏳ Attesa che il Gateway risponda a ${GATEWAY_URL}/actuator/health (max ${MAX_WAIT}s)..."
while [ $ELAPSED -lt $MAX_WAIT ]; do
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${GATEWAY_URL}/actuator/health" 2>/dev/null || echo "000")
    if [ "$HTTP_CODE" = "200" ]; then
        ONLINE=1
        break
    fi
    sleep 2
    ELAPSED=$((ELAPSED + 2))
    printf "."
done
echo ""

if [ $ONLINE -ne 1 ]; then
    echo "❌ Timeout: il Gateway non risponde (HTTP $HTTP_CODE) dopo ${MAX_WAIT} secondi."
    exit 1
fi
echo "✅ Gateway raggiungibile (risposta in ${ELAPSED}s)."

# 2. Esecuzione dei singoli test di verifica
FAILED_TESTS=0

run_check() {
    local NAME="$1"
    local ENDPOINT="$2"
    local EXPECTED_CODE="$3"
    local METHOD="${4:-GET}"

    local URL="${GATEWAY_URL}${ENDPOINT}"
    local CODE
    CODE=$(curl -s -X "$METHOD" -o /dev/null -w "%{http_code}" "$URL" 2>/dev/null || echo "000")

    if [ "$CODE" = "$EXPECTED_CODE" ]; then
        echo "  [PASS] $NAME -> HTTP $CODE (atteso $EXPECTED_CODE) [$ENDPOINT]"
    else
        echo "  [FAIL] $NAME -> HTTP $CODE (atteso $EXPECTED_CODE) [$ENDPOINT]"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
}

echo ""
echo "🔍 Esecuzione verifiche di integrità e routing:"

# Test 1: Gateway Direct Health
run_check "Gateway Actuator Health" "/actuator/health" "200"

# Test 2: Auth Service Health routed through Gateway
run_check "Auth Service Health" "/Auth/rest/auth/health" "200"

# Test 3: Swagger UI Aggregated Entrypoint
run_check "Swagger UI Page" "/swagger-ui.html" "302" # SpringDoc redirects swagger-ui.html to /webjars/swagger-ui/index.html

# Test 4: Gateway OpenAPI Spec
run_check "Gateway OpenAPI Docs" "/v3/api-docs" "200"

# Test 5: Auth Service OpenAPI Spec routed through Gateway
run_check "Auth Service OpenAPI Docs" "/Auth/v3/api-docs" "200"

# Test 6: Public JWKS Keyset Endpoint
run_check "Public JWKS Endpoint" "/Auth/rest/auth/jwks.json" "200"

# Test 7: Protected Route Security Enforcement (deve ritornare 401 Unauthorized se privo di token)
run_check "Expense Security Enforcement (No Token)" "/Expense/rest/expense/all" "401"

echo ""
echo "=================================================="
if [ $FAILED_TESTS -eq 0 ]; then
    echo "🎉 TUTTI I TEST SONO STATI SUPERATI CON SUCCESSO!"
    echo "   L'applicazione è sana, il routing è operativo e le policy di sicurezza sono attive."
    echo "=================================================="
    exit 0
else
    echo "❌ $FAILED_TESTS TEST FALLITI!"
    echo "   Verificare i log dei microservizi con: ./scripts/dev-logs.sh"
    echo "=================================================="
    exit 1
fi
