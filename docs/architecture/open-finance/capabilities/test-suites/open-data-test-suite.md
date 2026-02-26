# Test Suite: Open Data (Products & ATMs)
**Scope:** Open Products Data (Products), ATM Open Data (ATMs)
**Actors:** TPP (Unauthenticated or Client Token), ASPSP

## 1. Prerequisites
* None (Public endpoints) or Client Credentials.

## 2. Test Cases

### Suite A: ATM Data (ATM Open Data)
| ID | Test Case Description | Input Data | Expected Result | Type |
|----|-----------------------|------------|-----------------|------|
| **TC-ATM-001** | Get All ATMs | `GET /atms` | `200 OK`, List of ATMs with geo-coordinates | Functional |
| **TC-ATM-002** | Filter by Location | `GET /atms?lat=...&long=...` | `200 OK`, Only ATMs within radius returned | Functional |
| **TC-ATM-003** | Verify ATM Status | `GET /atms` | Response includes `Accessibility`, `Services` (CashDeposit, etc.) | Data Quality |

### Suite B: Product Data (Open Products Data)
| ID | Test Case Description | Input Data | Expected Result | Type |
|----|-----------------------|------------|-----------------|------|
| **TC-PRD-001** | Get Personal Current Accounts | `GET /products?type=PCA` | `200 OK`, List of products with Fees/Rates | Functional |
| **TC-PRD-002** | Get SME Loans | `GET /products?segment=SME` | `200 OK`, List of SME lending products | Functional |
| **TC-PRD-003** | Response Time (Caching) | Repeated calls to `/products` | TTLB < 100ms (Cache Hit) | NFR |
