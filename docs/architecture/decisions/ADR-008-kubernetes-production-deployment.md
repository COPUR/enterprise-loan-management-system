# ADR-008: Kubernetes Production Deployment with Istio Service Mesh

## Status
**Accepted** - December 2024

## Context

The Enterprise Loan Management System requires a production-grade container orchestration platform that can handle banking workloads with strict security, compliance, and availability requirements. The system must support microservices architecture with service mesh capabilities, zero-downtime deployments, and banking-grade observability.

## Decision

We will deploy on **Kubernetes with Istio Service Mesh** using AWS EKS as the managed platform, implementing zero-trust security, comprehensive observability, and banking-compliant resource management.

### Core Architecture Decision

```yaml
# Production Kubernetes deployment with banking security
apiVersion: apps/v1
kind: Deployment
metadata:
  name: enterprise-loan-system
  labels:
    app: enterprise-loan-system
    version: v1
    compliance: banking-grade
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 0      # Zero-downtime for banking operations
      maxSurge: 1
```

## Technical Implementation

### 1. **Kubernetes Cluster Architecture**

#### AWS EKS Configuration
- **Version**: EKS 1.28+ with regular updates
- **Node Groups**: Dedicated pools for banking-system and monitoring
- **Multi-AZ**: Cross-availability zone deployment for high availability
- **Networking**: VPC with public/private subnets and security groups

#### Banking Security Context
```yaml
securityContext:
  runAsNonRoot: true
  runAsUser: 1000
  runAsGroup: 1000
  allowPrivilegeEscalation: false
  readOnlyRootFilesystem: true
  capabilities:
    drop:
    - ALL
  seccompProfile:
    type: RuntimeDefault
```

### 2. **Istio Service Mesh Integration**

#### Service Mesh Architecture
```yaml
# Banking Gateway with OAuth2.1 and mTLS
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: banking-gateway
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
    - banking.example.com
```

#### Zero-Trust Security Policies
```yaml
# Strict mTLS enforcement for banking services
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: banking-mtls
spec:
  mtls:
    mode: STRICT
```

### 3. **Resource Management for Banking Workloads**

#### Resource Allocation
```yaml
resources:
  requests:
    memory: "1Gi"
    cpu: "500m"
  limits:
    memory: "2Gi"
    cpu: "1000m"
    ephemeral-storage: "1Gi"
```

#### Horizontal Pod Autoscaler
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: banking-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: enterprise-loan-system
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
```

## Banking-Specific Kubernetes Configuration

### 1. **Health Checks for Financial Services**

```yaml
# Comprehensive health checks for banking operations
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 30
  timeoutSeconds: 10
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3

startupProbe:
  httpGet:
    path: /actuator/health/startup
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 12
```

### 2. **Banking Compliance Annotations**

```yaml
metadata:
  annotations:
    banking.compliance/pci-dss: "v4.0"
    banking.compliance/fapi: "1.0-advanced"
    banking.compliance/sox: "required"
    banking.audit/required: "true"
    istio.io/rev: stable
    prometheus.io/scrape: "true"
    prometheus.io/port: "8080"
    prometheus.io/path: "/actuator/prometheus"
```

### 3. **Network Policies for Banking Security**

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: banking-network-policy
spec:
  podSelector:
    matchLabels:
      app: enterprise-loan-system
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
          app: banking-services
  egress:
  - to:
    - namespaceSelector:
        matchLabels:
          name: banking-data
    ports:
    - protocol: TCP
      port: 5432  # PostgreSQL
    - protocol: TCP
      port: 6379  # Redis
```

## Service Mesh Configuration

### 1. **Circuit Breaker for Banking Services**

```yaml
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: banking-circuit-breaker
spec:
  host: enterprise-loan-system
  trafficPolicy:
    outlierDetection:
      consecutiveErrors: 3
      interval: 30s
      baseEjectionTime: 30s
      maxEjectionPercent: 50
    connectionPool:
      tcp:
        maxConnections: 100
      http:
        http1MaxPendingRequests: 50
        maxRequestsPerConnection: 2
```

### 2. **Retry Policies for Financial Operations**

```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: banking-retry-policy
spec:
  http:
  - match:
    - uri:
        prefix: "/api/v1/loans"
  - match:
    - uri:
        prefix: "/api/v1/payments"
    retries:
      attempts: 3
      perTryTimeout: 2s
      retryOn: gateway-error,connect-failure,refused-stream
```

## Deployment Strategy

### 1. **Zero-Downtime Rolling Updates**

```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxUnavailable: 0        # Critical for banking operations
    maxSurge: 1             # Conservative approach for resource management
```

### 2. **Deployment Validation**

```bash
# Deployment validation script
kubectl rollout status deployment/enterprise-loan-system --timeout=300s
kubectl get pods -l app=enterprise-loan-system -o wide
kubectl exec deployment/enterprise-loan-system -- curl -f http://localhost:8080/actuator/health
```

## Observability and Monitoring

### 1. **Prometheus Integration**

```yaml
# Service monitor for banking metrics
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: banking-service-monitor
spec:
  selector:
    matchLabels:
      app: enterprise-loan-system
  endpoints:
  - port: http
    path: /actuator/prometheus
    interval: 30s
```

### 2. **Distributed Tracing**

```yaml
# Jaeger tracing configuration
apiVersion: v1
kind: ConfigMap
metadata:
  name: jaeger-config
data:
  jaeger.yaml: |
    sampling:
      default_strategy:
        type: probabilistic
        param: 0.1
    service_name: enterprise-loan-system
```

## Secrets Management

### 1. **Banking Secrets Configuration**

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: banking-secrets
  annotations:
    banking.compliance/encryption: "AES-256"
type: Opaque
data:
  database-password: <base64-encoded>
  jwt-secret: <base64-encoded>
  oauth-client-secret: <base64-encoded>
```

### 2. **Certificate Management**

```yaml
# cert-manager for TLS certificates
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: banking-tls-cert
spec:
  secretName: banking-tls-cert
  issuerRef:
    name: letsencrypt-prod
    kind: ClusterIssuer
  dnsNames:
  - banking.example.com
```

## Backup and Disaster Recovery

### 1. **Persistent Volume Configuration**

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: banking-data-pvc
spec:
  accessModes:
  - ReadWriteOnce
  resources:
    requests:
      storage: 100Gi
  storageClassName: gp3-encrypted
```

### 2. **Backup Strategy**

```yaml
# Velero backup configuration
apiVersion: velero.io/v1
kind: Backup
metadata:
  name: banking-daily-backup
spec:
  includedNamespaces:
  - banking-system
  labelSelector:
    matchLabels:
      backup: required
  ttl: 720h0m0s  # 30 days retention
```

## Consequences

### Positive
- ✅ **Banking-Grade Security**: Zero-trust with mTLS and strict network policies
- ✅ **High Availability**: Multi-AZ deployment with zero-downtime updates
- ✅ **Scalability**: Auto-scaling based on banking workload patterns
- ✅ **Observability**: Comprehensive monitoring and distributed tracing
- ✅ **Compliance**: Banking regulatory requirements met
- ✅ **Service Mesh Benefits**: Circuit breakers, retries, and traffic management

### Negative
- ❌ **Complexity**: Increased operational complexity with service mesh
- ❌ **Resource Overhead**: Istio sidecars consume additional resources
- ❌ **Learning Curve**: Team requires Kubernetes and Istio expertise

### Risks Mitigated
- ✅ **Service Failures**: Circuit breakers and health checks
- ✅ **Security Breaches**: Zero-trust architecture and network policies
- ✅ **Data Loss**: Automated backups and disaster recovery
- ✅ **Performance Issues**: Resource limits and monitoring
- ✅ **Compliance Violations**: Banking-specific security controls

## Performance Characteristics

### Expected Performance
- **Availability**: 99.99% uptime (4.38 minutes downtime per month)
- **Response Time**: P95 < 200ms for banking operations
- **Throughput**: 10,000+ transactions per second
- **Scaling**: 3-10 replicas based on load

### Resource Utilization
- **CPU**: 70% average utilization target
- **Memory**: 80% average utilization target
- **Network**: Optimized with service mesh routing

## Related ADRs
- ADR-006: Zero-Trust Security (Network security policies)
- ADR-007: Docker Multi-Stage Architecture (Container strategy)
- ADR-009: AWS EKS Infrastructure Design (Cloud platform)
- ADR-011: Monitoring & Observability (Prometheus integration)
- ADR-012: Security Architecture (OAuth2.1 + FAPI)

## Implementation Timeline
- **Phase 1**: Basic Kubernetes deployment ✅ Completed
- **Phase 2**: Istio service mesh integration ✅ Completed
- **Phase 3**: Banking security hardening ✅ Completed
- **Phase 4**: Production observability ✅ Completed
- **Phase 5**: Disaster recovery ✅ Completed

## Approval
- **Architecture Team**: Approved
- **Security Team**: Approved
- **Banking Compliance**: Approved
- **DevOps Team**: Approved
- **SRE Team**: Approved

---
*This ADR documents the production Kubernetes deployment strategy with Istio service mesh for enterprise banking operations, ensuring security, compliance, and operational excellence.*