module "service" {
  source                 = "../../modules/microservice-base"
  service_name           = "Bulk Payments Orchestration Service"
  service_slug           = "bulk-payments-service"
  environment            = var.environment
  database_engine        = var.database_engine
  cache_engine           = var.cache_engine
  identity_provider_url  = var.identity_provider_url
  observability_endpoint = var.observability_endpoint
}
