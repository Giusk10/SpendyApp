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

# --- FASE BASE: Runtime ---
FROM eclipse-temurin:21-jdk-alpine AS base
WORKDIR /app
RUN apk add --no-cache bash curl wget openssl
RUN adduser -D -u 1000 user
RUN chown -R user:user /app

# --- TARGET: Auth Microservice ---
FROM base AS auth-service
USER user
COPY --from=build --chown=user:user /app/AuthMicroService/target/*.jar /app/auth-service.jar
EXPOSE 8081
ENTRYPOINT ["java", "-Dserver.port=8081", "-jar", "/app/auth-service.jar"]

# --- TARGET: Expense Microservice ---
FROM base AS expense-service
USER user
COPY --from=build --chown=user:user /app/ExpenseMicroService/target/*.jar /app/expense-service.jar
EXPOSE 8084
ENTRYPOINT ["java", "-Dserver.port=8084", "-jar", "/app/expense-service.jar"]

# --- TARGET: Gateway Service ---
FROM base AS gateway-service
USER user
COPY --from=build --chown=user:user /app/Gateway/target/*.jar /app/gateway-service.jar
EXPOSE 7860 8080
ENTRYPOINT ["java", "-Dserver.port=7860", "-jar", "/app/gateway-service.jar"]

# --- TARGET (Default): All-in-One ---
FROM base AS all-in-one

# --- COPIA DEI JAR (Applicazioni) ---
COPY --from=build /app/AuthMicroService/target/*.jar /app/auth-service.jar
COPY --from=build /app/ExpenseMicroService/target/*.jar /app/expense-service.jar
COPY --from=build /app/Gateway/target/*.jar /app/gateway-service.jar

# --- COPIA DELLE LICENZE (Crediti Autori) ---
RUN mkdir -p /app/licenses/auth /app/licenses/expense /app/licenses/gateway
COPY --from=build /app/AuthMicroService/target/generated-sources/license/THIRD-PARTY.txt /app/licenses/auth/THIRD-PARTY.txt
COPY --from=build /app/ExpenseMicroService/target/generated-sources/license/THIRD-PARTY.txt /app/licenses/expense/THIRD-PARTY.txt
COPY --from=build /app/Gateway/target/generated-sources/license/THIRD-PARTY.txt /app/licenses/gateway/THIRD-PARTY.txt

# --- SETUP AVVIO ---
COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh
RUN chown -R user:user /app
USER user

# Espone la porta di HF
EXPOSE 7860

# Comando di avvio
ENTRYPOINT ["/bin/sh", "/app/entrypoint.sh"]