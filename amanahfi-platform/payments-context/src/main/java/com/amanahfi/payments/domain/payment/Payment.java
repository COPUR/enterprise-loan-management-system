package com.amanahfi.payments.domain.payment;

import com.amanahfi.shared.domain.money.Money;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Payment Aggregate Root
 * 
 * Represents a payment in the AmanahFi platform supporting:
 * - CBDC instant settlement (≤5 seconds)
 * - Cross-currency transfers
 * - Islamic banking compliance
 * - Stablecoin payments
 * - Comprehensive fee management
 */
@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    // Business Constants
    private static final BigDecimal DAILY_LIMIT_AED = new BigDecimal("500000.00"); // 500K AED daily limit
    private static final int CBDC_MAX_SETTLEMENT_SECONDS = 5;
    
    @Id
    private String paymentId;

    @NotBlank
    private String fromAccountId;

    @NotBlank
    private String toAccountId;

    @NotNull
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "payment_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "payment_currency"))
    })
    private Money amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;

    @NotBlank
    private String reference;

    @NotNull
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @NotNull
    private LocalDateTime createdAt;

    private LocalDateTime processedAt;
    private LocalDateTime completedAt;
    private LocalDateTime failedAt;

    // Processing References
    private String processingReference;
    private String settlementReference;
    private String failureReason;

    // Islamic Banking Properties
    private boolean islamicCompliant = false;

    // Digital Currency Properties
    private boolean cbdcPayment = false;
    private boolean stablecoinPayment = false;
    private String stablecoinType;

    // Cross-Currency Properties
    private boolean requiresCurrencyConversion = false;
    private String sourceCurrency;
    private String targetCurrency;

    // Compliance Properties
    private boolean complianceChecked = false;
    private String complianceCheckReference;
    private String complianceNotes;

    // Performance Metrics
    private Long settlementTimeSeconds;

    // Fees
    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PaymentFee> fees = new ArrayList<>();

    // Domain Events
    @Transient
    private List<Object> domainEvents = new ArrayList<>();

    /**
     * Creates a standard payment
     */
    public static Payment create(String fromAccountId, String toAccountId, 
                               Money amount, PaymentType paymentType, String reference) {
        validatePaymentParameters(fromAccountId, toAccountId, amount, reference);
        validatePaymentLimits(amount);
        
        Payment payment = new Payment();
        payment.paymentId = generatePaymentId();
        payment.fromAccountId = fromAccountId;
        payment.toAccountId = toAccountId;
        payment.amount = amount;
        payment.paymentType = paymentType;
        payment.reference = reference;
        payment.status = PaymentStatus.PENDING;
        payment.createdAt = LocalDateTime.now();
        
        payment.addDomainEvent(new PaymentCreatedEvent(payment.paymentId, fromAccountId, toAccountId, amount));
        
        return payment;
    }

    /**
     * Creates a CBDC payment for instant settlement
     */
    public static Payment createCbdcPayment(String fromAccountId, String toAccountId, 
                                          Money amount, String reference) {
        if (!"AED".equals(amount.getCurrency())) {
            throw new IllegalArgumentException("CBDC payments must use AED currency");
        }
        
        Payment payment = create(fromAccountId, toAccountId, amount, PaymentType.CBDC_TRANSFER, reference);
        payment.cbdcPayment = true;
        
        payment.addDomainEvent(new CbdcPaymentCreatedEvent(payment.paymentId, amount));
        
        return payment;
    }

    /**
     * Creates a cross-currency payment
     */
    public static Payment createCrossCurrency(String fromAccountId, String toAccountId, 
                                            Money amount, String reference) {
        Payment payment = create(fromAccountId, toAccountId, amount, PaymentType.CROSS_CURRENCY, reference);
        payment.requiresCurrencyConversion = true;
        payment.targetCurrency = amount.getCurrency();
        
        return payment;
    }

    /**
     * Creates a stablecoin payment
     */
    public static Payment createStablecoinPayment(String fromAccountId, String toAccountId, 
                                                Money amount, String stablecoinType, String reference) {
        Payment payment = create(fromAccountId, toAccountId, amount, PaymentType.STABLECOIN_TRANSFER, reference);
        payment.stablecoinPayment = true;
        payment.stablecoinType = stablecoinType;
        
        payment.addDomainEvent(new StablecoinPaymentCreatedEvent(payment.paymentId, stablecoinType, amount));
        
        return payment;
    }

    /**
     * Sets Islamic banking compliance
     */
    public Payment withIslamicCompliance(boolean compliant) {
        this.islamicCompliant = compliant;
        
        if (compliant) {
            addDomainEvent(new PaymentMarkedIslamicCompliantEvent(paymentId));
        }
        
        return this;
    }

    /**
     * Processes the payment
     */
    public void process(String processingReference) {
        if (status == PaymentStatus.COMPLETED) {
            throw new IllegalStateException("Payment is already completed");
        }
        if (status == PaymentStatus.FAILED) {
            throw new IllegalStateException("Cannot process a failed payment");
        }
        
        this.status = PaymentStatus.PROCESSING;
        this.processingReference = processingReference;
        this.processedAt = LocalDateTime.now();
        
        addDomainEvent(new PaymentProcessingStartedEvent(paymentId, processingReference));
    }

    /**
     * Completes the payment
     */
    public void complete(String settlementReference) {
        if (status != PaymentStatus.PROCESSING) {
            throw new IllegalStateException("Payment must be in PROCESSING status to complete");
        }
        
        this.status = PaymentStatus.COMPLETED;
        this.settlementReference = settlementReference;
        this.completedAt = LocalDateTime.now();
        
        // Calculate settlement time
        if (processedAt != null) {
            this.settlementTimeSeconds = ChronoUnit.SECONDS.between(processedAt, completedAt);
            // Ensure minimum 1 second for tracking purposes
            if (this.settlementTimeSeconds == 0) {
                this.settlementTimeSeconds = 1L;
            }
        }
        
        addDomainEvent(new PaymentCompletedEvent(paymentId, settlementReference, settlementTimeSeconds));
    }

    /**
     * Fails the payment with reason
     */
    public void fail(String reason) {
        if (status == PaymentStatus.COMPLETED) {
            throw new IllegalStateException("Cannot fail a completed payment");
        }
        
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.failedAt = LocalDateTime.now();
        
        addDomainEvent(new PaymentFailedEvent(paymentId, reason));
    }

    /**
     * Adds a fee to the payment
     */
    public void addFee(PaymentFeeType feeType, Money feeAmount, String description) {
        validateFeeForIslamicCompliance(feeType);
        validateFeeCurrency(feeAmount);
        
        PaymentFee fee = new PaymentFee(feeType, feeAmount, description);
        fee.assignToPayment(this);
        this.fees.add(fee);
        
        addDomainEvent(new PaymentFeeAddedEvent(paymentId, feeType, feeAmount));
    }

    /**
     * Marks compliance check as completed
     */
    public void markComplianceChecked(String checkReference, String notes) {
        this.complianceChecked = true;
        this.complianceCheckReference = checkReference;
        this.complianceNotes = notes;
        
        addDomainEvent(new PaymentComplianceCheckedEvent(paymentId, checkReference));
    }

    // Business Logic Methods

    public boolean isCbdcPayment() {
        return cbdcPayment;
    }

    public boolean isStablecoinPayment() {
        return stablecoinPayment;
    }

    public boolean canSettleInstantly() {
        return cbdcPayment && status == PaymentStatus.PENDING;
    }

    public int getMaxSettlementTimeSeconds() {
        return cbdcPayment ? CBDC_MAX_SETTLEMENT_SECONDS : 86400; // 24 hours for non-CBDC
    }

    public boolean meetsInstantSettlementRequirement() {
        return cbdcPayment && settlementTimeSeconds != null && settlementTimeSeconds <= CBDC_MAX_SETTLEMENT_SECONDS;
    }

    public boolean isIslamicCompliant() {
        return islamicCompliant;
    }

    public boolean canChargeInterest() {
        return !islamicCompliant;
    }

    public boolean canIncludeProfitSharing() {
        return islamicCompliant;
    }

    public boolean requiresCurrencyConversion() {
        return requiresCurrencyConversion;
    }

    public boolean isComplianceChecked() {
        return complianceChecked;
    }

    public Money getTotalFees() {
        if (fees.isEmpty()) {
            return Money.zero(amount.getCurrency());
        }
        
        Money total = Money.zero(amount.getCurrency());
        for (PaymentFee fee : fees) {
            total = total.add(fee.getAmount());
        }
        return total;
    }

    public Money getTotalAmountWithFees() {
        return amount.add(getTotalFees());
    }

    // Validation Methods

    private static void validatePaymentParameters(String fromAccountId, String toAccountId, 
                                                Money amount, String reference) {
        if (fromAccountId == null || fromAccountId.trim().isEmpty()) {
            throw new IllegalArgumentException("From account ID cannot be null or empty");
        }
        if (toAccountId == null || toAccountId.trim().isEmpty()) {
            throw new IllegalArgumentException("To account ID cannot be null or empty");
        }
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }
        if (amount == null || !amount.isPositive()) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        if (reference == null || reference.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment reference cannot be null or empty");
        }
    }

    private static void validatePaymentLimits(Money amount) {
        // Check daily limit for AED
        if ("AED".equals(amount.getCurrency()) && 
            amount.getAmount().compareTo(DAILY_LIMIT_AED) > 0) {
            throw new PaymentLimitExceededException(
                String.format("Payment amount %s exceeds daily limit of %s AED", 
                    amount, DAILY_LIMIT_AED)
            );
        }
    }

    private void validateFeeForIslamicCompliance(PaymentFeeType feeType) {
        if (islamicCompliant && 
            (feeType == PaymentFeeType.INTEREST_CHARGE || feeType == PaymentFeeType.LATE_PAYMENT_INTEREST)) {
            throw new IslamicComplianceViolationException(
                "Interest-based fees are not allowed in Islamic banking: " + feeType
            );
        }
    }

    private void validateFeeCurrency(Money feeAmount) {
        if (!this.amount.getCurrency().equals(feeAmount.getCurrency())) {
            throw new IllegalArgumentException(
                String.format("Fee currency %s must match payment currency %s", 
                    feeAmount.getCurrency(), this.amount.getCurrency())
            );
        }
    }

    private static String generatePaymentId() {
        return "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void addDomainEvent(Object event) {
        this.domainEvents.add(event);
    }

    public List<Object> getDomainEvents() {
        return new ArrayList<>(domainEvents);
    }

    public void clearDomainEvents() {
        this.domainEvents.clear();
    }
}