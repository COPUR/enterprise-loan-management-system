# Variables for Enterprise Banking AWS EKS Infrastructure

# Basic Configuration
variable "aws_region" {
  description = "AWS region for deployment"
  type        = string
  default     = "us-east-1"
  
  validation {
    condition = can(regex("^[a-z]{2}-[a-z]+-[0-9]$", var.aws_region))
    error_message = "AWS region must be a valid region format (e.g., us-east-1)."
  }
}

variable "environment" {
  description = "Environment name (development, staging, production)"
  type        = string
  
  validation {
    condition     = contains(["development", "staging", "production"], var.environment)
    error_message = "Environment must be one of: development, staging, production."
  }
}

variable "project_name" {
  description = "Name of the project"
  type        = string
  default     = "enterprise-banking"
  
  validation {
    condition     = can(regex("^[a-z0-9-]+$", var.project_name))
    error_message = "Project name must contain only lowercase letters, numbers, and hyphens."
  }
}

# Network Configuration
variable "vpc_cidr" {
  description = "CIDR block for VPC"
  type        = string
  default     = "10.0.0.0/16"
  
  validation {
    condition     = can(cidrhost(var.vpc_cidr, 0))
    error_message = "VPC CIDR must be a valid IPv4 CIDR block."
  }
}

variable "private_domain_name" {
  description = "Private domain name for Route53 hosted zone"
  type        = string
  default     = "banking.internal"
  
  validation {
    condition     = can(regex("^[a-z0-9.-]+$", var.private_domain_name))
    error_message = "Domain name must be a valid domain format."
  }
}

# EKS Configuration
variable "kubernetes_version" {
  description = "Kubernetes version for EKS cluster"
  type        = string
  default     = "1.28"
  
  validation {
    condition     = can(regex("^1\\.(2[4-9]|[3-9][0-9])$", var.kubernetes_version))
    error_message = "Kubernetes version must be 1.24 or higher."
  }
}

variable "cluster_endpoint_public_access" {
  description = "Enable public API server endpoint"
  type        = bool
  default     = false
}

variable "cluster_endpoint_public_access_cidrs" {
  description = "List of CIDR blocks that can access the public API server endpoint"
  type        = list(string)
  default     = []
  
  validation {
    condition = alltrue([
      for cidr in var.cluster_endpoint_public_access_cidrs : can(cidrhost(cidr, 0))
    ])
    error_message = "All CIDR blocks must be valid IPv4 CIDR blocks."
  }
}

# Node Group Configuration
variable "banking_services_instance_type" {
  description = "Instance type for banking services node group"
  type        = string
  default     = "c5.2xlarge"
  
  validation {
    condition = contains([
      "c5.large", "c5.xlarge", "c5.2xlarge", "c5.4xlarge", "c5.9xlarge",
      "c5n.large", "c5n.xlarge", "c5n.2xlarge", "c5n.4xlarge",
      "m5.large", "m5.xlarge", "m5.2xlarge", "m5.4xlarge", "m5.8xlarge"
    ], var.banking_services_instance_type)
    error_message = "Instance type must be a supported EC2 instance type for banking workloads."
  }
}

variable "banking_services_desired_size" {
  description = "Desired number of nodes in banking services node group"
  type        = number
  default     = 6
  
  validation {
    condition     = var.banking_services_desired_size >= 3 && var.banking_services_desired_size <= 20
    error_message = "Desired size must be between 3 and 20 nodes."
  }
}

variable "banking_services_min_size" {
  description = "Minimum number of nodes in banking services node group"
  type        = number
  default     = 3
  
  validation {
    condition     = var.banking_services_min_size >= 2
    error_message = "Minimum size must be at least 2 nodes for high availability."
  }
}

variable "banking_services_max_size" {
  description = "Maximum number of nodes in banking services node group"
  type        = number
  default     = 12
  
  validation {
    condition     = var.banking_services_max_size <= 50
    error_message = "Maximum size must not exceed 50 nodes."
  }
}

variable "ai_ml_instance_type" {
  description = "Instance type for AI/ML workloads node group"
  type        = string
  default     = "g4dn.xlarge"
  
  validation {
    condition = contains([
      "g4dn.xlarge", "g4dn.2xlarge", "g4dn.4xlarge", "g4dn.8xlarge",
      "p3.2xlarge", "p3.8xlarge", "p3.16xlarge",
      "p4d.24xlarge"
    ], var.ai_ml_instance_type)
    error_message = "Instance type must be a supported GPU instance type."
  }
}

variable "ai_ml_desired_size" {
  description = "Desired number of nodes in AI/ML node group"
  type        = number
  default     = 4
  
  validation {
    condition     = var.ai_ml_desired_size >= 2 && var.ai_ml_desired_size <= 10
    error_message = "Desired size must be between 2 and 10 nodes for AI/ML workloads."
  }
}

variable "compliance_instance_type" {
  description = "Instance type for compliance services node group"
  type        = string
  default     = "m5.xlarge"
  
  validation {
    condition = contains([
      "m5.large", "m5.xlarge", "m5.2xlarge", "m5.4xlarge",
      "c5.large", "c5.xlarge", "c5.2xlarge"
    ], var.compliance_instance_type)
    error_message = "Instance type must be suitable for compliance workloads."
  }
}

# Database Configuration
variable "aurora_postgres_version" {
  description = "Aurora PostgreSQL engine version"
  type        = string
  default     = "13.7"
  
  validation {
    condition = contains([
      "11.16", "11.17", "11.18", "11.19", "11.20", "11.21",
      "12.11", "12.12", "12.13", "12.14", "12.15", "12.16",
      "13.7", "13.8", "13.9", "13.10", "13.11", "13.12",
      "14.6", "14.7", "14.8", "14.9",
      "15.2", "15.3", "15.4"
    ], var.aurora_postgres_version)
    error_message = "Aurora PostgreSQL version must be a supported version."
  }
}

variable "aurora_instance_count" {
  description = "Number of Aurora instances"
  type        = number
  default     = 2
  
  validation {
    condition     = var.aurora_instance_count >= 1 && var.aurora_instance_count <= 15
    error_message = "Aurora instance count must be between 1 and 15."
  }
}

variable "aurora_min_capacity" {
  description = "Minimum Aurora Serverless capacity"
  type        = number
  default     = 0.5
  
  validation {
    condition = contains([
      0.5, 1, 1.5, 2, 2.5, 3, 3.5, 4, 4.5, 5, 5.5, 6, 6.5, 7, 7.5, 8
    ], var.aurora_min_capacity)
    error_message = "Aurora minimum capacity must be a valid ServerlessV2 capacity value."
  }
}

variable "aurora_max_capacity" {
  description = "Maximum Aurora Serverless capacity"
  type        = number
  default     = 16
  
  validation {
    condition     = var.aurora_max_capacity >= 1 && var.aurora_max_capacity <= 128
    error_message = "Aurora maximum capacity must be between 1 and 128 ACUs."
  }
}

variable "db_username" {
  description = "Database admin username"
  type        = string
  default     = "banking_admin"
  sensitive   = true
  
  validation {
    condition     = can(regex("^[a-zA-Z][a-zA-Z0-9_]*$", var.db_username))
    error_message = "Database username must start with a letter and contain only alphanumeric characters and underscores."
  }
}

variable "db_password" {
  description = "Database admin password"
  type        = string
  sensitive   = true
  
  validation {
    condition     = length(var.db_password) >= 12
    error_message = "Database password must be at least 12 characters long."
  }
}

# Cache Configuration
variable "redis_node_type" {
  description = "ElastiCache Redis node type"
  type        = string
  default     = "cache.r6g.large"
  
  validation {
    condition = contains([
      "cache.t4g.micro", "cache.t4g.small", "cache.t4g.medium",
      "cache.r6g.large", "cache.r6g.xlarge", "cache.r6g.2xlarge",
      "cache.r6g.4xlarge", "cache.r6g.8xlarge", "cache.r6g.12xlarge",
      "cache.r6g.16xlarge"
    ], var.redis_node_type)
    error_message = "Redis node type must be a supported ElastiCache instance type."
  }
}

variable "redis_num_cache_nodes" {
  description = "Number of cache nodes in the Redis cluster"
  type        = number
  default     = 3
  
  validation {
    condition     = var.redis_num_cache_nodes >= 2 && var.redis_num_cache_nodes <= 6
    error_message = "Number of cache nodes must be between 2 and 6 for high availability."
  }
}

variable "redis_auth_token" {
  description = "Auth token for Redis cluster"
  type        = string
  sensitive   = true
  default     = ""
  
  validation {
    condition     = length(var.redis_auth_token) >= 16 && length(var.redis_auth_token) <= 128
    error_message = "Redis auth token must be between 16 and 128 characters long."
  }
}

# Security Configuration
variable "enable_cluster_encryption" {
  description = "Enable EKS cluster encryption"
  type        = bool
  default     = true
}

variable "enable_irsa" {
  description = "Enable IAM Roles for Service Accounts (IRSA)"
  type        = bool
  default     = true
}

variable "enable_pod_security_policy" {
  description = "Enable Pod Security Policy"
  type        = bool
  default     = true
}

# AWS Auth Configuration
variable "aws_auth_roles" {
  description = "List of role maps to add to the aws-auth configmap"
  type = list(object({
    rolearn  = string
    username = string
    groups   = list(string)
  }))
  default = []
}

variable "aws_auth_users" {
  description = "List of user maps to add to the aws-auth configmap"
  type = list(object({
    userarn  = string
    username = string
    groups   = list(string)
  }))
  default = []
}

# Monitoring Configuration
variable "enable_cloudwatch_logs" {
  description = "Enable CloudWatch logging for EKS cluster"
  type        = bool
  default     = true
}

variable "log_retention_days" {
  description = "Number of days to retain CloudWatch logs"
  type        = number
  default     = 90
  
  validation {
    condition = contains([
      1, 3, 5, 7, 14, 30, 60, 90, 120, 150, 180, 365, 400, 545, 731, 1827, 3653
    ], var.log_retention_days)
    error_message = "Log retention days must be a valid CloudWatch logs retention value."
  }
}

variable "enable_performance_insights" {
  description = "Enable Performance Insights for RDS"
  type        = bool
  default     = true
}

# Backup Configuration
variable "backup_retention_period" {
  description = "Number of days to retain automated backups"
  type        = number
  default     = 35
  
  validation {
    condition     = var.backup_retention_period >= 7 && var.backup_retention_period <= 35
    error_message = "Backup retention period must be between 7 and 35 days."
  }
}

variable "enable_deletion_protection" {
  description = "Enable deletion protection for production resources"
  type        = bool
  default     = false
}

# Cost Optimization
variable "enable_spot_instances" {
  description = "Enable spot instances for non-critical workloads"
  type        = bool
  default     = false
}

variable "spot_instance_percentage" {
  description = "Percentage of spot instances in node groups"
  type        = number
  default     = 30
  
  validation {
    condition     = var.spot_instance_percentage >= 0 && var.spot_instance_percentage <= 100
    error_message = "Spot instance percentage must be between 0 and 100."
  }
}

# Compliance Configuration
variable "compliance_requirements" {
  description = "List of compliance requirements to enforce"
  type        = list(string)
  default     = ["PCI-DSS", "SOX", "GDPR", "FAPI"]
  
  validation {
    condition = alltrue([
      for req in var.compliance_requirements : contains(["PCI-DSS", "SOX", "GDPR", "FAPI", "HIPAA", "SOC2"], req)
    ])
    error_message = "Compliance requirements must be from the supported list: PCI-DSS, SOX, GDPR, FAPI, HIPAA, SOC2."
  }
}

variable "enable_encryption_at_rest" {
  description = "Enable encryption at rest for all supported services"
  type        = bool
  default     = true
}

variable "enable_encryption_in_transit" {
  description = "Enable encryption in transit for all supported services"
  type        = bool
  default     = true
}

# Multi-Region Configuration
variable "enable_cross_region_backup" {
  description = "Enable cross-region backup replication"
  type        = bool
  default     = false
}

variable "backup_region" {
  description = "AWS region for cross-region backup replication"
  type        = string
  default     = "us-west-2"
  
  validation {
    condition = can(regex("^[a-z]{2}-[a-z]+-[0-9]$", var.backup_region))
    error_message = "Backup region must be a valid AWS region format."
  }
}

# Resource Tagging
variable "additional_tags" {
  description = "Additional tags to apply to all resources"
  type        = map(string)
  default     = {}
}

variable "business_unit" {
  description = "Business unit responsible for the resources"
  type        = string
  default     = "banking-platform"
  
  validation {
    condition     = can(regex("^[a-z0-9-]+$", var.business_unit))
    error_message = "Business unit must contain only lowercase letters, numbers, and hyphens."
  }
}

variable "cost_center" {
  description = "Cost center for billing allocation"
  type        = string
  default     = "technology"
  
  validation {
    condition     = can(regex("^[a-z0-9-]+$", var.cost_center))
    error_message = "Cost center must contain only lowercase letters, numbers, and hyphens."
  }
}

variable "data_classification" {
  description = "Data classification level"
  type        = string
  default     = "restricted"
  
  validation {
    condition     = contains(["public", "internal", "confidential", "restricted"], var.data_classification)
    error_message = "Data classification must be one of: public, internal, confidential, restricted."
  }
}

# Feature Flags
variable "enable_eks_fargate" {
  description = "Enable EKS Fargate for serverless pods"
  type        = bool
  default     = false
}

variable "enable_istio_service_mesh" {
  description = "Enable Istio service mesh"
  type        = bool
  default     = true
}

variable "enable_prometheus_monitoring" {
  description = "Enable Prometheus monitoring stack"
  type        = bool
  default     = true
}

variable "enable_grafana_dashboards" {
  description = "Enable Grafana dashboards"
  type        = bool
  default     = true
}

variable "enable_fluentd_logging" {
  description = "Enable Fluentd for log aggregation"
  type        = bool
  default     = true
}

variable "enable_jaeger_tracing" {
  description = "Enable Jaeger for distributed tracing"
  type        = bool
  default     = true
}

# Development and Testing
variable "enable_debug_mode" {
  description = "Enable debug mode for development environments"
  type        = bool
  default     = false
}

variable "skip_final_snapshot" {
  description = "Skip final snapshot when deleting RDS cluster (for development)"
  type        = bool
  default     = true
}

# Storage Configuration
variable "ebs_volume_size" {
  description = "Size of EBS volumes for worker nodes (GB)"
  type        = number
  default     = 100
  
  validation {
    condition     = var.ebs_volume_size >= 20 && var.ebs_volume_size <= 1000
    error_message = "EBS volume size must be between 20 and 1000 GB."
  }
}

variable "ebs_volume_type" {
  description = "Type of EBS volumes for worker nodes"
  type        = string
  default     = "gp3"
  
  validation {
    condition     = contains(["gp2", "gp3", "io1", "io2"], var.ebs_volume_type)
    error_message = "EBS volume type must be one of: gp2, gp3, io1, io2."
  }
}

variable "efs_throughput_mode" {
  description = "EFS throughput mode"
  type        = string
  default     = "provisioned"
  
  validation {
    condition     = contains(["bursting", "provisioned"], var.efs_throughput_mode)
    error_message = "EFS throughput mode must be either 'bursting' or 'provisioned'."
  }
}

variable "efs_provisioned_throughput" {
  description = "EFS provisioned throughput in MB/s (only used when throughput_mode is provisioned)"
  type        = number
  default     = 500
  
  validation {
    condition     = var.efs_provisioned_throughput >= 1 && var.efs_provisioned_throughput <= 1024
    error_message = "EFS provisioned throughput must be between 1 and 1024 MB/s."
  }
}