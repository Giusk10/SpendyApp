# --- FASE 1: Build (Compilazione) ---
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copia tutto il codice sorgente
COPY . .

# Nota: Grazie alla modifica nel pom.xml padre, questi comandi generano
# automaticamente anche i file THIRD-PARTY.txt dentro le cartelle target.

# Compila Auth Service
WORKDIR /app/AuthMicroService
RUN mvn clean package -DskipTests

# Compila Expense Service
WORKDIR /app/ExpenseMicroService
RUN mvn clean package -DskipTests

# Compila Gateway Service
WORKDIR /app/Gateway
RUN mvn clean package -DskipTests

# --- FASE 2: Esecuzione (Immagine finale leggera) ---
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# Installa bash (utile per lo script)
RUN apk add --no-cache bash

# --- COPIA DEI JAR (Applicazioni) ---
COPY --from=build /app/AuthMicroService/target/*.jar /app/auth-service.jar
COPY --from=build /app/ExpenseMicroService/target/*.jar /app/expense-service.jar
COPY --from=build /app/Gateway/target/*.jar /app/gateway-service.jar

# --- COPIA DELLE LICENZE (Crediti Autori) ---
# 1. Creiamo una cartella per tenerle ordinate
RUN mkdir -p /app/licenses/auth /app/licenses/expense /app/licenses/gateway

# 2. Copiamo i file generati dal plugin Maven nella Fase 1
# Nota: Il plugin li mette di default in target/generated-sources/license/
COPY --from=build /app/AuthMicroService/target/generated-sources/license/THIRD-PARTY.txt /app/licenses/auth/THIRD-PARTY.txt
COPY --from=build /app/ExpenseMicroService/target/generated-sources/license/THIRD-PARTY.txt /app/licenses/expense/THIRD-PARTY.txt
COPY --from=build /app/Gateway/target/generated-sources/license/THIRD-PARTY.txt /app/licenses/gateway/THIRD-PARTY.txt

# --- SETUP AVVIO ---
# Copia lo script di avvio e rendilo eseguibile
COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

# Crea utente non-root (obbligatorio per Hugging Face)
RUN adduser -D -u 1000 user
RUN chown -R user:user /app
USER user

# Espone la porta di HF
EXPOSE 7860

# Comando di avvio
ENTRYPOINT ["/bin/sh", "/app/entrypoint.sh"]