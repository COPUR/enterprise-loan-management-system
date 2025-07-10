-- Enterprise Banking Database Sharding and Partitioning Strategy
-- Production-grade horizontal scaling with data sovereignty compliance

-- =============================================
-- PARTITIONING STRATEGY IMPLEMENTATION
-- =============================================

-- Create partitioning management schema
CREATE SCHEMA IF NOT EXISTS banking_partitioning;

-- Partition metadata table
CREATE TABLE banking_partitioning.partition_metadata (
    partition_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    table_name TEXT NOT NULL,
    partition_name TEXT NOT NULL,
    partition_type TEXT NOT NULL,
    partition_key TEXT NOT NULL,
    partition_range TEXT,
    partition_list TEXT,
    shard_id INTEGER,
    region TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_maintenance TIMESTAMP WITH TIME ZONE,
    record_count BIGINT DEFAULT 0,
    storage_size_mb BIGINT DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    
    CONSTRAINT valid_partition_type CHECK (partition_type IN ('RANGE', 'LIST', 'HASH', 'COMPOSITE'))
);

-- Shard configuration table
CREATE TABLE banking_partitioning.shard_configuration (
    shard_id INTEGER PRIMARY KEY,
    shard_name TEXT NOT NULL UNIQUE,
    region TEXT NOT NULL,
    database_host TEXT NOT NULL,
    database_port INTEGER NOT NULL DEFAULT 5432,
    database_name TEXT NOT NULL,
    connection_pool_size INTEGER DEFAULT 20,
    max_connections INTEGER DEFAULT 100,
    is_master BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_health_check TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT valid_region CHECK (region IN ('us-east-1', 'us-west-2', 'eu-west-1', 'ap-southeast-1'))
);

-- =============================================
-- CUSTOMER DATA PARTITIONING BY REGION
-- =============================================

-- Drop existing table to recreate with partitioning
DROP TABLE IF EXISTS banking_customer.customers_partitioned CASCADE;

-- Create partitioned customer table
CREATE TABLE banking_customer.customers_partitioned (
    customer_id UUID NOT NULL,
    customer_type customer_type NOT NULL DEFAULT 'INDIVIDUAL',
    region TEXT NOT NULL,
    
    -- Encrypted PII fields
    first_name_encrypted TEXT,
    last_name_encrypted TEXT,
    email_encrypted TEXT,
    phone_encrypted TEXT,
    ssn_encrypted TEXT,
    date_of_birth_encrypted TEXT,
    
    -- Searchable indices
    first_name_search_index TEXT,
    last_name_search_index TEXT,
    email_search_index TEXT,
    
    -- Customer details
    customer_status customer_status DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    -- Financial data
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
    
    PRIMARY KEY (customer_id, region)
) PARTITION BY LIST (region);

-- Create regional partitions for customers
CREATE TABLE banking_customer.customers_us_east_1 PARTITION OF banking_customer.customers_partitioned
    FOR VALUES IN ('us-east-1');

CREATE TABLE banking_customer.customers_us_west_2 PARTITION OF banking_customer.customers_partitioned
    FOR VALUES IN ('us-west-2');

CREATE TABLE banking_customer.customers_eu_west_1 PARTITION OF banking_customer.customers_partitioned
    FOR VALUES IN ('eu-west-1');

CREATE TABLE banking_customer.customers_ap_southeast_1 PARTITION OF banking_customer.customers_partitioned
    FOR VALUES IN ('ap-southeast-1');

-- Create indexes on partitioned table
CREATE INDEX idx_customers_partitioned_id ON banking_customer.customers_partitioned (customer_id);
CREATE INDEX idx_customers_partitioned_email_search ON banking_customer.customers_partitioned (email_search_index);
CREATE INDEX idx_customers_partitioned_status ON banking_customer.customers_partitioned (customer_status);
CREATE INDEX idx_customers_partitioned_created_at ON banking_customer.customers_partitioned (created_at);

-- =============================================
-- LOAN DATA PARTITIONING BY DATE
-- =============================================

-- Create partitioned loan table
CREATE TABLE banking_loan.loans_partitioned (
    loan_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    region TEXT NOT NULL,
    application_date DATE NOT NULL,
    
    -- Loan details
    loan_type loan_type NOT NULL,
    loan_status loan_status DEFAULT 'PENDING',
    
    -- Encrypted financial data
    principal_amount_encrypted TEXT NOT NULL,
    interest_rate_encrypted TEXT NOT NULL,
    term_months_encrypted TEXT NOT NULL,
    monthly_payment_encrypted TEXT,
    
    -- Loan details
    purpose_of_loan TEXT NOT NULL,
    collateral_description_encrypted TEXT,
    collateral_value_encrypted TEXT,
    
    -- Dates
    approval_date DATE,
    disbursement_date DATE,
    maturity_date DATE,
    
    -- Risk and compliance
    risk_score_encrypted TEXT,
    credit_decision_encrypted TEXT,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by TEXT NOT NULL,
    updated_by TEXT NOT NULL,
    version_number INTEGER DEFAULT 1,
    
    PRIMARY KEY (loan_id, application_date, region)
) PARTITION BY RANGE (application_date);

-- Create monthly partitions for loans (example for 2024)
CREATE TABLE banking_loan.loans_2024_01 PARTITION OF banking_loan.loans_partitioned
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

CREATE TABLE banking_loan.loans_2024_02 PARTITION OF banking_loan.loans_partitioned
    FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');

CREATE TABLE banking_loan.loans_2024_03 PARTITION OF banking_loan.loans_partitioned
    FOR VALUES FROM ('2024-03-01') TO ('2024-04-01');

CREATE TABLE banking_loan.loans_2024_04 PARTITION OF banking_loan.loans_partitioned
    FOR VALUES FROM ('2024-04-01') TO ('2024-05-01');

CREATE TABLE banking_loan.loans_2024_05 PARTITION OF banking_loan.loans_partitioned
    FOR VALUES FROM ('2024-05-01') TO ('2024-06-01');

CREATE TABLE banking_loan.loans_2024_06 PARTITION OF banking_loan.loans_partitioned
    FOR VALUES FROM ('2024-06-01') TO ('2024-07-01');

CREATE TABLE banking_loan.loans_2024_07 PARTITION OF banking_loan.loans_partitioned
    FOR VALUES FROM ('2024-07-01') TO ('2024-08-01');

CREATE TABLE banking_loan.loans_2024_08 PARTITION OF banking_loan.loans_partitioned
    FOR VALUES FROM ('2024-08-01') TO ('2024-09-01');

CREATE TABLE banking_loan.loans_2024_09 PARTITION OF banking_loan.loans_partitioned
    FOR VALUES FROM ('2024-09-01') TO ('2024-10-01');

CREATE TABLE banking_loan.loans_2024_10 PARTITION OF banking_loan.loans_partitioned
    FOR VALUES FROM ('2024-10-01') TO ('2024-11-01');

CREATE TABLE banking_loan.loans_2024_11 PARTITION OF banking_loan.loans_partitioned
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');

CREATE TABLE banking_loan.loans_2024_12 PARTITION OF banking_loan.loans_partitioned
    FOR VALUES FROM ('2024-12-01') TO ('2025-01-01');

-- Create indexes on partitioned loan table
CREATE INDEX idx_loans_partitioned_customer_id ON banking_loan.loans_partitioned (customer_id);
CREATE INDEX idx_loans_partitioned_status ON banking_loan.loans_partitioned (loan_status);
CREATE INDEX idx_loans_partitioned_type ON banking_loan.loans_partitioned (loan_type);
CREATE INDEX idx_loans_partitioned_region ON banking_loan.loans_partitioned (region);

-- =============================================
-- PAYMENT DATA PARTITIONING BY DATE AND HASH
-- =============================================

-- Create composite partitioned payment table
CREATE TABLE banking_payment.payments_partitioned (
    payment_id UUID NOT NULL,
    loan_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    payment_date DATE NOT NULL,
    customer_hash INTEGER NOT NULL,
    
    -- Payment details
    payment_amount_encrypted TEXT NOT NULL,
    payment_method_encrypted TEXT NOT NULL,
    
    -- Payment breakdown
    principal_amount_encrypted TEXT,
    interest_amount_encrypted TEXT,
    fees_amount_encrypted TEXT,
    
    -- Payment source
    source_account_encrypted TEXT,
    routing_number_encrypted TEXT,
    payment_reference_encrypted TEXT,
    
    -- Status and processing
    payment_status payment_status DEFAULT 'PENDING',
    processing_date TIMESTAMP WITH TIME ZONE,
    settlement_date DATE,
    
    -- Risk and fraud
    fraud_score_encrypted TEXT,
    risk_indicators_encrypted TEXT,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by TEXT NOT NULL,
    updated_by TEXT NOT NULL,
    version_number INTEGER DEFAULT 1,
    
    PRIMARY KEY (payment_id, payment_date, customer_hash)
) PARTITION BY RANGE (payment_date);

-- Create payment partitions (monthly with hash sub-partitioning)
CREATE TABLE banking_payment.payments_2024_01 PARTITION OF banking_payment.payments_partitioned
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01')
    PARTITION BY HASH (customer_hash);

CREATE TABLE banking_payment.payments_2024_01_h0 PARTITION OF banking_payment.payments_2024_01
    FOR VALUES WITH (MODULUS 4, REMAINDER 0);

CREATE TABLE banking_payment.payments_2024_01_h1 PARTITION OF banking_payment.payments_2024_01
    FOR VALUES WITH (MODULUS 4, REMAINDER 1);

CREATE TABLE banking_payment.payments_2024_01_h2 PARTITION OF banking_payment.payments_2024_01
    FOR VALUES WITH (MODULUS 4, REMAINDER 2);

CREATE TABLE banking_payment.payments_2024_01_h3 PARTITION OF banking_payment.payments_2024_01
    FOR VALUES WITH (MODULUS 4, REMAINDER 3);

-- Create indexes on partitioned payment table
CREATE INDEX idx_payments_partitioned_loan_id ON banking_payment.payments_partitioned (loan_id);
CREATE INDEX idx_payments_partitioned_customer_id ON banking_payment.payments_partitioned (customer_id);
CREATE INDEX idx_payments_partitioned_status ON banking_payment.payments_partitioned (payment_status);

-- =============================================
-- AUDIT LOG PARTITIONING BY DATE
-- =============================================

-- Create partitioned audit log table
CREATE TABLE banking_audit.audit_log_partitioned (
    audit_id UUID NOT NULL,
    table_name TEXT NOT NULL,
    operation TEXT NOT NULL,
    old_values JSONB,
    new_values JSONB,
    changed_by TEXT NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    client_ip INET,
    application_name TEXT,
    transaction_id TEXT,
    compliance_tags TEXT[],
    
    PRIMARY KEY (audit_id, changed_at)
) PARTITION BY RANGE (changed_at);

-- Create monthly audit partitions
CREATE TABLE banking_audit.audit_log_2024_01 PARTITION OF banking_audit.audit_log_partitioned
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

CREATE TABLE banking_audit.audit_log_2024_02 PARTITION OF banking_audit.audit_log_partitioned
    FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');

CREATE TABLE banking_audit.audit_log_2024_03 PARTITION OF banking_audit.audit_log_partitioned
    FOR VALUES FROM ('2024-03-01') TO ('2024-04-01');

-- Create indexes on partitioned audit log
CREATE INDEX idx_audit_log_partitioned_table_operation ON banking_audit.audit_log_partitioned (table_name, operation);
CREATE INDEX idx_audit_log_partitioned_changed_by ON banking_audit.audit_log_partitioned (changed_by);

-- =============================================
-- AUTOMATIC PARTITION MANAGEMENT
-- =============================================

-- Function to create new partitions automatically
CREATE OR REPLACE FUNCTION banking_partitioning.create_monthly_partitions(
    table_name TEXT,
    schema_name TEXT,
    start_date DATE,
    end_date DATE
) RETURNS INTEGER AS $$
DECLARE
    current_date DATE;
    partition_name TEXT;
    partition_sql TEXT;
    partitions_created INTEGER := 0;
BEGIN
    current_date := start_date;
    
    WHILE current_date < end_date LOOP
        -- Generate partition name
        partition_name := table_name || '_' || to_char(current_date, 'YYYY_MM');
        
        -- Create partition SQL
        partition_sql := format(
            'CREATE TABLE IF NOT EXISTS %I.%I PARTITION OF %I.%I FOR VALUES FROM (%L) TO (%L)',
            schema_name, partition_name, schema_name, table_name,
            current_date, current_date + INTERVAL '1 month'
        );
        
        -- Execute partition creation
        EXECUTE partition_sql;
        
        -- Update metadata
        INSERT INTO banking_partitioning.partition_metadata (
            table_name, partition_name, partition_type, partition_key, partition_range
        ) VALUES (
            schema_name || '.' || table_name,
            partition_name,
            'RANGE',
            'date_column',
            current_date || ' TO ' || (current_date + INTERVAL '1 month')
        ) ON CONFLICT DO NOTHING;
        
        partitions_created := partitions_created + 1;
        current_date := current_date + INTERVAL '1 month';
    END LOOP;
    
    RETURN partitions_created;
END;
$$ LANGUAGE plpgsql;

-- Function to drop old partitions
CREATE OR REPLACE FUNCTION banking_partitioning.drop_old_partitions(
    table_name TEXT,
    schema_name TEXT,
    retention_months INTEGER DEFAULT 84  -- 7 years
) RETURNS INTEGER AS $$
DECLARE
    partition_record RECORD;
    partition_sql TEXT;
    partitions_dropped INTEGER := 0;
BEGIN
    FOR partition_record IN
        SELECT partition_name
        FROM banking_partitioning.partition_metadata
        WHERE table_name = schema_name || '.' || table_name
        AND created_at < NOW() - (retention_months || ' months')::INTERVAL
    LOOP
        -- Drop partition
        partition_sql := format('DROP TABLE IF EXISTS %I.%I', schema_name, partition_record.partition_name);
        EXECUTE partition_sql;
        
        -- Remove from metadata
        DELETE FROM banking_partitioning.partition_metadata
        WHERE partition_name = partition_record.partition_name;
        
        partitions_dropped := partitions_dropped + 1;
    END LOOP;
    
    RETURN partitions_dropped;
END;
$$ LANGUAGE plpgsql;

-- Function to analyze partition performance
CREATE OR REPLACE FUNCTION banking_partitioning.analyze_partition_performance(
    table_name TEXT,
    schema_name TEXT
) RETURNS TABLE(
    partition_name TEXT,
    record_count BIGINT,
    storage_size_mb BIGINT,
    avg_query_time_ms NUMERIC,
    last_analyzed TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        pm.partition_name,
        pm.record_count,
        pm.storage_size_mb,
        CASE 
            WHEN pm.record_count > 0 THEN 
                (pm.storage_size_mb::NUMERIC / pm.record_count * 1000)
            ELSE 0
        END as avg_query_time_ms,
        pm.last_maintenance
    FROM banking_partitioning.partition_metadata pm
    WHERE pm.table_name = schema_name || '.' || table_name
    ORDER BY pm.created_at DESC;
END;
$$ LANGUAGE plpgsql;

-- =============================================
-- SHARDING MANAGEMENT FUNCTIONS
-- =============================================

-- Function to determine shard for customer
CREATE OR REPLACE FUNCTION banking_partitioning.get_customer_shard(
    customer_id UUID
) RETURNS INTEGER AS $$
BEGIN
    -- Hash-based sharding
    RETURN (hashtext(customer_id::TEXT) % 4) + 1;
END;
$$ LANGUAGE plpgsql;

-- Function to route query to appropriate shard
CREATE OR REPLACE FUNCTION banking_partitioning.route_to_shard(
    shard_id INTEGER,
    query_sql TEXT
) RETURNS TEXT AS $$
DECLARE
    shard_config RECORD;
    connection_string TEXT;
BEGIN
    -- Get shard configuration
    SELECT * INTO shard_config
    FROM banking_partitioning.shard_configuration
    WHERE shard_id = shard_id AND is_active = true;
    
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Shard % not found or inactive', shard_id;
    END IF;
    
    -- Build connection string
    connection_string := format(
        'host=%s port=%s dbname=%s',
        shard_config.database_host,
        shard_config.database_port,
        shard_config.database_name
    );
    
    -- Return connection info (in real implementation, this would execute the query)
    RETURN connection_string || ' | ' || query_sql;
END;
$$ LANGUAGE plpgsql;

-- =============================================
-- PARTITION MAINTENANCE JOBS
-- =============================================

-- Maintenance job for partition management
CREATE OR REPLACE FUNCTION banking_partitioning.maintenance_job() RETURNS void AS $$
DECLARE
    tables_to_maintain TEXT[] := ARRAY[
        'banking_loan.loans_partitioned',
        'banking_payment.payments_partitioned',
        'banking_audit.audit_log_partitioned'
    ];
    table_name TEXT;
    schema_name TEXT;
    base_table_name TEXT;
BEGIN
    FOREACH table_name IN ARRAY tables_to_maintain
    LOOP
        -- Parse schema and table name
        schema_name := split_part(table_name, '.', 1);
        base_table_name := split_part(table_name, '.', 2);
        
        -- Create partitions for next 3 months
        PERFORM banking_partitioning.create_monthly_partitions(
            base_table_name,
            schema_name,
            DATE_TRUNC('month', NOW()),
            DATE_TRUNC('month', NOW() + INTERVAL '3 months')
        );
        
        -- Drop old partitions (keep 7 years)
        PERFORM banking_partitioning.drop_old_partitions(
            base_table_name,
            schema_name,
            84
        );
    END LOOP;
    
    -- Update partition statistics
    ANALYZE banking_customer.customers_partitioned;
    ANALYZE banking_loan.loans_partitioned;
    ANALYZE banking_payment.payments_partitioned;
    ANALYZE banking_audit.audit_log_partitioned;
    
    -- Log maintenance completion
    INSERT INTO banking_audit.audit_log (
        table_name, operation, new_values, changed_by, compliance_tags
    ) VALUES (
        'PARTITION_MAINTENANCE', 'MAINTENANCE_JOB',
        jsonb_build_object('completed_at', NOW()),
        'SYSTEM_MAINTENANCE',
        ARRAY['PARTITION_MAINTENANCE', 'SYSTEM']
    );
END;
$$ LANGUAGE plpgsql;

-- =============================================
-- VIEWS FOR CROSS-PARTITION QUERIES
-- =============================================

-- View for customer search across partitions
CREATE OR REPLACE VIEW banking_customer.customer_search_view AS
SELECT 
    customer_id,
    customer_type,
    region,
    customer_status,
    kyc_status,
    compliance_level,
    risk_rating,
    created_at,
    updated_at
FROM banking_customer.customers_partitioned
WHERE customer_status = 'ACTIVE';

-- View for loan analytics across partitions
CREATE OR REPLACE VIEW banking_loan.loan_analytics_view AS
SELECT 
    loan_id,
    customer_id,
    region,
    loan_type,
    loan_status,
    application_date,
    approval_date,
    created_at,
    DATE_PART('month', application_date) as application_month,
    DATE_PART('year', application_date) as application_year
FROM banking_loan.loans_partitioned;

-- View for payment reporting across partitions
CREATE OR REPLACE VIEW banking_payment.payment_reporting_view AS
SELECT 
    payment_id,
    loan_id,
    customer_id,
    payment_date,
    payment_status,
    processing_date,
    settlement_date,
    created_at,
    DATE_PART('month', payment_date) as payment_month,
    DATE_PART('year', payment_date) as payment_year
FROM banking_payment.payments_partitioned;

-- =============================================
-- GRANT PERMISSIONS
-- =============================================

-- Grant schema permissions
GRANT USAGE ON SCHEMA banking_partitioning TO banking_admin_role;
GRANT SELECT ON banking_partitioning.partition_metadata TO banking_readonly_role, banking_admin_role;
GRANT SELECT ON banking_partitioning.shard_configuration TO banking_readonly_role, banking_admin_role;

-- Grant function permissions
GRANT EXECUTE ON FUNCTION banking_partitioning.create_monthly_partitions(TEXT, TEXT, DATE, DATE) TO banking_admin_role;
GRANT EXECUTE ON FUNCTION banking_partitioning.drop_old_partitions(TEXT, TEXT, INTEGER) TO banking_admin_role;
GRANT EXECUTE ON FUNCTION banking_partitioning.analyze_partition_performance(TEXT, TEXT) TO banking_admin_role;
GRANT EXECUTE ON FUNCTION banking_partitioning.get_customer_shard(UUID) TO banking_app_role;
GRANT EXECUTE ON FUNCTION banking_partitioning.maintenance_job() TO banking_admin_role;

-- Grant table permissions
GRANT SELECT ON banking_customer.customers_partitioned TO banking_app_role, banking_readonly_role;
GRANT SELECT ON banking_loan.loans_partitioned TO banking_app_role, banking_readonly_role;
GRANT SELECT ON banking_payment.payments_partitioned TO banking_app_role, banking_readonly_role;
GRANT SELECT ON banking_audit.audit_log_partitioned TO banking_audit_role, banking_admin_role;

-- Grant view permissions
GRANT SELECT ON banking_customer.customer_search_view TO banking_app_role, banking_readonly_role;
GRANT SELECT ON banking_loan.loan_analytics_view TO banking_app_role, banking_readonly_role;
GRANT SELECT ON banking_payment.payment_reporting_view TO banking_app_role, banking_readonly_role;

-- Success message
SELECT 'Database Sharding and Partitioning Strategy Implemented Successfully' AS status;