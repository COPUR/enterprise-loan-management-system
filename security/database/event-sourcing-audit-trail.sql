-- Event Sourcing for Enterprise Banking Audit Trail
-- Immutable event store with complete audit history and replay capabilities

-- =============================================
-- EVENT SOURCING SCHEMA
-- =============================================

-- Create event sourcing schema
CREATE SCHEMA IF NOT EXISTS banking_events;

-- Event store table (immutable)
CREATE TABLE banking_events.event_store (
    event_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    aggregate_id UUID NOT NULL,
    aggregate_type TEXT NOT NULL,
    event_type TEXT NOT NULL,
    event_version INTEGER NOT NULL,
    event_data JSONB NOT NULL,
    event_metadata JSONB DEFAULT '{}',
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    -- Audit fields
    caused_by TEXT NOT NULL,
    client_ip INET,
    user_agent TEXT,
    session_id TEXT,
    correlation_id UUID,
    
    -- Compliance fields
    compliance_tags TEXT[] DEFAULT ARRAY[]::TEXT[],
    retention_until TIMESTAMP WITH TIME ZONE,
    encryption_key_version INTEGER DEFAULT 1,
    
    -- Immutability constraints
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    -- Ensure events are append-only
    CONSTRAINT event_store_immutable CHECK (created_at = occurred_at)
);

-- Unique constraint on aggregate + version
CREATE UNIQUE INDEX idx_event_store_aggregate_version 
ON banking_events.event_store (aggregate_id, event_version);

-- Indexes for query performance
CREATE INDEX idx_event_store_aggregate_id ON banking_events.event_store (aggregate_id);
CREATE INDEX idx_event_store_aggregate_type ON banking_events.event_store (aggregate_type);
CREATE INDEX idx_event_store_event_type ON banking_events.event_store (event_type);
CREATE INDEX idx_event_store_occurred_at ON banking_events.event_store (occurred_at);
CREATE INDEX idx_event_store_caused_by ON banking_events.event_store (caused_by);
CREATE INDEX idx_event_store_correlation_id ON banking_events.event_store (correlation_id);

-- Gin index for JSONB queries
CREATE INDEX idx_event_store_event_data_gin ON banking_events.event_store USING gin (event_data);
CREATE INDEX idx_event_store_event_metadata_gin ON banking_events.event_store USING gin (event_metadata);

-- =============================================
-- AGGREGATE SNAPSHOTS
-- =============================================

-- Snapshot table for performance optimization
CREATE TABLE banking_events.aggregate_snapshots (
    snapshot_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    aggregate_id UUID NOT NULL,
    aggregate_type TEXT NOT NULL,
    snapshot_version INTEGER NOT NULL,
    snapshot_data JSONB NOT NULL,
    snapshot_metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    -- Unique constraint
    CONSTRAINT unique_aggregate_snapshot UNIQUE (aggregate_id, snapshot_version)
);

-- Indexes for snapshots
CREATE INDEX idx_aggregate_snapshots_aggregate_id ON banking_events.aggregate_snapshots (aggregate_id);
CREATE INDEX idx_aggregate_snapshots_aggregate_type ON banking_events.aggregate_snapshots (aggregate_type);
CREATE INDEX idx_aggregate_snapshots_created_at ON banking_events.aggregate_snapshots (created_at);

-- =============================================
-- EVENT PROJECTION TABLES
-- =============================================

-- Customer projection from events
CREATE TABLE banking_events.customer_projection (
    customer_id UUID PRIMARY KEY,
    current_version INTEGER NOT NULL,
    customer_type TEXT NOT NULL,
    customer_status TEXT NOT NULL,
    
    -- Projected fields
    first_name TEXT,
    last_name TEXT,
    email TEXT,
    phone TEXT,
    kyc_status TEXT,
    compliance_level TEXT,
    risk_rating TEXT,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_event_id UUID NOT NULL,
    
    -- Metadata
    projection_metadata JSONB DEFAULT '{}'
);

-- Loan projection from events
CREATE TABLE banking_events.loan_projection (
    loan_id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    current_version INTEGER NOT NULL,
    loan_type TEXT NOT NULL,
    loan_status TEXT NOT NULL,
    
    -- Projected fields
    principal_amount DECIMAL(18,2),
    interest_rate DECIMAL(5,4),
    term_months INTEGER,
    monthly_payment DECIMAL(18,2),
    
    -- Dates
    application_date DATE,
    approval_date DATE,
    disbursement_date DATE,
    maturity_date DATE,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_event_id UUID NOT NULL,
    
    -- Metadata
    projection_metadata JSONB DEFAULT '{}'
);

-- Payment projection from events
CREATE TABLE banking_events.payment_projection (
    payment_id UUID PRIMARY KEY,
    loan_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    current_version INTEGER NOT NULL,
    
    -- Projected fields
    payment_amount DECIMAL(18,2),
    payment_date DATE,
    payment_status TEXT,
    payment_method TEXT,
    
    -- Breakdown
    principal_amount DECIMAL(18,2),
    interest_amount DECIMAL(18,2),
    fees_amount DECIMAL(18,2),
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_event_id UUID NOT NULL,
    
    -- Metadata
    projection_metadata JSONB DEFAULT '{}'
);

-- =============================================
-- EVENT SOURCING FUNCTIONS
-- =============================================

-- Function to append events to the event store
CREATE OR REPLACE FUNCTION banking_events.append_event(
    p_aggregate_id UUID,
    p_aggregate_type TEXT,
    p_event_type TEXT,
    p_event_data JSONB,
    p_event_metadata JSONB DEFAULT '{}',
    p_expected_version INTEGER DEFAULT NULL,
    p_correlation_id UUID DEFAULT NULL
) RETURNS UUID AS $$
DECLARE
    v_event_id UUID;
    v_current_version INTEGER;
    v_new_version INTEGER;
    v_compliance_tags TEXT[];
BEGIN
    -- Get current version
    SELECT COALESCE(MAX(event_version), 0) INTO v_current_version
    FROM banking_events.event_store
    WHERE aggregate_id = p_aggregate_id;
    
    -- Check optimistic concurrency
    IF p_expected_version IS NOT NULL AND v_current_version != p_expected_version THEN
        RAISE EXCEPTION 'Concurrency conflict: expected version %, actual version %', 
            p_expected_version, v_current_version;
    END IF;
    
    -- Calculate new version
    v_new_version := v_current_version + 1;
    
    -- Determine compliance tags
    CASE p_aggregate_type
        WHEN 'Customer' THEN v_compliance_tags := ARRAY['PII', 'GDPR', 'KYC'];
        WHEN 'Loan' THEN v_compliance_tags := ARRAY['CREDIT_DATA', 'SOX', 'BASEL'];
        WHEN 'Payment' THEN v_compliance_tags := ARRAY['PAYMENT_DATA', 'PCI_DSS', 'AML'];
        ELSE v_compliance_tags := ARRAY['AUDIT'];
    END CASE;
    
    -- Insert event
    INSERT INTO banking_events.event_store (
        aggregate_id, aggregate_type, event_type, event_version,
        event_data, event_metadata, occurred_at,
        caused_by, client_ip, user_agent, session_id, correlation_id,
        compliance_tags, encryption_key_version
    ) VALUES (
        p_aggregate_id, p_aggregate_type, p_event_type, v_new_version,
        p_event_data, p_event_metadata, NOW(),
        COALESCE(current_setting('banking.user_id', true), current_user),
        COALESCE(inet_client_addr(), '127.0.0.1'::inet),
        COALESCE(current_setting('banking.user_agent', true), 'unknown'),
        COALESCE(current_setting('banking.session_id', true), 'unknown'),
        COALESCE(p_correlation_id, uuid_generate_v4()),
        v_compliance_tags, 1
    ) RETURNING event_id INTO v_event_id;
    
    -- Update projections
    PERFORM banking_events.update_projection(p_aggregate_id, p_aggregate_type, v_event_id);
    
    RETURN v_event_id;
END;
$$ LANGUAGE plpgsql;

-- Function to replay events and rebuild projections
CREATE OR REPLACE FUNCTION banking_events.replay_events(
    p_aggregate_id UUID,
    p_aggregate_type TEXT,
    p_from_version INTEGER DEFAULT 0,
    p_to_version INTEGER DEFAULT NULL
) RETURNS JSONB AS $$
DECLARE
    v_event RECORD;
    v_projection JSONB := '{}';
    v_events_processed INTEGER := 0;
BEGIN
    -- Get events in order
    FOR v_event IN
        SELECT event_id, event_type, event_version, event_data, occurred_at
        FROM banking_events.event_store
        WHERE aggregate_id = p_aggregate_id
        AND event_version > p_from_version
        AND (p_to_version IS NULL OR event_version <= p_to_version)
        ORDER BY event_version
    LOOP
        -- Apply event to projection
        v_projection := banking_events.apply_event(v_projection, v_event.event_type, v_event.event_data);
        v_events_processed := v_events_processed + 1;
    END LOOP;
    
    -- Add metadata
    v_projection := jsonb_set(v_projection, '{_metadata}', jsonb_build_object(
        'aggregate_id', p_aggregate_id,
        'aggregate_type', p_aggregate_type,
        'events_processed', v_events_processed,
        'replay_timestamp', NOW()
    ));
    
    RETURN v_projection;
END;
$$ LANGUAGE plpgsql;

-- Function to apply individual events to projections
CREATE OR REPLACE FUNCTION banking_events.apply_event(
    p_current_state JSONB,
    p_event_type TEXT,
    p_event_data JSONB
) RETURNS JSONB AS $$
DECLARE
    v_new_state JSONB;
BEGIN
    v_new_state := p_current_state;
    
    CASE p_event_type
        WHEN 'CustomerCreated' THEN
            v_new_state := jsonb_set(v_new_state, '{customer_id}', p_event_data->'customer_id');
            v_new_state := jsonb_set(v_new_state, '{customer_type}', p_event_data->'customer_type');
            v_new_state := jsonb_set(v_new_state, '{customer_status}', '"ACTIVE"');
            v_new_state := jsonb_set(v_new_state, '{first_name}', p_event_data->'first_name');
            v_new_state := jsonb_set(v_new_state, '{last_name}', p_event_data->'last_name');
            v_new_state := jsonb_set(v_new_state, '{email}', p_event_data->'email');
            v_new_state := jsonb_set(v_new_state, '{created_at}', to_jsonb(NOW()));
            
        WHEN 'CustomerUpdated' THEN
            IF p_event_data ? 'first_name' THEN
                v_new_state := jsonb_set(v_new_state, '{first_name}', p_event_data->'first_name');
            END IF;
            IF p_event_data ? 'last_name' THEN
                v_new_state := jsonb_set(v_new_state, '{last_name}', p_event_data->'last_name');
            END IF;
            IF p_event_data ? 'email' THEN
                v_new_state := jsonb_set(v_new_state, '{email}', p_event_data->'email');
            END IF;
            v_new_state := jsonb_set(v_new_state, '{updated_at}', to_jsonb(NOW()));
            
        WHEN 'CustomerSuspended' THEN
            v_new_state := jsonb_set(v_new_state, '{customer_status}', '"SUSPENDED"');
            v_new_state := jsonb_set(v_new_state, '{suspension_reason}', p_event_data->'reason');
            v_new_state := jsonb_set(v_new_state, '{updated_at}', to_jsonb(NOW()));
            
        WHEN 'CustomerActivated' THEN
            v_new_state := jsonb_set(v_new_state, '{customer_status}', '"ACTIVE"');
            v_new_state := jsonb_set(v_new_state, '{updated_at}', to_jsonb(NOW()));
            
        WHEN 'LoanApplicationSubmitted' THEN
            v_new_state := jsonb_set(v_new_state, '{loan_id}', p_event_data->'loan_id');
            v_new_state := jsonb_set(v_new_state, '{customer_id}', p_event_data->'customer_id');
            v_new_state := jsonb_set(v_new_state, '{loan_type}', p_event_data->'loan_type');
            v_new_state := jsonb_set(v_new_state, '{loan_status}', '"PENDING"');
            v_new_state := jsonb_set(v_new_state, '{principal_amount}', p_event_data->'principal_amount');
            v_new_state := jsonb_set(v_new_state, '{interest_rate}', p_event_data->'interest_rate');
            v_new_state := jsonb_set(v_new_state, '{term_months}', p_event_data->'term_months');
            v_new_state := jsonb_set(v_new_state, '{application_date}', p_event_data->'application_date');
            v_new_state := jsonb_set(v_new_state, '{created_at}', to_jsonb(NOW()));
            
        WHEN 'LoanApproved' THEN
            v_new_state := jsonb_set(v_new_state, '{loan_status}', '"APPROVED"');
            v_new_state := jsonb_set(v_new_state, '{approval_date}', p_event_data->'approval_date');
            v_new_state := jsonb_set(v_new_state, '{monthly_payment}', p_event_data->'monthly_payment');
            v_new_state := jsonb_set(v_new_state, '{updated_at}', to_jsonb(NOW()));
            
        WHEN 'LoanDisbursed' THEN
            v_new_state := jsonb_set(v_new_state, '{loan_status}', '"ACTIVE"');
            v_new_state := jsonb_set(v_new_state, '{disbursement_date}', p_event_data->'disbursement_date');
            v_new_state := jsonb_set(v_new_state, '{updated_at}', to_jsonb(NOW()));
            
        WHEN 'PaymentMade' THEN
            v_new_state := jsonb_set(v_new_state, '{payment_id}', p_event_data->'payment_id');
            v_new_state := jsonb_set(v_new_state, '{loan_id}', p_event_data->'loan_id');
            v_new_state := jsonb_set(v_new_state, '{payment_amount}', p_event_data->'payment_amount');
            v_new_state := jsonb_set(v_new_state, '{payment_date}', p_event_data->'payment_date');
            v_new_state := jsonb_set(v_new_state, '{payment_status}', '"COMPLETED"');
            v_new_state := jsonb_set(v_new_state, '{principal_amount}', p_event_data->'principal_amount');
            v_new_state := jsonb_set(v_new_state, '{interest_amount}', p_event_data->'interest_amount');
            v_new_state := jsonb_set(v_new_state, '{created_at}', to_jsonb(NOW()));
            
        ELSE
            -- Unknown event type, just update timestamp
            v_new_state := jsonb_set(v_new_state, '{updated_at}', to_jsonb(NOW()));
    END CASE;
    
    RETURN v_new_state;
END;
$$ LANGUAGE plpgsql;

-- Function to update projections based on events
CREATE OR REPLACE FUNCTION banking_events.update_projection(
    p_aggregate_id UUID,
    p_aggregate_type TEXT,
    p_event_id UUID
) RETURNS void AS $$
DECLARE
    v_projection JSONB;
BEGIN
    -- Replay events to get current state
    v_projection := banking_events.replay_events(p_aggregate_id, p_aggregate_type);
    
    -- Update appropriate projection table
    CASE p_aggregate_type
        WHEN 'Customer' THEN
            INSERT INTO banking_events.customer_projection (
                customer_id, current_version, customer_type, customer_status,
                first_name, last_name, email, phone, kyc_status, compliance_level, risk_rating,
                created_at, updated_at, last_event_id, projection_metadata
            ) VALUES (
                p_aggregate_id,
                (v_projection->'_metadata'->>'events_processed')::INTEGER,
                v_projection->>'customer_type',
                v_projection->>'customer_status',
                v_projection->>'first_name',
                v_projection->>'last_name',
                v_projection->>'email',
                v_projection->>'phone',
                COALESCE(v_projection->>'kyc_status', 'PENDING'),
                COALESCE(v_projection->>'compliance_level', 'BASIC'),
                COALESCE(v_projection->>'risk_rating', 'MEDIUM'),
                (v_projection->>'created_at')::TIMESTAMP WITH TIME ZONE,
                (v_projection->>'updated_at')::TIMESTAMP WITH TIME ZONE,
                p_event_id,
                v_projection->'_metadata'
            ) ON CONFLICT (customer_id) DO UPDATE SET
                current_version = EXCLUDED.current_version,
                customer_status = EXCLUDED.customer_status,
                first_name = EXCLUDED.first_name,
                last_name = EXCLUDED.last_name,
                email = EXCLUDED.email,
                phone = EXCLUDED.phone,
                kyc_status = EXCLUDED.kyc_status,
                compliance_level = EXCLUDED.compliance_level,
                risk_rating = EXCLUDED.risk_rating,
                updated_at = EXCLUDED.updated_at,
                last_event_id = EXCLUDED.last_event_id,
                projection_metadata = EXCLUDED.projection_metadata;
                
        WHEN 'Loan' THEN
            INSERT INTO banking_events.loan_projection (
                loan_id, customer_id, current_version, loan_type, loan_status,
                principal_amount, interest_rate, term_months, monthly_payment,
                application_date, approval_date, disbursement_date, maturity_date,
                created_at, updated_at, last_event_id, projection_metadata
            ) VALUES (
                p_aggregate_id,
                (v_projection->>'customer_id')::UUID,
                (v_projection->'_metadata'->>'events_processed')::INTEGER,
                v_projection->>'loan_type',
                v_projection->>'loan_status',
                (v_projection->>'principal_amount')::DECIMAL,
                (v_projection->>'interest_rate')::DECIMAL,
                (v_projection->>'term_months')::INTEGER,
                (v_projection->>'monthly_payment')::DECIMAL,
                (v_projection->>'application_date')::DATE,
                (v_projection->>'approval_date')::DATE,
                (v_projection->>'disbursement_date')::DATE,
                (v_projection->>'maturity_date')::DATE,
                (v_projection->>'created_at')::TIMESTAMP WITH TIME ZONE,
                (v_projection->>'updated_at')::TIMESTAMP WITH TIME ZONE,
                p_event_id,
                v_projection->'_metadata'
            ) ON CONFLICT (loan_id) DO UPDATE SET
                current_version = EXCLUDED.current_version,
                loan_status = EXCLUDED.loan_status,
                principal_amount = EXCLUDED.principal_amount,
                interest_rate = EXCLUDED.interest_rate,
                term_months = EXCLUDED.term_months,
                monthly_payment = EXCLUDED.monthly_payment,
                application_date = EXCLUDED.application_date,
                approval_date = EXCLUDED.approval_date,
                disbursement_date = EXCLUDED.disbursement_date,
                maturity_date = EXCLUDED.maturity_date,
                updated_at = EXCLUDED.updated_at,
                last_event_id = EXCLUDED.last_event_id,
                projection_metadata = EXCLUDED.projection_metadata;
                
        WHEN 'Payment' THEN
            INSERT INTO banking_events.payment_projection (
                payment_id, loan_id, customer_id, current_version,
                payment_amount, payment_date, payment_status, payment_method,
                principal_amount, interest_amount, fees_amount,
                created_at, updated_at, last_event_id, projection_metadata
            ) VALUES (
                p_aggregate_id,
                (v_projection->>'loan_id')::UUID,
                (v_projection->>'customer_id')::UUID,
                (v_projection->'_metadata'->>'events_processed')::INTEGER,
                (v_projection->>'payment_amount')::DECIMAL,
                (v_projection->>'payment_date')::DATE,
                v_projection->>'payment_status',
                v_projection->>'payment_method',
                (v_projection->>'principal_amount')::DECIMAL,
                (v_projection->>'interest_amount')::DECIMAL,
                (v_projection->>'fees_amount')::DECIMAL,
                (v_projection->>'created_at')::TIMESTAMP WITH TIME ZONE,
                (v_projection->>'updated_at')::TIMESTAMP WITH TIME ZONE,
                p_event_id,
                v_projection->'_metadata'
            ) ON CONFLICT (payment_id) DO UPDATE SET
                current_version = EXCLUDED.current_version,
                payment_amount = EXCLUDED.payment_amount,
                payment_date = EXCLUDED.payment_date,
                payment_status = EXCLUDED.payment_status,
                payment_method = EXCLUDED.payment_method,
                principal_amount = EXCLUDED.principal_amount,
                interest_amount = EXCLUDED.interest_amount,
                fees_amount = EXCLUDED.fees_amount,
                updated_at = EXCLUDED.updated_at,
                last_event_id = EXCLUDED.last_event_id,
                projection_metadata = EXCLUDED.projection_metadata;
    END CASE;
END;
$$ LANGUAGE plpgsql;

-- =============================================
-- EVENT STORE QUERIES
-- =============================================

-- Function to get events for an aggregate
CREATE OR REPLACE FUNCTION banking_events.get_events(
    p_aggregate_id UUID,
    p_from_version INTEGER DEFAULT 0,
    p_to_version INTEGER DEFAULT NULL
) RETURNS TABLE(
    event_id UUID,
    event_type TEXT,
    event_version INTEGER,
    event_data JSONB,
    occurred_at TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        es.event_id,
        es.event_type,
        es.event_version,
        es.event_data,
        es.occurred_at
    FROM banking_events.event_store es
    WHERE es.aggregate_id = p_aggregate_id
    AND es.event_version > p_from_version
    AND (p_to_version IS NULL OR es.event_version <= p_to_version)
    ORDER BY es.event_version;
END;
$$ LANGUAGE plpgsql;

-- Function to get events by correlation ID
CREATE OR REPLACE FUNCTION banking_events.get_events_by_correlation(
    p_correlation_id UUID
) RETURNS TABLE(
    event_id UUID,
    aggregate_id UUID,
    aggregate_type TEXT,
    event_type TEXT,
    event_data JSONB,
    occurred_at TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        es.event_id,
        es.aggregate_id,
        es.aggregate_type,
        es.event_type,
        es.event_data,
        es.occurred_at
    FROM banking_events.event_store es
    WHERE es.correlation_id = p_correlation_id
    ORDER BY es.occurred_at;
END;
$$ LANGUAGE plpgsql;

-- =============================================
-- SAMPLE EVENT DATA
-- =============================================

-- Sample customer creation event
SELECT banking_events.append_event(
    'f47ac10b-58cc-4372-a567-0e02b2c3d479'::UUID,
    'Customer',
    'CustomerCreated',
    '{"customer_id": "f47ac10b-58cc-4372-a567-0e02b2c3d479", "customer_type": "INDIVIDUAL", "first_name": "John", "last_name": "Doe", "email": "john.doe@example.com", "phone": "+1-555-0123"}'::JSONB,
    '{"source": "customer_service", "ip_address": "192.168.1.100"}'::JSONB,
    0,
    'correlation-123-456'::UUID
);

-- Sample loan application event
SELECT banking_events.append_event(
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890'::UUID,
    'Loan',
    'LoanApplicationSubmitted',
    '{"loan_id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890", "customer_id": "f47ac10b-58cc-4372-a567-0e02b2c3d479", "loan_type": "PERSONAL", "principal_amount": 50000.00, "interest_rate": 0.0575, "term_months": 60, "application_date": "2024-01-15"}'::JSONB,
    '{"source": "loan_service", "underwriter": "system"}'::JSONB,
    0,
    'correlation-123-456'::UUID
);

-- Sample payment event
SELECT banking_events.append_event(
    'p1a2y3m4-e5n6t-7890-abcd-ef1234567890'::UUID,
    'Payment',
    'PaymentMade',
    '{"payment_id": "p1a2y3m4-e5n6t-7890-abcd-ef1234567890", "loan_id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890", "customer_id": "f47ac10b-58cc-4372-a567-0e02b2c3d479", "payment_amount": 925.50, "payment_date": "2024-02-15", "principal_amount": 680.50, "interest_amount": 245.00, "fees_amount": 0.00}'::JSONB,
    '{"source": "payment_service", "payment_method": "ACH"}'::JSONB,
    0,
    'correlation-payment-789'::UUID
);

-- =============================================
-- GRANT PERMISSIONS
-- =============================================

-- Grant schema permissions
GRANT USAGE ON SCHEMA banking_events TO banking_app_role, banking_readonly_role, banking_admin_role;

-- Grant table permissions
GRANT SELECT, INSERT ON banking_events.event_store TO banking_app_role;
GRANT SELECT ON banking_events.event_store TO banking_readonly_role;
GRANT ALL ON banking_events.event_store TO banking_admin_role;

GRANT SELECT, INSERT, UPDATE, DELETE ON banking_events.aggregate_snapshots TO banking_app_role;
GRANT SELECT ON banking_events.aggregate_snapshots TO banking_readonly_role;

GRANT SELECT ON banking_events.customer_projection TO banking_app_role, banking_readonly_role;
GRANT SELECT ON banking_events.loan_projection TO banking_app_role, banking_readonly_role;
GRANT SELECT ON banking_events.payment_projection TO banking_app_role, banking_readonly_role;

-- Grant function permissions
GRANT EXECUTE ON FUNCTION banking_events.append_event(UUID, TEXT, TEXT, JSONB, JSONB, INTEGER, UUID) TO banking_app_role;
GRANT EXECUTE ON FUNCTION banking_events.replay_events(UUID, TEXT, INTEGER, INTEGER) TO banking_app_role, banking_admin_role;
GRANT EXECUTE ON FUNCTION banking_events.get_events(UUID, INTEGER, INTEGER) TO banking_app_role, banking_readonly_role;
GRANT EXECUTE ON FUNCTION banking_events.get_events_by_correlation(UUID) TO banking_app_role, banking_readonly_role;

-- Success message
SELECT 'Event Sourcing Audit Trail Implementation Completed Successfully' AS status;