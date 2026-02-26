# Requirement Traceability Matrix (RTM)
**Purpose:** Maps business capabilities to High-Level Design (HLD) components and Test Cases (TC).

| Use Case | HLD Component | API Endpoint | Test Suite | Primary Test Cases |
| :--- | :--- | :--- | :--- | :--- |
| **Personal Financial Management** (PFM) | AIS Service / Mongo Read-Model | `GET /accounts` | `ACCOUNT_INFORMATION_TEST_SUITE` | `TC-AIS-001` to `TC-AIS-007` |
| **Business Financial Management** (Corporate) | Corporate Consent Engine | `GET /accounts` | `ACCOUNT_INFORMATION_TEST_SUITE` | `TC-AIS-012`, `TC-TRSY-001` |
| **Confirmation of Payee** (CoP) | CoP Fuzzy Matcher | `POST /confirmation` | `CONFIRMATION_OF_PAYEE_TEST_SUITE` | `TC-COP-001` to `TC-COP-006` |
| **Banking Metadata** (Metadata) | Enrichment Service | `GET /parties` | `BANKING_METADATA_TEST_SUITE` | `TC-META-001` to `TC-META-006` |
| **Corporate Treasury Data** (Treasury) | Virtual Account Mgr | `GET /balances` | `CORPORATE_TREASURY_TEST_SUITE` | `TC-TRSY-001` to `TC-TRSY-006` |
| **Payment Initiation** (Payments) | Payment Orchestrator | `POST /payments` | `PAYMENTS_AND_BULK_PAYMENTS_TEST_SUITE` | `TC-PIS-001` to `TC-PIS-009` |
| **Recurring Payments** (VRP) | Mandate Engine | `POST /vrps` | `RECURRING_PAYMENTS_TEST_SUITE` | `TC-VRP-001` to `TC-VRP-007` |
| **Corporate Bulk Payments** (Bulk) | Bulk File Gateway | `POST /file-payments` | `CORPORATE_BULK_PAYMENTS_TEST_SUITE` | `TC-BLK-001` to `TC-BLK-008` |
| **Insurance Data Sharing** (Ins Data) | Policy ACL Adapter | `GET /policies` | `INSURANCE_SERVICES_TEST_SUITE` | `TC-INS-001` to `TC-INS-003` |
| **Insurance Quote Initiation** (Ins Quote) | Quote Engine | `POST /quotes` | `INSURANCE_SERVICES_TEST_SUITE` | `TC-QT-001` to `TC-QT-003` |
| **FX and Remittance Services** (FX Quote) | FX Streamer | `POST /fx-quotes` | `FX_AND_ONBOARDING_TEST_SUITE` | `TC-FX-001` to `TC-FX-004` |
| **Dynamic Onboarding** (Onboard) | eKYC Orchestrator | `POST /accounts` | `FX_AND_ONBOARDING_TEST_SUITE` | `TC-ONB-001` to `TC-ONB-003` |
| **Request to Pay** (RtP) | Notification Gateway | `POST /par` | `REQUEST_TO_PAY_TEST_SUITE` | `TC-RTP-001` to `TC-RTP-006` |
| **Open Products Data** (Products) | Open Data Cache | `GET /products` | `OPEN_DATA_TEST_SUITE` | `TC-PRD-001` to `TC-PRD-003` |
| **ATM Open Data** (ATM) | Geo-Spatial DB | `GET /atms` | `OPEN_DATA_TEST_SUITE` | `TC-ATM-001` to `TC-ATM-003` |
