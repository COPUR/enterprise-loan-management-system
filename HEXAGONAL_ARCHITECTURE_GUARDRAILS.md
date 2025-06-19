# 🔶 Hexagonal Architecture Guardrails & Clean Code Standards

## Executive Summary

This document establishes **mandatory** architectural guardrails for the Enterprise Banking System. These standards ensure consistent hexagonal architecture implementation, DDD compliance, and clean code practices across all microservices.

**⚠️ CRITICAL**: All code MUST comply with these standards before being merged to main branch.

---

## 🏗️ **Hexagonal Architecture Principles**

### **Core Principles**
1. **Domain Independence**: Business logic is completely isolated from infrastructure
2. **Port & Adapter Pattern**: All external dependencies accessed through interfaces
3. **Dependency Inversion**: High-level modules don't depend on low-level modules
4. **Testability**: Business logic can be tested without infrastructure

### **Architecture Layers**

```
🔶 Hexagonal Architecture Structure
┌─────────────────────────────────────────────────────────────┐
│                    🌐 External World                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │     Web     │  │  Database   │  │  External   │        │
│  │   Adapter   │  │   Adapter   │  │   APIs      │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
│         │                 │                 │              │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │             🔌 Infrastructure Layer                     │ │
│  │  ┌─────────────────────────────────────────────────────┐│ │
│  │  │           📱 Application Layer                      ││ │
│  │  │  ┌─────────────────────────────────────────────────┐││ │
│  │  │  │              💎 Domain Layer                    │││ │
│  │  │  │  ┌─────────────┐  ┌─────────────┐             │││ │
│  │  │  │  │  Aggregates │  │   Value     │             │││ │
│  │  │  │  │ & Entities  │  │  Objects    │             │││ │
│  │  │  │  └─────────────┘  └─────────────┘             │││ │
│  │  │  │  ┌─────────────┐  ┌─────────────┐             │││ │
│  │  │  │  │   Domain    │  │   Domain    │             │││ │
│  │  │  │  │  Services   │  │   Events    │             │││ │
│  │  │  │  └─────────────┘  └─────────────┘             │││ │
│  │  │  └─────────────────────────────────────────────────┘││ │
│  │  └─────────────────────────────────────────────────────┘│ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

---

## 📋 **Mandatory Package Structure**

### **Standard Package Layout**
```
com.bank.loanmanagement.{bounded-context}/
├── 💎 domain/
│   ├── model/                 # Aggregates, Entities, Value Objects
│   │   ├── {Aggregate}.java   # Pure business logic
│   │   ├── {ValueObject}.java # Immutable value objects
│   │   └── {Entity}.java      # Domain entities
│   ├── event/                 # Domain Events
│   │   └── {Event}.java       # Domain event definitions
│   ├── service/               # Domain Services
│   │   └── {DomainService}.java
│   └── port/                  # Hexagonal Ports
│       ├── in/                # Input Ports (Use Cases)
│       │   ├── {UseCase}.java           # Use case interfaces
│       │   └── {Command|Query}.java     # Commands and queries
│       └── out/               # Output Ports
│           ├── {Repository}.java        # Repository interfaces
│           └── {ExternalService}.java   # External service interfaces
├── 📱 application/
│   ├── service/               # Application Services (Use Case Implementations)
│   │   └── {UseCaseImpl}.java # Use case implementations
│   ├── dto/                   # Data Transfer Objects
│   │   ├── {Request}.java     # Request DTOs
│   │   └── {Response}.java    # Response DTOs
│   └── mapper/                # Domain ↔ DTO Mapping
│       └── {Mapper}.java      # Mapping logic
└── 🔌 infrastructure/
    └── adapter/               # Infrastructure Adapters
        ├── in/                # Inbound Adapters
        │   └── web/           # Web Controllers
        │       ├── {Controller}.java    # REST controllers
        │       └── dto/                 # Web DTOs
        │           ├── {WebRequest}.java
        │           └── {WebResponse}.java
        └── out/               # Outbound Adapters
            ├── persistence/   # Database Adapters
            │   ├── {JpaRepository}.java     # JPA repositories
            │   ├── {JpaEntity}.java         # JPA entities
            │   └── {RepositoryAdapter}.java # Repository implementations
            └── external/      # External Service Adapters
                └── {ExternalServiceAdapter}.java
```

---

## 🚫 **Forbidden Practices**

### **❌ Domain Layer Violations**

```java
// ❌ WRONG: JPA annotations in domain
@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue
    private Long id;
}

// ❌ WRONG: Spring annotations in domain
@Service
public class CustomerService {
    @Autowired
    private CustomerRepository repository;
}

// ❌ WRONG: Primitive obsession
public class Loan {
    private BigDecimal amount;  // Should be Money value object
    private String customerId;  // Should be CustomerId value object
}

// ❌ WRONG: Infrastructure leakage
public class Customer {
    public void save() {
        // Database logic in domain
        entityManager.persist(this);
    }
}
```

### **❌ Application Layer Violations**

```java
// ❌ WRONG: Business logic in application service
@Service
public class LoanApplicationService {
    public void approveLoan(Long loanId) {
        Loan loan = loanRepository.findById(loanId);
        // Business rules should be in domain, not here
        if (loan.getAmount().compareTo(BigDecimal.valueOf(100000)) > 0) {
            loan.setStatus(LoanStatus.REQUIRES_MANAGER_APPROVAL);
        }
    }
}

// ❌ WRONG: Direct infrastructure dependency
@Service
public class CustomerService {
    @Autowired
    private CustomerJpaRepository jpaRepository; // Should depend on domain interface
}
```

---

## ✅ **Correct Implementation Patterns**

### **✅ Clean Domain Model**

```java
// ✅ CORRECT: Pure domain aggregate
public class Customer extends AggregateRoot<CustomerId> {
    private CustomerId customerId;
    private PersonalName name;
    private EmailAddress email;
    private CreditLimit creditLimit;
    private Money usedCredit;
    
    // ✅ Business logic in domain
    public void reserveCredit(Money amount) {
        if (!hasAvailableCredit(amount)) {
            throw new InsufficientCreditException(customerId, amount);
        }
        
        this.usedCredit = this.usedCredit.add(amount);
        addDomainEvent(new CreditReservedEvent(customerId, amount));
    }
    
    private boolean hasAvailableCredit(Money amount) {
        Money availableCredit = creditLimit.getAmount().subtract(usedCredit);
        return availableCredit.isGreaterThanOrEqualTo(amount);
    }
}

// ✅ CORRECT: Value object
public final class Money {
    private final BigDecimal amount;
    private final Currency currency;
    
    private Money(BigDecimal amount, Currency currency) {
        this.amount = Objects.requireNonNull(amount);
        this.currency = Objects.requireNonNull(currency);
        validate();
    }
    
    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }
    
    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add different currencies");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }
    
    public boolean isGreaterThanOrEqualTo(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot compare different currencies");
        }
        return this.amount.compareTo(other.amount) >= 0;
    }
}
```

### **✅ Clean Use Case Implementation**

```java
// ✅ CORRECT: Use case interface (Domain Port In)
public interface CustomerManagementUseCase {
    Customer createCustomer(CreateCustomerCommand command);
    void reserveCredit(ReserveCreditCommand command);
    Customer findCustomer(CustomerId customerId);
}

// ✅ CORRECT: Command object
public record CreateCustomerCommand(
    String firstName,
    String lastName,
    String email,
    String phoneNumber,
    BigDecimal initialCreditLimit
) {
    public CreateCustomerCommand {
        Objects.requireNonNull(firstName, "First name is required");
        Objects.requireNonNull(lastName, "Last name is required");
        Objects.requireNonNull(email, "Email is required");
        if (initialCreditLimit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Credit limit cannot be negative");
        }
    }
}

// ✅ CORRECT: Application service implementation
@UseCase
@Transactional
public class CustomerManagementService implements CustomerManagementUseCase {
    
    private final CustomerRepository customerRepository;
    private final DomainEventPublisher eventPublisher;
    
    public CustomerManagementService(
        CustomerRepository customerRepository,
        DomainEventPublisher eventPublisher
    ) {
        this.customerRepository = customerRepository;
        this.eventPublisher = eventPublisher;
    }
    
    @Override
    public Customer createCustomer(CreateCustomerCommand command) {
        // 1. Create domain objects
        CustomerId customerId = CustomerIdGenerator.generateNew();
        PersonalName name = PersonalName.of(command.firstName(), command.lastName());
        EmailAddress email = EmailAddress.of(command.email());
        CreditLimit creditLimit = CreditLimit.of(Money.usd(command.initialCreditLimit()));
        
        // 2. Create aggregate
        Customer customer = Customer.create(customerId, name, email, creditLimit);
        
        // 3. Persist through repository port
        Customer savedCustomer = customerRepository.save(customer);
        
        // 4. Publish domain events
        savedCustomer.getDomainEvents().forEach(eventPublisher::publish);
        savedCustomer.clearDomainEvents();
        
        return savedCustomer;
    }
}
```

### **✅ Clean Repository Pattern**

```java
// ✅ CORRECT: Domain repository interface (Port Out)
public interface CustomerRepository {
    Customer save(Customer customer);
    Optional<Customer> findById(CustomerId customerId);
    Optional<Customer> findByEmail(EmailAddress email);
    boolean existsByEmail(EmailAddress email);
}

// ✅ CORRECT: Infrastructure repository adapter
@Repository
public class CustomerRepositoryAdapter implements CustomerRepository {
    
    private final CustomerJpaRepository jpaRepository;
    private final CustomerMapper mapper;
    
    public CustomerRepositoryAdapter(
        CustomerJpaRepository jpaRepository,
        CustomerMapper mapper
    ) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    public Customer save(Customer customer) {
        CustomerJpaEntity entity = mapper.toEntity(customer);
        CustomerJpaEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }
    
    @Override
    public Optional<Customer> findById(CustomerId customerId) {
        return jpaRepository.findById(customerId.getValue())
            .map(mapper::toDomain);
    }
}

// ✅ CORRECT: JPA entity (separate from domain)
@Entity
@Table(name = "customers")
public class CustomerJpaEntity {
    
    @Id
    private String id;
    
    @Column(name = "first_name", nullable = false)
    private String firstName;
    
    @Column(name = "last_name", nullable = false)
    private String lastName;
    
    @Column(name = "email", unique = true, nullable = false)
    private String email;
    
    @Column(name = "credit_limit_amount", precision = 19, scale = 2)
    private BigDecimal creditLimitAmount;
    
    @Column(name = "credit_limit_currency", length = 3)
    private String creditLimitCurrency;
    
    // Constructors, getters, setters
}
```

---

## 🧪 **Testing Standards**

### **Domain Testing**
```java
// ✅ CORRECT: Pure domain unit test
@DisplayName("Customer Credit Management")
class CustomerTest {
    
    @Test
    @DisplayName("Should reserve credit when sufficient limit available")
    void shouldReserveCreditWhenSufficientLimitAvailable() {
        // Given
        CustomerId customerId = CustomerId.of("CUST-001");
        Money creditLimit = Money.usd(BigDecimal.valueOf(10000));
        Customer customer = Customer.create(
            customerId,
            PersonalName.of("John", "Doe"),
            EmailAddress.of("john.doe@example.com"),
            CreditLimit.of(creditLimit)
        );
        Money reservationAmount = Money.usd(BigDecimal.valueOf(5000));
        
        // When
        customer.reserveCredit(reservationAmount);
        
        // Then
        assertThat(customer.getUsedCredit()).isEqualTo(reservationAmount);
        assertThat(customer.getDomainEvents())
            .hasSize(1)
            .first()
            .isInstanceOf(CreditReservedEvent.class);
    }
    
    @Test
    @DisplayName("Should throw exception when insufficient credit available")
    void shouldThrowExceptionWhenInsufficientCreditAvailable() {
        // Given
        Customer customer = createCustomerWithCreditLimit(Money.usd(BigDecimal.valueOf(1000)));
        Money reservationAmount = Money.usd(BigDecimal.valueOf(2000));
        
        // When & Then
        assertThatThrownBy(() -> customer.reserveCredit(reservationAmount))
            .isInstanceOf(InsufficientCreditException.class)
            .hasMessage("Insufficient credit for customer CUST-001: requested 2000.00 USD, available 1000.00 USD");
    }
}
```

### **Integration Testing**
```java
// ✅ CORRECT: Repository adapter integration test
@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:testdb"
})
class CustomerRepositoryAdapterTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private CustomerJpaRepository jpaRepository;
    
    private CustomerRepositoryAdapter repository;
    private CustomerMapper mapper;
    
    @BeforeEach
    void setUp() {
        mapper = new CustomerMapper();
        repository = new CustomerRepositoryAdapter(jpaRepository, mapper);
    }
    
    @Test
    @DisplayName("Should save and retrieve customer correctly")
    void shouldSaveAndRetrieveCustomerCorrectly() {
        // Given
        Customer customer = createSampleCustomer();
        
        // When
        Customer savedCustomer = repository.save(customer);
        Optional<Customer> retrievedCustomer = repository.findById(customer.getId());
        
        // Then
        assertThat(retrievedCustomer)
            .isPresent()
            .hasValueSatisfying(c -> {
                assertThat(c.getId()).isEqualTo(customer.getId());
                assertThat(c.getName()).isEqualTo(customer.getName());
                assertThat(c.getEmail()).isEqualTo(customer.getEmail());
            });
    }
}
```

---

## 🔒 **Security & Compliance Guidelines**

### **Data Protection**
```java
// ✅ CORRECT: Sensitive data handling
@Entity
@Table(name = "customers")
public class CustomerJpaEntity {
    
    @Column(name = "ssn")
    @Convert(converter = EncryptedStringConverter.class)  // Encrypt PII
    private String socialSecurityNumber;
    
    @Column(name = "email")
    @Convert(converter = HashedStringConverter.class)     // Hash for searches
    private String emailHash;
    
    @Column(name = "email_encrypted")
    @Convert(converter = EncryptedStringConverter.class)  // Encrypted original
    private String emailEncrypted;
}

// ✅ CORRECT: Audit logging
@AuditLogging
public class CustomerManagementService implements CustomerManagementUseCase {
    
    @Override
    @AuditLog(action = "CREATE_CUSTOMER", sensitiveData = false)
    public Customer createCustomer(CreateCustomerCommand command) {
        // Implementation with automatic audit logging
    }
    
    @Override
    @AuditLog(action = "RESERVE_CREDIT", sensitiveData = true)
    public void reserveCredit(ReserveCreditCommand command) {
        // Sensitive operation with enhanced logging
    }
}
```

### **Input Validation**
```java
// ✅ CORRECT: Domain-driven validation
public record CreateCustomerCommand(
    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    String firstName,
    
    @NotBlank(message = "Last name is required") 
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    String lastName,
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,
    
    @NotNull(message = "Credit limit is required")
    @DecimalMin(value = "0.0", message = "Credit limit must be non-negative")
    @Digits(integer = 10, fraction = 2, message = "Credit limit must have at most 10 integer and 2 fractional digits")
    BigDecimal initialCreditLimit
) {
    public CreateCustomerCommand {
        // Additional domain-specific validation
        if (email != null && email.contains("+")) {
            throw new IllegalArgumentException("Email addresses with + symbols are not supported");
        }
    }
}
```

---

## 📊 **Performance Guidelines**

### **Database Optimization**
```java
// ✅ CORRECT: Optimized JPA entity
@Entity
@Table(
    name = "customers",
    indexes = {
        @Index(name = "idx_customer_email_hash", columnList = "email_hash"),
        @Index(name = "idx_customer_status", columnList = "status"),
        @Index(name = "idx_customer_created_at", columnList = "created_at")
    }
)
@NamedQueries({
    @NamedQuery(
        name = "Customer.findActiveCustomersWithCreditLimit",
        query = "SELECT c FROM CustomerJpaEntity c WHERE c.status = 'ACTIVE' AND c.creditLimitAmount > :minLimit"
    )
})
public class CustomerJpaEntity {
    
    @Id
    @Column(name = "id", length = 36)
    private String id;
    
    @Column(name = "email_hash", length = 64, unique = true)
    private String emailHash;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private CustomerStatus status;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
```

### **Caching Strategy**
```java
// ✅ CORRECT: Repository with caching
@Repository
public class CustomerRepositoryAdapter implements CustomerRepository {
    
    private final CustomerJpaRepository jpaRepository;
    private final CustomerCache cache;
    
    @Override
    @Cacheable(value = "customers", key = "#customerId.value")
    public Optional<Customer> findById(CustomerId customerId) {
        return jpaRepository.findById(customerId.getValue())
            .map(mapper::toDomain);
    }
    
    @Override
    @CacheEvict(value = "customers", key = "#customer.id.value")
    public Customer save(Customer customer) {
        CustomerJpaEntity entity = mapper.toEntity(customer);
        CustomerJpaEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }
}
```

---

## 🚀 **Deployment & Monitoring**

### **Health Checks**
```java
// ✅ CORRECT: Custom health indicator
@Component
public class CustomerServiceHealthIndicator implements HealthIndicator {
    
    private final CustomerRepository customerRepository;
    
    @Override
    public Health health() {
        try {
            // Test critical functionality
            customerRepository.healthCheck();
            
            return Health.up()
                .withDetail("service", "Customer Management")
                .withDetail("status", "All systems operational")
                .withDetail("checked_at", Instant.now())
                .build();
                
        } catch (Exception e) {
            return Health.down()
                .withDetail("service", "Customer Management")
                .withDetail("error", e.getMessage())
                .withDetail("checked_at", Instant.now())
                .build();
        }
    }
}
```

### **Metrics & Monitoring**
```java
// ✅ CORRECT: Business metrics
@UseCase
@Transactional
public class CustomerManagementService implements CustomerManagementUseCase {
    
    private final MeterRegistry meterRegistry;
    private final Counter customerCreationCounter;
    private final Timer creditReservationTimer;
    
    public CustomerManagementService(
        CustomerRepository customerRepository,
        MeterRegistry meterRegistry
    ) {
        this.customerRepository = customerRepository;
        this.meterRegistry = meterRegistry;
        this.customerCreationCounter = Counter.builder("customer.creation")
            .description("Number of customers created")
            .tag("service", "customer-management")
            .register(meterRegistry);
        this.creditReservationTimer = Timer.builder("credit.reservation.duration")
            .description("Time taken to reserve credit")
            .register(meterRegistry);
    }
    
    @Override
    public Customer createCustomer(CreateCustomerCommand command) {
        return Timer.Sample.start(meterRegistry)
            .stop(Timer.builder("customer.creation.duration")
                .description("Time taken to create customer")
                .register(meterRegistry))
            .recordCallable(() -> {
                Customer customer = doCreateCustomer(command);
                customerCreationCounter.increment();
                return customer;
            });
    }
}
```

---

## 📋 **Code Review Checklist**

### **✅ Architecture Compliance**
- [ ] Domain layer free of infrastructure dependencies
- [ ] Proper port/adapter pattern implementation  
- [ ] Use case interfaces in domain/port/in
- [ ] Repository interfaces in domain/port/out
- [ ] Value objects used instead of primitives
- [ ] Domain events for cross-aggregate communication

### **✅ Clean Code Standards**
- [ ] Meaningful names for classes, methods, variables
- [ ] Methods do one thing and are < 20 lines
- [ ] No code duplication (DRY principle)
- [ ] Proper error handling with domain exceptions
- [ ] No infrastructure leakage in domain layer

### **✅ Testing Coverage**
- [ ] Unit tests for all domain logic (>95% coverage)
- [ ] Integration tests for adapters
- [ ] Architecture tests with ArchUnit
- [ ] Contract tests for external interfaces
- [ ] Performance tests for critical paths

### **✅ Security & Compliance**
- [ ] No hardcoded secrets or credentials
- [ ] Proper input validation and sanitization
- [ ] Sensitive data encryption at rest
- [ ] Audit logging for business operations
- [ ] GDPR compliance for personal data

---

## 🎯 **Enforcement Mechanisms**

### **1. Automated Architecture Tests**
- ArchUnit tests run in CI/CD pipeline
- Fail build if architecture rules violated
- Custom rules for banking domain compliance

### **2. Static Code Analysis**
- SonarQube quality gates
- SpotBugs security analysis  
- Checkstyle code formatting
- OWASP dependency checks

### **3. Code Review Process**
- Mandatory architecture review
- Two approvals required for main branch
- Architecture committee review for major changes

### **4. IDE Integration**
- Architecture plugins for IntelliJ/Eclipse
- Real-time violation detection
- Code templates for proper patterns

---

## 📞 **Support & Training**

### **Resources**
- **Architecture Documentation**: `/docs/architecture/`
- **Code Examples**: `/examples/hexagonal-patterns/`
- **Training Materials**: Internal architecture wiki
- **Community**: #architecture-guild Slack channel

### **Contacts**
- **Architecture Questions**: @architecture-team
- **Code Review Help**: @senior-developers  
- **Training Requests**: @tech-leads
- **Tool Issues**: @platform-engineering

---

**⚠️ Remember**: These guardrails exist to ensure maintainable, testable, and scalable enterprise banking software. Compliance is not optional—it's essential for system integrity and regulatory compliance.