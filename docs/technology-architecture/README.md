# Technology Architecture

This section contains infrastructure diagrams, deployment configurations, and monitoring specifications.

## Infrastructure Diagrams
- [AWS EKS Architecture](infrastructure-diagrams/aws-eks-architecture.puml) - Cloud infrastructure design
- [Cache Performance Architecture](infrastructure-diagrams/cache-performance-architecture.puml) - Redis caching strategy

## Infrastructure
- [Gradle Modernization Report](infrastructure/GRADLE_MODERNIZATION_REPORT.md) - Build system upgrades
- [Redis ElastiCache Documentation](infrastructure/REDIS_ELASTICACHE_DOCUMENTATION.md) - Caching implementation
- [Cache Performance Tests](infrastructure/CACHE_PERFORMANCE_TESTS.md) - Performance validation

## Deployment
- [AWS EKS Deployment Complete](deployment/AWS_EKS_DEPLOYMENT_COMPLETE.md) - Production deployment guide
- [Gitpod Deployment](deployment/GITPOD_DEPLOYMENT.md) - Development environment setup
- [CI/CD Pipeline](deployment/ci-cd-pipeline.puml) - Automated deployment workflow

## Monitoring
- [System Status Report](monitoring/SYSTEM_STATUS_REPORT.md) - Current system status
- [Monitoring Documentation](monitoring/MONITORING_DOCUMENTATION.md) - Observability setup
- [Monitoring Observability](monitoring/monitoring-observability.puml) - Monitoring architecture

## Technology Stack
- **Java 21**: Virtual threads for high concurrency
- **Spring Boot 3.3.6**: Enterprise framework
- **PostgreSQL 16.9**: Primary database
- **Redis 7.2**: Caching and session management
- **AWS EKS**: Container orchestration
- **Kubernetes 1.28**: Container platform