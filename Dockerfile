# Use a small Java 21 base image
FROM eclipse-temurin:21-jdk-alpine

# Create app directory
WORKDIR /app

# Copy your JAR into the image
COPY target/kittyp-0.0.1-SNAPSHOT.jar kittyp.jar

# Expose the port your Spring Boot app runs on
EXPOSE 8001

ENV SPRING_PROFILES_ACTIVE=local

# Command to run the jar
ENTRYPOINT ["java", "-jar", "kittyp.jar"]
