# Application Architecture Guide
## Enterprise Banking System - Microservices & Domain-Driven Design

### Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Domain-Driven Design](#domain-driven-design)
3. [Microservices Architecture](#microservices-architecture)
4. [Component Design](#component-design)
5. [Integration Patterns](#integration-patterns)
6. [Data Architecture](#data-architecture)
7. [Security Integration](#security-integration)
8. [Performance & Scalability](#performance--scalability)

---

## Architecture Overview

The Enterprise Banking System follows a microservices architecture built on Domain-Driven Design (DDD) principles. The system is designed for high availability, scalability, and maintainability while ensuring banking-grade security and compliance.

### Key Architectural Principles

1. **Domain-Driven Design (DDD)**
   - Bounded contexts for business domains
   - Aggregate roots for data consistency
   - Domain events for loose coupling
   - Ubiquitous language throughout

2. **Microservices Architecture**
   - Service autonomy and independence
   - Database per service pattern
   - API-first design approach
   - Event-driven communication

3. **12-Factor App Methodology**
   - Codebase versioning
   - Dependency management
   - Configuration externalization
   - Backing services abstraction

4. **Hexagonal Architecture**
   - Clean separation of concerns
   - Dependency inversion principle
   - Port and adapter pattern
   - Testability and maintainability

![Component Diagram](../generated-diagrams/Component%20Diagram_v1.0.0.svg)

---

## Domain-Driven Design

### Bounded Contexts

The system is organized into the following bounded contexts:

#### 1. Customer Management Context
**Purpose**: Manage customer information and credit assessment

**Core Domain Objects:**
- `Customer` (Aggregate Root)
- `CreditLimit` (Value Object)
- `CreditAssessment` (Domain Service)

**Responsibilities:**
- Customer profile management
- Credit limit evaluation
- Credit reservation and release
- Customer eligibility verification

#### 2. Loan Management Context - Hexagonal Architecture Complete
**Purpose**: Handle loan creation, management, and complete lifecycle with pure domain-driven design

**Core Domain Objects:**
- `Loan` (Aggregate Root) - **424 lines of pure domain logic**
- `LoanInstallment` (Entity) - **215 lines of business rules** 
- `LoanId` (Value Object) - Strong typing
- `LoanType` (Value Object) - Business categorization
- `LoanStatus` (Value Object) - State management
- `InstallmentStatus` (Value Object) - Payment tracking
- `Money` (Value Object) - Currency operations
- `CustomerId` (Value Object) - Customer reference

**Business Capabilities:**
- **Loan Application Processing** - Complete workflow with validation
- **Credit Assessment Integration** - Customer creditworthiness evaluation
- **Amortization Schedule Generation** - Mathematical precision
- **Payment Processing Engine** - Multi-installment allocation
- **Default Risk Management** - Automated monitoring and escalation
- **Loan Restructuring** - Term modification with audit trail
- **Regulatory Compliance** - Business rule enforcement
- **Event-Driven Communication** - Inter-context messaging

**Hexagonal Architecture Achievements:**
- **Zero Infrastructure Dependencies** - Pure domain models
- **Factory Method Pattern** - Controlled object creation
- **Domain Events System** - 8 comprehensive events
- **Business Logic Encapsulation** - Complete behavioral modeling
- **Port/Adapter Separation** - Clean persistence abstraction
- **Value Object Immutability** - Defensive programming
- **Aggregate Consistency** - Transaction boundary management

#### 3. Payment Processing Context
**Purpose**: Process loan payments and calculations

**Core Domain Objects:**
- `Payment` (Aggregate Root)
- `PaymentCalculation` (Domain Service)
- `PaymentStatus` (Value Object)

**Responsibilities:**
- Payment processing
- Installment allocation
- Discount and penalty calculation
- Payment validation

#### 4. Party Data Management Context
**Purpose**: Manage user roles and permissions

**Core Domain Objects:**
- `Party` (Aggregate Root)
- `PartyRole` (Entity)
- `PartyGroup` (Entity)
- `PartyRoleService` (Domain Service)

**Responsibilities:**
- Role assignment and management
- Temporal access control
- Authority level enforcement
- Compliance tracking

![Domain Model](../generated-diagrams/Domain%20Model_v1.0.0.svg)

### Domain Events - Comprehensive Event-Driven Architecture

The system implements a robust event-driven architecture with comprehensive domain events for complete business process tracking and inter-bounded context communication:

#### Loan Management Event System - 8 Complete Events

```java
// Loan Application Lifecycle Events
public class LoanApplicationSubmittedEvent extends DomainEvent {
    private final String loanId;
    private final String customerId;
    private final Money requestedAmount;
    private final LoanType loanType;
    private final String purpose;
    private final LocalDateTime timestamp;
}

public class LoanApprovedEvent extends DomainEvent {
    private final String loanId;
    private final String customerId;
    private final Money approvedAmount;
    private final String approvedBy;
    private final LocalDateTime timestamp;
}

public class LoanRejectedEvent extends DomainEvent {
    private final String loanId;
    private final String customerId;
    private final String rejectionReason;
    private final String rejectedBy;
    private final LocalDateTime timestamp;
}

public class LoanDisbursedEvent extends DomainEvent {
    private final String loanId;
    private final String customerId;
    private final Money disbursedAmount;
    private final LocalDate disbursementDate;
    private final LocalDateTime timestamp;
}

// Loan Payment Processing Events
public class LoanPaymentMadeEvent extends DomainEvent {
    private final String loanId;
    private final String customerId;
    private final Money paymentAmount;
    private final LocalDate paymentDate;
    private final LocalDateTime timestamp;
}

public class LoanPaidOffEvent extends DomainEvent {
    private final String loanId;
    private final String customerId;
    private final LocalDateTime timestamp;
}

// Loan Risk Management Events
public class LoanDefaultedEvent extends DomainEvent {
    private final String loanId;
    private final String customerId;
    private final Money outstandingAmount;
    private final String defaultReason;
    private final LocalDateTime timestamp;
}

public class LoanRestructuredEvent extends DomainEvent {
    private final String loanId;
    private final String customerId;
    private final BigDecimal newInterestRate;
    private final Integer newTermInMonths;
    private final String restructureReason;
    private final LocalDateTime timestamp;
}
```

#### **Customer Management Events**
```java
public class CreditReserved extends DomainEvent {
    private final CustomerId customerId;
    private final Money reservedAmount;
    private final Money remainingCredit;
    private final LocalDateTime timestamp;
}

public class CreditLimitUpdated extends DomainEvent {
    private final CustomerId customerId;
    private final Money oldLimit;
    private final Money newLimit;
    private final String reason;
    private final LocalDateTime timestamp;
}
```

#### **Payment Processing Events**
```java
public class PaymentProcessed extends DomainEvent {
    private final PaymentId paymentId;
    private final LoanId loanId;
    private final Money paymentAmount;
    private final Integer installmentsPaid;
    private final LocalDateTime timestamp;
}
```

#### **Party Management Events**
```java
public class PartyRoleAssigned extends DomainEvent {
    private final Long partyId;
    private final String roleName;
    private final Integer authorityLevel;
    private final LocalDateTime effectiveFrom;
    private final LocalDateTime timestamp;
}
```

**Event Processing Benefits:**
- **Loose Coupling**: Bounded contexts communicate through events
- **Audit Trail**: Complete business process tracking
- **Integration**: External system notifications
- **Analytics**: Business intelligence data source
- **Compliance**: Regulatory reporting requirements

### Aggregates Design

#### Customer Aggregate
```java
@Entity
public class Customer extends AggregateRoot {
    private CustomerId customerId;
    private String name;
    private String surname;
    private CreditLimit creditLimit;
    private Money usedCreditLimit;
    
    public boolean reserveCredit(Money amount) {
        if (!hasSufficientCredit(amount)) {
            return false;
        }
        
        this.usedCreditLimit = this.usedCreditLimit.add(amount);
        this.raiseEvent(new CreditReserved(
            this.customerId, 
            amount, 
            getAvailableCredit()
        ));
        
        return true;
    }
    
    public Money getAvailableCredit() {
        return creditLimit.getLimit().subtract(usedCreditLimit);
    }
}
```

#### Loan Aggregate - Clean Hexagonal Architecture Implementation

**Achievement Summary:**
- **Pure Domain Model**: 424 lines of clean business logic
- **Zero Infrastructure Dependencies**: No JPA contamination
- **Comprehensive Events**: 8 domain events for complete lifecycle
- **Factory Method Pattern**: Controlled domain object creation
- **Business Rule Enforcement**: Complete validation and invariants

```java
/**
 * Loan Domain Aggregate Root - Clean DDD Implementation
 * Pure domain model without infrastructure dependencies.
 * Source: com/bank/loanmanagement/domain/loan/Loan.java
 */
public class Loan extends AggregateRoot<LoanId> {
    
    private LoanId id;
    private CustomerId customerId;
    private Money principalAmount;
    private Money outstandingBalance;
    private BigDecimal interestRate;
    private Integer termInMonths;
    private LoanType loanType;
    private LoanStatus status;
    private String purpose;
    private LocalDate applicationDate;
    private LocalDate approvalDate;
    private LocalDate disbursementDate;
    private String approvedBy;
    private String rejectionReason;
    private List<LoanInstallment> installments;
    
    /**
     * Factory method for creating new loan applications
     * Enforces business rules and emits domain events
     */
    public static Loan create(
        LoanId id,
        CustomerId customerId,
        Money principalAmount,
        BigDecimal interestRate,
        Integer termInMonths,
        LoanType loanType,
        String purpose
    ) {
        // Business rule validation
        validateLoanCreationRules(principalAmount, interestRate, termInMonths);
        
        Loan loan = new Loan(id, customerId, principalAmount, 
                           interestRate, termInMonths, loanType, purpose);
        
        // Emit domain event
        loan.addDomainEvent(new LoanApplicationSubmittedEvent(
            id.getValue(),
            customerId.getValue(),
            principalAmount,
            loanType,
            purpose,
            LocalDateTime.now()
        ));
        
        return loan;
    }

    /**
     * Business operation: Approve loan application
     * Generates amortization schedule and transitions state
     */
    public void approve(String approvedBy) {
        if (this.status != LoanStatus.PENDING) {
            throw new LoanBusinessException("Only pending loans can be approved");
        }
        
        this.status = LoanStatus.APPROVED;
        this.approvalDate = LocalDate.now();
        this.approvedBy = approvedBy;
        
        // Generate installment schedule
        generateAmortizationSchedule();
        
        addDomainEvent(new LoanApprovedEvent(
            this.id.getValue(), 
            this.customerId.getValue(),
            this.principalAmount,
            this.approvedBy,
            LocalDateTime.now()
        ));
    }

    /**
     * Business operation: Process loan payment
     * Handles payment allocation and loan completion
     */
    public void makePayment(Money paymentAmount, LocalDate paymentDate) {
        validateActiveStatus();
        validatePaymentAmount(paymentAmount);
        
        allocatePaymentToInstallments(paymentAmount, paymentDate);
        updateOutstandingBalance();
        
        if (this.outstandingBalance.isZero()) {
            this.status = LoanStatus.PAID_OFF;
            addDomainEvent(new LoanPaidOffEvent(
                this.id.getValue(), 
                this.customerId.getValue(),
                LocalDateTime.now()
            ));
        }
        
        addDomainEvent(new LoanPaymentMadeEvent(
            this.id.getValue(), 
            this.customerId.getValue(), 
            paymentAmount, 
            paymentDate,
            LocalDateTime.now()
        ));
    }
    
    /**
     * Business operation: Mark loan as defaulted
     * Handles default processing and compliance requirements
     */
    public void markAsDefaulted(String defaultReason) {
        if (this.status == LoanStatus.PAID_OFF || this.status == LoanStatus.CANCELLED) {
            throw new LoanBusinessException("Cannot default a completed loan");
        }
        
        this.status = LoanStatus.DEFAULTED;
        
        addDomainEvent(new LoanDefaultedEvent(
            this.id.getValue(),
            this.customerId.getValue(),
            this.outstandingBalance,
            defaultReason,
            LocalDateTime.now()
        ));
    }
    
    /**
     * Business operation: Restructure loan terms
     * Modifies interest rate and term with proper validation
     */
    public void restructure(BigDecimal newInterestRate, Integer newTermInMonths, String reason) {
        validateActiveOrDefaultedStatus();
        validateRestructureParameters(newInterestRate, newTermInMonths);
        
        this.interestRate = newInterestRate;
        this.termInMonths = newTermInMonths;
        
        // Regenerate installment schedule
        regenerateAmortizationSchedule();
        
        addDomainEvent(new LoanRestructuredEvent(
            this.id.getValue(),
            this.customerId.getValue(),
            newInterestRate,
            newTermInMonths,
            reason,
            LocalDateTime.now()
        ));
    }
    
    // Additional business methods: calculateMonthlyPayment(), getTotalInterest(), 
    // getNextPaymentDue(), isOverdue(), getDaysOverdue(), etc.
}
```

**Domain Events Implemented:**
- `LoanApplicationSubmittedEvent` - New loan application
- `LoanApprovedEvent` - Loan approval with schedule generation
- `LoanRejectedEvent` - Application rejection
- `LoanDisbursedEvent` - Funds disbursement
- `LoanPaymentMadeEvent` - Payment processing
- `LoanPaidOffEvent` - Loan completion
- `LoanDefaultedEvent` - Default management
- `LoanRestructuredEvent` - Term modifications

**LoanInstallment Entity:**
```java
/**
 * LoanInstallment Entity - Pure Domain Implementation
 * Source: com/bank/loanmanagement/domain/loan/LoanInstallment.java
 */
public class LoanInstallment {
    
    private LoanId loanId;
    private Integer installmentNumber;
    private LocalDate dueDate;
    private Money principalAmount;
    private Money interestAmount;
    private Money totalAmount;
    private Money paidAmount;
    private LocalDate paymentDate;
    private InstallmentStatus status;
    
    /**
     * Factory method for installment creation
     */
    public static LoanInstallment create(
        LoanId loanId,
        Integer installmentNumber,
        LocalDate dueDate,
        Money principalAmount,
        Money interestAmount,
        Money totalAmount
    ) {
        validateInstallmentCreation(installmentNumber, dueDate, 
                                  principalAmount, interestAmount, totalAmount);
        
        return new LoanInstallment(loanId, installmentNumber, dueDate, 
                                 principalAmount, interestAmount, totalAmount);
    }
    
    /**
     * Process payment for this installment
     */
    public void processPayment(Money paymentAmount, LocalDate paymentDate) {
        validatePaymentProcessing(paymentAmount);
        
        this.paidAmount = this.paidAmount.add(paymentAmount);
        this.paymentDate = paymentDate;
        
        if (this.paidAmount.isGreaterThanOrEqual(this.totalAmount)) {
            this.status = InstallmentStatus.PAID;
        } else {
            this.status = InstallmentStatus.PARTIALLY_PAID;
        }
    }
}
```

#### Party Aggregate
```java
@Entity
public class Party extends AggregateRoot {
    private Long id;
    private String externalId;
    private String identifier;
    private PartyType partyType;
    private PartyStatus status;
    private Set<PartyRole> partyRoles;
    private Set<PartyGroup> partyGroups;
    
    public void addRole(PartyRole role) {
        partyRoles.add(role);
        role.setParty(this);
        
        this.raiseEvent(new PartyRoleAssigned(
            this.id,
            role.getRoleName(),
            role.getAuthorityLevel(),
            role.getEffectiveFrom()
        ));
    }
    
    public boolean hasRole(String roleName) {
        return partyRoles.stream()
            .anyMatch(role -> role.getRoleName().equals(roleName) 
                && role.isCurrentlyActive());
    }
    
    public Money getMaximumAuthorityLimit() {
        return partyRoles.stream()
            .filter(PartyRole::isCurrentlyActive)
            .map(role -> Money.of(role.getMonetaryLimit()))
            .max(Money::compareTo)
            .orElse(Money.ZERO);
    }
}
```

---

## Microservices Architecture

### Service Decomposition Strategy

The system is decomposed into microservices based on business capabilities and bounded contexts:

#### Core Banking Services

1. **Customer Management Service**
   - **Responsibility**: Customer profile and credit management
   - **Database**: Customer schema
   - **API Endpoints**: `/api/customers/*`
   - **Events Published**: CreditReserved, CreditReleased

2. **Loan Origination Service**
   - **Responsibility**: Loan processing and management
   - **Database**: Loan schema
   - **API Endpoints**: `/api/loans/*`
   - **Events Published**: LoanCreated, LoanUpdated

3. **Payment Processing Service**
   - **Responsibility**: Payment processing and calculations
   - **Database**: Payment schema
   - **API Endpoints**: `/api/payments/*`
   - **Events Published**: PaymentProcessed, LoanFullyPaid

4. **Party Data Management Service**
   - **Responsibility**: Role and permission management
   - **Database**: Party schema
   - **API Endpoints**: `/api/parties/*`
   - **Events Published**: PartyRoleAssigned, PartyRoleRevoked

#### Infrastructure Services

5. **API Gateway Service**
   - OAuth2.1 token validation
   - Rate limiting and throttling
   - Request routing and load balancing
   - Circuit breaker implementation

6. **Configuration Service**
   - Centralized configuration management
   - Environment-specific settings
   - Feature flag management
   - Dynamic configuration updates

7. **Monitoring Service**
   - Health check aggregation
   - Metrics collection
   - Distributed tracing
   - Performance monitoring

### Service Communication Patterns

#### Synchronous Communication
```java
// REST API with OAuth2.1 security
@RestController
@RequestMapping("/api/loans")
@PreAuthorize("hasRole('LOAN_OFFICER')")
public class LoanController {
    
    @PostMapping
    @PreAuthorize("@partyRoleService.hasMonetaryAuthority(authentication.name, #request.amount)")
    public ResponseEntity<LoanResponse> createLoan(@Valid @RequestBody CreateLoanRequest request) {
        
        // Call Customer Service for credit check
        CustomerCreditResponse creditResponse = customerServiceClient.checkCredit(
            request.getCustomerId(), 
            request.getAmount()
        );
        
        if (!creditResponse.isEligible()) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("Insufficient credit limit"));
        }
        
        Loan loan = loanApplicationService.createLoan(request);
        return ResponseEntity.ok(LoanResponse.from(loan));
    }
}
```

#### Asynchronous Communication
```java
// Event-driven communication with Kafka
@Component
public class PaymentEventHandler {
    
    @EventListener
    @Async
    public void handle(PaymentProcessed event) {
        
        // Update loan status
        loanService.updatePaymentStatus(
            event.getLoanId(), 
            event.getPaymentAmount(),
            event.getInstallmentsPaid()
        );
        
        // Release credit if loan is fully paid
        if (event.isLoanFullyPaid()) {
            customerService.releaseCredit(
                event.getCustomerId(),
                event.getTotalLoanAmount()
            );
        }
        
        // Publish notification event
        eventPublisher.publish(new LoanPaymentCompleted(event));
    }
}
```

### Service Discovery and Load Balancing

#### Kubernetes Service Discovery
```yaml
apiVersion: v1
kind: Service
metadata:
  name: loan-service
  namespace: banking-system
spec:
  selector:
    app: loan-service
  ports:
    - port: 8080
      targetPort: 8080
  type: ClusterIP
```

#### Circuit Breaker Pattern
```java
@Component
public class CustomerServiceClient {
    
    @CircuitBreaker(name = "customer-service", fallbackMethod = "getCustomerFallback")
    @Retry(name = "customer-service")
    @TimeLimiter(name = "customer-service")
    public CompletableFuture<Customer> getCustomer(Long customerId) {
        return CompletableFuture.supplyAsync(() -> {
            return restTemplate.getForObject(
                "/api/customers/{id}", 
                Customer.class, 
                customerId
            );
        });
    }
    
    public CompletableFuture<Customer> getCustomerFallback(Long customerId, Exception ex) {
        return CompletableFuture.completedFuture(
            Customer.builder()
                .id(customerId)
                .name("Unknown Customer")
                .status(CustomerStatus.TEMPORARILY_UNAVAILABLE)
                .build()
        );
    }
}
```

---

## Component Design

### Layered Architecture

Each microservice follows a clean layered architecture:

#### 1. Web Layer (Controllers)
**Responsibilities:**
- HTTP request/response handling
- Input validation and sanitization
- OAuth2.1 security enforcement
- API documentation with OpenAPI

```java
@RestController
@RequestMapping("/api/customers")
@SecurityRequirement(name = "oauth2")
@Tag(name = "Customer Management", description = "Customer profile and credit operations")
public class CustomerController {
    
    private final CustomerApplicationService customerService;
    
    @PostMapping
    @Operation(summary = "Create new customer", description = "Creates a new customer profile with credit assessment")
    @PreAuthorize("hasRole('CUSTOMER_MANAGER')")
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request,
            Authentication authentication) {
        
        String createdBy = authentication.getName();
        CustomerResponse response = customerService.createCustomer(request, createdBy);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .location(URI.create("/api/customers/" + response.getId()))
            .body(response);
    }
}
```

#### 2. Application Layer (Services)
**Responsibilities:**
- Use case orchestration
- Transaction management
- Event publishing
- Cross-cutting concerns

```java
@Service
@Transactional
@Validated
public class LoanApplicationService {
    
    private final LoanRepository loanRepository;
    private final CustomerRepository customerRepository;
    private final DomainEventPublisher eventPublisher;
    private final PartyRoleService partyRoleService;
    
    public LoanResponse createLoan(CreateLoanRequest request, String createdBy) {
        
        // Validate user authority
        if (!partyRoleService.hasMonetaryAuthority(createdBy, request.getAmount())) {
            throw new InsufficientAuthorityException("User lacks authority for amount: " + request.getAmount());
        }
        
        // Load customer aggregate
        Customer customer = customerRepository.findById(request.getCustomerId())
            .orElseThrow(() -> new CustomerNotFoundException(request.getCustomerId()));
        
        // Reserve credit
        if (!customer.reserveCredit(request.getAmount())) {
            throw new InsufficientCreditException("Customer has insufficient credit");
        }
        
        // Create loan aggregate using clean domain factory
        Loan loan = Loan.create(
            LoanId.generate(),
            customer.getId(),
            request.getAmount(),
            request.getInterestRate(),
            request.getNumberOfInstallments(),
            request.getLoanType(),
            request.getPurpose()
        );
        
        // Persist aggregates
        customerRepository.save(customer);
        loanRepository.save(loan);
        
        // Publish domain events
        eventPublisher.publishAll(customer.getDomainEvents());
        eventPublisher.publishAll(loan.getDomainEvents());
        
        return LoanResponse.from(loan);
    }
}
```

#### 3. Domain Layer (Business Logic)
**Responsibilities:**
- Business rules enforcement
- Domain model implementation
- Domain service logic
- Event generation

```java
@Component
public class PaymentCalculationService {
    
    public PaymentCalculationResult calculatePayment(
            List<LoanInstallment> installments, 
            Money paymentAmount, 
            LocalDate paymentDate) {
        
        Money remainingAmount = paymentAmount;
        List<InstallmentPayment> payments = new ArrayList<>();
        Money totalDiscount = Money.ZERO;
        Money totalPenalty = Money.ZERO;
        
        // Sort by due date (FIFO payment allocation)
        List<LoanInstallment> sortedInstallments = installments.stream()
            .filter(installment -> !installment.isPaid())
            .sorted(Comparator.comparing(LoanInstallment::getDueDate))
            .collect(Collectors.toList());
        
        for (LoanInstallment installment : sortedInstallments) {
            if (remainingAmount.isZero()) break;
            
            // Calculate discounts and penalties
            Money discount = installment.calculateEarlyPaymentDiscount(paymentDate);
            Money penalty = installment.calculateLatePaymentPenalty(paymentDate);
            Money effectiveAmount = installment.getAmount().subtract(discount).add(penalty);
            
            if (remainingAmount.isGreaterThanOrEqual(effectiveAmount)) {
                payments.add(new InstallmentPayment(installment, effectiveAmount));
                remainingAmount = remainingAmount.subtract(effectiveAmount);
                totalDiscount = totalDiscount.add(discount);
                totalPenalty = totalPenalty.add(penalty);
            }
        }
        
        return PaymentCalculationResult.builder()
            .installmentPayments(payments)
            .totalAmountUsed(paymentAmount.subtract(remainingAmount))
            .totalDiscount(totalDiscount)
            .totalPenalty(totalPenalty)
            .remainingAmount(remainingAmount)
            .build();
    }
}
```

#### 4. Infrastructure Layer (Technical Implementation)
**Responsibilities:**
- Database persistence
- External API integration
- Message publishing
- Configuration management

```java
@Repository
@Transactional
public class JpaLoanRepository implements LoanRepository {
    
    private final JpaLoanEntityRepository jpaRepository;
    private final LoanMapper loanMapper;
    
    @Override
    public Optional<Loan> findById(LoanId loanId) {
        return jpaRepository.findById(loanId.getValue())
            .map(loanMapper::toDomain);
    }
    
    @Override
    public Loan save(Loan loan) {
        LoanEntity entity = loanMapper.toEntity(loan);
        LoanEntity savedEntity = jpaRepository.save(entity);
        return loanMapper.toDomain(savedEntity);
    }
    
    @Override
    public List<Loan> findByCustomerId(CustomerId customerId) {
        return jpaRepository.findByCustomerId(customerId.getValue())
            .stream()
            .map(loanMapper::toDomain)
            .collect(Collectors.toList());
    }
}
```

### Security Integration

#### OAuth2.1 Resource Server Configuration
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class OAuth2ResourceServerConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(authenticationConverter())
                )
            )
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/actuator/health").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("BANKING_ADMIN")
                .requestMatchers("/api/loans/**").hasAnyRole("LOAN_OFFICER", "LOAN_MANAGER")
                .requestMatchers("/api/payments/**").hasAnyRole("LOAN_OFFICER", "PAYMENT_PROCESSOR")
                .requestMatchers("/api/parties/**").hasRole("USER_MANAGER")
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable())
            .build();
    }
    
    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation(keycloakIssuerUri);
        jwtDecoder.setJwtValidator(jwtValidator());
        return jwtDecoder;
    }
    
    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> authenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Extract roles from Keycloak token
            Collection<String> realmRoles = jwt.getClaimAsStringList("realm_access.roles");
            Collection<String> bankingRoles = jwt.getClaimAsStringList("banking_roles");
            
            Set<SimpleGrantedAuthority> authorities = new HashSet<>();
            
            if (realmRoles != null) {
                realmRoles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .forEach(authorities::add);
            }
            
            if (bankingRoles != null) {
                bankingRoles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .forEach(authorities::add);
            }
            
            return authorities;
        });
        return converter;
    }
}
```

#### Method-Level Security with Party Roles
```java
@Component("partyRoleService")
public class PartyRoleAuthorizationService {
    
    private final PartyRepository partyRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    
    public boolean hasMonetaryAuthority(String username, BigDecimal amount) {
        // Check cache first
        String cacheKey = "party:authority:" + username;
        BigDecimal cachedLimit = (BigDecimal) redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedLimit != null) {
            return amount.compareTo(cachedLimit) <= 0;
        }
        
        // Query database
        Optional<Party> party = partyRepository.findByIdentifier(username);
        if (party.isEmpty()) {
            return false;
        }
        
        BigDecimal maxLimit = party.get().getMaximumAuthorityLimit().getAmount();
        
        // Cache for 1 hour
        redisTemplate.opsForValue().set(cacheKey, maxLimit, Duration.ofHours(1));
        
        return amount.compareTo(maxLimit) <= 0;
    }
    
    public boolean hasRole(String username, String roleName) {
        Optional<Party> party = partyRepository.findByIdentifier(username);
        return party.map(p -> p.hasRole(roleName)).orElse(false);
    }
    
    public boolean isInBusinessUnit(String username, String businessUnit) {
        Optional<Party> party = partyRepository.findByIdentifier(username);
        return party.map(p -> p.getPartyRoles().stream()
            .anyMatch(role -> businessUnit.equals(role.getBusinessUnit()) && role.isCurrentlyActive()))
            .orElse(false);
    }
}
```

---

## Integration Patterns

### Event-Driven Architecture

#### Domain Event Publishing
```java
@Component
public class DomainEventPublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    public void publish(DomainEvent event) {
        try {
            String topic = determineTopicName(event);
            String eventData = objectMapper.writeValueAsString(event);
            
            kafkaTemplate.send(topic, event.getAggregateId(), eventData)
                .addCallback(
                    result -> log.info("Event published successfully: {}", event.getEventType()),
                    failure -> log.error("Failed to publish event: {}", event.getEventType(), failure)
                );
                
        } catch (Exception e) {
            log.error("Error publishing domain event", e);
            throw new EventPublishingException("Failed to publish event: " + event.getEventType(), e);
        }
    }
    
    private String determineTopicName(DomainEvent event) {
        String eventType = event.getEventType();
        
        if (eventType.startsWith("Customer")) {
            return "customer-events";
        } else if (eventType.startsWith("Loan")) {
            return "loan-events";
        } else if (eventType.startsWith("Payment")) {
            return "payment-events";
        } else if (eventType.startsWith("Party")) {
            return "party-events";
        } else {
            return "banking-events";
        }
    }
}
```

#### Event Consumption with Saga Pattern
```java
@Component
public class LoanCreationSaga {
    
    private final SagaStateRepository sagaStateRepository;
    private final CustomerServiceClient customerServiceClient;
    private final LoanServiceClient loanServiceClient;
    
    @KafkaListener(topics = "loan-events", groupId = "loan-creation-saga")
    public void handleLoanApplicationSubmitted(LoanApplicationSubmitted event) {
        
        SagaState sagaState = SagaState.builder()
            .sagaId(UUID.randomUUID().toString())
            .correlationId(event.getApplicationId())
            .status(SagaStatus.STARTED)
            .currentStep("VALIDATE_CUSTOMER")
            .build();
        
        sagaStateRepository.save(sagaState);
        
        try {
            // Step 1: Validate customer
            CustomerValidationRequest request = CustomerValidationRequest.builder()
                .customerId(event.getCustomerId())
                .loanAmount(event.getLoanAmount())
                .build();
            
            CustomerValidationResponse response = customerServiceClient.validateCustomer(request);
            
            if (response.isValid()) {
                sagaState.setCurrentStep("RESERVE_CREDIT");
                reserveCredit(sagaState, event);
            } else {
                compensate(sagaState, "Customer validation failed");
            }
            
        } catch (Exception e) {
            compensate(sagaState, "Error in customer validation: " + e.getMessage());
        }
    }
    
    private void reserveCredit(SagaState sagaState, LoanApplicationSubmitted event) {
        try {
            CreditReservationRequest request = CreditReservationRequest.builder()
                .customerId(event.getCustomerId())
                .amount(event.getLoanAmount())
                .reservationId(sagaState.getSagaId())
                .build();
            
            CreditReservationResponse response = customerServiceClient.reserveCredit(request);
            
            if (response.isSuccessful()) {
                sagaState.setCurrentStep("CREATE_LOAN");
                sagaState.addCompensationAction("RELEASE_CREDIT", request);
                createLoan(sagaState, event);
            } else {
                compensate(sagaState, "Credit reservation failed");
            }
            
        } catch (Exception e) {
            compensate(sagaState, "Error in credit reservation: " + e.getMessage());
        }
    }
    
    private void compensate(SagaState sagaState, String reason) {
        sagaState.setStatus(SagaStatus.COMPENSATING);
        sagaState.setFailureReason(reason);
        
        // Execute compensation actions in reverse order
        List<CompensationAction> actions = sagaState.getCompensationActions();
        Collections.reverse(actions);
        
        for (CompensationAction action : actions) {
            try {
                executeCompensationAction(action);
            } catch (Exception e) {
                log.error("Compensation action failed", e);
            }
        }
        
        sagaState.setStatus(SagaStatus.COMPENSATED);
        sagaStateRepository.save(sagaState);
    }
}
```

### API Gateway Integration

#### Rate Limiting and Circuit Breaker
```java
@Component
public class RateLimitingFilter implements GlobalFilter {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        
        String clientId = extractClientId(exchange);
        String endpoint = exchange.getRequest().getPath().value();
        
        return checkRateLimit(clientId, endpoint)
            .flatMap(allowed -> {
                if (!allowed) {
                    return handleRateLimitExceeded(exchange);
                }
                
                return applyCircuitBreaker(exchange, chain, endpoint);
            });
    }
    
    private Mono<Boolean> checkRateLimit(String clientId, String endpoint) {
        return Mono.fromCallable(() -> {
            String key = "rate_limit:" + clientId + ":" + endpoint;
            String currentCount = redisTemplate.opsForValue().get(key);
            
            if (currentCount == null) {
                redisTemplate.opsForValue().set(key, "1", Duration.ofMinutes(1));
                return true;
            }
            
            int count = Integer.parseInt(currentCount);
            int limit = getRateLimitForEndpoint(endpoint);
            
            if (count >= limit) {
                return false;
            }
            
            redisTemplate.opsForValue().increment(key);
            return true;
        });
    }
    
    private Mono<Void> applyCircuitBreaker(ServerWebExchange exchange, GatewayFilterChain chain, String endpoint) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(endpoint);
        
        return circuitBreaker.executeSupplier(() -> chain.filter(exchange))
            .cast(Void.class)
            .onErrorResume(CallNotPermittedException.class, 
                ex -> handleCircuitBreakerOpen(exchange));
    }
}
```

---

## Data Architecture

### Database Per Service Pattern

Each microservice maintains its own database schema to ensure data independence and service autonomy:

#### Customer Service Schema
```sql
-- Customer Management Tables
CREATE SCHEMA customer_management;

CREATE TABLE customer_management.customers (
    id BIGSERIAL PRIMARY KEY,
    customer_id VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    surname VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20),
    credit_limit_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    used_credit_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE customer_management.credit_reservations (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customer_management.customers(id),
    reservation_id VARCHAR(50) UNIQUE NOT NULL,
    reserved_amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

#### Loan Service Schema
```sql
-- Loan Management Tables
CREATE SCHEMA loan_management;

CREATE TABLE loan_management.loans (
    id BIGSERIAL PRIMARY KEY,
    loan_id VARCHAR(50) UNIQUE NOT NULL,
    customer_id VARCHAR(50) NOT NULL,
    loan_amount DECIMAL(15,2) NOT NULL,
    interest_rate DECIMAL(5,4) NOT NULL,
    number_of_installments INTEGER NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    remaining_amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    is_paid BOOLEAN NOT NULL DEFAULT FALSE,
    create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE loan_management.loan_installments (
    id BIGSERIAL PRIMARY KEY,
    installment_id VARCHAR(50) UNIQUE NOT NULL,
    loan_id BIGINT NOT NULL REFERENCES loan_management.loans(id),
    installment_number INTEGER NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    paid_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    due_date DATE NOT NULL,
    payment_date TIMESTAMP,
    is_paid BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

#### Party Management Schema
```sql
-- Party Data Management Tables
CREATE SCHEMA party_management;

CREATE TABLE party_management.parties (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(255) UNIQUE NOT NULL,
    identifier VARCHAR(255) UNIQUE NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    party_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    compliance_level VARCHAR(50) NOT NULL,
    department VARCHAR(100),
    title VARCHAR(100),
    employee_number VARCHAR(50),
    phone_number VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    last_login_at TIMESTAMP,
    password_changed_at TIMESTAMP,
    last_access_review_at TIMESTAMP,
    requires_access_review BOOLEAN DEFAULT FALSE,
    compliance_notes TEXT
);

CREATE TABLE party_management.party_roles (
    id BIGSERIAL PRIMARY KEY,
    party_id BIGINT NOT NULL REFERENCES party_management.parties(id),
    role_name VARCHAR(100) NOT NULL,
    role_description VARCHAR(255) NOT NULL,
    role_source VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    effective_to TIMESTAMP,
    authority_level INTEGER NOT NULL DEFAULT 1,
    business_unit VARCHAR(100),
    geographic_scope VARCHAR(100),
    monetary_limit BIGINT,
    assigned_by VARCHAR(255) NOT NULL,
    assignment_reason TEXT,
    approval_reference VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    last_reviewed_at TIMESTAMP,
    reviewed_by VARCHAR(100),
    next_review_due TIMESTAMP,
    requires_review BOOLEAN DEFAULT FALSE,
    compliance_notes TEXT,
    UNIQUE(party_id, role_name)
);
```

### Data Consistency Patterns

#### Event Sourcing for Audit Trail
```java
@Entity
@Table(name = "party_role_events")
public class PartyRoleEvent {
    
    @Id
    private String eventId;
    
    private Long partyId;
    private String eventType;
    private String eventData;
    private LocalDateTime occurredAt;
    private String userId;
    private String correlationId;
    private Long version;
    
    // Event reconstruction
    public static List<PartyRoleEvent> getEventHistory(Long partyId) {
        return eventRepository.findByPartyIdOrderByOccurredAt(partyId);
    }
    
    public static Party reconstructPartyFromEvents(List<PartyRoleEvent> events) {
        Party party = new Party();
        
        for (PartyRoleEvent event : events) {
            switch (event.getEventType()) {
                case "PartyCreated":
                    applyPartyCreatedEvent(party, event);
                    break;
                case "PartyRoleAssigned":
                    applyRoleAssignedEvent(party, event);
                    break;
                case "PartyRoleRevoked":
                    applyRoleRevokedEvent(party, event);
                    break;
            }
        }
        
        return party;
    }
}
```

#### Distributed Transaction with Saga Pattern
```java
@Component
public class LoanApplicationSaga {
    
    @SagaOrchestrationStart
    public void startLoanApplication(LoanApplicationSubmitted event) {
        
        SagaTransaction.builder()
            .sagaId(event.getApplicationId())
            .addStep(
                "validate-customer",
                () -> customerService.validateCustomer(event.getCustomerId()),
                () -> customerService.rejectApplication(event.getApplicationId())
            )
            .addStep(
                "reserve-credit",
                () -> customerService.reserveCredit(event.getCustomerId(), event.getLoanAmount()),
                () -> customerService.releaseCredit(event.getCustomerId(), event.getLoanAmount())
            )
            .addStep(
                "create-loan",
                () -> loanService.createLoan(event),
                () -> loanService.deleteLoan(event.getLoanId())
            )
            .addStep(
                "generate-installments",
                () -> loanService.generateInstallments(event.getLoanId()),
                () -> loanService.deleteInstallments(event.getLoanId())
            )
            .execute();
    }
}
```

---

## Performance & Scalability

### Caching Strategy

#### Multi-Level Caching
```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        RedisCacheManager.Builder builder = RedisCacheManager
            .RedisCacheManagerBuilder
            .fromConnectionFactory(redisConnectionFactory())
            .cacheDefaults(cacheConfiguration());
        
        return builder.build();
    }
    
    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }
}

@Service
public class PartyRoleService {
    
    @Cacheable(value = "party-roles", key = "#partyId")
    public List<PartyRole> getActiveRoles(Long partyId) {
        return partyRepository.findActiveRolesByPartyId(partyId);
    }
    
    @CacheEvict(value = "party-roles", key = "#partyId")
    public void assignRole(Long partyId, String roleName) {
        // Role assignment logic
    }
}
```

#### Database Query Optimization
```java
@Repository
public class OptimizedLoanRepository {
    
    // Use pagination for large result sets
    @Query("""
        SELECT l FROM Loan l 
        WHERE l.customerId = :customerId 
        AND l.status = :status
        ORDER BY l.createDate DESC
        """)
    Page<Loan> findByCustomerIdAndStatus(
        @Param("customerId") String customerId,
        @Param("status") LoanStatus status,
        Pageable pageable
    );
    
    // Use projection for read-only queries
    @Query("""
        SELECT new com.banking.loans.dto.LoanSummary(
            l.loanId, l.loanAmount, l.remainingAmount, l.status
        )
        FROM Loan l 
        WHERE l.customerId = :customerId
        """)
    List<LoanSummary> findLoanSummariesByCustomerId(@Param("customerId") String customerId);
    
    // Use native queries for complex operations
    @Query(value = """
        SELECT l.*, COUNT(li.id) as installment_count,
               SUM(CASE WHEN li.is_paid THEN li.amount ELSE 0 END) as paid_amount
        FROM loan_management.loans l
        LEFT JOIN loan_management.loan_installments li ON l.id = li.loan_id
        WHERE l.customer_id = :customerId
        GROUP BY l.id
        """, nativeQuery = true)
    List<Object[]> findLoanStatisticsByCustomerId(@Param("customerId") String customerId);
}
```

### Horizontal Scaling

#### Kubernetes Horizontal Pod Autoscaler
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: banking-app-hpa
  namespace: banking-system
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: banking-app
  minReplicas: 3
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  - type: Pods
    pods:
      metric:
        name: http_requests_per_second
      target:
        type: AverageValue
        averageValue: "100"
```

#### Database Connection Pooling
```yaml
# Application configuration
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      max-lifetime: 600000
      connection-timeout: 30000
      validation-timeout: 5000
      leak-detection-threshold: 60000
      
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        jdbc:
          batch_size: 25
        order_inserts: true
        order_updates: true
        batch_versioned_data: true
```

### Performance Monitoring

#### Application Performance Monitoring
```java
@Component
public class PerformanceMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Counter loanCreationCounter;
    private final Timer loanCreationTimer;
    private final Gauge activeLoanGauge;
    
    public PerformanceMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.loanCreationCounter = Counter.builder("loan.creation.count")
            .description("Number of loans created")
            .register(meterRegistry);
        this.loanCreationTimer = Timer.builder("loan.creation.duration")
            .description("Loan creation duration")
            .register(meterRegistry);
        this.activeLoanGauge = Gauge.builder("loan.active.count")
            .description("Number of active loans")
            .register(meterRegistry, this, PerformanceMetrics::getActiveLoanCount);
    }
    
    public void recordLoanCreation(Duration duration) {
        loanCreationCounter.increment();
        loanCreationTimer.record(duration);
    }
    
    private double getActiveLoanCount() {
        return loanRepository.countByStatus(LoanStatus.ACTIVE);
    }
}
```

---

## Conclusion

The Application Architecture of the Enterprise Banking System demonstrates a sophisticated implementation of modern software architecture principles:

### Key Achievements

1. **Domain-Driven Design Implementation**
   - Clear bounded contexts for business domains
   - Rich domain models with business logic
   - Event-driven communication between contexts
   - Ubiquitous language throughout the system

2. **Microservices Architecture**
   - Service autonomy and independence
   - Scalable and maintainable service design
   - Proper service decomposition strategies
   - Resilient communication patterns

3. **Security Integration**
   - OAuth2.1 integration at every layer
   - Method-level security with business rules
   - Comprehensive authorization framework
   - Audit trail for compliance requirements

4. **Performance and Scalability**
   - Multi-level caching strategies
   - Database optimization techniques
   - Horizontal scaling capabilities
   - Comprehensive monitoring and metrics

The architecture provides a solid foundation for the banking system that can evolve with changing business requirements while maintaining security, performance, and regulatory compliance standards.

For deployment and operational details, refer to the [Infrastructure Architecture Guide](../technology-architecture/Infrastructure-Architecture-Guide.md) and [Security Architecture Overview](../security-architecture/Security-Architecture-Overview.md).