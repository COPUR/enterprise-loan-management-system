# 🏦 Enterprise Loan Management System - Deployment Guide

## 📋 Quick Start

### Prerequisites
- Docker and Docker Compose
- OpenSSL (for SSL certificates)
- curl and jq (for testing)
- At least 16GB RAM and 50GB disk space

### 🚀 One-Command Deployment
```bash
./scripts/deploy.sh
```

This script will:
1. ✅ Check prerequisites
2. 🔐 Generate SSL certificates
3. 🏗️ Initialize infrastructure
4. 🔨 Build application
5. 🚀 Start all services
6. 💾 Initialize data
7. 📬 Create Postman collection
8. 🏥 Perform health checks

### 🔗 Service URLs After Deployment

| Service | URL | Description |
|---------|-----|-------------|
| **Main Application** | https://localhost | Primary banking interface |
| **API Gateway** | https://localhost/api | Gateway for all APIs |
| **Customer Service** | https://localhost/customers | Customer management |
| **Loan Service** | https://localhost/loans | Loan processing |
| **Payment Service** | https://localhost/payments | Payment processing |
| **Open Banking** | https://localhost/open-banking | FAPI 2.0 compliant APIs |
| **ML Analytics** | https://localhost/ml | AI/ML services |
| **Federation** | https://localhost/federation | Cross-region monitoring |
| **Keycloak (OAuth)** | http://localhost:8080 | Identity provider |
| **Grafana** | https://localhost/grafana | Dashboards (admin/admin) |
| **Prometheus** | https://localhost/prometheus | Metrics |
| **Kibana** | https://localhost/kibana | Log analysis |
| **Jaeger** | https://localhost/jaeger | Distributed tracing |

## 🧪 Testing

### Run All Tests
```bash
./scripts/test-runner.sh
```

### Specific Test Categories
```bash
./scripts/test-runner.sh health      # Quick health check
./scripts/test-runner.sh security    # Security tests
./scripts/test-runner.sh api         # API endpoint tests
./scripts/test-runner.sh performance # Performance tests
./scripts/test-runner.sh integration # End-to-end tests
```

### 📬 Postman Collection
The deployment creates a comprehensive Postman collection:
- **File**: `Enterprise-Banking-API-Collection.postman_collection.json`
- **Features**: 
  - OAuth 2.1 + DPoP authentication
  - Complete banking workflows
  - FAPI 2.0 compliance testing
  - Load testing scenarios
  - Security validation tests

## 🏗️ Architecture Overview

### Microservices
- **Party Data Server** (Port 8081): Identity and authentication
- **API Gateway** (Port 8082): Request routing and security
- **Customer Service** (Port 8083): Customer management
- **Loan Service** (Port 8084): Loan processing
- **Payment Service** (Port 8085): Payment processing
- **Open Banking Gateway** (Port 8086): FAPI compliance
- **ML Anomaly Service** (Port 8087): Fraud detection
- **Federation Monitoring** (Port 8088): Cross-region coordination

### Infrastructure Services
- **PostgreSQL** (Port 5432): Primary database
- **Redis** (Port 6379): Caching and sessions
- **Kafka** (Port 9092): Event streaming
- **Elasticsearch** (Port 9200): Search and logging
- **Keycloak** (Port 8080): OAuth 2.1 + DPoP
- **Prometheus** (Port 9090): Metrics collection
- **Grafana** (Port 3000): Visualization
- **Kibana** (Port 5601): Log analysis
- **Jaeger** (Port 16686): Distributed tracing
- **Nginx** (Port 80/443): Load balancer and SSL termination

## 🔐 Security Features

### OAuth 2.1 + DPoP + FAPI 2.0
- RFC 9449 compliant DPoP implementation
- FAPI 2.0 security profile
- JWT token binding
- Request signature validation

### Zero Trust Architecture
- mTLS everywhere
- Continuous verification
- Policy enforcement
- Advanced threat detection

### Compliance
- PCI DSS v4 compliance
- SOX compliance
- GDPR compliance
- Multi-jurisdictional support

## 📊 Monitoring & Observability

### Health Indicators
- **Banking Health**: Loan processing, payment systems
- **Fraud Detection**: ML model health
- **Compliance**: Regulatory framework monitoring
- **Customer Service**: Response times and availability

### Metrics
- **Business Metrics**: Customer count, active loans, payment rates
- **Performance Metrics**: Response times, error rates
- **Security Metrics**: Fraud detection, compliance checks
- **Infrastructure Metrics**: CPU, memory, database performance

### Dashboards
- **Banking Overview**: Key business metrics
- **System Performance**: Technical metrics
- **Security Dashboard**: Threat monitoring
- **Compliance Dashboard**: Regulatory compliance

## 🔧 Management Commands

### Service Management
```bash
./scripts/deploy.sh start    # Start all services
./scripts/deploy.sh stop     # Stop all services
./scripts/deploy.sh restart  # Restart all services
./scripts/deploy.sh health   # Check service health
./scripts/deploy.sh logs     # Show recent logs
./scripts/deploy.sh urls     # Show service URLs
./scripts/deploy.sh cleanup  # Clean up containers and volumes
```

### Database Management
```bash
# Connect to database
docker-compose exec postgres psql -U banking_user -d banking_db

# View sample data
docker-compose exec postgres psql -U banking_user -d banking_db -c "SELECT * FROM banking_customer.customers LIMIT 5;"
```

### Redis Management
```bash
# Connect to Redis
docker-compose exec redis redis-cli -a banking_password

# View cached data
docker-compose exec redis redis-cli -a banking_password KEYS "*"
```

### Kafka Management
```bash
# List topics
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list

# View topic messages
docker-compose exec kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic customer.events --from-beginning
```

## 🐛 Troubleshooting

### Common Issues

#### Services Not Starting
```bash
# Check Docker daemon
docker info

# Check logs
docker-compose logs <service-name>

# Restart specific service
docker-compose restart <service-name>
```

#### SSL Certificate Issues
```bash
# Regenerate certificates
cd scripts/nginx/ssl
./generate-certs.sh
```

#### Database Connection Issues
```bash
# Check database health
docker-compose exec postgres pg_isready -U banking_user -d banking_db

# Reset database
docker-compose down -v
docker-compose up -d postgres
```

#### Memory Issues
```bash
# Check resource usage
docker stats

# Increase Docker memory allocation (Docker Desktop)
# Recommended: 16GB RAM, 4GB swap
```

### Performance Tuning

#### JVM Settings
The application is optimized for Java 21 with:
- Virtual Threads for high concurrency
- G1GC for low-latency garbage collection
- Optimized heap settings

#### Database Optimization
- Connection pooling with HikariCP
- Read replicas for scaling
- Proper indexing for banking queries

#### Caching Strategy
- Redis for session storage
- Application-level caching
- CDN for static content

## 🔄 CI/CD Integration

### GitHub Actions
```yaml
name: Deploy Banking System
on:
  push:
    branches: [main]
jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Deploy
        run: |
          chmod +x scripts/deploy.sh
          ./scripts/deploy.sh
          ./scripts/test-runner.sh
```

### Production Deployment
For production deployment:
1. Update environment variables in `docker-compose.yml`
2. Replace self-signed certificates with CA-issued certificates
3. Configure external databases and Redis clusters
4. Set up proper monitoring and alerting
5. Configure backup and disaster recovery

## 📚 Additional Resources

- **API Documentation**: Available at https://localhost/api/docs
- **Grafana Dashboards**: https://localhost/grafana
- **System Metrics**: https://localhost/prometheus
- **Distributed Tracing**: https://localhost/jaeger
- **Log Analysis**: https://localhost/kibana

## 🆘 Support

For issues and questions:
1. Check the logs: `./scripts/deploy.sh logs`
2. Run health checks: `./scripts/test-runner.sh health`
3. Verify service URLs: `./scripts/deploy.sh urls`
4. Review this documentation
5. Check the Postman collection for API examples

---

🎉 **Deployment Complete!** Your Enterprise Banking System is ready for testing and development.