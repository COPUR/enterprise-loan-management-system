# Terraform Infrastructure for Open Finance Microservices

This directory contains provider-backed Terraform stacks for each Open Finance microservice.

## Structure

- `modules/microservice-base`
  - Shared AWS baseline resources for each service:
  - CloudWatch log group
  - SSM parameters for runtime configuration pointers
  - Secrets Manager bootstrap secret
  - IAM workload role
- `services/<service-slug>`
  - Service-specific root module wiring with explicit AWS provider config
  - Required deployment inputs (`environment`, engines, identity/observability endpoints)
  - Service outputs for downstream deployment automation

## Prerequisites

- Terraform `>= 1.3.0`
- AWS credentials configured in environment/profile
- Region provided via stack variable `aws_region` (defaults to `us-east-1`)

## Example

```bash
cd infra/terraform/services/business-financial-data-service
terraform init
terraform fmt -check
terraform validate
terraform plan \
  -var="aws_region=eu-west-1" \
  -var="environment=dev" \
  -var="database_engine=mongodb" \
  -var="cache_engine=redis" \
  -var="identity_provider_url=https://idp.dev.openfinance.local" \
  -var="observability_endpoint=https://otel.dev.openfinance.local"
```

## Notes

- These stacks provide baseline shared resources only.
- Network, compute runtime, managed database/cache instances, and service mesh wiring should be layered via environment-specific IaC stacks.
