package com.loanmanagement.loan.application.service;

import com.loanmanagement.loan.domain.event.LoanPaidOffEvent;
import com.loanmanagement.loan.domain.event.LoanDefaultedEvent;
import com.loanmanagement.loan.domain.event.LoanRestructuredEvent;
import com.loanmanagement.loan.application.port.in.ManageLoanStatusUseCase;
import com.loanmanagement.loan.application.port.out.LoanEventPublisher;
import com.loanmanagement.loan.application.port.out.LoanRepository;
import com.loanmanagement.loan.domain.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application Service for Loan Status Management Operations
 * Implements the Manage Loan Status Use Case following hexagonal architecture principles
 */
@Service
@Transactional
@Slf4j
public class LoanStatusManagementApplicationService implements ManageLoanStatusUseCase {
    
    private final LoanRepository loanRepository;
    private final LoanEventPublisher eventPublisher;
    
    public LoanStatusManagementApplicationService(
            LoanRepository loanRepository,
            LoanEventPublisher eventPublisher) {
        this.loanRepository = loanRepository;
        this.eventPublisher = eventPublisher;
    }
    
    @Override
    public Loan completeLoan(CompleteLoanCommand command) {
        log.debug("Completing loan: {}", command.loanId());
        
        Loan loan = loanRepository.findById(command.loanId())
                .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + command.loanId()));
        
        // Domain logic for completion
        if (!loan.canBeCompleted()) {
            throw new IllegalStateException("Loan cannot be completed in current state: " + loan.getStatus());
        }
        
        loan.markAsCompleted();
        
        // Save updated loan
        Loan savedLoan = loanRepository.save(loan);
        log.info("Loan completed: {} by {}", command.loanId(), command.completedBy());
        
        // Publish domain event
        LoanPaidOffEvent event = LoanPaidOffEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .occurredOn(java.time.LocalDateTime.now())
                .loanId(savedLoan.getId())
                .customerId(savedLoan.getCustomerId())
                .finalPaymentAmount(savedLoan.getTotalAmount())
                .totalAmountPaid(savedLoan.getTotalPaid())
                .paidOffDate(command.completionDate())
                .originalAmount(savedLoan.getTotalAmount())
                .totalInterestPaid(savedLoan.calculateTotalInterestPaid())
                .build();
        
        eventPublisher.publishEvent(event);
        log.debug("Published LoanPaidOffEvent for loan: {}", savedLoan.getId());
        
        return savedLoan;
    }
    
    @Override
    public Loan markLoanAsDefaulted(MarkDefaultedCommand command) {
        log.debug("Marking loan as defaulted: {}", command.loanId());
        
        Loan loan = loanRepository.findById(command.loanId())
                .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + command.loanId()));
        
        // Domain logic for default
        DefaultReason defaultReason = DefaultReason.builder()
                .reason(command.defaultReason())
                .daysPastDue(30)
                .totalAmountPastDue(com.loanmanagement.shared.domain.Money.of("USD", command.outstandingAmount()))
                .missedPayments(3)
                .lastPaymentDate(java.time.LocalDate.now().minusDays(30))
                .collectionActions(java.util.List.of("Email notification", "Phone call"))
                .build();
                
        loan.markAsDefaulted(defaultReason, LoanOfficerId.of(command.defaultedBy()));
        
        // Save updated loan
        Loan savedLoan = loanRepository.save(loan);
        log.warn("Loan marked as defaulted: {} by {} - outstanding amount: {}", 
                command.loanId(), command.defaultedBy(), command.outstandingAmount());
        
        // Publish domain event
        LoanDefaultedEvent event = LoanDefaultedEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .occurredOn(java.time.LocalDateTime.now())
                .loanId(savedLoan.getId())
                .customerId(savedLoan.getCustomerId())
                .defaultReason(defaultReason.getReason())
                .daysPastDue(defaultReason.getDaysPastDue())
                .totalAmountPastDue(defaultReason.getTotalAmountPastDue())
                .missedPayments(defaultReason.getMissedPayments())
                .lastPaymentDate(defaultReason.getLastPaymentDate())
                .collectionActions(defaultReason.getCollectionActions())
                .defaultDate(command.defaultDate())
                .officerId(LoanOfficerId.of(command.defaultedBy()))
                .build();
        
        eventPublisher.publishEvent(event);
        log.debug("Published LoanDefaultedEvent for loan: {}", savedLoan.getId());
        
        return savedLoan;
    }
    
    @Override
    public Loan restructureLoan(RestructureLoanCommand command) {
        log.debug("Restructuring loan: {}", command.loanId());
        
        Loan loan = loanRepository.findById(command.loanId())
                .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + command.loanId()));
        
        // Domain logic for restructuring
        if (!loan.canBeRestructured()) {
            throw new IllegalStateException("Loan cannot be restructured in current state: " + loan.getStatus());
        }
        
        // Create new loan terms
        LoanTerms newTerms = loan.getCurrentTerms().toBuilder()
                .interestRate(command.newInterestRate())
                .termInMonths(command.newTermMonths())
                .build();
        
        // Create restructuring reason
        RestructuringReason restructuringReason = RestructuringReason.builder()
                .reason(command.restructureReason())
                .justification("Interest rate and term modification")
                .temporaryHardship(false)
                .expectedDuration(null)
                .build();
        
        loan.restructure(newTerms, restructuringReason, LoanOfficerId.of(command.restructuredBy()));
        
        // Save updated loan
        Loan savedLoan = loanRepository.save(loan);
        log.info("Loan restructured: {} by {} - new rate: {}, new term: {} months", 
                command.loanId(), command.restructuredBy(), command.newInterestRate(), command.newTermMonths());
        
        // Publish domain event
        LoanRestructuredEvent event = LoanRestructuredEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .occurredOn(java.time.LocalDateTime.now())
                .loanId(savedLoan.getId())
                .customerId(savedLoan.getCustomerId())
                .originalTerms(loan.getOriginalTerms())
                .newTerms(newTerms)
                .restructuringReason(restructuringReason.getReason())
                .justification(restructuringReason.getJustification())
                .temporaryHardship(restructuringReason.isTemporaryHardship())
                .expectedDuration(restructuringReason.getExpectedDuration())
                .restructureDate(command.restructureDate())
                .officerId(LoanOfficerId.of(command.restructuredBy()))
                .termsComparison(java.util.Map.of(
                        "rateChange", newTerms.getInterestRate().subtract(loan.getOriginalTerms().getInterestRate()),
                        "termChange", newTerms.getTermInMonths() - loan.getOriginalTerms().getTermInMonths()
                ))
                .build();
        
        eventPublisher.publishEvent(event);
        log.debug("Published LoanRestructuredEvent for loan: {}", savedLoan.getId());
        
        return savedLoan;
    }
}