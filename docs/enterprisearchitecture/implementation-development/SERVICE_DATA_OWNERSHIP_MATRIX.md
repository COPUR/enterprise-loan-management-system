# Service Data Ownership Matrix

This matrix formalizes **service‑level data ownership** prior to extraction. Each service owns its database schema and exposes data **only** via API contracts or events. No cross‑service direct DB access is permitted.

## Ownership Rules
- **Single owner** per data set (schema/database).
- **Access** via REST/async events only.
- **No shared database tables** across services.

## Ownership Matrix

| Bounded Context Service | Owned Data Stores | Primary Aggregates | Access Policy |
| --- | --- | --- | --- |
| Open Finance Context Service | `open_finance_*` schemas (consent, AIS read models, idempotency, audit) | Consent, Account Read Models, Payment Intent | API + events only; no direct DB access from other contexts |
| Payment Context Service | `payment_*` schemas (payment transactions, idempotency) | Payment, Funds Reservation | API + events only |
| Loan Context Service | `loan_*` schemas (loan accounts, schedules, amortization) | LoanAccount, LoanSchedule | API + events only |
| Risk Context Service | `risk_*` schemas (risk rules, decision logs) | RiskAssessment, RiskDecision | API + events only |
| Customer Context Service | `customer_*` schemas (profile, KYC state, identifiers) | CustomerProfile, KycStatus | API + events only |
| Compliance Context Service | `compliance_*` schemas (audit, dispute evidence) | AuditRecord, LiabilityEvidence | API + events only |

## Notes
- Shared libraries (`shared-kernel`, `common-domain`) must remain **code‑level only** and must not imply shared persistence.
- Cross‑context access must use explicit OpenAPI contracts and event topics.

