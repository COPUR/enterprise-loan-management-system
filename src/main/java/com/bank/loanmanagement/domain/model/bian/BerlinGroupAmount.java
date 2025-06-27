package com.bank.loanmanagement.domain.model.bian;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;

@Data
@Builder
public class BerlinGroupAmount {
    private BigDecimal amount;
    private String currency;
    
    public static BerlinGroupAmount of(BigDecimal amount, String currency) {
        return BerlinGroupAmount.builder()
            .amount(amount)
            .currency(currency)
            .build();
    }
}