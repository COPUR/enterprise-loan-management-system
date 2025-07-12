/*
 * ===============================================================
 * ENTERPRISE BANKING SYSTEM - K6 PERFORMANCE TEST SUITE
 * ===============================================================
 * Document Information:
 * - Author: Senior Performance Engineer & Load Testing Specialist
 * - Version: 1.0.0
 * - Last Updated: December 2024
 * - Classification: Internal - Performance Testing
 * - Purpose: Comprehensive load testing for banking microservices
 * ===============================================================
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js';

// Custom Metrics for Banking Performance
export const errorRate = new Rate('errors');
export const responseTimeTrend = new Trend('response_time');
export const transactionCounter = new Counter('banking_transactions');
export const sagaSuccessRate = new Rate('saga_success_rate');

// Performance Test Configuration
export const options = {
  stages: [
    // Ramp-up phase
    { duration: '2m', target: 10 },   // Ramp up to 10 users over 2 minutes
    { duration: '5m', target: 20 },   // Stay at 20 users for 5 minutes
    { duration: '3m', target: 50 },   // Ramp up to 50 users over 3 minutes
    { duration: '10m', target: 50 },  // Stay at 50 users for 10 minutes (sustained load)
    { duration: '2m', target: 100 },  // Spike to 100 users for 2 minutes
    { duration: '5m', target: 20 },   // Ramp down to 20 users
    { duration: '2m', target: 0 },    // Ramp down to 0 users
  ],
  thresholds: {
    // Performance requirements for enterprise banking
    http_req_duration: ['p(95)<2000'], // 95% of requests must complete below 2s
    http_req_failed: ['rate<0.01'],    // Error rate must be less than 1%
    checks: ['rate>0.95'],             // 95% of checks must pass
    saga_success_rate: ['rate>0.98'],  // 98% of SAGA transactions must succeed
  },
};

// Test Configuration
const BASE_URL = __ENV.API_BASE_URL || 'http://localhost:8090';
const TEST_USER_PREFIX = 'loadtest_';
const MAX_TEST_CUSTOMERS = 1000;

// Banking Test Data Templates
const CUSTOMER_TEMPLATES = [
  {
    firstName: 'Ahmed',
    lastName: 'Al-Rahman',
    email: 'ahmed.rahman@loadtest.com',
    phone: '+971501000000',
    address: 'Load Test Street 123',
    city: 'Dubai',
    postalCode: '12345',
    country: 'UAE',
    creditScore: 750,
    creditLimit: 200000
  },
  {
    firstName: 'Sarah',
    lastName: 'Johnson',
    email: 'sarah.johnson@loadtest.com',
    phone: '+971501000001',
    address: 'Performance Test Ave 456',
    city: 'Abu Dhabi',
    postalCode: '12346',
    country: 'UAE',
    creditScore: 720,
    creditLimit: 150000
  },
];

const LOAN_TEMPLATES = [
  {
    loanAmount: 50000,
    interestRate: 0.15,
    installmentCount: 12,
    loanType: 'PERSONAL',
    purpose: 'HOME_RENOVATION'
  },
  {
    loanAmount: 100000,
    interestRate: 0.18,
    installmentCount: 24,
    loanType: 'PERSONAL',
    purpose: 'EDUCATION'
  },
  {
    loanAmount: 25000,
    interestRate: 0.20,
    installmentCount: 6,
    loanType: 'EMERGENCY',
    purpose: 'MEDICAL_EXPENSES'
  },
];

// Authentication Token (for testing purposes)
let authToken = null;

export function setup() {
  console.log('Setting up performance test environment...');
  
  // Health check before starting tests
  const healthResponse = http.get(`${BASE_URL}/actuator/health`);
  if (!check(healthResponse, {
    'API Gateway is healthy': (r) => r.status === 200,
  })) {
    throw new Error('API Gateway health check failed');
  }
  
  console.log('Performance test setup complete');
  return { startTime: new Date().toISOString() };
}

export default function (data) {
  // Generate unique test user ID for this VU
  const userId = `${TEST_USER_PREFIX}${__VU}_${__ITER}`;
  const customerTemplate = CUSTOMER_TEMPLATES[__VU % CUSTOMER_TEMPLATES.length];
  const loanTemplate = LOAN_TEMPLATES[__VU % LOAN_TEMPLATES.length];
  
  // Banking Workflow Performance Test
  group('Complete Banking Customer Journey', function () {
    
    // Step 1: Customer Registration/Retrieval
    group('Customer Management', function () {
      const customerData = {
        ...customerTemplate,
        firstName: `${customerTemplate.firstName}_${userId}`,
        email: `${userId}@loadtest.com`,
        phone: `+97150${String(__VU).padStart(7, '0')}`
      };
      
      const createCustomerResponse = http.post(
        `${BASE_URL}/api/v1/customers`,
        JSON.stringify(customerData),
        {
          headers: {
            'Content-Type': 'application/json',
            'X-Load-Test': 'true',
          },
        }
      );
      
      const customerSuccess = check(createCustomerResponse, {
        'Customer creation successful': (r) => r.status === 201 || r.status === 200,
        'Customer response time acceptable': (r) => r.timings.duration < 1000,
      });
      
      errorRate.add(!customerSuccess);
      responseTimeTrend.add(createCustomerResponse.timings.duration);
      
      if (customerSuccess && createCustomerResponse.json()) {
        const customerId = createCustomerResponse.json().id;
        
        // Customer retrieval test
        const getCustomerResponse = http.get(`${BASE_URL}/api/v1/customers/${customerId}`);
        check(getCustomerResponse, {
          'Customer retrieval successful': (r) => r.status === 200,
          'Customer data integrity': (r) => r.json() && r.json().id === customerId,
        });
      }
    });
    
    sleep(1); // Simulate user think time
    
    // Step 2: Loan Application (SAGA Transaction)
    group('Loan Origination SAGA', function () {
      const loanData = {
        ...loanTemplate,
        customerId: __VU, // Use VU ID as customer reference
        loanAmount: loanTemplate.loanAmount + (__VU * 1000), // Vary amounts
      };
      
      const loanApplicationResponse = http.post(
        `${BASE_URL}/api/v1/loans`,
        JSON.stringify(loanData),
        {
          headers: {
            'Content-Type': 'application/json',
            'X-Load-Test': 'true',
            'X-SAGA-Test': 'true',
          },
          timeout: '10s', // Extended timeout for SAGA
        }
      );
      
      const sagaSuccess = check(loanApplicationResponse, {
        'SAGA loan creation successful': (r) => r.status === 201 || r.status === 200,
        'SAGA response time within limits': (r) => r.timings.duration < 5000,
        'SAGA response has transaction ID': (r) => r.json() && r.json().sagaId,
      });
      
      sagaSuccessRate.add(sagaSuccess);
      transactionCounter.add(1);
      
      if (sagaSuccess && loanApplicationResponse.json()) {
        const loanId = loanApplicationResponse.json().id;
        
        // Verify loan creation with installments
        sleep(2); // Allow for async processing
        
        const loanDetailsResponse = http.get(`${BASE_URL}/api/v1/loans/${loanId}`);
        check(loanDetailsResponse, {
          'Loan details retrieval successful': (r) => r.status === 200,
          'Loan has installments': (r) => r.json() && r.json().installmentCount > 0,
        });
        
        // Get installment schedule
        const installmentsResponse = http.get(`${BASE_URL}/api/v1/loans/${loanId}/installments`);
        check(installmentsResponse, {
          'Installments generated successfully': (r) => r.status === 200,
          'Installments count matches loan': (r) => {
            const installments = r.json();
            return installments && installments.length === loanTemplate.installmentCount;
          },
        });
      }
    });
    
    sleep(2); // Simulate user think time
    
    // Step 3: Payment Processing
    group('Payment Processing', function () {
      // Simulate payment for existing loan
      const paymentData = {
        loanId: __VU, // Use VU ID as loan reference
        amount: 5000 + (__VU * 100), // Vary payment amounts
        paymentMethod: 'BANK_TRANSFER',
        paymentDate: new Date().toISOString(),
      };
      
      const paymentResponse = http.post(
        `${BASE_URL}/api/v1/payments`,
        JSON.stringify(paymentData),
        {
          headers: {
            'Content-Type': 'application/json',
            'X-Load-Test': 'true',
          },
        }
      );
      
      const paymentSuccess = check(paymentResponse, {
        'Payment processing successful': (r) => r.status === 201 || r.status === 200,
        'Payment response time acceptable': (r) => r.timings.duration < 2000,
        'Payment has transaction reference': (r) => r.json() && r.json().transactionReference,
      });
      
      transactionCounter.add(1);
      
      if (paymentSuccess && paymentResponse.json()) {
        const paymentId = paymentResponse.json().id;
        
        // Verify payment details
        const paymentDetailsResponse = http.get(`${BASE_URL}/api/v1/payments/${paymentId}`);
        check(paymentDetailsResponse, {
          'Payment details retrieval successful': (r) => r.status === 200,
          'Payment status is completed': (r) => r.json() && r.json().status === 'COMPLETED',
        });
      }
    });
    
    sleep(1);
    
    // Step 4: Performance Monitoring Endpoints
    group('System Health Monitoring', function () {
      const healthResponse = http.get(`${BASE_URL}/actuator/health`);
      check(healthResponse, {
        'System health check passes': (r) => r.status === 200,
        'Health response time acceptable': (r) => r.timings.duration < 500,
      });
      
      const metricsResponse = http.get(`${BASE_URL}/actuator/metrics`);
      check(metricsResponse, {
        'Metrics endpoint accessible': (r) => r.status === 200,
      });
    });
  });
  
  // Simulate real user behavior
  sleep(Math.random() * 3 + 1); // Random sleep between 1-4 seconds
}

export function teardown(data) {
  console.log('Performance test teardown...');
  console.log(`Test started at: ${data.startTime}`);
  console.log(`Test completed at: ${new Date().toISOString()}`);
}

// Generate HTML report
export function handleSummary(data) {
  return {
    'k6-performance-report.html': htmlReport(data),
    'k6-performance-summary.json': JSON.stringify(data),
  };
}

/*
 * Banking-Specific Performance Scenarios
 */

// High-Volume Transaction Scenario
export function highVolumeTransactionTest() {
  group('High Volume Transaction Processing', function () {
    const transactions = [];
    
    // Generate multiple concurrent transactions
    for (let i = 0; i < 10; i++) {
      const paymentData = {
        loanId: Math.floor(Math.random() * 1000) + 1,
        amount: Math.floor(Math.random() * 10000) + 1000,
        paymentMethod: ['BANK_TRANSFER', 'ONLINE_BANKING', 'MOBILE_APP'][i % 3],
        paymentDate: new Date().toISOString(),
      };
      
      transactions.push(
        http.asyncRequest('POST', `${BASE_URL}/api/v1/payments`, JSON.stringify(paymentData), {
          headers: { 'Content-Type': 'application/json' },
        })
      );
    }
    
    // Wait for all transactions to complete
    const responses = http.batch(transactions);
    const successfulTransactions = responses.filter(r => r.status === 200 || r.status === 201);
    
    check(responses, {
      'High volume transaction success rate > 95%': () => successfulTransactions.length / responses.length > 0.95,
    });
  });
}

// Islamic Banking Scenario
export function islamicBankingTest() {
  group('Islamic Banking (Sharia-Compliant) Operations', function () {
    const islamicLoanData = {
      customerId: __VU,
      loanAmount: 150000,
      interestRate: 0.12, // Profit rate for Islamic finance
      installmentCount: 24,
      loanType: 'MURABAHA',
      purpose: 'VEHICLE_PURCHASE',
      shariaCompliant: true,
    };
    
    const islamicLoanResponse = http.post(
      `${BASE_URL}/api/v1/loans/islamic`,
      JSON.stringify(islamicLoanData),
      {
        headers: {
          'Content-Type': 'application/json',
          'X-Sharia-Compliant': 'true',
        },
      }
    );
    
    check(islamicLoanResponse, {
      'Islamic loan creation successful': (r) => r.status === 201,
      'Sharia compliance verified': (r) => r.json() && r.json().shariaCompliant === true,
    });
  });
}

// Stress Test Scenario
export function stressTestScenario() {
  group('System Stress Test', function () {
    // Rapid-fire requests to test system limits
    const requests = [];
    
    for (let i = 0; i < 20; i++) {
      requests.push(['GET', `${BASE_URL}/actuator/health`, null, { tags: { name: 'stress_health' } }]);
      requests.push(['GET', `${BASE_URL}/api/v1/customers/${__VU}`, null, { tags: { name: 'stress_customer' } }]);
    }
    
    const batchResponses = http.batch(requests);
    
    check(batchResponses, {
      'Stress test - all requests processed': (responses) => responses.length === requests.length,
      'Stress test - error rate under 5%': (responses) => {
        const errors = responses.filter(r => r.status >= 400);
        return errors.length / responses.length < 0.05;
      },
    });
  });
}