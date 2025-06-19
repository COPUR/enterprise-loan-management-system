package com.bank.loanmanagement.domain.payment;

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
public class PaymentId implements Serializable {
    
    private String value;
    
    public static PaymentId generate() {
        return new PaymentId(UUID.randomUUID().toString());
    }
    
    public static PaymentId of(String value) {
        return new PaymentId(value);
    }
}