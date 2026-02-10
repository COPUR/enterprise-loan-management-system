# Master Task List: Open Finance Repository & Pipeline Strategy

**Objective:** Transition from monolithic analysis to decentralized, deployable microservices for each Open Finance Use Case (e.g., UC001, UC006, UC003), fully automated via CI/CD.
**Policy Update:** `DPoP` is mandatory for protected Open Finance endpoints.

---

## Universal Task List (Wave Execution Order)

- [ ] Wave 0 Platform Guardrails:
- [ ] Implement shared FAPI chain (JWT validation, scope enforcement, mandatory DPoP proof verification, mTLS binding checks).
- [ ] Re-enable security filter chain in integration/functional tests for protected APIs.
- [ ] Replace placeholder Jenkins/GitLab steps with executable quality/security gates.
- [ ] Replace Terraform output-only stubs with provider-backed baseline resources.
- [ ] Implement observability baseline in runtime code (traces, metrics, structured logs, PII masking).

- [ ] Wave 1 Pilot Hardening (Business Financial Data Service):
- [x] Resolve OpenAPI and controller path/header drift.
- [x] Implement runtime FAPI security chain (JWT + scope checks + DPoP proof verification) for Business Financial Data Service.
- [x] Re-enable security filter chain in Business Financial Data Service integration/functional tests with signed JWT + DPoP proofs.
- [x] Mark `DPoP` required in Business Financial Data Service OpenAPI contract for protected operations.
- [x] Replace production in-memory persistence adapters with durable DB/cache adapters.
- [x] Move ETag/state cache from local memory maps to distributed TTL cache.
- [x] Strengthen ETag hash material to include full response-significant fields.
- [x] Add CI contract tests to block future drift.

- [ ] Wave 2 AIS Rollout:
- [ ] Apply Wave 1 hardening pattern to Personal Financial Data Service.
- [ ] Apply Wave 1 hardening pattern to Banking Metadata Service.

- [ ] Wave 3 Context Rollout:
- [ ] Roll out shared security/observability/pipeline/IaC standards to remaining bounded contexts.

---

## Phase 1: Analysis & Governance (Current State & Guardrails)

*Goal: Audit the existing codebase/documentation and codify the NFRs into automated checks.*

### 1.1 Repository & Architecture Audit

- [ ] Analyze Current Repo Structure: review the existing monolithic repository or documentation store. Identify tightly coupled components (e.g., shared database models between Payment and Account domains) that must be decoupled.
- [ ] Catalog Existing Assets: list all current API contracts (Swagger/OpenAPI), shared libraries, and utility classes.
- [ ] Dependency Graphing: map dependencies between use cases (e.g., does Payment Initiation rely on Account Information DB tables?).
- [ ] Debt Identification: tag any code violating share‑nothing architecture (e.g., direct DB access across domains).

### 1.2 Guardrail Analysis & Codification

- [ ] Security Guardrails Review:
- [ ] Verify FAPI Compliance requirements (mTLS enforcement, detached JWS signatures, JWT validation, scope checks).
- [ ] Enforce `DPoP` as mandatory in protected API contracts and runtime security chain.
- [ ] Define PII Masking rules (log masking for IBAN, names).

- [ ] Performance Guardrails Review:
- [ ] Confirm SLA targets (500ms TTLB for APIs, 250ms for LFI backend).
- [ ] Define Rate Limiting policies per TPP.

- [ ] Linting & Quality Gate Definition:
- [ ] Create a ruleset for static code analysis (SonarQube) to enforce hexagonal architecture (domain layer must not import infrastructure).
- [ ] Define minimum code coverage (target: 85%).
- [ ] Add OpenAPI contract drift gate in CI (fail on spec/implementation mismatch).

---

## Phase 2: Microservice Creation (Per Use Case)

*Goal: Create isolated, deployable repositories for each Use Case, strictly following TDD & DDD.*

### 2.1 Repository Setup (Template Strategy)

- [ ] Create Microservice Template Repo: build a golden template repository containing:
- [ ] Folder Structure: `domain`, `application`, `infrastructure`, `tests`.
- [ ] Dockerfile: multi‑stage build optimized for production (distroless/alpine).
- [ ] Pre‑commit Hooks: husky/githooks for linting and unit test execution.
- [ ] Helm Chart: base template for Kubernetes deployment.

- [ ] Initialize Use Case Repos: fork the template for each targeted use case:
- [ ] `repo-uc001-account-info`
- [ ] `repo-uc006-payment-initiation`
- [ ] `repo-uc003-confirmation-payee`

### 2.2 Domain Implementation (Iterative per Repo)

- [ ] Domain Modeling (DDD):
- [ ] Implement aggregates and entities (e.g., `PaymentConsent`, `AccountStatement`) with pure logic.
- [ ] Implement value objects (e.g., `Money`, `IBAN`) with validation rules.

- [ ] TDD - Unit Testing:
- [ ] Write failing tests for domain logic (business rules).
- [ ] Implement logic to pass tests (red‑green‑refactor).

- [ ] Ports Definition:
- [ ] Define input ports (use case interfaces).
- [ ] Define output ports (repository interfaces, external service interfaces).

### 2.3 Adapter Implementation (Hexagonal)

- [ ] Infrastructure Adapters:
- [ ] Implement persistence adapter (MongoDB/PostgreSQL) with 3NF/document design.
- [ ] Implement external adapter (core banking connector) with circuit breakers.
- [ ] Replace all production-wired in-memory seeded adapters with persistent adapters; keep in-memory adapters test-only.

- [ ] API Adapter (Web):
- [ ] Implement REST controllers matching the OpenAPI specification.
- [ ] Implement idempotency shim (Redis check for `x-idempotency-key`).
- [ ] Implement exception handling (map domain errors to standard HTTP 4xx/5xx responses).
- [ ] Resolve Corporate AIS endpoint/path drift between OpenAPI and controller mappings.
- [ ] Enforce mandatory `DPoP` request handling (`required: true` in spec for protected APIs).
- [ ] Implement distributed ETag/state cache with TTL and bounded key space (no unbounded local maps).
- [ ] Strengthen ETag hashing input to include all response-significant fields.

### 2.4 Configuration & 12‑Factor

- [ ] Externalize Config: replace hardcoded values with environment variables (`DB_HOST`, `REDIS_URL`, `IDP_URL`).
- [ ] Secret Management: integrate with Vault/Secrets Manager (do not commit secrets).

---

## Phase 3: Pipelines & Automation (CI/CD)

*Goal: Automate the path from `git push` to Production deployment.*

### 3.1 Continuous Integration (CI) Pipeline

*Trigger: Pull Request or Push to Branch*

- [ ] Lint & Format Job:
- [ ] Run linter (ESLint/Checkstyle).
- [ ] Check commit message convention (Conventional Commits).

- [ ] Test & Coverage Job:
- [ ] Run unit tests.
- [ ] Run integration tests (with ephemeral DB containers).
- [ ] Re-enable real security filter chain in integration tests (no `addFilters=false` bypass for protected APIs).
- [ ] Generate coverage report. Fail pipeline if coverage < 85%.

- [ ] Security Scan Job:
- [ ] SAST: scan source code for vulnerabilities (SonarQube/Snyk).
- [ ] Dependency Check: scan libraries for CVEs (OWASP Dependency Check).
- [ ] Secret Detection: scan git history for leaked credentials (TruffleHog).

- [ ] Build & Publish Job:
- [ ] Build Docker image.
- [ ] Sign image (Cosign/Notary) for supply chain security.
- [ ] Push to container registry with immutable tags (e.g., `sha-xyz`).

### 3.2 Continuous Deployment (CD) Pipeline

*Trigger: Merge to Main / Release Tag*

- [ ] Infrastructure as Code (IaC) Provisioning:
- [ ] Terraform/Crossplane to provision RDS, ElastiCache (Redis), and IAM roles per microservice.
- [ ] Replace output-only Terraform module stubs with provider-backed resources (network, security, compute, data, observability).

- [ ] Deployment Strategy:
- [ ] Deploy Helm chart to dev/staging namespace.
- [ ] Run smoke tests (health check endpoint).

- [ ] E2E Testing Job:
- [ ] Run Postman/Newman collection against staging.
- [ ] Verify happy paths (payment success) and negative paths (idempotency check).

- [ ] Production Promotion:
- [ ] Manual approval gate for production.
- [ ] Blue/green or canary deployment rollout strategy.

### 3.3 Observability Automation

- [ ] Dashboard Provisioning: auto‑create Grafana dashboards for each microservice (latency, traffic, errors, saturation).
- [ ] Alerting Rules: auto‑configure Prometheus alerts (e.g., error rate > 1%).
- [ ] Log Aggregation: ensure JSON logs shipped to ELK/Splunk with PII masking active.
- [ ] Tracing Baseline: propagate and log trace id with `X-FAPI-Interaction-ID` correlation.
- [ ] Metrics Baseline: expose request latency, throughput, cache hit ratio, and error counters per endpoint.

---

## Mandatory Priority Fixes (Immediate Backlog)

- [ ] Corporate AIS OpenAPI vs implementation reconciliation:
- [ ] Align `/accounts`, `/balances`, `/transactions`, `/scheduled-payments`, `/parties` paths to final contract.
- [ ] Align controller mappings and generated links to same base path.
- [ ] Set `DPoP` to required in business financial data OpenAPI for protected operations.

- [ ] Corporate AIS production-readiness:
- [x] Replace `InMemoryCorporateConsentAdapter` runtime usage with persistent adapter.
- [x] Replace `InMemoryCorporateAccountReadAdapter` runtime usage with persistent adapter.
- [x] Move controller ETag map to Redis-backed TTL cache.
- [x] Improve ETag signature generation to include full response-significant payload.

- [ ] Cross-service baseline upgrades:
- [ ] Add security starter and enforce in integration tests.
- [ ] Add observability starter (trace/metrics/logging).
- [ ] Replace Jenkins/GitLab placeholder steps with runnable gates.
- [ ] Replace Terraform output-only module with real provider resources.
