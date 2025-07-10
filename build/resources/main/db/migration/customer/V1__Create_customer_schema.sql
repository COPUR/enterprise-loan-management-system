-- Customer Service Database Schema
CREATE SCHEMA IF NOT EXISTS customer_service;

-- Customers table
CREATE TABLE customer_service.customers (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(50) NOT NULL,
    date_of_birth DATE NOT NULL,
    monthly_income DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    
    CONSTRAINT chk_customer_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'BLOCKED'))
);

-- Indexes for performance
CREATE INDEX idx_customer_email ON customer_service.customers(email);
CREATE INDEX idx_customer_status ON customer_service.customers(status);

-- Audit table for customer changes
CREATE TABLE customer_service.customer_audit (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    old_values JSONB,
    new_values JSONB,
    changed_by VARCHAR(100),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);