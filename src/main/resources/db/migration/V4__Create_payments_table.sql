-- Create payments table
-- This table stores payment transaction records
CREATE TABLE payments (
    id VARCHAR(36) PRIMARY KEY,
    loan_id VARCHAR(36) NOT NULL,
    payment_amount DECIMAL(19,2) NOT NULL CHECK (payment_amount > 0),
    payment_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'INITIATED',
    installments_paid INTEGER NOT NULL DEFAULT 0 CHECK (installments_paid >= 0),
    total_discount DECIMAL(19,2) NOT NULL DEFAULT 0.00 CHECK (total_discount >= 0),
    total_penalty DECIMAL(19,2) NOT NULL DEFAULT 0.00 CHECK (total_penalty >= 0),
    is_loan_fully_paid BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT fk_payments_loan FOREIGN KEY (loan_id) REFERENCES loans(id) ON DELETE RESTRICT,
    CONSTRAINT payments_status_valid CHECK (payment_status IN ('INITIATED', 'PROCESSING', 'COMPLETED', 'FAILED'))
);

-- Create payment_installments table for tracking which installments were paid in each payment
CREATE TABLE payment_installments (
    payment_id VARCHAR(36) NOT NULL,
    installment_id VARCHAR(36) NOT NULL,
    
    PRIMARY KEY (payment_id, installment_id),
    CONSTRAINT fk_payment_installments_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_installments_installment FOREIGN KEY (installment_id) REFERENCES loan_installments(id) ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX idx_payments_loan_id ON payments(loan_id);
CREATE INDEX idx_payments_date ON payments(payment_date);
CREATE INDEX idx_payments_status ON payments(payment_status);
CREATE INDEX idx_payments_loan_date ON payments(loan_id, payment_date);
CREATE INDEX idx_payment_installments_payment ON payment_installments(payment_id);
CREATE INDEX idx_payment_installments_installment ON payment_installments(installment_id);

-- Add comments for documentation
COMMENT ON TABLE payments IS 'Payment transaction records';
COMMENT ON COLUMN payments.id IS 'Unique payment identifier (UUID)';
COMMENT ON COLUMN payments.loan_id IS 'Reference to the loan being paid';
COMMENT ON COLUMN payments.payment_amount IS 'Original payment amount requested';
COMMENT ON COLUMN payments.payment_date IS 'Date when payment was initiated';
COMMENT ON COLUMN payments.payment_status IS 'Payment processing status';
COMMENT ON COLUMN payments.installments_paid IS 'Number of installments paid in this transaction';
COMMENT ON COLUMN payments.total_discount IS 'Total early payment discount applied';
COMMENT ON COLUMN payments.total_penalty IS 'Total late payment penalty applied';
COMMENT ON COLUMN payments.is_loan_fully_paid IS 'Whether this payment completed the loan';
COMMENT ON COLUMN payments.created_at IS 'Record creation timestamp';
COMMENT ON COLUMN payments.updated_at IS 'Record last update timestamp';
COMMENT ON COLUMN payments.version IS 'Optimistic locking version';

COMMENT ON TABLE payment_installments IS 'Junction table linking payments to installments';
COMMENT ON COLUMN payment_installments.payment_id IS 'Reference to payment record';
COMMENT ON COLUMN payment_installments.installment_id IS 'Reference to installment that was paid';

-- Create trigger to automatically update updated_at column
CREATE TRIGGER update_payments_updated_at
    BEFORE UPDATE ON payments
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
