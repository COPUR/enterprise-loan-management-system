# Technology Architecture

This section contains infrastructure diagrams, deployment configurations, and monitoring specifications.

## Infrastructure Diagrams
- [AWS EKS Architecture](infrastructure-diagrams/generated-diagrams/AWS%20EKS%20Enterprise%20Loan%20Management%20System%20Architecture.svg) - Cloud infrastructure design
- [Cache Performance Architecture](infrastructure-diagrams/generated-diagrams/Multi-Level%20Cache%20Architecture%20-%20Enterprise%20Loan%20Management%20System.svg) - Redis caching strategy

## Infrastructure
- [Gradle Modernization Report](infrastructure/GRADLE_MODERNIZATION_REPORT.md) - Build system upgrades
- [Redis ElastiCache Documentation](infrastructure/REDIS_ELASTICACHE_DOCUMENTATION.md) - Caching implementation
- [Cache Performance Tests](infrastructure/CACHE_PERFORMANCE_TESTS.md) - Performance validation

## Deployment
- [AWS EKS Deployment Complete](deployment/AWS_EKS_DEPLOYMENT_COMPLETE.md) - Production deployment guide
- [Gitpod Deployment](deployment/GITPOD_DEPLOYMENT.md) - Development environment setup
- [CI/CD Pipeline](deployment/generated-diagrams/CI/CD%20Pipeline%20-%20Enterprise%20Loan%20Management%20System.svg) - Automated deployment workflow

## Monitoring
- [System Status Report](monitoring/SYSTEM_STATUS_REPORT.md) - Current system status
- [Monitoring Documentation](monitoring/MONITORING_DOCUMENTATION.md) - Observability setup
- [Monitoring Observability](monitoring/generated-diagrams/Monitoring%20&%20Observability%20-%20Enterprise%20Loan%20Management%20System.svg) - Monitoring architecture

## Technology Stack
- **Java 21**: Virtual threads for high concurrency
- **Spring Boot 3.3.6**: Enterprise framework
- **PostgreSQL 16.9**: Primary database
- **Redis 7.2**: Caching and session management
- **AWS EKS**: Container orchestration
- **Kubernetes 1.28**: Container platform