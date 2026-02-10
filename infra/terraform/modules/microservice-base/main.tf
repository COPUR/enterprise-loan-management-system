// Terraform module stub for microservice infrastructure.
// Populate with provider resources (VPC, RDS, Redis, IAM, etc.) as needed.

output "service_info" {
  value = {
    name        = var.service_name
    slug        = var.service_slug
    environment = var.environment
  }
}
