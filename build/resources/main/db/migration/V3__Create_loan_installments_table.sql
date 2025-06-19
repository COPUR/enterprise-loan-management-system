-- Create loan_installments table
-- This table stores individual installment information for each loan
CREATE TABLE loan_installments (
    id VARCHAR(36) PRIMARY KEY,
    loan_id VARCHAR(36) NOT NULL,
    amount DECIMAL(19,2) NOT NULL CHECK (amount > 0),
    paid_amount DECIMAL(19,2) NOT NULL DEFAULT 0.00 CHECK (paid_amount >= 0),
    due_date DATE NOT NULL,
    payment_date TIMESTAMP NULL,
    is_paid BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT fk_installments_loan FOREIGN KEY (loan_id) REFERENCES loans(id) ON DELETE CASCADE,
    CONSTRAINT installments_paid_amount_valid CHECK (paid_amount <= amount),
    CONSTRAINT installments_payment_consistency CHECK (
        (is_paid = TRUE AND payment_date IS NOT NULL AND paid_amount = amount) OR
        (is_paid = FALSE AND (payment_date IS NULL OR paid_amount = 0))
    )
);

-- Create indexes for better performance
CREATE INDEX idx_installments_loan_id ON loan_installments(loan_id);
CREATE INDEX idx_installments_due_date ON loan_installments(due_date);
CREATE INDEX idx_installments_is_paid ON loan_installments(is_paid);
CREATE INDEX idx_installments_payment_date ON loan_installments(payment_date);
CREATE INDEX idx_installments_loan_due ON loan_installments(loan_id, due_date);
CREATE INDEX idx_installments_loan_paid ON loan_installments(loan_id, is_paid);

-- Add comments for documentation
COMMENT ON TABLE loan_installments IS 'Individual installment records for loans';
COMMENT ON COLUMN loan_installments.id IS 'Unique installment identifier (UUID)';
COMMENT ON COLUMN loan_installments.loan_id IS 'Reference to the parent loan';
COMMENT ON COLUMN loan_installments.amount IS 'Installment amount to be paid';
COMMENT ON COLUMN loan_installments.paid_amount IS 'Amount actually paid (0 or equal to amount)';
COMMENT ON COLUMN loan_installments.due_date IS 'Date when installment is due';
COMMENT ON COLUMN loan_installments.payment_date IS 'Date when installment was paid (null if unpaid)';
COMMENT ON COLUMN loan_installments.is_paid IS 'Whether the installment has been paid';
COMMENT ON COLUMN loan_installments.created_at IS 'Record creation timestamp';
COMMENT ON COLUMN loan_installments.updated_at IS 'Record last update timestamp';
COMMENT ON COLUMN loan_installments.version IS 'Optimistic locking version';

-- Create trigger to automatically update updated_at column
CREATE TRIGGER update_installments_updated_at
    BEFORE UPDATE ON loan_installments
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Create function to check if all installments are paid for a loan
CREATE OR REPLACE FUNCTION check_loan_fully_paid()
RETURNS TRIGGER AS $$
BEGIN
    -- Update loan is_paid status when all installments are paid
    UPDATE loans 
    SET is_paid = NOT EXISTS (
        SELECT 1 FROM loan_installments 
        WHERE loan_id = NEW.loan_id AND is_paid = FALSE
    )
    WHERE id = NEW.loan_id;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to automatically update loan payment status
CREATE TRIGGER update_loan_payment_status
    AFTER UPDATE OF is_paid ON loan_installments
    FOR EACH ROW
    WHEN (NEW.is_paid = TRUE)
    EXECUTE FUNCTION check_loan_fully_paid();
