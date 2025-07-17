package com.bank.payment.domain;

import com.bank.shared.kernel.domain.AggregateRoot;
import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Payment Aggregate Root
 * 
 * Represents a financial payment with its processing lifecycle,
 * validation rules, and business logic for payment management.
 */
public class Payment extends AggregateRoot<PaymentId> {
    
    private PaymentId paymentId;
    private CustomerId customerId;
    private AccountId fromAccountId;
    private AccountId toAccountId;
    private Money amount;
    private Money fee;
    private PaymentType paymentType;
    private PaymentStatus status;
    private String description;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime updatedAt;
    
    // Loan payment specific fields
    private LoanId loanId;
    private Money scheduledAmount;
    private Money actualAmount;
    private Money principalAmount;
    private Money interestAmount;
    private Money penaltyAmount;
    private LocalDate scheduledDate;
    private LocalDateTime actualPaymentDate;
    private String transactionReference;
    
    // Private constructor for JPA
    protected Payment() {}
    
    private Payment(PaymentId paymentId, CustomerId customerId, AccountId fromAccountId,
                   AccountId toAccountId, Money amount, PaymentType paymentType, String description) {
        this.paymentId = Objects.requireNonNull(paymentId, "Payment ID cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.fromAccountId = Objects.requireNonNull(fromAccountId, "From account ID cannot be null");
        this.toAccountId = Objects.requireNonNull(toAccountId, "To account ID cannot be null");
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null");
        this.paymentType = Objects.requireNonNull(paymentType, "Payment type cannot be null");
        this.description = description;
        this.fee = paymentType.calculateFee(amount);
        this.status = PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        
        validatePaymentData();
        
        // Domain event
        addDomainEvent(new PaymentCreatedEvent(paymentId, customerId, amount, paymentType));
    }
    
    public static Payment create(PaymentId paymentId, CustomerId customerId, AccountId fromAccountId,
                                AccountId toAccountId, Money amount, PaymentType paymentType, String description) {
        return new Payment(paymentId, customerId, fromAccountId, toAccountId, amount, paymentType, description);
    }
    
    // Loan payment specific constructor
    private Payment(PaymentId paymentId, CustomerId customerId, LoanId loanId, Money scheduledAmount, LocalDate scheduledDate) {
        if (paymentId == null) {
            throw new IllegalArgumentException("Payment ID cannot be null");
        }
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }
        if (loanId == null) {
            throw new IllegalArgumentException("Loan ID cannot be null");
        }
        if (scheduledAmount == null) {
            throw new IllegalArgumentException("Scheduled amount cannot be null");
        }
        
        this.paymentId = paymentId;
        this.customerId = customerId;
        this.loanId = loanId;
        this.scheduledAmount = scheduledAmount;
        this.scheduledDate = scheduledDate;
        this.amount = scheduledAmount;
        this.paymentType = PaymentType.LOAN_PAYMENT;
        this.status = PaymentStatus.PENDING;
        this.penaltyAmount = Money.zero(scheduledAmount.getCurrency());
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        
        validateLoanPaymentData();
        
        // Domain event
        addDomainEvent(new LoanPaymentCreatedEvent(paymentId, customerId, loanId, scheduledAmount));
    }
    
    // Loan payment with interest calculation constructor
    private Payment(PaymentId paymentId, CustomerId customerId, LoanId loanId, Money scheduledAmount, 
                   Money outstandingBalance, BigDecimal monthlyInterestRate) {
        this(paymentId, customerId, loanId, scheduledAmount, LocalDate.now());
        calculateAmounts(outstandingBalance, monthlyInterestRate);
    }
    
    public static Payment createLoanPayment(PaymentId paymentId, CustomerId customerId, LoanId loanId, 
                                          Money scheduledAmount, LocalDate scheduledDate) {
        return new Payment(paymentId, customerId, loanId, scheduledAmount, scheduledDate);
    }
    
    public static Payment createLoanPayment(PaymentId paymentId, CustomerId customerId, LoanId loanId, 
                                          Money scheduledAmount, Money outstandingBalance, BigDecimal monthlyInterestRate) {
        return new Payment(paymentId, customerId, loanId, scheduledAmount, outstandingBalance, monthlyInterestRate);
    }
    
    private void validatePaymentData() {
        if (amount.isNegative() || amount.isZero()) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        if (fromAccountId != null && toAccountId != null && fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("From and to accounts cannot be the same");
        }
    }
    
    private void validateLoanPaymentData() {
        if (scheduledAmount.isNegative() || scheduledAmount.isZero()) {
            throw new IllegalArgumentException("Scheduled amount must be positive");
        }
    }
    
    @Override
    public PaymentId getId() {
        return paymentId;
    }
    
    public CustomerId getCustomerId() {
        return customerId;
    }
    
    public AccountId getFromAccountId() {
        return fromAccountId;
    }
    
    public AccountId getToAccountId() {
        return toAccountId;
    }
    
    public Money getAmount() {
        return amount;
    }
    
    public Money getFee() {
        return fee;
    }
    
    public Money getTotalAmount() {
        if (loanId != null) {
            // For loan payments, total includes penalty
            Money base = actualAmount != null ? actualAmount : scheduledAmount;
            if (base != null && penaltyAmount != null) {
                return base.add(penaltyAmount);
            }
            return base;
        }
        // Regular payment logic
        return amount.add(fee);
    }
    
    public PaymentType getPaymentType() {
        return paymentType;
    }
    
    public PaymentStatus getStatus() {
        return status;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getFailureReason() {
        return failureReason;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void confirm() {
        if (!status.canBeConfirmed()) {
            throw new IllegalStateException("Payment cannot be confirmed in current status: " + status);
        }
        this.status = PaymentStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new PaymentCompletedEvent(paymentId, customerId, amount));
    }
    
    public void fail(String reason) {
        if (!status.canBeFailed()) {
            throw new IllegalStateException("Payment cannot be failed in current status: " + status);
        }
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new PaymentFailedEvent(paymentId, customerId, reason));
    }
    
    public void cancel(String reason) {
        if (!status.canBeCancelled()) {
            throw new IllegalStateException("Payment cannot be cancelled in current status: " + status);
        }
        this.status = PaymentStatus.CANCELLED;
        this.failureReason = reason;
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new PaymentCancelledEvent(paymentId, customerId, reason));
    }
    
    public void refund() {
        if (!status.canBeRefunded()) {
            throw new IllegalStateException("Payment cannot be refunded in current status: " + status);
        }
        this.status = PaymentStatus.REFUNDED;
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new PaymentRefundedEvent(paymentId, customerId, amount));
    }
    
    public void markAsProcessing() {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only pending payments can be marked as processing");
        }
        this.status = PaymentStatus.PROCESSING;
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new PaymentProcessingEvent(paymentId, customerId));
    }
    
    public boolean isCompleted() {
        return status == PaymentStatus.COMPLETED;
    }
    
    public boolean isFailed() {
        return status == PaymentStatus.FAILED;
    }
    
    public boolean isPending() {
        return status == PaymentStatus.PENDING;
    }
    
    // Loan payment specific getters
    public LoanId getLoanId() {
        return loanId;
    }
    
    public Money getScheduledAmount() {
        return scheduledAmount;
    }
    
    public Money getActualAmount() {
        return actualAmount;
    }
    
    public Money getPrincipalAmount() {
        return principalAmount;
    }
    
    public Money getInterestAmount() {
        return interestAmount;
    }
    
    public Money getPenaltyAmount() {
        return penaltyAmount;
    }
    
    public LocalDate getScheduledDate() {
        return scheduledDate;
    }
    
    public LocalDateTime getActualPaymentDate() {
        return actualPaymentDate;
    }
    
    public String getTransactionReference() {
        return transactionReference;
    }
    
    
    // Loan payment specific business logic methods
    public void calculateAmounts(Money outstandingBalance, BigDecimal monthlyInterestRate) {
        if (scheduledAmount != null && monthlyInterestRate != null && outstandingBalance != null) {
            // Calculate interest portion
            this.interestAmount = outstandingBalance.multiply(monthlyInterestRate);
            
            // Calculate principal portion
            this.principalAmount = scheduledAmount.subtract(interestAmount);
            
            // Ensure principal is not negative
            if (principalAmount.isNegative()) {
                this.principalAmount = Money.zero(scheduledAmount.getCurrency());
                this.interestAmount = scheduledAmount;
            }
        }
    }
    
    public void applyLatePenalty(BigDecimal penaltyRate) {
        if (isLate() && scheduledAmount != null) {
            this.penaltyAmount = scheduledAmount.multiply(penaltyRate);
        }
    }
    
    public boolean isLate() {
        return scheduledDate != null && 
               LocalDate.now().isAfter(scheduledDate) && 
               !isCompleted();
    }
    
    public void markAsCompleted(Money actualAmount, String transactionRef) {
        if (!canBeCompleted()) {
            throw new IllegalStateException("Payment cannot be completed in current status: " + status);
        }
        
        this.actualAmount = actualAmount;
        this.actualPaymentDate = LocalDateTime.now();
        this.status = PaymentStatus.COMPLETED;
        this.transactionReference = transactionRef;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new LoanPaymentCompletedEvent(paymentId, customerId, loanId, actualAmount));
    }
    
    public void markAsFailed(String reason) {
        if (!canBeFailed()) {
            throw new IllegalStateException("Payment cannot be failed in current status: " + status);
        }
        
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new LoanPaymentFailedEvent(paymentId, customerId, loanId, reason));
    }
    
    private boolean canBeCompleted() {
        return status == PaymentStatus.PENDING || status == PaymentStatus.PROCESSING;
    }
    
    private boolean canBeFailed() {
        return status == PaymentStatus.PENDING || status == PaymentStatus.PROCESSING;
    }
}