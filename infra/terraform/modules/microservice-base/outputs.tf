output "service_info" {
  value = {
    name        = var.service_name
    slug        = var.service_slug
    environment = var.environment
  }
}

output "cloudwatch_log_group_name" {
  value = aws_cloudwatch_log_group.service.name
}

output "cloudwatch_log_group_arn" {
  value = aws_cloudwatch_log_group.service.arn
}

output "workload_role_arn" {
  value = aws_iam_role.workload.arn
}

output "secret_arn" {
  value = aws_secretsmanager_secret.service_runtime.arn
}

output "ssm_parameter_paths" {
  value = {
    database_engine       = aws_ssm_parameter.database_engine.name
    cache_engine          = aws_ssm_parameter.cache_engine.name
    identity_provider_url = aws_ssm_parameter.identity_provider_url.name
    observability         = aws_ssm_parameter.observability_endpoint.name
  }
}
