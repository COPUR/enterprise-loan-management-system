# Deployment Guide - Enterprise Loan Management System

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Environment Configuration](#environment-configuration)
4. [Database Setup](#database-setup)
5. [Docker Deployment](#docker-deployment)
6. [Kubernetes Deployment](#kubernetes-deployment)
7. [CI/CD Pipeline](#cicd-pipeline)
8. [Monitoring Setup](#monitoring-setup)
9. [Security Configuration](#security-configuration)
10. [Troubleshooting](#troubleshooting)

## Overview

This guide provides comprehensive instructions for deploying the Enterprise Loan Management System in various environments. The system supports multiple deployment options including Docker, Kubernetes, and cloud-native platforms.

### Deployment Architecture
```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Load Balancer │────▶│   API Gateway   │────▶│  Microservices  │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                                                          │
                              ┌───────────────────────────┴────────────┐
                              │                                        │
                        ┌─────▼─────┐  ┌──────────┐  ┌──────────┐  ┌─▼────────┐
                        │PostgreSQL │  │  Redis   │  │  Kafka   │  │Elasticsearch│
                        └───────────┘  └──────────┘  └──────────┘  └──────────┘
```

## Prerequisites

### System Requirements
- **CPU**: Minimum 4 cores (8 cores recommended)
- **Memory**: Minimum 16GB RAM (32GB recommended)
- **Storage**: Minimum 100GB SSD
- **OS**: Ubuntu 20.04 LTS or RHEL 8+

### Software Requirements
- Docker 20.10+
- Docker Compose 2.0+
- Kubernetes 1.24+ (for K8s deployment)
- Helm 3.8+ (for K8s deployment)
- PostgreSQL 14+
- Redis 6+
- Java 17+

### Network Requirements
- Outbound HTTPS (443) for external services
- Internal network connectivity between services
- Load balancer with SSL termination

## Environment Configuration

### Environment Variables
Create `.env` file for each environment:

```bash
# Application Configuration
SPRING_PROFILES_ACTIVE=production
SERVER_PORT=8080
APPLICATION_NAME=loan-management-system

# Database Configuration
DATABASE_HOST=postgres.internal.domain
DATABASE_PORT=5432
DATABASE_NAME=loandb
DATABASE_USER=loanuser
DATABASE_PASSWORD=${VAULT_DB_PASSWORD}
DATABASE_SSL_MODE=require

# Redis Configuration
REDIS_HOST=redis.internal.domain
REDIS_PORT=6379
REDIS_PASSWORD=${VAULT_REDIS_PASSWORD}
REDIS_SSL_ENABLED=true

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=kafka1:9092,kafka2:9092,kafka3:9092
KAFKA_SECURITY_PROTOCOL=SASL_SSL
KAFKA_SASL_MECHANISM=PLAIN
KAFKA_SASL_USERNAME=loanservice
KAFKA_SASL_PASSWORD=${VAULT_KAFKA_PASSWORD}

# Security Configuration
JWT_SECRET=${VAULT_JWT_SECRET}
OAUTH2_ISSUER_URI=https://auth.loanmanagement.com
OAUTH2_CLIENT_ID=loan-management-service
OAUTH2_CLIENT_SECRET=${VAULT_OAUTH_SECRET}

# Monitoring
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics,prometheus
MANAGEMENT_METRICS_EXPORT_PROMETHEUS_ENABLED=true
```

### Application Properties
Configure `application-production.yml`:

```yaml
spring:
  application:
    name: loan-management-system
  
  datasource:
    url: jdbc:postgresql://${DATABASE_HOST}:${DATABASE_PORT}/${DATABASE_NAME}?sslmode=${DATABASE_SSL_MODE}
    username: ${DATABASE_USER}
    password: ${DATABASE_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc:
          batch_size: 25
        order_inserts: true
        order_updates: true
  
  redis:
    host: ${REDIS_HOST}
    port: ${REDIS_PORT}
    password: ${REDIS_PASSWORD}
    ssl: ${REDIS_SSL_ENABLED}
    lettuce:
      pool:
        max-active: 10
        max-idle: 5
        min-idle: 2
  
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
    security:
      protocol: ${KAFKA_SECURITY_PROTOCOL}
    properties:
      sasl:
        mechanism: ${KAFKA_SASL_MECHANISM}
        jaas.config: >
          org.apache.kafka.common.security.plain.PlainLoginModule required
          username="${KAFKA_SASL_USERNAME}"
          password="${KAFKA_SASL_PASSWORD}";

logging:
  level:
    root: INFO
    com.loanmanagement: INFO
  pattern:
    console: "%d{ISO8601} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: /var/log/loan-management/application.log
    max-size: 100MB
    max-history: 30
```

## Database Setup

### PostgreSQL Installation
```bash
# Install PostgreSQL
sudo apt update
sudo apt install postgresql-14 postgresql-contrib-14

# Configure PostgreSQL
sudo -u postgres psql

-- Create database and user
CREATE DATABASE loandb;
CREATE USER loanuser WITH ENCRYPTED PASSWORD 'secure_password';
GRANT ALL PRIVILEGES ON DATABASE loandb TO loanuser;

-- Configure for production
ALTER SYSTEM SET max_connections = 200;
ALTER SYSTEM SET shared_buffers = '4GB';
ALTER SYSTEM SET effective_cache_size = '12GB';
ALTER SYSTEM SET maintenance_work_mem = '1GB';
ALTER SYSTEM SET checkpoint_completion_target = 0.9;
ALTER SYSTEM SET wal_buffers = '16MB';
ALTER SYSTEM SET default_statistics_target = 100;
ALTER SYSTEM SET random_page_cost = 1.1;
```

### Database Migration
```bash
# Run Flyway migrations
./mvnw flyway:migrate \
  -Dflyway.url=jdbc:postgresql://localhost:5432/loandb \
  -Dflyway.user=loanuser \
  -Dflyway.password=secure_password

# Verify migration
./mvnw flyway:info
```

### Database Backup Strategy
```bash
#!/bin/bash
# backup-database.sh
BACKUP_DIR="/backup/postgres"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
DATABASE="loandb"

# Create backup
pg_dump -h localhost -U loanuser -d $DATABASE -F c -b -v -f "$BACKUP_DIR/loandb_$TIMESTAMP.backup"

# Upload to S3
aws s3 cp "$BACKUP_DIR/loandb_$TIMESTAMP.backup" s3://loan-backups/postgres/

# Cleanup old backups (keep last 30 days)
find $BACKUP_DIR -name "*.backup" -mtime +30 -delete
```

## Docker Deployment

### Build Docker Image
```dockerfile
# Dockerfile
FROM eclipse-temurin:17-jre-alpine AS runtime

RUN addgroup -g 1000 spring && \
    adduser -u 1000 -G spring -s /bin/sh -D spring

WORKDIR /app

COPY --chown=spring:spring target/loan-management-system.jar app.jar

# Add health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", \
  "app.jar"]
```

### Docker Compose Deployment
```yaml
# docker-compose.production.yml
version: '3.8'

services:
  loan-service:
    image: loan-management-system:latest
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - DATABASE_HOST=postgres
      - REDIS_HOST=redis
      - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    networks:
      - loan-network
    deploy:
      replicas: 3
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '1'
          memory: 1G
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"

  postgres:
    image: postgres:14-alpine
    environment:
      POSTGRES_DB: loandb
      POSTGRES_USER: loanuser
      POSTGRES_PASSWORD_FILE: /run/secrets/db_password
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-scripts:/docker-entrypoint-initdb.d
    secrets:
      - db_password
    networks:
      - loan-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U loanuser"]
      interval: 30s
      timeout: 5s
      retries: 5

  redis:
    image: redis:6-alpine
    command: redis-server --requirepass_file /run/secrets/redis_password
    volumes:
      - redis_data:/data
    secrets:
      - redis_password
    networks:
      - loan-network
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 30s
      timeout: 5s
      retries: 5

  nginx:
    image: nginx:alpine
    ports:
      - "443:443"
      - "80:80"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/ssl:/etc/nginx/ssl:ro
    depends_on:
      - loan-service
    networks:
      - loan-network

networks:
  loan-network:
    driver: bridge

volumes:
  postgres_data:
  redis_data:

secrets:
  db_password:
    file: ./secrets/db_password.txt
  redis_password:
    file: ./secrets/redis_password.txt
```

### Deploy with Docker Compose
```bash
# Build and deploy
docker-compose -f docker-compose.production.yml up -d

# Scale services
docker-compose -f docker-compose.production.yml up -d --scale loan-service=3

# View logs
docker-compose -f docker-compose.production.yml logs -f loan-service

# Health check
curl https://localhost/actuator/health
```

## Kubernetes Deployment

### Kubernetes Manifests

#### Namespace and ConfigMap
```yaml
# namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: loan-management
---
# configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: loan-management-config
  namespace: loan-management
data:
  application.yml: |
    spring:
      profiles:
        active: production
      application:
        name: loan-management-system
    server:
      port: 8080
```

#### Deployment
```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: loan-management-service
  namespace: loan-management
  labels:
    app: loan-management
spec:
  replicas: 3
  selector:
    matchLabels:
      app: loan-management
  template:
    metadata:
      labels:
        app: loan-management
    spec:
      serviceAccountName: loan-management-sa
      containers:
      - name: loan-management
        image: loan-management-system:latest
        imagePullPolicy: Always
        ports:
        - containerPort: 8080
          name: http
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: DATABASE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: loan-management-secrets
              key: database-password
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: loan-management-secrets
              key: jwt-secret
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
        volumeMounts:
        - name: config
          mountPath: /app/config
        - name: logs
          mountPath: /var/log/loan-management
      volumes:
      - name: config
        configMap:
          name: loan-management-config
      - name: logs
        emptyDir: {}
```

#### Service and Ingress
```yaml
# service.yaml
apiVersion: v1
kind: Service
metadata:
  name: loan-management-service
  namespace: loan-management
spec:
  selector:
    app: loan-management
  ports:
  - port: 80
    targetPort: 8080
    name: http
  type: ClusterIP
---
# ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: loan-management-ingress
  namespace: loan-management
  annotations:
    kubernetes.io/ingress.class: nginx
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/rate-limit: "100"
spec:
  tls:
  - hosts:
    - api.loanmanagement.com
    secretName: loan-management-tls
  rules:
  - host: api.loanmanagement.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: loan-management-service
            port:
              number: 80
```

### Helm Deployment

#### Helm Chart Structure
```
helm/loan-management/
├── Chart.yaml
├── values.yaml
├── templates/
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── ingress.yaml
│   ├── configmap.yaml
│   ├── secrets.yaml
│   └── hpa.yaml
```

#### values.yaml
```yaml
replicaCount: 3

image:
  repository: loan-management-system
  pullPolicy: IfNotPresent
  tag: "latest"

service:
  type: ClusterIP
  port: 80

ingress:
  enabled: true
  className: "nginx"
  annotations:
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
  hosts:
    - host: api.loanmanagement.com
      paths:
        - path: /
          pathType: Prefix
  tls:
    - secretName: loan-management-tls
      hosts:
        - api.loanmanagement.com

resources:
  limits:
    cpu: 1000m
    memory: 2Gi
  requests:
    cpu: 500m
    memory: 1Gi

autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
  targetMemoryUtilizationPercentage: 80

postgresql:
  enabled: true
  auth:
    database: loandb
    username: loanuser
    existingSecret: postgresql-secret
  primary:
    persistence:
      size: 50Gi
    resources:
      limits:
        memory: 2Gi
        cpu: 1000m

redis:
  enabled: true
  auth:
    enabled: true
    existingSecret: redis-secret
  master:
    persistence:
      size: 10Gi
```

#### Deploy with Helm
```bash
# Add required repositories
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update

# Install the chart
helm install loan-management ./helm/loan-management \
  --namespace loan-management \
  --create-namespace \
  --values ./helm/loan-management/values.yaml \
  --values ./helm/loan-management/values.production.yaml

# Upgrade deployment
helm upgrade loan-management ./helm/loan-management \
  --namespace loan-management \
  --values ./helm/loan-management/values.yaml \
  --values ./helm/loan-management/values.production.yaml

# Check status
helm status loan-management -n loan-management
```

## CI/CD Pipeline

### GitHub Actions Workflow
```yaml
# .github/workflows/deploy.yml
name: Deploy to Production

on:
  push:
    branches: [main]
  workflow_dispatch:

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  build:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Cache Maven dependencies
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
    
    - name: Run tests
      run: ./mvnw test
    
    - name: Build application
      run: ./mvnw clean package -DskipTests
    
    - name: Set up Docker Buildx
      uses: docker/setup-buildx-action@v2
    
    - name: Log in to Container Registry
      uses: docker/login-action@v2
      with:
        registry: ${{ env.REGISTRY }}
        username: ${{ github.actor }}
        password: ${{ secrets.GITHUB_TOKEN }}
    
    - name: Build and push Docker image
      uses: docker/build-push-action@v4
      with:
        context: .
        push: true
        tags: |
          ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:latest
          ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ github.sha }}
        cache-from: type=gha
        cache-to: type=gha,mode=max

  deploy:
    needs: build
    runs-on: ubuntu-latest
    environment: production
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v3
    
    - name: Install kubectl
      uses: azure/setup-kubectl@v3
      with:
        version: 'v1.24.0'
    
    - name: Configure kubectl
      run: |
        echo "${{ secrets.KUBE_CONFIG }}" | base64 -d > kubeconfig
        export KUBECONFIG=kubeconfig
    
    - name: Deploy to Kubernetes
      run: |
        kubectl set image deployment/loan-management-service \
          loan-management=${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ github.sha }} \
          -n loan-management
        
        kubectl rollout status deployment/loan-management-service -n loan-management
    
    - name: Run smoke tests
      run: |
        ./scripts/smoke-tests.sh https://api.loanmanagement.com
```

### GitLab CI Pipeline
```yaml
# .gitlab-ci.yml
stages:
  - test
  - build
  - deploy

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository"
  DOCKER_DRIVER: overlay2
  DOCKER_TLS_CERTDIR: ""

cache:
  paths:
    - .m2/repository/

test:
  stage: test
  image: maven:3.8-openjdk-17
  script:
    - mvn test
  artifacts:
    reports:
      junit:
        - target/surefire-reports/TEST-*.xml

build:
  stage: build
  image: docker:latest
  services:
    - docker:dind
  before_script:
    - docker login -u $CI_REGISTRY_USER -p $CI_REGISTRY_PASSWORD $CI_REGISTRY
  script:
    - mvn clean package -DskipTests
    - docker build -t $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA .
    - docker push $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA
    - docker tag $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA $CI_REGISTRY_IMAGE:latest
    - docker push $CI_REGISTRY_IMAGE:latest

deploy:
  stage: deploy
  image: bitnami/kubectl:latest
  script:
    - kubectl set image deployment/loan-management-service 
        loan-management=$CI_REGISTRY_IMAGE:$CI_COMMIT_SHA 
        -n loan-management
    - kubectl rollout status deployment/loan-management-service -n loan-management
  environment:
    name: production
    url: https://api.loanmanagement.com
  only:
    - main
```

## Monitoring Setup

### Prometheus Configuration
```yaml
# prometheus-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-config
  namespace: monitoring
data:
  prometheus.yml: |
    global:
      scrape_interval: 15s
      evaluation_interval: 15s
    
    scrape_configs:
    - job_name: 'loan-management'
      kubernetes_sd_configs:
      - role: pod
        namespaces:
          names:
          - loan-management
      relabel_configs:
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        action: keep
        regex: true
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
        action: replace
        target_label: __metrics_path__
        regex: (.+)
      - source_labels: [__address__, __meta_kubernetes_pod_annotation_prometheus_io_port]
        action: replace
        regex: ([^:]+)(?::\d+)?;(\d+)
        replacement: $1:$2
        target_label: __address__
```

### Grafana Dashboard
```json
{
  "dashboard": {
    "title": "Loan Management System",
    "panels": [
      {
        "title": "Request Rate",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count{application=\"loan-management-system\"}[5m])"
          }
        ]
      },
      {
        "title": "Response Time",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{application=\"loan-management-system\"}[5m]))"
          }
        ]
      },
      {
        "title": "Error Rate",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count{application=\"loan-management-system\",status=~\"5..\"}[5m])"
          }
        ]
      }
    ]
  }
}
```

### ELK Stack Configuration
```yaml
# filebeat-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: filebeat-config
  namespace: logging
data:
  filebeat.yml: |
    filebeat.inputs:
    - type: container
      paths:
        - /var/log/containers/*loan-management*.log
      processors:
        - add_kubernetes_metadata:
            host: ${NODE_NAME}
            matchers:
            - logs_path:
                logs_path: "/var/log/containers/"
    
    output.elasticsearch:
      hosts: ['${ELASTICSEARCH_HOST:elasticsearch}:${ELASTICSEARCH_PORT:9200}']
      username: ${ELASTICSEARCH_USERNAME}
      password: ${ELASTICSEARCH_PASSWORD}
```

## Security Configuration

### SSL/TLS Setup
```nginx
# nginx-ssl.conf
server {
    listen 443 ssl http2;
    server_name api.loanmanagement.com;
    
    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;
    
    add_header Strict-Transport-Security "max-age=63072000" always;
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    
    location / {
        proxy_pass http://loan-management-backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### Secrets Management
```bash
# Using Kubernetes Secrets
kubectl create secret generic loan-management-secrets \
  --from-literal=database-password='secure_password' \
  --from-literal=jwt-secret='jwt_secret_key' \
  --from-literal=oauth-client-secret='oauth_secret' \
  -n loan-management

# Using HashiCorp Vault
vault kv put secret/loan-management/production \
  database_password='secure_password' \
  jwt_secret='jwt_secret_key' \
  oauth_client_secret='oauth_secret'
```

### Network Policies
```yaml
# network-policy.yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: loan-management-netpol
  namespace: loan-management
spec:
  podSelector:
    matchLabels:
      app: loan-management
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: ingress-nginx
    - podSelector:
        matchLabels:
          app: api-gateway
    ports:
    - protocol: TCP
      port: 8080
  egress:
  - to:
    - namespaceSelector:
        matchLabels:
          name: database
    ports:
    - protocol: TCP
      port: 5432
  - to:
    - namespaceSelector:
        matchLabels:
          name: cache
    ports:
    - protocol: TCP
      port: 6379
```

## Troubleshooting

### Common Deployment Issues

#### 1. Database Connection Failed
```bash
# Check database connectivity
kubectl exec -it loan-management-pod -n loan-management -- nc -zv postgres-service 5432

# Check database credentials
kubectl get secret loan-management-secrets -n loan-management -o jsonpath='{.data.database-password}' | base64 -d

# View application logs
kubectl logs -f deployment/loan-management-service -n loan-management
```

#### 2. Out of Memory Errors
```bash
# Check resource usage
kubectl top pods -n loan-management

# Increase memory limits
kubectl set resources deployment/loan-management-service \
  --limits=memory=4Gi --requests=memory=2Gi -n loan-management

# Check JVM heap settings
kubectl exec -it loan-management-pod -n loan-management -- jcmd 1 VM.flags
```

#### 3. Service Not Accessible
```bash
# Check service endpoints
kubectl get endpoints -n loan-management

# Check ingress status
kubectl describe ingress loan-management-ingress -n loan-management

# Test internal connectivity
kubectl run -it --rm debug --image=busybox --restart=Never -- \
  wget -O- http://loan-management-service.loan-management.svc.cluster.local/actuator/health
```

### Performance Tuning

#### JVM Optimization
```bash
# Optimal JVM flags for containerized environments
JAVA_OPTS="-XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:InitialRAMPercentage=50.0 \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+ParallelRefProcEnabled \
  -XX:+AlwaysPreTouch \
  -XX:+DisableExplicitGC"
```

#### Database Connection Pool
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      connection-test-query: SELECT 1
```

### Rollback Procedures

```bash
# Kubernetes rollback
kubectl rollout undo deployment/loan-management-service -n loan-management

# Helm rollback
helm rollback loan-management 1 -n loan-management

# Database rollback
flyway undo -target=V1.2

# Docker rollback
docker-compose -f docker-compose.production.yml up -d --no-deps loan-service
```

## Maintenance Procedures

### Zero-Downtime Deployment
```bash
#!/bin/bash
# rolling-update.sh

# Step 1: Deploy canary instance
kubectl set image deployment/loan-management-canary \
  loan-management=loan-management:new-version \
  -n loan-management

# Step 2: Route 10% traffic to canary
kubectl patch virtualservice loan-management \
  --type merge \
  -p '{"spec":{"http":[{"weight":10,"destination":{"host":"loan-management-canary"}}]}}'

# Step 3: Monitor metrics
./scripts/monitor-canary.sh

# Step 4: Full rollout if successful
kubectl set image deployment/loan-management-service \
  loan-management=loan-management:new-version \
  -n loan-management
```

### Backup and Restore
```bash
# Backup script
#!/bin/bash
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Database backup
kubectl exec -it postgres-0 -n database -- \
  pg_dump -U loanuser loandb > backup_$TIMESTAMP.sql

# Application state backup
kubectl get all,configmap,secret -n loan-management -o yaml > k8s_backup_$TIMESTAMP.yaml

# Upload to S3
aws s3 cp backup_$TIMESTAMP.sql s3://loan-backups/
aws s3 cp k8s_backup_$TIMESTAMP.yaml s3://loan-backups/
```

## Conclusion

This deployment guide provides comprehensive instructions for deploying the Enterprise Loan Management System in production. Follow the security best practices, implement proper monitoring, and maintain regular backups to ensure a stable and secure deployment.