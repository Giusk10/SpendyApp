#!/bin/sh
set -eu

echo "=================================================="
echo "🚀 Avvio SpendyApp All-in-One (Hugging Face Spaces)"
echo "=================================================="

# 1. Rilevamento dell'ambiente (sviluppo vs produzione)
IS_PRODUCTION=0
case "${ENVIRONMENT:-}${SPRING_PROFILES_ACTIVE:-}${APP_ENV:-}" in
    *prod*|*production*)
        IS_PRODUCTION=1
        ;;
esac

# 2. Validazione e gestione delle chiavi crittografiche RSA
if [ "$IS_PRODUCTION" -eq 1 ]; then
    echo "🔒 Ambiente di Produzione rilevato."
    if [ -z "${JWT_PRIVATE_KEY:-}" ] && [ -z "${JWT_PRIVATE_KEY_LOCATION:-}" ]; then
        echo "❌ ERRORE CRITICO DI SICUREZZA: Chiave privata RSA mancante in produzione!"
        echo "Configurare JWT_PRIVATE_KEY o JWT_PRIVATE_KEY_LOCATION tra le variabili d'ambiente protette."
        exit 1
    fi
    if [ -z "${JWT_PUBLIC_KEY:-}" ] && [ -z "${JWT_PUBLIC_KEY_LOCATION:-}" ]; then
        echo "❌ ERRORE CRITICO DI SICUREZZA: Chiave pubblica RSA mancante in produzione!"
        echo "Configurare JWT_PUBLIC_KEY o JWT_PUBLIC_KEY_LOCATION tra le variabili d'ambiente protette."
        exit 1
    fi
    if [ "${JWT_ALLOW_EPHEMERAL_KEYS:-false}" = "true" ]; then
        echo "❌ ERRORE CRITICO DI SICUREZZA: JWT_ALLOW_EPHEMERAL_KEYS non può essere 'true' in produzione!"
        exit 1
    fi
else
    echo "🧪 Ambiente di Sviluppo / Demo rilevato."
    # Generazione automatica chiavi RSA condivise se non esplicitamente fornite
    if [ -z "${JWT_PRIVATE_KEY:-}" ] && [ -z "${JWT_PRIVATE_KEY_LOCATION:-}" ]; then
        mkdir -p /app/keys
        if [ ! -f /app/keys/private_key.pem ]; then
            echo "🔑 Generazione automatica chiavi RSA (2048-bit) per sviluppo/test..."
            openssl genpkey -algorithm RSA -out /app/keys/private_key.pem -pkeyopt rsa_keygen_bits:2048 2>/dev/null || true
            openssl rsa -pubout -in /app/keys/private_key.pem -out /app/keys/public_key.pem 2>/dev/null || true
        fi
        if [ -f /app/keys/private_key.pem ]; then
            export JWT_PRIVATE_KEY_LOCATION="file:/app/keys/private_key.pem"
            export JWT_PUBLIC_KEY_LOCATION="file:/app/keys/public_key.pem"
            echo "✅ Chiavi locali configurate: /app/keys/"
        fi
    fi
fi

# File di log dedicati ai microservizi interni
AUTH_LOG="/app/auth.log"
EXPENSE_LOG="/app/expense.log"
touch "$AUTH_LOG" "$EXPENSE_LOG"

# Stream dei log in background con prefisso per diagnosi unificata
tail -n 0 -F "$AUTH_LOG" 2>/dev/null | sed 's/^/[Auth] /' &
TAIL_AUTH_PID=$!
tail -n 0 -F "$EXPENSE_LOG" 2>/dev/null | sed 's/^/[Expense] /' &
TAIL_EXPENSE_PID=$!

AUTH_PID=""
EXPENSE_PID=""
GATEWAY_PID=""

# Gestore dei segnali per terminazione pulita (Graceful Shutdown)
cleanup() {
    echo ""
    echo "🛑 Ricevuto segnale di terminazione. Arresto controllato dei microservizi..."
    [ -n "$GATEWAY_PID" ] && kill -TERM "$GATEWAY_PID" 2>/dev/null || true
    [ -n "$EXPENSE_PID" ] && kill -TERM "$EXPENSE_PID" 2>/dev/null || true
    [ -n "$AUTH_PID" ] && kill -TERM "$AUTH_PID" 2>/dev/null || true
    [ -n "$TAIL_AUTH_PID" ] && kill -TERM "$TAIL_AUTH_PID" 2>/dev/null || true
    [ -n "$TAIL_EXPENSE_PID" ] && kill -TERM "$TAIL_EXPENSE_PID" 2>/dev/null || true
    
    # Attende la terminazione dei processi JVM
    [ -n "$GATEWAY_PID" ] && wait "$GATEWAY_PID" 2>/dev/null || true
    [ -n "$EXPENSE_PID" ] && wait "$EXPENSE_PID" 2>/dev/null || true
    [ -n "$AUTH_PID" ] && wait "$AUTH_PID" 2>/dev/null || true
    echo "👋 Arresto completato."
    exit 0
}

trap cleanup TERM INT QUIT

# 3. Avvio Auth Microservice in background
echo "--> Avvio Auth MicroService (Porta 8081)..."
java -Xms128m -Xmx384m -XX:+UseG1GC \
     -Dserver.port=8081 \
     -jar /app/auth-service.jar > "$AUTH_LOG" 2>&1 &
AUTH_PID=$!

# 4. Avvio Expense Microservice in background
echo "--> Avvio Expense MicroService (Porta 8084)..."
java -Xms256m -Xmx512m -XX:+UseG1GC \
     -Dserver.port=8084 \
     -jar /app/expense-service.jar > "$EXPENSE_LOG" 2>&1 &
EXPENSE_PID=$!

# 5. Attesa readiness di Auth Service
echo "⏳ Attesa disponibilità Auth Service (http://localhost:8081/actuator/health)..."
AUTH_READY=0
for i in $(seq 1 60); do
    if ! kill -0 "$AUTH_PID" 2>/dev/null; then
        echo "❌ Auth Service è terminato prematuramente (PID $AUTH_PID)!"
        tail -n 100 "$AUTH_LOG"
        exit 1
    fi
    if wget -q -O - http://localhost:8081/actuator/health 2>/dev/null | grep -q '"status":"UP"'; then
        echo "✅ Auth Service è pronto e operativo (UP)!"
        AUTH_READY=1
        break
    fi
    sleep 1
done

if [ "$AUTH_READY" -ne 1 ]; then
    echo "❌ Timeout attesa Auth Service (60s). Impossibile proseguire!"
    tail -n 100 "$AUTH_LOG"
    cleanup
    exit 1
fi

# 6. Attesa readiness di Expense Service
echo "⏳ Attesa disponibilità Expense Service (http://localhost:8084/actuator/health)..."
EXPENSE_READY=0
for i in $(seq 1 60); do
    if ! kill -0 "$EXPENSE_PID" 2>/dev/null; then
        echo "❌ Expense Service è terminato prematuramente (PID $EXPENSE_PID)!"
        tail -n 100 "$EXPENSE_LOG"
        exit 1
    fi
    if wget -q -O - http://localhost:8084/actuator/health 2>/dev/null | grep -q '"status":"UP"'; then
        echo "✅ Expense Service è pronto e operativo (UP)!"
        EXPENSE_READY=1
        break
    fi
    sleep 1
done

if [ "$EXPENSE_READY" -ne 1 ]; then
    echo "❌ Timeout attesa Expense Service (60s). Impossibile proseguire!"
    tail -n 100 "$EXPENSE_LOG"
    cleanup
    exit 1
fi

# 7. Avvio Gateway Service
echo "--> Avvio API Gateway (Porta 7860)..."
java -Xms128m -Xmx384m -XX:+UseG1GC \
     -Dserver.port=7860 \
     -Dreactor.netty.http.server.accessLogEnabled=true \
     -jar /app/gateway-service.jar &
GATEWAY_PID=$!

# 8. Attesa disponibilità Gateway
echo "⏳ Attesa disponibilità API Gateway (http://localhost:7860/actuator/health)..."
for i in $(seq 1 30); do
    if ! kill -0 "$GATEWAY_PID" 2>/dev/null; then
        echo "❌ API Gateway è terminato prematuramente (PID $GATEWAY_PID)!"
        cleanup
        exit 1
    fi
    if wget -q -O - http://localhost:7860/actuator/health 2>/dev/null | grep -q '"status":"UP"'; then
        echo "✅ API Gateway è pronto e operativo (UP)!"
        break
    fi
    sleep 1
done

echo "=================================================="
echo "🎉 SpendyApp avviata con successo!"
echo "   Endpoint principale: http://0.0.0.0:7860"
echo "   Documentazione Swagger: http://0.0.0.0:7860/swagger-ui.html"
echo "=================================================="

# 9. Ciclo di supervisione dei processi JVM
# Se uno qualsiasi dei processi fallisce, l'intero container termina con codice d'errore
while true; do
    if ! kill -0 "$AUTH_PID" 2>/dev/null; then
        echo "❌ CRITICAL: Auth Service (PID $AUTH_PID) si è arrestato inaspettatamente!"
        tail -n 100 "$AUTH_LOG"
        cleanup
        exit 1
    fi
    if ! kill -0 "$EXPENSE_PID" 2>/dev/null; then
        echo "❌ CRITICAL: Expense Service (PID $EXPENSE_PID) si è arrestato inaspettatamente!"
        tail -n 100 "$EXPENSE_LOG"
        cleanup
        exit 1
    fi
    if ! kill -0 "$GATEWAY_PID" 2>/dev/null; then
        echo "❌ CRITICAL: Gateway Service (PID $GATEWAY_PID) si è arrestato inaspettatamente!"
        cleanup
        exit 1
    fi
    sleep 2
done