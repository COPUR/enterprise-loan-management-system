package com.bank.loan.domain.dto;

import com.bank.loan.domain.model.PaymentMethod;
import com.bank.loan.domain.model.PaymentStatus;
import com.bank.loan.domain.model.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for payment information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    
    private String paymentId;
    private String loanId;
    private String customerId;
    private BigDecimal amount;
    private BigDecimal processingFee;
    private BigDecimal totalAmount;
    private PaymentMethod paymentMethod;
    private PaymentType paymentType;
    private PaymentStatus status;
    private LocalDateTime paymentDate;
    private LocalDateTime processedDate;
    private String processedBy;
    private String description;
    private String paymentReference;
    private String transactionReference;
    
    // For test compatibility
    private Integer installmentNumber;
    private Integer installmentsPaid;
    private BigDecimal totalAmountSpent;
    private Boolean isLoanFullyPaid;
    private LocalDateTime paidAt;
}