-- Enhanced Sample Data for Enterprise Loan Management System
-- Compatible with implemented database schema and entity mappings
-- Environment: Development/SIT/UAT

-- =============================================================================
-- 1. CUSTOMERS - Using comprehensive customer_management schema
-- =============================================================================

-- Note: Using customer_management.customers table with proper field mapping
INSERT INTO customer_management.customers (
    customer_number, first_name, last_name, email, phone_number, date_of_birth, ssn, 
    credit_score, annual_income, employment_status, address_line1, address_line2, 
    city, state, zip_code, country, status
) VALUES
-- Personal Loan Applicants
('CUST0000001', 'John', 'Smith', 'john.smith@email.com', '555-0101', '1985-03-15', '123-45-6789', 
 720, 85000.00, 'EMPLOYED', '123 Main St', NULL, 'New York', 'NY', '10001', 'USA', 'ACTIVE'),

('CUST0000002', 'Sarah', 'Johnson', 'sarah.johnson@email.com', '555-0102', '1990-07-22', '234-56-7890', 
 750, 95000.00, 'EMPLOYED', '456 Oak Ave', NULL, 'Los Angeles', 'CA', '90210', 'USA', 'ACTIVE'),

('CUST0000003', 'Michael', 'Brown', 'michael.brown@email.com', '555-0103', '1982-11-08', '345-67-8901', 
 680, 120000.00, 'SELF_EMPLOYED', '789 Pine Rd', NULL, 'Chicago', 'IL', '60601', 'USA', 'ACTIVE'),

-- Business Loan Applicants
('CUST0000004', 'Jennifer', 'Davis', 'jennifer.davis@techstartup.com', '555-0104', '1988-05-12', '456-78-9012', 
 700, 150000.00, 'BUSINESS_OWNER', '321 Business Blvd', NULL, 'Austin', 'TX', '73301', 'USA', 'ACTIVE'),

('CUST0000005', 'Robert', 'Wilson', 'robert.wilson@manufacturing.com', '555-0105', '1975-09-30', '567-89-0123', 
 740, 200000.00, 'BUSINESS_OWNER', '654 Industrial Way', NULL, 'Detroit', 'MI', '48201', 'USA', 'ACTIVE'),

-- Mortgage Applicants
('CUST0000006', 'Lisa', 'Anderson', 'lisa.anderson@email.com', '555-0106', '1987-12-03', '678-90-1234', 
 760, 110000.00, 'EMPLOYED', '987 Suburban Dr', NULL, 'Denver', 'CO', '80201', 'USA', 'ACTIVE'),

('CUST0000007', 'David', 'Martinez', 'david.martinez@email.com', '555-0107', '1983-04-18', '789-01-2345', 
 710, 130000.00, 'EMPLOYED', '147 Family Lane', NULL, 'Phoenix', 'AZ', '85001', 'USA', 'ACTIVE'),

-- High-Risk Profiles
('CUST0000008', 'Amanda', 'Taylor', 'amanda.taylor@email.com', '555-0108', '1992-08-25', '890-12-3456', 
 620, 45000.00, 'PART_TIME', '258 Starter St', NULL, 'Miami', 'FL', '33101', 'USA', 'ACTIVE'),

('CUST0000009', 'Christopher', 'Moore', 'chris.moore@email.com', '555-0109', '1995-01-14', '901-23-4567', 
 580, 0.00, 'UNEMPLOYED', '369 Struggle Ave', NULL, 'Seattle', 'WA', '98101', 'USA', 'ACTIVE'),

-- Premium Customers
('CUST0000010', 'Elizabeth', 'White', 'elizabeth.white@email.com', '555-0110', '1978-06-07', '012-34-5678', 
 800, 250000.00, 'EXECUTIVE', '741 Executive Dr', NULL, 'San Francisco', 'CA', '94101', 'USA', 'ACTIVE');

-- =============================================================================
-- 2. UNDERWRITERS - Staff management
-- =============================================================================

INSERT INTO underwriters (
    underwriter_id, first_name, last_name, email, phone, specialization, 
    years_experience, approval_limit, status, hire_date
) VALUES
('UW001', 'James', 'Wilson', 'james.wilson@bank.com', '555-1001', 'PERSONAL_LOANS', 
 8, 100000.00, 'ACTIVE', '2019-03-15'),

('UW002', 'Maria', 'Garcia', 'maria.garcia@bank.com', '555-1002', 'BUSINESS_LOANS', 
 12, 500000.00, 'ACTIVE', '2017-07-22'),

('UW003', 'Robert', 'Chen', 'robert.chen@bank.com', '555-1003', 'MORTGAGES', 
 15, 1000000.00, 'ACTIVE', '2015-01-10');

-- =============================================================================
-- 3. LOAN OFFICERS - Sales staff
-- =============================================================================

INSERT INTO loan_officers (
    officer_id, first_name, last_name, email, phone, region, 
    portfolio_size, commission_rate, status, hire_date
) VALUES
('LO001', 'Susan', 'Thompson', 'susan.thompson@bank.com', '555-2001', 'NORTHEAST', 
 150, 0.0025, 'ACTIVE', '2020-05-01'),

('LO002', 'Michael', 'Davis', 'michael.davis@bank.com', '555-2002', 'SOUTHEAST', 
 200, 0.0030, 'ACTIVE', '2018-09-15'),

('LO003', 'Jennifer', 'Lee', 'jennifer.lee@bank.com', '555-2003', 'WEST', 
 175, 0.0028, 'ACTIVE', '2019-11-20');

-- =============================================================================
-- 4. LOAN APPLICATIONS - Various stages
-- =============================================================================

INSERT INTO loan_applications (
    application_id, customer_id, loan_type, requested_amount, requested_term_months, 
    purpose, application_date, status, assigned_underwriter, priority, 
    monthly_income, employment_years, collateral_value, business_revenue, 
    property_value, down_payment
) VALUES
-- Approved Applications
('APP2024001', 1, 'PERSONAL', 50000.00, 36, 'Home renovation', '2024-01-15', 
 'APPROVED', 'UW001', 'STANDARD', 7083.33, 5, NULL, NULL, NULL, NULL),

('APP2024002', 2, 'PERSONAL', 30000.00, 24, 'Debt consolidation', '2024-01-20', 
 'APPROVED', 'UW001', 'STANDARD', 7916.67, 3, NULL, NULL, NULL, NULL),

('APP2024003', 6, 'MORTGAGE', 400000.00, 360, 'Home purchase', '2024-02-01', 
 'APPROVED', 'UW003', 'HIGH', 9166.67, 8, NULL, NULL, 500000.00, 100000.00),

-- Pending Applications
('APP2024004', 4, 'BUSINESS', 200000.00, 60, 'Equipment purchase', '2024-02-15', 
 'UNDER_REVIEW', 'UW002', 'STANDARD', 12500.00, 10, 150000.00, 500000.00, NULL, NULL),

('APP2024005', 3, 'PERSONAL', 75000.00, 48, 'Business investment', '2024-03-01', 
 'PENDING_DOCUMENTS', 'UW001', 'STANDARD', 10000.00, 8, NULL, NULL, NULL, NULL),

-- Rejected Applications
('APP2024006', 8, 'PERSONAL', 25000.00, 36, 'Emergency expenses', '2024-03-10', 
 'REJECTED', 'UW001', 'STANDARD', 3750.00, 1, NULL, NULL, NULL, NULL),

('APP2024007', 9, 'PERSONAL', 15000.00, 24, 'Job training', '2024-03-15', 
 'REJECTED', 'UW001', 'LOW', 0.00, 0, NULL, NULL, NULL, NULL);

-- =============================================================================
-- 5. ACTIVE LOANS - Using simple loans table structure
-- =============================================================================

-- Note: Using existing loans table structure for compatibility
INSERT INTO loans (
    id, customer_id, loan_amount, number_of_installments, interest_rate, 
    create_date, is_paid
) VALUES
-- Performing Loans
('LOAN2024001', 1, 45000.00, 36, 0.092, '2024-02-01', FALSE),
('LOAN2024002', 2, 30000.00, 24, 0.085, '2024-02-15', FALSE),
('LOAN2024003', 6, 400000.00, 360, 0.0675, '2024-03-01', FALSE),

-- Delinquent Loans
('LOAN2024004', 3, 60000.00, 48, 0.115, '2023-06-15', FALSE),
('LOAN2024005', 8, 20000.00, 36, 0.159, '2023-09-01', FALSE),

-- Paid Off Loans
('LOAN2023001', 10, 80000.00, 60, 0.075, '2019-01-01', TRUE);

-- =============================================================================
-- 6. COMPLIANCE REPORTS - Regulatory reporting
-- =============================================================================

INSERT INTO compliance_reports (
    report_id, report_type, generation_date, reporting_period_start, 
    reporting_period_end, total_loans, total_amount, high_risk_loans, 
    compliance_score, regulatory_findings, generated_by, status
) VALUES
('RPT2024Q1001', 'FAIR_LENDING', '2024-04-01', '2024-01-01', '2024-03-31', 
 125, 15750000.00, 12, 94.5, 2, 'COMPLIANCE_SYSTEM', 'APPROVED'),

('RPT2024Q1002', 'RISK_ASSESSMENT', '2024-04-01', '2024-01-01', '2024-03-31', 
 125, 15750000.00, 12, 91.2, 5, 'COMPLIANCE_SYSTEM', 'UNDER_REVIEW');

-- =============================================================================
-- 7. CREDIT REPORTS - Bureau integration
-- =============================================================================

INSERT INTO credit_reports (
    report_id, customer_id, bureau_name, report_date, credit_score, 
    payment_history_score, credit_utilization, length_of_history, 
    credit_mix, new_credit, report_data, expiry_date, request_reason, 
    requested_by, status
) VALUES
('CR001001', 1, 'EXPERIAN', '2024-01-10', 720, 'EXCELLENT', 0.28, 'GOOD', 
 'GOOD', 'FAIR', '{"accounts": 8, "inquiries": 2}', '2024-07-10', 
 'LOAN_APPLICATION', 'UW001', 'ACTIVE'),

('CR002001', 2, 'EQUIFAX', '2024-01-15', 750, 'EXCELLENT', 0.15, 'EXCELLENT', 
 'EXCELLENT', 'GOOD', '{"accounts": 12, "inquiries": 1}', '2024-07-15', 
 'LOAN_APPLICATION', 'UW001', 'ACTIVE'),

('CR008001', 8, 'TRANSUNION', '2024-03-05', 620, 'FAIR', 0.68, 'FAIR', 
 'POOR', 'POOR', '{"accounts": 3, "inquiries": 8}', '2024-09-05', 
 'LOAN_APPLICATION', 'UW001', 'ACTIVE');

-- =============================================================================
-- 8. RISK ASSESSMENTS - Risk management
-- =============================================================================

INSERT INTO risk_assessments (
    assessment_id, customer_id, loan_id, assessment_date, assessment_type, 
    risk_score, risk_category, probability_of_default, loss_given_default, 
    exposure_at_default, risk_factors, mitigation_measures, model_version, 
    assessed_by, status
) VALUES
('RA001001', 1, 'LOAN2024001', '2024-01-30', 'APPLICATION', 7.2, 'LOW', 
 0.05, 0.45, 38542.15, '["stable_employment", "good_credit_history"]', 
 '["regular_monitoring", "payment_alerts"]', 'v2.1', 'RISK_ENGINE', 'APPROVED'),

('RA008001', 8, 'LOAN2024005', '2023-08-25', 'APPLICATION', 18.5, 'HIGH', 
 0.25, 0.65, 16890.45, '["low_credit_score", "part_time_employment", "high_utilization"]', 
 '["frequent_contact", "payment_plan_option", "early_intervention"]', 'v2.1', 'RISK_ENGINE', 'APPROVED');

-- =============================================================================
-- 9. PAYMENT DATA - Using payment_processing schema
-- =============================================================================

-- Note: Using payment_processing.payments table structure
INSERT INTO payment_processing.payments (
    payment_id, loan_id, customer_id, payment_amount, payment_date, 
    payment_method, payment_type, status, principal_applied, 
    interest_applied, fees_applied, processed_by
) VALUES
-- Regular payments for LOAN2024001
('PAY001001', 'LOAN2024001', 1, 1456.32, '2024-03-01', 'AUTO_DEBIT', 
 'REGULAR', 'COMPLETED', 1081.32, 375.00, 0.00, 'SYSTEM'),

('PAY001002', 'LOAN2024001', 1, 1456.32, '2024-04-01', 'AUTO_DEBIT', 
 'REGULAR', 'COMPLETED', 1089.65, 366.67, 0.00, 'SYSTEM'),

('PAY001003', 'LOAN2024001', 1, 1456.32, '2024-05-01', 'AUTO_DEBIT', 
 'REGULAR', 'COMPLETED', 1098.04, 358.28, 0.00, 'SYSTEM'),

-- Extra principal payment
('PAY001007', 'LOAN2024001', 1, 2000.00, '2024-08-15', 'ONLINE', 
 'EXTRA_PRINCIPAL', 'COMPLETED', 2000.00, 0.00, 0.00, 'LO001'),

-- Late payment with fees
('PAY005001', 'LOAN2024005', 8, 732.12, '2024-06-25', 'PHONE', 
 'LATE', 'COMPLETED', 581.12, 126.00, 25.00, 'LO002');

-- =============================================================================
-- 10. CUSTOMER DOCUMENTS - Document verification
-- =============================================================================

INSERT INTO customer_management.customer_documents (
    customer_id, document_type, document_number, file_path, verification_status
) VALUES
(1, 'DRIVERS_LICENSE', 'NY123456789', '/documents/customer_1/drivers_license.pdf', 'VERIFIED'),
(1, 'INCOME_STATEMENT', 'W2-2023', '/documents/customer_1/w2_2023.pdf', 'VERIFIED'),
(2, 'DRIVERS_LICENSE', 'CA987654321', '/documents/customer_2/drivers_license.pdf', 'VERIFIED'),
(3, 'BUSINESS_LICENSE', 'IL-BIZ-456789', '/documents/customer_3/business_license.pdf', 'PENDING'),
(6, 'PROPERTY_DEED', 'DEED-CO-789', '/documents/customer_6/property_deed.pdf', 'VERIFIED'),
(8, 'DRIVERS_LICENSE', 'FL456789123', '/documents/customer_8/drivers_license.pdf', 'REJECTED');

-- =============================================================================
-- 11. LOAN INSTALLMENTS - Payment schedules
-- =============================================================================

-- Note: Using loan_installments table if it exists, otherwise this data supports the loans
INSERT INTO loan_installments (
    loan_id, installment_number, due_date, amount, principal_amount, 
    interest_amount, paid_amount, status
) VALUES
-- LOAN2024001 installments (36 months, 6 payments made)
('LOAN2024001', 1, '2024-03-01', 1456.32, 1081.32, 375.00, 1456.32, 'PAID'),
('LOAN2024001', 2, '2024-04-01', 1456.32, 1089.65, 366.67, 1456.32, 'PAID'),
('LOAN2024001', 3, '2024-05-01', 1456.32, 1098.04, 358.28, 1456.32, 'PAID'),
('LOAN2024001', 4, '2024-06-01', 1456.32, 1106.49, 349.83, 1456.32, 'PAID'),
('LOAN2024001', 5, '2024-07-01', 1456.32, 1115.00, 341.32, 1456.32, 'PAID'),
('LOAN2024001', 6, '2024-08-01', 1456.32, 1123.57, 332.75, 1456.32, 'PAID'),
('LOAN2024001', 7, '2024-09-01', 1456.32, 1132.20, 324.12, 0.00, 'PENDING'),
('LOAN2024001', 8, '2024-10-01', 1456.32, 1140.89, 315.43, 0.00, 'PENDING');

-- =============================================================================
-- 12. EVENT SOURCING DATA - Domain events
-- =============================================================================

-- Customer events
INSERT INTO customer_management.customer_events (
    customer_id, event_type, event_data
) VALUES
(1, 'CustomerCreated', '{"customerId": 1, "createdBy": "SYSTEM", "timestamp": "2024-01-15T10:00:00Z"}'),
(1, 'CreditScoreUpdated', '{"customerId": 1, "oldScore": 700, "newScore": 720, "updatedBy": "CREDIT_BUREAU", "timestamp": "2024-01-20T14:30:00Z"}'),
(2, 'CustomerCreated', '{"customerId": 2, "createdBy": "LO001", "timestamp": "2024-02-20T11:15:00Z"}'),
(8, 'CustomerSuspended', '{"customerId": 8, "reason": "PAYMENT_DEFAULT", "suspendedBy": "COLLECTIONS", "timestamp": "2024-06-30T16:45:00Z"}');

COMMIT;

-- =============================================================================
-- Data Verification Queries
-- =============================================================================

-- Verify customer count by schema
-- SELECT COUNT(*) as customer_count FROM customer_management.customers;

-- Verify staff assignments
-- SELECT u.underwriter_id, u.specialization, COUNT(la.application_id) as applications_assigned 
-- FROM underwriters u 
-- LEFT JOIN loan_applications la ON u.underwriter_id = la.assigned_underwriter 
-- GROUP BY u.underwriter_id, u.specialization;

-- Verify loan application workflow
-- SELECT status, COUNT(*) as count FROM loan_applications GROUP BY status;

-- Verify payment processing
-- SELECT payment_type, COUNT(*) as count FROM payment_processing.payments GROUP BY payment_type;

-- Verify compliance reporting
-- SELECT report_type, status, COUNT(*) as count FROM compliance_reports GROUP BY report_type, status;

-- =============================================================================
-- Notes for Implementation
-- =============================================================================

/*
This enhanced sample data is designed to work with the implemented database schema:

1. Uses customer_management.customers table (comprehensive schema)
2. Uses new underwriters and loan_officers tables
3. Uses new loan_applications table for workflow management
4. Uses payment_processing.payments table (bounded context)
5. Uses new compliance_reports table for regulatory reporting
6. Uses new credit_reports table for bureau integration
7. Uses new risk_assessments table for risk management
8. Includes event sourcing data for audit trails

Foreign key relationships are properly maintained:
- customer_management.customers.id -> loan_applications.customer_id
- underwriters.underwriter_id -> loan_applications.assigned_underwriter
- customers.id -> loans.customer_id (simple schema compatibility)
- payment_processing.payments references loans and customers

The data supports testing of:
- Complete loan application workflow
- Staff assignment and workload management
- Payment processing with proper allocation
- Regulatory compliance reporting
- Credit bureau integration
- Risk assessment and management
- Event sourcing and audit trails

Schema Compatibility:
- Works with both bounded context and simple schemas
- Provides migration path from simple to complex schema
- Supports all sample data requirements from original specification
*/