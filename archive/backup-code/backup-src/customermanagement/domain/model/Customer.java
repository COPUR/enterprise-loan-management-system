
package com.bank.loanmanagement.customermanagement.domain.model;

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
@Table(name = "customers", schema = "customer_db")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer implements AggregateRoot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Long customerId;
    
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;
    
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;
    
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;
    
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;
    
    @Column(name = "credit_score")
    private Integer creditScore;
    
    @Column(name = "monthly_income", precision = 15, scale = 2)
    private BigDecimal monthlyIncome;
    
    @Column(name = "credit_limit", precision = 15, scale = 2)
    private BigDecimal creditLimit;
    
    @Column(name = "available_credit", precision = 15, scale = 2)
    private BigDecimal availableCredit;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CustomerStatus status;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Transient
    private List<DomainEvent> domainEvents = new ArrayList<>();
    
    public void reserveCredit(BigDecimal amount) {
        validateCreditReservation(amount);
        this.availableCredit = this.availableCredit.subtract(amount);
        addDomainEvent(new CreditReservedEvent(this.customerId, amount));
    }
    
    public void releaseCredit(BigDecimal amount) {
        this.availableCredit = this.availableCredit.add(amount);
        addDomainEvent(new CreditReleasedEvent(this.customerId, amount));
    }
    
    private void validateCreditReservation(BigDecimal amount) {
        if (amount.compareTo(this.availableCredit) > 0) {
            throw new InsufficientCreditException("Insufficient credit available");
        }
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
