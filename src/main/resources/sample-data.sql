-- Sample Data for Enterprise Loan Management System
-- Use Cases Testing - Comprehensive Dataset
-- Environment: Development/SIT/UAT

-- =============================================================================
-- 1. CUSTOMERS - Diverse customer profiles for testing
-- =============================================================================

INSERT INTO customers (customer_id, first_name, last_name, email, phone, date_of_birth, ssn, employment_status, annual_income, credit_score, debt_to_income_ratio, address, city, state, zip_code, customer_type, registration_date, status) VALUES
-- Personal Loan Applicants
(1, 'John', 'Smith', 'john.smith@email.com', '555-0101', '1985-03-15', '123-45-6789', 'EMPLOYED', 85000.00, 720, 0.35, '123 Main St', 'New York', 'NY', '10001', 'INDIVIDUAL', '2023-01-15', 'ACTIVE'),
(2, 'Sarah', 'Johnson', 'sarah.johnson@email.com', '555-0102', '1990-07-22', '234-56-7890', 'EMPLOYED', 95000.00, 750, 0.28, '456 Oak Ave', 'Los Angeles', 'CA', '90210', 'INDIVIDUAL', '2023-02-20', 'ACTIVE'),
(3, 'Michael', 'Brown', 'michael.brown@email.com', '555-0103', '1982-11-08', '345-67-8901', 'SELF_EMPLOYED', 120000.00, 680, 0.42, '789 Pine Rd', 'Chicago', 'IL', '60601', 'INDIVIDUAL', '2023-03-10', 'ACTIVE'),

-- Business Loan Applicants
(4, 'Jennifer', 'Davis', 'jennifer.davis@techstartup.com', '555-0104', '1988-05-12', '456-78-9012', 'BUSINESS_OWNER', 150000.00, 700, 0.25, '321 Business Blvd', 'Austin', 'TX', '73301', 'BUSINESS', '2023-04-05', 'ACTIVE'),
(5, 'Robert', 'Wilson', 'robert.wilson@manufacturing.com', '555-0105', '1975-09-30', '567-89-0123', 'BUSINESS_OWNER', 200000.00, 740, 0.30, '654 Industrial Way', 'Detroit', 'MI', '48201', 'BUSINESS', '2023-05-18', 'ACTIVE'),

-- Mortgage Applicants
(6, 'Lisa', 'Anderson', 'lisa.anderson@email.com', '555-0106', '1987-12-03', '678-90-1234', 'EMPLOYED', 110000.00, 760, 0.32, '987 Suburban Dr', 'Denver', 'CO', '80201', 'INDIVIDUAL', '2023-06-25', 'ACTIVE'),
(7, 'David', 'Martinez', 'david.martinez@email.com', '555-0107', '1983-04-18', '789-01-2345', 'EMPLOYED', 130000.00, 710, 0.38, '147 Family Lane', 'Phoenix', 'AZ', '85001', 'INDIVIDUAL', '2023-07-12', 'ACTIVE'),

-- High-Risk Profiles
(8, 'Amanda', 'Taylor', 'amanda.taylor@email.com', '555-0108', '1992-08-25', '890-12-3456', 'PART_TIME', 45000.00, 620, 0.55, '258 Starter St', 'Miami', 'FL', '33101', 'INDIVIDUAL', '2023-08-30', 'ACTIVE'),
(9, 'Christopher', 'Moore', 'chris.moore@email.com', '555-0109', '1995-01-14', '901-23-4567', 'UNEMPLOYED', 0.00, 580, 0.00, '369 Struggle Ave', 'Seattle', 'WA', '98101', 'INDIVIDUAL', '2023-09-15', 'ACTIVE'),

-- Premium Customers
(10, 'Elizabeth', 'White', 'elizabeth.white@email.com', '555-0110', '1978-06-07', '012-34-5678', 'EXECUTIVE', 250000.00, 800, 0.15, '741 Executive Dr', 'San Francisco', 'CA', '94101', 'PREMIUM', '2023-10-01', 'ACTIVE');

-- =============================================================================
-- 2. LOAN APPLICATIONS - Various stages and types
-- =============================================================================

INSERT INTO loan_applications (application_id, customer_id, loan_type, requested_amount, requested_term_months, purpose, application_date, status, assigned_underwriter, priority, monthly_income, employment_years, collateral_value, business_revenue, property_value, down_payment) VALUES
-- Approved Applications
('APP2024001', 1, 'PERSONAL', 50000.00, 36, 'Home renovation', '2024-01-15', 'APPROVED', 'UW001', 'STANDARD', 7083.33, 5, NULL, NULL, NULL, NULL),
('APP2024002', 2, 'PERSONAL', 30000.00, 24, 'Debt consolidation', '2024-01-20', 'APPROVED', 'UW002', 'STANDARD', 7916.67, 3, NULL, NULL, NULL, NULL),
('APP2024003', 6, 'MORTGAGE', 400000.00, 360, 'Home purchase', '2024-02-01', 'APPROVED', 'UW003', 'HIGH', 9166.67, 8, NULL, NULL, 500000.00, 100000.00),

-- Pending Applications
('APP2024004', 4, 'BUSINESS', 200000.00, 60, 'Equipment purchase', '2024-02-15', 'UNDER_REVIEW', 'UW001', 'STANDARD', 12500.00, 10, 150000.00, 500000.00, NULL, NULL),
('APP2024005', 3, 'PERSONAL', 75000.00, 48, 'Business investment', '2024-03-01', 'PENDING_DOCUMENTS', 'UW002', 'STANDARD', 10000.00, 8, NULL, NULL, NULL, NULL),

-- Rejected Applications
('APP2024006', 8, 'PERSONAL', 25000.00, 36, 'Emergency expenses', '2024-03-10', 'REJECTED', 'UW003', 'STANDARD', 3750.00, 1, NULL, NULL, NULL, NULL),
('APP2024007', 9, 'PERSONAL', 15000.00, 24, 'Job training', '2024-03-15', 'REJECTED', 'UW001', 'LOW', 0.00, 0, NULL, NULL, NULL, NULL);

-- =============================================================================
-- 3. ACTIVE LOANS - Various stages of servicing
-- =============================================================================

INSERT INTO loans (loan_id, customer_id, application_id, loan_type, principal_amount, interest_rate, term_months, monthly_payment, origination_date, maturity_date, status, current_balance, payments_made, next_payment_date, payment_status, underwriter_id, loan_officer_id) VALUES
-- Performing Loans
('LOAN2024001', 1, 'APP2024001', 'PERSONAL', 45000.00, 9.2, 36, 1456.32, '2024-02-01', '2027-02-01', 'ACTIVE', 38542.15, 6, '2024-08-01', 'CURRENT', 'UW001', 'LO001'),
('LOAN2024002', 2, 'APP2024002', 'PERSONAL', 30000.00, 8.5, 24, 1358.44, '2024-02-15', '2026-02-15', 'ACTIVE', 23456.78, 8, '2024-10-15', 'CURRENT', 'UW002', 'LO002'),
('LOAN2024003', 6, 'APP2024003', 'MORTGAGE', 400000.00, 6.75, 360, 2594.73, '2024-03-01', '2054-03-01', 'ACTIVE', 395234.56, 4, '2024-07-01', 'CURRENT', 'UW003', 'LO003'),

-- Delinquent Loans
('LOAN2024004', 3, NULL, 'PERSONAL', 60000.00, 11.5, 48, 1567.89, '2023-06-15', '2027-06-15', 'DELINQUENT', 52341.23, 12, '2024-06-15', '30_DAYS_PAST_DUE', 'UW001', 'LO001'),
('LOAN2024005', 8, NULL, 'PERSONAL', 20000.00, 15.9, 36, 707.12, '2023-09-01', '2026-09-01', 'DELINQUENT', 16890.45, 9, '2024-06-01', '60_DAYS_PAST_DUE', 'UW002', 'LO002'),

-- Paid Off Loans
('LOAN2023001', 10, NULL, 'PERSONAL', 80000.00, 7.5, 60, 1608.44, '2019-01-01', '2024-01-01', 'PAID_OFF', 0.00, 60, NULL, 'PAID_IN_FULL', 'UW003', 'LO003');

-- =============================================================================
-- 4. LOAN INSTALLMENTS - Payment schedules
-- =============================================================================

-- LOAN2024001 installments (36 months, 6 payments made)
INSERT INTO loan_installments (installment_id, loan_id, installment_number, due_date, principal_amount, interest_amount, total_amount, status, payment_date, actual_amount_paid, late_fee) VALUES
('INST001001', 'LOAN2024001', 1, '2024-03-01', 1081.32, 375.00, 1456.32, 'PAID', '2024-03-01', 1456.32, 0.00),
('INST001002', 'LOAN2024001', 2, '2024-04-01', 1089.65, 366.67, 1456.32, 'PAID', '2024-04-01', 1456.32, 0.00),
('INST001003', 'LOAN2024001', 3, '2024-05-01', 1098.04, 358.28, 1456.32, 'PAID', '2024-05-01', 1456.32, 0.00),
('INST001004', 'LOAN2024001', 4, '2024-06-01', 1106.49, 349.83, 1456.32, 'PAID', '2024-06-01', 1456.32, 0.00),
('INST001005', 'LOAN2024001', 5, '2024-07-01', 1115.00, 341.32, 1456.32, 'PAID', '2024-07-01', 1456.32, 0.00),
('INST001006', 'LOAN2024001', 6, '2024-08-01', 1123.57, 332.75, 1456.32, 'PAID', '2024-08-01', 1456.32, 0.00),
('INST001007', 'LOAN2024001', 7, '2024-09-01', 1132.20, 324.12, 1456.32, 'PENDING', NULL, NULL, NULL),
('INST001008', 'LOAN2024001', 8, '2024-10-01', 1140.89, 315.43, 1456.32, 'PENDING', NULL, NULL, NULL);

-- LOAN2024004 installments (delinquent loan)
INSERT INTO loan_installments (installment_id, loan_id, installment_number, due_date, principal_amount, interest_amount, total_amount, status, payment_date, actual_amount_paid, late_fee) VALUES
('INST004012', 'LOAN2024004', 12, '2024-05-15', 1234.56, 333.33, 1567.89, 'OVERDUE', NULL, NULL, 25.00),
('INST004013', 'LOAN2024004', 13, '2024-06-15', 1245.67, 322.22, 1567.89, 'OVERDUE', NULL, NULL, 25.00),
('INST004014', 'LOAN2024004', 14, '2024-07-15', 1256.89, 310.00, 1567.89, 'PENDING', NULL, NULL, NULL);

-- =============================================================================
-- 5. PAYMENTS - Transaction history
-- =============================================================================

INSERT INTO payments (payment_id, loan_id, customer_id, payment_amount, payment_date, payment_method, payment_type, transaction_id, principal_applied, interest_applied, fees_applied, remaining_balance, processed_by, status) VALUES
-- Regular payments
('PAY001001', 'LOAN2024001', 1, 1456.32, '2024-03-01', 'AUTO_DEBIT', 'REGULAR', 'TXN2024001001', 1081.32, 375.00, 0.00, 43918.68, 'SYSTEM', 'COMPLETED'),
('PAY001002', 'LOAN2024001', 1, 1456.32, '2024-04-01', 'AUTO_DEBIT', 'REGULAR', 'TXN2024001002', 1089.65, 366.67, 0.00, 42829.03, 'SYSTEM', 'COMPLETED'),
('PAY001003', 'LOAN2024001', 1, 1456.32, '2024-05-01', 'AUTO_DEBIT', 'REGULAR', 'TXN2024001003', 1098.04, 358.28, 0.00, 41730.99, 'SYSTEM', 'COMPLETED'),
('PAY001004', 'LOAN2024001', 1, 1456.32, '2024-06-01', 'AUTO_DEBIT', 'REGULAR', 'TXN2024001004', 1106.49, 349.83, 0.00, 40624.50, 'SYSTEM', 'COMPLETED'),
('PAY001005', 'LOAN2024001', 1, 1456.32, '2024-07-01', 'AUTO_DEBIT', 'REGULAR', 'TXN2024001005', 1115.00, 341.32, 0.00, 39509.50, 'SYSTEM', 'COMPLETED'),
('PAY001006', 'LOAN2024001', 1, 1456.32, '2024-08-01', 'AUTO_DEBIT', 'REGULAR', 'TXN2024001006', 1123.57, 332.75, 0.00, 38385.93, 'SYSTEM', 'COMPLETED'),

-- Extra principal payment
('PAY001007', 'LOAN2024001', 1, 2000.00, '2024-08-15', 'ONLINE', 'EXTRA_PRINCIPAL', 'TXN2024001007', 2000.00, 0.00, 0.00, 36385.93, 'LO001', 'COMPLETED'),

-- Late payment with fees
('PAY005001', 'LOAN2024005', 8, 732.12, '2024-06-25', 'PHONE', 'LATE', 'TXN2024005001', 581.12, 126.00, 25.00, 16309.33, 'LO002', 'COMPLETED');

-- =============================================================================
-- 6. UNDERWRITERS AND LOAN OFFICERS
-- =============================================================================

INSERT INTO underwriters (underwriter_id, first_name, last_name, email, phone, specialization, years_experience, approval_limit, status, hire_date) VALUES
('UW001', 'James', 'Wilson', 'james.wilson@bank.com', '555-1001', 'PERSONAL_LOANS', 8, 100000.00, 'ACTIVE', '2019-03-15'),
('UW002', 'Maria', 'Garcia', 'maria.garcia@bank.com', '555-1002', 'BUSINESS_LOANS', 12, 500000.00, 'ACTIVE', '2017-07-22'),
('UW003', 'Robert', 'Chen', 'robert.chen@bank.com', '555-1003', 'MORTGAGES', 15, 1000000.00, 'ACTIVE', '2015-01-10');

INSERT INTO loan_officers (officer_id, first_name, last_name, email, phone, region, portfolio_size, commission_rate, status, hire_date) VALUES
('LO001', 'Susan', 'Thompson', 'susan.thompson@bank.com', '555-2001', 'NORTHEAST', 150, 0.0025, 'ACTIVE', '2020-05-01'),
('LO002', 'Michael', 'Davis', 'michael.davis@bank.com', '555-2002', 'SOUTHEAST', 200, 0.0030, 'ACTIVE', '2018-09-15'),
('LO003', 'Jennifer', 'Lee', 'jennifer.lee@bank.com', '555-2003', 'WEST', 175, 0.0028, 'ACTIVE', '2019-11-20');

-- =============================================================================
-- 7. COMPLIANCE AND AUDIT DATA
-- =============================================================================

INSERT INTO compliance_reports (report_id, report_type, generation_date, reporting_period_start, reporting_period_end, total_loans, total_amount, high_risk_loans, compliance_score, regulatory_findings, generated_by, status) VALUES
('RPT2024Q1001', 'FAIR_LENDING', '2024-04-01', '2024-01-01', '2024-03-31', 125, 15750000.00, 12, 94.5, 2, 'COMPLIANCE_SYSTEM', 'APPROVED'),
('RPT2024Q1002', 'RISK_ASSESSMENT', '2024-04-01', '2024-01-01', '2024-03-31', 125, 15750000.00, 12, 91.2, 5, 'COMPLIANCE_SYSTEM', 'UNDER_REVIEW');

-- =============================================================================
-- 8. CREDIT BUREAU DATA
-- =============================================================================

INSERT INTO credit_reports (report_id, customer_id, bureau_name, report_date, credit_score, payment_history_score, credit_utilization, length_of_history, credit_mix, new_credit, report_data, expiry_date) VALUES
('CR001001', 1, 'EXPERIAN', '2024-01-10', 720, 'EXCELLENT', 0.28, 'GOOD', 'GOOD', 'FAIR', '{"accounts": 8, "inquiries": 2}', '2024-07-10'),
('CR002001', 2, 'EQUIFAX', '2024-01-15', 750, 'EXCELLENT', 0.15, 'EXCELLENT', 'EXCELLENT', 'GOOD', '{"accounts": 12, "inquiries": 1}', '2024-07-15'),
('CR008001', 8, 'TRANSUNION', '2024-03-05', 620, 'FAIR', 0.68, 'FAIR', 'POOR', 'POOR', '{"accounts": 3, "inquiries": 8}', '2024-09-05');

-- =============================================================================
-- 9. RISK ASSESSMENTS
-- =============================================================================

INSERT INTO risk_assessments (assessment_id, customer_id, loan_id, assessment_date, risk_score, risk_category, probability_of_default, loss_given_default, exposure_at_default, risk_factors, mitigation_measures, assessed_by) VALUES
('RA001001', 1, 'LOAN2024001', '2024-01-30', 7.2, 'LOW', 0.05, 0.45, 38542.15, '["stable_employment", "good_credit_history"]', '["regular_monitoring", "payment_alerts"]', 'RISK_ENGINE'),
('RA008001', 8, 'LOAN2024005', '2023-08-25', 18.5, 'HIGH', 0.25, 0.65, 16890.45, '["low_credit_score", "part_time_employment", "high_utilization"]', '["frequent_contact", "payment_plan_option", "early_intervention"]', 'RISK_ENGINE');

-- =============================================================================
-- 10. COLLECTION ACTIVITIES
-- =============================================================================

INSERT INTO collection_activities (activity_id, loan_id, customer_id, activity_date, activity_type, outcome, next_action_date, assigned_collector, notes, priority_level) VALUES
('COL004001', 'LOAN2024004', 3, '2024-06-20', 'PHONE_CALL', 'NO_ANSWER', '2024-06-22', 'COL001', 'Called primary and secondary numbers, left voicemail', 'MEDIUM'),
('COL004002', 'LOAN2024004', 3, '2024-06-22', 'EMAIL', 'SENT', '2024-06-25', 'COL001', 'Sent payment reminder and payment options', 'MEDIUM'),
('COL005001', 'LOAN2024005', 8, '2024-06-10', 'PHONE_CALL', 'CONTACT_MADE', '2024-06-17', 'COL002', 'Customer requested payment arrangement', 'HIGH'),
('COL005002', 'LOAN2024005', 8, '2024-06-17', 'PAYMENT_ARRANGEMENT', 'ARRANGED', '2024-07-01', 'COL002', 'Agreed to $400/month payment plan', 'HIGH');

-- =============================================================================
-- 11. SYSTEM CONFIGURATION FOR TESTING
-- =============================================================================

INSERT INTO system_config (config_key, config_value, config_type, description, environment, last_updated, updated_by) VALUES
-- Interest rates by loan type
('PERSONAL_LOAN_BASE_RATE', '8.5', 'DECIMAL', 'Base interest rate for personal loans', 'ALL', '2024-01-01', 'ADMIN'),
('BUSINESS_LOAN_BASE_RATE', '7.0', 'DECIMAL', 'Base interest rate for business loans', 'ALL', '2024-01-01', 'ADMIN'),
('MORTGAGE_BASE_RATE', '6.5', 'DECIMAL', 'Base interest rate for mortgages', 'ALL', '2024-01-01', 'ADMIN'),

-- Loan limits
('PERSONAL_LOAN_MAX_AMOUNT', '100000', 'INTEGER', 'Maximum personal loan amount', 'ALL', '2024-01-01', 'ADMIN'),
('BUSINESS_LOAN_MAX_AMOUNT', '1000000', 'INTEGER', 'Maximum business loan amount', 'ALL', '2024-01-01', 'ADMIN'),
('MORTGAGE_MAX_AMOUNT', '2000000', 'INTEGER', 'Maximum mortgage amount', 'ALL', '2024-01-01', 'ADMIN'),

-- Credit score requirements
('MIN_CREDIT_SCORE_PERSONAL', '650', 'INTEGER', 'Minimum credit score for personal loans', 'ALL', '2024-01-01', 'ADMIN'),
('MIN_CREDIT_SCORE_BUSINESS', '680', 'INTEGER', 'Minimum credit score for business loans', 'ALL', '2024-01-01', 'ADMIN'),
('MIN_CREDIT_SCORE_MORTGAGE', '620', 'INTEGER', 'Minimum credit score for mortgages', 'ALL', '2024-01-01', 'ADMIN'),

-- Processing fees
('ORIGINATION_FEE_RATE', '1.0', 'DECIMAL', 'Origination fee as percentage of loan amount', 'ALL', '2024-01-01', 'ADMIN'),
('LATE_PAYMENT_FEE', '25.00', 'DECIMAL', 'Late payment fee amount', 'ALL', '2024-01-01', 'ADMIN'),
('PREPAYMENT_PENALTY_RATE', '2.0', 'DECIMAL', 'Prepayment penalty rate', 'ALL', '2024-01-01', 'ADMIN');

-- =============================================================================
-- 12. AI/ML TRAINING DATA
-- =============================================================================

INSERT INTO ml_training_data (record_id, customer_profile, loan_characteristics, economic_indicators, outcome, prediction_accuracy, training_date, model_version) VALUES
('ML001001', '{"credit_score": 720, "income": 85000, "debt_ratio": 0.35}', '{"amount": 50000, "type": "PERSONAL", "term": 36}', '{"prime_rate": 7.5, "unemployment": 3.8}', 'APPROVED', 0.95, '2024-01-15', 'v2.1'),
('ML001002', '{"credit_score": 620, "income": 45000, "debt_ratio": 0.55}', '{"amount": 25000, "type": "PERSONAL", "term": 36}', '{"prime_rate": 7.5, "unemployment": 3.8}', 'REJECTED', 0.92, '2024-01-15', 'v2.1'),
('ML001003', '{"credit_score": 750, "income": 95000, "debt_ratio": 0.28}', '{"amount": 30000, "type": "PERSONAL", "term": 24}', '{"prime_rate": 7.5, "unemployment": 3.8}', 'APPROVED', 0.98, '2024-01-15', 'v2.1');

-- =============================================================================
-- 13. API USAGE ANALYTICS
-- =============================================================================

INSERT INTO api_usage_logs (log_id, endpoint, method, request_timestamp, response_time_ms, status_code, user_agent, ip_address, customer_id, loan_id, request_size, response_size) VALUES
('LOG001001', '/api/customers', 'GET', '2024-06-19 10:30:00', 245, 200, 'PostmanRuntime/7.32.2', '192.168.1.100', NULL, NULL, 0, 1024),
('LOG001002', '/api/loans/LOAN2024001', 'GET', '2024-06-19 10:31:15', 180, 200, 'PostmanRuntime/7.32.2', '192.168.1.100', 1, 'LOAN2024001', 0, 2048),
('LOG001003', '/api/loans/apply', 'POST', '2024-06-19 10:32:30', 1250, 201, 'PostmanRuntime/7.32.2', '192.168.1.100', 1, NULL, 512, 256);

COMMIT;

-- =============================================================================
-- Data Verification Queries
-- =============================================================================

-- Verify customer count by type
-- SELECT customer_type, COUNT(*) as count FROM customers GROUP BY customer_type;

-- Verify loan distribution by status
-- SELECT status, COUNT(*) as count FROM loans GROUP BY status;

-- Verify payment distribution by type
-- SELECT payment_type, COUNT(*) as count FROM payments GROUP BY payment_type;

-- Verify delinquency status
-- SELECT payment_status, COUNT(*) as count FROM loans WHERE payment_status IS NOT NULL GROUP BY payment_status;