# Master Certification Checklist
**Purpose:** Final verification before Production Go-Live.
**Audience:** LFI Implementation Team, QA Manager, CISO.
**Ref:** *Open Finance Platform Assurance*, *API Hub LFI Implementation Plan*

## 1. Functional Compliance (The "Happy Paths")
- [ ] **Account Mapping:** Verified that ALL account types (Current, Savings, Credit Cards, Corporate) are correctly mapped to Open Finance standards.
- [ ] **Data Integrity:** Spot checked 50 random transactions; `Amount`, `Date`, and `Description` match Core Banking exactly.
- [ ] **Payment Execution:** Successful end-to-end test of a Single Payment debiting a real account and crediting a beneficiary.
- [ ] **Corporate Workflows:** Verified that a corporate payment requiring 2 signatures holds status `PendingAuthorisation` until the second approval.
- [ ] **Consent Revocation:** Verified that revoking consent in the Banking App immediately blocks TPP access.

## 2. Security & NFRs (The "Guardrails")
- [ ] **Performance SLA:** Load test passed at **100 TPS** (or agreed volume) with **< 500ms** latency (95th percentile).
- [ ] **Penetration Test:** Third-party pen-test report received; all "Critical" and "High" vulnerabilities remediated and re-tested.
- [ ] **Idempotency:** Verified that network retries (replaying the same `x-idempotency-key`) do NOT create duplicate transactions.
- [ ] **Data Privacy:** Verified PII (IBAN, Customer Name) is **masked** in all application logs (Splunk/ELK).
- [ ] **mTLS:** Confirmed that the API Gateway rejects any connection not using a valid Directory-issued certificate.

## 3. Operational Readiness (The "Run" Phase)
- [ ] **Monitoring:** Dashboards set up to track "Error Rate" (Golden Signal), "Latency", and "Traffic Volume". Alerting thresholds configured.
- [ ] **Support:** Service Desk accounts created for LFI Ops team to handle TPP disputes.
- [ ] **Audit Logs:** Confirmed that `payment_audit` logs are immutable and accessible to Compliance for liability checks.
- [ ] **Disaster Recovery:** DR Failover test executed successfully; RTO/RPO objectives met.

## 4. Final Sign-off
| Role | Name | Signature | Date |
|------|------|-----------|------|
| **Head of Engineering** | ________________ | _________ | ____ |
| **CISO** | ________________ | _________ | ____ |
| **Product Owner** | ________________ | _________ | ____ |
| **Compliance Officer** | ________________ | _________ | ____ |
