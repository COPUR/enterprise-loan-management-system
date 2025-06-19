package com.bank.loanmanagement.domain.loan;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanId implements Serializable {
    
    private String value;
    
    public static LoanId generate() {
        return new LoanId(UUID.randomUUID().toString());
    }
    
    public static LoanId of(String value) {
        return new LoanId(value);
    }
}