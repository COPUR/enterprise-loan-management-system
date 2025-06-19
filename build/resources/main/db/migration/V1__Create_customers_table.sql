-- Create customers table
-- This table stores customer information and credit limits
CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    surname VARCHAR(100) NOT NULL,
    credit_limit DECIMAL(19,2) NOT NULL CHECK (credit_limit >= 1000.00 AND credit_limit <= 1000000.00),
    used_credit_limit DECIMAL(19,2) NOT NULL DEFAULT 0.00 CHECK (used_credit_limit >= 0.00),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT customers_credit_limit_valid CHECK (used_credit_limit <= credit_limit)
);

-- Create indexes for better performance
CREATE INDEX idx_customers_name ON customers(name);
CREATE INDEX idx_customers_surname ON customers(surname);
CREATE INDEX idx_customers_credit_limit ON customers(credit_limit);
CREATE INDEX idx_customers_created_at ON customers(created_at);

-- Add comments for documentation
COMMENT ON TABLE customers IS 'Customer information and credit management';
COMMENT ON COLUMN customers.id IS 'Unique customer identifier';
COMMENT ON COLUMN customers.name IS 'Customer first name';
COMMENT ON COLUMN customers.surname IS 'Customer last name';
COMMENT ON COLUMN customers.credit_limit IS 'Maximum credit limit available to customer (1,000 - 1,000,000)';
COMMENT ON COLUMN customers.used_credit_limit IS 'Currently used credit amount';
COMMENT ON COLUMN customers.created_at IS 'Record creation timestamp';
COMMENT ON COLUMN customers.updated_at IS 'Record last update timestamp';
COMMENT ON COLUMN customers.version IS 'Optimistic locking version';

-- Create trigger to automatically update updated_at column
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_customers_updated_at
    BEFORE UPDATE ON customers
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
