
package com.bank.loanmanagement.loanorigination.domain.model;

import com.bank.loanmanagement.sharedkernel.domain.AggregateRoot;
import com.bank.loanmanagement.sharedkernel.domain.DomainEvent;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "loans", schema = "loan_db")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Loan implements AggregateRoot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "loan_id")
    private Long loanId;
    
    @Column(name = "customer_id", nullable = false)
    private Long customerId;
    
    @Column(name = "loan_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal loanAmount;
    
    @Column(name = "interest_rate", precision = 5, scale = 3, nullable = false)
    private BigDecimal interestRate;
    
    @Column(name = "installments", nullable = false)
    private Integer installments;
    
    @Column(name = "monthly_payment", precision = 15, scale = 2)
    private BigDecimal monthlyPayment;
    
    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "loan_status", nullable = false)
    private LoanStatus loanStatus;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Transient
    private List<DomainEvent> domainEvents = new ArrayList<>();
    
    public void approve() {
        if (loanStatus != LoanStatus.PENDING) {
            throw new IllegalLoanStateException("Can only approve pending loans");
        }
        this.loanStatus = LoanStatus.APPROVED;
        calculatePayments();
        addDomainEvent(new LoanApprovedEvent(this.loanId, this.customerId, this.loanAmount));
    }
    
    public void reject(String reason) {
        if (loanStatus != LoanStatus.PENDING) {
            throw new IllegalLoanStateException("Can only reject pending loans");
        }
        this.loanStatus = LoanStatus.REJECTED;
        addDomainEvent(new LoanRejectedEvent(this.loanId, this.customerId, reason));
    }
    
    public void activate() {
        if (loanStatus != LoanStatus.APPROVED) {
            throw new IllegalLoanStateException("Can only activate approved loans");
        }
        this.loanStatus = LoanStatus.ACTIVE;
        addDomainEvent(new LoanActivatedEvent(this.loanId, this.customerId));
    }
    
    private void calculatePayments() {
        // PMT = P[r(1+r)^n]/[(1+r)^n-1]
        double monthlyRate = interestRate.doubleValue() / 100.0;
        double principal = loanAmount.doubleValue();
        int numPayments = installments;
        
        double payment = principal * (monthlyRate * Math.pow(1 + monthlyRate, numPayments)) / 
                        (Math.pow(1 + monthlyRate, numPayments) - 1);
        
        this.monthlyPayment = BigDecimal.valueOf(Math.round(payment * 100.0) / 100.0);
        this.totalAmount = this.monthlyPayment.multiply(BigDecimal.valueOf(numPayments));
    }
    
    @Override
    public List<DomainEvent> getDomainEvents() {
        return domainEvents;
    }
    
    @Override
    public void clearDomainEvents() {
        domainEvents.clear();
    }
    
    private void addDomainEvent(DomainEvent event) {
        domainEvents.add(event);
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
