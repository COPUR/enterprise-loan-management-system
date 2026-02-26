# Microservice Service Nomenclature

This document defines verbose, capability-based service names, repo slugs, and extraction waves for the Open Finance transformation.

## Naming Rules
- **Service names** are descriptive and avoid legacy use‑case codes.
- **Repo slugs** use lowercase kebab‑case.
- **Runtime identifiers** use snake‑case (for metrics/logging prefixes).

## Service Map

| Service Name (Verbose) | Repo Slug | Runtime ID | Scope (Legacy Use Case Description) | Extraction Wave |
| --- | --- | --- | --- | --- |
| Consent and Authorization Service | openfinance-consent-authorization-service | consent_authorization | Consent Management | Wave 2 |
| Personal Financial Data Service | openfinance-personal-financial-data-service | personal_financial_data | Personal Financial Management (Retail Data) | Wave 2 |
| Business Financial Data Service | openfinance-business-financial-data-service | business_financial_data | Business Financial Management (Corporate Data) | Wave 2 |
| Confirmation of Payee Verification Service | openfinance-confirmation-of-payee-service | confirmation_of_payee | Confirmation of Payee | Wave 1 |
| Banking Metadata Enrichment Service | openfinance-banking-metadata-service | banking_metadata | Banking Metadata | Wave 2 |
| Corporate Treasury Data Service | openfinance-corporate-treasury-service | corporate_treasury | Corporate Treasury Data | Wave 3 |
| Payment Initiation and Settlement Service | openfinance-payment-initiation-service | payment_initiation | Single/Future/International Payments | Wave 4 |
| Recurring Payments and Mandates Service | openfinance-recurring-payments-service | recurring_payments | Variable Recurring Payments | Wave 3 |
| Bulk Payments Orchestration Service | openfinance-bulk-payments-service | bulk_payments | Corporate Bulk Payments | Wave 3 |
| Insurance Policy Data Service | openfinance-insurance-policy-data-service | insurance_policy_data | Insurance Data Sharing | Wave 4 |
| Insurance Quote and Binding Service | openfinance-insurance-quote-service | insurance_quote | Insurance Quote Initiation | Wave 4 |
| Foreign Exchange and Remittance Service | openfinance-fx-remittance-service | fx_remittance | FX & Remittance | Wave 4 |
| Dynamic Onboarding and eKYC Service | openfinance-dynamic-onboarding-service | dynamic_onboarding | Dynamic Onboarding for FX | Wave 4 |
| Request to Pay Orchestration Service | openfinance-request-to-pay-service | request_to_pay | Request to Pay | Wave 3 |
| Open Products Catalog Service | openfinance-open-products-service | open_products | Public Products Data | Wave 1 |
| ATM Directory Service | openfinance-atm-directory-service | atm_directory | ATM Open Data | Wave 1 |

## Extraction Wave Rationale

- **Wave 1 (Low risk, read‑heavy):** Confirmation of Payee, Open Products Catalog, ATM Directory.
- **Wave 2 (Read + consent coupling):** Personal/Business Financial Data, Banking Metadata, Consent and Authorization.
- **Wave 3 (Moderate risk, workflow‑heavy):** Request to Pay, Recurring Payments, Bulk Payments, Corporate Treasury.
- **Wave 4 (High risk, transactional):** Payment Initiation, FX/Remittance, Insurance Quote/Data, Dynamic Onboarding.
