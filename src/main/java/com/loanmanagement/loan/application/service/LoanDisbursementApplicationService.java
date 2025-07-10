package com.loanmanagement.loan.application.service;

import com.loanmanagement.loan.domain.event.LoanDisbursedEvent;
import com.loanmanagement.loan.application.port.in.DisburseLoanUseCase;
import com.loanmanagement.loan.application.port.out.LoanEventPublisher;
import com.loanmanagement.loan.application.port.out.LoanRepository;
import com.loanmanagement.loan.domain.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application Service for Loan Disbursement Operations
 * Implements the Disburse Loan Use Case following hexagonal architecture principles
 */
@Service
@Transactional
@Slf4j
public class LoanDisbursementApplicationService implements DisburseLoanUseCase {
    
    private final LoanRepository loanRepository;
    private final LoanEventPublisher eventPublisher;
    
    public LoanDisbursementApplicationService(
            LoanRepository loanRepository,
            LoanEventPublisher eventPublisher) {
        this.loanRepository = loanRepository;
        this.eventPublisher = eventPublisher;
    }
    
    @Override
    public Loan disburseLoan(DisburseLoanCommand command) {
        log.debug("Disbursing loan: {}", command.loanId());
        
        Loan loan = loanRepository.findById(command.loanId())
                .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + command.loanId()));
        
        // Domain logic for disbursement
        if (!loan.canBeActivated()) {
            throw new IllegalStateException("Loan cannot be disbursed in current state: " + loan.getStatus());
        }
        
        // Create disbursement instructions
        DisbursementInstructions instructions = DisbursementInstructions.builder()
                .accountNumber(command.bankAccountNumber())
                .routingNumber("DEFAULT-ROUTING")
                .disbursementMethod(DisbursementMethod.valueOf(command.disbursementMethod()))
                .disbursementDate(command.disbursementDate().toLocalDate())
                .specialInstructions(command.disbursementNotes())
                .build();
        
        LoanOfficerId officerId = LoanOfficerId.of(command.disbursedBy());
        
        loan.disburse(instructions, officerId);
        
        // Save updated loan
        Loan savedLoan = loanRepository.save(loan);
        log.info("Loan disbursed: {} by {} to account: {}", 
                command.loanId(), command.disbursedBy(), command.bankAccountNumber());
        
        // Publish domain event
        LoanDisbursedEvent event = LoanDisbursedEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .occurredOn(java.time.LocalDateTime.now())
                .loanId(savedLoan.getId())
                .customerId(savedLoan.getCustomerId())
                .disbursedAmount(savedLoan.getPrincipalAmount())
                .disbursementMethod(instructions.getDisbursementMethod())
                .accountNumber(instructions.getAccountNumber())
                .routingNumber(instructions.getRoutingNumber())
                .disbursementDate(java.time.LocalDateTime.now())
                .disbursedBy(officerId)
                .specialInstructions(instructions.getSpecialInstructions())
                .build();
        
        eventPublisher.publishEvent(event);
        log.debug("Published LoanDisbursedEvent for loan: {}", savedLoan.getId());
        
        return savedLoan;
    }
}