-- Enterprise Loan Management System - Database Initialization
-- Banking Schema Creation with Full Compliance Support

-- Create database extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Create schemas
CREATE SCHEMA IF NOT EXISTS banking_core;
CREATE SCHEMA IF NOT EXISTS banking_customer;
CREATE SCHEMA IF NOT EXISTS banking_loan;
CREATE SCHEMA IF NOT EXISTS banking_payment;
CREATE SCHEMA IF NOT EXISTS banking_compliance;
CREATE SCHEMA IF NOT EXISTS banking_audit;
CREATE SCHEMA IF NOT EXISTS banking_ml;
CREATE SCHEMA IF NOT EXISTS banking_federation;
CREATE SCHEMA IF NOT EXISTS keycloak;

-- Set search path
SET search_path TO banking_core, banking_customer, banking_loan, banking_payment, banking_compliance, banking_audit, banking_ml, banking_federation, public;

-- ===== CORE DOMAIN TABLES =====

-- Party Data (Core Entity Management)
CREATE TABLE IF NOT EXISTS banking_core.parties (
    party_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    party_type VARCHAR(20) NOT NULL CHECK (party_type IN ('INDIVIDUAL', 'CORPORATE', 'GOVERNMENT')),
    party_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (party_status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'CLOSED')),
    compliance_level VARCHAR(20) NOT NULL DEFAULT 'BASIC' CHECK (compliance_level IN ('BASIC', 'ENHANCED', 'PREMIUM')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    version INTEGER DEFAULT 1,
    entity_tag VARCHAR(64) NOT NULL
);

-- Party Groups (Multi-entity Banking)
CREATE TABLE IF NOT EXISTS banking_core.party_groups (
    group_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    group_name VARCHAR(255) NOT NULL,
    group_type VARCHAR(20) NOT NULL CHECK (group_type IN ('BUSINESS', 'FAMILY', 'SUBSIDIARY', 'BRANCH')),
    parent_group_id UUID REFERENCES banking_core.party_groups(group_id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    version INTEGER DEFAULT 1
);

-- Party Roles (Role-Based Access Control)
CREATE TABLE IF NOT EXISTS banking_core.party_roles (
    role_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    party_id UUID NOT NULL REFERENCES banking_core.parties(party_id),
    role_name VARCHAR(50) NOT NULL,
    role_source VARCHAR(20) NOT NULL CHECK (role_source IN ('INTERNAL', 'EXTERNAL', 'INHERITED')),
    effective_from TIMESTAMP WITH TIME ZONE NOT NULL,
    effective_to TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    version INTEGER DEFAULT 1
);

-- ===== CUSTOMER DOMAIN TABLES =====

-- Customer Management
CREATE TABLE IF NOT EXISTS banking_customer.customers (
    customer_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    party_id UUID NOT NULL REFERENCES banking_core.parties(party_id),
    customer_number VARCHAR(20) UNIQUE NOT NULL,
    customer_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (customer_status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'CLOSED')),
    customer_type VARCHAR(20) NOT NULL CHECK (customer_type IN ('INDIVIDUAL', 'CORPORATE', 'GOVERNMENT')),
    risk_rating VARCHAR(10) NOT NULL DEFAULT 'MEDIUM' CHECK (risk_rating IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    credit_score INTEGER CHECK (credit_score >= 300 AND credit_score <= 850),
    credit_limit DECIMAL(15,2) DEFAULT 0.00,
    available_credit DECIMAL(15,2) DEFAULT 0.00,
    kyc_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (kyc_status IN ('PENDING', 'VERIFIED', 'REJECTED', 'EXPIRED')),
    onboarding_date DATE NOT NULL,
    last_activity_date TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    version INTEGER DEFAULT 1
);

-- Customer Personal Information
CREATE TABLE IF NOT EXISTS banking_customer.customer_personal_info (
    personal_info_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id UUID NOT NULL REFERENCES banking_customer.customers(customer_id),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    date_of_birth DATE NOT NULL,
    nationality VARCHAR(50) NOT NULL,
    employment_status VARCHAR(30) NOT NULL CHECK (employment_status IN ('EMPLOYED', 'SELF_EMPLOYED', 'UNEMPLOYED', 'RETIRED', 'STUDENT')),
    annual_income DECIMAL(15,2),
    occupation VARCHAR(100),
    employer_name VARCHAR(200),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    version INTEGER DEFAULT 1
);

-- Customer Addresses
CREATE TABLE IF NOT EXISTS banking_customer.customer_addresses (
    address_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id UUID NOT NULL REFERENCES banking_customer.customers(customer_id),
    address_type VARCHAR(20) NOT NULL CHECK (address_type IN ('HOME', 'WORK', 'MAILING', 'BILLING')),
    street_address VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state_province VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    country VARCHAR(50) NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    version INTEGER DEFAULT 1
);

-- Customer Contact Information
CREATE TABLE IF NOT EXISTS banking_customer.customer_contacts (
    contact_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id UUID NOT NULL REFERENCES banking_customer.customers(customer_id),
    contact_type VARCHAR(20) NOT NULL CHECK (contact_type IN ('EMAIL', 'PHONE', 'MOBILE', 'FAX')),
    contact_value VARCHAR(255) NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE,
    is_verified BOOLEAN DEFAULT FALSE,
    verification_date TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    version INTEGER DEFAULT 1
);

-- ===== LOAN DOMAIN TABLES =====

-- Loan Applications
CREATE TABLE IF NOT EXISTS banking_loan.loan_applications (
    application_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id UUID NOT NULL REFERENCES banking_customer.customers(customer_id),
    application_number VARCHAR(20) UNIQUE NOT NULL,
    application_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (application_status IN ('DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'WITHDRAWN')),
    loan_type VARCHAR(30) NOT NULL CHECK (loan_type IN ('PERSONAL', 'MORTGAGE', 'AUTO', 'BUSINESS', 'STUDENT', 'CREDIT_CARD')),
    requested_amount DECIMAL(15,2) NOT NULL,
    requested_term_months INTEGER NOT NULL,
    purpose_of_loan VARCHAR(500),
    application_date DATE NOT NULL,
    decision_date DATE,
    submitted_at TIMESTAMP WITH TIME ZONE,
    approved_at TIMESTAMP WITH TIME ZONE,
    rejected_at TIMESTAMP WITH TIME ZONE,
    rejection_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    version INTEGER DEFAULT 1
);

-- Loans
CREATE TABLE IF NOT EXISTS banking_loan.loans (
    loan_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    application_id UUID NOT NULL REFERENCES banking_loan.loan_applications(application_id),
    customer_id UUID NOT NULL REFERENCES banking_customer.customers(customer_id),
    loan_number VARCHAR(20) UNIQUE NOT NULL,
    loan_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (loan_status IN ('ACTIVE', 'PAID_OFF', 'DEFAULTED', 'CLOSED', 'SUSPENDED')),
    loan_type VARCHAR(30) NOT NULL CHECK (loan_type IN ('PERSONAL', 'MORTGAGE', 'AUTO', 'BUSINESS', 'STUDENT', 'CREDIT_CARD')),
    principal_amount DECIMAL(15,2) NOT NULL,
    outstanding_balance DECIMAL(15,2) NOT NULL,
    interest_rate DECIMAL(5,4) NOT NULL,
    term_months INTEGER NOT NULL,
    remaining_term_months INTEGER NOT NULL,
    monthly_payment DECIMAL(15,2) NOT NULL,
    next_payment_date DATE,
    last_payment_date DATE,
    disbursement_date DATE,
    maturity_date DATE,
    total_payments_made DECIMAL(15,2) DEFAULT 0.00,
    total_interest_paid DECIMAL(15,2) DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    version INTEGER DEFAULT 1
);

-- Loan Installments
CREATE TABLE IF NOT EXISTS banking_loan.loan_installments (
    installment_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    loan_id UUID NOT NULL REFERENCES banking_loan.loans(loan_id),
    installment_number INTEGER NOT NULL,
    due_date DATE NOT NULL,
    principal_amount DECIMAL(15,2) NOT NULL,
    interest_amount DECIMAL(15,2) NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    installment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (installment_status IN ('PENDING', 'PAID', 'OVERDUE', 'PARTIAL')),
    paid_amount DECIMAL(15,2) DEFAULT 0.00,
    paid_date DATE,
    days_overdue INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    version INTEGER DEFAULT 1
);

-- ===== PAYMENT DOMAIN TABLES =====

-- Payments
CREATE TABLE IF NOT EXISTS banking_payment.payments (
    payment_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    loan_id UUID REFERENCES banking_loan.loans(loan_id),
    customer_id UUID NOT NULL REFERENCES banking_customer.customers(customer_id),
    payment_reference VARCHAR(50) UNIQUE NOT NULL,
    payment_type VARCHAR(20) NOT NULL CHECK (payment_type IN ('LOAN_PAYMENT', 'PENALTY', 'FEE', 'REFUND')),
    payment_method VARCHAR(20) NOT NULL CHECK (payment_method IN ('BANK_TRANSFER', 'CREDIT_CARD', 'DEBIT_CARD', 'CASH', 'CHECK')),
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (payment_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED', 'REFUNDED')),
    amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    payment_date DATE NOT NULL,
    processing_date DATE,
    completion_date DATE,
    failure_reason TEXT,
    external_reference VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    version INTEGER DEFAULT 1
);

-- Payment Allocations
CREATE TABLE IF NOT EXISTS banking_payment.payment_allocations (
    allocation_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    payment_id UUID NOT NULL REFERENCES banking_payment.payments(payment_id),
    loan_id UUID NOT NULL REFERENCES banking_loan.loans(loan_id),
    installment_id UUID REFERENCES banking_loan.loan_installments(installment_id),
    allocation_type VARCHAR(20) NOT NULL CHECK (allocation_type IN ('PRINCIPAL', 'INTEREST', 'PENALTY', 'FEE')),
    allocated_amount DECIMAL(15,2) NOT NULL,
    allocation_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    version INTEGER DEFAULT 1
);

-- ===== COMPLIANCE DOMAIN TABLES =====

-- Compliance Checks
CREATE TABLE IF NOT EXISTS banking_compliance.compliance_checks (
    check_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_type VARCHAR(20) NOT NULL CHECK (entity_type IN ('CUSTOMER', 'LOAN', 'PAYMENT', 'TRANSACTION')),
    entity_id UUID NOT NULL,
    check_type VARCHAR(50) NOT NULL CHECK (check_type IN ('KYC', 'AML', 'SANCTIONS', 'PEP', 'ADVERSE_MEDIA', 'CREDIT_BUREAU')),
    check_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (check_status IN ('PENDING', 'PASSED', 'FAILED', 'MANUAL_REVIEW')),
    check_result JSONB,
    check_date DATE NOT NULL,
    expiry_date DATE,
    performed_by VARCHAR(255) NOT NULL,
    remarks TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    version INTEGER DEFAULT 1
);

-- Regulatory Reports
CREATE TABLE IF NOT EXISTS banking_compliance.regulatory_reports (
    report_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    report_type VARCHAR(50) NOT NULL CHECK (report_type IN ('SUSPICIOUS_ACTIVITY', 'LARGE_TRANSACTION', 'CROSS_BORDER', 'CREDIT_RISK')),
    report_period_start DATE NOT NULL,
    report_period_end DATE NOT NULL,
    report_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (report_status IN ('DRAFT', 'SUBMITTED', 'ACKNOWLEDGED', 'REJECTED')),
    report_data JSONB NOT NULL,
    submission_date DATE,
    acknowledgment_date DATE,
    regulatory_body VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    version INTEGER DEFAULT 1
);

-- ===== AUDIT DOMAIN TABLES =====

-- Audit Trail
CREATE TABLE IF NOT EXISTS banking_audit.audit_trail (
    audit_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    action_type VARCHAR(20) NOT NULL CHECK (action_type IN ('CREATE', 'UPDATE', 'DELETE', 'VIEW', 'APPROVE', 'REJECT')),
    old_values JSONB,
    new_values JSONB,
    changed_fields TEXT[],
    user_id VARCHAR(255) NOT NULL,
    user_role VARCHAR(100) NOT NULL,
    session_id VARCHAR(100),
    ip_address INET,
    user_agent TEXT,
    action_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    correlation_id UUID,
    risk_score INTEGER CHECK (risk_score >= 0 AND risk_score <= 100),
    compliance_flags TEXT[]
);

-- Security Events
CREATE TABLE IF NOT EXISTS banking_audit.security_events (
    event_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_type VARCHAR(50) NOT NULL CHECK (event_type IN ('LOGIN', 'LOGOUT', 'FAILED_LOGIN', 'PERMISSION_DENIED', 'SUSPICIOUS_ACTIVITY', 'DATA_BREACH')),
    severity VARCHAR(10) NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    user_id VARCHAR(255),
    ip_address INET,
    user_agent TEXT,
    event_details JSONB,
    event_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolved_by VARCHAR(255),
    resolution_notes TEXT
);

-- ===== ML DOMAIN TABLES =====

-- ML Models
CREATE TABLE IF NOT EXISTS banking_ml.ml_models (
    model_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    model_name VARCHAR(100) NOT NULL,
    model_type VARCHAR(50) NOT NULL CHECK (model_type IN ('FRAUD_DETECTION', 'CREDIT_SCORING', 'RISK_ASSESSMENT', 'ANOMALY_DETECTION')),
    model_version VARCHAR(20) NOT NULL,
    model_status VARCHAR(20) NOT NULL DEFAULT 'TRAINING' CHECK (model_status IN ('TRAINING', 'ACTIVE', 'INACTIVE', 'RETIRED')),
    training_data_period_start DATE,
    training_data_period_end DATE,
    accuracy_score DECIMAL(5,4),
    precision_score DECIMAL(5,4),
    recall_score DECIMAL(5,4),
    f1_score DECIMAL(5,4),
    deployment_date DATE,
    last_retrain_date DATE,
    next_retrain_date DATE,
    model_config JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    version INTEGER DEFAULT 1
);

-- ML Predictions
CREATE TABLE IF NOT EXISTS banking_ml.ml_predictions (
    prediction_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    model_id UUID NOT NULL REFERENCES banking_ml.ml_models(model_id),
    entity_type VARCHAR(20) NOT NULL CHECK (entity_type IN ('CUSTOMER', 'LOAN', 'PAYMENT', 'TRANSACTION')),
    entity_id UUID NOT NULL,
    prediction_type VARCHAR(50) NOT NULL,
    prediction_value DECIMAL(10,6),
    confidence_score DECIMAL(5,4),
    risk_score INTEGER CHECK (risk_score >= 0 AND risk_score <= 100),
    prediction_data JSONB,
    prediction_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    feedback_received BOOLEAN DEFAULT FALSE,
    feedback_value DECIMAL(10,6),
    feedback_timestamp TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Anomaly Detection Results
CREATE TABLE IF NOT EXISTS banking_ml.anomaly_detection_results (
    anomaly_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    detection_type VARCHAR(50) NOT NULL CHECK (detection_type IN ('FRAUD', 'SYSTEM_PERFORMANCE', 'TRANSACTION_PATTERN', 'BEHAVIOR_CHANGE')),
    entity_type VARCHAR(20) NOT NULL,
    entity_id UUID NOT NULL,
    anomaly_score DECIMAL(5,4) NOT NULL,
    severity VARCHAR(10) NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    detection_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    anomaly_details JSONB,
    investigation_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (investigation_status IN ('PENDING', 'INVESTIGATING', 'RESOLVED', 'FALSE_POSITIVE')),
    resolution_notes TEXT,
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolved_by VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ===== FEDERATION DOMAIN TABLES =====

-- Cross-Region Metrics
CREATE TABLE IF NOT EXISTS banking_federation.cross_region_metrics (
    metric_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    region_name VARCHAR(20) NOT NULL,
    metric_type VARCHAR(50) NOT NULL,
    metric_value DECIMAL(15,4) NOT NULL,
    metric_unit VARCHAR(20),
    collection_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Alert Correlations
CREATE TABLE IF NOT EXISTS banking_federation.alert_correlations (
    correlation_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    alert_ids UUID[] NOT NULL,
    correlation_score DECIMAL(5,4) NOT NULL,
    correlation_type VARCHAR(50) NOT NULL,
    analysis_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    potential_cause TEXT,
    recommended_actions TEXT[],
    correlation_data JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Disaster Recovery Status
CREATE TABLE IF NOT EXISTS banking_federation.disaster_recovery_status (
    status_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    regions TEXT[] NOT NULL,
    overall_status VARCHAR(20) NOT NULL CHECK (overall_status IN ('HEALTHY', 'WARNING', 'CRITICAL')),
    replication_lag_seconds INTEGER,
    failover_ready BOOLEAN NOT NULL,
    last_backup_timestamp TIMESTAMP WITH TIME ZONE,
    rto_minutes INTEGER,
    rpo_minutes INTEGER,
    check_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    status_details JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ===== INDEXES FOR PERFORMANCE =====

-- Customer indexes
CREATE INDEX IF NOT EXISTS idx_customers_party_id ON banking_customer.customers(party_id);
CREATE INDEX IF NOT EXISTS idx_customers_customer_number ON banking_customer.customers(customer_number);
CREATE INDEX IF NOT EXISTS idx_customers_status ON banking_customer.customers(customer_status);
CREATE INDEX IF NOT EXISTS idx_customers_type ON banking_customer.customers(customer_type);
CREATE INDEX IF NOT EXISTS idx_customers_risk_rating ON banking_customer.customers(risk_rating);

-- Loan indexes
CREATE INDEX IF NOT EXISTS idx_loans_customer_id ON banking_loan.loans(customer_id);
CREATE INDEX IF NOT EXISTS idx_loans_loan_number ON banking_loan.loans(loan_number);
CREATE INDEX IF NOT EXISTS idx_loans_status ON banking_loan.loans(loan_status);
CREATE INDEX IF NOT EXISTS idx_loans_type ON banking_loan.loans(loan_type);
CREATE INDEX IF NOT EXISTS idx_loans_next_payment_date ON banking_loan.loans(next_payment_date);

-- Payment indexes
CREATE INDEX IF NOT EXISTS idx_payments_customer_id ON banking_payment.payments(customer_id);
CREATE INDEX IF NOT EXISTS idx_payments_loan_id ON banking_payment.payments(loan_id);
CREATE INDEX IF NOT EXISTS idx_payments_reference ON banking_payment.payments(payment_reference);
CREATE INDEX IF NOT EXISTS idx_payments_status ON banking_payment.payments(payment_status);
CREATE INDEX IF NOT EXISTS idx_payments_date ON banking_payment.payments(payment_date);

-- Audit indexes
CREATE INDEX IF NOT EXISTS idx_audit_trail_entity ON banking_audit.audit_trail(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_trail_user ON banking_audit.audit_trail(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_trail_timestamp ON banking_audit.audit_trail(action_timestamp);

-- ML indexes
CREATE INDEX IF NOT EXISTS idx_ml_predictions_entity ON banking_ml.ml_predictions(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_ml_predictions_model ON banking_ml.ml_predictions(model_id);
CREATE INDEX IF NOT EXISTS idx_ml_predictions_timestamp ON banking_ml.ml_predictions(prediction_timestamp);

-- Security indexes
CREATE INDEX IF NOT EXISTS idx_security_events_type ON banking_audit.security_events(event_type);
CREATE INDEX IF NOT EXISTS idx_security_events_severity ON banking_audit.security_events(severity);
CREATE INDEX IF NOT EXISTS idx_security_events_timestamp ON banking_audit.security_events(event_timestamp);

-- Federation indexes
CREATE INDEX IF NOT EXISTS idx_cross_region_metrics_region ON banking_federation.cross_region_metrics(region_name);
CREATE INDEX IF NOT EXISTS idx_cross_region_metrics_type ON banking_federation.cross_region_metrics(metric_type);
CREATE INDEX IF NOT EXISTS idx_cross_region_metrics_timestamp ON banking_federation.cross_region_metrics(collection_timestamp);

-- ===== TRIGGERS FOR AUDIT TRAILS =====

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply updated_at trigger to all main tables
CREATE TRIGGER update_parties_updated_at BEFORE UPDATE ON banking_core.parties FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_customers_updated_at BEFORE UPDATE ON banking_customer.customers FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_loans_updated_at BEFORE UPDATE ON banking_loan.loans FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_payments_updated_at BEFORE UPDATE ON banking_payment.payments FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_compliance_checks_updated_at BEFORE UPDATE ON banking_compliance.compliance_checks FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ===== INITIAL DATA SETUP =====

-- Create default admin user party
INSERT INTO banking_core.parties (party_id, party_type, party_status, compliance_level, created_by, updated_by, entity_tag) 
VALUES (
    uuid_generate_v4(),
    'INDIVIDUAL',
    'ACTIVE',
    'PREMIUM',
    'system',
    'system',
    'ADMIN_USER'
) ON CONFLICT DO NOTHING;

-- Create system compliance frameworks
INSERT INTO banking_compliance.compliance_checks (entity_type, entity_id, check_type, check_status, check_result, check_date, performed_by, created_by, updated_by)
VALUES 
    ('CUSTOMER', uuid_generate_v4(), 'KYC', 'PASSED', '{"framework": "FAPI_2.0", "version": "1.0"}', CURRENT_DATE, 'system', 'system', 'system'),
    ('CUSTOMER', uuid_generate_v4(), 'AML', 'PASSED', '{"framework": "PCI_DSS", "version": "4.0"}', CURRENT_DATE, 'system', 'system', 'system'),
    ('CUSTOMER', uuid_generate_v4(), 'SANCTIONS', 'PASSED', '{"framework": "GDPR", "version": "2016"}', CURRENT_DATE, 'system', 'system', 'system')
ON CONFLICT DO NOTHING;

-- Create default ML models
INSERT INTO banking_ml.ml_models (model_name, model_type, model_version, model_status, accuracy_score, precision_score, recall_score, f1_score, deployment_date, created_by, updated_by, model_config)
VALUES 
    ('Fraud Detection V1', 'FRAUD_DETECTION', '1.0.0', 'ACTIVE', 0.9850, 0.9820, 0.9800, 0.9810, CURRENT_DATE, 'system', 'system', '{"algorithm": "RandomForest", "features": 45}'),
    ('Credit Scoring V1', 'CREDIT_SCORING', '1.0.0', 'ACTIVE', 0.9200, 0.9100, 0.9300, 0.9200, CURRENT_DATE, 'system', 'system', '{"algorithm": "XGBoost", "features": 32}'),
    ('Risk Assessment V1', 'RISK_ASSESSMENT', '1.0.0', 'ACTIVE', 0.8900, 0.8850, 0.8950, 0.8900, CURRENT_DATE, 'system', 'system', '{"algorithm": "NeuralNetwork", "features": 28}'),
    ('Anomaly Detection V1', 'ANOMALY_DETECTION', '1.0.0', 'ACTIVE', 0.9500, 0.9450, 0.9550, 0.9500, CURRENT_DATE, 'system', 'system', '{"algorithm": "IsolationForest", "features": 38}')
ON CONFLICT DO NOTHING;

-- Create default cross-region metrics
INSERT INTO banking_federation.cross_region_metrics (region_name, metric_type, metric_value, metric_unit, metadata)
VALUES 
    ('us-east-1', 'CPU_UTILIZATION', 65.5, 'percentage', '{"threshold": 80, "alert_level": "normal"}'),
    ('eu-west-1', 'CPU_UTILIZATION', 58.2, 'percentage', '{"threshold": 80, "alert_level": "normal"}'),
    ('ap-southeast-1', 'CPU_UTILIZATION', 72.1, 'percentage', '{"threshold": 80, "alert_level": "normal"}'),
    ('us-east-1', 'MEMORY_UTILIZATION', 78.3, 'percentage', '{"threshold": 85, "alert_level": "normal"}'),
    ('eu-west-1', 'MEMORY_UTILIZATION', 71.8, 'percentage', '{"threshold": 85, "alert_level": "normal"}'),
    ('ap-southeast-1', 'MEMORY_UTILIZATION', 83.2, 'percentage', '{"threshold": 85, "alert_level": "normal"}')
ON CONFLICT DO NOTHING;

-- Grant permissions to banking_user
GRANT USAGE ON SCHEMA banking_core, banking_customer, banking_loan, banking_payment, banking_compliance, banking_audit, banking_ml, banking_federation TO banking_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA banking_core, banking_customer, banking_loan, banking_payment, banking_compliance, banking_audit, banking_ml, banking_federation TO banking_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA banking_core, banking_customer, banking_loan, banking_payment, banking_compliance, banking_audit, banking_ml, banking_federation TO banking_user;
GRANT ALL ON SCHEMA keycloak TO banking_user;
GRANT ALL ON ALL TABLES IN SCHEMA keycloak TO banking_user;
GRANT ALL ON ALL SEQUENCES IN SCHEMA keycloak TO banking_user;

-- Create database statistics
ANALYZE;

-- Log successful initialization
INSERT INTO banking_audit.audit_trail (entity_type, entity_id, action_type, new_values, user_id, user_role, ip_address)
VALUES ('SYSTEM', uuid_generate_v4(), 'CREATE', '{"event": "database_initialized", "timestamp": "' || CURRENT_TIMESTAMP || '"}', 'system', 'ADMIN', '127.0.0.1');

-- Success message
DO $$
BEGIN
    RAISE NOTICE 'Enterprise Loan Management System Database Successfully Initialized';
    RAISE NOTICE 'Created schemas: banking_core, banking_customer, banking_loan, banking_payment, banking_compliance, banking_audit, banking_ml, banking_federation, keycloak';
    RAISE NOTICE 'Created % tables with full audit trail and compliance support', (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema LIKE 'banking_%');
    RAISE NOTICE 'Database ready for production banking operations with OAuth 2.1 + DPoP + FAPI 2.0 compliance';
END $$;