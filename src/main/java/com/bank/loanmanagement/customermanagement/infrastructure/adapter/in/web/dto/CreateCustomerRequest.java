
package com.bank.loanmanagement.customermanagement.infrastructure.adapter.in.web.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateCustomerRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private Integer creditScore;
    private BigDecimal monthlyIncome;
    private BigDecimal creditLimit;
}
