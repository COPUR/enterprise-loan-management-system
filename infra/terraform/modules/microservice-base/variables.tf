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
