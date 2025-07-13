package com.bank.loan.domain.dto;

import com.bank.loan.domain.model.LoanType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating a loan application
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanApplicationRequest {
    
    @NotNull(message = "Customer ID is required")
    @Positive(message = "Customer ID must be positive")
    private Long customerId;
    
    @NotNull(message = "Loan type is required")
    private LoanType loanType;
    
    @NotNull(message = "Requested amount is required")
    @DecimalMin(value = "1000.00", message = "Minimum loan amount is $1,000")
    @DecimalMax(value = "10000000.00", message = "Maximum loan amount is $10,000,000")
    private BigDecimal requestedAmount;
    
    @NotNull(message = "Term in months is required")
    @Min(value = 6, message = "Minimum loan term is 6 months")
    @Max(value = 480, message = "Maximum loan term is 480 months")
    private Integer requestedTermMonths;
    
    @NotBlank(message = "Purpose is required")
    @Size(max = 500, message = "Purpose cannot exceed 500 characters")
    private String purpose;
    
    @DecimalMin(value = "0.00", message = "Monthly income cannot be negative")
    private BigDecimal monthlyIncome;
    
    @Min(value = 0, message = "Employment years cannot be negative")
    private Integer employmentYears;
    
    @DecimalMin(value = "0.00", message = "Collateral value cannot be negative")
    private BigDecimal collateralValue;
    
    @DecimalMin(value = "0.00", message = "Business revenue cannot be negative")
    private BigDecimal businessRevenue;
    
    @DecimalMin(value = "0.00", message = "Property value cannot be negative")
    private BigDecimal propertyValue;
    
    @DecimalMin(value = "0.00", message = "Down payment cannot be negative")
    private BigDecimal downPayment;
}