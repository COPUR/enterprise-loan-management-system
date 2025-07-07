#!/bin/bash

# Enhanced Enterprise Banking System - Local Deployment Script
# Complete end-to-end deployment with full functionality

set -e

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="docker/compose/docker-compose.enhanced-enterprise.yml"
APP_NAME="Enhanced Enterprise Banking System"
LOG_FILE="logs/deployment.log"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1" | tee -a "$LOG_FILE"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1" | tee -a "$LOG_FILE"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1" | tee -a "$LOG_FILE"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$LOG_FILE"
}

log_step() {
    echo -e "${PURPLE}[STEP]${NC} $1" | tee -a "$LOG_FILE"
}

# Banner
show_banner() {
    echo -e "${CYAN}"
    echo "=================================================================="
    echo "    Enhanced Enterprise Banking System - Local Deployment"
    echo "=================================================================="
    echo "    Complete AI-Powered Banking Platform with:"
    echo "    • Spring AI with RAG capabilities"
    echo "    • FAPI-compliant OAuth2.1 security"
    echo "    • Event-driven architecture with SAGA patterns"
    echo "    • Service mesh with Istio"
    echo "    • Multi-language support with Arabic"
    echo "    • Real-time fraud detection"
    echo "    • Banking compliance standards"
    echo "=================================================================="
    echo -e "${NC}"
}

# Prerequisites check
check_prerequisites() {
    log_step "Checking prerequisites..."
    
    local missing_tools=()
    
    # Check Docker
    if ! command -v docker &> /dev/null; then
        missing_tools+=("docker")
    else
        log_success "Docker found: $(docker --version)"
    fi
    
    # Check Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        missing_tools+=("docker-compose")
    else
        log_success "Docker Compose found: $(docker-compose --version)"
    fi
    
    # Check available memory
    local available_memory
    if [[ "$OSTYPE" == "darwin"* ]]; then
        available_memory=$(sysctl hw.memsize | awk '{print $2/1024/1024/1024}')
    else
        available_memory=$(free -g | awk '/^Mem:/{print $2}')
    fi
    
    if (( $(echo "$available_memory < 8" | bc -l) )); then
        log_warning "Available memory: ${available_memory}GB. Recommended: 8GB+"
    else
        log_success "Available memory: ${available_memory}GB"
    fi
    
    # Check disk space
    local available_disk=$(df -h . | awk 'NR==2 {print $4}' | sed 's/G//')
    if (( $(echo "$available_disk < 10" | bc -l) )); then
        log_warning "Available disk space: ${available_disk}GB. Recommended: 10GB+"
    else
        log_success "Available disk space: ${available_disk}GB"
    fi
    
    if [ ${#missing_tools[@]} -ne 0 ]; then
        log_error "Missing required tools: ${missing_tools[*]}"
        echo "Please install the missing tools and try again."
        exit 1
    fi
    
    log_success "All prerequisites met!"
}

# Environment setup
setup_environment() {
    log_step "Setting up environment..."
    
    # Create necessary directories
    mkdir -p logs data/postgres data/redis data/kafka data/grafana data/prometheus
    
    # Set environment variables
    export COMPOSE_PROJECT_NAME="enhanced-banking"
    export POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-SecureBanking2024!}"
    export REDIS_PASSWORD="${REDIS_PASSWORD:-RedisSecure2024!}"
    export KEYCLOAK_ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin123}"
    export SPRING_PROFILES_ACTIVE="docker,enhanced,ai-enabled"
    export OPENAI_API_KEY="${OPENAI_API_KEY:-}"
    
    # Create .env file for Docker Compose
    cat > .env << EOF
# Enhanced Enterprise Banking System Environment Configuration
COMPOSE_PROJECT_NAME=enhanced-banking
POSTGRES_PASSWORD=${POSTGRES_PASSWORD}
REDIS_PASSWORD=${REDIS_PASSWORD}
KEYCLOAK_ADMIN_PASSWORD=${KEYCLOAK_ADMIN_PASSWORD}
SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}
OPENAI_API_KEY=${OPENAI_API_KEY}

# Banking System Configuration
BANKING_COMPLIANCE_STRICT=true
FAPI_ENABLED=true
AI_ENABLED=true
MULTI_LANGUAGE_ENABLED=true
FRAUD_DETECTION_ENABLED=true

# Resource Limits
JAVA_OPTS=-Xmx2g -XX:+UseG1GC -XX:+UseContainerSupport
POSTGRES_SHARED_BUFFERS=256MB
REDIS_MAXMEMORY=512MB
EOF
    
    log_success "Environment configured"
}

# Build application
build_application() {
    log_step "Building Enhanced Banking Application..."
    
    # Ensure Gradle wrapper is executable
    chmod +x ./gradlew
    
    # Build the application
    if ./gradlew clean build -x test --no-daemon; then
        log_success "Application built successfully"
    else
        log_error "Application build failed"
        exit 1
    fi
}

# Docker services management
start_infrastructure() {
    log_step "Starting infrastructure services..."
    
    # Stop any existing containers
    docker-compose -f "$COMPOSE_FILE" down -v 2>/dev/null || true
    
    # Pull latest images
    log_info "Pulling latest Docker images..."
    docker-compose -f "$COMPOSE_FILE" pull
    
    # Start infrastructure services first
    log_info "Starting PostgreSQL database..."
    docker-compose -f "$COMPOSE_FILE" up -d postgres-enhanced
    
    log_info "Starting Redis cluster..."
    docker-compose -f "$COMPOSE_FILE" up -d redis-cluster-enhanced
    
    log_info "Starting Kafka and Zookeeper..."
    docker-compose -f "$COMPOSE_FILE" up -d zookeeper-enhanced kafka-enhanced
    
    log_info "Starting Keycloak for OAuth2.1..."
    docker-compose -f "$COMPOSE_FILE" up -d keycloak-enhanced
    
    log_info "Starting Vector Database (Qdrant)..."
    docker-compose -f "$COMPOSE_FILE" up -d qdrant-enhanced
    
    log_success "Infrastructure services started"
}

# Wait for services to be ready
wait_for_services() {
    log_step "Waiting for infrastructure services to be ready..."
    
    # Wait for PostgreSQL
    log_info "Waiting for PostgreSQL..."
    timeout 120 bash -c 'until docker-compose -f docker/compose/docker-compose.enhanced-enterprise.yml exec -T postgres-enhanced pg_isready -U banking_user; do sleep 2; done'
    
    # Wait for Redis
    log_info "Waiting for Redis..."
    timeout 60 bash -c 'until docker-compose -f docker/compose/docker-compose.enhanced-enterprise.yml exec -T redis-cluster-enhanced redis-cli ping; do sleep 2; done'
    
    # Wait for Kafka
    log_info "Waiting for Kafka..."
    timeout 120 bash -c 'until docker-compose -f docker/compose/docker-compose.enhanced-enterprise.yml exec -T kafka-enhanced kafka-topics --bootstrap-server localhost:9092 --list &>/dev/null; do sleep 5; done'
    
    # Wait for Keycloak
    log_info "Waiting for Keycloak..."
    timeout 180 bash -c 'until curl -f http://localhost:8090/realms/master/.well-known/openid_configuration &>/dev/null; do sleep 5; done'
    
    # Wait for Qdrant
    log_info "Waiting for Qdrant Vector Database..."
    timeout 60 bash -c 'until curl -f http://localhost:6333/health &>/dev/null; do sleep 2; done'
    
    log_success "All infrastructure services are ready"
}

# Start application services
start_application_services() {
    log_step "Starting application services..."
    
    # Start the enhanced banking application
    log_info "Starting Enhanced Banking Application..."
    docker-compose -f "$COMPOSE_FILE" up -d banking-app-enhanced
    
    # Start monitoring services
    log_info "Starting monitoring stack..."
    docker-compose -f "$COMPOSE_FILE" up -d prometheus-enhanced grafana-enhanced jaeger-enhanced
    
    # Start service mesh components
    log_info "Starting service mesh components..."
    docker-compose -f "$COMPOSE_FILE" up -d envoy-proxy-enhanced
    
    log_success "Application services started"
}

# Initialize data and configurations
initialize_system() {
    log_step "Initializing system data and configurations..."
    
    # Wait for application to start
    log_info "Waiting for banking application..."
    timeout 180 bash -c 'until curl -f http://localhost:8080/actuator/health &>/dev/null; do sleep 5; done'
    
    # Initialize Kafka topics
    log_info "Creating Kafka topics for banking events..."
    docker-compose -f "$COMPOSE_FILE" exec -T kafka-enhanced bash -c '
        # Customer domain events
        kafka-topics --create --bootstrap-server localhost:9092 --topic banking.customer.lifecycle.created.v1 --partitions 10 --replication-factor 1 --if-not-exists
        kafka-topics --create --bootstrap-server localhost:9092 --topic banking.customer.onboarding.kyc-completed.v1 --partitions 10 --replication-factor 1 --if-not-exists
        
        # Loan domain events
        kafka-topics --create --bootstrap-server localhost:9092 --topic banking.account.loans.originated.v1 --partitions 20 --replication-factor 1 --if-not-exists
        kafka-topics --create --bootstrap-server localhost:9092 --topic banking.account.loans.approved.v1 --partitions 10 --replication-factor 1 --if-not-exists
        kafka-topics --create --bootstrap-server localhost:9092 --topic banking.account.loans.disbursed.v1 --partitions 10 --replication-factor 1 --if-not-exists
        
        # Payment events
        kafka-topics --create --bootstrap-server localhost:9092 --topic banking.transaction.payments.processed.v1 --partitions 50 --replication-factor 1 --if-not-exists
        kafka-topics --create --bootstrap-server localhost:9092 --topic banking.transaction.payments.failed.v1 --partitions 10 --replication-factor 1 --if-not-exists
        
        # AI and fraud events
        kafka-topics --create --bootstrap-server localhost:9092 --topic banking.ai.recommendation.generated.v1 --partitions 10 --replication-factor 1 --if-not-exists
        kafka-topics --create --bootstrap-server localhost:9092 --topic banking.fraud.transaction.flagged.v1 --partitions 20 --replication-factor 1 --if-not-exists
        
        # Compliance events
        kafka-topics --create --bootstrap-server localhost:9092 --topic banking.compliance.check-completed.v1 --partitions 5 --replication-factor 1 --if-not-exists
        
        echo "Banking Kafka topics created successfully"
    '
    
    # Initialize AI knowledge base
    if [ -n "$OPENAI_API_KEY" ]; then
        log_info "Initializing AI knowledge base..."
        curl -X POST http://localhost:8080/api/ai/knowledge/initialize \
            -H "Content-Type: application/json" \
            -d '{"source": "banking-regulations", "update": true}' || log_warning "AI initialization failed - continuing without AI features"
    else
        log_warning "OPENAI_API_KEY not set - AI features will be limited"
    fi
    
    # Configure Keycloak realm
    log_info "Configuring Keycloak OAuth2.1 realm..."
    sleep 10 # Wait for Keycloak to fully start
    
    # Create banking realm and client
    docker-compose -f "$COMPOSE_FILE" exec -T keycloak-enhanced bash -c '
        /opt/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080 --realm master --user admin --password admin123
        
        # Create banking-enterprise realm
        /opt/keycloak/bin/kcadm.sh create realms -s realm=banking-enterprise -s enabled=true -s displayName="Enhanced Enterprise Banking" || echo "Realm already exists"
        
        # Create banking client
        /opt/keycloak/bin/kcadm.sh create clients -r banking-enterprise -s clientId=banking-client -s enabled=true -s publicClient=false -s protocol=openid-connect -s "redirectUris=[\"http://localhost:8080/*\"]" || echo "Client already exists"
        
        echo "Keycloak configuration completed"
    ' || log_warning "Keycloak configuration failed - continuing with default setup"
    
    log_success "System initialization completed"
}

# Health checks
perform_health_checks() {
    log_step "Performing comprehensive health checks..."
    
    local services=(
        "PostgreSQL:http://localhost:5432:Database connection"
        "Redis:http://localhost:6379:Cache service"
        "Kafka:http://localhost:9092:Event streaming"
        "Banking App:http://localhost:8080/actuator/health:Main application"
        "Keycloak:http://localhost:8090/realms/master:OAuth2.1 server"
        "Qdrant:http://localhost:6333/health:Vector database"
        "Prometheus:http://localhost:9090/-/healthy:Metrics collection"
        "Grafana:http://localhost:3000/api/health:Monitoring dashboards"
        "Jaeger:http://localhost:16686:Distributed tracing"
    )
    
    local healthy_services=0
    local total_services=${#services[@]}
    
    for service in "${services[@]}"; do
        IFS=':' read -r name url description <<< "$service"
        
        if [[ "$name" == "PostgreSQL" ]]; then
            if docker-compose -f "$COMPOSE_FILE" exec -T postgres-enhanced pg_isready -U banking_user &>/dev/null; then
                log_success "  ✓ $name - $description"
                ((healthy_services++))
            else
                log_warning "  ⚠ $name - $description (not responding)"
            fi
        elif [[ "$name" == "Redis" ]]; then
            if docker-compose -f "$COMPOSE_FILE" exec -T redis-cluster-enhanced redis-cli ping &>/dev/null; then
                log_success "  ✓ $name - $description"
                ((healthy_services++))
            else
                log_warning "  ⚠ $name - $description (not responding)"
            fi
        elif [[ "$name" == "Kafka" ]]; then
            if docker-compose -f "$COMPOSE_FILE" exec -T kafka-enhanced kafka-topics --bootstrap-server localhost:9092 --list &>/dev/null; then
                log_success "  ✓ $name - $description"
                ((healthy_services++))
            else
                log_warning "  ⚠ $name - $description (not responding)"
            fi
        else
            if curl -f "$url" &>/dev/null; then
                log_success "  ✓ $name - $description"
                ((healthy_services++))
            else
                log_warning "  ⚠ $name - $description (not responding)"
            fi
        fi
    done
    
    if [[ $healthy_services -eq $total_services ]]; then
        log_success "All services are healthy ($healthy_services/$total_services)"
    else
        log_warning "Some services need attention ($healthy_services/$total_services healthy)"
    fi
}

# Display access information
show_access_info() {
    log_step "Enhanced Enterprise Banking System - Access Information"
    
    echo -e "${CYAN}"
    echo "=================================================================="
    echo "    ENHANCED ENTERPRISE BANKING SYSTEM - READY FOR USE"
    echo "=================================================================="
    echo ""
    echo "📱 BANKING APPLICATION:"
    echo "   • Main Application: http://localhost:8080"
    echo "   • Health Check: http://localhost:8080/actuator/health"
    echo "   • API Documentation: http://localhost:8080/swagger-ui.html"
    echo "   • Banking Dashboard: http://localhost:8080/dashboard"
    echo ""
    echo "🔐 SECURITY & AUTHENTICATION:"
    echo "   • Keycloak Admin: http://localhost:8090 (admin/admin123)"
    echo "   • OAuth2.1 Realm: banking-enterprise"
    echo "   • FAPI Compliance: Enabled"
    echo ""
    echo "📊 MONITORING & OBSERVABILITY:"
    echo "   • Grafana Dashboards: http://localhost:3000 (admin/admin123)"
    echo "   • Prometheus Metrics: http://localhost:9090"
    echo "   • Jaeger Tracing: http://localhost:16686"
    echo "   • Kafka UI: http://localhost:8082"
    echo ""
    echo "🤖 AI & ANALYTICS:"
    echo "   • AI Services: http://localhost:8080/api/ai/"
    echo "   • RAG Queries: http://localhost:8080/api/ai/rag/query"
    echo "   • Fraud Detection: http://localhost:8080/api/ai/fraud/analyze"
    echo "   • Vector Database: http://localhost:6333"
    echo ""
    echo "🗄️ DATA SERVICES:"
    echo "   • PostgreSQL: localhost:5432 (banking_user/${POSTGRES_PASSWORD})"
    echo "   • Redis Cache: localhost:6379"
    echo "   • Kafka Brokers: localhost:9092"
    echo ""
    echo "🧪 TESTING ENDPOINTS:"
    echo "   • Business Requirements: ./scripts/test-business-requirements.sh"
    echo "   • API Tests: ./postman/Enhanced-Enterprise-Banking-System.postman_collection.json"
    echo "   • Load Tests: ./scripts/load-test-enhanced-banking.sh"
    echo ""
    echo "📚 SAMPLE API CALLS:"
    echo "   • Create Loan: curl -X POST http://localhost:8080/api/loans \\"
    echo "     -H 'Content-Type: application/json' \\"
    echo "     -d '{\"customerId\":\"12345\",\"amount\":25000,\"numberOfInstallments\":24}'"
    echo ""
    echo "   • AI Loan Recommendation: curl http://localhost:8080/api/ai/recommendations/loans?customerId=12345"
    echo ""
    echo "   • Health Check: curl http://localhost:8080/actuator/health"
    echo ""
    echo "=================================================================="
    echo "    SYSTEM STATUS: READY FOR ENHANCED BANKING OPERATIONS"
    echo "=================================================================="
    echo -e "${NC}"
    
    # Save access info to file
    cat > "ENHANCED_BANKING_ACCESS.md" << EOF
# Enhanced Enterprise Banking System - Access Guide

## Service Access URLs

### Banking Application
- **Main Application**: http://localhost:8080
- **Health Check**: http://localhost:8080/actuator/health
- **API Documentation**: http://localhost:8080/swagger-ui.html
- **Banking Dashboard**: http://localhost:8080/dashboard

### Security & Authentication
- **Keycloak Admin Console**: http://localhost:8090
  - Username: admin
  - Password: admin123
  - Realm: banking-enterprise

### Monitoring & Observability
- **Grafana**: http://localhost:3000 (admin/admin123)
- **Prometheus**: http://localhost:9090
- **Jaeger Tracing**: http://localhost:16686
- **Kafka UI**: http://localhost:8082

### AI & Analytics
- **AI Services**: http://localhost:8080/api/ai/
- **RAG Queries**: http://localhost:8080/api/ai/rag/query
- **Fraud Detection**: http://localhost:8080/api/ai/fraud/analyze
- **Vector Database**: http://localhost:6333

### Data Services
- **PostgreSQL**: localhost:5432
  - Username: banking_user
  - Password: ${POSTGRES_PASSWORD}
  - Database: banking
- **Redis**: localhost:6379
- **Kafka**: localhost:9092

## Quick Test Commands

\`\`\`bash
# Health check
curl http://localhost:8080/actuator/health

# Create a loan
curl -X POST http://localhost:8080/api/loans \\
  -H "Content-Type: application/json" \\
  -d '{"customerId":"12345","amount":25000,"numberOfInstallments":24}'

# Get AI loan recommendations
curl http://localhost:8080/api/ai/recommendations/loans?customerId=12345

# Check system metrics
curl http://localhost:8080/actuator/metrics
\`\`\`

## Stopping the System

\`\`\`bash
# Stop all services
docker-compose -f docker/compose/docker-compose.enhanced-enterprise.yml down

# Stop and remove volumes
docker-compose -f docker/compose/docker-compose.enhanced-enterprise.yml down -v
\`\`\`
EOF
    
    log_success "Access information saved to ENHANCED_BANKING_ACCESS.md"
}

# Cleanup function
cleanup() {
    log_step "Cleaning up temporary files..."
    rm -f .env.tmp logs/deployment.tmp
}

# Error handling
handle_error() {
    log_error "Deployment failed at step: $1"
    echo "Check the logs for more details: $LOG_FILE"
    echo "To clean up, run: docker-compose -f $COMPOSE_FILE down -v"
    exit 1
}

# Trap errors
trap 'handle_error "${BASH_COMMAND}"' ERR

# Main deployment function
main() {
    # Create logs directory
    mkdir -p logs
    
    show_banner
    
    check_prerequisites
    setup_environment
    build_application
    start_infrastructure
    wait_for_services
    start_application_services
    initialize_system
    perform_health_checks
    show_access_info
    cleanup
    
    log_success "Enhanced Enterprise Banking System deployed successfully!"
    echo ""
    echo "🎉 The system is now ready for enhanced banking operations!"
    echo "📖 Check ENHANCED_BANKING_ACCESS.md for detailed access information"
    echo ""
}

# Script options
case "${1:-}" in
    "help"|"--help"|"-h")
        echo "Enhanced Enterprise Banking System - Local Deployment Script"
        echo ""
        echo "Usage: $0 [option]"
        echo ""
        echo "Options:"
        echo "  help          Show this help message"
        echo "  stop          Stop all services"
        echo "  logs          Show service logs"
        echo "  status        Show service status"
        echo "  clean         Clean up all data and containers"
        echo ""
        echo "Environment Variables:"
        echo "  POSTGRES_PASSWORD      PostgreSQL password (default: SecureBanking2024!)"
        echo "  REDIS_PASSWORD         Redis password (default: RedisSecure2024!)"
        echo "  KEYCLOAK_ADMIN_PASSWORD Keycloak admin password (default: admin123)"
        echo "  OPENAI_API_KEY         OpenAI API key for AI features"
        echo ""
        exit 0
        ;;
    "stop")
        log_info "Stopping Enhanced Enterprise Banking System..."
        docker-compose -f "$COMPOSE_FILE" down
        log_success "System stopped"
        exit 0
        ;;
    "logs")
        docker-compose -f "$COMPOSE_FILE" logs -f
        exit 0
        ;;
    "status")
        docker-compose -f "$COMPOSE_FILE" ps
        exit 0
        ;;
    "clean")
        log_warning "This will remove all data and containers. Are you sure? (y/N)"
        read -r response
        if [[ "$response" =~ ^[Yy]$ ]]; then
            docker-compose -f "$COMPOSE_FILE" down -v --remove-orphans
            docker system prune -f
            rm -rf data logs .env ENHANCED_BANKING_ACCESS.md
            log_success "System cleaned up"
        else
            log_info "Clean up cancelled"
        fi
        exit 0
        ;;
    "")
        # Default: run deployment
        main
        ;;
    *)
        log_error "Unknown option: $1"
        echo "Use '$0 help' for usage information"
        exit 1
        ;;
esac