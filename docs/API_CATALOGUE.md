# API Catalogue

This catalogue is generated from the OpenAPI contracts under `api/openapi`.

- Source of truth: `api/openapi/*.yaml`
- Generated at: 2026-02-26T15:18:15Z
- Scope: Open Finance capability services + bounded-context services

## Service Inventory

| Service | Contract | Title | Version | Endpoints | Security Schemes |
|---|---|---|---|---:|---|
| `atm-directory-service` | `api/openapi/atm-directory-service.yaml` | ATM Directory Service API | `0.1.0` | 1 | public |
| `banking-metadata-service` | `api/openapi/banking-metadata-service.yaml` | Banking Metadata Enrichment Service API | `0.1.0` | 4 | bearerAuth, dpopAuth |
| `business-financial-data-service` | `api/openapi/business-financial-data-service.yaml` | Business Financial Data Service API | `0.1.0` | 3 | bearerAuth, dpopAuth |
| `compliance-context` | `api/openapi/compliance-context.yaml` | Compliance Context Service API | `1.0.0` | 2 | bearerAuth, dpopAuth |
| `confirmation-of-payee-service` | `api/openapi/confirmation-of-payee-service.yaml` | Confirmation of Payee Verification Service API | `0.1.0` | 1 | bearerAuth, dpopAuth |
| `consent-authorization-service` | `api/openapi/consent-authorization-service.yaml` | Consent and Authorization Service API | `0.1.0` | 5 | dpopAuth |
| `customer-context` | `api/openapi/customer-context.yaml` | Customer Context Service API | `1.0.0` | 5 | bearerAuth, dpopAuth |
| `loan-context` | `api/openapi/loan-context.yaml` | Loan Context Service API | `1.0.0` | 7 | bearerAuth, dpopAuth |
| `open-finance-context` | `api/openapi/open-finance-context.yaml` | Open Finance Context Service API | `0.1.0` | 0 | bearerAuth, dpopAuth |
| `open-products-service` | `api/openapi/open-products-service.yaml` | Open Products Catalog Service API | `0.1.0` | 1 | public |
| `payment-context` | `api/openapi/payment-context.yaml` | Payment Context Service API | `1.0.0` | 6 | bearerAuth, dpopAuth |
| `personal-financial-data-service` | `api/openapi/personal-financial-data-service.yaml` | Personal Financial Data Service API | `0.1.0` | 7 | bearerAuth, dpopAuth |
| `risk-context` | `api/openapi/risk-context.yaml` | Risk Context Service API | `1.0.0` | 2 | bearerAuth, dpopAuth |

## Endpoint Catalogue

## ATM Directory Service API (`atm-directory-service`)

- Contract: `api/openapi/atm-directory-service.yaml`
- Version: `0.1.0`
- Security schemes: public
- Server base URL: `https://api.example.com/open-finance/v1`

| Method | Path | Operation ID | Summary | Effective Security |
|---|---|---|---|---|
| `GET` | `/atms` | `listAtms` | List ATMs | public |

## Banking Metadata Enrichment Service API (`banking-metadata-service`)

- Contract: `api/openapi/banking-metadata-service.yaml`
- Version: `0.1.0`
- Security schemes: bearerAuth, dpopAuth
- Server base URL: `https://api.example.com/open-finance/v1`

| Method | Path | Operation ID | Summary | Effective Security |
|---|---|---|---|---|
| `GET` | `/metadata/accounts/{AccountId}` | `getAccountMetadata` | Retrieve account scheme metadata | bearerAuth, dpopAuth |
| `GET` | `/metadata/accounts/{AccountId}/parties` | `getPartyMetadata` | Retrieve party metadata | bearerAuth, dpopAuth |
| `GET` | `/metadata/accounts/{AccountId}/transactions` | `getTransactionMetadata` | Retrieve transaction metadata | bearerAuth, dpopAuth |
| `GET` | `/metadata/standing-orders` | `getStandingOrderMetadata` | Retrieve standing order metadata | bearerAuth, dpopAuth |

## Business Financial Data Service API (`business-financial-data-service`)

- Contract: `api/openapi/business-financial-data-service.yaml`
- Version: `0.1.0`
- Security schemes: bearerAuth, dpopAuth
- Server base URL: `https://api.example.com/open-finance/v1/corporate`

| Method | Path | Operation ID | Summary | Effective Security |
|---|---|---|---|---|
| `GET` | `/accounts` | `listCorporateAccounts` | List corporate accounts | bearerAuth, dpopAuth |
| `GET` | `/accounts/{masterAccountId}/balances` | `getCorporateBalances` | Retrieve balances | bearerAuth, dpopAuth |
| `GET` | `/transactions` | `getCorporateTransactions` | Retrieve transactions | bearerAuth, dpopAuth |

## Compliance Context Service API (`compliance-context`)

- Contract: `api/openapi/compliance-context.yaml`
- Version: `1.0.0`
- Security schemes: bearerAuth, dpopAuth
- Server base URL: `https://api.sandbox.openfinance.ae`

| Method | Path | Operation ID | Summary | Effective Security |
|---|---|---|---|---|
| `POST` | `/api/v1/compliance/screen` | `screenCompliance` | Run compliance screening | bearerAuth, dpopAuth |
| `GET` | `/api/v1/compliance/screenings/{transactionId}` | `findComplianceResult` | Find compliance result by transaction id | bearerAuth, dpopAuth |

## Confirmation of Payee Verification Service API (`confirmation-of-payee-service`)

- Contract: `api/openapi/confirmation-of-payee-service.yaml`
- Version: `0.1.0`
- Security schemes: bearerAuth, dpopAuth
- Server base URL: `https://api.example.com/open-finance/v1`

| Method | Path | Operation ID | Summary | Effective Security |
|---|---|---|---|---|
| `POST` | `/confirmation-of-payee/confirmation` | `confirmPayee` | Verify payee details | dpopAuth, bearerAuth |

## Consent and Authorization Service API (`consent-authorization-service`)

- Contract: `api/openapi/consent-authorization-service.yaml`
- Version: `0.1.0`
- Security schemes: dpopAuth
- Server base URL: `https://api.example.com/open-finance/v1`

| Method | Path | Operation ID | Summary | Effective Security |
|---|---|---|---|---|
| `POST` | `/consents` | `createConsent` | Create consent | dpopAuth |
| `GET` | `/consents` | `listConsents` | List consents by customer | dpopAuth |
| `GET` | `/consents/{ConsentId}` | `getConsent` | Retrieve consent | dpopAuth |
| `POST` | `/consents/{ConsentId}/authorize` | `authorizeConsent` | Authorize consent | dpopAuth |
| `PATCH` | `/consents/{ConsentId}/revoke` | `revokeConsent` | Revoke consent | dpopAuth |

## Customer Context Service API (`customer-context`)

- Contract: `api/openapi/customer-context.yaml`
- Version: `1.0.0`
- Security schemes: bearerAuth, dpopAuth
- Server base URL: `https://api.sandbox.openfinance.ae`

| Method | Path | Operation ID | Summary | Effective Security |
|---|---|---|---|---|
| `POST` | `/api/v1/customers` | `createCustomer` | Create customer | bearerAuth, dpopAuth |
| `GET` | `/api/v1/customers/{customerId}` | `getCustomer` | Get customer by id | bearerAuth, dpopAuth |
| `PUT` | `/api/v1/customers/{customerId}/credit-limit` | `updateCreditLimit` | Update customer credit limit | bearerAuth, dpopAuth |
| `POST` | `/api/v1/customers/{customerId}/credit/release` | `releaseCredit` | Release reserved credit | bearerAuth, dpopAuth |
| `POST` | `/api/v1/customers/{customerId}/credit/reserve` | `reserveCredit` | Reserve credit | bearerAuth, dpopAuth |

## Loan Context Service API (`loan-context`)

- Contract: `api/openapi/loan-context.yaml`
- Version: `1.0.0`
- Security schemes: bearerAuth, dpopAuth
- Server base URL: `https://api.sandbox.openfinance.ae`

| Method | Path | Operation ID | Summary | Effective Security |
|---|---|---|---|---|
| `POST` | `/api/v1/loans` | `createLoanApplication` | Create loan application | bearerAuth, dpopAuth |
| `GET` | `/api/v1/loans/{loanId}` | `getLoan` | Get loan by id | bearerAuth, dpopAuth |
| `POST` | `/api/v1/loans/{loanId}/approve` | `approveLoan` | Approve loan | bearerAuth, dpopAuth |
| `POST` | `/api/v1/loans/{loanId}/cancel` | `cancelLoan` | Cancel loan | bearerAuth, dpopAuth |
| `POST` | `/api/v1/loans/{loanId}/disburse` | `disburseLoan` | Disburse loan | bearerAuth, dpopAuth |
| `POST` | `/api/v1/loans/{loanId}/payments` | `makePayment` | Make loan payment | bearerAuth, dpopAuth |
| `POST` | `/api/v1/loans/{loanId}/reject` | `rejectLoan` | Reject loan | bearerAuth, dpopAuth |

## Open Finance Context Service API (`open-finance-context`)

- Contract: `api/openapi/open-finance-context.yaml`
- Version: `0.1.0`
- Security schemes: bearerAuth, dpopAuth
- Server base URL: `https://api.sandbox.openfinance.ae/open-finance`

No endpoints are currently declared in this contract.

## Open Products Catalog Service API (`open-products-service`)

- Contract: `api/openapi/open-products-service.yaml`
- Version: `0.1.0`
- Security schemes: public
- Server base URL: `https://api.example.com/open-finance/v1`

| Method | Path | Operation ID | Summary | Effective Security |
|---|---|---|---|---|
| `GET` | `/products` | `listProducts` | List products | public |

## Payment Context Service API (`payment-context`)

- Contract: `api/openapi/payment-context.yaml`
- Version: `1.0.0`
- Security schemes: bearerAuth, dpopAuth
- Server base URL: `https://api.sandbox.openfinance.ae`

| Method | Path | Operation ID | Summary | Effective Security |
|---|---|---|---|---|
| `POST` | `/api/v1/payments` | `processPayment` | Process payment | bearerAuth, dpopAuth |
| `GET` | `/api/v1/payments/{paymentId}` | `getPayment` | Get payment by id | bearerAuth, dpopAuth |
| `POST` | `/api/v1/payments/{paymentId}/cancel` | `cancelPayment` | Cancel payment | bearerAuth, dpopAuth |
| `POST` | `/api/v1/payments/{paymentId}/confirm` | `confirmPayment` | Confirm payment | bearerAuth, dpopAuth |
| `POST` | `/api/v1/payments/{paymentId}/fail` | `failPayment` | Mark payment as failed | bearerAuth, dpopAuth |
| `POST` | `/api/v1/payments/{paymentId}/refund` | `refundPayment` | Refund payment | bearerAuth, dpopAuth |

## Personal Financial Data Service API (`personal-financial-data-service`)

- Contract: `api/openapi/personal-financial-data-service.yaml`
- Version: `0.1.0`
- Security schemes: bearerAuth, dpopAuth
- Server base URL: `https://api.example.com/open-finance/v1`

| Method | Path | Operation ID | Summary | Effective Security |
|---|---|---|---|---|
| `GET` | `/accounts` | `listAccounts` | List accounts | bearerAuth, dpopAuth |
| `GET` | `/accounts/{AccountId}` | `getAccount` | Retrieve account | bearerAuth, dpopAuth |
| `GET` | `/accounts/{AccountId}/balances` | `getBalances` | Retrieve balances | bearerAuth, dpopAuth |
| `GET` | `/accounts/{AccountId}/beneficiaries` | `getBeneficiaries` | Retrieve beneficiaries | bearerAuth, dpopAuth |
| `GET` | `/accounts/{AccountId}/direct-debits` | `getDirectDebits` | Retrieve direct debits | bearerAuth, dpopAuth |
| `GET` | `/accounts/{AccountId}/standing-orders` | `getStandingOrders` | Retrieve standing orders | bearerAuth, dpopAuth |
| `GET` | `/accounts/{AccountId}/transactions` | `getTransactions` | Retrieve transactions | bearerAuth, dpopAuth |

## Risk Context Service API (`risk-context`)

- Contract: `api/openapi/risk-context.yaml`
- Version: `1.0.0`
- Security schemes: bearerAuth, dpopAuth
- Server base URL: `https://api.sandbox.openfinance.ae`

| Method | Path | Operation ID | Summary | Effective Security |
|---|---|---|---|---|
| `POST` | `/api/v1/risk/assess` | `assessRisk` | Assess transaction risk | bearerAuth, dpopAuth |
| `GET` | `/api/v1/risk/assessments/{transactionId}` | `findRiskAssessment` | Find risk assessment by transaction id | bearerAuth, dpopAuth |

## Security Coverage Summary

- Protected contracts (Bearer + DPoP): `banking-metadata-service`, `business-financial-data-service`, `confirmation-of-payee-service`, `personal-financial-data-service`, `consent-authorization-service`, and bounded-context APIs.
- Public contracts: `atm-directory-service`, `open-products-service` (intended open-data exposure).
- Mandatory controls for protected APIs: mTLS at gateway, OAuth2/OIDC access token validation, DPoP proof validation, scope checks, rate limiting, structured audit logs.

> If runtime routes differ from this catalogue, update the relevant OpenAPI file first and regenerate this document as part of the same change set.
