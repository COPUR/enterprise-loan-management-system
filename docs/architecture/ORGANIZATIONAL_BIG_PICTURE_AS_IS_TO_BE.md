# Organizational Big Picture: As-Is vs To-Be

## Purpose

Provide an executive and engineering-aligned organizational architecture view for the Open Finance transformation, with explicit operating-model changes required to support centralized AAA (Keycloak preferred), service mesh security, and continuous compliance.

## Artifacts

- As-Is organizational model:
  - `docs/puml/service-mesh/organizational-as-is-big-picture.puml`
  - `docs/puml/service-mesh/organizational-as-is-big-picture.png`
  - `docs/puml/service-mesh/organizational-as-is-big-picture.svg`
- To-Be organizational model:
  - `docs/puml/service-mesh/organizational-to-be-big-picture.puml`
  - `docs/puml/service-mesh/organizational-to-be-big-picture.png`
  - `docs/puml/service-mesh/organizational-to-be-big-picture.svg`

## As-Is Summary

1. Teams are largely organized in domain silos with uneven platform reuse.
2. Identity and authorization controls are split across gateway and service code.
3. Governance is often project-gated rather than continuous.
4. Compliance evidence is produced late and mostly manually.
5. Operational model is reactive (incident-first vs policy/SLO-first).

## To-Be Summary

1. Domain product streams remain autonomous but consume shared platform capabilities.
2. AAA is centralized in Keycloak/LDAP with distributed policy enforcement points.
3. Security/compliance controls become policy-as-code and pipeline-enforced.
4. SRE and observability are standardized and measurable across all services.
5. Architecture, security, and portfolio governance shift to continuous cadence.

## Organizational Design Principles

1. Product autonomy with platform guardrails.
2. Centralize identity, decentralize decision enforcement.
3. Make compliance continuous and evidence-driven.
4. Treat security and reliability as first-class product requirements.
5. Standardize delivery controls while preserving domain ownership.

## Target Team Topology

### Domain Streams

- Open Finance Stream
- Lending Stream
- Payments Stream

### Platform Teams

- Identity Platform Team (Keycloak, LDAP federation, token policy)
- Mesh & Runtime Security Team (mTLS, authz policy, ingress/egress controls)
- DevSecOps Platform Team (CI/CD gates, supply chain controls, IaC governance)
- Observability Platform Team (OTel, SIEM, SLO tooling)

### Governance Bodies

- Architecture Board (ADR and runway ownership)
- Security & Compliance Council (control stewardship)
- Value Stream Governance (funding and outcome ownership)

## Transition Impacts

| Area | As-Is | To-Be | Impact |
| --- | --- | --- | --- |
| Identity ownership | Mixed across teams | Central identity platform | Reduced drift and duplicated logic |
| Authorization | Service-specific implementation | Distributed policy agents + mesh policies | Stronger consistency and auditability |
| DPoP | Partial/inconsistent | Standardized at gateway + policy layer | Strong replay/token-binding controls |
| Release governance | CAB-heavy/manual | Policy and quality gates in CI/CD | Faster and safer delivery |
| Audit readiness | Periodic/manual | Continuous evidence pipeline | Better regulatory posture |

## Success Criteria

1. All critical service streams use shared AAA and policy platforms.
2. Security controls are codified and tested in every release.
3. Evidence for liability/dispute scenarios is queryable by design.
4. Platform and domain teams share explicit operating contracts.

