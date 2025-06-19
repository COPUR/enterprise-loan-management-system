
package com.bank.loanmanagement.customermanagement.domain.model;

import com.bank.loanmanagement.sharedkernel.domain.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class CreditReservedEvent implements DomainEvent {
    private final Long customerId;
    private final BigDecimal amount;
    private final LocalDateTime occurredOn = LocalDateTime.now();
}
