# Multi-stage build: compile with a JDK+Maven image, run on a slim JRE image.

FROM eclipse-temurin:25-jdk AS build
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/dunning-engine-0.0.1-SNAPSHOT.jar app.jar
# Render sets $PORT at runtime; application.yml already reads it via ${PORT:8080}
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
