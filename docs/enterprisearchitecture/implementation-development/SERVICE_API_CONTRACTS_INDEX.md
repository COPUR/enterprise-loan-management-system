# Service API Contracts Index

Each bounded context service must publish an OpenAPI contract. These initial contracts are **stubs** to remove implicit coupling and enforce API‑first design.

| Bounded Context Service | OpenAPI File | Status |
| --- | --- | --- |
| Open Finance Context Service | `api/openapi/open-finance-context.yaml` | Stub |
| Payment Context Service | `api/openapi/payment-context.yaml` | Stub |
| Loan Context Service | `api/openapi/loan-context.yaml` | Stub |
| Risk Context Service | `api/openapi/risk-context.yaml` | Stub |
| Customer Context Service | `api/openapi/customer-context.yaml` | Stub |
| Compliance Context Service | `api/openapi/compliance-context.yaml` | Stub |

## Contract Rules
- All endpoints must be versioned (e.g., `/v1/...`).
- Authentication and token type must be explicit (Bearer/DPoP).
- Error schema must be documented and consistent.

