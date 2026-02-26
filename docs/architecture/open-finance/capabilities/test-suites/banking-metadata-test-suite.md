# Test Suite: Banking Metadata
**Scope:** Banking Metadata
**Actors:** TPP, ASPSP

## 1. Prerequisites
* Valid Client or User Token.

## 2. Test Cases

### Suite A: Transaction Enrichment
| ID | Test Case Description | Input Data | Expected Result | Type |
|----|-----------------------|------------|-----------------|------|
| **TC-META-001** | Get Merchant Details | `GET /accounts/{id}/transactions` | Response includes `MerchantName`, `CategoryCode` (MCC), and `GeoLocation` | Functional |
| **TC-META-002** | Get Fees/Charges | Transaction with fee applied | Response separates `Amount` (Principal) and `ChargeAmount` | Functional |
| **TC-META-003** | Get FX Details | Int'l Transaction | Response includes `ExchangeRate`, `OriginalCurrency`, `TargetCurrency` | Functional |

### Suite B: Product & Party Metadata
| ID | Test Case Description | Input Data | Expected Result | Type |
|----|-----------------------|------------|-----------------|------|
| **TC-META-004** | Get Account Parties | `GET /accounts/{id}/parties` | Returns `FullLegalName`, `KYCStatus`, `RelationshipStartDate` | Functional |
| **TC-META-005** | Get Scheme Metadata | `GET /accounts/{id}` | Returns `SchemeName` (IBAN/BBAN) and `SecondaryIdentification` (if any) | Functional |
| **TC-META-006** | Get Standing Order Schedule | `GET /standing-orders` | Returns `Frequency` (e.g., `EvryMnth`), `FirstPaymentDate`, `FinalPaymentDate` | Functional |
