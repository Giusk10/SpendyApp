#!/bin/sh

echo "🚀 Avvio SpendyApp All-in-One..."

# 1. Avvia Auth Service in background
echo "--> Avvio Auth Service (Porta 8081)..."
java -Xms512m -Xmx1024m -Dserver.port=8081 -jar /app/auth-service.jar > /app/auth.log 2>&1 &

# 2. Avvia Expense Service in background
echo "--> Avvio Expense Service (Porta 8084)..."
java -Xms512m -Xmx1024m -Dserver.port=8084 -jar /app/expense-service.jar > /app/expense.log 2>&1 &

# 3. Aspetta che i servizi backend siano pronti (15 secondi)
# Questo evita che il Gateway fallisca perché non trova nessuno
echo "⏳ Attendo 15 secondi per l'avvio dei backend..."
sleep 15

# 4. Avvia Gateway Service in foreground (Mantiene vivo il container)
# Nota: Il gateway userà la porta 7860 grazie alla variabile d'ambiente
echo "--> Avvio Gateway Service (Porta 7860)..."
java -Xms512m -Xmx1024m -Dserver.port=7860 -jar /app/gateway-service.jar