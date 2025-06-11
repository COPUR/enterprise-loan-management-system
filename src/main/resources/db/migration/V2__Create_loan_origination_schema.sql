-- Loan Origination Bounded Context
CREATE SCHEMA IF NOT EXISTS loan_origination;

CREATE TABLE loan_origination.loan_applications (
    id BIGSERIAL PRIMARY KEY,
    application_number VARCHAR(20) UNIQUE NOT NULL,
    customer_id BIGINT NOT NULL,
    requested_amount DECIMAL(15,2) NOT NULL CHECK (requested_amount >= 1000 AND requested_amount <= 500000),
    loan_purpose VARCHAR(100) NOT NULL,
    installment_count INTEGER NOT NULL CHECK (installment_count IN (6, 9, 12, 24)),
    monthly_interest_rate DECIMAL(5,4) NOT NULL CHECK (monthly_interest_rate >= 0.001 AND monthly_interest_rate <= 0.005),
    application_status VARCHAR(20) DEFAULT 'PENDING' CHECK (application_status IN ('PENDING', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'CANCELLED')),
    risk_assessment_score DECIMAL(5,2),
    debt_to_income_ratio DECIMAL(5,4),
    collateral_value DECIMAL(15,2),
    employment_verification BOOLEAN DEFAULT FALSE,
    income_verification BOOLEAN DEFAULT FALSE,
    credit_check_completed BOOLEAN DEFAULT FALSE,
    approval_date TIMESTAMP WITH TIME ZONE,
    rejection_reason TEXT,
    loan_officer_id BIGINT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version INTEGER DEFAULT 0
);

CREATE INDEX idx_loan_applications_customer_id ON loan_origination.loan_applications(customer_id);
CREATE INDEX idx_loan_applications_status ON loan_origination.loan_applications(application_status);
CREATE INDEX idx_loan_applications_amount ON loan_origination.loan_applications(requested_amount);

CREATE TABLE loan_origination.loans (
    id BIGSERIAL PRIMARY KEY,
    loan_number VARCHAR(20) UNIQUE NOT NULL,
    application_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    principal_amount DECIMAL(15,2) NOT NULL,
    installment_count INTEGER NOT NULL,
    monthly_interest_rate DECIMAL(5,4) NOT NULL,
    monthly_payment_amount DECIMAL(15,2) NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    outstanding_balance DECIMAL(15,2) NOT NULL,
    loan_status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (loan_status IN ('ACTIVE', 'PAID_OFF', 'DEFAULTED', 'RESTRUCTURED')),
    disbursement_date TIMESTAMP WITH TIME ZONE,
    maturity_date DATE,
    next_payment_date DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version INTEGER DEFAULT 0,
    FOREIGN KEY (application_id) REFERENCES loan_origination.loan_applications(id)
);

CREATE INDEX idx_loans_customer_id ON loan_origination.loans(customer_id);
CREATE INDEX idx_loans_status ON loan_origination.loans(loan_status);
CREATE INDEX idx_loans_next_payment_date ON loan_origination.loans(next_payment_date);

-- Loan Events for Event Sourcing
CREATE TABLE loan_origination.loan_events (
    id BIGSERIAL PRIMARY KEY,
    loan_id BIGINT,
    application_id BIGINT,
    event_type VARCHAR(100) NOT NULL,
    event_data JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Risk Assessment Rules
CREATE TABLE loan_origination.risk_assessment_rules (
    id BIGSERIAL PRIMARY KEY,
    rule_name VARCHAR(100) NOT NULL,
    rule_condition JSONB NOT NULL,
    risk_score_impact DECIMAL(5,2) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);