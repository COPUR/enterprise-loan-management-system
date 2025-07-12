# AmanahFi Platform - Multi-stage Dockerfile for Islamic Finance and CBDC Platform
# Base image: OpenJDK 21 with security updates
FROM bellsoft/liberica-openjdk-alpine:21-cds AS builder

# Install build dependencies
RUN apk add --no-cache git curl

# Set working directory
WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./

# Make gradlew executable
RUN chmod +x gradlew

# Download dependencies (this layer will be cached if dependencies don't change)
COPY build.gradle .
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src/ src/

# Build the application
RUN ./gradlew clean build -x test --no-daemon

# Production image
FROM bellsoft/liberica-openjdk-alpine:21-cds AS production

# Install runtime dependencies and security tools
RUN apk add --no-cache \
    dumb-init \
    curl \
    jq \
    openssl \
    ca-certificates \
    tzdata \
    && update-ca-certificates

# Create non-root user for security
RUN addgroup -g 1001 amanahfi && \
    adduser -D -s /bin/sh -u 1001 -G amanahfi amanahfi

# Set timezone to UAE
ENV TZ=Asia/Dubai

# Create application directories
RUN mkdir -p /app/logs /app/config /app/data && \
    chown -R amanahfi:amanahfi /app

# Switch to non-root user
USER amanahfi

# Set working directory
WORKDIR /app

# Copy JAR file from builder stage
COPY --from=builder --chown=amanahfi:amanahfi /app/build/libs/*.jar app.jar

# Copy configuration files
COPY --chown=amanahfi:amanahfi src/main/resources/application-amanahfi.yml config/
COPY --chown=amanahfi:amanahfi src/main/resources/messages*.properties config/

# Expose application port
EXPOSE 8080

# Health check port for Kubernetes
EXPOSE 8081

# JVM optimization for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -XX:+OptimizeStringConcat \
    -Djava.security.egd=file:/dev/urandom \
    -Djava.awt.headless=true \
    -Dspring.profiles.active=production,amanahfi \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=Asia/Dubai \
    -Djava.net.preferIPv4Stack=true"

# Islamic Finance specific environment variables
ENV ISLAMIC_FINANCE_ENABLED=true
ENV SHARIA_COMPLIANCE_MODE=strict
ENV CBDC_INTEGRATION_ENABLED=true

# Security hardening
ENV SPRING_SECURITY_REQUIRE_SSL=true
ENV MANAGEMENT_SECURITY_ENABLED=true

# Health check configuration
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

# Use dumb-init to handle signals properly
ENTRYPOINT ["dumb-init", "--"]

# Start the application
CMD ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

# Labels for image metadata
LABEL maintainer="AmanahFi Platform Team <platform@amanahfi.ae>"
LABEL version="1.0.0"
LABEL description="Sharia-compliant Islamic finance platform with CBDC integration"
LABEL org.opencontainers.image.title="AmanahFi Platform"
LABEL org.opencontainers.image.description="Islamic Finance and Digital Dirham Banking Platform"
LABEL org.opencontainers.image.vendor="AmanahFi Technologies"
LABEL org.opencontainers.image.version="1.0.0"
LABEL org.opencontainers.image.created="2024-12-11"
LABEL org.opencontainers.image.source="https://github.com/amanahfi/amanahfi-platform"
LABEL org.opencontainers.image.documentation="https://docs.amanahfi.ae"
LABEL org.opencontainers.image.licenses="Proprietary"

# Islamic Finance compliance labels
LABEL ae.amanahfi.compliance.sharia="certified"
LABEL ae.amanahfi.compliance.cbuae="compliant"
LABEL ae.amanahfi.compliance.vara="registered"
LABEL ae.amanahfi.compliance.hsa="approved"
LABEL ae.amanahfi.features.cbdc="enabled"
LABEL ae.amanahfi.features.islamic-finance="enabled"
LABEL ae.amanahfi.region="MENAT"
LABEL ae.amanahfi.jurisdiction="UAE"