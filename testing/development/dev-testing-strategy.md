# Development Environment Testing Strategy
## Enterprise Banking System - Comprehensive Testing Framework

### 🎯 **Development Testing Objectives**

**Primary Goals:**
- **Early Bug Detection** - Catch issues at the source before they propagate
- **Code Quality Assurance** - Ensure adherence to banking security standards
- **Rapid Feedback Loop** - Enable fast development cycles with confidence
- **Test-Driven Development** - Drive design through comprehensive test coverage

### 📋 **Testing Pyramid Structure**

```
                    /\
                   /  \
                  /E2E \     ← 10% (Critical User Journeys)
                 /______\
                /        \
               /Integration\ ← 20% (Service Interactions)
              /__________\
             /            \
            /    Unit      \ ← 70% (Business Logic)
           /______________\
```

### 🔬 **Unit Testing Framework**

**Coverage Requirements:**
- **Minimum 85% code coverage** for all banking domain logic
- **100% coverage** for security-critical functions
- **Mandatory tests** for all encryption/decryption operations

**Testing Categories:**

#### **1. Domain Model Testing**
```java
// Customer Domain Tests
@Test
void shouldEncryptPiiDataWhenCreatingCustomer() {
    // Given
    CustomerCreationCommand command = CustomerCreationCommand.builder()
        .firstName("John")
        .lastName("Doe")
        .email("john.doe@example.com")
        .ssn("123-45-6789")
        .build();
    
    // When
    Customer customer = Customer.create(command);
    
    // Then
    assertThat(customer.getFirstNameEncrypted()).isNotEqualTo("John");
    assertThat(customer.getDecryptedFirstName()).isEqualTo("John");
    assertThat(customer.getSsnHash()).isNotNull();
}

@Test
void shouldValidateKycStatusTransitions() {
    // Given
    Customer customer = createCustomerWithStatus(KycStatus.PENDING);
    
    // When & Then
    assertThat(customer.canTransitionTo(KycStatus.VERIFIED)).isTrue();
    assertThat(customer.canTransitionTo(KycStatus.REJECTED)).isTrue();
    
    customer.verifyKyc();
    assertThat(customer.canTransitionTo(KycStatus.PENDING)).isFalse();
}
```

#### **2. Security Function Testing**
```java
@Test
void shouldRotateEncryptionKeysSuccessfully() {
    // Given
    String originalData = "sensitive-data";
    String encryptedV1 = encryptionService.encrypt(originalData, KeyVersion.V1);
    
    // When
    encryptionService.rotateKeys();
    String encryptedV2 = encryptionService.encrypt(originalData, KeyVersion.V2);
    
    // Then
    assertThat(encryptedV1).isNotEqualTo(encryptedV2);
    assertThat(encryptionService.decrypt(encryptedV1)).isEqualTo(originalData);
    assertThat(encryptionService.decrypt(encryptedV2)).isEqualTo(originalData);
}

@Test
void shouldDetectAndPreventSqlInjection() {
    // Given
    String maliciousInput = "'; DROP TABLE customers; --";
    
    // When & Then
    assertThatThrownBy(() -> customerService.findByEmail(maliciousInput))
        .isInstanceOf(SecurityException.class)
        .hasMessageContaining("SQL injection attempt detected");
}
```

#### **3. Business Logic Testing**
```java
@Test
void shouldCalculateLoanPaymentScheduleAccurately() {
    // Given
    LoanApplication application = LoanApplication.builder()
        .principalAmount(Money.of(100000))
        .interestRate(new BigDecimal("0.05"))
        .termMonths(60)
        .build();
    
    // When
    PaymentSchedule schedule = loanCalculationService.calculateSchedule(application);
    
    // Then
    assertThat(schedule.getPayments()).hasSize(60);
    assertThat(schedule.getTotalInterest()).isEqualTo(Money.of(26596.47));
    assertThat(schedule.getMonthlyPayment()).isEqualTo(Money.of(2110.94));
}
```

### 🔗 **Integration Testing Framework**

**Scope:** Test interactions between different layers and external services

#### **1. Repository Integration Tests**
```java
@DataJpaTest
@Testcontainers
class CustomerRepositoryIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("banking_test")
        .withUsername("test")
        .withPassword("test");
    
    @Test
    void shouldEncryptAndDecryptCustomerData() {
        // Given
        Customer customer = Customer.builder()
            .firstName("John")
            .lastName("Doe")
            .email("john@example.com")
            .build();
        
        // When
        Customer saved = customerRepository.save(customer);
        Customer retrieved = customerRepository.findById(saved.getId()).orElseThrow();
        
        // Then
        assertThat(retrieved.getDecryptedFirstName()).isEqualTo("John");
        assertThat(retrieved.getFirstNameEncrypted()).isNotEqualTo("John");
    }
}
```

#### **2. Service Layer Integration Tests**
```java
@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
class LoanApplicationServiceIntegrationTest {
    
    @Test
    @Order(1)
    void shouldProcessLoanApplicationEndToEnd() {
        // Given
        CustomerCreationCommand customerCommand = createCustomerCommand();
        Customer customer = customerService.createCustomer(customerCommand);
        
        LoanApplicationCommand loanCommand = LoanApplicationCommand.builder()
            .customerId(customer.getId())
            .loanType(LoanType.PERSONAL)
            .principalAmount(Money.of(50000))
            .termMonths(36)
            .build();
        
        // When
        LoanApplication application = loanApplicationService.submitApplication(loanCommand);
        
        // Then
        assertThat(application.getStatus()).isEqualTo(LoanStatus.PENDING);
        assertThat(application.getRiskAssessment()).isNotNull();
    }
    
    @Test
    @Order(2) 
    void shouldTriggerFraudDetectionForHighRiskApplication() {
        // Given
        LoanApplicationCommand suspiciousCommand = LoanApplicationCommand.builder()
            .customerId(createHighRiskCustomer().getId())
            .loanType(LoanType.PERSONAL)
            .principalAmount(Money.of(500000)) // High amount
            .termMonths(12) // Short term
            .build();
        
        // When
        LoanApplication application = loanApplicationService.submitApplication(suspiciousCommand);
        
        // Then
        assertThat(application.getFraudScore()).isGreaterThan(0.7);
        assertThat(application.getStatus()).isEqualTo(LoanStatus.UNDER_REVIEW);
    }
}
```

### 🏛️ **Component Testing Framework**

**Scope:** Test complete components in isolation with mocked external dependencies

#### **1. REST API Component Tests**
```java
@WebMvcTest(CustomerController.class)
class CustomerControllerComponentTest {
    
    @MockBean
    private CustomerService customerService;
    
    @Test
    void shouldCreateCustomerWithValidData() throws Exception {
        // Given
        CreateCustomerRequest request = CreateCustomerRequest.builder()
            .firstName("John")
            .lastName("Doe")
            .email("john@example.com")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .build();
        
        Customer customer = Customer.builder()
            .id(UUID.randomUUID())
            .firstName("John")
            .lastName("Doe")
            .build();
        
        when(customerService.createCustomer(any())).thenReturn(customer);
        
        // When & Then
        mockMvc.perform(post("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }
    
    @Test
    void shouldValidateRequestData() throws Exception {
        // Given
        CreateCustomerRequest invalidRequest = CreateCustomerRequest.builder()
            .firstName("") // Invalid
            .email("invalid-email") // Invalid
            .build();
        
        // When & Then
        mockMvc.perform(post("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("firstName", "email")));
    }
}
```

### 🔒 **Security Testing Framework**

#### **1. Authentication & Authorization Tests**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityIntegrationTest {
    
    @Test
    void shouldRequireAuthenticationForProtectedEndpoints() {
        // When & Then
        restTemplate.exchange("/api/v1/customers", 
            HttpMethod.GET, 
            new HttpEntity<>(null), 
            String.class)
            .getStatusCode()
            .is4xxClientError();
    }
    
    @Test
    void shouldValidateFapiCompliantHeaders() {
        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("valid-jwt-token");
        headers.set("x-fapi-auth-date", String.valueOf(Instant.now().getEpochSecond()));
        headers.set("x-fapi-customer-ip-address", "192.168.1.100");
        headers.set("x-fapi-interaction-id", UUID.randomUUID().toString());
        
        // When & Then
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/v1/customers",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
```

### 📊 **Performance Testing Framework**

#### **1. Load Testing with JMeter Integration**
```java
@Test
void shouldHandleConcurrentLoanApplications() {
    // Given
    int concurrentUsers = 100;
    int applicationsPerUser = 10;
    CountDownLatch latch = new CountDownLatch(concurrentUsers);
    List<CompletableFuture<Void>> futures = new ArrayList<>();
    
    // When
    for (int i = 0; i < concurrentUsers; i++) {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                for (int j = 0; j < applicationsPerUser; j++) {
                    loanApplicationService.submitApplication(createLoanCommand());
                }
            } finally {
                latch.countDown();
            }
        });
        futures.add(future);
    }
    
    // Then
    assertThat(latch.await(60, TimeUnit.SECONDS)).isTrue();
    futures.forEach(future -> assertThat(future).isCompletedWithoutException());
}
```

### 🗄️ **Database Testing Framework**

#### **1. Migration Testing**
```java
@Test
@Sql(scripts = "/db/migration/test-data.sql")
void shouldMigrateDataWithoutLoss() {
    // Given
    int initialCustomerCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM banking_customer.customers", Integer.class);
    
    // When
    flywayMigrate.migrate();
    
    // Then
    int finalCustomerCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM banking_customer.customers_enhanced", Integer.class);
    
    assertThat(finalCustomerCount).isEqualTo(initialCustomerCount);
}
```

### 🔍 **Test Data Management**

#### **1. Test Data Factories**
```java
@Component
public class TestDataFactory {
    
    public Customer createTestCustomer() {
        return Customer.builder()
            .id(UUID.randomUUID())
            .firstName("Test")
            .lastName("Customer")
            .email("test@example.com")
            .customerStatus(CustomerStatus.ACTIVE)
            .kycStatus(KycStatus.VERIFIED)
            .build();
    }
    
    public LoanApplication createTestLoanApplication(Customer customer) {
        return LoanApplication.builder()
            .id(UUID.randomUUID())
            .customerId(customer.getId())
            .loanType(LoanType.PERSONAL)
            .principalAmount(Money.of(25000))
            .interestRate(new BigDecimal("0.065"))
            .termMonths(36)
            .status(LoanStatus.PENDING)
            .build();
    }
}
```

### 📈 **Test Metrics and Reporting**

#### **1. Coverage Requirements**
- **Unit Tests:** 85% line coverage, 90% branch coverage
- **Integration Tests:** 75% service interaction coverage
- **Security Tests:** 100% authentication/authorization paths
- **Performance Tests:** All critical paths under load

#### **2. Quality Gates**
- **No critical security vulnerabilities** (SonarQube)
- **Zero known SQL injection vectors**
- **All PII data encrypted in tests**
- **Maximum 5% test flakiness**

### 🚀 **Continuous Integration Pipeline**

```yaml
# .github/workflows/dev-testing.yml
name: Development Testing Pipeline

on:
  push:
    branches: [ develop, feature/* ]
  pull_request:
    branches: [ develop ]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '21'
      - name: Run Unit Tests
        run: ./gradlew test jacocoTestReport
      - name: Coverage Check
        run: ./gradlew jacocoTestCoverageVerification

  integration-tests:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_PASSWORD: test
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    steps:
      - uses: actions/checkout@v3
      - name: Run Integration Tests
        run: ./gradlew integrationTest

  security-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: OWASP Dependency Check
        run: ./gradlew dependencyCheckAnalyze
      - name: Security Tests
        run: ./gradlew securityTest
      - name: SonarQube Analysis
        run: ./gradlew sonarqube

  performance-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Performance Tests
        run: ./gradlew performanceTest
      - name: Load Tests
        run: ./gradlew loadTest
```

### 🛠️ **Development Environment Setup**

#### **1. Local Testing Infrastructure**
```bash
# Start local testing environment
docker-compose -f docker-compose.test.yml up -d

# Run all tests
./gradlew check

# Run specific test suites
./gradlew unitTest
./gradlew integrationTest
./gradlew securityTest
./gradlew performanceTest
```

#### **2. Test Environment Configuration**
```yaml
# application-test.yml
spring:
  profiles:
    active: test
  datasource:
    url: jdbc:postgresql://localhost:5432/banking_test
    username: test
    password: test
  jpa:
    hibernate:
      ddl-auto: create-drop

banking:
  encryption:
    key: test-key-for-development-only
  security:
    jwt:
      secret: test-jwt-secret
  testing:
    enabled: true
    mock-external-services: true
```

This comprehensive development testing strategy ensures **high-quality, secure code** through multiple layers of validation before code reaches the next environment.