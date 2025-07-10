# Development Environment Configuration
# Enterprise Banking EKS Infrastructure
# Local development and testing environment

# Basic Configuration
environment    = "development"
aws_region    = "us-east-1"
project_name  = "enterprise-banking"

# Network Configuration
vpc_cidr = "10.2.0.0/16"
private_domain_name = "dev.banking.internal"

# EKS Configuration
kubernetes_version = "1.28"
cluster_endpoint_public_access = true
cluster_endpoint_public_access_cidrs = ["0.0.0.0/0"]

# Banking Services Node Group
banking_services_instance_type = "c5.large"
banking_services_desired_size = 3
banking_services_min_size = 2
banking_services_max_size = 6

# AI/ML Workloads Node Group
ai_ml_instance_type = "g4dn.xlarge"
ai_ml_desired_size = 2

# Compliance Services Node Group
compliance_instance_type = "m5.large"

# Database Configuration
aurora_postgres_version = "13.7"
aurora_instance_count = 1
aurora_min_capacity = 0.5
aurora_max_capacity = 8
db_username = "banking_admin"

# Cache Configuration
redis_node_type = "cache.t4g.medium"
redis_num_cache_nodes = 2

# Security Configuration
enable_cluster_encryption = true
enable_irsa = true
enable_pod_security_policy = false

# Monitoring Configuration
enable_cloudwatch_logs = true
log_retention_days = 7
enable_performance_insights = false

# Backup Configuration
backup_retention_period = 7
enable_deletion_protection = false

# Cost Optimization
enable_spot_instances = true
spot_instance_percentage = 70

# Compliance Configuration
compliance_requirements = ["PCI-DSS", "GDPR"]
enable_encryption_at_rest = true
enable_encryption_in_transit = true

# Multi-Region Configuration
enable_cross_region_backup = false
backup_region = "us-west-2"

# Resource Tagging
business_unit = "banking-platform"
cost_center = "development"
data_classification = "internal"

additional_tags = {
  "Environment" = "development"
  "Purpose" = "development-testing"
  "AutoShutdown" = "enabled"
  "CostOptimization" = "aggressive"
}

# Feature Flags
enable_eks_fargate = false
enable_istio_service_mesh = true
enable_prometheus_monitoring = true
enable_grafana_dashboards = true
enable_fluentd_logging = false
enable_jaeger_tracing = false

# Development and Testing
enable_debug_mode = true
skip_final_snapshot = true

# Storage Configuration
ebs_volume_size = 50
ebs_volume_type = "gp3"
efs_throughput_mode = "bursting"
efs_provisioned_throughput = 100

# AWS Auth Configuration
aws_auth_roles = [
  {
    rolearn  = "arn:aws:iam::ACCOUNT_ID:role/BankingDevelopers"
    username = "banking-developers"
    groups   = ["system:masters"]
  }
]

aws_auth_users = []