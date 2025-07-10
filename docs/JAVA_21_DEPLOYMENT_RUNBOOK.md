# Java 21 Deployment Runbook

## Overview

This runbook provides step-by-step procedures for deploying, monitoring, and troubleshooting the Java 21 migration of the Enterprise Loan Management System.

## Table of Contents

1. [Pre-Deployment Checklist](#pre-deployment-checklist)
2. [Deployment Procedures](#deployment-procedures)
3. [Health Verification](#health-verification)
4. [Performance Validation](#performance-validation)
5. [Rollback Procedures](#rollback-procedures)
6. [Troubleshooting Guide](#troubleshooting-guide)
7. [Emergency Contacts](#emergency-contacts)

## Pre-Deployment Checklist

### System Requirements

- [ ] Java 21 installed on all deployment targets
- [ ] Docker version 24.0+ with buildx support
- [ ] Kubernetes 1.28+ cluster available
- [ ] Helm 3.12+ installed
- [ ] kubectl configured with proper permissions
- [ ] Database backup completed
- [ ] Redis cache cleared
- [ ] Load balancer configurations updated

### Environment Verification

```bash
# Verify Java version
java --version
# Expected: openjdk 21.0.1 2023-10-17

# Verify Docker
docker --version
docker buildx version

# Verify Kubernetes access
kubectl cluster-info
kubectl auth can-i create deployments --namespace=loan-management

# Verify Helm
helm version
```

### Pre-Deployment Tests

```bash
# Run integration tests
./gradlew integrationTest -Pjava21.features.enabled=true

# Run performance benchmarks
./gradlew performanceTest -Pjava21.benchmarks=true

# Verify build
./gradlew clean build -Pjava21.optimizations=true
```

## Deployment Procedures

### 1. Development Environment

```bash
# Set environment
export DEPLOYMENT_ENV=development
export DOCKER_REGISTRY=dev-registry.bank.internal:5000

# Run deployment script
./scripts/deployment/deploy-java21.sh \
  --env development \
  --registry $DOCKER_REGISTRY

# Verify deployment
kubectl get pods -n loan-management -l version=java21
kubectl logs -n loan-management -l version=java21 --tail=100
```

### 2. Staging Environment

```bash
# Set environment
export DEPLOYMENT_ENV=staging
export DOCKER_REGISTRY=staging-registry.bank.internal:5000

# Database migration
kubectl apply -f k8s/jobs/db-migration-java21.yaml
kubectl wait --for=condition=complete job/db-migration-java21 -n loan-management

# Deploy application
./scripts/deployment/deploy-java21.sh \
  --env staging \
  --registry $DOCKER_REGISTRY

# Run smoke tests
./scripts/test/smoke-tests-java21.sh --env staging
```

### 3. Production Environment

#### Blue-Green Deployment

```bash
# Set environment
export DEPLOYMENT_ENV=production
export DOCKER_REGISTRY=prod-registry.bank.internal:5000

# Deploy to green environment
helm upgrade --install loan-management-green \
  ./k8s/helm-charts/enterprise-loan-system \
  --namespace loan-management \
  --values ./k8s/helm-charts/enterprise-loan-system/values-java21-prod.yaml \
  --set deployment.color=green \
  --set image.tag=java21-$(git rev-parse --short HEAD)

# Verify green deployment
kubectl wait --for=condition=ready pod -l app=loan-management,color=green -n loan-management

# Run health checks
./scripts/health/health-check-java21.sh --color green

# Switch traffic to green
kubectl patch service loan-management-service -n loan-management \
  -p '{"spec":{"selector":{"color":"green"}}}'

# Monitor for 15 minutes
./scripts/monitoring/monitor-deployment.sh --duration 15m

# If successful, scale down blue
kubectl scale deployment loan-management-blue -n loan-management --replicas=0
```

#### Canary Deployment (Alternative)

```bash
# Deploy canary version (10% traffic)
helm upgrade --install loan-management-canary \
  ./k8s/helm-charts/enterprise-loan-system \
  --namespace loan-management \
  --values ./k8s/helm-charts/enterprise-loan-system/values-java21-canary.yaml \
  --set canary.enabled=true \
  --set canary.weight=10

# Monitor canary metrics
./scripts/monitoring/canary-analysis.sh --duration 30m

# Gradually increase traffic
for weight in 25 50 75 100; do
  kubectl patch virtualservice loan-management -n loan-management \
    --type merge \
    -p '{"spec":{"http":[{"weight":'$weight'}]}}'
  
  echo "Traffic at $weight%, monitoring for 10 minutes..."
  sleep 600
  
  # Check error rate
  ERROR_RATE=$(./scripts/monitoring/get-error-rate.sh)
  if (( $(echo "$ERROR_RATE > 0.05" | bc -l) )); then
    echo "Error rate too high: $ERROR_RATE"
    ./scripts/deployment/rollback-canary.sh
    exit 1
  fi
done
```

## Health Verification

### Application Health Checks

```bash
# Get service endpoint
SERVICE_URL=$(kubectl get service loan-management-service -n loan-management -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

# Basic health check
curl -f http://$SERVICE_URL:8080/actuator/health

# Detailed health check
curl -f http://$SERVICE_URL:8080/actuator/health | jq '.components'

# Java 21 specific checks
curl -f http://$SERVICE_URL:8080/actuator/health/java21 | jq .
```

### Virtual Threads Verification

```bash
# Check Virtual Threads metrics
curl -s http://$SERVICE_URL:8080/actuator/metrics/virtual.threads.active | jq .

# Monitor Virtual Thread creation rate
watch -n 5 'curl -s http://$SERVICE_URL:8080/actuator/metrics/virtual.threads.created | jq .measurements[0].value'

# Verify thread pool configuration
curl -s http://$SERVICE_URL:8080/actuator/configprops | jq '.contexts[].beans | to_entries[] | select(.key | contains("threading"))'
```

### Pattern Matching Performance

```bash
# Check pattern matching metrics
curl -s http://$SERVICE_URL:8080/actuator/metrics/pattern.matching.operations | jq .

# Monitor pattern matching latency
curl -s http://$SERVICE_URL:8080/actuator/metrics/pattern.matching.latency | jq '.measurements[] | select(.statistic=="P99")'
```

## Performance Validation

### Load Testing

```bash
# Run performance test suite
./scripts/performance/load-test-java21.sh \
  --target $SERVICE_URL \
  --duration 30m \
  --users 1000 \
  --ramp-up 5m

# Analyze results
./scripts/performance/analyze-results.sh \
  --baseline pre-java21-baseline.json \
  --current java21-results.json
```

### Expected Performance Metrics

| Metric | Target | Alert Threshold |
|--------|--------|-----------------|
| Payment Processing | >1,200/sec | <800/sec |
| Loan Processing | >30/sec | <20/sec |
| Response Time P95 | <500ms | >1000ms |
| Error Rate | <1% | >3% |
| CPU Usage | <70% | >85% |
| Memory Usage | <6GB | >8GB |
| Virtual Threads Active | <8,000 | >9,500 |

### Performance Monitoring Commands

```bash
# Real-time performance dashboard
./scripts/monitoring/performance-dashboard.sh

# Generate performance report
./scripts/monitoring/generate-performance-report.sh \
  --start "$(date -d '1 hour ago' -Iseconds)" \
  --end "$(date -Iseconds)" \
  --output performance-report-$(date +%Y%m%d-%H%M%S).pdf
```

## Rollback Procedures

### Immediate Rollback (< 5 minutes)

```bash
# For blue-green deployment
kubectl patch service loan-management-service -n loan-management \
  -p '{"spec":{"selector":{"color":"blue"}}}'

# Scale up blue deployment
kubectl scale deployment loan-management-blue -n loan-management --replicas=3

# Verify rollback
./scripts/health/health-check-java21.sh --color blue
```

### Standard Rollback

```bash
# Helm rollback
helm rollback loan-management -n loan-management

# Verify rollback
helm status loan-management -n loan-management

# Check application version
kubectl get deployment loan-management -n loan-management \
  -o jsonpath='{.spec.template.spec.containers[0].image}'
```

### Database Rollback (if needed)

```bash
# Only if database migration was performed
kubectl apply -f k8s/jobs/db-rollback-java21.yaml
kubectl wait --for=condition=complete job/db-rollback-java21 -n loan-management
```

## Troubleshooting Guide

### Virtual Threads Issues

#### Symptom: Virtual threads not being created

```bash
# Check JVM flags
kubectl exec -it deployment/loan-management -n loan-management -- \
  java -XX:+PrintFlagsFinal -version | grep Virtual

# Check thread pool configuration
kubectl logs -n loan-management deployment/loan-management | \
  grep -E "virtual.thread|VirtualThread"

# Solution: Update JVM options
kubectl set env deployment/loan-management -n loan-management \
  JAVA_OPTS="-XX:+UseZGC -Xms2g -Xmx8g --enable-preview"
```

#### Symptom: High memory usage

```bash
# Generate heap dump
kubectl exec -it deployment/loan-management -n loan-management -- \
  jcmd 1 GC.heap_dump /tmp/heapdump.hprof

# Copy heap dump locally
kubectl cp loan-management-pod:/tmp/heapdump.hprof ./heapdump.hprof

# Analyze with MAT or jhat
jhat -port 7000 heapdump.hprof
```

### Pattern Matching Issues

#### Symptom: Pattern matching compilation errors

```bash
# Check Java version in container
kubectl exec -it deployment/loan-management -n loan-management -- java --version

# Check preview features
kubectl exec -it deployment/loan-management -n loan-management -- \
  java --list-modules | grep jdk.compiler

# Solution: Ensure --enable-preview is set
kubectl patch deployment loan-management -n loan-management --type json \
  -p='[{"op": "add", "path": "/spec/template/spec/containers/0/env/-", \
       "value": {"name": "_JAVA_OPTIONS", "value": "--enable-preview"}}]'
```

### Performance Issues

#### Symptom: Slower than expected performance

```bash
# Enable detailed GC logging
kubectl set env deployment/loan-management -n loan-management \
  JAVA_OPTS="$JAVA_OPTS -Xlog:gc*:file=/app/logs/gc.log"

# Monitor GC activity
kubectl exec -it deployment/loan-management -n loan-management -- \
  tail -f /app/logs/gc.log

# Check thread pool saturation
for pool in loan-processing payment-processing risk-assessment; do
  echo "Pool: $pool"
  curl -s http://$SERVICE_URL:8080/actuator/metrics/thread.pool.$pool | jq .
done
```

### Common Issues and Solutions

| Issue | Check Command | Solution |
|-------|---------------|----------|
| Pod CrashLoopBackOff | `kubectl describe pod <pod-name>` | Check logs, increase memory limits |
| High CPU usage | `kubectl top pods -n loan-management` | Scale horizontally, optimize code |
| Database connection errors | `kubectl logs <pod> | grep -i connection` | Check connection pool settings |
| Redis timeout | `kubectl exec <pod> -- redis-cli ping` | Increase timeout, check Redis health |
| API latency | `curl -w "@curl-format.txt" -o /dev/null -s $URL` | Enable Virtual Threads, check DB queries |

## Emergency Contacts

### On-Call Rotation

| Role | Primary | Secondary | Escalation |
|------|---------|-----------|------------|
| Platform Lead | John Smith (+1-555-0100) | Jane Doe (+1-555-0101) | CTO |
| Java Expert | Bob Wilson (+1-555-0102) | Alice Brown (+1-555-0103) | Platform Lead |
| Database Admin | Charlie Davis (+1-555-0104) | Eve Johnson (+1-555-0105) | Platform Lead |
| Security Lead | Frank Miller (+1-555-0106) | Grace Lee (+1-555-0107) | CISO |

### Escalation Procedure

1. **Severity 1** (Production Down): Page on-call immediately
2. **Severity 2** (Degraded Performance): Alert on-call within 15 minutes
3. **Severity 3** (Minor Issues): Create ticket, notify during business hours

### Communication Channels

- **Slack**: #loan-management-prod
- **PagerDuty**: loan-management-java21
- **War Room**: https://meet.bank.internal/java21-warroom
- **Status Page**: https://status.bank.internal

## Post-Deployment Checklist

- [ ] All health checks passing
- [ ] Performance metrics within expected range
- [ ] No increase in error rates
- [ ] Virtual Threads metrics stable
- [ ] Pattern matching operations successful
- [ ] Database connections healthy
- [ ] Cache hit rates normal
- [ ] Security scans passed
- [ ] Monitoring alerts configured
- [ ] Documentation updated
- [ ] Team notified of successful deployment
- [ ] Post-deployment review scheduled

---

**Last Updated**: $(date)  
**Version**: 1.0  
**Next Review**: Quarterly