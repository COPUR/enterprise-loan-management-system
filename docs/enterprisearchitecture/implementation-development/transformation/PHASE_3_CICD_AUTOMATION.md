# Phase 3: CI/CD Automation Deliverable

## Purpose
Provide standardized Jenkins and GitLab pipelines for all microservice repos.

## Status
- [ ] Not Started
- [ ] In Progress
- [x] Delivered

## Deliverables
- `ci/templates/microservice/Jenkinsfile`
- `ci/templates/microservice/gitlab-ci.yml`
- Pipeline stages: lint, unit/integration tests, coverage gate, security scan, build, sign, publish
- Reference service wired with CI templates:
  - `services/openfinance-confirmation-of-payee-service/Jenkinsfile`
  - `services/openfinance-confirmation-of-payee-service/.gitlab-ci.yml`
