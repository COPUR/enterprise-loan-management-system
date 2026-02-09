# Test Suite: Account Information Services (AIS)
**Scope:** UC001 (Retail) & UC002 (Corporate)
**Actors:** TPP (AIS), PSU, ASPSP

## 1. Prerequisites
* Registered TPP with `accounts` scope.
* Valid PSU with active Current and Savings accounts.
* Consent created and authorized via OAuth2 flow.

## 2. Test Cases

### Suite A: Consent & Authorization
| ID | Test Case Description | Input Data | Expected Result | Type |
|----|-----------------------|------------|-----------------|------|
| **TC-AIS-001** | Create Account Consent (Happy Path) | `Permissions: ["ReadAccounts", "ReadBalances"]` | `201 Created`, `ConsentId` returned, Status: `AwaitingAuthorisation` | Functional |
| **TC-AIS-002** | Reject Consent with Invalid Permissions | `Permissions: ["ReadAdminData"]` | `400 Bad Request`, Error: `Invalid Permission` | Negative |
| **TC-AIS-003** | Access Data with Expired Consent | `ConsentId` (Expired) | `401 Unauthorized` or `403 Forbidden`, Error: `Consent Expired` | Negative |
| **TC-AIS-004** | Access Data with Revoked Token | `AccessToken` (Revoked) | `401 Unauthorized`, Error: `Token Invalid` | Security |

### Suite B: Account Retrieval
| ID | Test Case Description | Input Data | Expected Result | Type |
|----|-----------------------|------------|-----------------|------|
| **TC-AIS-005** | Get All Accounts | Valid Token | `200 OK`, List of linked accounts (IBAN, Currency, Type) | Functional |
| **TC-AIS-006** | Get Specific Account Details | `AccountId` (Valid) | `200 OK`, Account Scheme, Name, Status | Functional |
| **TC-AIS-007** | Get Account (BOLA Attack) | `AccountId` (Belonging to another user) | `403 Forbidden` or `404 Not Found` (Resource not linked to consent) | Security |

### Suite C: Balances & Transactions
| ID | Test Case Description | Input Data | Expected Result | Type |
|----|-----------------------|------------|-----------------|------|
| **TC-AIS-008** | Get Real-time Balances | `AccountId` | `200 OK`, `InterimAvailable` & `InterimBooked` match Core Banking | Functional |
| **TC-AIS-009** | Transaction History Filtering | `fromBookingDateTime=2024-01-01` | `200 OK`, Only transactions after date returned | Functional |
| **TC-AIS-010** | Transaction Pagination | Account with >100 txns | `200 OK`, Response includes `Links.Next` URL | Functional |
| **TC-AIS-011** | Pagination Performance | Follow `Next` Link | `200 OK`, Response Time < 500ms | NFR |
| **TC-AIS-012** | Corporate Multi-Currency | Corp Account (USD sub-account) | `200 OK`, Currency code `USD` returned correctly | Functional |

## 3. Automated Script Hints (Postman)
* **Pre-Request:** Generate JWT Client Assertion.
* **Test Script:**

```javascript
pm.test("Status code is 200", function () { pm.response.to.have.status(200); });
pm.test("TTLB < 500ms", function () { pm.expect(pm.response.responseTime).to.be.below(500); });
pm.test("Balance is number", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.Data.Balance[0].Amount.Amount).to.be.a('string'); // API returns string for currency
});
```
