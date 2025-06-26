#!/bin/bash

# Start a simple mock banking server for Postman testing

# Kill any existing processes on port 8080
lsof -ti:8080 | xargs kill -9 2>/dev/null || true

# Start a simple HTTP server that responds to banking API calls
python3 << 'EOF' &
import json
import time
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs
import uuid

class BankingMockHandler(BaseHTTPRequestHandler):
    loans = {}
    customers = {}
    
    def do_GET(self):
        path = urlparse(self.path).path
        query = parse_qs(urlparse(self.path).query)
        
        if path == '/actuator/health':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            response = {'status': 'UP', 'timestamp': time.time()}
            self.wfile.write(json.dumps(response).encode())
            
        elif path == '/api/v1/loans':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            customer_id = query.get('customerId', [None])[0]
            if customer_id:
                filtered_loans = [loan for loan in self.loans.values() if loan.get('customerId') == customer_id]
                self.wfile.write(json.dumps(filtered_loans).encode())
            else:
                self.wfile.write(json.dumps(list(self.loans.values())).encode())
                
        elif path.startswith('/api/v1/loans/') and path.endswith('/installments'):
            loan_id = path.split('/')[-2]
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            installments = [
                {'installmentNumber': i, 'amount': 916.67, 'status': 'PENDING', 'dueDate': time.time() + (i * 30 * 24 * 3600)}
                for i in range(1, 13)
            ]
            self.wfile.write(json.dumps(installments).encode())
            
        elif path.startswith('/api/v1/customers/'):
            customer_id = path.split('/')[-1]
            if customer_id in self.customers:
                self.send_response(200)
                self.send_header('Content-type', 'application/json')
                self.end_headers()
                self.wfile.write(json.dumps(self.customers[customer_id]).encode())
            else:
                self.send_response(404)
                self.end_headers()
                
        elif path == '/ai/health':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            response = {'status': 'OPERATIONAL', 'services': ['fraud-detection', 'recommendations']}
            self.wfile.write(json.dumps(response).encode())
            
        else:
            self.send_response(404)
            self.end_headers()
    
    def do_POST(self):
        content_length = int(self.headers['Content-Length'])
        post_data = self.rfile.read(content_length)
        
        try:
            data = json.loads(post_data.decode('utf-8'))
        except:
            data = {}
        
        path = urlparse(self.path).path
        
        if path == '/api/v1/loans':
            loan_id = f"LOAN-{int(time.time())}"
            loan = {
                'loanId': loan_id,
                'applicationReference': f"REF-{int(time.time())}",
                'customerId': data.get('customerId', 'unknown'),
                'amount': data.get('amount', 0),
                'termInMonths': data.get('termInMonths', 12),
                'loanType': data.get('loanType', 'PERSONAL'),
                'status': 'APPROVED',
                'createdAt': time.time()
            }
            self.loans[loan_id] = loan
            
            self.send_response(201)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps(loan).encode())
            
        elif path == '/api/v1/customers':
            customer_id = f"CUST-{int(time.time())}"
            customer = dict(data)
            customer['customerId'] = customer_id
            customer['createdAt'] = time.time()
            self.customers[customer_id] = customer
            
            self.send_response(201)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps(customer).encode())
            
        elif path.endswith('/pay'):
            loan_id = path.split('/')[-2]
            response = {
                'success': True,
                'transactionReference': f"TXN-{int(time.time())}",
                'amount': data.get('amount', 0),
                'fraudCheckResult': {'status': 'CLEAR', 'riskScore': 15}
            }
            
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps(response).encode())
            
        elif path.endswith('/approve'):
            loan_id = path.split('/')[-2]
            if loan_id in self.loans:
                self.loans[loan_id]['status'] = 'APPROVED'
                response = {'success': True, 'loan': self.loans[loan_id]}
            else:
                response = {'success': False, 'error': 'Loan not found'}
                
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps(response).encode())
            
        elif path == '/api/ai/fraud/analyze':
            response = {
                'riskScore': 25,
                'status': 'LOW_RISK',
                'modelUsed': 'enhanced-fraud-detection',
                'confidence': 0.87
            }
            
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps(response).encode())
            
        else:
            self.send_response(404)
            self.end_headers()

if __name__ == '__main__':
    server = HTTPServer(('localhost', 8080), BankingMockHandler)
    print("Mock Banking Server running on http://localhost:8080")
    server.serve_forever()
EOF

MOCK_PID=$!
echo "Mock server started with PID: $MOCK_PID"
echo "Waiting for server to start..."
sleep 3

# Test the server
if curl -s http://localhost:8080/actuator/health > /dev/null; then
    echo "✅ Mock banking server is running successfully!"
    echo "📋 Available endpoints:"
    echo "  - GET  /actuator/health"
    echo "  - POST /api/v1/loans"
    echo "  - GET  /api/v1/loans"
    echo "  - GET  /api/v1/loans/{id}/installments"
    echo "  - POST /api/v1/loans/{id}/pay"
    echo "  - POST /api/v1/customers"
    echo "  - GET  /api/v1/customers/{id}"
    echo "  - POST /api/ai/fraud/analyze"
    echo ""
    echo "🎯 Ready for Postman testing!"
else
    echo "❌ Failed to start mock server"
    kill $MOCK_PID 2>/dev/null
    exit 1
fi