package com.bank.loanmanagement.infrastructure.persistence;

import com.bank.loanmanagement.domain.loan.*;
import com.bank.loanmanagement.customermanagement.domain.model.CustomerId;
import com.bank.loanmanagement.sharedkernel.domain.Money;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Entity for Loan Aggregate - Infrastructure Layer
 * 
 * Maps the clean Loan domain model to database persistence.
 * Contains only persistence concerns - no business logic.
 */
@Entity
@Table(name = "loans", indexes = {
    @Index(name = "idx_loan_customer", columnList = "customerId"),
    @Index(name = "idx_loan_status", columnList = "status"),
    @Index(name = "idx_loan_type", columnList = "loanType"),
    @Index(name = "idx_loan_application_date", columnList = "applicationDate")
})
public class LoanJpaEntity {
    
    @EmbeddedId
    private LoanId id;
    
    @Embedded
    private CustomerId customerId;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "principal_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "principal_currency"))
    })
    private Money principalAmount;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "outstanding_balance")),
        @AttributeOverride(name = "currency", column = @Column(name = "outstanding_currency"))
    })
    private Money outstandingBalance;
    
    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal interestRate;
    
    @Column(nullable = false)
    private Integer termInMonths;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanType loanType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;
    
    @Column(nullable = false)
    private LocalDate applicationDate;
    
    private LocalDate approvalDate;
    private LocalDate disbursementDate;
    private LocalDate maturityDate;
    
    // NOTE: Installments mapping to be added when LoanInstallmentJpaEntity is created
    // @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    // private List<LoanInstallmentJpaEntity> installments = new ArrayList<>();
    
    @Embedded
    private LoanTerms loanTerms;
    
    private String purpose;
    private String collateralDescription;
    private String approvedBy;
    private String notes;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @Version
    private Long version;

    // Default constructor for JPA
    protected LoanJpaEntity() {}

    // Constructor from domain model
    public LoanJpaEntity(Loan loan) {
        this.id = loan.getId();
        this.customerId = loan.getCustomerId();
        this.principalAmount = loan.getPrincipalAmount();
        this.outstandingBalance = loan.getOutstandingBalance();
        this.interestRate = loan.getInterestRate();
        this.termInMonths = loan.getTermInMonths();
        this.loanType = loan.getLoanType();
        this.status = loan.getStatus();
        this.applicationDate = loan.getApplicationDate();
        this.approvalDate = loan.getApprovalDate();
        this.disbursementDate = loan.getDisbursementDate();
        this.maturityDate = loan.getMaturityDate();
        this.loanTerms = loan.getLoanTerms();
        this.purpose = loan.getPurpose();
        this.collateralDescription = loan.getCollateralDescription();
        this.approvedBy = loan.getApprovedBy();
        this.notes = loan.getNotes();
        this.createdAt = loan.getCreatedAt();
        this.updatedAt = loan.getUpdatedAt();
        this.version = loan.getVersion();
        
        // NOTE: Installment conversion to be added when LoanInstallmentJpaEntity is created
    }

    // Convert to domain model
    public Loan toDomainModel() {
        // Create loan using factory method, then set reconstruction values
        Loan loan = Loan.create(
            this.id,
            this.customerId,
            this.principalAmount,
            this.interestRate,
            this.termInMonths,
            this.loanType,
            this.purpose != null ? this.purpose : "Reconstructed from persistence"
        );
        
        // Override with persisted values
        loan.setOutstandingBalance(this.outstandingBalance);
        loan.setStatus(this.status);
        loan.setApplicationDate(this.applicationDate);
        loan.setApprovalDate(this.approvalDate);
        loan.setDisbursementDate(this.disbursementDate);
        loan.setMaturityDate(this.maturityDate);
        loan.setLoanTerms(this.loanTerms);
        loan.setCollateralDescription(this.collateralDescription);
        loan.setApprovedBy(this.approvedBy);
        loan.setNotes(this.notes);
        loan.setCreatedAt(this.createdAt);
        loan.setUpdatedAt(this.updatedAt);
        loan.setVersion(this.version);
        
        // NOTE: Installment conversion to be added when LoanInstallmentJpaEntity is created
        
        return loan;
    }

    // Getters and setters for JPA
    public LoanId getId() { return id; }
    public void setId(LoanId id) { this.id = id; }
    
    public CustomerId getCustomerId() { return customerId; }
    public void setCustomerId(CustomerId customerId) { this.customerId = customerId; }
    
    public Money getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(Money principalAmount) { this.principalAmount = principalAmount; }
    
    public Money getOutstandingBalance() { return outstandingBalance; }
    public void setOutstandingBalance(Money outstandingBalance) { this.outstandingBalance = outstandingBalance; }
    
    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    
    public Integer getTermInMonths() { return termInMonths; }
    public void setTermInMonths(Integer termInMonths) { this.termInMonths = termInMonths; }
    
    public LoanType getLoanType() { return loanType; }
    public void setLoanType(LoanType loanType) { this.loanType = loanType; }
    
    public LoanStatus getStatus() { return status; }
    public void setStatus(LoanStatus status) { this.status = status; }
    
    public LocalDate getApplicationDate() { return applicationDate; }
    public void setApplicationDate(LocalDate applicationDate) { this.applicationDate = applicationDate; }
    
    public LocalDate getApprovalDate() { return approvalDate; }
    public void setApprovalDate(LocalDate approvalDate) { this.approvalDate = approvalDate; }
    
    public LocalDate getDisbursementDate() { return disbursementDate; }
    public void setDisbursementDate(LocalDate disbursementDate) { this.disbursementDate = disbursementDate; }
    
    public LocalDate getMaturityDate() { return maturityDate; }
    public void setMaturityDate(LocalDate maturityDate) { this.maturityDate = maturityDate; }
    
    // NOTE: Installment getters/setters to be added when LoanInstallmentJpaEntity is created
    
    public LoanTerms getLoanTerms() { return loanTerms; }
    public void setLoanTerms(LoanTerms loanTerms) { this.loanTerms = loanTerms; }
    
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    
    public String getCollateralDescription() { return collateralDescription; }
    public void setCollateralDescription(String collateralDescription) { this.collateralDescription = collateralDescription; }
    
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}