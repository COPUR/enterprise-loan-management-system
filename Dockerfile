# Multi-stage build for Enterprise Loan Management System
FROM openjdk:21-jdk-slim AS builder

# Set working directory
WORKDIR /app

# Copy gradle wrapper and build files
COPY gradlew .
COPY gradle gradle/
COPY build.gradle settings.gradle ./

# Copy source code
COPY src src/

# Build application
RUN chmod +x gradlew && \
    ./gradlew bootJar --no-daemon

# Production image
FROM openjdk:21-jre-slim

# Install required packages and create user
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        curl \
        ca-certificates && \
    rm -rf /var/lib/apt/lists/* && \
    groupadd -r banking && \
    useradd -r -g banking -u 1000 banking && \
    mkdir -p /app/logs /app/config && \
    chown -R banking:banking /app

# Set working directory
WORKDIR /app

# Copy application jar from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Copy configuration files
COPY --chown=banking:banking docker/entrypoint.sh /app/entrypoint.sh
COPY --chown=banking:banking docker/healthcheck.sh /app/healthcheck.sh

# Set permissions
RUN chmod +x /app/entrypoint.sh /app/healthcheck.sh

# Switch to non-root user
USER banking

# Expose port
EXPOSE 5000

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD /app/healthcheck.sh

# Set JVM options for containerized environment
ENV JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseContainerSupport -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs/"

# Entry point
ENTRYPOINT ["/app/entrypoint.sh"]