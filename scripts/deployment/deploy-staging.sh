#!/bin/bash

# Staging Deployment Script for Enterprise Banking Platform
# 
# Simplified deployment script for staging environment with additional testing

set -euo pipefail

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Source utilities
source "$SCRIPT_DIR/config/deployment-config.sh"
source "$SCRIPT_DIR/utils/deployment-utils.sh"
source "$SCRIPT_DIR/utils/health-checks.sh"

# Default values
ENVIRONMENT="staging"
DEPLOYMENT_TYPE="rolling"
RUN_INTEGRATION_TESTS=true
RUN_PERFORMANCE_TESTS=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -v|--version)
            BUILD_VERSION="$2"
            shift 2
            ;;
        --skip-tests)
            RUN_INTEGRATION_TESTS=false
            shift
            ;;
        --performance-tests)
            RUN_PERFORMANCE_TESTS=true
            shift
            ;;
        -h|--help)
            echo "Usage: $0 -v VERSION [--skip-tests] [--performance-tests]"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

if [[ -z "${BUILD_VERSION:-}" ]]; then
    echo "Error: Build version is required. Use -v or --version"
    exit 1
fi

echo "=== Enterprise Banking Platform - Staging Deployment ==="
echo "Version: $BUILD_VERSION"
echo "Environment: $ENVIRONMENT"
echo "Deployment Type: $DEPLOYMENT_TYPE"
echo "Integration Tests: $RUN_INTEGRATION_TESTS"
echo "Performance Tests: $RUN_PERFORMANCE_TESTS"
echo

# Execute staging deployment
echo "Starting staging deployment..."

# Pre-deployment checks
echo "Running pre-deployment checks..."
check_prerequisites
validate_build_version "$BUILD_VERSION"
check_environment_health "$ENVIRONMENT"

# Deploy application
echo "Deploying to staging environment..."
deploy_to_environment "$ENVIRONMENT" "$BUILD_VERSION"

# Wait for deployment to be ready
echo "Waiting for deployment to be healthy..."
wait_for_healthy_deployment "$ENVIRONMENT" "$BUILD_VERSION"

# Run post-deployment validation
echo "Running post-deployment validation..."
validate_deployment "$ENVIRONMENT" "$BUILD_VERSION"

# Run integration tests
if [[ "$RUN_INTEGRATION_TESTS" == true ]]; then
    echo "Running integration tests..."
    run_smoke_tests "$ENVIRONMENT" "$BUILD_VERSION"
fi

# Run performance tests
if [[ "$RUN_PERFORMANCE_TESTS" == true ]]; then
    echo "Running performance tests..."
    run_performance_tests "$ENVIRONMENT"
fi

echo "✅ Staging deployment completed successfully!"
echo "🔗 Application URL: http://$(get_env_config "$ENVIRONMENT" "load_balancer")"
echo "📊 Monitoring: $GRAFANA_ENDPOINT"
echo "📋 Logs: $CENTRALIZED_LOGGING_ENDPOINT"