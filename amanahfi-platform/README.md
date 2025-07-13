# AmanahFi Platform - Islamic Finance Platform

## Overview

AmanahFi is a modern Islamic finance platform built for the UAE and MENAT region, designed to provide Sharia-compliant banking services with focus on Murabaha-as-a-service using UAE Digital Dirham (CBDC).

## Test-Driven Development (TDD) Implementation

This project follows strict TDD principles with tests written before implementation:

### ✅ Completed Features (TDD)

#### 1. Shared Kernel - Money Value Object
- **18 Tests Passing** - Complete Islamic finance-compliant monetary operations
- **Key Features:**
  - Precise decimal calculations for Sharia compliance
  - No negative amounts (Islamic finance principle)
  - MENAT currency support (AED, USD, EUR, SAR, QAR, KWD, BHD)
  - Profit-sharing calculations
  - Mathematical operations with validation

#### 2. Onboarding Context - Customer Aggregate
- **19 Tests Passing** - Complete customer lifecycle management
- **Key Features:**
  - UAE Emirates ID validation (784-YYYY-NNNNNNN-C format)
  - UAE mobile number validation (+971XXXXXXXXX)
  - KYC document management
  - Islamic banking preferences
  - Business customer support
  - Age validation (18+ requirement)
  - Customer status lifecycle (PENDING_KYC → ACTIVE → SUSPENDED)

#### 3. Accounts Context - Account Aggregate
- **19 Tests Passing** - Multi-currency wallet management
- **Key Features:**
  - Multi-currency support (AED, USD, EUR, SAR, QAR, KWD, BHD)
  - CBDC wallet for UAE Digital Dirham with instant settlement
  - Stablecoin wallets (USDC, USDT, etc.)
  - Islamic banking compliance (profit-sharing vs. interest)
  - Account operations (deposit, withdraw, transfer)
  - Account security (freeze/unfreeze for AML)
  - Transaction history with audit trail

#### 4. Payments Context - Payment Aggregate
- **20 Tests Passing** - CBDC settlement and payment processing
- **Key Features:**
  - CBDC payments with ≤5 second settlement requirement
  - Cross-currency payment support
  - Stablecoin payment processing
  - Islamic banking compliance (no interest-based fees)
  - Payment fees management
  - Compliance tracking and AML integration
  - Payment limits validation (500K AED daily limit)
  - Payment status lifecycle (PENDING → PROCESSING → COMPLETED/FAILED)

#### 5. Murabaha Context - Islamic Finance Contracts
- **21 Tests Passing** - Complete Islamic finance contract management
- **Key Features:**
  - Asset-backed financing (no speculation or Gharar)
  - Transparent profit margins (max 25% profit rate)
  - Sharia Supervisory Board approval workflow
  - Installment schedule generation and management
  - Early settlement with Sharia-compliant discounts
  - Asset delivery confirmation requirements
  - Contract default handling
  - Outstanding balance calculations
  - Islamic finance compliance validation (6-84 months terms, 10K AED minimum)

#### 6. Compliance Context - AML & Sharia Validation
- **18 Tests Passing** - Comprehensive compliance and risk management
- **Key Features:**
  - AML (Anti-Money Laundering) automated screening following CBUAE regulations
  - Sharia compliance validation for Islamic finance products
  - Transaction monitoring with risk-based thresholds (10K AED high-value, 50K AED enhanced due diligence)
  - Compliance violation tracking with severity levels
  - Manual review workflow for high-risk cases
  - Sanctions screening and PEP (Politically Exposed Person) checks
  - Compliance scoring and risk assessment
  - Audit trail and regulatory reporting
  - KYC (Know Your Customer) validation integration

### 🔒 FAPI 2.0 API Gateway Features

#### Security Implementation (Phase 2)
- **OAuth 2.1 + PKCE**: Mandatory PKCE for all authorization flows
- **DPoP Token Binding**: RFC 9449 compliance for token theft protection
- **Rate Limiting**: Client-based with different limits per endpoint type
- **Security Headers**: X-Frame-Options, Content-Type-Options, CSRF protection
- **Islamic Banking Headers**: X-Islamic-Banking, X-Sharia-Compliant, X-Regulatory-Compliance
- **Audit Logging**: Comprehensive security event tracking for CBUAE compliance

#### Gateway Features
- **Circuit Breakers**: Resilience4j integration with fallback responses
- **Service Routing**: Intelligent routing to 6 microservices
- **CORS Configuration**: UAE-specific domain support (*.amanahfi.ae)
- **Request Transformation**: Islamic banking context headers
- **High-Value Transaction**: Enhanced security for transactions >10K AED

### 🔄 Event Streaming Infrastructure

#### Kafka Integration (Phase 2)
- **Domain Event Publishing**: Asynchronous event publishing with Islamic banking compliance
- **Event Metadata Enrichment**: Automatic compliance and audit metadata injection
- **Topic Strategy**: Per-bounded-context topics with appropriate retention policies
- **Event Headers**: Islamic banking compliance indicators and regulatory metadata
- **Event Correlation**: Cross-context event tracing and causation tracking

#### Event Architecture
- **Event Types**: DomainEvent, IslamicBankingEvent interfaces
- **Event Metadata**: Regulatory compliance, audit trails, correlation IDs
- **Topic Configuration**: Context-specific topics with compliance-appropriate retention
- **Event Processing**: Reliable publishing with exactly-once semantics for financial data

#### Compliance Features
- **Audit Trail**: All Islamic banking events tracked for regulatory compliance
- **CBDC Events**: Special handling for UAE Digital Dirham transactions
- **Sharia Compliance**: Event validation for Islamic finance operations
- **AML Integration**: Anti-money laundering event streaming to compliance systems

### 🏗️ Architecture

#### Domain-Driven Design (DDD)
- **Bounded Contexts**: Onboarding, Accounts, Payments, Murabaha, Compliance, API Gateway
- **Aggregates**: Customer, Account, Payment, MurabahaContract, ComplianceCheck
- **Value Objects**: Money, CustomerId, EmiratesId, DPoPToken
- **Domain Events**: CustomerRegistered, PaymentCompleted, ComplianceCheckCreated, etc.

#### Hexagonal Architecture
```
Domain (Business Logic)
├── Entities (Customer, Account)
├── Value Objects (Money, EmiratesId)
├── Domain Services
└── Domain Events

Application (Use Cases)
├── Commands & Queries
├── Application Services
└── Event Handlers

Infrastructure (Technical)
├── Persistence (JPA/PostgreSQL)
├── Messaging (Kafka)
├── External APIs
└── Web Controllers
```

### 🧪 Testing Strategy

#### Test Pyramid
1. **Unit Tests** (Current) - Domain logic, value objects, aggregates
2. **Integration Tests** (Next) - Database, messaging, external APIs
3. **Contract Tests** (Future) - API contracts, event schemas
4. **End-to-End Tests** (Future) - Complete user journeys

#### TDD Red-Green-Refactor Cycle
1. **Red** - Write failing test
2. **Green** - Implement minimum code to pass
3. **Refactor** - Improve code quality while maintaining tests

### 📋 Requirements Mapping

#### Business Requirements Covered
- **BR-04**: ≤ 10 min funding time - Foundation laid with fast customer onboarding
- **BR-03**: Zero compliance breaches - Validation and business rules implemented
- **BR-07**: Gig-worker customers - Individual customer type supported

#### Technology Requirements Covered
- **Java 21** with modern features
- **Spring Boot 3.2** framework
- **Hexagonal Architecture** with DDD
- **Event-Driven Architecture** foundation
- **Test-First Development** approach

### 🚀 Next Implementation Steps (TDD)

#### 1. Accounts Context
```java
// Test First
@Test
void shouldCreateMultiCurrencyWallet() {
    // Given: Customer with AED and USD requirements
    // When: Creating wallet with multiple currencies
    // Then: Should support AED fiat, CBDC, and stablecoins
}
```

#### 2. Murabaha Context
```java
// Test First
@Test
void shouldCalculateShariaProfitMargin() {
    // Given: Asset cost and profit rate
    // When: Creating Murabaha contract
    // Then: Should calculate halal profit without interest
}
```

#### 3. Payments Context
```java
// Test First
@Test
void shouldSettleViaCBDC() {
    // Given: UAE Digital Dirham payment
    // When: Processing CBDC settlement
    // Then: Should complete in ≤ 5 seconds
}
```

#### 4. Compliance Context
```java
// Test First
@Test
void shouldPerformAMLCheck() {
    // Given: Customer transaction
    // When: AML screening triggered
    // Then: Should validate against CBUAE requirements
}
```

### 📊 Test Coverage

#### Current Status
- **Shared Kernel**: 100% (18/18 tests)
- **Onboarding Context**: 100% (19/19 tests)
- **Accounts Context**: 100% (19/19 tests)
- **Payments Context**: 100% (20/20 tests)
- **Murabaha Context**: 100% (21/21 tests)
- **Compliance Context**: 100% (18/18 tests)
- **Overall**: 115/115 tests passing

#### Quality Metrics
- **Code Coverage**: 100% on implemented features
- **Mutation Testing**: Planned for next phase
- **Performance Testing**: Foundation established

### 🏦 Islamic Finance Compliance

#### Sharia Principles Implemented
1. **No Riba (Interest)** - Money value object prevents interest calculations
2. **Asset-Backed Financing** - Murabaha framework ready
3. **Risk Sharing** - Profit-sharing calculations implemented
4. **Halal Validation** - Business rules enforce compliance

#### Regulatory Compliance
- **CBUAE** - UAE Central Bank requirements
- **VARA** - Virtual Assets Regulatory Authority
- **HSA** - Higher Sharia Authority guidelines

### 🔧 Development Setup

#### Prerequisites
- Java 21 LTS
- Gradle 8.14+
- Docker & Docker Compose
- PostgreSQL 15+

#### Running Tests
```bash
# All tests
./gradlew test

# Specific context
./gradlew :amanahfi-platform:shared-kernel:test
./gradlew :amanahfi-platform:onboarding-context:test

# With coverage
./gradlew test jacocoTestReport
```

#### Local Development
```bash
# Start infrastructure
docker-compose up -d

# Run application
./gradlew bootRun
```

### 📈 Performance Targets

#### Current Implementation
- **Unit Test Execution**: < 2 seconds
- **Memory Usage**: Optimized value objects
- **Startup Time**: < 10 seconds

#### Production Targets (Requirements)
- **API Latency**: P95 ≤ 300ms
- **CBDC Settlement**: ≤ 5 seconds
- **Throughput**: 1,000 TPS → 10,000 TPS
- **Availability**: ≥ 99.9%

### 🔮 Future Roadmap

#### Phase 1 (COMPLETE) - Foundation ✅
- ✅ Shared Kernel (Money, Islamic finance value objects) - 18 tests passing
- ✅ Customer Onboarding (KYC, UAE compliance) - 19 tests passing  
- ✅ Account Management (Multi-currency, CBDC, Stablecoins) - 19 tests passing
- ✅ CBDC Payments (≤5 second settlement) - 20 tests passing
- ✅ Murabaha Contracts (Islamic finance engine) - 21 tests passing
- ✅ Compliance Engine (AML/Sharia validation) - 18 tests passing
- **Total: 115 tests passing with 100% coverage on core domains**

#### Phase 2 - Integration & Scale
- ✅ **API Gateway & Security (FAPI 2.0)** - COMPLETED ✅
  - ✅ OAuth 2.1 + JWT resource server configuration
  - ✅ DPoP (Demonstration of Proof of Possession) token validation (RFC 9449)
  - ✅ Rate limiting with Islamic banking compliance
  - ✅ FAPI 2.0 security headers (X-Frame-Options, Content-Type-Options)
  - ✅ Islamic banking compliance headers (X-Islamic-Banking, X-Sharia-Compliant)
  - ✅ Circuit breaker patterns for microservices
  - ✅ CORS configuration for UAE domains (*.amanahfi.ae)
  - ✅ Audit event tracking for regulatory compliance
  - ✅ Fallback controllers for graceful degradation
  - ✅ Route configuration for all 6 bounded contexts
- ✅ **Event Streaming (Kafka Integration)** - COMPLETED ✅
  - ✅ Domain event publishing with Kafka producers
  - ✅ Islamic banking event interfaces (IslamicBankingEvent)
  - ✅ Event metadata enrichment for regulatory compliance
  - ✅ Topic configuration for each bounded context
  - ✅ Event headers for Islamic banking compliance
  - ✅ CBDC settlement event tracking (≤5 second requirement)
  - ✅ Murabaha contract event publishing with Sharia validation
  - ✅ AML compliance event streaming for audit trails
  - ✅ Event correlation and causation tracking
  - ✅ Batch event publishing for high-throughput scenarios
- 🔄 Integration Testing
- 🔄 Performance Optimization

#### Phase 3 - Enterprise Features
- 🔄 Multi-tenant Support
- 🔄 Advanced Analytics & AI
- 🔄 Mobile APIs (React Native)
- 🔄 Blockchain Integration (Corda)

---

**Built with ❤️ following Islamic finance principles and modern software engineering practices.**

*AmanahFi Platform v0.9.0-SNAPSHOT*