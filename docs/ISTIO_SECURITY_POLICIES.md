# Enterprise Banking Istio Security Policies

## Overview

This document provides comprehensive documentation for the Enterprise Banking Istio Security Policies implementation. The security framework ensures FAPI 2.0 compliance, PCI DSS requirements, SOX controls, and GDPR data protection through a multi-layered security approach using Istio service mesh capabilities.

## Security Architecture

### Defense in Depth Strategy

1. **Network Layer Security** - Kubernetes NetworkPolicies
2. **Transport Layer Security** - Istio mTLS enforcement
3. **Application Layer Security** - Istio RBAC policies
4. **Identity and Access Management** - Service accounts and JWT validation
5. **Certificate Management** - Automated PKI with cert-manager

### Compliance Framework Mapping

| Framework | Implementation | Policies |
|-----------|----------------|----------|
| **FAPI 2.0** | mTLS + JWT + RBAC | All services |
| **PCI DSS** | Network isolation + encryption | Payment services |
| **SOX** | Audit logging + access controls | All financial services |
| **GDPR** | Data access controls + audit | Customer services |

## mTLS Configuration

### Strict mTLS Enforcement

All inter-service communication within the banking namespace operates under strict mTLS:

```yaml
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: banking-default-mtls
  namespace: banking
spec:
  mtls:
    mode: STRICT
```

### Service-Specific mTLS

Each banking service has dedicated mTLS configuration based on compliance requirements:

#### Payment Service (PCI DSS Critical)
```yaml
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: payment-service-mtls
  namespace: banking
spec:
  selector:
    matchLabels:
      app: payment-service
  mtls:
    mode: STRICT
```

#### Customer Service (GDPR Compliant)
```yaml
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: customer-service-mtls
  namespace: banking
spec:
  selector:
    matchLabels:
      app: customer-service
  mtls:
    mode: STRICT
```

### DestinationRules for mTLS

Traffic policies ensure proper mTLS configuration:

```yaml
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: banking-services-mtls
  namespace: banking
spec:
  host: "*.banking.svc.cluster.local"
  trafficPolicy:
    tls:
      mode: ISTIO_MUTUAL
    connectionPool:
      tcp:
        maxConnections: 100
      http:
        http2MaxRequests: 1000
        h2UpgradePolicy: UPGRADE
```

## RBAC Policies

### Zero Trust Authorization

The security model implements a default-deny approach with explicit allow rules:

```yaml
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: banking-deny-all-default
  namespace: banking
spec:
  {}  # Empty spec denies all traffic by default
```

### Service-Specific Authorization

#### Loan Service RBAC
```yaml
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: loan-service-rbac
  namespace: banking
spec:
  selector:
    matchLabels:
      app: loan-service
  action: ALLOW
  rules:
  - from:
    - source:
        principals: ["cluster.local/ns/istio-system/sa/istio-ingressgateway-service-account"]
    to:
    - operation:
        paths: ["/api/v1/loans*"]
        methods: ["GET", "POST", "PUT", "DELETE"]
    when:
    - key: request.auth.claims[scope]
      values: ["loans:read", "loans:write", "loans:admin"]
```

#### Payment Service RBAC (PCI DSS Compliant)
```yaml
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: payment-service-rbac
  namespace: banking
spec:
  selector:
    matchLabels:
      app: payment-service
  action: ALLOW
  rules:
  - from:
    - source:
        principals: ["cluster.local/ns/istio-system/sa/istio-ingressgateway-service-account"]
    to:
    - operation:
        paths: ["/api/v1/payments*", "/open-banking/v3.1/pisp*"]
        methods: ["GET", "POST", "PUT"]
    when:
    - key: request.auth.claims[scope]
      values: ["payments:read", "payments:write", "openbanking:pisp"]
    - key: request.headers[x-idempotency-key]
      notValues: [""]
```

### JWT Validation

All external API access requires valid JWT tokens with proper claims:

- **Required Claims**: `iss`, `sub`, `aud`, `exp`, `iat`, `scope`
- **Scope Validation**: Service-specific scopes enforced
- **Token Expiry**: Maximum 1 hour token lifetime
- **Algorithm Whitelist**: RS256, PS256, ES256

### Open Banking FAPI Compliance

Special RBAC rules for Open Banking APIs:

```yaml
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: open-banking-rbac
  namespace: banking
spec:
  action: ALLOW
  rules:
  # Account Information Service Provider (AISP)
  - when:
    - key: request.auth.claims[scope]
      values: ["accounts", "openbanking:aisp"]
    - key: request.headers[x-fapi-financial-id]
      notValues: [""]
  # Payment Initiation Service Provider (PISP)
  - when:
    - key: request.auth.claims[scope]
      values: ["payments", "openbanking:pisp"]
    - key: request.headers[x-fapi-financial-id]
      notValues: [""]
    - key: request.headers[x-idempotency-key]
      notValues: [""]
```

## Network Policies

### Default Deny Network Policy

Kubernetes NetworkPolicies provide network-layer security:

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: banking-default-deny-all
  namespace: banking
spec:
  podSelector: {}
  policyTypes:
  - Ingress
  - Egress
```

### Service-Specific Network Rules

#### Payment Service (PCI DSS Network Segmentation)
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: payment-service-network-policy
  namespace: banking
spec:
  podSelector:
    matchLabels:
      app: payment-service
  policyTypes:
  - Ingress
  - Egress
  ingress:
  # Only from gateway
  - from:
    - namespaceSelector:
        matchLabels:
          name: istio-system
      podSelector:
        matchLabels:
          app: istio-ingressgateway
    ports:
    - protocol: TCP
      port: 8080
  egress:
  # To audit and compliance services only
  - to:
    - podSelector:
        matchLabels:
          app: audit-service
    ports:
    - protocol: TCP
      port: 8080
```

#### Customer Service (GDPR Network Controls)
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: customer-service-network-policy
  namespace: banking
spec:
  podSelector:
    matchLabels:
      app: customer-service
  policyTypes:
  - Ingress
  - Egress
  ingress:
  # From gateway and authorized services
  - from:
    - namespaceSelector:
        matchLabels:
          name: istio-system
  - from:
    - podSelector:
        matchLabels:
          app: loan-service
  egress:
  # To audit service for GDPR audit trail
  - to:
    - podSelector:
        matchLabels:
          app: audit-service
```

### DNS and Infrastructure Access

All pods require DNS resolution and Istio system communication:

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-dns-access
  namespace: banking
spec:
  podSelector: {}
  policyTypes:
  - Egress
  egress:
  - to:
    - namespaceSelector:
        matchLabels:
          name: kube-system
    ports:
    - protocol: UDP
      port: 53
    - protocol: TCP
      port: 53
```

## Service Accounts and RBAC

### Dedicated Service Accounts

Each service operates with its own service account:

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: payment-service
  namespace: banking
  labels:
    app: payment-service
    compliance: pci-dss
    security-level: critical
automountServiceAccountToken: true
```

### RBAC Bindings

Minimal permissions are granted to each service:

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: banking-service-reader
  namespace: banking
rules:
- apiGroups: [""]
  resources: ["configmaps", "secrets"]
  verbs: ["get", "list", "watch"]
- apiGroups: [""]
  resources: ["pods"]
  verbs: ["get", "list"]
```

### Cross-Namespace Access

Compliance services require broader access:

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: compliance-service-reader
rules:
- apiGroups: [""]
  resources: ["namespaces", "pods", "services"]
  verbs: ["get", "list", "watch"]
- apiGroups: ["security.istio.io"]
  resources: ["authorizationpolicies", "peerauthentications"]
  verbs: ["get", "list", "watch"]
```

## Certificate Management

### PKI Architecture

The banking PKI uses a three-tier certificate authority:

1. **Root CA** - Self-signed, 10-year validity
2. **Intermediate CA** - Banking services CA, 5-year validity
3. **Service Certificates** - Individual service certs, 1-year validity

### Automated Certificate Management

cert-manager handles all certificate lifecycle:

```yaml
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: payment-service-cert
  namespace: banking
spec:
  secretName: payment-service-tls
  duration: 8760h # 1 year
  renewBefore: 720h # 30 days
  commonName: payment-service.banking.svc.cluster.local
  dnsNames:
  - payment-service.banking.svc.cluster.local
  issuerRef:
    name: banking-ca-issuer
    kind: Issuer
  usages:
  - digital signature
  - key encipherment
  - server auth
  - client auth
```

### External Certificate Integration

Production deployments use Let's Encrypt for public-facing APIs:

```yaml
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: security@enterprisebank.com
    privateKeySecretRef:
      name: letsencrypt-prod-account-key
    solvers:
    - http01:
        ingress:
          class: istio
```

## Security Monitoring

### Key Security Metrics

Monitor these critical security indicators:

```promql
# mTLS Success Rate
sum(rate(istio_requests_total{security_policy="mutual_tls"}[5m])) by (source_service_name, destination_service_name)

# Authorization Policy Denials
sum(rate(istio_requests_total{response_code="403"}[5m])) by (destination_service_name)

# Certificate Expiration
cert_manager_certificate_expiration_timestamp_seconds - time() < 86400 * 30
```

### Security Alerts

Critical alerts for security violations:

```yaml
groups:
- name: banking.security
  rules:
  - alert: mTLSConnectionFailure
    expr: rate(istio_requests_total{security_policy!="mutual_tls"}[5m]) > 0
    for: 1m
    labels:
      severity: critical
    annotations:
      summary: "Non-mTLS connection detected"
      
  - alert: AuthorizationPolicyViolation
    expr: rate(istio_requests_total{response_code="403"}[5m]) > 10
    for: 2m
    labels:
      severity: warning
    annotations:
      summary: "High rate of authorization failures"
```

## Deployment

### Prerequisites

1. Kubernetes cluster with Istio installed
2. cert-manager for certificate management
3. Prometheus for monitoring
4. Proper RBAC permissions

### Quick Deployment

```bash
cd scripts/istio
./deploy-security-policies.sh
```

### Manual Deployment Steps

```bash
# 1. Deploy service accounts
kubectl apply -f k8s/istio/security/service-accounts.yaml

# 2. Deploy mTLS policies
kubectl apply -f k8s/istio/security/mtls-policies.yaml

# 3. Deploy RBAC policies
kubectl apply -f k8s/istio/security/rbac-policies.yaml

# 4. Deploy network policies
kubectl apply -f k8s/istio/security/network-policies.yaml

# 5. Deploy certificates
kubectl apply -f k8s/istio/security/security-certificates.yaml
```

### Verification Commands

```bash
# Check mTLS status
istioctl authn tls-check -n banking

# Verify authorization policies
kubectl get authorizationpolicy -n banking

# Check network policies
kubectl get networkpolicy -n banking

# Verify certificates
kubectl get certificate -n banking
```

## Security Testing

### mTLS Testing

```bash
# Test inter-service mTLS
kubectl exec deployment/loan-service -n banking -- curl -v https://payment-service:8080/health

# Should show successful TLS handshake with client certificate
```

### RBAC Testing

```bash
# Test unauthorized access (should fail)
kubectl run test-pod --image=curlimages/curl -n banking -- curl payment-service:8080/api/v1/payments

# Test authorized access with JWT
kubectl exec test-pod -n banking -- curl -H "Authorization: Bearer $JWT_TOKEN" payment-service:8080/api/v1/payments
```

### Network Policy Testing

```bash
# Test denied network access
kubectl exec deployment/customer-service -n banking -- curl external-service:8080

# Should timeout or be rejected
```

## Troubleshooting

### Common Issues

1. **mTLS Connection Failures**
   - Check certificate validity
   - Verify PeerAuthentication policies
   - Ensure DestinationRules are configured

2. **Authorization Denied (403)**
   - Verify JWT token claims
   - Check AuthorizationPolicy rules
   - Validate service account configuration

3. **Network Connectivity Issues**
   - Review NetworkPolicy rules
   - Check namespace labels
   - Verify DNS resolution

### Debug Commands

```bash
# Check Istio proxy configuration
istioctl proxy-config cluster <pod-name> -n banking

# View authorization policy evaluation
istioctl proxy-config authz <pod-name> -n banking

# Check certificate details
kubectl describe certificate <cert-name> -n banking

# View network policy details
kubectl describe networkpolicy <policy-name> -n banking
```

## Compliance Validation

### FAPI 2.0 Requirements

- ✅ mTLS for all API communications
- ✅ JWT Bearer token validation
- ✅ Request signature validation (via Envoy filters)
- ✅ Proper error responses with interaction IDs
- ✅ Rate limiting implementation

### PCI DSS Requirements

- ✅ Network segmentation (Requirement 1)
- ✅ Encrypted transmission (Requirement 4)
- ✅ Access control implementation (Requirement 7)
- ✅ Regular monitoring (Requirement 10)
- ✅ Security testing (Requirement 11)

### SOX Compliance

- ✅ Role-based access control
- ✅ Audit trail maintenance
- ✅ Separation of duties
- ✅ Change management controls
- ✅ Access review processes

### GDPR Compliance

- ✅ Data access controls
- ✅ Audit logging for personal data access
- ✅ Data encryption in transit
- ✅ Access right management
- ✅ Data breach detection capabilities

## Best Practices

### Security Hardening

1. **Regular Security Audits**
   - Weekly policy review
   - Monthly penetration testing
   - Quarterly compliance assessment

2. **Certificate Management**
   - Automated certificate rotation
   - Certificate expiry monitoring
   - Secure key storage

3. **Access Control**
   - Principle of least privilege
   - Regular access reviews
   - Automated access provisioning

4. **Monitoring and Alerting**
   - Real-time security monitoring
   - Automated incident response
   - Comprehensive audit logging

### Operational Procedures

1. **Policy Updates**
   - Test in staging environment first
   - Gradual rollout to production
   - Document all changes

2. **Incident Response**
   - Immediate security team notification
   - Automated policy enforcement
   - Post-incident review process

3. **Compliance Reporting**
   - Automated compliance dashboards
   - Regular audit reports
   - Regulatory submission automation

## Support and Documentation

### Resources

- [Istio Security Documentation](https://istio.io/latest/docs/concepts/security/)
- [FAPI 2.0 Specification](https://openid.net/specs/fapi-2_0-security-profile.html)
- [PCI DSS Requirements](https://www.pcisecuritystandards.org/)
- [Kubernetes Network Policies](https://kubernetes.io/docs/concepts/services-networking/network-policies/)

### Contact Information

- **Security Team**: security@enterprisebank.com
- **DevOps Team**: devops@enterprisebank.com
- **Compliance Team**: compliance@enterprisebank.com
- **Incident Response**: soc@enterprisebank.com

For security incidents or policy violations, contact the Security Operations Center immediately at soc@enterprisebank.com or the 24/7 hotline.