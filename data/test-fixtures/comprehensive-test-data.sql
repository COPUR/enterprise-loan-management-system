-- ===================================================================
-- Enterprise Loan Management System - Comprehensive Test Data
-- ===================================================================
-- Complete test dataset for development, testing, and demonstrations
-- Includes realistic banking scenarios and edge cases
-- ===================================================================

-- Disable foreign key checks for easier data loading
SET foreign_key_checks = 0;

-- ===================================================================
-- CUSTOMERS DATA
-- ===================================================================

-- Individual Customers (Various Credit Profiles)
INSERT INTO customers (customer_id, first_name, last_name, email, phone, date_of_birth, customer_type, status, ssn, created_at, updated_at) VALUES
-- Excellent Credit Customers
('CUST-001', 'Alice', 'Johnson', 'alice.johnson@email.com', '+1-555-0101', '1985-03-15', 'INDIVIDUAL', 'ACTIVE', 'XXX-XX-1001', NOW(), NOW()),
('CUST-002', 'Robert', 'Chen', 'robert.chen@email.com', '+1-555-0102', '1982-07-22', 'INDIVIDUAL', 'ACTIVE', 'XXX-XX-1002', NOW(), NOW()),
('CUST-003', 'Emily', 'Rodriguez', 'emily.rodriguez@email.com', '+1-555-0103', '1990-11-08', 'INDIVIDUAL', 'ACTIVE', 'XXX-XX-1003', NOW(), NOW()),

-- Good Credit Customers
('CUST-004', 'Michael', 'Davis', 'michael.davis@email.com', '+1-555-0104', '1988-04-30', 'INDIVIDUAL', 'ACTIVE', 'XXX-XX-1004', NOW(), NOW()),
('CUST-005', 'Sarah', 'Wilson', 'sarah.wilson@email.com', '+1-555-0105', '1992-09-12', 'INDIVIDUAL', 'ACTIVE', 'XXX-XX-1005', NOW(), NOW()),
('CUST-006', 'James', 'Brown', 'james.brown@email.com', '+1-555-0106', '1987-01-25', 'INDIVIDUAL', 'ACTIVE', 'XXX-XX-1006', NOW(), NOW()),

-- Fair Credit Customers
('CUST-007', 'Jennifer', 'Taylor', 'jennifer.taylor@email.com', '+1-555-0107', '1991-05-18', 'INDIVIDUAL', 'ACTIVE', 'XXX-XX-1007', NOW(), NOW()),
('CUST-008', 'David', 'Anderson', 'david.anderson@email.com', '+1-555-0108', '1984-12-03', 'INDIVIDUAL', 'ACTIVE', 'XXX-XX-1008', NOW(), NOW()),

-- Poor Credit Customers
('CUST-009', 'Lisa', 'Martinez', 'lisa.martinez@email.com', '+1-555-0109', '1989-08-14', 'INDIVIDUAL', 'ACTIVE', 'XXX-XX-1009', NOW(), NOW()),
('CUST-010', 'Kevin', 'Thompson', 'kevin.thompson@email.com', '+1-555-0110', '1986-02-28', 'INDIVIDUAL', 'ACTIVE', 'XXX-XX-1010', NOW(), NOW()),

-- First Time Borrowers
('CUST-011', 'Amanda', 'White', 'amanda.white@email.com', '+1-555-0111', '1995-06-10', 'INDIVIDUAL', 'ACTIVE', 'XXX-XX-1011', NOW(), NOW()),
('CUST-012', 'Ryan', 'Garcia', 'ryan.garcia@email.com', '+1-555-0112', '1993-09-22', 'INDIVIDUAL', 'ACTIVE', 'XXX-XX-1012', NOW(), NOW()),

-- Senior Customers
('CUST-013', 'Margaret', 'Jackson', 'margaret.jackson@email.com', '+1-555-0113', '1955-04-17', 'INDIVIDUAL', 'ACTIVE', 'XXX-XX-1013', NOW(), NOW()),
('CUST-014', 'William', 'Lee', 'william.lee@email.com', '+1-555-0114', '1952-11-29', 'INDIVIDUAL', 'ACTIVE', 'XXX-XX-1014', NOW(), NOW()),

-- High Net Worth Individuals
('CUST-015', 'Victoria', 'Sterling', 'victoria.sterling@email.com', '+1-555-0115', '1975-03-08', 'INDIVIDUAL', 'ACTIVE', 'XXX-XX-1015', NOW(), NOW()),

-- Corporate Customers
('CORP-001', 'Tech Innovations Inc', '', 'finance@techinnovations.com', '+1-555-0201', '2015-01-15', 'CORPORATE', 'ACTIVE', 'XX-XXXXXXX1', NOW(), NOW()),
('CORP-002', 'Green Energy Solutions', '', 'accounting@greenenergy.com', '+1-555-0202', '2018-03-22', 'CORPORATE', 'ACTIVE', 'XX-XXXXXXX2', NOW(), NOW()),
('CORP-003', 'Manufacturing Plus LLC', '', 'cfo@manufacturing.com', '+1-555-0203', '2012-07-10', 'CORPORATE', 'ACTIVE', 'XX-XXXXXXX3', NOW(), NOW()),
('CORP-004', 'Healthcare Services Corp', '', 'finance@healthcare.com', '+1-555-0204', '2010-05-18', 'CORPORATE', 'ACTIVE', 'XX-XXXXXXX4', NOW(), NOW()),
('CORP-005', 'Real Estate Development', '', 'admin@realestate.com', '+1-555-0205', '2008-12-01', 'CORPORATE', 'ACTIVE', 'XX-XXXXXXX5', NOW(), NOW()),

-- Small Business Customers
('SMB-001', 'Corner Bakery LLC', '', 'owner@cornerbakery.com', '+1-555-0301', '2020-06-01', 'SMALL_BUSINESS', 'ACTIVE', 'XX-XXXXXXX6', NOW(), NOW()),
('SMB-002', 'Auto Repair Shop Inc', '', 'manager@autorepair.com', '+1-555-0302', '2019-02-15', 'SMALL_BUSINESS', 'ACTIVE', 'XX-XXXXXXX7', NOW(), NOW()),
('SMB-003', 'Local Restaurant Group', '', 'finance@restaurant.com', '+1-555-0303', '2017-09-30', 'SMALL_BUSINESS', 'ACTIVE', 'XX-XXXXXXX8', NOW(), NOW());

-- ===================================================================
-- CUSTOMER ADDRESSES
-- ===================================================================

INSERT INTO customer_addresses (customer_id, street, city, state, zip_code, country, type, is_primary) VALUES
-- Individual customer addresses
(1, '123 Maple Street', 'San Francisco', 'CA', '94102', 'USA', 'HOME', true),
(1, '456 Corporate Plaza', 'San Francisco', 'CA', '94105', 'USA', 'WORK', false),
(2, '789 Oak Avenue', 'Los Angeles', 'CA', '90210', 'USA', 'HOME', true),
(3, '321 Pine Road', 'Chicago', 'IL', '60601', 'USA', 'HOME', true),
(4, '654 Elm Street', 'Houston', 'TX', '77001', 'USA', 'HOME', true),
(5, '987 Cedar Drive', 'Phoenix', 'AZ', '85001', 'USA', 'HOME', true),
(6, '147 Birch Lane', 'Philadelphia', 'PA', '19101', 'USA', 'HOME', true),
(7, '258 Willow Way', 'San Antonio', 'TX', '78201', 'USA', 'HOME', true),
(8, '369 Spruce Circle', 'San Diego', 'CA', '92101', 'USA', 'HOME', true),
(9, '741 Ash Boulevard', 'Dallas', 'TX', '75201', 'USA', 'HOME', true),
(10, '852 Poplar Place', 'San Jose', 'CA', '95101', 'USA', 'HOME', true),
(11, '963 Cherry Court', 'Austin', 'TX', '73301', 'USA', 'HOME', true),
(12, '159 Walnut Street', 'Jacksonville', 'FL', '32099', 'USA', 'HOME', true),
(13, '357 Hickory Drive', 'Fort Worth', 'TX', '76101', 'USA', 'HOME', true),
(14, '486 Magnolia Avenue', 'Columbus', 'OH', '43085', 'USA', 'HOME', true),
(15, '1500 Executive Drive', 'Beverly Hills', 'CA', '90210', 'USA', 'HOME', true),

-- Corporate addresses
(16, '1000 Innovation Blvd', 'Palo Alto', 'CA', '94301', 'USA', 'BUSINESS', true),
(17, '2000 Green Energy Way', 'Austin', 'TX', '78701', 'USA', 'BUSINESS', true),
(18, '3000 Manufacturing Drive', 'Detroit', 'MI', '48201', 'USA', 'BUSINESS', true),
(19, '4000 Healthcare Plaza', 'Boston', 'MA', '02101', 'USA', 'BUSINESS', true),
(20, '5000 Real Estate Center', 'Miami', 'FL', '33101', 'USA', 'BUSINESS', true),

-- Small business addresses  
(21, '100 Main Street', 'Springfield', 'IL', '62701', 'USA', 'BUSINESS', true),
(22, '200 Service Road', 'Portland', 'OR', '97201', 'USA', 'BUSINESS', true),
(23, '300 Restaurant Row', 'Nashville', 'TN', '37201', 'USA', 'BUSINESS', true);

-- ===================================================================
-- CREDIT SCORES
-- ===================================================================

INSERT INTO credit_scores (customer_id, score, reporting_agency, last_updated) VALUES
-- Excellent Credit (750+)
(1, 820, 'EXPERIAN', NOW() - INTERVAL 30 DAY),
(2, 785, 'EQUIFAX', NOW() - INTERVAL 45 DAY),
(3, 760, 'TRANSUNION', NOW() - INTERVAL 15 DAY),

-- Good Credit (670-749)
(4, 720, 'EXPERIAN', NOW() - INTERVAL 20 DAY),
(5, 695, 'EQUIFAX', NOW() - INTERVAL 35 DAY),
(6, 710, 'TRANSUNION', NOW() - INTERVAL 25 DAY),

-- Fair Credit (580-669)
(7, 640, 'EXPERIAN', NOW() - INTERVAL 40 DAY),
(8, 615, 'EQUIFAX', NOW() - INTERVAL 50 DAY),

-- Poor Credit (Below 580)
(9, 550, 'TRANSUNION', NOW() - INTERVAL 60 DAY),
(10, 520, 'EXPERIAN', NOW() - INTERVAL 70 DAY),

-- First Time Borrowers (No established credit)
(11, 0, 'NO_CREDIT', NOW() - INTERVAL 1 DAY),
(12, 0, 'NO_CREDIT', NOW() - INTERVAL 1 DAY),

-- Senior Customers
(13, 750, 'EXPERIAN', NOW() - INTERVAL 90 DAY),
(14, 780, 'EQUIFAX', NOW() - INTERVAL 120 DAY),

-- High Net Worth
(15, 850, 'EXPERIAN', NOW() - INTERVAL 10 DAY),

-- Corporate Credit Scores
(16, 780, 'D&B', NOW() - INTERVAL 30 DAY),
(17, 720, 'D&B', NOW() - INTERVAL 45 DAY),
(18, 690, 'D&B', NOW() - INTERVAL 60 DAY),
(19, 810, 'D&B', NOW() - INTERVAL 15 DAY),
(20, 750, 'D&B', NOW() - INTERVAL 90 DAY),

-- Small Business Credit Scores
(21, 650, 'D&B', NOW() - INTERVAL 180 DAY),
(22, 680, 'D&B', NOW() - INTERVAL 150 DAY),
(23, 620, 'D&B', NOW() - INTERVAL 200 DAY);

-- ===================================================================
-- FINANCIAL PROFILES
-- ===================================================================

INSERT INTO customer_financial_profiles (customer_id, annual_income, employment_status, employment_years, debt_to_income_ratio, credit_utilization, preferred_loan_term, max_loan_amount, assets_value, liabilities_value, created_at, updated_at) VALUES
-- Individual customers with varying financial profiles
(1, 125000.00, 'EMPLOYED', 8, 0.25, 0.15, 60, 50000.00, 250000.00, 75000.00, NOW(), NOW()),
(2, 95000.00, 'EMPLOYED', 12, 0.30, 0.20, 72, 40000.00, 180000.00, 60000.00, NOW(), NOW()),
(3, 78000.00, 'EMPLOYED', 5, 0.35, 0.25, 60, 35000.00, 150000.00, 45000.00, NOW(), NOW()),
(4, 67000.00, 'EMPLOYED', 6, 0.40, 0.30, 48, 25000.00, 120000.00, 55000.00, NOW(), NOW()),
(5, 85000.00, 'EMPLOYED', 4, 0.28, 0.18, 60, 45000.00, 160000.00, 48000.00, NOW(), NOW()),
(6, 72000.00, 'EMPLOYED', 10, 0.32, 0.22, 72, 30000.00, 140000.00, 52000.00, NOW(), NOW()),
(7, 58000.00, 'EMPLOYED', 3, 0.45, 0.35, 48, 20000.00, 95000.00, 65000.00, NOW(), NOW()),
(8, 61000.00, 'EMPLOYED', 7, 0.42, 0.32, 60, 22000.00, 110000.00, 58000.00, NOW(), NOW()),
(9, 45000.00, 'EMPLOYED', 2, 0.55, 0.45, 36, 15000.00, 75000.00, 70000.00, NOW(), NOW()),
(10, 52000.00, 'PART_TIME', 1, 0.50, 0.40, 48, 18000.00, 80000.00, 68000.00, NOW(), NOW()),
(11, 65000.00, 'EMPLOYED', 1, 0.20, 0.05, 60, 25000.00, 85000.00, 25000.00, NOW(), NOW()),
(12, 70000.00, 'EMPLOYED', 2, 0.22, 0.08, 48, 30000.00, 95000.00, 28000.00, NOW(), NOW()),
(13, 45000.00, 'RETIRED', 0, 0.15, 0.10, 36, 20000.00, 350000.00, 35000.00, NOW(), NOW()),
(14, 38000.00, 'RETIRED', 0, 0.18, 0.12, 24, 15000.00, 420000.00, 40000.00, NOW(), NOW()),
(15, 500000.00, 'SELF_EMPLOYED', 15, 0.20, 0.05, 120, 200000.00, 2500000.00, 180000.00, NOW(), NOW()),

-- Corporate financial profiles
(16, 5500000.00, 'BUSINESS', 8, 0.35, 0.25, 120, 2000000.00, 8500000.00, 3200000.00, NOW(), NOW()),
(17, 3200000.00, 'BUSINESS', 5, 0.30, 0.20, 84, 1500000.00, 5800000.00, 2100000.00, NOW(), NOW()),
(18, 8900000.00, 'BUSINESS', 11, 0.40, 0.30, 180, 5000000.00, 15200000.00, 6800000.00, NOW(), NOW()),
(19, 12500000.00, 'BUSINESS', 13, 0.25, 0.15, 240, 8000000.00, 25600000.00, 4800000.00, NOW(), NOW()),
(20, 6800000.00, 'BUSINESS', 15, 0.45, 0.35, 120, 3500000.00, 18900000.00, 8200000.00, NOW(), NOW()),

-- Small business financial profiles
(21, 285000.00, 'BUSINESS', 3, 0.50, 0.40, 60, 150000.00, 420000.00, 180000.00, NOW(), NOW()),
(22, 350000.00, 'BUSINESS', 4, 0.45, 0.35, 72, 200000.00, 580000.00, 220000.00, NOW(), NOW()),
(23, 480000.00, 'BUSINESS', 6, 0.40, 0.30, 84, 300000.00, 750000.00, 280000.00, NOW(), NOW());

-- ===================================================================
-- LOANS DATA
-- ===================================================================

INSERT INTO loans (loan_id, customer_id, loan_type, principal_amount, currency, interest_rate, term_months, monthly_payment, status, application_date, approved_date, disbursed_date, maturity_date, current_balance, created_at, updated_at) VALUES
-- Active Personal Loans
('LOAN-001', 'CUST-001', 'PERSONAL', 25000.00, 'USD', 0.0575, 60, 478.66, 'ACTIVE', '2024-01-15', '2024-01-18', '2024-01-20', '2029-01-20', 23500.00, NOW(), NOW()),
('LOAN-002', 'CUST-002', 'PERSONAL', 18000.00, 'USD', 0.0625, 48, 425.32, 'ACTIVE', '2024-02-01', '2024-02-03', '2024-02-05', '2028-02-05', 16800.00, NOW(), NOW()),
('LOAN-003', 'CUST-003', 'PERSONAL', 22000.00, 'USD', 0.0595, 60, 423.45, 'ACTIVE', '2024-01-20', '2024-01-25', '2024-01-28', '2029-01-28', 20900.00, NOW(), NOW()),

-- Auto Loans
('LOAN-004', 'CUST-004', 'AUTO', 35000.00, 'USD', 0.0425, 72, 548.32, 'ACTIVE', '2023-12-10', '2023-12-15', '2023-12-18', '2029-12-18', 32500.00, NOW(), NOW()),
('LOAN-005', 'CUST-005', 'AUTO', 28000.00, 'USD', 0.0445, 60, 524.88, 'ACTIVE', '2024-01-05', '2024-01-08', '2024-01-10', '2029-01-10', 26200.00, NOW(), NOW()),
('LOAN-006', 'CUST-006', 'AUTO', 42000.00, 'USD', 0.0395, 84, 543.21, 'ACTIVE', '2023-11-20', '2023-11-25', '2023-11-28', '2030-11-28', 39800.00, NOW(), NOW()),

-- Mortgage Loans
('LOAN-007', 'CUST-002', 'MORTGAGE', 450000.00, 'USD', 0.0375, 360, 2072.24, 'ACTIVE', '2023-06-01', '2023-06-25', '2023-07-01', '2053-07-01', 445000.00, NOW(), NOW()),
('LOAN-008', 'CUST-003', 'MORTGAGE', 320000.00, 'USD', 0.0395, 360, 1516.85, 'ACTIVE', '2023-08-15', '2023-09-05', '2023-09-15', '2053-09-15', 318000.00, NOW(), NOW()),
('LOAN-009', 'CUST-015', 'MORTGAGE', 850000.00, 'USD', 0.0345, 360, 3772.81, 'ACTIVE', '2023-05-10', '2023-05-30', '2023-06-05', '2053-06-05', 847000.00, NOW(), NOW()),

-- Business Loans
('LOAN-010', 'CORP-001', 'BUSINESS', 500000.00, 'USD', 0.0485, 120, 5247.89, 'ACTIVE', '2023-10-01', '2023-10-15', '2023-10-20', '2033-10-20', 485000.00, NOW(), NOW()),
('LOAN-011', 'CORP-002', 'BUSINESS', 750000.00, 'USD', 0.0465, 180, 5968.45, 'ACTIVE', '2023-09-01', '2023-09-20', '2023-09-25', '2038-09-25', 735000.00, NOW(), NOW()),
('LOAN-012', 'CORP-003', 'BUSINESS', 1200000.00, 'USD', 0.0445, 240, 7854.32, 'ACTIVE', '2023-07-15', '2023-08-10', '2023-08-15', '2043-08-15', 1180000.00, NOW(), NOW()),

-- Small Business Loans
('LOAN-013', 'SMB-001', 'SMALL_BUSINESS', 85000.00, 'USD', 0.0565, 84, 1198.45, 'ACTIVE', '2023-12-01', '2023-12-15', '2023-12-20', '2030-12-20', 82000.00, NOW(), NOW()),
('LOAN-014', 'SMB-002', 'SMALL_BUSINESS', 120000.00, 'USD', 0.0545, 96, 1465.78, 'ACTIVE', '2023-11-10', '2023-11-25', '2023-11-30', '2031-11-30', 117000.00, NOW(), NOW()),
('LOAN-015', 'SMB-003', 'SMALL_BUSINESS', 95000.00, 'USD', 0.0575, 72, 1425.33, 'ACTIVE', '2024-01-15', '2024-01-30', '2024-02-02', '2030-02-02', 92500.00, NOW(), NOW()),

-- Pending Loan Applications
('LOAN-016', 'CUST-007', 'PERSONAL', 15000.00, 'USD', 0.0695, 48, 355.48, 'PENDING', '2024-03-01', NULL, NULL, NULL, 0.00, NOW(), NOW()),
('LOAN-017', 'CUST-008', 'AUTO', 32000.00, 'USD', 0.0475, 72, 512.34, 'PENDING', '2024-03-05', NULL, NULL, NULL, 0.00, NOW(), NOW()),
('LOAN-018', 'CUST-009', 'PERSONAL', 8000.00, 'USD', 0.0895, 36, 254.78, 'UNDER_REVIEW', '2024-03-10', NULL, NULL, NULL, 0.00, NOW(), NOW()),

-- Rejected Applications
('LOAN-019', 'CUST-010', 'PERSONAL', 20000.00, 'USD', 0.0000, 0, 0.00, 'REJECTED', '2024-02-15', '2024-02-20', NULL, NULL, 0.00, NOW(), NOW()),

-- Paid Off Loans
('LOAN-020', 'CUST-001', 'AUTO', 28000.00, 'USD', 0.0425, 60, 523.45, 'PAID_OFF', '2019-03-01', '2019-03-05', '2019-03-08', '2024-03-08', 0.00, NOW(), NOW()),
('LOAN-021', 'CUST-013', 'PERSONAL', 12000.00, 'USD', 0.0625, 36, 367.89, 'PAID_OFF', '2021-01-15', '2021-01-20', '2021-01-25', '2024-01-25', 0.00, NOW(), NOW());

-- ===================================================================
-- LOAN INSTALLMENTS
-- ===================================================================

INSERT INTO loan_installments (loan_id, installment_number, due_date, amount, currency, status, paid_date, paid_amount, late_fee, created_at, updated_at) VALUES
-- LOAN-001 installments (3 paid, rest pending)
('LOAN-001', 1, '2024-02-20', 478.66, 'USD', 'PAID', '2024-02-20', 478.66, 0.00, NOW(), NOW()),
('LOAN-001', 2, '2024-03-20', 478.66, 'USD', 'PAID', '2024-03-20', 478.66, 0.00, NOW(), NOW()),
('LOAN-001', 3, '2024-04-20', 478.66, 'USD', 'PAID', '2024-04-18', 478.66, 0.00, NOW(), NOW()),
('LOAN-001', 4, '2024-05-20', 478.66, 'USD', 'PENDING', NULL, NULL, 0.00, NOW(), NOW()),
('LOAN-001', 5, '2024-06-20', 478.66, 'USD', 'PENDING', NULL, NULL, 0.00, NOW(), NOW()),

-- LOAN-002 installments (2 paid, one late, rest pending)
('LOAN-002', 1, '2024-03-05', 425.32, 'USD', 'PAID', '2024-03-05', 425.32, 0.00, NOW(), NOW()),
('LOAN-002', 2, '2024-04-05', 425.32, 'USD', 'PAID', '2024-04-08', 425.32, 25.00, NOW(), NOW()),
('LOAN-002', 3, '2024-05-05', 425.32, 'USD', 'PENDING', NULL, NULL, 0.00, NOW(), NOW()),

-- LOAN-004 installments (Auto loan with longer history)
('LOAN-004', 1, '2024-01-18', 548.32, 'USD', 'PAID', '2024-01-18', 548.32, 0.00, NOW(), NOW()),
('LOAN-004', 2, '2024-02-18', 548.32, 'USD', 'PAID', '2024-02-18', 548.32, 0.00, NOW(), NOW()),
('LOAN-004', 3, '2024-03-18', 548.32, 'USD', 'PAID', '2024-03-15', 548.32, 0.00, NOW(), NOW()),
('LOAN-004', 4, '2024-04-18', 548.32, 'USD', 'PENDING', NULL, NULL, 0.00, NOW(), NOW()),

-- LOAN-007 installments (Mortgage with payment history)
('LOAN-007', 1, '2023-08-01', 2072.24, 'USD', 'PAID', '2023-08-01', 2072.24, 0.00, NOW(), NOW()),
('LOAN-007', 2, '2023-09-01', 2072.24, 'USD', 'PAID', '2023-09-01', 2072.24, 0.00, NOW(), NOW()),
('LOAN-007', 3, '2023-10-01', 2072.24, 'USD', 'PAID', '2023-10-01', 2072.24, 0.00, NOW(), NOW()),
('LOAN-007', 4, '2023-11-01', 2072.24, 'USD', 'PAID', '2023-11-01', 2072.24, 0.00, NOW(), NOW()),
('LOAN-007', 5, '2023-12-01', 2072.24, 'USD', 'PAID', '2023-12-01', 2072.24, 0.00, NOW(), NOW()),
('LOAN-007', 6, '2024-01-01', 2072.24, 'USD', 'PAID', '2024-01-01', 2072.24, 0.00, NOW(), NOW()),
('LOAN-007', 7, '2024-02-01', 2072.24, 'USD', 'PAID', '2024-02-01', 2072.24, 0.00, NOW(), NOW()),
('LOAN-007', 8, '2024-03-01', 2072.24, 'USD', 'PAID', '2024-03-01', 2072.24, 0.00, NOW(), NOW()),
('LOAN-007', 9, '2024-04-01', 2072.24, 'USD', 'PAID', '2024-04-01', 2072.24, 0.00, NOW(), NOW()),
('LOAN-007', 10, '2024-05-01', 2072.24, 'USD', 'PENDING', NULL, NULL, 0.00, NOW(), NOW()),

-- Business loan installments
('LOAN-010', 1, '2023-11-20', 5247.89, 'USD', 'PAID', '2023-11-20', 5247.89, 0.00, NOW(), NOW()),
('LOAN-010', 2, '2023-12-20', 5247.89, 'USD', 'PAID', '2023-12-20', 5247.89, 0.00, NOW(), NOW()),
('LOAN-010', 3, '2024-01-20', 5247.89, 'USD', 'PAID', '2024-01-20', 5247.89, 0.00, NOW(), NOW()),
('LOAN-010', 4, '2024-02-20', 5247.89, 'USD', 'PAID', '2024-02-20', 5247.89, 0.00, NOW(), NOW()),
('LOAN-010', 5, '2024-03-20', 5247.89, 'USD', 'PAID', '2024-03-18', 5247.89, 0.00, NOW(), NOW()),
('LOAN-010', 6, '2024-04-20', 5247.89, 'USD', 'PENDING', NULL, NULL, 0.00, NOW(), NOW());

-- ===================================================================
-- PAYMENTS DATA
-- ===================================================================

INSERT INTO payments (payment_id, loan_id, customer_id, payment_amount, payment_currency, payment_date, status, payment_method, payment_reference, description, processed_by, created_at, updated_at) VALUES
-- Regular monthly payments
('PAY-001', 'LOAN-001', 'CUST-001', 478.66, 'USD', '2024-02-20', 'PROCESSED', 'BANK_TRANSFER', 'TXN-20240220-001', 'Monthly payment February', 'SYSTEM', NOW(), NOW()),
('PAY-002', 'LOAN-001', 'CUST-001', 478.66, 'USD', '2024-03-20', 'PROCESSED', 'BANK_TRANSFER', 'TXN-20240320-001', 'Monthly payment March', 'SYSTEM', NOW(), NOW()),
('PAY-003', 'LOAN-001', 'CUST-001', 478.66, 'USD', '2024-04-18', 'PROCESSED', 'ACH', 'TXN-20240418-001', 'Monthly payment April (early)', 'SYSTEM', NOW(), NOW()),

('PAY-004', 'LOAN-002', 'CUST-002', 425.32, 'USD', '2024-03-05', 'PROCESSED', 'ACH', 'TXN-20240305-001', 'Monthly payment March', 'SYSTEM', NOW(), NOW()),
('PAY-005', 'LOAN-002', 'CUST-002', 450.32, 'USD', '2024-04-08', 'PROCESSED', 'ACH', 'TXN-20240408-001', 'Late payment April + late fee', 'SYSTEM', NOW(), NOW()),

-- Auto loan payments
('PAY-006', 'LOAN-004', 'CUST-004', 548.32, 'USD', '2024-01-18', 'PROCESSED', 'BANK_TRANSFER', 'TXN-20240118-001', 'Auto loan payment January', 'SYSTEM', NOW(), NOW()),
('PAY-007', 'LOAN-004', 'CUST-004', 548.32, 'USD', '2024-02-18', 'PROCESSED', 'BANK_TRANSFER', 'TXN-20240218-001', 'Auto loan payment February', 'SYSTEM', NOW(), NOW()),
('PAY-008', 'LOAN-004', 'CUST-004', 548.32, 'USD', '2024-03-15', 'PROCESSED', 'BANK_TRANSFER', 'TXN-20240315-001', 'Auto loan payment March (early)', 'SYSTEM', NOW(), NOW()),

-- Mortgage payments
('PAY-009', 'LOAN-007', 'CUST-002', 2072.24, 'USD', '2024-02-01', 'PROCESSED', 'BANK_TRANSFER', 'TXN-20240201-001', 'Mortgage payment February', 'SYSTEM', NOW(), NOW()),
('PAY-010', 'LOAN-007', 'CUST-002', 2072.24, 'USD', '2024-03-01', 'PROCESSED', 'BANK_TRANSFER', 'TXN-20240301-001', 'Mortgage payment March', 'SYSTEM', NOW(), NOW()),
('PAY-011', 'LOAN-007', 'CUST-002', 2072.24, 'USD', '2024-04-01', 'PROCESSED', 'BANK_TRANSFER', 'TXN-20240401-001', 'Mortgage payment April', 'SYSTEM', NOW(), NOW()),

-- Business loan payments
('PAY-012', 'LOAN-010', 'CORP-001', 5247.89, 'USD', '2024-02-20', 'PROCESSED', 'WIRE_TRANSFER', 'TXN-20240220-002', 'Business loan payment February', 'SYSTEM', NOW(), NOW()),
('PAY-013', 'LOAN-010', 'CORP-001', 5247.89, 'USD', '2024-03-18', 'PROCESSED', 'WIRE_TRANSFER', 'TXN-20240318-001', 'Business loan payment March (early)', 'SYSTEM', NOW(), NOW()),

-- Extra payments
('PAY-014', 'LOAN-001', 'CUST-001', 1000.00, 'USD', '2024-03-25', 'PROCESSED', 'BANK_TRANSFER', 'TXN-20240325-001', 'Extra principal payment', 'SYSTEM', NOW(), NOW()),
('PAY-015', 'LOAN-004', 'CUST-004', 2000.00, 'USD', '2024-02-25', 'PROCESSED', 'BANK_TRANSFER', 'TXN-20240225-001', 'Extra payment toward principal', 'SYSTEM', NOW(), NOW()),

-- Failed payments
('PAY-016', 'LOAN-002', 'CUST-002', 425.32, 'USD', '2024-05-05', 'FAILED', 'ACH', 'TXN-20240505-001', 'Insufficient funds', NULL, NOW(), NOW()),
('PAY-017', 'LOAN-016', 'CUST-007', 355.48, 'USD', '2024-04-15', 'FAILED', 'BANK_TRANSFER', 'TXN-20240415-001', 'Account closed', NULL, NOW(), NOW()),

-- Pending payments
('PAY-018', 'LOAN-003', 'CUST-003', 423.45, 'USD', '2024-05-01', 'PENDING', 'ACH', 'TXN-20240501-001', 'Monthly payment May', NULL, NOW(), NOW()),
('PAY-019', 'LOAN-005', 'CUST-005', 524.88, 'USD', '2024-05-10', 'PENDING', 'BANK_TRANSFER', 'TXN-20240510-001', 'Auto loan payment May', NULL, NOW(), NOW());

-- ===================================================================
-- LOAN RECOMMENDATIONS
-- ===================================================================

INSERT INTO loan_recommendations (customer_id, recommended_loan_type, recommended_amount, recommended_term, estimated_interest_rate, monthly_payment, confidence_score, reasoning, created_at) VALUES
('CUST-007', 'PERSONAL', 18000.00, 48, 0.0675, 426.58, 0.75, 'Fair credit score and stable income support moderate personal loan approval', NOW()),
('CUST-008', 'AUTO', 28000.00, 60, 0.0525, 534.21, 0.68, 'Employment history and debt-to-income ratio indicate auto loan potential', NOW()),
('CUST-011', 'PERSONAL', 12000.00, 36, 0.0750, 377.42, 0.82, 'First-time borrower with good income and low debt, start with smaller amount', NOW()),
('CUST-012', 'AUTO', 25000.00, 60, 0.0550, 477.83, 0.78, 'Young professional with steady income, good candidate for auto financing', NOW()),
('SMB-001', 'SMALL_BUSINESS', 50000.00, 60, 0.0625, 969.84, 0.65, 'Small business with growth potential, consider equipment financing', NOW()),
('SMB-002', 'SMALL_BUSINESS', 75000.00, 72, 0.0595, 1089.45, 0.72, 'Established business with good cash flow, suitable for expansion loan', NOW()),
('CORP-004', 'BUSINESS', 2000000.00, 180, 0.0425, 15847.32, 0.88, 'Large corporation with excellent credit profile, prime rate eligible', NOW());

-- ===================================================================
-- AUDIT LOGS
-- ===================================================================

INSERT INTO audit_logs (entity_type, entity_id, action, user_id, timestamp, details, ip_address, user_agent) VALUES
('CUSTOMER', 'CUST-001', 'CREATED', 'SYSTEM', NOW() - INTERVAL 90 DAY, 'Customer profile created during onboarding', '192.168.1.100', 'Banking App v1.0'),
('CUSTOMER', 'CUST-001', 'CREDIT_CHECK', 'LOAN_OFFICER_001', NOW() - INTERVAL 75 DAY, 'Credit score retrieved for loan application', '192.168.1.102', 'Banking Admin Portal'),
('LOAN', 'LOAN-001', 'CREATED', 'SYSTEM', NOW() - INTERVAL 75 DAY, 'Loan application submitted by customer', '192.168.1.100', 'Banking App v1.0'),
('LOAN', 'LOAN-001', 'APPROVED', 'UNDERWRITER_001', NOW() - INTERVAL 72 DAY, 'Loan approved after automated and manual review', '192.168.1.105', 'Underwriting System'),
('LOAN', 'LOAN-001', 'DISBURSED', 'SYSTEM', NOW() - INTERVAL 70 DAY, 'Funds disbursed to customer account', '10.0.0.50', 'Core Banking System'),
('PAYMENT', 'PAY-001', 'PROCESSED', 'SYSTEM', NOW() - INTERVAL 40 DAY, 'Monthly payment processed successfully', '10.0.0.60', 'Payment Processing Service'),
('PAYMENT', 'PAY-002', 'PROCESSED', 'SYSTEM', NOW() - INTERVAL 10 DAY, 'Monthly payment processed successfully', '10.0.0.60', 'Payment Processing Service'),
('CUSTOMER', 'CUST-002', 'PROFILE_UPDATED', 'CUST-002', NOW() - INTERVAL 30 DAY, 'Customer updated contact information', '192.168.1.101', 'Banking Mobile App'),
('LOAN', 'LOAN-016', 'CREATED', 'SYSTEM', NOW() - INTERVAL 5 DAY, 'New loan application submitted', '192.168.1.103', 'Banking App v1.0'),
('PAYMENT', 'PAY-016', 'FAILED', 'SYSTEM', NOW() - INTERVAL 1 DAY, 'Payment failed due to insufficient funds', '10.0.0.60', 'Payment Processing Service'),
('CUSTOMER', 'CORP-001', 'DOCUMENT_UPLOADED', 'CORP_ADMIN_001', NOW() - INTERVAL 20 DAY, 'Financial statements uploaded for loan review', '203.0.113.10', 'Corporate Portal'),
('LOAN', 'LOAN-010', 'MODIFIED', 'RELATIONSHIP_MGR_001', NOW() - INTERVAL 15 DAY, 'Loan terms modified per customer request', '192.168.1.110', 'Relationship Manager Portal'),
('SYSTEM', 'BACKUP', 'COMPLETED', 'SYSTEM', NOW() - INTERVAL 1 DAY, 'Daily backup completed successfully', '10.0.0.10', 'Backup Service'),
('SECURITY', 'LOGIN_ATTEMPT', 'SUCCESS', 'CUST-001', NOW() - INTERVAL 2 HOUR, 'Customer login successful', '192.168.1.100', 'Banking Mobile App'),
('SECURITY', 'LOGIN_ATTEMPT', 'FAILED', 'UNKNOWN', NOW() - INTERVAL 1 HOUR, 'Failed login attempt with invalid credentials', '203.0.113.50', 'Unknown'),
('COMPLIANCE', 'KYC_CHECK', 'COMPLETED', 'COMPLIANCE_001', NOW() - INTERVAL 45 DAY, 'KYC verification completed for new customer', '192.168.1.120', 'Compliance System'),
('FRAUD', 'TRANSACTION', 'FLAGGED', 'FRAUD_SYSTEM', NOW() - INTERVAL 3 DAY, 'Large payment flagged for review', '10.0.0.70', 'Fraud Detection System'),
('FRAUD', 'TRANSACTION', 'CLEARED', 'FRAUD_ANALYST_001', NOW() - INTERVAL 2 DAY, 'Flagged transaction cleared after review', '192.168.1.130', 'Fraud Management Portal');

-- ===================================================================
-- LOAN OFFERS (AI Generated)
-- ===================================================================

INSERT INTO loan_offers (offer_id, customer_id, loan_type, offered_amount, interest_rate, term_months, monthly_payment, expiry_date, status, confidence_score, ai_model_version, created_at) VALUES
('OFFER-001', 'CUST-007', 'PERSONAL', 15000.00, 0.0695, 48, 355.48, DATE_ADD(NOW(), INTERVAL 30 DAY), 'ACTIVE', 0.75, 'GPT-4-TURBO-v1.2', NOW()),
('OFFER-002', 'CUST-008', 'AUTO', 30000.00, 0.0485, 72, 468.32, DATE_ADD(NOW(), INTERVAL 30 DAY), 'ACTIVE', 0.68, 'GPT-4-TURBO-v1.2', NOW()),
('OFFER-003', 'CUST-011', 'PERSONAL', 10000.00, 0.0750, 36, 314.52, DATE_ADD(NOW(), INTERVAL 30 DAY), 'ACTIVE', 0.82, 'GPT-4-TURBO-v1.2', NOW()),
('OFFER-004', 'CUST-012', 'AUTO', 22000.00, 0.0550, 60, 420.45, DATE_ADD(NOW(), INTERVAL 30 DAY), 'ACTIVE', 0.78, 'GPT-4-TURBO-v1.2', NOW()),
('OFFER-005', 'SMB-001', 'SMALL_BUSINESS', 60000.00, 0.0625, 60, 1163.89, DATE_ADD(NOW(), INTERVAL 30 DAY), 'ACTIVE', 0.65, 'GPT-4-TURBO-v1.2', NOW()),
('OFFER-006', 'CUST-009', 'PERSONAL', 5000.00, 0.0895, 24, 230.67, DATE_ADD(NOW(), INTERVAL 15 DAY), 'ACTIVE', 0.45, 'GPT-4-TURBO-v1.2', NOW()),
('OFFER-007', 'CORP-004', 'BUSINESS', 1500000.00, 0.0445, 120, 15789.45, DATE_ADD(NOW(), INTERVAL 60 DAY), 'ACTIVE', 0.88, 'GPT-4-TURBO-v1.2', NOW());

-- ===================================================================
-- SYSTEM CONFIGURATION
-- ===================================================================

INSERT INTO system_config (config_key, config_value, description, created_at, updated_at) VALUES
('MAX_LOAN_AMOUNT_PERSONAL', '100000.00', 'Maximum personal loan amount in USD', NOW(), NOW()),
('MAX_LOAN_AMOUNT_AUTO', '150000.00', 'Maximum auto loan amount in USD', NOW(), NOW()),
('MAX_LOAN_AMOUNT_MORTGAGE', '2000000.00', 'Maximum mortgage loan amount in USD', NOW(), NOW()),
('MAX_LOAN_AMOUNT_BUSINESS', '10000000.00', 'Maximum business loan amount in USD', NOW(), NOW()),
('MIN_CREDIT_SCORE_PERSONAL', '580', 'Minimum credit score for personal loans', NOW(), NOW()),
('MIN_CREDIT_SCORE_AUTO', '600', 'Minimum credit score for auto loans', NOW(), NOW()),
('MIN_CREDIT_SCORE_MORTGAGE', '640', 'Minimum credit score for mortgage loans', NOW(), NOW()),
('MAX_DTI_RATIO', '0.50', 'Maximum debt-to-income ratio allowed', NOW(), NOW()),
('LATE_FEE_AMOUNT', '25.00', 'Standard late fee amount in USD', NOW(), NOW()),
('GRACE_PERIOD_DAYS', '5', 'Grace period before late fees apply', NOW(), NOW()),
('INTEREST_RATE_BASE_PERSONAL', '0.0599', 'Base interest rate for personal loans', NOW(), NOW()),
('INTEREST_RATE_BASE_AUTO', '0.0449', 'Base interest rate for auto loans', NOW(), NOW()),
('INTEREST_RATE_BASE_MORTGAGE', '0.0379', 'Base interest rate for mortgage loans', NOW(), NOW()),
('FRAUD_DETECTION_THRESHOLD', '10000.00', 'Amount threshold for fraud detection', NOW(), NOW()),
('KYC_VERIFICATION_REQUIRED', 'true', 'Whether KYC verification is required', NOW(), NOW()),
('AI_RECOMMENDATIONS_ENABLED', 'true', 'Whether AI loan recommendations are enabled', NOW(), NOW()),
('TEST_MODE', 'true', 'Whether system is in test mode', NOW(), NOW());

-- Re-enable foreign key checks
SET foreign_key_checks = 1;

COMMIT;

-- ===================================================================
-- END OF TEST DATA
-- ===================================================================