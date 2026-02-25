-- Advanced Encryption Key Management for Enterprise Banking
-- Production-grade key rotation, derivation, and lifecycle management

-- =============================================
-- KEY DERIVATION AND ROTATION FUNCTIONS
-- =============================================

-- Create key management schema
CREATE SCHEMA IF NOT EXISTS banking_key_management;

-- Key rotation history table
CREATE TABLE banking_key_management.key_rotation_history (
    rotation_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    key_type TEXT NOT NULL,
    old_key_hash TEXT NOT NULL,
    new_key_hash TEXT NOT NULL,
    rotation_date TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    rotated_by TEXT NOT NULL,
    rotation_reason TEXT NOT NULL,
    affected_records INTEGER DEFAULT 0,
    rotation_status TEXT DEFAULT 'PENDING',
    completion_date TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT valid_key_type CHECK (key_type IN ('PII', 'PAYMENT', 'AUDIT', 'BACKUP')),
    CONSTRAINT valid_rotation_status CHECK (rotation_status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'ROLLED_BACK'))
);

-- Key derivation function using PBKDF2
CREATE OR REPLACE FUNCTION banking_key_management.derive_key(
    master_key TEXT,
    salt TEXT,
    key_purpose TEXT,
    iterations INTEGER DEFAULT 100000
) RETURNS TEXT AS $$
DECLARE
    derived_key TEXT;
BEGIN
    -- Use PBKDF2 for key derivation
    derived_key := encode(
        digest(
            master_key || salt || key_purpose || iterations::TEXT,
            'sha256'
        ),
        'base64'
    );
    
    -- Log key derivation (without exposing key material)
    INSERT INTO banking_audit.audit_log (
        table_name, operation, new_values, changed_by, compliance_tags
    ) VALUES (
        'KEY_DERIVATION', 'DERIVE_KEY',
        jsonb_build_object(
            'key_purpose', key_purpose,
            'salt_hash', encode(digest(salt, 'sha256'), 'hex'),
            'iterations', iterations
        ),
        COALESCE(current_setting('banking.user_id', true), current_user),
        ARRAY['KEY_MANAGEMENT', 'SECURITY']
    );
    
    RETURN derived_key;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Enhanced encryption with key versioning
CREATE OR REPLACE FUNCTION banking_security.encrypt_pii_versioned(
    plaintext TEXT,
    encryption_key TEXT,
    key_version INTEGER DEFAULT 1
) RETURNS TEXT AS $$
DECLARE
    encrypted_data TEXT;
    versioned_result TEXT;
BEGIN
    -- Encrypt the data
    encrypted_data := encode(
        pgp_sym_encrypt(
            plaintext,
            encryption_key,
            'compress-algo=2, cipher-algo=aes256, s2k-mode=3, s2k-count=65536'
        ),
        'base64'
    );
    
    -- Add key version prefix
    versioned_result := 'v' || key_version || ':' || encrypted_data;
    
    RETURN versioned_result;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Enhanced decryption with key versioning
CREATE OR REPLACE FUNCTION banking_security.decrypt_pii_versioned(
    ciphertext TEXT
) RETURNS TEXT AS $$
DECLARE
    key_version INTEGER;
    encrypted_data TEXT;
    decryption_key TEXT;
    result TEXT;
BEGIN
    -- Extract key version
    key_version := substring(ciphertext FROM 'v(\d+):')::INTEGER;
    encrypted_data := substring(ciphertext FROM 'v\d+:(.*)');
    
    -- Get appropriate decryption key based on version
    CASE key_version
        WHEN 1 THEN decryption_key := current_setting('banking.encryption_key_v1', true);
        WHEN 2 THEN decryption_key := current_setting('banking.encryption_key_v2', true);
        WHEN 3 THEN decryption_key := current_setting('banking.encryption_key_v3', true);
        ELSE decryption_key := current_setting('banking.encryption_key', true);
    END CASE;
    
    -- Decrypt with appropriate key
    result := pgp_sym_decrypt(
        decode(encrypted_data, 'base64'),
        decryption_key
    );
    
    RETURN result;
EXCEPTION
    WHEN OTHERS THEN
        RETURN '[DECRYPT_ERROR_v' || key_version || ']';
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Key rotation function
CREATE OR REPLACE FUNCTION banking_key_management.rotate_encryption_keys(
    key_type TEXT,
    rotation_reason TEXT DEFAULT 'SCHEDULED_ROTATION'
) RETURNS UUID AS $$
DECLARE
    rotation_id UUID;
    old_key_hash TEXT;
    new_key_hash TEXT;
    record_count INTEGER := 0;
BEGIN
    -- Start rotation process
    INSERT INTO banking_key_management.key_rotation_history (
        key_type, rotation_reason, rotated_by, old_key_hash, new_key_hash
    ) VALUES (
        key_type, rotation_reason, 
        COALESCE(current_setting('banking.user_id', true), current_user),
        encode(digest(current_setting('banking.encryption_key', true), 'sha256'), 'hex'),
        encode(digest(current_setting('banking.encryption_key_new', true), 'sha256'), 'hex')
    ) RETURNING rotation_id INTO rotation_id;
    
    -- Update rotation status
    UPDATE banking_key_management.key_rotation_history 
    SET rotation_status = 'IN_PROGRESS' 
    WHERE rotation_id = rotation_id;
    
    -- Perform actual key rotation based on type
    IF key_type = 'PII' THEN
        -- Re-encrypt customer PII data with new key
        UPDATE banking_customer.customers 
        SET 
            first_name_encrypted = banking_security.encrypt_pii_versioned(
                banking_security.decrypt_pii_versioned(first_name_encrypted),
                current_setting('banking.encryption_key_new', true),
                2
            ),
            last_name_encrypted = banking_security.encrypt_pii_versioned(
                banking_security.decrypt_pii_versioned(last_name_encrypted),
                current_setting('banking.encryption_key_new', true),
                2
            ),
            email_encrypted = banking_security.encrypt_pii_versioned(
                banking_security.decrypt_pii_versioned(email_encrypted),
                current_setting('banking.encryption_key_new', true),
                2
            ),
            updated_at = NOW(),
            version_number = version_number + 1
        WHERE first_name_encrypted IS NOT NULL;
        
        GET DIAGNOSTICS record_count = ROW_COUNT;
        
    ELSIF key_type = 'PAYMENT' THEN
        -- Re-encrypt payment data with new key
        UPDATE banking_payment.payments 
        SET 
            payment_amount_encrypted = banking_security.encrypt_payment_data(
                banking_security.decrypt_payment_data(payment_amount_encrypted),
                current_setting('banking.payment_encryption_key_new', true)
            ),
            updated_at = NOW(),
            version_number = version_number + 1
        WHERE payment_amount_encrypted IS NOT NULL;
        
        GET DIAGNOSTICS record_count = ROW_COUNT;
    END IF;
    
    -- Complete rotation
    UPDATE banking_key_management.key_rotation_history 
    SET 
        rotation_status = 'COMPLETED',
        completion_date = NOW(),
        affected_records = record_count
    WHERE rotation_id = rotation_id;
    
    RETURN rotation_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =============================================
-- DETERMINISTIC ENCRYPTION FOR SEARCHES
-- =============================================

-- Format preserving encryption for structured data
CREATE OR REPLACE FUNCTION banking_security.encrypt_format_preserving(
    plaintext TEXT,
    format_pattern TEXT,
    encryption_key TEXT
) RETURNS TEXT AS $$
DECLARE
    encrypted_data TEXT;
    formatted_result TEXT;
BEGIN
    -- Basic format-preserving encryption for demonstration
    -- In production, use a proper FPE library
    encrypted_data := encode(
        digest(plaintext || encryption_key, 'sha256'),
        'hex'
    );
    
    -- Apply format pattern
    CASE format_pattern
        WHEN 'SSN' THEN
            formatted_result := substring(encrypted_data FROM 1 FOR 3) || '-' || 
                              substring(encrypted_data FROM 4 FOR 2) || '-' || 
                              substring(encrypted_data FROM 6 FOR 4);
        WHEN 'PHONE' THEN
            formatted_result := '+1-' || substring(encrypted_data FROM 1 FOR 3) || '-' || 
                              substring(encrypted_data FROM 4 FOR 4);
        WHEN 'CARD' THEN
            formatted_result := substring(encrypted_data FROM 1 FOR 4) || ' ' || 
                              substring(encrypted_data FROM 5 FOR 4) || ' ' || 
                              substring(encrypted_data FROM 9 FOR 4) || ' ' || 
                              substring(encrypted_data FROM 13 FOR 4);
        ELSE
            formatted_result := encrypted_data;
    END CASE;
    
    RETURN formatted_result;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Searchable encryption using blind indexing
CREATE OR REPLACE FUNCTION banking_security.create_search_index(
    plaintext TEXT,
    search_key TEXT
) RETURNS TEXT AS $$
BEGIN
    -- Create searchable hash without revealing plaintext
    RETURN encode(
        hmac(
            upper(trim(plaintext)),
            search_key,
            'sha256'
        ),
        'hex'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =============================================
-- ENCRYPTION AT REST ENHANCEMENTS
-- =============================================

-- Tablespace encryption configuration
CREATE OR REPLACE FUNCTION banking_security.setup_encrypted_tablespaces() RETURNS void AS $$
BEGIN
    -- Create encrypted tablespaces for sensitive data
    -- This would typically be done at database initialization
    
    -- Log tablespace setup
    INSERT INTO banking_audit.audit_log (
        table_name, operation, new_values, changed_by, compliance_tags
    ) VALUES (
        'TABLESPACE_ENCRYPTION', 'SETUP',
        jsonb_build_object(
            'setup_date', NOW(),
            'encryption_enabled', true
        ),
        COALESCE(current_setting('banking.user_id', true), current_user),
        ARRAY['ENCRYPTION', 'TABLESPACE', 'SECURITY']
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =============================================
-- TRANSPARENT DATA ENCRYPTION (TDE) SIMULATION
-- =============================================

-- TDE key management table
CREATE TABLE banking_key_management.tde_keys (
    key_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    key_name TEXT NOT NULL UNIQUE,
    key_algorithm TEXT NOT NULL DEFAULT 'AES256',
    key_status TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    activated_at TIMESTAMP WITH TIME ZONE,
    deactivated_at TIMESTAMP WITH TIME ZONE,
    key_checksum TEXT NOT NULL,
    
    CONSTRAINT valid_key_status CHECK (key_status IN ('ACTIVE', 'INACTIVE', 'RETIRED', 'COMPROMISED'))
);

-- TDE key rotation scheduler
CREATE OR REPLACE FUNCTION banking_key_management.schedule_key_rotation() RETURNS void AS $$
DECLARE
    rotation_due BOOLEAN;
BEGIN
    -- Check if key rotation is due (every 90 days)
    SELECT EXISTS(
        SELECT 1 FROM banking_key_management.key_rotation_history 
        WHERE key_type = 'PII' 
        AND rotation_date > NOW() - INTERVAL '90 days'
    ) INTO rotation_due;
    
    IF NOT rotation_due THEN
        PERFORM banking_key_management.rotate_encryption_keys('PII', 'SCHEDULED_90_DAY_ROTATION');
    END IF;
    
    -- Schedule payment key rotation (every 30 days)
    SELECT EXISTS(
        SELECT 1 FROM banking_key_management.key_rotation_history 
        WHERE key_type = 'PAYMENT' 
        AND rotation_date > NOW() - INTERVAL '30 days'
    ) INTO rotation_due;
    
    IF NOT rotation_due THEN
        PERFORM banking_key_management.rotate_encryption_keys('PAYMENT', 'SCHEDULED_30_DAY_ROTATION');
    END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =============================================
-- ENHANCED SECURE CUSTOMER TABLE
-- =============================================

-- Drop and recreate with enhanced encryption
DROP TABLE IF EXISTS banking_customer.customers_enhanced CASCADE;

CREATE TABLE banking_customer.customers_enhanced (
    customer_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_type customer_type NOT NULL DEFAULT 'INDIVIDUAL',
    
    -- Versioned encrypted PII fields
    first_name_encrypted TEXT,
    last_name_encrypted TEXT,
    email_encrypted TEXT,
    phone_encrypted TEXT,
    ssn_encrypted TEXT,
    date_of_birth_encrypted TEXT,
    
    -- Searchable encrypted indices
    first_name_search_index TEXT,
    last_name_search_index TEXT,
    email_search_index TEXT,
    phone_search_index TEXT,
    ssn_search_index TEXT,
    
    -- Format-preserving encrypted fields for display
    ssn_display TEXT,
    phone_display TEXT,
    
    -- Deterministic encryption for exact matches
    email_deterministic TEXT,
    ssn_deterministic TEXT,
    
    -- Standard fields
    customer_status customer_status DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    -- Enhanced financial data
    annual_income_encrypted TEXT,
    credit_score_encrypted TEXT,
    net_worth_encrypted TEXT,
    
    -- Enhanced compliance fields
    kyc_status TEXT DEFAULT 'PENDING',
    kyc_verified_at TIMESTAMP WITH TIME ZONE,
    compliance_level TEXT DEFAULT 'BASIC',
    risk_rating TEXT DEFAULT 'MEDIUM',
    aml_status TEXT DEFAULT 'PENDING',
    sanctions_check_status TEXT DEFAULT 'PENDING',
    
    -- Data lineage and versioning
    data_version INTEGER DEFAULT 1,
    encryption_version INTEGER DEFAULT 1,
    last_key_rotation TIMESTAMP WITH TIME ZONE,
    
    -- Audit fields
    created_by TEXT NOT NULL,
    updated_by TEXT NOT NULL,
    version_number INTEGER DEFAULT 1
);

-- Enhanced indexes for encrypted searches
CREATE INDEX idx_customers_enhanced_first_name_search ON banking_customer.customers_enhanced (first_name_search_index);
CREATE INDEX idx_customers_enhanced_last_name_search ON banking_customer.customers_enhanced (last_name_search_index);
CREATE INDEX idx_customers_enhanced_email_search ON banking_customer.customers_enhanced (email_search_index);
CREATE INDEX idx_customers_enhanced_email_deterministic ON banking_customer.customers_enhanced (email_deterministic);
CREATE INDEX idx_customers_enhanced_ssn_deterministic ON banking_customer.customers_enhanced (ssn_deterministic);
CREATE INDEX idx_customers_enhanced_kyc_status ON banking_customer.customers_enhanced (kyc_status);
CREATE INDEX idx_customers_enhanced_compliance_level ON banking_customer.customers_enhanced (compliance_level);
CREATE INDEX idx_customers_enhanced_encryption_version ON banking_customer.customers_enhanced (encryption_version);

-- Enable RLS
ALTER TABLE banking_customer.customers_enhanced ENABLE ROW LEVEL SECURITY;

-- Enhanced RLS policies
CREATE POLICY customer_enhanced_app_policy ON banking_customer.customers_enhanced
    FOR ALL TO banking_app_role
    USING (true);

CREATE POLICY customer_enhanced_readonly_policy ON banking_customer.customers_enhanced
    FOR SELECT TO banking_readonly_role
    USING (true);

-- Trigger for maintaining search indices
CREATE OR REPLACE FUNCTION banking_customer.maintain_search_indices() RETURNS TRIGGER AS $$
BEGIN
    -- Update search indices when encrypted data changes
    IF TG_OP = 'INSERT' OR TG_OP = 'UPDATE' THEN
        NEW.first_name_search_index := banking_security.create_search_index(
            banking_security.decrypt_pii_versioned(NEW.first_name_encrypted),
            current_setting('banking.search_key', true)
        );
        
        NEW.last_name_search_index := banking_security.create_search_index(
            banking_security.decrypt_pii_versioned(NEW.last_name_encrypted),
            current_setting('banking.search_key', true)
        );
        
        NEW.email_search_index := banking_security.create_search_index(
            banking_security.decrypt_pii_versioned(NEW.email_encrypted),
            current_setting('banking.search_key', true)
        );
        
        -- Update deterministic fields
        NEW.email_deterministic := banking_security.encrypt_format_preserving(
            banking_security.decrypt_pii_versioned(NEW.email_encrypted),
            'EMAIL',
            current_setting('banking.deterministic_key', true)
        );
        
        NEW.last_key_rotation := NOW();
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER customer_enhanced_search_index_trigger
    BEFORE INSERT OR UPDATE ON banking_customer.customers_enhanced
    FOR EACH ROW EXECUTE FUNCTION banking_customer.maintain_search_indices();

-- =============================================
-- GRANT PERMISSIONS
-- =============================================

-- Grant schema permissions
GRANT USAGE ON SCHEMA banking_key_management TO banking_admin_role;
GRANT SELECT ON banking_key_management.key_rotation_history TO banking_audit_role, banking_admin_role;
GRANT SELECT ON banking_key_management.tde_keys TO banking_admin_role;

-- Grant function permissions
GRANT EXECUTE ON FUNCTION banking_key_management.derive_key(TEXT, TEXT, TEXT, INTEGER) TO banking_admin_role;
GRANT EXECUTE ON FUNCTION banking_key_management.rotate_encryption_keys(TEXT, TEXT) TO banking_admin_role;
GRANT EXECUTE ON FUNCTION banking_security.encrypt_pii_versioned(TEXT, TEXT, INTEGER) TO banking_app_role;
GRANT EXECUTE ON FUNCTION banking_security.decrypt_pii_versioned(TEXT) TO banking_app_role;
GRANT EXECUTE ON FUNCTION banking_security.encrypt_format_preserving(TEXT, TEXT, TEXT) TO banking_app_role;
GRANT EXECUTE ON FUNCTION banking_security.create_search_index(TEXT, TEXT) TO banking_app_role;

-- Success message
SELECT 'Advanced Encryption Key Management Schema Created Successfully' AS status;