#!/bin/sh

echo "🚀 Avvio SpendyApp All-in-One..."

# 1. Avvia Auth Service in background
echo "--> Avvio Auth Service (Porta 8081)..."
java -Xms1G -Xmx1G -XX:+UseG1GC -Dserver.port=8081 -jar /app/auth-service.jar > /app/auth.log 2>&1 &

# 2. Avvia Expense Service in background
echo "--> Avvio Expense Service (Porta 8084)..."
java -Xms6G -Xmx6G -XX:+AlwaysPreTouch -XX:+UseG1GC -Dserver.port=8084 -jar /app/expense-service.jar > /app/expense.log 2>&1 &

# 3. Attesa readiness dei servizi backend tramite health check (senza sleep arbitrari)
echo "⏳ Attendo che Auth Service sia pronto (http://localhost:8081/actuator/health)..."
for i in $(seq 1 60); do
  if wget -q -O - http://localhost:8081/actuator/health 2>/dev/null | grep -q '"status":"UP"'; then
    echo "✅ Auth Service è pronto!"
    break
  fi
  sleep 1
done

echo "⏳ Attendo che Expense Service sia pronto (http://localhost:8084/actuator/health)..."
for i in $(seq 1 60); do
  if wget -q -O - http://localhost:8084/actuator/health 2>/dev/null | grep -q '"status":"UP"'; then
    echo "✅ Expense Service è pronto!"
    break
  fi
  sleep 1
done

# 4. Avvia Gateway Service in foreground con Access Logs forzati
echo "--> Avvio Gateway Service (Porta 7860)..."
java -Xms4G -Xmx4G -XX:+AlwaysPreTouch -XX:+UseG1GC \
     -Dserver.port=7860 \
     -Dreactor.netty.http.server.accessLogEnabled=true \
     -jar /app/gateway-service.jar