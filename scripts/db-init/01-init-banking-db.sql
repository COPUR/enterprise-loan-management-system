-- Enterprise Banking Database Initialization Script
-- Creates the banking enterprise database and user with proper permissions

-- Create database if it doesn't exist
SELECT 'CREATE DATABASE banking_enterprise' 
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'banking_enterprise')\gexec

-- Create extensions for banking operations
\c banking_enterprise;

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Create banking schema
CREATE SCHEMA IF NOT EXISTS banking;
CREATE SCHEMA IF NOT EXISTS audit;

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE banking_enterprise TO banking_enterprise;
GRANT ALL ON SCHEMA banking TO banking_enterprise;
GRANT ALL ON SCHEMA audit TO banking_enterprise;
GRANT ALL ON SCHEMA public TO banking_enterprise;

-- Set default privileges
ALTER DEFAULT PRIVILEGES IN SCHEMA banking GRANT ALL ON TABLES TO banking_enterprise;
ALTER DEFAULT PRIVILEGES IN SCHEMA banking GRANT ALL ON SEQUENCES TO banking_enterprise;
ALTER DEFAULT PRIVILEGES IN SCHEMA audit GRANT ALL ON TABLES TO banking_enterprise;
ALTER DEFAULT PRIVILEGES IN SCHEMA audit GRANT ALL ON SEQUENCES TO banking_enterprise;

-- Create audit function for banking compliance
CREATE OR REPLACE FUNCTION audit.audit_trigger_function()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        INSERT INTO audit.audit_log (
            table_name, operation, old_values, changed_by, changed_at
        ) VALUES (
            TG_TABLE_NAME, TG_OP, row_to_json(OLD), current_user, now()
        );
        RETURN OLD;
    ELSIF TG_OP = 'UPDATE' THEN
        INSERT INTO audit.audit_log (
            table_name, operation, old_values, new_values, changed_by, changed_at
        ) VALUES (
            TG_TABLE_NAME, TG_OP, row_to_json(OLD), row_to_json(NEW), current_user, now()
        );
        RETURN NEW;
    ELSIF TG_OP = 'INSERT' THEN
        INSERT INTO audit.audit_log (
            table_name, operation, new_values, changed_by, changed_at
        ) VALUES (
            TG_TABLE_NAME, TG_OP, row_to_json(NEW), current_user, now()
        );
        RETURN NEW;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Create audit log table
CREATE TABLE IF NOT EXISTS audit.audit_log (
    id BIGSERIAL PRIMARY KEY,
    table_name TEXT NOT NULL,
    operation TEXT NOT NULL,
    old_values JSONB,
    new_values JSONB,
    changed_by TEXT NOT NULL DEFAULT current_user,
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Create indexes for audit log
CREATE INDEX IF NOT EXISTS idx_audit_log_table_name ON audit.audit_log(table_name);
CREATE INDEX IF NOT EXISTS idx_audit_log_changed_at ON audit.audit_log(changed_at);
CREATE INDEX IF NOT EXISTS idx_audit_log_changed_by ON audit.audit_log(changed_by);

-- Banking system configuration
COMMENT ON DATABASE banking_enterprise IS 'Enterprise Banking Loan Management System Database';
COMMENT ON SCHEMA banking IS 'Main banking business logic schema';
COMMENT ON SCHEMA audit IS 'Audit trail schema for banking compliance';