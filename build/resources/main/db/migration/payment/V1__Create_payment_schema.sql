-- Payment Service Database Schema
CREATE SCHEMA IF NOT EXISTS payment_service;

-- Payments table
CREATE TABLE payment_service.payments (
    id BIGSERIAL PRIMARY KEY,
    loan_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    due_date DATE NOT NULL,
    payment_date DATE,
    status VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL,
    penalty_amount DECIMAL(19, 2),
    discount_amount DECIMAL(19, 2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    
    CONSTRAINT chk_payment_status CHECK (status IN ('PENDING', 'COMPLETED', 'OVERDUE', 'CANCELLED', 'FAILED')),
    CONSTRAINT chk_payment_type CHECK (type IN ('REGULAR', 'PARTIAL', 'FULL', 'PENALTY', 'PREPAYMENT'))
);

-- Indexes for performance
CREATE INDEX idx_payment_loan_id ON payment_service.payments(loan_id);
CREATE INDEX idx_payment_status ON payment_service.payments(status);
CREATE INDEX idx_payment_due_date ON payment_service.payments(due_date);
CREATE INDEX idx_payment_overdue ON payment_service.payments(status, due_date) 
    WHERE status = 'PENDING' AND due_date < CURRENT_DATE;

-- Loan cache table (for data independence)
CREATE TABLE payment_service.loan_cache (
    loan_id BIGINT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    principal_amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    monthly_payment DECIMAL(19, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Payment schedule table
CREATE TABLE payment_service.payment_schedule (
    id BIGSERIAL PRIMARY KEY,
    loan_id BIGINT NOT NULL,
    installment_number INTEGER NOT NULL,
    due_date DATE NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(loan_id, installment_number)
);

-- Event outbox table for reliable event publishing
CREATE TABLE payment_service.event_outbox (
    id BIGSERIAL PRIMARY KEY,
    aggregate_id BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_data JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,
    
    INDEX idx_outbox_unpublished (published_at) WHERE published_at IS NULL
);