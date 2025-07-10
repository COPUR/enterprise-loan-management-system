# Comprehensive Deployment Guide - Enterprise Loan Management System

## 📋 Overview

This guide provides comprehensive deployment instructions for the Enterprise Loan Management System with FAPI 2.0 + DPoP security. The system is designed for enterprise-grade banking deployment with Kubernetes, Istio service mesh, and complete observability.

**Architecture**: Microservices with Hexagonal Architecture  
**Container Platform**: Kubernetes 1.28+  
**Service Mesh**: Istio 1.20+  
**Security**: FAPI 2.0 + DPoP (Banking Grade)  
**Database**: PostgreSQL 15+ (Primary), Redis 7+ (Cache)  
**Messaging**: Apache Kafka 3.6+  

---

## 🏗️ Deployment Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     Production Architecture                      │
├─────────────────────────────────────────────────────────────────┤
│  Load Balancer (AWS ALB/NLB)                                   │
│         ↓                                                       │
│  Istio Ingress Gateway (TLS Termination)                       │
│         ↓                                                       │
│  ┌─────────────────────────────────────────────────┐          │
│  │          Kubernetes Cluster (EKS/GKE/AKS)       │          │
│  │                                                  │          │
│  │  ┌───────────────┐  ┌───────────────┐         │          │
│  │  │ Banking Pods  │  │ Security Pods │         │          │
│  │  │ - Loan API    │  │ - OAuth2/PAR  │         │          │
│  │  │ - Payment API │  │ - DPoP Valid. │         │          │
│  │  │ - AI Service  │  │ - Audit Svc   │         │          │
│  │  └───────────────┘  └───────────────┘         │          │
│  │                                                  │          │
│  │  ┌───────────────┐  ┌───────────────┐         │          │
│  │  │  Data Layer   │  │  Monitoring   │         │          │
│  │  │ - PostgreSQL  │  │ - Prometheus  │         │          │
│  │  │ - Redis       │  │ - Grafana     │         │          │
│  │  │ - Kafka       │  │ - ELK Stack   │         │          │
│  │  └───────────────┘  └───────────────┘         │          │
│  └─────────────────────────────────────────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🚀 Quick Start Deployment

### Prerequisites

1. **Infrastructure Requirements**
   - Kubernetes cluster (1.28+) with at least 3 worker nodes
   - Istio service mesh (1.20+) installed
   - kubectl CLI configured
   - Helm 3.0+ installed
   - Docker registry access

2. **External Services**
   - PostgreSQL 15+ (RDS recommended for AWS)
   - Redis 7+ cluster (ElastiCache recommended)
   - Kafka cluster (MSK recommended for AWS)
   - Keycloak instance with FAPI 2.0 realm

### Step 1: Clone Repository

```bash
git clone https://github.com/yourorg/enterprise-loan-management-system.git
cd enterprise-loan-management-system
```

### Step 2: Configure Secrets

```bash
# Create namespace
kubectl create namespace banking-prod

# Create database secrets
kubectl create secret generic postgres-credentials \
  --from-literal=username=banking_user \
  --from-literal=password='<secure-password>' \
  --from-literal=database=banking_fapi2 \
  -n banking-prod

# Create Redis secrets
kubectl create secret generic redis-credentials \
  --from-literal=password='<redis-password>' \
  -n banking-prod

# Create OAuth2 client secrets
kubectl create secret generic oauth2-credentials \
  --from-literal=client-id=enterprise-banking-app \
  --from-literal=private-key='<private-key-pem>' \
  -n banking-prod
```

### Step 3: Deploy with Helm

```bash
# Add Helm repository
helm repo add banking-system ./k8s/helm-charts/enterprise-loan-system

# Install with production values
helm install enterprise-banking banking-system \
  --namespace banking-prod \
  --values k8s/helm-charts/enterprise-loan-system/values-production.yaml \
  --set image.tag=v1.0.0 \
  --set ingress.host=api.banking.example.com \
  --set postgresql.host=postgres.example.com \
  --set redis.host=redis.example.com \
  --set kafka.bootstrapServers=kafka.example.com:9092
```

---

## 🔧 Detailed Deployment Steps

### 1. Database Setup

#### PostgreSQL Configuration

```sql
-- Create database and user
CREATE DATABASE banking_fapi2;
CREATE USER banking_user WITH ENCRYPTED PASSWORD 'secure_password';
GRANT ALL PRIVILEGES ON DATABASE banking_fapi2 TO banking_user;

-- Enable required extensions
\c banking_fapi2
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Performance tuning for banking workload
ALTER SYSTEM SET max_connections = 200;
ALTER SYSTEM SET shared_buffers = '4GB';
ALTER SYSTEM SET effective_cache_size = '12GB';
ALTER SYSTEM SET work_mem = '16MB';
ALTER SYSTEM SET maintenance_work_mem = '512MB';
ALTER SYSTEM SET checkpoint_completion_target = 0.9;
ALTER SYSTEM SET wal_buffers = '16MB';
ALTER SYSTEM SET default_statistics_target = 100;
ALTER SYSTEM SET random_page_cost = 1.1;
```

#### Redis Configuration

```bash
# Redis configuration for DPoP JTI storage
cat <<EOF > redis.conf
# Security
requirepass your_redis_password
protected-mode yes

# Persistence
save 900 1
save 300 10
save 60 10000
appendonly yes
appendfsync everysec

# Memory management
maxmemory 4gb
maxmemory-policy allkeys-lru

# Performance
tcp-keepalive 60
timeout 300
tcp-backlog 511
EOF
```

### 2. Kubernetes Deployment

#### Apply Base Configurations

```bash
# Apply namespace and RBAC
kubectl apply -f k8s/manifests/namespace.yaml
kubectl apply -f k8s/manifests/rbac.yaml

# Apply ConfigMaps
kubectl apply -f k8s/manifests/configmap.yaml

# Apply Secrets (after creating them)
kubectl apply -f k8s/manifests/secrets.yaml
```

#### Deploy Application Components

```bash
# Deploy core banking services
kubectl apply -f k8s/manifests/loan-service-deployment.yaml
kubectl apply -f k8s/manifests/payment-service-deployment.yaml
kubectl apply -f k8s/manifests/customer-service-deployment.yaml

# Deploy security services
kubectl apply -f k8s/manifests/oauth2-service-deployment.yaml
kubectl apply -f k8s/manifests/audit-service-deployment.yaml

# Deploy AI services
kubectl apply -f k8s/manifests/ai-assistant-deployment.yaml

# Apply services
kubectl apply -f k8s/manifests/services/
```

### 3. Istio Service Mesh Configuration

#### Apply Istio Policies

```bash
# Gateway configuration
kubectl apply -f k8s/istio/banking-gateway.yaml

# Authentication policies
kubectl apply -f k8s/istio/banking-authentication.yaml

# Security policies
kubectl apply -f k8s/istio/security-policies.yaml

# Observability configuration
kubectl apply -f k8s/istio/banking-telemetry.yaml
```

#### Example Istio Gateway Configuration

```yaml
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: banking-gateway
  namespace: banking-prod
spec:
  selector:
    istio: ingressgateway
  servers:
  - port:
      number: 443
      name: https
      protocol: HTTPS
    tls:
      mode: SIMPLE
      credentialName: banking-tls-cert
    hosts:
    - api.banking.example.com
---
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: banking-routes
  namespace: banking-prod
spec:
  hosts:
  - api.banking.example.com
  gateways:
  - banking-gateway
  http:
  - match:
    - uri:
        prefix: /api/v1/loans
    route:
    - destination:
        host: secure-loan-service
        port:
          number: 8080
      headers:
        request:
          add:
            x-forwarded-proto: https
```

### 4. Monitoring Stack Deployment

#### Deploy Prometheus

```bash
# Add Prometheus Helm repo
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts

# Install Prometheus with custom values
helm install prometheus prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --create-namespace \
  --values monitoring/prometheus/values-banking.yaml
```

#### Deploy Grafana Dashboards

```bash
# Import banking-specific dashboards
kubectl apply -f monitoring/grafana/dashboards/

# Key dashboards included:
# - FAPI 2.0 Security Metrics
# - DPoP Validation Performance
# - Banking Transaction Monitoring
# - Regulatory Compliance Dashboard
```

#### Deploy ELK Stack

```bash
# Deploy Elasticsearch
kubectl apply -f monitoring/elk-stack/elasticsearch/

# Deploy Logstash with banking pipelines
kubectl apply -f monitoring/elk-stack/logstash/

# Deploy Kibana
kubectl apply -f monitoring/elk-stack/kibana/

# Deploy Filebeat for log collection
kubectl apply -f monitoring/elk-stack/filebeat/
```

---

## ⚙️ Configuration Management

### 1. Environment-Specific Configurations

#### Production Configuration

```yaml
# k8s/manifests/configmap-prod.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: banking-config-prod
  namespace: banking-prod
data:
  application.yml: |
    spring:
      profiles:
        active: fapi2-dpop,production
      
    # FAPI 2.0 Configuration
    fapi:
      version: "2.0"
      enabled: true
      security-profile: advanced
      
    # DPoP Configuration
    dpop:
      enabled: true
      proof:
        expiration-time: 60
        clock-skew-tolerance: 30
      jti:
        cache-size: 10000
        cleanup-interval: 300
        
    # Banking Configuration
    banking:
      compliance:
        strict-validation: true
        audit-enabled: true
        fapi-validation: true
        dpop-required: true
        par-required: true
```

### 2. Scaling Configuration

#### Horizontal Pod Autoscaling

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: loan-service-hpa
  namespace: banking-prod
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: secure-loan-service
  minReplicas: 3
  maxReplicas: 20
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
  - type: Pods
    pods:
      metric:
        name: http_requests_per_second
      target:
        type: AverageValue
        averageValue: "100"
```

#### Vertical Pod Autoscaling

```yaml
apiVersion: autoscaling.k8s.io/v1
kind: VerticalPodAutoscaler
metadata:
  name: loan-service-vpa
  namespace: banking-prod
spec:
  targetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: secure-loan-service
  updatePolicy:
    updateMode: "Auto"
  resourcePolicy:
    containerPolicies:
    - containerName: loan-service
      minAllowed:
        cpu: 500m
        memory: 1Gi
      maxAllowed:
        cpu: 4
        memory: 8Gi
```

---

## 🔒 Security Hardening

### 1. Network Policies

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: banking-network-policy
  namespace: banking-prod
spec:
  podSelector:
    matchLabels:
      app: banking
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: istio-system
    - podSelector:
        matchLabels:
          app: banking
    ports:
    - protocol: TCP
      port: 8080
  egress:
  - to:
    - podSelector:
        matchLabels:
          app: postgres
    - podSelector:
        matchLabels:
          app: redis
    - podSelector:
        matchLabels:
          app: kafka
  - to:
    - namespaceSelector: {}
      podSelector:
        matchLabels:
          k8s-app: kube-dns
    ports:
    - protocol: UDP
      port: 53
```

### 2. Pod Security Standards

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: secure-loan-service
spec:
  securityContext:
    runAsNonRoot: true
    runAsUser: 1000
    fsGroup: 2000
    seccompProfile:
      type: RuntimeDefault
  containers:
  - name: loan-service
    image: banking/loan-service:v1.0.0
    securityContext:
      allowPrivilegeEscalation: false
      readOnlyRootFilesystem: true
      capabilities:
        drop:
        - ALL
    volumeMounts:
    - name: tmp
      mountPath: /tmp
    - name: app-logs
      mountPath: /app/logs
  volumes:
  - name: tmp
    emptyDir: {}
  - name: app-logs
    emptyDir: {}
```

---

## 📊 Production Readiness Checklist

### Pre-Production Validation

- [ ] **Security Validation**
  - [ ] FAPI 2.0 conformance testing completed
  - [ ] DPoP implementation validated
  - [ ] Penetration testing completed
  - [ ] Security scanning (SAST/DAST) passed

- [ ] **Performance Testing**
  - [ ] Load testing completed (10,000 TPS achieved)
  - [ ] Stress testing passed
  - [ ] Database performance optimized
  - [ ] Cache hit rates > 90%

- [ ] **High Availability**
  - [ ] Multi-AZ deployment configured
  - [ ] Database replication active
  - [ ] Redis cluster mode enabled
  - [ ] Kafka replication factor = 3

- [ ] **Disaster Recovery**
  - [ ] Backup procedures tested
  - [ ] Recovery time objective (RTO) < 1 hour
  - [ ] Recovery point objective (RPO) < 5 minutes
  - [ ] Failover procedures documented

- [ ] **Monitoring & Alerting**
  - [ ] All dashboards configured
  - [ ] Alert rules active
  - [ ] On-call rotation established
  - [ ] Runbooks documented

---

## 🚨 Troubleshooting Guide

### Common Issues and Solutions

#### 1. DPoP Validation Failures

```bash
# Check Redis connectivity
kubectl exec -it deployment/secure-loan-service -n banking-prod -- redis-cli -h redis ping

# Check DPoP configuration
kubectl logs deployment/secure-loan-service -n banking-prod | grep -i dpop

# Verify time synchronization
kubectl exec -it deployment/secure-loan-service -n banking-prod -- date
```

#### 2. Database Connection Issues

```bash
# Test database connectivity
kubectl run -it --rm debug --image=postgres:15 --restart=Never -- \
  psql -h postgres.example.com -U banking_user -d banking_fapi2 -c "SELECT 1"

# Check connection pool metrics
kubectl exec -it deployment/secure-loan-service -n banking-prod -- \
  curl localhost:8080/actuator/metrics/hikaricp.connections.active
```

#### 3. Istio Service Mesh Issues

```bash
# Check Istio injection
kubectl get pods -n banking-prod -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.spec.containers[*].name}{"\n"}{end}'

# Verify Istio configuration
istioctl analyze -n banking-prod

# Check Envoy proxy logs
kubectl logs deployment/secure-loan-service -n banking-prod -c istio-proxy
```

---

## 🔄 Maintenance Operations

### Rolling Updates

```bash
# Update deployment with zero downtime
kubectl set image deployment/secure-loan-service \
  loan-service=banking/loan-service:v1.1.0 \
  -n banking-prod

# Monitor rollout status
kubectl rollout status deployment/secure-loan-service -n banking-prod

# Rollback if needed
kubectl rollout undo deployment/secure-loan-service -n banking-prod
```

### Database Migrations

```bash
# Run Liquibase migrations
kubectl run -it --rm liquibase --image=liquibase/liquibase:4.25 --restart=Never -- \
  --url=jdbc:postgresql://postgres.example.com:5432/banking_fapi2 \
  --username=banking_user \
  --password=$DB_PASSWORD \
  --changeLogFile=db/changelog/db.changelog-master.xml \
  update
```

### Backup Procedures

```bash
# Database backup
kubectl apply -f k8s/jobs/postgres-backup-job.yaml

# Redis backup
kubectl exec -it redis-master-0 -n banking-prod -- redis-cli BGSAVE

# Verify backups
kubectl logs job/postgres-backup -n banking-prod
```

---

## 📈 Performance Tuning

### JVM Optimization

```yaml
env:
- name: JAVA_OPTS
  value: >-
    -Xms2g
    -Xmx2g
    -XX:+UseG1GC
    -XX:MaxGCPauseMillis=200
    -XX:+UseStringDeduplication
    -XX:+AlwaysPreTouch
    -XX:+DisableExplicitGC
    -XX:+UseContainerSupport
    -XX:MaxRAMPercentage=75.0
    -Djava.security.egd=file:/dev/./urandom
```

### Connection Pool Tuning

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
```

---

## 📚 Additional Resources

### Documentation
- [Kubernetes Best Practices](https://kubernetes.io/docs/concepts/cluster-administration/manage-deployment/)
- [Istio Production Deployment](https://istio.io/latest/docs/ops/deployment/)
- [PostgreSQL Tuning Guide](https://wiki.postgresql.org/wiki/Tuning_Your_PostgreSQL_Server)

### Monitoring Dashboards
- Grafana: `https://grafana.banking.example.com`
- Kibana: `https://kibana.banking.example.com`
- Jaeger: `https://jaeger.banking.example.com`
- Prometheus: `https://prometheus.banking.example.com`

---

**Document Version**: 1.0  
**Last Updated**: January 2025  
**Deployment Level**: **PRODUCTION GRADE**  
**Security Profile**: **BANKING COMPLIANT**