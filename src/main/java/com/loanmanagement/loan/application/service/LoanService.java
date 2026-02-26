package com.loanmanagement.loan.application.service;

import com.loanmanagement.customer.application.port.out.CustomerRepository;
import com.loanmanagement.customer.domain.model.Customer;
import com.loanmanagement.loan.application.port.in.CreateLoanUseCase;
import com.loanmanagement.loan.application.port.in.LoanManagementUseCase;
import com.loanmanagement.loan.application.port.out.LoanRepository;
import com.loanmanagement.loan.domain.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Deprecated // Use the new specialized services instead
public class LoanService implements CreateLoanUseCase, LoanManagementUseCase {
    
    private final LoanRepository loanRepository;
    private final CustomerRepository customerRepository;
    
    public LoanService(LoanRepository loanRepository, CustomerRepository customerRepository) {
        this.loanRepository = loanRepository;
        this.customerRepository = customerRepository;
    }
    
    @Override
    public Loan createLoan(CreateLoanCommand command) {
        Customer customer = customerRepository.findById(command.customerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + command.customerId()));
        
        if (!customer.isActive()) {
            throw new IllegalArgumentException("Customer is not active");
        }
        
        if (!customer.isEligibleForLoan(command.principalAmount())) {
            throw new IllegalArgumentException("Customer is not eligible for requested loan amount");
        }
        
        // Convert to proper domain types
        LoanId loanId = LoanId.generate();
        CustomerId customerId = CustomerId.of(command.customerId().toString());
        LoanOfficerId officerId = LoanOfficerId.generate();
        
        // Convert Money types
        com.loanmanagement.shared.domain.Money convertedAmount = com.loanmanagement.shared.domain.Money.of(
                command.principalAmount().getCurrency(),
                command.principalAmount().getAmount()
        );
        
        // Create loan terms
        LoanTerms terms = LoanTerms.builder()
                .interestRate(command.interestRate())
                .termInMonths(command.termMonths())
                .paymentFrequency(PaymentFrequency.MONTHLY)
                .build();
        
        Loan loan = Loan.createApplication(
                loanId,
                customerId,
                convertedAmount,
                LoanPurpose.PERSONAL,
                terms,
                officerId
        );
        
        return loanRepository.save(loan);
    }
    
    @Override
    public Loan approveLoan(Long loanId) {
        Loan loan = findLoanById(loanId);
        
        // Create approval conditions
        ApprovalConditions conditions = ApprovalConditions.builder()
                .approvedAmount(loan.getPrincipalAmount())
                .approvedTerms(loan.getOriginalTerms())
                .expirationDate(java.time.LocalDate.now().plusDays(30))
                .conditions(java.util.List.of("Standard approval"))
                .build();
        
        LoanOfficerId officerId = LoanOfficerId.generate();
        loan.approve(conditions, officerId);
        return loanRepository.save(loan);
    }
    
    @Override
    public Loan rejectLoan(Long loanId, String reason) {
        Loan loan = findLoanById(loanId);
        
        // Create rejection reason
        RejectionReason rejectionReason = RejectionReason.builder()
                .primaryReason(reason)
                .details(java.util.List.of(reason))
                .appealable(true)
                .appealDeadline(java.time.LocalDate.now().plusDays(30))
                .build();
        
        LoanOfficerId officerId = LoanOfficerId.generate();
        loan.reject(rejectionReason, officerId);
        return loanRepository.save(loan);
    }
    
    @Override
    public Loan disburseLoan(Long loanId) {
        Loan loan = findLoanById(loanId);
        
        // Create disbursement instructions
        DisbursementInstructions instructions = DisbursementInstructions.builder()
                .accountNumber("DEFAULT-ACCOUNT")
                .routingNumber("DEFAULT-ROUTING")
                .disbursementMethod(DisbursementMethod.ACH_TRANSFER)
                .disbursementDate(java.time.LocalDate.now())
                .specialInstructions("Automated disbursement")
                .build();
        
        LoanOfficerId officerId = LoanOfficerId.generate();
        loan.disburse(instructions, officerId);
        return loanRepository.save(loan);
    }
    
    @Override
    public Loan completeLoan(Long loanId) {
        Loan loan = findLoanById(loanId);
        loan.markAsCompleted();
        return loanRepository.save(loan);
    }
    
    @Override
    public Loan markAsDefaulted(Long loanId) {
        Loan loan = findLoanById(loanId);
        
        // Create default reason
        DefaultReason defaultReason = DefaultReason.builder()
                .reason("Loan marked as defaulted")
                .daysPastDue(30)
                .totalAmountPastDue(loan.getRemainingBalance())
                .missedPayments(3)
                .lastPaymentDate(java.time.LocalDate.now().minusDays(30))
                .collectionActions(java.util.List.of("Email notification"))
                .build();
        
        LoanOfficerId officerId = LoanOfficerId.generate();
        loan.markAsDefaulted(defaultReason, officerId);
        return loanRepository.save(loan);
    }
    
    private Loan findLoanById(Long loanId) {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + loanId));
    }
}