#!/bin/bash

# Script to regenerate SVG files from updated PlantUML files
# This script identifies PlantUML files that have been updated and regenerates their corresponding SVG files

echo "🔄 Starting SVG regeneration from updated PlantUML files..."

# Function to generate SVG from PlantUML file
generate_svg() {
    local puml_file="$1"
    local output_dir="$2"
    
    echo "📊 Generating SVG from: $puml_file"
    
    # Create output directory if it doesn't exist
    mkdir -p "$output_dir"
    
    # Generate SVG using PlantUML
    if plantuml -tsvg -o "$output_dir" "$puml_file"; then
        echo "✅ Successfully generated SVG for: $puml_file"
        return 0
    else
        echo "❌ Failed to generate SVG for: $puml_file"
        return 1
    fi
}

# Base directory for docs
DOCS_DIR="/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/docs"

# Counter for tracking
TOTAL_FILES=0
SUCCESS_COUNT=0
FAILED_COUNT=0

echo "🔍 Scanning for PlantUML files..."

# Key PlantUML files that were updated and need SVG regeneration
declare -A PUML_FILES=(
    # Architecture diagrams
    ["$DOCS_DIR/architecture/hexagonal-architecture.puml"]="$DOCS_DIR/architecture/generated-diagrams"
    ["$DOCS_DIR/architecture/system-context.puml"]="$DOCS_DIR/architecture/generated-diagrams"
    ["$DOCS_DIR/architecture/component-diagram.puml"]="$DOCS_DIR/architecture/generated-diagrams"
    ["$DOCS_DIR/architecture/deployment-diagram.puml"]="$DOCS_DIR/architecture/generated-diagrams"
    ["$DOCS_DIR/architecture/security-architecture.puml"]="$DOCS_DIR/architecture/generated-diagrams"
    
    # Application architecture diagrams
    ["$DOCS_DIR/application-architecture/microservices/microservices-architecture-diagram.puml"]="$DOCS_DIR/application-architecture/microservices/generated-diagrams"
    ["$DOCS_DIR/application-architecture/microservices/hexagonal-architecture.puml"]="$DOCS_DIR/application-architecture/microservices/generated-diagrams"
    ["$DOCS_DIR/application-architecture/microservices/component-diagram.puml"]="$DOCS_DIR/application-architecture/microservices/generated-diagrams"
    ["$DOCS_DIR/application-architecture/sequence-diagrams/loan-creation-sequence.puml"]="$DOCS_DIR/application-architecture/sequence-diagrams/generated-diagrams"
    ["$DOCS_DIR/application-architecture/sequence-diagrams/oauth2-authentication-sequence.puml"]="$DOCS_DIR/application-architecture/sequence-diagrams/generated-diagrams"
    ["$DOCS_DIR/application-architecture/sequence-diagrams/payment-processing-sequence.puml"]="$DOCS_DIR/application-architecture/sequence-diagrams/generated-diagrams"
    
    # Security architecture diagrams
    ["$DOCS_DIR/security-architecture/security-models/fapi-security-architecture.puml"]="$DOCS_DIR/security-architecture/security-models/generated-diagrams"
    ["$DOCS_DIR/security-architecture/security-models/security-architecture-diagram.puml"]="$DOCS_DIR/security-architecture/security-models/generated-diagrams"
    
    # Business architecture diagrams
    ["$DOCS_DIR/business-architecture/domain-models/bounded-contexts.puml"]="$DOCS_DIR/business-architecture/domain-models/generated-diagrams"
    ["$DOCS_DIR/business-architecture/domain-models/domain-model.puml"]="$DOCS_DIR/business-architecture/domain-models/generated-diagrams"
    ["$DOCS_DIR/business-architecture/use-cases/banking-workflow.puml"]="$DOCS_DIR/business-architecture/use-cases/generated-diagrams"
    
    # Technology architecture diagrams
    ["$DOCS_DIR/technology-architecture/infrastructure-diagrams/aws-eks-architecture.puml"]="$DOCS_DIR/technology-architecture/infrastructure-diagrams/generated-diagrams"
    ["$DOCS_DIR/technology-architecture/infrastructure-diagrams/cache-performance-architecture.puml"]="$DOCS_DIR/technology-architecture/infrastructure-diagrams/generated-diagrams"
    ["$DOCS_DIR/technology-architecture/infrastructure-diagrams/oauth2-infrastructure-architecture.puml"]="$DOCS_DIR/technology-architecture/infrastructure-diagrams/generated-diagrams"
    ["$DOCS_DIR/technology-architecture/monitoring/monitoring-observability.puml"]="$DOCS_DIR/technology-architecture/monitoring/generated-diagrams"
    
    # Data architecture diagrams
    ["$DOCS_DIR/data-architecture/data-models/er-diagram.puml"]="$DOCS_DIR/data-architecture/data-models/generated-diagrams"
    ["$DOCS_DIR/data-architecture/data-models/database-isolation-diagram.puml"]="$DOCS_DIR/data-architecture/data-models/generated-diagrams"
    
    # Additional diagrams
    ["$DOCS_DIR/puml/oauth2-keycloak-architecture.puml"]="$DOCS_DIR/puml/generated-diagrams"
    ["$DOCS_DIR/puml/oauth2-sequence-flow.puml"]="$DOCS_DIR/puml/generated-diagrams"
    ["$DOCS_DIR/puml/istio-service-mesh-architecture.puml"]="$DOCS_DIR/puml/generated-diagrams"
    ["$DOCS_DIR/puml/secure-microservices-overview.puml"]="$DOCS_DIR/puml/generated-diagrams"
)

echo "📋 Found ${#PUML_FILES[@]} PlantUML files to process"

# Process each PlantUML file
for puml_file in "${!PUML_FILES[@]}"; do
    output_dir="${PUML_FILES[$puml_file]}"
    
    if [[ -f "$puml_file" ]]; then
        echo "🔄 Processing: $puml_file"
        TOTAL_FILES=$((TOTAL_FILES + 1))
        
        if generate_svg "$puml_file" "$output_dir"; then
            SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
        else
            FAILED_COUNT=$((FAILED_COUNT + 1))
        fi
    else
        echo "⚠️  PlantUML file not found: $puml_file"
    fi
done

echo ""
echo "📊 SVG Generation Summary:"
echo "  Total files processed: $TOTAL_FILES"
echo "  Successfully generated: $SUCCESS_COUNT"
echo "  Failed: $FAILED_COUNT"

# Create versioned copies for important diagrams
echo ""
echo "🏷️  Creating versioned copies of key diagrams..."

# Function to create versioned copy
create_versioned_copy() {
    local svg_file="$1"
    local version="v1.0.0"
    
    if [[ -f "$svg_file" ]]; then
        local dir=$(dirname "$svg_file")
        local filename=$(basename "$svg_file" .svg)
        local versioned_file="$dir/${filename}_${version}.svg"
        
        cp "$svg_file" "$versioned_file"
        echo "📋 Created versioned copy: $versioned_file"
    fi
}

# Key diagrams that need versioned copies
KEY_DIAGRAMS=(
    "$DOCS_DIR/architecture/generated-diagrams/Enterprise Loan Management System - Hexagonal Architecture.svg"
    "$DOCS_DIR/architecture/generated-diagrams/Enterprise Loan Management System - System Context.svg"
    "$DOCS_DIR/application-architecture/microservices/generated-diagrams/Enterprise Banking System - Istio Service Mesh Microservices Architecture.svg"
    "$DOCS_DIR/application-architecture/sequence-diagrams/generated-diagrams/Loan Creation Sequence.svg"
    "$DOCS_DIR/security-architecture/security-models/generated-diagrams/FAPI Security Architecture.svg"
    "$DOCS_DIR/technology-architecture/infrastructure-diagrams/generated-diagrams/AWS EKS Enterprise Loan Management System Architecture.svg"
)

for diagram in "${KEY_DIAGRAMS[@]}"; do
    create_versioned_copy "$diagram"
done

echo ""
echo "✅ SVG regeneration completed!"
echo "🎯 Next steps:"
echo "  1. Review generated SVG files for accuracy"
echo "  2. Update documentation references if needed"
echo "  3. Commit updated SVG files to version control"
echo "  4. Verify diagrams display correctly in documentation"

echo ""
echo "🔗 Generated diagrams locations:"
echo "  - Architecture: $DOCS_DIR/architecture/generated-diagrams/"
echo "  - Application: $DOCS_DIR/application-architecture/*/generated-diagrams/"
echo "  - Security: $DOCS_DIR/security-architecture/security-models/generated-diagrams/"
echo "  - Infrastructure: $DOCS_DIR/technology-architecture/infrastructure-diagrams/generated-diagrams/"