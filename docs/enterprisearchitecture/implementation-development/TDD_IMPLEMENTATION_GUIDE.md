# TDD Implementation Guide for Open Finance Integration

## Overview
This guide outlines the Test-Driven Development approach for implementing Open Finance functionality, maintaining the project's high code quality standards with 83% coverage target and comprehensive architectural guardrails.

## TDD Principles Applied

### Red-Green-Refactor Cycle
1. **🔴 Red**: Write failing test first
2. **🟢 Green**: Write minimal code to pass test
3. **🔄 Refactor**: Improve code while keeping tests green

### Testing Strategy Layers

#### 1. Architecture Tests (ArchUnit) - Guardrails 🛡️
```java
@Tag("architecture")
class ArchitectureTest {
    // Enforce hexagonal architecture
    // Validate dependency directions
    // Ensure clean code principles
}
```

#### 2. Domain Model Tests (Unit + Property-Based) - 90% Coverage 🎯
```java
@Tag("unit")
@Tag("property-based")
class ConsentTest {
    // Business rule validation
    // State transition testing
    // Invariant verification
    // Property-based testing with jqwik
}
```

#### 3. Integration Tests (TestContainers) - 80% Coverage 🔧
```java
@Tag("integration")
class ConsentRepositoryIntegrationTest {
    // Database integration
    // Kafka event publishing
    // External service integration
}
```

#### 4. Security Tests (FAPI 2.0 Compliance) - 95% Coverage 🔒
```java
@Tag("security")
class OpenFinanceSecurityTest {
    // OAuth 2.1 flow validation
    // mTLS certificate verification
    // DPoP token validation
}
```

#### 5. Performance Tests (JMH) - Baseline Established ⚡
```java
@Tag("performance")
class ConsentPerformanceTest {
    // Response time benchmarks
    // Throughput measurements
    // Memory usage profiling
}
```

## Implementation Phases with TDD

### Phase 1: Domain Foundation (Week 1-2)

#### 1.1 Create Failing Tests First
```bash
# Create test structure
mkdir -p open-finance-context/open-finance-domain/src/test/java/com/enterprise/openfinance/domain

# Write failing tests
touch ConsentTest.java
touch ParticipantTest.java
touch ConsentScopeTest.java
```

#### 1.2 Test Categories
- **Architecture Tests**: Validate clean architecture principles
- **Value Object Tests**: Immutability, equality, validation
- **Aggregate Tests**: Business rules, state transitions
- **Domain Event Tests**: Event publishing, immutability

#### 1.3 Coverage Targets
- **Domain Layer**: 95% line coverage
- **Business Rules**: 100% branch coverage
- **Error Scenarios**: All edge cases covered

### Phase 2: Application Services (Week 3-4)

#### 2.1 Use Case Testing
```java
@Test
@DisplayName("Given valid consent request, When creating consent, Then should return consent ID")
void should_create_consent_successfully() {
    // Given: Mock dependencies
    var mockRepository = mock(ConsentRepository.class);
    var mockEventPublisher = mock(EventPublisher.class);
    var useCase = new CreateConsentUseCase(mockRepository, mockEventPublisher);
    
    // When: Execute use case
    var command = CreateConsentCommand.builder()...build();
    var result = useCase.execute(command);
    
    // Then: Verify result and interactions
    assertThat(result.isSuccess()).isTrue();
    verify(mockRepository).save(any(Consent.class));
    verify(mockEventPublisher).publish(any(ConsentCreatedEvent.class));
}
```

#### 2.2 Saga Testing
```java
@Test
@DisplayName("Given consent authorization saga, When customer approves, Then should complete successfully")
void should_complete_consent_authorization_saga() {
    // Test saga orchestration
    // Verify compensation logic
    // Test timeout scenarios
}
```

### Phase 3: Infrastructure Adapters (Week 5-6)

#### 3.1 Repository Integration Tests
```java
@Tag("integration")
@Testcontainers
class ConsentJpaRepositoryIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("openfinance_test")
            .withUsername("test")
            .withPassword("test");
    
    @Test
    void should_persist_and_retrieve_consent() {
        // Given: Consent entity
        var consent = ConsentTestData.createValidConsent();
        
        // When: Saving to database
        var savedConsent = repository.save(consent);
        
        // Then: Should retrieve correctly
        var retrieved = repository.findById(savedConsent.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get()).usingRecursiveComparison()
                .isEqualTo(savedConsent);
    }
}
```

#### 3.2 CBUAE Integration Tests
```java
@Test
@DisplayName("Given valid participant ID, When fetching from CBUAE directory, Then should return participant data")
void should_fetch_participant_from_cbuae() {
    // Given: Wiremock CBUAE server
    var cbuaeServer = WireMockServer.builder()
            .port(8089)
            .httpsPort(8443)
            .build();
    
    // When: Making CBUAE API call
    var participant = cbuaeAdapter.getParticipant(participantId);
    
    // Then: Should return valid data
    assertThat(participant).isNotNull();
    assertThat(participant.getId()).isEqualTo(participantId);
}
```

### Phase 4: API Layer (Week 7-8)

#### 4.1 REST Controller Tests
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ConsentControllerIntegrationTest {
    
    @Test
    @WithMockUser(authorities = {"CONSENT_CREATE"})
    void should_create_consent_via_rest_api() {
        // Given: Consent creation request
        var request = ConsentCreationRequest.builder()...build();
        
        // When: POST to /consents
        mockMvc.perform(post("/open-banking/v1/consents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                
        // Then: Should return 201 Created
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.consentId").exists())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }
}
```

### Phase 5: Security Implementation (Week 9-10)

#### 5.1 FAPI 2.0 Compliance Tests
```java
@Tag("security")
class FAPIComplianceTest {
    
    @Test
    void should_validate_dpop_token() {
        // Test DPoP token validation
        // Verify token binding
        // Test replay attack prevention
    }
    
    @Test
    void should_enforce_mtls_authentication() {
        // Test mutual TLS
        // Verify certificate validation
        // Test certificate revocation
    }
}
```

### Phase 6: End-to-End Testing (Week 11-12)

#### 6.1 Complete Journey Tests
```java
@SpringBootTest
@Testcontainers
@Tag("e2e")
class OpenFinanceE2ETest {
    
    @Test
    void should_complete_full_consent_and_data_sharing_journey() {
        // Given: Customer, Participant, and CBUAE setup
        
        // When: Complete flow
        // 1. Create consent
        // 2. Authorize consent
        // 3. Request data
        // 4. Verify data sharing
        // 5. Revoke consent
        
        // Then: Verify all steps completed correctly
    }
}
```

## Coverage Metrics and Guardrails

### Coverage Targets by Layer
```yaml
Domain Layer: 95%
Application Layer: 90%
Infrastructure Layer: 85%
API Layer: 80%
Overall Project: 83%+
```

### Enforcement Mechanisms
```gradle
// build.gradle
jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = 0.83
            }
        }
        rule {
            element = 'CLASS'
            limit {
                minimum = 0.75
            }
        }
    }
}

check.dependsOn jacocoTestCoverageVerification
```

### ArchUnit Guardrails
```java
// Architecture rules enforced at build time
@ArchTest
static ArchRule domain_should_not_depend_on_infrastructure = 
    noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAPackage("..infrastructure..");

@ArchTest  
static ArchRule aggregates_should_extend_aggregate_root =
    classes()
        .that().areAnnotatedWith(AggregateRoot.class)
        .should().beAssignableTo(com.enterprise.shared.domain.AggregateRoot.class);
```

## Testing Infrastructure Setup

### Test Configuration
```java
// TestConfiguration.java
@TestConfiguration
@EnableTestcontainers
public class OpenFinanceTestConfiguration {
    
    @Bean
    @Primary
    public Clock testClock() {
        return Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
    }
    
    @Bean
    public WireMockServer cbuaeWireMock() {
        return new WireMockServer(wireMockConfig()
                .port(8089)
                .httpsPort(8443)
                .keystorePath("test-keystore.jks"));
    }
}
```

### Test Data Builders
```java
// ConsentTestData.java
public class ConsentTestData {
    
    public static Consent.ConsentBuilder validConsent() {
        return Consent.builder()
                .id(ConsentId.generate())
                .customerId(CustomerId.of("CUST-" + RandomStringUtils.randomAlphabetic(8)))
                .participantId(ParticipantId.of("PART-" + RandomStringUtils.randomAlphabetic(8)))
                .scopes(Set.of(ConsentScope.ACCOUNT_INFORMATION))
                .purpose(ConsentPurpose.LOAN_APPLICATION)
                .expiryDate(LocalDateTime.now().plusDays(30));
    }
    
    public static ConsentCreationCommand.ConsentCreationCommandBuilder validConsentCommand() {
        return ConsentCreationCommand.builder()
                .customerId("CUST-12345678")
                .participantId("PART-87654321")
                .scopes(Set.of("account_information"))
                .purpose("loan_application")
                .validityDays(30);
    }
}
```

### Property-Based Testing Setup
```java
// Property-based test example
@Property(tries = 100)
@Label("Consent state transitions should be valid")
void consent_state_transitions_should_be_valid(
        @ForAll("validConsents") Consent consent,
        @ForAll("validStateTransitions") ConsentAction action) {
    
    Assume.that(isValidTransition(consent.getStatus(), action));
    
    var originalStatus = consent.getStatus();
    var events = consent.getDomainEvents().size();
    
    // When: Applying action
    applyAction(consent, action);
    
    // Then: State should be valid
    assertThat(consent.getStatus()).isNotEqualTo(originalStatus);
    assertThat(consent.getDomainEvents()).hasSizeGreaterThan(events);
}
```

## Continuous Integration Pipeline

### GitHub Actions Configuration
```yaml
name: TDD Pipeline
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_PASSWORD: postgres
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          
      - name: Run Architecture Tests
        run: ./gradlew archTest
        
      - name: Run Unit Tests
        run: ./gradlew test
        
      - name: Run Integration Tests  
        run: ./gradlew integrationTest
        
      - name: Run Security Tests
        run: ./gradlew securityTest
        
      - name: Generate Coverage Report
        run: ./gradlew jacocoTestReport
        
      - name: Verify Coverage
        run: ./gradlew jacocoTestCoverageVerification
        
      - name: Upload Coverage to Codecov
        uses: codecov/codecov-action@v3
```

## Test Organization Best Practices

### Package Structure
```
src/test/java/
├── com/enterprise/openfinance/
│   ├── domain/
│   │   ├── ArchitectureTest.java
│   │   ├── model/
│   │   │   ├── consent/
│   │   │   │   ├── ConsentTest.java
│   │   │   │   ├── ConsentIdTest.java
│   │   │   │   └── ConsentScopeTest.java
│   │   │   └── participant/
│   │   │       └── ParticipantTest.java
│   │   └── service/
│   │       └── ConsentDomainServiceTest.java
│   ├── application/
│   │   ├── usecase/
│   │   │   └── CreateConsentUseCaseTest.java
│   │   └── saga/
│   │       └── ConsentAuthorizationSagaTest.java
│   └── infrastructure/
│       ├── adapter/
│       │   ├── input/
│       │   │   └── rest/
│       │   │       └── ConsentControllerTest.java
│       │   └── output/
│       │       ├── persistence/
│       │       │   └── ConsentJpaRepositoryTest.java
│       │       └── cbuae/
│       │           └── CBUAEAdapterTest.java
│       └── config/
│           └── SecurityConfigTest.java
└── testfixtures/
    ├── ConsentTestData.java
    ├── ParticipantTestData.java
    └── TestConfiguration.java
```

### Test Tags Strategy
```java
// Unit tests - fast, isolated
@Tag("unit")

// Integration tests - with external dependencies
@Tag("integration") 

// Architecture tests - structural validation
@Tag("architecture")

// Security tests - FAPI compliance
@Tag("security")

// Performance tests - benchmarks
@Tag("performance")

// Property-based tests - invariant verification
@Tag("property-based")

// End-to-end tests - full system validation
@Tag("e2e")

// Contract tests - API contract validation
@Tag("contract")
```

## Quality Gates

### Pre-commit Hooks
```bash
#!/bin/bash
# .git/hooks/pre-commit
echo "Running TDD quality gates..."

# Run architecture tests
echo "🏗️  Running architecture tests..."
./gradlew archTest || exit 1

# Run fast unit tests
echo "⚡ Running unit tests..."
./gradlew test --tests "*Test" -x integrationTest || exit 1

# Check code coverage
echo "📊 Checking code coverage..."
./gradlew jacocoTestCoverageVerification || exit 1

echo "✅ All quality gates passed!"
```

### Build Pipeline Gates
1. **Architecture Compliance**: ArchUnit rules must pass
2. **Unit Test Coverage**: Must maintain 83%+ overall coverage
3. **Integration Tests**: All integration tests must pass
4. **Security Tests**: FAPI 2.0 compliance tests must pass
5. **Performance Regression**: Benchmarks within acceptable range

## Monitoring and Metrics

### Test Metrics Dashboard
- **Coverage Trends**: Track coverage over time
- **Test Execution Time**: Monitor test suite performance
- **Flaky Test Detection**: Identify unstable tests
- **Architecture Violations**: Track rule violations
- **Security Test Results**: FAPI compliance status

### Key Performance Indicators
```yaml
Test Coverage: 83%+
Test Execution Time: <10 minutes for full suite
Architecture Violations: 0
Security Test Pass Rate: 100%
Integration Test Stability: >99%
```

This TDD approach ensures high code quality, comprehensive test coverage, and maintains architectural integrity throughout the Open Finance implementation.