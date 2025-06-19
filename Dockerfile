# Multi-stage Dockerfile for Enterprise Loan Management System
# Following 12-Factor App methodology and banking security standards
# Optimized for Java 21 with Spring Boot 3.3.6

# Build stage - Using OpenJDK 21 with Gradle 8.11.1
FROM gradle:8.11.1-jdk21-alpine AS builder

# Metadata for enterprise tracking and compliance
LABEL maintainer="Enterprise Banking Team <team@banking.com>"
LABEL version="1.0.0"
LABEL description="Enterprise Loan Management System - Banking Microservice"
LABEL vendor="Banking Solutions Inc."
LABEL build-date="2025-06-19"
LABEL spring-boot-version="3.3.6"
LABEL java-version="21"

# Set working directory
WORKDIR /app

# Copy Gradle wrapper and build files first (for better layer caching)
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./

# Download dependencies first (cache layer optimization)
RUN gradle dependencies --no-daemon

# Copy source code and configuration
COPY src/ src/

# Build the application with comprehensive validation
# Note: Skipping tests in Docker build for faster builds - tests run in CI/CD pipeline
RUN gradle clean bootJar buildInfo --no-daemon -x test \
    && cp build/libs/enterprise-loan-management-system.jar app.jar \
    && ls -la build/

# Runtime stage - Minimal JRE for production
FROM eclipse-temurin:21-jre-alpine AS runtime

# Install security updates and required packages for banking compliance
RUN apk update \
    && apk add --no-cache \
        curl \
        ca-certificates \
        tzdata \
        dumb-init \
    && update-ca-certificates

# Create non-root user for security (banking compliance requirement)
RUN addgroup -S banking && adduser -S -G banking -u 1000 -h /app -s /bin/false banking

# Set working directory
WORKDIR /app

# Create required directories
RUN mkdir -p /app/logs /app/config /app/tmp \
    && chown -R banking:banking /app

# Copy application jar and build info from builder stage
COPY --from=builder --chown=banking:banking /app/app.jar ./

# Copy build info if it exists
RUN --mount=from=builder,source=/app/build/resources/main,target=/tmp/build \
    cp /tmp/build/build-info.properties ./ 2>/dev/null || echo "Build info not found, skipping..." \
    && chown banking:banking build-info.properties 2>/dev/null || true

# Copy configuration and scripts
COPY --chown=banking:banking docker/entrypoint.sh /app/entrypoint.sh
COPY --chown=banking:banking docker/healthcheck.sh /app/healthcheck.sh

# Set executable permissions
RUN chmod +x /app/entrypoint.sh /app/healthcheck.sh

# Switch to non-root user
USER banking

# Expose application port (12-Factor: Port Binding)
EXPOSE 8080

# Health check endpoint for container orchestration
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD /app/healthcheck.sh || exit 1

# Set JVM options for production (Java 21 optimizations with 12-Factor principles)
ENV JAVA_OPTS="\
    -server \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:G1HeapRegionSize=16m \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+ExitOnOutOfMemoryError \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/logs/heapdump.hprof \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=production \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=UTC"

# 12-Factor App: Environment variables for configuration
ENV SERVER_PORT=8080
ENV SPRING_PROFILES_ACTIVE=production
ENV DATABASE_URL=jdbc:postgresql://postgres:5432/banking
ENV DATABASE_USERNAME=banking_user
ENV DATABASE_PASSWORD=""
ENV REDIS_HOST=redis
ENV REDIS_PORT=6379
ENV KAFKA_BOOTSTRAP_SERVERS=kafka:9092
ENV EUREKA_URL=http://eureka:8761/eureka
ENV LOG_LEVEL_APP=INFO
ENV JPA_DDL_AUTO=validate
ENV ACTUATOR_ENDPOINTS=health,info,metrics,prometheus

# Application metadata
ENV APP_NAME="Enterprise Loan Management System"
ENV APP_VERSION="1.0.0"
ENV APP_DESCRIPTION="Banking microservice with loan management capabilities"

# Security and compliance environment variables
ENV BANKING_COMPLIANCE_STRICT=true
ENV FAPI_ENABLED=true
ENV PCI_ENABLED=true
ENV AUDIT_ENABLED=true
ENV KYC_REQUIRED=true

# Performance tuning environment variables
ENV CACHE_TTL=3600
ENV CONNECTION_TIMEOUT=5000
ENV READ_TIMEOUT=10000
ENV MAX_CONCURRENT_REQUESTS=100

# Volume for logs and temporary files (12-Factor: Logs)
VOLUME ["/app/logs", "/app/tmp"]

# Start the application using dumb-init for proper signal handling
ENTRYPOINT ["/usr/bin/dumb-init", "--", "java", "-jar", "app.jar"]

# Multi-architecture support labels
ARG BUILDPLATFORM
ARG TARGETPLATFORM
RUN echo "Building for $TARGETPLATFORM on $BUILDPLATFORM" || true

# Final stage metadata for compliance and tracking
LABEL org.opencontainers.image.title="Enterprise Loan Management System"
LABEL org.opencontainers.image.description="Banking microservice with comprehensive loan management"
LABEL org.opencontainers.image.version="1.0.0"
LABEL org.opencontainers.image.authors="Enterprise Banking Team"
LABEL org.opencontainers.image.vendor="Banking Solutions Inc."
LABEL org.opencontainers.image.licenses="Proprietary"
LABEL org.opencontainers.image.source="https://github.com/banking/enterprise-loan-management"
LABEL org.opencontainers.image.documentation="https://docs.banking.com/loan-management"

# Security and compliance labels
LABEL banking.compliance.fapi="true"
LABEL banking.compliance.pci="true"
LABEL banking.security.level="high"
LABEL banking.audit.enabled="true"
LABEL banking.environment="production"