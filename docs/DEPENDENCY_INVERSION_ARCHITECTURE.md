# Dependency Inversion Architecture

## Overview

This document describes the implementation of proper dependency inversion principles in the Loan Management System. The dependency inversion principle states that high-level modules should not depend on low-level modules; both should depend on abstractions.

## Architecture Principles

### 1. Dependency Inversion Principle (DIP)
- **High-level modules** (Application Services) do not depend on low-level modules (Infrastructure)
- **Both depend on abstractions** (Ports/Interfaces)
- **Abstractions do not depend on details**; details depend on abstractions

### 2. Framework Agnostic Design
- Application layer services can work with any framework
- Infrastructure adapters handle framework-specific implementations
- Configuration layer wires abstractions with concrete implementations

## Implementation Structure

```
src/main/java/com/loanmanagement/
├── shared/
│   ├── application/
│   │   └── port/
│   │       └── out/
│   │           ├── TransactionManager.java       # Transaction abstraction
│   │           ├── LoggingPort.java             # Logging abstraction
│   │           ├── LoggingFactory.java          # Logger factory
│   │           ├── TimeProvider.java            # Time operations abstraction
│   │           ├── ValidationPort.java          # Validation abstraction
│   │           ├── HttpClientPort.java          # HTTP client abstraction
│   │           └── ServiceRegistryPort.java     # Service discovery abstraction
│   └── infrastructure/
│       ├── adapter/
│       │   └── out/
│       │       ├── transaction/
│       │       │   └── SpringTransactionManagerAdapter.java
│       │       ├── logging/
│       │       │   ├── Slf4jLoggingAdapter.java
│       │       │   └── Slf4jLoggingFactory.java
│       │       ├── time/
│       │       │   └── SystemTimeProviderAdapter.java
│       │       ├── validation/
│       │       │   └── BeanValidationAdapter.java
│       │       └── http/
│       │           └── RestTemplateHttpClientAdapter.java
│       └── config/
│           └── DependencyInversionConfiguration.java
```

## Abstractions Implemented

### 1. TransactionManager
```java
public interface TransactionManager {
    <T> T executeInTransaction(Supplier<T> operation);
    <T> T executeInReadOnlyTransaction(Supplier<T> operation);
    void executeWithoutTransaction(Runnable operation);
    <T> T executeWithAttributes(Supplier<T> operation, TransactionAttributes attributes);
}
```

**Purpose**: Abstracts transaction management from Spring's `@Transactional`
**Implementation**: `SpringTransactionManagerAdapter` using `PlatformTransactionManager`

### 2. LoggingPort
```java
public interface LoggingPort {
    void debug(String message, Object... args);
    void info(String message, Object... args);
    void warn(String message, Object... args);
    void error(String message, Object... args);
    // ... other methods
}
```

**Purpose**: Abstracts logging from SLF4J/Logback framework
**Implementation**: `Slf4jLoggingAdapter` using SLF4J Logger

### 3. TimeProvider
```java
public interface TimeProvider {
    LocalDateTime now();
    LocalDate today();
    ZonedDateTime nowZoned();
    long currentTimeMillis();
    Clock getClock();
}
```

**Purpose**: Abstracts time operations for better testability
**Implementation**: `SystemTimeProviderAdapter` using `Clock.systemDefaultZone()`

### 4. ValidationPort
```java
public interface ValidationPort {
    <T> ValidationResult validate(T object);
    <T> ValidationResult validate(T object, Class<?>... groups);
    <T> ValidationResult validateProperty(T object, String propertyName);
}
```

**Purpose**: Abstracts validation framework (JSR-303)
**Implementation**: `BeanValidationAdapter` using Jakarta Bean Validation

### 5. HttpClientPort
```java
public interface HttpClientPort {
    <T> HttpResponse<T> get(String url, Class<T> responseType);
    <T, R> HttpResponse<R> post(String url, T requestBody, Class<R> responseType);
    // ... other HTTP methods
    <T> CompletableFuture<HttpResponse<T>> getAsync(String url, Class<T> responseType);
}
```

**Purpose**: Abstracts HTTP client operations
**Implementation**: `RestTemplateHttpClientAdapter` using Spring's RestTemplate

## Framework-Agnostic Services

### Example: CreateLoanApplicationServiceImpl
```java
public class CreateLoanApplicationServiceImpl implements CreateLoanUseCase {
    private final LoanRepository loanRepository;
    private final CustomerRepository customerRepository;
    private final LoanEventPublisher eventPublisher;
    private final LoanFacadeService loanFacadeService;
    private final TransactionManager transactionManager;  // ← Abstraction
    private final LoggingPort logger;                     // ← Abstraction
    
    // No Spring annotations or dependencies!
}
```

### Benefits:
1. **Testability**: Easy to mock abstractions in unit tests
2. **Framework Independence**: Can switch from Spring to Quarkus/Micronaut
3. **Clear Boundaries**: Explicit contracts between layers
4. **Maintainability**: Changes in infrastructure don't affect business logic

## Configuration and Wiring

### DependencyInversionConfiguration
```java
@Configuration
@ConditionalOnProperty(name = "loan.management.dependency-inversion.enabled", 
                      havingValue = "true", matchIfMissing = true)
public class DependencyInversionConfiguration {
    
    @Bean
    public TransactionManager transactionManager(PlatformTransactionManager ptm) {
        return new SpringTransactionManagerAdapter(ptm);
    }
    
    @Bean
    public LoggingFactory loggingFactory() {
        return new Slf4jLoggingFactory();
    }
    
    // ... other beans
}
```

## Usage Examples

### Framework-Agnostic Service
```java
public class ExternalCreditCheckService {
    private final HttpClientPort httpClient;      // ← Not RestTemplate
    private final ValidationPort validator;       // ← Not Bean Validation directly
    private final TimeProvider timeProvider;      // ← Not LocalDateTime.now()
    private final LoggingPort logger;            // ← Not SLF4J Logger
    
    public CreditCheckResult checkCreditScore(CreditCheckRequest request) {
        logger.info("Checking credit score for customer: {}", request.customerId());
        
        ValidationPort.ValidationResult result = validator.validate(request);
        if (!result.valid()) {
            return CreditCheckResult.invalid(result.getViolationMessages());
        }
        
        HttpClientPort.HttpResponse<ExternalCreditResponse> response = 
            httpClient.post("/credit-check", request, ExternalCreditResponse.class);
            
        return mapToResult(response, timeProvider.now());
    }
}
```

### Testing with Abstractions
```java
@Test
void shouldCheckCreditScore() {
    // Mock abstractions instead of framework classes
    HttpClientPort mockHttpClient = mock(HttpClientPort.class);
    ValidationPort mockValidator = mock(ValidationPort.class);
    TimeProvider mockTimeProvider = mock(TimeProvider.class);
    LoggingFactory mockLoggingFactory = mock(LoggingFactory.class);
    
    ExternalCreditCheckService service = new ExternalCreditCheckService(
        mockHttpClient, mockValidator, mockTimeProvider, mockLoggingFactory);
    
    // Test without any framework dependencies
}
```

## Benefits Achieved

### 1. **True Framework Independence**
- Application services have zero Spring dependencies
- Can be deployed in any Java environment
- Easy to test in isolation

### 2. **Improved Testability**
- All external dependencies are abstractions
- Easy to mock and stub
- Deterministic tests with controlled time/logging

### 3. **Better Separation of Concerns**
- Business logic completely separated from infrastructure
- Infrastructure changes don't affect application layer
- Clear architectural boundaries

### 4. **Enhanced Maintainability**
- Easy to switch infrastructure implementations
- Framework upgrades don't impact business logic
- Clear contracts between layers

### 5. **Performance Benefits**
- Custom transaction handling allows optimization
- Async operations through abstractions
- Better resource management

## Migration Path

### From Spring-Dependent to Framework-Agnostic

1. **Identify Framework Dependencies**
   ```java
   // Before: Direct Spring dependency
   @Service
   @Transactional
   public class LoanService {
       @Autowired
       private LoanRepository repository;
   }
   ```

2. **Create Abstractions**
   ```java
   // Create abstractions for external concerns
   public interface TransactionManager { ... }
   public interface LoggingPort { ... }
   ```

3. **Implement Framework-Agnostic Service**
   ```java
   // After: Framework-agnostic
   public class LoanServiceImpl implements LoanUseCase {
       private final LoanRepository repository;
       private final TransactionManager transactionManager;
       
       public LoanServiceImpl(LoanRepository repository, 
                             TransactionManager transactionManager) {
           this.repository = repository;
           this.transactionManager = transactionManager;
       }
   }
   ```

4. **Wire with Configuration**
   ```java
   @Configuration
   public class ServiceConfiguration {
       @Bean
       public LoanUseCase loanUseCase(LoanRepository repository,
                                     TransactionManager transactionManager) {
           return new LoanServiceImpl(repository, transactionManager);
       }
   }
   ```

## Best Practices

### 1. **Abstract External Dependencies**
- Always create interfaces for external systems
- Keep abstractions minimal and focused
- Use composition over inheritance

### 2. **Avoid Framework Annotations in Business Logic**
- No `@Service`, `@Transactional`, `@Autowired` in application services
- Use constructor injection in configuration classes
- Keep business logic pure

### 3. **Test Abstractions**
- Write tests for both abstractions and implementations
- Verify contracts are maintained
- Test edge cases and error conditions

### 4. **Documentation**
- Document abstraction purpose and usage
- Provide examples of implementation
- Maintain architecture decision records

## Conclusion

The dependency inversion implementation provides a clean, maintainable, and testable architecture that truly separates business logic from infrastructure concerns. This approach enables the system to evolve independently at different layers and supports long-term maintainability.