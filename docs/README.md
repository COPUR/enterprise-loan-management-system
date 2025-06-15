# Enterprise Loan Management System Documentation

Welcome to the comprehensive documentation for the Enterprise Loan Management System. This system is built using Domain-Driven Design (DDD) principles and Hexagonal Architecture patterns to provide a robust, scalable, and maintainable solution for managing enterprise loans.

## 📚 Documentation Structure

### Architecture Documentation
- [Domain-Driven Design ADR](architecture/adr/ADR-001-domain-driven-design.md)
- [Hexagonal Architecture ADR](architecture/adr/ADR-002-hexagonal-architecture.md)
- [SAGA Pattern ADR](architecture/adr/ADR-003-saga-pattern.md)

### Architecture Diagrams
- [Domain Model Diagram](architecture/diagrams/domain-model.puml)
- [Hexagonal Architecture Diagram](architecture/diagrams/hexagonal-architecture.puml)
- [Bounded Contexts Diagram](architecture/diagrams/bounded-contexts.puml)
- [Entity-Relationship Diagram](architecture/diagrams/er-diagram.puml)
- [Component Diagram](architecture/diagrams/component-diagram.puml)
- [Loan Creation Sequence Diagram](architecture/diagrams/loan-creation-sequence.puml)
- [Payment Processing Sequence Diagram](architecture/diagrams/payment-processing-sequence.puml)

### API Documentation
- [OpenAPI Specification](api/openapi.yml)

### Deployment Documentation
- [Local Development Setup](deployment/local-development.md)
- [Kubernetes Deployment](deployment/kubernetes/)

## 🏗️ System Overview

### Bounded Contexts

The system is organized into three main bounded contexts:

1. **Customer Management** - Handles customer data and credit limit management
2. **Loan Origination** - Manages loan creation and installment scheduling
3. **Payment Processing** - Handles payment processing and calculations

### Key Features

- **Loan Creation** with business rule validation
- **Payment Processing** with early/late payment calculations
- **Credit Management** with automatic limit updates
- **Role-Based Access Control** (ADMIN and CUSTOMER roles)
- **Event-Driven Architecture** with SAGA pattern implementation
- **Real-time notifications** through domain events

## 🚀 Getting Started

### Prerequisites

- Java 21+
- Docker & Docker Compose
- Gradle 8.0+

### Quick Start

1. **Start Infrastructure Services**
   ```bash
   docker-compose up -d
