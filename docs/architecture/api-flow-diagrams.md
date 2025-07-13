# Enterprise Banking Platform - API Flow Diagrams

## Overview

This document provides comprehensive architecture diagrams showing synchronous and asynchronous API flows for the Enterprise Banking Platform.

## 1. Synchronous API Flows

### 1.1 Customer Onboarding Flow

```mermaid
sequenceDiagram
    participant Client
    participant Gateway as API Gateway
    participant Auth as Auth Service
    participant Customer as Customer Service
    participant KYC as KYC Service
    participant Credit as Credit Service
    participant Events as Event Bus

    Client->>Gateway: POST /api/v1/customers
    Note over Client,Gateway: Headers: Idempotency-Key, X-FAPI-Financial-Id
    
    Gateway->>Auth: Validate OAuth2 Token
    Auth-->>Gateway: Token Valid + Scopes
    
    Gateway->>Customer: Create Customer Request
    Customer->>KYC: Initiate KYC Verification
    KYC-->>Customer: KYC Result
    
    Customer->>Credit: Calculate Initial Credit Limit
    Credit-->>Customer: Credit Assessment
    
    Customer->>Events: Publish CustomerCreated Event
    Customer-->>Gateway: Customer Response + HATEOAS Links
    Gateway-->>Client: 201 Created + HAL+JSON

    Note over Client,Events: Async: KYC verification continues in background
```

### 1.2 Loan Application Flow

```mermaid
sequenceDiagram
    participant Client
    participant Gateway as API Gateway
    participant Loan as Loan Service
    participant Customer as Customer Service
    participant Credit as Credit Service
    participant Risk as Risk Engine
    participant Events as Event Bus

    Client->>Gateway: POST /api/v1/loans
    Note over Client,Gateway: Headers: Idempotency-Key
    
    Gateway->>Loan: Create Loan Application
    Loan->>Customer: Verify Customer & Credit
    Customer-->>Loan: Customer Valid + Available Credit
    
    Loan->>Credit: Reserve Credit Amount
    Credit-->>Loan: Credit Reserved
    
    Loan->>Risk: Risk Assessment
    Risk-->>Loan: Risk Score + Decision
    
    alt Risk Score Acceptable
        Loan->>Events: Publish LoanApplicationSubmitted
        Loan-->>Gateway: 201 Created + Approval Links
    else Risk Score Too High
        Loan->>Credit: Release Reserved Credit
        Loan->>Events: Publish LoanRejected
        Loan-->>Gateway: 422 Unprocessable Entity
    end
    
    Gateway-->>Client: Response + HATEOAS Links
```

### 1.3 Payment Processing Flow

```mermaid
sequenceDiagram
    participant Client
    participant Gateway as API Gateway
    participant Payment as Payment Service
    participant Fraud as Fraud Detection
    participant Account as Account Service
    participant Settlement as Settlement Service
    participant Events as Event Bus

    Client->>Gateway: POST /api/v1/payments
    Note over Client,Gateway: Headers: Idempotency-Key, X-FAPI-Interaction-Id
    
    Gateway->>Payment: Process Payment Request
    Payment->>Fraud: Real-time Fraud Check
    
    alt Fraud Check Passed
        Fraud-->>Payment: Low Risk Score
        Payment->>Account: Validate Accounts & Balance
        Account-->>Payment: Accounts Valid
        
        Payment->>Settlement: Initiate Settlement
        Settlement-->>Payment: Settlement ID
        
        Payment->>Events: Publish PaymentInitiated
        Payment-->>Gateway: 201 Created + Processing Links
    else Fraud Detected
        Fraud-->>Payment: High Risk Score
        Payment->>Events: Publish FraudDetected
        Payment-->>Gateway: 422 Fraud Alert
    end
    
    Gateway-->>Client: Response + HATEOAS Links
```

## 2. Asynchronous API Flows

### 2.1 Event-Driven Loan Processing

```mermaid
sequenceDiagram
    participant Loan as Loan Service
    participant Events as Event Bus
    participant Underwriting as Underwriting Service
    participant Compliance as Compliance Service
    participant Customer as Customer Service
    participant Notification as Notification Service

    Loan->>Events: Publish LoanApplicationSubmitted
    Events->>Underwriting: Consume Event
    Events->>Compliance: Consume Event
    
    par Parallel Processing
        Underwriting->>Underwriting: Credit Analysis
        and
        Compliance->>Compliance: AML/KYC Checks
    end
    
    Underwriting->>Events: Publish UnderwritingCompleted
    Compliance->>Events: Publish ComplianceChecked
    
    Loan->>Events: Consume Both Events
    
    alt All Checks Passed
        Loan->>Events: Publish LoanApproved
        Events->>Customer: Update Credit Status
        Events->>Notification: Send Approval Notice
    else Any Check Failed
        Loan->>Events: Publish LoanRejected
        Events->>Customer: Release Reserved Credit
        Events->>Notification: Send Rejection Notice
    end
```

### 2.2 Payment Settlement Saga

```mermaid
sequenceDiagram
    participant Payment as Payment Service
    participant Events as Event Bus
    participant Account as Account Service
    participant Settlement as Settlement Service
    participant Notification as Notification Service
    participant Saga as Payment Saga

    Payment->>Events: Publish PaymentInitiated
    Events->>Saga: Start Payment Saga
    
    Saga->>Account: Debit Source Account
    
    alt Debit Successful
        Account->>Events: Publish AccountDebited
        Events->>Saga: Continue Saga
        
        Saga->>Account: Credit Target Account
        
        alt Credit Successful
            Account->>Events: Publish AccountCredited
            Events->>Saga: Complete Saga
            
            Saga->>Settlement: Mark Settlement Complete
            Saga->>Events: Publish PaymentCompleted
            Events->>Notification: Send Success Notice
        else Credit Failed
            Account->>Events: Publish AccountCreditFailed
            Events->>Saga: Compensate
            
            Saga->>Account: Reverse Debit (Compensating Action)
            Saga->>Events: Publish PaymentFailed
        end
    else Debit Failed
        Account->>Events: Publish AccountDebitFailed
        Events->>Saga: Fail Saga
        
        Saga->>Events: Publish PaymentFailed
        Events->>Notification: Send Failure Notice
    end
```

### 2.3 Real-time Event Streaming

```mermaid
sequenceDiagram
    participant Client
    participant Gateway as API Gateway
    participant SSE as SSE Endpoint
    participant Events as Event Bus
    participant Services as Banking Services

    Client->>Gateway: GET /api/v1/payments/{id}/events
    Note over Client,Gateway: Accept: text/event-stream
    
    Gateway->>SSE: Establish SSE Connection
    SSE-->>Client: 200 OK + Event Stream
    
    Services->>Events: Publish Domain Events
    Events->>SSE: Route Events by Filter
    
    loop Real-time Updates
        SSE->>Client: Server-Sent Event
        Note over SSE,Client: event: PaymentProcessing<br/>data: {"status": "processing"}
    end
    
    Services->>Events: Publish PaymentCompleted
    Events->>SSE: Final Event
    SSE->>Client: Server-Sent Event
    Note over SSE,Client: event: PaymentCompleted<br/>data: {"status": "completed"}
    
    SSE->>Client: Connection Close
```

## 3. API Gateway Architecture

```mermaid
graph TB
    subgraph "External Clients"
        Web[Web Application]
        Mobile[Mobile Apps]
        Partner[Partner APIs]
        Admin[Admin Portal]
    end
    
    subgraph "API Gateway Layer"
        Gateway[Istio Gateway]
        LB[Load Balancer]
        
        subgraph "Gateway Filters"
            Auth[Authentication]
            Rate[Rate Limiting]
            CORS[CORS Handler]
            Trace[Tracing]
            Log[Logging]
        end
    end
    
    subgraph "Service Mesh (Istio)"
        subgraph "Banking Services"
            Customer[Customer Service]
            Loan[Loan Service]
            Payment[Payment Service]
            Account[Account Service]
        end
        
        subgraph "Platform Services"
            AuthSvc[Auth Service]
            Notification[Notification Service]
            Fraud[Fraud Detection]
            Compliance[Compliance Service]
        end
    end
    
    subgraph "Event Infrastructure"
        Kafka[Apache Kafka]
        Schema[Schema Registry]
        EventStore[Event Store]
    end
    
    subgraph "Data Layer"
        PostgreSQL[(PostgreSQL)]
        Redis[(Redis Cache)]
        S3[(Document Store)]
    end
    
    Web --> Gateway
    Mobile --> Gateway
    Partner --> Gateway
    Admin --> Gateway
    
    Gateway --> LB
    LB --> Auth
    Auth --> Rate
    Rate --> CORS
    CORS --> Trace
    Trace --> Log
    
    Log --> Customer
    Log --> Loan
    Log --> Payment
    Log --> Account
    Log --> AuthSvc
    Log --> Notification
    Log --> Fraud
    Log --> Compliance
    
    Customer --> Kafka
    Loan --> Kafka
    Payment --> Kafka
    Account --> Kafka
    
    Kafka --> Schema
    Kafka --> EventStore
    
    Customer --> PostgreSQL
    Loan --> PostgreSQL
    Payment --> PostgreSQL
    Account --> PostgreSQL
    
    Customer --> Redis
    Loan --> Redis
    Payment --> Redis
    
    Compliance --> S3
    Fraud --> S3
```

## 4. HATEOAS Navigation Flow

```mermaid
graph LR
    subgraph "Customer Journey"
        A[Create Customer] --> B[Customer Created]
        B --> C[Update Credit Limit]
        B --> D[Reserve Credit]
        B --> E[Customer Events Stream]
    end
    
    subgraph "Loan Journey"
        F[Apply for Loan] --> G[Loan Application]
        G --> H[Approve Loan]
        G --> I[Reject Loan]
        H --> J[Disburse Loan]
        J --> K[Active Loan]
        K --> L[Make Payment]
        K --> M[Amortization Schedule]
        K --> N[Loan Events Stream]
    end
    
    subgraph "Payment Journey"
        O[Process Payment] --> P[Payment Initiated]
        P --> Q[Payment Processing]
        Q --> R[Payment Completed]
        Q --> S[Payment Failed]
        R --> T[Get Receipt]
        R --> U[Refund Payment]
        S --> V[Retry Payment]
        P --> W[Cancel Payment]
        P --> X[Payment Events Stream]
    end
    
    B -.-> F
    D -.-> O
    L -.-> O
```

## 5. Security Flow (FAPI 2.0)

```mermaid
sequenceDiagram
    participant Client
    participant AuthZ as Authorization Server
    participant Gateway as API Gateway
    participant Resource as Resource Server

    Note over Client,Resource: FAPI 2.0 Security Flow with mTLS and JARM

    Client->>AuthZ: 1. PAR Request (mTLS)
    Note over Client,AuthZ: Pushed Authorization Request<br/>with Rich Authorization Request
    AuthZ-->>Client: 2. Request URI

    Client->>AuthZ: 3. Authorization Request
    Note over Client,AuthZ: Uses Request URI from PAR
    AuthZ-->>Client: 4. JARM Response (JWT)
    Note over Client,AuthZ: JWT Secured Authorization Response

    Client->>AuthZ: 5. Token Request (mTLS + PKCE)
    AuthZ-->>Client: 6. Access Token (JWT)

    Client->>Gateway: 7. API Request
    Note over Client,Gateway: mTLS + Bearer Token<br/>+ FAPI Headers
    
    Gateway->>Gateway: 8. Validate mTLS Certificate
    Gateway->>AuthZ: 9. Token Introspection
    AuthZ-->>Gateway: 10. Token Valid + Claims
    
    Gateway->>Resource: 11. Forward Request
    Note over Gateway,Resource: Add User Context Headers
    Resource-->>Gateway: 12. Response + HATEOAS
    Gateway-->>Client: 13. Final Response
```

## 6. Idempotency Pattern

```mermaid
sequenceDiagram
    participant Client
    participant Gateway as API Gateway
    participant Service as Banking Service
    participant Cache as Idempotency Cache
    participant DB as Database

    Client->>Gateway: POST /api/v1/payments
    Note over Client,Gateway: Idempotency-Key: uuid-123

    Gateway->>Service: Forward Request
    Service->>Cache: Check Idempotency Key
    
    alt Key Exists (Duplicate Request)
        Cache-->>Service: Return Cached Response
        Service-->>Gateway: Cached Result
        Gateway-->>Client: 200 OK (Idempotent)
        Note over Client,Cache: Same response as original
    else Key Not Exists (New Request)
        Cache-->>Service: Key Not Found
        Service->>DB: Process Business Logic
        DB-->>Service: Success
        
        Service->>Cache: Store Response with Key
        Service-->>Gateway: New Response
        Gateway-->>Client: 201 Created
        Note over Client,Cache: Response cached for future duplicates
    end
```

## 7. Event Sourcing Pattern

```mermaid
graph TB
    subgraph "Command Side"
        Cmd[Command Handler]
        Agg[Aggregate Root]
        Events[Domain Events]
    end
    
    subgraph "Event Store"
        Stream[Event Stream]
        Snapshot[Snapshots]
    end
    
    subgraph "Query Side"
        Projection[Event Projections]
        ReadModel[Read Models]
        Views[Materialized Views]
    end
    
    subgraph "Event Bus"
        Kafka[Apache Kafka]
        Handlers[Event Handlers]
    end
    
    Cmd --> Agg
    Agg --> Events
    Events --> Stream
    Events --> Kafka
    
    Stream --> Projection
    Projection --> ReadModel
    ReadModel --> Views
    
    Kafka --> Handlers
    Handlers --> Projection
    
    Stream -.-> Snapshot
    Snapshot -.-> Agg
```

## 8. Rate Limiting Architecture

```mermaid
graph TB
    subgraph "Client Requests"
        Web[Web Client]
        Mobile[Mobile App]
        API[API Client]
    end
    
    subgraph "Rate Limiting Layer"
        Gateway[API Gateway]
        
        subgraph "Rate Limiters"
            Auth[Auth Endpoints: 5/min]
            Financial[Financial Ops: 10/min]
            Read[Read Ops: 100/min]
            Admin[Admin Ops: 2/min]
        end
        
        Cache[Redis Cache]
    end
    
    subgraph "Backend Services"
        Services[Banking Services]
    end
    
    Web --> Gateway
    Mobile --> Gateway
    API --> Gateway
    
    Gateway --> Auth
    Gateway --> Financial
    Gateway --> Read
    Gateway --> Admin
    
    Auth --> Cache
    Financial --> Cache
    Read --> Cache
    Admin --> Cache
    
    Auth --> Services
    Financial --> Services
    Read --> Services
    Admin --> Services
```

## API Design Principles

### 1. RESTful Design
- Resource-based URLs
- HTTP verbs for actions
- Stateless interactions
- Cacheable responses

### 2. HATEOAS Implementation
- Hypermedia controls in responses
- Discoverable API actions
- State transition guidance
- Reduced client coupling

### 3. Event-Driven Architecture
- Asynchronous processing
- Loose coupling between services
- Eventual consistency
- Scalable event handling

### 4. Security-First Approach
- FAPI 2.0 compliance
- mTLS for transport security
- OAuth 2.1 for authorization
- Comprehensive audit trails

### 5. Observability
- Distributed tracing
- Structured logging
- Custom metrics
- Health monitoring