variable "service_name" {
  type        = string
  description = "Verbose service name"
}

variable "service_slug" {
  type        = string
  description = "Kebab-case service slug"
}

variable "environment" {
  type        = string
  description = "Deployment environment (dev/stage/prod)"
}

variable "database_engine" {
  type        = string
  description = "Database engine (postgres, mongo, etc)"
}

variable "cache_engine" {
  type        = string
  description = "Cache engine (redis, memcached)"
}

variable "identity_provider_url" {
  type        = string
  description = "OIDC/IdP URL"
}

variable "observability_endpoint" {
  type        = string
  description = "Metrics/logging endpoint"
}

variable "parameter_prefix" {
  type        = string
  description = "Base SSM parameter path prefix"
  default     = "/openfinance"
}

variable "log_retention_days" {
  type        = number
  description = "CloudWatch log retention"
  default     = 30
}

variable "workload_principal" {
  type        = string
  description = "AWS principal allowed to assume service workload role"
  default     = "ecs-tasks.amazonaws.com"
}

variable "tags" {
  type        = map(string)
  description = "Additional resource tags"
  default     = {}
}
