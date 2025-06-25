#!/bin/bash

# Generate Versioned PlantUML Diagrams - Enterprise Banking System
# Creates v1.0.0 versioned SVG diagrams with proper directory structure

set -euo pipefail

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${BLUE}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

# Check if PlantUML is available
if ! command -v plantuml &> /dev/null; then
    log_error "PlantUML is not installed. Please install PlantUML first."
    exit 1
fi

log_info "Starting versioned diagram generation for Enterprise Banking System"

# Find all PlantUML files
GENERATED_COUNT=0
FAILED_COUNT=0

# Process each PlantUML file
while IFS= read -r -d '' puml_file; do
    log_info "Processing: $puml_file"
    
    # Get directory path
    dir_path=$(dirname "$puml_file")
    generated_dir="$dir_path/generated-diagrams"
    
    # Create generated-diagrams directory if it doesn't exist
    mkdir -p "$generated_dir"
    
    # Generate SVG
    if plantuml -tsvg "$puml_file" 2>/dev/null; then
        # Find all SVG files in the directory that are newer than the PUML file
        while IFS= read -r -d '' svg_file; do
            # Skip if already in generated-diagrams directory
            if [[ "$svg_file" == *"/generated-diagrams/"* ]]; then
                continue
            fi
            
            # Check if this SVG was just created (newer than the PUML file)
            if [[ "$svg_file" -nt "$puml_file" ]]; then
                # Extract filename without extension
                filename=$(basename "$svg_file" .svg)
                
                # Check if already versioned
                if [[ "$filename" == *"_v1.0.0" ]]; then
                    # Move to generated-diagrams if not already there
                    mv "$svg_file" "$generated_dir/"
                    log_success "Moved already versioned: $filename"
                else
                    # Add version suffix and move
                    versioned_filename="${filename}_v1.0.0.svg"
                    target_path="$generated_dir/$versioned_filename"
                    
                    mv "$svg_file" "$target_path"
                    log_success "Generated versioned: $versioned_filename"
                fi
                
                ((GENERATED_COUNT++))
            fi
        done < <(find "$dir_path" -maxdepth 1 -name "*.svg" -type f -print0 2>/dev/null)
    else
        log_error "Failed to generate SVG for: $puml_file"
        ((FAILED_COUNT++))
    fi
done < <(find docs -name "*.puml" -type f -print0)

# Generate summary report
log_info "==================================="
log_info "Diagram Generation Summary"
log_info "==================================="
log_success "Successfully generated: $GENERATED_COUNT diagrams"
if [[ $FAILED_COUNT -gt 0 ]]; then
    log_error "Failed to generate: $FAILED_COUNT diagrams"
fi

# List all generated versioned diagrams
log_info "Generated versioned diagrams:"
while IFS= read -r -d '' diagram; do
    log_success "  ✓ $diagram"
done < <(find docs -name "*_v1.0.0.svg" -type f -print0 | sort -z)

log_info "Diagram generation complete!"