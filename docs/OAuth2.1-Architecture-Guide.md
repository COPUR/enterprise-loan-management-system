# OAuth2.1 Architecture Guide
## Enterprise Banking System - Authentication, Authorization & Audit

### Table of Contents
1. [Overview](#overview)
2. [Architecture Components](#architecture-components)
3. [Authentication Flow](#authentication-flow)
4. [Authorization Model](#authorization-model)
5. [Security Features](#security-features)
6. [Implementation Details](#implementation-details)
7. [Compliance & Audit](#compliance--audit)
8. [Monitoring & Observability](#monitoring--observability)

---

## Overview

The Enterprise Banking System implements a comprehensive OAuth2.1 authentication and authorization architecture that meets banking industry compliance requirements. The system provides multi-layered security with identity federation, role-based access control, and comprehensive audit capabilities.

### Key Features

- **OAuth2.1 Compliance** with PKCE (Proof Key for Code Exchange)
- **Multi-layered Authorization** (Keycloak + LDAP + Party Data Management)
- **Banking Compliance** (FAPI, PCI DSS, SOX, GDPR)
- **Real-time Audit** and compliance reporting
- **Temporal Access Control** with role expiration and review cycles
- **12-Factor App** methodology for cloud-native deployment

![OAuth2.1 Architecture Overview](generated-diagrams/OAuth2.1%20Architecture%20Overview.svg)

---

## Architecture Components

### 1. Identity & Access Management Layer

#### Keycloak OAuth2.1 Server
- **Purpose**: Central authorization server implementing OAuth2.1 standard
- **Features**:
  - Banking realm configuration (`banking-realm`)
  - Authorization Code Flow with PKCE
  - JWT token management with RS256 signing
  - Session management and brute force protection
  - Event-driven audit logging
  - User federation with LDAP

#### LDAP Identity Provider
- **Purpose**: Enterprise directory for user identity storage
- **Features**:
  - OpenLDAP 1.5.0 with banking organization structure
  - TLS encryption for secure communication
  - Group membership management
  - Department and role mapping
  - Read-only service account for security

#### Party Data Management System
- **Purpose**: Authoritative source for business roles and permissions
- **Features**:
  - Temporal role assignments with effective dates
  - Authority level management (1-10 scale)
  - Monetary limit enforcement
  - Business unit and geographic scoping
  - Compliance tracking and review cycles

### 2. Application Services Layer

#### Banking Application (OAuth2.1 Resource Server)
- JWT token validation using Keycloak public keys
- Method-level security with role-based authorization
- Integration with Party Data Management for business rules
- OWASP Top 10 security protection
- Comprehensive audit logging

#### API Gateway
- OAuth2.1 token validation
- Rate limiting and DDoS protection
- FAPI compliance headers
- Circuit breaker patterns
- Performance monitoring

---

## Authentication Flow

The system implements OAuth2.1 Authorization Code Flow with PKCE for enhanced security:

![OAuth2.1 Authentication Sequence](generated-diagrams/OAuth2.1%20Authentication%20%26%20Authorization%20Sequence.svg)

### Step-by-Step Process

1. **Initial Request**: User accesses banking application
2. **OAuth2.1 Redirect**: Application redirects to Keycloak authorization endpoint with PKCE challenge
3. **User Authentication**: Keycloak presents login form with banking theme
4. **LDAP Verification**: Keycloak authenticates user against LDAP directory
5. **Role Resolution**: System queries Party Data Management for authoritative business roles
6. **Token Generation**: Keycloak generates JWT token with comprehensive claims
7. **Token Exchange**: Application exchanges authorization code for access token
8. **API Access**: Application uses JWT token for secure API access
9. **Business Authorization**: Each request validates roles against Party Data Management
10. **Audit Logging**: All access decisions are logged for compliance

### Token Claims Structure

```json
{
  "sub": "john.smith@banking.local",
  "realm_access": {
    "roles": ["LOAN_OFFICER"]
  },
  "banking_roles": ["LOAN_OFFICER", "LOAN_MANAGER"],
  "banking_groups": ["loan-officers", "banking-operations"],
  "monetary_limit": 500000,
  "authority_level": 7,
  "business_unit": "Commercial Loans",
  "session_id": "12345-abcde",
  "iat": 1640995200,
  "exp": 1640998800,
  "iss": "http://keycloak:8080/realms/banking-realm",
  "aud": "banking-app"
}
```

---

## Authorization Model

### Multi-Layered Authorization Strategy

The system implements a sophisticated authorization model with three distinct layers:

#### Layer 1: Keycloak Realm Authorization
- **Purpose**: Basic authentication and realm-level roles
- **Scope**: Application access and basic permissions
- **Source**: Keycloak realm configuration

#### Layer 2: LDAP Group Membership
- **Purpose**: Organizational structure and department mapping
- **Scope**: Group-based permissions and hierarchy
- **Source**: Enterprise LDAP directory

#### Layer 3: Party Data Management (Authoritative)
- **Purpose**: Business-specific roles and fine-grained permissions
- **Scope**: Monetary limits, temporal controls, compliance
- **Source**: Banking application database

### Role Hierarchy

```
BANKING_ADMIN
├── LOAN_MANAGER
│   ├── LOAN_OFFICER
│   └── LOAN_VIEWER
├── COMPLIANCE_OFFICER
│   └── AUDIT_VIEWER
└── USER_MANAGER
    └── CUSTOMER_SERVICE
```

### Authority Levels and Monetary Limits

| Role | Authority Level | Monetary Limit | Description |
|------|----------------|----------------|-------------|
| BANKING_ADMIN | 10 | Unlimited | Full system administration |
| LOAN_MANAGER | 8 | $2,000,000 | Senior loan management |
| LOAN_OFFICER | 7 | $500,000 | Standard loan operations |
| COMPLIANCE_OFFICER | 6 | N/A | Compliance oversight |
| CUSTOMER_SERVICE | 3 | $10,000 | Customer support |
| LOAN_VIEWER | 2 | N/A | Read-only access |

---

## Security Features

### OAuth2.1 Enhancements
- **PKCE (Proof Key for Code Exchange)**: Prevents authorization code interception attacks
- **State Parameter**: Prevents CSRF attacks during authorization flow
- **Token Binding**: Prevents token theft and replay attacks
- **Enhanced Token Introspection**: Real-time token validation

### Banking-Specific Security
- **FAPI 1.0 Advanced Compliance**: Financial-grade API security
- **mTLS Support**: Mutual TLS for client authentication
- **Request Object Signing**: JWS signature for critical requests
- **Geographic Restrictions**: IP-based access controls
- **Time-based Access**: Temporal role assignments

### Audit & Compliance Features
- **Immutable Audit Logs**: Tamper-proof event logging
- **Real-time Monitoring**: Instant security event detection
- **Compliance Reporting**: Automated regulatory reports
- **Access Reviews**: Periodic role certification
- **Segregation of Duties**: Conflict detection and prevention

---

## Implementation Details

### Configuration Management (12-Factor App)

All configuration is externalized via environment variables:

```properties
# Keycloak OAuth2.1 Configuration
KEYCLOAK_URL=http://keycloak:8080
KEYCLOAK_REALM=banking-realm
KEYCLOAK_CLIENT_ID=banking-app
KEYCLOAK_CLIENT_SECRET=${SECRET}

# Security Configuration
OAUTH2_ENABLED=true
ROLES_DATABASE_ENABLED=true
ROLES_LDAP_ENABLED=true
ROLES_CACHE_TTL=3600

# Banking Compliance
FAPI_ENABLED=true
PCI_ENABLED=true
AUDIT_ENABLED=true
```

### Database Schema

#### Party Data Management Tables

```sql
-- Core party information
CREATE TABLE parties (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(255) UNIQUE NOT NULL,
    identifier VARCHAR(255) UNIQUE NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    party_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    compliance_level VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Role assignments
CREATE TABLE party_roles (
    id BIGSERIAL PRIMARY KEY,
    party_id BIGINT NOT NULL REFERENCES parties(id),
    role_name VARCHAR(100) NOT NULL,
    role_source VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from TIMESTAMP NOT NULL DEFAULT NOW(),
    effective_to TIMESTAMP,
    authority_level INTEGER NOT NULL DEFAULT 1,
    monetary_limit BIGINT,
    business_unit VARCHAR(100),
    assigned_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Group memberships
CREATE TABLE party_groups (
    id BIGSERIAL PRIMARY KEY,
    party_id BIGINT NOT NULL REFERENCES parties(id),
    group_name VARCHAR(100) NOT NULL,
    group_type VARCHAR(50) NOT NULL,
    group_role VARCHAR(50) NOT NULL DEFAULT 'MEMBER',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from TIMESTAMP NOT NULL DEFAULT NOW(),
    effective_to TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

### Domain Model

![Domain Model](generated-diagrams/Domain%20Model.svg)

The domain model shows the complete Party Data Management bounded context with:

- **Party Aggregate Root**: Central entity for user representation
- **PartyRole Entity**: Role assignments with temporal controls
- **PartyGroup Entity**: Group memberships and hierarchies
- **Value Objects**: Supporting types for compliance and configuration
- **Domain Events**: Event-driven architecture for audit trails

---

## Compliance & Audit

### Regulatory Compliance

#### FAPI (Financial-grade API) Compliance
- **FAPI 1.0 Advanced** implementation
- **Request object signing** with JWS
- **mTLS client authentication**
- **Enhanced security headers**

#### PCI DSS Compliance
- **Data encryption** at rest and in transit
- **Access control measures** with role-based permissions
- **Regular security testing** and vulnerability assessments
- **Network segmentation** and monitoring

#### SOX Compliance
- **Segregation of duties** enforcement
- **Access certification** and periodic reviews
- **Immutable audit trails** for financial operations
- **Change management** controls

### Audit Capabilities

#### Event Types Logged
- Authentication success/failure
- Authorization decisions
- Role assignments/revocations
- Permission escalations
- Data access events
- Administrative actions
- System configuration changes

#### Audit Trail Features
- **Immutable storage** in append-only logs
- **Digital signatures** for log integrity
- **Real-time streaming** to SIEM systems
- **Long-term retention** for regulatory requirements
- **Search and correlation** capabilities

---

## Monitoring & Observability

### Key Metrics

#### Authentication Metrics
- Authentication success rate: >99.9%
- Average authentication time: <2 seconds
- Failed login attempts: <0.1%
- Account lockout rate: <0.01%

#### Authorization Metrics
- Token validation time: <50ms
- Role resolution time: <100ms
- Authorization success rate: >99.95%
- Permission escalation attempts: 0

#### System Performance
- API response time: <200ms (95th percentile)
- System availability: >99.95%
- Database query time: <10ms
- Cache hit ratio: >95%

### Alerting Rules

#### Security Alerts
- Multiple failed login attempts (>5 in 5 minutes)
- Unusual access patterns (off-hours, new locations)
- Permission escalation attempts
- System configuration changes
- Token validation failures (>1% error rate)

#### Performance Alerts
- API response time >500ms
- Database connection pool exhaustion
- Cache miss ratio >20%
- High memory usage >80%
- Disk space usage >85%

### Dashboard Components

#### Security Dashboard
- Real-time authentication status
- Failed login attempt trends
- Geographic access patterns
- Role assignment changes
- Compliance violation alerts

#### Operational Dashboard
- System health overview
- Performance metrics
- Error rate trends
- Capacity utilization
- Service dependency status

---

## Deployment & Operations

### Container Architecture

The system is deployed using Docker containers with the following services:

- **Keycloak**: OAuth2.1 authorization server
- **OpenLDAP**: Identity provider
- **Banking Application**: Main business logic
- **PostgreSQL**: Primary database
- **Redis**: Caching and session storage
- **Kafka**: Event streaming
- **Prometheus/Grafana**: Monitoring

### Kubernetes Deployment

#### Security Contexts
```yaml
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

#### Health Checks
```yaml
livenessProbe:
  httpGet:
    path: /api/actuator/health/liveness
    port: 8080
  initialDelaySeconds: 90
  periodSeconds: 30

readinessProbe:
  httpGet:
    path: /api/actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
```

### Backup & Recovery

#### Database Backup
- **Continuous WAL archiving** for point-in-time recovery
- **Daily full backups** with 30-day retention
- **Cross-region replication** for disaster recovery
- **Encrypted backup storage** with AES-256

#### Configuration Backup
- **GitOps approach** for configuration management
- **Versioned deployments** with rollback capability
- **Secret management** with external key stores
- **Infrastructure as Code** with Terraform

---

## Troubleshooting Guide

### Common Issues

#### Authentication Failures
```bash
# Check Keycloak logs
kubectl logs -n banking-system deployment/keycloak

# Check LDAP connectivity
ldapsearch -x -H ldap://ldap:389 -D "cn=admin,dc=banking,dc=local" -w password

# Verify JWT token
curl -H "Authorization: Bearer $TOKEN" http://keycloak:8080/realms/banking-realm/protocol/openid-connect/userinfo
```

#### Authorization Issues
```bash
# Check party role assignments
SELECT * FROM party_roles WHERE party_id = 123 AND active = true;

# Verify role cache
redis-cli GET "party:roles:john.smith"

# Check authorization logs
grep "authorization" /var/log/banking-app/application.log
```

#### Performance Issues
```bash
# Monitor token validation time
curl -w "@curl-format.txt" -s -o /dev/null http://api/validate-token

# Check database performance
SELECT * FROM pg_stat_activity WHERE state = 'active';

# Monitor cache performance
redis-cli INFO stats
```

---

## Security Best Practices

### Development Guidelines

1. **Never hardcode secrets** in configuration files
2. **Use environment variables** for all sensitive configuration
3. **Implement proper error handling** without exposing internal details
4. **Validate all inputs** using whitelist validation
5. **Log security events** comprehensively
6. **Test security controls** regularly
7. **Keep dependencies updated** with security patches

### Operational Guidelines

1. **Rotate secrets regularly** (quarterly for high-value secrets)
2. **Monitor security metrics** continuously
3. **Conduct access reviews** quarterly
4. **Test disaster recovery** procedures monthly
5. **Update security documentation** with changes
6. **Train staff** on security procedures
7. **Perform security assessments** annually

---

## Conclusion

The OAuth2.1 architecture implemented in the Enterprise Banking System provides a robust, scalable, and compliant solution for modern banking security requirements. The multi-layered approach ensures defense in depth while maintaining usability and performance.

Key benefits achieved:

- **Enhanced Security** with OAuth2.1 and PKCE
- **Regulatory Compliance** with FAPI, PCI DSS, and SOX
- **Operational Excellence** with comprehensive monitoring
- **Scalability** through microservices architecture
- **Maintainability** via 12-Factor App methodology

For technical support or questions, please refer to the [API Documentation](API-Documentation.md) or contact the Security Architecture team.