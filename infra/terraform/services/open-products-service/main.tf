module "service" {
  source                 = "../../modules/microservice-base"
  service_name           = "Open Products Catalog Service"
  service_slug           = "open-products-service"
  environment            = var.environment
  database_engine        = var.database_engine
  cache_engine           = var.cache_engine
  identity_provider_url  = var.identity_provider_url
  observability_endpoint = var.observability_endpoint
}
