package com.bank.loanmanagement.loan.domain.customer;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditCustomer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String surname;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal creditLimit;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal usedCreditLimit;
    
    public BigDecimal getAvailableCredit() {
        return creditLimit.subtract(usedCreditLimit);
    }
    
    public boolean hasEnoughCreditFor(BigDecimal amount) {
        return getAvailableCredit().compareTo(amount) >= 0;
    }
    
    public void allocateCredit(BigDecimal amount) {
        if (!hasEnoughCreditFor(amount)) {
            throw new IllegalArgumentException("Insufficient credit limit");
        }
        this.usedCreditLimit = this.usedCreditLimit.add(amount);
    }
    
    public void releaseCredit(BigDecimal amount) {
        this.usedCreditLimit = this.usedCreditLimit.subtract(amount);
        if (this.usedCreditLimit.compareTo(BigDecimal.ZERO) < 0) {
            this.usedCreditLimit = BigDecimal.ZERO;
        }
    }
}