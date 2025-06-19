# Infrastructure Architecture Guide
## Enterprise Banking System - Cloud-Native Infrastructure & DevOps

### Table of Contents
1. [Infrastructure Overview](#infrastructure-overview)
2. [Container Architecture](#container-architecture)
3. [Kubernetes Deployment](#kubernetes-deployment)
4. [Monitoring & Observability](#monitoring--observability)
5. [CI/CD Pipeline](#cicd-pipeline)
6. [Security Infrastructure](#security-infrastructure)
7. [Data Management](#data-management)
8. [Disaster Recovery](#disaster-recovery)

---

## Infrastructure Overview

The Enterprise Banking System is built on a cloud-native infrastructure following 12-Factor App methodology and microservices architecture. The system provides high availability, scalability, and security for mission-critical banking operations.

### Architecture Principles

1. **Cloud-Native Design**
   - Container-first architecture
   - Kubernetes orchestration
   - Horizontal auto-scaling
   - Service mesh integration

2. **Infrastructure as Code (IaC)**
   - Terraform for infrastructure provisioning
   - Helm charts for application deployment
   - GitOps workflow with ArgoCD
   - Configuration drift detection

3. **Zero-Trust Security**
   - Network segmentation with VPCs
   - Encrypted communication (mTLS)
   - Identity-based access control
   - Continuous security monitoring

4. **Observability-First**
   - Comprehensive metrics collection
   - Distributed tracing
   - Centralized logging
   - Real-time alerting

![Microservices Architecture](../generated-diagrams/Microservices%20Architecture.svg)

---

## Container Architecture

### Multi-Stage Docker Build

The system uses optimized multi-stage Docker builds for security and performance:

```dockerfile
# Build stage
FROM gradle:8.5-jdk21-alpine AS builder
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY src ./src
RUN gradle build --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -g 1000 banking && adduser -u 1000 -G banking -s /bin/sh -D banking
USER banking:banking
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Container Security

- **Non-root user execution** for security isolation
- **Read-only root filesystem** where possible
- **Minimal base images** (Alpine Linux)
- **Security scanning** with Trivy and Docker Scout
- **Runtime protection** with Falco

### Image Management

```yaml
Image Registry: harbor.banking.local
Naming Convention: 
  - banking-app:1.0.0
  - keycloak-custom:23.0.4-banking
  - postgres:16-alpine
  
Security:
  - Image signing with Cosign
  - Vulnerability scanning
  - SBOM generation
  - Harbor security policies
```

---

## Kubernetes Deployment

### Cluster Architecture

#### Production Cluster Specifications
```yaml
Kubernetes Version: 1.28.x
Node Configuration:
  Control Plane: 3 nodes (4 vCPU, 8GB RAM)
  Worker Nodes: 6 nodes (8 vCPU, 32GB RAM)
  Networking: Calico CNI
  Storage: Persistent volumes with CSI drivers
```

### Namespace Organization

```yaml
Namespaces:
  banking-system:     # Main application components
  banking-auth:       # OAuth2.1 and identity services
  banking-data:       # Database and cache services
  banking-monitoring: # Observability stack
  banking-security:   # Security tools and policies
```

### Resource Management

#### Pod Resource Limits
```yaml
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: banking-app
    resources:
      requests:
        memory: "512Mi"
        cpu: "250m"
      limits:
        memory: "2Gi"
        cpu: "1000m"
    securityContext:
      runAsNonRoot: true
      runAsUser: 1000
      runAsGroup: 1000
      allowPrivilegeEscalation: false
      readOnlyRootFilesystem: false
      capabilities:
        drop: ["ALL"]
        add: ["NET_BIND_SERVICE"]
```

### Auto-Scaling Configuration

#### Horizontal Pod Autoscaler (HPA)
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: banking-app-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: banking-app
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
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
      - type: Percent
        value: 100
        periodSeconds: 60
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 10
        periodSeconds: 60
```

#### Vertical Pod Autoscaler (VPA)
```yaml
apiVersion: autoscaling.k8s.io/v1
kind: VerticalPodAutoscaler
metadata:
  name: banking-app-vpa
spec:
  targetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: banking-app
  updatePolicy:
    updateMode: "Auto"
  resourcePolicy:
    containerPolicies:
    - containerName: banking-app
      maxAllowed:
        cpu: 2
        memory: 4Gi
      minAllowed:
        cpu: 100m
        memory: 256Mi
```

### Service Mesh Integration

#### Istio Configuration
```yaml
apiVersion: install.istio.io/v1alpha1
kind: IstioOperator
metadata:
  name: banking-system
spec:
  values:
    global:
      meshID: banking-mesh
      network: banking-network
    pilot:
      env:
        EXTERNAL_ISTIOD: false
  components:
    pilot:
      k8s:
        resources:
          requests:
            cpu: 200m
            memory: 128Mi
    ingressGateways:
    - name: istio-ingressgateway
      enabled: true
      k8s:
        service:
          type: LoadBalancer
```

---

## Monitoring & Observability

### Metrics Collection

#### Prometheus Configuration
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-config
data:
  prometheus.yml: |
    global:
      scrape_interval: 15s
      evaluation_interval: 15s
    
    rule_files:
      - "banking_rules.yml"
    
    scrape_configs:
    - job_name: 'banking-app'
      kubernetes_sd_configs:
      - role: pod
      relabel_configs:
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        action: keep
        regex: true
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
        action: replace
        target_label: __metrics_path__
        regex: (.+)
    
    - job_name: 'keycloak'
      static_configs:
      - targets: ['keycloak:8080']
      metrics_path: '/realms/banking-realm/metrics'
    
    - job_name: 'postgres-exporter'
      static_configs:
      - targets: ['postgres-exporter:9187']
```

#### Banking-Specific Metrics
```yaml
Custom Metrics:
  banking_loans_created_total: Counter of loans created
  banking_authentication_duration_seconds: Authentication time histogram
  banking_authorization_failures_total: Authorization failure counter
  banking_transaction_amount_histogram: Transaction amount distribution
  banking_compliance_violations_total: Compliance violation counter
  
Prometheus Rules:
  - Banking SLA monitoring (99.9% uptime)
  - OAuth2.1 performance metrics
  - Security incident detection
  - Capacity planning metrics
```

### Distributed Tracing

#### Jaeger Integration
```yaml
apiVersion: jaegertracing.io/v1
kind: Jaeger
metadata:
  name: banking-jaeger
spec:
  strategy: production
  collector:
    resources:
      limits:
        memory: 1Gi
        cpu: 500m
  storage:
    type: elasticsearch
    elasticsearch:
      nodeCount: 3
      resources:
        limits:
          memory: 2Gi
        requests:
          memory: 1Gi
```

### Centralized Logging

#### ELK Stack Configuration
```yaml
Elasticsearch:
  Version: 8.11.x
  Nodes: 3 master, 6 data nodes
  Storage: 2TB per data node
  Indices:
    - banking-app-logs-*
    - banking-auth-logs-*
    - banking-audit-logs-*
    - banking-security-logs-*

Logstash Pipeline:
  input:
    beats:
      port: 5044
  filter:
    if [kubernetes][namespace] == "banking-system" {
      mutate {
        add_field => { "environment" => "production" }
        add_field => { "service_type" => "banking" }
      }
    }
  output:
    elasticsearch:
      hosts => ["elasticsearch:9200"]
      index => "banking-%{[kubernetes][namespace]}-%{+YYYY.MM.dd}"

Kibana Dashboards:
  - Banking Application Performance
  - OAuth2.1 Authentication Analysis
  - Security Incident Dashboard
  - Compliance Audit Reports
```

### Alerting Rules

#### Critical Banking Alerts
```yaml
groups:
- name: banking.critical
  rules:
  - alert: BankingApplicationDown
    expr: up{job="banking-app"} == 0
    for: 30s
    labels:
      severity: critical
    annotations:
      summary: "Banking application is down"
      description: "Banking application has been down for more than 30 seconds"

  - alert: OAuth2AuthenticationFailureHigh
    expr: rate(banking_authentication_failures_total[5m]) > 0.1
    for: 2m
    labels:
      severity: warning
    annotations:
      summary: "High OAuth2.1 authentication failure rate"
      description: "Authentication failure rate is {{ $value }} failures/sec"

  - alert: BankingComplianceViolation
    expr: increase(banking_compliance_violations_total[1m]) > 0
    for: 0s
    labels:
      severity: critical
    annotations:
      summary: "Banking compliance violation detected"
      description: "Compliance violation: {{ $labels.violation_type }}"
```

---

## CI/CD Pipeline

### GitOps Workflow

#### GitHub Actions Pipeline
```yaml
name: Banking System CI/CD
on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  security-scan:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    - name: Run Trivy vulnerability scanner
      uses: aquasecurity/trivy-action@master
      with:
        scan-type: 'fs'
        scan-ref: '.'
        format: 'sarif'
        output: 'trivy-results.sarif'
    
    - name: OWASP Dependency Check
      run: |
        ./gradlew dependencyCheckAnalyze
        
  build-and-test:
    runs-on: ubuntu-latest
    needs: security-scan
    steps:
    - uses: actions/checkout@v4
    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
    
    - name: Cache Gradle packages
      uses: actions/cache@v3
      with:
        path: ~/.gradle/caches
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle') }}
    
    - name: Run tests
      run: ./gradlew test jacocoTestReport
    
    - name: Upload coverage reports
      uses: codecov/codecov-action@v3

  build-image:
    runs-on: ubuntu-latest
    needs: build-and-test
    if: github.ref == 'refs/heads/main'
    steps:
    - uses: actions/checkout@v4
    - name: Build and push Docker image
      uses: docker/build-push-action@v5
      with:
        context: .
        push: true
        tags: |
          harbor.banking.local/banking/app:${{ github.sha }}
          harbor.banking.local/banking/app:latest
        cache-from: type=gha
        cache-to: type=gha,mode=max

  deploy:
    runs-on: ubuntu-latest
    needs: build-image
    if: github.ref == 'refs/heads/main'
    steps:
    - name: Deploy to staging
      run: |
        # Update ArgoCD application
        kubectl patch application banking-app-staging \
          -p '{"spec":{"source":{"helm":{"parameters":[{"name":"image.tag","value":"'${{ github.sha }}'"}]}}}}' \
          --type merge
```

### ArgoCD Configuration

#### Application Definition
```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: banking-system-prod
  namespace: argocd
spec:
  project: banking
  source:
    repoURL: https://github.com/banking/enterprise-loan-management-system
    targetRevision: main
    path: k8s/helm/banking-system
    helm:
      parameters:
      - name: image.tag
        value: "1.0.0"
      - name: replicas
        value: "3"
      - name: environment
        value: "production"
  destination:
    server: https://kubernetes.default.svc
    namespace: banking-system
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
    - CreateNamespace=true
    retry:
      limit: 5
      backoff:
        duration: 5s
        factor: 2
        maxDuration: 3m
```

---

## Security Infrastructure

### Network Security

#### Network Policies
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: banking-app-network-policy
spec:
  podSelector:
    matchLabels:
      app: banking-app
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
          app: istio-gateway
    ports:
    - protocol: TCP
      port: 8080
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
          app: keycloak
    ports:
    - protocol: TCP
      port: 8080
```

### Security Scanning

#### Policy as Code with OPA Gatekeeper
```yaml
apiVersion: templates.gatekeeper.sh/v1beta1
kind: ConstraintTemplate
metadata:
  name: bannedimagerepos
spec:
  crd:
    spec:
      names:
        kind: BannedImageRepos
      validation:
        type: object
        properties:
          repos:
            type: array
            items:
              type: string
  targets:
    - target: admission.k8s.gatekeeper.sh
      rego: |
        package bannedimagerepos
        
        violation[{"msg": msg}] {
          container := input.review.object.spec.containers[_]
          image := container.image
          banned_repo := input.parameters.repos[_]
          startswith(image, banned_repo)
          msg := sprintf("Image %v uses banned repository %v", [image, banned_repo])
        }
```

---

## Data Management

### Database Architecture

#### PostgreSQL High Availability
```yaml
Primary Database:
  Version: PostgreSQL 16
  Configuration:
    shared_preload_libraries: 'pg_stat_statements,auto_explain'
    max_connections: 200
    shared_buffers: 8GB
    effective_cache_size: 24GB
    work_mem: 64MB
    maintenance_work_mem: 2GB
    
Streaming Replication:
  Read Replicas: 2 instances
  Synchronous Commit: on
  WAL Archiving: enabled
  Point-in-time Recovery: enabled
  
Backup Strategy:
  Full Backup: Daily at 2 AM UTC
  WAL Archiving: Continuous
  Retention: 30 days
  Cross-region Replication: enabled
```

#### Redis Cluster
```yaml
Redis Configuration:
  Mode: Cluster
  Nodes: 6 (3 masters, 3 replicas)
  Memory: 8GB per node
  Persistence: AOF + RDB
  
Banking Use Cases:
  - OAuth2.1 token blacklist
  - Session storage
  - Rate limiting counters
  - Cache for Party role data
  - Authentication failure tracking
```

### Data Protection

#### Encryption Strategy
```yaml
Encryption at Rest:
  Database: AES-256 with TDE
  Kubernetes Secrets: Sealed Secrets
  Storage Volumes: LUKS encryption
  
Encryption in Transit:
  TLS Version: 1.3
  Certificate Management: cert-manager
  Service Mesh: mTLS with Istio
  
Key Management:
  Provider: HashiCorp Vault
  Key Rotation: Quarterly
  Hardware Security Module: Enabled
```

---

## Disaster Recovery

### Backup Strategy

#### Multi-Tier Backup
```yaml
Tier 1 - Local Backups:
  Frequency: Every 4 hours
  Retention: 7 days
  Storage: Local SSD arrays
  RTO: 15 minutes
  RPO: 4 hours

Tier 2 - Regional Backups:
  Frequency: Daily
  Retention: 30 days
  Storage: Object storage (S3-compatible)
  RTO: 2 hours
  RPO: 24 hours

Tier 3 - Cross-Region Backups:
  Frequency: Weekly
  Retention: 1 year
  Storage: Glacier-class storage
  RTO: 8 hours
  RPO: 1 week
```

### High Availability Design

#### Multi-Zone Deployment
```yaml
Availability Zones: 3
Node Distribution:
  Control Plane: 1 node per AZ
  Worker Nodes: 2 nodes per AZ
  
Load Balancing:
  External: Application Load Balancer
  Internal: Istio service mesh
  Database: pgpool-II
  
Failover Strategy:
  Automated: Health check based
  Manual: Emergency procedures
  Testing: Monthly DR drills
```

### Recovery Procedures

#### Banking System Recovery Playbook

1. **Assessment Phase (0-15 minutes)**
   - Determine incident scope and impact
   - Activate incident response team
   - Assess data integrity and availability

2. **Immediate Response (15-60 minutes)**
   - Switch to disaster recovery site if needed
   - Restore critical services (OAuth2.1, core banking)
   - Validate system functionality

3. **Full Recovery (1-4 hours)**
   - Restore all microservices
   - Synchronize data from backups
   - Perform comprehensive testing

4. **Post-Recovery (4-24 hours)**
   - Monitor system stability
   - Document lessons learned
   - Update recovery procedures

---

## Performance Optimization

### Capacity Planning

#### Resource Requirements by Environment

| Environment | CPU Cores | Memory | Storage | Network |
|-------------|-----------|---------|---------|---------|
| Development | 16 | 64GB | 1TB | 1Gbps |
| Staging | 32 | 128GB | 5TB | 10Gbps |
| Production | 96 | 384GB | 20TB | 25Gbps |

### Optimization Strategies

#### Database Performance
```sql
-- Optimized indexes for banking queries
CREATE INDEX CONCURRENTLY idx_loans_customer_status 
ON loans(customer_id, status) 
WHERE status IN ('ACTIVE', 'PENDING');

CREATE INDEX CONCURRENTLY idx_party_roles_active 
ON party_roles(party_id, role_name, effective_from, effective_to) 
WHERE active = true;

-- Partitioning for audit logs
CREATE TABLE audit_logs (
    id BIGSERIAL,
    event_time TIMESTAMP NOT NULL,
    user_id VARCHAR(255),
    action VARCHAR(100),
    details JSONB
) PARTITION BY RANGE (event_time);
```

#### Application Performance
- **JVM Tuning**: G1GC with 8GB heap
- **Connection Pooling**: HikariCP with 20 connections
- **Caching Strategy**: Multi-level caching (L1: Caffeine, L2: Redis)
- **Async Processing**: Virtual threads (Java 21)

---

## Compliance Infrastructure

### Audit Trail Infrastructure

#### Immutable Audit Logging
```yaml
Audit Pipeline:
  Collection: Filebeat -> Logstash -> Elasticsearch
  Storage: Write-once indices with ILM
  Retention: 7 years for financial records
  Integrity: Digital signatures with timestamping
  
Compliance Reports:
  SOX: Quarterly access reviews
  PCI DSS: Monthly security assessments
  GDPR: Data processing activity records
  FAPI: Real-time transaction monitoring
```

### Regulatory Compliance

#### Automated Compliance Checking
```yaml
Policies:
  - Data retention enforcement
  - Geographic data residency
  - Encryption compliance validation
  - Access control verification
  - Audit trail completeness

Tools:
  - Open Policy Agent (OPA)
  - Falco for runtime security
  - Compliance Operator
  - Custom policy engines
```

---

## Conclusion

The infrastructure architecture provides a robust, scalable, and secure foundation for the Enterprise Banking System. Key achievements include:

### Infrastructure Benefits

1. **High Availability**: 99.95% uptime SLA
2. **Scalability**: Auto-scaling from 3 to 100+ pods
3. **Security**: Defense-in-depth with zero-trust principles
4. **Compliance**: Automated regulatory compliance validation
5. **Observability**: Comprehensive monitoring and alerting
6. **Recovery**: Sub-4-hour disaster recovery capability

### Operational Excellence

- **GitOps**: Automated deployments with ArgoCD
- **Infrastructure as Code**: Terraform and Helm charts
- **Security**: Continuous vulnerability scanning
- **Monitoring**: Real-time performance and security metrics
- **Compliance**: Automated audit trail and reporting

For detailed operational procedures, refer to the [Security Architecture Overview](../security-architecture/Security-Architecture-Overview.md) and [OAuth2.1 Architecture Guide](../OAuth2.1-Architecture-Guide.md).