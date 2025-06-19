#!/bin/bash

# =============================================================================
# Quick Hexagonal Architecture Validation Runner
# Enterprise Banking System - One-Click Architecture Validation
# =============================================================================

set -euo pipefail

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}🏦 Enterprise Banking System - Hexagonal Architecture Validation${NC}"
echo -e "${BLUE}=================================================================${NC}"
echo ""

# Check if we're in the right directory
if [ ! -f "gradlew" ]; then
    echo "❌ Error: gradlew not found. Please run from project root directory."
    exit 1
fi

# Run the comprehensive validation script
if [ -f "scripts/hexagonal-architecture-validation.sh" ]; then
    echo -e "${GREEN}🚀 Starting comprehensive hexagonal architecture validation...${NC}"
    echo ""
    ./scripts/hexagonal-architecture-validation.sh
else
    echo "❌ Error: hexagonal-architecture-validation.sh not found in scripts/ directory"
    exit 1
fi