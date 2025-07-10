-- Enterprise Banking Database Security Schema
-- Production-grade encryption, masking, and compliance

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
CREATE EXTENSION IF NOT EXISTS "btree_gin";

-- Create security schemas
CREATE SCHEMA IF NOT EXISTS banking_security;
CREATE SCHEMA IF NOT EXISTS banking_audit;
CREATE SCHEMA IF NOT EXISTS banking_compliance;

-- Set up Row Level Security (RLS) roles
CREATE ROLE banking_app_role;
CREATE ROLE banking_readonly_role;
CREATE ROLE banking_admin_role;
CREATE ROLE banking_compliance_role;
CREATE ROLE banking_audit_role;

-- =============================================
-- ENCRYPTION FUNCTIONS
-- =============================================

-- PII Encryption function using AES-256
CREATE OR REPLACE FUNCTION banking_security.encrypt_pii(
    plaintext TEXT,
    encryption_key TEXT DEFAULT current_setting('banking.encryption_key', true)
) RETURNS TEXT AS $$
BEGIN
    RETURN encode(
        pgp_sym_encrypt(
            plaintext,
            encryption_key,
            'compress-algo=1, cipher-algo=aes256'
        ),
        'base64'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- PII Decryption function
CREATE OR REPLACE FUNCTION banking_security.decrypt_pii(
    ciphertext TEXT,
    encryption_key TEXT DEFAULT current_setting('banking.encryption_key', true)
) RETURNS TEXT AS $$
BEGIN
    RETURN pgp_sym_decrypt(
        decode(ciphertext, 'base64'),
        encryption_key
    );
EXCEPTION
    WHEN OTHERS THEN
        RETURN '[DECRYPT_ERROR]';
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Payment data encryption (stronger encryption)
CREATE OR REPLACE FUNCTION banking_security.encrypt_payment_data(
    plaintext TEXT,
    encryption_key TEXT DEFAULT current_setting('banking.payment_encryption_key', true)
) RETURNS TEXT AS $$
BEGIN
    RETURN encode(
        pgp_sym_encrypt(
            plaintext,
            encryption_key,
            'compress-algo=2, cipher-algo=aes256, s2k-mode=3, s2k-count=65536'
        ),
        'base64'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Payment data decryption
CREATE OR REPLACE FUNCTION banking_security.decrypt_payment_data(
    ciphertext TEXT,
    encryption_key TEXT DEFAULT current_setting('banking.payment_encryption_key', true)
) RETURNS TEXT AS $$
BEGIN
    RETURN pgp_sym_decrypt(
        decode(ciphertext, 'base64'),
        encryption_key
    );
EXCEPTION
    WHEN OTHERS THEN
        RETURN '[DECRYPT_ERROR]';
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =============================================
-- DATA MASKING FUNCTIONS
-- =============================================

-- SSN masking function
CREATE OR REPLACE FUNCTION banking_security.mask_ssn(ssn TEXT) RETURNS TEXT AS $$
BEGIN
    IF LENGTH(ssn) >= 4 THEN
        RETURN 'XXX-XX-' || RIGHT(ssn, 4);
    ELSE
        RETURN 'XXX-XX-XXXX';
    END IF;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Credit card masking function
CREATE OR REPLACE FUNCTION banking_security.mask_credit_card(card_number TEXT) RETURNS TEXT AS $$
BEGIN
    IF LENGTH(card_number) >= 4 THEN
        RETURN '**** **** **** ' || RIGHT(card_number, 4);
    ELSE
        RETURN '**** **** **** ****';
    END IF;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Bank account masking function
CREATE OR REPLACE FUNCTION banking_security.mask_account_number(account_number TEXT) RETURNS TEXT AS $$
BEGIN
    IF LENGTH(account_number) >= 4 THEN
        RETURN '******' || RIGHT(account_number, 4);
    ELSE
        RETURN '**********';
    END IF;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Email masking function
CREATE OR REPLACE FUNCTION banking_security.mask_email(email TEXT) RETURNS TEXT AS $$
DECLARE
    at_pos INTEGER;
    local_part TEXT;
    domain_part TEXT;
BEGIN
    at_pos := POSITION('@' IN email);
    IF at_pos > 0 THEN
        local_part := SUBSTRING(email FROM 1 FOR at_pos - 1);
        domain_part := SUBSTRING(email FROM at_pos);
        
        IF LENGTH(local_part) > 2 THEN
            RETURN LEFT(local_part, 2) || '***' || domain_part;
        ELSE
            RETURN '***' || domain_part;
        END IF;
    ELSE
        RETURN '***@***.com';
    END IF;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- =============================================
-- AUDIT LOGGING FUNCTIONS
-- =============================================

-- Audit log table
CREATE TABLE IF NOT EXISTS banking_audit.audit_log (
    audit_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    table_name TEXT NOT NULL,
    operation TEXT NOT NULL,
    old_values JSONB,
    new_values JSONB,
    changed_by TEXT NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    client_ip INET,
    application_name TEXT,
    transaction_id TEXT,
    compliance_tags TEXT[]
);

-- Create index for audit queries
CREATE INDEX IF NOT EXISTS idx_audit_log_table_operation ON banking_audit.audit_log (table_name, operation);
CREATE INDEX IF NOT EXISTS idx_audit_log_changed_at ON banking_audit.audit_log (changed_at);
CREATE INDEX IF NOT EXISTS idx_audit_log_changed_by ON banking_audit.audit_log (changed_by);

-- Audit logging function
CREATE OR REPLACE FUNCTION banking_audit.log_operation() RETURNS TRIGGER AS $$
DECLARE
    old_data JSONB;
    new_data JSONB;
    compliance_tags TEXT[] := ARRAY[]::TEXT[];
BEGIN
    -- Determine compliance tags based on table
    IF TG_TABLE_NAME ~ 'customer' THEN
        compliance_tags := ARRAY['PII', 'GDPR', 'KYC'];
    ELSIF TG_TABLE_NAME ~ 'loan' THEN
        compliance_tags := ARRAY['CREDIT_DATA', 'SOX', 'BASEL'];
    ELSIF TG_TABLE_NAME ~ 'payment' THEN
        compliance_tags := ARRAY['PAYMENT_DATA', 'PCI_DSS', 'AML'];
    END IF;

    -- Handle different operations
    IF TG_OP = 'DELETE' THEN
        old_data := to_jsonb(OLD);
        new_data := NULL;
    ELSIF TG_OP = 'INSERT' THEN
        old_data := NULL;
        new_data := to_jsonb(NEW);
    ELSIF TG_OP = 'UPDATE' THEN
        old_data := to_jsonb(OLD);
        new_data := to_jsonb(NEW);
    END IF;

    -- Insert audit record
    INSERT INTO banking_audit.audit_log (
        table_name,
        operation,
        old_values,
        new_values,
        changed_by,
        client_ip,
        application_name,
        transaction_id,
        compliance_tags
    ) VALUES (
        TG_TABLE_NAME,
        TG_OP,
        old_data,
        new_data,
        COALESCE(current_setting('banking.user_id', true), current_user),
        COALESCE(inet_client_addr(), '127.0.0.1'::inet),
        COALESCE(current_setting('application_name', true), 'unknown'),
        COALESCE(current_setting('banking.transaction_id', true), txid_current()::text),
        compliance_tags
    );

    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =============================================
-- SECURE CUSTOMER TABLE WITH ENCRYPTION
-- =============================================

-- Drop existing customer table if exists
DROP TABLE IF EXISTS banking_customer.customers CASCADE;

-- Create secure customer table with encryption
CREATE TABLE banking_customer.customers (
    customer_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_type customer_type NOT NULL DEFAULT 'INDIVIDUAL',
    
    -- Encrypted PII fields
    first_name_encrypted TEXT, -- Encrypted
    last_name_encrypted TEXT,  -- Encrypted
    email_encrypted TEXT,      -- Encrypted
    phone_encrypted TEXT,      -- Encrypted
    ssn_encrypted TEXT,        -- Encrypted (SSN)
    date_of_birth_encrypted TEXT, -- Encrypted
    
    -- Searchable hashed fields for lookup
    email_hash TEXT UNIQUE,
    phone_hash TEXT,
    ssn_hash TEXT UNIQUE,
    
    -- Non-sensitive fields
    customer_status customer_status DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    -- Financial data (encrypted)
    annual_income_encrypted TEXT,
    credit_score_encrypted TEXT,
    
    -- Compliance fields
    kyc_status TEXT DEFAULT 'PENDING',
    kyc_verified_at TIMESTAMP WITH TIME ZONE,
    compliance_level TEXT DEFAULT 'BASIC',
    risk_rating TEXT DEFAULT 'MEDIUM',
    
    -- Audit fields
    created_by TEXT NOT NULL,
    updated_by TEXT NOT NULL,
    version_number INTEGER DEFAULT 1,
    
    -- Constraints
    CONSTRAINT valid_customer_type CHECK (customer_type IN ('INDIVIDUAL', 'CORPORATE', 'GOVERNMENT')),
    CONSTRAINT valid_customer_status CHECK (customer_status IN ('ACTIVE', 'SUSPENDED', 'CLOSED')),
    CONSTRAINT valid_kyc_status CHECK (kyc_status IN ('PENDING', 'IN_PROGRESS', 'VERIFIED', 'REJECTED')),
    CONSTRAINT valid_compliance_level CHECK (compliance_level IN ('BASIC', 'ENHANCED', 'HIGH')),
    CONSTRAINT valid_risk_rating CHECK (risk_rating IN ('LOW', 'MEDIUM', 'HIGH', 'VERY_HIGH'))
);

-- Create indexes for performance
CREATE INDEX idx_customers_type_status ON banking_customer.customers (customer_type, customer_status);
CREATE INDEX idx_customers_email_hash ON banking_customer.customers (email_hash);
CREATE INDEX idx_customers_kyc_status ON banking_customer.customers (kyc_status);
CREATE INDEX idx_customers_created_at ON banking_customer.customers (created_at);

-- Enable RLS
ALTER TABLE banking_customer.customers ENABLE ROW LEVEL SECURITY;

-- RLS policies
CREATE POLICY customer_app_policy ON banking_customer.customers
    FOR ALL TO banking_app_role
    USING (true);

CREATE POLICY customer_readonly_policy ON banking_customer.customers
    FOR SELECT TO banking_readonly_role
    USING (true);

CREATE POLICY customer_compliance_policy ON banking_customer.customers
    FOR SELECT TO banking_compliance_role
    USING (kyc_status = 'VERIFIED');

-- Create audit trigger
CREATE TRIGGER customer_audit_trigger
    AFTER INSERT OR UPDATE OR DELETE ON banking_customer.customers
    FOR EACH ROW EXECUTE FUNCTION banking_audit.log_operation();

-- =============================================
-- SECURE LOAN TABLE WITH ENCRYPTION
-- =============================================

-- Drop existing loan table if exists
DROP TABLE IF EXISTS banking_loan.loans CASCADE;

-- Create secure loan table
CREATE TABLE banking_loan.loans (
    loan_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id UUID NOT NULL,
    
    -- Loan details (some encrypted)
    loan_type loan_type NOT NULL,
    loan_status loan_status DEFAULT 'PENDING',
    
    -- Encrypted financial data
    principal_amount_encrypted TEXT NOT NULL,
    interest_rate_encrypted TEXT NOT NULL,
    term_months_encrypted TEXT NOT NULL,
    monthly_payment_encrypted TEXT,
    
    -- Searchable derived fields (for queries)
    principal_amount_range TEXT, -- 'LOW', 'MEDIUM', 'HIGH'
    interest_rate_range TEXT,    -- 'LOW', 'MEDIUM', 'HIGH'
    
    -- Loan purpose and details
    purpose_of_loan TEXT NOT NULL,
    collateral_description_encrypted TEXT,
    collateral_value_encrypted TEXT,
    
    -- Dates
    application_date DATE NOT NULL DEFAULT CURRENT_DATE,
    approval_date DATE,
    disbursement_date DATE,
    maturity_date DATE,
    
    -- Risk and compliance
    risk_score_encrypted TEXT,
    credit_decision_encrypted TEXT,
    underwriter_notes_encrypted TEXT,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by TEXT NOT NULL,
    updated_by TEXT NOT NULL,
    version_number INTEGER DEFAULT 1,
    
    -- Foreign key
    CONSTRAINT fk_loan_customer FOREIGN KEY (customer_id) REFERENCES banking_customer.customers(customer_id),
    
    -- Constraints
    CONSTRAINT valid_loan_type CHECK (loan_type IN ('PERSONAL', 'MORTGAGE', 'AUTO', 'BUSINESS', 'STUDENT')),
    CONSTRAINT valid_loan_status CHECK (loan_status IN ('PENDING', 'APPROVED', 'REJECTED', 'DISBURSED', 'ACTIVE', 'PAID_OFF', 'DEFAULTED')),
    CONSTRAINT valid_purpose CHECK (purpose_of_loan IN ('HOME_PURCHASE', 'HOME_IMPROVEMENT', 'DEBT_CONSOLIDATION', 'AUTO_PURCHASE', 'BUSINESS_EXPANSION', 'EDUCATION', 'PERSONAL_USE')),
    CONSTRAINT valid_dates CHECK (application_date <= COALESCE(approval_date, CURRENT_DATE))
);

-- Create indexes
CREATE INDEX idx_loans_customer_id ON banking_loan.loans (customer_id);
CREATE INDEX idx_loans_status ON banking_loan.loans (loan_status);
CREATE INDEX idx_loans_type ON banking_loan.loans (loan_type);
CREATE INDEX idx_loans_application_date ON banking_loan.loans (application_date);
CREATE INDEX idx_loans_amount_range ON banking_loan.loans (principal_amount_range);

-- Enable RLS
ALTER TABLE banking_loan.loans ENABLE ROW LEVEL SECURITY;

-- RLS policies
CREATE POLICY loan_app_policy ON banking_loan.loans
    FOR ALL TO banking_app_role
    USING (true);

CREATE POLICY loan_readonly_policy ON banking_loan.loans
    FOR SELECT TO banking_readonly_role
    USING (true);

-- Create audit trigger
CREATE TRIGGER loan_audit_trigger
    AFTER INSERT OR UPDATE OR DELETE ON banking_loan.loans
    FOR EACH ROW EXECUTE FUNCTION banking_audit.log_operation();

-- =============================================
-- SECURE PAYMENT TABLE WITH ENCRYPTION
-- =============================================

-- Drop existing payment table if exists
DROP TABLE IF EXISTS banking_payment.payments CASCADE;

-- Create secure payment table
CREATE TABLE banking_payment.payments (
    payment_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    loan_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    
    -- Payment details (encrypted)
    payment_amount_encrypted TEXT NOT NULL,
    payment_method_encrypted TEXT NOT NULL,
    payment_date DATE NOT NULL,
    
    -- Payment breakdown (encrypted)
    principal_amount_encrypted TEXT,
    interest_amount_encrypted TEXT,
    fees_amount_encrypted TEXT,
    
    -- Payment source information (encrypted)
    source_account_encrypted TEXT,
    routing_number_encrypted TEXT,
    payment_reference_encrypted TEXT,
    
    -- Payment status and processing
    payment_status payment_status DEFAULT 'PENDING',
    processing_date TIMESTAMP WITH TIME ZONE,
    settlement_date DATE,
    
    -- Risk and fraud data
    fraud_score_encrypted TEXT,
    risk_indicators_encrypted TEXT,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by TEXT NOT NULL,
    updated_by TEXT NOT NULL,
    version_number INTEGER DEFAULT 1,
    
    -- Foreign keys
    CONSTRAINT fk_payment_loan FOREIGN KEY (loan_id) REFERENCES banking_loan.loans(loan_id),
    CONSTRAINT fk_payment_customer FOREIGN KEY (customer_id) REFERENCES banking_customer.customers(customer_id),
    
    -- Constraints
    CONSTRAINT valid_payment_status CHECK (payment_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED', 'REFUNDED')),
    CONSTRAINT valid_payment_date CHECK (payment_date <= CURRENT_DATE + INTERVAL '30 days')
);

-- Create indexes
CREATE INDEX idx_payments_loan_id ON banking_payment.payments (loan_id);
CREATE INDEX idx_payments_customer_id ON banking_payment.payments (customer_id);
CREATE INDEX idx_payments_status ON banking_payment.payments (payment_status);
CREATE INDEX idx_payments_date ON banking_payment.payments (payment_date);
CREATE INDEX idx_payments_created_at ON banking_payment.payments (created_at);

-- Enable RLS
ALTER TABLE banking_payment.payments ENABLE ROW LEVEL SECURITY;

-- RLS policies
CREATE POLICY payment_app_policy ON banking_payment.payments
    FOR ALL TO banking_app_role
    USING (true);

CREATE POLICY payment_readonly_policy ON banking_payment.payments
    FOR SELECT TO banking_readonly_role
    USING (true);

-- Create audit trigger
CREATE TRIGGER payment_audit_trigger
    AFTER INSERT OR UPDATE OR DELETE ON banking_payment.payments
    FOR EACH ROW EXECUTE FUNCTION banking_audit.log_operation();

-- =============================================
-- DATA RETENTION POLICIES
-- =============================================

-- Create data retention function
CREATE OR REPLACE FUNCTION banking_compliance.apply_retention_policy() RETURNS void AS $$
BEGIN
    -- Archive old audit logs (keep 7 years)
    DELETE FROM banking_audit.audit_log 
    WHERE changed_at < NOW() - INTERVAL '7 years';
    
    -- Archive closed customer accounts (keep 10 years after closure)
    -- This would typically move to archive tables, not delete
    -- UPDATE banking_customer.customers 
    -- SET archived_at = NOW() 
    -- WHERE customer_status = 'CLOSED' 
    -- AND updated_at < NOW() - INTERVAL '10 years';
    
    -- Log retention policy execution
    INSERT INTO banking_audit.audit_log (
        table_name, operation, new_values, changed_by, compliance_tags
    ) VALUES (
        'SYSTEM', 'RETENTION_POLICY', 
        jsonb_build_object('executed_at', NOW()), 
        'SYSTEM', 
        ARRAY['DATA_RETENTION', 'COMPLIANCE']
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =============================================
-- SECURE VIEWS FOR APPLICATION ACCESS
-- =============================================

-- Customer view with decryption (for authorized access)
CREATE OR REPLACE VIEW banking_customer.customer_view AS
SELECT 
    customer_id,
    customer_type,
    CASE 
        WHEN has_column_privilege('banking_customer.customers', 'first_name_encrypted', 'SELECT') 
        THEN banking_security.decrypt_pii(first_name_encrypted)
        ELSE '[REDACTED]'
    END AS first_name,
    CASE 
        WHEN has_column_privilege('banking_customer.customers', 'last_name_encrypted', 'SELECT') 
        THEN banking_security.decrypt_pii(last_name_encrypted)
        ELSE '[REDACTED]'
    END AS last_name,
    CASE 
        WHEN has_column_privilege('banking_customer.customers', 'email_encrypted', 'SELECT') 
        THEN banking_security.decrypt_pii(email_encrypted)
        ELSE banking_security.mask_email(banking_security.decrypt_pii(email_encrypted))
    END AS email,
    customer_status,
    kyc_status,
    compliance_level,
    risk_rating,
    created_at,
    updated_at
FROM banking_customer.customers;

-- Loan view with decryption
CREATE OR REPLACE VIEW banking_loan.loan_view AS
SELECT 
    loan_id,
    customer_id,
    loan_type,
    loan_status,
    CASE 
        WHEN has_column_privilege('banking_loan.loans', 'principal_amount_encrypted', 'SELECT') 
        THEN banking_security.decrypt_pii(principal_amount_encrypted)::NUMERIC
        ELSE 0
    END AS principal_amount,
    CASE 
        WHEN has_column_privilege('banking_loan.loans', 'interest_rate_encrypted', 'SELECT') 
        THEN banking_security.decrypt_pii(interest_rate_encrypted)::NUMERIC
        ELSE 0
    END AS interest_rate,
    application_date,
    approval_date,
    loan_status,
    created_at,
    updated_at
FROM banking_loan.loans;

-- Payment view with decryption
CREATE OR REPLACE VIEW banking_payment.payment_view AS
SELECT 
    payment_id,
    loan_id,
    customer_id,
    CASE 
        WHEN has_column_privilege('banking_payment.payments', 'payment_amount_encrypted', 'SELECT') 
        THEN banking_security.decrypt_payment_data(payment_amount_encrypted)::NUMERIC
        ELSE 0
    END AS payment_amount,
    payment_date,
    payment_status,
    processing_date,
    settlement_date,
    created_at,
    updated_at
FROM banking_payment.payments;

-- =============================================
-- GRANT PERMISSIONS
-- =============================================

-- Grant schema permissions
GRANT USAGE ON SCHEMA banking_security TO banking_app_role, banking_readonly_role, banking_admin_role;
GRANT USAGE ON SCHEMA banking_audit TO banking_audit_role, banking_admin_role;
GRANT USAGE ON SCHEMA banking_compliance TO banking_compliance_role, banking_admin_role;

-- Grant function permissions
GRANT EXECUTE ON FUNCTION banking_security.encrypt_pii(TEXT, TEXT) TO banking_app_role;
GRANT EXECUTE ON FUNCTION banking_security.decrypt_pii(TEXT, TEXT) TO banking_app_role;
GRANT EXECUTE ON FUNCTION banking_security.encrypt_payment_data(TEXT, TEXT) TO banking_app_role;
GRANT EXECUTE ON FUNCTION banking_security.decrypt_payment_data(TEXT, TEXT) TO banking_app_role;

-- Grant view permissions
GRANT SELECT ON banking_customer.customer_view TO banking_app_role, banking_readonly_role;
GRANT SELECT ON banking_loan.loan_view TO banking_app_role, banking_readonly_role;
GRANT SELECT ON banking_payment.payment_view TO banking_app_role, banking_readonly_role;

-- Grant audit permissions
GRANT SELECT ON banking_audit.audit_log TO banking_audit_role, banking_admin_role;

-- =============================================
-- SAMPLE ENCRYPTED DATA
-- =============================================

-- Set encryption keys (these would come from Vault in production)
SET banking.encryption_key = 'sample_pii_encryption_key_32_chars';
SET banking.payment_encryption_key = 'sample_payment_encryption_key_32';
SET banking.user_id = 'SYSTEM_INIT';

-- Insert sample encrypted customer data
INSERT INTO banking_customer.customers (
    customer_type,
    first_name_encrypted,
    last_name_encrypted,
    email_encrypted,
    phone_encrypted,
    ssn_encrypted,
    date_of_birth_encrypted,
    email_hash,
    phone_hash,
    ssn_hash,
    annual_income_encrypted,
    credit_score_encrypted,
    kyc_status,
    compliance_level,
    risk_rating,
    created_by,
    updated_by
) VALUES (
    'INDIVIDUAL',
    banking_security.encrypt_pii('John'),
    banking_security.encrypt_pii('Doe'),
    banking_security.encrypt_pii('john.doe@example.com'),
    banking_security.encrypt_pii('+1-555-0123'),
    banking_security.encrypt_pii('123-45-6789'),
    banking_security.encrypt_pii('1985-05-15'),
    encode(digest('john.doe@example.com', 'sha256'), 'hex'),
    encode(digest('+1-555-0123', 'sha256'), 'hex'),
    encode(digest('123-45-6789', 'sha256'), 'hex'),
    banking_security.encrypt_pii('75000'),
    banking_security.encrypt_pii('750'),
    'VERIFIED',
    'HIGH',
    'MEDIUM',
    'SYSTEM_INIT',
    'SYSTEM_INIT'
);

-- Success message
SELECT 'Enterprise Banking Database Security Schema Created Successfully' AS status;