-- Create credit reports table for credit bureau integration
-- This table stores credit reports from major credit bureaus (Experian, Equifax, TransUnion)
CREATE TABLE credit_reports (
    report_id VARCHAR(30) PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    bureau_name VARCHAR(20) NOT NULL CHECK (bureau_name IN ('EXPERIAN', 'EQUIFAX', 'TRANSUNION')),
    report_date DATE NOT NULL,
    credit_score INTEGER CHECK (credit_score >= 300 AND credit_score <= 850),
    payment_history_score VARCHAR(20) CHECK (payment_history_score IN ('EXCELLENT', 'VERY_GOOD', 'GOOD', 'FAIR', 'POOR')),
    credit_utilization DECIMAL(5,3) CHECK (credit_utilization >= 0 AND credit_utilization <= 1),
    length_of_history VARCHAR(20) CHECK (length_of_history IN ('EXCELLENT', 'VERY_GOOD', 'GOOD', 'FAIR', 'POOR')),
    credit_mix VARCHAR(20) CHECK (credit_mix IN ('EXCELLENT', 'VERY_GOOD', 'GOOD', 'FAIR', 'POOR')),
    new_credit VARCHAR(20) CHECK (new_credit IN ('EXCELLENT', 'VERY_GOOD', 'GOOD', 'FAIR', 'POOR')),
    derogatory_marks INTEGER DEFAULT 0 CHECK (derogatory_marks >= 0),
    total_accounts INTEGER DEFAULT 0 CHECK (total_accounts >= 0),
    open_accounts INTEGER DEFAULT 0 CHECK (open_accounts >= 0),
    closed_accounts INTEGER DEFAULT 0 CHECK (closed_accounts >= 0),
    hard_inquiries INTEGER DEFAULT 0 CHECK (hard_inquiries >= 0),
    soft_inquiries INTEGER DEFAULT 0 CHECK (soft_inquiries >= 0),
    oldest_account_age_months INTEGER CHECK (oldest_account_age_months >= 0),
    average_account_age_months INTEGER CHECK (average_account_age_months >= 0),
    total_credit_limit DECIMAL(15,2) CHECK (total_credit_limit >= 0),
    total_balance DECIMAL(15,2) CHECK (total_balance >= 0),
    report_data JSONB NOT NULL,
    raw_report_data JSONB,
    expiry_date DATE NOT NULL,
    report_cost DECIMAL(8,2) DEFAULT 0.00 CHECK (report_cost >= 0),
    request_reason VARCHAR(50) NOT NULL CHECK (request_reason IN ('LOAN_APPLICATION', 'ACCOUNT_REVIEW', 'FRAUD_INVESTIGATION', 'COLLECTIONS', 'PREQUALIFICATION')),
    requested_by VARCHAR(100) NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'EXPIRED', 'DISPUTED', 'FROZEN')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version INTEGER DEFAULT 0,
    
    -- Foreign key constraints
    CONSTRAINT fk_credit_reports_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE RESTRICT,
    
    -- Business rule constraints
    CONSTRAINT chk_expiry_after_report_date CHECK (expiry_date > report_date),
    CONSTRAINT chk_open_closed_accounts_total CHECK (open_accounts + closed_accounts = total_accounts),
    CONSTRAINT chk_oldest_not_exceed_average CHECK (oldest_account_age_months IS NULL OR average_account_age_months IS NULL OR oldest_account_age_months >= average_account_age_months),
    CONSTRAINT chk_utilization_calculation CHECK (total_credit_limit = 0 OR (total_balance / total_credit_limit) >= (credit_utilization - 0.01) AND (total_balance / total_credit_limit) <= (credit_utilization + 0.01))
);

-- Create indexes for better performance
CREATE INDEX idx_credit_reports_customer_id ON credit_reports(customer_id);
CREATE INDEX idx_credit_reports_bureau_name ON credit_reports(bureau_name);
CREATE INDEX idx_credit_reports_report_date ON credit_reports(report_date);
CREATE INDEX idx_credit_reports_credit_score ON credit_reports(credit_score);
CREATE INDEX idx_credit_reports_expiry_date ON credit_reports(expiry_date);
CREATE INDEX idx_credit_reports_status ON credit_reports(status);
CREATE INDEX idx_credit_reports_request_reason ON credit_reports(request_reason);
CREATE INDEX idx_credit_reports_customer_bureau ON credit_reports(customer_id, bureau_name);
CREATE INDEX idx_credit_reports_customer_active ON credit_reports(customer_id, status) WHERE status = 'ACTIVE';

-- Create partial index for recent reports
CREATE INDEX idx_credit_reports_recent ON credit_reports(customer_id, bureau_name, report_date DESC) 
WHERE report_date >= CURRENT_DATE - INTERVAL '12 months';

-- Add comments for documentation
COMMENT ON TABLE credit_reports IS 'Credit bureau reports for customer credit assessment';
COMMENT ON COLUMN credit_reports.report_id IS 'Unique credit report identifier';
COMMENT ON COLUMN credit_reports.customer_id IS 'Customer for whom credit report was obtained';
COMMENT ON COLUMN credit_reports.bureau_name IS 'Credit bureau (EXPERIAN, EQUIFAX, TRANSUNION)';
COMMENT ON COLUMN credit_reports.credit_score IS 'FICO credit score (300-850)';
COMMENT ON COLUMN credit_reports.payment_history_score IS 'Payment history component score';
COMMENT ON COLUMN credit_reports.credit_utilization IS 'Credit utilization ratio (0.0-1.0)';
COMMENT ON COLUMN credit_reports.derogatory_marks IS 'Number of negative marks (bankruptcies, liens, etc.)';
COMMENT ON COLUMN credit_reports.hard_inquiries IS 'Number of hard credit inquiries';
COMMENT ON COLUMN credit_reports.soft_inquiries IS 'Number of soft credit inquiries';
COMMENT ON COLUMN credit_reports.total_credit_limit IS 'Total available credit across all accounts';
COMMENT ON COLUMN credit_reports.total_balance IS 'Total outstanding balances';
COMMENT ON COLUMN credit_reports.report_data IS 'Parsed credit report data in JSON format';
COMMENT ON COLUMN credit_reports.raw_report_data IS 'Raw credit bureau response data';
COMMENT ON COLUMN credit_reports.request_reason IS 'Business reason for requesting credit report';
COMMENT ON COLUMN credit_reports.requested_by IS 'User or system that requested the report';

-- Create trigger to automatically update updated_at column
CREATE TRIGGER update_credit_reports_updated_at
    BEFORE UPDATE ON credit_reports
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();