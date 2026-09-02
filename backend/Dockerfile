FROM maven:3.9.16-eclipse-temurin-25-alpine AS build

WORKDIR /app
COPY pom.xml ./
COPY src ./src

# Tests that start Spring need a database; verify the running stack separately.
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -DskipTests package

FROM eclipse-temurin:25-jre-alpine

WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build --chown=spring:spring /app/target/*.jar app.jar
USER spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
