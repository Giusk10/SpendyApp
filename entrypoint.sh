#!/bin/sh

echo "🚀 Avvio SpendyApp All-in-One..."

# Assicura chiavi RSA condivise se non esplicitamente configurate via env
if [ -z "$JWT_PRIVATE_KEY" ] && [ -z "$JWT_PRIVATE_KEY_LOCATION" ]; then
    mkdir -p /app/keys
    if [ ! -f /app/keys/private_key.pem ]; then
        echo "🔑 Generazione automatica chiavi RSA condivise per SpendyApp..."
        openssl genpkey -algorithm RSA -out /app/keys/private_key.pem -pkeyopt rsa_keygen_bits:2048 2>/dev/null || true
        openssl rsa -pubout -in /app/keys/private_key.pem -out /app/keys/public_key.pem 2>/dev/null || true
    fi
    if [ -f /app/keys/private_key.pem ]; then
        export JWT_PRIVATE_KEY_LOCATION="file:/app/keys/private_key.pem"
        export JWT_PUBLIC_KEY_LOCATION="file:/app/keys/public_key.pem"
    fi
fi

# File di log
AUTH_LOG="/app/auth.log"
EXPENSE_LOG="/app/expense.log"
touch "$AUTH_LOG" "$EXPENSE_LOG"

# Stream logs in background so HF Spaces shows all microservice logs
tail -n 0 -F "$AUTH_LOG" 2>/dev/null | sed 's/^/[Auth] /' &
TAIL_AUTH_PID=$!
tail -n 0 -F "$EXPENSE_LOG" 2>/dev/null | sed 's/^/[Expense] /' &
TAIL_EXPENSE_PID=$!

# 1. Avvia Auth Service in background
echo "--> Avvio Auth Service (Porta 8081)..."
java -Xms256m -Xmx512m -XX:+UseG1GC -Dserver.port=8081 -jar /app/auth-service.jar > "$AUTH_LOG" 2>&1 &
AUTH_PID=$!

# 2. Avvia Expense Service in background
echo "--> Avvio Expense Service (Porta 8084)..."
java -Xms512m -Xmx2G -XX:+UseG1GC -Dserver.port=8084 -jar /app/expense-service.jar > "$EXPENSE_LOG" 2>&1 &
EXPENSE_PID=$!

# 3. Attesa readiness Auth Service
echo "⏳ Attendo che Auth Service sia pronto (http://localhost:8081/actuator/health)..."
AUTH_READY=0
for i in $(seq 1 60); do
  if ! kill -0 $AUTH_PID 2>/dev/null; then
    echo "❌ Auth Service è terminato inaspettatamente (PID $AUTH_PID)!"
    cat "$AUTH_LOG"
    exit 1
  fi
  if wget -q -O - http://localhost:8081/actuator/health 2>/dev/null | grep -q '"status":"UP"'; then
    echo "✅ Auth Service è pronto!"
    AUTH_READY=1
    break
  fi
  sleep 1
done

if [ "$AUTH_READY" -ne 1 ]; then
  echo "⚠️ Timeout attesa Auth Service (60s). Ultimi log:"
  tail -n 50 "$AUTH_LOG"
fi

# 4. Attesa readiness Expense Service
echo "⏳ Attendo che Expense Service sia pronto (http://localhost:8084/actuator/health)..."
EXPENSE_READY=0
for i in $(seq 1 60); do
  if ! kill -0 $EXPENSE_PID 2>/dev/null; then
    echo "❌ Expense Service è terminato inaspettatamente (PID $EXPENSE_PID)!"
    cat "$EXPENSE_LOG"
    exit 1
  fi
  if wget -q -O - http://localhost:8084/actuator/health 2>/dev/null | grep -q '"status":"UP"'; then
    echo "✅ Expense Service è pronto!"
    EXPENSE_READY=1
    break
  fi
  sleep 1
done

if [ "$EXPENSE_READY" -ne 1 ]; then
  echo "⚠️ Timeout attesa Expense Service (60s). Ultimi log:"
  tail -n 50 "$EXPENSE_LOG"
fi

# 5. Avvia Gateway Service in foreground
echo "--> Avvio Gateway Service (Porta 7860)..."
exec java -Xms256m -Xmx512m -XX:+UseG1GC \
     -Dserver.port=7860 \
     -Dreactor.netty.http.server.accessLogEnabled=true \
     -jar /app/gateway-service.jar