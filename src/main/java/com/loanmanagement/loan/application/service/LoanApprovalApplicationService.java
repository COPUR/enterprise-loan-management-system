package com.loanmanagement.loan.application.service;

import com.loanmanagement.loan.domain.event.LoanApprovedEvent;
import com.loanmanagement.loan.domain.event.LoanRejectedEvent;
import com.loanmanagement.loan.application.port.in.ApproveLoanUseCase;
import com.loanmanagement.loan.application.port.out.LoanEventPublisher;
import com.loanmanagement.loan.application.port.out.LoanRepository;
import com.loanmanagement.loan.domain.model.Loan;
import com.loanmanagement.loan.domain.model.LoanOfficerId;
import com.loanmanagement.loan.domain.service.LoanFacadeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application Service for Loan Approval Operations
 * Implements the Approve Loan Use Case following hexagonal architecture principles
 */
@Service
@Transactional
@Slf4j
public class LoanApprovalApplicationService implements ApproveLoanUseCase {
    
    private final LoanRepository loanRepository;
    private final LoanEventPublisher eventPublisher;
    private final LoanFacadeService loanFacadeService;
    
    public LoanApprovalApplicationService(
            LoanRepository loanRepository,
            LoanEventPublisher eventPublisher,
            LoanFacadeService loanFacadeService) {
        this.loanRepository = loanRepository;
        this.eventPublisher = eventPublisher;
        this.loanFacadeService = loanFacadeService;
    }
    
    @Override
    public Loan approveLoan(ApproveLoanCommand command) {
        log.debug("Approving loan: {}", command.loanId());
        
        Loan loan = loanRepository.findById(command.loanId())
                .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + command.loanId()));
        
        // Domain logic for approval using facade service
        loanFacadeService.approveLoan(loan);
        
        // Save updated loan
        Loan savedLoan = loanRepository.save(loan);
        log.info("Loan approved: {} by {}", command.loanId(), command.approvedBy());
        
        // Publish domain event
        LoanApprovedEvent event = LoanApprovedEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .occurredOn(java.time.LocalDateTime.now())
                .loanId(savedLoan.getId())
                .customerId(savedLoan.getCustomerId())
                .approvedAmount(savedLoan.getPrincipalAmount())
                .approvedTerms(savedLoan.getOriginalTerms())
                .conditions(java.util.List.of(command.approvalNotes()))
                .approvalDate(command.approvalDate())
                .approvingOfficerId(LoanOfficerId.of(command.approvedBy()))
                .approvalExpirationDate(command.approvalDate().toLocalDate().plusDays(30))
                .termsModified(false)
                .build();
        
        eventPublisher.publishEvent(event);
        log.debug("Published LoanApprovedEvent for loan: {}", savedLoan.getId());
        
        return savedLoan;
    }
    
    @Override
    public Loan rejectLoan(RejectLoanCommand command) {
        log.debug("Rejecting loan: {}", command.loanId());
        
        Loan loan = loanRepository.findById(command.loanId())
                .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + command.loanId()));
        
        // Domain logic for rejection using facade service
        loanFacadeService.rejectLoan(loan, command.reason());
        
        // Save updated loan
        Loan savedLoan = loanRepository.save(loan);
        log.info("Loan rejected: {} by {} - reason: {}", command.loanId(), command.rejectedBy(), command.reason());
        
        // Publish domain event
        LoanRejectedEvent event = LoanRejectedEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .occurredOn(java.time.LocalDateTime.now())
                .loanId(savedLoan.getId())
                .customerId(savedLoan.getCustomerId())
                .primaryReason(command.reason())
                .rejectionDetails(java.util.List.of(command.reason()))
                .appealable(true)
                .appealDeadline(command.rejectionDate().toLocalDate().plusDays(30))
                .rejectionDate(command.rejectionDate())
                .rejectingOfficerId(LoanOfficerId.of(command.rejectedBy()))
                .build();
        
        eventPublisher.publishEvent(event);
        log.debug("Published LoanRejectedEvent for loan: {}", savedLoan.getId());
        
        return savedLoan;
    }
}