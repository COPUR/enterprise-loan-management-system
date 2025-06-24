
package com.bank.loanmanagement.customermanagement.domain.port.in;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CreateCustomerCommand {
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private Integer creditScore;
    private BigDecimal monthlyIncome;
    private BigDecimal creditLimit;
}
