-- Customer Management Bounded Context
CREATE SCHEMA IF NOT EXISTS customer_management;

CREATE TABLE customer_management.customers (
    id BIGSERIAL PRIMARY KEY,
    customer_number VARCHAR(20) UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(20),
    date_of_birth DATE NOT NULL,
    ssn VARCHAR(11) UNIQUE NOT NULL,
    credit_score INTEGER CHECK (credit_score >= 300 AND credit_score <= 850),
    annual_income DECIMAL(15,2),
    employment_status VARCHAR(50),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(50),
    zip_code VARCHAR(10),
    country VARCHAR(50) DEFAULT 'USA',
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version INTEGER DEFAULT 0
);

CREATE INDEX idx_customers_email ON customer_management.customers(email);
CREATE INDEX idx_customers_ssn ON customer_management.customers(ssn);
CREATE INDEX idx_customers_credit_score ON customer_management.customers(credit_score);
CREATE INDEX idx_customers_status ON customer_management.customers(status);

-- Customer Documents
CREATE TABLE customer_management.customer_documents (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    document_number VARCHAR(100),
    file_path VARCHAR(500),
    verification_status VARCHAR(20) DEFAULT 'PENDING' CHECK (verification_status IN ('PENDING', 'VERIFIED', 'REJECTED')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customer_management.customers(id)
);

-- Customer Events for Event Sourcing
CREATE TABLE customer_management.customer_events (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_data JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customer_management.customers(id)
);