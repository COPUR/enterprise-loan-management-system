# Microservices Transformation Plan

## Objective
Transform the current monorepo implementation into **separate, deployable microservices** per use case, with automated CI/CD (GitHub Actions + Jenkins + GitLab), Terraform IaC stubs, and phase‑based delivery artifacts.

## Assumptions
- Each microservice will be a **separate repository**.
- CI/CD pipelines must support **Jenkins** and **GitLab** in addition to existing workflows.
- Terraform stubs will be created for each service.
- Each phase produces a **delivery artifact** committed to this repo.

## Service Prioritization (Waves)
See `MICROSERVICE_SERVICE_NOMENCLATURE.md` for full service names and repo slugs.

**Wave 1 (Read‑heavy, low risk)**
- Confirmation of Payee Verification Service
- Open Products Catalog Service
- ATM Directory Service

**Wave 2 (Consent‑coupled read services)**
- Consent and Authorization Service
- Personal Financial Data Service
- Business Financial Data Service
- Banking Metadata Enrichment Service

**Wave 3 (Workflow‑heavy)**
- Request to Pay Orchestration Service
- Recurring Payments and Mandates Service
- Bulk Payments Orchestration Service
- Corporate Treasury Data Service

**Wave 4 (Transaction‑critical)**
- Payment Initiation and Settlement Service
- Foreign Exchange and Remittance Service
- Insurance Policy Data Service
- Insurance Quote and Binding Service
- Dynamic Onboarding and eKYC Service

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

### Phase 4: Infrastructure & Deployment
**Goal:** Terraform stubs + deployment topology.

**Deliverables**
- `transformation/phases/PHASE_4_TERRAFORM_AND_DEPLOYMENT.md`
- `infra/terraform/modules/microservice-base/*`
- `infra/terraform/services/*`

## Phase Gates
- **Gate A:** All Phase 1 deliverables completed and approved.
- **Gate B:** Template repo validated with a working service skeleton.
- **Gate C:** CI/CD templates passing in a reference service repo.
- **Gate D:** Terraform stubs reviewed and environment variables defined.

