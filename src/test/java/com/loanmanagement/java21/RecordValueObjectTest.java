package com.loanmanagement.java21;

import com.loanmanagement.loan.domain.model.*;
import com.loanmanagement.loan.domain.valueobject.*;
import com.loanmanagement.common.valueobject.Money;
import com.loanmanagement.common.valueobject.EmailAddress;
import com.loanmanagement.common.valueobject.PhoneNumber;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Java 21 Record Value Objects Migration Tests")
class RecordValueObjectTest {

    @Test
    @DisplayName("Should convert Money value object to record with validation")
    void shouldConvertMoneyValueObjectToRecordWithValidation() {
        // given
        BigDecimal amount = new BigDecimal("50000.00");
        Currency currency = Currency.getInstance("USD");
        
        // when - Money should be converted to a record
        Money money = new Money(amount, currency);
        
        // then
        assertThat(money)
            .isNotNull()
            .describedAs("Money record should be created successfully");
        
        assertThat(money.amount())
            .isEqualTo(amount)
            .describedAs("Money record should provide amount accessor");
        
        assertThat(money.currency())
            .isEqualTo(currency)
            .describedAs("Money record should provide currency accessor");
        
        // Records should have automatic equals/hashCode/toString
        Money sameAmount = new Money(amount, currency);
        assertThat(money)
            .isEqualTo(sameAmount)
            .describedAs("Money records with same values should be equal");
        
        assertThat(money.hashCode())
            .isEqualTo(sameAmount.hashCode())
            .describedAs("Money records should have consistent hashCode");
        
        assertThat(money.toString())
            .contains("50000.00")
            .contains("USD")
            .describedAs("Money record should have meaningful toString");
    }

    @Test
    @DisplayName("Should convert EmailAddress value object to record with validation")
    void shouldConvertEmailAddressValueObjectToRecordWithValidation() {
        // given
        String validEmail = "customer@bank.com";
        
        // when - EmailAddress should be converted to a record
        EmailAddress email = new EmailAddress(validEmail);
        
        // then
        assertThat(email)
            .isNotNull()
            .describedAs("EmailAddress record should be created");
        
        assertThat(email.value())
            .isEqualTo(validEmail)
            .describedAs("EmailAddress record should provide value accessor");
        
        // Test validation in record
        assertThrows(IllegalArgumentException.class, () -> 
            new EmailAddress("invalid-email"),
            "EmailAddress record should validate format");
        
        assertThrows(IllegalArgumentException.class, () -> 
            new EmailAddress(null),
            "EmailAddress record should reject null values");
    }

    @Test
    @DisplayName("Should convert PhoneNumber value object to record with validation")
    void shouldConvertPhoneNumberValueObjectToRecordWithValidation() {
        // given
        String validPhone = "+1-555-123-4567";
        
        // when - PhoneNumber should be converted to a record
        PhoneNumber phone = new PhoneNumber(validPhone);
        
        // then
        assertThat(phone)
            .isNotNull()
            .describedAs("PhoneNumber record should be created");
        
        assertThat(phone.value())
            .isEqualTo(validPhone)
            .describedAs("PhoneNumber record should provide value accessor");
        
        // Test validation
        assertThrows(IllegalArgumentException.class, () -> 
            new PhoneNumber("invalid-phone"),
            "PhoneNumber record should validate format");
    }

    @Test
    @DisplayName("Should convert LoanApplicationId to record with UUID generation")
    void shouldConvertLoanApplicationIdToRecordWithUuidGeneration() {
        // given
        String applicationId = "LOAN-APP-" + UUID.randomUUID();
        
        // when - LoanApplicationId should be converted to a record
        LoanApplicationId loanId = new LoanApplicationId(applicationId);
        
        // then
        assertThat(loanId)
            .isNotNull()
            .describedAs("LoanApplicationId record should be created");
        
        assertThat(loanId.value())
            .isEqualTo(applicationId)
            .describedAs("LoanApplicationId record should store the ID value");
        
        // Test factory method for UUID generation
        LoanApplicationId generatedId = LoanApplicationId.generate();
        assertThat(generatedId.value())
            .startsWith("LOAN-APP-")
            .describedAs("Generated LoanApplicationId should have proper prefix");
        
        // Test immutability
        LoanApplicationId sameId = new LoanApplicationId(applicationId);
        assertThat(loanId)
            .isEqualTo(sameId)
            .describedAs("LoanApplicationId records should be equal with same value");
    }

    @Test
    @DisplayName("Should convert CustomerId to record with proper validation")
    void shouldConvertCustomerIdToRecordWithProperValidation() {
        // given
        String customerId = "CUST-12345";
        
        // when - CustomerId should be converted to a record
        CustomerId customer = new CustomerId(customerId);
        
        // then
        assertThat(customer)
            .isNotNull()
            .describedAs("CustomerId record should be created");
        
        assertThat(customer.value())
            .isEqualTo(customerId)
            .describedAs("CustomerId record should provide value accessor");
        
        // Test validation
        assertThrows(IllegalArgumentException.class, () -> 
            new CustomerId(null),
            "CustomerId record should reject null values");
        
        assertThrows(IllegalArgumentException.class, () -> 
            new CustomerId(""),
            "CustomerId record should reject empty values");
    }

    @Test
    @DisplayName("Should convert InterestRate value object to record with percentage validation")
    void shouldConvertInterestRateValueObjectToRecordWithPercentageValidation() {
        // given
        BigDecimal rate = new BigDecimal("5.75");
        
        // when - InterestRate should be converted to a record
        InterestRate interestRate = new InterestRate(rate);
        
        // then
        assertThat(interestRate)
            .isNotNull()
            .describedAs("InterestRate record should be created");
        
        assertThat(interestRate.percentage())
            .isEqualTo(rate)
            .describedAs("InterestRate record should provide percentage accessor");
        
        // Test validation
        assertThrows(IllegalArgumentException.class, () -> 
            new InterestRate(new BigDecimal("-1.0")),
            "InterestRate record should reject negative values");
        
        assertThrows(IllegalArgumentException.class, () -> 
            new InterestRate(new BigDecimal("101.0")),
            "InterestRate record should reject rates over 100%");
        
        // Test business methods
        assertThat(interestRate.asDecimal())
            .isEqualTo(new BigDecimal("0.0575"))
            .describedAs("InterestRate record should convert to decimal");
    }

    @Test
    @DisplayName("Should convert LoanTerm value object to record with duration validation")
    void shouldConvertLoanTermValueObjectToRecordWithDurationValidation() {
        // given
        int months = 60; // 5 years
        
        // when - LoanTerm should be converted to a record
        LoanTerm loanTerm = new LoanTerm(months);
        
        // then
        assertThat(loanTerm)
            .isNotNull()
            .describedAs("LoanTerm record should be created");
        
        assertThat(loanTerm.months())
            .isEqualTo(months)
            .describedAs("LoanTerm record should provide months accessor");
        
        // Test validation
        assertThrows(IllegalArgumentException.class, () -> 
            new LoanTerm(0),
            "LoanTerm record should reject zero months");
        
        assertThrows(IllegalArgumentException.class, () -> 
            new LoanTerm(-12),
            "LoanTerm record should reject negative months");
        
        // Test business methods
        assertThat(loanTerm.years())
            .isEqualTo(5)
            .describedAs("LoanTerm record should convert to years");
        
        assertThat(loanTerm.isLongTerm())
            .isTrue()
            .describedAs("60 months should be considered long term");
    }

    @Test
    @DisplayName("Should convert Address value object to record with proper structure")
    void shouldConvertAddressValueObjectToRecordWithProperStructure() {
        // given
        String street = "123 Banking Street";
        String city = "New York";
        String state = "NY";
        String zipCode = "10001";
        String country = "USA";
        
        // when - Address should be converted to a record
        Address address = new Address(street, city, state, zipCode, country);
        
        // then
        assertThat(address)
            .isNotNull()
            .describedAs("Address record should be created");
        
        assertThat(address.street()).isEqualTo(street);
        assertThat(address.city()).isEqualTo(city);
        assertThat(address.state()).isEqualTo(state);
        assertThat(address.zipCode()).isEqualTo(zipCode);
        assertThat(address.country()).isEqualTo(country);
        
        // Test validation
        assertThrows(IllegalArgumentException.class, () -> 
            new Address(null, city, state, zipCode, country),
            "Address record should reject null street");
        
        assertThrows(IllegalArgumentException.class, () -> 
            new Address(street, null, state, zipCode, country),
            "Address record should reject null city");
        
        // Test business methods
        assertThat(address.fullAddress())
            .contains(street)
            .contains(city)
            .contains(state)
            .contains(zipCode)
            .contains(country)
            .describedAs("Address record should provide formatted full address");
    }

    @Test
    @DisplayName("Should convert CreditScore value object to record with range validation")
    void shouldConvertCreditScoreValueObjectToRecordWithRangeValidation() {
        // given
        int score = 750;
        LocalDate assessmentDate = LocalDate.now();
        
        // when - CreditScore should be converted to a record
        CreditScore creditScore = new CreditScore(score, assessmentDate);
        
        // then
        assertThat(creditScore)
            .isNotNull()
            .describedAs("CreditScore record should be created");
        
        assertThat(creditScore.score()).isEqualTo(score);
        assertThat(creditScore.assessmentDate()).isEqualTo(assessmentDate);
        
        // Test validation
        assertThrows(IllegalArgumentException.class, () -> 
            new CreditScore(299, assessmentDate),
            "CreditScore record should reject scores below 300");
        
        assertThrows(IllegalArgumentException.class, () -> 
            new CreditScore(851, assessmentDate),
            "CreditScore record should reject scores above 850");
        
        // Test business methods
        assertThat(creditScore.isExcellent())
            .isTrue()
            .describedAs("750 credit score should be excellent");
        
        assertThat(creditScore.getRiskLevel())
            .isEqualTo("LOW")
            .describedAs("Excellent credit score should have low risk");
    }

    @Test
    @DisplayName("Should demonstrate record pattern matching with sealed interfaces")
    void shouldDemonstrateRecordPatternMatchingWithSealedInterfaces() {
        // given
        LoanDecisionResult approvedResult = new ApprovedLoanDecision(
            new Money(new BigDecimal("50000"), Currency.getInstance("USD")),
            new InterestRate(new BigDecimal("5.5")),
            new LoanTerm(60)
        );
        
        LoanDecisionResult rejectedResult = new RejectedLoanDecision(
            "Insufficient credit score",
            "CREDIT_SCORE_TOO_LOW"
        );
        
        // when - Using pattern matching with records (Java 21 feature)
        String approvedMessage = processLoanDecision(approvedResult);
        String rejectedMessage = processLoanDecision(rejectedResult);
        
        // then
        assertThat(approvedMessage)
            .contains("approved")
            .contains("50000")
            .contains("5.5")
            .describedAs("Pattern matching should extract approved loan details");
        
        assertThat(rejectedMessage)
            .contains("rejected")
            .contains("Insufficient credit score")
            .describedAs("Pattern matching should extract rejection reason");
    }

    @Test
    @DisplayName("Should convert audit information to record with immutable timestamp")
    void shouldConvertAuditInformationToRecordWithImmutableTimestamp() {
        // given
        String userId = "USER-123";
        LocalDateTime timestamp = LocalDateTime.now();
        String action = "LOAN_APPLICATION_SUBMITTED";
        
        // when - AuditInfo should be converted to a record
        AuditInfo auditInfo = new AuditInfo(userId, timestamp, action);
        
        // then
        assertThat(auditInfo)
            .isNotNull()
            .describedAs("AuditInfo record should be created");
        
        assertThat(auditInfo.userId()).isEqualTo(userId);
        assertThat(auditInfo.timestamp()).isEqualTo(timestamp);
        assertThat(auditInfo.action()).isEqualTo(action);
        
        // Test immutability - records are immutable by default
        AuditInfo sameAuditInfo = new AuditInfo(userId, timestamp, action);
        assertThat(auditInfo)
            .isEqualTo(sameAuditInfo)
            .describedAs("AuditInfo records should be immutable and equal");
        
        // Test factory method for current timestamp
        AuditInfo currentAudit = AuditInfo.now(userId, action);
        assertThat(currentAudit.userId()).isEqualTo(userId);
        assertThat(currentAudit.action()).isEqualTo(action);
        assertThat(currentAudit.timestamp()).isBefore(LocalDateTime.now().plusSeconds(1));
    }

    // Helper methods for testing - These will fail until Java 21 record implementation

    private String processLoanDecision(LoanDecisionResult decision) {
        // This should use Java 21 pattern matching with records
        // return switch (decision) {
        //     case ApprovedLoanDecision(var amount, var rate, var term) -> 
        //         "Loan approved for " + amount + " at " + rate + "% for " + term + " months";
        //     case RejectedLoanDecision(var reason, var code) -> 
        //         "Loan rejected: " + reason + " (Code: " + code + ")";
        // };
        throw new UnsupportedOperationException("Pattern matching with records not yet implemented - Java 21 migration pending");
    }

    // Sealed interface for loan decisions (Java 17+ feature working with records)
    public sealed interface LoanDecisionResult 
        permits ApprovedLoanDecision, RejectedLoanDecision {
    }

    // Record implementations - These will be implemented during Green phase
    public record ApprovedLoanDecision(
        Money amount,
        InterestRate rate,
        LoanTerm term
    ) implements LoanDecisionResult {
        public ApprovedLoanDecision {
            // Compact constructor validation
            if (amount == null) throw new IllegalArgumentException("Amount cannot be null");
            if (rate == null) throw new IllegalArgumentException("Rate cannot be null");
            if (term == null) throw new IllegalArgumentException("Term cannot be null");
        }
    }

    public record RejectedLoanDecision(
        String reason,
        String errorCode
    ) implements LoanDecisionResult {
        public RejectedLoanDecision {
            // Compact constructor validation
            if (reason == null || reason.trim().isEmpty()) {
                throw new IllegalArgumentException("Rejection reason cannot be null or empty");
            }
            if (errorCode == null || errorCode.trim().isEmpty()) {
                throw new IllegalArgumentException("Error code cannot be null or empty");
            }
        }
    }

    // Placeholder record definitions that will be implemented during Green phase
    record Money(BigDecimal amount, Currency currency) {
        public Money {
            if (amount == null) throw new IllegalArgumentException("Amount cannot be null");
            if (currency == null) throw new IllegalArgumentException("Currency cannot be null");
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Amount cannot be negative");
            }
        }
    }

    record EmailAddress(String value) {
        public EmailAddress {
            if (value == null || !value.contains("@")) {
                throw new IllegalArgumentException("Invalid email address");
            }
        }
    }

    record PhoneNumber(String value) {
        public PhoneNumber {
            if (value == null || value.length() < 10) {
                throw new IllegalArgumentException("Invalid phone number");
            }
        }
    }

    record LoanApplicationId(String value) {
        public LoanApplicationId {
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException("Loan application ID cannot be null or empty");
            }
        }
        
        public static LoanApplicationId generate() {
            return new LoanApplicationId("LOAN-APP-" + UUID.randomUUID());
        }
    }

    record CustomerId(String value) {
        public CustomerId {
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException("Customer ID cannot be null or empty");
            }
        }
    }

    record InterestRate(BigDecimal percentage) {
        public InterestRate {
            if (percentage == null) throw new IllegalArgumentException("Percentage cannot be null");
            if (percentage.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Interest rate cannot be negative");
            }
            if (percentage.compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("Interest rate cannot exceed 100%");
            }
        }
        
        public BigDecimal asDecimal() {
            return percentage.divide(new BigDecimal("100"));
        }
    }

    record LoanTerm(int months) {
        public LoanTerm {
            if (months <= 0) {
                throw new IllegalArgumentException("Loan term must be positive");
            }
        }
        
        public int years() {
            return months / 12;
        }
        
        public boolean isLongTerm() {
            return months > 36; // More than 3 years
        }
    }

    record Address(String street, String city, String state, String zipCode, String country) {
        public Address {
            if (street == null || street.trim().isEmpty()) {
                throw new IllegalArgumentException("Street cannot be null or empty");
            }
            if (city == null || city.trim().isEmpty()) {
                throw new IllegalArgumentException("City cannot be null or empty");
            }
        }
        
        public String fullAddress() {
            return String.join(", ", street, city, state, zipCode, country);
        }
    }

    record CreditScore(int score, LocalDate assessmentDate) {
        public CreditScore {
            if (score < 300 || score > 850) {
                throw new IllegalArgumentException("Credit score must be between 300 and 850");
            }
            if (assessmentDate == null) {
                throw new IllegalArgumentException("Assessment date cannot be null");
            }
        }
        
        public boolean isExcellent() {
            return score >= 750;
        }
        
        public String getRiskLevel() {
            if (score >= 750) return "LOW";
            if (score >= 650) return "MEDIUM";
            return "HIGH";
        }
    }

    record AuditInfo(String userId, LocalDateTime timestamp, String action) {
        public AuditInfo {
            if (userId == null || userId.trim().isEmpty()) {
                throw new IllegalArgumentException("User ID cannot be null or empty");
            }
            if (timestamp == null) {
                throw new IllegalArgumentException("Timestamp cannot be null");
            }
            if (action == null || action.trim().isEmpty()) {
                throw new IllegalArgumentException("Action cannot be null or empty");
            }
        }
        
        public static AuditInfo now(String userId, String action) {
            return new AuditInfo(userId, LocalDateTime.now(), action);
        }
    }
}