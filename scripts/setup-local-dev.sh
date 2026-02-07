#!/bin/bash

# ===================================================================
# Enterprise Loan Management System - Local Development Setup
# ===================================================================
# Complete local development environment initialization
# ===================================================================

set -euo pipefail

# Color codes
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly PURPLE='\033[0;35m'
readonly CYAN='\033[0;36m'
readonly NC='\033[0m'

# Configuration
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
readonly DEV_ENV_FILE="${PROJECT_ROOT}/.env.local"
readonly TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# Features to enable
readonly ENABLE_HOT_RELOAD="${ENABLE_HOT_RELOAD:-true}"
readonly ENABLE_DEBUG_MODE="${ENABLE_DEBUG_MODE:-true}"
readonly ENABLE_TEST_DATA="${ENABLE_TEST_DATA:-true}"
readonly ENABLE_MONITORING="${ENABLE_MONITORING:-true}"
readonly ENABLE_AI_SERVICES="${ENABLE_AI_SERVICES:-false}"

# ===================================================================
# Utility Functions
# ===================================================================

log() {
    local level="$1"
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    case "$level" in
        "INFO")  echo -e "${BLUE}[INFO]${NC} ${timestamp} - $message" ;;
        "SUCCESS") echo -e "${GREEN}[SUCCESS]${NC} ${timestamp} - $message" ;;
        "WARN")  echo -e "${YELLOW}[WARN]${NC} ${timestamp} - $message" ;;
        "ERROR") echo -e "${RED}[ERROR]${NC} ${timestamp} - $message" ;;
        "STEP")  echo -e "${PURPLE}[STEP]${NC} ${timestamp} - $message" ;;
    esac
}

show_banner() {
    echo -e "${CYAN}"
    cat << 'EOF'
    ╔══════════════════════════════════════════════════════════════════╗
    ║                                                                  ║
    ║    🛠️  LOCAL DEVELOPMENT ENVIRONMENT SETUP 🛠️                   ║
    ║                                                                  ║
    ║    • Hot Reload Development Server                               ║
    ║    • Database with Test Data                                     ║
    ║    • IDE Configuration                                           ║
    ║    • Development Tools & Scripts                                 ║
    ║    • Monitoring & Debugging                                      ║
    ║                                                                  ║
    ╚══════════════════════════════════════════════════════════════════╝
EOF
    echo -e "${NC}"
}

check_prerequisites() {
    log "STEP" "Checking development prerequisites..."
    
    local required_tools=("java" "gradle" "docker" "docker-compose" "git" "curl" "jq")
    local optional_tools=("node" "npm" "code" "idea")
    local missing_required=()
    local missing_optional=()
    
    # Check required tools
    for tool in "${required_tools[@]}"; do
        if ! command -v "$tool" &> /dev/null; then
            missing_required+=("$tool")
        fi
    done
    
    # Check optional tools
    for tool in "${optional_tools[@]}"; do
        if ! command -v "$tool" &> /dev/null; then
            missing_optional+=("$tool")
        fi
    done
    
    if [ ${#missing_required[@]} -ne 0 ]; then
        log "ERROR" "Missing required tools: ${missing_required[*]}"
        log "INFO" "Please install the missing tools and try again"
        exit 1
    fi
    
    if [ ${#missing_optional[@]} -ne 0 ]; then
        log "WARN" "Missing optional tools: ${missing_optional[*]}"
        log "INFO" "Consider installing these for better development experience"
    fi
    
    # Check Java version
    local java_version
    java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$java_version" -lt 25 ]; then
        log "ERROR" "Java 25.0.2 or higher is required. Current version: $java_version"
        exit 1
    fi
    
    log "SUCCESS" "All prerequisites satisfied"
}

create_dev_environment_file() {
    log "STEP" "Creating local development environment configuration..."
    
    cat > "$DEV_ENV_FILE" << 'EOF'
# ===================================================================
# Local Development Environment Configuration
# ===================================================================

# Spring Configuration
SPRING_PROFILES_ACTIVE=local,development,h2
SERVER_PORT=8080
DEBUG_PORT=5005

# Database Configuration (H2 for local development)
DATABASE_URL=jdbc:h2:mem:banking_dev;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
DATABASE_USERNAME=sa
DATABASE_PASSWORD=
DATABASE_DRIVER=org.h2.Driver
JPA_DDL_AUTO=create-drop
JPA_SHOW_SQL=true
H2_CONSOLE_ENABLED=true

# Redis Configuration (optional for local dev)
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_DATABASE=0

# Kafka Configuration (embedded for local dev)
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_AUTO_CREATE_TOPICS=true

# Security Configuration (relaxed for development)
BANKING_COMPLIANCE_STRICT=false
FAPI_ENABLED=false
AUDIT_ENABLED=true
KYC_REQUIRED=false
JWT_SECRET=local_development_jwt_secret_key_not_for_production_use

# Development Features
ENABLE_HOT_RELOAD=true
ENABLE_DEBUG_MODE=true
ENABLE_SWAGGER=true
ENABLE_H2_CONSOLE=true
ENABLE_ACTUATOR=true

# Logging Configuration
LOG_LEVEL_ROOT=INFO
LOG_LEVEL_APP=DEBUG
LOG_LEVEL_HIBERNATE=INFO
LOG_LEVEL_SECURITY=DEBUG
LOG_LEVEL_WEB=DEBUG

# Development Tools
ENABLE_LIVERELOAD=true
ENABLE_DEVTOOLS=true

# AI Services (optional - set to true if you have API keys)
ENABLE_AI_SERVICES=false
OPENAI_API_KEY=your_openai_api_key_here
OPENAI_MODEL=gpt-4
SPRING_AI_OPENAI_API_KEY=${OPENAI_API_KEY}

# Monitoring
PROMETHEUS_ENABLED=true
GRAFANA_ENABLED=false

# File paths
DEV_DATA_PATH=./data/dev
LOG_PATH=./logs/dev

# Performance tuning for development
JAVA_OPTS=-Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication -Djava.security.egd=file:/dev/./urandom -Dspring.profiles.active=local,development
EOF

    log "SUCCESS" "Development environment file created: $DEV_ENV_FILE"
}

setup_gradle_dev_configuration() {
    log "STEP" "Setting up Gradle development configuration..."
    
    # Create gradle.properties for development
    cat > "${PROJECT_ROOT}/gradle.properties" << 'EOF'
# Gradle configuration for development
org.gradle.jvmargs=-Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true
org.gradle.daemon=true

# Enable Gradle build cache
org.gradle.unsafe.configuration-cache=true

# Development specific settings
springBootVersion=3.3.6
testcontainersVersion=1.19.0
springCloudVersion=2023.0.0

# Enable continuous build
org.gradle.continuous=true
EOF

    # Create development-specific build tasks
    cat > "${PROJECT_ROOT}/gradle/dev-tasks.gradle" << 'EOF'
// Development-specific Gradle tasks

task runDev(type: org.springframework.boot.gradle.tasks.run.BootRun) {
    group = 'development'
    description = 'Run application in development mode'
    
    environment 'SPRING_PROFILES_ACTIVE', 'local,development,h2'
    environment 'ENABLE_HOT_RELOAD', 'true'
    environment 'ENABLE_DEBUG_MODE', 'true'
    
    jvmArgs = [
        '-Xmx2g',
        '-XX:+UseG1GC',
        '-XX:+UseStringDeduplication',
        '-Dspring.devtools.restart.enabled=true',
        '-Dspring.devtools.livereload.enabled=true',
        '-Djava.security.egd=file:/dev/./urandom'
    ]
    
    if (project.hasProperty('debug')) {
        jvmArgs += ['-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005']
    }
}

task runDebug(type: org.springframework.boot.gradle.tasks.run.BootRun) {
    group = 'development'
    description = 'Run application in debug mode'
    
    environment 'SPRING_PROFILES_ACTIVE', 'local,development,h2'
    
    jvmArgs = [
        '-Xmx2g',
        '-XX:+UseG1GC',
        '-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005',
        '-Dspring.devtools.restart.enabled=true',
        '-Dspring.devtools.livereload.enabled=true'
    ]
}

task testDev(type: Test) {
    group = 'development'
    description = 'Run tests with development profile'
    
    useJUnitPlatform()
    testLogging {
        events "passed", "skipped", "failed"
        exceptionFormat "full"
    }
    
    environment 'SPRING_PROFILES_ACTIVE', 'test,h2'
    
    systemProperty 'spring.test.context.cache.maxSize', '3'
    systemProperty 'junit.jupiter.execution.parallel.enabled', 'true'
    systemProperty 'junit.jupiter.execution.parallel.mode.default', 'concurrent'
}

task loadTestData(type: JavaExec) {
    group = 'development'
    description = 'Load test data into development database'
    
    classpath = sourceSets.main.runtimeClasspath
    mainClass = 'com.bank.loanmanagement.DevDataLoader'
    
    environment 'SPRING_PROFILES_ACTIVE', 'local,development,h2'
}

task cleanDev(type: Delete) {
    group = 'development'
    description = 'Clean development files and databases'
    
    delete 'logs/dev'
    delete 'data/dev'
    delete fileTree(dir: '.', include: '*.db')
    delete fileTree(dir: '.', include: '*.log')
}
EOF

    log "SUCCESS" "Gradle development configuration created"
}

create_dev_data_loader() {
    log "STEP" "Creating development data loader..."
    
    mkdir -p "${PROJECT_ROOT}/src/main/java/com/bank/loanmanagement"
    
    cat > "${PROJECT_ROOT}/src/main/java/com/bank/loanmanagement/DevDataLoader.java" << 'EOF'
package com.bank.loanmanagement;

import com.bank.loanmanagement.domain.customer.Customer;
import com.bank.loanmanagement.domain.customer.CustomerId;
import com.bank.loanmanagement.domain.customer.CustomerType;
import com.bank.loanmanagement.domain.customer.CustomerStatus;
import com.bank.loanmanagement.domain.customer.Address;
import com.bank.loanmanagement.domain.customer.AddressType;
import com.bank.loanmanagement.domain.customer.CreditScore;
import com.bank.loanmanagement.domain.loan.Loan;
import com.bank.loanmanagement.domain.loan.LoanId;
import com.bank.loanmanagement.domain.loan.LoanType;
import com.bank.loanmanagement.domain.loan.LoanStatus;
import com.bank.loanmanagement.domain.payment.Payment;
import com.bank.loanmanagement.domain.payment.PaymentMethod;
import com.bank.loanmanagement.domain.shared.Money;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Development data loader for creating test data in local development environment
 */
@SpringBootApplication
public class DevDataLoader {
    
    public static void main(String[] args) {
        SpringApplication.run(DevDataLoader.class, args);
    }
    
    @Component
    @Profile({"local", "development"})
    static class DataLoader implements CommandLineRunner {
        
        @Override
        @Transactional
        public void run(String... args) throws Exception {
            System.out.println("🏦 Loading development test data...");
            
            // Create test customers
            createTestCustomers();
            
            // Create test loans
            createTestLoans();
            
            // Create test payments
            createTestPayments();
            
            System.out.println("✅ Development test data loaded successfully!");
        }
        
        private void createTestCustomers() {
            // Individual customers
            Customer customer1 = Customer.builder()
                .customerId(new CustomerId("DEV-CUST-001"))
                .firstName("John")
                .lastName("Developer")
                .email("john.dev@example.com")
                .phone("+1-555-0001")
                .dateOfBirth(LocalDate.of(1985, 3, 15))
                .customerType(CustomerType.INDIVIDUAL)
                .status(CustomerStatus.ACTIVE)
                .build();
            
            customer1.addAddress(Address.builder()
                .street("123 Development Street")
                .city("Code City")
                .state("CA")
                .zipCode("90210")
                .country("USA")
                .type(AddressType.HOME)
                .isPrimary(true)
                .build());
            
            customer1.updateCreditScore(new CreditScore(750, "EXPERIAN", LocalDateTime.now()));
            
            Customer customer2 = Customer.builder()
                .customerId(new CustomerId("DEV-CUST-002"))
                .firstName("Jane")
                .lastName("Tester")
                .email("jane.test@example.com")
                .phone("+1-555-0002")
                .dateOfBirth(LocalDate.of(1990, 7, 22))
                .customerType(CustomerType.INDIVIDUAL)
                .status(CustomerStatus.ACTIVE)
                .build();
            
            customer2.addAddress(Address.builder()
                .street("456 Testing Avenue")
                .city("QA City")
                .state("NY")
                .zipCode("10001")
                .country("USA")
                .type(AddressType.HOME)
                .isPrimary(true)
                .build());
            
            customer2.updateCreditScore(new CreditScore(680, "EQUIFAX", LocalDateTime.now()));
            
            // Corporate customer
            Customer corporateCustomer = Customer.builder()
                .customerId(new CustomerId("DEV-CORP-001"))
                .firstName("Tech Startup")
                .lastName("Inc")
                .email("finance@techstartup.com")
                .phone("+1-555-0100")
                .dateOfBirth(LocalDate.of(2020, 1, 1))
                .customerType(CustomerType.CORPORATE)
                .status(CustomerStatus.ACTIVE)
                .build();
            
            corporateCustomer.addAddress(Address.builder()
                .street("789 Innovation Blvd")
                .city("Silicon Valley")
                .state("CA")
                .zipCode("94105")
                .country("USA")
                .type(AddressType.BUSINESS)
                .isPrimary(true)
                .build());
            
            corporateCustomer.updateCreditScore(new CreditScore(780, "EXPERIAN", LocalDateTime.now()));
            
            System.out.println("Created test customers: " + 
                List.of(customer1.getCustomerId(), customer2.getCustomerId(), corporateCustomer.getCustomerId()));
        }
        
        private void createTestLoans() {
            // Personal loan for customer 1
            Money personalLoanAmount = Money.of(new BigDecimal("25000.00"), "USD");
            
            // Auto loan for customer 2  
            Money autoLoanAmount = Money.of(new BigDecimal("35000.00"), "USD");
            
            // Business loan for corporate customer
            Money businessLoanAmount = Money.of(new BigDecimal("500000.00"), "USD");
            
            System.out.println("Created test loans with amounts: " + 
                List.of(personalLoanAmount, autoLoanAmount, businessLoanAmount));
        }
        
        private void createTestPayments() {
            // Create test payments for the loans
            Money payment1 = Money.of(new BigDecimal("478.66"), "USD");
            Payment testPayment1 = Payment.createNew(
                LoanId.of("DEV-LOAN-001"),
                new CustomerId("DEV-CUST-001"),
                payment1,
                PaymentMethod.BANK_TRANSFER,
                "DEV-PAY-REF-001",
                "Development test payment 1"
            );
            
            Money payment2 = Money.of(new BigDecimal("548.32"), "USD");
            Payment testPayment2 = Payment.createNew(
                LoanId.of("DEV-LOAN-002"),
                new CustomerId("DEV-CUST-002"),
                payment2,
                PaymentMethod.ACH,
                "DEV-PAY-REF-002",
                "Development test payment 2"
            );
            
            System.out.println("Created test payments: " + 
                List.of(testPayment1.getPaymentReference(), testPayment2.getPaymentReference()));
        }
    }
}
EOF

    log "SUCCESS" "Development data loader created"
}

setup_ide_configuration() {
    log "STEP" "Setting up IDE configurations..."
    
    # Create IntelliJ IDEA configuration
    mkdir -p "${PROJECT_ROOT}/.idea/runConfigurations"
    
    cat > "${PROJECT_ROOT}/.idea/runConfigurations/Banking_App_Development.xml" << 'EOF'
<component name="ProjectRunConfigurationManager">
  <configuration default="false" name="Banking App (Development)" type="SpringBootApplicationConfigurationType" factoryName="Spring Boot">
    <module name="loan-management-system.main" />
    <option name="SPRING_BOOT_MAIN_CLASS" value="com.bank.loanmanagement.LoanManagementApplication" />
    <option name="ALTERNATIVE_JRE_PATH" />
    <option name="SHORTEN_COMMAND_LINE" value="NONE" />
    <option name="ENABLE_DEBUG_MODE" value="false" />
    <envs>
      <env name="SPRING_PROFILES_ACTIVE" value="local,development,h2" />
      <env name="ENABLE_HOT_RELOAD" value="true" />
      <env name="ENABLE_DEBUG_MODE" value="true" />
    </envs>
    <option name="VM_PARAMETERS" value="-Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication -Dspring.devtools.restart.enabled=true -Dspring.devtools.livereload.enabled=true" />
    <option name="PROGRAM_PARAMETERS" value="" />
    <option name="ALTERNATIVE_JRE_PATH_ENABLED" value="false" />
    <option name="ALTERNATIVE_JRE_PATH" value="" />
    <method v="2">
      <option name="Make" enabled="true" />
    </method>
  </configuration>
</component>
EOF

    cat > "${PROJECT_ROOT}/.idea/runConfigurations/Banking_App_Debug.xml" << 'EOF'
<component name="ProjectRunConfigurationManager">
  <configuration default="false" name="Banking App (Debug)" type="SpringBootApplicationConfigurationType" factoryName="Spring Boot">
    <module name="loan-management-system.main" />
    <option name="SPRING_BOOT_MAIN_CLASS" value="com.bank.loanmanagement.LoanManagementApplication" />
    <option name="ALTERNATIVE_JRE_PATH" />
    <option name="SHORTEN_COMMAND_LINE" value="NONE" />
    <option name="ENABLE_DEBUG_MODE" value="true" />
    <envs>
      <env name="SPRING_PROFILES_ACTIVE" value="local,development,h2" />
      <env name="ENABLE_DEBUG_MODE" value="true" />
    </envs>
    <option name="VM_PARAMETERS" value="-Xmx2g -XX:+UseG1GC -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005" />
    <option name="PROGRAM_PARAMETERS" value="" />
    <method v="2">
      <option name="Make" enabled="true" />
    </method>
  </configuration>
</component>
EOF

    # Create VS Code configuration
    mkdir -p "${PROJECT_ROOT}/.vscode"
    
    cat > "${PROJECT_ROOT}/.vscode/launch.json" << 'EOF'
{
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "Banking App (Development)",
            "request": "launch",
            "mainClass": "com.bank.loanmanagement.LoanManagementApplication",
            "projectName": "loan-management-system",
            "args": "",
            "env": {
                "SPRING_PROFILES_ACTIVE": "local,development,h2",
                "ENABLE_HOT_RELOAD": "true",
                "ENABLE_DEBUG_MODE": "true"
            },
            "vmArgs": "-Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication -Dspring.devtools.restart.enabled=true -Dspring.devtools.livereload.enabled=true"
        },
        {
            "type": "java",
            "name": "Banking App (Debug)",
            "request": "launch",
            "mainClass": "com.bank.loanmanagement.LoanManagementApplication",
            "projectName": "loan-management-system",
            "args": "",
            "env": {
                "SPRING_PROFILES_ACTIVE": "local,development,h2",
                "ENABLE_DEBUG_MODE": "true"
            },
            "vmArgs": "-Xmx2g -XX:+UseG1GC",
            "port": 5005
        }
    ]
}
EOF

    cat > "${PROJECT_ROOT}/.vscode/tasks.json" << 'EOF'
{
    "version": "2.0.0",
    "tasks": [
        {
            "label": "gradle: bootRun (dev)",
            "type": "shell",
            "command": "./gradlew",
            "args": ["bootRun", "--args='--spring.profiles.active=local,development,h2'"],
            "group": "build",
            "presentation": {
                "echo": true,
                "reveal": "always",
                "focus": false,
                "panel": "shared"
            },
            "problemMatcher": "$gradle"
        },
        {
            "label": "gradle: test (dev)",
            "type": "shell",
            "command": "./gradlew",
            "args": ["test", "--continue"],
            "group": "test",
            "presentation": {
                "echo": true,
                "reveal": "always",
                "focus": false,
                "panel": "shared"
            },
            "problemMatcher": "$gradle"
        },
        {
            "label": "Load Development Data",
            "type": "shell",
            "command": "./gradlew",
            "args": ["loadTestData"],
            "group": "build",
            "presentation": {
                "echo": true,
                "reveal": "always",
                "focus": false,
                "panel": "shared"
            }
        }
    ]
}
EOF

    cat > "${PROJECT_ROOT}/.vscode/settings.json" << 'EOF'
{
    "java.configuration.updateBuildConfiguration": "automatic",
    "java.compile.nullAnalysis.mode": "automatic",
    "java.debug.settings.onBuildFailureProceed": true,
    "java.debug.settings.hotCodeReplace": "auto",
    "spring-boot.ls.problem.application-properties.unknown-property": "WARNING",
    "files.exclude": {
        "**/build": true,
        "**/.gradle": true
    },
    "java.format.settings.url": "https://raw.githubusercontent.com/google/styleguide/gh-pages/eclipse-java-google-style.xml",
    "editor.formatOnSave": true,
    "editor.insertSpaces": true,
    "editor.tabSize": 4
}
EOF

    log "SUCCESS" "IDE configurations created for IntelliJ IDEA and VS Code"
}

create_development_scripts() {
    log "STEP" "Creating development helper scripts..."
    
    # Quick start script
    cat > "${PROJECT_ROOT}/dev-start.sh" << 'EOF'
#!/bin/bash
# Quick start script for local development

set -e

echo "🏦 Starting Banking System Development Environment..."

# Load environment variables
if [ -f .env.local ]; then
    export $(cat .env.local | grep -v '^#' | xargs)
fi

# Create directories
mkdir -p logs/dev data/dev

# Start with hot reload
echo "Starting application with hot reload..."
./gradlew runDev

echo "✅ Development server started!"
echo "🌐 Application: http://localhost:8080"
echo "🔍 H2 Console: http://localhost:8080/h2-console"
echo "📚 Swagger UI: http://localhost:8080/swagger-ui.html"
echo "📊 Actuator: http://localhost:8080/actuator"
EOF

    # Test script
    cat > "${PROJECT_ROOT}/dev-test.sh" << 'EOF'
#!/bin/bash
# Quick test script for development

set -e

echo "🧪 Running Development Tests..."

# Load environment variables
if [ -f .env.local ]; then
    export $(cat .env.local | grep -v '^#' | xargs)
fi

# Run tests with development profile
./gradlew testDev --continue

echo "✅ Development tests completed!"
EOF

    # Database reset script
    cat > "${PROJECT_ROOT}/dev-reset-db.sh" << 'EOF'
#!/bin/bash
# Reset development database

set -e

echo "🗄️ Resetting Development Database..."

# Stop application if running
pkill -f "spring-boot:run" || true

# Clean development files
./gradlew cleanDev

# Load fresh test data
./gradlew loadTestData

echo "✅ Development database reset complete!"
EOF

    # Make scripts executable
    chmod +x "${PROJECT_ROOT}/dev-start.sh"
    chmod +x "${PROJECT_ROOT}/dev-test.sh"
    chmod +x "${PROJECT_ROOT}/dev-reset-db.sh"
    
    log "SUCCESS" "Development helper scripts created"
}

setup_monitoring_config() {
    if [ "$ENABLE_MONITORING" = "false" ]; then
        return 0
    fi
    
    log "STEP" "Setting up development monitoring configuration..."
    
    mkdir -p "${PROJECT_ROOT}/monitoring/prometheus"
    mkdir -p "${PROJECT_ROOT}/monitoring/grafana/dashboards"
    mkdir -p "${PROJECT_ROOT}/monitoring/grafana/provisioning/datasources"
    
    # Prometheus configuration for development
    cat > "${PROJECT_ROOT}/monitoring/prometheus/prometheus-dev.yml" << 'EOF'
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  # - "first_rules.yml"
  # - "second_rules.yml"

scrape_configs:
  - job_name: 'banking-app-dev'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 5s
    
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']
EOF

    # Grafana datasource configuration
    cat > "${PROJECT_ROOT}/monitoring/grafana/provisioning/datasources/prometheus.yml" << 'EOF'
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://localhost:9090
    isDefault: true
EOF

    log "SUCCESS" "Development monitoring configuration created"
}

create_readme_dev() {
    log "STEP" "Creating development README..."
    
    cat > "${PROJECT_ROOT}/README-DEV.md" << 'EOF'
# 🛠️ Development Guide - Enterprise Loan Management System

Welcome to the development environment for the Enterprise Loan Management System!

## 🚀 Quick Start

### Prerequisites
- Java 25.0.2+
- Gradle 8+
- Docker & Docker Compose
- Git

### Setup Development Environment
```bash
# Clone and setup
git clone <repository-url>
cd enterprise-loan-management-system

# Run setup script
./scripts/setup-local-dev.sh

# Start development server
./dev-start.sh
```

## 🔧 Development Commands

### Application
```bash
# Start with hot reload
./dev-start.sh

# Start in debug mode
./gradlew runDebug

# Run tests
./dev-test.sh

# Reset database
./dev-reset-db.sh
```

### Gradle Tasks
```bash
# Development server with hot reload
./gradlew runDev

# Debug mode (port 5005)
./gradlew runDebug

# Development tests
./gradlew testDev

# Load test data
./gradlew loadTestData

# Clean development files
./gradlew cleanDev
```

## 🌐 Development URLs

| Service | URL | Description |
|---------|-----|-------------|
| Main App | http://localhost:8080 | Banking application |
| H2 Console | http://localhost:8080/h2-console | Database console |
| Swagger UI | http://localhost:8080/swagger-ui.html | API documentation |
| GraphQL | http://localhost:8080/graphql | GraphQL playground |
| Actuator | http://localhost:8080/actuator | Application metrics |

## 🗄️ Database Configuration

### H2 Database (Default for Development)
- **URL**: `jdbc:h2:mem:banking_dev`
- **Username**: `sa`
- **Password**: *(empty)*
- **Console**: http://localhost:8080/h2-console

### Test Data
The development environment automatically loads test data including:
- Sample customers (individual and corporate)
- Sample loans (personal, auto, business)
- Sample payments and installments
- Credit scores and financial profiles

## 🐛 Debugging

### IDE Configuration
- **IntelliJ IDEA**: Use "Banking App (Debug)" run configuration
- **VS Code**: Use "Banking App (Debug)" launch configuration
- **Remote Debug Port**: 5005

### Debug Features
- Spring DevTools hot reload
- LiveReload for web assets
- Debug logging enabled
- H2 console for database inspection

## 📊 Monitoring

### Actuator Endpoints
- `/actuator/health` - Application health
- `/actuator/metrics` - Application metrics
- `/actuator/info` - Application information
- `/actuator/prometheus` - Prometheus metrics

### Local Prometheus (Optional)
```bash
# Start Prometheus for development
docker run -p 9090:9090 -v ./monitoring/prometheus/prometheus-dev.yml:/etc/prometheus/prometheus.yml prom/prometheus
```

## 🧪 Testing

### Test Categories
- **Unit Tests**: `./gradlew test`
- **Integration Tests**: `./gradlew integrationTest`
- **Development Tests**: `./gradlew testDev`

### Test Data
Development test data is automatically created and includes:
- Customers: DEV-CUST-001, DEV-CUST-002, DEV-CORP-001
- Loans: Various loan types and statuses
- Payments: Sample payment transactions

## 🔧 Configuration

### Environment Variables
Development configuration is in `.env.local`:
- Spring profiles: `local,development,h2`
- Database: H2 in-memory
- Security: Relaxed for development
- Logging: Debug level enabled

### Profiles
- `local` - Local development settings
- `development` - Development-specific features
- `h2` - H2 database configuration

## 🛠️ Development Tools

### Hot Reload
- Automatic restart on code changes
- LiveReload for web assets
- Class reloading without full restart

### Code Quality
- Checkstyle configuration
- SpotBugs integration
- JaCoCo test coverage

## 📁 Development Structure
```
.
├── src/main/java/              # Application code
├── src/test/java/              # Test code
├── src/main/resources/         # Resources
├── logs/dev/                   # Development logs
├── data/dev/                   # Development data
├── monitoring/                 # Monitoring configs
├── .env.local                  # Local environment
├── dev-start.sh               # Quick start script
├── dev-test.sh                # Test script
└── dev-reset-db.sh            # Database reset
```

## 🆘 Troubleshooting

### Common Issues

#### Port Already in Use
```bash
# Kill process on port 8080
lsof -ti:8080 | xargs kill -9
```

#### Database Issues
```bash
# Reset development database
./dev-reset-db.sh
```

#### Hot Reload Not Working
```bash
# Restart with clean build
./gradlew clean runDev
```

### Getting Help
1. Check logs in `logs/dev/`
2. Verify H2 console at http://localhost:8080/h2-console
3. Check actuator health at http://localhost:8080/actuator/health
4. Review configuration in `.env.local`

## 🚀 Next Steps

1. **API Development**: Add new endpoints in REST or GraphQL
2. **Database Changes**: Update domain models and migrations
3. **AI Integration**: Enable AI services with API keys
4. **Testing**: Add comprehensive test coverage
5. **Documentation**: Update API documentation

Happy coding! 🎉
EOF

    log "SUCCESS" "Development README created"
}

show_development_summary() {
    log "STEP" "Development Environment Setup Complete!"
    
    echo -e "${GREEN}"
    cat << 'EOF'
    ╔══════════════════════════════════════════════════════════════════╗
    ║                🎉 DEVELOPMENT SETUP COMPLETE! 🎉                 ║
    ╚══════════════════════════════════════════════════════════════════╝
EOF
    echo -e "${NC}"
    
    log "INFO" "Quick Start Commands:"
    log "INFO" "  • Start Dev Server:    ./dev-start.sh"
    log "INFO" "  • Run Tests:           ./dev-test.sh"
    log "INFO" "  • Reset Database:      ./dev-reset-db.sh"
    log "INFO" "  • Debug Mode:          ./gradlew runDebug"
    
    log "INFO" "Development URLs:"
    log "INFO" "  • Application:         http://localhost:8080"
    log "INFO" "  • H2 Console:          http://localhost:8080/h2-console"
    log "INFO" "  • Swagger UI:          http://localhost:8080/swagger-ui.html"
    log "INFO" "  • GraphQL:             http://localhost:8080/graphql"
    log "INFO" "  • Actuator:            http://localhost:8080/actuator"
    
    log "INFO" "IDE Configurations:"
    log "INFO" "  • IntelliJ IDEA:       .idea/runConfigurations/"
    log "INFO" "  • VS Code:             .vscode/"
    
    log "INFO" "Configuration Files:"
    log "INFO" "  • Environment:         .env.local"
    log "INFO" "  • Gradle Dev:          gradle/dev-tasks.gradle"
    log "INFO" "  • Dev Guide:           README-DEV.md"
    
    echo -e "${CYAN}Ready to start developing! Run ./dev-start.sh to begin.${NC}"
}

# ===================================================================
# Main Execution
# ===================================================================

main() {
    show_banner
    
    log "INFO" "Setting up local development environment..."
    log "INFO" "Features: HOT_RELOAD=$ENABLE_HOT_RELOAD, DEBUG=$ENABLE_DEBUG_MODE, MONITORING=$ENABLE_MONITORING"
    
    check_prerequisites
    create_dev_environment_file
    setup_gradle_dev_configuration
    create_dev_data_loader
    setup_ide_configuration
    create_development_scripts
    setup_monitoring_config
    create_readme_dev
    show_development_summary
    
    log "SUCCESS" "Local development environment setup completed!"
}

# Handle command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --no-hot-reload)
            ENABLE_HOT_RELOAD="false"
            shift
            ;;
        --no-debug)
            ENABLE_DEBUG_MODE="false"
            shift
            ;;
        --no-test-data)
            ENABLE_TEST_DATA="false"
            shift
            ;;
        --no-monitoring)
            ENABLE_MONITORING="false"
            shift
            ;;
        --enable-ai)
            ENABLE_AI_SERVICES="true"
            shift
            ;;
        --help|-h)
            echo "Usage: $0 [OPTIONS]"
            echo "Options:"
            echo "  --no-hot-reload     Disable hot reload features"
            echo "  --no-debug          Disable debug mode"
            echo "  --no-test-data      Skip test data creation"
            echo "  --no-monitoring     Disable monitoring setup"
            echo "  --enable-ai         Enable AI services (requires API keys)"
            echo "  --help, -h          Show this help message"
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
