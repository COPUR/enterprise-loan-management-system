package com.bank.loanmanagement.application.service;

import com.bank.loanmanagement.domain.customer.CreditCustomer;
import com.bank.loanmanagement.domain.loan.CreditLoan;
import com.bank.loanmanagement.domain.loan.CreditLoanInstallment;
import com.bank.loanmanagement.infrastructure.repository.CreditCustomerRepository;
import com.bank.loanmanagement.infrastructure.repository.CreditLoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanService {
    
    private final CreditCustomerRepository customerRepository;
    private final CreditLoanRepository loanRepository;
    
    private static final List<Integer> ALLOWED_INSTALLMENTS = Arrays.asList(6, 9, 12, 24);
    private static final BigDecimal MIN_INTEREST_RATE = BigDecimal.valueOf(0.1);
    private static final BigDecimal MAX_INTEREST_RATE = BigDecimal.valueOf(0.5);
    
    @Transactional
    public CreditLoan createLoan(Long customerId, BigDecimal amount, BigDecimal interestRate, Integer numberOfInstallments) {
        // Validate business rules
        validateCreateLoanBusinessRules(customerId, amount, interestRate, numberOfInstallments);
        
        CreditCustomer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        
        // Check if customer has enough credit limit
        if (!customer.hasEnoughCreditFor(amount)) {
            throw new IllegalArgumentException("Customer does not have enough credit limit");
        }
        
        // Create loan
        CreditLoan loan = CreditLoan.builder()
            .customerId(customerId)
            .loanAmount(amount)
            .numberOfInstallments(numberOfInstallments)
            .interestRate(interestRate)
            .createDate(LocalDate.now())
            .isPaid(false)
            .build();
        
        // Generate installments
        loan.generateInstallments();
        
        // Allocate credit limit
        customer.allocateCredit(amount);
        
        // Save entities
        customerRepository.save(customer);
        return loanRepository.save(loan);
    }
    
    public List<CreditLoan> listLoans(Long customerId, Integer numberOfInstallments, Boolean isPaid) {
        if (customerId != null) {
            if (numberOfInstallments != null && isPaid != null) {
                return loanRepository.findByCustomerIdAndNumberOfInstallmentsAndIsPaid(customerId, numberOfInstallments, isPaid);
            } else if (numberOfInstallments != null) {
                return loanRepository.findByCustomerIdAndNumberOfInstallments(customerId, numberOfInstallments);
            } else if (isPaid != null) {
                return loanRepository.findByCustomerIdAndIsPaid(customerId, isPaid);
            } else {
                return loanRepository.findByCustomerId(customerId);
            }
        }
        return loanRepository.findAll();
    }
    
    public List<CreditLoanInstallment> listInstallments(Long loanId) {
        CreditLoan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + loanId));
        return loan.getInstallments();
    }
    
    @Transactional
    public PaymentResult payLoan(Long loanId, BigDecimal paymentAmount) {
        CreditLoan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + loanId));
        
        CreditCustomer customer = customerRepository.findById(loan.getCustomerId())
            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + loan.getCustomerId()));
        
        LocalDate paymentDate = LocalDate.now();
        List<CreditLoanInstallment> payableInstallments = loan.getPayableInstallments(paymentDate);
        
        if (payableInstallments.isEmpty()) {
            throw new IllegalArgumentException("No installments available for payment within 3 months");
        }
        
        BigDecimal remainingAmount = paymentAmount;
        int installmentsPaid = 0;
        BigDecimal totalAmountSpent = BigDecimal.ZERO;
        
        for (CreditLoanInstallment installment : payableInstallments) {
            BigDecimal effectiveAmount = installment.getEffectiveAmount(paymentDate);
            
            if (remainingAmount.compareTo(effectiveAmount) >= 0) {
                // Can pay this installment fully
                installment.markAsPaid(effectiveAmount, paymentDate);
                remainingAmount = remainingAmount.subtract(effectiveAmount);
                totalAmountSpent = totalAmountSpent.add(effectiveAmount);
                installmentsPaid++;
                
                // Release credit for paid installment - release the original loan amount proportionally
                BigDecimal loanAmountPerInstallment = loan.getLoanAmount().divide(BigDecimal.valueOf(loan.getNumberOfInstallments()), 2, BigDecimal.ROUND_HALF_UP);
                customer.releaseCredit(loanAmountPerInstallment);
            } else {
                // Cannot pay this installment fully, stop here
                break;
            }
        }
        
        // Check if loan is fully paid
        boolean isLoanFullyPaid = loan.isFullyPaid();
        if (isLoanFullyPaid) {
            loan.markAsPaid();
        }
        
        // Save entities
        customerRepository.save(customer);
        loanRepository.save(loan);
        
        return PaymentResult.builder()
            .installmentsPaid(installmentsPaid)
            .totalAmountSpent(totalAmountSpent)
            .isLoanFullyPaid(isLoanFullyPaid)
            .build();
    }
    
    private void validateCreateLoanBusinessRules(Long customerId, BigDecimal amount, BigDecimal interestRate, Integer numberOfInstallments) {
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID is required");
        }
        
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Loan amount must be positive");
        }
        
        if (interestRate == null || interestRate.compareTo(MIN_INTEREST_RATE) < 0 || interestRate.compareTo(MAX_INTEREST_RATE) > 0) {
            throw new IllegalArgumentException("Interest rate must be between 0.1 and 0.5");
        }
        
        if (numberOfInstallments == null || !ALLOWED_INSTALLMENTS.contains(numberOfInstallments)) {
            throw new IllegalArgumentException("Number of installments can only be 6, 9, 12, or 24");
        }
    }
    
    public static class PaymentResult {
        private final int installmentsPaid;
        private final BigDecimal totalAmountSpent;
        private final boolean isLoanFullyPaid;
        
        @lombok.Builder
        public PaymentResult(int installmentsPaid, BigDecimal totalAmountSpent, boolean isLoanFullyPaid) {
            this.installmentsPaid = installmentsPaid;
            this.totalAmountSpent = totalAmountSpent;
            this.isLoanFullyPaid = isLoanFullyPaid;
        }
        
        public int getInstallmentsPaid() { return installmentsPaid; }
        public BigDecimal getTotalAmountSpent() { return totalAmountSpent; }
        public boolean isLoanFullyPaid() { return isLoanFullyPaid; }
    }
}