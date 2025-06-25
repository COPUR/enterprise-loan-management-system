# Technology Architecture

This section contains infrastructure diagrams, deployment configurations, and monitoring specifications.

## Infrastructure Diagrams
- [**Istio OAuth2.1 Infrastructure**](infrastructure-diagrams/generated-diagrams/OAuth2.1%20Infrastructure%20Architecture%20-%20Banking%20System_v1.0.0.svg) - Istio service mesh with OAuth2.1 and FAPI compliance
- [AWS EKS Architecture](infrastructure-diagrams/generated-diagrams/AWS%20EKS%20Enterprise%20Loan%20Management%20System%20Architecture_v1.0.0.svg) - Cloud infrastructure design
- [Cache Performance Architecture](infrastructure-diagrams/generated-diagrams/Multi-Level%20Cache%20Architecture%20-%20Enterprise%20Loan%20Management%20System_v1.0.0.svg) - Redis caching strategy

## Infrastructure
- [Gradle Modernization Report](infrastructure/GRADLE_MODERNIZATION_REPORT.md) - Build system upgrades
- [Redis ElastiCache Documentation](infrastructure/REDIS_ELASTICACHE_DOCUMENTATION.md) - Caching implementation
- [Cache Performance Tests](infrastructure/CACHE_PERFORMANCE_TESTS.md) - Performance validation

## Deployment
- [AWS EKS Deployment Complete](deployment/AWS_EKS_DEPLOYMENT_COMPLETE.md) - Production deployment guide
- [Gitpod Deployment](deployment/GITPOD_DEPLOYMENT.md) - Development environment setup
- [CI/CD Pipeline](deployment/generated-diagrams/CI/CD%20Pipeline%20-%20Enterprise%20Loan%20Management%20System_v1.0.0.svg) - Automated deployment workflow

## Monitoring
- [System Status Report](monitoring/SYSTEM_STATUS_REPORT.md) - Current system status
- [Monitoring Documentation](monitoring/MONITORING_DOCUMENTATION.md) - Observability setup
- [Monitoring Observability](monitoring/generated-diagrams/Monitoring%20&%20Observability%20-%20Enterprise%20Loan%20Management%20System_v1.0.0.svg) - Monitoring architecture

## Technology Stack
- **Istio Service Mesh**: Zero trust service-to-service communication
- **Java 21**: Virtual threads for high concurrency
- **Spring Boot 3.3.6**: Enterprise framework
- **PostgreSQL 16.9**: Primary database
- **Redis 7.2**: Distributed caching and session management
- **AWS EKS**: Container orchestration
- **Kubernetes 1.28**: Container platform

---

## Technology Architecture Contact

**Istio Service Mesh & Infrastructure Design**
- **Architect**: Copur (AliCo)
- **GitHub**: [@copur](https://github.com/copur)
- **Expertise**: Service Mesh, OAuth2.1 FAPI, Distributed Systems