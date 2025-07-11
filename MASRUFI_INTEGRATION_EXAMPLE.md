# 🔗 MasruFi Framework Integration Example

This document demonstrates how to integrate the MasruFi Framework Islamic Finance module with the existing Enterprise Loan Management System.

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                Enterprise Loan Management System            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────┐    ┌─────────────────────────────────┐ │
│  │  Loan Service   │    │     MasruFi Framework Module   │ │
│  │                 │    │                                 │ │
│  │  - Standard     │◄──►│  - Islamic Finance Services    │ │
│  │    Loans        │    │  - Sharia Compliance           │ │
│  │  - Corporate    │    │  - UAE Cryptocurrency          │ │
│  │    Loans        │    │  - Enterprise Integration      │ │
│  │  - Personal     │    │                                 │ │
│  │    Loans        │    │                                 │ │
│  └─────────────────┘    └─────────────────────────────────┘ │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 🚀 Integration Steps

### 1. Add MasruFi Framework Dependency

Update your main `build.gradle`:

```gradle
dependencies {
    // Add MasruFi Framework
    implementation project(':masrufi-framework')
    
    // Existing dependencies...
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
}
```

### 2. Enable MasruFi Framework

Add configuration to `application.yml`:

```yaml
# Enterprise Loan Management System Configuration
spring:
  application:
    name: enterprise-loan-system
  profiles:
    active: default,masrufi  # Enable MasruFi profile

# Enable MasruFi Framework
masrufi:
  framework:
    enabled: true
    integration-mode: EXTENSION
    islamic-finance:
      enabled: true
      default-currency: "AED"
      supported-models:
        - MURABAHA
        - MUSHARAKAH
        - IJARAH
        - QARD_HASSAN
    enterprise-integration:
      host-system-base-url: "http://localhost:8080"
      authentication-method: JWT
      event-publishing:
        enabled: true
        topic-prefix: "enterprise.masrufi.events"
```

### 3. Create Integrated Loan Service

```java
package com.bank.loan.service;

import com.masrufi.framework.MasrufiFrameworkFacade;
import com.masrufi.framework.domain.model.IslamicFinancing;
import com.masrufi.framework.domain.model.CreateMurabahaCommand;
import com.masrufi.framework.infrastructure.integration.EnterpriseLoanSystemIntegration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Enhanced Loan Service with Islamic Finance capabilities
 * 
 * This service demonstrates how the enterprise loan system can
 * seamlessly integrate Islamic finance capabilities through the
 * MasruFi Framework without modifying core business logic.
 */
@Slf4j
@Service
public class EnhancedLoanService {

    private final LoanService standardLoanService;
    private final MasrufiFrameworkFacade masrufiFramework;
    private final EnterpriseLoanSystemIntegration masrufiIntegration;

    public EnhancedLoanService(
            LoanService standardLoanService,
            @Autowired(required = false) MasrufiFrameworkFacade masrufiFramework,
            @Autowired(required = false) EnterpriseLoanSystemIntegration masrufiIntegration) {
        this.standardLoanService = standardLoanService;
        this.masrufiFramework = masrufiFramework;
        this.masrufiIntegration = masrufiIntegration;
        
        log.info("🏦 Enhanced Loan Service initialized");
        if (isMasrufiFrameworkAvailable()) {
            log.info("🕌 MasruFi Framework integration enabled");
        } else {
            log.info("📋 Operating with standard loan capabilities only");
        }
    }

    /**
     * Create loan with automatic Islamic finance detection
     */
    public LoanApplicationResult createLoan(LoanApplicationRequest request) {
        log.info("💼 Processing loan application: {}", request.getApplicationId());

        // Check if Islamic finance is requested
        if (request.isIslamicFinanceRequested() && isMasrufiFrameworkAvailable()) {
            return createIslamicFinanceLoan(request);
        } else {
            return createStandardLoan(request);
        }
    }

    /**
     * Create Islamic finance loan using MasruFi Framework
     */
    private LoanApplicationResult createIslamicFinanceLoan(LoanApplicationRequest request) {
        log.info("🕌 Creating Islamic finance loan: {}", request.getIslamicFinanceType());

        try {
            IslamicFinancing islamicFinancing = null;

            // Route to appropriate Islamic finance service
            switch (request.getIslamicFinanceType()) {
                case "MURABAHA":
                    islamicFinancing = createMurabahaFinancing(request);
                    break;
                case "MUSHARAKAH":
                    islamicFinancing = masrufiFramework.getMusharakahService()
                        .createMusharakah(buildMusharakahCommand(request));
                    break;
                case "IJARAH":
                    islamicFinancing = masrufiFramework.getIjarahService()
                        .createIjarah(buildIjarahCommand(request));
                    break;
                case "QARD_HASSAN":
                    islamicFinancing = masrufiFramework.getQardHassanService()
                        .createQardHassanLoan(buildQardHassanCommand(request));
                    break;
                default:
                    throw new UnsupportedOperationException(
                        "Islamic finance type not supported: " + request.getIslamicFinanceType());
            }

            // Register with enterprise system
            masrufiIntegration.registerIslamicFinancing(islamicFinancing);

            // Create enterprise loan record
            Loan enterpriseLoan = standardLoanService.createLoanFromIslamicFinancing(islamicFinancing);

            log.info("✅ Islamic finance loan created successfully: {}", islamicFinancing.getFinancingId());

            return LoanApplicationResult.builder()
                .applicationId(request.getApplicationId())
                .loanId(enterpriseLoan.getLoanId())
                .islamicFinancingId(islamicFinancing.getFinancingId())
                .loanType("ISLAMIC_FINANCE")
                .islamicFinanceType(request.getIslamicFinanceType())
                .principalAmount(islamicFinancing.getPrincipalAmount())
                .totalAmount(islamicFinancing.getTotalAmount())
                .shariaCompliant(true)
                .status("APPROVED")
                .createdDate(LocalDateTime.now())
                .build();

        } catch (Exception e) {
            log.error("❌ Failed to create Islamic finance loan", e);
            
            // Fallback to standard loan if Islamic finance fails
            log.info("🔄 Falling back to standard loan processing");
            return createStandardLoan(request);
        }
    }

    /**
     * Create standard loan using existing enterprise system
     */
    private LoanApplicationResult createStandardLoan(LoanApplicationRequest request) {
        log.info("📋 Creating standard loan for application: {}", request.getApplicationId());

        Loan loan = standardLoanService.createLoan(request);

        return LoanApplicationResult.builder()
            .applicationId(request.getApplicationId())
            .loanId(loan.getLoanId())
            .loanType("STANDARD")
            .principalAmount(Money.of(loan.getPrincipalAmount(), loan.getCurrency()))
            .status("APPROVED")
            .createdDate(LocalDateTime.now())
            .build();
    }

    /**
     * Create Murabaha financing with detailed asset handling
     */
    private IslamicFinancing createMurabahaFinancing(LoanApplicationRequest request) {
        CreateMurabahaCommand command = CreateMurabahaCommand.builder()
            .customerProfile(buildCustomerProfile(request))
            .assetDescription(request.getAssetDescription())
            .assetCost(Money.of(request.getLoanAmount(), request.getCurrency()))
            .profitMargin(new BigDecimal(request.getProfitMargin()))
            .maturityDate(request.getMaturityDate())
            .supplier(request.getAssetSupplier())
            .jurisdiction(request.getJurisdiction())
            .build();

        return masrufiFramework.getMurabahaService().createMurabaha(command);
    }

    /**
     * Check if Islamic finance capabilities are available
     */
    public boolean isMasrufiFrameworkAvailable() {
        return masrufiFramework != null && masrufiFramework.isFrameworkReady();
    }

    /**
     * Get Islamic finance capabilities information
     */
    public IslamicFinanceCapabilities getIslamicFinanceCapabilities() {
        if (!isMasrufiFrameworkAvailable()) {
            return IslamicFinanceCapabilities.unavailable();
        }

        return IslamicFinanceCapabilities.builder()
            .available(true)
            .frameworkVersion("1.0.0")
            .supportedModels(List.of("MURABAHA", "MUSHARAKAH", "IJARAH", "QARD_HASSAN"))
            .supportedCurrencies(List.of("AED", "SAR", "USD", "UAE-CBDC"))
            .shariaCompliant(true)
            .uaeCryptocurrencySupport(true)
            .frameworkInfo(masrufiFramework.getFrameworkInfo())
            .build();
    }

    // Helper methods for building commands (implementation details)
    private CustomerProfile buildCustomerProfile(LoanApplicationRequest request) {
        return CustomerProfile.builder()
            .customerId(request.getCustomerId())
            .customerName(request.getCustomerName())
            .customerType(CustomerType.valueOf(request.getCustomerType()))
            .build();
    }

    // Additional helper methods would be implemented here...
}
```

### 4. Create REST Controller with Islamic Finance Support

```java
package com.bank.loan.controller;

import com.bank.loan.service.EnhancedLoanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Enhanced Loan Controller with Islamic Finance capabilities
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/loans")
public class EnhancedLoanController {

    private final EnhancedLoanService loanService;

    public EnhancedLoanController(EnhancedLoanService loanService) {
        this.loanService = loanService;
    }

    /**
     * Create loan application (supports both standard and Islamic finance)
     */
    @PostMapping("/applications")
    public ResponseEntity<LoanApplicationResult> createLoanApplication(
            @RequestBody LoanApplicationRequest request) {
        
        log.info("📝 Received loan application: {}", request.getApplicationId());

        LoanApplicationResult result = loanService.createLoan(request);

        if (result.isIslamicFinance()) {
            log.info("🕌 Islamic finance loan created: {}", result.getIslamicFinancingId());
        } else {
            log.info("📋 Standard loan created: {}", result.getLoanId());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Get Islamic finance capabilities
     */
    @GetMapping("/islamic-finance/capabilities")
    public ResponseEntity<IslamicFinanceCapabilities> getIslamicFinanceCapabilities() {
        IslamicFinanceCapabilities capabilities = loanService.getIslamicFinanceCapabilities();
        return ResponseEntity.ok(capabilities);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<HealthStatus> getHealth() {
        boolean islamicFinanceAvailable = loanService.isMasrufiFrameworkAvailable();
        
        HealthStatus status = HealthStatus.builder()
            .status("UP")
            .standardLoansAvailable(true)
            .islamicFinanceAvailable(islamicFinanceAvailable)
            .timestamp(LocalDateTime.now())
            .build();

        return ResponseEntity.ok(status);
    }
}
```

### 5. Event Handling Integration

```java
package com.bank.loan.integration;

import com.masrufi.framework.domain.event.IslamicFinanceEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Event handler for MasruFi Framework events
 */
@Slf4j
@Component
public class MasrufiEventHandler {

    /**
     * Handle Islamic financing registration events
     */
    @EventListener
    public void handleIslamicFinancingRegistered(IslamicFinancingRegisteredEvent event) {
        log.info("🕌 Islamic financing registered: {} -> {}", 
            event.getIslamicFinancingId(), event.getEnterpriseLoanId());
        
        // Update enterprise system records
        // Send notifications
        // Update reporting systems
    }

    /**
     * Handle Islamic financing status updates
     */
    @EventListener
    public void handleIslamicFinancingStatusUpdated(IslamicFinancingStatusUpdatedEvent event) {
        log.info("📊 Islamic financing status updated: {} -> {}", 
            event.getIslamicFinancingId(), event.getNewStatus());
        
        // Sync status with enterprise loan
        // Update customer notifications
        // Trigger compliance reporting
    }
}
```

## 🔧 Configuration Management

### Development Environment

```yaml
# application-dev.yml
masrufi:
  framework:
    enabled: true
    islamic-finance:
      enabled: true
      supported-models: [MURABAHA, QARD_HASSAN]  # Limited for dev
    uae-cryptocurrency:
      enabled: false  # Disabled for development
    sharia-compliance:
      strict-mode: false  # Relaxed for testing
```

### Production Environment

```yaml
# application-prod.yml
masrufi:
  framework:
    enabled: true
    islamic-finance:
      enabled: true
      supported-models:
        - MURABAHA
        - MUSHARAKAH
        - IJARAH
        - SALAM
        - ISTISNA
        - QARD_HASSAN
    uae-cryptocurrency:
      enabled: true
      supported-currencies:
        - UAE-CBDC
        - ADIB-DD
        - ENBD-DC
    sharia-compliance:
      strict-mode: true
      sharia-board: "UAE_HIGHER_SHARIA_AUTHORITY"
```

## 📊 Monitoring Integration

### Custom Metrics

```java
@Component
public class LoanServiceMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Counter standardLoansCounter;
    private final Counter islamicFinanceCounter;
    
    public LoanServiceMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.standardLoansCounter = Counter.builder("loans.standard.created")
            .description("Number of standard loans created")
            .register(meterRegistry);
        this.islamicFinanceCounter = Counter.builder("loans.islamic.created")
            .description("Number of Islamic finance loans created")
            .register(meterRegistry);
    }
    
    public void recordStandardLoan() {
        standardLoansCounter.increment();
    }
    
    public void recordIslamicFinanceLoan(String type) {
        islamicFinanceCounter.increment(
            Tags.of("islamic_finance_type", type)
        );
    }
}
```

### Health Indicators

```java
@Component
public class IntegratedLoanSystemHealthIndicator implements HealthIndicator {
    
    private final EnhancedLoanService loanService;
    
    @Override
    public Health health() {
        Health.Builder status = Health.up();
        
        status.withDetail("standard_loans", "UP");
        
        if (loanService.isMasrufiFrameworkAvailable()) {
            status.withDetail("islamic_finance", "UP");
            status.withDetail("masrufi_framework_version", "1.0.0");
        } else {
            status.withDetail("islamic_finance", "UNAVAILABLE");
        }
        
        return status.build();
    }
}
```

## 🧪 Testing Integration

### Integration Test Example

```java
@SpringBootTest
@ActiveProfiles({"test", "masrufi"})
class IntegratedLoanServiceTest {
    
    @Autowired
    private EnhancedLoanService loanService;
    
    @Test
    void shouldCreateMurabahaFinancing() {
        // Given
        LoanApplicationRequest request = LoanApplicationRequest.builder()
            .applicationId("APP-001")
            .customerId("CUST-001")
            .islamicFinanceRequested(true)
            .islamicFinanceType("MURABAHA")
            .assetDescription("Toyota Camry 2024")
            .loanAmount(80000.0)
            .currency("AED")
            .profitMargin(0.15)
            .build();

        // When
        LoanApplicationResult result = loanService.createLoan(request);

        // Then
        assertThat(result.isIslamicFinance()).isTrue();
        assertThat(result.getIslamicFinanceType()).isEqualTo("MURABAHA");
        assertThat(result.isShariaCompliant()).isTrue();
    }
    
    @Test
    void shouldFallbackToStandardLoan_whenIslamicFinanceUnavailable() {
        // Test fallback mechanism
    }
}
```

## 🚀 Deployment

### Docker Configuration

```dockerfile
# Dockerfile
FROM openjdk:21-jdk-slim

# Copy MasruFi Framework configuration
COPY masrufi-framework/src/main/resources/application-masrufi.yml /app/config/

# Copy enterprise application
COPY build/libs/enterprise-loan-system.jar /app/

# Run with MasruFi profile enabled
CMD ["java", "-jar", "/app/enterprise-loan-system.jar", "--spring.profiles.active=prod,masrufi"]
```

### Kubernetes Deployment

```yaml
# k8s/deployment.yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: enterprise-loan-system
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: loan-system
        image: enterprise-loan-system:latest
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod,masrufi"
        - name: MASRUFI_FRAMEWORK_ENABLED
          value: "true"
        - name: MASRUFI_UAE_CRYPTOCURRENCY_ENABLED
          value: "true"
```

## 🎯 Benefits of This Integration

### 1. **High Cohesion**
- All Islamic finance logic centralized in MasruFi Framework
- Clean separation of concerns
- Easy to maintain and update

### 2. **Loose Coupling**
- Enterprise system operates independently
- MasruFi Framework can be enabled/disabled via configuration
- Graceful fallback to standard loans

### 3. **Zero Disruption**
- Existing loan functionality remains unchanged
- Islamic finance added as optional enhancement
- Backward compatibility maintained

### 4. **Production Ready**
- Comprehensive error handling and fallbacks
- Monitoring and health checks
- Event-driven integration

### 5. **Regulatory Compliance**
- Sharia compliance automated
- UAE cryptocurrency support
- Audit trails and reporting

---

This integration example demonstrates how the MasruFi Framework can be seamlessly added to existing enterprise loan management systems, providing Islamic finance capabilities without disrupting core business operations.