# Test Suite: Corporate Treasury Data
**Scope:** Corporate Treasury Data (Corporate Treasury)
**Actors:** Corporate TPP (TMS/ERP), Corporate PSU (Treasurer), ASPSP

## 1. Prerequisites
* Corporate Consent with `ReadAccounts`, `ReadBalances`, `ReadTransactions` permissions.
* Configuration for Virtual Accounts (vIBANs) and Sweeping structures exists on the LFI side.

## 2. Test Cases

### Suite A: Virtual Account Management (VAM)
| ID | Test Case Description | Input Data | Expected Result | Type |
|----|-----------------------|------------|-----------------|------|
| **TC-TRSY-001** | Get Virtual Accounts | `GET /accounts` (with corporate consent) | `200 OK`, Response includes Master Account AND associated vIBANs | Functional |
| **TC-TRSY-002** | Filter by Master IBAN | `GET /accounts?masterAccountId={ID}` | `200 OK`, Returns only vIBANs linked to that master | Functional |
| **TC-TRSY-003** | Get Real-Time Liquidity | `GET /balances` for a Master Account | `200 OK`, Returns aggregated balance of all underlying vIBANs | Data Integrity |

### Suite B: Sweeping & Pooling
| ID | Test Case Description | Input Data | Expected Result | Type |
|----|-----------------------|------------|-----------------|------|
| **TC-TRSY-004** | Identify Sweeping Txns | `GET /transactions` | Response contains `TransactionCode: SWEEP` or `ProprietaryCode: ZBA` | Functional |
| **TC-TRSY-005** | Cross-Border Visibility | `GET /accounts` (Multi-jurisdiction) | `200 OK`, Returns accounts from UAE and potentially other branches (if Global One view enabled) | Functional |
| **TC-TRSY-006** | Data Freshness (High Vol) | Trigger 500 txns, then `GET /balances` | `200 OK`, Balance updates within < 5 seconds of core posting | NFR |

### Suite C: Corporate Entitlements
| ID | Test Case Description | Input Data | Expected Result | Type |
|----|-----------------------|------------|-----------------|------|
| **TC-TRSY-007** | Restricted User Access | Token for "Junior Accountant" | `GET /accounts` returns masked balances or restricted list | Security |
| **TC-TRSY-008** | Unauthorized Division Access | Access Division B accounts with Division A token | `403 Forbidden` | Security |
