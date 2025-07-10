# Testing Guide - Enterprise Loan Management System

## Table of Contents
1. [Overview](#overview)
2. [Testing Strategy](#testing-strategy)
3. [Unit Testing](#unit-testing)
4. [Integration Testing](#integration-testing)
5. [Architecture Testing](#architecture-testing)
6. [Performance Testing](#performance-testing)
7. [Security Testing](#security-testing)
8. [End-to-End Testing](#end-to-end-testing)
9. [Test Data Management](#test-data-management)
10. [Continuous Testing](#continuous-testing)

## Overview

This guide provides comprehensive testing strategies and practices for the Enterprise Loan Management System. Following Test-Driven Development (TDD) principles, our testing approach ensures high quality, maintainability, and reliability.

### Testing Philosophy
- **Test First**: Write tests before implementation
- **Test Coverage**: Maintain minimum 80% code coverage
- **Test Pyramid**: Balance unit, integration, and E2E tests
- **Fast Feedback**: Tests should run quickly
- **Isolation**: Tests should be independent

## Testing Strategy

### Test Pyramid
```
         /\
        /E2E\        (5%)  - End-to-End Tests
       /______\
      /  API   \     (15%) - API/Contract Tests
     /__________\
    /Integration \   (20%) - Integration Tests
   /______________\
  /     Unit      \  (60%) - Unit Tests
 /__________________\
```

### Test Categories

| Category | Purpose | Execution Time | Frequency |
|----------|---------|----------------|-----------|
| Unit | Test individual components | < 5 seconds | Every commit |
| Integration | Test component interactions | < 30 seconds | Every commit |
| Architecture | Verify design constraints | < 10 seconds | Every commit |
| API | Test REST endpoints | < 1 minute | Every commit |
| Performance | Test system performance | < 5 minutes | Daily |
| Security | Test security features | < 3 minutes | Daily |
| E2E | Test complete workflows | < 10 minutes | Before release |

## Unit Testing

### Domain Model Testing

```java
@DisplayName("Customer Domain Tests")
class CustomerTest {
    
    @Test
    @DisplayName("Should create customer with valid data")
    void shouldCreateCustomerWithValidData() {
        // Given
        PersonalName name = new PersonalName("John", "Doe");
        EmailAddress email = new EmailAddress("john.doe@example.com");
        PhoneNumber phone = new PhoneNumber("+1-555-123-4567");
        LocalDate dateOfBirth = LocalDate.of(1990, 1, 15);
        
        // When
        Customer customer = new Customer(name, email, phone, dateOfBirth);
        
        // Then
        assertAll(
            () -> assertNotNull(customer.getId()),
            () -> assertEquals(name, customer.getName()),
            () -> assertEquals(email, customer.getEmail()),
            () -> assertEquals(phone, customer.getPhone()),
            () -> assertEquals(dateOfBirth, customer.getDateOfBirth()),
            () -> assertEquals(CustomerStatus.PENDING, customer.getStatus())
        );
    }
    
    @Test
    @DisplayName("Should reject customer under 18 years old")
    void shouldRejectCustomerUnder18() {
        // Given
        PersonalName name = new PersonalName("Jane", "Doe");
        EmailAddress email = new EmailAddress("jane.doe@example.com");
        PhoneNumber phone = new PhoneNumber("+1-555-987-6543");
        LocalDate dateOfBirth = LocalDate.now().minusYears(17);
        
        // When/Then
        assertThrows(UnderageCustomerException.class, 
            () -> new Customer(name, email, phone, dateOfBirth),
            "Customer must be at least 18 years old");
    }
    
    @Nested
    @DisplayName("Customer Status Transitions")
    class StatusTransitions {
        
        private Customer customer;
        
        @BeforeEach
        void setUp() {
            customer = CustomerFixture.pendingCustomer().build();
        }
        
        @Test
        @DisplayName("Should transition from PENDING to ACTIVE")
        void shouldTransitionFromPendingToActive() {
            // When
            customer.activate();
            
            // Then
            assertEquals(CustomerStatus.ACTIVE, customer.getStatus());
        }
        
        @Test
        @DisplayName("Should not activate already active customer")
        void shouldNotActivateAlreadyActiveCustomer() {
            // Given
            customer.activate();
            
            // When/Then
            assertThrows(InvalidStateTransitionException.class,
                () -> customer.activate());
        }
    }
}
```

### Value Object Testing

```java
@DisplayName("Money Value Object Tests")
class MoneyTest {
    
    @Test
    @DisplayName("Should create money with valid amount")
    void shouldCreateMoneyWithValidAmount() {
        // When
        Money money = Money.of("USD", BigDecimal.valueOf(100.50));
        
        // Then
        assertEquals("USD", money.getCurrency());
        assertEquals(BigDecimal.valueOf(100.50), money.getAmount());
    }
    
    @Test
    @DisplayName("Should not allow negative amounts")
    void shouldNotAllowNegativeAmounts() {
        assertThrows(IllegalArgumentException.class,
            () -> Money.of("USD", BigDecimal.valueOf(-100)));
    }
    
    @Test
    @DisplayName("Should add money with same currency")
    void shouldAddMoneyWithSameCurrency() {
        // Given
        Money money1 = Money.of("USD", BigDecimal.valueOf(100));
        Money money2 = Money.of("USD", BigDecimal.valueOf(50));
        
        // When
        Money result = money1.add(money2);
        
        // Then
        assertEquals(Money.of("USD", BigDecimal.valueOf(150)), result);
    }
    
    @Test
    @DisplayName("Should not add money with different currencies")
    void shouldNotAddMoneyWithDifferentCurrencies() {
        // Given
        Money money1 = Money.of("USD", BigDecimal.valueOf(100));
        Money money2 = Money.of("EUR", BigDecimal.valueOf(50));
        
        // When/Then
        assertThrows(CurrencyMismatchException.class,
            () -> money1.add(money2));
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"USD", "EUR", "GBP", "JPY"})
    @DisplayName("Should support multiple currencies")
    void shouldSupportMultipleCurrencies(String currency) {
        // When
        Money money = Money.of(currency, BigDecimal.TEN);
        
        // Then
        assertEquals(currency, money.getCurrency());
    }
}
```

### Service Testing

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("Loan Application Service Tests")
class LoanApplicationServiceTest {
    
    @Mock
    private CustomerRepository customerRepository;
    
    @Mock
    private LoanRepository loanRepository;
    
    @Mock
    private RiskAssessmentService riskAssessmentService;
    
    @Mock
    private EventPublisher eventPublisher;
    
    @InjectMocks
    private LoanApplicationService loanApplicationService;
    
    @Test
    @DisplayName("Should create loan application successfully")
    void shouldCreateLoanApplicationSuccessfully() {
        // Given
        Long customerId = 123L;
        Customer customer = CustomerFixture.activeCustomer()
            .withId(customerId)
            .build();
        
        CreateLoanCommand command = CreateLoanCommand.builder()
            .customerId(customerId)
            .principalAmount(Money.of("USD", BigDecimal.valueOf(50000)))
            .termMonths(60)
            .purpose("HOME_IMPROVEMENT")
            .build();
        
        RiskAssessment assessment = RiskAssessment.low();
        
        when(customerRepository.findById(customerId))
            .thenReturn(Optional.of(customer));
        when(riskAssessmentService.assess(customer, command))
            .thenReturn(assessment);
        when(loanRepository.save(any(Loan.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        Loan loan = loanApplicationService.createLoan(command);
        
        // Then
        assertAll(
            () -> assertNotNull(loan),
            () -> assertEquals(customerId, loan.getCustomerId()),
            () -> assertEquals(command.getPrincipalAmount(), loan.getPrincipalAmount()),
            () -> assertEquals(LoanStatus.PENDING_APPROVAL, loan.getStatus())
        );
        
        verify(eventPublisher).publish(any(LoanApplicationSubmittedEvent.class));
    }
    
    @Test
    @DisplayName("Should reject high risk loan application")
    void shouldRejectHighRiskLoanApplication() {
        // Given
        Long customerId = 123L;
        Customer customer = CustomerFixture.activeCustomer().build();
        CreateLoanCommand command = createLoanCommand(customerId);
        
        RiskAssessment assessment = RiskAssessment.high("Poor credit history");
        
        when(customerRepository.findById(customerId))
            .thenReturn(Optional.of(customer));
        when(riskAssessmentService.assess(customer, command))
            .thenReturn(assessment);
        
        // When/Then
        LoanApplicationRejectedException exception = assertThrows(
            LoanApplicationRejectedException.class,
            () -> loanApplicationService.createLoan(command)
        );
        
        assertEquals("Poor credit history", exception.getReason());
        verify(loanRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }
}
```

## Integration Testing

### Repository Testing

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@DisplayName("Customer Repository Integration Tests")
class CustomerRepositoryIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Test
    @DisplayName("Should save and retrieve customer")
    void shouldSaveAndRetrieveCustomer() {
        // Given
        Customer customer = CustomerFixture.activeCustomer().build();
        
        // When
        Customer savedCustomer = customerRepository.save(customer);
        entityManager.flush();
        entityManager.clear();
        
        // Then
        Optional<Customer> retrievedCustomer = customerRepository.findById(savedCustomer.getId());
        
        assertTrue(retrievedCustomer.isPresent());
        assertEquals(savedCustomer.getEmail(), retrievedCustomer.get().getEmail());
    }
    
    @Test
    @DisplayName("Should find customers by status")
    void shouldFindCustomersByStatus() {
        // Given
        Customer activeCustomer1 = CustomerFixture.activeCustomer()
            .withEmail("active1@example.com")
            .build();
        Customer activeCustomer2 = CustomerFixture.activeCustomer()
            .withEmail("active2@example.com")
            .build();
        Customer pendingCustomer = CustomerFixture.pendingCustomer()
            .withEmail("pending@example.com")
            .build();
        
        customerRepository.saveAll(List.of(activeCustomer1, activeCustomer2, pendingCustomer));
        entityManager.flush();
        
        // When
        List<Customer> activeCustomers = customerRepository.findByStatus(CustomerStatus.ACTIVE);
        
        // Then
        assertEquals(2, activeCustomers.size());
        assertTrue(activeCustomers.stream()
            .allMatch(c -> c.getStatus() == CustomerStatus.ACTIVE));
    }
    
    @Test
    @DisplayName("Should handle concurrent updates with optimistic locking")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldHandleConcurrentUpdatesWithOptimisticLocking() {
        // Given
        Customer customer = customerRepository.save(
            CustomerFixture.activeCustomer().build()
        );
        
        // When - Simulate concurrent updates
        CompletableFuture<Void> update1 = CompletableFuture.runAsync(() -> {
            transactionTemplate.execute(status -> {
                Customer c1 = customerRepository.findById(customer.getId()).orElseThrow();
                c1.updateMonthlyIncome(Money.of("USD", BigDecimal.valueOf(6000)));
                customerRepository.save(c1);
                return null;
            });
        });
        
        CompletableFuture<Void> update2 = CompletableFuture.runAsync(() -> {
            transactionTemplate.execute(status -> {
                Customer c2 = customerRepository.findById(customer.getId()).orElseThrow();
                c2.updateMonthlyIncome(Money.of("USD", BigDecimal.valueOf(7000)));
                customerRepository.save(c2);
                return null;
            });
        });
        
        // Then
        assertThrows(CompletionException.class, 
            () -> CompletableFuture.allOf(update1, update2).join());
    }
}
```

### REST API Testing

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DisplayName("Customer API Integration Tests")
class CustomerApiIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private CustomerService customerService;
    
    @Test
    @DisplayName("Should create customer via API")
    void shouldCreateCustomerViaApi() throws Exception {
        // Given
        CreateCustomerRequest request = CreateCustomerRequest.builder()
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .phone("+1-555-123-4567")
            .dateOfBirth(LocalDate.of(1990, 1, 15))
            .monthlyIncome(BigDecimal.valueOf(5000))
            .build();
        
        Customer customer = CustomerFixture.activeCustomer()
            .withId(123L)
            .build();
        
        when(customerService.createCustomer(any())).thenReturn(customer);
        
        // When & Then
        mockMvc.perform(post("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/customers/123"))
            .andExpect(jsonPath("$.id").value(123))
            .andExpect(jsonPath("$.firstName").value("John"))
            .andExpect(jsonPath("$.lastName").value("Doe"))
            .andExpect(jsonPath("$.email").value("john.doe@example.com"))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
    
    @Test
    @DisplayName("Should validate customer creation request")
    void shouldValidateCustomerCreationRequest() throws Exception {
        // Given
        CreateCustomerRequest request = CreateCustomerRequest.builder()
            .firstName("") // Invalid: empty
            .lastName("Doe")
            .email("invalid-email") // Invalid: format
            .phone("+1-555-123-4567")
            .dateOfBirth(LocalDate.now()) // Invalid: too young
            .monthlyIncome(BigDecimal.valueOf(-1000)) // Invalid: negative
            .build();
        
        // When & Then
        mockMvc.perform(post("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.error.details").isArray())
            .andExpect(jsonPath("$.error.details[?(@.field=='firstName')]").exists())
            .andExpect(jsonPath("$.error.details[?(@.field=='email')]").exists())
            .andExpect(jsonPath("$.error.details[?(@.field=='dateOfBirth')]").exists())
            .andExpect(jsonPath("$.error.details[?(@.field=='monthlyIncome')]").exists());
    }
}
```

## Architecture Testing

### Hexagonal Architecture Tests

```java
@AnalyzeClasses(packages = "com.loanmanagement", 
    importOptions = ImportOption.DoNotIncludeTests.class)
@DisplayName("Hexagonal Architecture Tests")
class HexagonalArchitectureTest {
    
    @ArchTest
    static final ArchRule domainShouldNotDependOnInfrastructure =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..infrastructure..", "..adapter..", "..spring..");
    
    @ArchTest
    static final ArchRule domainShouldNotDependOnApplication =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..application..")
            .because("Domain should be independent of application layer");
    
    @ArchTest
    static final ArchRule portsShouldBeInterfaces =
        classes()
            .that().resideInAPackage("..port..")
            .and().areNotInnerClasses()
            .should().beInterfaces()
            .because("Ports should be interfaces to ensure proper abstraction");
    
    @ArchTest
    static final ArchRule adaptersShouldImplementPorts =
        classes()
            .that().resideInAPackage("..adapter..")
            .and().areAnnotatedWith(Component.class)
            .should().implement(JavaClass.Predicates.resideInAPackage("..port.."))
            .because("Adapters should implement port interfaces");
    
    @ArchTest
    static final ArchRule applicationServicesShouldUseTransactional =
        methods()
            .that().areDeclaredInClassesThat()
            .resideInAPackage("..application.service..")
            .and().arePublic()
            .and().doNotHaveName("toString")
            .should().beAnnotatedWith(Transactional.class)
            .because("Application services should manage transactions");
}
```

### Package Dependencies Test

```java
@DisplayName("Package Dependency Tests")
class PackageDependencyTest {
    
    private static final String BASE_PACKAGE = "com.loanmanagement";
    
    @Test
    @DisplayName("Should enforce layered architecture")
    void shouldEnforceLayeredArchitecture() {
        JavaClasses classes = new ClassFileImporter()
            .importPackages(BASE_PACKAGE);
        
        Architectures.LayeredArchitecture layeredArchitecture = layeredArchitecture()
            .layer("Domain").definedBy("..domain..")
            .layer("Application").definedBy("..application..")
            .layer("Infrastructure").definedBy("..infrastructure..")
            .layer("Presentation").definedBy("..adapter.in.web..")
            
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure")
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure", "Presentation")
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("Presentation").mayNotBeAccessedByAnyLayer();
        
        layeredArchitecture.check(classes);
    }
    
    @Test
    @DisplayName("Should follow onion architecture")
    void shouldFollowOnionArchitecture() {
        JavaClasses classes = new ClassFileImporter()
            .importPackages(BASE_PACKAGE);
        
        onionArchitecture()
            .domainModels("..domain.model..")
            .domainServices("..domain.service..")
            .applicationServices("..application..")
            .adapter("web", "..adapter.in.web..")
            .adapter("persistence", "..adapter.out.persistence..")
            .adapter("messaging", "..adapter.out.messaging..")
            .check(classes);
    }
}
```

## Performance Testing

### Load Testing

```java
@SpringBootTest
@ActiveProfiles("performance")
@DisplayName("Loan Management Load Tests")
class LoanManagementLoadTest {
    
    @Autowired
    private PerformanceMetricsCollector metricsCollector;
    
    @Test
    @DisplayName("Should handle concurrent loan applications")
    void shouldHandleConcurrentLoanApplications() throws InterruptedException {
        // Given
        int concurrentUsers = 100;
        int operationsPerUser = 10;
        CountDownLatch latch = new CountDownLatch(concurrentUsers);
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        
        metricsCollector.startCollection();
        
        // When
        for (int i = 0; i < concurrentUsers; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerUser; j++) {
                        long startTime = System.currentTimeMillis();
                        
                        // Simulate loan application
                        CreateLoanCommand command = generateLoanCommand(userId, j);
                        loanService.createLoan(command);
                        
                        long responseTime = System.currentTimeMillis() - startTime;
                        metricsCollector.recordOperation("CreateLoan", responseTime, true);
                    }
                } catch (Exception e) {
                    metricsCollector.recordOperation("CreateLoan", 0, false);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Then
        assertTrue(latch.await(120, TimeUnit.SECONDS));
        metricsCollector.stopCollection();
        
        PerformanceReport report = metricsCollector.generateReport();
        
        assertAll(
            () -> assertTrue(report.getAverageResponseTime() < 1000, 
                "Average response time should be under 1 second"),
            () -> assertTrue(report.getThroughput() > 50, 
                "Throughput should be above 50 req/sec"),
            () -> assertTrue(report.getErrorRate() < 5.0, 
                "Error rate should be below 5%")
        );
    }
}
```

### Database Performance Testing

```java
@DataJpaTest
@DisplayName("Database Performance Tests")
class DatabasePerformanceTest {
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Test
    @DisplayName("Should efficiently query large datasets")
    void shouldEfficientlyQueryLargeDatasets() {
        // Given - Insert test data
        List<Customer> customers = IntStream.range(0, 10000)
            .mapToObj(i -> CustomerFixture.activeCustomer()
                .withEmail("customer" + i + "@example.com")
                .build())
            .collect(Collectors.toList());
        
        customerRepository.saveAll(customers);
        
        // When - Measure query performance
        long startTime = System.currentTimeMillis();
        Page<Customer> page = customerRepository.findByStatus(
            CustomerStatus.ACTIVE, 
            PageRequest.of(0, 100)
        );
        long queryTime = System.currentTimeMillis() - startTime;
        
        // Then
        assertEquals(100, page.getContent().size());
        assertTrue(queryTime < 100, "Query should complete within 100ms");
    }
    
    @Test
    @DisplayName("Should use indexes effectively")
    @Sql("/sql/analyze-query-plan.sql")
    void shouldUseIndexesEffectively() {
        // Verify that queries use indexes by checking execution plans
        // This test would analyze PostgreSQL query plans
    }
}
```

## Security Testing

### Authentication Testing

```java
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Security Tests")
class SecurityTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    @DisplayName("Should reject unauthenticated requests")
    void shouldRejectUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/v1/customers"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    @DisplayName("Should accept authenticated requests")
    @WithMockUser(roles = "USER")
    void shouldAcceptAuthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/v1/customers"))
            .andExpect(status().isOk());
    }
    
    @Test
    @DisplayName("Should enforce role-based access control")
    void shouldEnforceRoleBasedAccessControl() throws Exception {
        // User without ADMIN role
        String userToken = generateToken("user", "USER");
        
        mockMvc.perform(post("/api/v1/admin/users")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden());
        
        // User with ADMIN role
        String adminToken = generateToken("admin", "ADMIN");
        
        mockMvc.perform(post("/api/v1/admin/users")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());
    }
    
    @Test
    @DisplayName("Should validate JWT tokens")
    void shouldValidateJwtTokens() throws Exception {
        // Invalid token
        mockMvc.perform(get("/api/v1/customers")
                .header("Authorization", "Bearer invalid.token.here"))
            .andExpect(status().isUnauthorized());
        
        // Expired token
        String expiredToken = generateExpiredToken();
        
        mockMvc.perform(get("/api/v1/customers")
                .header("Authorization", "Bearer " + expiredToken))
            .andExpect(status().isUnauthorized());
    }
}
```

### SQL Injection Testing

```java
@SpringBootTest
@DisplayName("SQL Injection Tests")
class SqlInjectionTest {
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Test
    @DisplayName("Should prevent SQL injection in queries")
    void shouldPreventSqlInjection() {
        // Attempt SQL injection
        String maliciousInput = "'; DROP TABLE customers; --";
        
        // This should safely handle the malicious input
        List<Customer> customers = customerRepository.findByEmail(maliciousInput);
        
        // Verify no damage was done
        assertTrue(customers.isEmpty());
        assertDoesNotThrow(() -> customerRepository.count());
    }
}
```

## End-to-End Testing

### Complete Workflow Testing

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("End-to-End Loan Application Workflow")
class LoanApplicationE2ETest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    private static Long customerId;
    private static Long loanId;
    private static String authToken;
    
    @BeforeAll
    static void setUp() {
        // Obtain authentication token
        authToken = authenticateUser();
    }
    
    @Test
    @Order(1)
    @DisplayName("Step 1: Create customer")
    void createCustomer() {
        // Given
        CreateCustomerRequest request = CreateCustomerRequest.builder()
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@e2etest.com")
            .phone("+1-555-999-8888")
            .dateOfBirth(LocalDate.of(1985, 5, 15))
            .monthlyIncome(BigDecimal.valueOf(8000))
            .build();
        
        // When
        ResponseEntity<CustomerResponse> response = restTemplate
            .withBasicAuth("user", "password")
            .postForEntity("/api/v1/customers", request, CustomerResponse.class);
        
        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        customerId = response.getBody().getId();
    }
    
    @Test
    @Order(2)
    @DisplayName("Step 2: Verify customer KYC")
    void verifyCustomerKyc() {
        // When
        ResponseEntity<Void> response = restTemplate
            .withBasicAuth("admin", "password")
            .postForEntity("/api/v1/customers/" + customerId + "/kyc/verify", 
                null, Void.class);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
    
    @Test
    @Order(3)
    @DisplayName("Step 3: Submit loan application")
    void submitLoanApplication() {
        // Given
        CreateLoanRequest request = CreateLoanRequest.builder()
            .customerId(customerId)
            .principalAmount(BigDecimal.valueOf(50000))
            .termMonths(60)
            .purpose("HOME_IMPROVEMENT")
            .build();
        
        // When
        ResponseEntity<LoanResponse> response = restTemplate
            .withBasicAuth("user", "password")
            .postForEntity("/api/v1/loans", request, LoanResponse.class);
        
        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        loanId = response.getBody().getId();
        assertEquals("PENDING_APPROVAL", response.getBody().getStatus());
    }
    
    @Test
    @Order(4)
    @DisplayName("Step 4: Approve loan")
    void approveLoan() {
        // Given
        ApproveLoanRequest request = new ApproveLoanRequest();
        request.setApprovedBy("loan_officer_1");
        request.setNotes("All criteria met");
        
        // When
        ResponseEntity<Void> response = restTemplate
            .withBasicAuth("loan_officer", "password")
            .postForEntity("/api/v1/loans/" + loanId + "/approve", 
                request, Void.class);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
    
    @Test
    @Order(5)
    @DisplayName("Step 5: Disburse loan")
    void disburseLoan() {
        // When
        ResponseEntity<Void> response = restTemplate
            .withBasicAuth("admin", "password")
            .postForEntity("/api/v1/loans/" + loanId + "/disburse", 
                null, Void.class);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        // Verify loan status
        ResponseEntity<LoanResponse> loanResponse = restTemplate
            .withBasicAuth("user", "password")
            .getForEntity("/api/v1/loans/" + loanId, LoanResponse.class);
        
        assertEquals("ACTIVE", loanResponse.getBody().getStatus());
    }
    
    @Test
    @Order(6)
    @DisplayName("Step 6: Make payment")
    void makePayment() {
        // Given
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
            .loanId(loanId)
            .amount(BigDecimal.valueOf(1000))
            .paymentMethod("BANK_TRANSFER")
            .reference("PAY123456")
            .build();
        
        // When
        ResponseEntity<PaymentResponse> response = restTemplate
            .withBasicAuth("user", "password")
            .postForEntity("/api/v1/payments", request, PaymentResponse.class);
        
        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("COMPLETED", response.getBody().getStatus());
    }
}
```

## Test Data Management

### Test Fixtures

```java
public class CustomerFixture {
    
    private Long id;
    private String firstName = "John";
    private String lastName = "Doe";
    private String email = "john.doe@example.com";
    private String phone = "+1-555-123-4567";
    private LocalDate dateOfBirth = LocalDate.of(1990, 1, 15);
    private BigDecimal monthlyIncome = BigDecimal.valueOf(5000);
    private CustomerStatus status = CustomerStatus.ACTIVE;
    
    public static CustomerFixture activeCustomer() {
        return new CustomerFixture().withStatus(CustomerStatus.ACTIVE);
    }
    
    public static CustomerFixture pendingCustomer() {
        return new CustomerFixture().withStatus(CustomerStatus.PENDING);
    }
    
    public CustomerFixture withId(Long id) {
        this.id = id;
        return this;
    }
    
    public CustomerFixture withEmail(String email) {
        this.email = email;
        return this;
    }
    
    public CustomerFixture withStatus(CustomerStatus status) {
        this.status = status;
        return this;
    }
    
    public Customer build() {
        Customer customer = new Customer(
            new PersonalName(firstName, lastName),
            new EmailAddress(email),
            new PhoneNumber(phone),
            dateOfBirth
        );
        
        if (id != null) {
            ReflectionTestUtils.setField(customer, "id", new CustomerId(id));
        }
        
        if (status != CustomerStatus.PENDING) {
            ReflectionTestUtils.setField(customer, "status", status);
        }
        
        return customer;
    }
}
```

### Test Data Builders

```java
@Component
@Profile("test")
public class TestDataBuilder {
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private LoanRepository loanRepository;
    
    public Customer createCustomer() {
        return createCustomer("test" + UUID.randomUUID() + "@example.com");
    }
    
    public Customer createCustomer(String email) {
        Customer customer = CustomerFixture.activeCustomer()
            .withEmail(email)
            .build();
        return customerRepository.save(customer);
    }
    
    public Loan createLoan(Customer customer) {
        return createLoan(customer, BigDecimal.valueOf(10000), 36);
    }
    
    public Loan createLoan(Customer customer, BigDecimal amount, int termMonths) {
        Loan loan = Loan.builder()
            .customerId(customer.getId())
            .principalAmount(Money.of("USD", amount))
            .interestRate(BigDecimal.valueOf(5.5))
            .termMonths(termMonths)
            .status(LoanStatus.ACTIVE)
            .build();
        return loanRepository.save(loan);
    }
    
    @Transactional
    public void setupCompleteScenario() {
        // Create multiple customers with different statuses
        List<Customer> customers = IntStream.range(0, 10)
            .mapToObj(i -> createCustomer("scenario" + i + "@example.com"))
            .collect(Collectors.toList());
        
        // Create loans for some customers
        customers.stream()
            .limit(5)
            .forEach(customer -> {
                Loan loan = createLoan(customer);
                // Create payment history
                IntStream.range(0, 3).forEach(i -> 
                    createPayment(loan, BigDecimal.valueOf(500))
                );
            });
    }
}
```

## Continuous Testing

### Test Pipeline Configuration

```yaml
# .github/workflows/test.yml
name: Test Pipeline

on: [push, pull_request]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run Unit Tests
        run: ./mvnw test -Dgroups=unit
      - name: Upload Coverage
        uses: codecov/codecov-action@v3
        with:
          file: ./target/site/jacoco/jacoco.xml

  integration-tests:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:14
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
        run: ./mvnw test -Dgroups=integration

  performance-tests:
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v3
      - name: Run Performance Tests
        run: ./mvnw test -Dgroups=performance
      - name: Store Performance Results
        uses: actions/upload-artifact@v3
        with:
          name: performance-results
          path: target/performance-reports/
```

### Test Coverage Requirements

```xml
<!-- pom.xml -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <configuration>
        <rules>
            <rule>
                <element>BUNDLE</element>
                <limits>
                    <limit>
                        <counter>LINE</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.80</minimum>
                    </limit>
                    <limit>
                        <counter>BRANCH</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.75</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</plugin>
```

## Best Practices

### Test Organization
1. **One assertion per test method** - Keep tests focused
2. **Descriptive test names** - Use @DisplayName
3. **Arrange-Act-Assert pattern** - Clear test structure
4. **Test data builders** - Reusable test data creation
5. **Isolated tests** - No dependencies between tests

### Performance
1. **Parallel test execution** - Speed up test runs
2. **Test containers** - Realistic integration tests
3. **In-memory databases** - Fast unit tests
4. **Selective test runs** - Group tests by category

### Maintenance
1. **Regular test review** - Remove obsolete tests
2. **Test refactoring** - Keep tests clean
3. **Failure analysis** - Fix flaky tests immediately
4. **Documentation** - Document complex test scenarios

## Conclusion

This comprehensive testing guide ensures high-quality software delivery through systematic testing practices. By following these guidelines, the team can maintain confidence in the system's reliability, performance, and security.