#!/bin/bash

# Enterprise Loan Management System - PlantUML Diagram Compilation Script
# This script compiles all PlantUML diagrams to multiple formats for documentation

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DIAGRAM_DIR="$SCRIPT_DIR"
OUTPUT_DIR="$SCRIPT_DIR/enterprise-governance/documentation/generated-diagrams"

echo "🏗️ Enterprise Loan Management System - Diagram Compilation"
echo "=================================================="

# Create output directory structure
mkdir -p "$OUTPUT_DIR/svg"
mkdir -p "$OUTPUT_DIR/png" 
mkdir -p "$OUTPUT_DIR/pdf"

# Check for PlantUML installation
check_plantuml() {
    if command -v plantuml &> /dev/null; then
        echo "✅ PlantUML found: $(which plantuml)"
        return 0
    elif command -v java &> /dev/null && [ -f "plantuml.jar" ]; then
        echo "✅ PlantUML JAR found with Java: $(java -version 2>&1 | head -n1)"
        return 1
    else
        echo "❌ PlantUML not found. Please install PlantUML or download plantuml.jar"
        echo ""
        echo "Installation options:"
        echo "1. npm install -g plantuml"
        echo "2. brew install plantuml (macOS)"
        echo "3. Download plantuml.jar from http://plantuml.com/download"
        exit 1
    fi
}

# Compile single diagram
compile_diagram() {
    local puml_file="$1"
    local filename=$(basename "$puml_file" .puml)
    
    echo "📊 Compiling: $filename"
    
    if command -v plantuml &> /dev/null; then
        # Using system PlantUML
        plantuml -tsvg -o "$OUTPUT_DIR/svg" "$puml_file"
        plantuml -tpng -o "$OUTPUT_DIR/png" "$puml_file"
        plantuml -tpdf -o "$OUTPUT_DIR/pdf" "$puml_file"
    else
        # Using Java JAR
        java -jar plantuml.jar -tsvg -o "$OUTPUT_DIR/svg" "$puml_file"
        java -jar plantuml.jar -tpng -o "$OUTPUT_DIR/png" "$puml_file"
        java -jar plantuml.jar -tpdf -o "$OUTPUT_DIR/pdf" "$puml_file"
    fi
    
    echo "   ✅ Generated: SVG, PNG, PDF"
}

# Main compilation process
main() {
    echo "🔍 Checking PlantUML installation..."
    check_plantuml
    
    echo ""
    echo "📁 Diagram directory: $DIAGRAM_DIR"
    echo "📁 Output directory: $OUTPUT_DIR"
    echo ""
    
    # Find and compile all .puml files
    if [ -d "$DIAGRAM_DIR" ]; then
        echo "🚀 Starting diagram compilation..."
        echo ""
        
        find "$DIAGRAM_DIR" -name "*.puml" -type f | sort | while read -r puml_file; do
            compile_diagram "$puml_file"
        done
        
        echo ""
        echo "📈 Compilation Summary:"
        echo "======================"
        echo "SVG files: $(find "$OUTPUT_DIR/svg" -name "*.svg" 2>/dev/null | wc -l)"
        echo "PNG files: $(find "$OUTPUT_DIR/png" -name "*.png" 2>/dev/null | wc -l)"
        echo "PDF files: $(find "$OUTPUT_DIR/pdf" -name "*.pdf" 2>/dev/null | wc -l)"
        echo ""
        echo "✅ All diagrams compiled successfully!"
        echo "📂 Output location: $OUTPUT_DIR"
        
    else
        echo "❌ Diagram directory not found: $DIAGRAM_DIR"
        exit 1
    fi
}

# List available diagrams
list_diagrams() {
    echo "Available PlantUML Diagrams:"
    echo "==============================="
    
    if [ -d "$DIAGRAM_DIR" ]; then
        find "$DIAGRAM_DIR" -name "*.puml" -type f | sort | while read -r puml_file; do
            filename=$(basename "$puml_file")
            title=$(head -n 5 "$puml_file" | grep -E "^@startuml" | sed 's/@startuml //' || echo "Untitled")
            echo "$filename - $title"
        done
    else
        echo "No diagram directory found"
    fi
}

# Help information
show_help() {
    echo "Enterprise Loan Management System - PlantUML Compilation"
    echo ""
    echo "Usage: $0 [command]"
    echo ""
    echo "Commands:"
    echo "  compile, build     Compile all PlantUML diagrams"
    echo "  list, ls          List available diagrams"
    echo "  help, -h, --help  Show this help message"
    echo ""
    echo "Output formats:"
    echo "  - SVG (vector graphics for web)"
    echo "  - PNG (raster graphics for presentations)"
    echo "  - PDF (print-ready documentation)"
    echo ""
    echo "Examples:"
    echo "  $0 compile        # Compile all diagrams"
    echo "  $0 list          # List available diagrams"
}

# Command parsing
case "${1:-compile}" in
    "compile"|"build"|"")
        main
        ;;
    "list"|"ls")
        list_diagrams
        ;;
    "help"|"-h"|"--help")
        show_help
        ;;
    *)
        echo "Unknown command: $1"
        echo ""
        show_help
        exit 1
        ;;
esac