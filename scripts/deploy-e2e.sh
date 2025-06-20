#!/bin/bash

# ===================================================================
# Enterprise Loan Management System - End-to-End Deployment Script
# ===================================================================
# Comprehensive deployment pipeline with testing and validation
# Author: Banking System DevOps Team
# Version: 1.0.0
# ===================================================================

set -euo pipefail

# Color codes for output
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly PURPLE='\033[0;35m'
readonly CYAN='\033[0;36m'
readonly NC='\033[0m' # No Color

# Configuration
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
readonly TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
readonly LOG_FILE="${PROJECT_ROOT}/logs/deployment_${TIMESTAMP}.log"

# Deployment Configuration
readonly ENVIRONMENT="${ENVIRONMENT:-local}"
readonly SKIP_TESTS="${SKIP_TESTS:-false}"
readonly SKIP_INTEGRATION_TESTS="${SKIP_INTEGRATION_TESTS:-false}"
readonly SKIP_E2E_TESTS="${SKIP_E2E_TESTS:-false}"
readonly FORCE_REBUILD="${FORCE_REBUILD:-false}"
readonly ENABLE_MONITORING="${ENABLE_MONITORING:-true}"
readonly ENABLE_LDAP="${ENABLE_LDAP:-true}"
readonly PARALLEL_EXECUTION="${PARALLEL_EXECUTION:-true}"

# Service Health Check Configuration
readonly HEALTH_CHECK_TIMEOUT=300
readonly HEALTH_CHECK_INTERVAL=5
readonly MAX_RETRIES=60

# ===================================================================
# Utility Functions
# ===================================================================

log() {
    local level="$1"
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    case "$level" in
        "INFO")  echo -e "${GREEN}[INFO]${NC} ${timestamp} - $message" | tee -a "$LOG_FILE" ;;
        "WARN")  echo -e "${YELLOW}[WARN]${NC} ${timestamp} - $message" | tee -a "$LOG_FILE" ;;
        "ERROR") echo -e "${RED}[ERROR]${NC} ${timestamp} - $message" | tee -a "$LOG_FILE" ;;
        "DEBUG") echo -e "${BLUE}[DEBUG]${NC} ${timestamp} - $message" | tee -a "$LOG_FILE" ;;
        "SUCCESS") echo -e "${GREEN}[SUCCESS]${NC} ${timestamp} - $message" | tee -a "$LOG_FILE" ;;
        "STEP") echo -e "${PURPLE}[STEP]${NC} ${timestamp} - $message" | tee -a "$LOG_FILE" ;;
    esac
}

show_banner() {
    echo -e "${CYAN}"
    cat << 'EOF'
    ╔══════════════════════════════════════════════════════════════════╗
    ║                                                                  ║
    ║    🏦 ENTERPRISE LOAN MANAGEMENT SYSTEM - E2E DEPLOYMENT 🏦     ║
    ║                                                                  ║
    ║    • Comprehensive Banking Application Stack                     ║
    ║    • Microservices Architecture with DDD                        ║
    ║    • Full Test Suite Integration                                 ║
    ║    • Banking Compliance & Security                              ║
    ║    • Production-Ready Infrastructure                             ║
    ║                                                                  ║
    ╚══════════════════════════════════════════════════════════════════╝
EOF
    echo -e "${NC}"
}

check_prerequisites() {
    log "STEP" "Checking prerequisites..."
    
    local required_tools=("docker" "docker-compose" "curl" "jq" "java" "gradle")
    local missing_tools=()
    
    for tool in "${required_tools[@]}"; do
        if ! command -v "$tool" &> /dev/null; then
            missing_tools+=("$tool")
        fi
    done
    
    if [ ${#missing_tools[@]} -ne 0 ]; then
        log "ERROR" "Missing required tools: ${missing_tools[*]}"
        log "INFO" "Please install the missing tools and try again"
        exit 1
    fi
    
    # Check Docker daemon
    if ! docker info &> /dev/null; then
        log "ERROR" "Docker daemon is not running. Please start Docker and try again."
        exit 1
    fi
    
    # Check available disk space (minimum 10GB)
    local available_space=$(df "$PROJECT_ROOT" | awk 'NR==2 {print $4}')
    local min_space=$((10 * 1024 * 1024)) # 10GB in KB
    
    if [ "$available_space" -lt "$min_space" ]; then
        log "WARN" "Low disk space detected. Available: $(($available_space / 1024 / 1024))GB, Minimum: 10GB"
    fi
    
    log "SUCCESS" "All prerequisites satisfied"
}

setup_environment() {
    log "STEP" "Setting up deployment environment..."
    
    # Create necessary directories
    mkdir -p "${PROJECT_ROOT}/logs"
    mkdir -p "${PROJECT_ROOT}/data/test-fixtures"
    mkdir -p "${PROJECT_ROOT}/data/init-scripts"
    mkdir -p "${PROJECT_ROOT}/monitoring/prometheus"
    mkdir -p "${PROJECT_ROOT}/monitoring/grafana"
    mkdir -p "${PROJECT_ROOT}/config/keycloak/realms"
    mkdir -p "${PROJECT_ROOT}/config/ldap/bootstrap"
    
    # Set environment variables for Docker Compose
    export COMPOSE_PROJECT_NAME="banking-${ENVIRONMENT}"
    export COMPOSE_FILE="${PROJECT_ROOT}/docker-compose.yml"
    
    if [ "$ENVIRONMENT" = "test" ]; then
        export COMPOSE_FILE="${PROJECT_ROOT}/docker-compose.test.yml"
    fi
    
    # Load environment-specific configuration
    if [ -f "${PROJECT_ROOT}/.env.${ENVIRONMENT}" ]; then
        log "INFO" "Loading environment configuration from .env.${ENVIRONMENT}"
        set -a
        source "${PROJECT_ROOT}/.env.${ENVIRONMENT}"
        set +a
    fi
    
    log "SUCCESS" "Environment setup completed"
}

cleanup_previous_deployment() {
    log "STEP" "Cleaning up previous deployment..."
    
    # Stop and remove containers
    docker-compose -f "$COMPOSE_FILE" down --remove-orphans --volumes 2>/dev/null || true
    
    # Clean up orphaned containers
    docker container prune -f >/dev/null 2>&1 || true
    
    # Clean up unused networks
    docker network prune -f >/dev/null 2>&1 || true
    
    if [ "$FORCE_REBUILD" = "true" ]; then
        log "INFO" "Force rebuild enabled - removing images and volumes"
        docker-compose -f "$COMPOSE_FILE" down --rmi all --volumes 2>/dev/null || true
        docker volume prune -f >/dev/null 2>&1 || true
        docker image prune -f >/dev/null 2>&1 || true
    fi
    
    log "SUCCESS" "Cleanup completed"
}

build_application() {
    log "STEP" "Building application..."
    
    cd "$PROJECT_ROOT"
    
    # Clean and build with Gradle
    log "INFO" "Running Gradle clean build..."
    ./gradlew clean build -x test --parallel --build-cache
    
    # Build Docker images
    log "INFO" "Building Docker images..."
    if [ "$PARALLEL_EXECUTION" = "true" ]; then
        docker-compose -f "$COMPOSE_FILE" build --parallel --progress=plain
    else
        docker-compose -f "$COMPOSE_FILE" build --progress=plain
    fi
    
    log "SUCCESS" "Application build completed"
}

initialize_test_data() {
    log "STEP" "Initializing test data and fixtures..."
    
    # Create test data initialization script
    cat > "${PROJECT_ROOT}/data/init-scripts/00-init-test-data.sql" << 'EOF'
-- ===================================================================
-- Banking System Test Data Initialization
-- ===================================================================

-- Create test customers
INSERT INTO customers (customer_id, first_name, last_name, email, phone, date_of_birth, customer_type, status, created_at, updated_at) VALUES
('CUST-001', 'John', 'Doe', 'john.doe@email.com', '+1-555-0101', '1985-03-15', 'INDIVIDUAL', 'ACTIVE', NOW(), NOW()),
('CUST-002', 'Jane', 'Smith', 'jane.smith@email.com', '+1-555-0102', '1990-07-22', 'INDIVIDUAL', 'ACTIVE', NOW(), NOW()),
('CUST-003', 'Robert', 'Johnson', 'robert.johnson@email.com', '+1-555-0103', '1978-11-08', 'INDIVIDUAL', 'ACTIVE', NOW(), NOW()),
('CUST-004', 'Emily', 'Davis', 'emily.davis@email.com', '+1-555-0104', '1992-04-30', 'INDIVIDUAL', 'ACTIVE', NOW(), NOW()),
('CUST-005', 'Michael', 'Wilson', 'michael.wilson@email.com', '+1-555-0105', '1983-09-12', 'INDIVIDUAL', 'ACTIVE', NOW(), NOW()),
('CORP-001', 'Tech Solutions Inc', '', 'finance@techsolutions.com', '+1-555-0201', '2010-01-01', 'CORPORATE', 'ACTIVE', NOW(), NOW()),
('CORP-002', 'Green Energy Corp', '', 'accounting@greenenergy.com', '+1-555-0202', '2015-05-15', 'CORPORATE', 'ACTIVE', NOW(), NOW());

-- Create test addresses
INSERT INTO customer_addresses (street, city, state, zip_code, country, type, is_primary, customer_id) VALUES
('123 Main Street', 'New York', 'NY', '10001', 'USA', 'HOME', true, 1),
('456 Oak Avenue', 'Los Angeles', 'CA', '90210', 'USA', 'HOME', true, 2),
('789 Pine Road', 'Chicago', 'IL', '60601', 'USA', 'HOME', true, 3),
('321 Elm Street', 'Houston', 'TX', '77001', 'USA', 'HOME', true, 4),
('654 Maple Drive', 'Phoenix', 'AZ', '85001', 'USA', 'HOME', true, 5),
('1000 Corporate Blvd', 'San Francisco', 'CA', '94105', 'USA', 'BUSINESS', true, 6),
('2000 Green Way', 'Seattle', 'WA', '98101', 'USA', 'BUSINESS', true, 7);

-- Create test credit scores
INSERT INTO credit_scores (customer_id, score, reporting_agency, last_updated) VALUES
(1, 750, 'EXPERIAN', NOW()),
(2, 680, 'EQUIFAX', NOW()),
(3, 720, 'TRANSUNION', NOW()),
(4, 800, 'EXPERIAN', NOW()),
(5, 650, 'EQUIFAX', NOW()),
(6, 780, 'EXPERIAN', NOW()),
(7, 820, 'TRANSUNION', NOW());

-- Create test loans
INSERT INTO loans (loan_id, customer_id, loan_type, principal_amount, currency, interest_rate, term_months, monthly_payment, status, application_date, approved_date, disbursed_date, created_at, updated_at) VALUES
('LOAN-001', 'CUST-001', 'PERSONAL', 25000.00, 'USD', 0.0575, 60, 478.66, 'ACTIVE', '2024-01-15', '2024-01-18', '2024-01-20', NOW(), NOW()),
('LOAN-002', 'CUST-002', 'AUTO', 35000.00, 'USD', 0.0425, 72, 548.32, 'ACTIVE', '2024-02-01', '2024-02-03', '2024-02-05', NOW(), NOW()),
('LOAN-003', 'CUST-003', 'MORTGAGE', 450000.00, 'USD', 0.0375, 360, 2072.24, 'ACTIVE', '2024-01-10', '2024-01-25', '2024-02-01', NOW(), NOW()),
('LOAN-004', 'CUST-004', 'PERSONAL', 15000.00, 'USD', 0.0625, 48, 355.48, 'PENDING', '2024-03-01', NULL, NULL, NOW(), NOW()),
('LOAN-005', 'CORP-001', 'BUSINESS', 500000.00, 'USD', 0.0485, 120, 5247.89, 'ACTIVE', '2024-01-05', '2024-01-12', '2024-01-15', NOW(), NOW());

-- Create test payments
INSERT INTO payments (payment_id, loan_id, customer_id, payment_amount, payment_currency, payment_date, status, payment_method, payment_reference, description, created_at, updated_at) VALUES
('PAY-001', 'LOAN-001', 'CUST-001', 478.66, 'USD', '2024-02-20', 'PROCESSED', 'BANK_TRANSFER', 'TXN-20240220-001', 'Monthly payment February', NOW(), NOW()),
('PAY-002', 'LOAN-001', 'CUST-001', 478.66, 'USD', '2024-03-20', 'PROCESSED', 'BANK_TRANSFER', 'TXN-20240320-001', 'Monthly payment March', NOW(), NOW()),
('PAY-003', 'LOAN-002', 'CUST-002', 548.32, 'USD', '2024-03-05', 'PROCESSED', 'ACH', 'TXN-20240305-001', 'Monthly payment March', NOW(), NOW()),
('PAY-004', 'LOAN-003', 'CUST-003', 2072.24, 'USD', '2024-03-01', 'PROCESSED', 'BANK_TRANSFER', 'TXN-20240301-001', 'Monthly mortgage payment', NOW(), NOW()),
('PAY-005', 'CORP-001', 'CORP-001', 5247.89, 'USD', '2024-02-15', 'PROCESSED', 'WIRE_TRANSFER', 'TXN-20240215-001', 'Business loan payment', NOW(), NOW());

-- Create test loan installments
INSERT INTO loan_installments (loan_id, installment_number, due_date, amount, currency, status, paid_date, paid_amount, created_at, updated_at) VALUES
('LOAN-001', 1, '2024-02-20', 478.66, 'USD', 'PAID', '2024-02-20', 478.66, NOW(), NOW()),
('LOAN-001', 2, '2024-03-20', 478.66, 'USD', 'PAID', '2024-03-20', 478.66, NOW(), NOW()),
('LOAN-001', 3, '2024-04-20', 478.66, 'USD', 'PENDING', NULL, NULL, NOW(), NOW()),
('LOAN-002', 1, '2024-03-05', 548.32, 'USD', 'PAID', '2024-03-05', 548.32, NOW(), NOW()),
('LOAN-002', 2, '2024-04-05', 548.32, 'USD', 'PENDING', NULL, NULL, NOW(), NOW()),
('LOAN-003', 1, '2024-03-01', 2072.24, 'USD', 'PAID', '2024-03-01', 2072.24, NOW(), NOW()),
('LOAN-003', 2, '2024-04-01', 2072.24, 'USD', 'PENDING', NULL, NULL, NOW(), NOW());

-- Create test financial profiles for loan recommendations
INSERT INTO customer_financial_profiles (customer_id, annual_income, employment_status, employment_years, debt_to_income_ratio, credit_utilization, preferred_loan_term, max_loan_amount, created_at, updated_at) VALUES
('CUST-001', 75000.00, 'EMPLOYED', 5, 0.35, 0.25, 60, 30000.00, NOW(), NOW()),
('CUST-002', 85000.00, 'EMPLOYED', 8, 0.28, 0.15, 72, 40000.00, NOW(), NOW()),
('CUST-003', 120000.00, 'EMPLOYED', 12, 0.42, 0.20, 360, 500000.00, NOW(), NOW()),
('CUST-004', 95000.00, 'EMPLOYED', 3, 0.25, 0.10, 48, 20000.00, NOW(), NOW()),
('CUST-005', 65000.00, 'EMPLOYED', 7, 0.38, 0.30, 60, 25000.00, NOW(), NOW());

-- Insert sample loan recommendations
INSERT INTO loan_recommendations (customer_id, recommended_loan_type, recommended_amount, recommended_term, estimated_interest_rate, monthly_payment, confidence_score, reasoning, created_at) VALUES
('CUST-004', 'PERSONAL', 18000.00, 48, 0.0575, 426.58, 0.85, 'Good credit score and stable income support personal loan approval', NOW()),
('CUST-005', 'AUTO', 28000.00, 60, 0.0450, 521.34, 0.78, 'Credit score and employment history indicate auto loan eligibility', NOW());

-- Create audit log entries
INSERT INTO audit_logs (entity_type, entity_id, action, user_id, timestamp, details) VALUES
('LOAN', 'LOAN-001', 'CREATED', 'SYSTEM', NOW(), 'Loan application created for customer CUST-001'),
('LOAN', 'LOAN-001', 'APPROVED', 'UNDERWRITER-001', NOW(), 'Loan approved after credit check and income verification'),
('PAYMENT', 'PAY-001', 'PROCESSED', 'SYSTEM', NOW(), 'Payment processed successfully via bank transfer'),
('CUSTOMER', 'CUST-001', 'PROFILE_UPDATED', 'CUST-001', NOW(), 'Customer profile information updated');

COMMIT;
EOF

    # Create Keycloak realm configuration
    cat > "${PROJECT_ROOT}/config/keycloak/realms/banking-realm.json" << 'EOF'
{
  "realm": "banking-realm",
  "enabled": true,
  "displayName": "Banking Enterprise Realm",
  "displayNameHtml": "<div class=\"kc-logo-text\"><span>Banking Enterprise</span></div>",
  "loginTheme": "banking",
  "adminTheme": "banking",
  "accountTheme": "banking",
  "emailTheme": "banking",
  "sslRequired": "external",
  "users": [
    {
      "username": "bank-admin",
      "enabled": true,
      "email": "admin@banking.local",
      "firstName": "Bank",
      "lastName": "Administrator",
      "credentials": [{
        "type": "password",
        "value": "banking_admin_2024",
        "temporary": false
      }],
      "realmRoles": ["admin", "loan-officer"],
      "clientRoles": {
        "banking-app": ["admin", "loan-manager"]
      }
    },
    {
      "username": "loan-officer",
      "enabled": true,
      "email": "officer@banking.local",
      "firstName": "Loan",
      "lastName": "Officer",
      "credentials": [{
        "type": "password",
        "value": "officer_2024",
        "temporary": false
      }],
      "realmRoles": ["loan-officer"],
      "clientRoles": {
        "banking-app": ["loan-officer"]
      }
    },
    {
      "username": "customer1",
      "enabled": true,
      "email": "customer1@email.com",
      "firstName": "John",
      "lastName": "Customer",
      "credentials": [{
        "type": "password",
        "value": "customer_2024",
        "temporary": false
      }],
      "realmRoles": ["customer"],
      "clientRoles": {
        "banking-app": ["customer"]
      }
    }
  ],
  "roles": {
    "realm": [
      {
        "name": "admin",
        "description": "Banking system administrator"
      },
      {
        "name": "loan-officer",
        "description": "Loan processing officer"
      },
      {
        "name": "customer",
        "description": "Banking customer"
      },
      {
        "name": "underwriter",
        "description": "Loan underwriter"
      }
    ]
  },
  "clients": [
    {
      "clientId": "banking-app",
      "enabled": true,
      "clientAuthenticatorType": "client-secret",
      "secret": "banking-app-secret-2024",
      "standardFlowEnabled": true,
      "serviceAccountsEnabled": true,
      "authorizationServicesEnabled": true,
      "redirectUris": ["http://localhost:8080/*", "http://banking-app:8080/*"],
      "webOrigins": ["http://localhost:8080", "http://banking-app:8080"],
      "protocol": "openid-connect",
      "attributes": {
        "oauth2.device.authorization.grant.enabled": "false",
        "oidc.ciba.grant.enabled": "false"
      }
    }
  ]
}
EOF

    # Create LDAP bootstrap data
    cat > "${PROJECT_ROOT}/config/ldap/bootstrap/banking-users.ldif" << 'EOF'
# Banking Enterprise LDAP Bootstrap Data

# Organizational Units
dn: ou=People,dc=banking,dc=local
objectClass: organizationalUnit
ou: People

dn: ou=Groups,dc=banking,dc=local
objectClass: organizationalUnit
ou: Groups

# Groups
dn: cn=bank-admins,ou=Groups,dc=banking,dc=local
objectClass: groupOfNames
cn: bank-admins
description: Banking System Administrators
member: uid=admin,ou=People,dc=banking,dc=local

dn: cn=loan-officers,ou=Groups,dc=banking,dc=local
objectClass: groupOfNames
cn: loan-officers
description: Loan Processing Officers
member: uid=loanofficer1,ou=People,dc=banking,dc=local
member: uid=loanofficer2,ou=People,dc=banking,dc=local

dn: cn=customers,ou=Groups,dc=banking,dc=local
objectClass: groupOfNames
cn: customers
description: Banking Customers
member: uid=customer1,ou=People,dc=banking,dc=local
member: uid=customer2,ou=People,dc=banking,dc=local

# Users
dn: uid=admin,ou=People,dc=banking,dc=local
objectClass: inetOrgPerson
objectClass: posixAccount
objectClass: shadowAccount
uid: admin
cn: Banking Administrator
sn: Administrator
givenName: Banking
mail: admin@banking.local
telephoneNumber: +1-555-0001
uidNumber: 1000
gidNumber: 1000
homeDirectory: /home/admin
loginShell: /bin/bash
userPassword: {SSHA}banking_admin_ldap_2024

dn: uid=loanofficer1,ou=People,dc=banking,dc=local
objectClass: inetOrgPerson
objectClass: posixAccount
objectClass: shadowAccount
uid: loanofficer1
cn: Loan Officer One
sn: Officer
givenName: Loan
mail: loanofficer1@banking.local
telephoneNumber: +1-555-0101
uidNumber: 1001
gidNumber: 1001
homeDirectory: /home/loanofficer1
loginShell: /bin/bash
userPassword: {SSHA}officer1_ldap_2024

dn: uid=customer1,ou=People,dc=banking,dc=local
objectClass: inetOrgPerson
objectClass: posixAccount
objectClass: shadowAccount
uid: customer1
cn: John Doe
sn: Doe
givenName: John
mail: john.doe@email.com
telephoneNumber: +1-555-0201
uidNumber: 2001
gidNumber: 2001
homeDirectory: /home/customer1
loginShell: /bin/bash
userPassword: {SSHA}customer1_ldap_2024
EOF

    log "SUCCESS" "Test data initialization completed"
}

deploy_infrastructure() {
    log "STEP" "Deploying infrastructure services..."
    
    cd "$PROJECT_ROOT"
    
    # Start infrastructure services first
    local infrastructure_services=(
        "postgres"
        "redis" 
        "zookeeper"
        "kafka"
    )
    
    if [ "$ENABLE_LDAP" = "true" ]; then
        infrastructure_services+=("ldap")
    fi
    
    for service in "${infrastructure_services[@]}"; do
        log "INFO" "Starting $service..."
        docker-compose -f "$COMPOSE_FILE" up -d "$service"
        wait_for_service_health "$service"
    done
    
    # Start identity services
    if [ "$ENABLE_LDAP" = "true" ]; then
        log "INFO" "Starting identity services..."
        docker-compose -f "$COMPOSE_FILE" up -d keycloak phpldapadmin
        wait_for_service_health "keycloak"
    fi
    
    log "SUCCESS" "Infrastructure deployment completed"
}

deploy_application() {
    log "STEP" "Deploying main application..."
    
    cd "$PROJECT_ROOT"
    
    # Start the main banking application
    log "INFO" "Starting banking application..."
    docker-compose -f "$COMPOSE_FILE" up -d banking-app
    wait_for_service_health "banking-app"
    
    # Start monitoring services if enabled
    if [ "$ENABLE_MONITORING" = "true" ]; then
        log "INFO" "Starting monitoring services..."
        docker-compose -f "$COMPOSE_FILE" up -d prometheus grafana
        wait_for_service_health "prometheus"
    fi
    
    # Start API gateway
    log "INFO" "Starting API gateway..."
    docker-compose -f "$COMPOSE_FILE" up -d nginx
    wait_for_service_health "nginx"
    
    log "SUCCESS" "Application deployment completed"
}

wait_for_service_health() {
    local service_name="$1"
    local timeout="${2:-$HEALTH_CHECK_TIMEOUT}"
    local interval="${3:-$HEALTH_CHECK_INTERVAL}"
    local elapsed=0
    
    log "INFO" "Waiting for $service_name to be healthy..."
    
    while [ $elapsed -lt $timeout ]; do
        if docker-compose -f "$COMPOSE_FILE" ps "$service_name" | grep -q "healthy\|Up"; then
            log "SUCCESS" "$service_name is healthy"
            return 0
        fi
        
        sleep "$interval"
        elapsed=$((elapsed + interval))
        
        if [ $((elapsed % 30)) -eq 0 ]; then
            log "INFO" "Still waiting for $service_name... (${elapsed}s elapsed)"
        fi
    done
    
    log "ERROR" "$service_name failed to become healthy within ${timeout}s"
    return 1
}

run_tests() {
    if [ "$SKIP_TESTS" = "true" ]; then
        log "INFO" "Skipping tests as requested"
        return 0
    fi
    
    log "STEP" "Running comprehensive test suite..."
    
    cd "$PROJECT_ROOT"
    
    # Unit tests
    log "INFO" "Running unit tests..."
    ./gradlew test --parallel --continue
    
    # Integration tests
    if [ "$SKIP_INTEGRATION_TESTS" = "false" ]; then
        log "INFO" "Running integration tests..."
        ./gradlew integrationTest --continue
    fi
    
    # End-to-end tests
    if [ "$SKIP_E2E_TESTS" = "false" ]; then
        log "INFO" "Running end-to-end tests..."
        docker-compose -f "$COMPOSE_FILE" --profile e2e-testing up --abort-on-container-exit test-runner
    fi
    
    # API tests
    log "INFO" "Running API tests..."
    "${SCRIPT_DIR}/test-api-endpoints.sh"
    
    log "SUCCESS" "All tests completed successfully"
}

validate_deployment() {
    log "STEP" "Validating deployment..."
    
    local services_to_validate=(
        "banking-app:8080:/api/actuator/health"
        "keycloak:8080:/health/ready"
        "prometheus:9090:/-/healthy"
        "grafana:3000:/api/health"
    )
    
    for service_info in "${services_to_validate[@]}"; do
        IFS=':' read -r service port endpoint <<< "$service_info"
        
        local url="http://localhost:${port}${endpoint}"
        log "INFO" "Validating $service at $url"
        
        if curl -f -s --max-time 10 "$url" > /dev/null; then
            log "SUCCESS" "$service validation passed"
        else
            log "ERROR" "$service validation failed"
            return 1
        fi
    done
    
    # Validate database connectivity
    log "INFO" "Validating database connectivity..."
    if docker-compose -f "$COMPOSE_FILE" exec -T postgres pg_isready -U banking_user -d banking_system > /dev/null; then
        log "SUCCESS" "Database connectivity validated"
    else
        log "ERROR" "Database connectivity validation failed"
        return 1
    fi
    
    # Validate Kafka connectivity
    log "INFO" "Validating Kafka connectivity..."
    if docker-compose -f "$COMPOSE_FILE" exec -T kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; then
        log "SUCCESS" "Kafka connectivity validated"
    else
        log "ERROR" "Kafka connectivity validation failed"
        return 1
    fi
    
    log "SUCCESS" "Deployment validation completed"
}

show_deployment_summary() {
    log "STEP" "Deployment Summary"
    
    echo -e "${GREEN}"
    cat << 'EOF'
    ╔══════════════════════════════════════════════════════════════════╗
    ║                    🎉 DEPLOYMENT SUCCESSFUL! 🎉                  ║
    ╚══════════════════════════════════════════════════════════════════╝
EOF
    echo -e "${NC}"
    
    log "INFO" "Service Endpoints:"
    log "INFO" "  • Banking API:        http://localhost:8080/api"
    log "INFO" "  • GraphQL Playground: http://localhost:8080/graphql"
    log "INFO" "  • OpenAPI Docs:       http://localhost:8080/swagger-ui.html"
    log "INFO" "  • Keycloak Admin:     http://localhost:8090 (admin/banking_keycloak_admin_2024)"
    log "INFO" "  • LDAP Admin:         http://localhost:8091"
    log "INFO" "  • Prometheus:         http://localhost:9090"
    log "INFO" "  • Grafana:            http://localhost:3000 (admin/banking_admin_pass)"
    log "INFO" "  • API Gateway:        http://localhost:80"
    
    log "INFO" "Test Credentials:"
    log "INFO" "  • Admin User:    bank-admin / banking_admin_2024"
    log "INFO" "  • Loan Officer: loan-officer / officer_2024"
    log "INFO" "  • Customer:     customer1 / customer_2024"
    
    log "INFO" "Useful Commands:"
    log "INFO" "  • View logs:          docker-compose -f $COMPOSE_FILE logs -f"
    log "INFO" "  • Stop services:      docker-compose -f $COMPOSE_FILE down"
    log "INFO" "  • Restart service:    docker-compose -f $COMPOSE_FILE restart <service>"
    log "INFO" "  • Run tests:          ./scripts/run-tests.sh"
    log "INFO" "  • Monitor services:   docker-compose -f $COMPOSE_FILE ps"
    
    echo -e "${CYAN}For detailed logs, check: ${LOG_FILE}${NC}"
}

# ===================================================================
# Main Execution
# ===================================================================

main() {
    # Trap to ensure cleanup on exit
    trap 'log "INFO" "Deployment script interrupted"' INT TERM
    
    show_banner
    
    log "INFO" "Starting end-to-end deployment for environment: $ENVIRONMENT"
    log "INFO" "Configuration: SKIP_TESTS=$SKIP_TESTS, FORCE_REBUILD=$FORCE_REBUILD, ENABLE_MONITORING=$ENABLE_MONITORING"
    
    check_prerequisites
    setup_environment
    cleanup_previous_deployment
    build_application
    initialize_test_data
    deploy_infrastructure
    deploy_application
    run_tests
    validate_deployment
    show_deployment_summary
    
    log "SUCCESS" "End-to-end deployment completed successfully!"
}

# Handle command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --environment|-e)
            ENVIRONMENT="$2"
            shift 2
            ;;
        --skip-tests)
            SKIP_TESTS="true"
            shift
            ;;
        --skip-integration-tests)
            SKIP_INTEGRATION_TESTS="true"
            shift
            ;;
        --skip-e2e-tests)
            SKIP_E2E_TESTS="true"
            shift
            ;;
        --force-rebuild)
            FORCE_REBUILD="true"
            shift
            ;;
        --no-monitoring)
            ENABLE_MONITORING="false"
            shift
            ;;
        --no-ldap)
            ENABLE_LDAP="false"
            shift
            ;;
        --sequential)
            PARALLEL_EXECUTION="false"
            shift
            ;;
        --help|-h)
            echo "Usage: $0 [OPTIONS]"
            echo "Options:"
            echo "  --environment, -e ENV    Deployment environment (local, test, staging, production)"
            echo "  --skip-tests            Skip all tests"
            echo "  --skip-integration-tests Skip integration tests only"
            echo "  --skip-e2e-tests        Skip end-to-end tests only"
            echo "  --force-rebuild         Force rebuild of all images and volumes"
            echo "  --no-monitoring         Disable monitoring services"
            echo "  --no-ldap              Disable LDAP services"
            echo "  --sequential           Disable parallel execution"
            echo "  --help, -h             Show this help message"
            exit 0
            ;;
        *)
            log "ERROR" "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Execute main function
main "$@"