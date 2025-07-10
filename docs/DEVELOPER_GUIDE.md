# Developer Guide
## Enterprise Loan Management Platform

### Version 2.0 | January 2025

---

## 1. Introduction

Welcome to the Enterprise Loan Management Platform development team. This guide provides everything you need to contribute effectively to the platform. We've built this system using modern Java practices and industry-standard patterns to ensure maintainability and scalability.

### 1.1 Platform Overview

The platform is a comprehensive loan management solution built on:
- Java 21 with virtual threads
- Spring Boot 3.4.3
- Hexagonal architecture
- Domain-driven design principles
- Event-driven microservices

### 1.2 Development Philosophy

We follow these core principles:
- **Clean Code** - Code is written for humans to read
- **Test-Driven** - Tests are written before implementation
- **Domain-First** - Business logic drives technical decisions
- **Continuous Improvement** - Regular refactoring and optimization

## 2. Getting Started

### 2.1 Prerequisites

Ensure you have the following installed:

```bash
# Required versions
Java 21 (LTS)
Docker 20.10+
Git 2.30+
IntelliJ IDEA 2024.x or VS Code with Java extensions

# Optional but recommended
Gradle 8.11+ (project includes wrapper)
PostgreSQL 15+ (for local debugging)
Redis 7+ (for local caching)
```

### 2.2 Repository Setup

```bash
# Clone the repository
git clone git@github.com:enterprise/loan-management-platform.git
cd loan-management-platform

# Set up pre-commit hooks
./scripts/setup-hooks.sh

# Verify Java version
java -version
# Should show: openjdk version "21.x.x"

# Build the project
./gradlew clean build

# Run tests
./gradlew test

# Start local environment
docker-compose up -d
./gradlew bootRun
```

### 2.3 IDE Configuration

**IntelliJ IDEA Setup**
1. Import as Gradle project
2. Enable annotation processing
3. Set project SDK to Java 21
4. Install recommended plugins:
   - Lombok
   - Spring Boot
   - Docker
   - SonarLint

2. Import code style:
   - File → Settings → Editor → Code Style → Import Scheme
   - Select `config/ide/intellij-codestyle.xml`

3. Enable annotation processing:
   - File → Settings → Build → Compiler → Annotation Processors
   - Check "Enable annotation processing"

#### VS Code
1. Install extensions:
   - Java Extension Pack
   - Spring Boot Extension Pack
   - Lombok Annotations Support
   - Docker

2. Configure settings.json:
```json
{
  "java.configuration.updateBuildConfiguration": "automatic",
  "java.compile.nullAnalysis.mode": "automatic",
  "editor.formatOnSave": true
}
```

### Local Infrastructure Setup

#### Using Docker Compose
```yaml
# docker-compose.yml
version: '3.8'
services:
  postgres:
    image: postgres:14-alpine
    environment:
      POSTGRES_DB: loandb
      POSTGRES_USER: loanuser
      POSTGRES_PASSWORD: loanpass
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:6-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

  kafka:
    image: confluentinc/cp-kafka:latest
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
    ports:
      - "9092:9092"

  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    ports:
      - "2181:2181"

volumes:
  postgres_data:
  redis_data:
```

## Project Structure

```
enterprise-loan-management-system/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/loanmanagement/
│   │   │       ├── customer/          # Customer bounded context
│   │   │       │   ├── domain/       # Domain layer
│   │   │       │   ├── application/  # Application services
│   │   │       │   └── infrastructure/ # Infrastructure adapters
│   │   │       ├── loan/             # Loan bounded context
│   │   │       ├── payment/          # Payment bounded context
│   │   │       └── shared/           # Shared kernel
│   │   └── resources/
│   │       ├── application.yml       # Main configuration
│   │       ├── application-dev.yml   # Development profile
│   │       └── db/migration/         # Flyway migrations
│   └── test/
│       ├── java/                     # Test sources
│       └── resources/                # Test resources
├── docs/                             # Documentation
├── scripts/                          # Utility scripts
└── k8s/                             # Kubernetes manifests
```

### Package Structure Guidelines

#### Domain Layer
```java
com.loanmanagement.{context}.domain/
├── model/           # Entities and Value Objects
├── service/         # Domain Services
├── event/           # Domain Events
└── port/            # Port interfaces
    ├── in/          # Inbound ports (use cases)
    └── out/         # Outbound ports (repositories)
```

#### Application Layer
```java
com.loanmanagement.{context}.application/
├── service/         # Application Services
├── port/            # Application-specific ports
└── dto/             # Data Transfer Objects
```

#### Infrastructure Layer
```java
com.loanmanagement.{context}.infrastructure/
├── adapter/
│   ├── in/          # Inbound adapters (controllers)
│   └── out/         # Outbound adapters (repositories)
├── config/          # Configuration classes
└── persistence/     # JPA entities
```

## Development Workflow

### Feature Development Process

1. **Create Feature Branch**
```bash
git checkout -b feature/LOAN-123-customer-validation
```

2. **Write Failing Tests (TDD)**
```java
@Test
@DisplayName("Should validate customer email format")
void shouldValidateCustomerEmailFormat() {
    // Given
    String invalidEmail = "invalid-email";
    
    // When/Then
    assertThrows(InvalidEmailException.class, 
        () -> new EmailAddress(invalidEmail));
}
```

3. **Implement Feature**
```java
public record EmailAddress(String value) {
    public EmailAddress {
        if (!isValidEmail(value)) {
            throw new InvalidEmailException(value);
        }
    }
    
    private static boolean isValidEmail(String email) {
        return email != null && 
               email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }
}
```

4. **Run Tests**
```bash
./gradlew test -Dtest=EmailAddressTest
```

5. **Commit Changes**
```bash
git add .
git commit -m "feat(customer): add email validation

- Implement EmailAddress value object
- Add validation for email format
- Add comprehensive tests

LOAN-123"
```

### Code Review Process

1. **Create Pull Request**
   - Use PR template
   - Link to JIRA ticket
   - Add relevant reviewers

2. **Review Checklist**
   - [ ] Tests pass
   - [ ] Code coverage > 80%
   - [ ] No security vulnerabilities
   - [ ] Documentation updated
   - [ ] Performance impact assessed

## Coding Standards

### Java Coding Standards

#### General Guidelines
- Use Java 17 features (records, switch expressions, text blocks)
- Prefer immutability
- Follow SOLID principles
- Use meaningful names

#### Naming Conventions
```java
// Classes - PascalCase
public class CustomerService { }

// Interfaces - PascalCase, no 'I' prefix
public interface CustomerRepository { }

// Methods - camelCase, verb phrases
public Customer createCustomer(CreateCustomerCommand command) { }

// Variables - camelCase, descriptive
private final CustomerRepository customerRepository;

// Constants - UPPER_SNAKE_CASE
public static final int MAX_RETRY_ATTEMPTS = 3;
```

#### Code Style Example
```java
package com.loanmanagement.customer.domain.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Customer aggregate root representing a bank customer.
 * 
 * <p>This class encapsulates all customer-related business rules
 * and ensures consistency of the customer state.</p>
 */
public class Customer {
    private final CustomerId id;
    private PersonalName name;
    private EmailAddress email;
    private PhoneNumber phone;
    private LocalDate dateOfBirth;
    private CustomerStatus status;
    
    /**
     * Creates a new customer with the provided details.
     * 
     * @param name customer's personal name
     * @param email customer's email address
     * @param phone customer's phone number
     * @param dateOfBirth customer's date of birth
     * @throws IllegalArgumentException if any parameter is null
     */
    public Customer(PersonalName name, 
                   EmailAddress email, 
                   PhoneNumber phone, 
                   LocalDate dateOfBirth) {
        this.id = CustomerId.generate();
        this.name = Objects.requireNonNull(name, "Name is required");
        this.email = Objects.requireNonNull(email, "Email is required");
        this.phone = Objects.requireNonNull(phone, "Phone is required");
        this.dateOfBirth = Objects.requireNonNull(dateOfBirth, "Date of birth is required");
        this.status = CustomerStatus.PENDING;
        
        validateAge();
    }
    
    private void validateAge() {
        if (getAge() < 18) {
            throw new UnderageCustomerException(
                "Customer must be at least 18 years old");
        }
    }
    
    // Domain methods...
}
```

### Testing Standards

#### Test Structure
```java
@SpringBootTest
@DisplayName("Customer Service Tests")
class CustomerServiceTest {
    
    @Nested
    @DisplayName("When creating a customer")
    class CreateCustomerTests {
        
        @Test
        @DisplayName("Should create customer with valid data")
        void shouldCreateCustomerWithValidData() {
            // Given - Arrange test data
            CreateCustomerCommand command = createValidCommand();
            
            // When - Execute the action
            Customer customer = customerService.createCustomer(command);
            
            // Then - Assert the outcome
            assertAll(
                () -> assertNotNull(customer.getId()),
                () -> assertEquals("John", customer.getName().firstName()),
                () -> assertEquals(CustomerStatus.PENDING, customer.getStatus())
            );
        }
    }
}
```

## Testing Guidelines

### Test Categories

1. **Unit Tests**
   - Test single units in isolation
   - Mock external dependencies
   - Fast execution
   - Location: `src/test/java/.../unit/`

2. **Integration Tests**
   - Test component integration
   - Use test containers
   - Test database operations
   - Location: `src/test/java/.../integration/`

3. **Architecture Tests**
   - Verify architectural constraints
   - Check package dependencies
   - Enforce coding rules
   - Location: `src/test/java/.../architecture/`

### Testing Best Practices

```java
// Use descriptive test names
@Test
@DisplayName("Should calculate loan interest correctly for annual rate")
void shouldCalculateLoanInterestCorrectlyForAnnualRate() { }

// Use parameterized tests for multiple scenarios
@ParameterizedTest
@ValueSource(strings = {"", " ", "invalid-email"})
@DisplayName("Should reject invalid email formats")
void shouldRejectInvalidEmailFormats(String email) { }

// Use test fixtures
@BeforeEach
void setUp() {
    testCustomer = CustomerFixture.validCustomer().build();
}

// Verify behavior, not implementation
verify(eventPublisher).publish(any(CustomerCreatedEvent.class));
```

## API Development

### REST API Guidelines

#### Endpoint Design
```java
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {
        
        Customer customer = customerService.createCustomer(
            mapper.toCommand(request));
        
        return ResponseEntity
            .created(URI.create("/api/v1/customers/" + customer.getId()))
            .body(mapper.toResponse(customer));
    }
    
    @GetMapping("/{id}")
    public CustomerResponse getCustomer(@PathVariable Long id) {
        return mapper.toResponse(
            customerService.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id))
        );
    }
}
```

#### Error Handling
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            CustomerNotFoundException ex) {
        
        ErrorResponse error = ErrorResponse.builder()
            .code("CUSTOMER_NOT_FOUND")
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
            
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
```

### API Documentation
```java
@Operation(summary = "Create a new customer",
          description = "Creates a new customer with the provided details")
@ApiResponses({
    @ApiResponse(responseCode = "201", 
                description = "Customer created successfully"),
    @ApiResponse(responseCode = "400", 
                description = "Invalid customer data"),
    @ApiResponse(responseCode = "409", 
                description = "Customer already exists")
})
@PostMapping
public ResponseEntity<CustomerResponse> createCustomer(
        @RequestBody CreateCustomerRequest request) {
    // Implementation
}
```

## Domain Development

### Creating Domain Entities

```java
// Value Object
public record Money(String currency, BigDecimal amount) {
    public Money {
        Objects.requireNonNull(currency, "Currency is required");
        Objects.requireNonNull(amount, "Amount is required");
        
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
    }
    
    public Money add(Money other) {
        if (!currency.equals(other.currency)) {
            throw new CurrencyMismatchException(currency, other.currency);
        }
        return new Money(currency, amount.add(other.amount));
    }
}

// Entity
public class LoanInstallment {
    private final InstallmentId id;
    private final LocalDate dueDate;
    private Money principalAmount;
    private Money interestAmount;
    private InstallmentStatus status;
    
    public void markAsPaid(Payment payment) {
        if (status != InstallmentStatus.PENDING) {
            throw new InstallmentAlreadyPaidException(id);
        }
        
        // Business logic for payment application
        this.status = InstallmentStatus.PAID;
    }
}
```

### Implementing Use Cases

```java
@Component
@RequiredArgsConstructor
public class CreateLoanUseCase {
    private final CustomerRepository customerRepository;
    private final LoanRepository loanRepository;
    private final RiskAssessmentService riskAssessmentService;
    private final EventPublisher eventPublisher;
    
    @Transactional
    public Loan execute(CreateLoanCommand command) {
        // 1. Validate customer exists
        Customer customer = customerRepository.findById(command.customerId())
            .orElseThrow(() -> new CustomerNotFoundException(command.customerId()));
        
        // 2. Perform risk assessment
        RiskAssessment assessment = riskAssessmentService.assess(customer, command);
        
        if (assessment.isHighRisk()) {
            throw new LoanApplicationRejectedException(assessment.getReason());
        }
        
        // 3. Create loan
        Loan loan = Loan.create(customer, command, assessment);
        
        // 4. Save loan
        Loan savedLoan = loanRepository.save(loan);
        
        // 5. Publish event
        eventPublisher.publish(new LoanCreatedEvent(savedLoan));
        
        return savedLoan;
    }
}
```

## Security Implementation

### Authentication Setup

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            .build();
    }
}
```

### Method Security

```java
@Service
@PreAuthorize("hasRole('LOAN_OFFICER')")
public class LoanApprovalService {
    
    @PreAuthorize("hasPermission(#loanId, 'LOAN', 'APPROVE')")
    public void approveLoan(Long loanId) {
        // Implementation
    }
    
    @PostAuthorize("returnObject.customerId == authentication.principal.customerId")
    public Loan getLoan(Long loanId) {
        // Implementation
    }
}
```

## Troubleshooting

### Common Issues

#### 1. Database Connection Issues
```bash
# Check PostgreSQL is running
docker ps | grep postgres

# Check connection settings
psql -h localhost -p 5432 -U loanuser -d loandb

# Check application properties
grep datasource src/main/resources/application.yml
```

#### 2. Test Failures
```bash
# Run specific test with debug output
./gradlew test -Dtest=CustomerServiceTest -X

# Skip tests temporarily
./gradlew clean install -DskipTests

# Run only unit tests
./gradlew test -Dgroups=unit
```

#### 3. Build Issues
```bash
# Clean build
./gradlew clean install

# Update dependencies
./gradlew versions:display-dependency-updates

# Check for dependency conflicts
./gradlew dependency:tree
```

### Debug Configuration

```yaml
# application-dev.yml
logging:
  level:
    com.loanmanagement: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql: TRACE
    
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
```

### Performance Profiling

```java
// Add timing logs
@Slf4j
@Component
@Aspect
public class PerformanceAspect {
    
    @Around("@annotation(Timed)")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        
        try {
            return joinPoint.proceed();
        } finally {
            long duration = System.currentTimeMillis() - start;
            log.info("{} executed in {} ms", 
                    joinPoint.getSignature().toShortString(), duration);
        }
    }
}
```

## Additional Resources

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Domain-Driven Design Reference](https://www.domainlanguage.com/ddd/reference/)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Test-Driven Development](https://martinfowler.com/bliki/TestDrivenDevelopment.html)

## Getting Help

- **Slack Channel**: #loan-management-dev
- **Wiki**: https://wiki.company.com/loan-management
- **Tech Lead**: tech-lead@company.com
- **Architecture Team**: architecture@company.com