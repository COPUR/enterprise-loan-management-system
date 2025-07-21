# Open Finance System - Knowledge Transfer Documentation

## 📋 Table of Contents
1. [System Overview](#system-overview)
2. [Architecture Deep Dive](#architecture-deep-dive)
3. [Development Guidelines](#development-guidelines)
4. [Feature Development Process](#feature-development-process)
5. [Compliance Requirements](#compliance-requirements)
6. [Testing Strategy](#testing-strategy)
7. [Deployment Procedures](#deployment-procedures)
8. [Monitoring and Operations](#monitoring-and-operations)
9. [Troubleshooting Guide](#troubleshooting-guide)
10. [Team Onboarding](#team-onboarding)

## System Overview

### 🎯 Business Purpose
The Open Finance Enterprise Loan Management System enables UAE banks to comply with CBUAE regulation C7/2023 by providing secure, standardized APIs for sharing customer financial data across authorized participants.

### 🏗️ High-Level Architecture
```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Participants  │────│  Open Finance    │────│   Customer      │
│   (Banks/TPPs)  │    │     Gateway      │    │   Applications  │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │
                       ┌────────┴────────┐
                       │                 │
            ┌──────────▼──────────┐     ┌▼──────────────┐
            │   Consent Engine    │     │  Data Services │
            │  (Keycloak/OAuth)   │     │   (Sagra/APIs) │
            └─────────────────────┘     └────────────────┘
                                │
                    ┌───────────▼───────────┐
                    │    Platform Services   │
                    │  • Enterprise Loans   │
                    │  • AmanahFi (Islamic) │
                    │  • Masrufi (Expense)  │
                    └───────────────────────┘
```

### 🔧 Core Technologies
- **Backend**: Spring Boot 3.2, Java 21
- **Security**: Keycloak, OAuth 2.1, FAPI 2.0
- **Data**: PostgreSQL, MongoDB, Redis
- **Messaging**: Apache Kafka
- **Monitoring**: Prometheus, Grafana
- **Infrastructure**: Docker, Kubernetes, AWS

## Architecture Deep Dive

### 🏛️ Clean Architecture Implementation

#### Domain Layer (Core Business Logic)
```java
// Example domain entity
@Entity
@AggregateRoot
public class Consent extends AggregateRoot<ConsentId> {
    private final ParticipantId participantId;
    private final CustomerId customerId;
    private final Set<ConsentScope> scopes;
    private ConsentStatus status;
    private Instant expiresAt;
    
    // Business methods enforce domain rules
    public void authorize() {
        if (isExpired()) {
            throw new ConsentExpiredException(getId());
        }
        this.status = ConsentStatus.AUTHORIZED;
        recordDomainEvent(new ConsentAuthorizedEvent(this));
    }
}
```

#### Application Layer (Use Cases)
```java
@UseCase
@Transactional
public class AuthorizeConsentUseCase {
    
    public CompletableFuture<ConsentAuthorizationResult> execute(
            ConsentAuthorizationRequest request) {
        
        // 1. Load consent aggregate
        var consent = consentRepository.findById(request.getConsentId());
        
        // 2. Apply business rules
        consent.authorize();
        
        // 3. Persist changes
        consentRepository.save(consent);
        
        // 4. Trigger saga for cross-platform updates
        return consentAuthorizationSaga.orchestrate(request);
    }
}
```

#### Infrastructure Layer (Technical Implementation)
```java
@RestController
@RequestMapping("/open-finance/v1/consents")
public class ConsentController {
    
    @PostMapping("/{consentId}/authorize")
    @PreAuthorize("hasScope('CONSENT_MANAGEMENT')")
    public CompletableFuture<ResponseEntity<ConsentResponse>> authorize(
            @PathVariable String consentId,
            @RequestHeader("DPoP") String dpopProof) {
        
        return authorizeConsentUseCase.execute(
            ConsentAuthorizationRequest.of(consentId, dpopProof)
        ).thenApply(result -> ResponseEntity.ok(
            ConsentResponse.from(result)
        ));
    }
}
```

### 🎯 Bounded Contexts

#### 1. Open Finance Context
**Responsibility**: FAPI 2.0 compliant APIs, consent management
**Key Components**:
- `ConsentAggregate` - Consent lifecycle management
- `ParticipantAggregate` - Bank/TPP registration
- `DataSharingRequestSaga` - Cross-platform orchestration

#### 2. Enterprise Loans Context  
**Responsibility**: Traditional banking products
**Key Components**:
- `LoanAggregate` - Loan lifecycle
- `CreditAssessmentService` - Risk evaluation
- `PaymentScheduleService` - Repayment management

#### 3. AmanahFi Context (Islamic Finance)
**Responsibility**: Sharia-compliant financial products
**Key Components**:
- `IslamicProductAggregate` - Murabaha, Ijarah, etc.
- `ShariaComplianceService` - Religious validation
- `ProfitSharingCalculator` - Islamic profit calculations

### 🔄 Event-Driven Architecture

#### Event Sourcing Implementation
```java
@EventSourcedAggregate
public class ConsentAggregate {
    
    private final EventStore eventStore;
    
    public void handle(CreateConsentCommand command) {
        var event = new ConsentCreatedEvent(
            command.getConsentId(),
            command.getParticipantId(),
            command.getCustomerId(),
            command.getScopes()
        );
        
        // Persist event
        eventStore.append(command.getConsentId(), event);
        
        // Update read model
        eventBus.publish(event);
    }
}
```

#### Saga Pattern for Distributed Transactions
```java
@Saga
public class ConsentAuthorizationSaga {
    
    @SagaOrchestrationStart
    public void handle(ConsentAuthorizationRequest request) {
        // Step 1: Validate participant
        sagaManager.choreography()
            .step("validate-participant")
            .invoke(() -> participantService.validate(request.getParticipantId()))
            .onSuccess(() -> handle(new ParticipantValidatedEvent(request)))
            .onFailure(() -> handle(new ParticipantValidationFailedEvent(request)))
            .execute();
    }
}
```

## Development Guidelines

### 🎨 Code Style and Standards

#### 1. Naming Conventions
```java
// Classes: PascalCase with descriptive names
public class ConsentAuthorizationService {}

// Methods: camelCase describing action
public CompletableFuture<ConsentResult> authorizeConsent() {}

// Constants: UPPER_SNAKE_CASE
public static final String FAPI_VERSION = "2.0";

// Variables: camelCase, descriptive
private final ConsentRepository consentRepository;
```

#### 2. Package Structure
```
com.enterprise.openfinance
├── domain/
│   ├── model/           # Aggregates, entities, value objects
│   ├── service/         # Domain services
│   └── event/           # Domain events
├── application/
│   ├── usecase/         # Application use cases
│   ├── saga/            # Saga orchestrators
│   └── handler/         # Event handlers
└── infrastructure/
    ├── adapter/         # Port implementations
    ├── config/          # Configuration
    └── monitoring/      # Observability
```

#### 3. Error Handling Strategy
```java
// Custom exceptions for domain violations
public class ConsentExpiredException extends DomainException {
    public ConsentExpiredException(ConsentId consentId) {
        super("CONSENT_EXPIRED", 
              String.format("Consent %s has expired", consentId.getValue()));
    }
}

// Global exception handler
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(
            DomainException ex) {
        return ResponseEntity.badRequest()
            .body(ErrorResponse.of(ex.getErrorCode(), ex.getMessage()));
    }
}
```

### 🔐 Security Implementation Patterns

#### 1. FAPI 2.0 Security Validation
```java
@Component
public class FAPI2SecurityValidator {
    
    public SecurityValidationResult validateRequest(
            String dpopProof, String requestSignature) {
        
        // 1. Validate DPoP token structure and signature
        var dpopValidation = validateDPoPToken(dpopProof);
        if (!dpopValidation.isValid()) {
            return SecurityValidationResult.invalid("Invalid DPoP token");
        }
        
        // 2. Validate request signature for non-repudiation
        var signatureValidation = validateRequestSignature(requestSignature);
        if (!signatureValidation.isValid()) {
            return SecurityValidationResult.invalid("Invalid request signature");
        }
        
        return SecurityValidationResult.valid();
    }
}
```

#### 2. Consent Validation
```java
@Service
public class ConsentValidationService {
    
    public ConsentValidationResult validateForAccountAccess(
            ConsentId consentId, 
            ParticipantId participantId,
            Set<ConsentScope> requiredScopes) {
        
        var consent = consentRepository.findByIdAndParticipant(
            consentId, participantId
        ).orElse(null);
        
        if (consent == null) {
            return ConsentValidationResult.invalid("CONSENT_NOT_FOUND");
        }
        
        if (consent.isExpired()) {
            return ConsentValidationResult.invalid("CONSENT_EXPIRED");
        }
        
        if (!consent.hasScopes(requiredScopes)) {
            return ConsentValidationResult.invalid("INSUFFICIENT_SCOPE");
        }
        
        return ConsentValidationResult.valid();
    }
}
```

### 📊 Monitoring and Observability

#### 1. Metrics Collection
```java
@Component
public class ConsentMetrics {
    
    private final Counter consentCreations;
    private final Timer consentValidationDuration;
    private final Gauge activeConsents;
    
    public ConsentMetrics(MeterRegistry meterRegistry) {
        this.consentCreations = Counter.builder("consent_creations_total")
            .description("Total consent creations")
            .register(meterRegistry);
            
        this.consentValidationDuration = Timer.builder("consent_validation_duration")
            .description("Time to validate consent")
            .register(meterRegistry);
    }
    
    public void recordConsentCreation(ParticipantId participantId) {
        consentCreations.increment(
            Tags.of("participant", participantId.getValue())
        );
    }
}
```

#### 2. Structured Logging
```java
@Slf4j
@Component
public class ConsentService {
    
    public void authorizeConsent(ConsentId consentId) {
        log.info("Authorizing consent",
            kv("consentId", consentId.getValue()),
            kv("action", "consent_authorization"),
            kv("timestamp", Instant.now())
        );
        
        try {
            // Business logic
            log.info("Consent authorized successfully",
                kv("consentId", consentId.getValue()),
                kv("status", "success")
            );
        } catch (Exception e) {
            log.error("Consent authorization failed",
                kv("consentId", consentId.getValue()),
                kv("error", e.getMessage()),
                e
            );
            throw e;
        }
    }
}
```

## Feature Development Process

### 🚀 Adding New Features - Step-by-Step Guide

#### Phase 1: Analysis and Design

##### 1. Business Requirements Analysis
```markdown
# Feature Request Template

## Business Context
- **Feature Name**: Account Balance History API
- **Business Value**: Enable participants to access historical balance data
- **Compliance Impact**: Must comply with CBUAE data retention policies

## Functional Requirements
- [ ] Retrieve balance history for specific date range
- [ ] Support pagination for large datasets
- [ ] Filter by account type and currency
- [ ] Include metadata about data freshness

## Non-Functional Requirements  
- [ ] Response time: < 500ms for 90th percentile
- [ ] Support up to 1000 concurrent requests
- [ ] Maintain 99.9% availability
- [ ] Ensure PCI-DSS compliance for sensitive data
```

##### 2. Domain Modeling
```java
// Step 1: Define value objects
@ValueObject
public record AccountBalanceHistoryId(String value) {
    public static AccountBalanceHistoryId generate() {
        return new AccountBalanceHistoryId("BAL-HIST-" + UUID.randomUUID());
    }
}

// Step 2: Define entities
@Entity
public class AccountBalanceHistory extends Entity<AccountBalanceHistoryId> {
    private final AccountId accountId;
    private final Money balance;
    private final Instant recordedAt;
    private final BalanceType type;
    
    // Domain methods
    public boolean isWithinDateRange(DateRange range) {
        return range.contains(recordedAt);
    }
}

// Step 3: Define aggregates if needed
@AggregateRoot
public class AccountHistory extends AggregateRoot<AccountId> {
    private final List<AccountBalanceHistory> history;
    
    public List<AccountBalanceHistory> getHistoryForPeriod(
            DateRange period, 
            Pageable pageable) {
        return history.stream()
            .filter(h -> h.isWithinDateRange(period))
            .sorted(Comparator.comparing(h -> h.getRecordedAt()))
            .skip(pageable.getOffset())
            .limit(pageable.getPageSize())
            .toList();
    }
}
```

##### 3. API Design
```yaml
# OpenAPI specification
/open-finance/v1/accounts/{accountId}/balance-history:
  get:
    summary: Get account balance history
    parameters:
      - name: accountId
        in: path
        required: true
        schema:
          type: string
          pattern: '^ACC-[A-Z0-9]{8,12}$'
      - name: from-date
        in: query
        schema:
          type: string
          format: date
      - name: to-date
        in: query
        schema:
          type: string
          format: date
      - name: page
        in: query
        schema:
          type: integer
          minimum: 0
      - name: size
        in: query
        schema:
          type: integer
          minimum: 1
          maximum: 100
    responses:
      '200':
        description: Balance history retrieved successfully
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/BalanceHistoryResponse'
```

#### Phase 2: Implementation

##### 1. Test-Driven Development (TDD)
```java
// Red: Write failing test first
@Test
@DisplayName("Should retrieve balance history for valid date range")
void should_retrieve_balance_history_for_valid_date_range() {
    // Given
    var accountId = AccountId.of("ACC-12345678");
    var fromDate = LocalDate.now().minusDays(30);
    var toDate = LocalDate.now();
    var pageable = PageRequest.of(0, 10);
    
    // When
    var result = accountHistoryService.getBalanceHistory(
        accountId, fromDate, toDate, pageable
    );
    
    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(10);
    assertThat(result.getContent().get(0).getRecordedAt())
        .isAfter(fromDate.atStartOfDay().toInstant(ZoneOffset.UTC));
}

// Green: Implement minimum code to pass
@Service
public class AccountHistoryService {
    
    public Page<AccountBalanceHistory> getBalanceHistory(
            AccountId accountId,
            LocalDate fromDate, 
            LocalDate toDate,
            Pageable pageable) {
        
        var dateRange = DateRange.between(fromDate, toDate);
        var account = accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException(accountId));
            
        return account.getHistoryForPeriod(dateRange, pageable);
    }
}
```

##### 2. Domain Implementation
```java
// Domain service for complex business rules
@DomainService
public class BalanceHistoryDomainService {
    
    public BalanceHistoryAnalysis analyzeBalancePatterns(
            List<AccountBalanceHistory> history) {
        
        // Complex domain logic
        var trends = calculateTrends(history);
        var volatility = calculateVolatility(history);
        var averageBalance = calculateAverage(history);
        
        return BalanceHistoryAnalysis.builder()
            .trends(trends)
            .volatility(volatility)
            .averageBalance(averageBalance)
            .build();
    }
}
```

##### 3. Application Layer
```java
// Use case orchestrates domain objects
@UseCase
@Transactional(readOnly = true)
public class GetBalanceHistoryUseCase {
    
    private final AccountRepository accountRepository;
    private final ConsentValidationService consentValidator;
    private final BalanceHistoryDomainService domainService;
    private final PrometheusMetricsCollector metricsCollector;
    
    public CompletableFuture<BalanceHistoryResult> execute(
            GetBalanceHistoryRequest request) {
        
        var timer = metricsCollector.startTimer("balance_history_retrieval");
        
        return CompletableFuture
            .supplyAsync(() -> validateRequest(request))
            .thenCompose(this::retrieveBalanceHistory)
            .thenApply(this::enrichWithAnalysis)
            .whenComplete((result, error) -> {
                timer.stop();
                if (error != null) {
                    metricsCollector.incrementErrorCounter("balance_history_error");
                }
            });
    }
    
    private ValidationResult validateRequest(GetBalanceHistoryRequest request) {
        // Validate consent
        var consentValidation = consentValidator.validateForAccountAccess(
            request.getConsentId(),
            request.getParticipantId(),
            Set.of(ConsentScope.ACCOUNT_INFORMATION)
        );
        
        if (!consentValidation.isValid()) {
            throw new InvalidConsentException(consentValidation.getViolations());
        }
        
        return ValidationResult.valid();
    }
}
```

##### 4. Infrastructure Layer
```java
// REST controller
@RestController
@RequestMapping("/open-finance/v1/accounts")
@Validated
public class AccountHistoryController {
    
    private final GetBalanceHistoryUseCase useCase;
    
    @GetMapping("/{accountId}/balance-history")
    @PreAuthorize("hasScope('ACCOUNT_INFORMATION')")
    public CompletableFuture<ResponseEntity<BalanceHistoryResponse>> getBalanceHistory(
            @PathVariable @Valid @Pattern(regexp = "^ACC-[A-Z0-9]{8,12}$") 
            String accountId,
            
            @RequestParam("from-date") @DateTimeFormat(iso = ISO.DATE) 
            LocalDate fromDate,
            
            @RequestParam("to-date") @DateTimeFormat(iso = ISO.DATE) 
            LocalDate toDate,
            
            @RequestParam(defaultValue = "0") @Min(0) 
            int page,
            
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) 
            int size,
            
            @RequestHeader("X-Consent-Id") String consentId,
            @RequestHeader("X-Participant-Id") String participantId,
            @RequestHeader("DPoP") String dpopProof) {
        
        var request = GetBalanceHistoryRequest.builder()
            .accountId(AccountId.of(accountId))
            .fromDate(fromDate)
            .toDate(toDate)
            .pageable(PageRequest.of(page, size))
            .consentId(ConsentId.of(consentId))
            .participantId(ParticipantId.of(participantId))
            .dpopProof(dpopProof)
            .build();
            
        return useCase.execute(request)
            .thenApply(result -> ResponseEntity.ok(
                BalanceHistoryResponse.from(result)
            ));
    }
}

// Repository implementation
@Repository
public class JpaAccountHistoryRepository implements AccountHistoryRepository {
    
    @Query("""
        SELECT h FROM AccountBalanceHistory h 
        WHERE h.accountId = :accountId 
        AND h.recordedAt BETWEEN :fromDate AND :toDate 
        ORDER BY h.recordedAt DESC
        """)
    Page<AccountBalanceHistory> findBalanceHistory(
        @Param("accountId") AccountId accountId,
        @Param("fromDate") Instant fromDate,
        @Param("toDate") Instant toDate,
        Pageable pageable
    );
}
```

#### Phase 3: Testing Strategy

##### 1. Unit Tests
```java
@ExtendWith(MockitoExtension.class)
class GetBalanceHistoryUseCaseTest {
    
    @Mock private AccountRepository accountRepository;
    @Mock private ConsentValidationService consentValidator;
    @InjectMocks private GetBalanceHistoryUseCase useCase;
    
    @Test
    void should_retrieve_balance_history_successfully() {
        // Given
        var request = createValidRequest();
        when(consentValidator.validateForAccountAccess(any(), any(), any()))
            .thenReturn(ConsentValidationResult.valid());
        when(accountRepository.findById(any()))
            .thenReturn(Optional.of(createAccountWithHistory()));
        
        // When
        var result = useCase.execute(request).join();
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getBalanceHistory()).hasSize(5);
        verify(accountRepository).findById(request.getAccountId());
    }
}
```

##### 2. Integration Tests
```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestContainers
class AccountHistoryIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("openfinance")
            .withUsername("test")
            .withPassword("test");
    
    @Test
    void should_retrieve_balance_history_end_to_end() {
        // Given
        var accountId = createTestAccount();
        var consentId = createValidConsent();
        
        // When
        var response = restTemplate.exchange(
            "/open-finance/v1/accounts/{accountId}/balance-history?from-date=2024-01-01&to-date=2024-01-31",
            HttpMethod.GET,
            createAuthenticatedRequest(),
            BalanceHistoryResponse.class,
            accountId
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).isNotEmpty();
    }
}
```

##### 3. Contract Tests
```java
@ExtendWith(PactConsumerTestExt.class)
class BalanceHistoryConsumerTest {
    
    @Pact(consumer = "balance-history-client")
    public RequestResponsePact balanceHistoryPact(PactDslWithProvider builder) {
        return builder
            .given("account exists with balance history")
            .uponReceiving("a request for balance history")
            .path("/open-finance/v1/accounts/ACC-12345678/balance-history")
            .method("GET")
            .headers("Authorization", "Bearer token", "DPoP", "proof")
            .willRespondWith()
            .status(200)
            .body(balanceHistoryResponseBody())
            .toPact();
    }
}
```

#### Phase 4: Security and Compliance

##### 1. Security Testing
```java
@SpringBootTest
@AutoConfigureMockMvc
class AccountHistorySecurityTest {
    
    @Test
    @WithMockUser(authorities = {"SCOPE_WRONG_SCOPE"})
    void should_deny_access_with_insufficient_scope() throws Exception {
        mockMvc.perform(get("/open-finance/v1/accounts/ACC-12345678/balance-history")
                .header("DPoP", "valid-dpop-token"))
               .andExpect(status().isForbidden());
    }
    
    @Test
    void should_validate_dpop_token() throws Exception {
        mockMvc.perform(get("/open-finance/v1/accounts/ACC-12345678/balance-history")
                .header("Authorization", "Bearer valid-token")
                .header("DPoP", "invalid-dpop-token"))
               .andExpect(status().isUnauthorized());
    }
}
```

##### 2. Compliance Validation
```java
@Component
public class BalanceHistoryComplianceValidator {
    
    public ComplianceCheckResult validateCBUAECompliance(
            GetBalanceHistoryRequest request) {
        
        var checks = List.of(
            validateConsentScope(request),
            validateDataRetentionPeriod(request),
            validateAuditTrail(request),
            validateDataMinimization(request)
        );
        
        var violations = checks.stream()
            .filter(check -> !check.isPassed())
            .map(ComplianceCheck::getViolationType)
            .toList();
            
        return violations.isEmpty() 
            ? ComplianceCheckResult.compliant()
            : ComplianceCheckResult.nonCompliant(violations);
    }
}
```

### 📊 Performance Optimization

#### 1. Database Optimization
```java
// Optimized repository with proper indexing
@Repository
public class OptimizedAccountHistoryRepository {
    
    // Use database hints for better query planning
    @Query(value = """
        SELECT /*+ INDEX(account_balance_history idx_account_recorded_at) */ 
        * FROM account_balance_history 
        WHERE account_id = :accountId 
        AND recorded_at BETWEEN :fromDate AND :toDate 
        ORDER BY recorded_at DESC 
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<AccountBalanceHistory> findOptimizedBalanceHistory(
        @Param("accountId") String accountId,
        @Param("fromDate") Instant fromDate,
        @Param("toDate") Instant toDate,
        @Param("limit") int limit,
        @Param("offset") int offset
    );
}
```

#### 2. Caching Strategy
```java
@Service
public class CachedBalanceHistoryService {
    
    @Cacheable(
        value = "balance-history",
        key = "#accountId.value + '_' + #fromDate + '_' + #toDate + '_' + #pageable.pageNumber",
        condition = "#pageable.pageSize <= 100",
        unless = "#result.isEmpty()"
    )
    public List<AccountBalanceHistory> getBalanceHistory(
            AccountId accountId,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable) {
        
        return repository.findBalanceHistory(accountId, fromDate, toDate, pageable);
    }
    
    @CacheEvict(value = "balance-history", key = "#accountId.value + '_*'")
    public void evictCacheForAccount(AccountId accountId) {
        // Cache eviction when balance is updated
    }
}
```

## Compliance Requirements

### 🏛️ CBUAE Regulation C7/2023 Checklist

#### For Each New Feature:
- [ ] **Consent Requirement**: Does the feature access customer data? Ensure proper consent validation
- [ ] **Data Minimization**: Only collect and share necessary data fields
- [ ] **Purpose Limitation**: Data usage must match consented purposes
- [ ] **Retention Policy**: Implement appropriate data retention periods
- [ ] **Audit Trail**: Log all data access and processing activities
- [ ] **Customer Notification**: Inform customers of data sharing activities
- [ ] **Participant Verification**: Validate requesting participant credentials

#### Implementation Template:
```java
@RestController
public class NewFeatureController {
    
    // Always validate consent first
    private void validateConsent(ConsentId consentId, ParticipantId participantId, 
                               Set<ConsentScope> requiredScopes) {
        var validation = consentValidator.validateConsent(
            consentId, participantId, requiredScopes
        );
        
        if (!validation.isValid()) {
            // Log compliance violation
            auditService.logComplianceViolation(
                "INVALID_CONSENT", validation.getViolations()
            );
            throw new InvalidConsentException(validation.getViolations());
        }
    }
    
    // Always log data access
    private void logDataAccess(ConsentId consentId, CustomerId customerId, 
                              String dataType, String action) {
        auditService.logDataAccess(AuditEvent.builder()
            .consentId(consentId)
            .customerId(customerId)
            .dataType(dataType)
            .action(action)
            .timestamp(Instant.now())
            .build());
    }
}
```

### 🔒 PCI-DSS v4 Requirements

#### Security Controls for New Features:
```java
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration {
    
    // Requirement 7: Restrict access to cardholder data
    @PreAuthorize("hasRole('PCI_AUTHORIZED') and hasScope('CARDHOLDER_DATA')")
    public void accessCardholderData() {}
    
    // Requirement 3: Protect stored cardholder data
    @EncryptSensitiveData
    @MaskInLogs
    public String getCardNumber() {}
    
    // Requirement 4: Encrypt transmission of cardholder data
    @RequireTLS
    @RequireMTLS
    public void transmitSensitiveData() {}
}

// Custom security annotations
@Target(METHOD)
@Retention(RUNTIME)
@interface EncryptSensitiveData {}

@Target(METHOD)
@Retention(RUNTIME)
@interface MaskInLogs {}
```

## Testing Strategy

### 🧪 Test Pyramid Implementation

#### 1. Unit Tests (70%)
```java
// Fast, isolated, focused tests
@ExtendWith(MockitoExtension.class)
class ConsentServiceTest {
    
    @Test
    @DisplayName("Should authorize consent when all conditions are met")
    void should_authorize_consent_successfully() {
        // Test business logic in isolation
    }
}
```

#### 2. Integration Tests (20%)
```java
// Test component interactions
@SpringBootTest
@Testcontainers
class ConsentIntegrationTest {
    
    @Test
    void should_persist_consent_and_publish_event() {
        // Test database integration and event publishing
    }
}
```

#### 3. End-to-End Tests (10%)
```java
// Test complete user journeys
@SpringBootTest(webEnvironment = RANDOM_PORT)
class ConsentE2ETest {
    
    @Test
    void should_complete_consent_authorization_journey() {
        // Test full API workflow
    }
}
```

### 📋 Testing Checklist for New Features

#### Before Implementation:
- [ ] Define test scenarios covering happy path and edge cases
- [ ] Create test data fixtures for consistent testing
- [ ] Plan performance test scenarios
- [ ] Design security test cases

#### During Implementation:
- [ ] Follow TDD: Red → Green → Refactor
- [ ] Maintain test coverage above 90%
- [ ] Test error handling and edge cases
- [ ] Validate security and compliance requirements

#### After Implementation:
- [ ] Run full test suite
- [ ] Execute performance tests
- [ ] Conduct security testing
- [ ] Validate compliance requirements

## Deployment Procedures

### 🚀 Feature Deployment Pipeline

#### 1. Development Environment
```bash
# Local development
./infrastructure/deploy.sh docker -e development

# Run specific feature tests  
./gradlew test --tests "*NewFeatureTest"

# Generate test coverage report
./gradlew jacocoTestReport
```

#### 2. Staging Deployment
```bash
# Deploy to staging
./infrastructure/deploy.sh kubernetes -e staging

# Run integration tests against staging
./gradlew integrationTest -Denv=staging

# Perform smoke tests
curl -H "Authorization: Bearer $TOKEN" \
     https://staging.api.openfinance.local/health
```

#### 3. Production Deployment
```bash
# Blue-green deployment
kubectl apply -f k8s/blue-green/

# Canary release (10% traffic)
kubectl patch ingress openfinance-ingress \
  --patch '{"spec":{"rules":[{"http":{"paths":[{"path":"/","backend":{"serviceName":"openfinance-canary","servicePort":80}}]}}]}}'

# Monitor metrics during rollout
kubectl logs -f deployment/openfinance-canary

# Full rollout after validation
kubectl patch ingress openfinance-ingress \
  --patch '{"spec":{"rules":[{"http":{"paths":[{"path":"/","backend":{"serviceName":"openfinance-service","servicePort":80}}]}}]}}'
```

### 📊 Rollback Procedures
```bash
# Quick rollback
kubectl rollout undo deployment/openfinance-api

# Rollback to specific revision
kubectl rollout undo deployment/openfinance-api --to-revision=3

# Verify rollback success
kubectl rollout status deployment/openfinance-api
```

## Monitoring and Operations

### 📊 Feature Monitoring Setup

#### 1. Custom Metrics for New Features
```java
@Component
public class NewFeatureMetrics {
    
    private final Counter featureUsageCounter;
    private final Timer featureResponseTime;
    private final Gauge featureErrorRate;
    
    public NewFeatureMetrics(MeterRegistry meterRegistry) {
        this.featureUsageCounter = Counter.builder("new_feature_usage_total")
            .description("Total usage of new feature")
            .tag("feature", "balance-history")
            .register(meterRegistry);
    }
    
    public void recordFeatureUsage(String participant, String outcome) {
        featureUsageCounter.increment(
            Tags.of("participant", participant, "outcome", outcome)
        );
    }
}
```

#### 2. Alerting Rules
```yaml
# Prometheus alerting rules for new feature
groups:
  - name: balance-history-alerts
    rules:
      - alert: HighErrorRate
        expr: rate(new_feature_errors_total[5m]) > 0.05
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High error rate for balance history feature"
          
      - alert: SlowResponseTime
        expr: histogram_quantile(0.95, rate(new_feature_duration_seconds_bucket[5m])) > 2
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Balance history API response time is too slow"
```

#### 3. Dashboard Creation
```json
{
  "dashboard": {
    "title": "New Feature - Balance History",
    "panels": [
      {
        "title": "Request Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(new_feature_usage_total[5m])",
            "legendFormat": "{{participant}}"
          }
        ]
      },
      {
        "title": "Error Rate",
        "type": "singlestat",
        "targets": [
          {
            "expr": "rate(new_feature_errors_total[5m]) / rate(new_feature_usage_total[5m]) * 100"
          }
        ]
      }
    ]
  }
}
```

## Troubleshooting Guide

### 🔧 Common Issues and Solutions

#### 1. Performance Issues
```bash
# Check database query performance
kubectl exec -it postgres-0 -- psql -U openfinance -d openfinance \
  -c "SELECT query, mean_time, calls FROM pg_stat_statements ORDER BY mean_time DESC LIMIT 10;"

# Check Redis cache hit rate
redis-cli info stats | grep keyspace_hits

# Monitor JVM metrics
kubectl exec -it openfinance-api-0 -- jps -v
```

#### 2. Security Issues
```bash
# Check FAPI compliance violations
kubectl logs -l app=openfinance-api | grep "FAPI_VIOLATION"

# Validate SSL certificates
openssl s_client -connect api.openfinance.local:443 -verify_return_error

# Check OAuth token validation
curl -v -H "Authorization: Bearer $TOKEN" https://keycloak.openfinance.local/auth/realms/openfinance/protocol/openid_connect/userinfo
```

#### 3. Data Consistency Issues
```bash
# Check event store integrity
kubectl exec -it postgres-0 -- psql -U openfinance -d eventstore \
  -c "SELECT aggregate_id, COUNT(*) FROM events GROUP BY aggregate_id HAVING COUNT(*) > 1000;"

# Validate read model consistency
kubectl exec -it mongodb-0 -- mongo --eval "db.consents.find().count()"
```

### 📱 Emergency Response Procedures

#### 1. Critical Bug in Production
```bash
# Immediate rollback
kubectl rollout undo deployment/openfinance-api

# Isolate affected pods
kubectl label pod openfinance-api-xyz-123 quarantine=true

# Collect diagnostic information
kubectl logs openfinance-api-xyz-123 > incident-logs.txt
kubectl describe pod openfinance-api-xyz-123 > pod-status.txt
```

#### 2. Security Incident
```bash
# Block suspicious participant
kubectl patch configmap security-config --patch '
{
  "data": {
    "blocked-participants": "BANK-SUSPICIOUS01,BANK-MALICIOUS02"
  }
}'

# Rotate compromised secrets
kubectl delete secret openfinance-secrets
kubectl create secret generic openfinance-secrets --from-env-file=.env.emergency

# Audit trail analysis
grep "SECURITY_VIOLATION" /var/log/audit/* | tail -100
```

## Team Onboarding

### 👥 New Developer Onboarding Checklist

#### Week 1: Environment Setup
- [ ] Clone repository and setup development environment
- [ ] Run local Docker deployment successfully
- [ ] Complete security training and get UAE banking clearance
- [ ] Review system architecture documentation
- [ ] Attend team standup and planning meetings

#### Week 2: Code Familiarization
- [ ] Complete code review of core domain models
- [ ] Implement first bug fix with TDD approach
- [ ] Set up development tools and IDE configurations
- [ ] Learn deployment procedures for staging environment
- [ ] Shadow experienced team member on feature development

#### Week 3: Feature Development
- [ ] Implement first small feature end-to-end
- [ ] Conduct code review with senior team member
- [ ] Learn monitoring and alerting systems
- [ ] Complete compliance training (CBUAE, PCI-DSS, FAPI)
- [ ] Participate in incident response drill

#### Week 4: Full Productivity
- [ ] Take ownership of medium-complexity feature
- [ ] Contribute to architectural decisions
- [ ] Mentor newer team members
- [ ] Lead feature deployment to production
- [ ] Present feature demo to stakeholders

### 📚 Learning Resources

#### Technical Documentation
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Domain-Driven Design](https://martinfowler.com/bliki/DomainDrivenDesign.html)
- [Event Sourcing](https://martinfowler.com/eaaDev/EventSourcing.html)
- [FAPI 2.0 Specification](https://openid.net/specs/fapi-2_0-security-profile.html)

#### UAE Banking Regulations
- [CBUAE Regulation C7/2023](https://www.centralbank.ae/en/cbuae-regulation)
- [PCI-DSS v4 Requirements](https://www.pcisecuritystandards.org/)
- [UAE Data Protection Law](https://government.ae/en/information-and-services/justice-safety-and-the-law/handling-of-information-and-data)

### 🎯 Career Development Paths

#### Backend Developer Track
1. **Junior Developer** → Master core backend technologies
2. **Mid-level Developer** → Lead feature development and mentoring
3. **Senior Developer** → Architecture decisions and technical leadership
4. **Principal Engineer** → System-wide technical strategy

#### DevOps Track
1. **DevOps Engineer** → Master deployment and infrastructure
2. **Site Reliability Engineer** → Focus on system reliability and performance
3. **Platform Engineer** → Build developer productivity tools
4. **Infrastructure Architect** → Design scalable infrastructure solutions

#### Security Track
1. **Security Engineer** → Implement security controls and compliance
2. **Security Architect** → Design secure systems and protocols
3. **Compliance Officer** → Ensure regulatory adherence
4. **CISO** → Overall security strategy and risk management

---

## 📞 Support and Resources

### Team Contacts
- **Tech Lead**: Mohammad Al-Rashid (m.alrashid@enterprise.ae)
- **Security Architect**: Sarah Al-Zahra (s.alzahra@enterprise.ae)  
- **DevOps Lead**: Ahmed Al-Mahmoud (a.mahmoud@enterprise.ae)
- **Compliance Officer**: Fatima Al-Mansouri (f.almansouri@enterprise.ae)

### Emergency Escalation
- **Production Issues**: +971-50-XXX-XXXX (24/7 On-call)
- **Security Incidents**: security-incident@enterprise.ae
- **Compliance Issues**: compliance-urgent@enterprise.ae

### Documentation Links
- [API Documentation](https://api.openfinance.enterprise.ae/docs)
- [Architecture Diagrams](https://confluence.enterprise.ae/openfinance/architecture)
- [Runbooks](https://confluence.enterprise.ae/openfinance/operations)
- [Incident Response](https://confluence.enterprise.ae/openfinance/incidents)

---

**Last Updated**: January 2024  
**Version**: 1.0  
**Document Owner**: OpenFinance Platform Team