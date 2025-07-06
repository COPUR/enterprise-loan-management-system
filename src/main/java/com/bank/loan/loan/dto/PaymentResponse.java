package com.bank.loan.loan.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment Response DTO
 * 
 * Response object for payment operations with comprehensive payment details
 * including payment allocation waterfall information for regulatory compliance.
 */
public class PaymentResponse {
    
    private String paymentId;
    private String loanId;
    private BigDecimal amount;
    private String currency;
    private String paymentType;
    private String paymentMethodType;
    private String status;
    private LocalDateTime paymentDate;
    private LocalDateTime processedDate;
    
    // Payment allocation waterfall details
    private BigDecimal principalAmount;
    private BigDecimal interestAmount;
    private BigDecimal lateFee;
    private BigDecimal processingFee;
    private BigDecimal discountAmount;
    
    // Payment processing details
    private String paymentReference;
    private String paymentChannel;
    private String processedBy;
    
    // Compliance and audit information
    private String allocationNotes;
    private String complianceStatus;

    // Constructors
    public PaymentResponse() {}

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

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getPaymentMethodType() {
        return paymentMethodType;
    }

    public void setPaymentMethodType(String paymentMethodType) {
        this.paymentMethodType = paymentMethodType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public LocalDateTime getProcessedDate() {
        return processedDate;
    }

    public void setProcessedDate(LocalDateTime processedDate) {
        this.processedDate = processedDate;
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

    public String getAllocationNotes() {
        return allocationNotes;
    }

    public void setAllocationNotes(String allocationNotes) {
        this.allocationNotes = allocationNotes;
    }

    public String getComplianceStatus() {
        return complianceStatus;
    }

    public void setComplianceStatus(String complianceStatus) {
        this.complianceStatus = complianceStatus;
    }

    @Override
    public String toString() {
        return "PaymentResponse{" +
                "paymentId='" + paymentId + '\'' +
                ", loanId='" + loanId + '\'' +
                ", amount=" + amount +
                ", paymentType='" + paymentType + '\'' +
                ", status='" + status + '\'' +
                ", processedDate=" + processedDate +
                '}';
    }
}