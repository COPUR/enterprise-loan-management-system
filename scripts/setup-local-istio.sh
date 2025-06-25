#!/bin/bash

# Enterprise Loan Management System - Local Istio Setup
# Sets up local Kubernetes environment with Istio service mesh for testing

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
CLUSTER_NAME="banking-local"
ISTIO_VERSION="1.20.1"
KIALI_VERSION="v1.79"
PROMETHEUS_VERSION="v2.48.0"
GRAFANA_VERSION="10.2.2"

echo -e "${BLUE}🏦 Enterprise Banking System - Local Istio Environment Setup${NC}"
echo -e "${BLUE}================================================================${NC}"

# Function to print status
print_status() {
    echo -e "${GREEN}✓${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    echo -e "\n${BLUE}Checking prerequisites...${NC}"
    
    # Check if Docker is running
    if ! docker info > /dev/null 2>&1; then
        print_error "Docker is not running. Please start Docker first."
        exit 1
    fi
    print_status "Docker is running"
    
    # Check if kubectl is installed
    if ! command -v kubectl &> /dev/null; then
        print_error "kubectl is not installed. Please install kubectl first."
        exit 1
    fi
    print_status "kubectl is installed"
    
    # Check if kind is installed
    if ! command -v kind &> /dev/null; then
        print_warning "kind is not installed. Installing kind..."
        # Install kind
        if [[ "$OSTYPE" == "darwin"* ]]; then
            if command -v brew &> /dev/null; then
                brew install kind
            else
                curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.20.0/kind-darwin-amd64
                chmod +x ./kind
                sudo mv ./kind /usr/local/bin/kind
            fi
        else
            curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.20.0/kind-linux-amd64
            chmod +x ./kind
            sudo mv ./kind /usr/local/bin/kind
        fi
        print_status "kind installed"
    else
        print_status "kind is installed"
    fi
    
    # Check if istioctl is installed
    if ! command -v istioctl &> /dev/null; then
        print_warning "istioctl is not installed. Installing istioctl..."
        curl -L https://istio.io/downloadIstio | ISTIO_VERSION=${ISTIO_VERSION} sh -
        sudo mv istio-${ISTIO_VERSION}/bin/istioctl /usr/local/bin/
        rm -rf istio-${ISTIO_VERSION}
        print_status "istioctl installed"
    else
        print_status "istioctl is installed"
    fi
}

# Create kind cluster with proper configuration for Istio
create_cluster() {
    echo -e "\n${BLUE}Creating Kubernetes cluster with kind...${NC}"
    
    # Check if cluster already exists
    if kind get clusters | grep -q "^${CLUSTER_NAME}$"; then
        print_warning "Cluster ${CLUSTER_NAME} already exists. Deleting and recreating..."
        kind delete cluster --name ${CLUSTER_NAME}
    fi
    
    # Create kind cluster configuration
    cat > kind-config.yaml << EOF
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
name: ${CLUSTER_NAME}
nodes:
- role: control-plane
  kubeadmConfigPatches:
  - |
    kind: InitConfiguration
    nodeRegistration:
      kubeletExtraArgs:
        node-labels: "ingress-ready=true"
  extraPortMappings:
  - containerPort: 80
    hostPort: 80
    protocol: TCP
  - containerPort: 443
    hostPort: 443
    protocol: TCP
  - containerPort: 15021
    hostPort: 15021
    protocol: TCP
  - containerPort: 31400
    hostPort: 31400
    protocol: TCP
- role: worker
- role: worker
networking:
  disableDefaultCNI: false
  podSubnet: "10.244.0.0/16"
  serviceSubnet: "10.96.0.0/12"
EOF
    
    # Create the cluster
    kind create cluster --config kind-config.yaml
    print_status "Kubernetes cluster created"
    
    # Update kubeconfig
    kubectl cluster-info --context kind-${CLUSTER_NAME}
    print_status "kubectl configured for cluster"
    
    # Clean up config file
    rm kind-config.yaml
}

# Install Istio
install_istio() {
    echo -e "\n${BLUE}Installing Istio service mesh...${NC}"
    
    # Install Istio with demo profile (suitable for local testing)
    istioctl install --set values.defaultRevision=default --set values.pilot.env.EXTERNAL_ISTIOD=false -y
    print_status "Istio control plane installed"
    
    # Enable automatic sidecar injection for default namespace
    kubectl label namespace default istio-injection=enabled
    print_status "Automatic sidecar injection enabled for default namespace"
    
    # Create banking system namespace and enable injection
    kubectl create namespace banking-system || true
    kubectl label namespace banking-system istio-injection=enabled
    print_status "Banking system namespace created with Istio injection"
    
    # Install Istio addons
    echo -e "\n${BLUE}Installing Istio addons...${NC}"
    
    # Kiali
    kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-${ISTIO_VERSION%.*}/samples/addons/kiali.yaml
    print_status "Kiali installed"
    
    # Prometheus
    kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-${ISTIO_VERSION%.*}/samples/addons/prometheus.yaml
    print_status "Prometheus installed"
    
    # Grafana
    kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-${ISTIO_VERSION%.*}/samples/addons/grafana.yaml
    print_status "Grafana installed"
    
    # Jaeger
    kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-${ISTIO_VERSION%.*}/samples/addons/jaeger.yaml
    print_status "Jaeger installed"
    
    # Wait for addons to be ready
    echo -e "\n${BLUE}Waiting for addons to be ready...${NC}"
    kubectl wait --for=condition=ready pod -l app=kiali -n istio-system --timeout=300s
    kubectl wait --for=condition=ready pod -l app=prometheus -n istio-system --timeout=300s
    kubectl wait --for=condition=ready pod -l app=grafana -n istio-system --timeout=300s
    print_status "All addons are ready"
}

# Create Istio gateway and virtual service
create_istio_gateway() {
    echo -e "\n${BLUE}Creating Istio gateway and virtual services...${NC}"
    
    # Create Istio gateway
    cat > istio-gateway.yaml << EOF
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: banking-gateway
  namespace: banking-system
spec:
  selector:
    istio: ingressgateway
  servers:
  - port:
      number: 80
      name: http
      protocol: HTTP
    hosts:
    - banking.local
    - "*.banking.local"
  - port:
      number: 443
      name: https
      protocol: HTTPS
    tls:
      mode: SIMPLE
      credentialName: banking-tls
    hosts:
    - banking.local
    - "*.banking.local"
---
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: banking-virtualservice
  namespace: banking-system
spec:
  hosts:
  - banking.local
  gateways:
  - banking-gateway
  http:
  - match:
    - uri:
        prefix: /api
    route:
    - destination:
        host: enterprise-loan-system
        port:
          number: 8080
    timeout: 30s
    retries:
      attempts: 3
      perTryTimeout: 10s
  - match:
    - uri:
        prefix: /actuator
    route:
    - destination:
        host: enterprise-loan-system
        port:
          number: 8080
  - match:
    - uri:
        prefix: /swagger-ui
    route:
    - destination:
        host: enterprise-loan-system
        port:
          number: 8080
  - match:
    - uri:
        prefix: /graphql
    route:
    - destination:
        host: enterprise-loan-system
        port:
          number: 8080
  - match:
    - uri:
        prefix: /
    route:
    - destination:
        host: enterprise-loan-system
        port:
          number: 8080
---
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: banking-destination-rule
  namespace: banking-system
spec:
  host: enterprise-loan-system
  trafficPolicy:
    loadBalancer:
      simple: LEAST_CONN
    connectionPool:
      tcp:
        maxConnections: 100
      http:
        http1MaxPendingRequests: 50
        maxRequestsPerConnection: 10
        consecutiveGatewayErrors: 5
        interval: 30s
        baseEjectionTime: 30s
    circuitBreaker:
      consecutiveGatewayErrors: 5
      interval: 30s
      baseEjectionTime: 30s
      maxEjectionPercent: 50
  subsets:
  - name: v1
    labels:
      version: v1
EOF
    
    kubectl apply -f istio-gateway.yaml
    print_status "Istio gateway and virtual service created"
    
    # Clean up
    rm istio-gateway.yaml
}

# Deploy PostgreSQL and Redis
deploy_dependencies() {
    echo -e "\n${BLUE}Deploying PostgreSQL and Redis...${NC}"
    
    # PostgreSQL
    cat > postgres-deployment.yaml << EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres
  namespace: banking-system
  labels:
    app: postgres
spec:
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
        version: v1
    spec:
      containers:
      - name: postgres
        image: postgres:16-alpine
        env:
        - name: POSTGRES_DB
          value: banking_system
        - name: POSTGRES_USER
          value: postgres
        - name: POSTGRES_PASSWORD
          value: password
        - name: PGDATA
          value: /var/lib/postgresql/data/pgdata
        ports:
        - containerPort: 5432
        volumeMounts:
        - name: postgres-storage
          mountPath: /var/lib/postgresql/data
        readinessProbe:
          exec:
            command:
            - pg_isready
            - -U
            - postgres
          initialDelaySeconds: 5
          periodSeconds: 5
        livenessProbe:
          exec:
            command:
            - pg_isready
            - -U
            - postgres
          initialDelaySeconds: 30
          periodSeconds: 10
      volumes:
      - name: postgres-storage
        emptyDir: {}
---
apiVersion: v1
kind: Service
metadata:
  name: postgres
  namespace: banking-system
  labels:
    app: postgres
spec:
  ports:
  - port: 5432
    targetPort: 5432
  selector:
    app: postgres
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis
  namespace: banking-system
  labels:
    app: redis
spec:
  replicas: 1
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
        version: v1
    spec:
      containers:
      - name: redis
        image: redis:7-alpine
        ports:
        - containerPort: 6379
        command:
        - redis-server
        - --appendonly
        - "yes"
        readinessProbe:
          exec:
            command:
            - redis-cli
            - ping
          initialDelaySeconds: 5
          periodSeconds: 5
        livenessProbe:
          exec:
            command:
            - redis-cli
            - ping
          initialDelaySeconds: 30
          periodSeconds: 10
---
apiVersion: v1
kind: Service
metadata:
  name: redis
  namespace: banking-system
  labels:
    app: redis
spec:
  ports:
  - port: 6379
    targetPort: 6379
  selector:
    app: redis
EOF
    
    kubectl apply -f postgres-deployment.yaml
    print_status "PostgreSQL and Redis deployed"
    
    # Wait for databases to be ready
    kubectl wait --for=condition=ready pod -l app=postgres -n banking-system --timeout=300s
    kubectl wait --for=condition=ready pod -l app=redis -n banking-system --timeout=300s
    print_status "Databases are ready"
    
    # Clean up
    rm postgres-deployment.yaml
}

# Deploy the banking application
deploy_banking_app() {
    echo -e "\n${BLUE}Deploying banking application...${NC}"
    
    # Build the application first
    echo -e "${YELLOW}Building application...${NC}"
    cd "$(dirname "$0")/.."
    ./gradlew clean bootJar --no-daemon
    
    # Build Docker image
    docker build -t banking-app:local .
    
    # Load image into kind cluster
    kind load docker-image banking-app:local --name ${CLUSTER_NAME}
    
    # Create application deployment
    cat > banking-app-deployment.yaml << EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: enterprise-loan-system
  namespace: banking-system
  labels:
    app: enterprise-loan-system
    version: v1
spec:
  replicas: 2
  selector:
    matchLabels:
      app: enterprise-loan-system
      version: v1
  template:
    metadata:
      labels:
        app: enterprise-loan-system
        version: v1
      annotations:
        sidecar.istio.io/inject: "true"
    spec:
      containers:
      - name: banking-app
        image: banking-app:local
        imagePullPolicy: Never
        ports:
        - containerPort: 8080
          protocol: TCP
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "local,docker"
        - name: DATABASE_URL
          value: "jdbc:postgresql://postgres:5432/banking_system"
        - name: DATABASE_USERNAME
          value: "postgres"
        - name: DATABASE_PASSWORD
          value: "password"
        - name: REDIS_HOST
          value: "redis"
        - name: REDIS_PORT
          value: "6379"
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 90
          periodSeconds: 30
          timeoutSeconds: 5
          failureThreshold: 3
        startupProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 30
---
apiVersion: v1
kind: Service
metadata:
  name: enterprise-loan-system
  namespace: banking-system
  labels:
    app: enterprise-loan-system
spec:
  ports:
  - port: 8080
    targetPort: 8080
    name: http
  selector:
    app: enterprise-loan-system
EOF
    
    kubectl apply -f banking-app-deployment.yaml
    print_status "Banking application deployed"
    
    # Wait for application to be ready
    echo -e "${YELLOW}Waiting for banking application to be ready...${NC}"
    kubectl wait --for=condition=ready pod -l app=enterprise-loan-system -n banking-system --timeout=600s
    print_status "Banking application is ready"
    
    # Clean up
    rm banking-app-deployment.yaml
}

# Configure local DNS
configure_dns() {
    echo -e "\n${BLUE}Configuring local DNS...${NC}"
    
    # Get the Istio ingress gateway external IP
    INGRESS_HOST=$(kubectl get po -l istio=ingressgateway -n istio-system -o jsonpath='{.items[0].status.hostIP}')
    INGRESS_PORT=$(kubectl -n istio-system get service istio-ingressgateway -o jsonpath='{.spec.ports[?(@.name=="http2")].nodePort}')
    SECURE_INGRESS_PORT=$(kubectl -n istio-system get service istio-ingressgateway -o jsonpath='{.spec.ports[?(@.name=="https")].nodePort}')
    
    print_status "Ingress Gateway: http://${INGRESS_HOST}:${INGRESS_PORT}"
    print_status "Secure Ingress Gateway: https://${INGRESS_HOST}:${SECURE_INGRESS_PORT}"
    
    # Add entries to /etc/hosts
    if ! grep -q "banking.local" /etc/hosts; then
        print_warning "Adding banking.local to /etc/hosts (requires sudo)"
        echo "${INGRESS_HOST} banking.local" | sudo tee -a /etc/hosts > /dev/null
        echo "${INGRESS_HOST} api.banking.local" | sudo tee -a /etc/hosts > /dev/null
        echo "${INGRESS_HOST} grafana.banking.local" | sudo tee -a /etc/hosts > /dev/null
        echo "${INGRESS_HOST} kiali.banking.local" | sudo tee -a /etc/hosts > /dev/null
        print_status "DNS entries added to /etc/hosts"
    else
        print_status "DNS entries already exist in /etc/hosts"
    fi
}

# Setup port forwarding for observability tools
setup_port_forwarding() {
    echo -e "\n${BLUE}Setting up port forwarding for observability tools...${NC}"
    
    # Create port forwarding script
    cat > port-forward-services.sh << 'EOF'
#!/bin/bash
echo "🔗 Setting up port forwarding for Istio services..."

# Kill existing port-forward processes
pkill -f "kubectl.*port-forward" || true

# Wait a moment
sleep 2

# Port forward Kiali
kubectl port-forward svc/kiali 20001:20001 -n istio-system > /dev/null 2>&1 &
echo "✓ Kiali: http://localhost:20001"

# Port forward Grafana
kubectl port-forward svc/grafana 3000:3000 -n istio-system > /dev/null 2>&1 &
echo "✓ Grafana: http://localhost:3000"

# Port forward Prometheus
kubectl port-forward svc/prometheus 9090:9090 -n istio-system > /dev/null 2>&1 &
echo "✓ Prometheus: http://localhost:9090"

# Port forward Jaeger
kubectl port-forward svc/tracing 80:80 -n istio-system > /dev/null 2>&1 &
echo "✓ Jaeger: http://localhost:80"

# Port forward Banking Application directly (alternative access)
kubectl port-forward svc/enterprise-loan-system 8080:8080 -n banking-system > /dev/null 2>&1 &
echo "✓ Banking App (direct): http://localhost:8080"

echo ""
echo "🎯 Access URLs:"
echo "   Banking App: http://banking.local:${INGRESS_PORT:-80}"
echo "   API Docs: http://banking.local:${INGRESS_PORT:-80}/swagger-ui.html"
echo "   GraphQL: http://banking.local:${INGRESS_PORT:-80}/graphql"
echo "   Health Check: http://banking.local:${INGRESS_PORT:-80}/actuator/health"
echo ""
echo "📊 Observability:"
echo "   Kiali (Service Mesh): http://localhost:20001"
echo "   Grafana (Metrics): http://localhost:3000"
echo "   Prometheus (Metrics): http://localhost:9090"
echo "   Jaeger (Tracing): http://localhost:80"
echo ""
echo "Press Ctrl+C to stop port forwarding"
wait
EOF
    
    chmod +x port-forward-services.sh
    print_status "Port forwarding script created: ./port-forward-services.sh"
}

# Create management scripts
create_management_scripts() {
    echo -e "\n${BLUE}Creating management scripts...${NC}"
    
    # Create cleanup script
    cat > cleanup-local-istio.sh << EOF
#!/bin/bash
echo "🧹 Cleaning up local Istio environment..."

# Stop port forwarding
pkill -f "kubectl.*port-forward" || true

# Delete kind cluster
kind delete cluster --name ${CLUSTER_NAME} || true

# Remove DNS entries (requires manual intervention)
echo "⚠️  Please manually remove these entries from /etc/hosts:"
echo "   ${INGRESS_HOST:-127.0.0.1} banking.local"
echo "   ${INGRESS_HOST:-127.0.0.1} api.banking.local"
echo "   ${INGRESS_HOST:-127.0.0.1} grafana.banking.local"
echo "   ${INGRESS_HOST:-127.0.0.1} kiali.banking.local"

echo "✓ Local Istio environment cleaned up"
EOF
    
    chmod +x cleanup-local-istio.sh
    
    # Create status script
    cat > status-local-istio.sh << 'EOF'
#!/bin/bash
echo "📊 Local Istio Environment Status"
echo "=================================="

echo ""
echo "🔧 Cluster Status:"
kubectl cluster-info --context kind-banking-local 2>/dev/null || echo "❌ Cluster not running"

echo ""
echo "📦 Istio System Pods:"
kubectl get pods -n istio-system

echo ""
echo "🏦 Banking System Pods:"
kubectl get pods -n banking-system

echo ""
echo "🌐 Services:"
kubectl get svc -n banking-system
kubectl get svc -n istio-system | grep -E "(istio-ingressgateway|kiali|grafana|prometheus|jaeger)"

echo ""
echo "🚪 Istio Gateways:"
kubectl get gateway -n banking-system

echo ""
echo "🔄 Virtual Services:"
kubectl get virtualservice -n banking-system

echo ""
echo "📈 Destination Rules:"
kubectl get destinationrule -n banking-system
EOF
    
    chmod +x status-local-istio.sh
    
    print_status "Management scripts created"
}

# Main execution
main() {
    echo -e "\n${BLUE}Starting local Istio environment setup...${NC}"
    
    check_prerequisites
    create_cluster
    install_istio
    create_istio_gateway
    deploy_dependencies
    deploy_banking_app
    configure_dns
    setup_port_forwarding
    create_management_scripts
    
    echo -e "\n${GREEN}🎉 Local Istio environment setup complete!${NC}"
    echo -e "\n${BLUE}📋 Summary:${NC}"
    echo -e "   Cluster: ${CLUSTER_NAME}"
    echo -e "   Istio Version: ${ISTIO_VERSION}"
    echo -e "   Banking App: http://banking.local:${INGRESS_PORT:-80}"
    echo -e "\n${BLUE}🚀 Next Steps:${NC}"
    echo -e "   1. Run: ${YELLOW}./port-forward-services.sh${NC} (for observability tools)"
    echo -e "   2. Visit: ${YELLOW}http://banking.local:${INGRESS_PORT:-80}${NC}"
    echo -e "   3. Check status: ${YELLOW}./status-local-istio.sh${NC}"
    echo -e "   4. Clean up: ${YELLOW}./cleanup-local-istio.sh${NC}"
    echo -e "\n${GREEN}Happy testing with Istio service mesh! 🕸️${NC}"
}

# Execute main function
main "$@"