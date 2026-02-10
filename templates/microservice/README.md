# Microservice Template

This template provides the standard structure for each extracted microservice.

```
.
├── domain
├── application
├── infrastructure
└── tests
```

## Conventions
- Domain layer contains only business logic and ports (no framework dependencies).
- Application layer orchestrates use cases and depends only on domain ports.
- Infrastructure layer implements adapters (REST, persistence, messaging).

## CI/CD
Use pipeline templates from `ci/templates/microservice/`.

## Terraform
Use service stubs under `infra/terraform/services/<service-slug>`.

