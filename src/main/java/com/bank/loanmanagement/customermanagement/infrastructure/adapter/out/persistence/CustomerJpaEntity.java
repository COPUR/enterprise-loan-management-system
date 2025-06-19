package com.bank.loanmanagement.customermanagement.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;

/**
 * JPA entity for Customer persistence.
 * Separate from domain model to avoid infrastructure contamination.
 */
@Entity
@Table(
    name = "customers",
    indexes = {
        @Index(name = "idx_customer_email", columnList = "email"),
        @Index(name = "idx_customer_phone", columnList = "phone_number"),
        @Index(name = "idx_customer_status", columnList = "status"),
        @Index(name = "idx_customer_created_at", columnList = "created_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_customer_email", columnNames = "email")
    }
)
public class CustomerJpaEntity {
    
    @Id
    @Column(name = "id", length = 50)
    private String id;
    
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;
    
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;
    
    @Column(name = "email", nullable = false, unique = true, length = 254)
    private String email;
    
    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;
    
    @Column(name = "credit_limit_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal creditLimitAmount;
    
    @Column(name = "credit_limit_currency", nullable = false, length = 3)
    private String creditLimitCurrency;
    
    @Column(name = "used_credit_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal usedCreditAmount;
    
    @Column(name = "used_credit_currency", nullable = false, length = 3)
    private String usedCreditCurrency;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CustomerStatusJpa status;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Version
    @Column(name = "version")
    private Long version;
    
    // Default constructor for JPA
    protected CustomerJpaEntity() {}
    
    // Constructor for creation
    public CustomerJpaEntity(
        String id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        BigDecimal creditLimitAmount,
        String creditLimitCurrency,
        BigDecimal usedCreditAmount,
        String usedCreditCurrency,
        CustomerStatusJpa status
    ) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.creditLimitAmount = creditLimitAmount;
        this.creditLimitCurrency = creditLimitCurrency;
        this.usedCreditAmount = usedCreditAmount;
        this.usedCreditCurrency = usedCreditCurrency;
        this.status = status;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public BigDecimal getCreditLimitAmount() {
        return creditLimitAmount;
    }
    
    public void setCreditLimitAmount(BigDecimal creditLimitAmount) {
        this.creditLimitAmount = creditLimitAmount;
    }
    
    public String getCreditLimitCurrency() {
        return creditLimitCurrency;
    }
    
    public void setCreditLimitCurrency(String creditLimitCurrency) {
        this.creditLimitCurrency = creditLimitCurrency;
    }
    
    public BigDecimal getUsedCreditAmount() {
        return usedCreditAmount;
    }
    
    public void setUsedCreditAmount(BigDecimal usedCreditAmount) {
        this.usedCreditAmount = usedCreditAmount;
    }
    
    public String getUsedCreditCurrency() {
        return usedCreditCurrency;
    }
    
    public void setUsedCreditCurrency(String usedCreditCurrency) {
        this.usedCreditCurrency = usedCreditCurrency;
    }
    
    public CustomerStatusJpa getStatus() {
        return status;
    }
    
    public void setStatus(CustomerStatusJpa status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Long getVersion() {
        return version;
    }
    
    public void setVersion(Long version) {
        this.version = version;
    }
}