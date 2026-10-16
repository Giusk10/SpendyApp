# ==============================================================================
# Dockerfile Multi-Stage per SpendyApp
# Target disponibili:
#   - auth-service:     Container isolato per AuthMicroService (Porta 8081)
#   - expense-service:  Container isolato per ExpenseMicroService (Porta 8084)
#   - gateway-service:  Container isolato per Spring Cloud Gateway (Porta 7860)
#   - all-in-one:       Target di default per Hugging Face Spaces (Porta 7860)
# ==============================================================================

# --- FASE 1: Build (Compilazione Maven centralizzata con Reactor) ---
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# 1. Copia preventiva dei POM per caching efficace delle dipendenze
COPY pom.xml ./
COPY AuthMicroService/pom.xml ./AuthMicroService/
COPY ExpenseMicroService/pom.xml ./ExpenseMicroService/
COPY Gateway/pom.xml ./Gateway/

# Download delle dipendenze esterne
RUN mvn dependency:go-offline -B || true

# 2. Copia dei file sorgente dei moduli applicativi
COPY AuthMicroService/src ./AuthMicroService/src
COPY ExpenseMicroService/src ./ExpenseMicroService/src
COPY Gateway/src ./Gateway/src

# 3. Compilazione unificata e generazione dei file THIRD-PARTY di licenza
RUN mvn clean package -DskipTests -B

# --- FASE 2: Immagine Base Runtime Leggera ---
FROM eclipse-temurin:21-jre-alpine AS base
WORKDIR /app
RUN apk add --no-cache bash curl wget openssl && \
    adduser -D -u 1000 user && \
    chown -R user:user /app

# --- TARGET: Auth Microservice ---
FROM base AS auth-service
USER user
COPY --from=build --chown=user:user /app/AuthMicroService/target/AuthMicroService-*.jar /app/auth-service.jar
EXPOSE 8081
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -q -O - http://localhost:8081/actuator/health | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["java", "-Dserver.port=8081", "-jar", "/app/auth-service.jar"]

# --- TARGET: Expense Microservice ---
FROM base AS expense-service
USER user
COPY --from=build --chown=user:user /app/ExpenseMicroService/target/ExpenseMicroService-*.jar /app/expense-service.jar
EXPOSE 8084
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -q -O - http://localhost:8084/actuator/health | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["java", "-Dserver.port=8084", "-jar", "/app/expense-service.jar"]

# --- TARGET: Gateway Service ---
FROM base AS gateway-service
USER user
COPY --from=build --chown=user:user /app/Gateway/target/Gateway-*.jar /app/gateway-service.jar
EXPOSE 7860
HEALTHCHECK --interval=15s --timeout=5s --start-period=20s --retries=3 \
  CMD wget -q -O - http://localhost:7860/actuator/health | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["java", "-Dserver.port=7860", "-jar", "/app/gateway-service.jar"]

# --- TARGET (Default): All-in-One per Hugging Face Spaces ---
FROM base AS all-in-one

# Copia dei JAR applicativi
COPY --from=build --chown=user:user /app/AuthMicroService/target/AuthMicroService-*.jar /app/auth-service.jar
COPY --from=build --chown=user:user /app/ExpenseMicroService/target/ExpenseMicroService-*.jar /app/expense-service.jar
COPY --from=build --chown=user:user /app/Gateway/target/Gateway-*.jar /app/gateway-service.jar

# Copia delle licenze delle dipendenze terze (compliance)
RUN mkdir -p /app/licenses/auth /app/licenses/expense /app/licenses/gateway
COPY --from=build /app/AuthMicroService/target/generated-sources/license/THIRD-PARTY.txt /app/licenses/auth/THIRD-PARTY.txt
COPY --from=build /app/ExpenseMicroService/target/generated-sources/license/THIRD-PARTY.txt /app/licenses/expense/THIRD-PARTY.txt
COPY --from=build /app/Gateway/target/generated-sources/license/THIRD-PARTY.txt /app/licenses/gateway/THIRD-PARTY.txt

# Setup script di avvio e permessi
COPY deploy/huggingface/entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh && chown -R user:user /app
USER user

# Espone la porta perimetrale HF Spaces
EXPOSE 7860

HEALTHCHECK --interval=20s --timeout=5s --start-period=45s --retries=3 \
  CMD wget -q -O - http://localhost:7860/actuator/health | grep -q '"status":"UP"' || exit 1

# Comando di avvio supervisionato
ENTRYPOINT ["/bin/sh", "/app/entrypoint.sh"]