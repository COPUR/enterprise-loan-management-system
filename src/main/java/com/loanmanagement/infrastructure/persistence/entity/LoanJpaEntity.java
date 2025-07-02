package com.loanmanagement.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "loan")
public class LoanJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "customer_id", nullable = false)
    private Long customerId;
    
    @Column(name = "loan_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal loanAmount;
    
    @Column(name = "number_of_installment", nullable = false)
    private Integer numberOfInstallment;
    
    @Column(name = "create_date", nullable = false)
    private LocalDate createDate;
    
    @Column(name = "is_paid", nullable = false)
    private Boolean isPaid = false;
    
    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LoanInstallmentJpaEntity> installments = new ArrayList<>();
    
    // Constructors, getters, setters
}
