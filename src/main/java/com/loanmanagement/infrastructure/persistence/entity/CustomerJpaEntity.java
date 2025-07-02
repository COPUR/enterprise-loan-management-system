package com.loanmanagement.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "customer")
public class CustomerJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String surname;
    
    @Column(name = "credit_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal creditLimit;
    
    @Column(name = "used_credit_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal usedCreditLimit = BigDecimal.ZERO;
    
    // Constructors, getters, setters
    public CustomerJpaEntity() {}
    
    public CustomerJpaEntity(String name, String surname, BigDecimal creditLimit) {
        this.name = name;
        this.surname = surname;
        this.creditLimit = creditLimit;
    }
    
    // Getters and setters omitted for brevity
}


