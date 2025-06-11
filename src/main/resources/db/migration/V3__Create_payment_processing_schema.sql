-- Payment Processing Bounded Context
CREATE SCHEMA IF NOT EXISTS payment_processing;

CREATE TABLE payment_processing.payments (
    id BIGSERIAL PRIMARY KEY,
    payment_number VARCHAR(20) UNIQUE NOT NULL,
    loan_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    payment_type VARCHAR(20) DEFAULT 'REGULAR' CHECK (payment_type IN ('REGULAR', 'EARLY', 'PARTIAL', 'LATE')),
    scheduled_amount DECIMAL(15,2) NOT NULL,
    actual_amount DECIMAL(15,2) NOT NULL,
    principal_amount DECIMAL(15,2) NOT NULL,
    interest_amount DECIMAL(15,2) NOT NULL,
    penalty_amount DECIMAL(15,2) DEFAULT 0,
    scheduled_date DATE NOT NULL,
    actual_payment_date TIMESTAMP WITH TIME ZONE,
    payment_status VARCHAR(20) DEFAULT 'PENDING' CHECK (payment_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED', 'REFUNDED')),
    payment_method VARCHAR(20) CHECK (payment_method IN ('BANK_TRANSFER', 'ACH', 'WIRE', 'CHECK', 'CASH')),
    transaction_reference VARCHAR(100),
    processor_reference VARCHAR(100),
    failure_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version INTEGER DEFAULT 0
);

CREATE INDEX idx_payments_loan_id ON payment_processing.payments(loan_id);
CREATE INDEX idx_payments_customer_id ON payment_processing.payments(customer_id);
CREATE INDEX idx_payments_status ON payment_processing.payments(payment_status);
CREATE INDEX idx_payments_scheduled_date ON payment_processing.payments(scheduled_date);
CREATE INDEX idx_payments_actual_date ON payment_processing.payments(actual_payment_date);

-- Payment Schedules
CREATE TABLE payment_processing.payment_schedules (
    id BIGSERIAL PRIMARY KEY,
    loan_id BIGINT NOT NULL,
    installment_number INTEGER NOT NULL,
    due_date DATE NOT NULL,
    principal_amount DECIMAL(15,2) NOT NULL,
    interest_amount DECIMAL(15,2) NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    outstanding_balance DECIMAL(15,2) NOT NULL,
    payment_status VARCHAR(20) DEFAULT 'PENDING' CHECK (payment_status IN ('PENDING', 'PAID', 'OVERDUE', 'PARTIALLY_PAID')),
    paid_amount DECIMAL(15,2) DEFAULT 0,
    paid_date TIMESTAMP WITH TIME ZONE,
    penalty_amount DECIMAL(15,2) DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payment_schedules_loan_id ON payment_processing.payment_schedules(loan_id);
CREATE INDEX idx_payment_schedules_due_date ON payment_processing.payment_schedules(due_date);
CREATE INDEX idx_payment_schedules_status ON payment_processing.payment_schedules(payment_status);

-- Penalty Calculations
CREATE TABLE payment_processing.penalty_calculations (
    id BIGSERIAL PRIMARY KEY,
    payment_schedule_id BIGINT NOT NULL,
    days_overdue INTEGER NOT NULL,
    penalty_rate DECIMAL(5,4) NOT NULL,
    penalty_amount DECIMAL(15,2) NOT NULL,
    calculation_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    applied_date TIMESTAMP WITH TIME ZONE,
    FOREIGN KEY (payment_schedule_id) REFERENCES payment_processing.payment_schedules(id)
);

-- Payment Events for Event Sourcing
CREATE TABLE payment_processing.payment_events (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT,
    loan_id BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_data JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Interest Rate History
CREATE TABLE payment_processing.interest_rate_history (
    id BIGSERIAL PRIMARY KEY,
    loan_id BIGINT NOT NULL,
    old_rate DECIMAL(5,4),
    new_rate DECIMAL(5,4) NOT NULL,
    effective_date DATE NOT NULL,
    reason VARCHAR(255),
    applied_by BIGINT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);