terraform {
  required_version = ">= 1.6"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.23"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.11"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.5"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
  }

  backend "s3" {
    bucket         = "openfinance-terraform-state"
    key            = "production/terraform.tfstate"
    region         = "me-south-1"  # UAE region
    encrypt        = true
    dynamodb_table = "openfinance-terraform-locks"
    
    # Enhanced security for UAE compliance
    kms_key_id = "arn:aws:kms:me-south-1:ACCOUNT:key/KEY-ID"
  }
}

# Provider configurations
provider "aws" {
  region = var.aws_region
  
  default_tags {
    tags = {
      Project     = "OpenFinance"
      Environment = var.environment
      ManagedBy   = "Terraform"
      Compliance  = "CBUAE-C7-2023"
      DataClass   = "Restricted"
      CostCenter  = "FinancialServices"
    }
  }
}

provider "kubernetes" {
  host                   = module.eks.cluster_endpoint
  cluster_ca_certificate = base64decode(module.eks.cluster_certificate_authority_data)
  
  exec {
    api_version = "client.authentication.k8s.io/v1beta1"
    command     = "aws"
    args        = ["eks", "get-token", "--cluster-name", module.eks.cluster_id]
  }
}

provider "helm" {
  kubernetes {
    host                   = module.eks.cluster_endpoint
    cluster_ca_certificate = base64decode(module.eks.cluster_certificate_authority_data)
    
    exec {
      api_version = "client.authentication.k8s.io/v1beta1"
      command     = "aws"
      args        = ["eks", "get-token", "--cluster-name", module.eks.cluster_id]
    }
  }
}

# Data sources
data "aws_availability_zones" "available" {
  state = "available"
}

data "aws_caller_identity" "current" {}

# Local values for configuration
locals {
  name_prefix = "openfinance-${var.environment}"
  
  # UAE-specific configurations
  region_code = "uae"
  compliance_tags = {
    "compliance.cbuae.gov.ae/regulation" = "C7-2023"
    "security.level"                     = "high"
    "data.classification"                = "restricted"
    "audit.required"                     = "true"
  }
  
  # Network configuration
  vpc_cidr = "10.0.0.0/16"
  azs      = slice(data.aws_availability_zones.available.names, 0, 3)
  
  # Cluster configuration
  cluster_version = "1.28"
  
  # Node group configurations
  node_groups = {
    main = {
      name           = "main-nodes"
      instance_types = ["m5.xlarge"]
      min_size      = 3
      max_size      = 10
      desired_size  = 3
      
      labels = {
        role                = "main"
        "compliance.level"  = "high-security"
      }
      
      taints = [{
        key    = "compliance-workload"
        value  = "true"
        effect = "NO_SCHEDULE"
      }]
    }
    
    monitoring = {
      name           = "monitoring-nodes"
      instance_types = ["m5.large"]
      min_size      = 2
      max_size      = 5
      desired_size  = 2
      
      labels = {
        role = "monitoring"
      }
    }
  }
}

# Generate random passwords for services
resource "random_password" "database_passwords" {
  for_each = toset([
    "postgres_admin",
    "postgres_openfinance",
    "postgres_keycloak",
    "postgres_grafana",
    "redis_admin",
    "mongodb_admin",
    "mongodb_openfinance"
  ])
  
  length  = 32
  special = true
}

resource "random_password" "application_secrets" {
  for_each = toset([
    "keycloak_admin",
    "grafana_admin",
    "jwt_signing_key",
    "encryption_key",
    "cbuae_api_key"
  ])
  
  length  = 64
  special = true
}

# VPC Module
module "vpc" {
  source = "terraform-aws-modules/vpc/aws"
  version = "~> 5.0"

  name = "${local.name_prefix}-vpc"
  cidr = local.vpc_cidr

  azs             = local.azs
  private_subnets = [for k, v in local.azs : cidrsubnet(local.vpc_cidr, 4, k)]
  public_subnets  = [for k, v in local.azs : cidrsubnet(local.vpc_cidr, 8, k + 48)]
  database_subnets = [for k, v in local.azs : cidrsubnet(local.vpc_cidr, 8, k + 52)]

  enable_nat_gateway   = true
  single_nat_gateway   = false
  enable_dns_hostnames = true
  enable_dns_support   = true
  
  # Enhanced security for UAE compliance
  enable_flow_log                      = true
  create_flow_log_cloudwatch_iam_role  = true
  create_flow_log_cloudwatch_log_group = true
  
  # VPC Endpoints for security
  enable_s3_endpoint       = true
  enable_dynamodb_endpoint = true

  public_subnet_tags = {
    "kubernetes.io/role/elb" = "1"
  }

  private_subnet_tags = {
    "kubernetes.io/role/internal-elb" = "1"
  }

  database_subnet_tags = {
    "Tier" = "Database"
    "Backup" = "Required"
  }

  tags = merge(local.compliance_tags, {
    "kubernetes.io/cluster/${local.name_prefix}-eks" = "shared"
  })
}

# EKS Cluster
module "eks" {
  source = "terraform-aws-modules/eks/aws"
  version = "~> 19.0"

  cluster_name    = "${local.name_prefix}-eks"
  cluster_version = local.cluster_version

  vpc_id                         = module.vpc.vpc_id
  subnet_ids                     = module.vpc.private_subnets
  cluster_endpoint_public_access = true
  cluster_endpoint_private_access = true
  
  # Enhanced security configuration
  cluster_endpoint_public_access_cidrs = var.allowed_cidr_blocks
  cluster_encryption_config = {
    provider_key_arn = aws_kms_key.eks.arn
    resources        = ["secrets"]
  }

  # Cluster add-ons
  cluster_addons = {
    coredns = {
      most_recent = true
    }
    kube-proxy = {
      most_recent = true
    }
    vpc-cni = {
      most_recent    = true
      before_compute = true
      configuration_values = jsonencode({
        env = {
          ENABLE_PREFIX_DELEGATION = "true"
          WARM_PREFIX_TARGET       = "1"
        }
      })
    }
    aws-ebs-csi-driver = {
      most_recent = true
    }
  }

  # Node groups
  eks_managed_node_groups = {
    for name, config in local.node_groups : name => {
      name           = "${local.name_prefix}-${config.name}"
      instance_types = config.instance_types
      min_size      = config.min_size
      max_size      = config.max_size
      desired_size  = config.desired_size

      labels = config.labels
      taints = lookup(config, "taints", [])

      vpc_security_group_ids = [aws_security_group.additional_eks_nodes.id]

      block_device_mappings = {
        xvda = {
          device_name = "/dev/xvda"
          ebs = {
            volume_size = 100
            volume_type = "gp3"
            iops        = 3000
            throughput  = 150
            encrypted   = true
            kms_key_id  = aws_kms_key.ebs.arn
            delete_on_termination = true
          }
        }
      }

      metadata_options = {
        http_endpoint               = "enabled"
        http_tokens                = "required"
        http_put_response_hop_limit = 2
        instance_metadata_tags      = "disabled"
      }

      tags = local.compliance_tags
    }
  }

  # AWS Auth configuration
  manage_aws_auth_configmap = true
  aws_auth_roles = var.aws_auth_roles
  aws_auth_users = var.aws_auth_users

  tags = local.compliance_tags
}

# KMS Keys for encryption
resource "aws_kms_key" "eks" {
  description             = "EKS Secret Encryption Key"
  deletion_window_in_days = 7
  enable_key_rotation     = true

  tags = merge(local.compliance_tags, {
    Name = "${local.name_prefix}-eks-secrets"
  })
}

resource "aws_kms_alias" "eks" {
  name          = "alias/${local.name_prefix}-eks-secrets"
  target_key_id = aws_kms_key.eks.key_id
}

resource "aws_kms_key" "ebs" {
  description             = "EBS Encryption Key"
  deletion_window_in_days = 7
  enable_key_rotation     = true

  tags = merge(local.compliance_tags, {
    Name = "${local.name_prefix}-ebs"
  })
}

resource "aws_kms_alias" "ebs" {
  name          = "alias/${local.name_prefix}-ebs"
  target_key_id = aws_kms_key.ebs.key_id
}

resource "aws_kms_key" "rds" {
  description             = "RDS Encryption Key"
  deletion_window_in_days = 7
  enable_key_rotation     = true

  tags = merge(local.compliance_tags, {
    Name = "${local.name_prefix}-rds"
  })
}

# Additional security group for EKS nodes
resource "aws_security_group" "additional_eks_nodes" {
  name_prefix = "${local.name_prefix}-eks-additional-"
  vpc_id      = module.vpc.vpc_id

  # Compliance monitoring
  ingress {
    description = "Prometheus monitoring"
    from_port   = 9090
    to_port     = 9100
    protocol    = "tcp"
    cidr_blocks = module.vpc.private_subnets_cidr_blocks
  }

  # FAPI 2.0 compliance ports
  ingress {
    description = "FAPI OAuth endpoints"
    from_port   = 8443
    to_port     = 8443
    protocol    = "tcp"
    cidr_blocks = module.vpc.private_subnets_cidr_blocks
  }

  tags = merge(local.compliance_tags, {
    Name = "${local.name_prefix}-eks-additional-sg"
  })
}

# RDS for PostgreSQL
module "rds" {
  source = "terraform-aws-modules/rds/aws"
  version = "~> 6.0"

  identifier = "${local.name_prefix}-postgres"

  engine            = "postgres"
  engine_version    = "15.4"
  instance_class    = "db.r6g.xlarge"
  allocated_storage = 100
  max_allocated_storage = 1000
  storage_type      = "gp3"
  storage_throughput = 125
  storage_iops      = 3000

  # Enhanced security
  storage_encrypted   = true
  kms_key_id         = aws_kms_key.rds.arn
  
  db_name  = "openfinance"
  username = "postgres"
  password = random_password.database_passwords["postgres_admin"].result
  port     = "5432"

  vpc_security_group_ids = [aws_security_group.rds.id]
  db_subnet_group_name   = module.vpc.database_subnet_group

  # UAE compliance requirements
  backup_retention_period = 30
  backup_window          = "03:00-04:00"
  maintenance_window     = "Sun:04:00-Sun:05:00"
  
  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]
  monitoring_interval = "60"
  monitoring_role_arn = aws_iam_role.rds_enhanced_monitoring.arn
  
  performance_insights_enabled = true
  performance_insights_kms_key_id = aws_kms_key.rds.arn
  performance_insights_retention_period = 7

  # Deletion protection for production
  deletion_protection = var.environment == "production" ? true : false
  
  # Multi-AZ for high availability
  multi_az               = true
  publicly_accessible    = false
  
  tags = merge(local.compliance_tags, {
    Name = "${local.name_prefix}-postgres"
  })
}

# RDS Security Group
resource "aws_security_group" "rds" {
  name_prefix = "${local.name_prefix}-rds-"
  vpc_id      = module.vpc.vpc_id

  ingress {
    description = "PostgreSQL from EKS"
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    security_groups = [module.eks.node_security_group_id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(local.compliance_tags, {
    Name = "${local.name_prefix}-rds-sg"
  })
}

# IAM role for RDS enhanced monitoring
resource "aws_iam_role" "rds_enhanced_monitoring" {
  name_prefix = "${local.name_prefix}-rds-monitoring-"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "monitoring.rds.amazonaws.com"
        }
      }
    ]
  })

  tags = local.compliance_tags
}

resource "aws_iam_role_policy_attachment" "rds_enhanced_monitoring" {
  role       = aws_iam_role.rds_enhanced_monitoring.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonRDSEnhancedMonitoringRole"
}

# ElastiCache Redis Cluster
resource "aws_elasticache_subnet_group" "redis" {
  name       = "${local.name_prefix}-redis-subnet-group"
  subnet_ids = module.vpc.private_subnets

  tags = local.compliance_tags
}

resource "aws_elasticache_replication_group" "redis" {
  replication_group_id       = "${local.name_prefix}-redis"
  description                = "Open Finance Redis cluster"
  
  node_type                  = "cache.r6g.large"
  port                       = 6379
  parameter_group_name       = aws_elasticache_parameter_group.redis.name
  
  num_cache_clusters         = 3
  automatic_failover_enabled = true
  multi_az_enabled          = true
  
  subnet_group_name = aws_elasticache_subnet_group.redis.name
  security_group_ids = [aws_security_group.redis.id]
  
  # Enhanced security
  at_rest_encryption_enabled = true
  transit_encryption_enabled = true
  auth_token                 = random_password.database_passwords["redis_admin"].result
  kms_key_id                = aws_kms_key.ebs.arn
  
  # Maintenance and backup
  maintenance_window         = "sun:05:00-sun:09:00"
  snapshot_retention_limit   = 5
  snapshot_window           = "03:00-05:00"
  
  # Logging
  log_delivery_configuration {
    destination      = aws_cloudwatch_log_group.redis.name
    destination_type = "cloudwatch-logs"
    log_format      = "text"
    log_type        = "slow-log"
  }

  tags = merge(local.compliance_tags, {
    Name = "${local.name_prefix}-redis"
  })
}

resource "aws_elasticache_parameter_group" "redis" {
  family = "redis7.x"
  name   = "${local.name_prefix}-redis-params"

  parameter {
    name  = "maxmemory-policy"
    value = "allkeys-lru"
  }

  parameter {
    name  = "timeout"
    value = "300"
  }

  tags = local.compliance_tags
}

resource "aws_security_group" "redis" {
  name_prefix = "${local.name_prefix}-redis-"
  vpc_id      = module.vpc.vpc_id

  ingress {
    description = "Redis from EKS"
    from_port   = 6379
    to_port     = 6379
    protocol    = "tcp"
    security_groups = [module.eks.node_security_group_id]
  }

  tags = merge(local.compliance_tags, {
    Name = "${local.name_prefix}-redis-sg"
  })
}

resource "aws_cloudwatch_log_group" "redis" {
  name              = "/aws/elasticache/${local.name_prefix}-redis"
  retention_in_days = 30
  kms_key_id       = aws_kms_key.ebs.arn

  tags = local.compliance_tags
}

# Application Load Balancer
resource "aws_lb" "main" {
  name               = "${local.name_prefix}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets           = module.vpc.public_subnets

  # Enhanced security
  drop_invalid_header_fields = true
  preserve_host_header      = true
  
  enable_deletion_protection = var.environment == "production" ? true : false

  access_logs {
    bucket  = aws_s3_bucket.alb_logs.id
    prefix  = "alb"
    enabled = true
  }

  tags = merge(local.compliance_tags, {
    Name = "${local.name_prefix}-alb"
  })
}

resource "aws_security_group" "alb" {
  name_prefix = "${local.name_prefix}-alb-"
  vpc_id      = module.vpc.vpc_id

  ingress {
    description = "HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = var.allowed_cidr_blocks
  }

  ingress {
    description = "HTTP redirect"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = var.allowed_cidr_blocks
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(local.compliance_tags, {
    Name = "${local.name_prefix}-alb-sg"
  })
}

# S3 bucket for ALB access logs
resource "aws_s3_bucket" "alb_logs" {
  bucket        = "${local.name_prefix}-alb-logs-${random_password.application_secrets["encryption_key"].result}"
  force_destroy = var.environment != "production"

  tags = local.compliance_tags
}

resource "aws_s3_bucket_versioning" "alb_logs" {
  bucket = aws_s3_bucket.alb_logs.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_encryption" "alb_logs" {
  bucket = aws_s3_bucket.alb_logs.id

  server_side_encryption_configuration {
    rule {
      apply_server_side_encryption_by_default {
        kms_master_key_id = aws_kms_key.ebs.arn
        sse_algorithm     = "aws:kms"
      }
    }
  }
}

resource "aws_s3_bucket_public_access_block" "alb_logs" {
  bucket = aws_s3_bucket.alb_logs.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_policy" "alb_logs" {
  bucket = aws_s3_bucket.alb_logs.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          AWS = "arn:aws:iam::754344448648:root"  # Middle East region ELB service account
        }
        Action   = "s3:PutObject"
        Resource = "${aws_s3_bucket.alb_logs.arn}/*"
      },
      {
        Effect = "Allow"
        Principal = {
          Service = "delivery.logs.amazonaws.com"
        }
        Action   = "s3:PutObject"
        Resource = "${aws_s3_bucket.alb_logs.arn}/*"
        Condition = {
          StringEquals = {
            "s3:x-amz-acl" = "bucket-owner-full-control"
          }
        }
      }
    ]
  })
}

# CloudWatch Log Groups
resource "aws_cloudwatch_log_group" "application" {
  name              = "/aws/eks/${local.name_prefix}/application"
  retention_in_days = 30
  kms_key_id       = aws_kms_key.ebs.arn

  tags = local.compliance_tags
}

resource "aws_cloudwatch_log_group" "audit" {
  name              = "/aws/eks/${local.name_prefix}/audit"
  retention_in_days = 90
  kms_key_id       = aws_kms_key.ebs.arn

  tags = merge(local.compliance_tags, {
    Purpose = "Audit"
    Retention = "Long-term"
  })
}

# Outputs
output "cluster_id" {
  description = "EKS cluster ID"
  value       = module.eks.cluster_id
}

output "cluster_arn" {
  description = "EKS cluster ARN"
  value       = module.eks.cluster_arn
}

output "cluster_endpoint" {
  description = "EKS cluster endpoint"
  value       = module.eks.cluster_endpoint
  sensitive   = true
}

output "cluster_security_group_id" {
  description = "EKS cluster security group ID"
  value       = module.eks.cluster_security_group_id
}

output "rds_endpoint" {
  description = "RDS endpoint"
  value       = module.rds.db_instance_endpoint
  sensitive   = true
}

output "redis_endpoint" {
  description = "Redis cluster endpoint"
  value       = aws_elasticache_replication_group.redis.primary_endpoint_address
  sensitive   = true
}

output "load_balancer_dns" {
  description = "Load balancer DNS name"
  value       = aws_lb.main.dns_name
}