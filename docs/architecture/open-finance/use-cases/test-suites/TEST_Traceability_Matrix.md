# Requirement Traceability Matrix (RTM)
**Purpose:** Maps Use Cases (UC) to High-Level Design (HLD) components and Test Cases (TC).

| Use Case | HLD Component | API Endpoint | Test Suite | Primary Test Cases |
| :--- | :--- | :--- | :--- | :--- |
| **UC001** (PFM) | AIS Service / Mongo Read-Model | `GET /accounts` | `TEST_UC001_UC002` | `TC-AIS-001` to `TC-AIS-007` |
| **UC002** (Corporate) | Corporate Consent Engine | `GET /accounts` | `TEST_UC001_UC002` | `TC-AIS-012`, `TC-TRSY-001` |
| **UC003** (CoP) | CoP Fuzzy Matcher | `POST /confirmation` | `TEST_UC003` | `TC-COP-001` to `TC-COP-006` |
| **UC004** (Metadata) | Enrichment Service | `GET /parties` | `TEST_UC004` | `TC-META-001` to `TC-META-006` |
| **UC005** (Treasury) | Virtual Account Mgr | `GET /balances` | `TEST_UC005` | `TC-TRSY-001` to `TC-TRSY-006` |
| **UC006** (Payments) | Payment Orchestrator | `POST /payments` | `TEST_UC006_UC008` | `TC-PIS-001` to `TC-PIS-009` |
| **UC007** (VRP) | Mandate Engine | `POST /vrps` | `TEST_UC007` | `TC-VRP-001` to `TC-VRP-007` |
| **UC008** (Bulk) | Bulk File Gateway | `POST /file-payments` | `TEST_UC008` | `TC-BLK-001` to `TC-BLK-008` |
| **UC009** (Ins Data) | Policy ACL Adapter | `GET /policies` | `TEST_UC009_UC010` | `TC-INS-001` to `TC-INS-003` |
| **UC010** (Ins Quote) | Quote Engine | `POST /quotes` | `TEST_UC009_UC010` | `TC-QT-001` to `TC-QT-003` |
| **UC011** (FX Quote) | FX Streamer | `POST /fx-quotes` | `TEST_UC011_UC012` | `TC-FX-001` to `TC-FX-004` |
| **UC012** (Onboard) | eKYC Orchestrator | `POST /accounts` | `TEST_UC011_UC012` | `TC-ONB-001` to `TC-ONB-003` |
| **UC013** (RtP) | Notification Gateway | `POST /par` | `TEST_UC013` | `TC-RTP-001` to `TC-RTP-006` |
| **UC014** (Products) | Open Data Cache | `GET /products` | `TEST_UC014_UC015` | `TC-PRD-001` to `TC-PRD-003` |
| **UC015** (ATM) | Geo-Spatial DB | `GET /atms` | `TEST_UC014_UC015` | `TC-ATM-001` to `TC-ATM-003` |
