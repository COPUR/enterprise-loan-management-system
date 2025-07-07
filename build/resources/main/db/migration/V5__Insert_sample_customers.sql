-- Insert sample customers for development and testing
-- These are realistic customer profiles for testing the loan management system

INSERT INTO customers (id, name, surname, credit_limit, used_credit_limit) VALUES
-- High credit limit customers
(1, 'John', 'Smith', 100000.00, 0.00),
(2, 'Sarah', 'Johnson', 150000.00, 25000.00),
(3, 'Michael', 'Brown', 200000.00, 45000.00),

-- Medium credit limit customers  
(4, 'Emily', 'Davis', 75000.00, 15000.00),
(5, 'David', 'Wilson', 80000.00, 0.00),
(6, 'Lisa', 'Miller', 90000.00, 30000.00),

-- Lower credit limit customers
(7, 'James', 'Garcia', 50000.00, 10000.00),
(8, 'Maria', 'Rodriguez', 45000.00, 0.00),
(9, 'Robert', 'Martinez', 60000.00, 20000.00),
(10, 'Jennifer', 'Anderson', 55000.00, 5000.00),

-- Business customers with higher limits
(11, 'Anderson', 'Enterprises', 500000.00, 150000.00),
(12, 'Global', 'Solutions Inc', 750000.00, 200000.00),
(13, 'Tech', 'Innovations LLC', 300000.00, 75000.00),

-- Additional individual customers
(14, 'Christopher', 'Taylor', 65000.00, 12000.00),
(15, 'Amanda', 'Thomas', 70000.00, 18000.00),
(16, 'Daniel', 'Jackson', 85000.00, 0.00),
(17, 'Jessica', 'White', 95000.00, 35000.00),
(18, 'Matthew', 'Harris', 110000.00, 40000.00),
(19, 'Ashley', 'Martin', 75000.00, 22000.00),
(20, 'Andrew', 'Thompson', 120000.00, 60000.00);

-- Update the sequence to continue from the last inserted ID
SELECT setval('customers_id_seq', (SELECT MAX(id) FROM customers));

-- Add some constraints validation comments
COMMENT ON TABLE customers IS 'Sample customers for development and testing purposes. Credit limits range from 45,000 to 750,000 with various utilization levels.';
