# Java 21 Training Guide for Banking Teams

## Overview

This training guide provides comprehensive education on Java 21 features as implemented in the Enterprise Loan Management System. It's designed for development teams, operations staff, and technical stakeholders who need to understand and work with the new Java 21 codebase.

## Table of Contents

1. [Introduction to Java 21](#introduction-to-java-21)
2. [Virtual Threads in Banking](#virtual-threads-in-banking)
3. [Pattern Matching for Business Logic](#pattern-matching-for-business-logic)
4. [Sequenced Collections for Financial Data](#sequenced-collections-for-financial-data)
5. [Record Patterns for Data Validation](#record-patterns-for-data-validation)
6. [Banking Code Examples](#banking-code-examples)
7. [Performance Optimization](#performance-optimization)
8. [Debugging and Troubleshooting](#debugging-and-troubleshooting)
9. [Best Practices](#best-practices)
10. [Hands-On Exercises](#hands-on-exercises)

## Introduction to Java 21

### What's New in Java 21

Java 21 is a Long-Term Support (LTS) release that introduces several groundbreaking features particularly beneficial for banking applications:

- **Virtual Threads (JEP 444)**: Lightweight threads for high-concurrency applications
- **Pattern Matching for switch (JEP 441)**: Enhanced switch expressions with pattern matching
- **Sequenced Collections (JEP 431)**: Collections with a defined encounter order
- **Record Patterns (JEP 440)**: Deconstructing record values in pattern matching
- **String Templates (Preview)**: Safe and efficient string composition

### Why Java 21 for Banking?

| Feature | Banking Benefit | Example Use Case |
|---------|-----------------|------------------|
| Virtual Threads | Handle thousands of concurrent transactions | Payment processing, loan applications |
| Pattern Matching | Type-safe business rules | Customer risk assessment, loan approval |
| Sequenced Collections | Ordered financial data | Transaction history, payment sequences |
| Record Patterns | Validated data structures | Payment validation, compliance checks |

## Virtual Threads in Banking

### Understanding Virtual Threads

Virtual Threads are lightweight, user-mode threads managed by the JVM, not the OS. They're perfect for I/O-intensive banking operations.

#### Traditional Threading Model

```java
// Old approach - Platform Threads (Limited scalability)
public class TraditionalPaymentProcessor {
    private final ExecutorService executor = Executors.newFixedThreadPool(200);
    
    public void processPayments(List<Payment> payments) {
        for (Payment payment : payments) {
            executor.submit(() -> {
                // Each thread consumes ~2MB stack memory
                validatePayment(payment);
                processWithBank(payment);
                updateDatabase(payment);
            });
        }
    }
}
```

#### Java 21 Virtual Threads

```java
// New approach - Virtual Threads (Massive scalability)
public class Java21PaymentProcessor {
    private final ExecutorService virtualExecutor = 
        Executors.newVirtualThreadPerTaskExecutor();
    
    public void processPayments(List<Payment> payments) {
        for (Payment payment : payments) {
            virtualExecutor.submit(() -> {
                // Each virtual thread consumes ~1KB memory
                validatePayment(payment);
                processWithBank(payment);
                updateDatabase(payment);
            });
        }
    }
}
```

### Banking Use Cases for Virtual Threads

#### 1. High-Frequency Payment Processing

```java
@Service
public class PaymentProcessingService {
    private final ExecutorService paymentExecutor = 
        Executors.newVirtualThreadPerTaskExecutor();
    
    public CompletableFuture<PaymentResult> processPayment(PaymentRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            // All these operations can block without issues
            var validation = validatePayment(request);          // 50ms
            var fraudCheck = performFraudCheck(request);        // 200ms
            var complianceCheck = checkCompliance(request);     // 150ms
            var bankProcessing = processWithBank(request);      // 300ms
            
            return new PaymentResult(validation, fraudCheck, 
                                   complianceCheck, bankProcessing);
        }, paymentExecutor);
    }
}
```

#### 2. Concurrent Loan Application Processing

```java
@Service
public class LoanApplicationService {
    private final ExecutorService loanExecutor = 
        Executors.newVirtualThreadPerTaskExecutor();
    
    public LoanDecision processLoanApplication(LoanApplication application) {
        // Process multiple checks concurrently
        var creditCheckFuture = CompletableFuture.supplyAsync(
            () -> performCreditCheck(application), loanExecutor);
        
        var incomeVerificationFuture = CompletableFuture.supplyAsync(
            () -> verifyIncome(application), loanExecutor);
        
        var riskAssessmentFuture = CompletableFuture.supplyAsync(
            () -> assessRisk(application), loanExecutor);
        
        // Combine results
        return CompletableFuture.allOf(
            creditCheckFuture, incomeVerificationFuture, riskAssessmentFuture
        ).thenApply(v -> {
            var creditScore = creditCheckFuture.join();
            var incomeVerified = incomeVerificationFuture.join();
            var riskLevel = riskAssessmentFuture.join();
            
            return makeLoanDecision(creditScore, incomeVerified, riskLevel);
        }).join();
    }
}
```

### Virtual Threads Best Practices

#### ✅ Do's

- Use for I/O-bound operations (database calls, web services)
- Create many virtual threads (thousands or millions)
- Use with blocking I/O operations
- Combine with CompletableFuture for async processing

#### ❌ Don'ts

- Use for CPU-intensive computations
- Use synchronized blocks (use ReentrantLock instead)
- Pool virtual threads (create per task)
- Mix with ThreadLocal extensively

## Pattern Matching for Business Logic

### Enhanced Switch Expressions

Pattern matching makes business logic more readable and type-safe.

#### Traditional Approach

```java
// Old way - Verbose and error-prone
public LoanDecision evaluateLoanApplication(Customer customer, LoanApplication application) {
    if (customer instanceof PremiumCustomer) {
        PremiumCustomer premium = (PremiumCustomer) customer;
        if (premium.getCreditScore() >= 750 && premium.getIncome().compareTo(THRESHOLD) > 0) {
            return new LoanDecision(APPROVED, premium.getPreferredRate());
        }
    } else if (customer instanceof StandardCustomer) {
        StandardCustomer standard = (StandardCustomer) customer;
        if (standard.getCreditScore() >= 650 && standard.getEmploymentStatus() == STABLE) {
            return new LoanDecision(CONDITIONAL_APPROVAL, standard.getStandardRate());
        }
    }
    // More conditions...
    return new LoanDecision(REJECTED, null);
}
```

#### Java 21 Pattern Matching

```java
// New way - Concise and type-safe
public LoanDecision evaluateLoanApplication(Customer customer, LoanApplication application) {
    return switch (customer) {
        case PremiumCustomer(var id, var name, var creditScore, var income, var dob, 
                           var relationshipYears, var assets) 
            when creditScore >= 750 && income.compareTo(THRESHOLD) > 0 -> {
            yield new LoanDecision(
                APPROVED, 
                calculatePremiumRate(creditScore, relationshipYears),
                "Premium customer with excellent credit"
            );
        }
        
        case StandardCustomer(var id, var name, var creditScore, var income, var dob, 
                            var employment, var tenure) 
            when creditScore >= 650 && employment.isStable() -> {
            yield new LoanDecision(
                CONDITIONAL_APPROVAL,
                calculateStandardRate(creditScore, tenure),
                "Standard approval with conditions"
            );
        }
        
        case YoungProfessional(var id, var name, var creditScore, var income, var dob, 
                             var education, var potential) 
            when creditScore >= 700 && potential.isHigh() -> {
            yield new LoanDecision(
                APPROVED,
                calculateYouthRate(creditScore, education),
                "Young professional with high potential"
            );
        }
        
        default -> new LoanDecision(
            REJECTED,
            null,
            "Does not meet lending criteria"
        );
    };
}
```

### Pattern Matching for State Transitions

```java
public class LoanStateMachine {
    
    public LoanTransitionResult transition(LoanState currentState, LoanAction action) {
        return switch (currentState) {
            case PendingState pending when action instanceof Approve approve -> {
                var conditions = validateApprovalConditions(approve);
                var newState = new ApprovedState(
                    LocalDateTime.now(),
                    approve.reason(),
                    approve.officerId(),
                    conditions,
                    approve.approvedAmount()
                );
                yield new LoanTransitionResult(true, "Loan approved", newState);
            }
            
            case PendingState pending when action instanceof Reject reject -> {
                var newState = new RejectedState(
                    LocalDateTime.now(),
                    reject.reason(),
                    reject.officerId()
                );
                yield new LoanTransitionResult(true, "Loan rejected", newState);
            }
            
            case ApprovedState approved when action instanceof Disburse disburse -> {
                var newState = new DisbursedState(
                    LocalDateTime.now(),
                    disburse.amount(),
                    disburse.method(),
                    disburse.account()
                );
                yield new LoanTransitionResult(true, "Loan disbursed", newState);
            }
            
            case DisbursedState disbursed when action instanceof MakePayment payment -> {
                var updatedState = disbursed.withPayment(payment);
                var message = payment.amount().equals(disbursed.remainingAmount()) ?
                    "Loan fully paid" : "Payment processed";
                yield new LoanTransitionResult(true, message, updatedState);
            }
            
            default -> new LoanTransitionResult(
                false,
                "Invalid transition: " + action.getClass().getSimpleName() + 
                " not allowed from " + currentState.getClass().getSimpleName(),
                currentState
            );
        };
    }
}
```

## Sequenced Collections for Financial Data

### Understanding Sequenced Collections

Sequenced Collections provide a defined encounter order with efficient access to first and last elements.

#### New Interfaces in Java 21

```java
public interface SequencedCollection<E> extends Collection<E> {
    E getFirst();                    // O(1) access to first element
    E getLast();                     // O(1) access to last element
    void addFirst(E e);
    void addLast(E e);
    SequencedCollection<E> reversed();
}

public interface SequencedSet<E> extends SequencedCollection<E>, Set<E> {
    SequencedSet<E> reversed();
}

public interface SequencedMap<K,V> extends Map<K,V> {
    Map.Entry<K,V> firstEntry();
    Map.Entry<K,V> lastEntry();
    SequencedMap<K,V> reversed();
}
```

### Banking Applications

#### 1. Transaction History Management

```java
public class AccountTransactionHistory {
    // Ordered transaction history
    private final SequencedSet<Transaction> transactions = new LinkedHashSet<>();
    
    public void addTransaction(Transaction transaction) {
        transactions.addLast(transaction);
        
        // Maintain size limit (keep last 1000 transactions)
        while (transactions.size() > 1000) {
            transactions.removeFirst();
        }
    }
    
    public Transaction getLatestTransaction() {
        return transactions.isEmpty() ? null : transactions.getLast(); // O(1)
    }
    
    public Transaction getFirstTransaction() {
        return transactions.isEmpty() ? null : transactions.getFirst(); // O(1)
    }
    
    public SequencedSet<Transaction> getRecentTransactions(int count) {
        return transactions.reversed() // Most recent first
            .stream()
            .limit(count)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }
    
    public BigDecimal calculateRunningBalance() {
        var balance = BigDecimal.ZERO;
        for (var transaction : transactions) { // Ordered iteration
            balance = balance.add(transaction.getAmount());
        }
        return balance;
    }
}
```

#### 2. Payment Schedule Management

```java
public class LoanPaymentSchedule {
    private final SequencedSet<ScheduledPayment> schedule = new LinkedHashSet<>();
    
    public void generateSchedule(LoanTerms terms) {
        var paymentAmount = calculateMonthlyPayment(terms);
        var currentDate = terms.startDate();
        
        for (int i = 0; i < terms.termInMonths(); i++) {
            var payment = new ScheduledPayment(
                i + 1,
                currentDate,
                paymentAmount,
                calculatePrincipal(i, terms),
                calculateInterest(i, terms)
            );
            schedule.addLast(payment);
            currentDate = currentDate.plusMonths(1);
        }
    }
    
    public Optional<ScheduledPayment> getNextDuePayment() {
        return schedule.stream()
            .filter(payment -> !payment.isPaid())
            .findFirst(); // First unpaid payment
    }
    
    public ScheduledPayment getFinalPayment() {
        return schedule.getLast(); // O(1) access
    }
    
    public List<ScheduledPayment> getPastDuePayments() {
        var today = LocalDate.now();
        return schedule.stream()
            .filter(payment -> payment.dueDate().isBefore(today) && !payment.isPaid())
            .toList();
    }
}
```

#### 3. Audit Trail Management

```java
public class LoanAuditTrail {
    private final SequencedMap<LocalDateTime, AuditEvent> events = new LinkedHashMap<>();
    
    public void recordEvent(AuditEvent event) {
        events.put(event.timestamp(), event);
    }
    
    public AuditEvent getFirstEvent() {
        var firstEntry = events.firstEntry(); // O(1)
        return firstEntry != null ? firstEntry.getValue() : null;
    }
    
    public AuditEvent getLatestEvent() {
        var lastEntry = events.lastEntry(); // O(1)
        return lastEntry != null ? lastEntry.getValue() : null;
    }
    
    public SequencedMap<LocalDateTime, AuditEvent> getEventsInReverse() {
        return events.reversed(); // View in reverse chronological order
    }
    
    public Duration getTotalProcessingTime() {
        if (events.isEmpty()) return Duration.ZERO;
        
        var firstEvent = events.firstEntry().getValue();
        var lastEvent = events.lastEntry().getValue();
        
        return Duration.between(firstEvent.timestamp(), lastEvent.timestamp());
    }
}
```

## Record Patterns for Data Validation

### Record Definitions for Banking

```java
// Financial data records with validation
public record PaymentRequest(
    String paymentId,
    BigDecimal amount,
    String fromAccount,
    String toAccount,
    PaymentType type,
    String description,
    LocalDateTime requestTime
) {
    // Compact constructor with validation
    public PaymentRequest {
        if (paymentId == null || paymentId.isBlank()) {
            throw new IllegalArgumentException("Payment ID is required");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (fromAccount == null || !isValidAccountNumber(fromAccount)) {
            throw new IllegalArgumentException("Invalid from account");
        }
        if (toAccount == null || !isValidAccountNumber(toAccount)) {
            throw new IllegalArgumentException("Invalid to account");
        }
        Objects.requireNonNull(type, "Payment type is required");
        Objects.requireNonNull(requestTime, "Request time is required");
    }
    
    private static boolean isValidAccountNumber(String account) {
        return account.matches("\\d{10,12}"); // 10-12 digits
    }
}

public record CustomerProfile(
    String customerId,
    String name,
    BigDecimal creditScore,
    BigDecimal annualIncome,
    LocalDate dateOfBirth,
    Address address,
    ContactInfo contactInfo
) {
    public CustomerProfile {
        Objects.requireNonNull(customerId, "Customer ID is required");
        Objects.requireNonNull(name, "Name is required");
        
        if (creditScore.compareTo(BigDecimal.valueOf(300)) < 0 || 
            creditScore.compareTo(BigDecimal.valueOf(850)) > 0) {
            throw new IllegalArgumentException("Credit score must be between 300-850");
        }
        
        if (annualIncome.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Annual income cannot be negative");
        }
    }
}
```

### Pattern Matching with Records

```java
public class PaymentValidator {
    
    public ValidationResult validatePayment(PaymentRequest request) {
        return switch (request) {
            // Validate amount constraints
            case PaymentRequest(var id, var amount, var from, var to, var type, var desc, var time) 
                when amount.compareTo(BigDecimal.ZERO) <= 0 -> {
                yield ValidationResult.invalid("Amount must be positive");
            }
            
            // Large international transfers require approval
            case PaymentRequest(var id, var amount, var from, var to, INTERNATIONAL, var desc, var time) 
                when amount.compareTo(BigDecimal.valueOf(50000)) > 0 -> {
                yield ValidationResult.requiresApproval(
                    "Large international transfer requires manual approval",
                    List.of("MANAGER_APPROVAL", "COMPLIANCE_CHECK")
                );
            }
            
            // High-risk countries require additional checks
            case PaymentRequest(var id, var amount, var from, var to, INTERNATIONAL, var desc, var time) 
                when isHighRiskCountry(extractCountryCode(to)) -> {
                yield ValidationResult.requiresApproval(
                    "Payment to high-risk country requires enhanced due diligence",
                    List.of("AML_CHECK", "SANCTIONS_SCREENING")
                );
            }
            
            // Same account transfer validation
            case PaymentRequest(var id, var amount, var from, var to, var type, var desc, var time) 
                when from.equals(to) -> {
                yield ValidationResult.invalid("Cannot transfer to the same account");
            }
            
            // Valid payment
            case PaymentRequest(var id, var amount, var from, var to, var type, var desc, var time) 
                when isValidAccountFormat(from) && isValidAccountFormat(to) -> {
                yield ValidationResult.valid("Payment validation passed");
            }
            
            default -> ValidationResult.invalid("Invalid payment request format");
        };
    }
}
```

## Banking Code Examples

### Complete Loan Processing Example

```java
@Service
public class ComprehensiveLoanProcessor {
    
    private final ExecutorService virtualExecutor = 
        Executors.newVirtualThreadPerTaskExecutor();
    
    public LoanProcessingResult processLoanApplication(LoanApplicationRequest request) {
        return switch (request) {
            case LoanApplicationRequest(var customer, var amount, var term, PERSONAL_LOAN, var purpose) 
                when amount.compareTo(BigDecimal.valueOf(100000)) <= 0 -> {
                yield processPersonalLoan(customer, amount, term, purpose);
            }
            
            case LoanApplicationRequest(var customer, var amount, var term, MORTGAGE, var purpose) 
                when amount.compareTo(BigDecimal.valueOf(10000000)) <= 0 -> {
                yield processMortgageLoan(customer, amount, term, purpose);
            }
            
            case LoanApplicationRequest(var customer, var amount, var term, BUSINESS_LOAN, var purpose) 
                when customer instanceof BusinessCustomer business -> {
                yield processBusinessLoan(business, amount, term, purpose);
            }
            
            default -> LoanProcessingResult.rejected("Unsupported loan type or amount");
        };
    }
    
    private LoanProcessingResult processPersonalLoan(
            Customer customer, BigDecimal amount, LoanTerm term, String purpose) {
        
        // Parallel processing with Virtual Threads
        var creditCheckFuture = CompletableFuture.supplyAsync(
            () -> performCreditCheck(customer), virtualExecutor);
        
        var incomeVerificationFuture = CompletableFuture.supplyAsync(
            () -> verifyIncome(customer), virtualExecutor);
        
        var riskAssessmentFuture = CompletableFuture.supplyAsync(
            () -> assessPersonalLoanRisk(customer, amount, term), virtualExecutor);
        
        var fraudCheckFuture = CompletableFuture.supplyAsync(
            () -> performFraudCheck(customer), virtualExecutor);
        
        // Wait for all checks to complete
        var allChecks = CompletableFuture.allOf(
            creditCheckFuture, incomeVerificationFuture, 
            riskAssessmentFuture, fraudCheckFuture
        );
        
        try {
            allChecks.get(30, TimeUnit.SECONDS); // 30-second timeout
            
            var creditScore = creditCheckFuture.join();
            var incomeVerified = incomeVerificationFuture.join();
            var riskLevel = riskAssessmentFuture.join();
            var fraudRisk = fraudCheckFuture.join();
            
            return makeLoanDecision(customer, amount, term, 
                                  creditScore, incomeVerified, riskLevel, fraudRisk);
            
        } catch (TimeoutException e) {
            return LoanProcessingResult.error("Processing timeout - please try again");
        } catch (Exception e) {
            return LoanProcessingResult.error("Processing error: " + e.getMessage());
        }
    }
    
    private LoanProcessingResult makeLoanDecision(
            Customer customer, BigDecimal amount, LoanTerm term,
            CreditScore creditScore, boolean incomeVerified, 
            RiskLevel riskLevel, FraudRisk fraudRisk) {
        
        return switch (customer) {
            case PremiumCustomer(var id, var name, var score, var income, var dob, 
                               var years, var assets) 
                when creditScore.value() >= 750 && incomeVerified && 
                     riskLevel == RiskLevel.LOW && fraudRisk == FraudRisk.LOW -> {
                
                var interestRate = calculatePremiumRate(creditScore, years);
                var conditions = List.of("Premium customer - standard terms");
                
                yield LoanProcessingResult.approved(amount, interestRate, term, conditions);
            }
            
            case StandardCustomer(var id, var name, var score, var income, var dob, 
                                var employment, var tenure) 
                when creditScore.value() >= 650 && incomeVerified && 
                     riskLevel != RiskLevel.HIGH && fraudRisk == FraudRisk.LOW -> {
                
                var interestRate = calculateStandardRate(creditScore, employment);
                var conditions = generateStandardConditions(riskLevel);
                
                yield LoanProcessingResult.approved(amount, interestRate, term, conditions);
            }
            
            default -> {
                var reasons = buildRejectionReasons(creditScore, incomeVerified, riskLevel, fraudRisk);
                yield LoanProcessingResult.rejected(reasons);
            }
        };
    }
}
```

### Real-time Payment Processing

```java
@Service
public class RealTimePaymentProcessor {
    
    private final ExecutorService paymentExecutor = 
        Executors.newVirtualThreadPerTaskExecutor();
    
    private final SequencedSet<PaymentRecord> paymentHistory = 
        new ConcurrentSkipListSet<>(Comparator.comparing(PaymentRecord::timestamp));
    
    public PaymentResult processPayment(PaymentRequest request) {
        // Record start of processing
        var startTime = LocalDateTime.now();
        
        try {
            // Validate payment request using pattern matching
            var validationResult = validatePaymentRequest(request);
            if (!validationResult.isValid()) {
                return PaymentResult.failed(validationResult.errors());
            }
            
            // Parallel validation and processing
            var validationFuture = CompletableFuture.supplyAsync(
                () -> performDetailedValidation(request), paymentExecutor);
            
            var balanceCheckFuture = CompletableFuture.supplyAsync(
                () -> checkAccountBalance(request), paymentExecutor);
            
            var fraudCheckFuture = CompletableFuture.supplyAsync(
                () -> performRealtimeFraudCheck(request), paymentExecutor);
            
            var complianceCheckFuture = CompletableFuture.supplyAsync(
                () -> performComplianceCheck(request), paymentExecutor);
            
            // Wait for all validations
            var allValidations = CompletableFuture.allOf(
                validationFuture, balanceCheckFuture, 
                fraudCheckFuture, complianceCheckFuture
            );
            
            allValidations.get(5, TimeUnit.SECONDS); // 5-second timeout for real-time
            
            // Check all validation results
            var validationPassed = validationFuture.join();
            var balanceAvailable = balanceCheckFuture.join();
            var fraudCheckPassed = fraudCheckFuture.join();
            var complianceCheckPassed = complianceCheckFuture.join();
            
            if (!validationPassed || !balanceAvailable || 
                !fraudCheckPassed || !complianceCheckPassed) {
                return PaymentResult.failed(collectFailureReasons(
                    validationPassed, balanceAvailable, 
                    fraudCheckPassed, complianceCheckPassed));
            }
            
            // Process payment
            var paymentResult = executePayment(request);
            
            // Record successful payment
            var paymentRecord = new PaymentRecord(
                request.paymentId(),
                request.amount(),
                request.fromAccount(),
                request.toAccount(),
                startTime,
                LocalDateTime.now(),
                PaymentStatus.COMPLETED
            );
            
            paymentHistory.addLast(paymentRecord);
            
            return paymentResult;
            
        } catch (TimeoutException e) {
            return PaymentResult.failed(List.of("Payment processing timeout"));
        } catch (Exception e) {
            return PaymentResult.failed(List.of("Payment processing error: " + e.getMessage()));
        }
    }
    
    public PaymentAnalytics getPaymentAnalytics(Duration period) {
        var cutoffTime = LocalDateTime.now().minus(period);
        
        var recentPayments = paymentHistory.stream()
            .filter(payment -> payment.timestamp().isAfter(cutoffTime))
            .toList();
        
        var totalAmount = recentPayments.stream()
            .map(PaymentRecord::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        var averageProcessingTime = recentPayments.stream()
            .mapToLong(payment -> Duration.between(
                payment.startTime(), payment.endTime()).toMillis())
            .average()
            .orElse(0.0);
        
        return new PaymentAnalytics(
            recentPayments.size(),
            totalAmount,
            averageProcessingTime,
            paymentHistory.getFirst(), // First payment ever - O(1)
            paymentHistory.getLast()   // Latest payment - O(1)
        );
    }
}
```

## Performance Optimization

### Virtual Threads Configuration

```java
@Configuration
public class VirtualThreadsConfiguration {
    
    @Bean("loanProcessingExecutor")
    public ExecutorService loanProcessingExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
    
    @Bean("paymentProcessingExecutor")
    public ExecutorService paymentProcessingExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
    
    @Bean("riskAssessmentExecutor")
    public ExecutorService riskAssessmentExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
    
    // Configure async processing
    @Configuration
    @EnableAsync
    public static class AsyncConfiguration implements AsyncConfigurer {
        
        @Override
        public Executor getAsyncExecutor() {
            return Executors.newVirtualThreadPerTaskExecutor();
        }
        
        @Override
        public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
            return (ex, method, params) -> {
                log.error("Async execution error in method: {}", method.getName(), ex);
            };
        }
    }
}
```

### JVM Tuning for Java 21

```properties
# JVM options for production
-XX:+UseZGC
-XX:+UseLargePages
-XX:MaxGCPauseMillis=10
-Xms4g
-Xmx16g
--enable-preview
-Djdk.virtualThreadScheduler.parallelism=32
-Djdk.virtualThreadScheduler.maxPoolSize=512
```

### Performance Monitoring

```java
@Component
public class Java21PerformanceMonitor {
    
    private final MeterRegistry meterRegistry;
    
    @EventListener
    public void onVirtualThreadCreated(VirtualThreadCreatedEvent event) {
        Counter.builder("virtual.threads.created")
            .tag("pool.name", event.getPoolName())
            .register(meterRegistry)
            .increment();
    }
    
    @EventListener
    public void onPatternMatchingExecution(PatternMatchingEvent event) {
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("pattern.matching.execution.time")
            .tag("pattern.type", event.getPatternType())
            .register(meterRegistry));
    }
    
    @EventListener
    public void onSequencedCollectionOperation(SequencedCollectionEvent event) {
        Gauge.builder("sequenced.collections.size")
            .tag("collection.type", event.getCollectionType())
            .register(meterRegistry, this, monitor -> event.getCollectionSize());
    }
}
```

## Debugging and Troubleshooting

### Virtual Threads Debugging

```java
// Enable Virtual Threads monitoring
public class VirtualThreadsDebugger {
    
    public void debugVirtualThreads() {
        // Check Virtual Threads status
        var activeVirtualThreads = Thread.getAllStackTraces().keySet().stream()
            .filter(Thread::isVirtual)
            .count();
        
        System.out.println("Active Virtual Threads: " + activeVirtualThreads);
        
        // Monitor Virtual Thread creation
        Thread.ofVirtual().name("debug-virtual-thread").start(() -> {
            System.out.println("Virtual Thread: " + Thread.currentThread());
            System.out.println("Is Virtual: " + Thread.currentThread().isVirtual());
        });
    }
    
    // Custom exception handling for Virtual Threads
    public void handleVirtualThreadException(Runnable task) {
        Thread.ofVirtual().uncaughtExceptionHandler((thread, exception) -> {
            log.error("Uncaught exception in virtual thread: {}", thread.getName(), exception);
        }).start(task);
    }
}
```

### Pattern Matching Debugging

```java
public class PatternMatchingDebugger {
    
    public void debugPatternMatching(Customer customer) {
        var result = switch (customer) {
            case PremiumCustomer premium -> {
                System.out.println("Matched PremiumCustomer: " + premium);
                yield "Premium";
            }
            case StandardCustomer standard -> {
                System.out.println("Matched StandardCustomer: " + standard);
                yield "Standard";
            }
            default -> {
                System.out.println("No specific pattern matched for: " + customer.getClass());
                yield "Unknown";
            }
        };
        
        System.out.println("Pattern matching result: " + result);
    }
}
```

## Best Practices

### Code Organization

```java
// ✅ Good: Separate executors for different domains
@Service
public class BankingOperationsService {
    private final ExecutorService loanExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final ExecutorService paymentExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final ExecutorService riskExecutor = Executors.newVirtualThreadPerTaskExecutor();
}

// ❌ Bad: Single executor for everything
@Service
public class BadBankingOperationsService {
    private final ExecutorService sharedExecutor = Executors.newVirtualThreadPerTaskExecutor();
}
```

### Error Handling

```java
// ✅ Good: Comprehensive error handling
public PaymentResult processPayment(PaymentRequest request) {
    try {
        return switch (request) {
            case PaymentRequest(var id, var amount, var from, var to, var type, var desc, var time) 
                when isValidPayment(request) -> {
                yield executePayment(request);
            }
            default -> PaymentResult.failed(List.of("Invalid payment request"));
        };
    } catch (PatternMatchException e) {
        log.error("Pattern matching error for payment: {}", request.paymentId(), e);
        return PaymentResult.failed(List.of("Payment validation error"));
    } catch (Exception e) {
        log.error("Unexpected error processing payment: {}", request.paymentId(), e);
        return PaymentResult.failed(List.of("Internal processing error"));
    }
}
```

### Testing Strategies

```java
@Test
void shouldProcessPaymentWithVirtualThreads() {
    // Given
    var request = new PaymentRequest(/*... parameters ...*/); 
    var processor = new PaymentProcessor();
    
    // When
    var startTime = System.currentTimeMillis();
    var result = processor.processPayment(request);
    var endTime = System.currentTimeMillis();
    
    // Then
    assertThat(result.isSuccessful()).isTrue();
    assertThat(endTime - startTime).isLessThan(5000); // < 5 seconds
    
    // Verify Virtual Threads were used
    verify(virtualThreadsMetrics).incrementActiveThreads();
}

@Test
void shouldMatchCustomerPatternsCorrectly() {
    // Given
    var premiumCustomer = new PremiumCustomer(/*... parameters ...*/);
    var riskAssessor = new RiskAssessor();
    
    // When
    var assessment = riskAssessor.assessRisk(premiumCustomer);
    
    // Then
    assertThat(assessment.riskLevel()).isEqualTo(RiskLevel.LOW);
    assertThat(assessment.interestRate()).isLessThan(BigDecimal.valueOf(0.05));
}
```

## Hands-On Exercises

### Exercise 1: Virtual Threads Payment Processing

**Task**: Create a payment processing service that can handle 1000 concurrent payments using Virtual Threads.

**Requirements**:
1. Create a `PaymentProcessor` class
2. Use Virtual Threads for concurrent processing
3. Include proper error handling and timeouts
4. Measure and log processing time

**Template**:
```java
public class PaymentProcessor {
    // TODO: Implement using Virtual Threads
    public List<PaymentResult> processPayments(List<PaymentRequest> payments) {
        // Your implementation here
    }
}
```

### Exercise 2: Pattern Matching Risk Assessment

**Task**: Implement a risk assessment engine using pattern matching for different customer types.

**Requirements**:
1. Handle `PremiumCustomer`, `StandardCustomer`, and `YoungProfessional`
2. Use pattern matching with guard conditions
3. Calculate appropriate interest rates
4. Include rejection scenarios

**Template**:
```java
public class RiskAssessmentEngine {
    // TODO: Implement using pattern matching
    public RiskAssessment assessCustomer(Customer customer, LoanRequest request) {
        return switch (customer) {
            // Your pattern matching implementation here
        };
    }
}
```

### Exercise 3: Sequenced Collections Transaction History

**Task**: Create a transaction history manager using Sequenced Collections.

**Requirements**:
1. Maintain ordered transaction history
2. Provide O(1) access to first and last transactions
3. Support transaction queries by date range
4. Calculate running balances efficiently

**Template**:
```java
public class TransactionHistoryManager {
    private final SequencedSet<Transaction> transactions = new LinkedHashSet<>();
    
    // TODO: Implement methods using Sequenced Collections
    public void addTransaction(Transaction transaction) {
        // Your implementation here
    }
    
    public Transaction getLatestTransaction() {
        // Your implementation here
    }
    
    public BigDecimal calculateRunningBalance() {
        // Your implementation here
    }
}
```

### Exercise Solutions

<details>
<summary>Click to reveal solutions</summary>

#### Exercise 1 Solution:
```java
public class PaymentProcessor {
    private final ExecutorService virtualExecutor = 
        Executors.newVirtualThreadPerTaskExecutor();
    
    public List<PaymentResult> processPayments(List<PaymentRequest> payments) {
        var startTime = System.currentTimeMillis();
        
        var futures = payments.stream()
            .map(payment -> CompletableFuture.supplyAsync(
                () -> processIndividualPayment(payment), virtualExecutor))
            .toList();
        
        var results = futures.stream()
            .map(future -> {
                try {
                    return future.get(30, TimeUnit.SECONDS);
                } catch (Exception e) {
                    return PaymentResult.failed(List.of("Processing timeout or error"));
                }
            })
            .toList();
        
        var endTime = System.currentTimeMillis();
        System.out.printf("Processed %d payments in %d ms%n", 
                         payments.size(), endTime - startTime);
        
        return results;
    }
    
    private PaymentResult processIndividualPayment(PaymentRequest payment) {
        // Simulate payment processing
        try {
            Thread.sleep(100); // 100ms processing time
            return PaymentResult.successful(payment.paymentId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return PaymentResult.failed(List.of("Processing interrupted"));
        }
    }
}
```

#### Exercise 2 Solution:
```java
public class RiskAssessmentEngine {
    
    public RiskAssessment assessCustomer(Customer customer, LoanRequest request) {
        return switch (customer) {
            case PremiumCustomer(var id, var name, var creditScore, var income, 
                               var dob, var years, var assets) 
                when creditScore >= 750 && income.compareTo(request.amount().multiply(BigDecimal.valueOf(3))) > 0 -> {
                yield new RiskAssessment(
                    RiskLevel.LOW,
                    calculatePremiumRate(creditScore, years),
                    "Premium customer - excellent credit"
                );
            }
            
            case StandardCustomer(var id, var name, var creditScore, var income, 
                                var dob, var employment, var tenure) 
                when creditScore >= 650 && employment.isStable() -> {
                yield new RiskAssessment(
                    RiskLevel.MEDIUM,
                    calculateStandardRate(creditScore, tenure),
                    "Standard customer - good credit"
                );
            }
            
            case YoungProfessional(var id, var name, var creditScore, var income, 
                                 var dob, var education, var potential) 
                when creditScore >= 700 && potential.isHigh() -> {
                yield new RiskAssessment(
                    RiskLevel.LOW,
                    calculateYouthRate(creditScore, education),
                    "Young professional - high potential"
                );
            }
            
            default -> new RiskAssessment(
                RiskLevel.HIGH,
                null,
                "Does not meet lending criteria"
            );
        };
    }
    
    private BigDecimal calculatePremiumRate(int creditScore, int years) {
        var baseRate = BigDecimal.valueOf(0.035); // 3.5%
        var creditAdjustment = BigDecimal.valueOf((750 - creditScore) * 0.0001);
        var loyaltyDiscount = BigDecimal.valueOf(years * 0.0001);
        return baseRate.add(creditAdjustment).subtract(loyaltyDiscount);
    }
}
```

#### Exercise 3 Solution:
```java
public class TransactionHistoryManager {
    private final SequencedSet<Transaction> transactions = new LinkedHashSet<>();
    
    public void addTransaction(Transaction transaction) {
        transactions.addLast(transaction);
        
        // Maintain size limit
        while (transactions.size() > 10000) {
            transactions.removeFirst();
        }
    }
    
    public Transaction getLatestTransaction() {
        return transactions.isEmpty() ? null : transactions.getLast(); // O(1)
    }
    
    public Transaction getFirstTransaction() {
        return transactions.isEmpty() ? null : transactions.getFirst(); // O(1)
    }
    
    public BigDecimal calculateRunningBalance() {
        return transactions.stream()
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public List<Transaction> getTransactionsInDateRange(LocalDate start, LocalDate end) {
        return transactions.stream()
            .filter(t -> !t.getDate().isBefore(start) && !t.getDate().isAfter(end))
            .toList();
    }
    
    public SequencedSet<Transaction> getRecentTransactions(int count) {
        return transactions.reversed() // Most recent first
            .stream()
            .limit(count)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
```

</details>

## Conclusion

Java 21 brings powerful features that are particularly well-suited for banking applications:

- **Virtual Threads** enable massive scalability for I/O-intensive operations
- **Pattern Matching** makes business logic more readable and type-safe
- **Sequenced Collections** provide efficient ordered data management
- **Record Patterns** enable robust data validation and extraction

By following the patterns and best practices outlined in this guide, your team can effectively leverage these features to build high-performance, maintainable banking applications.

### Next Steps

1. **Practice**: Work through the hands-on exercises
2. **Experiment**: Try implementing these patterns in your current codebase
3. **Measure**: Use the performance monitoring tools to validate improvements
4. **Share**: Discuss learnings with your team and contribute to the knowledge base

### Additional Resources

- [Java 21 Documentation](https://docs.oracle.com/en/java/javase/21/)
- [Virtual Threads Guide](https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html)
- [Pattern Matching Reference](https://docs.oracle.com/en/java/javase/21/language/pattern-matching.html)
- [Performance Monitoring Guide](./JAVA_21_PERFORMANCE_ANALYSIS.md)
- [Deployment Runbook](./JAVA_21_DEPLOYMENT_RUNBOOK.md)

---

**Training Version**: 1.0  
**Last Updated**: $(date)  
**Target Audience**: Banking Development Teams  
**Prerequisites**: Java 17+ experience, Spring Boot knowledge