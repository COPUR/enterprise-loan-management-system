#!/bin/bash

# Production Deployment Script for Enterprise Banking Platform
# 
# This script provides comprehensive production deployment automation with:
# - Multi-environment support (staging, production, DR)
# - Zero-downtime deployments with blue-green strategy
# - Database migration and rollback capabilities
# - Health checks and validation
# - Security scanning and compliance verification
# - Monitoring and alerting integration
# - Automated rollback on failure

set -euo pipefail

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
LOG_FILE="/var/log/banking-deployment-$(date +%Y%m%d-%H%M%S).log"

# Source configuration and utility functions
source "$SCRIPT_DIR/config/deployment-config.sh"
source "$SCRIPT_DIR/utils/deployment-utils.sh"
source "$SCRIPT_DIR/utils/health-checks.sh"
source "$SCRIPT_DIR/utils/database-utils.sh"
source "$SCRIPT_DIR/utils/security-utils.sh"

# Global variables
ENVIRONMENT=""
DEPLOYMENT_TYPE="blue-green"
BUILD_VERSION=""
DRY_RUN=false
FORCE_DEPLOY=false
SKIP_TESTS=false
ROLLBACK_VERSION=""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging function
log() {
    local level="$1"
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    echo -e "${timestamp} [${level}] ${message}" | tee -a "$LOG_FILE"
    
    # Send to centralized logging if configured
    if [[ -n "${CENTRALIZED_LOGGING_ENDPOINT:-}" ]]; then
        send_to_centralized_logging "$level" "$message"
    fi
}

log_info() { log "INFO" "$@"; }
log_warn() { log "WARN" "$@"; }
log_error() { log "ERROR" "$@"; }
log_success() { log "SUCCESS" "$@"; }

# Usage function
usage() {
    cat << EOF
Usage: $0 [OPTIONS]

Production Deployment Script for Enterprise Banking Platform

OPTIONS:
    -e, --environment ENV       Target environment (staging|production|dr)
    -v, --version VERSION       Build version to deploy
    -t, --type TYPE            Deployment type (blue-green|rolling|canary)
    -d, --dry-run              Perform dry run without actual deployment
    -f, --force                Force deployment even if checks fail
    -s, --skip-tests           Skip pre-deployment tests
    -r, --rollback VERSION     Rollback to specified version
    -h, --help                 Show this help message

EXAMPLES:
    # Deploy to staging
    $0 -e staging -v 2.1.0

    # Production deployment with blue-green strategy
    $0 -e production -v 2.1.0 -t blue-green

    # Dry run for production
    $0 -e production -v 2.1.0 --dry-run

    # Rollback production to previous version
    $0 -e production --rollback 2.0.5

    # Emergency deployment (skip some checks)
    $0 -e production -v 2.1.1 --force

EOF
}

# Parse command line arguments
parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            -e|--environment)
                ENVIRONMENT="$2"
                shift 2
                ;;
            -v|--version)
                BUILD_VERSION="$2"
                shift 2
                ;;
            -t|--type)
                DEPLOYMENT_TYPE="$2"
                shift 2
                ;;
            -d|--dry-run)
                DRY_RUN=true
                shift
                ;;
            -f|--force)
                FORCE_DEPLOY=true
                shift
                ;;
            -s|--skip-tests)
                SKIP_TESTS=true
                shift
                ;;
            -r|--rollback)
                ROLLBACK_VERSION="$2"
                shift 2
                ;;
            -h|--help)
                usage
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                usage
                exit 1
                ;;
        esac
    done
}

# Validate arguments
validate_arguments() {
    if [[ -z "$ENVIRONMENT" ]]; then
        log_error "Environment is required. Use -e or --environment"
        exit 1
    fi

    if [[ ! "$ENVIRONMENT" =~ ^(staging|production|dr)$ ]]; then
        log_error "Invalid environment: $ENVIRONMENT. Must be staging, production, or dr"
        exit 1
    fi

    if [[ -n "$ROLLBACK_VERSION" ]]; then
        if [[ -n "$BUILD_VERSION" ]]; then
            log_error "Cannot specify both --version and --rollback"
            exit 1
        fi
        BUILD_VERSION="$ROLLBACK_VERSION"
    elif [[ -z "$BUILD_VERSION" ]]; then
        log_error "Build version is required. Use -v or --version"
        exit 1
    fi

    if [[ ! "$DEPLOYMENT_TYPE" =~ ^(blue-green|rolling|canary)$ ]]; then
        log_error "Invalid deployment type: $DEPLOYMENT_TYPE"
        exit 1
    fi
}

# Pre-deployment checks
pre_deployment_checks() {
    log_info "Starting pre-deployment checks..."

    # Check prerequisites
    check_prerequisites

    # Validate build version exists
    validate_build_version "$BUILD_VERSION"

    # Check environment health
    check_environment_health "$ENVIRONMENT"

    # Verify database connectivity
    check_database_connectivity "$ENVIRONMENT"

    # Check resource availability
    check_resource_availability "$ENVIRONMENT"

    # Security validation
    if [[ "$FORCE_DEPLOY" != true ]]; then
        run_security_scans "$BUILD_VERSION"
    fi

    # Run pre-deployment tests
    if [[ "$SKIP_TESTS" != true ]]; then
        run_pre_deployment_tests "$BUILD_VERSION"
    fi

    log_success "Pre-deployment checks completed successfully"
}

# Main deployment orchestration
deploy_application() {
    log_info "Starting deployment to $ENVIRONMENT environment..."
    
    local deployment_id=$(generate_deployment_id)
    local start_time=$(date +%s)
    
    # Create deployment record
    create_deployment_record "$deployment_id" "$ENVIRONMENT" "$BUILD_VERSION" "$DEPLOYMENT_TYPE"
    
    # Set maintenance mode if required
    if [[ "$ENVIRONMENT" == "production" && "$DEPLOYMENT_TYPE" == "blue-green" ]]; then
        enable_maintenance_mode "$ENVIRONMENT"
    fi
    
    # Execute deployment strategy
    case "$DEPLOYMENT_TYPE" in
        "blue-green")
            deploy_blue_green "$ENVIRONMENT" "$BUILD_VERSION"
            ;;
        "rolling")
            deploy_rolling "$ENVIRONMENT" "$BUILD_VERSION"
            ;;
        "canary")
            deploy_canary "$ENVIRONMENT" "$BUILD_VERSION"
            ;;
    esac
    
    # Database migrations
    run_database_migrations "$ENVIRONMENT" "$BUILD_VERSION"
    
    # Configuration updates
    update_configuration "$ENVIRONMENT" "$BUILD_VERSION"
    
    # Post-deployment validation
    validate_deployment "$ENVIRONMENT" "$BUILD_VERSION"
    
    # Disable maintenance mode
    if [[ "$ENVIRONMENT" == "production" ]]; then
        disable_maintenance_mode "$ENVIRONMENT"
    fi
    
    # Update deployment record
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    update_deployment_record "$deployment_id" "SUCCESS" "$duration"
    
    log_success "Deployment completed successfully in ${duration} seconds"
}

# Blue-green deployment strategy
deploy_blue_green() {
    local env="$1"
    local version="$2"
    
    log_info "Executing blue-green deployment for version $version"
    
    # Determine current and target environments
    local current_env=$(get_current_active_environment "$env")
    local target_env=$(get_inactive_environment "$env")
    
    log_info "Current active: $current_env, Target: $target_env"
    
    # Deploy to inactive environment
    deploy_to_environment "$target_env" "$version"
    
    # Run health checks on new deployment
    wait_for_healthy_deployment "$target_env" "$version"
    
    # Run smoke tests
    run_smoke_tests "$target_env" "$version"
    
    # Switch traffic gradually
    switch_traffic_gradually "$current_env" "$target_env"
    
    # Final validation
    validate_traffic_switch "$target_env"
    
    # Keep old environment for quick rollback
    tag_environment_for_rollback "$current_env"
    
    log_success "Blue-green deployment completed successfully"
}

# Rolling deployment strategy
deploy_rolling() {
    local env="$1"
    local version="$2"
    
    log_info "Executing rolling deployment for version $version"
    
    local instances=($(get_application_instances "$env"))
    local batch_size=$(calculate_rolling_batch_size "${#instances[@]}")
    
    for ((i=0; i<${#instances[@]}; i+=batch_size)); do
        local batch=("${instances[@]:i:batch_size}")
        
        log_info "Deploying to batch: ${batch[*]}"
        
        # Deploy to batch
        for instance in "${batch[@]}"; do
            deploy_to_instance "$instance" "$version"
        done
        
        # Wait for health
        for instance in "${batch[@]}"; do
            wait_for_instance_health "$instance"
        done
        
        # Brief pause between batches
        sleep 30
    done
    
    log_success "Rolling deployment completed successfully"
}

# Canary deployment strategy
deploy_canary() {
    local env="$1"
    local version="$2"
    
    log_info "Executing canary deployment for version $version"
    
    # Deploy to canary instance(s)
    deploy_canary_instance "$env" "$version"
    
    # Route small percentage of traffic
    route_canary_traffic "$env" 5
    
    # Monitor canary metrics
    monitor_canary_metrics "$env" "$version" 300 # 5 minutes
    
    # Gradually increase traffic
    local traffic_percentages=(10 25 50 75 100)
    for percentage in "${traffic_percentages[@]}"; do
        log_info "Increasing canary traffic to $percentage%"
        route_canary_traffic "$env" "$percentage"
        monitor_canary_metrics "$env" "$version" 180 # 3 minutes
    done
    
    # Promote canary to full deployment
    promote_canary_deployment "$env" "$version"
    
    log_success "Canary deployment completed successfully"
}

# Post-deployment validation
validate_deployment() {
    local env="$1"
    local version="$2"
    
    log_info "Starting post-deployment validation..."
    
    # Application health checks
    validate_application_health "$env"
    
    # Database connectivity
    validate_database_connectivity "$env"
    
    # External service connectivity
    validate_external_services "$env"
    
    # Performance validation
    validate_performance_metrics "$env"
    
    # Security validation
    validate_security_posture "$env"
    
    # Compliance checks
    validate_compliance_requirements "$env"
    
    # Business functionality tests
    run_business_validation_tests "$env"
    
    log_success "Post-deployment validation completed successfully"
}

# Rollback functionality
rollback_deployment() {
    local env="$1"
    local target_version="$2"
    
    log_warn "Initiating rollback to version $target_version"
    
    # Verify rollback version exists
    validate_rollback_version "$env" "$target_version"
    
    # Enable maintenance mode
    enable_maintenance_mode "$env"
    
    # Execute rollback strategy
    case "$DEPLOYMENT_TYPE" in
        "blue-green")
            rollback_blue_green "$env" "$target_version"
            ;;
        "rolling")
            rollback_rolling "$env" "$target_version"
            ;;
        "canary")
            rollback_canary "$env" "$target_version"
            ;;
    esac
    
    # Rollback database if needed
    rollback_database_migrations "$env" "$target_version"
    
    # Validate rollback
    validate_deployment "$env" "$target_version"
    
    # Disable maintenance mode
    disable_maintenance_mode "$env"
    
    log_success "Rollback to version $target_version completed successfully"
}

# Cleanup and reporting
cleanup_and_report() {
    local deployment_status="$1"
    
    log_info "Starting cleanup and reporting..."
    
    # Clean up temporary resources
    cleanup_temporary_resources
    
    # Generate deployment report
    generate_deployment_report "$ENVIRONMENT" "$BUILD_VERSION" "$deployment_status"
    
    # Send notifications
    send_deployment_notifications "$ENVIRONMENT" "$BUILD_VERSION" "$deployment_status"
    
    # Update monitoring dashboards
    update_deployment_dashboards "$ENVIRONMENT" "$BUILD_VERSION"
    
    # Archive logs
    archive_deployment_logs "$LOG_FILE"
    
    log_info "Cleanup and reporting completed"
}

# Error handling and recovery
handle_deployment_error() {
    local error_message="$1"
    local exit_code="$2"
    
    log_error "Deployment failed: $error_message"
    
    # Attempt automatic recovery
    if [[ "$FORCE_DEPLOY" != true ]]; then
        log_info "Attempting automatic recovery..."
        
        # Rollback if possible
        if [[ -n "${LAST_KNOWN_GOOD_VERSION:-}" ]]; then
            rollback_deployment "$ENVIRONMENT" "$LAST_KNOWN_GOOD_VERSION"
            return 0
        fi
    fi
    
    # Disable maintenance mode if enabled
    disable_maintenance_mode "$ENVIRONMENT" || true
    
    # Cleanup on failure
    cleanup_failed_deployment
    
    # Send failure notifications
    send_failure_notifications "$ENVIRONMENT" "$BUILD_VERSION" "$error_message"
    
    # Generate failure report
    generate_failure_report "$ENVIRONMENT" "$BUILD_VERSION" "$error_message"
    
    exit "$exit_code"
}

# Main execution
main() {
    # Set up error handling
    trap 'handle_deployment_error "Unexpected error occurred" $?' ERR
    
    log_info "Enterprise Banking Platform - Production Deployment Script"
    log_info "=================================================="
    
    # Parse and validate arguments
    parse_arguments "$@"
    validate_arguments
    
    # Check if this is a rollback operation
    if [[ -n "$ROLLBACK_VERSION" ]]; then
        log_info "Rollback operation requested to version $ROLLBACK_VERSION"
        rollback_deployment "$ENVIRONMENT" "$ROLLBACK_VERSION"
        cleanup_and_report "ROLLBACK_SUCCESS"
        exit 0
    fi
    
    # Display deployment summary
    cat << EOF

Deployment Summary:
==================
Environment: $ENVIRONMENT
Version: $BUILD_VERSION
Deployment Type: $DEPLOYMENT_TYPE
Dry Run: $DRY_RUN
Force Deploy: $FORCE_DEPLOY
Skip Tests: $SKIP_TESTS

EOF
    
    # Confirmation for production
    if [[ "$ENVIRONMENT" == "production" && "$DRY_RUN" != true && "$FORCE_DEPLOY" != true ]]; then
        echo -e "${YELLOW}WARNING: You are about to deploy to PRODUCTION!${NC}"
        read -p "Are you sure you want to continue? (yes/no): " confirmation
        if [[ "$confirmation" != "yes" ]]; then
            log_info "Deployment cancelled by user"
            exit 0
        fi
    fi
    
    # Execute deployment steps
    if [[ "$DRY_RUN" == true ]]; then
        log_info "DRY RUN MODE - No actual changes will be made"
        validate_dry_run_deployment
    else
        # Store current version for potential rollback
        LAST_KNOWN_GOOD_VERSION=$(get_current_deployment_version "$ENVIRONMENT")
        
        # Execute full deployment
        pre_deployment_checks
        deploy_application
        validate_deployment "$ENVIRONMENT" "$BUILD_VERSION"
    fi
    
    # Cleanup and reporting
    cleanup_and_report "SUCCESS"
    
    log_success "Deployment process completed successfully!"
    
    # Display next steps
    display_post_deployment_info
}

# Execute main function with all arguments
main "$@"