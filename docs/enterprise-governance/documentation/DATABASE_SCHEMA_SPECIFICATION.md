# Database Schema Specification
## Microservices Architecture - Isolated Database Design

### Overview

The Enterprise Loan Management System implements database-per-microservice pattern with four isolated schemas, each optimized for specific domain responsibilities and access patterns.

---

## 1. Customer Database Schema (`customer_db`)

**Connection**: Port 5432, Schema: `customer_db`  
**Service**: Customer Management Microservice (Port 8081)  
**Connection Pool**: HikariCP, Max Pool Size: 20  

### 1.1 Customers Table
```sql
CREATE TABLE customers (
    customer_id VARCHAR(255) PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20),
    date_of_birth DATE,
    identification_number VARCHAR(50) UNIQUE,
    identification_type VARCHAR(20) DEFAULT 'SSN',
    credit_limit DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    available_credit DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    used_credit DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    annual_income DECIMAL(15,2),
    employment_status VARCHAR(50),
    credit_score INTEGER,
    account_status VARCHAR(50) DEFAULT 'ACTIVE',
    kyc_status VARCHAR(50) DEFAULT 'PENDING',
    risk_level VARCHAR(20) DEFAULT 'MEDIUM',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP,
    version INTEGER DEFAULT 1
);

-- Indexes
CREATE INDEX idx_customers_email ON customers(email);
CREATE INDEX idx_customers_phone ON customers(phone);
CREATE INDEX idx_customers_credit_score ON customers(credit_score);
CREATE INDEX idx_customers_account_status ON customers(account_status);
CREATE INDEX idx_customers_created_at ON customers(created_at);

-- Constraints
ALTER TABLE customers ADD CONSTRAINT chk_credit_limit_positive 
    CHECK (credit_limit >= 0);
ALTER TABLE customers ADD CONSTRAINT chk_available_credit_valid 
    CHECK (available_credit >= 0 AND available_credit <= credit_limit);
ALTER TABLE customers ADD CONSTRAINT chk_credit_score_range 
    CHECK (credit_score >= 300 AND credit_score <= 850);
ALTER TABLE customers ADD CONSTRAINT chk_annual_income_positive 
    CHECK (annual_income >= 0);
```

### 1.2 Customer Addresses Table
```sql
CREATE TABLE customer_addresses (
    address_id SERIAL PRIMARY KEY,
    customer_id VARCHAR(255) NOT NULL,
    address_type VARCHAR(50) DEFAULT 'PRIMARY',
    street VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(50) NOT NULL,
    zip_code VARCHAR(20) NOT NULL,
    country VARCHAR(50) DEFAULT 'USA',
    is_primary BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
);

CREATE INDEX idx_customer_addresses_customer_id ON customer_addresses(customer_id);
CREATE INDEX idx_customer_addresses_primary ON customer_addresses(customer_id, is_primary);
```

### 1.3 Credit Reservations Table
```sql
CREATE TABLE credit_reservations (
    reservation_id VARCHAR(255) PRIMARY KEY,
    customer_id VARCHAR(255) NOT NULL,
    reserved_amount DECIMAL(15,2) NOT NULL,
    purpose VARCHAR(100) NOT NULL,
    loan_application_id VARCHAR(255),
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    released_at TIMESTAMP,
    actual_amount_used DECIMAL(15,2) DEFAULT 0.00,
    notes TEXT,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
);

CREATE INDEX idx_credit_reservations_customer_id ON credit_reservations(customer_id);
CREATE INDEX idx_credit_reservations_status ON credit_reservations(status);
CREATE INDEX idx_credit_reservations_expires_at ON credit_reservations(expires_at);

-- Constraint
ALTER TABLE credit_reservations ADD CONSTRAINT chk_reserved_amount_positive 
    CHECK (reserved_amount > 0);
```

### 1.4 Credit History Table
```sql
CREATE TABLE credit_history (
    history_id SERIAL PRIMARY KEY,
    customer_id VARCHAR(255) NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    previous_available_credit DECIMAL(15,2),
    new_available_credit DECIMAL(15,2),
    reference_id VARCHAR(255),
    reference_type VARCHAR(50),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
);

CREATE INDEX idx_credit_history_customer_id ON credit_history(customer_id);
CREATE INDEX idx_credit_history_created_at ON credit_history(created_at);
CREATE INDEX idx_credit_history_transaction_type ON credit_history(transaction_type);
```

---

## 2. Loan Database Schema (`loan_db`)

**Connection**: Port 5432, Schema: `loan_db`  
**Service**: Loan Origination Microservice (Port 8082)  
**Connection Pool**: HikariCP, Max Pool Size: 25  

### 2.1 Loans Table
```sql
CREATE TABLE loans (
    loan_id VARCHAR(255) PRIMARY KEY,
    application_id VARCHAR(255) UNIQUE,
    customer_id VARCHAR(255) NOT NULL,
    loan_amount DECIMAL(15,2) NOT NULL,
    outstanding_amount DECIMAL(15,2) NOT NULL,
    interest_rate DECIMAL(5,4) NOT NULL,
    installment_count INTEGER NOT NULL,
    installment_amount DECIMAL(15,2) NOT NULL,
    total_repayment_amount DECIMAL(15,2) NOT NULL,
    loan_type VARCHAR(50) NOT NULL,
    purpose VARCHAR(100),
    status VARCHAR(50) DEFAULT 'PENDING_APPROVAL',
    application_date TIMESTAMP NOT NULL,
    approval_date TIMESTAMP,
    disbursement_date TIMESTAMP,
    first_installment_date DATE,
    last_installment_date DATE,
    maturity_date DATE,
    collateral_type VARCHAR(50) DEFAULT 'NONE',
    collateral_value DECIMAL(15,2) DEFAULT 0.00,
    employment_verification_status VARCHAR(50) DEFAULT 'PENDING',
    credit_verification_status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version INTEGER DEFAULT 1,
    saga_id VARCHAR(255)
);

-- Indexes
CREATE INDEX idx_loans_customer_id ON loans(customer_id);
CREATE INDEX idx_loans_status ON loans(status);
CREATE INDEX idx_loans_application_date ON loans(application_date);
CREATE INDEX idx_loans_disbursement_date ON loans(disbursement_date);
CREATE INDEX idx_loans_maturity_date ON loans(maturity_date);
CREATE INDEX idx_loans_saga_id ON loans(saga_id);

-- Constraints
ALTER TABLE loans ADD CONSTRAINT chk_loan_amount_positive 
    CHECK (loan_amount > 0);
ALTER TABLE loans ADD CONSTRAINT chk_interest_rate_range 
    CHECK (interest_rate >= 0.001 AND interest_rate <= 0.5);
ALTER TABLE loans ADD CONSTRAINT chk_installment_count_valid 
    CHECK (installment_count IN (6, 9, 12, 24));
ALTER TABLE loans ADD CONSTRAINT chk_outstanding_amount_valid 
    CHECK (outstanding_amount >= 0 AND outstanding_amount <= loan_amount);
```

### 2.2 Loan Installments Table
```sql
CREATE TABLE loan_installments (
    installment_id SERIAL PRIMARY KEY,
    loan_id VARCHAR(255) NOT NULL,
    installment_number INTEGER NOT NULL,
    due_date DATE NOT NULL,
    principal_amount DECIMAL(15,2) NOT NULL,
    interest_amount DECIMAL(15,2) NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    paid_date TIMESTAMP,
    paid_amount DECIMAL(15,2) DEFAULT 0.00,
    discount_applied DECIMAL(15,2) DEFAULT 0.00,
    penalty_applied DECIMAL(15,2) DEFAULT 0.00,
    remaining_amount DECIMAL(15,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (loan_id) REFERENCES loans(loan_id) ON DELETE CASCADE,
    UNIQUE(loan_id, installment_number)
);

CREATE INDEX idx_loan_installments_loan_id ON loan_installments(loan_id);
CREATE INDEX idx_loan_installments_due_date ON loan_installments(due_date);
CREATE INDEX idx_loan_installments_status ON loan_installments(status);

-- Constraints
ALTER TABLE loan_installments ADD CONSTRAINT chk_installment_number_positive 
    CHECK (installment_number > 0);
ALTER TABLE loan_installments ADD CONSTRAINT chk_principal_amount_positive 
    CHECK (principal_amount >= 0);
ALTER TABLE loan_installments ADD CONSTRAINT chk_total_amount_calculation 
    CHECK (total_amount = principal_amount + interest_amount);
```

### 2.3 Loan Applications Table
```sql
CREATE TABLE loan_applications (
    application_id VARCHAR(255) PRIMARY KEY,
    customer_id VARCHAR(255) NOT NULL,
    requested_amount DECIMAL(15,2) NOT NULL,
    requested_term INTEGER NOT NULL,
    loan_type VARCHAR(50) NOT NULL,
    purpose VARCHAR(100),
    employment_details JSONB,
    income_verification JSONB,
    collateral_details JSONB,
    application_status VARCHAR(50) DEFAULT 'SUBMITTED',
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP,
    reviewer_id VARCHAR(255),
    review_notes TEXT,
    approval_conditions JSONB,
    rejection_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_loan_applications_customer_id ON loan_applications(customer_id);
CREATE INDEX idx_loan_applications_status ON loan_applications(application_status);
CREATE INDEX idx_loan_applications_submitted_at ON loan_applications(submitted_at);
```

### 2.4 Loan Documents Table
```sql
CREATE TABLE loan_documents (
    document_id SERIAL PRIMARY KEY,
    loan_id VARCHAR(255) NOT NULL,
    document_type VARCHAR(100) NOT NULL,
    document_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500),
    file_size BIGINT,
    mime_type VARCHAR(100),
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    uploaded_by VARCHAR(255),
    verification_status VARCHAR(50) DEFAULT 'PENDING',
    verified_at TIMESTAMP,
    verified_by VARCHAR(255),
    FOREIGN KEY (loan_id) REFERENCES loans(loan_id) ON DELETE CASCADE
);

CREATE INDEX idx_loan_documents_loan_id ON loan_documents(loan_id);
CREATE INDEX idx_loan_documents_type ON loan_documents(document_type);
CREATE INDEX idx_loan_documents_verification_status ON loan_documents(verification_status);
```

---

## 3. Payment Database Schema (`payment_db`)

**Connection**: Port 5432, Schema: `payment_db`  
**Service**: Payment Processing Microservice (Port 8083)  
**Connection Pool**: HikariCP, Max Pool Size: 30  

### 3.1 Payments Table
```sql
CREATE TABLE payments (
    payment_id VARCHAR(255) PRIMARY KEY,
    loan_id VARCHAR(255) NOT NULL,
    customer_id VARCHAR(255) NOT NULL,
    payment_amount DECIMAL(15,2) NOT NULL,
    payment_date TIMESTAMP NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    payment_reference VARCHAR(255),
    status VARCHAR(50) DEFAULT 'PENDING',
    processing_fee DECIMAL(15,2) DEFAULT 0.00,
    total_amount DECIMAL(15,2) NOT NULL,
    bank_details JSONB,
    payment_gateway_response JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    failed_at TIMESTAMP,
    failure_reason TEXT,
    version INTEGER DEFAULT 1
);

-- Indexes
CREATE INDEX idx_payments_loan_id ON payments(loan_id);
CREATE INDEX idx_payments_customer_id ON payments(customer_id);
CREATE INDEX idx_payments_payment_date ON payments(payment_date);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_reference ON payments(payment_reference);
CREATE INDEX idx_payments_created_at ON payments(created_at);

-- Constraints
ALTER TABLE payments ADD CONSTRAINT chk_payment_amount_positive 
    CHECK (payment_amount > 0);
ALTER TABLE payments ADD CONSTRAINT chk_total_amount_calculation 
    CHECK (total_amount = payment_amount + processing_fee);
```

### 3.2 Payment Installments Table
```sql
CREATE TABLE payment_installments (
    payment_installment_id SERIAL PRIMARY KEY,
    payment_id VARCHAR(255) NOT NULL,
    installment_number INTEGER NOT NULL,
    due_amount DECIMAL(15,2) NOT NULL,
    paid_amount DECIMAL(15,2) NOT NULL,
    principal_paid DECIMAL(15,2) NOT NULL,
    interest_paid DECIMAL(15,2) NOT NULL,
    penalty_paid DECIMAL(15,2) DEFAULT 0.00,
    discount_applied DECIMAL(15,2) DEFAULT 0.00,
    remaining_amount DECIMAL(15,2) DEFAULT 0.00,
    early_payment_days INTEGER DEFAULT 0,
    late_payment_days INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (payment_id) REFERENCES payments(payment_id) ON DELETE CASCADE
);

CREATE INDEX idx_payment_installments_payment_id ON payment_installments(payment_id);
CREATE INDEX idx_payment_installments_installment_number ON payment_installments(installment_number);

-- Constraints
ALTER TABLE payment_installments ADD CONSTRAINT chk_paid_amount_positive 
    CHECK (paid_amount >= 0);
ALTER TABLE payment_installments ADD CONSTRAINT chk_payment_breakdown_valid 
    CHECK (paid_amount = principal_paid + interest_paid + penalty_paid - discount_applied);
```

### 3.3 Payment History Table
```sql
CREATE TABLE payment_history (
    history_id SERIAL PRIMARY KEY,
    payment_id VARCHAR(255) NOT NULL,
    loan_id VARCHAR(255) NOT NULL,
    customer_id VARCHAR(255) NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    balance_before DECIMAL(15,2),
    balance_after DECIMAL(15,2),
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    description TEXT,
    reference_data JSONB,
    FOREIGN KEY (payment_id) REFERENCES payments(payment_id) ON DELETE CASCADE
);

CREATE INDEX idx_payment_history_payment_id ON payment_history(payment_id);
CREATE INDEX idx_payment_history_loan_id ON payment_history(loan_id);
CREATE INDEX idx_payment_history_customer_id ON payment_history(customer_id);
CREATE INDEX idx_payment_history_transaction_date ON payment_history(transaction_date);
CREATE INDEX idx_payment_history_transaction_type ON payment_history(transaction_type);
```

### 3.4 Payment Calculations Table
```sql
CREATE TABLE payment_calculations (
    calculation_id SERIAL PRIMARY KEY,
    loan_id VARCHAR(255) NOT NULL,
    installment_number INTEGER NOT NULL,
    calculation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    due_date DATE NOT NULL,
    base_amount DECIMAL(15,2) NOT NULL,
    discount_rate DECIMAL(8,6) DEFAULT 0.000000,
    penalty_rate DECIMAL(8,6) DEFAULT 0.000000,
    discount_amount DECIMAL(15,2) DEFAULT 0.00,
    penalty_amount DECIMAL(15,2) DEFAULT 0.00,
    final_amount DECIMAL(15,2) NOT NULL,
    early_payment_days INTEGER DEFAULT 0,
    late_payment_days INTEGER DEFAULT 0,
    calculation_type VARCHAR(50) DEFAULT 'STANDARD',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payment_calculations_loan_id ON payment_calculations(loan_id);
CREATE INDEX idx_payment_calculations_installment ON payment_calculations(installment_number);
CREATE INDEX idx_payment_calculations_due_date ON payment_calculations(due_date);
```

---

## 4. Gateway Database Schema (`banking_gateway`)

**Connection**: Port 5432, Schema: `banking_gateway`  
**Service**: API Gateway (Port 8080)  
**Connection Pool**: HikariCP, Max Pool Size: 15  

### 4.1 SAGA States Table
```sql
CREATE TABLE saga_states (
    saga_id VARCHAR(255) PRIMARY KEY,
    saga_type VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'STARTED',
    current_step VARCHAR(100) NOT NULL,
    saga_data JSONB NOT NULL,
    completed_steps JSONB DEFAULT '[]'::jsonb,
    compensation_data JSONB DEFAULT '{}'::jsonb,
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP,
    timeout_at TIMESTAMP NOT NULL,
    last_heartbeat TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    error_details JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version INTEGER DEFAULT 1
);

-- Indexes
CREATE INDEX idx_saga_states_type ON saga_states(saga_type);
CREATE INDEX idx_saga_states_status ON saga_states(status);
CREATE INDEX idx_saga_states_timeout_at ON saga_states(timeout_at);
CREATE INDEX idx_saga_states_created_at ON saga_states(created_at);
CREATE INDEX idx_saga_states_current_step ON saga_states(current_step);

-- Constraints
ALTER TABLE saga_states ADD CONSTRAINT chk_retry_count_valid 
    CHECK (retry_count >= 0 AND retry_count <= max_retries);
```

### 4.2 Audit Events Table
```sql
CREATE TABLE audit_events (
    event_id SERIAL PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    user_id VARCHAR(255),
    customer_id VARCHAR(255),
    resource_type VARCHAR(100),
    resource_id VARCHAR(255),
    action VARCHAR(100) NOT NULL,
    request_method VARCHAR(10),
    request_url VARCHAR(500),
    request_ip VARCHAR(45),
    user_agent TEXT,
    request_headers JSONB,
    request_body JSONB,
    response_status INTEGER,
    response_body JSONB,
    processing_time_ms INTEGER,
    success BOOLEAN NOT NULL,
    error_message TEXT,
    risk_score INTEGER,
    compliance_flags JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_audit_events_event_type ON audit_events(event_type);
CREATE INDEX idx_audit_events_service_name ON audit_events(service_name);
CREATE INDEX idx_audit_events_user_id ON audit_events(user_id);
CREATE INDEX idx_audit_events_customer_id ON audit_events(customer_id);
CREATE INDEX idx_audit_events_action ON audit_events(action);
CREATE INDEX idx_audit_events_created_at ON audit_events(created_at);
CREATE INDEX idx_audit_events_success ON audit_events(success);
CREATE INDEX idx_audit_events_resource ON audit_events(resource_type, resource_id);
```

### 4.3 Session Data Table
```sql
CREATE TABLE session_data (
    session_id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    customer_id VARCHAR(255),
    session_data JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    last_accessed TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent TEXT,
    is_active BOOLEAN DEFAULT true
);

-- Indexes
CREATE INDEX idx_session_data_user_id ON session_data(user_id);
CREATE INDEX idx_session_data_customer_id ON session_data(customer_id);
CREATE INDEX idx_session_data_expires_at ON session_data(expires_at);
CREATE INDEX idx_session_data_active ON session_data(is_active);

-- Auto-cleanup expired sessions
CREATE OR REPLACE FUNCTION cleanup_expired_sessions()
RETURNS void AS $$
BEGIN
    DELETE FROM session_data WHERE expires_at < NOW();
END;
$$ LANGUAGE plpgsql;
```

### 4.4 Circuit Breaker States Table
```sql
CREATE TABLE circuit_breaker_states (
    breaker_name VARCHAR(100) PRIMARY KEY,
    state VARCHAR(20) NOT NULL,
    failure_count INTEGER DEFAULT 0,
    last_failure_time TIMESTAMP,
    last_success_time TIMESTAMP,
    next_attempt_time TIMESTAMP,
    state_changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    configuration JSONB NOT NULL,
    metrics JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_circuit_breaker_states_state ON circuit_breaker_states(state);
CREATE INDEX idx_circuit_breaker_states_next_attempt ON circuit_breaker_states(next_attempt_time);
CREATE INDEX idx_circuit_breaker_states_updated_at ON circuit_breaker_states(updated_at);
```

---

## Cross-Database Relationships

### Foreign Key Considerations
Since each microservice has its own database, traditional foreign key constraints cannot be used across databases. Instead, the system uses:

1. **Referential Integrity through Application Logic**
2. **Event-Driven Consistency through SAGA patterns**
3. **Data Consistency Validation through service calls**

### Data Synchronization Events
```sql
-- Event triggers for cross-service data consistency
CREATE TABLE data_sync_events (
    event_id SERIAL PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    source_service VARCHAR(100) NOT NULL,
    target_service VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(255) NOT NULL,
    event_data JSONB NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 5,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    error_message TEXT
);
```

---

## Database Performance Optimization

### Connection Pool Configuration
```yaml
# HikariCP Configuration per Service
customer-service:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      max-lifetime: 1800000
      connection-timeout: 20000
      leak-detection-threshold: 60000

loan-service:
  datasource:
    hikari:
      maximum-pool-size: 25
      minimum-idle: 8
      idle-timeout: 300000
      max-lifetime: 1800000

payment-service:
  datasource:
    hikari:
      maximum-pool-size: 30
      minimum-idle: 10
      idle-timeout: 300000
      max-lifetime: 1800000

gateway-service:
  datasource:
    hikari:
      maximum-pool-size: 15
      minimum-idle: 3
      idle-timeout: 300000
      max-lifetime: 1800000
```

### Database Monitoring Views
```sql
-- Performance monitoring view for each database
CREATE VIEW database_performance_metrics AS
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size,
    pg_stat_get_tuples_returned(c.oid) as tuples_returned,
    pg_stat_get_tuples_fetched(c.oid) as tuples_fetched,
    pg_stat_get_tuples_inserted(c.oid) as tuples_inserted,
    pg_stat_get_tuples_updated(c.oid) as tuples_updated,
    pg_stat_get_tuples_deleted(c.oid) as tuples_deleted
FROM pg_tables t
JOIN pg_class c ON c.relname = t.tablename
WHERE schemaname IN ('customer_db', 'loan_db', 'payment_db', 'banking_gateway');
```

### Backup and Recovery Strategy
```sql
-- Backup configuration for each schema
-- Daily incremental backups with point-in-time recovery
-- Weekly full backups with 6-month retention
-- Cross-region backup replication for disaster recovery

-- Example backup command structure:
-- pg_dump -h localhost -p 5432 -U postgres -n customer_db --format=custom --file=customer_db_backup.dump
-- pg_dump -h localhost -p 5432 -U postgres -n loan_db --format=custom --file=loan_db_backup.dump
-- pg_dump -h localhost -p 5432 -U postgres -n payment_db --format=custom --file=payment_db_backup.dump
-- pg_dump -h localhost -p 5432 -U postgres -n banking_gateway --format=custom --file=gateway_backup.dump
```

This database schema specification provides complete isolation between microservices while maintaining data integrity through application-level consistency patterns and comprehensive audit trails for regulatory compliance.