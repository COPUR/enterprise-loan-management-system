-- UAT Database Initialization Script
-- Enterprise Loan Management System - User Acceptance Testing

-- Create database and user with proper security
\echo 'Initializing UAT database with security configurations...'

-- Create schemas for domain separation
CREATE SCHEMA IF NOT EXISTS customer_management;
CREATE SCHEMA IF NOT EXISTS loan_origination;
CREATE SCHEMA IF NOT EXISTS payment_processing;
CREATE SCHEMA IF NOT EXISTS compliance_audit;
CREATE SCHEMA IF NOT EXISTS security_monitoring;

-- Grant permissions to banking user
GRANT USAGE ON SCHEMA customer_management TO banking_user;
GRANT USAGE ON SCHEMA loan_origination TO banking_user;
GRANT USAGE ON SCHEMA payment_processing TO banking_user;
GRANT USAGE ON SCHEMA compliance_audit TO banking_user;
GRANT USAGE ON SCHEMA security_monitoring TO banking_user;

GRANT CREATE ON SCHEMA customer_management TO banking_user;
GRANT CREATE ON SCHEMA loan_origination TO banking_user;
GRANT CREATE ON SCHEMA payment_processing TO banking_user;
GRANT CREATE ON SCHEMA compliance_audit TO banking_user;
GRANT CREATE ON SCHEMA security_monitoring TO banking_user;

-- Create customers table in customer_management schema
CREATE TABLE IF NOT EXISTS customer_management.customers (
    id SERIAL PRIMARY KEY,
    customer_number VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    credit_score INTEGER CHECK (credit_score >= 300 AND credit_score <= 850),
    annual_income DECIMAL(15,2),
    employment_status VARCHAR(20),
    city VARCHAR(50),
    state VARCHAR(10),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create loans table in loan_origination schema
CREATE TABLE IF NOT EXISTS loan_origination.loans (
    id SERIAL PRIMARY KEY,
    loan_number VARCHAR(20) UNIQUE NOT NULL,
    customer_id INTEGER NOT NULL,
    principal_amount DECIMAL(15,2) NOT NULL CHECK (principal_amount >= 1000 AND principal_amount <= 500000),
    installment_count INTEGER NOT NULL CHECK (installment_count IN (6, 9, 12, 24)),
    monthly_interest_rate DECIMAL(5,4) NOT NULL CHECK (monthly_interest_rate >= 0.001 AND monthly_interest_rate <= 0.005),
    monthly_payment_amount DECIMAL(15,2) NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    outstanding_balance DECIMAL(15,2) NOT NULL,
    loan_status VARCHAR(20) DEFAULT 'ACTIVE',
    disbursement_date TIMESTAMP,
    maturity_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customer_management.customers(id)
);

-- Create payments table in payment_processing schema
CREATE TABLE IF NOT EXISTS payment_processing.payments (
    id SERIAL PRIMARY KEY,
    payment_number VARCHAR(20) UNIQUE NOT NULL,
    loan_id INTEGER NOT NULL,
    customer_id INTEGER NOT NULL,
    payment_type VARCHAR(20) NOT NULL,
    scheduled_amount DECIMAL(15,2) NOT NULL,
    actual_amount DECIMAL(15,2),
    principal_amount DECIMAL(15,2),
    interest_amount DECIMAL(15,2),
    penalty_amount DECIMAL(15,2) DEFAULT 0,
    scheduled_date DATE NOT NULL,
    actual_payment_date TIMESTAMP,
    payment_status VARCHAR(20) DEFAULT 'PENDING',
    payment_method VARCHAR(20),
    transaction_reference VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (loan_id) REFERENCES loan_origination.loans(id),
    FOREIGN KEY (customer_id) REFERENCES customer_management.customers(id)
);

-- Create audit trail table for compliance
CREATE TABLE IF NOT EXISTS compliance_audit.audit_trail (
    id SERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id INTEGER NOT NULL,
    action VARCHAR(20) NOT NULL,
    old_values JSONB,
    new_values JSONB,
    user_id VARCHAR(50),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address INET,
    user_agent TEXT
);

-- Create security events table
CREATE TABLE IF NOT EXISTS security_monitoring.security_events (
    id SERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    description TEXT,
    source_ip INET,
    user_id VARCHAR(50),
    additional_data JSONB,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert UAT test data
\echo 'Inserting UAT test data...'

-- Insert test customers
INSERT INTO customer_management.customers (customer_number, name, email, credit_score, annual_income, employment_status, city, state) VALUES
('UAT001', 'Alice Johnson', 'alice.johnson@uattest.com', 780, 95000.00, 'EMPLOYED', 'Boston', 'MA'),
('UAT002', 'Bob Smith', 'bob.smith@uattest.com', 720, 75000.00, 'EMPLOYED', 'Austin', 'TX'),
('UAT003', 'Carol Brown', 'carol.brown@uattest.com', 650, 68000.00, 'EMPLOYED', 'Seattle', 'WA'),
('UAT004', 'David Wilson', 'david.wilson@uattest.com', 800, 120000.00, 'EMPLOYED', 'Denver', 'CO'),
('UAT005', 'Eva Martinez', 'eva.martinez@uattest.com', 690, 82000.00, 'EMPLOYED', 'Phoenix', 'AZ')
ON CONFLICT (customer_number) DO NOTHING;

-- Insert test loans
INSERT INTO loan_origination.loans (loan_number, customer_id, principal_amount, installment_count, monthly_interest_rate, monthly_payment_amount, total_amount, outstanding_balance, disbursement_date, maturity_date) VALUES
('UAT-LOAN-001', 1, 75000.00, 12, 0.0015, 6562.50, 78750.00, 75000.00, CURRENT_TIMESTAMP, CURRENT_DATE + INTERVAL '12 months'),
('UAT-LOAN-002', 2, 50000.00, 24, 0.0020, 2291.67, 55000.00, 50000.00, CURRENT_TIMESTAMP, CURRENT_DATE + INTERVAL '24 months'),
('UAT-LOAN-003', 3, 35000.00, 18, 0.0018, 2083.33, 38150.00, 35000.00, CURRENT_TIMESTAMP, CURRENT_DATE + INTERVAL '18 months'),
('UAT-LOAN-004', 4, 100000.00, 24, 0.0012, 4583.33, 110000.00, 100000.00, CURRENT_TIMESTAMP, CURRENT_DATE + INTERVAL '24 months'),
('UAT-LOAN-005', 5, 60000.00, 12, 0.0025, 5250.00, 63000.00, 60000.00, CURRENT_TIMESTAMP, CURRENT_DATE + INTERVAL '12 months')
ON CONFLICT (loan_number) DO NOTHING;

-- Insert test payments
INSERT INTO payment_processing.payments (payment_number, loan_id, customer_id, payment_type, scheduled_amount, actual_amount, principal_amount, interest_amount, scheduled_date, actual_payment_date, payment_status, payment_method, transaction_reference) VALUES
('UAT-PAY-001', 1, 1, 'REGULAR', 6562.50, 6562.50, 6450.00, 112.50, CURRENT_DATE - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '30 days', 'COMPLETED', 'ACH', 'UAT-TXN-001'),
('UAT-PAY-002', 2, 2, 'REGULAR', 2291.67, 2291.67, 2191.67, 100.00, CURRENT_DATE - INTERVAL '20 days', CURRENT_TIMESTAMP - INTERVAL '20 days', 'COMPLETED', 'WIRE', 'UAT-TXN-002'),
('UAT-PAY-003', 3, 3, 'REGULAR', 2083.33, 0.00, 0.00, 0.00, CURRENT_DATE - INTERVAL '10 days', NULL, 'OVERDUE', 'BANK_TRANSFER', NULL),
('UAT-PAY-004', 4, 4, 'EARLY', 4583.33, 4583.33, 4463.33, 120.00, CURRENT_DATE + INTERVAL '10 days', CURRENT_TIMESTAMP - INTERVAL '5 days', 'COMPLETED', 'ACH', 'UAT-TXN-004'),
('UAT-PAY-005', 5, 5, 'REGULAR', 5250.00, 0.00, 0.00, 0.00, CURRENT_DATE + INTERVAL '5 days', NULL, 'PENDING', 'BANK_TRANSFER', NULL)
ON CONFLICT (payment_number) DO NOTHING;

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_customers_email ON customer_management.customers(email);
CREATE INDEX IF NOT EXISTS idx_customers_status ON customer_management.customers(status);
CREATE INDEX IF NOT EXISTS idx_loans_customer_id ON loan_origination.loans(customer_id);
CREATE INDEX IF NOT EXISTS idx_loans_status ON loan_origination.loans(loan_status);
CREATE INDEX IF NOT EXISTS idx_payments_loan_id ON payment_processing.payments(loan_id);
CREATE INDEX IF NOT EXISTS idx_payments_customer_id ON payment_processing.payments(customer_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payment_processing.payments(payment_status);
CREATE INDEX IF NOT EXISTS idx_audit_entity ON compliance_audit.audit_trail(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_security_events_type ON security_monitoring.security_events(event_type);
CREATE INDEX IF NOT EXISTS idx_security_events_timestamp ON security_monitoring.security_events(timestamp);

-- Create views for UAT testing
CREATE OR REPLACE VIEW customer_management.customer_summary AS
SELECT 
    c.id,
    c.customer_number,
    c.name,
    c.email,
    c.credit_score,
    c.status,
    COUNT(l.id) as total_loans,
    COALESCE(SUM(l.outstanding_balance), 0) as total_outstanding,
    COALESCE(AVG(l.monthly_interest_rate), 0) as avg_interest_rate
FROM customer_management.customers c
LEFT JOIN loan_origination.loans l ON c.id = l.customer_id
GROUP BY c.id, c.customer_number, c.name, c.email, c.credit_score, c.status;

-- Grant permissions on tables and views
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA customer_management TO banking_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA loan_origination TO banking_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA payment_processing TO banking_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA compliance_audit TO banking_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA security_monitoring TO banking_user;

GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA customer_management TO banking_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA loan_origination TO banking_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA payment_processing TO banking_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA compliance_audit TO banking_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA security_monitoring TO banking_user;

GRANT SELECT ON customer_management.customer_summary TO banking_user;

-- Enable row level security for compliance
ALTER TABLE compliance_audit.audit_trail ENABLE ROW LEVEL SECURITY;
ALTER TABLE security_monitoring.security_events ENABLE ROW LEVEL SECURITY;

-- Create security policies
CREATE POLICY audit_trail_policy ON compliance_audit.audit_trail
    FOR ALL TO banking_user
    USING (true);

CREATE POLICY security_events_policy ON security_monitoring.security_events
    FOR ALL TO banking_user
    USING (true);

\echo 'UAT database initialization completed successfully!'
\echo 'Schemas created: customer_management, loan_origination, payment_processing, compliance_audit, security_monitoring'
\echo 'Test data inserted: 5 customers, 5 loans, 5 payments'
\echo 'Indexes and views created for optimal performance'
\echo 'Row-level security enabled for audit and security tables'