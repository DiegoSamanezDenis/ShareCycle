# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Build Spring Boot jar without tests
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
# Copy the fat jar built by Spring Boot
COPY --from=build /app/target/*-SNAPSHOT.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]

