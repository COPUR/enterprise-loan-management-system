#!/bin/bash

# Enterprise Banking System - Minimal Deployment Script
# This script builds and deploys a minimal working version of the banking system

set -e

echo "🏦 Enterprise Banking System - Minimal Deployment"
echo "================================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check prerequisites
print_status "Checking prerequisites..."

if ! command -v docker &> /dev/null; then
    print_error "Docker is not installed or not in PATH"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    print_error "Docker Compose is not installed or not in PATH"
    exit 1
fi

print_success "Prerequisites check passed"

# Set project directory
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_DIR"

print_status "Project directory: $PROJECT_DIR"

# Create necessary directories
print_status "Creating necessary directories..."
mkdir -p docker/init-scripts
mkdir -p monitoring/prometheus
mkdir -p monitoring/grafana/provisioning
mkdir -p logs

# Stop any existing containers
print_status "Stopping existing containers..."
docker-compose -f docker-compose-minimal.yml down --remove-orphans || true

# Clean up old images (optional)
if [ "$1" = "--clean" ]; then
    print_status "Cleaning up old Docker images..."
    docker image prune -f
    docker volume prune -f
fi

# Build and start services
print_status "Building and starting services..."
docker-compose -f docker-compose-minimal.yml build --no-cache

# Start infrastructure services first
print_status "Starting infrastructure services (PostgreSQL, Redis)..."
docker-compose -f docker-compose-minimal.yml up -d postgres redis

# Wait for infrastructure to be ready
print_status "Waiting for infrastructure services to be ready..."
sleep 30

# Check PostgreSQL health
print_status "Checking PostgreSQL connection..."
for i in {1..30}; do
    if docker-compose -f docker-compose-minimal.yml exec postgres pg_isready -U banking_user -d banking_system; then
        print_success "PostgreSQL is ready"
        break
    fi
    if [ $i -eq 30 ]; then
        print_error "PostgreSQL failed to start"
        exit 1
    fi
    sleep 2
done

# Check Redis health
print_status "Checking Redis connection..."
for i in {1..30}; do
    if docker-compose -f docker-compose-minimal.yml exec redis redis-cli ping; then
        print_success "Redis is ready"
        break
    fi
    if [ $i -eq 30 ]; then
        print_error "Redis failed to start"
        exit 1
    fi
    sleep 2
done

# Start the banking application
print_status "Starting banking application..."
docker-compose -f docker-compose-minimal.yml up -d banking-app

# Wait for application to be ready
print_status "Waiting for banking application to be ready..."
sleep 60

# Check application health
print_status "Checking application health..."
for i in {1..60}; do
    if curl -f http://localhost:8080/actuator/health 2>/dev/null; then
        print_success "Banking application is ready"
        break
    fi
    if [ $i -eq 60 ]; then
        print_warning "Banking application health check failed, but continuing..."
        break
    fi
    sleep 3
done

# Start monitoring services
print_status "Starting monitoring services (Prometheus, Grafana)..."
docker-compose -f docker-compose-minimal.yml up -d prometheus grafana

# Wait for monitoring to be ready
sleep 30

# Display status
print_status "Checking all services status..."
docker-compose -f docker-compose-minimal.yml ps

echo ""
print_success "🎉 Deployment completed!"
echo ""
echo "🔗 Service URLs:"
echo "   Banking Application: http://localhost:8080"
echo "   Health Check:       http://localhost:8080/actuator/health"
echo "   Metrics:            http://localhost:8080/actuator/metrics"
echo "   Prometheus:         http://localhost:9090"
echo "   Grafana:            http://localhost:3000 (admin/banking_grafana_pass)"
echo ""
echo "📊 Monitoring:"
echo "   Application logs:   docker-compose -f docker-compose-minimal.yml logs -f banking-app"
echo "   All services:       docker-compose -f docker-compose-minimal.yml logs -f"
echo ""
echo "🛑 To stop all services:"
echo "   docker-compose -f docker-compose-minimal.yml down"
echo ""

# Test basic endpoints
print_status "Testing basic endpoints..."

echo "Testing health endpoint..."
if curl -s http://localhost:8080/actuator/health | grep -q "UP"; then
    print_success "Health endpoint is working"
else
    print_warning "Health endpoint test failed"
fi

echo "Testing metrics endpoint..."
if curl -s http://localhost:8080/actuator/metrics | grep -q "names"; then
    print_success "Metrics endpoint is working"
else
    print_warning "Metrics endpoint test failed"
fi

print_success "Minimal Enterprise Banking System is now running!"
print_status "Check the logs with: docker-compose -f docker-compose-minimal.yml logs -f"