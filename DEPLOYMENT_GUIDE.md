# Enhanced Enterprise Banking System - Complete Deployment Guide

## Overview

This guide provides comprehensive instructions for deploying the Enhanced Enterprise Banking System locally with full functionality including AI capabilities, service mesh architecture, and banking compliance features.

## Prerequisites

### System Requirements
- **Operating System**: macOS, Linux, or Windows with WSL2
- **Memory**: 8GB RAM minimum (16GB recommended)
- **Disk Space**: 20GB free space minimum
- **Docker**: Docker Desktop 20.10+ with Docker Compose 2.0+
- **Network**: Internet connection for downloading images and AI services

### Required Tools
- Docker Desktop with Docker Compose
- curl (for API testing)
- jq (for JSON parsing, optional but recommended)
- Git (for repository access)

### Environment Variables (Optional)
```bash
export OPENAI_API_KEY="your-openai-api-key"          # For AI features
export POSTGRES_PASSWORD="SecureBanking2024!"        # Database password
export REDIS_PASSWORD="RedisSecure2024!"             # Redis password
export KEYCLOAK_ADMIN_PASSWORD="admin123"            # Keycloak admin password
```

## Quick Start - One-Command Deployment

### 1. Deploy the Complete System
```bash
./deploy-local-enterprise.sh
```

This single command will:
- Check all prerequisites
- Build the banking application
- Start all infrastructure services (PostgreSQL, Redis, Kafka, Keycloak)
- Deploy the enhanced banking application
- Configure AI services and vector database
- Set up monitoring stack (Prometheus, Grafana, Jaeger)
- Initialize Kafka topics for banking events
- Configure OAuth2.1 security with FAPI compliance
- Perform comprehensive health checks
- Display access information

### 2. Test the Deployment
```bash
./test-deployment.sh
```

This will run comprehensive tests to validate:
- Application health and readiness
- Banking API functionality
- AI-powered features
- Security and authentication
- Data layer connectivity
- Event streaming capabilities
- Monitoring services
- Performance characteristics

## Architecture Components

### Core Banking Application
- **Technology**: Spring Boot 3.3.6 with Java 21
- **Architecture**: Hexagonal architecture with DDD principles
- **Features**: Loan management, customer onboarding, payment processing
- **AI Integration**: Spring AI with RAG capabilities
- **Security**: FAPI-compliant OAuth2.1 with DPoP token binding

### Infrastructure Services

#### Database Layer
- **PostgreSQL 16**: Primary database with enterprise clustering
- **Redis Cluster**: Distributed caching and session management
- **Qdrant Vector DB**: AI embeddings and similarity search

#### Messaging & Events
- **Apache Kafka**: Event streaming with banking-specific topics
- **Zookeeper**: Kafka cluster coordination
- **Event-Driven Architecture**: SAGA patterns for complex transactions

#### Security & Identity
- **Keycloak 23.0**: OAuth2.1 and FAPI-compliant identity management
- **mTLS**: Service-to-service communication security
- **Token Management**: Distributed Redis-based token storage

#### Monitoring & Observability
- **Prometheus**: Metrics collection and alerting
- **Grafana**: Real-time dashboards and visualization
- **Jaeger**: Distributed tracing and performance monitoring
- **ELK Stack**: Centralized logging and audit trails

## Service Access Information

### Banking Application
| Service | URL | Description |
|---------|-----|-------------|
| Main Application | http://localhost:8080 | Core banking APIs and dashboard |
| Health Check | http://localhost:8080/actuator/health | Application health status |
| API Documentation | http://localhost:8080/swagger-ui.html | Interactive API documentation |
| Metrics | http://localhost:8080/actuator/metrics | Application metrics |

### Security & Authentication
| Service | URL | Credentials | Description |
|---------|-----|-------------|-------------|
| Keycloak Admin | http://localhost:8090 | admin/admin123 | OAuth2.1 server administration |
| Banking Realm | http://localhost:8090/realms/banking-enterprise | - | FAPI-compliant authentication |

### Monitoring & Observability
| Service | URL | Credentials | Description |
|---------|-----|-------------|-------------|
| Grafana | http://localhost:3000 | admin/admin123 | Banking system dashboards |
| Prometheus | http://localhost:9090 | - | Metrics and alerting |
| Jaeger | http://localhost:16686 | - | Distributed tracing |
| Kafka UI | http://localhost:8082 | - | Kafka topic management |

### AI & Analytics
| Service | URL | Description |
|---------|-----|-------------|
| AI Services | http://localhost:8080/api/ai/ | AI-powered banking intelligence |
| RAG Queries | http://localhost:8080/api/ai/rag/query | Retrieval augmented generation |
| Fraud Detection | http://localhost:8080/api/ai/fraud/analyze | Real-time fraud analysis |
| Vector Database | http://localhost:6333 | Qdrant vector storage |

### Data Services
| Service | Connection | Credentials | Description |
|---------|------------|-------------|-------------|
| PostgreSQL | localhost:5432 | banking_user/SecureBanking2024! | Primary database |
| Redis | localhost:6379 | RedisSecure2024! | Cache and sessions |
| Kafka | localhost:9092 | - | Event streaming |

## API Testing Examples

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

### Create a Loan Application
```bash
curl -X POST http://localhost:8080/api/loans \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "12345",
    "amount": 25000,
    "numberOfInstallments": 24,
    "loanType": "PERSONAL",
    "purpose": "HOME_IMPROVEMENT"
  }'
```

### AI-Powered Loan Recommendations
```bash
curl "http://localhost:8080/api/ai/recommendations/loans?customerId=12345"
```

### Real-time Fraud Detection
```bash
curl -X POST http://localhost:8080/api/ai/fraud/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "txn-789",
    "amount": 5000,
    "merchantId": "merchant-123",
    "location": "New York, NY"
  }'
```

### RAG-Based Banking Assistant
```bash
curl -X POST http://localhost:8080/api/ai/rag/query \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What are the requirements for a mortgage loan approval?",
    "customerId": "12345",
    "context": "loan_application"
  }'
```

## Business Requirements Testing

The system implements all Orange Solution business requirements:

### 1. Enhanced Loan Creation with AI
```bash
# Create a loan with AI risk assessment
curl -X POST http://localhost:8080/api/loans \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "12345",
    "amount": 25000,
    "numberOfInstallments": 24,
    "aiRecommendations": true,
    "riskAssessment": "enhanced"
  }'
```

### 2. Intelligent Customer Loan Retrieval
```bash
# Get customer loans with AI insights
curl "http://localhost:8080/api/loans?customerId=12345&aiInsights=true"
```

### 3. Smart Installment Management
```bash
# Get installments with predictive analysis
curl "http://localhost:8080/api/loans/67890/installments?predictiveAnalysis=true"
```

### 4. AI-Powered Payment Processing
```bash
# Process payment with fraud detection
curl -X POST http://localhost:8080/api/loans/67890/pay \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 1200,
    "fraudDetection": "enhanced",
    "aiValidation": true
  }'
```

## Advanced Configuration

### Environment Customization
Create a custom `.env` file:
```bash
# Banking System Configuration
COMPOSE_PROJECT_NAME=enhanced-banking
POSTGRES_PASSWORD=YourSecurePassword
REDIS_PASSWORD=YourRedisPassword
KEYCLOAK_ADMIN_PASSWORD=YourKeycloakPassword
OPENAI_API_KEY=your-openai-api-key

# Feature Toggles
BANKING_COMPLIANCE_STRICT=true
FAPI_ENABLED=true
AI_ENABLED=true
MULTI_LANGUAGE_ENABLED=true
FRAUD_DETECTION_ENABLED=true

# Performance Tuning
JAVA_OPTS=-Xmx4g -XX:+UseG1GC
POSTGRES_SHARED_BUFFERS=512MB
REDIS_MAXMEMORY=1GB
```

### Monitoring Configuration
Custom Grafana dashboards are pre-configured for:
- Banking transaction metrics
- Loan approval rates
- AI model performance
- Fraud detection accuracy
- System performance and health

## Management Commands

### Start the System
```bash
./deploy-local-enterprise.sh
```

### Stop the System
```bash
./deploy-local-enterprise.sh stop
```

### View Service Logs
```bash
./deploy-local-enterprise.sh logs
```

### Check Service Status
```bash
./deploy-local-enterprise.sh status
```

### Clean Up Everything
```bash
./deploy-local-enterprise.sh clean
```

## Troubleshooting

### Common Issues

#### 1. Insufficient Memory
**Problem**: Services fail to start or are killed by Docker
**Solution**: Increase Docker Desktop memory allocation to 8GB+

#### 2. Port Conflicts
**Problem**: "Port already in use" errors
**Solution**: Stop conflicting services or modify port mappings in docker-compose file

#### 3. Database Connection Issues
**Problem**: Application fails to connect to PostgreSQL
**Solution**: Wait for database initialization (can take 2-3 minutes on first run)

#### 4. AI Features Not Working
**Problem**: AI endpoints return errors
**Solution**: Ensure OPENAI_API_KEY is set and valid

#### 5. Slow Performance
**Problem**: System responds slowly
**Solution**: Increase allocated CPU/memory, reduce number of services if needed

### Health Check Commands
```bash
# Check all service health
docker-compose -f docker-compose.enhanced-enterprise.yml ps

# Check application logs
docker-compose -f docker-compose.enhanced-enterprise.yml logs banking-app-enhanced

# Check database connectivity
docker-compose -f docker-compose.enhanced-enterprise.yml exec postgres-enhanced pg_isready

# Check Redis connectivity
docker-compose -f docker-compose.enhanced-enterprise.yml exec redis-cluster-enhanced redis-cli ping
```

### Performance Optimization
1. **Memory Allocation**: Ensure Docker has 8GB+ allocated
2. **CPU Allocation**: Allocate 4+ CPU cores to Docker
3. **Disk Performance**: Use SSD storage for better performance
4. **Network**: Ensure stable internet connection for AI services

## Security Considerations

### Production Deployment
For production deployment, update:
1. **Database Passwords**: Use strong, unique passwords
2. **Redis Authentication**: Enable AUTH with secure passwords
3. **Keycloak Configuration**: Configure proper realm and client settings
4. **API Keys**: Secure storage of OpenAI and other API keys
5. **Network Security**: Implement proper firewall rules
6. **TLS/SSL**: Enable HTTPS for all endpoints

### FAPI Compliance
The system implements FAPI 1.0 Advanced Profile including:
- DPoP (Demonstrating Proof-of-Possession) token binding
- PKCE (Proof Key for Code Exchange)
- Strong client authentication
- Comprehensive audit logging

## Support and Documentation

### Additional Resources
- **Architecture Documentation**: docs/architecture/
- **API Specifications**: docs/api/
- **Security Architecture**: docs/security-architecture/
- **Kafka Design**: docs/kafka/Enterprise-Banking-Kafka-Design.md
- **Postman Collections**: postman/

### Getting Help
1. Check this deployment guide first
2. Review the troubleshooting section
3. Examine service logs for specific errors
4. Consult the architecture documentation
5. Test with the provided Postman collection

## Success Criteria

A successful deployment should achieve:
- ✅ All health checks passing
- ✅ Banking APIs responding correctly
- ✅ AI features operational (with API key)
- ✅ Security services configured
- ✅ Monitoring dashboards accessible
- ✅ Event streaming functional
- ✅ 90%+ test success rate

The system is ready for banking operations when the test suite shows 90%+ success rate and all critical services are healthy.