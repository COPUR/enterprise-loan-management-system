package com.bank.loanmanagement.loan.application.results;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CustomerDetails(
    String customerId,
    String firstName,
    String lastName,
    String email,
    String phoneNumber,
    LocalDate dateOfBirth,
    String kycStatus,
    String creditScore,
    BigDecimal monthlyIncome,
    LocalDateTime createdAt,
    Boolean isActive
) {}