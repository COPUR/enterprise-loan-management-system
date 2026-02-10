module "service" {
  source                 = "../../modules/microservice-base"
  service_name           = "Corporate Treasury Data Service"
  service_slug           = "corporate-treasury-service"
  environment            = var.environment
  database_engine        = var.database_engine
  cache_engine           = var.cache_engine
  identity_provider_url  = var.identity_provider_url
  observability_endpoint = var.observability_endpoint
}
