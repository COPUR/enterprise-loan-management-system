-- Create loan officers table for sales staff management
-- This table stores loan officer information including region and commission tracking
CREATE TABLE loan_officers (
    officer_id VARCHAR(20) PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20),
    region VARCHAR(50) NOT NULL,
    portfolio_size INTEGER DEFAULT 0 CHECK (portfolio_size >= 0),
    commission_rate DECIMAL(5,4) NOT NULL CHECK (commission_rate >= 0 AND commission_rate <= 1),
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'ON_LEAVE')),
    hire_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version INTEGER DEFAULT 0
);

-- Create indexes for better performance
CREATE INDEX idx_loan_officers_region ON loan_officers(region);
CREATE INDEX idx_loan_officers_status ON loan_officers(status);
CREATE INDEX idx_loan_officers_portfolio_size ON loan_officers(portfolio_size);
CREATE INDEX idx_loan_officers_email ON loan_officers(email);
CREATE INDEX idx_loan_officers_commission_rate ON loan_officers(commission_rate);

-- Add comments for documentation
COMMENT ON TABLE loan_officers IS 'Loan officer sales staff management with regional assignment and commission tracking';
COMMENT ON COLUMN loan_officers.officer_id IS 'Unique loan officer identifier';
COMMENT ON COLUMN loan_officers.region IS 'Sales region assignment (NORTHEAST, SOUTHEAST, WEST, etc.)';
COMMENT ON COLUMN loan_officers.portfolio_size IS 'Number of active loans in portfolio';
COMMENT ON COLUMN loan_officers.commission_rate IS 'Commission rate as decimal (0.0025 = 0.25%)';
COMMENT ON COLUMN loan_officers.status IS 'Employment status (ACTIVE, INACTIVE, ON_LEAVE)';

-- Create trigger to automatically update updated_at column
CREATE TRIGGER update_loan_officers_updated_at
    BEFORE UPDATE ON loan_officers
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();