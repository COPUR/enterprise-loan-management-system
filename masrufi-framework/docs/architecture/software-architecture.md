# 🏗️ MasruFi Framework - Software Architecture Specification

[![Architecture Version](https://img.shields.io/badge/architecture-v1.0.0-blue.svg)](https://masrufi.com)
[![Framework Status](https://img.shields.io/badge/status-Production--Ready-green.svg)](https://masrufi.com)
[![Compliance](https://img.shields.io/badge/Architecture-Hexagonal-gold.svg)](https://masrufi.com/architecture)

**Document Information:**
- **Document Type**: Software Architecture Specification
- **Version**: 1.0.0
- **Last Updated**: December 2024
- **Architect**: Ali&Co Architecture Team
- **Classification**: Technical Documentation
- **Audience**: Solution Architects, Technical Leads, Senior Developers

## 🎯 Architecture Overview

The **MasruFi Framework** implements a **Hexagonal Architecture** (Ports and Adapters pattern) combined with **Domain-Driven Design (DDD)** principles to create a high-cohesion, loosely-coupled Islamic Finance extension module. This architecture ensures that the framework can integrate seamlessly with existing enterprise loan management systems without requiring modifications to core business logic.

### **Architecture Principles**

1. **🔗 High Cohesion**: All Islamic Finance logic is centralized within the framework module
2. **🔀 Loose Coupling**: Minimal dependencies on host enterprise systems
3. **🔌 Plug-and-Play**: Non-invasive integration through well-defined interfaces
4. **📦 Domain Isolation**: Clear separation between Islamic Finance domain and technical concerns
5. **🔄 Event-Driven**: Asynchronous communication through domain events
6. **🎯 SOLID Principles**: Adherence to SOLID design principles throughout
7. **🧪 Testable Design**: Architecture optimized for comprehensive testing strategies

## 🏛️ Hexagonal Architecture Implementation

```
                    🕌 MasruFi Framework - Hexagonal Architecture
                    
┌─────────────────────────────────────────────────────────────────────────┐
│                              EXTERNAL WORLD                            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  🌐 Web APIs        📱 Mobile Apps       🏦 Enterprise Systems         │
│  💳 Crypto Networks  🔒 HSA Services     📊 Monitoring Systems         │
│                                                                         │
├─────────────────────────────────────────────────────────────────────────┤
│                           ADAPTERS (Infrastructure)                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐   │
│  │   REST API  │  │  Event Bus  │  │ Database    │  │ Crypto      │   │
│  │   Adapter   │  │   Adapter   │  │ Adapter     │  │ Adapter     │   │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘   │
│                                                                         │
├─────────────────────────────────────────────────────────────────────────┤
│                              PORTS (Interfaces)                        │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐   │
│  │   Primary   │  │   Primary   │  │  Secondary  │  │  Secondary  │   │
│  │    Ports    │  │    Ports    │  │    Ports    │  │    Ports    │   │
│  │    (In)     │  │    (In)     │  │    (Out)    │  │    (Out)    │   │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘   │
│                                                                         │
├─────────────────────────────────────────────────────────────────────────┤
│                         DOMAIN CORE (Business Logic)                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  🕌 Islamic Finance Domain                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐   │
│  │  Murabaha   │  │ Musharakah  │  │   Ijarah    │  │   Salam     │   │
│  │   Service   │  │   Service   │  │   Service   │  │   Service   │   │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘   │
│                                                                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────────────┐ │
│  │   Istisna   │  │ Qard Hassan │  │    Sharia Compliance Service   │ │
│  │   Service   │  │   Service   │  │                                 │ │
│  └─────────────┘  └─────────────┘  └─────────────────────────────────┘ │
│                                                                         │
│  🔒 Shared Kernel                                                      │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │  Value Objects │ Domain Events │ Business Rules │ Entities     │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

## 📦 Domain Model Architecture

### **Domain Structure**

The MasruFi Framework follows **Domain-Driven Design** with clear bounded contexts for Islamic Finance:

```java
com.masrufi.framework.domain/
├── exception/                      // Domain-specific exceptions
│   ├── ShariaViolationException
│   ├── AssetValidationException
│   └── CustomerValidationException
├── model/                         // Domain entities and value objects
│   ├── IslamicFinancing           // Aggregate root
│   ├── CustomerProfile            // Entity
│   ├── Money                      // Value object
│   ├── ShariaComplianceResult     // Value object
│   └── IslamicFinancingId         // Identity value object
├── port/
│   ├── in/                        // Primary ports (use cases)
│   │   └── CreateMurabahaUseCase
│   └── out/                       // Secondary ports (infrastructure)
│       ├── IslamicFinancingRepository
│       ├── ShariaComplianceValidationPort
│       ├── AssetValidationPort
│       └── CustomerValidationPort
└── service/                       // Domain services
    ├── MurabahaService
    ├── MusharakahService
    ├── IjarahService
    ├── SalamService
    ├── IstisnaService
    ├── QardHassanService
    └── ShariaComplianceService
```

### **Aggregate Design**

#### **Islamic Financing Aggregate**

```java
@Entity
@Table(name = "islamic_financing")
public class IslamicFinancing implements AggregateRoot<IslamicFinancingId> {
    
    @EmbeddedId
    private IslamicFinancingId id;
    
    @Embedded
    private CustomerProfile customerProfile;
    
    @Enumerated(EnumType.STRING)
    private IslamicFinancingType type;
    
    @Embedded
    private Money principal;
    
    @Embedded
    private Money totalAmount;
    
    private BigDecimal profitMargin;
    
    @Enumerated(EnumType.STRING)
    private FinancingStatus status;
    
    @Embedded
    private ShariaComplianceResult complianceResult;
    
    private LocalDateTime createdAt;
    private LocalDateTime maturityDate;
    
    // Domain methods
    public void validateShariaCompliance() { /* Business logic */ }
    public void calculateProfitDistribution() { /* Business logic */ }
    public void processPayment(Money amount) { /* Business logic */ }
    public void markAsCompleted() { /* Business logic */ }
    
    // Domain events
    @DomainEvents
    Collection<DomainEvent> domainEvents() { /* Event publishing */ }
}
```

#### **Value Objects**

```java
@Embeddable
public class Money {
    private BigDecimal amount;
    private Currency currency;
    
    public Money add(Money other) { /* Implementation */ }
    public Money subtract(Money other) { /* Implementation */ }
    public Money multiply(BigDecimal factor) { /* Implementation */ }
    public boolean isPositive() { /* Implementation */ }
    
    // Immutable value object with business rules
}

@Embeddable
public class ShariaComplianceResult {
    private boolean compliant;
    private String validationReference;
    private LocalDateTime validatedAt;
    private String validatedBy;
    private List<String> complianceChecks;
    
    public static ShariaComplianceResult compliant(String reference) { /* Factory */ }
    public static ShariaComplianceResult nonCompliant(String reason) { /* Factory */ }
}
```

## 🔌 Port and Adapter Pattern Implementation

### **Primary Ports (Inbound)**

Primary ports define the use cases that drive the Islamic Finance domain:

```java
// Use case interfaces (Primary Ports)
public interface CreateMurabahaUseCase {
    IslamicFinancing createMurabaha(CreateMurabahaCommand command);
}

public interface CreateMusharakahUseCase {
    IslamicFinancing createMusharakah(CreateMusharakahCommand command);
}

public interface ValidateShariaComplianceUseCase {
    ShariaComplianceResult validateCompliance(IslamicFinancing financing);
}

public interface ProcessCryptocurrencyPaymentUseCase {
    PaymentResult processCryptoPayment(CryptocurrencyPaymentCommand command);
}
```

### **Secondary Ports (Outbound)**

Secondary ports define interfaces for external dependencies:

```java
// Repository interfaces (Secondary Ports)
public interface IslamicFinancingRepository {
    IslamicFinancingId save(IslamicFinancing financing);
    Optional<IslamicFinancing> findById(IslamicFinancingId id);
    List<IslamicFinancing> findByCustomerId(CustomerId customerId);
    void delete(IslamicFinancingId id);
}

// External service interfaces (Secondary Ports)
public interface ShariaComplianceValidationPort {
    ShariaComplianceResult validateMurabaha(MurabahaContract contract);
    ShariaComplianceResult validateMusharakah(MusharakahContract contract);
    ShariaComplianceResult validateAssetPermissibility(Asset asset);
}

public interface UAECryptocurrencyNetworkPort {
    CryptocurrencyBalance getBalance(WalletAddress address);
    TransactionResult transfer(CryptocurrencyTransferRequest request);
    ExchangeRate getExchangeRate(Currency from, Currency to);
}

public interface EnterpriseLoanSystemPort {
    CustomerProfile getCustomerProfile(CustomerId customerId);
    AccountInformation getAccountInformation(AccountId accountId);
    void publishEvent(DomainEvent event);
    ComplianceRecord createComplianceRecord(ComplianceData data);
}
```

### **Adapter Implementations**

#### **REST API Adapter (Primary)**

```java
@RestController
@RequestMapping("/api/v1/islamic-finance")
@Validated
public class IslamicFinanceRestAdapter {
    
    private final CreateMurabahaUseCase createMurabahaUseCase;
    private final CreateMusharakahUseCase createMusharakahUseCase;
    private final ValidateShariaComplianceUseCase shariaComplianceUseCase;
    
    @PostMapping("/murabaha")
    public ResponseEntity<IslamicFinancingResponse> createMurabaha(
            @Valid @RequestBody CreateMurabahaRequest request) {
        
        CreateMurabahaCommand command = mapToCommand(request);
        IslamicFinancing financing = createMurabahaUseCase.createMurabaha(command);
        
        return ResponseEntity.ok(mapToResponse(financing));
    }
    
    @PostMapping("/musharakah")
    public ResponseEntity<IslamicFinancingResponse> createMusharakah(
            @Valid @RequestBody CreateMusharakahRequest request) {
        
        CreateMusharakahCommand command = mapToCommand(request);
        IslamicFinancing financing = createMusharakahUseCase.createMusharakah(command);
        
        return ResponseEntity.ok(mapToResponse(financing));
    }
    
    @PostMapping("/validate-compliance")
    public ResponseEntity<ShariaComplianceResponse> validateCompliance(
            @Valid @RequestBody ShariaComplianceRequest request) {
        
        IslamicFinancing financing = findFinancing(request.getFinancingId());
        ShariaComplianceResult result = shariaComplianceUseCase.validateCompliance(financing);
        
        return ResponseEntity.ok(mapToResponse(result));
    }
}
```

#### **Database Adapter (Secondary)**

```java
@Repository
@Transactional
public class JpaIslamicFinancingRepository implements IslamicFinancingRepository {
    
    private final SpringDataIslamicFinancingRepository springRepository;
    private final IslamicFinancingMapper mapper;
    
    @Override
    public IslamicFinancingId save(IslamicFinancing financing) {
        IslamicFinancingJpaEntity entity = mapper.toEntity(financing);
        IslamicFinancingJpaEntity saved = springRepository.save(entity);
        return saved.getId();
    }
    
    @Override
    public Optional<IslamicFinancing> findById(IslamicFinancingId id) {
        return springRepository.findById(id)
                .map(mapper::toDomain);
    }
    
    @Override
    public List<IslamicFinancing> findByCustomerId(CustomerId customerId) {
        return springRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}

// JPA Entity for persistence
@Entity
@Table(name = "islamic_financing")
public class IslamicFinancingJpaEntity {
    
    @Id
    @Column(name = "financing_id")
    private String id;
    
    @Column(name = "customer_id", nullable = false)
    private String customerId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "financing_type", nullable = false)
    private IslamicFinancingType type;
    
    @Column(name = "principal_amount", precision = 19, scale = 2)
    private BigDecimal principalAmount;
    
    @Column(name = "principal_currency", length = 3)
    private String principalCurrency;
    
    @Column(name = "total_amount", precision = 19, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(name = "profit_margin", precision = 5, scale = 4)
    private BigDecimal profitMargin;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private FinancingStatus status;
    
    @Column(name = "sharia_compliant")
    private Boolean shariaCompliant;
    
    @Column(name = "compliance_reference")
    private String complianceReference;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "maturity_date")
    private LocalDateTime maturityDate;
    
    // Getters, setters, constructors
}
```

#### **Event Publishing Adapter (Secondary)**

```java
@Component
public class KafkaEventPublishingAdapter implements DomainEventPublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    @Override
    @Async
    public void publish(DomainEvent event) {
        try {
            String topic = resolveTopicName(event);
            String eventJson = objectMapper.writeValueAsString(event);
            
            ProducerRecord<String, Object> record = new ProducerRecord<>(
                topic, 
                event.getAggregateId(), 
                eventJson
            );
            
            // Add headers for event metadata
            record.headers().add("event-type", event.getClass().getSimpleName().getBytes());
            record.headers().add("event-version", "1.0".getBytes());
            record.headers().add("timestamp", String.valueOf(event.getOccurredOn().toEpochMilli()).getBytes());
            
            kafkaTemplate.send(record);
            
            log.info("Published event: {} for aggregate: {}", 
                event.getClass().getSimpleName(), 
                event.getAggregateId());
                
        } catch (Exception e) {
            log.error("Failed to publish event: {}", event, e);
            // Implement retry mechanism or dead letter queue
        }
    }
    
    private String resolveTopicName(DomainEvent event) {
        return "masrufi.islamic-finance." + 
               CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, 
                   event.getClass().getSimpleName().replace("Event", ""));
    }
}
```

## 🔄 Event-Driven Architecture

### **Domain Events**

The framework publishes domain events for integration with enterprise systems:

```java
// Base domain event
public abstract class DomainEvent {
    private final String aggregateId;
    private final Instant occurredOn;
    private final String eventId;
    
    protected DomainEvent(String aggregateId) {
        this.aggregateId = aggregateId;
        this.occurredOn = Instant.now();
        this.eventId = UUID.randomUUID().toString();
    }
    
    // Getters and utility methods
}

// Islamic Finance specific events
public class MurabahaContractCreatedEvent extends DomainEvent {
    private final String contractId;
    private final String customerId;
    private final Money assetValue;
    private final Money profitAmount;
    private final Money totalPrice;
    private final LocalDateTime maturityDate;
    private final boolean shariaCompliant;
    
    public MurabahaContractCreatedEvent(String contractId, 
                                       String customerId,
                                       Money assetValue,
                                       Money profitAmount,
                                       Money totalPrice,
                                       LocalDateTime maturityDate,
                                       boolean shariaCompliant) {
        super(contractId);
        this.contractId = contractId;
        this.customerId = customerId;
        this.assetValue = assetValue;
        this.profitAmount = profitAmount;
        this.totalPrice = totalPrice;
        this.maturityDate = maturityDate;
        this.shariaCompliant = shariaCompliant;
    }
    
    // Getters
}

public class ShariaComplianceValidatedEvent extends DomainEvent {
    private final String financingId;
    private final boolean compliant;
    private final String validationReference;
    private final List<String> complianceChecks;
    private final String validatedBy;
    
    // Constructor and getters
}

public class CryptocurrencyPaymentProcessedEvent extends DomainEvent {
    private final String paymentId;
    private final String financingId;
    private final Money amount;
    private final String cryptoCurrency;
    private final String transactionHash;
    private final String networkId;
    
    // Constructor and getters
}
```

### **Event Handlers**

```java
@Component
@Slf4j
public class IslamicFinanceEventHandler {
    
    private final EnterpriseLoanSystemIntegration enterpriseIntegration;
    private final NotificationService notificationService;
    private final AuditService auditService;
    
    @EventListener
    @Async
    public void handleMurabahaContractCreated(MurabahaContractCreatedEvent event) {
        log.info("Processing Murabaha contract created event: {}", event.getContractId());
        
        // Notify enterprise system
        enterpriseIntegration.notifyLoanCreated(event);
        
        // Send customer notification
        notificationService.sendContractCreatedNotification(event);
        
        // Create audit record
        auditService.recordContractCreation(event);
        
        log.info("Murabaha contract event processing completed: {}", event.getContractId());
    }
    
    @EventListener
    @Async
    public void handleShariaComplianceValidated(ShariaComplianceValidatedEvent event) {
        log.info("Processing Sharia compliance validation event: {}", event.getFinancingId());
        
        if (!event.isCompliant()) {
            // Handle non-compliance
            notificationService.sendComplianceViolationAlert(event);
            auditService.recordComplianceViolation(event);
        } else {
            // Record successful compliance
            auditService.recordComplianceValidation(event);
        }
    }
    
    @EventListener
    @Async
    public void handleCryptocurrencyPaymentProcessed(CryptocurrencyPaymentProcessedEvent event) {
        log.info("Processing cryptocurrency payment event: {}", event.getPaymentId());
        
        // Update enterprise system
        enterpriseIntegration.updatePaymentStatus(event);
        
        // Regulatory reporting
        auditService.recordCryptocurrencyTransaction(event);
    }
}
```

## 🛡️ Security Architecture

### **Authentication & Authorization**

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class MasrufiSecurityConfiguration {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                    .jwtDecoder(jwtDecoder())
                )
            )
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/v1/islamic-finance/**").hasRole("ISLAMIC_FINANCE_USER")
                .requestMatchers("/api/v1/sharia-compliance/**").hasRole("SHARIA_OFFICER")
                .requestMatchers("/api/v1/cryptocurrency/**").hasRole("CRYPTO_OPERATOR")
                .requestMatchers("/actuator/health/**").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .build();
    }
    
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<String> authorities = jwt.getClaimAsStringList("authorities");
            return authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        });
        return converter;
    }
}

// Role-based access control for Islamic Finance operations
@PreAuthorize("hasRole('ISLAMIC_FINANCE_OFFICER')")
public class IslamicFinanceService {
    
    @PreAuthorize("hasAuthority('CREATE_MURABAHA')")
    public IslamicFinancing createMurabaha(CreateMurabahaCommand command) {
        // Implementation
    }
    
    @PreAuthorize("hasAuthority('VALIDATE_SHARIA_COMPLIANCE')")
    public ShariaComplianceResult validateCompliance(IslamicFinancing financing) {
        // Implementation
    }
}
```

### **Data Encryption**

```java
@Component
public class IslamicFinanceDataEncryption {
    
    private final AESUtil aesUtil;
    
    @EventListener
    public void encryptSensitiveData(IslamicFinancing financing) {
        // Encrypt sensitive customer data
        CustomerProfile encrypted = encryptCustomerProfile(financing.getCustomerProfile());
        financing.updateCustomerProfile(encrypted);
    }
    
    private CustomerProfile encryptCustomerProfile(CustomerProfile profile) {
        return CustomerProfile.builder()
            .customerId(profile.getCustomerId()) // Not encrypted - used for queries
            .customerName(aesUtil.encrypt(profile.getCustomerName()))
            .nationalId(aesUtil.encrypt(profile.getNationalId()))
            .phoneNumber(aesUtil.encrypt(profile.getPhoneNumber()))
            .emailAddress(aesUtil.encrypt(profile.getEmailAddress()))
            .build();
    }
}
```

## 🧪 Testing Architecture

### **Test Structure**

```
src/test/java/com/masrufi/framework/
├── architecture/                  // Architecture tests
│   └── ArchitectureTest.java
├── domain/
│   ├── model/                     // Domain model tests
│   │   ├── IslamicFinancingTest.java
│   │   ├── MoneyTest.java
│   │   └── ShariaComplianceResultTest.java
│   └── service/                   // Domain service tests
│       ├── MurabahaServiceTest.java
│       ├── MusharakahServiceTest.java
│       └── ShariaComplianceServiceTest.java
├── infrastructure/
│   ├── adapter/                   // Adapter tests
│   │   ├── JpaIslamicFinancingRepositoryTest.java
│   │   ├── KafkaEventPublishingAdapterTest.java
│   │   └── UAECryptocurrencyNetworkAdapterTest.java
│   └── integration/               // Integration tests
│       ├── IslamicFinanceIntegrationTest.java
│       ├── ShariaComplianceIntegrationTest.java
│       └── CryptocurrencyIntegrationTest.java
└── api/                          // API tests
    ├── IslamicFinanceRestAdapterTest.java
    └── ShariaComplianceRestAdapterTest.java
```

### **Architecture Testing**

```java
@AnalyzeClasses(packages = "com.masrufi.framework")
public class ArchitectureTest {
    
    @ArchTest
    static final ArchRule domain_should_not_depend_on_infrastructure = 
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..infrastructure..");
    
    @ArchTest
    static final ArchRule domain_should_not_depend_on_api = 
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..api..");
    
    @ArchTest
    static final ArchRule use_cases_should_be_interfaces = 
        classes()
            .that().resideInAPackage("..domain.port.in..")
            .and().haveSimpleNameEndingWith("UseCase")
            .should().beInterfaces();
    
    @ArchTest
    static final ArchRule repositories_should_be_interfaces = 
        classes()
            .that().resideInAPackage("..domain.port.out..")
            .and().haveSimpleNameEndingWith("Repository")
            .should().beInterfaces();
    
    @ArchTest
    static final ArchRule domain_services_should_be_annotated = 
        classes()
            .that().resideInAPackage("..domain.service..")
            .should().beAnnotatedWith(Service.class);
    
    @ArchTest
    static final ArchRule adapters_should_implement_ports = 
        classes()
            .that().resideInAPackage("..infrastructure.adapter..")
            .and().areNotInterfaces()
            .should().implement(JavaClass.Predicates.resideInAPackage("..domain.port.."));
}
```

### **Domain Model Testing**

```java
@ExtendWith(MockitoExtension.class)
class MurabahaServiceTest {
    
    @Mock
    private IslamicFinancingRepository repository;
    
    @Mock
    private ShariaComplianceValidationPort shariaValidation;
    
    @Mock
    private AssetValidationPort assetValidation;
    
    @Mock
    private DomainEventPublisher eventPublisher;
    
    @InjectMocks
    private MurabahaService murabahaService;
    
    @Test
    @DisplayName("Should create valid Murabaha contract with Sharia compliance")
    void shouldCreateValidMurabahaContract() {
        // Given
        CreateMurabahaCommand command = CreateMurabahaCommand.builder()
            .customerProfile(createValidCustomerProfile())
            .assetDescription("Toyota Camry 2024")
            .assetCost(Money.of("80000", "AED"))
            .profitMargin(new BigDecimal("0.15"))
            .maturityDate(LocalDateTime.now().plusYears(3))
            .build();
        
        when(assetValidation.validateAsset(any())).thenReturn(AssetValidationResult.valid());
        when(shariaValidation.validateMurabaha(any())).thenReturn(ShariaComplianceResult.compliant("HSA-001"));
        when(repository.save(any())).thenReturn(new IslamicFinancingId("MUR-001"));
        
        // When
        IslamicFinancing result = murabahaService.createMurabaha(command);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(IslamicFinancingType.MURABAHA);
        assertThat(result.getPrincipal()).isEqualTo(Money.of("80000", "AED"));
        assertThat(result.getTotalAmount()).isEqualTo(Money.of("92000", "AED")); // 15% profit
        assertThat(result.getComplianceResult().isCompliant()).isTrue();
        
        verify(eventPublisher).publish(any(MurabahaContractCreatedEvent.class));
    }
    
    @Test
    @DisplayName("Should reject Murabaha for non-Sharia compliant asset")
    void shouldRejectNonShariaCompliantAsset() {
        // Given
        CreateMurabahaCommand command = CreateMurabahaCommand.builder()
            .customerProfile(createValidCustomerProfile())
            .assetDescription("Alcohol Distribution License")
            .assetCost(Money.of("50000", "AED"))
            .profitMargin(new BigDecimal("0.10"))
            .build();
        
        when(assetValidation.validateAsset(any())).thenReturn(AssetValidationResult.invalid("Prohibited asset"));
        
        // When & Then
        assertThatThrownBy(() -> murabahaService.createMurabaha(command))
            .isInstanceOf(ShariaViolationException.class)
            .hasMessageContaining("Prohibited asset");
        
        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }
}
```

## 📊 Performance Architecture

### **Caching Strategy**

```java
@Configuration
@EnableCaching
public class MasrufiCacheConfiguration {
    
    @Bean
    public CacheManager cacheManager() {
        RedisCacheManager.Builder builder = RedisCacheManager
            .RedisCacheManagerBuilder
            .fromConnectionFactory(redisConnectionFactory())
            .cacheDefaults(cacheConfiguration());
            
        return builder.build();
    }
    
    private RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }
}

// Caching in domain services
@Service
@Transactional
public class ShariaComplianceService {
    
    @Cacheable(value = "sharia-compliance", key = "#asset.assetId")
    public ShariaComplianceResult validateAssetPermissibility(Asset asset) {
        // Expensive compliance validation logic
        return performDetailedShariaValidation(asset);
    }
    
    @CacheEvict(value = "sharia-compliance", allEntries = true)
    public void refreshComplianceRules() {
        // Refresh compliance rules from Higher Sharia Authority
    }
}
```

### **Database Optimization**

```sql
-- Islamic Financing table with optimized indexes
CREATE TABLE islamic_financing (
    financing_id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL,
    financing_type VARCHAR(20) NOT NULL,
    principal_amount DECIMAL(19,2) NOT NULL,
    principal_currency VARCHAR(3) NOT NULL,
    total_amount DECIMAL(19,2) NOT NULL,
    profit_margin DECIMAL(5,4),
    status VARCHAR(20) NOT NULL,
    sharia_compliant BOOLEAN NOT NULL DEFAULT false,
    compliance_reference VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    maturity_date TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1
);

-- Performance indexes
CREATE INDEX idx_islamic_financing_customer_id ON islamic_financing(customer_id);
CREATE INDEX idx_islamic_financing_type_status ON islamic_financing(financing_type, status);
CREATE INDEX idx_islamic_financing_created_at ON islamic_financing(created_at);
CREATE INDEX idx_islamic_financing_maturity_date ON islamic_financing(maturity_date) WHERE maturity_date IS NOT NULL;
CREATE INDEX idx_islamic_financing_sharia_compliant ON islamic_financing(sharia_compliant);

-- Partitioning by year for large datasets
CREATE TABLE islamic_financing_2024 PARTITION OF islamic_financing
FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');

CREATE TABLE islamic_financing_2025 PARTITION OF islamic_financing
FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');
```

## 🚀 Deployment Architecture

### **Docker Configuration**

```dockerfile
# Multi-stage Dockerfile for MasruFi Framework
FROM bellsoft/liberica-openjdk-alpine:21-cds AS builder

WORKDIR /app
COPY . .
RUN ./gradlew bootJar --no-daemon

FROM bellsoft/liberica-openjdk-alpine:21-cds AS production

# Security: Create non-root user
RUN addgroup -g 1001 masrufi && \
    adduser -D -s /bin/sh -u 1001 -G masrufi masrufi

# Islamic Finance framework configuration
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar masrufi-framework.jar

# Health check for Islamic Finance services
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health/islamic-finance || exit 1

# Security context
USER masrufi:masrufi

# JVM optimization for Islamic Finance workloads
ENV JVM_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication"

EXPOSE 8080

CMD ["java", "-jar", "masrufi-framework.jar"]
```

### **Kubernetes Deployment**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: masrufi-framework
  namespace: islamic-finance
  labels:
    app: masrufi-framework
    component: islamic-finance
    version: v1.0.0
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: masrufi-framework
  template:
    metadata:
      labels:
        app: masrufi-framework
        component: islamic-finance
        version: v1.0.0
    spec:
      serviceAccountName: masrufi-service-account
      securityContext:
        runAsNonRoot: true
        runAsUser: 1001
        runAsGroup: 1001
        fsGroup: 1001
      containers:
      - name: masrufi-framework
        image: masrufi/framework:1.0.0
        imagePullPolicy: IfNotPresent
        ports:
        - name: http
          containerPort: 8080
          protocol: TCP
        - name: actuator
          containerPort: 8081
          protocol: TCP
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production,islamic-finance,uae-crypto"
        - name: MASRUFI_FRAMEWORK_ENABLED
          value: "true"
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: masrufi-secrets
              key: database-url
        - name: REDIS_URL
          valueFrom:
            secretKeyRef:
              name: masrufi-secrets
              key: redis-url
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: actuator
          initialDelaySeconds: 60
          periodSeconds: 30
          timeoutSeconds: 10
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: actuator
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        volumeMounts:
        - name: config-volume
          mountPath: /app/config
          readOnly: true
      volumes:
      - name: config-volume
        configMap:
          name: masrufi-config
---
apiVersion: v1
kind: Service
metadata:
  name: masrufi-framework-service
  namespace: islamic-finance
  labels:
    app: masrufi-framework
spec:
  type: ClusterIP
  ports:
  - name: http
    port: 80
    targetPort: 8080
    protocol: TCP
  - name: actuator
    port: 8081
    targetPort: 8081
    protocol: TCP
  selector:
    app: masrufi-framework
```

## 📈 Monitoring & Observability Architecture

### **Metrics Configuration**

```java
@Configuration
public class MasrufiMetricsConfiguration {
    
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> masrufiMetricsCustomizer() {
        return registry -> registry.config()
            .commonTags("application", "masrufi-framework")
            .commonTags("component", "islamic-finance");
    }
    
    @Component
    public class IslamicFinanceMetrics {
        
        private final Counter murabahaCreatedCounter;
        private final Counter shariaComplianceValidationCounter;
        private final Timer cryptoPaymentProcessingTimer;
        private final Gauge activeIslamicFinancingGauge;
        
        public IslamicFinanceMetrics(MeterRegistry meterRegistry) {
            this.murabahaCreatedCounter = Counter.builder("masrufi.murabaha.created")
                .description("Number of Murabaha contracts created")
                .register(meterRegistry);
                
            this.shariaComplianceValidationCounter = Counter.builder("masrufi.sharia.validation")
                .description("Number of Sharia compliance validations")
                .tag("result", "compliant")
                .register(meterRegistry);
                
            this.cryptoPaymentProcessingTimer = Timer.builder("masrufi.crypto.payment.processing")
                .description("Time taken to process cryptocurrency payments")
                .register(meterRegistry);
                
            this.activeIslamicFinancingGauge = Gauge.builder("masrufi.financing.active")
                .description("Number of active Islamic financing contracts")
                .register(meterRegistry, this, IslamicFinanceMetrics::getActiveFinancingCount);
        }
        
        public void recordMurabahaCreated() {
            murabahaCreatedCounter.increment();
        }
        
        public void recordShariaValidation(boolean compliant) {
            shariaComplianceValidationCounter.increment(
                Tags.of("result", compliant ? "compliant" : "non-compliant")
            );
        }
        
        public Timer.Sample startCryptoPaymentTimer() {
            return Timer.start(cryptoPaymentProcessingTimer);
        }
        
        private double getActiveFinancingCount() {
            // Implementation to get active financing count
            return islamicFinancingRepository.countByStatus(FinancingStatus.ACTIVE);
        }
    }
}
```

---

## 🔧 Configuration Management

### **Application Properties**

```yaml
# application-masrufi.yml
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
      business-rules:
        enabled: true
        hot-reload-enabled: true
        max-profit-margin: 0.30
        min-asset-value: 10000
        
    uae-cryptocurrency:
      enabled: true
      supported-currencies:
        - UAE-CBDC
        - ADIB-DD
        - ENBD-DC
        - FAB-DT
      network:
        timeout: 30000
        retry-attempts: 3
        
    sharia-compliance:
      enabled: true
      strict-mode: true
      validation-timeout: 10000
      
    enterprise-integration:
      event-publishing:
        enabled: true
        topic-prefix: "masrufi.events"
      data-sync:
        enabled: true
        sync-interval-seconds: 300
        
    monitoring:
      enabled: true
      metrics:
        enabled: true
      health-checks:
        enabled: true
        
    security:
      oauth2:
        resource-server:
          jwt:
            issuer-uri: "${OAUTH2_ISSUER_URI:http://localhost:8080/realms/masrufi}"
      encryption:
        algorithm: "AES"
        key-size: 256
```

## 📋 Architecture Decision Records (ADRs)

### **ADR-001: Hexagonal Architecture Adoption**

**Status**: Accepted  
**Date**: 2024-12-01

**Context**: Need for high cohesion and loose coupling in Islamic Finance extension module.

**Decision**: Adopt Hexagonal Architecture (Ports and Adapters pattern) for MasruFi Framework.

**Consequences**:
- ✅ Clear separation between business logic and technical concerns
- ✅ Improved testability through dependency inversion
- ✅ Enhanced flexibility for integration with different enterprise systems
- ❌ Increased initial complexity for developers unfamiliar with the pattern

### **ADR-002: Event-Driven Integration**

**Status**: Accepted  
**Date**: 2024-12-01

**Context**: Need for non-invasive integration with existing enterprise loan management systems.

**Decision**: Use event-driven architecture for integration with host systems.

**Consequences**:
- ✅ Loose coupling between systems
- ✅ Asynchronous processing capabilities
- ✅ Better scalability and resilience
- ❌ Eventual consistency challenges
- ❌ Increased complexity in debugging distributed systems

### **ADR-003: Domain-Driven Design**

**Status**: Accepted  
**Date**: 2024-12-01

**Context**: Complex Islamic Finance domain requires clear modeling and boundaries.

**Decision**: Apply Domain-Driven Design principles with Islamic Finance as bounded context.

**Consequences**:
- ✅ Clear domain model representation
- ✅ Improved communication with domain experts
- ✅ Better code organization and maintainability
- ❌ Learning curve for developers new to DDD
- ❌ Potential over-engineering for simple scenarios

---

**Document Control:**
- **Prepared By**: MasruFi Framework Architecture Team
- **Reviewed By**: Senior Solution Architects
- **Approved By**: Chief Technology Officer
- **Next Review**: Quarterly architecture review

*🏗️ This architecture specification ensures that the MasruFi Framework delivers world-class Islamic Finance capabilities through proven software architecture patterns and enterprise-grade design principles.*