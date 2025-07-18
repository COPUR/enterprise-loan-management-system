package com.bank.infrastructure.domain;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.math.BigDecimal;

/**
 * Banking Domain Models using Java 21 Records and Sealed Classes
 * 
 * Comprehensive domain model implementations leveraging:
 * - Record classes for immutable data transfer objects
 * - Sealed classes for type safety and exhaustive pattern matching
 * - Value objects for domain-driven design
 * - Islamic banking domain models
 * - Compliance and regulatory models
 * 
 * This provides a modern, type-safe foundation for banking operations.
 */
@Component
public class BankingDomainModels {

    // =================================================================
    // CUSTOMER DOMAIN MODELS
    // =================================================================

    /**
     * Customer information record
     */
    public record CustomerInfo(
        String customerId,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        Address address,
        CustomerType customerType,
        LocalDate dateOfBirth,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        public CustomerInfo {
            if (customerId == null || customerId.trim().isEmpty()) {
                throw new IllegalArgumentException("Customer ID cannot be null or empty");
            }
            if (firstName == null || firstName.trim().isEmpty()) {
                throw new IllegalArgumentException("First name cannot be null or empty");
            }
            if (lastName == null || lastName.trim().isEmpty()) {
                throw new IllegalArgumentException("Last name cannot be null or empty");
            }
        }
        
        public String getFullName() {
            return firstName + " " + lastName;
        }
        
        public boolean isMinor() {
            return dateOfBirth.isAfter(LocalDate.now().minusYears(18));
        }
    }

    /**
     * Address value object
     */
    public record Address(
        String street,
        String city,
        String state,
        String zipCode,
        String country
    ) {
        public Address {
            if (street == null || street.trim().isEmpty()) {
                throw new IllegalArgumentException("Street cannot be null or empty");
            }
            if (city == null || city.trim().isEmpty()) {
                throw new IllegalArgumentException("City cannot be null or empty");
            }
            if (country == null || country.trim().isEmpty()) {
                throw new IllegalArgumentException("Country cannot be null or empty");
            }
        }
        
        public String getFormattedAddress() {
            return String.format("%s, %s, %s %s, %s", street, city, state, zipCode, country);
        }
    }

    /**
     * Customer type sealed interface
     */
    public sealed interface CustomerType
        permits IndividualCustomer, CorporateCustomer, IslamicBankingCustomer {
    }

    public record IndividualCustomer(
        String nationalId,
        String occupation,
        Money monthlyIncome,
        CreditProfile creditProfile
    ) implements CustomerType {}

    public record CorporateCustomer(
        String companyRegistrationNumber,
        String businessType,
        Money annualRevenue,
        String taxId,
        List<String> authorizedSignatories
    ) implements CustomerType {}

    public record IslamicBankingCustomer(
        String nationalId,
        String occupation,
        Money monthlyIncome,
        boolean shariaCompliant,
        List<IslamicProduct> preferredProducts
    ) implements CustomerType {}

    // =================================================================
    // ACCOUNT DOMAIN MODELS
    // =================================================================

    /**
     * Bank account sealed interface
     */
    public sealed interface BankAccount
        permits SavingsAccount, CheckingAccount, IslamicAccount {
    }

    public record SavingsAccount(
        String accountNumber,
        String customerId,
        Money balance,
        AccountStatus status,
        BigDecimal interestRate,
        Money minimumBalance,
        LocalDateTime createdAt
    ) implements BankAccount {}

    public record CheckingAccount(
        String accountNumber,
        String customerId,
        Money balance,
        AccountStatus status,
        Money overdraftLimit,
        Money monthlyFee,
        LocalDateTime createdAt
    ) implements BankAccount {}

    public record IslamicAccount(
        String accountNumber,
        String customerId,
        Money balance,
        AccountStatus status,
        IslamicProduct productType,
        boolean shariaCompliant,
        LocalDateTime createdAt
    ) implements BankAccount {}

    /**
     * Account status sealed interface
     */
    public sealed interface AccountStatus
        permits ActiveAccount, SuspendedAccount, ClosedAccount, FrozenAccount {
    }

    public record ActiveAccount(LocalDateTime activatedAt) implements AccountStatus {}
    public record SuspendedAccount(LocalDateTime suspendedAt, String reason) implements AccountStatus {}
    public record ClosedAccount(LocalDateTime closedAt, String reason) implements AccountStatus {}
    public record FrozenAccount(LocalDateTime frozenAt, String reason, String authorizedBy) implements AccountStatus {}

    // =================================================================
    // LOAN DOMAIN MODELS
    // =================================================================

    /**
     * Loan application record
     */
    public record LoanApplication(
        String applicationId,
        String customerId,
        LoanType loanType,
        Money requestedAmount,
        int termInMonths,
        LoanPurpose purpose,
        ApplicationStatus status,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt,
        String reviewedBy,
        List<Document> documents,
        CreditAssessment creditAssessment
    ) {
        public LoanApplication {
            if (applicationId == null || applicationId.trim().isEmpty()) {
                throw new IllegalArgumentException("Application ID cannot be null or empty");
            }
            if (requestedAmount == null || requestedAmount.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Requested amount must be positive");
            }
            if (termInMonths <= 0) {
                throw new IllegalArgumentException("Term in months must be positive");
            }
        }
        
        public Money getMonthlyPayment() {
            // Simplified calculation - in real implementation use proper loan calculation
            return new Money(requestedAmount.getCurrency(), 
                requestedAmount.getAmount().divide(BigDecimal.valueOf(termInMonths)));
        }
    }

    /**
     * Loan type sealed interface
     */
    public sealed interface LoanType
        permits PersonalLoan, MortgageLoan, AutoLoan, BusinessLoan, IslamicLoan {
    }

    public record PersonalLoan(
        BigDecimal interestRate,
        boolean securityRequired,
        Money maxAmount
    ) implements LoanType {}

    public record MortgageLoan(
        BigDecimal interestRate,
        Money propertyValue,
        BigDecimal loanToValueRatio,
        int maxTermYears
    ) implements LoanType {}

    public record AutoLoan(
        BigDecimal interestRate,
        String vehicleType,
        Money vehicleValue,
        int maxTermYears
    ) implements LoanType {}

    public record BusinessLoan(
        BigDecimal interestRate,
        String businessType,
        Money annualRevenue,
        List<String> collateralTypes
    ) implements LoanType {}

    public record IslamicLoan(
        IslamicProduct productType,
        BigDecimal profitRate,
        boolean shariaCompliant,
        String shariaBoard
    ) implements LoanType {}

    /**
     * Loan purpose enumeration
     */
    public enum LoanPurpose {
        HOME_PURCHASE, HOME_IMPROVEMENT, EDUCATION, MEDICAL, BUSINESS_EXPANSION,
        WORKING_CAPITAL, EQUIPMENT_FINANCING, DEBT_CONSOLIDATION, PERSONAL_USE
    }

    /**
     * Application status sealed interface
     */
    public sealed interface ApplicationStatus
        permits Submitted, UnderReview, Approved, Rejected, Cancelled {
    }

    public record Submitted(LocalDateTime submittedAt) implements ApplicationStatus {}
    public record UnderReview(LocalDateTime reviewStartedAt, String reviewedBy) implements ApplicationStatus {}
    public record Approved(LocalDateTime approvedAt, String approvedBy, Money approvedAmount) implements ApplicationStatus {}
    public record Rejected(LocalDateTime rejectedAt, String rejectedBy, String reason) implements ApplicationStatus {}
    public record Cancelled(LocalDateTime cancelledAt, String cancelledBy, String reason) implements ApplicationStatus {}

    // =================================================================
    // ISLAMIC BANKING DOMAIN MODELS
    // =================================================================

    /**
     * Islamic banking product sealed interface
     */
    public sealed interface IslamicProduct
        permits Murabaha, Ijara, Musharaka, Mudaraba, Sukuk {
    }

    public record Murabaha(
        Money costPrice,
        Money sellingPrice,
        BigDecimal profitMargin,
        int paymentTermMonths,
        String commodity
    ) implements IslamicProduct {
        public Money getProfit() {
            return new Money(sellingPrice.getCurrency(), 
                sellingPrice.getAmount().subtract(costPrice.getAmount()));
        }
    }

    public record Ijara(
        Money assetValue,
        Money monthlyRental,
        int leaseTerm,
        String assetType,
        boolean endOwnership
    ) implements IslamicProduct {
        public Money getTotalRental() {
            return new Money(monthlyRental.getCurrency(), 
                monthlyRental.getAmount().multiply(BigDecimal.valueOf(leaseTerm)));
        }
    }

    public record Musharaka(
        Money bankContribution,
        Money customerContribution,
        BigDecimal bankProfitRatio,
        BigDecimal customerProfitRatio,
        String businessType
    ) implements IslamicProduct {
        public Money getTotalCapital() {
            return new Money(bankContribution.getCurrency(), 
                bankContribution.getAmount().add(customerContribution.getAmount()));
        }
    }

    public record Mudaraba(
        Money capitalProvided,
        BigDecimal profitSharingRatio,
        String businessActivity,
        LocalDate maturityDate
    ) implements IslamicProduct {}

    public record Sukuk(
        Money faceValue,
        BigDecimal expectedReturn,
        String underlyingAsset,
        LocalDate maturityDate,
        String sukukType
    ) implements IslamicProduct {}

    // =================================================================
    // PAYMENT DOMAIN MODELS
    // =================================================================

    /**
     * Payment transaction record
     */
    public record PaymentTransaction(
        String transactionId,
        String customerId,
        Money amount,
        PaymentType paymentType,
        PaymentStatus status,
        String fromAccount,
        String toAccount,
        String description,
        LocalDateTime initiatedAt,
        LocalDateTime completedAt,
        List<PaymentInstruction> instructions
    ) {
        public PaymentTransaction {
            if (transactionId == null || transactionId.trim().isEmpty()) {
                throw new IllegalArgumentException("Transaction ID cannot be null or empty");
            }
            if (amount == null || amount.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Amount must be positive");
            }
        }
        
        public boolean isCompleted() {
            return status instanceof PaymentCompleted;
        }
        
        public Optional<LocalDateTime> getCompletedAt() {
            return isCompleted() ? Optional.of(completedAt) : Optional.empty();
        }
    }

    /**
     * Payment type sealed interface
     */
    public sealed interface PaymentType
        permits ACHPayment, WireTransfer, CardPayment, DigitalWallet, IslamicPayment {
    }

    public record ACHPayment(
        String routingNumber,
        String accountNumber,
        ACHType achType
    ) implements PaymentType {
        public enum ACHType { DEBIT, CREDIT }
    }

    public record WireTransfer(
        String swiftCode,
        String beneficiaryBank,
        String beneficiaryAccount,
        String intermediaryBank
    ) implements PaymentType {}

    public record CardPayment(
        String cardNumber,
        String cardType,
        String merchantId,
        String authorizationCode
    ) implements PaymentType {}

    public record DigitalWallet(
        String walletProvider,
        String walletId,
        String deviceId
    ) implements PaymentType {}

    public record IslamicPayment(
        IslamicProduct productType,
        String contractReference,
        boolean shariaCompliant
    ) implements PaymentType {}

    /**
     * Payment status sealed interface
     */
    public sealed interface PaymentStatus
        permits PaymentInitiated, PaymentProcessing, PaymentCompleted, PaymentFailed, PaymentCancelled {
    }

    public record PaymentInitiated(LocalDateTime initiatedAt) implements PaymentStatus {}
    public record PaymentProcessing(LocalDateTime processingStartedAt) implements PaymentStatus {}
    public record PaymentCompleted(LocalDateTime completedAt, String confirmationNumber) implements PaymentStatus {}
    public record PaymentFailed(LocalDateTime failedAt, String failureReason, String errorCode) implements PaymentStatus {}
    public record PaymentCancelled(LocalDateTime cancelledAt, String cancelledBy, String reason) implements PaymentStatus {}

    // =================================================================
    // COMPLIANCE DOMAIN MODELS
    // =================================================================

    /**
     * Compliance record
     */
    public record ComplianceRecord(
        String recordId,
        String customerId,
        ComplianceType complianceType,
        ComplianceStatus status,
        LocalDateTime checkedAt,
        String checkedBy,
        List<ComplianceViolation> violations,
        LocalDateTime nextReviewDate
    ) {}

    /**
     * Compliance type sealed interface
     */
    public sealed interface ComplianceType
        permits KYCCompliance, AMLCompliance, SanctionScreening, PEPScreening, TaxCompliance {
    }

    public record KYCCompliance(
        String documentType,
        String documentNumber,
        LocalDate expiryDate,
        boolean verified
    ) implements ComplianceType {}

    public record AMLCompliance(
        String riskRating,
        Money transactionLimit,
        List<String> watchListMatches
    ) implements ComplianceType {}

    public record SanctionScreening(
        List<String> sanctionLists,
        boolean cleared,
        String screeningProvider
    ) implements ComplianceType {}

    public record PEPScreening(
        boolean isPEP,
        String politicalExposure,
        String sourceOfWealth
    ) implements ComplianceType {}

    public record TaxCompliance(
        String taxId,
        String taxResidency,
        boolean fatcaCompliant,
        boolean crsCompliant
    ) implements ComplianceType {}

    // =================================================================
    // SUPPORTING DOMAIN MODELS
    // =================================================================

    /**
     * Document record
     */
    public record Document(
        String documentId,
        String fileName,
        String documentType,
        String mimeType,
        long fileSize,
        String uploadedBy,
        LocalDateTime uploadedAt,
        DocumentStatus status
    ) {}

    /**
     * Document status sealed interface
     */
    public sealed interface DocumentStatus
        permits DocumentUploaded, DocumentVerified, DocumentRejected {
    }

    public record DocumentUploaded(LocalDateTime uploadedAt) implements DocumentStatus {}
    public record DocumentVerified(LocalDateTime verifiedAt, String verifiedBy) implements DocumentStatus {}
    public record DocumentRejected(LocalDateTime rejectedAt, String rejectedBy, String reason) implements DocumentStatus {}

    /**
     * Credit assessment record
     */
    public record CreditAssessment(
        String assessmentId,
        String customerId,
        int creditScore,
        CreditRating creditRating,
        Money recommendedLimit,
        BigDecimal recommendedRate,
        LocalDateTime assessedAt,
        String assessedBy,
        List<CreditFactor> factors
    ) {}

    /**
     * Credit rating enumeration
     */
    public enum CreditRating {
        EXCELLENT, GOOD, FAIR, POOR, VERY_POOR
    }

    /**
     * Credit factor record
     */
    public record CreditFactor(
        String factorType,
        String description,
        int impact,
        CreditFactorType factorCategory
    ) {}

    /**
     * Credit factor type enumeration
     */
    public enum CreditFactorType {
        PAYMENT_HISTORY, CREDIT_UTILIZATION, CREDIT_HISTORY_LENGTH, 
        CREDIT_MIX, NEW_CREDIT, INCOME_VERIFICATION
    }

    /**
     * Credit profile record
     */
    public record CreditProfile(
        String customerId,
        int creditScore,
        CreditRating rating,
        Money creditLimit,
        Money availableCredit,
        LocalDateTime lastUpdated,
        List<CreditAccount> accounts
    ) {
        public Money getUsedCredit() {
            return new Money(creditLimit.getCurrency(), 
                creditLimit.getAmount().subtract(availableCredit.getAmount()));
        }
        
        public BigDecimal getCreditUtilization() {
            if (creditLimit.getAmount().compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ZERO;
            }
            return getUsedCredit().getAmount()
                .divide(creditLimit.getAmount(), 4, java.math.RoundingMode.HALF_UP);
        }
    }

    /**
     * Credit account record
     */
    public record CreditAccount(
        String accountNumber,
        String accountType,
        Money balance,
        Money creditLimit,
        int paymentHistory,
        LocalDate openedDate,
        LocalDate lastActivityDate
    ) {}

    /**
     * Payment instruction record
     */
    public record PaymentInstruction(
        String instructionId,
        String instructionType,
        String details,
        int processingOrder,
        LocalDateTime executedAt
    ) {}

    /**
     * Compliance violation record
     */
    public record ComplianceViolation(
        String violationId,
        String violationType,
        String description,
        ViolationSeverity severity,
        LocalDateTime detectedAt,
        String detectedBy,
        ViolationStatus status
    ) {}

    /**
     * Violation severity enumeration
     */
    public enum ViolationSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    /**
     * Violation status sealed interface
     */
    public sealed interface ViolationStatus
        permits ViolationOpen, ViolationInProgress, ViolationResolved, ViolationWaived {
    }

    public record ViolationOpen(LocalDateTime openedAt) implements ViolationStatus {}
    public record ViolationInProgress(LocalDateTime startedAt, String assignedTo) implements ViolationStatus {}
    public record ViolationResolved(LocalDateTime resolvedAt, String resolvedBy, String resolution) implements ViolationStatus {}
    public record ViolationWaived(LocalDateTime waivedAt, String waivedBy, String reason) implements ViolationStatus {}

    /**
     * Compliance status sealed interface
     */
    public sealed interface ComplianceStatus
        permits Compliant, NonCompliant, ComplianceUnderReview, Exempted {
    }

    public record Compliant(LocalDateTime verifiedAt, String verifiedBy) implements ComplianceStatus {}
    public record NonCompliant(LocalDateTime detectedAt, List<String> issues) implements ComplianceStatus {}
    public record ComplianceUnderReview(LocalDateTime reviewStartedAt, String reviewedBy) implements ComplianceStatus {}
    public record Exempted(LocalDateTime exemptedAt, String exemptedBy, String reason) implements ComplianceStatus {}
}