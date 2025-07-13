package com.bank.loan.domain.dto;

import com.bank.loan.domain.model.LoanStatus;
import com.bank.loan.domain.model.LoanType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for loan information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanResponse {
    
    private String loanId;
    private Long customerId;
    private BigDecimal amount;
    private BigDecimal loanAmount; // Principal amount
    private BigDecimal totalAmount; // Principal + interest
    private BigDecimal interestRate;
    private BigDecimal monthlyPayment;
    private Integer numberOfInstallments;
    private Integer termMonths;
    private LoanType loanType;
    private LoanStatus status;
    private Boolean isPaid;
    private LocalDate disbursementDate;
    private LocalDate maturityDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // For integration tests
    private List<InstallmentResponse> installments;
}