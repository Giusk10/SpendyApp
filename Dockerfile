FROM eclipse-temurin:21-jdk-alpine
VOLUME /tmp
WORKDIR /app
# Copiamo il jar generato. Il percorso specifico verrà passato dal contesto di build
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
# JAVA_TOOL_OPTIONS viene letto automaticamente dalla JVM per i limiti di memoria
ENTRYPOINT ["java","-jar","app.jar"]