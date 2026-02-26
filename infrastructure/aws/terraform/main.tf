# Enterprise Banking AWS EKS Infrastructure
# Terraform Configuration for Production-Grade Kubernetes Deployment
# Compliance: PCI DSS, SOX, GDPR, FAPI 2.0

terraform {
  required_version = ">= 1.6"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.24"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.12"
    }
  }

  backend "s3" {
    bucket         = "enterprise-banking-terraform-state"
    key            = "infrastructure/terraform.tfstate"
    region         = "us-east-1"
    encrypt        = true
    dynamodb_table = "terraform-state-lock"
  }
}

# Configure the AWS Provider
provider "aws" {
  region = var.aws_region
  
  default_tags {
    tags = {
      Environment         = var.environment
      Project            = "enterprise-banking"
      Compliance         = "PCI-DSS,SOX,GDPR,FAPI"
      ManagedBy          = "terraform"
      BackupRequired     = "true"
      SecurityLevel      = "critical"
      DataClassification = "restricted"
    }
  }
}

# Data sources for availability zones
data "aws_availability_zones" "available" {
  state = "available"
}

# Data source for current AWS account
data "aws_caller_identity" "current" {}

# Data source for current AWS region
data "aws_region" "current" {}

# Local variables
locals {
  cluster_name = "${var.environment}-banking-cluster"
  
  vpc_cidr = var.vpc_cidr
  azs      = slice(data.aws_availability_zones.available.names, 0, 3)
  
  private_subnets  = [for k, v in local.azs : cidrsubnet(local.vpc_cidr, 8, k + 10)]
  public_subnets   = [for k, v in local.azs : cidrsubnet(local.vpc_cidr, 8, k)]
  database_subnets = [for k, v in local.azs : cidrsubnet(local.vpc_cidr, 8, k + 20)]
  
  common_tags = {
    Environment = var.environment
    Cluster     = local.cluster_name
    Owner       = "banking-platform-team"
  }
}

# VPC Module
module "vpc" {
  source = "terraform-aws-modules/vpc/aws"
  version = "~> 5.0"

  name = "${var.environment}-banking-vpc"
  cidr = local.vpc_cidr

  azs              = local.azs
  private_subnets  = local.private_subnets
  public_subnets   = local.public_subnets
  database_subnets = local.database_subnets

  enable_nat_gateway = true
  enable_vpn_gateway = false
  single_nat_gateway = false
  one_nat_gateway_per_az = true

  enable_dns_hostnames = true
  enable_dns_support   = true

  # VPC Flow Logs for security monitoring
  enable_flow_log                      = true
  create_flow_log_cloudwatch_iam_role  = true
  create_flow_log_cloudwatch_log_group = true
  flow_log_cloudwatch_log_group_retention_in_days = 90

  # Public subnet tags for ALB
  public_subnet_tags = {
    "kubernetes.io/role/elb" = "1"
    "kubernetes.io/cluster/${local.cluster_name}" = "shared"
    SubnetType = "public"
  }

  # Private subnet tags for NLB
  private_subnet_tags = {
    "kubernetes.io/role/internal-elb" = "1"
    "kubernetes.io/cluster/${local.cluster_name}" = "shared"
    SubnetType = "private"
  }

  # Database subnet tags
  database_subnet_tags = {
    SubnetType = "database"
  }

  tags = merge(local.common_tags, {
    Name = "${var.environment}-banking-vpc"
  })
}

# Security Groups
resource "aws_security_group" "eks_cluster_sg" {
  name_prefix = "${local.cluster_name}-cluster-"
  vpc_id      = module.vpc.vpc_id

  ingress {
    description = "HTTPS API access"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = [local.vpc_cidr]
  }

  egress {
    description = "All outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(local.common_tags, {
    Name = "${local.cluster_name}-cluster-sg"
  })
}

resource "aws_security_group" "eks_node_sg" {
  name_prefix = "${local.cluster_name}-node-"
  vpc_id      = module.vpc.vpc_id

  ingress {
    description = "Node to node communication"
    from_port   = 0
    to_port     = 65535
    protocol    = "tcp"
    self        = true
  }

  ingress {
    description     = "Cluster API to node kubelets"
    from_port       = 10250
    to_port         = 10250
    protocol        = "tcp"
    security_groups = [aws_security_group.eks_cluster_sg.id]
  }

  ingress {
    description     = "Cluster API to node kube-proxy"
    from_port       = 10256
    to_port         = 10256
    protocol        = "tcp"
    security_groups = [aws_security_group.eks_cluster_sg.id]
  }

  egress {
    description = "All outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(local.common_tags, {
    Name = "${local.cluster_name}-node-sg"
  })
}

# KMS Key for EKS Secrets Encryption
resource "aws_kms_key" "eks_secrets" {
  description             = "KMS key for EKS secrets encryption"
  deletion_window_in_days = 7
  enable_key_rotation     = true

  tags = merge(local.common_tags, {
    Name = "${local.cluster_name}-secrets-key"
  })
}

resource "aws_kms_alias" "eks_secrets" {
  name          = "alias/${local.cluster_name}-secrets"
  target_key_id = aws_kms_key.eks_secrets.key_id
}

# EKS Cluster
module "eks" {
  source = "terraform-aws-modules/eks/aws"
  version = "~> 19.21"

  cluster_name    = local.cluster_name
  cluster_version = var.kubernetes_version

  vpc_id                   = module.vpc.vpc_id
  subnet_ids               = module.vpc.private_subnets
  control_plane_subnet_ids = module.vpc.private_subnets

  # Cluster endpoint configuration
  cluster_endpoint_private_access = true
  cluster_endpoint_public_access  = var.cluster_endpoint_public_access
  cluster_endpoint_public_access_cidrs = var.cluster_endpoint_public_access_cidrs

  # Cluster encryption
  cluster_encryption_config = [
    {
      provider_key_arn = aws_kms_key.eks_secrets.arn
      resources        = ["secrets"]
    }
  ]

  # Cluster addons
  cluster_addons = {
    coredns = {
      most_recent = true
      configuration_values = jsonencode({
        tolerations = [
          {
            key    = "CriticalAddonsOnly"
            operator = "Exists"
          }
        ]
      })
    }
    kube-proxy = {
      most_recent = true
    }
    vpc-cni = {
      most_recent = true
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
    aws-efs-csi-driver = {
      most_recent = true
    }
  }

  # Managed Node Groups
  eks_managed_node_groups = {
    banking_services = {
      name           = "banking-services"
      description    = "Node group for banking application services"
      instance_types = ["c5.2xlarge"]
      
      min_size     = 3
      max_size     = 12
      desired_size = 6

      ami_type        = "AL2_x86_64"
      platform        = "linux"
      capacity_type   = "ON_DEMAND"
      
      disk_size = 100
      disk_type = "gp3"
      disk_encrypted = true
      disk_kms_key_id = aws_kms_key.eks_secrets.arn

      subnet_ids = module.vpc.private_subnets

      labels = {
        workload-type = "banking-services"
        compliance    = "pci-dss"
      }

      taints = [
        {
          key    = "banking-services"
          value  = "true"
          effect = "NO_SCHEDULE"
        }
      ]

      update_config = {
        max_unavailable_percentage = 25
      }

      tags = merge(local.common_tags, {
        NodeGroup = "banking-services"
      })
    }

    ai_ml_workloads = {
      name           = "ai-ml-workloads"
      description    = "Node group for AI/ML workloads"
      instance_types = ["g4dn.xlarge"]
      
      min_size     = 2
      max_size     = 8
      desired_size = 4

      ami_type        = "AL2_x86_64_GPU"
      platform        = "linux"
      capacity_type   = "SPOT"
      
      disk_size = 200
      disk_type = "gp3"
      disk_encrypted = true
      disk_kms_key_id = aws_kms_key.eks_secrets.arn

      subnet_ids = module.vpc.private_subnets

      labels = {
        workload-type = "ai-ml"
        node-type     = "gpu"
      }

      taints = [
        {
          key    = "ai-ml-workloads"
          value  = "true"
          effect = "NO_SCHEDULE"
        },
        {
          key    = "nvidia.com/gpu"
          value  = "true"
          effect = "NO_SCHEDULE"
        }
      ]

      tags = merge(local.common_tags, {
        NodeGroup = "ai-ml-workloads"
      })
    }

    compliance_services = {
      name           = "compliance-services"
      description    = "Node group for compliance and audit services"
      instance_types = ["m5.xlarge"]
      
      min_size     = 2
      max_size     = 6
      desired_size = 3

      ami_type        = "AL2_x86_64"
      platform        = "linux"
      capacity_type   = "ON_DEMAND"
      
      disk_size = 100
      disk_type = "gp3"
      disk_encrypted = true
      disk_kms_key_id = aws_kms_key.eks_secrets.arn

      subnet_ids = module.vpc.private_subnets

      labels = {
        workload-type = "compliance"
        security-level = "high"
      }

      taints = [
        {
          key    = "compliance-services"
          value  = "true"
          effect = "NO_SCHEDULE"
        }
      ]

      tags = merge(local.common_tags, {
        NodeGroup = "compliance-services"
      })
    }
  }

  # AWS Auth ConfigMap
  manage_aws_auth_configmap = true
  aws_auth_roles = var.aws_auth_roles
  aws_auth_users = var.aws_auth_users

  tags = local.common_tags
}

# EFS File System for shared storage
resource "aws_efs_file_system" "banking_shared_storage" {
  creation_token = "${local.cluster_name}-shared-storage"
  
  performance_mode = "generalPurpose"
  throughput_mode  = "provisioned"
  provisioned_throughput_in_mibps = 500

  encrypted  = true
  kms_key_id = aws_kms_key.eks_secrets.arn

  lifecycle_policy {
    transition_to_ia = "AFTER_30_DAYS"
  }

  tags = merge(local.common_tags, {
    Name = "${local.cluster_name}-shared-storage"
  })
}

# EFS Mount Targets
resource "aws_efs_mount_target" "banking_shared_storage" {
  count           = length(module.vpc.private_subnets)
  file_system_id  = aws_efs_file_system.banking_shared_storage.id
  subnet_id       = module.vpc.private_subnets[count.index]
  security_groups = [aws_security_group.efs.id]
}

# Security Group for EFS
resource "aws_security_group" "efs" {
  name_prefix = "${local.cluster_name}-efs-"
  vpc_id      = module.vpc.vpc_id

  ingress {
    description     = "NFS from EKS nodes"
    from_port       = 2049
    to_port         = 2049
    protocol        = "tcp"
    security_groups = [aws_security_group.eks_node_sg.id]
  }

  egress {
    description = "All outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(local.common_tags, {
    Name = "${local.cluster_name}-efs-sg"
  })
}

# RDS Subnet Group
resource "aws_db_subnet_group" "banking" {
  name       = "${local.cluster_name}-db-subnet-group"
  subnet_ids = module.vpc.database_subnets

  tags = merge(local.common_tags, {
    Name = "${local.cluster_name}-db-subnet-group"
  })
}

# Security Group for RDS
resource "aws_security_group" "rds" {
  name_prefix = "${local.cluster_name}-rds-"
  vpc_id      = module.vpc.vpc_id

  ingress {
    description     = "PostgreSQL from EKS nodes"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.eks_node_sg.id]
  }

  tags = merge(local.common_tags, {
    Name = "${local.cluster_name}-rds-sg"
  })
}

# RDS KMS Key
resource "aws_kms_key" "rds" {
  description             = "KMS key for RDS encryption"
  deletion_window_in_days = 7
  enable_key_rotation     = true

  tags = merge(local.common_tags, {
    Name = "${local.cluster_name}-rds-key"
  })
}

resource "aws_kms_alias" "rds" {
  name          = "alias/${local.cluster_name}-rds"
  target_key_id = aws_kms_key.rds.key_id
}

# RDS Aurora PostgreSQL Cluster
resource "aws_rds_cluster" "banking" {
  cluster_identifier = "${local.cluster_name}-db"
  
  engine         = "aurora-postgresql"
  engine_version = var.aurora_postgres_version
  engine_mode    = "provisioned"
  
  database_name   = "banking"
  master_username = var.db_username
  master_password = var.db_password
  
  db_subnet_group_name   = aws_db_subnet_group.banking.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  
  backup_retention_period = 35
  preferred_backup_window = "03:00-04:00"
  preferred_maintenance_window = "sun:04:00-sun:05:00"
  
  storage_encrypted = true
  kms_key_id       = aws_kms_key.rds.arn
  
  copy_tags_to_snapshot = true
  deletion_protection   = var.environment == "production" ? true : false
  skip_final_snapshot   = var.environment == "production" ? false : true
  final_snapshot_identifier = var.environment == "production" ? "${local.cluster_name}-final-snapshot" : null
  
  enabled_cloudwatch_logs_exports = ["postgresql"]
  
  serverlessv2_scaling_configuration {
    max_capacity = var.aurora_max_capacity
    min_capacity = var.aurora_min_capacity
  }

  tags = merge(local.common_tags, {
    Name = "${local.cluster_name}-db"
  })
}

# RDS Aurora Instances
resource "aws_rds_cluster_instance" "banking" {
  count              = var.aurora_instance_count
  identifier         = "${local.cluster_name}-db-${count.index}"
  cluster_identifier = aws_rds_cluster.banking.id
  
  instance_class = "db.serverless"
  engine         = aws_rds_cluster.banking.engine
  engine_version = aws_rds_cluster.banking.engine_version
  
  performance_insights_enabled = true
  monitoring_interval         = 60
  
  tags = merge(local.common_tags, {
    Name = "${local.cluster_name}-db-${count.index}"
  })
}

# ElastiCache Subnet Group
resource "aws_elasticache_subnet_group" "banking" {
  name       = "${local.cluster_name}-cache-subnet-group"
  subnet_ids = module.vpc.private_subnets

  tags = merge(local.common_tags, {
    Name = "${local.cluster_name}-cache-subnet-group"
  })
}

# Security Group for ElastiCache
resource "aws_security_group" "elasticache" {
  name_prefix = "${local.cluster_name}-cache-"
  vpc_id      = module.vpc.vpc_id

  ingress {
    description     = "Redis from EKS nodes"
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [aws_security_group.eks_node_sg.id]
  }

  tags = merge(local.common_tags, {
    Name = "${local.cluster_name}-cache-sg"
  })
}

# ElastiCache Redis Replication Group
resource "aws_elasticache_replication_group" "banking" {
  replication_group_id       = "${local.cluster_name}-cache"
  description                = "Redis cluster for banking application"
  
  node_type                  = var.redis_node_type
  port                       = 6379
  parameter_group_name       = "default.redis7"
  
  num_cache_clusters         = var.redis_num_cache_nodes
  automatic_failover_enabled = true
  multi_az_enabled          = true
  
  subnet_group_name  = aws_elasticache_subnet_group.banking.name
  security_group_ids = [aws_security_group.elasticache.id]
  
  at_rest_encryption_enabled = true
  transit_encryption_enabled = true
  auth_token                 = var.redis_auth_token
  
  snapshot_retention_limit = 7
  snapshot_window         = "03:00-05:00"
  maintenance_window      = "sun:05:00-sun:07:00"
  
  log_delivery_configuration {
    destination      = aws_cloudwatch_log_group.redis_slow.name
    destination_type = "cloudwatch-logs"
    log_format       = "text"
    log_type         = "slow-log"
  }

  tags = merge(local.common_tags, {
    Name = "${local.cluster_name}-cache"
  })
}

# CloudWatch Log Group for Redis
resource "aws_cloudwatch_log_group" "redis_slow" {
  name              = "/aws/elasticache/${local.cluster_name}/slow-log"
  retention_in_days = 30

  tags = local.common_tags
}

# S3 Bucket for Application Data
resource "aws_s3_bucket" "banking_data" {
  bucket = "${local.cluster_name}-banking-data-${random_id.bucket_suffix.hex}"

  tags = merge(local.common_tags, {
    Name = "${local.cluster_name}-banking-data"
  })
}

resource "aws_s3_bucket_versioning" "banking_data" {
  bucket = aws_s3_bucket.banking_data.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_encryption" "banking_data" {
  bucket = aws_s3_bucket.banking_data.id

  server_side_encryption_configuration {
    rule {
      apply_server_side_encryption_by_default {
        kms_master_key_id = aws_kms_key.s3.arn
        sse_algorithm     = "aws:kms"
      }
    }
  }
}

resource "aws_s3_bucket_public_access_block" "banking_data" {
  bucket = aws_s3_bucket.banking_data.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# S3 KMS Key
resource "aws_kms_key" "s3" {
  description             = "KMS key for S3 encryption"
  deletion_window_in_days = 7
  enable_key_rotation     = true

  tags = merge(local.common_tags, {
    Name = "${local.cluster_name}-s3-key"
  })
}

resource "aws_kms_alias" "s3" {
  name          = "alias/${local.cluster_name}-s3"
  target_key_id = aws_kms_key.s3.key_id
}

# Random ID for bucket naming
resource "random_id" "bucket_suffix" {
  byte_length = 4
}

# CloudWatch Log Groups
resource "aws_cloudwatch_log_group" "eks_cluster" {
  name              = "/aws/eks/${local.cluster_name}/cluster"
  retention_in_days = 90

  tags = local.common_tags
}

# Application Load Balancer
resource "aws_lb" "banking_alb" {
  name               = "${local.cluster_name}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = module.vpc.public_subnets

  enable_deletion_protection = var.environment == "production" ? true : false

  access_logs {
    bucket  = aws_s3_bucket.alb_logs.bucket
    prefix  = "alb"
    enabled = true
  }

  tags = merge(local.common_tags, {
    Name = "${local.cluster_name}-alb"
  })
}

# Security Group for ALB
resource "aws_security_group" "alb" {
  name_prefix = "${local.cluster_name}-alb-"
  vpc_id      = module.vpc.vpc_id

  ingress {
    description = "HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "All outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(local.common_tags, {
    Name = "${local.cluster_name}-alb-sg"
  })
}

# S3 Bucket for ALB Logs
resource "aws_s3_bucket" "alb_logs" {
  bucket = "${local.cluster_name}-alb-logs-${random_id.bucket_suffix.hex}"

  tags = merge(local.common_tags, {
    Name = "${local.cluster_name}-alb-logs"
  })
}

resource "aws_s3_bucket_policy" "alb_logs" {
  bucket = aws_s3_bucket.alb_logs.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          AWS = "arn:aws:iam::${data.aws_elb_service_account.main.id}:root"
        }
        Action   = "s3:PutObject"
        Resource = "${aws_s3_bucket.alb_logs.arn}/*"
      }
    ]
  })
}

# Data source for ELB service account
data "aws_elb_service_account" "main" {}

# Route53 Private Hosted Zone
resource "aws_route53_zone" "private" {
  name = var.private_domain_name

  vpc {
    vpc_id = module.vpc.vpc_id
  }

  tags = merge(local.common_tags, {
    Name = "${local.cluster_name}-private-zone"
  })
}

# Outputs
output "cluster_endpoint" {
  description = "Endpoint for EKS control plane"
  value       = module.eks.cluster_endpoint
}

output "cluster_security_group_id" {
  description = "Security group ids attached to the cluster control plane"
  value       = module.eks.cluster_security_group_id
}

output "cluster_iam_role_name" {
  description = "IAM role name associated with EKS cluster"
  value       = module.eks.cluster_iam_role_name
}

output "cluster_certificate_authority_data" {
  description = "Base64 encoded certificate data required to communicate with the cluster"
  value       = module.eks.cluster_certificate_authority_data
}

output "cluster_primary_security_group_id" {
  description = "The cluster primary security group ID created by the EKS cluster"
  value       = module.eks.cluster_primary_security_group_id
}

output "vpc_id" {
  description = "ID of the VPC where the cluster is deployed"
  value       = module.vpc.vpc_id
}

output "private_subnets" {
  description = "List of IDs of private subnets"
  value       = module.vpc.private_subnets
}

output "public_subnets" {
  description = "List of IDs of public subnets"
  value       = module.vpc.public_subnets
}

output "database_subnets" {
  description = "List of IDs of database subnets"
  value       = module.vpc.database_subnets
}

output "rds_cluster_endpoint" {
  description = "RDS cluster endpoint"
  value       = aws_rds_cluster.banking.endpoint
  sensitive   = true
}

output "rds_cluster_reader_endpoint" {
  description = "RDS cluster reader endpoint"
  value       = aws_rds_cluster.banking.reader_endpoint
  sensitive   = true
}

output "elasticache_primary_endpoint" {
  description = "ElastiCache primary endpoint"
  value       = aws_elasticache_replication_group.banking.primary_endpoint_address
  sensitive   = true
}

output "efs_file_system_id" {
  description = "EFS file system ID"
  value       = aws_efs_file_system.banking_shared_storage.id
}

output "s3_bucket_banking_data" {
  description = "Name of the S3 bucket for banking data"
  value       = aws_s3_bucket.banking_data.bucket
}

output "load_balancer_dns" {
  description = "DNS name of the load balancer"
  value       = aws_lb.banking_alb.dns_name
}