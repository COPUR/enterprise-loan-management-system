#!/bin/bash

# Enterprise Loan Management System - Deployment Script
# Comprehensive end-to-end deployment for local Docker environment

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Logging function
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] ✅ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] ⚠️  $1${NC}"
}

log_error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ❌ $1${NC}"
}

log_info() {
    echo -e "${CYAN}[$(date +'%Y-%m-%d %H:%M:%S')] ℹ️  $1${NC}"
}

# Banner
echo -e "${PURPLE}"
cat << 'EOF'
╔══════════════════════════════════════════════════════════════════════════════════════════════════════════╗
║                                                                                                          ║
║                           🏦 Enterprise Loan Management System 🏦                                       ║
║                                                                                                          ║
║                                    Comprehensive Deployment Script                                      ║
║                                                                                                          ║
║                    🔐 OAuth 2.1 + DPoP + FAPI 2.0 | 🤖 ML Fraud Detection                            ║
║                    🌍 Multi-Region Federation | 📊 Full Observability Stack                           ║
║                    🔒 Zero Trust Security | 🏪 Open Banking Gateway                                   ║
║                                                                                                          ║
╚══════════════════════════════════════════════════════════════════════════════════════════════════════════╝
EOF
echo -e "${NC}"

# Check prerequisites
check_prerequisites() {
    log "🔍 Checking prerequisites..."
    
    local missing_tools=()
    
    # Check Docker
    if ! command -v docker &> /dev/null; then
        missing_tools+=("docker")
    fi
    
    # Check Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        missing_tools+=("docker-compose")
    fi
    
    # Check OpenSSL
    if ! command -v openssl &> /dev/null; then
        missing_tools+=("openssl")
    fi
    
    # Check curl
    if ! command -v curl &> /dev/null; then
        missing_tools+=("curl")
    fi
    
    # Check jq
    if ! command -v jq &> /dev/null; then
        missing_tools+=("jq")
    fi
    
    if [ ${#missing_tools[@]} -ne 0 ]; then
        log_error "Missing required tools: ${missing_tools[*]}"
        log_info "Please install the missing tools and try again"
        exit 1
    fi
    
    # Check Docker daemon
    if ! docker info &> /dev/null; then
        log_error "Docker daemon is not running"
        log_info "Please start Docker and try again"
        exit 1
    fi
    
    log_success "All prerequisites met"
}

# Build application
build_application() {
    log "🔨 Building Enterprise Loan Management System..."
    
    cd "$PROJECT_ROOT"
    
    # Clean previous builds
    log "🧹 Cleaning previous builds..."
    ./gradlew clean
    
    # Build application
    log "📦 Building application..."
    ./gradlew build -x test
    
    # Build Docker images
    log "🐳 Building Docker images..."
    docker build -t enterprise-loan-system:1.0.0 -f Dockerfile .
    
    log_success "Application built successfully"
}

# Generate SSL certificates
generate_certificates() {
    log "🔐 Generating SSL certificates..."
    
    if [ -f "$SCRIPT_DIR/nginx/ssl/banking.crt" ]; then
        log_info "SSL certificates already exist, skipping generation"
        return 0
    fi
    
    cd "$SCRIPT_DIR/nginx/ssl"
    ./generate-certs.sh
    
    log_success "SSL certificates generated"
}

# Initialize infrastructure
initialize_infrastructure() {
    log "🏗️  Initializing infrastructure..."
    
    cd "$PROJECT_ROOT"
    
    # Create required directories
    mkdir -p logs
    mkdir -p data/postgres
    mkdir -p data/redis
    mkdir -p data/kafka
    mkdir -p data/elasticsearch
    mkdir -p data/grafana
    mkdir -p data/prometheus
    mkdir -p data/keycloak
    
    # Set proper permissions
    sudo chown -R 1000:1000 data/elasticsearch || true
    sudo chown -R 472:472 data/grafana || true
    sudo chown -R 65534:65534 data/prometheus || true
    
    log_success "Infrastructure initialized"
}

# Start core services
start_core_services() {
    log "🚀 Starting core services..."
    
    cd "$PROJECT_ROOT"
    
    # Start infrastructure services first
    log "📊 Starting infrastructure services..."
    docker-compose up -d postgres redis zookeeper kafka elasticsearch
    
    # Wait for services to be ready
    log "⏳ Waiting for infrastructure services..."
    sleep 30
    
    # Start identity and monitoring services
    log "🔐 Starting identity and monitoring services..."
    docker-compose up -d keycloak prometheus grafana kibana jaeger
    
    # Wait for services to be ready
    log "⏳ Waiting for identity and monitoring services..."
    sleep 30
    
    log_success "Core services started"
}

# Initialize data
initialize_data() {
    log "💾 Initializing data..."
    
    cd "$PROJECT_DIR"
    
    # Initialize Kafka topics
    log "📡 Initializing Kafka topics..."
    docker-compose up kafka-init
    
    # Initialize Redis data
    log "🗄️  Initializing Redis data..."
    docker-compose exec redis /bin/bash -c "
        if [ -f /docker-entrypoint-initdb.d/redis-init.sh ]; then
            chmod +x /docker-entrypoint-initdb.d/redis-init.sh
            /docker-entrypoint-initdb.d/redis-init.sh
        fi
    " || log_warning "Redis initialization script not found or failed"
    
    # Wait for database to be ready
    log "⏳ Waiting for database to be ready..."
    timeout 60 bash -c '
        until docker-compose exec postgres pg_isready -U banking_user -d banking_db; do
            echo "Waiting for database..."
            sleep 5
        done
    '
    
    log_success "Data initialized"
}

# Start application services
start_application_services() {
    log "🏦 Starting banking application services..."
    
    cd "$PROJECT_ROOT"
    
    # Start all banking services
    docker-compose up -d \
        party-data-server \
        api-gateway \
        customer-service \
        loan-service \
        payment-service \
        open-banking-gateway \
        ml-anomaly-service \
        federation-monitoring
    
    # Wait for services to be ready
    log "⏳ Waiting for banking services..."
    sleep 45
    
    log_success "Banking application services started"
}

# Start load balancer
start_load_balancer() {
    log "🌐 Starting load balancer..."
    
    cd "$PROJECT_ROOT"
    
    # Start nginx
    docker-compose up -d nginx
    
    # Wait for nginx to be ready
    log "⏳ Waiting for load balancer..."
    sleep 10
    
    log_success "Load balancer started"
}

# Health check
health_check() {
    log "🏥 Performing health checks..."
    
    local services=(
        "http://localhost:5432|PostgreSQL Database"
        "http://localhost:6379|Redis Cache"
        "http://localhost:9092|Kafka Message Broker"
        "http://localhost:8080|Keycloak Identity Provider"
        "http://localhost:9200|Elasticsearch Search"
        "http://localhost:8081/actuator/health|Party Data Server"
        "http://localhost:8082/actuator/health|API Gateway"
        "http://localhost:8083/actuator/health|Customer Service"
        "http://localhost:8084/actuator/health|Loan Service"
        "http://localhost:8085/actuator/health|Payment Service"
        "http://localhost:8086/actuator/health|Open Banking Gateway"
        "http://localhost:8087/actuator/health|ML Anomaly Service"
        "http://localhost:8088/actuator/health|Federation Monitoring"
        "http://localhost:9090|Prometheus Monitoring"
        "http://localhost:3000|Grafana Dashboards"
        "http://localhost:5601|Kibana Logging"
        "http://localhost:16686|Jaeger Tracing"
        "http://localhost:80|Nginx Load Balancer"
    )
    
    local healthy=0
    local total=${#services[@]}
    
    for service in "${services[@]}"; do
        IFS='|' read -r url name <<< "$service"
        
        if curl -s --max-time 10 "$url" > /dev/null 2>&1; then
            log_success "$name is healthy"
            ((healthy++))
        else
            log_warning "$name is not responding"
        fi
    done
    
    log_info "Health check completed: $healthy/$total services healthy"
    
    if [ "$healthy" -eq "$total" ]; then
        log_success "All services are healthy!"
        return 0
    else
        log_warning "Some services are not healthy. Please check the logs."
        return 1
    fi
}

# Show service URLs
show_service_urls() {
    log "🔗 Service URLs:"
    
    echo -e "${CYAN}"
    cat << 'EOF'
╔══════════════════════════════════════════════════════════════════════════════════════════════════════════╗
║                                           🔗 Service URLs                                                ║
╠══════════════════════════════════════════════════════════════════════════════════════════════════════════╣
║                                                                                                          ║
║  🏦 Banking Applications:                                                                                ║
║    • Main Application:        https://localhost                                                         ║
║    • API Gateway:             https://localhost/api                                                     ║
║    • Customer Service:        https://localhost/customers                                               ║
║    • Loan Service:            https://localhost/loans                                                   ║
║    • Payment Service:         https://localhost/payments                                                ║
║    • Open Banking Gateway:    https://localhost/open-banking                                            ║
║    • ML Anomaly Service:      https://localhost/ml                                                      ║
║    • Federation Monitoring:   https://localhost/federation                                             ║
║                                                                                                          ║
║  🔐 Identity & Security:                                                                                ║
║    • Keycloak (OAuth 2.1):    http://localhost:8080                                                    ║
║    • Party Data Server:       http://localhost:8081                                                    ║
║    • Authentication:          https://localhost/auth                                                    ║
║                                                                                                          ║
║  📊 Monitoring & Observability:                                                                        ║
║    • Grafana Dashboards:      https://localhost/grafana (admin/admin)                                  ║
║    • Prometheus Metrics:      https://localhost/prometheus                                              ║
║    • Kibana Logging:          https://localhost/kibana                                                  ║
║    • Jaeger Tracing:          https://localhost/jaeger                                                  ║
║                                                                                                          ║
║  🏗️  Infrastructure:                                                                                   ║
║    • PostgreSQL Database:     localhost:5432 (banking_user/banking_password)                          ║
║    • Redis Cache:             localhost:6379 (banking_password)                                        ║
║    • Kafka Message Broker:    localhost:9092                                                           ║
║    • Elasticsearch:           localhost:9200                                                            ║
║                                                                                                          ║
╚══════════════════════════════════════════════════════════════════════════════════════════════════════════╝
EOF
    echo -e "${NC}"
}

# Create Postman collection
create_postman_collection() {
    log "📬 Creating Postman collection..."
    
    cat > "$PROJECT_ROOT/Enterprise-Loan-Management-System.postman_collection.json" << 'EOF'
{
    "info": {
        "name": "Enterprise Loan Management System",
        "description": "Comprehensive API collection for banking operations with OAuth 2.1 + DPoP + FAPI 2.0 security",
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
    },
    "auth": {
        "type": "oauth2",
        "oauth2": [
            {
                "key": "tokenName",
                "value": "Banking Access Token",
                "type": "string"
            },
            {
                "key": "callBackUrl",
                "value": "https://localhost/auth/callback",
                "type": "string"
            },
            {
                "key": "authUrl",
                "value": "http://localhost:8080/realms/banking/protocol/openid-connect/auth",
                "type": "string"
            },
            {
                "key": "accessTokenUrl",
                "value": "http://localhost:8080/realms/banking/protocol/openid-connect/token",
                "type": "string"
            },
            {
                "key": "clientId",
                "value": "banking-app",
                "type": "string"
            },
            {
                "key": "clientSecret",
                "value": "banking-secret",
                "type": "string"
            },
            {
                "key": "scope",
                "value": "openid profile email banking",
                "type": "string"
            },
            {
                "key": "grant_type",
                "value": "authorization_code",
                "type": "string"
            }
        ]
    },
    "item": [
        {
            "name": "Authentication",
            "item": [
                {
                    "name": "Get OAuth Token",
                    "request": {
                        "method": "POST",
                        "header": [
                            {
                                "key": "Content-Type",
                                "value": "application/x-www-form-urlencoded"
                            }
                        ],
                        "body": {
                            "mode": "urlencoded",
                            "urlencoded": [
                                {
                                    "key": "grant_type",
                                    "value": "client_credentials"
                                },
                                {
                                    "key": "client_id",
                                    "value": "banking-app"
                                },
                                {
                                    "key": "client_secret",
                                    "value": "banking-secret"
                                },
                                {
                                    "key": "scope",
                                    "value": "banking"
                                }
                            ]
                        },
                        "url": {
                            "raw": "http://localhost:8080/realms/banking/protocol/openid-connect/token",
                            "protocol": "http",
                            "host": ["localhost"],
                            "port": "8080",
                            "path": ["realms", "banking", "protocol", "openid-connect", "token"]
                        }
                    }
                }
            ]
        },
        {
            "name": "Customer Management",
            "item": [
                {
                    "name": "Get All Customers",
                    "request": {
                        "method": "GET",
                        "header": [
                            {
                                "key": "Authorization",
                                "value": "Bearer {{access_token}}"
                            }
                        ],
                        "url": {
                            "raw": "https://localhost/customers",
                            "protocol": "https",
                            "host": ["localhost"],
                            "path": ["customers"]
                        }
                    }
                },
                {
                    "name": "Get Customer by ID",
                    "request": {
                        "method": "GET",
                        "header": [
                            {
                                "key": "Authorization",
                                "value": "Bearer {{access_token}}"
                            }
                        ],
                        "url": {
                            "raw": "https://localhost/customers/110e8400-e29b-41d4-a716-446655440001",
                            "protocol": "https",
                            "host": ["localhost"],
                            "path": ["customers", "110e8400-e29b-41d4-a716-446655440001"]
                        }
                    }
                },
                {
                    "name": "Create Customer",
                    "request": {
                        "method": "POST",
                        "header": [
                            {
                                "key": "Authorization",
                                "value": "Bearer {{access_token}}"
                            },
                            {
                                "key": "Content-Type",
                                "value": "application/json"
                            }
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n    \"firstName\": \"John\",\n    \"lastName\": \"Doe\",\n    \"email\": \"john.doe@example.com\",\n    \"phone\": \"+1-555-0123\",\n    \"dateOfBirth\": \"1985-05-15\",\n    \"address\": {\n        \"street\": \"123 Main St\",\n        \"city\": \"New York\",\n        \"state\": \"NY\",\n        \"zipCode\": \"10001\",\n        \"country\": \"USA\"\n    }\n}"
                        },
                        "url": {
                            "raw": "https://localhost/customers",
                            "protocol": "https",
                            "host": ["localhost"],
                            "path": ["customers"]
                        }
                    }
                }
            ]
        },
        {
            "name": "Loan Management",
            "item": [
                {
                    "name": "Get All Loans",
                    "request": {
                        "method": "GET",
                        "header": [
                            {
                                "key": "Authorization",
                                "value": "Bearer {{access_token}}"
                            }
                        ],
                        "url": {
                            "raw": "https://localhost/loans",
                            "protocol": "https",
                            "host": ["localhost"],
                            "path": ["loans"]
                        }
                    }
                },
                {
                    "name": "Submit Loan Application",
                    "request": {
                        "method": "POST",
                        "header": [
                            {
                                "key": "Authorization",
                                "value": "Bearer {{access_token}}"
                            },
                            {
                                "key": "Content-Type",
                                "value": "application/json"
                            }
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n    \"customerId\": \"110e8400-e29b-41d4-a716-446655440001\",\n    \"loanType\": \"PERSONAL\",\n    \"requestedAmount\": 25000.00,\n    \"termMonths\": 60,\n    \"purposeOfLoan\": \"Home renovation\",\n    \"annualIncome\": 75000.00,\n    \"employmentStatus\": \"EMPLOYED\"\n}"
                        },
                        "url": {
                            "raw": "https://localhost/loans/applications",
                            "protocol": "https",
                            "host": ["localhost"],
                            "path": ["loans", "applications"]
                        }
                    }
                },
                {
                    "name": "Get Loan Details",
                    "request": {
                        "method": "GET",
                        "header": [
                            {
                                "key": "Authorization",
                                "value": "Bearer {{access_token}}"
                            }
                        ],
                        "url": {
                            "raw": "https://localhost/loans/990e8400-e29b-41d4-a716-446655440001",
                            "protocol": "https",
                            "host": ["localhost"],
                            "path": ["loans", "990e8400-e29b-41d4-a716-446655440001"]
                        }
                    }
                }
            ]
        },
        {
            "name": "Payment Processing",
            "item": [
                {
                    "name": "Process Payment",
                    "request": {
                        "method": "POST",
                        "header": [
                            {
                                "key": "Authorization",
                                "value": "Bearer {{access_token}}"
                            },
                            {
                                "key": "Content-Type",
                                "value": "application/json"
                            }
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n    \"loanId\": \"990e8400-e29b-41d4-a716-446655440001\",\n    \"amount\": 486.87,\n    \"paymentMethod\": \"BANK_TRANSFER\",\n    \"paymentDate\": \"2024-05-01\",\n    \"description\": \"Monthly loan payment\"\n}"
                        },
                        "url": {
                            "raw": "https://localhost/payments",
                            "protocol": "https",
                            "host": ["localhost"],
                            "path": ["payments"]
                        }
                    }
                },
                {
                    "name": "Get Payment History",
                    "request": {
                        "method": "GET",
                        "header": [
                            {
                                "key": "Authorization",
                                "value": "Bearer {{access_token}}"
                            }
                        ],
                        "url": {
                            "raw": "https://localhost/payments/history?loanId=990e8400-e29b-41d4-a716-446655440001",
                            "protocol": "https",
                            "host": ["localhost"],
                            "path": ["payments", "history"],
                            "query": [
                                {
                                    "key": "loanId",
                                    "value": "990e8400-e29b-41d4-a716-446655440001"
                                }
                            ]
                        }
                    }
                }
            ]
        },
        {
            "name": "Open Banking",
            "item": [
                {
                    "name": "Account Information",
                    "request": {
                        "method": "GET",
                        "header": [
                            {
                                "key": "Authorization",
                                "value": "Bearer {{access_token}}"
                            },
                            {
                                "key": "x-fapi-auth-date",
                                "value": "{{$timestamp}}"
                            },
                            {
                                "key": "x-fapi-customer-ip-address",
                                "value": "192.168.1.1"
                            },
                            {
                                "key": "x-fapi-interaction-id",
                                "value": "{{$guid}}"
                            }
                        ],
                        "url": {
                            "raw": "https://localhost/open-banking/accounts/110e8400-e29b-41d4-a716-446655440001",
                            "protocol": "https",
                            "host": ["localhost"],
                            "path": ["open-banking", "accounts", "110e8400-e29b-41d4-a716-446655440001"]
                        }
                    }
                },
                {
                    "name": "Payment Initiation",
                    "request": {
                        "method": "POST",
                        "header": [
                            {
                                "key": "Authorization",
                                "value": "Bearer {{access_token}}"
                            },
                            {
                                "key": "Content-Type",
                                "value": "application/json"
                            },
                            {
                                "key": "x-fapi-auth-date",
                                "value": "{{$timestamp}}"
                            },
                            {
                                "key": "x-fapi-customer-ip-address",
                                "value": "192.168.1.1"
                            },
                            {
                                "key": "x-fapi-interaction-id",
                                "value": "{{$guid}}"
                            }
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n    \"Data\": {\n        \"Initiation\": {\n            \"InstructionIdentification\": \"ACME412\",\n            \"EndToEndIdentification\": \"FRESCO.21302.GFX.20\",\n            \"InstructedAmount\": {\n                \"Amount\": \"486.87\",\n                \"Currency\": \"USD\"\n            },\n            \"DebtorAccount\": {\n                \"SchemeName\": \"UK.OBIE.SortCodeAccountNumber\",\n                \"Identification\": \"08080021325698\",\n                \"Name\": \"ACME Inc\",\n                \"SecondaryIdentification\": \"0002\"\n            },\n            \"CreditorAccount\": {\n                \"SchemeName\": \"UK.OBIE.SortCodeAccountNumber\",\n                \"Identification\": \"08080021325677\",\n                \"Name\": \"ACME Inc\"\n            },\n            \"RemittanceInformation\": {\n                \"Reference\": \"FRESCO-101\",\n                \"Unstructured\": \"Internal ops code 5120101\"\n            }\n        }\n    },\n    \"Risk\": {\n        \"PaymentContextCode\": \"EcommerceGoods\",\n        \"MerchantCategoryCode\": \"5967\",\n        \"MerchantCustomerIdentification\": \"053598653254\",\n        \"DeliveryAddress\": {\n            \"AddressLine\": [\n                \"Flat 7\",\n                \"Acacia Lodge\"\n            ],\n            \"StreetName\": \"Acacia Avenue\",\n            \"BuildingNumber\": \"27\",\n            \"PostCode\": \"GU31 2ZZ\",\n            \"TownName\": \"Sparsholt\",\n            \"CountrySubDivision\": [\n                \"Wessex\"\n            ],\n            \"Country\": \"UK\"\n        }\n    }\n}"
                        },
                        "url": {
                            "raw": "https://localhost/open-banking/payment-initiation",
                            "protocol": "https",
                            "host": ["localhost"],
                            "path": ["open-banking", "payment-initiation"]
                        }
                    }
                }
            ]
        },
        {
            "name": "ML & Analytics",
            "item": [
                {
                    "name": "Fraud Detection",
                    "request": {
                        "method": "POST",
                        "header": [
                            {
                                "key": "Authorization",
                                "value": "Bearer {{access_token}}"
                            },
                            {
                                "key": "Content-Type",
                                "value": "application/json"
                            }
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n    \"transactionId\": \"TXN123456\",\n    \"customerId\": \"110e8400-e29b-41d4-a716-446655440001\",\n    \"amount\": 1000.00,\n    \"currency\": \"USD\",\n    \"merchantCategory\": \"5967\",\n    \"transactionTime\": \"2024-05-01T10:30:00Z\",\n    \"location\": {\n        \"country\": \"US\",\n        \"city\": \"New York\",\n        \"ipAddress\": \"192.168.1.100\"\n    }\n}"
                        },
                        "url": {
                            "raw": "https://localhost/ml/fraud-detection",
                            "protocol": "https",
                            "host": ["localhost"],
                            "path": ["ml", "fraud-detection"]
                        }
                    }
                },
                {
                    "name": "Risk Assessment",
                    "request": {
                        "method": "POST",
                        "header": [
                            {
                                "key": "Authorization",
                                "value": "Bearer {{access_token}}"
                            },
                            {
                                "key": "Content-Type",
                                "value": "application/json"
                            }
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n    \"customerId\": \"110e8400-e29b-41d4-a716-446655440001\",\n    \"loanAmount\": 25000.00,\n    \"loanTerm\": 60,\n    \"loanPurpose\": \"HOME_IMPROVEMENT\",\n    \"customerData\": {\n        \"annualIncome\": 75000.00,\n        \"employmentStatus\": \"EMPLOYED\",\n        \"creditScore\": 750,\n        \"debtToIncomeRatio\": 0.25\n    }\n}"
                        },
                        "url": {
                            "raw": "https://localhost/ml/risk-assessment",
                            "protocol": "https",
                            "host": ["localhost"],
                            "path": ["ml", "risk-assessment"]
                        }
                    }
                }
            ]
        },
        {
            "name": "Federation Monitoring",
            "item": [
                {
                    "name": "Federation Status",
                    "request": {
                        "method": "GET",
                        "header": [
                            {
                                "key": "Authorization",
                                "value": "Bearer {{access_token}}"
                            }
                        ],
                        "url": {
                            "raw": "https://localhost/federation/status?regions=us-east-1,eu-west-1,ap-southeast-1",
                            "protocol": "https",
                            "host": ["localhost"],
                            "path": ["federation", "status"],
                            "query": [
                                {
                                    "key": "regions",
                                    "value": "us-east-1,eu-west-1,ap-southeast-1"
                                }
                            ]
                        }
                    }
                },
                {
                    "name": "Global Dashboard",
                    "request": {
                        "method": "GET",
                        "header": [
                            {
                                "key": "Authorization",
                                "value": "Bearer {{access_token}}"
                            }
                        ],
                        "url": {
                            "raw": "https://localhost/federation/dashboard?regions=us-east-1,eu-west-1,ap-southeast-1",
                            "protocol": "https",
                            "host": ["localhost"],
                            "path": ["federation", "dashboard"],
                            "query": [
                                {
                                    "key": "regions",
                                    "value": "us-east-1,eu-west-1,ap-southeast-1"
                                }
                            ]
                        }
                    }
                }
            ]
        },
        {
            "name": "Health & Monitoring",
            "item": [
                {
                    "name": "System Health",
                    "request": {
                        "method": "GET",
                        "url": {
                            "raw": "https://localhost/api/actuator/health",
                            "protocol": "https",
                            "host": ["localhost"],
                            "path": ["api", "actuator", "health"]
                        }
                    }
                },
                {
                    "name": "System Metrics",
                    "request": {
                        "method": "GET",
                        "url": {
                            "raw": "https://localhost/api/actuator/metrics",
                            "protocol": "https",
                            "host": ["localhost"],
                            "path": ["api", "actuator", "metrics"]
                        }
                    }
                }
            ]
        }
    ]
}
EOF
    
    log_success "Postman collection created: Enterprise-Loan-Management-System.postman_collection.json"
}

# Show logs
show_logs() {
    log "📋 Showing recent logs..."
    
    cd "$PROJECT_ROOT"
    
    echo -e "${CYAN}=== Recent Application Logs ===${NC}"
    docker-compose logs --tail=10 party-data-server api-gateway customer-service loan-service payment-service
    
    echo -e "${CYAN}=== Recent Infrastructure Logs ===${NC}"
    docker-compose logs --tail=5 postgres redis kafka keycloak
}

# Cleanup function
cleanup() {
    log "🧹 Cleaning up..."
    
    cd "$PROJECT_ROOT"
    
    # Stop all containers
    docker-compose down -v
    
    # Remove unused images
    docker image prune -f
    
    # Remove unused volumes
    docker volume prune -f
    
    log_success "Cleanup completed"
}

# Main execution
main() {
    case "${1:-deploy}" in
        "deploy")
            check_prerequisites
            generate_certificates
            initialize_infrastructure
            build_application
            start_core_services
            initialize_data
            start_application_services
            start_load_balancer
            create_postman_collection
            
            echo -e "${GREEN}"
            echo "🎉 Enterprise Loan Management System deployed successfully!"
            echo -e "${NC}"
            
            health_check
            show_service_urls
            ;;
        
        "start")
            cd "$PROJECT_ROOT"
            docker-compose up -d
            log_success "All services started"
            ;;
        
        "stop")
            cd "$PROJECT_ROOT"
            docker-compose down
            log_success "All services stopped"
            ;;
        
        "restart")
            cd "$PROJECT_ROOT"
            docker-compose restart
            log_success "All services restarted"
            ;;
        
        "health")
            health_check
            ;;
        
        "logs")
            show_logs
            ;;
        
        "urls")
            show_service_urls
            ;;
        
        "cleanup")
            cleanup
            ;;
        
        "help"|"-h"|"--help")
            echo "Enterprise Loan Management System - Deployment Script"
            echo ""
            echo "Usage: $0 [command]"
            echo ""
            echo "Commands:"
            echo "  deploy    - Full deployment (default)"
            echo "  start     - Start all services"
            echo "  stop      - Stop all services"
            echo "  restart   - Restart all services"
            echo "  health    - Check service health"
            echo "  logs      - Show recent logs"
            echo "  urls      - Show service URLs"
            echo "  cleanup   - Clean up containers and volumes"
            echo "  help      - Show this help message"
            ;;
        
        *)
            log_error "Unknown command: $1"
            echo "Use '$0 help' for available commands"
            exit 1
            ;;
    esac
}

# Execute main function
main "$@"