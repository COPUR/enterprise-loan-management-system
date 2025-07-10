# Active-Active Multi-Region Architecture Module
# Enterprise Banking 99.999% Availability Implementation

terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
      configuration_aliases = [
        aws.primary,
        aws.secondary,
        aws.tertiary
      ]
    }
  }
}

# Local variables
locals {
  regions = {
    primary   = var.primary_region
    secondary = var.secondary_region
    tertiary  = var.tertiary_region
  }
  
  common_tags = {
    Architecture = "active-active"
    Availability = "99.999"
    Environment  = var.environment
    Project      = "enterprise-banking"
  }
}

# AWS Global Accelerator
resource "aws_globalaccelerator_accelerator" "banking_global" {
  name            = "${var.environment}-banking-global-accelerator"
  ip_address_type = "IPV4"
  enabled         = true

  attributes {
    flow_logs_enabled   = true
    flow_logs_s3_bucket = aws_s3_bucket.global_accelerator_logs.bucket
    flow_logs_s3_prefix = "flow-logs/"
  }

  tags = merge(local.common_tags, {
    Name = "${var.environment}-banking-global-accelerator"
  })
}

# Global Accelerator Listener
resource "aws_globalaccelerator_listener" "banking_https" {
  accelerator_arn = aws_globalaccelerator_accelerator.banking_global.id
  protocol        = "TCP"
  port_range {
    from = 443
    to   = 443
  }

  client_affinity = "SOURCE_IP"
}

resource "aws_globalaccelerator_listener" "banking_http" {
  accelerator_arn = aws_globalaccelerator_accelerator.banking_global.id
  protocol        = "TCP"
  port_range {
    from = 80
    to   = 80
  }

  client_affinity = "SOURCE_IP"
}

# Primary Region Endpoint Group
resource "aws_globalaccelerator_endpoint_group" "primary" {
  listener_arn = aws_globalaccelerator_listener.banking_https.id
  
  endpoint_group_region        = var.primary_region
  traffic_dial_percentage      = var.primary_traffic_percentage
  health_check_grace_period_seconds = 30
  health_check_path           = "/health/deep"
  health_check_port           = 443
  health_check_protocol       = "HTTPS"
  health_check_interval_seconds = 30
  threshold_count             = 3

  endpoint_configuration {
    endpoint_id = var.primary_alb_arn
    weight      = 100
  }

  port_override {
    listener_port = 443
    endpoint_port = 443
  }

  port_override {
    listener_port = 80
    endpoint_port = 80
  }
}

# Secondary Region Endpoint Group
resource "aws_globalaccelerator_endpoint_group" "secondary" {
  listener_arn = aws_globalaccelerator_listener.banking_https.id
  
  endpoint_group_region        = var.secondary_region
  traffic_dial_percentage      = var.secondary_traffic_percentage
  health_check_grace_period_seconds = 30
  health_check_path           = "/health/deep"
  health_check_port           = 443
  health_check_protocol       = "HTTPS"
  health_check_interval_seconds = 30
  threshold_count             = 3

  endpoint_configuration {
    endpoint_id = var.secondary_alb_arn
    weight      = 100
  }

  port_override {
    listener_port = 443
    endpoint_port = 443
  }

  port_override {
    listener_port = 80
    endpoint_port = 80
  }
}

# Tertiary Region Endpoint Group
resource "aws_globalaccelerator_endpoint_group" "tertiary" {
  listener_arn = aws_globalaccelerator_listener.banking_https.id
  
  endpoint_group_region        = var.tertiary_region
  traffic_dial_percentage      = var.tertiary_traffic_percentage
  health_check_grace_period_seconds = 30
  health_check_path           = "/health/deep"
  health_check_port           = 443
  health_check_protocol       = "HTTPS"
  health_check_interval_seconds = 30
  threshold_count             = 3

  endpoint_configuration {
    endpoint_id = var.tertiary_alb_arn
    weight      = 100
  }

  port_override {
    listener_port = 443
    endpoint_port = 443
  }

  port_override {
    listener_port = 80
    endpoint_port = 80
  }
}

# S3 Bucket for Global Accelerator Logs
resource "aws_s3_bucket" "global_accelerator_logs" {
  bucket = "${var.environment}-banking-global-accelerator-logs-${random_id.bucket_suffix.hex}"

  tags = merge(local.common_tags, {
    Name = "${var.environment}-global-accelerator-logs"
  })
}

resource "aws_s3_bucket_versioning" "global_accelerator_logs" {
  bucket = aws_s3_bucket.global_accelerator_logs.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_encryption" "global_accelerator_logs" {
  bucket = aws_s3_bucket.global_accelerator_logs.id

  server_side_encryption_configuration {
    rule {
      apply_server_side_encryption_by_default {
        sse_algorithm = "AES256"
      }
    }
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "global_accelerator_logs" {
  bucket = aws_s3_bucket.global_accelerator_logs.id

  rule {
    id     = "lifecycle"
    status = "Enabled"

    expiration {
      days = 90
    }

    noncurrent_version_expiration {
      noncurrent_days = 30
    }
  }
}

# Random ID for unique bucket naming
resource "random_id" "bucket_suffix" {
  byte_length = 4
}

# Route 53 Hosted Zone
resource "aws_route53_zone" "banking_global" {
  name = var.domain_name

  tags = merge(local.common_tags, {
    Name = "${var.environment}-banking-global-zone"
  })
}

# Route 53 Health Checks for each region
resource "aws_route53_health_check" "primary_region" {
  fqdn                            = var.primary_alb_dns_name
  port                            = 443
  type                            = "HTTPS"
  resource_path                   = "/health/deep"
  failure_threshold               = 3
  request_interval                = 30
  measure_latency                 = true
  cloudwatch_logs_region          = var.primary_region
  
  regions = ["us-east-1", "us-west-2", "eu-west-1"]

  tags = merge(local.common_tags, {
    Name = "${var.environment}-primary-region-health"
  })
}

resource "aws_route53_health_check" "secondary_region" {
  fqdn                            = var.secondary_alb_dns_name
  port                            = 443
  type                            = "HTTPS"
  resource_path                   = "/health/deep"
  failure_threshold               = 3
  request_interval                = 30
  measure_latency                 = true
  cloudwatch_logs_region          = var.secondary_region
  
  regions = ["eu-west-1", "eu-central-1", "us-east-1"]

  tags = merge(local.common_tags, {
    Name = "${var.environment}-secondary-region-health"
  })
}

resource "aws_route53_health_check" "tertiary_region" {
  fqdn                            = var.tertiary_alb_dns_name
  port                            = 443
  type                            = "HTTPS"
  resource_path                   = "/health/deep"
  failure_threshold               = 3
  request_interval                = 30
  measure_latency                 = true
  cloudwatch_logs_region          = var.tertiary_region
  
  regions = ["ap-southeast-1", "ap-northeast-1", "us-west-2"]

  tags = merge(local.common_tags, {
    Name = "${var.environment}-tertiary-region-health"
  })
}

# Route 53 Records with Geolocation and Health Check
resource "aws_route53_record" "banking_api_primary" {
  zone_id = aws_route53_zone.banking_global.zone_id
  name    = "api"
  type    = "A"

  set_identifier = "primary-region"
  health_check_id = aws_route53_health_check.primary_region.id

  geolocation_routing_policy {
    continent = "NA"
  }

  alias {
    name                   = aws_globalaccelerator_accelerator.banking_global.dns_name
    zone_id                = aws_globalaccelerator_accelerator.banking_global.hosted_zone_id
    evaluate_target_health = true
  }
}

resource "aws_route53_record" "banking_api_secondary" {
  zone_id = aws_route53_zone.banking_global.zone_id
  name    = "api"
  type    = "A"

  set_identifier = "secondary-region"
  health_check_id = aws_route53_health_check.secondary_region.id

  geolocation_routing_policy {
    continent = "EU"
  }

  alias {
    name                   = aws_globalaccelerator_accelerator.banking_global.dns_name
    zone_id                = aws_globalaccelerator_accelerator.banking_global.hosted_zone_id
    evaluate_target_health = true
  }
}

resource "aws_route53_record" "banking_api_tertiary" {
  zone_id = aws_route53_zone.banking_global.zone_id
  name    = "api"
  type    = "A"

  set_identifier = "tertiary-region"
  health_check_id = aws_route53_health_check.tertiary_region.id

  geolocation_routing_policy {
    continent = "AS"
  }

  alias {
    name                   = aws_globalaccelerator_accelerator.banking_global.dns_name
    zone_id                = aws_globalaccelerator_accelerator.banking_global.hosted_zone_id
    evaluate_target_health = true
  }
}

# Default record for unmatched locations
resource "aws_route53_record" "banking_api_default" {
  zone_id = aws_route53_zone.banking_global.zone_id
  name    = "api"
  type    = "A"

  set_identifier = "default"
  health_check_id = aws_route53_health_check.primary_region.id

  geolocation_routing_policy {
    country = "*"
  }

  alias {
    name                   = aws_globalaccelerator_accelerator.banking_global.dns_name
    zone_id                = aws_globalaccelerator_accelerator.banking_global.hosted_zone_id
    evaluate_target_health = true
  }
}

# Aurora Global Database Cluster
resource "aws_rds_global_cluster" "banking_global" {
  global_cluster_identifier = "${var.environment}-banking-global-cluster"
  engine                    = "aurora-postgresql"
  engine_version            = var.aurora_engine_version
  database_name             = "banking"
  
  deletion_protection = var.environment == "production" ? true : false
  
  tags = merge(local.common_tags, {
    Name = "${var.environment}-banking-global-cluster"
  })
}

# CloudWatch Dashboard for Global Monitoring
resource "aws_cloudwatch_dashboard" "global_banking" {
  dashboard_name = "${var.environment}-banking-global-dashboard"

  dashboard_body = jsonencode({
    widgets = [
      {
        type   = "metric"
        x      = 0
        y      = 0
        width  = 12
        height = 6

        properties = {
          metrics = [
            ["AWS/GlobalAccelerator", "NewFlowCount", "Accelerator", aws_globalaccelerator_accelerator.banking_global.id],
            ["AWS/GlobalAccelerator", "ProcessedBytesIn", "Accelerator", aws_globalaccelerator_accelerator.banking_global.id],
            ["AWS/GlobalAccelerator", "ProcessedBytesOut", "Accelerator", aws_globalaccelerator_accelerator.banking_global.id]
          ]
          period = 300
          stat   = "Sum"
          region = "us-west-2"  # Global Accelerator metrics are in us-west-2
          title  = "Global Accelerator Metrics"
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 6
        width  = 12
        height = 6

        properties = {
          metrics = [
            ["AWS/Route53", "HealthCheckStatus", "HealthCheckId", aws_route53_health_check.primary_region.id],
            ["AWS/Route53", "HealthCheckStatus", "HealthCheckId", aws_route53_health_check.secondary_region.id],
            ["AWS/Route53", "HealthCheckStatus", "HealthCheckId", aws_route53_health_check.tertiary_region.id]
          ]
          period = 300
          stat   = "Average"
          region = "us-east-1"
          title  = "Regional Health Checks"
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 12
        width  = 12
        height = 6

        properties = {
          metrics = [
            ["AWS/RDS", "DatabaseConnections", "DBClusterIdentifier", "${var.environment}-banking-primary-${var.primary_region}"],
            ["AWS/RDS", "AuroraGlobalDBReplicationLag", "DBClusterIdentifier", "${var.environment}-banking-secondary-${var.secondary_region}"],
            ["AWS/RDS", "AuroraGlobalDBReplicationLag", "DBClusterIdentifier", "${var.environment}-banking-secondary-${var.tertiary_region}"]
          ]
          period = 300
          stat   = "Average"
          region = var.primary_region
          title  = "Aurora Global Database Metrics"
        }
      }
    ]
  })
}

# CloudWatch Alarms for Global Monitoring
resource "aws_cloudwatch_metric_alarm" "global_accelerator_health" {
  alarm_name          = "${var.environment}-global-accelerator-unhealthy-endpoints"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "TargetCount"
  namespace           = "AWS/GlobalAccelerator"
  period              = "300"
  statistic           = "Average"
  threshold           = "1"
  alarm_description   = "This metric monitors Global Accelerator healthy endpoints"
  alarm_actions       = [aws_sns_topic.global_alerts.arn]

  dimensions = {
    Accelerator = aws_globalaccelerator_accelerator.banking_global.id
  }

  tags = local.common_tags
}

resource "aws_cloudwatch_metric_alarm" "aurora_replication_lag" {
  count = length([var.secondary_region, var.tertiary_region])
  
  alarm_name          = "${var.environment}-aurora-replication-lag-${element([var.secondary_region, var.tertiary_region], count.index)}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "AuroraGlobalDBReplicationLag"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = "5000" # 5 seconds in milliseconds
  alarm_description   = "This metric monitors Aurora Global DB replication lag"
  alarm_actions       = [aws_sns_topic.global_alerts.arn]

  dimensions = {
    DBClusterIdentifier = "${var.environment}-banking-secondary-${element([var.secondary_region, var.tertiary_region], count.index)}"
  }

  tags = local.common_tags
}

# SNS Topic for Global Alerts
resource "aws_sns_topic" "global_alerts" {
  name = "${var.environment}-banking-global-alerts"

  tags = merge(local.common_tags, {
    Name = "${var.environment}-banking-global-alerts"
  })
}

resource "aws_sns_topic_subscription" "global_alerts_email" {
  count     = length(var.alert_email_addresses)
  topic_arn = aws_sns_topic.global_alerts.arn
  protocol  = "email"
  endpoint  = var.alert_email_addresses[count.index]
}

# AWS Config Rules for Global Compliance
resource "aws_config_configuration_recorder" "global_banking" {
  name     = "${var.environment}-banking-global-recorder"
  role_arn = aws_iam_role.config_role.arn

  recording_group {
    all_supported                 = true
    include_global_resource_types = true
  }

  depends_on = [aws_config_delivery_channel.global_banking]
}

resource "aws_config_delivery_channel" "global_banking" {
  name           = "${var.environment}-banking-global-delivery-channel"
  s3_bucket_name = aws_s3_bucket.config_bucket.bucket
}

resource "aws_s3_bucket" "config_bucket" {
  bucket        = "${var.environment}-banking-config-${random_id.bucket_suffix.hex}"
  force_destroy = true

  tags = merge(local.common_tags, {
    Name = "${var.environment}-banking-config"
  })
}

resource "aws_s3_bucket_policy" "config_bucket_policy" {
  bucket = aws_s3_bucket.config_bucket.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "AWSConfigBucketPermissionsCheck"
        Effect = "Allow"
        Principal = {
          Service = "config.amazonaws.com"
        }
        Action   = "s3:GetBucketAcl"
        Resource = aws_s3_bucket.config_bucket.arn
        Condition = {
          StringEquals = {
            "AWS:SourceAccount" = data.aws_caller_identity.current.account_id
          }
        }
      },
      {
        Sid    = "AWSConfigBucketExistenceCheck"
        Effect = "Allow"
        Principal = {
          Service = "config.amazonaws.com"
        }
        Action   = "s3:ListBucket"
        Resource = aws_s3_bucket.config_bucket.arn
        Condition = {
          StringEquals = {
            "AWS:SourceAccount" = data.aws_caller_identity.current.account_id
          }
        }
      },
      {
        Sid    = "AWSConfigBucketDelivery"
        Effect = "Allow"
        Principal = {
          Service = "config.amazonaws.com"
        }
        Action   = "s3:PutObject"
        Resource = "${aws_s3_bucket.config_bucket.arn}/*"
        Condition = {
          StringEquals = {
            "s3:x-amz-acl" = "bucket-owner-full-control"
            "AWS:SourceAccount" = data.aws_caller_identity.current.account_id
          }
        }
      }
    ]
  })
}

# IAM Role for AWS Config
resource "aws_iam_role" "config_role" {
  name = "${var.environment}-banking-config-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "config.amazonaws.com"
        }
      }
    ]
  })

  tags = local.common_tags
}

resource "aws_iam_role_policy_attachment" "config_role_policy" {
  role       = aws_iam_role.config_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/ConfigRole"
}

# Data sources
data "aws_caller_identity" "current" {}

# Outputs
output "global_accelerator_dns_name" {
  description = "DNS name of the Global Accelerator"
  value       = aws_globalaccelerator_accelerator.banking_global.dns_name
}

output "global_accelerator_hosted_zone_id" {
  description = "Hosted zone ID of the Global Accelerator"
  value       = aws_globalaccelerator_accelerator.banking_global.hosted_zone_id
}

output "route53_zone_id" {
  description = "Route 53 hosted zone ID"
  value       = aws_route53_zone.banking_global.zone_id
}

output "route53_name_servers" {
  description = "Route 53 name servers"
  value       = aws_route53_zone.banking_global.name_servers
}

output "aurora_global_cluster_id" {
  description = "Aurora Global Database cluster identifier"
  value       = aws_rds_global_cluster.banking_global.id
}

output "global_monitoring_dashboard_url" {
  description = "CloudWatch dashboard URL for global monitoring"
  value       = "https://console.aws.amazon.com/cloudwatch/home?region=${var.primary_region}#dashboards:name=${aws_cloudwatch_dashboard.global_banking.dashboard_name}"
}

output "global_alerts_topic_arn" {
  description = "SNS topic ARN for global alerts"
  value       = aws_sns_topic.global_alerts.arn
}