variable "aws_region" {
  description = "AWS region for deployment (UAE region preferred)"
  type        = string
  default     = "me-south-1"
  
  validation {
    condition = contains([
      "me-south-1",    # UAE - Bahrain (closest to UAE)
      "me-central-1",  # UAE - Israel (if available)
      "eu-west-1",     # Europe - Ireland (backup)
      "eu-central-1"   # Europe - Frankfurt (backup)
    ], var.aws_region)
    error_message = "Region must be Middle East or Europe for UAE compliance requirements."
  }
}

variable "environment" {
  description = "Environment name (production, staging, development)"
  type        = string
  default     = "production"
  
  validation {
    condition = contains([
      "production",
      "staging", 
      "development",
      "sandbox"
    ], var.environment)
    error_message = "Environment must be one of: production, staging, development, sandbox."
  }
}

variable "allowed_cidr_blocks" {
  description = "CIDR blocks allowed to access the infrastructure"
  type        = list(string)
  default = [
    "10.0.0.0/8",     # Private networks
    "172.16.0.0/12",  # Private networks  
    "192.168.0.0/16", # Private networks
  ]
  
  validation {
    condition = length(var.allowed_cidr_blocks) > 0
    error_message = "At least one CIDR block must be specified."
  }
}

variable "aws_auth_roles" {
  description = "List of role maps for AWS auth ConfigMap"
  type = list(object({
    rolearn  = string
    username = string
    groups   = list(string)
  }))
  default = []
}

variable "aws_auth_users" {
  description = "List of user maps for AWS auth ConfigMap"
  type = list(object({
    userarn  = string
    username = string
    groups   = list(string)
  }))
  default = []
}

variable "enable_irsa" {
  description = "Enable IAM Roles for Service Accounts (IRSA)"
  type        = bool
  default     = true
}

variable "cluster_endpoint_private_access" {
  description = "Enable private API server endpoint"
  type        = bool
  default     = true
}

variable "cluster_endpoint_public_access" {
  description = "Enable public API server endpoint"
  type        = bool
  default     = true
}

variable "cluster_endpoint_public_access_cidrs" {
  description = "CIDR blocks that can access the public API server endpoint"
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "manage_aws_auth" {
  description = "Whether to apply the aws-auth ConfigMap"
  type        = bool
  default     = true
}

# Database configuration
variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.r6g.xlarge"
  
  validation {
    condition = can(regex("^db\\.", var.db_instance_class))
    error_message = "DB instance class must be a valid RDS instance type."
  }
}

variable "db_allocated_storage" {
  description = "The allocated storage in GB"
  type        = number
  default     = 100
  
  validation {
    condition = var.db_allocated_storage >= 20 && var.db_allocated_storage <= 65536
    error_message = "DB allocated storage must be between 20 GB and 65536 GB."
  }
}

variable "db_backup_retention_period" {
  description = "The days to retain backups"
  type        = number
  default     = 30
  
  validation {
    condition = var.db_backup_retention_period >= 7 && var.db_backup_retention_period <= 35
    error_message = "Backup retention must be between 7 and 35 days for compliance."
  }
}

variable "db_multi_az" {
  description = "Enable Multi-AZ deployment for RDS"
  type        = bool
  default     = true
}

variable "db_deletion_protection" {
  description = "Enable deletion protection for RDS"
  type        = bool
  default     = null  # Will be set based on environment
}

# Redis configuration
variable "redis_node_type" {
  description = "ElastiCache Redis node type"
  type        = string
  default     = "cache.r6g.large"
  
  validation {
    condition = can(regex("^cache\\.", var.redis_node_type))
    error_message = "Redis node type must be a valid ElastiCache instance type."
  }
}

variable "redis_num_cache_clusters" {
  description = "Number of cache clusters (nodes) in the replication group"
  type        = number
  default     = 3
  
  validation {
    condition = var.redis_num_cache_clusters >= 2 && var.redis_num_cache_clusters <= 6
    error_message = "Number of Redis cache clusters must be between 2 and 6."
  }
}

variable "redis_automatic_failover_enabled" {
  description = "Enable automatic failover for Redis"
  type        = bool
  default     = true
}

# EKS Node Group configuration
variable "node_groups" {
  description = "EKS managed node groups configuration"
  type = map(object({
    instance_types = list(string)
    min_size      = number
    max_size      = number
    desired_size  = number
    disk_size     = optional(number, 100)
    ami_type      = optional(string, "AL2_x86_64")
    capacity_type = optional(string, "ON_DEMAND")
    labels        = optional(map(string), {})
    taints = optional(list(object({
      key    = string
      value  = string
      effect = string
    })), [])
  }))
  default = {
    main = {
      instance_types = ["m5.xlarge"]
      min_size      = 3
      max_size      = 10
      desired_size  = 3
      disk_size     = 100
      labels = {
        role               = "main"
        "compliance.level" = "high-security"
      }
      taints = [{
        key    = "compliance-workload"
        value  = "true" 
        effect = "NO_SCHEDULE"
      }]
    }
    monitoring = {
      instance_types = ["m5.large"]
      min_size      = 2
      max_size      = 5
      desired_size  = 2
      disk_size     = 50
      labels = {
        role = "monitoring"
      }
    }
  }
}

# Monitoring and logging configuration
variable "enable_cloudwatch_logging" {
  description = "Enable CloudWatch logging"
  type        = bool
  default     = true
}

variable "cloudwatch_log_retention_days" {
  description = "CloudWatch log retention in days"
  type        = number
  default     = 30
  
  validation {
    condition = contains([1, 3, 5, 7, 14, 30, 60, 90, 120, 150, 180, 365, 400, 545, 731, 1827, 3653], var.cloudwatch_log_retention_days)
    error_message = "Log retention days must be a valid CloudWatch retention period."
  }
}

variable "audit_log_retention_days" {
  description = "Audit log retention in days (compliance requirement)"
  type        = number
  default     = 90
  
  validation {
    condition = var.audit_log_retention_days >= 90
    error_message = "Audit logs must be retained for at least 90 days for UAE compliance."
  }
}

# Security configuration
variable "enable_encryption_at_rest" {
  description = "Enable encryption at rest for all resources"
  type        = bool
  default     = true
}

variable "enable_encryption_in_transit" {
  description = "Enable encryption in transit for all resources"
  type        = bool
  default     = true
}

variable "kms_key_deletion_window" {
  description = "KMS key deletion window in days"
  type        = number
  default     = 7
  
  validation {
    condition = var.kms_key_deletion_window >= 7 && var.kms_key_deletion_window <= 30
    error_message = "KMS key deletion window must be between 7 and 30 days."
  }
}

variable "enable_key_rotation" {
  description = "Enable automatic KMS key rotation"
  type        = bool
  default     = true
}

# Backup configuration
variable "enable_automated_backups" {
  description = "Enable automated backups for all resources"
  type        = bool
  default     = true
}

variable "backup_retention_period" {
  description = "Backup retention period in days"
  type        = number
  default     = 30
  
  validation {
    condition = var.backup_retention_period >= 7
    error_message = "Backup retention must be at least 7 days."
  }
}

# Compliance and tagging
variable "compliance_tags" {
  description = "Additional compliance tags to apply to all resources"
  type        = map(string)
  default = {
    "compliance.cbuae.gov.ae/regulation" = "C7-2023"
    "security.level"                     = "high"
    "data.classification"                = "restricted"
    "audit.required"                     = "true"
    "backup.required"                    = "true"
    "monitoring.required"                = "true"
  }
}

variable "cost_center" {
  description = "Cost center for resource billing"
  type        = string
  default     = "FinancialServices"
}

variable "business_unit" {
  description = "Business unit owning the resources"
  type        = string
  default     = "OpenFinance"
}

variable "data_classification" {
  description = "Data classification level"
  type        = string
  default     = "restricted"
  
  validation {
    condition = contains(["public", "internal", "confidential", "restricted"], var.data_classification)
    error_message = "Data classification must be one of: public, internal, confidential, restricted."
  }
}

# Network configuration
variable "vpc_cidr" {
  description = "CIDR block for VPC"
  type        = string
  default     = "10.0.0.0/16"
  
  validation {
    condition = can(cidrhost(var.vpc_cidr, 0))
    error_message = "VPC CIDR must be a valid IPv4 CIDR block."
  }
}

variable "enable_dns_hostnames" {
  description = "Enable DNS hostnames in VPC"
  type        = bool
  default     = true
}

variable "enable_dns_support" {
  description = "Enable DNS support in VPC"
  type        = bool
  default     = true
}

variable "enable_nat_gateway" {
  description = "Enable NAT Gateway for private subnets"
  type        = bool
  default     = true
}

variable "single_nat_gateway" {
  description = "Use single NAT Gateway for all private subnets"
  type        = bool
  default     = false
}

variable "enable_vpn_gateway" {
  description = "Enable VPN gateway for hybrid connectivity"
  type        = bool
  default     = false
}

# Load balancer configuration
variable "load_balancer_type" {
  description = "Type of load balancer (application or network)"
  type        = string
  default     = "application"
  
  validation {
    condition = contains(["application", "network"], var.load_balancer_type)
    error_message = "Load balancer type must be either 'application' or 'network'."
  }
}

variable "enable_deletion_protection" {
  description = "Enable deletion protection for load balancer"
  type        = bool
  default     = null  # Will be set based on environment
}

variable "enable_cross_zone_load_balancing" {
  description = "Enable cross-zone load balancing"
  type        = bool
  default     = true
}

# SSL/TLS configuration
variable "ssl_policy" {
  description = "SSL policy for load balancer"
  type        = string
  default     = "ELBSecurityPolicy-TLS-1-2-2017-01"
  
  validation {
    condition = can(regex("^ELBSecurityPolicy-", var.ssl_policy))
    error_message = "SSL policy must be a valid ELB security policy."
  }
}

variable "certificate_arn" {
  description = "ARN of SSL certificate for HTTPS listeners"
  type        = string
  default     = null
}

# Monitoring configuration
variable "enable_enhanced_monitoring" {
  description = "Enable enhanced monitoring for RDS"
  type        = bool
  default     = true
}

variable "monitoring_interval" {
  description = "Enhanced monitoring interval in seconds"
  type        = number
  default     = 60
  
  validation {
    condition = contains([0, 1, 5, 10, 15, 30, 60], var.monitoring_interval)
    error_message = "Monitoring interval must be 0, 1, 5, 10, 15, 30, or 60 seconds."
  }
}

variable "enable_performance_insights" {
  description = "Enable Performance Insights for RDS"
  type        = bool
  default     = true
}

variable "performance_insights_retention_period" {
  description = "Performance Insights retention period in days"
  type        = number
  default     = 7
  
  validation {
    condition = contains([7, 731], var.performance_insights_retention_period)
    error_message = "Performance Insights retention must be 7 or 731 days."
  }
}

# Auto Scaling configuration  
variable "enable_cluster_autoscaler" {
  description = "Enable cluster autoscaler"
  type        = bool
  default     = true
}

variable "cluster_autoscaler_helm_version" {
  description = "Cluster autoscaler Helm chart version"
  type        = string
  default     = "9.21.0"
}

# Add-ons configuration
variable "enable_aws_load_balancer_controller" {
  description = "Enable AWS Load Balancer Controller"
  type        = bool
  default     = true
}

variable "enable_external_dns" {
  description = "Enable External DNS"
  type        = bool  
  default     = false
}

variable "enable_cert_manager" {
  description = "Enable cert-manager for automatic SSL certificate management"
  type        = bool
  default     = true
}

variable "enable_secrets_store_csi_driver" {
  description = "Enable AWS Secrets Store CSI Driver"
  type        = bool
  default     = true
}

# Resource limits and quotas
variable "resource_quotas" {
  description = "Resource quotas for namespaces"
  type = map(object({
    cpu_limit      = string
    memory_limit   = string
    storage_limit  = string
    pod_limit      = number
  }))
  default = {
    production = {
      cpu_limit     = "20"
      memory_limit  = "40Gi"  
      storage_limit = "100Gi"
      pod_limit     = 50
    }
    monitoring = {
      cpu_limit     = "10"
      memory_limit  = "20Gi"
      storage_limit = "50Gi" 
      pod_limit     = 25
    }
  }
}