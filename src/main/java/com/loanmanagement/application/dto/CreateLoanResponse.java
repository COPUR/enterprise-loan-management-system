package com.loanmanagement.application.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for loan creation operations.
 * Follows clean code principles with minimal, necessary imports.
 */
public record CreateLoanResponse(
    Long loanId,
    BigDecimal loanAmount,
    Integer numberOfInstallments,
    List<InstallmentDto> installments
) {}