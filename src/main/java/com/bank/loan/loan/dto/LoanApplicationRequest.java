package com.bank.loan.loan.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Loan Application Request DTO
 * 
 * Request object for loan application with comprehensive validation
 * for banking regulatory compliance.
 */
public class LoanApplicationRequest {
    
    @NotBlank(message = "Customer ID is required")
    private String customerId;
    
    @NotNull(message = "Loan amount is required")
    @DecimalMin(value = "100.00", message = "Minimum loan amount is $100.00")
    @DecimalMax(value = "10000000.00", message = "Maximum loan amount is $10,000,000.00")
    private BigDecimal amount;
    
    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "USD|EUR|GBP|CAD", message = "Supported currencies: USD, EUR, GBP, CAD")
    private String currency = "USD";
    
    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "0.01", message = "Minimum interest rate is 0.01%")
    @DecimalMax(value = "50.00", message = "Maximum interest rate is 50.00%")
    private Double interestRate;
    
    @NotNull(message = "Installment count is required")
    @Min(value = 1, message = "Minimum installment count is 1")
    @Max(value = 480, message = "Maximum installment count is 480 months")
    private Integer installmentCount;
    
    @NotBlank(message = "Loan type is required")
    @Pattern(regexp = "MORTGAGE|PERSONAL|AUTO|BUSINESS|STUDENT", 
             message = "Valid loan types: MORTGAGE, PERSONAL, AUTO, BUSINESS, STUDENT")
    private String loanType;
    
    @NotBlank(message = "Loan purpose is required")
    @Size(max = 500, message = "Purpose description cannot exceed 500 characters")
    private String purpose;
    
    @Size(max = 1000, message = "Collateral description cannot exceed 1000 characters")
    private String collateralDescription;
    
    @DecimalMin(value = "0.00", message = "Collateral value cannot be negative")
    private BigDecimal collateralValue;

    // Constructors
    public LoanApplicationRequest() {}

    public LoanApplicationRequest(String customerId, BigDecimal amount, Double interestRate, 
                                 Integer installmentCount, String loanType, String purpose) {
        this.customerId = customerId;
        this.amount = amount;
        this.interestRate = interestRate;
        this.installmentCount = installmentCount;
        this.loanType = loanType;
        this.purpose = purpose;
    }

    // Getters and Setters
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

    public String getCollateralDescription() {
        return collateralDescription;
    }

    public void setCollateralDescription(String collateralDescription) {
        this.collateralDescription = collateralDescription;
    }

    public BigDecimal getCollateralValue() {
        return collateralValue;
    }

    public void setCollateralValue(BigDecimal collateralValue) {
        this.collateralValue = collateralValue;
    }

    @Override
    public String toString() {
        return "LoanApplicationRequest{" +
                "customerId='" + customerId + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", interestRate=" + interestRate +
                ", installmentCount=" + installmentCount +
                ", loanType='" + loanType + '\'' +
                ", purpose='" + purpose + '\'' +
                '}';
    }
}