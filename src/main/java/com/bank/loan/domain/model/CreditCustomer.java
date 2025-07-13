package com.bank.loan.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Credit Customer entity for integration tests
 * Simplified version focused on credit operations
 */
@Entity
@Table(name = "credit_customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditCustomer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "surname", nullable = false)
    private String surname;
    
    @Column(name = "credit_limit", precision = 15, scale = 2, nullable = false)
    private BigDecimal creditLimit;
    
    @Column(name = "used_credit_limit", precision = 15, scale = 2)
    private BigDecimal usedCreditLimit;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (usedCreditLimit == null) {
            usedCreditLimit = BigDecimal.ZERO;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Get available credit limit
     */
    public BigDecimal getAvailableCreditLimit() {
        return creditLimit.subtract(usedCreditLimit != null ? usedCreditLimit : BigDecimal.ZERO);
    }
    
    /**
     * Check if customer has sufficient credit for amount
     */
    public boolean hasSufficientCredit(BigDecimal amount) {
        return getAvailableCreditLimit().compareTo(amount) >= 0;
    }
    
    /**
     * Reserve credit for a loan
     */
    public void reserveCredit(BigDecimal amount) {
        if (!hasSufficientCredit(amount)) {
            throw new IllegalArgumentException("Insufficient credit limit");
        }
        this.usedCreditLimit = this.usedCreditLimit.add(amount);
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Release reserved credit
     */
    public void releaseCredit(BigDecimal amount) {
        this.usedCreditLimit = this.usedCreditLimit.subtract(amount);
        if (this.usedCreditLimit.compareTo(BigDecimal.ZERO) < 0) {
            this.usedCreditLimit = BigDecimal.ZERO;
        }
        this.updatedAt = LocalDateTime.now();
    }
}