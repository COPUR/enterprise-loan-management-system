package com.bank.loanmanagement.domain.loan;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "loans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditLoan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long customerId;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal loanAmount;
    
    @Column(nullable = false)
    private Integer numberOfInstallments;
    
    @Column(nullable = false)
    private LocalDate createDate;
    
    @Column(nullable = false)
    private Boolean isPaid;
    
    @Column(nullable = false, precision = 5, scale = 3)
    private BigDecimal interestRate;
    
    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CreditLoanInstallment> installments = new ArrayList<>();
    
    public BigDecimal getTotalAmount() {
        return loanAmount.multiply(BigDecimal.ONE.add(interestRate));
    }
    
    public BigDecimal getInstallmentAmount() {
        return getTotalAmount().divide(BigDecimal.valueOf(numberOfInstallments), 2, BigDecimal.ROUND_HALF_UP);
    }
    
    public void generateInstallments() {
        installments.clear();
        BigDecimal installmentAmount = getInstallmentAmount();
        LocalDate dueDate = createDate.plusMonths(1).withDayOfMonth(1);
        
        for (int i = 1; i <= numberOfInstallments; i++) {
            CreditLoanInstallment installment = CreditLoanInstallment.builder()
                .loan(this)
                .amount(installmentAmount)
                .paidAmount(BigDecimal.ZERO)
                .dueDate(dueDate)
                .isPaid(false)
                .build();
            
            installments.add(installment);
            dueDate = dueDate.plusMonths(1);
        }
    }
    
    public void markAsPaid() {
        this.isPaid = true;
    }
    
    public boolean isFullyPaid() {
        return installments.stream().allMatch(CreditLoanInstallment::getIsPaid);
    }
    
    public List<CreditLoanInstallment> getUnpaidInstallments() {
        return installments.stream()
            .filter(installment -> !installment.getIsPaid())
            .sorted((i1, i2) -> i1.getDueDate().compareTo(i2.getDueDate()))
            .toList();
    }
    
    public List<CreditLoanInstallment> getPayableInstallments(LocalDate currentDate) {
        LocalDate threeMonthsFromNow = currentDate.plusMonths(3);
        return getUnpaidInstallments().stream()
            .filter(installment -> !installment.getDueDate().isAfter(threeMonthsFromNow))
            .toList();
    }
}