package com.loanmanagement.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "loan_installment")
public class LoanInstallmentJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private LoanJpaEntity loan;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "paid_amount", precision = 15, scale = 2)
    private BigDecimal paidAmount;
    
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
    
    @Column(name = "payment_date")
    private LocalDate paymentDate;
    
    @Column(name = "is_paid", nullable = false)
    private Boolean isPaid = false;
    
    // Constructors, getters, setters
}