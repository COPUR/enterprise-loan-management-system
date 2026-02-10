variable "environment" {
  type        = string
  description = "Deployment environment"
}

variable "database_engine" {
  type        = string
  description = "Database engine"
}

variable "cache_engine" {
  type        = string
  description = "Cache engine"
}

variable "identity_provider_url" {
  type        = string
  description = "OIDC/IdP URL"
}

variable "observability_endpoint" {
  type        = string
  description = "Observability endpoint"
}
