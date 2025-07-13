package com.bank.customer.infrastructure.persistence;

import com.bank.customer.domain.Customer;
import com.bank.customer.domain.CreditProfile;
import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;

/**
 * JPA Entity for Customer persistence
 * 
 * Separates domain model from persistence concerns (Hexagonal Architecture)
 * Maps between domain objects and database representation
 */
@Entity
@Table(name = "customers")
public class CustomerJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "customer_id", unique = true, nullable = false)
    private String customerId;
    
    @Column(name = "first_name", nullable = false)
    private String firstName;
    
    @Column(name = "last_name", nullable = false)
    private String lastName;
    
    @Column(name = "email", unique = true, nullable = false)
    private String email;
    
    @Column(name = "phone_number")
    private String phoneNumber;
    
    @Column(name = "credit_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal creditLimit;
    
    @Column(name = "used_credit", nullable = false, precision = 19, scale = 2)
    private BigDecimal usedCredit;
    
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
    
    @Column(name = "status", nullable = false)
    private String status;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Version
    private Long version;
    
    // JPA requires default constructor
    protected CustomerJpaEntity() {}
    
    /**
     * Create JPA entity from domain model
     */
    public static CustomerJpaEntity fromDomain(Customer customer) {
        CustomerJpaEntity entity = new CustomerJpaEntity();
        entity.customerId = customer.getId().getValue();
        entity.firstName = customer.getFirstName();
        entity.lastName = customer.getLastName();
        entity.email = customer.getEmail();
        entity.phoneNumber = customer.getPhoneNumber();
        entity.creditLimit = customer.getCreditProfile().getCreditLimit().getAmount();
        entity.usedCredit = customer.getCreditProfile().getUsedCredit().getAmount();
        entity.currency = customer.getCreditProfile().getCreditLimit().getCurrency().getCurrencyCode();
        entity.status = "ACTIVE"; // Simplified for now
        entity.createdAt = customer.getCreatedAt();
        entity.updatedAt = customer.getUpdatedAt();
        entity.version = customer.getVersion();
        return entity;
    }
    
    /**
     * Convert to domain model
     */
    public Customer toDomain() {
        Money creditLimitMoney = Money.of(creditLimit, Currency.getInstance(currency));
        Money usedCreditMoney = Money.of(usedCredit, Currency.getInstance(currency));
        CreditProfile creditProfile = CreditProfile.create(creditLimitMoney, usedCreditMoney);
        
        Customer customer = Customer.create(
            CustomerId.of(customerId),
            firstName,
            lastName,
            email,
            phoneNumber,
            creditLimitMoney
        );
        
        customer.setVersion(version);
        return customer;
    }
    
    /**
     * Update entity from domain model (for updates)
     */
    public void updateFromDomain(Customer customer) {
        this.firstName = customer.getFirstName();
        this.lastName = customer.getLastName();
        this.email = customer.getEmail();
        this.phoneNumber = customer.getPhoneNumber();
        this.creditLimit = customer.getCreditProfile().getCreditLimit().getAmount();
        this.usedCredit = customer.getCreditProfile().getUsedCredit().getAmount();
        this.currency = customer.getCreditProfile().getCreditLimit().getCurrency().getCurrencyCode();
        this.updatedAt = customer.getUpdatedAt();
        this.version = customer.getVersion();
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public String getCustomerId() { return customerId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public BigDecimal getCreditLimit() { return creditLimit; }
    public BigDecimal getUsedCredit() { return usedCredit; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}