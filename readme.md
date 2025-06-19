# Enterprise Banking System - Loan Management Platform

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/banking/enterprise-loan-management-system)
[![Security Scan](https://img.shields.io/badge/security-compliant-green)](https://github.com/banking/enterprise-loan-management-system/security)
[![Coverage](https://img.shields.io/badge/coverage-95%25-brightgreen)](https://codecov.io/gh/banking/enterprise-loan-management-system)
[![OAuth2.1](https://img.shields.io/badge/OAuth2.1-FAPI%20Compliant-blue)](docs/OAuth2.1-Architecture-Guide.md)
[![Java Version](https://img.shields.io/badge/Java-21-blue)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.6-green)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-Proprietary-red)](LICENSE)

## Overview

A comprehensive enterprise-grade banking system built on modern microservices architecture with OAuth2.1 authentication, domain-driven design, and full regulatory compliance. The system provides secure loan origination, customer management, and payment processing capabilities for financial institutions.

### Key Features

- **🔐 OAuth2.1 Security**: Keycloak-based authentication with PKCE and FAPI compliance
- **🏛️ Banking Compliance**: PCI DSS, SOX, GDPR, and Basel III compliance
- **🏗️ Microservices Architecture**: Cloud-native design with Kubernetes deployment
- **📊 Domain-Driven Design**: Bounded contexts for banking operations
- **🔍 Comprehensive Audit**: Immutable audit trails for regulatory compliance
- **⚡ High Performance**: Auto-scaling with 99.95% uptime SLA
- **🛡️ Security-First**: OWASP Top 10 protection and zero-trust architecture

## Architecture Overview

![System Architecture](docs/generated-diagrams/OAuth2.1%20Architecture%20Overview.svg)

The system implements a multi-layered security architecture with:

- **Identity Layer**: Keycloak OAuth2.1 + LDAP integration
- **Authorization Layer**: Role-based access control with Party Data Management
- **Application Layer**: Spring Boot microservices with hexagonal architecture
- **Data Layer**: PostgreSQL with Redis caching and event streaming

## Quick Start

### Prerequisites

- Java 21+
- Docker & Docker Compose
- Kubernetes 1.28+
- Helm 3.13+

### Local Development

```bash
# Clone the repository
git clone https://github.com/banking/enterprise-loan-management-system.git
cd enterprise-loan-management-system

# Start local development environment
docker-compose up -d

# Build the application
./gradlew build

# Run tests
./gradlew test

# Start the application
./gradlew bootRun
```

### Production Deployment

```bash
# Deploy to Kubernetes using Helm
helm install banking-system k8s/helm/banking-system \
  --namespace banking-system \
  --create-namespace \
  --values k8s/helm/banking-system/values-production.yaml

# Verify deployment
kubectl get pods -n banking-system
```

## Documentation

### Architecture Documentation

| Document | Description |
|----------|-------------|
| [OAuth2.1 Architecture Guide](docs/OAuth2.1-Architecture-Guide.md) | Complete OAuth2.1 implementation with Keycloak |
| [Security Architecture](docs/security-architecture/Security-Architecture-Overview.md) | OWASP Top 10 compliance and banking security |
| [Application Architecture](docs/application-architecture/Application-Architecture-Guide.md) | Microservices and DDD implementation |
| [Infrastructure Architecture](docs/infrastructure-architecture/Infrastructure-Architecture-Guide.md) | Kubernetes deployment and operations |

### API & Operations

| Document | Description |
|----------|-------------|
| [API Documentation](docs/API-Documentation.md) | RESTful APIs with OAuth2.1 integration |
| [Deployment & Operations](docs/deployment-operations/Deployment-Operations-Guide.md) | Production deployment and operational procedures |

### Domain Models

![Domain Model](docs/generated-diagrams/Domain%20Model.svg)

## Security & Compliance

### OAuth2.1 Implementation

The system implements OAuth2.1 Authorization Code Flow with PKCE for enhanced security:

- **Keycloak Authorization Server**: Banking realm with LDAP integration
- **Multi-layered Authorization**: Keycloak + LDAP + Party Data Management
- **FAPI 1.0 Advanced**: Financial-grade API security compliance
- **Comprehensive Audit**: Real-time security event logging

### Banking Compliance

- **PCI DSS**: Payment card data protection
- **SOX**: Financial reporting controls
- **GDPR**: Data privacy and protection
- **Basel III**: Risk management framework

### Security Features

![Security Architecture](docs/generated-diagrams/OWASP%20Top%2010%20Security%20Architecture.svg)

- **OWASP Top 10 Protection**: Complete mitigation of web application risks
- **Zero Trust Architecture**: Continuous verification and monitoring
- **Encryption**: AES-256 at rest, TLS 1.3 in transit
- **Rate Limiting**: API protection against abuse

## Technology Stack

### Core Technologies

- **Backend**: Java 21, Spring Boot 3.3, Spring Security
- **Database**: PostgreSQL 16 with Redis caching
- **Messaging**: Apache Kafka for event streaming
- **Authentication**: Keycloak OAuth2.1 server
- **Directory**: OpenLDAP for identity management

### Infrastructure

- **Container Platform**: Docker with Kubernetes 1.28+
- **Service Mesh**: Istio for secure microservices communication
- **Monitoring**: Prometheus, Grafana, Jaeger
- **CI/CD**: GitHub Actions with ArgoCD GitOps
- **Cloud**: AWS EKS with multi-AZ deployment

### Development Tools

- **Build**: Gradle 8.5 with dependency management
- **Testing**: JUnit 5, Testcontainers, WireMock
- **Code Quality**: SonarQube, SpotBugs, OWASP Dependency Check
- **Documentation**: PlantUML, OpenAPI 3.0

## Project Structure

```
enterprise-loan-management-system/
├── src/main/java/com/banking/loans/
│   ├── domain/                 # Domain entities and business logic
│   ├── application/            # Application services and use cases
│   ├── infrastructure/         # External integrations and persistence
│   └── presentation/           # REST controllers and DTOs
├── k8s/                        # Kubernetes manifests
│   ├── helm/                   # Helm charts
│   └── base/                   # Base Kubernetes resources
├── config/                     # Configuration files
│   ├── keycloak/              # OAuth2.1 realm configuration
│   └── ldap/                  # LDAP directory setup
├── docs/                       # Technical documentation
│   ├── architecture/          # Architecture diagrams
│   ├── security-architecture/ # Security documentation
│   └── generated-diagrams/    # Generated SVG diagrams
└── scripts/                    # Deployment and utility scripts
```

## API Overview

### Core Banking APIs

```bash
# Customer Management
POST   /api/v1/customers           # Create customer
GET    /api/v1/customers/{id}      # Get customer details
PUT    /api/v1/customers/{id}      # Update customer

# Loan Management
POST   /api/v1/loans               # Create loan application
POST   /api/v1/loans/{id}/approve  # Approve loan
GET    /api/v1/loans/{id}/installments # Get payment schedule

# Payment Processing
POST   /api/v1/payments            # Process payment
GET    /api/v1/payments/{id}       # Get payment details

# OAuth2.1 Integration
POST   /oauth2/token               # Get access token
GET    /oauth2/userinfo            # Get user information
POST   /oauth2/revoke              # Revoke token
```

### Authentication Example

```bash
# OAuth2.1 Authorization Code Flow with PKCE
curl -X POST https://api.banking.enterprise.com/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=authorization_code" \
  -d "code=${AUTHORIZATION_CODE}" \
  -d "client_id=banking-app" \
  -d "code_verifier=${CODE_VERIFIER}"

# Use access token for API calls
curl -X GET https://api.banking.enterprise.com/api/v1/customers/123 \
  -H "Authorization: Bearer ${ACCESS_TOKEN}"
```

## Development Guidelines

### Code Quality Standards

- **Test Coverage**: Minimum 90% line coverage
- **Security**: OWASP guidelines and dependency scanning
- **Performance**: Sub-200ms API response times
- **Documentation**: Comprehensive API and architecture docs

### Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/new-feature`
3. Make changes following coding standards
4. Run tests: `./gradlew test`
5. Submit a pull request with detailed description

### Code Style

```java
// Example: Domain entity with DDD principles
@Entity
@Table(name = "loans")
public class Loan extends AggregateRoot {
    
    @Id
    private LoanId loanId;
    
    @Embedded
    private Money amount;
    
    @Embedded
    private InterestRate interestRate;
    
    // Domain methods
    public void approve(UserId approver, Money authorityLimit) {
        if (amount.isGreaterThan(authorityLimit)) {
            throw new InsufficientAuthorityException();
        }
        
        this.status = LoanStatus.APPROVED;
        this.addDomainEvent(new LoanApprovedEvent(loanId, approver));
    }
}
```

## Deployment Environments

### Environment Configuration

| Environment | URL | Purpose | OAuth2.1 Realm |
|-------------|-----|---------|-----------------|
| Development | http://localhost:8080 | Local development | `banking-dev` |
| Testing | https://api-test.banking.local | Integration testing | `banking-test` |
| Staging | https://api-staging.banking.local | Pre-production | `banking-staging` |
| Production | https://api.banking.enterprise.com | Live operations | `banking-realm` |

### Infrastructure as Code

```yaml
# Example Kubernetes deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: banking-app
spec:
  replicas: 3
  selector:
    matchLabels:
      app: banking-app
  template:
    spec:
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
      containers:
      - name: banking-app
        image: harbor.banking.local/banking/app:1.0.0
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: OAUTH2_ISSUER_URI
          valueFrom:
            configMapKeyRef:
              name: banking-config
              key: oauth2.issuer-uri
```

## Monitoring & Observability

### Key Metrics

- **Application Performance**: 99.95% uptime, <200ms response time
- **Security Metrics**: Authentication success rate >99.9%
- **Business Metrics**: Loan processing time, approval rates
- **Infrastructure**: CPU, memory, disk utilization

### Dashboards

![Monitoring Dashboard](docs/generated-diagrams/Monitoring%20&%20Observability%20-%20Enterprise%20Loan%20Management%20System.svg)

### Health Checks

```bash
# Application health
curl https://api.banking.enterprise.com/actuator/health

# OAuth2.1 health
curl https://keycloak.banking.local/health

# Database connectivity
curl https://api.banking.enterprise.com/actuator/health/db
```

## Troubleshooting

### Common Issues

| Issue | Solution |
|-------|----------|
| OAuth2.1 authentication failure | Check Keycloak realm configuration and LDAP connectivity |
| Database connection timeout | Verify connection pool settings and database health |
| High API latency | Check database query performance and cache hit ratios |
| Pod startup failure | Review resource limits and configuration |

### Support

- **Documentation**: [Technical Documentation](docs/)
- **API Reference**: [API Documentation](docs/API-Documentation.md)
- **Runbooks**: [Operations Guide](docs/deployment-operations/Deployment-Operations-Guide.md)
- **Security**: [Security Architecture](docs/security-architecture/Security-Architecture-Overview.md)

## License

This project is proprietary software owned by the Banking Enterprise. All rights reserved.

## Contact

- **Development Team**: dev-team@banking.enterprise.com
- **Security Team**: security@banking.enterprise.com
- **Operations Team**: ops@banking.enterprise.com

---

**Built with ❤️ for secure banking operations**