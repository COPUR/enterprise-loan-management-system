#!/bin/bash

# Gitpod Setup Script for Enterprise Loan Management System
# Automated initialization with sample banking data

set -e

echo "🏦 Enterprise Loan Management System - Gitpod Setup"
echo "=================================================="

# Function to wait for service
wait_for_service() {
    local service_name=$1
    local port=$2
    local max_attempts=30
    local attempt=0
    
    echo "⏳ Waiting for $service_name on port $port..."
    
    while ! nc -z localhost $port; do
        attempt=$((attempt + 1))
        if [ $attempt -eq $max_attempts ]; then
            echo "❌ $service_name failed to start"
            exit 1
        fi
        sleep 2
    done
    
    echo "✅ $service_name is ready"
}

# Set environment variables
export JAVA_HOME=/home/gitpod/.sdkman/candidates/java/21.0.1-tem
export PATH=$JAVA_HOME/bin:$PATH
export DATABASE_URL="jdbc:postgresql://localhost:5432/enterprise_loan_db?user=loan_admin&password=secure_banking_2024"
export REDIS_URL="redis://localhost:6379"
export SPRING_PROFILES_ACTIVE="development,showcase"
export SERVER_PORT=5000

echo "☕ Java Environment:"
java -version

# Start PostgreSQL
echo "🐘 Starting PostgreSQL..."
sudo service postgresql start
wait_for_service "PostgreSQL" 5432

# Initialize database schema
echo "📊 Initializing banking database schema..."
sudo -u postgres psql enterprise_loan_db -c "
-- Customer Management Schema
CREATE TABLE IF NOT EXISTS customers (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(50),
    address TEXT,
    credit_score INTEGER DEFAULT 650,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Loan Management Schema
CREATE TABLE IF NOT EXISTS loans (
    id SERIAL PRIMARY KEY,
    customer_id INTEGER REFERENCES customers(id),
    amount DECIMAL(15,2) NOT NULL,
    interest_rate DECIMAL(5,4) NOT NULL,
    term_months INTEGER NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    purpose TEXT,
    approved_amount DECIMAL(15,2),
    approved_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Payment Processing Schema
CREATE TABLE IF NOT EXISTS payments (
    id SERIAL PRIMARY KEY,
    loan_id INTEGER REFERENCES loans(id),
    amount DECIMAL(15,2) NOT NULL,
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    payment_method VARCHAR(50) DEFAULT 'BANK_TRANSFER',
    status VARCHAR(50) DEFAULT 'COMPLETED',
    reference_number VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Installment Schedule Schema
CREATE TABLE IF NOT EXISTS installments (
    id SERIAL PRIMARY KEY,
    loan_id INTEGER REFERENCES loans(id),
    installment_number INTEGER NOT NULL,
    due_date DATE NOT NULL,
    principal_amount DECIMAL(15,2) NOT NULL,
    interest_amount DECIMAL(15,2) NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    paid_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Banking Audit Schema
CREATE TABLE IF NOT EXISTS audit_logs (
    id SERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id INTEGER NOT NULL,
    action VARCHAR(50) NOT NULL,
    old_values JSONB,
    new_values JSONB,
    user_id VARCHAR(100),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
"

# Insert sample banking data
echo "💰 Inserting sample banking data..."
sudo -u postgres psql enterprise_loan_db -c "
-- Sample Customers
INSERT INTO customers (name, email, phone, address, credit_score) VALUES
('Alice Johnson', 'alice.johnson@example.com', '+1-555-0101', '123 Oak Street, Springfield, IL 62701', 720),
('Bob Smith', 'bob.smith@example.com', '+1-555-0102', '456 Pine Avenue, Madison, WI 53703', 680),
('Carol Brown', 'carol.brown@example.com', '+1-555-0103', '789 Elm Drive, Austin, TX 73301', 750),
('David Wilson', 'david.wilson@example.com', '+1-555-0104', '321 Maple Lane, Portland, OR 97201', 695),
('Emma Davis', 'emma.davis@example.com', '+1-555-0105', '654 Cedar Court, Denver, CO 80201', 710)
ON CONFLICT (email) DO NOTHING;

-- Sample Loans
INSERT INTO loans (customer_id, amount, interest_rate, term_months, status, purpose, approved_amount, approved_at) VALUES
(1, 25000.00, 0.12, 24, 'APPROVED', 'Home improvement', 25000.00, CURRENT_TIMESTAMP - INTERVAL '30 days'),
(2, 50000.00, 0.15, 36, 'APPROVED', 'Business expansion', 45000.00, CURRENT_TIMESTAMP - INTERVAL '20 days'),
(3, 15000.00, 0.10, 12, 'APPROVED', 'Debt consolidation', 15000.00, CURRENT_TIMESTAMP - INTERVAL '15 days'),
(4, 75000.00, 0.18, 48, 'PENDING', 'Equipment purchase', NULL, NULL),
(5, 30000.00, 0.14, 24, 'APPROVED', 'Vehicle purchase', 30000.00, CURRENT_TIMESTAMP - INTERVAL '10 days');

-- Sample Payments
INSERT INTO payments (loan_id, amount, payment_method, reference_number) VALUES
(1, 1200.50, 'BANK_TRANSFER', 'TXN-2024-001'),
(1, 1200.50, 'BANK_TRANSFER', 'TXN-2024-002'),
(2, 1450.75, 'ACH_DEBIT', 'TXN-2024-003'),
(3, 1350.25, 'ONLINE_PAYMENT', 'TXN-2024-004'),
(5, 1400.00, 'BANK_TRANSFER', 'TXN-2024-005');

-- Sample Installments for Loan 1 (24 months)
INSERT INTO installments (loan_id, installment_number, due_date, principal_amount, interest_amount, total_amount, status, paid_date) VALUES
(1, 1, CURRENT_DATE - INTERVAL '30 days', 950.50, 250.00, 1200.50, 'PAID', CURRENT_TIMESTAMP - INTERVAL '30 days'),
(1, 2, CURRENT_DATE, 960.75, 239.75, 1200.50, 'PAID', CURRENT_TIMESTAMP - INTERVAL '1 day'),
(1, 3, CURRENT_DATE + INTERVAL '30 days', 971.15, 229.35, 1200.50, 'PENDING', NULL);

-- Audit Log Entries
INSERT INTO audit_logs (entity_type, entity_id, action, new_values, user_id) VALUES
('CUSTOMER', 1, 'CREATE', '{\"name\": \"Alice Johnson\", \"email\": \"alice.johnson@example.com\"}', 'system'),
('LOAN', 1, 'APPROVE', '{\"status\": \"APPROVED\", \"approved_amount\": 25000.00}', 'loan_officer_1'),
('PAYMENT', 1, 'CREATE', '{\"amount\": 1200.50, \"method\": \"BANK_TRANSFER\"}', 'system');
"

# Start Redis
echo "⚡ Starting Redis cache..."
sudo service redis-server start
wait_for_service "Redis" 6379

# Warm up Redis cache with banking data
echo "🔥 Warming up Redis cache..."
redis-cli SET "customer:1:profile" '{"id":1,"name":"Alice Johnson","creditScore":720,"status":"ACTIVE"}' EX 3600
redis-cli SET "loan:1:summary" '{"id":1,"amount":25000.00,"status":"APPROVED","paymentsCount":2}' EX 3600
redis-cli SET "system:stats" '{"totalCustomers":5,"activeLloans":4,"totalPayments":5}' EX 300

echo "🔨 Building Enterprise Loan Management System..."
./gradlew clean build -x test --no-daemon --console=plain

echo "🚀 Starting Banking System..."
echo "Dashboard: https://5000-$GITPOD_WORKSPACE_ID.$GITPOD_WORKSPACE_CLUSTER_HOST"
echo "API Docs: https://5000-$GITPOD_WORKSPACE_ID.$GITPOD_WORKSPACE_CLUSTER_HOST/swagger-ui.html"
echo "Health: https://5000-$GITPOD_WORKSPACE_ID.$GITPOD_WORKSPACE_CLUSTER_HOST/actuator/health"

# Start the banking application
java -jar build/libs/enterprise-loan-system-1.0.0.jar &

# Wait for application startup
wait_for_service "Banking System" 5000

echo ""
echo "🎉 Enterprise Loan Management System Ready!"
echo "=================================================="
echo "✅ PostgreSQL: Running with sample banking data"
echo "✅ Redis: Running with cached customer profiles"
echo "✅ Banking APIs: Available with Swagger documentation"
echo "✅ Sample Data: 5 customers, 5 loans, 5 payments ready"
echo ""
echo "Quick Test Commands:"
echo "curl https://5000-$GITPOD_WORKSPACE_ID.$GITPOD_WORKSPACE_CLUSTER_HOST/actuator/health"
echo "curl https://5000-$GITPOD_WORKSPACE_ID.$GITPOD_WORKSPACE_CLUSTER_HOST/api/customers"
echo ""
echo "Happy Banking! 🏦"