
package com.bank.loanmanagement.customermanagement.infrastructure.adapter.in.web.dto;

import com.bank.loanmanagement.customermanagement.domain.model.CustomerStatus;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateCustomerRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private Integer creditScore;
    private BigDecimal monthlyIncome;
    private BigDecimal creditLimit;
    private CustomerStatus status;
}
