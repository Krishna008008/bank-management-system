# ==============================================================================
# Multi-Stage Dockerfile for Apex Bank Management System
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build & Package with Maven
# ------------------------------------------------------------------------------
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# 1. Copy pom.xml and download dependencies first (leveraging Docker layer cache)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 2. Copy source code and build executable fat JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# ------------------------------------------------------------------------------
# Stage 2: Ultra-lightweight JRE Runtime Image
# ------------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Set non-root user for container security
RUN addgroup -S bankgroup && adduser -S bankuser -G bankgroup

# Create directory for SQLite persistent database and Log4j2 logs
RUN mkdir -p /app/data /app/logs && chown -R bankuser:bankgroup /app

# Copy packaged JAR from builder stage
COPY --from=builder --chown=bankuser:bankgroup /build/target/bank-management-*-jar-with-dependencies.jar /app/app.jar

USER bankuser

# Expose Web Portal and REST API port
EXPOSE 8080

# Environment variables
ENV PORT=8080

# Start embedded Web Server and REST API
ENTRYPOINT ["java", "-cp", "/app/app.jar", "com.example.bank.api.BankServer"]
