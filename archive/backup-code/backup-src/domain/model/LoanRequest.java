package com.bank.loanmanagement.domain.model;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanRequest {
    private String customerId;
    private String loanType;
    private BigDecimal requestedAmount;
    private Integer termMonths;
    private String purpose;
    private BigDecimal monthlyIncome;
    private Integer employmentYears;
    private BigDecimal collateralValue;
    private BigDecimal businessRevenue;
    private BigDecimal propertyValue;
    private BigDecimal downPayment;
    private LocalDateTime requestDate;
    private String priority;
    private String status;
    
    // Additional fields for AI processing
    private String originalPrompt;
    private Double confidenceScore;
    private String processingNotes;
}