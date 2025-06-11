#!/bin/bash
# Enterprise Loan Management System - EKS Deployment Script

set -e

# Configuration
CLUSTER_NAME="enterprise-loan-system"
AWS_REGION="us-west-2"
NAMESPACE="banking-system"
HELM_RELEASE_NAME="enterprise-loan-system"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Logging function
log() {
    echo -e "${GREEN}[$(date '+%Y-%m-%d %H:%M:%S')] $1${NC}"
}

warn() {
    echo -e "${YELLOW}[$(date '+%Y-%m-%d %H:%M:%S')] WARNING: $1${NC}"
}

error() {
    echo -e "${RED}[$(date '+%Y-%m-%d %H:%M:%S')] ERROR: $1${NC}"
    exit 1
}

# Check prerequisites
check_prerequisites() {
    log "Checking prerequisites..."
    
    # Check AWS CLI
    if ! command -v aws &> /dev/null; then
        error "AWS CLI is not installed"
    fi
    
    # Check kubectl
    if ! command -v kubectl &> /dev/null; then
        error "kubectl is not installed"
    fi
    
    # Check Helm
    if ! command -v helm &> /dev/null; then
        error "Helm is not installed"
    fi
    
    # Check Docker
    if ! command -v docker &> /dev/null; then
        error "Docker is not installed"
    fi
    
    # Check Terraform
    if ! command -v terraform &> /dev/null; then
        error "Terraform is not installed"
    fi
    
    log "All prerequisites satisfied"
}

# Deploy infrastructure
deploy_infrastructure() {
    log "Deploying AWS infrastructure with Terraform..."
    
    cd terraform/aws-eks
    
    # Initialize Terraform
    terraform init
    
    # Validate configuration
    terraform validate
    
    # Plan deployment
    terraform plan -out=tfplan
    
    # Apply infrastructure
    terraform apply tfplan
    
    # Extract outputs
    export RDS_ENDPOINT=$(terraform output -raw rds_endpoint)
    export REDIS_ENDPOINT=$(terraform output -raw redis_endpoint)
    export CLUSTER_NAME=$(terraform output -raw cluster_name)
    
    cd ../..
    
    log "Infrastructure deployment completed"
}

# Configure kubectl
configure_kubectl() {
    log "Configuring kubectl for EKS cluster..."
    
    aws eks update-kubeconfig --region $AWS_REGION --name $CLUSTER_NAME
    
    # Verify cluster access
    kubectl get nodes || error "Failed to connect to EKS cluster"
    
    log "kubectl configured successfully"
}

# Install cluster components
install_cluster_components() {
    log "Installing essential cluster components..."
    
    # Install AWS Load Balancer Controller
    log "Installing AWS Load Balancer Controller..."
    kubectl apply -k "github.com/aws/eks-charts/stable/aws-load-balancer-controller//crds?ref=master"
    
    helm repo add eks https://aws.github.io/eks-charts
    helm repo update
    
    helm upgrade --install aws-load-balancer-controller eks/aws-load-balancer-controller \
        -n kube-system \
        --set clusterName=$CLUSTER_NAME \
        --set serviceAccount.create=true \
        --set serviceAccount.name=aws-load-balancer-controller \
        --wait
    
    # Install Metrics Server
    log "Installing Metrics Server..."
    kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
    
    # Install Cluster Autoscaler
    log "Installing Cluster Autoscaler..."
    kubectl apply -f https://raw.githubusercontent.com/kubernetes/autoscaler/master/cluster-autoscaler/cloudprovider/aws/examples/cluster-autoscaler-autodiscover.yaml
    
    kubectl -n kube-system annotate deployment.apps/cluster-autoscaler cluster-autoscaler.kubernetes.io/safe-to-evict="false"
    
    log "Cluster components installed successfully"
}

# Deploy monitoring stack
deploy_monitoring() {
    log "Deploying monitoring stack..."
    
    # Add Helm repositories
    helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
    helm repo add grafana https://grafana.github.io/helm-charts
    helm repo add elastic https://helm.elastic.co
    helm repo update
    
    # Create monitoring namespace
    kubectl create namespace monitoring --dry-run=client -o yaml | kubectl apply -f -
    
    # Install Prometheus and Grafana
    log "Installing Prometheus and Grafana..."
    helm upgrade --install prometheus prometheus-community/kube-prometheus-stack \
        --namespace monitoring \
        --set prometheus.prometheusSpec.serviceMonitorSelectorNilUsesHelmValues=false \
        --set grafana.adminPassword=admin123 \
        --set grafana.service.type=LoadBalancer \
        --wait
    
    # Install ELK Stack
    log "Installing ELK Stack..."
    helm upgrade --install elasticsearch elastic/elasticsearch \
        --namespace monitoring \
        --set replicas=1 \
        --set minimumMasterNodes=1 \
        --wait
    
    helm upgrade --install kibana elastic/kibana \
        --namespace monitoring \
        --set service.type=LoadBalancer \
        --wait
    
    helm upgrade --install logstash elastic/logstash \
        --namespace monitoring \
        --wait
    
    log "Monitoring stack deployed successfully"
}

# Deploy ArgoCD
deploy_argocd() {
    log "Deploying ArgoCD for GitOps..."
    
    # Create ArgoCD namespace
    kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -
    
    # Install ArgoCD
    kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
    
    # Wait for ArgoCD to be ready
    kubectl wait --for=condition=available --timeout=600s deployment/argocd-server -n argocd
    
    # Configure ArgoCD service as LoadBalancer
    kubectl patch svc argocd-server -n argocd -p '{"spec": {"type": "LoadBalancer"}}'
    
    # Apply ArgoCD applications
    kubectl apply -f k8s/argocd/application.yaml
    
    log "ArgoCD deployed successfully"
}

# Build and push application image
build_and_push_image() {
    log "Building and pushing application container image..."
    
    # Get AWS account ID
    AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
    ECR_REGISTRY="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"
    IMAGE_NAME="$ECR_REGISTRY/enterprise-loan-system"
    
    # Create ECR repository if it doesn't exist
    aws ecr describe-repositories --repository-names enterprise-loan-system --region $AWS_REGION 2>/dev/null || \
        aws ecr create-repository --repository-name enterprise-loan-system --region $AWS_REGION
    
    # Login to ECR
    aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR_REGISTRY
    
    # Build application
    ./gradlew bootJar
    
    # Build Docker image
    docker build -t $IMAGE_NAME:latest -t $IMAGE_NAME:$(git rev-parse --short HEAD) .
    
    # Push images
    docker push $IMAGE_NAME:latest
    docker push $IMAGE_NAME:$(git rev-parse --short HEAD)
    
    export IMAGE_TAG=$(git rev-parse --short HEAD)
    
    log "Container image built and pushed successfully"
}

# Deploy application
deploy_application() {
    log "Deploying Enterprise Loan Management System..."
    
    # Create namespace
    kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -
    
    # Create secrets (placeholder - should be configured with real values)
    kubectl create secret generic banking-app-secrets \
        --from-literal=database-password="$DB_PASSWORD" \
        --from-literal=redis-password="$REDIS_PASSWORD" \
        --from-literal=jwt-secret="$JWT_SECRET" \
        --namespace $NAMESPACE \
        --dry-run=client -o yaml | kubectl apply -f -
    
    # Deploy using Helm
    helm upgrade --install $HELM_RELEASE_NAME ./k8s/helm-charts/enterprise-loan-system \
        --namespace $NAMESPACE \
        --set image.repository="$ECR_REGISTRY/enterprise-loan-system" \
        --set image.tag="$IMAGE_TAG" \
        --set database.host="$RDS_ENDPOINT" \
        --set redis.host="$REDIS_ENDPOINT" \
        --set app.env=production \
        --wait --timeout=10m
    
    log "Application deployed successfully"
}

# Validate deployment
validate_deployment() {
    log "Validating deployment..."
    
    # Check pod status
    kubectl get pods -n $NAMESPACE
    
    # Wait for pods to be ready
    kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=enterprise-loan-system -n $NAMESPACE --timeout=300s
    
    # Check services
    kubectl get svc -n $NAMESPACE
    
    # Check ingress
    kubectl get ingress -n $NAMESPACE
    
    # Health check
    log "Performing health checks..."
    
    # Port forward for health check
    kubectl port-forward svc/enterprise-loan-system 8080:5000 -n $NAMESPACE &
    PF_PID=$!
    
    sleep 10
    
    # Health check
    if curl -f http://localhost:8080/actuator/health; then
        log "Health check passed"
    else
        error "Health check failed"
    fi
    
    # Cache health check
    if curl -f http://localhost:8080/api/v1/cache/health; then
        log "Cache health check passed"
    else
        warn "Cache health check failed"
    fi
    
    # Banking compliance check
    if curl -f http://localhost:8080/api/v1/tdd/coverage-report; then
        log "Banking compliance check passed"
    else
        warn "Banking compliance check failed"
    fi
    
    # Clean up port forward
    kill $PF_PID 2>/dev/null || true
    
    log "Deployment validation completed"
}

# Print access information
print_access_info() {
    log "Deployment completed successfully!"
    
    echo ""
    echo "=== Access Information ==="
    
    # Application URL
    ALB_URL=$(kubectl get ingress enterprise-loan-system-ingress -n $NAMESPACE -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "Pending...")
    echo "Application URL: https://$ALB_URL"
    
    # ArgoCD URL
    ARGOCD_URL=$(kubectl get svc argocd-server -n argocd -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "Pending...")
    echo "ArgoCD URL: https://$ARGOCD_URL"
    
    # Grafana URL
    GRAFANA_URL=$(kubectl get svc prometheus-grafana -n monitoring -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "Pending...")
    echo "Grafana URL: http://$GRAFANA_URL (admin/admin123)"
    
    # Kibana URL
    KIBANA_URL=$(kubectl get svc kibana-kibana -n monitoring -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "Pending...")
    echo "Kibana URL: http://$KIBANA_URL"
    
    # ArgoCD admin password
    ARGOCD_PASSWORD=$(kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d 2>/dev/null || echo "Check manually")
    echo "ArgoCD admin password: $ARGOCD_PASSWORD"
    
    echo ""
    echo "=== Next Steps ==="
    echo "1. Configure DNS for your domain to point to the ALB"
    echo "2. Update SSL certificate ARN in ingress configuration"
    echo "3. Configure proper secrets in AWS Secrets Manager"
    echo "4. Set up GitOps repository for automated deployments"
    echo "5. Configure monitoring alerts and notifications"
    
    log "Banking system is ready for production use!"
}

# Main deployment workflow
main() {
    log "Starting Enterprise Loan Management System EKS Deployment"
    
    # Check for required environment variables
    if [[ -z "$DB_PASSWORD" || -z "$REDIS_PASSWORD" || -z "$JWT_SECRET" ]]; then
        error "Required environment variables not set: DB_PASSWORD, REDIS_PASSWORD, JWT_SECRET"
    fi
    
    check_prerequisites
    deploy_infrastructure
    configure_kubectl
    install_cluster_components
    deploy_monitoring
    deploy_argocd
    build_and_push_image
    deploy_application
    validate_deployment
    print_access_info
    
    log "Deployment completed successfully! 🚀"
}

# Handle script arguments
case "${1:-deploy}" in
    "deploy")
        main
        ;;
    "validate")
        validate_deployment
        ;;
    "clean")
        log "Cleaning up deployment..."
        helm uninstall $HELM_RELEASE_NAME -n $NAMESPACE || true
        kubectl delete namespace $NAMESPACE || true
        log "Cleanup completed"
        ;;
    *)
        echo "Usage: $0 [deploy|validate|clean]"
        exit 1
        ;;
esac