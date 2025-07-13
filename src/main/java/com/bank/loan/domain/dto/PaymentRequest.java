package com.bank.loan.domain.dto;

import com.bank.loan.domain.model.PaymentMethod;
import com.bank.loan.domain.model.PaymentType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for making a payment
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {
    
    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be positive")
    private BigDecimal amount;
    
    private PaymentMethod paymentMethod;
    private PaymentType paymentType;
    private String paymentReference;
    private String description;
    private String transactionReference;
}