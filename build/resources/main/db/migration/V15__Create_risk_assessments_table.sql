-- Create risk assessments table for loan risk management
-- This table stores risk analysis results including probability of default and loss calculations
CREATE TABLE risk_assessments (
    assessment_id VARCHAR(30) PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    loan_id VARCHAR(36),
    application_id VARCHAR(20),
    assessment_date DATE NOT NULL,
    assessment_type VARCHAR(30) NOT NULL CHECK (assessment_type IN ('APPLICATION', 'ANNUAL_REVIEW', 'EVENT_DRIVEN', 'PORTFOLIO_REVIEW', 'STRESS_TEST')),
    risk_score DECIMAL(5,2) NOT NULL CHECK (risk_score >= 0 AND risk_score <= 100),
    risk_category VARCHAR(20) NOT NULL CHECK (risk_category IN ('LOW', 'MEDIUM', 'HIGH', 'VERY_HIGH', 'CRITICAL')),
    probability_of_default DECIMAL(5,4) NOT NULL CHECK (probability_of_default >= 0 AND probability_of_default <= 1),
    loss_given_default DECIMAL(5,4) NOT NULL CHECK (loss_given_default >= 0 AND loss_given_default <= 1),
    exposure_at_default DECIMAL(15,2) NOT NULL CHECK (exposure_at_default >= 0),
    expected_loss DECIMAL(15,2) GENERATED ALWAYS AS (exposure_at_default * probability_of_default * loss_given_default) STORED,
    risk_factors JSONB NOT NULL,
    protective_factors JSONB,
    mitigation_measures JSONB,
    model_version VARCHAR(20) NOT NULL,
    confidence_score DECIMAL(5,2) CHECK (confidence_score >= 0 AND confidence_score <= 100),
    stress_test_results JSONB,
    regulatory_capital_required DECIMAL(15,2) CHECK (regulatory_capital_required >= 0),
    economic_scenario VARCHAR(50) DEFAULT 'BASE_CASE' CHECK (economic_scenario IN ('OPTIMISTIC', 'BASE_CASE', 'ADVERSE', 'SEVERELY_ADVERSE')),
    review_date DATE,
    next_assessment_due DATE,
    assessed_by VARCHAR(100) NOT NULL,
    approved_by VARCHAR(100),
    approval_date DATE,
    status VARCHAR(20) DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'SUPERSEDED')),
    override_reason TEXT,
    override_approved_by VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version INTEGER DEFAULT 0,
    
    -- Foreign key constraints
    CONSTRAINT fk_risk_assessments_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE RESTRICT,
    CONSTRAINT fk_risk_assessments_loan FOREIGN KEY (loan_id) REFERENCES loans(id) ON DELETE SET NULL,
    CONSTRAINT fk_risk_assessments_application FOREIGN KEY (application_id) REFERENCES loan_applications(application_id) ON DELETE SET NULL,
    
    -- Business rule constraints
    CONSTRAINT chk_assessment_date_not_future CHECK (assessment_date <= CURRENT_DATE),
    CONSTRAINT chk_approval_date_after_assessment CHECK (approval_date IS NULL OR approval_date >= assessment_date),
    CONSTRAINT chk_review_date_after_assessment CHECK (review_date IS NULL OR review_date >= assessment_date),
    CONSTRAINT chk_next_assessment_after_current CHECK (next_assessment_due IS NULL OR next_assessment_due > assessment_date),
    CONSTRAINT chk_approved_status_requires_approver CHECK ((status != 'APPROVED') OR (approved_by IS NOT NULL AND approval_date IS NOT NULL)),
    CONSTRAINT chk_override_requires_approval CHECK ((override_reason IS NULL) OR (override_approved_by IS NOT NULL)),
    CONSTRAINT chk_loan_or_application_exists CHECK ((loan_id IS NOT NULL) OR (application_id IS NOT NULL))
);

-- Create indexes for better performance
CREATE INDEX idx_risk_assessments_customer_id ON risk_assessments(customer_id);
CREATE INDEX idx_risk_assessments_loan_id ON risk_assessments(loan_id);
CREATE INDEX idx_risk_assessments_application_id ON risk_assessments(application_id);
CREATE INDEX idx_risk_assessments_assessment_date ON risk_assessments(assessment_date);
CREATE INDEX idx_risk_assessments_risk_category ON risk_assessments(risk_category);
CREATE INDEX idx_risk_assessments_risk_score ON risk_assessments(risk_score);
CREATE INDEX idx_risk_assessments_probability_of_default ON risk_assessments(probability_of_default);
CREATE INDEX idx_risk_assessments_expected_loss ON risk_assessments(expected_loss);
CREATE INDEX idx_risk_assessments_status ON risk_assessments(status);
CREATE INDEX idx_risk_assessments_next_due ON risk_assessments(next_assessment_due);
CREATE INDEX idx_risk_assessments_assessed_by ON risk_assessments(assessed_by);
CREATE INDEX idx_risk_assessments_model_version ON risk_assessments(model_version);

-- Create composite indexes for common queries
CREATE INDEX idx_risk_assessments_customer_date ON risk_assessments(customer_id, assessment_date DESC);
CREATE INDEX idx_risk_assessments_high_risk ON risk_assessments(risk_category, probability_of_default) 
WHERE risk_category IN ('HIGH', 'VERY_HIGH', 'CRITICAL');
CREATE INDEX idx_risk_assessments_pending_approval ON risk_assessments(assessment_date) 
WHERE status = 'PENDING_APPROVAL';

-- Add comments for documentation
COMMENT ON TABLE risk_assessments IS 'Risk assessment results for customers and loans with PD/LGD/EAD calculations';
COMMENT ON COLUMN risk_assessments.assessment_id IS 'Unique risk assessment identifier';
COMMENT ON COLUMN risk_assessments.assessment_type IS 'Type of risk assessment (APPLICATION, ANNUAL_REVIEW, etc.)';
COMMENT ON COLUMN risk_assessments.risk_score IS 'Overall risk score (0-100, higher = riskier)';
COMMENT ON COLUMN risk_assessments.risk_category IS 'Risk category classification';
COMMENT ON COLUMN risk_assessments.probability_of_default IS 'Probability of default within 12 months (0.0-1.0)';
COMMENT ON COLUMN risk_assessments.loss_given_default IS 'Expected loss percentage if default occurs (0.0-1.0)';
COMMENT ON COLUMN risk_assessments.exposure_at_default IS 'Expected outstanding amount at time of default';
COMMENT ON COLUMN risk_assessments.expected_loss IS 'Calculated expected loss (EAD * PD * LGD)';
COMMENT ON COLUMN risk_assessments.risk_factors IS 'JSON array of identified risk factors';
COMMENT ON COLUMN risk_assessments.protective_factors IS 'JSON array of factors that reduce risk';
COMMENT ON COLUMN risk_assessments.mitigation_measures IS 'JSON array of recommended risk mitigation actions';
COMMENT ON COLUMN risk_assessments.model_version IS 'Version of risk model used for assessment';
COMMENT ON COLUMN risk_assessments.confidence_score IS 'Model confidence in the assessment (0-100)';
COMMENT ON COLUMN risk_assessments.regulatory_capital_required IS 'Required regulatory capital allocation';
COMMENT ON COLUMN risk_assessments.economic_scenario IS 'Economic scenario used for assessment';
COMMENT ON COLUMN risk_assessments.override_reason IS 'Reason for manual override of model results';

-- Create trigger to automatically update updated_at column
CREATE TRIGGER update_risk_assessments_updated_at
    BEFORE UPDATE ON risk_assessments
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();