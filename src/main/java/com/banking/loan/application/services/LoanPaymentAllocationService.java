package com.banking.loan.application.services;

import com.banking.loan.application.commands.ProcessLoanPaymentCommand;
import com.banking.loan.application.results.PaymentAllocationResult;
import com.banking.loan.domain.loan.Loan;
import com.banking.loan.domain.loan.LoanInstallment;
import com.banking.loan.domain.services.PaymentWaterfallService;
import com.banking.loan.domain.services.LateFeeCalculationService;
import com.banking.loan.domain.ports.out.LoanRepository;
import com.banking.loan.infrastructure.compliance.RegulatoryComplianceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Industry-Standard Loan Payment Allocation Service
 * 
 * Implements banking industry best practices for loan payment processing:
 * - Payment waterfall allocation (fees → interest → principal)
 * - Late fee assessment and penalty calculation
 * - Partial payment handling with proper allocation rules
 * - Regulatory compliance validation (TILA, RESPA, FDCPA)
 * - Transaction isolation and audit trail generation
 * 
 * Architecture Guardrails Compliance:
 * ✅ Request Parsing: Uses typed command objects
 * ✅ Validation: Comprehensive business rule validation
 * ✅ Response Types: Structured result objects
 * ✅ Type Safety: Strong typing throughout
 * ✅ Dependency Inversion: Interface-based dependencies
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoanPaymentAllocationService {
    
    private final LoanRepository loanRepository;
    private final PaymentWaterfallService paymentWaterfallService;
    private final LateFeeCalculationService lateFeeCalculationService;
    private final RegulatoryComplianceService regulatoryComplianceService;
    
    /**
     * Process loan payment with industry-standard allocation logic
     * 
     * Payment Waterfall Order (Industry Standard):
     * 1. Late fees and penalties
     * 2. Accrued interest
     * 3. Principal (current installment)
     * 4. Principal (past due installments)
     * 5. Principal (future installments - prepayment)
     */
    @Transactional
    public PaymentAllocationResult processLoanPayment(ProcessLoanPaymentCommand command) {
        
        log.info("Processing loan payment: loanId={}, amount={}, correlationId={}", 
            command.loanId(), command.amount(), command.correlationId());
        
        // 1. Load loan with payment schedule and current state
        Loan loan = loanRepository.findByIdWithScheduleAndPayments(command.loanId())
            .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + command.loanId()));
        
        // 2. Validate payment request
        validatePaymentRequest(loan, command);
        
        // 3. Assess late fees and penalties as of payment date
        LateFeeAssessment lateFeeAssessment = lateFeeCalculationService.assessLateFees(
            loan, command.paymentDate());
        
        // 4. Create payment waterfall based on loan type and jurisdiction
        PaymentWaterfall waterfall = paymentWaterfallService.createWaterfall(
            loan.getLoanType(), 
            loan.getJurisdiction(),
            lateFeeAssessment
        );
        
        // 5. Allocate payment according to industry standard waterfall
        PaymentAllocationResult allocationResult = waterfall.allocatePayment(
            loan,
            command.amount(),
            command.paymentDate(),
            command.allocationStrategy()
        );
        
        // 6. Apply allocations to loan and installments
        applyPaymentAllocations(loan, allocationResult);
        
        // 7. Regulatory compliance validation
        regulatoryComplianceService.validatePaymentCompliance(loan, allocationResult);
        
        // 8. Save updated loan state
        loanRepository.save(loan);
        
        // 9. Log payment processing completion
        log.info("Payment allocation completed: loanId={}, totalAllocated={}, correlationId={}", 
            command.loanId(), allocationResult.getTotalAllocated(), command.correlationId());
        
        return allocationResult;
    }
    
    /**
     * Validate payment request against business rules and regulatory requirements
     */
    private void validatePaymentRequest(Loan loan, ProcessLoanPaymentCommand command) {
        
        // Business rule validations
        if (command.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentException("Payment amount must be positive");
        }
        
        if (command.paymentDate().isAfter(LocalDate.now().plusDays(1))) {
            throw new InvalidPaymentException("Payment date cannot be more than 1 day in the future");
        }
        
        if (loan.getStatus().equals("CHARGED_OFF")) {
            throw new InvalidPaymentException("Payments cannot be processed for charged-off loans");
        }
        
        // Regulatory validations
        if (!regulatoryComplianceService.isPaymentMethodAllowed(command.paymentMethod(), loan.getLoanType())) {
            throw new InvalidPaymentException("Payment method not allowed for this loan type");
        }
        
        // Fraud detection placeholder
        if (command.fraudCheckRequired() && isSuspiciousPayment(command)) {
            throw new FraudDetectionException("Payment flagged for manual review");
        }
    }
    
    /**
     * Apply payment allocations to loan and installment entities
     */
    private void applyPaymentAllocations(Loan loan, PaymentAllocationResult allocationResult) {
        
        // Apply late fee payments
        allocationResult.getLateFeeAllocations().forEach(allocation -> {
            loan.applyLateFeePayment(allocation.getInstallmentId(), allocation.getAmount());
        });
        
        // Apply interest payments
        allocationResult.getInterestAllocations().forEach(allocation -> {
            LoanInstallment installment = loan.getInstallment(allocation.getInstallmentId());
            installment.applyInterestPayment(allocation.getAmount());
        });
        
        // Apply principal payments
        allocationResult.getPrincipalAllocations().forEach(allocation -> {
            LoanInstallment installment = loan.getInstallment(allocation.getInstallmentId());
            installment.applyPrincipalPayment(allocation.getAmount());
        });
        
        // Update loan balance and status
        loan.updateBalanceFromPayment(allocationResult.getTotalAllocated());
        
        // Check if loan is paid in full
        if (loan.getRemainingBalance().compareTo(BigDecimal.ZERO) == 0) {
            loan.markAsPaidInFull();
        }
    }
    
    /**
     * Fraud detection logic placeholder
     */
    private boolean isSuspiciousPayment(ProcessLoanPaymentCommand command) {
        // Implement fraud detection rules:
        // - Large payment amounts
        // - Unusual payment patterns
        // - Payment method mismatches
        // - Geographic anomalies
        // - Velocity checks
        
        return command.amount().compareTo(new BigDecimal("50000")) > 0; // Simple threshold
    }
    
    /**
     * Calculate payment allocation for specific installment
     * Used for installment-specific payment processing
     */
    @Transactional(readOnly = true)
    public PaymentAllocationResult calculateInstallmentPayment(
            String loanId, 
            Integer installmentNumber, 
            BigDecimal paymentAmount) {
        
        Loan loan = loanRepository.findByIdWithSchedule(loanId)
            .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + loanId));
        
        LoanInstallment installment = loan.getInstallmentByNumber(installmentNumber)
            .orElseThrow(() -> new InstallmentNotFoundException(
                "Installment not found: " + installmentNumber + " for loan: " + loanId));
        
        // Calculate allocation for specific installment
        return paymentWaterfallService.calculateInstallmentPaymentAllocation(
            installment, paymentAmount, LocalDate.now());
    }
    
    // Exception classes for proper error handling
    public static class LoanNotFoundException extends RuntimeException {
        public LoanNotFoundException(String message) { super(message); }
    }
    
    public static class InstallmentNotFoundException extends RuntimeException {
        public InstallmentNotFoundException(String message) { super(message); }
    }
    
    public static class InvalidPaymentException extends RuntimeException {
        public InvalidPaymentException(String message) { super(message); }
    }
    
    public static class FraudDetectionException extends RuntimeException {
        public FraudDetectionException(String message) { super(message); }
    }
}