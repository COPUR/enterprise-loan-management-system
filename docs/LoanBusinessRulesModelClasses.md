# Loan Business Rules Model Classes

This document describes the model classes created to support the `LoanBusinessRulesService` for loan eligibility assessment and risk analysis.

## Created Classes

### 1. LoanEligibilityResult
**Location**: `/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/src/main/java/com/loanmanagement/loan/domain/model/LoanEligibilityResult.java`

**Purpose**: Main result object containing complete eligibility assessment for a loan application.

**Key Features**:
- Contains all individual check results (credit score, DTI, LTV, employment, banking history)
- Lists business rule violations and additional requirements
- Provides utility methods for checking eligibility status
- Supports categorization of violations by severity

### 2. BusinessRuleViolation
**Location**: `/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/src/main/java/com/loanmanagement/loan/domain/model/BusinessRuleViolation.java`

**Purpose**: Represents a violation of a business rule during loan assessment.

**Key Features**:
- Captures rule type, description, severity, and comparison values
- Includes timestamp and remediation advice
- Provides formatted messages for UI display
- Supports different severity levels (ERROR, WARNING, INFO)

### 3. BusinessRuleType
**Location**: `/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/src/main/java/com/loanmanagement/loan/domain/model/BusinessRuleType.java`

**Purpose**: Enumeration defining types of business rules that can be violated.

**Key Features**:
- Comprehensive list of loan assessment rules
- Categorizes rules by type (Credit, Financial, Employment, etc.)
- Provides default remediation advice for each rule type
- Indicates which rules are typically blocking vs. warning

### 4. ViolationSeverity
**Location**: `/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/src/main/java/com/loanmanagement/loan/domain/model/ViolationSeverity.java`

**Purpose**: Enumeration defining severity levels for business rule violations.

**Key Features**:
- Three levels: ERROR (blocking), WARNING (attention), INFO (informational)
- Priority-based comparison
- UI support with CSS classes and color codes
- Utility methods for determining impact on approval

### 5. CreditScoreCheck
**Location**: `/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/src/main/java/com/loanmanagement/loan/domain/model/CreditScoreCheck.java`

**Purpose**: Contains credit score eligibility assessment results.

**Key Features**:
- Compares actual vs. required credit scores
- Categorizes credit scores (Excellent, Good, Fair, Poor)
- Provides improvement recommendations
- Calculates qualification for premium rates

### 6. DebtToIncomeCheck
**Location**: `/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/src/main/java/com/loanmanagement/loan/domain/model/DebtToIncomeCheck.java`

**Purpose**: Contains debt-to-income ratio assessment results.

**Key Features**:
- Tracks current and projected DTI ratios
- Compares against maximum allowed DTI
- Calculates needed debt reduction or income increase
- Provides risk level assessment

### 7. LoanToValueCheck
**Location**: `/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/src/main/java/com/loanmanagement/loan/domain/model/LoanToValueCheck.java`

**Purpose**: Contains loan-to-value ratio assessment for secured loans.

**Key Features**:
- Handles both secured and unsecured loans
- Compares LTV ratio against limits
- Calculates equity percentage
- Determines mortgage insurance requirements

### 8. EmploymentCheck
**Location**: `/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/src/main/java/com/loanmanagement/loan/domain/model/EmploymentCheck.java`

**Purpose**: Contains employment stability assessment results.

**Key Features**:
- Evaluates employment type and duration
- Provides stability ratings
- Lists required documentation by employment type
- Identifies risk factors and verification needs

### 9. BankingHistoryCheck
**Location**: `/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/src/main/java/com/loanmanagement/loan/domain/model/BankingHistoryCheck.java`

**Purpose**: Contains banking relationship history assessment.

**Key Features**:
- Evaluates customer banking experience level
- Determines risk mitigation measures
- Provides relationship value assessment
- Recommends monitoring frequency

### 10. RiskScore
**Location**: `/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/src/main/java/com/loanmanagement/loan/domain/model/RiskScore.java`

**Purpose**: Contains calculated overall risk score and analysis.

**Key Features**:
- 0-1000 risk score scale with categorization
- Lists contributing risk factors
- Provides approval recommendations
- Suggests interest rate adjustments and monitoring levels

### 11. RiskFactor
**Location**: `/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/src/main/java/com/loanmanagement/loan/domain/model/RiskFactor.java`

**Purpose**: Represents individual factors contributing to risk assessment.

**Key Features**:
- Captures factor name, value, weight, and contribution
- Categorizes factors by assessment type
- Provides improvement recommendations
- Supports comparison and ranking

## Design Patterns Used

### Builder Pattern
All main classes use Lombok's `@Builder` annotation for flexible object construction:
```java
LoanEligibilityResult result = LoanEligibilityResult.builder()
    .eligible(true)
    .creditScoreCheck(creditCheck)
    .debtToIncomeCheck(dtiCheck)
    // ... other fields
    .build();
```

### Value Objects
All classes are immutable value objects using Lombok's `@Value` annotation:
- Immutable state
- Built-in equals/hashCode
- Thread-safe
- Domain-driven design principles

### Validation
Comprehensive validation in constructors:
- Null checks for required fields
- Range validation for numeric values
- Business rule validation

### Factory Methods
Static factory methods for common scenarios:
```java
CreditScoreCheck.passing(750, 650)
LoanToValueCheck.unsecured()
BusinessRuleViolation.error(ruleType, description)
```

## Integration with LoanBusinessRulesService

These classes are designed to work seamlessly with the `LoanBusinessRulesService`:

1. **Input Processing**: Service takes `CustomerProfile` and `LoanApplication`
2. **Individual Checks**: Each check type returns its specific result object
3. **Violation Collection**: Service collects all violations in a unified list
4. **Result Assembly**: Service builds the complete `LoanEligibilityResult`
5. **Risk Calculation**: Service calculates overall `RiskScore` with `RiskFactor` contributions

## Usage Examples

### Basic Eligibility Check
```java
LoanEligibilityResult result = loanBusinessRulesService.checkLoanEligibility(customer, application);
if (result.isEligible()) {
    // Process approval
} else {
    // Handle violations
    result.getViolations().forEach(violation -> {
        log.warn("Rule violation: {}", violation.getFormattedMessage());
    });
}
```

### Risk Assessment
```java
RiskScore riskScore = loanBusinessRulesService.calculateRiskScore(customer, application);
if (riskScore.isAcceptableRisk()) {
    int rateAdjustment = riskScore.getSuggestedRateAdjustment();
    // Apply rate adjustment
}
```

### Detailed Analysis
```java
CreditScoreCheck creditCheck = result.getCreditScoreCheck();
if (!creditCheck.isPassed()) {
    String improvement = creditCheck.getEstimatedImprovementTime();
    // Provide guidance to customer
}
```

## Testing

A comprehensive test class `LoanBusinessRulesModelTest` has been created to validate:
- Object construction and validation
- Business logic calculations
- Edge cases and error conditions
- Integration between related classes

## Notes

- All classes follow Domain-Driven Design principles
- Validation is performed at construction time
- Rich domain models with business logic embedded
- Comprehensive utility methods for common operations
- Consistent error handling and messaging
- Support for both programmatic and UI-friendly access