#!/bin/bash

# Enterprise Banking System - Project Export Script for macOS
# Exports the comprehensive project to a new repository: enhanced-loan-management

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# Project configuration
CURRENT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$CURRENT_DIR/.." && pwd)"
EXPORT_NAME="enhanced-loan-management"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
EXPORT_DIR="$HOME/Desktop/${EXPORT_NAME}_export_${TIMESTAMP}"
NEW_REPO_DIR="$HOME/Desktop/$EXPORT_NAME"

# Logging functions
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] ✅ $1${NC}"
}

log_error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ❌ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] ⚠️  $1${NC}"
}

log_info() {
    echo -e "${CYAN}[$(date +'%Y-%m-%d %H:%M:%S')] ℹ️  $1${NC}"
}

# Banner
show_banner() {
    echo -e "${PURPLE}"
    cat << 'EOF'
╔══════════════════════════════════════════════════════════════════════════════════════════════════════════╗
║                                                                                                          ║
║                      🚀 Enterprise Banking System - Project Export Tool 🚀                            ║
║                                                                                                          ║
║                          Exporting to: enhanced-loan-management                                          ║
║                                                                                                          ║
╚══════════════════════════════════════════════════════════════════════════════════════════════════════════╝
EOF
    echo -e "${NC}"
}

# Check prerequisites
check_prerequisites() {
    log "Checking prerequisites..."
    
    # Check if rsync is available
    if ! command -v rsync &> /dev/null; then
        log_error "rsync is not installed. Please install it using: brew install rsync"
        exit 1
    fi
    
    # Check if git is available
    if ! command -v git &> /dev/null; then
        log_error "git is not installed. Please install it using: brew install git"
        exit 1
    fi
    
    # Check if current directory is the project
    if [ ! -f "$PROJECT_ROOT/build.gradle" ] && [ ! -f "$PROJECT_ROOT/pom.xml" ]; then
        log_error "This doesn't appear to be the project root directory"
        exit 1
    fi
    
    log_success "Prerequisites check passed"
}

# Create export directory structure
create_export_structure() {
    log "Creating export directory structure..."
    
    mkdir -p "$EXPORT_DIR"
    mkdir -p "$NEW_REPO_DIR"
    
    log_success "Export directories created"
}

# Export source code and configurations
export_source_code() {
    log "Exporting source code and configurations..."
    
    # Define directories to include
    local include_dirs=(
        "src"
        "docs"
        "k8s"
        "scripts"
        "security"
        "testing"
        "monitoring"
        "gradle"
        ".github"
    )
    
    # Define files to include
    local include_files=(
        "build.gradle"
        "settings.gradle"
        "gradle.properties"
        "gradlew"
        "gradlew.bat"
        "docker-compose.yml"
        "docker-compose.prod.yml"
        "Dockerfile"
        ".gitignore"
        ".gitattributes"
        "README.md"
        "CLAUDE.md"
        "LICENSE"
    )
    
    # Export directories
    for dir in "${include_dirs[@]}"; do
        if [ -d "$PROJECT_ROOT/$dir" ]; then
            log "Copying directory: $dir"
            rsync -av --progress \
                --exclude="*.class" \
                --exclude="*.jar" \
                --exclude="*.war" \
                --exclude="*.log" \
                --exclude="*.tmp" \
                --exclude="*.cache" \
                --exclude=".DS_Store" \
                --exclude="Thumbs.db" \
                "$PROJECT_ROOT/$dir" "$EXPORT_DIR/"
        else
            log_warning "Directory not found: $dir"
        fi
    done
    
    # Export files
    for file in "${include_files[@]}"; do
        if [ -f "$PROJECT_ROOT/$file" ]; then
            log "Copying file: $file"
            cp "$PROJECT_ROOT/$file" "$EXPORT_DIR/"
        else
            log_warning "File not found: $file"
        fi
    done
    
    log_success "Source code exported"
}

# Create comprehensive documentation
create_documentation() {
    log "Creating comprehensive documentation..."
    
    # Create main README
    cat > "$EXPORT_DIR/README.md" << 'EOF'
# Enhanced Loan Management System

## 🏦 Enterprise Banking System - Next Generation

A comprehensive, cloud-native banking system built with modern architecture patterns and enterprise-grade security.

### 🚀 Key Features

- **Microservices Architecture** with Spring Boot
- **Event-Driven Architecture** with Apache Kafka
- **Advanced Security** with OAuth 2.1 + DPoP, FAPI 2.0
- **Resilience Patterns** with Circuit Breakers, Bulkheads, and Retries
- **Database Enhancements** with encryption, sharding, and event sourcing
- **Comprehensive Testing** framework (Dev, SIT, UAT, Pre-prod, Prod)
- **AI/ML Integration** for fraud detection and risk assessment
- **Real-time Monitoring** with Prometheus, Grafana, and ELK stack

### 📋 Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Getting Started](#getting-started)
3. [Development Setup](#development-setup)
4. [Testing Strategy](#testing-strategy)
5. [Deployment](#deployment)
6. [Security](#security)
7. [Monitoring](#monitoring)
8. [Contributing](#contributing)

### 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                              Enhanced Loan Management System                        │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                     │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐                 │
│  │   API Gateway   │    │  Load Balancer  │    │   Web Frontend  │                 │
│  │   (Kong/Nginx)  │    │   (HAProxy)     │    │   (React/Vue)   │                 │
│  └─────────────────┘    └─────────────────┘    └─────────────────┘                 │
│                                                                                     │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐                 │
│  │   Customer      │    │      Loan       │    │    Payment      │                 │
│  │   Service       │    │    Service      │    │    Service      │                 │
│  └─────────────────┘    └─────────────────┘    └─────────────────┘                 │
│                                                                                     │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐                 │
│  │   Fraud         │    │   Notification  │    │    Audit        │                 │
│  │   Detection     │    │    Service      │    │    Service      │                 │
│  └─────────────────┘    └─────────────────┘    └─────────────────┘                 │
│                                                                                     │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐                 │
│  │   PostgreSQL    │    │     Redis       │    │     Kafka       │                 │
│  │   (Primary DB)  │    │    (Cache)      │    │   (Messaging)   │                 │
│  └─────────────────┘    └─────────────────┘    └─────────────────┘                 │
│                                                                                     │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

### 🚀 Getting Started

#### Prerequisites

- Java 17+
- Docker & Docker Compose
- Kubernetes (for production deployment)
- PostgreSQL 15+
- Redis 7+
- Apache Kafka 3.0+

#### Quick Start

```bash
# Clone the repository
git clone https://github.com/your-org/enhanced-loan-management.git
cd enhanced-loan-management

# Start infrastructure services
docker-compose up -d

# Build the project
./gradlew clean build

# Run tests
./gradlew test

# Start the application
./gradlew bootRun
```

### 💻 Development Setup

See [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) for detailed development setup instructions.

### 🧪 Testing Strategy

Our comprehensive testing framework includes:

1. **Development Testing** - Unit, Integration, Component tests
2. **System Integration Testing (SIT)** - End-to-end system validation
3. **User Acceptance Testing (UAT)** - Business user validation
4. **Pre-Production Testing** - Regression and performance testing
5. **Production Deployment** - Zero-downtime blue-green deployment

See [docs/TESTING_STRATEGY.md](docs/TESTING_STRATEGY.md) for details.

### 🚀 Deployment

See [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) for deployment instructions.

### 🔒 Security

- OAuth 2.1 + DPoP for authentication
- FAPI 2.0 compliance
- End-to-end encryption
- Advanced threat detection

See [docs/SECURITY.md](docs/SECURITY.md) for security details.

### 📊 Monitoring

- Prometheus + Grafana for metrics
- ELK stack for logging
- Distributed tracing with Jaeger
- Custom business metrics

See [docs/MONITORING.md](docs/MONITORING.md) for monitoring setup.

### 🤝 Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

### 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
EOF

    # Create migration guide
    cat > "$EXPORT_DIR/MIGRATION_GUIDE.md" << EOF
# Migration Guide

## From enterprise-loan-management-system to enhanced-loan-management

### Export Information
- **Export Date**: $(date)
- **Source Project**: enterprise-loan-management-system
- **Target Project**: enhanced-loan-management
- **Export Tool Version**: 1.0.0

### Migration Steps

1. **Initialize New Repository**
   \`\`\`bash
   cd ~/Desktop/enhanced-loan-management
   git init
   git remote add origin https://github.com/your-org/enhanced-loan-management.git
   \`\`\`

2. **Configure Git**
   \`\`\`bash
   git config user.name "Your Name"
   git config user.email "your.email@example.com"
   \`\`\`

3. **Initial Commit**
   \`\`\`bash
   git add .
   git commit -m "Initial commit: Enhanced Loan Management System"
   git branch -M main
   git push -u origin main
   \`\`\`

### Post-Migration Tasks

- [ ] Update CI/CD pipelines
- [ ] Configure secrets and environment variables
- [ ] Update documentation links
- [ ] Set up branch protection rules
- [ ] Configure webhooks
- [ ] Update team access permissions

### Environment Variables

Update the following environment variables in your new repository:

\`\`\`bash
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/enhanced_loan_db
DATABASE_USERNAME=your_username
DATABASE_PASSWORD=your_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Security
JWT_SECRET=your_jwt_secret
ENCRYPTION_KEY=your_encryption_key
\`\`\`
EOF

    # Create contribution guide
    cat > "$EXPORT_DIR/CONTRIBUTING.md" << 'EOF'
# Contributing to Enhanced Loan Management System

We love your input! We want to make contributing to this project as easy and transparent as possible.

## Development Process

1. Fork the repo and create your branch from `main`
2. If you've added code that should be tested, add tests
3. If you've changed APIs, update the documentation
4. Ensure the test suite passes
5. Make sure your code lints
6. Issue that pull request!

## Code Style

- Use 4 spaces for indentation
- Follow the existing code style
- Add meaningful comments
- Write descriptive commit messages

## Testing

- Write unit tests for all new functionality
- Ensure all tests pass before submitting PR
- Aim for >80% code coverage

## Pull Request Process

1. Update the README.md with details of changes
2. Update the docs with any new environment variables
3. Increase version numbers following SemVer
4. PR will be merged after review by maintainers
EOF

    log_success "Documentation created"
}

# Clean and prepare the export
clean_export() {
    log "Cleaning export directory..."
    
    # Remove build artifacts
    find "$EXPORT_DIR" -type d -name "build" -exec rm -rf {} + 2>/dev/null || true
    find "$EXPORT_DIR" -type d -name "out" -exec rm -rf {} + 2>/dev/null || true
    find "$EXPORT_DIR" -type d -name ".gradle" -exec rm -rf {} + 2>/dev/null || true
    find "$EXPORT_DIR" -type d -name "node_modules" -exec rm -rf {} + 2>/dev/null || true
    find "$EXPORT_DIR" -type d -name ".idea" -exec rm -rf {} + 2>/dev/null || true
    find "$EXPORT_DIR" -type d -name ".vscode" -exec rm -rf {} + 2>/dev/null || true
    
    # Remove log files
    find "$EXPORT_DIR" -type f -name "*.log" -delete 2>/dev/null || true
    find "$EXPORT_DIR" -type f -name "*.tmp" -delete 2>/dev/null || true
    
    # Remove macOS specific files
    find "$EXPORT_DIR" -type f -name ".DS_Store" -delete 2>/dev/null || true
    
    log_success "Export cleaned"
}

# Create project structure summary
create_project_summary() {
    log "Creating project structure summary..."
    
    cat > "$EXPORT_DIR/PROJECT_STRUCTURE.md" << 'EOF'
# Project Structure

```
enhanced-loan-management/
├── src/
│   ├── main/
│   │   ├── java/com/bank/
│   │   │   ├── application/          # Application services
│   │   │   ├── domain/              # Domain models and business logic
│   │   │   ├── infrastructure/      # Infrastructure layer
│   │   │   │   ├── resilience/      # Circuit breakers and exception handling
│   │   │   │   ├── persistence/     # Database repositories
│   │   │   │   └── security/        # Security configurations
│   │   │   └── api/                 # REST controllers
│   │   └── resources/
│   │       ├── application.yml       # Application configuration
│   │       └── db/migration/         # Flyway migration scripts
│   └── test/                        # Test files
├── docs/                            # Documentation
│   ├── architecture/                # Architecture diagrams
│   ├── api/                        # API documentation
│   └── deployment/                 # Deployment guides
├── k8s/                            # Kubernetes manifests
│   ├── base/                       # Base configurations
│   ├── overlays/                   # Environment-specific configs
│   └── helm-charts/                # Helm charts
├── scripts/                        # Utility scripts
│   ├── build/                      # Build scripts
│   ├── deployment/                 # Deployment scripts
│   └── testing/                    # Testing scripts
├── security/                       # Security configurations
│   └── database/                   # Database security scripts
├── testing/                        # Testing frameworks
│   ├── development/                # Dev environment tests
│   ├── sit/                        # System integration tests
│   ├── uat/                        # User acceptance tests
│   ├── preprod/                    # Pre-production tests
│   └── production/                 # Production deployment tests
├── monitoring/                     # Monitoring configurations
│   ├── prometheus/                 # Prometheus configs
│   ├── grafana/                    # Grafana dashboards
│   └── alerts/                     # Alert rules
├── docker-compose.yml              # Local development setup
├── Dockerfile                      # Application container
└── build.gradle                    # Build configuration
```
EOF

    log_success "Project structure summary created"
}

# Initialize new Git repository
initialize_git_repo() {
    log "Initializing new Git repository..."
    
    cd "$NEW_REPO_DIR"
    
    # Copy all exported files to new repo
    cp -R "$EXPORT_DIR"/* "$NEW_REPO_DIR/"
    
    # Initialize git
    git init
    
    # Create .gitignore if it doesn't exist
    if [ ! -f ".gitignore" ]; then
        cat > .gitignore << 'EOF'
# Build artifacts
build/
target/
out/
*.class
*.jar
*.war

# IDE files
.idea/
*.iml
.vscode/
.settings/
.project
.classpath

# Gradle
.gradle/
gradle-app.setting
!gradle-wrapper.jar

# Logs
*.log
logs/

# OS files
.DS_Store
Thumbs.db

# Environment files
.env
.env.local

# Temporary files
*.tmp
*.temp
*.cache

# Database
*.db
*.sqlite

# Node modules (if any frontend)
node_modules/

# Python
__pycache__/
*.pyc
venv/
EOF
    fi
    
    # Initial commit
    git add .
    git commit -m "Initial commit: Enhanced Loan Management System

- Comprehensive enterprise banking system
- Microservices architecture
- Advanced security features
- Resilience patterns implementation
- Complete testing framework
- Production-ready deployment scripts"
    
    log_success "Git repository initialized"
}

# Create deployment package
create_deployment_package() {
    log "Creating deployment package..."
    
    cd "$EXPORT_DIR"
    
    # Create archive
    local archive_name="${EXPORT_NAME}_${TIMESTAMP}.tar.gz"
    tar -czf "$HOME/Desktop/$archive_name" .
    
    log_success "Deployment package created: $HOME/Desktop/$archive_name"
    
    # Calculate size
    local size=$(du -sh "$HOME/Desktop/$archive_name" | cut -f1)
    log_info "Package size: $size"
}

# Generate final report
generate_export_report() {
    log "Generating export report..."
    
    local report_file="$NEW_REPO_DIR/EXPORT_REPORT.md"
    
    cat > "$report_file" << EOF
# Export Report

## Export Summary

- **Export Date**: $(date)
- **Source Directory**: $PROJECT_ROOT
- **Target Repository**: $NEW_REPO_DIR
- **Export Package**: $HOME/Desktop/${EXPORT_NAME}_${TIMESTAMP}.tar.gz

## Exported Components

### Source Code
- ✅ Java source files
- ✅ Test files
- ✅ Resource files
- ✅ Configuration files

### Infrastructure
- ✅ Kubernetes manifests
- ✅ Docker configurations
- ✅ Helm charts
- ✅ CI/CD pipelines

### Documentation
- ✅ README files
- ✅ Architecture documentation
- ✅ API documentation
- ✅ Deployment guides

### Testing
- ✅ Unit test frameworks
- ✅ Integration test suites
- ✅ Performance test scripts
- ✅ Security test configurations

### Security
- ✅ Security policies
- ✅ Encryption configurations
- ✅ Authentication setups
- ✅ Compliance documentation

## Statistics

- **Total Files**: $(find "$NEW_REPO_DIR" -type f | wc -l | tr -d ' ')
- **Total Directories**: $(find "$NEW_REPO_DIR" -type d | wc -l | tr -d ' ')
- **Lines of Code**: $(find "$NEW_REPO_DIR" -name "*.java" -type f -exec wc -l {} + | tail -1 | awk '{print $1}')
- **Repository Size**: $(du -sh "$NEW_REPO_DIR" | cut -f1)

## Next Steps

1. Navigate to the new repository:
   \`\`\`bash
   cd $NEW_REPO_DIR
   \`\`\`

2. Add your remote repository:
   \`\`\`bash
   git remote add origin https://github.com/your-username/enhanced-loan-management.git
   \`\`\`

3. Push to remote:
   \`\`\`bash
   git push -u origin main
   \`\`\`

## Notes

- All sensitive information has been excluded
- Build artifacts have been cleaned
- The repository is ready for immediate use
- Remember to update environment-specific configurations
EOF

    log_success "Export report generated: $report_file"
}

# Main execution
main() {
    show_banner
    
    log "Starting project export process..."
    log "Source: $PROJECT_ROOT"
    log "Target: $NEW_REPO_DIR"
    
    # Execute export steps
    check_prerequisites
    create_export_structure
    export_source_code
    create_documentation
    create_project_summary
    clean_export
    initialize_git_repo
    create_deployment_package
    generate_export_report
    
    # Clean up temporary export directory
    rm -rf "$EXPORT_DIR"
    
    # Final summary
    echo -e "\n${GREEN}════════════════════════════════════════════════════════════════════${NC}"
    log_success "🎉 Project export completed successfully!"
    echo -e "${GREEN}════════════════════════════════════════════════════════════════════${NC}\n"
    
    log_info "New repository location: $NEW_REPO_DIR"
    log_info "Deployment package: $HOME/Desktop/${EXPORT_NAME}_${TIMESTAMP}.tar.gz"
    
    echo -e "\n${CYAN}Next steps:${NC}"
    echo "1. cd $NEW_REPO_DIR"
    echo "2. git remote add origin <your-github-repo-url>"
    echo "3. git push -u origin main"
    
    echo -e "\n${YELLOW}Remember to:${NC}"
    echo "- Update CI/CD configuration"
    echo "- Set up secrets and environment variables"
    echo "- Configure branch protection rules"
    echo "- Update team access permissions"
}

# Run main function
main "$@"