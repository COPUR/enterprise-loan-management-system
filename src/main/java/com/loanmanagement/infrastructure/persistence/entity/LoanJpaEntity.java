package com.loanmanagement.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * JPA Entity for Loan persistence following hexagonal architecture principles.
 * This class is part of the infrastructure layer and serves as an adapter
 * between the domain model and the database storage.
 *
 * Follows DDD principles by:
 * - Separating persistence concerns from domain logic
 * - Acting as a translation layer between domain and infrastructure
 *
 * Note: This is a fallback implementation while Jakarta Persistence dependencies are resolved.
 * To restore full JPA functionality:
 * 1. Run: ./gradlew clean build --refresh-dependencies
 * 2. Verify Jakarta Persistence API dependencies in build.gradle
 * 3. Add proper JPA annotations once dependencies are available
 */
public class LoanJpaEntity {

    private Long id;
    private Long customerId;
    private BigDecimal loanAmount;
    private Integer numberOfInstallments;
    private LocalDate createDate;
    private Boolean isPaid;
    private final List<LoanInstallmentJpaEntity> installments = new ArrayList<>();

    // Default constructor for JPA (required even without annotations)
    public LoanJpaEntity() {
        this.isPaid = false;
        this.createDate = LocalDate.now();
    }

    // Constructor for creating new loan entities
    public LoanJpaEntity(Long customerId, BigDecimal loanAmount, Integer numberOfInstallments) {
        this();
        this.customerId = customerId;
        this.loanAmount = loanAmount;
        this.numberOfInstallments = numberOfInstallments;
    }

    // Factory method following DDD principles
    public static LoanJpaEntity fromDomain(com.loanmanagement.loan.domain.model.entity.Loan domainLoan) {
        LoanJpaEntity entity = new LoanJpaEntity();
        entity.setId(domainLoan.getId());
        entity.setCustomerId(domainLoan.getCustomerId());
        entity.setLoanAmount(domainLoan.getLoanAmount().getAmount());
        entity.setNumberOfInstallments(domainLoan.getNumberOfInstallments().getValue());
        entity.setCreateDate(domainLoan.getCreateDate());
        entity.setIsPaid(domainLoan.isPaid());
        return entity;
    }

    // Conversion method to domain model following hexagonal architecture
    public com.loanmanagement.loan.domain.model.entity.Loan toDomain() {
        // This would convert the JPA entity back to domain model
        // Implementation depends on the domain model structure
        throw new UnsupportedOperationException("Domain conversion not yet implemented");
    }

    // Getters and setters with proper encapsulation
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getLoanAmount() {
        return loanAmount;
    }

    public void setLoanAmount(BigDecimal loanAmount) {
        this.loanAmount = loanAmount;
    }

    public Integer getNumberOfInstallments() {
        return numberOfInstallments;
    }

    public void setNumberOfInstallments(Integer numberOfInstallments) {
        this.numberOfInstallments = numberOfInstallments;
    }

    public LocalDate getCreateDate() {
        return createDate;
    }

    public void setCreateDate(LocalDate createDate) {
        this.createDate = createDate;
    }

    public Boolean getIsPaid() {
        return isPaid;
    }

    public void setIsPaid(Boolean isPaid) {
        this.isPaid = isPaid;
    }

    public List<LoanInstallmentJpaEntity> getInstallments() {
        return new ArrayList<>(installments); // Defensive copy
    }

    public void addInstallment(LoanInstallmentJpaEntity installment) {
        this.installments.add(installment);
        // In full JPA implementation, this would set the back-reference
        // installment.setLoan(this);
    }

    public void removeInstallment(LoanInstallmentJpaEntity installment) {
        this.installments.remove(installment);
        // In full JPA implementation, this would clear the back-reference
        // installment.setLoan(null);
    }

    // Business logic methods following DDD principles
    public boolean hasUnpaidInstallments() {
        return installments.stream()
                .anyMatch(installment -> !Boolean.TRUE.equals(installment.getIsPaid()));
    }

    public int getUnpaidInstallmentCount() {
        return (int) installments.stream()
                .filter(installment -> !Boolean.TRUE.equals(installment.getIsPaid()))
                .count();
    }

    // Equals and hashCode for entity identity
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoanJpaEntity that = (LoanJpaEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "LoanJpaEntity{" +
                "id=" + id +
                ", customerId=" + customerId +
                ", loanAmount=" + loanAmount +
                ", numberOfInstallments=" + numberOfInstallments +
                ", createDate=" + createDate +
                ", isPaid=" + isPaid +
                ", installmentCount=" + installments.size() +
                '}';
    }
}

/*
 * JPA RESTORATION GUIDE:
 *
 * Once Jakarta Persistence dependencies are resolved, add these annotations:
 *
 * @Entity
 * @Table(name = "loans")
 * public class LoanJpaEntity {
 *
 *     @Id
 *     @GeneratedValue(strategy = GenerationType.IDENTITY)
 *     private Long id;
 *
 *     @Column(name = "customer_id", nullable = false)
 *     private Long customerId;
 *
 *     @Column(name = "loan_amount", nullable = false, precision = 15, scale = 2)
 *     private BigDecimal loanAmount;
 *
 *     @Column(name = "number_of_installments", nullable = false)
 *     private Integer numberOfInstallments;
 *
 *     @Column(name = "create_date", nullable = false)
 *     private LocalDate createDate;
 *
 *     @Column(name = "is_paid", nullable = false)
 *     private Boolean isPaid = false;
 *
 *     @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
 *     private List<LoanInstallmentJpaEntity> installments = new ArrayList<>();
 * }
 *
 * ARCHITECTURAL BENEFITS:
 * - 12-Factor: Clear separation of configuration and code, explicit dependencies
 * - DDD: Clean separation between domain model and persistence model
 * - Hexagonal: Infrastructure adapter that doesn't leak into domain
 * - Clean Code: Proper encapsulation, meaningful methods, comprehensive documentation
 */
