# Enhanced Enterprise Banking System - Docker Configuration Guide

## 🏦 Enhanced Docker Architecture v2.0

This guide documents the enhanced Docker configuration for the Enhanced Enterprise Banking System, optimized for DDD/Hexagonal Architecture, BIAN compliance, FAPI security, and Islamic banking features.

## 📋 Available Docker Configurations

### 1. **Dockerfile.enhanced-v2** (RECOMMENDED)
Enhanced multi-stage Docker build with comprehensive banking features:

```bash
# Build enhanced production image
docker build -f Dockerfile.enhanced-v2 --target runtime -t banking-system:enhanced .

# Build functional testing environment
docker build -f Dockerfile.enhanced-v2 --target functional-testing -t banking-system:test .

# Build development environment
docker build -f Dockerfile.enhanced-v2 --target development -t banking-system:dev .

# Build Kubernetes-optimized image
docker build -f Dockerfile.enhanced-v2 --target kubernetes -t banking-system:k8s .
```

### 2. **Original Dockerfile**
Comprehensive multi-stage build with banking compliance:

```bash
# Build standard production image
docker build --target runtime -t banking-system:standard .
```

## 🚀 Key Enhancements in v2.0

### **Architecture Support**
- ✅ **Domain-Driven Design (DDD)**: Full support for domain entities, value objects, and aggregates
- ✅ **Hexagonal Architecture**: Clean separation with ports and adapters
- ✅ **CQRS & Event Sourcing**: Command/Query separation with event streaming
- ✅ **Microservices**: Service mesh ready with cloud-native patterns

### **Banking Compliance**
- ✅ **BIAN Compliance**: Banking Industry Architecture Network standards
- ✅ **FAPI Security**: Financial-grade API security (OAuth2.1, MTLS, JWS)
- ✅ **Islamic Banking**: Sharia-compliant features (Murabaha, Ijara, Musharaka)
- ✅ **PCI/GDPR**: Data protection and security compliance

### **AI/ML Integration**
- ✅ **Credit Scoring**: AI-powered risk assessment
- ✅ **OpenAI Integration**: Spring AI framework support
- ✅ **Vector Databases**: Support for ML embeddings and recommendations

### **Comprehensive Testing**
- ✅ **Functional Tests**: End-to-end banking workflow validation
- ✅ **Architecture Tests**: DDD/Hexagonal compliance validation
- ✅ **Security Tests**: FAPI and banking security compliance
- ✅ **Performance Tests**: Load testing and scalability validation

## 🏗️ Multi-Stage Build Targets

### **Stage 1: Builder**
- Java 21 with Gradle 8.11.1
- Multi-layer JAR extraction for optimal Docker caching
- Comprehensive dependency management
- Build metadata generation

### **Stage 2: Runtime (Production)**
- Eclipse Temurin JRE 21 (Alpine-based)
- Security hardened with non-root user
- Banking compliance environment variables
- Enhanced health checks and monitoring

### **Stage 3: Functional Testing**
- Extended testing environment with TestContainers
- Banking-specific test configurations
- AI/ML integration test support
- Comprehensive test reporting

### **Stage 4: Development**
- Hot reload with Spring DevTools
- Debug port exposure (5005)
- Development tools (PostgreSQL client, Redis CLI, etc.)
- Enhanced logging for debugging

### **Stage 5: Kubernetes**
- Cloud-native optimizations
- Service mesh integration
- Kubernetes health checks (liveness, readiness)
- ConfigMap and Secret integration

## 🔧 Environment Variables

### **Core Application**
```bash
# Application Configuration
SERVER_PORT=8080
MANAGEMENT_PORT=8081
SPRING_PROFILES_ACTIVE=production
APP_NAME="Enhanced Enterprise Banking System"
APP_VERSION="2.0.0"
```

### **Database Configuration**
```bash
# PostgreSQL Configuration
DATABASE_URL=jdbc:postgresql://postgres:5432/banking
DATABASE_USERNAME=banking_user
DATABASE_PASSWORD=your_secure_password
DATABASE_POOL_SIZE=20
HIBERNATE_DDL_AUTO=validate
```

### **Security & FAPI Compliance**
```bash
# Banking Security
FAPI_ENABLED=true
BANKING_JWT_SECRET=your_jwt_secret
BANKING_JWT_ALGORITHM=RS256
BANKING_SECURITY_STRICT=true
AUDIT_ENABLED=true
```

### **Islamic Banking**
```bash
# Sharia Compliance
ISLAMIC_BANKING_ENABLED=true
SHARIA_COMPLIANCE_STRICT=true
HIJRI_CALENDAR_ENABLED=true
```

### **AI/ML Configuration**
```bash
# AI Features
AI_CREDIT_SCORING_ENABLED=true
OPENAI_API_KEY=your_openai_key
OPENAI_MODEL=gpt-4
ML_RISK_ASSESSMENT_ENABLED=true
```

### **Monitoring & Observability**
```bash
# Metrics and Monitoring
PROMETHEUS_ENABLED=true
JAEGER_ENABLED=true
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics,prometheus,readiness,liveness
```

## 🐳 Docker Compose Examples

### **Production Deployment**
```yaml
version: '3.8'
services:
  banking-app:
    image: banking-system:enhanced
    ports:
      - "8080:8080"
      - "8081:8081"
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - DATABASE_URL=jdbc:postgresql://postgres:5432/banking
      - REDIS_HOST=redis
      - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
      - FAPI_ENABLED=true
      - ISLAMIC_BANKING_ENABLED=true
    depends_on:
      - postgres
      - redis
      - kafka
    
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: banking
      POSTGRES_USER: banking_user
      POSTGRES_PASSWORD: secure_password
    
  redis:
    image: redis:7-alpine
    
  kafka:
    image: confluentinc/cp-kafka:latest
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
```

### **Development Environment**
```yaml
version: '3.8'
services:
  banking-dev:
    build:
      context: .
      dockerfile: Dockerfile.enhanced-v2
      target: development
    ports:
      - "8080:8080"
      - "5005:5005"  # Debug port
    environment:
      - SPRING_PROFILES_ACTIVE=development,local
      - JPA_SHOW_SQL=true
      - LOGGING_LEVEL_BANKING=DEBUG
    volumes:
      - ./src:/app/src:ro
```

### **Functional Testing**
```yaml
version: '3.8'
services:
  banking-tests:
    build:
      context: .
      dockerfile: Dockerfile.enhanced-v2
      target: functional-testing
    environment:
      - SPRING_PROFILES_ACTIVE=test,functional
      - FUNCTIONAL_TESTS_ENABLED=true
      - BIAN_TESTS_ENABLED=true
      - FAPI_TESTS_ENABLED=true
      - ISLAMIC_BANKING_TESTS_ENABLED=true
    volumes:
      - ./test-reports:/app/test-reports
```

## ☸️ Kubernetes Deployment

### **Deployment YAML**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: enhanced-banking-system
  namespace: banking-system
spec:
  replicas: 3
  selector:
    matchLabels:
      app: enhanced-banking-system
  template:
    metadata:
      labels:
        app: enhanced-banking-system
        version: v2.0.0
    spec:
      containers:
      - name: banking-app
        image: banking-system:k8s
        ports:
        - containerPort: 8080
          name: http
        - containerPort: 8081
          name: management
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "kubernetes"
        - name: KUBERNETES_NAMESPACE
          valueFrom:
            fieldRef:
              fieldPath: metadata.namespace
        - name: POD_NAME
          valueFrom:
            fieldRef:
              fieldPath: metadata.name
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8081
          initialDelaySeconds: 90
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 10
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
```

## 🔐 Security Best Practices

### **Container Security**
- ✅ Non-root user execution (UID 1000/1001)
- ✅ Minimal base image (Alpine Linux)
- ✅ Security updates applied
- ✅ No sensitive data in environment variables
- ✅ Read-only root filesystem where possible

### **Banking Compliance**
- ✅ FAPI security headers enforcement
- ✅ JWT token validation with RS256
- ✅ Audit logging enabled
- ✅ TLS/MTLS support
- ✅ Rate limiting configured

### **Network Security**
- ✅ Minimal port exposure
- ✅ Health check endpoints secured
- ✅ Service mesh compatibility
- ✅ Network policies support

## 📊 Monitoring & Observability

### **Health Checks**
- **Liveness**: `/actuator/health/liveness`
- **Readiness**: `/actuator/health/readiness`
- **Health**: `/actuator/health`
- **Metrics**: `/actuator/metrics`
- **Prometheus**: `/actuator/prometheus`

### **Logging**
- Structured JSON logging for production
- Banking audit trail compliance
- Correlation IDs for distributed tracing
- Security event logging

### **Metrics**
- Banking-specific business metrics
- JVM and application performance metrics
- Custom metrics for loan processing
- Islamic banking compliance metrics

## 🚀 Performance Optimizations

### **JVM Tuning**
- G1 garbage collector optimized for banking workloads
- Container-aware memory management
- Optimal heap sizing for microservices
- JIT compilation optimizations

### **Docker Optimizations**
- Multi-layer JAR extraction for faster startup
- Dependency layer caching
- Minimal image layers
- Build-time optimization

### **Startup Performance**
- Lazy initialization where appropriate
- Connection pool pre-warming
- Cache pre-loading strategies
- Health check optimization

## 📝 Usage Examples

### **Quick Start - Production**
```bash
# Build and run production image
docker build -f Dockerfile.enhanced-v2 --target runtime -t banking-system:prod .
docker run -d \
  --name enhanced-banking \
  -p 8080:8080 \
  -p 8081:8081 \
  -e DATABASE_URL=jdbc:postgresql://localhost:5432/banking \
  -e FAPI_ENABLED=true \
  -e ISLAMIC_BANKING_ENABLED=true \
  banking-system:prod
```

### **Development with Debug**
```bash
# Build and run development image
docker build -f Dockerfile.enhanced-v2 --target development -t banking-system:dev .
docker run -d \
  --name enhanced-banking-dev \
  -p 8080:8080 \
  -p 5005:5005 \
  -e SPRING_PROFILES_ACTIVE=development \
  -v $(pwd)/src:/app/src:ro \
  banking-system:dev
```

### **Run Functional Tests**
```bash
# Build and run functional test suite
docker build -f Dockerfile.enhanced-v2 --target functional-testing -t banking-system:test .
docker run --rm \
  --name banking-functional-tests \
  -v $(pwd)/test-reports:/app/test-reports \
  -e FUNCTIONAL_TESTS_ENABLED=true \
  banking-system:test
```

## 🎯 Migration from Original Dockerfile

The enhanced Dockerfile maintains backward compatibility while adding:

1. **Extended multi-stage builds** for different environments
2. **Banking-specific environment variables** and configurations
3. **Enhanced security** with banking compliance features
4. **Comprehensive testing** support with functional test suites
5. **Kubernetes optimization** with cloud-native patterns
6. **AI/ML integration** support for modern banking features

## 📞 Support & Troubleshooting

### **Common Issues**
- **Memory Issues**: Adjust `MaxRAMPercentage` in JAVA_OPTS
- **Startup Failures**: Check database connectivity and environment variables
- **Test Failures**: Ensure TestContainers Docker socket access
- **Security Issues**: Verify FAPI configuration and JWT secrets

### **Debugging**
```bash
# Check application logs
docker logs enhanced-banking

# Exec into container for debugging
docker exec -it enhanced-banking bash

# Check health endpoints
curl http://localhost:8081/actuator/health
```

---

*Enhanced Enterprise Banking System Docker Configuration v2.0*  
*Supporting DDD/Hexagonal Architecture, BIAN Compliance, FAPI Security & Islamic Banking*