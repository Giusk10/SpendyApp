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
    echo "❌ Errore: curl non risulta installato o non è presente nel PATH." >&2
    exit 1
fi

# 1. Attesa disponibilità iniziale del Gateway
MAX_WAIT=60
ELAPSED=0
ONLINE=0

echo "⏳ Attesa che il Gateway risponda a ${GATEWAY_URL}/actuator/health (max ${MAX_WAIT}s)..."
while [ $ELAPSED -lt $MAX_WAIT ]; do
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 --max-time 10 "${GATEWAY_URL}/actuator/health" 2>/dev/null || echo "000")
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
    echo "❌ Timeout: il Gateway non risponde (HTTP $HTTP_CODE) dopo ${MAX_WAIT} secondi." >&2
    exit 1
fi
echo "✅ Gateway raggiungibile (risposta in ${ELAPSED}s)."

# 2. Esecuzione dei singoli test di verifica
FAILED_TESTS=0
TOTAL_CHECKS=0

run_check() {
    local NAME="$1"
    local ENDPOINT="$2"
    local EXPECTED_CODES="$3"
    local METHOD="${4:-GET}"
    local DATA="${5:-}"
    local CONTENT_TYPE="${6:-}"

    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    local URL="${GATEWAY_URL}${ENDPOINT}"
    local CODE
    local CURL_ARGS=(-s -o /dev/null -w "%{http_code}" --connect-timeout 5 --max-time 10 -X "$METHOD")

    if [ -n "$CONTENT_TYPE" ]; then
        CURL_ARGS+=(-H "Content-Type: $CONTENT_TYPE")
    fi
    if [ -n "$DATA" ]; then
        CURL_ARGS+=(-d "$DATA")
    fi

    CODE=$(curl "${CURL_ARGS[@]}" "$URL" 2>/dev/null || echo "000")

    local MATCHED=0
    for exp in $EXPECTED_CODES; do
        if [ "$CODE" = "$exp" ]; then
            MATCHED=1
            break
        fi
    done

    if [ $MATCHED -eq 1 ]; then
        echo "  [PASS] $NAME -> HTTP $CODE (atteso $EXPECTED_CODES) [$ENDPOINT]"
    else
        echo "  [FAIL] $NAME -> HTTP $CODE (atteso $EXPECTED_CODES) [$ENDPOINT]" >&2
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
}

echo ""
echo "🔍 Esecuzione verifiche di integrità, routing e sicurezza:"

# Test 1: Gateway Direct Health
run_check "Gateway Actuator Health" "/actuator/health" "200"

# Test 2: Auth Service Health routed through Gateway
run_check "Auth Service Health" "/Auth/rest/auth/health" "200"

# Test 3: Swagger UI Entrypoint
run_check "Swagger UI Page" "/swagger-ui.html" "200 302"

# Test 4: Swagger UI Static Resources
run_check "Swagger UI Webjars" "/webjars/swagger-ui/index.html" "200"

# Test 5: Gateway OpenAPI Spec
run_check "Gateway OpenAPI Docs" "/v3/api-docs" "200"

# Test 6: Auth Service OpenAPI Spec routed through Gateway
run_check "Auth Service OpenAPI Docs" "/Auth/v3/api-docs" "200"

# Test 7: Expense Service OpenAPI Spec routed through Gateway
run_check "Expense Service OpenAPI Docs" "/Expense/v3/api-docs" "200"

# Test 8: Public JWKS Keyset Endpoint
run_check "Public JWKS Endpoint" "/Auth/rest/auth/jwks.json" "200"

# Test 9: Protected Route Security Enforcement (Expense, no token -> 401)
run_check "Expense Security Enforcement (No Token)" "/Expense/rest/expense/all" "401"

# Test 10: Protected Route Security Enforcement (Auth profile, no token -> 401)
run_check "Auth Profile Security Enforcement (No Token)" "/Auth/rest/auth/profile" "401"

# Test 11: Non-destructive Synthetic Auth Verification (Invalid credentials -> 401)
run_check "Synthetic Auth Bad Credentials Rejection" "/Auth/rest/auth/login" "401" "POST" '{"username":"synthetic_smoke_user","password":"WrongPassword123!"}' "application/json"

echo ""
echo "=================================================="
if [ $FAILED_TESTS -eq 0 ]; then
    echo "🎉 TUTTI I TEST ($TOTAL_CHECKS/$TOTAL_CHECKS) SONO STATI SUPERATI CON SUCCESSO!"
    echo "   L'applicazione è sana, il routing è operativo e le policy di sicurezza sono attive."
    echo "=================================================="
    exit 0
else
    echo "❌ $FAILED_TESTS/$TOTAL_CHECKS TEST FALLITI!" >&2
    echo "   Verificare i log dei microservizi con: ./scripts/dev-logs.sh" >&2
    echo "==================================================" >&2
    exit 1
fi
