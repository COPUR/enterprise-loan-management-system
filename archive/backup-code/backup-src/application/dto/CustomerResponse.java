
package com.bank.loanmanagement.application.dto;

import com.bank.loanmanagement.domain.model.Customer;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {
    
    private Long customerId;
    private String name;
    private String email;
    private String phoneNumber;
    private BigDecimal creditLimit;
    private BigDecimal availableCredit;
    private Customer.CustomerStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
