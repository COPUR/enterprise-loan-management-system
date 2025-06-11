#!/bin/bash
# Enterprise Loan Management System - Health Check Script

# Set timeout for health checks
TIMEOUT=10

# Health check endpoint
HEALTH_URL="http://localhost:5000/actuator/health"

# Check if application is responding
if curl -f -s --max-time $TIMEOUT "$HEALTH_URL" > /dev/null 2>&1; then
    # Additional banking-specific health checks
    READINESS_URL="http://localhost:5000/actuator/health/readiness"
    
    if curl -f -s --max-time $TIMEOUT "$READINESS_URL" > /dev/null 2>&1; then
        echo "Banking system healthy and ready"
        exit 0
    else
        echo "Banking system not ready"
        exit 1
    fi
else
    echo "Banking system health check failed"
    exit 1
fi