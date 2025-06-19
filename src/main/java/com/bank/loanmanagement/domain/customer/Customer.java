package com.bank.loanmanagement.domain.customer;

import com.bank.loanmanagement.domain.shared.AggregateRoot;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "customers", indexes = {
    @Index(name = "idx_customer_email", columnList = "email"),
    @Index(name = "idx_customer_phone", columnList = "phoneNumber"),
    @Index(name = "idx_customer_ssn", columnList = "ssn"),
    @Index(name = "idx_customer_status", columnList = "status")
})
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer extends AggregateRoot<CustomerId> {
    
    @EmbeddedId
    private CustomerId id;
    
    @Column(nullable = false)
    private String firstName;
    
    @Column(nullable = false)
    private String lastName;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String phoneNumber;
    
    @Column(unique = true)
    private String ssn;
    
    private LocalDate dateOfBirth;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomerStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomerType type;
    
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Address> addresses = new HashSet<>();
    
    @Embedded
    private CreditScore creditScore;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @Version
    private Long version;
    
    public void addAddress(Address address) {
        addresses.add(address);
        address.setCustomer(this);
    }
    
    public void removeAddress(Address address) {
        addresses.remove(address);
        address.setCustomer(null);
    }
    
    public void updateCreditScore(int score, String reportingAgency) {
        this.creditScore = new CreditScore(score, reportingAgency, LocalDateTime.now());
        addDomainEvent(new CustomerCreditScoreUpdatedEvent(this.id.getValue(), score));
    }
    
    public void activate() {
        if (this.status != CustomerStatus.PENDING) {
            throw new IllegalStateException("Customer can only be activated from PENDING status");
        }
        this.status = CustomerStatus.ACTIVE;
        addDomainEvent(new CustomerActivatedEvent(this.id.getValue()));
    }
    
    public void suspend(String reason) {
        if (this.status != CustomerStatus.ACTIVE) {
            throw new IllegalStateException("Only active customers can be suspended");
        }
        this.status = CustomerStatus.SUSPENDED;
        addDomainEvent(new CustomerSuspendedEvent(this.id.getValue(), reason));
    }
    
    public void close(String reason) {
        this.status = CustomerStatus.CLOSED;
        addDomainEvent(new CustomerClosedEvent(this.id.getValue(), reason));
    }
    
    public boolean isEligibleForLoan() {
        return status == CustomerStatus.ACTIVE && 
               creditScore != null && 
               creditScore.getScore() >= 600;
    }
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    @Override
    public CustomerId getId() {
        return id;
    }
}