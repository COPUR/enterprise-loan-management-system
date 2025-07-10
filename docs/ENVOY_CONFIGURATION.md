# Enterprise Banking Envoy Proxy Configuration

## Overview

This document provides comprehensive documentation for the Enterprise Banking Envoy Proxy configuration, designed to meet FAPI 2.0 compliance requirements and provide secure, scalable API gateway functionality for banking services.

## Architecture

### Components

1. **Envoy Gateway** - Main API gateway handling external traffic
2. **Envoy Sidecars** - Service mesh proxies for inter-service communication
3. **Security Policies** - Istio-based security configurations
4. **FAPI Compliance Filters** - Custom filters for Open Banking compliance

### Security Features

- **FAPI 2.0 Compliance** - Full compliance with Financial-grade API standards
- **mTLS** - Mutual TLS for all inter-service communication
- **JWT Authentication** - Bearer token validation with Keycloak integration
- **Rate Limiting** - FAPI-compliant rate limiting policies
- **Request Signing** - Support for JWS request signatures
- **Audit Logging** - Comprehensive audit trail for compliance

## Configuration Files

### Core Configuration

- `envoy-base-config.yaml` - Base Envoy configuration template
- `envoy-sidecar-configmap.yaml` - Sidecar proxy configuration
- `envoy-gateway-deployment.yaml` - Gateway deployment and service
- `envoy-security-policies.yaml` - Istio security policies

### Deployment Scripts

- `deploy-envoy-configuration.sh` - Automated deployment script

## FAPI 2.0 Compliance

### Required Headers

The Envoy configuration enforces the following FAPI-required headers:

```http
x-fapi-interaction-id: <UUID>
x-fapi-auth-date: <ISO8601 timestamp>
x-fapi-customer-ip-address: <Client IP>
Authorization: Bearer <JWT token>
```

### Security Headers

Response headers automatically added for compliance:

```http
Cache-Control: no-store, no-cache, must-revalidate
Pragma: no-cache
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
Strict-Transport-Security: max-age=31536000; includeSubDomains
```

### Rate Limiting

FAPI-compliant rate limiting configuration:

- **Default**: 1000 requests per minute per client
- **Open Banking APIs**: 500 requests per minute per client
- **Burst capacity**: 10% above baseline
- **Headers**: `x-ratelimit-limit`, `x-ratelimit-remaining`

## API Routing

### Open Banking APIs

```yaml
# Account Information Services (AISP)
/open-banking/v3.1/aisp/* → account-information-service

# Payment Initiation Services (PISP)
/open-banking/v3.1/pisp/* → payment-initiation-service
```

### Core Banking APIs

```yaml
# Loan Management
/api/v1/loans/* → loan-management-service

# Customer Management  
/api/v1/customers/* → customer-management-service

# Payment Processing
/api/v1/payments/* → payment-processing-service

# AI Banking Services
/api/v1/ai/* → ai-banking-service
```

## Security Configuration

### mTLS Configuration

```yaml
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: banking-envoy-mtls
spec:
  mtls:
    mode: STRICT
```

### RBAC Policies

```yaml
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: banking-envoy-rbac
spec:
  rules:
  - from:
    - source:
        principals: ["cluster.local/ns/banking/sa/banking-api-client"]
    to:
    - operation:
        methods: ["GET", "POST", "PUT", "DELETE"]
    when:
    - key: request.headers[x-fapi-interaction-id]
      notValues: [""]
```

### JWT Authentication

```yaml
providers:
  banking_keycloak:
    issuer: https://keycloak.enterprisebank.com/realms/banking
    audiences:
    - banking-api
    - open-banking-api
    remote_jwks:
      uri: https://keycloak.enterprisebank.com/realms/banking/protocol/openid_connect/certs
```

## Circuit Breaker Configuration

### Connection Limits

```yaml
connectionPool:
  tcp:
    maxConnections: 200
    connectTimeout: 10s
  http:
    http1MaxPendingRequests: 100
    http2MaxRequests: 1000
    maxRequestsPerConnection: 10
    maxRetries: 3
```

### Outlier Detection

```yaml
outlierDetection:
  consecutiveGatewayErrors: 3
  consecutive5xxErrors: 5
  interval: 30s
  baseEjectionTime: 30s
  maxEjectionPercent: 50
```

## Load Balancing

### Algorithms

- **Round Robin** - Default for most services
- **Least Request** - For AI services with variable processing times
- **Random** - For health check endpoints

### Health Checks

```yaml
health_checks:
- timeout: 3s
  interval: 10s
  unhealthy_threshold: 3
  healthy_threshold: 2
  http_health_check:
    path: "/actuator/health"
```

## Monitoring and Observability

### Metrics Collection

Envoy automatically exposes metrics on port 9901:

```
/stats/prometheus - Prometheus format metrics
/stats - JSON format statistics
/config_dump - Current configuration
```

### Key Metrics

- `envoy_http_downstream_rq_total` - Total requests
- `envoy_http_downstream_rq_xx` - Response status codes
- `envoy_cluster_upstream_rq_retry` - Retry attempts
- `envoy_cluster_upstream_rq_timeout` - Request timeouts

### Grafana Dashboard

The deployment includes a pre-configured Grafana dashboard with:

- Request rate and latency
- Error rate by service
- Circuit breaker status
- FAPI compliance metrics
- Security policy violations

## Deployment

### Prerequisites

1. Kubernetes cluster with Istio installed
2. Banking namespace created and labeled
3. TLS certificates for HTTPS endpoints
4. Keycloak authentication server configured

### Quick Deployment

```bash
cd scripts/envoy
./deploy-envoy-configuration.sh
```

### Manual Deployment

```bash
# Create namespace
kubectl create namespace banking
kubectl label namespace banking istio-injection=enabled

# Deploy configurations
kubectl apply -f k8s/envoy/envoy-sidecar-configmap.yaml
kubectl apply -f k8s/envoy/envoy-gateway-deployment.yaml
kubectl apply -f k8s/envoy/envoy-security-policies.yaml

# Verify deployment
kubectl get pods -n banking -l app=banking-envoy-gateway
```

## TLS Configuration

### Certificate Requirements

- **Subject**: CN=api.enterprisebank.com
- **SAN**: api.enterprisebank.com, openbanking.enterprisebank.com, *.enterprisebank.com
- **Key Usage**: Digital Signature, Key Encipherment
- **Extended Key Usage**: Server Authentication

### Certificate Rotation

Certificates are automatically reloaded when secrets are updated:

```bash
# Update certificate
kubectl create secret tls banking-tls-secret \
  --cert=new-cert.pem \
  --key=new-key.pem \
  --namespace=istio-system \
  --dry-run=client -o yaml | kubectl apply -f -

# Restart gateway pods to reload
kubectl rollout restart deployment/banking-envoy-gateway -n banking
```

## Troubleshooting

### Common Issues

1. **503 Service Unavailable**
   - Check backend service health
   - Verify service mesh connectivity
   - Check circuit breaker status

2. **401 Unauthorized**
   - Verify JWT token validity
   - Check Keycloak configuration
   - Validate FAPI headers

3. **429 Too Many Requests**
   - Review rate limiting configuration
   - Check client request patterns
   - Adjust rate limits if needed

### Debug Commands

```bash
# Check Envoy configuration
kubectl exec -n banking deployment/banking-envoy-gateway \
  -c envoy-gateway -- curl -s http://localhost:9901/config_dump

# View access logs
kubectl logs -n banking deployment/banking-envoy-gateway \
  -c envoy-gateway --follow

# Check cluster status
kubectl exec -n banking deployment/banking-envoy-gateway \
  -c envoy-gateway -- curl -s http://localhost:9901/clusters

# View listener configuration
kubectl exec -n banking deployment/banking-envoy-gateway \
  -c envoy-gateway -- curl -s http://localhost:9901/listeners
```

### Log Analysis

Access logs format:
```
[timestamp] "method path protocol" status_code flags bytes_received bytes_sent
duration upstream_time client_ip user_agent request_id authority upstream
fapi_interaction=interaction_id customer_id=customer compliance_level=level
```

## Performance Tuning

### Connection Pooling

```yaml
upstream_connection_options:
  tcp_keepalive:
    keepalive_probes: 3
    keepalive_time: 30
    keepalive_interval: 5
```

### Buffer Limits

```yaml
max_request_bytes: 5242880  # 5MB for banking APIs
```

### Timeout Configuration

```yaml
route:
  timeout: 30s  # Default timeout
  retry_policy:
    num_retries: 3
    per_try_timeout: 10s
```

## Security Best Practices

1. **Regular Certificate Rotation** - Every 90 days
2. **Rate Limit Monitoring** - Alert on threshold breaches
3. **Access Log Analysis** - Daily security reviews
4. **Configuration Audits** - Weekly compliance checks
5. **Penetration Testing** - Quarterly security assessments

## Compliance Verification

### FAPI 2.0 Checklist

- ✅ TLS 1.3 for all communications
- ✅ JWT Bearer tokens with proper validation
- ✅ Required FAPI headers enforced
- ✅ Rate limiting per FAPI guidelines
- ✅ Audit logging for all requests
- ✅ Proper error responses with interaction IDs
- ✅ CORS configuration for browser clients

### PCI DSS Requirements

- ✅ Encrypted transmission of cardholder data
- ✅ Strong access controls
- ✅ Regular monitoring and testing
- ✅ Secure network architecture
- ✅ Vulnerability management

## Support

For issues or questions:

1. Check the troubleshooting section
2. Review Envoy and Istio documentation
3. Contact the DevOps team
4. File an issue in the project repository

## Updates and Maintenance

### Regular Tasks

- Weekly configuration reviews
- Monthly security audits
- Quarterly performance assessments
- Annual compliance certifications

### Version Updates

Follow semantic versioning for configuration changes:
- **Patch** (x.x.1): Bug fixes, security patches
- **Minor** (x.1.x): New features, configuration enhancements  
- **Major** (1.x.x): Breaking changes, major refactoring