-- Advanced Query Optimization for Enterprise Banking Database
-- Production-grade performance tuning with banking-specific optimizations

-- =============================================
-- QUERY OPTIMIZATION SCHEMA
-- =============================================

-- Create query optimization schema
CREATE SCHEMA IF NOT EXISTS banking_optimization;

-- Query performance monitoring table
CREATE TABLE banking_optimization.query_performance (
    query_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    query_hash TEXT NOT NULL,
    query_text TEXT NOT NULL,
    execution_time INTERVAL NOT NULL,
    plan_cost NUMERIC,
    rows_returned BIGINT,
    buffer_hits BIGINT,
    buffer_misses BIGINT,
    io_read_time INTERVAL,
    io_write_time INTERVAL,
    executed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    executed_by TEXT NOT NULL,
    
    -- Query classification
    query_type TEXT NOT NULL,
    table_names TEXT[],
    is_slow_query BOOLEAN DEFAULT false,
    
    -- Performance metrics
    cpu_usage NUMERIC,
    memory_usage NUMERIC,
    temp_files_used INTEGER DEFAULT 0,
    
    CONSTRAINT valid_query_type CHECK (query_type IN ('SELECT', 'INSERT', 'UPDATE', 'DELETE', 'COMPLEX'))
);

-- Indexes for query performance
CREATE INDEX idx_query_performance_hash ON banking_optimization.query_performance (query_hash);
CREATE INDEX idx_query_performance_execution_time ON banking_optimization.query_performance (execution_time);
CREATE INDEX idx_query_performance_executed_at ON banking_optimization.query_performance (executed_at);
CREATE INDEX idx_query_performance_slow_query ON banking_optimization.query_performance (is_slow_query);

-- Index recommendation table
CREATE TABLE banking_optimization.index_recommendations (
    recommendation_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    table_name TEXT NOT NULL,
    column_names TEXT[] NOT NULL,
    index_type TEXT NOT NULL,
    estimated_benefit NUMERIC,
    estimated_cost NUMERIC,
    query_pattern TEXT,
    recommendation_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    implemented_at TIMESTAMP WITH TIME ZONE,
    is_implemented BOOLEAN DEFAULT false,
    
    CONSTRAINT valid_index_type CHECK (index_type IN ('BTREE', 'HASH', 'GIN', 'GIST', 'SPGIST', 'BRIN', 'PARTIAL'))
);

-- Query optimization rules table
CREATE TABLE banking_optimization.optimization_rules (
    rule_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    rule_name TEXT NOT NULL UNIQUE,
    rule_description TEXT NOT NULL,
    rule_pattern TEXT NOT NULL,
    optimization_hint TEXT NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- =============================================
-- BANKING-SPECIFIC OPTIMIZED INDEXES
-- =============================================

-- Customer search optimization indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_customers_composite_search 
ON banking_customer.customers (customer_status, kyc_status, compliance_level, created_at);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_customers_name_search 
ON banking_customer.customers USING gin (
    to_tsvector('english', COALESCE(first_name_encrypted, '') || ' ' || COALESCE(last_name_encrypted, ''))
);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_customers_risk_profile 
ON banking_customer.customers (risk_rating, annual_income_encrypted, credit_score_encrypted)
WHERE customer_status = 'ACTIVE';

-- Loan performance optimization indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_loans_risk_analysis 
ON banking_loan.loans (loan_type, loan_status, principal_amount_range, interest_rate_range, application_date);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_loans_maturity_tracking 
ON banking_loan.loans (maturity_date, loan_status, customer_id)
WHERE loan_status IN ('ACTIVE', 'DISBURSED');

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_loans_approval_pipeline 
ON banking_loan.loans (application_date, loan_status, loan_type, created_at)
WHERE loan_status IN ('PENDING', 'APPROVED');

-- Payment processing optimization indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_payments_processing_queue 
ON banking_payment.payments (payment_status, payment_date, processing_date)
WHERE payment_status IN ('PENDING', 'PROCESSING');

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_payments_settlement_tracking 
ON banking_payment.payments (settlement_date, payment_status, customer_id)
WHERE payment_status = 'COMPLETED';

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_payments_fraud_monitoring 
ON banking_payment.payments (fraud_score_encrypted, payment_date, payment_amount_encrypted)
WHERE payment_status = 'COMPLETED';

-- Audit log optimization indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_log_compliance_search 
ON banking_audit.audit_log (compliance_tags, changed_at, table_name)
WHERE compliance_tags IS NOT NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_log_user_activity 
ON banking_audit.audit_log (changed_by, changed_at, operation)
WHERE changed_by IS NOT NULL;

-- =============================================
-- MATERIALIZED VIEWS FOR PERFORMANCE
-- =============================================

-- Customer risk summary view
CREATE MATERIALIZED VIEW banking_optimization.customer_risk_summary AS
SELECT 
    c.customer_id,
    c.customer_type,
    c.customer_status,
    c.risk_rating,
    c.compliance_level,
    c.kyc_status,
    COUNT(l.loan_id) as total_loans,
    SUM(CASE WHEN l.loan_status = 'ACTIVE' THEN 1 ELSE 0 END) as active_loans,
    COALESCE(AVG(CASE WHEN l.loan_status = 'ACTIVE' THEN 
        banking_security.decrypt_pii(l.principal_amount_encrypted)::NUMERIC 
    END), 0) as avg_loan_amount,
    COUNT(p.payment_id) as total_payments,
    SUM(CASE WHEN p.payment_status = 'COMPLETED' THEN 1 ELSE 0 END) as completed_payments,
    c.created_at,
    c.updated_at
FROM banking_customer.customers c
LEFT JOIN banking_loan.loans l ON c.customer_id = l.customer_id
LEFT JOIN banking_payment.payments p ON l.loan_id = p.loan_id
WHERE c.customer_status = 'ACTIVE'
GROUP BY c.customer_id, c.customer_type, c.customer_status, c.risk_rating, 
         c.compliance_level, c.kyc_status, c.created_at, c.updated_at;

-- Create index on materialized view
CREATE INDEX idx_customer_risk_summary_risk_rating 
ON banking_optimization.customer_risk_summary (risk_rating, compliance_level);

-- Loan portfolio summary view
CREATE MATERIALIZED VIEW banking_optimization.loan_portfolio_summary AS
SELECT 
    l.loan_type,
    l.loan_status,
    DATE_TRUNC('month', l.application_date) as application_month,
    COUNT(*) as loan_count,
    SUM(banking_security.decrypt_pii(l.principal_amount_encrypted)::NUMERIC) as total_amount,
    AVG(banking_security.decrypt_pii(l.principal_amount_encrypted)::NUMERIC) as avg_amount,
    AVG(banking_security.decrypt_pii(l.interest_rate_encrypted)::NUMERIC) as avg_interest_rate,
    COUNT(CASE WHEN l.loan_status = 'DEFAULTED' THEN 1 END) as defaulted_count,
    COUNT(CASE WHEN l.loan_status = 'PAID_OFF' THEN 1 END) as paid_off_count
FROM banking_loan.loans l
WHERE l.application_date >= CURRENT_DATE - INTERVAL '2 years'
GROUP BY l.loan_type, l.loan_status, DATE_TRUNC('month', l.application_date);

-- Create index on loan portfolio view
CREATE INDEX idx_loan_portfolio_summary_type_status 
ON banking_optimization.loan_portfolio_summary (loan_type, loan_status, application_month);

-- Payment performance summary view
CREATE MATERIALIZED VIEW banking_optimization.payment_performance_summary AS
SELECT 
    DATE_TRUNC('day', p.payment_date) as payment_day,
    p.payment_status,
    COUNT(*) as payment_count,
    SUM(banking_security.decrypt_payment_data(p.payment_amount_encrypted)::NUMERIC) as total_amount,
    AVG(EXTRACT(EPOCH FROM (p.settlement_date - p.payment_date))) as avg_settlement_time_seconds,
    COUNT(CASE WHEN p.fraud_score_encrypted IS NOT NULL THEN 1 END) as fraud_checks_performed,
    AVG(EXTRACT(EPOCH FROM (p.processing_date - p.created_at))) as avg_processing_time_seconds
FROM banking_payment.payments p
WHERE p.payment_date >= CURRENT_DATE - INTERVAL '90 days'
GROUP BY DATE_TRUNC('day', p.payment_date), p.payment_status;

-- Create index on payment performance view
CREATE INDEX idx_payment_performance_summary_day_status 
ON banking_optimization.payment_performance_summary (payment_day, payment_status);

-- =============================================
-- OPTIMIZATION FUNCTIONS
-- =============================================

-- Function to analyze query performance
CREATE OR REPLACE FUNCTION banking_optimization.analyze_query_performance(
    p_query_text TEXT,
    p_execution_time INTERVAL,
    p_rows_returned BIGINT DEFAULT 0
) RETURNS UUID AS $$
DECLARE
    v_query_id UUID;
    v_query_hash TEXT;
    v_is_slow_query BOOLEAN;
    v_query_type TEXT;
    v_table_names TEXT[];
BEGIN
    -- Generate query hash
    v_query_hash := encode(digest(p_query_text, 'sha256'), 'hex');
    
    -- Determine if it's a slow query (>1 second)
    v_is_slow_query := p_execution_time > INTERVAL '1 second';
    
    -- Extract query type
    v_query_type := CASE 
        WHEN upper(p_query_text) ~ '^SELECT' THEN 'SELECT'
        WHEN upper(p_query_text) ~ '^INSERT' THEN 'INSERT'
        WHEN upper(p_query_text) ~ '^UPDATE' THEN 'UPDATE'
        WHEN upper(p_query_text) ~ '^DELETE' THEN 'DELETE'
        ELSE 'COMPLEX'
    END;
    
    -- Extract table names (simplified regex)
    v_table_names := ARRAY(
        SELECT DISTINCT unnest(
            regexp_split_to_array(
                regexp_replace(p_query_text, '.*(FROM|JOIN|INTO|UPDATE)\s+([a-zA-Z_][a-zA-Z0-9_]*\.[a-zA-Z_][a-zA-Z0-9_]*)', '\2', 'gi'),
                '\s+'
            )
        )
    );
    
    -- Insert performance record
    INSERT INTO banking_optimization.query_performance (
        query_hash, query_text, execution_time, rows_returned,
        executed_by, query_type, table_names, is_slow_query
    ) VALUES (
        v_query_hash, p_query_text, p_execution_time, p_rows_returned,
        COALESCE(current_setting('banking.user_id', true), current_user),
        v_query_type, v_table_names, v_is_slow_query
    ) RETURNING query_id INTO v_query_id;
    
    -- Generate index recommendations for slow queries
    IF v_is_slow_query THEN
        PERFORM banking_optimization.generate_index_recommendations(v_query_id);
    END IF;
    
    RETURN v_query_id;
END;
$$ LANGUAGE plpgsql;

-- Function to generate index recommendations
CREATE OR REPLACE FUNCTION banking_optimization.generate_index_recommendations(
    p_query_id UUID
) RETURNS INTEGER AS $$
DECLARE
    v_query_record RECORD;
    v_table_name TEXT;
    v_recommendations_created INTEGER := 0;
BEGIN
    -- Get query information
    SELECT * INTO v_query_record
    FROM banking_optimization.query_performance
    WHERE query_id = p_query_id;
    
    -- Analyze each table in the query
    FOREACH v_table_name IN ARRAY v_query_record.table_names
    LOOP
        -- Check for missing indexes based on query patterns
        IF v_query_record.query_text ~* 'WHERE.*' || v_table_name || '.*=' THEN
            -- Recommend equality index
            INSERT INTO banking_optimization.index_recommendations (
                table_name, column_names, index_type, estimated_benefit,
                query_pattern, recommendation_reason
            ) VALUES (
                v_table_name,
                ARRAY['id'], -- Simplified - would extract actual columns
                'BTREE',
                v_query_record.execution_time::NUMERIC,
                'Equality filter',
                'Query contains equality filter on this table'
            ) ON CONFLICT DO NOTHING;
            
            v_recommendations_created := v_recommendations_created + 1;
        END IF;
        
        IF v_query_record.query_text ~* 'ORDER BY.*' || v_table_name THEN
            -- Recommend ordering index
            INSERT INTO banking_optimization.index_recommendations (
                table_name, column_names, index_type, estimated_benefit,
                query_pattern, recommendation_reason
            ) VALUES (
                v_table_name,
                ARRAY['created_at'], -- Simplified
                'BTREE',
                v_query_record.execution_time::NUMERIC * 0.8,
                'Order by clause',
                'Query contains ORDER BY on this table'
            ) ON CONFLICT DO NOTHING;
            
            v_recommendations_created := v_recommendations_created + 1;
        END IF;
        
        IF v_query_record.query_text ~* 'GROUP BY.*' || v_table_name THEN
            -- Recommend grouping index
            INSERT INTO banking_optimization.index_recommendations (
                table_name, column_names, index_type, estimated_benefit,
                query_pattern, recommendation_reason
            ) VALUES (
                v_table_name,
                ARRAY['group_column'], -- Simplified
                'BTREE',
                v_query_record.execution_time::NUMERIC * 0.6,
                'Group by clause',
                'Query contains GROUP BY on this table'
            ) ON CONFLICT DO NOTHING;
            
            v_recommendations_created := v_recommendations_created + 1;
        END IF;
    END LOOP;
    
    RETURN v_recommendations_created;
END;
$$ LANGUAGE plpgsql;

-- Function to refresh materialized views
CREATE OR REPLACE FUNCTION banking_optimization.refresh_materialized_views() RETURNS void AS $$
BEGIN
    -- Refresh customer risk summary
    REFRESH MATERIALIZED VIEW banking_optimization.customer_risk_summary;
    
    -- Refresh loan portfolio summary
    REFRESH MATERIALIZED VIEW banking_optimization.loan_portfolio_summary;
    
    -- Refresh payment performance summary
    REFRESH MATERIALIZED VIEW banking_optimization.payment_performance_summary;
    
    -- Log refresh
    INSERT INTO banking_audit.audit_log (
        table_name, operation, new_values, changed_by, compliance_tags
    ) VALUES (
        'MATERIALIZED_VIEWS', 'REFRESH',
        jsonb_build_object('refreshed_at', NOW()),
        COALESCE(current_setting('banking.user_id', true), current_user),
        ARRAY['PERFORMANCE', 'OPTIMIZATION']
    );
END;
$$ LANGUAGE plpgsql;

-- =============================================
-- QUERY OPTIMIZATION RULES
-- =============================================

-- Insert optimization rules
INSERT INTO banking_optimization.optimization_rules (
    rule_name, rule_description, rule_pattern, optimization_hint
) VALUES 
    ('avoid_select_star', 'Avoid SELECT * in production queries', 'SELECT \*', 'Specify only required columns'),
    ('use_limit_with_order', 'Use LIMIT with ORDER BY for pagination', 'LIMIT.*(?!ORDER BY)', 'Add ORDER BY clause for consistent results'),
    ('avoid_leading_wildcards', 'Avoid leading wildcards in LIKE queries', 'LIKE ''%.*''', 'Use full-text search or suffix matching'),
    ('use_exists_over_in', 'Use EXISTS instead of IN for subqueries', 'IN \(SELECT', 'Replace with EXISTS for better performance'),
    ('avoid_or_in_where', 'Avoid OR conditions in WHERE clause', 'WHERE.*OR.*', 'Use UNION or separate queries'),
    ('use_covering_indexes', 'Use covering indexes for frequent queries', 'SELECT.*FROM.*WHERE.*', 'Create covering indexes for commonly queried columns'),
    ('avoid_functions_in_where', 'Avoid functions in WHERE clause', 'WHERE.*\(.*\)', 'Use functional indexes or rewrite query'),
    ('batch_inserts', 'Use batch operations for multiple inserts', 'INSERT.*VALUES.*INSERT', 'Use multi-row INSERT or COPY'),
    ('use_prepared_statements', 'Use prepared statements for repeated queries', 'PREPARE.*EXECUTE', 'Prepare frequently executed queries'),
    ('optimize_joins', 'Optimize JOIN operations', 'JOIN.*ON.*=.*', 'Ensure proper indexes on join columns');

-- =============================================
-- PERFORMANCE MONITORING VIEWS
-- =============================================

-- View for slow queries
CREATE OR REPLACE VIEW banking_optimization.slow_queries_report AS
SELECT 
    query_hash,
    query_text,
    AVG(execution_time) as avg_execution_time,
    COUNT(*) as execution_count,
    MAX(execution_time) as max_execution_time,
    MIN(execution_time) as min_execution_time,
    MAX(executed_at) as last_executed,
    query_type,
    table_names
FROM banking_optimization.query_performance
WHERE is_slow_query = true
GROUP BY query_hash, query_text, query_type, table_names
ORDER BY avg_execution_time DESC;

-- View for index recommendations
CREATE OR REPLACE VIEW banking_optimization.index_recommendations_report AS
SELECT 
    table_name,
    column_names,
    index_type,
    estimated_benefit,
    recommendation_reason,
    COUNT(*) as frequency,
    MAX(created_at) as last_recommended,
    is_implemented
FROM banking_optimization.index_recommendations
GROUP BY table_name, column_names, index_type, estimated_benefit, 
         recommendation_reason, is_implemented
ORDER BY estimated_benefit DESC, frequency DESC;

-- View for performance metrics
CREATE OR REPLACE VIEW banking_optimization.performance_metrics AS
SELECT 
    'total_queries' as metric_name,
    COUNT(*)::NUMERIC as metric_value,
    'Last 24 hours' as time_period
FROM banking_optimization.query_performance
WHERE executed_at > NOW() - INTERVAL '24 hours'

UNION ALL

SELECT 
    'slow_queries' as metric_name,
    COUNT(*)::NUMERIC as metric_value,
    'Last 24 hours' as time_period
FROM banking_optimization.query_performance
WHERE is_slow_query = true
AND executed_at > NOW() - INTERVAL '24 hours'

UNION ALL

SELECT 
    'avg_execution_time' as metric_name,
    EXTRACT(EPOCH FROM AVG(execution_time))::NUMERIC as metric_value,
    'Last 24 hours' as time_period
FROM banking_optimization.query_performance
WHERE executed_at > NOW() - INTERVAL '24 hours'

UNION ALL

SELECT 
    'pending_recommendations' as metric_name,
    COUNT(*)::NUMERIC as metric_value,
    'Current' as time_period
FROM banking_optimization.index_recommendations
WHERE is_implemented = false;

-- =============================================
-- MAINTENANCE PROCEDURES
-- =============================================

-- Procedure to maintain query performance data
CREATE OR REPLACE FUNCTION banking_optimization.maintain_performance_data() RETURNS void AS $$
BEGIN
    -- Clean old performance data (keep 30 days)
    DELETE FROM banking_optimization.query_performance
    WHERE executed_at < NOW() - INTERVAL '30 days';
    
    -- Clean old recommendations (keep 90 days)
    DELETE FROM banking_optimization.index_recommendations
    WHERE created_at < NOW() - INTERVAL '90 days'
    AND is_implemented = false;
    
    -- Update table statistics
    ANALYZE banking_customer.customers;
    ANALYZE banking_loan.loans;
    ANALYZE banking_payment.payments;
    ANALYZE banking_audit.audit_log;
    
    -- Refresh materialized views
    PERFORM banking_optimization.refresh_materialized_views();
    
    -- Log maintenance
    INSERT INTO banking_audit.audit_log (
        table_name, operation, new_values, changed_by, compliance_tags
    ) VALUES (
        'PERFORMANCE_MAINTENANCE', 'MAINTAIN',
        jsonb_build_object(
            'maintained_at', NOW(),
            'old_data_cleaned', true,
            'statistics_updated', true,
            'views_refreshed', true
        ),
        'SYSTEM_MAINTENANCE',
        ARRAY['PERFORMANCE', 'MAINTENANCE']
    );
END;
$$ LANGUAGE plpgsql;

-- =============================================
-- GRANT PERMISSIONS
-- =============================================

-- Grant schema permissions
GRANT USAGE ON SCHEMA banking_optimization TO banking_app_role, banking_readonly_role, banking_admin_role;

-- Grant table permissions
GRANT SELECT ON banking_optimization.query_performance TO banking_app_role, banking_readonly_role;
GRANT INSERT ON banking_optimization.query_performance TO banking_app_role;
GRANT ALL ON banking_optimization.query_performance TO banking_admin_role;

GRANT SELECT ON banking_optimization.index_recommendations TO banking_app_role, banking_readonly_role;
GRANT ALL ON banking_optimization.index_recommendations TO banking_admin_role;

GRANT SELECT ON banking_optimization.optimization_rules TO banking_app_role, banking_readonly_role;

-- Grant materialized view permissions
GRANT SELECT ON banking_optimization.customer_risk_summary TO banking_app_role, banking_readonly_role;
GRANT SELECT ON banking_optimization.loan_portfolio_summary TO banking_app_role, banking_readonly_role;
GRANT SELECT ON banking_optimization.payment_performance_summary TO banking_app_role, banking_readonly_role;

-- Grant view permissions
GRANT SELECT ON banking_optimization.slow_queries_report TO banking_app_role, banking_readonly_role;
GRANT SELECT ON banking_optimization.index_recommendations_report TO banking_app_role, banking_readonly_role;
GRANT SELECT ON banking_optimization.performance_metrics TO banking_app_role, banking_readonly_role;

-- Grant function permissions
GRANT EXECUTE ON FUNCTION banking_optimization.analyze_query_performance(TEXT, INTERVAL, BIGINT) TO banking_app_role;
GRANT EXECUTE ON FUNCTION banking_optimization.refresh_materialized_views() TO banking_admin_role;
GRANT EXECUTE ON FUNCTION banking_optimization.maintain_performance_data() TO banking_admin_role;

-- Success message
SELECT 'Advanced Query Optimization Implementation Completed Successfully' AS status;