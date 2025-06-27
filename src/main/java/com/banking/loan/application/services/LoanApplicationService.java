package com.banking.loan.application.services;

import com.banking.loan.application.ports.in.LoanApplicationUseCase;
import com.banking.loan.application.ports.out.LoanRepository;
import com.banking.loan.application.ports.out.CustomerRepository;
import com.banking.loan.application.ports.out.AIRiskAssessmentPort;
import com.banking.loan.application.ports.out.ComplianceCheckPort;
import com.banking.loan.domain.loan.*;
import com.banking.loan.domain.services.PaymentScheduleGenerator;
import com.banking.loan.domain.shared.DomainEventPublisher;
import com.banking.loan.domain.shared.Customer;
import com.banking.loan.application.commands.*;
import com.banking.loan.application.queries.*;
import com.banking.loan.application.results.*;
import com.banking.loan.application.exceptions.*;
import com.banking.loan.application.mappers.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Loan Application Service - Hexagonal Architecture Application Layer
 * Orchestrates use cases and coordinates between domain and infrastructure
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LoanApplicationService implements LoanApplicationUseCase {
    
    private final LoanRepository loanRepository;
    private final CustomerRepository customerRepository;
    private final AIRiskAssessmentPort aiRiskAssessmentPort;
    private final ComplianceCheckPort complianceCheckPort;
    private final DomainEventPublisher eventPublisher;
    
    @Override
    public LoanApplicationResult submitLoanApplication(SubmitLoanApplicationCommand command) {
        log.info("Processing loan application for customer: {}", command.customerId());
        
        try {
            // Validate customer exists and is eligible
            Customer customer = customerRepository.findById(new CustomerId(command.customerId()))
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + command.customerId()));
            
            validateCustomerEligibility(customer, command.amount());
            
            // Create loan aggregate
            LoanAggregate loan = LoanAggregate.createApplication(
                new LoanId(UUID.randomUUID().toString()),
                new CustomerId(command.customerId()),
                new LoanAmount(command.amount(), "USD"),
                new LoanTerm(command.termInMonths()),
                LoanType.valueOf(command.loanType()),
                command.applicantId(),
                command.correlationId(),
                command.tenantId()
            );
            
            // Save loan (this will trigger domain events)
            LoanAggregate savedLoan = loanRepository.save(loan);
            
            // Publish domain events
            eventPublisher.publishEventsFrom(savedLoan);
            
            // Trigger AI risk assessment asynchronously
            initiateAIRiskAssessment(savedLoan.getId(), command.correlationId());
            
            log.info("Loan application submitted successfully: {}", savedLoan.getId().value());
            
            return new LoanApplicationResult(
                savedLoan.getId().value(),
                generateApplicationReference(savedLoan),
                savedLoan.getCustomerId().value(),
                savedLoan.getAmount().value(),
                savedLoan.getStatus().name(),
                java.time.LocalDateTime.now(),
                "Loan application submitted successfully"
            );
                
        } catch (Exception e) {
            log.error("Failed to submit loan application for customer: {}", command.customerId(), e);
            throw new LoanApplicationException("Failed to submit loan application", e);
        }
    }
    
    @Override
    public LoanApprovalResult approveLoan(ApproveLoanCommand command) {
        log.info("Processing loan approval: {}", command.loanId());
        
        try {
            LoanAggregate loan = loanRepository.findById(new LoanId(command.loanId()))
                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + command.loanId()));
            
            // Perform final compliance check
            ComplianceCheckResult complianceResult = complianceCheckPort.performFinalCheck(
                loan.getId().value(), 
                command.correlationId()
            );
            
            if (!complianceResult.isCompliant()) {
                throw new ComplianceViolationException("Loan approval failed compliance check");
            }
            
            // Create payment schedule
            PaymentSchedule schedule = PaymentScheduleGenerator.generate(
                loan.getAmount(),
                loan.getTerm(),
                new InterestRate(command.approvedInterestRate())
            );
            
            // Approve the loan
            loan.approve(
                new InterestRate(command.approvedInterestRate()),
                schedule,
                command.approvedBy(),
                command.correlationId()
            );
            
            // Save and publish events
            LoanAggregate savedLoan = loanRepository.save(loan);
            eventPublisher.publishEventsFrom(savedLoan);
            
            log.info("Loan approved successfully: {}", savedLoan.getId().value());
            
            return LoanApprovalResult.builder()
                .loanId(savedLoan.getId().value())
                .approvedAmount(savedLoan.getAmount().value())
                .interestRate(savedLoan.getInterestRate().value())
                .firstPaymentDate(schedule.getFirstPaymentDate())
                .monthlyInstallment(schedule.getMonthlyInstallment())
                .totalPayableAmount(schedule.getTotalPayableAmount())
                .loanAgreementNumber(generateLoanAgreementNumber(savedLoan))
                .build();
                
        } catch (Exception e) {
            log.error("Failed to approve loan: {}", command.loanId(), e);
            throw new LoanApprovalException("Failed to approve loan", e);
        }
    }
    
    @Override
    public LoanRejectionResult rejectLoan(RejectLoanCommand command) {
        log.info("Processing loan rejection: {}", command.loanId());
        
        try {
            LoanAggregate loan = loanRepository.findById(new LoanId(command.loanId()))
                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + command.loanId()));
            
            List<RejectionReason> reasons = command.rejectionReasons().stream()
                .map(reason -> new RejectionReason(
                    "MANUAL_REJECTION",
                    reason,
                    "MANUAL",
                    true
                ))
                .toList();
            
            loan.reject(reasons, command.rejectedBy(), command.correlationId());
            
            // Save and publish events
            LoanAggregate savedLoan = loanRepository.save(loan);
            eventPublisher.publishEventsFrom(savedLoan);
            
            log.info("Loan rejected successfully: {}", savedLoan.getId().value());
            
            return LoanRejectionResult.builder()
                .loanId(savedLoan.getId().value())
                .rejectionReasons(command.rejectionReasons())
                .alternativeOptions(generateAlternativeOptions(savedLoan))
                .appealProcess("Contact customer service within 30 days")
                .build();
                
        } catch (Exception e) {
            log.error("Failed to reject loan: {}", command.loanId(), e);
            throw new LoanRejectionException("Failed to reject loan", e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public LoanDetails getLoanDetails(GetLoanDetailsQuery query) {
        log.debug("Retrieving loan details: {}", query.loanId());
        
        LoanAggregate loan = loanRepository.findById(new LoanId(query.loanId()))
            .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + query.loanId()));
        
        return LoanDetailsMapper.toDetails(loan);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<LoanSummary> getCustomerLoans(GetCustomerLoansQuery query) {
        log.debug("Retrieving loans for customer: {}", query.customerId());
        
        List<LoanAggregate> loans = loanRepository.findByCustomerId(new CustomerId(query.customerId()));
        
        return loans.stream()
            .map(LoanSummaryMapper::toSummary)
            .toList();
    }
    
    // Private helper methods
    
    private void validateCustomerEligibility(Customer customer, BigDecimal loanAmount) {
        if (!customer.isEligibleForLoan(loanAmount)) {
            throw new CustomerNotEligibleException("Customer not eligible for loan amount: " + loanAmount);
        }
    }
    
    private void initiateAIRiskAssessment(LoanId loanId, String correlationId) {
        // This triggers an asynchronous AI risk assessment
        // The result will be handled by an event handler
        aiRiskAssessmentPort.initiateAssessment(loanId.value(), correlationId);
    }
    
    private String generateApplicationReference(LoanAggregate loan) {
        return "LA-" + loan.getId().value().substring(0, 8).toUpperCase() + 
               "-" + System.currentTimeMillis() % 10000;
    }
    
    private String generateLoanAgreementNumber(LoanAggregate loan) {
        return "AGR-" + loan.getId().value().substring(0, 8).toUpperCase() + 
               "-" + loan.getCustomerId().value().substring(0, 4).toUpperCase();
    }
    
    private List<String> generateAlternativeOptions(LoanAggregate loan) {
        // AI-powered alternative recommendations could be integrated here
        return List.of(
            "Consider a smaller loan amount",
            "Extend the loan term for lower monthly payments",
            "Provide additional collateral",
            "Add a co-signer to the application"
        );
    }
}

