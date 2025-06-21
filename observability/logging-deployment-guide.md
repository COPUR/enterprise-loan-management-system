# Enterprise Banking Centralized Logging Deployment Guide

## Overview

This guide provides comprehensive instructions for deploying the centralized logging and observability infrastructure for the Enterprise Loan Management System with PCI-DSS v4 compliance and data sovereignty controls.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    Application Layer                            │
├─────────────────────────────────────────────────────────────────┤
│ Customer Management │ Loan Origination │ Payment Processing    │
│ Domain Service      │ Domain Service   │ Domain Service        │
└─────────────────────┬───────────────────┬─────────────────────┘
                      │                   │
┌─────────────────────┴───────────────────┴─────────────────────┐
│                 Logging Infrastructure                        │
├─────────────────────────────────────────────────────────────────┤
│  OpenTelemetry   │    Fluent Bit     │   Data Masking        │
│  Collector       │    Log Processor  │   Service             │
├─────────────────────────────────────────────────────────────────┤
│  Elasticsearch   │    Prometheus     │   Grafana             │
│  Event Store     │    Metrics        │   Dashboards          │
├─────────────────────────────────────────────────────────────────┤
│  Regional Data   │    SIEM           │   Compliance          │
│  Storage         │    Integration    │   Monitoring          │
└─────────────────────────────────────────────────────────────────┘
```

## Prerequisites

### System Requirements
- Docker Engine 20.10+
- Docker Compose 2.0+
- Minimum 16GB RAM
- 100GB+ available disk space
- TLS certificates for production deployment

### Network Requirements
- Ports 9200, 5601, 9090, 3000, 4317, 4318 available
- Outbound internet access for SIEM integration
- Regional network compliance (EU, US, APAC)

## Quick Start Deployment

### 1. Clone and Setup
```bash
git clone <repository-url>
cd enterprise-loan-management-system
```

### 2. Environment Configuration
```bash
# Copy environment template
cp observability/.env.template observability/.env

# Configure data residency (US, EU, or APAC)
export DATA_RESIDENCY=US
export ENVIRONMENT=production
export ELASTIC_PASSWORD=changeme123!
export GRAFANA_ADMIN_PASSWORD=admin123!
```

### 3. Generate SSL Certificates
```bash
# Generate CA and service certificates
./observability/scripts/generate-certificates.sh
```

### 4. Deploy Observability Stack
```bash
# Deploy complete observability infrastructure
docker-compose -f docker-compose.observability.yml up -d

# Verify deployment
docker-compose -f docker-compose.observability.yml ps
```

### 5. Configure Index Templates
```bash
# Apply Elasticsearch index templates
curl -X PUT "localhost:9200/_index_template/banking-logs" \
  -H "Content-Type: application/json" \
  -d @observability/elasticsearch/index-templates/banking-logs-template.json
```

## Regional Deployment Configurations

### United States (CCPA/SOX Compliance)
```bash
export DATA_RESIDENCY=US
export COMPLIANCE_FRAMEWORKS="CCPA,SOX,PCI-DSS-v4"
export RETENTION_AUDIT_LOGS="P2555D"  # 7 years
export RETENTION_TRANSACTION_LOGS="P1095D"  # 3 years
```

### European Union (GDPR Compliance)
```bash
export DATA_RESIDENCY=EU
export COMPLIANCE_FRAMEWORKS="GDPR,PCI-DSS-v4"
export GDPR_ENABLED=true
export DATA_SUBJECT_RIGHTS=true
export CROSS_BORDER_TRANSFER=false
```

### Asia-Pacific (Multi-jurisdictional)
```bash
export DATA_RESIDENCY=APAC
export COMPLIANCE_FRAMEWORKS="PDPA,APPI,Privacy-Act-1988,PCI-DSS-v4"
export MULTI_JURISDICTION=true
export PRIMARY_JURISDICTION=Australia
```

## Component Configuration

### OpenTelemetry Collector
The OTEL collector processes all logs, metrics, and traces with PCI-DSS compliance:

```yaml
# Key configuration points:
- OTLP receivers on ports 4317 (gRPC) and 4318 (HTTP)
- Automatic PCI data masking processors
- Regional data residency routing
- Compliance-aware attribute processing
```

**Health Check:**
```bash
curl http://localhost:13133/
```

### Fluent Bit Log Aggregation
Industry-standard log collection with advanced filtering:

```bash
# Check Fluent Bit status
curl http://localhost:2020/api/v1/health

# View processing metrics
curl http://localhost:2020/api/v1/metrics
```

### Elasticsearch Secure Storage
Multi-region compliant data storage:

```bash
# Verify cluster health
curl -k -u elastic:${ELASTIC_PASSWORD} \
  https://localhost:9200/_cluster/health

# Check index policies
curl -k -u elastic:${ELASTIC_PASSWORD} \
  https://localhost:9200/_ilm/policy/banking-logs-policy
```

### Prometheus Monitoring
Enhanced security monitoring with compliance metrics:

```bash
# Access Prometheus UI
https://localhost:9090

# Check targets status
curl https://localhost:9090/api/v1/targets
```

### Grafana Dashboards
PCI-DSS compliance and operational dashboards:

```bash
# Access Grafana (admin/admin123!)
https://localhost:3000

# Import banking compliance dashboard
# Dashboard ID: banking-pci-dss-compliance
```

## Security Configuration

### TLS/SSL Setup
All communications are encrypted with TLS 1.3:

```bash
# Generate certificates for production
./observability/scripts/generate-production-certs.sh

# Verify certificate validity
openssl x509 -in observability/ssl/ca.crt -text -noout
```

### Authentication & Authorization
```bash
# Elasticsearch security
curl -k -u elastic:${ELASTIC_PASSWORD} \
  https://localhost:9200/_security/user

# Grafana LDAP integration (optional)
# Configure in observability/grafana/ldap.toml
```

### Network Security
```bash
# Firewall rules (adjust for your environment)
ufw allow from 10.0.0.0/8 to any port 9200
ufw allow from 10.0.0.0/8 to any port 9090
ufw deny 9200
ufw deny 9090
```

## Data Classification and Masking

### Automatic PCI-DSS Data Masking
```bash
# Test data masking service
curl -X POST http://localhost:8081/api/v1/mask-test \
  -H "Content-Type: application/json" \
  -d '{"message": "Customer card 4111-1111-1111-1111 processed"}'

# Expected response: "Customer card ****-****-****-1111 processed"
```

### Data Classification Levels
- **PUBLIC**: No restrictions
- **INTERNAL**: Standard corporate controls
- **CONFIDENTIAL**: Access controls and monitoring
- **RESTRICTED**: Banking customer data
- **PCI-DSS**: Payment card industry data

## SIEM Integration

### Splunk Integration
```bash
# Configure Splunk HEC
export SPLUNK_HEC_URL="https://splunk.company.com:8088/services/collector"
export SPLUNK_HEC_TOKEN="your-hec-token"

# Test connectivity
curl -k "${SPLUNK_HEC_URL}/health" \
  -H "Authorization: Splunk ${SPLUNK_HEC_TOKEN}"
```

### Security Event Forwarding
```bash
# Enable SIEM forwarding
docker exec -it fluent-bit \
  /fluent-bit/bin/fluent-bit -c /fluent-bit/etc/fluent-bit-siem.conf
```

## Compliance Monitoring

### PCI-DSS v4 Requirements
-  Requirement 10.2.1: Audit logs for all user access
-  Requirement 10.3.1: Log entry integrity protection
-  Requirement 3.4.1: Encryption at rest and in transit
-  Requirement 12.10.1: Incident response procedures

### GDPR Compliance (EU)
-  Article 32: Security of processing
-  Article 33: Notification of data breaches
-  Article 17: Right to erasure implementation
-  Article 25: Data protection by design

### Automated Compliance Reporting
```bash
# Generate daily compliance report
curl -X POST http://localhost:8081/api/v1/compliance/report \
  -H "Content-Type: application/json" \
  -d '{"framework": "PCI-DSS-v4", "period": "daily"}'
```

## Performance Tuning

### Elasticsearch Optimization
```bash
# Increase heap size for production
export ES_JAVA_OPTS="-Xms4g -Xmx4g"

# Configure index lifecycle management
curl -X PUT "localhost:9200/_ilm/policy/banking-logs-policy" \
  -H "Content-Type: application/json" \
  -d @observability/elasticsearch/ilm-policy.json
```

### Fluent Bit Performance
```bash
# Adjust buffer settings for high throughput
# Edit observability/fluentbit/fluent-bit.conf:
# Buffer_Max_Size 256k
# Mem_Buf_Limit 1024MB
```

### OpenTelemetry Scaling
```bash
# Scale OTEL collector replicas
docker-compose -f docker-compose.observability.yml \
  up -d --scale otel-collector=3
```

## Monitoring and Alerting

### Critical Alerts Setup
```bash
# Apply Prometheus alerting rules
curl -X POST http://localhost:9090/-/reload

# Verify alert rules
curl http://localhost:9090/api/v1/rules
```

### Alert Notifications
```bash
# Configure Alertmanager (Slack, email, PagerDuty)
# Edit observability/alertmanager/alertmanager.yml
docker-compose restart alertmanager
```

## Backup and Disaster Recovery

### Elasticsearch Snapshots
```bash
# Configure S3 snapshot repository
curl -X PUT "localhost:9200/_snapshot/banking-backups" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "s3",
    "settings": {
      "bucket": "banking-logs-backup",
      "region": "us-east-1",
      "base_path": "elasticsearch"
    }
  }'

# Create daily snapshots
curl -X PUT "localhost:9200/_snapshot/banking-backups/daily-$(date +%Y%m%d)"
```

### Prometheus Data Backup
```bash
# Backup Prometheus data
docker exec prometheus promtool tsdb create-blocks-from-openmetrics \
  /prometheus/export.txt /prometheus/data
```

## Troubleshooting

### Common Issues

#### 1. Elasticsearch Cluster Red Status
```bash
# Check cluster allocation
curl localhost:9200/_cluster/allocation/explain

# Increase memory map limit
echo 'vm.max_map_count=262144' >> /etc/sysctl.conf
sysctl -p
```

#### 2. High Memory Usage
```bash
# Monitor container memory
docker stats --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}"

# Adjust JVM heap sizes
export ES_JAVA_OPTS="-Xms2g -Xmx2g"
```

#### 3. Log Processing Delays
```bash
# Check Fluent Bit buffer status
curl http://localhost:2020/api/v1/metrics | grep buffer

# Increase processing threads
# Edit fluent-bit.conf: Workers 8
```

#### 4. Certificate Issues
```bash
# Regenerate certificates
./observability/scripts/regenerate-certs.sh

# Verify certificate chain
openssl verify -CAfile observability/ssl/ca.crt \
  observability/ssl/elasticsearch.crt
```

### Log Analysis
```bash
# View container logs
docker-compose logs -f elasticsearch
docker-compose logs -f otel-collector
docker-compose logs -f fluent-bit

# Check application integration
tail -f logs/application.log | grep -E "(ERROR|WARN|PCI|AUDIT)"
```

## Maintenance

### Regular Tasks
```bash
# Weekly index cleanup
curl -X DELETE "localhost:9200/banking-logs-*-$(date -d '90 days ago' +%Y.%m.%d)"

# Monthly certificate renewal
./observability/scripts/renew-certificates.sh

# Quarterly compliance audit
curl -X POST http://localhost:8081/api/v1/compliance/audit \
  -d '{"scope": "full", "frameworks": ["PCI-DSS-v4", "GDPR"]}'
```

### Updates and Patches
```bash
# Update container images
docker-compose pull
docker-compose up -d

# Apply security patches
./observability/scripts/security-update.sh
```

## Production Deployment Checklist

- [ ] SSL certificates configured and valid
- [ ] Firewall rules properly configured
- [ ] Data residency settings verified
- [ ] Backup procedures tested
- [ ] Compliance reports generated successfully
- [ ] SIEM integration tested
- [ ] Alert notifications working
- [ ] Performance benchmarks met
- [ ] Security scan completed
- [ ] Documentation updated

## Support and Resources

### Documentation
- [OpenTelemetry Collector Configuration](https://opentelemetry.io/docs/collector/)
- [Elasticsearch Security Guide](https://www.elastic.co/guide/en/elasticsearch/reference/current/security-minimal-setup.html)
- [PCI-DSS v4 Requirements](https://www.pcisecuritystandards.org/)
- [GDPR Compliance Guide](https://gdpr.eu/)

### Monitoring URLs
- Elasticsearch: https://localhost:9200
- Kibana: https://localhost:5601
- Prometheus: https://localhost:9090
- Grafana: https://localhost:3000
- OpenTelemetry Health: http://localhost:13133

### Emergency Contacts
- Security Team: security@bank.com
- Compliance Team: compliance@bank.com
- DevOps Team: devops@bank.com
- Incident Response: incident@bank.com

---

**Note**: This deployment guide assumes a production environment with proper security controls. Adjust configurations based on your specific infrastructure and compliance requirements.