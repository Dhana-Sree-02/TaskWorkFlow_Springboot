# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and build definitions
COPY pom.xml .
COPY src ./src

# Package the application (builds executable WAR, skipping tests for speed)
RUN mvn clean package -DskipTests

# Stage 2: Runtime environment
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built WAR file from the build stage (using wildcard to handle versioning)
COPY --from=build /app/target/*.war app.war

# Expose port 8001 (configured in application.properties)
EXPOSE 8001

# Execute the application
ENTRYPOINT ["java", "-jar", "app.war"]
