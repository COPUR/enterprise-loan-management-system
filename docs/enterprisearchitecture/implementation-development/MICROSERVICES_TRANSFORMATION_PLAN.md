# Microservices Transformation Plan

## Objective
Transform the current monorepo implementation into **separate, deployable microservices** per use case, with automated CI/CD (GitHub Actions + Jenkins + GitLab), Terraform IaC stubs, and phase‑based delivery artifacts.

## Assumptions
- Each microservice will be a **separate repository**.
- CI/CD pipelines must support **Jenkins** and **GitLab** in addition to existing workflows.
- Terraform modules will be **provider-backed** (no output-only stubs) for each service.
- Each phase produces a **delivery artifact** committed to this repo.
- Java toolchain baseline is **Java 23** for all extracted services.
- **DPoP is mandatory** for all protected Open Finance endpoints (AIS, PIS, CoP, Metadata). Open Data services can remain tokenless or bearer-only if approved by security architecture.

## Scope
- In scope:
- FAPI security chain (JWT validation, scope enforcement, DPoP proof verification, mTLS/client cert binding checks).
- OpenAPI and implementation alignment with contract tests in CI.
- Production-ready persistence/cache adapters behind existing ports (MongoDB/Redis for AIS services).
- Distributed ETag/cache strategy with TTL and bounded growth.
- Standard observability baseline (trace id, metrics, structured logs with PII masking).
- Runnable Jenkins/GitLab pipelines and provider-backed Terraform base module.

- Out of scope for this plan revision:
- Business feature expansion not required to close guardrail/compliance gaps.
- Non-critical cosmetic refactors unrelated to security, contract, persistence, observability, or delivery.

## Wave Prioritization (Dependency-Driven)
See `MICROSERVICE_SERVICE_NOMENCLATURE.md` for full service names and repo slugs.

**Wave 0: Platform Guardrails**
- Shared FAPI security starter.
- Shared observability baseline.
- Runnable CI/CD templates (Jenkins + GitLab) with quality/security gates.
- Provider-backed Terraform `microservice-base` module.
- OpenAPI contract-test framework and breaking-change gate.

**Wave 1: Pilot Hardening (Business Financial Data Service)**
- Resolve OpenAPI drift:
- Contract paths and controller mappings must match.
- `DPoP` marked required in OpenAPI and enforced in runtime.
- Remove in-memory seeded runtime adapters and replace with production adapters behind ports.
- Move controller-local ETag cache to distributed TTL cache.
- Strengthen ETag hash input to include full response-significant fields.
- Re-enable security filter chain in integration tests.

**Wave 2: AIS Rollout**
- Apply Wave 1 pattern to Personal Financial Data Service and Banking Metadata Enrichment Service.

**Wave 3: Remaining Bounded Context Services**
- Roll out shared modules and gates to payment, risk, customer, compliance, loan, and open-finance context repos.

## Phase Plan & Deliverables

### Phase 1: Analysis & Governance
**Goal:** document current structure, dependencies, and guardrails.

**Deliverables**
- `transformation/phases/PHASE_1_ANALYSIS_AND_GOVERNANCE.md`
- `transformation/MICROSERVICES_REPO_AUDIT.md`

### Phase 2: Template & Repo Setup
**Goal:** define the golden template and repo creation workflow.

**Deliverables**
- `transformation/phases/PHASE_2_TEMPLATE_AND_REPO_SETUP.md`
- `templates/microservice/README.md`

### Phase 3: CI/CD & Automation
**Goal:** standard pipelines with Jenkins + GitLab support.

**Deliverables**
- `transformation/phases/PHASE_3_CICD_AUTOMATION.md`
- `ci/templates/microservice/Jenkinsfile`
- `ci/templates/microservice/gitlab-ci.yml`
- Pipeline quality gate implementation:
- contract tests against OpenAPI.
- coverage enforcement `>=85%`.
- SAST, dependency scan, secret scan.

### Phase 4: Infrastructure & Deployment
**Goal:** provider-backed Terraform modules + deployment topology.

**Deliverables**
- `transformation/phases/PHASE_4_TERRAFORM_AND_DEPLOYMENT.md`
- `infra/terraform/modules/microservice-base/*`
- `infra/terraform/services/*`
- Provider resources for:
- network/security primitives.
- database/cache backing services.
- IAM/secrets and observability sinks.

## Phase Gates
- **Gate A:** All Phase 1 deliverables completed and approved.
- **Gate B:** Template repo validated with a working service skeleton.
- **Gate C:** CI/CD templates are runnable and passing in a reference service repo.
- **Gate D:** Terraform module provisions real resources in a dev environment.
- **Gate E:** Pilot service passes mandatory compliance checks:
- DPoP proof validation enabled.
- OpenAPI contract drift eliminated.
- No in-memory runtime persistence adapters.
- Distributed bounded ETag/cache strategy in place.
