#!/bin/bash

# Enterprise Loan Management System - Dashboard Setup
# Automated setup for monitoring dashboards

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Logging functions
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] ✅ $1${NC}"
}

log_error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ❌ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] ⚠️  $1${NC}"
}

log_info() {
    echo -e "${CYAN}[$(date +'%Y-%m-%d %H:%M:%S')] ℹ️  $1${NC}"
}

# Wait for services to be ready
wait_for_service() {
    local service_url="$1"
    local service_name="$2"
    local timeout=300
    local count=0
    
    log "⏳ Waiting for $service_name to be ready..."
    
    while [ $count -lt $timeout ]; do
        if curl -s --max-time 5 "$service_url" >/dev/null 2>&1; then
            log_success "$service_name is ready"
            return 0
        fi
        count=$((count + 5))
        sleep 5
    done
    
    log_error "$service_name is not ready after $timeout seconds"
    return 1
}

# Setup Grafana datasources
setup_grafana_datasources() {
    log "📊 Setting up Grafana datasources..."
    
    # Wait for Grafana to be ready
    wait_for_service "http://localhost:3000/api/health" "Grafana"
    
    # Add Prometheus datasource
    curl -s -X POST \
        -H "Content-Type: application/json" \
        -d '{
            "name": "prometheus-banking",
            "type": "prometheus",
            "url": "http://prometheus:9090",
            "access": "proxy",
            "isDefault": true,
            "basicAuth": false
        }' \
        -u admin:admin \
        http://localhost:3000/api/datasources || log_warning "Prometheus datasource may already exist"
    
    # Add Elasticsearch datasource
    curl -s -X POST \
        -H "Content-Type: application/json" \
        -d '{
            "name": "elasticsearch-banking",
            "type": "elasticsearch",
            "url": "http://elasticsearch:9200",
            "access": "proxy",
            "database": "banking-*",
            "basicAuth": false,
            "jsonData": {
                "timeField": "@timestamp",
                "esVersion": "8.0.0"
            }
        }' \
        -u admin:admin \
        http://localhost:3000/api/datasources || log_warning "Elasticsearch datasource may already exist"
    
    # Add Jaeger datasource
    curl -s -X POST \
        -H "Content-Type: application/json" \
        -d '{
            "name": "jaeger-banking",
            "type": "jaeger",
            "url": "http://jaeger:16686",
            "access": "proxy",
            "basicAuth": false
        }' \
        -u admin:admin \
        http://localhost:3000/api/datasources || log_warning "Jaeger datasource may already exist"
    
    log_success "Grafana datasources configured"
}

# Import Grafana dashboards
import_grafana_dashboards() {
    log "📈 Importing Grafana dashboards..."
    
    # Import banking overview dashboard
    if [ -f "$PROJECT_ROOT/scripts/grafana/dashboards/banking-overview.json" ]; then
        local dashboard_json=$(cat "$PROJECT_ROOT/scripts/grafana/dashboards/banking-overview.json")
        
        curl -s -X POST \
            -H "Content-Type: application/json" \
            -d "{
                \"dashboard\": $dashboard_json,
                \"overwrite\": true,
                \"inputs\": [
                    {
                        \"name\": \"DS_PROMETHEUS\",
                        \"type\": \"datasource\",
                        \"pluginId\": \"prometheus\",
                        \"value\": \"prometheus-banking\"
                    }
                ]
            }" \
            -u admin:admin \
            http://localhost:3000/api/dashboards/import
        
        log_success "Banking overview dashboard imported"
    fi
    
    # Create additional dashboards
    create_system_dashboard
    create_security_dashboard
    create_compliance_dashboard
}

# Create system performance dashboard
create_system_dashboard() {
    log "⚡ Creating system performance dashboard..."
    
    cat > /tmp/system-dashboard.json << 'EOF'
{
  "dashboard": {
    "id": null,
    "title": "System Performance Dashboard",
    "tags": ["banking", "system", "performance"],
    "timezone": "",
    "panels": [
      {
        "id": 1,
        "title": "CPU Usage",
        "type": "timeseries",
        "targets": [
          {
            "expr": "100 - (avg by (instance) (irate(node_cpu_seconds_total{mode=\"idle\"}[5m])) * 100)",
            "legendFormat": "{{instance}}"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 0}
      },
      {
        "id": 2,
        "title": "Memory Usage",
        "type": "timeseries",
        "targets": [
          {
            "expr": "(1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100",
            "legendFormat": "Memory Usage %"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 0}
      },
      {
        "id": 3,
        "title": "Database Connections",
        "type": "stat",
        "targets": [
          {
            "expr": "sum(pg_stat_database_numbackends)",
            "legendFormat": "Active Connections"
          }
        ],
        "gridPos": {"h": 8, "w": 6, "x": 0, "y": 8}
      },
      {
        "id": 4,
        "title": "Redis Memory",
        "type": "stat",
        "targets": [
          {
            "expr": "redis_memory_used_bytes",
            "legendFormat": "Redis Memory"
          }
        ],
        "gridPos": {"h": 8, "w": 6, "x": 6, "y": 8}
      }
    ],
    "time": {"from": "now-1h", "to": "now"},
    "refresh": "5s"
  },
  "overwrite": true
}
EOF
    
    curl -s -X POST \
        -H "Content-Type: application/json" \
        -d @/tmp/system-dashboard.json \
        -u admin:admin \
        http://localhost:3000/api/dashboards/db
    
    rm -f /tmp/system-dashboard.json
    log_success "System performance dashboard created"
}

# Create security monitoring dashboard
create_security_dashboard() {
    log "🔒 Creating security monitoring dashboard..."
    
    cat > /tmp/security-dashboard.json << 'EOF'
{
  "dashboard": {
    "id": null,
    "title": "Security Monitoring Dashboard",
    "tags": ["banking", "security", "fapi"],
    "timezone": "",
    "panels": [
      {
        "id": 1,
        "title": "Failed Authentication Attempts",
        "type": "stat",
        "targets": [
          {
            "expr": "sum(rate(http_requests_total{status=~\"401|403\"}[5m]))",
            "legendFormat": "Failed Auth/sec"
          }
        ],
        "gridPos": {"h": 8, "w": 6, "x": 0, "y": 0}
      },
      {
        "id": 2,
        "title": "DPoP Token Validations",
        "type": "timeseries",
        "targets": [
          {
            "expr": "rate(banking_dpop_validations_total[5m])",
            "legendFormat": "{{status}}"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 6, "y": 0}
      },
      {
        "id": 3,
        "title": "FAPI Compliance Rate",
        "type": "gauge",
        "targets": [
          {
            "expr": "rate(banking_fapi_compliance_checks_total{status=\"passed\"}[5m]) / rate(banking_fapi_compliance_checks_total[5m]) * 100",
            "legendFormat": "FAPI Compliance %"
          }
        ],
        "gridPos": {"h": 8, "w": 6, "x": 18, "y": 0}
      },
      {
        "id": 4,
        "title": "Zero Trust Verifications",
        "type": "timeseries",
        "targets": [
          {
            "expr": "rate(banking_zero_trust_verifications_total[5m])",
            "legendFormat": "{{verification_type}}"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 8}
      },
      {
        "id": 5,
        "title": "Threat Detection Alerts",
        "type": "stat",
        "targets": [
          {
            "expr": "sum(banking_threat_detections_total)",
            "legendFormat": "Threats Detected"
          }
        ],
        "gridPos": {"h": 8, "w": 6, "x": 12, "y": 8}
      }
    ],
    "time": {"from": "now-1h", "to": "now"},
    "refresh": "5s"
  },
  "overwrite": true
}
EOF
    
    curl -s -X POST \
        -H "Content-Type: application/json" \
        -d @/tmp/security-dashboard.json \
        -u admin:admin \
        http://localhost:3000/api/dashboards/db
    
    rm -f /tmp/security-dashboard.json
    log_success "Security monitoring dashboard created"
}

# Create compliance dashboard
create_compliance_dashboard() {
    log "📊 Creating compliance monitoring dashboard..."
    
    cat > /tmp/compliance-dashboard.json << 'EOF'
{
  "dashboard": {
    "id": null,
    "title": "Compliance Monitoring Dashboard",
    "tags": ["banking", "compliance", "regulatory"],
    "timezone": "",
    "panels": [
      {
        "id": 1,
        "title": "PCI DSS Compliance Score",
        "type": "gauge",
        "targets": [
          {
            "expr": "banking_compliance_score{framework=\"pci_dss\"}",
            "legendFormat": "PCI DSS Score"
          }
        ],
        "gridPos": {"h": 8, "w": 6, "x": 0, "y": 0}
      },
      {
        "id": 2,
        "title": "SOX Compliance Score",
        "type": "gauge",
        "targets": [
          {
            "expr": "banking_compliance_score{framework=\"sox\"}",
            "legendFormat": "SOX Score"
          }
        ],
        "gridPos": {"h": 8, "w": 6, "x": 6, "y": 0}
      },
      {
        "id": 3,
        "title": "GDPR Compliance Score",
        "type": "gauge",
        "targets": [
          {
            "expr": "banking_compliance_score{framework=\"gdpr\"}",
            "legendFormat": "GDPR Score"
          }
        ],
        "gridPos": {"h": 8, "w": 6, "x": 12, "y": 0}
      },
      {
        "id": 4,
        "title": "Audit Events Rate",
        "type": "timeseries",
        "targets": [
          {
            "expr": "rate(banking_audit_events_total[5m])",
            "legendFormat": "{{event_type}}"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 8}
      },
      {
        "id": 5,
        "title": "Regulatory Reports Generated",
        "type": "stat",
        "targets": [
          {
            "expr": "sum(banking_regulatory_reports_total)",
            "legendFormat": "Reports"
          }
        ],
        "gridPos": {"h": 8, "w": 6, "x": 12, "y": 8}
      }
    ],
    "time": {"from": "now-24h", "to": "now"},
    "refresh": "30s"
  },
  "overwrite": true
}
EOF
    
    curl -s -X POST \
        -H "Content-Type: application/json" \
        -d @/tmp/compliance-dashboard.json \
        -u admin:admin \
        http://localhost:3000/api/dashboards/db
    
    rm -f /tmp/compliance-dashboard.json
    log_success "Compliance monitoring dashboard created"
}

# Setup Prometheus alerts
setup_prometheus_alerts() {
    log "🚨 Setting up Prometheus alerts..."
    
    # Check if AlertManager is running
    wait_for_service "http://localhost:9093" "AlertManager"
    
    # Test alert configuration
    if curl -s -X POST "http://localhost:9090/-/reload" >/dev/null 2>&1; then
        log_success "Prometheus alerts configuration reloaded"
    else
        log_warning "Could not reload Prometheus configuration"
    fi
}

# Create monitoring summary
create_monitoring_summary() {
    log "📋 Creating monitoring summary..."
    
    cat > "$PROJECT_ROOT/MONITORING-SUMMARY.md" << 'EOF'
# 📊 Enterprise Banking System - Monitoring Summary

## 🎯 Dashboard URLs

### Primary Dashboards
- **Banking Overview**: http://localhost:3000/d/banking-overview
- **System Performance**: http://localhost:3000/d/system-performance
- **Security Monitoring**: http://localhost:3000/d/security-monitoring
- **Compliance Dashboard**: http://localhost:3000/d/compliance-monitoring

### Infrastructure Monitoring
- **Grafana**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9090
- **AlertManager**: http://localhost:9093
- **Jaeger**: http://localhost:16686
- **Kibana**: http://localhost:5601

## 🔍 Key Metrics

### Business Metrics
- **Customer Count**: `banking_customers_total`
- **Active Loans**: `banking_loans_active_total`
- **Loan Volume**: `banking_loans_outstanding_amount`
- **Payment Rate**: `rate(banking_payments_total[5m])`
- **Fraud Detection Rate**: `rate(banking_fraud_detections_total[5m])`

### Security Metrics
- **Failed Authentication**: `rate(http_requests_total{status=~"401|403"}[5m])`
- **DPoP Validations**: `rate(banking_dpop_validations_total[5m])`
- **FAPI Compliance**: `banking_fapi_compliance_rate`
- **Zero Trust Verifications**: `rate(banking_zero_trust_verifications_total[5m])`

### System Metrics
- **Response Time**: `histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))`
- **Error Rate**: `rate(http_requests_total{status=~"5.."}[5m])`
- **Database Connections**: `pg_stat_database_numbackends`
- **Redis Memory**: `redis_memory_used_bytes`

### Compliance Metrics
- **PCI DSS Score**: `banking_compliance_score{framework="pci_dss"}`
- **SOX Score**: `banking_compliance_score{framework="sox"}`
- **GDPR Score**: `banking_compliance_score{framework="gdpr"}`
- **Audit Events**: `rate(banking_audit_events_total[5m])`

## 🚨 Alert Rules

### Critical Alerts
- **Service Down**: Service availability < 99%
- **High Error Rate**: Error rate > 5%
- **Database Issues**: Connection failures or slow queries
- **Security Violations**: Failed authentication spikes
- **Compliance Violations**: Compliance score < 90%

### Warning Alerts
- **High Response Time**: P95 latency > 2 seconds
- **Memory Usage**: Memory usage > 80%
- **Disk Space**: Disk usage > 85%
- **Fraud Detection**: Unusual fraud detection patterns

## 📈 SLA Targets

### Availability
- **Overall System**: 99.9% uptime
- **Critical APIs**: 99.95% uptime
- **Payment Processing**: 99.99% uptime

### Performance
- **API Response Time**: < 500ms P95
- **Payment Processing**: < 1s P95
- **Fraud Detection**: < 100ms P95

### Security
- **Authentication Success**: > 99.5%
- **FAPI Compliance**: 100%
- **Zero Trust Verification**: > 99.9%

## 🔧 Troubleshooting

### Common Issues
1. **Dashboard not loading**: Check Grafana service status
2. **No metrics**: Verify Prometheus scraping configuration
3. **Missing data**: Check service endpoints and network connectivity
4. **Alert not firing**: Verify AlertManager configuration

### Useful Commands
```bash
# Check dashboard status
./scripts/monitoring/setup-dashboards.sh status

# Reload dashboards
./scripts/monitoring/setup-dashboards.sh reload

# Test metrics
curl http://localhost:9090/api/v1/query?query=up

# Check alerts
curl http://localhost:9093/api/v1/alerts
```

## 📚 Additional Resources
- **Grafana Documentation**: https://grafana.com/docs/
- **Prometheus Documentation**: https://prometheus.io/docs/
- **Banking Metrics Guide**: Available in project documentation
- **FAPI Monitoring**: Specialized dashboards for Open Banking compliance
EOF

    log_success "Monitoring summary created: MONITORING-SUMMARY.md"
}

# Show dashboard URLs
show_dashboard_urls() {
    echo -e "${CYAN}"
    cat << 'EOF'
╔══════════════════════════════════════════════════════════════════════════════════════════════════════════╗
║                                         📊 Dashboard URLs                                               ║
╠══════════════════════════════════════════════════════════════════════════════════════════════════════════╣
║                                                                                                          ║
║  🏦 Banking Dashboards:                                                                                 ║
║    • Banking Overview:        http://localhost:3000/d/banking-overview                                 ║
║    • System Performance:      http://localhost:3000/d/system-performance                               ║
║    • Security Monitoring:     http://localhost:3000/d/security-monitoring                              ║
║    • Compliance Dashboard:    http://localhost:3000/d/compliance-monitoring                            ║
║                                                                                                          ║
║  🔧 Infrastructure:                                                                                     ║
║    • Grafana:                 http://localhost:3000 (admin/admin)                                      ║
║    • Prometheus:              http://localhost:9090                                                     ║
║    • AlertManager:            http://localhost:9093                                                     ║
║    • Jaeger Tracing:          http://localhost:16686                                                    ║
║    • Kibana Logs:             http://localhost:5601                                                     ║
║                                                                                                          ║
╚══════════════════════════════════════════════════════════════════════════════════════════════════════════╝
EOF
    echo -e "${NC}"
}

# Main execution
main() {
    cd "$PROJECT_ROOT"
    
    case "${1:-setup}" in
        "setup")
            setup_grafana_datasources
            import_grafana_dashboards
            setup_prometheus_alerts
            create_monitoring_summary
            show_dashboard_urls
            ;;
            
        "datasources")
            setup_grafana_datasources
            ;;
            
        "dashboards")
            import_grafana_dashboards
            ;;
            
        "alerts")
            setup_prometheus_alerts
            ;;
            
        "status")
            log "🔍 Checking monitoring services status..."
            curl -s http://localhost:3000/api/health && log_success "Grafana is healthy" || log_error "Grafana is not responding"
            curl -s http://localhost:9090/-/healthy && log_success "Prometheus is healthy" || log_error "Prometheus is not responding"
            curl -s http://localhost:9093/-/healthy && log_success "AlertManager is healthy" || log_error "AlertManager is not responding"
            ;;
            
        "reload")
            log "🔄 Reloading monitoring configuration..."
            curl -s -X POST http://localhost:9090/-/reload && log_success "Prometheus reloaded" || log_error "Failed to reload Prometheus"
            ;;
            
        "urls")
            show_dashboard_urls
            ;;
            
        "help"|"-h"|"--help")
            echo "Enterprise Banking System - Dashboard Setup"
            echo ""
            echo "Usage: $0 [command]"
            echo ""
            echo "Commands:"
            echo "  setup       - Complete monitoring setup (default)"
            echo "  datasources - Setup Grafana datasources only"
            echo "  dashboards  - Import dashboards only"
            echo "  alerts      - Setup Prometheus alerts only"
            echo "  status      - Check monitoring services status"
            echo "  reload      - Reload monitoring configuration"
            echo "  urls        - Show dashboard URLs"
            echo "  help        - Show this help message"
            ;;
            
        *)
            log_error "Unknown command: $1"
            echo "Use '$0 help' for available commands"
            exit 1
            ;;
    esac
}

# Execute main function
main "$@"