-- Banking Database Initialization Script
-- Creates basic schema for enterprise loan management system

-- Create schemas
CREATE SCHEMA IF NOT EXISTS banking;
CREATE SCHEMA IF NOT EXISTS audit;
CREATE SCHEMA IF NOT EXISTS security;

-- Set default schema
SET search_path TO banking, audit, security, public;

-- Create basic tables (minimal set)
CREATE TABLE IF NOT EXISTS customers (
    id BIGSERIAL PRIMARY KEY,
    customer_number VARCHAR(50) UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS loans (
    id BIGSERIAL PRIMARY KEY,
    loan_number VARCHAR(50) UNIQUE NOT NULL,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    principal_amount DECIMAL(15,2) NOT NULL,
    interest_rate DECIMAL(5,4) NOT NULL,
    term_months INTEGER NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    application_date DATE DEFAULT CURRENT_DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    loan_id BIGINT NOT NULL REFERENCES loans(id),
    payment_amount DECIMAL(15,2) NOT NULL,
    payment_date DATE NOT NULL,
    payment_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX idx_customers_email ON customers(email);
CREATE INDEX idx_customers_status ON customers(status);
CREATE INDEX idx_loans_customer_id ON loans(customer_id);
CREATE INDEX idx_loans_status ON loans(status);
CREATE INDEX idx_payments_loan_id ON payments(loan_id);
CREATE INDEX idx_payments_date ON payments(payment_date);

-- Insert sample data for testing
INSERT INTO customers (customer_number, first_name, last_name, email, phone) VALUES
('CUST-001', 'John', 'Doe', 'john.doe@example.com', '+1-555-0123'),
('CUST-002', 'Jane', 'Smith', 'jane.smith@example.com', '+1-555-0124'),
('CUST-003', 'Bob', 'Johnson', 'bob.johnson@example.com', '+1-555-0125');

INSERT INTO loans (loan_number, customer_id, principal_amount, interest_rate, term_months, status) VALUES
('LOAN-001', 1, 250000.00, 0.0450, 360, 'ACTIVE'),
('LOAN-002', 2, 35000.00, 0.0675, 84, 'PENDING'),
('LOAN-003', 3, 150000.00, 0.0525, 240, 'APPROVED');

-- Grant permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA banking TO banking_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA banking TO banking_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA audit TO banking_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA audit TO banking_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA security TO banking_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA security TO banking_user;