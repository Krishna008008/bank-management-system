# ==============================================================================
# Multi-Stage Dockerfile for Spring Boot Bank Management System
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build & Package with Maven
# ------------------------------------------------------------------------------
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build executable Spring Boot JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# ------------------------------------------------------------------------------
# Stage 2: JRE Runtime Image
# ------------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Set non-root user for security
RUN addgroup -S bankgroup && adduser -S bankuser -G bankgroup

# Create directory for SQLite persistent database and Log4j2 logs
RUN mkdir -p /app/data /app/logs && chown -R bankuser:bankgroup /app

# Copy Spring Boot executable JAR
COPY --from=builder --chown=bankuser:bankgroup /build/target/bank-management-*.jar /app/app.jar

USER bankuser

# Expose Web Portal and REST API port
EXPOSE 8080

ENV PORT=8080

# Run Spring Boot application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
