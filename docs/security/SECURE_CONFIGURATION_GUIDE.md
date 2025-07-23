# Enterprise Banking System - Secure Configuration Guide

## CRITICAL SECURITY NOTICE

**🔒 SECURITY VULNERABILITY FIXES IMPLEMENTED**

This document outlines the secure configuration practices for the Enterprise Banking System following the remediation of critical security vulnerabilities that exposed authentication secrets, database credentials, and OAuth client secrets.

## Security Vulnerabilities Fixed

### 1. **OAuth Client Secret Exposure**
- **Issue**: Hardcoded OAuth client secrets in configuration files
- **Files Fixed**: 
  - `src/main/resources/application.properties`
  - `src/main/resources/application-enterprise.yml`
  - `src/main/resources/application-enterprise-minimal.yml`
  - Docker Compose configurations
  - Kubernetes manifests

### 2. **Database Credential Exposure**
- **Issue**: Default database passwords in configuration
- **Files Fixed**:
  - All application configuration files
  - Microservices configuration

### 3. **JWT Secret Exposure**
- **Issue**: Hardcoded JWT secrets with predictable defaults
- **Files Fixed**: `src/main/resources/application-dev.yml`

### 4. **Kubernetes Secret Exposure**
- **Issue**: Base64 encoded secrets committed to version control
- **Files Fixed**:
  - `k8s/istio/banking-gateway.yaml`
  - `k8s/istio/security/service-accounts.yaml`
  - `k8s/keycloak/keycloak-deployment.yaml`

## Required Environment Variables

All sensitive configuration must now be provided via environment variables:

### Core Security Variables
```bash
# OAuth 2.1 Configuration
export KEYCLOAK_CLIENT_SECRET="your-secure-client-secret"

# Database Configuration
export DATABASE_PASSWORD="your-secure-database-password"
export PGPASSWORD="your-secure-postgres-password"

# Redis Configuration
export REDIS_PASSWORD="your-secure-redis-password"

# JWT Configuration
export JWT_SECRET="your-secure-jwt-secret-256-bits-minimum"

# API Keys
export OPENAI_API_KEY="your-openai-api-key"
```

### Keycloak Configuration
```bash
export KEYCLOAK_URL="https://keycloak.yourdomain.com"
export KEYCLOAK_REALM="banking-realm"
export KEYCLOAK_CLIENT_ID="banking-app"
export KEYCLOAK_CLIENT_SECRET="your-secure-keycloak-client-secret"
```

### Microservices Configuration
```bash
export CUSTOMER_SERVICE_URL="https://customer.internal.yourdomain.com"
export LOAN_SERVICE_URL="https://loan.internal.yourdomain.com"
export PAYMENT_SERVICE_URL="https://payment.internal.yourdomain.com"
```

## Deployment Security

### Local Development
1. Create a `.env.local` file (excluded from git):
```bash
cp .env.example .env.local
# Edit .env.local with your local credentials
```

2. Source the environment file:
```bash
source .env.local
```

### Docker Deployment
```bash
# Use environment file
docker-compose --env-file .env.local up

# Or set variables inline
KEYCLOAK_CLIENT_SECRET=your-secret \
DATABASE_PASSWORD=your-password \
docker-compose up
```

### Kubernetes Deployment

#### Option 1: Create Secrets Manually
```bash
# Create OAuth secret
kubectl create secret generic oauth-secrets \
  --from-literal=client-secret=your-secure-client-secret

# Create database secret
kubectl create secret generic database-secrets \
  --from-literal=password=your-secure-password

# Create JWT secret
kubectl create secret generic jwt-secrets \
  --from-literal=secret=your-secure-jwt-secret
```

#### Option 2: Use External Secret Operator (Recommended)
```yaml
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: banking-secret-store
spec:
  provider:
    aws:
      service: SecretsManager
      region: us-west-2
---
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: banking-oauth-secret
spec:
  secretStoreRef:
    name: banking-secret-store
    kind: SecretStore
  target:
    name: oauth-secrets
  data:
  - secretKey: client-secret
    remoteRef:
      key: banking/oauth/client-secret
```

### Production Security Checklist

- [ ] All secrets are stored in external secret management (AWS Secrets Manager, Azure Key Vault, etc.)
- [ ] No default values for sensitive configuration
- [ ] Environment variables are properly secured in deployment platform
- [ ] Secret rotation is implemented
- [ ] Audit logging is enabled for secret access
- [ ] Network policies restrict access to internal services
- [ ] mTLS is configured for all service-to-service communication

## Security Monitoring

### Required Monitoring
1. **Secret Access Auditing**: Monitor all secret retrievals
2. **Configuration Change Tracking**: Alert on any configuration modifications
3. **Failed Authentication Monitoring**: Track failed OAuth attempts
4. **Database Connection Monitoring**: Monitor database access patterns

### Alerting Rules
```yaml
# Example Prometheus alert for configuration exposure
- alert: ConfigurationSecretExposed
  expr: increase(config_secret_access_total[5m]) > 10
  for: 2m
  labels:
    severity: critical
  annotations:
    summary: "Potential configuration secret exposure detected"
```

## Compliance Notes

This configuration follows:
- **PCI DSS Requirements**: No cardholder data stored in plain text
- **FAPI 2.0 Security Profile**: OAuth 2.1 with PKCE and DPoP
- **NIST Cybersecurity Framework**: Secure configuration management
- **ISO 27001**: Information security management

## Emergency Response

If secrets are compromised:
1. **Immediate**: Rotate all affected secrets
2. **Short-term**: Audit access logs for unauthorized usage
3. **Long-term**: Review and enhance secret management practices

## Support

For security-related configuration issues:
- Security Team: security@yourbank.com
- DevSecOps Team: devsecops@yourbank.com
- Emergency: security-emergency@yourbank.com

---

**Last Updated**: December 2024
**Classification**: Internal - Security Documentation
**Review Frequency**: Quarterly