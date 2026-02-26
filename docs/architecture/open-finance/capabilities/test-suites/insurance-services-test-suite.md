# Test Suite: Insurance Services
**Scope:** Insurance Data Sharing (Data), Insurance Quote Initiation (Quotes)
**Actors:** TPP, Insurer

## 2. Test Cases

### Suite A: Insurance Data
| ID | Test Case Description | Input Data | Expected Result | Type |
|----|-----------------------|------------|-----------------|------|
| **TC-INS-001** | Get Motor Policies | Consent: `ReadPolicies` | `200 OK`, List of active motor policies | Functional |
| **TC-INS-002** | Get Policy Details | `PolicyId` | `200 OK`, Coverage details, Premium, Expiry | Functional |
| **TC-INS-003** | Data Schema Validation | -- | Response validates against Open Insurance JSON Schema (Strict types) | Functional |

### Suite B: Insurance Quotes
| ID | Test Case Description | Input Data | Expected Result | Type |
|----|-----------------------|------------|-----------------|------|
| **TC-QT-001** | Request Motor Quote | Car Details, Driver Age | `201 Created`, `QuoteId`, Premium Amount | Functional |
| **TC-QT-002** | Bind Policy (Purchase) | `QuoteId`, Payment Ref | `200 OK`, `PolicyId` created, Certificate generated | Functional |
| **TC-QT-003** | Quote Parameter Manipulation | Accept Quote with changed vehicle data | `400 Bad Request` (Quote bound to original inputs) | Security |
