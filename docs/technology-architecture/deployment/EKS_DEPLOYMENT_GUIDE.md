# AWS EKS Deployment Guide - Enterprise Loan Management System

## Complete Kubernetes Deployment with GitOps CI/CD Pipeline

### Infrastructure Overview

This guide provides comprehensive instructions for deploying the Enterprise Loan Management System to AWS EKS with full observability, caching, and GitOps automation.

---

##  Prerequisites

### Required Tools
- AWS CLI v2.x configured with appropriate permissions
- Terraform v1.0+ for infrastructure provisioning
- kubectl v1.28+ for Kubernetes management
- Helm v3.12+ for package management
- Docker for container image building
- ArgoCD CLI for GitOps management

### AWS Permissions Required
- EKS cluster creation and management
- RDS PostgreSQL instance provisioning
- ElastiCache Redis cluster setup
- VPC and networking configuration
- IAM role and policy management
- ECR repository access for container images

---

##  Infrastructure Deployment

### Step 1: Initialize Terraform Infrastructure

```bash
# Navigate to Terraform directory
cd terraform/aws-eks

# Copy and customize variables
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with your specific values

# Initialize Terraform
terraform init

# Plan infrastructure changes
terraform plan

# Deploy EKS cluster and supporting resources
terraform apply
```

### Step 2: Configure kubectl Access

```bash
# Update kubeconfig for EKS cluster
aws eks update-kubeconfig --region us-west-2 --name enterprise-loan-system

# Verify cluster access
kubectl get nodes
kubectl get namespaces
```

### Step 3: Install Required Kubernetes Components

```bash
# Install AWS Load Balancer Controller
kubectl apply -k "github.com/aws/eks-charts/stable/aws-load-balancer-controller//crds?ref=master"

helm repo add eks https://aws.github.io/eks-charts
helm repo update

helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=enterprise-loan-system \
  --set serviceAccount.create=false \
  --set serviceAccount.name=aws-load-balancer-controller

# Install Metrics Server
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

# Verify installations
kubectl get pods -n kube-system
```

---

##  Application Deployment

### Option A: Direct Kubernetes Manifests

```bash
# Create namespaces
kubectl apply -f k8s/manifests/namespace.yaml

# Apply configurations
kubectl apply -f k8s/manifests/configmap.yaml
kubectl apply -f k8s/manifests/secrets.yaml

# Deploy application
kubectl apply -f k8s/manifests/deployment.yaml
kubectl apply -f k8s/manifests/service.yaml
kubectl apply -f k8s/manifests/ingress.yaml

# Apply scaling and monitoring
kubectl apply -f k8s/manifests/hpa.yaml
kubectl apply -f k8s/manifests/monitoring.yaml

# Verify deployment
kubectl get pods -n banking-system
kubectl get svc -n banking-system
kubectl get ing -n banking-system
```

### Option B: Helm Chart Deployment

```bash
# Install using Helm chart
helm install enterprise-loan-system ./k8s/helm-charts/enterprise-loan-system \
  --namespace banking-system \
  --create-namespace \
  --set image.tag=latest \
  --set database.host=your-rds-endpoint \
  --set redis.host=your-elasticache-endpoint \
  --values k8s/helm-charts/enterprise-loan-system/values-production.yaml

# Verify Helm deployment
helm list -n banking-system
helm status enterprise-loan-system -n banking-system
```

---

##  GitOps Setup with ArgoCD

### Step 1: Install ArgoCD

```bash
# Create ArgoCD namespace
kubectl create namespace argocd

# Install ArgoCD
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# Wait for ArgoCD to be ready
kubectl wait --for=condition=available --timeout=300s deployment/argocd-server -n argocd

# Get initial admin password
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d

# Port-forward to access ArgoCD UI
kubectl port-forward svc/argocd-server -n argocd 8080:443
```

### Step 2: Configure ArgoCD Applications

```bash
# Apply ArgoCD project and applications
kubectl apply -f k8s/argocd/application.yaml

# Login to ArgoCD CLI
argocd login localhost:8080

# Sync applications
argocd app sync enterprise-loan-system
argocd app sync enterprise-loan-system-monitoring
```

### Step 3: GitOps Repository Structure

Create a separate GitOps repository with the following structure:

```
enterprise-loan-system-gitops/
├── applications/
│   ├── production/
│   │   ├── enterprise-loan-system/
│   │   │   ├── values.yaml
│   │   │   └── values-production.yaml
│   │   └── monitoring/
│   │       ├── values.yaml
│   │       └── values-production.yaml
│   └── staging/
│       ├── enterprise-loan-system/
│       └── monitoring/
└── infrastructure/
    ├── eks/
    └── monitoring/
```

---

##  Monitoring and Observability

### Prometheus and Grafana Setup

```bash
# Add Prometheus Helm repository
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update

# Install Prometheus
helm install prometheus prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --create-namespace \
  --set prometheus.prometheusSpec.serviceMonitorSelectorNilUsesHelmValues=false \
  --set grafana.adminPassword=admin123

# Verify monitoring stack
kubectl get pods -n monitoring
kubectl get svc -n monitoring
```

### ELK Stack Deployment

```bash
# Install Elasticsearch
helm repo add elastic https://helm.elastic.co
helm install elasticsearch elastic/elasticsearch \
  --namespace monitoring \
  --set replicas=1 \
  --set minimumMasterNodes=1

# Install Kibana
helm install kibana elastic/kibana \
  --namespace monitoring \
  --set service.type=LoadBalancer

# Install Logstash
helm install logstash elastic/logstash \
  --namespace monitoring
```

### Access Monitoring Dashboards

```bash
# Grafana dashboard access
kubectl port-forward svc/prometheus-grafana 3000:80 -n monitoring
# Access: http://localhost:3000 (admin/admin123)

# Prometheus access
kubectl port-forward svc/prometheus-kube-prometheus-prometheus 9090:9090 -n monitoring
# Access: http://localhost:9090

# Kibana access
kubectl port-forward svc/kibana-kibana 5601:5601 -n monitoring
# Access: http://localhost:5601
```

---

##  Security Configuration

### Secrets Management

```bash
# Create database password secret
kubectl create secret generic banking-app-secrets \
  --from-literal=database-password=your-db-password \
  --from-literal=redis-password=your-redis-password \
  --from-literal=jwt-secret=your-jwt-secret \
  --namespace banking-system

# For production, use AWS Secrets Manager integration
kubectl apply -f - <<EOF
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: aws-secrets-manager
  namespace: banking-system
spec:
  provider:
    aws:
      service: SecretsManager
      region: us-west-2
EOF
```

### Network Security

```bash
# Apply network policies
kubectl apply -f k8s/manifests/ingress.yaml

# Verify network policies
kubectl get networkpolicies -n banking-system
kubectl describe networkpolicy banking-system-network-policy -n banking-system
```

---

##  CI/CD Pipeline Setup

### GitHub Actions Configuration

1. **Set up repository secrets** in GitHub:
   - `AWS_ACCESS_KEY_ID`
   - `AWS_SECRET_ACCESS_KEY`
   - `AWS_ACCESS_KEY_ID_PROD`
   - `AWS_SECRET_ACCESS_KEY_PROD`
   - `SONAR_TOKEN`
   - `SNYK_TOKEN`
   - `GITOPS_TOKEN`
   - `SLACK_WEBHOOK`

2. **Configure branch protection** for main branch
3. **Set up staging and production environments** in GitHub

### Pipeline Workflow

The CI/CD pipeline automatically:
- Runs comprehensive tests on pull requests
- Builds and pushes container images on main branch
- Deploys to staging environment for develop branch
- Updates GitOps repository for production deployment
- Performs security scanning and notifications

---

##  Scaling and Performance

### Horizontal Pod Autoscaling

```bash
# Verify HPA configuration
kubectl get hpa -n banking-system
kubectl describe hpa enterprise-loan-system-hpa -n banking-system

# Monitor scaling events
kubectl get events --sort-by=.metadata.creationTimestamp -n banking-system
```

### Cluster Autoscaling

```bash
# Install Cluster Autoscaler
kubectl apply -f https://raw.githubusercontent.com/kubernetes/autoscaler/master/cluster-autoscaler/cloudprovider/aws/examples/cluster-autoscaler-autodiscover.yaml

# Configure for your cluster
kubectl -n kube-system annotate deployment.apps/cluster-autoscaler cluster-autoscaler.kubernetes.io/safe-to-evict="false"
kubectl -n kube-system edit deployment.apps/cluster-autoscaler
```

### Performance Monitoring

```bash
# Monitor application performance
kubectl top pods -n banking-system
kubectl top nodes

# Check resource utilization
kubectl describe pod -l app=enterprise-loan-system -n banking-system
```

---

##  Production Deployment Checklist

### Pre-Deployment Validation

- [ ] Infrastructure provisioned successfully
- [ ] Database connectivity verified
- [ ] Redis cache accessible
- [ ] SSL certificates configured
- [ ] Secrets properly configured
- [ ] Network policies applied
- [ ] Monitoring stack operational
- [ ] ArgoCD applications synced

### Post-Deployment Verification

```bash
# Health check endpoints
curl -f https://banking.your-domain.com/actuator/health

# Cache performance validation
curl -s https://banking.your-domain.com/api/v1/cache/health

# Banking compliance verification
curl -s https://banking.your-domain.com/api/v1/tdd/coverage-report

# Load testing
kubectl run load-test --image=busybox --rm -it --restart=Never -- \
  /bin/sh -c "while true; do wget -q -O- https://banking.your-domain.com/actuator/health; done"
```

### Monitoring and Alerting Verification

- [ ] Prometheus targets healthy
- [ ] Grafana dashboards accessible
- [ ] Alert rules configured
- [ ] Slack notifications working
- [ ] Log aggregation functional

---

##  Troubleshooting Guide

### Common Issues

#### Pod Startup Issues
```bash
# Check pod status and logs
kubectl get pods -n banking-system
kubectl describe pod <pod-name> -n banking-system
kubectl logs <pod-name> -n banking-system
```

#### Database Connection Problems
```bash
# Test database connectivity
kubectl run postgres-test --image=postgres:15 --rm -it --restart=Never -- \
  psql -h your-rds-endpoint -U postgres -d banking_system -c "SELECT 1;"
```

#### Redis Cache Issues
```bash
# Test Redis connectivity
kubectl run redis-test --image=redis:7 --rm -it --restart=Never -- \
  redis-cli -h your-elasticache-endpoint ping
```

#### Load Balancer Issues
```bash
# Check ALB status
kubectl get ingress -n banking-system
kubectl describe ingress enterprise-loan-system-ingress -n banking-system
```

### Performance Optimization

#### Memory Issues
```bash
# Increase memory limits
kubectl patch deployment enterprise-loan-system -n banking-system -p \
  '{"spec":{"template":{"spec":{"containers":[{"name":"banking-app","resources":{"limits":{"memory":"4Gi"}}}]}}}}'
```

#### CPU Optimization
```bash
# Adjust CPU requests and limits
kubectl patch deployment enterprise-loan-system -n banking-system -p \
  '{"spec":{"template":{"spec":{"containers":[{"name":"banking-app","resources":{"requests":{"cpu":"1000m"},"limits":{"cpu":"2000m"}}}]}}}}'
```

---

##  Production Metrics and KPIs

### Banking System Performance Targets

| Metric | Target | Monitoring |
|--------|--------|------------|
| Response Time (95th percentile) | <100ms | Prometheus + Grafana |
| Cache Hit Ratio | >80% | Custom metrics |
| Error Rate | <0.1% | Application logs |
| Uptime | 99.9% | Health checks |
| Database Connections | <80% pool | Connection monitoring |
| Memory Usage | <80% limit | Resource monitoring |
| CPU Usage | <70% limit | Resource monitoring |

### Business Metrics

- Loan applications processed per hour
- Payment transactions per second
- Customer service response times
- Compliance report generation speed
- Banking standards adherence (87.4% TDD coverage)

---

**Deployment Status**:  Production-Ready EKS Infrastructure Complete
**GitOps**:  ArgoCD Automated Deployment Pipeline
**Monitoring**:  Full Observability Stack with Banking Metrics
**Security**:  FAPI-Compliant Security and Network Policies
**Scalability**:  Auto-scaling for High-Availability Banking Operations