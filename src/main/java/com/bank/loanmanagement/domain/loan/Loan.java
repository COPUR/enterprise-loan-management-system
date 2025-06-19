package com.bank.loanmanagement.domain.loan;

import com.bank.loanmanagement.domain.customer.CustomerId;
import com.bank.loanmanagement.domain.shared.AggregateRoot;
import com.bank.loanmanagement.domain.shared.Money;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "loans", indexes = {
    @Index(name = "idx_loan_customer", columnList = "customerId"),
    @Index(name = "idx_loan_status", columnList = "status"),
    @Index(name = "idx_loan_type", columnList = "loanType"),
    @Index(name = "idx_loan_application_date", columnList = "applicationDate")
})
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan extends AggregateRoot<LoanId> {
    
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
    
    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<LoanInstallment> installments = new ArrayList<>();
    
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
    
    public void approve(String approvedBy) {
        if (this.status != LoanStatus.PENDING) {
            throw new IllegalStateException("Only pending loans can be approved");
        }
        this.status = LoanStatus.APPROVED;
        this.approvalDate = LocalDate.now();
        this.approvedBy = approvedBy;
        generateInstallments();
        addDomainEvent(new LoanApprovedEvent(this.id.getValue(), this.customerId.getValue()));
    }
    
    public void reject(String reason) {
        if (this.status != LoanStatus.PENDING) {
            throw new IllegalStateException("Only pending loans can be rejected");
        }
        this.status = LoanStatus.REJECTED;
        this.notes = reason;
        addDomainEvent(new LoanRejectedEvent(this.id.getValue(), this.customerId.getValue(), reason));
    }
    
    public void disburse() {
        if (this.status != LoanStatus.APPROVED) {
            throw new IllegalStateException("Only approved loans can be disbursed");
        }
        this.status = LoanStatus.ACTIVE;
        this.disbursementDate = LocalDate.now();
        this.maturityDate = disbursementDate.plusMonths(termInMonths);
        this.outstandingBalance = this.principalAmount;
        addDomainEvent(new LoanDisbursedEvent(this.id.getValue(), this.customerId.getValue(), this.principalAmount));
    }
    
    public void makePayment(Money paymentAmount, LocalDate paymentDate) {
        if (this.status != LoanStatus.ACTIVE) {
            throw new IllegalStateException("Payments can only be made on active loans");
        }
        
        if (paymentAmount.isGreaterThan(this.outstandingBalance)) {
            throw new IllegalArgumentException("Payment amount cannot exceed outstanding balance");
        }
        
        this.outstandingBalance = this.outstandingBalance.subtract(paymentAmount);
        
        if (this.outstandingBalance.isZero()) {
            this.status = LoanStatus.PAID_OFF;
            addDomainEvent(new LoanPaidOffEvent(this.id.getValue(), this.customerId.getValue()));
        }
        
        addDomainEvent(new LoanPaymentMadeEvent(this.id.getValue(), this.customerId.getValue(), paymentAmount, paymentDate));
    }
    
    public void markAsDefault() {
        if (this.status != LoanStatus.ACTIVE) {
            throw new IllegalStateException("Only active loans can be marked as default");
        }
        this.status = LoanStatus.DEFAULTED;
        addDomainEvent(new LoanDefaultedEvent(this.id.getValue(), this.customerId.getValue()));
    }
    
    private void generateInstallments() {
        installments.clear();
        Money monthlyPayment = calculateMonthlyPayment();
        LocalDate installmentDate = approvalDate.plusMonths(1);
        
        for (int i = 1; i <= termInMonths; i++) {
            LoanInstallment installment = LoanInstallment.builder()
                .installmentNumber(i)
                .dueDate(installmentDate)
                .principalAmount(calculatePrincipalForInstallment(i))
                .interestAmount(calculateInterestForInstallment(i))
                .totalAmount(monthlyPayment)
                .status(InstallmentStatus.PENDING)
                .loan(this)
                .build();
            
            installments.add(installment);
            installmentDate = installmentDate.plusMonths(1);
        }
    }
    
    private Money calculateMonthlyPayment() {
        double monthlyRate = interestRate.doubleValue() / 12;
        double numerator = monthlyRate * Math.pow(1 + monthlyRate, termInMonths);
        double denominator = Math.pow(1 + monthlyRate, termInMonths) - 1;
        double monthlyPayment = principalAmount.getAmount().doubleValue() * (numerator / denominator);
        
        return Money.of(BigDecimal.valueOf(monthlyPayment), principalAmount.getCurrency());
    }
    
    private Money calculatePrincipalForInstallment(int installmentNumber) {
        // Simplified calculation - in real implementation would use amortization schedule
        Money monthlyPayment = calculateMonthlyPayment();
        Money interestAmount = calculateInterestForInstallment(installmentNumber);
        return monthlyPayment.subtract(interestAmount);
    }
    
    private Money calculateInterestForInstallment(int installmentNumber) {
        // Simplified calculation - in real implementation would calculate based on remaining balance
        BigDecimal monthlyRate = interestRate.divide(BigDecimal.valueOf(12), 6, BigDecimal.ROUND_HALF_UP);
        return outstandingBalance.multiply(monthlyRate);
    }
    
    public boolean isOverdue() {
        return installments.stream()
            .anyMatch(installment -> installment.getDueDate().isBefore(LocalDate.now()) && 
                                   installment.getStatus() == InstallmentStatus.PENDING);
    }
    
    public Money getTotalPaid() {
        return installments.stream()
            .filter(installment -> installment.getStatus() == InstallmentStatus.PAID)
            .map(LoanInstallment::getTotalAmount)
            .reduce(Money.zero(principalAmount.getCurrency()), Money::add);
    }
    
    @Override
    public LoanId getId() {
        return id;
    }
}