# Этап 1: Сборка исполняемого JAR через Maven (Java 23)
FROM maven:3.9-eclipse-temurin-23 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Этап 2: Запуск приложения
FROM eclipse-temurin:23-jre
WORKDIR /app
COPY --from=build /app/target/BankService-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]