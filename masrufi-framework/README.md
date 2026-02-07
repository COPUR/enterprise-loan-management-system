# 🕌 MasruFi Framework - Islamic Finance Extension Module

[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)](https://github.com/COPUR/enterprise-loan-management-system)
[![Sharia Compliant](https://img.shields.io/badge/Sharia-Compliant-green.svg)](https://masrufi.com/compliance)
[![UAE Ready](https://img.shields.io/badge/UAE-Ready-gold.svg)](https://masrufi.com/uae)
[![License](https://img.shields.io/badge/license-Proprietary-red.svg)](https://masrufi.com/license)

The **MasruFi Framework** is a high-cohesion, loosely-coupled extension module that adds comprehensive Islamic finance capabilities to existing enterprise loan management systems. Designed with modern software architecture principles, it integrates seamlessly without requiring modifications to your core business logic.

## 🎯 Key Features

### 📋 Islamic Finance Models
- **Murabaha** - Cost-plus financing with disclosed profit margins
- **Musharakah** - Partnership financing with profit/loss sharing
- **Ijarah** - Asset leasing with ownership transfer options
- **Salam** - Forward sale financing for commodity transactions
- **Istisna** - Manufacturing and construction project financing
- **Qard Hassan** - Interest-free benevolent loans for welfare

### 🧪 Test-Driven Development Excellence
- **300+ Passing Tests** - Comprehensive test coverage with enterprise banking integration
- **Domain-Driven Design** - Rich domain models with business logic
- **TDD Implementation** - Test-first development approach with RED-GREEN-REFACTOR cycles
- **Enterprise Security** - Multi-Factor Authentication and Security Audit Filter
- **Risk Analytics** - Real-time risk assessment and monitoring with ML capabilities
- **FAPI 2.0 Security** - Financial-grade API security compliance with OAuth 2.1 and DPoP
- **Performance Optimization** - Multi-level caching strategies with event sourcing
- **Production Infrastructure** - CI/CD pipeline with blue-green deployment
- **Monitoring & Observability** - Comprehensive Grafana dashboards and Prometheus alerts

### 🔒 Sharia Compliance
- ✅ **Riba-Free**: Completely eliminates interest-based transactions
- ✅ **Gharar-Free**: Removes uncertainty through detailed specifications
- ✅ **Asset-Backed**: All financing tied to real economic activities
- ✅ **Permissible Assets**: Validates asset compliance with Islamic principles
- ✅ **UAE Sharia Authority**: Certified compliance framework

### 💰 UAE Cryptocurrency Integration
- **UAE-CBDC** - UAE Central Bank Digital Currency
- **ADIB-DD** - Abu Dhabi Islamic Bank Digital Dirham
- **ENBD-DC** - Emirates NBD Digital Currency
- **FAB-DT** - First Abu Dhabi Bank Digital Token
- **CBD-DD** - Commercial Bank of Dubai Digital Dirham
- **RAK-DC** - RAK Bank Digital Currency
- **MASHREQ-DC** - Mashreq Bank Digital Currency

### 🏗️ Architecture Excellence
- **High Cohesion**: All Islamic finance logic centralized in one module
- **Loose Coupling**: Minimal dependencies on host enterprise systems
- **Event-Driven**: Advanced event sourcing with snapshot strategy and projections
- **Hot-Swappable**: Business rules can be updated without downtime
- **Multi-Tenant**: Supports multiple jurisdictions and currencies
- **Enterprise Security**: OAuth 2.1 with DPoP, MFA, and comprehensive audit trails
- **Production Ready**: Blue-green deployment with health checks and monitoring
- **Database Optimized**: Multi-level caching with partitioning and indexing
- **Compliance First**: Built-in PCI DSS v4.0 and regulatory compliance

## 📊 Framework Architecture Diagrams

### Integration Architecture
| Diagram | Description | Source |
|---------|-------------|--------|
| **[Framework Integration Architecture](../docs/images/framework-integration-architecture.svg)** | Complete integration model showing how MasruFi extends host systems | [PlantUML Source](../docs/puml/masrufi-framework/framework-integration-architecture.puml) |
| **[Multi-Jurisdiction Support](../docs/images/multi-jurisdiction-support.svg)** | Global Islamic banking jurisdiction support and compliance | [PlantUML Source](../docs/puml/masrufi-framework/multi-jurisdiction-support.puml) |

### Islamic Finance Process Flows
| Diagram | Description | Source |
|---------|-------------|--------|
| **[Islamic Finance Product Flow](../docs/images/islamic-finance-product-flow.svg)** | End-to-end Islamic finance product selection and lifecycle | [PlantUML Source](../docs/puml/masrufi-framework/islamic-finance-product-flow.puml) |

## 🚀 Quick Start

### Prerequisites
- Java 25.0.2+
- Spring Boot 3.2.0+
- Existing Enterprise Loan Management System

### 1. Add Dependency

#### Gradle
```gradle
dependencies {
    implementation 'com.masrufi.framework:masrufi-framework:1.0.0'
}
```

#### Maven
```xml
<dependency>
    <groupId>com.masrufi.framework</groupId>
    <artifactId>masrufi-framework</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Enable MasruFi Framework

Add to your `application.yml`:
```yaml
masrufi:
  framework:
    enabled: true
    islamic-finance:
      enabled: true
    uae-cryptocurrency:
      enabled: true
    sharia-compliance:
      enabled: true
      strict-mode: true
```

### 3. Auto-Configuration

The framework automatically configures itself through Spring Boot's auto-configuration mechanism. No additional setup required!

```java
@SpringBootApplication
public class YourEnterpriseApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourEnterpriseApplication.class, args);
        // MasruFi Framework is now available!
    }
}
```

## 📖 Usage Examples

### Creating a Murabaha Financing

```java
@Autowired
private MasrufiFrameworkFacade masrufiFramework;

public void createMurabahaFinancing() {
    CreateMurabahaCommand command = CreateMurabahaCommand.builder()
        .customerProfile(CustomerProfile.builder()
            .customerId("CUST-001")
            .customerName("Ahmed Al-Rashid")
            .customerType(CustomerType.INDIVIDUAL)
            .build())
        .assetDescription("Toyota Camry 2024")
        .assetCost(Money.of("80000", "AED"))
        .profitMargin(new BigDecimal("0.15")) // 15% profit
        .maturityDate(LocalDateTime.now().plusYears(3))
        .supplier("Toyota Dealer UAE")
        .build();

    IslamicFinancing murabaha = masrufiFramework
        .getMurabahaService()
        .createMurabaha(command);
        
    logger.info("Murabaha created: {}", murabaha.getFinancingId());
}
```

### Integration with Enterprise System

```java
@Autowired
private EnterpriseLoanSystemIntegration enterpriseIntegration;

public void integrateWithEnterpriseSystem(IslamicFinancing islamicFinancing) {
    // Register Islamic financing with enterprise system
    enterpriseIntegration.registerIslamicFinancing(islamicFinancing);
    
    // Synchronize customer data
    CustomerProfile customer = enterpriseIntegration
        .synchronizeCustomerData(islamicFinancing.getCustomerProfile().getCustomerId());
    
    // Exchange compliance data
    enterpriseIntegration.exchangeComplianceData(islamicFinancing);
}
```

### Sharia Compliance Validation

```java
@Autowired
private ShariaComplianceService shariaCompliance;

public void validateCompliance(IslamicFinancing financing) {
    boolean isCompliant = shariaCompliance.validateCompliance(financing);
    
    if (isCompliant) {
        logger.info("✅ Financing is Sharia compliant");
    } else {
        logger.warn("❌ Sharia compliance issues detected");
    }
}
```

## 🔧 Configuration

### Complete Configuration Example

```yaml
masrufi:
  framework:
    enabled: true
    version: "1.0.0"
    integration-mode: EXTENSION
    
    islamic-finance:
      enabled: true
      default-currency: "AED"
      supported-models:
        - MURABAHA
        - MUSHARAKAH
        - IJARAH
        - SALAM
        - ISTISNA
        - QARD_HASSAN
      supported-jurisdictions:
        - UAE
        - SAUDI_ARABIA
        - QATAR
        - KUWAIT
        - BAHRAIN
        - OMAN
      business-rules:
        enabled: true
        hot-reload-enabled: true
        validate-on-startup: true
    
    uae-cryptocurrency:
      enabled: true
      supported-currencies:
        - UAE-CBDC
        - ADIB-DD
        - ENBD-DC
        - FAB-DT
        - CBD-DD
        - RAK-DC
        - MASHREQ-DC
      smart-contract:
        enabled: true
        gas-limit: 500000
    
    sharia-compliance:
      enabled: true
      strict-mode: true
      sharia-board: "UAE_HIGHER_SHARIA_AUTHORITY"
      rules:
        validate-riba: true
        validate-gharar: true
        validate-asset-backing: true
        max-profit-margin: 30.0
    
    enterprise-integration:
      host-system-base-url: "http://localhost:8080"
      authentication-method: JWT
      event-publishing:
        enabled: true
        topic-prefix: "masrufi.events"
      data-sync:
        enabled: true
        sync-interval-seconds: 300
```

## 🏛️ Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                 Enterprise Loan Management System          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              MasruFi Framework Module               │   │
│  │                                                     │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐ │   │
│  │  │  Murabaha   │  │ Musharakah  │  │   Ijarah    │ │   │
│  │  │   Service   │  │   Service   │  │   Service   │ │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘ │   │
│  │                                                     │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐ │   │
│  │  │    Salam    │  │   Istisna   │  │ Qard Hassan │ │   │
│  │  │   Service   │  │   Service   │  │   Service   │ │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘ │   │
│  │                                                     │   │
│  │  ┌─────────────────────────────────────────────────┐ │   │
│  │  │        Sharia Compliance Service               │ │   │
│  │  └─────────────────────────────────────────────────┘ │   │
│  │                                                     │   │
│  │  ┌─────────────────────────────────────────────────┐ │   │
│  │  │     Enterprise Integration Service              │ │   │
│  │  └─────────────────────────────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Integration Points

1. **Event Publishing**: Islamic finance events published to enterprise event bus
2. **Data Synchronization**: Customer and loan data synchronized between systems
3. **Compliance Exchange**: Sharia compliance data shared with enterprise compliance systems
4. **API Integration**: RESTful APIs for direct integration
5. **Health Monitoring**: Health indicators for operational monitoring

## 🏢 Enterprise Banking Platform Integration

### 🔐 Advanced Security Features

The MasruFi Framework now includes enterprise-grade security components:

#### Multi-Factor Authentication Service
- **TOTP Authentication**: Google Authenticator and compatible apps
- **SMS OTP**: 6-digit codes with 5-minute expiry
- **Email OTP**: 8-character alphanumeric codes
- **Hardware Token**: RSA SecurID and YubiKey support
- **Biometric Authentication**: Fingerprint and facial recognition
- **Account Lockout**: 5 failed attempts trigger 30-minute lockout
- **Session Management**: Secure session handling with 30-minute expiry

#### Security Audit Filter
- **Real-time Logging**: All API requests logged with metadata
- **Sensitive Data Access**: Special tracking for customer, loan, and payment data
- **Admin Action Monitoring**: Detailed classification of administrative actions
- **Compliance Events**: Automatic compliance event generation for auditing
- **Authentication Detection**: Automatic detection of JWT, OAuth2, and Certificate authentication
- **Performance Monitoring**: Response time tracking and performance metrics

### 📊 Production Monitoring & Observability

#### Grafana Dashboards
- **Islamic Banking Overview**: System health and business metrics
- **Sharia Compliance Metrics**: Compliance scoring and validation rates
- **UAE Cryptocurrency Tracking**: CBDC transaction volumes and settlement times
- **Customer Journey Analytics**: Islamic banking customer conversion funnels

#### Prometheus Alerts
- **Sharia Compliance Violations**: Real-time alerts for compliance breaches
- **High-Value Transaction Monitoring**: Alerts for transactions >10K AED
- **Authentication Failures**: Security alerts for failed authentication attempts
- **Business KPI Alerts**: Islamic banking conversion rates and profit margins

### 🚀 Production Infrastructure

#### CI/CD Pipeline
- **Automated Testing**: 300+ tests including Islamic finance compliance
- **Security Scanning**: SAST, DAST, and dependency vulnerability scanning
- **Sharia Compliance Validation**: Automated validation of Islamic finance rules
- **Blue-Green Deployment**: Zero-downtime deployment with health checks

#### Database Optimization
- **Multi-Level Caching**: L1 (Caffeine), L2 (Redis), L3 (Database)
- **Event Sourcing**: Complete audit trail for Islamic finance transactions
- **Partitioning Strategy**: Optimized for Islamic banking transaction patterns
- **Connection Pooling**: HikariCP optimization for high-throughput scenarios

## 📊 Monitoring & Operations

### Health Checks

The framework provides comprehensive health indicators:

```bash
# Check overall framework health
curl http://localhost:8080/actuator/health/masrufi

# Check specific service health
curl http://localhost:8080/actuator/health/islamic-finance
curl http://localhost:8080/actuator/health/sharia-compliance
curl http://localhost:8080/actuator/health/uae-cryptocurrency
curl http://localhost:8080/actuator/health/enterprise-security
curl http://localhost:8080/actuator/health/mfa-service
```

### Metrics

Available metrics include:
- Islamic financing creation rate
- Sharia compliance validation success rate
- UAE cryptocurrency transaction volume
- Integration health status
- Business rule execution performance
- MFA authentication success rates
- Security audit event rates
- Enterprise banking platform integration metrics

### Logging

The framework uses structured logging with Islamic finance context:

```
🕌 [MasruFi] 14:30:15.123 INFO  MurabahaService - Creating Murabaha financing for customer: CUST-001
🕌 [MasruFi] 14:30:15.234 INFO  ShariaComplianceService - Validating Sharia compliance for financing: MURABAHA-1634567890123
🕌 [MasruFi] 14:30:15.345 INFO  UAECryptocurrencyService - Processing UAE-CBDC transaction: TX-1634567890456
🔒 [MasruFi] 14:30:15.456 INFO  MFAService - TOTP authentication successful for customer: CUST-001
🔍 [MasruFi] 14:30:15.567 INFO  SecurityAuditFilter - Sensitive data access logged: customer-profile
```

## 🧪 Testing

### Unit Tests

```bash
./gradlew test
```

### Integration Tests

```bash
./gradlew integrationTest
```

### Architecture Tests

```bash
./gradlew archTest
```

### Module Validation

```bash
./gradlew validateModule
```

## 🔐 Security

### Sharia Compliance Security
- All transactions validated against Islamic principles
- Asset permissibility checks
- Profit margin validation
- Gharar (uncertainty) elimination

### Data Security
- Encrypted storage of sensitive Islamic finance data
- Secure communication with UAE cryptocurrency networks
- Audit trails for all Sharia compliance decisions

### Integration Security
- JWT-based authentication with enterprise systems
- mTLS for secure service communication
- Role-based access control for Islamic finance operations

## 🌍 Multi-Jurisdiction Support

The framework supports Islamic finance operations across multiple jurisdictions:

- **🇦🇪 UAE**: Complete support with UAE cryptocurrency integration
- **🇸🇦 Saudi Arabia**: Saudi Islamic banking standards
- **🇶🇦 Qatar**: Qatar Central Bank compliance
- **🇰🇼 Kuwait**: Kuwait Finance House standards
- **🇧🇭 Bahrain**: Bahrain Central Bank Islamic banking rules
- **🇴🇲 Oman**: Bank Muscat Islamic finance compliance
- **🇹🇷 Turkey**: Turkish participation banking regulations
- **🇵🇰 Pakistan**: State Bank of Pakistan Islamic banking directives

## 📝 License

This software is proprietary and licensed under the MasruFi Framework License.
Copyright © 2024 Ali&Co. All rights reserved.

## 🤝 Support

### Documentation
- [Framework Documentation](https://docs.masrufi.com)
- [API Reference](https://api.masrufi.com/docs)
- [Integration Guide](https://docs.masrufi.com/integration)

### Contact
- **Development Team**: [dev@masrufi.com](mailto:dev@masrufi.com)
- **Technical Support**: [support@masrufi.com](mailto:support@masrufi.com)
- **Compliance Questions**: [compliance@masrufi.com](mailto:compliance@masrufi.com)
- **UAE Operations**: [uae@masrufi.com](mailto:uae@masrufi.com)

### Community
- [GitHub Issues](https://github.com/COPUR/enterprise-loan-management-system/issues)
- [Stack Overflow Tag: masrufi-framework](https://stackoverflow.com/questions/tagged/masrufi-framework)

---

## 🏆 About Ali&Co

MasruFi Framework is developed by **Ali&Co**, a leading provider of enterprise financial technology solutions. With deep expertise in Islamic finance and modern software architecture, we deliver production-ready solutions for global financial institutions.

**Expertise Areas:**
- Islamic Finance Technology
- Enterprise Banking Systems
- UAE Financial Services
- Sharia Compliance Automation
- Cryptocurrency Integration
- Multi-Jurisdiction Financial Platforms

---

*🕌 Building the future of Islamic finance technology, one transaction at a time.*
