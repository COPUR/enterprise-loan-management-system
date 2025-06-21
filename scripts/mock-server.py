#!/usr/bin/env python3
"""
Mock Banking API Server for Load Testing
=========================================

This script provides a mock HTTP server that simulates the Enterprise Loan Management System
APIs for the purpose of load testing and demonstration.

Features:
- Health check endpoint
- Customer management APIs
- Loan recommendation APIs
- Payment processing APIs
- Metrics and monitoring endpoints
- JWT authentication simulation
- Configurable response times and failure rates

Usage: python3 mock-server.py [port]
"""

import json
import time
import random
import sys
from datetime import datetime, timedelta
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs
import uuid

class MockBankingAPIHandler(BaseHTTPRequestHandler):
    """HTTP Request Handler for mock banking API endpoints."""
    
    def log_message(self, format, *args):
        """Custom logging to reduce noise."""
        timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        print(f"[{timestamp}] {format % args}")
    
    def send_json_response(self, status_code, data, delay_ms=None):
        """Send JSON response with optional delay."""
        if delay_ms:
            time.sleep(delay_ms / 1000.0)
        
        self.send_response(status_code)
        self.send_header('Content-type', 'application/json')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type, Authorization')
        self.end_headers()
        
        response_json = json.dumps(data, indent=2)
        self.wfile.write(response_json.encode('utf-8'))
    
    def do_OPTIONS(self):
        """Handle CORS preflight requests."""
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type, Authorization')
        self.end_headers()
    
    def do_GET(self):
        """Handle GET requests."""
        parsed_path = urlparse(self.path)
        path = parsed_path.path
        
        # Health check endpoint
        if path == '/actuator/health':
            self.handle_health_check()
        
        # Metrics endpoint
        elif path == '/actuator/metrics':
            self.handle_metrics()
        
        # Prometheus endpoint
        elif path == '/actuator/prometheus':
            self.handle_prometheus()
        
        # API health endpoint
        elif path == '/api/v1/health':
            self.handle_api_health()
        
        # Customers endpoint
        elif path == '/api/v1/customers':
            self.handle_get_customers()
        
        # Customer by ID
        elif path.startswith('/api/v1/customers/'):
            customer_id = path.split('/')[-1]
            self.handle_get_customer(customer_id)
        
        # Loans endpoint
        elif path == '/api/v1/loans':
            self.handle_get_loans()
        
        # Loan recommendations latest
        elif path.endswith('/latest'):
            customer_id = path.split('/')[-2]
            self.handle_get_latest_recommendations(customer_id)
        
        # Payments endpoint
        elif path == '/api/v1/payments':
            self.handle_get_payments()
        
        # OAuth2 health
        elif path == '/oauth2/health':
            self.handle_oauth_health()
        
        else:
            self.send_json_response(404, {
                "error": "NOT_FOUND",
                "message": f"Endpoint {path} not found",
                "timestamp": datetime.now().isoformat()
            })
    
    def do_POST(self):
        """Handle POST requests."""
        parsed_path = urlparse(self.path)
        path = parsed_path.path
        
        # Read request body
        content_length = int(self.headers.get('Content-Length', 0))
        post_data = self.rfile.read(content_length).decode('utf-8')
        
        try:
            request_data = json.loads(post_data) if post_data else {}
        except json.JSONDecodeError:
            request_data = {}
        
        # Authentication endpoint
        if path == '/api/v1/auth/login':
            self.handle_login(request_data)
        
        # Customer creation
        elif path == '/api/v1/customers':
            self.handle_create_customer(request_data)
        
        # Loan recommendations
        elif path == '/api/v1/loans/recommendations':
            self.handle_generate_recommendations(request_data)
        
        # Loan creation
        elif path == '/api/v1/loans':
            self.handle_create_loan(request_data)
        
        # Payment processing
        elif path == '/api/v1/payments':
            self.handle_process_payment(request_data)
        
        else:
            self.send_json_response(404, {
                "error": "NOT_FOUND",
                "message": f"Endpoint {path} not found",
                "timestamp": datetime.now().isoformat()
            })
    
    def handle_health_check(self):
        """Handle /actuator/health endpoint."""
        # Simulate occasional unhealthy state (5% chance)
        if random.random() < 0.05:
            self.send_json_response(503, {
                "status": "DOWN",
                "components": {
                    "db": {"status": "DOWN", "details": {"error": "Connection timeout"}},
                    "redis": {"status": "UP"},
                    "diskSpace": {"status": "UP", "details": {"free": 10737418240, "threshold": 10485760}}
                }
            }, delay_ms=random.randint(100, 500))
        else:
            self.send_json_response(200, {
                "status": "UP",
                "components": {
                    "db": {"status": "UP", "details": {"database": "H2", "validationQuery": "isValid()"}},
                    "redis": {"status": "UP", "details": {"version": "7.0.15"}},
                    "diskSpace": {"status": "UP", "details": {"free": 10737418240, "threshold": 10485760}}
                }
            }, delay_ms=random.randint(10, 100))
    
    def handle_metrics(self):
        """Handle /actuator/metrics endpoint."""
        self.send_json_response(200, {
            "names": [
                "jvm.memory.used",
                "jvm.gc.memory.allocated",
                "system.cpu.usage",
                "process.uptime",
                "http.server.requests",
                "jdbc.connections.active",
                "cache.gets",
                "cache.puts"
            ]
        }, delay_ms=random.randint(20, 80))
    
    def handle_prometheus(self):
        """Handle /actuator/prometheus endpoint."""
        prometheus_metrics = f"""# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{{area="heap",id="PS Eden Space",}} {random.randint(50000000, 200000000)}
jvm_memory_used_bytes{{area="heap",id="PS Survivor Space",}} {random.randint(1000000, 10000000)}

# HELP system_cpu_usage The "recent cpu usage" for the whole system
# TYPE system_cpu_usage gauge
system_cpu_usage {random.uniform(0.1, 0.8)}

# HELP http_server_requests_seconds  
# TYPE http_server_requests_seconds summary
http_server_requests_seconds_count{{exception="None",method="GET",outcome="SUCCESS",status="200",uri="/api/v1/customers",}} {random.randint(100, 1000)}
http_server_requests_seconds_sum{{exception="None",method="GET",outcome="SUCCESS",status="200",uri="/api/v1/customers",}} {random.uniform(10.0, 50.0)}

# HELP jdbc_connections_active Number of active connections that can be allocated from the data source.
# TYPE jdbc_connections_active gauge
jdbc_connections_active{{name="dataSource",}} {random.randint(5, 15)}
"""
        
        self.send_response(200)
        self.send_header('Content-type', 'text/plain')
        self.end_headers()
        self.wfile.write(prometheus_metrics.encode('utf-8'))
    
    def handle_api_health(self):
        """Handle /api/v1/health endpoint."""
        self.send_json_response(200, {
            "status": "healthy",
            "timestamp": datetime.now().isoformat(),
            "version": "1.0.0",
            "environment": "development"
        }, delay_ms=random.randint(10, 50))
    
    def handle_oauth_health(self):
        """Handle /oauth2/health endpoint."""
        self.send_json_response(200, {
            "status": "UP",
            "oauth2_provider": "active",
            "token_validation": "operational"
        }, delay_ms=random.randint(20, 100))
    
    def handle_login(self, request_data):
        """Handle authentication login."""
        username = request_data.get('username', '')
        password = request_data.get('password', '')
        
        # Simulate authentication logic
        if username and password:
            token = f"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ7dXNlcm5hbWV9IiwiaWF0Ijp7int9LCJleHAiOnt9fQ.{str(uuid.uuid4())[:16]}"
            self.send_json_response(200, {
                "token": token,
                "tokenType": "Bearer",
                "expiresIn": 3600,
                "refreshToken": str(uuid.uuid4()),
                "user": {
                    "id": str(uuid.uuid4()),
                    "username": username,
                    "roles": ["USER"]
                }
            }, delay_ms=random.randint(50, 200))
        else:
            self.send_json_response(401, {
                "error": "INVALID_CREDENTIALS",
                "message": "Invalid username or password"
            })
    
    def handle_get_customers(self):
        """Handle GET /api/v1/customers."""
        customers = [
            {
                "id": str(uuid.uuid4()),
                "name": f"Customer {i}",
                "email": f"customer{i}@example.com",
                "customerType": "INDIVIDUAL" if i % 2 == 0 else "CORPORATE",
                "creditScore": random.randint(600, 850),
                "status": "ACTIVE",
                "createdAt": (datetime.now() - timedelta(days=random.randint(1, 365))).isoformat()
            }
            for i in range(1, random.randint(5, 20))
        ]
        
        self.send_json_response(200, {
            "customers": customers,
            "totalCount": len(customers),
            "page": 0,
            "size": len(customers)
        }, delay_ms=random.randint(50, 200))
    
    def handle_get_customer(self, customer_id):
        """Handle GET /api/v1/customers/{id}."""
        # Simulate 10% chance of customer not found
        if random.random() < 0.1:
            self.send_json_response(404, {
                "error": "CUSTOMER_NOT_FOUND",
                "message": f"Customer with ID {customer_id} not found"
            })
            return
        
        customer = {
            "id": customer_id,
            "name": f"Customer {customer_id[:8]}",
            "email": f"customer{customer_id[:8]}@example.com",
            "customerType": random.choice(["INDIVIDUAL", "CORPORATE"]),
            "creditScore": random.randint(600, 850),
            "status": "ACTIVE",
            "addresses": [
                {
                    "type": "PRIMARY",
                    "street": "123 Main St",
                    "city": "New York",
                    "state": "NY",
                    "zipCode": "10001",
                    "country": "US"
                }
            ],
            "createdAt": (datetime.now() - timedelta(days=random.randint(1, 365))).isoformat(),
            "lastModified": datetime.now().isoformat()
        }
        
        self.send_json_response(200, customer, delay_ms=random.randint(30, 150))
    
    def handle_create_customer(self, request_data):
        """Handle POST /api/v1/customers."""
        # Simulate validation errors (15% chance)
        if random.random() < 0.15:
            self.send_json_response(400, {
                "error": "VALIDATION_ERROR",
                "message": "Customer validation failed",
                "details": ["Email is required", "Name must be at least 2 characters"]
            })
            return
        
        customer_id = str(uuid.uuid4())
        customer = {
            "id": customer_id,
            "name": request_data.get('name', f'New Customer {customer_id[:8]}'),
            "email": request_data.get('email', f'customer{customer_id[:8]}@example.com'),
            "customerType": request_data.get('customerType', 'INDIVIDUAL'),
            "status": "ACTIVE",
            "createdAt": datetime.now().isoformat()
        }
        
        self.send_json_response(201, customer, delay_ms=random.randint(100, 300))
    
    def handle_generate_recommendations(self, request_data):
        """Handle POST /api/v1/loans/recommendations."""
        customer_id = request_data.get('customerId', str(uuid.uuid4()))
        
        # Simulate processing delay and occasional failures (8% chance)
        if random.random() < 0.08:
            self.send_json_response(500, {
                "error": "INTERNAL_ERROR",
                "message": "AI recommendation service temporarily unavailable"
            })
            return
        
        recommendations = [
            {
                "id": str(uuid.uuid4()),
                "loanType": random.choice(["PERSONAL", "MORTGAGE", "AUTO", "BUSINESS"]),
                "amount": random.randint(10000, 500000),
                "currency": "USD",
                "interestRatePercentage": round(random.uniform(3.5, 15.0), 2),
                "termMonths": random.choice([12, 24, 36, 48, 60]),
                "monthlyPayment": round(random.uniform(200, 2000), 2),
                "riskLevel": random.choice(["LOW", "MEDIUM", "HIGH"]),
                "reasoning": f"Recommended based on credit profile and financial history",
                "confidenceScore": round(random.uniform(0.7, 0.95), 2),
                "features": random.sample(["Competitive Rate", "Fast Approval", "No Prepayment Penalty", "Flexible Terms"], 2)
            }
            for _ in range(random.randint(1, 4))
        ]
        
        response = {
            "customerId": customer_id,
            "recommendations": recommendations,
            "riskAssessment": {
                "riskLevel": random.choice(["LOW", "MEDIUM", "HIGH"]),
                "riskScore": random.randint(15, 85),
                "defaultProbability": round(random.uniform(0.02, 0.15), 2),
                "riskFactors": ["DTI ratio above optimal"] if random.random() < 0.3 else [],
                "mitigatingFactors": ["Excellent credit score", "Stable employment"] if random.random() < 0.7 else [],
                "confidenceLevel": round(random.uniform(0.8, 0.95), 2)
            },
            "analysisVersion": "v2.1",
            "generatedAt": datetime.now().isoformat()
        }
        
        self.send_json_response(200, response, delay_ms=random.randint(200, 800))
    
    def handle_get_latest_recommendations(self, customer_id):
        """Handle GET /api/v1/loans/recommendations/{customerId}/latest."""
        # Simulate 20% chance of no recommendations found
        if random.random() < 0.2:
            self.send_response(204)
            self.end_headers()
            return
        
        # Return mock latest recommendations
        response = {
            "customerId": customer_id,
            "recommendations": [
                {
                    "id": str(uuid.uuid4()),
                    "loanType": "PERSONAL",
                    "amount": 25000,
                    "currency": "USD",
                    "interestRatePercentage": 7.25,
                    "termMonths": 60,
                    "monthlyPayment": 495.87,
                    "riskLevel": "MEDIUM",
                    "reasoning": "Based on recent financial analysis",
                    "confidenceScore": 0.89,
                    "features": ["Competitive Rate", "Fast Approval"]
                }
            ],
            "generatedAt": (datetime.now() - timedelta(hours=2)).isoformat()
        }
        
        self.send_json_response(200, response, delay_ms=random.randint(100, 300))
    
    def handle_get_loans(self):
        """Handle GET /api/v1/loans."""
        loans = [
            {
                "id": str(uuid.uuid4()),
                "customerId": str(uuid.uuid4()),
                "loanType": random.choice(["PERSONAL", "MORTGAGE", "AUTO", "BUSINESS"]),
                "principalAmount": random.randint(10000, 500000),
                "interestRate": round(random.uniform(3.5, 15.0), 2),
                "termMonths": random.choice([12, 24, 36, 48, 60]),
                "status": random.choice(["ACTIVE", "PENDING", "APPROVED", "CLOSED"]),
                "createdAt": (datetime.now() - timedelta(days=random.randint(1, 365))).isoformat()
            }
            for _ in range(random.randint(3, 15))
        ]
        
        self.send_json_response(200, {
            "loans": loans,
            "totalCount": len(loans)
        }, delay_ms=random.randint(50, 200))
    
    def handle_create_loan(self, request_data):
        """Handle POST /api/v1/loans."""
        # Simulate processing delay
        delay_ms = random.randint(300, 1000)
        
        # Simulate occasional processing failures (12% chance)
        if random.random() < 0.12:
            self.send_json_response(400, {
                "error": "LOAN_PROCESSING_ERROR",
                "message": "Unable to process loan application",
                "details": ["Insufficient credit score", "DTI ratio too high"]
            }, delay_ms=delay_ms)
            return
        
        loan = {
            "id": str(uuid.uuid4()),
            "customerId": request_data.get('customerId', str(uuid.uuid4())),
            "loanType": request_data.get('loanType', 'PERSONAL'),
            "principalAmount": request_data.get('amount', 25000),
            "interestRate": request_data.get('interestRate', 7.25),
            "termMonths": request_data.get('termMonths', 60),
            "status": "PENDING",
            "applicationDate": datetime.now().isoformat(),
            "expectedDecisionDate": (datetime.now() + timedelta(days=3)).isoformat()
        }
        
        self.send_json_response(201, loan, delay_ms=delay_ms)
    
    def handle_get_payments(self):
        """Handle GET /api/v1/payments."""
        payments = [
            {
                "id": str(uuid.uuid4()),
                "loanId": str(uuid.uuid4()),
                "amount": round(random.uniform(100, 2000), 2),
                "paymentDate": (datetime.now() - timedelta(days=random.randint(1, 30))).isoformat(),
                "paymentMethod": random.choice(["BANK_TRANSFER", "CREDIT_CARD", "DEBIT_CARD", "ACH"]),
                "status": random.choice(["COMPLETED", "PENDING", "FAILED"]),
                "transactionId": str(uuid.uuid4())
            }
            for _ in range(random.randint(5, 25))
        ]
        
        self.send_json_response(200, {
            "payments": payments,
            "totalCount": len(payments)
        }, delay_ms=random.randint(40, 180))
    
    def handle_process_payment(self, request_data):
        """Handle POST /api/v1/payments."""
        # Simulate payment processing delay
        delay_ms = random.randint(500, 1500)
        
        # Simulate occasional payment failures (10% chance)
        if random.random() < 0.1:
            self.send_json_response(400, {
                "error": "PAYMENT_FAILED",
                "message": "Payment could not be processed",
                "details": ["Insufficient funds", "Card declined"]
            }, delay_ms=delay_ms)
            return
        
        payment = {
            "id": str(uuid.uuid4()),
            "loanId": request_data.get('loanId', str(uuid.uuid4())),
            "amount": request_data.get('amount', 500.0),
            "paymentMethod": request_data.get('paymentMethod', 'BANK_TRANSFER'),
            "status": "COMPLETED",
            "transactionId": str(uuid.uuid4()),
            "processedAt": datetime.now().isoformat(),
            "confirmationNumber": f"PAY-{random.randint(100000, 999999)}"
        }
        
        self.send_json_response(201, payment, delay_ms=delay_ms)

def run_mock_server(port=8080):
    """Run the mock server on the specified port."""
    server_address = ('', port)
    httpd = HTTPServer(server_address, MockBankingAPIHandler)
    
    print(f"Mock Banking API Server starting on port {port}")
    print(f"Available endpoints:")
    print(f"  - GET  http://localhost:{port}/actuator/health")
    print(f"  - GET  http://localhost:{port}/actuator/metrics")
    print(f"  - GET  http://localhost:{port}/actuator/prometheus")
    print(f"  - GET  http://localhost:{port}/api/v1/health")
    print(f"  - POST http://localhost:{port}/api/v1/auth/login")
    print(f"  - GET  http://localhost:{port}/api/v1/customers")
    print(f"  - POST http://localhost:{port}/api/v1/customers")
    print(f"  - POST http://localhost:{port}/api/v1/loans/recommendations")
    print(f"  - GET  http://localhost:{port}/api/v1/loans")
    print(f"  - POST http://localhost:{port}/api/v1/loans")
    print(f"  - GET  http://localhost:{port}/api/v1/payments")
    print(f"  - POST http://localhost:{port}/api/v1/payments")
    print(f"  - GET  http://localhost:{port}/oauth2/health")
    print()
    print("Server features:")
    print("  - Realistic response times (10-1500ms)")
    print("  - Simulated failures (5-15% based on endpoint)")
    print("  - JWT authentication simulation")
    print("  - CORS support")
    print("  - Prometheus metrics")
    print()
    print("Press Ctrl+C to stop the server")
    
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nServer stopping...")
        httpd.server_close()

if __name__ == '__main__':
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8080
    run_mock_server(port)