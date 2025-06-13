# Production Deployment Guide
## Enterprise Loan Management System - Microservices Architecture

### Overview

This guide provides comprehensive instructions for deploying the Enterprise Loan Management System microservices architecture in production environments with Docker, Kubernetes, and cloud-native configurations.

---

## 1. Infrastructure Requirements

### 1.1 Minimum System Requirements

**Development Environment**:
- CPU: 4 cores, 2.4GHz
- RAM: 8GB
- Storage: 50GB SSD
- Network: 100 Mbps

**Production Environment**:
- CPU: 16 cores, 2.8GHz (per node)
- RAM: 32GB (per node)
- Storage: 500GB SSD (per node)
- Network: 1 Gbps
- Load Balancer: Application Load Balancer with SSL termination

### 1.2 Software Dependencies

**Required Software**:
- Java 21 (OpenJDK or Oracle JDK)
- Gradle 9.0+
- PostgreSQL 16.9+
- Redis 7.0+
- Apache Kafka 3.5+ (for SAGA events)
- Docker 24.0+
- Kubernetes 1.28+

**Optional but Recommended**:
- Prometheus (monitoring)
- Grafana (dashboards)
- Jaeger (distributed tracing)
- Elasticsearch + Kibana (log aggregation)

---

## 2. Environment Configuration

### 2.1 Environment Variables

Create environment-specific configuration files:

**`.env.development`**:
```bash
# Database Configuration
DATABASE_URL=jdbc:postgresql://localhost:5432/banking_system
PGHOST=localhost
PGPORT=5432
PGUSER=banking_user
PGPASSWORD=secure_password
PGDATABASE=banking_system

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=redis_password

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_SECURITY_PROTOCOL=PLAINTEXT

# Service Ports
API_GATEWAY_PORT=8080
CUSTOMER_SERVICE_PORT=8081
LOAN_SERVICE_PORT=8082
PAYMENT_SERVICE_PORT=8083

# Security Configuration
JWT_SECRET_KEY=your_jwt_secret_key_here
JWT_EXPIRATION_MS=3600000
ENCRYPTION_KEY=your_encryption_key_here

# External APIs
CREDIT_BUREAU_API_URL=https://api.creditbureau.com
PAYMENT_GATEWAY_API_URL=https://api.paymentgateway.com

# Monitoring
PROMETHEUS_ENABLED=true
JAEGER_ENABLED=false
LOG_LEVEL=INFO
```

**`.env.production`**:
```bash
# Database Configuration (use connection pooling)
DATABASE_URL=jdbc:postgresql://prod-db-cluster:5432/banking_system
PGHOST=prod-db-cluster
PGPORT=5432
PGUSER=banking_prod_user
PGPASSWORD=${DB_PASSWORD_SECRET}
PGDATABASE=banking_system

# Redis Cluster Configuration
REDIS_CLUSTER_NODES=redis-node-1:6379,redis-node-2:6379,redis-node-3:6379
REDIS_PASSWORD=${REDIS_PASSWORD_SECRET}

# Kafka Cluster Configuration
KAFKA_BOOTSTRAP_SERVERS=kafka-1:9092,kafka-2:9092,kafka-3:9092
KAFKA_SECURITY_PROTOCOL=SASL_SSL
KAFKA_SASL_MECHANISM=SCRAM-SHA-256

# Production Service Configuration
API_GATEWAY_PORT=8080
CUSTOMER_SERVICE_REPLICAS=3
LOAN_SERVICE_REPLICAS=3
PAYMENT_SERVICE_REPLICAS=4

# Enhanced Security
JWT_SECRET_KEY=${JWT_SECRET_FROM_VAULT}
ENCRYPTION_KEY=${ENCRYPTION_KEY_FROM_VAULT}
OWASP_SECURITY_ENABLED=true
RATE_LIMITING_ENABLED=true

# Production Monitoring
PROMETHEUS_ENABLED=true
JAEGER_ENABLED=true
LOG_LEVEL=WARN
METRICS_EXPORT_INTERVAL=30s
```

---

## 3. Docker Containerization

### 3.1 Multi-Stage Dockerfile

**`Dockerfile`**:
```dockerfile
# Multi-stage build for production optimization
FROM gradle:8.5-jdk21 AS builder

WORKDIR /app
COPY gradle/ gradle/
COPY gradlew .
COPY build.gradle .
COPY settings.gradle .
COPY gradle.properties .

# Copy source code
COPY src/ src/

# Build application with optimization
RUN ./gradlew clean build -x test --parallel --build-cache

# Production runtime image
FROM openjdk:21-jdk-slim AS runtime

# Security: Create non-root user
RUN groupadd -r banking && useradd -r -g banking banking

# Install runtime dependencies
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    curl \
    jq \
    postgresql-client \
    redis-tools && \
    rm -rf /var/lib/apt/lists/*

# Application directory
WORKDIR /app

# Copy built JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Health check script
COPY scripts/health-check.sh /app/health-check.sh
RUN chmod +x /app/health-check.sh

# Switch to non-root user
USER banking

# JVM optimization for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -XX:+ExitOnOutOfMemoryError \
    -Djava.security.egd=file:/dev/./urandom"

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD /app/health-check.sh

# Start application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### 3.2 Service-Specific Dockerfiles

**`docker/customer-service/Dockerfile`**:
```dockerfile
FROM loan-management-base:latest
ENV SERVICE_NAME=customer-service
ENV SERVER_PORT=8081
ENV SPRING_PROFILES_ACTIVE=production,customer-service
EXPOSE 8081
```

**`docker/loan-service/Dockerfile`**:
```dockerfile
FROM loan-management-base:latest
ENV SERVICE_NAME=loan-service
ENV SERVER_PORT=8082
ENV SPRING_PROFILES_ACTIVE=production,loan-service
EXPOSE 8082
```

### 3.3 Docker Compose Configuration

**`docker-compose.yml`**:
```yaml
version: '3.8'

services:
  # Database Services
  postgres:
    image: postgres:16.9
    environment:
      POSTGRES_DB: banking_system
      POSTGRES_USER: banking_user
      POSTGRES_PASSWORD: secure_password
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./scripts/init-databases.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U banking_user -d banking_system"]
      interval: 30s
      timeout: 10s
      retries: 3

  redis:
    image: redis:7.2-alpine
    command: redis-server --requirepass redis_password
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "--raw", "incr", "ping"]
      interval: 30s
      timeout: 10s
      retries: 3

  kafka:
    image: confluentinc/cp-kafka:7.4.0
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    ports:
      - "9092:9092"
    depends_on:
      - zookeeper

  zookeeper:
    image: confluentinc/cp-zookeeper:7.4.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  # Microservices
  api-gateway:
    build:
      context: .
      dockerfile: docker/api-gateway/Dockerfile
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=production,api-gateway
      - DATABASE_URL=jdbc:postgresql://postgres:5432/banking_system
      - REDIS_HOST=redis
      - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
    depends_on:
      - postgres
      - redis
      - kafka
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  customer-service:
    build:
      context: .
      dockerfile: docker/customer-service/Dockerfile
    ports:
      - "8081:8081"
    environment:
      - SPRING_PROFILES_ACTIVE=production,customer-service
      - DATABASE_URL=jdbc:postgresql://postgres:5432/banking_system
    depends_on:
      - postgres
    deploy:
      replicas: 2

  loan-service:
    build:
      context: .
      dockerfile: docker/loan-service/Dockerfile
    ports:
      - "8082:8082"
    environment:
      - SPRING_PROFILES_ACTIVE=production,loan-service
      - DATABASE_URL=jdbc:postgresql://postgres:5432/banking_system
    depends_on:
      - postgres
    deploy:
      replicas: 2

  payment-service:
    build:
      context: .
      dockerfile: docker/payment-service/Dockerfile
    ports:
      - "8083:8083"
    environment:
      - SPRING_PROFILES_ACTIVE=production,payment-service
      - DATABASE_URL=jdbc:postgresql://postgres:5432/banking_system
    depends_on:
      - postgres
    deploy:
      replicas: 3

  # Monitoring Stack
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana_data:/var/lib/grafana
      - ./monitoring/grafana-dashboards:/etc/grafana/provisioning/dashboards

volumes:
  postgres_data:
  redis_data:
  grafana_data:

networks:
  default:
    name: banking-network
```

---

## 4. Kubernetes Deployment

### 4.1 Namespace Configuration

**`k8s/namespace.yaml`**:
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: banking-system
  labels:
    name: banking-system
    environment: production
```

### 4.2 ConfigMaps and Secrets

**`k8s/configmap.yaml`**:
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: banking-config
  namespace: banking-system
data:
  application.yml: |
    spring:
      profiles:
        active: production
      datasource:
        hikari:
          maximum-pool-size: 20
          minimum-idle: 5
      redis:
        timeout: 2000ms
        lettuce:
          pool:
            max-active: 8
            max-idle: 8
    management:
      endpoints:
        web:
          exposure:
            include: health,info,metrics,prometheus
      endpoint:
        health:
          show-details: always
    banking:
      circuit-breaker:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
      rate-limiting:
        requests-per-minute: 1000
        burst-capacity: 100
```

**`k8s/secrets.yaml`**:
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: banking-secrets
  namespace: banking-system
type: Opaque
data:
  DATABASE_PASSWORD: # base64 encoded password
  REDIS_PASSWORD: # base64 encoded password
  JWT_SECRET_KEY: # base64 encoded JWT secret
  ENCRYPTION_KEY: # base64 encoded encryption key
```

### 4.3 Persistent Volumes

**`k8s/persistent-volumes.yaml`**:
```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: postgres-pv
spec:
  capacity:
    storage: 100Gi
  accessModes:
    - ReadWriteOnce
  persistentVolumeReclaimPolicy: Retain
  storageClassName: ssd-storage
  hostPath:
    path: /data/postgres

---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgres-pvc
  namespace: banking-system
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 100Gi
  storageClassName: ssd-storage
```

### 4.4 Database Deployment

**`k8s/postgres-deployment.yaml`**:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres
  namespace: banking-system
spec:
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
      - name: postgres
        image: postgres:16.9
        ports:
        - containerPort: 5432
        env:
        - name: POSTGRES_DB
          value: banking_system
        - name: POSTGRES_USER
          value: banking_user
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: banking-secrets
              key: DATABASE_PASSWORD
        volumeMounts:
        - name: postgres-storage
          mountPath: /var/lib/postgresql/data
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
        livenessProbe:
          exec:
            command:
            - pg_isready
            - -U
            - banking_user
            - -d
            - banking_system
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          exec:
            command:
            - pg_isready
            - -U
            - banking_user
            - -d
            - banking_system
          initialDelaySeconds: 5
          periodSeconds: 5
      volumes:
      - name: postgres-storage
        persistentVolumeClaim:
          claimName: postgres-pvc

---
apiVersion: v1
kind: Service
metadata:
  name: postgres-service
  namespace: banking-system
spec:
  selector:
    app: postgres
  ports:
  - port: 5432
    targetPort: 5432
  type: ClusterIP
```

### 4.5 Microservice Deployments

**`k8s/api-gateway-deployment.yaml`**:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-gateway
  namespace: banking-system
spec:
  replicas: 3
  selector:
    matchLabels:
      app: api-gateway
  template:
    metadata:
      labels:
        app: api-gateway
    spec:
      containers:
      - name: api-gateway
        image: banking/api-gateway:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production,api-gateway"
        - name: DATABASE_URL
          value: "jdbc:postgresql://postgres-service:5432/banking_system"
        - name: DATABASE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: banking-secrets
              key: DATABASE_PASSWORD
        - name: REDIS_HOST
          value: "redis-service"
        - name: REDIS_PASSWORD
          valueFrom:
            secretKeyRef:
              name: banking-secrets
              key: REDIS_PASSWORD
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        volumeMounts:
        - name: config-volume
          mountPath: /app/config
      volumes:
      - name: config-volume
        configMap:
          name: banking-config

---
apiVersion: v1
kind: Service
metadata:
  name: api-gateway-service
  namespace: banking-system
spec:
  selector:
    app: api-gateway
  ports:
  - port: 8080
    targetPort: 8080
  type: LoadBalancer
```

### 4.6 Horizontal Pod Autoscaler

**`k8s/hpa.yaml`**:
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: api-gateway-hpa
  namespace: banking-system
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: api-gateway
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80

---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: customer-service-hpa
  namespace: banking-system
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: customer-service
  minReplicas: 2
  maxReplicas: 8
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

---

## 5. CI/CD Pipeline

### 5.1 GitHub Actions Workflow

**`.github/workflows/deploy.yml`**:
```yaml
name: Deploy Banking System

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: banking-system

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
    
    - name: Cache Gradle packages
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
    
    - name: Run tests
      run: ./gradlew test --parallel
    
    - name: Run security scan
      run: ./gradlew dependencyCheckAnalyze
    
    - name: Generate test report
      uses: dorny/test-reporter@v1
      if: success() || failure()
      with:
        name: Test Results
        path: build/test-results/test/*.xml
        reporter: java-junit

  build:
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Log in to Container Registry
      uses: docker/login-action@v2
      with:
        registry: ${{ env.REGISTRY }}
        username: ${{ github.actor }}
        password: ${{ secrets.GITHUB_TOKEN }}
    
    - name: Build and push Docker images
      run: |
        docker build -t ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}/api-gateway:${{ github.sha }} .
        docker build -t ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}/customer-service:${{ github.sha }} -f docker/customer-service/Dockerfile .
        docker build -t ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}/loan-service:${{ github.sha }} -f docker/loan-service/Dockerfile .
        docker build -t ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}/payment-service:${{ github.sha }} -f docker/payment-service/Dockerfile .
        
        docker push ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}/api-gateway:${{ github.sha }}
        docker push ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}/customer-service:${{ github.sha }}
        docker push ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}/loan-service:${{ github.sha }}
        docker push ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}/payment-service:${{ github.sha }}

  deploy:
    needs: build
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Configure kubectl
      uses: azure/setup-kubectl@v3
      with:
        version: 'v1.28.0'
    
    - name: Deploy to Kubernetes
      run: |
        echo "${{ secrets.KUBECONFIG }}" | base64 -d > kubeconfig
        export KUBECONFIG=kubeconfig
        
        # Update image tags in deployment files
        sed -i "s/latest/${{ github.sha }}/g" k8s/*-deployment.yaml
        
        # Apply Kubernetes manifests
        kubectl apply -f k8s/namespace.yaml
        kubectl apply -f k8s/configmap.yaml
        kubectl apply -f k8s/secrets.yaml
        kubectl apply -f k8s/persistent-volumes.yaml
        kubectl apply -f k8s/postgres-deployment.yaml
        kubectl apply -f k8s/redis-deployment.yaml
        kubectl apply -f k8s/api-gateway-deployment.yaml
        kubectl apply -f k8s/customer-service-deployment.yaml
        kubectl apply -f k8s/loan-service-deployment.yaml
        kubectl apply -f k8s/payment-service-deployment.yaml
        kubectl apply -f k8s/hpa.yaml
        
        # Wait for deployment to complete
        kubectl rollout status deployment/api-gateway -n banking-system
        kubectl rollout status deployment/customer-service -n banking-system
        kubectl rollout status deployment/loan-service -n banking-system
        kubectl rollout status deployment/payment-service -n banking-system
```

---

## 6. Monitoring and Observability

### 6.1 Prometheus Configuration

**`monitoring/prometheus.yml`**:
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "banking-rules.yml"

scrape_configs:
  - job_name: 'api-gateway'
    static_configs:
      - targets: ['api-gateway-service:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 30s

  - job_name: 'customer-service'
    static_configs:
      - targets: ['customer-service:8081']
    metrics_path: '/actuator/prometheus'

  - job_name: 'loan-service'
    static_configs:
      - targets: ['loan-service:8082']
    metrics_path: '/actuator/prometheus'

  - job_name: 'payment-service'
    static_configs:
      - targets: ['payment-service:8083']
    metrics_path: '/actuator/prometheus'

  - job_name: 'postgres'
    static_configs:
      - targets: ['postgres-exporter:9187']

  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']

alerting:
  alertmanagers:
    - static_configs:
        - targets: ['alertmanager:9093']
```

### 6.2 Grafana Dashboard

**`monitoring/grafana-dashboard.json`** (excerpt):
```json
{
  "dashboard": {
    "title": "Banking System - Microservices Dashboard",
    "panels": [
      {
        "title": "API Gateway - Request Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_requests_total{job=\"api-gateway\"}[5m])",
            "legendFormat": "{{method}} {{status}}"
          }
        ]
      },
      {
        "title": "Circuit Breaker States",
        "type": "stat",
        "targets": [
          {
            "expr": "circuit_breaker_state",
            "legendFormat": "{{name}}"
          }
        ]
      },
      {
        "title": "Database Connection Pool",
        "type": "graph",
        "targets": [
          {
            "expr": "hikaricp_connections_active",
            "legendFormat": "Active Connections"
          }
        ]
      }
    ]
  }
}
```

---

## 7. Security Hardening

### 7.1 Network Policies

**`k8s/network-policy.yaml`**:
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: banking-network-policy
  namespace: banking-system
spec:
  podSelector: {}
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
  egress:
  - to:
    - podSelector:
        matchLabels:
          app: postgres
    ports:
    - protocol: TCP
      port: 5432
  - to:
    - podSelector:
        matchLabels:
          app: redis
    ports:
    - protocol: TCP
      port: 6379
```

### 7.2 Pod Security Standards

**`k8s/pod-security-policy.yaml`**:
```yaml
apiVersion: policy/v1beta1
kind: PodSecurityPolicy
metadata:
  name: banking-psp
spec:
  privileged: false
  allowPrivilegeEscalation: false
  requiredDropCapabilities:
    - ALL
  volumes:
    - 'configMap'
    - 'emptyDir'
    - 'projected'
    - 'secret'
    - 'downwardAPI'
    - 'persistentVolumeClaim'
  hostNetwork: false
  hostIPC: false
  hostPID: false
  runAsUser:
    rule: 'MustRunAsNonRoot'
  seLinux:
    rule: 'RunAsAny'
  fsGroup:
    rule: 'RunAsAny'
```

---

## 8. Backup and Disaster Recovery

### 8.1 Database Backup Strategy

**`scripts/backup-databases.sh`**:
```bash
#!/bin/bash

BACKUP_DIR="/backups/$(date +%Y-%m-%d)"
mkdir -p $BACKUP_DIR

# Backup each database schema
pg_dump -h $PGHOST -p $PGPORT -U $PGUSER -n customer_db \
    --format=custom --file=$BACKUP_DIR/customer_db_backup.dump

pg_dump -h $PGHOST -p $PGPORT -U $PGUSER -n loan_db \
    --format=custom --file=$BACKUP_DIR/loan_db_backup.dump

pg_dump -h $PGHOST -p $PGPORT -U $PGUSER -n payment_db \
    --format=custom --file=$BACKUP_DIR/payment_db_backup.dump

pg_dump -h $PGHOST -p $PGPORT -U $PGUSER -n banking_gateway \
    --format=custom --file=$BACKUP_DIR/gateway_backup.dump

# Upload to cloud storage
aws s3 sync $BACKUP_DIR s3://banking-backups/$(date +%Y-%m-%d)/

# Cleanup local backups older than 7 days
find /backups -type d -mtime +7 -exec rm -rf {} \;
```

### 8.2 Disaster Recovery Plan

**Recovery Time Objective (RTO)**: 4 hours  
**Recovery Point Objective (RPO)**: 1 hour  

**Recovery Procedures**:
1. **Infrastructure Recovery**: Restore Kubernetes cluster from Infrastructure as Code
2. **Database Recovery**: Restore from point-in-time backup
3. **Application Recovery**: Deploy latest verified container images
4. **Data Validation**: Run comprehensive data integrity checks
5. **Service Validation**: Execute health checks and integration tests

---

## 9. Performance Optimization

### 9.1 JVM Tuning

**Production JVM Settings**:
```bash
JAVA_OPTS="-server \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UseStringDeduplication \
    -XX:+UseCompressedOops \
    -XX:+UseCompressedClassPointers \
    -XX:+ExitOnOutOfMemoryError \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/heapdumps/ \
    -XX:+PrintGCDetails \
    -XX:+PrintGCTimeStamps \
    -Xloggc:/app/logs/gc.log \
    -XX:+UseGCLogFileRotation \
    -XX:NumberOfGCLogFiles=5 \
    -XX:GCLogFileSize=10M"
```

### 9.2 Database Optimization

**PostgreSQL Configuration**:
```sql
-- postgresql.conf optimizations for production
shared_buffers = 8GB
effective_cache_size = 24GB
maintenance_work_mem = 2GB
checkpoint_completion_target = 0.9
wal_buffers = 64MB
default_statistics_target = 100
random_page_cost = 1.1
effective_io_concurrency = 200
work_mem = 256MB
min_wal_size = 1GB
max_wal_size = 4GB
max_worker_processes = 16
max_parallel_workers_per_gather = 4
max_parallel_workers = 16
```

This deployment guide provides comprehensive instructions for production deployment with enterprise-grade security, monitoring, and scalability configurations.