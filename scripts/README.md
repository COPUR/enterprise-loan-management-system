#  Hexagonal Architecture Validation Scripts

This directory contains comprehensive validation scripts for ensuring hexagonal architecture compliance in the Enterprise Banking System.

##  Quick Start

### Run Full Validation
```bash
# One-click comprehensive validation
./scripts/run-validation.sh
```

### Individual Validations
```bash
# Domain purity check only
./scripts/hexagonal-architecture-validation.sh domain-purity

# Factory methods validation
./scripts/hexagonal-architecture-validation.sh factory-methods

# Domain events validation
./scripts/hexagonal-architecture-validation.sh domain-events

# Architecture metrics
./scripts/hexagonal-architecture-validation.sh metrics

# Generate report only
./scripts/hexagonal-architecture-validation.sh report
```

##  Available Scripts

### 1. `hexagonal-architecture-validation.sh`
**Comprehensive hexagonal architecture validation script**

**Features:**
-  **Domain Purity Validation** - Zero infrastructure dependencies
- 🏭 **Factory Method Pattern** - Loan.create() and LoanInstallment.create() validation
-  **Domain Events System** - 8+ comprehensive events verification
- 💎 **Value Objects Validation** - Immutable business concepts
- 🏛️ **Aggregate Roots** - Proper boundary enforcement
-  **Domain Metrics** - Complexity and quality measurements
-  **Architecture Tests** - ArchUnit and domain purity tests
-  **Test Coverage** - 87.4%+ overall, 95%+ domain coverage
-  **Quality Report** - Comprehensive architecture report generation

**Usage:**
```bash
./scripts/hexagonal-architecture-validation.sh [command]

Commands:
  validate        - Run full validation (default)
  domain-purity   - Check domain layer purity
  factory-methods - Validate factory method patterns
  domain-events   - Validate domain events system
  metrics         - Generate domain metrics
  report          - Generate architecture report
  help            - Show help message
```

### 2. `run-validation.sh`
**Quick runner for comprehensive validation**

Simple one-click script that runs the full hexagonal architecture validation with proper error handling and directory checks.

##  Validation Criteria

### Domain Purity Requirements
-  **Zero JPA Contamination** - No `jakarta.persistence` imports in domain
-  **No Spring Dependencies** - No `org.springframework` imports in domain
-  **Infrastructure Isolation** - No infrastructure package imports
-  **Annotation Free** - No `@Repository`, `@Service`, `@Component` in domain

### Factory Method Requirements
-  **Loan.create()** - Static factory method for loan creation
-  **LoanInstallment.create()** - Static factory method for installment creation
-  **No Builder Patterns** - Factory methods preferred over builders in domain

### Domain Events Requirements
-  **Minimum 8 Events** - Comprehensive lifecycle coverage
-  **Proper Inheritance** - All events extend `DomainEvent`
-  **Event Coverage** - Application, approval, payment, default, restructure events

### Architecture Quality Metrics
-  **Loan Domain** - 424+ lines of pure business logic
-  **LoanInstallment** - 215+ lines of business rules
-  **Test Coverage** - 87.4%+ overall, 95%+ domain layer
-  **Value Objects** - Strong typing and immutability

##  CI/CD Integration

These scripts are integrated into the CI/CD pipeline:

```yaml
- name:  Hexagonal Architecture Validation
  run: |
    ./scripts/hexagonal-architecture-validation.sh validate
```

The validation runs automatically on:
-  Push to main/develop branches
-  Pull requests
-  Manual workflow dispatch
-  Release workflows

##  Example Output

```bash
 ENTERPRISE BANKING - HEXAGONAL ARCHITECTURE VALIDATION
========================================
 DOMAIN PURITY VALIDATION
========================================
ℹ️  Checking for JPA contamination in domain layer...
 Domain layer is free from JPA contamination
ℹ️  Checking for Spring Framework contamination in domain layer...
 Domain layer is free from Spring Framework contamination
 DOMAIN PURITY VALIDATION PASSED

========================================
🏭 FACTORY METHOD PATTERN VALIDATION
========================================
ℹ️  Validating Loan.create() factory method...
 Loan.create() factory method found
ℹ️  Validating LoanInstallment.create() factory method...
 LoanInstallment.create() factory method found
 FACTORY METHOD PATTERN VALIDATION PASSED

========================================
 DOMAIN EVENTS SYSTEM VALIDATION
========================================
ℹ️  Counting domain events...
 Domain Events Found: 8
 Sufficient domain events found: 8
ℹ️  Domain Events Inventory:
   • LoanApplicationSubmittedEvent
   • LoanApprovedEvent
   • LoanRejectedEvent
   • LoanDisbursedEvent
   • LoanPaymentMadeEvent
   • LoanPaidOffEvent
   • LoanDefaultedEvent
   • LoanRestructuredEvent
 DOMAIN EVENTS SYSTEM VALIDATION PASSED

 HEXAGONAL ARCHITECTURE VALIDATION SUCCESSFUL
 All validations passed! Architecture is compliant with enterprise standards.

Summary:
    Domain Purity
    Factory Methods
    Domain Events (8+)
    Value Objects
    Aggregate Roots
    Architecture Tests
    Test Coverage (87.4%+)

 Ready for production deployment!
```

##  Troubleshooting

### Common Issues

**Domain Contamination:**
```bash
# Fix JPA contamination
find src/main/java -path "*/domain/*" -name "*.java" -exec grep -l "jakarta.persistence" {} \;
```

**Missing Factory Methods:**
```bash
# Check factory method implementation
grep -r "public static.*create" src/main/java/*/domain/
```

**Insufficient Domain Events:**
```bash
# List current domain events
find src/main/java -path "*/domain/*/event/*" -name "*Event.java"
```

### Prerequisites

1. **Java 21** - Required for compilation
2. **Gradle** - Build tool (./gradlew)
3. **Git** - Version control
4. **Bash** - Shell environment

### Script Permissions

If scripts are not executable:
```bash
chmod +x scripts/*.sh
```

##  Continuous Improvement

These scripts evolve with the architecture:

- **Version Tracking** - Scripts versioned with code
- **Metric Evolution** - Quality gates adjust with maturity
- **New Validations** - Additional checks as architecture grows
- **Performance Optimization** - Script execution improvements

## 🤝 Contributing

When adding new validation rules:

1. **Add to validation function** - New check in appropriate section
2. **Update requirements** - Document new criteria
3. **Test thoroughly** - Validate on clean and contaminated code
4. **Update CI/CD** - Ensure pipeline integration
5. **Document changes** - Update this README

---

** Enterprise Banking System - Hexagonal Architecture Excellence**