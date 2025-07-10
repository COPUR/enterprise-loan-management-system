# ADR-009: AWS EKS Infrastructure Design

## Status
**ACCEPTED** - Enterprise Banking Cloud Infrastructure

## Date
2025-01-08

## Context

The Enterprise Loan Management System requires a robust, scalable, and secure cloud infrastructure capable of handling enterprise banking workloads with strict compliance requirements. The system must support:

- **Regulatory Compliance**: PCI DSS, SOX, GDPR, FAPI 2.0
- **High Availability**: 99.999% uptime SLA
- **Security**: Zero-trust architecture, data encryption, audit trails
- **Scalability**: Auto-scaling based on demand
- **Multi-tenancy**: Support for multiple banking entities
- **Disaster Recovery**: RTO < 15 minutes, RPO < 5 minutes
- **Cost Optimization**: Efficient resource utilization

## Decision

We will implement a comprehensive AWS EKS (Elastic Kubernetes Service) infrastructure with the following architecture:

### 1. Multi-Region Architecture
- **Primary Region**: us-east-1 (N. Virginia) 
- **Secondary Region**: us-west-2 (Oregon)
- **DR Region**: eu-west-1 (Ireland)

### 2. EKS Cluster Design
- **Production Clusters**: 3 clusters across 3 AZs per region
- **Staging Clusters**: 2 clusters across 2 AZs
- **Development Clusters**: 1 cluster, single AZ

### 3. Network Architecture
- **VPC**: Multi-AZ with private and public subnets
- **Security Groups**: Granular access control
- **Network ACLs**: Additional layer of security
- **NAT Gateways**: High availability internet access
- **Transit Gateway**: Multi-VPC connectivity

### 4. Security Implementation
- **IAM Roles**: Fine-grained permissions with RBAC
- **KMS**: Customer-managed keys for encryption
- **Secrets Manager**: Credential management
- **Certificate Manager**: SSL/TLS certificate automation
- **GuardDuty**: Threat detection
- **Security Hub**: Centralized security findings

### 5. Storage and Data
- **EFS**: Shared persistent storage
- **EBS**: High-performance block storage
- **S3**: Object storage with versioning and encryption
- **RDS**: Aurora PostgreSQL with read replicas
- **ElastiCache**: Redis for caching
- **DynamoDB**: Configuration and session storage

### 6. Monitoring and Observability
- **CloudWatch**: Metrics, logs, and alarms
- **X-Ray**: Distributed tracing
- **Prometheus**: Kubernetes metrics
- **Grafana**: Visualization and dashboards
- **ElasticSearch**: Log aggregation and search

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              AWS GLOBAL INFRASTRUCTURE                          │
│                                                                                 │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐             │
│  │   us-east-1     │    │   us-west-2     │    │   eu-west-1     │             │
│  │   (Primary)     │    │  (Secondary)    │    │     (DR)        │             │
│  │                 │    │                 │    │                 │             │
│  │ ┌─────────────┐ │    │ ┌─────────────┐ │    │ ┌─────────────┐ │             │
│  │ │     EKS     │ │    │ │     EKS     │ │    │ │     EKS     │ │             │
│  │ │  Production │ │    │ │  Production │ │    │ │     DR      │ │             │
│  │ │   Cluster   │ │    │ │   Cluster   │ │    │ │   Cluster   │ │             │
│  │ │             │ │    │ │             │ │    │ │             │ │             │
│  │ │  3 AZs      │ │    │ │  3 AZs      │ │    │ │  2 AZs      │ │             │
│  │ │  6 Nodes    │ │    │ │  6 Nodes    │ │    │ │  4 Nodes    │ │             │
│  │ └─────────────┘ │    │ └─────────────┘ │    │ └─────────────┘ │             │
│  │                 │    │                 │    │                 │             │
│  │ ┌─────────────┐ │    │ ┌─────────────┐ │    │                 │             │
│  │ │   Staging   │ │    │ │   Staging   │ │    │                 │             │
│  │ │   Cluster   │ │    │ │   Cluster   │ │    │                 │             │
│  │ │   2 AZs     │ │    │ │   2 AZs     │ │    │                 │             │
│  │ │   4 Nodes   │ │    │ │   4 Nodes   │ │    │                 │             │
│  │ └─────────────┘ │    │ └─────────────┘ │    │                 │             │
│  └─────────────────┘    └─────────────────┘    └─────────────────┘             │
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                        SHARED SERVICES                                 │   │
│  │                                                                         │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │   │
│  │  │     IAM     │  │     KMS     │  │  Secrets    │  │Certificate  │    │   │
│  │  │   Roles     │  │   Keys      │  │  Manager    │  │  Manager    │    │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘    │   │
│  │                                                                         │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │   │
│  │  │ GuardDuty   │  │Security Hub │  │ CloudTrail  │  │   Config    │    │   │
│  │  │             │  │             │  │             │  │             │    │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘    │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## Implementation Details

### 1. VPC Configuration

```yaml
# Primary VPC (us-east-1)
VPC_CIDR: 10.0.0.0/16

Subnets:
  Public:
    - 10.0.1.0/24 (AZ-a)
    - 10.0.2.0/24 (AZ-b)
    - 10.0.3.0/24 (AZ-c)
  
  Private:
    - 10.0.10.0/24 (AZ-a)
    - 10.0.11.0/24 (AZ-b)
    - 10.0.12.0/24 (AZ-c)
  
  Database:
    - 10.0.20.0/24 (AZ-a)
    - 10.0.21.0/24 (AZ-b)
    - 10.0.22.0/24 (AZ-c)
```

### 2. EKS Cluster Specifications

```yaml
Production Cluster:
  Version: 1.28
  Endpoint: Private
  Node Groups:
    - Banking Services:
        Instance Type: c5.2xlarge
        Min Size: 3
        Max Size: 12
        Desired: 6
    - AI/ML Workloads:
        Instance Type: g4dn.xlarge
        Min Size: 2
        Max Size: 8
        Desired: 4
    - Compliance Services:
        Instance Type: m5.xlarge
        Min Size: 2
        Max Size: 6
        Desired: 3

Staging Cluster:
  Version: 1.28
  Endpoint: Private
  Node Groups:
    - General Purpose:
        Instance Type: c5.large
        Min Size: 2
        Max Size: 8
        Desired: 4
```

### 3. Security Configuration

```yaml
IAM Roles:
  EKS Cluster Service Role:
    Policies:
      - AmazonEKSClusterPolicy
      - Custom Banking Compliance Policy
  
  Node Group Role:
    Policies:
      - AmazonEKSWorkerNodePolicy
      - AmazonEKS_CNI_Policy
      - AmazonEC2ContainerRegistryReadOnly
  
  Pod Execution Role:
    Policies:
      - Custom Pod Security Policy
      - Banking Service Access Policy

KMS Keys:
  - EKS Secrets Encryption
  - EBS Volume Encryption
  - S3 Bucket Encryption
  - RDS Encryption
  - CloudWatch Logs Encryption

Security Groups:
  EKS Control Plane:
    Ingress:
      - Port 443: From Node Groups
      - Port 10250: From Node Groups
    Egress:
      - All traffic to Node Groups
  
  Node Groups:
    Ingress:
      - Port 443: From Control Plane
      - Port 1025-65535: From other nodes
      - Port 53: DNS
    Egress:
      - All traffic to internet (via NAT)
      - All traffic to Control Plane
```

### 4. Storage Configuration

```yaml
EFS File Systems:
  Shared Application Data:
    Performance Mode: General Purpose
    Throughput Mode: Provisioned (500 MB/s)
    Encryption: Enabled
    Backup: Daily

EBS Volumes:
  Node Storage:
    Type: gp3
    Size: 100GB
    IOPS: 3000
    Encryption: Enabled
  
  Database Storage:
    Type: io2
    Size: 1TB
    IOPS: 10000
    Encryption: Enabled

S3 Buckets:
  Application Artifacts:
    Versioning: Enabled
    Encryption: SSE-KMS
    Lifecycle: 90 days to IA, 365 days to Glacier
  
  Audit Logs:
    Versioning: Enabled
    Encryption: SSE-KMS
    Retention: 7 years
    Compliance: WORM
```

### 5. Database Configuration

```yaml
RDS Aurora PostgreSQL:
  Engine Version: 13.7
  Instance Class: db.r6g.2xlarge
  Multi-AZ: true
  Read Replicas: 2 per region
  Backup Retention: 35 days
  Point-in-Time Recovery: Enabled
  Encryption: KMS
  Performance Insights: Enabled

ElastiCache Redis:
  Engine Version: 7.0
  Node Type: cache.r6g.large
  Cluster Mode: Enabled
  Replicas: 2 per shard
  Shards: 3
  Encryption: In-transit and at-rest
  Backup: Daily snapshots

DynamoDB:
  Tables:
    - Session Store
    - Configuration Store
    - Audit Trail
  Encryption: Customer managed KMS
  Point-in-Time Recovery: Enabled
  Backup: On-demand + scheduled
```

### 6. Networking and Connectivity

```yaml
Transit Gateway:
  - Connect Production VPCs
  - Connect to on-premises via VPN
  - Route table isolation

VPC Peering:
  - Cross-region replication
  - Disaster recovery connectivity

NAT Gateways:
  - High availability (one per AZ)
  - Bandwidth: 45 Gbps

Internet Gateway:
  - Public subnet internet access
  - ALB/NLB connectivity

VPC Endpoints:
  - S3 Gateway Endpoint
  - DynamoDB Gateway Endpoint
  - Interface Endpoints:
    - EC2, ECR, KMS, Secrets Manager
    - CloudWatch, X-Ray
```

### 7. Load Balancing and Ingress

```yaml
Application Load Balancer:
  Scheme: Internet-facing
  Security Groups: ALB-SG
  Target Groups:
    - Banking API (HTTPS:443)
    - Health Checks (/health)
  
  Listeners:
    - HTTPS:443 with SSL termination
    - HTTP:80 redirect to HTTPS

Network Load Balancer:
  Scheme: Internal
  Cross-zone Load Balancing: Enabled
  Target Groups:
    - Internal Services
    - Database connections

Kubernetes Ingress:
  Controller: AWS Load Balancer Controller
  Annotations:
    - alb.ingress.kubernetes.io/scheme: internet-facing
    - alb.ingress.kubernetes.io/ssl-policy: ELBSecurityPolicy-TLS-1-2-2017-01
    - alb.ingress.kubernetes.io/certificate-arn: ${CERTIFICATE_ARN}
```

### 8. Auto Scaling Configuration

```yaml
Cluster Autoscaler:
  Version: 1.28.0
  Configuration:
    scale-down-enabled: true
    scale-down-delay-after-add: 10m
    scale-down-unneeded-time: 10m
    skip-nodes-with-local-storage: false

Horizontal Pod Autoscaler:
  Banking Services:
    Min Replicas: 3
    Max Replicas: 20
    Metrics:
      - CPU: 70%
      - Memory: 80%
      - Custom: Queue Length

Vertical Pod Autoscaler:
  Update Mode: Auto
  Resource Policy:
    Banking Services: CPU 100m-2000m, Memory 256Mi-4Gi
    AI Services: CPU 500m-4000m, Memory 1Gi-8Gi
```

### 9. Monitoring and Alerting

```yaml
CloudWatch:
  Metrics:
    - EKS Cluster Health
    - Node Group Utilization
    - Pod Performance
    - Application Metrics
  
  Alarms:
    - High CPU (>80% for 5 minutes)
    - High Memory (>85% for 5 minutes)
    - Pod Restarts (>3 in 10 minutes)
    - Failed Health Checks

Prometheus:
  Deployment: Kubernetes native
  Storage: EBS persistent volumes
  Retention: 30 days
  Scrape Interval: 15s
  
  Metrics:
    - kube-state-metrics
    - node-exporter
    - Banking application metrics

Grafana:
  Deployment: Kubernetes
  Authentication: OIDC with AWS SSO
  Dashboards:
    - Kubernetes Cluster Overview
    - Banking Application Metrics
    - Infrastructure Health
    - Compliance Dashboards

AWS X-Ray:
  Tracing: Enabled for all services
  Sampling Rate: 10%
  Retention: 30 days
  Service Map: Full topology
```

### 10. Backup and Disaster Recovery

```yaml
EKS Backup Strategy:
  Velero:
    Storage: S3 buckets
    Schedule: Daily at 2 AM UTC
    Retention: 30 days
    Cross-region replication: Enabled

Database Backup:
  RDS:
    Automated: Daily, 35-day retention
    Manual: Before major updates
    Cross-region: Automated snapshots
  
  DynamoDB:
    Point-in-time recovery: Enabled
    On-demand backups: Weekly
    Cross-region replication: Active

Application Data:
  EFS: AWS Backup daily
  EBS: Snapshot lifecycle policy
  S3: Cross-region replication

Disaster Recovery:
  RTO: 15 minutes
  RPO: 5 minutes
  Testing: Monthly DR drills
  Documentation: Runbook automation
```

## Cost Optimization

### 1. Instance Selection
- **Spot Instances**: 30% of non-critical workloads
- **Reserved Instances**: 50% baseline capacity
- **Savings Plans**: Compute optimization

### 2. Resource Right-sizing
- **VPA**: Automatic resource adjustment
- **Cluster Autoscaler**: Dynamic scaling
- **Scheduled Scaling**: Predictable patterns

### 3. Storage Optimization
- **EBS gp3**: Cost-effective high performance
- **S3 Intelligent Tiering**: Automatic cost optimization
- **EFS Intelligent Tiering**: Infrequent access optimization

### 4. Network Cost Control
- **VPC Endpoints**: Reduce NAT Gateway costs
- **CloudFront**: Global content delivery
- **Direct Connect**: Predictable network costs

## Security Implementation

### 1. Network Security
```yaml
Security Groups:
  Banking-API-SG:
    Inbound:
      - 443/tcp from ALB-SG
      - 8080/tcp from ALB-SG
    Outbound:
      - 443/tcp to 0.0.0.0/0
      - 5432/tcp to DB-SG
  
  Database-SG:
    Inbound:
      - 5432/tcp from Banking-API-SG
      - 6379/tcp from Banking-API-SG
    Outbound: None

Network ACLs:
  Public Subnet:
    Inbound:
      - 443/tcp from 0.0.0.0/0
      - 80/tcp from 0.0.0.0/0
    Outbound:
      - 443/tcp to 0.0.0.0/0
      - 80/tcp to 0.0.0.0/0
  
  Private Subnet:
    Inbound:
      - All from VPC CIDR
    Outbound:
      - 443/tcp to 0.0.0.0/0
```

### 2. Identity and Access Management
```yaml
RBAC Policies:
  Banking Developers:
    Resources: ["pods", "services", "deployments"]
    Verbs: ["get", "list", "create", "update", "patch"]
    Namespaces: ["banking-dev", "banking-staging"]
  
  Production Operators:
    Resources: ["*"]
    Verbs: ["get", "list", "watch"]
    Namespaces: ["banking-prod"]
  
  Security Team:
    Resources: ["*"]
    Verbs: ["*"]
    Namespaces: ["*"]

Pod Security Standards:
  Level: Restricted
  Policies:
    - No privileged containers
    - No host network/PID/IPC
    - Read-only root filesystem
    - Non-root user required
    - Resource limits mandatory
```

### 3. Encryption
```yaml
Encryption at Rest:
  EBS Volumes: KMS encryption
  EFS: KMS encryption
  S3 Buckets: SSE-KMS
  RDS: KMS encryption
  Secrets: KMS encryption

Encryption in Transit:
  Service Mesh: mTLS
  Database: SSL/TLS
  Cache: TLS
  External APIs: TLS 1.3
  Load Balancers: SSL termination
```

## Compliance Requirements

### 1. PCI DSS Compliance
- **Network Segmentation**: Isolated subnets for card data
- **Encryption**: All data encrypted at rest and in transit
- **Access Control**: Strict IAM policies and RBAC
- **Monitoring**: Comprehensive logging and alerting
- **Vulnerability Scanning**: Regular security assessments

### 2. SOX Compliance
- **Change Management**: GitOps with approval workflows
- **Audit Trails**: Immutable logs in CloudTrail
- **Access Reviews**: Quarterly IAM audits
- **Segregation of Duties**: Role-based access control
- **Data Integrity**: Backup and recovery validation

### 3. GDPR Compliance
- **Data Sovereignty**: Regional data residency
- **Data Encryption**: Customer-managed keys
- **Access Logging**: Detailed audit trails
- **Data Retention**: Automated lifecycle policies
- **Right to be Forgotten**: Data deletion capabilities

## Implementation Phases

### Phase 1: Foundation Infrastructure (Weeks 1-2)
1. **VPC Setup**: Multi-AZ networking with security groups
2. **IAM Configuration**: Roles, policies, and access management
3. **Security Services**: GuardDuty, Security Hub, Config
4. **Base Monitoring**: CloudWatch, CloudTrail setup

### Phase 2: EKS Cluster Deployment (Weeks 3-4)
1. **Cluster Creation**: Production and staging environments
2. **Node Groups**: Auto-scaling configurations
3. **Networking**: CNI, load balancers, ingress controllers
4. **Storage**: EFS, EBS, and persistent volume setup

### Phase 3: Database and Storage (Weeks 5-6)
1. **RDS Aurora**: Multi-AZ PostgreSQL with read replicas
2. **ElastiCache**: Redis cluster with high availability
3. **S3 Buckets**: Application data and backup storage
4. **DynamoDB**: Configuration and session management

### Phase 4: Security Hardening (Weeks 7-8)
1. **Pod Security**: Standards and admission controllers
2. **Network Policies**: Kubernetes-native security
3. **Secrets Management**: Integration with AWS Secrets Manager
4. **Certificate Management**: Automated SSL/TLS

### Phase 5: Monitoring and Observability (Weeks 9-10)
1. **Prometheus Setup**: Metrics collection and storage
2. **Grafana Deployment**: Dashboards and visualization
3. **Log Aggregation**: ELK stack or CloudWatch Logs Insights
4. **Distributed Tracing**: AWS X-Ray integration

### Phase 6: Disaster Recovery (Weeks 11-12)
1. **Cross-region Setup**: Secondary region deployment
2. **Backup Configuration**: Automated backup strategies
3. **DR Testing**: Regular disaster recovery drills
4. **Runbook Creation**: Automated recovery procedures

## Maintenance and Operations

### 1. Regular Maintenance
- **Cluster Upgrades**: Quarterly Kubernetes version updates
- **Node Patching**: Monthly security updates
- **Certificate Rotation**: Automated every 90 days
- **Backup Verification**: Weekly restore testing

### 2. Security Operations
- **Vulnerability Scanning**: Daily container scans
- **Penetration Testing**: Quarterly assessments
- **Security Reviews**: Monthly access audits
- **Incident Response**: 24/7 SOC monitoring

### 3. Performance Optimization
- **Resource Analysis**: Weekly capacity planning
- **Cost Reviews**: Monthly cost optimization
- **Performance Tuning**: Continuous optimization
- **Scaling Analysis**: Load testing and planning

## Risk Mitigation

### 1. Technical Risks
- **Single Point of Failure**: Multi-AZ and multi-region deployment
- **Data Loss**: Multiple backup strategies and replication
- **Security Breaches**: Defense-in-depth security model
- **Performance Degradation**: Auto-scaling and monitoring

### 2. Operational Risks
- **Human Error**: Infrastructure as Code and automation
- **Knowledge Gaps**: Comprehensive documentation and training
- **Vendor Lock-in**: Kubernetes-native solutions
- **Compliance Violations**: Continuous compliance monitoring

### 3. Business Risks
- **Cost Overruns**: Budget monitoring and alerts
- **Service Disruption**: High availability architecture
- **Regulatory Changes**: Flexible compliance framework
- **Skill Shortage**: Cross-training and knowledge sharing

## Success Metrics

### 1. Availability Metrics
- **Uptime**: 99.999% availability target
- **MTTR**: Mean Time to Recovery < 15 minutes
- **MTBF**: Mean Time Between Failures > 720 hours

### 2. Performance Metrics
- **Response Time**: API calls < 100ms p95
- **Throughput**: 10,000 requests/second sustained
- **Resource Utilization**: CPU < 70%, Memory < 80%

### 3. Security Metrics
- **Security Incidents**: Zero security breaches
- **Compliance Score**: 100% audit compliance
- **Vulnerability Remediation**: < 24 hours for critical

### 4. Cost Metrics
- **Cost per Transaction**: Optimized to budget targets
- **Resource Efficiency**: > 85% utilization
- **Cost Predictability**: < 5% variance from budget

## Consequences

### Positive Outcomes
- **Scalability**: Automatic scaling based on demand
- **Reliability**: High availability with disaster recovery
- **Security**: Enterprise-grade security and compliance
- **Cost Efficiency**: Optimized resource utilization
- **Operational Excellence**: Automated operations and monitoring

### Trade-offs
- **Complexity**: Increased operational complexity
- **Learning Curve**: Team training on AWS and Kubernetes
- **Vendor Dependency**: AWS-specific services
- **Initial Cost**: Higher upfront infrastructure investment

### Mitigation Strategies
- **Training Programs**: Comprehensive team training
- **Documentation**: Detailed operational procedures
- **Automation**: Reduce manual operations
- **Monitoring**: Comprehensive observability stack

## Related ADRs
- ADR-010: Active-Active Architecture
- ADR-007: Docker Multi-Stage Architecture
- ADR-011: Multi-Entity Banking Architecture

## References
- [AWS EKS Best Practices](https://aws.github.io/aws-eks-best-practices/)
- [Kubernetes Security Best Practices](https://kubernetes.io/docs/concepts/security/)
- [AWS Well-Architected Framework](https://aws.amazon.com/architecture/well-architected/)
- [PCI DSS Requirements](https://www.pcisecuritystandards.org/)
- [FAPI Security Profile](https://openid.net/specs/fapi-2_0-security-profile.html)