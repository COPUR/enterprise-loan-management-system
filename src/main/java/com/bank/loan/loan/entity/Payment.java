package com.bank.loan.loan.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment Entity
 * 
 * Represents a loan payment with comprehensive banking compliance attributes
 * including payment allocation waterfall and regulatory tracking.
 */
public class Payment {
    
    private String paymentId;
    private String loanId;
    private String customerId;
    private BigDecimal amount;
    private String currency = "USD";
    private LocalDateTime paymentDate;
    private String paymentType;
    private String status;
    
    // Payment allocation waterfall components
    private BigDecimal principalAmount;
    private BigDecimal interestAmount;
    private BigDecimal lateFee;
    private BigDecimal processingFee;
    private BigDecimal discountAmount;
    
    // Payment method and processing
    private String paymentMethodType;
    private String paymentReference;
    private String paymentChannel;
    private String processedBy;
    
    // Audit and compliance
    private LocalDateTime createdDate;
    private LocalDateTime processedDate;
    private String idempotencyKey;
    private String fiapiInteractionId;
    
    // Payment allocation details
    private String allocationStrategy;
    private String complianceNotes;

    // Constructors
    public Payment() {}

    public Payment(String paymentId, String loanId, BigDecimal amount, String paymentType) {
        this.paymentId = paymentId;
        this.loanId = loanId;
        this.amount = amount;
        this.paymentType = paymentType;
        this.status = "PENDING";
        this.createdDate = LocalDateTime.now();
    }

    // Getters and Setters
    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getLoanId() {
        return loanId;
    }

    public void setLoanId(String loanId) {
        this.loanId = loanId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getPrincipalAmount() {
        return principalAmount;
    }

    public void setPrincipalAmount(BigDecimal principalAmount) {
        this.principalAmount = principalAmount;
    }

    public BigDecimal getInterestAmount() {
        return interestAmount;
    }

    public void setInterestAmount(BigDecimal interestAmount) {
        this.interestAmount = interestAmount;
    }

    public BigDecimal getLateFee() {
        return lateFee;
    }

    public void setLateFee(BigDecimal lateFee) {
        this.lateFee = lateFee;
    }

    public BigDecimal getProcessingFee() {
        return processingFee;
    }

    public void setProcessingFee(BigDecimal processingFee) {
        this.processingFee = processingFee;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getPaymentMethodType() {
        return paymentMethodType;
    }

    public void setPaymentMethodType(String paymentMethodType) {
        this.paymentMethodType = paymentMethodType;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public String getPaymentChannel() {
        return paymentChannel;
    }

    public void setPaymentChannel(String paymentChannel) {
        this.paymentChannel = paymentChannel;
    }

    public String getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(String processedBy) {
        this.processedBy = processedBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getProcessedDate() {
        return processedDate;
    }

    public void setProcessedDate(LocalDateTime processedDate) {
        this.processedDate = processedDate;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getFiapiInteractionId() {
        return fiapiInteractionId;
    }

    public void setFiapiInteractionId(String fiapiInteractionId) {
        this.fiapiInteractionId = fiapiInteractionId;
    }

    public String getAllocationStrategy() {
        return allocationStrategy;
    }

    public void setAllocationStrategy(String allocationStrategy) {
        this.allocationStrategy = allocationStrategy;
    }

    public String getComplianceNotes() {
        return complianceNotes;
    }

    public void setComplianceNotes(String complianceNotes) {
        this.complianceNotes = complianceNotes;
    }

    @Override
    public String toString() {
        return "Payment{" +
                "paymentId='" + paymentId + '\'' +
                ", loanId='" + loanId + '\'' +
                ", amount=" + amount +
                ", paymentType='" + paymentType + '\'' +
                ", status='" + status + '\'' +
                ", paymentDate=" + paymentDate +
                '}';
    }
}