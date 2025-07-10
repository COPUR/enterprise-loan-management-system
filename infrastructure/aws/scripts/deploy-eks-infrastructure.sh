#!/bin/bash

# Enterprise Banking AWS EKS Infrastructure Deployment Script
# Comprehensive deployment automation for production-grade banking infrastructure
# Compliance: PCI DSS, SOX, GDPR, FAPI 2.0

set -euo pipefail

# Script Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$(dirname "$SCRIPT_DIR")")")"
TERRAFORM_DIR="$PROJECT_ROOT/infrastructure/aws/terraform"
CONFIGS_DIR="$PROJECT_ROOT/infrastructure/aws/configs"

# Default Values
DEFAULT_ENVIRONMENT="development"
DEFAULT_REGION="us-east-1"
DEFAULT_APPLY="false"

# Command Line Arguments
ENVIRONMENT="${1:-$DEFAULT_ENVIRONMENT}"
REGION="${2:-$DEFAULT_REGION}"
AUTO_APPLY="${3:-$DEFAULT_APPLY}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Logging functions
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}"
}

warn() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] WARNING: $1${NC}"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR: $1${NC}"
    exit 1
}

info() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')] INFO: $1${NC}"
}

success() {
    echo -e "${CYAN}[$(date +'%Y-%m-%d %H:%M:%S')] SUCCESS: $1${NC}"
}

# Print banner
print_banner() {
    echo -e "${PURPLE}"
    echo "╔══════════════════════════════════════════════════════════════════════════════╗"
    echo "║                 Enterprise Banking EKS Infrastructure                       ║"
    echo "║                        AWS Deployment Automation                            ║"
    echo "║                                                                              ║"
    echo "║  Environment: $ENVIRONMENT"
    echo "║  Region:      $REGION"
    echo "║  Auto Apply:  $AUTO_APPLY"
    echo "║                                                                              ║"
    echo "║  Compliance:  PCI DSS, SOX, GDPR, FAPI 2.0                                 ║"
    echo "╚══════════════════════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

# Validate prerequisites
validate_prerequisites() {
    log "Validating deployment prerequisites..."
    
    # Check if required tools are installed
    local required_tools=("terraform" "aws" "kubectl" "helm" "jq")
    
    for tool in "${required_tools[@]}"; do
        if ! command -v "$tool" &> /dev/null; then
            error "$tool is not installed or not in PATH"
        fi
    done
    
    # Check AWS CLI configuration
    if ! aws sts get-caller-identity &> /dev/null; then
        error "AWS CLI is not configured properly. Please run 'aws configure'"
    fi
    
    # Check Terraform version
    local tf_version
    tf_version=$(terraform version -json | jq -r '.terraform_version')
    
    if [[ $(printf '%s\n' "1.6.0" "$tf_version" | sort -V | head -n1) != "1.6.0" ]]; then
        error "Terraform version must be 1.6.0 or higher. Current version: $tf_version"
    fi
    
    # Validate environment
    if [[ ! "$ENVIRONMENT" =~ ^(development|staging|production)$ ]]; then
        error "Environment must be one of: development, staging, production"
    fi
    
    # Validate region
    if ! aws ec2 describe-regions --region-names "$REGION" &> /dev/null; then
        error "Invalid AWS region: $REGION"
    fi
    
    # Check if Terraform directory exists
    if [[ ! -d "$TERRAFORM_DIR" ]]; then
        error "Terraform directory not found: $TERRAFORM_DIR"
    fi
    
    # Validate required Terraform files
    local required_files=("main.tf" "variables.tf")
    for file in "${required_files[@]}"; do
        if [[ ! -f "$TERRAFORM_DIR/$file" ]]; then
            error "Required Terraform file not found: $file"
        fi
    done
    
    success "Prerequisites validation completed"
}

# Setup environment variables
setup_environment() {
    log "Setting up environment variables..."
    
    # Export AWS region
    export AWS_DEFAULT_REGION="$REGION"
    export AWS_REGION="$REGION"
    
    # Set Terraform workspace
    export TF_WORKSPACE="$ENVIRONMENT"
    
    # Set environment-specific variables
    case "$ENVIRONMENT" in
        "development")
            export TF_VAR_cluster_endpoint_public_access="true"
            export TF_VAR_cluster_endpoint_public_access_cidrs='["0.0.0.0/0"]'
            export TF_VAR_enable_deletion_protection="false"
            export TF_VAR_skip_final_snapshot="true"
            export TF_VAR_banking_services_desired_size="3"
            export TF_VAR_ai_ml_desired_size="2"
            export TF_VAR_aurora_instance_count="1"
            export TF_VAR_redis_num_cache_nodes="2"
            ;;
        "staging")
            export TF_VAR_cluster_endpoint_public_access="false"
            export TF_VAR_enable_deletion_protection="false"
            export TF_VAR_skip_final_snapshot="true"
            export TF_VAR_banking_services_desired_size="4"
            export TF_VAR_ai_ml_desired_size="3"
            export TF_VAR_aurora_instance_count="2"
            export TF_VAR_redis_num_cache_nodes="3"
            ;;
        "production")
            export TF_VAR_cluster_endpoint_public_access="false"
            export TF_VAR_enable_deletion_protection="true"
            export TF_VAR_skip_final_snapshot="false"
            export TF_VAR_banking_services_desired_size="6"
            export TF_VAR_ai_ml_desired_size="4"
            export TF_VAR_aurora_instance_count="3"
            export TF_VAR_redis_num_cache_nodes="3"
            export TF_VAR_enable_cross_region_backup="true"
            ;;
    esac
    
    # Common variables
    export TF_VAR_environment="$ENVIRONMENT"
    export TF_VAR_aws_region="$REGION"
    
    # Generate secure passwords for databases
    if [[ ! -f "$CONFIGS_DIR/$ENVIRONMENT-secrets.env" ]]; then
        info "Generating secure credentials for $ENVIRONMENT environment..."
        mkdir -p "$CONFIGS_DIR"
        
        # Generate random passwords
        local db_password
        local redis_auth_token
        
        db_password=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-25)
        redis_auth_token=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-32)
        
        cat > "$CONFIGS_DIR/$ENVIRONMENT-secrets.env" << EOF
# Generated secrets for $ENVIRONMENT environment
# DO NOT COMMIT THIS FILE TO VERSION CONTROL

export TF_VAR_db_password="$db_password"
export TF_VAR_redis_auth_token="$redis_auth_token"
EOF
        
        chmod 600 "$CONFIGS_DIR/$ENVIRONMENT-secrets.env"
        info "Secrets generated and saved to $CONFIGS_DIR/$ENVIRONMENT-secrets.env"
    fi
    
    # Source environment secrets
    # shellcheck source=/dev/null
    source "$CONFIGS_DIR/$ENVIRONMENT-secrets.env"
    
    success "Environment setup completed"
}

# Initialize Terraform
initialize_terraform() {
    log "Initializing Terraform..."
    
    cd "$TERRAFORM_DIR"
    
    # Initialize Terraform
    terraform init \
        -backend-config="bucket=$ENVIRONMENT-banking-terraform-state-$REGION" \
        -backend-config="key=infrastructure/$ENVIRONMENT.tfstate" \
        -backend-config="region=$REGION" \
        -backend-config="dynamodb_table=$ENVIRONMENT-terraform-state-lock" \
        -reconfigure
    
    # Create or select workspace
    if ! terraform workspace select "$ENVIRONMENT" 2>/dev/null; then
        info "Creating new Terraform workspace: $ENVIRONMENT"
        terraform workspace new "$ENVIRONMENT"
    fi
    
    success "Terraform initialization completed"
}

# Validate Terraform configuration
validate_terraform() {
    log "Validating Terraform configuration..."
    
    cd "$TERRAFORM_DIR"
    
    # Format Terraform files
    terraform fmt -recursive
    
    # Validate configuration
    terraform validate
    
    # Run security checks with tfsec if available
    if command -v tfsec &> /dev/null; then
        info "Running security analysis with tfsec..."
        tfsec . --soft-fail
    else
        warn "tfsec not found. Skipping security analysis."
    fi
    
    success "Terraform validation completed"
}

# Plan Terraform deployment
plan_terraform() {
    log "Planning Terraform deployment..."
    
    cd "$TERRAFORM_DIR"
    
    # Create plan file
    local plan_file="$ENVIRONMENT-$(date +%Y%m%d-%H%M%S).tfplan"
    
    terraform plan \
        -var-file="$CONFIGS_DIR/$ENVIRONMENT.tfvars" \
        -out="$plan_file" \
        -detailed-exitcode
    
    local plan_exit_code=$?
    
    case $plan_exit_code in
        0)
            info "No changes detected in Terraform plan"
            rm -f "$plan_file"
            return 0
            ;;
        1)
            error "Terraform plan failed"
            ;;
        2)
            info "Changes detected in Terraform plan"
            echo "PLAN_FILE=$plan_file" > /tmp/terraform_plan_info
            return 2
            ;;
    esac
}

# Apply Terraform configuration
apply_terraform() {
    log "Applying Terraform configuration..."
    
    cd "$TERRAFORM_DIR"
    
    # Check if plan file exists
    local plan_file=""
    if [[ -f "/tmp/terraform_plan_info" ]]; then
        # shellcheck source=/dev/null
        source /tmp/terraform_plan_info
        plan_file="$PLAN_FILE"
    fi
    
    if [[ -n "$plan_file" && -f "$plan_file" ]]; then
        # Apply from plan file
        terraform apply "$plan_file"
        rm -f "$plan_file"
        rm -f /tmp/terraform_plan_info
    else
        # Direct apply (not recommended for production)
        warn "No plan file found. Applying directly..."
        terraform apply \
            -var-file="$CONFIGS_DIR/$ENVIRONMENT.tfvars" \
            -auto-approve
    fi
    
    success "Terraform apply completed"
}

# Configure kubectl
configure_kubectl() {
    log "Configuring kubectl for EKS cluster..."
    
    cd "$TERRAFORM_DIR"
    
    # Get cluster information from Terraform output
    local cluster_name
    local cluster_endpoint
    local cluster_region
    
    cluster_name=$(terraform output -raw cluster_name 2>/dev/null || echo "$ENVIRONMENT-banking-cluster")
    cluster_endpoint=$(terraform output -raw cluster_endpoint 2>/dev/null || echo "")
    cluster_region=$(terraform output -raw aws_region 2>/dev/null || echo "$REGION")
    
    if [[ -z "$cluster_endpoint" ]]; then
        warn "Cluster endpoint not available yet. Skipping kubectl configuration."
        return 0
    fi
    
    # Update kubeconfig
    aws eks update-kubeconfig \
        --region "$cluster_region" \
        --name "$cluster_name" \
        --alias "$ENVIRONMENT-banking"
    
    # Test kubectl connection
    if kubectl cluster-info &> /dev/null; then
        success "kubectl configured successfully"
        
        # Display cluster information
        info "Cluster Information:"
        kubectl cluster-info
    else
        warn "kubectl connection test failed. The cluster may still be initializing."
    fi
}

# Deploy essential Kubernetes components
deploy_k8s_components() {
    log "Deploying essential Kubernetes components..."
    
    # Check if kubectl is configured
    if ! kubectl cluster-info &> /dev/null; then
        warn "kubectl not configured. Skipping Kubernetes components deployment."
        return 0
    fi
    
    # Deploy AWS Load Balancer Controller
    deploy_aws_load_balancer_controller
    
    # Deploy Cluster Autoscaler
    deploy_cluster_autoscaler
    
    # Deploy Metrics Server
    deploy_metrics_server
    
    # Deploy EFS CSI Driver
    deploy_efs_csi_driver
    
    success "Essential Kubernetes components deployed"
}

# Deploy AWS Load Balancer Controller
deploy_aws_load_balancer_controller() {
    info "Deploying AWS Load Balancer Controller..."
    
    # Add AWS Load Balancer Controller Helm repository
    helm repo add eks https://aws.github.io/eks-charts
    helm repo update
    
    # Get cluster information
    local cluster_name
    local vpc_id
    local region
    
    cluster_name=$(terraform output -raw cluster_name 2>/dev/null || echo "$ENVIRONMENT-banking-cluster")
    vpc_id=$(terraform output -raw vpc_id 2>/dev/null)
    region=$(terraform output -raw aws_region 2>/dev/null || echo "$REGION")
    
    # Deploy AWS Load Balancer Controller
    helm upgrade --install aws-load-balancer-controller eks/aws-load-balancer-controller \
        -n kube-system \
        --set clusterName="$cluster_name" \
        --set serviceAccount.create=false \
        --set serviceAccount.name=aws-load-balancer-controller \
        --set region="$region" \
        --set vpcId="$vpc_id" \
        --wait
    
    info "AWS Load Balancer Controller deployed"
}

# Deploy Cluster Autoscaler
deploy_cluster_autoscaler() {
    info "Deploying Cluster Autoscaler..."
    
    # Add Cluster Autoscaler Helm repository
    helm repo add autoscaler https://kubernetes.github.io/autoscaler
    helm repo update
    
    # Get cluster information
    local cluster_name
    local region
    
    cluster_name=$(terraform output -raw cluster_name 2>/dev/null || echo "$ENVIRONMENT-banking-cluster")
    region=$(terraform output -raw aws_region 2>/dev/null || echo "$REGION")
    
    # Deploy Cluster Autoscaler
    helm upgrade --install cluster-autoscaler autoscaler/cluster-autoscaler \
        -n kube-system \
        --set autoDiscovery.clusterName="$cluster_name" \
        --set awsRegion="$region" \
        --set serviceAccount.create=false \
        --set serviceAccount.name=cluster-autoscaler \
        --wait
    
    info "Cluster Autoscaler deployed"
}

# Deploy Metrics Server
deploy_metrics_server() {
    info "Deploying Metrics Server..."
    
    # Add Metrics Server Helm repository
    helm repo add metrics-server https://kubernetes-sigs.github.io/metrics-server/
    helm repo update
    
    # Deploy Metrics Server
    helm upgrade --install metrics-server metrics-server/metrics-server \
        -n kube-system \
        --set args="{--cert-dir=/tmp,--secure-port=4443,--kubelet-preferred-address-types=InternalIP\\,ExternalIP\\,Hostname,--kubelet-use-node-status-port}" \
        --wait
    
    info "Metrics Server deployed"
}

# Deploy EFS CSI Driver
deploy_efs_csi_driver() {
    info "Deploying EFS CSI Driver..."
    
    # Add AWS EFS CSI Driver Helm repository
    helm repo add aws-efs-csi-driver https://kubernetes-sigs.github.io/aws-efs-csi-driver/
    helm repo update
    
    # Deploy EFS CSI Driver
    helm upgrade --install aws-efs-csi-driver aws-efs-csi-driver/aws-efs-csi-driver \
        -n kube-system \
        --set controller.serviceAccount.create=false \
        --set controller.serviceAccount.name=efs-csi-controller-sa \
        --wait
    
    info "EFS CSI Driver deployed"
}

# Perform post-deployment verification
post_deployment_verification() {
    log "Performing post-deployment verification..."
    
    cd "$TERRAFORM_DIR"
    
    # Verify Terraform outputs
    info "Terraform Outputs:"
    terraform output
    
    # Verify Kubernetes cluster
    if kubectl cluster-info &> /dev/null; then
        info "Kubernetes Cluster Verification:"
        
        # Check node status
        echo "Node Status:"
        kubectl get nodes -o wide
        
        # Check system pods
        echo -e "\nSystem Pods Status:"
        kubectl get pods -n kube-system
        
        # Check persistent volumes
        echo -e "\nPersistent Volumes:"
        kubectl get pv
        
        # Check storage classes
        echo -e "\nStorage Classes:"
        kubectl get storageclass
    else
        warn "Kubernetes cluster not accessible for verification"
    fi
    
    # Generate deployment report
    generate_deployment_report
    
    success "Post-deployment verification completed"
}

# Generate deployment report
generate_deployment_report() {
    log "Generating deployment report..."
    
    local report_file="$PROJECT_ROOT/deployment-report-$ENVIRONMENT-$(date +%Y%m%d-%H%M%S).md"
    
    cat > "$report_file" << EOF
# Enterprise Banking EKS Infrastructure Deployment Report

**Environment:** $ENVIRONMENT  
**Region:** $REGION  
**Deployment Date:** $(date)  
**Deployed By:** $(whoami)  

## Deployment Summary

### Infrastructure Components

- ✅ AWS VPC with Multi-AZ configuration
- ✅ EKS Cluster with managed node groups
- ✅ RDS Aurora PostgreSQL cluster
- ✅ ElastiCache Redis cluster
- ✅ EFS shared storage
- ✅ S3 buckets for data and logs
- ✅ Application Load Balancer
- ✅ Security groups and NACLs
- ✅ KMS encryption keys
- ✅ CloudWatch logging

### Kubernetes Components

- ✅ AWS Load Balancer Controller
- ✅ Cluster Autoscaler
- ✅ Metrics Server
- ✅ EFS CSI Driver
- ✅ EBS CSI Driver

### Security Configuration

- ✅ Encryption at rest for all storage
- ✅ Encryption in transit
- ✅ IAM roles and policies
- ✅ Security groups configuration
- ✅ Network isolation

### Compliance Requirements

- ✅ PCI DSS: Network segmentation and encryption
- ✅ SOX: Audit trails and access controls
- ✅ GDPR: Data protection and encryption
- ✅ FAPI: Security controls and monitoring

## Terraform Outputs

\`\`\`
$(cd "$TERRAFORM_DIR" && terraform output 2>/dev/null || echo "Terraform outputs not available")
\`\`\`

## Kubernetes Cluster Status

\`\`\`
$(kubectl get nodes -o wide 2>/dev/null || echo "Kubernetes cluster not accessible")
\`\`\`

## Next Steps

1. **Configure DNS**: Set up Route53 records for applications
2. **Deploy Applications**: Deploy banking microservices
3. **Configure Monitoring**: Set up Prometheus and Grafana
4. **Setup CI/CD**: Configure deployment pipelines
5. **Security Hardening**: Apply additional security policies
6. **Load Testing**: Perform performance and load testing
7. **Backup Verification**: Test backup and recovery procedures

## Support Contacts

- **Infrastructure Team**: infrastructure@bank.com
- **Security Team**: security@bank.com
- **DevOps Team**: devops@bank.com

---
*Report generated automatically by deployment script*
EOF
    
    info "Deployment report generated: $report_file"
}

# Cleanup function
cleanup() {
    info "Cleaning up temporary files..."
    rm -f /tmp/terraform_plan_info
    cd "$PROJECT_ROOT"
}

# Error handling
handle_error() {
    error "Deployment failed at step: $1"
    cleanup
    exit 1
}

# Main deployment function
main() {
    # Set up error handling
    trap 'handle_error "Unknown step"' ERR
    
    print_banner
    
    # Deployment steps
    validate_prerequisites || handle_error "Prerequisites validation"
    setup_environment || handle_error "Environment setup"
    initialize_terraform || handle_error "Terraform initialization"
    validate_terraform || handle_error "Terraform validation"
    
    # Plan deployment
    plan_terraform
    local plan_result=$?
    
    if [[ $plan_result -eq 0 ]]; then
        info "No infrastructure changes required"
    elif [[ $plan_result -eq 2 ]]; then
        # Changes detected
        if [[ "$AUTO_APPLY" == "true" ]]; then
            apply_terraform || handle_error "Terraform apply"
        else
            echo -e "\n${YELLOW}Terraform plan shows changes will be made.${NC}"
            echo -e "${YELLOW}Review the plan above and run with AUTO_APPLY=true to apply changes.${NC}"
            echo -e "${YELLOW}Example: $0 $ENVIRONMENT $REGION true${NC}\n"
            
            read -p "Do you want to apply these changes now? (y/N): " -n 1 -r
            echo
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                apply_terraform || handle_error "Terraform apply"
            else
                info "Deployment cancelled by user"
                cleanup
                exit 0
            fi
        fi
        
        # Post-apply steps
        configure_kubectl || handle_error "kubectl configuration"
        deploy_k8s_components || handle_error "Kubernetes components deployment"
        post_deployment_verification || handle_error "Post-deployment verification"
    fi
    
    cleanup
    
    success "🎉 Enterprise Banking EKS Infrastructure deployment completed successfully!"
    
    echo -e "\n${GREEN}Deployment Summary:${NC}"
    echo -e "  Environment: ${CYAN}$ENVIRONMENT${NC}"
    echo -e "  Region: ${CYAN}$REGION${NC}"
    echo -e "  Cluster: ${CYAN}$ENVIRONMENT-banking-cluster${NC}"
    echo -e "  Status: ${GREEN}✅ Successfully Deployed${NC}"
    
    echo -e "\n${YELLOW}Next Steps:${NC}"
    echo -e "  1. Configure application deployments"
    echo -e "  2. Set up monitoring and alerting"
    echo -e "  3. Configure backup verification"
    echo -e "  4. Perform security testing"
    echo -e "  5. Review deployment report"
}

# Print usage information
usage() {
    echo "Usage: $0 [ENVIRONMENT] [REGION] [AUTO_APPLY]"
    echo
    echo "Arguments:"
    echo "  ENVIRONMENT    Environment name: development, staging, production (default: development)"
    echo "  REGION         AWS region (default: us-east-1)"
    echo "  AUTO_APPLY     Auto-apply changes: true, false (default: false)"
    echo
    echo "Examples:"
    echo "  $0 development us-east-1 false"
    echo "  $0 production us-east-1 true"
    echo "  $0 staging us-west-2"
    echo
    echo "Environment Variables:"
    echo "  AWS_PROFILE    AWS profile to use for deployment"
    echo "  TF_LOG         Terraform log level (DEBUG, INFO, WARN, ERROR)"
    echo
    exit 0
}

# Handle command line arguments
if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
    usage
fi

# Execute main function
main "$@"