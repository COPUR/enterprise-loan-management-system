# Data Architecture Overview

## 📋 Executive Summary

The Enterprise Loan Management System data architecture is designed as a modern, cloud-native, event-driven platform that supports traditional and Islamic banking operations across multiple jurisdictions. The architecture emphasizes data security, compliance, real-time processing, and advanced analytics capabilities.

## 🏗️ Data Architecture Principles

### 1. Data as a Strategic Asset
- Comprehensive data governance framework
- Data quality metrics and monitoring
- Master data management across all domains
- Data lifecycle management with retention policies

### 2. Security and Privacy by Design
- Encryption at rest and in transit (AES-256-GCM, TLS 1.3)
- Zero-trust data access model
- Comprehensive audit trails for all data operations
- Privacy-preserving analytics and processing

### 3. Event-Driven Architecture
- Event sourcing for complete audit trails
- CQRS (Command Query Responsibility Segregation)
- Real-time event streaming with Apache Kafka
- Eventually consistent distributed systems

### 4. Multi-Model Data Management
- Relational data for transactional consistency (PostgreSQL)
- Document storage for flexible schemas (MongoDB)
- In-memory caching for performance (Redis)
- Time-series data for analytics (InfluxDB)

## 📊 Data Architecture Layers

### Layer 1: Data Sources
```
┌─────────────────────────────────────────────────────────┐
│                    Data Sources                          │
├─────────────┬─────────────┬──────────────┬─────────────┤
│ Customer    │ Loan        │ Payment      │ External    │
│ Applications│ Origination │ Processing   │ Data Feeds  │
│             │             │              │             │
│ • Web Portal│ • Credit    │ • Payment    │ • Credit    │
│ • Mobile App│   Scoring   │   Gateways   │   Bureaus   │
│ • API Calls │ • Document  │ • Bank       │ • Market    │
│ • Call Center│  Processing │   Networks   │   Data      │
└─────────────┴─────────────┴──────────────┴─────────────┘
```

### Layer 2: Data Ingestion
```
┌─────────────────────────────────────────────────────────┐
│                 Data Ingestion Layer                     │
├─────────────┬─────────────┬──────────────┬─────────────┤
│ Real-time   │ Batch       │ Change Data  │ API         │
│ Streaming   │ Processing  │ Capture      │ Integration │
│             │             │              │             │
│ • Kafka     │ • ETL Jobs  │ • Debezium   │ • REST APIs │
│ • Kinesis   │ • Airflow   │ • Triggers   │ • GraphQL   │
│ • WebSockets│ • Spark     │ • Log Mining │ • gRPC      │
└─────────────┴─────────────┴──────────────┴─────────────┘
```

### Layer 3: Data Storage
```
┌─────────────────────────────────────────────────────────┐
│                   Data Storage Layer                     │
├─────────────┬─────────────┬──────────────┬─────────────┤
│ Transactional│ Document   │ Cache        │ Analytics   │
│ Data        │ Store       │ Layer        │ Store       │
│             │             │              │             │
│ • PostgreSQL│ • MongoDB   │ • Redis      │ • Snowflake │
│ • Event     │ • Document  │ • Hazelcast  │ • ClickHouse│
│   Store     │   Collections│ • Distributed│ • Data Lake │
│ • ACID      │ • Flexible  │   Sessions   │ • S3/Delta  │
│   Compliance│   Schema    │ • Fast Access│   Lake      │
└─────────────┴─────────────┴──────────────┴─────────────┘
```

### Layer 4: Data Processing
```
┌─────────────────────────────────────────────────────────┐
│                Data Processing Layer                     │
├─────────────┬─────────────┬──────────────┬─────────────┤
│ Stream      │ Batch       │ ML/AI        │ Complex     │
│ Processing  │ Processing  │ Processing   │ Analytics   │
│             │             │              │             │
│ • Kafka     │ • Apache    │ • TensorFlow │ • Apache    │
│   Streams   │   Spark     │ • PyTorch    │   Drill     │
│ • Apache    │ • Apache    │ • MLflow     │ • Presto    │
│   Flink     │   Beam      │ • Kubeflow   │ • Superset  │
└─────────────┴─────────────┴──────────────┴─────────────┘
```

### Layer 5: Data Access & API
```
┌─────────────────────────────────────────────────────────┐
│               Data Access & API Layer                    │
├─────────────┬─────────────┬──────────────┬─────────────┤
│ Application │ Analytics   │ Real-time    │ External    │
│ APIs        │ APIs        │ APIs         │ APIs        │
│             │             │              │             │
│ • REST      │ • GraphQL   │ • WebSocket  │ • Open      │
│ • gRPC      │ • SQL APIs  │ • Server-Sent│   Banking   │
│ • FAPI 2.0  │ • Data Mesh │   Events     │ • Partner   │
│ • OAuth 2.1 │ • Self-     │ • Streaming  │   APIs      │
│             │   Service   │   APIs       │             │
└─────────────┴─────────────┴──────────────┴─────────────┘
```

## 🗂️ Data Domains and Models

### Core Banking Domains

#### 1. Customer Domain
```sql
-- Customer Master Entity
CREATE TABLE customers (
    customer_id UUID PRIMARY KEY,
    external_customer_id VARCHAR(50) UNIQUE,
    customer_type VARCHAR(20) CHECK (customer_type IN ('INDIVIDUAL', 'BUSINESS')),
    status VARCHAR(20) CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
    
    -- Personal Information (Individual)
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    date_of_birth DATE,
    nationality VARCHAR(3), -- ISO 3166-1 alpha-3
    
    -- Business Information
    business_name VARCHAR(200),
    business_registration_number VARCHAR(50),
    industry_code VARCHAR(10),
    
    -- Contact Information
    email VARCHAR(320),
    phone_primary VARCHAR(20),
    phone_secondary VARCHAR(20),
    
    -- Address Information
    address_line1 VARCHAR(200),
    address_line2 VARCHAR(200),
    city VARCHAR(100),
    state_province VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(3), -- ISO 3166-1 alpha-3
    
    -- Compliance & Risk
    kyc_status VARCHAR(20) CHECK (kyc_status IN ('PENDING', 'VERIFIED', 'REJECTED')),
    kyc_completion_date TIMESTAMP,
    risk_rating VARCHAR(10) CHECK (risk_rating IN ('LOW', 'MEDIUM', 'HIGH')),
    pep_status BOOLEAN DEFAULT FALSE,
    sanctions_check BOOLEAN DEFAULT FALSE,
    
    -- Islamic Banking
    sharia_compliant_only BOOLEAN DEFAULT FALSE,
    
    -- Audit Fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL,
    updated_by UUID,
    version INTEGER NOT NULL DEFAULT 1
);
```

#### 2. Loan Domain
```sql
-- Loan Master Entity
CREATE TABLE loans (
    loan_id UUID PRIMARY KEY,
    loan_number VARCHAR(50) UNIQUE NOT NULL,
    customer_id UUID NOT NULL REFERENCES customers(customer_id),
    
    -- Loan Details
    product_code VARCHAR(20) NOT NULL,
    product_category VARCHAR(30) CHECK (product_category IN (
        'PERSONAL', 'AUTO', 'MORTGAGE', 'BUSINESS',
        'MURABAHA', 'MUSHARAKA', 'IJARAH', 'SALAM'
    )),
    
    -- Financial Terms
    principal_amount DECIMAL(15,2) NOT NULL,
    interest_rate DECIMAL(8,4),
    profit_rate DECIMAL(8,4), -- For Islamic products
    term_months INTEGER NOT NULL,
    payment_frequency VARCHAR(10) CHECK (payment_frequency IN ('MONTHLY', 'QUARTERLY', 'SEMI_ANNUAL', 'ANNUAL')),
    
    -- Status & Lifecycle
    status VARCHAR(20) CHECK (status IN (
        'APPLICATION', 'UNDERWRITING', 'APPROVED', 'REJECTED', 
        'ACTIVE', 'PAID_OFF', 'DEFAULT', 'CHARGED_OFF'
    )),
    approval_date TIMESTAMP,
    disbursement_date TIMESTAMP,
    maturity_date DATE,
    
    -- Islamic Banking Specific
    sharia_compliant BOOLEAN DEFAULT FALSE,
    underlying_asset VARCHAR(200), -- For asset-backed financing
    ownership_structure TEXT, -- Musharaka/Mudaraba details
    
    -- Risk & Compliance
    credit_score INTEGER,
    debt_to_income_ratio DECIMAL(5,2),
    collateral_value DECIMAL(15,2),
    ltv_ratio DECIMAL(5,2),
    
    -- Audit Fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL,
    updated_by UUID,
    version INTEGER NOT NULL DEFAULT 1
);
```

#### 3. Payment Domain
```sql
-- Payment Transaction Entity
CREATE TABLE payment_transactions (
    transaction_id UUID PRIMARY KEY,
    payment_reference VARCHAR(50) UNIQUE NOT NULL,
    loan_id UUID NOT NULL REFERENCES loans(loan_id),
    customer_id UUID NOT NULL REFERENCES customers(customer_id),
    
    -- Transaction Details
    transaction_type VARCHAR(20) CHECK (transaction_type IN (
        'PAYMENT', 'REFUND', 'FEE', 'PENALTY', 'ADJUSTMENT'
    )),
    amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    
    -- Payment Breakdown
    principal_amount DECIMAL(15,2) DEFAULT 0,
    interest_amount DECIMAL(15,2) DEFAULT 0,
    profit_amount DECIMAL(15,2) DEFAULT 0, -- Islamic banking
    fee_amount DECIMAL(15,2) DEFAULT 0,
    penalty_amount DECIMAL(15,2) DEFAULT 0,
    
    -- Status & Processing
    status VARCHAR(20) CHECK (status IN (
        'PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED'
    )),
    payment_method VARCHAR(20) CHECK (payment_method IN (
        'ACH', 'WIRE', 'CARD', 'CHECK', 'CASH', 'DIGITAL_WALLET'
    )),
    
    -- External References
    external_transaction_id VARCHAR(100),
    gateway_reference VARCHAR(100),
    bank_reference VARCHAR(100),
    
    -- Timing
    scheduled_date DATE,
    processed_date TIMESTAMP,
    settlement_date DATE,
    
    -- Audit Fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL,
    updated_by UUID,
    version INTEGER NOT NULL DEFAULT 1
);
```

## 📡 Event-Driven Data Architecture

### Event Store Design
```sql
-- Event Store for Audit and Sourcing
CREATE TABLE event_store (
    event_id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_version INTEGER NOT NULL,
    event_data JSONB NOT NULL,
    metadata JSONB,
    correlation_id UUID,
    causation_id UUID,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Index for efficient querying
    UNIQUE(aggregate_id, event_version)
);

-- Event Type Examples
-- Customer Events: CustomerCreated, CustomerUpdated, KYCCompleted
-- Loan Events: LoanApplicationSubmitted, LoanApproved, LoanDisbursed
-- Payment Events: PaymentReceived, PaymentFailed, PaymentRefunded
```

### Kafka Topics Structure
```yaml
# Core Business Events
topics:
  - name: customer-events
    partitions: 10
    retention: 30d
    
  - name: loan-events
    partitions: 15
    retention: 90d # Regulatory requirement
    
  - name: payment-events
    partitions: 20
    retention: 90d
    
  - name: risk-events
    partitions: 5
    retention: 180d
    
  - name: compliance-events
    partitions: 3
    retention: 2555d # 7 years
    
  # Islamic Banking Events
  - name: sharia-events
    partitions: 5
    retention: 2555d
    
  # Integration Events
  - name: external-events
    partitions: 8
    retention: 30d
```

## 🔒 Data Security Architecture

### Encryption Strategy
```yaml
encryption:
  at_rest:
    algorithm: AES-256-GCM
    key_management: AWS KMS / HashiCorp Vault
    databases:
      - PostgreSQL: TDE (Transparent Data Encryption)
      - MongoDB: Field-level encryption
      - Redis: Memory encryption
      
  in_transit:
    protocol: TLS 1.3
    certificate_authority: Internal PKI
    mutual_tls: true
    
  application_level:
    sensitive_fields:
      - SSN/National ID
      - Bank account numbers
      - Credit card details
      - Biometric data
    tokenization: Format-preserving encryption
```

### Access Control
```yaml
access_control:
  model: Attribute-Based Access Control (ABAC)
  
  attributes:
    - subject: user, role, department, clearance_level
    - resource: table, column, row, document
    - action: read, write, update, delete
    - environment: time, location, network, device
    
  policies:
    - customer_data:
        allow: [customer_service, account_manager]
        conditions: [same_region, business_hours]
        
    - sensitive_pii:
        allow: [compliance_officer, senior_manager]
        require: [mfa, audit_log]
        
    - financial_data:
        allow: [finance_team, auditor]
        mask: [non_finance_users]
```

## 📊 Data Quality Framework

### Data Quality Dimensions
1. **Accuracy**: Data correctly represents reality
2. **Completeness**: All required data is present
3. **Consistency**: Data is uniform across systems
4. **Timeliness**: Data is up-to-date and available when needed
5. **Validity**: Data conforms to defined formats and rules
6. **Uniqueness**: No inappropriate duplication of data

### Data Quality Rules
```sql
-- Data Quality Rules Examples
CREATE TABLE data_quality_rules (
    rule_id UUID PRIMARY KEY,
    rule_name VARCHAR(100) NOT NULL,
    table_name VARCHAR(100) NOT NULL,
    column_name VARCHAR(100),
    rule_type VARCHAR(50), -- NOT_NULL, RANGE, REGEX, REFERENTIAL
    rule_expression TEXT,
    severity VARCHAR(20), -- ERROR, WARNING, INFO
    is_active BOOLEAN DEFAULT TRUE
);

-- Example Rules
INSERT INTO data_quality_rules VALUES 
    ('rule-001', 'Email Format Validation', 'customers', 'email', 'REGEX', 
     '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$', 'ERROR', TRUE),
    
    ('rule-002', 'Loan Amount Range', 'loans', 'principal_amount', 'RANGE', 
     'BETWEEN 1000 AND 500000', 'ERROR', TRUE),
     
    ('rule-003', 'Interest Rate Validation', 'loans', 'interest_rate', 'RANGE', 
     'BETWEEN 0.001 AND 0.50', 'WARNING', TRUE);
```

### Data Quality Monitoring
```python
# Data Quality Monitoring Pipeline
class DataQualityMonitor:
    def __init__(self):
        self.rules_engine = DataQualityRulesEngine()
        self.metrics_collector = MetricsCollector()
        
    def run_quality_checks(self, table_name):
        """Run all active quality rules for a table"""
        rules = self.get_active_rules(table_name)
        results = []
        
        for rule in rules:
            result = self.execute_rule(rule)
            results.append(result)
            
            # Emit metrics
            self.metrics_collector.record_quality_metric(
                table=table_name,
                rule=rule.name,
                status=result.status,
                violation_count=result.violations,
                timestamp=datetime.now()
            )
            
        return results
```

## 🚀 Data Performance and Scalability

### Database Optimization
```sql
-- Partitioning Strategy for Large Tables
CREATE TABLE payment_transactions_2024 
PARTITION OF payment_transactions 
FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');

-- Indexing Strategy
CREATE INDEX idx_loans_customer_status ON loans(customer_id, status) 
WHERE status IN ('ACTIVE', 'DEFAULT');

CREATE INDEX idx_payments_loan_date ON payment_transactions(loan_id, processed_date DESC);

-- Materialized Views for Analytics
CREATE MATERIALIZED VIEW loan_portfolio_summary AS
SELECT 
    product_category,
    COUNT(*) as loan_count,
    SUM(principal_amount) as total_principal,
    AVG(interest_rate) as avg_interest_rate,
    COUNT(CASE WHEN status = 'DEFAULT' THEN 1 END) as default_count
FROM loans 
WHERE status = 'ACTIVE'
GROUP BY product_category;
```

### Caching Strategy
```yaml
caching_layers:
  application_cache:
    provider: Redis Cluster
    ttl: 300s # 5 minutes
    data_types:
      - customer_profiles
      - loan_calculations
      - exchange_rates
      
  database_cache:
    provider: PostgreSQL Query Cache
    size: 2GB
    hit_ratio_target: 95%
    
  cdn_cache:
    provider: CloudFlare
    ttl: 3600s # 1 hour
    data_types:
      - static_content
      - api_documentation
      - public_rates
```

## 🔄 Data Lifecycle Management

### Retention Policies
```yaml
retention_policies:
  customer_data:
    active_customers: indefinite
    inactive_customers: 7_years
    deceased_customers: 10_years
    
  loan_data:
    active_loans: indefinite
    closed_loans: 7_years
    charged_off_loans: 10_years
    
  transaction_data:
    payment_records: 7_years
    audit_logs: 10_years
    compliance_logs: indefinite
    
  operational_data:
    application_logs: 90_days
    performance_metrics: 1_year
    security_logs: 2_years
```

### Archival Strategy
```python
class DataArchivalService:
    def __init__(self):
        self.archive_storage = S3ArchiveStorage()
        self.database = DatabaseConnection()
        
    def archive_old_data(self, table_name: str, retention_days: int):
        """Archive data older than retention period"""
        
        # Identify records to archive
        cutoff_date = datetime.now() - timedelta(days=retention_days)
        records_to_archive = self.database.query(f"""
            SELECT * FROM {table_name} 
            WHERE created_at < %s
        """, [cutoff_date])
        
        # Archive to S3
        archive_key = f"{table_name}/{cutoff_date.year}/{cutoff_date.month}"
        self.archive_storage.store_data(archive_key, records_to_archive)
        
        # Delete from primary storage
        self.database.execute(f"""
            DELETE FROM {table_name} 
            WHERE created_at < %s
        """, [cutoff_date])
        
        # Log archival
        logger.info(f"Archived {len(records_to_archive)} records from {table_name}")
```

## 📈 Analytics and Reporting Architecture

### Data Warehouse Schema
```sql
-- Star Schema for Loan Analytics
CREATE TABLE dim_customer (
    customer_key SERIAL PRIMARY KEY,
    customer_id UUID UNIQUE,
    customer_type VARCHAR(20),
    age_group VARCHAR(20),
    income_bracket VARCHAR(20),
    credit_score_range VARCHAR(20),
    geographic_region VARCHAR(50),
    effective_date DATE,
    expiry_date DATE,
    is_current BOOLEAN
);

CREATE TABLE dim_product (
    product_key SERIAL PRIMARY KEY,
    product_code VARCHAR(20) UNIQUE,
    product_category VARCHAR(30),
    product_name VARCHAR(100),
    is_sharia_compliant BOOLEAN,
    effective_date DATE,
    expiry_date DATE,
    is_current BOOLEAN
);

CREATE TABLE fact_loan_performance (
    loan_key UUID,
    customer_key INTEGER REFERENCES dim_customer(customer_key),
    product_key INTEGER REFERENCES dim_product(product_key),
    date_key INTEGER,
    
    -- Measures
    principal_balance DECIMAL(15,2),
    interest_accrued DECIMAL(15,2),
    payments_received DECIMAL(15,2),
    days_past_due INTEGER,
    
    -- Flags
    is_current BOOLEAN,
    is_delinquent BOOLEAN,
    is_default BOOLEAN
);
```

### Real-Time Analytics
```python
# Stream Processing for Real-Time Analytics
class LoanPerformanceAnalytics:
    def __init__(self):
        self.kafka_consumer = KafkaConsumer(['payment-events', 'loan-events'])
        self.metrics_store = InfluxDBMetrics()
        
    def process_payment_event(self, event):
        """Process payment events for real-time metrics"""
        
        loan_id = event['loan_id']
        amount = event['amount']
        payment_type = event['transaction_type']
        
        # Update loan performance metrics
        self.metrics_store.write_metric(
            measurement='loan_payments',
            tags={
                'loan_id': loan_id,
                'payment_type': payment_type,
                'product_category': event['product_category']
            },
            fields={
                'amount': amount,
                'cumulative_payments': self.get_cumulative_payments(loan_id)
            },
            timestamp=event['timestamp']
        )
        
        # Update portfolio metrics
        self.update_portfolio_metrics(event)
```

## 🔗 Data Integration Patterns

### API-First Data Access
```yaml
# OpenAPI 3.0 Data API Specification
openapi: 3.0.3
info:
  title: Enterprise Loan Management - Data API
  version: 1.0.0

paths:
  /customers/{customerId}:
    get:
      summary: Retrieve customer data
      parameters:
        - name: customerId
          in: path
          required: true
          schema:
            type: string
            format: uuid
        - name: include
          in: query
          schema:
            type: array
            items:
              type: string
              enum: [loans, payments, demographics, risk_profile]
      responses:
        '200':
          description: Customer data
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Customer'
```

### Event-Driven Integration
```python
# Event-Driven Data Synchronization
class DataSynchronizationService:
    def __init__(self):
        self.event_publisher = KafkaEventPublisher()
        self.external_apis = ExternalAPIClient()
        
    @event_handler('CustomerUpdated')
    def sync_customer_data(self, event):
        """Synchronize customer data with external systems"""
        
        customer_id = event['customer_id']
        changes = event['changes']
        
        # Sync with credit bureau
        if 'credit_score' in changes:
            self.external_apis.update_credit_bureau(customer_id, changes)
            
        # Sync with KYC provider
        if 'kyc_status' in changes:
            self.external_apis.update_kyc_provider(customer_id, changes)
            
        # Publish downstream events
        self.event_publisher.publish('CustomerSynced', {
            'customer_id': customer_id,
            'sync_timestamp': datetime.now(),
            'external_systems': ['credit_bureau', 'kyc_provider']
        })
```

## 🎯 Success Metrics

### Data Architecture KPIs
- **Data Quality Score**: 95%+ across all critical data elements
- **Data Availability**: 99.99% uptime for production databases
- **Query Performance**: P95 < 100ms for operational queries
- **Data Freshness**: <5 minutes latency for real-time data
- **Compliance**: 100% compliance with data regulations

### Business Impact Metrics
- **Time to Insights**: Reduce from days to minutes
- **Data-Driven Decisions**: 90% of business decisions supported by data
- **Regulatory Reporting**: Automated 95% of compliance reports
- **Customer 360 View**: Single source of truth for all customer data
- **Real-Time Risk Assessment**: <30 seconds for loan decisions

---

**Document Version**: 1.0  
**Last Updated**: January 2025  
**Owner**: Data Architecture Team  
**Review Cycle**: Quarterly