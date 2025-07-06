-- FAPI 2.0 + DPoP Banking Test Data
-- Comprehensive test dataset for end-to-end testing

-- =============================================================================
-- CUSTOMERS TEST DATA
-- =============================================================================

-- Individual Customers
INSERT INTO customers (customer_id, customer_type, status, created_date, last_modified_date) VALUES
('CUST001', 'INDIVIDUAL', 'ACTIVE', '2020-01-15 10:00:00', '2024-12-01 15:30:00'),
('CUST002', 'INDIVIDUAL', 'ACTIVE', '2021-03-22 14:30:00', '2024-11-15 09:45:00'),
('CUST003', 'INDIVIDUAL', 'ACTIVE', '2022-06-10 11:15:00', '2024-10-20 16:20:00'),
('CUST004', 'INDIVIDUAL', 'PENDING_VERIFICATION', '2024-01-05 09:30:00', '2024-01-05 09:30:00'),
('CUST005', 'INDIVIDUAL', 'SUSPENDED', '2019-11-20 13:45:00', '2024-08-15 14:10:00');

-- Corporate Customers
INSERT INTO customers (customer_id, customer_type, status, created_date, last_modified_date) VALUES
('CORP001', 'CORPORATE', 'ACTIVE', '2018-05-01 08:00:00', '2024-11-30 10:15:00'),
('CORP002', 'CORPORATE', 'ACTIVE', '2020-09-15 12:00:00', '2024-10-25 11:30:00'),
('CORP003', 'CORPORATE', 'PENDING_VERIFICATION', '2024-01-20 16:45:00', '2024-01-20 16:45:00');

-- Customer Personal Information
INSERT INTO customer_personal_info (customer_id, first_name, last_name, date_of_birth, email, phone, ssn, created_date) VALUES
('CUST001', 'John', 'Smith', '1985-07-15', 'john.smith@example.com', '+1-555-0123', '123-45-6789', '2020-01-15 10:00:00'),
('CUST002', 'Jane', 'Doe', '1990-03-22', 'jane.doe@example.com', '+1-555-0124', '987-65-4321', '2021-03-22 14:30:00'),
('CUST003', 'Michael', 'Johnson', '1978-11-08', 'michael.johnson@example.com', '+1-555-0125', '456-78-9123', '2022-06-10 11:15:00'),
('CUST004', 'Sarah', 'Williams', '1995-02-14', 'sarah.williams@example.com', '+1-555-0126', '789-12-3456', '2024-01-05 09:30:00'),
('CUST005', 'Robert', 'Brown', '1982-09-30', 'robert.brown@example.com', '+1-555-0127', '321-54-9876', '2019-11-20 13:45:00');

-- Customer Addresses
INSERT INTO customer_addresses (customer_id, address_type, street, city, state, zip_code, country, is_primary, created_date) VALUES
('CUST001', 'HOME', '123 Main Street', 'New York', 'NY', '10001', 'US', true, '2020-01-15 10:00:00'),
('CUST001', 'MAILING', '456 Oak Avenue', 'Brooklyn', 'NY', '11201', 'US', false, '2020-01-15 10:00:00'),
('CUST002', 'HOME', '789 Pine Road', 'Los Angeles', 'CA', '90210', 'US', true, '2021-03-22 14:30:00'),
('CUST003', 'HOME', '321 Elm Street', 'Chicago', 'IL', '60601', 'US', true, '2022-06-10 11:15:00'),
('CUST004', 'HOME', '654 Maple Drive', 'Houston', 'TX', '77001', 'US', true, '2024-01-05 09:30:00'),
('CUST005', 'HOME', '987 Cedar Lane', 'Phoenix', 'AZ', '85001', 'US', true, '2019-11-20 13:45:00');

-- Corporate Customer Information
INSERT INTO corporate_customer_info (customer_id, company_name, business_license, tax_id, industry, annual_revenue, employee_count, incorporation_date, created_date) VALUES
('CORP001', 'TechCorp Industries Inc.', 'BL123456789', '12-3456789', 'TECHNOLOGY', 50000000.00, 250, '2015-01-01', '2018-05-01 08:00:00'),
('CORP002', 'Manufacturing Solutions LLC', 'BL987654321', '98-7654321', 'MANUFACTURING', 25000000.00, 150, '2018-03-15', '2020-09-15 12:00:00'),
('CORP003', 'Green Energy Partners', 'BL456789123', '45-6789123', 'ENERGY', 75000000.00, 500, '2020-06-01', '2024-01-20 16:45:00');

-- =============================================================================
-- LOANS TEST DATA
-- =============================================================================

-- Active Loans
INSERT INTO loans (loan_id, customer_id, loan_type, amount, currency, interest_rate, installment_count, 
                  monthly_payment, start_date, end_date, status, purpose, collateral_type, 
                  collateral_value, approved_by, approved_date, created_date, last_modified_date) VALUES
('LOAN001', 'CUST001', 'MORTGAGE', 250000.00, 'USD', 3.25, 360, 1088.00, '2020-02-01', '2050-02-01', 'ACTIVE', 
 'Home Purchase', 'REAL_ESTATE', 350000.00, 'EMP002', '2020-01-25 14:30:00', '2020-01-20 09:00:00', '2024-12-01 10:15:00'),

('LOAN002', 'CUST002', 'PERSONAL', 25000.00, 'USD', 8.50, 60, 514.00, '2021-04-01', '2026-04-01', 'ACTIVE', 
 'Debt Consolidation', 'NONE', 0.00, 'EMP003', '2021-03-28 11:45:00', '2021-03-25 10:30:00', '2024-11-20 14:20:00'),

('LOAN003', 'CUST003', 'AUTO', 35000.00, 'USD', 4.75, 72, 547.00, '2022-07-01', '2028-07-01', 'ACTIVE', 
 'Vehicle Purchase', 'VEHICLE', 40000.00, 'EMP002', '2022-06-20 16:00:00', '2022-06-15 13:45:00', '2024-10-30 11:30:00'),

('LOAN004', 'CORP001', 'BUSINESS', 500000.00, 'USD', 5.25, 120, 5374.00, '2023-01-15', '2033-01-15', 'ACTIVE', 
 'Equipment Purchase', 'EQUIPMENT', 600000.00, 'EMP002', '2023-01-10 09:30:00', '2023-01-05 11:00:00', '2024-12-01 15:45:00'),

('LOAN005', 'CUST004', 'MORTGAGE', 180000.00, 'USD', 3.75, 360, 833.00, '2024-02-01', '2054-02-01', 'PENDING_APPROVAL', 
 'First Time Home Purchase', 'REAL_ESTATE', 220000.00, NULL, NULL, '2024-01-10 14:00:00', '2024-01-15 09:20:00');

-- Loan Applications (Pending/In Process)
INSERT INTO loans (loan_id, customer_id, loan_type, amount, currency, interest_rate, installment_count, 
                  monthly_payment, start_date, end_date, status, purpose, collateral_type, 
                  collateral_value, created_date, last_modified_date) VALUES
('LOAN006', 'CUST005', 'PERSONAL', 15000.00, 'USD', 9.25, 48, 378.00, NULL, NULL, 'UNDER_REVIEW', 
 'Home Improvement', 'NONE', 0.00, '2024-01-20 16:30:00', '2024-01-22 10:15:00'),

('LOAN007', 'CORP002', 'BUSINESS', 750000.00, 'USD', 4.95, 180, 5892.00, NULL, NULL, 'CREDIT_ASSESSMENT', 
 'Business Expansion', 'REAL_ESTATE', 1200000.00, '2024-01-25 11:45:00', '2024-01-25 11:45:00'),

('LOAN008', 'CORP003', 'BUSINESS', 2000000.00, 'USD', 4.50, 240, 12669.00, NULL, NULL, 'PENDING_DOCUMENTATION', 
 'Green Energy Infrastructure', 'EQUIPMENT', 3000000.00, '2024-01-22 09:15:00', '2024-01-23 14:30:00');

-- =============================================================================
-- PAYMENTS TEST DATA
-- =============================================================================

-- Payment History for LOAN001 (Mortgage)
INSERT INTO payments (payment_id, loan_id, amount, currency, payment_date, payment_type, status, 
                     principal_amount, interest_amount, late_fee, discount_amount, 
                     payment_method_type, payment_reference, processed_by, created_date) VALUES
-- Regular payments for 2024
('PAY001', 'LOAN001', 1088.00, 'USD', '2024-01-01', 'REGULAR', 'COMPLETED', 
 408.33, 679.67, 0.00, 0.00, 'BANK_TRANSFER', 'ACH-20240101-001', 'EMP005', '2024-01-01 08:30:00'),
('PAY002', 'LOAN001', 1088.00, 'USD', '2024-02-01', 'REGULAR', 'COMPLETED', 
 409.44, 678.56, 0.00, 0.00, 'BANK_TRANSFER', 'ACH-20240201-001', 'EMP005', '2024-02-01 08:30:00'),
('PAY003', 'LOAN001', 1088.00, 'USD', '2024-03-01', 'REGULAR', 'COMPLETED', 
 410.55, 677.45, 0.00, 0.00, 'BANK_TRANSFER', 'ACH-20240301-001', 'EMP005', '2024-03-01 08:30:00'),
('PAY004', 'LOAN001', 1088.00, 'USD', '2024-04-01', 'REGULAR', 'COMPLETED', 
 411.67, 676.33, 0.00, 0.00, 'BANK_TRANSFER', 'ACH-20240401-001', 'EMP005', '2024-04-01 08:30:00'),
('PAY005', 'LOAN001', 5000.00, 'USD', '2024-05-15', 'EARLY_PAYMENT', 'COMPLETED', 
 4750.00, 0.00, 0.00, 250.00, 'WIRE_TRANSFER', 'WIRE-20240515-001', 'EMP005', '2024-05-15 14:20:00');

-- Payments for LOAN002 (Personal Loan)
INSERT INTO payments (payment_id, loan_id, amount, currency, payment_date, payment_type, status, 
                     principal_amount, interest_amount, late_fee, discount_amount, 
                     payment_method_type, payment_reference, processed_by, created_date) VALUES
('PAY010', 'LOAN002', 514.00, 'USD', '2024-01-01', 'REGULAR', 'COMPLETED', 
 337.71, 176.29, 0.00, 0.00, 'AUTO_DEBIT', 'AUTO-20240101-002', 'SYSTEM', '2024-01-01 00:05:00'),
('PAY011', 'LOAN002', 514.00, 'USD', '2024-02-01', 'REGULAR', 'COMPLETED', 
 340.10, 173.90, 0.00, 0.00, 'AUTO_DEBIT', 'AUTO-20240201-002', 'SYSTEM', '2024-02-01 00:05:00'),
('PAY012', 'LOAN002', 564.00, 'USD', '2024-03-05', 'LATE', 'COMPLETED', 
 340.10, 173.90, 50.00, 0.00, 'BANK_TRANSFER', 'LATE-20240305-002', 'EMP005', '2024-03-05 10:15:00');

-- Payments for LOAN003 (Auto Loan)
INSERT INTO payments (payment_id, loan_id, amount, currency, payment_date, payment_type, status, 
                     principal_amount, interest_amount, late_fee, discount_amount, 
                     payment_method_type, payment_reference, processed_by, created_date) VALUES
('PAY020', 'LOAN003', 547.00, 'USD', '2024-01-01', 'REGULAR', 'COMPLETED', 
 408.33, 138.67, 0.00, 0.00, 'AUTO_DEBIT', 'AUTO-20240101-003', 'SYSTEM', '2024-01-01 00:10:00'),
('PAY021', 'LOAN003', 547.00, 'USD', '2024-02-01', 'REGULAR', 'COMPLETED', 
 409.95, 137.05, 0.00, 0.00, 'AUTO_DEBIT', 'AUTO-20240201-003', 'SYSTEM', '2024-02-01 00:10:00');

-- Business Loan Payments
INSERT INTO payments (payment_id, loan_id, amount, currency, payment_date, payment_type, status, 
                     principal_amount, interest_amount, late_fee, discount_amount, 
                     payment_method_type, payment_reference, processed_by, created_date) VALUES
('PAY030', 'LOAN004', 5374.00, 'USD', '2024-01-15', 'REGULAR', 'COMPLETED', 
 3186.92, 2187.08, 0.00, 0.00, 'WIRE_TRANSFER', 'WIRE-20240115-004', 'EMP005', '2024-01-15 09:30:00'),
('PAY031', 'LOAN004', 5374.00, 'USD', '2024-02-15', 'REGULAR', 'COMPLETED', 
 3200.85, 2173.15, 0.00, 0.00, 'WIRE_TRANSFER', 'WIRE-20240215-004', 'EMP005', '2024-02-15 09:30:00');

-- =============================================================================
-- CREDIT ASSESSMENTS TEST DATA
-- =============================================================================

INSERT INTO credit_assessments (assessment_id, customer_id, loan_id, credit_score, assessment_date, 
                               assessment_type, risk_level, debt_to_income_ratio, employment_verification, 
                               annual_income, assets_value, liabilities_value, assessment_notes, 
                               assessed_by, created_date) VALUES
('ASSESS001', 'CUST001', 'LOAN001', 750, '2020-01-22', 'MORTGAGE', 'LOW', 0.28, 'VERIFIED', 
 85000.00, 450000.00, 125000.00, 'Strong credit profile with stable employment history.', 'EMP004', '2020-01-22 10:30:00'),

('ASSESS002', 'CUST002', 'LOAN002', 680, '2021-03-26', 'PERSONAL', 'MEDIUM', 0.35, 'VERIFIED', 
 65000.00, 150000.00, 45000.00, 'Moderate credit profile. Debt consolidation will improve DTI ratio.', 'EMP004', '2021-03-26 14:15:00'),

('ASSESS003', 'CUST003', 'LOAN003', 720, '2022-06-18', 'AUTO', 'LOW', 0.22, 'VERIFIED', 
 72000.00, 200000.00, 35000.00, 'Good credit score with excellent payment history.', 'EMP004', '2022-06-18 11:45:00'),

('ASSESS004', 'CORP001', 'LOAN004', 780, '2023-01-08', 'BUSINESS', 'LOW', 0.45, 'VERIFIED', 
 5200000.00, 12000000.00, 2800000.00, 'Strong corporate financials. Equipment purchase is well-justified.', 'EMP004', '2023-01-08 16:20:00'),

('ASSESS005', 'CUST004', 'LOAN005', 695, '2024-01-12', 'MORTGAGE', 'MEDIUM', 0.31, 'PENDING', 
 58000.00, 75000.00, 18000.00, 'First-time homebuyer. Employment verification in progress.', 'EMP004', '2024-01-12 13:30:00');

-- =============================================================================
-- AUDIT LOGS TEST DATA
-- =============================================================================

INSERT INTO audit_logs (audit_id, entity_type, entity_id, action, user_id, user_role, 
                       ip_address, user_agent, timestamp, old_values, new_values, 
                       request_id, session_id, details) VALUES
('AUDIT001', 'CUSTOMER', 'CUST001', 'CREATED', 'loan-officer', 'LOAN_OFFICER', 
 '192.168.1.100', 'Mozilla/5.0 (Banking App)', '2020-01-15 10:00:00', NULL, 
 '{"status": "ACTIVE", "type": "INDIVIDUAL"}', 'REQ-20200115-001', 'SESS-001', 'Customer onboarding completed'),

('AUDIT002', 'LOAN', 'LOAN001', 'APPROVED', 'senior-loan-officer', 'SENIOR_LOAN_OFFICER', 
 '192.168.1.101', 'Mozilla/5.0 (Banking App)', '2020-01-25 14:30:00', 
 '{"status": "PENDING_APPROVAL"}', '{"status": "APPROVED", "approved_by": "EMP002"}', 
 'REQ-20200125-002', 'SESS-002', 'Mortgage loan approved after credit assessment'),

('AUDIT003', 'PAYMENT', 'PAY001', 'PROCESSED', 'payment-processor-1', 'PAYMENT_PROCESSOR', 
 '192.168.1.102', 'Banking Payment System v2.0', '2024-01-01 08:30:00', NULL, 
 '{"amount": 1088.00, "status": "COMPLETED"}', 'REQ-20240101-003', 'SESS-003', 'Regular mortgage payment processed'),

('AUDIT004', 'CUSTOMER', 'CORP001', 'MODIFIED', 'banking-admin', 'BANKING_ADMIN', 
 '192.168.1.103', 'Mozilla/5.0 (Admin Panel)', '2024-11-30 10:15:00', 
 '{"annual_revenue": 45000000}', '{"annual_revenue": 50000000}', 
 'REQ-20241130-004', 'SESS-004', 'Corporate customer revenue updated'),

('AUDIT005', 'LOAN', 'LOAN005', 'CREATED', 'loan-officer-1', 'LOAN_OFFICER', 
 '192.168.1.104', 'Mozilla/5.0 (Banking App)', '2024-01-10 14:00:00', NULL, 
 '{"amount": 180000, "status": "PENDING_APPROVAL", "type": "MORTGAGE"}', 
 'REQ-20240110-005', 'SESS-005', 'New mortgage application submitted');

-- =============================================================================
-- DPOP TEST DATA (Security Context)
-- =============================================================================

-- DPoP JTI Blacklist (for replay prevention testing)
INSERT INTO dpop_jti_blacklist (jti, expiry_time, client_id, created_date) VALUES
('jti-test-001', '2025-01-01 12:00:00', 'fapi2-banking-app-production', NOW()),
('jti-test-002', '2025-01-01 12:05:00', 'fapi2-mobile-banking-app', NOW()),
('jti-test-003', '2025-01-01 12:10:00', 'fapi2-corporate-banking-client', NOW());

-- FAPI Interaction Logs
INSERT INTO fapi_interaction_logs (interaction_id, client_id, endpoint, http_method, 
                                 timestamp, response_status, dpop_jkt, user_id, 
                                 request_duration_ms, ip_address) VALUES
('12345678-1234-1234-1234-123456789012', 'fapi2-banking-app-production', '/api/v1/loans', 'GET', 
 NOW(), 200, 'eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9...', 'CUST001', 250, '192.168.1.100'),

('23456789-2345-2345-2345-234567890123', 'fapi2-mobile-banking-app', '/api/v1/payments', 'POST', 
 NOW(), 201, 'eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9...', 'CUST002', 350, '192.168.1.101'),

('34567890-3456-3456-3456-345678901234', 'fapi2-corporate-banking-client', '/api/v1/loans', 'POST', 
 NOW(), 201, 'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...', 'CORP001', 420, '192.168.1.102');

-- =============================================================================
-- MIGRATION TEST DATA
-- =============================================================================

-- Migration Status Tracking
INSERT INTO migration_status (client_id, migration_phase, fapi2_enabled, dpop_enabled, 
                            migration_date, rollback_reason, last_updated) VALUES
('fapi2-banking-app-production', 'PHASE_4_FULL_MIGRATION', true, true, '2024-11-01 00:00:00', NULL, NOW()),
('fapi2-banking-app-staging', 'PHASE_3_GRADUAL_ROLLOUT', true, true, '2024-10-15 00:00:00', NULL, NOW()),
('fapi2-mobile-banking-app', 'PHASE_3_GRADUAL_ROLLOUT', true, true, '2024-10-20 00:00:00', NULL, NOW()),
('fapi2-corporate-banking-client', 'PHASE_2_PILOT_CLIENTS', true, true, '2024-09-01 00:00:00', NULL, NOW()),
('legacy-client-1', 'PHASE_1_INTERNAL_TESTING', false, false, NULL, 'Performance issues', NOW());

-- Performance Metrics for Migration Monitoring
INSERT INTO performance_metrics (metric_date, client_id, endpoint, avg_response_time_ms, 
                               error_rate_percent, throughput_rps, dpop_validation_time_ms) VALUES
(CURRENT_DATE, 'fapi2-banking-app-production', '/api/v1/loans', 245, 0.1, 150, 25),
(CURRENT_DATE, 'fapi2-banking-app-production', '/api/v1/payments', 320, 0.2, 200, 30),
(CURRENT_DATE, 'fapi2-mobile-banking-app', '/api/v1/loans', 280, 0.3, 120, 28),
(CURRENT_DATE, 'fapi2-corporate-banking-client', '/api/v1/loans', 195, 0.1, 80, 22);

-- =============================================================================
-- INDEXES AND CONSTRAINTS (if not already created)
-- =============================================================================

-- Performance indexes for common queries
CREATE INDEX IF NOT EXISTS idx_loans_customer_status ON loans(customer_id, status);
CREATE INDEX IF NOT EXISTS idx_payments_loan_date ON payments(loan_id, payment_date);
CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp ON audit_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_fapi_interaction_logs_timestamp ON fapi_interaction_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_dpop_jti_expiry ON dpop_jti_blacklist(expiry_time);

-- =============================================================================
-- TEST DATA SUMMARY
-- =============================================================================

/*
Test Data Summary:
- 8 Customers (5 Individual, 3 Corporate)
- 8 Loans (5 Active, 3 Pending/Under Review) 
- 12 Payments (Regular, Late, Early Payment scenarios)
- 5 Credit Assessments
- 5 Audit Log entries
- 3 DPoP JTI blacklist entries
- 3 FAPI interaction logs
- 5 Migration status records
- 4 Performance metrics entries

This dataset provides comprehensive coverage for:
✅ End-to-end FAPI 2.0 + DPoP testing
✅ All loan lifecycle stages
✅ Payment processing scenarios
✅ Credit assessment workflows
✅ Audit and compliance tracking
✅ Migration monitoring
✅ Security validation (DPoP replay prevention)
✅ Performance monitoring
*/