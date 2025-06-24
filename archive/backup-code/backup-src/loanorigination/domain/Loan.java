package com.bank.loanmanagement.loanorigination.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Loan {
    private Long id;
    private String loanNumber;
    private Long customerId;
    private BigDecimal principalAmount;
    private Integer installmentCount;
    private BigDecimal monthlyInterestRate;
    private BigDecimal monthlyPaymentAmount;
    private BigDecimal totalAmount;
    private BigDecimal outstandingBalance;
    private String loanStatus;
    private LocalDateTime disbursementDate;
    private LocalDate maturityDate;
    private LocalDate nextPaymentDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version;

    // Constructors
    public Loan() {}

    public Loan(String loanNumber, Long customerId, BigDecimal principalAmount, 
                Integer installmentCount, BigDecimal monthlyInterestRate) {
        this.loanNumber = loanNumber;
        this.customerId = customerId;
        this.principalAmount = principalAmount;
        this.installmentCount = installmentCount;
        this.monthlyInterestRate = monthlyInterestRate;
        this.loanStatus = "ACTIVE";
        this.outstandingBalance = principalAmount;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.version = 0;
        calculatePaymentDetails();
    }

    // Business Logic Methods
    public void calculatePaymentDetails() {
        if (principalAmount != null && monthlyInterestRate != null && installmentCount != null) {
            // Calculate monthly payment using standard loan formula
            // M = P * [r(1+r)^n] / [(1+r)^n - 1]
            double r = monthlyInterestRate.doubleValue();
            int n = installmentCount;
            double p = principalAmount.doubleValue();
            
            double monthlyPayment = p * (r * Math.pow(1 + r, n)) / (Math.pow(1 + r, n) - 1);
            this.monthlyPaymentAmount = BigDecimal.valueOf(monthlyPayment).setScale(2, BigDecimal.ROUND_HALF_UP);
            this.totalAmount = this.monthlyPaymentAmount.multiply(BigDecimal.valueOf(n));
        }
    }

    public boolean isValidInstallmentCount() {
        return installmentCount != null && 
               (installmentCount == 6 || installmentCount == 9 || 
                installmentCount == 12 || installmentCount == 24);
    }

    public boolean isValidInterestRate() {
        return monthlyInterestRate != null && 
               monthlyInterestRate.compareTo(BigDecimal.valueOf(0.001)) >= 0 && 
               monthlyInterestRate.compareTo(BigDecimal.valueOf(0.005)) <= 0;
    }

    public boolean isActive() {
        return "ACTIVE".equals(loanStatus);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLoanNumber() { return loanNumber; }
    public void setLoanNumber(String loanNumber) { this.loanNumber = loanNumber; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(BigDecimal principalAmount) { 
        this.principalAmount = principalAmount;
        calculatePaymentDetails();
    }

    public Integer getInstallmentCount() { return installmentCount; }
    public void setInstallmentCount(Integer installmentCount) { 
        this.installmentCount = installmentCount;
        calculatePaymentDetails();
    }

    public BigDecimal getMonthlyInterestRate() { return monthlyInterestRate; }
    public void setMonthlyInterestRate(BigDecimal monthlyInterestRate) { 
        this.monthlyInterestRate = monthlyInterestRate;
        calculatePaymentDetails();
    }

    public BigDecimal getMonthlyPaymentAmount() { return monthlyPaymentAmount; }
    public void setMonthlyPaymentAmount(BigDecimal monthlyPaymentAmount) { 
        this.monthlyPaymentAmount = monthlyPaymentAmount; 
    }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getOutstandingBalance() { return outstandingBalance; }
    public void setOutstandingBalance(BigDecimal outstandingBalance) { 
        this.outstandingBalance = outstandingBalance; 
    }

    public String getLoanStatus() { return loanStatus; }
    public void setLoanStatus(String loanStatus) { this.loanStatus = loanStatus; }

    public LocalDateTime getDisbursementDate() { return disbursementDate; }
    public void setDisbursementDate(LocalDateTime disbursementDate) { 
        this.disbursementDate = disbursementDate; 
    }

    public LocalDate getMaturityDate() { return maturityDate; }
    public void setMaturityDate(LocalDate maturityDate) { this.maturityDate = maturityDate; }

    public LocalDate getNextPaymentDate() { return nextPaymentDate; }
    public void setNextPaymentDate(LocalDate nextPaymentDate) { 
        this.nextPaymentDate = nextPaymentDate; 
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}