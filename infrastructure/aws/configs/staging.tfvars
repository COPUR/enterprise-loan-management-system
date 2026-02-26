# Staging Environment Configuration
# Enterprise Banking EKS Infrastructure
# Pre-production testing environment

# Basic Configuration
environment    = "staging"
aws_region    = "us-east-1"
project_name  = "enterprise-banking"

# Network Configuration
vpc_cidr = "10.1.0.0/16"
private_domain_name = "staging.banking.internal"

# EKS Configuration
kubernetes_version = "1.28"
cluster_endpoint_public_access = false
cluster_endpoint_public_access_cidrs = []

# Banking Services Node Group
banking_services_instance_type = "c5.xlarge"
banking_services_desired_size = 4
banking_services_min_size = 2
banking_services_max_size = 8

# AI/ML Workloads Node Group
ai_ml_instance_type = "g4dn.xlarge"
ai_ml_desired_size = 3

# Compliance Services Node Group
compliance_instance_type = "m5.large"

# Database Configuration
aurora_postgres_version = "13.7"
aurora_instance_count = 2
aurora_min_capacity = 0.5
aurora_max_capacity = 16
db_username = "banking_admin"

# Cache Configuration
redis_node_type = "cache.r6g.large"
redis_num_cache_nodes = 3

# Security Configuration
enable_cluster_encryption = true
enable_irsa = true
enable_pod_security_policy = true

# Monitoring Configuration
enable_cloudwatch_logs = true
log_retention_days = 30
enable_performance_insights = true

# Backup Configuration
backup_retention_period = 14
enable_deletion_protection = false

# Cost Optimization
enable_spot_instances = true
spot_instance_percentage = 50

# Compliance Configuration
compliance_requirements = ["PCI-DSS", "SOX", "GDPR", "FAPI"]
enable_encryption_at_rest = true
enable_encryption_in_transit = true

# Multi-Region Configuration
enable_cross_region_backup = false
backup_region = "us-west-2"

# Resource Tagging
business_unit = "banking-platform"
cost_center = "technology"
data_classification = "internal"

additional_tags = {
  "Environment" = "staging"
  "Purpose" = "pre-production-testing"
  "AutoShutdown" = "enabled"
}

# Feature Flags
enable_eks_fargate = false
enable_istio_service_mesh = true
enable_prometheus_monitoring = true
enable_grafana_dashboards = true
enable_fluentd_logging = true
enable_jaeger_tracing = true

# Development and Testing
enable_debug_mode = true
skip_final_snapshot = true

# Storage Configuration
ebs_volume_size = 100
ebs_volume_type = "gp3"
efs_throughput_mode = "provisioned"
efs_provisioned_throughput = 500

# AWS Auth Configuration
aws_auth_roles = [
  {
    rolearn  = "arn:aws:iam::ACCOUNT_ID:role/BankingPlatformAdmins"
    username = "banking-platform-admins"
    groups   = ["system:masters"]
  },
  {
    rolearn  = "arn:aws:iam::ACCOUNT_ID:role/BankingDevelopers"
    username = "banking-developers"
    groups   = ["banking:developers"]
  }
]

aws_auth_users = []