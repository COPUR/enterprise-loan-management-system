# Shared Kernel - Domain Foundation

## 🎯 Overview

The **Shared Kernel** contains common domain concepts, value objects, and foundational patterns shared across all bounded contexts. It implements **Domain-Driven Design** principles with **security-first patterns** and **PCI-DSS v4.0 compliance** at the domain level.

## 🏗️ Architecture Pattern

```
┌─────────────────────────────────────────┐
│            Shared Kernel                │
├─────────────────┬───────────────────────┤
│ Domain Patterns │    Security Kernel    │
│ • Value Objects │ • Encryption Services│
│ • Entities      │ • Audit Framework    │
│ • Events        │ • Validation Rules   │
│ • Aggregates    │ • Access Control     │
└─────────────────┴───────────────────────┘
                  │
    ┌─────────────┼─────────────┐
    │             │             │
┌───▼────┐  ┌────▼────┐  ┌────▼────┐
│ Loan   │  │ Payment │  │Customer │
│Context │  │ Context │  │ Context │
└────────┘  └─────────┘  └─────────┘
```

## 💎 Core Domain Value Objects

### Money - Currency Handling
```java
/**
 * Money value object with precision handling and currency support
 * PCI-DSS compliant with audit logging for financial amounts
 */
@Value
@Builder
@JsonDeserialize(builder = Money.MoneyBuilder.class)
public class Money implements Comparable<Money> {
    
    @NotNull
    private final BigDecimal value;
    
    @NotNull
    @Pattern(regexp = "[A-Z]{3}")
    private final String currency;
    
    // Factory methods
    public static Money of(BigDecimal value, String currency) {
        validateAmount(value);
        validateCurrency(currency);
        return new Money(value.setScale(2, RoundingMode.HALF_UP), currency);
    }
    
    public static Money of(double value) {
        return of(BigDecimal.valueOf(value), "USD");
    }
    
    // Arithmetic operations with audit logging
    @AuditOperation(operation = "MONEY_ADD")
    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(this.value.add(other.value), this.currency);
    }
    
    @AuditOperation(operation = "MONEY_SUBTRACT")
    public Money subtract(Money other) {
        validateSameCurrency(other);
        var result = this.value.subtract(other.value);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientFundsException("Negative amount not allowed");
        }
        return new Money(result, this.currency);
    }
    
    // Security validations
    private static void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("Amount cannot have more than 2 decimal places");
        }
        // Prevent extreme values (anti-fraud measure)
        if (amount.compareTo(new BigDecimal("999999999.99")) > 0) {
            throw new IllegalArgumentException("Amount exceeds maximum allowed value");
        }
    }
    
    // Sensitive data handling - mask for logging
    @JsonIgnore
    public String getMaskedAmount() {
        if (value.compareTo(new BigDecimal("10000")) > 0) {
            return "*****.** " + currency;
        }
        return value.toString() + " " + currency;
    }
    
    @Override
    public int compareTo(Money other) {
        validateSameCurrency(other);
        return this.value.compareTo(other.value);
    }
}
```

### CustomerId - Secure Identity
```java
/**
 * Customer identifier with encryption and privacy protection
 * GDPR compliant with pseudonymization support
 */
@Value
@JsonDeserialize(builder = CustomerId.CustomerIdBuilder.class)
public class CustomerId implements Identifier {
    
    private static final String PREFIX = "CUST";
    private static final Pattern VALID_FORMAT = Pattern.compile("^CUST-[A-Z0-9]{8}-[A-Z0-9]{4}-[A-Z0-9]{4}$");
    
    @NotNull
    @Pattern(regexp = "^CUST-[A-Z0-9]{8}-[A-Z0-9]{4}-[A-Z0-9]{4}$")
    private final String value;
    
    // Factory methods
    public static CustomerId generate() {
        var uuid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        var formatted = String.format("%s-%s-%s-%s", 
            PREFIX, 
            uuid.substring(0, 8),
            uuid.substring(8, 12),
            uuid.substring(12, 16)
        );
        return new CustomerId(formatted);
    }
    
    public static CustomerId of(String value) {
        validateFormat(value);
        return new CustomerId(value);
    }
    
    // Pseudonymization for GDPR compliance
    public CustomerId pseudonymize(PseudonymizationKey key) {
        var pseudonym = pseudonymizationService.pseudonymize(this.value, key);
        return new CustomerId(pseudonym);
    }
    
    // PII handling
    @JsonIgnore
    public String getMaskedValue() {
        return value.substring(0, 8) + "-****-****";
    }
    
    private static void validateFormat(String value) {
        if (value == null || !VALID_FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid CustomerId format");
        }
    }
    
    @Override
    public String toString() {
        return value;
    }
}
```

### EncryptedPersonalData - PII Protection
```java
/**
 * Encrypted personal data container for PII protection
 * Automatic encryption/decryption with field-level security
 */
@Value
@Builder
public class EncryptedPersonalData<T> {
    
    @NotNull
    private final String encryptedValue;
    
    @NotNull
    private final String keyId;
    
    @NotNull
    private final Instant encryptedAt;
    
    private final Class<T> dataType;
    
    // Factory method with automatic encryption
    public static <T> EncryptedPersonalData<T> encrypt(T plainValue, Class<T> type) {
        var encryptionKey = keyManagementService.getCurrentKey();
        var encryptedBytes = encryptionService.encrypt(
            serialize(plainValue), 
            encryptionKey
        );
        
        return EncryptedPersonalData.<T>builder()
            .encryptedValue(Base64.getEncoder().encodeToString(encryptedBytes))
            .keyId(encryptionKey.getId())
            .encryptedAt(Instant.now())
            .dataType(type)
            .build();
    }
    
    // Controlled decryption with audit logging
    @AuditOperation(operation = "PII_DECRYPT", severity = AuditSeverity.HIGH)
    public T decrypt(DecryptionContext context) {
        validateDecryptionAuthorization(context);
        
        var encryptionKey = keyManagementService.getKey(keyId);
        var decryptedBytes = encryptionService.decrypt(
            Base64.getDecoder().decode(encryptedValue),
            encryptionKey
        );
        
        auditService.logPIIAccess(context.getUserId(), keyId, dataType);
        
        return deserialize(decryptedBytes, dataType);
    }
    
    // Safe comparison without decryption
    public boolean matches(T plainValue) {
        var encryptedComparison = encrypt(plainValue, dataType);
        return secureCompare(this.encryptedValue, encryptedComparison.encryptedValue);
    }
    
    // GDPR right to be forgotten
    @AuditOperation(operation = "PII_FORGOTTEN")
    public EncryptedPersonalData<T> forget() {
        return EncryptedPersonalData.<T>builder()
            .encryptedValue("FORGOTTEN")
            .keyId("FORGOTTEN")
            .encryptedAt(Instant.now())
            .dataType(dataType)
            .build();
    }
    
    @Override
    public String toString() {
        return String.format("EncryptedPersonalData[type=%s, keyId=%s, encryptedAt=%s]", 
            dataType.getSimpleName(), keyId, encryptedAt);
    }
}
```

## 🔒 Security Kernel

### AuditableEntity - Comprehensive Auditing
```java
/**
 * Base entity with comprehensive audit trail
 * PCI-DSS Requirement 10 compliance
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;
    
    @Column(name = "version", nullable = false)
    @Version
    private Long version;
    
    @Column(name = "audit_hash")
    private String auditHash;
    
    // Integrity verification
    @PrePersist
    @PreUpdate
    protected void calculateAuditHash() {
        this.auditHash = calculateEntityHash();
    }
    
    private String calculateEntityHash() {
        var data = String.format("%s|%s|%s|%s|%d", 
            getId(), createdAt, updatedAt, updatedBy, version);
        return hashingService.calculateSHA256(data);
    }
    
    public boolean verifyIntegrity() {
        return Objects.equals(this.auditHash, calculateEntityHash());
    }
    
    // Audit trail methods
    public AuditTrail getAuditTrail() {
        return AuditTrail.builder()
            .entityId(getId().toString())
            .createdAt(createdAt)
            .createdBy(createdBy)
            .updatedAt(updatedAt)
            .updatedBy(updatedBy)
            .version(version)
            .integrityHash(auditHash)
            .build();
    }
    
    protected abstract Identifier getId();
}
```

### SecureValidationRules - Input Security
```java
/**
 * Security-first validation rules for all inputs
 * OWASP compliance with injection prevention
 */
@Component
public class SecureValidationRules {
    
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i).*(union|select|insert|delete|update|drop|create|alter|exec|execute|script|javascript|vbscript).*");
    
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i).*(<script|javascript:|vbscript:|onload|onerror|onclick).*");
    
    public static boolean isValidAmount(BigDecimal amount) {
        if (amount == null) return false;
        if (amount.compareTo(BigDecimal.ZERO) < 0) return false;
        if (amount.scale() > 2) return false;
        if (amount.precision() > 12) return false; // Prevent overflow attacks
        return true;
    }
    
    public static boolean isValidCustomerName(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        if (name.length() > 100) return false; // Prevent buffer overflow
        if (containsSqlInjection(name)) return false;
        if (containsXSS(name)) return false;
        if (containsControlCharacters(name)) return false;
        return true;
    }
    
    public static boolean isValidAccountNumber(String accountNumber) {
        if (accountNumber == null) return false;
        // Must be tokenized format - never accept raw account numbers
        if (!accountNumber.startsWith("tok_")) return false;
        if (accountNumber.length() < 20 || accountNumber.length() > 50) return false;
        return accountNumber.matches("^tok_[A-Za-z0-9_-]+$");
    }
    
    public static String sanitizeInput(String input) {
        if (input == null) return null;
        
        // Remove potential injection attempts
        var sanitized = input
            .replaceAll("(?i)<script[^>]*>.*?</script>", "")
            .replaceAll("(?i)<.*?>", "")
            .replaceAll("(?i)(union|select|insert|delete|update|drop)", "")
            .trim();
        
        // Limit length to prevent DoS
        if (sanitized.length() > 1000) {
            sanitized = sanitized.substring(0, 1000);
        }
        
        return sanitized;
    }
    
    private static boolean containsSqlInjection(String input) {
        return SQL_INJECTION_PATTERN.matcher(input).matches();
    }
    
    private static boolean containsXSS(String input) {
        return XSS_PATTERN.matcher(input).matches();
    }
    
    private static boolean containsControlCharacters(String input) {
        return input.chars().anyMatch(Character::isISOControl);
    }
}
```

## 🎭 Domain Events Framework

### Base Domain Event
```java
/**
 * Base domain event with security and audit integration
 * Event sourcing compatible with GDPR compliance
 */
public sealed interface DomainEvent 
    permits CustomerEvent, LoanEvent, PaymentEvent, SecurityEvent {
    
    String getEventId();
    String getAggregateId();
    String getEventType();
    Instant getOccurredAt();
    String getCorrelationId();
    Map<String, Object> getMetadata();
    
    // Security context
    default String getUserId() {
        return (String) getMetadata().get("userId");
    }
    
    default String getSessionId() {
        return (String) getMetadata().get("sessionId");
    }
    
    default String getIpAddress() {
        return (String) getMetadata().get("ipAddress");
    }
    
    // GDPR compliance
    default boolean containsPII() {
        return Boolean.parseBoolean((String) getMetadata().get("containsPII"));
    }
    
    default DomainEvent anonymize() {
        if (!containsPII()) return this;
        
        var anonymizedMetadata = new HashMap<>(getMetadata());
        anonymizedMetadata.put("userId", "ANONYMIZED");
        anonymizedMetadata.put("ipAddress", "ANONYMIZED");
        anonymizedMetadata.put("containsPII", "false");
        
        return createAnonymizedCopy(anonymizedMetadata);
    }
    
    DomainEvent createAnonymizedCopy(Map<String, Object> anonymizedMetadata);
}
```

### Security Event Types
```java
/**
 * Security-specific domain events for audit and monitoring
 * PCI-DSS Requirement 10.2 compliance
 */
public sealed interface SecurityEvent extends DomainEvent
    permits AuthenticationAttempted, AuthorizationFailed, DataAccessed, 
            SensitiveDataDecrypted, SecurityViolationDetected {
    
    SecurityLevel getSecurityLevel();
    String getThreatCategory();
    Map<String, String> getSecurityContext();
}

@Value
@Builder
public class SensitiveDataDecrypted implements SecurityEvent {
    String eventId;
    String aggregateId;
    String dataType;
    String keyId;
    String userId;
    String purpose;
    Instant occurredAt;
    String correlationId;
    Map<String, Object> metadata;
    
    @Override
    public String getEventType() { return "SensitiveDataDecrypted"; }
    
    @Override
    public SecurityLevel getSecurityLevel() { return SecurityLevel.HIGH; }
    
    @Override
    public String getThreatCategory() { return "DATA_ACCESS"; }
    
    @Override
    public Map<String, String> getSecurityContext() {
        return Map.of(
            "dataType", dataType,
            "keyId", keyId,
            "purpose", purpose,
            "riskLevel", "HIGH"
        );
    }
}
```

## 🏛️ Aggregate Root Pattern

### Secure Aggregate Root Base
```java
/**
 * Base aggregate root with security and event sourcing
 * Domain-driven design with security-first approach
 */
public abstract class SecureAggregateRoot<ID extends Identifier> extends AuditableEntity {
    
    @Transient
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    
    @Transient
    private final SecurityContext securityContext;
    
    protected SecureAggregateRoot() {
        this.securityContext = SecurityContextHolder.getContext();
    }
    
    // Event handling with security validation
    protected void recordDomainEvent(DomainEvent event) {
        validateEventSecurity(event);
        enrichEventWithSecurityContext(event);
        domainEvents.add(event);
        auditService.logDomainEvent(event);
    }
    
    public List<DomainEvent> getUncommittedEvents() {
        return Collections.unmodifiableList(domainEvents);
    }
    
    public void markEventsAsCommitted() {
        domainEvents.clear();
    }
    
    // Security validations
    private void validateEventSecurity(DomainEvent event) {
        if (event.containsPII() && !hasPrivacyPermission()) {
            throw new SecurityException("Insufficient permissions to create PII event");
        }
        
        if (event instanceof SecurityEvent securityEvent) {
            if (securityEvent.getSecurityLevel() == SecurityLevel.CRITICAL) {
                validateCriticalEventPermission();
            }
        }
    }
    
    private void enrichEventWithSecurityContext(DomainEvent event) {
        var metadata = new HashMap<>(event.getMetadata());
        metadata.put("userId", getCurrentUserId());
        metadata.put("sessionId", getCurrentSessionId());
        metadata.put("ipAddress", getCurrentIpAddress());
        metadata.put("timestamp", Instant.now().toString());
        
        // Don't modify the original event, this would need to be handled
        // differently in a real implementation
    }
    
    // Abstract methods
    public abstract ID getId();
    
    // Security helper methods
    private boolean hasPrivacyPermission() {
        return securityContext.getAuthentication()
            .getAuthorities()
            .stream()
            .anyMatch(auth -> auth.getAuthority().equals("PRIVACY_DATA_ACCESS"));
    }
    
    private void validateCriticalEventPermission() {
        if (!hasRole("SECURITY_ADMIN")) {
            throw new SecurityException("Critical security events require SECURITY_ADMIN role");
        }
    }
    
    private boolean hasRole(String role) {
        return securityContext.getAuthentication()
            .getAuthorities()
            .stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_" + role));
    }
}
```

## 🧪 Testing Support

### Secure Test Data Builders
```java
/**
 * Test data builders with security considerations
 * Never generate real PII in tests
 */
public class TestDataBuilder {
    
    public static Money validAmount() {
        return Money.of(1500.00);
    }
    
    public static Money largeAmount() {
        return Money.of(50000.00);
    }
    
    public static CustomerId validCustomerId() {
        return CustomerId.of("CUST-TEST1234-T5ST-TE5T");
    }
    
    public static EncryptedPersonalData<String> encryptedTestSSN() {
        // Use fake SSN for testing - never real data
        return EncryptedPersonalData.encrypt("123-45-6789", String.class);
    }
    
    // Security test scenarios
    public static String sqlInjectionAttempt() {
        return "'; DROP TABLE customers; --";
    }
    
    public static String xssAttempt() {
        return "<script>alert('xss')</script>";
    }
    
    public static String oversizedInput() {
        return "x".repeat(10000); // 10KB string for buffer overflow tests
    }
    
    // Valid test inputs
    public static String validCustomerName() {
        return "John Test Customer";
    }
    
    public static String validAccountToken() {
        return "tok_test_account_1234567890";
    }
}

/**
 * Security assertion helpers for tests
 */
public class SecurityAssertions {
    
    public static void assertNoSqlInjection(String input) {
        if (SecureValidationRules.containsSqlInjection(input)) {
            fail("Input contains SQL injection attempt: " + input);
        }
    }
    
    public static void assertNoXSS(String input) {
        if (SecureValidationRules.containsXSS(input)) {
            fail("Input contains XSS attempt: " + input);
        }
    }
    
    public static void assertPIIProtected(Object data) {
        var serialized = JsonUtils.serialize(data);
        
        // Check for common PII patterns
        if (serialized.matches(".*\\d{3}-\\d{2}-\\d{4}.*")) { // SSN
            fail("Unencrypted SSN found in data");
        }
        if (serialized.matches(".*\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}.*")) { // Credit card
            fail("Unencrypted credit card number found in data");
        }
    }
    
    public static void assertAuditLogGenerated(String operation, String entityId) {
        var auditLogs = testAuditService.getLogsForEntity(entityId);
        assertThat(auditLogs)
            .extracting(AuditLog::getOperation)
            .contains(operation);
    }
}
```

## 📋 Configuration

### Security Configuration
```yaml
shared-kernel:
  security:
    encryption:
      provider: "AWS_KMS"
      default-algorithm: "AES-256-GCM"
      key-rotation-days: 90
    
    audit:
      enabled: true
      high-risk-events: true
      pii-access-logging: true
      retention-days: 2555  # 7 years
      
    validation:
      input-sanitization: true
      sql-injection-prevention: true
      xss-prevention: true
      max-input-size: 1048576  # 1MB
      
  privacy:
    gdpr-compliance: true
    pseudonymization: true
    right-to-be-forgotten: true
    data-minimization: true
```

## 🎯 Usage Examples

### Creating Secure Domain Objects
```java
// Money with validation
var loanAmount = Money.of(25000.00, "USD");
var payment = Money.of(1250.00, "USD");
var remaining = loanAmount.subtract(payment); // Audited operation

// Customer ID generation
var customerId = CustomerId.generate();
var maskedId = customerId.getMaskedValue(); // For logging

// Encrypted PII handling
var encryptedSSN = EncryptedPersonalData.encrypt("123-45-6789", String.class);
var ssn = encryptedSSN.decrypt(decryptionContext); // Audited access

// Secure validation
var isValid = SecureValidationRules.isValidCustomerName(customerName);
var sanitized = SecureValidationRules.sanitizeInput(userInput);
```

### Domain Events with Security
```java
// Publishing domain events
var event = LoanApproved.builder()
    .loanId(loanId)
    .customerId(customerId)
    .approvedAmount(amount)
    .occurredAt(Instant.now())
    .correlationId(correlationId)
    .metadata(Map.of("containsPII", "false"))
    .build();

aggregateRoot.recordDomainEvent(event); // Automatic security validation
```

---

**Shared Kernel Documentation**  
**Version**: 1.0  
**Security Level**: Foundation for PCI-DSS v4.0 Compliance  
**Architecture Pattern**: Domain-Driven Design + Security Kernel