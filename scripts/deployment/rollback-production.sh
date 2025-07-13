#!/bin/bash

# Production Rollback Script for Enterprise Banking Platform
# 
# Emergency rollback script for production environment

set -euo pipefail

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Source utilities
source "$SCRIPT_DIR/config/deployment-config.sh"
source "$SCRIPT_DIR/utils/deployment-utils.sh"
source "$SCRIPT_DIR/utils/health-checks.sh"
source "$SCRIPT_DIR/utils/database-utils.sh"

# Variables
ENVIRONMENT="production"
TARGET_VERSION=""
FORCE_ROLLBACK=false
SKIP_VALIDATION=false

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Usage
usage() {
    cat << EOF
Usage: $0 [OPTIONS]

Emergency Production Rollback Script

OPTIONS:
    -v, --version VERSION       Target version to rollback to
    -f, --force                 Force rollback without confirmation
    -s, --skip-validation       Skip post-rollback validation
    -h, --help                  Show this help message

EXAMPLES:
    # Rollback to specific version
    $0 -v 2.0.5

    # Emergency rollback (force mode)
    $0 -v 2.0.5 --force

    # Quick rollback (skip validation)
    $0 -v 2.0.5 --skip-validation

EOF
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -v|--version)
            TARGET_VERSION="$2"
            shift 2
            ;;
        -f|--force)
            FORCE_ROLLBACK=true
            shift
            ;;
        -s|--skip-validation)
            SKIP_VALIDATION=true
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            usage
            exit 1
            ;;
    esac
done

# Validate arguments
if [[ -z "$TARGET_VERSION" ]]; then
    echo -e "${RED}Error: Target version is required. Use -v or --version${NC}"
    exit 1
fi

# Get current version
CURRENT_VERSION=$(get_current_deployment_version "$ENVIRONMENT")

echo -e "${YELLOW}=== EMERGENCY PRODUCTION ROLLBACK ===${NC}"
echo -e "${YELLOW}WARNING: This will rollback the production environment!${NC}"
echo
echo "Current Version: $CURRENT_VERSION"
echo "Target Version: $TARGET_VERSION"
echo "Environment: $ENVIRONMENT"
echo "Force Mode: $FORCE_ROLLBACK"
echo "Skip Validation: $SKIP_VALIDATION"
echo

# Confirmation
if [[ "$FORCE_ROLLBACK" != true ]]; then
    echo -e "${RED}⚠️  CRITICAL: You are about to rollback PRODUCTION!${NC}"
    echo -e "${RED}This action cannot be easily undone.${NC}"
    echo
    read -p "Type 'ROLLBACK' to confirm: " confirmation
    if [[ "$confirmation" != "ROLLBACK" ]]; then
        echo "Rollback cancelled."
        exit 0
    fi
fi

# Log rollback initiation
echo "$(date): Initiating production rollback from $CURRENT_VERSION to $TARGET_VERSION" | tee -a /var/log/banking-rollback.log

# Pre-rollback checks
echo "Running pre-rollback checks..."

# Verify target version exists
if ! validate_build_version "$TARGET_VERSION"; then
    echo -e "${RED}Error: Target version $TARGET_VERSION not found${NC}"
    exit 1
fi

# Check if target version is older than current
if ! version_is_older "$TARGET_VERSION" "$CURRENT_VERSION"; then
    echo -e "${YELLOW}Warning: Target version $TARGET_VERSION is not older than current version $CURRENT_VERSION${NC}"
    if [[ "$FORCE_ROLLBACK" != true ]]; then
        read -p "Continue anyway? (yes/no): " continue_rollback
        if [[ "$continue_rollback" != "yes" ]]; then
            echo "Rollback cancelled."
            exit 0
        fi
    fi
fi

# Enable maintenance mode
echo "Enabling maintenance mode..."
enable_maintenance_mode "$ENVIRONMENT"

# Create pre-rollback backup
echo "Creating pre-rollback backup..."
create_database_backup "$ENVIRONMENT" "pre-rollback-$TARGET_VERSION"

# Execute rollback
echo "Executing rollback to version $TARGET_VERSION..."

# Application rollback
deploy_to_environment "$ENVIRONMENT" "$TARGET_VERSION"

# Database rollback
echo "Rolling back database migrations..."
rollback_database_migrations "$ENVIRONMENT" "$TARGET_VERSION"

# Wait for rollback to complete
echo "Waiting for rollback to complete..."
wait_for_healthy_deployment "$ENVIRONMENT" "$TARGET_VERSION"

# Post-rollback validation
if [[ "$SKIP_VALIDATION" != true ]]; then
    echo "Running post-rollback validation..."
    validate_deployment "$ENVIRONMENT" "$TARGET_VERSION"
    
    # Run critical tests
    echo "Running critical functionality tests..."
    run_smoke_tests "$ENVIRONMENT" "$TARGET_VERSION"
fi

# Disable maintenance mode
echo "Disabling maintenance mode..."
disable_maintenance_mode "$ENVIRONMENT"

# Send notifications
echo "Sending rollback notifications..."
send_rollback_notification "$ENVIRONMENT" "$CURRENT_VERSION" "$TARGET_VERSION"

echo -e "${GREEN}✅ Production rollback completed successfully!${NC}"
echo
echo "Rollback Summary:"
echo "=================="
echo "Environment: $ENVIRONMENT"
echo "Previous Version: $CURRENT_VERSION"
echo "Current Version: $TARGET_VERSION"
echo "Rollback Time: $(date)"
echo
echo "Next Steps:"
echo "1. Monitor application health and metrics"
echo "2. Verify business functionality"
echo "3. Investigate and fix issues in $CURRENT_VERSION"
echo "4. Plan forward deployment with fixes"

# Helper functions
get_current_deployment_version() {
    local env="$1"
    local namespace=$(get_namespace "$env")
    
    kubectl get deployment "$APP_NAME" -n "$namespace" -o jsonpath='{.spec.template.spec.containers[0].image}' 2>/dev/null | \
        sed "s/.*:${IMAGE_TAG_PREFIX}//" || echo "unknown"
}

version_is_older() {
    local version1="$1"
    local version2="$2"
    
    # Simple version comparison (would use proper semver in production)
    [[ "$version1" < "$version2" ]]
}

send_rollback_notification() {
    local env="$1"
    local from_version="$2"
    local to_version="$3"
    
    local message="🚨 PRODUCTION ROLLBACK COMPLETED
Environment: $env
From Version: $from_version
To Version: $to_version
Time: $(date)
Operator: $(whoami)"
    
    # Send notifications
    send_slack_notification "$message"
    send_email_notification "$message"
    
    # Log to audit
    echo "$(date): Production rollback notification sent" | tee -a /var/log/banking-rollback.log
}