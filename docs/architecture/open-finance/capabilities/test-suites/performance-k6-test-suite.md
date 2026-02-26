# Performance Test Scripts (K6)
**Scope:** NFR Validation (SLA: 500ms, 99.9% Availability)
**Tool:** Grafana K6

## 1. Script: `load_test_accounts.js`
*Simulates high-volume read traffic for Personal Financial Management (Accounts).*

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
  stages: [
    { duration: '30s', target: 50 }, // Ramp up to 50 users
    { duration: '1m', target: 50 },  // Stay at 50 users
    { duration: '30s', target: 0 },  // Ramp down
  ],
  thresholds: {
    'http_req_duration': ['p(95)<500'], // 95% of requests must be < 500ms
    'http_req_failed': ['rate<0.01'],   // Error rate < 1%
  },
};

const BASE_URL = '[https://api.sandbox.openfinance.ae](https://api.sandbox.openfinance.ae)';
const TOKEN = '{{ACCESS_TOKEN}}'; // Replace with valid token

export default function () {
  let params = {
    headers: {
      'Authorization': `Bearer ${TOKEN}`,
      'x-fapi-interaction-id': 'k6-test-run-' + __VU,
      'Accept': 'application/json',
    },
  };

  let res = http.get(`${BASE_URL}/open-banking/v1/accounts`, params);

  check(res, {
    'is status 200': (r) => r.status === 200,
    'TTLB < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(1);
}
```

## 2. Script: `stress_test_payments.js`

*Simulates burst traffic for Payment Initiation (Payments) to test Idempotency and Locking.*

```javascript
import http from 'k6/http';
import { check } from 'k6';
import { uuidv4 } from '[https://jslib.k6.io/k6-utils/1.4.0/index.js](https://jslib.k6.io/k6-utils/1.4.0/index.js)';

export let options = {
  scenarios: {
    burst_payment: {
      executor: 'constant-arrival-rate',
      rate: 100, // 100 payments per second
      timeUnit: '1s',
      duration: '30s',
      preAllocatedVUs: 50,
    },
  },
};

export default function () {
  const payload = JSON.stringify({
    Data: {
      ConsentId: "CONS_TEST_123",
      Initiation: {
        InstructedAmount: { Amount: "10.00", Currency: "AED" },
        CreditorAccount: { SchemeName: "IBAN", Identification: "AE21..." }
      }
    }
  });

  const params = {
    headers: {
      'Authorization': 'Bearer {{TOKEN}}',
      'Content-Type': 'application/json',
      'x-idempotency-key': uuidv4(), // Unique key per request
      'x-jws-signature': 'detached-sig-mock'
    },
  };

  let res = http.post('[https://api.sandbox.openfinance.ae/open-banking/v1/payments](https://api.sandbox.openfinance.ae/open-banking/v1/payments)', payload, params);

  check(res, {
    'is created 201': (r) => r.status === 201,
  });
}
```
