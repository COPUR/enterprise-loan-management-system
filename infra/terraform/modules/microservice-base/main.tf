terraform {
  required_version = ">= 1.3.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = ">= 3.6"
    }
  }
}

locals {
  name_prefix = "${var.environment}-${var.service_slug}"
  common_tags = merge({
    ManagedBy   = "terraform"
    Module      = "microservice-base"
    ServiceName = var.service_name
    ServiceSlug = var.service_slug
    Environment = var.environment
  }, var.tags)

  parameter_root = "${var.parameter_prefix}/${var.environment}/${var.service_slug}"
}

resource "aws_cloudwatch_log_group" "service" {
  name              = "/openfinance/${var.environment}/${var.service_slug}"
  retention_in_days = var.log_retention_days
  tags              = local.common_tags
}

resource "aws_ssm_parameter" "database_engine" {
  name        = "${local.parameter_root}/database_engine"
  description = "Database engine for ${var.service_name}"
  type        = "String"
  value       = var.database_engine
  tags        = local.common_tags
}

resource "aws_ssm_parameter" "cache_engine" {
  name        = "${local.parameter_root}/cache_engine"
  description = "Cache engine for ${var.service_name}"
  type        = "String"
  value       = var.cache_engine
  tags        = local.common_tags
}

resource "aws_ssm_parameter" "identity_provider_url" {
  name        = "${local.parameter_root}/identity_provider_url"
  description = "Identity provider URL for ${var.service_name}"
  type        = "String"
  value       = var.identity_provider_url
  tags        = local.common_tags
}

resource "aws_ssm_parameter" "observability_endpoint" {
  name        = "${local.parameter_root}/observability_endpoint"
  description = "Observability endpoint for ${var.service_name}"
  type        = "String"
  value       = var.observability_endpoint
  tags        = local.common_tags
}

resource "random_password" "bootstrap_secret" {
  length  = 40
  special = true
}

resource "aws_secretsmanager_secret" "service_runtime" {
  name                    = "${local.name_prefix}/runtime"
  description             = "Runtime bootstrap secret for ${var.service_name}"
  recovery_window_in_days = 7
  tags                    = local.common_tags
}

resource "aws_secretsmanager_secret_version" "service_runtime" {
  secret_id = aws_secretsmanager_secret.service_runtime.id
  secret_string = jsonencode({
    generated_at = timestamp()
    token        = random_password.bootstrap_secret.result
  })
}

data "aws_iam_policy_document" "workload_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = [var.workload_principal]
    }
  }
}

resource "aws_iam_role" "workload" {
  name               = "${local.name_prefix}-workload-role"
  assume_role_policy = data.aws_iam_policy_document.workload_assume_role.json
  tags               = local.common_tags
}
