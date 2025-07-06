package com.bank.loan.loan.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Loan Entity
 * 
 * Represents a loan in the banking system with comprehensive attributes
 * for regulatory compliance and business operations.
 */
public class Loan {
    
    private String loanId;
    private String customerId;
    private BigDecimal amount;
    private String currency = "USD";
    private Double interestRate;
    private Integer installmentCount;
    private BigDecimal monthlyPayment;
    private String loanType;
    private String purpose;
    private String status;
    private String collateralType;
    private BigDecimal collateralValue;
    private String createdBy;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
    private String approvedBy;
    private LocalDateTime approvedDate;
    private String approvalNotes;
    private String approvalConditions;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // Constructors
    public Loan() {}

    public Loan(String loanId, String customerId, BigDecimal amount, Double interestRate, 
                Integer installmentCount, String loanType, String purpose) {
        this.loanId = loanId;
        this.customerId = customerId;
        this.amount = amount;
        this.interestRate = interestRate;
        this.installmentCount = installmentCount;
        this.loanType = loanType;
        this.purpose = purpose;
        this.status = "PENDING_APPROVAL";
        this.createdDate = LocalDateTime.now();
        this.lastModifiedDate = LocalDateTime.now();
    }

    // Getters and Setters
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

    public Double getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(Double interestRate) {
        this.interestRate = interestRate;
    }

    public Integer getInstallmentCount() {
        return installmentCount;
    }

    public void setInstallmentCount(Integer installmentCount) {
        this.installmentCount = installmentCount;
    }

    public BigDecimal getMonthlyPayment() {
        return monthlyPayment;
    }

    public void setMonthlyPayment(BigDecimal monthlyPayment) {
        this.monthlyPayment = monthlyPayment;
    }

    public String getLoanType() {
        return loanType;
    }

    public void setLoanType(String loanType) {
        this.loanType = loanType;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCollateralType() {
        return collateralType;
    }

    public void setCollateralType(String collateralType) {
        this.collateralType = collateralType;
    }

    public BigDecimal getCollateralValue() {
        return collateralValue;
    }

    public void setCollateralValue(BigDecimal collateralValue) {
        this.collateralValue = collateralValue;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDateTime getApprovedDate() {
        return approvedDate;
    }

    public void setApprovedDate(LocalDateTime approvedDate) {
        this.approvedDate = approvedDate;
    }

    public String getApprovalNotes() {
        return approvalNotes;
    }

    public void setApprovalNotes(String approvalNotes) {
        this.approvalNotes = approvalNotes;
    }

    public String getApprovalConditions() {
        return approvalConditions;
    }

    public void setApprovalConditions(String approvalConditions) {
        this.approvalConditions = approvalConditions;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    @Override
    public String toString() {
        return "Loan{" +
                "loanId='" + loanId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", interestRate=" + interestRate +
                ", installmentCount=" + installmentCount +
                ", loanType='" + loanType + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}