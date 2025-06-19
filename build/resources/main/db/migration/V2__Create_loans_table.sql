-- Create loans table
-- This table stores loan information including principal, interest, and installment details
CREATE TABLE loans (
    id VARCHAR(36) PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    loan_amount DECIMAL(19,2) NOT NULL CHECK (loan_amount > 0),
    number_of_installments INTEGER NOT NULL CHECK (number_of_installments IN (6, 9, 12, 24)),
    interest_rate DECIMAL(19,3) NOT NULL CHECK (interest_rate >= 0.1 AND interest_rate <= 0.5),
    create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_paid BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT fk_loans_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE RESTRICT
);

-- Create indexes for better performance
CREATE INDEX idx_loans_customer_id ON loans(customer_id);
CREATE INDEX idx_loans_create_date ON loans(create_date);
CREATE INDEX idx_loans_is_paid ON loans(is_paid);
CREATE INDEX idx_loans_number_of_installments ON loans(number_of_installments);
CREATE INDEX idx_loans_customer_paid ON loans(customer_id, is_paid);

-- Add comments for documentation
COMMENT ON TABLE loans IS 'Loan records with principal, interest, and payment tracking';
COMMENT ON COLUMN loans.id IS 'Unique loan identifier (UUID)';
COMMENT ON COLUMN loans.customer_id IS 'Reference to customer who owns the loan';
COMMENT ON COLUMN loans.loan_amount IS 'Principal loan amount (before interest)';
COMMENT ON COLUMN loans.number_of_installments IS 'Number of installments (6, 9, 12, or 24)';
COMMENT ON COLUMN loans.interest_rate IS 'Interest rate as decimal (0.1 = 10%, 0.5 = 50%)';
COMMENT ON COLUMN loans.create_date IS 'Loan creation date';
COMMENT ON COLUMN loans.is_paid IS 'Whether the loan is fully paid off';
COMMENT ON COLUMN loans.created_at IS 'Record creation timestamp';
COMMENT ON COLUMN loans.updated_at IS 'Record last update timestamp';
COMMENT ON COLUMN loans.version IS 'Optimistic locking version';

-- Create trigger to automatically update updated_at column
CREATE TRIGGER update_loans_updated_at
    BEFORE UPDATE ON loans
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
