# --- FASE 1: Build (Compilazione) ---
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copia tutto il codice sorgente
COPY . .

# Compila Auth Service
WORKDIR /app/auth-service
RUN mvn clean package -DskipTests

# Compila Expense Service
WORKDIR /app/expense-service
RUN mvn clean package -DskipTests

# Compila Gateway Service
WORKDIR /app/gateway-service
RUN mvn clean package -DskipTests

# --- FASE 2: Esecuzione (Immagine finale leggera) ---
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# Installa bash (utile per lo script)
RUN apk add --no-cache bash

# Copia i JAR compilati dalla fase precedente
COPY --from=build /app/auth-service/target/*.jar /app/auth-service.jar
COPY --from=build /app/expense-service/target/*.jar /app/expense-service.jar
COPY --from=build /app/gateway-service/target/*.jar /app/gateway-service.jar

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