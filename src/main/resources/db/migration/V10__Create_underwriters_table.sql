-- Create underwriters table for staff management
-- This table stores underwriter information including specialization and approval limits
CREATE TABLE underwriters (
    underwriter_id VARCHAR(20) PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20),
    specialization VARCHAR(50) NOT NULL CHECK (specialization IN ('PERSONAL_LOANS', 'BUSINESS_LOANS', 'MORTGAGES')),
    years_experience INTEGER NOT NULL CHECK (years_experience >= 0),
    approval_limit DECIMAL(15,2) NOT NULL CHECK (approval_limit > 0),
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'ON_LEAVE')),
    hire_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version INTEGER DEFAULT 0
);

-- Create indexes for better performance
CREATE INDEX idx_underwriters_specialization ON underwriters(specialization);
CREATE INDEX idx_underwriters_status ON underwriters(status);
CREATE INDEX idx_underwriters_approval_limit ON underwriters(approval_limit);
CREATE INDEX idx_underwriters_email ON underwriters(email);

-- Add comments for documentation
COMMENT ON TABLE underwriters IS 'Underwriter staff management with specialization and approval limits';
COMMENT ON COLUMN underwriters.underwriter_id IS 'Unique underwriter identifier';
COMMENT ON COLUMN underwriters.specialization IS 'Loan type specialization (PERSONAL_LOANS, BUSINESS_LOANS, MORTGAGES)';
COMMENT ON COLUMN underwriters.years_experience IS 'Years of underwriting experience';
COMMENT ON COLUMN underwriters.approval_limit IS 'Maximum loan amount this underwriter can approve';
COMMENT ON COLUMN underwriters.status IS 'Employment status (ACTIVE, INACTIVE, ON_LEAVE)';

-- Create trigger to automatically update updated_at column
CREATE TRIGGER update_underwriters_updated_at
    BEFORE UPDATE ON underwriters
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();