#!/bin/bash

# Script to update diagram references in markdown files to point to the latest generated SVG files
# This script updates references to ensure they point to the newly generated diagrams

echo "🔄 Updating diagram references in markdown files..."

# Base directory for docs
DOCS_DIR="/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/docs"

# Function to update diagram references in a file
update_diagram_references() {
    local file="$1"
    
    echo "📝 Updating diagram references in: $file"
    
    # Backup the original file
    cp "$file" "$file.bak"
    
    # Update references to point to the latest generated diagrams
    # Architecture diagrams
    sed -i '' 's|Enterprise Loan Management System - Hexagonal Architecture\.svg|Enterprise Loan Management System - Hexagonal Architecture (Java 21).svg|g' "$file"
    sed -i '' 's|Enterprise Loan Management System - System Context\.svg|Enterprise Loan Management System - System Context (Java 21).svg|g' "$file"
    
    # Microservices diagrams
    sed -i '' 's|Enterprise Banking System - Istio Service Mesh Microservices Architecture\.svg|Enterprise Loan Management - Microservices Architecture.svg|g' "$file"
    sed -i '' 's|Enterprise Loan Management - Microservices Architecture\.svg|Enterprise Loan Management - Microservices Architecture.svg|g' "$file"
    
    # Sequence diagrams
    sed -i '' 's|Loan Creation Sequence\.svg|Loan Creation Sequence (Java 21 + FAPI 2.0).svg|g' "$file"
    
    # Security diagrams
    sed -i '' 's|FAPI Security Architecture\.svg|FAPI 2.0 Security Architecture (Java 21).svg|g' "$file"
    
    # Infrastructure diagrams
    sed -i '' 's|AWS EKS Enterprise Loan Management System Architecture\.svg|AWS EKS Enterprise Loan Management System Architecture (Java 21).svg|g' "$file"
    
    echo "✅ Updated diagram references in: $file"
}

# Function to add diagram references to architecture overview
add_diagram_references_to_architecture_overview() {
    local file="$DOCS_DIR/ARCHITECTURE_OVERVIEW.md"
    
    echo "📊 Adding diagram references to architecture overview..."
    
    # Add diagram references after the System Overview section
    if ! grep -q "## Architecture Diagrams" "$file"; then
        # Find the line number after "## Architecture Principles"
        local line_num=$(grep -n "## Architecture Principles" "$file" | cut -d: -f1)
        if [[ -n "$line_num" ]]; then
            # Insert architecture diagrams section before Architecture Principles
            sed -i '' "${line_num}i\\
\\
## Architecture Diagrams\\
\\
### System Context\\
![System Context](architecture/generated-diagrams/Enterprise%20Loan%20Management%20System%20-%20System%20Context%20(Java%2021).svg)\\
\\
### Hexagonal Architecture\\
![Hexagonal Architecture](architecture/generated-diagrams/Enterprise%20Loan%20Management%20System%20-%20Hexagonal%20Architecture%20(Java%2021).svg)\\
\\
### Microservices Architecture\\
![Microservices Architecture](application-architecture/microservices/generated-diagrams/Enterprise%20Loan%20Management%20-%20Microservices%20Architecture.svg)\\
\\
### Security Architecture\\
![Security Architecture](security-architecture/security-models/generated-diagrams/FAPI%202.0%20Security%20Architecture%20(Java%2021).svg)\\
\\
" "$file"
        fi
    fi
}

# Function to update business architecture README
update_business_architecture_readme() {
    local file="$DOCS_DIR/business-architecture/README.md"
    
    echo "📋 Updating business architecture README..."
    
    # Update the existing references to use the correct paths
    sed -i '' 's|domain-models/generated-diagrams/Domain%20Model_v1.0.0.svg|domain-models/generated-diagrams/Domain%20Model.svg|g' "$file"
    sed -i '' 's|domain-models/generated-diagrams/Bounded%20Contexts_v1.0.0.svg|domain-models/generated-diagrams/Bounded%20Contexts.svg|g' "$file"
    sed -i '' 's|use-cases/generated-diagrams/Banking%20Workflow_v1.0.0.svg|use-cases/generated-diagrams/Banking%20Workflow.svg|g' "$file"
    
    echo "✅ Updated business architecture README"
}

# Function to update main README
update_main_readme() {
    local file="readme.md"
    
    echo "📖 Updating main README..."
    
    # Add architecture diagrams section if it doesn't exist
    if ! grep -q "## Architecture Diagrams" "$file"; then
        # Find the line number after the architecture section
        local line_num=$(grep -n "## 🏗️ Architecture" "$file" | cut -d: -f1)
        if [[ -n "$line_num" ]]; then
            # Calculate the line number to insert after the architecture description
            local insert_line=$((line_num + 30))  # Approximate position after architecture description
            
            # Insert architecture diagrams section
            sed -i '' "${insert_line}i\\
\\
### Architecture Diagrams\\
\\
#### System Context\\
![System Context](docs/architecture/generated-diagrams/Enterprise%20Loan%20Management%20System%20-%20System%20Context%20(Java%2021).svg)\\
\\
#### Hexagonal Architecture\\
![Hexagonal Architecture](docs/architecture/generated-diagrams/Enterprise%20Loan%20Management%20System%20-%20Hexagonal%20Architecture%20(Java%2021).svg)\\
\\
#### Microservices Architecture\\
![Microservices](docs/application-architecture/microservices/generated-diagrams/Enterprise%20Loan%20Management%20-%20Microservices%20Architecture.svg)\\
\\
" "$file"
        fi
    fi
    
    echo "✅ Updated main README"
}

# Function to create a diagram index
create_diagram_index() {
    local file="$DOCS_DIR/DIAGRAM_REFERENCE_INDEX.md"
    
    echo "📊 Creating comprehensive diagram reference index..."
    
    cat > "$file" << 'EOF'
# Diagram Reference Index

This document provides a comprehensive index of all architecture diagrams in the Enterprise Loan Management System documentation.

## Architecture Diagrams

### System Context
- **File**: `docs/architecture/generated-diagrams/Enterprise Loan Management System - System Context (Java 21).svg`
- **Source**: `docs/architecture/system-context.puml`
- **Description**: High-level system context showing external systems and user interactions
- **Last Updated**: January 9, 2025

### Hexagonal Architecture
- **File**: `docs/architecture/generated-diagrams/Enterprise Loan Management System - Hexagonal Architecture (Java 21).svg`
- **Source**: `docs/architecture/hexagonal-architecture.puml`
- **Description**: Detailed hexagonal architecture with ports, adapters, and domain model
- **Last Updated**: January 9, 2025

### Component Diagram
- **File**: `docs/architecture/generated-diagrams/Enterprise Loan Management System - Component Diagram.svg`
- **Source**: `docs/architecture/component-diagram.puml`
- **Description**: System components and their relationships

### Deployment Diagram
- **File**: `docs/architecture/generated-diagrams/Enterprise Loan Management System - Deployment Diagram.svg`
- **Source**: `docs/architecture/deployment-diagram.puml`
- **Description**: System deployment architecture

## Application Architecture Diagrams

### Microservices Architecture
- **File**: `docs/application-architecture/microservices/generated-diagrams/Enterprise Loan Management - Microservices Architecture.svg`
- **Source**: `docs/application-architecture/microservices/microservices-architecture-diagram.puml`
- **Description**: Java 21 microservices with Istio service mesh
- **Last Updated**: January 9, 2025

### Sequence Diagrams

#### Loan Creation Sequence
- **File**: `docs/application-architecture/sequence-diagrams/generated-diagrams/Loan Creation Sequence (Java 21 + FAPI 2.0).svg`
- **Source**: `docs/application-architecture/sequence-diagrams/loan-creation-sequence.puml`
- **Description**: Loan creation process with FAPI 2.0 and Zero Trust security
- **Last Updated**: January 9, 2025

#### OAuth2 Authentication Sequence
- **File**: `docs/application-architecture/sequence-diagrams/generated-diagrams/OAuth2.1 Authentication & Authorization Sequence.svg`
- **Source**: `docs/application-architecture/sequence-diagrams/oauth2-authentication-sequence.puml`
- **Description**: OAuth 2.1 authentication flow with FAPI 2.0 compliance

#### Payment Processing Sequence
- **File**: `docs/application-architecture/sequence-diagrams/generated-diagrams/Payment Processing Sequence.svg`
- **Source**: `docs/application-architecture/sequence-diagrams/payment-processing-sequence.puml`
- **Description**: Payment processing workflow with fraud detection

## Security Architecture Diagrams

### FAPI 2.0 Security Architecture
- **File**: `docs/security-architecture/security-models/generated-diagrams/FAPI 2.0 Security Architecture (Java 21).svg`
- **Source**: `docs/security-architecture/security-models/fapi-security-architecture.puml`
- **Description**: FAPI 2.0 security framework with Zero Trust architecture
- **Last Updated**: January 9, 2025

### OWASP Security Architecture
- **File**: `docs/security-architecture/security-models/generated-diagrams/OWASP Top 10 Security Architecture.svg`
- **Source**: `docs/security-architecture/security-models/security-architecture-diagram.puml`
- **Description**: OWASP Top 10 security implementation

## Business Architecture Diagrams

### Domain Model
- **File**: `docs/business-architecture/domain-models/generated-diagrams/Domain Model.svg`
- **Source**: `docs/business-architecture/domain-models/domain-model.puml`
- **Description**: Core business domain model with entities and relationships

### Bounded Contexts
- **File**: `docs/business-architecture/domain-models/generated-diagrams/Bounded Contexts.svg`
- **Source**: `docs/business-architecture/domain-models/bounded-contexts.puml`
- **Description**: Domain-driven design bounded contexts

### Banking Workflow
- **File**: `docs/business-architecture/use-cases/generated-diagrams/Banking Workflow.svg`
- **Source**: `docs/business-architecture/use-cases/banking-workflow.puml`
- **Description**: Core banking process workflows

## Technology Architecture Diagrams

### AWS EKS Architecture
- **File**: `docs/technology-architecture/infrastructure-diagrams/generated-diagrams/AWS EKS Enterprise Loan Management System Architecture (Java 21).svg`
- **Source**: `docs/technology-architecture/infrastructure-diagrams/aws-eks-architecture.puml`
- **Description**: AWS EKS deployment architecture with Java 21 optimization
- **Last Updated**: January 9, 2025

### Cache Performance Architecture
- **File**: `docs/technology-architecture/infrastructure-diagrams/generated-diagrams/Multi-Level Cache Architecture - Enterprise Loan Management System.svg`
- **Source**: `docs/technology-architecture/infrastructure-diagrams/cache-performance-architecture.puml`
- **Description**: Multi-level caching architecture for performance optimization

### OAuth2.1 Infrastructure
- **File**: `docs/technology-architecture/infrastructure-diagrams/generated-diagrams/OAuth2.1 Infrastructure Architecture - Banking System.svg`
- **Source**: `docs/technology-architecture/infrastructure-diagrams/oauth2-infrastructure-architecture.puml`
- **Description**: OAuth 2.1 infrastructure setup with Keycloak

### Monitoring & Observability
- **File**: `docs/technology-architecture/monitoring/generated-diagrams/Monitoring & Observability - Enterprise Loan Management System.svg`
- **Source**: `docs/technology-architecture/monitoring/monitoring-observability.puml`
- **Description**: Comprehensive monitoring and observability architecture

## Data Architecture Diagrams

### Entity Relationship Diagram
- **File**: `docs/data-architecture/data-models/generated-diagrams/Entity Relationship Diagram.svg`
- **Source**: `docs/data-architecture/data-models/er-diagram.puml`
- **Description**: Database entity relationships and schema design

### Database Isolation Architecture
- **File**: `docs/data-architecture/data-models/generated-diagrams/Database Isolation Architecture.svg`
- **Source**: `docs/data-architecture/data-models/database-isolation-diagram.puml`
- **Description**: Database isolation and multi-tenancy architecture

## Usage Notes

### Viewing Diagrams
- All SVG files can be viewed directly in web browsers
- GitHub automatically renders SVG files in markdown
- For best quality, use the latest versioned files

### Updating Diagrams
1. Edit the source PlantUML (.puml) file
2. Generate new SVG using: `plantuml -tsvg -o target_directory source_file.puml`
3. Update references in markdown files if needed
4. Commit both PlantUML and SVG files

### Diagram Conventions
- All diagrams use consistent color schemes
- Titles include technology versions (Java 21, Spring Boot 3.4.3)
- Security diagrams emphasize FAPI 2.0 and Zero Trust
- Architecture diagrams show current implementation

---

**Last Updated**: January 9, 2025  
**Status**: Current with Java 21 implementation  
**Maintenance**: Update when architecture changes
EOF

    echo "✅ Created comprehensive diagram reference index"
}

# Main execution
echo "🔄 Starting diagram reference updates..."

# Update key documentation files
update_main_readme
add_diagram_references_to_architecture_overview
update_business_architecture_readme

# Update diagram references in key files
FILES_TO_UPDATE=(
    "$DOCS_DIR/ARCHITECTURE_OVERVIEW.md"
    "$DOCS_DIR/business-architecture/README.md"
    "$DOCS_DIR/enterprise-governance/documentation/DIAGRAM_INDEX.md"
    "$DOCS_DIR/enterprise-governance/documentation/README.md"
    "$DOCS_DIR/data-architecture/README.md"
    "$DOCS_DIR/infrastructure-architecture/Infrastructure-Architecture-Guide.md"
)

for file in "${FILES_TO_UPDATE[@]}"; do
    if [[ -f "$file" ]]; then
        update_diagram_references "$file"
    else
        echo "⚠️  File not found: $file"
    fi
done

# Create comprehensive diagram index
create_diagram_index

echo ""
echo "✅ Diagram reference updates completed!"
echo "📋 Summary:"
echo "  - Updated main README with architecture diagrams"
echo "  - Enhanced architecture overview with diagram references"
echo "  - Updated business architecture documentation"
echo "  - Created comprehensive diagram reference index"
echo "  - Updated paths to use latest generated SVG files"

echo ""
echo "🎯 Next steps:"
echo "  1. Review updated documentation files"
echo "  2. Test diagram links in GitHub/markdown viewers"
echo "  3. Commit updated documentation"
echo "  4. Verify all diagrams display correctly"

echo ""
echo "📁 Key files updated:"
echo "  - readme.md (main project README)"
echo "  - docs/ARCHITECTURE_OVERVIEW.md"
echo "  - docs/business-architecture/README.md"
echo "  - docs/DIAGRAM_REFERENCE_INDEX.md (new)"