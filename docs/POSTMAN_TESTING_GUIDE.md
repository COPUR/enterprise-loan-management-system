# Postman Testing Guide - Enterprise Banking System

## Overview

This guide provides comprehensive instructions for testing the Enterprise Banking System APIs using Postman. The testing suite has been updated and refactored to work with the current application structure.

## 📁 Available Test Collections

### 1. **Updated-Enterprise-Banking-API.postman_collection.json**
- **Purpose**: Main API testing collection for the banking system
- **Authentication**: Basic Auth (admin/admin)
- **Scope**: Complete API endpoint coverage
- **Status**: ✅ Updated for current codebase

### 2. **Standalone-API-Tests.postman_collection.json**
- **Purpose**: Connectivity and structure testing without full app
- **Authentication**: None required
- **Scope**: Basic connectivity, mock services, API structure
- **Status**: ✅ Working and validated

### 3. **Legacy Collections** (for reference)
- Enhanced-Enterprise-Banking-System.postman_collection.json
- Tools/api-testing/Enterprise-Loan-Management-*.postman_collection.json

## 🚀 Quick Start

### Option 1: Run Standalone Tests (Recommended)
```bash
# No application startup required
./scripts/test/run-standalone-tests.sh all

# Or run specific test groups
./scripts/test/run-standalone-tests.sh mock
./scripts/test/run-standalone-tests.sh connectivity
./scripts/test/run-standalone-tests.sh structure
```

### Option 2: Run Full API Tests
```bash
# Requires application to be running
./scripts/test/updated-postman-tests.sh basic

# Or run specific test groups
./scripts/test/updated-postman-tests.sh health
./scripts/test/updated-postman-tests.sh loan
./scripts/test/updated-postman-tests.sh all
```

## 📋 Test Collection Details

### Standalone API Tests

#### 🔗 Connectivity Tests
- **Port 8080 Connectivity**: Tests if the application port is accessible
- **Health Endpoint**: Checks actuator/health endpoint availability
- **API Base Path**: Validates basic API structure

#### 🧪 Mock API Tests
- **Mock Health Check**: External service to verify network connectivity
- **Mock JSON Response**: Validates JSON parsing and structure handling

#### 🔍 API Structure Validation
- **Loan API Structure**: Tests loan endpoint paths and responses
- **Error Handling**: Validates API error response formats

### Updated Enterprise Banking API

#### 🏥 Health & Monitoring
- **Application Health Check**: GET /actuator/health
- **Application Info**: GET /actuator/info
- **Application Metrics**: GET /actuator/metrics

#### 🏦 Loan Management API
- **Create Loan Application**: POST /api/v1/loans
- **Get Loan by ID**: GET /api/v1/loans/{loanId}
- **Approve Loan**: POST /api/v1/loans/{loanId}/approve
- **Disburse Loan**: POST /api/v1/loans/{loanId}/disburse
- **Make Loan Payment**: POST /api/v1/loans/{loanId}/payments
- **Reject Loan**: POST /api/v1/loans/{loanId}/reject

#### 🗄️ Database Console
- **H2 Database Console**: GET /h2-console

#### 🧪 API Testing Utilities
- **Generate Test Data**: POST /api/v1/test-data/generate

## 🔧 Configuration

### Environment Variables

#### Updated-Enterprise-Environment.postman_environment.json
```json
{
  "base_url": "http://localhost:8080",
  "api_version": "v1",
  "auth_username": "admin",
  "auth_password": "admin",
  "test_customer_id": "CUST-TEST-12345",
  "test_loan_id": "LOAN-TEST-67890"
}
```

### Authentication Setup

#### Basic Authentication
- **Username**: admin
- **Password**: admin
- **Applied to**: All main API endpoints

#### Headers
- **Content-Type**: application/json
- **X-Correlation-ID**: Auto-generated UUID for request tracking

## 📊 Test Results & Reporting

### Result Files Location
```
test-results/
├── standalone-test-report.html          # HTML report for standalone tests
├── standalone-results.json              # JSON results for standalone tests
├── connectivity-results.json            # Connectivity test results
├── mock-results.json                    # Mock API test results
├── structure-results.json               # API structure test results
├── complete-test-results.html           # Full API test HTML report
└── complete-test-results.json           # Full API test JSON results
```

### Interpreting Results

#### Successful Test Example
```json
{
  "run": {
    "stats": {
      "tests": {
        "total": 4,
        "passed": 4,
        "failed": 0
      },
      "assertions": {
        "total": 8,
        "passed": 8,
        "failed": 0
      }
    }
  }
}
```

#### Test Status Meanings
- **✅ PASS**: Test completed successfully
- **❌ FAIL**: Test failed (check error details)
- **⚠️ SKIP**: Test skipped (usually due to preconditions)

## 🔍 Troubleshooting

### Common Issues

#### 1. Newman Not Found
```bash
# Solution: Install Newman globally
npm install -g newman newman-reporter-htmlextra
```

#### 2. Application Not Running
```bash
# Solution: Start the application first
./gradlew bootRun

# Or use standalone tests that don't require the app
./scripts/test/run-standalone-tests.sh mock
```

#### 3. Connection Refused
```bash
# Check if port 8080 is available
netstat -an | grep 8080

# Or use a different port in environment variables
```

#### 4. Authentication Failures
- Verify credentials in environment file
- Check if security is enabled in application.yml
- Try without authentication for health endpoints

### Application Startup Issues

#### Compilation Errors
```bash
# Check for missing dependencies
./gradlew dependencies

# Build without tests to identify issues
./gradlew build -x test
```

#### Port Already in Use
```bash
# Find process using port 8080
lsof -i :8080

# Kill the process
kill -9 <PID>
```

## 🚀 Advanced Usage

### Running Tests in CI/CD

#### GitHub Actions Example
```yaml
- name: Run API Tests
  run: |
    ./scripts/test/run-standalone-tests.sh all
    ./scripts/test/updated-postman-tests.sh health
```

### Custom Test Scenarios

#### Load Testing
```bash
# Run tests with iterations
newman run postman/Updated-Enterprise-Banking-API.postman_collection.json \
  --environment postman/Updated-Enterprise-Environment.postman_environment.json \
  --iteration-count 100 \
  --delay-request 500
```

#### Performance Testing
```bash
# Add timing assertions
newman run postman/Updated-Enterprise-Banking-API.postman_collection.json \
  --environment postman/Updated-Enterprise-Environment.postman_environment.json \
  --timeout 5000 \
  --reporters cli,json
```

### Integration with Monitoring

#### Export Results to Grafana
```bash
# Convert JSON results to metrics format
jq '.run.stats' test-results/standalone-results.json > metrics.json
```

## 📈 Test Coverage

### Current Coverage

#### API Endpoints
- ✅ Health endpoints (100%)
- ✅ Loan management (85%)
- ⚠️ Customer management (pending compilation fixes)
- ⚠️ Payment processing (pending compilation fixes)

#### Test Types
- ✅ Connectivity tests
- ✅ Mock API tests
- ✅ Structure validation
- ✅ Error handling
- ✅ Authentication flow
- ⚠️ Load testing (basic)
- ❌ Security testing (future)

### Expansion Opportunities

#### Additional Test Scenarios
1. **End-to-End Workflows**
   - Complete loan application process
   - Customer onboarding flow
   - Payment processing cycle

2. **Security Testing**
   - Authentication bypass attempts
   - Input validation testing
   - SQL injection prevention

3. **Performance Testing**
   - Concurrent user simulation
   - Database connection pooling
   - Response time benchmarks

## 🔄 Maintenance

### Regular Updates

#### Monthly Tasks
- [ ] Update test data and scenarios
- [ ] Verify all endpoints are working
- [ ] Update documentation with new features
- [ ] Review and optimize test performance

#### After Code Changes
- [ ] Update collection if new endpoints added
- [ ] Modify environment variables if needed
- [ ] Test authentication changes
- [ ] Validate error message formats

### Version Control

#### Collection Versioning
- Follow semantic versioning (e.g., v3.0.0)
- Update info.version in collection files
- Tag releases with git tags

#### Environment Management
- Keep separate environments for dev/staging/prod
- Use environment-specific URLs and credentials
- Never commit sensitive data to version control

## 🎯 Best Practices

### Test Organization
1. **Folder Structure**: Group related tests logically
2. **Naming Convention**: Use descriptive test names
3. **Documentation**: Include test descriptions and expected outcomes
4. **Error Handling**: Always test both success and failure scenarios

### Performance Optimization
1. **Request Delays**: Add appropriate delays between requests
2. **Timeout Settings**: Set reasonable timeouts for different operations
3. **Resource Cleanup**: Clean up test data after tests
4. **Parallel Execution**: Use Newman's parallel capabilities cautiously

### Security Considerations
1. **Credential Management**: Use environment variables for sensitive data
2. **SSL Verification**: Enable SSL verification in production
3. **Request Logging**: Be careful with logging sensitive information
4. **Access Control**: Limit test account permissions

## 📞 Support

### Getting Help
- **Documentation**: Check this guide first
- **Issues**: Report problems via GitHub issues
- **Logs**: Check application.log for detailed error information
- **Community**: Join discussions for best practices

### Useful Commands
```bash
# Check Newman version
newman --version

# List available collections
ls postman/*.postman_collection.json

# View test results
cat test-results/standalone-results.json | jq '.run.stats'

# Generate HTML report
newman run collection.json --reporters htmlextra
```

---

*This guide is maintained as part of the Enterprise Banking System documentation. For updates and contributions, please follow the project's contribution guidelines.*

**Last Updated**: January 2024  
**Version**: 3.0.0  
**Compatibility**: Newman 5.3+, Postman 10.0+