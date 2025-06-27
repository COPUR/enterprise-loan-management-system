package com.banking.loan.application.commands;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateCustomerCommand(
    String firstName,
    String lastName,
    String email,
    String phoneNumber,
    LocalDate dateOfBirth,
    String nationalId,
    BigDecimal monthlyIncome,
    String address
) {}