-- Loan Service Database Schema
CREATE SCHEMA IF NOT EXISTS loan_service;

-- Loans table
CREATE TABLE loan_service.loans (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    principal_amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    interest_rate DECIMAL(5, 4) NOT NULL,
    term_months INTEGER NOT NULL,
    monthly_payment DECIMAL(19, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    application_date DATE NOT NULL,
    approval_date DATE,
    disbursement_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    
    CONSTRAINT chk_loan_status CHECK (status IN ('PENDING', 'APPROVED', 'ACTIVE', 'COMPLETED', 'REJECTED', 'DEFAULTED')),
    CONSTRAINT chk_term_months CHECK (term_months BETWEEN 1 AND 60),
    CONSTRAINT chk_interest_rate CHECK (interest_rate BETWEEN 0.01 AND 0.50)
);

-- Indexes for performance
CREATE INDEX idx_loan_customer_id ON loan_service.loans(customer_id);
CREATE INDEX idx_loan_status ON loan_service.loans(status);
CREATE INDEX idx_loan_application_date ON loan_service.loans(application_date);

-- Customer cache table (for data independence)
CREATE TABLE loan_service.customer_cache (
    customer_id BIGINT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    monthly_income DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(50) NOT NULL,
    last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Event outbox table for reliable event publishing
CREATE TABLE loan_service.event_outbox (
    id BIGSERIAL PRIMARY KEY,
    aggregate_id BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_data JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,
    
    INDEX idx_outbox_unpublished (published_at) WHERE published_at IS NULL
);