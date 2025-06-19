# Deployment & Operations Guide
## Enterprise Banking System - Production Deployment & Operations

### Table of Contents
1. [Deployment Overview](#deployment-overview)
2. [Environment Setup](#environment-setup)
3. [Production Deployment](#production-deployment)
4. [Operations Procedures](#operations-procedures)
5. [Monitoring & Alerting](#monitoring--alerting)
6. [Backup & Recovery](#backup--recovery)
7. [Security Operations](#security-operations)
8. [Troubleshooting](#troubleshooting)

---

## Deployment Overview

The Enterprise Banking System follows a GitOps-based deployment model with Infrastructure as Code (IaC) principles. The deployment architecture supports multi-environment promotion with automated testing and rollback capabilities.

### Deployment Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Deployment Pipeline                         │
├─────────────────────────────────────────────────────────────────┤
│  Developer → Git Push → CI/CD → Container Registry → Kubernetes │
│      ↓           ↓         ↓            ↓              ↓        │
│   Local     GitHub     Docker       Harbor          ArgoCD      │
│   Testing   Actions    Build        Registry       Deployment   │
└─────────────────────────────────────────────────────────────────┘
```

### Deployment Environments

| Environment | Purpose | Infrastructure | Deployment Method |
|-------------|---------|----------------|-------------------|
| Development | Active development | Local/Docker Compose | Manual/Automated |
| Testing | Integration testing | Kubernetes cluster | GitOps (ArgoCD) |
| Staging | Pre-production validation | Production-like cluster | GitOps (ArgoCD) |
| Production | Live banking operations | High-availability cluster | GitOps (ArgoCD) |

---

## Environment Setup

### Prerequisites

#### Required Tools
```bash
# Kubernetes tooling
kubectl >= 1.28
helm >= 3.13
argocd >= 2.8

# Infrastructure tooling
terraform >= 1.6
ansible >= 2.15
docker >= 24.0

# Development tools
git >= 2.40
jq >= 1.6
yq >= 4.35
```

#### Infrastructure Requirements

##### Production Environment
```yaml
Kubernetes Cluster:
  Version: 1.28.x
  Nodes: 
    - Control Plane: 3 nodes (4 vCPU, 8GB RAM, 100GB SSD)
    - Worker Nodes: 6 nodes (8 vCPU, 32GB RAM, 500GB SSD)
  
Network:
  CNI: Calico
  Load Balancer: HAProxy/NGINX
  Ingress: Istio Gateway
  
Storage:
  Persistent Volumes: 10TB total
  Storage Class: fast-ssd
  Backup Storage: Object storage (S3-compatible)
```

### Configuration Management

#### Terraform Infrastructure
```hcl
# infrastructure/main.tf
terraform {
  required_version = ">= 1.6"
  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.23"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.11"
    }
  }
}

module "banking_cluster" {
  source = "./modules/kubernetes-cluster"
  
  cluster_name     = "banking-production"
  node_count       = 6
  node_size        = "Standard_D8s_v3"
  kubernetes_version = "1.28.3"
  
  tags = {
    Environment = "production"
    Application = "banking-system"
    Compliance  = "pci-dss,sox"
  }
}

module "monitoring_stack" {
  source = "./modules/monitoring"
  
  cluster_name = module.banking_cluster.cluster_name
  namespace    = "banking-monitoring"
  
  prometheus_retention = "30d"
  grafana_admin_password = var.grafana_admin_password
}
```

#### Helm Chart Values
```yaml
# k8s/helm/banking-system/values-production.yaml
replicaCount: 3

image:
  repository: harbor.banking.local/banking/app
  tag: "1.0.0"
  pullPolicy: IfNotPresent

service:
  type: ClusterIP
  port: 8080

ingress:
  enabled: true
  className: istio
  annotations:
    kubernetes.io/ingress.class: istio
    cert-manager.io/cluster-issuer: letsencrypt-prod
  hosts:
    - host: api.banking.enterprise.com
      paths:
        - path: /
          pathType: Prefix
  tls:
    - secretName: banking-api-tls
      hosts:
        - api.banking.enterprise.com

resources:
  limits:
    cpu: 2000m
    memory: 4Gi
  requests:
    cpu: 1000m
    memory: 2Gi

autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 20
  targetCPUUtilizationPercentage: 70
  targetMemoryUtilizationPercentage: 80

securityContext:
  runAsNonRoot: true
  runAsUser: 1000
  runAsGroup: 1000
  allowPrivilegeEscalation: false
  readOnlyRootFilesystem: false
  capabilities:
    drop: ["ALL"]
    add: ["NET_BIND_SERVICE"]

env:
  - name: SPRING_PROFILES_ACTIVE
    value: "production"
  - name: KEYCLOAK_URL
    valueFrom:
      configMapKeyRef:
        name: banking-config
        key: keycloak.url
  - name: DATABASE_PASSWORD
    valueFrom:
      secretKeyRef:
        name: banking-secrets
        key: database.password
```

---

## Production Deployment

### Pre-Deployment Checklist

#### Infrastructure Validation
```bash
#!/bin/bash
# scripts/pre-deployment-check.sh

echo "=== Pre-Deployment Infrastructure Check ==="

# Check Kubernetes cluster health
kubectl cluster-info
kubectl get nodes -o wide

# Verify namespace existence
kubectl get namespace banking-system || kubectl create namespace banking-system

# Check persistent volumes
kubectl get pv | grep Available

# Validate secrets and configmaps
kubectl get secrets -n banking-system
kubectl get configmaps -n banking-system

# Check Istio service mesh
kubectl get pods -n istio-system

# Verify monitoring stack
kubectl get pods -n banking-monitoring

echo "=== Infrastructure check completed ==="
```

#### Security Validation
```bash
#!/bin/bash
# scripts/security-validation.sh

echo "=== Security Validation ==="

# Check RBAC configuration
kubectl auth can-i --list --as=system:serviceaccount:banking-system:banking-app

# Validate network policies
kubectl get networkpolicies -n banking-system

# Check pod security policies
kubectl get podsecuritypolicies

# Verify TLS certificates
kubectl get certificates -n banking-system

# Check secrets encryption at rest
kubectl get secrets -o yaml | grep -i encryption

echo "=== Security validation completed ==="
```

### Deployment Process

#### Step 1: Database Migration
```bash
#!/bin/bash
# Database migration and setup

# Create database migration job
kubectl create job banking-db-migration \
  --from=cronjob/banking-db-backup \
  --dry-run=client -o yaml | \
  sed 's/cronjob\/banking-db-backup/job\/banking-db-migration/' | \
  kubectl apply -f -

# Wait for migration completion
kubectl wait --for=condition=complete job/banking-db-migration --timeout=600s

# Verify migration status
kubectl logs job/banking-db-migration
```

#### Step 2: Application Deployment
```bash
#!/bin/bash
# Deploy banking application

# Update ArgoCD application
kubectl patch application banking-system-prod \
  -n argocd \
  --type='merge' \
  -p='{"spec":{"source":{"helm":{"parameters":[{"name":"image.tag","value":"1.0.0"}]}}}}'

# Monitor deployment progress
kubectl rollout status deployment/banking-app -n banking-system

# Verify pod health
kubectl get pods -n banking-system -l app=banking-app

# Check service endpoints
kubectl get endpoints -n banking-system
```

#### Step 3: OAuth2.1 Configuration
```bash
#!/bin/bash
# Configure Keycloak and OAuth2.1

# Import banking realm configuration
kubectl exec -n banking-auth deployment/keycloak -- \
  /opt/keycloak/bin/kc.sh import \
  --file /opt/keycloak/data/import/banking-realm.json

# Configure LDAP federation
kubectl apply -f k8s/keycloak/ldap-user-federation.yaml

# Verify OAuth2.1 endpoints
curl -k https://keycloak.banking.local/realms/banking-realm/.well-known/openid_configuration
```

#### Step 4: Health Checks
```bash
#!/bin/bash
# Comprehensive health validation

# Application health
curl -f https://api.banking.enterprise.com/actuator/health

# OAuth2.1 health
curl -f https://keycloak.banking.local/health

# Database connectivity
kubectl exec -n banking-data deployment/postgres -- \
  pg_isready -U banking_user -d banking_db

# Redis connectivity
kubectl exec -n banking-data deployment/redis -- \
  redis-cli ping

echo "=== Health checks completed ==="
```

### Blue-Green Deployment

#### Blue-Green Setup
```yaml
# k8s/blue-green/banking-app-green.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: banking-app-green
  namespace: banking-system
  labels:
    app: banking-app
    version: green
spec:
  replicas: 3
  selector:
    matchLabels:
      app: banking-app
      version: green
  template:
    metadata:
      labels:
        app: banking-app
        version: green
    spec:
      containers:
      - name: banking-app
        image: harbor.banking.local/banking/app:1.1.0
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 90
          periodSeconds: 30
```

#### Traffic Switching
```bash
#!/bin/bash
# Blue-green traffic switching

# Deploy green version
kubectl apply -f k8s/blue-green/banking-app-green.yaml

# Wait for green deployment to be ready
kubectl rollout status deployment/banking-app-green -n banking-system

# Run smoke tests on green version
./scripts/smoke-tests.sh green

# Switch traffic to green
kubectl patch service banking-app-service -n banking-system \
  -p '{"spec":{"selector":{"version":"green"}}}'

# Monitor traffic and errors
./scripts/monitor-deployment.sh

# Clean up blue version (after verification)
kubectl delete deployment banking-app-blue -n banking-system
```

---

## Operations Procedures

### Daily Operations

#### Morning Health Check
```bash
#!/bin/bash
# scripts/daily-health-check.sh

echo "=== Daily Banking System Health Check ==="
echo "Date: $(date)"

# Check overall system status
kubectl get nodes
kubectl get pods -n banking-system --field-selector=status.phase!=Running

# Check critical services
services=("banking-app" "keycloak" "postgres" "redis")
for service in "${services[@]}"; do
  echo "Checking $service..."
  kubectl get pods -n banking-system -l app=$service
done

# Check certificate expiry
kubectl get certificates -n banking-system -o custom-columns=NAME:.metadata.name,READY:.status.conditions[0].status,AGE:.metadata.creationTimestamp

# Database connection test
kubectl exec -n banking-data deployment/postgres -- \
  psql -U banking_user -d banking_db -c "SELECT 1"

# OAuth2.1 endpoint test
curl -f -s https://keycloak.banking.local/realms/banking-realm/.well-known/openid_configuration > /dev/null
echo "OAuth2.1 endpoint: OK"

# Generate daily report
echo "=== Daily Health Check Completed ==="
```

#### Log Analysis
```bash
#!/bin/bash
# Daily log analysis and alerting

# Check for error patterns
kubectl logs -n banking-system deployment/banking-app --since=24h | \
  grep -i error | \
  awk '{print $1, $2, $NF}' | \
  sort | uniq -c | sort -nr

# OAuth2.1 authentication failures
kubectl logs -n banking-auth deployment/keycloak --since=24h | \
  grep "AUTHENTICATION_FAILED" | \
  wc -l

# Database slow queries
kubectl exec -n banking-data deployment/postgres -- \
  psql -U banking_user -d banking_db -c \
  "SELECT query, mean_time, calls FROM pg_stat_statements ORDER BY mean_time DESC LIMIT 10"
```

### Scaling Operations

#### Manual Scaling
```bash
#!/bin/bash
# Manual application scaling

# Scale banking application
kubectl scale deployment banking-app --replicas=10 -n banking-system

# Scale database connections
kubectl patch deployment banking-app -n banking-system \
  -p '{"spec":{"template":{"spec":{"containers":[{"name":"banking-app","env":[{"name":"SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE","value":"50"}]}]}}}}'

# Monitor scaling progress
kubectl rollout status deployment/banking-app -n banking-system

# Update HPA limits if needed
kubectl patch hpa banking-app-hpa -n banking-system \
  -p '{"spec":{"maxReplicas":25}}'
```

#### Database Scaling
```bash
#!/bin/bash
# Database scaling operations

# Scale read replicas
kubectl scale deployment postgres-read-replica --replicas=3 -n banking-data

# Monitor replication lag
kubectl exec -n banking-data deployment/postgres -- \
  psql -U postgres -c "SELECT client_addr, state, sync_state FROM pg_stat_replication"

# Update connection strings for read replicas
kubectl patch configmap banking-config -n banking-system \
  --patch '{"data":{"database.read.url":"jdbc:postgresql://postgres-read-replica:5432/banking_db"}}'
```

### Configuration Updates

#### Rolling Configuration Updates
```bash
#!/bin/bash
# Update configuration without downtime

# Update ConfigMap
kubectl patch configmap banking-config -n banking-system \
  --patch '{"data":{"app.feature.new-loan-types":"true"}}'

# Trigger rolling restart to pick up new config
kubectl rollout restart deployment/banking-app -n banking-system

# Monitor rollout
kubectl rollout status deployment/banking-app -n banking-system

# Verify configuration update
kubectl exec -n banking-system deployment/banking-app -- \
  env | grep app.feature.new-loan-types
```

---

## Monitoring & Alerting

### Prometheus Alerting Rules

#### Banking-Specific Alerts
```yaml
# k8s/monitoring/banking-alerts.yaml
groups:
- name: banking-system.rules
  rules:
  - alert: BankingApplicationDown
    expr: up{job="banking-app"} == 0
    for: 30s
    labels:
      severity: critical
      team: banking-ops
    annotations:
      summary: "Banking application is down"
      description: "Banking application {{ $labels.instance }} has been down for more than 30 seconds"
      runbook_url: "https://wiki.banking.local/runbooks/app-down"

  - alert: OAuth2AuthenticationFailureRate
    expr: rate(keycloak_authentication_failures_total[5m]) > 0.1
    for: 2m
    labels:
      severity: warning
      team: security-ops
    annotations:
      summary: "High OAuth2.1 authentication failure rate"
      description: "Authentication failure rate is {{ $value }} failures/sec over the last 5 minutes"

  - alert: LoanProcessingLatency
    expr: histogram_quantile(0.95, rate(banking_loan_processing_duration_seconds_bucket[5m])) > 30
    for: 5m
    labels:
      severity: warning
      team: banking-ops
    annotations:
      summary: "Loan processing latency is high"
      description: "95th percentile loan processing time is {{ $value }}s"

  - alert: DatabaseConnectionPoolExhaustion
    expr: hikaricp_connections_active / hikaricp_connections_max > 0.9
    for: 2m
    labels:
      severity: critical
      team: database-ops
    annotations:
      summary: "Database connection pool nearly exhausted"
      description: "Connection pool utilization is {{ $value | humanizePercentage }}"
```

#### Integration with External Systems
```bash
#!/bin/bash
# Alert manager webhook for banking compliance

curl -X POST https://compliance-system.banking.local/alerts \
  -H "Content-Type: application/json" \
  -d '{
    "alert": "PCI_COMPLIANCE_VIOLATION",
    "severity": "critical",
    "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'",
    "details": {
      "system": "banking-app",
      "violation_type": "unauthorized_access_attempt",
      "user_id": "unknown",
      "ip_address": "192.168.1.100"
    }
  }'
```

### Grafana Dashboards

#### Banking Operations Dashboard
```json
{
  "dashboard": {
    "title": "Banking System Operations",
    "panels": [
      {
        "title": "Application Health",
        "type": "stat",
        "targets": [
          {
            "expr": "up{job=\"banking-app\"}",
            "legendFormat": "{{instance}}"
          }
        ]
      },
      {
        "title": "OAuth2.1 Authentication Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(keycloak_authentication_total[5m])",
            "legendFormat": "Success Rate"
          },
          {
            "expr": "rate(keycloak_authentication_failures_total[5m])",
            "legendFormat": "Failure Rate"
          }
        ]
      },
      {
        "title": "Loan Processing Metrics",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(banking_loans_created_total[5m])",
            "legendFormat": "Loans Created/sec"
          },
          {
            "expr": "rate(banking_loans_approved_total[5m])",
            "legendFormat": "Loans Approved/sec"
          }
        ]
      }
    ]
  }
}
```

---

## Backup & Recovery

### Automated Backup Procedures

#### Database Backup
```bash
#!/bin/bash
# scripts/backup-database.sh

BACKUP_DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backups/postgres"
DATABASE="banking_db"

# Create backup directory
mkdir -p $BACKUP_DIR

# Perform database backup
kubectl exec -n banking-data deployment/postgres -- \
  pg_dump -U banking_user -d $DATABASE | \
  gzip > $BACKUP_DIR/banking_db_$BACKUP_DATE.sql.gz

# Upload to object storage
aws s3 cp $BACKUP_DIR/banking_db_$BACKUP_DATE.sql.gz \
  s3://banking-backups/database/

# Verify backup integrity
gunzip -t $BACKUP_DIR/banking_db_$BACKUP_DATE.sql.gz

# Clean up local backup (keep last 7 days)
find $BACKUP_DIR -name "*.sql.gz" -mtime +7 -delete

echo "Database backup completed: banking_db_$BACKUP_DATE.sql.gz"
```

#### Configuration Backup
```bash
#!/bin/bash
# Backup Kubernetes configurations

BACKUP_DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backups/k8s-config"

mkdir -p $BACKUP_DIR

# Backup all banking system resources
kubectl get all -n banking-system -o yaml > $BACKUP_DIR/banking-system_$BACKUP_DATE.yaml

# Backup secrets and configmaps
kubectl get secrets,configmaps -n banking-system -o yaml > $BACKUP_DIR/banking-configs_$BACKUP_DATE.yaml

# Backup custom resources
kubectl get applications.argoproj.io -n argocd -o yaml > $BACKUP_DIR/argocd-apps_$BACKUP_DATE.yaml

# Compress and upload
tar -czf $BACKUP_DIR/k8s-config_$BACKUP_DATE.tar.gz $BACKUP_DIR/*.yaml
aws s3 cp $BACKUP_DIR/k8s-config_$BACKUP_DATE.tar.gz s3://banking-backups/k8s-config/
```

### Disaster Recovery Procedures

#### Database Recovery
```bash
#!/bin/bash
# Database disaster recovery

RESTORE_FILE="banking_db_20240115_143000.sql.gz"

# Download backup from object storage
aws s3 cp s3://banking-backups/database/$RESTORE_FILE /tmp/

# Stop application to prevent writes
kubectl scale deployment banking-app --replicas=0 -n banking-system

# Create recovery database
kubectl exec -n banking-data deployment/postgres -- \
  createdb -U postgres banking_db_recovery

# Restore database
gunzip -c /tmp/$RESTORE_FILE | \
kubectl exec -i -n banking-data deployment/postgres -- \
  psql -U postgres -d banking_db_recovery

# Validate data integrity
kubectl exec -n banking-data deployment/postgres -- \
  psql -U postgres -d banking_db_recovery -c \
  "SELECT COUNT(*) FROM customers; SELECT COUNT(*) FROM loans;"

# Switch to recovery database
kubectl patch configmap banking-config -n banking-system \
  --patch '{"data":{"database.name":"banking_db_recovery"}}'

# Restart application
kubectl scale deployment banking-app --replicas=3 -n banking-system
```

---

## Security Operations

### Security Monitoring

#### Real-time Security Monitoring
```bash
#!/bin/bash
# Security monitoring script

# Monitor failed authentication attempts
kubectl logs -n banking-auth deployment/keycloak --follow | \
  grep "AUTHENTICATION_FAILED" | \
  while read line; do
    echo "$(date): $line"
    # Alert on high failure rate
    failures=$(echo "$line" | grep -o "failures: [0-9]*" | cut -d' ' -f2)
    if [ "$failures" -gt 5 ]; then
      curl -X POST https://security-team.banking.local/alerts \
        -d "High authentication failure rate detected: $failures"
    fi
  done
```

#### Compliance Auditing
```bash
#!/bin/bash
# Daily compliance audit

# Check user access logs
kubectl exec -n banking-data deployment/postgres -- \
  psql -U banking_user -d banking_db -c \
  "SELECT user_id, action, resource, timestamp FROM audit_logs 
   WHERE timestamp >= NOW() - INTERVAL '24 hours' 
   AND action IN ('LOGIN', 'LOAN_APPROVED', 'PAYMENT_PROCESSED')
   ORDER BY timestamp DESC LIMIT 100"

# Check privilege escalations
kubectl logs -n banking-system deployment/banking-app --since=24h | \
  grep "PRIVILEGE_ESCALATION" | \
  while read line; do
    echo "Privilege escalation detected: $line"
    # Log to compliance system
    curl -X POST https://compliance.banking.local/violations \
      -d "$line"
  done
```

### Certificate Management

#### Certificate Renewal
```bash
#!/bin/bash
# Automated certificate renewal

# Check certificate expiry
kubectl get certificates -n banking-system -o json | \
  jq -r '.items[] | select(.status.notAfter | fromdateiso8601 < (now + 604800)) | .metadata.name'

# Renew certificates using cert-manager
kubectl annotate certificate banking-api-tls -n banking-system \
  cert-manager.io/issue-temporary-certificate="true"

# Verify renewal
kubectl describe certificate banking-api-tls -n banking-system
```

---

## Troubleshooting

### Common Issues

#### Application Won't Start
```bash
#!/bin/bash
# Troubleshooting application startup

# Check pod status
kubectl get pods -n banking-system -l app=banking-app

# Check events
kubectl get events -n banking-system --sort-by='.lastTimestamp'

# Check logs
kubectl logs -n banking-system deployment/banking-app --previous

# Check configuration
kubectl describe configmap banking-config -n banking-system
kubectl describe secret banking-secrets -n banking-system

# Check resource constraints
kubectl top pods -n banking-system
kubectl describe nodes
```

#### Database Connection Issues
```bash
#!/bin/bash
# Database connectivity troubleshooting

# Test database connectivity from app pod
kubectl exec -n banking-system deployment/banking-app -- \
  nc -zv postgres.banking-data.svc.cluster.local 5432

# Check database status
kubectl exec -n banking-data deployment/postgres -- \
  pg_isready -U banking_user -d banking_db

# Check connection pool
kubectl logs -n banking-system deployment/banking-app | \
  grep -i "connection.*pool\|hikari"

# Check database performance
kubectl exec -n banking-data deployment/postgres -- \
  psql -U postgres -c "SELECT * FROM pg_stat_activity WHERE state = 'active'"
```

#### OAuth2.1 Authentication Problems
```bash
#!/bin/bash
# OAuth2.1 troubleshooting

# Check Keycloak health
kubectl exec -n banking-auth deployment/keycloak -- \
  curl -f http://localhost:8080/health

# Test OAuth2.1 endpoints
curl -k https://keycloak.banking.local/realms/banking-realm/.well-known/openid_configuration

# Check LDAP connectivity
kubectl exec -n banking-auth deployment/keycloak -- \
  ldapsearch -x -H ldap://ldap:389 -D "cn=admin,dc=banking,dc=local" -w password

# Check token validation
kubectl logs -n banking-system deployment/banking-app | \
  grep -i "token\|jwt\|oauth"
```

### Performance Issues

#### Application Performance Tuning
```bash
#!/bin/bash
# Performance optimization

# Check JVM metrics
kubectl exec -n banking-system deployment/banking-app -- \
  jcmd 1 VM.flags | grep -E "(Heap|GC)"

# Monitor garbage collection
kubectl logs -n banking-system deployment/banking-app | \
  grep -i "gc\|garbage"

# Check database query performance
kubectl exec -n banking-data deployment/postgres -- \
  psql -U banking_user -d banking_db -c \
  "SELECT query, mean_time, calls FROM pg_stat_statements 
   WHERE mean_time > 1000 ORDER BY mean_time DESC LIMIT 10"

# Monitor cache hit ratios
kubectl exec -n banking-data deployment/redis -- \
  redis-cli info stats | grep keyspace_hits
```

### Emergency Procedures

#### Emergency Shutdown
```bash
#!/bin/bash
# Emergency system shutdown

echo "EMERGENCY SHUTDOWN INITIATED"

# Stop new traffic
kubectl patch service banking-app-service -n banking-system \
  -p '{"spec":{"selector":{"app":"maintenance"}}}'

# Scale down applications
kubectl scale deployment banking-app --replicas=0 -n banking-system

# Backup critical data
./scripts/emergency-backup.sh

# Notify stakeholders
curl -X POST https://notifications.banking.local/emergency \
  -d "Banking system emergency shutdown completed at $(date)"

echo "Emergency shutdown completed"
```

#### Emergency Recovery
```bash
#!/bin/bash
# Emergency recovery procedures

echo "EMERGENCY RECOVERY INITIATED"

# Check infrastructure health
kubectl cluster-info
kubectl get nodes

# Restore from backups if needed
if [ "$1" == "restore" ]; then
  ./scripts/restore-from-backup.sh latest
fi

# Start critical services
kubectl scale deployment postgres --replicas=1 -n banking-data
kubectl scale deployment keycloak --replicas=1 -n banking-auth
kubectl scale deployment banking-app --replicas=1 -n banking-system

# Verify system health
sleep 60
./scripts/health-check.sh

# Gradually increase capacity
kubectl scale deployment banking-app --replicas=3 -n banking-system

echo "Emergency recovery completed"
```

---

## Performance Optimization

### Database Optimization

#### Query Performance Tuning
```sql
-- Database performance optimization
-- Analyze slow queries
SELECT query, mean_time, calls, total_time
FROM pg_stat_statements 
WHERE mean_time > 100 
ORDER BY mean_time DESC;

-- Optimize frequently used indexes
CREATE INDEX CONCURRENTLY idx_loans_customer_status_created 
ON loans(customer_id, status, created_at) 
WHERE status IN ('ACTIVE', 'PENDING');

-- Update table statistics
ANALYZE loans;
ANALYZE customers;
ANALYZE payments;
```

#### Connection Pool Optimization
```yaml
# Application configuration for connection pooling
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      validation-timeout: 5000
      initialization-fail-timeout: 1
```

### Application Optimization

#### JVM Tuning
```bash
#!/bin/bash
# JVM optimization for banking application

# Production JVM flags
JVM_OPTS="-Xms2g -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UseStringDeduplication \
  -XX:+PrintGCDetails \
  -XX:+PrintGCTimeStamps \
  -Djava.security.egd=file:/dev/./urandom"

# Update deployment with optimized JVM settings
kubectl patch deployment banking-app -n banking-system \
  -p '{"spec":{"template":{"spec":{"containers":[{"name":"banking-app","env":[{"name":"JAVA_OPTS","value":"'$JVM_OPTS'"}]}]}}}}'
```

---

## Conclusion

The Deployment & Operations Guide provides comprehensive procedures for managing the Enterprise Banking System in production. Key operational capabilities include:

### Deployment Excellence
- **GitOps Automation**: Streamlined deployment with ArgoCD
- **Blue-Green Deployments**: Zero-downtime updates
- **Infrastructure as Code**: Consistent environment provisioning
- **Security Integration**: Automated security validation

### Operational Efficiency
- **Automated Monitoring**: Comprehensive system observability
- **Proactive Alerting**: Early issue detection and response
- **Disaster Recovery**: Rapid system restoration capabilities
- **Performance Optimization**: Continuous system tuning

### Compliance & Security
- **Audit Procedures**: Comprehensive compliance monitoring
- **Security Operations**: Real-time threat detection
- **Certificate Management**: Automated certificate lifecycle
- **Emergency Procedures**: Defined incident response protocols

For detailed technical implementation, refer to the [Infrastructure Architecture Guide](../infrastructure-architecture/Infrastructure-Architecture-Guide.md) and [Security Architecture Overview](../security-architecture/Security-Architecture-Overview.md).