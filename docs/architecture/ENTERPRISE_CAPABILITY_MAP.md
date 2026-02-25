# Enterprise Capability Map (As-Is to To-Be)

## Purpose

Define the target capability landscape for enterprise banking and Open Finance transformation, including current maturity, target maturity, and delivery alignment.

## Capability Map Diagram

- Source: `docs/puml/service-mesh/enterprise-capability-map.puml`
- Rendered: `docs/puml/service-mesh/enterprise-capability-map.png`
- Rendered: `docs/puml/service-mesh/enterprise-capability-map.svg`

## L1 Capability Domains

### Business Capabilities

1. Customer & Identity Management
2. Open Finance Data Sharing
3. Payment Initiation & Settlement
4. Lending & Credit Lifecycle
5. Compliance & Risk Control

### Enabling Capabilities

1. Identity & Access Platform (Keycloak + LDAP + DPoP)
2. API & Integration Platform (Gateway + FAPI + PAR)
3. Service Mesh Security Runtime
4. Data Platform & Governance
5. Observability & SRE
6. DevSecOps & Supply Chain

### Governance Capabilities

1. Architecture Governance
2. Policy-as-Code Governance
3. Audit, Liability & Evidence
4. Portfolio & Backlog Governance

## Maturity Baseline and Targets

| Capability | As-Is (1-5) | To-Be Target (1-5) | Key Improvement Theme |
| --- | --- | --- | --- |
| Customer & Identity Management | 2.5 | 4.2 | Centralize IAM and token contracts |
| Open Finance Data Sharing | 2.8 | 4.0 | Standard contracts + policy enforcement |
| Payment Initiation & Settlement | 2.6 | 4.1 | Strong consistency + replay-safe controls |
| Lending & Credit Lifecycle | 3.0 | 4.0 | Domain autonomy + platform integration |
| Compliance & Risk Control | 2.4 | 4.3 | Continuous controls and evidence |
| Identity & Access Platform | 2.0 | 4.4 | Keycloak/LDAP + DPoP platformization |
| API & Integration Platform | 3.0 | 4.2 | FAPI hardening and gateway governance |
| Service Mesh Security Runtime | 1.8 | 4.0 | mTLS/authz/egress standardization |
| Data Platform & Governance | 2.7 | 4.0 | Ownership contracts + quality controls |
| Observability & SRE | 2.3 | 4.1 | OTel baseline + SLO operations |
| DevSecOps & Supply Chain | 2.5 | 4.2 | Signed artifacts + policy gates |
| Architecture Governance | 2.6 | 4.1 | ADR-driven runway governance |
| Policy-as-Code Governance | 1.9 | 4.2 | Unified policy lifecycle |
| Audit, Liability & Evidence | 2.1 | 4.4 | Immutable event evidence |
| Portfolio & Backlog Governance | 2.8 | 4.0 | Capability-funded planning model |

## Capability Ownership Model

| Capability Group | Primary Owner | Supporting Owner |
| --- | --- | --- |
| Business capabilities | Domain product streams | Enterprise architecture |
| Enabling capabilities | Platform engineering | Security and SRE |
| Governance capabilities | Architecture/Security councils | Product and compliance |

## Quarterly Capability Review Process

1. Score each capability against Process, Automation, Security, Reliability, and Compliance dimensions.
2. Validate score with objective metrics (test gates, SLOs, incident data, audit outcomes).
3. Convert gaps into epics and prioritized backlog entries.
4. Re-baseline targets quarterly with architecture board approval.

