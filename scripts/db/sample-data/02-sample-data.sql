-- Enterprise Loan Management System - Sample Data Population
-- Comprehensive sample data for testing and demonstration

-- Set search path
SET search_path TO banking_core, banking_customer, banking_loan, banking_payment, banking_compliance, banking_audit, banking_ml, banking_federation, public;

-- ===== SAMPLE PARTY DATA =====

-- Sample parties for different customer types
INSERT INTO banking_core.parties (party_id, party_type, party_status, compliance_level, created_by, updated_by, entity_tag) VALUES
    ('550e8400-e29b-41d4-a716-446655440001', 'INDIVIDUAL', 'ACTIVE', 'PREMIUM', 'system', 'system', 'INDIVIDUAL_001'),
    ('550e8400-e29b-41d4-a716-446655440002', 'INDIVIDUAL', 'ACTIVE', 'ENHANCED', 'system', 'system', 'INDIVIDUAL_002'),
    ('550e8400-e29b-41d4-a716-446655440003', 'INDIVIDUAL', 'ACTIVE', 'BASIC', 'system', 'system', 'INDIVIDUAL_003'),
    ('550e8400-e29b-41d4-a716-446655440004', 'CORPORATE', 'ACTIVE', 'PREMIUM', 'system', 'system', 'CORPORATE_001'),
    ('550e8400-e29b-41d4-a716-446655440005', 'CORPORATE', 'ACTIVE', 'ENHANCED', 'system', 'system', 'CORPORATE_002'),
    ('550e8400-e29b-41d4-a716-446655440006', 'GOVERNMENT', 'ACTIVE', 'PREMIUM', 'system', 'system', 'GOVERNMENT_001');

-- Sample party groups
INSERT INTO banking_core.party_groups (group_id, group_name, group_type, created_by, updated_by) VALUES
    ('660e8400-e29b-41d4-a716-446655440001', 'ABC Corporation Family', 'FAMILY', 'system', 'system'),
    ('660e8400-e29b-41d4-a716-446655440002', 'TechStart Inc Group', 'BUSINESS', 'system', 'system'),
    ('660e8400-e29b-41d4-a716-446655440003', 'Federal Agency Division', 'SUBSIDIARY', 'system', 'system');

-- Sample party roles
INSERT INTO banking_core.party_roles (role_id, party_id, role_name, role_source, effective_from, created_by) VALUES
    ('770e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440001', 'PREMIUM_CUSTOMER', 'INTERNAL', '2024-01-01', 'system'),
    ('770e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440002', 'STANDARD_CUSTOMER', 'INTERNAL', '2024-01-15', 'system'),
    ('770e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440003', 'BASIC_CUSTOMER', 'INTERNAL', '2024-02-01', 'system'),
    ('770e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440004', 'CORPORATE_CLIENT', 'INTERNAL', '2024-01-10', 'system'),
    ('770e8400-e29b-41d4-a716-446655440005', '550e8400-e29b-41d4-a716-446655440005', 'SME_CLIENT', 'INTERNAL', '2024-01-20', 'system'),
    ('770e8400-e29b-41d4-a716-446655440006', '550e8400-e29b-41d4-a716-446655440006', 'GOVERNMENT_CLIENT', 'INTERNAL', '2024-01-05', 'system');

-- ===== SAMPLE CUSTOMER DATA =====

-- Sample customers with different profiles
INSERT INTO banking_customer.customers (customer_id, party_id, customer_number, customer_status, customer_type, risk_rating, credit_score, credit_limit, available_credit, kyc_status, onboarding_date, created_by, updated_by) VALUES
    ('110e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440001', 'CUST-001', 'ACTIVE', 'INDIVIDUAL', 'LOW', 785, 150000.00, 145000.00, 'VERIFIED', '2024-01-01', 'system', 'system'),
    ('110e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440002', 'CUST-002', 'ACTIVE', 'INDIVIDUAL', 'MEDIUM', 720, 75000.00, 70000.00, 'VERIFIED', '2024-01-15', 'system', 'system'),
    ('110e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440003', 'CUST-003', 'ACTIVE', 'INDIVIDUAL', 'HIGH', 650, 25000.00, 22000.00, 'VERIFIED', '2024-02-01', 'system', 'system'),
    ('110e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440004', 'CORP-001', 'ACTIVE', 'CORPORATE', 'LOW', 800, 2000000.00, 1800000.00, 'VERIFIED', '2024-01-10', 'system', 'system'),
    ('110e8400-e29b-41d4-a716-446655440005', '550e8400-e29b-41d4-a716-446655440005', 'CORP-002', 'ACTIVE', 'CORPORATE', 'MEDIUM', 740, 500000.00, 450000.00, 'VERIFIED', '2024-01-20', 'system', 'system'),
    ('110e8400-e29b-41d4-a716-446655440006', '550e8400-e29b-41d4-a716-446655440006', 'GOV-001', 'ACTIVE', 'GOVERNMENT', 'LOW', 750, 5000000.00, 4500000.00, 'VERIFIED', '2024-01-05', 'system', 'system');

-- Sample personal information
INSERT INTO banking_customer.customer_personal_info (personal_info_id, customer_id, first_name, last_name, date_of_birth, nationality, employment_status, annual_income, occupation, employer_name, created_by, updated_by) VALUES
    ('220e8400-e29b-41d4-a716-446655440001', '110e8400-e29b-41d4-a716-446655440001', 'John', 'Smith', '1985-05-15', 'USA', 'EMPLOYED', 120000.00, 'Software Engineer', 'Tech Corp Inc', 'system', 'system'),
    ('220e8400-e29b-41d4-a716-446655440002', '110e8400-e29b-41d4-a716-446655440002', 'Sarah', 'Johnson', '1990-08-22', 'USA', 'EMPLOYED', 85000.00, 'Marketing Manager', 'Marketing Solutions LLC', 'system', 'system'),
    ('220e8400-e29b-41d4-a716-446655440003', '110e8400-e29b-41d4-a716-446655440003', 'Michael', 'Brown', '1995-12-03', 'USA', 'SELF_EMPLOYED', 45000.00, 'Freelance Designer', 'Self-Employed', 'system', 'system');

-- Sample addresses
INSERT INTO banking_customer.customer_addresses (address_id, customer_id, address_type, street_address, city, state_province, postal_code, country, is_primary, created_by, updated_by) VALUES
    ('330e8400-e29b-41d4-a716-446655440001', '110e8400-e29b-41d4-a716-446655440001', 'HOME', '123 Main Street', 'New York', 'NY', '10001', 'USA', TRUE, 'system', 'system'),
    ('330e8400-e29b-41d4-a716-446655440002', '110e8400-e29b-41d4-a716-446655440002', 'HOME', '456 Oak Avenue', 'Los Angeles', 'CA', '90210', 'USA', TRUE, 'system', 'system'),
    ('330e8400-e29b-41d4-a716-446655440003', '110e8400-e29b-41d4-a716-446655440003', 'HOME', '789 Pine Road', 'Chicago', 'IL', '60601', 'USA', TRUE, 'system', 'system'),
    ('330e8400-e29b-41d4-a716-446655440004', '110e8400-e29b-41d4-a716-446655440004', 'WORK', '100 Corporate Plaza', 'San Francisco', 'CA', '94105', 'USA', TRUE, 'system', 'system'),
    ('330e8400-e29b-41d4-a716-446655440005', '110e8400-e29b-41d4-a716-446655440005', 'WORK', '200 Business Center', 'Seattle', 'WA', '98101', 'USA', TRUE, 'system', 'system'),
    ('330e8400-e29b-41d4-a716-446655440006', '110e8400-e29b-41d4-a716-446655440006', 'WORK', '300 Federal Building', 'Washington', 'DC', '20001', 'USA', TRUE, 'system', 'system');

-- Sample contact information
INSERT INTO banking_customer.customer_contacts (contact_id, customer_id, contact_type, contact_value, is_primary, is_verified, verification_date, created_by, updated_by) VALUES
    ('440e8400-e29b-41d4-a716-446655440001', '110e8400-e29b-41d4-a716-446655440001', 'EMAIL', 'john.smith@email.com', TRUE, TRUE, '2024-01-01', 'system', 'system'),
    ('440e8400-e29b-41d4-a716-446655440002', '110e8400-e29b-41d4-a716-446655440001', 'MOBILE', '+1-555-0101', TRUE, TRUE, '2024-01-01', 'system', 'system'),
    ('440e8400-e29b-41d4-a716-446655440003', '110e8400-e29b-41d4-a716-446655440002', 'EMAIL', 'sarah.johnson@email.com', TRUE, TRUE, '2024-01-15', 'system', 'system'),
    ('440e8400-e29b-41d4-a716-446655440004', '110e8400-e29b-41d4-a716-446655440002', 'MOBILE', '+1-555-0102', TRUE, TRUE, '2024-01-15', 'system', 'system'),
    ('440e8400-e29b-41d4-a716-446655440005', '110e8400-e29b-41d4-a716-446655440003', 'EMAIL', 'michael.brown@email.com', TRUE, TRUE, '2024-02-01', 'system', 'system'),
    ('440e8400-e29b-41d4-a716-446655440006', '110e8400-e29b-41d4-a716-446655440003', 'MOBILE', '+1-555-0103', TRUE, TRUE, '2024-02-01', 'system', 'system');

-- ===== SAMPLE LOAN DATA =====

-- Sample loan applications
INSERT INTO banking_loan.loan_applications (application_id, customer_id, application_number, application_status, loan_type, requested_amount, requested_term_months, purpose_of_loan, application_date, decision_date, submitted_at, approved_at, created_by, updated_by) VALUES
    ('880e8400-e29b-41d4-a716-446655440001', '110e8400-e29b-41d4-a716-446655440001', 'APP-001', 'APPROVED', 'PERSONAL', 25000.00, 60, 'Home renovation', '2024-01-15', '2024-01-20', '2024-01-15 10:00:00', '2024-01-20 14:30:00', 'system', 'system'),
    ('880e8400-e29b-41d4-a716-446655440002', '110e8400-e29b-41d4-a716-446655440002', 'APP-002', 'APPROVED', 'AUTO', 35000.00, 72, 'Vehicle purchase', '2024-01-25', '2024-01-30', '2024-01-25 09:00:00', '2024-01-30 16:00:00', 'system', 'system'),
    ('880e8400-e29b-41d4-a716-446655440003', '110e8400-e29b-41d4-a716-446655440003', 'APP-003', 'UNDER_REVIEW', 'PERSONAL', 15000.00, 48, 'Debt consolidation', '2024-02-05', NULL, '2024-02-05 11:00:00', NULL, 'system', 'system'),
    ('880e8400-e29b-41d4-a716-446655440004', '110e8400-e29b-41d4-a716-446655440004', 'APP-004', 'APPROVED', 'BUSINESS', 500000.00, 120, 'Equipment purchase', '2024-01-12', '2024-01-18', '2024-01-12 14:00:00', '2024-01-18 10:00:00', 'system', 'system'),
    ('880e8400-e29b-41d4-a716-446655440005', '110e8400-e29b-41d4-a716-446655440005', 'APP-005', 'APPROVED', 'BUSINESS', 150000.00, 84, 'Working capital', '2024-01-22', '2024-01-28', '2024-01-22 15:00:00', '2024-01-28 11:00:00', 'system', 'system');

-- Sample loans
INSERT INTO banking_loan.loans (loan_id, application_id, customer_id, loan_number, loan_status, loan_type, principal_amount, outstanding_balance, interest_rate, term_months, remaining_term_months, monthly_payment, next_payment_date, disbursement_date, maturity_date, created_by, updated_by) VALUES
    ('990e8400-e29b-41d4-a716-446655440001', '880e8400-e29b-41d4-a716-446655440001', '110e8400-e29b-41d4-a716-446655440001', 'LOAN-001', 'ACTIVE', 'PERSONAL', 25000.00, 23500.00, 0.0675, 60, 57, 486.87, '2024-05-01', '2024-02-01', '2029-02-01', 'system', 'system'),
    ('990e8400-e29b-41d4-a716-446655440002', '880e8400-e29b-41d4-a716-446655440002', '110e8400-e29b-41d4-a716-446655440002', 'LOAN-002', 'ACTIVE', 'AUTO', 35000.00, 33200.00, 0.0550, 72, 69, 569.23, '2024-05-01', '2024-02-05', '2030-02-05', 'system', 'system'),
    ('990e8400-e29b-41d4-a716-446655440003', '880e8400-e29b-41d4-a716-446655440004', '110e8400-e29b-41d4-a716-446655440004', 'LOAN-003', 'ACTIVE', 'BUSINESS', 500000.00, 485000.00, 0.0475, 120, 117, 5185.32, '2024-05-01', '2024-02-01', '2034-02-01', 'system', 'system'),
    ('990e8400-e29b-41d4-a716-446655440004', '880e8400-e29b-41d4-a716-446655440005', '110e8400-e29b-41d4-a716-446655440005', 'LOAN-004', 'ACTIVE', 'BUSINESS', 150000.00, 145000.00, 0.0525, 84, 81, 2156.78, '2024-05-01', '2024-02-01', '2031-02-01', 'system', 'system');

-- Sample loan installments
INSERT INTO banking_loan.loan_installments (installment_id, loan_id, installment_number, due_date, principal_amount, interest_amount, total_amount, installment_status, paid_amount, paid_date, created_by, updated_by) VALUES
    ('aa0e8400-e29b-41d4-a716-446655440001', '990e8400-e29b-41d4-a716-446655440001', 1, '2024-03-01', 345.62, 141.25, 486.87, 'PAID', 486.87, '2024-03-01', 'system', 'system'),
    ('aa0e8400-e29b-41d4-a716-446655440002', '990e8400-e29b-41d4-a716-446655440001', 2, '2024-04-01', 347.55, 139.32, 486.87, 'PAID', 486.87, '2024-04-01', 'system', 'system'),
    ('aa0e8400-e29b-41d4-a716-446655440003', '990e8400-e29b-41d4-a716-446655440001', 3, '2024-05-01', 349.49, 137.38, 486.87, 'PENDING', 0.00, NULL, 'system', 'system'),
    ('aa0e8400-e29b-41d4-a716-446655440004', '990e8400-e29b-41d4-a716-446655440002', 1, '2024-03-05', 407.85, 161.38, 569.23, 'PAID', 569.23, '2024-03-05', 'system', 'system'),
    ('aa0e8400-e29b-41d4-a716-446655440005', '990e8400-e29b-41d4-a716-446655440002', 2, '2024-04-05', 409.73, 159.50, 569.23, 'PAID', 569.23, '2024-04-05', 'system', 'system'),
    ('aa0e8400-e29b-41d4-a716-446655440006', '990e8400-e29b-41d4-a716-446655440002', 3, '2024-05-05', 411.62, 157.61, 569.23, 'PENDING', 0.00, NULL, 'system', 'system');

-- ===== SAMPLE PAYMENT DATA =====

-- Sample payments
INSERT INTO banking_payment.payments (payment_id, loan_id, customer_id, payment_reference, payment_type, payment_method, payment_status, amount, currency, payment_date, processing_date, completion_date, created_by, updated_by) VALUES
    ('bb0e8400-e29b-41d4-a716-446655440001', '990e8400-e29b-41d4-a716-446655440001', '110e8400-e29b-41d4-a716-446655440001', 'PAY-001', 'LOAN_PAYMENT', 'BANK_TRANSFER', 'COMPLETED', 486.87, 'USD', '2024-03-01', '2024-03-01', '2024-03-01', 'system', 'system'),
    ('bb0e8400-e29b-41d4-a716-446655440002', '990e8400-e29b-41d4-a716-446655440001', '110e8400-e29b-41d4-a716-446655440001', 'PAY-002', 'LOAN_PAYMENT', 'BANK_TRANSFER', 'COMPLETED', 486.87, 'USD', '2024-04-01', '2024-04-01', '2024-04-01', 'system', 'system'),
    ('bb0e8400-e29b-41d4-a716-446655440003', '990e8400-e29b-41d4-a716-446655440002', '110e8400-e29b-41d4-a716-446655440002', 'PAY-003', 'LOAN_PAYMENT', 'CREDIT_CARD', 'COMPLETED', 569.23, 'USD', '2024-03-05', '2024-03-05', '2024-03-05', 'system', 'system'),
    ('bb0e8400-e29b-41d4-a716-446655440004', '990e8400-e29b-41d4-a716-446655440002', '110e8400-e29b-41d4-a716-446655440002', 'PAY-004', 'LOAN_PAYMENT', 'CREDIT_CARD', 'COMPLETED', 569.23, 'USD', '2024-04-05', '2024-04-05', '2024-04-05', 'system', 'system'),
    ('bb0e8400-e29b-41d4-a716-446655440005', '990e8400-e29b-41d4-a716-446655440003', '110e8400-e29b-41d4-a716-446655440004', 'PAY-005', 'LOAN_PAYMENT', 'BANK_TRANSFER', 'COMPLETED', 5185.32, 'USD', '2024-03-01', '2024-03-01', '2024-03-01', 'system', 'system');

-- Sample payment allocations
INSERT INTO banking_payment.payment_allocations (allocation_id, payment_id, loan_id, installment_id, allocation_type, allocated_amount, allocation_date, created_by) VALUES
    ('cc0e8400-e29b-41d4-a716-446655440001', 'bb0e8400-e29b-41d4-a716-446655440001', '990e8400-e29b-41d4-a716-446655440001', 'aa0e8400-e29b-41d4-a716-446655440001', 'PRINCIPAL', 345.62, '2024-03-01', 'system'),
    ('cc0e8400-e29b-41d4-a716-446655440002', 'bb0e8400-e29b-41d4-a716-446655440001', '990e8400-e29b-41d4-a716-446655440001', 'aa0e8400-e29b-41d4-a716-446655440001', 'INTEREST', 141.25, '2024-03-01', 'system'),
    ('cc0e8400-e29b-41d4-a716-446655440003', 'bb0e8400-e29b-41d4-a716-446655440002', '990e8400-e29b-41d4-a716-446655440001', 'aa0e8400-e29b-41d4-a716-446655440002', 'PRINCIPAL', 347.55, '2024-04-01', 'system'),
    ('cc0e8400-e29b-41d4-a716-446655440004', 'bb0e8400-e29b-41d4-a716-446655440002', '990e8400-e29b-41d4-a716-446655440001', 'aa0e8400-e29b-41d4-a716-446655440002', 'INTEREST', 139.32, '2024-04-01', 'system'),
    ('cc0e8400-e29b-41d4-a716-446655440005', 'bb0e8400-e29b-41d4-a716-446655440003', '990e8400-e29b-41d4-a716-446655440002', 'aa0e8400-e29b-41d4-a716-446655440004', 'PRINCIPAL', 407.85, '2024-03-05', 'system'),
    ('cc0e8400-e29b-41d4-a716-446655440006', 'bb0e8400-e29b-41d4-a716-446655440003', '990e8400-e29b-41d4-a716-446655440002', 'aa0e8400-e29b-41d4-a716-446655440004', 'INTEREST', 161.38, '2024-03-05', 'system');

-- ===== SAMPLE COMPLIANCE DATA =====

-- Sample compliance checks
INSERT INTO banking_compliance.compliance_checks (check_id, entity_type, entity_id, check_type, check_status, check_result, check_date, performed_by, created_by, updated_by) VALUES
    ('dd0e8400-e29b-41d4-a716-446655440001', 'CUSTOMER', '110e8400-e29b-41d4-a716-446655440001', 'KYC', 'PASSED', '{"identity_verified": true, "address_verified": true, "income_verified": true, "documents": ["passport", "utility_bill", "bank_statement"]}', '2024-01-01', 'compliance_officer', 'system', 'system'),
    ('dd0e8400-e29b-41d4-a716-446655440002', 'CUSTOMER', '110e8400-e29b-41d4-a716-446655440001', 'AML', 'PASSED', '{"pep_check": false, "sanctions_check": false, "adverse_media": false, "risk_score": 15}', '2024-01-01', 'compliance_officer', 'system', 'system'),
    ('dd0e8400-e29b-41d4-a716-446655440003', 'CUSTOMER', '110e8400-e29b-41d4-a716-446655440002', 'KYC', 'PASSED', '{"identity_verified": true, "address_verified": true, "income_verified": true, "documents": ["drivers_license", "bank_statement", "tax_return"]}', '2024-01-15', 'compliance_officer', 'system', 'system'),
    ('dd0e8400-e29b-41d4-a716-446655440004', 'CUSTOMER', '110e8400-e29b-41d4-a716-446655440002', 'AML', 'PASSED', '{"pep_check": false, "sanctions_check": false, "adverse_media": false, "risk_score": 25}', '2024-01-15', 'compliance_officer', 'system', 'system'),
    ('dd0e8400-e29b-41d4-a716-446655440005', 'LOAN', '990e8400-e29b-41d4-a716-446655440001', 'CREDIT_BUREAU', 'PASSED', '{"credit_score": 785, "bureau": "Experian", "report_date": "2024-01-15", "delinquencies": 0}', '2024-01-15', 'underwriter', 'system', 'system');

-- Sample regulatory reports
INSERT INTO banking_compliance.regulatory_reports (report_id, report_type, report_period_start, report_period_end, report_status, report_data, submission_date, regulatory_body, created_by, updated_by) VALUES
    ('ee0e8400-e29b-41d4-a716-446655440001', 'SUSPICIOUS_ACTIVITY', '2024-01-01', '2024-01-31', 'SUBMITTED', '{"total_reports": 0, "false_positives": 0, "confirmed_suspicious": 0}', '2024-02-01', 'FinCEN', 'system', 'system'),
    ('ee0e8400-e29b-41d4-a716-446655440002', 'LARGE_TRANSACTION', '2024-01-01', '2024-01-31', 'SUBMITTED', '{"total_transactions": 5, "total_amount": 1250000.00, "currency": "USD"}', '2024-02-01', 'FinCEN', 'system', 'system'),
    ('ee0e8400-e29b-41d4-a716-446655440003', 'CREDIT_RISK', '2024-01-01', '2024-01-31', 'SUBMITTED', '{"total_loans": 4, "total_exposure": 710000.00, "risk_weighted_assets": 568000.00}', '2024-02-01', 'FDIC', 'system', 'system');

-- ===== SAMPLE AUDIT DATA =====

-- Sample audit trail entries
INSERT INTO banking_audit.audit_trail (audit_id, entity_type, entity_id, action_type, old_values, new_values, changed_fields, user_id, user_role, session_id, ip_address, user_agent, correlation_id, risk_score, compliance_flags) VALUES
    ('ff0e8400-e29b-41d4-a716-446655440001', 'CUSTOMER', '110e8400-e29b-41d4-a716-446655440001', 'CREATE', NULL, '{"customer_number": "CUST-001", "customer_status": "ACTIVE", "customer_type": "INDIVIDUAL"}', ARRAY['customer_number', 'customer_status', 'customer_type'], 'system', 'ADMIN', 'session_001', '192.168.1.100', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36', 'ff0e8400-e29b-41d4-a716-446655440001', 10, ARRAY['PCI_COMPLIANT']),
    ('ff0e8400-e29b-41d4-a716-446655440002', 'LOAN', '990e8400-e29b-41d4-a716-446655440001', 'CREATE', NULL, '{"loan_number": "LOAN-001", "loan_status": "ACTIVE", "principal_amount": 25000.00}', ARRAY['loan_number', 'loan_status', 'principal_amount'], 'underwriter_001', 'UNDERWRITER', 'session_002', '192.168.1.101', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36', 'ff0e8400-e29b-41d4-a716-446655440002', 20, ARRAY['LOAN_APPROVED']),
    ('ff0e8400-e29b-41d4-a716-446655440003', 'PAYMENT', 'bb0e8400-e29b-41d4-a716-446655440001', 'CREATE', NULL, '{"payment_reference": "PAY-001", "payment_status": "COMPLETED", "amount": 486.87}', ARRAY['payment_reference', 'payment_status', 'amount'], 'customer_001', 'CUSTOMER', 'session_003', '192.168.1.102', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36', 'ff0e8400-e29b-41d4-a716-446655440003', 5, ARRAY['PAYMENT_PROCESSED']);

-- Sample security events
INSERT INTO banking_audit.security_events (event_id, event_type, severity, user_id, ip_address, user_agent, event_details, resolved_at, resolved_by, resolution_notes) VALUES
    ('1f0e8400-e29b-41d4-a716-446655440001', 'LOGIN', 'LOW', 'john.smith@email.com', '192.168.1.100', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36', '{"authentication_method": "OAuth2.1", "mfa_used": true, "device_trusted": true}', '2024-01-01 10:00:00', 'system', 'Successful login'),
    ('1f0e8400-e29b-41d4-a716-446655440002', 'FAILED_LOGIN', 'MEDIUM', 'unknown_user@email.com', '192.168.1.200', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36', '{"failure_reason": "invalid_credentials", "attempts": 3, "account_locked": false}', '2024-01-01 10:15:00', 'security_team', 'Monitored - no further action required'),
    ('1f0e8400-e29b-41d4-a716-446655440003', 'SUSPICIOUS_ACTIVITY', 'HIGH', 'sarah.johnson@email.com', '192.168.1.150', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36', '{"activity_type": "unusual_transaction_pattern", "transaction_count": 10, "time_window": "1_hour"}', '2024-01-02 15:30:00', 'fraud_team', 'Investigated - legitimate activity confirmed');

-- ===== SAMPLE ML DATA =====

-- Sample ML predictions
INSERT INTO banking_ml.ml_predictions (prediction_id, model_id, entity_type, entity_id, prediction_type, prediction_value, confidence_score, risk_score, prediction_data, feedback_received, feedback_value, feedback_timestamp) VALUES
    ('2f0e8400-e29b-41d4-a716-446655440001', (SELECT model_id FROM banking_ml.ml_models WHERE model_name = 'Fraud Detection V1'), 'PAYMENT', 'bb0e8400-e29b-41d4-a716-446655440001', 'FRAUD_PROBABILITY', 0.0234, 0.9876, 2, '{"features": {"amount": 486.87, "time_of_day": "morning", "location": "home", "device": "trusted"}}', TRUE, 0.0000, '2024-03-01 12:00:00'),
    ('2f0e8400-e29b-41d4-a716-446655440002', (SELECT model_id FROM banking_ml.ml_models WHERE model_name = 'Credit Scoring V1'), 'CUSTOMER', '110e8400-e29b-41d4-a716-446655440001', 'CREDIT_SCORE', 785.0000, 0.9200, 10, '{"features": {"income": 120000, "employment_years": 8, "debt_to_income": 0.15, "payment_history": "excellent"}}', FALSE, NULL, NULL),
    ('2f0e8400-e29b-41d4-a716-446655440003', (SELECT model_id FROM banking_ml.ml_models WHERE model_name = 'Risk Assessment V1'), 'LOAN', '990e8400-e29b-41d4-a716-446655440001', 'DEFAULT_PROBABILITY', 0.0345, 0.8900, 15, '{"features": {"loan_amount": 25000, "loan_term": 60, "ltv_ratio": 0.75, "customer_tier": "premium"}}', TRUE, 0.0000, '2024-02-01 16:00:00');

-- Sample anomaly detection results
INSERT INTO banking_ml.anomaly_detection_results (anomaly_id, detection_type, entity_type, entity_id, anomaly_score, severity, anomaly_details, investigation_status, resolution_notes, resolved_at, resolved_by) VALUES
    ('3f0e8400-e29b-41d4-a716-446655440001', 'TRANSACTION_PATTERN', 'CUSTOMER', '110e8400-e29b-41d4-a716-446655440002', 0.7500, 'MEDIUM', '{"pattern_type": "unusual_payment_timing", "deviation": 2.5, "baseline": "monthly_payment", "observed": "bi_weekly_payment"}', 'RESOLVED', 'Customer confirmed change in payment frequency due to salary adjustment', '2024-02-15 10:00:00', 'analyst_001'),
    ('3f0e8400-e29b-41d4-a716-446655440002', 'SYSTEM_PERFORMANCE', 'SYSTEM', '990e8400-e29b-41d4-a716-446655440003', 0.8200, 'HIGH', '{"metric": "response_time", "threshold": 500, "observed": 850, "service": "loan_processing"}', 'RESOLVED', 'Database query optimization applied', '2024-02-10 14:30:00', 'devops_team'),
    ('3f0e8400-e29b-41d4-a716-446655440003', 'FRAUD', 'PAYMENT', 'bb0e8400-e29b-41d4-a716-446655440005', 0.9100, 'CRITICAL', '{"indicators": ["unusual_amount", "off_hours", "new_device"], "confidence": 0.91, "action": "payment_blocked"}', 'INVESTIGATING', 'Escalated to fraud investigation team', NULL, NULL);

-- ===== SAMPLE FEDERATION DATA =====

-- Sample cross-region metrics (updated to current timestamp)
INSERT INTO banking_federation.cross_region_metrics (metric_id, region_name, metric_type, metric_value, metric_unit, metadata) VALUES
    ('4f0e8400-e29b-41d4-a716-446655440001', 'us-east-1', 'TRANSACTION_VOLUME', 1250.0000, 'count', '{"period": "1_hour", "threshold": 2000, "status": "normal"}'),
    ('4f0e8400-e29b-41d4-a716-446655440002', 'eu-west-1', 'TRANSACTION_VOLUME', 890.0000, 'count', '{"period": "1_hour", "threshold": 1500, "status": "normal"}'),
    ('4f0e8400-e29b-41d4-a716-446655440003', 'ap-southeast-1', 'TRANSACTION_VOLUME', 1150.0000, 'count', '{"period": "1_hour", "threshold": 1800, "status": "normal"}'),
    ('4f0e8400-e29b-41d4-a716-446655440004', 'us-east-1', 'RESPONSE_TIME', 125.5000, 'milliseconds', '{"threshold": 200, "percentile": "p95", "status": "normal"}'),
    ('4f0e8400-e29b-41d4-a716-446655440005', 'eu-west-1', 'RESPONSE_TIME', 142.8000, 'milliseconds', '{"threshold": 200, "percentile": "p95", "status": "normal"}'),
    ('4f0e8400-e29b-41d4-a716-446655440006', 'ap-southeast-1', 'RESPONSE_TIME', 138.2000, 'milliseconds', '{"threshold": 200, "percentile": "p95", "status": "normal"}');

-- Sample alert correlations
INSERT INTO banking_federation.alert_correlations (correlation_id, alert_ids, correlation_score, correlation_type, potential_cause, recommended_actions, correlation_data) VALUES
    ('5f0e8400-e29b-41d4-a716-446655440001', ARRAY['ALERT_001', 'ALERT_002', 'ALERT_003'], 0.8500, 'PERFORMANCE_DEGRADATION', 'Database connection pool exhaustion across regions', ARRAY['Increase connection pool size', 'Review database query performance', 'Monitor connection leaks'], '{"affected_regions": ["us-east-1", "eu-west-1"], "pattern": "simultaneous_degradation", "confidence": 0.85}'),
    ('5f0e8400-e29b-41d4-a716-446655440002', ARRAY['ALERT_004', 'ALERT_005'], 0.7200, 'SECURITY_INCIDENT', 'Coordinated fraud attack pattern detected', ARRAY['Enhance fraud detection rules', 'Implement additional verification', 'Review transaction patterns'], '{"attack_vector": "credential_stuffing", "regions": ["us-east-1", "ap-southeast-1"], "timeline": "2024-01-15T10:00:00Z"}');

-- Sample disaster recovery status
INSERT INTO banking_federation.disaster_recovery_status (status_id, regions, overall_status, replication_lag_seconds, failover_ready, last_backup_timestamp, rto_minutes, rpo_minutes, status_details) VALUES
    ('6f0e8400-e29b-41d4-a716-446655440001', ARRAY['us-east-1', 'eu-west-1', 'ap-southeast-1'], 'HEALTHY', 3, TRUE, '2024-01-15 23:59:59', 15, 5, '{"primary_region": "us-east-1", "standby_regions": ["eu-west-1", "ap-southeast-1"], "last_failover_test": "2024-01-10T14:00:00Z", "test_result": "PASSED"}'),
    ('6f0e8400-e29b-41d4-a716-446655440002', ARRAY['us-east-1', 'eu-west-1'], 'WARNING', 8, TRUE, '2024-01-15 23:45:00', 20, 10, '{"primary_region": "us-east-1", "standby_regions": ["eu-west-1"], "warning_reason": "replication_lag_threshold", "threshold": 5}');

-- Update sequence values to avoid conflicts
SELECT setval('banking_core.parties_version_seq', 100, false);
SELECT setval('banking_customer.customers_version_seq', 100, false);
SELECT setval('banking_loan.loans_version_seq', 100, false);
SELECT setval('banking_payment.payments_version_seq', 100, false);

-- Update statistics
ANALYZE;

-- Log successful sample data loading
INSERT INTO banking_audit.audit_trail (entity_type, entity_id, action_type, new_values, user_id, user_role, ip_address)
VALUES ('SYSTEM', uuid_generate_v4(), 'CREATE', '{"event": "sample_data_loaded", "timestamp": "' || CURRENT_TIMESTAMP || '", "records_created": 100}', 'system', 'ADMIN', '127.0.0.1');

-- Success message
DO $$
DECLARE
    customer_count INTEGER;
    loan_count INTEGER;
    payment_count INTEGER;
    compliance_count INTEGER;
    audit_count INTEGER;
    ml_count INTEGER;
    federation_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO customer_count FROM banking_customer.customers;
    SELECT COUNT(*) INTO loan_count FROM banking_loan.loans;
    SELECT COUNT(*) INTO payment_count FROM banking_payment.payments;
    SELECT COUNT(*) INTO compliance_count FROM banking_compliance.compliance_checks;
    SELECT COUNT(*) INTO audit_count FROM banking_audit.audit_trail;
    SELECT COUNT(*) INTO ml_count FROM banking_ml.ml_predictions;
    SELECT COUNT(*) INTO federation_count FROM banking_federation.cross_region_metrics;
    
    RAISE NOTICE 'Enterprise Loan Management System Sample Data Successfully Loaded';
    RAISE NOTICE 'Created % customers, % loans, % payments', customer_count, loan_count, payment_count;
    RAISE NOTICE 'Created % compliance checks, % audit entries', compliance_count, audit_count;
    RAISE NOTICE 'Created % ML predictions, % federation metrics', ml_count, federation_count;
    RAISE NOTICE 'Database ready for comprehensive testing and demonstration';
END $$;