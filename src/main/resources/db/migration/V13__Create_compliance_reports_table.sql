-- Create compliance reports table for regulatory reporting
-- This table stores regulatory compliance reports and audit findings
CREATE TABLE compliance_reports (
    report_id VARCHAR(30) PRIMARY KEY,
    report_type VARCHAR(50) NOT NULL CHECK (report_type IN ('FAIR_LENDING', 'RISK_ASSESSMENT', 'CFPB_EXAMINATION', 'HMDA_REPORTING', 'CRA_ASSESSMENT', 'BSA_AML', 'FDCPA_COMPLIANCE')),
    generation_date DATE NOT NULL,
    reporting_period_start DATE NOT NULL,
    reporting_period_end DATE NOT NULL,
    total_loans INTEGER NOT NULL CHECK (total_loans >= 0),
    total_amount DECIMAL(20,2) NOT NULL CHECK (total_amount >= 0),
    high_risk_loans INTEGER DEFAULT 0 CHECK (high_risk_loans >= 0),
    compliance_score DECIMAL(5,2) CHECK (compliance_score >= 0 AND compliance_score <= 100),
    regulatory_findings INTEGER DEFAULT 0 CHECK (regulatory_findings >= 0),
    findings_details JSONB,
    report_data JSONB,
    report_file_path VARCHAR(500),
    generated_by VARCHAR(100) NOT NULL,
    reviewed_by VARCHAR(100),
    review_date DATE,
    status VARCHAR(30) DEFAULT 'GENERATED' CHECK (status IN ('GENERATED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'SUBMITTED')),
    submission_date DATE,
    regulator_reference VARCHAR(50),
    next_report_due DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version INTEGER DEFAULT 0,
    
    -- Business rule constraints
    CONSTRAINT chk_reporting_period_valid CHECK (reporting_period_end >= reporting_period_start),
    CONSTRAINT chk_generation_date_valid CHECK (generation_date >= reporting_period_end),
    CONSTRAINT chk_high_risk_not_exceed_total CHECK (high_risk_loans <= total_loans),
    CONSTRAINT chk_review_date_after_generation CHECK (review_date IS NULL OR review_date >= generation_date),
    CONSTRAINT chk_submission_after_approval CHECK (submission_date IS NULL OR status = 'SUBMITTED')
);

-- Create indexes for better performance
CREATE INDEX idx_compliance_reports_report_type ON compliance_reports(report_type);
CREATE INDEX idx_compliance_reports_generation_date ON compliance_reports(generation_date);
CREATE INDEX idx_compliance_reports_status ON compliance_reports(status);
CREATE INDEX idx_compliance_reports_reporting_period ON compliance_reports(reporting_period_start, reporting_period_end);
CREATE INDEX idx_compliance_reports_compliance_score ON compliance_reports(compliance_score);
CREATE INDEX idx_compliance_reports_regulatory_findings ON compliance_reports(regulatory_findings);
CREATE INDEX idx_compliance_reports_next_due ON compliance_reports(next_report_due);
CREATE INDEX idx_compliance_reports_generated_by ON compliance_reports(generated_by);

-- Create partial index for pending reviews
CREATE INDEX idx_compliance_reports_pending_review ON compliance_reports(report_type, generation_date) 
WHERE status = 'UNDER_REVIEW';

-- Add comments for documentation
COMMENT ON TABLE compliance_reports IS 'Regulatory compliance reports and audit findings tracking';
COMMENT ON COLUMN compliance_reports.report_id IS 'Unique compliance report identifier';
COMMENT ON COLUMN compliance_reports.report_type IS 'Type of regulatory report (FAIR_LENDING, RISK_ASSESSMENT, etc.)';
COMMENT ON COLUMN compliance_reports.reporting_period_start IS 'Start date of reporting period';
COMMENT ON COLUMN compliance_reports.reporting_period_end IS 'End date of reporting period';
COMMENT ON COLUMN compliance_reports.total_loans IS 'Total number of loans in reporting period';
COMMENT ON COLUMN compliance_reports.total_amount IS 'Total loan amount in reporting period';
COMMENT ON COLUMN compliance_reports.high_risk_loans IS 'Number of high-risk loans identified';
COMMENT ON COLUMN compliance_reports.compliance_score IS 'Overall compliance score (0-100)';
COMMENT ON COLUMN compliance_reports.regulatory_findings IS 'Number of regulatory findings/violations';
COMMENT ON COLUMN compliance_reports.findings_details IS 'Detailed findings in JSON format';
COMMENT ON COLUMN compliance_reports.report_data IS 'Complete report data in JSON format';
COMMENT ON COLUMN compliance_reports.generated_by IS 'System or user that generated the report';
COMMENT ON COLUMN compliance_reports.reviewed_by IS 'Compliance officer who reviewed the report';
COMMENT ON COLUMN compliance_reports.status IS 'Report status in workflow';
COMMENT ON COLUMN compliance_reports.regulator_reference IS 'Reference number from regulatory submission';

-- Create trigger to automatically update updated_at column
CREATE TRIGGER update_compliance_reports_updated_at
    BEFORE UPDATE ON compliance_reports
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();