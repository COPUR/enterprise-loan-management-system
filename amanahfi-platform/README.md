# 🌙 AmanahFi Platform - UAE & MENAT Islamic Finance Platform

[![Java 21](https://img.shields.io/badge/Java-21-ED8B00.svg?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3.3](https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F.svg?logo=spring-boot)](https://spring.io/projects/spring-boot)
[![CBUAE Compliant](https://img.shields.io/badge/CBUAE-Compliant-00A859)](https://centralbank.ae/)
[![VARA Compliant](https://img.shields.io/badge/VARA-Compliant-B8860B)](https://vara.ae/)
[![HSA Approved](https://img.shields.io/badge/HSA-Approved-228B22)](https://www.centralbank.ae/en/focus/islamic-banking)
[![CBDC Ready](https://img.shields.io/badge/CBDC-Ready-4169E1)](https://www.centralbank.ae/en/focus/digital-currency)

## 🏛️ Overview

**AmanahFi Platform** is a comprehensive Islamic finance platform designed for the UAE and MENAT (Middle East, North Africa, and Turkey) region. Built with cutting-edge technology and strict adherence to Sharia principles, regulatory compliance, and modern banking standards.

### 🎯 Key Features

- **🕌 Sharia-Compliant Financial Products**: Full implementation of 6 Islamic finance models
- **🏛️ Regulatory Compliance**: CBUAE, VARA, HSA, and MENAT regional compliance
- **💎 CBDC Integration**: Digital Dirham (AED-CBDC) and multi-currency support
- **🌐 Multi-Tenancy**: Country-specific deployments across MENAT region
- **🔒 Zero Trust Security**: Comprehensive security architecture with mTLS
- **📊 Real-time Analytics**: Advanced risk assessment and compliance monitoring
- **🌍 Multilingual Support**: Arabic, English, Turkish, Urdu, Persian, French

## 🏗️ Architecture

### Hexagonal (Ports & Adapters) Architecture
```
┌─────────────────────────────────────────────────┐
│                 Web Layer                       │
│        (REST APIs, GraphQL, WebSocket)         │
├─────────────────────────────────────────────────┤
│                Application Layer                │
│         (Use Cases, Command Handlers)          │
├─────────────────────────────────────────────────┤
│                 Domain Layer                    │
│    (Business Logic, Domain Models, Events)     │
├─────────────────────────────────────────────────┤
│              Infrastructure Layer              │
│   (Database, Kafka, External APIs, CBDC)      │
└─────────────────────────────────────────────────┘
```

### Event-Driven Architecture
- **Custom Event Store**: Built without Axon Framework
- **Apache Kafka**: Event streaming and message processing
- **Event Sourcing**: Complete audit trail and state reconstruction
- **CQRS Pattern**: Separate read and write models
- **Saga Pattern**: Distributed transaction management

## 🕌 Islamic Finance Models

### 1. Murabaha (Cost-Plus Financing)
- Asset-based financing with disclosed profit margin
- Sharia-compliant alternative to conventional loans
- Clear ownership transfer and asset backing

### 2. Musharakah (Partnership Financing)
- Profit and loss sharing partnerships
- Joint venture financing structures
- Risk sharing between parties

### 3. Ijarah (Lease Financing)
- Asset leasing with ownership retention
- Equipment and real estate financing
- Rental-based revenue model

### 4. Salam (Forward Sale Financing)
- Commodity forward financing
- Agricultural and commodity trading
- Deferred delivery contracts

### 5. Istisna (Manufacturing Financing)
- Project and construction financing
- Made-to-order asset financing
- Progressive financing structures

### 6. Qard Hassan (Benevolent Loan)
- Interest-free charitable loans
- Social finance and community support
- Administrative fee-only structure

## 🏛️ Regulatory Compliance

### UAE Central Bank (CBUAE)
- Open Finance API compliance
- Basel III regulatory framework
- Digital banking regulations
- Anti-Money Laundering (AML) compliance

### Virtual Asset Regulatory Authority (VARA)
- Cryptocurrency asset compliance
- Digital asset custody regulations
- Virtual asset service provider (VASP) requirements
- DLT and blockchain compliance

### Higher Sharia Authority (HSA)
- Islamic finance product approval
- Sharia governance framework
- Compliance monitoring and reporting
- Fatwa management system

## 💎 CBDC & Cryptocurrency Support

### Digital Dirham (AED-CBDC)
- R3 Corda integration for CBDC transactions
- Central bank digital currency support
- Cross-border payment facilitation
- Real-time settlement capabilities

### Multi-Currency Support
- UAE Dirham (AED)
- Saudi Riyal (SAR)
- Turkish Lira (TRY)
- Pakistani Rupee (PKR)
- Other MENAT currencies

## 🌍 Multi-Tenant Architecture

### Geographic Coverage
- **🇦🇪 UAE**: Primary market with full CBDC integration
- **🇸🇦 Saudi Arabia**: SAMA compliance and Riyal support
- **🇹🇷 Turkey**: BDDK compliance and Lira integration
- **🇵🇰 Pakistan**: SBP compliance and Rupee support
- **🇦🇿 Azerbaijan**: CBAR compliance and Manat support
- **🇮🇷 Iran**: CBI compliance and Rial support
- **🇮🇱 Israel**: BOI compliance and Shekel support

### Data & Computational Sovereignty
- Country-specific data residency
- Local regulatory compliance
- Sovereign cloud deployment
- Regional data governance

## 🔒 Security Architecture

### Zero Trust Security Model
- **Identity Verification**: Keycloak IAM with OAuth 2.1
- **Network Security**: mTLS for all communications
- **API Security**: FAPI 2.0 Advanced security profile
- **Data Encryption**: End-to-end encryption at rest and in transit
- **Audit Logging**: Comprehensive security event logging

### Security Features
- Multi-factor authentication (MFA)
- Biometric authentication support
- Hardware security module (HSM) integration
- Real-time fraud detection
- Advanced threat protection

## 🚀 Technology Stack

### Core Technologies
- **Java 21**: Latest LTS with virtual threads
- **Spring Boot 3.3**: Enterprise application framework
- **PostgreSQL**: Primary database with encryption
- **Apache Kafka**: Event streaming platform
- **Redis**: Caching and session management

### Integration Technologies
- **R3 Corda**: CBDC and DLT integration
- **Keycloak**: Identity and access management
- **Drools**: Business rules engine
- **OpenAPI 3.0**: API documentation
- **Micrometer**: Observability and metrics

### DevOps & Infrastructure
- **Docker**: Containerization
- **Kubernetes**: Container orchestration
- **Istio**: Service mesh
- **Prometheus**: Monitoring
- **Grafana**: Visualization

## 📊 Business Capabilities

### Customer Onboarding
- Digital KYC/AML verification
- Sharia-compliant customer screening
- Multi-jurisdiction compliance checking
- Real-time risk assessment

### Product Origination
- Islamic finance product catalog
- Sharia compliance validation
- Automated underwriting
- Risk-based pricing

### Payment Processing
- Real-time payment processing
- CBDC transaction support
- Cross-border payments
- Settlement and clearing

### Compliance & Reporting
- Real-time compliance monitoring
- Regulatory reporting automation
- Audit trail management
- Risk analytics and dashboards

## 🧪 Testing Strategy

### Test-Driven Development (TDD)
- **95%+ Code Coverage**: Comprehensive test coverage
- **Unit Tests**: Domain logic validation
- **Integration Tests**: Component interaction testing
- **Contract Tests**: API contract validation
- **Architecture Tests**: Architectural constraint enforcement

### Quality Assurance
- **SonarQube**: Code quality analysis
- **ArchUnit**: Architecture compliance testing
- **Testcontainers**: Integration test infrastructure
- **Performance Testing**: Load and stress testing

## 🌐 Internationalization (i18n)

### Supported Languages
- **🇦🇪 Arabic**: Primary language for UAE and regional markets
- **🇬🇧 English**: International business language
- **🇹🇷 Turkish**: Turkey market support
- **🇵🇰 Urdu**: Pakistan market support
- **🇮🇷 Persian**: Iran market support
- **🇫🇷 French**: North Africa market support

### Localization Features
- Right-to-left (RTL) text support
- Cultural and religious calendar integration
- Local number and currency formatting
- Timezone and date localization

## 🚀 Getting Started

### Prerequisites
- Java 21 or higher
- Docker and Docker Compose
- PostgreSQL 15+
- Apache Kafka 3.7+
- Redis 7+

### Development Setup
```bash
# Clone the repository
git clone <repository-url>
cd amanahfi-platform

# Start infrastructure services
docker-compose up -d postgres kafka redis keycloak

# Run the application
./gradlew bootRun

# Run tests
./gradlew test

# Generate test coverage report
./gradlew jacocoTestReport
```

### Production Deployment
```bash
# Build production image
./gradlew bootBuildImage

# Deploy to Kubernetes
kubectl apply -f k8s/

# Monitor deployment
kubectl get pods -n amanahfi-platform
```

## 📈 Monitoring & Observability

### Metrics
- Business metrics (transaction volumes, success rates)
- Technical metrics (response times, error rates)
- Security metrics (authentication failures, fraud attempts)
- Compliance metrics (regulatory reporting status)

### Logging
- Structured JSON logging
- Correlation ID tracking
- Security event logging
- Performance monitoring

### Alerting
- Real-time alert management
- Regulatory compliance alerts
- Security incident alerts
- Business metric alerts

## 🤝 Contributing

### Development Guidelines
- Follow hexagonal architecture principles
- Implement comprehensive test coverage
- Adhere to Sharia compliance requirements
- Maintain regulatory compliance standards
- Use defensive programming practices

### Code Quality Standards
- **Clean Code**: Robert Martin principles
- **SOLID Principles**: Object-oriented design
- **DRY Principle**: Don't repeat yourself
- **YAGNI Principle**: You ain't gonna need it
- **TDD Approach**: Test-driven development

## 📋 Compliance Certifications

- ✅ **CBUAE Open Finance** - API compliance
- ✅ **VARA Digital Assets** - Cryptocurrency compliance
- ✅ **HSA Sharia Governance** - Islamic finance compliance
- ✅ **ISO 27001** - Information security management
- ✅ **PCI DSS** - Payment card industry compliance
- ✅ **SOC 2 Type II** - Service organization controls

## 📞 Support

### Technical Support
- **Email**: support@amanahfi.ae
- **Phone**: +971-4-XXX-XXXX
- **Portal**: https://support.amanahfi.ae

### Regulatory Inquiries
- **CBUAE Compliance**: compliance-uae@amanahfi.ae
- **Sharia Board**: sharia@amanahfi.ae
- **Risk Management**: risk@amanahfi.ae

## 📄 License

This project is proprietary software owned by AmanahFi Platform. All rights reserved.

---

**Built with 💚 for the Islamic Finance Community**

*Empowering ethical finance through technology excellence*