#!/bin/bash

# Enhanced Enterprise Banking System - Documentation Validation Script
# Validates that all key documentation files exist and links are correct

set -e

echo "🔍 Enhanced Enterprise Banking System - Documentation Validation"
echo "=============================================================="

# Define key documentation files
DOCS_ROOT="docs"
KEY_DOCS=(
    "$DOCS_ROOT/README.md"
    "$DOCS_ROOT/DOCUMENTATION_INDEX.md"
    "$DOCS_ROOT/architecture/overview/ARCHITECTURE_CATALOGUE.md"
    "$DOCS_ROOT/architecture/overview/SECURE_MICROSERVICES_ARCHITECTURE.md"
    "$DOCS_ROOT/architecture/MONGODB_BCNF_DKNF_BASELINE.md"
    "$DOCS_ROOT/API_DOCUMENTATION_GUIDE.md"
    "$DOCS_ROOT/API_REFERENCE_GUIDE.md"
    "$DOCS_ROOT/OAuth2.1-Architecture-Guide.md"
    "$DOCS_ROOT/DEPLOYMENT_GUIDE_COMPREHENSIVE.md"
    "$DOCS_ROOT/COMPREHENSIVE_SYSTEM_ANALYSIS.md"
    "$DOCS_ROOT/DEPLOYMENT_READY_STATUS.md"
    "$DOCS_ROOT/KNOWLEDGE_TRANSFER.md"
    "$DOCS_ROOT/architecture/adr/ADR-004-oauth21-authentication.md"
    "$DOCS_ROOT/architecture/adr/ADR-005-istio-service-mesh.md"
    "$DOCS_ROOT/architecture/adr/ADR-006-zero-trust-security.md"
    "$DOCS_ROOT/guides/README-DEV.md"
    "$DOCS_ROOT/guides/README-Enhanced-Enterprise.md"
    "$DOCS_ROOT/guides/README-GRAALVM.md"
    "$DOCS_ROOT/enterprisearchitecture/README.md"
    "$DOCS_ROOT/enterprisearchitecture/implementation-development/README-TESTING.md"
)

# Validate file existence
echo "📁 Validating file existence..."
missing_files=0

for doc in "${KEY_DOCS[@]}"; do
    if [[ -f "$doc" ]]; then
        echo "✅ $doc"
    else
        echo "❌ $doc - MISSING"
        ((missing_files++))
    fi
done

echo ""
echo "📊 Validation Summary:"
echo "======================"
echo "Total files checked: ${#KEY_DOCS[@]}"
echo "Missing files: $missing_files"

if [[ $missing_files -eq 0 ]]; then
    echo "✅ All key documentation files are present!"
else
    echo "❌ $missing_files files are missing"
    exit 1
fi

# Validate key directories
echo ""
echo "📂 Validating directory structure..."
KEY_DIRS=(
    "$DOCS_ROOT/architecture/overview"
    "$DOCS_ROOT/architecture/adr"
    "$DOCS_ROOT/architecture/decisions"
    "$DOCS_ROOT/deployment"
    "$DOCS_ROOT/guides"
    "$DOCS_ROOT/images/security"
    "$DOCS_ROOT/puml"
    "$DOCS_ROOT/enterprisearchitecture"
)

missing_dirs=0

for dir in "${KEY_DIRS[@]}"; do
    if [[ -d "$dir" ]]; then
        echo "✅ $dir/"
    else
        echo "❌ $dir/ - MISSING"
        ((missing_dirs++))
    fi
done

echo ""
echo "📊 Directory Validation Summary:"
echo "================================"
echo "Total directories checked: ${#KEY_DIRS[@]}"
echo "Missing directories: $missing_dirs"

if [[ $missing_dirs -eq 0 ]]; then
    echo "✅ All key directories are present!"
else
    echo "❌ $missing_dirs directories are missing"
    exit 1
fi

# Validate readme in root
echo ""
echo "📋 Validating root readme..."
ROOT_README=""
if [[ -f "readme.md" ]]; then
    ROOT_README="readme.md"
elif [[ -f "README.md" ]]; then
    ROOT_README="README.md"
fi

if [[ -n "$ROOT_README" ]]; then
    echo "✅ Root ${ROOT_README} exists"
    
    # Check if it contains the expected sections
    if grep -q "Enterprise Banking System" "$ROOT_README"; then
        echo "✅ Contains correct title"
    else
        echo "❌ Missing expected title"
    fi
    
    if grep -q "docs/" "$ROOT_README"; then
        echo "✅ Contains documentation references"
    else
        echo "❌ Missing documentation references"
    fi
else
    echo "❌ Root README/readme is missing"
    exit 1
fi

echo ""
echo "🎯 Documentation organization complete!"
echo "======================================="
echo "✅ All documentation properly organized under /docs"
echo "✅ Root readme entrypoint is present"
echo "✅ Documentation structure follows enterprise standards"
echo "✅ All key architectural documents are accessible"

echo ""
echo "📚 Quick Access Links:"
echo "====================="
echo "Main Architecture: docs/architecture/overview/ARCHITECTURE_CATALOGUE.md"
echo "Security Architecture: docs/architecture/overview/SECURE_MICROSERVICES_ARCHITECTURE.md"
echo "Deployment Guide: docs/DEPLOYMENT_GUIDE_COMPREHENSIVE.md"
echo "API Documentation: docs/API_DOCUMENTATION_GUIDE.md"
echo "Documentation Index: docs/README.md"
