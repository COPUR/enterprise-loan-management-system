package com.bank.loan.domain.dto;

import com.bank.loan.domain.model.InstallmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for loan installment information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstallmentResponse {
    
    private Long id;
    private Long loanId;
    private Integer installmentNumber;
    private BigDecimal amount;
    private BigDecimal principalAmount;
    private BigDecimal interestAmount;
    private BigDecimal paidAmount;
    private BigDecimal remainingBalance;
    private LocalDate dueDate;
    private LocalDateTime paidDate;
    private InstallmentStatus status;
    private Boolean isPaid;
    private Integer daysOverdue;
    private String paymentReference;
    
    // Helper method for tests
    public BigDecimal getRemainingAmount() {
        if (paidAmount == null) {
            return amount;
        }
        return amount.subtract(paidAmount);
    }
}