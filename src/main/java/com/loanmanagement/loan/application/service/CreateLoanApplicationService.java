package com.loanmanagement.loan.application.service;

import com.loanmanagement.customer.application.port.out.CustomerRepository;
import com.loanmanagement.customer.domain.model.Customer;
import com.loanmanagement.loan.domain.event.LoanApplicationSubmittedEvent;
import com.loanmanagement.loan.application.port.in.CreateLoanUseCase;
import com.loanmanagement.loan.application.port.out.LoanEventPublisher;
import com.loanmanagement.loan.application.port.out.LoanRepository;
import com.loanmanagement.loan.domain.model.Loan;
import com.loanmanagement.loan.domain.service.LoanFacadeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application Service for Creating Loans
 * Implements the Create Loan Use Case following hexagonal architecture principles
 */
@Service
@Transactional
@Slf4j
public class CreateLoanApplicationService implements CreateLoanUseCase {
    
    private final LoanRepository loanRepository;
    private final CustomerRepository customerRepository;
    private final LoanEventPublisher eventPublisher;
    private final LoanFacadeService loanFacadeService;
    
    public CreateLoanApplicationService(
            LoanRepository loanRepository, 
            CustomerRepository customerRepository,
            LoanEventPublisher eventPublisher,
            LoanFacadeService loanFacadeService) {
        this.loanRepository = loanRepository;
        this.customerRepository = customerRepository;
        this.eventPublisher = eventPublisher;
        this.loanFacadeService = loanFacadeService;
    }
    
    @Override
    public Loan createLoan(CreateLoanCommand command) {
        log.debug("Creating loan application for customer: {}", command.customerId());
        
        // Validate customer exists and is active
        Customer customer = customerRepository.findById(command.customerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + command.customerId()));
        
        if (!customer.isActive()) {
            throw new IllegalArgumentException("Customer is not active");
        }
        
        // Validate loan eligibility
        if (!customer.isEligibleForLoan(command.principalAmount())) {
            throw new IllegalArgumentException("Customer is not eligible for requested loan amount");
        }
        
        // Convert Money types
        com.loanmanagement.shared.domain.Money convertedAmount = com.loanmanagement.shared.domain.Money.of(
                command.principalAmount().getCurrency(),
                command.principalAmount().getAmount()
        );
        
        // Create loan domain object using facade service
        Loan loan = loanFacadeService.createSimpleLoan(
                command.customerId(),
                convertedAmount,
                command.interestRate(),
                command.termMonths()
        );
        
        // Save loan
        Loan savedLoan = loanRepository.save(loan);
        log.info("Loan application created with ID: {}", savedLoan.getId());
        
        // Publish domain event
        LoanApplicationSubmittedEvent event = LoanApplicationSubmittedEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .occurredOn(java.time.LocalDateTime.now())
                .loanId(savedLoan.getId())
                .customerId(savedLoan.getCustomerId())
                .requestedAmount(savedLoan.getPrincipalAmount())
                .loanPurpose(savedLoan.getPurpose())
                .requestedTerms(savedLoan.getCurrentTerms())
                .loanOfficerId(savedLoan.getAssignedOfficerId())
                .applicationDate(savedLoan.getApplicationDate())
                .build();
        
        eventPublisher.publishEvent(event);
        log.debug("Published LoanApplicationSubmittedEvent for loan: {}", savedLoan.getId());
        
        return savedLoan;
    }
}