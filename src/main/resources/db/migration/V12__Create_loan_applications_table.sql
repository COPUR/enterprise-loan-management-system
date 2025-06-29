-- Create loan applications table for application workflow management
-- This table stores loan application information throughout the underwriting process
CREATE TABLE loan_applications (
    application_id VARCHAR(20) PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    loan_type VARCHAR(20) NOT NULL CHECK (loan_type IN ('PERSONAL', 'BUSINESS', 'MORTGAGE', 'AUTO_LOAN')),
    requested_amount DECIMAL(15,2) NOT NULL CHECK (requested_amount > 0),
    requested_term_months INTEGER NOT NULL CHECK (requested_term_months > 0 AND requested_term_months <= 480),
    purpose VARCHAR(255),
    application_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('PENDING', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'PENDING_DOCUMENTS', 'CANCELLED')),
    assigned_underwriter VARCHAR(20),
    priority VARCHAR(20) DEFAULT 'STANDARD' CHECK (priority IN ('LOW', 'STANDARD', 'HIGH', 'URGENT')),
    monthly_income DECIMAL(15,2) CHECK (monthly_income >= 0),
    employment_years INTEGER CHECK (employment_years >= 0),
    collateral_value DECIMAL(15,2) CHECK (collateral_value >= 0),
    business_revenue DECIMAL(15,2) CHECK (business_revenue >= 0),
    property_value DECIMAL(15,2) CHECK (property_value >= 0),
    down_payment DECIMAL(15,2) CHECK (down_payment >= 0),
    decision_date DATE,
    decision_reason TEXT,
    approved_amount DECIMAL(15,2) CHECK (approved_amount >= 0),
    approved_rate DECIMAL(5,3) CHECK (approved_rate >= 0),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version INTEGER DEFAULT 0,
    
    -- Foreign key constraints
    CONSTRAINT fk_loan_applications_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE RESTRICT,
    CONSTRAINT fk_loan_applications_underwriter FOREIGN KEY (assigned_underwriter) REFERENCES underwriters(underwriter_id) ON DELETE SET NULL,
    
    -- Business rule constraints
    CONSTRAINT chk_approved_amount_not_exceed_requested CHECK (approved_amount IS NULL OR approved_amount <= requested_amount),
    CONSTRAINT chk_down_payment_for_mortgage CHECK (loan_type != 'MORTGAGE' OR down_payment IS NOT NULL),
    CONSTRAINT chk_business_revenue_for_business_loan CHECK (loan_type != 'BUSINESS' OR business_revenue IS NOT NULL),
    CONSTRAINT chk_decision_date_after_application CHECK (decision_date IS NULL OR decision_date >= application_date)
);

-- Create indexes for better performance
CREATE INDEX idx_loan_applications_customer_id ON loan_applications(customer_id);
CREATE INDEX idx_loan_applications_status ON loan_applications(status);
CREATE INDEX idx_loan_applications_loan_type ON loan_applications(loan_type);
CREATE INDEX idx_loan_applications_assigned_underwriter ON loan_applications(assigned_underwriter);
CREATE INDEX idx_loan_applications_application_date ON loan_applications(application_date);
CREATE INDEX idx_loan_applications_priority ON loan_applications(priority);
CREATE INDEX idx_loan_applications_requested_amount ON loan_applications(requested_amount);
CREATE INDEX idx_loan_applications_status_underwriter ON loan_applications(status, assigned_underwriter);

-- Add comments for documentation
COMMENT ON TABLE loan_applications IS 'Loan application workflow management with underwriting tracking';
COMMENT ON COLUMN loan_applications.application_id IS 'Unique loan application identifier';
COMMENT ON COLUMN loan_applications.loan_type IS 'Type of loan (PERSONAL, BUSINESS, MORTGAGE, AUTO_LOAN)';
COMMENT ON COLUMN loan_applications.requested_amount IS 'Amount requested by customer';
COMMENT ON COLUMN loan_applications.requested_term_months IS 'Requested loan term in months (1-480)';
COMMENT ON COLUMN loan_applications.status IS 'Application status in workflow';
COMMENT ON COLUMN loan_applications.assigned_underwriter IS 'Underwriter responsible for review';
COMMENT ON COLUMN loan_applications.priority IS 'Processing priority (LOW, STANDARD, HIGH, URGENT)';
COMMENT ON COLUMN loan_applications.monthly_income IS 'Applicant monthly income';
COMMENT ON COLUMN loan_applications.employment_years IS 'Years of employment history';
COMMENT ON COLUMN loan_applications.collateral_value IS 'Value of collateral offered';
COMMENT ON COLUMN loan_applications.business_revenue IS 'Annual business revenue (for business loans)';
COMMENT ON COLUMN loan_applications.property_value IS 'Property value (for mortgages)';
COMMENT ON COLUMN loan_applications.down_payment IS 'Down payment amount (for mortgages)';
COMMENT ON COLUMN loan_applications.approved_amount IS 'Final approved loan amount';
COMMENT ON COLUMN loan_applications.approved_rate IS 'Final approved interest rate';

-- Create trigger to automatically update updated_at column
CREATE TRIGGER update_loan_applications_updated_at
    BEFORE UPDATE ON loan_applications
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();