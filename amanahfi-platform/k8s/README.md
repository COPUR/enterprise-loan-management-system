# AmanahFi Platform Kubernetes Deployment

This directory contains Kubernetes manifests and Kustomize configurations for deploying the AmanahFi Islamic Finance and CBDC Platform.

## 🏗️ Architecture Overview

The AmanahFi Platform is deployed using a microservices architecture with the following components:

- **AmanahFi Platform Application**: Main Spring Boot application
- **PostgreSQL Cluster**: Primary database for persistent storage
- **Redis Cluster**: Caching layer for performance optimization
- **Kafka Cluster**: Event streaming for domain events
- **Keycloak**: Identity and Access Management
- **Monitoring Stack**: Prometheus, Grafana, AlertManager

## 📁 Directory Structure

```
k8s/
├── base/                           # Base Kustomize configuration
│   ├── namespace.yaml             # Namespace definition
│   ├── configmap.yaml             # Application configuration
│   ├── secret.yaml                # Secret placeholders
│   ├── deployment.yaml            # Main application deployment
│   ├── service.yaml               # Service definitions
│   ├── ingress.yaml               # Ingress configuration
│   ├── hpa.yaml                   # Horizontal Pod Autoscaler
│   ├── rbac.yaml                  # RBAC permissions
│   └── kustomization.yaml         # Base kustomization
├── overlays/                      # Environment-specific overlays
│   ├── development/               # Development environment
│   ├── staging/                   # Staging environment
│   └── production/                # Production environment
├── monitoring/                    # Monitoring and observability
│   ├── prometheus/                # Prometheus configuration
│   ├── grafana/                   # Grafana dashboards
│   └── alertmanager/              # Alert management
└── security/                      # Security policies
    ├── network-policies/          # Network security policies
    ├── pod-security/              # Pod security standards
    └── certificates/              # TLS certificates
```

## 🚀 Quick Start

### Prerequisites

1. **Kubernetes Cluster**: v1.25+
2. **Kustomize**: v4.0+
3. **kubectl**: v1.25+
4. **Helm**: v3.0+ (for dependencies)

### 1. Install Dependencies

```bash
# Install PostgreSQL using Helm
helm repo add bitnami https://charts.bitnami.com/bitnami
helm install postgres bitnami/postgresql \
  --namespace amanahfi-platform \
  --create-namespace \
  --set auth.database=amanahfi_platform \
  --set auth.username=amanahfi_user

# Install Redis Cluster
helm install redis bitnami/redis-cluster \
  --namespace amanahfi-platform \
  --set cluster.nodes=6 \
  --set cluster.replicas=1

# Install Kafka
helm install kafka bitnami/kafka \
  --namespace amanahfi-platform \
  --set replicaCount=3
```

### 2. Configure Secrets

```bash
# Create secrets (replace with actual values)
kubectl create secret generic amanahfi-platform-secrets \
  --namespace amanahfi-platform \
  --from-literal=DATABASE_PASSWORD=your_db_password \
  --from-literal=REDIS_PASSWORD=your_redis_password \
  --from-literal=JWT_SECRET_KEY=your_jwt_secret

# Create TLS certificate secret
kubectl create secret tls amanahfi-platform-tls \
  --namespace amanahfi-platform \
  --cert=path/to/tls.crt \
  --key=path/to/tls.key
```

### 3. Deploy Application

```bash
# Deploy to development
kubectl apply -k overlays/development

# Deploy to staging
kubectl apply -k overlays/staging

# Deploy to production
kubectl apply -k overlays/production
```

## 🔧 Configuration

### Environment Variables

Key configuration parameters are defined in ConfigMaps:

| Variable | Description | Default |
|----------|-------------|---------|
| `ISLAMIC_FINANCE_ENABLED` | Enable Islamic Finance features | `true` |
| `SHARIA_COMPLIANCE_MODE` | Compliance validation level | `strict` |
| `CBDC_INTEGRATION_ENABLED` | Enable Digital Dirham features | `true` |
| `DIGITAL_DIRHAM_NETWORK` | Corda network environment | `production` |
| `SUPPORTED_JURISDICTIONS` | Supported MENAT countries | `UAE,SAU,QAT,KWT,BHR,OMN,TUR` |

### Resource Requirements

#### Development
- **CPU**: 500m (request), 1000m (limit)
- **Memory**: 1Gi (request), 2Gi (limit)
- **Replicas**: 2

#### Production
- **CPU**: 1000m (request), 2000m (limit)
- **Memory**: 2Gi (request), 4Gi (limit)
- **Replicas**: 5 (min), 50 (max via HPA)

## 🛡️ Security

### Network Policies

- **Default Deny**: All ingress/egress traffic blocked by default
- **Ingress Allow**: Traffic allowed from ingress controller and monitoring
- **Egress Allow**: Specific egress to databases, external APIs
- **Workload Isolation**: Separate policies for Islamic Finance and CBDC workloads

### Pod Security

- **Security Context**: Non-root user (1001), read-only filesystem
- **Service Account**: Minimal RBAC permissions
- **Resource Limits**: CPU and memory limits enforced
- **Image Security**: Distroless base images, vulnerability scanning

### Mutual TLS

```yaml
# Enable mTLS for high-security endpoints
nginx.ingress.kubernetes.io/auth-tls-verify-client: "on"
nginx.ingress.kubernetes.io/auth-tls-secret: "amanahfi-platform/ca-secret"
```

## 📊 Monitoring

### Metrics

The application exposes Prometheus metrics at `/actuator/prometheus`:

- **Business Metrics**: Islamic Finance transactions, CBDC transfers
- **Application Metrics**: JVM, HTTP requests, database connections
- **Regulatory Metrics**: Compliance validations, Sharia checks

### Health Checks

- **Liveness Probe**: `/actuator/health/liveness`
- **Readiness Probe**: `/actuator/health/readiness`
- **Startup Probe**: `/actuator/health/liveness` (with extended timeout)

### Dashboards

Pre-configured Grafana dashboards available:

1. **Application Overview**: Request rates, response times, error rates
2. **Islamic Finance**: Product metrics, Sharia compliance rates
3. **CBDC Operations**: Digital Dirham transactions, Corda network status
4. **Infrastructure**: JVM metrics, database performance, cache hit rates

## 🚨 Alerting

### Critical Alerts

- **Application Down**: All pods unavailable
- **High Error Rate**: >5% error rate for 5 minutes
- **Sharia Compliance Failure**: Non-compliant transaction detected
- **CBDC Network Issues**: Corda network connectivity problems

### Warning Alerts

- **High Response Time**: >1s average response time
- **Memory Usage**: >80% memory utilization
- **Database Connections**: >80% connection pool usage

## 🔄 CI/CD Integration

### GitHub Actions

```yaml
# Example deployment workflow
- name: Deploy to Kubernetes
  run: |
    kubectl apply -k k8s/overlays/${{ env.ENVIRONMENT }}
    kubectl rollout status deployment/amanahfi-platform -n amanahfi-platform
```

### GitOps with ArgoCD

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: amanahfi-platform
spec:
  source:
    repoURL: https://github.com/amanahfi/amanahfi-platform
    path: k8s/overlays/production
    targetRevision: main
  destination:
    server: https://kubernetes.default.svc
    namespace: amanahfi-platform
```

## 🌍 Multi-Region Deployment

### Regional Considerations

- **UAE (Primary)**: Full feature set, primary data center
- **Saudi Arabia**: Compliance with SAMA regulations
- **Qatar**: QCB regulatory requirements
- **Kuwait, Bahrain, Oman**: Regional adaptations

### Data Residency

```yaml
# Node affinity for data residency
nodeAffinity:
  requiredDuringSchedulingIgnoredDuringExecution:
    nodeSelectorTerms:
    - matchExpressions:
      - key: topology.kubernetes.io/region
        operator: In
        values: ["uae-central-1"]
```

## 🔍 Troubleshooting

### Common Issues

1. **Pod Startup Failures**
   ```bash
   kubectl logs -f deployment/amanahfi-platform -n amanahfi-platform
   kubectl describe pod <pod-name> -n amanahfi-platform
   ```

2. **Database Connection Issues**
   ```bash
   kubectl exec -it <pod-name> -n amanahfi-platform -- nc -zv postgres 5432
   ```

3. **Configuration Issues**
   ```bash
   kubectl get configmap amanahfi-platform-config -n amanahfi-platform -o yaml
   ```

### Debug Mode

Enable debug logging:

```bash
kubectl patch configmap amanahfi-platform-config -n amanahfi-platform \
  --patch '{"data":{"LOGGING_LEVEL_COM_AMANAHFI":"DEBUG"}}'
kubectl rollout restart deployment/amanahfi-platform -n amanahfi-platform
```

## 📚 Additional Resources

- [AmanahFi Platform Documentation](https://docs.amanahfi.ae)
- [Islamic Finance Integration Guide](https://docs.amanahfi.ae/islamic-finance)
- [CBDC Integration Guide](https://docs.amanahfi.ae/cbdc-integration)
- [Security Best Practices](https://docs.amanahfi.ae/security)
- [Monitoring and Observability](https://docs.amanahfi.ae/monitoring)

## 🆘 Support

For technical support:

- **Email**: platform-support@amanahfi.ae
- **Slack**: #amanahfi-platform-support
- **Documentation**: https://docs.amanahfi.ae
- **Issues**: https://github.com/amanahfi/amanahfi-platform/issues

---

*Built with 💚 for Sharia-compliant financial innovation*