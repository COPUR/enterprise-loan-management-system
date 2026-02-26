# Variables for Active-Active Multi-Region Architecture Module

# Basic Configuration
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

# Regional Configuration
variable "primary_region" {
  description = "Primary AWS region"
  type        = string
  default     = "us-east-1"
  
  validation {
    condition = can(regex("^[a-z]{2}-[a-z]+-[0-9]$", var.primary_region))
    error_message = "Primary region must be a valid AWS region format."
  }
}

variable "secondary_region" {
  description = "Secondary AWS region"
  type        = string
  default     = "eu-west-1"
  
  validation {
    condition = can(regex("^[a-z]{2}-[a-z]+-[0-9]$", var.secondary_region))
    error_message = "Secondary region must be a valid AWS region format."
  }
}

variable "tertiary_region" {
  description = "Tertiary AWS region"
  type        = string
  default     = "ap-southeast-1"
  
  validation {
    condition = can(regex("^[a-z]{2}-[a-z]+-[0-9]$", var.tertiary_region))
    error_message = "Tertiary region must be a valid AWS region format."
  }
}

# Traffic Distribution
variable "primary_traffic_percentage" {
  description = "Traffic percentage for primary region"
  type        = number
  default     = 100
  
  validation {
    condition     = var.primary_traffic_percentage >= 0 && var.primary_traffic_percentage <= 100
    error_message = "Primary traffic percentage must be between 0 and 100."
  }
}

variable "secondary_traffic_percentage" {
  description = "Traffic percentage for secondary region"
  type        = number
  default     = 100
  
  validation {
    condition     = var.secondary_traffic_percentage >= 0 && var.secondary_traffic_percentage <= 100
    error_message = "Secondary traffic percentage must be between 0 and 100."
  }
}

variable "tertiary_traffic_percentage" {
  description = "Traffic percentage for tertiary region"
  type        = number
  default     = 100
  
  validation {
    condition     = var.tertiary_traffic_percentage >= 0 && var.tertiary_traffic_percentage <= 100
    error_message = "Tertiary traffic percentage must be between 0 and 100."
  }
}

# Load Balancer Configuration
variable "primary_alb_arn" {
  description = "ARN of the primary region Application Load Balancer"
  type        = string
  
  validation {
    condition     = can(regex("^arn:aws:elasticloadbalancing:", var.primary_alb_arn))
    error_message = "Primary ALB ARN must be a valid load balancer ARN."
  }
}

variable "secondary_alb_arn" {
  description = "ARN of the secondary region Application Load Balancer"
  type        = string
  
  validation {
    condition     = can(regex("^arn:aws:elasticloadbalancing:", var.secondary_alb_arn))
    error_message = "Secondary ALB ARN must be a valid load balancer ARN."
  }
}

variable "tertiary_alb_arn" {
  description = "ARN of the tertiary region Application Load Balancer"
  type        = string
  
  validation {
    condition     = can(regex("^arn:aws:elasticloadbalancing:", var.tertiary_alb_arn))
    error_message = "Tertiary ALB ARN must be a valid load balancer ARN."
  }
}

variable "primary_alb_dns_name" {
  description = "DNS name of the primary region Application Load Balancer"
  type        = string
  
  validation {
    condition     = can(regex("^[a-z0-9.-]+$", var.primary_alb_dns_name))
    error_message = "Primary ALB DNS name must be a valid DNS name."
  }
}

variable "secondary_alb_dns_name" {
  description = "DNS name of the secondary region Application Load Balancer"
  type        = string
  
  validation {
    condition     = can(regex("^[a-z0-9.-]+$", var.secondary_alb_dns_name))
    error_message = "Secondary ALB DNS name must be a valid DNS name."
  }
}

variable "tertiary_alb_dns_name" {
  description = "DNS name of the tertiary region Application Load Balancer"
  type        = string
  
  validation {
    condition     = can(regex("^[a-z0-9.-]+$", var.tertiary_alb_dns_name))
    error_message = "Tertiary ALB DNS name must be a valid DNS name."
  }
}

# Domain Configuration
variable "domain_name" {
  description = "Primary domain name for the banking application"
  type        = string
  default     = "banking.example.com"
  
  validation {
    condition     = can(regex("^[a-z0-9.-]+\\.[a-z]{2,}$", var.domain_name))
    error_message = "Domain name must be a valid domain format."
  }
}

variable "api_subdomain" {
  description = "API subdomain"
  type        = string
  default     = "api"
  
  validation {
    condition     = can(regex("^[a-z0-9-]+$", var.api_subdomain))
    error_message = "API subdomain must contain only lowercase letters, numbers, and hyphens."
  }
}

# Aurora Global Database Configuration
variable "aurora_engine_version" {
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
    ], var.aurora_engine_version)
    error_message = "Aurora PostgreSQL version must be a supported version."
  }
}

variable "aurora_backup_retention_period" {
  description = "Number of days to retain Aurora backups"
  type        = number
  default     = 35
  
  validation {
    condition     = var.aurora_backup_retention_period >= 7 && var.aurora_backup_retention_period <= 35
    error_message = "Aurora backup retention period must be between 7 and 35 days."
  }
}

variable "aurora_preferred_backup_window" {
  description = "Preferred backup window for Aurora"
  type        = string
  default     = "03:00-04:00"
  
  validation {
    condition     = can(regex("^[0-9]{2}:[0-9]{2}-[0-9]{2}:[0-9]{2}$", var.aurora_preferred_backup_window))
    error_message = "Aurora preferred backup window must be in HH:MM-HH:MM format."
  }
}

variable "aurora_preferred_maintenance_window" {
  description = "Preferred maintenance window for Aurora"
  type        = string
  default     = "sun:04:00-sun:05:00"
  
  validation {
    condition     = can(regex("^[a-z]{3}:[0-9]{2}:[0-9]{2}-[a-z]{3}:[0-9]{2}:[0-9]{2}$", var.aurora_preferred_maintenance_window))
    error_message = "Aurora preferred maintenance window must be in day:HH:MM-day:HH:MM format."
  }
}

# Health Check Configuration
variable "health_check_path" {
  description = "Health check path for load balancers"
  type        = string
  default     = "/health/deep"
  
  validation {
    condition     = can(regex("^/", var.health_check_path))
    error_message = "Health check path must start with /."
  }
}

variable "health_check_interval" {
  description = "Health check interval in seconds"
  type        = number
  default     = 30
  
  validation {
    condition     = var.health_check_interval >= 10 && var.health_check_interval <= 300
    error_message = "Health check interval must be between 10 and 300 seconds."
  }
}

variable "health_check_timeout" {
  description = "Health check timeout in seconds"
  type        = number
  default     = 10
  
  validation {
    condition     = var.health_check_timeout >= 5 && var.health_check_timeout <= 60
    error_message = "Health check timeout must be between 5 and 60 seconds."
  }
}

variable "health_check_healthy_threshold" {
  description = "Number of consecutive successful health checks required"
  type        = number
  default     = 3
  
  validation {
    condition     = var.health_check_healthy_threshold >= 2 && var.health_check_healthy_threshold <= 10
    error_message = "Health check healthy threshold must be between 2 and 10."
  }
}

variable "health_check_unhealthy_threshold" {
  description = "Number of consecutive failed health checks required"
  type        = number
  default     = 3
  
  validation {
    condition     = var.health_check_unhealthy_threshold >= 2 && var.health_check_unhealthy_threshold <= 10
    error_message = "Health check unhealthy threshold must be between 2 and 10."
  }
}

# Monitoring and Alerting
variable "alert_email_addresses" {
  description = "List of email addresses for alerts"
  type        = list(string)
  default     = []
  
  validation {
    condition = alltrue([
      for email in var.alert_email_addresses : can(regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", email))
    ])
    error_message = "All email addresses must be valid email format."
  }
}

variable "alert_slack_webhook_url" {
  description = "Slack webhook URL for alerts"
  type        = string
  default     = ""
  sensitive   = true
}

variable "enable_detailed_monitoring" {
  description = "Enable detailed CloudWatch monitoring"
  type        = bool
  default     = true
}

variable "cloudwatch_log_retention_days" {
  description = "Number of days to retain CloudWatch logs"
  type        = number
  default     = 90
  
  validation {
    condition = contains([
      1, 3, 5, 7, 14, 30, 60, 90, 120, 150, 180, 365, 400, 545, 731, 1827, 3653
    ], var.cloudwatch_log_retention_days)
    error_message = "CloudWatch log retention days must be a valid retention value."
  }
}

# Failover Configuration
variable "enable_automatic_failover" {
  description = "Enable automatic failover for databases"
  type        = bool
  default     = true
}

variable "failover_priority" {
  description = "Map of regions to failover priority (lower number = higher priority)"
  type        = map(number)
  default = {
    "us-east-1"      = 1
    "eu-west-1"      = 2
    "ap-southeast-1" = 3
    "us-west-2"      = 4
    "eu-central-1"   = 5
    "ap-northeast-1" = 6
  }
  
  validation {
    condition = alltrue([
      for priority in values(var.failover_priority) : priority >= 1 && priority <= 10
    ])
    error_message = "Failover priorities must be between 1 and 10."
  }
}

variable "rto_target_minutes" {
  description = "Recovery Time Objective target in minutes"
  type        = number
  default     = 15
  
  validation {
    condition     = var.rto_target_minutes >= 1 && var.rto_target_minutes <= 60
    error_message = "RTO target must be between 1 and 60 minutes."
  }
}

variable "rpo_target_minutes" {
  description = "Recovery Point Objective target in minutes"
  type        = number
  default     = 5
  
  validation {
    condition     = var.rpo_target_minutes >= 1 && var.rpo_target_minutes <= 30
    error_message = "RPO target must be between 1 and 30 minutes."
  }
}

# Performance Configuration
variable "global_accelerator_flow_logs_enabled" {
  description = "Enable Global Accelerator flow logs"
  type        = bool
  default     = true
}

variable "route53_health_check_measure_latency" {
  description = "Enable latency measurement for Route 53 health checks"
  type        = bool
  default     = true
}

variable "aurora_performance_insights_enabled" {
  description = "Enable Performance Insights for Aurora"
  type        = bool
  default     = true
}

variable "aurora_performance_insights_retention_period" {
  description = "Performance Insights retention period in days"
  type        = number
  default     = 7
  
  validation {
    condition     = contains([7, 731], var.aurora_performance_insights_retention_period)
    error_message = "Performance Insights retention period must be 7 or 731 days."
  }
}

# Security Configuration
variable "enable_deletion_protection" {
  description = "Enable deletion protection for critical resources"
  type        = bool
  default     = false
}

variable "enable_encryption_at_rest" {
  description = "Enable encryption at rest for all storage"
  type        = bool
  default     = true
}

variable "enable_encryption_in_transit" {
  description = "Enable encryption in transit for all communication"
  type        = bool
  default     = true
}

variable "kms_key_rotation_enabled" {
  description = "Enable automatic KMS key rotation"
  type        = bool
  default     = true
}

# Compliance Configuration
variable "compliance_standards" {
  description = "List of compliance standards to enforce"
  type        = list(string)
  default     = ["SOX", "PCI-DSS", "GDPR", "FAPI"]
  
  validation {
    condition = alltrue([
      for standard in var.compliance_standards : contains(["SOX", "PCI-DSS", "GDPR", "FAPI", "HIPAA", "SOC2"], standard)
    ])
    error_message = "Compliance standards must be from the supported list."
  }
}

variable "enable_config_rules" {
  description = "Enable AWS Config rules for compliance monitoring"
  type        = bool
  default     = true
}

variable "enable_cloudtrail_logging" {
  description = "Enable CloudTrail logging for audit trails"
  type        = bool
  default     = true
}

variable "config_delivery_frequency" {
  description = "AWS Config delivery frequency"
  type        = string
  default     = "TwentyFour_Hours"
  
  validation {
    condition = contains([
      "One_Hour", "Three_Hours", "Six_Hours", "Twelve_Hours", "TwentyFour_Hours"
    ], var.config_delivery_frequency)
    error_message = "Config delivery frequency must be a valid option."
  }
}

# Cost Optimization
variable "enable_cost_optimization" {
  description = "Enable cost optimization features"
  type        = bool
  default     = true
}

variable "reserved_instance_percentage" {
  description = "Percentage of capacity to cover with reserved instances"
  type        = number
  default     = 70
  
  validation {
    condition     = var.reserved_instance_percentage >= 0 && var.reserved_instance_percentage <= 100
    error_message = "Reserved instance percentage must be between 0 and 100."
  }
}

variable "spot_instance_percentage" {
  description = "Percentage of capacity to use spot instances for non-critical workloads"
  type        = number
  default     = 20
  
  validation {
    condition     = var.spot_instance_percentage >= 0 && var.spot_instance_percentage <= 50
    error_message = "Spot instance percentage must be between 0 and 50."
  }
}

# Resource Tagging
variable "additional_tags" {
  description = "Additional tags to apply to all resources"
  type        = map(string)
  default     = {}
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

variable "business_unit" {
  description = "Business unit responsible for the resources"
  type        = string
  default     = "banking-platform"
  
  validation {
    condition     = can(regex("^[a-z0-9-]+$", var.business_unit))
    error_message = "Business unit must contain only lowercase letters, numbers, and hyphens."
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
variable "enable_experimental_features" {
  description = "Enable experimental features"
  type        = bool
  default     = false
}

variable "enable_blue_green_deployment" {
  description = "Enable blue-green deployment capabilities"
  type        = bool
  default     = false
}

variable "enable_canary_deployment" {
  description = "Enable canary deployment capabilities"
  type        = bool
  default     = true
}

variable "enable_chaos_engineering" {
  description = "Enable chaos engineering for resilience testing"
  type        = bool
  default     = false
}