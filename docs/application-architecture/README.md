# Application Architecture

This section contains microservices design, API specifications, integration patterns, and sequence diagrams.

## Microservices
- [Component Diagram](microservices/component-diagram.puml) - System component relationships
- [Microservices Architecture](microservices/microservices-architecture-diagram.puml) - Service architecture design
- [Hexagonal Architecture](microservices/hexagonal-architecture.puml) - Ports and adapters pattern
- [Simple Architecture](microservices/simple-architecture.puml) - Simplified system view
- [Gradle 9 Microservices Upgrade Report](microservices/GRADLE_9_MICROSERVICES_UPGRADE_REPORT.md) - Modernization documentation

## API Specifications
- [OpenFinance API Documentation](api-specifications/OPENFINANCE_API_DOCUMENTATION.md) - RESTful API specifications

## Integration Patterns
- [SAGA Workflow Diagram](integration-patterns/saga-workflow-diagram.puml) - Distributed transaction patterns

## Sequence Diagrams

### Loan Creation Sequence
- **File**: [Loan Creation Sequence](sequence-diagrams/loan-creation-sequence.puml)
- **Purpose**: Complete loan origination process with business rule validation and SAGA coordination
- **Key Features**:
  - Credit eligibility validation with customer assessment
  - Business rule enforcement (amount: $1,000-$500,000, rate: 0.1%-0.5%, installments: 6,9,12,24 months)
  - SAGA pattern implementation for distributed transaction consistency
  - Automatic credit reservation with compensation logic
  - Event-driven communication with domain events
  - Comprehensive error handling and rollback mechanisms

### Payment Processing Sequence
- **File**: [Payment Processing Sequence](sequence-diagrams/payment-processing-sequence.puml)
- **Purpose**: End-to-end payment processing with calculation logic and loan completion
- **Key Features**:
  - Payment validation and loan status verification
  - Smart payment calculation with early/late payment adjustments
  - Installment ordering (earliest unpaid installments first)
  - No partial payment enforcement (full installment amounts only)
  - Automatic loan completion detection and credit release
  - Real-time payment state management (INITIATED → PROCESSING → COMPLETED)

### Additional Use Case Sequences

#### AI Risk Assessment Flow
- Real-time customer risk analysis using OpenAI GPT-4o
- Portfolio risk scoring with machine learning insights
- Automated compliance monitoring and regulatory guidance
- Natural language query processing for banking operations

#### Natural Language Banking Interface
- Conversational loan application processing
- Intent recognition and entity extraction
- AI-powered eligibility assessment and recommendations
- Multi-language support for international banking

## Architecture Principles
- Domain-Driven Design (DDD)
- Hexagonal Architecture
- Microservices Pattern
- Event-Driven Architecture
- SAGA Pattern Implementation