# Banking Health Indicators

## Overview

The Enterprise Loan Management System includes comprehensive banking-specific health indicators that extend Spring Boot Actuator to provide detailed insights into critical banking services.

## Health Indicators

### 1. Loan Processing Health Indicator

**Endpoint**: `/actuator/health/loanProcessing`

**Monitors**:
- Pending loans count
- Currently processing loans
- Average processing time
- System load metrics

**Thresholds**:
- ⚠️ Warning: 50+ pending loans
- 🚨 Critical: 100+ pending loans
- ⚠️ Warning: 10+ processing loans
- 🚨 Critical: 20+ processing loans

**Sample Response**:
```json
{
  "status": "UP",
  "details": {
    "pendingLoans": 15,
    "processingLoans": 3,
    "averageProcessingTime": "3.5s",
    "systemLoad": "LOW",
    "status": "HEALTHY"
  }
}
```

### 2. Payment System Health Indicator

**Endpoint**: `/actuator/health/paymentSystem`

**Monitors**:
- Redis connectivity
- Recent payments throughput
- Payment queue status
- System heartbeat

**Thresholds**:
- ⚠️ Warning: <5 payments/hour
- 🚨 Critical: <1 payment/hour
- ⚠️ Warning: 50+ items in queue
- 🚨 Critical: 100+ items in queue

**Sample Response**:
```json
{
  "status": "UP",
  "details": {
    "redisConnectivity": "CONNECTED",
    "recentPayments": 25,
    "paymentQueue": "NORMAL (5 items)",
    "paymentThroughput": "15.5 payments/min",
    "status": "OPERATIONAL"
  }
}
```

### 3. Compliance Service Health Indicator

**Endpoint**: `/actuator/health/complianceService`

**Monitors**:
- Compliance checks activity
- FAPI compliance status
- Audit trail health
- Regulatory framework status

**Thresholds**:
- ⚠️ Warning: <5 compliance checks/hour
- 🚨 Critical: <1 compliance check/hour
- 🚨 Critical: Any FAPI violations

**Sample Response**:
```json
{
  "status": "UP",
  "details": {
    "complianceChecks": 12,
    "fapiCompliance": "ACTIVE",
    "auditTrail": "ACTIVE",
    "regulatoryFramework": "ALL_FRAMEWORKS_COMPLIANT",
    "status": "COMPLIANT"
  }
}
```

### 4. Fraud Detection Health Indicator

**Endpoint**: `/actuator/health/fraudDetection`

**Monitors**:
- Fraud detection engine status
- ML model health
- Detection accuracy
- Suspicious activities

**Thresholds**:
- ⚠️ Warning: <90% detection accuracy
- 🚨 Critical: <85% detection accuracy
- ⚠️ Warning: <10 fraud checks/hour
- 🚨 Critical: <1 fraud check/hour

**Sample Response**:
```json
{
  "status": "UP",
  "details": {
    "engineStatus": "ACTIVE",
    "fraudChecks": 45,
    "mlModelStatus": "OPERATIONAL",
    "suspiciousActivities": 2,
    "detectionAccuracy": "96.5%",
    "status": "PROTECTING"
  }
}
```

### 5. Customer Service Health Indicator

**Endpoint**: `/actuator/health/customerService`

**Monitors**:
- Service availability
- Response times
- Customer operations
- Customer satisfaction

**Thresholds**:
- ⚠️ Warning: >5s average response time
- 🚨 Critical: >10s average response time
- ⚠️ Warning: <20 operations/hour
- 🚨 Critical: <10 operations/hour

**Sample Response**:
```json
{
  "status": "UP",
  "details": {
    "customerOperations": 75,
    "averageResponseTime": "2.3s",
    "serviceAvailability": "AVAILABLE",
    "activeSessions": 28,
    "customerSatisfaction": "4.2/5.0",
    "status": "AVAILABLE"
  }
}
```

## Health Groups

### Banking Group
**Endpoint**: `/actuator/health/banking`

Includes all banking-specific health indicators for comprehensive system health.

### Core Group
**Endpoint**: `/actuator/health/core`

Includes only critical services (loans and payments) for high-frequency monitoring.

### Security Group
**Endpoint**: `/actuator/health/security`

Includes compliance and fraud detection services for security monitoring.

### Readiness Check
**Endpoint**: `/actuator/health/readiness`

Kubernetes readiness probe - determines if the service is ready to accept traffic.

### Liveness Check
**Endpoint**: `/actuator/health/liveness`

Kubernetes liveness probe - determines if the service should be restarted.

## Prometheus Metrics

All health indicators export metrics to Prometheus:

### Health Status Metrics
```
banking_health_status_total{service="loan-processing",status="up"}
banking_health_status_total{service="payment-system",status="down"}
```

### Health Check Duration
```
banking_health_check_duration_seconds{service="loan-processing"}
```

### Business Metrics
```
banking_loans_pending
banking_payments_recent
banking_compliance_checks
banking_fraud_checks
banking_customer_operations
```

## Configuration

Health indicators can be configured in `application-health.yml`:

```yaml
banking:
  health:
    thresholds:
      loan-processing:
        pending-loans-critical: 100
        processing-time-critical: 600s
      payment-system:
        throughput-critical: 1
        queue-size-critical: 100
```

## Alerting Integration

Health indicators integrate with Prometheus AlertManager for automated alerting:

### Critical Alerts
- Service DOWN status
- Threshold breaches
- FAPI compliance violations
- Fraud detection failures

### Warning Alerts
- High response times
- Queue backlogs
- Low activity levels
- Performance degradation

## Security

Health endpoints are secured with Spring Security:

- Authentication required for detailed health information
- Role-based access control
- Audit logging for all health check access

## Usage Examples

### Check Overall Banking Health
```bash
curl -u health-monitor:password \
  http://localhost:8080/actuator/health/banking
```

### Check Specific Service
```bash
curl -u health-monitor:password \
  http://localhost:8080/actuator/health/loanProcessing
```

### Kubernetes Health Probes
```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 5
  periodSeconds: 5
```

### Grafana Dashboard Queries
```promql
# Health status by service
banking_health_status_total

# Average health check duration
rate(banking_health_check_duration_seconds_sum[5m]) / 
rate(banking_health_check_duration_seconds_count[5m])

# Current pending loans
banking_loans_pending

# Payment system throughput
rate(banking_payments_recent[1h])
```

## Troubleshooting

### Common Issues

1. **Database Connectivity**
   - Check database connection pools
   - Verify network connectivity
   - Review authentication credentials

2. **Redis Connectivity**
   - Verify Redis cluster health
   - Check network policies
   - Review connection timeouts

3. **High Response Times**
   - Monitor database query performance
   - Check for resource contention
   - Review application logs

4. **FAPI Compliance Issues**
   - Check security configuration
   - Verify certificate validity
   - Review audit logs

### Diagnostic Commands

```bash
# Check all health indicators
curl -u health-monitor:password \
  http://localhost:8080/actuator/health | jq

# Check metrics
curl http://localhost:8080/actuator/prometheus | grep banking_health

# Review logs
kubectl logs -f deployment/loan-management-system | grep "health"
```

## Performance Considerations

- Health checks are cached (default 30s TTL)
- Database queries are optimized for performance
- Redis operations use connection pooling
- Metrics collection is asynchronous
- Health check timeouts are configurable

## Best Practices

1. **Monitor Regularly**: Set up automated monitoring dashboards
2. **Set Appropriate Thresholds**: Tune thresholds based on business requirements
3. **Implement Alerting**: Configure alerts for critical health events
4. **Review Logs**: Regularly review health check logs for patterns
5. **Performance Testing**: Test health indicators under load
6. **Documentation**: Keep health indicator documentation updated
7. **Security**: Protect health endpoints with appropriate authentication