package com.bank.loanmanagement.paymentprocessing.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Payment {
    private Long id;
    private String paymentNumber;
    private Long loanId;
    private Long customerId;
    private String paymentType;
    private BigDecimal scheduledAmount;
    private BigDecimal actualAmount;
    private BigDecimal principalAmount;
    private BigDecimal interestAmount;
    private BigDecimal penaltyAmount;
    private LocalDate scheduledDate;
    private LocalDateTime actualPaymentDate;
    private String paymentStatus;
    private String paymentMethod;
    private String transactionReference;
    private String processorReference;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version;

    // Constructors
    public Payment() {}

    public Payment(String paymentNumber, Long loanId, Long customerId, 
                   BigDecimal scheduledAmount, LocalDate scheduledDate) {
        this.paymentNumber = paymentNumber;
        this.loanId = loanId;
        this.customerId = customerId;
        this.scheduledAmount = scheduledAmount;
        this.scheduledDate = scheduledDate;
        this.paymentType = "REGULAR";
        this.paymentStatus = "PENDING";
        this.penaltyAmount = BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.version = 0;
    }

    // Business Logic Methods
    public void calculateAmounts(BigDecimal outstandingBalance, BigDecimal monthlyInterestRate) {
        if (scheduledAmount != null && monthlyInterestRate != null && outstandingBalance != null) {
            // Calculate interest portion
            this.interestAmount = outstandingBalance.multiply(monthlyInterestRate)
                .setScale(2, BigDecimal.ROUND_HALF_UP);
            
            // Calculate principal portion
            this.principalAmount = scheduledAmount.subtract(interestAmount)
                .setScale(2, BigDecimal.ROUND_HALF_UP);
            
            // Ensure principal is not negative
            if (principalAmount.compareTo(BigDecimal.ZERO) < 0) {
                this.principalAmount = BigDecimal.ZERO;
                this.interestAmount = scheduledAmount;
            }
        }
    }

    public void applyLatePenalty(BigDecimal penaltyRate) {
        if (isLate() && scheduledAmount != null) {
            this.penaltyAmount = scheduledAmount.multiply(penaltyRate)
                .setScale(2, BigDecimal.ROUND_HALF_UP);
        }
    }

    public boolean isLate() {
        return scheduledDate != null && LocalDate.now().isAfter(scheduledDate) && 
               !"COMPLETED".equals(paymentStatus);
    }

    public boolean isPending() {
        return "PENDING".equals(paymentStatus);
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(paymentStatus);
    }

    public boolean isFailed() {
        return "FAILED".equals(paymentStatus);
    }

    public void markAsCompleted(BigDecimal actualAmount, String transactionRef) {
        this.actualAmount = actualAmount;
        this.actualPaymentDate = LocalDateTime.now();
        this.paymentStatus = "COMPLETED";
        this.transactionReference = transactionRef;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsFailed(String reason) {
        this.paymentStatus = "FAILED";
        this.failureReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPaymentNumber() { return paymentNumber; }
    public void setPaymentNumber(String paymentNumber) { this.paymentNumber = paymentNumber; }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }

    public BigDecimal getScheduledAmount() { return scheduledAmount; }
    public void setScheduledAmount(BigDecimal scheduledAmount) { this.scheduledAmount = scheduledAmount; }

    public BigDecimal getActualAmount() { return actualAmount; }
    public void setActualAmount(BigDecimal actualAmount) { this.actualAmount = actualAmount; }

    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; }

    public BigDecimal getInterestAmount() { return interestAmount; }
    public void setInterestAmount(BigDecimal interestAmount) { this.interestAmount = interestAmount; }

    public BigDecimal getPenaltyAmount() { return penaltyAmount; }
    public void setPenaltyAmount(BigDecimal penaltyAmount) { this.penaltyAmount = penaltyAmount; }

    public LocalDate getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDate scheduledDate) { this.scheduledDate = scheduledDate; }

    public LocalDateTime getActualPaymentDate() { return actualPaymentDate; }
    public void setActualPaymentDate(LocalDateTime actualPaymentDate) { 
        this.actualPaymentDate = actualPaymentDate; 
    }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) { 
        this.transactionReference = transactionReference; 
    }

    public String getProcessorReference() { return processorReference; }
    public void setProcessorReference(String processorReference) { 
        this.processorReference = processorReference; 
    }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public BigDecimal getTotalAmount() {
        BigDecimal total = actualAmount != null ? actualAmount : scheduledAmount;
        if (total != null && penaltyAmount != null) {
            total = total.add(penaltyAmount);
        }
        return total;
    }
}