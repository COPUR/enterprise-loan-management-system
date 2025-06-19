# OpenAI Assistant Testing Guide for Enterprise Loan Management System

## 🤖 AI-Powered Banking Assistant Testing Manual

### Prerequisites

1. **OpenAI API Configuration**
   ```bash
   export OPENAI_API_KEY="your-openai-api-key-here"
   export OPENAI_MODEL="gpt-4"
   export OPENAI_TEMPERATURE="0.3"
   ```

2. **Application Setup**
   ```bash
   # Start with AI profile enabled
   ./gradlew bootRun --args='--spring.profiles.active=dev,ai'
   
   # Or using environment variables
   SPRING_PROFILES_ACTIVE=dev,ai java -jar enterprise-loan-management-system.jar
   ```

3. **Verify AI Service Health**
   ```bash
   curl http://localhost:8080/api/ai/health
   ```

## 🎯 AI Use Case Testing Scenarios

### 1. Loan Application Analysis with AI

#### Test Case 1.1: Personal Loan Analysis
```bash
curl -X POST http://localhost:8080/api/ai/analyze/loan-application \
  -H "Content-Type: application/json" \
  -d '{
    "applicationId": "APP2024001",
    "customerId": 1,
    "loanType": "PERSONAL",
    "loanAmount": 50000.00,
    "termMonths": 36,
    "purpose": "Home renovation",
    "customerProfile": {
      "creditScore": 720,
      "monthlyIncome": 8000.00,
      "employmentStatus": "EMPLOYED",
      "employmentYears": 5,
      "debtToIncomeRatio": 0.35,
      "existingDebt": 2800.00
    },
    "requestedInterestRate": 8.5,
    "collateralOffered": false
  }'
```

**Expected AI Response:**
- Loan viability score (1-10)
- Risk assessment and recommendations
- Interest rate suggestions
- Approval likelihood
- Conditions and requirements

#### Test Case 1.2: High-Risk Application Analysis
```bash
curl -X POST http://localhost:8080/api/ai/analyze/loan-application \
  -H "Content-Type: application/json" \
  -d '{
    "applicationId": "APP2024002",
    "customerId": 8,
    "loanType": "PERSONAL",
    "loanAmount": 25000.00,
    "termMonths": 36,
    "customerProfile": {
      "creditScore": 620,
      "monthlyIncome": 3750.00,
      "employmentStatus": "PART_TIME",
      "employmentYears": 1,
      "debtToIncomeRatio": 0.55,
      "existingDebt": 2100.00
    }
  }'
```

### 2. Credit Risk Assessment with AI

#### Test Case 2.1: Standard Risk Assessment
```bash
curl -X POST http://localhost:8080/api/ai/assess/credit-risk \
  -H "Content-Type: application/json" \
  -d '{
    "customerData": {
      "customerId": 1,
      "creditScore": 720,
      "monthlyIncome": 8000.00,
      "employmentStatus": "EMPLOYED",
      "employmentYears": 5,
      "existingDebt": 2800.00,
      "paymentHistory": "EXCELLENT",
      "accountAge": "5_YEARS",
      "creditUtilization": 0.28
    },
    "loanData": {
      "loanType": "PERSONAL",
      "requestedAmount": 50000.00,
      "termMonths": 36,
      "purpose": "HOME_RENOVATION",
      "loanToValueRatio": 0.8
    }
  }'
```

**Expected AI Analysis:**
- Credit risk score (1-100)
- Probability of default percentage
- Risk category (LOW/MEDIUM/HIGH/CRITICAL)
- Primary risk factors identification
- Mitigation strategies
- Portfolio impact assessment

### 3. Personalized Loan Recommendations

#### Test Case 3.1: Prime Customer Recommendations
```bash
curl -X POST http://localhost:8080/api/ai/recommend/loans \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 2,
    "customerSegment": "PRIME",
    "creditScore": 750,
    "monthlyIncome": 7916.67,
    "employmentStatus": "EMPLOYED",
    "existingLoans": 1,
    "totalExistingDebt": 15000.00,
    "financialGoals": ["HOME_PURCHASE", "INVESTMENT"],
    "riskTolerance": "MODERATE",
    "preferredLoanTypes": ["MORTGAGE", "PERSONAL"],
    "relationshipLength": "3_YEARS"
  }'
```

### 4. AI-Powered Fraud Detection

#### Test Case 4.1: Suspicious Transaction Analysis
```bash
curl -X POST http://localhost:8080/api/ai/detect/fraud \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "TXN2024001",
    "customerId": 3,
    "transactionAmount": 5000.00,
    "transactionType": "LOAN_APPLICATION",
    "timestamp": "2024-06-19T14:30:00Z",
    "deviceInfo": {
      "ipAddress": "192.168.1.100",
      "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
      "deviceFingerprint": "abc123def456"
    },
    "locationInfo": {
      "country": "US",
      "state": "NY",
      "city": "New York"
    },
    "customerBehavior": {
      "averageTransactionAmount": 1500.00,
      "transactionFrequency": "MONTHLY",
      "usualTransactionTimes": ["09:00", "17:00"],
      "typicalLocations": ["NY", "NJ"]
    }
  }'
```

### 5. Collection Strategy Generation

#### Test Case 5.1: Early Delinquency Strategy
```bash
curl -X POST http://localhost:8080/api/ai/strategy/collection \
  -H "Content-Type: application/json" \
  -d '{
    "loanId": "LOAN2024004",
    "customerId": 3,
    "delinquencyStage": "30_DAYS_PAST_DUE",
    "amountPastDue": 1567.89,
    "totalLoanBalance": 52341.23,
    "paymentHistory": {
      "totalPayments": 12,
      "latePayments": 3,
      "missedPayments": 1
    },
    "customerProfile": {
      "contactPreference": "PHONE",
      "bestContactTime": "EVENING",
      "languagePreference": "ENGLISH",
      "previousCollectionResponse": "COOPERATIVE"
    },
    "financialHardship": {
      "reported": true,
      "reason": "REDUCED_HOURS",
      "temporary": true
    }
  }'
```

### 6. Batch AI Processing

#### Test Case 6.1: Multiple Operations Batch
```bash
curl -X POST http://localhost:8080/api/ai/analyze/batch \
  -H "Content-Type: application/json" \
  -d '{
    "operations": [
      {
        "type": "loan_analysis",
        "id": "batch_001",
        "data": {
          "applicationId": "APP2024004",
          "customerId": 4,
          "loanType": "BUSINESS",
          "loanAmount": 100000.00,
          "termMonths": 48,
          "customerProfile": {
            "creditScore": 690,
            "businessRevenue": 300000.00
          }
        }
      },
      {
        "type": "risk_assessment",
        "id": "batch_002",
        "data": {
          "customerData": {
            "customerId": 5,
            "creditScore": 740,
            "monthlyIncome": 16666.67
          },
          "loanData": {
            "loanType": "BUSINESS",
            "requestedAmount": 250000.00,
            "termMonths": 60
          }
        }
      }
    ]
  }'
```

## 📊 AI Performance Monitoring

### 1. AI Service Health Dashboard
```bash
curl http://localhost:8080/api/ai/insights/dashboard
```

### 2. AI Configuration Verification
```bash
curl http://localhost:8080/api/ai/config
```

### 3. Response Time Monitoring
```bash
# Measure response times
time curl -X POST http://localhost:8080/api/ai/analyze/loan-application \
  -H "Content-Type: application/json" \
  -d @sample-loan-application.json
```

## 🧪 Testing Best Practices

### 1. Test Data Preparation
Create realistic test data that mirrors production scenarios:

```json
{
  "highRiskCustomer": {
    "creditScore": 580,
    "debtToIncomeRatio": 0.65,
    "employmentStatus": "UNEMPLOYED"
  },
  "primeCustomer": {
    "creditScore": 800,
    "debtToIncomeRatio": 0.15,
    "employmentStatus": "EXECUTIVE"
  }
}
```

### 2. AI Response Validation

#### Check Response Structure
```bash
# Verify required fields in AI response
response=$(curl -s -X POST http://localhost:8080/api/ai/analyze/loan-application -d @test-data.json)
echo $response | jq '.confidence, .recommendation, .timestamp'
```

#### Validate Business Logic
```bash
# Ensure AI recommendations align with business rules
echo $response | jq '.finalRecommendation' | grep -E "APPROVE|REJECT|CONDITIONAL"
```

### 3. Load Testing AI Endpoints

#### Single Request Performance
```bash
ab -n 100 -c 10 -T 'application/json' -p loan-app.json \
  http://localhost:8080/api/ai/analyze/loan-application
```

#### Batch Processing Performance
```bash
ab -n 50 -c 5 -T 'application/json' -p batch-requests.json \
  http://localhost:8080/api/ai/analyze/batch
```

## 🔍 AI Quality Assurance

### 1. Accuracy Testing
- **Loan Approval Accuracy:** Compare AI recommendations with manual underwriting
- **Risk Score Precision:** Validate against historical default data
- **Fraud Detection Rate:** Test against known fraud patterns

### 2. Consistency Testing
```bash
# Run same request multiple times to check consistency
for i in {1..5}; do
  curl -X POST http://localhost:8080/api/ai/analyze/loan-application \
    -H "Content-Type: application/json" \
    -d @consistent-test.json | jq '.confidence'
done
```

### 3. Bias Detection Testing
Test AI responses across different customer demographics:
- Age groups
- Geographic locations
- Income levels
- Employment types

## 🚨 Error Handling Tests

### 1. Invalid Data Handling
```bash
# Test with missing required fields
curl -X POST http://localhost:8080/api/ai/analyze/loan-application \
  -H "Content-Type: application/json" \
  -d '{"invalidData": true}'
```

### 2. API Rate Limiting
```bash
# Test rate limiting behavior
for i in {1..50}; do
  curl -X POST http://localhost:8080/api/ai/analyze/loan-application \
    -H "Content-Type: application/json" \
    -d @test-data.json &
done
```

### 3. Service Unavailability
```bash
# Test behavior when OpenAI API is unavailable
OPENAI_API_KEY="invalid" curl -X POST http://localhost:8080/api/ai/analyze/loan-application \
  -H "Content-Type: application/json" \
  -d @test-data.json
```

## 📈 AI Metrics and KPIs

### 1. Response Time Metrics
- **Target:** < 2 seconds for individual requests
- **Batch Processing:** < 10 seconds for 10 operations
- **Health Check:** < 100ms

### 2. Accuracy Metrics
- **Loan Approval Accuracy:** > 90%
- **Risk Assessment Precision:** > 85%
- **Fraud Detection Rate:** > 95%

### 3. Business Impact Metrics
- **Processing Time Reduction:** Target 70% faster than manual
- **Decision Consistency:** > 95% reproducible results
- **Customer Satisfaction:** Improved response times

## 🔧 Troubleshooting Guide

### Common Issues and Solutions

1. **AI Service Not Responding**
   ```bash
   # Check AI service health
   curl http://localhost:8080/api/ai/health
   
   # Verify OpenAI API key configuration
   echo $OPENAI_API_KEY | head -c 20
   ```

2. **Slow AI Responses**
   ```bash
   # Check model configuration
   curl http://localhost:8080/api/ai/config | jq '.models'
   
   # Monitor system resources
   top -p $(pgrep java)
   ```

3. **Inconsistent AI Results**
   ```bash
   # Verify temperature settings
   curl http://localhost:8080/api/ai/config | jq '.models.*.temperature'
   ```

## 🎯 Production Deployment Checklist

### Pre-Deployment AI Testing
- [ ] All AI endpoints respond correctly
- [ ] Performance meets SLA requirements
- [ ] Error handling works properly
- [ ] Security validation passes
- [ ] Business rule compliance verified
- [ ] Load testing completed successfully

### Monitoring Setup
- [ ] AI response time alerts configured
- [ ] Accuracy monitoring dashboard created
- [ ] Error rate thresholds set
- [ ] Business metrics tracking enabled

### Compliance Verification
- [ ] FAPI security standards met
- [ ] Data privacy requirements satisfied
- [ ] Audit logging implemented
- [ ] Bias detection monitoring active

This comprehensive testing guide ensures the AI-powered Enterprise Loan Management System delivers accurate, reliable, and compliant banking services while maintaining the highest standards of performance and security.