
package com.bank.loanmanagement.customermanagement.infrastructure.adapter.in.web.dto;

import com.bank.loanmanagement.customermanagement.domain.model.CustomerStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CustomerResponse {
    private Long customerId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private Integer creditScore;
    private BigDecimal monthlyIncome;
    private BigDecimal creditLimit;
    private BigDecimal availableCredit;
    private CustomerStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
