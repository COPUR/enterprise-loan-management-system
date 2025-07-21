#!/bin/bash

# Open Finance Infrastructure Deployment Script
# Supports Docker Compose, Kubernetes, and Terraform deployments
# UAE CBUAE C7/2023 compliant infrastructure setup

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
LOG_FILE="/tmp/openfinance-deploy-$(date +%Y%m%d-%H%M%S).log"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging function
log() {
    local level=$1
    shift
    echo -e "[$(date +'%Y-%m-%d %H:%M:%S')] [$level] $*" | tee -a "$LOG_FILE"
}

info() { log "INFO" "${BLUE}$*${NC}"; }
warn() { log "WARN" "${YELLOW}$*${NC}"; }
error() { log "ERROR" "${RED}$*${NC}"; }
success() { log "SUCCESS" "${GREEN}$*${NC}"; }

# Error handler
error_handler() {
    local line_no=$1
    error "Script failed at line $line_no"
    error "Check log file: $LOG_FILE"
    exit 1
}

trap 'error_handler $LINENO' ERR

# Usage information
usage() {
    cat << EOF
Usage: $0 [OPTIONS] DEPLOYMENT_TYPE

Open Finance Infrastructure Deployment Script

DEPLOYMENT_TYPE:
    docker      Deploy using Docker Compose (development/testing)
    kubernetes  Deploy to Kubernetes cluster
    terraform   Deploy AWS infrastructure using Terraform
    all         Deploy complete infrastructure (Terraform + Kubernetes)

OPTIONS:
    -e, --environment ENV    Environment (production|staging|development) [default: development]
    -r, --region REGION      AWS region [default: me-south-1]
    -n, --namespace NS       Kubernetes namespace [default: open-finance]
    -c, --config FILE        Configuration file path
    -d, --dry-run           Show what would be deployed without making changes
    -f, --force             Force deployment without confirmation
    -v, --verbose           Enable verbose output
    -h, --help              Show this help message

EXAMPLES:
    # Development deployment with Docker
    $0 docker -e development

    # Production deployment to Kubernetes
    $0 kubernetes -e production -r me-south-1

    # Full AWS infrastructure deployment
    $0 terraform -e production -r me-south-1

    # Complete deployment (infrastructure + application)
    $0 all -e production -r me-south-1

COMPLIANCE:
    - UAE CBUAE Open Finance regulation C7/2023
    - PCI-DSS v4 security standards
    - FAPI 2.0 security profile
    - High availability and disaster recovery
    - Comprehensive monitoring and alerting

PREREQUISITES:
    - Docker and Docker Compose (for docker deployment)
    - kubectl and Helm (for kubernetes deployment)  
    - Terraform >= 1.6 (for terraform deployment)
    - AWS CLI configured (for AWS deployments)
    - Valid SSL certificates in certificates/ directory
EOF
}

# Default values
ENVIRONMENT="development"
AWS_REGION="me-south-1"
NAMESPACE="open-finance"
CONFIG_FILE=""
DRY_RUN=false
FORCE=false
VERBOSE=false
DEPLOYMENT_TYPE=""

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--environment)
            ENVIRONMENT="$2"
            shift 2
            ;;
        -r|--region)
            AWS_REGION="$2"
            shift 2
            ;;
        -n|--namespace)
            NAMESPACE="$2"
            shift 2
            ;;
        -c|--config)
            CONFIG_FILE="$2"
            shift 2
            ;;
        -d|--dry-run)
            DRY_RUN=true
            shift
            ;;
        -f|--force)
            FORCE=true
            shift
            ;;
        -v|--verbose)
            VERBOSE=true
            set -x
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        docker|kubernetes|terraform|all)
            DEPLOYMENT_TYPE="$1"
            shift
            ;;
        *)
            error "Unknown option: $1"
            usage
            exit 1
            ;;
    esac
done

# Validate deployment type
if [[ -z "$DEPLOYMENT_TYPE" ]]; then
    error "Deployment type is required"
    usage
    exit 1
fi

# Validate environment
case $ENVIRONMENT in
    production|staging|development) ;;
    *)
        error "Invalid environment: $ENVIRONMENT"
        exit 1
        ;;
esac

# Pre-deployment checks
check_prerequisites() {
    info "🔍 Checking prerequisites for $DEPLOYMENT_TYPE deployment..."

    case $DEPLOYMENT_TYPE in
        docker|all)
            if ! command -v docker >/dev/null 2>&1; then
                error "Docker is not installed or not in PATH"
                return 1
            fi
            if ! command -v docker-compose >/dev/null 2>&1; then
                error "Docker Compose is not installed or not in PATH"
                return 1
            fi
            ;;
    esac

    case $DEPLOYMENT_TYPE in
        kubernetes|all)
            if ! command -v kubectl >/dev/null 2>&1; then
                error "kubectl is not installed or not in PATH"
                return 1
            fi
            if ! command -v helm >/dev/null 2>&1; then
                error "Helm is not installed or not in PATH"
                return 1
            fi
            ;;
    esac

    case $DEPLOYMENT_TYPE in
        terraform|all)
            if ! command -v terraform >/dev/null 2>&1; then
                error "Terraform is not installed or not in PATH"
                return 1
            fi
            if ! command -v aws >/dev/null 2>&1; then
                error "AWS CLI is not installed or not in PATH"
                return 1
            fi
            # Check AWS credentials
            if ! aws sts get-caller-identity >/dev/null 2>&1; then
                error "AWS credentials not configured or invalid"
                return 1
            fi
            ;;
    esac

    success "✅ Prerequisites check passed"
}

# Generate configuration files
generate_config() {
    info "📝 Generating configuration for $ENVIRONMENT environment..."

    # Create environment-specific env file
    local env_file="$SCRIPT_DIR/.env.$ENVIRONMENT"
    cat > "$env_file" << EOF
# Open Finance Infrastructure Configuration
# Environment: $ENVIRONMENT
# Generated: $(date)

# Basic Configuration
ENVIRONMENT=$ENVIRONMENT
AWS_REGION=$AWS_REGION
NAMESPACE=$NAMESPACE

# Database Configuration
POSTGRES_PASSWORD=$(openssl rand -base64 32)
POSTGRES_USER=openfinance
POSTGRES_DB=openfinance

# Redis Configuration  
REDIS_PASSWORD=$(openssl rand -base64 32)

# MongoDB Configuration
MONGO_PASSWORD=$(openssl rand -base64 32)
MONGO_USER=openfinance

# Keycloak Configuration
KEYCLOAK_PASSWORD=$(openssl rand -base64 32)
KEYCLOAK_REALM=open-finance
KEYCLOAK_CLIENT_ID=open-finance-api

# Application Secrets
JWT_SIGNING_KEY=$(openssl rand -base64 64)
ENCRYPTION_KEY=$(openssl rand -base64 32)
CBUAE_API_KEY=placeholder-get-from-cbuae

# SSL/TLS Configuration
KEYSTORE_PASSWORD=$(openssl rand -base64 32)
TRUSTSTORE_PASSWORD=$(openssl rand -base64 32)

# Monitoring Configuration
GRAFANA_PASSWORD=$(openssl rand -base64 32)
PROMETHEUS_RETENTION=30d

# Compliance Configuration
PCI_DSS_MODE=strict
FAPI_SECURITY_ENABLED=true
CBUAE_COMPLIANCE_MODE=strict
EOF

    success "✅ Configuration generated: $env_file"
}

# Generate SSL certificates for development
generate_dev_certificates() {
    local cert_dir="$SCRIPT_DIR/certificates"
    
    if [[ ! -d "$cert_dir" ]]; then
        info "🔐 Generating development SSL certificates..."
        mkdir -p "$cert_dir"
        
        # Root CA
        openssl genrsa -out "$cert_dir/ca-key.pem" 4096
        openssl req -new -x509 -days 365 -key "$cert_dir/ca-key.pem" -out "$cert_dir/ca.pem" \
            -subj "/C=AE/ST=Dubai/L=Dubai/O=Enterprise/OU=OpenFinance/CN=OpenFinance CA"
        
        # Server certificate
        openssl genrsa -out "$cert_dir/server-key.pem" 4096
        openssl req -new -key "$cert_dir/server-key.pem" -out "$cert_dir/server.csr" \
            -subj "/C=AE/ST=Dubai/L=Dubai/O=Enterprise/OU=OpenFinance/CN=*.openfinance.local"
        
        # Extensions for SAN
        cat > "$cert_dir/server-ext.cnf" << EOF
basicConstraints=CA:FALSE
subjectAltName=@alt_names
keyUsage=digitalSignature,keyEncipherment
extendedKeyUsage=serverAuth

[alt_names]
DNS.1=*.openfinance.local
DNS.2=*.openfinance.enterprise.local
DNS.3=keycloak.openfinance.local
DNS.4=api.openfinance.local
DNS.5=monitoring.openfinance.local
DNS.6=localhost
IP.1=127.0.0.1
IP.2=::1
EOF
        
        openssl x509 -req -days 365 -in "$cert_dir/server.csr" -CA "$cert_dir/ca.pem" \
            -CAkey "$cert_dir/ca-key.pem" -out "$cert_dir/server.pem" \
            -extensions v3_req -extfile "$cert_dir/server-ext.cnf" -CAcreateserial
        
        # Create service-specific certificates
        cp "$cert_dir/server.pem" "$cert_dir/keycloak-tls.crt"
        cp "$cert_dir/server-key.pem" "$cert_dir/keycloak-tls.key"
        cp "$cert_dir/server.pem" "$cert_dir/grafana.crt"
        cp "$cert_dir/server-key.pem" "$cert_dir/grafana.key"
        cp "$cert_dir/server.pem" "$cert_dir/kibana.crt"
        cp "$cert_dir/server-key.pem" "$cert_dir/kibana.key"
        
        success "✅ Development certificates generated"
    else
        info "📋 Using existing certificates"
    fi
}

# Docker deployment
deploy_docker() {
    info "🐳 Deploying Open Finance with Docker Compose..."
    
    cd "$SCRIPT_DIR/docker"
    
    # Generate certificates for development
    if [[ "$ENVIRONMENT" != "production" ]]; then
        generate_dev_certificates
    fi
    
    # Check if certificates exist
    if [[ ! -d "$SCRIPT_DIR/certificates" ]] || [[ ! -f "$SCRIPT_DIR/certificates/server.pem" ]]; then
        error "SSL certificates not found. Generate certificates first."
        return 1
    fi
    
    local env_file="$SCRIPT_DIR/.env.$ENVIRONMENT"
    if [[ -f "$env_file" ]]; then
        export $(grep -v '^#' "$env_file" | xargs)
    fi
    
    if [[ "$DRY_RUN" == "true" ]]; then
        info "🔍 Dry run - Docker Compose configuration:"
        docker-compose config
        return 0
    fi
    
    # Stop existing containers
    docker-compose down --remove-orphans || true
    
    # Pull latest images
    docker-compose pull
    
    # Start infrastructure services first
    info "📊 Starting infrastructure services..."
    docker-compose up -d postgres redis-cluster mongodb zookeeper kafka
    
    # Wait for databases to be ready
    info "⏳ Waiting for databases to be ready..."
    sleep 30
    
    # Start remaining services
    info "🚀 Starting application and monitoring services..."
    docker-compose up -d
    
    # Health check
    info "🏥 Performing health checks..."
    sleep 60
    
    local services=("open-finance-api" "keycloak" "grafana")
    for service in "${services[@]}"; do
        if docker-compose ps "$service" | grep -q "Up"; then
            success "✅ $service is healthy"
        else
            warn "⚠️ $service health check failed"
        fi
    done
    
    success "🎉 Docker deployment completed successfully!"
    info "📋 Access URLs:"
    info "  - Open Finance API: https://localhost:8080"
    info "  - Keycloak Admin: https://localhost:8443"
    info "  - Grafana: https://localhost:3000"
    info "  - Kibana: https://localhost:5601"
}

# Kubernetes deployment
deploy_kubernetes() {
    info "☸️ Deploying Open Finance to Kubernetes..."
    
    # Check cluster connectivity
    if ! kubectl cluster-info >/dev/null 2>&1; then
        error "Cannot connect to Kubernetes cluster"
        return 1
    fi
    
    local cluster_info=$(kubectl cluster-info | head -1)
    info "📋 Connected to cluster: $cluster_info"
    
    # Create namespace
    if ! kubectl get namespace "$NAMESPACE" >/dev/null 2>&1; then
        info "🏗️ Creating namespace: $NAMESPACE"
        kubectl create namespace "$NAMESPACE"
    fi
    
    # Apply compliance labels
    kubectl label namespace "$NAMESPACE" \
        "compliance.cbuae.gov.ae/regulation=C7-2023" \
        "security.level=high" \
        "data.classification=restricted" \
        --overwrite
    
    if [[ "$DRY_RUN" == "true" ]]; then
        info "🔍 Dry run - Kubernetes manifests:"
        kubectl apply -f "$SCRIPT_DIR/kubernetes/" --dry-run=client -o yaml
        return 0
    fi
    
    # Deploy secrets
    info "🔐 Deploying secrets..."
    local env_file="$SCRIPT_DIR/.env.$ENVIRONMENT"
    if [[ -f "$env_file" ]]; then
        kubectl create secret generic open-finance-secrets \
            --namespace="$NAMESPACE" \
            --from-env-file="$env_file" \
            --dry-run=client -o yaml | kubectl apply -f -
    fi
    
    # Deploy certificates
    if [[ -d "$SCRIPT_DIR/certificates" ]]; then
        kubectl create secret tls open-finance-certificates \
            --namespace="$NAMESPACE" \
            --cert="$SCRIPT_DIR/certificates/server.pem" \
            --key="$SCRIPT_DIR/certificates/server-key.pem" \
            --dry-run=client -o yaml | kubectl apply -f -
    fi
    
    # Apply Kubernetes manifests
    info "📦 Deploying Kubernetes resources..."
    kubectl apply -f "$SCRIPT_DIR/kubernetes/" --namespace="$NAMESPACE"
    
    # Wait for deployment
    info "⏳ Waiting for deployment to be ready..."
    kubectl rollout status deployment/open-finance-api --namespace="$NAMESPACE" --timeout=600s
    
    # Check pod status
    info "🏥 Checking pod status..."
    kubectl get pods --namespace="$NAMESPACE" -o wide
    
    # Get service endpoints
    local api_service=$(kubectl get service open-finance-api-service --namespace="$NAMESPACE" -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')
    if [[ -n "$api_service" ]]; then
        success "✅ Open Finance API available at: https://$api_service"
    fi
    
    success "🎉 Kubernetes deployment completed successfully!"
}

# Terraform deployment
deploy_terraform() {
    info "🏗️ Deploying AWS infrastructure with Terraform..."
    
    cd "$SCRIPT_DIR/terraform"
    
    # Initialize Terraform
    info "🔧 Initializing Terraform..."
    terraform init -upgrade
    
    # Create terraform.tfvars file
    local tfvars_file="terraform.tfvars"
    cat > "$tfvars_file" << EOF
aws_region  = "$AWS_REGION"
environment = "$ENVIRONMENT"

# Environment-specific overrides
allowed_cidr_blocks = ["10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16"]

# Production-specific configurations
$(if [[ "$ENVIRONMENT" == "production" ]]; then
    echo 'db_deletion_protection = true'
    echo 'enable_deletion_protection = true'
    echo 'db_backup_retention_period = 30'
    echo 'audit_log_retention_days = 90'
else
    echo 'db_deletion_protection = false'
    echo 'enable_deletion_protection = false'
    echo 'db_backup_retention_period = 7'
    echo 'audit_log_retention_days = 30'
fi)

# Compliance tags
compliance_tags = {
  "compliance.cbuae.gov.ae/regulation" = "C7-2023"
  "security.level"                     = "high"
  "data.classification"                = "restricted"
  "environment"                        = "$ENVIRONMENT"
}
EOF
    
    if [[ "$DRY_RUN" == "true" ]]; then
        info "🔍 Terraform plan (dry run):"
        terraform plan -var-file="$tfvars_file"
        return 0
    fi
    
    # Plan infrastructure changes
    info "📋 Planning infrastructure changes..."
    terraform plan -var-file="$tfvars_file" -out=tfplan
    
    # Apply if not forced, ask for confirmation
    if [[ "$FORCE" != "true" ]] && [[ "$ENVIRONMENT" == "production" ]]; then
        echo
        warn "⚠️ You are about to deploy to PRODUCTION environment!"
        read -p "Are you sure you want to continue? (yes/no): " -r
        if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
            info "❌ Deployment cancelled by user"
            return 1
        fi
    fi
    
    # Apply infrastructure
    info "🚀 Applying infrastructure changes..."
    terraform apply tfplan
    
    # Store outputs
    terraform output -json > terraform-outputs.json
    
    success "✅ AWS infrastructure deployed successfully!"
    
    # Show important outputs
    info "📋 Important infrastructure details:"
    echo "  - EKS Cluster: $(terraform output -raw cluster_id)"
    echo "  - Load Balancer: $(terraform output -raw load_balancer_dns)"
    echo "  - RDS Endpoint: $(terraform output -raw rds_endpoint)"
    
    # Configure kubectl
    info "⚙️ Configuring kubectl for EKS cluster..."
    aws eks update-kubeconfig --region "$AWS_REGION" --name "$(terraform output -raw cluster_id)"
    
    success "🎉 Terraform deployment completed successfully!"
}

# Complete deployment (Terraform + Kubernetes)
deploy_all() {
    info "🌟 Starting complete infrastructure deployment..."
    
    # Deploy AWS infrastructure first
    deploy_terraform
    
    # Wait for EKS cluster to be fully ready
    info "⏳ Waiting for EKS cluster to be ready..."
    sleep 60
    
    # Deploy application to Kubernetes
    deploy_kubernetes
    
    success "🏆 Complete deployment finished successfully!"
}

# Cleanup function
cleanup() {
    info "🧹 Performing cleanup..."
    
    case $DEPLOYMENT_TYPE in
        docker)
            cd "$SCRIPT_DIR/docker"
            docker-compose down --remove-orphans
            ;;
        kubernetes)
            kubectl delete namespace "$NAMESPACE" --ignore-not-found=true
            ;;
        terraform)
            cd "$SCRIPT_DIR/terraform"
            if [[ -f "terraform.tfvars" ]]; then
                terraform destroy -var-file="terraform.tfvars" -auto-approve
            fi
            ;;
    esac
}

# Handle script interruption
trap cleanup INT TERM

# Main execution
main() {
    info "🚀 Starting Open Finance deployment..."
    info "📋 Configuration:"
    info "  - Deployment Type: $DEPLOYMENT_TYPE"
    info "  - Environment: $ENVIRONMENT"
    info "  - AWS Region: $AWS_REGION"
    info "  - Namespace: $NAMESPACE"
    info "  - Dry Run: $DRY_RUN"
    
    # Run pre-deployment checks
    check_prerequisites
    
    # Generate configuration
    generate_config
    
    # Deploy based on type
    case $DEPLOYMENT_TYPE in
        docker)
            deploy_docker
            ;;
        kubernetes)
            deploy_kubernetes
            ;;
        terraform)
            deploy_terraform
            ;;
        all)
            deploy_all
            ;;
        *)
            error "Unknown deployment type: $DEPLOYMENT_TYPE"
            exit 1
            ;;
    esac
    
    success "🏆 Open Finance deployment completed successfully!"
    info "📋 Log file: $LOG_FILE"
}

# Execute main function
main "$@"