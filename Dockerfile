# -------- STAGE 1: Build the JAR --------
FROM maven:3.9.4-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy the project files into the container
COPY . .

# Build the project and skip tests (optional)
RUN mvn clean package -DskipTests

# -------- STAGE 2: Run the JAR --------
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Copy only the built JAR from the builder stage
COPY --from=builder /app/target/kittyp-0.0.1-SNAPSHOT.jar kittyp.jar

# Set the Spring profile and port
ENV SPRING_PROFILES_ACTIVE=dev
ENV PORT=8001

# Expose the port (informational for Docker, not binding)
EXPOSE 8001

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "kittyp.jar"]
